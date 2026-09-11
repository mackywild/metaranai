# V0.5.3 → V0.5.4 update

V0.5.4 keeps the same Android identity and data store.

- applicationId: `jp.metaranai.app`
- SharedPreferences: `metaranai`
- versionCode: 9
- versionName: 0.5.4

既存の `profile`, `history`, `search_history`, `external_artists`, Genre Lens, Vocal DNA, Spotify/Last.fm設定はそのまま引き継ぎます。

## Main fix

Genre Lensのプール判定を総Artist数ではなく未評価Artist数へ変更しました。未評価が10組未満になると、評価済みArtistを除外して20組を目標に追加探索します。

同じ固定署名鍵でV0.5.4をbuildし、`adb install -r app-release.apk` で更新してください。
