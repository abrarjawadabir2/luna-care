import { z } from 'zod';

// Explicit allowlist of writable profile fields.
// FORBIDDEN / STRIPPED: role, isPremium, aiMessagesUsedThisMonth, id, createdAt, updatedAt
export const updateProfileSchema = z.object({
  displayName: z.string().min(1).max(100).optional(),
  username: z.string().min(3).max(30).optional(),
  avatar: z.string().max(500).optional().nullable(),
  bio: z.string().max(500).optional().nullable(),
  userMode: z.enum(['SELF_TRACKING', 'SUPPORT_MODE', 'EDUCATION_ONLY']).optional(),
  genderMode: z.string().max(50).optional(),
  pronoun: z.string().max(50).optional(),
  customPronoun: z.string().max(50).optional().nullable(),
  bodyRelevantMode: z.string().max(50).optional(),
  supportRelationship: z.string().max(50).optional().nullable(),
  religion: z.string().max(50).optional().nullable(),
  country: z.string().max(100).optional().nullable(),
  region: z.string().max(100).optional().nullable(),
  city: z.string().max(100).optional().nullable(),
  locationPrivacyMode: z.enum(['OFF', 'ON_DEVICE_ONLY', 'APPROXIMATE_REGION', 'TEMPORARY_EXACT']).optional(),
  selectedConditions: z.array(z.string().max(100)).max(20).optional(),
  behaviourFocuses: z.array(z.string().max(100)).max(30).optional(),
  averageCycleLength: z.number().int().min(15).max(60).optional(),
  averagePeriodLength: z.number().int().min(1).max(20).optional()
}).strict(); // Any other fields (like role, id, userId, isAdmin, isStaff, subscriptionStatus, emailVerified, createdAt, audit fields) are strictly REJECTED

export const claimUsernameSchema = z.object({
  username: z.string().min(1).max(50)
}).strict();

export const usernameAvailabilityQuerySchema = z.object({
  username: z.string().min(1).max(50)
});

