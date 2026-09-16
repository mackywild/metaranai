from pathlib import Path
import plistlib, json, re

root = Path(__file__).resolve().parents[1]
android = (root/'app/build.gradle.kts').read_text()
main = (root/'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()
store = (root/'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
assert 'versionCode = 20' in android
assert 'versionName = "0.9.2"' in android
assert 'メタルバンド探索アプリケーション' in main
assert 'out.put("version", 90)' in store
assert 'require(root.optInt("version")' not in store

required = [
    'iosApp/project.yml',
    'iosApp/MetaranaiIOS/MetaranaiIOSApp.swift',
    'iosApp/MetaranaiIOS/ContentView.swift',
    'iosApp/MetaranaiIOS/MetaranaiAppState.swift',
    'iosApp/MetaranaiIOS/Core/CoreModels.swift',
    'iosApp/MetaranaiIOS/Core/PortableBackup.swift',
    'iosApp/MetaranaiIOS/Core/MetalDataParser.swift',
    'iosApp/MetaranaiIOS/Core/GenreLensCore.swift',
    'iosApp/MetaranaiIOS/Core/RecommendationCore.swift',
    'iosApp/MetaranaiIOS/Core/SeedCatalog.swift',
    'iosApp/MetaranaiIOS/Core/CatalogFingerprint.swift',
    'iosApp/MetaranaiIOS/Services/LastFMService.swift',
    'iosApp/MetaranaiIOS/Services/SpotifyAuthManager.swift',
    'iosApp/MetaranaiIOS/Services/SpotifyIdentityResolver.swift',
    'iosApp/MetaranaiIOS/Services/KeychainStore.swift',
    'iosApp/MetaranaiIOS/Info.plist',
    'iosApp/MetaranaiIOS/PrivacyInfo.xcprivacy',
    'iosApp/scripts/build_simulator.sh',
    'iosApp/scripts/archive_device.sh',
    '.github/workflows/ios.yml',
    'docs/14_TESTFLIGHT_BETA.md',
    'docs/15_IOS_SPOTIFY_AND_PRIVACY.md',
    'DATA_COMPATIBILITY_V080.md',
    'UPDATE_FROM_V0.7.0.md',
    'docs/16_V080_TEST_MATRIX.md',
]
for rel in required:
    assert (root/rel).exists(), rel

backup = (root/'iosApp/MetaranaiIOS/Core/PortableBackup.swift').read_text()
assert 'static let format = "metaranai-backup"' in backup
assert 'static let currentVersion = 90' in backup
assert 'root["version"]' in backup
assert 'version ==' not in backup
for key in ['profile','history','external_artists','genre_lens_v05','vocal_profile_v05']:
    assert f'"{key}"' in backup

parser = (root/'iosApp/MetaranaiIOS/Core/MetalDataParser.swift').read_text()
for legacy in ['"SOME", "MAYBE"', '"MEH", "MISS"']:
    assert legacy in (root/'iosApp/MetaranaiIOS/Core/CoreModels.swift').read_text()
assert 'externalArtistsJSON' in parser

lens = (root/'iosApp/MetaranaiIOS/Core/GenreLensCore.swift').read_text()
for genre in ['Glam Metal','Japanese Metal','Progressive Metal','Nu Metal','Thrash Metal','Black Metal']:
    assert f'name: "{genre}"' in lens, genre
assert lens.count('.init(name:') == 18

seed = (root/'iosApp/MetaranaiIOS/Core/SeedCatalog.swift').read_text()
assert seed.count('MetalArtist(') >= 35
assert 'SKYWINGS' in seed and 'Marius Danielsen' in seed

ui = (root/'iosApp/MetaranaiIOS/ContentView.swift').read_text()
for text in ['今日のメタル','本日のジャンル:','メタル図鑑','メタルDNA','バンドを探す','JSONバックアップを復元']:
    assert text in ui, text
for reaction in ['全部好き','普通に刺さる','何曲か刺さる','イマイチ','興味なし']:
    assert reaction in (root/'iosApp/MetaranaiIOS/Core/CoreModels.swift').read_text()

appstate = (root/'iosApp/MetaranaiIOS/MetaranaiAppState.swift').read_text()
assert 'SeedCatalog.artists' in appstate
assert 'ensureGenrePool' in appstate
assert 'searchMetalArtists' in appstate
assert 'similarArtists' in appstate
assert 'PortableBackupCodec.restore' in appstate
assert 'PortableBackupCodec.exportData' in appstate

spotify = (root/'iosApp/MetaranaiIOS/Services/SpotifyAuthManager.swift').read_text()
assert 'metaranai-login://spotify/callback' in spotify
assert 'code_challenge_method' in spotify and 'S256' in spotify
assert 'client_secret' not in spotify.lower()
assert 'KeychainStore' in spotify
resolver = (root/'iosApp/MetaranaiIOS/Services/SpotifyIdentityResolver.swift').read_text()
assert 'spotify_artist_links_v080' in resolver
assert 'spotify_artist_links_v064' in resolver
assert 'CatalogFingerprint.canonicalName(rowName) == target' in resolver
assert '/albums/\\(id)/tracks' in resolver
assert 'itunes.apple.com/search' in resolver
fingerprint = (root/'iosApp/MetaranaiIOS/Core/CatalogFingerprint.swift').read_text()
for marker in ['canonicalName', 'trackMatches', 'albumMatches', 'evidenceScore', 'isStrong']:
    assert marker in fingerprint, marker
coretests = (root/'iosApp/CoreTests/main.swift').read_text()
assert "Yutaro Abe's ASTRAL WIND" in coretests
assert 'wrongSameNameTracks' in coretests

with open(root/'iosApp/MetaranaiIOS/Info.plist','rb') as f:
    info=plistlib.load(f)
assert info['CFBundleShortVersionString']=='0.9.2'
assert info['CFBundleVersion']=='20'
assert info['CFBundleURLTypes'][0]['CFBundleURLSchemes']==['metaranai-login']
with open(root/'iosApp/MetaranaiIOS/PrivacyInfo.xcprivacy','rb') as f:
    privacy=plistlib.load(f)
assert privacy['NSPrivacyTracking'] is False

workflow=(root/'.github/workflows/ios.yml').read_text()
assert 'swiftc -swift-version 5' in workflow
assert 'xcodegen generate' in workflow
assert 'CODE_SIGNING_ALLOWED=NO' in workflow
assert 'generic/platform=iOS Simulator' in workflow
assert 'Core/CatalogFingerprint.swift' in workflow

fixture=json.loads((root/'tests/fixtures/metaranai-backup-v064-sample.json').read_text())
assert fixture['format']=='metaranai-backup'
assert len(json.loads(fixture['preferences']['history']))==2
assert len(json.loads(fixture['preferences']['external_artists']))==2

print('V080_IOS_BETA_STRUCTURE_OK')
print('Android v0.9.2 retains portable JSON + SwiftUI iOS beta + PKCE + simulator CI')
