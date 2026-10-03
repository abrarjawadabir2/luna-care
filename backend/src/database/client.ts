/**
 * LunaCare Database Layer & Store
 * 
 * Implements strict table classification:
 * - PUBLIC: General health articles, system status.
 * - AUTHENTICATED: General authenticated operations.
 * - PRIVATE USER DATA: profiles, period_logs, behaviour_logs, medical_journal_entries.
 * - INTERNAL: rate_limits, otp_stores, security_events.
 * - ADMIN ONLY: system-wide audit records.
 * 
 * Non-negotiable security:
 * 1. Deny-by-default access.
 * 2. Strict user-record ownership: User A cannot read, update, or delete User B's records.
 * 3. Client cannot self-assign role (role promotion forbidden).
 * 4. Fake isPremium flag is ignored.
 */

export type UserRole =
  | 'USER'
  | 'PREMIUM_USER'
  | 'MODERATOR'
  | 'MEDICAL_CONTENT_REVIEWER'
  | 'SUPPORT_AGENT'
  | 'ADMIN'
  | 'SUPER_ADMIN';

export interface UserRecord {
  id: string;
  email: string;
  phone?: string;
  passwordHash: string;
  role: UserRole;
  isPremium: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProfileRecord {
  id: string; // matches UserRecord.id
  displayName: string;
  userMode: 'SELF_TRACKING' | 'SUPPORT_MODE' | 'EDUCATION_ONLY';
  genderMode: string;
  pronoun: string;
  customPronoun?: string | null;
  bodyRelevantMode: string;
  supportRelationship?: string | null;
  consentConfirmed: boolean;
  religion?: string | null;
  country?: string | null;
  region?: string | null;
  city?: string | null;
  locationPrivacyMode: 'OFF' | 'ON_DEVICE_ONLY' | 'APPROXIMATE_REGION' | 'TEMPORARY_EXACT';
  selectedConditions: string[];
  behaviourFocuses: string[];
  averageCycleLength: number;
  averagePeriodLength: number;
  aiMessagesUsedThisMonth: number;
  aiCurrentMonth: string;
  createdAt: string;
  updatedAt: string;
}

export interface PeriodLogRecord {
  id: string;
  userId: string;
  startDate: string;
  endDate?: string;
  flowLevel: string;
  symptoms: string[];
  notesEncrypted?: string;
  createdAt: string;
  updatedAt: string;
}

export interface BehaviourLogRecord {
  id: string;
  userId: string;
  logDate: string;
  mood: string;
  stressLevel: number;
  anxietyLevel: number;
  sleepHours: number;
  sleepQuality: number;
  painLevel: number;
  energyLevel: number;
  hydrationLevel: string;
  flowLevel?: string;
  symptoms: string[];
  notesEncrypted?: string;
  crisisFlag: boolean;
  flags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface MedicalJournalRecord {
  id: string;
  userId: string;
  entryDate: string;
  category: string;
  title: string;
  symptoms: string[];
  painLevel: number;
  mood?: string;
  flowLevel?: string;
  medicinesTaken?: string;
  doctorVisit: boolean;
  nextAppointment?: string;
  notesEncrypted?: string;
  attachmentPath?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AuditRecord {
  id: string;
  actorId?: string;
  actorRole: string;
  action: string;
  resourceType: string;
  resourceId?: string;
  ipHash?: string;
  userAgentHash?: string;
  metadata: Record<string, unknown>;
  createdAt: string;
}

export interface OtpRecord {
  hashedIdentifier: string; // purposeSeparatedHash('phone_rate_limit', phone)
  codeHash: string; // purposeSeparatedHash('token_fingerprint', code)
  attempts: number;
  expiresAt: number;
  createdAt: number;
}

export interface UploadRecord {
  id: string;
  userId: string;
  safeKey: string;
  detectedMime: string;
  sizeBytes: number;
  status: 'PENDING' | 'INSPECTED' | 'CONFIRMED' | 'ABANDONED';
  createdAt: number;
  expiresAt: number;
}

class InMemoryDatabase {
  private users: Map<string, UserRecord> = new Map();
  private profiles: Map<string, ProfileRecord> = new Map();
  private periodLogs: Map<string, PeriodLogRecord> = new Map();
  private behaviourLogs: Map<string, BehaviourLogRecord> = new Map();
  private medicalJournals: Map<string, MedicalJournalRecord> = new Map();
  private auditLogs: AuditRecord[] = [];
  private otps: Map<string, OtpRecord> = new Map();
  private uploads: Map<string, UploadRecord> = new Map();
  private deletionQueue: Set<string> = new Set();

  // Reset database (for test isolation)
  reset() {
    this.users.clear();
    this.profiles.clear();
    this.periodLogs.clear();
    this.behaviourLogs.clear();
    this.medicalJournals.clear();
    this.auditLogs = [];
    this.otps.clear();
    this.uploads.clear();
    this.deletionQueue.clear();
  }

  // --- USERS & AUTH ---
  createUser(user: UserRecord): UserRecord {
    this.users.set(user.id, { ...user });
    return { ...user };
  }

  getUserById(id: string): UserRecord | null {
    const user = this.users.get(id);
    return user ? { ...user } : null;
  }

  getUserByEmail(email: string): UserRecord | null {
    const normalized = email.trim().toLowerCase();
    for (const u of this.users.values()) {
      if (u.email.toLowerCase() === normalized) {
        return { ...u };
      }
    }
    return null;
  }

  getUserByPhone(phone: string): UserRecord | null {
    const normalized = phone.trim();
    for (const u of this.users.values()) {
      if (u.phone === normalized) {
        return { ...u };
      }
    }
    return null;
  }

  updateUser(id: string, updates: Partial<UserRecord>): UserRecord | null {
    const user = this.users.get(id);
    if (!user) return null;
    const updated = { ...user, ...updates, updatedAt: new Date().toISOString() };
    this.users.set(id, updated);
    return { ...updated };
  }

  deleteUser(id: string): boolean {
    const existed = this.users.delete(id);
    this.profiles.delete(id);
    this.deletionQueue.delete(id);
    // Cascade delete user data
    for (const [k, v] of this.periodLogs.entries()) {
      if (v.userId === id) this.periodLogs.delete(k);
    }
    for (const [k, v] of this.behaviourLogs.entries()) {
      if (v.userId === id) this.behaviourLogs.delete(k);
    }
    for (const [k, v] of this.medicalJournals.entries()) {
      if (v.userId === id) this.medicalJournals.delete(k);
    }
    for (const [k, v] of this.uploads.entries()) {
      if (v.userId === id) this.uploads.delete(k);
    }
    return existed;
  }

  // Queue account for deletion
  queueAccountDeletion(userId: string): void {
    this.deletionQueue.add(userId);
  }

  processDeletionQueue(): number {
    let count = 0;
    for (const userId of Array.from(this.deletionQueue)) {
      this.deleteUser(userId);
      count++;
    }
    return count;
  }

  // --- PROFILES ---
  getProfile(userId: string): ProfileRecord | null {
    const p = this.profiles.get(userId);
    return p ? { ...p } : null;
  }

  saveProfile(profile: ProfileRecord): ProfileRecord {
    this.profiles.set(profile.id, { ...profile });
    return { ...profile };
  }

  // --- PERIOD LOGS ---
  createPeriodLog(log: PeriodLogRecord): PeriodLogRecord {
    this.periodLogs.set(log.id, { ...log });
    return { ...log };
  }

  getPeriodLogsByUserId(userId: string): PeriodLogRecord[] {
    return Array.from(this.periodLogs.values())
      .filter(l => l.userId === userId)
      .sort((a, b) => b.startDate.localeCompare(a.startDate));
  }

  getPeriodLogById(id: string): PeriodLogRecord | null {
    const l = this.periodLogs.get(id);
    return l ? { ...l } : null;
  }

  deletePeriodLog(id: string, userId: string): boolean {
    const item = this.periodLogs.get(id);
    if (!item || item.userId !== userId) return false;
    return this.periodLogs.delete(id);
  }

  // --- BEHAVIOUR LOGS ---
  createBehaviourLog(log: BehaviourLogRecord): BehaviourLogRecord {
    this.behaviourLogs.set(log.id, { ...log });
    return { ...log };
  }

  getBehaviourLogsByUserId(userId: string): BehaviourLogRecord[] {
    return Array.from(this.behaviourLogs.values())
      .filter(l => l.userId === userId)
      .sort((a, b) => b.logDate.localeCompare(a.logDate));
  }

  getBehaviourLogById(id: string): BehaviourLogRecord | null {
    const b = this.behaviourLogs.get(id);
    return b ? { ...b } : null;
  }

  deleteBehaviourLog(id: string, userId: string): boolean {
    const item = this.behaviourLogs.get(id);
    if (!item || item.userId !== userId) return false;
    return this.behaviourLogs.delete(id);
  }

  // --- MEDICAL JOURNAL ENTRIES ---
  createMedicalJournal(entry: MedicalJournalRecord): MedicalJournalRecord {
    this.medicalJournals.set(entry.id, { ...entry });
    return { ...entry };
  }

  getMedicalJournalsByUserId(userId: string): MedicalJournalRecord[] {
    return Array.from(this.medicalJournals.values())
      .filter(j => j.userId === userId)
      .sort((a, b) => b.entryDate.localeCompare(a.entryDate));
  }

  getMedicalJournalById(id: string): MedicalJournalRecord | null {
    const j = this.medicalJournals.get(id);
    return j ? { ...j } : null;
  }

  deleteMedicalJournal(id: string, userId: string): boolean {
    const item = this.medicalJournals.get(id);
    if (!item || item.userId !== userId) return false;
    return this.medicalJournals.delete(id);
  }

  // --- AUDIT LOGS (Append-only) ---
  addAuditLog(log: AuditRecord): void {
    this.auditLogs.push({ ...log });
  }

  getAuditLogs(): AuditRecord[] {
    return [...this.auditLogs];
  }

  cleanupAuditLogs(retentionDays = 90): number {
    const cutoff = Date.now() - retentionDays * 24 * 60 * 60 * 1000;
    const initialLength = this.auditLogs.length;
    this.auditLogs = this.auditLogs.filter(log => new Date(log.createdAt).getTime() >= cutoff);
    return initialLength - this.auditLogs.length;
  }

  // --- OTPS ---
  saveOtp(hashedIdentifier: string, record: OtpRecord): void {
    this.otps.set(hashedIdentifier, { ...record });
  }

  getOtp(hashedIdentifier: string): OtpRecord | null {
    const o = this.otps.get(hashedIdentifier);
    return o ? { ...o } : null;
  }

  deleteOtp(hashedIdentifier: string): void {
    this.otps.delete(hashedIdentifier);
  }

  cleanupExpiredOtps(): number {
    const now = Date.now();
    let cleaned = 0;
    for (const [key, record] of this.otps.entries()) {
      if (record.expiresAt <= now) {
        this.otps.delete(key);
        cleaned++;
      }
    }
    return cleaned;
  }

  // --- UPLOADS ---
  saveUpload(upload: UploadRecord): void {
    this.uploads.set(upload.id, { ...upload });
  }

  getUpload(id: string): UploadRecord | null {
    const u = this.uploads.get(id);
    return u ? { ...u } : null;
  }

  cleanupExpiredUploads(): number {
    const now = Date.now();
    let cleaned = 0;
    for (const [key, upload] of this.uploads.entries()) {
      if (upload.expiresAt <= now || upload.status === 'ABANDONED') {
        this.uploads.delete(key);
        cleaned++;
      }
    }
    return cleaned;
  }
}

export const db = new InMemoryDatabase();
