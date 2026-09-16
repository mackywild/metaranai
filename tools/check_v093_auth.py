from pathlib import Path

root = Path(__file__).resolve().parents[1]
acc = (root / 'app/src/main/java/jp/metaranai/app/AccountManager.kt').read_text()
vm = (root / 'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
ui = (root / 'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()
build = (root / 'app/build.gradle.kts').read_text()
workflow = (root / '.github/workflows/android.yml').read_text()
docs = (root / 'docs/17_ACCOUNT_AND_FIREBASE_SETUP.md').read_text()

assert 'versionCode = 21' in build
assert 'versionName = "0.9.3"' in build
assert 'metaranai-v0.9.3-apk' in workflow

# Authentication is no longer blocked by an absent Storage bucket.
auth_block = acc[acc.index('val authConfigured'):acc.index('val configured')]
assert 'FIREBASE_API_KEY' in auth_block
assert 'FIREBASE_APP_ID' in auth_block
assert 'FIREBASE_PROJECT_ID' in auth_block
assert 'FIREBASE_STORAGE_BUCKET' not in auth_block
assert 'val storageConfigured' in acc
assert 'FIREBASE_STORAGE_BUCKET.isNotBlank()' in acc
assert 'if (BuildConfig.FIREBASE_STORAGE_BUCKET.isNotBlank())' in acc

# Google is the primary Android auth flow: Credential Manager -> Google ID token -> Firebase Auth.
assert 'GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)' in acc
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

# Build pipeline exposes auth values independently from the Storage bucket.
for key in ['FIREBASE_API_KEY', 'FIREBASE_APP_ID', 'FIREBASE_PROJECT_ID', 'GOOGLE_WEB_CLIENT_ID']:
    assert key in workflow
assert 'Email/Password' in docs and 'Google' in docs

print('V093_AUTH_OK')
