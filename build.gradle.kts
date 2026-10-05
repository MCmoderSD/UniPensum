// Top-level build file where you can add configuration options common to all sub-projects/modules.

// The Android Gradle Plugin brings its own libraries, some in versions with known security issues. These
// constraints require fixed versions on the build classpath. A constraint only raises a version, so it does
// nothing once the plugin asks for something newer itself. The versions are in gradle/libs.versions.toml.
buildscript {
    dependencies {
        constraints {
            libs.bundles.buildToolFixes.get().forEach { classpath(it) }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}
