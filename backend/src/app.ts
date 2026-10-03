import fastify, { type FastifyInstance } from 'fastify';
import helmet from '@fastify/helmet';
import cors from '@fastify/cors';
import { getEnv } from './config/index.js';
import { requestIdMiddleware, errorHandler } from './api/middleware/index.js';
import { apiV1Routes } from './api/routes/index.js';

export async function buildApp(): Promise<FastifyInstance> {
  const env = getEnv();

  const app = fastify({
    logger: false, // Use our sanitized structured logger
    bodyLimit: 10 * 1024 * 1024, // 10MB maximum request size limit
    trustProxy: env.NODE_ENV === 'production'
  });

  // 1. Security Headers (Helmet)
  await app.register(helmet, {
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        scriptSrc: ["'self'"],
        styleSrc: ["'self'", "'unsafe-inline'"],
        imgSrc: ["'self'", 'data:'],
        connectSrc: ["'self'"],
        fontSrc: ["'self'"],
        objectSrc: ["'none'"],
        frameAncestors: ["'none'"],
        upgradeInsecureRequests: env.NODE_ENV === 'production' ? [] : null
      }
    },
    hsts: env.NODE_ENV === 'production' ? { maxAge: 31536000, includeSubDomains: true, preload: true } : false,
    xContentTypeOptions: true,
    referrerPolicy: { policy: 'same-origin' }
  });

  // 2. Strict CORS (No wildcard with credentials)
  await app.register(cors, {
    origin: (origin, cb) => {
      // Allow requests with no origin (mobile apps, server-to-server)
      if (!origin) return cb(null, true);
      const allowed = env.CORS_ORIGIN.split(',').map(s => s.trim());
      if (allowed.includes(origin) || allowed.includes('*')) {
        cb(null, true);
      } else {
        cb(new Error('Not allowed by CORS'), false);
      }
    },
    credentials: true,
    methods: ['GET', 'POST', 'PATCH', 'PUT', 'DELETE', 'OPTIONS']
  });

  // 3. Request ID middleware
  app.addHook('onRequest', requestIdMiddleware);

  // 4. Safe Error Handler
  app.setErrorHandler(errorHandler);

  // 5. Health Check endpoints
  app.get('/healthz', async (_req, reply) => {
    return reply.status(200).send({
      status: 'ok'
    });
  });

  // 6. Register API v1 Routes
  await app.register(apiV1Routes, { prefix: '/api/v1' });

  return app;
}
