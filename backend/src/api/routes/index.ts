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

export const apiV1Routes: FastifyPluginAsync = async (fastify) => {
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
