package com.webtoapp.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.webtoapp.core.i18n.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 浏览器内核设置界面
 * 显示当前 WebView 信息、已安装的浏览器列表、推荐浏览器下载
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserKernelScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // WebView 信息
    var webViewInfo by remember { mutableStateOf<WebViewInfo?>(null) }
    
    // 已安装的浏览器
    var installedBrowsers by remember { mutableStateOf<List<BrowserInfo>>(emptyList()) }
    
    // Load数据
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            webViewInfo = getWebViewInfo(context)
            installedBrowsers = getInstalledBrowsers(context)
        }
    }
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(Strings.browserKernelTitle)
                        Text(
                            Strings.browserKernelSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, Strings.back)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 当前 WebView 信息卡片
            item {
                CurrentWebViewCard(
                    webViewInfo = webViewInfo,
                    onOpenDeveloperOptions = {
                        openDeveloperOptions(context)
                    }
                )
            }
            
            // 已安装的浏览器
            item {
                SectionHeader(
                    title = Strings.installedBrowsers,
                    subtitle = Strings.installedBrowsersDesc
                )
            }
            
            if (installedBrowsers.isEmpty()) {
                item {
                    Card(
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
            
            // 推荐浏览器下载
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
            
            // 帮助说明
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HelpCard()
            }
            
            // 底部间距
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 当前 WebView 信息卡片
 */
@Composable
private fun CurrentWebViewCard(
    webViewInfo: WebViewInfo?,
    onOpenDeveloperOptions: () -> Unit
) {
    Card(
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
            
            OutlinedButton(
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
 * 信息行
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
 * 区域标题
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
 * 已安装浏览器卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstalledBrowserCard(
    browser: BrowserInfo,
    isCurrentProvider: Boolean,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 浏览器图标
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
            
            Column(modifier = Modifier.weight(1f)) {
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
 * 推荐浏览器卡片
 */
@Composable
private fun RecommendedBrowserCard(
    browser: RecommendedBrowser,
    isInstalled: Boolean,
    onDownload: () -> Unit,
    onOpenUrl: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 浏览器图标占位
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
            
            Column(modifier = Modifier.weight(1f)) {
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
                    // Play Store 下载按钮
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
                    
                    // Web page下载按钮（如果有下载链接）
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
 * 帮助卡片
 */
@Composable
private fun HelpCard() {
    Card(
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

// ==================== 数据类 ====================

/**
 * WebView 信息
 */
data class WebViewInfo(
    val providerName: String,
    val version: String,
    val packageName: String
)

/**
 * 已安装浏览器信息
 */
data class BrowserInfo(
    val name: String,
    val packageName: String,
    val version: String,
    val icon: android.graphics.drawable.Drawable?,
    val canBeWebViewProvider: Boolean
)

/**
 * 推荐浏览器
 */
data class RecommendedBrowser(
    val name: String,
    val packageName: String,
    val description: String,
    val downloadUrl: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val brandColor: androidx.compose.ui.graphics.Color
)

// ==================== 推荐浏览器列表 ====================

/**
 * 获取推荐浏览器列表
 * 使用函数以便在运行时获取正确的多语言字符串
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

// ==================== 工具函数 ====================

/**
 * 获取当前 WebView 信息
 */
private fun getWebViewInfo(context: Context): WebViewInfo {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val webViewPackage = WebView.getCurrentWebViewPackage()
            if (webViewPackage != null) {
                WebViewInfo(
                    providerName = webViewPackage.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: webViewPackage.packageName,
                    version = webViewPackage.versionName ?: "Unknown",
                    packageName = webViewPackage.packageName
                )
            } else {
                getDefaultWebViewInfo()
            }
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
 * 获取已安装的浏览器列表
 */
private fun getInstalledBrowsers(context: Context): List<BrowserInfo> {
    val pm = context.packageManager
    val browsers = mutableListOf<BrowserInfo>()
    
    // 查询所有可以处理 HTTP 请求的应用
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
    val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    }
    
    // WebView 提供者包名列表（这些浏览器可以作为 WebView 提供者）
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
        
        // 排除自身和系统应用选择器
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
            // 忽略无法获取信息的包
        }
    }
    
    // 按名称排序，优先显示可作为 WebView 提供者的浏览器
    return browsers.sortedWith(
        compareByDescending<BrowserInfo> { it.canBeWebViewProvider }
            .thenBy { it.name }
    )
}

/**
 * 打开开发者选项
 */
private fun openDeveloperOptions(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        context.startActivity(intent)
    } catch (e: Exception) {
        // 如果无法直接打开开发者选项，尝试打开设置
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        } catch (e2: Exception) {
            // 忽略
        }
    }
}

/**
 * 打开应用
 */
private fun openApp(context: Context, packageName: String) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        // 忽略
    }
}

/**
 * 打开 Play Store
 */
private fun openPlayStore(context: Context, packageName: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        context.startActivity(intent)
    } catch (e: Exception) {
        // 如果没有 Play Store，打开浏览器
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            context.startActivity(intent)
        } catch (e2: Exception) {
            // 忽略
        }
    }
}

/**
 * 打开 URL
 */
private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // 忽略
    }
}
