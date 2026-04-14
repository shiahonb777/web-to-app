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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.webtoapp.ui.components.EnhancedElevatedCard
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
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
import com.webtoapp.ui.components.ThemedBackgroundBox
import androidx.compose.ui.graphics.Color
import org.koin.compose.koinInject

/**
 * settings
 * displaycurrent WebView, list, download
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserKernelScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // WebView
    var webViewInfo by remember { mutableStateOf<WebViewInfo?>(null) }
    
    // Note
    var installedBrowsers by remember { mutableStateOf<List<BrowserInfo>>(emptyList()) }
    
    // management
    val engineManager: EngineManager = koinInject()
    val geckoDownloader = remember { GeckoEngineDownloader(context, engineManager.fileManager) }
    val downloadState by geckoDownloader.downloadState.collectAsStateWithLifecycle()
    var geckoStatus by remember { mutableStateOf(engineManager.getEngineStatus(EngineType.GECKOVIEW)) }
    var geckoSize by remember { mutableLongStateOf(engineManager.getEngineSize(EngineType.GECKOVIEW)) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Shields
    val shields: BrowserShields = koinInject()
    val shieldsConfig by shields.config.collectAsStateWithLifecycle()
    val sessionStats by shields.stats.sessionStats.collectAsStateWithLifecycle()
    var shieldsExpanded by remember { mutableStateOf(false) }
    
    // Load
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            webViewInfo = getWebViewInfo(context)
            installedBrowsers = getInstalledBrowsers(context)
        }
    }
    
    // download refreshstate
    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.Completed) {
            geckoStatus = engineManager.getEngineStatus(EngineType.GECKOVIEW)
            geckoSize = engineManager.getEngineSize(EngineType.GECKOVIEW)
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(Strings.browserKernelTitle, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text(
                            Strings.browserKernelSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== area =====
            item {
                SectionHeader(
                    title = Strings.embeddedEngineTitle,
                    subtitle = Strings.embeddedEngineDesc
                )
            }
            
            // System WebView card( default, always)
            item {
                EngineCard(
                    name = Strings.engineSystemWebView,
                    description = Strings.engineSystemWebViewDesc,
                    icon = Icons.Outlined.WebAsset,
                    statusText = Strings.engineReady,
                    statusColor = MaterialTheme.colorScheme.primary,
                    isDefault = true,
                    actions = {}
                )
            }
            
            // GeckoView card
            item {
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
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            // ===== Shields settings =====
            item {
                SectionHeader(
                    title = Strings.shieldsPrivacyProtection,
                    subtitle = Strings.shieldsPrivacySubtitle
                )
            }
            
            item {
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
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            // ===== current WebView card =====
            item {
                CurrentWebViewCard(
                    webViewInfo = webViewInfo,
                    onOpenDeveloperOptions = {
                        openDeveloperOptions(context)
                    }
                )
            }
            
            // Note
            item {
                SectionHeader(
                    title = Strings.installedBrowsers,
                    subtitle = Strings.installedBrowsersDesc
                )
            }
            
            if (installedBrowsers.isEmpty()) {
                item {
                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    Strings.noBrowserInstalled,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(installedBrowsers) { browser ->
                    InstalledBrowserCard(
                        browser = browser,
                        isCurrentProvider = webViewInfo?.packageName == browser.packageName,
                        onOpen = {
                            openApp(context, browser.packageName)
                        }
                    )
                }
            }
            
            // download
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = Strings.recommendedBrowsers,
                    subtitle = Strings.recommendedBrowsersDesc
                )
            }
            
            items(getRecommendedBrowsers()) { browser ->
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
            
            // help
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HelpCard()
            }
            
            // bottom
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // delete dialog
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

/**
 * current WebView card
 */
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

/**
 * Note
 */
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

/**
 * area
 */
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

/**
 * card
 */
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
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
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
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    Strings.engineDefault,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor
                    )
                }
            }
            
            actions()
        }
    }
}

