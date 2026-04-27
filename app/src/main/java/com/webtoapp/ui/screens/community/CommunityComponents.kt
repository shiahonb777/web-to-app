package com.webtoapp.ui.screens.community

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webtoapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════
// 社区物理动画工具包
// Jobs-style: 毛玻璃、高斯模糊、弹簧物理、粒子系统
// ═══════════════════════════════════════════════════════════

// ─── 物理弹簧常量 ───

object CommunityPhysics {
    // 点赞弹跳 — 大阻尼、高弹性
    val LikeBounce = spring<Float>(dampingRatio = 0.35f, stiffness = Spring.StiffnessHigh)
    // 按压回弹 — iOS 触觉感
    val PressDown = spring<Float>(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
    // 列表入场 — 自然阻尼
    val ItemEntrance = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
    // 形态变化 — 中等弹性
    val MorphButton = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
    // Tab 指示器
    val TabIndicator = spring<Dp>(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
    // 毛玻璃渐入
    val GlassFade = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
}

// ═══════════════════════════════════════════════════════════
// 毛玻璃 / 高斯模糊 — Jobs-style Frosted Glass
// ═══════════════════════════════════════════════════════════

/**
 * 毛玻璃 Surface — API 31+ 使用真实 blur，低版本使用更实的半透明表面
 * 参数:
 *   blurRadius — 高斯模糊半径（px）
 *   tintAlpha  — 表面着色透明度
 *   noiseAlpha — 噪点颗粒感透明度
 */
@Composable
fun FrostedGlassSurface(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 28.dp,
    tintAlpha: Float = 0.72f,
    cornerRadius: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val shape = RoundedCornerShape(cornerRadius)

    Box(modifier) {
        // 模糊层
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                Modifier
                    .matchParentSize()
                    .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                    .clip(shape)
                    .background(surfaceColor.copy(alpha = tintAlpha))
            )
        } else {
            // 低版本表面方案 — 提高不透明度保持层次感
            Box(
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(surfaceColor.copy(alpha = 0.92f))
            )
        }
        // 内容
        content()
    }
}

/**
 * 毛玻璃顶栏 Modifier — 用于 TopAppBar 底层
 */
fun Modifier.frostedTopBar(
    blurRadius: Dp = 24.dp
): Modifier = composed {
    val color = MaterialTheme.colorScheme.surface
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this
            .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
            .background(color.copy(alpha = 0.68f))
    } else {
        this.background(color.copy(alpha = 0.94f))
    }
}

/**
 * 毛玻璃底栏 — 带顶部微光线条
 */
@Composable
fun FrostedBottomBar(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val divider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

    Box(modifier) {
        // 毛玻璃背景
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                Modifier
                    .matchParentSize()
                    .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                    .background(surfaceColor.copy(alpha = 0.72f))
            )
        } else {
            Box(
                Modifier
                    .matchParentSize()
                    .background(surfaceColor.copy(alpha = 0.95f))
            )
        }
        // 顶部微光线
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .align(Alignment.TopCenter)
                .background(divider)
        )
        content()
    }
}

// ═══════════════════════════════════════════════════════════
// iOS-style 按压缩放
// ═══════════════════════════════════════════════════════════

fun Modifier.pressScale(pressedScale: Float = 0.96f): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) pressedScale else 1f,
        CommunityPhysics.PressDown, label = "pressScale"
    )
    this.scale(scale)
}

// ═══════════════════════════════════════════════════════════
// 渐变横扫 Shimmer
// ═══════════════════════════════════════════════════════════

@Composable
fun GradientShimmer(
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
    highlightColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f)
) {
    val transition = rememberInfiniteTransition(label = "gradShimmer")
    val offset by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerOffset"
    )
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val brush = Brush.linearGradient(
            0f to baseColor, 0.4f to highlightColor,
            0.6f to highlightColor, 1f to baseColor,
            start = Offset(w * offset, 0f),
            end = Offset(w * (offset + 1f), size.height)
        )
        drawRect(brush)
    }
}

// ═══════════════════════════════════════════════════════════
// 粒子爆炸 — 点赞时的光点扩散
// ═══════════════════════════════════════════════════════════

data class BurstParticle(
    val angle: Float, val speed: Float,
    val radius: Float, val color: Color
)

