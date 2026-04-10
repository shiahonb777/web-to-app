package com.webtoapp.ui.components.announcement

import com.webtoapp.ui.components.PremiumOutlinedButton
import com.webtoapp.ui.components.PremiumSwitch
import com.webtoapp.ui.components.PremiumFilterChip
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumTextField
import androidx.compose.material.icons.automirrored.outlined.Article

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * 模板渐变配色 — 每个模板的两色渐变
 */
private val templateColors = mapOf(
    AnnouncementTemplate.MINIMAL to listOf(Color(0xFFF5F5F5), Color(0xFFE8E8E8)),
    AnnouncementTemplate.XIAOHONGSHU to listOf(Color(0xFFFF2442), Color(0xFFFF6B81)),
    AnnouncementTemplate.GRADIENT to listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    AnnouncementTemplate.GLASSMORPHISM to listOf(Color(0xFF6366F1), Color(0xFFA855F7)),
    AnnouncementTemplate.NEON to listOf(Color(0xFF0D0D1A), Color(0xFF1A1A2E)),
    AnnouncementTemplate.CUTE to listOf(Color(0xFFFF69B4), Color(0xFFFFB6D9)),
    AnnouncementTemplate.ELEGANT to listOf(Color(0xFFF8F3E8), Color(0xFFF0E6D0)),
    AnnouncementTemplate.FESTIVE to listOf(Color(0xFFB22222), Color(0xFFDC143C)),
    AnnouncementTemplate.DARK to listOf(Color(0xFF1A1A2E), Color(0xFF252542)),
    AnnouncementTemplate.NATURE to listOf(Color(0xFF10B981), Color(0xFF34D399))
)

/**
 * 模板图标 emoji
 */
private val templateEmoji = mapOf(
    AnnouncementTemplate.MINIMAL to "✦",
    AnnouncementTemplate.XIAOHONGSHU to "📕",
    AnnouncementTemplate.GRADIENT to "🎨",
    AnnouncementTemplate.GLASSMORPHISM to "🔮",
    AnnouncementTemplate.NEON to "⚡",
    AnnouncementTemplate.CUTE to "🎀",
    AnnouncementTemplate.ELEGANT to "💎",
    AnnouncementTemplate.FESTIVE to "🎊",
    AnnouncementTemplate.DARK to "🌙",
    AnnouncementTemplate.NATURE to "🌿"
)

/**
 * 模板强调色 (用于文字、边框等)
 */
private val templateAccentColor = mapOf(
    AnnouncementTemplate.MINIMAL to Color(0xFF1A1A1A),
    AnnouncementTemplate.XIAOHONGSHU to Color(0xFFFF2442),
    AnnouncementTemplate.GRADIENT to Color(0xFF667EEA),
    AnnouncementTemplate.GLASSMORPHISM to Color(0xFF6366F1),
    AnnouncementTemplate.NEON to Color(0xFF00F5FF),
    AnnouncementTemplate.CUTE to Color(0xFFD63384),
    AnnouncementTemplate.ELEGANT to Color(0xFFD4AF37),
    AnnouncementTemplate.FESTIVE to Color(0xFFB22222),
    AnnouncementTemplate.DARK to Color(0xFF8B5CF6),
    AnnouncementTemplate.NATURE to Color(0xFF10B981)
)

/**
 * 预览区域内容颜色 (模拟排版行的颜色)
 */
private val templatePreviewContentColor = mapOf(
    AnnouncementTemplate.MINIMAL to Color(0xFFCCCCCC),
    AnnouncementTemplate.XIAOHONGSHU to Color.White.copy(alpha = 0.5f),
    AnnouncementTemplate.GRADIENT to Color.White.copy(alpha = 0.4f),
    AnnouncementTemplate.GLASSMORPHISM to Color.White.copy(alpha = 0.4f),
    AnnouncementTemplate.NEON to Color(0xFF00F5FF).copy(alpha = 0.3f),
    AnnouncementTemplate.CUTE to Color.White.copy(alpha = 0.5f),
    AnnouncementTemplate.ELEGANT to Color(0xFFD4AF37).copy(alpha = 0.3f),
    AnnouncementTemplate.FESTIVE to Color(0xFFFFD700).copy(alpha = 0.4f),
    AnnouncementTemplate.DARK to Color(0xFF8B5CF6).copy(alpha = 0.3f),
    AnnouncementTemplate.NATURE to Color.White.copy(alpha = 0.4f)
)

