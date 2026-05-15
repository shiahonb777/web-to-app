package com.webtoapp.core.apkbuilder

import com.webtoapp.data.model.GalleryItem
import com.webtoapp.data.model.GalleryItemType
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.MultiWebSite
import java.io.File
import java.util.zip.ZipFile

internal data class ApkArtifactVerificationRequest(
    val apkFile: File,
    val config: ApkConfig,
    val encryptionEnabled: Boolean,
    val htmlFiles: List<HtmlFile> = emptyList(),
    val galleryItems: List<GalleryItem> = emptyList(),
    val multiWebSites: List<MultiWebSite> = emptyList(),
    val wordPressProjectDir: File? = null,
    val nodejsProjectDir: File? = null,
    val phpAppProjectDir: File? = null,
    val pythonAppProjectDir: File? = null,
    val goAppProjectDir: File? = null,
    val frontendProjectDir: File? = null,
    val multiWebProjectDir: File? = null
)

internal data class ApkArtifactVerificationResult(
    val issues: List<ApkArtifactIssue>,
    val entryCount: Int = 0,
    val checkedEntryCount: Int = 0
) {
    val passed: Boolean get() = issues.isEmpty()
}

internal data class ApkArtifactIssue(
    val key: String,
    val message: String,
    val path: String? = null
) {
    fun summary(): String {
        return if (path.isNullOrBlank()) "$key: $message" else "$key: $message [$path]"
    }
}

internal object ApkArtifactVerifier {

