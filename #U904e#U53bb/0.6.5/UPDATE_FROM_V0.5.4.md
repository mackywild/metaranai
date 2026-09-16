# Update from v0.5.4 to v0.6.0

V0.6.0は `applicationId=jp.metaranai.app` と SharedPreferences `metaranai` を維持しています。

固定署名を既に設定済みの場合:

```bash
adb install -r app-release.apk
```

で既存のMETAL DNA / Local Metal DB / 評価履歴 / Genre Lens / Spotify / Last.fm設定を保持したまま更新できます。

署名が異なるAPKから移行する場合は、従来どおり先にバックアップを取得してください。
