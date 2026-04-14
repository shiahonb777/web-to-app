package com.webtoapp.core.export

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.webtoapp.data.model.WebApp
import com.webtoapp.ui.webview.WebViewActivity
import com.webtoapp.util.threadLocalCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*/**
 * Note: brief English comment.
 */
class AppExporter(private val context: Context) {

    companion object {
        private const val ACTION_SHORTCUT_CREATED = "com.webtoapp.SHORTCUT_CREATED"
        private const val SHORTCUT_ICON_SIZE = 192
        private const val BUFFER_SIZE = 8192
        
        // Note: brief English comment.
        private val gson: Gson by lazy {
            GsonBuilder().setPrettyPrinting().create()
        }
        
        // Pre-compiled regex for file/package name sanitization
        private val SANITIZE_FILENAME_REGEX = Regex("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]")
        private val SANITIZE_PACKAGE_REGEX = Regex("[^a-z0-9]")
        
        // Note: brief English comment.
        private val dateFormat: ThreadLocal<SimpleDateFormat> = threadLocalCompat {
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        }
    }

    /**
     * Note: brief English comment.
     * Note: brief English comment.
     */
    fun createShortcut(webApp: WebApp): ShortcutResult {
        return try {
            // Note: brief English comment.
            val iconBitmap = prepareIconBitmap(webApp)
            val icon = if (iconBitmap != null) {
                IconCompat.createWithBitmap(iconBitmap)
            } else {
                IconCompat.createWithResource(context, android.R.drawable.sym_def_app_icon)
            }

            // Note: brief English comment.
            val launchIntent = Intent(context, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("app_id", webApp.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            // Note: brief English comment.
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    // Note: brief English comment.
                    createShortcutApi26(webApp, icon, launchIntent)
                }
                else -> {
                    // Note: brief English comment.
                    createShortcutLegacy(webApp, iconBitmap, launchIntent)
                }
            }
        } catch (e: Exception) {
            ShortcutResult.Error("创建失败: ${e.message}")
        }
    }

    /**
     * Note: brief English comment.
     */
    private fun createShortcutApi26(
        webApp: WebApp,
        icon: IconCompat,
        launchIntent: Intent
    ): ShortcutResult {
        // Note: brief English comment.
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            // Note: brief English comment.
            return tryOpenShortcutSettings() ?: ShortcutResult.Error(
                "当前启动器不支持创建快捷方式，请尝试更换默认桌面或手动授权"
            )
        }

        // Note: brief English comment.
        val shortcutInfo = ShortcutInfoCompat.Builder(context, "webapp_${webApp.id}")
            .setShortLabel(webApp.name.take(10)) // 限制长度避免截断
            .setLongLabel(webApp.name.take(25))
            .setIcon(icon)
            .setIntent(launchIntent)
            .setAlwaysBadged() // Show应用角标
            .build()

        // Note: brief English comment.
        val callbackIntent = Intent(ACTION_SHORTCUT_CREATED).apply {
            `package` = context.packageName
        }
        val successCallback = PendingIntent.getBroadcast(
            context,
            webApp.id.toInt(),
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Note: brief English comment.
        val result = ShortcutManagerCompat.requestPinShortcut(
            context,
            shortcutInfo,
            successCallback.intentSender
        )

        return if (result) {
            ShortcutResult.Success
        } else {
            // Note: brief English comment.
            checkAndRequestPermission()
        }
    }

    /**
     * Note: brief English comment.
     */
    @Suppress("DEPRECATION")
    private fun createShortcutLegacy(
        webApp: WebApp,
        iconBitmap: Bitmap?,
        launchIntent: Intent
    ): ShortcutResult {
        val shortcutIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
            putExtra(Intent.EXTRA_SHORTCUT_NAME, webApp.name)
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent)
            putExtra("duplicate", false) // 不允许重复创建
            
