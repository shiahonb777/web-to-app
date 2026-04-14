package com.webtoapp.ui.screens

import android.net.Uri
import com.webtoapp.ui.components.PremiumOutlinedButton
import android.provider.DocumentsContract
import com.webtoapp.core.logging.AppLogger
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.linux.HtmlProjectOptimizer
import com.webtoapp.core.linux.NativeNodeEngine
import com.webtoapp.data.model.HtmlConfig
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.HtmlFileType
import com.webtoapp.ui.components.*
import com.webtoapp.util.HtmlProjectProcessor
import com.webtoapp.util.ZipProjectImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.webtoapp.ui.components.ThemedBackgroundBox
import androidx.compose.ui.graphics.Color
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * file importarea
 * ZIP import UI mode
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FolderImportSection(
    folderAnalysis: ZipProjectImporter.ZipProjectAnalysis?,
    folderImporting: Boolean,
    folderError: String?,
    folderEntryFile: String,
    onSelectFolder: () -> Unit,
    onChangeEntry: () -> Unit,
    onShowFileList: () -> Unit,
    onReimport: () -> Unit
) {
    if (folderImporting) {
        // import
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = AppStringsProvider.current().folderImporting,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else if (folderAnalysis != null) {
        // ( ZIP UI)
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // + selectbutton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStringsProvider.current().zipProjectAnalysis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    TextButton(onClick = onReimport) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStringsProvider.current().zipReimport, style = MaterialTheme.typography.labelMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // file
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .clickable { onChangeEntry() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(
                            text = AppStringsProvider.current().zipEntryFile,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = folderEntryFile,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (folderAnalysis.htmlFiles.size > 1) {
                        Icon(
                            Icons.Outlined.SwapHoriz,
                            contentDescription = AppStringsProvider.current().zipChangeEntry,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Note
                Text(
                    text = AppStringsProvider.current().zipResourceStats,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // typelabel
                val stats = folderAnalysis.stats
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stats.forEach { stat ->
                        AssistChip(
                            onClick = { onShowFileList() },
                            label = {
                                Text(
                                    "${stat.type.icon} ${stat.type.label}: ${stat.count}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // file
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = AppStringsProvider.current().zipTotalFiles.replace("%d", folderAnalysis.totalFileCount.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AppStringsProvider.current().zipTotalSize.replace("%s", folderAnalysis.formattedTotalSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // filelistbutton
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onShowFileList,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.List,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStringsProvider.current().zipFileTreeTitle, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        
        // warning
        if (folderAnalysis.warnings.isNotEmpty()) {
            EnhancedElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    folderAnalysis.warnings.forEach { warning ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    } else {
        // state: selectfile
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = AppStringsProvider.current().folderSelectFolder,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = AppStringsProvider.current().folderSelectHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                PremiumOutlinedButton(
                    onClick = onSelectFolder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStringsProvider.current().folderSelectFolder)
                }
                
                // error
                if (folderError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    EnhancedElevatedCard(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = folderError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== file import ====================

/** when file/directory */
internal val FOLDER_SKIP_PATTERNS = setOf(
    "__MACOSX", ".DS_Store", "Thumbs.db", ".git", ".svn", ".hg",
    "node_modules", ".idea", ".vscode"
)

/**
 * from SAF file import HTML item
 * 
 * DocumentsContract API SAF,
 * map file local directory, item.
 */
internal fun importFolderFromSaf(
    context: android.content.Context,
    treeUri: Uri
): ZipProjectImporter.ZipProjectAnalysis {
    val tempDir = File(context.cacheDir, "folder_import_${System.currentTimeMillis()}").apply {
        if (exists()) deleteRecursively()
        mkdirs()
    }
    
    try {
        // SAF localdirectory
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        
        // file app
        var folderName = "HTML Project"
        context.contentResolver.query(
            docUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                folderName = cursor.getString(0) ?: folderName
            }
        }
        
        copyDocumentTree(context, treeUri, docId, tempDir)
        
        // handle directory( ZIP import)
        val projectRoot = unwrapSingleRootDir(tempDir)
        
        // item
        return analyzeFolder(projectRoot, folderName)
        
    } catch (e: Exception) {
        tempDir.deleteRecursively()
        AppLogger.e("FolderImport", "文件夹导入失败", e)
        throw RuntimeException(AppStringsProvider.current().folderImportFailed.replace("%s", e.message ?: "Unknown"), e)
    }
}

/**
 * SAF localdirectory
 */
internal fun copyDocumentTree(
    context: android.content.Context,
    treeUri: Uri,
    parentDocId: String,
    targetDir: File
) {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
    
    context.contentResolver.query(
        childrenUri,
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        ),
        null, null, null
    )?.use { cursor ->
        val docIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
        
        while (cursor.moveToNext()) {
            val childDocId = cursor.getString(docIdIndex)
            val childName = cursor.getString(nameIndex) ?: continue
            val mimeType = cursor.getString(mimeIndex) ?: ""
            
            // file/directory
            if (FOLDER_SKIP_PATTERNS.any { childName.equals(it, ignoreCase = true) }) {
                continue
            }
            
            if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                // directory: handle
                val subDir = File(targetDir, childName).apply { mkdirs() }
                copyDocumentTree(context, treeUri, childDocId, subDir)
            } else {
                // file: local
                val targetFile = File(targetDir, childName)
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                try {
                    context.contentResolver.openInputStream(fileUri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.w("FolderImport", "复制文件失败: $childName", e)
                }
            }
        }
    }
}

/**
 * ifdirectory directory, expand( ZIP import)
 */
internal fun unwrapSingleRootDir(dir: File): File {
    val children = dir.listFiles() ?: return dir
    return if (children.size == 1 && children[0].isDirectory) {
        children[0]
    } else {
        dir
    }
}

/** file */
internal val HTML_EXT = setOf("html", "htm", "xhtml")
internal val CSS_EXT = setOf("css")
internal val JS_EXT = setOf("js", "mjs", "jsx", "ts", "tsx")
internal val IMG_EXT = setOf("png", "jpg", "jpeg", "gif", "webp", "svg", "ico", "bmp", "avif")
internal val FONT_EXT = setOf("ttf", "otf", "woff", "woff2", "eot")
internal val AUDIO_EXT = setOf("mp3", "wav", "ogg", "aac", "flac", "m4a")
internal val VIDEO_EXT = setOf("mp4", "webm", "mkv", "avi", "mov")
internal val DATA_EXT = setOf("json", "xml", "csv", "txt", "md", "yaml", "yml")

/**
 * file item, ZipProjectAnalysis
 */
internal fun analyzeFolder(
    projectDir: File,
    folderName: String
): ZipProjectImporter.ZipProjectAnalysis {
    val allFiles = mutableListOf<ZipProjectImporter.ProjectFile>()
    val warnings = mutableListOf<String>()
    
    projectDir.walkTopDown()
        .filter { it.isFile }
        .filter { file -> !FOLDER_SKIP_PATTERNS.any { file.name.equals(it, ignoreCase = true) } }
        .forEach { file ->
            val relativePath = file.relativeTo(projectDir).path
            val resourceType = classifyFileByExt(file.name)
            allFiles.add(
                ZipProjectImporter.ProjectFile(
                    relativePath = relativePath,
                    absolutePath = file.absolutePath,
                    size = file.length(),
                    resourceType = resourceType
                )
            )
        }
    
    val htmlFiles = allFiles.filter { it.resourceType == ZipProjectImporter.ResourceType.HTML }
    
    // file
    val entryFile = htmlFiles.find { it.relativePath.equals("index.html", ignoreCase = true) }?.relativePath
        ?: htmlFiles.find { it.relativePath.equals("index.htm", ignoreCase = true) }?.relativePath
        ?: htmlFiles.find { it.fileName.equals("index.html", ignoreCase = true) }?.relativePath
        ?: htmlFiles.find { !it.relativePath.contains('/') }?.relativePath
        ?: htmlFiles.firstOrNull()?.relativePath
    
    if (entryFile == null && htmlFiles.isEmpty()) {
        warnings.add(AppStringsProvider.current().folderNoHtmlWarning)
    }
    
    if (allFiles.any { it.size > 50 * 1024 * 1024 }) {
        warnings.add("存在超过 50MB 的大文件，打包 APK 时可能影响安装包大小")
    }
    
    val stats = ZipProjectImporter.ResourceType.entries
        .map { type ->
            val files = allFiles.filter { it.resourceType == type }
            ZipProjectImporter.ResourceStats(
                type = type,
                count = files.size,
                totalSize = files.sumOf { it.size }
            )
        }
        .filter { it.count > 0 }
    
    return ZipProjectImporter.ZipProjectAnalysis(
        extractDir = projectDir.absolutePath,
        allFiles = allFiles,
        entryFile = entryFile ?: htmlFiles.firstOrNull()?.relativePath ?: "index.html",
        htmlFiles = htmlFiles,
        stats = stats,
        totalFileCount = allFiles.size,
        totalSize = allFiles.sumOf { it.size },
        warnings = warnings,
        zipFileName = folderName  // zipFileName file
    )
}

/** file type */
internal fun classifyFileByExt(fileName: String): ZipProjectImporter.ResourceType {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        in HTML_EXT -> ZipProjectImporter.ResourceType.HTML
        in CSS_EXT -> ZipProjectImporter.ResourceType.CSS
        in JS_EXT -> ZipProjectImporter.ResourceType.JS
        in IMG_EXT -> ZipProjectImporter.ResourceType.IMAGE
        in FONT_EXT -> ZipProjectImporter.ResourceType.FONT
        in AUDIO_EXT -> ZipProjectImporter.ResourceType.AUDIO
        in VIDEO_EXT -> ZipProjectImporter.ResourceType.VIDEO
        in DATA_EXT -> ZipProjectImporter.ResourceType.DATA
        else -> ZipProjectImporter.ResourceType.OTHER
    }
}
