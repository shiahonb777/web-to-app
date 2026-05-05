package com.webtoapp.core.apkbuilder

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import java.io.File
import java.io.FileOutputStream











interface ShellTemplateProvider {





    fun getTemplate(): File?




    val sourceName: String





    val estimatedSize: Long get() = -1

    fun supports(config: ApkConfig): Boolean = true





    val allowFallbackOnMissing: Boolean get() = true
}







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







class AssetTemplateProvider(
    private val context: Context,
    private val assetPath: String = "template/webview_shell.apk"
) : ShellTemplateProvider {

    override val sourceName = "asset($assetPath)"
    override val allowFallbackOnMissing: Boolean = false

    private val cacheDir = File(context.cacheDir, "shell_templates").apply { mkdirs() }

    override fun supports(config: ApkConfig): Boolean {
        return config.appType.trim().uppercase() in setOf(
            "WEB",
            "HTML",
            "FRONTEND",
            "IMAGE",
            "VIDEO",
            "GALLERY",
            "WORDPRESS",
            "NODEJS_APP",
            "PHP_APP",
            "PYTHON_APP",
            "GO_APP",
            "MULTI_WEB"
        )
    }

    override fun getTemplate(): File? {
        val cached = File(cacheDir, "shell.apk")
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




class CompositeTemplateProvider(
    private val providers: List<ShellTemplateProvider>
) : ShellTemplateProvider {

    override val sourceName: String
        get() = "composite[${providers.joinToString(",") { it.sourceName }}]"

    override fun getTemplate(): File? {
        return getTemplateFor(null)
    }

    fun getTemplateFor(config: ApkConfig?): File? {
        for (provider in providers) {
            if (config != null && !provider.supports(config)) {
                continue
            }
            val template = provider.getTemplate()
            if (template != null) {
                AppLogger.i("CompositeTemplateProvider", "Using template from: ${provider.sourceName}")
                return template
            }
            if (config != null && !provider.allowFallbackOnMissing) {
                AppLogger.e(
                    "CompositeTemplateProvider",
                    "Required template provider missing for appType=${config.appType}: ${provider.sourceName}"
                )
                return null
            }
        }
        AppLogger.e("CompositeTemplateProvider", "No template available from any provider")
        return null
    }

    companion object {



        fun default(context: Context): CompositeTemplateProvider {
            return CompositeTemplateProvider(
                listOf(
                    // Fast path: embedded asset (present in `full` flavor).
                    AssetTemplateProvider(context),
                    // Slim flavor path: fetch the real template APK from the
                    // cloud server and cache it locally. Returns null silently
                    // when the server is unreachable so the chain can continue.
                    RemoteTemplateProvider(context),
                    // Last-resort fallback: use the running APK as the template
                    // (only useful for very simple app types).
                    SelfAsTemplateProvider(context)
                )
            )
        }
    }
}
