import type { JobDefinition } from '../job-runner.js';
import { db } from '../../database/client.js';

export const processAccountDeletionQueueJob: JobDefinition = {
  name: 'processAccountDeletionQueue',
  timeoutMs: 30000,
  retries: 2,
  execute: async () => {
    const deletedCount = db.processDeletionQueue();
    return { deletedCount };
  }
};
