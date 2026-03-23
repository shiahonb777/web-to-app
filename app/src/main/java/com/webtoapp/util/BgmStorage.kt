package com.webtoapp.util

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.webtoapp.data.model.BgmItem
import com.webtoapp.data.model.LrcData
import com.webtoapp.data.model.LrcLine
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 背景音乐存储工具
 * 管理预置音乐和用户上传的音乐
 */
object BgmStorage {
    
    // User上传的音乐存储目录
    private const val BGM_DIR = "bgm"
    
    // 预置音乐资源目录（assets）
    private const val ASSETS_BGM_DIR = "bgm"
    
    /**
     * 获取用户音乐存储目录
     */
    fun getBgmDir(context: Context): File {
        val dir = File(context.filesDir, BGM_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * 扫描所有可用的背景音乐（预置 + 用户上传）
     */
    fun scanAllBgm(context: Context): List<BgmItem> {
        val result = mutableListOf<BgmItem>()
        
        // 扫描预置音乐
        result.addAll(scanAssetsBgm(context))
        
        // 扫描用户上传的音乐
        result.addAll(scanUserBgm(context))
        
        return result
    }
    
    /**
     * 扫描预置音乐（assets/bgm/）
     * 自动配对同名的 mp3 和 png 文件
     */
    fun scanAssetsBgm(context: Context): List<BgmItem> {
        val result = mutableListOf<BgmItem>()
        
        try {
            val assetManager = context.assets
            val files = assetManager.list(ASSETS_BGM_DIR) ?: return emptyList()
            
            // 找出所有 mp3 文件
            val mp3Files = files.filter { it.lowercase().endsWith(".mp3") }
            
            for (mp3File in mp3Files) {
                val nameWithoutExt = mp3File.substringBeforeLast(".")
                
                // Find同名封面图片（支持 png、jpg、jpeg）
                val coverFile = files.find { file ->
                    val fileNameWithoutExt = file.substringBeforeLast(".")
                    val ext = file.substringAfterLast(".").lowercase()
                    fileNameWithoutExt == nameWithoutExt && ext in listOf("png", "jpg", "jpeg")
                }
                
                // Find同名 LRC 文件（先检查用户目录，再检查 assets）
                val bgmPath = "asset:///$ASSETS_BGM_DIR/$mp3File"
                val userLrcFile = File(getBgmDir(context), "$nameWithoutExt.lrc")
                val lrcData = if (userLrcFile.exists()) {
                    // 优先加载用户目录的 LRC
                    loadLrcFromFile(userLrcFile)
                } else {
                    // No则尝试从 assets 加载
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
            e.printStackTrace()
        }
        
        return result
    }
    
    /**
     * 扫描用户上传的音乐
     * 自动配对同名的 mp3 和图片文件
     */
    fun scanUserBgm(context: Context): List<BgmItem> {
        val result = mutableListOf<BgmItem>()
        val bgmDir = getBgmDir(context)
        
        if (!bgmDir.exists()) return emptyList()
        
        val files = bgmDir.listFiles() ?: return emptyList()
        
        // 找出所有 mp3 文件
        val mp3Files = files.filter { it.extension.lowercase() == "mp3" }
        
        for (mp3File in mp3Files) {
            val nameWithoutExt = mp3File.nameWithoutExtension
            
            // Find同名封面图片
            val coverFile = files.find { file ->
                file.nameWithoutExtension == nameWithoutExt &&
                file.extension.lowercase() in listOf("png", "jpg", "jpeg")
            }
            
            // Find同名 LRC 文件
            val lrcFile = files.find { file ->
                file.nameWithoutExtension == nameWithoutExt &&
                file.extension.lowercase() == "lrc"
            }
            
            // Load LRC 数据
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
     * 保存用户上传的 MP3 文件
     * @param uri 音乐文件 URI
     * @param customName 自定义名称（可选，为空则使用 UUID）
     * @return 保存后的文件路径，失败返回 null
     */
    fun saveBgm(context: Context, uri: Uri, customName: String? = null): String? {
        return try {
            val bgmDir = getBgmDir(context)
            
            // Cleanup文件名，移除特殊字符，避免路径问题
            val safeName = if (customName.isNullOrBlank()) {
                "bgm_${UUID.randomUUID()}"
            } else {
                customName.replace(Regex("[^a-zA-Z0-9\u4e00-\u9fa5_-]"), "_")
            }
            val fileName = "${safeName}.mp3"
            val destFile = File(bgmDir, fileName)
            
            // Copy文件内容
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                android.util.Log.e("BgmStorage", "无法打开音频文件: $uri")
                return null
            }
            
            inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    val bytes = input.copyTo(output)
                    android.util.Log.d("BgmStorage", "音频文件已保存: ${destFile.absolutePath}, 大小: $bytes bytes")
                }
            }
            
            // Verify文件是否成功保存
            if (!destFile.exists() || destFile.length() == 0L) {
                android.util.Log.e("BgmStorage", "音频文件保存失败或为空: ${destFile.absolutePath}")
                return null
            }
            
            destFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("BgmStorage", "保存音频文件异常", e)
            null
        }
    }
    
    /**
     * 保存用户上传的封面图片
     * @param uri 图片文件 URI
     * @param bgmName 对应的音乐名称（用于同名配对）
     * @return 保存后的文件路径，失败返回 null
     */
    fun saveCover(context: Context, uri: Uri, bgmName: String): String? {
        return try {
            val bgmDir = getBgmDir(context)
            
            // Get文件扩展名
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
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 删除用户上传的音乐（同时删除封面）
     */
    fun deleteBgm(context: Context, bgmItem: BgmItem): Boolean {
        if (bgmItem.isAsset) return false // 不能删除预置资源
        
        return try {
            // Delete音乐文件
            File(bgmItem.path).delete()
            
            // Delete封面文件（如果存在）
            bgmItem.coverPath?.let { File(it).delete() }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 获取音乐文件（处理 asset:// 协议）
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
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 复制音乐到指定目录（用于 APK 构建）
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
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 保存 LRC 数据到文件
     * @param bgmPath 音乐文件路径
     * @param lrcData LRC 数据
     * @return 保存是否成功
     */
    fun saveLrc(context: Context, bgmPath: String, lrcData: LrcData): Boolean {
        return try {
            // 确定 LRC 文件路径
            val lrcPath = getLrcPathForBgm(context, bgmPath)
            val lrcFile = File(lrcPath)
            
            // 确保目录存在
            lrcFile.parentFile?.mkdirs()
            
            // 将 LrcData 转换为 LRC 格式文本
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
            android.util.Log.d("BgmStorage", "LRC 保存成功: $lrcPath")
            true
        } catch (e: Exception) {
            android.util.Log.e("BgmStorage", "保存 LRC 失败", e)
            false
        }
    }
    
    /**
     * 从文件加载 LRC 数据
     */
    fun loadLrcFromFile(lrcFile: File): LrcData? {
        return try {
            if (!lrcFile.exists()) return null
            parseLrcText(lrcFile.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            android.util.Log.e("BgmStorage", "加载 LRC 失败: ${lrcFile.path}", e)
            null
        }
    }
    
    /**
     * 从 assets 加载 LRC 数据
     */
    fun loadLrcFromAssets(context: Context, assetPath: String): LrcData? {
        return try {
            val text = context.assets.open(assetPath).bufferedReader().readText()
            parseLrcText(text)
        } catch (e: Exception) {
            android.util.Log.e("BgmStorage", "从 assets 加载 LRC 失败: $assetPath", e)
            null
        }
    }
    
    /**
     * 解析 LRC 文本
     */
    private fun parseLrcText(text: String): LrcData? {
        val lines = mutableListOf<LrcLine>()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        
        val timeRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")
        val metaRegex = Regex("""\[(ti|ar|al):(.*)]""", RegexOption.IGNORE_CASE)
        
        text.lines().forEach { line ->
            // Parse元数据
            metaRegex.find(line)?.let { match ->
                when (match.groupValues[1].lowercase()) {
                    "ti" -> title = match.groupValues[2].trim()
                    "ar" -> artist = match.groupValues[2].trim()
                    "al" -> album = match.groupValues[2].trim()
                }
                return@forEach
            }
            
            // Parse时间戳行
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
        
        // 计算每行的结束时间（下一行的开始时间）
        for (i in 0 until lines.size - 1) {
            lines[i] = lines[i].copy(endTime = lines[i + 1].startTime)
        }
        
        return if (lines.isNotEmpty()) {
            LrcData(lines = lines, title = title, artist = artist, album = album)
        } else null
    }
    
    /**
     * 获取音乐对应的 LRC 文件路径
     */
    fun getLrcPathForBgm(context: Context, bgmPath: String): String {
        return if (bgmPath.startsWith("asset:///")) {
            // Assets 中的音乐，LRC 保存到用户目录
            val assetPath = bgmPath.removePrefix("asset:///")
            val name = File(assetPath).nameWithoutExtension
            File(getBgmDir(context), "$name.lrc").absolutePath
        } else {
            // User目录的音乐，LRC 保存在同目录
            val musicFile = File(bgmPath)
            File(musicFile.parent, "${musicFile.nameWithoutExtension}.lrc").absolutePath
        }
    }
    
    /**
     * 检查音乐是否有 LRC 文件
     */
    fun hasLrc(context: Context, bgmPath: String): Boolean {
        val lrcPath = getLrcPathForBgm(context, bgmPath)
        return File(lrcPath).exists()
    }
    
    /**
     * 加载音乐的 LRC 数据
     */
    fun loadLrc(context: Context, bgmPath: String): LrcData? {
        val lrcPath = getLrcPathForBgm(context, bgmPath)
        val lrcFile = File(lrcPath)
        return if (lrcFile.exists()) loadLrcFromFile(lrcFile) else null
    }
}
