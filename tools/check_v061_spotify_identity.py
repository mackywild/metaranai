from pathlib import Path
root=Path(__file__).resolve().parents[1]
spotify=(root/'app/src/main/java/jp/metaranai/app/SpotifyClient.kt').read_text()
store=(root/'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
vm=(root/'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
models=(root/'app/src/main/java/jp/metaranai/app/Models.kt').read_text()
build=(root/'app/build.gradle.kts').read_text()
assert 'versionCode = 14' in build and 'versionName = "0.6.4"' in build
assert 'store.spotifyArtistLinkV064(artist)' in spotify
assert 'spotify_artist_links_v064' in store
resolver=spotify.split('suspend fun resolveArtistDestination',1)[1].split('private data class CandidateFingerprint',1)[0]
assert 'normalized.contains(target)' not in resolver and 'target.contains(normalized)' not in resolver
assert 'canonicalExactName(candidate.optString("name")) != target' in spotify
assert 'resolveSpotifyFromMusicBrainz' in resolver
assert 'artist.metadataConfidence >= 90' in resolver
assert 'verification: String = ""' in models
assert 'spotify.resolveArtistDestination(artist)' in vm
print('V061_SAFETY_COMPAT_OK')