    fun verify(request: ApkArtifactVerificationRequest): ApkArtifactVerificationResult {
        if (!request.apkFile.exists()) {
            return ApkArtifactVerificationResult(
                issues = listOf(
                    ApkArtifactIssue("apkFile", "APK artifact does not exist", request.apkFile.absolutePath)
                )
            )
        }

        if (!request.apkFile.canRead()) {
            return ApkArtifactVerificationResult(
                issues = listOf(
                    ApkArtifactIssue("apkFile", "APK artifact cannot be read", request.apkFile.absolutePath)
                )
            )
        }

        return try {
            ZipFile(request.apkFile).use { zip ->
                val entries = zip.entries().asSequence()
                    .filterNot { it.isDirectory }
                    .associateBy { it.name }
                val issues = mutableListOf<ApkArtifactIssue>()
                val checkedEntries = mutableSetOf<String>()

                issues.requireEntry(
                    entries = entries,
                    checkedEntries = checkedEntries,
                    key = "config",
                    path = ApkTemplate.CONFIG_PATH,
                    label = "app config"
                )

                if (request.encryptionEnabled) {
                    issues.requireEntry(
                        entries = entries,
                        checkedEntries = checkedEntries,
                        key = "encryptedConfig",
                        path = "${ApkTemplate.CONFIG_PATH}.enc",
                        label = "encrypted app config"
                    )
                }

                when (request.config.appType) {
                    "IMAGE" -> {
                        issues.requireAsset(
                            entries = entries,
                            checkedEntries = checkedEntries,
                            key = "mediaContent",
                            path = "assets/media_content.png",
                            label = "image media content",
                            encrypted = request.encryptionEnabled
                        )
                    }
                    "VIDEO" -> {
                        issues.requireAsset(
                            entries = entries,
                            checkedEntries = checkedEntries,
                            key = "mediaContent",
                            path = "assets/media_content.mp4",
                            label = "video media content",
                            encrypted = request.encryptionEnabled
                        )
                    }
                    "HTML" -> {
                        issues.requireAsset(
                            entries = entries,
                            checkedEntries = checkedEntries,
                            key = "htmlEntryFile",
                            path = "assets/html/${normalizeAssetPath(request.config.htmlEntryFile)}",
                            label = "HTML entry file",
                            encrypted = request.encryptionEnabled
                        )
                    }
                    "GALLERY" -> {
                        request.galleryItems.forEachIndexed { index, item ->
                            val ext = if (item.type == GalleryItemType.VIDEO) "mp4" else "png"
                            issues.requireAsset(
                                entries = entries,
                                checkedEntries = checkedEntries,
                                key = "galleryItems[$index]",
                                path = "assets/gallery/item_$index.$ext",
                                label = "gallery item ${index + 1}",
                                encrypted = request.encryptionEnabled
                            )
                        }
                    }
                    "FRONTEND" -> {
                        if (request.frontendProjectDir != null) {
                            issues.requireProjectAssets(
                                entries = entries,
                                checkedEntries = checkedEntries,
                                key = "frontendProject",
                                label = "frontend project",
                                projectDir = request.frontendProjectDir,
                                config = RuntimeAssetEmbedder.frontendConfig()
                            )
                        } else {
                            issues.requireAsset(
                                entries = entries,
                                checkedEntries = checkedEntries,
                                key = "frontendEntryFile",
                                path = "assets/html/${normalizeAssetPath(request.config.htmlEntryFile)}",
                                label = "frontend entry file",
                                encrypted = request.encryptionEnabled
                            )
                        }
                    }
                    "WORDPRESS" -> issues.requireProjectAssets(
                        entries = entries,
                        checkedEntries = checkedEntries,
                        key = "wordPressProject",
                        label = "WordPress project",
                        projectDir = request.wordPressProjectDir,
                        assetPrefix = "assets/wordpress",
                        excludeDirs = emptySet(),
                        allowEmptyAssetPaths = WORDPRESS_ALLOWED_EMPTY_ASSET_PATHS
                    )
                    "NODEJS_APP" -> issues.requireProjectAssets(
                        entries = entries,
                        checkedEntries = checkedEntries,
                        key = "nodejsProject",
                        label = "Node.js project",
                        projectDir = request.nodejsProjectDir,
                        config = RuntimeAssetEmbedder.nodeJsConfig()
                    )
                    "PHP_APP" -> issues.requireProjectAssets(
                        entries = entries,
                        checkedEntries = checkedEntries,
                        key = "phpAppProject",
                        label = "PHP app project",
                        projectDir = request.phpAppProjectDir,
                        config = RuntimeAssetEmbedder.phpConfig()
                    )
                    "PYTHON_APP" -> issues.requireProjectAssets(
                        entries = entries,
                        checkedEntries = checkedEntries,
                        key = "pythonAppProject",
                        label = "Python app project",
                        projectDir = request.pythonAppProjectDir,
                        assetPrefix = RuntimeAssetEmbedder.pythonConfig().assetPrefix,
                        excludeDirs = RuntimeAssetEmbedder.pythonConfig().excludeDirs,
                        allowEmptyAssetPaths = PYTHON_ALLOWED_EMPTY_ASSET_PATHS
                    )
                    "GO_APP" -> issues.requireProjectAssets(
                        entries = entries,
                        checkedEntries = checkedEntries,
                        key = "goAppProject",
                        label = "Go app project",
                        projectDir = request.goAppProjectDir,
                        config = RuntimeAssetEmbedder.goConfig()
                    )
                    "MULTI_WEB" -> issues.requireMultiWebAssets(
                        entries = entries,
                        checkedEntries = checkedEntries,
                        sites = request.multiWebSites,
                        projectDir = request.multiWebProjectDir
                    )
                }

                ApkArtifactVerificationResult(
                    issues = issues,
                    entryCount = entries.size,
                    checkedEntryCount = checkedEntries.size
                )
            }
        } catch (e: Exception) {
            ApkArtifactVerificationResult(
                issues = listOf(
                    ApkArtifactIssue(
                        key = "apkFile",
                        message = "APK artifact could not be opened: ${e.message ?: e::class.java.simpleName}",
                        path = request.apkFile.absolutePath
                    )
                )
            )
        }
    }

    private fun MutableList<ApkArtifactIssue>.requireAsset(
        entries: Map<String, java.util.zip.ZipEntry>,
        checkedEntries: MutableSet<String>,
        key: String,
        path: String,
        label: String,
        encrypted: Boolean
    ) {
        val artifactPath = if (encrypted) "$path.enc" else path
        requireEntry(entries, checkedEntries, key, artifactPath, label)
    }

