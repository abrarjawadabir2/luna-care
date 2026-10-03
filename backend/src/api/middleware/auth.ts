import type { FastifyRequest, FastifyReply } from 'fastify';
import jwt from 'jsonwebtoken';
import { getEnv } from '../../config/env.js';
import type { UserRole } from '../../database/index.js';
import { AuditService } from '../../audit/audit.service.js';

export interface AuthenticatedUser {
  id: string;
  email: string;
  role: UserRole;
  isPremium: boolean;
}

declare module 'fastify' {
  interface FastifyRequest {
    user?: AuthenticatedUser;
  }
}

const JWT_ISSUER = 'lunacare-backend';
const JWT_AUDIENCE = 'lunacare-client';

export interface TokenPayload {
  sub: string;
  email: string;
  role: UserRole;
  isPremium: boolean;
  iss: string;
  aud: string;
}

export function signUserToken(user: { id: string; email: string; role: UserRole; isPremium: boolean }, expiresIn = '1h'): string {
  const env = getEnv();
  return jwt.sign(
    {
      sub: user.id,
      email: user.email,
      role: user.role,
      isPremium: user.isPremium
    },
    env.JWT_SECRET,
    {
      issuer: JWT_ISSUER,
      audience: JWT_AUDIENCE,
      expiresIn: expiresIn as unknown as number
    }
  );
}

export async function requireAuth(request: FastifyRequest, reply: FastifyReply) {
  const authHeader = request.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return reply.status(401).send({
      error: {
        code: 'UNAUTHORIZED',
        message: 'Authentication required. Bearer token missing.',
        requestId: request.requestId
      }
    });
  }

  const token = authHeader.substring(7).trim();
  const env = getEnv();

  try {
    const decoded = jwt.verify(token, env.JWT_SECRET, {
      issuer: JWT_ISSUER,
      audience: JWT_AUDIENCE
    }) as TokenPayload;

    request.user = {
      id: decoded.sub,
      email: decoded.email,
      role: decoded.role,
      isPremium: decoded.isPremium
    };
  } catch (err) {
    return reply.status(401).send({
      error: {
        code: 'INVALID_TOKEN',
        message: 'Invalid, expired, or tampered authentication token.',
        requestId: request.requestId
      }
    });
  }
}

export function requireRole(allowedRoles: UserRole[]) {
  return async (request: FastifyRequest, reply: FastifyReply) => {
    if (!request.user) {
      return reply.status(401).send({
        error: {
          code: 'UNAUTHORIZED',
          message: 'Authentication required.',
          requestId: request.requestId
        }
      });
    }

    if (!allowedRoles.includes(request.user.role)) {
      await AuditService.recordEvent({
        eventType: 'security.authorization.denied',
        userId: request.user.id,
        requestId: request.requestId,
        resourceType: 'endpoint',
        result: 'denied',
        reasonCode: 'INSUFFICIENT_ROLE',
        metadata: { userRole: request.user.role, allowedRoles }
      });

      return reply.status(403).send({
        error: {
          code: 'FORBIDDEN',
          message: 'Insufficient privileges for this action.',
          requestId: request.requestId
        }
      });
    }
  };
}

export function requireOwnership(resourceOwnerId: string, request: FastifyRequest, reply: FastifyReply): boolean {
  if (!request.user) {
    reply.status(401).send({
      error: { code: 'UNAUTHORIZED', message: 'Authentication required.', requestId: request.requestId }
    });
    return false;
  }

  // Super admins may have administrative access, but regular users CANNOT access other users' data
  const isSuperAdmin = request.user.role === 'SUPER_ADMIN';
  const isOwner = request.user.id === resourceOwnerId;

  if (!isOwner && !isSuperAdmin) {
    AuditService.recordEvent({
      eventType: 'security.authorization.denied',
      userId: request.user.id,
      requestId: request.requestId,
      resourceType: 'user_record',
      resourceId: resourceOwnerId,
      result: 'denied',
      reasonCode: 'CROSS_USER_ACCESS_ATTEMPT',
      metadata: { attemptedOwnerId: resourceOwnerId }
    });

    reply.status(403).send({
      error: {
        code: 'FORBIDDEN',
        message: 'Access denied: Cannot access or modify records owned by another user.',
        requestId: request.requestId
      }
    });
    return false;
  }
  return true;
}
