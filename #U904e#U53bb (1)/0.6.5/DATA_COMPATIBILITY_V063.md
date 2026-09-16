# Data compatibility contract: V0.4〜V0.6.2 -> V0.6.3

V0.6.3 is additive. `applicationId` remains `jp.metaranai.app` and SharedPreferences remains `metaranai`.

Preserved:
- profile / history / search_history / external_artists
- spotify_client_id / tokens / Last.fm API key
- genre_lens_v05 / vocal_profile_v05
- all legacy Spotify cache keys (backup compatibility only)

New:
- `spotify_artist_links_v063`: Track Fingerprint / verified identity cache used by V0.6.3.

V0.6.2 and older Spotify direct-link caches are preserved but are not automatically trusted by the V0.6.3 resolver.
