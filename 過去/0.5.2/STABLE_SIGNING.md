# 固定署名を設定する（V0.5.1以降の上書き更新用）

Androidは同じpackage名でも署名鍵が違うAPKを上書きできません。
V0.5.1を正式な基準版にするなら、ここで一度だけ固定署名鍵を作り、以後V0.6/V1.0でも同じ鍵を使います。

## 1. keystore作成

```bash
keytool -genkeypair -v \
  -keystore metaranai-release.jks \
  -alias metaranai \
  -keyalg RSA -keysize 4096 -validity 10000
```

この `metaranai-release.jks` はGitへcommitしません。紛失すると同じアプリとして更新できなくなるため安全な場所へ保管してください。

## 2. Base64化

macOS / Linux / Termux:

```bash
base64 -w 0 metaranai-release.jks > metaranai-keystore-base64.txt
```

macOSで `-w` がない場合:

```bash
base64 < metaranai-release.jks | tr -d '\n' > metaranai-keystore-base64.txt
```

## 3. GitHub Repository Secrets

Repository → Settings → Secrets and variables → Actions に以下を登録します。

- `METARANAI_KEYSTORE_B64` : 上記Base64文字列
- `METARANAI_KEYSTORE_PASSWORD` : keystore password
- `METARANAI_KEY_ALIAS` : `metaranai`
- `METARANAI_KEY_PASSWORD` : key password

## 4. Actions

Secretsが設定されている場合は `assembleRelease` で固定署名APKを生成します。
未設定なら従来どおりdebug APKを生成します。

**V0.5.1を固定署名release版として一度インストールした後は、同じSecretsを維持する限りV0.6以降は通常の上書き更新ができます。**
