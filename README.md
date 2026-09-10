# メタらない？ v0.6.3

V0.6.3 adds a track-fingerprint Spotify Identity Resolver. It keeps the strict no-partial-match rule, but can now confirm underground artists even when Spotify genre metadata is empty.

自分のMETAL DNAを学習し、地下まで掘って「今日の1組」を推薦しながら、聴くほどPersonal Metal Archiveを育てるAndroidアプリ。

## V0.6.3 — SPOTIFY TRACK FINGERPRINT

- Spotify Artist検索は2026年仕様に合わせ `limit=10` でページング
- Artist名の部分一致は引き続き直行禁止
- MusicBrainz MBID -> Spotify relationを最優先
- 完全一致候補はLast.fm `artist.getTopTracks` とSpotify収録曲名を比較
- 2曲以上一致したら「曲指紋」で本人確認してArtistページへ直行
- 1曲一致でも高信頼MBID/メタデータ/Genre証拠があれば直行
- 同名Artistが複数いる場合は、曲指紋で一意な勝者が出た時だけ直行
- Spotify側の曲は必ずSpotify Artist IDでフィルタし、同名別Artistの楽曲混入を防止
- Track Searchが薄い地下ArtistではArtist Albums -> Album Tracksで補完
- 新しい本人確認済みcache `spotify_artist_links_v063` を使用
- v0.6.2以前のSpotifyリンクcacheは保存するが自動利用しない

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
- versionCode: 13
- versionName: 0.6.3
- V0.4〜V0.6.2の既存データを維持
- 固定署名を設定済みなら同じ署名鍵で上書き更新可能
