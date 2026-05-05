package com.webtoapp.core.apkbuilder

import com.webtoapp.data.model.GalleryItem
import com.webtoapp.data.model.GalleryItemType
import com.webtoapp.data.model.HtmlFile
import java.io.File
import java.util.zip.ZipFile

internal data class ApkArtifactVerificationRequest(
    val apkFile: File,
    val config: ApkConfig,
    val encryptionEnabled: Boolean,
    val htmlFiles: List<HtmlFile> = emptyList(),
    val galleryItems: List<GalleryItem> = emptyList(),
    val wordPressProjectDir: File? = null,
    val nodejsProjectDir: File? = null,
    val phpAppProjectDir: File? = null,
    val pythonAppProjectDir: File? = null,
    val goAppProjectDir: File? = null,
    val frontendProjectDir: File? = null
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
                        config = RuntimeAssetEmbedder.pythonConfig()
                    )
                    "GO_APP" -> issues.requireProjectAssets(
                        entries = entries,
                        checkedEntries = checkedEntries,
                        key = "goAppProject",
                        label = "Go app project",
                        projectDir = request.goAppProjectDir,
                        config = RuntimeAssetEmbedder.goConfig()
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
            requireEntry(
                entries = entries,
                checkedEntries = checkedEntries,
                key = "$key[$index]",
                path = path,
                label = "$label file",
                allowEmpty = path in allowEmptyAssetPaths
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

    private fun normalizeAssetPath(value: String): String {
        return value.trim().replace('\\', '/').trimStart('/')
    }

    private val WORDPRESS_ALLOWED_EMPTY_ASSET_PATHS = setOf(
        "assets/wordpress/wp-includes/js/swfobject.js",
        "assets/wordpress/wp-includes/js/swfupload/handlers.js",
        "assets/wordpress/wp-includes/js/swfupload/handlers.min.js",
        "assets/wordpress/wp-includes/js/swfupload/license.txt",
        "assets/wordpress/wp-includes/js/swfupload/swfupload.js"
    )
}
