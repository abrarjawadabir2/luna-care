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

  // Strip script, iframe, object, embed, svg, link, form, style tags and contents (including unclosed trailing tags)
  sanitized = sanitized.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '');
  sanitized = sanitized.replace(/<script\b[\s\S]*$/gi, '');
  sanitized = sanitized.replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '');
  sanitized = sanitized.replace(/<iframe\b[\s\S]*$/gi, '');
  sanitized = sanitized.replace(/<object\b[^<]*(?:(?!<\/object>)<[^<]*)*<\/object>/gi, '');
  sanitized = sanitized.replace(/<embed\b[^>]*>/gi, '');
  sanitized = sanitized.replace(/<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>/gi, '');
  sanitized = sanitized.replace(/<style\b[\s\S]*$/gi, '');
  sanitized = sanitized.replace(/<svg\b[^<]*(?:(?!<\/svg>)<[^<]*)*<\/svg>/gi, '');
  sanitized = sanitized.replace(/<link\b[^>]*>/gi, '');
  sanitized = sanitized.replace(/<form\b[^<]*(?:(?!<\/form>)<[^<]*)*<\/form>/gi, '');

  // Strip inline event handlers (e.g., onload=, onerror=, onclick=)
  sanitized = sanitized.replace(/\bon\w+\s*=\s*(['"]).*?\1/gi, '');
  sanitized = sanitized.replace(/\bon\w+\s*=\s*[^\s>]+/gi, '');

  // Strip remaining HTML tags and javascript: pseudo-protocol
  sanitized = sanitized.replace(/<[^>]+>/g, '');
  sanitized = sanitized.replace(/javascript\s*:/gi, '');

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

const PROMPT_INJECTION_PATTERNS: RegExp[] = [
  /ignore\s+(?:all\s+|any\s+)?(?:previous|prior|above)\s+instructions?/i,
  /disregard\s+(?:all\s+|any\s+)?(?:previous|prior|above)\s+instructions?/i,
  /reveal\s+(?:the\s+)?(?:system\s+prompt|secrets?|credentials?|env)/i,
  /dump\s+(?:the\s+)?(?:environment|database|secrets?)/i,
  /(?:execute|run)\s+(?:shell|command|terminal)/i,
  /<\|im_start\|>|\[INST\]/i
];

/**
 * Sanitizes untrusted Markdown extracted from converted documents:
 * - Strips dangerous HTML tags and inline event handlers
 * - Escapes remaining raw HTML brackets
 * - Neutralizes unsafe link schemes (javascript:, data:, vbscript:, file:, http://)
 * - Removes external/tracking Markdown image embeds
 * - Flags embedded prompt-injection patterns as warnings
 */
export function sanitizeConvertedMarkdown(markdown: string): {
  safeMarkdown: string;
  warnings: string[];
} {
  if (typeof markdown !== 'string') {
    return { safeMarkdown: '', warnings: [] };
  }

  const warnings: string[] = [];
  let text = markdown;

  // 1. Strip dangerous HTML blocks and tags (including unclosed script/iframe/style blocks)
  const dangerousTags = /<\s*(script|iframe|object|embed|style|svg|form)\b[^<]*(?:(?!<\/\1>)<[^<]*)*<\/\1>/gi;
  if (dangerousTags.test(text)) {
    warnings.push('DANGEROUS_HTML_STRIPPED');
    text = text.replace(dangerousTags, '');
  }

  const unclosedDangerousBlock = /<\s*(script|iframe|style)\b[\s\S]*$/gi;
  if (unclosedDangerousBlock.test(text)) {
    if (!warnings.includes('DANGEROUS_HTML_STRIPPED')) {
      warnings.push('DANGEROUS_HTML_STRIPPED');
    }
    text = text.replace(unclosedDangerousBlock, '');
  }

  const standaloneDangerousTags = /<\s*(script|iframe|object|embed|style|svg|form|link|meta|base)\b[^>]*\/?>/gi;
  if (standaloneDangerousTags.test(text)) {
    if (!warnings.includes('DANGEROUS_HTML_STRIPPED')) {
      warnings.push('DANGEROUS_HTML_STRIPPED');
    }
    text = text.replace(standaloneDangerousTags, '');
  }

  // 2. Strip inline event handlers (e.g., onerror=, onload=)
  if (/\bon\w+\s*=/i.test(text)) {
    warnings.push('EVENT_HANDLER_STRIPPED');
    text = text.replace(/\bon\w+\s*=\s*(['"]).*?\1/gi, '');
    text = text.replace(/\bon\w+\s*=\s*[^\s>]+/gi, '');
  }

  // 3. Neutralize Markdown images (supports balanced parentheses in URLs)
  text = replaceMarkdownLinks(text, true, (alt) => {
    if (!warnings.includes('EXTERNAL_IMAGE_NEUTRALIZED')) {
      warnings.push('EXTERNAL_IMAGE_NEUTRALIZED');
    }
    const cleanAlt = alt.trim() || 'image';
    return `[Image removed for privacy: ${cleanAlt}]`;
  });

  // 4. Validate Markdown links strictly (supports balanced parentheses in URLs); allow only #anchors or https:// URLs
  text = replaceMarkdownLinks(text, false, (label, url) => {
    const trimmedUrl = url.trim().replace(/^<|>$/g, '');
    const isAnchor = trimmedUrl.startsWith('#') && !/[\s<>]/.test(trimmedUrl);
    const isSafeHttps = /^https:\/\//i.test(trimmedUrl) && isSafeUrl(trimmedUrl);
    if (isAnchor || isSafeHttps) {
      return `[${label}](${trimmedUrl})`;
    }
    if (!warnings.includes('UNSAFE_LINK_NEUTRALIZED')) {
      warnings.push('UNSAFE_LINK_NEUTRALIZED');
    }
    return `[${label} (unsafe link removed)]`;
  });

  // 5. Escape remaining raw HTML angle brackets so no raw HTML can execute on render
  if (/[<>]/.test(text)) {
    if (!warnings.includes('RAW_HTML_ESCAPED')) {
      warnings.push('RAW_HTML_ESCAPED');
    }
    text = text.replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  // 6. Detect embedded prompt-injection directives (data-only framing)
  if (PROMPT_INJECTION_PATTERNS.some((pattern) => pattern.test(text))) {
    warnings.push('PROMPT_INJECTION_PATTERN_DETECTED');
  }

  return { safeMarkdown: text.trim(), warnings };
}

/**
 * Parses Markdown links/images while balancing nested parentheses inside URLs
 * (e.g. `javascript:alert(1)` or `https://example.org/wiki/Cycle_(biology)`).
 */
function replaceMarkdownLinks(
  input: string,
  imagesOnly: boolean,
  replacer: (label: string, url: string) => string
): string {
  let out = '';
  let i = 0;
  while (i < input.length) {
    const isImage = input[i] === '!' && input[i + 1] === '[';
    const isLink = !isImage && input[i] === '[';

    if ((imagesOnly && isImage) || (!imagesOnly && isLink)) {
      const labelStart = isImage ? i + 2 : i + 1;
      const labelEnd = input.indexOf(']', labelStart);
      if (labelEnd !== -1 && input[labelEnd + 1] === '(') {
        let depth = 1;
        let cursor = labelEnd + 2;
        while (cursor < input.length && depth > 0) {
          const ch = input[cursor];
          if (ch === '(') depth++;
          else if (ch === ')') depth--;
          cursor++;
        }
        if (depth === 0) {
          const label = input.slice(labelStart, labelEnd);
          const url = input.slice(labelEnd + 2, cursor - 1);
          out += replacer(label, url);
          i = cursor;
          continue;
        }
      }
    }

    out += input[i];
    i++;
  }
  return out;
}
