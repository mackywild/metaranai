# Changelog

## V0.9.1.1 — AUTO DNA CORRECTION
- V0.9.1で追加したDNA手動スライダー／「この数値でDNAを生成」／ランダムDNAを廃止。
- DNAの8軸数値とDNA名はユーザー編集不可とし、評価・探索・Spotify解析による学習結果だけで更新。
- DNA名は学習による数値変動5回ごとに自動再生成し、頻繁な名称揺れを抑制。
- DNA画面は編集UIから読み取り専用の進捗表示へ変更し、次回自動生成までの残り変動回数を表示。
- Android / iOS双方で生成済みDNA名と変動カウンタを永続化。バックアップ復元時も互換性を維持。
- Android versionCode 19 / versionName 0.9.1.1。iOSはApp Store互換のためMarketing Version 0.9.1のままBuild 19。


## V0.9.1 配布向けUI調整
- 日本語中心の配布向けUIへ整理
- Spotify完全一致時のみリンク有効
- 「見つからなかった」記録を追加（DNA学習対象外）
- Vo絞り込みUIを廃止し内部データのみ維持
- 数値スライダー式Metal DNA生成を追加


## 0.9.0 — ACCOUNT & PERSONALIZATION
- Brand-new user profile seed changed from melody-heavy values to neutral 0.50 x 8 axes. Existing saved profiles are preserved.
- First-run onboarding added: account choice + Spotify or multi-Genre initial METAL DNA. The old melody-heavy catalog is not used to create a new user profile.
- Firebase Auth gateway added for Google, Apple, Facebook, X/Twitter, Email/Password, and Anonymous Guest.
- Cloud backup uses Firebase Storage per UID (`users/{uid}/metaranai-backup.json`) so the growing Local Metal DB is not limited by a single Firestore document.
- Existing V0.8-and-earlier local records require explicit migration when an authenticated account has no cloud backup.
- Cloud restore imports the same portable JSON and rebuilds the SQLite Archive.
- Manual cloud sync, sign-out and account-delete controls added.
- Existing portable JSON format remains compatible; export version raised to 90.
- iOS neutral seed / backup version / marketing version raised to V0.9.0 foundation.
- Added account/Firebase setup manual and V0.9.0 release test matrix.

## 0.8.0 — CROSS PLATFORM BETA
- SwiftUIでiOS版5タブ（今日 / 探す / 図鑑 / DNA / 設定）を実装。
- Android RecommendationEngine / METAL DNA / Strict Genre Lens / 5段階評価をSwiftへ移植。
- Android built-in catalog 35組をiOS SeedCatalogへ移植。
- Last.fm世界検索、Genre Lens未評価候補補充、Deep DiveをiOSへ実装。
- Spotify Authorization Code + PKCEをiOSへ実装し、tokenをKeychainへ保存。
- Spotify本人確認はAndroid v0.6.4 verified cacheを再利用し、新規候補は完全一致 + Last.fm/Apple曲・Album指紋で保守的に確認。
- Android V0.5〜V0.7のmetaranai-backup JSONをiOSで復元し、iOSからAndroid互換JSONを書き出せるようにした。
- 旧 MAYBE/MISS 評価migrationをiOSでも維持。
- Info.plist URL Scheme `metaranai-login://spotify/callback`、AppIcon、PrivacyInfo.xcprivacyを追加。
- XcodeGen project、Simulator build script、Device Archive scriptを追加。
- GitHub ActionsへSwift core test + macOS iOS Simulator buildを追加。
- TestFlight / iOS Spotify / Privacy手順書を追加。
- Android versionCode 16 / versionName 0.8.0へ更新。

