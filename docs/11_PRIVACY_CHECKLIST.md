# Privacy / Data Safety Checklist

公開前に実データフローを再監査する。

現在想定する外部通信:
- Spotify Web API: Spotify認証・聴取傾向・Artist identity
- Last.fm: Artist / tag / similar / stats探索
- MusicBrainz: Artist metadata / identity
- Apple/iTunes public catalog: Spotify本人照合の補助fingerprint

ローカル保存:
- Metal DNA
- 評価履歴
- 検索履歴
- Local Metal DB
- API/OAuth設定

実装とStore申告が一致していることを確認し、Privacy Policyを公開URLへ配置してからストア申請する。
