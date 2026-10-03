import type { FastifyPluginAsync } from 'fastify';
import { HealthController } from '../controllers/index.js';
import { requireAuth } from '../middleware/auth.js';

export const moodsRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.addHook('preHandler', requireAuth);

  // GET /api/v1/moods
  fastify.get('/', HealthController.getBehaviourLogs);

  // POST /api/v1/moods
  fastify.post('/', HealthController.createBehaviourLog);

  // DELETE /api/v1/moods/:id
  fastify.delete('/:id', HealthController.deleteBehaviourLog);
};
