plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ai.spatialreal.examples.hostclient"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.spatialreal.examples.hostclient"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        ndk {
            // The SpatialReal SDK runs on 64-bit ARM
            abiFilters += "arm64-v8a"
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
    }
}

dependencies {
    implementation(libs.spatialreal.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    // Connects to your Host mode server
    implementation(libs.okhttp)
}
