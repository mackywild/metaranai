from pathlib import Path
root=Path(__file__).resolve().parents[1]
spotify=(root/'app/src/main/java/jp/metaranai/app/SpotifyClient.kt').read_text()
build=(root/'app/build.gradle.kts').read_text()
assert 'versionCode = 19' in build and 'versionName = "0.9.1.1"' in build
assert 'artist.getTopTracks' in spotify
assert 'lastFmTopTrackNames' in spotify
assert 'spotifyCatalogForCandidate' in spotify
assert 'matchingNames' in spotify
assert 'trackHasArtistId' in spotify
assert 'type=track&limit=10&offset=$offset' in spotify
assert '/top-tracks' not in spotify
assert '/albums/$albumId/tracks?limit=50' in spotify
print('V063_TRACK_FINGERPRINT_COMPAT_OK')
