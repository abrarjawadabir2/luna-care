# LunaCare

**Privacy-First, Trauma-Informed Menstrual & Reproductive Health Platform**

[![CI](https://github.com/abrarjawadabir2/luna-care/actions/workflows/ci.yml/badge.svg)](https://github.com/abrarjawadabir2/luna-care/actions/workflows/ci.yml)
[![CodeQL](https://github.com/abrarjawadabir2/luna-care/actions/workflows/codeql.yml/badge.svg)](https://github.com/abrarjawadabir2/luna-care/actions/workflows/codeql.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

## Overview

LunaCare is an open-source, local-first menstrual and reproductive health application designed to empower menstruators, learners, and supportive partners with dignified, stigma-free tracking, evidence-based education, and confidential self-care tools.

Rooted in a **zero-surveillance philosophy**, LunaCare keeps intimate biological timelines and notes sovereign to the user's device. When optional cloud synchronization or AI assistance is enabled, all interactions are mediated through a hardened, trusted backend that enforces strict tenant isolation, cryptographic sanitization, and automated crisis heuristics.

---

## Features

- **Local-First Cycle & Flow Tracking:** Log period start/end dates, flow intensity (Spotting, Light, Medium, Heavy), and physical symptoms with on-device calculation of cycle lengths and variation windows.
- **Holistic Behavioral & Mood Logging:** Track emotional well-being, stress, sleep quality, pain levels, and hydration with correlation trend visualizations.
- **Medical Journal & Clinical Preparation:** Structure clinical notes, medication compliance, and appointment reminders to facilitate clear communication with healthcare professionals.
- **Evidence-Based Reproductive Education:** Curated medical articles covering cycle biology, PCOS/PCOD management, and comprehensive menstrual cup folding, insertion, and hygiene protocols.
- **Supporter Mode:** Dedicated, privacy-respecting view for partners and caregivers providing cycle phase education and supportive care guidance without exposing intimate personal journals.
- **On-Device PIN Security:** Protect sensitive local data with on-device PIN authentication backed by PBKDF2 hashing and automatic 15-minute lockout thresholds.
- **Immediate Crisis Support:** Integrated heuristic detection for urgent distress and self-harm keywords, instantly presenting vetted local and international crisis helpline cards.

---

## Architecture Overview

LunaCare employs a defense-in-depth architecture separating client-side presentation from privileged services:

```
[ Native Android Client (Kotlin / Compose) ]
                  │
                  ▼
         [ Room Database ] (Local-First On-Device Storage)
                  │
                  ▼ (Optional Sync via TLS 1.3 + JWT)
    [ Trusted Backend Gateway (Fastify / TypeScript) ]
       ├── Sanitized Audit Logging (Purpose-Hashed HMAC)
       ├── File Inspector Worker (Rust / Memory-Safe)
       ├── Automated Maintenance & Deletion Scheduler
       ├── PostgreSQL / Supabase Database (Row-Level Security)
       └── Gemini AI Platform (Redacted Context Mediation)
```

- **Client Layer:** Android application written in Kotlin and Jetpack Compose utilizing Room SQLite for offline-first data sovereignty.
- **Trusted Gateway:** Fastify backend enforcing strict input validation (Zod schemas), sliding-window rate limiting, and global search engine blocking (`X-Robots-Tag: noindex`).
- **File Inspector:** Focused Rust worker utilizing `#![forbid(unsafe_code)]` to enforce strict magic-byte verification and 5MB size limits on all media uploads.
- **Database Layer:** PostgreSQL with Row-Level Security (RLS) guaranteeing strict tenant isolation.

---

## Technology Stack

- **Android Client:** Kotlin 2.0+, Jetpack Compose, Material 3, Android Architecture Components (ViewModel, StateFlow), Room 2.6+.
- **Backend Service:** Node.js 22+, Fastify v5, TypeScript, Zod, `@fastify/helmet`, `@fastify/cors`.
- **Security Worker:** Rust 1.75+, `#![forbid(unsafe_code)]`, Serde.
- **Database:** PostgreSQL 15+ / Supabase.
- **CI/CD & Testing:** GitHub Actions, CodeQL static analysis, Roborazzi screenshot testing, Node.js test runner.

---

## Prerequisites

Before setting up LunaCare, ensure the following tools are installed on your workstation using official installation methods:

- **Git:** Version 2.30+ ([git-scm.com](https://git-scm.com/))
- **Java Development Kit (JDK):** JDK 17 or JDK 21 LTS ([adoptium.net](https://adoptium.net/))
- **Android Studio:** Ladybug (2024.2.1+) or newer ([developer.android.com/studio](https://developer.android.com/studio))
- **Node.js & npm (for backend development):** Node.js 20+ LTS ([nodejs.org](https://nodejs.org/))
- **Rust toolchain (for Rust file inspector):** Installed via the official installer ([rustup.rs](https://rustup.rs/))

---

## Setup & Installation

Clone the repository and navigate to the project directory:

```bash
git clone https://github.com/abrarjawadabir2/luna-care.git
cd luna-care
```

### macOS

Ensure your installed tools are accessible from the terminal:

```bash
git --version
java -version
node --version
npm --version
```

Build and test the Android client using the Gradle wrapper:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### Windows

Open PowerShell or Windows Terminal:

```powershell
git --version
java -version
node --version
npm --version
```

Build and test the Android client using the Windows Gradle wrapper:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

### Linux

Verify system prerequisites:

```bash
git --version
java -version
node --version
npm --version
```

Ensure the Gradle wrapper has execution permissions, then build:

```bash
chmod +x ./gradlew
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### Android Studio Setup

1. Launch **Android Studio**.
2. Select **Open** and choose the cloned `luna-care` directory.
3. Allow Gradle to synchronize dependencies.
4. When prompted, install the required Android SDK Platform (API 35) and Build Tools via the Android Studio SDK Manager.
5. Create a local environment configuration if cloud features are required:
   ```bash
   cp .env.example .env
   ```
6. Launch an Android Virtual Device (AVD) running API 34+ or connect a physical device with USB debugging enabled.
7. Click **Run 'app'** (or press `Shift + F10`) to deploy the debug build.
8. **Security Note:** Never commit `local.properties` or private release signing keystores.

---

## Backend Development

To develop or test the trusted backend gateway:

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Install dependencies:
   ```bash
   npm ci
   ```
3. Create your local environment configuration:
   ```bash
   cp .env.example .env
   ```
   *Fill in your local database connection and generate a minimum 32-character secret for `JWT_SECRET`.*
4. Start the development server:
   ```bash
   npm run dev
   ```
5. Run the backend security boundary tests:
   ```bash
   npm test
   ```

---

## Rust File Inspector Setup

If modifying the media inspection worker:

1. Verify the official Rust toolchain:
   ```bash
   rustc --version
   cargo --version
   ```
2. Navigate to the worker directory:
   ```bash
   cd backend-rust
   ```
3. Run the memory safety and format inspection tests:
   ```bash
   cargo test
   ```
4. Build the release binary:
   ```bash
   cargo build --release
   ```

---

## Testing & Quality Assurance

LunaCare enforces comprehensive verification across all project components:

```bash
# 1. Android unit & algorithm tests
./gradlew testDebugUnitTest

# 2. Backend security boundaries & threat defenses
cd backend && npm test

# 3. Rust memory safety & file signature tests
cd backend-rust && cargo test
```

---

## Security Principles

- **Zero Secrets in Version Control:** All API tokens, JWT signing secrets, database credentials, and signing keys are supplied exclusively via environment variables or secret managers.
- **Client-Side Minimization:** Privileged credentials (such as `GEMINI_API_KEY` or database service keys) never reside within the Android APK.
- **Defense in Depth:** Every request to sensitive endpoints requires verified authentication, server-side authorization checks, and database-level Row-Level Security (RLS).
- **Constant-Time Verification:** Login, registration, and OTP verification endpoints return uniform error codes to prevent user account enumeration.
- **Search Engine Blocking:** All API endpoints and authenticated surfaces emit `X-Robots-Tag: noindex, nofollow, noarchive` headers.

---

## Privacy Principles

- **No Third-Party Advertising SDKs:** LunaCare does not incorporate tracking beacons, advertising networks, or third-party behavioral analytics.
- **Coarse Geolocation:** Healthcare facility lookups utilize coarse, city-level bounding boxes. Exact GPS coordinates are never transmitted or persisted on backend servers.
- **Full Data Sovereignty:** Users can export their complete data record at any time or trigger immediate cascading account deletion across all tables.

---

## Contributing

We welcome contributions that respect user privacy and adhere to our security architecture:

1. Fork the repository and create a feature branch (`git checkout -b feat/your-feature`).
2. Adhere to the established code conventions (Kotlin, TypeScript, Rust).
3. Ensure all tests pass (`./gradlew testDebugUnitTest`, `npm test`, `cargo test`).
4. Ensure no un-ignored configuration files or credentials enter the commit.
5. Submit a descriptive Pull Request against the `main` branch.

---

## Medical Disclaimer

LunaCare is an educational and self-tracking application designed to assist individuals in observing personal wellness trends. **LunaCare is not a certified medical device and does not provide formal medical diagnoses, clinical treatment plans, or contraception guarantees.** Always consult a qualified healthcare provider regarding reproductive health concerns, severe pain, or medical conditions.

---

## License

LunaCare is licensed under the [Apache License, Version 2.0](LICENSE).  
Copyright 2026 Abrar Jawad. See [NOTICE](NOTICE) for additional attribution details.
