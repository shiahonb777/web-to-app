package com.webtoapp.core.apkbuilder

import com.webtoapp.data.model.GalleryItem
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.NetworkTrustConfig
import com.webtoapp.util.NetworkTrustStorage
import java.io.File

data class BuildInputPreflightRequest(
    val appType: String,
    val htmlEntryFile: String = "index.html",
    val mediaContentPath: String? = null,
    val htmlFiles: List<HtmlFile> = emptyList(),
    val galleryItems: List<GalleryItem> = emptyList(),
    val wordPressProjectDir: File? = null,
    val nodejsProjectDir: File? = null,
    val phpAppProjectDir: File? = null,
    val pythonAppProjectDir: File? = null,
    val goAppProjectDir: File? = null,
    val frontendProjectDir: File? = null,
    val networkTrustConfig: NetworkTrustConfig = NetworkTrustConfig(),




    val phpBinaryPath: String? = null,
    val nodeBinaryPath: String? = null,
    val pythonBinaryPath: String? = null,
    val muslLinkerPath: String? = null
)

data class BuildInputPreflightResult(
    val issues: List<BuildInputIssue>
) {
    val passed: Boolean get() = issues.isEmpty()
}

data class BuildInputIssue(
    val key: String,
    val message: String,
    val path: String? = null
) {
    fun summary(): String {
        return if (path.isNullOrBlank()) "$key: $message" else "$key: $message [$path]"
    }
}

object BuildInputPreflight {

    fun check(request: BuildInputPreflightRequest): BuildInputPreflightResult {
        val issues = mutableListOf<BuildInputIssue>()

        when (request.appType) {
            "IMAGE", "VIDEO" -> {
                issues.requireReadableFile(
                    key = "mediaContentPath",
                    label = "${request.appType.lowercase()} content",
                    path = request.mediaContentPath,
                    requireNonEmpty = true
                )
            }
            "HTML" -> {
                issues.requireHtmlFiles(request.htmlEntryFile, request.htmlFiles)
            }
            "GALLERY" -> {
                issues.requireGalleryItems(request.galleryItems)
            }
            "FRONTEND" -> {
                if (request.frontendProjectDir?.let { it.exists() && it.isDirectory && it.canRead() } != true) {
                    issues.requireHtmlFiles(
                        entryFile = request.htmlEntryFile,
                        htmlFiles = request.htmlFiles,
                        label = "Frontend app"
                    )
                }
            }
            "WORDPRESS" -> {
                issues.requireReadableDirectory(
                    key = "wordPressProjectDir",
                    label = "WordPress project directory",
                    dir = request.wordPressProjectDir
                )
                issues.requireNativeBinary(
                    key = "phpBinary",
                    label = "PHP runtime (libphp.so)",
                    path = request.phpBinaryPath,
                    minSize = 1024L * 1024L,
                    hint = "PHP runtime not initialized; open Settings → Runtime Engines and enable PHP"
                )
            }
            "NODEJS_APP" -> {
                issues.requireReadableDirectory(
                    key = "nodejsProjectDir",
                    label = "Node.js project directory",
                    dir = request.nodejsProjectDir
                )
                issues.requireNativeBinary(
                    key = "nodeBinary",
                    label = "Node.js runtime (libnode.so)",
                    path = request.nodeBinaryPath,
                    minSize = 1024L * 1024L,
                    hint = "Node.js runtime not downloaded; open Settings → Runtime Engines and download Node.js"
                )
            }
            "PHP_APP" -> {
                issues.requireReadableDirectory(
                    key = "phpAppProjectDir",
                    label = "PHP app project directory",
                    dir = request.phpAppProjectDir
                )
                issues.requireNativeBinary(
                    key = "phpBinary",
                    label = "PHP runtime (libphp.so)",
                    path = request.phpBinaryPath,
                    minSize = 1024L * 1024L,
                    hint = "PHP runtime not initialized; open Settings → Runtime Engines and enable PHP"
                )
            }
            "PYTHON_APP" -> {
                issues.requireReadableDirectory(
                    key = "pythonAppProjectDir",
                    label = "Python app project directory",
                    dir = request.pythonAppProjectDir
                )
                issues.requireNativeBinary(
                    key = "pythonBinary",
                    label = "Python runtime (libpython3.so)",
                    path = request.pythonBinaryPath,
                    minSize = 1024L * 1024L,
                    hint = "Python runtime not downloaded; open Settings → Runtime Engines and download Python"
                )
                issues.requireNativeBinary(
                    key = "muslLinker",
                    label = "musl linker (libmusl-linker.so)",
                    path = request.muslLinkerPath,
                    minSize = 1024L,
                    hint = "Python musl linker missing; re-download Python runtime in Settings → Runtime Engines"
                )
            }
            "GO_APP" -> {
                issues.requireReadableDirectory(
                    key = "goAppProjectDir",
                    label = "Go app project directory",
                    dir = request.goAppProjectDir
                )
            }
        }

        issues.requireCustomCaCertificates(request.networkTrustConfig)

        return BuildInputPreflightResult(issues)
    }

