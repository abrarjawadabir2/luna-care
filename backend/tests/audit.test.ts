import test, { describe, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { sanitizeAuditMetadata, isProhibitedKey } from '../src/audit/audit.sanitizer.js';
import { AuditService } from '../src/audit/audit.service.js';
import { db } from '../src/database/index.js';
import { buildApp } from '../src/app.js';
import { signUserToken } from '../src/api/middleware/auth.js';
import type { FastifyInstance } from 'fastify';

describe('LunaCare Audit System & Privacy Sanitizer', () => {
  let app: FastifyInstance;

  beforeEach(async () => {
    db.reset();
    app = await buildApp();
  });

  // TEST 1: Sanitizer detects and removes prohibited sensitive fields
  test('1. Audit sanitizer removes all prohibited sensitive fields', () => {
    const rawMetadata = {
      safeUserId: '12345',
      action: 'login',
      password: 'PlaintextPassword123!',
      otp: '123456',
      token: 'jwt.token.secret',
      accessToken: 'access_secret_123',
      refreshToken: 'refresh_secret_123',
      authorization: 'Bearer supersecret',
      cookie: 'session_id=123',
      medicalNote: 'Patient diagnosed with endometriosis',
      journalContent: 'Severe pelvic pain and heavy bleeding',
      notesEncrypted: 'vault_encrypted_data',
      exactGps: '37.7749,-122.4194',
      latitude: 37.7749,
      longitude: -122.4194,
      geminiApiKey: 'AIzaSyA_secret_gemini_key',
      safeCount: 42
    };

    const sanitized = sanitizeAuditMetadata(rawMetadata) as Record<string, unknown>;

    // Prohibited fields must be stripped completely
    assert.equal(sanitized['password'], undefined);
    assert.equal(sanitized['otp'], undefined);
    assert.equal(sanitized['token'], undefined);
    assert.equal(sanitized['accessToken'], undefined);
    assert.equal(sanitized['refreshToken'], undefined);
    assert.equal(sanitized['authorization'], undefined);
    assert.equal(sanitized['cookie'], undefined);
    assert.equal(sanitized['medicalNote'], undefined);
    assert.equal(sanitized['journalContent'], undefined);
    assert.equal(sanitized['notesEncrypted'], undefined);
    assert.equal(sanitized['exactGps'], undefined);
    assert.equal(sanitized['latitude'], undefined);
    assert.equal(sanitized['longitude'], undefined);
    assert.equal(sanitized['geminiApiKey'], undefined);

    // Non-prohibited operational fields must remain intact
    assert.equal(sanitized['safeUserId'], '12345');
    assert.equal(sanitized['action'], 'login');
    assert.equal(sanitized['safeCount'], 42);
  });

  // TEST 2: Helper isProhibitedKey flags variations accurately
  test('2. isProhibitedKey flags case-insensitive and snake_case sensitive keys', () => {
    assert.ok(isProhibitedKey('password'));
    assert.ok(isProhibitedKey('PASSWORD'));
    assert.ok(isProhibitedKey('api_key'));
    assert.ok(isProhibitedKey('geminiApiKey'));
    assert.ok(isProhibitedKey('symptom_notes'));
    assert.ok(isProhibitedKey('exact_gps'));
    assert.equal(isProhibitedKey('userId'), false);
    assert.equal(isProhibitedKey('requestId'), false);
  });

  // TEST 3: Audit service records safe structured event
  test('3. Audit event contains event type, result, request ID, safe reason code, and timestamp', async () => {
    const event = await AuditService.recordEvent({
      eventType: 'auth.login.failure',
      userId: 'user_test_1',
      requestId: 'req_12345',
      resourceType: 'user',
      resourceId: 'user_test_1',
      result: 'failure',
      reasonCode: 'INVALID_CREDENTIALS',
      metadata: {
        accountHash: 'sha256_mock_hash',
        password: 'AttemptedPassword123!' // MUST BE SANITIZED OUT
      }
    });

    assert.ok(event.id);
    assert.equal(event.eventType, 'auth.login.failure');
    assert.equal(event.userId, 'user_test_1');
    assert.equal(event.requestId, 'req_12345');
    assert.equal(event.result, 'failure');
    assert.equal(event.reasonCode, 'INVALID_CREDENTIALS');
    assert.ok(event.timestamp instanceof Date);
    assert.equal(event.metadata?.['password'], undefined);
    assert.equal(event.metadata?.['accountHash'], 'sha256_mock_hash');
  });

  // TEST 4: Non-admin users cannot access audit logs endpoint
  test('4. Non-admin users cannot access audit logs (returns 403)', async () => {
    const userToken = signUserToken({
      id: 'regular_user_id',
      email: 'user@example.com',
      role: 'USER',
      isPremium: false
    });

    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/audit/logs',
      headers: { authorization: `Bearer ${userToken}` }
    });

    assert.equal(res.statusCode, 403);
    const body = res.json();
    assert.equal(body.error.code, 'FORBIDDEN');
  });

  // TEST 5: Admins can access audit logs endpoint
  test('5. Admin users can access audit logs (returns 200)', async () => {
    // Record sample audit events
    await AuditService.recordEvent({
      eventType: 'auth.login.success',
      userId: 'admin_id',
      result: 'success',
      metadata: { action: 'admin_login' }
    });

    const adminToken = signUserToken({
      id: 'admin_id',
      email: 'admin@lunacare.internal',
      role: 'ADMIN',
      isPremium: true
    });

    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/audit/logs',
      headers: { authorization: `Bearer ${adminToken}` }
    });

    assert.equal(res.statusCode, 200);
    const body = res.json();
    assert.ok(Array.isArray(body.logs));
    assert.ok(body.total >= 1);
  });
});
