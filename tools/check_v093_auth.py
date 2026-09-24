import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
acc = (root / 'app/src/main/java/jp/metaranai/app/AccountManager.kt').read_text()
vm = (root / 'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
ui = (root / 'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()
root_build = (root / 'build.gradle.kts').read_text()
build = (root / 'app/build.gradle.kts').read_text()
workflow = (root / '.github/workflows/android.yml').read_text()
docs = (root / 'docs/17_ACCOUNT_AND_FIREBASE_SETUP.md').read_text()
google_services = json.loads((root / 'app/google-services.json').read_text())

assert 'versionCode = 21' in build
assert 'versionName = "0.9.3"' in build
assert 'metaranai-v0.9.3-apk' in workflow

# Firebase standard Android integration must be wired through google-services.json.
assert 'id("com.google.gms.google-services") version "4.5.0" apply false' in root_build
assert 'id("com.google.gms.google-services")' in build
assert 'com.google.firebase:firebase-bom:34.19.0' in build
clients = google_services.get('client', [])
assert any(
    c.get('client_info', {}).get('android_client_info', {}).get('package_name') == 'jp.metaranai.app'
    for c in clients
)

# Standard google-services resources are primary, BuildConfig remains a compatibility fallback.
assert 'FirebaseApp.initializeApp(context)' in acc
assert 'FirebaseApp.DEFAULT_APP_NAME' in acc
assert 'legacyFirebaseConfigured()' in acc
assert '"default_web_client_id"' in acc
assert 'BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()' in acc
assert 'val authConfigured: Boolean get() = auth != null' in acc

# Authentication is no longer blocked by an absent Storage bucket.
assert 'val storageConfigured: Boolean get() = storage != null' in acc
assert 'firebaseApp.options.storageBucket' in acc
assert 'val cloudSyncConfigured: Boolean get() = storageConfigured' in acc

# Google is the primary Android auth flow: Credential Manager -> Google ID token -> Firebase Auth.
assert 'val webClientId = generatedWebClientId()' in acc
assert 'GetSignInWithGoogleOption.Builder(webClientId)' in acc
assert 'CredentialManager.create(activity).getCredential' in acc
assert 'GoogleAuthProvider.getCredential(google.idToken, null)' in acc
assert 'Googleで続ける' in ui
assert 'おすすめ' in ui

# Email is a fallback flow and requires verification before login is accepted.
assert acc.count('sendEmailVerification()') >= 2
assert 'メール認証が完了していません' in acc
assert '確認メールを送信して登録' in ui
assert '既存メールでログイン' in ui

# Cloud sync is optional after successful authentication.
assert 'val cloudConfigured: Boolean get() = accounts.cloudSyncConfigured' in vm
assert 'if (!accounts.cloudSyncConfigured)' in vm
assert 'ログインしました（クラウド同期は未設定）' in vm
assert 'enabled = vm.cloudConfigured' in ui
assert 'ログインは利用可能です。クラウド同期のみ' in ui

# CI must always emit an AAB artifact; only the signed artifact is Play-uploadable.
assert ':app:bundleRelease' in workflow
assert 'metaranai-v0.9.3-aab' in workflow
assert 'metaranai-v0.9.3-aab-unsigned' in workflow
assert 'METARANAI_KEYSTORE_B64' in workflow

# Google sign-in needs an OAuth web client in the refreshed google-services.json.
oauth_clients = [
    oauth
    for client in clients
    for oauth in client.get('oauth_client', [])
    if oauth.get('client_type') == 3 and oauth.get('client_id')
]
if not oauth_clients:
    print('V093_AUTH_WARN: google-services.json has no Web OAuth client yet. Enable Google sign-in, register SHA fingerprints, then re-download the file.')

assert 'google-services.json' in docs
assert 'Email/Password' in docs and 'Google' in docs

print('V093_AUTH_OK')
