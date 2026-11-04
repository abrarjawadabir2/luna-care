import type { FastifyPluginAsync } from 'fastify';
import { UsersController } from '../controllers/index.js';
import { requireAuth, optionalAuth } from '../middleware/auth.js';
import { createRateLimiter } from '../middleware/rateLimit.js';

export const usersRoutes: FastifyPluginAsync = async (fastify) => {
  // Rate limiter for account-level sensitive operations (e.g. deletion, export)
  const accountOpLimiter = createRateLimiter({
    maxLimit: 5,
    windowMs: 60 * 60 * 1000,
    keyPrefix: 'account_op',
    extractKey: (req, hashedIp) => req.user?.id || hashedIp
  });

  // Rate limiter for username availability checks (Req 94)
  // Anonymous: limited per hashed IP (e.g. 60/min)
  // Authenticated: limited per user + hashed IP
  const usernameAvailabilityLimiter = createRateLimiter({
    maxLimit: 60,
    windowMs: 60 * 1000,
    keyPrefix: 'username_avail',
    extractKey: (req, hashedIp) => {
      const user = req.user;
      return user?.id ? `auth_${user.id}_${hashedIp}` : hashedIp;
    }
  });

  // Rate limiter for public profile lookup by username to deter automated scraping
  const publicProfileLimiter = createRateLimiter({
    maxLimit: 60,
    windowMs: 60 * 1000,
    keyPrefix: 'public_profile'
  });

  // 1. GET /api/v1/users/username-availability?username=... (Public / Authenticated with rate limiting)
  fastify.get('/username-availability', {
    preHandler: [optionalAuth, usernameAvailabilityLimiter]
  }, UsersController.checkAvailability);

  // 2. GET /api/v1/users/me (Private authenticated profile)
  fastify.get('/me', { preHandler: requireAuth }, UsersController.getProfile);

  // 3. PATCH /api/v1/users/me (Private authenticated profile update)
  fastify.patch('/me', { preHandler: requireAuth }, UsersController.updateProfile);

  // 4. DELETE /api/v1/users/me (Account deletion)
  fastify.delete('/me', { preHandler: [requireAuth, accountOpLimiter] }, UsersController.deleteAccount);

  // 5. GET /api/v1/users/export (Account export)
  fastify.get('/export', { preHandler: [requireAuth, accountOpLimiter] }, UsersController.exportAccount);

  // 6. GET /api/v1/users/:username (Public allowlisted profile lookup, Req 92, 110)
  fastify.get('/:username', { preHandler: publicProfileLimiter }, UsersController.getPublicProfile);
};
