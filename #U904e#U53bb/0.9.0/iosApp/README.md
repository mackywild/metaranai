# メタらない？ iOS v0.9.0 — ACCOUNT / PERSONALIZATION COMPAT

V0.9.0はV0.8.0 SwiftUI betaを維持し、neutral初期DNAとversion 90 portable JSON互換へ更新した。Android側ではFirebaseアカウント/Cloud Syncを先行実装。iOSのGoogle/Apple/Facebook/Xクラウド認証配線はXcode/Firebase SDK設定後の次段階。

## 実装済み

- TODAY'S METAL / Personal METAL DNA推薦
- Strict Genre Lens（18ジャンル、複数選択）
- 5段階評価とDNA学習
- METAL ARCHIVE（検索 / Genre / 評価 / Vo / 並び替え）
- Last.fm世界検索
- 未評価候補が少ないGenre Lensの自動補充
- Deep Dive（Similar Artist発掘）
- Spotify Authorization Code + PKCE
- Spotify誤リンク防止用の独立カタログ指紋照合
- Spotify / YouTube / MV導線
- Android V0.5〜V0.7の `metaranai-backup` JSON復元
- iOSからAndroid互換JSONを書き出し
- AppIcon / PrivacyInfo.xcprivacy

## 最初にやること

1. MacへXcodeをインストールする。
2. `brew install xcodegen`。
3. `cd iosApp && xcodegen generate`。
4. `MetaranaiIOS.xcodeproj` をXcodeで開く。
5. Signing & CapabilitiesでTeamを選択する。
6. Spotify Dashboardへ `metaranai-login://spotify/callback` を登録し、Bundle IDに `jp.metaranai.ios` を設定する。
7. Simulator buildは `./scripts/build_simulator.sh`。
8. 実機/TestFlightは `docs/14_TESTFLIGHT_BETA.md` を参照。

## データ互換

Android側のSQLiteはバックアップの必須形式ではない。`external_artists`を含む従来JSONをportable sourceとして維持しているため、Androidで書き出したJSONをiOSへ持ってきて復元できる。iOS側でも同じenvelopeでexportする。

## 注意

このZIPからTestFlightへ直接アップロードされるわけではない。Apple DeveloperのTeam/Signing/App Store Connect登録は所有者アカウントで行う必要がある。GitHub Actionsでは署名不要のiOS Simulator buildを回し、SwiftUIを含むcompile regressionを確認する。
