import crypto from 'node:crypto';
import { logger } from '../utils/logger.js';
import { AuditService } from '../audit/audit.service.js';
import { db } from '../database/index.js';

const EXTENSION_BY_MIME: Record<string, string> = {
  'image/jpeg': '.jpg',
  'image/png': '.png',
  'image/webp': '.webp',
  'application/pdf': '.pdf',
};

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

    // PNG: full 8-byte signature 89 50 4E 47 0D 0A 1A 0A
    if (
      buffer.length >= 8 &&
      buffer[0] === 0x89 && buffer[1] === 0x50 && buffer[2] === 0x4E && buffer[3] === 0x47 &&
      buffer[4] === 0x0D && buffer[5] === 0x0A && buffer[6] === 0x1A && buffer[7] === 0x0A
    ) {
      return { valid: true, mime: 'image/png' };
    }

    // JPEG: FF D8 FF (require at least 4 bytes including marker byte)
    if (buffer.length >= 4 && buffer[0] === 0xFF && buffer[1] === 0xD8 && buffer[2] === 0xFF && buffer[3] !== 0x00) {
      return { valid: true, mime: 'image/jpeg' };
    }

    // PDF: %PDF- (25 50 44 46 2D)
    if (
      buffer.length >= 5 &&
      buffer[0] === 0x25 && buffer[1] === 0x50 && buffer[2] === 0x44 && buffer[3] === 0x46 && buffer[4] === 0x2D
    ) {
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
    // 1. Validate strict base64 syntax before decoding
    const compactBase64 = input.base64Data.replace(/\s+/g, '');
    if (
      compactBase64.length === 0 ||
      compactBase64.length % 4 !== 0 ||
      !/^[A-Za-z0-9+/]+={0,2}$/.test(compactBase64)
    ) {
      await AuditService.recordEvent({
        eventType: 'file.upload.rejected',
        userId: input.userId,
        resourceType: 'upload',
        result: 'failure',
        reasonCode: 'INVALID_BASE64_PAYLOAD',
        metadata: { claimed: input.claimedContentType }
      });

      return {
        valid: false,
        detectedMime: 'unknown',
        sizeBytes: 0,
        safeKey: '',
        error: 'Malformed base64 file payload.'
      };
    }

    const buffer = Buffer.from(compactBase64, 'base64');
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

    // 4. Generate random, unguessable storage key (UUID) to prevent path traversal & overwrites.
    // Derive extension strictly from verified magic-byte MIME type (never trust client-supplied filename extension).
    const extension = EXTENSION_BY_MIME[inspection.mime] ?? '.bin';
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
