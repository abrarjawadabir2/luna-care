````markdown
<div align="center">

# LunaCare

### Private, supportive, and security-focused women’s health companion

LunaCare is an Android health and wellbeing application designed to support menstrual health tracking, mood reflection, medical journaling, educational awareness, and privacy-conscious access to AI-assisted guidance.

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](#)
[![Build](https://img.shields.io/badge/Build-Gradle-02303A?style=flat-square&logo=gradle&logoColor=white)](#)
[![AI](https://img.shields.io/badge/AI-Gemini-4285F4?style=flat-square&logo=google&logoColor=white)](#)
[![Security](https://img.shields.io/badge/Focus-Security%20%26%20Privacy-8A4D4E?style=flat-square)](#)
[![Status](https://img.shields.io/badge/Status-Active%20Development-5D5A84?style=flat-square)](#)

[Open in Google AI Studio](https://ai.studio/apps/ae458a1a-d802-40d6-bc03-0bca9bce51af) ·
[View Repository](https://github.com/abrarjawadabir2/luna-care-security-review)

</div>

---

## Overview

LunaCare provides a calm and user-friendly environment where users can record health information, understand menstrual patterns, reflect on emotional wellbeing, and access educational support.

The project places strong emphasis on:

- Privacy-conscious health-data handling
- Secure authentication
- Menstrual-cycle and flow tracking
- Mood and wellbeing check-ins
- Medical journaling
- Educational health content
- Crisis-support accessibility
- Responsible AI assistance
- Accessible and reduced-motion-friendly interfaces

> LunaCare provides educational and wellbeing support. It does not diagnose medical conditions or replace advice from qualified healthcare professionals.

---

## Core Features

### Menstrual Health

- Period and cycle logging
- Flow-level tracking
- Symptom recording
- Pain-level tracking
- Product-use information
- Cycle-history overview
- Non-diagnostic health warnings

Supported flow levels:

- Spotting
- Light
- Medium
- Heavy
- Very heavy

### Mood and Wellbeing

- Daily mood check-ins
- Emotional reflection
- Behaviour and symptom notes
- Supportive wellbeing suggestions
- Crisis-support access

### Medical Journal

- Private health notes
- Symptom and medicine records
- Personal questions for medical appointments
- Date-based journal history
- Protected user-owned records

### Health Education

- Menstrual-health awareness
- PCOS and PCOD information
- Menstrual-cup care
- Hygiene guidance
- Self-care education
- Warning-sign awareness

### Luna AI Assistant

- Educational cycle support
- Health-topic explanations
- Self-care suggestions
- Assistance preparing questions for a doctor
- Privacy-conscious AI interactions

The AI assistant must not automatically receive private medical notes, authentication information, or exact GPS coordinates.

### Nearby Care

- Manual city-based search
- Temporary location access
- No background location tracking
- Exact GPS disabled by default
- Temporary coordinates cleared after use

---

## Authentication Options

LunaCare supports the following authentication methods when configured:

- Google
- Facebook
- Email and password
- Phone number with OTP verification

Security requirements include:

- Generic authentication error messages
- Server-side rate limiting
- Separate OTP request and verification limits
- Temporary cooldown protection
- Secure provider-managed password hashing
- No plaintext password storage
- No OTP values in logs
- No hidden authentication bypass
- No master password or universal OTP

---

## Security and Privacy

LunaCare follows a security-first design approach.

### Authentication Security

- Trusted authentication-provider integration
- Secure session handling
- Rate limiting for login and OTP endpoints
- Account-enumeration protection
- Generic error responses
- Secure password-reset flows
- Optional multi-factor authentication support

### Database Security

- Row Level Security for private records
- User-owned data access
- Deny-by-default database policies
- Server-verified user identity
- Protected role-management logic
- No client-controlled administrator access

### Sensitive Data Protection

LunaCare must never log:

- Passwords
- OTP codes
- Access tokens
- Refresh tokens
- Full phone numbers
- Private medical-journal content
- Crisis-related text
- Exact GPS coordinates

Non-password identifiers used for security monitoring should use purpose-separated server-side HMAC hashing.

### No-Backdoor Policy

The application must not include:

- Hidden administrator accounts
- Master passwords
- Universal OTP codes
- Secret login routes
- Hardcoded authentication credentials
- Client-side administrator flags
- Production debug-authentication bypasses

---

## User Interface

LunaCare uses a soft and professional healthcare-inspired visual system.

| Design element | Value |
|---|---|
| Background | `#FFF9F8` |
| Primary | `#8A4D4E` |
| Secondary | `#5D5A84` |
| Primary container | `#F3D5D4` |
| Secondary container | `#E5E1FF` |
| Main text | `#2C2021` |
| Secondary text | `#665354` |
| Error | `#BA1A1A` |
| Error container | `#FFDAD6` |

Interface principles:

- Clean and calm visual hierarchy
- Rounded cards and controls
- Accessible contrast
- Minimum 44×44-pixel touch targets
- Clear loading and error states
- Friendly empty-state messages
- Reduced-motion support
- Responsive mobile layouts
- No unnecessary animation

---

## Technology

The project is developed as an Android application using:

- Android Studio
- Gradle
- Google AI Studio
- Gemini API
- Android SDK
- Secure environment configuration

The application should continue using its existing technology stack and should avoid unnecessary frameworks, duplicated services, or unrelated runtime languages.

---

## Run Locally

### Requirements

Install the following before opening the project:

- [Android Studio](https://developer.android.com/studio)
- Android SDK
- Java Development Kit supported by the project
- Git
- A Gemini API key

### 1. Clone the repository

```bash
git clone https://github.com/abrarjawadabir2/luna-care-security-review.git
cd luna-care-security-review
````

### 2. Open the project

1. Launch Android Studio.
2. Select **Open**.
3. Choose the cloned project directory.
4. Wait for Gradle synchronization to complete.
5. Allow Android Studio to install required SDK components if prompted.

### 3. Configure the environment

Create a file named `.env` in the project root:

```env
GEMINI_API_KEY=your_gemini_api_key
```

Use `.env.example` as a reference when available.

Never commit the real `.env` file or production credentials.

Confirm `.gitignore` includes:

```gitignore
.env
.env.*
!.env.example
local.properties
*.jks
*.keystore
```

### 4. Configure the debug build

If Android Studio reports a signing-configuration error, review:

```text
app/build.gradle.kts
```

For local development only, remove or correct the following line when it references a missing configuration:

```kotlin
signingConfig = signingConfigs.getByName("debugConfig")
```

Do not remove a valid production signing configuration.

### 5. Run the application

1. Start an Android emulator or connect a physical Android device.
2. Enable USB debugging when using a physical device.
3. Select the target device in Android Studio.
4. Click **Run**.

---

## Build from the Command Line

### macOS or Linux

```bash
./gradlew assembleDebug
```

### Windows

```powershell
gradlew.bat assembleDebug
```

The debug APK is normally generated inside:

```text
app/build/outputs/apk/debug/
```

---

## Testing

Run local unit tests:

### macOS or Linux

```bash
./gradlew test
```

### Windows

```powershell
gradlew.bat test
```

Run Android instrumentation tests with an emulator or connected device:

```bash
./gradlew connectedAndroidTest
```

Security testing should verify:

* Users cannot access another user’s health records
* OTP requests and verification attempts are rate-limited separately
* Authentication errors do not expose whether an account exists
* Passwords and OTPs never appear in logs
* Exact GPS is not stored by default
* Private data is not automatically sent to the AI assistant
* Administrator privileges cannot be controlled from the client
* File uploads cannot execute code or cross user boundaries
* Service-role keys and server secrets are absent from application builds

---

## Recommended Branch Workflow

The `main` branch should remain protected.

Create a separate branch for each update:

```bash
git checkout main
git pull origin main
git checkout -b feature/update-name
```

Commit the changes:

```bash
git add .
git commit -m "feat: describe the LunaCare update"
git push -u origin feature/update-name
```

Then open a pull request and merge it after review and successful checks.

Recommended protections:

* Require a pull request before merging
* Block force pushes
* Restrict branch deletion
* Require resolved review conversations
* Require successful security checks when CI is configured
* Use squash merging for a clean history

---

## Environment Variables

Only public configuration may be included in client-visible code.

Example:

```env
GEMINI_API_KEY=
```

Authentication-provider, database, SMS, signing, and security secrets must be stored in secure server-side or deployment secret storage.

Never commit:

* API secrets
* Service-role keys
* OAuth client secrets
* SMS-provider secrets
* Signing keys
* Database passwords
* Private encryption keys

---

## Medical Disclaimer

LunaCare is intended for educational, organisational, and general wellbeing purposes.

It does not:

* Diagnose medical conditions
* Provide emergency medical treatment
* Replace a doctor, nurse, counsellor, or licensed medical professional
* Guarantee the accuracy of cycle predictions
* Make clinical decisions on behalf of users

Users experiencing severe bleeding, fainting, intense pain, breathing difficulty, pregnancy-related concerns, self-harm risk, or another emergency should contact an appropriate local emergency or medical service.

---

## Privacy Principles

LunaCare is designed around the following principles:

1. Collect only information necessary for a selected feature.
2. Keep location access disabled by default.
3. Do not store exact location unless explicitly required and consented to.
4. Do not expose one user’s information to another user.
5. Do not send private health data to AI automatically.
6. Do not store authentication credentials in application tables.
7. Use secure backend validation and authorization.
8. Provide clear deletion, logout, and session-control options.
9. Keep crisis-support access available without a paywall.
10. Never use hidden backdoors to access private user information.

---

## Development Status

LunaCare is under active development.

Before a production release, the application should complete:

* Security review
* Privacy review
* Medical-content review
* Row Level Security testing
* Dependency audit
* Secret scanning
* Accessibility testing
* Authentication-provider configuration
* Production signing
* Crisis-support resource verification
* Terms and Privacy Policy review

---

## Contributing

Contributions should follow the protected-branch workflow.

Before submitting a pull request:

1. Keep changes focused.
2. Do not overwrite unrelated files.
3. Do not commit secrets.
4. Run the test suite.
5. Check the project build.
6. Document security-sensitive changes in the pull request.
7. Avoid adding new dependencies unless necessary.
8. Preserve the LunaCare design and privacy principles.

---

## Project Links

* **GitHub:** [https://github.com/abrarjawadabir2/luna-care-security-review](https://github.com/abrarjawadabir2/luna-care-security-review)
* **Google AI Studio:** [https://ai.studio/apps/ae458a1a-d802-40d6-bc03-0bca9bce51af](https://ai.studio/apps/ae458a1a-d802-40d6-bc03-0bca9bce51af)

---

<div align="center">

### LunaCare

Built with a focus on privacy, safety, accessibility, and respectful health education.

</div>
```
