# App Store / TestFlight公開手順

App Store Connect: https://appstoreconnect.apple.com/
公式workflow: https://developer.apple.com/help/app-store-connect/get-started/app-store-connect-workflow

1. Apple Developer Program登録を完了する。
2. App Store Connect > Appsでapp recordを作る。
3. Xcodeで署名済みbuildをArchive/Uploadする。
4. TestFlightへbuildを追加する。
5. Internal testingで実機確認する。
6. External testersを使う場合はTest Informationを入力し、必要なBeta App Reviewを通す。
7. Store metadata / privacy / screenshotsを完成させる。
8. Buildをversionへ紐付け、Add for Review > Submit for Review。

公式TestFlight: https://developer.apple.com/help/app-store-connect/test-a-beta-version/testflight-overview/
公式提出: https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/submit-an-app
