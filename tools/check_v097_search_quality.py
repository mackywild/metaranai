from pathlib import Path

root = Path(__file__).resolve().parents[1]
matcher = (root / 'app/src/main/java/jp/metaranai/app/SearchQueryMatcher.kt').read_text()
vm = (root / 'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
ext = (root / 'app/src/main/java/jp/metaranai/app/ExternalDiscoveryClient.kt').read_text()
ui = (root / 'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()
models = (root / 'app/src/main/java/jp/metaranai/app/Models.kt').read_text()

# Query semantics: split on whitespace and require all terms across band/country/genre.
assert r'.split(Regex("\\s+"))' in matcher
assert 'terms.all' in matcher
assert 'SearchQueryMatcher.matches(it, q)' in vm
assert 'SearchQueryMatcher.exactName(it, q)' in vm

# Last.fm fuzzy neighbors are suggestions, not normal results or cache inserts.
assert 'val suggestions = enriched.filterNot { SearchQueryMatcher.matches(it, q) }' in ext
assert 'val combined = mergeCache(strict)' in ext
assert 'val suggestions: List<MetalArtist>' in models
assert '_remoteSearchSuggestions' in vm
assert '"もしかして…"' in ui

# Search result cards are clean: no discovery/hidden/cache labels.
search_block = ui[ui.index('private fun SearchScreen'):ui.index('private fun ArchiveScreen')]
assert '発掘度' not in search_block
assert 'HIDDEN' not in search_block
assert 'ローカル図鑑へ保存済み' not in search_block
assert '外部' not in search_block or '外部検索失敗' not in search_block

# YouTube result action is red/white and forced to one line.
assert 'containerColor = Color(0xFFFF0000)' in search_block
assert 'Text("YouTube", color = Color.White' in search_block
assert 'maxLines = 1' in search_block
assert '半角スペース区切りはAND検索' in search_block

# Success status no longer reports DB/fetched counts.
assert 'Local DB' not in vm[vm.index('fun searchExternal'):vm.index('fun clearRemoteSearch')]
assert 'Metal判定' not in vm[vm.index('fun searchExternal'):vm.index('fun clearRemoteSearch')]

print('V097_SEARCH_QUALITY_OK')