            if (iconBitmap != null) {
                putExtra(Intent.EXTRA_SHORTCUT_ICON, iconBitmap)
            } else {
                putExtra(
                    Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(
                        context,
                        android.R.drawable.sym_def_app_icon
                    )
                )
            }
        }

        context.sendBroadcast(shortcutIntent)
        
        // Note: brief English comment.
        return ShortcutResult.Pending("快捷方式请求已发送，请检查桌面")
    }

    /**
     * Note: brief English comment.
     * Note: brief English comment.
     */
    private fun prepareIconBitmap(webApp: WebApp): Bitmap? {
        webApp.iconPath?.let { path ->
            var original: Bitmap? = null
            try {
                original = when {
                    // Note: brief English comment.
                    path.startsWith("/") -> {
                        val file = File(path)
                        if (file.exists()) {
                            BitmapFactory.decodeFile(path)
                        } else null
                    }
                    // file:// URI
                    path.startsWith("file://") -> {
                        val file = File(Uri.parse(path).path ?: return null)
                        if (file.exists()) {
                            BitmapFactory.decodeFile(file.absolutePath)
                        } else null
                    }
                    // content:// URI（backward compatible）
                    else -> {
                        val uri = Uri.parse(path)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                }
                
                if (original != null) {
                    // Note: brief English comment.
                    val scaled = Bitmap.createScaledBitmap(original, SHORTCUT_ICON_SIZE, SHORTCUT_ICON_SIZE, true)
                    if (scaled !== original) {
                        original.recycle()
                    }
                    return scaled
                } else {
                    return null
                }
            } catch (e: Exception) {
                // Note: brief English comment.
                original?.recycle()
            }
        }
        return null
    }

    /**
     * Note: brief English comment.
     */
    private fun checkAndRequestPermission(): ShortcutResult {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        val message = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                "请在 设置 > 应用设置 > 应用管理 > WebToApp > 权限管理 中开启「桌面快捷方式」权限"
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                "请在 设置 > 应用 > 应用管理 > WebToApp > 权限 中开启「创建桌面快捷方式」权限"
            }
            manufacturer.contains("oppo") -> {
                "请在 设置 > 应用管理 > WebToApp > 权限 中开启「桌面快捷方式」权限"
            }
            manufacturer.contains("vivo") -> {
                "请在 i管家 > 应用管理 > 权限管理 中开启「桌面快捷方式」权限"
            }
            manufacturer.contains("meizu") -> {
                "请在 手机管家 > 权限管理 中开启「桌面快捷方式」权限"
            }
            manufacturer.contains("samsung") -> {
                "请确认桌面已解锁编辑状态，或尝试长按应用图标添加到主屏幕"
            }
            else -> {
                "创建快捷方式失败，请检查桌面设置或应用权限"
            }
        }
        
        return ShortcutResult.PermissionRequired(message)
    }

    /**
     * Note: brief English comment.
     */
    private fun tryOpenShortcutSettings(): ShortcutResult? {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ShortcutResult.PermissionRequired("请在应用设置中开启快捷方式权限后重试")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Note: brief English comment.
     */
    fun exportConfig(webApp: WebApp): ExportResult {
        return try {
            val exportDir = getExportDirectory()
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = dateFormat.get()?.format(Date()) ?: System.currentTimeMillis().toString()
            val fileName = "${webApp.name}_config_$timestamp.json"
            val file = File(exportDir, fileName)

            // Note: brief English comment.
            val exportData = AppExportData(
                version = 1,
                exportTime = System.currentTimeMillis(),
                app = webApp.toExportFormat()
            )

            // Note: brief English comment.
            FileOutputStream(file).buffered(BUFFER_SIZE).use { stream ->
                stream.write(gson.toJson(exportData).toByteArray())
            }

            ExportResult.Success(file.absolutePath)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Export failed")
        }
    }

    /**
     * Note: brief English comment.
     */
    fun exportAsTemplate(webApp: WebApp): ExportResult {
        return try {
            val exportDir = getExportDirectory()
            val projectDir = File(exportDir, sanitizeFileName(webApp.name))

            if (projectDir.exists()) {
                projectDir.deleteRecursively()
            }
            projectDir.mkdirs()

            // Note: brief English comment.
            createTemplateProject(projectDir, webApp)

            ExportResult.Success(projectDir.absolutePath)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "导出模板失败")
        }
    }

    /**
     * Note: brief English comment.
     */
    private fun getExportDirectory(): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "WebToApp")
        } else {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "WebToApp")
        }
    }

    /**
     * Note: brief English comment.
     */
    private fun createTemplateProject(projectDir: File, webApp: WebApp) {
        // Note: brief English comment.
        val appDir = File(projectDir, "app/src/main")
        appDir.mkdirs()
        File(appDir, "java/com/webtoapp/generated").mkdirs()
        File(appDir, "res/values").mkdirs()
        File(appDir, "res/xml").mkdirs()
        File(appDir, "res/mipmap-xxxhdpi").mkdirs()

        // Generatebuild.gradle.kts
        File(projectDir, "build.gradle.kts").writeText(generateRootBuildGradle())
        File(projectDir, "settings.gradle.kts").writeText(generateSettingsGradle(webApp.name))
        File(projectDir, "app/build.gradle.kts").writeText(generateAppBuildGradle(webApp))

        // GenerateAndroidManifest.xml
        File(appDir, "AndroidManifest.xml").writeText(generateManifest())

        // Note: brief English comment.
        File(appDir, "java/com/webtoapp/generated/AppConfig.kt")
            .writeText(generateAppConfig(webApp))

        // Generatestrings.xml
        File(appDir, "res/values/strings.xml").writeText(generateStrings(webApp))
        File(appDir, "res/xml/network_security_config.xml").writeText(generateNetworkSecurityConfig())

        // Note: brief English comment.
        webApp.iconPath?.let { path ->
            try {
                val uri = Uri.parse(path)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    File(appDir, "res/mipmap-xxxhdpi/ic_launcher.png").outputStream().buffered(BUFFER_SIZE).use { output ->
                        input.copyTo(output, BUFFER_SIZE)
                    }
                }
            } catch (e: Exception) {
                // Note: brief English comment.
            }
        }

        // GenerateREADME
        File(projectDir, "README.md").writeText(generateReadme(webApp))
    }

    private fun generateRootBuildGradle(): String = """
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
}
    """.trimIndent()

    private fun generateSettingsGradle(appName: String): String = """
rootProject.name = "${sanitizeFileName(appName)}"
include(":app")
    """.trimIndent()

    private fun generateAppBuildGradle(webApp: WebApp): String {
        val packageName = "com.webtoapp.${sanitizePackageName(webApp.name)}"
        return """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "$packageName"
    compileSdk = 34

    defaultConfig {
        applicationId = "$packageName"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.webkit:webkit:1.9.0")
}
        """.trimIndent()
    }

    private fun generateManifest(): String = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:networkSecurityConfig="@xml/network_security_config"
        android:usesCleartextTraffic="false"
        android:theme="@style/Theme.AppCompat.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
    """.trimIndent()

    private fun generateNetworkSecurityConfig(): String = """
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">localhost</domain>
        <domain includeSubdomains="false">127.0.0.1</domain>
        <domain includeSubdomains="false">10.0.2.2</domain>
    </domain-config>
