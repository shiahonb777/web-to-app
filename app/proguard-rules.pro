# WebToApp ProGuard Rules
#
# 策略：启用代码收缩（移除未使用代码）+ 资源压缩（移除未使用资源），
# 禁用混淆与激进优化。这是经过线上事故反复验证后的"稳定优先"配置，
# 减包体积仍能保留 30%+ 收益，但完全避免反射/泛型/ServiceLoader/JNI 类陷阱。
#
# 收益与权衡：
#   - shrink:    保留，移除未使用类/方法（约 25% 体积削减）
#   - obfuscate: 关闭（开源项目无意义，反而引入 Gson/Koin/反射 bug）
#   - optimize:  关闭部分激进优化（保留默认收缩，避免内联引发的 NPE）
#
# 出问题时排查方法：
#   1. ./gradlew :app:assembleRelease -PandroidProguardPrintUsage=true
#      会在 build/outputs/mapping/release/usage.txt 写出被 R8 删除的所有内容
#   2. ./gradlew :app:assembleRelease -PandroidProguardPrintSeeds=true
#      会写出被显式 keep 的所有内容
#   3. 把崩溃栈对照 build/outputs/mapping/release/mapping.txt 反推

-dontobfuscate

# 关掉一组容易破坏反射 / Compose / 协程语义的优化模式
# 这些是 R8 历史上反复出现 bug 的优化通道，关掉它们体积代价可忽略
-optimizations !class/merging/*,!field/*,!method/marking/static,!method/inlining/*,!code/allocation/variable
-optimizationpasses 1

# ============================================================
# 调试 / 崩溃堆栈可读
# ============================================================
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations,RuntimeInvisibleTypeAnnotations
-keepattributes AnnotationDefault
-keepattributes MethodParameters

# 让崩溃栈打印 R8 重命名前的类名 / 行号
-renamesourcefileattribute SourceFile

# ============================================================
# Android 组件 — Manifest 引用，必须保名
# ============================================================
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.accessibilityservice.AccessibilityService
-keep public class * extends androidx.work.ListenableWorker
-keep public class * extends android.app.Application
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.lifecycle.ViewModel
-keep public class * extends androidx.lifecycle.AndroidViewModel

# ============================================================
# 通用反射 — Parcelable / Serializable / enum / native
# ============================================================
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

# JNI native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# WebView @JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# View 的 XML inflate / setOnClick 反射回调
-keepclassmembers class * extends android.view.View {
    void set*(***);
    *** get*();
}
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# ============================================================
# Kotlin
# ============================================================
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { public <methods>; }
-keep class kotlin.jvm.internal.DefaultConstructorMarker { *; }
-keepclassmembers class kotlin.coroutines.jvm.internal.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# Kotlin Coroutines — ServiceLoader 加载 Dispatchers.Main
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
# Coroutines 调试 / 异常 hooks
-keepnames class kotlinx.coroutines.flow.** { *; }

# ============================================================
# 项目自身代码 — 全部保留（开源 + 重反射）
# ============================================================
-keep class com.webtoapp.** { *; }
-keepclassmembers class com.webtoapp.** { *; }
-keep enum com.webtoapp.** { *; }
-keep interface com.webtoapp.** { *; }

# data class 的合成构造器（含默认参数）— Gson 反序列化必须
-keepclassmembers class com.webtoapp.data.model.** {
    <init>(...);
}

# ============================================================
# Room — KSP 生成的 _Impl 类已经在 com.webtoapp.** 范围内
# ============================================================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# ============================================================
# Gson — 序列化 / 反序列化通过反射
# ============================================================
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class * implements com.google.gson.InstanceCreator
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.JsonAdapter <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

# Gson TypeToken — R8 full mode 会把 `object : TypeToken<List<...>>() {}`
# 匿名子类的 Signature 属性剥掉，运行时抛 IllegalStateException。
# 参考: https://github.com/google/gson/blob/main/Troubleshooting.md#r8
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# ============================================================
# Koin DI — 大量反射 + ServiceLoader
# ============================================================
-keep class org.koin.** { *; }
-keepclassmembers class * {
    public <init>(org.koin.core.scope.Scope);
}
-keepclassmembers class * extends org.koin.core.module.Module { *; }
-dontwarn org.koin.**

# ============================================================
# OkHttp / Okio — Platform 反射检测 OS 安全栈
# ============================================================
-keep class okhttp3.internal.platform.** { *; }
-keep class okhttp3.internal.publicsuffix.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ============================================================
# Coil — ServiceLoader 加载 fetcher / decoder / mapper
# ============================================================
-keep class coil.util.** { *; }
-keep class coil.fetch.** { *; }
-keep class coil.decode.** { *; }
-keep class coil.map.** { *; }
-dontwarn coil.**

# ============================================================
# apksig — 反射读取 @Asn1Class/@Asn1Field 注解序列化 PKCS#7
# ============================================================
-keep class com.android.apksig.** { *; }
-keepclassmembers class com.android.apksig.** { *; }
-keep @com.android.apksig.internal.asn1.Asn1Class class *
-keepclassmembers class * {
    @com.android.apksig.internal.asn1.Asn1Field *;
}
-dontwarn com.android.apksig.**

# ============================================================
# GeckoView — 大量 JNI / 注解反射
# ============================================================
-keep class org.mozilla.geckoview.** { *; }
-keep class org.mozilla.gecko.** { *; }
-keepclassmembers class * extends org.mozilla.geckoview.GeckoSession$* { *; }
-dontwarn org.mozilla.geckoview.**

# ============================================================
# ZXing — 反射查找编码格式
# ============================================================
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.** { *; }
-dontwarn com.google.zxing.**
-dontwarn com.journeyapps.**

# ============================================================
# Vico 图表 — Compose 渲染器反射
# ============================================================
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# ============================================================
# BillingClient — AIDL stub
# ============================================================
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.android.billingclient.**

# ============================================================
# Credentials API + GoogleId — 反射解析 ID Token
# ============================================================
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.** { *; }
-keep class com.google.android.gms.auth.api.identity.** { *; }
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.**

# ============================================================
# DataStore Preferences
# ============================================================
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ============================================================
# Security crypto (alpha — 可能有反射)
# ============================================================
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn androidx.security.crypto.**
-dontwarn com.google.crypto.tink.**

# ============================================================
# Compress / xz — ServiceLoader 加载格式
# ============================================================
-keep class org.apache.commons.compress.compressors.FileNameUtil { *; }
-keep class org.apache.commons.compress.archivers.** { *; }
-keep class org.tukaani.xz.** { *; }
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**
-dontwarn org.brotli.dec.**
-dontwarn org.yaml.snakeyaml.**

# ============================================================
# Compose Runtime — 已有 consumer rules，仅压制 warn
# ============================================================
-dontwarn androidx.compose.**

# ============================================================
# Material / AppCompat — 已有 consumer rules，仅兜底
# ============================================================
-dontwarn com.google.android.material.**

# ============================================================
# Custom Tabs (browser)
# ============================================================
-keep class androidx.browser.** { *; }
-dontwarn androidx.browser.**

# ============================================================
# Haze (背景模糊)
# ============================================================
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**
