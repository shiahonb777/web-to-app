package com.webtoapp.ui.screens

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.R
import com.webtoapp.util.AppUpdateChecker
import kotlinx.coroutines.launch

/**
 * 关于作者页面 - 现代简约风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 当前版本信息
    val (currentVersionName, currentVersionCode) = remember {
        AppUpdateChecker.getCurrentVersionInfo(context)
    }
    
    // 检查更新状态
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdateChecker.UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var checkError by remember { mutableStateOf<String?>(null) }
    
    // 下载状态
    var isDownloading by remember { mutableStateOf(false) }
    var downloadId by remember { mutableLongStateOf(-1L) }
    
    // 监听下载完成
    DisposableEffect(downloadId) {
        if (downloadId == -1L) return@DisposableEffect onDispose {}
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (id == downloadId) {
                    isDownloading = false
                    Toast.makeText(context, "下载完成，正在安装...", Toast.LENGTH_SHORT).show()
                    AppUpdateChecker.installApk(context, downloadId)
                }
            }
        }
        
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_EXPORTED)
        
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }
    
    // 主题色
    val primaryGradient = listOf(Color(0xFF667eea), Color(0xFF764ba2))
    val accentColor = Color(0xFF667eea)
    
    // 动画
    val infiniteTransition = rememberInfiniteTransition(label = "about")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 背景装饰
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
            ) {
                // ========== 头像与作者信息区 ==========
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 头像（带发光效果）
                        Box(
                            modifier = Modifier
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF667eea).copy(alpha = glowAlpha),
                                                Color.Transparent
                                            )
                                        ),
                                        radius = size.minDimension / 2 + 20.dp.toPx()
                                    )
                                }
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.avatar_shihao),
                                contentDescription = "作者头像",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 3.dp,
                                        brush = Brush.linearGradient(primaryGradient),
                                        shape = CircleShape
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // 作者名
                        Text(
                            text = "Shihao",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // 标语
                        Text(
                            text = "独立开发者 · AI 爱好者",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 应用信息
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "WebToApp",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.clickable {
                                    // 点击版本号检查更新
                                    scope.launch {
                                        isCheckingUpdate = true
                                        checkError = null
                                        val result = AppUpdateChecker.checkUpdate(currentVersionCode)
                                        isCheckingUpdate = false
                                        result.onSuccess { info ->
                                            updateInfo = info
                                            showUpdateDialog = true
                                        }.onFailure { e ->
                                            checkError = e.message
                                            Toast.makeText(context, "检查更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "v$currentVersionName",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    if (isCheckingUpdate) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // ========== 检查更新按钮 ==========
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable(enabled = !isCheckingUpdate && !isDownloading) {
                            scope.launch {
                                isCheckingUpdate = true
                                checkError = null
                                val result = AppUpdateChecker.checkUpdate(currentVersionCode)
                                isCheckingUpdate = false
                                result.onSuccess { info ->
                                    updateInfo = info
                                    showUpdateDialog = true
                                }.onFailure { e ->
                                    checkError = e.message
                                    Toast.makeText(context, "检查更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.SystemUpdate,
                                null,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "检查更新",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (isDownloading) "正在下载..." 
                                    else if (isCheckingUpdate) "正在检查..."
                                    else "当前版本 v$currentVersionName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        if (isCheckingUpdate || isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Outlined.ChevronRight,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // ========== 联系卡片区 ==========
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 简介卡片
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Favorite,
                                    null,
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "关于这个应用",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "WebToApp 是我独立开发的一款工具，可以将网站、图片、视频快速转换成独立的 Android 应用。\n\n" +
                                "如果你有任何问题、建议或想法，欢迎随时联系我！",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 24.sp
                            )
                        }
                    }
                    
                    // 官网与开源链接卡片
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Language,
                                    null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "官网与开源",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                "可在官网留言，作者会根据留言来更新\n作者的所有作品都会展示在官网",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 官网
                            LinkItem(
                                icon = Icons.Outlined.Public,
                                label = "🌐 官网",
                                value = "shiaho.sbs",
                                description = "路线1（主站）",
                                context = context
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            LinkItem(
                                icon = Icons.Outlined.Public,
                                label = "🌐 官网备用",
                                value = "shiaho.top",
                                description = "路线2（备用）",
                                context = context
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // GitHub
                            LinkItem(
                                icon = Icons.Outlined.Code,
                                label = "📦 GitHub",
                                value = "github.com/shiahonb777/web-to-app",
                                description = "开源代码",
                                context = context
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Gitee
                            LinkItem(
                                icon = Icons.Outlined.Code,
                                label = "📦 Gitee",
                                value = "gitee.com/ashiahonb777/web-to-app",
                                description = "国内直连",
                                context = context
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 联系方式卡片
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Group,
                                    null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "加入我们",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                "招 AI 编程队友！有好想法一起实现 🚀",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // QQ群
                            ContactItem(
                                icon = Icons.Outlined.Groups,
                                label = "QQ 群",
                                value = "1041130206",
                                description = "交流学习、更新消息、最新安装包",
                                onCopy = {
                                    copyToClipboard(context, "QQ群", "1041130206")
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 作者QQ
                            ContactItem(
                                icon = Icons.Outlined.Person,
                                label = "作者 QQ",
                                value = "2711674184",
                                description = "问题反馈、合作咨询",
                                onCopy = {
                                    copyToClipboard(context, "QQ", "2711674184")
                                }
                            )
                        }
                    }
                    
                    // ========== B站主页展示 ==========
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "📺",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "作者 B 站主页",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // B站主页图片
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.bilibili_homepage),
                                    contentDescription = "B站主页",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                "欢迎关注作者 B 站，获取最新动态和教程视频 💕",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // ========== 更新日志 - 树状展示 ==========
                    ChangelogTreeCard()
                    
                    // 底部留白
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // 底部信息
                    Text(
                        text = "Made with ❤️ by Shihao",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
    
    // ========== 更新对话框 ==========
    if (showUpdateDialog && updateInfo != null) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            currentVersion = currentVersionName,
            isDownloading = isDownloading,
            onDismiss = { showUpdateDialog = false },
            onDownload = {
                if (updateInfo!!.downloadUrl.isNotEmpty()) {
                    isDownloading = true
                    downloadId = AppUpdateChecker.downloadApk(
                        context,
                        updateInfo!!.downloadUrl,
                        updateInfo!!.versionName
                    )
                    if (downloadId == -1L) {
                        isDownloading = false
                        Toast.makeText(context, "下载启动失败", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "开始下载，请查看通知栏", Toast.LENGTH_SHORT).show()
                        showUpdateDialog = false
                    }
                } else {
                    Toast.makeText(context, "未找到下载链接", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

/**
 * 更新对话框
 */
