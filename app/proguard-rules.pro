# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep native methods - CRITICAL for JNI
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep MltHelper class and all its native methods
-keep class uc.ucworks.videosnap.util.MltHelper {
    native <methods>;
    public <methods>;
}

# Keep all domain models (used with Gson/Room)
-keep class uc.ucworks.videosnap.domain.** { *; }
-keep class uc.ucworks.videosnap.TimelineClip { *; }
-keep class uc.ucworks.videosnap.TimelineTrack { *; }
-keep class uc.ucworks.videosnap.VideoProject { *; }
-keep class uc.ucworks.videosnap.VideoEffect { *; }

# Keep data entities for Room
-keep class uc.ucworks.videosnap.data.local.entity.** { *; }

# Keep Gson TypeAdapters
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# MediaCodec and Android Media Framework
-keep class android.media.** { *; }
-dontwarn android.media.**

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep WorkManager
-keep class androidx.work.** { *; }
-keep class uc.ucworks.videosnap.workers.** { *; }

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** Companion;
}

# Keep ExoPlayer (Media3)
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep reflection-based access
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Optimize
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose