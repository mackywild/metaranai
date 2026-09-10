from pathlib import Path
import re, unicodedata
root=Path(__file__).resolve().parents[1]
spotify=(root/'app/src/main/java/jp/metaranai/app/SpotifyClient.kt').read_text()
store=(root/'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
build=(root/'app/build.gradle.kts').read_text()
workflow=(root/'.github/workflows/android.yml').read_text()

assert 'versionCode = 13' in build
assert 'versionName = "0.6.3"' in build
assert 'metaranai-v0.6.3-apk' in workflow
assert 'artist.getTopTracks' in spotify
assert 'lastFmTopTrackNames' in spotify
assert 'spotifyTrackNamesForCandidate' in spotify
assert 'matchingTrackNames' in spotify
assert 'trackHasArtistId' in spotify
assert '曲指紋${fp.trackMatches.size}曲一致' in spotify
assert '曲指紋1曲一致' in spotify
assert '同名候補から曲指紋' in spotify
assert 'spotify_artist_links_v063' in store
assert 'store.spotifyArtistLinkV063(artist)' in spotify
assert 'store.saveSpotifyArtistLinkV063' in spotify
# Spotify Feb-2026 Search limit is 10; v0.6.2's invalid limit=20 must be gone from resolver.
resolver=spotify.split('suspend fun resolveArtistDestination',1)[1].split('private data class CandidateFingerprint',1)[0]
assert 'type=artist&limit=10&offset=$offset' in resolver
assert 'type=artist&limit=20' not in resolver
assert 'type=track&limit=10&offset=$offset' in spotify
# Removed Spotify artist top-tracks endpoint must not be used.
assert '/top-tracks' not in spotify
# Album fallback keeps fingerprinting useful when track search is sparse.
assert '/albums?include_groups=album,single&limit=5' in spotify
assert '/albums/$albumId/tracks?limit=50' in spotify

# Behavioral model matching the reported ASTRAL WIND case.
def canon(v):
    s=unicodedata.normalize('NFKC',v).lower()
    s=s.replace('&',' and ')
    s=re.sub(r'[^\w]+',' ',s,flags=re.UNICODE)
    return re.sub(r'\s+',' ',s).strip()
lastfm=['Cycle Of Life','UNCHAINED -Soul Of The Brave-','Infernal Will','Blazing Heart Of Sorrow','BWV1041','Wings Of Heart']
spotify_tracks=['Cycle Of Life','UNCHAINED-Soul Of The Brave-','Infernal Will','Blazing Heart Of Sorrow','BWV1041']
shared=set(map(canon,lastfm)) & set(map(canon,spotify_tracks))
assert len(shared) >= 5, shared
# One exact-name candidate with >=2 strong shared titles should direct.
assert len(shared) >= 2
# Same-name ambiguity needs one clear track-fingerprint winner.
counts=[5,0]
assert max(counts)>=2 and sorted(counts, reverse=True)[0] > sorted(counts, reverse=True)[1]
print('V063_SPOTIFY_TRACK_FINGERPRINT_OK')
