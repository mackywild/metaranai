from pathlib import Path
root=Path(__file__).resolve().parents[1]
spotify=(root/'app/src/main/java/jp/metaranai/app/SpotifyClient.kt').read_text()
store=(root/'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
vm=(root/'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
models=(root/'app/src/main/java/jp/metaranai/app/Models.kt').read_text()
build=(root/'app/build.gradle.kts').read_text()

assert 'versionCode = 13' in build
assert 'versionName = "0.6.3"' in build
assert 'suspend fun resolveArtistDestination(artist: MetalArtist)' in spotify
assert 'store.spotifyArtistLinkV063(artist)' in spotify
assert 'spotify_artist_links_v063' in store
assert 'spotify_artist_links_v05' in store and 'spotify_artist_links_v061' in store
resolver = spotify.split('suspend fun resolveArtistDestination',1)[1].split('private data class CandidateFingerprint',1)[0]
assert 'store.spotifyArtistLink(artist.name)' not in resolver
assert 'saveSpotifyArtistLink(artist.name' not in resolver
assert 'normalized.contains(target)' not in resolver
assert 'target.contains(normalized)' not in resolver
assert 'canonicalExactName(candidate.optString("name")) != target' in resolver
assert 'resolveSpotifyFromMusicBrainz' in resolver
assert 'artist.metadataConfidence >= 90' in resolver
assert 'MusicBrainz MBIDで本人確認' in spotify
assert '同名Artistを曲/Genreで一意に特定できない' in resolver
assert 'genreEvidenceScore' in spotify
assert 'verification: String = ""' in models
assert 'spotify.resolveArtistDestination(artist)' in vm
print('V061_SAFETY_COMPAT_OK')
