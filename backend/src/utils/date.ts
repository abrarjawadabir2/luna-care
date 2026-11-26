const ISO_DATE_REGEX = /^(\d{4})-(\d{2})-(\d{2})$/;

/**
 * Validates that a YYYY-MM-DD string represents a real calendar date
 * (e.g. rejects 2026-02-29, 2026-02-30, 2026-04-31 while accepting 2024-02-29).
 */
export function isValidCalendarDate(value: string): boolean {
  const match = ISO_DATE_REGEX.exec(value);
  if (!match) return false;

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);

  if (month < 1 || month > 12 || day < 1 || day > 31) {
    return false;
  }

  const parsed = new Date(Date.UTC(year, month - 1, day));
  return (
    parsed.getUTCFullYear() === year &&
    parsed.getUTCMonth() === month - 1 &&
    parsed.getUTCDate() === day
  );
}
