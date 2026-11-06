import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import type { FastifyRequest } from 'fastify';
import { isCloudflareIp, isTrustedProxy, getTrustedClientIp } from '../src/security/proxy.js';

function fakeRequest(peer: string, header?: string): FastifyRequest {
  return {
    ip: peer,
    raw: { socket: { remoteAddress: peer } },
    headers: header === undefined ? {} : { 'cf-connecting-ip': header }
  } as unknown as FastifyRequest;
}

describe('Cloudflare proxy address validation', () => {
  test('accepts actual published IPv4 CIDR addresses, including ranges previously missed', () => {
    assert.equal(isCloudflareIp('198.41.200.5'), true);
    assert.equal(isCloudflareIp('173.245.49.5'), true);
    assert.equal(isCloudflareIp('103.21.245.1'), true);
    assert.equal(isCloudflareIp('::ffff:198.41.200.5'), true);
  });

  test('rejects neighboring networks previously accepted by string prefix matching', () => {
    assert.equal(isCloudflareIp('198.41.1.2'), false);
    assert.equal(isCloudflareIp('131.0.76.1'), false);
    assert.equal(isCloudflareIp('104.15.255.255'), false);
    assert.equal(isCloudflareIp('104.28.0.1'), false);
    assert.equal(isCloudflareIp('198.51.100.1'), false);
    assert.equal(isCloudflareIp('not-an-ip'), false);
  });

  test('supports Cloudflare published IPv6 networks without trusting other IPv6 addresses', () => {
    assert.equal(isCloudflareIp('2606:4700::1'), true);
    assert.equal(isCloudflareIp('2a06:98c0::1'), true);
    assert.equal(isCloudflareIp('2001:db8::1'), false);
  });

  test('trusts only physical Cloudflare or local tunnel peers for forwarding', () => {
    assert.equal(isTrustedProxy('127.0.0.1'), true);
    assert.equal(isTrustedProxy('::1'), true);
    assert.equal(isTrustedProxy('198.41.1.2'), false);
    assert.equal(getTrustedClientIp(fakeRequest('198.41.1.2', '198.51.100.99')), '198.41.1.2');
    assert.equal(getTrustedClientIp(fakeRequest('198.41.200.5', '198.51.100.99')), '198.51.100.99');
    assert.equal(getTrustedClientIp(fakeRequest('127.0.0.1', '198.51.100.99')), '198.51.100.99');
    assert.equal(getTrustedClientIp(fakeRequest('127.0.0.1', '198.51.100.99, 203.0.113.1')), '127.0.0.1');
    assert.equal(getTrustedClientIp(fakeRequest('127.0.0.1', 'bad-value')), '127.0.0.1');
  });
});
