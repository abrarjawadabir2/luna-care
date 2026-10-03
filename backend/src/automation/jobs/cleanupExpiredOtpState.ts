import type { JobDefinition } from '../job-runner.js';
import { db } from '../../database/client.js';

export const cleanupExpiredOtpStateJob: JobDefinition = {
  name: 'cleanupExpiredOtpState',
  timeoutMs: 10000,
  retries: 1,
  execute: async () => {
    const cleanedCount = db.cleanupExpiredOtps();
    return { cleanedCount };
  }
};
