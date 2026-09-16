# Troubleshooting

## Android APKが更新できない
- applicationIdが `jp.metaranai.app` か確認
- versionCodeが上がっているか確認
- 同じ固定署名keystoreか確認

## JSON復元後にArchiveが0
- backup JSON内の `preferences.external_artists` が存在するか確認
- 設定画面の `SQLite Mirror` 件数を確認
- 同じJSONを再度「バックアップを復元」から読み込む

## Spotifyが検索画面へfallback
V0.6.4以降は誤同名Artistへの直行を防ぐため、identityが確定できない場合は検索へfallbackする。これは安全側の仕様。

## Last.fm発掘が動かない
API Key、通信、過剰リクエストを確認する。
