# Data Compatibility Contract — V0.9+

These are release invariants, not implementation suggestions.

- Android applicationId stays `jp.metaranai.app` for in-place updates.
- Android SharedPreferences file stays `metaranai`.
- Portable backup envelope stays `format = metaranai-backup` and older version numbers are accepted.
- Existing keys (`profile`, `history`, `search_history`, `external_artists`, Genre/Vocal settings and verified Spotify caches) are not cleared during account registration.
- `external_artists` from an imported legacy JSON remains sufficient to rebuild the SQLite mirror.
- Signing in never silently chooses local over cloud or cloud over local when both contain personal data.
- Cloud sync strips reusable Spotify access/refresh tokens; manual JSON backup remains the independent recovery path.
- New-user seed is neutral. Existing users' stored profiles are never replaced by that seed.
