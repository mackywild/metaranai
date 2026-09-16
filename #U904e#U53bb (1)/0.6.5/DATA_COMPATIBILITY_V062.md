# Data compatibility contract: V0.4〜V0.6.1 → V0.6.2

V0.6.2 is additive. `applicationId` remains `jp.metaranai.app` and SharedPreferences remains `metaranai`.

Existing profile, five-level ratings, history, external artist database, search history, Genre Lens, Vocal DNA, Last.fm settings, Spotify authentication tokens, and older Spotify-link caches are preserved.

Spotify direct-link cache policy:
- `spotify_artist_links_v05`: preserved, not trusted automatically.
- `spotify_artist_links_v061`: preserved, not trusted automatically by the new resolver.
- `spotify_artist_links_v062`: new verified cache used by V0.6.2.
