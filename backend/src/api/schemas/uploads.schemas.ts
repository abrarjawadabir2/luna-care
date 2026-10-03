import { z } from 'zod';

export const uploadMetadataSchema = z.object({
  originalFilename: z.string().min(1).max(255),
  contentType: z.enum(['image/jpeg', 'image/png', 'image/webp', 'application/pdf']),
  base64Data: z.string().min(1, 'File content required')
}).strict();
