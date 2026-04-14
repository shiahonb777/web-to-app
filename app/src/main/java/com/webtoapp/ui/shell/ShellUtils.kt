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
 * Note
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
    // scheme URL default scheme( https)
    // http: // URL,
    // / support HTTP,
    val withScheme = if (!trimmed.startsWith("http://", ignoreCase = true) &&
                          !trimmed.startsWith("https://", ignoreCase = true)) {
        // scheme, https default
        "https://$trimmed"
    } else {
        // scheme,
        trimmed
    }
    return withScheme
}

/**
 * verify Deep Link URL list
 * intent URL
 * 
 * @param url verify URL
 * @param allowedHosts list
 * @param targetUrl config URL( always)
 * @return if URL back URL, back targetUrl
 */
internal fun validateDeepLinkUrl(url: String, allowedHosts: List<String>, targetUrl: String): String {
    if (allowedHosts.isEmpty()) return url  // config
    
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
    
    // config URL default
    val configHost = try {
        java.net.URL(normalizeShellTargetUrlForSecurity(targetUrl)).host?.lowercase()
    } catch (e: Exception) { null }
    
    val allAllowed = buildSet {
        addAll(allowedHosts.map { it.lowercase() })
        configHost?.let { add(it) }
    }
    
    // check( support)
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
        // = file
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

        // directory; file
        val subList = context.assets.list(childAssetPath)
        if (subList != null && subList.isNotEmpty()) {
            extractedDirs++
            extractAssetsRecursive(context, childAssetPath, childDest)
        } else {
            // file
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