    private fun MutableList<ApkArtifactIssue>.requireEntry(
        entries: Map<String, java.util.zip.ZipEntry>,
        checkedEntries: MutableSet<String>,
        key: String,
        path: String,
        label: String,
        allowEmpty: Boolean = false
    ) {
        checkedEntries += path
        val entry = entries[path]
        when {
            entry == null -> add(ApkArtifactIssue(key, "$label is missing from APK", path))
            entry.size == 0L && !allowEmpty -> add(ApkArtifactIssue(key, "$label is empty in APK", path))
        }
    }

    private fun MutableList<ApkArtifactIssue>.requireProjectAssets(
        entries: Map<String, java.util.zip.ZipEntry>,
        checkedEntries: MutableSet<String>,
        key: String,
        label: String,
        projectDir: File?,
        config: RuntimeAssetEmbedder.EmbedConfig
    ) {
        requireProjectAssets(
            entries = entries,
            checkedEntries = checkedEntries,
            key = key,
            label = label,
            projectDir = projectDir,
            assetPrefix = config.assetPrefix,
            excludeDirs = config.excludeDirs
        )
    }

    private fun MutableList<ApkArtifactIssue>.requireProjectAssets(
        entries: Map<String, java.util.zip.ZipEntry>,
        checkedEntries: MutableSet<String>,
        key: String,
        label: String,
        projectDir: File?,
        assetPrefix: String,
        excludeDirs: Set<String>,
        allowEmptyAssetPaths: Set<String> = emptySet()
    ) {
        if (projectDir == null) {
            add(ApkArtifactIssue(key, "$label directory was not resolved"))
            return
        }

        val expectedPaths = collectProjectAssetPaths(projectDir, assetPrefix, excludeDirs)
        if (expectedPaths.isEmpty()) {
            add(ApkArtifactIssue(key, "$label has no project files to verify", projectDir.absolutePath))
            return
        }

        expectedPaths.forEachIndexed { index, path ->
            val sourceFile = resolveSourceFileForAssetPath(projectDir, assetPrefix, path)
            requireEntry(
                entries = entries,
                checkedEntries = checkedEntries,
                key = "$key[$index]",
                path = path,
                label = "$label file",
                allowEmpty = path in allowEmptyAssetPaths ||
                    isAllowedEmptyPythonAssetPath(path) ||
                    sourceFile?.length() == 0L
            )
        }
    }

    private fun collectProjectAssetPaths(
        projectDir: File,
        assetPrefix: String,
        excludeDirs: Set<String>
    ): List<String> {
        if (!projectDir.exists() || !projectDir.isDirectory) return emptyList()

        val basePrefix = assetPrefix.trimEnd('/')
        return projectDir.walkTopDown()
            .onEnter { dir -> dir == projectDir || dir.name !in excludeDirs }
            .filter { it.isFile }
            .map { file ->
                val relativePath = file.relativeTo(projectDir).invariantSeparatorsPath
                "$basePrefix/$relativePath"
            }
            .toList()
    }

    private fun resolveSourceFileForAssetPath(
        projectDir: File,
        assetPrefix: String,
        assetPath: String
    ): File? {
        val prefix = assetPrefix.trimEnd('/') + "/"
        if (!assetPath.startsWith(prefix)) return null
        val relativePath = assetPath.removePrefix(prefix)
        if (relativePath.isBlank()) return null
        return File(projectDir, relativePath)
    }

    private fun MutableList<ApkArtifactIssue>.requireMultiWebAssets(
        entries: Map<String, java.util.zip.ZipEntry>,
        checkedEntries: MutableSet<String>,
        sites: List<MultiWebSite>,
        projectDir: File?
    ) {
        val localSites = sites.filter {
            it.enabled && it.type.uppercase() != "URL" && it.localFilePath.isNotBlank()
        }
        if (localSites.isEmpty()) return

        if (projectDir == null) {
            add(ApkArtifactIssue("multiWebProject", "Multi-web project directory was not resolved"))
            return
        }

        localSites.forEachIndexed { index, site ->
            val relativePath = normalizeAssetPath(site.localFilePath)
            val expectedSource = File(projectDir, relativePath)
            if (!expectedSource.exists() || !expectedSource.isFile) {
                add(
                    ApkArtifactIssue(
                        key = "multiWebSites[$index]",
                        message = "Multi-web local site source file is missing",
                        path = expectedSource.absolutePath
                    )
                )
                return@forEachIndexed
            }

            requireEntry(
                entries = entries,
                checkedEntries = checkedEntries,
                key = "multiWebSites[$index]",
                path = "assets/html_projects/$relativePath",
                label = "multi-web local site file"
            )
        }
    }

