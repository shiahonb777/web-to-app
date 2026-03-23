package com.webtoapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.frontend.FrontendFramework
import com.webtoapp.core.frontend.SampleProject
import com.webtoapp.core.frontend.SampleProjectManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.theme.enhanced.LocalEnhancedBackgroundEnabled
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vue.js 自定义图标
 */
@Composable
fun VueLogo(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "vue")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Canvas(modifier = modifier.size(48.dp)) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        
        val outerPath = Path().apply {
            moveTo(centerX, height * 0.85f)
            lineTo(width * 0.15f, height * 0.15f)
            lineTo(width * 0.30f, height * 0.15f)
            lineTo(centerX, height * 0.60f)
            lineTo(width * 0.70f, height * 0.15f)
            lineTo(width * 0.85f, height * 0.15f)
            close()
        }
        
        drawPath(
            path = outerPath,
            color = Color(0xFF42B883).copy(alpha = glowAlpha),
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )
        
        drawPath(
            path = outerPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF42B883), Color(0xFF35495E))
            )
        )
        
        val innerPath = Path().apply {
            moveTo(centerX, height * 0.70f)
            lineTo(width * 0.30f, height * 0.15f)
            lineTo(width * 0.42f, height * 0.15f)
            lineTo(centerX, height * 0.50f)
            lineTo(width * 0.58f, height * 0.15f)
            lineTo(width * 0.70f, height * 0.15f)
            close()
        }
        
        drawPath(path = innerPath, color = Color(0xFF35495E))
    }
}

/**
 * React 自定义图标
 */
@Composable
fun ReactLogo(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "react")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Canvas(modifier = modifier.size(48.dp)) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        val reactBlue = Color(0xFF61DAFB)
        
        drawCircle(color = reactBlue, radius = width * 0.1f, center = Offset(centerX, centerY))
        
        val orbitRadiusX = width * 0.38f
        val orbitRadiusY = height * 0.15f
        
        for (i in 0..2) {
            rotate(degrees = rotation + i * 60f, pivot = Offset(centerX, centerY)) {
                drawOval(
                    color = reactBlue,
                    topLeft = Offset(centerX - orbitRadiusX, centerY - orbitRadiusY),
                    size = Size(orbitRadiusX * 2, orbitRadiusY * 2),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }
        }
        
        for (i in 0..2) {
            val angle = Math.toRadians((rotation + i * 120.0))
            val electronX = centerX + (orbitRadiusX * 0.9f * cos(angle)).toFloat()
            val electronY = centerY + (orbitRadiusY * 0.9f * sin(angle)).toFloat()
            
            rotate(degrees = i * 60f, pivot = Offset(centerX, centerY)) {
                drawCircle(color = reactBlue, radius = 4f, center = Offset(electronX, electronY))
            }
        }
    }
}

/**
 * Vite 自定义图标
 */
@Composable
fun ViteLogo(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "vite")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    
    Canvas(modifier = modifier.size(48.dp)) {
        val width = size.width
        val height = size.height
        
        val lightningPath = Path().apply {
            moveTo(width * 0.65f, height * 0.05f)
            lineTo(width * 0.25f, height * 0.50f)
            lineTo(width * 0.45f, height * 0.50f)
            lineTo(width * 0.35f, height * 0.95f)
            lineTo(width * 0.75f, height * 0.45f)
            lineTo(width * 0.55f, height * 0.45f)
            close()
        }
        
        drawPath(
            path = lightningPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFD62E).copy(alpha = 0.3f + shimmer * 0.3f),
                    Color(0xFF646CFF).copy(alpha = 0.3f + shimmer * 0.3f)
                ),
                start = Offset(0f, 0f),
                end = Offset(width, height)
            ),
            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        drawPath(
            path = lightningPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFD62E), Color(0xFF646CFF)),
                start = Offset(0f, 0f),
                end = Offset(width, height)
            )
        )
        
        val highlightPath = Path().apply {
            moveTo(width * 0.55f, height * 0.15f)
            lineTo(width * 0.35f, height * 0.45f)
            lineTo(width * 0.45f, height * 0.45f)
            close()
        }
        
        drawPath(path = highlightPath, color = Color.White.copy(alpha = 0.3f + shimmer * 0.2f))
    }
}

