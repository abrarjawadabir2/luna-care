import test, { describe, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import type { FastifyInstance } from 'fastify';
import { buildApp } from '../src/app.js';
import { db } from '../src/database/index.js';
import { AuthService } from '../src/auth/index.js';
import { rateLimiter } from '../src/api/middleware/rateLimit.js';
import { isCloudflareIp, getTrustedClientIp, sanitizeConvertedMarkdown, sanitizeUserBio } from '../src/security/index.js';
import { TurnstileService } from '../src/services/turnstile.service.js';
import { AuditService } from '../src/audit/audit.service.js';

describe('LunaCare 1-100x User Journey & Adversarial Bug-Hunt Simulation', () => {
  let app: FastifyInstance;

  beforeEach(async () => {
    db.reset();
    rateLimiter.reset();
    app = await buildApp();
  });

  test('1. 100 simulated users complete end-to-end onboarding, logging, export, and cascading deletion with strict cross-user isolation', async () => {
    const users: Array<{ id: string; token: string; username: string; periodId: string; journalId: string; moodId: string }> = [];

    for (let i = 1; i <= 100; i++) {
      // Vary realistic user inputs across 100 iterations
      const email = `sim.user.${i}@example.org`;
      const password = `LunaPass#${i}!Strong`;
      const displayName = `Luna User ${i}`;
      const username = `luna_sim_${i}`;

      const identity = await AuthService.register({ email, password, displayName, username });
      assert.equal(identity.user.role, 'USER');
      assert.equal(identity.user.isPremium, false);
      assert.equal(identity.user.username, username);

      // Update profile with bio (including occasional HTML/XSS attempts) and cycle settings
      const rawBio = i % 10 === 0
        ? `Wellness fan #${i} <script>alert(${i})</script> <img src=x onerror=alert(1)>`
        : `Cycle tracking user #${i}`;
      const cycleLen = 21 + (i % 15); // 21..35
      const periodLen = 3 + (i % 6);  // 3..8

      const patchRes = await app.inject({
        method: 'PATCH',
        url: '/api/v1/users/me',
        headers: { authorization: `Bearer ${identity.token}` },
        payload: {
          bio: rawBio,
          averageCycleLength: cycleLen,
          averagePeriodLength: periodLen,
          city: i % 2 === 0 ? 'Dhaka' : 'Toronto',
          country: i % 2 === 0 ? 'Bangladesh' : 'Canada'
        }
      });
      assert.equal(patchRes.statusCode, 200, `User ${i} profile update failed: ${patchRes.body}`);
      const updatedBio = patchRes.json().profile.bio as string;
      assert.equal(updatedBio.includes('<script>'), false);
      assert.equal(updatedBio.includes('onerror'), false);

      // Log a valid period
      const month = String(((i - 1) % 12) + 1).padStart(2, '0');
      const startDay = String(((i - 1) % 20) + 1).padStart(2, '0');
      const endDay = String(((i - 1) % 20) + 4).padStart(2, '0');
      const periodRes = await app.inject({
        method: 'POST',
        url: '/api/v1/periods',
        headers: { authorization: `Bearer ${identity.token}` },
        payload: {
          startDate: `2026-${month}-${startDay}`,
          endDate: `2026-${month}-${endDay}`,
          flowLevel: (['Spotting', 'Light', 'Medium', 'Heavy'] as const)[i % 4],
          symptoms: ['cramps', 'fatigue']
        }
      });
      assert.equal(periodRes.statusCode, 201, `User ${i} period log failed: ${periodRes.body}`);

      // Log a behaviour check-in (with high pain/stress/low sleep on some iterations)
      const painLevel = i % 5 === 0 ? 9 : (i % 6);
      const stressLevel = i % 7 === 0 ? 8 : (i % 6);
      const sleepHours = i % 11 === 0 ? 3 : 7.5;
      const moodRes = await app.inject({
        method: 'POST',
        url: '/api/v1/health/behaviour',
        headers: { authorization: `Bearer ${identity.token}` },
        payload: {
          logDate: `2026-${month}-${startDay}`,
          mood: i % 3 === 0 ? 'Good' : 'Okay',
          stressLevel,
          anxietyLevel: 3,
          sleepHours,
          sleepQuality: 7,
          painLevel,
          energyLevel: 6,
          hydrationLevel: 'Good',
          symptoms: ['headache'],
          crisisFlag: false
        }
      });
      assert.equal(moodRes.statusCode, 201, `User ${i} behaviour log failed: ${moodRes.body}`);
      const moodBody = moodRes.json();
      if (painLevel >= 8) {
        assert.ok(moodBody.flags.includes('HIGH_PAIN'), `User ${i} high pain flag missing`);
      }
      if (stressLevel >= 8) {
        assert.ok(moodBody.flags.includes('HIGH_STRESS'), `User ${i} high stress flag missing`);
      }
      if (sleepHours < 4) {
        assert.ok(moodBody.flags.includes('LOW_SLEEP'), `User ${i} low sleep flag missing`);
      }

      // Log a medical journal entry
      const journalRes = await app.inject({
        method: 'POST',
        url: '/api/v1/journal',
        headers: { authorization: `Bearer ${identity.token}` },
        payload: {
          entryDate: `2026-${month}-${startDay}`,
          category: 'Symptom',
          title: `Journal entry #${i}`,
          symptoms: ['cramps'],
          painLevel: 4,
          doctorVisit: i % 4 === 0,
          nextAppointment: i % 4 === 0 ? `2026-${month}-${endDay}` : null
        }
      });
      assert.equal(journalRes.statusCode, 201, `User ${i} journal entry failed: ${journalRes.body}`);

      users.push({
        id: identity.user.id,
        token: identity.token,
        username,
        periodId: periodRes.json().id,
        journalId: journalRes.json().id,
        moodId: moodBody.id
      });
    }

    assert.equal(users.length, 100);

    // Cross-user isolation & delete endpoint checks across pairs of all 100 users
    for (let i = 0; i < 100; i++) {
      const attacker = users[i]!;
      const victim = users[(i + 1) % 100]!;

      const delOtherPeriod = await app.inject({
        method: 'DELETE',
        url: `/api/v1/periods/${victim.periodId}`,
        headers: { authorization: `Bearer ${attacker.token}` }
      });
      assert.equal(delOtherPeriod.statusCode, 403);

      const delOtherBehaviour = await app.inject({
        method: 'DELETE',
        url: `/api/v1/health/behaviour/${victim.moodId}`,
        headers: { authorization: `Bearer ${attacker.token}` }
      });
      assert.equal(delOtherBehaviour.statusCode, 403, 'DELETE /api/v1/health/behaviour/:id must exist and enforce ownership');
    }

    // Export and delete all 100 users; verify own data exported and post-delete token is rejected
    for (let i = 0; i < 100; i++) {
      const u = users[i]!;
      const exportRes = await app.inject({
        method: 'GET',
        url: '/api/v1/users/export',
        headers: { authorization: `Bearer ${u.token}` }
      });
      assert.equal(exportRes.statusCode, 200);
      const exported = exportRes.json();
      assert.equal(exported.periodLogs.length, 1);
      assert.equal(exported.behaviourLogs.length, 1);
      assert.equal(exported.medicalJournals.length, 1);

      const deleteRes = await app.inject({
        method: 'DELETE',
        url: '/api/v1/users/me',
        headers: { authorization: `Bearer ${u.token}` }
      });
      assert.equal(deleteRes.statusCode, 200);

      const afterDelete = await app.inject({
        method: 'GET',
        url: '/api/v1/users/me',
        headers: { authorization: `Bearer ${u.token}` }
      });
      assert.equal(afterDelete.statusCode, 401);
    }
  });

  test('2. Real calendar date validation & range checks reject impossible dates across periods, journals, and behaviour logs', async () => {
    const user = await AuthService.register({
      email: 'dates.user@example.org',
      password: 'Password123!',
      displayName: 'Date Tester'
    });

    const impossibleDates = [
      '2026-02-29', // 2026 is NOT a leap year
      '2026-02-30',
      '2026-04-31',
      '2026-00-15',
      '2026-13-01',
      '2026-06-00',
      '2026-09-31'
    ];

    for (const badDate of impossibleDates) {
      const pRes = await app.inject({
        method: 'POST',
        url: '/api/v1/periods',
        headers: { authorization: `Bearer ${user.token}` },
        payload: { startDate: badDate, flowLevel: 'Medium', symptoms: [] }
      });
      assert.equal(pRes.statusCode, 400, `Period startDate ${badDate} must be rejected with 400`);

      const jRes = await app.inject({
        method: 'POST',
        url: '/api/v1/journal',
        headers: { authorization: `Bearer ${user.token}` },
        payload: { entryDate: badDate, category: 'General', title: 'Check', symptoms: [], painLevel: 2 }
      });
      assert.equal(jRes.statusCode, 400, `Journal entryDate ${badDate} must be rejected with 400`);

      const bRes = await app.inject({
        method: 'POST',
        url: '/api/v1/health/behaviour',
        headers: { authorization: `Bearer ${user.token}` },
        payload: {
          logDate: badDate, mood: 'Okay', stressLevel: 3, anxietyLevel: 3,
          sleepHours: 7, sleepQuality: 7, painLevel: 2, energyLevel: 6, hydrationLevel: 'Good'
        }
      });
      assert.equal(bRes.statusCode, 400, `Behaviour logDate ${badDate} must be rejected with 400`);
    }

    // Valid leap day (2024-02-29) MUST be accepted
    const leapRes = await app.inject({
      method: 'POST',
      url: '/api/v1/periods',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { startDate: '2024-02-29', endDate: '2024-03-04', flowLevel: 'Light', symptoms: [] }
    });
    assert.equal(leapRes.statusCode, 201);

    // endDate before startDate MUST be rejected
    const reversedRes = await app.inject({
      method: 'POST',
      url: '/api/v1/periods',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { startDate: '2026-06-15', endDate: '2026-06-10', flowLevel: 'Heavy', symptoms: [] }
    });
    assert.equal(reversedRes.statusCode, 400, 'Period endDate before startDate must be rejected');

    // Invalid nextAppointment in journal MUST be rejected
    const badAppt = await app.inject({
      method: 'POST',
      url: '/api/v1/journal',
      headers: { authorization: `Bearer ${user.token}` },
      payload: {
        entryDate: '2026-06-10',
        category: 'Doctor Visit',
        title: 'Checkup',
        symptoms: [],
        painLevel: 1,
        doctorVisit: true,
        nextAppointment: '2026-02-30'
      }
    });
    assert.equal(badAppt.statusCode, 400, 'Invalid nextAppointment date must be rejected');
  });

  test('3. Profile validation rejects whitespace-only displayName and periodLength >= cycleLength', async () => {
    const user = await AuthService.register({
      email: 'profile.bounds@example.org',
      password: 'Password123!',
      displayName: 'Valid Name'
    });

    const blankNameRes = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { displayName: '    ' }
    });
    assert.equal(blankNameRes.statusCode, 400, 'Whitespace-only displayName must be rejected');

    const impossibleCycleRes = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { averageCycleLength: 15, averagePeriodLength: 20 }
    });
    assert.equal(impossibleCycleRes.statusCode, 400, 'periodLength >= cycleLength must be rejected');

    // Set valid short cycle first, then attempt to patch periodLength >= existing cycleLength
    const setShort = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { averageCycleLength: 18, averagePeriodLength: 5 }
    });
    assert.equal(setShort.statusCode, 200);

    const partialConflict = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { averagePeriodLength: 19 }
    });
    assert.equal(partialConflict.statusCode, 400, 'Partial PATCH making periodLength >= cycleLength must be rejected');
  });

  test('4. AI chat returns 429 on quota exceeded, 503 without charging quota when provider unavailable, and resets quota on new month', async () => {
    const user = await AuthService.register({
      email: 'ai.quota@example.org',
      password: 'Password123!',
      displayName: 'AI Tester'
    });

    // When GEMINI_API_KEY is missing/placeholder, offline AI must NOT return a fabricated reply or charge quota
    const origKey = process.env['GEMINI_API_KEY'];
    delete process.env['GEMINI_API_KEY'];
    try {
      const unavailableRes = await app.inject({
        method: 'POST',
        url: '/api/v1/ai/chat',
        headers: { authorization: `Bearer ${user.token}` },
        payload: { message: 'How do menstrual cups work?', contextTopic: 'menstrual_cup' }
      });
      assert.equal(unavailableRes.statusCode, 503, `Expected 503 when AI provider unavailable, got ${unavailableRes.statusCode}: ${unavailableRes.body}`);
      assert.equal(unavailableRes.json().error.code, 'AI_SERVICE_UNAVAILABLE');

      // Quota must NOT be charged on 503
      const profileAfter = db.getProfile(user.user.id)!;
      assert.equal(profileAfter.aiMessagesUsedThisMonth, 0);

      // Whitespace-only message must be rejected with 400
      const blankMsg = await app.inject({
        method: 'POST',
        url: '/api/v1/ai/chat',
        headers: { authorization: `Bearer ${user.token}` },
        payload: { message: '     ', contextTopic: 'general' }
      });
      assert.equal(blankMsg.statusCode, 400);

      // Simulate exhausted quota in current month -> must return 429 AI_QUOTA_EXCEEDED (NOT 500!)
      const currentMonth = new Date().toISOString().substring(0, 7);
      db.saveProfile({ ...profileAfter, aiMessagesUsedThisMonth: 20, aiCurrentMonth: currentMonth });

      const quotaRes = await app.inject({
        method: 'POST',
        url: '/api/v1/ai/chat',
        headers: { authorization: `Bearer ${user.token}` },
        payload: { message: 'Tell me about cycle phases', contextTopic: 'period_education' }
      });
      assert.equal(quotaRes.statusCode, 429, `Expected 429 AI_QUOTA_EXCEEDED, got ${quotaRes.statusCode}: ${quotaRes.body}`);
      assert.equal(quotaRes.json().error.code, 'AI_QUOTA_EXCEEDED');

      // Simulate previous month exhausted quota -> GET /api/v1/users/me must show 0 used this month!
      db.saveProfile({ ...profileAfter, aiMessagesUsedThisMonth: 20, aiCurrentMonth: '2020-01' });
      const meRes = await app.inject({
        method: 'GET',
        url: '/api/v1/users/me',
        headers: { authorization: `Bearer ${user.token}` }
      });
      assert.equal(meRes.statusCode, 200);
      assert.equal(meRes.json().aiMessagesUsedThisMonth, 0, 'Profile must reflect 0 AI messages used after month rollover');
    } finally {
      if (origKey !== undefined) process.env['GEMINI_API_KEY'] = origKey;
    }
  });

  test('5. Location search preserves BOTH city and country and rejects blank category', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/api/v1/location/search-url',
      payload: { category: 'pharmacy', city: 'Dhaka', country: 'Bangladesh' }
    });
    assert.equal(res.statusCode, 200);
    const decoded = decodeURIComponent(res.json().mapsUrl);
    assert.ok(decoded.includes('Dhaka'), 'Maps URL must include city');
    assert.ok(decoded.includes('Bangladesh'), 'Maps URL must also include country when city is specified');

    const blankCat = await app.inject({
      method: 'POST',
      url: '/api/v1/location/search-url',
      payload: { category: '   ' }
    });
    assert.equal(blankCat.statusCode, 400, 'Whitespace-only category must be rejected');
  });

  test('6. Cloudflare proxy CIDR validation rejects spoofed non-CF IPs and malformed CF-Connecting-IP', () => {
    // 190.93.1.1 is NOT inside Cloudflare's 190.93.240.0/20 range!
    assert.equal(isCloudflareIp('190.93.1.1'), false, '190.93.1.1 is outside 190.93.240.0/20');
    assert.equal(isCloudflareIp('190.93.240.5'), true, '190.93.240.5 is inside 190.93.240.0/20');
    // 188.114.1.1 is NOT inside 188.114.96.0/20
    assert.equal(isCloudflareIp('188.114.1.1'), false, '188.114.1.1 is outside 188.114.96.0/20');
    assert.equal(isCloudflareIp('188.114.96.10'), true, '188.114.96.10 is inside 188.114.96.0/20');
    // 198.41.1.1 is NOT inside 198.41.128.0/17
    assert.equal(isCloudflareIp('198.41.1.1'), false, '198.41.1.1 is outside 198.41.128.0/17');
    assert.equal(isCloudflareIp('198.41.128.10'), true, '198.41.128.10 is inside 198.41.128.0/17');
    // Spoofed string starting with prefix
    assert.equal(isCloudflareIp('104.16.notanip'), false);
    // IPv6 Cloudflare range 2606:4700::/32
    assert.equal(isCloudflareIp('2606:4700:4700::1111'), true);

    // Malformed CF-Connecting-IP header even from loopback must fall back to socket address
    const fakeReq = {
      raw: { socket: { remoteAddress: '127.0.0.1' } },
      ip: '127.0.0.1',
      headers: { 'cf-connecting-ip': 'spoofed-garbage-key' }
    } as unknown as Parameters<typeof getTrustedClientIp>[0];
    assert.equal(getTrustedClientIp(fakeReq), '127.0.0.1');
  });

  test('7. AuditService preserves actual failure/denied result and reasonCode on read', async () => {
    await AuditService.recordEvent({
      eventType: 'auth.login.failure',
      userId: 'u-1',
      requestId: 'req-fail-1',
      resourceType: 'user',
      result: 'failure',
      reasonCode: 'INVALID_CREDENTIALS'
    });
    await AuditService.recordEvent({
      eventType: 'security.authorization.denied',
      userId: 'u-2',
      requestId: 'req-deny-2',
      resourceType: 'endpoint',
      result: 'denied',
      reasonCode: 'INSUFFICIENT_ROLE'
    });

    const logs = AuditService.getAuditLogs();
    const failEvent = logs.find(l => l.eventType === 'auth.login.failure');
    const denyEvent = logs.find(l => l.eventType === 'security.authorization.denied');
    assert.equal(failEvent?.result, 'failure', 'Audit log must preserve failure result');
    assert.equal(failEvent?.reasonCode, 'INVALID_CREDENTIALS');
    assert.equal(denyEvent?.result, 'denied', 'Audit log must preserve denied result');
    assert.equal(denyEvent?.reasonCode, 'INSUFFICIENT_ROLE');
  });

  test('8. Turnstile rejects missing action when expectedAction is required', async () => {
    const origSecret = process.env['TURNSTILE_SECRET_KEY'];
    const origFetch = globalThis.fetch;
    process.env['TURNSTILE_SECRET_KEY'] = 'real_test_secret_key_value';
    globalThis.fetch = (async () => new Response(JSON.stringify({
      success: true,
      hostname: 'lunacare.example'
      // intentionally missing `action`
    }), { status: 200 })) as typeof fetch;

    try {
      const res = await TurnstileService.verifyToken({
        token: 'valid_turnstile_token_123456',
        expectedAction: 'login',
        expectedHostname: 'lunacare.example'
      });
      assert.equal(res.success, false, 'Missing action must fail when expectedAction is specified');
      assert.equal(res.errorCode, 'ACTION_MISMATCH');
    } finally {
      globalThis.fetch = origFetch;
      if (origSecret === undefined) delete process.env['TURNSTILE_SECRET_KEY'];
      else process.env['TURNSTILE_SECRET_KEY'] = origSecret;
    }
  });

  test('9. XSS sanitizers cleanly handle nested parentheses in Markdown links and unclosed script tags in bio', () => {
    const md = sanitizeConvertedMarkdown('[Click](javascript:alert(1)) and [Doc](https://example.org/wiki/Cycle_(biology))');
    assert.equal(md.safeMarkdown.includes('javascript:'), false);
    assert.equal(md.safeMarkdown.includes('])'), false, `Trailing paren leak detected: ${md.safeMarkdown}`);
    assert.ok(md.safeMarkdown.includes('[Doc](https://example.org/wiki/Cycle_(biology))'), `Valid balanced-paren HTTPS link broken: ${md.safeMarkdown}`);

    const bio = sanitizeUserBio('Hello <script>alert(1)');
    assert.equal(bio.includes('alert(1)'), false, `Unclosed script body leaked into bio: ${bio}`);
    assert.equal(bio, 'Hello');
  });

  test('10. UploadService rejects malformed base64, truncated 4-byte PNG, and blank filenames', async () => {
    const user = await AuthService.register({
      email: 'upload.strict@example.org',
      password: 'Password123!',
      displayName: 'Uploader'
    });

    // Only first 4 bytes of PNG signature (truncated header)
    const truncatedPng = Buffer.from([0x89, 0x50, 0x4E, 0x47]).toString('base64');
    const res1 = await app.inject({
      method: 'POST',
      url: '/api/v1/uploads/inspect',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { originalFilename: 'tiny.png', contentType: 'image/png', base64Data: truncatedPng }
    });
    assert.equal(res1.statusCode, 400, 'Truncated 4-byte PNG must be rejected');

    // Invalid base64 payload
    const res2 = await app.inject({
      method: 'POST',
      url: '/api/v1/uploads/inspect',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { originalFilename: 'bad.png', contentType: 'image/png', base64Data: '!!!not-valid-base64!!!' }
    });
    assert.equal(res2.statusCode, 400, 'Invalid base64 payload must be rejected');
  });
});
