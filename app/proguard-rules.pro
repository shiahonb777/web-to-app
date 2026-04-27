# WebToApp ProGuard Rules
# 策略：启用代码收缩（移除未使用代码）+ 资源压缩（移除未使用资源）
# 但禁用混淆（-dontobfuscate），因为项目开源且大量使用 Gson/Koin/反射/JNI，
# 混淆会导致字段名被重命名、Gson 反序列化失败、反射找不到类等严重 bug。

-dontobfuscate

# ===== 保留属性（调试/崩溃堆栈可读）=====
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# ===== Android 组件（AndroidManifest 引用，必须保留类名）=====
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.accessibilityservice.AccessibilityService

# ===== Room 数据库 =====
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ===== Kotlin =====
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { public <methods>; }
-dontwarn kotlin.**

# Kotlin Coroutines — ServiceLoader 反射加载
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# ===== Gson 序列化 =====
-keep class sun.misc.Unsafe { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ===== WebView JS 接口 =====
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ===== 通用规则 =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===== com.webtoapp 全部保留（Gson/Koin/反射/JNI/Compose 密集使用）=====
# 项目开源，禁用混淆后只需防止 R8 误删反射引用的类
-keep class com.webtoapp.** { *; }
-keepclassmembers class com.webtoapp.** { *; }

# ===== 第三方库 =====
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn androidx.compose.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**
-dontwarn com.google.zxing.**
-dontwarn com.journeyapps.**
-dontwarn org.yaml.snakeyaml.**
-dontwarn org.mozilla.geckoview.**

# apksig — 反射读取 @Asn1Class/@Asn1Field 注解序列化 PKCS#7
-keep class com.android.apksig.** { *; }
-keepclassmembers class com.android.apksig.** { *; }
-dontwarn com.android.apksig.**

# Koin DI
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# GeckoView
-keep class org.mozilla.geckoview.** { *; }
