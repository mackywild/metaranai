# Last.fm APIセットアップ

公式API: https://www.last.fm/api

1. Last.fmアカウントを用意する。
2. API account/applicationを作成しAPI Keyを取得する。
3. メタらない？ > 設定 > Last.fm API Keyへ入力する。
4. 「未知のMetalを発掘」で疎通確認する。

## 運用上の注意
Last.fmは識別可能なUser-Agentを推奨し、過剰なAPIコールを避けるよう案内している。
API結果はLocal Metal DBへキャッシュし、同じ情報を無駄に再取得しないこと。

公式ドキュメント: https://www.last.fm/api/intro
