package com.webtoapp.ui.screens
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.ui.components.PremiumSwitch
import com.webtoapp.ui.components.PremiumOutlinedButton

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.webtoapp.ui.components.EnhancedElevatedCard
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.webkit.WebViewCompat
import com.webtoapp.core.engine.EngineManager
import com.webtoapp.core.engine.EngineStatus
import com.webtoapp.core.engine.EngineType
import com.webtoapp.core.engine.download.DownloadState
import com.webtoapp.core.engine.download.GeckoEngineDownloader
import com.webtoapp.core.engine.shields.BrowserShields
import com.webtoapp.core.engine.shields.ShieldsConfig
import com.webtoapp.core.engine.shields.ShieldsReferrerPolicy
import com.webtoapp.core.engine.shields.SslErrorPolicy
import com.webtoapp.core.engine.shields.ThirdPartyCookiePolicy
import com.webtoapp.core.i18n.Strings
import com.webtoapp.util.openUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color
import com.webtoapp.ui.design.WtaBadge
import com.webtoapp.ui.design.WtaEmptyState
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSection
import com.webtoapp.ui.design.WtaRadius
import com.webtoapp.ui.design.WtaSettingRow
import com.webtoapp.ui.design.WtaChoiceRow
import com.webtoapp.ui.design.WtaSpacing





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserKernelScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    var webViewInfo by remember { mutableStateOf<WebViewInfo?>(null) }


    var installedBrowsers by remember { mutableStateOf<List<BrowserInfo>>(emptyList()) }


    val engineManager = remember { EngineManager.getInstance(context) }
    val geckoDownloader = remember { GeckoEngineDownloader(context, engineManager.fileManager) }
    val downloadState by geckoDownloader.downloadState.collectAsStateWithLifecycle()
    var geckoStatus by remember { mutableStateOf(engineManager.getEngineStatus(EngineType.GECKOVIEW)) }
    var geckoSize by remember { mutableLongStateOf(engineManager.getEngineSize(EngineType.GECKOVIEW)) }
    var showDeleteDialog by remember { mutableStateOf(false) }


    val shields = remember { BrowserShields.getInstance(context) }
    val shieldsConfig by shields.config.collectAsStateWithLifecycle()
    val sessionStats by shields.stats.sessionStats.collectAsStateWithLifecycle()
    var shieldsExpanded by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            webViewInfo = getWebViewInfo(context)
            installedBrowsers = getInstalledBrowsers(context)
        }
    }


    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.Completed) {
            geckoStatus = engineManager.getEngineStatus(EngineType.GECKOVIEW)
            geckoSize = engineManager.getEngineSize(EngineType.GECKOVIEW)
        }
    }

    WtaScreen(
        title = Strings.browserKernelTitle,
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = WtaSpacing.ScreenHorizontal,
                vertical = WtaSpacing.ScreenVertical
            ),
            verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)
        ) {

            item {
                WtaSection(
                    title = Strings.embeddedEngineTitle,
                    description = Strings.embeddedEngineDesc
                ) {
                EngineCard(
                    name = Strings.engineSystemWebView,
                    description = Strings.engineSystemWebViewDesc,
                    icon = Icons.Outlined.WebAsset,
                    statusText = Strings.engineReady,
                    statusColor = MaterialTheme.colorScheme.primary,
                    isDefault = true,
                    actions = {}
                )
                GeckoViewEngineCard(
                    status = geckoStatus,
                    downloadState = downloadState,
                    diskSize = geckoSize,
                    onDownload = {
                        scope.launch {
                            geckoDownloader.download()
                        }
                    },
                    onCancel = { geckoDownloader.cancelDownload() },
                    onDelete = { showDeleteDialog = true },
                    onRetry = {
                        geckoDownloader.resetState()
                        scope.launch {
                            geckoDownloader.download()
                        }
                    }
                )
            }
            }


            item {
                WtaSection(
                    title = Strings.shieldsPrivacyProtection,
                    description = Strings.shieldsPrivacySubtitle
                ) {
                ShieldsSettingsCard(
                    config = shieldsConfig,
                    sessionStats = sessionStats,
                    expanded = shieldsExpanded,
                    onExpandToggle = { shieldsExpanded = !shieldsExpanded },
                    onToggleEnabled = { shields.setEnabled(it) },
                    onToggleHttpsUpgrade = { shields.setHttpsUpgrade(it) },
                    onToggleTrackerBlocking = { shields.setTrackerBlocking(it) },
                    onToggleCookieConsent = { shields.setCookieConsentBlock(it) },
                    onToggleGpc = { shields.setGpcEnabled(it) },
                    onToggleReaderMode = { shields.setReaderMode(it) },
                    onCookiePolicyChange = { shields.setThirdPartyCookiePolicy(it) },
                    onReferrerPolicyChange = { shields.setReferrerPolicy(it) },
                    onSslErrorPolicyChange = { shields.setSslErrorPolicy(it) },
                    trackerRuleCount = shields.trackerBlocker.getRuleCount()
                )
            }
            }


            item {
                WtaSection(
                    title = Strings.currentWebViewInfo
                ) {
                CurrentWebViewCard(
                    webViewInfo = webViewInfo,
                    onOpenDeveloperOptions = {
                        openDeveloperOptions(context)
                    }
                )
            }
            }


            item {
                WtaSection(
                    title = Strings.installedBrowsers,
                    description = Strings.installedBrowsersDesc
                ) {
                    if (installedBrowsers.isEmpty()) {
                        WtaEmptyState(
                            title = Strings.noBrowserInstalled,
                            icon = Icons.Outlined.SearchOff
                        )
                    } else {
                        installedBrowsers.forEach { browser ->
                            InstalledBrowserCard(
                                browser = browser,
                                isCurrentProvider = webViewInfo?.packageName == browser.packageName,
                                onOpen = {
                                    openApp(context, browser.packageName)
                                }
                            )
                        }
                    }
                }
            }


            item {
                WtaSection(
                    title = Strings.recommendedBrowsers,
                    description = Strings.recommendedBrowsersDesc
                ) {
                    getRecommendedBrowsers().forEach { browser ->
                        val isInstalled = installedBrowsers.any { it.packageName == browser.packageName }
                        RecommendedBrowserCard(
                            browser = browser,
                            isInstalled = isInstalled,
                            onDownload = {
                                openPlayStore(context, browser.packageName)
                            },
                            onOpenUrl = {
                                openUrl(context, browser.downloadUrl)
                            }
                        )
                    }
                }
            }


            item {
                WtaSection(
                    title = Strings.howToEnableDeveloperOptions
                ) {
                    HelpCard()
                }
            }


            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }


        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(Strings.engineDeleteBtn) },
                text = { Text(Strings.engineDeleteConfirm) },
                confirmButton = {
                    TextButton(onClick = {
                        engineManager.deleteEngine(EngineType.GECKOVIEW)
                        geckoStatus = engineManager.getEngineStatus(EngineType.GECKOVIEW)
                        geckoSize = 0L
                        geckoDownloader.resetState()
                        showDeleteDialog = false
                    }) {
                        Text(Strings.confirm, color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(Strings.cancel)
                    }
                }
            )
        }
    }
}




