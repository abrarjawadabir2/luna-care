import crypto from 'node:crypto';
import bcrypt from 'bcryptjs';
import { db, type UserRecord, type ProfileRecord } from '../database/index.js';
import { purposeSeparatedHash, generateSecureOtp, safeTimingCompare } from '../security/index.js';
import { signUserToken } from '../api/middleware/auth.js';
import { AuditService } from '../audit/audit.service.js';
import { logger } from '../utils/logger.js';

export class AuthService {
  // 1. REGISTER
  static async register(input: { email: string; password: string; displayName: string; phone?: string }) {
    const existing = db.getUserByEmail(input.email);
    if (existing) {
      throw new Error('EMAIL_ALREADY_EXISTS');
    }

    const salt = await bcrypt.genSalt(12);
    const passwordHash = await bcrypt.hash(input.password, salt);
    const userId = crypto.randomUUID();
    const now = new Date().toISOString();

    // STRICT: Role is always USER. Client cannot self-promote. isPremium is always false.
    const user: UserRecord = {
      id: userId,
      email: input.email.toLowerCase().trim(),
      phone: input.phone?.trim(),
      passwordHash,
      role: 'USER',
      isPremium: false,
      createdAt: now,
      updatedAt: now
    };

    const profile: ProfileRecord = {
      id: userId,
      displayName: input.displayName.trim(),
      userMode: 'SELF_TRACKING',
      genderMode: 'PREFER_NOT_TO_SAY',
      pronoun: 'PREFER_NOT_TO_SAY',
      bodyRelevantMode: 'PREFER_NOT_TO_SAY',
      consentConfirmed: false,
      locationPrivacyMode: 'OFF',
      selectedConditions: [],
      behaviourFocuses: [],
      averageCycleLength: 28,
      averagePeriodLength: 5,
      aiMessagesUsedThisMonth: 0,
      aiCurrentMonth: new Date().toISOString().substring(0, 7),
      createdAt: now,
      updatedAt: now
    };

    db.createUser(user);
    db.saveProfile(profile);

    await AuditService.recordEvent({
      eventType: 'auth.login.success',
      userId,
      resourceType: 'user',
      resourceId: userId,
      result: 'success',
      metadata: { action: 'register', emailHash: purposeSeparatedHash('audit_reference', user.email) }
    });

    const token = signUserToken({ id: user.id, email: user.email, role: user.role, isPremium: user.isPremium });

    return {
      user: {
        id: user.id,
        email: user.email,
        displayName: profile.displayName,
        role: user.role,
        isPremium: user.isPremium
      },
      token
    };
  }

  // 2. LOGIN
  static async login(input: { email: string; password: string }) {
    const user = db.getUserByEmail(input.email);

    if (!user) {
      await AuditService.recordEvent({
        eventType: 'auth.login.failure',
        resourceType: 'user',
        result: 'failure',
        reasonCode: 'INVALID_CREDENTIALS',
        metadata: { accountHash: purposeSeparatedHash('audit_reference', input.email) }
      });
      // Anti-enumeration: exact same message whether email or password is wrong
      throw new Error('INVALID_CREDENTIALS');
    }

    const passwordMatch = await bcrypt.compare(input.password, user.passwordHash);
    if (!passwordMatch) {
      await AuditService.recordEvent({
        eventType: 'auth.login.failure',
        userId: user.id,
        resourceType: 'user',
        resourceId: user.id,
        result: 'failure',
        reasonCode: 'INVALID_CREDENTIALS',
        metadata: { accountHash: purposeSeparatedHash('audit_reference', input.email) }
      });
      throw new Error('INVALID_CREDENTIALS');
    }

    await AuditService.recordEvent({
      eventType: 'auth.login.success',
      userId: user.id,
      resourceType: 'user',
      resourceId: user.id,
      result: 'success',
      metadata: { accountHash: purposeSeparatedHash('audit_reference', input.email) }
    });

    const profile = db.getProfile(user.id);
    const token = signUserToken({ id: user.id, email: user.email, role: user.role, isPremium: user.isPremium });

    return {
      user: {
        id: user.id,
        email: user.email,
        displayName: profile?.displayName || 'User',
        role: user.role,
        isPremium: user.isPremium
      },
      token
    };
  }

