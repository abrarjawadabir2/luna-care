import type { FastifyRequest, FastifyReply } from 'fastify';
import { AuthService } from '../../auth/index.js';
import {
  registerSchema,
  loginSchema,
  otpRequestSchema,
  otpVerifySchema,
  passwordResetRequestSchema
} from '../schemas/index.js';
import { AuditService } from '../../audit/audit.service.js';

export class AuthController {
  static async register(request: FastifyRequest, reply: FastifyReply) {
    const input = registerSchema.parse(request.body);
    try {
      const result = await AuthService.register(input);
      return reply.status(201).send(result);
    } catch (err: unknown) {
      if (err instanceof Error && err.message === 'EMAIL_ALREADY_EXISTS') {
        return reply.status(400).send({
          error: {
            code: 'REGISTRATION_FAILED',
            message: 'Unable to register account with the provided details.',
            requestId: request.requestId
          }
        });
      }
      throw err;
    }
  }

  static async login(request: FastifyRequest, reply: FastifyReply) {
    const input = loginSchema.parse(request.body);
    try {
      const result = await AuthService.login(input);
      return reply.status(200).send(result);
    } catch (err: unknown) {
      if (err instanceof Error && err.message === 'INVALID_CREDENTIALS') {
        return reply.status(401).send({
          error: {
            code: 'INVALID_CREDENTIALS',
            message: 'Incorrect email or password.',
            requestId: request.requestId
          }
        });
      }
      throw err;
    }
  }

  static async requestOtp(request: FastifyRequest, reply: FastifyReply) {
    const input = otpRequestSchema.parse(request.body);
    const result = await AuthService.requestOtp(input.phone);
    return reply.status(200).send(result);
  }

  static async verifyOtp(request: FastifyRequest, reply: FastifyReply) {
    const input = otpVerifySchema.parse(request.body);
    try {
      const result = await AuthService.verifyOtp(input.phone, input.code);
      return reply.status(200).send(result);
    } catch (err: unknown) {
      if (err instanceof Error && (err.message === 'INVALID_OR_EXPIRED_OTP' || err.message === 'TOO_MANY_FAILED_ATTEMPTS')) {
        return reply.status(400).send({
          error: {
            code: 'INVALID_OTP',
            message: 'The verification code is invalid or expired. Request a new code and try again.',
            requestId: request.requestId
          }
        });
      }
      throw err;
    }
  }

  static async requestPasswordReset(request: FastifyRequest, reply: FastifyReply) {
    const input = passwordResetRequestSchema.parse(request.body);
    await AuditService.recordEvent({
      eventType: 'auth.password_reset.request',
      resourceType: 'user',
      result: 'success',
      metadata: { emailDomain: input.email.split('@')[1] }
    });

    return reply.status(200).send({
      message: 'If that email is registered, you will receive a reset link.'
    });
  }
}
