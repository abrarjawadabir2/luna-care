import type { FastifyError, FastifyRequest, FastifyReply } from 'fastify';
import { ZodError } from 'zod';
import { logger } from '../../utils/logger.js';

export function errorHandler(error: FastifyError, request: FastifyRequest, reply: FastifyReply) {
  const requestId = request.requestId || 'unknown';

  // 1. Zod validation errors
  if (error instanceof ZodError) {
    logger.warn('validation.failed', {
      requestId,
      fieldCount: error.errors.length,
      fields: error.errors.map(e => e.path.join('.'))
    });

    return reply.status(400).send({
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Request payload validation failed. Check field types and constraints.',
        details: error.errors.map(e => ({
          field: e.path.join('.'),
          message: e.message
        })),
        requestId
      }
    });
  }

  // 2. Fastify schema or body-size errors (FST_ERR_CTP_BODY_TOO_LARGE, etc.)
  if (error.statusCode === 413 || error.code === 'FST_ERR_CTP_BODY_TOO_LARGE') {
    return reply.status(413).send({
      error: {
        code: 'PAYLOAD_TOO_LARGE',
        message: 'Request payload exceeds maximum allowed size.',
        requestId
      }
    });
  }

  // 3. Known client errors (4xx)
  if (error.statusCode && error.statusCode >= 400 && error.statusCode < 500) {
    logger.warn('request.client_error', {
      requestId,
      statusCode: error.statusCode,
      code: error.code
    });

    return reply.status(error.statusCode).send({
      error: {
        code: error.code || 'BAD_REQUEST',
        message: error.message || 'Unable to process the request.',
        requestId
      }
    });
  }

  // 4. Server internal error (500) - NEVER leak stack trace or internal message to client
  logger.error('server.internal_error', {
    requestId,
    errorName: error.name,
    errorMessage: error.message,
    errorCode: error.code
  });

  return reply.status(500).send({
    error: {
      code: 'INTERNAL_SERVER_ERROR',
      message: 'An unexpected error occurred. Please try again later.',
      requestId
    }
  });
}
