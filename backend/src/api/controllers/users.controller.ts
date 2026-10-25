import type { FastifyRequest, FastifyReply } from 'fastify';
import { db } from '../../database/index.js';
import { updateProfileSchema } from '../schemas/index.js';
import { AuditService } from '../../audit/audit.service.js';
import { validateUsername } from '../../utils/username.js';

export class UsersController {
  static async getProfile(request: FastifyRequest, reply: FastifyReply) {
    const userId = request.user!.id;
    const profile = db.getProfile(userId);
    const user = db.getUserById(userId);

    if (!profile || !user) {
      return reply.status(404).send({
        error: { code: 'NOT_FOUND', message: 'User profile not found.', requestId: request.requestId }
      });
    }

    return reply.status(200).send({
      id: profile.id,
      email: user.email,
      role: user.role,
      isPremium: user.isPremium,
      displayName: profile.displayName,
      username: profile.username || null,
      usernameNormalized: profile.username_normalized || null,
      usernameUpdatedAt: profile.username_updated_at || null,
      avatar: profile.avatar || null,
      bio: profile.bio || null,
      userMode: profile.userMode,
      genderMode: profile.genderMode,
      pronoun: profile.pronoun,
      customPronoun: profile.customPronoun,
      bodyRelevantMode: profile.bodyRelevantMode,
      supportRelationship: profile.supportRelationship,
      religion: profile.religion,
      country: profile.country,
      region: profile.region,
      city: profile.city,
      locationPrivacyMode: profile.locationPrivacyMode,
      selectedConditions: profile.selectedConditions,
      behaviourFocuses: profile.behaviourFocuses,
      averageCycleLength: profile.averageCycleLength,
      averagePeriodLength: profile.averagePeriodLength,
      aiMessagesUsedThisMonth: profile.aiMessagesUsedThisMonth
    });
  }

  static async updateProfile(request: FastifyRequest, reply: FastifyReply) {
    const userId = request.user!.id;
    const updates = updateProfileSchema.parse(request.body);

    const existing = db.getProfile(userId);
    if (!existing) {
      return reply.status(404).send({
        error: { code: 'NOT_FOUND', message: 'User profile not found.', requestId: request.requestId }
      });
    }

    // Handle username update if provided
    if (updates.username !== undefined && updates.username !== null) {
      const validation = validateUsername(updates.username);
      if (!validation.valid) {
        await AuditService.recordEvent({
          eventType: 'user.username.change_failed',
          userId,
          requestId: request.requestId,
          resourceType: 'username',
          result: 'failure',
          reasonCode: validation.errorCode,
          metadata: { reason: validation.errorCode }
        });

        // Safe message mapping: both reserved and unavailable map to "Username is not available."
        const clientMessage = validation.errorCode === 'USERNAME_RESERVED'
          ? 'Username is not available.'
          : (validation.errorMessage || 'Invalid username format.');

        return reply.status(400).send({
          error: {
            code: validation.errorCode || 'USERNAME_INVALID',
            message: clientMessage,
            requestId: request.requestId
          }
        });
      }

      // Attempt atomic database claim
      const claimResult = db.claimUsername(userId, validation.rawUsername!, validation.normalized!);
      if (!claimResult.success) {
        await AuditService.recordEvent({
          eventType: 'user.username.change_failed',
          userId,
          requestId: request.requestId,
          resourceType: 'username',
          result: 'failure',
          reasonCode: claimResult.reason,
          metadata: { reason: claimResult.reason }
        });

        if (claimResult.reason === 'USERNAME_CHANGE_LIMITED') {
          return reply.status(429).send({
            error: {
              code: 'USERNAME_CHANGE_LIMITED',
              message: 'Username can only be changed once every 14 days.',
              requestId: request.requestId
            }
          });
        }

        return reply.status(409).send({
          error: {
            code: 'USERNAME_NOT_AVAILABLE',
            message: 'Username is not available.',
            requestId: request.requestId
          }
        });
      }

      // Record audit event for successful claim or change
      const eventType = existing.username_normalized ? 'user.username.changed' : 'user.username.claimed';
      await AuditService.recordEvent({
        eventType,
        userId,
        requestId: request.requestId,
        resourceType: 'username',
        resourceId: validation.normalized,
        result: 'success',
        metadata: {
          action: existing.username_normalized ? 'change' : 'claim',
          usernameLength: validation.normalized!.length
        }
      });
    }

    // Save profile with remaining updates (username already updated atomically if claimed)
    const currentProfile = db.getProfile(userId)!;
    const { username: _u, ...otherUpdates } = updates;
    const updated = db.saveProfile({
      ...currentProfile,
      ...otherUpdates,
      updatedAt: new Date().toISOString()
    });

    await AuditService.recordEvent({
      eventType: 'account.profile.updated',
      userId,
      requestId: request.requestId,
      resourceType: 'profile',
      resourceId: userId,
      result: 'success',
      metadata: { updatedFields: Object.keys(updates) }
    });

    return reply.status(200).send({ profile: updated });
  }

