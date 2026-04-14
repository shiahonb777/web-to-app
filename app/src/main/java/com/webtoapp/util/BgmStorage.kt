package com.webtoapp.util

import android.content.Context
import android.net.Uri
import com.webtoapp.data.model.BgmItem
import com.webtoapp.data.model.LrcData
import com.webtoapp.data.model.LrcLine
import com.webtoapp.core.logging.AppLogger
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Note.
 * Note.
 */
object BgmStorage {
    
    private const val TAG = "BgmStorage"
    
    // User
    private const val BGM_DIR = "bgm"
    
    // （assets）
    private const val ASSETS_BGM_DIR = "bgm"
    
    // Pre-compiled regex for LRC parsing (avoid creating per file)
    private val LRC_TIME_REGEX = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")
    private val LRC_META_REGEX = Regex("""\[(ti|ar|al):(.*)]""", RegexOption.IGNORE_CASE)
    
    // Pre-compiled regex for safe file names
    private val SAFE_NAME_REGEX = Regex("[^a-zA-Z0-9\u4e00-\u9fa5_-]")
    
    // Image cover extensions set (avoid listOf per scan call)
    private val IMAGE_COVER_EXTENSIONS = setOf("png", "jpg", "jpeg")
    
    /**
     * Note.
     */
    fun getBgmDir(context: Context): File {
        val dir = File(context.filesDir, BGM_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * （ + ）
     */
    fun scanAllBgm(context: Context): List<BgmItem> {
        val result = mutableListOf<BgmItem>()
        
        // Note.
        result.addAll(scanAssetsBgm(context))
        
        // Note.
        result.addAll(scanUserBgm(context))
        
        return result
    }
    
    /**
     * （assets/bgm/）
     * mp3 png
     */
    fun scanAssetsBgm(context: Context): List<BgmItem> {
        val result = mutableListOf<BgmItem>()
        
        try {
            val assetManager = context.assets
            val files = assetManager.list(ASSETS_BGM_DIR) ?: return emptyList()
            
            // mp3
            val mp3Files = files.filter { it.lowercase().endsWith(".mp3") }
            
            for (mp3File in mp3Files) {
                val nameWithoutExt = mp3File.substringBeforeLast(".")
                
                // Find matching cover image (png/jpg/jpeg)
                val coverFile = files.find { file ->
                    val fileNameWithoutExt = file.substringBeforeLast(".")
                    val ext = file.substringAfterLast(".").lowercase()
                    fileNameWithoutExt == nameWithoutExt && ext in IMAGE_COVER_EXTENSIONS
                }
                
                // Find matching LRC, prefer user file over assets
                val bgmPath = "asset:///$ASSETS_BGM_DIR/$mp3File"
                val userLrcFile = File(getBgmDir(context), "$nameWithoutExt.lrc")
                val lrcData = if (userLrcFile.exists()) {
                    // LRC
                    loadLrcFromFile(userLrcFile)
                } else {
                    // No assets
                    val assetLrcFile = files.find { file ->
                        val fileNameWithoutExt = file.substringBeforeLast(".")
                        val ext = file.substringAfterLast(".").lowercase()
                        fileNameWithoutExt == nameWithoutExt && ext == "lrc"
                    }
                    assetLrcFile?.let { loadLrcFromAssets(context, "$ASSETS_BGM_DIR/$it") }
                }
                
                result.add(BgmItem(
                    name = nameWithoutExt,
                    path = bgmPath,
                    coverPath = coverFile?.let { "asset:///$ASSETS_BGM_DIR/$it" },
                    isAsset = true,
                    lrcData = lrcData
                ))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
        }
        
        return result
    }
    
    /**
     * Note.
     * mp3
     */
    fun scanUserBgm(context: Context): List<BgmItem> {
        val result = mutableListOf<BgmItem>()
        val bgmDir = getBgmDir(context)
        
        if (!bgmDir.exists()) return emptyList()
        
        val files = bgmDir.listFiles() ?: return emptyList()
        
        // mp3
        val mp3Files = files.filter { it.extension.lowercase() == "mp3" }
        
        for (mp3File in mp3Files) {
            val nameWithoutExt = mp3File.nameWithoutExtension
            
            // Find
            val coverFile = files.find { file ->
                file.nameWithoutExtension == nameWithoutExt &&
                file.extension.lowercase() in IMAGE_COVER_EXTENSIONS
            }
            
            // Find LRC
            val lrcFile = files.find { file ->
                file.nameWithoutExtension == nameWithoutExt &&
                file.extension.lowercase() == "lrc"
            }
            
            // Load LRC
            val lrcData = lrcFile?.let { loadLrcFromFile(it) }
            
            result.add(BgmItem(
                name = nameWithoutExt,
                path = mp3File.absolutePath,
                coverPath = coverFile?.absolutePath,
                isAsset = false,
                lrcData = lrcData
            ))
        }
        
        return result
    }
    
    /**
     * MP3
     * @param uri parameter
     * @param customName parameter
     * @return result
     */
    fun saveBgm(context: Context, uri: Uri, customName: String? = null): String? {
        return try {
            val bgmDir = getBgmDir(context)
            
            // Cleanup，，
            val safeName = if (customName.isNullOrBlank()) {
                "bgm_${UUID.randomUUID()}"
            } else {
                customName.replace(SAFE_NAME_REGEX, "_")
            }
            val fileName = "${safeName}.mp3"
            val destFile = File(bgmDir, fileName)
            
            // Copy
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                AppLogger.e(TAG, "无法打开音频文件: $uri")
                return null
            }
            
            inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    val bytes = input.copyTo(output)
                    AppLogger.d(TAG, "音频文件已保存: ${destFile.absolutePath}, 大小: $bytes bytes")
                }
            }
            
            // Verify
            if (!destFile.exists() || destFile.length() == 0L) {
                AppLogger.e(TAG, "音频文件保存失败或为空: ${destFile.absolutePath}")
                return null
            }
            
            destFile.absolutePath
        } catch (e: Exception) {
            AppLogger.e(TAG, "保存音频文件异常", e)
            null
        }
    }
    
