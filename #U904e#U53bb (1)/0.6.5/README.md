# メタらない？ v0.6.4

V0.6.4 strengthens Spotify identity resolution after a real regression with `Yutaro Abe's ASTRAL WIND`. It keeps the strict no-partial-match rule and now verifies underground artists using multiple independent catalogs (Last.fm + Apple/iTunes) plus Spotify Artist-ID-specific albums/tracks.

自分のMETAL DNAを学習し、地下まで掘って「今日の1組」を推薦しながら、聴くほどPersonal Metal Archiveを育てるAndroidアプリ。

## V0.6.4 — MULTI-CATALOG SPOTIFY IDENTITY

- Spotify Artist検索は2026年仕様に合わせ `limit=10` でページング
- Artist名の部分一致は引き続き直行禁止
- MusicBrainz MBID -> Spotify relationを最優先
- Spotify検索は通常の名前検索 + `artist:` field検索の二経路で完全一致候補を収集
- 完全一致候補はLast.fmに加えApple/iTunes公開Catalogでも独立照合
- Apple側はartistIdごとに曲/Albumを取得し、同名Artistを混ぜない
- Spotify側もArtist ID単位でAlbums / Album Tracksを取得
- 2曲以上、2Album以上、または曲+Album一致なら強い本人証拠として直行
- 同名Artistが複数いる場合は、独立Catalog Fingerprintで一意な勝者が出た時だけ直行
- Spotify Genreはdeprecatedのため主判定には使わない
- 新しい本人確認済みcache `spotify_artist_links_v064` を使用
- v0.6.3以前のSpotifyリンクcacheは保存するが自動利用しない

## V0.6.0 — PERSONAL METAL ARCHIVE

V0.5系で育ててきたLocal Metal DBを、推薦の裏側だけでなくユーザー自身が探索できる資産へ昇格。

- **METAL ARCHIVE**: Built-in + External Artistを重複排除して図鑑表示、評価/Genre/Vo等で絞り込み
- **WHY THIS ARTIST?**: Genre Lens・一致DNA・高評価Artistとの近さ・Vocal DNA・HIDDEN・発掘ルートを説明
- **DEEP DIVE**: 任意ArtistからLast.fm Similar Artistsを掘り、未評価 + DNA + HIDDENで再順位付け
- **LISTEN ROUTES**: Spotify / YouTube Artist / MV / Live
- **APP ICON**: Metalピック / ホーン / サウンドウェーブをモチーフにした専用アイコン
- Strict Genre Lens / 未評価自動補充 / 5段階評価 / 無制限Local Metal DBを継続

## Compatibility

- applicationId: `jp.metaranai.app`
- SharedPreferences: `metaranai`
- versionCode: 14
- versionName: 0.6.4
- V0.4〜V0.6.3の既存データを維持
- 固定署名を設定済みなら同じ署名鍵で上書き更新可能
