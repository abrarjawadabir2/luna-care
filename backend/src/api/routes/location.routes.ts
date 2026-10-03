import type { FastifyPluginAsync } from 'fastify';
import { LocationController } from '../controllers/index.js';

export const locationRoutes: FastifyPluginAsync = async (fastify) => {
  // GET /api/v1/location/categories — Publicly accessible categories
  fastify.get('/categories', LocationController.getCategories);

  // POST /api/v1/location/search-url — Build privacy-preserving Google Maps query URL
  fastify.post('/search-url', LocationController.searchUrl);
};
