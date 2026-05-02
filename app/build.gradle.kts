import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

// ── Resolve TFLite / LiteRT duplicate class conflict ────────────────────────
configurations.all {
    exclude(group = "org.tensorflow",         module = "tensorflow-lite-api")
    exclude(group = "com.google.android.gms", module = "play-services-tflite-support")
    resolutionStrategy {
        force("com.google.android.gms:play-services-tflite-java:16.4.0")
    }
}

android {
    namespace  = "com.smartvision.ai"
    compileSdk = 35

    defaultConfig {
        applicationId    = "com.smartvision.ai"
        minSdk           = 26
        targetSdk        = 35
        versionCode      = 1
        versionName      = "1.0.0"
        multiDexEnabled  = true

        buildConfigField("String", "GEMINI_API_KEY",
            "\"${localProps.getProperty("GEMINI_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    // ── Fix duplicate .so native libs ────────────────────────────────────
    packaging {
        jniLibs {
            pickFirsts += setOf(
                "lib/arm64-v8a/libtensorflowlite_jni.so",
                "lib/arm64-v8a/libtensorflowlite_gpu_jni.so",
                "lib/x86_64/libtensorflowlite_jni.so",
                "lib/x86_64/libtensorflowlite_gpu_jni.so",
                "lib/armeabi-v7a/libtensorflowlite_jni.so",
                "lib/armeabi-v7a/libtensorflowlite_gpu_jni.so",
                "lib/x86/libtensorflowlite_jni.so",
                "lib/x86/libtensorflowlite_gpu_jni.so"
            )
        }
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE.md"
            )
        }
    }
}

dependencies {
    // ── Compose BOM ──────────────────────────────────────────────────────
    val bom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── AndroidX Core ────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.multidex:multidex:2.0.1")

    // ── Navigation ───────────────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // ── CameraX ──────────────────────────────────────────────────────────
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.2.0")

    // ── ML Kit (bundles its own TFLite — no extra TFLite needed) ─────────
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:object-detection:17.0.2")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("com.google.mlkit:language-id:17.0.6")

    // ── Gemini — strip conflicting TFLite transitive deps ────────────────
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0") {
        exclude(group = "org.tensorflow")
        exclude(group = "com.google.android.gms", module = "play-services-tflite-java")
        exclude(group = "com.google.android.gms", module = "play-services-tflite-gpu")
        exclude(group = "com.google.android.gms", module = "play-services-tflite-support")
    }

    // ── Firebase ─────────────────────────────────────────────────────────
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // ── Hilt (KSP) ───────────────────────────────────────────────────────
    implementation("com.google.dagger:hilt-android:2.55")
    ksp("com.google.dagger:hilt-compiler:2.55")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ── Image Loading ─────────────────────────────────────────────────────
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ── Accompanist ──────────────────────────────────────────────────────
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")

    // ── DataStore ────────────────────────────────────────────────────────
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ── Coroutines ───────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // ── Testing ──────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
