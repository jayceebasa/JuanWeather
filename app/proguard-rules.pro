# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ─────────────────────────────────────────────────────────────────────
# CRITICAL: R8 Configuration - Disable optimizations that break reflection
# ─────────────────────────────────────────────────────────────────────

# Disable optimization for classes that use reflection
-optimizationpasses 3
-dontshrink

# ─────────────────────────────────────────────────────────────────────
# CRITICAL: Keep networking classes needed for API calls
# ─────────────────────────────────────────────────────────────────────

# Keep Retrofit interfaces and annotations
-keep interface com.juanweather.data.remote.** { *; }
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

# ESSENTIAL: Keep Retrofit's Call, Response, and all generic wrappers
-keep class retrofit2.Call { *; }
-keep class retrofit2.Response { *; }
-keep class retrofit2.Callback { *; }
-keepclasseswithmembers class retrofit2.** {
    <methods>;
}

# Keep all Retrofit annotations
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

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

# CRITICAL: Gson type token for generic types (must be before model rules)
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keep class com.google.gson.reflect.TypeToken$* { *; }

# CRITICAL: Preserve ALL data model classes exactly as they are
# Do NOT rename fields, methods, or the classes themselves
-keep class com.juanweather.data.models.** { *; }
-keepclassmembers class com.juanweather.data.models.** {
    <init>(...);
    !static <fields>;
    !static <methods>;
    *** <fields>;
    *** get*();
    *** set*();
}

# Keep all ViewModel and Repository classes
-keep class com.juanweather.viewmodel.** { *; }
-keep class com.juanweather.data.repository.** { *; }

# Preserve generic signatures for Gson
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Signature
-keepattributes LocalVariableTypeTable

# CRITICAL: Keep generic type information for Retrofit/Gson deserialization
-keepattributes TypeAnnotatedElement
-keepattributes ParameterizedType

# Keep Room database classes
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Additional Gson rules for generic type handling and internal classes
-keep class com.google.gson.internal.** { *; }
-keep class com.google.gson.stream.** { *; }

# Preserve line numbers and source file attribute for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

