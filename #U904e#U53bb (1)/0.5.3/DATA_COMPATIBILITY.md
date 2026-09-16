# Data compatibility contract: V0.4 / V0.5 / V0.5.1 / V0.5.2 → V0.5.3

V0.5.3 is an additive update. Existing analysis data remains the compatibility contract.

| Item | Existing data | V0.5.3 behavior |
|---|---|---|
| applicationId | `jp.metaranai.app` | unchanged |
| prefs file | `metaranai` | unchanged |
| `profile` | existing Metal DNA JSON | unchanged |
| `history` | V0.4/V0.5 records | loaded and preserved |
| old `HIT` | 刺さった | maps to `普通に刺さる` |
| old `MAYBE` | 微妙 | maps to `何曲か刺さる` |
| old `MISS` | 刺さらない | maps to `イマイチ` |
| `search_history` | existing history | preserved; V0.5.3 does not truncate new saves |
| `external_artists` | existing external cache | preserved; 500 artist cap removed |
| Spotify credentials/tokens | legacy keys | unchanged |
| Last.fm API key | legacy key | unchanged |
| Genre Lens | `genre_lens_v05` | unchanged |
| Vocal DNA | `vocal_profile_v05` | unchanged |
| Spotify direct-link cache | `spotify_artist_links_v05` | unchanged |

V0.5.3 never infers the two new extreme ratings from old data. A legacy HIT is **not** promoted to `全部好き`, and a legacy MISS is **not** demoted to `興味なし`.

No `SharedPreferences.clear()` or destructive migration is performed.
