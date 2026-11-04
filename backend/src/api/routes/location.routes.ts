import type { FastifyPluginAsync } from 'fastify';
import { LocationController } from '../controllers/index.js';
import { createRateLimiter } from '../middleware/rateLimit.js';

export const locationRoutes: FastifyPluginAsync = async (fastify) => {
  const locationSearchLimiter = createRateLimiter({
    maxLimit: 60,
    windowMs: 60 * 1000,
    keyPrefix: 'location_search'
  });

  // GET /api/v1/location/categories — Publicly accessible categories
  fastify.get('/categories', LocationController.getCategories);

  // POST /api/v1/location/search-url — Build privacy-preserving Google Maps query URL
  fastify.post('/search-url', { preHandler: locationSearchLimiter }, LocationController.searchUrl);
};
