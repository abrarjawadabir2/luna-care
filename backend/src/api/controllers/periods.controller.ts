import crypto from 'node:crypto';
import type { FastifyRequest, FastifyReply } from 'fastify';
import { db } from '../../database/index.js';
import { createPeriodLogSchema } from '../schemas/index.js';
import { requireOwnership } from '../middleware/auth.js';

export class PeriodsController {
  static async getPeriodLogs(request: FastifyRequest, reply: FastifyReply) {
    const userId = request.user!.id;
    const logs = db.getPeriodLogsByUserId(userId);
    return reply.status(200).send({ logs });
  }

  static async createPeriodLog(request: FastifyRequest, reply: FastifyReply) {
    const userId = request.user!.id;
    const input = createPeriodLogSchema.parse(request.body);
    const now = new Date().toISOString();

    const record = db.createPeriodLog({
      id: crypto.randomUUID(),
      userId,
      startDate: input.startDate,
      endDate: input.endDate || undefined,
      flowLevel: input.flowLevel,
      symptoms: input.symptoms,
      notesEncrypted: input.notesEncrypted || undefined,
      createdAt: now,
      updatedAt: now
    });

    return reply.status(201).send(record);
  }

  static async deletePeriodLog(request: FastifyRequest<{ Params: { id: string } }>, reply: FastifyReply) {
    const logId = request.params.id;
    const existing = db.getPeriodLogById(logId);

    if (!existing) {
      return reply.status(404).send({
        error: { code: 'NOT_FOUND', message: 'Period log not found.', requestId: request.requestId }
      });
    }

    if (!requireOwnership(existing.userId, request, reply)) {
      return;
    }

    db.deletePeriodLog(logId, existing.userId);
    return reply.status(200).send({ success: true, id: logId });
  }
}
