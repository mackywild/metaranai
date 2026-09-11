# V0.8.0 Test Matrix

## DATA SAFETY — Release Blocker

- [x] V0.6.4 sample JSONをV0.8.0 Swift Coreでdecode
- [x] history件数を保持
- [x] external_artists件数を保持
- [x] legacy MAYBE -> SOME
- [x] legacy MISS -> MEH
- [x] iOS UserDefaultsへrestore
- [x] version 80形式へexport
- [x] history / external_artistsのround-trip一致
- [x] Android側は旧JSON restore後にSQLite mirror再構築経路を維持

## RECOMMENDATION

- [x] Strict Genre LensをSwift Coreへ移植
- [x] Thrash Metal Lensの候補がLens外へ漏れないCore Test
- [x] 5段階評価でMETAL DNAが更新される
- [x] iOS Genre Lens候補不足時のLast.fm補充経路

## SPOTIFY IDENTITY

- [x] 部分一致を完全一致として扱わない
- [x] `Yutaro Abe's ASTRAL WIND` の曲名表記揺れを正規化
- [x] 7曲 + 2 Album相当のfingerprintがstrong evidenceになる
- [x] 同名別Artist用の無関係曲群はscore 0
- [x] Android V0.6.4 verified cacheの互換読込経路
- [x] 新規iOS verified cacheはv080 keyへ分離

## iOS SOURCE / CI

- [x] 全Swift sourceのsyntax parse
- [x] Platform-independent Swift Coreのcompile + executable test
- [x] Info.plist parse
- [x] PrivacyInfo.xcprivacy parse
- [x] XcodeGen YAML parse
- [x] GitHub Actions macOS Simulator build job定義
- [ ] 実Mac/XcodeでSimulator build — GitHub Actions / 所有者Macで実施
- [ ] 実iPhoneでSpotify OAuth callback確認
- [ ] TestFlightへupload
- [ ] Android -> iOS -> Androidの実ファイル往復E2E

未完了項目はApple/Xcode/実機/所有者アカウントが必要なため、V0.8.0 source package内では自動完結しない。
