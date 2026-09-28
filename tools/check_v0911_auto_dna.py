from pathlib import Path

root = Path(__file__).resolve().parents[1]
vm = (root / 'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
store = (root / 'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
ui = (root / 'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()
generator = (root / 'app/src/main/java/jp/metaranai/app/DnaNameGenerator.kt').read_text()
ios_state = (root / 'iosApp/MetaranaiIOS/MetaranaiAppState.swift').read_text()
ios_generator = (root / 'iosApp/MetaranaiIOS/Core/DNANameGenerator.swift').read_text()
ios_ui = (root / 'iosApp/MetaranaiIOS/ContentView.swift').read_text()
backup = (root / 'iosApp/MetaranaiIOS/Core/PortableBackup.swift').read_text()

# Android: DNA values/name stay read-only and naming is regenerated every 5 learned changes.
assert 'const val REGENERATION_INTERVAL = 5' in generator
assert 'DnaNamePolicy.REGENERATION_INTERVAL' in vm
assert 'DnaNamePolicy.shouldRegenerate(next)' in vm
assert 'DnaNameGenerator.generate' in (root / 'app/src/main/java/jp/metaranai/app/RecommendationEngine.kt').read_text()
assert 'const val VERSION = 2' in generator
assert 'dna_generated_name_v0911' in store
assert 'dna_learning_change_count_v0911' in store
assert 'dna_name_generator_version_v095' in store
assert 'saveDnaNameGeneratorVersion(DnaNameGenerator.VERSION)' in vm

# The generator must infer from ranked parameters rather than select from the old fixed threshold list.
for phrase in ['旋律疾走', '極重咆哮', '劇場清唱', '高速技巧', '清唱歌心']:
    assert phrase in generator
assert 'melodic > .88f' not in generator
assert '探索型オールラウンドメタラー' not in generator

assert 'fun saveManualDna' not in vm
block = ui[ui.index('private fun DnaScreen'):ui.index('private fun SettingsScreen')]
assert 'Slider(' not in block
assert 'この数値でDNAを生成' not in block
assert 'ランダムDNAで遊ぶ' not in block
assert 'あなたのメタルDNA' in block
assert '手動編集はできません' not in block
assert 'DNA名の次回自動生成まで' not in ui

# iOS mirrors the same inference/timing/migration rule.
assert 'static let regenerationInterval = 5' in ios_generator
assert 'DNANamePolicy.regenerationInterval' in ios_state
assert 'DNANamePolicy.shouldRegenerate(nextChangeCount: next)' in ios_state
assert 'static let version = 2' in ios_generator
assert 'dna_name_generator_version_v095' in ios_state
assert 'dna_name_generator_version_v095' in backup
for phrase in ['旋律疾走', '極重咆哮', '劇場清唱', '高速技巧', '清唱歌心']:
    assert phrase in ios_generator

ios_block = ios_ui[ios_ui.index('private struct DNAView'):ios_ui.index('private struct DNAProgress')]
assert 'Slider(' not in ios_block
assert 'この数値でDNAを生成' not in ios_block
assert 'ランダムDNAで遊ぶ' not in ios_block
assert 'あなたのメタルDNA' in ios_block
assert '手動編集はできません' not in ios_block
assert 'DNA名の次回自動生成まで' not in ios_block

print('V0911_AUTO_DNA_OK')
