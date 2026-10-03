import type { FastifyPluginAsync } from 'fastify';
import { AuditController } from '../controllers/index.js';
import { requireAuth, requireRole } from '../middleware/auth.js';

export const auditRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.addHook('preHandler', requireAuth);
  // STRICT: Only ADMIN and SUPER_ADMIN can view system audit logs
  fastify.addHook('preHandler', requireRole(['ADMIN', 'SUPER_ADMIN']));

  // GET /api/v1/audit/logs
  fastify.get('/logs', AuditController.getLogs);
};
