# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ─────────────────────────────────────────────────────────────────────
# CRITICAL: Keep networking classes needed for API calls
# ─────────────────────────────────────────────────────────────────────

# Keep Retrofit interfaces and annotations
-keep interface com.juanweather.data.remote.** { *; }
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Gson and its reflection capabilities
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    <init>(...);
    @com.google.gson.annotations.SerializedName <fields>;
}

# CRITICAL: Keep all model classes and their field names for Gson reflection
-keep class com.juanweather.data.models.** { *; }
-keepclassmembers class com.juanweather.data.models.** {
    <init>(...);
    *;
}

# Keep all ViewModel and Repository classes
-keep class com.juanweather.viewmodel.** { *; }
-keep class com.juanweather.data.repository.** { *; }

# Preserve generic signatures for Gson
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

# Keep Room database classes
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Gson type token for generic types (critical for ParameterizedType support)
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keep class com.google.gson.reflect.TypeToken$* { *; }

# CRITICAL: Keep generic type information for Retrofit/Gson deserialization
-keepattributes TypeAnnotatedElement
-keepattributes ParameterizedType

# Keep all data model classes and preserve their field names/types
-keep class com.juanweather.data.models.** {
    <init>(...);
    !static <fields>;
    !static <methods>;
}

-keepclassmembers class com.juanweather.data.models.** {
    *** <fields>;
}

# Preserve line numbers and source file attribute for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile


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