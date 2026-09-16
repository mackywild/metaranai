# MusicBrainz API運用

公式API: https://musicbrainz.org/doc/MusicBrainz_API

- 通常の参照用途ではAPI Keyは不要。
- 意味のあるUser-Agentを必ず設定する。
- 原則として1アプリ/1IPから平均1 request/secを超えない。
- 取得済みmetadataはLocal DBへキャッシュする。

推奨User-Agent例:
`Metaranai/0.7.0 (YOUR_CONTACT_URL_OR_EMAIL)`

公開前に連絡先を実在するプロジェクトURLまたは連絡可能なメールへ置き換える。
