import type { FastifyPluginAsync } from 'fastify';
import { PeriodsController } from '../controllers/index.js';
import { requireAuth } from '../middleware/auth.js';

export const periodsRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.addHook('preHandler', requireAuth);

  // GET /api/v1/periods
  fastify.get('/', PeriodsController.getPeriodLogs);

  // POST /api/v1/periods
  fastify.post('/', PeriodsController.createPeriodLog);

  // DELETE /api/v1/periods/:id
  fastify.delete('/:id', PeriodsController.deletePeriodLog);
};
