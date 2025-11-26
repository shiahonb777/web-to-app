package com.webtoapp.core.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.google.gson.GsonBuilder
import com.webtoapp.data.model.WebApp
import com.webtoapp.ui.webview.WebViewActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 应用导出器 - 用于创建快捷方式和导出配置
 */
class AppExporter(private val context: Context) {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * 创建桌面快捷方式
     */
    fun createShortcut(webApp: WebApp): ShortcutResult {
        return try {
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                return ShortcutResult.Error("设备不支持创建快捷方式")
            }

            // 准备图标
            val icon = prepareIcon(webApp)

            // 创建启动Intent
            val intent = Intent(context, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("app_id", webApp.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            // 创建快捷方式信息
            val shortcutInfo = ShortcutInfoCompat.Builder(context, "webapp_${webApp.id}")
                .setShortLabel(webApp.name)
                .setLongLabel(webApp.name)
                .setIcon(icon)
                .setIntent(intent)
                .build()

            // 请求创建快捷方式
            val result = ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)

            if (result) {
                ShortcutResult.Success
            } else {
                ShortcutResult.Error("创建快捷方式失败")
            }
        } catch (e: Exception) {
            ShortcutResult.Error(e.message ?: "未知错误")
        }
    }

    /**
     * 导出配置为JSON文件
     */
    fun exportConfig(webApp: WebApp): ExportResult {
        return try {
            val exportDir = getExportDirectory()
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val fileName = "${webApp.name}_config_$timestamp.json"
            val file = File(exportDir, fileName)

            // 创建导出数据结构
            val exportData = AppExportData(
                version = 1,
                exportTime = System.currentTimeMillis(),
                app = webApp.toExportFormat()
            )

            // 写入文件
            FileOutputStream(file).use { stream ->
                stream.write(gson.toJson(exportData).toByteArray())
            }

            ExportResult.Success(file.absolutePath)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "导出失败")
        }
    }

    /**
     * 导出为完整项目模板
     */
    fun exportAsTemplate(webApp: WebApp): ExportResult {
        return try {
            val exportDir = getExportDirectory()
            val projectDir = File(exportDir, sanitizeFileName(webApp.name))

            if (projectDir.exists()) {
                projectDir.deleteRecursively()
            }
            projectDir.mkdirs()

            // 创建项目结构
            createTemplateProject(projectDir, webApp)

            ExportResult.Success(projectDir.absolutePath)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "导出模板失败")
        }
    }

    /**
     * 准备快捷方式图标
     */
    private fun prepareIcon(webApp: WebApp): IconCompat {
        webApp.iconPath?.let { path ->
            try {
                val uri = Uri.parse(path)
                val inputStream = context.contentResolver.openInputStream(uri)
                inputStream?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        // 调整大小
                        val size = 192
                        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true)
                        return IconCompat.createWithBitmap(scaledBitmap)
                    }
                }
            } catch (e: Exception) {
                // 使用默认图标
            }
        }

        // 返回默认图标
        return IconCompat.createWithResource(context, android.R.drawable.sym_def_app_icon)
    }

    /**
     * 获取导出目录
     */
    private fun getExportDirectory(): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "WebToApp")
        } else {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "WebToApp")
        }
    }

    /**
     * 创建模板项目
     */
    private fun createTemplateProject(projectDir: File, webApp: WebApp) {
        // 创建目录结构
        val appDir = File(projectDir, "app/src/main")
        appDir.mkdirs()
        File(appDir, "java/com/webtoapp/generated").mkdirs()
        File(appDir, "res/values").mkdirs()
        File(appDir, "res/mipmap-xxxhdpi").mkdirs()

        // 生成build.gradle.kts
        File(projectDir, "build.gradle.kts").writeText(generateRootBuildGradle())
        File(projectDir, "settings.gradle.kts").writeText(generateSettingsGradle(webApp.name))
        File(projectDir, "app/build.gradle.kts").writeText(generateAppBuildGradle(webApp))

        // 生成AndroidManifest.xml
        File(appDir, "AndroidManifest.xml").writeText(generateManifest(webApp))

        // 生成配置类
        File(appDir, "java/com/webtoapp/generated/AppConfig.kt")
            .writeText(generateAppConfig(webApp))

        // 生成strings.xml
        File(appDir, "res/values/strings.xml").writeText(generateStrings(webApp))

        // 保存图标
        webApp.iconPath?.let { path ->
            try {
                val uri = Uri.parse(path)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    File(appDir, "res/mipmap-xxxhdpi/ic_launcher.png").outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                // 忽略图标保存错误
            }
        }

        // 生成README
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

    private fun generateManifest(webApp: WebApp): String = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:usesCleartextTraffic="true"
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

    private fun generateAppConfig(webApp: WebApp): String {
        return """
package com.webtoapp.generated

/**
 * 自动生成的应用配置
 */
object AppConfig {
    const val APP_NAME = "${webApp.name}"
    const val TARGET_URL = "${webApp.url}"
    
    // 激活码配置
    const val ACTIVATION_ENABLED = ${webApp.activationEnabled}
    val ACTIVATION_CODES = listOf(${webApp.activationCodes.joinToString { "\"$it\"" }})
    
    // 广告拦截配置
    const val AD_BLOCK_ENABLED = ${webApp.adBlockEnabled}
    val AD_BLOCK_RULES = listOf(${webApp.adBlockRules.joinToString { "\"$it\"" }})
    
    // 公告配置
    const val ANNOUNCEMENT_ENABLED = ${webApp.announcementEnabled}
    const val ANNOUNCEMENT_TITLE = "${webApp.announcement?.title ?: ""}"
    const val ANNOUNCEMENT_CONTENT = "${webApp.announcement?.content ?: ""}"
    const val ANNOUNCEMENT_LINK = "${webApp.announcement?.linkUrl ?: ""}"
    const val ANNOUNCEMENT_SHOW_ONCE = ${webApp.announcement?.showOnce ?: true}
    
    // WebView配置
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
        return name.replace(Regex("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]"), "_")
    }

    private fun sanitizePackageName(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
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
 * 导出数据结构
 */
data class AppExportData(
    val version: Int,
    val exportTime: Long,
    val app: Map<String, Any?>
)

/**
 * 快捷方式创建结果
 */
sealed class ShortcutResult {
    data object Success : ShortcutResult()
    data class Error(val message: String) : ShortcutResult()
}

/**
 * 导出结果
 */
sealed class ExportResult {
    data class Success(val path: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}
