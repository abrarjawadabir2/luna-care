# LunaCare Patch Checklist

## Repository safety

- [ ] Existing features inspected before changes
- [ ] No unrelated files overwritten
- [ ] No hidden backdoor, master password, or hardcoded administrator
- [ ] No service-role or provider secret in client code
- [ ] No unnecessary language or framework added

## UI and flow

- [ ] LunaCare design system preserved
- [ ] Login and sign-up improved
- [ ] Phone OTP flow explains each step
- [ ] Dashboard varies by user mode
- [ ] Period flow-information screen implemented
- [ ] Back, home, loading, error, and empty states work
- [ ] Reduced-motion preference respected
- [ ] Mobile and desktop layouts verified

## Authentication and hashing

- [ ] Password hashing delegated to trusted auth provider
- [ ] Custom password storage, if unavoidable, uses Argon2id
- [ ] Email, phone, IP, device, and audit identifiers use purpose-separated HMAC
- [ ] HMAC secret is server-side only
- [ ] OTP request rate limit enforced server-side
- [ ] OTP verification rate limit enforced separately
- [ ] Invalid and expired codes use the same generic message
- [ ] OTP, passwords, tokens, and full identifiers are absent from logs

## Database and privacy

- [ ] RLS enabled on every personal-data table
- [ ] Cross-user reads and writes are denied
- [ ] Users cannot update their own role
- [ ] Admins cannot read decrypted medical notes
- [ ] Sensitive notes are encrypted where required
- [ ] GPS is off by default
- [ ] Exact coordinates are not stored by default
- [ ] AI receives no private journal or GPS data automatically

## Validation

- [ ] Lint passes
- [ ] Type checking passes
- [ ] Unit tests pass
- [ ] Production build passes
- [ ] Two-user RLS test completed
- [ ] Secret scan completed
- [ ] Manual security review completed
- [ ] Medical and privacy/legal review scheduled
