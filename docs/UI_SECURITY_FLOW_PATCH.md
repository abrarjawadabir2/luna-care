# LunaCare UI, Flow, Backend, and Security Patch

This document is the implementation brief for a careful LunaCare update. It is designed for Google AI Studio, Codex, or a development team working on the existing repository.

## Patch strategy

- Preserve working features.
- Do not rebuild the app from scratch.
- Avoid unnecessary overwrites.
- Keep the current framework and project structure unless a change is required for security or stability.
- Do not introduce unused Kotlin, Java, Python, or TypeScript files.
- Use the smallest practical set of technologies.
- Keep security controls server-side where required.
- Never add a hidden backdoor, master password, hardcoded administrator, or authentication bypass.

## Internal specialist roles

Treat the work as six coordinated responsibilities:

1. Product management: simplify flows and remove confusing steps.
2. UI/UX design: improve visual consistency, spacing, accessibility, and clarity.
3. Frontend engineering: create reusable components and responsive screens.
4. Backend engineering: secure authentication, data access, API routes, rate limits, and storage.
5. Security engineering: review hashing, salting, sessions, RBAC, RLS, OTP handling, and audit logs.
6. QA: test the main flows, security boundaries, mobile layout, and failure states.

## Visual system

Keep the LunaCare identity:

- Background: `#fff8f7`
- Primary: `#8a4d4e`
- Primary container: `#d48c8c`
- Secondary: `#5d5a84`
- Secondary container: `#d1ccfe`
- Tertiary: `#8d4d39`
- Error: `#ba1a1a`
- Text: `#201a1a`
- Muted text: `#524343`
- Body font: Manrope
- Heading font: Plus Jakarta Sans
- Rounded `2xl` and `3xl` cards
- Soft shadows and calm spacing

Every screen must include a clear title, short purpose statement, one primary action, visible back navigation where needed, and loading, error, and empty states.

## Screens to improve

- Opening page
- Login and sign-up
- Email authentication
- Phone OTP request and verification
- Social authentication
- Onboarding
- Home dashboard
- Period tracker
- Mood check-in
- Flow information
- Medical journal
- Health awareness
- Menstrual cup education
- Care shop and nearby search
- AI assistant
- Permission center
- Settings
- Security screens
- Crisis support

## User flow information

Add concise guidance so the user always knows what is happening.

Examples:

- Login: “Sign in securely to continue your LunaCare journey.”
- Phone request: “We’ll send a one-time code to verify this number.”
- OTP verification: “Enter the 6-digit code. Codes expire after a short time.”
- Location: “Exact GPS is used temporarily for nearby search and is not stored by default.”
- Medical journal: “Your notes are private and do not replace professional care.”
- Health awareness: “This information is educational, not diagnostic.”

## Dashboard update

For self-tracking users, show:

- Current cycle day
- Next period estimate
- Today’s flow status
- Prediction disclaimer
- Log Flow
- Mood Check-in
- Medical Journal
- Find Nearby
- Learn
- AI Assistant

For support-mode users, show:

- Relationship support card
- Consent and privacy reminder
- Practical support suggestions
- Health education
- Warning signs

For education-only users, show:

- Learning progress
- Recommended topic
- Menstrual health basics
- Mental wellbeing
- Product guidance

## Flow information feature

Create a dedicated period-flow experience.

### Flow levels

- Spotting: very small amount of blood or light marks.
- Light: low flow and lighter protection may be enough.
- Medium: moderate flow.
- Heavy: higher flow that may require more frequent product changes.
- Very heavy: urgent warning if sudden, persistent, or accompanied by dizziness, weakness, or fainting.

### Flow log fields

- Date
- Flow level
- Symptoms
- Pain level
- Notes
- Product used
- Product change frequency

Product options:

- Pad
- Period panty
- Menstrual cup
- Tampon
- Cloth
- Other
- Prefer not to say

Change frequency options:

- Every hour
- Every 2–3 hours
- Every 4–6 hours
- Less often
- Not sure

Show a professional-care warning for very heavy bleeding, dizziness, fainting, severe pain, fever, unusual odor, or pregnancy concern.

## Frontend architecture

Prefer reusable components:

- `AppShell`
- `TopAppBar`
- `BottomNav`
- `SidePanel`
- `PrimaryButton`
- `SecondaryButton`
- `IconButton`
- `Card`
- `InfoCard`
- `WarningCard`
- `FlowLevelCard`
- `QuickActionCard`
- `StepHeader`
- `FormField`
- `SelectCard`
- `ChipSelector`
- `LoadingState`
- `ErrorState`
- `EmptyState`
- `PermissionCard`
- `SecurityInfoCard`

Avoid duplicated button, card, navigation, and form code.

## Backend responsibilities

The backend must enforce:

