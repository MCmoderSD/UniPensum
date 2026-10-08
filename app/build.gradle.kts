@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// The one version to maintain: the version code follows from it ("1.2.3" becomes 10203000) plus a build number.
// A "-SNAPSHOT" suffix marks a development build, which the release workflow builds but does not publish.
val appVersion = "1.0-SNAPSHOT"

// Google Play accepts every version code once, so every build that is uploaded needs its own. The release workflow
// passes the number of commits since the last release as VERSION_BUILD; a build on this machine uses 0.
val versionBuild: Int = providers.environmentVariable("VERSION_BUILD").orNull
    ?.takeUnless(String::isBlank)
    ?.let {
        requireNotNull(it.toIntOrNull()) { "VERSION_BUILD \"$it\" must be a whole number." }
    } ?: 0

fun versionCodeOf(version: String, build: Int): Int {
    require(Regex("""\d{1,2}(\.\d{1,2}){0,2}(-SNAPSHOT)?""").matches(version)) {
        "appVersion \"$version\" must look like 1.2.3 or 1.2.3-SNAPSHOT, with one to three parts from 0 to 99."
    }
    require(build in 0..999) {
        "VERSION_BUILD $build must be from 0 to 999. Release the version, or raise appVersion, to start counting again."
    }
    val parts = version.substringBefore('-').split('.').map(String::toInt)
    val (major, minor, patch) = parts + listOf(0, 0)
    val base = major * 10_000 + minor * 100 + patch
    require(base > 0) { "appVersion \"$version\" must be above 0." }
    return base * 1_000 + build
}

// Release signing comes from keystore.properties (local, not in git) or from the environment (CI).
// Without either, the release build stays unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) file.inputStream().use(::load)
}

fun signingValue(property: String, variable: String): String? =
    (keystoreProperties.getProperty(property) ?: providers.environmentVariable(variable).orNull)
        ?.takeUnless(String::isBlank)

android {
    namespace = "de.mcmodersd.unipensum"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "de.mcmodersd.unipensum"
        minSdk = 31
        targetSdk = 37
        versionCode = versionCodeOf(appVersion, versionBuild)
        versionName = appVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val store = signingValue("storeFile", "SIGNING_STORE_FILE")
        if (store != null) {
            create("release") {
                storeFile = rootProject.file(store)
                storePassword = signingValue("storePassword", "SIGNING_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "SIGNING_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = true
                // Material is only used for the bottom sheet mechanics, so most of it can go.
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**", "com.google.android.material.**")
            }
        }
    }
    androidResources {
        // The list of languages follows from the values-xx folders, so a new translation is one new strings.xml.
        // The language the texts fall back to is named in src/main/res/resources.properties.
        generateLocaleConfig = true
    }
    lint {
        // A language may lag behind a new text: what is missing falls back to English, so it is no reason to fail.
        warning += "MissingTranslation"
    }
    bundle {
        // The language of the app is set in the Android settings and can differ from the language of the device,
        // so Google Play has to deliver every language, not only the one of the device.
        language {
            enableSplit = false
        }
    }
    compileOptions {
        // Java 17 for records and switch expressions in the domain layer.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.fragment)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.viewpager2)
    implementation(libs.recyclerview)
    implementation(libs.core)
    implementation(libs.customview)

    testImplementation(libs.junit)

    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

// Lint runs on a classpath of its own, with the same outdated libraries as the Android Gradle Plugin.
// The same fixed versions are required there, see the root build file.
configurations.matching { it.name == "androidLintTool" }.configureEach {
    val lintTool = this
    libs.bundles.buildToolFixes.get().forEach {
        lintTool.dependencyConstraints.add(project.dependencies.constraints.create(it))
    }
}