@Composable
private fun CurrentWebViewCard(
    webViewInfo: WebViewInfo?,
    onOpenDeveloperOptions: () -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.WebAsset,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    Strings.currentWebViewInfo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (webViewInfo != null) {
                InfoRow(Strings.webViewProvider, webViewInfo.providerName)
                InfoRow(Strings.webViewVersion, webViewInfo.version)
                InfoRow(Strings.webViewPackage, webViewInfo.packageName)
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            PremiumOutlinedButton(
                onClick = onOpenDeveloperOptions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.changeWebViewProvider)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                Strings.changeWebViewProviderDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}




@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}




@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}




@Composable
private fun EngineCard(
    name: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    statusText: String,
    statusColor: androidx.compose.ui.graphics.Color,
    isDefault: Boolean = false,
    actions: @Composable ColumnScope.() -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(WtaRadius.Card))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (isDefault) {
                            Spacer(modifier = Modifier.width(8.dp))
                            WtaBadge(
                                text = Strings.engineDefault,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                WtaBadge(
                    text = statusText,
                    containerColor = statusColor.copy(alpha = 0.12f),
                    contentColor = statusColor
                )
            }

            actions()
        }
    }
}




@Composable
private fun GeckoViewEngineCard(
    status: EngineStatus,
    downloadState: DownloadState,
    diskSize: Long,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit
) {
    val statusText = when (status) {
        is EngineStatus.READY -> Strings.engineReady
        is EngineStatus.DOWNLOADED -> Strings.engineDownloaded
        is EngineStatus.NOT_DOWNLOADED -> Strings.engineNotDownloaded
    }
    val statusColor = when (status) {
        is EngineStatus.READY -> MaterialTheme.colorScheme.primary
        is EngineStatus.DOWNLOADED -> MaterialTheme.colorScheme.tertiary
        is EngineStatus.NOT_DOWNLOADED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    EngineCard(
        name = Strings.engineGeckoView,
        description = Strings.engineGeckoViewDesc,
        icon = Icons.Outlined.LocalFireDepartment,
        statusText = if (downloadState is DownloadState.Downloading) Strings.engineDownloading else statusText,
        statusColor = if (downloadState is DownloadState.Downloading) MaterialTheme.colorScheme.tertiary else statusColor,
        isDefault = false
    ) {
        Spacer(modifier = Modifier.height(12.dp))


        AnimatedVisibility(visible = downloadState is DownloadState.Downloading) {
            val progress = (downloadState as? DownloadState.Downloading)?.progress ?: 0f
            val message = (downloadState as? DownloadState.Downloading)?.message ?: ""
            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onCancel,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(Strings.engineCancelDownload, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }


        AnimatedVisibility(visible = downloadState is DownloadState.Error) {
            val errorMsg = (downloadState as? DownloadState.Error)?.message ?: ""
            EnhancedElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        errorMsg,
                        modifier = Modifier.weight(weight = 1f, fill = true),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(onClick = onRetry) {
                        Text(Strings.engineRetry)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }


        if (status is EngineStatus.DOWNLOADED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${Strings.engineVersionLabel}: ${status.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (diskSize > 0) {
                    Text(
                        "${Strings.engineCurrentSize}: ${formatFileSize(diskSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }


        if (downloadState !is DownloadState.Downloading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (status is EngineStatus.DOWNLOADED) {
                    PremiumOutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.engineDeleteBtn, style = MaterialTheme.typography.labelMedium)
                    }
                } else if (status is EngineStatus.NOT_DOWNLOADED) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${Strings.engineEstimatedSize}: ~${EngineType.GECKOVIEW.estimatedSizeMb} MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        FilledTonalButton(onClick = onDownload) {
                            Icon(Icons.Outlined.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Strings.engineDownloadBtn, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}




private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstalledBrowserCard(
    browser: BrowserInfo,
    isCurrentProvider: Boolean,
    onOpen: () -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

                if (browser.icon != null) {
                Image(
                    bitmap = browser.icon.toBitmap().asImageBitmap(),
                    contentDescription = browser.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(WtaRadius.Card))
                )
                } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(WtaRadius.Card))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Language,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        browser.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (isCurrentProvider) {
                        Spacer(modifier = Modifier.width(8.dp))
                        WtaBadge(
                            text = Strings.currentlyUsing,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    browser.version,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (browser.canBeWebViewProvider) {
                    WtaBadge(
                        text = Strings.canBeWebViewProvider,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}




@Composable
private fun RecommendedBrowserCard(
    browser: RecommendedBrowser,
    isInstalled: Boolean,
    onDownload: () -> Unit,
    onOpenUrl: () -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(WtaRadius.Card))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        browser.icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Text(
                    browser.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    browser.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isInstalled) {
                WtaBadge(
                    text = Strings.installed,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                Row {

                    FilledTonalButton(
                        onClick = onDownload,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Shop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.download, style = MaterialTheme.typography.labelMedium)
                    }


                    if (browser.downloadUrl.isNotEmpty() && !browser.downloadUrl.startsWith("market://")) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onOpenUrl,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Language,
                                contentDescription = Strings.openInBrowser,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}




@Composable
private fun HelpCard() {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    Strings.howToEnableDeveloperOptions,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                Strings.developerOptionsSteps,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                Strings.webViewNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}






@Composable
private fun ShieldsSettingsCard(
    config: ShieldsConfig,
    sessionStats: com.webtoapp.core.engine.shields.SessionStats,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleHttpsUpgrade: (Boolean) -> Unit,
    onToggleTrackerBlocking: (Boolean) -> Unit,
    onToggleCookieConsent: (Boolean) -> Unit,
    onToggleGpc: (Boolean) -> Unit,
    onToggleReaderMode: (Boolean) -> Unit,
    onCookiePolicyChange: (ThirdPartyCookiePolicy) -> Unit,
    onReferrerPolicyChange: (ShieldsReferrerPolicy) -> Unit,
    onSslErrorPolicyChange: (SslErrorPolicy) -> Unit,
    trackerRuleCount: Int
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(WtaRadius.Control))
                        .background(
                            if (config.enabled) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (config.enabled)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        Strings.shieldsMasterSwitch,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (config.enabled) Strings.shieldsEnabledWithRules.replace("%d", trackerRuleCount.toString()) else Strings.shieldsDisabled,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                PremiumSwitch(
                    checked = config.enabled,
                    onCheckedChange = onToggleEnabled
                )
            }


            if (config.enabled && sessionStats.total > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                EnhancedElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ShieldStatItem(Strings.shieldsStatAds, sessionStats.totalAdsBlocked)
                        ShieldStatItem(Strings.shieldsStatTrackers, sessionStats.totalTrackersBlocked)
                        ShieldStatItem("HTTPS↑", sessionStats.totalHttpsUpgrades)
                        ShieldStatItem("Cookie", sessionStats.totalCookieConsentsBlocked)
                    }
                }
            }


            if (config.enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onExpandToggle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (expanded) Strings.shieldsCollapseSettings else Strings.shieldsExpandSettings)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))


                        ShieldToggleRow(
                            title = "HTTPS Everywhere",
                            subtitle = Strings.shieldsHttpsUpgradeDesc,
                            icon = Icons.Outlined.Lock,
                            checked = config.httpsUpgrade,
                            onCheckedChange = onToggleHttpsUpgrade
                        )


                        ShieldPolicySelector(
                            title = Strings.sslErrorPolicyTitle,
                            currentValue = config.sslErrorPolicy.displayName,
                            options = SslErrorPolicy.entries.map { it.displayName },
                            onSelect = { index ->
                                onSslErrorPolicyChange(SslErrorPolicy.entries[index])
                            }
                        )


                        ShieldToggleRow(
                            title = Strings.shieldsTrackerBlocking,
                            subtitle = Strings.shieldsTrackerBlockingDesc,
                            icon = Icons.Outlined.RemoveCircleOutline,
                            checked = config.trackerBlocking,
                            onCheckedChange = onToggleTrackerBlocking
                        )


                        ShieldToggleRow(
                            title = Strings.shieldsCookiePopup,
                            subtitle = Strings.shieldsCookiePopupDesc,
                            icon = Icons.Outlined.DoNotDisturbOn,
                            checked = config.cookieConsentBlock,
                            onCheckedChange = onToggleCookieConsent
                        )


                        ShieldToggleRow(
                            title = "Global Privacy Control",
                            subtitle = Strings.shieldsGpcDesc,
                            icon = Icons.Outlined.PrivacyTip,
                            checked = config.gpcEnabled,
                            onCheckedChange = onToggleGpc
                        )


                        ShieldToggleRow(
                            title = Strings.shieldsReaderMode,
                            subtitle = Strings.shieldsReaderModeDesc,
                            icon = Icons.Outlined.AutoStories,
                            checked = config.readerModeEnabled,
                            onCheckedChange = onToggleReaderMode
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))


                        ShieldPolicySelector(
                            title = Strings.shieldsThirdPartyCookiePolicy,
                            currentValue = config.thirdPartyCookiePolicy.displayName,
                            options = ThirdPartyCookiePolicy.entries.map { it.displayName },
                            onSelect = { index ->
                                onCookiePolicyChange(ThirdPartyCookiePolicy.entries[index])
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))


                        ShieldPolicySelector(
                            title = Strings.shieldsReferrerPolicy,
                            currentValue = config.referrerPolicy.displayName,
                            options = ShieldsReferrerPolicy.entries.map { it.displayName },
                            onSelect = { index ->
                                onReferrerPolicyChange(ShieldsReferrerPolicy.entries[index])
                            }
                        )
                    }
                }
            }
        }
    }
}




@Composable
private fun ShieldToggleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    WtaSettingRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = { onCheckedChange(!checked) }
    ) {
        PremiumSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}




