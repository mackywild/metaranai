# Spotify Identity Resolver v0.6.4

v0.6.4 fixes the real regression reproduced with `Yutaro Abe's ASTRAL WIND`.

## Why v0.6.3 could still fall back to search

v0.6.3 depended on Last.fm top-track availability for the new track fingerprint. For underground artists, Last.fm may have an artist/tags/listener record but an empty or sparse Top Tracks response. Spotify genres are also deprecated and often empty, so an exact Spotify artist match could still lack enough independent identity evidence.

The previous regression script only used hard-coded track lists; it did not model the `Last.fm Top Tracks = empty` path. v0.6.4 adds that path to regression coverage.

## v0.6.4 resolver

1. Trusted MusicBrainz MBID relation remains highest priority.
2. Spotify Artist Search now uses both a plain-name query (matching the user-facing Spotify search behavior) and an artist-field query.
3. Only canonical exact-name Spotify artists remain candidates. Partial-name direct navigation stays forbidden.
4. Candidate Spotify catalogs are read by Spotify Artist ID (Artist Albums + Album Tracks, plus Track Search).
5. Independent identity catalogs are:
   - Last.fm Top Tracks
   - Apple/iTunes public Search catalog (no API key), grouped by Apple `artistId` so same-name artists are not merged.
6. Track titles and album titles are compared after conservative normalization.
7. A unique exact-name candidate is direct only with a strong independent fingerprint (2+ tracks, 2+ albums, or track+album), or existing high-confidence metadata evidence.
8. Multiple same-name Spotify candidates need one clearly stronger catalog fingerprint; otherwise the app falls back to Spotify Search.

## Cache

v0.6.4 uses `spotify_artist_links_v064`. Older Spotify link caches stay stored for compatibility but are not auto-read by the v0.6.4 resolver.
