# Data compatibility contract: V0.4〜V0.6.2 → V0.6.3

V0.6.3 is additive. Existing analysis data remains the compatibility contract.

| Item | Existing data | V0.6.3 behavior |
|---|---|---|
| applicationId | `jp.metaranai.app` | unchanged |
| prefs file | `metaranai` | unchanged |
| `profile` | existing Metal DNA JSON | unchanged |
| `history` | V0.4〜V0.6.2 records | loaded and preserved |
| old `HIT` | 刺さった | maps to `普通に刺さる` |
| old `MAYBE` | 微妙 | maps to `何曲か刺さる` |
| old `MISS` | 刺さらない | maps to `イマイチ` |
| `search_history` | existing history | preserved / no truncation |
| `external_artists` | existing external cache | preserved / no artificial cap |
| Spotify credentials/tokens | legacy keys | unchanged |
| Last.fm API key | legacy key | unchanged |
| Genre Lens | `genre_lens_v05` | unchanged |
| Vocal DNA | `vocal_profile_v05` | unchanged |
| Old Spotify link caches | `spotify_artist_links_v05/v061/v062` | preserved, not auto-trusted |
| New Spotify verified cache | none | `spotify_artist_links_v063` added |

Personal Metal Archive, Deep Dive, Strict Genre Lens and the five-level rating history remain unchanged.
V0.6.3 only changes Spotify identity resolution and its verified-link cache.

No `SharedPreferences.clear()` or destructive migration is performed.
