# Android固定署名

V0.5系で発生したdebug署名差し替わりを再発させないため、配布版は同じkeystoreを継続使用する。

## GitHub Actions Secrets
- `METARANAI_KEYSTORE_B64`
- `METARANAI_KEYSTORE_PASSWORD`
- `METARANAI_KEY_ALIAS`
- `METARANAI_KEY_PASSWORD`

## 原本保護
`.jks`原本はGitリポジトリへcommitしない。
GitHub Secretsだけを唯一の保管場所にせず、暗号化したオフライン/別媒体バックアップを用意する。

## 更新条件
同一 `applicationId=jp.metaranai.app` + 同一署名鍵 + 増加したversionCodeで更新する。
