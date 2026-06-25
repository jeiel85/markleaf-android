// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("com.android.test") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // Compose compiler moved into the Kotlin repo at Kotlin 2.0; its version is
    // the Kotlin version and it is applied via this plugin instead of the old
    // composeOptions { kotlinCompilerExtensionVersion } block.
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("io.github.takahirom.roborazzi") version "1.29.0" apply false
}
