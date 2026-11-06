import type { FastifyRequest } from 'fastify';

/**
 * LunaCare Trusted Proxy & Cloudflare IP Validation Utilities
 * 
 * Ensures that client IP addresses (such as CF-Connecting-IP) are ONLY trusted
 * when the TCP connection originates from a verified Cloudflare IP address or local Cloudflare Tunnel daemon.
 * Untrusted direct clients attempting to spoof CF-Connecting-IP will have their headers strictly ignored.
 */

// Cloudflare official IPv4 CIDR ranges
const CLOUDFLARE_IPV4_PREFIXES = [
  '173.245.48.',
  '103.21.244.',
  '103.22.200.',
  '103.31.4.',
  '141.101.',
  '108.162.',
  '190.93.',
  '188.114.',
  '197.234.240.',
  '198.41.',
  '162.158.',
  '104.16.',
  '104.17.',
  '104.18.',
  '104.19.',
  '104.20.',
  '104.21.',
  '104.22.',
  '104.23.',
  '104.24.',
  '104.25.',
  '104.26.',
  '104.27.',
  '172.64.',
  '172.65.',
  '172.66.',
  '172.67.',
  '172.68.',
  '172.69.',
  '172.70.',
  '172.71.',
  '131.0.72.'
];

export function isLoopbackAddress(ip: string): boolean {
  return ip === '127.0.0.1' || ip === '::1' || ip === '::ffff:127.0.0.1' || ip === 'localhost';
}

export function isCloudflareIp(ip: string): boolean {
  if (!ip) return false;
  const cleanIp = ip.replace(/^::ffff:/, '');
  return CLOUDFLARE_IPV4_PREFIXES.some(prefix => cleanIp.startsWith(prefix));
}

export function isTrustedProxy(ip: string): boolean {
  return isLoopbackAddress(ip) || isCloudflareIp(ip);
}

/**
 * Safely extracts the real client IP address.
 * Only trusts CF-Connecting-IP if the direct socket connection is from a verified trusted proxy.
 */
export function getTrustedClientIp(request: FastifyRequest): string {
  const socketAddress = request.raw?.socket?.remoteAddress || request.ip || '127.0.0.1';
  const cfConnectingIp = request.headers['cf-connecting-ip'];

  if (cfConnectingIp && typeof cfConnectingIp === 'string') {
    // Check if the physical connection originated from our Cloudflare Tunnel (loopback) or Cloudflare edge
    if (isTrustedProxy(socketAddress)) {
      return cfConnectingIp.trim();
    }
  }

  return socketAddress.replace(/^::ffff:/, '');
}
