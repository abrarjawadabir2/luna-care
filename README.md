# LunaCare

An open-source menstrual wellness project with an Android application, a supporting backend, and a standalone public information page.

[Source](https://github.com/abrarjawadabir2/luna-care) · [Releases](https://github.com/abrarjawadabir2/luna-care/releases) · [Security policy](SECURITY.md) · [License](LICENSE)

## Project status

**In development. Not yet approved for general public health-data use.** The Android app uses local Room storage. The Node backend currently uses an **in-memory database**, so its authentication, usernames, journal and health-record APIs are **not production-persistent**. Production startup is deliberately blocked until a persistent, reviewed PostgreSQL adapter and deployment controls are implemented. Database migration files alone do not make that adapter functional.

The repository's earlier GitHub release contains APKs. Those assets have not been newly verified by this update as safely signed, functional production releases. Do not rely on a debug APK to protect sensitive information.

There is **no native Windows or macOS LunaCare desktop application yet**. The HTML file in `web/index.html` is a public information page, not a replacement for the Android app.

## Current components

| Component | Location | Purpose |
| --- | --- | --- |
| Android application | `app/` | Kotlin, Jetpack Compose and on-device Room tracking |
| Node backend | `backend/` | Fastify API and supporting services; development/test storage only |
| Rust worker | `backend-rust/` | Internal file-inspection component |
| Database migration drafts | `database/migrations/` | PostgreSQL schema and RLS SQL; deployment not verified |
| Public HTML site | `web/index.html` | Static landing page, no login or health-data collection |
| CI | `.github/workflows/` | Builds, lint, tests, dependency checks and security analysis |

## Getting started

Install [Git](https://git-scm.com/downloads), [Android Studio](https://developer.android.com/studio) and the JDK supported by the Gradle project using their official installers. Android Studio can install required SDK components. No Homebrew installation is required.

```bash
git clone https://github.com/abrarjawadabir2/luna-care.git
cd luna-care
```

### macOS and Linux

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

### Windows (PowerShell)

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

### Android Studio

Open the repository root in Android Studio, synchronize Gradle, install the requested Android SDK, and run `app` on an emulator or a test device. The debug build uses the standard per-developer Android debug signing key; do not use that key for production releases.

### Backend development

Install a supported [Node.js LTS](https://nodejs.org/) release. Then:

```sh
cd backend
npm ci
npm run typecheck
npm test
npm run build
npm run dev
```

Create `backend/.env` from `backend/.env.example` for local testing only. **Never commit secrets.** The backend intentionally refuses `NODE_ENV=production` while storage remains in-memory.

### Rust worker

If working on the internal Rust inspector, install Rust from [rustup.rs](https://rustup.rs/), then run:

```sh
cd backend-rust
cargo fmt --check
cargo clippy --all-targets --all-features -- -D warnings
cargo test
```

### Public HTML page

`web/index.html` is a dependency-free informational webpage. Open it directly in a browser or publish it with a static web host. It is not connected to user profiles, period records or the backend.

## Developer agent tooling (optional)

LunaCare does not bundle ECC into the application. Developers who use Antigravity can review [affaan-m/ECC](https://github.com/affaan-m/ECC) and optionally run `scripts/setup-ecc.sh` on macOS/Linux or `scripts/setup-ecc.ps1` on Windows. Both invoke the pinned `ecc-universal@2.2.3` minimal Antigravity setup. This is **not** part of runtime installation, CI, or app release builds. Review third-party scripts before running them.

## Android release signing

A publishable Android release requires a private keystore, verified release signing, successful checks, privacy review and a production-ready data architecture. Tag-triggered release CI expects the GitHub Actions secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_STORE_PASSWORD` and `ANDROID_KEY_PASSWORD`. The private keystore must contain the alias `upload`. No real credentials or signing files belong in Git.

Do not treat a generated but unsigned `app-release.apk` as a distributable installer.

## Security and privacy

Health data is sensitive. Use synthetic information when testing. Never put passwords, OTP values, service-role keys or private medical notes in public repositories, analytics, logs or agent prompts. Production services must enforce authentication, authorization, rate limits, private storage, retention and backups; the current backend does **not** meet all release requirements.

Please follow [SECURITY.md](SECURITY.md) when reporting issues. Do not disclose exploitable details in public issues before a fix is available.

## Medical disclaimer

LunaCare is for education and personal wellness tracking. It cannot diagnose, prescribe, provide emergency care, or guarantee pregnancy or contraception outcomes. Seek a qualified clinician for medical concerns.

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
