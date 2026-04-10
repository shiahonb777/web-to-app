package com.webtoapp.ui.screens

import android.app.DownloadManager
import com.webtoapp.ui.components.PremiumButton
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.webtoapp.R
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.theme.AppColors
import com.webtoapp.ui.theme.LocalAppTheme
import com.webtoapp.util.AppUpdateChecker
import com.webtoapp.util.openUrl
import kotlinx.coroutines.launch
import com.webtoapp.ui.components.ThemedBackgroundBox

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
    
    // Check更新状态
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdateChecker.UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var checkError by remember { mutableStateOf<String?>(null) }
    
    // Download状态
    var isDownloading by remember { mutableStateOf(false) }
    var downloadId by remember { mutableLongStateOf(-1L) }
    
    // Listen下载完成
    DisposableEffect(downloadId) {
        if (downloadId == -1L) return@DisposableEffect onDispose {}
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (id == downloadId) {
                    isDownloading = false
                Toast.makeText(context, Strings.downloadComplete, Toast.LENGTH_SHORT).show()
                    AppUpdateChecker.installApk(context, downloadId)
                }
            }
        }
        
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }
    
    // Theme色 - 使用当前主题的渐变色
    val theme = LocalAppTheme.current
    val primaryGradient = theme.gradients.primary.ifEmpty { listOf(Color(0xFF667eea), Color(0xFF764ba2)) }
    val accentColor = primaryGradient.first()
    
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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.about, fontWeight = FontWeight.Bold) },
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
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 背景装饰（减小高度）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // ========== 头像与作者信息区 ==========
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
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
                                        radius = size.minDimension / 2 + 14.dp.toPx()
                                    )
                                }
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.avatar_shiaho),
                                contentDescription = Strings.authorAvatar,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.5.dp,
                                        brush = Brush.linearGradient(primaryGradient),
                                        shape = CircleShape
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 作者名
                        Text(
                            text = "Shiaho",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // 标语
                        Text(
                            text = Strings.authorTagline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // App信息
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
                                        val result = AppUpdateChecker.checkUpdate(currentVersionName, currentVersionCode)
                                        isCheckingUpdate = false
                                        result.onSuccess { info ->
                                            updateInfo = info
                                            showUpdateDialog = true
                                        }.onFailure { e ->
                                            checkError = e.message
                                            Toast.makeText(context, "${Strings.checkUpdateFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
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
                EnhancedElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable(enabled = !isCheckingUpdate && !isDownloading) {
                            scope.launch {
                                isCheckingUpdate = true
                                checkError = null
                                val result = AppUpdateChecker.checkUpdate(currentVersionName, currentVersionCode)
                                isCheckingUpdate = false
                                result.onSuccess { info ->
                                    updateInfo = info
                                    showUpdateDialog = true
                                }.onFailure { e ->
                                    checkError = e.message
                                    Toast.makeText(context, "${Strings.checkUpdateFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
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
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2196F3).copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_about_update),
                                    null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    Strings.checkUpdate,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (isDownloading) Strings.downloading 
                                    else if (isCheckingUpdate) Strings.checking
                                    else "${Strings.currentVersion} v$currentVersionName",
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // ========== 自动检查更新开关 ==========
                var autoCheckEnabled by remember {
                    mutableStateOf(AppUpdateChecker.isAutoCheckEnabled(context))
                }
                EnhancedElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Autorenew,
                                    null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    Strings.autoCheckUpdate,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    Strings.autoCheckUpdateDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = autoCheckEnabled,
                            onCheckedChange = { enabled ->
                                autoCheckEnabled = enabled
                                AppUpdateChecker.setAutoCheckEnabled(context, enabled)
                            }
                        )
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
                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFE91E63).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_about_heart),
                                        null,
                                        tint = Color(0xFFE91E63),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    Strings.aboutThisApp,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                Strings.aboutAppDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 24.sp
                            )
                        }
                    }
                    
                    // ========== 社交媒体快捷入口 ==========
                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(accentColor.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Share,
                                        null,
                                        tint = accentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    Strings.socialMedia,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 社交媒体网格 - 第一行
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // X (Twitter)
                                SocialMediaButton(
                                    modifier = Modifier.weight(weight = 1f, fill = true),
                                    label = "X",
                                    subtitle = "@shiaho777",
                                    backgroundColor = Color(0xFF000000),
                                    iconRes = R.drawable.ic_social_x,
                                    link = "https://x.com/@shiaho777",
                                    context = context
                                )
                                
                                // Telegram
                                SocialMediaButton(
                                    modifier = Modifier.weight(weight = 1f, fill = true),
                                    label = "Telegram",
                                    subtitle = Strings.communityGroup,
                                    backgroundColor = Color(0xFF0088CC),
                                    iconRes = R.drawable.ic_social_telegram,
                                    link = "https://t.me/webtoapp777",
                                    context = context
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 社交媒体网格 - 第二行
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // GitHub
                                SocialMediaButton(
                                    modifier = Modifier.weight(weight = 1f, fill = true),
                                    label = "GitHub",
                                    subtitle = Strings.openSourceRepository,
                                    backgroundColor = Color(0xFF24292E),
                                    iconRes = R.drawable.ic_social_github,
                                    link = "https://github.com/shiahonb777/web-to-app",
                                    context = context
                                )
                                
                                // Bilibili
                                SocialMediaButton(
                                    modifier = Modifier.weight(weight = 1f, fill = true),
                                    label = "Bilibili",
                                    subtitle = Strings.videoTutorialLabel,
                                    backgroundColor = Color(0xFFFB7299),
                                    iconRes = R.drawable.ic_social_bilibili,
                                    link = "https://b23.tv/8mGDo2N",
                                    context = context
                                )
                            }
                        }
                    }
                    
                    // ========== 交流群卡片 ==========
                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF12B7F5).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Groups,
                                        null,
                                        tint = Color(0xFF12B7F5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    Strings.joinCommunityGroup,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                Strings.communityGroupDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // QQ群
                            ContactCardCompact(
                                context = context,
                                iconText = "Q",
                                iconColor = Color.White,
                                iconBgColor = Color(0xFF12B7F5),
                                label = Strings.qqGroupLabel,
                                value = "1041130206",
                                description = Strings.exchangeLearningUpdates,
                                link = "https://qun.qq.com/universal-share/share?ac=1&authKey=85Y3%2FckhO7c13%2F1%2F4kee5U7dg5dBPQ%2BDvKyGRVxiLVIgO8WxHdq%2BviYCtfWP4IsJ&busi_data=eyJncm91cENvZGUiOiIxMDQxMTMwMjA2IiwidG9rZW4iOiI1ZUhyRWF0bWhYVjN1T2p2VDJVODRPS3lKNzRCMjlyRmgrK3Robzg1cDhrbkF0bHlYR1d4eU43eW9QUTRGOUs4IiwidWluIjoiMjcxMTY3NDE4NCJ9&data=KG-7jSMVH0EM00Ekocv3-F15tvRkal3f4yQPwRmKS7dK0h13g8VPDADK2doELNhlgyPjrFJDFANTkzbibLL1ug&svctype=4&tempid=h5_group_info",
                                copyValue = "1041130206"
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // TG群
                            ContactCardCompact(
                                context = context,
                                iconText = "TG",
                                iconColor = Color.White,
                                iconBgColor = Color(0xFF0088CC),
                                label = Strings.telegramGroupLabel,
                                value = "webtoapp777",
                                description = Strings.internationalUserGroup,
                                link = "https://t.me/webtoapp777",
                                copyValue = "https://t.me/webtoapp777"
                            )
                        }
                    }
                    
                    // ========== 联系作者卡片 ==========
                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFF6B6B).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Person,
                                        null,
                                        tint = Color(0xFFFF6B6B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    Strings.contactAuthor,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                Strings.contactAuthorDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 作者QQ
                            ContactCardCompact(
                                context = context,
                                iconText = "Q",
                                iconColor = Color.White,
                                iconBgColor = Color(0xFF12B7F5),
                                label = "QQ",
                                value = "2711674184",
                                description = Strings.feedbackConsultation,
                                link = "https://i.qq.com/2711674184",
                                copyValue = "2711674184"
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // QQ邮箱
                            ContactCardCompact(
                                context = context,
                                iconText = "✉",
                                iconColor = Color.White,
                                iconBgColor = Color(0xFFFF6B6B),
                                label = "QQ Email",
                                value = "2711674184@qq.com",
                                description = Strings.emailContact,
                                link = "mailto:2711674184@qq.com",
                                copyValue = "2711674184@qq.com"
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Gmail
                            ContactCardCompact(
                                context = context,
                                iconText = "G",
                                iconColor = Color.White,
                                iconBgColor = Color(0xFFEA4335),
                                label = "Gmail",
                                value = "weuwo479@gmail.com",
                                description = Strings.internationalEmail,
                                link = "mailto:weuwo479@gmail.com",
                                copyValue = "weuwo479@gmail.com"
                            )
                        }
                    }
                    
                    // ========== 开源仓库卡片 ==========
                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppColors.Success.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Code,
                                        null,
                                        tint = AppColors.Success,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    Strings.openSourceRepo,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                Strings.welcomeStarSupport,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // GitHub
                            ContactCardCompact(
                                context = context,
                                iconText = "⌘",
                                iconColor = Color.White,
                                iconBgColor = Color(0xFF24292E),
                                label = "GitHub",
                                value = "shiahonb777/web-to-app",
                                description = Strings.internationalAccess,
                                link = "https://github.com/shiahonb777/web-to-app",
                                copyValue = "https://github.com/shiahonb777/web-to-app"
                            )
                        }
                    }
                    
                    // ========== 更新日志 ==========
                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppColors.Success.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.History,
                                        null,
                                        tint = AppColors.Success,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    Strings.changelog,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // v1.9.5
                            VersionSection(
                                version = "v1.9.5",
                                isLatest = true
                            ) {
                                ChangeItem(emoji = "feature", text = Strings.cookiesPersistenceFeature)
                                ChangeItem(emoji = "feature", text = Strings.multiApiKeyManagement)
                                ChangeItem(emoji = "feature", text = Strings.modelNameSearchFeature)
                                ChangeItem(emoji = "feature", text = Strings.hideUrlPreviewFeature)
                                ChangeItem(emoji = "feature", text = Strings.popupBlockerFeature)
                                ChangeItem(emoji = "improve", text = Strings.optimizeCustomApiEndpoint)
                                ChangeItem(emoji = "improve", text = Strings.optimizeModelNameDisplay)
                                ChangeItem(emoji = "improve", text = Strings.optimizeMultiLanguageAdaptation)
                                ChangeItem(emoji = "bugfix", text = Strings.fixGalleryBuildPath)
                                ChangeItem(emoji = "bugfix", text = Strings.fixMicrophonePermission)
                                ChangeItem(emoji = "bugfix", text = Strings.fixZoomPropertyNotWorking)
                                ChangeItem(emoji = "bugfix", text = Strings.fixActivationCodeLanguage)
                                ChangeItem(emoji = "bugfix", text = Strings.fixFrontendGalleryFilename)
                                ChangeItem(emoji = "bugfix", text = Strings.fixCoreConfigEditAppType)
                                ChangeItem(emoji = "bugfix", text = Strings.fixKeyboardInitIssue)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.9.0
                            VersionSection(
                                version = "v1.9.0"
                            ) {
                                ChangeItem(emoji = "feature", text = Strings.browserEngineFeature)
                                ChangeItem(emoji = "feature", text = Strings.browserSpoofingFeature)
                                ChangeItem(emoji = "feature", text = Strings.hostsBlockFeature)
                                ChangeItem(emoji = "feature", text = Strings.longPressMenuFeature)
                                ChangeItem(emoji = "feature", text = Strings.apkArchitectureFeature)
                                ChangeItem(emoji = "feature", text = Strings.mediaGalleryFeature)
                                ChangeItem(emoji = "improve", text = Strings.optimizeExtensionModule)
                                ChangeItem(emoji = "improve", text = Strings.optimizeEnglishArabicTranslation)
                                ChangeItem(emoji = "improve", text = Strings.optimizeThemeInteraction)
                                ChangeItem(emoji = "improve", text = Strings.optimizeApiConfigTest)
                                ChangeItem(emoji = "bugfix", text = Strings.fixAppNameSpaces)
                                ChangeItem(emoji = "bugfix", text = Strings.fixAnnouncementJump)
                                ChangeItem(emoji = "bugfix", text = Strings.fixExternalBrowserCrash)
                                ChangeItem(emoji = "bugfix", text = Strings.fixDownloadError)
                                ChangeItem(emoji = "bugfix", text = Strings.fixModuleEditCrash)
                                ChangeItem(emoji = "bugfix", text = Strings.fixAiImageInvalid)
                                ChangeItem(emoji = "bugfix", text = Strings.fixDownloaderPlayerCooperation)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.8.5
                            VersionSection(
                                version = "v1.8.5"
                            ) {
                                ChangeItem(emoji = "feature", text = Strings.appCategoryFeature)
                                ChangeItem(emoji = "feature", text = Strings.faviconFetchFeature)
                                ChangeItem(emoji = "feature", text = Strings.randomAppNameFeature)
                                ChangeItem(emoji = "feature", text = Strings.multiAppIconFeature)
                                ChangeItem(emoji = "improve", text = Strings.optimizeDataBackup)
                                ChangeItem(emoji = "improve", text = Strings.optimizeBlackTech)
                                ChangeItem(emoji = "bugfix", text = Strings.fixElementBlocker)
                                ChangeItem(emoji = "bugfix", text = Strings.fixBackgroundRunCrash)
                                ChangeItem(emoji = "bugfix", text = Strings.fixI18nStringAdaptation)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.8.0
                            VersionSection(
                                version = "v1.8.0"
                            ) {
                                ChangeItem(emoji = "i18n", text = Strings.multiLanguageSupport)
                                ChangeItem(emoji = "upload", text = Strings.shareApkFeature)
                                ChangeItem(emoji = "module", text = Strings.elementBlockerModule)
                                ChangeItem(emoji = "lock", text = Strings.forcedRunFeature)
                                ChangeItem(emoji = "linux", text = Strings.linuxOneClickBuild)
                                ChangeItem(emoji = "framework", text = Strings.frontendFrameworkToApk)
                                ChangeItem(emoji = "design", text = Strings.optimizeThemeFeature)
                                ChangeItem(emoji = "feature", text = Strings.optimizeAboutPageUi)
                                ChangeItem(emoji = "bugfix", text = Strings.fixFullscreenStatusBarIssue)
                                ChangeItem(emoji = "bugfix", text = Strings.fixDeviceCrashIssue)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.8.0
                            VersionSection(
                                version = "v1.8.0"
                            ) {
                                ChangeItem(emoji = "web", text = Strings.isolatedBrowserEnvironment)
                                ChangeItem(emoji = "play", text = Strings.backgroundRunFeature)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.7.7
                            VersionSection(
                                version = "v1.7.7"
                            ) {
                                ChangeItem(emoji = "design", text = Strings.statusBarStyleConfig)
                                ChangeItem(emoji = "security", text = Strings.apkEncryptionProtection)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.7.6
                            VersionSection(
                                version = "v1.7.6"
                            ) {
                                ChangeItem(emoji = "feature", text = Strings.bootAutoStartAndScheduled)
                                ChangeItem(emoji = "save", text = Strings.dataBackupExportImport)
                                ChangeItem(emoji = "feature", text = Strings.fullscreenStatusBarOverlay)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.7.5
                            VersionSection(
                                version = "v1.7.5"
                            ) {
                                ChangeItem(emoji = "feature", text = Strings.fullscreenShowStatusBar)
                                ChangeItem(emoji = "bugfix", text = Strings.fixHtmlLongPressCopy)
                                ChangeItem(emoji = "mobile", text = Strings.supportAndroid6)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.7.4
                            VersionSection(
                                version = "v1.7.4"
                            ) {
                                ChangeItem(emoji = "bugfix", text = Strings.fixHtmlStatusBar)
                                ChangeItem(emoji = "bugfix", text = Strings.fixEmptyAppName)
                                ChangeItem(emoji = "bugfix", text = Strings.fixAiModuleCodeOverlay)
                                ChangeItem(emoji = "bugfix", text = Strings.fixAiHtmlToolCallFailed)
                                ChangeItem(emoji = "feature", text = Strings.optimizeAiHtmlPrompt)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.7.3
                            VersionSection(
                                version = "v1.7.3"
                            ) {
                                ChangeItem(emoji = "feature", text = Strings.statusBarFollowTheme)
                                ChangeItem(emoji = "feature", text = Strings.customStatusBarBgColor)
                                ChangeItem(emoji = "bugfix", text = Strings.fixStatusBarTextVisibility)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.7.2
                            VersionSection(version = "v1.7.2") {
                                ChangeItem(emoji = "bugfix", text = Strings.fixJsFileSelectorCompat)
                                ChangeItem(emoji = "bugfix", text = Strings.fixVideoFullscreenRotation)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.7.1
                            VersionSection(version = "v1.7.1") {
                                ChangeItem(emoji = "bugfix", text = Strings.fixXhsImageSave)
                                ChangeItem(emoji = "feature", text = Strings.newXhsImageDownloader)
                                ChangeItem(emoji = "bugfix", text = Strings.fixBlobExportFailed)
                                ChangeItem(emoji = "bugfix", text = Strings.fixHtmlCssJsNotWorking)
                                ChangeItem(emoji = "bugfix", text = Strings.fixTaskListDuplicateName)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.7.0
                            VersionSection(version = "v1.7.0") {
                                ChangeItem(emoji = "bugfix", text = Strings.fixKnownIssues)
                                ChangeItem(emoji = "ai", text = Strings.optimizeAiAgentArch)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.6.0
                            VersionSection(version = "v1.6.0") {
                                ChangeItem(emoji = "extension", text = Strings.extensionModuleSystem)
                                ChangeItem(emoji = "ai", text = Strings.aiModuleDeveloperAgent)
                                ChangeItem(emoji = "design", text = Strings.aiIconGeneration)
                                ChangeItem(emoji = "library", text = Strings.iconLibrary)
                                ChangeItem(emoji = "music", text = Strings.onlineMusicSearch)
                                ChangeItem(emoji = "announce", text = Strings.announcementTemplates)
                                ChangeItem(emoji = "web", text = Strings.webAutoTranslate)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.5.0
                            VersionSection(version = "v1.5.0") {
                                ChangeItem(emoji = "feature", text = Strings.aiHtmlCodingFeature)
                                ChangeItem(emoji = "feature", text = Strings.htmlAppFeature)
                                ChangeItem(emoji = "feature", text = Strings.themeSystemFeature)
                                ChangeItem(emoji = "feature", text = Strings.bgmLrcFeature)
                                ChangeItem(emoji = "feature", text = Strings.aiSettingsFeature)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.3.0
                            VersionSection(version = "v1.3.0") {
                                ChangeItem(emoji = "feature", text = Strings.mediaAppFeature)
                                ChangeItem(emoji = "feature", text = Strings.userScriptInjection)
                                ChangeItem(emoji = "feature", text = Strings.splashScreenFeature)
                                ChangeItem(emoji = "feature", text = Strings.videoTrimFeature)
                                ChangeItem(emoji = "bugfix", text = Strings.fixShortcutIconError)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.2.x
                            VersionSection(version = "v1.2.x") {
                                ChangeItem(emoji = "feature", text = Strings.fullscreenModeFeature)
                                ChangeItem(emoji = "bugfix", text = Strings.fixApkIconCrop)
                                ChangeItem(emoji = "bugfix", text = Strings.fixReleaseIconNotWorking)
                                ChangeItem(emoji = "bugfix", text = Strings.fixApkPackageConflict)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.1.0
                            VersionSection(version = "v1.1.0") {
                                ChangeItem(emoji = "feature", text = Strings.oneClickBuildApk)
                                ChangeItem(emoji = "feature", text = Strings.appModifierFeature)
                                ChangeItem(emoji = "feature", text = Strings.cloneInstallFeature)
                                ChangeItem(emoji = "feature", text = Strings.desktopModeFeature)
                                ChangeItem(emoji = "design", text = Strings.materialDesign3UI)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // v1.0.0
                            VersionSection(version = "v1.0.0") {
                                ChangeItem(emoji = "celebrate", text = Strings.initialVersionRelease)
                                ChangeItem(emoji = "feature", text = Strings.urlToShortcutBasic)
                                ChangeItem(emoji = "feature", text = Strings.activationCodeAnnouncementAdBlock)
                                ChangeItem(emoji = "feature", text = Strings.projectTemplateExport)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // ========== 法律免责声明 ==========
                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFF5722).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Gavel,
                                        null,
                                        tint = Color(0xFFFF5722),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    Strings.legalDisclaimer,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 重要提示框
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Outlined.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        Strings.disclaimerWarningText,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 1. 软件性质声明
                            LegalSection(
                                title = Strings.legalDisclaimerTitle1,
                                content = "本软件为开源技术研究与教育演示工具，所有功能均基于Android系统公开API实现，" +
                                        "旨在展示移动应用开发技术。本软件不鼓励、不支持任何非法用途。"
                            )
                            
                            // 2. 用户责任
                            LegalSection(
                                title = Strings.legalDisclaimerTitle2,
                                content = "用户应确保在合法、正当的场景下使用本软件，包括但不限于：\n" +
                                        "• 自我管理：用于个人专注力训练、学习时间管理\n" +
                                        "• 企业展示：用于展会、商场等场景的展示终端\n" +
                                        "• 家长监护：在未成年人知情同意下的合理使用\n" +
                                        "• 教育研究：用于技术学习和安全研究\n\n" +
                                        "严禁将本软件用于任何侵犯他人人身自由、隐私权、财产权等合法权益的行为。"
                            )
                            
                            // 3. 特殊功能声明
                            LegalSection(
                                title = Strings.legalDisclaimerTitle3,
                                content = "本软件包含的「强制运行」及相关硬件控制功能（以下简称「高级功能」）属于技术演示性质：\n\n" +
                                        "1. 【知情同意原则】高级功能仅应在设备所有者或使用者完全知情并明确同意的情况下启用\n\n" +
                                        "2. 【自主控制原则】所有功能均提供紧急退出机制，用户可通过密码随时终止\n\n" +
                                        "3. 【技术中立原则】功能本身不具有违法性，其合法性取决于使用者的具体使用方式和目的\n\n" +
                                        "4. 【风险自担原则】启用高级功能可能造成设备发热、电池消耗加快等情况，用户需自行承担相关风险"
                            )
                            
                            // 4. 免责条款
                            LegalSection(
                                title = Strings.legalDisclaimerTitle4,
                                content = "1. 本软件按「现状」提供，开发者不对软件的适用性、可靠性、安全性作任何明示或暗示的保证\n\n" +
                                        "2. 用户因违反法律法规或本声明使用本软件所产生的一切法律责任，由用户自行承担，与开发者无关\n\n" +
                                        "3. 开发者不对因使用本软件导致的任何直接、间接、偶然、特殊或惩罚性损害承担责任\n\n" +
                                        "4. 任何第三方利用本软件源代码进行的修改、分发行为，其法律责任由该第三方自行承担"
                            )
                            
                            // 5. 合规使用
                            LegalSection(
                                title = Strings.legalDisclaimerTitle5,
                                content = "To ensure lawful and compliant use, it is recommended that users:\n" +
                                        "• Obtain written or electronic consent from the actual device user before use\n" +
                                        "• Establish appropriate usage policies and management procedures in enterprise scenarios\n" +
                                        "• Ensure compliance with relevant education laws and regulations in educational scenarios\n" +
                                        "• Regularly review and comply with the latest applicable local laws and regulations"
                            )
                            
                            // 6. 版权与开源
                            LegalSection(
                                title = Strings.legalDisclaimerTitle6,
                                content = "This software is released under the MIT License. Users may freely use, modify, and distribute it, provided the original copyright notice is retained." +
                                        "Any legal responsibilities arising from derivative works based on this software are borne solely by the developer of such derivative works."
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 最终声明
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        Strings.finalUserAgreementConfirmation,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "By continuing to use this software, you acknowledge that:\n" +
                                        "• You are at least 18 years old or have obtained consent from your legal guardian\n" +
                                        "• You have fully read and understood all terms above\n" +
                                        "• You agree to comply with all terms of use and applicable local laws and regulations\n" +
                                        "• You voluntarily assume all risks and liabilities that may arise from using this software",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                "This statement takes effect on the date of publication. The developer reserves the right to modify this statement at any time.\nLast updated: Jan 2026",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    // 底部留白
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // 底部信息
                    Text(
                        text = Strings.madeWithLove,
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
                        Toast.makeText(context, Strings.downloadStartFailed, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, Strings.startDownloadCheckNotification, Toast.LENGTH_SHORT).show()
                        showUpdateDialog = false
                    }
                } else {
                    Toast.makeText(context, Strings.downloadLinkNotFound, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
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
                tint = if (updateInfo.hasUpdate) Color(0xFF2196F3) else AppColors.Success,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                if (updateInfo.hasUpdate) Strings.newVersionFound else Strings.latestVersion,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (updateInfo.hasUpdate) {
                    // Version对比
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
                            color = AppColors.Success
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
                    
                    // Update说明
                    if (updateInfo.releaseNotes.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
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
                        Strings.updateRecommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        Strings.currentVersionIs.replace("%s", currentVersion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            if (updateInfo.hasUpdate) {
                PremiumButton(
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
                    Text(if (isDownloading) Strings.downloading else Strings.updateNow)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(Strings.btnOk)
                }
            }
        },
        dismissButton = {
            if (updateInfo.hasUpdate) {
                TextButton(onClick = onDismiss) {
                    Text(Strings.updateLater)
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
        color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
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
                modifier = Modifier.weight(weight = 1f, fill = true)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
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
                    Strings.copy,
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
                    context.openUrl(link)
                } catch (e: Exception) {
                    Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                }
            },
        shape = RoundedCornerShape(12.dp),
        color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
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
                modifier = Modifier.weight(weight = 1f, fill = true)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
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
                // Copy按钮
                FilledTonalIconButton(onClick = onCopy) {
                    Icon(
                        Icons.Default.ContentCopy,
                        Strings.copy,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                // 打开链接按钮
                FilledTonalIconButton(
                    onClick = {
                        try {
                            context.openUrl(link)
                        } catch (e: Exception) {
                            Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        Icons.Outlined.OpenInNew,
                        Strings.openAction,
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
                    color = AppColors.Success
                ) {
                    Text(
                        Strings.latestTag,
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
 * 更新项 — 接受 emoji 字符串，内部映射为 Material Icon
 */
@Composable
private fun ChangeItem(emoji: String, text: String) {
    val iconMap = mapOf(
        "feature" to Icons.Outlined.AutoAwesome,
        "improve" to Icons.Outlined.FitnessCenter,
        "bugfix" to Icons.Outlined.BugReport,
        "design" to Icons.Outlined.Palette,
        "security" to Icons.Outlined.Lock,
        "save" to Icons.Outlined.Save,
        "mobile" to Icons.Outlined.PhoneAndroid,
        "celebrate" to Icons.Outlined.Celebration,
        "i18n" to Icons.Outlined.Language,
        "upload" to Icons.Outlined.Upload,
        "module" to Icons.Outlined.ViewModule,
        "lock" to Icons.Outlined.Lock,
        "linux" to Icons.Outlined.Terminal,
        "framework" to Icons.Outlined.Code,
        "web" to Icons.Outlined.Language,
        "play" to Icons.Filled.PlayArrow,
        "extension" to Icons.Outlined.Extension,
        "ai" to Icons.Outlined.SmartToy,
        "library" to Icons.Outlined.MenuBook,
        "music" to Icons.Outlined.MusicNote,
        "announce" to Icons.Outlined.Campaign,
    )
    val icon = iconMap[emoji] ?: Icons.Outlined.Circle
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
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
    Toast.makeText(context, "${label} ${Strings.copied}", Toast.LENGTH_SHORT).show()
}

/**
 * 社交媒体按钮 - 带品牌色彩的卡片式按钮（文字图标版本）
 */
@Composable
private fun SocialMediaButton(
    modifier: Modifier = Modifier,
    label: String,
    subtitle: String,
    backgroundColor: Color,
    iconText: String,
    link: String,
    context: Context
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clickable {
                try {
                    context.openUrl(link)
                } catch (e: Exception) {
                    Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                }
            },
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * 社交媒体按钮 - 带品牌色彩的卡片式按钮（AI图标版本）
 */
@Composable
private fun SocialMediaButton(
    modifier: Modifier = Modifier,
    label: String,
    subtitle: String,
    backgroundColor: Color,
    iconRes: Int,
    link: String,
    context: Context
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clickable {
                try {
                    context.openUrl(link)
                } catch (e: Exception) {
                    Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                }
            },
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * 紧凑型联系方式卡片 - 带自定义图标
 */
@Composable
private fun ContactCardCompact(
    context: Context,
    iconText: String,
    iconColor: Color,
    iconBgColor: Color,
    label: String,
    value: String,
    description: String,
    link: String,
    copyValue: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    context.openUrl(link)
                } catch (e: Exception) {
                    Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                }
            },
        shape = RoundedCornerShape(14.dp),
        color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
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
                modifier = Modifier.weight(weight = 1f, fill = true)
            ) {
                // Custom图标
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = iconBgColor,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Copy按钮
                FilledTonalIconButton(
                    onClick = { copyToClipboard(context, label, copyValue) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        Strings.copy,
                        modifier = Modifier.size(16.dp)
                    )
                }
                // 打开链接按钮
                FilledTonalIconButton(
                    onClick = {
                        try {
                            context.openUrl(link)
                        } catch (e: Exception) {
                            Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.OpenInNew,
                        Strings.openAction,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 法律声明段落组件
 */
@Composable
private fun LegalSection(
    title: String,
    content: String
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}
