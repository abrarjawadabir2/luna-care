import type { AuthenticatedUser } from '../api/middleware/auth.js';

declare module 'fastify' {
  interface FastifyRequest {
    requestId: string;
    user?: AuthenticatedUser;
  }
}
