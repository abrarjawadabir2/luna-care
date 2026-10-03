import { z } from 'zod';

export const registerSchema = z.object({
  email: z.string().email().max(255),
  password: z.string().min(8, 'Password must be at least 8 characters').max(128),
  displayName: z.string().min(1).max(100),
  phone: z.string().regex(/^\+?[1-9]\d{1,14}$/, 'Invalid international phone number').optional()
}).strict(); // Reject unexpected fields

export const loginSchema = z.object({
  email: z.string().email().max(255),
  password: z.string().min(1).max(128)
}).strict();

export const otpRequestSchema = z.object({
  phone: z.string().regex(/^\+?[1-9]\d{1,14}$/, 'Invalid international phone number format')
}).strict();

export const otpVerifySchema = z.object({
  phone: z.string().regex(/^\+?[1-9]\d{1,14}$/, 'Invalid international phone number format'),
  code: z.string().length(6, 'Verification code must be exactly 6 digits').regex(/^\d{6}$/, 'Code must be digits')
}).strict();

export const passwordResetRequestSchema = z.object({
  email: z.string().email().max(255)
}).strict();

export const oauthVerifySchema = z.object({
  provider: z.enum(['google', 'facebook']),
  idToken: z.string().min(10)
}).strict();
