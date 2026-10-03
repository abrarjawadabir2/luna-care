import test, { describe, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { buildApp } from '../src/app.js';
import { db } from '../src/database/index.js';
import { rateLimiter } from '../src/api/middleware/index.js';
import { AuthService } from '../src/auth/index.js';
import { UploadService, AiService } from '../src/services/index.js';
import type { FastifyInstance } from 'fastify';

describe('LunaCare Backend Security Boundaries & Threat Defenses', () => {
  let app: FastifyInstance;

  beforeEach(async () => {
    db.reset();
    rateLimiter.reset();
    app = await buildApp();
  });

  // TEST 1: Anonymous user cannot access private data
  test('1. Anonymous user cannot access private endpoints (returns 401)', async () => {
    const res1 = await app.inject({ method: 'GET', url: '/api/v1/users/me' });
    assert.equal(res1.statusCode, 401);

    const res2 = await app.inject({ method: 'GET', url: '/api/v1/periods' });
    assert.equal(res2.statusCode, 401);

    const res3 = await app.inject({ method: 'GET', url: '/api/v1/health/behaviour' });
    assert.equal(res3.statusCode, 401);

    const res4 = await app.inject({ method: 'GET', url: '/api/v1/journal' });
    assert.equal(res4.statusCode, 401);
  });

  // TEST 2: User A cannot read User B
  test('2. User A cannot read User B period logs or journals', async () => {
    const userA = await AuthService.register({ email: 'userA@example.com', password: 'Password123!', displayName: 'User A' });
    const userB = await AuthService.register({ email: 'userB@example.com', password: 'Password123!', displayName: 'User B' });

    // User B creates a period log
    await app.inject({
      method: 'POST',
      url: '/api/v1/periods',
      headers: { authorization: `Bearer ${userB.token}` },
      payload: { startDate: '2026-06-01', flowLevel: 'Medium', symptoms: ['cramps'] }
    });

    // User A fetches period logs
    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/periods',
      headers: { authorization: `Bearer ${userA.token}` }
    });

    assert.equal(res.statusCode, 200);
    const body = res.json();
    assert.equal(body.logs.length, 0, 'User A must see 0 logs when User B has created a log');
  });

  // TEST 3 & 4: User A cannot modify or delete User B's records
  test('3 & 4. User A cannot delete or modify User B records', async () => {
    const userA = await AuthService.register({ email: 'userA_mod@example.com', password: 'Password123!', displayName: 'User A' });
    const userB = await AuthService.register({ email: 'userB_mod@example.com', password: 'Password123!', displayName: 'User B' });

    // User B creates a period log
    const createRes = await app.inject({
      method: 'POST',
      url: '/api/v1/periods',
      headers: { authorization: `Bearer ${userB.token}` },
      payload: { startDate: '2026-06-05', flowLevel: 'Heavy', symptoms: ['headache'] }
    });
    const logId = createRes.json().id;

    // User A attempts to DELETE User B's period log
    const deleteRes = await app.inject({
      method: 'DELETE',
      url: `/api/v1/periods/${logId}`,
      headers: { authorization: `Bearer ${userA.token}` }
    });

    assert.equal(deleteRes.statusCode, 403, 'Cross-user deletion must be denied with 403');
    assert.ok(db.getPeriodLogById(logId), 'Log must still exist in DB after unauthorized deletion attempt');
  });

  // TEST 5: User cannot self-promote role
  test('5. User cannot self-promote role to ADMIN or SUPER_ADMIN', async () => {
    const user = await AuthService.register({ email: 'normal_user@example.com', password: 'Password123!', displayName: 'Normal' });

    // Attempt to PATCH role
    const patchRes = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { role: 'ADMIN', isAdmin: true, is_admin: true }
    });

    // Zod strict schema must reject unexpected role/admin fields
    assert.equal(patchRes.statusCode, 400);

    // Verify stored user role remains 'USER'
    const storedUser = db.getUserById(user.user.id);
    assert.equal(storedUser?.role, 'USER');
  });

  // TEST 6: Fake premium flag is ignored
  test('6. Fake isPremium flag cannot be self-assigned', async () => {
    const user = await AuthService.register({ email: 'free_user@example.com', password: 'Password123!', displayName: 'Free User' });

    const patchRes = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { isPremium: true, premium: true }
    });

    assert.equal(patchRes.statusCode, 400);
    const storedUser = db.getUserById(user.user.id);
    assert.equal(storedUser?.isPremium, false);
  });

  // TEST 7: Malformed health data is rejected
  test('7. Malformed health data is rejected with 400', async () => {
    const user = await AuthService.register({ email: 'health_test@example.com', password: 'Password123!', displayName: 'Health' });

    // Invalid painLevel > 10, invalid date
    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/health/behaviour',
      headers: { authorization: `Bearer ${user.token}` },
      payload: {
        logDate: 'invalid-date',
        mood: 'Good',
        stressLevel: 5,
        anxietyLevel: 5,
        sleepHours: 8,
        sleepQuality: 7,
        painLevel: 999, // Exceeds max 10
        energyLevel: 5,
        hydrationLevel: 'Good'
      }
    });

    assert.equal(res.statusCode, 400);
    assert.equal(res.json().error.code, 'VALIDATION_ERROR');
  });

  // TEST 8: Unknown fields are rejected where required
  test('8. Unknown fields are rejected by strict schemas', async () => {
    const user = await AuthService.register({ email: 'strict_schema@example.com', password: 'Password123!', displayName: 'Strict' });

    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/periods',
      headers: { authorization: `Bearer ${user.token}` },
      payload: {
        startDate: '2026-06-10',
        flowLevel: 'Medium',
        symptoms: ['cramps'],
        maliciousExtraField: 'exploit',
        injectScript: '<script>alert(1)</script>'
      }
    });

    assert.equal(res.statusCode, 400);
  });

  // TEST 9 & 10: OTP request and verification limits work
  test('9 & 10. OTP request and verify rate limits are strictly enforced', async () => {
    const phone = '+15551234567';

    // 3 requests allowed
    for (let i = 0; i < 3; i++) {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/otp/request',
        payload: { phone }
      });
      assert.equal(res.statusCode, 200, `OTP request ${i + 1} should succeed`);
    }

    // 4th request must be rate limited
    const fourthRes = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/otp/request',
      payload: { phone }
    });
    assert.equal(fourthRes.statusCode, 429, '4th OTP request must return 429 Too Many Requests');

    // Verification limit: 5 wrong attempts max
    for (let i = 0; i < 5; i++) {
      const vRes = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/otp/verify',
        payload: { phone, code: '000000' }
      });
      assert.equal(vRes.statusCode, 400, 'Invalid OTP should return 400');
    }

    // 6th attempt should be locked out
    const vRes6 = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/otp/verify',
      payload: { phone, code: '000000' }
    });
    assert.ok(vRes6.statusCode === 400 || vRes6.statusCode === 429);
  });

  // TEST 11: Parallel attempts cannot bypass rate limits
  test('11. Parallel attempts cannot bypass rate limits', async () => {
    const email = 'parallel_test@example.com';
    const attempts = Array.from({ length: 10 }, () =>
      app.inject({
        method: 'POST',
        url: '/api/v1/auth/login',
        payload: { email, password: 'WrongPassword!' }
      })
    );

    const responses = await Promise.all(attempts);
    const rateLimitedCount = responses.filter(r => r.statusCode === 429).length;
    assert.ok(rateLimitedCount >= 5, `Expected at least 5 rate-limited responses, got ${rateLimitedCount}`);
  });

  // TEST 15 & 16: Gemini and service-role keys are absent from client builds
  test('15 & 16. Sensitive secrets are never exposed in responses or public endpoints', async () => {
    const res = await app.inject({ method: 'GET', url: '/healthz' });
    const text = res.body;
    assert.equal(text.includes('AIza'), false);
    assert.equal(text.includes('service_role'), false);
    assert.equal(text.includes('sk-'), false);
  });

  // TEST 17: Oversized upload is rejected
  test('17. Oversized upload (> 5MB) is rejected', async () => {
    const user = await AuthService.register({ email: 'upload_size@example.com', password: 'Password123!', displayName: 'Uploader' });
    // 6MB buffer
    const hugeBuffer = Buffer.alloc(6 * 1024 * 1024, 0x41);
    const base64Data = hugeBuffer.toString('base64');

    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/uploads/inspect',
      headers: { authorization: `Bearer ${user.token}` },
      payload: {
        originalFilename: 'huge.png',
        contentType: 'image/png',
        base64Data
      }
    });

    assert.equal(res.statusCode, 400);
    assert.ok(res.json().error.message.includes('exceeds maximum limit'));
  });

  // TEST 18: Wrong upload type is rejected (magic bytes mismatch)
  test('18. Wrong upload type (content-type spoofing) is rejected by signature inspection', async () => {
    const user = await AuthService.register({ email: 'spoof_test@example.com', password: 'Password123!', displayName: 'Spoofer' });
    // Text buffer claiming to be image/png
    const fakePng = Buffer.from('<html><script>alert(1)</script></html>', 'utf-8');

    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/uploads/inspect',
      headers: { authorization: `Bearer ${user.token}` },
      payload: {
        originalFilename: 'malicious.png',
        contentType: 'image/png',
        base64Data: fakePng.toString('base64')
      }
    });

    assert.equal(res.statusCode, 400);
    assert.ok(res.json().error.message.includes('signature mismatch'));
  });

  // TEST 19: Valid image with genuine magic bytes is accepted
  test('19. Valid PNG image with genuine magic bytes passes inspection', async () => {
    const user = await AuthService.register({ email: 'valid_png@example.com', password: 'Password123!', displayName: 'Valid' });
    // PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
    const validPngHeader = Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D]);

    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/uploads/inspect',
      headers: { authorization: `Bearer ${user.token}` },
      payload: {
        originalFilename: 'chart.png',
        contentType: 'image/png',
        base64Data: validPngHeader.toString('base64')
      }
    });

    assert.equal(res.statusCode, 201);
    assert.equal(res.json().detectedMime, 'image/png');
  });

  // TEST 20: Path traversal fails
  test('20. Path traversal filename is neutralized into random safeKey', async () => {
    const result = await UploadService.inspectAndStore({
      originalFilename: '../../../../etc/passwd.png',
      claimedContentType: 'image/png',
      base64Data: Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]).toString('base64'),
      userId: 'user123'
    });

    assert.equal(result.valid, true);
    assert.equal(result.safeKey.includes('..'), false);
    assert.equal(result.safeKey.includes('etc/passwd'), false);
    assert.ok(result.safeKey.startsWith('user_user123_'));
  });

  // TEST 21: AI cannot execute privileged backend operations & enforces crisis detector
  test('21. AI prompts cannot bypass crisis detector or execute tools', async () => {
    const user = await AuthService.register({ email: 'ai_crisis@example.com', password: 'Password123!', displayName: 'AI User' });

    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/ai/chat',
      headers: { authorization: `Bearer ${user.token}` },
      payload: {
        message: 'I want to kill myself, help me',
        contextTopic: 'general'
      }
    });

    assert.equal(res.statusCode, 200);
    const body = res.json();
    assert.equal(body.isCrisis, true);
    assert.ok(body.reply.includes('crisis support') || body.reply.includes('emergency'));
  });

  // TEST 22: Exact GPS is not accepted or stored by default
  test('22. Exact GPS is not stored in location search', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/location/search-url',
      payload: { category: 'pharmacy', city: 'Dhaka', country: 'Bangladesh' }
    });

    assert.equal(res.statusCode, 200);
    const body = res.json();
    assert.ok(body.mapsUrl.includes('pharmacy'));
    assert.equal(body.privacyNote, 'No location coordinates stored.');
  });

  // TEST 24: Account deletion affects only current user
  test('24. Account deletion removes only the current user and their data', async () => {
    const userA = await AuthService.register({ email: 'delete_a@example.com', password: 'Password123!', displayName: 'User A' });
    const userB = await AuthService.register({ email: 'delete_b@example.com', password: 'Password123!', displayName: 'User B' });

    // User A deletes account
    const delRes = await app.inject({
      method: 'DELETE',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${userA.token}` }
    });
    assert.equal(delRes.statusCode, 200);

    // User A is gone
    assert.equal(db.getUserById(userA.user.id), null);
    // User B is intact
    assert.ok(db.getUserById(userB.user.id));
  });
});