    private fun MutableList<BuildInputIssue>.requireCustomCaCertificates(config: NetworkTrustConfig) {
        config.customCaCertificates.forEachIndexed { index, cert ->
            val file = File(cert.filePath)
            when {
                cert.filePath.isBlank() -> add(BuildInputIssue("customCa[$index]", "Custom CA path is blank"))
                !file.exists() -> add(BuildInputIssue("customCa[$index]", "Custom CA file does not exist", file.absolutePath))
                !file.isFile -> add(BuildInputIssue("customCa[$index]", "Custom CA path is not a file", file.absolutePath))
                !file.canRead() -> add(BuildInputIssue("customCa[$index]", "Custom CA file cannot be read", file.absolutePath))
                !NetworkTrustStorage.validateCertificateFile(file.absolutePath) -> {
                    add(BuildInputIssue("customCa[$index]", "Custom CA file is not a valid X.509 certificate", file.absolutePath))
                }
            }
        }
    }

    private fun MutableList<BuildInputIssue>.requireHtmlFiles(
        entryFile: String,
        htmlFiles: List<HtmlFile>,
        label: String = "HTML app"
    ) {
        if (htmlFiles.isEmpty()) {
            add(BuildInputIssue("htmlFiles", "$label has no files to embed"))
            return
        }

        val normalizedEntry = normalizeAssetPath(entryFile)
        if (normalizedEntry.isBlank()) {
            add(BuildInputIssue("htmlEntryFile", "HTML entry file is blank"))
        } else {
            val entryFound = htmlFiles.any { normalizeAssetPath(it.name).equals(normalizedEntry, ignoreCase = true) }
            if (!entryFound) {
                add(BuildInputIssue("htmlEntryFile", "HTML entry file is not included in htmlFiles", entryFile))
            }
        }

        htmlFiles.forEachIndexed { index, file ->
            requireReadableFile(
                key = "htmlFiles[$index]",
                label = "HTML project file '${file.name.ifBlank { "(unnamed)" }}'",
                path = file.path,
                requireNonEmpty = true
            )
        }
    }

    private fun MutableList<BuildInputIssue>.requireGalleryItems(items: List<GalleryItem>) {
        if (items.isEmpty()) {
            add(BuildInputIssue("galleryItems", "Gallery app has no media items to embed"))
            return
        }

        items.forEachIndexed { index, item ->
            requireReadableFile(
                key = "galleryItems[$index]",
                label = "Gallery item '${item.name.ifBlank { item.id }}'",
                path = item.path,
                requireNonEmpty = true
            )
        }
    }

    private fun MutableList<BuildInputIssue>.requireReadableFile(
        key: String,
        label: String,
        path: String?,
        requireNonEmpty: Boolean
    ) {
        val trimmedPath = path?.trim().orEmpty()
        if (trimmedPath.isBlank()) {
            add(BuildInputIssue(key, "$label path is blank"))
            return
        }

        val file = File(trimmedPath)
        when {
            !file.exists() -> add(BuildInputIssue(key, "$label does not exist", file.absolutePath))
            !file.isFile -> add(BuildInputIssue(key, "$label is not a file", file.absolutePath))
            !file.canRead() -> add(BuildInputIssue(key, "$label cannot be read", file.absolutePath))
            requireNonEmpty && file.length() == 0L -> add(BuildInputIssue(key, "$label is empty", file.absolutePath))
        }
    }

    private fun MutableList<BuildInputIssue>.requireNativeBinary(
        key: String,
        label: String,
        path: String?,
        minSize: Long,
        hint: String
    ) {
        val trimmed = path?.trim().orEmpty()
        if (trimmed.isBlank()) {
            add(BuildInputIssue(key, "$label not available — $hint"))
            return
        }
        val file = File(trimmed)
        when {
            !file.exists() -> add(BuildInputIssue(key, "$label not installed — $hint", file.absolutePath))
            !file.isFile -> add(BuildInputIssue(key, "$label path is not a file — $hint", file.absolutePath))
            !file.canRead() -> add(BuildInputIssue(key, "$label cannot be read — $hint", file.absolutePath))
            file.length() < minSize -> add(
                BuildInputIssue(
                    key,
                    "$label is smaller than expected (${file.length()} bytes); likely corrupted — $hint",
                    file.absolutePath
                )
            )
        }
    }


    private fun MutableList<BuildInputIssue>.requireReadableDirectory(
        key: String,
        label: String,
        dir: File?
    ) {
        when {
            dir == null -> add(BuildInputIssue(key, "$label was not resolved"))
            !dir.exists() -> add(BuildInputIssue(key, "$label does not exist", dir.absolutePath))
            !dir.isDirectory -> add(BuildInputIssue(key, "$label is not a directory", dir.absolutePath))
            !dir.canRead() -> add(BuildInputIssue(key, "$label cannot be read", dir.absolutePath))
        }
    }

    private fun normalizeAssetPath(value: String): String {
        return value.trim().replace('\\', '/').trimStart('/')
    }
}
