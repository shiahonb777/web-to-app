package com.webtoapp.core.frontend

import android.content.Context
import com.webtoapp.core.cloud.GitHubAccelerator
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream










data class GitHubRepoSpec(
    val owner: String,
    val repo: String,
    val branch: String? = null,
    val subPath: String? = null
)




data class GitHubFetchResult(
    val localPath: String,
    val owner: String,
    val repo: String,
    val branch: String,
    val subPath: String?
)




sealed class GitHubFetchState {
    object Idle : GitHubFetchState()
    object Resolving : GitHubFetchState()
    data class Downloading(val progress: Float, val urlHint: String) : GitHubFetchState()
    object Extracting : GitHubFetchState()
    data class Success(val localPath: String) : GitHubFetchState()
    data class Error(val message: String) : GitHubFetchState()
}










class GitHubRepoFetcher(private val context: Context) {

    private val _state = MutableStateFlow<GitHubFetchState>(GitHubFetchState.Idle)
    val state: StateFlow<GitHubFetchState> = _state.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun reset() {
        _state.value = GitHubFetchState.Idle
        _logs.value = emptyList()
    }


    suspend fun fetch(
        input: String,
        explicitBranch: String? = null,
        explicitSubPath: String? = null
    ): Result<GitHubFetchResult> = withContext(Dispatchers.IO) {
        _logs.value = emptyList()
        log("Parsing input: $input")

        val spec = parseInput(input)
        if (spec == null) {
            val msg = "Invalid GitHub URL or repo identifier"
            return@withContext fail(msg)
        }

        val branch = explicitBranch?.takeIf { it.isNotBlank() }?.trim() ?: spec.branch
        val subPath = explicitSubPath?.takeIf { it.isNotBlank() }?.trim('/', ' ') ?: spec.subPath
        val branchCandidates = if (branch != null) listOf(branch) else BRANCH_FALLBACKS

        _state.value = GitHubFetchState.Resolving
        log("Owner: ${spec.owner}, Repo: ${spec.repo}, Branch hint: ${branch ?: "auto"}, SubPath: ${subPath ?: "(root)"}")


        var lastError: String? = null
        for (br in branchCandidates) {
            val zipUrl = "https://github.com/${spec.owner}/${spec.repo}/archive/refs/heads/$br.zip"
            val candidates = GitHubAccelerator.accelerateWithFallbacks(zipUrl)
            log("Trying branch: $br (${candidates.size} url candidate(s))")

            for (candidate in candidates) {
                log("Download: $candidate")
                val download = try {
                    downloadToTempFile(candidate)
                } catch (e: Exception) {
                    log("Download error: ${e.message}")
                    AppLogger.w(TAG, "Download failed: $candidate", e)
                    lastError = e.message
                    null
                }
                if (download == null) continue


                _state.value = GitHubFetchState.Extracting
                log("Extracting archive...")
                val extracted = try {
                    extract(download, spec.repo, subPath)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Extract failed", e)
                    log("Extract error: ${e.message}")
                    download.delete()
                    return@withContext fail(e.message ?: "Failed to extract archive")
                } finally {
                    download.delete()
                }


                _state.value = GitHubFetchState.Success(extracted.absolutePath)
                log("Done: ${extracted.absolutePath}")
                return@withContext Result.success(
                    GitHubFetchResult(
                        localPath = extracted.absolutePath,
                        owner = spec.owner,
                        repo = spec.repo,
                        branch = br,
                        subPath = subPath
                    )
                )
            }
        }

        fail(lastError ?: "Failed to download repository (check URL and branch)")
    }


