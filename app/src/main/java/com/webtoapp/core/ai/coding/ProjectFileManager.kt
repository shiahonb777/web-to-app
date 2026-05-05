package com.webtoapp.core.ai.coding

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.util.threadLocalCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*







class ProjectFileManager(private val context: Context) {

    companion object {
        private const val TAG = "ProjectFileManager"
        private const val PROJECTS_ROOT = "AiCoding/projects"


        private val VERSION_EXTRACT_REGEX = Regex("""_v(\d+)\.""")
        private val VERSION_SUFFIX_REGEX = Regex("""_v\d+$""")
    }




    fun getProjectsRoot(): File {
        val dir = File(context.getExternalFilesDir(null), PROJECTS_ROOT)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }




    fun getSessionProjectDir(sessionId: String): File {
        val dir = File(getProjectsRoot(), sessionId)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }










    fun createFile(
        sessionId: String,
        filename: String,
        content: String,
        createNewVersion: Boolean = false
    ): ProjectFileInfo {
        val projectDir = getSessionProjectDir(sessionId)


        val baseName = filename.substringBeforeLast(".")
        val extension = filename.substringAfterLast(".", "")


        val existingFile = File(projectDir, filename)
        val actualFilename: String
        val version: Int

        if (existingFile.exists() && createNewVersion) {

            version = findNextVersion(projectDir, baseName, extension)
            actualFilename = "${baseName}_v${version}.${extension}"
        } else if (existingFile.exists()) {

            version = extractVersion(filename) ?: 1
            actualFilename = filename
        } else {

            version = 1
            actualFilename = filename
        }


        val file = File(projectDir, actualFilename)
        file.writeText(content)

        AppLogger.d(TAG, "Created file: ${file.absolutePath}, version: $version")

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




    fun readFile(sessionId: String, filename: String): String? {
        val file = File(getSessionProjectDir(sessionId), filename)
        return if (file.exists()) file.readText() else null
    }




    fun modifyFile(
        sessionId: String,
        filename: String,
        content: String
    ): ProjectFileInfo {
        return createFile(sessionId, filename, content, createNewVersion = true)
    }




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




    fun getFileVersions(sessionId: String, baseName: String): List<ProjectFileInfo> {
        return listFiles(sessionId).filter {
            getBaseName(it.name) == baseName
        }.sortedByDescending { it.version }
    }




    fun getLatestVersion(sessionId: String, baseName: String): ProjectFileInfo? {
        return getFileVersions(sessionId, baseName).firstOrNull()
    }




    fun deleteFile(sessionId: String, filename: String): Boolean {
        val file = File(getSessionProjectDir(sessionId), filename)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }




    fun deleteProject(sessionId: String): Boolean {
        val projectDir = getSessionProjectDir(sessionId)
        return if (projectDir.exists()) {
            projectDir.deleteRecursively()
        } else {
            false
        }
    }




    fun exportProject(sessionId: String, targetDir: File, includeAllVersions: Boolean = false): Boolean {
        val projectDir = getSessionProjectDir(sessionId)
        if (!projectDir.exists()) return false

        if (!targetDir.exists()) targetDir.mkdirs()

        val files = if (includeAllVersions) {
            projectDir.listFiles()?.toList() ?: emptyList()
        } else {

            getLatestVersionFiles(sessionId)
        }

        files.forEach { file ->
            if (file.isFile) {
                file.copyTo(File(targetDir, file.name), overwrite = true)
            }
        }

        return true
    }




    private fun getLatestVersionFiles(sessionId: String): List<File> {
        val projectDir = getSessionProjectDir(sessionId)
        val allFiles = projectDir.listFiles()?.filter { it.isFile } ?: return emptyList()


        return allFiles
            .groupBy { getBaseName(it.name) }
            .map { (_, files) ->
                files.maxBy { extractVersion(it.name) ?: 1 }
            }
    }




    private fun findNextVersion(projectDir: File, baseName: String, extension: String): Int {
        val existingVersions = projectDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(baseName) && it.extension == extension }
            ?.mapNotNull { extractVersion(it.name) }
            ?: emptyList()

        return (existingVersions.maxOrNull() ?: 0) + 1
    }





    private fun extractVersion(filename: String): Int? {
        return VERSION_EXTRACT_REGEX.find(filename)?.groupValues?.get(1)?.toIntOrNull()
    }




    private fun getBaseName(filename: String): String {
        val withoutExt = filename.substringBeforeLast(".")
        return withoutExt.replace(VERSION_SUFFIX_REGEX, "")
    }




    private fun getFileType(extension: String): ProjectFileType {
        return when (extension.lowercase()) {
            "html", "htm" -> ProjectFileType.HTML
            "css" -> ProjectFileType.CSS
            "js", "jsx", "ts", "tsx", "mjs", "cjs" -> ProjectFileType.JS
            "json" -> ProjectFileType.JSON
            "svg" -> ProjectFileType.SVG
            "png", "jpg", "jpeg", "gif", "webp", "ico" -> ProjectFileType.IMAGE
            else -> ProjectFileType.OTHER
        }
    }
}


private val PROJECT_FILE_VERSION_SUFFIX_REGEX = Regex("""_v\d+$""")




private val FILE_INFO_DATE_FORMAT = threadLocalCompat {
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
}

data class ProjectFileInfo(
    val name: String,
    val path: String,
    val relativePath: String,
    val size: Long,
    val version: Int,
    val type: ProjectFileType,
    val createdAt: Long,
    val modifiedAt: Long
) {



    fun getBaseName(): String {
        val withoutExt = name.substringBeforeLast(".")
        return withoutExt.replace(PROJECT_FILE_VERSION_SUFFIX_REGEX, "")
    }




    fun getExtension(): String = name.substringAfterLast(".", "")




    var isLatest: Boolean = false




    fun formatSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }




    fun formatTime(): String {
        return FILE_INFO_DATE_FORMAT.get()!!.format(Date(modifiedAt))
    }
}
