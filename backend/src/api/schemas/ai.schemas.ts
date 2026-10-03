import { z } from 'zod';

export const aiChatSchema = z.object({
  message: z.string().min(1, 'Message cannot be empty').max(1000, 'Message cannot exceed 1000 characters'),
  // Explicitly allow ONLY broad educational context (e.g. current cycle phase or topic), NEVER raw notes or GPS
  contextTopic: z.enum(['period_education', 'menstrual_cup', 'mood_wellbeing', 'self_care', 'general']).default('general')
}).strict();
