# V0.5.2 → V0.5.3 update

V0.5.3 keeps the same Android identity and data store:

- applicationId: `jp.metaranai.app`
- SharedPreferences: `metaranai`
- Existing profile/history/search/external artist/Genre Lens/Vocal DNA keys are preserved.

## What changes

- Adds Glam Metal, Japanese Metal and Nu Metal Genre Lenses.
- Strengthens Progressive Metal aliases.
- Japanese Metal can match artists whose region/country is Japan even if Last.fm does not expose an exact `japanese metal` tag.
- Strict Genre Lens behavior from V0.5.2 is retained.

## Install

Build V0.5.3 with the same signing key used for the installed V0.5.2 APK, then install as an update (`adb install -r app-release.apk`).