    private fun downloadToTempFile(url: String): File? {
        val req = Request.Builder().url(url).get().build()
        NetworkModule.downloadClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                log("HTTP ${resp.code}")
                return null
            }
            val body = resp.body ?: return null
            val total = body.contentLength()
            val tempFile = File(context.cacheDir, "gh_repo_${System.currentTimeMillis()}.zip")
            try {
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { out ->
                        val buf = ByteArray(64 * 1024)
                        var totalRead = 0L
                        var read: Int
                        _state.value = GitHubFetchState.Downloading(0f, url)
                        while (input.read(buf).also { read = it } != -1) {
                            out.write(buf, 0, read)
                            totalRead += read
                            if (totalRead > MAX_DOWNLOAD_SIZE) {
                                throw IllegalStateException("Archive larger than ${MAX_DOWNLOAD_SIZE / 1024 / 1024} MB")
                            }
                            val progress = if (total > 0) (totalRead.toFloat() / total).coerceIn(0f, 1f) else 0f
                            _state.value = GitHubFetchState.Downloading(progress, url)
                        }
                    }
                }
                return tempFile
            } catch (e: Exception) {
                tempFile.delete()
                throw e
            }
        }
    }


    private fun extract(zipFile: File, repoName: String, subPath: String?): File {
        val extractDir = File(
            context.filesDir,
            "github_imports/${sanitize(repoName)}_${System.currentTimeMillis()}"
        ).apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }

        var entryCount = 0
        var totalBytes = 0L

        try {
            ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    entryCount++
                    if (entryCount > MAX_ENTRY_COUNT) {
                        throw IllegalStateException("Archive has too many entries (> $MAX_ENTRY_COUNT)")
                    }

                    val entryName = entry.name
                    if (shouldSkipEntry(entryName)) {
                        zis.closeEntry()
                        entry = zis.nextEntry
                        continue
                    }

                    val target = File(extractDir, entryName).canonicalFile
                    if (!target.path.startsWith(extractDir.canonicalPath)) {
                        AppLogger.w(TAG, "Skip unsafe zip entry: $entryName")
                        zis.closeEntry()
                        entry = zis.nextEntry
                        continue
                    }

                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { out ->
                            val buf = ByteArray(64 * 1024)
                            var read: Int
                            while (zis.read(buf).also { read = it } != -1) {
                                totalBytes += read
                                if (totalBytes > MAX_EXTRACTED_SIZE) {
                                    throw IllegalStateException(
                                        "Extracted content larger than ${MAX_EXTRACTED_SIZE / 1024 / 1024} MB"
                                    )
                                }
                                out.write(buf, 0, read)
                            }
                        }
                    }

                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            extractDir.deleteRecursively()
            throw e
        }


        val unwrapped = unwrapSingleRootDir(extractDir)


        val finalDir = if (!subPath.isNullOrBlank()) {
            val sub = File(unwrapped, subPath).canonicalFile
            if (!sub.path.startsWith(unwrapped.canonicalPath)) {
                extractDir.deleteRecursively()
                throw IllegalStateException("Invalid sub-path: $subPath")
            }
            if (!sub.exists() || !sub.isDirectory) {
                extractDir.deleteRecursively()
                throw IllegalStateException("Sub-path not found in repository: $subPath")
            }
            sub
        } else {
            unwrapped
        }

        return finalDir
    }


    private fun unwrapSingleRootDir(dir: File): File {
        val children = dir.listFiles() ?: return dir
        return if (children.size == 1 && children[0].isDirectory) children[0] else dir
    }


    private fun shouldSkipEntry(name: String): Boolean {
        val parts = name.split('/', '\\')
        return parts.any { p -> SKIP_PATTERNS.any { it.equals(p, ignoreCase = true) } }
    }


    private fun sanitize(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(64)


    private fun log(line: String) {
        _logs.value = _logs.value + line
        AppLogger.d(TAG, line)
    }


    private fun fail(message: String): Result<GitHubFetchResult> {
        _state.value = GitHubFetchState.Error(message)
        log("Failed: $message")
        return Result.failure(IllegalStateException(message))
    }


    companion object {
        private const val TAG = "GitHubRepoFetcher"


        private const val MAX_DOWNLOAD_SIZE = 300L * 1024 * 1024


        private const val MAX_EXTRACTED_SIZE = 1024L * 1024 * 1024


        private const val MAX_ENTRY_COUNT = 50_000


        private val SKIP_PATTERNS = setOf(
            "__MACOSX", ".DS_Store", "Thumbs.db"
        )


        private val BRANCH_FALLBACKS = listOf("main", "master")


        private val SHORT_RE = Regex("^([\\w.-]+)/([\\w.-]+?)(?:\\.git)?$")
        private val SSH_RE = Regex("^git@github\\.com:([\\w.-]+)/([\\w.-]+?)(?:\\.git)?$")
        private val HTTPS_RE = Regex(
            "^https?://github\\.com/([\\w.-]+)/([\\w.-]+?)(?:\\.git)?(?:/tree/([^/]+)(?:/(.+))?)?/?$"
        )
        private val ARCHIVE_RE = Regex(
            "^https?://github\\.com/([\\w.-]+)/([\\w.-]+?)/archive/(?:refs/heads/)?([^/]+?)\\.zip$"
        )




        fun parseInput(raw: String): GitHubRepoSpec? {
            val input = raw.trim()
            if (input.isEmpty()) return null


            ARCHIVE_RE.matchEntire(input)?.let {
                return GitHubRepoSpec(
                    owner = it.groupValues[1],
                    repo = it.groupValues[2].removeSuffix(".git"),
                    branch = it.groupValues[3]
                )
            }


            HTTPS_RE.matchEntire(input)?.let {
                return GitHubRepoSpec(
                    owner = it.groupValues[1],
                    repo = it.groupValues[2].removeSuffix(".git"),
                    branch = it.groupValues[3].takeIf { v -> v.isNotEmpty() },
                    subPath = it.groupValues[4].takeIf { v -> v.isNotEmpty() }
                )
            }


            SSH_RE.matchEntire(input)?.let {
                return GitHubRepoSpec(
                    owner = it.groupValues[1],
                    repo = it.groupValues[2].removeSuffix(".git")
                )
            }


            SHORT_RE.matchEntire(input)?.let {
                if (input.contains("://") || input.startsWith("git@")) return@let
                return GitHubRepoSpec(
                    owner = it.groupValues[1],
                    repo = it.groupValues[2].removeSuffix(".git")
                )
            }

            return null
        }
    }
}
