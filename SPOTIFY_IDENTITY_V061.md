# Spotify Artist Identity Resolver — v0.6.1

V0.6.1 changes Spotify direct navigation from "best guess" to "verified identity only".

## Direct-link order

1. Read only the V0.6.1 identity cache (`spotify_artist_links_v061`).
2. If a high-confidence MBID (metadataConfidence >= 90) exists, inspect MusicBrainz URL relations and accept only an `open.spotify.com/artist/<id>` relation.
3. Otherwise query Spotify Artist Search and keep only canonical exact-name matches (case/Unicode/whitespace normalized only).
4. Never direct-open partial-name matches.
5. If exactly one exact-name candidate has strong genre evidence, direct-open it.
6. If multiple exact-name candidates exist, direct-open only when exactly one has strong genre evidence.
7. Any ambiguity falls back to the Spotify search-results page.

## Cache safety

The legacy name-only cache `spotify_artist_links_v05` is preserved for backup compatibility but never read by the V0.6.1 resolver. Verified links are stored under an identity key based on MBID when available, otherwise name + country + area + begin date.

## MBID provenance

New discovery data prefers the MBID returned directly by Last.fm. A MusicBrainz name-search-only MBID is treated as lower confidence and cannot by itself authorize a direct Spotify jump.
