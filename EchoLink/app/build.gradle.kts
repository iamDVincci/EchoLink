import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val sdkRoot = File(System.getenv("LOCALAPPDATA") ?: "C:\\Users\\DELL\\AppData\\Local", "Android\\Sdk")
val hasNdkAndCmake = File(sdkRoot, "ndk").exists() && File(sdkRoot, "cmake").exists()

android {
    namespace = "com.antigravity.echolink"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.antigravity.echolink"
        minSdk = 29
        targetSdk = 35
        versionCode = 240
        versionName = "2.4.0-tactical"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        if (hasNdkAndCmake) {
            externalNativeBuild {
                cmake {
                    cppFlags += listOf("-std=c++17", "-O3", "-ffast-math", "-Wall")
                    arguments += listOf("-DANDROID_STL=c++_shared")
                }
            }
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        prefab = hasNdkAndCmake
        buildConfig = true
    }

    if (hasNdkAndCmake) {
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)

    // Native C++ Oboe Low-Latency Audio Engine
    implementation(libs.google.oboe)

    // Jetpack Core-Telecom for Bluetooth SCO / BLE Audio & Stem Controls
    implementation(libs.androidx.core.telecom)

    // Multi-Transport P2P (Nearby Connections + Wi-Fi Direct + LAN + BLE)
    implementation(libs.play.services.nearby)

    // Security & Cryptography (ChaCha20-Poly1305 / ECDH)
    implementation(libs.bouncycastle.prov)

    // QR Code Generation & CameraX Scanning for Offline Call Link
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
