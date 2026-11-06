import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import type { FastifyInstance } from 'fastify';
import { buildApp } from '../src/app.js';
import { AuthService } from '../src/auth/index.js';
import { db } from '../src/database/index.js';
import { getTrustedClientIp, isTrustedProxy } from '../src/security/index.js';

describe('LunaCare Cache Security & Multi-Tenant Isolation Suite', () => {
  let app: FastifyInstance;

  before(async () => {
    app = await buildApp();
    await app.ready();
  });

  after(async () => {
    db.reset();
    await app.close();
  });

  // TEST 1: Strict Anti-Caching Headers on Health Check & API
  test('1. API endpoints emit strict no-store and anti-caching response headers', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/healthz'
    });

    assert.equal(res.statusCode, 200);
    const cacheControl = res.headers['cache-control'] as string;
    assert.ok(cacheControl, 'Cache-Control header must be present');
    assert.ok(cacheControl.includes('private'), 'Cache-Control must be private');
    assert.ok(cacheControl.includes('no-store'), 'Cache-Control must contain no-store');
    assert.ok(cacheControl.includes('no-cache'), 'Cache-Control must contain no-cache');
    assert.ok(cacheControl.includes('must-revalidate'), 'Cache-Control must contain must-revalidate');

    assert.equal(res.headers['pragma'], 'no-cache');
    assert.equal(res.headers['expires'], '0');
  });

  // TEST 2: Strict Anti-Caching Headers on Authenticated Health Endpoints
  test('2. Private health routes emit no-store headers preventing edge caching', async () => {
    const user = await AuthService.register({
      email: 'cache_test_user@example.com',
      password: 'Password123!',
      displayName: 'Cache User'
    });

    const res = await app.inject({
      method: 'GET',
      url: '/api/v1/periods',
      headers: { authorization: `Bearer ${user.token}` }
    });

    assert.equal(res.statusCode, 200);
    const cacheControl = res.headers['cache-control'] as string;
    assert.ok(cacheControl.includes('no-store'));
    assert.ok(cacheControl.includes('private'));
  });

  // TEST 3: Multi-Tenant Health Data Isolation (User A vs User B)
  test('3. Cross-user isolation: User B cannot observe cached or stored records of User A', async () => {
    // Register User A
    const userA = await AuthService.register({
      email: 'user_a_isolation@example.com',
      password: 'Password123!',
      displayName: 'User A'
    });

    // Register User B
    const userB = await AuthService.register({
      email: 'user_b_isolation@example.com',
      password: 'Password123!',
      displayName: 'User B'
    });

    // User A logs a private cycle
    const postRes = await app.inject({
      method: 'POST',
      url: '/api/v1/periods',
      headers: { authorization: `Bearer ${userA.token}` },
      payload: {
        startDate: '2026-11-01',
        endDate: '2026-11-05',
        flowLevel: 'Medium',
        symptoms: ['cramps', 'fatigue'],
        notesEncrypted: 'Secret personal observation for User A'
      }
    });
    assert.equal(postRes.statusCode, 201);
    const userAPeriodId = postRes.json().id;

    // User B fetches period logs
    const listResB = await app.inject({
      method: 'GET',
      url: '/api/v1/periods',
      headers: { authorization: `Bearer ${userB.token}` }
    });
    assert.equal(listResB.statusCode, 200);
    const userBBody = listResB.json();
    const userBLogs = userBBody.logs;
    assert.equal(Array.isArray(userBLogs), true);
    // User B must have ZERO of User A's logs
    assert.equal(userBLogs.some((l: { id: string }) => l.id === userAPeriodId), false);

    // User B attempts to delete User A's log
    const deleteRes = await app.inject({
      method: 'DELETE',
      url: `/api/v1/periods/${userAPeriodId}`,
      headers: { authorization: `Bearer ${userB.token}` }
    });
    assert.equal(deleteRes.statusCode, 403);
  });

  // TEST 4: Trusted Proxy & Client IP Spoofing Prevention
  test('4. getTrustedClientIp rejects spoofed CF-Connecting-IP from untrusted direct sockets', () => {
    // Simulation 1: Direct untrusted client attempting header spoofing
    const fakeUntrustedRequest = {
      ip: '203.0.113.50',
      headers: {
        'cf-connecting-ip': '198.51.100.99' // Attacker attempting to spoof IP
      },
      raw: {
        socket: {
          remoteAddress: '203.0.113.50' // Physical untrusted connection
        }
      }
    };

    // @ts-expect-error Mock request object
    const resolvedIp1 = getTrustedClientIp(fakeUntrustedRequest);
    // Must IGNORE the spoofed cf-connecting-ip and use physical socket address!
    assert.equal(resolvedIp1, '203.0.113.50');

    // Simulation 2: Request from trusted loopback (Cloudflare Tunnel on 127.0.0.1)
    const trustedTunnelRequest = {
      ip: '127.0.0.1',
      headers: {
        'cf-connecting-ip': '198.51.100.99' // Real end-user client IP from tunnel
      },
      raw: {
        socket: {
          remoteAddress: '127.0.0.1' // Physical connection from local cloudflared daemon
        }
      }
    };

    // @ts-expect-error Mock request object
    const resolvedIp2 = getTrustedClientIp(trustedTunnelRequest);
    // Must trust cf-connecting-ip because physical connection is from trusted tunnel loopback!
    assert.equal(resolvedIp2, '198.51.100.99');
  });

  // TEST 5: isTrustedProxy helper verification
  test('5. isTrustedProxy recognizes loopback and Cloudflare official IP prefixes', () => {
    assert.equal(isTrustedProxy('127.0.0.1'), true);
    assert.equal(isTrustedProxy('::1'), true);
    assert.equal(isTrustedProxy('173.245.48.10'), true);
    assert.equal(isTrustedProxy('104.16.123.45'), true);
    assert.equal(isTrustedProxy('198.41.200.5'), true);

    // Untrusted public IPs
    assert.equal(isTrustedProxy('8.8.8.8'), false);
    assert.equal(isTrustedProxy('1.1.1.1'), false);
    assert.equal(isTrustedProxy('203.0.113.1'), false);
  });
});
