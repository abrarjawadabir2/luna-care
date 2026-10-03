import type { FastifyPluginAsync } from 'fastify';
import { AuthController } from '../controllers/index.js';
import { createRateLimiter } from '../middleware/rateLimit.js';

export const authRoutes: FastifyPluginAsync = async (fastify) => {
  // Stricter rate limits for authentication endpoints to prevent brute force
  const loginLimiter = createRateLimiter({
    maxLimit: 5,
    windowMs: 10 * 60 * 1000,
    keyPrefix: 'auth_login'
  });

  const registerLimiter = createRateLimiter({
    maxLimit: 10,
    windowMs: 15 * 60 * 1000,
    keyPrefix: 'auth_register'
  });

  const otpRequestLimiter = createRateLimiter({
    maxLimit: 3,
    windowMs: 10 * 60 * 1000,
    keyPrefix: 'otp_request'
  });

  const otpVerifyLimiter = createRateLimiter({
    maxLimit: 5,
    windowMs: 10 * 60 * 1000,
    keyPrefix: 'otp_verify'
  });

  // POST /api/v1/auth/register
  fastify.post('/register', { preHandler: registerLimiter }, AuthController.register);

  // POST /api/v1/auth/login
  fastify.post('/login', { preHandler: loginLimiter }, AuthController.login);

  // POST /api/v1/auth/otp/request
  fastify.post('/otp/request', { preHandler: otpRequestLimiter }, AuthController.requestOtp);

  // POST /api/v1/auth/otp/verify
  fastify.post('/otp/verify', { preHandler: otpVerifyLimiter }, AuthController.verifyOtp);

  // POST /api/v1/auth/password-reset
  fastify.post('/password-reset', AuthController.requestPasswordReset);
};