@Composable
private fun ShieldPolicySelector(
    title: String,
    currentValue: String,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        WtaChoiceRow(
            title = title,
            value = currentValue,
            icon = Icons.Default.ExpandMore,
            onClick = { showMenu = true }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            fontWeight = if (option == currentValue) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(index)
                        showMenu = false
                    },
                    leadingIcon = if (option == currentValue) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
        }
    }
}




@Composable
private fun ShieldStatItem(label: String, count: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}






data class WebViewInfo(
    val providerName: String,
    val version: String,
    val packageName: String
)




data class BrowserInfo(
    val name: String,
    val packageName: String,
    val version: String,
    val icon: android.graphics.drawable.Drawable?,
    val canBeWebViewProvider: Boolean
)




data class RecommendedBrowser(
    val name: String,
    val packageName: String,
    val description: String,
    val downloadUrl: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)







private fun getRecommendedBrowsers(): List<RecommendedBrowser> = listOf(
    RecommendedBrowser(
        name = "Google Chrome",
        packageName = "com.android.chrome",
        description = Strings.browserChromeDesc,
        downloadUrl = "market://details?id=com.android.chrome",
        icon = Icons.Outlined.Language
    ),
    RecommendedBrowser(
        name = "Microsoft Edge",
        packageName = "com.microsoft.emmx",
        description = Strings.browserEdgeDesc,
        downloadUrl = "market://details?id=com.microsoft.emmx",
        icon = Icons.Outlined.Explore
    ),
    RecommendedBrowser(
        name = "Mozilla Firefox",
        packageName = "org.mozilla.firefox",
        description = Strings.browserFirefoxDesc,
        downloadUrl = "market://details?id=org.mozilla.firefox",
        icon = Icons.Outlined.LocalFireDepartment
    ),
    RecommendedBrowser(
        name = "Brave",
        packageName = "com.brave.browser",
        description = Strings.browserBraveDesc,
        downloadUrl = "market://details?id=com.brave.browser",
        icon = Icons.Outlined.Shield
    ),
    RecommendedBrowser(
        name = "Via Browser",
        packageName = "mark.via.gp",
        description = Strings.browserViaDesc,
        downloadUrl = "market://details?id=mark.via.gp",
        icon = Icons.Outlined.Speed
    )
)






