plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("app.cash.paparazzi")
}

android {
    namespace = "com.gonzalocamera.padelcounter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gonzalocamera.padelcounter"
        minSdk = 30
        targetSdk = 34
        versionCode = 5
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            // Configure in ~/.gradle/gradle.properties:
            //   PADEL_STORE_FILE=/path/to/release.jks
            //   PADEL_STORE_PASSWORD=...
            //   PADEL_KEY_ALIAS=padel-score
            //   PADEL_KEY_PASSWORD=...
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

    bundle {
        density { enableSplit = true }
        abi { enableSplit = true }
    }
}

dependencies {

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))

    // Compose base
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Wear OS Compose
    implementation("androidx.wear.compose:compose-material:1.4.1")
    implementation("androidx.wear.compose:compose-foundation:1.4.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // Preview tooling
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.test:core:1.5.0")
}