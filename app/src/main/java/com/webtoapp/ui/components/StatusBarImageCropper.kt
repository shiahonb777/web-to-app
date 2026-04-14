package com.webtoapp.ui.components

import android.graphics.Bitmap
import com.webtoapp.core.logging.AppLogger
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
import com.webtoapp.core.i18n.AppStringsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * status bar dialog
 * 
 * Note
 * × status bar
 * select area
 * preview
 * save
 * 
 * @param imageUri URI
 * @param statusBarHeightDp status bar( dp) , 0 systemdefault
 * @param onCropComplete, back path
 * @param onDismiss
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
    
    // Get
    val screenWidthPx = remember {
        context.resources.displayMetrics.widthPixels
    }
    
    val topInsetPx = WindowInsets.statusBars.getTop(density)
    val systemStatusBarHeight = if (topInsetPx > 0) {
        topInsetPx
    } else {
        with(density) { 24.dp.roundToPx() }
    }
    
    // orsystemdefault
    val targetHeightPx = remember(statusBarHeightDp) {
        if (statusBarHeightDp > 0) {
            with(density) { statusBarHeightDp.dp.roundToPx() }
        } else {
            systemStatusBarHeight
        }
    }
    
    // Note
    val cropAspectRatio = remember(screenWidthPx, targetHeightPx) {
        screenWidthPx.toFloat() / targetHeightPx.toFloat()
    }
    
    // Load
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // previewarea
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    
    // ( Y, )
    var cropOffsetY by remember { mutableFloatStateOf(0f) }
    
    // ( previewarea)
    val scaleFactor = remember(originalBitmap, previewSize) {
        originalBitmap?.let { bitmap ->
            if (previewSize.width > 0 && bitmap.width > 0) {
                previewSize.width.toFloat() / bitmap.width.toFloat()
            } else 1f
        } ?: 1f
    }
    
    // Zoom
    val scaledImageHeight = remember(originalBitmap, scaleFactor) {
        originalBitmap?.let { (it.height * scaleFactor).toInt() } ?: 0
    }
    
    // ( previewin)
    val cropBoxHeightPx = remember(previewSize, cropAspectRatio) {
        if (previewSize.width > 0) {
            (previewSize.width / cropAspectRatio).toInt()
        } else {
            with(density) { targetHeightPx.toDp().roundToPx() }
        }
    }
    
    // Max
    val maxOffsetY = remember(scaledImageHeight, cropBoxHeightPx) {
        (scaledImageHeight - cropBoxHeightPx).coerceAtLeast(0).toFloat()
    }
    
    // Load
    LaunchedEffect(imageUri) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    // Note
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                    
                    // open and
                    context.contentResolver.openInputStream(imageUri)?.use { stream ->
                        val decodeOptions = BitmapFactory.Options().apply {
                            // if,
                            val maxDimension = 2048
                            if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
                                inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
                            }
                        }
                        val bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
                        if (bitmap != null) {
                            originalBitmap = bitmap
                        } else {
                            errorMessage = AppStringsProvider.current().cannotParseImage
                        }
                    }
                } ?: run {
                    errorMessage = AppStringsProvider.current().cannotOpenImage
                }
            } catch (e: Exception) {
                errorMessage = AppStringsProvider.current().loadImageFailed.format(e.message)
            }
        }
        isLoading = false
    }
    
    // Initialize
    LaunchedEffect(originalBitmap, previewSize, cropBoxHeightPx) {
        if (originalBitmap != null && previewSize.width > 0) {
            val scaledHeight = originalBitmap!!.height * (previewSize.width.toFloat() / originalBitmap!!.width.coerceAtLeast(1))
            cropOffsetY = ((scaledHeight - cropBoxHeightPx) / 2).coerceAtLeast(0f)
        }
    }
    
    // handlestate
    var isCropping by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = { if (!isCropping) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CropFree,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(AppStringsProvider.current().cropStatusBarBg)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // hint
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
                            text = AppStringsProvider.current().dragToSelectArea,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                // Imagepreview area
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
                                    text = AppStringsProvider.current().loadingImage,
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
                                // Note
                                Image(
                                    bitmap = originalBitmap!!.asImageBitmap(),
                                    contentDescription = AppStringsProvider.current().originalImage,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.FillWidth
                                )
                                
                                // Note
                                if (cropOffsetY > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(with(density) { cropOffsetY.toDp() })
                                            .background(Color.Black.copy(alpha = 0.6f))
                                    )
                                }
                                
                                // Note
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
                                
                                // Note
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
                
                // Note
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
                                    text = AppStringsProvider.current().cropSize,
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
                                    text = AppStringsProvider.current().originalSize,
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
                
                // Note
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
                Text(AppStringsProvider.current().confirmCrop)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCropping
            ) {
                Text(AppStringsProvider.current().btnCancel)
            }
        }
    )
}

/**
 * Note
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
 * execute andsave
 * 
 * @return path, failedback null
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
        // area
        val originalY = (cropOffsetY / scaleFactor).toInt().coerceIn(0, bitmap.height - 1)
        val originalCropHeight = (targetHeightPx / scaleFactor).toInt()
            .coerceIn(1, bitmap.height - originalY)
        
        // Note
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            0,
            originalY,
            bitmap.width,
            originalCropHeight.coerceAtMost(bitmap.height - originalY)
        )
        
        // Zoom
        val scaledBitmap = Bitmap.createScaledBitmap(
            croppedBitmap,
            targetWidthPx,
            targetHeightPx,
            true
        )
        
        // Save app directory
        val statusBarDir = File(context.filesDir, "statusbar_backgrounds")
        if (!statusBarDir.exists()) {
            statusBarDir.mkdirs()
        }
        
        val outputFile = File(statusBarDir, "statusbar_bg_${System.currentTimeMillis()}.png")
        FileOutputStream(outputFile).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        // bitmap
        if (croppedBitmap != bitmap) {
            croppedBitmap.recycle()
        }
        if (scaledBitmap != croppedBitmap) {
            scaledBitmap.recycle()
        }
        
        outputFile.absolutePath
    } catch (e: Exception) {
        AppLogger.e("StatusBarImageCropper", "Operation failed", e)
        null
    }
}
