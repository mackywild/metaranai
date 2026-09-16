from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
main = (root / "app/src/main/java/jp/metaranai/app/MainActivity.kt").read_text()
viewmodel = (root / "app/src/main/java/jp/metaranai/app/MainViewModel.kt").read_text()
gradle = (root / "app/build.gradle.kts").read_text()
assert 'versionCode = 17' in gradle
assert 'versionName = "0.9.0"' in gradle
assert 'v0.9.0 · ACCOUNT & PERSONALIZATION' in main
assert 'なぜこのArtist？ / スコアを見る' in main
assert 'private fun ReactionSelector' in main
assert 'sortMode' in main and 'おすすめ順' in main
assert 'archiveDatabaseCount()' in viewmodel

for rel in [
    'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml',
    'app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml',
    'app/src/main/res/drawable/ic_launcher_monochrome.xml',
]:
    ET.parse(root / rel)

required_docs = [
    'docs/03_SPOTIFY_SETUP.md', 'docs/05_ANDROID_SIGNING.md', 'docs/06_IOS_SIGNING.md',
    'docs/07_GOOGLE_PLAY_RELEASE.md', 'docs/08_APP_STORE_RELEASE.md', 'docs/10_BACKUP_RESTORE.md',
    'docs/11_PRIVACY_CHECKLIST.md', 'docs/THIRD_PARTY_NOTICES.md', 'iosApp/README.md',
    'iosApp/MetaranaiIOS/LegacyBackupImporter.swift'
]
for rel in required_docs:
    assert (root / rel).exists(), rel
print("V070_PROJECT_REFORGE_OK")
