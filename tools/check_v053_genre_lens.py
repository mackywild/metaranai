from pathlib import Path
root = Path(__file__).resolve().parents[1]
genre = (root/'app/src/main/java/jp/metaranai/app/GenreLens.kt').read_text()
tags = (root/'app/src/main/java/jp/metaranai/app/DiscoveryTagMapper.kt').read_text()
build = (root/'app/build.gradle.kts').read_text()
ext = (root/'app/src/main/java/jp/metaranai/app/ExternalDiscoveryClient.kt').read_text()
ui = (root/'app/src/main/java/jp/metaranai/app/MainActivity.kt').read_text()

assert genre.count('Lens(\"') == 18, genre.count('Lens(\"')
for name in ['Glam Metal', 'Japanese Metal', 'Progressive Metal', 'Nu Metal']:
    assert f'Lens("{name}"' in genre, name
for alias in ['hair metal', 'sleaze metal', 'j-metal', 'japanese heavy metal', 'progressive death metal', 'progressive power metal', 'nü metal', 'rap metal']:
    assert alias in genre or alias in tags, alias
assert 'locationText.contains("japan")' in genre
assert 'lens.name == "Japanese Metal"' in genre
assert 'versionCode = 10' in build
assert 'versionName = "0.6.0"' in build
assert 'Metaranai-Android/0.6.0' in ext
assert 'v0.6.0' in ui
print('V053_GENRE_LENS_EXPANSION_OK')
