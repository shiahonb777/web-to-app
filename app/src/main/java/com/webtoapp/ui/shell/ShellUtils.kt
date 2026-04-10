package com.webtoapp.ui.shell

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.util.ensureWebUrlScheme
import com.webtoapp.util.normalizeExternalIntentUrl
import com.webtoapp.util.upgradeRemoteHttpToHttps
import java.io.File

/**
 * 格式化时间（毫秒）
 */
internal fun formatTimeMs(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000 / 60) % 60
    val hours = ms / 1000 / 60 / 60
    return if (hours > 0) {
        String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

internal fun normalizeShellTargetUrlForSecurity(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    // 只为没有 scheme 的 URL 添加默认 scheme（https）
    // 对于已有 http:// 的 URL，保持原样不强制升级
    // 原因：很多内网/旧网站只支持 HTTP，强制升级会导致无法访问
    val withScheme = if (!trimmed.startsWith("http://", ignoreCase = true) &&
                          !trimmed.startsWith("https://", ignoreCase = true)) {
        // 没有 scheme，添加 https 默认值
        "https://$trimmed"
    } else {
        // 已有 scheme，保持原样
        trimmed
    }
    return withScheme
}

/**
 * 验证 Deep Link URL 是否在允许的域名列表中
 * 防止恶意 intent 携带非法 URL
 * 
 * @param url 待验证的 URL
 * @param allowedHosts 允许的域名列表
 * @param targetUrl 配置的目标 URL（其域名始终允许）
 * @return 如果 URL 安全则返回 URL，否则返回 targetUrl
 */
internal fun validateDeepLinkUrl(url: String, allowedHosts: List<String>, targetUrl: String): String {
    if (allowedHosts.isEmpty()) return url  // 未配置白名单则放行
    
    val urlHost = try {
        java.net.URL(url).host?.lowercase()
    } catch (e: Exception) {
        AppLogger.w("ShellActivity", "Invalid deep link URL: $url")
        return targetUrl
    }
    
    if (urlHost.isNullOrBlank()) {
        AppLogger.w("ShellActivity", "Deep link URL has no host: $url")
        return targetUrl
    }
    
    // 提取配置 URL 的域名作为默认允许
    val configHost = try {
        java.net.URL(normalizeShellTargetUrlForSecurity(targetUrl)).host?.lowercase()
    } catch (e: Exception) { null }
    
    val allAllowed = buildSet {
        addAll(allowedHosts.map { it.lowercase() })
        configHost?.let { add(it) }
    }
    
    // 检查域名是否在白名单中（支持子域名匹配）
    val isAllowed = allAllowed.any { allowedHost ->
        urlHost == allowedHost || urlHost.endsWith(".$allowedHost")
    }
    
    if (!isAllowed) {
        AppLogger.w("ShellActivity", "Deep link URL host '$urlHost' not in allowed list: $allAllowed, redirecting to target URL")
        return targetUrl
    }
    
    return url
}

internal fun normalizeExternalUrlForIntent(rawUrl: String): String {
    val safeUrl = normalizeExternalIntentUrl(rawUrl)
    if (safeUrl.isEmpty()) {
        AppLogger.w("ShellActivity", "Blocked invalid or dangerous external URL: $rawUrl")
        return ""
    }
    return normalizeShellTargetUrlForSecurity(safeUrl)
}

internal fun shouldReextractAssets(marker: File, expectedToken: String): Boolean {
    if (!marker.exists()) return true
    return try {
        marker.readText() != expectedToken
    } catch (_: Exception) {
        true
    }
}

internal fun writeExtractionMarker(marker: File, token: String) {
    marker.parentFile?.mkdirs()
    marker.writeText(token)
}

internal fun buildExtractionToken(
    context: Context,
    scope: String,
    configVersionCode: Int,
    extra: String = ""
): String {
    val packageInfo = try {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, 0)
        }
    } catch (_: Exception) {
        null
    }

    val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode ?: 0L
    } else {
        @Suppress("DEPRECATION")
        (packageInfo?.versionCode ?: 0).toLong()
    }
    val apkLastUpdate = packageInfo?.lastUpdateTime ?: 0L

    return listOf(
        scope,
        "cfg=$configVersionCode",
        "apkVer=$apkVersionCode",
        "apkUpdated=$apkLastUpdate",
        "extra=$extra"
    ).joinToString("|")
}

internal fun extractAssetsRecursive(context: Context, assetPath: String, destDir: File) {
    AppLogger.d("extractAssets", "提取: assetPath='$assetPath' -> destDir='${destDir.absolutePath}'")
    destDir.mkdirs()
    val children = context.assets.list(assetPath)
    if (children == null) {
        AppLogger.w("extractAssets", "assets.list('$assetPath') 返回 null")
        return
    }
    AppLogger.d("extractAssets", "assets.list('$assetPath') -> ${children.size} 项: ${children.take(20).joinToString()}")

    if (children.isEmpty()) {
        // 叶子节点 = 文件
        context.assets.open(assetPath).use { input ->
            val destFile = File(destDir.parentFile, destDir.name)
            destFile.outputStream().use { output ->
                val bytes = input.copyTo(output)
                AppLogger.d("extractAssets", "  文件(叶子): $assetPath -> ${destFile.absolutePath} ($bytes bytes)")
            }
        }
        return
    }

    var extractedFiles = 0
    var extractedDirs = 0
    for (child in children) {
        val childAssetPath = "$assetPath/$child"
        val childDest = File(destDir, child)

        // 尝试列出子目录；若为空则说明是文件
        val subList = context.assets.list(childAssetPath)
        if (subList != null && subList.isNotEmpty()) {
            extractedDirs++
            extractAssetsRecursive(context, childAssetPath, childDest)
        } else {
            // 复制文件
            context.assets.open(childAssetPath).use { input ->
                childDest.outputStream().use { output ->
                    val bytes = input.copyTo(output)
                    extractedFiles++
                    AppLogger.d("extractAssets", "  文件: $child ($bytes bytes)")
                }
            }
        }
    }
    AppLogger.i("extractAssets", "'$assetPath' 提取完成: $extractedFiles 个文件, $extractedDirs 个子目录")
}
