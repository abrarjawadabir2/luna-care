import crypto from 'node:crypto';
import { db } from '../database/client.js';
import { logger } from '../utils/logger.js';
import { sanitizeAuditMetadata } from './audit.sanitizer.js';
import type { AuditEvent, CreateAuditEventInput } from './audit.types.js';

export class AuditService {
  /**
   * Server-generated audit event recording.
   * Prohibited fields are stripped before storage or logging.
   */
  static async recordEvent(input: CreateAuditEventInput): Promise<AuditEvent> {
    const id = crypto.randomUUID();
    const timestamp = input.timestamp || new Date();
    const safeMetadata = input.metadata
      ? (sanitizeAuditMetadata(input.metadata) as Record<string, unknown>)
      : {};

    const auditEvent: AuditEvent = {
      id,
      eventType: input.eventType,
      userId: input.userId,
      requestId: input.requestId,
      resourceType: input.resourceType,
      resourceId: input.resourceId,
      result: input.result,
      reasonCode: input.reasonCode,
      timestamp,
      metadata: safeMetadata
    };

    // Store in immutable append-only database table/store
    db.addAuditLog({
      id,
      actorId: input.userId,
      actorRole: input.userId ? 'USER' : 'SYSTEM',
      action: input.eventType,
      resourceType: input.resourceType || 'system',
      resourceId: input.resourceId,
      metadata: safeMetadata,
      createdAt: timestamp.toISOString()
    });

    // Structured server log
    const logMeta = {
      auditId: id,
      eventType: input.eventType,
      userId: input.userId,
      requestId: input.requestId,
      result: input.result,
      reasonCode: input.reasonCode,
      ...safeMetadata
    };

    if (input.result === 'failure' || input.result === 'denied') {
      logger.warn(`audit.${input.eventType}`, logMeta);
    } else {
      logger.info(`audit.${input.eventType}`, logMeta);
    }

    return auditEvent;
  }

  /**
   * Retrieve audit logs (Admin only)
   */
  static getAuditLogs(): AuditEvent[] {
    const rawLogs = db.getAuditLogs();
    return rawLogs.map(r => ({
      id: r.id,
      eventType: r.action,
      userId: r.actorId,
      resourceType: r.resourceType,
      resourceId: r.resourceId,
      result: 'success',
      timestamp: new Date(r.createdAt),
      metadata: r.metadata
    }));
  }
}
