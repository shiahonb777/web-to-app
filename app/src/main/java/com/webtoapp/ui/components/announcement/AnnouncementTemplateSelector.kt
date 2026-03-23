package com.webtoapp.ui.components.announcement

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * 模板预览颜色配置
 */
private val templateColors = mapOf(
    AnnouncementTemplate.MINIMAL to listOf(Color(0xFF1A1A1A), Color(0xFF333333)),
    AnnouncementTemplate.XIAOHONGSHU to listOf(Color(0xFFFF2442), Color(0xFFFF6B6B)),
    AnnouncementTemplate.GRADIENT to listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    AnnouncementTemplate.GLASSMORPHISM to listOf(Color(0xFF6366F1), Color(0xFFA855F7)),
    AnnouncementTemplate.NEON to listOf(Color(0xFF00F5FF), Color(0xFFFF00FF)),
    AnnouncementTemplate.CUTE to listOf(Color(0xFFFF69B4), Color(0xFFFFB6C1)),
    AnnouncementTemplate.ELEGANT to listOf(Color(0xFFD4AF37), Color(0xFFF4E4BA)),
    AnnouncementTemplate.FESTIVE to listOf(Color(0xFFB22222), Color(0xFFFFD700)),
    AnnouncementTemplate.DARK to listOf(Color(0xFF1A1A2E), Color(0xFF8B5CF6)),
    AnnouncementTemplate.NATURE to listOf(Color(0xFF22C55E), Color(0xFF86EFAC))
)

/**
 * 公告模板选择器
 */
@Composable
fun AnnouncementTemplateSelector(
    selectedTemplate: AnnouncementTemplate,
    onTemplateSelected: (AnnouncementTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = Strings.selectAnnouncementStyle,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
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
 * 单个模板卡片
 */
@Composable
private fun TemplateCard(
    template: AnnouncementTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "border"
    )
    
    val colors = templateColors[template] ?: listOf(Color.Gray, Color.DarkGray)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // 预览卡片
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = colors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // 模拟卡片内容
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    template.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(1.5.dp))
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 模板名称
        Text(
            text = template.getLocalizedDisplayName(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        // 选中指示器
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

/**
 * 公告模板预览对话框
 */
@Composable
fun AnnouncementPreviewDialog(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(Strings.previewAnnouncementEffect)
        },
        text = {
            Column {
                Text(
                    text = "${Strings.theme}: ${config.template.getLocalizedDisplayName()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = config.template.getLocalizedDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
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
 * 完整的公告配置卡片（包含模板选择）
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
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Announcement,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "弹窗公告",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            if (enabled) {
                // 模板选择器
                AnnouncementTemplateSelector(
                    selectedTemplate = template,
                    onTemplateSelected = onTemplateChange
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Announcement标题
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(Strings.announcementTitle) },
                    placeholder = { Text(Strings.inputAnnouncementTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Announcement内容
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    label = { Text(Strings.announcementContent) },
                    placeholder = { Text(Strings.inputAnnouncementContent) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                // 链接设置
                OutlinedTextField(
                    value = linkUrl ?: "",
                    onValueChange = { onLinkUrlChange(it.ifBlank { null }) },
                    label = { Text(Strings.linkAddress) },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (!linkUrl.isNullOrBlank()) {
                    OutlinedTextField(
                        value = linkText ?: "",
                        onValueChange = { onLinkTextChange(it.ifBlank { null }) },
                        label = { Text(Strings.linkText) },
                        placeholder = { Text(Strings.viewDetails) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Show频率
                Text(
                    Strings.displayFrequency,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = showOnce,
                        onClick = { onShowOnceChange(true) },
                        label = { Text(Strings.showOnceOnly) },
                        leadingIcon = if (showOnce) {
                            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = !showOnce,
                        onClick = { onShowOnceChange(false) },
                        label = { Text(Strings.everyLaunch) },
                        leadingIcon = if (!showOnce) {
                            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                        } else null
                    )
                }
                
                // 预览按钮
                OutlinedButton(
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
