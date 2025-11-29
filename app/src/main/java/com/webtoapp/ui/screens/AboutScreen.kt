package com.webtoapp.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 关于作者页面 - 像素/动漫风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // 定义一些“动漫/像素”风格的颜色
    val primaryColor = Color(0xFF6200EE)
    val accentColor = Color(0xFF03DAC5)
    val pixelBgColor = Color(0xFFF0F4F8)
    val cardBgColor = Color.White
    val borderColor = Color(0xFF333333)
    
    // 渐变背景
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFFE0F7FA), Color(0xFFF3E5F5))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "关于作者",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color(0xFF333333))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent // 使用 Box 的渐变背景
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 头像与标题区域（模拟像素风卡片）
                PixelCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = borderColor
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // 模拟头像
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .border(3.dp, borderColor, CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD180)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "WebToApp",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = primaryColor
                        )
                        
                        Text(
                            text = "v1.1.0",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .background(accentColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "本应用由作者（shihao）独立开发\n有任何问题都可以找我",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF555555),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // 2. 作者联系方式 & 招募
                PixelCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = borderColor,
                    backgroundColor = Color(0xFFFFF9C4) // 淡黄色背景
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Group, null, tint = Color(0xFFFF6F00))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "加入我们",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "招 AI 编程队友！\n如果你有好的想法，欢迎和我一起实现！",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = borderColor.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // QQ群
                        Text(
                            "作者每天都会在群里互动，交流学习，发布更新消息、体验版和最新安装包。",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF444444)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // QQ号复制
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("作者 QQ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    "2711674184",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("QQ", "2711674184")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "QQ号已复制", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, "复制", tint = primaryColor)
                            }
                        }
                    }
                }

                // 3. 树状更新日志
                PixelCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = borderColor
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.NewReleases, null, tint = Color(0xFFD50000))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "更新日志 v1.1",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 树状结构绘制
                        ChangeLogTreeItem(Icons.Outlined.BugReport, "修复安装包签名冲突问题")
                        ChangeLogTreeItem(Icons.Outlined.Palette, "优化图标替换逻辑")
                        ChangeLogTreeItem(Icons.Outlined.Star, "新增应用启动自动权限请求")
                        ChangeLogTreeItem(Icons.Outlined.Info, "修复主页点击空白问题")
                        ChangeLogTreeItem(Icons.Outlined.Code, "增加访问电脑版功能")
                        ChangeLogTreeItem(Icons.Outlined.Group, "增加关于作者页面")
                        ChangeLogTreeItem(Icons.Outlined.Palette, "UI 细节优化")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * 自定义像素风卡片容器
 */
@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Black,
    backgroundColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        content()
    }
}

/**
 * 更新日志树状条目
 */
@Composable
fun ChangeLogTreeItem(
    icon: ImageVector,
    text: String,
    isLast: Boolean = false
) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        // 左侧线条
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(Color(0xFFE0E0E0))
            )
        }
        
        // 内容
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 连接点
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .offset(x = (-23).dp) // 调整位置使其在线条上
                    .background(Color.White, CircleShape)
                    .border(2.dp, Color(0xFF6200EE), CircleShape)
            )
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = (-12).dp),
                tint = Color(0xFF6200EE)
            )
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
