package com.webtoapp.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.core.cloud.ActivationCode
import com.webtoapp.core.cloud.ActivationSettings
import com.webtoapp.core.cloud.Announcement
import com.webtoapp.core.cloud.AppDownloadManager
import com.webtoapp.core.cloud.AppStoreItem
import com.webtoapp.core.cloud.AppStoreListResponse
import com.webtoapp.core.cloud.AppUser
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.GeoDistribution
import com.webtoapp.core.cloud.StoreModuleInfo
import com.webtoapp.core.cloud.UpdateConfig
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.ui.components.ApkExportPreflightPanel
import com.webtoapp.ui.components.PremiumFilterChip
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.design.WtaEmptyState
import com.webtoapp.ui.design.WtaRadius
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSettingCard
import com.webtoapp.ui.design.WtaSize
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.screens.ecosystem.AnimatedCounter
import com.webtoapp.ui.screens.ecosystem.Avatar
import com.webtoapp.ui.screens.ecosystem.EcosystemMotion
import com.webtoapp.ui.screens.ecosystem.GlassDivider
import com.webtoapp.ui.screens.ecosystem.LikeBurstEffect
import com.webtoapp.ui.screens.ecosystem.ModuleCard
import com.webtoapp.ui.screens.ecosystem.StaggeredItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

internal fun formatDownloads(count: Int): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
    count >= 10_000 -> String.format("%.1fK", count / 1_000f)
    else -> count.toString()
}

@Composable
internal fun MyPublishedItemsHeader(
    title: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    pills: @Composable RowScope.() -> Unit
) {
    WtaSettingCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(32.dp)
                ) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Refresh,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                pills()
            }
        }
    }
}




@Composable
internal fun StatsOverviewRow(
    totalDownloads: Int,
    avgRating: Float,
    totalLikes: Int,
    downloadLabel: String = Strings.ecosystemDownload
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Download,
            iconColor = MaterialTheme.colorScheme.primary,
            value = formatDownloads(totalDownloads),
            label = downloadLabel
        )

        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Star,
            iconColor = MaterialTheme.colorScheme.tertiary,
            value = if (avgRating > 0f) String.format("%.1f", avgRating) else "-",
            label = Strings.ecosystemRatingLabel.format(0f).substringBefore(" ")
        )

        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.ThumbUp,
            iconColor = MaterialTheme.colorScheme.secondary,
            value = formatDownloads(totalLikes),
            label = Strings.ecosystemLike
        )
    }
}

@Composable
internal fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String
) {
    WtaSettingCard(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = iconColor)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}




@Composable
internal fun ItemDeleteConfirmDialog(
    itemName: String,
    itemKind: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = {
            Surface(
                shape = RoundedCornerShape(WtaRadius.Card),
                color = Color.Transparent,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                            ),
                            RoundedCornerShape(WtaRadius.Card)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Warning, null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        title = { Text(Strings.confirmDelete, fontWeight = FontWeight.Bold) },
        text = {
            Text("${Strings.confirmDeleteAppMessage.format(itemName)}\n\n$itemKind")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                shape = RoundedCornerShape(WtaRadius.Button),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(if (isDeleting) Strings.deleting else Strings.confirmDelete)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(Strings.ecosystemCancel)
            }
        }
    )
}


@Composable
internal fun PublishedItemLoadingState(message: String) {
    WtaSettingCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.5.dp)
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}


@Composable
internal fun PublishedItemErrorState(errorMsg: String?, onRetry: () -> Unit) {
    WtaStatusBanner(
        title = Strings.operationFailed,
        message = errorMsg ?: Strings.operationFailed,
        tone = WtaStatusTone.Error,
        actionLabel = Strings.retry,
        onAction = onRetry
    )
}


@Composable
internal fun PublishedItemEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onAction: () -> Unit
) {
    WtaEmptyState(
        title = title,
        message = subtitle,
        icon = icon,
        actionLabel = Strings.close,
        onAction = onAction
    )
}


