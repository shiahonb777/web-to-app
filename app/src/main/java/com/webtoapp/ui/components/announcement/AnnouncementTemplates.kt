package com.webtoapp.ui.components.announcement

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.Announcement
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton
import com.webtoapp.data.model.AnnouncementTemplateType

/**
 * 公告模板类型
 */
enum class AnnouncementTemplate(
    val type: AnnouncementTemplateType,
    val displayName: String,
    val description: String,
    val icon: ImageVector
) {
    MINIMAL(AnnouncementTemplateType.MINIMAL, "极简风", "简洁克制的留白排版", Icons.Outlined.CropSquare),
    XIAOHONGSHU(AnnouncementTemplateType.XIAOHONGSHU, "小红书风", "鲜明活泼的社交卡片风格", Icons.AutoMirrored.Outlined.MenuBook),
    GRADIENT(AnnouncementTemplateType.GRADIENT, "渐变风", "柔和梦幻的渐变视觉层次", Icons.Outlined.Gradient),
    GLASSMORPHISM(AnnouncementTemplateType.GLASSMORPHISM, "毛玻璃风", "通透模糊与悬浮质感", Icons.Outlined.PhoneIphone),
    NEON(AnnouncementTemplateType.NEON, "霓虹风", "高对比度的醒目提示效果", Icons.Outlined.Bolt),
    CUTE(AnnouncementTemplateType.CUTE, "可爱风", "轻松俏皮的趣味表达", Icons.Outlined.FavoriteBorder),
    ELEGANT(AnnouncementTemplateType.ELEGANT, "优雅风", "柔和细腻的高级感呈现", Icons.Outlined.Diamond),
    FESTIVE(AnnouncementTemplateType.FESTIVE, "节日风", "热闹明快的庆典氛围", Icons.Outlined.Celebration),
    DARK(AnnouncementTemplateType.DARK, "暗黑风", "深色高对比的信息聚焦", Icons.Outlined.DarkMode),
    NATURE(AnnouncementTemplateType.NATURE, "自然风", "舒缓清新的平衡表达", Icons.Outlined.Park)
}

/**
 * 公告配置扩展 - 包含模板信息
 */
data class AnnouncementConfig(
    val announcement: Announcement,
    val template: AnnouncementTemplate = AnnouncementTemplate.XIAOHONGSHU,
    val primaryColor: Color = Color(0xFF007AFF),
    val showEmoji: Boolean = true,
    val animationEnabled: Boolean = true
)

private fun Announcement.linkUrlOrNull(): String? = linkUrl?.takeIf { it.isNotBlank() }

private fun Announcement.linkTextOrDefault(defaultText: String = Strings.viewDetails): String =
    linkText?.ifEmpty { defaultText } ?: defaultText

/**
 * 公告弹窗 - 根据模板显示不同样式
 * 所有模板遵循统一的设计语言: 圆角 24-28dp / 渐变色系 / 微动画
 */
@Composable
fun AnnouncementDialog(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)? = null,
    onNeverShowChecked: ((Boolean) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        when (config.template) {
            AnnouncementTemplate.MINIMAL -> MinimalTemplate(config, onDismiss, onLinkClick)
            AnnouncementTemplate.XIAOHONGSHU -> XiaohongshuTemplate(config, onDismiss, onLinkClick)
            AnnouncementTemplate.GRADIENT -> GradientTemplate(config, onDismiss, onLinkClick)
            AnnouncementTemplate.GLASSMORPHISM -> GlassmorphismTemplate(config, onDismiss, onLinkClick)
            AnnouncementTemplate.NEON -> NeonTemplate(config, onDismiss, onLinkClick)
            AnnouncementTemplate.CUTE -> CuteTemplate(config, onDismiss, onLinkClick)
            AnnouncementTemplate.ELEGANT -> ElegantTemplate(config, onDismiss, onLinkClick)
            AnnouncementTemplate.FESTIVE -> FestiveTemplate(config, onDismiss, onLinkClick)
            AnnouncementTemplate.DARK -> DarkTemplate(config, onDismiss, onLinkClick)
            AnnouncementTemplate.NATURE -> NatureTemplate(config, onDismiss, onLinkClick)
        }
    }
}

// ==========================================
// 共享的对话框容器壳 — 保证所有模板的入场动画一致
// ==========================================
@Composable
private fun TemplateShell(
    config: AnnouncementConfig,
    widthFraction: Float = 0.88f,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    cornerRadius: Int = 28,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .animateEnterExit(config.animationEnabled),
        shape = RoundedCornerShape(cornerRadius.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(content = content)
    }
}