@Composable
private fun UpdateDialog(
    updateInfo: AppUpdateChecker.UpdateInfo,
    currentVersion: String,
    isDownloading: Boolean,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (updateInfo.hasUpdate) Icons.Outlined.SystemUpdate else Icons.Outlined.CheckCircle,
                null,
                tint = if (updateInfo.hasUpdate) Color(0xFF2196F3) else Color(0xFF4CAF50),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                if (updateInfo.hasUpdate) "发现新版本" else "已是最新版本",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (updateInfo.hasUpdate) {
                    // 版本对比
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                "v$currentVersion",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Icon(
                            Icons.Outlined.ArrowForward,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF4CAF50)
                        ) {
                            Text(
                                updateInfo.versionName,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 更新说明
                    if (updateInfo.releaseNotes.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                updateInfo.releaseNotes,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "建议更新到最新版本以获得更好的体验",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        "当前版本 v$currentVersion 已是最新版本",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            if (updateInfo.hasUpdate) {
                Button(
                    onClick = onDownload,
                    enabled = !isDownloading
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isDownloading) "下载中..." else "立即更新")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("好的")
                }
            }
        },
        dismissButton = {
            if (updateInfo.hasUpdate) {
                TextButton(onClick = onDismiss) {
                    Text("稍后更新")
                }
            }
        }
    )
}

/**
 * 联系方式项
 */
