# V0.6.4 -> V0.7.0 Update

## 更新前に推奨
V0.6.4 > 設定 > DATA SAFETY > 分析データをバックアップ からJSONを書き出す。

## データ互換
V0.7.0は同じapplicationId / SharedPreferences名 / legacy keysを維持する。
通常の同一署名アップデートでは既存データをそのまま読む。

初回V0.7.0起動時:
1. 従来 `external_artists` JSONを読む。
2. SQLite `metaranai_archive.db` が空ならJSONから構築する。
3. JSONは削除しない。

旧JSONを手動復元した場合:
1. SharedPreferencesへ復元。
2. `external_artists` をparse。
3. SQLite ArchiveをそのJSON内容で置換/再構築。
4. ViewModelをreload。

従って、V0.6.4までに書き出したJSONバックアップだけで復元可能。
