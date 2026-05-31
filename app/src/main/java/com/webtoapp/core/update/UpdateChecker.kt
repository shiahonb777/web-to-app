package com.webtoapp.core.update

import com.webtoapp.core.logging.AppLogger
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object UpdateChecker {

    private const val TAG = "UpdateChecker"

    private const val MIRROR_PREFIX = "https://gh-proxy.org/"
    private const val OWNER = "shiahonb777"
    private const val REPO = "web-to-app"
    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    private const val TIMEOUT_MS = 12000

    data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
        override fun toString(): String = "$major.$minor.$patch"

        override fun compareTo(other: Version): Int {
            if (major != other.major) return major - other.major
            if (minor != other.minor) return minor - other.minor
            return patch - other.patch
        }

        companion object {
            fun parse(raw: String): Version? {
                val core = raw.trim().removePrefix("v").removePrefix("V")
                    .substringBefore('-').substringBefore('+')
                val parts = core.split('.')
                val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
                val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
                return Version(major, minor, patch)
            }
        }
    }

    data class ReleaseInfo(
        val version: String,
        val downloadUrl: String,
        val sizeBytes: Long,
        val sha256: String?,
        val releaseNotes: String
    )

    sealed class Result {
        data class UpdateAvailable(val info: ReleaseInfo, val currentVersion: String) : Result()
        data class UpToDate(val version: String) : Result()
        data class Failed(val message: String, val throwable: Throwable? = null) : Result()
    }

    fun withMirror(url: String): String =
        if (url.startsWith(MIRROR_PREFIX)) url else MIRROR_PREFIX + url

    suspend fun check(currentVersionName: String): Result = withContext(Dispatchers.IO) {
        val current = Version.parse(currentVersionName)
            ?: return@withContext Result.Failed("Cannot parse current version: $currentVersionName")

        try {
            val json = fetchLatestReleaseJson()
                ?: return@withContext Result.Failed("Empty response from release API")

            val release = JSONObject(json)
            val tag = release.optString("tag_name").ifBlank { release.optString("name") }
            val latest = Version.parse(tag)
                ?: return@withContext Result.Failed("Cannot parse release tag: $tag")

            if (latest <= current) {
                return@withContext Result.UpToDate(current.toString())
            }

            val asset = pickBestApkAsset(release)
                ?: return@withContext Result.Failed("No APK asset found in release $tag")

            val rawUrl = asset.optString("browser_download_url")
            if (rawUrl.isBlank()) {
                return@withContext Result.Failed("Release asset has no download URL")
            }

            val info = ReleaseInfo(
                version = latest.toString(),
                downloadUrl = withMirror(rawUrl),
                sizeBytes = asset.optLong("size", 0L),
                sha256 = asset.optString("digest").takeIf { it.isNotBlank() }
                    ?.substringAfter("sha256:", "")?.takeIf { it.isNotBlank() },
                releaseNotes = release.optString("body").trim()
            )
            Result.UpdateAvailable(info, current.toString())
        } catch (e: Exception) {
            AppLogger.e(TAG, "Update check failed", e)
            Result.Failed(e.message ?: "Update check failed", e)
        }
    }

    private fun pickBestApkAsset(release: JSONObject): JSONObject? {
        val assets = release.optJSONArray("assets") ?: return null
        var best: JSONObject? = null
        var bestVersion: Version? = null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name")
            if (!name.endsWith(".apk", ignoreCase = true)) continue
            val assetVersion = Version.parse(name.removeSuffix(".APK").removeSuffix(".apk").substringAfterLast('-'))
            if (best == null || (assetVersion != null && bestVersion != null && assetVersion > bestVersion) ||
                (assetVersion != null && bestVersion == null)) {
                best = asset
                bestVersion = assetVersion
            }
        }
        return best
    }

    private fun fetchLatestReleaseJson(): String? {
        val candidates = listOf(withMirror(LATEST_RELEASE_API), LATEST_RELEASE_API)
        var lastError: Exception? = null
        for (endpoint in candidates) {
            try {
                return httpGet(endpoint)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Release API failed via $endpoint: ${e.message}")
                lastError = e
            }
        }
        lastError?.let { throw it }
        return null
    }

    private fun httpGet(endpoint: String): String {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "WebToApp-UpdateChecker")
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code from $endpoint")
            }
            return connection.inputStream.bufferedReader().use(BufferedReader::readText)
        } finally {
            connection?.disconnect()
        }
    }
}
