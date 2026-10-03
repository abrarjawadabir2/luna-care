import crypto from 'node:crypto';
import { getEnv } from '../config/env.js';

export type HashPurpose =
  | 'email_rate_limit'
  | 'phone_rate_limit'
  | 'ip_rate_limit'
  | 'device_rate_limit'
  | 'audit_reference'
  | 'token_fingerprint';

/**
 * Purpose-separated HMAC-SHA256 hashing for security identifiers.
 * Guarantees that a hash for one purpose cannot be correlated with another purpose.
 */
export function purposeSeparatedHash(purpose: HashPurpose, value: string): string {
  const secret = getEnv().SERVER_SECRET;
  const normalized = value.trim().toLowerCase();
  const payload = `${purpose}:${normalized}`;
  return crypto.createHmac('sha256', secret).update(payload).digest('hex');
}

/**
 * Constant-time string comparison to defend against timing attacks.
 */
export function safeTimingCompare(a: string, b: string): boolean {
  try {
    const bufA = Buffer.from(a, 'utf-8');
    const bufB = Buffer.from(b, 'utf-8');
    if (bufA.length !== bufB.length) {
      return false;
    }
    return crypto.timingSafeEqual(bufA, bufB);
  } catch {
    return false;
  }
}

/**
 * Generates a cryptographically secure 6-digit numeric OTP code.
 */
export function generateSecureOtp(): string {
  const code = crypto.randomInt(100000, 1000000);
  return code.toString();
}

/**
 * Generates a cryptographically random token string (hex).
 */
export function generateSecureToken(bytes = 32): string {
  return crypto.randomBytes(bytes).toString('hex');
}
