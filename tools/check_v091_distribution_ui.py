from pathlib import Path
root=Path(__file__).resolve().parents[1]
ui=(root/'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()
models=(root/'app/src/main/java/jp/metaranai/app/Models.kt').read_text()
vm=(root/'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
genre=(root/'app/src/main/java/jp/metaranai/app/GenreLens.kt').read_text()
ios=(root/'iosApp/MetaranaiIOS/ContentView.swift').read_text()
ios_models=(root/'iosApp/MetaranaiIOS/Core/CoreModels.swift').read_text()
assert 'メタルバンド探索アプリケーション' in ui
assert 'v0.9.1 ·' not in ui
assert '本日のジャンル:' in ui
assert '今日のメタル' in ui
home = ui[ui.index('private fun HomeScreen'):ui.index('private fun ReactionSelector')]
assert 'DNA一致度' not in home
assert '🌐 外部発掘' not in home
assert 'vm.openYouTube(rec.artist, "mv")' not in home
assert 'vm.openYouTube(rec.artist, "live")' not in home
assert 'Text("MV")' not in home
assert 'Text("ライブ")' not in home
details = ui[ui.index('private fun ArtistDetailsDialog'):ui.index('private fun ArchiveScreen')]
assert 'Color(0xFFFF0000)' in details
assert 'Text("YouTube", color = Color.White' in details
assert 'spotifyOpenStatus.collectAsState()' not in ui
assert 'mediaOpenStatus.collectAsState()' not in ui
assert 'SpotifyButton(vm, rec.artist' in home
assert 'vm.openYouTube(rec.artist)' in home
assert 'ReactionSelector(onReaction = vm::react)' in home
assert 'onArtistDetails' not in home
assert '詳細・試聴・評価' not in home
assert 'Modifier.clickable' not in home
assert '0 -> HomeScreen(vm)' in ui
assert 'WHY THIS ARTIST?' not in ui
assert 'vocalFilter' not in ui
assert 'NOT_FOUND("🔍 見つからなかった"' in models
assert 'reaction != Reaction.NOT_FOUND' in vm
assert 'private fun SpotifyButton' in ui and 'enabled = available == true' in ui
assert 'fun saveManualDna' not in vm
assert 'この数値でDNAを生成' not in ui and 'ランダムDNAで遊ぶ' not in ui
assert 'あなたのメタルDNA' in ui and 'LinearProgressIndicator' in ui
assert '手動編集はできません' not in ui[ui.index('private fun DnaScreen'):ui.index('private fun SettingsScreen')]
assert 'DNA名の次回自動生成まで' not in ui
assert 'fun displayName' in genre
assert 'メタルバンド探索アプリケーション' in ios
assert 'case notFound = "NOT_FOUND"' in ios_models
assert 'VOCAL DNA' not in ios
print('V091_DISTRIBUTION_UI_OK')
