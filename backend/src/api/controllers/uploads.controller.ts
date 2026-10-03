import type { FastifyRequest, FastifyReply } from 'fastify';
import { uploadMetadataSchema } from '../schemas/index.js';
import { UploadService } from '../../services/index.js';

export class UploadsController {
  static async inspect(request: FastifyRequest, reply: FastifyReply) {
    const user = request.user!;
    const data = uploadMetadataSchema.parse(request.body);

    const result = await UploadService.inspectAndStore({
      originalFilename: data.originalFilename,
      claimedContentType: data.contentType,
      base64Data: data.base64Data,
      userId: user.id
    });

    if (!result.valid) {
      return reply.status(400).send({
        error: {
          code: 'INVALID_FILE_PAYLOAD',
          message: result.error || 'File validation failed.',
          requestId: request.requestId
        }
      });
    }

    return reply.status(201).send({
      success: true,
      safeKey: result.safeKey,
      detectedMime: result.detectedMime,
      sizeBytes: result.sizeBytes
    });
  }
}
