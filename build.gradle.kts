plugins {
    kotlin("kapt") version "2.0.0"
    id("com.android.application") version "8.11.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin) apply false
}