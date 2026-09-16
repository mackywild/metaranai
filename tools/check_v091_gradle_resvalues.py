from pathlib import Path

gradle = Path("app/build.gradle.kts").read_text()
workflow = Path(".github/workflows/android.yml").read_text()

assert 'versionCode = 21' in gradle
assert 'versionName = "0.9.3"' in gradle
assert 'resValue("string", "facebook_app_id"' in gradle
assert 'resValue("string", "facebook_client_token"' in gradle
assert 'resValues = true' in gradle, "AGP requires buildFeatures.resValues when defaultConfig.resValue() is used"
assert 'metaranai-v0.9.3-apk' in workflow
print("V091_GRADLE_RESVALUES_OK")
