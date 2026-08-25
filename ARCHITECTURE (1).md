# Architecture v0.5

```text
V0.4 METAL DNA (legacy-compatible)
        |
        +-----------------------------+
        |                             |
        v                             v
Genre Lens                     Vocal DNA (new key)
manual / weekday               male/female/mixed
        |                             |
        v                             |
Last.fm artist.getSimilar             |
Last.fm tag.getTopArtists <------------+
        |
artist.getTopTags / getInfo
        |
MusicBrainz normalize
        |
External catalog (max 500)
        |
Recommendation V5
  DNA affinity = strongest
  Genre Lens = optional 20%
  Hidden / novelty / exploration
        |
TODAY'S おすすメタル
        |
Spotify GET /search
  exact artist name match
        |
external_urls.spotify
  | direct match -> Artist page
  | no match     -> Spotify search fallback
```

## Persistence rule

Legacy keys are a compatibility contract. Never rename or clear them during V0.x upgrades.

New V0.5 keys:
- `genre_lens_v05`
- `vocal_profile_v05`
- `spotify_artist_links_v05`

## Recommendation weights

Genre Lens OFF:
- DNA affinity 50
- Hidden 20
- Novelty 15
- Exploration 10
- Discovery 5

Genre Lens ON:
- DNA affinity 45
- Genre Lens 20
- Hidden 15
- Novelty 10
- Exploration 5
- Discovery 5

This guarantees the user remains the same listener even when intentionally exploring another genre.
