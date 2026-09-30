# Google Play Privacy Policy Publish

Google Playではプライバシーポリシーの「ファイル」をアップロードするのではなく、
一般公開されたURLをPlay Consoleへ登録する。

本リポジトリでは以下を公開用ページとする。

- Source: `docs/privacy-policy.html`
- GitHub Pages URL: `https://mackywild.github.io/metaranai/privacy-policy.html`

## GitHub Pagesを有効化

1. GitHub repository の **Settings**
2. **Pages**
3. Build and deployment:
   - Source: **Deploy from a branch**
   - Branch: **main**
   - Folder: **/docs**
4. Save
5. 数分後、上記URLへブラウザからアクセスできることを確認する

## Play Consoleへ登録

1. Play Console
2. **ポリシーとプログラム > アプリのコンテンツ**
3. **プライバシー ポリシー**
4. 上記GitHub Pages URLを入力
5. 保存

## 公開前チェック

- URLがログイン不要で表示できること
- PDFではなく通常のWebページとして表示できること
- Google Playのデータ セーフティ回答とプライバシーポリシーの内容が一致していること
- アプリ内からもプライバシーポリシーへアクセスできること
- アカウント削除導線と削除対象データが実装内容と一致していること

## 現在のアプリ実装に基づく主なデータフロー

- Firebase Authentication: UID / email / display name / provider / verification state
- Firebase Storage / Firestore: optional cloud backup + metadata
- Local device: Metal DNA / ratings / search history / Local Metal DB / settings
- Spotify: OAuth token, Top Artists, Recently Played (optional)
- Last.fm / MusicBrainz / Apple public catalog: artist discovery and identity verification
- Optional provider auth: Google / Apple / Facebook / X
- YouTube: external navigation when user taps the YouTube action

公開直前に実装とStore申告を再確認すること。