  /**
   * Username Availability API (Req 84)
   * GET /api/v1/users/username-availability?username=...
   * Strict anti-enumeration: Never reveals user IDs, emails, phone numbers, or account details
   */
  static async checkAvailability(request: FastifyRequest, reply: FastifyReply) {
    const query = request.query as { username?: string };
    const rawUsername = query.username;

    if (!rawUsername || typeof rawUsername !== 'string') {
      return reply.status(400).send({
        available: false,
        reason: 'USERNAME_INVALID',
        message: 'Username query parameter is required.'
      });
    }

    const validation = validateUsername(rawUsername);
    if (!validation.valid) {
      if (validation.errorCode === 'USERNAME_RESERVED') {
        return reply.status(200).send({
          available: false,
          reason: 'USERNAME_NOT_AVAILABLE'
        });
      }
      return reply.status(200).send({
        available: false,
        reason: 'USERNAME_INVALID',
        message: validation.errorMessage
      });
    }

    const check = db.isUsernameAvailable(validation.normalized!, request.user?.id);
    if (!check.available) {
      return reply.status(200).send({
        available: false,
        reason: 'USERNAME_NOT_AVAILABLE'
      });
    }

    return reply.status(200).send({
      available: true
    });
  }

  /**
   * Public Profile Lookup (Req 91, 92, 110)
   * GET /api/v1/users/:username and GET /api/v1/u/:username
   * Strict allowlist: returns ONLY username, displayName, avatar, publicBio
   * NEVER returns internal user ID, email, phone, role, isPremium, period logs, medical journal, or location
   */
  static async getPublicProfile(request: FastifyRequest, reply: FastifyReply) {
    const params = request.params as { username?: string };
    const rawUsername = params.username;

    if (!rawUsername) {
      return reply.status(400).send({
        error: { code: 'INVALID_REQUEST', message: 'Username parameter is required.', requestId: request.requestId }
      });
    }

    const publicProfile = db.getPublicProfileByUsername(rawUsername);
    if (!publicProfile) {
      return reply.status(404).send({
        error: { code: 'NOT_FOUND', message: 'User profile not found.', requestId: request.requestId }
      });
    }

    return reply.status(200).send(publicProfile);
  }

  static async deleteAccount(request: FastifyRequest, reply: FastifyReply) {
    const userId = request.user!.id;

    await AuditService.recordEvent({
      eventType: 'account.delete.requested',
      userId,
      requestId: request.requestId,
      resourceType: 'user_account',
      resourceId: userId,
      result: 'success'
    });

    // Cascade delete user data (strictly using internal immutable user id)
    db.deleteUser(userId);

    await AuditService.recordEvent({
      eventType: 'account.deleted',
      userId,
      requestId: request.requestId,
      resourceType: 'user_account',
      resourceId: userId,
      result: 'success'
    });

    return reply.status(200).send({
      success: true,
      message: 'User account and all associated health records permanently deleted.',
      requestId: request.requestId
    });
  }

  static async exportAccount(request: FastifyRequest, reply: FastifyReply) {
    const userId = request.user!.id;

    await AuditService.recordEvent({
      eventType: 'account.export.requested',
      userId,
      requestId: request.requestId,
      resourceType: 'user_account',
      resourceId: userId,
      result: 'success'
    });

    const user = db.getUserById(userId);
    const profile = db.getProfile(userId);
    const periodLogs = db.getPeriodLogsByUserId(userId);
    const behaviourLogs = db.getBehaviourLogsByUserId(userId);
    const medicalJournals = db.getMedicalJournalsByUserId(userId);

    return reply.status(200).send({
      exportDate: new Date().toISOString(),
      account: {
        id: user?.id,
        email: user?.email,
        role: user?.role,
        isPremium: user?.isPremium
      },
      profile,
      periodLogs,
      behaviourLogs,
      medicalJournals
    });
  }
}
