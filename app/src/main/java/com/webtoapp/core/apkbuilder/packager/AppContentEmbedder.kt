package com.webtoapp.core.apkbuilder

import com.webtoapp.core.crypto.AssetEncryptor
import com.webtoapp.core.crypto.EncryptionConfig
import java.io.File
import java.util.zip.ZipOutputStream

/**
 * Note: brief English comment.
 *
 * Note: brief English comment.
 * Note: brief English comment.
 *
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 */
interface AppContentEmbedder {
    
    /**
     * Note: brief English comment.
     *
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    fun embed(zipOut: ZipOutputStream, ctx: EmbedContext): EmbedResult
}

/**
 * Note: brief English comment.
 *
 * Note: brief English comment.
 */
class EmbedContext(
    val config: ApkConfig,
    val logger: BuildLogger,
    val encryptor: AssetEncryptor?,
    val encryptionConfig: EncryptionConfig,
    // App-type-specific resources
    val mediaContentPath: String?,
    val htmlFiles: List<com.webtoapp.data.model.HtmlFile>,
    val galleryItems: List<com.webtoapp.data.model.GalleryItem>,
    val projectDir: File?,
    // Delegated ApkBuilder functions (avoid leaking ApkBuilder internals)
    val fnAddMediaContent: (ZipOutputStream, String, Boolean, AssetEncryptor?, EncryptionConfig) -> Unit,
    val fnAddHtmlFiles: (ZipOutputStream, List<com.webtoapp.data.model.HtmlFile>, AssetEncryptor?, EncryptionConfig) -> Int,
    val fnAddGalleryItems: (ZipOutputStream, List<com.webtoapp.data.model.GalleryItem>, AssetEncryptor?, EncryptionConfig) -> Unit,
    val fnAddWordPressFiles: (ZipOutputStream, File) -> Unit,
    val fnAddNodeJsFiles: (ZipOutputStream, File) -> Unit,
    val fnAddFrontendFiles: (ZipOutputStream, File, List<com.webtoapp.data.model.HtmlFile>) -> Unit,
    val fnAddPhpAppFiles: (ZipOutputStream, File) -> Unit,
    val fnAddPythonAppFiles: (ZipOutputStream, File) -> Unit,
    val fnAddGoAppFiles: (ZipOutputStream, File) -> Unit
)

/**
 * Note: brief English comment.
 */
data class EmbedResult(
    val success: Boolean,
    val itemCount: Int = 0,
    val message: String = ""
)

/**
 * Note: brief English comment.
 */
object AppContentEmbedderFactory {
    