    /**
     * Note.
     * @param uri parameter
     * @param bgmName parameter
     * @return result
     */
    fun saveCover(context: Context, uri: Uri, bgmName: String): String? {
        return try {
            val bgmDir = getBgmDir(context)
            
            // Get
            val mimeType = context.contentResolver.getType(uri)
            val extension = when {
                mimeType?.contains("png") == true -> "png"
                mimeType?.contains("jpeg") == true || mimeType?.contains("jpg") == true -> "jpg"
                else -> "png"
            }
            
            val destFile = File(bgmDir, "$bgmName.$extension")
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            destFile.absolutePath
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            null
        }
    }
    
    /**
     * （）
     */
    fun deleteBgm(context: Context, bgmItem: BgmItem): Boolean {
        if (bgmItem.isAsset) return false // Note.
        
        return try {
            // Delete
            File(bgmItem.path).delete()
            
            // Delete（）
            bgmItem.coverPath?.let { File(it).delete() }
            
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            false
        }
    }
    
    /**
     * （ asset:// ）
     */
    fun getBgmInputStream(context: Context, path: String): java.io.InputStream? {
        return try {
            if (path.startsWith("asset:///")) {
                val assetPath = path.removePrefix("asset:///")
                context.assets.open(assetPath)
            } else {
                File(path).inputStream()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            null
        }
    }
    
    /**
     * （ APK ）
     */
    fun copyBgmToDir(context: Context, bgmItem: BgmItem, destDir: File): File? {
        return try {
            val destFile = File(destDir, "${bgmItem.name}.mp3")
            
            getBgmInputStream(context, bgmItem.path)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            destFile
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            null
        }
    }
    
    /**
     * LRC
     * @param bgmPath parameter
     * @param lrcData parameter
     * @return result
     */
    fun saveLrc(context: Context, bgmPath: String, lrcData: LrcData): Boolean {
        return try {
            // LRC
            val lrcPath = getLrcPathForBgm(context, bgmPath)
            val lrcFile = File(lrcPath)
            
            // Note.
            lrcFile.parentFile?.mkdirs()
            
            // LrcData LRC
            val lrcContent = buildString {
                appendLine("[ti:${lrcData.title ?: ""}]")
                appendLine("[ar:${lrcData.artist ?: ""}]")
                appendLine("[al:${lrcData.album ?: ""}]")
                appendLine()
                
                lrcData.lines.forEach { line ->
                    val minutes = (line.startTime / 60000).toInt()
                    val seconds = ((line.startTime % 60000) / 1000).toInt()
                    val millis = ((line.startTime % 1000) / 10).toInt()
                    appendLine("[%02d:%02d.%02d]%s".format(minutes, seconds, millis, line.text))
                }
            }
            
            lrcFile.writeText(lrcContent, Charsets.UTF_8)
            AppLogger.d(TAG, "LRC 保存成功: $lrcPath")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "保存 LRC 失败", e)
            false
        }
    }
    
