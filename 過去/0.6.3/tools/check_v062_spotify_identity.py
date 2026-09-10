from pathlib import Path
root=Path(__file__).resolve().parents[1]
spotify=(root/'app/src/main/java/jp/metaranai/app/SpotifyClient.kt').read_text()
mb=(root/'app/src/main/java/jp/metaranai/app/MusicBrainzClient.kt').read_text()
store=(root/'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
vm=(root/'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
build=(root/'app/build.gradle.kts').read_text()
workflow=(root/'.github/workflows/android.yml').read_text()

assert 'versionCode = 13' in build
assert 'versionName = "0.6.3"' in build
assert 'metaranai-v0.6.3-apk' in workflow
assert 'store.spotifyArtistLinkV063(artist)' in spotify
assert 'store.saveSpotifyArtistLinkV063' in spotify
assert 'spotify_artist_links_v063' in store
assert 'store.spotifyArtistLinkV062(artist)' not in spotify
assert 'store.spotifyArtistLink(artist.name)' not in spotify
assert 'normalized.contains(target)' not in spotify
assert 'target.contains(normalized)' not in spotify
assert 'resolveIdentityForSpotify(artist)' in spotify
for marker in ['countryMatch', 'areaMatch', 'beginExact', '名前完全一致', '国一致', '開始年一致']:
    assert marker in mb, marker
assert 'spotifyArtistRelation' in mb
assert 'MusicBrainzメタデータ照合' in spotify
assert 'exactMatches.size == 1 && strongIdentityReason != null' in spotify
assert 'exactMatches.size > 1' in spotify
assert '同名Artistを曲/Genreで一意に特定できない' in spotify
assert 'spotify_artist_links_v05' in store and 'spotify_artist_links_v061' in store and 'spotify_artist_links_v062' in store
assert 'spotifyLinkCached(artist: MetalArtist): Boolean = store.spotifyArtistLinkV063(artist) != null' in vm
assert 'artist.metadataConfidence >= 90' in store
print('V062_METADATA_IDENTITY_COMPAT_OK')
