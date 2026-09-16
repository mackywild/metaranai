# Update from v0.6.1 to v0.6.2

- applicationId remains `jp.metaranai.app`.
- SharedPreferences remains `metaranai`.
- Existing profile/history/Local Metal DB/Genre Lens/Vocal DNA/Spotify authentication data are preserved.
- The old Spotify direct-link caches are preserved but V0.6.2 uses a new verified cache: `spotify_artist_links_v062`.
- Partial-name matches never open directly.
- Exact-name direct navigation is restored when MusicBrainz metadata or usable Spotify genre evidence confirms the identity.