/**
 * 模板预览标题行颜色
 */
private val templatePreviewTitleColor = mapOf(
    AnnouncementTemplate.MINIMAL to Color(0xFF999999),
    AnnouncementTemplate.XIAOHONGSHU to Color.White.copy(alpha = 0.8f),
    AnnouncementTemplate.GRADIENT to Color.White.copy(alpha = 0.7f),
    AnnouncementTemplate.GLASSMORPHISM to Color.White.copy(alpha = 0.7f),
    AnnouncementTemplate.NEON to Color(0xFF00F5FF).copy(alpha = 0.6f),
    AnnouncementTemplate.CUTE to Color.White.copy(alpha = 0.8f),
    AnnouncementTemplate.ELEGANT to Color(0xFFD4AF37).copy(alpha = 0.5f),
    AnnouncementTemplate.FESTIVE to Color(0xFFFFD700).copy(alpha = 0.7f),
    AnnouncementTemplate.DARK to Color(0xFFA78BFA).copy(alpha = 0.6f),
    AnnouncementTemplate.NATURE to Color.White.copy(alpha = 0.7f)
)

/**
 * 公告模板选择器 — 重构版
 *
 * 设计亮点：
 *  • 更大的预览卡片 (116dp宽) 带真实弹窗缩略图
 *  • 选中边框使用模板自身强调色渐变
 *  • 底部显示模板名称 + 选中勾
 *  • 弹簧缩放 + 阴影联动动画
 */
