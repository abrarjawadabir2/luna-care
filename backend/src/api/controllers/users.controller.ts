import type { FastifyRequest, FastifyReply } from 'fastify';
import { db } from '../../database/index.js';
import { updateProfileSchema } from '../schemas/index.js';
import { AuditService } from '../../audit/audit.service.js';

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

    const updated = db.saveProfile({
      ...existing,
      ...updates,
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

    // Cascade delete user data
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
