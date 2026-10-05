from pathlib import Path
root = Path(__file__).resolve().parents[1]
ui = (root / 'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()
vm = (root / 'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
client = (root / 'app/src/main/java/jp/metaranai/app/SpotifyClient.kt').read_text()
discovery = (root / 'app/src/main/java/jp/metaranai/app/ExternalDiscoveryClient.kt').read_text()
search = ui[ui.index('private fun SearchScreen'):ui.index('private fun SearchArtistCard')]
assert search.index('OutlinedTextField(') < search.index('"グローバル検索"') < search.index('item { LinkedDiscoveryControls') < search.index('"検索結果') < search.index('items(merged)')
assert 'DeepDivePanel(' not in search
assert 'SearchDeepDiveDialog(' in search
assert 'deepDiveArtist = artist' in search
controls = ui[ui.index('private fun LinkedDiscoveryControls'):ui.index('private fun DnaScreen')]
assert 'Spotifyにログイン' not in controls
assert 'vm.spotifyConnected()' in controls
assert 'vm.syncSpotifyForSearch()' in controls
assert 'onSearchStarted()' in controls
assert 'showSearchResults = true' in controls
assert 'allowAuthorization = !showSearchResults' in vm
assert 'check(allowAuthorization)' in client
manual = vm[vm.index('fun syncExternalDiscovery'):vm.index('private fun discoverySeeds')]
assert 'ensureGenreLensPool' not in manual
assert 'fallbackGenres = fallbackDiscoveryGenres()' in manual
assert 'publishDiscoveryResults(result.discoveredArtists)' in manual
assert 'excludedArtistNames' in manual
assert 'for (page in 1..3)' in discovery
assert 'deepDiveJob?.cancel()' in vm
print('V0112_SEARCH_DISCOVERY_OK')
