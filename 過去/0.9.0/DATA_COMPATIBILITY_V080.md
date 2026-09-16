# Data Compatibility — V0.8.0

V0.8.0の最優先条件は、Androidで蓄積した既存データをiOS移行で失わないこと。

## Portable contract

引き続き以下を正規portable形式とする。

- `format = "metaranai-backup"`
- `preferences` object
- `profile`
- `history`
- `search_history`
- `external_artists`
- `genre_lens_v05`
- `vocal_profile_v05`
- Spotify / Last.fm関連設定

`version`は読込時の足切り条件にしない。V0.5 / V0.6 / V0.7形式をV0.8.0 iOSで読める。

## Android -> iOS

1. AndroidでJSON backupを書き出す
2. iPhoneのFilesへ渡す
3. iOS Settings > DATA SAFETY > JSONバックアップを復元
4. history / external_artists / profile / Genre Lensを復元
5. Spotify tokenが含まれる場合はKeychainへ移行

## iOS -> Android

iOS V0.8.0も同じenvelopeをversion 80で書き出す。Android側Importerはspecific versionを要求しないため、同じpreferences keysを復元できる。

## DBについて

Android SQLite mirrorはportable backupではない。iOSもDBファイルコピーを必須にしない。portable sourceはJSONで固定する。
