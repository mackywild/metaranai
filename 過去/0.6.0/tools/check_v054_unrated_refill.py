from pathlib import Path
root = Path(__file__).resolve().parents[1]
vm = (root/'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
ext = (root/'app/src/main/java/jp/metaranai/app/ExternalDiscoveryClient.kt').read_text()
ui = (root/'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()
build = (root/'app/build.gradle.kts').read_text()

assert 'minimumUnratedLensPoolPerGenre = 10' in vm
assert 'refillTargetUnratedLensPoolPerGenre = 20' in vm
assert 'private fun ratedArtistNames()' in vm
assert 'private fun lensUnratedCandidates' in vm
assert 'private fun lensUnratedCounts' in vm
assert 'val strict = lensUnratedCandidates(genres)' in vm
assert 'excludedArtistNames = ratedArtistNames()' in vm
assert 'minimumPerGenre = refillTargetUnratedLensPoolPerGenre' in vm
assert '未評価${counts[it] ?: 0}組・総数${totals[it] ?: 0}組' in vm
assert 'showReactionStatus("${rec.artist.name} は今日すでに評価済み。未評価候補を探します")' in vm
assert 'showReactionStatus("${reaction.label} を記録しました")' in vm
assert 'excludedArtistNames: Set<String> = emptySet()' in ext
assert 'archive.filterNot { it.name.trim().lowercase() in excluded }' in ext
assert 'normalized in knownNames || normalized in excluded' in ext
assert 'for (page in 1..3)' in ext
assert '"page" to page.coerceAtLeast(1).toString()' in ext
assert 'reactionStatus by vm.reactionStatus.collectAsState()' in ui
assert 'if (reactionStatus.isNotBlank())' in ui
assert 'versionCode = 10' in build
assert 'versionName = "0.6.0"' in build

# Behavioral model: total pool can be healthy while unrated pool requires refill.
total_black = {f'band{i}' for i in range(30)}
rated = {f'band{i}' for i in range(23)}
unrated = total_black - rated
assert len(total_black) == 30
assert len(unrated) == 7
assert len(unrated) < 10
refill_need = 20 - len(unrated)
assert refill_need == 13
print('V054_UNRATED_REFILL_OK')
