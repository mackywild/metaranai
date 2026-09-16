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
- Android versionCode: 21
- Android versionName: 0.9.3
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
```
