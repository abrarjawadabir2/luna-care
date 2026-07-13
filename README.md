<div align="center">

# 🌙 LunaCare

### A privacy-focused women's health and wellbeing companion

LunaCare is an Android application built to support menstrual health tracking, emotional wellbeing, private journaling, and responsible AI-assisted guidance — all without compromising the privacy of the people who use it.

<br>

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#technology)
[![Build](https://img.shields.io/badge/Build-Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)](#technology)
[![AI](https://img.shields.io/badge/AI-Google%20Gemini-4285F4?style=for-the-badge&logo=google&logoColor=white)](#luna-ai-assistant)
[![Security](https://img.shields.io/badge/Focus-Security%20%26%20Privacy-8A4D4E?style=for-the-badge)](#security-and-privacy)
[![Status](https://img.shields.io/badge/Status-Active%20Development-5D5A84?style=for-the-badge)](#development-status)

<br>

**[Open in Google AI Studio](https://ai.studio/apps/ae458a1a-d802-40d6-bc03-0bca9bce51af)** &nbsp;•&nbsp; **[View Repository](https://github.com/abrarjawadabir2/luna-care-security-review)**

</div>

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Luna AI Assistant](#luna-ai-assistant)
- [Nearby Care](#nearby-care)
- [Authentication](#authentication)
- [Security and Privacy](#security-and-privacy)
- [File-Upload Security](#file-upload-security)
- [User Interface](#user-interface)
- [Technology](#technology)
- [Local Development](#local-development)
- [Testing](#testing)
- [Contributing](#contributing)
- [Medical Disclaimer](#medical-disclaimer)
- [Privacy Principles](#privacy-principles)
- [Development Status](#development-status)

---

## Overview

LunaCare gives people a calm, private space to record menstrual health information, understand their own cycle patterns, reflect on emotional wellbeing, keep personal health notes, and access clear educational content — all on their terms.

The project is built around four core principles:

- **Privacy-first** health-data handling
- **Secure and responsible** authentication
- **Accessible and supportive** user experiences
- **Educational guidance** without medical diagnosis

> **Medical notice:** LunaCare provides educational and wellbeing support. It does not diagnose medical conditions, prescribe treatment, or replace qualified healthcare professionals.

---

## Key Features

### 🩸 Menstrual Health Tracking

LunaCare supports structured menstrual-cycle and flow logging, including:

- Period start and end dates
- Cycle-history records
- Menstrual-flow tracking
- Pain-level recording
- Symptom tracking
- Product-use information
- Personal period notes
- Estimated cycle insights

**Supported flow levels:**

| Flow level | Description |
|---|---|
| Spotting | Small marks or light stains |
| Light | Low menstrual flow |
| Medium | Moderate menstrual flow |
| Heavy | Higher flow requiring more frequent product changes |
| Very heavy | Unusually high or sudden bleeding that may require medical attention |

LunaCare does not diagnose the cause of menstrual symptoms. When a user records very heavy bleeding, fainting, dizziness, severe pain, fever, unusual discharge, or pregnancy-related concerns, the app displays a clear recommendation to seek professional medical advice.

---

### 💙 Mood and Wellbeing

Users can record and reflect on their emotional wellbeing through:

- Daily mood check-ins
- Emotional-state tracking
- Stress and energy reflections
- Behaviour notes
- Self-care suggestions
- Supportive wellbeing content
- Crisis-support access

Crisis-support information remains accessible to every user, free of charge — never gated behind a subscription or AI credits.

---

### 📔 Private Medical Journal

The medical journal lets users record private health information, such as:

- Symptoms
- Health questions
- Medication notes
- Appointment preparation
- Personal observations
- Menstrual-health concerns
- Follow-up reminders

Sensitive journal content is protected from unauthorized access and excluded from analytics, application logs, and AI prompts — unless the user explicitly chooses to share it.

---

### 📘 Health Education

LunaCare includes educational content covering topics such as:

- Menstrual health and hygiene
- PCOS and PCOD awareness
- Menstrual-cup use and care
- Period-product guidance
- Emotional wellbeing and self-care
- Warning signs that may require medical attention

All educational content is written in clear, respectful, non-diagnostic language.

---

## Luna AI Assistant

Luna is an AI-assisted educational companion that helps users understand general health and wellbeing topics.

**Example use cases:**

- Understanding menstrual-cycle concepts
- Learning about menstrual-flow levels
- Preparing questions for a healthcare professional
- Exploring general self-care suggestions
- Learning about PCOS and PCOD
- Understanding menstrual-cup care
- Finding relevant educational content

### AI Privacy Rules

The AI assistant must **never** automatically receive:

- Private medical-journal entries
- Exact GPS coordinates
- Passwords or verification codes
- Authentication tokens
- Personal contact details
- Full health-profile records
- Crisis-related text
- Private period notes

Sensitive information is only included when the user explicitly selects it and gives clear consent.

> Luna provides educational support and cannot diagnose medical conditions.

---

## Nearby Care

LunaCare helps users discover relevant nearby services while keeping location data private.

**Location principles:**

- Location access is disabled by default
- Permission is requested only when the user selects **Find Nearby**
- Manual city-based search remains available
- Exact GPS coordinates are used temporarily, never stored
- Background location tracking is not permitted
- Location history is not stored
- Temporary coordinates are cleared after use
- Exact GPS is never automatically sent to the AI assistant

---

## Authentication

LunaCare supports the following authentication methods when configured:

- Google
- Facebook
- Email and password
- Phone number with one-time password (OTP) verification

### Authentication Security

The authentication system includes:

- Trusted authentication-provider integration
- Secure password hashing managed by the provider
- Generic login error messages
- Email and phone-number verification
- Server-side rate limiting
- Separate limits for OTP requests and OTP verification
- Progressive delays after repeated failures
- Temporary cooldown protection
- CAPTCHA or additional verification after suspicious activity
- Secure password-reset flows
- Session revocation where supported
- Optional multi-factor authentication

**Safe login error:**
```text
Incorrect email or password.
```

**Safe password-reset response:**
```text
If that email is registered, you will receive a reset link.
```

**Safe OTP error:**
```text
The verification code is invalid or expired. Request a new code and try again.
```

None of these responses reveal whether an account, email address, or phone number actually exists.

---

## Security and Privacy

LunaCare follows a security-first development approach.

### Server-Side Validation

All sensitive inputs are validated on the trusted backend, even when already checked in the interface, including:

- Email addresses, passwords, phone numbers, OTP codes
- Usernames and display names
- Period records, health notes, medical-journal fields
- AI prompts
- Uploaded-file metadata

Validation uses strict schemas, approved value lists, length restrictions, date validation, number-range validation, and rejection of unknown fields. Frontend validation improves usability, but is never treated as the primary security boundary.

### Password Protection

Passwords are never stored in plaintext. Where Supabase Auth or another trusted authentication provider is used, password hashing and verification remains fully managed by that provider.

LunaCare must not store passwords in application database tables, profile records, device logs, analytics, crash reports, local storage, AI prompts, or audit logs — and must never use MD5, SHA-1, reversible encryption, or plain SHA-256 for password storage.

### Purpose-Separated Hashing

Non-password security identifiers may use server-side HMAC-SHA-256 hashing, with separate context labels per purpose:

```text
email_rate_limit
phone_rate_limit
ip_rate_limit
device_rate_limit
user_agent_rate_limit
email_lookup
phone_lookup
audit_reference
```

Recommended construction:

```text
HMAC_SHA256(server_secret, purpose + ":" + normalized_value)
```

The HMAC secret stays in secure backend storage and is never included in the Android application or frontend bundle.

### Rate Limiting

Server-side rate limiting protects password login, email signup, phone OTP requests and verification, password-reset requests, verification-email resends, OAuth failures, identity linking, and AI assistant usage — using account-identifier hashes, server-derived IP hashes, device/session hashes, and authenticated user IDs. Frontend timers improve the interface but never replace backend enforcement.

### Database Protection

Private LunaCare records use deny-by-default access rules, covering user and health profiles, period/flow/mood/behaviour records, medical-journal entries, medication reminders, menstrual-cup records, support notes, notifications, bookmarks, AI usage records, subscription records, and private attachments.

**Security requirements:**

- Users may access only their own private records
- The authenticated user ID always comes from the verified session
- User IDs supplied by the client are never trusted
- Users cannot change their own role
- Administrator privileges are verified on the backend
- Private files use protected storage with short-lived signed URLs
- Service-role keys never appear in client code

### No-Backdoor Policy

LunaCare must never include hidden administrator accounts, master passwords, universal OTP codes, secret login routes, hardcoded credentials, client-controlled admin flags, production debug-login buttons, authentication bypass parameters, or service-role keys in application code.

Emergency administrative access, when required, follows a formal, time-limited, MFA-protected, and fully audited process.

### Sensitive Logging Policy

LunaCare never logs passwords, OTP codes, access/refresh tokens, OAuth authorization codes, provider secrets, full phone numbers or emails, private medical notes, raw crisis-support text, exact GPS coordinates, or payment information. Security logs contain only safe operational metadata.

---

## File-Upload Security

Any file-upload feature includes:

- Approved MIME-type allowlists and file-signature validation
- File-size and file-count limits
- Server-generated object names and path-traversal protection
- Isolated, non-executable, private storage
- Signed URLs for sensitive files
- Safe `Content-Type` and `Content-Disposition` headers
- User-ownership verification and metadata removal where appropriate
- Malware scanning where available

The application never trusts a filename or extension alone.

---

## User Interface

LunaCare uses a calm, professional, healthcare-inspired design.

### Design System

| Design element | Value |
|---|---|
| Background | `#FFF9F8` |
| Surface | `#FFF9F8` |
| Surface container | `#F9E9E8` |
| Primary | `#8A4D4E` |
| Primary container | `#F3D5D4` |
| Secondary | `#5D5A84` |
| Secondary container | `#E5E1FF` |
| Tertiary | `#8D4D39` |
| Main text | `#2C2021` |
| Secondary text | `#665354` |
| Outline | `#8A7374` |
| Error | `#BA1A1A` |
| Error container | `#FFDAD6` |

### Interface Principles

- Calm visual hierarchy and professional typography
- Rounded cards and controls with accessible color contrast
- Minimum 44×44-pixel touch targets
- Clear loading indicators and friendly empty states
- Safe error messages
- Reduced-motion support
- Consistent navigation and responsive mobile layouts
- No unnecessary animations
- No fake cycle predictions before the user provides data

---

## Empty and Initial States

**Dashboard**
```text
Start your cycle journey

Add your latest period date to begin receiving cycle insights.
```

**Flow Tracker**
```text
How is your flow today?

Choose the option that feels closest to your experience.
```

**Medical Journal**
```text
Your journal is empty. Add a private note whenever you need to record a symptom, question, or health update.
```

**Mood History**
```text
No mood check-ins yet. Take a moment to record how you feel today.
```

**Notifications**
```text
You are all caught up. New reminders and important updates will appear here.
```

**AI Assistant**
```text
How can Luna help today?
```

Suggested prompts: *Understand my cycle · Learn about menstrual flow · Prepare questions for a doctor · Explore self-care suggestions · Learn about PCOS and PCOD · Understand menstrual-cup care*

---

## Technology

- Android Studio, Android SDK, Gradle
- Kotlin and existing Android project components
- Google AI Studio and the Google Gemini API
- Secure environment configuration
- Git and GitHub

The project continues to build on its current stack, avoiding unnecessary frameworks, duplicated services, or unrelated runtime languages.

---

## Project Structure

```text
luna-care-security-review/
├── app/
│   ├── src/
│   │   ├── main/
│   │   ├── test/
│   │   └── androidTest/
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── .env.example
├── .gitignore
└── README.md
```

*The exact structure may change as development continues.*

---

## Local Development

### Prerequisites

- Android Studio
- Android SDK
- A compatible Java Development Kit
- Git
- A Gemini API key
- An Android emulator or physical Android device

### Clone the Repository

```bash
git clone https://github.com/abrarjawadabir2/luna-care-security-review.git
cd luna-care-security-review
```

### Open the Project

1. Open Android Studio and select **Open**.
2. Choose the cloned repository directory.
3. Wait for Gradle synchronization to finish.
4. Install any requested Android SDK components.
5. Allow Android Studio to resolve compatible project dependencies.

### Configure Environment Variables

Create a `.env` file in the project root:

```env
GEMINI_API_KEY=your_gemini_api_key
```

Use `.env.example` as a reference when available, and **never commit the real `.env` file.**

Recommended `.gitignore` entries:

```gitignore
.env
.env.*
!.env.example
local.properties
*.jks
*.keystore
.idea/
.DS_Store
build/
app/build/
```

Only public configuration should ever be included in application code.

### Debug Signing Configuration

If Android Studio reports that `debugConfig` does not exist, review `app/build.gradle.kts` — the following line may reference a signing configuration unavailable locally:

```kotlin
signingConfig = signingConfigs.getByName("debugConfig")
```

For local development, remove or correct this line **only** when the referenced configuration is missing. Never remove or expose a valid production signing configuration.

### Run the Application

1. Start an Android emulator or connect a physical device (enable USB debugging if physical).
2. Select the target device in Android Studio.
3. Click **Run**.
4. Review Logcat for build or runtime errors.

---

## Command-Line Build

**macOS and Linux**
```bash
./gradlew assembleDebug
```

**Windows**
```powershell
gradlew.bat assembleDebug
```

The generated debug APK is normally located at `app/build/outputs/apk/debug/`.

---

## Testing

**Unit tests**
```bash
./gradlew test          # macOS / Linux
gradlew.bat test        # Windows
```

**Instrumentation tests** (requires an emulator or connected device)
```bash
./gradlew connectedAndroidTest
```

### Recommended Security Tests

The test suite should verify that:

- Malformed input is rejected on the backend
- Authentication errors do not reveal account existence
- Login attempts are rate-limited, with OTP requests and verification tracked separately
- Passwords and OTPs never appear in logs
- Users cannot modify their own roles or access another user's health records
- Exact GPS is not stored by default
- The AI never automatically receives private health information
- Private files cannot be accessed by another user
- Service-role keys and backend secrets are absent from application builds
- Reduced-motion preferences are respected
- Empty states never display fake health data

---

## Dependency Security

Before releasing a new version:

- Review all Gradle dependencies and check for known vulnerabilities
- Update compatible dependencies without forcing breaking upgrades
- Document unresolved high-risk findings
- Run tests after dependency updates
- Keep the Gradle wrapper current and verified

---

## Branch and Pull Request Workflow

The `main` branch stays protected:

- Require a pull request before merging
- Block force pushes and restrict branch deletion
- Require resolved review conversations
- Use squash merging
- Require CodeQL or security checks once correctly configured
- Don't apply `main` restrictions to every development branch

**Create a new branch:**
```bash
git checkout main
git pull origin main
git checkout -b feature/update-name
```

**Commit changes:**
```bash
git add .
git commit -m "feat: describe the LunaCare update"
git push -u origin feature/update-name
```

Then open a pull request into `main`.

### Recommended Commit Format

```text
feat: add menstrual flow tracking
fix: resolve phone OTP verification issue
security: strengthen authentication rate limiting
ui: improve dashboard empty states
refactor: simplify health journal components
test: add cross-user access tests
docs: update project documentation
```

---

## Environment and Secret Management

**Never commit:** Gemini API keys, authentication-provider secrets, Supabase service-role keys, OAuth client secrets, SMS-provider secrets, signing keys, database passwords, encryption keys, or webhook secrets.

**Instead, use:** local environment files for development, GitHub repository secrets for CI/CD, secure deployment secret management, and key rotation after any accidental exposure.

Secrets already committed to Git history should be considered compromised and rotated immediately.

---

## Medical Disclaimer

LunaCare is intended for educational, organisational, and general wellbeing purposes. It does **not** diagnose medical conditions, prescribe medication, provide emergency treatment, replace a doctor or licensed professional, guarantee cycle predictions, make clinical decisions, or confirm pregnancy or any other medical condition.

Users experiencing severe bleeding, fainting, intense pain, breathing difficulty, pregnancy-related concerns, self-harm risk, or another emergency should contact an appropriate healthcare or emergency service.

---

## Privacy Principles

1. Collect only the information required for a selected feature.
2. Keep exact location access disabled by default.
3. Don't store location history.
4. Keep private records isolated by user.
5. Never send health data to AI automatically.
6. Don't store authentication credentials in application tables.
7. Validate and authorize every request on the backend.
8. Provide secure logout and session controls.
9. Keep crisis-support access available without payment.
10. Never use hidden backdoors to access user data.
11. Keep sensitive information out of analytics and logs.
12. Provide clear explanations before requesting permissions.

---

## Development Status

LunaCare is currently under active development. Before a production release, the project should complete:

- Authentication-provider configuration
- Database security review and Row Level Security testing
- Medical-content review
- Privacy and legal review
- Dependency audit and secret scanning
- Accessibility testing
- File-upload security review
- Crisis-support resource verification
- Production signing
- Backup and recovery planning
- Terms of Service and Privacy Policy review
- Penetration testing where appropriate

---

## Contributing

Contributions are welcome through the protected pull-request workflow. Before submitting a pull request:

1. Keep the change focused — don't overwrite unrelated files.
2. Never commit secrets.
3. Reuse existing components where possible; avoid unnecessary dependencies.
4. Run the test suite and confirm the project builds successfully.
5. Document security-sensitive changes.
6. Preserve the LunaCare design system.
7. Follow the project's privacy and medical-safety principles.

---

## Project Links

- **GitHub Repository:** [luna-care-security-review](https://github.com/abrarjawadabir2/luna-care-security-review)
- **Google AI Studio:** [Open LunaCare](https://ai.studio/apps/ae458a1a-d802-40d6-bc03-0bca9bce51af)

---

## License

This project is licensed under the **Apache License 2.0**.

You may use, modify, and distribute this software in accordance with the terms of the license. See the [`LICENSE`](LICENSE) file for complete details.

Copyright © 2026 Abrar Jawad

---

## Maintainer

**Abrar Jawad** — GitHub: [@abrarjawadabir2](https://github.com/abrarjawadabir2)

---

<div align="center">

### LunaCare

**Privacy-focused health support with security, accessibility, and respectful education at its core.**

</div>
