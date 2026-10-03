import crypto from 'node:crypto';
import { jobLock } from './job-lock.js';
import { AuditService } from '../audit/audit.service.js';
import { logger } from '../utils/logger.js';

export interface JobContext {
  jobId: string;
  jobName: string;
  attempt: number;
}

export interface JobDefinition {
  name: string;
  timeoutMs?: number; // Default 30s
  retries?: number;   // Default 1
  execute: (context: JobContext) => Promise<Record<string, unknown> | void>;
}

export interface JobExecutionResult {
  jobId: string;
  jobName: string;
  status: 'completed' | 'skipped' | 'failed';
  attempts: number;
  durationMs: number;
  details?: Record<string, unknown>;
  error?: string;
}

export class JobRunner {
  /**
   * Safely execute an automation job with locking, timeout, retry bounding, and sanitized auditing.
   */
  static async run(job: JobDefinition): Promise<JobExecutionResult> {
    const jobId = crypto.randomUUID();
    const timeoutMs = job.timeoutMs ?? 30000;
    const maxRetries = Math.min(job.retries ?? 1, 3); // Bounded retries
    const startTime = Date.now();

    // 1. Acquire execution lock to prevent overlapping runs
    const lockAcquired = jobLock.acquireLock(job.name, timeoutMs + 10000, jobId);
    if (!lockAcquired) {
      logger.info('automation.job.skipped', { jobName: job.name, reason: 'Lock held by another instance' });
      return {
        jobId,
        jobName: job.name,
        status: 'skipped',
        attempts: 0,
        durationMs: Date.now() - startTime,
        details: { reason: 'locked' }
      };
    }

    let lastError: Error | null = null;
    let attempts = 0;

    try {
      // Audit job start
      await AuditService.recordEvent({
        eventType: 'automation.job.started',
        resourceType: 'automation_job',
        resourceId: job.name,
        result: 'success',
        metadata: { jobId, jobName: job.name }
      });

      // 2. Execute with timeout & bounded retry
      while (attempts <= maxRetries) {
        attempts++;
        try {
          const result = await this.executeWithTimeout(job, { jobId, jobName: job.name, attempt: attempts }, timeoutMs);

          const durationMs = Date.now() - startTime;
          await AuditService.recordEvent({
            eventType: 'automation.job.completed',
            resourceType: 'automation_job',
            resourceId: job.name,
            result: 'success',
            metadata: { jobId, jobName: job.name, durationMs, attempts, ...result }
          });

          return {
            jobId,
            jobName: job.name,
            status: 'completed',
            attempts,
            durationMs,
            details: result || undefined
          };
        } catch (err) {
          lastError = err instanceof Error ? err : new Error(String(err));
          logger.warn('automation.job.attempt_failed', {
            jobId,
            jobName: job.name,
            attempt: attempts,
            error: lastError.message
          });

          if (attempts > maxRetries) {
            break;
          }
        }
      }

      // Max retries exceeded
      const durationMs = Date.now() - startTime;
      const safeErrorMessage = lastError ? lastError.message.substring(0, 200) : 'Unknown execution failure';

      await AuditService.recordEvent({
        eventType: 'automation.job.failed',
        resourceType: 'automation_job',
        resourceId: job.name,
        result: 'failure',
        reasonCode: 'MAX_RETRIES_EXCEEDED',
        metadata: { jobId, jobName: job.name, durationMs, attempts, error: safeErrorMessage }
      });

      return {
        jobId,
        jobName: job.name,
        status: 'failed',
        attempts,
        durationMs,
        error: safeErrorMessage
      };
    } finally {
      // 3. Always release lock in finally block
      jobLock.releaseLock(job.name, jobId);
    }
  }

  private static executeWithTimeout(
    job: JobDefinition,
    context: JobContext,
    timeoutMs: number
  ): Promise<Record<string, unknown> | void> {
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        reject(new Error(`Job '${job.name}' timed out after ${timeoutMs}ms`));
      }, timeoutMs);

      // Unref timer so it doesn't hold open Node process in tests
      if (typeof timer.unref === 'function') {
        timer.unref();
      }

      Promise.resolve(job.execute(context))
        .then(result => {
          clearTimeout(timer);
          resolve(result);
        })
        .catch(err => {
          clearTimeout(timer);
          reject(err);
        });
    });
  }
}
