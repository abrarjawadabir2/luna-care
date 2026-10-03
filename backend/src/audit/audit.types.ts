export type AuditEventType =
  | 'auth.login.success'
  | 'auth.login.failure'
  | 'auth.otp.request'
  | 'auth.otp.failure'
  | 'auth.otp.success'
  | 'auth.password_reset.request'
  | 'security.rate_limit.triggered'
  | 'security.authorization.denied'
  | 'account.profile.updated'
  | 'account.export.requested'
  | 'account.delete.requested'
  | 'account.deleted'
  | 'admin.role.changed'
  | 'file.upload.accepted'
  | 'file.upload.rejected'
  | 'ai.request.accepted'
  | 'ai.request.rejected'
  | 'database.access.denied'
  | 'automation.job.started'
  | 'automation.job.completed'
  | 'automation.job.failed';

export type AuditResult = 'success' | 'failure' | 'denied';

export interface AuditEvent {
  id?: string;
  eventType: AuditEventType | string;
  userId?: string;
  requestId?: string;
  resourceType?: string;
  resourceId?: string;
  result: AuditResult;
  reasonCode?: string;
  timestamp: Date;
  metadata?: Record<string, unknown>;
}

export interface CreateAuditEventInput {
  eventType: AuditEventType | string;
  userId?: string;
  requestId?: string;
  resourceType?: string;
  resourceId?: string;
  result: AuditResult;
  reasonCode?: string;
  timestamp?: Date;
  metadata?: Record<string, unknown>;
}