@Composable
fun AnnouncementTemplateSelector(
    selectedTemplate: AnnouncementTemplate,
    onTemplateSelected: (AnnouncementTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 标题行
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Palette,
                    null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = Strings.selectAnnouncementStyle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            // 当前选中标签
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = (templateAccentColor[selectedTemplate] ?: MaterialTheme.colorScheme.primary)
                    .copy(alpha = 0.1f)
            ) {
                Text(
                    text = selectedTemplate.getLocalizedDisplayName(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = templateAccentColor[selectedTemplate] ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            items(AnnouncementTemplate.entries) { template ->
                TemplateCard(
                    template = template,
                    isSelected = template == selectedTemplate,
                    onClick = { onTemplateSelected(template) }
                )
            }
        }
    }
}

/**
 * 单个模板卡片 — 高保真预览
 */
@Composable
private fun TemplateCard(
    template: AnnouncementTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cardScale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 400f),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 2.dp,
        animationSpec = tween(300),
        label = "elevation"
    )

    val colors = templateColors[template] ?: listOf(Color.Gray, Color.DarkGray)
    val emoji = templateEmoji[template] ?: "✦"
    val accentColor = templateAccentColor[template] ?: Color.Gray
    val isDarkPreview = template == AnnouncementTemplate.NEON || template == AnnouncementTemplate.DARK
    val titleLineColor = templatePreviewTitleColor[template] ?: Color.White.copy(alpha = 0.6f)
    val contentLineColor = templatePreviewContentColor[template] ?: Color.White.copy(alpha = 0.3f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(116.dp)
            .scale(cardScale)
    ) {
        // 预览区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .shadow(elevation, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = if (isSelected) 2.5.dp else 0.dp,
                    brush = if (isSelected) Brush.linearGradient(
                        colors = listOf(accentColor, accentColor.copy(alpha = 0.5f))
                    ) else Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.Transparent)
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = colors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .clickable(onClick = onClick)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            // 模拟弹窗缩略图
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Emoji
                Text(text = emoji, fontSize = 22.sp)
                Spacer(modifier = Modifier.height(6.dp))

                // 模拟标题行
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(5.dp)
                        .background(titleLineColor, RoundedCornerShape(2.5.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))

                // 模拟内容行
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(3.dp)
                        .background(contentLineColor, RoundedCornerShape(1.5.dp))
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(3.dp)
                        .background(contentLineColor.copy(alpha = contentLineColor.alpha * 0.6f), RoundedCornerShape(1.5.dp))
                )
            }

            // 选中勾
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .shadow(3.dp, CircleShape)
                        .background(accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (isDarkPreview) Color.White else Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 模板名称
        Text(
            text = template.getLocalizedDisplayName(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) accentColor
                    else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 公告模板预览对话框 — 增强版
 */
@Composable
fun AnnouncementPreviewDialog(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = templateColors[config.template] ?: listOf(Color.Gray, Color.DarkGray)
    val accentColor = templateAccentColor[config.template] ?: MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = colors,
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    templateEmoji[config.template] ?: "✦",
                    fontSize = 24.sp
                )
            }
        },
        title = {
            Text(
                Strings.previewAnnouncementEffect,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 模板信息卡片
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accentColor.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    brush = Brush.linearGradient(colors = colors)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                templateEmoji[config.template] ?: "✦",
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = config.template.getLocalizedDisplayName(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = config.template.getLocalizedDescription(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 功能标签
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (config.showEmoji) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.08f)
                        ) {
                            Text(
                                "😊 Emoji",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (config.animationEnabled) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.08f)
                        ) {
                            Text(
                                "✨ Animation",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Visibility, null, Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Strings.btnPreview)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}

/**
 * 完整的公告配置卡片（包含模板选择）— 增强版
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedAnnouncementCard(
    enabled: Boolean,
    template: AnnouncementTemplate,
    title: String,
    content: String,
    linkUrl: String?,
    linkText: String?,
    showOnce: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onTemplateChange: (AnnouncementTemplate) -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onLinkUrlChange: (String?) -> Unit,
    onLinkTextChange: (String?) -> Unit,
    onShowOnceChange: (Boolean) -> Unit,
    onPreview: () -> Unit
) {
    val accentColor = templateAccentColor[template] ?: MaterialTheme.colorScheme.primary

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 标题行 — 使用当前模板强调色的渐变图标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val headerColors = templateColors[template] ?: listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                brush = if (enabled)
                                    Brush.linearGradient(
                                        colors = headerColors,
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    )
                                else Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Campaign,
                            null,
                            tint = if (enabled) Color.White
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = Strings.popupAnnouncement,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = Strings.announcementSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                PremiumSwitch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            AnimatedVisibility(visible = enabled) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // 模板选择器
                    AnnouncementTemplateSelector(
                        selectedTemplate = template,
                        onTemplateSelected = onTemplateChange
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // 公告标题
                    PremiumTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text(Strings.announcementTitle) },
                        placeholder = { Text(Strings.inputAnnouncementTitle) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Title, null, Modifier.size(20.dp))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 公告内容
                    PremiumTextField(
                        value = content,
                        onValueChange = onContentChange,
                        label = { Text(Strings.announcementContent) },
                        placeholder = { Text(Strings.inputAnnouncementContent) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Outlined.Article, null, Modifier.size(20.dp))
                        },
                        supportingText = {
                            Text(
                                "${content.length}/500",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (content.length > 500)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 链接设置区
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Link, null,
                                    modifier = Modifier.size(18.dp),
                                    tint = accentColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    Strings.announcementLinkSection,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            PremiumTextField(
                                value = linkUrl ?: "",
                                onValueChange = { onLinkUrlChange(it.ifBlank { null }) },
                                label = { Text(Strings.linkUrl) },
                                placeholder = { Text("https://...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            AnimatedVisibility(visible = !linkUrl.isNullOrBlank()) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    PremiumTextField(
                                        value = linkText ?: "",
                                        onValueChange = { onLinkTextChange(it.ifBlank { null }) },
                                        label = { Text(Strings.linkText) },
                                        placeholder = { Text(Strings.viewDetails) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // 显示频率
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumFilterChip(
                            selected = showOnce,
                            onClick = { onShowOnceChange(true) },
                            label = { Text(Strings.showOnceOnly) },
                            leadingIcon = if (showOnce) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                        PremiumFilterChip(
                            selected = !showOnce,
                            onClick = { onShowOnceChange(false) },
                            label = { Text(Strings.everyLaunch) },
                            leadingIcon = if (!showOnce) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                    }

                    // 预览按钮
                    PremiumOutlinedButton(
                        onClick = onPreview,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = title.isNotBlank() || content.isNotBlank()
                    ) {
                        Icon(Icons.Outlined.Preview, null, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.previewAnnouncementEffect)
                    }
                }
            }
        }
    }
}
