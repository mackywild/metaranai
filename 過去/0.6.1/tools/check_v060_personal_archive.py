from pathlib import Path
root=Path(__file__).resolve().parents[1]
ui=(root/'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()
vm=(root/'app/src/main/java/jp/metaranai/app/MainViewModel.kt').read_text()
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
build=(root/'app/build.gradle.kts').read_text()
store=(root/'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
ext=(root/'app/src/main/java/jp/metaranai/app/ExternalDiscoveryClient.kt').read_text()
workflow=(root/'.github/workflows/android.yml').read_text()

assert 'versionCode = 11' in build
assert 'versionName = "0.6.1"' in build
assert 'val tabs = listOf("今日", "探す", "図鑑", "DNA", "設定")' in ui
assert 'private fun ArchiveScreen' in ui
for marker in ['PERSONAL METAL ARCHIVE','未評価','GenreLensCatalog.names()','VocalType.FEMALE','Spotify本人確認済みリンク取得済み']:
    assert marker in ui, marker
assert 'WHY THIS ARTIST?' in ui
assert 'fun whyThisArtist' in vm
assert 'fun deepDive(artist: MetalArtist)' in vm
assert 'externalDiscovery.discover(listOf(artist.name), limitPerSeed = 18)' in vm
assert 'fun openYouTube' in vm and 'official music video' in vm and ' live' in vm
assert 'android:icon="@mipmap/ic_launcher"' in manifest
assert 'android:roundIcon="@mipmap/ic_launcher_round"' in manifest
for density in ['mdpi','hdpi','xhdpi','xxhdpi','xxxhdpi']:
    assert (root/f'app/src/main/res/mipmap-{density}/ic_launcher.png').exists(), density
assert 'out.put("version", 61)' in store
assert 'Metaranai-Android/0.6.1' in ext
assert 'metaranai-v0.6.1-apk' in workflow
print('V060_PERSONAL_METAL_ARCHIVE_OK')