</network-security-config>
    """.trimIndent()

    private fun generateAppConfig(webApp: WebApp): String {
        return """
package com.webtoapp.generated

/**
 * Note: brief English comment.
 */
object AppConfig {
    const val APP_NAME = "${webApp.name}"
    const val TARGET_URL = "${webApp.url}"
    
    // Note: brief English comment.
    const val ACTIVATION_ENABLED = ${webApp.activationEnabled}
    val ACTIVATION_CODES = listOf(${webApp.activationCodes.joinToString { "\"$it\"" }})
    
    // Note: brief English comment.
    const val AD_BLOCK_ENABLED = ${webApp.adBlockEnabled}
    val AD_BLOCK_RULES = listOf(${webApp.adBlockRules.joinToString { "\"$it\"" }})
    
    // Note: brief English comment.
    const val ANNOUNCEMENT_ENABLED = ${webApp.announcementEnabled}
    const val ANNOUNCEMENT_TITLE = "${webApp.announcement?.title ?: ""}"
    const val ANNOUNCEMENT_CONTENT = "${webApp.announcement?.content ?: ""}"
    const val ANNOUNCEMENT_LINK = "${webApp.announcement?.linkUrl ?: ""}"
    const val ANNOUNCEMENT_SHOW_ONCE = ${webApp.announcement?.showOnce ?: true}
    
