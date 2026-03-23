package com.webtoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.linux.*
import com.webtoapp.ui.theme.LocalAppTheme
import com.webtoapp.ui.theme.enhanced.LocalEnhancedBackgroundEnabled
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinuxEnvironmentScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isEnhanced = LocalEnhancedBackgroundEnabled.current
    val theme = LocalAppTheme.current
    
    // Get主题主色调
    val themeAccentColor = MaterialTheme.colorScheme.primary
    
    val envManager = remember { LinuxEnvironmentManager.getInstance(context) }
    val envState by envManager.state.collectAsState()
    val installProgress by envManager.installProgress.collectAsState()
    
    var envInfo by remember { mutableStateOf<EnvironmentInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(envState) {
        isLoading = true
        envManager.checkEnvironment()
        envInfo = envManager.getEnvironmentInfo()
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.buildEnvironment) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, Strings.back)
                    }
                },
                actions = {
                    if (envInfo != null) {
                        IconButton(onClick = { showResetDialog = true }) {
                            Icon(Icons.Outlined.RestartAlt, Strings.btnReset)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(envState, installProgress, isEnhanced, themeAccentColor) {
                scope.launch {
                    envManager.initialize { _, _ -> }
                    envInfo = envManager.getEnvironmentInfo()
                }
            }
            
            AnimatedVisibility(visible = envInfo != null) {
                envInfo?.let { info ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        BuildToolsCard(info, isEnhanced, themeAccentColor)
                        StorageCard(info, isEnhanced, themeAccentColor) { showClearCacheDialog = true }
                        FeaturesCard(isEnhanced, themeAccentColor)
                        TechCard(isEnhanced, themeAccentColor)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Outlined.Warning, null, tint = Color(0xFFFFA726)) },
            title = { Text(Strings.resetEnvironment) },
            text = { Text(Strings.resetEnvConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        scope.launch {
                            envManager.reset()
                            envInfo = envManager.getEnvironmentInfo()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE53935))
                ) { Text(Strings.btnReset) }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text(Strings.btnCancel) } }
        )
    }
    
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            icon = { Icon(Icons.Outlined.CleaningServices, null) },
            title = { Text(Strings.clearCacheTitle) },
            text = { Text(Strings.clearCacheConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheDialog = false
                    scope.launch {
                        envManager.clearCache()
                        envInfo = envManager.getEnvironmentInfo()
                    }
                }) { Text(Strings.clean) }
            },
            dismissButton = { TextButton(onClick = { showClearCacheDialog = false }) { Text(Strings.btnCancel) } }
        )
    }
}

/**
 * 通用卡片容器 - 完全不使用 Card 组件
 */
@Composable
private fun CardContainer(
    modifier: Modifier = Modifier,
    isEnhanced: Boolean,
    backgroundColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val bgColor = backgroundColor ?: if (isEnhanced) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
    ) {
        Column(content = content)
    }
}

