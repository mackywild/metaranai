# メタらない？ v0.6.0

自分のMETAL DNAを学習し、地下まで掘って「今日の1組」を推薦しながら、聴くほどPersonal Metal Archiveを育てるAndroidアプリ。

## V0.6.0 — PERSONAL METAL ARCHIVE

V0.5系で育ててきたLocal Metal DBを、推薦の裏側だけでなくユーザー自身が探索できる資産へ昇格。

- **METAL ARCHIVE**
  - Built-in + External Artistを重複排除して図鑑表示
  - Archive総数 / External DB / 評価済み / 💘全部好き を表示
  - バンド名 / 国 / ジャンル検索
  - 未評価 / 5段階評価 / Genre Lens / Vocal Typeで絞り込み
  - 過去評価・当時のDNA MATCH・HIDDEN・発掘度を表示
- **WHY THIS ARTIST?**
  - 推薦スコア内訳に加えて、Genre Lens・一致DNA・高評価Artistとの近さ・Vocal DNA・HIDDEN・発掘ルートを説明
- **DEEP DIVE**
  - 任意ArtistをSeedとしてLast.fm Similar Artistsを地下探索
  - 未評価Artistを優先し、Personal METAL DNA + HIDDEN + 発掘度で再順位付け
  - 取得候補は既存の無制限Local Metal DBへ保持
- **LISTEN ROUTES**
  - Spotify Artist直行
  - YouTube Artist検索
  - YouTube MV検索
  - YouTube Live検索
- **APP ICON**
  - Metalピック / ホーン / サウンドウェーブをモチーフにした専用アイコンを追加
- V0.5.4のStrict Genre Lens / 未評価10組未満での自動補充 / 5段階評価を継続

## Compatibility

- applicationId: `jp.metaranai.app`
- SharedPreferences: `metaranai`
- versionCode: 10
- versionName: 0.6.0
- V0.4〜V0.5.4の既存データを維持
- 固定署名を設定済みなら同じ署名鍵で上書き更新可能
