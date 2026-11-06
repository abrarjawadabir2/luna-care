import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import { TurnstileService } from '../src/services/turnstile.service.js';

describe('LunaCare Cloudflare Turnstile Verification Security Suite', () => {

  // TEST 1: Missing or empty token rejected
  test('1. TurnstileService rejects empty or non-string tokens', async () => {
    // @ts-expect-error Testing invalid runtime input
    const res1 = await TurnstileService.verifyToken({ token: null });
    assert.equal(res1.success, false);
    assert.equal(res1.errorCode, 'MISSING_TOKEN');

    const res2 = await TurnstileService.verifyToken({ token: '' });
    assert.equal(res2.success, false);
    assert.equal(res2.errorCode, 'MISSING_TOKEN');
  });

  // TEST 2: Abnormal token lengths rejected
  test('2. TurnstileService rejects abnormally short or oversized tokens', async () => {
    const res1 = await TurnstileService.verifyToken({ token: 'short' });
    assert.equal(res1.success, false);
    assert.equal(res1.errorCode, 'INVALID_TOKEN_FORMAT');

    const hugeToken = 'a'.repeat(3000);
    const res2 = await TurnstileService.verifyToken({ token: hugeToken });
    assert.equal(res2.success, false);
    assert.equal(res2.errorCode, 'INVALID_TOKEN_FORMAT');
  });

  // TEST 3: Safe bypass in test/dev environment when secret is unconfigured
  test('3. TurnstileService safely bypasses verification in test environment when key is omitted', async () => {
    const originalSecret = process.env['TURNSTILE_SECRET_KEY'];
    delete process.env['TURNSTILE_SECRET_KEY'];

    try {
      const res = await TurnstileService.verifyToken({
        token: 'valid_test_token_1234567890',
        expectedAction: 'login'
      });

      assert.equal(res.success, true);
      assert.equal(res.bypassed, true);
      assert.equal(res.action, 'login');
    } finally {
      if (originalSecret) {
        process.env['TURNSTILE_SECRET_KEY'] = originalSecret;
      }
    }
  });

  // TEST 4: Mock secret format handling
  test('4. TurnstileService handles mock secret prefix in dev/test environment', async () => {
    process.env['TURNSTILE_SECRET_KEY'] = 'mock_secret_key_12345';

    try {
      const res = await TurnstileService.verifyToken({
        token: 'sample_turnstile_response_token_abc123',
        expectedAction: 'register'
      });

      assert.equal(res.success, true);
      assert.equal(res.bypassed, true);
      assert.equal(res.action, 'register');
    } finally {
      delete process.env['TURNSTILE_SECRET_KEY'];
    }
  });
});
