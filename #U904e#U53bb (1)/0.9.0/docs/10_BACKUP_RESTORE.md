# JSONバックアップ / 復元 — 最重要

## 結論
V0.7.0でも、これまでの `metaranai-backup-v0.6.x.json` をそのまま復元できる設計を維持する。

## 互換フォーマット
```json
{
  "format": "metaranai-backup",
  "version": 64,
  "preferences": {
    "profile": "...",
    "history": "...",
    "external_artists": "..."
  }
}
```

`version`が64であることを要求しない。V0.7.0のImporterは `format=metaranai-backup` と `preferences` を読み、既存keysを復元する。

## V0.7.0 DB化後の流れ
旧JSONを選択
→ SharedPreferencesを復元
→ `external_artists` JSONをparse
→ SQLite `metaranai_archive.db`を全再構築
→ ViewModelを再読込

つまりSQLiteファイル自体をバックアップしていなくても、従来JSONだけでLocal Metal DBを復元できる。

## 守るkeys
少なくとも以下を削除/renameしない。
- `profile`
- `history`
- `search_history`
- `external_artists`
- `spotify_client_id`
- `spotify_access_token`
- `spotify_refresh_token`
- `spotify_token_expiry`
- `lastfm_api_key`
- `vocal_profile_v05`
- `genre_lens_v05`
- 過去のSpotify identity caches

## 大型更新前
1. V0.6.x側で最新JSONバックアップを書き出す。
2. ファイルサイズが0ではないことを確認する。
3. V0.7.0インストール後にArchive件数と評価履歴を確認する。
4. 問題があれば新規評価を始める前に旧JSONから再復元する。
