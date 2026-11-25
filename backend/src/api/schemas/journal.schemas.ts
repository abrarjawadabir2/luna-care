import { z } from 'zod';
import { calendarDateSchema } from './calendar-date.js';

export const createMedicalJournalSchema = z.object({
  entryDate: calendarDateSchema,
  category: z.string().min(1).max(50),
  title: z.string().min(1).max(200),
  symptoms: z.array(z.string().max(100)).max(30).default([]),
  painLevel: z.number().int().min(0).max(10),
  mood: z.string().max(50).optional().nullable(),
  flowLevel: z.string().max(50).optional().nullable(),
  medicinesTaken: z.string().max(500).optional().nullable(),
  doctorVisit: z.boolean().default(false),
  nextAppointment: calendarDateSchema.optional().nullable(),
  notesEncrypted: z.string().max(10000).optional().nullable()
}).strict();
