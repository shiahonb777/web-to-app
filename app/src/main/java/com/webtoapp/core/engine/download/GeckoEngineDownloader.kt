package com.webtoapp.core.engine.download

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.engine.EngineType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.webtoapp.core.network.NetworkModule
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float, val message: String) : DownloadState()
    data object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class GeckoEngineDownloader(
    private val context: Context,
    private val fileManager: EngineFileManager
) {
    companion object {
        private const val TAG = "GeckoEngineDownloader"
        private const val MAVEN_BASE_URL = "https://maven.mozilla.org/maven2/org/mozilla/geckoview"
        // Keep in sync with app/build.gradle.kts geckoview dependency version.
        const val DEFAULT_VERSION = "128.0.20240704121409"
        val SUPPORTED_ABIS = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

        private val ABI_ARTIFACT_MAP = mapOf(
            "arm64-v8a" to "geckoview-arm64-v8a",
            "armeabi-v7a" to "geckoview-armeabi-v7a",
            "x86_64" to "geckoview-x86_64",
            "x86" to "geckoview-x86"
        )
    }

    private val client get() = NetworkModule.downloadClient

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    @Volatile
    private var cancelRequested = false

    suspend fun download(
        abi: String? = null,
        version: String = DEFAULT_VERSION
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val targetAbi = abi ?: fileManager.getDevicePrimaryAbi()
            cancelRequested = false
            val versionCandidates = buildList {
                add(version)
                if (version != DEFAULT_VERSION) add(DEFAULT_VERSION)
            }.distinct()

            try {
                val artifactName = ABI_ARTIFACT_MAP[targetAbi]
                    ?: throw IllegalArgumentException("Unsupported ABI: $targetAbi")
                val tempAar = File(context.cacheDir, "geckoview_temp.aar")
                var lastError: String? = null

                for ((index, candidateVersion) in versionCandidates.withIndex()) {
                    if (fileManager.isAbiDownloaded(EngineType.GECKOVIEW, targetAbi)) {
                        val existingVersion = fileManager.getDownloadedVersion(EngineType.GECKOVIEW)
                        if (existingVersion == candidateVersion) {
                            AppLogger.i(TAG, "GeckoView already exists, skip")
                            _downloadState.value = DownloadState.Completed
                            return@withContext true
                        }
                    }

                    _downloadState.value = DownloadState.Downloading(0f, "Preparing...")
                    val aarUrl = MAVEN_BASE_URL + "/" + artifactName + "/" + candidateVersion + "/" + artifactName + "-" + candidateVersion + ".aar"
                    AppLogger.i(TAG, "Download URL: $aarUrl")
                    _downloadState.value = DownloadState.Downloading(
                        0.01f,
                        "Downloading GeckoView (${index + 1}/${versionCandidates.size})..."
                    )

                    val downloadSuccess = downloadFile(aarUrl, tempAar) { progress ->
                        if (!cancelRequested) {
                            _downloadState.value = DownloadState.Downloading(
                                progress * 0.8f,
                                "Downloading... " + (progress * 100).toInt() + "%"
                            )
                        }
                    }

                    if (cancelRequested) {
                        tempAar.delete()
                        _downloadState.value = DownloadState.Idle
                        return@withContext false
                    }

                    if (!downloadSuccess) {
                        tempAar.delete()
                        lastError = "Download failed for version $candidateVersion"
                        AppLogger.w(TAG, lastError!!)
                        continue
                    }

                    _downloadState.value = DownloadState.Downloading(0.85f, "Extracting...")
                    val extractSuccess = extractSoFromAar(tempAar, targetAbi)
                    tempAar.delete()

                    if (!extractSuccess) {
                        lastError = "Extract failed for version $candidateVersion"
                        AppLogger.w(TAG, lastError!!)
                        continue
                    }

                    fileManager.setDownloadedVersion(EngineType.GECKOVIEW, candidateVersion)
                    _downloadState.value = DownloadState.Completed
                    return@withContext true
                }

                _downloadState.value = DownloadState.Error(lastError ?: "Download failed")
                false

            } catch (e: CancellationException) {
                _downloadState.value = DownloadState.Idle
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "Download GeckoView failed", e)
                _downloadState.value = DownloadState.Error(e.message ?: "Unknown error")
                false
            }
        }
    }

    fun cancelDownload() {
        cancelRequested = true
    }

    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }

    fun getDownloadUrl(abi: String, version: String = DEFAULT_VERSION): String {
        val artifactName = ABI_ARTIFACT_MAP[abi] ?: ABI_ARTIFACT_MAP["arm64-v8a"]!!
        return MAVEN_BASE_URL + "/" + artifactName + "/" + version + "/" + artifactName + "-" + version + ".aar"
    }

    private fun downloadFile(
        url: String,
        destFile: File,
        onProgress: ((Float) -> Unit)? = null
    ): Boolean {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                AppLogger.e(TAG, "HTTP " + response.code)
                return false
            }

            val body = response.body ?: return false
            val contentLength = body.contentLength()
            val input = body.byteStream()
            val output = FileOutputStream(destFile)

            try {
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (cancelRequested) return false
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        onProgress?.invoke(totalBytesRead.toFloat() / contentLength)
                    }
                }
            } finally {
                output.close()
                input.close()
            }

            return destFile.exists() && destFile.length() > 0
        } catch (e: Exception) {
            AppLogger.e(TAG, "Download error: " + url, e)
            return false
        }
    }

    private fun extractSoFromAar(aarFile: File, targetAbi: String): Boolean {
        try {
            val abiDir = fileManager.getAbiDir(EngineType.GECKOVIEW, targetAbi)
            var extractedCount = 0
            val prefix = "jni/" + targetAbi + "/"

            ZipInputStream(aarFile.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory && name.startsWith(prefix) && name.endsWith(".so")) {
                        val soFileName = name.substringAfterLast("/")
                        val destFile = File(abiDir, soFileName)
                        FileOutputStream(destFile).use { out ->
                            zis.copyTo(out)
                        }
                        AppLogger.i(TAG, "Extracted: " + soFileName)
                        extractedCount++
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            AppLogger.i(TAG, "Extracted " + extractedCount + " .so files")
            return extractedCount > 0
        } catch (e: Exception) {
            AppLogger.e(TAG, "Extract AAR failed", e)
            return false
        }
    }
}
