import test, { describe, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { jobLock } from '../src/automation/job-lock.js';
import { JobRunner, type JobDefinition } from '../src/automation/job-runner.js';
import { scheduler } from '../src/automation/scheduler.js';
import { db } from '../src/database/index.js';

describe('LunaCare Automation System & Job Runner', () => {
  beforeEach(() => {
    jobLock.reset();
    db.reset();
  });

  // TEST 1: Job locking prevents overlapping executions
  test('1. Job locking prevents duplicate simultaneous execution', async () => {
    const lock1 = jobLock.acquireLock('test_job', 10000, 'worker_1');
    assert.equal(lock1, true, 'First worker should acquire the lock');

    const lock2 = jobLock.acquireLock('test_job', 10000, 'worker_2');
    assert.equal(lock2, false, 'Second worker must be denied the lock');

    assert.equal(jobLock.isLocked('test_job'), true);

    // Release lock by worker 1
    const release = jobLock.releaseLock('test_job', 'worker_1');
    assert.equal(release, true);

    // Now worker 2 can acquire
    const lock3 = jobLock.acquireLock('test_job', 10000, 'worker_2');
    assert.equal(lock3, true);
    jobLock.releaseLock('test_job', 'worker_2');
  });

  // TEST 2: JobRunner skips safely when job is locked
  test('2. JobRunner skips safely when job is already locked', async () => {
    jobLock.acquireLock('locked_job', 10000, 'other_instance');

    let executed = false;
    const dummyJob: JobDefinition = {
      name: 'locked_job',
      execute: async () => {
        executed = true;
      }
    };

    const result = await JobRunner.run(dummyJob);
    assert.equal(result.status, 'skipped');
    assert.equal(executed, false, 'Job function must not run when locked');

    jobLock.releaseLock('locked_job', 'other_instance');
  });

  // TEST 3: Job timeout enforcement
  test('3. Job timeout triggers error and releases lock', async () => {
    const slowJob: JobDefinition = {
      name: 'slow_job',
      timeoutMs: 50, // 50ms short timeout
      retries: 0,
      execute: async () => {
        await new Promise(resolve => setTimeout(resolve, 200));
      }
    };

    const result = await JobRunner.run(slowJob);
    assert.equal(result.status, 'failed');
    assert.ok(result.error?.includes('timed out'));

    // Lock must be released even after timeout
    assert.equal(jobLock.isLocked('slow_job'), false);
  });

  // TEST 4: Job failure handling, retry bounding, and lock release
  test('4. Job failure retries up to bounded limit and audits failure', async () => {
    let callCount = 0;
    const failingJob: JobDefinition = {
      name: 'failing_job',
      retries: 2,
      execute: async () => {
        callCount++;
        throw new Error('Database connection reset');
      }
    };

    const result = await JobRunner.run(failingJob);
    assert.equal(result.status, 'failed');
    assert.equal(result.attempts, 3, 'Initial run + 2 retries = 3 attempts');
    assert.equal(callCount, 3);
    assert.ok(result.error?.includes('Database connection reset'));

    // Lock must be cleanly released after all retries fail
    assert.equal(jobLock.isLocked('failing_job'), false);

    // Verify audit events recorded: start and failure
    const auditLogs = db.getAuditLogs();
    const startEvent = auditLogs.find(l => l.action === 'automation.job.started' && l.resourceId === 'failing_job');
    const failEvent = auditLogs.find(l => l.action === 'automation.job.failed' && l.resourceId === 'failing_job');

    assert.ok(startEvent, 'Must audit job start');
    assert.ok(failEvent, 'Must audit job failure');
  });

  // TEST 5: Successful job audits status and returns details
  test('5. Successful job completes, audits status, and releases lock', async () => {
    const successJob: JobDefinition = {
      name: 'success_job',
      execute: async () => {
        return { itemsCleaned: 15 };
      }
    };

    const result = await JobRunner.run(successJob);
    assert.equal(result.status, 'completed');
    assert.equal(result.details?.['itemsCleaned'], 15);
    assert.equal(jobLock.isLocked('success_job'), false);

    const auditLogs = db.getAuditLogs();
    const completedEvent = auditLogs.find(l => l.action === 'automation.job.completed' && l.resourceId === 'success_job');
    assert.ok(completedEvent, 'Must audit job completion');
  });

  // TEST 6: Central scheduler registry and start/stop lifecycle
  test('6. Scheduler registers central jobs and handles start/stop gracefully', () => {
    const jobs = scheduler.getRegisteredJobs();
    assert.ok(jobs.includes('cleanupExpiredOtpState'));
    assert.ok(jobs.includes('cleanupOldRateLimitEntries'));
    assert.ok(jobs.includes('cleanupExpiredSessions'));
    assert.ok(jobs.includes('cleanupTemporaryUploads'));
    assert.ok(jobs.includes('processAccountDeletionQueue'));
    assert.ok(jobs.includes('securityAuditMaintenance'));

    // Lifecycle check
    scheduler.start();
    scheduler.stop();
  });
});