  // 3. OTP REQUEST
  static async requestOtp(phone: string): Promise<{ success: boolean; message: string; devCode?: string }> {
    const hashedPhone = purposeSeparatedHash('phone_rate_limit', phone);
    const code = generateSecureOtp();
    const codeHash = purposeSeparatedHash('token_fingerprint', code);
    const now = Date.now();
    const expiresAt = now + 5 * 60 * 1000; // 5 minutes

    db.saveOtp(hashedPhone, {
      hashedIdentifier: hashedPhone,
      codeHash,
      attempts: 0,
      expiresAt,
      createdAt: now
    });

    await AuditService.recordEvent({
      eventType: 'auth.otp.request',
      resourceType: 'phone',
      result: 'success',
      metadata: { phoneHash: hashedPhone }
    });

    logger.info('otp.requested', { phoneHash: hashedPhone, expiresInSeconds: 300 });

    // In non-production testing environment, return devCode for automated integration tests
    const devCode = process.env['NODE_ENV'] === 'test' ? code : undefined;

    return {
      success: true,
      message: 'Verification code dispatched if phone is eligible.',
      ...(devCode ? { devCode } : {})
    };
  }

  // 4. OTP VERIFY
  static async verifyOtp(phone: string, inputCode: string) {
    const hashedPhone = purposeSeparatedHash('phone_rate_limit', phone);
    const otp = db.getOtp(hashedPhone);
    const now = Date.now();

    if (!otp || otp.expiresAt <= now) {
      if (otp) db.deleteOtp(hashedPhone);
      await AuditService.recordEvent({
        eventType: 'auth.otp.failure',
        resourceType: 'phone',
        result: 'failure',
        reasonCode: 'EXPIRED_OR_NOT_FOUND',
        metadata: { phoneHash: hashedPhone }
      });
      throw new Error('INVALID_OR_EXPIRED_OTP');
    }

    // Limit verification attempts: max 5
    if (otp.attempts >= 5) {
      db.deleteOtp(hashedPhone);
      await AuditService.recordEvent({
        eventType: 'auth.otp.failure',
        resourceType: 'phone',
        result: 'denied',
        reasonCode: 'TOO_MANY_FAILED_ATTEMPTS',
        metadata: { phoneHash: hashedPhone }
      });
      throw new Error('TOO_MANY_FAILED_ATTEMPTS');
    }

    const inputCodeHash = purposeSeparatedHash('token_fingerprint', inputCode);
    const match = safeTimingCompare(otp.codeHash, inputCodeHash);

    if (!match) {
      otp.attempts += 1;
      db.saveOtp(hashedPhone, otp);
      await AuditService.recordEvent({
        eventType: 'auth.otp.failure',
        resourceType: 'phone',
        result: 'failure',
        reasonCode: 'MISMATCH',
        metadata: { phoneHash: hashedPhone, attempt: otp.attempts }
      });
      throw new Error('INVALID_OR_EXPIRED_OTP');
    }

    // Code verified: delete OTP immediately (single-use)
    db.deleteOtp(hashedPhone);

    // Find or create phone user
    let user = db.getUserByPhone(phone);
    if (!user) {
      const userId = crypto.randomUUID();
      const isoNow = new Date().toISOString();
      user = {
        id: userId,
        email: `phone_${hashedPhone.substring(0, 8)}@lunacare.internal`,
        phone,
        passwordHash: '',
        role: 'USER',
        isPremium: false,
        createdAt: isoNow,
        updatedAt: isoNow
      };
      db.createUser(user);

      const profile: ProfileRecord = {
        id: userId,
        displayName: 'Phone Companion',
        userMode: 'SELF_TRACKING',
        genderMode: 'PREFER_NOT_TO_SAY',
        pronoun: 'PREFER_NOT_TO_SAY',
        bodyRelevantMode: 'PREFER_NOT_TO_SAY',
        consentConfirmed: false,
        locationPrivacyMode: 'OFF',
        selectedConditions: [],
        behaviourFocuses: [],
        averageCycleLength: 28,
        averagePeriodLength: 5,
        aiMessagesUsedThisMonth: 0,
        aiCurrentMonth: isoNow.substring(0, 7),
        createdAt: isoNow,
        updatedAt: isoNow
      };
      db.saveProfile(profile);
    }

    await AuditService.recordEvent({
      eventType: 'auth.otp.success',
      userId: user.id,
      resourceType: 'user',
      resourceId: user.id,
      result: 'success',
      metadata: { phoneHash: hashedPhone }
    });

    const token = signUserToken({ id: user.id, email: user.email, role: user.role, isPremium: user.isPremium });

    return {
      user: {
        id: user.id,
        email: user.email,
        phone: user.phone,
        role: user.role,
        isPremium: user.isPremium
      },
      token
    };
  }
}
