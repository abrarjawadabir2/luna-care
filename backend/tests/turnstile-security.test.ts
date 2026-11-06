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
  // Real Siteverify responses must match BOTH intended action and hostname.
  test('5. Verified Turnstile responses require action and hostname binding', async () => {
    const originalSecret = process.env['TURNSTILE_SECRET_KEY'];
    const originalHostnames = process.env['TURNSTILE_ALLOWED_HOSTNAMES'];
    const originalFetch = globalThis.fetch;
    process.env['TURNSTILE_SECRET_KEY'] = 'test_secret_that_triggers_real_validation';
    process.env['TURNSTILE_ALLOWED_HOSTNAMES'] = 'lunacare.example';
    try {
      const verify = async (payload: Record<string, unknown>) => {
        globalThis.fetch = async () => new Response(JSON.stringify(payload), {
          status: 200,
          headers: { 'content-type': 'application/json' }
        });
        return TurnstileService.verifyToken({
          token: 'synthetic_challenge_token_12345',
          expectedAction: 'login'
        });
      };
      assert.equal((await verify({ success: true, hostname: 'lunacare.example' })).errorCode, 'ACTION_MISMATCH');
      assert.equal((await verify({ success: true, hostname: 'attacker.example', action: 'login' })).errorCode, 'HOSTNAME_MISMATCH');
      assert.equal((await verify({ success: true, hostname: 'lunacare.example', action: 'other' })).errorCode, 'ACTION_MISMATCH');
      assert.equal((await verify({ success: true, hostname: 'lunacare.example', action: 'login' })).success, true);
    } finally {
      globalThis.fetch = originalFetch;
      if (originalSecret === undefined) delete process.env['TURNSTILE_SECRET_KEY'];
      else process.env['TURNSTILE_SECRET_KEY'] = originalSecret;
      if (originalHostnames === undefined) delete process.env['TURNSTILE_ALLOWED_HOSTNAMES'];
      else process.env['TURNSTILE_ALLOWED_HOSTNAMES'] = originalHostnames;
    }
  });

  test('6. Real Turnstile validation fails closed without a configured hostname', async () => {
    const oldSecret = process.env['TURNSTILE_SECRET_KEY'];
    const oldHostnames = process.env['TURNSTILE_ALLOWED_HOSTNAMES'];
    process.env['TURNSTILE_SECRET_KEY'] = 'test_secret_that_triggers_real_validation';
    delete process.env['TURNSTILE_ALLOWED_HOSTNAMES'];
    try {
      const result = await TurnstileService.verifyToken({
        token: 'synthetic_challenge_token_12345',
        expectedAction: 'register'
      });
      assert.equal(result.success, false);
      assert.equal(result.errorCode, 'HOSTNAME_CONFIGURATION_REQUIRED');
    } finally {
      if (oldSecret === undefined) delete process.env['TURNSTILE_SECRET_KEY'];
      else process.env['TURNSTILE_SECRET_KEY'] = oldSecret;
      if (oldHostnames === undefined) delete process.env['TURNSTILE_ALLOWED_HOSTNAMES'];
      else process.env['TURNSTILE_ALLOWED_HOSTNAMES'] = oldHostnames;
    }
  });

});
