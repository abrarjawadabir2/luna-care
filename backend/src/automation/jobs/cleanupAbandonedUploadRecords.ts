import type { JobDefinition } from '../job-runner.js';
import { db } from '../../database/client.js';

export const cleanupAbandonedUploadRecordsJob: JobDefinition = {
  name: 'cleanupAbandonedUploadRecords',
  timeoutMs: 15000,
  retries: 1,
  execute: async () => {
    const cleanedCount = db.cleanupExpiredUploads();
    return { cleanedCount };
  }
};
