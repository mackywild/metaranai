plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val signingStorePath = System.getenv("METARANAI_KEYSTORE_PATH")
val signingStorePassword = System.getenv("METARANAI_KEYSTORE_PASSWORD")
val signingKeyAlias = System.getenv("METARANAI_KEY_ALIAS")
val signingKeyPassword = System.getenv("METARANAI_KEY_PASSWORD")
val hasReleaseSigning = listOf(signingStorePath, signingStorePassword, signingKeyAlias, signingKeyPassword).all { !it.isNullOrBlank() }

android {
    namespace = "jp.metaranai.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "jp.metaranai.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 17
        versionName = "0.9.0"
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(signingStorePath!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    defaultConfig {
        buildConfigField("String", "FIREBASE_API_KEY", "\"${System.getenv("FIREBASE_API_KEY") ?: ""}\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"${System.getenv("FIREBASE_APP_ID") ?: ""}\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${System.getenv("FIREBASE_PROJECT_ID") ?: ""}\"")
        buildConfigField("String", "FIREBASE_STORAGE_BUCKET", "\"${System.getenv("FIREBASE_STORAGE_BUCKET") ?: ""}\"")
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"${System.getenv("SPOTIFY_CLIENT_ID") ?: ""}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${System.getenv("GOOGLE_WEB_CLIENT_ID") ?: ""}\"")
        buildConfigField("String", "LASTFM_API_KEY", "\"${System.getenv("LASTFM_API_KEY") ?: ""}\"")
        buildConfigField("String", "FACEBOOK_APP_ID", "\"${System.getenv("FACEBOOK_APP_ID") ?: ""}\"")
        buildConfigField("String", "FACEBOOK_CLIENT_TOKEN", "\"${System.getenv("FACEBOOK_CLIENT_TOKEN") ?: ""}\"")
        resValue("string", "facebook_app_id", System.getenv("FACEBOOK_APP_ID") ?: "1234567890")
        resValue("string", "facebook_client_token", System.getenv("FACEBOOK_CLIENT_TOKEN") ?: "disabled")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.04.01"))
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
    implementation("com.facebook.android:facebook-login:18.3.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
