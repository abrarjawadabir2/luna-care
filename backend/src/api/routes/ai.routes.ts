import type { FastifyPluginAsync } from 'fastify';
import { AiController } from '../controllers/index.js';
import { requireAuth } from '../middleware/auth.js';
import { createRateLimiter } from '../middleware/rateLimit.js';

export const aiRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.addHook('preHandler', requireAuth);

  const aiLimiter = createRateLimiter({
    maxLimit: 15,
    windowMs: 60 * 1000,
    keyPrefix: 'ai_chat',
    extractKey: (req) => req.user?.id || 'anonymous'
  });

  // POST /api/v1/ai/chat
  fastify.post('/chat', { preHandler: aiLimiter }, AiController.processChat);
};
