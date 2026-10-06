from pathlib import Path

root = Path(__file__).resolve().parents[1]
ui = (root / 'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()

settings = ui[ui.index('private enum class SettingsPanel'):ui.index('private fun GenreSelector')]

# Settings are now a compact menu. Detailed controls live in dialogs.
assert 'private enum class SettingsPanel' in settings
assert 'private fun SettingsMenuItem' in settings
assert 'private fun SettingsDialogShell' in settings
assert 'AlertDialog(' in settings

for title in [
    'アカウント・同期',
    'ジャンル固定',
    'Spotify連携',
    '図鑑データ',
    'バックアップ',
    'プライバシーポリシー',
    'アカウント削除',
]:
    assert f'title = "{title}"' in settings, title

# API configuration is a debug-only menu and dialog; discovery stays in Search.
debug_menu = settings[settings.index('        if (BuildConfig.DEBUG) {'):settings.index('title = "図鑑データ"')]
assert 'title = "外部検索・発掘（開発用）"' in debug_menu
assert 'SettingsPanel.DISCOVERY -> if (BuildConfig.DEBUG) SettingsDialogShell(' in settings
assert 'このバージョンではLast.fm連携を利用できません。' in settings
assert 'title = "外部検索・発掘",' not in settings

# Genre Lens is presented to users as a genre-fixing feature.
assert 'title = "ジャンル固定"' in settings
assert '"曜日固定"' in settings
assert '"手動固定"' in settings

# Privacy policy is directly reachable from the app.
assert 'PRIVACY_POLICY_URL = "https://mackywild.github.io/metaranai/privacy-policy.html"' in settings
assert 'Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))' in settings

# Account deletion is separated from the ordinary account dialog and kept at the bottom.
assert settings.index('title = "プライバシーポリシー"') < settings.index('title = "アカウント削除"')
account_content = ui[ui.index('private fun AccountSettingsContent'):ui.index('private fun formatCompact')]
assert 'vm::deleteAccount' not in account_content
assert 'showDeleteConfirm' in settings
assert 'vm.deleteAccount()' in settings
assert '端末内のLocal DBやJSONデータは残ります' in settings

# Old fully-expanded settings cards are no longer used by SettingsScreen.
screen = ui[ui.index('private fun SettingsScreen'):ui.index('private fun SettingsMenuItem')]
assert 'SettingsCard(' not in screen
assert 'AccountSettingsCard(' not in screen
# Backup export remains present; release-specific filename is checked by check_release_metadata.py.
assert 'CreateDocument("application/json")' in screen
assert '分析データをバックアップ' in screen

print('V098_SETTINGS_DIALOGS_OK')