/**
 * GeckoView card( download/delete/)
 */
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
        
        // download
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
        
        // errorstate
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
        
        // download
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
        
        // button
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

/**
 * file
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}

/**
 * card
 */
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
            // icon
            if (browser.icon != null) {
                Image(
                    bitmap = browser.icon.toBitmap().asImageBitmap(),
                    contentDescription = browser.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
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
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                Strings.currentlyUsing,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    browser.version,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (browser.canBeWebViewProvider) {
                    Text(
                        Strings.canBeWebViewProvider,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
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

/**
 * card
 */
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
            // icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = browser.brandColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        browser.icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.surface
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
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        Strings.installed,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            } else {
                Row {
                    // Play Store downloadbutton
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
                    
                    // Web pagedownloadbutton( if download)
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

/**
 * helpcard
 */
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

// ==================== Shields settingscard ====================

/**
 * Shields settingscard
 */
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
            // header: icon + +
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (config.enabled) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
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
            
            // session
            if (config.enabled && sessionStats.total > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
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
            
            // expand/ settings
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
                        
                        // HTTPS
                        ShieldToggleRow(
                            title = "HTTPS Everywhere",
                            subtitle = Strings.shieldsHttpsUpgradeDesc,
                            icon = Icons.Outlined.Lock,
                            checked = config.httpsUpgrade,
                            onCheckedChange = onToggleHttpsUpgrade
                        )

                        // SSL errorhandle
                        ShieldPolicySelector(
                            title = Strings.sslErrorPolicyTitle,
                            currentValue = config.sslErrorPolicy.displayName,
                            options = SslErrorPolicy.entries.map { it.displayName },
                            onSelect = { index ->
                                onSslErrorPolicyChange(SslErrorPolicy.entries[index])
                            }
                        )

                        // intercept
                        ShieldToggleRow(
                            title = Strings.shieldsTrackerBlocking,
                            subtitle = Strings.shieldsTrackerBlockingDesc,
                            icon = Icons.Outlined.RemoveCircleOutline,
                            checked = config.trackerBlocking,
                            onCheckedChange = onToggleTrackerBlocking
                        )
                        
                        // Cookie dialog close
                        ShieldToggleRow(
                            title = Strings.shieldsCookiePopup,
                            subtitle = Strings.shieldsCookiePopupDesc,
                            icon = Icons.Outlined.DoNotDisturbOn,
                            checked = config.cookieConsentBlock,
                            onCheckedChange = onToggleCookieConsent
                        )
                        
                        // GPC
                        ShieldToggleRow(
                            title = "Global Privacy Control",
                            subtitle = Strings.shieldsGpcDesc,
                            icon = Icons.Outlined.PrivacyTip,
                            checked = config.gpcEnabled,
                            onCheckedChange = onToggleGpc
                        )
                        
                        // mode
                        ShieldToggleRow(
                            title = Strings.shieldsReaderMode,
                            subtitle = Strings.shieldsReaderModeDesc,
                            icon = Icons.Outlined.AutoStories,
                            checked = config.readerModeEnabled,
                            onCheckedChange = onToggleReaderMode
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // Cookie
                        ShieldPolicySelector(
                            title = Strings.shieldsThirdPartyCookiePolicy,
                            currentValue = config.thirdPartyCookiePolicy.displayName,
                            options = ThirdPartyCookiePolicy.entries.map { it.displayName },
                            onSelect = { index ->
                                onCookiePolicyChange(ThirdPartyCookiePolicy.entries[index])
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Referrer
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

/**
 * Shield
 */
@Composable
private fun ShieldToggleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        PremiumSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * Shield select
 */
@Composable
private fun ShieldPolicySelector(
    title: String,
    currentValue: String,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(weight = 1f, fill = true)
        )
        
        Box {
            PremiumOutlinedButton(
                onClick = { showMenu = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    currentValue,
                    style = MaterialTheme.typography.labelSmall
                )
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            
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
}

/**
 * Shield
 */
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

// Note

/**
 * WebView
 */
data class WebViewInfo(
    val providerName: String,
    val version: String,
    val packageName: String
)

/**
 * Note
 */
data class BrowserInfo(
    val name: String,
    val packageName: String,
    val version: String,
    val icon: android.graphics.drawable.Drawable?,
    val canBeWebViewProvider: Boolean
)

/**
 * Note
 */
data class RecommendedBrowser(
    val name: String,
    val packageName: String,
    val description: String,
    val downloadUrl: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val brandColor: androidx.compose.ui.graphics.Color
)

// ==================== list ====================

/**
 * list
 * run
 */
private fun getRecommendedBrowsers(): List<RecommendedBrowser> = listOf(
    RecommendedBrowser(
        name = "Google Chrome",
        packageName = "com.android.chrome",
        description = Strings.browserChromeDesc,
        downloadUrl = "market://details?id=com.android.chrome",
        icon = Icons.Outlined.Language,
        brandColor = androidx.compose.ui.graphics.Color(0xFF4285F4)
    ),
    RecommendedBrowser(
        name = "Microsoft Edge",
        packageName = "com.microsoft.emmx",
        description = Strings.browserEdgeDesc,
        downloadUrl = "market://details?id=com.microsoft.emmx",
        icon = Icons.Outlined.Explore,
        brandColor = androidx.compose.ui.graphics.Color(0xFF0078D4)
    ),
    RecommendedBrowser(
        name = "Mozilla Firefox",
        packageName = "org.mozilla.firefox",
        description = Strings.browserFirefoxDesc,
        downloadUrl = "market://details?id=org.mozilla.firefox",
        icon = Icons.Outlined.LocalFireDepartment,
        brandColor = androidx.compose.ui.graphics.Color(0xFFFF7139)
    ),
    RecommendedBrowser(
        name = "Brave",
        packageName = "com.brave.browser",
        description = Strings.browserBraveDesc,
        downloadUrl = "market://details?id=com.brave.browser",
        icon = Icons.Outlined.Shield,
        brandColor = androidx.compose.ui.graphics.Color(0xFFFB542B)
    ),
    RecommendedBrowser(
        name = "Via Browser",
        packageName = "mark.via.gp",
        description = Strings.browserViaDesc,
        downloadUrl = "market://details?id=mark.via.gp",
        icon = Icons.Outlined.Speed,
        brandColor = androidx.compose.ui.graphics.Color(0xFF5C6BC0)
    )
)

// Note

/**
 * current WebView
 */
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

/**
 * list
 */
private fun getInstalledBrowsers(context: Context): List<BrowserInfo> {
    val pm = context.packageManager
    val browsers = mutableListOf<BrowserInfo>()
    
    // handle HTTP app
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
    val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    }
    
    // WebView list( WebView)
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
        
        // systemappselect
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
            // Note
        }
    }
    
    // , preferdisplay WebView
    return browsers.sortedWith(
        compareByDescending<BrowserInfo> { it.canBeWebViewProvider }
            .thenBy { it.name }
    )
}

/**
 * open
 */
private fun openDeveloperOptions(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        context.startActivity(intent)
    } catch (e: Exception) {
        // if open, opensettings
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        } catch (e2: Exception) {
            // Note
        }
    }
}

/**
 * openapp
 */
private fun openApp(context: Context, packageName: String) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        // Note
    }
}

/**
 * open Play Store
 */
private fun openPlayStore(context: Context, packageName: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        context.startActivity(intent)
    } catch (e: Exception) {
        // if Play Store, open
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            context.startActivity(intent)
        } catch (e2: Exception) {
            // Note
        }
    }
}

/**
 * open URL
 */
private fun openUrl(context: Context, url: String) {
    try {
        context.openUrl(url)
    } catch (e: Exception) {
        // Note
    }
}
