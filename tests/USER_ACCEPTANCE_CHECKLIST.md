# LunaCare usability test guide (private data not required)

**Status:** Test plan only. Automated tests are recorded in GitHub Actions; human testing has not yet been conducted.
**Environment:** Authorized beta build, not a production health app. Never use real menstrual, medical, location, journal, or password data in test reports.

## Participants and consent

Recruit consenting adults who are comfortable testing a health-oriented application. Provide a brief description, a stop/withdraw option, and an explanation of what you will record. Use synthetic profile information and fictional health dates. Avoid collecting screenshots of private entries or credentials.

A useful small exploratory group includes first-time Android users, experienced Android users, participants using larger text or screen readers, and people who prefer either self-tracking or educational-only mode. Report the sample size rather than treating a few participants as representative of all users.

## Android tasks (manually perform on a physical device or emulator)

| Task | Expected behavior | Result |
| --- | --- | --- |
| 1. Open app as a guest | Guest option is discoverable; no forced account | Not tested with humans |
| 2. Complete supported onboarding mode | Prompts explain consent, privacy and mode choices clearly | Not tested with humans |
| 3. Record a fictional period | A new period appears in the timeline and persists after relaunch | Not tested with humans |
| 4. Enter invalid cycle length, e.g. 'abc' | Save is disabled; no silent reset to 28 days | Automated Android unit tests for validation; UI not manually tested |
| 5. Write a fictional journal entry | It is saved, readable and removable | Not tested with humans |
| 6. Edit profile and try reserved username 'admin' | Invalid name rejected, Save disabled | Not tested with humans |
| 7. Try an ordinary new username | Format status notes local-only; does NOT claim global reservation | Not tested with humans |
| 8. Quickly edit username and tap Save | Old approved input must not be saved while validation runs | Code fix; needs manual UI check |
| 9. Open AI assistant | Honest 'not connected yet' notice; no fake answer, quota use or paywall | Code fix; needs manual UI check |
| 10. Delete all data, then cancel | Records remain untouched | Not tested with humans |
| 11. Delete all data, then confirm | Local records cleared as documented; no undo promise | Not tested with humans |
| 12. Rotate screen, enable 200% font scale | Content and controls remain usable without text clipping | Not tested with humans |

## Website tasks

Automated real-Chromium test: `python web/tests/test_user_journeys.py`. It runs at 320px, 390px, 768px and 1440px and checks page landmarks, keyboard skip link, internal navigation, FAQ disclosure, external HTTPS links and horizontal overflow. CI saves **public-page-only** screenshots; manually review them after each design change.

Additional manual steps: use keyboard only, try 200% browser zoom, enable reduced motion, inspect screen reader announcements and read all wording for clarity.

## Backend user journey (automated, synthetic accounts)

`backend/tests/user-journeys-e2e.test.ts` covers registration/login, case-insensitive username claims, two-account privacy isolation, period/journal creation, date input rejection, CORS preflight and cache headers. It does not test a deployed database, real OTP delivery or live network resilience.

## Feedback record (one response per participant; leave blank until observed)

- Session ID / date:
- Device model, OS version, accessibility features used:
- Task number and completion: completed / needed help / failed / skipped
- Time or number of attempts (only if measured):
- Confusion or exact quote (with consent, without sensitive data):
- Expected behavior versus observed behavior:
- Severity: critical / high / medium / low
- Reproduction steps on a test account:
- Fix commit and retest result:
- Participant consent for anonymous feedback: yes / no

## Release gate

Do not claim a successful real-user beta until consented human sessions have actually occurred. Resolve critical and high accessibility, privacy, data integrity, and navigation defects; complete Android emulator and physical-device regression checks; verify account persistence/backups/authorization in a real backend; and obtain an approved production-signing and deployment process before public release.
