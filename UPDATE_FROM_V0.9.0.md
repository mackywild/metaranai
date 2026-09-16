# Update from v0.9.0 to v0.9.1

## Android build hotfix

V0.9.0 introduced Facebook resource values through `defaultConfig.resValue(...)`, but Android Gradle Plugin 9.x requires the `resValues` build feature to be enabled explicitly.

V0.9.1 adds:

```kotlin
buildFeatures {
    compose = true
    buildConfig = true
    resValues = true
}
```

No application ID, SharedPreferences name, backup format, account data, METAL DNA, ratings, Local Metal DB, or Spotify identity data is reset by this update.
