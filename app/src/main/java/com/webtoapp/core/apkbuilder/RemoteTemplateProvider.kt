package com.webtoapp.core.apkbuilder

import android.content.Context
import com.webtoapp.BuildConfig
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.network.NetworkModule
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File


/**
 * Downloads the shell template APK from the WebToApp cloud server and caches
 * it locally. Used primarily by the `slim` flavor, which does not embed the
 * template APK at build time. The `full` flavor still ships the template in
 * assets and will typically never reach this provider, unless the embedded
 * asset is somehow missing.
 *
 * Server contract (stable):
 *   GET {BASE_URL}/api/v1/shell-template/latest?versionCode=<n>&variant=<slim|full>
 *     200 + application/vnd.android.package-archive → APK bytes
 *           + response header `X-Template-Version: <semver or hash>` (optional, used for cache key)
 *     304 → reuse cached file for the version advertised in request `If-None-Match`
 *     404 / 5xx → provider returns null; composite will fall back to next provider
 *
 * Cache layout:
 *   <cacheDir>/shell_templates/remote_v<version>.apk
 *   <cacheDir>/shell_templates/remote.etag
 *
 * Failure policy: never throws to the builder; network errors and missing
 * endpoints just return null and let `CompositeTemplateProvider` continue.
 */
class RemoteTemplateProvider(
    private val context: Context,
    private val baseUrl: String = DEFAULT_BASE_URL
) : ShellTemplateProvider {

    companion object {
        private const val TAG = "RemoteTemplateProvider"
        // Same origin used by CloudApiClient. Kept as a constant to avoid a
        // hard module dependency and to let callers override for testing.
        const val DEFAULT_BASE_URL = "https://api.shiaho.sbs"
        private const val ENDPOINT_PATH = "/api/v1/shell-template/latest"
    }

    override val sourceName = "remote(${baseUrl.substringAfter("//").substringBefore("/")})"

    // Allow composite fallback on failure so `SelfAsTemplateProvider` can still run.
    override val allowFallbackOnMissing: Boolean = true

    private val cacheDir: File = File(context.cacheDir, "shell_templates").apply { mkdirs() }

    override fun supports(config: ApkConfig): Boolean {
        // Mirror AssetTemplateProvider's supported set.
        return config.appType.trim().uppercase() in setOf(
            "WEB", "HTML", "FRONTEND", "IMAGE", "VIDEO", "GALLERY",
            "WORDPRESS", "NODEJS_APP", "PHP_APP", "PYTHON_APP", "GO_APP", "MULTI_WEB"
        )
    }

    override fun getTemplate(): File? {
        val variant = if (BuildConfig.BUNDLED_SHELL_TEMPLATE) "full" else "slim"
        val url = "$baseUrl$ENDPOINT_PATH?versionCode=${BuildConfig.VERSION_CODE}&variant=$variant"
        val etagFile = File(cacheDir, "remote.etag")
        val cachedVersion = if (etagFile.exists()) etagFile.readText().trim() else ""
        val cachedApk = if (cachedVersion.isNotEmpty()) {
            File(cacheDir, "remote_v$cachedVersion.apk").takeIf { it.isFile && it.length() > 0 }
        } else null

        return try {
            val requestBuilder = Request.Builder().url(url).get()
            if (!cachedVersion.isEmpty()) {
                requestBuilder.header("If-None-Match", cachedVersion)
            }
            NetworkModule.downloadClient.newCall(requestBuilder.build()).execute().use { resp ->
                when {
                    resp.code == 304 && cachedApk != null -> {
                        AppLogger.d(TAG, "Remote template unchanged (304), using cached v$cachedVersion")
                        cachedApk
                    }
                    resp.isSuccessful -> {
                        val body = resp.body ?: return@use null
                        val version = (resp.header("X-Template-Version")
                            ?: resp.header("ETag")?.trim('"')
                            ?: System.currentTimeMillis().toString())
                        val target = File(cacheDir, "remote_v$version.apk")
                        val tmp = File(cacheDir, "remote_v$version.apk.part")
                        tmp.sink().buffer().use { sink ->
                            sink.writeAll(body.source())
                        }
                        if (!tmp.renameTo(target)) {
                            tmp.copyTo(target, overwrite = true)
                            tmp.delete()
                        }
                        etagFile.writeText(version)
                        AppLogger.i(TAG, "Downloaded remote template v$version (${target.length() / 1024} KB)")
                        target
                    }
                    resp.code == 404 -> {
                        AppLogger.d(TAG, "Remote template endpoint not available (404) — falling back")
                        cachedApk
                    }
                    else -> {
                        AppLogger.w(TAG, "Remote template request failed: HTTP ${resp.code}")
                        cachedApk
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Remote template fetch error: ${e.message}")
            cachedApk
        }
    }
}
