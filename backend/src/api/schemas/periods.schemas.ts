import { z } from 'zod';
import { isValidCalendarDate } from '../../utils/date.js';

const calendarDateSchema = (fieldName: string) =>
  z.string().refine(isValidCalendarDate, `${fieldName} must be a valid calendar date in YYYY-MM-DD format`);

export const createPeriodLogSchema = z.object({
  startDate: calendarDateSchema('startDate'),
  endDate: calendarDateSchema('endDate').optional().nullable(),
  flowLevel: z.enum(['Spotting', 'Light', 'Medium', 'Heavy']),
  symptoms: z.array(z.string().max(100)).max(30).default([]),
  notesEncrypted: z.string().max(5000).optional().nullable()
}).strict().refine(
  (data) => !data.endDate || data.endDate >= data.startDate,
  { message: 'endDate cannot be earlier than startDate', path: ['endDate'] }
);
