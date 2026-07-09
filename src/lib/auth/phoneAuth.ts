import { assertAuthActionAllowed, resetAuthRateLimit } from './authSecurity';
import { supabase } from '../supabase';
import type { Session, User } from '@supabase/supabase-js';

export type PhoneAuthErrorCode =
  | "INVALID_PHONE"
  | "INVALID_CODE_FORMAT"
  | "OTP_REQUEST_FAILED"
  | "OTP_VERIFICATION_FAILED"
  | "RATE_LIMITED"
  | "SECURITY_CHECK_FAILED";

export interface PhoneAuthFailure {
  ok: false;
  code: PhoneAuthErrorCode;
  message: string;
  retryAfterSeconds?: number;
  captchaRequired?: boolean;
}

export interface PhoneOtpRequestSuccess {
  ok: true;
  maskedPhone: string;
  captchaRequired: boolean;
}

export interface PhoneOtpVerificationSuccess {
  ok: true;
  user: User;
  session: Session;
}

export function normalizeToE164(phone: string): string {
  const trimmed = phone.trim();
  const normalized = trimmed.replace(/[\s\(\)\.\-]/g, '');
  if (!/^\+[1-9]\d{7,14}$/.test(normalized)) {
    throw new Error('INVALID_PHONE');
  }
  return normalized;
}

export function maskPhone(phone: string): string {
  try {
    const normalized = normalizeToE164(phone);
    // e.g. +60 •••• 1234
    const prefix = normalized.substring(0, 3);
    const suffix = normalized.substring(normalized.length - 4);
    return `${prefix} •••• ${suffix}`;
  } catch (e) {
    return ""; // fail safe
  }
}

const GENERIC_OTP_ERROR = "We could not send a verification code. Please wait and try again.";
const GENERIC_VERIFY_ERROR = "The verification code is invalid or expired. Request a new code and try again.";

export async function requestPhoneOtp(phone: string): Promise<PhoneOtpRequestSuccess | PhoneAuthFailure> {
  let normalizedPhone: string;
  try {
    normalizedPhone = normalizeToE164(phone);
  } catch (e) {
    return { ok: false, code: "INVALID_PHONE", message: "Enter a valid phone number with its country code." };
  }

  const decision = await assertAuthActionAllowed("phone_otp_request", normalizedPhone);
  if (!decision.allowed) {
    return {
      ok: false,
      code: "RATE_LIMITED",
      message: "Too many attempts. Please wait before trying again.",
      retryAfterSeconds: decision.retryAfterSeconds,
      captchaRequired: decision.captchaRequired
    };
  }

  try {
    const { error } = await supabase.auth.signInWithOtp({
      phone: normalizedPhone,
      options: { shouldCreateUser: true }
    });

    if (error) {
      return { ok: false, code: "OTP_REQUEST_FAILED", message: GENERIC_OTP_ERROR };
    }

    return {
      ok: true,
      maskedPhone: maskPhone(normalizedPhone),
      captchaRequired: false
    };
  } catch (e) {
    return { ok: false, code: "OTP_REQUEST_FAILED", message: GENERIC_OTP_ERROR };
  }
}

export async function verifyPhoneOtp(phone: string, token: string): Promise<PhoneOtpVerificationSuccess | PhoneAuthFailure> {
  let normalizedPhone: string;
  try {
    normalizedPhone = normalizeToE164(phone);
  } catch (e) {
    return { ok: false, code: "INVALID_PHONE", message: "Enter a valid phone number with its country code." };
  }

  const normalizedToken = token.replace(/\s/g, '');
  if (!/^\d{6}$/.test(normalizedToken)) {
    return { ok: false, code: "INVALID_CODE_FORMAT", message: GENERIC_VERIFY_ERROR };
  }

  const decision = await assertAuthActionAllowed("phone_otp_verify", normalizedPhone);
  if (!decision.allowed) {
    return {
      ok: false,
      code: "RATE_LIMITED",
      message: "Too many attempts. Please wait before trying again.",
      retryAfterSeconds: decision.retryAfterSeconds,
      captchaRequired: decision.captchaRequired
    };
  }

  try {
    const { data, error } = await supabase.auth.verifyOtp({
      phone: normalizedPhone,
      token: normalizedToken,
      type: "sms"
    });

    if (error || !data.user || !data.session) {
      return { ok: false, code: "OTP_VERIFICATION_FAILED", message: GENERIC_VERIFY_ERROR };
    }

    await resetAuthRateLimit(normalizedPhone);

    return {
      ok: true,
      user: data.user,
      session: data.session
    };
  } catch (e) {
    return { ok: false, code: "OTP_VERIFICATION_FAILED", message: GENERIC_VERIFY_ERROR };
  }
}
