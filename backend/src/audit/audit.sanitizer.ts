/**
 * LunaCare Audit Sanitizer
 * 
 * Enforces strict privacy guarantees:
 * Prohibited fields are stripped or redacted before any audit record reaches logger or database.
 */

const PROHIBITED_KEYS = [
  'password',
  'otp',
  'token',
  'accesstoken',
  'refreshtoken',
  'access_token',
  'refresh_token',
  'authorization',
  'cookie',
  'geminiapikey',
  'gemini_api_key',
  'apikey',
  'api_key',
  'databasepassword',
  'database_password',
  'servicerolekey',
  'service_role_key',
  'oauthsecret',
  'oauth_secret',
  'smssecret',
  'sms_secret',
  'secret',
  'medicaljournal',
  'medical_journal',
  'journalcontent',
  'journal_content',
  'symptomnotes',
  'symptom_notes',
  'notesencrypted',
  'notes_encrypted',
  'notes',
  'note',
  'medicalnote',
  'medical_note',
  'moodtext',
  'mood_text',
  'crisissupport',
  'crisis_support',
  'crisismessage',
  'crisis_message',
  'crisistext',
  'crisis_text',
  'exactgps',
  'exact_gps',
  'gps',
  'latitude',
  'longitude',
  'lat',
  'lng',
  'filecontent',
  'file_content',
  'documentcontents',
  'document_contents',
  'buffer',
  'payload'
];

const PROHIBITED_PATTERNS = [
  /password/i,
  /otp/i,
  /token/i,
  /secret/i,
  /authorization/i,
  /cookie/i,
  /gemini/i,
  /journal/i,
  /symptom/i,
  /note/i,
  /medical/i,
  /crisis/i,
  /gps/i,
  /latitude/i,
  /longitude/i,
  /document/i
];

export function isProhibitedKey(key: string): boolean {
  const normalized = key.toLowerCase().replace(/[-_]/g, '');
  if (PROHIBITED_KEYS.some(k => k.replace(/[-_]/g, '') === normalized)) {
    return true;
  }
  return PROHIBITED_PATTERNS.some(p => p.test(key));
}

export function sanitizeAuditMetadata(obj: unknown, depth = 0): unknown {
  if (depth > 5) return '[MAX_DEPTH]';
  if (obj === null || obj === undefined) return obj;
  if (typeof obj !== 'object') return obj;

  if (Array.isArray(obj)) {
    return obj.map(item => sanitizeAuditMetadata(item, depth + 1));
  }

  const sanitized: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(obj as Record<string, unknown>)) {
    if (isProhibitedKey(key)) {
      // Completely omit prohibited fields from audit records to guarantee zero leakage
      continue;
    }

    if (typeof value === 'object' && value !== null) {
      sanitized[key] = sanitizeAuditMetadata(value, depth + 1);
    } else {
      sanitized[key] = value;
    }
  }

  return sanitized;
}
