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

// ════════════════════════════════════════════════
// 应用管理控制台 (Premium UI)
// ════════════════════════════════════════════════

/** 管理控制台用渐变色组 */
internal val mgmtGradientBlue = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
internal val mgmtGradientGreen = listOf(Color(0xFF11998E), Color(0xFF38EF7D))
internal val mgmtGradientOrange = listOf(Color(0xFFF7971E), Color(0xFFFFD200))
internal val mgmtGradientRed = listOf(Color(0xFFEB3349), Color(0xFFF45C43))
internal val mgmtGradientPurple = listOf(Color(0xFFa18cd1), Color(0xFFfbc2eb))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppManagementSheet(
    app: AppStoreItem,
    apiClient: CloudApiClient,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    data class MgmtTab(val icon: ImageVector, val label: String, val gradient: List<Color>)
    val tabs = listOf(
        MgmtTab(Icons.Outlined.Dashboard, "Overview", mgmtGradientBlue),
        MgmtTab(Icons.Outlined.VpnKey, "Activation Code", mgmtGradientPurple),
        MgmtTab(Icons.Outlined.Campaign, "Announcements", mgmtGradientOrange),
        MgmtTab(Icons.Outlined.SystemUpdate, "Updates", mgmtGradientGreen),
        MgmtTab(Icons.Outlined.People, "Users", mgmtGradientRed)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.95f),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ══ Premium Header ══
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2))))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(52.dp),
                        shadowElevation = 8.dp
                    ) {
                        if (!app.icon.isNullOrBlank()) {
                            AsyncImage(
                                model = app.icon, contentDescription = app.name,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Apps, null, Modifier.size(24.dp), tint = Color.White)
                            }
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.2f)) {
                                Text("v${app.versionName}", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.2f)) {
                                Text("Management Console", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ══ Premium Pill-Style Tab Bar ══
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { idx, tab ->
                    val isSelected = selectedTab == idx
                    Surface(
                        onClick = { selectedTab = idx },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        modifier = Modifier.height(42.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Brush.linearGradient(tab.gradient)
                                    else Brush.linearGradient(listOf(
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    )),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(tab.icon, null, Modifier.size(16.dp), tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Text(tab.label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when (selectedTab) {
                0 -> ManagementOverviewTab(app, apiClient, scope, onAppUpdated = { /* refresh */ }, onAppDeleted = { /* close sheet */ })
                1 -> ManagementActivationTab(app, apiClient, scope)
                2 -> ManagementAnnouncementTab(app, apiClient, scope)
                3 -> ManagementUpdateTab(app, apiClient, scope)
                4 -> ManagementUsersTab(app, apiClient, scope)
            }
        }
    }
}

/** 渐变统计迷你卡 */
@Composable
internal fun GradientMiniStat(gradient: List<Color>, icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(gradient)).padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(18.dp), tint = Color.White.copy(alpha = 0.85f))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-0.5).sp)
            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Medium)
        }
    }
}

/** 信息行（带左色条） */
@Composable
internal fun OverviewInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

// ── 概览 Tab ──
