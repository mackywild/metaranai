# iOS Spotify / Privacy Notes — V0.8.0

## Spotify認証

V0.8.0はAuthorization Code with PKCEを使う。mobile appへClient Secretを埋め込まない。

Redirect URI:

`metaranai-login://spotify/callback`

SpotifyのiOSアプリ登録ではBundle IDも `jp.metaranai.ios` に合わせる。Redirect URIはDashboard登録値とアプリ値が完全一致する必要がある。

## Token保存

iOSではaccess token / refresh token / expiryをKeychainへ保存する。Android由来JSONを復元した場合は、legacy preferenceからKeychainへ移行する。

ただしportable JSON backup互換性を維持するため、バックアップ書き出し時にはKeychain内tokenを従来キー名へ戻してJSONへ含める。バックアップJSON自体は秘密情報を含む可能性があるため共有しないこと。

## Privacy Manifest

`PrivacyInfo.xcprivacy` をiOS targetへ同梱する。

Appleはprivacy manifestをアプリ/SDKのprivacy practiceとRequired Reason API申告に使用している。App Store提出前に、実際に採用したSDK・APIとmanifest内容が一致しているか再確認する。

公式:
https://developer.apple.com/documentation/bundleresources/adding-a-privacy-manifest-to-your-app-or-third-party-sdk

## メタらない？側のデータ方針

現状は独自サーバへユーザープロファイルを送信しない設計。Spotify / Last.fm / MusicBrainz / Apple公開カタログ等へ、機能実行に必要なAPI requestは送信する。

App Store ConnectのApp Privacy回答は「端末内保存」と「外部サービスへ送られるrequest」を区別して、公開直前に実装と照合すること。
