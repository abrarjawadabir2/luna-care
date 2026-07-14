# Google AI Studio Patch Prompt

Apply the requirements in [`UI_SECURITY_FLOW_PATCH.md`](UI_SECURITY_FLOW_PATCH.md) to the existing LunaCare project.

Work as a careful patch, not a full rewrite. Inspect the repository first, preserve working behavior, and make the smallest safe set of changes.

## Core working rules

1. Inspect the complete repository before editing.
2. Preserve working screens, routes, authentication flows, data models, and the LunaCare design system.
3. Do not overwrite unrelated files or replace the project with a new starter template.
4. Keep the existing framework and use the smallest practical technology set.
5. Do not introduce unnecessary Kotlin, Java, Python, or TypeScript files.
6. Improve the UI using the existing LunaCare visual system.
7. Add the period-flow information feature and clear user-facing flow explanations.
8. Never add a hidden backdoor, master password, hardcoded administrator, universal OTP, secret login route, or client-controlled admin flag.
9. Never log passwords, OTPs, access tokens, refresh tokens, decrypted health notes, private AI messages, or exact GPS coordinates.
10. Run lint, type checking, tests, dependency/security checks, and the production build before finishing.

---

# Security hardening requirements

## 1. Validate every input again on the server

Do not trust browser or mobile validation as a security boundary.

Audit every authentication and account field, including:

- email
- password
- username
- display name
- phone number
- OTP
- profile text
- medical-journal text
- AI prompt text
- uploaded file metadata

Requirements:

- Keep client-side validation for good UX.
- Repeat validation on the trusted backend before processing or storing data.
- Use the schema library already appropriate for the project, such as Zod for TypeScript or the equivalent already in use.
- Prefer strict allowlists and schemas over blacklists.
- Reject malformed input instead of silently changing important values.
- Normalize email and phone identifiers consistently.
- Validate lengths, formats, ranges, enum values, date values, and allowed object keys.
- Reject unknown or excessive request fields.
- Use parameterized database queries or the Supabase client safely; never construct SQL from user input.
- Render user-generated content as text by default.
- Do not use `dangerouslySetInnerHTML` for untrusted content.
- If rich text is intentionally supported, sanitize it with a proven library and a strict allowlist.
- Protect against stored and reflected XSS.
- Add request-body and upload-size limits.

Rejected-submission logging must contain only safe metadata:

- timestamp
- endpoint or action name
- validation rule identifier
- actor/user ID if authenticated
- purpose-separated hashed IP/device identifiers when permitted
- outcome and risk level

Do not log the rejected raw password, OTP, free-text health content, session tokens, full phone number, or full email address.

## 2. Harden login, signup, OTP, and reset flows against brute force

Implement server-side controls for:

- password login
- email signup
- phone OTP requests
- phone OTP verification
- password-reset requests
- verification-email resend
- social OAuth start and callback failures
- AI assistant requests

Use layered controls:

- per-account or normalized-identifier hash
- per-IP hash derived on the server
- per-device/session hash where available
- authenticated user ID where available
- progressive delays
- temporary cooldowns
- CAPTCHA or an additional challenge after suspicious behavior
- audit events and alerts for abnormal patterns

Suggested starting policy:

- after repeated login failures, progressively increase delay
- after 5 failed account attempts within 15 minutes, apply a temporary 15-minute protection window
- after more than 20 login requests from one IP hash in one minute, throttle that IP hash
- limit OTP requests separately from OTP verification attempts
- never create a permanent account lock from failed attempts alone, because attackers could intentionally lock out legitimate users
- clear or reduce failed-attempt state after successful authentication

All enforcement must happen in a trusted backend, gateway, authentication-provider control, or Supabase Edge Function. Frontend timers and local storage are UX safeguards only and must not be treated as authoritative.

Do not reveal exact thresholds or remaining attempts to the user.

Send a security notification after an account-protection event when the contact channel is verified, but do not include sensitive details.

## 3. Password storage and verification

If the project uses Supabase Auth, Firebase Auth, Clerk, Auth0, or another trusted provider:

- use the provider's built-in password hashing, reset, session, OAuth, and MFA flows
- do not manually hash passwords in the frontend
- do not duplicate password hashes in application tables
- remove any homegrown password, JWT, or session implementation that competes with the trusted provider unless it is strictly required and security-reviewed

If a custom password backend is unavoidable:

- use Argon2id as the preferred password hash
- use a unique random salt per password
- store only the encoded password hash
- keep any optional pepper in server-side secret management
- use constant-time verification
- never use MD5, SHA-1, plain SHA-256, reversible encryption, or a regular string comparison for passwords
- hash on signup and password change
- create a gradual rehash strategy that upgrades legacy hashes only after a successful login
- do not attempt to rehash unknown plaintext passwords in a migration

Search the whole repository and logs for accidental password handling and remove it.

## 4. Prevent account and email enumeration

Review all user-facing authentication responses.

Login failures should use one generic message, for example:

> Incorrect email or password.

Do not distinguish among:

- unknown email
- wrong password
- temporarily protected account
- disabled account
- provider-internal error

Password reset should say:

> If that email is registered, you'll receive a reset link.

Signup should not directly confirm whether an email or phone number already belongs to an account. When a duplicate may exist, guide the user to log in or recover access without exposing account state.

Search the codebase for unsafe phrases such as:

- not found
- does not exist
- email is registered
- already registered
- wrong password
- account exists
- phone is registered

Replace them with non-enumerating responses.

Keep the internal security reason in protected server-side logs, not in the API response.

## 5. Prefer trusted authentication providers

Use the authentication provider already selected for LunaCare. If Supabase Auth is already configured, keep it as the source of truth.

The provider should handle:

- email/password signup and login
- password reset
- secure sessions and token rotation
- Google and Facebook OAuth where configured
- phone OTP where configured
- MFA support
- account deletion or lifecycle hooks where available

TikTok login, if retained, must use an official secure OAuth flow through a trusted backend and must remain hidden in production until credentials, callback validation, state, PKCE, and provider approval are complete.

Store only application-specific non-credential data in LunaCare tables, such as:

- authenticated provider user ID
- plan/tier
- onboarding state
- preferences
- user mode

Never store provider passwords, OTP values, OAuth authorization codes, provider client secrets, access tokens, or refresh tokens in public application tables.

## 6. Purpose-separated hashing and salting

Use trusted-provider password hashing for passwords.

For non-password security identifiers, use purpose-separated HMAC-SHA-256 on the server.

Examples of separate purposes:

- `email_rate_limit`
- `phone_rate_limit`
- `ip_rate_limit`
- `device_rate_limit`
- `user_agent_rate_limit`
- `email_lookup`
- `phone_lookup`
- `audit_reference`

Recommended construction:

```text
HMAC_SHA256(server_secret, purpose + ":" + normalized_value)
```

Requirements:

- use a secret held only in backend secret management
- do not expose the secret or pepper in frontend bundles
- use different purpose labels so a hash from one subsystem cannot be reused in another
- do not use one static public salt for every data category
- rotate secrets through a documented versioned process
- store a hash/key version where rotation requires it
- do not hash passwords with this helper

## 7. Database and authorization hardening

Enable Row Level Security on every private table.

Use deny-by-default policies. A normal user should only access rows where the verified session user matches the row owner.

Review at minimum:

- profiles
- health profiles
- period and flow logs
- mood and behavior logs
- medical journals
- reminders
- cup-care logs
- bookmarks
- notifications
- AI usage
- subscription data
- private attachments

Requirements:

- never trust a frontend-supplied `user_id`
- derive the user from the verified server session
- prevent users from editing their own role
- perform role changes only through authorized backend logic
- no broad `USING (true)` policies on private data
- no authenticated-read-all policy
- keep the Supabase service-role key out of frontend code
- inspect privileged functions, `SECURITY DEFINER` functions, and their `search_path`
- use private storage buckets for medical attachments
- use short-lived signed URLs for private files
- verify RLS with at least two real test users attempting cross-account access

## 8. Secret management and repository history

Scan the entire repository and the generated production bundle for:

- API keys
- database credentials
- service-role keys
- OAuth client secrets
- SMS-provider secrets
- private signing keys
- passwords
- tokens
- webhook secrets
- hardcoded administrator credentials

For anything discovered:

- remove it from source code
- move it to environment variables or managed secret storage
- ensure only explicitly public configuration is included in frontend bundles
- add local secret files to `.gitignore`
- inspect Git history, not only the current working tree
- rotate any secret that has already been committed or exposed
- do not print secret values in the completion report

Add automated secret scanning to CI where practical.

## 9. Dependency security

Run the dependency audit appropriate to the project.

Provide a before/after report containing:

- package name
- installed version
- severity
- affected dependency path
- whether a safe update exists
- whether the update is breaking

Automatically update only changes that are compatible and verified by tests. Flag critical or high vulnerabilities that require manual decisions.

Do not blindly run force-upgrade commands that may break the app.

Add dependency scanning to CI.

## 10. Secure file uploads

Review every upload path, including:

- profile images
- medical-journal attachments
- receipts or documents
- AI attachments

Requirements:

- validate actual content type/signature where feasible, not only the filename or extension
- allowlist approved MIME types
- cap file size and count
- generate server-controlled object names
- prevent directory traversal and user-controlled storage paths
- keep private uploads in isolated non-executable storage
- do not serve user uploads as executable HTML, JavaScript, SVG, or another active content type unless explicitly sanitized and security-reviewed
- set safe `Content-Type` and `Content-Disposition`
- use private buckets and signed URLs for sensitive content
- remove embedded metadata where appropriate for privacy
- consider malware scanning before content becomes available
- ensure one user cannot retrieve another user's upload

