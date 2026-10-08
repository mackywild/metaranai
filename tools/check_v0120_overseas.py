from pathlib import Path
root = Path(__file__).resolve().parents[1]
base = root / 'app/src/main/java/jp/metaranai/app'
vm = (base / 'MainViewModel.kt').read_text()
ui = (base / 'MainActivity.kt').read_text()
setup = (base / 'GuidedOnboardingScreen.kt').read_text()
store = (base / 'LocalStore.kt').read_text()
assert '海外アーティストも聴きますか？' in setup
assert ' / 5' in setup and 'regionAnswered' in setup
assert 'SettingsPanel.OVERSEAS -> SettingsDialogShell' in ui
assert 'remember(query, external, overseasPreference)' in ui
assert 'OverseasPreferenceChoices(overseasPreference, onSelect = vm::setOverseasPreference)' in ui
assert 'prefs.getString("overseas_preference_v0120", null)' in store
assert 'if (!o.has("overseas_preference_v0120")) e.remove' in store
assert '_overseasPreference.value = store.overseasPreference()' in vm[vm.index('private fun reloadFromStore'):]
assert 'val catalog = eligibleArtists()' in vm
assert '_remoteSearchResults.value = applyArtistPreference(result.results)' in vm
assert '_remoteSearchSuggestions.value = applyArtistPreference(result.suggestions)' in vm
assert 'DiscoverySearchResults.choose(artists, allArtists(), _profile.value, activeGenres(), rated, _overseasPreference.value)' in vm
assert 'val direct = applyArtistPreference(result.artists)' in vm
assert 'candidates = eligibleArtists()' in vm
assert 'fun archiveArtists' not in vm or 'eligibleArtists()' not in vm[vm.index('fun archiveArtists'):vm.index('fun reactionFor')]
print('V0120_OVERSEAS_PREFERENCE_OK')
