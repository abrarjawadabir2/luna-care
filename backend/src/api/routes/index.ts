import type { FastifyPluginAsync } from 'fastify';
import { healthRoutes } from './health.routes.js';
import { authRoutes } from './auth.routes.js';
import { profileRoutes } from './profile.routes.js';
import { accountRoutes } from './account.routes.js';
import { usersRoutes } from './users.routes.js';
import { periodsRoutes } from './periods.routes.js';
import { moodsRoutes } from './moods.routes.js';
import { journalRoutes } from './journal.routes.js';
import { aiRoutes } from './ai.routes.js';
import { locationRoutes } from './location.routes.js';
import { uploadsRoutes } from './uploads.routes.js';
import { auditRoutes } from './audit.routes.js';
import { UsersController } from '../controllers/index.js';
import { requireAuth } from '../middleware/auth.js';

export const apiV1Routes: FastifyPluginAsync = async (fastify) => {
  // Direct routes for authenticated user private profile and public username profile (Req 92, 110)
  fastify.get('/me', { preHandler: requireAuth }, UsersController.getProfile);
  fastify.patch('/me', { preHandler: requireAuth }, UsersController.updateProfile);
  fastify.get('/u/:username', UsersController.getPublicProfile);

  await fastify.register(healthRoutes, { prefix: '/health' });
  await fastify.register(authRoutes, { prefix: '/auth' });
  await fastify.register(profileRoutes, { prefix: '/profile' });
  await fastify.register(accountRoutes, { prefix: '/account' });
  await fastify.register(usersRoutes, { prefix: '/users' });
  await fastify.register(periodsRoutes, { prefix: '/periods' });
  await fastify.register(moodsRoutes, { prefix: '/moods' });
  await fastify.register(journalRoutes, { prefix: '/journal' });
  await fastify.register(aiRoutes, { prefix: '/ai' });
  await fastify.register(locationRoutes, { prefix: '/location' });
  await fastify.register(uploadsRoutes, { prefix: '/uploads' });
  await fastify.register(auditRoutes, { prefix: '/audit' });
};
