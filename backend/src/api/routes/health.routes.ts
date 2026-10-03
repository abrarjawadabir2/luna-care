import type { FastifyPluginAsync } from 'fastify';
import { HealthController } from '../controllers/index.js';
import { requireAuth } from '../middleware/auth.js';

export const healthRoutes: FastifyPluginAsync = async (fastify) => {
  // GET /api/v1/health — Safe operational information only
  fastify.get('/', async (_request, reply) => {
    return reply.status(200).send({
      status: 'ok'
    });
  });

  // GET /api/v1/health/behaviour (authenticated alias)
  fastify.get('/behaviour', { preHandler: requireAuth }, HealthController.getBehaviourLogs);

  // POST /api/v1/health/behaviour (authenticated alias)
  fastify.post('/behaviour', { preHandler: requireAuth }, HealthController.createBehaviourLog);
};
