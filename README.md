<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# LunaCare Security Review

This repository contains the LunaCare AI Studio application and its security, UI, flow, and architecture review materials.

View the app in AI Studio: https://ai.studio/apps/ae458a1a-d802-40d6-bc03-0bca9bce51af

## Implementation specification

The current UI, frontend, backend, flow-information, hashing, rate-limiting, privacy, and database-hardening patch is documented here:

- [LunaCare UI, Flow, Backend, and Security Patch](docs/UI_SECURITY_FLOW_PATCH.md)

The specification is intended to be applied as a careful patch rather than a full rewrite. Working features should be preserved, unnecessary overwrites avoided, and server-side security controls verified before release.

## Run locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open Android Studio.
2. Select **Open** and choose the directory containing this project.
3. Allow Android Studio to resolve compatible project dependencies.
4. Create a `.env` file in the project directory and set `GEMINI_API_KEY` using `.env.example` as the template.
5. Remove this debug-only line from `app/build.gradle.kts` before a release build:

   ```kotlin
   signingConfig = signingConfigs.getByName("debugConfig")
   ```

6. Run the app on an emulator or physical device.

## Security principles

- No hidden backdoor or master password
- No service-role or provider secret in client code
- Server-side rate limiting for authentication and OTP flows
- Purpose-separated HMAC hashing for private identifiers
- Trusted authentication-provider password hashing
- Row Level Security for private user data
- Exact GPS disabled and unstored by default
- No passwords, OTPs, tokens, medical notes, or exact GPS in logs

## Before production release

Complete a manual security review, database/RLS verification, medical-content review, privacy/legal review, and release-signing configuration before publishing the application.
