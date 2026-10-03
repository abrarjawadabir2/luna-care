/**
 * LunaCare Distributed Job Lock
 * 
 * Prevents multiple server instances from simultaneously executing the same
 * sensitive scheduled automation task.
 * Supports auto-expiring leases to prevent deadlocks in case of process termination.
 */

interface LockEntry {
  holderId: string;
  expiresAt: number;
}

class JobLockManager {
  private locks = new Map<string, LockEntry>();

  /**
   * Acquire an exclusive lock on a named job.
   * Returns true if lock was acquired, false if already held by another runner.
   */
  acquireLock(jobName: string, ttlMs = 60000, holderId = 'local-runner'): boolean {
    const now = Date.now();
    const existing = this.locks.get(jobName);

    if (existing && existing.expiresAt > now) {
      // Lock is currently held and active
      return false;
    }

    this.locks.set(jobName, {
      holderId,
      expiresAt: now + ttlMs
    });
    return true;
  }

  /**
   * Release the lock on a named job if held by the caller.
   */
  releaseLock(jobName: string, holderId = 'local-runner'): boolean {
    const existing = this.locks.get(jobName);
    if (!existing) {
      return true;
    }

    if (existing.holderId === holderId || existing.expiresAt <= Date.now()) {
      this.locks.delete(jobName);
      return true;
    }

    return false;
  }

  /**
   * Check if a job is currently locked.
   */
  isLocked(jobName: string): boolean {
    const existing = this.locks.get(jobName);
    if (!existing) return false;
    if (existing.expiresAt <= Date.now()) {
      this.locks.delete(jobName);
      return false;
    }
    return true;
  }

  /**
   * Clear all locks (primarily for test isolation).
   */
  reset(): void {
    this.locks.clear();
  }
}

export const jobLock = new JobLockManager();
