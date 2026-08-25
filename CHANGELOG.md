# Changelog

## 0.5.0

- V0.4データ完全互換を維持（applicationId / SharedPreferences / legacy keys unchanged）
- Spotify Artistページ直接遷移（Web API search + external_urls.spotify）
- Spotify未収録/照合失敗時は検索へfallback
- Genre Lens: OFF / 手動 / 曜日
- 曜日ごとに複数ジャンル設定
- Genre Lens時にLast.fm tag.getTopArtistsからジャンル母集団を追加発掘
- DNAを主軸に残した推薦V5
- Vocal DNA: 男性Vo / 女性Vo / 混成Vo
- Last.fm tagからVoタイプ推定
- 動的タイプ名へVo偏愛prefix追加
- 既存履歴からListening Mapを再分析
- アプリ内JSON Backup / Restore
- ADB V0.4 backup/restore scripts
- 固定署名用GitHub Secrets対応
- Android 16 / API 36, Gradle 9.5.0 CIへ固定