    private fun normalizeAssetPath(value: String): String {
        return value.trim().replace('\\', '/').trimStart('/')
    }

    private fun isAllowedEmptyPythonAssetPath(path: String): Boolean {
        if (!path.startsWith("assets/python_app/.pypackages/")) return false
        return path.endsWith("/__init__.py") ||
            path.endsWith("/__init__.py-tpl") ||
            path.endsWith("/py.typed") ||
            path.endsWith("/REQUESTED")
    }

    private val WORDPRESS_ALLOWED_EMPTY_ASSET_PATHS = setOf(
        "assets/wordpress/wp-includes/js/swfobject.js",
        "assets/wordpress/wp-includes/js/swfupload/handlers.js",
        "assets/wordpress/wp-includes/js/swfupload/handlers.min.js",
        "assets/wordpress/wp-includes/js/swfupload/license.txt",
        "assets/wordpress/wp-includes/js/swfupload/swfupload.js"
    )

    private val PYTHON_ALLOWED_EMPTY_ASSET_PATHS = setOf(
        "assets/python_app/.pypackages/anyio/_backends/__init__.py",
        "assets/python_app/.pypackages/anyio/_core/__init__.py",
        "assets/python_app/.pypackages/anyio/py.typed",
        "assets/python_app/.pypackages/blinker/py.typed",
        "assets/python_app/.pypackages/click/py.typed",
        "assets/python_app/.pypackages/exceptiongroup/py.typed",
        "assets/python_app/.pypackages/fastapi-0.99.1.dist-info/REQUESTED",
        "assets/python_app/.pypackages/fastapi/dependencies/__init__.py",
        "assets/python_app/.pypackages/fastapi/openapi/__init__.py",
        "assets/python_app/.pypackages/fastapi/py.typed",
        "assets/python_app/.pypackages/flask/py.typed",
        "assets/python_app/.pypackages/idna/py.typed",
        "assets/python_app/.pypackages/itsdangerous/py.typed",
        "assets/python_app/.pypackages/jinja2/py.typed",
        "assets/python_app/.pypackages/Django-5.0.dist-info/REQUESTED",
        "assets/python_app/.pypackages/asgiref/py.typed",
        "assets/python_app/.pypackages/markupsafe/py.typed",
        "assets/python_app/.pypackages/packaging/py.typed",
        "assets/python_app/.pypackages/pydantic-1.10.16.dist-info/REQUESTED",
        "assets/python_app/.pypackages/pydantic/py.typed",
        "assets/python_app/.pypackages/sqlparse/py.typed",
        "assets/python_app/.pypackages/starlette/py.typed",
        "assets/python_app/.pypackages/gunicorn-21.2.0.dist-info/REQUESTED",
        "assets/python_app/.pypackages/uvicorn-0.23.2.dist-info/REQUESTED",
        "assets/python_app/.pypackages/uvicorn/lifespan/__init__.py",
        "assets/python_app/.pypackages/uvicorn/loops/__init__.py",
        "assets/python_app/.pypackages/uvicorn/middleware/__init__.py",
        "assets/python_app/.pypackages/uvicorn/protocols/__init__.py",
        "assets/python_app/.pypackages/uvicorn/protocols/http/__init__.py",
        "assets/python_app/.pypackages/uvicorn/protocols/websockets/__init__.py",
        "assets/python_app/.pypackages/werkzeug/middleware/__init__.py",
        "assets/python_app/.pypackages/werkzeug/py.typed",
        "assets/python_app/.pypackages/werkzeug/sansio/__init__.py"
    )
}
