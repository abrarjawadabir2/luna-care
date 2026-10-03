import crypto from 'node:crypto';
import type { FastifyRequest, FastifyReply } from 'fastify';

declare module 'fastify' {
  interface FastifyRequest {
    requestId: string;
  }
}

export async function requestIdMiddleware(request: FastifyRequest, reply: FastifyReply) {
  const incomingId = request.headers['x-request-id'];
  const requestId = typeof incomingId === 'string' && incomingId.trim().length > 0
    ? incomingId.trim().substring(0, 64)
    : crypto.randomUUID();

  request.requestId = requestId;
  reply.header('x-request-id', requestId);
}
