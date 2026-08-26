from pathlib import Path
root=Path(__file__).resolve().parents[1]
app=(root/'app/build.gradle.kts').read_text()
store=(root/'app/src/main/java/jp/metaranai/app/LocalStore.kt').read_text()
assert 'applicationId = "jp.metaranai.app"' in app
assert 'versionCode = 5' in app
assert 'versionName = "0.5.0"' in app
assert 'getSharedPreferences("metaranai"' in store
for key in ['profile','history','search_history','external_artists','spotify_client_id','spotify_access_token','spotify_refresh_token','spotify_token_expiry','lastfm_api_key']:
    assert f'"{key}"' in store, key
assert '.clear()' not in store
for key in ['genre_lens_v05','vocal_profile_v05','spotify_artist_links_v05']:
    assert key in store
print('UPDATE_COMPAT_OK')
