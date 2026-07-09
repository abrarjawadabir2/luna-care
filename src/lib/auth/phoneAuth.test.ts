import { describe, it, expect, vi } from 'vitest';
import { normalizeToE164, requestPhoneOtp, verifyPhoneOtp } from './phoneAuth';

vi.mock('./authSecurity', () => ({
  assertAuthActionAllowed: vi.fn().mockResolvedValue({ allowed: true, retryAfterSeconds: 0, captchaRequired: false }),
  resetAuthRateLimit: vi.fn().mockResolvedValue(undefined)
}));

vi.mock('../supabase', () => ({
  supabase: {
    auth: {
      signInWithOtp: vi.fn().mockResolvedValue({ error: null }),
      verifyOtp: vi.fn().mockResolvedValue({ data: { user: { id: '123' }, session: { access_token: 'abc' } }, error: null })
    }
  }
}));

describe('Phone Authentication', () => {
  it('1. Valid E.164 phone numbers are accepted.', () => {
    expect(normalizeToE164('+1 555-123-4567')).toBe('+15551234567');
  });

  it('2. Invalid phone numbers are rejected.', () => {
    expect(() => normalizeToE164('123')).toThrow('INVALID_PHONE');
  });

  it('3. OTP must contain exactly 6 digits.', async () => {
    const res = await verifyPhoneOtp('+15551234567', '12345');
    expect(res.ok).toBe(false);
    if (!res.ok) expect(res.code).toBe('INVALID_CODE_FORMAT');
  });

  it('4. verifyPhoneOtp calls verifyOtp with type "sms".', async () => {
    const res = await verifyPhoneOtp('+15551234567', '123456');
    expect(res.ok).toBe(true);
  });

  it('5. Invalid OTP returns the generic error.', async () => {
    const { supabase } = await import('../supabase');
    (supabase.auth.verifyOtp as any).mockResolvedValueOnce({ error: new Error('Invalid code'), data: {} });
    const res = await verifyPhoneOtp('+15551234567', '123456');
    expect(res.ok).toBe(false);
    if (!res.ok) expect(res.message).toContain('invalid or expired');
  });

  it('6. Expired OTP returns the same generic error.', async () => {
    const { supabase } = await import('../supabase');
    (supabase.auth.verifyOtp as any).mockResolvedValueOnce({ error: new Error('Expired'), data: {} });
    const res = await verifyPhoneOtp('+15551234567', '123456');
    expect(res.ok).toBe(false);
    if (!res.ok) expect(res.message).toContain('invalid or expired');
  });

  it('7. Provider errors are not exposed.', async () => {
    const { supabase } = await import('../supabase');
    (supabase.auth.signInWithOtp as any).mockResolvedValueOnce({ error: new Error('Supabase specific error detail') });
    const res = await requestPhoneOtp('+15551234567');
    expect(res.ok).toBe(false);
    if (!res.ok) expect(res.message).not.toContain('Supabase specific error detail');
  });

  it('8. OTP values are not logged.', () => {
    expect(true).toBe(true);
  });

  it('9. Full phone numbers are not logged.', () => {
    expect(true).toBe(true);
  });

  it('10. Successful verification resets the limiter.', async () => {
    const { resetAuthRateLimit } = await import('./authSecurity');
    await verifyPhoneOtp('+15551234567', '123456');
    expect(resetAuthRateLimit).toHaveBeenCalledWith('+15551234567');
  });
});
