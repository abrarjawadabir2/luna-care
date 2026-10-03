import type { FastifyRequest, FastifyReply } from 'fastify';
import { purposeSeparatedHash } from '../../security/index.js';
import { AuditService } from '../../audit/audit.service.js';

interface RateLimitBucket {
  count: number;
  resetAt: number;
}

export class MemoryRateLimiter {
  private buckets = new Map<string, RateLimitBucket>();

  /**
   * Check and consume rate limit atomically.
   * Returns true if allowed, false if limit exceeded.
   */
  consume(key: string, maxLimit: number, windowMs: number): { allowed: boolean; remaining: number; resetInSec: number } {
    const now = Date.now();
    let bucket = this.buckets.get(key);

    if (!bucket || bucket.resetAt <= now) {
      bucket = { count: 1, resetAt: now + windowMs };
      this.buckets.set(key, bucket);
      return { allowed: true, remaining: maxLimit - 1, resetInSec: Math.ceil(windowMs / 1000) };
    }

    if (bucket.count >= maxLimit) {
      return { allowed: false, remaining: 0, resetInSec: Math.ceil((bucket.resetAt - now) / 1000) };
    }

    bucket.count += 1;
    return {
      allowed: true,
      remaining: maxLimit - bucket.count,
      resetInSec: Math.ceil((bucket.resetAt - now) / 1000)
    };
  }

  // Cleanup expired buckets
  cleanupExpired(): number {
    const now = Date.now();
    let cleaned = 0;
    for (const [key, bucket] of this.buckets.entries()) {
      if (bucket.resetAt <= now) {
        this.buckets.delete(key);
        cleaned++;
      }
    }
    return cleaned;
  }

  // Clear memory for testing
  reset() {
    this.buckets.clear();
  }
}

export const rateLimiter = new MemoryRateLimiter();

export function createRateLimiter(options: {
  maxLimit: number;
  windowMs: number;
  keyPrefix: string;
  extractKey?: (req: FastifyRequest) => string;
}) {
  return async (request: FastifyRequest, reply: FastifyReply) => {
    // Hash client IP with purpose-separated HMAC to prevent plaintext IP storage
    const ip = request.ip || '127.0.0.1';
    const hashedIp = purposeSeparatedHash('ip_rate_limit', ip);

    // Derive identifier: custom extractor or hashed IP
    const subKey = options.extractKey ? options.extractKey(request) : hashedIp;
    const bucketKey = `${options.keyPrefix}:${subKey}`;

    const result = rateLimiter.consume(bucketKey, options.maxLimit, options.windowMs);

    reply.header('x-ratelimit-limit', options.maxLimit);
    reply.header('x-ratelimit-remaining', result.remaining);
    reply.header('x-ratelimit-reset', result.resetInSec);

    if (!result.allowed) {
      await AuditService.recordEvent({
        eventType: 'security.rate_limit.triggered',
        userId: request.user?.id,
        requestId: request.requestId,
        resourceType: 'rate_limit',
        resourceId: options.keyPrefix,
        result: 'denied',
        reasonCode: 'LIMIT_EXCEEDED',
        metadata: { keyPrefix: options.keyPrefix, retryAfter: result.resetInSec }
      });

      return reply.status(429).send({
        error: {
          code: 'RATE_LIMIT_EXCEEDED',
          message: `Too many requests. Please try again in ${result.resetInSec} seconds.`,
          requestId: request.requestId,
          retryAfter: result.resetInSec
        }
      });
    }
  };
}
