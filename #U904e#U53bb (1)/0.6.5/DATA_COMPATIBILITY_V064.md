# Data compatibility contract: V0.4〜V0.6.3 -> V0.6.4

V0.6.4 is additive. `applicationId` remains `jp.metaranai.app` and SharedPreferences remains `metaranai`.

Preserved:
- profile / METAL DNA
- history / five-level reactions
- search_history
- external_artists / Local Metal DB
- genre_lens_v05
- vocal_profile_v05
- Spotify Client ID / access token / refresh token / expiry
- Last.fm API key
- all older Spotify-link caches

Added:
- `spotify_artist_links_v064`: multi-catalog verified Spotify identity cache.

No migration clears existing preferences.
