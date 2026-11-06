import { BlockList, isIP } from 'node:net';
import type { FastifyRequest } from 'fastify';

/**
 * Check Cloudflare proxy addresses using their complete published CIDR networks,
 * not string prefixes. Prefix checks incorrectly trusted unrelated IP addresses
 * (for example 198.41.1.2 is NOT in Cloudflare's 198.41.128.0/17).
 *
 * Source: https://www.cloudflare.com/ips-v4 and https://www.cloudflare.com/ips-v6
 * Review/update these networks whenever Cloudflare publishes changes.
 */
const CLOUDFLARE_IPV4_CIDRS: readonly string[] = [
  '173.245.48.0/20', '103.21.244.0/22', '103.22.200.0/22',
  '103.31.4.0/22', '141.101.64.0/18', '108.162.192.0/18',
  '190.93.240.0/20', '188.114.96.0/20', '197.234.240.0/22',
  '198.41.128.0/17', '162.158.0.0/15', '104.16.0.0/13',
  '104.24.0.0/14', '172.64.0.0/13', '131.0.72.0/22'
];

const CLOUDFLARE_IPV6_CIDRS: readonly string[] = [
  '2400:cb00::/32', '2606:4700::/32', '2803:f800::/32',
  '2405:b500::/32', '2405:8100::/32', '2a06:98c0::/29',
  '2c0f:f248::/32'
];

const trustedProxyBlocks = new BlockList();
for (const range of CLOUDFLARE_IPV4_CIDRS) {
  const [network, mask] = range.split('/');
  if (!network || !mask) throw new Error('Invalid bundled Cloudflare IPv4 CIDR');
  trustedProxyBlocks.addSubnet(network, Number(mask), 'ipv4');
}
for (const range of CLOUDFLARE_IPV6_CIDRS) {
  const [network, mask] = range.split('/');
  if (!network || !mask) throw new Error('Invalid bundled Cloudflare IPv6 CIDR');
  trustedProxyBlocks.addSubnet(network, Number(mask), 'ipv6');
}

function normalizeIp(ip: string): string {
  // Node may report IPv4 peers as IPv4-mapped IPv6 socket addresses.
  return ip.startsWith('::ffff:') ? ip.slice(7) : ip;
}

export function isLoopbackAddress(ip: string): boolean {
  return ip === '127.0.0.1' || ip === '::1' ||
    ip === '::ffff:127.0.0.1';
}

export function isCloudflareIp(ip: string): boolean {
  const normalized = normalizeIp(ip);
  const family = isIP(normalized);
  if (family === 4) return trustedProxyBlocks.check(normalized, 'ipv4');
  if (family === 6) return trustedProxyBlocks.check(normalized, 'ipv6');
  return false;
}

export function isTrustedProxy(ip: string): boolean {
  return isLoopbackAddress(ip) || isCloudflareIp(ip);
}

/**
 * Trust CF-Connecting-IP only when the TCP peer is a verified Cloudflare edge
 * or local cloudflared tunnel. Never trust arbitrary forwarded-IP headers.
 * Malformed/comma-separated header values fail closed to the socket address.
 */
export function getTrustedClientIp(request: FastifyRequest): string {
  const socketAddress = request.raw?.socket?.remoteAddress || request.ip || '';
  const directIp = normalizeIp(socketAddress);
  const header = request.headers['cf-connecting-ip'];

  if (typeof header === 'string' && isTrustedProxy(socketAddress)) {
    const candidate = header.trim();
    if (isIP(candidate) !== 0) {
      return candidate;
    }
  }
  return directIp;
}
