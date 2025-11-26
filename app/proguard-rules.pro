# WebToApp ProGuard Rules

# Keep data classes
-keep class com.webtoapp.data.model.** { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# WebView
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
