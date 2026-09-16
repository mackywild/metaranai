# Spotify Developerセットアップ

公式Dashboard: https://developer.spotify.com/dashboard
公式Appsガイド: https://developer.spotify.com/documentation/web-api/concepts/apps

## App作成
1. Spotify for Developersへログインする。
2. DashboardでCreate appを実行する。
3. App Name / Descriptionを入力し規約を確認する。
4. Client IDを控える。モバイルアプリにClient Secretを埋め込まない。
5. Android Package IDは `jp.metaranai.app` を使用する。
6. iOS化時はBundle IDもSpotify Dashboardへ登録する。

## OAuth
モバイルではAuthorization Code with PKCEを使用する。
公式: https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow

現Android実装のloopback callbackを使う場合は、DashboardとアプリのRedirect URIを完全一致させる。
Spotifyはloopbackでは `127.0.0.1` の明示IPを許可している一方、`localhost` はRedirect URIとして不可としている。
公式: https://developer.spotify.com/documentation/web-api/concepts/redirect_uri

## iOS
SpotifyのAppsガイドに従い、iOS用redirect schemeはアプリ固有・小文字・専用schemeとし、Bundle IDを登録する。
V0.8.0でiOS OAuthを実装する際に確定する。
