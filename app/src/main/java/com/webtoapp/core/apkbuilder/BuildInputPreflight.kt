package com.webtoapp.core.apkbuilder

import com.webtoapp.data.model.GalleryItem
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.MultiWebSite
import com.webtoapp.data.model.NetworkTrustConfig
import com.webtoapp.util.NetworkTrustStorage
import java.io.File

data class BuildInputPreflightRequest(
    val appType: String,
    val htmlEntryFile: String = "index.html",
    val mediaContentPath: String? = null,
    val htmlFiles: List<HtmlFile> = emptyList(),
    val galleryItems: List<GalleryItem> = emptyList(),
    val multiWebSites: List<MultiWebSite> = emptyList(),
    val wordPressProjectDir: File? = null,
    val nodejsProjectDir: File? = null,
    val phpAppProjectDir: File? = null,
    val pythonAppProjectDir: File? = null,
    val goAppProjectDir: File? = null,
    val frontendProjectDir: File? = null,
    val multiWebProjectDir: File? = null,
    val networkTrustConfig: NetworkTrustConfig = NetworkTrustConfig(),




    val phpBinaryPath: String? = null,
    val nodeBinaryPath: String? = null,
    val pythonBinaryPath: String? = null,
    val muslLinkerPath: String? = null,
    val builderMuslLinkerPath: String? = null
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
                if (request.pythonAppProjectDir.requiresPythonDependencyPrebundle()) {
                    issues.requireNativeBinary(
                        key = "pythonPrebundleMuslLinker",
                        label = "build-time musl linker (executable)",
                        path = request.builderMuslLinkerPath,
                        minSize = 1024L,
                        hint = "This Python project still needs requirements pre-bundling; the current builder app cannot execute pip safely. Pre-populate .pypackages or use a build with bundled Python runtime."
                    )
                }
            }
            "GO_APP" -> {
                issues.requireReadableDirectory(
                    key = "goAppProjectDir",
                    label = "Go app project directory",
                    dir = request.goAppProjectDir
                )
                request.goAppProjectDir?.let { dir ->
                    val binaryName = detectGoBinaryName(dir)
                    val binaryPath = binaryName?.let {
                        com.webtoapp.core.golang.GoDependencyManager.findBinaryPath(dir, it)
                    } ?: com.webtoapp.core.golang.GoDependencyManager.detectAnyCompatibleBinary(dir)?.absolutePath

                    issues.requireNativeBinary(
                        key = "goBinary",
                        label = "Go executable binary",
                        path = binaryPath,
                        minSize = 1024L,
                        hint = "GO_APP export now requires a prebuilt binary. Build the target-ABI binary first, then export."
                    )
                }
            }
            "MULTI_WEB" -> {
                issues.requireMultiWebSites(
                    items = request.multiWebSites,
                    projectDir = request.multiWebProjectDir
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

    private fun MutableList<BuildInputIssue>.requireMultiWebSites(
        items: List<MultiWebSite>,
        projectDir: File?
    ) {
        val enabledSites = items.filter { it.enabled }
        if (enabledSites.isEmpty()) {
            add(BuildInputIssue("multiWebSites", "Multi-web app has no enabled sites"))
            return
        }

        var requiresLocalProject = false
        enabledSites.forEachIndexed { index, site ->
            val siteType = site.type.uppercase()
            when {
                siteType == "URL" -> {
                    if (site.url.isBlank()) {
                        add(BuildInputIssue("multiWebSites[$index]", "Multi-web URL site is missing its URL"))
                    }
                }
                site.localFilePath.isBlank() -> {
                    add(BuildInputIssue("multiWebSites[$index]", "Multi-web local site is missing its file path"))
                }
                else -> {
                    requiresLocalProject = true
                }
            }
        }

        if (!requiresLocalProject) return

        if (projectDir == null) {
            add(BuildInputIssue("multiWebProjectDir", "Multi-web local site directory was not resolved"))
            return
        }

        enabledSites.forEachIndexed { index, site ->
            if (site.type.uppercase() == "URL" || site.localFilePath.isBlank()) return@forEachIndexed
            val expectedFile = File(projectDir, site.localFilePath.trimStart('/'))
            when {
                !expectedFile.exists() -> add(
                    BuildInputIssue(
                        "multiWebSites[$index]",
                        "Multi-web local site file does not exist",
                        expectedFile.absolutePath
                    )
                )
                !expectedFile.isFile -> add(
                    BuildInputIssue(
                        "multiWebSites[$index]",
                        "Multi-web local site path is not a file",
                        expectedFile.absolutePath
                    )
                )
                !expectedFile.canRead() -> add(
                    BuildInputIssue(
                        "multiWebSites[$index]",
                        "Multi-web local site file cannot be read",
                        expectedFile.absolutePath
                    )
                )
                expectedFile.length() == 0L -> add(
                    BuildInputIssue(
                        "multiWebSites[$index]",
                        "Multi-web local site file is empty",
                        expectedFile.absolutePath
                    )
                )
            }
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

    private fun File?.requiresPythonDependencyPrebundle(): Boolean {
        if (this == null || !exists() || !isDirectory) return false
        val requirements = File(this, "requirements.txt")
        if (!requirements.exists() || !requirements.isFile || !requirements.canRead()) return false
        return !com.webtoapp.core.python.PythonDependencyManager.hasInstalledPackages(File(this, ".pypackages"))
    }

    private fun detectGoBinaryName(projectDir: File): String? {
        val goMod = File(projectDir, "go.mod")
        if (goMod.exists()) {
            goMod.readLines().firstOrNull { it.startsWith("module ") }
                ?.substringAfter("module ")
                ?.trim()
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }
        return projectDir.name.takeIf { it.isNotBlank() }
    }

    private fun normalizeAssetPath(value: String): String {
        return value.trim().replace('\\', '/').trimStart('/')
    }
}
