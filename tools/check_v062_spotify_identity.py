from pathlib import Path
root=Path(__file__).resolve().parents[1]
spotify=(root/'app/src/main/java/jp/metaranai/app/SpotifyClient.kt').read_text()
mb=(root/'app/src/main/java/jp/metaranai/app/MusicBrainzClient.kt').read_text()
store=(root/'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
vm=(root/'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
build=(root/'app/build.gradle.kts').read_text()
assert 'versionCode = 20' in build and 'versionName = "0.9.2"' in build
assert 'store.spotifyArtistLinkV064(artist)' in spotify and 'store.saveSpotifyArtistLinkV064' in spotify
assert 'resolveIdentityForSpotify(artist)' in spotify
for marker in ['countryMatch','areaMatch','beginExact','名前完全一致','国一致','開始年一致']: assert marker in mb, marker
assert 'MusicBrainzメタデータ照合' in spotify
assert 'exactMatches.size == 1 && strongIdentityReason != null' in spotify
assert 'exactMatches.size > 1' in spotify
assert 'spotifyLinkCached(artist: MetalArtist): Boolean = store.spotifyArtistLinkV064(artist) != null' in vm
print('V062_METADATA_IDENTITY_COMPAT_OK')
