package com.webtoapp.ui.screens.ecosystem

import android.os.Build
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webtoapp.core.cloud.InstalledItemsTracker
import com.webtoapp.core.cloud.ModuleItem
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.image.EcosystemAvatarImage
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.design.WtaRadius
import com.webtoapp.ui.theme.AppColors
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object EcosystemMotion {
    val LikeBounce = spring<Float>(dampingRatio = 0.35f, stiffness = Spring.StiffnessHigh)
    val PressDown = spring<Float>(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
    val ItemEntrance = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
    val MorphButton = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
    val GlassFade = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
}

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                Modifier
                    .matchParentSize()
                    .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                    .clip(shape)
                    .background(surfaceColor.copy(alpha = tintAlpha))
            )
        } else {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(surfaceColor.copy(alpha = 0.92f))
            )
        }
        content()
    }
}

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

@Composable
fun FrostedBottomBar(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val divider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

    Box(modifier) {
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

fun Modifier.pressScale(pressedScale: Float = 0.96f): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) pressedScale else 1f,
        EcosystemMotion.PressDown,
        label = "pressScale"
    )
    this.scale(scale)
}

@Composable
fun GradientShimmer(
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
    highlightColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f)
) {
    val transition = rememberInfiniteTransition(label = "gradShimmer")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerOffset"
    )
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val brush = Brush.linearGradient(
            0f to baseColor,
            0.4f to highlightColor,
            0.6f to highlightColor,
            1f to baseColor,
            start = Offset(w * offset, 0f),
            end = Offset(w * (offset + 1f), size.height)
        )
        drawRect(brush)
    }
}

private data class BurstParticle(
    val angle: Float,
    val speed: Float,
    val radius: Float,
    val color: Color
)

@Composable
fun LikeBurstEffect(
    trigger: Boolean,
    color: Color = MaterialTheme.colorScheme.primary,
    particleCount: Int = 8,
    modifier: Modifier = Modifier
) {
    val particles = remember(trigger) {
        if (trigger) {
            List(particleCount) {
                BurstParticle(
                    angle = (360f / particleCount) * it + Random.nextFloat() * 20f,
                    speed = Random.nextFloat() * 4f + 3f,
                    radius = Random.nextFloat() * 2.5f + 1f,
                    color = color.copy(alpha = Random.nextFloat() * 0.4f + 0.6f)
                )
            }
        } else {
            emptyList()
        }
    }

    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(trigger) {
        if (trigger) {
            progress = 0f
            val start = System.nanoTime()
            while (progress < 1f) {
                delay(16)
                progress = ((System.nanoTime() - start) / 400_000_000f).coerceAtMost(1f)
            }
        }
    }

    if (trigger && progress < 1f) {
        Canvas(modifier) {
            particles.forEach { particle ->
                val radians = Math.toRadians(particle.angle.toDouble())
                val distance = particle.speed * progress * 24f
                val alpha = (1f - progress).pow(1.5f) * particle.color.alpha
                drawCircle(
                    color = particle.color.copy(alpha = alpha),
                    radius = particle.radius * (1f - progress * 0.5f),
                    center = center + Offset(
                        (cos(radians) * distance).toFloat(),
                        (sin(radians) * distance).toFloat()
                    )
                )
            }
        }
    }
}

@Composable
fun StaggeredItem(
    index: Int,
    staggerDelay: Long = 45L,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * staggerDelay)
        visible = true
    }

    val offsetY by animateFloatAsState(
        if (visible) 0f else 20f,
        EcosystemMotion.ItemEntrance,
        label = "staggerY"
    )
    val alpha by animateFloatAsState(
        if (visible) 1f else 0f,
        tween(250),
        label = "staggerAlpha"
    )

    Box(Modifier.graphicsLayer { translationY = offsetY; this.alpha = alpha }) {
        content()
    }
}

