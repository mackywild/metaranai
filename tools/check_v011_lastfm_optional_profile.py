from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
vm = (root / 'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
ui = (root / 'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()
store = (root / 'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
client = (root / 'app/src/main/java/jp/metaranai/app/LastFmProfileClient.kt').read_text()
build = (root / 'app/build.gradle.kts').read_text()

# Optional public-profile linkage. No Last.fm user session/password is required.
for marker in [
    '"method" to "user.getTopArtists"',
    '"method" to "user.getRecentTracks"',
    '"method" to "artist.getTopTags"',
    '"overall" to .35f',
    '"12month" to .28f',
    '"6month" to .22f',
    '"7day" to .15f',
]:
    assert marker in client, marker

for marker in [
    'lastfm_username_v011',
    'lastfm_profile_summary_v011',
    'lastfm_profile_seeds_v011',
]:
    assert marker in store, marker

assert 'fun syncLastFmProfile(username: String, completeOnboarding: Boolean = false, showSearchResults: Boolean = false)' in vm
assert 'val lastFm = _lastFmProfileSeeds.value' in vm
assert 'strong + lastFm + _spotifyTopArtists.value + partial + searched + profileSeeds' in vm
assert 'Last.fm連携（任意）' in ui
assert 'Last.fmの視聴履歴から始める' in ui
assert 'Last.fm未登録でも発掘機能は利用できます' in ui

# Existing Deep Dive is a protected regression boundary: one explicit seed,
# same discovery limit, same direct-source preference, same score weights.
m = re.search(r'fun deepDive\(artist: MetalArtist\) \{(.*?)\n    \}\n\n    fun clearDeepDive', vm, re.S)
assert m, 'deepDive body not found'
deep = m.group(1)
for marker in [
    'externalDiscovery.discover(listOf(artist.name), limitPerSeed = 18)',
    'candidate.sourceSeed?.equals(artist.name, true) == true',
    '_profile.value.similarity(candidate.vector) * .68f',
    'candidate.hiddenScore.coerceIn(0, 100) / 100f * .22f',
    'candidate.discovery * .10f',
    '.take(12)',
]:
    assert marker in deep, marker
assert 'lastFmProfile.sync' not in deep
assert '_lastFmProfileSeeds' not in deep

print('V011_LASTFM_OPTIONAL_PROFILE_OK')
