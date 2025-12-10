// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // build.gradle.kts (Project Level)

    // FIX: Update version from "8.1.0" to "8.9.1"
    id("com.android.application") version "8.9.1" apply false

    // You might also need to bump Kotlin if you are in 2025
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false // or "2.0.0" if available

    id("com.google.gms.google-services") version "4.4.0" apply false

}