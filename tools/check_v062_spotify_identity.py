from pathlib import Path
root=Path(__file__).resolve().parents[1]
spotify=(root/'app/src/main/java/jp/metaranai/app/SpotifyClient.kt').read_text()
mb=(root/'app/src/main/java/jp/metaranai/app/MusicBrainzClient.kt').read_text()
store=(root/'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
vm=(root/'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
build=(root/'app/build.gradle.kts').read_text()
workflow=(root/'.github/workflows/android.yml').read_text()

assert 'versionCode = 12' in build
assert 'versionName = "0.6.2"' in build
assert 'metaranai-v0.6.2-apk' in workflow
assert 'store.spotifyArtistLinkV062(artist)' in spotify
assert 'store.saveSpotifyArtistLinkV062' in spotify
assert 'spotify_artist_links_v062' in store
assert 'store.spotifyArtistLinkV061(artist)' not in spotify
assert 'store.spotifyArtistLink(artist.name)' not in spotify
assert 'canonicalExactName(candidate.optString("name")) == target' in spotify
assert 'normalized.contains(target)' not in spotify
assert 'target.contains(normalized)' not in spotify
assert 'limit=20' in spotify

# New recovery path: Local DB metadata -> MusicBrainz exact identity -> Spotify.
assert 'resolveIdentityForSpotify(artist)' in spotify
for marker in ['countryMatch', 'areaMatch', 'beginExact', '名前完全一致', '国一致', '開始年一致']:
    assert marker in mb, marker
assert 'spotifyArtistRelation' in mb
assert 'MusicBrainzメタデータ照合' in spotify

# Unique exact match may now open when identity or metal genre evidence is present.
assert 'exactMatches.size == 1 && strongIdentityReason != null' in spotify
assert 'exactMatches.size == 1 && scored.single().second >= 2' in spotify
assert 'exactMatches.size == 1 && scored.single().second == 1' in spotify
# Same-name ambiguity remains guarded.
assert 'exactMatches.size > 1 && scored.count { it.second >= 2 } == 1' in spotify
assert '同名Artistを一意に特定できない' in spotify
# Completely unverified exact match still falls back safely.
assert '名前は完全一致だが本人確認材料不足' in spotify
# Old caches remain but are not auto-read by v0.6.2.
assert 'spotify_artist_links_v05' in store and 'spotify_artist_links_v061' in store
assert 'spotifyLinkCached(artist: MetalArtist): Boolean = store.spotifyArtistLinkV062(artist) != null' in vm

# Cache MBID must only be used as identity key at high metadata confidence.
assert 'artist.metadataConfidence >= 90' in store

# Small behavioral model for the intended policy.
def decision(exact_count, strong_identity=False, evidence_scores=()):
    if exact_count == 0:
        return 'search'
    if exact_count == 1 and strong_identity:
        return 'direct'
    if exact_count == 1 and evidence_scores and evidence_scores[0] >= 1:
        return 'direct'
    if exact_count > 1 and sum(s >= 2 for s in evidence_scores) == 1:
        return 'direct'
    return 'search'

assert decision(1, strong_identity=True, evidence_scores=(0,)) == 'direct'
assert decision(1, strong_identity=False, evidence_scores=(1,)) == 'direct'
assert decision(1, strong_identity=False, evidence_scores=(0,)) == 'search'
assert decision(2, strong_identity=False, evidence_scores=(0,0)) == 'search'
assert decision(2, strong_identity=False, evidence_scores=(3,0)) == 'direct'
print('V062_SPOTIFY_IDENTITY_OK')
