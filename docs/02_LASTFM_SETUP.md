# Last.fm API / 任意プロフィール連携セットアップ

公式API: https://www.last.fm/api

## リリースビルド
1. 開発者がLast.fm API account/applicationを作成し、API Keyを取得する。
2. GitHub Actions / release環境の `LASTFM_API_KEY` に設定する。
3. 一般ユーザーはAPI Keyを入力しない。Artist検索、Genre補充、Deep Diveはアプリ側Keyで実行する。
4. Last.fmを利用しているユーザーだけ、設定 > Last.fm連携（任意）でユーザー名を入力できる。
5. 公開のTop Artists / Recent TracksからMetal DNAと発掘Seedを強化する。Last.fmパスワードやsession keyは保存しない。

## 開発ビルド
`LASTFM_API_KEY` が空のビルドでは、設定 > 外部検索・発掘に開発用API Key入力欄を表示する。
これはローカル検証用fallbackであり、一般ユーザー向けの必須設定ではない。

## 利用API
- user.getTopArtists: overall / 12month / 6month / 7day
- user.getRecentTracks: 最大200件
- artist.getTopTags: Metal Seed判定
- artist.getSimilar / tag.getTopArtists / artist.search: 既存の発掘機能

## 運用上の注意
Last.fmは識別可能なUser-Agentを使用し、過剰なAPIコールを避ける。
発掘結果はLocal Metal DBへキャッシュし、同じ情報を無駄に再取得しない。

公式ドキュメント:
- https://www.last.fm/api/intro
- https://www.last.fm/api/show/user.getTopArtists
- https://www.last.fm/api/show/user.getRecentTracks
