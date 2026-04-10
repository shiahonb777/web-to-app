package com.webtoapp.ui.screens

import android.net.Uri
import com.webtoapp.ui.components.PremiumOutlinedButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumTextField
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.adblock.HostsSource
import com.webtoapp.core.i18n.Strings
import kotlinx.coroutines.launch
import com.webtoapp.ui.components.ThemedBackgroundBox
import androidx.compose.ui.graphics.Color

/**
 * Hosts 广告拦截管理界面
 * 支持从文件和 URL 导入 hosts 规则
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostsAdBlockScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Get AdBlocker 实例（通过 Koin 注入）
    val adBlocker = remember { org.koin.java.KoinJavaComponent.get<com.webtoapp.core.adblock.AdBlocker>(com.webtoapp.core.adblock.AdBlocker::class.java) }
    
    // 状态
    var hostsRulesCount by remember { mutableIntStateOf(adBlocker.getHostsFileRuleCount()) }
    var isImporting by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var importUrl by remember { mutableStateOf("") }
    
    // 已启用的源
    var enabledSources by remember { mutableStateOf(adBlocker.getEnabledHostsSources()) }
    
    // File选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isImporting = true
                val result = adBlocker.importHostsFromFile(context, it)
                result.fold(
                    onSuccess = { count ->
                        hostsRulesCount = adBlocker.getHostsFileRuleCount()
                        adBlocker.saveHostsRules(context)
                        snackbarHostState.showSnackbar(
                            String.format(java.util.Locale.getDefault(), Strings.importHostsSuccess, count)
                        )
                    },
                    onFailure = { error ->
                        snackbarHostState.showSnackbar(
                            "${Strings.importHostsFailed}: ${error.message}"
                        )
                    }
                )
                isImporting = false
            }
        }
    }
    
    // Load已保存的规则
    LaunchedEffect(Unit) {
        adBlocker.loadHostsRules(context)
        hostsRulesCount = adBlocker.getHostsFileRuleCount()
        enabledSources = adBlocker.getEnabledHostsSources()
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(Strings.hostsAdBlock, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text(
                            Strings.hostsAdBlockSubtitle,
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
            // 统计卡片
            item {
                EnhancedElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                String.format(java.util.Locale.getDefault(), Strings.hostsRulesCount, hostsRulesCount),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (enabledSources.isNotEmpty()) {
                                Text(
                                    "${enabledSources.size} ${Strings.enabledSources}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        if (hostsRulesCount > 0) {
                            Icon(
                                Icons.Outlined.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            // Import选项
            item {
                Text(
                    Strings.importFromFile,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 从文件导入
                    EnhancedElevatedCard(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.weight(weight = 1f, fill = true),
                        enabled = !isImporting
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.FileOpen,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                Strings.importFromFile,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    // 从 URL 导入
                    EnhancedElevatedCard(
                        onClick = { showUrlDialog = true },
                        modifier = Modifier.weight(weight = 1f, fill = true),
                        enabled = !isImporting
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.Link,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                Strings.importFromUrl,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // 常用 hosts 源
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    Strings.popularHostsSources,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            items(AdBlocker.POPULAR_HOSTS_SOURCES) { source ->
                HostsSourceCard(
                    source = source,
                    isEnabled = enabledSources.contains(source.url),
                    isImporting = isImporting,
                    onImport = {
                        scope.launch {
                            isImporting = true
                            val result = adBlocker.importHostsFromUrl(source.url)
                            result.fold(
                                onSuccess = { count ->
                                    hostsRulesCount = adBlocker.getHostsFileRuleCount()
                                    enabledSources = adBlocker.getEnabledHostsSources()
                                    adBlocker.saveHostsRules(context)
                                    snackbarHostState.showSnackbar(
                                        String.format(java.util.Locale.getDefault(), Strings.importHostsSuccess, count)
                                    )
                                },
                                onFailure = { error ->
                                    snackbarHostState.showSnackbar(
                                        "${Strings.importHostsFailed}: ${error.message}"
                                    )
                                }
                            )
                            isImporting = false
                        }
                    }
                )
            }
            
            // 清空按钮
            if (hostsRulesCount > 0) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    PremiumOutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.clearHostsRules)
                    }
                }
            }
            
            // 说明
            item {
                Spacer(modifier = Modifier.height(8.dp))
                EnhancedElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            Strings.hostsBlockingDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Load指示器
        if (isImporting) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(Strings.importingHosts)
                    }
                }
            }
        }
    }
    
    // URL 导入对话框
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { if (!isImporting) showUrlDialog = false },
            title = { Text(Strings.importFromUrl) },
            text = {
                Column {
                    PremiumTextField(
                        value = importUrl,
                        onValueChange = { importUrl = it },
                        label = { Text(Strings.importHostsUrl) },
                        placeholder = { Text(Strings.importHostsUrlHint) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isImporting
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (importUrl.isNotBlank()) {
                            scope.launch {
                                isImporting = true
                                showUrlDialog = false
                                val result = adBlocker.importHostsFromUrl(importUrl)
                                result.fold(
                                    onSuccess = { count ->
                                        hostsRulesCount = adBlocker.getHostsFileRuleCount()
                                        enabledSources = adBlocker.getEnabledHostsSources()
                                        adBlocker.saveHostsRules(context)
                                        snackbarHostState.showSnackbar(
                                            String.format(java.util.Locale.getDefault(), Strings.importHostsSuccess, count)
                                        )
                                        importUrl = ""
                                    },
                                    onFailure = { error ->
                                        snackbarHostState.showSnackbar(
                                            "${Strings.importHostsFailed}: ${error.message}"
                                        )
                                    }
                                )
                                isImporting = false
                            }
                        }
                    },
                    enabled = importUrl.isNotBlank() && !isImporting
                ) {
                    Text(Strings.downloadAndImport)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUrlDialog = false; importUrl = "" },
                    enabled = !isImporting
                ) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }
    
    // 清空确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(Strings.clearHostsRules) },
            text = { Text(Strings.clearHostsConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            adBlocker.clearHostsFileRules()
                            adBlocker.saveHostsRules(context)
                            hostsRulesCount = 0
                            enabledSources = emptySet()
                            showClearDialog = false
                            snackbarHostState.showSnackbar(Strings.hostsCleared)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
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
        }
}

/**
 * Hosts 源卡片
 */
@Composable
private fun HostsSourceCard(
    source: HostsSource,
    isEnabled: Boolean,
    isImporting: Boolean,
    onImport: () -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        source.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (isEnabled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                Strings.hostsSourceAdded,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    source.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            FilledTonalButton(
                onClick = onImport,
                enabled = !isImporting,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    if (isEnabled) Icons.Outlined.Refresh else Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (isEnabled) Strings.retry else Strings.downloadAndImport,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