@Composable
fun LikeBurstEffect(
    trigger: Boolean,
    color: Color = MaterialTheme.colorScheme.primary,
    particleCount: Int = 8,
    modifier: Modifier = Modifier
) {
    val particles = remember(trigger) {
        if (trigger) List(particleCount) {
            BurstParticle(
                angle = (360f / particleCount) * it + Random.nextFloat() * 20f,
                speed = Random.nextFloat() * 4f + 3f,
                radius = Random.nextFloat() * 2.5f + 1f,
                color = color.copy(alpha = Random.nextFloat() * 0.4f + 0.6f)
            )
        } else emptyList()
    }

    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(trigger) {
        if (trigger) {
            progress = 0f
            val t0 = System.nanoTime()
            while (progress < 1f) {
                delay(16)
                progress = ((System.nanoTime() - t0) / 400_000_000f).coerceAtMost(1f)
            }
        }
    }

    if (trigger && progress < 1f) {
        Canvas(modifier) {
            particles.forEach { p ->
                val rad = Math.toRadians(p.angle.toDouble())
                val dist = p.speed * progress * 24f
                val a = (1f - progress).pow(1.5f) * p.color.alpha
                drawCircle(
                    color = p.color.copy(alpha = a),
                    radius = p.radius * (1f - progress * 0.5f),
                    center = center + Offset(
                        (cos(rad) * dist).toFloat(),
                        (sin(rad) * dist).toFloat()
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Stagger 列表入场 — 弹簧物理
// ═══════════════════════════════════════════════════════════

@Composable
fun StaggeredItem(
    index: Int,
    staggerDelay: Long = 45L,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * staggerDelay); visible = true }

    val offsetY by animateFloatAsState(
        if (visible) 0f else 20f,
        CommunityPhysics.ItemEntrance, label = "staggerY"
    )
    val alpha by animateFloatAsState(
        if (visible) 1f else 0f,
        tween(250), label = "staggerAlpha"
    )

    Box(Modifier.graphicsLayer { translationY = offsetY; this.alpha = alpha }) {
        content()
    }
}

// ═══════════════════════════════════════════════════════════
// 通用组件
// ═══════════════════════════════════════════════════════════

@Composable
fun Avatar(
    name: String,
    avatarUrl: String? = null,
    size: Int = 40,
    modifier: Modifier = Modifier
) {
    if (avatarUrl != null) {
        AsyncImage(
            model = avatarUrl, contentDescription = null,
            modifier = modifier.size(size.dp).clip(CircleShape)
        )
    } else {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = modifier.size(size.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = (size * 0.38f).sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun dividerColor() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

@Composable
fun ShimmerBlock(
    width: Dp, height: Dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp),
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "sBlock")
    val offset by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "sBlockOff"
    )
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Surface(shape = shape, color = Color.Transparent, modifier = modifier.width(width).height(height)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            drawRect(
                Brush.linearGradient(
                    0f to base, 0.5f to highlight, 1f to base,
                    start = Offset(w * offset, 0f),
                    end = Offset(w * (offset + 0.8f), size.height)
                )
            )
        }
    }
}

@Composable
fun AnimatedCounter(
    count: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
) {
    val animatedCount by animateIntAsState(
        count,
        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "counter"
    )
    AnimatedContent(
        targetState = animatedCount,
        transitionSpec = {
            if (targetState > initialState)
                slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
            else
                slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
        },
        modifier = modifier, label = "counterContent"
    ) { target ->
        Text("$target", style = style, color = color)
    }
}

// ═══════════════════════════════════════════════════════════
// 毛玻璃分隔区域 — 半透明带微模糊的段落分隔器
// ═══════════════════════════════════════════════════════════

@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
    Box(
        modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .drawBehind {
                drawRect(color)
                // 微光高亮线
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.3f to Color.White.copy(alpha = 0.04f),
                        0.7f to Color.White.copy(alpha = 0.04f),
                        1f to Color.Transparent
                    )
                )
            }
    )
}

// ═══════════════════════════════════════════════════════════
// 统一模块卡片 — 市场与社区共用
// ═══════════════════════════════════════════════════════════

/**
 * 统一模块卡片组件
 *
 * 从市场浏览或社区发现页均可使用，点击跳转到统一的 ModuleDetailScreen。
 * 无论入口如何，模块只有一个身份、一张卡片。
 */
@Composable
fun ModuleCard(
    module: com.webtoapp.core.cloud.ModuleItem,
    onClick: () -> Unit,
    onInstall: () -> Unit,
    hasUpdate: Boolean = false
) {
    val installedTracker = org.koin.compose.koinInject<com.webtoapp.core.cloud.InstalledItemsTracker>()
    val isInstalled = installedTracker.isInstalled(module.id)

    com.webtoapp.ui.components.EnhancedElevatedCard(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            // Header: icon + name + author
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Extension, null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(module.name, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall, maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(weight = 1f, fill = false))
                        if (module.isFeatured) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(5.dp),
                                color = Color(0xFFFFA726).copy(alpha = 0.12f)) {
                                Text(com.webtoapp.core.i18n.Strings.featured, fontSize = 10.sp,
                                    color = Color(0xFFFFA726), fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text("by ${module.authorName}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }

            // Description
            if (!module.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(module.description, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }

            // Tags
            if (module.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
                    module.tags.take(3).forEach { tag ->
                        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Text(tag, fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom bar: stats + install
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                // Downloads
                Icon(Icons.Outlined.Download, null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.width(3.dp))
                Text("${module.downloads}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.width(12.dp))

                // Likes
                Icon(Icons.Outlined.FavoriteBorder, null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFE91E63).copy(alpha = 0.6f))
                Spacer(modifier = Modifier.width(3.dp))
                Text("${module.likeCount}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.width(12.dp))

                // Rating
                Icon(Icons.Outlined.Star, null,
                    modifier = Modifier.size(14.dp), tint = Color(0xFFFFC107))
                Spacer(modifier = Modifier.width(3.dp))
                Text(if (module.ratingCount > 0) "${module.rating}" else "-",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.width(12.dp))

                // Version
                module.versionName?.let {
                    Text("v$it", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }

                Spacer(modifier = Modifier.weight(weight = 1f, fill = true))

                // Install / Installed / Update button
                if (hasUpdate) {
                    androidx.compose.material3.Button(
                        onClick = onInstall,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Icon(Icons.Outlined.SystemUpdate, null,
                            modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(com.webtoapp.core.i18n.Strings.update, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                } else if (isInstalled) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.08f)
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                        ) {
                                Icon(Icons.Filled.Check, null,
                                modifier = Modifier.size(15.dp), tint = Color(0xFF4CAF50))
                            Text(com.webtoapp.core.i18n.Strings.installed, fontSize = 12.sp,
                                color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    androidx.compose.material3.FilledTonalButton(
                        onClick = onInstall,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Outlined.FileDownload, null,
                            modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(com.webtoapp.core.i18n.Strings.install, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
