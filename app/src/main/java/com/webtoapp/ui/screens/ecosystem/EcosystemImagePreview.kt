package com.webtoapp.ui.screens.ecosystem

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.webtoapp.core.cloud.GitHubAccelerator
import com.webtoapp.core.i18n.Strings
import com.webtoapp.util.MediaSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.Locale

data class EcosystemPreviewImage(
    val originalUrl: String,
    val previewUrl: String = originalUrl
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EcosystemImagePreviewDialog(
    images: List<EcosystemPreviewImage>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (images.isEmpty()) return

    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var activeScale by remember { mutableFloatStateOf(1f) }
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, images.lastIndex),
        pageCount = { images.size }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = activeScale <= 1.02f
                ) { page ->
                    ZoomablePreviewImage(
                        image = images[page],
                        isActivePage = pagerState.currentPage == page,
                        onScaleChanged = { scale -> activeScale = scale }
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Strings.cdBack,
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "${pagerState.currentPage + 1} / ${images.size}",
                        modifier = Modifier.padding(top = 14.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Row {
                        IconButton(
                            onClick = {
                                val sourceUrl = images.getOrNull(pagerState.currentPage)?.originalUrl ?: return@IconButton
                                scope.launch {
                                    savePreviewImage(context, sourceUrl)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Outlined.Download,
                                contentDescription = Strings.notifSaveCompleted,
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = {
                                val sourceUrl = images.getOrNull(pagerState.currentPage)?.originalUrl ?: return@IconButton
                                scope.launch {
                                    sharePreviewImage(context, sourceUrl)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Outlined.Share,
                                contentDescription = Strings.shareImage,
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomablePreviewImage(
    image: EcosystemPreviewImage,
    isActivePage: Boolean,
    onScaleChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    var scale by remember(image.originalUrl, image.previewUrl) { mutableFloatStateOf(1f) }
    var offset by remember(image.originalUrl, image.previewUrl) { mutableStateOf(Offset.Zero) }
    val showOriginalImage = scale > 1.02f && image.originalUrl != image.previewUrl

    LaunchedEffect(scale, isActivePage) {
        if (isActivePage) onScaleChanged(scale)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .pointerInput(image.originalUrl, image.previewUrl) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.02f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            offset = centeredOffsetForTap(
                                tapOffset = tapOffset,
                                containerWidth = size.width.toFloat(),
                                containerHeight = size.height.toFloat(),
                                scale = scale
                            )
                        }
                        if (isActivePage) onScaleChanged(scale)
                    }
                )
            }
            .pointerInput(image.originalUrl, image.previewUrl) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                    val newOffset = if (newScale <= 1.02f) {
                        Offset.Zero
                    } else {
                        clampOffset(
                            current = offset + pan,
                            containerWidth = size.width.toFloat(),
                            containerHeight = size.height.toFloat(),
                            scale = newScale
                        )
                    }
                    scale = if (newScale <= 1.02f) 1f else newScale
                    offset = newOffset
                    if (isActivePage) onScaleChanged(scale)
                }
            },
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(GitHubAccelerator.accelerate(image.previewUrl))
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        if (showOriginalImage) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(GitHubAccelerator.accelerate(image.originalUrl))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                loading = {
                    LoadingPreviewOverlay()
                },
                success = {
                    SubcomposeAsyncImageContent()
                }
            )
        }
    }
}

@Composable
private fun LoadingPreviewOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = Strings.loading,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun centeredOffsetForTap(
    tapOffset: Offset,
    containerWidth: Float,
    containerHeight: Float,
    scale: Float
): Offset {
    val rawOffset = Offset(
        x = (containerWidth / 2f - tapOffset.x) * (scale - 1f),
        y = (containerHeight / 2f - tapOffset.y) * (scale - 1f)
    )
    return clampOffset(
        current = rawOffset,
        containerWidth = containerWidth,
        containerHeight = containerHeight,
        scale = scale
    )
}

private fun clampOffset(
    current: Offset,
    containerWidth: Float,
    containerHeight: Float,
    scale: Float
): Offset {
    val maxX = (containerWidth * (scale - 1f) / 2f).coerceAtLeast(0f)
    val maxY = (containerHeight * (scale - 1f) / 2f).coerceAtLeast(0f)
    return Offset(
        x = current.x.coerceIn(-maxX, maxX),
        y = current.y.coerceIn(-maxY, maxY)
    )
}

private suspend fun savePreviewImage(context: android.content.Context, sourceUrl: String) {
    val resolvedUrl = GitHubAccelerator.accelerate(sourceUrl) ?: sourceUrl
    val fileName = buildPreviewFileName(sourceUrl)
    when (val result = MediaSaver.saveFromUrl(context, resolvedUrl, fileName, guessImageMimeType(fileName))) {
        is MediaSaver.SaveResult.Success -> {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    Strings.savedToGallery.format(fileName),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        is MediaSaver.SaveResult.Error -> {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    Strings.aiImageDownloadFailed.format(result.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

private suspend fun sharePreviewImage(context: android.content.Context, sourceUrl: String) {
    val resolvedUrl = GitHubAccelerator.accelerate(sourceUrl) ?: sourceUrl
    val fileName = buildPreviewFileName(sourceUrl)
    val targetFile = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "ecosystem_share_images").apply { mkdirs() }
        val output = File(cacheDir, fileName)
        URL(resolvedUrl).openStream().use { input ->
            output.outputStream().use { out ->
                input.copyTo(out)
            }
        }
        output
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        targetFile
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = guessImageMimeType(fileName)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    withContext(Dispatchers.Main) {
        try {
            context.startActivity(Intent.createChooser(shareIntent, Strings.shareImage).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(
                context,
                Strings.shareFailed.format(e.message ?: "unknown"),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

private fun buildPreviewFileName(sourceUrl: String): String {
    val parsed = Uri.parse(sourceUrl)
    val lastSegment = parsed.lastPathSegment?.substringAfterLast('/')?.substringBefore('?').orEmpty()
    if (lastSegment.isNotBlank() && lastSegment.contains('.')) {
        return lastSegment
    }
    return "ecosystem_image_${System.currentTimeMillis()}.jpg"
}

private fun guessImageMimeType(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "image/jpeg"
}
