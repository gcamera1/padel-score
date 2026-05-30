plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.gonzalocamera.padelcounter.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gonzalocamera.padelcounter"
        minSdk = 26
        targetSdk = 34
        versionCode = (project.properties["PADEL_VERSION_CODE"] as String).toInt()
        versionName = project.properties["PADEL_VERSION_NAME"] as String
    }

    signingConfigs {
        create("release") {
            val props = project.rootProject.properties
            storeFile = (props["PADEL_STORE_FILE"] as? String)?.let { file(it) }
            storePassword = props["PADEL_STORE_PASSWORD"] as? String
            keyAlias = props["PADEL_KEY_ALIAS"] as? String
            keyPassword = props["PADEL_KEY_PASSWORD"] as? String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseConfig = signingConfigs.findByName("release")
            if (releaseConfig?.storeFile != null) {
                signingConfig = releaseConfig
            }
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":shared"))

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Material3 Adaptive (WindowSizeClass, ListDetailPaneScaffold)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.compose.material3.adaptive.layout)
    implementation(libs.compose.material3.adaptive.navigation)

    // Downloadable Google Fonts
    implementation(libs.compose.ui.text.google.fonts)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Google Play Services Wearable
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)

    // Kotlin libraries
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Preview tooling
    debugImplementation(libs.compose.ui.tooling)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.room.testing)
}
