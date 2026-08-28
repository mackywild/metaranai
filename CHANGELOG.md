# Changelog

## 0.6.0

- 新アプリアイコンを追加（Android launcher icon各densityへ配置）
- 下部ナビの「発掘」を `図鑑` へ刷新し、Personal Metal Archiveを実装
- Archive総数 / External DB / 評価済み / 全部好き件数を表示
- Archiveをバンド名・国・ジャンル、5段階評価/未評価、Genre Lens、Vocal Typeで絞り込み
- Archive各ArtistからSpotify / YouTube / Deep Diveへ直行
- `WHY THIS ARTIST?` を追加し、Genre Lens・DNA一致・高評価Artist類似・Vocal DNA・HIDDEN・発掘ルートを説明
- `DEEP DIVE` を追加。任意ArtistをSeedにLast.fm Similar Artistsを掘り、未評価 + Personal DNA + HIDDENで並び替え
- YouTube Artist / MV / Live検索導線を追加（YouTube API Key不要の外部検索導線）
- Deep Dive取得Artistは従来の無制限Local Metal DBへマージ
- V0.5.4のStrict Genre Lens / 未評価自動補充 / 5段階評価 / 世界検索を継続
- applicationId / SharedPreferences / legacy keysを維持

## 0.5.4

- Genre Lensの補充判定を「総候補数」から「未評価候補数」へ変更
- 過去に評価済みのArtistをStrict Genre Lensの通常推薦から除外
- 未評価候補が各ジャンル10組未満になるとLast.fmから自動補充
- 補充時は評価済みArtistを候補数へ含めず、未評価20組を目標に新規発掘
- Genre Lens表示へ「未評価○組 / 総数○組」を追加
- 評価ボタンの無言returnを廃止し、当日評価済みの場合も画面へ理由を表示
- 評価成功時に「○○を記録しました」のフィードバックを表示
- 未評価候補が尽きた場合は評価済み・別ジャンルへ戻さず、地下探索状態へ移行
- V0.5.3の18 Genre Lens、無制限Local DB、5段階評価、世界検索を継続
- applicationId / SharedPreferences / legacy keysを維持

## 0.5.3

- Genre Lensへ `Glam Metal` を追加（hair metal / sleaze metal / glam rock系タグも補助判定）
- Genre Lensへ `Japanese Metal` を追加
- Japanese MetalはLast.fmタグだけでなくMusicBrainz/既存データのJapan地域情報でも判定
- Japanese Metalは地域Lensとして扱い、音楽的な並び順はPersonal METAL DNAを優先
- `Progressive Metal` の別名判定を強化（prog / progressive death / progressive power / technical progressive）
- Genre Lensへ `Nu Metal` を追加（nü metal / rap metalを補助判定）
- V0.5.2のStrict Genre Lens、各ジャンル10組自動補充、無制限Local DB、5段階評価を継続
- applicationId / SharedPreferences / legacy keysを維持

## 0.5.2

- Genre Lensを加点方式から必須候補条件へ変更
- 複数Genre LensはOR条件として扱う
- 指定ジャンル候補が各10組未満ならLast.fmタグ探索で先に自動補充
- Genre Lens探索中は別ジャンルの「今日」を表示しない
- Genre Lens候補数をリアルタイム表示
- `tag.getTopArtists`由来のジャンル情報をLocal DBへ保持
- Genre Lens内ではMETAL DNAを主軸にHIDDEN/未知/探索/発掘度で順位付け
- V0.5.1の無制限Local DB・世界検索・5段階評価・Spotify直行を継続
- applicationId / SharedPreferences / legacy keysを維持

## 0.5.1

- Local Metal DBの500 Artist上限を撤廃
- 外部発掘・外部検索で取得したMetal Artistを継続キャッシュ
- 検索履歴の50件保存上限を撤廃
- Last.fm `artist.search` によるリアルタイム外部Artist検索
- 外部検索結果をTop Tags / listeners / playcount / MusicBrainzで照合してLocal DBへ保存
- Spotify Artistページ直行を継続（完全一致できない場合はSpotify検索へfallback）
- 評価を3段階から5段階へ拡張
  - 💘 全部好き
  - 🔥 普通に刺さる
  - 🎵 何曲か刺さる
  - 😐 イマイチ
  - 💀 興味なし
- 旧評価の安全移行: HIT→普通に刺さる / MAYBE→何曲か刺さる / MISS→イマイチ
- METAL DNA / Vocal DNA / Genre分析の学習重みを5段階評価へ対応
- 発掘統計へ「全部好き」「好評価率」「平均刺さり」を追加
- Genre Lens切替時に「今日」を即時再推薦
- Genre Lens切替後、選択ジャンルのLast.fm母集団を自動補充して再推薦
- GitHub ActionsのSecrets条件判定を`env`経由へ修正
- V0.5と同一`applicationId` / SharedPreferences / legacy keysを維持

## 0.5.0

- Spotify Artistページ直接遷移
- Genre Lens: OFF / 手動 / 曜日
- 曜日ごとに複数ジャンル設定
- Vocal DNA: 男性Vo / 女性Vo / 混成Vo
- MusicBrainz + Last.fm HIDDEN discovery
- JSON Backup / Restore
