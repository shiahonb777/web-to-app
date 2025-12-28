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
                                        val result = AppUpdateChecker.checkUpdate(currentVersionName)
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
                                val result = AppUpdateChecker.checkUpdate(currentVersionName)
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
                
                // ========== 数据备份卡片 ==========
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    com.webtoapp.ui.components.DataBackupCard()
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
                                "欢迎加入交流群，一起学习进步 🚀",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // QQ群
                            ContactItemWithLink(
                                context = context,
                                icon = Icons.Outlined.Groups,
                                label = "QQ 群",
                                value = "1041130206",
                                description = "交流学习、更新消息、最新安装包",
                                link = "https://qun.qq.com/universal-share/share?ac=1&authKey=85Y3%2FckhO7c13%2F1%2F4kee5U7dg5dBPQ%2BDvKyGRVxiLVIgO8WxHdq%2BviYCtfWP4IsJ&busi_data=eyJncm91cENvZGUiOiIxMDQxMTMwMjA2IiwidG9rZW4iOiI1ZUhyRWF0bWhYVjN1T2p2VDJVODRPS3lKNzRCMjlyRmgrK3Robzg1cDhrbkF0bHlYR1d4eU43eW9QUTRGOUs4IiwidWluIjoiMjcxMTY3NDE4NCJ9&data=KG-7jSMVH0EM00Ekocv3-F15tvRkal3f4yQPgRmKS7dK0h13g8VPDADK2doELNhlgyPjrFJDFANTkzbibLL1ug&svctype=4&tempid=h5_group_info",
                                onCopy = {
                                    copyToClipboard(context, "QQ群", "1041130206")
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 作者QQ
                            ContactItemWithLink(
                                context = context,
                                icon = Icons.Outlined.Person,
                                label = "作者 QQ",
                                value = "2711674184",
                                description = "问题反馈、合作咨询",
                                link = "https://i.qq.com/2711674184",
                                onCopy = {
                                    copyToClipboard(context, "QQ", "2711674184")
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Bilibili
                            ContactItemWithLink(
                                context = context,
                                icon = Icons.Outlined.PlayCircle,
                                label = "Bilibili",
                                value = "关注我的B站",
                                description = "视频教程、开发日志",
                                link = "https://b23.tv/8mGDo2N",
                                onCopy = {
                                    copyToClipboard(context, "Bilibili链接", "https://b23.tv/8mGDo2N")
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Gitee 开源地址
                            ContactItemWithLink(
                                context = context,
                                icon = Icons.Outlined.Code,
                                label = "Gitee",
                                value = "开源仓库",
                                description = "国内直连，访问更快",
                                link = "https://gitee.com/ashiahonb777/web-to-app",
                                onCopy = {
                                    copyToClipboard(context, "Gitee地址", "https://gitee.com/ashiahonb777/web-to-app")
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // GitHub 开源地址
                            ContactItemWithLink(
                                context = context,
                                icon = Icons.Outlined.Code,
                                label = "GitHub",
                                value = "开源仓库",
                                description = "Star ⭐ 支持一下",
                                link = "https://github.com/shiahonb777/web-to-app",
                                onCopy = {
                                    copyToClipboard(context, "GitHub地址", "https://github.com/shiahonb777/web-to-app")
                                }
                            )
                        }
                    }
                    
                    // ========== 更新日志 ==========
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
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
                            
                            // v1.7.6
                            VersionSection(
                                version = "v1.7.6",
                                isLatest = true
                            ) {
                                ChangeItem("✨", "开机自启动和定时自启动功能")
                                ChangeItem("💾", "数据备份：一键导出/导入所有数据")
                                ChangeItem("✨", "全屏模式状态栏透明叠加显示")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.7.5
                            VersionSection(
                                version = "v1.7.5"
                            ) {
                                ChangeItem("✨", "全屏模式下可选择显示状态栏")
                                ChangeItem("🐛", "修复HTML项目长按文字无法复制")
                                ChangeItem("📱", "支持Android 6.0系统")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.7.4
                            VersionSection(
                                version = "v1.7.4"
                            ) {
                                ChangeItem("🐛", "修复HTML应用不显示状态栏的问题")
                                ChangeItem("🐛", "修复部分系统应用名称显示为空")
                                ChangeItem("🐛", "修复AI模块开发代码块内容叠加")
                                ChangeItem("🐛", "修复AI HTML编程工具调用失败")
                                ChangeItem("✨", "优化AI HTML编程提示词和模型兼容性")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.7.3
                            VersionSection(
                                version = "v1.7.3"
                            ) {
                                ChangeItem("✨", "状态栏颜色跟随主题：默认跟随主题色彩")
                                ChangeItem("✨", "支持自定义状态栏背景颜色")
                                ChangeItem("🐛", "修复状态栏文字看不清的问题")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.7.2
                            VersionSection(version = "v1.7.2") {
                                ChangeItem("🐛", "修复JS文件选择器兼容性问题")
                                ChangeItem("🐛", "修复视频全屏未自动横屏")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.7.1
                            VersionSection(version = "v1.7.1") {
                                ChangeItem("🐛", "修复小红书等网站图片长按无法保存")
                                ChangeItem("✨", "新增小红书图片下载器模块")
                                ChangeItem("🐛", "修复Blob格式文件导出失败")
                                ChangeItem("🐛", "修复HTML项目CSS/JS不生效")
                                ChangeItem("🐛", "修复任务列表显示双重名称")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.7.0
                            VersionSection(version = "v1.7.0") {
                                ChangeItem("🐛", "修复数十个已知问题")
                                ChangeItem("🤖", "优化AI Agent编程架构")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.6.0
                            VersionSection(version = "v1.6.0") {
                                ChangeItem("🧩", "扩展模块系统：类油猴脚本JS/CSS注入")
                                ChangeItem("🤖", "AI模块开发Agent：自然语言生成模块")
                                ChangeItem("🎨", "AI图标生成：AI生成应用图标")
                                ChangeItem("📚", "图标库：收藏管理生成的图标")
                                ChangeItem("🎵", "在线音乐搜索：在线搜索下载BGM")
                                ChangeItem("📢", "公告模板：10种精美公告弹窗模板")
                                ChangeItem("🌐", "网页自动翻译：网页内容自动翻译")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.5.0
                            VersionSection(version = "v1.5.0") {
                                ChangeItem("✨", "AI HTML编程：AI辅助生成代码")
                                ChangeItem("✨", "HTML应用：HTML/CSS/JS转独立App")
                                ChangeItem("✨", "主题系统：多款精美主题+深色模式")
                                ChangeItem("✨", "背景音乐：BGM+LRC歌词同步显示")
                                ChangeItem("✨", "AI设置：统一管理API密钥和模型")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.3.0
                            VersionSection(version = "v1.3.0") {
                                ChangeItem("✨", "媒体应用：图片/视频转独立App")
                                ChangeItem("✨", "用户脚本注入：自定义JS脚本")
                                ChangeItem("✨", "启动画面：图片/视频启动动画")
                                ChangeItem("✨", "视频裁剪：可视化选择视频片段")
                                ChangeItem("🐛", "修复快捷方式图标错误问题")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.2.x
                            VersionSection(version = "v1.2.x") {
                                ChangeItem("✨", "全屏模式：隐藏工具栏")
                                ChangeItem("🐛", "修复APK图标裁剪问题")
                                ChangeItem("🐛", "修复Release版图标不生效")
                                ChangeItem("🐛", "修复APK包名/权限冲突")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.1.0
                            VersionSection(version = "v1.1.0") {
                                ChangeItem("✨", "一键构建独立APK安装包")
                                ChangeItem("✨", "应用修改器：修改图标和名称")
                                ChangeItem("✨", "克隆安装：独立包名克隆应用")
                                ChangeItem("✨", "访问电脑版：强制桌面模式")
                                ChangeItem("🎨", "Material Design 3 界面")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.0.0
                            VersionSection(version = "v1.0.0") {
                                ChangeItem("🎉", "初始版本发布")
                                ChangeItem("✨", "URL转快捷方式基本功能")
                                ChangeItem("✨", "激活码/公告/广告拦截")
                                ChangeItem("✨", "项目模板导出")
                            }
                        }
                    }
                    
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
 * 带链接的联系方式项
 */
@Composable
private fun ContactItemWithLink(
    context: Context,
    icon: ImageVector,
    label: String,
    value: String,
    description: String,
    link: String,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // 打开链接
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(link))
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
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Row {
                // 复制按钮
                FilledTonalIconButton(onClick = onCopy) {
                    Icon(
                        Icons.Default.ContentCopy,
                        "复制",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                // 打开链接按钮
                FilledTonalIconButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(link))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        Icons.Outlined.OpenInNew,
                        "打开",
                        modifier = Modifier.size(18.dp)
                    )
                }
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

