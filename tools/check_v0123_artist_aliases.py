from pathlib import Path
r = Path(__file__).resolve().parents[1]
b = r / 'app/src/main/java/jp/metaranai/app'
store = (b/'LocalStore.kt').read_text()
spotify = (b/'SpotifyClient.kt').read_text()
mb = (b/'MusicBrainzClient.kt').read_text()
ui = (b/'MainActivity.kt').read_text()
vm = (b/'MainViewModel.kt').read_text()
assert 'aliases = buildList' in store and 'o.optString("canonicalName")' in store
assert 'put("aliases", JSONArray()' in store
assert 'OR alias:' in mb and 'exactNameOrAlias' in mb
assert 'ArtistAliases.forLookup(artist)' in spotify
assert 'canonicalExactName(candidate.optString("name")) != target' in spotify
assert 'resolveSpotifyFromMusicBrainz' in spotify and 'spotifyCatalogForCandidate' in spotify
assert 'false -> "Spotifyで検索"' in ui and 'Spotify未対応' not in ui
assert 'openExternalUrl(destination.url)' in vm
assert 'ArtistAliases.availabilityKey(artist)' in vm
assert 'ArtistAliases.expandRatedNames' in vm and 'vm.latestReactionsByArtist()' in ui
assert 'SettingsDialogShell(title = artist.displayName' in ui
assert 'SpotifyRedirectCopyButton' in ui
print('V0123_ARTIST_ALIASES_OK')
