package com.webtoapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Javascript
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.core.download.DependencyDownloadEngine
import com.webtoapp.core.golang.GoDependencyManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.nodejs.NodeDependencyManager
import com.webtoapp.core.python.PythonDependencyManager
import com.webtoapp.core.wordpress.WordPressDependencyManager
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumFilterChip
import com.webtoapp.ui.components.PremiumOutlinedButton
import com.webtoapp.ui.design.WtaBadge
import com.webtoapp.ui.theme.AppColors
import com.webtoapp.ui.design.WtaRadius
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSection
import com.webtoapp.ui.design.WtaSectionDivider
import com.webtoapp.ui.design.WtaSettingCard
import com.webtoapp.ui.design.WtaSettingRow
import com.webtoapp.ui.design.WtaSpacing
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun RuntimeDepsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var phpReady by remember { mutableStateOf(WordPressDependencyManager.isPhpReady(context)) }
    var wpReady by remember { mutableStateOf(WordPressDependencyManager.isWordPressReady(context)) }
    var sqliteReady by remember { mutableStateOf(WordPressDependencyManager.isSqlitePluginReady(context)) }
    var nodeReady by remember { mutableStateOf(NodeDependencyManager.isNodeReady(context)) }
    var pythonReady by remember { mutableStateOf(PythonDependencyManager.isPythonReady(context)) }

    var wpCacheSize by remember { mutableLongStateOf(0L) }
    var nodeCacheSize by remember { mutableLongStateOf(0L) }
    var pythonCacheSize by remember { mutableLongStateOf(0L) }
    var goCacheSize by remember { mutableLongStateOf(0L) }
    var phpCacheSize by remember { mutableLongStateOf(0L) }
    var sqliteCacheSize by remember { mutableLongStateOf(0L) }

    var wpProjectCount by remember { mutableIntStateOf(0) }
    var nodeProjectCount by remember { mutableIntStateOf(0) }
    var pythonProjectCount by remember { mutableIntStateOf(0) }
    var goProjectCount by remember { mutableIntStateOf(0) }
    var docsProjectCount by remember { mutableIntStateOf(0) }

    var wpMirrorRegion by remember { mutableStateOf(WordPressDependencyManager.getMirrorRegion()) }

    var isDownloading by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadLabel by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val engineState by DependencyDownloadEngine.state.collectAsStateWithLifecycle()
    val wpDownloadState by WordPressDependencyManager.downloadState.collectAsStateWithLifecycle()
    val nodeDownloadState by NodeDependencyManager.downloadState.collectAsStateWithLifecycle()
    val pythonDownloadState by PythonDependencyManager.downloadState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            wpCacheSize = WordPressDependencyManager.getCacheSize(context)
            phpCacheSize = wpCacheSize
            sqliteCacheSize = wpCacheSize
            nodeCacheSize = NodeDependencyManager.getCacheSize(context)
            pythonCacheSize = PythonDependencyManager.getCacheSize(context)
            goCacheSize = GoDependencyManager.getCacheSize(context)

            wpProjectCount = countSubdirs(WordPressDependencyManager.getWordPressProjectsDir(context))
            nodeProjectCount = countSubdirs(NodeDependencyManager.getNodeProjectsDir(context))
            pythonProjectCount = countSubdirs(File(context.filesDir, "python_projects"))
            goProjectCount = countSubdirs(File(context.filesDir, "go_projects"))
            docsProjectCount = countSubdirs(File(context.filesDir, "docs_projects"))
        }
    }

    LaunchedEffect(engineState) {
        when (val es = engineState) {
            is DependencyDownloadEngine.State.Downloading -> {
                isDownloading = true
                isPaused = false
                downloadProgress = es.progress
                downloadLabel = es.displayName
            }
            is DependencyDownloadEngine.State.Paused -> {
                isDownloading = true
                isPaused = true
                downloadProgress = es.progress
                downloadLabel = es.displayName
            }
            is DependencyDownloadEngine.State.Extracting -> {
                isDownloading = true
                isPaused = false
                downloadLabel = es.displayName
            }
            is DependencyDownloadEngine.State.Complete -> {
                isDownloading = false
                isPaused = false
                phpReady = WordPressDependencyManager.isPhpReady(context)
                wpReady = WordPressDependencyManager.isWordPressReady(context)
                sqliteReady = WordPressDependencyManager.isSqlitePluginReady(context)
                nodeReady = NodeDependencyManager.isNodeReady(context)
                pythonReady = PythonDependencyManager.isPythonReady(context)
                wpCacheSize = withContext(Dispatchers.IO) { WordPressDependencyManager.getCacheSize(context) }
                phpCacheSize = wpCacheSize
                sqliteCacheSize = wpCacheSize
                nodeCacheSize = withContext(Dispatchers.IO) { NodeDependencyManager.getCacheSize(context) }
                pythonCacheSize = withContext(Dispatchers.IO) { PythonDependencyManager.getCacheSize(context) }
            }
            is DependencyDownloadEngine.State.Error -> {
                isDownloading = false
                isPaused = false
                snackbarHostState.showSnackbar(es.message)
            }
            else -> {
                isDownloading = false
                isPaused = false
            }
        }
    }

    LaunchedEffect(wpDownloadState) {
        when (wpDownloadState) {
            is WordPressDependencyManager.DownloadState.Complete -> {
                wpCacheSize = withContext(Dispatchers.IO) { WordPressDependencyManager.getCacheSize(context) }
                phpCacheSize = wpCacheSize
                sqliteCacheSize = wpCacheSize
            }
            is WordPressDependencyManager.DownloadState.Error -> {
                snackbarHostState.showSnackbar((wpDownloadState as WordPressDependencyManager.DownloadState.Error).message)
            }
            else -> Unit
        }
    }

    LaunchedEffect(nodeDownloadState) {
        when (nodeDownloadState) {
            is NodeDependencyManager.DownloadState.Complete -> {
                nodeCacheSize = withContext(Dispatchers.IO) { NodeDependencyManager.getCacheSize(context) }
            }
            is NodeDependencyManager.DownloadState.Error -> {
                snackbarHostState.showSnackbar((nodeDownloadState as NodeDependencyManager.DownloadState.Error).message)
            }
            else -> Unit
        }
    }

    LaunchedEffect(pythonDownloadState) {
        when (pythonDownloadState) {
            is PythonDependencyManager.DownloadState.Complete -> {
                pythonCacheSize = withContext(Dispatchers.IO) { PythonDependencyManager.getCacheSize(context) }
                pythonReady = PythonDependencyManager.isPythonReady(context)
            }
            is PythonDependencyManager.DownloadState.Error -> {
                snackbarHostState.showSnackbar((pythonDownloadState as PythonDependencyManager.DownloadState.Error).message)
            }
            else -> Unit
        }
    }

    val totalCacheSize = wpCacheSize + nodeCacheSize + pythonCacheSize + goCacheSize
    val allRuntimesReady = phpReady && wpReady && sqliteReady && nodeReady && pythonReady
    val readyCount = listOf(phpReady, wpReady, sqliteReady, nodeReady, pythonReady).count { it }

    val installWpDeps: () -> Unit = {
        scope.launch {
            isDownloading = true
            val success = WordPressDependencyManager.downloadAllDependencies(context)
            isDownloading = false
            if (success) {
                phpReady = WordPressDependencyManager.isPhpReady(context)
                wpReady = WordPressDependencyManager.isWordPressReady(context)
                sqliteReady = WordPressDependencyManager.isSqlitePluginReady(context)
                wpCacheSize = withContext(Dispatchers.IO) { WordPressDependencyManager.getCacheSize(context) }
                phpCacheSize = wpCacheSize
                sqliteCacheSize = wpCacheSize
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Filled.DeleteSweep, null) },
            title = { Text(Strings.depClearAll) },
            text = { Text(Strings.depClearConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                WordPressDependencyManager.clearCache(context)
                                NodeDependencyManager.clearCache(context)
                                PythonDependencyManager.clearCache(context)
                                GoDependencyManager.clearCache(context)
                            }
                            wpCacheSize = 0L
                            phpCacheSize = 0L
                            sqliteCacheSize = 0L
                            nodeCacheSize = 0L
                            pythonCacheSize = 0L
                            goCacheSize = 0L
                            phpReady = false
                            wpReady = false
                            sqliteReady = false
                            nodeReady = false
                            pythonReady = false
                            snackbarHostState.showSnackbar(Strings.depClearDone)
                        }
                    }
                ) {
                    Text(Strings.btnConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }

    WtaScreen(
        title = Strings.runtimeDepsTitle,
        snackbarHostState = snackbarHostState,
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = WtaSpacing.ScreenHorizontal,
                    vertical = WtaSpacing.ScreenVertical
                ),
            verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)
        ) {
            StatusOverviewCard(
                readyCount = readyCount,
                totalCount = 5,
                allReady = allRuntimesReady,
                totalCacheSize = totalCacheSize,
                isDownloading = isDownloading,
                isPaused = isPaused,
                downloadProgress = downloadProgress,
                downloadLabel = downloadLabel,
                engineState = engineState,
                onPause = { DependencyDownloadEngine.pause() },
                onResume = { DependencyDownloadEngine.resume() }
            )

            WtaSection(title = Strings.depSectionRuntimes) {
                WtaSettingCard {
                        RuntimeItemRow(
                            icon = Icons.Outlined.Code,
                            iconColor = AppColors.Php,
                        title = Strings.depPhpRuntime,
                        description = Strings.depPhpDesc,
                        isReady = phpReady,
                        onInstall = installWpDeps
                    )
                    WtaSectionDivider()
                        RuntimeItemRow(
                            icon = Icons.Outlined.Language,
                            iconColor = AppColors.WordPress,
                        title = Strings.depWpCore,
                        description = Strings.depWpCoreDesc,
                        isReady = wpReady,
                        onInstall = installWpDeps
                    )
                    WtaSectionDivider()
                        RuntimeItemRow(
                            icon = Icons.Outlined.Javascript,
                            iconColor = AppColors.NodeJs,
                        title = Strings.depNodeRuntime,
                        description = Strings.depNodeDesc,
                        isReady = nodeReady,
                        onInstall = {
                            scope.launch {
                                isDownloading = true
                                val success = NodeDependencyManager.downloadNodeRuntime(context)
                                isDownloading = false
                                if (success) {
                                    nodeReady = NodeDependencyManager.isNodeReady(context)
                                    nodeCacheSize = withContext(Dispatchers.IO) { NodeDependencyManager.getCacheSize(context) }
                                }
                            }
                        }
                    )
                    WtaSectionDivider()
                        RuntimeItemRow(
                            icon = Icons.Outlined.Terminal,
                            iconColor = AppColors.Python,
                        title = Strings.depPythonRuntime,
                        description = Strings.depPythonDesc,
                        isReady = pythonReady,
                        onInstall = {
                            scope.launch {
                                isDownloading = true
                                val success = PythonDependencyManager.downloadPythonRuntime(context)
                                isDownloading = false
                                if (success) {
                                    pythonReady = PythonDependencyManager.isPythonReady(context)
                                    pythonCacheSize = withContext(Dispatchers.IO) { PythonDependencyManager.getCacheSize(context) }
                                }
                            }
                        }
                    )
                    WtaSectionDivider()
                        RuntimeItemRow(
                            icon = Icons.Outlined.RocketLaunch,
                            iconColor = AppColors.Go,
                        title = Strings.depGoInfo,
                        description = Strings.depGoDesc,
                        isReady = true,
                        onInstall = null
                    )
                }
            }

            WtaSection(title = Strings.depSectionRuntimePlugins) {
                WtaSettingCard {
                        RuntimeItemRow(
                            icon = Icons.Outlined.Storage,
                            iconColor = AppColors.SQLite,
                        title = Strings.depSqlitePlugin,
                        description = Strings.depSqliteDesc,
                        isReady = sqliteReady,
                        onInstall = installWpDeps
                    )
                }
            }

            WtaSection(title = Strings.depSectionProjects) {
                WtaSettingCard {
                    val items = listOf(
                        ProjectEntry(Strings.depWpProjects, wpProjectCount, AppColors.WordPress),
                        ProjectEntry(Strings.depNodeProjects, nodeProjectCount, AppColors.NodeJs),
                        ProjectEntry(Strings.depPythonProjects, pythonProjectCount, AppColors.Python),
                        ProjectEntry(Strings.depGoProjects, goProjectCount, AppColors.Go),
                        ProjectEntry(Strings.depDocsProjects, docsProjectCount, Color(0xFFE97627))
                    )
                    items.forEachIndexed { index, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(entry.color)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = entry.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = Strings.depProjectCount(entry.count),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (entry.count > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                fontWeight = if (entry.count > 0) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                        if (index < items.lastIndex) {
                            WtaSectionDivider()
                        }
                    }
                }
            }

            WtaSection(title = Strings.depSectionDownload) {
                WtaSettingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = Strings.depMirrorSource,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Strings.depMirrorDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val currentRegion = wpMirrorRegion
                            PremiumFilterChip(
                                selected = currentRegion == WordPressDependencyManager.MirrorRegion.CN,
                                onClick = { onMirrorChange("cn") { wpMirrorRegion = it } },
                                label = { Text(Strings.depMirrorCN) },
                                leadingIcon = if (currentRegion == WordPressDependencyManager.MirrorRegion.CN) {
                                    { Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp)) }
                                } else null
                            )
                            PremiumFilterChip(
                                selected = currentRegion == WordPressDependencyManager.MirrorRegion.GLOBAL,
                                onClick = { onMirrorChange("global") { wpMirrorRegion = it } },
                                label = { Text(Strings.depMirrorGlobal) },
                                leadingIcon = if (currentRegion == WordPressDependencyManager.MirrorRegion.GLOBAL) {
                                    { Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp)) }
                                } else null
                            )
                            PremiumFilterChip(
                                selected = currentRegion != WordPressDependencyManager.MirrorRegion.CN &&
                                    currentRegion != WordPressDependencyManager.MirrorRegion.GLOBAL,
                                onClick = { onMirrorChange("auto") { wpMirrorRegion = it } },
                                label = { Text(Strings.depMirrorAuto) }
                            )
                        }

                        if (!allRuntimesReady) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))

                            PremiumButton(
                                onClick = {
                                    scope.launch {
                                        isDownloading = true
                                        val wpSuccess = WordPressDependencyManager.downloadAllDependencies(context)
                                        if (wpSuccess) {
                                            phpReady = WordPressDependencyManager.isPhpReady(context)
                                            wpReady = WordPressDependencyManager.isWordPressReady(context)
                                            sqliteReady = WordPressDependencyManager.isSqlitePluginReady(context)
                                            wpCacheSize = withContext(Dispatchers.IO) { WordPressDependencyManager.getCacheSize(context) }
                                        }
                                        val nodeSuccess = NodeDependencyManager.downloadNodeRuntime(context)
                                        if (nodeSuccess) {
                                            nodeReady = NodeDependencyManager.isNodeReady(context)
                                            nodeCacheSize = withContext(Dispatchers.IO) { NodeDependencyManager.getCacheSize(context) }
                                        }
                                        val pythonSuccess = PythonDependencyManager.downloadPythonRuntime(context)
                                        if (pythonSuccess) {
                                            pythonReady = PythonDependencyManager.isPythonReady(context)
                                            pythonCacheSize = withContext(Dispatchers.IO) { PythonDependencyManager.getCacheSize(context) }
                                        }
                                        isDownloading = false
                                        if (wpSuccess && nodeSuccess && pythonSuccess) {
                                            snackbarHostState.showSnackbar(Strings.depAllReady)
                                        }
                                    }
                                },
                                enabled = !isDownloading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(WtaRadius.Control),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(Strings.depStatusDownloading)
                                } else {
                                    Icon(Icons.Outlined.CloudDownload, null, Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(Strings.depDownloadAll)
                                }
                            }
                        }
                    }
                }
            }

            WtaSection(title = Strings.depSectionStorage) {
                WtaSettingCard {
                    StorageSummary(
                        wpCacheSize = wpCacheSize,
                        nodeCacheSize = nodeCacheSize,
                        pythonCacheSize = pythonCacheSize,
                        goCacheSize = goCacheSize,
                        phpCacheSize = phpCacheSize,
                        sqliteCacheSize = sqliteCacheSize,
                        totalSize = totalCacheSize
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PremiumOutlinedButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { WordPressDependencyManager.clearCache(context) }
                                    wpCacheSize = 0L
                                    phpCacheSize = 0L
                                    sqliteCacheSize = 0L
                                    phpReady = false
                                    wpReady = false
                                    sqliteReady = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(WtaRadius.Control),
                            enabled = wpCacheSize > 0
                        ) { Text(Strings.depClearWpCache, maxLines = 1) }

                        PremiumOutlinedButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { NodeDependencyManager.clearCache(context) }
                                    nodeCacheSize = 0L
                                    nodeReady = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(WtaRadius.Control),
                            enabled = nodeCacheSize > 0
                        ) { Text(Strings.depClearNodeCache, maxLines = 1) }

                        PremiumOutlinedButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { WordPressDependencyManager.clearCache(context) }
                                    wpCacheSize = 0L
                                    phpCacheSize = 0L
                                    sqliteCacheSize = 0L
                                    phpReady = false
                                    wpReady = false
                                    sqliteReady = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(WtaRadius.Control),
                            enabled = phpCacheSize > 0
                        ) { Text(Strings.depClearPhpCache, maxLines = 1) }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PremiumOutlinedButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { PythonDependencyManager.clearCache(context) }
                                    pythonCacheSize = 0L
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(WtaRadius.Control),
                            enabled = pythonCacheSize > 0
                        ) { Text(Strings.depClearPythonCache, maxLines = 1) }

                        PremiumOutlinedButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { GoDependencyManager.clearCache(context) }
                                    goCacheSize = 0L
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(WtaRadius.Control),
                            enabled = goCacheSize > 0
                        ) { Text(Strings.depClearGoCache, maxLines = 1) }

                        PremiumOutlinedButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { WordPressDependencyManager.clearCache(context) }
                                    wpCacheSize = 0L
                                    phpCacheSize = 0L
                                    sqliteCacheSize = 0L
                                    phpReady = false
                                    wpReady = false
                                    sqliteReady = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(WtaRadius.Control),
                            enabled = sqliteCacheSize > 0
                        ) { Text(Strings.depClearSqliteCache, maxLines = 1) }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(WtaRadius.Control),
                        enabled = totalCacheSize > 0,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Outlined.DeleteForever, null, Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Strings.depClearAll)
                    }
                }
            }
        }
    }
}

