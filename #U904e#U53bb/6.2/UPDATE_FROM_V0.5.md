# V0.5.0 → V0.5.1 update

V0.5.1 keeps:

- applicationId `jp.metaranai.app`
- SharedPreferences file `metaranai`
- Metal DNA `profile`
- discovery `history`
- search history
- external artist cache
- Spotify credentials/tokens
- Last.fm API key
- Genre Lens settings
- Vocal DNA

If the APK is signed with the same key as the installed V0.5 APK, install it as a normal update. Do not uninstall first.

Before updating, the in-app JSON backup is still recommended.

After update verify:

1. existing discovery history is visible;
2. Metal DNA is unchanged before a new rating;
3. old HIT/MAYBE/MISS records display as the compatible V0.5.1 level;
4. Local Metal DB count is retained;
5. Spotify / Last.fm settings are retained;
6. Genre Lens settings are retained.
