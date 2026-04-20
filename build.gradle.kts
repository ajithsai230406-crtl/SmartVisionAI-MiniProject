// Top-level build file
plugins {
    alias(libs.plugins.android.application)  apply false
    alias(libs.plugins.android.library)      apply false
    alias(libs.plugins.kotlin.android)       apply false
    alias(libs.plugins.hilt)                 apply false
    alias(libs.plugins.kotlin.kapt)          apply false
    id("com.google.gms.google-services") version "4.4.4" apply falsex
}
