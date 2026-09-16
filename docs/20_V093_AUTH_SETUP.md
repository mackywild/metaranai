# V0.9.3 Authentication Setup

V0.9.3 separates **authentication** from **cloud backup**.
A missing Firebase Storage bucket no longer blocks Google or Email/Password login.

## Recommended provider order on Android
1. **Google** — primary login.
2. **Email/Password** — fallback. New accounts must verify their email before login is accepted.
3. **Guest** — always available; local-only fallback is preserved when Firebase Auth is not configured.
4. Apple / Facebook / X remain optional providers and are shown as secondary choices.

## Firebase Console
For Android package `jp.metaranai.app`:

1. Add/register the Android app in the Firebase project.
2. Add the **SHA-1 and SHA-256** fingerprints of the stable release signing certificate.
3. Authentication -> Sign-in method:
   - Enable **Google**.
   - Enable **Email/Password**.
   - Enable Anonymous only if Firebase-backed guest accounts are desired.
4. The Web OAuth client ID created for the Firebase/Google project is used as `GOOGLE_WEB_CLIENT_ID`.

## Required build values for Google + Email authentication
Set these GitHub Actions repository secrets (or environment variables):

- `FIREBASE_API_KEY`
- `FIREBASE_APP_ID`
- `FIREBASE_PROJECT_ID`
- `GOOGLE_WEB_CLIENT_ID` — Web OAuth client ID, not the Android client ID.

`FIREBASE_STORAGE_BUCKET` is **not required for authentication**.

## Optional cloud sync
To enable per-user portable backup sync, additionally configure:

- `FIREBASE_STORAGE_BUCKET`
- Firebase Storage security rules
- Firestore security rules for lightweight backup metadata

Without a Storage bucket, login still succeeds and the app shows that cloud sync is unavailable.

## Email registration flow

```text
New registration
  -> Firebase createUserWithEmailAndPassword
  -> sendEmailVerification
  -> app signs the unverified session out
  -> user opens verification link
  -> user logs in with Email/Password
  -> verified account is accepted
```

If an unverified user tries to log in, V0.9.3 attempts to resend the verification email and does not start cloud migration/sync.

## Google login flow

```text
Googleで続ける
  -> Android Credential Manager
  -> Google ID token
  -> GoogleAuthProvider credential
  -> FirebaseAuth.signInWithCredential
  -> Firebase UID
```

The app uses the Web OAuth client ID as the server client ID for the Google ID token request.

## GitHub Actions
After adding/changing repository secrets, rebuild the APK. The values are embedded into `BuildConfig` at build time, so an already-built APK does not change when a secret is edited.

## Release check
Run:

```bash
python tools/check_v093_auth.py
```

Then verify on a physical Android device:
- Google sign-in with the release-signed APK.
- Email registration -> verification link -> login.
- Login with `FIREBASE_STORAGE_BUCKET` absent.
- Cloud sync with Storage configured.
- Sign-out and second sign-in.
