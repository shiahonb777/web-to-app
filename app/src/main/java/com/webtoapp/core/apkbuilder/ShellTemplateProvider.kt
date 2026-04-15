package com.webtoapp.core.apkbuilder

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import java.io.File
import java.io.FileOutputStream

/**
 * Note: brief English comment.
 * 
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 * 
 * Note: brief English comment.
 */
interface ShellTemplateProvider {
    
    /**
     * Note: brief English comment.
     * Note: brief English comment.
     */
    fun getTemplate(): File?
    
    /**
     * Note: brief English comment.
     */
    val sourceName: String
    
    /**
     * Note: brief English comment.
     * Note: brief English comment.
     */
    val estimatedSize: Long get() = -1
}

/**
 * Note: brief English comment.
 * 
 * Note: brief English comment.
 * Note: brief English comment.
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
 * Note: brief English comment.
 * 
 * Note: brief English comment.
 * Note: brief English comment.
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
 * Note: brief English comment.
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
         * Note: brief English comment.
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
