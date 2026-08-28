# Data compatibility contract: V0.4〜V0.5.4 → V0.6.0

V0.6.0 is an additive update. Existing analysis data remains the compatibility contract.

| Item | Existing data | V0.6.0 behavior |
|---|---|---|
| applicationId | `jp.metaranai.app` | unchanged |
| prefs file | `metaranai` | unchanged |
| `profile` | existing Metal DNA JSON | unchanged |
| `history` | V0.4〜V0.5.4 records | loaded and preserved |
| old `HIT` | 刺さった | maps to `普通に刺さる` |
| old `MAYBE` | 微妙 | maps to `何曲か刺さる` |
| old `MISS` | 刺さらない | maps to `イマイチ` |
| `search_history` | existing history | preserved / no truncation |
| `external_artists` | existing external cache | preserved / no artificial cap |
| Spotify credentials/tokens | legacy keys | unchanged |
| Last.fm API key | legacy key | unchanged |
| Genre Lens | `genre_lens_v05` | unchanged |
| Vocal DNA | `vocal_profile_v05` | unchanged |
| Spotify direct-link cache | `spotify_artist_links_v05` | unchanged |

Personal Metal Archive and Deep Dive reuse the existing data structures. Deep Dive only merges additional Artist records into `external_artists`; it does not replace the archive.

V0.6.0 never infers the two extreme ratings from legacy data. A legacy HIT is **not** promoted to `全部好き`, and a legacy MISS is **not** demoted to `興味なし`.

No `SharedPreferences.clear()` or destructive migration is performed.