// ==========================================
// 共享的底部按钮栏
// ==========================================
@Composable
private fun DialogActions(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?,
    confirmText: String = Strings.btnConfirm,
    confirmGradient: List<Color>? = null,
    confirmTextColor: Color = Color.White,
    linkTextColor: Color = MaterialTheme.colorScheme.primary
) {
    val linkUrl = config.announcement.linkUrlOrNull()

    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (onLinkClick != null && linkUrl != null) {
            PremiumOutlinedButton(
                onClick = { onLinkClick(linkUrl) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    config.announcement.linkTextOrDefault(),
                    fontWeight = FontWeight.SemiBold,
                    color = linkTextColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp))
            }
        }

        if (confirmGradient != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(brush = Brush.horizontalGradient(confirmGradient))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    confirmText,
                    fontWeight = FontWeight.SemiBold,
                    color = confirmTextColor,
                    fontSize = 15.sp
                )
            }
        } else {
            PremiumButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(confirmText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ==========================================
// 1. 极简风 MINIMAL — 纯白大留白
// ==========================================
@Composable
private fun MinimalTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?
) {
    val linkUrl = config.announcement.linkUrlOrNull()

    TemplateShell(config, backgroundColor = Color.White) {
        Column(modifier = Modifier.padding(36.dp)) {
            if (config.announcement.title.isNotBlank()) {
                Text(
                    text = config.announcement.title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W300,
                    letterSpacing = 3.sp,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(1.5.dp)
                        .background(Color(0xFFCCCCCC))
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(
                text = config.announcement.content,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                lineHeight = 28.sp,
                color = Color(0xFF444444)
            )

            Spacer(modifier = Modifier.height(36.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        Strings.btnConfirm,
                        color = Color(0xFF999999),
                        fontWeight = FontWeight.W300,
                        letterSpacing = 1.sp
                    )
                }

                if (onLinkClick != null && linkUrl != null) {
                    TextButton(
                        onClick = { onLinkClick(linkUrl) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            config.announcement.linkTextOrDefault(Strings.viewDetails),
                            color = Color(0xFF1A1A1A),
                            fontWeight = FontWeight.W400,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. 小红书风 — 渐变粉红色头部 + 圆角卡片
// ==========================================
@Composable
private fun XiaohongshuTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?
) {
    TemplateShell(config) {
        // 渐变头部
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFF2442), Color(0xFFFF6B81)),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (config.showEmoji) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📢", fontSize = 26.sp)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                Text(
                    text = config.announcement.title.ifEmpty { Strings.popupAnnouncement },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 内容区
        Column(modifier = Modifier.padding(24.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFF0F2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = config.announcement.content,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 26.sp,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(18.dp)
                )
            }
        }

        DialogActions(
            config, onDismiss, onLinkClick,
            confirmText = Strings.iUnderstand,
            confirmGradient = listOf(Color(0xFFFF2442), Color(0xFFFF6B81)),
            linkTextColor = Color(0xFFFF2442)
        )
    }
}

// ==========================================
// 3. 渐变风 — 整体紫蓝渐变背景
// ==========================================
@Composable
private fun GradientTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?
) {
    val linkUrl = config.announcement.linkUrlOrNull()

    Card(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .animateEnterExit(config.animationEnabled),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            // 装饰光圈
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = (-30).dp, y = (-30).dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
            )

            Column(modifier = Modifier.padding(32.dp)) {
                if (config.showEmoji) {
                    Text("✨", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = config.announcement.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = config.announcement.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                if (onLinkClick != null && linkUrl != null) {
                    Surface(
                        onClick = { onLinkClick(linkUrl) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                config.announcement.linkTextOrDefault(),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            Strings.btnConfirm,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF667EEA)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. 毛玻璃风 — iOS 风格磨砂感
// ==========================================
@Composable
private fun GlassmorphismTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?
) {
    val linkUrl = config.announcement.linkUrlOrNull()

    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .animateEnterExit(config.animationEnabled),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF2F8F9FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 头部图标区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6366F1).copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                if (config.showEmoji) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF6366F1), Color(0xFFA855F7))
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Campaign,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (config.announcement.title.isNotBlank()) {
                    Text(
                        text = config.announcement.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = config.announcement.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF555555),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = Color(0x20000000),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // iOS 风格的按钮
            if (onLinkClick != null && linkUrl != null) {
                TextButton(
                    onClick = { onLinkClick(linkUrl) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Text(
                        config.announcement.linkTextOrDefault(),
                        color = Color(0xFF6366F1),
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp
                    )
                }
                HorizontalDivider(
                    color = Color(0x20000000),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text(
                    Strings.btnConfirm,
                    color = Color(0xFF6366F1),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// ==========================================
// 5. 霓虹风 — 暗底 + 发光渐变边框
// ==========================================
@Composable
private fun NeonTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?
) {
    // 呼吸发光动画
    val glowTransition = rememberInfiniteTransition(label = "neon_glow")
    val glowAlpha by glowTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .animateEnterExit(config.animationEnabled)
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00F5FF).copy(alpha = glowAlpha),
                        Color(0xFFBF00FF).copy(alpha = glowAlpha),
                        Color(0xFF00F5FF).copy(alpha = glowAlpha)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D1A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            if (config.showEmoji) {
                Text("⚡", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = config.announcement.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00F5FF)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF00F5FF), Color(0xFFBF00FF))
                        ),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = config.announcement.content,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFE0E0E8),
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            DialogActions(
                config, onDismiss, onLinkClick,
                confirmGradient = listOf(Color(0xFF00F5FF), Color(0xFFBF00FF)),
                confirmTextColor = Color.White,
                linkTextColor = Color(0xFF00F5FF)
            )
        }
    }
}

// ==========================================
// 6. 可爱风 — 粉色泡泡 + 圆润形态
// ==========================================
@Composable
private fun CuteTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?
) {
    TemplateShell(config, backgroundColor = Color(0xFFFFF0F5), cornerRadius = 32) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (config.showEmoji) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFF69B4), Color(0xFFFFB6D9))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎀", fontSize = 32.sp)
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            Text(
                text = config.announcement.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD63384),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = config.announcement.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp,
                    modifier = Modifier.padding(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            DialogActions(
                config, onDismiss, onLinkClick,
                confirmText = "${Strings.btnConfirm} 💕",
                confirmGradient = listOf(Color(0xFFFF69B4), Color(0xFFFFB6D9)),
                linkTextColor = Color(0xFFD63384)
            )
        }
    }
}

// ==========================================
// 7. 优雅风 — 金色调 + 衬线字体感
// ==========================================
@Composable
private fun ElegantTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?
) {
    TemplateShell(config, backgroundColor = Color(0xFFFAF8F5)) {
        Column(modifier = Modifier.padding(32.dp)) {
            // 顶部金色装饰线
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color(0xFFD4AF37))
                            )
                        )
                )
                if (config.showEmoji) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text("💎", fontSize = 24.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFD4AF37), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = config.announcement.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.W300,
                color = Color(0xFF2C2C2C),
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = config.announcement.content,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF555555),
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 底部金色装饰线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFD4AF37).copy(alpha = 0.3f))
            )
        }

        DialogActions(
            config, onDismiss, onLinkClick,
            confirmGradient = listOf(Color(0xFFD4AF37), Color(0xFFF4E4BA)),
            confirmTextColor = Color(0xFF2C2C2C),
            linkTextColor = Color(0xFFD4AF37)
        )
    }
}

