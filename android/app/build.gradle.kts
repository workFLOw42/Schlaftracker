import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.room)
}

// Release-Signing: Werte aus local.properties laden
val releaseProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

// Versionierung: version.properties enthält die Version des nächsten Releases
val versionPropsFile = file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) versionPropsFile.inputStream().use { load(it) }
}
val appVersionCode = versionProps.getProperty("VERSION_CODE", "1").toInt()
val appVersionName: String = versionProps.getProperty("VERSION_NAME", "1.0.0")

android {
    namespace = "de.schlafgut.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.schlafgut.app"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            debugSymbolLevel = "FULL"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = releaseProperties.getProperty("RELEASE_STORE_FILE")?.let { file(it) }
            storePassword = releaseProperties.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = releaseProperties.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = releaseProperties.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Biometric
    implementation(libs.androidx.biometric)

    // Health Connect
    implementation(libs.androidx.health.connect)

    // Google Drive Backup
    implementation(libs.play.services.auth)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.http.client.gson)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// --- Automatische Versionierung ---
// Nach jedem erfolgreichen bundleRelease wird version.properties hochgezaehlt:
// VERSION_CODE +1, Patch-Version +1 (1.3.2 -> 1.3.3). Das erzeugte AAB traegt die gebaute Version.
val bumpVersion = tasks.register("bumpVersion") {
    group = "versioning"
    description = "Zaehlt versionCode und Patch-Version in version.properties hoch."
    val propsFile = versionPropsFile
    doLast {
        val props = Properties().apply { propsFile.inputStream().use { load(it) } }
        val code = props.getProperty("VERSION_CODE").toInt()
        val oldName = props.getProperty("VERSION_NAME")
        val parts = oldName.split(".").map { it.toInt() }.toMutableList()
        parts[parts.lastIndex] = parts.last() + 1
        val newName = parts.joinToString(".")
        propsFile.writeText(
            "# Wird von ./gradlew bundleRelease automatisch hochgezaehlt (Task bumpVersion).\n" +
                "# Enthaelt immer die Version des NAECHSTEN Releases.\n" +
                "VERSION_CODE=${code + 1}\n" +
                "VERSION_NAME=$newName\n"
        )
        println("Version gebaut: $code / $oldName -> naechste: ${code + 1} / $newName")
    }
}

tasks.matching { it.name == "bundleRelease" }.configureEach {
    finalizedBy(bumpVersion)
}
