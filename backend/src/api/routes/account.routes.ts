import type { FastifyPluginAsync } from 'fastify';
import { UsersController } from '../controllers/index.js';
import { requireAuth } from '../middleware/auth.js';
import { createRateLimiter } from '../middleware/rateLimit.js';

export const accountRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.addHook('preHandler', requireAuth);

  const accountOpLimiter = createRateLimiter({
    maxLimit: 5,
    windowMs: 60 * 60 * 1000,
    keyPrefix: 'account_op',
    extractKey: (req) => req.user?.id || 'anonymous'
  });

  // GET /api/v1/account/export
  fastify.get('/export', { preHandler: accountOpLimiter }, UsersController.exportAccount);

  // DELETE /api/v1/account
  fastify.delete('/', { preHandler: accountOpLimiter }, UsersController.deleteAccount);
  fastify.delete('/me', { preHandler: accountOpLimiter }, UsersController.deleteAccount);
};
