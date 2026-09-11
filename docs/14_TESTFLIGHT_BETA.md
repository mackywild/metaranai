# V0.8.0 iOS / TestFlight Beta 手順

この手順の目的は、`iosApp`をMacでビルドし、所有者のApple Developer / App Store ConnectアカウントからTestFlightへアップロードすること。

## 0. 前提

- Mac + Xcode
- Apple ID
- Apple Developer Program加入（TestFlight/App Store配布時）
- App Store Connectでアプリ作成権限
- Spotify Developer DashboardのClient ID
- Last.fm API Key

V0.8.0のBundle IDは `jp.metaranai.ios`、Versionは `0.8.0`、Buildは `16`。

## 1. Xcode Projectを生成

```bash
brew install xcodegen
cd iosApp
xcodegen generate
open MetaranaiIOS.xcodeproj
```

まずSimulatorで確認するだけなら署名は不要。

```bash
./scripts/build_simulator.sh
```

## 2. Apple Developer側

XcodeのTarget `MetaranaiIOS` > Signing & Capabilitiesで:

1. Automatically manage signingを有効化
2. Teamを自分のApple Developer Teamへ変更
3. Bundle Identifierが `jp.metaranai.ios` になっていることを確認

Bundle IDを変更する場合は、Spotify Dashboard側のiOS Bundle IDも同時に合わせる。

## 3. Spotify iOS設定

Spotify Developer DashboardでiOSアプリ情報を設定する。

- Bundle ID: `jp.metaranai.ios`
- Redirect URI: `metaranai-login://spotify/callback`
- API/SDK: Web API / iOS用途

文字列はアプリ側と完全一致させる。V0.8.0はAuthorization Code + PKCEで認証し、Client SecretはiPhone内へ埋め込まない。

公式:
- https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow
- https://developer.spotify.com/documentation/web-api/concepts/apps

## 4. 実機ビルド

Xcode上部のDestinationを接続したiPhoneへ変更しRun。

最初に確認する項目:

- Androidで書き出した旧JSONを復元できる
- 評価件数が一致する
- Local Metal DB件数が概ね一致する
- Genre Lensが復元される
- TODAY'S METALが表示される
- 5段階評価が保存される
- Spotify PKCEログインからアプリへ戻れる
- Spotifyリンクが同名別Artistへ誤爆しない
- YouTube / MV導線が開く
- Deep DiveでLocal DBが増える
- iOSで書き出したJSONをAndroid側で復元できる

## 5. Archiveを作る

ターミナルなら:

```bash
export APPLE_TEAM_ID="あなたのTeam ID"
cd iosApp
./scripts/archive_device.sh
```

またはXcodeで `Product > Archive`。

## 6. TestFlightへアップロード

Xcodeの `Window > Organizer` で作成したArchiveを選び、`Distribute App` からTestFlight/App Store Connect向けの配布を選択する。

Apple公式ではApp Store Connectへbuildをアップロード後、処理が完了するとTestFlightでテスターへ配布できる。内部テスターはApp Store Connectユーザーから最大100人まで追加できる。

公式:
- https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds
- https://developer.apple.com/help/app-store-connect/test-a-beta-version/testflight-overview/
- https://developer.apple.com/help/app-store-connect/test-a-beta-version/add-internal-testers/

## 7. 外部テストへ進む場合

外部テスター配布は内部テストより追加情報・Beta App Reviewが必要になる場合がある。まず自分/身内でInternal Testingを通してから進める。

## 8. App Store本番前に別途必要

V0.8.0はBeta。V0.9.0で少なくとも以下を確定する。

- App Store説明文
- スクリーンショット
- Support URL
- Privacy Policy URL
- App Privacy回答
- 年齢レーティング
- 暗号化/Export Compliance回答
- 第三者サービス表記
- 最終Bundle ID / Versioning
