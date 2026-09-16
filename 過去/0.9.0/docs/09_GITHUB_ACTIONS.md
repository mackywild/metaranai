# GitHub Actions

Android workflow: `.github/workflows/android.yml`

- main push / workflow_dispatchでbuild
- 固定署名Secretsがある: release APK
- Secretsがない: debug APK fallback

V0.7.0からbuild前に以下のregression checkを実行する:
- update/data compatibility
- Strict Genre Lens
- 未評価候補の自動補充
- Personal Metal Archive
- Yutaro Abe's ASTRAL WIND Spotify identity fixture
- V0.6.x JSON backup -> V0.7.0 SQLite再構築
- V0.7.0 UI / adaptive icon / release foundation

iOSはmacOS runner導入後にbuild/testを追加する。
