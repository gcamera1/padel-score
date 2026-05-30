plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.paparazzi)
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
    implementation(platform(libs.compose.bom))

    // Compose base
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.core.splashscreen)

    // Wear OS Compose
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)

    // DataStore
    implementation(libs.datastore.preferences)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)

    // Preview tooling
    debugImplementation(libs.compose.ui.tooling)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
}
