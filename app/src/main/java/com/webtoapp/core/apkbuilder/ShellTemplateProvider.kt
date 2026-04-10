package com.webtoapp.core.apkbuilder

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import java.io.File
import java.io.FileOutputStream

/**
 * Shell 模板提供者接口
 * 
 * 将模板 APK 的来源与 ApkBuilder 解耦：
 * - SelfAsTemplateProvider: 当前方案，使用自身 APK 作为模板
 * - AssetTemplateProvider: 从 assets 加载预置的轻量 Shell APK
 * - RemoteTemplateProvider: (未来) 从远程下载专用 Shell APK
 * 
 * 当未来实现独立 Shell APK 时，只需新增 Provider 即可，无需修改 ApkBuilder
 */
interface ShellTemplateProvider {
    
    /**
     * 获取可用的模板 APK 文件
     * @return 模板文件，不可用时返回 null
     */
    fun getTemplate(): File?
    
    /**
     * 模板来源描述（用于日志）
     */
    val sourceName: String
    
    /**
     * 预估模板大小（字节），用于构建前的存储空间检查
     * 返回 -1 表示未知
     */
    val estimatedSize: Long get() = -1
}

/**
 * 使用当前应用自身 APK 作为模板（现有方案）
 * 
 * 优点：无需额外资源，始终可用
 * 缺点：模板包含完整 Builder 代码，导出 APK 偏大
 */
class SelfAsTemplateProvider(private val context: Context) : ShellTemplateProvider {
    
    override val sourceName = "self-apk"
    
    override val estimatedSize: Long
        get() = try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            File(appInfo.sourceDir).length()
        } catch (e: Exception) { -1 }
    
    override fun getTemplate(): File? {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            val sourceApk = File(appInfo.sourceDir)
            if (sourceApk.exists()) sourceApk else null
        } catch (e: Exception) {
            AppLogger.e("SelfAsTemplateProvider", "Failed to get self APK", e)
            null
        }
    }
}

/**
 * 从 assets 目录加载预置的轻量 Shell APK
 * 
 * 未来如果提供独立的 shell.apk（仅包含 WebView 运行时，不含 Builder UI），
 * 将其放入 assets/template/ 即可自动使用
 */
class AssetTemplateProvider(
    private val context: Context,
    private val assetPath: String = "template/webview_shell.apk"
) : ShellTemplateProvider {
    
    override val sourceName = "asset($assetPath)"
    
    private val cacheDir = File(context.cacheDir, "shell_templates").apply { mkdirs() }
    
    override fun getTemplate(): File? {
        val cached = File(cacheDir, "shell.apk")
        if (cached.exists()) return cached
        
        return try {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(cached).use { output ->
                    input.copyTo(output)
                }
            }
            cached
        } catch (e: Exception) {
            AppLogger.d("AssetTemplateProvider", "No asset template at $assetPath")
            null
        }
    }
}

/**
 * 模板选择策略：优先使用轻量 Asset 模板，回退到自身 APK
 */
class CompositeTemplateProvider(
    private val providers: List<ShellTemplateProvider>
) : ShellTemplateProvider {
    
    override val sourceName: String
        get() = "composite[${providers.joinToString(",") { it.sourceName }}]"
    
    override fun getTemplate(): File? {
        for (provider in providers) {
            val template = provider.getTemplate()
            if (template != null) {
                AppLogger.i("CompositeTemplateProvider", "Using template from: ${provider.sourceName}")
                return template
            }
        }
        AppLogger.e("CompositeTemplateProvider", "No template available from any provider")
        return null
    }
    
    companion object {
        /**
         * 默认策略：Asset 优先 → 自身 APK 回退
         */
        fun default(context: Context): CompositeTemplateProvider {
            return CompositeTemplateProvider(
                listOf(
                    AssetTemplateProvider(context),
                    SelfAsTemplateProvider(context)
                )
            )
        }
    }
}