// ==========================================
// 8. 节日风 — 红金庆典
// ==========================================
@Composable
private fun FestiveTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?
) {
    TemplateShell(config) {
        // 红色渐变头部
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFB22222), Color(0xFFDC143C)),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            // 装饰粒子
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-20).dp, y = (-20).dp)
                    .background(Color(0xFFFFD700).copy(alpha = 0.1f), CircleShape)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (config.showEmoji) {
                    Text("🎊", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = config.announcement.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 内容区
        Column(modifier = Modifier.padding(24.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFF8E1),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = config.announcement.content,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 26.sp,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(18.dp)
                )
            }
        }

        DialogActions(
            config, onDismiss, onLinkClick,
            confirmGradient = listOf(Color(0xFFB22222), Color(0xFFDC143C)),
            linkTextColor = Color(0xFFB22222)
        )
    }
}

// ==========================================
// 9. 暗黑风 — 深灰底 + 紫色强调
// ==========================================
@Composable
private fun DarkTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?
) {
    TemplateShell(config, backgroundColor = Color(0xFF1A1A2E)) {
        Column(modifier = Modifier.padding(28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (config.showEmoji) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA))
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌙", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                }
                Text(
                    text = config.announcement.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF252542),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = config.announcement.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFB0B0C0),
                    lineHeight = 26.sp,
                    modifier = Modifier.padding(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        DialogActions(
            config, onDismiss, onLinkClick,
            confirmGradient = listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA)),
            linkTextColor = Color(0xFFA78BFA)
        )
    }
}

// ==========================================
// 10. 自然风 — 绿色清新
// ==========================================
@Composable
private fun NatureTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?
) {
    TemplateShell(config) {
        // 绿色渐变头部
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF10B981), Color(0xFF34D399)),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (config.showEmoji) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌿", fontSize = 26.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(
                    text = config.announcement.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 内容区
        Column(modifier = Modifier.padding(24.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF0FFF4),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = config.announcement.content,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 26.sp,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(18.dp)
                )
            }
        }

        DialogActions(
            config, onDismiss, onLinkClick,
            confirmGradient = listOf(Color(0xFF10B981), Color(0xFF34D399)),
            linkTextColor = Color(0xFF10B981)
        )
    }
}

/**
 * AnnouncementTemplate 本地化扩展函数
 */
fun AnnouncementTemplate.getLocalizedDisplayName(): String = this.displayName

fun AnnouncementTemplate.getLocalizedDescription(): String = this.description

/**
 * 通用的进出场动画扩展 — 弹簧缩放 + 淡入
 */
@Composable
private fun Modifier.animateEnterExit(enabled: Boolean): Modifier = composed {
    if (!enabled) return@composed this

    val transition = updateTransition(targetState = true, label = "dialog_transition")

    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.65f, stiffness = 350f) },
        label = "dialog_scale"
    ) { state -> if (state) 1f else 0.85f }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(250) },
        label = "dialog_alpha"
    ) { state -> if (state) 1f else 0f }

    val offsetY by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.75f, stiffness = 300f) },
        label = "dialog_offsetY"
    ) { state -> if (state) 0f else 24f }

    this
        .scale(scale)
        .graphicsLayer {
            this.alpha = alpha
            translationY = offsetY
        }
}
