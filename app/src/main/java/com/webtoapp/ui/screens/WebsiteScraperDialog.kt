package com.webtoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.scraper.WebsiteScraper
import com.webtoapp.ui.animation.AnimatedAlertDialog
import com.webtoapp.ui.components.*

/**
 * 网站离线打包对话框
 * 
 * 输入 URL → 配置抓取参数 → 实时显示进度 → 自动创建离线 HTML 应用
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteScraperDialog(
    onDismiss: () -> Unit,
    onStartScrape: (
        name: String,
        url: String,
        maxDepth: Int,
        downloadCdn: Boolean,
        onProgress: (WebsiteScraper.ScrapeProgress) -> Unit
    ) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var appName by remember { mutableStateOf("") }
    var maxDepth by remember { mutableIntStateOf(3) }
    var downloadCdn by remember { mutableStateOf(true) }
    var showAdvanced by remember { mutableStateOf(false) }
    
    // Scraping state
    var isScraping by remember { mutableStateOf(false) }
    var scrapeProgress by remember { mutableStateOf<WebsiteScraper.ScrapeProgress?>(null) }
    
    // Accent color
    val accentColor = Color(0xFF6366F1)
    
    AnimatedAlertDialog(
        onDismissRequest = { if (!isScraping) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.CloudDownload,
                        null,
                        modifier = Modifier.size(22.dp),
                        tint = accentColor
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "网站离线打包",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "抓取网站前端资源生成离线应用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // URL 输入
                PremiumTextField(
                    value = url,
                    onValueChange = { 
                        url = it
                        // Auto-extract domain as app name
                        if (appName.isBlank() && it.isNotBlank()) {
                            try {
                                val host = java.net.URL(
                                    if (it.startsWith("http")) it else "https://$it"
                                ).host
                                appName = host.removePrefix("www.")
                            } catch (_: Exception) {}
                        }
                    },
                    label = { Text("网站 URL") },
                    placeholder = { Text("https://example.com") },
                    leadingIcon = { Icon(Icons.Outlined.Link, null) },
                    singleLine = true,
                    enabled = !isScraping,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    )
                )
                
                // App 名称
                PremiumTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text(Strings.labelAppName) },
                    placeholder = { Text("我的离线应用") },
                    leadingIcon = { Icon(Icons.Outlined.AppShortcut, null) },
                    singleLine = true,
                    enabled = !isScraping,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                
                // Advanced settings toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Tune,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "高级配置",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    PremiumSwitch(
                        checked = showAdvanced,
                        onCheckedChange = { showAdvanced = it }
                    )
                }
                
                // Advanced settings
                AnimatedVisibility(
                    visible = showAdvanced,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Depth slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "爬取深度",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "$maxDepth 层",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor
                                )
                            }
                            Slider(
                                value = maxDepth.toFloat(),
                                onValueChange = { maxDepth = it.toInt() },
                                valueRange = 1f..5f,
                                steps = 3,
                                enabled = !isScraping,
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor
                                )
                            )
                            Text(
                                "深度越大抓取越完整，但耗时越长",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // CDN toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "下载 CDN 资源",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "包含第三方 CDN 上的 JS/CSS/字体",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            PremiumSwitch(
                                checked = downloadCdn,
                                onCheckedChange = { downloadCdn = it }
                            )
                        }
                    }
                }
                
                HorizontalDivider()
                
                // Info card
                if (!isScraping) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.06f),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                null,
                                modifier = Modifier.size(18.dp),
                                tint = accentColor
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "将抓取网站的 HTML、CSS、JS、图片、字体等前端资源，" +
                                "重写为本地相对路径，生成可离线运行的应用。" +
                                "适用于静态网站、文档站点、单页应用等。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Progress section
                if (isScraping && scrapeProgress != null) {
                    val progress = scrapeProgress!!
                    
                    // Animated progress
                    val animatedFileCount by animateIntAsState(
                        targetValue = progress.downloadedFiles,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "fileCount"
                    )
                    
                    // Pulse animation
                    var pulseAlpha by remember { mutableFloatStateOf(0.6f) }
                    LaunchedEffect(isScraping) {
                        while (isScraping) {
                            kotlinx.coroutines.delay(800)
                            pulseAlpha = if (pulseAlpha > 0.8f) 0.6f else 1f
                        }
                    }
                    val animPulse by animateFloatAsState(
                        targetValue = pulseAlpha,
                        animationSpec = tween(600),
                        label = "pulseAlpha"
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Phase indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val phaseIcon = when (progress.phase) {
                                    WebsiteScraper.ScrapeProgress.Phase.ANALYZING -> Icons.Outlined.Search
                                    WebsiteScraper.ScrapeProgress.Phase.DOWNLOADING -> Icons.Outlined.CloudDownload
                                    WebsiteScraper.ScrapeProgress.Phase.REWRITING -> Icons.Outlined.AutoFixHigh
                                    WebsiteScraper.ScrapeProgress.Phase.COMPLETE -> Icons.Outlined.CheckCircle
                                    WebsiteScraper.ScrapeProgress.Phase.ERROR -> Icons.Outlined.Error
                                }
                                val phaseColor = when (progress.phase) {
                                    WebsiteScraper.ScrapeProgress.Phase.COMPLETE -> Color(0xFF22C55E)
                                    WebsiteScraper.ScrapeProgress.Phase.ERROR -> MaterialTheme.colorScheme.error
                                    else -> accentColor
                                }
                                
                                Icon(
                                    phaseIcon,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = phaseColor.copy(alpha = animPulse)
                                )
                                Text(
                                    progress.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Stats row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatChip(
                                    label = "文件",
                                    value = "$animatedFileCount",
                                    color = accentColor
                                )
                                StatChip(
                                    label = "大小",
                                    value = formatBytes(progress.downloadedBytes),
                                    color = Color(0xFF22C55E)
                                )
                            }
                            
                            // Indeterminate progress
                            if (progress.phase != WebsiteScraper.ScrapeProgress.Phase.COMPLETE &&
                                progress.phase != WebsiteScraper.ScrapeProgress.Phase.ERROR
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = accentColor
                                )
                            }
                            
                            // Current file
                            if (progress.currentFile.isNotBlank()) {
                                Text(
                                    progress.currentFile,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isScraping) {
                PremiumButton(
                    onClick = {
                        if (url.isBlank()) return@PremiumButton
                        val finalUrl = if (url.startsWith("http")) url else "https://$url"
                        isScraping = true
                        onStartScrape(appName, finalUrl, maxDepth, downloadCdn) { progress ->
                            scrapeProgress = progress
                            if (progress.phase == WebsiteScraper.ScrapeProgress.Phase.COMPLETE ||
                                progress.phase == WebsiteScraper.ScrapeProgress.Phase.ERROR
                            ) {
                                isScraping = false
                                if (progress.phase == WebsiteScraper.ScrapeProgress.Phase.COMPLETE) {
                                    // Auto-dismiss on success after a short delay
                                    // (handled by caller via UiState.Success)
                                }
                            }
                        }
                    },
                    enabled = url.isNotBlank()
                ) {
                    Icon(Icons.Outlined.CloudDownload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("开始抓取")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isScraping
            ) {
                Text(Strings.btnCancel)
            }
        }
    )
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
