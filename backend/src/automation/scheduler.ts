import { JobRunner, type JobDefinition } from './job-runner.js';
import { cleanupExpiredOtpStateJob } from './jobs/cleanupExpiredOtpState.js';
import { cleanupOldRateLimitEntriesJob } from './jobs/cleanupOldRateLimitEntries.js';
import { cleanupExpiredSessionsJob } from './jobs/cleanupExpiredSessions.js';
import { cleanupTemporaryUploadsJob } from './jobs/cleanupTemporaryUploads.js';
import { cleanupTemporaryLocationDataJob } from './jobs/cleanupTemporaryLocationData.js';
import { cleanupAbandonedUploadRecordsJob } from './jobs/cleanupAbandonedUploadRecords.js';
import { processAccountDeletionQueueJob } from './jobs/processAccountDeletionQueue.js';
import { securityAuditMaintenanceJob } from './jobs/securityAuditMaintenance.js';
import { logger } from '../utils/logger.js';

export interface ScheduledTask {
  job: JobDefinition;
  intervalMs: number;
  timer?: NodeJS.Timeout;
}

export class Scheduler {
  private tasks: Map<string, ScheduledTask> = new Map();
  private isRunning = false;

  constructor() {
    this.registerJobs();
  }

  private registerJobs() {
    // 1. Cleanup expired OTPs every 5 minutes
    this.register(cleanupExpiredOtpStateJob, 5 * 60 * 1000);

    // 2. Cleanup expired rate limit buckets every 10 minutes
    this.register(cleanupOldRateLimitEntriesJob, 10 * 60 * 1000);

    // 3. Process account deletion queue every 5 minutes
    this.register(processAccountDeletionQueueJob, 5 * 60 * 1000);

    // 4. Cleanup expired uploads and abandoned records hourly
    this.register(cleanupTemporaryUploadsJob, 60 * 60 * 1000);
    this.register(cleanupAbandonedUploadRecordsJob, 60 * 60 * 1000);

    // 5. Cleanup expired sessions hourly
    this.register(cleanupExpiredSessionsJob, 60 * 60 * 1000);

    // 6. Cleanup temporary location data hourly
    this.register(cleanupTemporaryLocationDataJob, 60 * 60 * 1000);

    // 7. Security audit maintenance daily
    this.register(securityAuditMaintenanceJob, 24 * 60 * 60 * 1000);
  }

  register(job: JobDefinition, intervalMs: number) {
    this.tasks.set(job.name, { job, intervalMs });
  }

  /**
   * Start the scheduler and all periodic timers.
   */
  start() {
    if (this.isRunning) return;
    this.isRunning = true;
    logger.info('automation.scheduler.started', { taskCount: this.tasks.size });

    for (const [name, task] of this.tasks.entries()) {
      const timer = setInterval(async () => {
        try {
          await JobRunner.run(task.job);
        } catch (err) {
          logger.error('automation.scheduler.error', {
            jobName: name,
            error: err instanceof Error ? err.message : String(err)
          });
        }
      }, task.intervalMs);

      // Unref so timers don't prevent Node process termination during tests
      if (typeof timer.unref === 'function') {
        timer.unref();
      }

      task.timer = timer;
    }
  }

  /**
   * Stop all timers gracefully.
   */
  stop() {
    if (!this.isRunning) return;
    for (const task of this.tasks.values()) {
      if (task.timer) {
        clearInterval(task.timer);
        task.timer = undefined;
      }
    }
    this.isRunning = false;
    logger.info('automation.scheduler.stopped', {});
  }

  /**
   * Run a specific job immediately (on-demand execution).
   */
  async trigger(jobName: string) {
    const task = this.tasks.get(jobName);
    if (!task) {
      throw new Error(`Job '${jobName}' not found in scheduler registry.`);
    }
    return JobRunner.run(task.job);
  }

  getRegisteredJobs(): string[] {
    return Array.from(this.tasks.keys());
  }
}

export const scheduler = new Scheduler();
