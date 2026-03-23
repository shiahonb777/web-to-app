package com.webtoapp.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 状态栏图片裁剪对话框
 * 
 * 功能特点：
 * - 固定裁剪比例：屏幕宽度 × 状态栏高度
 * - 上下拖动选择裁剪区域
 * - 实时预览裁剪效果
 * - 自动保存裁剪后的图片
 * 
 * @param imageUri 原始图片 URI
 * @param statusBarHeightDp 状态栏高度（dp），0 表示使用系统默认
 * @param onCropComplete 裁剪完成回调，返回裁剪后的图片路径
 * @param onDismiss 取消回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusBarImageCropper(
    imageUri: Uri,
    statusBarHeightDp: Int,
    onCropComplete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    // Get屏幕宽度
    val screenWidthPx = remember {
        context.resources.displayMetrics.widthPixels
    }
    
    // Get实际状态栏高度
    val systemStatusBarHeight = remember {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            with(density) { 24.dp.roundToPx() }
        }
    }
    
    // 使用自定义高度或系统默认高度
    val targetHeightPx = remember(statusBarHeightDp) {
        if (statusBarHeightDp > 0) {
            with(density) { statusBarHeightDp.dp.roundToPx() }
        } else {
            systemStatusBarHeight
        }
    }
    
    // 计算裁剪比例（宽:高）
    val cropAspectRatio = remember(screenWidthPx, targetHeightPx) {
        screenWidthPx.toFloat() / targetHeightPx.toFloat()
    }
    
    // Load原始图片
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 预览区域尺寸
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    
    // 裁剪位置（Y 偏移量，相对于缩放后的图片）
    var cropOffsetY by remember { mutableFloatStateOf(0f) }
    
    // 计算缩放比例（图片缩放到预览区域宽度）
    val scaleFactor = remember(originalBitmap, previewSize) {
        originalBitmap?.let { bitmap ->
            if (previewSize.width > 0 && bitmap.width > 0) {
                previewSize.width.toFloat() / bitmap.width.toFloat()
            } else 1f
        } ?: 1f
    }
    
    // Zoom后的图片高度
    val scaledImageHeight = remember(originalBitmap, scaleFactor) {
        originalBitmap?.let { (it.height * scaleFactor).toInt() } ?: 0
    }
    
    // 裁剪框高度（在预览中的像素高度）
    val cropBoxHeightPx = remember(previewSize, cropAspectRatio) {
        if (previewSize.width > 0) {
            (previewSize.width / cropAspectRatio).toInt()
        } else {
            with(density) { targetHeightPx.toDp().roundToPx() }
        }
    }
    
    // Max偏移量
    val maxOffsetY = remember(scaledImageHeight, cropBoxHeightPx) {
        (scaledImageHeight - cropBoxHeightPx).coerceAtLeast(0).toFloat()
    }
    
    // Load图片
    LaunchedEffect(imageUri) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    // 先获取图片尺寸
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                    
                    // 重新打开流并解码
                    context.contentResolver.openInputStream(imageUri)?.use { stream ->
                        val decodeOptions = BitmapFactory.Options().apply {
                            // 如果图片太大，进行采样
                            val maxDimension = 2048
                            if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
                                inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
                            }
                        }
                        val bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
                        if (bitmap != null) {
                            originalBitmap = bitmap
                        } else {
                            errorMessage = Strings.cannotParseImage
                        }
                    }
                } ?: run {
                    errorMessage = Strings.cannotOpenImage
                }
            } catch (e: Exception) {
                errorMessage = Strings.loadImageFailed.format(e.message)
            }
        }
        isLoading = false
    }
    
    // Initialize裁剪位置为中间
    LaunchedEffect(originalBitmap, previewSize, cropBoxHeightPx) {
        if (originalBitmap != null && previewSize.width > 0) {
            val scaledHeight = originalBitmap!!.height * (previewSize.width.toFloat() / originalBitmap!!.width.coerceAtLeast(1))
            cropOffsetY = ((scaledHeight - cropBoxHeightPx) / 2).coerceAtLeast(0f)
        }
    }
    
    // 裁剪处理状态
    var isCropping by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = { if (!isCropping) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CropFree, contentDescription = null)
                Text(Strings.cropStatusBarBg)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 提示信息
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = Strings.dragToSelectArea,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                // Image预览和裁剪区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .onSizeChanged { previewSize = it },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = Strings.loadingImage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        errorMessage != null -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        originalBitmap != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(maxOffsetY) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            cropOffsetY = (cropOffsetY + dragAmount.y).coerceIn(0f, maxOffsetY)
                                        }
                                    }
                            ) {
                                // 原始图片
                                Image(
                                    bitmap = originalBitmap!!.asImageBitmap(),
                                    contentDescription = Strings.originalImage,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.FillWidth
                                )
                                
                                // 半透明遮罩（上方）
                                if (cropOffsetY > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(with(density) { cropOffsetY.toDp() })
                                            .background(Color.Black.copy(alpha = 0.6f))
                                    )
                                }
                                
                                // 裁剪框边框
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(with(density) { cropBoxHeightPx.toDp() })
                                        .offset(y = with(density) { cropOffsetY.toDp() })
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(0.dp)
                                        )
                                )
                                
                                // 半透明遮罩（下方）
                                val bottomMaskTop = cropOffsetY + cropBoxHeightPx
                                val bottomMaskHeight = (scaledImageHeight - bottomMaskTop).coerceAtLeast(0f)
                                if (bottomMaskHeight > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(with(density) { bottomMaskHeight.toDp() })
                                            .offset(y = with(density) { bottomMaskTop.toDp() })
                                            .background(Color.Black.copy(alpha = 0.6f))
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 裁剪信息
                if (originalBitmap != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = Strings.cropSize,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${screenWidthPx} × ${targetHeightPx} px",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Strings.originalSize,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${originalBitmap!!.width} × ${originalBitmap!!.height} px",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                
                // 裁剪进度
                if (isCropping) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    originalBitmap?.let { bitmap ->
                        isCropping = true
                        scope.launch {
                            val result = cropAndSave(
                                context = context,
                                bitmap = bitmap,
                                cropOffsetY = cropOffsetY,
                                scaleFactor = scaleFactor,
                                targetWidthPx = screenWidthPx,
                                targetHeightPx = targetHeightPx
                            )
                            isCropping = false
                            if (result != null) {
                                onCropComplete(result)
                            }
                        }
                    }
                },
                enabled = originalBitmap != null && !isCropping
            ) {
                Text(Strings.confirmCrop)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCropping
            ) {
                Text(Strings.btnCancel)
            }
        }
    )
}

