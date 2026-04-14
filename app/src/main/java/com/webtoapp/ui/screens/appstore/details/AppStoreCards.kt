package com.webtoapp.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webtoapp.core.cloud.AppDownloadManager
import com.webtoapp.core.cloud.AppStoreItem
import com.webtoapp.core.cloud.AppStoreListResponse
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.StoreModuleInfo
import com.webtoapp.core.cloud.ActivationCode
import com.webtoapp.core.cloud.ActivationSettings
import com.webtoapp.core.cloud.Announcement
import com.webtoapp.core.cloud.UpdateConfig
import com.webtoapp.core.cloud.AppUser
import com.webtoapp.core.cloud.GeoDistribution
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumFilterChip
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.screens.community.Avatar
import com.webtoapp.ui.screens.community.GlassDivider
import com.webtoapp.ui.screens.community.CommunityPhysics
import com.webtoapp.ui.screens.community.LikeBurstEffect
import com.webtoapp.ui.screens.community.AnimatedCounter
import com.webtoapp.ui.screens.community.StaggeredItem
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun ModuleStoreCard(
    module: StoreModuleInfo,
    installedTracker: com.webtoapp.core.cloud.InstalledItemsTracker,
    onClick: () -> Unit,
    onInstall: () -> Unit
) {
    val isInstalled = installedTracker.isInstalled(module.id)
    EnhancedElevatedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Gradient icon
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Extension,
                            null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(module.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(weight = 1f, fill = false))
                        if (module.isFeatured) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFFA726).copy(alpha = 0.12f)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(Icons.Filled.Star, null, modifier = Modifier.size(10.dp), tint = Color(0xFFFFA726))
                                    Text(Strings.moduleStoreFeatured, fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFFA726), letterSpacing = 0.3.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("by ${module.authorName}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        letterSpacing = 0.2.sp)
                }
            }

            if (!module.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(module.description, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    lineHeight = 18.sp)
            }

            // Tags as styled pills
            if (module.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    module.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ) {
                            Text(tag, fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                letterSpacing = 0.2.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom bar: stats pills + install button
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Downloads pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Outlined.Download, null, modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Text("${module.downloads}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Rating pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFC107).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Filled.Star, null, modifier = Modifier.size(11.dp),
                            tint = Color(0xFFFFC107))
                        Text(
                            if (module.ratingCount > 0) "${module.rating}" else "-",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFC107)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Version pill
                module.versionName?.let {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text("v$it", fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.weight(weight = 1f, fill = true))

                // Install button — shows Strings.storeInstalled if already installed
                if (isInstalled) {
                    FilledTonalButton(
                        onClick = onClick,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
                        )
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(14.dp),
                            tint = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.storeInstalled, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4CAF50))
                    }
                } else {
                    Button(
                        onClick = onInstall,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Outlined.Download, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.moduleStoreInstall, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}


// ════════════════════════════════════════════════
// applistcard- Premium Design
// ════════════════════════════════════════════════

@Composable
internal fun AppListCard(
    app: AppStoreItem,
    onClick: () -> Unit,
    downloadManager: AppDownloadManager? = null
) {
    val ratingColor = when {
        app.rating >= 4.0f -> Color(0xFF10B981)
        app.rating >= 3.0f -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    val accentColor = when {
        app.isFeatured -> Color(0xFFF59E0B)
        app.rating >= 4.5f -> Color(0xFF10B981)
        else -> MaterialTheme.colorScheme.primary
    }

    // Check download state
    val tasks = downloadManager?.activeTasks?.collectAsState()?.value ?: emptyMap()
    val downloadedList = downloadManager?.downloadedApps?.collectAsState()?.value ?: emptyList()
    val currentTask = tasks[app.id]
    val isDownloaded = downloadedList.any { it.appId == app.id }
    val isActiveDownload = currentTask?.status == AppDownloadManager.DownloadStatus.DOWNLOADING
    val downloadProgress = currentTask?.progress ?: 0f

    EnhancedElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // ─ Accent strip on left edge ─
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(80.dp)
                    .align(Alignment.CenterVertically)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.0f),
                                accentColor.copy(alpha = 0.8f),
                                accentColor.copy(alpha = 0.0f)
                            )
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ─ App icon with subtle shadow ─
                Box {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = RoundedCornerShape(15.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 2.dp
                    ) {
                        if (!app.icon.isNullOrBlank()) {
                            AsyncImage(
                                model = app.icon,
                                contentDescription = app.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(15.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Apps,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // ─ Info column ─
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Title row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            app.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (app.isFeatured) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Star,
                                        null,
                                        modifier = Modifier.size(10.dp),
                                        tint = Color(0xFFF59E0B)
                                    )
                                    Text(
                                        Strings.selected,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFF59E0B),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }

                    // Author + version
                    Text(
                        "${app.authorName} · v${app.versionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        letterSpacing = 0.2.sp
                    )

                    // Stats pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rating pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ratingColor.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    null,
                                    modifier = Modifier.size(11.dp),
                                    tint = ratingColor
                                )
                                Text(
                                    String.format("%.1f", app.rating),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ratingColor
                                )
                            }
                        }

                        // Download count pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Download,
                                    null,
                                    modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    formatDownloads(app.downloads),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // ─ Download / Installed button ─
                when {
                    isDownloaded -> {
                        // Already downloaded — green check
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, null,
                                    modifier = Modifier.size(15.dp),
                                    tint = Color(0xFF4CAF50))
                                Text(Strings.storeModulesInstalled, fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4CAF50))
                            }
                        }
                    }
                    isActiveDownload -> {
                        // Downloading — progress ring
                        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.size(30.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                            Text(
                                "${(downloadProgress * 100).toInt()}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    else -> {
                        // Not downloaded — download button (opens detail)
                        FilledTonalButton(
                            onClick = onClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Outlined.Download, null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Strings.storeGet, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}


// ════════════════════════════════════════════════
// app bottomdialog
// ════════════════════════════════════════════════
