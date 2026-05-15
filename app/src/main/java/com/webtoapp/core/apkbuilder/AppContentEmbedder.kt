package com.webtoapp.core.apkbuilder

import com.webtoapp.core.crypto.AssetEncryptor
import com.webtoapp.core.crypto.EncryptionConfig
import java.io.File
import java.util.zip.ZipOutputStream












interface AppContentEmbedder {








    fun embed(zipOut: ZipOutputStream, ctx: EmbedContext): EmbedResult
}






class EmbedContext(
    val config: ApkConfig,
    val logger: BuildLogger,
    val encryptor: AssetEncryptor?,
    val encryptionConfig: EncryptionConfig,

    val mediaContentPath: String?,
    val htmlFiles: List<com.webtoapp.data.model.HtmlFile>,
    val galleryItems: List<com.webtoapp.data.model.GalleryItem>,
    val projectDir: File?,
    val secondaryProjectDir: File?,

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




data class EmbedResult(
    val success: Boolean,
    val itemCount: Int = 0,
    val message: String = ""
)




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
            "MULTI_WEB" -> MultiWebContentEmbedder()
            "WEB" -> null
            else -> null
        }
    }
}






class MediaContentEmbedder : AppContentEmbedder {
    override fun embed(zipOut: ZipOutputStream, ctx: EmbedContext): EmbedResult {
        val mediaPath = ctx.mediaContentPath ?: return EmbedResult(false, message = "No media content path")
        ctx.logger.log("Embedding single media content: $mediaPath")
        val isVideo = ctx.config.appType == "VIDEO"
        ctx.fnAddMediaContent(zipOut, mediaPath, isVideo, ctx.encryptor, ctx.encryptionConfig)
        return EmbedResult(true, 1, "Media content embedded")
    }
}




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
        } else {

            val entryFile = ctx.config.htmlEntryFile
            val embeddedNames = ctx.htmlFiles.map { it.name }
            val entryFound = embeddedNames.any { it.equals(entryFile, ignoreCase = true) }
            if (!entryFound) {
                ctx.logger.warn("⚠️ Entry file '$entryFile' was NOT found in embedded file list!")
                ctx.logger.warn("   Embedded files: ${embeddedNames.joinToString(", ")}")
                ctx.logger.warn("   The app may show ERR_FILE_NOT_FOUND at runtime.")
                ctx.logger.warn("   ShellContentRouter will attempt auto-discovery as fallback.")
            } else {
                ctx.logger.log("✓ Entry file '$entryFile' confirmed in embedded files")
            }
        }
        return EmbedResult(count > 0, count, "$count HTML files embedded")
    }
}




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




class FrontendContentEmbedder : AppContentEmbedder {
    override fun embed(zipOut: ZipOutputStream, ctx: EmbedContext): EmbedResult {
        val dir = ctx.projectDir
        if (dir != null && dir.exists()) {
            ctx.logger.section("Embed Frontend Project Files")
            ctx.fnAddFrontendFiles(zipOut, dir, ctx.htmlFiles)
            return EmbedResult(true, message = "Frontend files embedded")
        }

        if (ctx.htmlFiles.isNotEmpty()) {
            ctx.logger.section("Embed Frontend Files (from file list)")
            val count = ctx.fnAddHtmlFiles(zipOut, ctx.htmlFiles, ctx.encryptor, ctx.encryptionConfig)
            ctx.logger.logKeyValue("frontendFilesEmbeddedCount", count)
            return EmbedResult(count > 0, count, "$count frontend files embedded (fallback)")
        }
        return EmbedResult(false, message = "No frontend project directory or files")
    }
}




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

class MultiWebContentEmbedder : AppContentEmbedder {
    override fun embed(zipOut: ZipOutputStream, ctx: EmbedContext): EmbedResult {
        val dir = ctx.secondaryProjectDir
        if (dir == null || !dir.exists()) {
            return EmbedResult(true, message = "No multi-web local project files to embed")
        }
        ctx.logger.section("Embed Multi-Web Local Site Files")
        RuntimeAssetEmbedder.embedProjectFiles(
            zipOut = zipOut,
            projectDir = dir,
            config = RuntimeAssetEmbedder.multiWebConfig(),
            logger = ctx.logger
        )
        return EmbedResult(true, message = "Multi-web local site files embedded")
    }
}
