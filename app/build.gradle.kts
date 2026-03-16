import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization") version "2.1.20"
}

android {
    namespace = "com.example.livetv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.livetv"
        minSdk = 23
        targetSdk = 34
        versionCode = 26
        versionName = "1.0.24"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // Load keystore config from keystore.properties (local) or gradle.properties (CI)
            val keystorePropsFile = rootProject.file("keystore.properties")
            if (keystorePropsFile.exists()) {
                println("Loading keystore config from keystore.properties")
                val keystoreProps = Properties()
                keystoreProps.load(keystorePropsFile.inputStream())
                storeFile = rootProject.file(keystoreProps.getProperty("RELEASE_STORE_FILE"))
                storePassword = keystoreProps.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = keystoreProps.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = keystoreProps.getProperty("RELEASE_KEY_PASSWORD")
                println("Keystore file: ${storeFile?.absolutePath}")
                println("Using release keystore for signing")
            } else {
                println("keystore.properties not found, falling back to gradle.properties")
                // Fallback to gradle.properties (used in CI)
                storeFile = rootProject.file(findProperty("RELEASE_STORE_FILE") ?: "app/keystore/release.keystore.jks")
                storePassword = findProperty("RELEASE_STORE_PASSWORD") as String?
                keyAlias = findProperty("RELEASE_KEY_ALIAS") as String?
                keyPassword = findProperty("RELEASE_KEY_PASSWORD") as String?
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            // Use proper release keystore if available, fallback to debug for development
            val keystorePropsFile = rootProject.file("keystore.properties")
            signingConfig = if (keystorePropsFile.exists() || hasProperty("RELEASE_STORE_PASSWORD")) {
                signingConfigs.getByName("release")
            } else {
                println("Warning: Using debug keystore for release build. Set RELEASE_STORE_PASSWORD to use release keystore.")
                signingConfigs.getByName("debug")
            }
            // FIX #26: Enable R8 minification and obfuscation for release builds.
            // proguard-rules.pro keeps WebView JS bridge classes, OkHttp internals, and
            // Jsoup so R8 doesn't strip them.
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        // FIX #26/#18: Ensure BuildConfig is generated (required for BuildConfig.DEBUG guards
        // added to Scraper.kt; explicit in AGP 8+ where it defaults to false).
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.tv:tv-foundation:1.0.0-beta01")
    implementation("androidx.tv:tv-material:1.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.compose.material:material-icons-core:1.7.3")
    implementation("androidx.compose.material:material-icons-extended:1.7.3")
    // Team logo loading from bundled assets
    implementation("io.coil-kt:coil-compose:2.7.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.03"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
