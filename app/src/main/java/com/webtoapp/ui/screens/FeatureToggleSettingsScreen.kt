package com.webtoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.settings.FeatureToggleManager
import com.webtoapp.ui.components.ThemedBackgroundBox
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureToggleSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val toggleManager = remember { FeatureToggleManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    val isAiToolsEnabled by toggleManager.isAiToolsEnabled.collectAsState(initial = true)
    val isDevToolsEnabled by toggleManager.isDevToolsEnabled.collectAsState(initial = true)
    val isBrowserNetworkEnabled by toggleManager.isBrowserNetworkEnabled.collectAsState(initial = true)
    val isDataStatsEnabled by toggleManager.isDataStatsEnabled.collectAsState(initial = true)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        Strings.featureToggleSettings,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = Strings.featureToggleSettings)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 说明文字
            Text(
                Strings.featureToggleDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // AI 工具
            FeatureToggleItem(
                title = Strings.featureAiTools,
                description = Strings.featureAiToolsDesc,
                checked = isAiToolsEnabled,
                onCheckedChange = { scope.launch { toggleManager.setAiToolsEnabled(it) } }
            )

            // 开发工具
            FeatureToggleItem(
                title = Strings.featureDevTools,
                description = Strings.featureDevToolsDesc,
                checked = isDevToolsEnabled,
                onCheckedChange = { scope.launch { toggleManager.setDevToolsEnabled(it) } }
            )

            // 浏览器 & 网络
            FeatureToggleItem(
                title = Strings.featureBrowserNetwork,
                description = Strings.featureBrowserNetworkDesc,
                checked = isBrowserNetworkEnabled,
                onCheckedChange = { scope.launch { toggleManager.setBrowserNetworkEnabled(it) } }
            )

            // 数据 & 统计
            FeatureToggleItem(
                title = Strings.featureDataStats,
                description = null,
                checked = isDataStatsEnabled,
                onCheckedChange = { scope.launch { toggleManager.setDataStatsEnabled(it) } }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeatureToggleItem(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (description != null) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
