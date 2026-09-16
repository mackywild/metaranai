# メタらない？ v0.9.2 — DNA UI CLEANUP

誰がインストールしても、その人自身のMETAL DNAから始まるためのアカウント/初期登録アップデート。


V0.9.2は、METAL DNAをユーザー編集不可の自動学習プロフィールとして整理し、DNA画面から内部仕様の説明や「次回変動まであと○回」表示を取り除いた配布向け更新です。

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
Social/Email cloud accountを有効にするには `docs/17_ACCOUNT_AND_FIREBASE_SETUP.md` の設定が必要。Firebase未設定でもGuest + Local + JSON Backupは利用可能。

## Compatibility
- Android applicationId: `jp.metaranai.app`
- Android SharedPreferences: `metaranai`
- Android versionCode: 20
- Android versionName: 0.9.2
- iOS Bundle ID: `jp.metaranai.ios`
- iOS Version: 0.9.2
- iOS Build: 20
- Portable backup: `format = metaranai-backup`, version 90

## Regression
```bash
python tools/check_update_compat.py
python tools/check_v064_spotify_identity.py
python tools/check_v070_backup_compat.py
python tools/check_v080_ios_beta.py
python tools/check_v090_account_personalization.py
```
