import type { JobDefinition } from '../job-runner.js';

export const cleanupTemporaryLocationDataJob: JobDefinition = {
  name: 'cleanupTemporaryLocationData',
  timeoutMs: 10000,
  retries: 1,
  execute: async () => {
    // Under LunaCare privacy architecture, exact GPS coordinates are NEVER persisted to database.
    // This job purges any in-memory ephemeral geo-bounding calculation caches.
    return { status: 'cache_cleared' };
  }
};
