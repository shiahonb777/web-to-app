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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webtoapp.core.logging.AppLogger
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
import com.webtoapp.core.cloud.ReviewSubmissionInfo
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumFilterChip
import com.webtoapp.ui.components.ApkExportPreflightPanel
import com.webtoapp.ui.design.WtaActionBar
import com.webtoapp.ui.design.WtaCapabilityLevel
import com.webtoapp.ui.design.WtaDangerRow
import com.webtoapp.ui.design.WtaEmptyState
import com.webtoapp.ui.design.WtaRadius
import com.webtoapp.ui.design.WtaRowTone
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSection
import com.webtoapp.ui.design.WtaSectionDivider
import com.webtoapp.ui.design.WtaSectionHeaderStyle
import com.webtoapp.ui.design.WtaChoiceRow
import com.webtoapp.ui.design.WtaSettingCard
import com.webtoapp.ui.design.WtaSettingRow
import com.webtoapp.ui.design.WtaSize
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone
import com.webtoapp.ui.design.WtaTextFieldRow
import com.webtoapp.ui.design.WtaToggleRow
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.screens.ecosystem.AnimatedCounter
import com.webtoapp.ui.screens.ecosystem.Avatar
import com.webtoapp.ui.screens.ecosystem.EcosystemMotion
import com.webtoapp.ui.screens.ecosystem.GlassDivider
import com.webtoapp.ui.screens.ecosystem.LikeBurstEffect
import com.webtoapp.ui.screens.ecosystem.ModuleCard
import com.webtoapp.ui.screens.ecosystem.StaggeredItem
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
private fun ManagementMetricCard(
    accent: Color,
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    WtaSettingCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(WtaSize.IconPlate)
                    .clip(RoundedCornerShape(WtaRadius.IconPlate))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(WtaSize.Icon),
                    tint = accent
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ManagementInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    WtaSettingRow(
        title = label,
        modifier = modifier,
        titleMaxLines = 1,
        trailing = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Composable
private fun ManagementBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        modifier = modifier
            .clip(RoundedCornerShape(WtaRadius.Button))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}

@Composable
private fun ManagementTabChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(WtaRadius.Control))
            .background(
                if (selected) {
                    Brush.linearGradient(gradient)
                } else {
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun ManagementStatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    ManagementBadge(label = label, color = color, modifier = modifier)
}

@Composable
private fun ManagementActionRow(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    WtaSettingCard(onClick = onClick) {
        WtaSettingRow(
            title = title,
            subtitle = subtitle,
            icon = icon,
            trailing = {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EcosystemAppManagementSheet(
    app: AppStoreItem,
    apiClient: CloudApiClient,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    data class MgmtTab(val icon: ImageVector, val label: String, val gradient: List<Color>)
    val mgmtGradients = rememberMgmtGradients()
    val tabs = listOf(
        MgmtTab(Icons.Outlined.Dashboard, Strings.cloudOverview, mgmtGradients.blue),
        MgmtTab(Icons.Outlined.VpnKey, Strings.activationCode, mgmtGradients.purple),
        MgmtTab(Icons.Outlined.Campaign, Strings.publishAnnouncement, mgmtGradients.orange),
        MgmtTab(Icons.Outlined.SystemUpdate, Strings.version, mgmtGradients.green),
        MgmtTab(Icons.Outlined.People, Strings.totalUsers, mgmtGradients.red),
        MgmtTab(Icons.Outlined.ReceiptLong, Strings.reviews, mgmtGradients.purple)
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
            WtaSettingCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(WtaRadius.Card))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!app.icon.isNullOrBlank()) {
                            AsyncImage(
                                model = app.icon,
                                contentDescription = app.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ManagementBadge("v${app.versionName}", MaterialTheme.colorScheme.primary)
                            ManagementBadge("Management Console", MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { idx, tab ->
                    ManagementTabChip(
                        label = tab.label,
                        icon = tab.icon,
                        selected = selectedTab == idx,
                        gradient = tab.gradient,
                        onClick = { selectedTab = idx }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when (selectedTab) {
                0 -> ManagementOverviewTab(app, apiClient, scope, tabs.first().gradient, onAppUpdated = { }, onAppDeleted = { })
                1 -> ManagementActivationTab(app, apiClient, scope, tabs[1].gradient)
                2 -> ManagementAnnouncementTab(app, apiClient, scope, tabs[2].gradient)
                3 -> ManagementUpdateTab(app, apiClient, scope, tabs[3].gradient)
                4 -> ManagementUsersTab(app, apiClient, scope)
                5 -> ManagementReviewTab(app, apiClient, scope)
            }
        }
    }
}

@Composable
internal fun ManagementReviewTab(app: AppStoreItem, apiClient: CloudApiClient, scope: kotlinx.coroutines.CoroutineScope) {
    var submissions by remember { mutableStateOf<List<ReviewSubmissionInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            errorMsg = null
            when (val result = apiClient.getAppReviewSubmissions(app.id)) {
                is com.webtoapp.core.auth.AuthResult.Success -> submissions = result.data
                is com.webtoapp.core.auth.AuthResult.Error -> errorMsg = result.message
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            ManagementActionRow(
                title = "提审历史",
                icon = Icons.Outlined.ReceiptLong,
                subtitle = "查看提审记录与回执",
                onClick = { load() }
            )
        }
        if (isLoading) {
            item { WtaStatusBanner(message = Strings.loading, tone = WtaStatusTone.Info) }
        } else if (errorMsg != null) {
            item { WtaStatusBanner(message = errorMsg ?: Strings.loading, tone = WtaStatusTone.Error, actionLabel = Strings.retry, onAction = { load() }) }
        } else if (submissions.isEmpty()) {
            item { WtaEmptyState(title = "提审历史", message = Strings.noData) }
        } else {
            items(submissions, key = { it.id }) { submission ->
                WtaSettingCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ManagementBadge(submission.status, MaterialTheme.colorScheme.primary)
                            ManagementBadge(submission.reviewMode, MaterialTheme.colorScheme.tertiary)
                        }
                        Text(submission.submitMessage ?: "", style = MaterialTheme.typography.bodyMedium)
                        submission.rejectionReason?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        submission.adminReplyText?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}



@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ManagementOverviewTab(app: AppStoreItem, apiClient: CloudApiClient? = null, scope: kotlinx.coroutines.CoroutineScope? = null, gradient: List<Color>? = null, onAppUpdated: (() -> Unit)? = null, onAppDeleted: (() -> Unit)? = null) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editName by remember(app) { mutableStateOf(app.name) }
    var editDescription by remember(app) { mutableStateOf(app.description ?: "") }
    var editCategory by remember(app) { mutableStateOf(app.category) }
    var isUpdating by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }
    var actionFailureReport by remember { mutableStateOf<SheetFailureReport?>(null) }

    val categories = listOf(
        "tools" to Strings.catTools, "social" to Strings.catSocial, "education" to Strings.catEducation,
        "entertainment" to Strings.catEntertainment, "productivity" to Strings.catProductivity,
        "lifestyle" to Strings.catLifestyle, "business" to Strings.catBusiness,
        "news" to Strings.catNews, "finance" to Strings.catFinance,
        "health" to Strings.catHealth, "other" to Strings.catOther
    )

    if (showEditDialog && apiClient != null && scope != null) {
        AlertDialog(
            onDismissRequest = { if (!isUpdating) showEditDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(WtaRadius.Control))
                            .background(Brush.linearGradient(gradient ?: listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp), tint = Color.White)
                    }
                    Text(Strings.editLabel, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    WtaTextFieldRow(
                        title = Strings.appNameLabel,
                        value = editName,
                        onValueChange = { editName = it },
                        placeholder = Strings.appNamePlaceholder,
                        singleLine = true
                    )
                    WtaTextFieldRow(
                        title = Strings.appDescLabel,
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        placeholder = Strings.appDescPlaceholder,
                        singleLine = false
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Strings.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                        categories.forEach { (key, label) ->
                            FilterChip(
                                selected = editCategory == key,
                                onClick = { editCategory = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                        }
                    }
                    updateError?.let {
                        WtaStatusBanner(
                            message = it,
                            tone = WtaStatusTone.Error
                        )
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        isUpdating = true
                        updateError = null
                        scope.launch {
                            when (val result = apiClient.updateStoreApp(app.id, editName, editDescription, editCategory)) {
                                is com.webtoapp.core.auth.AuthResult.Success -> {
                                    showEditDialog = false
                                    onAppUpdated?.invoke()
                                }
                                is com.webtoapp.core.auth.AuthResult.Error -> {
                                    updateError = result.message
                                }
                            }
                            isUpdating = false
                        }
                    },
                    enabled = !isUpdating && editName.isNotBlank(),
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (isUpdating) Strings.saving else Strings.save)
                }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }, enabled = !isUpdating) { Text(Strings.cancel) } }
        )
    }


    if (showDeleteDialog && apiClient != null && scope != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(WtaRadius.Control))
                            .background(Brush.linearGradient(gradient ?: listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.secondary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.DeleteForever, null, Modifier.size(16.dp), tint = Color.White)
                    }
                    Text(Strings.deleteThisApp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Strings.confirmDeleteAppMessage.format(app.name), style = MaterialTheme.typography.bodyMedium)
                    Text(Strings.deleteAppWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        isDeleting = true
                        scope.launch {
                            when (val result = apiClient.deleteStoreApp(app.id)) {
                                is com.webtoapp.core.auth.AuthResult.Success -> {
                                    showDeleteDialog = false
                                    onAppDeleted?.invoke()
                                }
                                is com.webtoapp.core.auth.AuthResult.Error -> {
                                    actionFailureReport = buildSheetFailureReport(
                                        title = Strings.appDeleteFailed,
                                        stage = Strings.deleteAppStage,
                                        summary = result.message,
                                        contextLines = listOf(
                                            "appId=${app.id}",
                                            "appName=${app.name}",
                                            "versionName=${app.versionName}"
                                        )
                                    )
                                    isDeleting = false
                                }
                            }
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (isDeleting) Strings.deleting else Strings.confirmDelete)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) { Text(Strings.cancel) } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ManagementMetricCard(MaterialTheme.colorScheme.primary, Icons.Outlined.Download, "${app.downloads}", Strings.downloads, Modifier.weight(1f))
                ManagementMetricCard(MaterialTheme.colorScheme.tertiary, Icons.Filled.Star, String.format("%.1f", app.rating), Strings.ecosystemRatingLabel.format(app.rating).substringBefore(" "), Modifier.weight(1f))
                ManagementMetricCard(MaterialTheme.colorScheme.secondary, Icons.Outlined.ThumbUp, "${app.likeCount}", Strings.likes, Modifier.weight(1f))
            }
        }

        item {
            WtaSection(
                title = Strings.appInfo,
                level = WtaCapabilityLevel.Common,
                headerStyle = WtaSectionHeaderStyle.Prominent
            ) {
                WtaSettingCard {
                    ManagementInfoRow(Strings.appId, "#${app.id}")
                    WtaSectionDivider()
                    ManagementInfoRow(Strings.version, "v${app.versionName}")
                    WtaSectionDivider()
                    ManagementInfoRow(Strings.category, app.category)
                    WtaSectionDivider()
                    ManagementInfoRow(Strings.packageName, app.packageName ?: "—")
                    WtaSectionDivider()
                    ManagementInfoRow(Strings.publisher, app.authorName)
                    app.createdAt?.let {
                        WtaSectionDivider()
                        ManagementInfoRow(Strings.publishTime, it.take(10))
                    }
                }
                if (apiClient != null) {
                    ManagementActionRow(
                        title = Strings.editLabel,
                        icon = Icons.Outlined.Edit,
                        subtitle = Strings.appInfo,
                        onClick = { showEditDialog = true }
                    )
                }
            }
        }

        if (apiClient != null && scope != null) {
            item {
                WtaSection(
                    title = Strings.dangerZone,
                    description = Strings.deleteAppDataWarning,
                    level = WtaCapabilityLevel.Advanced,
                    headerStyle = WtaSectionHeaderStyle.Quiet,
                    collapsible = false
                ) {
                    WtaSettingCard {
                        WtaDangerRow(
                            title = Strings.deleteThisApp,
                            subtitle = Strings.confirmDeleteAppMessage.format(app.name),
                            onClick = { showDeleteDialog = true }
                        )
                    }
                }
            }
        }
    }

    actionFailureReport?.let { report ->
        SheetFailureReportDialog(
            report = report,
            onDismiss = { actionFailureReport = null }
        )
    }
}


@Composable
internal fun ManagementActivationTab(app: AppStoreItem, apiClient: CloudApiClient, scope: kotlinx.coroutines.CoroutineScope, gradient: List<Color>) {
    var settings by remember { mutableStateOf<ActivationSettings?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newCodes by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    fun loadSettings() { scope.launch { isLoading = true; errorMsg = null; when (val r = apiClient.getActivationSettings(app.id)) { is com.webtoapp.core.auth.AuthResult.Success -> settings = r.data; is com.webtoapp.core.auth.AuthResult.Error -> errorMsg = r.message }; isLoading = false } }
    LaunchedEffect(Unit) { loadSettings() }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAdding) showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(WtaRadius.Control))
                            .background(Brush.linearGradient(gradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.VpnKey, null, Modifier.size(16.dp), tint = Color.White)
                    }
                    Text(Strings.addActivationCode, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(Strings.selectTemplateQuickGenerate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        listOf(
                            "numeric6" to "\uD83D\uDD22 ${Strings.numericCodeLabel}",
                            "standard" to "\uD83D\uDCCB ${Strings.standardCodeLabel}",
                            "uuid" to "\uD83D\uDD17 ${Strings.uuidCodeLabel}"
                        ).forEach { (id, label) ->
                            PremiumFilterChip(
                                selected = selectedTemplate == id,
                                onClick = {
                                    selectedTemplate = id
                                    newCodes = when (id) {
                                        "numeric6" -> (1..5).map { String.format("%06d", (100000..999999).random()) }
                                        "standard" -> {
                                            val c = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                                            (1..5).map {
                                                (1..3).joinToString("-") { (1..4).map { c.random() }.joinToString("") }
                                            }
                                        }
                                        else -> (1..5).map { java.util.UUID.randomUUID().toString() }
                                    }.joinToString("\n")
                                },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                    WtaTextFieldRow(
                        title = Strings.orCustomPerLine,
                        value = newCodes,
                        onValueChange = { newCodes = it; selectedTemplate = null },
                        placeholder = Strings.enterActivationCode,
                        singleLine = false
                    )
                    WtaStatusBanner(
                        message = Strings.totalCodes.format(newCodes.lines().count { it.isNotBlank() }),
                        tone = WtaStatusTone.Info
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        val cl = newCodes.lines().filter { it.isNotBlank() }.map { it.trim() }
                        if (cl.isNotEmpty()) {
                            isAdding = true
                            scope.launch {
                                apiClient.createActivationCodes(app.id, cl)
                                loadSettings()
                                showAddDialog = false
                                isAdding = false
                                newCodes = ""
                            }
                        }
                    },
                    enabled = !isAdding && newCodes.lines().any { it.isNotBlank() }
                ) {
                    if (isAdding) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (isAdding) Strings.adding else Strings.addLabel)
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }, enabled = !isAdding) { Text(Strings.cancel) } }
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        if (isLoading) {
            item {
                WtaStatusBanner(
                    message = Strings.loadingActivationCodes,
                    tone = WtaStatusTone.Info
                )
            }
        } else if (errorMsg != null) {
            item {
                WtaStatusBanner(
                    message = errorMsg ?: Strings.loadingActivationCodes,
                    tone = WtaStatusTone.Error,
                    actionLabel = Strings.retry,
                    onAction = { loadSettings() }
                )
            }
        }
        else {
            val s = settings ?: return@LazyColumn

            item {
                WtaSection(
                    title = Strings.activationCodeSettings,
                    level = WtaCapabilityLevel.Advanced,
                    headerStyle = WtaSectionHeaderStyle.Quiet
                ) {
                    WtaSettingCard {
                        WtaToggleRow(
                            title = Strings.enableActivationVerification,
                            subtitle = Strings.userNeedActivationCode,
                            checked = s.enabled,
                            onCheckedChange = {
                                scope.launch {
                                    apiClient.updateActivationSettings(app.id, it, s.deviceBindingEnabled, s.maxDevicesPerCode)
                                    loadSettings()
                                }
                            },
                            icon = Icons.Outlined.VpnKey
                        )
                        WtaSectionDivider()
                        WtaToggleRow(
                            title = Strings.enableDeviceBinding,
                            subtitle = Strings.oneCodeOneDevice,
                            checked = s.deviceBindingEnabled,
                            onCheckedChange = {
                                scope.launch {
                                    apiClient.updateActivationSettings(app.id, s.enabled, it, s.maxDevicesPerCode)
                                    loadSettings()
                                }
                            },
                            icon = Icons.Outlined.Devices
                        )
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ManagementMetricCard(MaterialTheme.colorScheme.primary, Icons.Outlined.VpnKey, "${s.totalCodes}", Strings.totalActivationCodesShort, Modifier.weight(1f))
                    ManagementMetricCard(MaterialTheme.colorScheme.secondary, Icons.Outlined.CheckCircle, "${s.usedCodes}", Strings.usedCodes, Modifier.weight(1f))
                    ManagementMetricCard(MaterialTheme.colorScheme.tertiary, Icons.Outlined.Pending, "${s.totalCodes - s.usedCodes}", Strings.unusedCodes, Modifier.weight(1f))
                }
            }

            item {
                ManagementActionRow(
                    title = Strings.addActivationCode,
                    icon = Icons.Outlined.Add,
                    subtitle = Strings.selectTemplateQuickGenerate,
                    onClick = { showAddDialog = true }
                )
            }

            items(s.codes, key = { it.id }) { code ->
                WtaSettingCard {
                    WtaSettingRow(
                        title = code.code,
                        subtitle = code.usedByDeviceId?.let { "📱 ${it.take(8)}…" } ?: Strings.unusedCode,
                        icon = if (code.isUsed) Icons.Outlined.CheckCircle else Icons.Outlined.Pending,
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                ManagementStatusPill(
                                    label = if (code.isUsed) Strings.usedCode else Strings.unusedCode,
                                    color = if (code.isUsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                )
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            apiClient.deleteActivationCode(app.id, code.id)
                                            loadSettings()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Outlined.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
internal fun ManagementAnnouncementTab(app: AppStoreItem, apiClient: CloudApiClient, scope: kotlinx.coroutines.CoroutineScope, gradient: List<Color>) {
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var annoTitle by remember { mutableStateOf("") }
    var annoContent by remember { mutableStateOf("") }
    var annoType by remember { mutableStateOf("info") }
    var annoPinned by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }

    fun load() { scope.launch { isLoading = true; errorMsg = null; when (val r = apiClient.getAnnouncements(app.id)) { is com.webtoapp.core.auth.AuthResult.Success -> announcements = r.data; is com.webtoapp.core.auth.AuthResult.Error -> errorMsg = r.message }; isLoading = false } }
    LaunchedEffect(Unit) { load() }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCreating) showCreateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(WtaRadius.Control))
                            .background(Brush.linearGradient(gradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Campaign, null, Modifier.size(16.dp), tint = Color.White)
                    }
                    Text(Strings.publishAnnouncement, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    WtaSettingCard {
                        Text(Strings.selectTemplate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            listOf("info" to Strings.infoNotice, "warning" to Strings.warningNotice, "event" to Strings.eventNotice).forEach { (type, label) ->
                                PremiumFilterChip(
                                    selected = annoType == type,
                                    onClick = { annoType = type },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                        WtaSectionDivider()
                        WtaTextFieldRow(
                            title = Strings.titleLabel,
                            value = annoTitle,
                            onValueChange = { annoTitle = it; selectedTemplateId = null },
                            placeholder = Strings.titleLabel,
                            singleLine = true
                        )
                        WtaSectionDivider()
                        WtaTextFieldRow(
                            title = Strings.contentLabel,
                            value = annoContent,
                            onValueChange = { annoContent = it; selectedTemplateId = null },
                            placeholder = Strings.contentLabel,
                            singleLine = false
                        )
                        WtaSectionDivider()
                        WtaToggleRow(
                            title = Strings.pinnedLabel,
                            checked = annoPinned,
                            onCheckedChange = { annoPinned = it },
                            icon = Icons.Outlined.PushPin
                        )
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        isCreating = true
                        scope.launch {
                            apiClient.createAnnouncement(app.id, annoTitle, annoContent, annoType, annoPinned)
                            load()
                            showCreateDialog = false
                            isCreating = false
                            annoTitle = ""
                            annoContent = ""
                            selectedTemplateId = null
                        }
                    },
                    enabled = !isCreating && annoTitle.isNotBlank() && annoContent.isNotBlank()
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (isCreating) Strings.publishing else Strings.publishButton)
                }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }, enabled = !isCreating) { Text(Strings.cancel) } }
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            ManagementActionRow(
                title = Strings.publishNewAnnouncement,
                icon = Icons.Outlined.Campaign,
                subtitle = Strings.selectTemplate,
                onClick = { showCreateDialog = true }
            )
        }
        if (isLoading) {
            item { WtaStatusBanner(message = Strings.loadingAnnouncements, tone = WtaStatusTone.Info) }
        } else if (errorMsg != null) {
            item {
                WtaStatusBanner(
                    message = errorMsg ?: Strings.loadingAnnouncements,
                    tone = WtaStatusTone.Error,
                    actionLabel = Strings.retry,
                    onAction = { load() }
                )
            }
        }
        else if (announcements.isEmpty()) {
            item {
                WtaEmptyState(
                    title = Strings.noAnnouncements,
                    message = Strings.publishNewAnnouncement
                )
            }
        } else {
            items(announcements, key = { it.id }) { anno ->
                val typeLabel = when (anno.type) { "warning" -> Strings.warningNotice; "event" -> Strings.eventNotice; else -> Strings.infoNotice }
                WtaSettingCard {
                    WtaSettingRow(
                        title = anno.title,
                        subtitle = anno.content,
                        icon = Icons.Outlined.Campaign,
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                ManagementStatusPill(label = typeLabel, color = MaterialTheme.colorScheme.primary)
                                if (anno.isPinned) {
                                    ManagementStatusPill(label = Strings.pinnedBadge, color = MaterialTheme.colorScheme.error)
                                }
                                IconButton(onClick = { scope.launch { apiClient.deleteAnnouncement(app.id, anno.id); load() } }) {
                                    Icon(Icons.Outlined.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
internal fun ManagementUpdateTab(app: AppStoreItem, apiClient: CloudApiClient, scope: kotlinx.coroutines.CoroutineScope, gradient: List<Color>) {
    var config by remember { mutableStateOf<UpdateConfig?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showPushDialog by remember { mutableStateOf(false) }
    var pushVersionName by remember { mutableStateOf("") }; var pushVersionCode by remember { mutableStateOf("") }
    var pushTitle by remember { mutableStateOf("") }; var pushContent by remember { mutableStateOf("") }
    var pushForce by remember { mutableStateOf(false) }
    var useR2 by remember { mutableStateOf(false) }
    var pushTemplateId by remember { mutableStateOf("simple") }; var isPushing by remember { mutableStateOf(false) }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }


    var myApps by remember { mutableStateOf<List<AppStoreItem>>(emptyList()) }
    var selectedSourceApp by remember { mutableStateOf<AppStoreItem?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }
    var isLoadingApps by remember { mutableStateOf(false) }

    fun load() { scope.launch { isLoading = true; errorMsg = null; when (val r = apiClient.getUpdateConfig(app.id)) { is com.webtoapp.core.auth.AuthResult.Success -> config = r.data; is com.webtoapp.core.auth.AuthResult.Error -> errorMsg = r.message }; isLoading = false } }
    LaunchedEffect(Unit) { load() }

    data class UTemplate(val id: String, val name: String, val desc: String, val t: String, val c: String)
    val templates = listOf(
        UTemplate("simple", Strings.templateSimple, Strings.templateSimpleDesc, Strings.templateSimpleTitle, Strings.templateSimpleContent),
        UTemplate("dialog", Strings.templateDialog, Strings.templateDialogDesc, Strings.templateDialogTitle, Strings.templateDialogContent),
        UTemplate("fullscreen", Strings.templateFullscreen, Strings.templateFullscreenDesc, Strings.templateFullscreenTitle, Strings.templateFullscreenContent)
    )


    LaunchedEffect(showPushDialog) {
        if (showPushDialog && myApps.isEmpty()) {
            isLoadingApps = true
            when (val r = apiClient.listMyApps()) {
                is com.webtoapp.core.auth.AuthResult.Success -> {
                    myApps = r.data.apps.filter { it.id != app.id }
                }
                is com.webtoapp.core.auth.AuthResult.Error -> {  }
            }
            isLoadingApps = false
        }
    }

    if (showPushDialog) {
        AlertDialog(
            onDismissRequest = { if (!isPushing) showPushDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(WtaRadius.Control))
                            .background(Brush.linearGradient(gradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.SystemUpdate, null, Modifier.size(16.dp), tint = Color.White)
                    }
                    Text(Strings.pushUpdate, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(Strings.updateTemplate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        templates.forEach { t ->
                            PremiumFilterChip(
                                selected = selectedTemplateId == t.id,
                                onClick = {
                                    selectedTemplateId = t.id
                                    pushTemplateId = t.id
                                    pushTitle = t.t
                                    pushContent = t.c
                                },
                                label = {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(t.name, fontSize = 11.sp, fontWeight = if (selectedTemplateId == t.id) FontWeight.Bold else FontWeight.Normal)
                                        Text(t.desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    }
                                }
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WtaTextFieldRow(
                            title = Strings.versionNoLabel,
                            value = pushVersionName,
                            onValueChange = { pushVersionName = it },
                            placeholder = "1.2.0",
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        WtaTextFieldRow(
                            title = Strings.versionCodeShortLabel,
                            value = pushVersionCode,
                            onValueChange = { pushVersionCode = it },
                            placeholder = "2",
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    WtaTextFieldRow(
                        title = Strings.updateTitleLabel,
                        value = pushTitle,
                        onValueChange = { pushTitle = it; selectedTemplateId = null },
                        placeholder = Strings.updateTitleLabel,
                        singleLine = true
                    )
                    WtaTextFieldRow(
                        title = Strings.updateContentLabel,
                        value = pushContent,
                        onValueChange = { pushContent = it; selectedTemplateId = null },
                        placeholder = Strings.updateContentLabel,
                        singleLine = false
                    )
                    WtaSectionDivider()
                    WtaSettingRow(
                        title = Strings.linkUpdateApp,
                        subtitle = selectedSourceApp?.name ?: Strings.selectPublishedAppAsSource,
                        icon = Icons.Outlined.Apps,
                        onClick = { showAppPicker = !showAppPicker }
                    )
                    AnimatedVisibility(visible = showAppPicker) {
                        WtaSettingCard {
                            Column(
                                Modifier.fillMaxWidth().padding(6.dp).heightIn(max = 180.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isLoadingApps) {
                                    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                    }
                                } else if (myApps.isEmpty()) {
                                    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                                        Text(Strings.noOtherPublishedApps, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                    }
                                } else {
                                    myApps.forEach { item ->
                                        val isSelected = selectedSourceApp?.id == item.id
                                        WtaSettingRow(
                                            title = item.name,
                                            subtitle = "v${item.versionName} · " + Strings.ecosystemDownloadsCount.format(item.downloads),
                                            icon = if (item.icon != null) Icons.Outlined.Apps else Icons.Outlined.Android,
                                            onClick = {
                                                selectedSourceApp = item
                                                showAppPicker = false
                                                if (pushVersionName.isBlank()) pushVersionName = item.versionName
                                            },
                                            trailing = {
                                                if (isSelected) {
                                                    Icon(Icons.Filled.RadioButtonChecked, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    WtaToggleRow(
                        title = if (pushForce) Strings.forceUpdate else Strings.optionalUpdate,
                        subtitle = if (pushForce) Strings.forceUpdateDesc else Strings.optionalUpdateDesc,
                        checked = pushForce,
                        onCheckedChange = { pushForce = it },
                        icon = Icons.Outlined.PriorityHigh
                    )
                    WtaToggleRow(
                        title = if (useR2) Strings.r2CdnAccelerate else Strings.r2CloudStorage,
                        subtitle = if (useR2) Strings.r2CdnDesc else Strings.r2StorageDesc,
                        checked = useR2,
                        onCheckedChange = { useR2 = it },
                        icon = Icons.Outlined.Cloud
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        isPushing = true
                        scope.launch {
                            apiClient.pushUpdate(app.id, pushVersionName, pushVersionCode.toIntOrNull() ?: 1, pushTitle, pushContent, selectedSourceApp?.id, pushForce, 0, pushTemplateId)
                            load()
                            showPushDialog = false
                            isPushing = false
                        }
                    },
                    enabled = !isPushing && pushVersionName.isNotBlank() && pushTitle.isNotBlank()
                ) {
                    if (isPushing) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (isPushing) Strings.pushingUpdate else Strings.pushUpdateButton)
                }
            },
            dismissButton = { TextButton(onClick = { showPushDialog = false }, enabled = !isPushing) { Text(Strings.cancel) } }
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            ManagementActionRow(
                title = Strings.pushNewVersion,
                icon = Icons.Outlined.SystemUpdate,
                subtitle = Strings.updateTemplate,
                onClick = { showPushDialog = true }
            )
        }
        if (isLoading) {
            item { WtaStatusBanner(message = Strings.loadingUpdateConfig, tone = WtaStatusTone.Info) }
        } else if (errorMsg != null) {
            item {
                WtaStatusBanner(
                    message = errorMsg ?: Strings.loadingUpdateConfig,
                    tone = WtaStatusTone.Error,
                    actionLabel = Strings.retry,
                    onAction = { load() }
                )
            }
        }
        else {
            val c = config ?: return@LazyColumn
            if (c.isActive) {
                item {
                    WtaSection(
                        title = Strings.currentUpdateConfig,
                        level = WtaCapabilityLevel.Common,
                        headerStyle = WtaSectionHeaderStyle.Prominent
                    ) {
                        WtaSettingCard {
                            ManagementInfoRow(Strings.targetVersion, "v${c.latestVersionName} (${c.latestVersionCode})")
                            WtaSectionDivider()
                            ManagementInfoRow(Strings.updateTitleLabel2, c.updateTitle)
                            WtaSectionDivider()
                            ManagementInfoRow(Strings.templateLabel, c.templateId)
                            WtaSectionDivider()
                            ManagementInfoRow(Strings.sourceApp, c.sourceAppName ?: c.apkUrl?.take(35)?.plus("…") ?: "—")
                            WtaSectionDivider()
                            WtaSettingRow(
                                title = Strings.updateContentLabel,
                                subtitle = c.updateContent,
                                icon = Icons.Outlined.Description
                            )
                        }
                    }
                }
            } else {
                item {
                    WtaEmptyState(
                        title = Strings.noUpdateConfig,
                        message = Strings.pushNewVersion,
                        icon = Icons.Outlined.SystemUpdate
                    )
                }
            }
        }
    }
}


@Composable
internal fun ManagementUsersTab(app: AppStoreItem, apiClient: CloudApiClient, scope: kotlinx.coroutines.CoroutineScope) {
    var users by remember { mutableStateOf<List<AppUser>>(emptyList()) }
    var geoData by remember { mutableStateOf<List<GeoDistribution>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showGeo by remember { mutableStateOf(false) }

    fun load() { scope.launch { isLoading = true; errorMsg = null; when (val r = apiClient.getAppUsers(app.id)) { is com.webtoapp.core.auth.AuthResult.Success -> users = r.data; is com.webtoapp.core.auth.AuthResult.Error -> errorMsg = r.message }; when (val r = apiClient.getUserGeoDistribution(app.id)) { is com.webtoapp.core.auth.AuthResult.Success -> geoData = r.data; is com.webtoapp.core.auth.AuthResult.Error -> {} }; isLoading = false } }
    LaunchedEffect(Unit) { load() }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            ManagementActionRow(
                title = Strings.geoDistribution,
                icon = Icons.Outlined.Public,
                subtitle = Strings.countriesRegions,
                onClick = { showGeo = !showGeo }
            )
        }
        if (isLoading) {
            item { WtaStatusBanner(message = Strings.loadingUsers, tone = WtaStatusTone.Info) }
        }
        else if (errorMsg != null) {
            item {
                WtaStatusBanner(
                    message = errorMsg ?: Strings.loadingUsers,
                    tone = WtaStatusTone.Error,
                    actionLabel = Strings.retry,
                    onAction = { load() }
                )
            }
        }
        else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ManagementMetricCard(MaterialTheme.colorScheme.primary, Icons.Outlined.People, "${users.size}", Strings.totalUsers, Modifier.weight(1f))
                    ManagementMetricCard(MaterialTheme.colorScheme.secondary, Icons.Outlined.FiberManualRecord, "${users.count { it.isActive }}", Strings.activeUsers, Modifier.weight(1f))
                    ManagementMetricCard(MaterialTheme.colorScheme.tertiary, Icons.Outlined.Public, "${geoData.size}", Strings.countriesRegions, Modifier.weight(1f))
                }
            }

            if (showGeo && geoData.isNotEmpty()) {
                items(geoData) { geo ->
                    WtaSettingCard {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(countryFlag(geo.countryCode), fontSize = 20.sp)
                                    Text(geo.country, fontWeight = FontWeight.SemiBold)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${geo.count}", fontWeight = FontWeight.Bold)
                                    Text("${String.format("%.1f", geo.percentage)}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                }
                            }
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(WtaRadius.Button)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                                Box(Modifier.fillMaxWidth(geo.percentage / 100f).fillMaxHeight().clip(RoundedCornerShape(WtaRadius.Button)).background(MaterialTheme.colorScheme.primary))
                            }
                            geo.regions.take(3).forEach { r ->
                                Row(
                                    Modifier.fillMaxWidth().padding(start = 28.dp),
                                    Arrangement.SpaceBetween
                                ) {
                                    Text(r.region, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    Text("${r.count}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }

            item {
                WtaSection(
                    title = Strings.userList,
                    level = WtaCapabilityLevel.Common,
                    headerStyle = WtaSectionHeaderStyle.Quiet
                ) {}
            }
            if (users.isEmpty()) {
                item {
                    WtaEmptyState(
                        title = Strings.noUserData,
                        message = Strings.userList
                    )
                }
            }
            else {
                items(users, key = { it.id }) { user ->
                    WtaSettingCard {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(user.id.take(2).uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${user.id.take(12)}…", style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                    if (user.isActive) Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    user.deviceModel?.let { Text("📱 $it", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                                    user.country?.let { Text("${countryFlag(it)} $it", fontSize = 10.sp) }
                                    user.appVersion?.let { Text("v$it", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
                                }
                            }
                            user.activationCode?.let {
                                ManagementStatusPill(
                                    label = Strings.usedCode,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


internal fun countryFlag(countryCode: String): String {
    if (countryCode.length != 2) return "🌍"
    val first = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
    val second = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}
