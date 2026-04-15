package com.webtoapp.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.R
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.stats.HealthStatus
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.GalleryItemType
import com.webtoapp.data.model.WebApp
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.theme.AppColors
import java.io.File

@Composable
fun AppCard(
    app: WebApp,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onEditCore: () -> Unit = {},
    onDelete: () -> Unit,
    onCreateShortcut: () -> Unit = {},
    onExport: () -> Unit = {},
    onBuildApk: () -> Unit = {},
    onShareApk: () -> Unit = {},
    onMoveToCategory: () -> Unit = {},
    healthStatus: HealthStatus? = null,
    previewImageLoader: ImageLoader,
    screenshotPath: String? = null,
    screenshotVersion: Int = 0,
    isScreenshotLoading: Boolean = false,
    onCaptureScreenshot: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    EnhancedElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppCardIcon(
                app = app,
                healthStatus = healthStatus
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = appCardSubtitle(app),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppTypeChip(appType = app.appType)
                    if (app.activationEnabled) {
                        FeatureChip(icon = Icons.Outlined.Key, label = AppStringsProvider.current().activationCodeVerify)
                    }
                    if (app.adBlockEnabled) {
                        FeatureChip(icon = Icons.Outlined.Block, label = AppStringsProvider.current().adBlocking)
                    }
                    if (app.announcementEnabled) {
                        FeatureChip(icon = Icons.Outlined.Info, label = AppStringsProvider.current().popupAnnouncement)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            AppCardPreview(
                app = app,
                previewImageLoader = previewImageLoader,
                screenshotPath = screenshotPath,
                screenshotVersion = screenshotVersion,
                isScreenshotLoading = isScreenshotLoading,
                onCaptureScreenshot = onCaptureScreenshot
            )

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = AppStringsProvider.current().more)
                }

                AppCardMenu(
                    expanded = expanded,
                    appType = app.appType,
                    onDismiss = { expanded = false },
                    onEdit = onEdit,
                    onEditCore = onEditCore,
                    onCreateShortcut = onCreateShortcut,
                    onBuildApk = onBuildApk,
                    onShareApk = onShareApk,
                    onExport = onExport,
                    onMoveToCategory = onMoveToCategory,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun AppCardIcon(
    app: WebApp,
    healthStatus: HealthStatus?
) {
    Box {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            if (app.iconPath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(app.iconPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = app.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(defaultAppTypeIconRes(app.appType)),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (healthStatus != null && healthStatus != HealthStatus.UNKNOWN) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(healthDotColor(healthStatus))
            )
        }
    }
}

@Composable
private fun AppCardPreview(
    app: WebApp,
    previewImageLoader: ImageLoader,
    screenshotPath: String?,
    screenshotVersion: Int,
    isScreenshotLoading: Boolean,
    onCaptureScreenshot: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .width(30.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = onCaptureScreenshot != null) {
                val clickMessage =
                    "thumbnail tapped: appId=${app.id}, hasHandler=${onCaptureScreenshot != null}, hasPath=${screenshotPath != null}, version=$screenshotVersion, loading=$isScreenshotLoading"
                AppLogger.i("ScreenshotFlow", clickMessage)
                android.util.Log.i("ScreenshotFlow", clickMessage)
                onCaptureScreenshot?.invoke()
            },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        if (screenshotPath != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(screenshotPath))
                        .setParameter("v", screenshotVersion)
                        .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                        .crossfade(true)
                        .build(),
                    imageLoader = previewImageLoader,
                    contentDescription = AppStringsProvider.current().btnPreview,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                if (isScreenshotLoading) {
                    AppCardPreviewLoading()
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isScreenshotLoading -> AppCardPreviewLoading()
                    onCaptureScreenshot != null -> {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = AppStringsProvider.current().btnPreview,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        Icon(
                            painter = painterResource(defaultAppTypeIconRes(app.appType)),
                            contentDescription = AppStringsProvider.current().btnPreview,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppCardPreviewLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(12.dp),
            strokeWidth = 1.5.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun appCardSubtitle(app: WebApp): String {
    return when (app.appType) {
        AppType.IMAGE,
        AppType.VIDEO -> app.mediaConfig?.mediaPath?.let { File(it).name } ?: app.url
        AppType.HTML -> app.htmlConfig?.entryFile?.takeIf { it.isNotBlank() } ?: "index.html"
        AppType.FRONTEND -> {
            app.htmlConfig?.entryFile?.takeIf { it.isNotBlank() }
                ?: app.htmlConfig?.projectDir?.let { File(it).name }
                ?: "index.html"
        }
        AppType.GALLERY -> {
            val config = app.galleryConfig
            if (config != null && config.items.isNotEmpty()) {
                val imageCount = config.items.count { it.type == GalleryItemType.IMAGE }
                val videoCount = config.items.count { it.type == GalleryItemType.VIDEO }
                buildString {
                    if (imageCount > 0) append("$imageCount ${AppStringsProvider.current().galleryImages}")
                    if (imageCount > 0 && videoCount > 0) append(", ")
                    if (videoCount > 0) append("$videoCount ${AppStringsProvider.current().galleryVideos}")
                }
            } else {
                AppStringsProvider.current().galleryEmpty
            }
        }
        else -> app.url
    }
}

private fun defaultAppTypeIconRes(appType: AppType): Int {
    return when (appType) {
        AppType.WEB -> R.drawable.ic_type_web
        AppType.IMAGE,
        AppType.VIDEO -> R.drawable.ic_type_media
        AppType.HTML -> R.drawable.ic_type_html
        AppType.GALLERY -> R.drawable.ic_type_gallery
        AppType.FRONTEND -> R.drawable.ic_type_frontend
        AppType.WORDPRESS -> R.drawable.ic_type_wordpress
        AppType.NODEJS_APP -> R.drawable.ic_type_nodejs
        AppType.PHP_APP -> R.drawable.ic_type_php
        AppType.PYTHON_APP -> R.drawable.ic_type_python
        AppType.GO_APP -> R.drawable.ic_type_go
        AppType.MULTI_WEB -> R.drawable.ic_type_web
    }
}

private fun healthDotColor(healthStatus: HealthStatus): Color {
    return when (healthStatus) {
        HealthStatus.ONLINE -> AppColors.Success
        HealthStatus.SLOW -> Color(0xFFFFC107)
        HealthStatus.OFFLINE -> Color(0xFFF44336)
        HealthStatus.UNKNOWN -> Color(0xFF9E9E9E)
    }
}