    /**
     * LRC
     */
    fun loadLrcFromFile(lrcFile: File): LrcData? {
        return try {
            if (!lrcFile.exists()) return null
            parseLrcText(lrcFile.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            AppLogger.e(TAG, "加载 LRC 失败: ${lrcFile.path}", e)
            null
        }
    }
    
    /**
     * assets LRC
     */
    fun loadLrcFromAssets(context: Context, assetPath: String): LrcData? {
        return try {
            val text = context.assets.open(assetPath).bufferedReader().readText()
            parseLrcText(text)
        } catch (e: Exception) {
            AppLogger.e(TAG, "从 assets 加载 LRC 失败: $assetPath", e)
            null
        }
    }
    
    /**
     * LRC
     */
    private fun parseLrcText(text: String): LrcData? {
        val lines = mutableListOf<LrcLine>()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        
        val timeRegex = LRC_TIME_REGEX
        val metaRegex = LRC_META_REGEX
        
        text.lines().forEach { line ->
            // Parse
            metaRegex.find(line)?.let { match ->
                when (match.groupValues[1].lowercase()) {
                    "ti" -> title = match.groupValues[2].trim()
                    "ar" -> artist = match.groupValues[2].trim()
                    "al" -> album = match.groupValues[2].trim()
                }
                return@forEach
            }
            
            // Parse
            timeRegex.find(line)?.let { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: 0
                val seconds = match.groupValues[2].toLongOrNull() ?: 0
                val millis = match.groupValues[3].let {
                    if (it.length == 2) it.toLong() * 10 else it.toLong()
                }
                val lyricText = match.groupValues[4].trim()
                
                if (lyricText.isNotEmpty()) {
                    val startTime = minutes * 60000 + seconds * 1000 + millis
                    lines.add(LrcLine(startTime = startTime, endTime = startTime + 5000, text = lyricText))
                }
            }
        }
        
        // （）
        for (i in 0 until lines.size - 1) {
            lines[i] = lines[i].copy(endTime = lines[i + 1].startTime)
        }
        
        return if (lines.isNotEmpty()) {
            LrcData(lines = lines, title = title, artist = artist, album = album)
        } else null
    }
    
    /**
     * LRC
     */
    fun getLrcPathForBgm(context: Context, bgmPath: String): String {
        return if (bgmPath.startsWith("asset:///")) {
            // Assets ，LRC
            val assetPath = bgmPath.removePrefix("asset:///")
            val name = File(assetPath).nameWithoutExtension
            File(getBgmDir(context), "$name.lrc").absolutePath
        } else {
            // User，LRC
            val musicFile = File(bgmPath)
            File(musicFile.parent, "${musicFile.nameWithoutExtension}.lrc").absolutePath
        }
    }
    
    /**
     * LRC
     */
    fun hasLrc(context: Context, bgmPath: String): Boolean {
        val lrcPath = getLrcPathForBgm(context, bgmPath)
        return File(lrcPath).exists()
    }
    
    /**
     * LRC
     */
    fun loadLrc(context: Context, bgmPath: String): LrcData? {
        val lrcPath = getLrcPathForBgm(context, bgmPath)
        val lrcFile = File(lrcPath)
        return if (lrcFile.exists()) loadLrcFromFile(lrcFile) else null
    }
}
