package com.webtoapp.ui.screens

import androidx.compose.foundation.background
import com.webtoapp.ui.components.PremiumOutlinedButton
import com.webtoapp.ui.components.PremiumFilterChip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.wordpress.WordPressDependencyManager
import com.webtoapp.ui.components.EnhancedElevatedCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.webtoapp.ui.components.ThemedBackgroundBox
import androidx.compose.ui.graphics.Color

/**
 * WordPress settings- switch, management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPressSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // state
    var phpReady by remember { mutableStateOf(WordPressDependencyManager.isPhpReady(context)) }
    var wpReady by remember { mutableStateOf(WordPressDependencyManager.isWordPressReady(context)) }
    var sqliteReady by remember { mutableStateOf(WordPressDependencyManager.isSqlitePluginReady(context)) }
    var cacheSize by remember { mutableLongStateOf(0L) }
    var mirrorRegion by remember { mutableStateOf(WordPressDependencyManager.getMirrorRegion()) }
    
    // refresh
    LaunchedEffect(Unit) {
        cacheSize = withContext(Dispatchers.IO) { WordPressDependencyManager.getCacheSize(context) }
    }
    
    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(AppStringsProvider.current().wpSettings) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, AppStringsProvider.current().back)
                    }
                }
            )
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // statecard
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = AppStringsProvider.current().wpDownloadDeps,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    DependencyStatusRow("PHP", phpReady)
                    DependencyStatusRow("WordPress Core", wpReady)
                    DependencyStatusRow("SQLite Plugin", sqliteReady)
                }
            }
            
            // settingscard
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CloudDownload,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = AppStringsProvider.current().wpMirrorSource,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Note
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(AppStringsProvider.current().wpAutoDetect)
                            Text(
                                text = AppStringsProvider.current().wpDownloadDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Note
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumFilterChip(
                            selected = mirrorRegion == WordPressDependencyManager.MirrorRegion.CN,
                            onClick = {
                                mirrorRegion = WordPressDependencyManager.MirrorRegion.CN
                                WordPressDependencyManager.setMirrorRegion(WordPressDependencyManager.MirrorRegion.CN)
                            },
                            label = { Text(AppStringsProvider.current().wpMirrorCN) },
                            leadingIcon = if (mirrorRegion == WordPressDependencyManager.MirrorRegion.CN) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null
                        )
                        PremiumFilterChip(
                            selected = mirrorRegion == WordPressDependencyManager.MirrorRegion.GLOBAL,
                            onClick = {
                                mirrorRegion = WordPressDependencyManager.MirrorRegion.GLOBAL
                                WordPressDependencyManager.setMirrorRegion(WordPressDependencyManager.MirrorRegion.GLOBAL)
                            },
                            label = { Text(AppStringsProvider.current().wpMirrorGlobal) },
                            leadingIcon = if (mirrorRegion == WordPressDependencyManager.MirrorRegion.GLOBAL) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null
                        )
                        PremiumFilterChip(
                            selected = mirrorRegion != WordPressDependencyManager.MirrorRegion.CN
                                    && mirrorRegion != WordPressDependencyManager.MirrorRegion.GLOBAL,
                            onClick = {
                                WordPressDependencyManager.setMirrorRegion(null)
                                mirrorRegion = WordPressDependencyManager.getMirrorRegion()
                            },
                            label = { Text(AppStringsProvider.current().wpAutoDetect) }
                        )
                    }
                }
            }
            
            // managementcard
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Storage,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = AppStringsProvider.current().wpCacheSize,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = formatSize(cacheSize),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    PremiumOutlinedButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    WordPressDependencyManager.clearCache(context)
                                }
                                cacheSize = 0L
                                phpReady = false
                                wpReady = false
                                sqliteReady = false
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(AppStringsProvider.current().wpClearCache)
                    }
                }
            }
        }
    }
        }
}

@Composable
private fun DependencyStatusRow(name: String, ready: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium
        )
        Icon(
            imageVector = if (ready) Icons.Default.CheckCircle else Icons.Outlined.Cancel,
            contentDescription = null,
            tint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}
