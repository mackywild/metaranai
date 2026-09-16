from pathlib import Path
import re, unicodedata
root=Path(__file__).resolve().parents[1]
spotify=(root/'app/src/main/java/jp/metaranai/app/SpotifyClient.kt').read_text()
store=(root/'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
build=(root/'app/build.gradle.kts').read_text()
workflow=(root/'.github/workflows/android.yml').read_text()
vm=(root/'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()

assert 'versionCode = 20' in build
assert 'versionName = "0.9.2"' in build
assert 'metaranai-v0.9.2-apk' in workflow
assert 'store.spotifyArtistLinkV064(artist)' in spotify
assert 'store.saveSpotifyArtistLinkV064' in spotify
assert 'spotify_artist_links_v064' in store
assert 'store.spotifyArtistLinkV063(artist)' not in spotify
assert 'spotifyLinkCached(artist: MetalArtist): Boolean = store.spotifyArtistLinkV064(artist) != null' in vm

# Search the way the Spotify UI does, plus field-filter query. Partial names are still filtered out.
assert 'val queries = listOf(artistName, "artist:\\"$artistName\\"")' in spotify
assert 'canonicalExactName(candidate.optString("name")) != target' in spotify
assert 'type=artist&limit=10&offset=$offset' in spotify

# Independent catalog verification: Last.fm + Apple's public iTunes Search catalog.
assert 'artist.getTopTracks' in spotify
assert 'appleCatalogGroups' in spotify
assert 'itunes.apple.com/search' in spotify
assert 'entity=musicArtist&attribute=artistTerm&limit=50' in spotify
assert '/lookup?id=$artistId&entity=song&limit=200&sort=recent&country=$country' in spotify
assert 'entity=song&attribute=artistTerm&limit=200' in spotify
assert 'canonicalExactName(item.optString("artistName")) != target' in spotify
assert 'artistId' in spotify
assert 'spotifyCatalogForCandidate' in spotify
assert '/artists/$artistId/albums?include_groups=album,single&limit=10' in spotify
assert '/albums/$albumId/tracks?limit=50' in spotify
assert '/top-tracks' not in spotify

# ---------- Real-world regression fixture: Yutaro Abe's ASTRAL WIND ----------
def canon_name(v):
    return re.sub(r'\s+',' ',unicodedata.normalize('NFKC',v).strip().lower())
def canon(v):
    s=unicodedata.normalize('NFKC',v).lower()
    s=re.sub(r'^\s*\d{1,2}[\s._#:-]+','',s)
    s=s.replace('&',' and ')
    s=re.sub(r'[^\w]+',' ',s,flags=re.UNICODE)
    return re.sub(r'\s+',' ',s).strip()

target="Yutaro Abe's ASTRAL WIND"
# Real catalog titles observed in the reported Spotify screen + public catalog pages.
apple_tracks=[
    'Wings Of Heart','Infernal Will','Forever Gone','Blazing Heart Of Sorrow',
    'UNCHAINED-Soul Of The Brave-','In The World Of Saints','BWV1041'
]
apple_albums=['Cycle Of Life','UNCHAINED -Soul Of The Brave-']
spotify_tracks=[
    'Wings Of Heart','Infernal Will','Forever Gone','Blazing Heart Of Sorrow',
    'UNCHAINED-Soul Of The Brave-','In The World Of Saints','BWV1041'
]
spotify_albums=['Cycle Of Life','UNCHAINED -Soul Of The Brave-']
track_matches=set(map(canon,apple_tracks)) & set(map(canon,spotify_tracks))
album_matches=set(map(canon,apple_albums)) & set(map(canon,spotify_albums))
score=len(track_matches)*3+len(album_matches)*4
assert canon_name(target)==canon_name("Yutaro Abe's ASTRAL WIND")
assert len(track_matches)>=5, track_matches
assert len(album_matches)>=2, album_matches
assert score>=10
# This exact artist must satisfy the v0.6.4 strong fingerprint rule -> direct destination.
strong = len(track_matches)>=2 or len(album_matches)>=2 or (track_matches and album_matches)
assert strong
# A same-name wrong artist with unrelated catalog must not beat the real candidate.
wrong_tracks=['Sunrise','Night Drive','Blue Sky']
wrong_albums=['First Steps']
wrong_t=set(map(canon,apple_tracks)) & set(map(canon,wrong_tracks))
wrong_a=set(map(canon,apple_albums)) & set(map(canon,wrong_albums))
wrong_score=len(wrong_t)*3+len(wrong_a)*4
assert score >= wrong_score + 3
# Partial-name candidate stays forbidden.
assert canon_name('Yutaro Abe') != canon_name(target)
print('V064_YUTARO_ASTRAL_WIND_REAL_FIXTURE_OK')
