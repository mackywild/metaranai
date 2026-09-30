from pathlib import Path

root = Path(__file__).resolve().parents[1]
build = (root / 'app/build.gradle.kts').read_text()
workflow = (root / '.github/workflows/android.yml').read_text()

# Current release metadata belongs in one release-specific check.
# Historical feature regression tests must not pin these values.
assert 'versionCode = 28' in build
assert 'versionName = "0.10.1"' in build

for artifact in [
    'metaranai-v0.10.1-apk',
    'metaranai-v0.10.1-aab',
    'metaranai-v0.10.1-aab-unsigned',
]:
    assert artifact in workflow, artifact

print('RELEASE_METADATA_OK')
