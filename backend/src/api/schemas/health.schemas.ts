import { z } from 'zod';

const dateRegex = /^\d{4}-\d{2}-\d{2}$/;

export const createBehaviourLogSchema = z.object({
  logDate: z.string().regex(dateRegex, 'logDate must be YYYY-MM-DD'),
  mood: z.string().min(1).max(50),
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
