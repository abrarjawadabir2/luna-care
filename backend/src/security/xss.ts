/**
 * LunaCare Defensive XSS Mitigation & Safe URL Validation Utilities
 * 
 * Provides defense-in-depth protections:
 * 1. Strict URL scheme validation (rejects javascript:, vbscript:, data:, file:, etc.)
 * 2. Context-aware HTML entity escaping (defends against reflected and stored HTML injection)
 * 3. User profile bio sanitization
 * 4. Untrusted AI output sanitization
 * 5. SVG security inspection
 */

const DANGEROUS_SCHEMES = new Set([
  'javascript',
  'vbscript',
  'data',
  'blob',
  'file',
  'about'
]);

/**
 * Escapes untrusted text for safe insertion into HTML contexts.
 * Encodes &, <, >, ", ', /, and ` into their corresponding HTML entities.
 */
export function escapeHtml(input: string): string {
  if (typeof input !== 'string') return '';
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;')
    .replace(/\//g, '&#x2F;')
    .replace(/`/g, '&#x60;');
}

/**
 * Validates that a URL is safe to use in href, src, or redirect contexts.
 * Strictly disallows javascript:, data:, vbscript:, and unapproved schemes.
 */
export function isSafeUrl(url: string, options: { allowHttpInDev?: boolean } = {}): boolean {
  if (typeof url !== 'string') return false;

  const trimmed = url.trim();
  if (!trimmed || trimmed.length > 2048) return false;

  // Reject ASCII control characters and whitespace within the URL
  // eslint-disable-next-line no-control-regex
  if (/[\x00-\x1F\x7F\s]/.test(trimmed)) return false;

  // Reject protocol-relative URLs (e.g., //attacker.com)
  if (trimmed.startsWith('//')) return false;

  // Check for scheme
  const schemeMatch = /^([a-zA-Z][a-zA-Z0-9+.-]*):/.exec(trimmed);
  if (schemeMatch) {
    const scheme = schemeMatch[1]!.toLowerCase();
    if (DANGEROUS_SCHEMES.has(scheme)) {
      return false;
    }

    if (scheme === 'https') {
      try {
        const parsed = new URL(trimmed);
        return parsed.protocol === 'https:';
      } catch {
        return false;
      }
    }

    if (scheme === 'http') {
      const isDevOrTest = process.env['NODE_ENV'] !== 'production';
      if (options.allowHttpInDev || isDevOrTest) {
        try {
          const parsed = new URL(trimmed);
          const isLoopback = parsed.hostname === 'localhost' || parsed.hostname === '127.0.0.1';
          return parsed.protocol === 'http:' && isLoopback;
        } catch {
          return false;
        }
      }
      return false;
    }

    // Any other scheme (ftp, mailto, tel, custom) is not permitted for web/profile links
    return false;
  }

  // If no scheme, it must be a safe relative storage key or relative path
  // Must not contain path traversal (..)
  if (trimmed.includes('..')) return false;

  // Must match safe storage key format (e.g., user_UUID_random.png) or safe relative path
  return /^[a-zA-Z0-9_./-]+$/.test(trimmed);
}

/**
 * Sanitizes user-profile bio text by removing script tags, event handlers, and dangerous HTML.
 * Preserves normal text, paragraphs, punctuation, and emojis.
 */
export function sanitizeUserBio(bio: string): string {
  if (typeof bio !== 'string') return '';
  let sanitized = bio;

  // Strip script, iframe, object, embed, svg, link, form, style tags and contents
  sanitized = sanitized.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '');
  sanitized = sanitized.replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '');
  sanitized = sanitized.replace(/<object\b[^<]*(?:(?!<\/object>)<[^<]*)*<\/object>/gi, '');
  sanitized = sanitized.replace(/<embed\b[^>]*>/gi, '');
  sanitized = sanitized.replace(/<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>/gi, '');
  sanitized = sanitized.replace(/<svg\b[^<]*(?:(?!<\/svg>)<[^<]*)*<\/svg>/gi, '');
  sanitized = sanitized.replace(/<link\b[^>]*>/gi, '');
  sanitized = sanitized.replace(/<form\b[^<]*(?:(?!<\/form>)<[^<]*)*<\/form>/gi, '');

  // Strip inline event handlers (e.g., onload=, onerror=, onclick=)
  sanitized = sanitized.replace(/\bon\w+\s*=\s*(['"]).*?\1/gi, '');
  sanitized = sanitized.replace(/\bon\w+\s*=\s*[^\s>]+/gi, '');

  // Strip remaining HTML tags
  sanitized = sanitized.replace(/<[^>]+>/g, '');

  return sanitized.trim();
}

/**
 * Sanitizes model-generated output from LLMs (e.g. Gemini), ensuring that if a client
 * mistakenly attempts to render AI responses as raw HTML, dangerous scripts or tags are defused.
 */
export function sanitizeAiOutput(text: string): { safeText: string; containsHtmlOrScript: boolean } {
  if (typeof text !== 'string') return { safeText: '', containsHtmlOrScript: false };

  const hasScriptOrHtml = /<\s*(script|iframe|object|embed|svg|img|a|form|style)\b|javascript:/i.test(text);
  const safeText = escapeHtml(text);

  return {
    safeText,
    containsHtmlOrScript: hasScriptOrHtml
  };
}

/**
 * Inspects SVG content for embedded scripts, event handlers, or foreign objects.
 */
export function isSafeSvg(svgContent: string): { safe: boolean; reason?: string } {
  if (typeof svgContent !== 'string') return { safe: false, reason: 'INVALID_INPUT' };

  if (/<script\b/i.test(svgContent)) {
    return { safe: false, reason: 'EMBEDDED_SCRIPT_TAG' };
  }

  if (/\bon\w+\s*=/i.test(svgContent)) {
    return { safe: false, reason: 'EVENT_HANDLER_ATTRIBUTE' };
  }

  if (/javascript:/i.test(svgContent)) {
    return { safe: false, reason: 'JAVASCRIPT_PSEUDO_PROTOCOL' };
  }

  if (/<foreignObject\b/i.test(svgContent)) {
    return { safe: false, reason: 'FOREIGN_OBJECT_TAG' };
  }

  return { safe: true };
}
