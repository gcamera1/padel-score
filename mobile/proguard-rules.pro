# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**

# DataStore Preferences
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.gonzalocamera.padelcounter.shared.**$$serializer { *; }
-keepclassmembers class com.gonzalocamera.padelcounter.shared.** { *** Companion; }
-keepclasseswithmembers class com.gonzalocamera.padelcounter.shared.** { kotlinx.serialization.KSerializer serializer(...); }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**

# Compose: keep runtime metadata
-dontwarn androidx.compose.**

# Google Play Services Wearable
-keep class com.google.android.gms.wearable.** { *; }
