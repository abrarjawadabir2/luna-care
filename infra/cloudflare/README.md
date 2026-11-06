# LunaCare Cloudflare Integration & Security Specification

> **SECURITY CLASSIFICATION: CONFIDENTIAL / INTERNAL INFRASTRUCTURE SPECIFICATION**  
> **Status: PRE-AUTHORIZATION ASSESSMENT & SPECIFICATION ONLY**  
> **Hard Rule:** Zero paid resources may be created, purchased, or launched without explicit human approval. Never modify production DNS records without authorization.

---

## 1. System Topology Overview

LunaCare adopts a hybrid Edge-to-Core security topology leveraging Cloudflare at the perimeter and an isolated AWS EC2 + private RDS PostgreSQL environment for sensitive health record processing:

```
                            INTERNET
                               |
                               v
                     CLOUDFLARE PERIMETER
                 (DNS, DDoS Shield, Edge TLS)
                               |
                   +-----------+-----------+
                   |                       |
                   v                       v
          PUBLIC STATIC SITE          SECURE API
          (Cloudflare Pages)      (api.lunacare.example)
                   |                       |
             `web/index.html`              |
          Zero-compute edge CDN            |
         Strict CSP / `_headers`           v
                                    Cloudflare WAF
                                 & Edge Rate Limiting
                                           |
                                           v
                                   Cloudflare Tunnel
                              (`cloudflared` outbound QUIC)
                                  Zero Inbound Open Ports!
                                           |
                                           v
                                     AMAZON AWS VPC
                                    (Private Subnet)
                                           |
                                   EC2: Node.js API
                                    (Fastify v5)
                                           |
                                           v
                                  RDS PostgreSQL (RLS)
                                 (Private DB Subnets)
```

---

## 2. Key Architecture Decisions

### 2.1 Public Website: Cloudflare Pages (Selected over Workers Static Assets)
- **Decision:** Cloudflare Pages is chosen to host `web/index.html`.
- **Rationale:** LunaCare's landing page is pure, responsive, accessible static HTML/CSS with zero runtime JavaScript requirements. Pages provides Git-connected deployments, branch preview environments for PR testing, $0 cost with unlimited requests, and declarative edge security headers via `web/_headers`. Workers Static Assets would add unnecessary V8 worker execution complexity and request caps.
- **Privacy Boundary:** The website is strictly isolated from the backend database; it holds zero user health records, zero credentials, and zero analytics tracking cookies.

### 2.2 Secure Ingress: Cloudflare Tunnel on AWS EC2 (Selected over ALB Ingress)
- **Decision:** Use Cloudflare Tunnel (`cloudflared` systemd daemon on EC2) to connect the Node.js API to Cloudflare edge.
- **Rationale:**
  1. **Zero Open Inbound Ports:** The EC2 Security Group permits ZERO inbound traffic (no port 80, 443, 3000, or 22). The instance only initiates outbound HTTPS/QUIC connections to Cloudflare.
  2. **Elimination of Origin IP Exposure:** The EC2 instance does not require a public IPv4 address, preventing direct-to-origin DDoS, port scans, and WAF bypass attacks.
  3. **Cost Savings:** Eliminates the need for an AWS Application Load Balancer ($18.50+/mo) and public IPv4 allocation fees ($7.30+/mo).
  4. **SSH Closure:** Shell administration is handled exclusively through AWS Systems Manager (SSM) Session Manager.

### 2.3 Strict Origin Protection & Proxy Verification
- Fastify backend does not blindly trust `X-Forwarded-For` or `CF-Connecting-IP`.
- `getTrustedClientIp` verifies that incoming TCP connections arrive from trusted loopback (`cloudflared` daemon) or official Cloudflare IP ranges before honoring `CF-Connecting-IP`.
- Requests bypassing the tunnel or presenting forged headers are attributed to the raw socket remote address.

### 2.4 Cache Security: Absolute Private Health Data Isolation
- Cloudflare must **NEVER** cache authenticated responses or private health data.
- The Fastify backend globally emits:
  ```http
  Cache-Control: private, no-store, no-cache, must-revalidate
  Pragma: no-cache
  Expires: 0
  ```
  on all `/api/v1/` routes.
- Cloudflare Cache Rules explicitly enforce `bypass_cache` for `api.lunacare.example/*` and `/api/*`. Only public static assets (`/index.html`, `/robots.txt`, `/sitemap.xml`) are cache-eligible.

### 2.5 Cloudflare Turnstile: Server-Side Bot Defense
- Evaluated for web authentication requests (`/login`, `/register`, `/password-reset`).
- `TurnstileService.verifyToken` validates challenge tokens server-to-server with `https://challenges.cloudflare.com/turnstile/v0/siteverify`.
- Turnstile secret keys are stored in environment variables, never committed to Git or embedded in client bundles.
- Native Android app requests rely on mobile app integrity and device PIN hashing rather than web-based browser challenges.

---

## 3. Directory Contents

- `infra/cloudflare/terraform/`: Modular Terraform configuration for Cloudflare DNS, WAF, Rate Limiting, Cache Rules, Tunnel, and Pages.
- `infra/cloudflare/config/`: Configuration templates for `cloudflared.yml`.
- `infra/cloudflare/tests/`: Automated static verification tests for Cloudflare infrastructure.
- `web/_headers`: Declarative edge security headers for Cloudflare Pages.
- `web/robots.txt`: Search engine crawling policy disallowing private health and API endpoints.
- `web/sitemap.xml`: Canonical public sitemap.
- `docs-private/CLOUDFLARE.md`: Strictly confidential internal architectural runbook (Git-ignored).
