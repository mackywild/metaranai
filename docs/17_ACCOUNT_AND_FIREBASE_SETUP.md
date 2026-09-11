# V0.9.0 Account / Firebase Setup

V0.9.0 adds per-user accounts and cloud backup while preserving the legacy `metaranai-backup` JSON as the portable recovery format.

## 1. Firebase project
1. Create a Firebase project dedicated to メタらない？.
2. Enable Authentication providers to ship: Google, Apple, Facebook, Twitter/X, Email/Password and Anonymous.
3. Enable Cloud Firestore and Cloud Storage.
4. Deploy the rules in `firebase/firestore.rules` and `firebase/storage.rules` before distribution.

## 2. Android build secrets
Set these as GitHub Actions repository secrets (or equivalent environment variables):
- `FIREBASE_API_KEY`
- `FIREBASE_APP_ID`
- `FIREBASE_PROJECT_ID`
- `FIREBASE_STORAGE_BUCKET`
- `GOOGLE_WEB_CLIENT_ID` — Firebase/Google OAuth Web client ID used by Credential Manager
- `FACEBOOK_APP_ID`
- `FACEBOOK_CLIENT_TOKEN`
- `SPOTIFY_CLIENT_ID`
- `LASTFM_API_KEY` — app-level Last.fm key so a brand-new user can discover outside the bundled seeds

The Android app initializes Firebase from BuildConfig, so `google-services.json` is not required by this project layout.

## 3. Provider setup
### Google
1. Register Android package `jp.metaranai.app` in Firebase/Google Cloud.
2. Register SHA-1 and SHA-256 from the stable release signing certificate.
3. Enable Google Authentication in Firebase.
4. Put the Web OAuth client ID in `GOOGLE_WEB_CLIENT_ID`.
5. V0.9.0 uses Android Credential Manager -> Google ID token -> Firebase Auth. Do not route Google through generic OAuthProvider.

### Apple
Enable Sign in with Apple in Apple Developer and Firebase Authentication. Android uses Firebase's `apple.com` OAuth provider flow. iOS release wiring still needs the Apple/Firebase SDK configuration in Xcode.

### Facebook
1. Create a Meta app and enable Facebook Login.
2. Add package `jp.metaranai.app` and the release key hash.
3. Configure Firebase Facebook provider with Meta App ID/App Secret.
4. Set `FACEBOOK_APP_ID` and `FACEBOOK_CLIENT_TOKEN` for the Android build.
5. V0.9.0 Android uses the Meta Facebook Login SDK access token -> `FacebookAuthProvider` -> Firebase Auth.

### X
Create an X developer application, configure Firebase's callback URL, enable Twitter/X in Firebase and configure its API key/secret. Android uses Firebase `twitter.com` OAuthProvider.

### Email / Guest
Enable Email/Password and Anonymous in Firebase Authentication. Guest also has a local-only fallback when Firebase is not configured.

## 4. Cloud data model
Portable state is stored at:
`users/{firebaseUid}/metaranai-backup.json`

Firestore stores lightweight metadata. Cloud JSON intentionally removes Spotify access/refresh tokens and local guest account IDs. Manual JSON Backup/Restore remains independent of cloud sync.

## 5. Existing-user migration — NEVER AUTO OVERWRITE
V0.9.0 does not clear existing local data. On first authenticated login:
- cloud data + local data: prompt the user to choose which wins;
- cloud only: restore cloud data;
- local only: ask before uploading the existing data to the new account;
- neither: start new-user onboarding.

Old V0.5/V0.6/V0.7/V0.8 JSON backup versions remain importable and rebuilding the SQLite archive from `external_artists` remains part of restore.

## 6. New-user personalization
New installs use a neutral 0.50 MetalVector. They must build the initial DNA with either:
- Spotify listening analysis; or
- one or more Genre Lens selections.

Genre onboarding uses `GenreLensCatalog.vectorFor(...)`, not the old bundled artist catalogue. This prevents the historical melodic/power-heavy seed from becoming every new user's DNA.

## 7. Release verification
Before public release, verify every enabled provider, sign-out/sign-in, second-device restore, V0.6.x JSON migration, account deletion, Firebase Security Rules, and the stable signing fingerprints. Also run the permanent Yutaro Abe's ASTRAL WIND Spotify identity regression.
