# Metaranai V0.4 Architecture

```text
Spotify Top / Recent --------------------┐
                                        v
                                  YOUR METAL DNA
                                        |
HIT / Search ----------------------> Seed Selector
                                        |
                                        v
                              Last.fm getSimilar
                                        |
                              Last.fm getTopTags
                                        |
                               Metal Tag Filter
                                        |
                         Last.fm artist.getInfo
                         listeners / playcount / mbid
                                        |
                           top candidates (<=12)
                                        v
                            MusicBrainz Artist Search
                         MBID / area / country / lifespan
                                        |
                                        v
                              HiddenScoreEngine
                                        |
                                        v
                             External Catalog <=500
                                        |
                                        v
                           Recommendation Engine V4
                     affinity + hidden + novelty + explore
                                        |
                                        v
                              Today's おすすメタル
```

## Responsibility split
- `ExternalDiscoveryClient`: Last.fm discovery and orchestration
- `MusicBrainzClient`: artist normalization and metadata enrichment
- `HiddenScoreEngine`: popularity/rarity score only; never mutates taste profile
- `DiscoveryTagMapper`: Last.fm tags -> MetalVector
- `RecommendationEngine`: DNA affinity + hidden score ranking
- `LocalStore`: persistent external metadata cache

## Failure policy
- Last.fm metadata unavailable: candidate may still survive with neutral hidden inputs.
- MusicBrainz match unavailable: keep Last.fm candidate; metadataConfidence is lower.
- MusicBrainz throttling/temporary error: do not fail the whole discovery batch.
- External metadata never directly changes METAL DNA.

## API safety
MusicBrainz enrichment is capped at 12 artists per discovery run and requests are serialized at >=1.1 seconds apart. This keeps the client below the public API's one-request-per-second application rule.
