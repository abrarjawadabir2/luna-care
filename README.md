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
