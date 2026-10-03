import type { JobDefinition } from '../job-runner.js';
import { rateLimiter } from '../../api/middleware/rateLimit.js';

export const cleanupOldRateLimitEntriesJob: JobDefinition = {
  name: 'cleanupOldRateLimitEntries',
  timeoutMs: 10000,
  retries: 1,
  execute: async () => {
    const cleanedCount = rateLimiter.cleanupExpired();
    return { cleanedCount };
  }
};
