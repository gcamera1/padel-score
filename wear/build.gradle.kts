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
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)

    // Wear OS Compose
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)

    // DataStore
    implementation(libs.datastore.preferences)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)

    // Google Play Services Wearable
    implementation(libs.play.services.wearable)

    // Kotlin libraries
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.play.services)

    // Preview tooling
    debugImplementation(libs.compose.ui.tooling)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
}
