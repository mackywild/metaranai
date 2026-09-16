from pathlib import Path

root = Path(__file__).resolve().parents[1]
vm = (root / 'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
store = (root / 'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
ui = (root / 'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()
ios_state = (root / 'iosApp/MetaranaiIOS/MetaranaiAppState.swift').read_text()
ios_ui = (root / 'iosApp/MetaranaiIOS/ContentView.swift').read_text()
backup = (root / 'iosApp/MetaranaiIOS/Core/PortableBackup.swift').read_text()
build = (root / 'app/build.gradle.kts').read_text()
workflow = (root / '.github/workflows/android.yml').read_text()

assert 'versionCode = 19' in build
assert 'versionName = "0.9.1.1"' in build
assert 'metaranai-v0.9.1.1-apk' in workflow

# Android: DNA values/name are read-only and the name is regenerated every 5 learned changes.
assert 'private val dnaRegenerationInterval = 5' in vm
assert 'registerDnaLearningChange' in vm
assert 'dna_generated_name_v0911' in store
assert 'dna_learning_change_count_v0911' in store
assert 'fun saveManualDna' not in vm
assert 'Slider(' not in ui[ui.index('private fun DnaScreen'):ui.index('private fun SettingsScreen')]
assert 'この数値でDNAを生成' not in ui
assert 'ランダムDNAで遊ぶ' not in ui
assert '手動編集はできません' in ui
assert 'DNA名の次回自動生成まで' in ui

# iOS mirrors the same ownership/regeneration rule.
assert 'dnaRegenerationIntervalValue = 5' in ios_state
assert 'registerDNALearningChange' in ios_state
assert 'func saveManualDNA' not in ios_state
assert 'dna_generated_name_v0911' in backup
assert 'dna_learning_change_count_v0911' in backup
block = ios_ui[ios_ui.index('private struct DNAView'):ios_ui.index('private struct DNAProgress')]
assert 'Slider(' not in block
assert 'この数値でDNAを生成' not in block
assert 'ランダムDNAで遊ぶ' not in block
assert '手動編集はできません' in block

print('V0911_AUTO_DNA_OK')
