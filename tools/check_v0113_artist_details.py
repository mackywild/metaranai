from pathlib import Path
r = Path(__file__).resolve().parents[1]
ui = (r / 'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()
vm = (r / 'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
client = (r / 'app/src/main/java/jp/metaranai/app/SpotifyClient.kt').read_text()
store = (r / 'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
card = ui[ui.index('private fun SearchArtistCard'):ui.index('private fun ArtistDetailsDialog')]
archive = ui[ui.index('private fun ArchiveScreen'):ui.index('private fun StatCard')]
details = ui[ui.index('private fun ArtistDetailsDialog'):ui.index('private fun ArchiveScreen')]
for block in (card, archive):
    assert 'SpotifyButton(' not in block and 'vm.openYouTube(' not in block
    assert 'onArtistDetails' in block
    assert 'onDeepDive(artist)' in block or 'vm.deepDive(artist)' in block
assert 'SpotifyButton(vm, artist,' in details
assert 'vm.openYouTube(artist)' in details
# Today retains its original direct-media flow, including its deep-dive panel.
home = ui[ui.index('private fun HomeScreen'):ui.index('private fun ReactionSelector')]
assert 'onArtistDetails' not in home and 'ArtistDetailsDialog' not in home
assert 'DeepDivePanel(vm, deepDiveStatus, deepDiveResults, deepDiving)' in home
panel = ui[ui.index('private fun DeepDivePanel'):ui.index('private fun ExternalMeta')]
assert 'if (onArtistDetails != null)' in panel
assert 'SpotifyButton(vm, artist,' in panel and 'vm.openYouTube(artist)' in panel
assert 'mediaOpenStatus.collectAsState()' not in ui and 'spotifyOpenStatus.collectAsState()' not in ui
assert 'vm.rateArtist(artist, reaction)' in details
assert 'selected = record?.reaction' in details
assert 'fun rateArtist(artist: MetalArtist, reaction: Reaction): Boolean' in vm
assert 'ArtistRatingHistory.record(_history.value, record)' in vm
assert 'store.saveExternalArtists(_externalArtists.value)' in vm
ranking = vm[vm.index('fun selectSpotifyTopPeriod'):vm.index('fun shuffle')]
assert 'registerDnaLearningChange' not in ranking and 'updatedProfile' not in ranking
assert 'spotify.fetchTopArtists(period)' in ranking
fetch = client[client.index('suspend fun fetchTopArtists'):client.index('V0.6.4 Spotify Identity Resolver')]
assert 'time_range=${period.apiValue}' in fetch
assert 'authorize(' not in fetch
assert 'spotify_top_snapshot_v0113_${period.apiValue}' in store
assert 'period == SpotifyTopPeriod.HALF_YEAR' in store and 'loadSpotifyTopArtists()' in store
assert 'Spotifyを連携すると' in ui and 'Last.fmを連携すると' in ui
print('V0113_ARTIST_DETAILS_OK')
