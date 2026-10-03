import type { FastifyPluginAsync } from 'fastify';
import { UsersController } from '../controllers/index.js';
import { requireAuth } from '../middleware/auth.js';

export const profileRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.addHook('preHandler', requireAuth);

  // GET /api/v1/profile
  fastify.get('/', UsersController.getProfile);
  fastify.get('/me', UsersController.getProfile);

  // PATCH /api/v1/profile
  fastify.patch('/', UsersController.updateProfile);
  fastify.patch('/me', UsersController.updateProfile);
};
