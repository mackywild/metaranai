from pathlib import Path

root = Path(__file__).resolve().parents[1]
ui = (root / 'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()
vm = (root / 'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
store = (root / 'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()

archive = ui[ui.index('private fun ArchiveScreen'):ui.index('private fun archiveStarRating')]

# Archive search follows the same strict AND matcher as the Search screen.
assert 'SearchQueryMatcher.matches(artist, query)' in archive
assert '半角スペース区切りはAND検索' in archive

# Discovery / novelty use a 5-star visual scale instead of numeric values.
assert 'archiveStarRating(artist.hiddenScore)' in archive
assert 'archiveStarRating((artist.discovery * 100).toInt())' in archive
assert '発掘度 ${artist.hiddenScore}' not in archive
assert '新規性 ${(artist.discovery * 100).toInt()}%' not in archive
assert '"★".repeat(filled) + "☆".repeat(5 - filled)' in ui

# Evaluation metadata: timestamp is visible, historical DNA similarity remains internal only.
assert '評価日時 ${formatEvaluationDateTime(record.date)}' in archive
assert '最終評価' not in archive
assert '当時DNA一致度' not in archive
assert 'record.score' not in archive
assert 'LocalDateTime.now().withNano(0).toString()' in vm
assert 'it.date.startsWith(today)' in vm
assert 'LocalDateTime.parse(row.date).toLocalDate()' in vm
assert 'put("score",r.score)' in store

# Spotify verification chatter is hidden from Archive cards.
assert 'Spotify本人確認済みリンク取得済み' not in archive
assert 'spotifyLinkCached(artist)' not in archive

# YouTube button uses the branded red/white treatment and stays on one line.
assert 'containerColor = Color(0xFFFF0000)' in archive
assert 'Text("YouTube", color = Color.White' in archive
assert 'maxLines = 1' in archive

print('V0101_ARCHIVE_UI_OK')
