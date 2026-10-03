import { z } from 'zod';

const dateRegex = /^\d{4}-\d{2}-\d{2}$/;

export const createMedicalJournalSchema = z.object({
  entryDate: z.string().regex(dateRegex, 'entryDate must be YYYY-MM-DD'),
  category: z.string().min(1).max(50),
  title: z.string().min(1).max(200),
  symptoms: z.array(z.string().max(100)).max(30).default([]),
  painLevel: z.number().int().min(0).max(10),
  mood: z.string().max(50).optional().nullable(),
  flowLevel: z.string().max(50).optional().nullable(),
  medicinesTaken: z.string().max(500).optional().nullable(),
  doctorVisit: z.boolean().default(false),
  nextAppointment: z.string().regex(dateRegex).optional().nullable(),
  notesEncrypted: z.string().max(10000).optional().nullable()
}).strict();
