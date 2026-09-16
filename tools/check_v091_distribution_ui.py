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
assert 'WHY THIS ARTIST?' not in ui
assert 'vocalFilter' not in ui
assert 'NOT_FOUND("🔍 見つからなかった"' in models
assert 'reaction != Reaction.NOT_FOUND' in vm
assert 'private fun SpotifyButton' in ui and 'enabled = available == true' in ui
assert 'fun saveManualDna' in vm and 'この数値でDNAを生成' in ui and 'ランダムDNAで遊ぶ' in ui
assert 'fun displayName' in genre
assert 'メタルバンド探索アプリケーション' in ios
assert 'case notFound = "NOT_FOUND"' in ios_models
assert 'VOCAL DNA' not in ios
print('V091_DISTRIBUTION_UI_OK')
