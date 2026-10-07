from pathlib import Path
root = Path(__file__).resolve().parents[1]
base = root / 'app/src/main/java/jp/metaranai/app'
ui = (base / 'GuidedOnboardingScreen.kt').read_text()
vm = (base / 'MainViewModel.kt').read_text()
store = (base / 'LocalStore.kt').read_text()
main = (base / 'MainActivity.kt').read_text()
setup = (base / 'SpotifySetup.kt').read_text()
client = (base / 'SpotifyClient.kt').read_text()
assert 'GuidedOnboardingScreen(vm)' in main
for label in ['アカウントを使いますか？', 'Spotifyを連携しますか？', 'Last.fmを連携しますか？', '最初の好みを教えてください']:
    assert label in ui
assert ui.count('SetupSecondary("スキップ"') == 2
assert 'rememberSaveable' in ui and 'PasswordVisualTransformation()' in ui
assert 'vm::completeOnboardingWithArtist' in ui and 'vm.completeOnboardingWithDiagnosis(result)' in ui
assert 'vm::completeOnboardingFromSpotify' in ui
assert 'store.saveExternalArtists(_externalArtists.value)' in vm[vm.index('private fun saveOnboardingArtists'):vm.index('private fun finishTasteOnboarding')]
assert 'store.saveOnboardingSeedArtists' in vm
assert 'archiveDb.replaceAll' in store
assert 'onboardingInProgress()' in store and 'OnboardingPolicy.isComplete' in vm
spotify_start = vm[vm.index('fun startWithSpotifyOnboarding'):vm.index('fun skipSpotifyOnboarding')]
assert 'markOnboardingCompleted' not in spotify_start
assert 'SpotifyRedirectCopyButton()' in ui and 'SpotifyRedirectCopyButton()' in main
assert 'ClipData.newPlainText("Spotify Redirect URI", SpotifySetup.REDIRECT_URI)' in setup
assert 'private val redirectUri = SpotifySetup.REDIRECT_URI' in client
assert 'vm.signInFacebook' not in ui and 'vm.signInProvider' not in ui
print('V0119_GUIDED_ONBOARDING_OK')
