# Account / Firebase Setup — V0.9.3

V0.9.3 keeps per-user accounts and portable cloud backup while separating **authentication** from **Firebase Storage**.

The important rule is:

- Google / Email authentication requires Firebase Authentication configuration.
- Cloud backup additionally requires Firebase Storage.
- A missing Storage bucket must not block login.

## 1. Firebase project
1. Create a Firebase project dedicated to メタらない？.
2. Register Android package `jp.metaranai.app`.
3. Add the SHA-1 and SHA-256 fingerprints from the stable release signing certificate.
4. In Authentication -> Sign-in method, enable the providers you intend to ship.
   - Recommended Android primary: **Google**
   - Fallback: **Email/Password**
   - Optional: Anonymous, Apple, Facebook, Twitter/X
5. Enable Cloud Firestore and Cloud Storage only when cloud backup/sync is required.
6. Deploy `firebase/firestore.rules` and `firebase/storage.rules` before enabling production cloud sync.

## 2. Android Firebase configuration
The primary Android configuration is the Firebase standard setup:

1. Keep the downloaded Firebase config at `app/google-services.json`.
2. The project-level Gradle build declares `com.google.gms.google-services`.
3. The app module applies `com.google.gms.google-services`.
4. Firebase initializes from the generated Android resources at runtime.

The existing BuildConfig environment values remain supported only as a compatibility fallback for old CI/release setups:

- `FIREBASE_API_KEY`
- `FIREBASE_APP_ID`
- `FIREBASE_PROJECT_ID`
- `FIREBASE_STORAGE_BUCKET`
- `GOOGLE_WEB_CLIENT_ID`

Other existing integrations still use their environment values:

- `FACEBOOK_APP_ID`
- `FACEBOOK_CLIENT_TOKEN`
- `SPOTIFY_CLIENT_ID`
- `LASTFM_API_KEY`

Important: after enabling Google Authentication and registering the release / Play App Signing SHA fingerprints, download `google-services.json` again and replace `app/google-services.json`. The refreshed file contains the OAuth Web client used by Credential Manager.

## 3. Google — primary Android login
1. Register package `jp.metaranai.app` in Firebase / Google Cloud.
2. Register the SHA-1 and SHA-256 fingerprints for every signing certificate you use, especially the Google Play **App signing key**.
3. Enable Google in Firebase Authentication.
4. Download the updated `google-services.json` and replace `app/google-services.json`.
5. The app reads the generated `default_web_client_id` first. `GOOGLE_WEB_CLIENT_ID` is retained only as a compatibility fallback.
6. Rebuild and verify Google login using the Play internal-test installation.

V0.9.3 flow:

```text
Googleで続ける
  -> Android Credential Manager
  -> Google ID token
  -> GoogleAuthProvider
  -> FirebaseAuth.signInWithCredential
  -> Firebase UID
```

## 4. Email / Password — fallback login
Enable Email/Password in Firebase Authentication.

New registration flow:

```text
createUserWithEmailAndPassword
  -> sendEmailVerification
  -> sign out unverified session
  -> user opens verification link
  -> user logs in again
```

V0.9.3 does not accept an unverified Email/Password account as a normal signed-in account. If an unverified user attempts login, the app tries to resend the verification email.

## 5. Guest
If Firebase Authentication is configured and Anonymous is enabled, guest uses Firebase Anonymous Auth.

If Firebase is not configured, the existing local-only guest fallback remains available.

## 6. Optional providers
### Apple
Enable Sign in with Apple in Apple Developer and Firebase Authentication. Android can use Firebase's `apple.com` OAuth provider flow. Native iOS Firebase account wiring is a separate release task.

### Facebook
1. Create a Meta app and enable Facebook Login.
2. Add package `jp.metaranai.app` and the release key hash.
3. Configure Firebase Facebook provider with Meta App ID/App Secret.
4. Set `FACEBOOK_APP_ID` and `FACEBOOK_CLIENT_TOKEN`.

### X / Twitter
Create an X developer application, configure Firebase's callback URL, and enable Twitter/X in Firebase Authentication.

## 7. Cloud data model
Cloud portable state is stored at:

`users/{firebaseUid}/metaranai-backup.json`

Firestore stores lightweight backup metadata.

Cloud JSON intentionally omits Spotify access/refresh tokens and local guest account IDs. Manual JSON Backup/Restore remains independent of cloud sync.

If `FIREBASE_STORAGE_BUCKET` is missing, authentication still works but upload/download controls are disabled.

## 8. Existing-user migration — NEVER AUTO OVERWRITE
On first authenticated login when cloud sync is enabled:
- cloud data + local data: prompt which copy wins;
- cloud only: restore cloud data;
- local only: ask before uploading local data;
- neither: continue new-user onboarding.

When Storage is not configured, V0.9.3 does not attempt this cloud migration step.

## 9. Release verification
## 9. Google Play AAB / signing
GitHub Actions always runs `:app:bundleRelease`.

- With the four `METARANAI_KEY*` repository secrets, the workflow publishes `metaranai-v0.9.3-aab`. This is the Play-uploadable signed bundle.
- Without those signing secrets, the workflow still publishes `metaranai-v0.9.3-aab-unsigned` for build verification only. **Do not upload the unsigned bundle to Google Play.**

Required signing secrets:

- `METARANAI_KEYSTORE_B64`
- `METARANAI_KEYSTORE_PASSWORD`
- `METARANAI_KEY_ALIAS`
- `METARANAI_KEY_PASSWORD`

After the first signed AAB is accepted by Play Console, copy the Play App Signing SHA-1 / SHA-256 into the Firebase Android app settings, enable Google sign-in, then re-download `google-services.json`.

## 10. Release verification
Before public release, verify:
- Google login on the release-signed APK;
- Email registration -> verification email -> login;
- Google/Email login with no `FIREBASE_STORAGE_BUCKET`;
- cloud upload/restore when Storage is configured;
- sign-out/sign-in;
- account deletion;
- Firebase Security Rules;
- stable signing SHA fingerprints.

Also run:

```bash
python tools/check_v093_auth.py
```

For a focused V0.9.3 checklist, see `docs/20_V093_AUTH_SETUP.md`.
