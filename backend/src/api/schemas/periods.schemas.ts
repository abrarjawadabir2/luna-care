import { z } from 'zod';
import { calendarDateSchema } from './calendar-date.js';

export const createPeriodLogSchema = z.object({
  startDate: calendarDateSchema,
  endDate: calendarDateSchema.optional().nullable(),
  flowLevel: z.enum(['Spotting', 'Light', 'Medium', 'Heavy']),
  symptoms: z.array(z.string().max(100)).max(30).default([]),
  notesEncrypted: z.string().max(5000).optional().nullable()
}).strict().refine(
  period => !period.endDate || period.endDate >= period.startDate,
  { path: ['endDate'], message: 'End date cannot be before the start date.' }
);
