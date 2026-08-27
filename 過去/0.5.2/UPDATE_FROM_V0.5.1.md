# V0.5.1 → V0.5.2 update

V0.5.2 keeps the same Android application identity and data store:

- `applicationId = jp.metaranai.app`
- SharedPreferences file: `metaranai`
- existing keys: `profile`, `history`, `search_history`, `external_artists`, Spotify/Last.fm settings
- V0.5+ keys: `genre_lens_v05`, `vocal_profile_v05`, `spotify_artist_links_v05`

No migration clears or renames existing analysis data.

## Update procedure

1. Export the in-app JSON backup before updating.
2. Build V0.5.2 with the same signing key used for the installed V0.5.1 APK.
3. Install the new APK as an update.
4. Confirm history, METAL DNA, Local Metal DB count, Spotify/Last.fm settings, and Genre Lens settings remain.

If Android reports a signing mismatch, do **not** uninstall until the backup has been verified.

## Genre Lens behavior change

V0.5.1 treated Genre Lens mostly as a ranking signal. V0.5.2 treats it as a hard candidate condition. If the selected genre pool is too small, the app first expands that genre through Last.fm and only then selects TODAY'S recommendation.
