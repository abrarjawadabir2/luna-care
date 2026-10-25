/**
 * LunaCare Username Utilities: Normalization, Validation, and Policy Enforcement
 * 
 * Enforces production-grade username rules:
 * - Length: 3-30 characters
 * - Characters: a-z, A-Z, 0-9, _, .
 * - No spaces, no email @ inside, no slashes, no HTML/script, no emoji, no control/zero-width chars
 * - No leading/trailing separators (. or _)
 * - No repeated or mixed consecutive separators (.., __, ._, _.)
 * - Unicode NFKC normalization
 * - Case-insensitive uniqueness via username_normalized
 * - Reserved list protection to prevent authority/official impersonation
 */

export interface UsernameValidationResult {
  valid: boolean;
  rawUsername?: string;
  normalized?: string;
  errorCode?: 'USERNAME_INVALID' | 'USERNAME_NOT_AVAILABLE' | 'USERNAME_RESERVED';
  errorMessage?: string;
}

// Backend-only reserved names (Req 88)
export const RESERVED_USERNAMES = new Set<string>([
  'admin',
  'administrator',
  'root',
  'system',
  'support',
  'help',
  'security',
  'moderator',
  'staff',
  'official',
  'lunacare',
  'luna_care',
  'luna',
  'api',
  'www',
  'mail',
  'account',
  'accounts',
  'billing',
  'privacy',
  'terms',
  'legal',
  'status',
  'health',
  'auth',
  'login',
  'signup',
  'register',
  'settings',
  'me',
  'profile',
  'user',
  'users',
  'null',
  'undefined',
  'anonymous',
  'guest',
  'doctor',
  'nurse',
  'compliance',
  'emergency',
  'crisis',
  'internal',
  'operator',
  'moderation'
]);

/**
 * Check if a normalized username matches reserved list or official impersonation patterns
 */
export function isReservedUsername(normalized: string): boolean {
  if (RESERVED_USERNAMES.has(normalized)) {
    return true;
  }

  // Prevent names that falsely imply official LunaCare authority (Req 88, 89)
  if (
    normalized.startsWith('lunacare_admin') ||
    normalized.startsWith('lunacare_support') ||
    normalized.startsWith('lunacare_official') ||
    normalized.startsWith('lunacare_team') ||
    normalized.startsWith('lunacare_security') ||
    normalized.startsWith('lunacare_staff') ||
    normalized.startsWith('lunacare_help') ||
    normalized.startsWith('lunacare_system') ||
    normalized.startsWith('official_lunacare') ||
    normalized.startsWith('official_') ||
    normalized.endsWith('_official') ||
    normalized.startsWith('admin_') ||
    normalized.startsWith('support_') ||
    normalized.startsWith('security_')
  ) {
    return true;
  }

  return false;
}

/**
 * Normalizes a username:
 * 1. Trim whitespace
 * 2. Strip single leading '@' if entered by user
 * 3. Convert to Unicode NFKC
 * 4. Convert to lowercase
 */
export function normalizeUsername(raw: string): string {
  let cleaned = raw.trim();
  if (cleaned.startsWith('@')) {
    cleaned = cleaned.substring(1);
  }
  return cleaned.normalize('NFKC').toLowerCase();
}

/**
 * Validates raw username input according to strict policy
 */
export function validateUsername(raw: unknown): UsernameValidationResult {
  if (typeof raw !== 'string') {
    return {
      valid: false,
      errorCode: 'USERNAME_INVALID',
      errorMessage: 'Username must be a string.'
    };
  }

  // 1. Trim whitespace
  let cleaned = raw.trim();

  // Strip single leading '@' for user convenience if they typed @myname
  if (cleaned.startsWith('@')) {
    cleaned = cleaned.substring(1);
  }

  // Check for invisible, zero-width, or control characters
  // \u0000-\u001F, \u007F-\u009F, \u200B-\u200D, \uFEFF, \u202A-\u202E
  const invisibleRegex = /[\u0000-\u001F\u007F-\u009F\u200B-\u200D\uFEFF\u202A-\u202E\u00A0]/;
  if (invisibleRegex.test(cleaned)) {
    return {
      valid: false,
      errorCode: 'USERNAME_INVALID',
      errorMessage: 'Username contains invisible or control characters.'
    };
  }

  // Check length: 3 to 30 characters
  if (cleaned.length < 3) {
    return {
      valid: false,
      errorCode: 'USERNAME_INVALID',
      errorMessage: 'Username must contain 3–30 characters.'
    };
  }

  if (cleaned.length > 30) {
    return {
      valid: false,
      errorCode: 'USERNAME_INVALID',
      errorMessage: 'Username must contain 3–30 characters.'
    };
  }

  // Allowed characters: a-z, A-Z, 0-9, _, ., -
  // Reject spaces, email-style @, slashes, backslashes, HTML, emoji, quotes, etc.
  const allowedCharsRegex = /^[a-zA-Z0-9_.-]+$/;
  if (!allowedCharsRegex.test(cleaned)) {
    return {
      valid: false,
      errorCode: 'USERNAME_INVALID',
      errorMessage: 'Only letters, numbers, periods, hyphens, and underscores are allowed.'
    };
  }

  // Reject leading separator
  if (cleaned.startsWith('.') || cleaned.startsWith('_') || cleaned.startsWith('-')) {
    return {
      valid: false,
      errorCode: 'USERNAME_INVALID',
      errorMessage: 'Username cannot start with a period, hyphen, or underscore.'
    };
  }

  // Reject trailing separator
  if (cleaned.endsWith('.') || cleaned.endsWith('_') || cleaned.endsWith('-')) {
    return {
      valid: false,
      errorCode: 'USERNAME_INVALID',
      errorMessage: 'Username cannot end with a period, hyphen, or underscore.'
    };
  }

  // Reject repeated or consecutive separators (.., __, --, ._, _., .-, -., _-, -_)
  if (/[-._]{2,}/.test(cleaned)) {
    return {
      valid: false,
      errorCode: 'USERNAME_INVALID',
      errorMessage: 'Username cannot contain consecutive periods, hyphens, or underscores.'
    };
  }

  // Unicode NFKC normalization
  const normalized = cleaned.normalize('NFKC').toLowerCase();

  // Reserved names check
  if (isReservedUsername(normalized)) {
    return {
      valid: false,
      errorCode: 'USERNAME_RESERVED',
      errorMessage: 'Username is not available.'
    };
  }

  return {
    valid: true,
    rawUsername: cleaned,
    normalized
  };
}
