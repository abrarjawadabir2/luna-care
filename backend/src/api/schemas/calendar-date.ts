import { z } from 'zod';
import { isValidCalendarDate } from '../../utils/date.js';

/**
 * A string shaped YYYY-MM-DD is not necessarily a real calendar date:
 * "2026-02-30" must not become a period or medical appointment.
 */
export function isRealCalendarDate(value: string): boolean {
  return isValidCalendarDate(value);
}

export const calendarDateSchema = z.string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, 'Date must be YYYY-MM-DD')
  .refine(isRealCalendarDate, 'Enter a real calendar date.');
