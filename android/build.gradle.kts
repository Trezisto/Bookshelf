// Top-level build file. AGP's built-in Kotlin support compiles the app module;
// the Kotlin Gradle plugin version comes from the Compose/serialization compiler plugins below.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