    fun create(appType: String): AppContentEmbedder? {
        return when (appType) {
            "IMAGE", "VIDEO" -> MediaContentEmbedder()
            "HTML" -> HtmlContentEmbedder()
            "GALLERY" -> GalleryContentEmbedder()
            "WORDPRESS" -> WordPressContentEmbedder()
            "NODEJS_APP" -> NodeJsContentEmbedder()
            "FRONTEND" -> FrontendContentEmbedder()
            "PHP_APP" -> PhpAppContentEmbedder()
            "PYTHON_APP" -> PythonAppContentEmbedder()
            "GO_APP" -> GoAppContentEmbedder()
            "WEB" -> null  // WEB type has no app-specific content to embed
            else -> null
        }
    }
}

// ==================== Concrete Embedders ====================

/**
 * Note: brief English comment.
 */
class MediaContentEmbedder : AppContentEmbedder {
    override fun embed(zipOut: ZipOutputStream, ctx: EmbedContext): EmbedResult {
        val mediaPath = ctx.mediaContentPath ?: return EmbedResult(false, message = "No media content path")
        ctx.logger.log("Embedding single media content: $mediaPath")
        val isVideo = ctx.config.appType == "VIDEO"
        ctx.fnAddMediaContent(zipOut, mediaPath, isVideo, ctx.encryptor, ctx.encryptionConfig)
        return EmbedResult(true, 1, "Media content embedded")
    }
}

/**
 * Note: brief English comment.
 */
class HtmlContentEmbedder : AppContentEmbedder {
    override fun embed(zipOut: ZipOutputStream, ctx: EmbedContext): EmbedResult {
        if (ctx.htmlFiles.isEmpty()) {
            ctx.logger.warn("HTML app but htmlFiles is empty! htmlConfig=${ctx.config.htmlEntryFile}")
            return EmbedResult(false, message = "No HTML files")
        }
        ctx.logger.section("Embed HTML Files")
        val count = ctx.fnAddHtmlFiles(zipOut, ctx.htmlFiles, ctx.encryptor, ctx.encryptionConfig)
        ctx.logger.logKeyValue("htmlFilesEmbeddedCount", count)
        if (count == 0) {
            ctx.logger.warn("HTML app failed to embed any files!")
        }
        return EmbedResult(count > 0, count, "$count HTML files embedded")
    }
}

/**
 * Note: brief English comment.
 */
class GalleryContentEmbedder : AppContentEmbedder {
    override fun embed(zipOut: ZipOutputStream, ctx: EmbedContext): EmbedResult {
        if (ctx.galleryItems.isEmpty()) {
            ctx.logger.warn("Gallery app but galleryItems is empty!")
            return EmbedResult(false, message = "No gallery items")
        }
        ctx.logger.section("Embed Gallery Items")
        ctx.fnAddGalleryItems(zipOut, ctx.galleryItems, ctx.encryptor, ctx.encryptionConfig)
        ctx.logger.logKeyValue("galleryItemsEmbeddedCount", ctx.galleryItems.size)
        return EmbedResult(true, ctx.galleryItems.size, "${ctx.galleryItems.size} gallery items embedded")
    }
}

/**
 * Note: brief English comment.
 */
class WordPressContentEmbedder : AppContentEmbedder {
    override fun embed(zipOut: ZipOutputStream, ctx: EmbedContext): EmbedResult {
        val dir = ctx.projectDir
        if (dir == null || !dir.exists()) {
            ctx.logger.warn("WordPress app but project directory missing!")
            return EmbedResult(false, message = "Project directory missing")
        }
        ctx.logger.section("Embed WordPress Files")
        ctx.fnAddWordPressFiles(zipOut, dir)
        return EmbedResult(true, message = "WordPress files embedded")
    }
}

/**
 * Note: brief English comment.
 */
class NodeJsContentEmbedder : AppContentEmbedder {
    override fun embed(zipOut: ZipOutputStream, ctx: EmbedContext): EmbedResult {
        val dir = ctx.projectDir
        if (dir == null || !dir.exists()) {
            ctx.logger.warn("Node.js app but project directory missing!")
            return EmbedResult(false, message = "Project directory missing")
        }
        ctx.logger.section("Embed Node.js Files")
        ctx.fnAddNodeJsFiles(zipOut, dir)
        return EmbedResult(true, message = "Node.js files embedded")
    }
}

/**
 * Note: brief English comment.
 */
class FrontendContentEmbedder : AppContentEmbedder {
    override fun embed(zipOut: ZipOutputStream, ctx: EmbedContext): EmbedResult {
        val dir = ctx.projectDir
        if (dir != null && dir.exists()) {
            ctx.logger.section("Embed Frontend Project Files")
            ctx.fnAddFrontendFiles(zipOut, dir, ctx.htmlFiles)
            return EmbedResult(true, message = "Frontend files embedded")
        }
        // Fallback: use htmlFiles list if projectDir not set
        if (ctx.htmlFiles.isNotEmpty()) {
            ctx.logger.section("Embed Frontend Files (from file list)")
            val count = ctx.fnAddHtmlFiles(zipOut, ctx.htmlFiles, ctx.encryptor, ctx.encryptionConfig)
            ctx.logger.logKeyValue("frontendFilesEmbeddedCount", count)
            return EmbedResult(count > 0, count, "$count frontend files embedded (fallback)")
        }
        return EmbedResult(false, message = "No frontend project directory or files")
    }
}

/**
 * Note: brief English comment.
 */
class PhpAppContentEmbedder : AppContentEmbedder {
    override fun embed(zipOut: ZipOutputStream, ctx: EmbedContext): EmbedResult {
        val dir = ctx.projectDir
        if (dir == null || !dir.exists()) {
            ctx.logger.warn("PHP app but project directory missing!")
            return EmbedResult(false, message = "Project directory missing")
        }
        ctx.logger.section("Embed PHP App Files")
        ctx.fnAddPhpAppFiles(zipOut, dir)
        return EmbedResult(true, message = "PHP app files embedded")
    }
}

/**
 * Note: brief English comment.
 */
class PythonAppContentEmbedder : AppContentEmbedder {
    override fun embed(zipOut: ZipOutputStream, ctx: EmbedContext): EmbedResult {
        val dir = ctx.projectDir
        if (dir == null || !dir.exists()) {
            ctx.logger.warn("Python app but project directory missing!")
            return EmbedResult(false, message = "Project directory missing")
        }
        ctx.logger.section("Embed Python App Files")
        ctx.fnAddPythonAppFiles(zipOut, dir)
        return EmbedResult(true, message = "Python app files embedded")
    }
}

/**
 * Note: brief English comment.
 */
class GoAppContentEmbedder : AppContentEmbedder {
    override fun embed(zipOut: ZipOutputStream, ctx: EmbedContext): EmbedResult {
        val dir = ctx.projectDir
        if (dir == null || !dir.exists()) {
            ctx.logger.warn("Go app but project directory missing!")
            return EmbedResult(false, message = "Project directory missing")
        }
        ctx.logger.section("Embed Go App Files")
        ctx.fnAddGoAppFiles(zipOut, dir)
        return EmbedResult(true, message = "Go app files embedded")
    }
}
