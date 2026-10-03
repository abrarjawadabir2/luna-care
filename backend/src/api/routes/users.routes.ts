import type { FastifyPluginAsync } from 'fastify';
import { UsersController } from '../controllers/index.js';
import { requireAuth } from '../middleware/auth.js';
import { createRateLimiter } from '../middleware/rateLimit.js';

export const usersRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.addHook('preHandler', requireAuth);

  const accountOpLimiter = createRateLimiter({
    maxLimit: 5,
    windowMs: 60 * 60 * 1000,
    keyPrefix: 'account_op',
    extractKey: (req) => req.user?.id || 'anonymous'
  });

  // GET /api/v1/users/me
  fastify.get('/me', UsersController.getProfile);

  // PATCH /api/v1/users/me
  fastify.patch('/me', UsersController.updateProfile);

  // DELETE /api/v1/users/me
  fastify.delete('/me', { preHandler: accountOpLimiter }, UsersController.deleteAccount);

  // GET /api/v1/users/export
  fastify.get('/export', { preHandler: accountOpLimiter }, UsersController.exportAccount);
};
