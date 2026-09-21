plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ovalvoi.andropods"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ovalvoi.andropods"
        minSdk = 31
        // Pinned to 35 deliberately. With targetSdk = 36 on an Android 17
        // (API 37) Pixel 7, BLE scan results are silently suppressed: the scan
        // registers, stays active, never calls onScanFailed, and delivers zero
        // results. Verified side by side -- identical code at 35 receives
        // ~1400 advertisements in 25s, at 36 receives exactly 0.
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing comes from the environment, never from the repo. CI
    // sets these from repository secrets (see .github/workflows/android.yml);
    // locally, export them or leave them unset for an unsigned release build.
    val releaseKeystore = System.getenv("ANDROPODS_KEYSTORE_FILE")
        ?.let(::file)
        ?.takeIf { it.exists() }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("ANDROPODS_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROPODS_KEY_ALIAS")
                keyPassword = System.getenv("ANDROPODS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        // For BuildConfig.DEBUG, which gates the Settings preview button.
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    testImplementation(libs.kotlinx.coroutines.test)
}