## 0.7.0 — PROJECT REFORGE
- TODAY画面の情報階層を整理し、WHY THIS ARTIST / SCORE BREAKDOWNを折りたたみ化。
- 5段階評価UIを2列化し、主要操作までのスクロール量を削減。
- METAL ARCHIVEへDNAおすすめ順 / HIDDEN / 名前順ソートを追加。
- Android `SQLiteOpenHelper` ベースの `metaranai_archive.db` を追加。
- 既存 `external_artists` JSONを初回起動時にSQLiteへmirrorし、以後もJSON + DBを同期。
- 従来 `metaranai-backup` JSON formatを継続し、V0.4〜V0.6.x backupをversion固定で拒否しない。
- 旧JSON復元後にSQLite Archiveを `external_artists` から全再構築する。
- Android Adaptive Icon / Android 13+ monochrome themed iconを追加。
- iOS用AppIcon asset setとSwiftUI移植starterを追加。
- iOS側にも従来JSON envelopeを読めるLegacyBackupImporter sampleを追加。
- `docs/`へSpotify / Last.fm / MusicBrainz / Android署名 / Apple Developer / Google Play / App Store / TestFlight / Privacy / Backupのマニュアルを追加。
- iOS正式版はまだ未リリース。V0.8.0 TestFlightを次段階とする。

## 0.6.4
- `Yutaro Abe's ASTRAL WIND` 実ケースでSpotify Identity Resolverを再試験し、v0.6.3の不足を修正。
- Spotify Artist SearchをUIに近い通常名前検索 + artist field検索の二経路に変更。
- Spotify Genre（deprecated / 空になり得る）への依存を本人確認の主経路から外した。
- Last.fm Top Tracksが空/疎な地下Artist向けに、Apple/iTunes Search APIを独立Catalog Fingerprintとして追加。
- Apple結果はartistId単位に分離し、同名Artistの曲を混在させない。
- Spotify候補はArtist ID単位でAlbums / Album Tracksまで取得し、曲名 + Album名を照合。
- 完全一致1件でも、2曲以上・2Album以上・曲+Album一致など強い独立証拠がある場合のみ直行。
- 同名Artist複数は1候補だけが明確に強いCatalog Fingerprintを持つ場合のみ直行。
- 新cache `spotify_artist_links_v064` を使用し、v0.6.3以前の判定を自動再利用しない。
- 旧v0.6.3のASTRAL WIND試験がハードコードのみだった点を修正し、Last.fm空 + Apple Catalog有りの回帰ケースを追加。

## 0.6.3
- Spotify Searchの2026年仕様に対応し、`limit=20` を廃止して `limit=10` + offset pagingへ変更。
- Last.fm `artist.getTopTracks` とSpotify収録曲を照合するTrack Fingerprintを追加。
- 完全一致1件 + 2曲以上一致でSpotify Artistページへ直接遷移。
- 1曲一致時は高信頼MBID / MusicBrainzメタデータ / Genre証拠を補助条件として使用。
- 同名Artistが複数いる場合は、曲指紋で一意の勝者が出た場合のみ直行。
- Spotify曲検索結果はSpotify Artist IDでフィルタし、同名別Artistの曲を混ぜない。
- Spotify Track Searchが少ない場合はArtist Albums / Album Tracksで曲指紋を補完。
- 2026年に削除されたSpotify Artist Top Tracks endpointは使用しない。
- 新しい本人確認済みcache `spotify_artist_links_v063` を使用し、v0.6.2以前のcacheは自動利用しない。

## 0.6.2
- Spotify Identity Resolver 2: exact-name direct navigation restored when identity evidence is strong.
- Added MusicBrainz disambiguation using country / area / begin year.
- Partial-name direct navigation remains prohibited.
- Same-name ambiguity remains conservative.
- New verified cache `spotify_artist_links_v062`; old caches are preserved but not auto-read.

## 0.6.1

- Spotify Artist Identity Resolverを全面修正
- 信頼度90以上のMBID -> MusicBrainz URL relation -> Spotify Artistを最優先
- Last.fmが返したMBIDをMusicBrainzの名前検索結果より優先し、同名誤同定を抑制
- Artist名の部分一致で直接遷移する挙動を廃止
- 完全同名ArtistはGenre証拠で一意に絞れた場合のみ直行
- 判定不能時は安全にSpotify検索へフォールバック
- 旧 `spotify_artist_links_v05` の誤リンクcacheを無効化し、`spotify_artist_links_v061`へ切替

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