private fun onMirrorChange(
    region: String,
    setUiRegion: (WordPressDependencyManager.MirrorRegion) -> Unit
) {
    val wpRegion = when (region) {
        "cn" -> WordPressDependencyManager.MirrorRegion.CN
        "global" -> WordPressDependencyManager.MirrorRegion.GLOBAL
        else -> null
    }
    val nodeRegion = when (region) {
        "cn" -> NodeDependencyManager.MirrorRegion.CN
        "global" -> NodeDependencyManager.MirrorRegion.GLOBAL
        else -> null
    }
    WordPressDependencyManager.setMirrorRegion(wpRegion)
    NodeDependencyManager.setMirrorRegion(nodeRegion)
    setUiRegion(WordPressDependencyManager.getMirrorRegion())
}

@Composable
private fun StatusOverviewCard(
    readyCount: Int,
    totalCount: Int,
    allReady: Boolean,
    totalCacheSize: Long,
    isDownloading: Boolean,
    isPaused: Boolean,
    downloadProgress: Float,
    downloadLabel: String,
    engineState: DependencyDownloadEngine.State,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    val gradientColors = if (allReady) {
        listOf(Color(0xFF34C759), Color(0xFF66BB6A))
    } else {
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WtaRadius.Card),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (allReady) Icons.Filled.CheckCircle else Icons.Outlined.Speed,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (allReady) Strings.depAllReady else Strings.depSomeNotReady,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$readyCount / $totalCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatSize(totalCacheSize),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = Strings.depTotalStorage,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isPaused) "${Strings.depDlPaused} · $downloadLabel" else downloadLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                        .clip(RoundedCornerShape(WtaRadius.Button)),
                        color = if (isPaused) Color.White.copy(alpha = 0.5f) else Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )

                    when (val dlState = engineState) {
                        is DependencyDownloadEngine.State.Downloading -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${DependencyDownloadEngine.formatSize(dlState.bytesDownloaded)} / ${DependencyDownloadEngine.formatSize(dlState.totalBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = DependencyDownloadEngine.formatSpeed(dlState.speedBytesPerSec),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${Strings.depDlEta} ${DependencyDownloadEngine.formatEta(dlState.etaSeconds)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { if (isPaused) onResume() else onPause() }) {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                        contentDescription = if (isPaused) Strings.depDlResume else Strings.depDlPause,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                        is DependencyDownloadEngine.State.Paused -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${DependencyDownloadEngine.formatSize(dlState.bytesDownloaded)} / ${DependencyDownloadEngine.formatSize(dlState.totalBytes)} · ${Strings.depDlPaused}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { if (isPaused) onResume() else onPause() }) {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                        contentDescription = if (isPaused) Strings.depDlResume else Strings.depDlPause,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun RuntimeItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    isReady: Boolean,
    onInstall: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(WtaRadius.Control))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isReady) {
            WtaBadge(
                text = Strings.depStatusReady,
                icon = Icons.Filled.CheckCircle,
                containerColor = AppColors.Success.copy(alpha = 0.12f),
                contentColor = AppColors.Success
            )
        } else if (onInstall != null) {
            FilledTonalButton(
                onClick = onInstall,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(WtaRadius.Button)
            ) {
                Icon(Icons.Filled.Download, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(Strings.depInstall, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            WtaBadge(
                text = Strings.depStatusNotInstalled,
                containerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private data class ProjectEntry(val name: String, val count: Int, val color: Color)

@Composable
private fun StorageSummary(
    wpCacheSize: Long,
    nodeCacheSize: Long,
    pythonCacheSize: Long,
    goCacheSize: Long,
    phpCacheSize: Long,
    sqliteCacheSize: Long,
    totalSize: Long
) {
    val wpColor = AppColors.WordPress
    val nodeColor = AppColors.NodeJs
    val pythonColor = AppColors.Python
    val goColor = AppColors.Go
    val phpColor = AppColors.Php
    val sqliteColor = AppColors.SQLite

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = Strings.depTotalStorage, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(text = formatSize(totalSize), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }

    if (totalSize > 0) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(WtaRadius.Button))
        ) {
            listOf(
                wpCacheSize to wpColor,
                nodeCacheSize to nodeColor,
                pythonCacheSize to pythonColor,
                goCacheSize to goColor
            ).filter { it.first > 0 }.forEach { (size, color) ->
                val fraction = (size.toFloat() / totalSize).coerceAtLeast(0.01f)
                Box(
                    modifier = Modifier
                        .weight(fraction, true)
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StorageLegendItem("WordPress", wpCacheSize, wpColor)
            StorageLegendItem("Node.js", nodeCacheSize, nodeColor)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StorageLegendItem("Python", pythonCacheSize, pythonColor)
            StorageLegendItem("Go", goCacheSize, goColor)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StorageLegendItem("PHP", phpCacheSize, phpColor)
            StorageLegendItem("SQLite", sqliteCacheSize, sqliteColor)
        }
    }
}

@Composable
private fun StorageLegendItem(label: String, size: Long, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "$label ${formatSize(size)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format(java.util.Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun countSubdirs(dir: File): Int {
    if (!dir.exists()) return 0
    return dir.listFiles()?.count { it.isDirectory } ?: 0
}
