import crypto from 'node:crypto';
import type { FastifyRequest, FastifyReply } from 'fastify';
import { db } from '../../database/index.js';
import { createBehaviourLogSchema } from '../schemas/index.js';
import { requireOwnership } from '../middleware/auth.js';

export class HealthController {
  static async getBehaviourLogs(request: FastifyRequest, reply: FastifyReply) {
    const userId = request.user!.id;
    const logs = db.getBehaviourLogsByUserId(userId);
    return reply.status(200).send({ logs });
  }

  static async createBehaviourLog(request: FastifyRequest, reply: FastifyReply) {
    const userId = request.user!.id;
    const input = createBehaviourLogSchema.parse(request.body);
    const now = new Date().toISOString();

    const record = db.createBehaviourLog({
      id: crypto.randomUUID(),
      userId,
      logDate: input.logDate,
      mood: input.mood,
      stressLevel: input.stressLevel,
      anxietyLevel: input.anxietyLevel,
      sleepHours: input.sleepHours,
      sleepQuality: input.sleepQuality,
      painLevel: input.painLevel,
      energyLevel: input.energyLevel,
      hydrationLevel: input.hydrationLevel,
      flowLevel: input.flowLevel || undefined,
      symptoms: input.symptoms,
      notesEncrypted: input.notesEncrypted || undefined,
      crisisFlag: input.crisisFlag,
      flags: [],
      createdAt: now,
      updatedAt: now
    });

    return reply.status(201).send(record);
  }

  static async deleteBehaviourLog(request: FastifyRequest<{ Params: { id: string } }>, reply: FastifyReply) {
    const logId = request.params.id;
    const existing = db.getBehaviourLogById(logId);

    if (!existing) {
      return reply.status(404).send({
        error: { code: 'NOT_FOUND', message: 'Behaviour log not found.', requestId: request.requestId }
      });
    }

    if (!requireOwnership(existing.userId, request, reply)) {
      return;
    }

    db.deleteBehaviourLog(logId, existing.userId);
    return reply.status(200).send({ success: true, id: logId });
  }
}
