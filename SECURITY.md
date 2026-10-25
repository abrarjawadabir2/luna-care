# Security Policy

## Supported Versions

We take the security and privacy of LunaCare and its users seriously. Because LunaCare manages sensitive reproductive and menstrual health data, we prioritize rapid vulnerability remediation and proactive defense-in-depth.

The following versions are currently supported with security updates:

| Component | Version Line | Security Status |
| :--- | :--- | :--- |
| LunaCare Core (Full Stack) | `1.6.x` | :white_check_mark: Active Support (Current Release) |
| LunaCare Core | `1.5.x` | :warning: Critical Security Patches Only |
| LunaCare Core | `< 1.5` | :x: End of Life / Unsupported |
| Android App (`app`) | `>= 1.5.0` (SDK 36) | :white_check_mark: Active Support |
| Backend API (`backend`) | Node.js 20+ LTS | :white_check_mark: Active Support |
| File Inspector Worker (`backend-rust`) | Rust 1.75+ (Safe code enforced) | :white_check_mark: Active Support |

---

## Reporting a Vulnerability

If you discover a security vulnerability, privacy leak, or authentication flaw in LunaCare, please disclose it responsibly.

### 1. Private Disclosure Channels
- **GitHub Security Advisories (Preferred):** Open a private advisory report at [GitHub Security Advisories](https://github.com/abrarjawadabir2/luna-care/security/advisories/new).
- **Direct Maintainer Contact:** Reach out directly to the maintainer via email at `security@lunacare.app` or via GitHub private communication.
- **Do Not File Public Issues:** Please do not open public GitHub issues, discussions, or pull requests disclosing unpatched security vulnerabilities.

### 2. Information to Include in Your Report
To help us triage and reproduce the issue quickly, please provide:
- **Summary & Impact:** Clear description of the vulnerability, potential real-world impact, and affected assets (e.g., Android app, Fastify backend, Rust worker).
- **Reproduction Steps:** Step-by-step instructions or minimal proof-of-concept (curl commands, script, sample payload).
- **Environment Details:** Affected version, operating system, and runtime details (e.g., Android version/device, Node.js version).
- **Mitigation Ideas:** Any suggested remediations or configuration workarounds (if identified).

---

## Response & Remediation SLA

| Phase | Target Timeline | Details |
| :--- | :--- | :--- |
| **Initial Acknowledgment** | Within 48 hours | Confirm receipt of the report and assign a lead triage engineer. |
| **Triage & Validation** | Within 5 business days | Verify exploitability, determine CVSS severity, and notify researcher. |
| **Fix Development** | 7–30 days | Develop, review, and test a private security patch across affected versions. |
| **Coordinated Disclosure** | 30–90 days | Deploy patch, publish security advisory, and credit the security researcher. |

---

## Vulnerability Scope

### In Scope
- **Health Data Privacy & Isolation:** Cross-tenant leaks (BOLA/IDOR), database Row-Level Security (RLS) bypasses, unauthorized period or journal access.
- **Authentication & Sessions:** JWT forgery, token replay, OTP brute-force, PBKDF2 PIN bypasses, account enumeration.
- **AI Gateway & Context Redaction:** Gemini AI prompt sanitization bypasses, sensitive context leakage to external LLMs, crisis detector evasion.
- **Upload & Memory Safety:** File inspection bypasses (`backend-rust`), magic byte spoofing, path traversal, decompression bombs.
- **Injection & Endpoint Security:** SQL injection, command execution, SSRF, server-side data exposure via debug/internal routes.

### Out of Scope
- Volumetric Denial of Service (DoS/DDoS) attacks against hosting or upstream cloud infrastructure.
- Social engineering, phishing, or physical attacks against LunaCare maintainers or contributors.
- Attacks requiring physical device access to an unlocked, rooted, or jailbroken device where OS-level sandboxing has been disabled.
- Weaknesses in third-party services (e.g., Supabase, Google Cloud) that have already been reported upstream and are outside LunaCare application controls.

---

## Safe Harbor & Research Conduct

We consider security research conducted under this policy to be authorized. We commit to:
- Not pursuing civil or criminal legal action against researchers who act in good faith.
- Not revoking access or suspending accounts used solely for authorized security testing.
- Publicly acknowledging and crediting researchers (in GitHub Security Advisories and release notes) upon request after the coordinated fix is released.

*Thank you for helping keep LunaCare and its community's health data safe and private.*

