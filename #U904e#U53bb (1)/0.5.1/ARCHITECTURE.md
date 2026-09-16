# Architecture v0.5.1

```text
Spotify Top/Recent ───────────────┐
                                  │
5-level feedback ───────────────┐ │
                               ▼ ▼
                         METAL DNA / VOCAL DNA
                               │
                               ├───────────────┐
                               ▼               │
                         Genre Lens            │
                    (manual / weekday)         │
                               │               │
          ┌────────────────────┴───────┐       │
          ▼                            ▼       │
Last.fm similar/tag pool       Last.fm artist.search
          │                            │
          ▼                            ▼
     Top Tags / Metal Filter / listeners / playcount
                         │
                         ▼
                    MusicBrainz
                         │
                         ▼
              Unlimited Local Metal DB
                         │
                         ▼
            Recommendation Engine V5.1
                         │
                         ▼
                TODAY'S おすすメタル
                         │
               5-level feedback loop
```

## Persistence

V0.5.1 intentionally keeps the V0.4/V0.5 SharedPreferences file `metaranai` and all legacy keys.
The external artist JSON remains in `external_artists`, but the previous 500-entry truncation is removed.

## Search

1. Search Local Metal DB immediately.
2. User can expand the same query to Last.fm `artist.search`.
3. Candidate Top Tags are checked for metal relevance.
4. listeners/playcount and MusicBrainz metadata enrich the candidate.
5. Accepted candidates are merged into `external_artists` and remain available offline/local-first on later searches.

## Genre Lens invalidation

A Genre Lens change:

1. writes the existing `genre_lens_v05` key,
2. immediately chooses a new TODAY recommendation using a lens-sensitive seed,
3. excludes the currently displayed artist when another candidate exists,
4. debounces for 550 ms,
5. expands the selected genre pool through Last.fm,
6. merges new candidates into Local Metal DB,
7. recalculates TODAY again.

## Five-level feedback

| Reaction | DNA action | Genre signal |
|---|---|---:|
| 全部好き | strong blend toward artist | +1.40 |
| 普通に刺さる | blend toward artist | +1.00 |
| 何曲か刺さる | weak blend | +0.45 |
| イマイチ | weak move-away | -0.30 |
| 興味なし | strong move-away | -1.00 |

Legacy `MAYBE` and `MISS` are parsed rather than rewritten destructively.
