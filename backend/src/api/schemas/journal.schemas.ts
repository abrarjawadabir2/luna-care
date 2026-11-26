import { z } from 'zod';
import { isValidCalendarDate } from '../../utils/date.js';

const calendarDateSchema = (fieldName: string) =>
  z.string().refine(isValidCalendarDate, `${fieldName} must be a valid calendar date in YYYY-MM-DD format`);

export const createMedicalJournalSchema = z.object({
  entryDate: calendarDateSchema('entryDate'),
  category: z.string().trim().min(1).max(50),
  title: z.string().trim().min(1).max(200),
  symptoms: z.array(z.string().max(100)).max(30).default([]),
  painLevel: z.number().int().min(0).max(10),
  mood: z.string().max(50).optional().nullable(),
  flowLevel: z.string().max(50).optional().nullable(),
  medicinesTaken: z.string().max(500).optional().nullable(),
  doctorVisit: z.boolean().default(false),
  nextAppointment: calendarDateSchema('nextAppointment').optional().nullable(),
  notesEncrypted: z.string().max(10000).optional().nullable()
}).strict();
