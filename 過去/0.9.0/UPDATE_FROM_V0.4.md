# V0.4.0 → V0.5.0 更新手順（分析データ保護版）

## 重要
V0.5.0 は以下を維持しています。

- applicationId: `jp.metaranai.app`
- SharedPreferences名: `metaranai`
- 既存キー: `profile`, `history`, `search_history`, `external_artists`, Spotify/Last.fm設定一式
- V0.5の新設定は `*_v05` の新規キーへ保存
- `prefs.clear()` や履歴初期化処理はありません
- versionCode: 5 / versionName: 0.5.0

したがって **同じ署名鍵で署名されたAPKなら通常の上書き更新でV0.4の分析データをそのまま利用できます。**

## ただし：GitHub Actions の debug APK の署名について
GitHub-hosted runnerで毎回自動生成されるdebug keystoreは、以前インストールしたAPKと署名が一致しない可能性があります。
署名が違うAPKはAndroidが上書き更新を拒否します。拒否されても、その時点では既存アプリ/データは消えません。

50件の分析履歴を絶対に守るため、更新前に以下のADBバックアップを実施してください。

## 1. V0.4データをバックアップ
USBデバッグを有効にし、PCから次を実行します。

### macOS / Linux / Termux + adb
```bash
adb exec-out run-as jp.metaranai.app cat shared_prefs/metaranai.xml > metaranai-v0.4-data.xml
```

### Windows cmd
```bat
adb exec-out run-as jp.metaranai.app cat shared_prefs/metaranai.xml > metaranai-v0.4-data.xml
```

ファイルサイズが0バイトでないことを確認してください。

> V0.4はdebug APK想定なので `run-as` が利用できます。バックアップにはSpotify token/API key等も含まれるため公開しないでください。

## 2. まず上書き更新を試す
```bash
adb install -r app-debug.apk
```

成功した場合：
- V0.4の履歴・DNA・外部Artist cacheはそのまま残ります。
- アプリを開き、発掘件数が以前の値のままか確認してください。

`INSTALL_FAILED_UPDATE_INCOMPATIBLE` 等の署名エラーの場合：
- **既存アプリをすぐ削除しないでください。**
- 手順1のバックアップがあることを確認してから、一度だけ再インストール移行を行います。

## 3. 署名が違った場合の一度だけの移行
1. V0.4バックアップが存在することを確認
2. V0.4をアンインストール
3. V0.5 APKをインストール
4. V0.5を一度起動して終了
5. 下記でSharedPreferencesを復元

```bash
adb push metaranai-v0.4-data.xml /data/local/tmp/metaranai-v0.4-data.xml
adb shell am force-stop jp.metaranai.app
adb shell run-as jp.metaranai.app mkdir -p shared_prefs
adb shell run-as jp.metaranai.app sh -c 'cat /data/local/tmp/metaranai-v0.4-data.xml > shared_prefs/metaranai.xml'
adb shell am force-stop jp.metaranai.app
```

その後アプリを起動します。

## 4. 更新後確認
- 発掘件数が50件前後の既存値を維持している
- METAL DNAがV0.4時点の値を維持している
- 検索履歴が残っている
- External cacheが残っている
- Spotify Client ID / Last.fm API Keyが残っている

## 5. V0.5以降
設定 → `DATA SAFETY` からアプリ内JSONバックアップ/復元が利用できます。
V0.6以降へ進む前にJSONを1本保存しておくことを推奨します。
