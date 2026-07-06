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
        val mobileVersion = (project.properties["PADEL_MOBILE_VERSION_CODE"] as String).toInt()
        val wearVersion = (project.properties["PADEL_WEAR_VERSION_CODE"] as String).toInt()
        // Play requires a distinct versionCode per bundle when phone + wear ship
        // together, so these MUST differ. versionName stays shared across modules.
        require(mobileVersion != wearVersion) {
            "versionCode collision: mobile and wear must differ (both=$mobileVersion)"
        }
        println("Version OK: mobile versionCode=$mobileVersion, wear versionCode=$wearVersion")
    }
}

tasks.matching { it.name.contains("bundleRelease") }.configureEach {
    dependsOn("checkVersionConsistency")
}