    // Note: brief English comment.
    const val JAVASCRIPT_ENABLED = ${webApp.webViewConfig.javaScriptEnabled}
    const val DOM_STORAGE_ENABLED = ${webApp.webViewConfig.domStorageEnabled}
    const val ZOOM_ENABLED = ${webApp.webViewConfig.zoomEnabled}
    const val DESKTOP_MODE = ${webApp.webViewConfig.desktopMode}
}
        """.trimIndent()
    }

    private fun generateStrings(webApp: WebApp): String = """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">${webApp.name}</string>
</resources>
    """.trimIndent()

    private fun generateReadme(webApp: WebApp): String = """
# ${webApp.name}

这是由 WebToApp 生成的Android项目模板。

## 配置信息

- **目标网址**: ${webApp.url}
- **激活码验证**: ${if (webApp.activationEnabled) "启用" else "禁用"}
- **广告拦截**: ${if (webApp.adBlockEnabled) "启用" else "禁用"}
- **弹窗公告**: ${if (webApp.announcementEnabled) "启用" else "禁用"}

## 编译方法

1. 使用 Android Studio 打开此项目
2. 等待 Gradle 同步完成
3. 点击 Build > Build Bundle(s) / APK(s) > Build APK(s)
4. 生成的 APK 位于 `app/build/outputs/apk/` 目录

## 注意事项

- 需要 Android Studio Hedgehog 或更高版本
- 需要 JDK 17 或更高版本
- 首次编译需要下载依赖，请确保网络畅通
    """.trimIndent()

    private fun sanitizeFileName(name: String): String {
        return name.replace(SANITIZE_FILENAME_REGEX, "_")
    }

    private fun sanitizePackageName(name: String): String {
        return name.lowercase()
            .replace(SANITIZE_PACKAGE_REGEX, "")
            .take(20)
            .ifEmpty { "app" }
    }

    private fun WebApp.toExportFormat() = mapOf(
        "id" to id,
        "name" to name,
        "url" to url,
        "activationEnabled" to activationEnabled,
        "activationCodes" to activationCodes,
        "adBlockEnabled" to adBlockEnabled,
        "adBlockRules" to adBlockRules,
        "announcementEnabled" to announcementEnabled,
        "announcement" to announcement,
        "webViewConfig" to webViewConfig
    )
}

/**
 * Note: brief English comment.
 */
data class AppExportData(
    val version: Int,
    val exportTime: Long,
    val app: Map<String, Any?>
)

/**
 * Note: brief English comment.
 */
sealed class ShortcutResult {
    /** Note: brief English comment. */
    data object Success : ShortcutResult()
    
    /** Note: brief English comment. */
    data class Pending(val message: String) : ShortcutResult()
    
    /** Note: brief English comment. */
    data class PermissionRequired(val message: String) : ShortcutResult()
    
    /** Note: brief English comment. */
    data class Error(val message: String) : ShortcutResult()
}

/**
 * Note: brief English comment.
 */
sealed class ExportResult {
    data class Success(val path: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}
