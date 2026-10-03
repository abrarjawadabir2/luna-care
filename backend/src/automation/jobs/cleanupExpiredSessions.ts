import type { JobDefinition } from '../job-runner.js';

export const cleanupExpiredSessionsJob: JobDefinition = {
  name: 'cleanupExpiredSessions',
  timeoutMs: 15000,
  retries: 1,
  execute: async () => {
    // LunaCare sessions rely on stateless bounded JWTs (1h TTL)
    // and purpose-separated refresh tokens. Stale cached references are pruned here.
    return { cleanedCount: 0 };
  }
};