@Composable
fun Avatar(
    name: String,
    avatarUrl: String? = null,
    size: Int = 40,
    modifier: Modifier = Modifier
) {
    if (avatarUrl != null) {
        EcosystemAvatarImage(
            avatarUrl = avatarUrl,
            size = size.dp,
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.38f).sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun dividerColor() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

@Composable
fun ShimmerBlock(
    width: Dp,
    height: Dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(WtaRadius.Button),
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "sBlock")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "sBlockOff"
    )
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            drawRect(
                Brush.linearGradient(
                    0f to base,
                    0.5f to highlight,
                    1f to base,
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
    style: TextStyle = MaterialTheme.typography.bodySmall,
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
            if (targetState > initialState) {
                androidx.compose.animation.slideInVertically { -it } + androidx.compose.animation.fadeIn() togetherWith
                    androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut()
            } else {
                androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn() togetherWith
                    androidx.compose.animation.slideOutVertically { -it } + androidx.compose.animation.fadeOut()
            }
        },
        modifier = modifier,
        label = "counterContent"
    ) { target ->
        Text("$target", style = style, color = color)
    }
}

fun formatEcosystemRelativeTime(timestamp: String?): String? {
    val millis = parseEcosystemTimeMillis(timestamp) ?: return null
    val now = System.currentTimeMillis()
    return if (kotlin.math.abs(now - millis) < DateUtils.MINUTE_IN_MILLIS) {
        Strings.justNow
    } else {
        DateUtils.getRelativeTimeSpanString(
            millis,
            now,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
}

fun ecosystemPublishTimeLabel(timestamp: String?): String? {
    return formatEcosystemRelativeTime(timestamp)?.let { "${Strings.publishTime}：$it" }
}

private fun parseEcosystemTimeMillis(timestamp: String?): Long? {
    val raw = timestamp?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val compactOffset = raw.replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
    val candidates = listOf(raw, compactOffset, raw.replace("Z", "+0000"))
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd HH:mm:ss"
    )
    for (candidate in candidates) {
        for (pattern in patterns) {
            try {
                val formatter = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                return formatter.parse(candidate)?.time
            } catch (_: Exception) {
            }
        }
    }
    return null
}

@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
    Box(
        modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .drawBehind {
                drawRect(color)
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

@Composable
fun ModuleCard(
    module: ModuleItem,
    onClick: () -> Unit,
    onInstall: () -> Unit,
    hasUpdate: Boolean = false
) {
    val installedTracker = koinInject<InstalledItemsTracker>()
    val isInstalled = installedTracker.isInstalled(module.id)

    EnhancedElevatedCard(
        shape = RoundedCornerShape(WtaRadius.Card),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(WtaRadius.Card))
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
                    Icon(
                        Icons.Outlined.Extension,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            module.name,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (module.isFeatured) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(WtaRadius.Button))
                                    .background(AppColors.Warning.copy(alpha = 0.12f))
                            ) {
                                Text(
                                    Strings.featured,
                                    fontSize = 10.sp,
                                    color = AppColors.Warning,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        "by ${module.authorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            if (!module.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    module.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            if (module.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    module.tags.take(3).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(WtaRadius.Button))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Text(
                                tag,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    "${module.downloads}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = AppColors.BilibiliPink.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    "${module.likeCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    Icons.Outlined.Star,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = AppColors.Warning
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    if (module.ratingCount > 0) "${module.rating}" else "-",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                module.versionName?.let {
                    Text(
                        "v$it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                when {
                    hasUpdate -> {
                        FilledTonalButton(
                            onClick = onInstall,
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(WtaRadius.Card)
                        ) {
                            Icon(Icons.Outlined.SystemUpdate, null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Strings.update, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    isInstalled -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(WtaRadius.Card))
                                .background(AppColors.Success.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.Check, null, modifier = Modifier.size(15.dp), tint = AppColors.Success)
                                Text(Strings.installed, fontSize = 12.sp, color = AppColors.Success, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    else -> {
                        FilledTonalButton(
                            onClick = onInstall,
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(WtaRadius.Card)
                        ) {
                            Icon(Icons.Outlined.FileDownload, null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Strings.install, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
