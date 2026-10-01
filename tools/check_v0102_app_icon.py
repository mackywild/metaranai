from pathlib import Path

root = Path(__file__).resolve().parents[1]
manifest = (root / 'app/src/main/AndroidManifest.xml').read_text()
adaptive_v26 = (root / 'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml').read_text()
adaptive_v33 = (root / 'app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml').read_text()

assert 'android:icon="@mipmap/ic_launcher"' in manifest
assert 'android:roundIcon="@mipmap/ic_launcher_round"' in manifest

for density in ['mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi']:
    base = root / f'app/src/main/res/mipmap-{density}'
    assert (base / 'ic_launcher.webp').exists(), density
    assert (base / 'ic_launcher_round.webp').exists(), density
    assert not (base / 'ic_launcher.png').exists(), density
    assert not (base / 'ic_launcher_round.png').exists(), density

assert (root / 'app/src/main/res/drawable-nodpi/ic_launcher_art.webp').exists()
for adaptive in [adaptive_v26, adaptive_v33]:
    assert '@drawable/ic_launcher_art' in adaptive
    assert '@android:color/transparent' in adaptive

# Do not fall back to the previous themed monochrome mark on Android 13+.
assert '<monochrome' not in adaptive_v33

print('V0102_APP_ICON_OK')