private fun getWebViewInfo(context: Context): WebViewInfo {
    return try {
        val webViewPackage = WebViewCompat.getCurrentWebViewPackage(context)
        if (webViewPackage != null) {
            WebViewInfo(
                providerName = webViewPackage.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: webViewPackage.packageName,
                version = webViewPackage.versionName ?: "Unknown",
                packageName = webViewPackage.packageName
            )
        } else {
            getDefaultWebViewInfo()
        }
    } catch (e: Exception) {
        getDefaultWebViewInfo()
    }
}

private fun getDefaultWebViewInfo(): WebViewInfo {
    return WebViewInfo(
        providerName = "Android System WebView",
        version = "Unknown",
        packageName = "com.google.android.webview"
    )
}




private fun getInstalledBrowsers(context: Context): List<BrowserInfo> {
    val pm = context.packageManager
    val browsers = mutableListOf<BrowserInfo>()


    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
    val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    }


    val webViewProviderPackages = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "com.google.android.webview",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.opera.browser",
        "com.opera.mini.native"
    )

    for (resolveInfo in resolveInfoList) {
        val packageName = resolveInfo.activityInfo.packageName


        if (packageName == context.packageName ||
            packageName == "android" ||
            packageName.contains("resolver") ||
            packageName.contains("chooser")) {
            continue
        }

        try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }

            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }

            browsers.add(
                BrowserInfo(
                    name = appInfo.loadLabel(pm).toString(),
                    packageName = packageName,
                    version = packageInfo.versionName ?: "Unknown",
                    icon = appInfo.loadIcon(pm),
                    canBeWebViewProvider = webViewProviderPackages.contains(packageName)
                )
            )
        } catch (e: Exception) {

        }
    }


    return browsers.sortedWith(
        compareByDescending<BrowserInfo> { it.canBeWebViewProvider }
            .thenBy { it.name }
    )
}




private fun openDeveloperOptions(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        context.startActivity(intent)
    } catch (e: Exception) {

        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        } catch (e2: Exception) {

        }
    }
}




private fun openApp(context: Context, packageName: String) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent)
        }
    } catch (e: Exception) {

    }
}




private fun openPlayStore(context: Context, packageName: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        context.startActivity(intent)
    } catch (e: Exception) {

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            context.startActivity(intent)
        } catch (e2: Exception) {

        }
    }
}




private fun openUrl(context: Context, url: String) {
    try {
        context.openUrl(url)
    } catch (e: Exception) {

    }
}
