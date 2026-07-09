export type AuthRateLimitAction = "phone_otp_request" | "phone_otp_verify";

export interface AuthRateLimitDecision {
  allowed: boolean;
  retryAfterSeconds: number;
  captchaRequired: boolean;
}

export class AuthRateLimitError extends Error {
  retryAfterSeconds: number;
  captchaRequired: boolean;

  constructor(message: string, retryAfterSeconds: number, captchaRequired: boolean) {
    super(message);
    this.name = "AuthRateLimitError";
    this.retryAfterSeconds = retryAfterSeconds;
    this.captchaRequired = captchaRequired;
  }
}

// In a real application, we would use a robust crypto library to hash the identifier.
// Since we are in the browser, we use the Web Crypto API.
async function hashIdentifier(identifier: string): Promise<string> {
  const msgBuffer = new TextEncoder().encode(identifier);
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

// Local rate limit state for UX purposes. The server is the authoritative source.
const localRateLimits: Record<string, { count: number, firstAttemptAt: number, blockedUntil: number }> = {};

function checkLocalRateLimit(identifierHash: string, action: AuthRateLimitAction): AuthRateLimitDecision | null {
  const now = Date.now();
  const key = `${action}_${identifierHash}`;
  
  if (!localRateLimits[key]) {
    localRateLimits[key] = { count: 0, firstAttemptAt: now, blockedUntil: 0 };
  }
  
  const state = localRateLimits[key];
  
  // 10 minutes sliding window
  if (now - state.firstAttemptAt > 10 * 60 * 1000) {
    state.count = 0;
    state.firstAttemptAt = now;
    state.blockedUntil = 0;
  }
  
  if (state.blockedUntil > now) {
    return {
      allowed: false,
      retryAfterSeconds: Math.ceil((state.blockedUntil - now) / 1000),
      captchaRequired: state.count >= (action === "phone_otp_request" ? 5 : 8)
    };
  }
  
  state.count += 1;
  
  if (action === "phone_otp_request" && state.count > 3) {
    state.blockedUntil = now + 10 * 60 * 1000;
    return { allowed: false, retryAfterSeconds: 10 * 60, captchaRequired: true };
  }
  
  if (action === "phone_otp_verify" && state.count > 5) {
    state.blockedUntil = now + 15 * 60 * 1000;
    return { allowed: false, retryAfterSeconds: 15 * 60, captchaRequired: true };
  }
  
  return null;
}

export async function assertAuthActionAllowed(
  action: AuthRateLimitAction,
  normalizedPhone: string
): Promise<AuthRateLimitDecision> {
  const identifierHash = await hashIdentifier(normalizedPhone);
  
  // 1. Browser-side local guard
  const localDecision = checkLocalRateLimit(identifierHash, action);
  if (localDecision && !localDecision.allowed) {
    return localDecision;
  }
  
  // 2. Call Supabase Edge Function
  try {
    const { supabase } = await import('../supabase');
    const { data, error } = await supabase.functions.invoke('auth-rate-limit', {
      body: {
        operation: 'consume',
        action,
        identifierHash
      }
    });

    if (error) {
      // Fail closed if the server-side security service is unavailable
      return { allowed: false, retryAfterSeconds: 300, captchaRequired: false };
    }
    
    if (data && !data.allowed) {
      // Sync local state if server blocked us
      const key = `${action}_${identifierHash}`;
      localRateLimits[key] = {
        count: (action === "phone_otp_request" ? 3 : 5) + 1,
        firstAttemptAt: Date.now(),
        blockedUntil: Date.now() + (data.retryAfterSeconds * 1000)
      };
      
      return {
        allowed: false,
        retryAfterSeconds: data.retryAfterSeconds || 300,
        captchaRequired: data.captchaRequired || false
      };
    }

    return { allowed: true, retryAfterSeconds: 0, captchaRequired: false };
  } catch (e) {
    // Fail closed if the server-side security service is unavailable
    return { allowed: false, retryAfterSeconds: 300, captchaRequired: false };
  }
}

export async function resetAuthRateLimit(normalizedPhone: string): Promise<void> {
  const identifierHash = await hashIdentifier(normalizedPhone);
  
  // Clear local limiter state
  const reqKey = `phone_otp_request_${identifierHash}`;
  const verKey = `phone_otp_verify_${identifierHash}`;
  delete localRateLimits[reqKey];
  delete localRateLimits[verKey];
  
  // Call the Edge Function with operation = "reset"
  try {
    const { supabase } = await import('../supabase');
    await supabase.functions.invoke('auth-rate-limit', {
      body: {
        operation: 'reset',
        identifierHash
      }
    });
  } catch (e) {
    // Do not invalidate an already successful login if resetting fails
    // Never log details
  }
}
