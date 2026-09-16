# Spotify Artist Identity Resolver — v0.6.2

V0.6.2 keeps the V0.6.1 safety rule that partial names never open an Artist page directly, while reducing unnecessary Spotify-search fallbacks.

Direct navigation priority:

1. High-confidence Local DB MBID -> MusicBrainz Spotify URL relation.
2. Exact artist name + Local DB country/area/begin-year -> MusicBrainz identity recovery -> Spotify URL relation.
3. If MusicBrainz has no Spotify relation, one unique Spotify exact-name result may open directly when the MusicBrainz identity is strong.
4. One unique Spotify exact-name result may also open directly when Spotify genre evidence is usable. Generic metal evidence is accepted only when there is exactly one exact-name result.
5. Multiple same-name Spotify results still require one and only one candidate with strong genre evidence. Otherwise the app opens Spotify search.

V0.5 (`spotify_artist_links_v05`) and V0.6.1 (`spotify_artist_links_v061`) caches remain in SharedPreferences for backup compatibility. V0.6.2 reads only `spotify_artist_links_v062`, preventing stale identity decisions from being reused automatically.
