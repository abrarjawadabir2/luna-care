import type { FastifyPluginAsync } from 'fastify';
import { UploadsController } from '../controllers/index.js';
import { requireAuth } from '../middleware/auth.js';
import { createRateLimiter } from '../middleware/rateLimit.js';

export const uploadsRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.addHook('preHandler', requireAuth);

  const uploadLimiter = createRateLimiter({
    maxLimit: 10,
    windowMs: 60 * 60 * 1000,
    keyPrefix: 'user_upload',
    extractKey: (req) => req.user?.id || 'anonymous'
  });

  // POST /api/v1/uploads/inspect
  fastify.post('/inspect', { preHandler: uploadLimiter }, UploadsController.inspect);
};