@Composable
private fun ContactItem(
    icon: ImageVector,
    label: String,
    value: String,
    description: String,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            value,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            FilledTonalIconButton(onClick = onCopy) {
                Icon(
                    Icons.Default.ContentCopy,
                    "复制",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 版本区块
 */
@Composable
private fun VersionSection(
    version: String,
    isLatest: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                version,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (isLatest) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF4CAF50)
                ) {
                    Text(
                        "最新",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        content()
    }
}

/**
 * 更新项
 */
@Composable
private fun ChangeItem(emoji: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            emoji,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 复制到剪贴板
 */
private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "${label}已复制", Toast.LENGTH_SHORT).show()
}

/**
 * 链接项（可点击打开浏览器）
 */
@Composable
private fun LinkItem(
    icon: ImageVector,
    label: String,
    value: String,
    description: String,
    context: Context
) {
    val url = if (value.startsWith("http")) value else "https://$value"
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                }
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Icon(
                Icons.Outlined.OpenInNew,
                "打开",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 树状更新日志卡片 - 完整版
 */
@Composable
private fun ChangelogTreeCard() {
    // 版本展开状态
    var expandedVersions by remember { mutableStateOf(setOf("v1.5.0")) }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.History,
                    null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "更新日志",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // v1.5.0
            TreeVersionNode(
                version = "v1.5.0",
                isLatest = true,
                isExpanded = expandedVersions.contains("v1.5.0"),
                onToggle = { expandedVersions = if (expandedVersions.contains("v1.5.0")) expandedVersions - "v1.5.0" else expandedVersions + "v1.5.0" }
            ) {
                TreeCategory("✨ 新增功能", Color(0xFF4CAF50)) {
                    TreeItem("AI LRC 字幕生成：使用 AI 分析音频自动生成 LRC 格式歌词")
                    TreeSubItem("支持多种 AI 供应商：Google Gemini、OpenAI GPT-4o、智谱 GLM、火山引擎、MiniMax、OpenRouter 等")
                    TreeSubItem("时间轴精准对齐，支持中/英/日/韩多语言")
                    TreeItem("AI HTML 编程助手：使用 AI 辅助生成和修改 HTML 代码")
                    TreeSubItem("支持多种文本/图像生成模型")
                    TreeSubItem("会话管理、模板选择、样式定制")
                    TreeSubItem("代码块解析、实时预览")
                    TreeSubItem("Markdown 渲染：支持标题、列表、表格、代码块等格式")
                    TreeSubItem("Mermaid 图表：支持流程图、时序图、类图等图表渲染")
                    TreeItem("AI 设置界面：统一管理 API 密钥和模型")
                    TreeSubItem("支持添加多个 API Key，实时测试连接")
                    TreeSubItem("支持自定义 Base URL，模型列表从 API 实时获取")
                    TreeItem("HTML 应用：支持将 HTML/CSS/JS 项目转换为独立 Android 应用")
                    TreeItem("主题系统：全新的主题定制功能")
                    TreeSubItem("内置多款精美主题风格，支持深色模式")
                    TreeSubItem("可自定义动画效果开关和速度")
                    TreeSubItem("主题适配：导出 APK 的弹窗自动适配用户选择的主题")
                    TreeItem("背景音乐（BGM）：为应用添加背景音乐")
                    TreeSubItem("支持 LRC 歌词同步显示、循环播放")
                    TreeItem("横屏模式：WebView 应用支持强制横屏显示")
                    TreeItem("公告按钮：公告弹窗支持自定义按钮和跳转链接")
                }
                TreeCategory("🎨 优化改进", Color(0xFF2196F3)) {
                    TreeItem("主页 UI 整合 AI 编程、主题设置、AI 设置入口")
                    TreeItem("FAB 菜单新增 HTML 应用创建入口")
                }
                TreeCategory("🐛 Bug 修复", Color(0xFFF44336)) {
                    TreeItem("修复注入 JavaScript 脚本导致 APK 安装失败的问题")
                    TreeSubItem("根因：JSON 序列化未正确处理特殊字符")
                    TreeSubItem("方案：使用 Gson 库安全序列化脚本数据")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // v1.3.0
            TreeVersionNode(
                version = "v1.3.0",
                isLatest = false,
                isExpanded = expandedVersions.contains("v1.3.0"),
                onToggle = { expandedVersions = if (expandedVersions.contains("v1.3.0")) expandedVersions - "v1.3.0" else expandedVersions + "v1.3.0" }
            ) {
                TreeCategory("✨ 新增功能", Color(0xFF4CAF50)) {
                    TreeItem("媒体应用：支持图片/视频转换为独立 App")
                    TreeSubItem("图片转 App：全屏展示，支持铺满屏幕")
                    TreeSubItem("视频转 App：支持循环播放、音频开关、自动播放")
                    TreeSubItem("媒体应用支持导出为独立 APK")
                    TreeItem("用户脚本注入：支持自定义 JavaScript 脚本")
                    TreeSubItem("支持多个脚本管理（启用/禁用）")
                    TreeSubItem("支持页面加载前/后执行时机")
                    TreeSubItem("导出 APK 完整支持脚本注入")
                    TreeItem("启动画面（Splash Screen）")
                    TreeSubItem("支持图片/视频启动画面，内置视频裁剪器")
                    TreeSubItem("视频启动画面支持音频开关")
                    TreeSubItem("支持点击跳过、横屏显示、铺满屏幕")
                }
                TreeCategory("🎨 优化改进", Color(0xFF2196F3)) {
                    TreeItem("数据模型重构，支持视频裁剪配置持久化")
                    TreeItem("Shell 模式完整支持启动画面播放")
                    TreeItem("主页 FAB 改为展开菜单")
                }
                TreeCategory("🐛 Bug 修复", Color(0xFFF44336)) {
                    TreeItem("修复快捷方式图标错误使用启动图片的问题")
                    TreeItem("修复数据库 schema 不匹配导致的闪退问题")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // v1.2.x
            TreeVersionNode(
                version = "v1.2.x",
                isLatest = false,
                isExpanded = expandedVersions.contains("v1.2.x"),
                onToggle = { expandedVersions = if (expandedVersions.contains("v1.2.x")) expandedVersions - "v1.2.x" else expandedVersions + "v1.2.x" }
            ) {
                TreeCategory("✨ 新增功能", Color(0xFF4CAF50)) {
                    TreeItem("全屏模式：隐藏工具栏，无浏览器特征")
                }
                TreeCategory("🐛 Bug 修复", Color(0xFFF44336)) {
                    TreeItem("修复构建 APK 图标被放大裁剪的问题")
                    TreeSubItem("遵循 Android Adaptive Icon 规范处理图标")
                    TreeItem("修复 Release 版本自定义图标不生效的问题")
                    TreeItem("修复导出 APK 包名非法导致安装失败")
                    TreeItem("修复导出 APK 权限/Provider 冲突问题")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // v1.1.0
            TreeVersionNode(
                version = "v1.1.0",
                isLatest = false,
                isExpanded = expandedVersions.contains("v1.1.0"),
                onToggle = { expandedVersions = if (expandedVersions.contains("v1.1.0")) expandedVersions - "v1.1.0" else expandedVersions + "v1.1.0" }
            ) {
                TreeCategory("✨ 新增功能", Color(0xFF4CAF50)) {
                    TreeItem("一键构建独立 APK 安装包（无需 Android Studio）")
                    TreeItem("应用修改器：修改已安装应用的图标和名称")
                    TreeItem("克隆安装：生成独立包名的克隆应用")
                    TreeItem("访问电脑版：强制桌面模式加载网页")
                    TreeItem("启动自动请求运行时权限")
                    TreeItem("关于作者页面")
                }
                TreeCategory("🎨 优化改进", Color(0xFF2196F3)) {
                    TreeItem("全新 Material Design 3 界面")
                    TreeItem("优化图标替换逻辑（支持自适应图标）")
                    TreeItem("使用官方 apksig 签名库")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // v1.0.0
            TreeVersionNode(
                version = "v1.0.0",
                isLatest = false,
                isExpanded = expandedVersions.contains("v1.0.0"),
                onToggle = { expandedVersions = if (expandedVersions.contains("v1.0.0")) expandedVersions - "v1.0.0" else expandedVersions + "v1.0.0" }
            ) {
                TreeCategory("🎉 初始版本", Color(0xFF9C27B0)) {
                    TreeItem("支持 URL 转快捷方式基本功能")
                    TreeItem("支持激活码、公告、广告拦截")
                    TreeItem("支持项目模板导出")
                }
            }
        }
    }
}

/**
 * 树状版本节点
 */
@Composable
private fun TreeVersionNode(
    version: String,
    isLatest: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        // 版本标题行
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            shape = RoundedCornerShape(8.dp),
            color = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                    else Color.Transparent
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 展开/收起图标
                Icon(
                    if (isExpanded) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 版本号
                Text(
                    version,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // 最新标签
                if (isLatest) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF4CAF50)
                    ) {
                        Text(
                            "最新",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
        
        // 展开内容
        if (isExpanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * 树状分类
 */
@Composable
private fun TreeCategory(
    title: String,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 树枝线
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(16.dp)
                    .background(color.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        Column(modifier = Modifier.padding(start = 10.dp)) {
            content()
        }
    }
}

/**
 * 树状更新项
 */
@Composable
private fun TreeItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 树枝连接线
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .width(12.dp)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        // 节点圆点
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 树状子项（缩进更多）
 */
@Composable
private fun TreeSubItem(text: String) {
    Row(
        modifier = Modifier
            .padding(start = 20.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 小圆点
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(4.dp)
                .background(MaterialTheme.colorScheme.outline, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

