from pathlib import Path
root=Path(__file__).resolve().parents[1]
store=(root/'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
vm=(root/'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
acc=(root/'app/src/main/java/jp/metaranai/app/AccountManager.kt').read_text()
ui=(root/'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()
gradle=(root/'app/build.gradle.kts').read_text()
workflow=(root/'.github/workflows/android.yml').read_text()
ios=(root/'iosApp/MetaranaiIOS/Core/CoreModels.swift').read_text()
assert 'MetalVector(.50f,.50f,.50f,.50f,.50f,.50f,.50f,.50f)' in store
assert 'melody: 0.50' in ios and 'catchy: 0.50' in ios
assert 'out.put("version", 90)' in store
assert 'setLegacyAccountMigrationPending(true)' in vm and 'migrateLocalDataToAccount' in vm
assert 'GenreLensCatalog.vectorFor(genres)' in vm and 'MetalCatalog.artists.filter' not in vm[vm.index('fun completeOnboardingWithGenres'):vm.index('fun startWithSpotifyOnboarding')]
assert 'fun signInGoogle' in acc and 'CredentialManager.create(activity)' in acc and 'GoogleAuthProvider.getCredential' in acc
assert 'GOOGLE_WEB_CLIENT_ID' in gradle and 'credentials:1.6.0' in gradle and 'googleid:1.2.0' in gradle
assert 'fun signInFacebook' in acc and 'FacebookAuthProvider.getCredential' in acc and 'facebook-login:18.3.0' in gradle
assert 'apple.com' in acc and 'twitter.com' in acc
assert 'vm.signInGoogle' in ui and 'vm.signInFacebook' in ui
assert 'FirebaseAuth' in acc and 'FirebaseStorage' in acc and 'metaranai-backup.json' in acc
assert 'FIREBASE_STORAGE_BUCKET' in gradle and 'SPOTIFY_CLIENT_ID' in gradle and 'LASTFM_API_KEY' in gradle
assert 'GOOGLE_WEB_CLIENT_ID' in workflow and 'FACEBOOK_APP_ID' in workflow
assert 'startWithSpotifyOnboarding' in vm
assert 'resolveCloudConflictUseCloud' in vm and 'resolveCloudConflictUseLocal' in vm
assert '何も決めず探索する' not in ui
print('V090_ACCOUNT_PERSONALIZATION_OK')
