# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Room database entities and DAOs
-keep class com.example.db.** { *; }
-keepclassmembers class * {
    @androidx.room.Entity *;
    @androidx.room.Dao *;
    @androidx.room.Database *;
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
    @androidx.room.ForeignKey *;
    @androidx.room.Index *;
    @androidx.room.Query *;
    @androidx.room.Insert *;
    @androidx.room.Update *;
    @androidx.room.Delete *;
    @androidx.room.Transaction *;
}

# Keep Moshi adapters
-keep class com.squareup.moshi.** { *; }
-keep @interface com.squareup.moshi.JsonClass
-keep @interface com.squareup.moshi.Json
-keep @interface com.squareup.moshi.JsonAdapter
-keep @interface com.squareup.moshi.JsonClass
-keep class * implements com.squareup.moshi.JsonAdapter { *; }

# Keep Retrofit interfaces
-keep interface com.example.api.** { *; }
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.flow.** { *; }

# Keep Coil
-keep class coil.** { *; }
-keep class coil.compose.** { *; }

# Keep Firebase AI
-keep class com.google.firebase.ai.** { *; }
-keep class com.google.ai.** { *; }

# Keep AndroidX components
-keep class androidx.lifecycle.** { *; }
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }
-keep class androidx.room.** { *; }
-keep class androidx.work.** { *; }

# Keep Gson (if used)
-keep class com.google.gson.** { *; }

# Keep Serialization
-keep class kotlinx.serialization.** { *; }

# Keep Application class
-keep class com.example.MainActivity { *; }

# Keep ViewModels
-keep class com.example.viewmodel.** { *; }

# Keep Audio classes
-keep class com.example.audio.** { *; }

# Keep Model classes
-keep class com.example.model.** { *; }

# Keep Navigation
-keep class com.example.navigation.** { *; }

# Keep DB classes
-keep class com.example.db.** { *; }

# Preserve annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Don't warn about missing classes
-dontwarn com.google.firebase.**
-dontwarn com.google.ai.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn kotlinx.coroutines.**
-dontwarn kotlinx.serialization.**
-dontwarn androidx.compose.**
-dontwarn coil.**

# Optimize
-optimizationpasses 5
-allowaccessmodification