## 11. GPS, health data, and AI privacy

- exact GPS must remain off by default
- request location only when the user selects Find Nearby
- do not store location history
- clear temporary coordinates after the nearby search
- store only city/region/country when the user explicitly consents
- never send exact GPS to the AI assistant automatically
- never include private medical notes or journals in AI prompts without explicit user selection and consent
- do not train models on private user content
- encrypt sensitive note fields according to the existing LunaCare security design
- never include decrypted health data in logs, analytics, crash reports, or audit events

## 12. Audit logging and monitoring

Create protected security events for:

- login success and failure
- rate-limit activation
- CAPTCHA requirement
- password-reset request and completion
- OTP request and verification failure
- MFA changes
- role changes
- administrator access
- data export
- account deletion
- suspicious cross-user access
- RLS denial
- bulk export or bulk deletion
- secret or security configuration changes

Audit events must be append-only or otherwise resistant to modification by normal application users.

Do not record raw passwords, OTPs, tokens, full contact details, raw health notes, raw crisis text, or exact GPS.

Update the Python audit script only where it is already part of the project or required by the existing security design. Do not introduce Python into the runtime application simply for convenience.

## 13. CI/CD security gates

Update the pipeline to run:

1. dependency installation from the lockfile
2. lint
3. type checking or platform compilation
4. unit and integration tests
5. authentication and authorization tests
6. RLS/cross-user access tests
7. secret scanning
8. dependency vulnerability scanning
9. static security analysis where available
10. database migration validation
11. production build
12. audit-script simulation where already supported

Critical failures should block deployment.

## 14. Required tests

Add or update tests proving:

- server validation rejects malformed input even when client validation is bypassed
- user-generated text cannot create executable script content
- unknown email and wrong password return the same public response
- repeated login failures trigger server-side protection
- OTP request and verification limits are separate
- password-reset responses do not reveal account existence
- passwords and OTPs never enter application tables or logs
- purpose-separated hashes differ for the same input used for different purposes
- service-role and provider secrets are absent from frontend bundles
- one user cannot read or modify another user's private rows
- users cannot change their own role
- anonymous users cannot read private tables
- private uploads enforce type, size, ownership, and signed access
- exact GPS is not stored by default
- AI does not automatically receive private notes or location
- logout clears tokens, temporary GPS, cached user data, and temporary AI context
- reduced-motion behavior remains available
- the app still passes its UI and period-flow tests

---

# UI and period-flow update

Preserve the LunaCare visual system:

- warm cream surfaces
- muted rose primary color
- lavender secondary accents
- warm coral highlights
- Manrope body text
- Plus Jakarta Sans headings
- rounded cards
- calm shadows
- readable spacing
- clear loading, empty, error, permission-denied, and offline states

Add or refine the period-flow feature with:

- spotting
- light
- medium
- heavy
- very heavy
- flow history
- pain level
- symptoms
- product used
- product-change frequency
- optional notes
- clear non-diagnostic explanations
- visible warning for very heavy bleeding, dizziness, fainting, severe pain, fever, unusual odor/discharge, or pregnancy concern

The warning must state that LunaCare cannot diagnose the cause and that professional medical help may be needed.

Keep the interface simple, mobile-friendly, accessible, and consistent across login, signup, OTP, dashboard, cycle tracking, mood, medical journal, education, nearby care, AI assistant, permissions, settings, and crisis support.

---

# Completion checklist

Before finishing, answer these questions with evidence:

- Can malformed form data bypass server validation?
- Can login or OTP endpoints be spammed without server-side throttling?
- Would a database dump reveal plaintext passwords or private note content?
- Do login, signup, and reset messages reveal whether an account exists?
- Is authentication delegated to the trusted provider rather than custom session code?
- Does the repository or Git history contain exposed secrets?
- Are there known dependency vulnerabilities?
- Can an uploaded file become executable content or be accessed by another user?
- Can one authenticated user access another user's private records?
- Can a normal user change their own role?
- Is exact GPS stored or sent to AI without explicit consent?

Do not claim a control is complete without code, configuration, and tests that demonstrate it.

## Required completion report

Provide:

- files inspected
- files changed
- UI and flow improvements
- frontend and backend changes
- server-validation changes
- authentication-provider configuration
- brute-force and OTP protection
- password-hashing approach
- generic error-message changes
- purpose-separated hashing design
- database and RLS findings
- secret-scan results without exposing secret values
- dependency-audit before/after summary
- file-upload findings
- tests added and results
- environment variables required
- secrets that must be rotated
- manual setup still required
- remaining medical, privacy, legal, and security risks
