package com.webtoapp.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.webtoapp.core.extension.ExtensionModule
import com.webtoapp.core.extension.ModuleCategory
import com.webtoapp.core.extension.QrCodeUtils
import com.webtoapp.core.i18n.Strings
import java.io.File
import java.io.FileOutputStream

/**
 * 二维码海报分享对话框
 */
@Composable
fun QrCodeShareDialog(
    module: ExtensionModule,
    shareCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Check是否可以生成二维码
    val canGenerate = QrCodeUtils.canGenerateQrCode(shareCode)
    val contentSize = QrCodeUtils.getContentSize(shareCode)
    
    // Get当前语言的海报文本
    val scanText = Strings.scanToImportModule
    val subtitleText = Strings.extensionModuleSubtitle
    
    // Generate海报
    val posterBitmap = remember(shareCode, module, scanText, subtitleText) {
        if (canGenerate) {
            generatePoster(module, shareCode, scanText, subtitleText)
        } else null
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.shareModule,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = Strings.close
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (canGenerate && posterBitmap != null) {
                    // 海报预览
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = posterBitmap.asImageBitmap(),
                            contentDescription = Strings.sharePoster,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // 操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Save海报
                        OutlinedButton(
                            onClick = {
                                savePosterToGallery(context, posterBitmap, module.name)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.savePoster)
                        }
                        
                        // 分享海报
                        Button(
                            onClick = {
                                sharePoster(context, posterBitmap, module.name)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.sharePosterBtn)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 提示文字
                    Text(
                        text = Strings.scanQrToImport,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                } else {
                    // 分享码太长，无法生成二维码，提供文件分享替代方案
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = Strings.moduleTooLargeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = Strings.moduleTooLargeDesc.format(contentSize),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // File分享按钮
                        Button(
                            onClick = {
                                shareModuleFile(context, module)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.shareModuleFile)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = Strings.shareFileHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * 生成分享海报
 */
private fun generatePoster(module: ExtensionModule, shareCode: String, scanText: String, subtitleText: String): Bitmap? {
    val qrCodeBitmap = QrCodeUtils.generateQrCode(shareCode, 400) ?: return null
    
    // 海报尺寸
    val posterWidth = 720
    val posterHeight = 1080
    
    val poster = Bitmap.createBitmap(posterWidth, posterHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(poster)
    
    // Get分类颜色
    val categoryColor = getCategoryColor(module.category)
    val categoryColorLight = adjustAlpha(categoryColor, 0.15f)
    
    // 绘制渐变背景
    val bgGradient = LinearGradient(
        0f, 0f, posterWidth.toFloat(), posterHeight.toFloat(),
        intArrayOf(
            Color.WHITE,
            categoryColorLight,
            Color.WHITE
        ),
        floatArrayOf(0f, 0.5f, 1f),
        Shader.TileMode.CLAMP
    )
    val bgPaint = Paint().apply {
        shader = bgGradient
    }
    canvas.drawRect(0f, 0f, posterWidth.toFloat(), posterHeight.toFloat(), bgPaint)
    
    // 顶部装饰条
    val headerPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, posterWidth.toFloat(), 0f,
            categoryColor,
            adjustAlpha(categoryColor, 0.7f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, posterWidth.toFloat(), 8f, headerPaint)
    
    // 分类图标和名称区域
    val categoryY = 60f
    val categoryPaint = Paint().apply {
        isAntiAlias = true
        color = categoryColor
        textSize = 48f
        typeface = Typeface.DEFAULT_BOLD
    }
    
    // 分类背景圆角矩形
    val categoryBgPaint = Paint().apply {
        isAntiAlias = true
        color = categoryColorLight
    }
    val categoryText = "${module.category.icon} ${module.category.getDisplayName()}"
    val categoryWidth = categoryPaint.measureText(categoryText) + 48f
    val categoryRect = RectF(
        (posterWidth - categoryWidth) / 2,
        categoryY,
        (posterWidth + categoryWidth) / 2,
        categoryY + 64f
    )
    canvas.drawRoundRect(categoryRect, 32f, 32f, categoryBgPaint)
    
    // 绘制分类文字
    categoryPaint.textAlign = Paint.Align.CENTER
    canvas.drawText(categoryText, posterWidth / 2f, categoryY + 46f, categoryPaint)
    
    // Module名称
    val namePaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#1a1a1a")
        textSize = 56f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    
    // Handle长名称换行
    val nameLines = wrapText(module.name, namePaint, posterWidth - 80f)
    var nameY = 180f
    for (line in nameLines) {
        canvas.drawText(line, posterWidth / 2f, nameY, namePaint)
        nameY += 68f
    }
    
    // Module描述
    if (module.description.isNotBlank()) {
        val descPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#666666")
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }
        
        val descLines = wrapText(module.description, descPaint, posterWidth - 100f, maxLines = 3)
        var descY = nameY + 20f
        for (line in descLines) {
            canvas.drawText(line, posterWidth / 2f, descY, descPaint)
            descY += 44f
        }
    }
    
    // 二维码区域
    val qrSize = 320
    val qrX = (posterWidth - qrSize) / 2
    val qrY = 420
    
    // 二维码背景卡片
    val cardPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        setShadowLayer(20f, 0f, 8f, Color.parseColor("#20000000"))
    }
    val cardRect = RectF(
        qrX - 30f, qrY - 30f,
        qrX + qrSize + 30f, qrY + qrSize + 30f
    )
    canvas.drawRoundRect(cardRect, 24f, 24f, cardPaint)
    
    // 绘制二维码
    canvas.drawBitmap(qrCodeBitmap, qrX.toFloat(), qrY.toFloat(), null)
    
    // 扫码提示
    val scanPaint = Paint().apply {
        isAntiAlias = true
        color = categoryColor
        textSize = 30f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(scanText, posterWidth / 2f, qrY + qrSize + 80f, scanPaint)
    
    // 标签区域
    if (module.tags.isNotEmpty()) {
        val tagPaint = Paint().apply {
            isAntiAlias = true
            textSize = 26f
        }
        val tagBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#f0f0f0")
        }
        
        val tagsToShow = module.tags.take(4)
        var tagX = 40f
        val tagY = qrY + qrSize + 140f
        val tagHeight = 42f
        val tagPadding = 20f
        val maxTagWidth = posterWidth - 80f
        var currentRowWidth = 0f
        
        for (tag in tagsToShow) {
            val tagText = "#$tag"
            tagPaint.color = Color.parseColor("#888888")
            val tagWidth = tagPaint.measureText(tagText) + tagPadding * 2
            
            if (currentRowWidth + tagWidth > maxTagWidth && currentRowWidth > 0) {
                break // 一行放不下就不放了
            }
            
            // 标签背景
            val tagRect = RectF(tagX, tagY, tagX + tagWidth, tagY + tagHeight)
            canvas.drawRoundRect(tagRect, tagHeight / 2, tagHeight / 2, tagBgPaint)
            
            // 标签文字
            canvas.drawText(tagText, tagX + tagPadding, tagY + 30f, tagPaint)
            
            tagX += tagWidth + 12f
            currentRowWidth += tagWidth + 12f
        }
    }
    
    // 底部品牌区域
    val bottomY = posterHeight - 100f
    
    // 分隔线
    val linePaint = Paint().apply {
        color = Color.parseColor("#e0e0e0")
        strokeWidth = 1f
    }
    canvas.drawLine(60f, bottomY - 40f, posterWidth - 60f, bottomY - 40f, linePaint)
    
    // 品牌 logo 文字
    val brandPaint = Paint().apply {
        isAntiAlias = true
        color = categoryColor
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("WebToApp", posterWidth / 2f, bottomY + 10f, brandPaint)
    
    // 副标题
    val subtitlePaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#999999")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(subtitleText, posterWidth / 2f, bottomY + 46f, subtitlePaint)
    
    return poster
}

/**
 * 文字换行处理
 */
private fun wrapText(text: String, paint: Paint, maxWidth: Float, maxLines: Int = 2): List<String> {
    val lines = mutableListOf<String>()
    var remaining = text
    
    while (remaining.isNotEmpty() && lines.size < maxLines) {
        var end = remaining.length
        while (end > 0 && paint.measureText(remaining.substring(0, end)) > maxWidth) {
            end--
        }
        if (end == 0) end = 1
        
        val line = remaining.substring(0, end)
        lines.add(if (lines.size == maxLines - 1 && end < remaining.length) {
            "${line.dropLast(1)}..."
        } else line)
        remaining = remaining.substring(end).trimStart()
    }
    
    return lines
}

/**
 * 获取分类颜色
 */
private fun getCategoryColor(category: ModuleCategory): Int {
    return when (category) {
        ModuleCategory.CONTENT_FILTER -> Color.parseColor("#FF5722")
        ModuleCategory.CONTENT_ENHANCE -> Color.parseColor("#4CAF50")
        ModuleCategory.STYLE_MODIFIER -> Color.parseColor("#9C27B0")
        ModuleCategory.THEME -> Color.parseColor("#E91E63")
        ModuleCategory.FUNCTION_ENHANCE -> Color.parseColor("#2196F3")
        ModuleCategory.AUTOMATION -> Color.parseColor("#00BCD4")
        ModuleCategory.NAVIGATION -> Color.parseColor("#009688")
        ModuleCategory.DATA_EXTRACT -> Color.parseColor("#673AB7")
        ModuleCategory.DATA_SAVE -> Color.parseColor("#3F51B5")
        ModuleCategory.INTERACTION -> Color.parseColor("#FF9800")
        ModuleCategory.ACCESSIBILITY -> Color.parseColor("#795548")
        ModuleCategory.MEDIA -> Color.parseColor("#E91E63")
        ModuleCategory.VIDEO -> Color.parseColor("#F44336")
        ModuleCategory.IMAGE -> Color.parseColor("#8BC34A")
        ModuleCategory.AUDIO -> Color.parseColor("#FF5722")
        ModuleCategory.SECURITY -> Color.parseColor("#F44336")
        ModuleCategory.ANTI_TRACKING -> Color.parseColor("#607D8B")
        ModuleCategory.SOCIAL -> Color.parseColor("#03A9F4")
        ModuleCategory.SHOPPING -> Color.parseColor("#FF9800")
        ModuleCategory.READING -> Color.parseColor("#8D6E63")
        ModuleCategory.TRANSLATE -> Color.parseColor("#00BCD4")
        ModuleCategory.DEVELOPER -> Color.parseColor("#607D8B")
        ModuleCategory.OTHER -> Color.parseColor("#9E9E9E")
    }
}

/**
 * 调整颜色透明度
 */
private fun adjustAlpha(color: Int, factor: Float): Int {
    val alpha = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
    return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}

/**
 * 保存海报到相册
 */
private fun savePosterToGallery(context: Context, bitmap: Bitmap, moduleName: String) {
    try {
        val filename = "WebToApp_${moduleName}_${System.currentTimeMillis()}.png"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WebToApp")
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val webToAppDir = File(picturesDir, "WebToApp")
            if (!webToAppDir.exists()) {
                webToAppDir.mkdirs()
            }
            val file = File(webToAppDir, filename)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }
        
        Toast.makeText(context, Strings.posterSavedToGallery, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, Strings.saveFailed.format(e.message), Toast.LENGTH_SHORT).show()
    }
}

/**
 * 分享海报
 */
private fun sharePoster(context: Context, bitmap: Bitmap, moduleName: String) {
    try {
        val cacheDir = File(context.cacheDir, "share_poster")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val file = File(cacheDir, "poster_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, Strings.shareModuleText.format(moduleName))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, Strings.shareModule))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, Strings.shareFailed.format(e.message), Toast.LENGTH_SHORT).show()
    }
}

/**
 * 分享模块文件
 */
private fun shareModuleFile(context: Context, module: ExtensionModule) {
    try {
        // Create临时文件
        val cacheDir = File(context.cacheDir, "share_modules")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        // File名使用模块名称
        val safeFileName = module.name.replace(Regex("[^a-zA-Z0-9\u4e00-\u9fa5_-]"), "_")
        val file = File(cacheDir, "${safeFileName}.wtamod")
        
        // 写入模块 JSON 数据
        file.writeText(module.toJson())
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, Strings.shareModuleFileSubject.format(module.name))
            putExtra(Intent.EXTRA_TEXT, Strings.shareModuleFileText.format(module.name))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, Strings.shareModuleFile))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, Strings.shareFailed.format(e.message), Toast.LENGTH_SHORT).show()
    }
}
