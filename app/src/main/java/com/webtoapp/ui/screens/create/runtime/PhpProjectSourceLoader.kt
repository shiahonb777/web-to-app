package com.webtoapp.ui.screens.create.runtime

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.webtoapp.ui.screens.create.common.ProjectImportException
import com.webtoapp.ui.screens.create.common.unwrapSingleDirectoryRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

class PhpProjectSourceLoader {
    suspend fun copyDocumentTreeToTempDir(context: Context, treeUri: Uri, tempDir: File): File =
        withContext(Dispatchers.IO) {
            val treeDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: throw ProjectImportException("目录不存在")
            if (!treeDoc.exists()) {
                throw ProjectImportException("目录不存在")
            }
            copyDocumentTreeToLocal(context, treeDoc, tempDir)
            if (tempDir.listFiles().isNullOrEmpty()) {
                throw ProjectImportException("目录不存在")
            }
            tempDir
        }

    suspend fun extractZipToTempDir(context: Context, zipUri: Uri, tempDir: File): File =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory &&
                            !name.startsWith("__MACOSX/") &&
                            !name.substringAfterLast("/").startsWith("._")
                        ) {
                            val outFile = File(tempDir, name)
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { output -> zipStream.copyTo(output) }
                        }
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            } ?: throw ProjectImportException("ZIP 解压失败")

            val projectDir = unwrapSingleDirectoryRoot(tempDir)
            if (!projectDir.walkTopDown().maxDepth(3).any { it.extension == "php" }) {
                throw ProjectImportException("ZIP 中未找到 PHP 文件")
            }
            projectDir
        }

    private fun copyDocumentTreeToLocal(context: Context, docDir: DocumentFile, destDir: File) {
        docDir.listFiles().forEach { doc ->
            if (doc.isDirectory) {
                val childDir = File(destDir, doc.name ?: "dir")
                childDir.mkdirs()
                copyDocumentTreeToLocal(context, doc, childDir)
            } else if (doc.isFile) {
                val outFile = File(destDir, doc.name ?: "file")
                context.contentResolver.openInputStream(doc.uri)?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
}
