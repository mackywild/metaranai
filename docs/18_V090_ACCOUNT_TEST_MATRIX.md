# V0.9.0 Account & Personalization Test Matrix

## Automated release gates
- [x] NEW_USER_NEUTRAL_DNA — no profile key => all 8 dimensions start at 0.50.
- [x] GENRE_ONBOARDING_NO_LEGACY_BIAS — initial DNA uses `GenreLensCatalog.vectorFor`, not the old built-in artist catalogue.
- [x] LEGACY_PROFILE_PRESERVED — saved V0.4–V0.8 profile path remains unchanged.
- [x] LEGACY_JSON_RESTORE — old `metaranai-backup` JSON remains accepted and SQLite rebuild path remains present.
- [x] ACCOUNT_MIGRATION_NO_OVERWRITE — local + cloud enters explicit conflict choice.
- [x] GOOGLE_AUTH_PATH — Credential Manager + Google ID token + Firebase credential path is wired.
- [x] FACEBOOK_AUTH_PATH — Meta Login SDK token + Firebase `FacebookAuthProvider` path is wired.
- [x] APPLE_X_AUTH_PATH — Firebase `apple.com` / `twitter.com` OAuthProvider paths are wired.
- [x] GUEST_MODE — local guest fallback exists without Firebase.
- [x] CROSS_PLATFORM_BACKUP_CODEC — V0.9 export remains `format=metaranai-backup`, version 90; Swift core round-trip passes.
- [x] YUTARO_SPOTIFY_REGRESSION — `Yutaro Abe's ASTRAL WIND` fixture continues to pass.

## Manual / real-account gates before public release
These cannot be truthfully completed without the project owner's Firebase/Google/Meta/X/Apple credentials and real devices.
- [ ] Google real account sign-in on signed Android APK (release SHA-1/SHA-256 registered).
- [ ] Facebook real account sign-in with Meta app/key hash configured.
- [ ] Apple and X provider real sign-in.
- [ ] Email create/sign-out/sign-in.
- [ ] Second-device cloud restore using the same UID.
- [ ] Local+cloud conflict: verify neither side is overwritten before explicit choice.
- [ ] Account deletion with provider re-authentication where required.
- [ ] Firebase Storage/Firestore Security Rules deny another UID.
- [ ] iOS provider SDK wiring and TestFlight account flow.

Public release is blocked until the relevant enabled-provider manual gates pass.
