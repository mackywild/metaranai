from pathlib import Path
root=Path(__file__).resolve().parents[1]
spotify=(root/'app/src/main/java/jp/metaranai/app/SpotifyClient.kt').read_text()
store=(root/'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
vm=(root/'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
models=(root/'app/src/main/java/jp/metaranai/app/Models.kt').read_text()
build=(root/'app/build.gradle.kts').read_text()

assert 'versionCode = 11' in build
assert 'versionName = "0.6.1"' in build
assert 'suspend fun resolveArtistDestination(artist: MetalArtist)' in spotify
assert 'store.spotifyArtistLinkV061(artist)' in spotify
assert 'spotify_artist_links_v061' in store
assert 'spotify_artist_links_v05' in store  # legacy preserved
# Critical regression: old name-only cache must not be read by resolver.
resolver = spotify.split('suspend fun resolveArtistDestination',1)[1].split('private suspend fun authorize',1)[0]
assert 'store.spotifyArtistLink(artist.name)' not in resolver
assert 'saveSpotifyArtistLink(artist.name' not in resolver
# Partial names must never be direct candidates.
assert 'normalized.contains(target)' not in resolver
assert 'target.contains(normalized)' not in resolver
assert 'canonicalExactName(candidate.optString("name")) == target' in resolver
# Strong identity path and conservative ambiguity handling.
assert 'resolveSpotifyFromMusicBrainz' in resolver
assert 'artist.metadataConfidence >= 90' in resolver
assert 'inc=url-rels' in spotify
assert 'MusicBrainz MBIDで本人確認' in spotify
assert '同名Artistを一意に特定できない' in resolver
assert 'ジャンル本人確認不足' in resolver
assert 'candidatesWithEvidence.size == 1' in resolver
assert 'genreEvidenceScore' in spotify
assert 'verification: String = ""' in models
assert 'spotify.resolveArtistDestination(artist)' in vm
print('V061_SPOTIFY_IDENTITY_OK')

ext=(root/'app/src/main/java/jp/metaranai/app/ExternalDiscoveryClient.kt').read_text()
assert 'info.mbid != null -> 90' in ext
assert 'mbid = info.mbid ?: mb?.mbid' in ext
