package com.webtoapp.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════
// Global animation utilities
// ═══════════════════════════════════════════════════════════

// ==================== 1. Staggered Entrance ====================

/**
 * Staggered enter animation for list items.
 * Slide up + fade in, each item delayed by [staggerDelayMs].
 *
 * Usage: wrap each LazyColumn item with StaggeredAnimatedItem.
 */
@Composable
fun StaggeredAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    staggerDelayMs: Long = 50L,
    slideOffsetDp: Int = 40,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * staggerDelayMs)
        visible = true
    }

    val density = LocalDensity.current
    val slideOffsetPx = with(density) { slideOffsetDp.dp.roundToPx() }

    // Use spring instead of tween to keep velocity on interrupt
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + slideInVertically(
            initialOffsetY = { slideOffsetPx },
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        modifier = modifier
    ) {
        content()
    }
}

// ==================== 2. Empty-State Breathing Float ====================

/**
 * Breathing float modifier.
 * Slow vertical drift + slight rotation for static elements.
 */
@Composable
fun Modifier.breathingFloat(
    floatAmountDp: Float = 6f,
    rotationDegrees: Float = 2f,
    durationMs: Int = 3000
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")

    val translationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = floatAmountDp,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -rotationDegrees,
        targetValue = rotationDegrees,
        animationSpec = infiniteRepeatable(
            animation = tween((durationMs * 1.3f).toInt(), easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    return this.graphicsLayer {
        this.translationY = -translationY * density
        this.rotationZ = rotation
        this.scaleX = scale
        this.scaleY = scale
    }
}

// ==================== 3. Dialog Spring Scale ====================

/**
 * Dialog enter scale animation.
 * Spring effect: scale 0.85->1.0 + alpha 0->1.
 */
@Composable
fun AnimatedDialogContent(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dialogScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "dialogAlpha"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) {
        content()
    }
}

// ==================== 4. Snackbar Spring Slide ====================

/**
 * Snackbar enter/exit animation specs.
 */
val SnackbarEnterTransition: EnterTransition = slideInVertically(
    initialOffsetY = { it },
    animationSpec = spring(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMediumLow
    )
) + fadeIn(
    animationSpec = tween(200)
)

val SnackbarExitTransition: ExitTransition = slideOutHorizontally(
    targetOffsetX = { it },
    animationSpec = tween(250, easing = FastOutSlowInEasing)
) + fadeOut(
    animationSpec = tween(200)
)

// ==================== 5. Tab Switch Direction ====================

/**
 * Compute tab transition direction.
 * @return Positive = slide left (target on right), negative = slide right (target on left).
 */
fun tabSlideDirection(previousTab: Int, currentTab: Int): Int {
    return if (currentTab > previousTab) 1 else -1
}

/**
 * Enter transition for tab page content.
 */
fun tabEnterTransition(direction: Int): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> direction * fullWidth / 4 },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeIn(
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    )
}

/**
 * Exit transition for tab page content.
 */
fun tabExitTransition(direction: Int): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> -direction * fullWidth / 4 },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeOut(
        animationSpec = tween(250, easing = FastOutSlowInEasing)
    )
}

// ==================== 6. Card Collapse/Expand ====================

/**
 * Expand/collapse specs for collapsible cards (Apple-like physical style).
 * Removes fadeIn/fadeOut and relies on clipping + spring motion only.
 * This gives a physical stretch-like expansion feel.
 */
val CardExpandTransition: EnterTransition = expandVertically(
    animationSpec = spring(
        dampingRatio = 0.82f, // "Q"
        stiffness = 350f      // (iOS)
    ),
    expandFrom = androidx.compose.ui.Alignment.Top,
    clip = true
)

val CardCollapseTransition: ExitTransition = shrinkVertically(
    // Bezier easing mimics physical fall; force interpolate to 0 to avoid residual pixel snap at the base point.
    animationSpec = tween(
        durationMillis = 280,
        easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f) // Comment
    ),
    shrinkTowards = androidx.compose.ui.Alignment.Top,
    clip = true
)

// ==================== 7. Switch Ripple Effect ====================

/**
 * Animation params for ripple effect.
 */
data class RippleAnimState(
    val isActive: Boolean = false,
    val progress: Float = 0f
)

@Composable
fun rememberRippleAnimatable(): Animatable<Float, AnimationVector1D> {
    return remember { Animatable(0f) }
}
