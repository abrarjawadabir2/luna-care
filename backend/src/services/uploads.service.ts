import crypto from 'node:crypto';
import path from 'node:path';
import { logger } from '../utils/logger.js';
import { AuditService } from '../audit/audit.service.js';
import { db } from '../database/index.js';

export interface InspectionResult {
  valid: boolean;
  detectedMime: string;
  sizeBytes: number;
  safeKey: string;
  error?: string;
}

export class UploadService {
  private static MAX_BYTES = 5 * 1024 * 1024; // 5 MB

  /**
   * Inspects magic bytes to verify true file type regardless of client-supplied extension/MIME.
   */
  static inspectMagicBytes(buffer: Buffer): { valid: boolean; mime: string } {
    if (buffer.length < 4) return { valid: false, mime: 'unknown' };

    // PNG: 89 50 4E 47 0D 0A 1A 0A
    if (buffer[0] === 0x89 && buffer[1] === 0x50 && buffer[2] === 0x4E && buffer[3] === 0x47) {
      return { valid: true, mime: 'image/png' };
    }

    // JPEG: FF D8 FF
    if (buffer[0] === 0xFF && buffer[1] === 0xD8 && buffer[2] === 0xFF) {
      return { valid: true, mime: 'image/jpeg' };
    }

    // PDF: %PDF (25 50 44 46)
    if (buffer[0] === 0x25 && buffer[1] === 0x50 && buffer[2] === 0x44 && buffer[3] === 0x46) {
      return { valid: true, mime: 'application/pdf' };
    }

    // WEBP: RIFF....WEBP
    if (
      buffer.length >= 12 &&
      buffer[0] === 0x52 && buffer[1] === 0x49 && buffer[2] === 0x46 && buffer[3] === 0x46 &&
      buffer[8] === 0x57 && buffer[9] === 0x45 && buffer[10] === 0x42 && buffer[11] === 0x50
    ) {
      return { valid: true, mime: 'image/webp' };
    }

    return { valid: false, mime: 'unknown' };
  }

  static async inspectAndStore(input: {
    originalFilename: string;
    claimedContentType: string;
    base64Data: string;
    userId: string;
  }): Promise<InspectionResult> {
    // 1. Decode base64 buffer safely
    const buffer = Buffer.from(input.base64Data, 'base64');
    const sizeBytes = buffer.length;

    // 2. Enforce file size limit
    if (sizeBytes > this.MAX_BYTES) {
      await AuditService.recordEvent({
        eventType: 'file.upload.rejected',
        userId: input.userId,
        resourceType: 'upload',
        result: 'failure',
        reasonCode: 'FILE_SIZE_LIMIT_EXCEEDED',
        metadata: { sizeBytes, maxBytes: this.MAX_BYTES }
      });

      return {
        valid: false,
        detectedMime: 'unknown',
        sizeBytes,
        safeKey: '',
        error: `File exceeds maximum limit of 5MB (got ${(sizeBytes / 1024 / 1024).toFixed(2)}MB)`
      };
    }

    // 3. Inspect magic bytes signature (never trust Content-Type)
    const inspection = this.inspectMagicBytes(buffer);
    if (!inspection.valid || inspection.mime !== input.claimedContentType) {
      logger.warn('upload.signature_mismatch', {
        claimed: input.claimedContentType,
        detected: inspection.mime,
        sizeBytes
      });

      await AuditService.recordEvent({
        eventType: 'file.upload.rejected',
        userId: input.userId,
        resourceType: 'upload',
        result: 'failure',
        reasonCode: 'SIGNATURE_MISMATCH',
        metadata: { claimed: input.claimedContentType, detected: inspection.mime, sizeBytes }
      });

      return {
        valid: false,
        detectedMime: inspection.mime,
        sizeBytes,
        safeKey: '',
        error: `File signature mismatch. Claimed ${input.claimedContentType} but detected ${inspection.mime}`
      };
    }

    // 4. Generate random, unguessable storage key (UUID) to prevent path traversal & overwrites
    // Strip any path traversal attempts from original filename
    const sanitizedExt = path.extname(path.basename(input.originalFilename)).toLowerCase().replace(/[^a-z0-9.]/g, '');
    const extension = sanitizedExt || (inspection.mime === 'image/jpeg' ? '.jpg' : inspection.mime === 'image/png' ? '.png' : inspection.mime === 'image/webp' ? '.webp' : '.pdf');
    const safeKey = `user_${input.userId}_${crypto.randomUUID()}${extension}`;
    const uploadId = crypto.randomUUID();

    db.saveUpload({
      id: uploadId,
      userId: input.userId,
      safeKey,
      detectedMime: inspection.mime,
      sizeBytes,
      status: 'CONFIRMED',
      createdAt: Date.now(),
      expiresAt: Date.now() + 7 * 24 * 60 * 60 * 1000
    });

    logger.info('upload.inspected_successfully', {
      userId: input.userId,
      safeKey,
      detectedMime: inspection.mime,
      sizeBytes
    });

    await AuditService.recordEvent({
      eventType: 'file.upload.accepted',
      userId: input.userId,
      resourceType: 'upload',
      resourceId: safeKey,
      result: 'success',
      metadata: { detectedMime: inspection.mime, sizeBytes }
    });

    return {
      valid: true,
      detectedMime: inspection.mime,
      sizeBytes,
      safeKey
    };
  }
}