@Composable
internal fun StatPillVersion(text: String) {
    Surface(
        shape = RoundedCornerShape(WtaRadius.Button),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
internal fun StatPillWithIcon(
    icon: ImageVector,
    text: String,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    bgColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Surface(shape = RoundedCornerShape(WtaRadius.Button), color = bgColor) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(10.dp), tint = iconColor)
            Text(text, fontSize = 10.sp, fontWeight = fontWeight, color = textColor)
        }
    }
}


@Composable
internal fun PublishedItemStatsPills(
    versionName: String?,
    downloads: Int,
    rating: Float,
    ratingCount: Int,
    likeCount: Int
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        versionName?.let { StatPillVersion("v$it") }
        StatPillWithIcon(
            icon = Icons.Outlined.Download,
            text = formatDownloads(downloads)
        )
        StatPillWithIcon(
            icon = Icons.Filled.Star,
            text = if (ratingCount > 0) String.format("%.1f", rating) else "-",
            iconColor = MaterialTheme.colorScheme.tertiary,
            textColor = MaterialTheme.colorScheme.tertiary,
            bgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
            fontWeight = FontWeight.SemiBold
        )
        if (likeCount > 0) {
            StatPillWithIcon(
                icon = Icons.Outlined.ThumbUp,
                text = "$likeCount",
                iconColor = MaterialTheme.colorScheme.secondary,
                textColor = MaterialTheme.colorScheme.secondary,
                bgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.32f)
            )
        }
    }
}


@Composable
internal fun CategoryTag(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(WtaRadius.Button),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}








internal data class SheetFailureReport(
    val title: String,
    val summary: String,
    val details: String
)

internal fun buildSheetFailureReport(
    title: String,
    stage: String,
    summary: String,
    contextLines: List<String>,
    throwable: Throwable? = null
): SheetFailureReport {
    throwable?.let { AppLogger.e("EcosystemPublishing", "$title failed at $stage", it) }
    val details = buildString {
        appendLine(title)
        appendLine("stage: $stage")
        appendLine("summary: $summary")
        if (contextLines.isNotEmpty()) {
            appendLine()
            appendLine("context:")
            contextLines.forEach { appendLine(it) }
        }
        if (throwable != null) {
            appendLine()
            appendLine("exception:")
            appendLine(android.util.Log.getStackTraceString(throwable))
        }
        appendLine()
        appendLine("recent_logs:")
        append(AppLogger.getRecentLogTail())
    }
    return SheetFailureReport(title = title, summary = summary, details = details)
}

internal fun buildApkBuildFailureReport(
    context: android.content.Context,
    project: com.webtoapp.data.model.WebApp,
    error: com.webtoapp.core.apkbuilder.BuildResult.Error
): SheetFailureReport {
    val buildLog = com.webtoapp.core.apkbuilder.BuildLogger(context).readLogContent(error.logPath)
    val diagnostic = error.diagnostic

    val details = buildString {
        appendLine("APK 构建失败")
        appendLine("stage: ${diagnostic?.stage?.label ?: "apk_build"}")
        appendLine("summary: ${error.message}")
        if (diagnostic != null) {
            appendLine("cause: ${diagnostic.cause.name}")
        }
        appendLine()
        appendLine("project:")
        appendLine("name=${project.name}")
        appendLine("appType=${project.appType}")
        appendLine("source=${project.url}")
        if (diagnostic?.details?.isNotEmpty() == true) {
            appendLine()
            appendLine("context:")
            diagnostic.details.forEach { (key, value) ->
                appendLine("$key=$value")
            }
        }
        appendLine()
        appendLine("log_path:")
        appendLine(error.logPath ?: "<unavailable>")
        appendLine()
        appendLine("build_log:")
        appendLine(buildLog ?: "<build log unavailable>")
        appendLine()
        appendLine("recent_logs:")
        append(AppLogger.getRecentLogTail())
    }

    return SheetFailureReport(
        title = Strings.apkBuildFailed,
        summary = error.message,
        details = details
    )
}

@Composable
internal fun SheetFailureReportDialog(
    report: SheetFailureReport,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(report.title)
                Text(
                    report.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = report.details,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .padding(bottom = 48.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall
                    )

                    FilledTonalButton(
                        onClick = { clipboardManager.setText(AnnotatedString(report.details)) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Strings.copy)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.close)
            }
        }
    )
}
