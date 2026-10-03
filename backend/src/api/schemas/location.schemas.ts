import { z } from 'zod';

export const nearbySearchSchema = z.object({
  category: z.string().min(1).max(50),
  // Optional temporary city/country for approximate search
  city: z.string().max(100).optional(),
  country: z.string().max(100).optional()
  // NOTE: Exact GPS coordinates are intentionally NOT accepted or stored by backend
}).strict();
