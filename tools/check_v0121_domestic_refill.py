from pathlib import Path
r = Path(__file__).resolve().parents[1]
b = r / 'app/src/main/java/jp/metaranai/app'
vm = (b / 'MainViewModel.kt').read_text()
ui = (b / 'MainActivity.kt').read_text()
client = (b / 'ExternalDiscoveryClient.kt').read_text()
store = (b / 'LocalStore.kt').read_text()
assert 'StateFlow<Recommendation?>' in vm
assert 'if (strict.isEmpty()) return null' in vm
refill = vm[vm.index('private fun refreshAfterReaction'):vm.index('private fun showReactionStatus')]
assert 'if (genres.isEmpty())' not in refill
assert 'ensureGenreLensPool(force = true)' in refill
assert 'requested.ifEmpty { listOf("Metal") }' in client
assert 'RefillPageWalker.collect' in client
assert 'freshNames' not in client
assert 'store.discoveryPage(tag, preference)' in client and 'store.saveDiscoveryPage(tag, preference, next)' in client
assert 'cancelRecommendationRefill()' in vm and 'generation != refillGeneration' in vm
assert 'private fun allArtists(): List<MetalArtist> = (_initialCatalog.value' in vm
assert 'store.saveInitialCatalog(_initialCatalog.value)' in vm
assert 'initial_catalog_v0121' in store
assert '新しい候補を再検索' in ui and 'rec == null' in ui
assert 'remember(external, history, initialCatalog)' in ui
print('V0121_DOMESTIC_REFILL_OK')
