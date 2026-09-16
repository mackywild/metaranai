# 初期セットアップ

## Android
1. JDK 17を準備する。
2. Android SDK Platform 36 / Build Tools 36.0.0を準備する。
3. `gradle :app:assembleDebug` で開発ビルドする。
4. 配布版は `docs/05_ANDROID_SIGNING.md` の固定署名を使う。

## アプリ内で必要な外部設定
- Last.fm API Key: 地下Artist探索用
- Spotify Client ID: Spotify DNA同期/Artist照合用
- MusicBrainz: API Key不要。適切なUser-Agentとレート制限順守が必要

## データ保護
大型更新前には設定 > DATA SAFETY > 分析データをバックアップを実行する。
V0.7.0は従来の `metaranai-backup` JSONを読み込める。
