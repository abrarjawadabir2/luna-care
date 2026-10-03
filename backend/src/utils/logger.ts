// Structured sanitized logger — zero sensitive payload leakage

export type LogLevel = 'info' | 'warn' | 'error' | 'debug';

const SENSITIVE_KEY_PATTERNS = [
  /password/i,
  /token/i,
  /secret/i,
  /otp/i,
  /authorization/i,
  /cookie/i,
  /gps/i,
  /latitude/i,
  /longitude/i,
  /journal/i,
  /medical/i,
  /notes/i,
  /crisis/i
];

function sanitizeObject(obj: unknown, depth = 0): unknown {
  if (depth > 4) return '[MAX_DEPTH]';
  if (obj === null || obj === undefined) return obj;
  if (typeof obj !== 'object') return obj;

  if (Array.isArray(obj)) {
    return obj.map(item => sanitizeObject(item, depth + 1));
  }

  const sanitized: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(obj as Record<string, unknown>)) {
    if (SENSITIVE_KEY_PATTERNS.some(pattern => pattern.test(key))) {
      sanitized[key] = '[REDACTED]';
    } else if (typeof value === 'object' && value !== null) {
      sanitized[key] = sanitizeObject(value, depth + 1);
    } else {
      sanitized[key] = value;
    }
  }
  return sanitized;
}

export const logger = {
  log(level: LogLevel, event: string, meta: Record<string, unknown> = {}) {
    const payload = {
      timestamp: new Date().toISOString(),
      level,
      event,
      ...sanitizeObject(meta) as Record<string, unknown>
    };
    const line = JSON.stringify(payload);
    if (level === 'error') {
      console.error(line);
    } else if (level === 'warn') {
      console.warn(line);
    } else {
      console.log(line);
    }
  },

  info(event: string, meta: Record<string, unknown> = {}) {
    this.log('info', event, meta);
  },

  warn(event: string, meta: Record<string, unknown> = {}) {
    this.log('warn', event, meta);
  },

  error(event: string, meta: Record<string, unknown> = {}) {
    this.log('error', event, meta);
  },

  debug(event: string, meta: Record<string, unknown> = {}) {
    if (process.env['NODE_ENV'] !== 'production') {
      this.log('debug', event, meta);
    }
  }
};