- Authentication
- Phone OTP request and verification
- Login and OTP rate limits
- Profile creation
- User-mode storage
- Flow-log storage
- Medical-journal storage
- AI quotas
- Audit logging
- RBAC and RLS

Never trust a `user_id` sent by the frontend. Resolve the authenticated user from the verified session.

## Hashing and salting

### Passwords

If Supabase Auth, Firebase Auth, Clerk, or another trusted provider is used, do not manually hash passwords in the frontend. The provider must handle password hashing.

For a custom password system, use Argon2id with a unique salt per password and a server-side pepper. Plain SHA-256 is not acceptable for passwords.

### Non-password identifiers

Use HMAC-SHA-256 for privacy-preserving identifiers such as:

- normalized email
- normalized phone number
- IP address
- user agent
- device/session identifier
- audit references

Use purpose separation:

- `email_rate_limit`
- `phone_rate_limit`
- `ip_rate_limit`
- `user_agent_rate_limit`
- `device_rate_limit`
- `email_lookup`
- `phone_lookup`
- `audit_reference`

Conceptual form:

```text
HMAC_SHA256(server_secret, purpose + ":" + normalized_value)
```

The HMAC secret must remain in server-side environment variables and must never be exposed to client code.

Different purposes must produce different hashes for the same value.

## Rate limiting

Rate-limit:

- Email login
- Phone OTP request
- Phone OTP verification
- Password reset
- Social login callback failures
- AI assistant usage

Combine:

- account/email/phone hash
- IP hash
- device hash
- authenticated user ID where available

Use generic messages:

- “Email or password is incorrect.”
- “The verification code is invalid or expired.”
- “Too many attempts. Please wait before trying again.”
- “We could not complete the security check. Please try again later.”

Client-side throttling is only a usability safeguard. Server-side enforcement is mandatory.

## Database security

- Enable RLS on every personal-data table.
- Users may access only their own records.
- Users may not update their own role.
- Admin role checks must be server-verified.
- No service-role key may appear in frontend code.
- No `admin=true` browser flag may grant access.
- Admins must not read decrypted medical notes.
- No hidden backdoor or universal reset code.

## Sensitive data

Encrypt sensitive note fields before storage where the architecture safely supports it:

- Medical-journal notes
- Period and flow notes
- Mood and behaviour notes
- Menstrual cup notes
- Medicine notes
- Support notes
- Saved AI notes

Never log decrypted data, OTPs, passwords, access tokens, refresh tokens, or exact GPS coordinates.

## Location privacy

- GPS off by default
- Ask only after “Find Nearby” is selected
- No background tracking
- No location history
- Exact coordinates used temporarily only
- Store city/region/country only with consent
- Clear temporary GPS after search
- Do not send location to AI automatically

## Performance and code quality

- Avoid unnecessary libraries.
- Avoid duplicate state.
- Split very large screens.
- Lazy-load heavy features.
- Do not load all educational content at once.
- Remove dead code and unused imports.
- Keep strict types.
- Do not add Kotlin, Java, or Python unless the repository and feature genuinely require them.

## Accessibility

- Minimum 44×44 touch targets
- Proper labels
- Screen-reader support
- Visible keyboard focus
- Good contrast
- Reduced-motion support
- Errors connected to relevant inputs
- Warnings must not rely on color alone

## Tests

Add or update tests for:

### UI

- Login and sign-up render
- Phone OTP flow renders
- Dashboard changes by user mode
- Flow screen renders
- Very heavy flow triggers warning
- Loading, error, and empty states

### Security

- Passwords are never stored in app tables
- OTPs are never logged
- Phone and email hashes do not expose original values
- Purpose-separated hashes differ
- OTP request and verification limits are independent
- Invalid and expired codes show the same generic message
- Service-role key is absent from frontend bundles
- Users cannot modify roles
- Cross-user database access is denied

### Privacy

- Exact GPS is not stored by default
- Sensitive notes are encrypted before save
- AI receives no GPS or medical notes automatically
- Logout clears temporary sensitive state

## Final validation

Before merging:

1. Run lint.
2. Run type checking.
3. Run unit tests.
4. Run the production build.
5. Check mobile and desktop layouts.
6. Check reduced-motion behavior.
7. Confirm no secrets or sensitive values appear in logs.
8. Confirm server-side rate limiting is active.
9. Confirm RLS policies using at least two test users.

## Expected implementation report

The implementation response should include:

1. Files inspected
2. Files changed
3. UI improvements
4. Flow-information feature
5. Frontend architecture changes
6. Backend and security changes
7. Hashing and purpose-separation approach
8. Rate-limit controls
9. Database and RLS review result
10. Tests added
11. Remaining risks
12. Required environment variables
13. Manual setup steps
14. Medical, legal, and security review requirements
