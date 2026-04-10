package com.webtoapp.ui.screens
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton
import com.webtoapp.ui.components.PremiumFilterChip

import com.webtoapp.ui.theme.AppColors
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.download.DependencyDownloadEngine
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.golang.GoDependencyManager
import com.webtoapp.core.nodejs.NodeDependencyManager
import com.webtoapp.core.python.PythonDependencyManager
import com.webtoapp.core.wordpress.WordPressDependencyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * 统一运行时 & 依赖管理页面
 * 
 * 整合 WordPress/PHP 依赖、Node.js 运行时、
 * Python/Go/Docs 项目文件管理、镜像源设置、缓存管理。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuntimeDepsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // ===== 运行时状态 =====
    var phpReady by remember { mutableStateOf(WordPressDependencyManager.isPhpReady(context)) }
    var wpReady by remember { mutableStateOf(WordPressDependencyManager.isWordPressReady(context)) }
    var sqliteReady by remember { mutableStateOf(WordPressDependencyManager.isSqlitePluginReady(context)) }
    var nodeReady by remember { mutableStateOf(NodeDependencyManager.isNodeReady(context)) }
    var pythonReady by remember { mutableStateOf(PythonDependencyManager.isPythonReady(context)) }
    
    // ===== 缓存 =====
    var wpCacheSize by remember { mutableLongStateOf(0L) }
    var nodeCacheSize by remember { mutableLongStateOf(0L) }
    var pythonCacheSize by remember { mutableLongStateOf(0L) }
    var goCacheSize by remember { mutableLongStateOf(0L) }
    var phpCacheSize by remember { mutableLongStateOf(0L) }
    var sqliteCacheSize by remember { mutableLongStateOf(0L) }
    
    // ===== 项目文件统计 =====
    var wpProjectCount by remember { mutableIntStateOf(0) }
    var nodeProjectCount by remember { mutableIntStateOf(0) }
    var pythonProjectCount by remember { mutableIntStateOf(0) }
    var goProjectCount by remember { mutableIntStateOf(0) }
    var docsProjectCount by remember { mutableIntStateOf(0) }
    
    // ===== 镜像源 =====
    var wpMirrorRegion by remember { mutableStateOf(WordPressDependencyManager.getMirrorRegion()) }
    var nodeMirrorRegion by remember { mutableStateOf(NodeDependencyManager.getMirrorRegion()) }
    
    // ===== UI 状态 =====
    var isDownloading by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadLabel by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // ===== 引擎丰富状态 =====
    val engineState by DependencyDownloadEngine.state.collectAsStateWithLifecycle()
    
    // 刷新数据
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
    
    // 监听下载状态
    val wpDownloadState by WordPressDependencyManager.downloadState.collectAsStateWithLifecycle()
    val nodeDownloadState by NodeDependencyManager.downloadState.collectAsStateWithLifecycle()
    val pythonDownloadState by PythonDependencyManager.downloadState.collectAsStateWithLifecycle()
    
    // 引擎状态驱动 UI
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
    
    // 后备：继续监听各 Manager 的补充事件（Complete 刷新缓存大小）
    LaunchedEffect(wpDownloadState) {
        when (wpDownloadState) {
            is WordPressDependencyManager.DownloadState.Complete -> {
                wpCacheSize = withContext(Dispatchers.IO) { WordPressDependencyManager.getCacheSize(context) }
                phpCacheSize = wpCacheSize
                sqliteCacheSize = wpCacheSize
            }
            is WordPressDependencyManager.DownloadState.Error -> {
                val st = wpDownloadState as WordPressDependencyManager.DownloadState.Error
                snackbarHostState.showSnackbar(st.message)
            }
            else -> {}
        }
    }
    
    LaunchedEffect(nodeDownloadState) {
        when (nodeDownloadState) {
            is NodeDependencyManager.DownloadState.Complete -> {
                nodeCacheSize = withContext(Dispatchers.IO) { NodeDependencyManager.getCacheSize(context) }
            }
            is NodeDependencyManager.DownloadState.Error -> {
                val st = nodeDownloadState as NodeDependencyManager.DownloadState.Error
                snackbarHostState.showSnackbar(st.message)
            }
            else -> {}
        }
    }
    
    LaunchedEffect(pythonDownloadState) {
        when (pythonDownloadState) {
            is PythonDependencyManager.DownloadState.Complete -> {
                pythonCacheSize = withContext(Dispatchers.IO) { PythonDependencyManager.getCacheSize(context) }
                pythonReady = PythonDependencyManager.isPythonReady(context)
            }
            is PythonDependencyManager.DownloadState.Error -> {
                val st = pythonDownloadState as PythonDependencyManager.DownloadState.Error
                snackbarHostState.showSnackbar(st.message)
            }
            else -> {}
        }
    }
    
    val totalCacheSize = wpCacheSize + nodeCacheSize + pythonCacheSize + goCacheSize
    val allRuntimesReady = phpReady && wpReady && sqliteReady && nodeReady && pythonReady
    val readyCount = listOf(phpReady, wpReady, sqliteReady, nodeReady, pythonReady).count { it }
    
    // ===== 清理确认对话框 =====
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Outlined.DeleteSweep, null) },
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
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(Strings.btnConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            Strings.runtimeDepsTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            Strings.runtimeDepsSubtitle,
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
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            
            // ============ 1. 状态概览卡片 ============
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
            
            // ============ 2. 运行时环境 ============
            SectionHeader(
                icon = Icons.Outlined.Memory,
                title = Strings.depSectionRuntimes
            )
            
            // 统一下载 WordPress 依赖的安装逻辑（PHP/WP/SQLite 共用）
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

            // PHP Runtime
            RuntimeItemCard(
                icon = Icons.Outlined.Code,
                iconColor = Color(0xFF777BB3),
                title = Strings.depPhpRuntime,
                description = Strings.depPhpDesc,
                isReady = phpReady,
                onInstall = installWpDeps
            )
            
            // WordPress Core（允许单独点击安装，本质调用统一安装）
            RuntimeItemCard(
                icon = Icons.Outlined.Language,
                iconColor = Color(0xFF21759B),
                title = Strings.depWpCore,
                description = Strings.depWpCoreDesc,
                isReady = wpReady,
                onInstall = installWpDeps
            )

            // Node.js Runtime
            RuntimeItemCard(
                icon = Icons.Outlined.Javascript,
                iconColor = Color(0xFF68A063),
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
            
            // Python Runtime
            RuntimeItemCard(
                icon = Icons.Outlined.Terminal,
                iconColor = Color(0xFF3776AB),
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
            
            // Go（不需要运行时下载，提供信息说明）
            RuntimeItemCard(
                icon = Icons.Outlined.RocketLaunch,
                iconColor = Color(0xFF00ADD8),
                title = Strings.depGoInfo,
                description = Strings.depGoDesc,
                isReady = true,
                onInstall = {}
            )

            // ============ 运行时插件 ============
            SectionHeader(
                icon = Icons.Outlined.Extension,
                title = Strings.depSectionRuntimePlugins
            )

            // SQLite Plugin（允许单独点击安装，本质调用统一安装）
            RuntimeItemCard(
                icon = Icons.Outlined.Storage,
                iconColor = Color(0xFF003B57),
                title = Strings.depSqlitePlugin,
                description = Strings.depSqliteDesc,
                isReady = sqliteReady,
                onInstall = installWpDeps
            )

            // ============ 3. 项目文件 ============
            SectionHeader(
                icon = Icons.Outlined.Folder,
                title = Strings.depSectionProjects
            )
            
            ProjectFilesCard(
                items = listOf(
                    ProjectEntry(Strings.depWpProjects, wpProjectCount, Color(0xFF21759B)),
                    ProjectEntry(Strings.depNodeProjects, nodeProjectCount, Color(0xFF68A063)),
                    ProjectEntry(Strings.depPythonProjects, pythonProjectCount, Color(0xFF3776AB)),
                    ProjectEntry(Strings.depGoProjects, goProjectCount, Color(0xFF00ADD8)),
                    ProjectEntry(Strings.depDocsProjects, docsProjectCount, Color(0xFFE97627))
                )
            )
            
            // ============ 4. 镜像源 & 下载 ============
            SectionHeader(
                icon = Icons.Outlined.CloudDownload,
                title = Strings.depSectionDownload
            )
            
            MirrorAndDownloadCard(
                wpMirrorRegion = wpMirrorRegion,
                onWpMirrorChange = { regionStr ->
                    WordPressDependencyManager.setMirrorRegion(
                        when (regionStr) {
                            "cn" -> WordPressDependencyManager.MirrorRegion.CN
                            "global" -> WordPressDependencyManager.MirrorRegion.GLOBAL
                            else -> null
                        }
                    )
                    // 同步设置 Node 镜像
                    NodeDependencyManager.setMirrorRegion(
                        when (regionStr) {
                            "cn" -> NodeDependencyManager.MirrorRegion.CN
                            "global" -> NodeDependencyManager.MirrorRegion.GLOBAL
                            else -> null
                        }
                    )
                    nodeMirrorRegion = NodeDependencyManager.getMirrorRegion()
                    wpMirrorRegion = WordPressDependencyManager.getMirrorRegion()
                },
                allReady = allRuntimesReady,
                isDownloading = isDownloading,
                onDownloadAll = {
                    scope.launch {
                        isDownloading = true
                        // 下载 WordPress 依赖
                        val wpSuccess = WordPressDependencyManager.downloadAllDependencies(context)
                        if (wpSuccess) {
                            phpReady = WordPressDependencyManager.isPhpReady(context)
                            wpReady = WordPressDependencyManager.isWordPressReady(context)
                            sqliteReady = WordPressDependencyManager.isSqlitePluginReady(context)
                            wpCacheSize = withContext(Dispatchers.IO) { WordPressDependencyManager.getCacheSize(context) }
                        }
                        // 下载 Node.js 运行时
                        val nodeSuccess = NodeDependencyManager.downloadNodeRuntime(context)
                        if (nodeSuccess) {
                            nodeReady = NodeDependencyManager.isNodeReady(context)
                            nodeCacheSize = withContext(Dispatchers.IO) { NodeDependencyManager.getCacheSize(context) }
                        }
                        // 下载 Python 运行时
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
                }
            )
            
            // ============ 5. 存储空间 ============
            SectionHeader(
                icon = Icons.Outlined.PieChart,
                title = Strings.depSectionStorage
            )
            
            StorageCard(
                wpCacheSize = wpCacheSize,
                nodeCacheSize = nodeCacheSize,
                pythonCacheSize = pythonCacheSize,
                goCacheSize = goCacheSize,
                phpCacheSize = phpCacheSize,
                sqliteCacheSize = sqliteCacheSize,
                totalSize = totalCacheSize,
                onClearWp = {
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
                onClearNode = {
                    scope.launch {
                        withContext(Dispatchers.IO) { NodeDependencyManager.clearCache(context) }
                        nodeCacheSize = 0L
                        nodeReady = false
                    }
                },
                onClearPython = {
                    scope.launch {
                        withContext(Dispatchers.IO) { PythonDependencyManager.clearCache(context) }
                        pythonCacheSize = 0L
                    }
                },
                onClearGo = {
                    scope.launch {
                        withContext(Dispatchers.IO) { GoDependencyManager.clearCache(context) }
                        goCacheSize = 0L
                    }
                },
                onClearPhp = {
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
                onClearSqlite = {
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
                onClearAll = { showClearDialog = true }
            )
        }
    }
        }
}

// ==================== 子组件 ====================

/**
 * 状态概览 — 顶部渐变卡片
 */
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
        listOf(AppColors.Success, Color(0xFF66BB6A))
    } else {
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    }
    
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 状态图标
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
                                imageVector = if (allReady) Icons.Filled.CheckCircle else Icons.Outlined.Downloading,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(
                            text = if (allReady) Strings.depAllReady else Strings.depSomeNotReady,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$readyCount / $totalCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    
                    // 缓存总量
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
                
                // 下载进度条 + 详细信息
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column {
                        // 第一行：文件名 + 暂停/继续按钮 + 百分比
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPaused) "${Strings.depDlPaused} · $downloadLabel" else downloadLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.weight(weight = 1f, fill = true)
                            )
                            // 暂停/继续按钮
                            IconButton(
                                onClick = { if (isPaused) onResume() else onPause() }
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                    contentDescription = if (isPaused) Strings.depDlResume else Strings.depDlPause,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "${(downloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isPaused) Color.White.copy(alpha = 0.5f) else Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                        
                        // 第二行：大小 + 速度 + ETA
                        val dlState = engineState
                        if (dlState is DependencyDownloadEngine.State.Downloading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${DependencyDownloadEngine.formatSize(dlState.bytesDownloaded)} / ${DependencyDownloadEngine.formatSize(dlState.totalBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = DependencyDownloadEngine.formatSpeed(dlState.speedBytesPerSec),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "${Strings.depDlEta} ${DependencyDownloadEngine.formatEta(dlState.etaSeconds)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            // 第三行：开始时间 + 下载地址
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${Strings.depDlStartTime} ${DependencyDownloadEngine.formatTime(dlState.startTimeMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                            Text(
                                text = dlState.url,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        } else if (dlState is DependencyDownloadEngine.State.Paused) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${DependencyDownloadEngine.formatSize(dlState.bytesDownloaded)} / ${DependencyDownloadEngine.formatSize(dlState.totalBytes)} · ${Strings.depDlPaused}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = dlState.url,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 区域标题
 */
@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 运行时项目卡片
 */
@Composable
private fun RuntimeItemCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    isReady: Boolean,
    onInstall: (() -> Unit)?
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // 文本
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 状态
            if (isReady) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AppColors.Success.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            null,
                            tint = AppColors.Success,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            Strings.depStatusReady,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.Success,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (onInstall != null) {
                FilledTonalButton(
                    onClick = onInstall,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Download,
                        null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        Strings.depInstall,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                ) {
                    Text(
                        Strings.depStatusNotInstalled,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * 项目文件卡片
 */
private data class ProjectEntry(
    val name: String,
    val count: Int,
    val color: Color
)

@Composable
private fun ProjectFilesCard(items: List<ProjectEntry>) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            items.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 色块指示
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
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    )
                    
                    Text(
                        text = Strings.depProjectCount(entry.count),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (entry.count > 0) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        fontWeight = if (entry.count > 0) FontWeight.Medium else FontWeight.Normal
                    )
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * 镜像源 & 下载卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MirrorAndDownloadCard(
    wpMirrorRegion: WordPressDependencyManager.MirrorRegion,
    onWpMirrorChange: (String) -> Unit,
    allReady: Boolean,
    isDownloading: Boolean,
    onDownloadAll: () -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 镜像源标题
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
            
            // 镜像源选择芯片
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val currentRegion = wpMirrorRegion
                
                PremiumFilterChip(
                    selected = currentRegion == WordPressDependencyManager.MirrorRegion.CN,
                    onClick = { onWpMirrorChange("cn") },
                    label = { Text(Strings.depMirrorCN) },
                    leadingIcon = if (currentRegion == WordPressDependencyManager.MirrorRegion.CN) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
                PremiumFilterChip(
                    selected = currentRegion == WordPressDependencyManager.MirrorRegion.GLOBAL,
                    onClick = { onWpMirrorChange("global") },
                    label = { Text(Strings.depMirrorGlobal) },
                    leadingIcon = if (currentRegion == WordPressDependencyManager.MirrorRegion.GLOBAL) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
                PremiumFilterChip(
                    selected = currentRegion != WordPressDependencyManager.MirrorRegion.CN
                            && currentRegion != WordPressDependencyManager.MirrorRegion.GLOBAL,
                    onClick = { onWpMirrorChange("auto") },
                    label = { Text(Strings.depMirrorAuto) }
                )
            }
            
            // 一键下载按钮
            if (!allReady) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                
                PremiumButton(
                    onClick = onDownloadAll,
                    enabled = !isDownloading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Strings.depDownloadAllDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

/**
 * 存储空间卡片 — 支持 WordPress / Node.js / Python / Go / PHP / SQLite 六种运行时
 */
@Composable
private fun StorageCard(
    wpCacheSize: Long,
    nodeCacheSize: Long,
    pythonCacheSize: Long,
    goCacheSize: Long,
    phpCacheSize: Long,
    sqliteCacheSize: Long,
    totalSize: Long,
    onClearWp: () -> Unit,
    onClearNode: () -> Unit,
    onClearPython: () -> Unit,
    onClearGo: () -> Unit,
    onClearPhp: () -> Unit,
    onClearSqlite: () -> Unit,
    onClearAll: () -> Unit
) {
    val wpColor = Color(0xFF21759B)
    val nodeColor = Color(0xFF68A063)
    val pythonColor = Color(0xFF3776AB)
    val goColor = Color(0xFF00ADD8)
    val phpColor = Color(0xFF777BB3)
    val sqliteColor = Color(0xFF003B57)

    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 总占用
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Strings.depTotalStorage,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatSize(totalSize),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 存储条 — 4 段
            if (totalSize > 0) {
                val segments = listOf(
                    wpCacheSize to wpColor,
                    nodeCacheSize to nodeColor,
                    pythonCacheSize to pythonColor,
                    goCacheSize to goColor
                ).filter { it.first > 0 }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    segments.forEach { (size, color) ->
                        val fraction = (size.toFloat() / totalSize).coerceAtLeast(0.01f)
                        Box(
                            modifier = Modifier
                                .weight(weight = fraction, fill = true)
                                .fillMaxHeight()
                                .background(color)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 图例 — 三行两列
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StorageLegendItem("WordPress", wpCacheSize, wpColor)
                    StorageLegendItem("Node.js", nodeCacheSize, nodeColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StorageLegendItem("Python", pythonCacheSize, pythonColor)
                    StorageLegendItem("Go", goCacheSize, goColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StorageLegendItem("PHP", phpCacheSize, phpColor)
                    StorageLegendItem("SQLite", sqliteCacheSize, sqliteColor)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            
            // 清理按钮组 — 第一行 WordPress & Node.js & PHP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PremiumOutlinedButton(
                    onClick = onClearWp,
                    modifier = Modifier.weight(weight = 1f, fill = true),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                    enabled = wpCacheSize > 0
                ) {
                    Text(
                        Strings.depClearWpCache,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
                PremiumOutlinedButton(
                    onClick = onClearNode,
                    modifier = Modifier.weight(weight = 1f, fill = true),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                    enabled = nodeCacheSize > 0
                ) {
                    Text(
                        Strings.depClearNodeCache,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
                PremiumOutlinedButton(
                    onClick = onClearPhp,
                    modifier = Modifier.weight(weight = 1f, fill = true),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                    enabled = phpCacheSize > 0
                ) {
                    Text(
                        Strings.depClearPhpCache,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 清理按钮组 — 第二行 Python & Go & SQLite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PremiumOutlinedButton(
                    onClick = onClearPython,
                    modifier = Modifier.weight(weight = 1f, fill = true),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                    enabled = pythonCacheSize > 0
                ) {
                    Text(
                        Strings.depClearPythonCache,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
                PremiumOutlinedButton(
                    onClick = onClearGo,
                    modifier = Modifier.weight(weight = 1f, fill = true),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                    enabled = goCacheSize > 0
                ) {
                    Text(
                        Strings.depClearGoCache,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
                PremiumOutlinedButton(
                    onClick = onClearSqlite,
                    modifier = Modifier.weight(weight = 1f, fill = true),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                    enabled = sqliteCacheSize > 0
                ) {
                    Text(
                        Strings.depClearSqliteCache,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            FilledTonalButton(
                onClick = onClearAll,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                enabled = totalSize > 0,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Outlined.DeleteSweep, null, Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Strings.depClearAll)
            }
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
        Text(
            text = "$label ${formatSize(size)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== 工具函数 ====================

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
