plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ai.spatialreal.examples.pipecatclient"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.spatialreal.examples.pipecatclient"
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
    // The Pipecat route of the SpatialReal SDK. It uses protobuf-javalite;
    // do not add the full protobuf-java to this app.
    implementation(libs.spatialreal.android.pipecat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    // Fetches the session token from your server
    implementation(libs.okhttp)
}
