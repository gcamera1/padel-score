# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# DataStore Preferences
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**

# Compose: keep runtime metadata
-dontwarn androidx.compose.**
