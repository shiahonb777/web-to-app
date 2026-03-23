package com.webtoapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.webtoapp.core.i18n.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 图标库项目
 */
data class IconLibraryItem(
    val id: String = UUID.randomUUID().toString(),
    val path: String,           // Icon文件路径
    val name: String,           // Show名称
    val isAiGenerated: Boolean = false,  // Yes否AI生成
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 图标库存储管理
 */
object IconLibraryStorage {
    
    private const val LIBRARY_DIR = "icon_library"
    private val _iconsFlow = MutableStateFlow<List<IconLibraryItem>>(emptyList())
    val iconsFlow: Flow<List<IconLibraryItem>> = _iconsFlow
    
    /**
     * 初始化并加载图标库
     */
    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        val libraryDir = getLibraryDir(context)
        val icons = mutableListOf<IconLibraryItem>()
        
        libraryDir.listFiles()?.forEach { file ->
            if (file.isFile && isImageFile(file.name)) {
                icons.add(
                    IconLibraryItem(
                        id = file.nameWithoutExtension,
                        path = file.absolutePath,
                        name = file.nameWithoutExtension,
                        isAiGenerated = file.name.startsWith("ai_"),
                        createdAt = file.lastModified()
                    )
                )
            }
        }
        
        _iconsFlow.value = icons.sortedByDescending { it.createdAt }
    }
    
    /**
     * 从 Base64 保存 AI 生成的图标
     */
    suspend fun saveFromBase64(
        context: Context,
        base64Data: String,
        name: String = Strings.aiIcon
    ): IconLibraryItem? = withContext(Dispatchers.IO) {
        try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            
            val id = "ai_${UUID.randomUUID().toString().take(8)}"
            val file = File(getLibraryDir(context), "$id.png")
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            val item = IconLibraryItem(
                id = id,
                path = file.absolutePath,
                name = name,
                isAiGenerated = true
            )
            
            _iconsFlow.value = listOf(item) + _iconsFlow.value
            item
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 从 Uri 保存图标到图标库
     */
    suspend fun saveFromUri(
        context: Context,
        uri: Uri,
        name: String = Strings.icon
    ): IconLibraryItem? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            val id = "lib_${UUID.randomUUID().toString().take(8)}"
            val file = File(getLibraryDir(context), "$id.png")
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            val item = IconLibraryItem(
                id = id,
                path = file.absolutePath,
                name = name,
                isAiGenerated = false
            )
            
            _iconsFlow.value = listOf(item) + _iconsFlow.value
            item
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 删除图标
     */
    suspend fun delete(context: Context, item: IconLibraryItem) = withContext(Dispatchers.IO) {
        try {
            File(item.path).delete()
            _iconsFlow.value = _iconsFlow.value.filter { it.id != item.id }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取所有图标
     */
    fun getIcons(): List<IconLibraryItem> = _iconsFlow.value
    
    private fun getLibraryDir(context: Context): File {
        val dir = File(context.filesDir, LIBRARY_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    private fun isImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".png") || lower.endsWith(".jpg") || 
               lower.endsWith(".jpeg") || lower.endsWith(".webp")
    }
}
