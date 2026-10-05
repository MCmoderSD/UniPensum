import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// The one version to maintain: the version code follows from it ("1.2.3" becomes 10203).
// A "-SNAPSHOT" suffix marks a development build, which the release workflow builds but does not publish.
val appVersion = "1.0-SNAPSHOT"

fun versionCodeOf(version: String): Int {
    require(Regex("""\d{1,2}(\.\d{1,2}){0,2}(-SNAPSHOT)?""").matches(version)) {
        "appVersion \"$version\" must look like 1.2.3 or 1.2.3-SNAPSHOT, with one to three parts from 0 to 99."
    }
    val parts = version.substringBefore('-').split('.').map(String::toInt)
    val (major, minor, patch) = parts + listOf(0, 0)
    val code = major * 10_000 + minor * 100 + patch
    require(code > 0) { "appVersion \"$version\" must be above 0." }
    return code
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
        versionCode = versionCodeOf(appVersion)
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
