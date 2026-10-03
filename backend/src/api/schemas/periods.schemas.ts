import { z } from 'zod';

const dateRegex = /^\d{4}-\d{2}-\d{2}$/;

export const createPeriodLogSchema = z.object({
  startDate: z.string().regex(dateRegex, 'startDate must be YYYY-MM-DD'),
  endDate: z.string().regex(dateRegex, 'endDate must be YYYY-MM-DD').optional().nullable(),
  flowLevel: z.enum(['Spotting', 'Light', 'Medium', 'Heavy']),
  symptoms: z.array(z.string().max(100)).max(30).default([]),
  notesEncrypted: z.string().max(5000).optional().nullable()
}).strict();
