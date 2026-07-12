# Google AI Studio Patch Prompt

Apply the requirements in [`UI_SECURITY_FLOW_PATCH.md`](UI_SECURITY_FLOW_PATCH.md) to the existing LunaCare project.

Work as a careful patch, not a full rewrite.

## Instructions

1. Inspect the complete repository before editing.
2. Preserve all working screens, routes, authentication, and design choices.
3. Do not overwrite unrelated files.
4. Keep the current framework and use the smallest practical technology set.
5. Do not introduce unnecessary Kotlin, Java, Python, or TypeScript files.
6. Improve the UI using the existing LunaCare visual system.
7. Add the period-flow information feature and user-friendly flow explanations.
8. Enforce authentication rate limits on a trusted backend.
9. Use trusted-provider password hashing. Do not hash passwords in the client.
10. Use purpose-separated HMAC-SHA-256 for email, phone, IP, device, and audit identifiers.
11. Keep HMAC secrets and peppers in server-side secret storage only.
12. Enable and test Row Level Security for all private tables.
13. Never add a hidden backdoor, master password, hardcoded admin, or client-controlled admin flag.
14. Keep exact GPS off and unstored by default.
15. Never log passwords, OTPs, tokens, decrypted health notes, or exact GPS.
16. Add or update tests described in the patch specification.
17. Run lint, type checking, tests, and the production build before finishing.

## Required completion report

Provide:

- Files inspected
- Files changed
- UI and flow improvements
- Frontend and backend changes
- Hashing and rate-limit design
- Database/RLS findings
- Tests added
- Environment variables required
- Manual setup still required
- Remaining production risks
