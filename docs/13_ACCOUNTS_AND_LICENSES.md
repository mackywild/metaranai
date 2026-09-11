# Accounts / API Keys / Developer Programs Checklist

V0.7.0時点の公開準備一覧。公開直前には各公式ページで最新条件を再確認する。

| 対象 | 何が必要か | 現在の用途 |
|---|---|---|
| Last.fm | Last.fm account + API application/API Key | discovery / tags / similar / stats |
| Spotify | Spotify developer app + Client ID | OAuth(PKCE) / listening DNA / Artist identity |
| MusicBrainz | 通常参照はAPI Key不要。meaningful User-Agent必須 | Artist metadata / identity |
| Google Play | Play Console developer account。1回限り25 USD登録料 | Android一般公開 |
| Apple | Apple Developer Program。年99 USD（地域通貨の場合あり） | App Store / TestFlight配布 |

## Last.fm
API account作成: https://www.last.fm/api/account/create
API intro: https://www.last.fm/api/intro

## Spotify
Dashboard: https://developer.spotify.com/dashboard
Apps: https://developer.spotify.com/documentation/web-api/concepts/apps
PKCE: https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow
Redirect URI: https://developer.spotify.com/documentation/web-api/concepts/redirect_uri

## MusicBrainz
API: https://musicbrainz.org/doc/MusicBrainz_API
Rate limit: https://musicbrainz.org/doc/MusicBrainz_API/Rate_Limiting

## Google Play
https://support.google.com/googleplay/android-developer/answer/6112435?hl=ja

## Apple Developer / App Store Connect
Enrollment: https://developer.apple.com/help/account/membership/program-enrollment
App Store Connect workflow: https://developer.apple.com/help/app-store-connect/get-started/app-store-connect-workflow
TestFlight: https://developer.apple.com/help/app-store-connect/test-a-beta-version/testflight-overview/
