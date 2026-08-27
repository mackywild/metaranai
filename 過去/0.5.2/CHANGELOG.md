# Changelog

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
