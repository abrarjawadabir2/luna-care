import type { JobDefinition } from '../job-runner.js';
import { db } from '../../database/client.js';

export const securityAuditMaintenanceJob: JobDefinition = {
  name: 'securityAuditMaintenance',
  timeoutMs: 30000,
  retries: 1,
  execute: async () => {
    // Retain audit records within standard 90-day retention policy
    const prunedCount = db.cleanupAuditLogs(90);
    return { prunedCount, retentionDays: 90 };
  }
};
