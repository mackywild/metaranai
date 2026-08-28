from pathlib import Path
root=Path(__file__).resolve().parents[1]
genre=(root/'app/src/main/java/jp/metaranai/app/GenreLens.kt').read_text()
engine=(root/'app/src/main/java/jp/metaranai/app/RecommendationEngine.kt').read_text()
vm=(root/'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
ext=(root/'app/src/main/java/jp/metaranai/app/ExternalDiscoveryClient.kt').read_text()
ui=(root/'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()
assert 'fun matches(artist: MetalArtist' in genre
assert 'val eligible = if (lensActive) GenreLensCatalog.filter' in engine
assert 'ensureGenrePool' in ext
assert 'minimumUnratedLensPoolPerGenre = 10' in vm
assert 'Genre Lens未評価候補不足' in vm
assert 'GENRE LENS DIGGING' in ui
assert '指定ジャンル以外は出さない' in ui
print('V052_STRICT_GENRE_LENS_OK')
