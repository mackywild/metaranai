# Data compatibility contract: V0.4 → V0.5

V0.5 intentionally treats V0.4 local data as a compatibility contract.

| Item | V0.4 | V0.5 |
|---|---|---|
| applicationId | jp.metaranai.app | jp.metaranai.app |
| prefs file | metaranai | metaranai |
| profile | same JSON schema | unchanged |
| history | same JSON schema | unchanged |
| search_history | same JSON schema | unchanged |
| external_artists | V0.4 fields | backward-compatible; vocalType optional |
| Spotify credentials/tokens | legacy keys | unchanged |
| Last.fm API key | legacy key | unchanged |
| Genre Lens | none | new key genre_lens_v05 |
| Vocal DNA | none | new key vocal_profile_v05 |
| Spotify direct-link cache | none | new key spotify_artist_links_v05 |

No destructive migration is performed. Missing V0.5 fields use safe defaults.
