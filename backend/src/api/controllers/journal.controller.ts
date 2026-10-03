import crypto from 'node:crypto';
import type { FastifyRequest, FastifyReply } from 'fastify';
import { db } from '../../database/index.js';
import { createMedicalJournalSchema } from '../schemas/index.js';
import { requireOwnership } from '../middleware/auth.js';
import { AuditService } from '../../audit/audit.service.js';

export class JournalController {
  static async getMedicalJournals(request: FastifyRequest, reply: FastifyReply) {
    const userId = request.user!.id;
    const entries = db.getMedicalJournalsByUserId(userId);
    return reply.status(200).send({ entries });
  }

  static async createMedicalJournal(request: FastifyRequest, reply: FastifyReply) {
    const userId = request.user!.id;
    const input = createMedicalJournalSchema.parse(request.body);
    const now = new Date().toISOString();

    const record = db.createMedicalJournal({
      id: crypto.randomUUID(),
      userId,
      entryDate: input.entryDate,
      category: input.category,
      title: input.title,
      symptoms: input.symptoms,
      painLevel: input.painLevel,
      mood: input.mood || undefined,
      flowLevel: input.flowLevel || undefined,
      medicinesTaken: input.medicinesTaken || undefined,
      doctorVisit: input.doctorVisit,
      nextAppointment: input.nextAppointment || undefined,
      notesEncrypted: input.notesEncrypted || undefined,
      createdAt: now,
      updatedAt: now
    });

    await AuditService.recordEvent({
      eventType: 'journal.entry.created',
      userId,
      requestId: request.requestId,
      resourceType: 'journal_entry',
      resourceId: record.id,
      result: 'success'
    });

    return reply.status(201).send(record);
  }

  static async deleteMedicalJournal(request: FastifyRequest<{ Params: { id: string } }>, reply: FastifyReply) {
    const entryId = request.params.id;
    const existing = db.getMedicalJournalById(entryId);

    if (!existing) {
      return reply.status(404).send({
        error: { code: 'NOT_FOUND', message: 'Medical journal entry not found.', requestId: request.requestId }
      });
    }

    if (!requireOwnership(existing.userId, request, reply)) {
      return;
    }

    db.deleteMedicalJournal(entryId, existing.userId);

    await AuditService.recordEvent({
      eventType: 'journal.entry.deleted',
      userId: existing.userId,
      requestId: request.requestId,
      resourceType: 'journal_entry',
      resourceId: entryId,
      result: 'success'
    });

    return reply.status(200).send({ success: true, id: entryId });
  }
}

