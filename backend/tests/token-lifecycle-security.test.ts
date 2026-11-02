import { beforeEach, describe, test } from 'node:test';
import assert from 'node:assert/strict';
import type { FastifyInstance } from 'fastify';
import { buildApp } from '../src/app.js';
import { db } from '../src/database/index.js';
import { AuthService } from '../src/auth/index.js';
import { signUserToken, optionalAuth } from '../src/api/middleware/auth.js';
import { rateLimiter } from '../src/api/middleware/rateLimit.js';

describe('Signed token session and password reset security', () => {
  let app: FastifyInstance;

  beforeEach(async () => {
    db.reset();
    rateLimiter.reset();
    app = await buildApp();
  });

  test('a deleted account cannot continue using a previously signed JWT', async () => {
    const identity = await AuthService.register({
      email: 'deleted-session@example.com',
      password: 'Password123!',
      displayName: 'Test User'
    });

    const before = await app.inject({
      method: 'GET',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${identity.token}` }
    });
    assert.equal(before.statusCode, 200);

    db.deleteUser(identity.user.id);

    const after = await app.inject({
      method: 'GET',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${identity.token}` }
    });
    assert.equal(after.statusCode, 401);
    assert.equal(after.json().error.code, 'INVALID_TOKEN');
  });

  test('role and premium status are resolved from current server records, not stale claims', async () => {
    const identity = await AuthService.register({
      email: 'role-change@example.com',
      password: 'Password123!',
      displayName: 'Test User'
    });
    const user = db.getUserById(identity.user.id)!;
    db.updateUser(user.id, { role: 'ADMIN', isPremium: true });
    const adminToken = signUserToken({ ...user, role: 'ADMIN', isPremium: true });

    const admin = await app.inject({
      method: 'GET',
      url: '/api/v1/audit/logs',
      headers: { authorization: `Bearer ${adminToken}` }
    });
    assert.equal(admin.statusCode, 200);

    db.updateUser(user.id, { role: 'USER', isPremium: false });

    const denied = await app.inject({
      method: 'GET',
      url: '/api/v1/audit/logs',
      headers: { authorization: `Bearer ${adminToken}` }
    });
    assert.equal(denied.statusCode, 403);

    const me = await app.inject({
      method: 'GET',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${adminToken}` }
    });
    assert.equal(me.statusCode, 200);
    assert.equal(me.json().role, 'USER');
    assert.equal(me.json().isPremium, false);
  });

  test('optional authentication does not attach stale or deleted account claims', async () => {
    const identity = await AuthService.register({
      email: 'optional-session@example.com',
      password: 'Password123!',
      displayName: 'Test User'
    });
    app.get('/test/optional-identity', { preHandler: optionalAuth }, async (request) => ({
      id: request.user?.id ?? null,
      role: request.user?.role ?? null
    }));

    db.updateUser(identity.user.id, { role: 'MODERATOR' });
    const updated = await app.inject({
      method: 'GET',
      url: '/test/optional-identity',
      headers: { authorization: `Bearer ${identity.token}` }
    });
    assert.equal(updated.json().role, 'MODERATOR');

    db.deleteUser(identity.user.id);
    const deleted = await app.inject({
      method: 'GET',
      url: '/test/optional-identity',
      headers: { authorization: `Bearer ${identity.token}` }
    });
    assert.deepEqual(deleted.json(), { id: null, role: null });
  });

  test('password-reset requests are rate-limited without account enumeration', async () => {
    const identity = await AuthService.register({
      email: 'reset-existing@example.com',
      password: 'Password123!',
      displayName: 'Test User'
    });
    assert.ok(identity.token);

    const emailAddresses = [
      'reset-existing@example.com',
      'reset-unknown@example.com',
      'reset-unknown2@example.com'
    ];
    const messages: string[] = [];
    for (const email of emailAddresses) {
      const response = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/password-reset',
        payload: { email }
      });
      assert.equal(response.statusCode, 200);
      messages.push(response.json().message);
    }
    assert.equal(new Set(messages).size, 1);

    const blocked = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/password-reset',
      payload: { email: 'another@example.com' }
    });
    assert.equal(blocked.statusCode, 429);
    assert.equal(blocked.json().error.code, 'RATE_LIMIT_EXCEEDED');
  });
});