@Composable
private fun StatusCard(
    state: EnvironmentState,
    progress: InstallProgress,
    isEnhanced: Boolean,
    themeColor: Color,
    onInstall: () -> Unit
) {
    val isReady = state is EnvironmentState.Ready
    val isInstalling = state is EnvironmentState.Downloading || state is EnvironmentState.Installing
    
    // 使用主题色作为就绪状态的颜色
    val readyColor = themeColor
    
    val cardColor = when {
        isReady -> readyColor.copy(alpha = if (isEnhanced) 0.2f else 0.15f)
        isInstalling -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isEnhanced) 0.4f else 0.3f)
        else -> if (isEnhanced) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f) 
                else MaterialTheme.colorScheme.surfaceVariant
    }
    
    CardContainer(isEnhanced = isEnhanced, backgroundColor = cardColor) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // 状态图标 - 使用主题色
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isReady -> readyColor
                                isInstalling -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isReady -> Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        isInstalling -> CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color.White, strokeWidth = 3.dp)
                        else -> Icon(Icons.Outlined.Build, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (state) {
                            is EnvironmentState.Ready -> Strings.envReady
                            is EnvironmentState.NotInstalled -> Strings.envNotInstalled
                            is EnvironmentState.Downloading -> "${Strings.envDownloading}: ${state.component}"
                            is EnvironmentState.Installing -> "${Strings.envInstalling}: ${state.step}"
                            else -> Strings.ready
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (state) {
                            is EnvironmentState.Ready -> Strings.canBuildFrontend
                            is EnvironmentState.NotInstalled -> Strings.builtInPackagerReady
                            is EnvironmentState.Downloading -> "${(state.progress * 100).toInt()}%"
                            is EnvironmentState.Installing -> "${(state.progress * 100).toInt()}%"
                            else -> Strings.canBuildFrontend
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isInstalling) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    val progressValue = when (state) {
                        is EnvironmentState.Downloading -> state.progress
                        is EnvironmentState.Installing -> state.progress
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = progressValue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    if (progress.currentStep.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            progress.currentStep,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            AnimatedVisibility(visible = state is EnvironmentState.NotInstalled) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Button(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Download, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.installAdvancedBuildTool)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        Strings.optionalEsbuildHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BuildToolsCard(info: EnvironmentInfo, isEnhanced: Boolean, themeColor: Color) {
    CardContainer(isEnhanced = isEnhanced) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Build, 
                        null, 
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.buildTools, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ToolRow(
                icon = Icons.Outlined.Code, 
                name = Strings.builtInPackager, 
                status = Strings.ready, 
                description = Strings.pureKotlinImpl, 
                color = themeColor, 
                isAvailable = true,
                isEnhanced = isEnhanced
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ToolRow(
                icon = Icons.Outlined.Speed,
                name = "esbuild",
                status = if (info.esbuildAvailable) Strings.installed else Strings.notInstalled,
                description = Strings.highPerfBuildTool,
                color = if (info.esbuildAvailable) themeColor else Color(0xFF9E9E9E),
                isAvailable = info.esbuildAvailable,
                isEnhanced = isEnhanced
            )
        }
    }
}

@Composable
private fun ToolRow(
    icon: ImageVector,
    name: String,
    status: String,
    description: String,
    color: Color,
    isAvailable: Boolean,
    isEnhanced: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isEnhanced) color.copy(alpha = 0.08f)
                else color.copy(alpha = 0.05f)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        status,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Medium
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
    }
}

@Composable
private fun StorageCard(info: EnvironmentInfo, isEnhanced: Boolean, themeColor: Color, onClearCache: () -> Unit) {
    CardContainer(isEnhanced = isEnhanced) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Storage, 
                        null, 
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.storageUsage, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Storage项
            StorageRow(Strings.buildTools, formatSize(info.storageUsed), isEnhanced)
            Spacer(modifier = Modifier.height(8.dp))
            StorageRow(Strings.cache, formatSize(info.cacheSize), isEnhanced)
            
            if (info.cacheSize > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onClearCache, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.CleaningServices, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.btnClearCache)
                }
            }
        }
    }
}

@Composable
private fun StorageRow(label: String, value: String, isEnhanced: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isEnhanced) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FeaturesCard(isEnhanced: Boolean, themeColor: Color) {
    CardContainer(isEnhanced = isEnhanced) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Lightbulb, 
                        null, 
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.supportedFeatures, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val features = listOf(
                Strings.featureImportBuiltProjects,
                Strings.featureAutoDetectFramework,
                Strings.featureSupportViteWebpack,
                Strings.featureTypeScriptSupport,
                Strings.featureStaticAssets,
                Strings.featureEsbuildOptional
            )
            
            features.forEach { text ->
                FeatureRow(text, isEnhanced, themeColor)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun FeatureRow(text: String, isEnhanced: Boolean, themeColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(themeColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Check, 
                null, 
                tint = themeColor, 
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text, 
            style = MaterialTheme.typography.bodyMedium, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TechCard(isEnhanced: Boolean, themeColor: Color) {
    val bgColor = if (isEnhanced) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    CardContainer(isEnhanced = isEnhanced, backgroundColor = bgColor) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Info, 
                        null, 
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.techDescription, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                Strings.techDescriptionContent,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}
