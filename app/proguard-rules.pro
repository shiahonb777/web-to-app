# WebToApp ProGuard Rules
# 优化 APK 大小同时保持应用正常运行

# ===== 保留数据模型类（Gson 序列化需要）=====
-keep class com.webtoapp.data.model.** { *; }
-keep class com.webtoapp.core.shell.ShellConfig { *; }
-keep class com.webtoapp.core.shell.ShellConfig$* { *; }
-keep class com.webtoapp.core.shell.EmbeddedShellModule { *; }
-keep class com.webtoapp.ui.data.** { *; }

# 保留所有 data class（Kotlin）
-keepclassmembers class * {
    <init>(...);
}

# ===== Room 数据库 =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ===== Kotlin =====
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Lazy {
    public <methods>;
}
-dontwarn kotlin.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ===== Gson 序列化 =====
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ===== OkHttp =====
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ===== WebView =====
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ===== Compose =====
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ===== 防止崩溃的通用规则 =====
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes InnerClasses

# 保留枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留 Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留 Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
