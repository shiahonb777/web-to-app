package com.webtoapp.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.webtoapp.ui.design.WtaMotion
import kotlinx.coroutines.delay

/**
 * Motion helpers used across the app.
 *
 * Design intent: every entrance animation here leans on [WtaMotion] so that
 * scale/spring/fade parameters stay identical to the rest of the app. Loose
 * bespoke springs used to produce a "jelly" feeling the user disliked; they
 * are replaced with the settle/snap/enter tweens.
 */

/**
 * Stagger-in an item by its index. Each item slides up from below with a
 * spring. No fade — the motion alone communicates arrival. A small stagger
 * delay makes lists look like they are unfolding rather than all appearing
 * at once.
 */
@Composable
fun StaggeredAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    staggerDelayMs: Long = 35L,
    slideOffsetDp: Int = 20,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * staggerDelayMs)
        visible = true
    }

    val density = LocalDensity.current
    val slideOffsetPx = with(density) { slideOffsetDp.dp.roundToPx() }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { slideOffsetPx },
            animationSpec = WtaMotion.settleSpring()
        ),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Subtle breathing / float loop. The previous implementation rotated the
 * element as it moved which looked like a toy bobbing in the breeze. This
 * version drops rotation entirely, halves the travel, and uses a single
 * long easing curve so the element feels alive without being distracting.
 *
 * Kept as a compatibility shim for any screen that still references it; new
 * code should avoid continuous idle animations because they undermine the
 * "considered" feel of the rest of the app.
 */
@Composable
fun Modifier.breathingFloat(
    floatAmountDp: Float = 3f,
    @Suppress("UNUSED_PARAMETER") rotationDegrees: Float = 0f,
    durationMs: Int = 4200
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
    return this.graphicsLayer {
        this.translationY = -translationY * density
    }
}

/**
 * Dialog content with pure scale-based appearance. No fade — the dialog
 * pops in from a smaller scale with a bouncy spring, which alone conveys
 * arrival. Matches iOS alert controller physics.
 */
@Composable
fun AnimatedDialogContent(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = if (visible) WtaMotion.bouncySpring() else WtaMotion.snapSpring(),
        label = "dialogScale"
    )
    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        content()
    }
}

/**
 * Snackbar transitions. Entry springs up from below. Exit drifts down and off.
 * Pure translation, no fade — the spring carries the weight.
 */
val SnackbarEnterTransition: EnterTransition = slideInVertically(
    initialOffsetY = { it },
    animationSpec = WtaMotion.bouncySpring()
)

val SnackbarExitTransition: ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = WtaMotion.settleSpring()
    )

/**
 * Tab transitions. Content slides horizontally from the direction the user
 * navigated, driven by a spring. No fade — tabs are literal physical surfaces
 * sliding past each other.
 */
fun tabSlideDirection(previousTab: Int, currentTab: Int): Int {
    return if (currentTab > previousTab) 1 else -1
}

fun tabEnterTransition(direction: Int): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> direction * fullWidth },
        animationSpec = WtaMotion.settleSpring()
    )
}

fun tabExitTransition(direction: Int): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> -direction * fullWidth / 4 },
        animationSpec = WtaMotion.gentleSpring()
    )
}

/**
 * Card expand/collapse transitions used by [CollapsibleCard] and the various
 * settings sections.
 *
 * Pure vertical expansion driven by springs. No fade — the height change
 * alone conveys the reveal. Expansion uses the settle spring; collapse uses
 * the snap spring so content retracts faster than it expands.
 */
val CardExpandTransition: EnterTransition = expandVertically(
    animationSpec = WtaMotion.settleSpring(),
    expandFrom = androidx.compose.ui.Alignment.Top,
    clip = true
)

val CardCollapseTransition: ExitTransition = shrinkVertically(
    animationSpec = WtaMotion.snapSpring(),
    shrinkTowards = androidx.compose.ui.Alignment.Top,
    clip = true
)

data class RippleAnimState(
    val isActive: Boolean = false,
    val progress: Float = 0f
)

@Composable
fun rememberRippleAnimatable(): Animatable<Float, AnimationVector1D> {
    return remember { Animatable(0f) }
}
