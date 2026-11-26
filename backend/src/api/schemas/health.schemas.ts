import { z } from 'zod';
import { isValidCalendarDate } from '../../utils/date.js';

export const createBehaviourLogSchema = z.object({
  logDate: z.string().refine(isValidCalendarDate, 'logDate must be a valid calendar date in YYYY-MM-DD format'),
  mood: z.string().trim().min(1).max(50),
  stressLevel: z.number().int().min(0).max(10),
  anxietyLevel: z.number().int().min(0).max(10),
  sleepHours: z.number().min(0).max(24),
  sleepQuality: z.number().int().min(0).max(10),
  painLevel: z.number().int().min(0).max(10),
  energyLevel: z.number().int().min(0).max(10),
  hydrationLevel: z.enum(['Low', 'Okay', 'Good', 'Great']),
  flowLevel: z.string().max(50).optional().nullable(),
  symptoms: z.array(z.string().max(100)).max(30).default([]),
  notesEncrypted: z.string().max(5000).optional().nullable(),
  crisisFlag: z.boolean().default(false)
}).strict();
