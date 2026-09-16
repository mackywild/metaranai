# Update from v0.6.0 to v0.6.1

`applicationId=jp.metaranai.app` と SharedPreferences `metaranai` は維持。
評価履歴、Local Metal DB、Genre Lens、Spotify認証情報はそのまま残ります。

V0.6.0までの `spotify_artist_links_v05` は誤リンクを含む可能性があるため、V0.6.1では読みません。削除もしないためバックアップ互換性は維持します。新しい本人確認済みリンクは `spotify_artist_links_v061` に保存されます。
