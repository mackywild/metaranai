# iOS署名 / Apple Developer Program

公式登録案内: https://developer.apple.com/help/account/membership/program-enrollment

## 個人でApp Store / TestFlight配布する場合
1. 2ファクタ認証を有効にしたApple Accountを用意する。
2. Apple Developer Programへ登録する。
3. App Store Connectでapp recordを作成する。
4. XcodeのSigning & CapabilitiesでTeam / Bundle Identifierを設定する。
5. Archiveを作成してApp Store ConnectへUploadする。

Apple Developer Programは年額99 USD（地域により現地通貨表示）。個人登録ではApp Store販売元に正式な個人名が表示される。

## V0.7.0時点
`iosApp/`は移植starterであり、App Store提出可能な完成版ではない。
V0.8.0でXcode project、Bundle ID、Spotify callback、TestFlight buildを正式化する。
