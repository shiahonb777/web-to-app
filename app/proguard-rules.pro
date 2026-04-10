# WebToApp ProGuard Rules
# 优化 APK 大小同时保持应用正常运行

# ===== 保留数据模型类（Gson 序列化需要）=====
-keep class com.webtoapp.data.model.** { *; }
-keep class com.webtoapp.core.shell.** { *; }
-keep class com.webtoapp.ui.data.** { *; }

# 保留数据模型类的构造函数（Gson 反序列化需要）
# Note: Do NOT blanket-keep all constructors — it defeats R8 optimization.
-keepclassmembers class com.webtoapp.data.model.** {
    <init>(...);
}
-keepclassmembers class com.webtoapp.core.shell.** {
    <init>(...);
}
-keepclassmembers class com.webtoapp.core.isolation.** {
    <init>(...);
}
-keepclassmembers class com.webtoapp.core.forcedrun.ForcedRunConfig {
    <init>(...);
}
-keepclassmembers class com.webtoapp.core.blacktech.BlackTechConfig {
    <init>(...);
}
-keepclassmembers class com.webtoapp.core.disguise.DisguiseConfig {
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
# Note: Do NOT blanket-keep kotlin.** — it defeats R8 optimization.
# Only keep what's actually needed for reflection/serialization.
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Lazy {
    public <methods>;
}
-dontwarn kotlin.**

# Kotlin Coroutines — MUST keep Android Main dispatcher (loaded via ServiceLoader reflection)
# Without these rules, R8 strips AndroidDispatcherFactory → Dispatchers.Main crashes at runtime
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
# Keep ServiceLoader service configuration files
-keep class kotlinx.coroutines.CoroutineExceptionHandler
-keep class kotlinx.coroutines.internal.MainDispatcherFactory

# ===== Gson 序列化 =====
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
# Note: Gson ships its own consumer ProGuard rules.
# Only keep classes used via reflection that aren't covered by consumer rules.
-keep class sun.misc.Unsafe { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Keep all fields annotated with @SerializedName (Gson deserialization)
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Keep Gson-deserialized classes outside main data model packages
-keep class com.webtoapp.core.isolation.** { *; }
-keep class com.webtoapp.core.stats.** { *; }
-keep class com.webtoapp.core.extension.agent.GeneratedModuleData { *; }
-keep enum com.webtoapp.core.extension.ModuleRunMode { *; }
-keep enum com.webtoapp.core.extension.ModuleUiType { *; }
-keep enum com.webtoapp.core.extension.ModuleCategory { *; }
-keep enum com.webtoapp.core.extension.ModuleSourceType { *; }

# ===== OkHttp =====
# Note: OkHttp and Okio ship their own consumer ProGuard rules.
# Only suppress warnings; blanket -keep is unnecessary and inflates APK.
-dontwarn okhttp3.**
-dontwarn okio.**

# ===== WebView =====
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ===== Compose =====
# Note: Compose ships its own consumer ProGuard rules.
# Only suppress warnings; blanket -keep is unnecessary and inflates APK.
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

# ===== R8 优化策略 =====
# 选择性混淆：Shell 模式运行时类通过下方 -keep 规则保持类名不变，
# 编辑器侧代码（UI Screens、APK Builder 等）允许混淆以提高安全性和减小体积。
# 注意：如果新增了 Shell 运行时依赖的类，必须在下方添加对应的 -keep 规则。

# 保留 AndroidManifest 中引用的组件类名（Activity/Service/Receiver/Provider）
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ===== Shell 模式运行时（生成的 APK 必须保留）=====
# Shell Activity 及 UI 组件
-keep class com.webtoapp.ui.shell.** { *; }

# WebView 引擎和管理器
-keep class com.webtoapp.core.webview.** { *; }

# GeckoView 引擎
-keep class com.webtoapp.core.geckoview.** { *; }
-keep class org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.geckoview.**

# 后端运行时（PHP, Node, Python, Go）
-keep class com.webtoapp.core.linux.** { *; }
-keep class com.webtoapp.core.wordpress.** { *; }

# 强制运行模式
-keep class com.webtoapp.core.forcedrun.** { *; }

# 伪装/隐私功能
-keep class com.webtoapp.core.disguise.** { *; }
-keep class com.webtoapp.core.blacktech.** { *; }

# 扩展系统（Shell 模式中运行扩展）
-keep class com.webtoapp.core.extension.** { *; }

# 下载管理
-keep class com.webtoapp.util.DownloadNotificationManager { *; }
-keep class com.webtoapp.util.MediaSaver { *; }
-keep class com.webtoapp.util.MediaSaver$* { *; }

# 日志系统
-keep class com.webtoapp.core.logging.** { *; }

# i18n 字符串（运行时引用）
-keep class com.webtoapp.core.i18n.** { *; }

# APK 构建器本身不需要在 Shell APK 中保留（仅编辑器使用）
# 但 ApkConfig 等数据类可能被序列化引用
-keep class com.webtoapp.core.apkbuilder.ApkConfig { *; }

# Native APK 优化器 — JNI 从 C 层反射创建 OptimizeResult 对象
-keep class com.webtoapp.core.apkbuilder.NativeApkOptimizer { *; }
-keep class com.webtoapp.core.apkbuilder.NativeApkOptimizer$OptimizeResult { *; }
-keep class com.webtoapp.core.apkbuilder.NativeApkOptimizer$SizeBreakdown { *; }

# ===== 第三方库额外规则 =====
# Apache Commons Compress（编辑器使用，Shell 模式不需要，R8 可安全移除）
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**

# ZXing（编辑器使用）
-dontwarn com.google.zxing.**
-dontwarn com.journeyapps.**

# apksig（编辑器使用 — V1/V2/V3 签名引擎）
# apksig 的 Asn1DerEncoder 使用反射（getDeclaredAnnotation/getDeclaredFields）
# 读取 @Asn1Class/@Asn1Field 注解来序列化 PKCS#7 SignedData 结构。
# 如果 R8 剥离了这些注解和字段，签名会失败：
# "SignatureException: Failed to encode signature block"
-keep class com.android.apksig.** { *; }
-keepclassmembers class com.android.apksig.** { *; }
-dontwarn com.android.apksig.**

# SnakeYAML
-dontwarn org.yaml.snakeyaml.**

# Koin DI
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# ===== 反射实例化的类（Koin/ViewModel/Application）=====
# Application 类
-keep class com.webtoapp.WebToAppApplication { *; }

# ViewModels（通过 Koin/AndroidX 反射创建）
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Koin 模块中注册的所有单例和工厂类
-keep class com.webtoapp.data.database.AppDatabase { *; }
-keep class com.webtoapp.data.repository.** { *; }
-keep class com.webtoapp.core.usecase.** { *; }
-keep class com.webtoapp.core.activation.ActivationManager { *; }
-keep class com.webtoapp.core.announcement.AnnouncementManager { *; }
-keep class com.webtoapp.core.adblock.AdBlocker { *; }
-keep class com.webtoapp.core.shell.ShellModeManager { *; }
-keep class com.webtoapp.core.crypto.KeyManager { *; }
-keep class com.webtoapp.core.crypto.NativeCrypto { *; }
-keep class com.webtoapp.core.crypto.NativeCryptoOptimized { *; }
-keep class com.webtoapp.core.perf.NativePerfEngine { *; }
-keep class com.webtoapp.core.perf.NativeSysOptimizer { *; }
-keep class com.webtoapp.core.perf.SystemPerfOptimizer { *; }
-keep class com.webtoapp.core.perf.SystemPerfOptimizer$SystemProfile { *; }
-keep class com.webtoapp.core.kernel.BrowserKernel { *; }
-keep class com.webtoapp.core.extension.ExtensionManager { *; }
-keep class com.webtoapp.core.stats.** { *; }
-keep class com.webtoapp.core.batch.BatchImportService { *; }
-keep class com.webtoapp.di.** { *; }

# DAO 接口（Room 生成实现类）
-keep class * extends androidx.room.RoomDatabase { *; }
-keep interface com.webtoapp.data.dao.** { *; }
