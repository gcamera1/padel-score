// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.paparazzi) apply false
}

tasks.register("checkVersionConsistency") {
    doLast {
        val mobileVersion = project.properties["PADEL_VERSION_CODE"]
        val wearVersion = project.properties["PADEL_VERSION_CODE"]
        require(mobileVersion == wearVersion) {
            "versionCode mismatch: mobile=$mobileVersion, wear=$wearVersion"
        }
        println("Version consistency OK: versionCode=$mobileVersion")
    }
}

tasks.matching { it.name.contains("bundleRelease") }.configureEach {
    dependsOn("checkVersionConsistency")
}
