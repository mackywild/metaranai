# V0.9.2 → V0.9.3

V0.9.3 is an authentication reliability update.

## Changes
- Google is the recommended/primary Android login path.
- Firebase Authentication is separated from Firebase Storage configuration.
- `FIREBASE_STORAGE_BUCKET` is no longer required for Google or Email/Password authentication.
- Email account creation now sends an email verification message and requires verification before normal login is accepted.
- Unverified Email/Password login attempts resend the verification email when possible.
- When Storage is absent, authenticated sessions remain valid and only cloud sync is disabled.
- Google / Email / Guest are emphasized in onboarding; Apple / Facebook / X remain secondary optional providers.
- Added `tools/check_v093_auth.py` and `docs/20_V093_AUTH_SETUP.md`.

## Version
- Android: versionCode 21 / versionName 0.9.3
- iOS project metadata: Marketing Version 0.9.3 / Build 21
- Portable backup format remains version 90.

## Existing data
No reset or migration of METAL DNA, ratings, Local Metal DB, Spotify identity cache, or portable backup format is required.