/**
 * Sample project card - 完全重构版
 * 在强化模式下使用纯透明背景，无边框无阴影
 */
@Composable
fun SampleProjectsCard(
    onSelectSample: (SampleProject) -> Unit,
    modifier: Modifier = Modifier
) {
    val samples = remember { SampleProjectManager.getSampleProjects() }
    val isEnhanced = LocalEnhancedBackgroundEnabled.current
    
    // 根据模式选择不同的容器样式
    val containerModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .then(
            if (isEnhanced) {
                Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    )
                )
            } else {
                Modifier.background(MaterialTheme.colorScheme.surface)
            }
        )
    
    Box(modifier = containerModifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 标题区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon容器 - 渐变背景
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        Strings.sampleProjects,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        Strings.quickExperienceFrontend,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (isEnhanced) 0.9f else 1f
                        )
                    )
                }
                
                // 标签
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = if (isEnhanced) 0.8f else 1f
                        ))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        Strings.quickExperience,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 示例项目列表
            samples.forEachIndexed { index, sample ->
                SampleProjectItem(
                    sample = sample,
                    onClick = { onSelectSample(sample) },
                    isEnhanced = isEnhanced
                )
                if (index < samples.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * 单个示例项目项 - 重构版
 */
@Composable
private fun SampleProjectItem(
    sample: SampleProject,
    onClick: () -> Unit,
    isEnhanced: Boolean
) {
    val frameworkColor = getFrameworkColor(sample.framework)
    
    // 悬停/按压动画
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )
    
    val itemModifier = Modifier
        .fillMaxWidth()
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(RoundedCornerShape(16.dp))
        .then(
            if (isEnhanced) {
                // 强化模式：使用渐变边框 + 半透明背景
                Modifier
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                frameworkColor.copy(alpha = 0.4f),
                                frameworkColor.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                frameworkColor.copy(alpha = 0.08f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                            )
                        )
                    )
            } else {
                // 普通模式：纯色背景
                Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }
        )
        .clickable(
            onClick = {
                isPressed = true
                onClick()
            }
        )
        .padding(14.dp)
    
    Row(
        modifier = itemModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 框架图标
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            frameworkColor.copy(alpha = if (isEnhanced) 0.2f else 0.15f),
                            frameworkColor.copy(alpha = if (isEnhanced) 0.08f else 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            when (sample.framework) {
                FrontendFramework.VUE -> VueLogo(modifier = Modifier.size(34.dp))
                FrontendFramework.REACT -> ReactLogo(modifier = Modifier.size(34.dp))
                FrontendFramework.VITE -> ViteLogo(modifier = Modifier.size(34.dp))
                else -> Text(sample.icon, fontSize = 22.sp)
            }
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                sample.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(3.dp))
            
            Text(
                sample.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (isEnhanced) 0.85f else 1f
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 标签
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                sample.tags.take(2).forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(frameworkColor.copy(alpha = if (isEnhanced) 0.15f else 0.1f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = frameworkColor
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Play按钮
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            frameworkColor.copy(alpha = 0.2f),
                            frameworkColor.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = Strings.run,
                tint = frameworkColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 获取框架对应的颜色
 */
private fun getFrameworkColor(framework: FrontendFramework): Color {
    return when (framework) {
        FrontendFramework.VUE -> Color(0xFF42B883)
        FrontendFramework.REACT -> Color(0xFF61DAFB)
        FrontendFramework.NEXT -> Color(0xFF000000)
        FrontendFramework.NUXT -> Color(0xFF00DC82)
        FrontendFramework.ANGULAR -> Color(0xFFDD0031)
        FrontendFramework.SVELTE -> Color(0xFFFF3E00)
        FrontendFramework.VITE -> Color(0xFF646CFF)
        FrontendFramework.UNKNOWN -> Color.Gray
    }
}
