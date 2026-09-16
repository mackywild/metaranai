# Data Compatibility — V0.7.0

## Non-negotiable rule
The portable backup contract remains:

- `format = metaranai-backup`
- top-level `preferences` object
- legacy preference keys remain unchanged

V0.7.0 adds SQLite only as a local mirror for the External Artist archive. It does not replace the JSON backup contract.

## Upgrade without manual restore
Same package + same signature upgrade:
1. Existing `metaranai` SharedPreferences remains installed.
2. V0.7.0 reads `external_artists`.
3. If `metaranai_archive.db` is empty, it is bootstrapped from that JSON.
4. Existing profile/history/Genre Lens/Spotify/Last.fm settings remain in place.

## Restore from a V0.5-V0.6.x JSON backup
1. Importer validates only `format = metaranai-backup` and the presence of `preferences`.
2. It does not reject the backup just because the numeric `version` is older.
3. Preferences are committed synchronously.
4. Restored `external_artists` is parsed.
5. SQLite archive mirror is replaced with those restored artists.
6. ViewModel reloads all user state.

## Legacy V0.4 raw SharedPreferences backup
The old adb/XML migration helpers remain under `tools/backup_v04_data.*` and `tools/restore_v04_data.sh`.
Once restored into the normal `metaranai` SharedPreferences, V0.7.0 can bootstrap its SQLite mirror in the same way.
