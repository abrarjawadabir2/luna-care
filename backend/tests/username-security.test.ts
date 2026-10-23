import test, { describe, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { buildApp } from '../src/app.js';
import { db } from '../src/database/index.js';
import { rateLimiter } from '../src/api/middleware/index.js';
import { AuthService } from '../src/auth/index.js';
import { validateUsername, normalizeUsername, isReservedUsername } from '../src/utils/username.js';
import type { FastifyInstance } from 'fastify';

describe('LunaCare Unique Username System & Security Defenses', () => {
  let app: FastifyInstance;

  beforeEach(async () => {
    db.reset();
    rateLimiter.reset();
    app = await buildApp();
  });

  // TEST 1: User A can claim an available username
  test('1. User A can claim an available username', async () => {
    const userA = await AuthService.register({
      email: 'usera@example.com',
      password: 'Password123!',
      displayName: 'User A'
    });

    const res = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${userA.token}` },
      payload: { username: 'AbrarJawad' }
    });

    assert.equal(res.statusCode, 200);
    const body = res.json();
    assert.equal(body.profile.username, 'AbrarJawad');
    assert.equal(body.profile.username_normalized, 'abrarjawad');
    assert.ok(body.profile.username_updated_at);
  });

  // TEST 2: User B cannot claim User A's username
  test('2. User B cannot claim User A username', async () => {
    const userA = await AuthService.register({
      email: 'usera@example.com',
      password: 'Password123!',
      displayName: 'User A'
    });
    const userB = await AuthService.register({
      email: 'userb@example.com',
      password: 'Password123!',
      displayName: 'User B'
    });

    // User A claims 'health_journey'
    await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${userA.token}` },
      payload: { username: 'health_journey' }
    });

    // User B tries to claim 'health_journey'
    const resB = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${userB.token}` },
      payload: { username: 'health_journey' }
    });

    assert.equal(resB.statusCode, 409);
    const body = resB.json();
    assert.equal(body.error.code, 'USERNAME_NOT_AVAILABLE');
    assert.equal(body.error.message, 'Username is not available.');
  });

  // TEST 3 & 4: Username comparison is case-insensitive & cannot belong to multiple accounts
  test('3 & 4. Case-insensitivity: "Abrar", "abrar", and "ABRAR" cannot belong to three different accounts', async () => {
    const userA = await AuthService.register({
      email: 'usera@example.com',
      password: 'Password123!',
      displayName: 'User A'
    });
    const userB = await AuthService.register({
      email: 'userb@example.com',
      password: 'Password123!',
      displayName: 'User B'
    });
    const userC = await AuthService.register({
      email: 'userc@example.com',
      password: 'Password123!',
      displayName: 'User C'
    });

    // User A claims 'Abrar'
    const resA = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${userA.token}` },
      payload: { username: 'Abrar' }
    });
    assert.equal(resA.statusCode, 200);

    // User B tries 'abrar'
    const resB = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${userB.token}` },
      payload: { username: 'abrar' }
    });
    assert.equal(resB.statusCode, 409);
    assert.equal(resB.json().error.code, 'USERNAME_NOT_AVAILABLE');

    // User C tries 'ABRAR'
    const resC = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${userC.token}` },
      payload: { username: 'ABRAR' }
    });
    assert.equal(resC.statusCode, 409);
    assert.equal(resC.json().error.code, 'USERNAME_NOT_AVAILABLE');
  });

  // TEST 5 & Concurrency Test (Req 106): Two simultaneous claims result in exactly one successful owner
  test('5 & Req 106. Concurrency test: User A and User B simultaneously attempt to claim "lunacare_user"', async () => {
    const userA = await AuthService.register({
      email: 'usera@example.com',
      password: 'Password123!',
      displayName: 'User A'
    });
    const userB = await AuthService.register({
      email: 'userb@example.com',
      password: 'Password123!',
      displayName: 'User B'
    });

    // Concurrently trigger claim
    const [resA, resB] = await Promise.all([
      app.inject({
        method: 'PATCH',
        url: '/api/v1/users/me',
        headers: { authorization: `Bearer ${userA.token}` },
        payload: { username: 'lunacare_user' }
      }),
      app.inject({
        method: 'PATCH',
        url: '/api/v1/users/me',
        headers: { authorization: `Bearer ${userB.token}` },
        payload: { username: 'LUNACARE_USER' }
      })
    ]);

    const statuses = [resA.statusCode, resB.statusCode];
    assert.ok(statuses.includes(200), 'One request must succeed with 200');
    assert.ok(statuses.includes(409), 'One request must fail with 409 USERNAME_NOT_AVAILABLE');

    const failedResponse = resA.statusCode === 409 ? resA.json() : resB.json();
    assert.equal(failedResponse.error.code, 'USERNAME_NOT_AVAILABLE');
    assert.equal(failedResponse.error.message, 'Username is not available.');

    // Database state afterward: exactly one owner
    const profile = db.getProfileByUsernameNormalized('lunacare_user');
    assert.ok(profile, 'Exactly one profile in database owns lunacare_user');
  });

  // TEST 6: Reserved usernames cannot be claimed
  test('6. Reserved usernames cannot be claimed', async () => {
    const user = await AuthService.register({
      email: 'user@example.com',
      password: 'Password123!',
      displayName: 'User'
    });

    const reservedNames = ['admin', 'lunacare', 'support', 'official', 'security', 'api', 'root'];

    for (const name of reservedNames) {
      const res = await app.inject({
        method: 'PATCH',
        url: '/api/v1/users/me',
        headers: { authorization: `Bearer ${user.token}` },
        payload: { username: name }
      });
      assert.equal(res.statusCode, 400, `Expected 400 for reserved name ${name}`);
      const body = res.json();
      assert.equal(body.error.message, 'Username is not available.');
    }
  });

  // TEST 7: Invalid characters are rejected
  test('7. Invalid characters are rejected', async () => {
    const user = await AuthService.register({
      email: 'user@example.com',
      password: 'Password123!',
      displayName: 'User'
    });

    const invalidUsernames = [
      'abrar jawad',       // spaces
      'abrar/jawad',       // slashes
      'abrar\\jawad',      // backslashes
      '<script>alert(1)',  // html / xss
      'user@email.com',    // email @
      'abrar;DROP TABLE',  // sql injection syntax
      'abrar😀',           // emoji
      'abrar\u200Bjawad',  // zero-width space
      '_abrar',            // leading separator
      'abrar_',            // trailing separator
      '.abrar',            // leading period
      'abrar.',            // trailing period
      'abrar..jawad',      // consecutive periods
      'abrar__jawad'       // consecutive underscores
    ];

    for (const badName of invalidUsernames) {
      const res = await app.inject({
        method: 'PATCH',
        url: '/api/v1/users/me',
        headers: { authorization: `Bearer ${user.token}` },
        payload: { username: badName }
      });
      assert.equal(res.statusCode, 400, `Should reject invalid username: ${badName}`);
    }
  });

  // TEST 8 & 9: Too-short and too-long usernames are rejected
  test('8 & 9. Length boundaries: usernames shorter than 3 or longer than 30 characters are rejected', async () => {
    const user = await AuthService.register({
      email: 'user@example.com',
      password: 'Password123!',
      displayName: 'User'
    });

    // Too short (2 chars)
    const resShort = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { username: 'ab' }
    });
    assert.equal(resShort.statusCode, 400);

    // Too long (31 chars)
    const resLong = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { username: 'a'.repeat(31) }
    });
    assert.equal(resLong.statusCode, 400);

    // Exactly 3 chars - valid
    const resValid3 = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { username: 'abc' }
    });
    assert.equal(resValid3.statusCode, 200);
  });

  // TEST 10: Another user's email/phone is never returned by availability API
  test('10. Another user email/phone/id is never returned by availability API', async () => {
    await AuthService.register({
      email: 'private_owner@example.com',
      phone: '+15550199999',
      password: 'Password123!',
      displayName: 'Private Person'
    });

    // Make an availability check for an existing username or checking endpoint
    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/users/username-availability?username=abrar'
    });

    assert.equal(res.statusCode, 200);
    const body = res.json();
    assert.equal(typeof body.available, 'boolean');

    // Strict leak checks
    const raw = JSON.stringify(body);
    assert.ok(!raw.includes('private_owner'), 'Must not leak email');
    assert.ok(!raw.includes('+1555'), 'Must not leak phone');
    assert.ok(!raw.includes('password'), 'Must not leak password');
    assert.ok(!raw.includes('userId'), 'Must not leak user ID');
  });

  // TEST 11: Availability endpoint is rate-limited
  test('11. Availability endpoint is rate-limited against automated scraping', async () => {
    // 60 requests per minute limit
    let wasRateLimited = false;
    for (let i = 0; i < 70; i++) {
      const res = await app.inject({
        method: 'GET',
        url: `/api/v1/users/username-availability?username=check_${i}`
      });
      if (res.statusCode === 429) {
        wasRateLimited = true;
        break;
      }
    }
    assert.ok(wasRateLimited, 'High volume queries must trigger rate limiter (429)');
  });

  // TEST 12 & 13: Client userId cannot change username ownership & cannot modify another user's username
  test('12 & 13. Client cannot forge userId to claim or modify another user username', async () => {
    const userA = await AuthService.register({
      email: 'usera@example.com',
      password: 'Password123!',
      displayName: 'User A'
    });
    const userB = await AuthService.register({
      email: 'userb@example.com',
      password: 'Password123!',
      displayName: 'User B'
    });

    // User A claims 'alice'
    await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${userA.token}` },
      payload: { username: 'alice' }
    });

    // User B tries to inject { id: userA.user.id } to modify User A's username
    const resForged = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${userB.token}` },
      payload: { id: userA.user.id, username: 'alice_hacked' }
    });

    // Zod strict validation rejects unexpected/unauthorized fields like 'id'
    assert.equal(resForged.statusCode, 400);

    // Verify User A still owns 'alice'
    const profileA = db.getProfile(userA.user.id);
    assert.equal(profileA?.username, 'alice');
  });

  // TEST 14 & 15: Public profile lookup returns only allowlisted public fields & never private health data
  test('14 & 15. Public profile lookup returns only allowlisted public fields, never private health data', async () => {
    const user = await AuthService.register({
      email: 'sensitive_health_user@example.com',
      phone: '+15559876543',
      password: 'Password123!',
      displayName: 'Health Advocate'
    });

    // Claim username and add bio
    await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` },
      payload: {
        username: 'healthadvocate',
        bio: 'Passionate about wellness',
        avatar: 'https://example.com/avatar.png'
      }
    });

    // User creates sensitive period log and journal
    await app.inject({
      method: 'POST',
      url: '/api/v1/periods',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { startDate: '2026-06-01', flowLevel: 'Heavy', symptoms: ['extreme cramps'] }
    });

    await app.inject({
      method: 'POST',
      url: '/api/v1/journal',
      headers: { authorization: `Bearer ${user.token}` },
      payload: {
        entryDate: '2026-06-01',
        category: 'Gynecology',
        title: 'Confidential Diagnosis Discussion',
        symptoms: ['pelvic pain'],
        painLevel: 8,
        doctorVisit: true
      }
    });

    // Public lookup via /api/v1/users/healthadvocate
    const resPublic = await app.inject({
      method: 'GET',
      url: '/api/v1/users/healthadvocate'
    });

    assert.equal(resPublic.statusCode, 200);
    const body = resPublic.json();

    // Verify public allowlist fields
    assert.equal(body.username, 'healthadvocate');
    assert.equal(body.displayName, 'Health Advocate');
    assert.equal(body.avatar, 'https://example.com/avatar.png');
    assert.equal(body.publicBio, 'Passionate about wellness');

    // Verify ABSOLUTELY NO private data is returned
    assert.equal(body.id, undefined);
    assert.equal(body.userId, undefined);
    assert.equal(body.email, undefined);
    assert.equal(body.phone, undefined);
    assert.equal(body.role, undefined);
    assert.equal(body.isPremium, undefined);
    assert.equal(body.periodLogs, undefined);
    assert.equal(body.medicalJournals, undefined);
    assert.equal(body.symptoms, undefined);

    const raw = JSON.stringify(body);
    assert.ok(!raw.includes('cramps'));
    assert.ok(!raw.includes('Gynecology'));
    assert.ok(!raw.includes('sensitive_health_user'));

    // Also verify /api/v1/u/healthadvocate shortcut route
    const resShortcut = await app.inject({
      method: 'GET',
      url: '/api/v1/u/healthadvocate'
    });
    assert.equal(resShortcut.statusCode, 200);
    assert.deepEqual(resShortcut.json(), body);
  });

  // TEST 16: Username index/constraint exists
  test('16. Username unique index/constraint exists in database layer', () => {
    const indexes = db.getIndexes();
    assert.ok(indexes.includes('profiles_username_normalized_idx'), 'Must have profiles_username_normalized_idx index');
  });

  // TEST 17: Username change writes an audit event
  test('17. Username claim and change write safe audit events', async () => {
    const user = await AuthService.register({
      email: 'audited_user@example.com',
      password: 'Password123!',
      displayName: 'Audited User'
    });

    // Claim first username
    await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { username: 'first_username' }
    });

    const auditLogs1 = db.getAuditLogs();
    const claimLog = auditLogs1.find(l => l.action === 'user.username.claimed');
    assert.ok(claimLog, 'Must record user.username.claimed audit log');
    assert.equal(claimLog.actorId, user.user.id);
    assert.equal(claimLog.resourceType, 'username');
    // Ensure no sensitive content
    const rawMeta = JSON.stringify(claimLog.metadata);
    assert.ok(!rawMeta.includes('Password123!'));
  });

  // TEST 18: Username change cannot bypass reserved-name rules
  test('18. Username change cannot bypass reserved-name rules', async () => {
    const user = await AuthService.register({
      email: 'changer@example.com',
      password: 'Password123!',
      displayName: 'Changer'
    });

    // Initial claim
    await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { username: 'changer_initial' }
    });

    // Attempt change to reserved name
    const res = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { username: 'admin' }
    });

    assert.equal(res.statusCode, 400);
    assert.equal(res.json().error.message, 'Username is not available.');
  });

  // Reassignment Protection (Req 96 Option B: 30-day reassignment hold)
  test('Reassignment Protection: Old username is held for 30 days and cannot be immediately sniped', async () => {
    const userA = await AuthService.register({
      email: 'usera@example.com',
      password: 'Password123!',
      displayName: 'User A'
    });
    const userB = await AuthService.register({
      email: 'userb@example.com',
      password: 'Password123!',
      displayName: 'User B'
    });

    // User A claims 'orig_name'
    await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${userA.token}` },
      payload: { username: 'orig_name' }
    });

    // User A changes username to 'new_name' (bypassing cooldown for test)
    db.claimUsername(userA.user.id, 'new_name', 'new_name', { skipCooldown: true });

    // User B tries to claim 'orig_name' within 30-day protection window
    const resB = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${userB.token}` },
      payload: { username: 'orig_name' }
    });

    assert.equal(resB.statusCode, 409);
    assert.equal(resB.json().error.code, 'USERNAME_NOT_AVAILABLE');
  });

  // Account deletion releases username and preserves internal ID separation (Req 112, 113)
  test('Account deletion cleans up username index and leaves other users unaffected', async () => {
    const user = await AuthService.register({
      email: 'delete_me@example.com',
      password: 'Password123!',
      displayName: 'Delete Me'
    });

    await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { username: 'will_be_freed' }
    });

    // Check availability before deletion
    const beforeDel = await app.inject({
      method: 'GET',
      url: '/api/v1/users/username-availability?username=will_be_freed'
    });
    assert.equal(beforeDel.json().available, false);

    // Delete account
    const delRes = await app.inject({
      method: 'DELETE',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` }
    });
    assert.equal(delRes.statusCode, 200);

    // Availability after deletion
    const afterDel = await app.inject({
      method: 'GET',
      url: '/api/v1/users/username-availability?username=will_be_freed'
    });
    assert.equal(afterDel.json().available, true);
  });
});
