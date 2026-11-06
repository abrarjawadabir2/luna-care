import { logger } from '../utils/logger.js';
import { AuditService } from '../audit/audit.service.js';

export interface TurnstileVerificationResult {
  success: boolean;
  bypassed?: boolean;
  errorCode?: string;
  errorMessage?: string;
  hostname?: string;
  action?: string;
}

const CLOUDFLARE_SITEVERIFY_URL = 'https://challenges.cloudflare.com/turnstile/v0/siteverify';

export class TurnstileService {
  /**
   * Verifies a Cloudflare Turnstile token on the backend server.
   * Turnstile secret key is read from process environment and never exposed to client bundles.
   */
  static async verifyToken(input: {
    token: string;
    remoteIp?: string;
    expectedAction?: string;
  }): Promise<TurnstileVerificationResult> {
    const { token, remoteIp, expectedAction } = input;

    // 1. Basic token validation
    if (!token || typeof token !== 'string') {
      return {
        success: false,
        errorCode: 'MISSING_TOKEN',
        errorMessage: 'Turnstile challenge token is required.'
      };
    }

    const trimmedToken = token.trim();
    if (trimmedToken.length < 10 || trimmedToken.length > 2048) {
      return {
        success: false,
        errorCode: 'INVALID_TOKEN_FORMAT',
        errorMessage: 'Invalid Turnstile token length.'
      };
    }

    const secretKey = process.env['TURNSTILE_SECRET_KEY'];
    const isTestOrDev = process.env['NODE_ENV'] !== 'production';

    // 2. Safe bypass in local test/dev when no secret key is provided
    if (!secretKey || secretKey.startsWith('mock_') || secretKey === 'placeholder') {
      if (isTestOrDev) {
        return {
          success: true,
          bypassed: true,
          action: expectedAction
        };
      }
      logger.error('turnstile.missing_secret_in_production');
      return {
        success: false,
        errorCode: 'SERVER_CONFIGURATION_ERROR',
        errorMessage: 'Bot verification is currently misconfigured on server.'
      };
    }

    // 3. Server-to-server call to Cloudflare Siteverify API
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000); // 5s timeout

      const formData = new URLSearchParams();
      formData.append('secret', secretKey);
      formData.append('response', trimmedToken);
      if (remoteIp) {
        formData.append('remoteip', remoteIp);
      }

      const response = await fetch(CLOUDFLARE_SITEVERIFY_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formData,
        signal: controller.signal
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        logger.warn('turnstile.siteverify_http_error', { status: response.status });
        return {
          success: false,
          errorCode: 'VERIFICATION_SERVICE_ERROR',
          errorMessage: 'Unable to verify bot challenge with Cloudflare.'
        };
      }

      const data = await response.json() as {
        success: boolean;
        'error-codes'?: string[];
        challenge_ts?: string;
        hostname?: string;
        action?: string;
      };

      if (!data.success) {
        const errorCodes = data['error-codes'] || [];
        logger.warn('turnstile.verification_failed', { errorCodes });
        await AuditService.recordEvent({
          eventType: 'security.rate_limit.triggered',
          resourceType: 'turnstile',
          result: 'denied',
          reasonCode: 'BOT_CHALLENGE_FAILED',
          metadata: { errorCodes }
        });

        return {
          success: false,
          errorCode: errorCodes[0] || 'CHALLENGE_FAILED',
          errorMessage: 'Bot challenge verification failed.'
        };
      }

      // 4. Validate expected action if specified
      if (expectedAction && data.action && data.action !== expectedAction) {
        logger.warn('turnstile.action_mismatch', { expected: expectedAction, received: data.action });
        return {
          success: false,
          errorCode: 'ACTION_MISMATCH',
          errorMessage: `Turnstile action mismatch: expected ${expectedAction}.`
        };
      }

      return {
        success: true,
        hostname: data.hostname,
        action: data.action
      };
    } catch (err: unknown) {
      logger.error('turnstile.verification_exception', {
        error: err instanceof Error ? err.message : 'Unknown error'
      });
      return {
        success: false,
        errorCode: 'VERIFICATION_REQUEST_FAILED',
        errorMessage: 'Bot challenge verification timed out or failed.'
      };
    }
  }
}
