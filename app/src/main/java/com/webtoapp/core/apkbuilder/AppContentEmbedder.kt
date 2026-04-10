package com.webtoapp.core.apkbuilder

import com.webtoapp.core.crypto.AssetEncryptor
import com.webtoapp.core.crypto.EncryptionConfig
import java.io.File
import java.util.zip.ZipOutputStream

/**
 * 应用内容嵌入策略接口
 *
 * 将 modifyApk 中按 AppType 分支的嵌入逻辑拆分为独立策略，
 * 每种 AppType 实现自己的 embed() 方法，消除 modifyApk 中的长 if-else 链。
 *
 * 设计原则：
 * - 每个策略只关注自己类型的资源嵌入
 * - 公共资源（splash、BGM、status bar bg）由调用方统一处理，不在策略内
 * - 策略通过 EmbedContext 获取所有需要的依赖和函数引用
 */
interface AppContentEmbedder {
    
    /**
     * 将应用特定内容嵌入到 APK 的 ZipOutputStream 中
     *
     * @param zipOut APK 输出流
     * @param ctx 嵌入上下文（包含所有可能需要的资源引用和 ApkBuilder 委托函数）
     * @return 嵌入结果
     */
    fun embed(zipOut: ZipOutputStream, ctx: EmbedContext): EmbedResult
}

/**
 * 嵌入上下文 — 传递给策略的数据和 ApkBuilder 委托函数
 *
 * 函数引用（fn*）指向 ApkBuilder 的 private 方法，避免暴露 ApkBuilder 内部实现。
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
 * 嵌入结果
 */
data class EmbedResult(
    val success: Boolean,
    val itemCount: Int = 0,
    val message: String = ""
)

/**
 * 策略工厂 — 根据 AppType 选择对应的嵌入策略
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
 * 媒体内容嵌入（IMAGE/VIDEO 类型）
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
 * HTML 文件嵌入
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
 * Gallery 内容嵌入
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
 * WordPress 项目嵌入
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
 * Node.js 项目嵌入
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
 * Frontend 项目嵌入
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
 * PHP 应用嵌入
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
 * Python 应用嵌入
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
 * Go 应用嵌入
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
