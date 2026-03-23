package com.webtoapp.core.ai.htmlcoding

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 项目文件管理器
 * 
 * 每个对话会话对应一个项目文件夹，AI 通过工具在文件夹内创建和修改文件。
 * 支持版本迭代：文件修改时创建新版本（v1, v2, v3...），历史版本保留可浏览。
 */
class ProjectFileManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ProjectFileManager"
        private const val PROJECTS_ROOT = "HtmlCoding/projects"
    }
    
    /**
     * Get project root directory
     */
    fun getProjectsRoot(): File {
        val dir = File(context.getExternalFilesDir(null), PROJECTS_ROOT)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    /**
     * 获取会话的项目文件夹
     */
    fun getSessionProjectDir(sessionId: String): File {
        val dir = File(getProjectsRoot(), sessionId)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    /**
     * 创建文件（支持版本控制）
     * 
     * @param sessionId 会话ID
     * @param filename 文件名（如 index.html）
     * @param content 文件内容
     * @param createNewVersion 是否创建新版本（迭代时为 true）
     * @return 创建的文件信息
     */
    fun createFile(
        sessionId: String,
        filename: String,
        content: String,
        createNewVersion: Boolean = false
    ): ProjectFileInfo {
        val projectDir = getSessionProjectDir(sessionId)
        
        // Parse文件名和扩展名
        val baseName = filename.substringBeforeLast(".")
        val extension = filename.substringAfterLast(".", "")
        
        // Check是否需要创建新版本
        val existingFile = File(projectDir, filename)
        val actualFilename: String
        val version: Int
        
        if (existingFile.exists() && createNewVersion) {
            // 找到下一个可用版本号
            version = findNextVersion(projectDir, baseName, extension)
            actualFilename = "${baseName}_v${version}.${extension}"
        } else if (existingFile.exists()) {
            // 覆盖现有文件，版本号不变
            version = extractVersion(filename) ?: 1
            actualFilename = filename
        } else {
            // 新文件
            version = 1
            actualFilename = filename
        }
        
        // 写入文件
        val file = File(projectDir, actualFilename)
        file.writeText(content)
        
        Log.d(TAG, "Created file: ${file.absolutePath}, version: $version")
        
        return ProjectFileInfo(
            name = actualFilename,
            path = file.absolutePath,
            relativePath = actualFilename,
            size = content.length.toLong(),
            version = version,
            type = getFileType(extension),
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 读取文件内容
     */
    fun readFile(sessionId: String, filename: String): String? {
        val file = File(getSessionProjectDir(sessionId), filename)
        return if (file.exists()) file.readText() else null
    }
    
    /**
     * 修改文件（创建新版本）
     */
    fun modifyFile(
        sessionId: String,
        filename: String,
        content: String
    ): ProjectFileInfo {
        return createFile(sessionId, filename, content, createNewVersion = true)
    }
    
    /**
     * 获取项目中的所有文件
     */
    fun listFiles(sessionId: String): List<ProjectFileInfo> {
        val projectDir = getSessionProjectDir(sessionId)
        if (!projectDir.exists()) return emptyList()
        
        return projectDir.listFiles()
            ?.filter { it.isFile }
            ?.map { file ->
                val extension = file.extension
                ProjectFileInfo(
                    name = file.name,
                    path = file.absolutePath,
                    relativePath = file.name,
                    size = file.length(),
                    version = extractVersion(file.name) ?: 1,
                    type = getFileType(extension),
                    createdAt = file.lastModified(),
                    modifiedAt = file.lastModified()
                )
            }
            ?.sortedWith(compareBy({ getBaseName(it.name) }, { -it.version }))
            ?: emptyList()
    }
    
    /**
     * 获取文件的所有版本
     */
    fun getFileVersions(sessionId: String, baseName: String): List<ProjectFileInfo> {
        return listFiles(sessionId).filter { 
            getBaseName(it.name) == baseName 
        }.sortedByDescending { it.version }
    }
    
    /**
     * 获取最新版本的文件
     */
    fun getLatestVersion(sessionId: String, baseName: String): ProjectFileInfo? {
        return getFileVersions(sessionId, baseName).firstOrNull()
    }
    
    /**
     * 删除文件
     */
    fun deleteFile(sessionId: String, filename: String): Boolean {
        val file = File(getSessionProjectDir(sessionId), filename)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
    
    /**
     * 删除整个项目
     */
    fun deleteProject(sessionId: String): Boolean {
        val projectDir = getSessionProjectDir(sessionId)
        return if (projectDir.exists()) {
            projectDir.deleteRecursively()
        } else {
            false
        }
    }
    
    /**
     * 复制项目到指定目录
     */
    fun exportProject(sessionId: String, targetDir: File, includeAllVersions: Boolean = false): Boolean {
        val projectDir = getSessionProjectDir(sessionId)
        if (!projectDir.exists()) return false
        
        if (!targetDir.exists()) targetDir.mkdirs()
        
        val files = if (includeAllVersions) {
            projectDir.listFiles()?.toList() ?: emptyList()
        } else {
            // 只导出每个文件的最新版本
            getLatestVersionFiles(sessionId)
        }
        
        files.forEach { file ->
            if (file.isFile) {
                file.copyTo(File(targetDir, file.name), overwrite = true)
            }
        }
        
        return true
    }
    
    /**
     * 获取每个文件的最新版本
     */
    private fun getLatestVersionFiles(sessionId: String): List<File> {
        val projectDir = getSessionProjectDir(sessionId)
        val allFiles = projectDir.listFiles()?.filter { it.isFile } ?: return emptyList()
        
        // 按基础文件名分组，取每组中版本最高的
        return allFiles
            .groupBy { getBaseName(it.name) }
            .map { (_, files) ->
                files.maxByOrNull { extractVersion(it.name) ?: 1 }!!
            }
    }
    
    /**
     * 查找下一个可用版本号
     */
    private fun findNextVersion(projectDir: File, baseName: String, extension: String): Int {
        val existingVersions = projectDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(baseName) && it.extension == extension }
            ?.mapNotNull { extractVersion(it.name) }
            ?: emptyList()
        
        return (existingVersions.maxOrNull() ?: 0) + 1
    }
    
    /**
     * 从文件名中提取版本号
     * 例如：index_v2.html -> 2, index.html -> null
     */
    private fun extractVersion(filename: String): Int? {
        val regex = Regex("""_v(\d+)\.""")
        return regex.find(filename)?.groupValues?.get(1)?.toIntOrNull()
    }
    
    /**
     * 获取文件的基础名称（不含版本号和扩展名）
     */
    private fun getBaseName(filename: String): String {
        val withoutExt = filename.substringBeforeLast(".")
        return withoutExt.replace(Regex("""_v\d+$"""), "")
    }
    
    /**
     * 根据扩展名获取文件类型
     */
    private fun getFileType(extension: String): ProjectFileType {
        return when (extension.lowercase()) {
            "html", "htm" -> ProjectFileType.HTML
            "css" -> ProjectFileType.CSS
            "js" -> ProjectFileType.JS
            "json" -> ProjectFileType.JSON
            "svg" -> ProjectFileType.SVG
            "png", "jpg", "jpeg", "gif", "webp" -> ProjectFileType.IMAGE
            else -> ProjectFileType.OTHER
        }
    }
}

/**
 * 项目文件信息
 */
data class ProjectFileInfo(
    val name: String,           // File名（如 index_v2.html）
    val path: String,           // 绝对路径
    val relativePath: String,   // 相对于项目目录的路径
    val size: Long,             // File大小（字节）
    val version: Int,           // Version号
    val type: ProjectFileType,  // File类型
    val createdAt: Long,        // Create时间
    val modifiedAt: Long        // 修改时间
) {
    /**
     * 获取基础文件名（不含版本号）
     */
    fun getBaseName(): String {
        val withoutExt = name.substringBeforeLast(".")
        return withoutExt.replace(Regex("""_v\d+$"""), "")
    }
    
    /**
     * 获取扩展名
     */
    fun getExtension(): String = name.substringAfterLast(".", "")
    
    /**
     * 是否是最新版本（需要外部判断）
     */
    var isLatest: Boolean = false
    
    /**
     * 格式化文件大小
     */
    fun formatSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }
    
    /**
     * 格式化时间
     */
    fun formatTime(): String {
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(modifiedAt))
    }
}