/**
 * 计算采样率
 */
private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1
    
    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    
    return inSampleSize
}

/**
 * 执行裁剪并保存
 * 
 * @return 裁剪后的图片路径，失败返回 null
 */
private suspend fun cropAndSave(
    context: android.content.Context,
    bitmap: Bitmap,
    cropOffsetY: Float,
    scaleFactor: Float,
    targetWidthPx: Int,
    targetHeightPx: Int
): String? = withContext(Dispatchers.IO) {
    try {
        // 计算原图上的裁剪区域
        val originalY = (cropOffsetY / scaleFactor).toInt().coerceIn(0, bitmap.height - 1)
        val originalCropHeight = (targetHeightPx / scaleFactor).toInt()
            .coerceIn(1, bitmap.height - originalY)
        
        // 裁剪原图
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            0,
            originalY,
            bitmap.width,
            originalCropHeight.coerceAtMost(bitmap.height - originalY)
        )
        
        // Zoom到目标尺寸
        val scaledBitmap = Bitmap.createScaledBitmap(
            croppedBitmap,
            targetWidthPx,
            targetHeightPx,
            true
        )
        
        // Save到应用私有目录
        val statusBarDir = File(context.filesDir, "statusbar_backgrounds")
        if (!statusBarDir.exists()) {
            statusBarDir.mkdirs()
        }
        
        val outputFile = File(statusBarDir, "statusbar_bg_${System.currentTimeMillis()}.png")
        FileOutputStream(outputFile).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        // 回收临时 bitmap
        if (croppedBitmap != bitmap) {
            croppedBitmap.recycle()
        }
        if (scaledBitmap != croppedBitmap) {
            scaledBitmap.recycle()
        }
        
        outputFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
