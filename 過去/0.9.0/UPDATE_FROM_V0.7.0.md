# V0.7.0 -> V0.8.0 Update

V0.8.0はAndroidの既存データ互換を維持しつつ、iOS Betaを追加する更新です。

## Android

- `applicationId = jp.metaranai.app` を維持
- SharedPreferences `metaranai` を維持
- 既存 `profile / history / external_artists / genre_lens_v05 / vocal_profile_v05` 等を維持
- `versionCode = 16`, `versionName = 0.8.0`
- 同一署名APKなら通常の上書き更新を行う

## 更新前の推奨バックアップ

設定画面から従来のJSONバックアップを書き出してください。V0.8.0でもこの形式を正式なPortable Backupとして維持します。

```text
format = metaranai-backup
preferences = existing SharedPreferences data
```

V0.8.0は古いbackup `version` を理由に拒否しません。

## iOSへデータを持っていく場合

1. AndroidでJSONバックアップを書き出す。
2. iPhoneのファイルAppへJSONを渡す。
3. iOS版「設定」->「DATA SAFETY」->「JSONバックアップを復元」。
4. 評価履歴、Local Metal DB、Genre Lens、METAL DNAを確認する。
5. Spotifyは必要に応じてiOS側で再接続する。

## 戻す場合

iOS V0.8.0も同じ `metaranai-backup` envelopeでJSONを書き出します。Android側Importerはversion固定判定をしないため、同じpreferences keyを復元できます。

## 回帰確認

GitHub ActionsではAndroid互換チェックに加え、Swift portable core testとiOS Simulator buildを実行します。`Yutaro Abe's ASTRAL WIND` のSpotify本人判定用Catalog FingerprintもCore Testに固定しています。
