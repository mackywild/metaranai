# メタらない？ v0.11.5 — CONNECTION STORYBOARD

- Spotify / Last.fmの「連携すると」を横に読む3コマのイラストへ変更。音楽の履歴 → Metal DNA → 未知のMetal発掘を視覚化。
- 横スワイプで各コマを読み、短い説明で効果を確認。ログイン・連携・解析の手順は維持。
- Android versionCode 35 / versionName 0.11.5。

# メタらない？ v0.11.4 — TODAY DIRECT ACTIONS

- 「今日」は従来どおりSpotify / YouTube / 評価 / 深掘りを直接実行。詳細ダイアログは開かない。
- 「今日」の深掘り結果も従来の配信先ボタンを維持。
- 「探す」「図鑑」はアーティストをタップして詳細から試聴・評価・再評価。
- Spotify / YouTube押下時の検索・移動案内は引き続き非表示。
- Android versionCode 34 / versionName 0.11.4。

# メタらない？ v0.11.3 — ARTIST DETAILS & SPOTIFY PERIODS

- アーティストをタップして詳細を開き、Spotify / YouTubeへ移動、評価・同日中の再評価が可能。
- 「探す」「図鑑」「深掘り結果」の一覧からの直接Spotify / YouTube遷移を廃止。深掘りは一覧で実行可能。
- 配信先を開いた際の検索・移動案内を非表示化。「今日」の試聴も詳細ダイアログへ統一。
- DNA画面でSpotify Topアーティストを約1か月 / 約6か月 / 約1年で切り替え。期間別に保存し、表示期間の切り替えはDNA再学習を行わない。
- Spotify / Last.fm連携設定に、好みの解析・未知Metal発掘・ランキング比較など利用者向けの魅力を追加。
- Android versionCode 33 / versionName 0.11.3。JSONバックアップ互換性を維持。iOSは変更なし。
- Spotifyの期間はプロバイダー定義の概算（4週間 / 6か月 / 約1年）。公式仕様: https://developer.spotify.com/documentation/web-api/reference/get-users-top-artists-and-tracks

# メタらない？ v0.11.2 — SEARCH & DISCOVERY FIXES

- 「探す」は検索窓 → グローバル検索 → Spotify / Last.fm / 好みからの発掘 → 検索結果の順。
- 連携先から取得したMetal候補を検索結果へ表示。検索を切り替えると前の結果をクリア。
- Spotifyログインは設定で実施。「探す」は保存済みセッションだけで解析し、再認証が必要なら設定を案内。
- 手動発掘はジャンル補充の候補数チェックで終了せず、類似候補と最大3ページのジャンル候補を探索。
- 外部で新規候補を取得できない場合、固定ジャンル内の保存済み未評価候補を明示して表示。通信エラーと全候補評価済みは理由を表示。
- 「探す」の深掘りは詳細検索ダイアログを開き、さらに掘ると同じダイアログの対象・結果を更新。
- Android versionCode 32 / versionName 0.11.2。iOS実装と配布バージョンは変更なし。

# メタらない？ v0.11.0 — OPTIONAL LAST.FM TASTE PROFILE

V0.11.0は、既存Deep Diveを壊さず、Last.fmの公開視聴履歴を任意のパーソナライズ入力として追加するアップデートです。

## V0.11.0 Highlights
- Spotify / Last.fm（任意）/ ジャンル選択の3経路で初期Metal DNAを作成。
- Last.fmはユーザー名のみ保存し、パスワードやLast.fmユーザーセッションは扱わない。
- overall / 12month / 6month / 7day Top Artists + Recent TracksからMetal Seedを抽出。
- Last.fm未連携でもArtist Search / Genre補充 / Deep Diveは従来どおり利用可能。
- Deep Dive本体はV0.6系の内部処理を維持し、V0.11回帰チェックで保護。
- Android versionCode 30 / versionName 0.11.0。

# メタらない？ v0.9.3 — AUTHENTICATION RELIABILITY

V0.9.3は、GoogleをAndroidの主力ログインとして整理し、認証とクラウドStorageを分離した認証安定化アップデートです。DNA/推薦/Local Metal DBの既存データ互換性は維持します。

## V0.9.3 Highlights
- Google: Android Credential Manager -> Google ID token -> Firebase Authentication。
- Email/Password: fallback。新規登録は確認メールを送り、確認完了後にログイン。
- Guest: Firebase未設定でもLocal Guestで利用可能。
- Firebase Authに必要なのは `FIREBASE_API_KEY` / `FIREBASE_APP_ID` / `FIREBASE_PROJECT_ID`。
- `FIREBASE_STORAGE_BUCKET` はクラウド同期専用。未設定でもGoogle/Emailログインを止めない。
- Android versionCode 21 / versionName 0.9.3。
- iOS project metadata: Marketing Version 0.9.3 / Build 21（Native Firebase account wiringは別release task）。

## V0.9.2 Highlights
- DNA画面は「あなたのメタルDNA」「DNA名」「8軸の数値」「刺さっているジャンル」を中心に表示。
- DNA名・DNA数値はユーザー操作で変更せず、評価・探索・Spotify解析による学習結果で自動更新。
- 自動命名の内部カウンタや再生成タイミングはUIに表示しない。
- Android versionCode 20 / versionName 0.9.2。
- iOS Marketing Version 0.9.2 / Build 20。

## V0.9.0 Highlights
- 新規ユーザーの初期MetalVectorを8軸すべて0.50のニュートラルへ変更。旧ユーザーの保存済みprofileは変更しない。
- 初回OnboardingでSpotify視聴傾向またはGenreを選択して初期METAL DNAを生成。Genre初期値は旧built-in catalogではなくGenre Lens定義から作る。
- Firebase Authentication基盤：Google / Apple / Facebook / X(Twitter) / Email / Anonymous Guest。
- Firebase StorageへユーザーUID単位でportable JSONを保存し、別端末から復元。
- V0.8以前の既存端末データは、cloudが空のとき明示確認後のみアカウントへupload。勝手に上書きしない。
- 従来 `metaranai-backup` JSONはversion 90で継続し、旧versionを拒否しない。
- Account Delete / Sign Out / Manual Cloud Syncを追加。
- iOS Coreの初期DNAもニュートラル化し、backup version 90へ更新。
- `Yutaro Abe's ASTRAL WIND` Spotify Identity回帰試験は継続。

## Firebase
Google/Email認証とCloud Storageは独立設定。詳細は `docs/17_ACCOUNT_AND_FIREBASE_SETUP.md` / `docs/20_V093_AUTH_SETUP.md`。Firebase未設定でもGuest + Local + JSON Backupは利用可能。

## Compatibility
- Android applicationId: `jp.metaranai.app`
- Android SharedPreferences: `metaranai`
- Android versionCode: 34
- Android versionName: 0.11.4
- iOS Bundle ID: `jp.metaranai.ios`
- iOS Version: 0.9.3
- iOS Build: 21
- Portable backup: `format = metaranai-backup`, version 90

## Regression
```bash
python tools/check_update_compat.py
python tools/check_v064_spotify_identity.py
python tools/check_v070_backup_compat.py
python tools/check_v080_ios_beta.py
python tools/check_v090_account_personalization.py
python tools/check_v011_lastfm_optional_profile.py
```
