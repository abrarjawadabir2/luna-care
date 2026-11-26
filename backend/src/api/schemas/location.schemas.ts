import { z } from 'zod';

export const nearbySearchSchema = z.object({
  category: z.string().trim().min(1, 'Category cannot be empty').max(50),
  // Optional temporary city/country for approximate search
  city: z.string().trim().max(100).optional(),
  country: z.string().trim().max(100).optional()
  // NOTE: Exact GPS coordinates are intentionally NOT accepted or stored by backend
}).strict();
