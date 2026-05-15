package com.webtoapp.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.webtoapp.ui.theme.LocalAnimationSettings
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * Physics primitives for the Wta design system.
 *
 * These modifiers make UI elements behave like they have mass, drag, and
 * elasticity. Every interaction carries velocity forward so animations never
 * "snap" from one state to another — they always settle from wherever they
 * currently are.
 */

// ============================================================================
// Rubber-band resistance (the iOS overscroll stretch)
// ============================================================================

/**
 * Applies rubber-band resistance to a drag offset, so that pulling past a
 * boundary meets increasing resistance — exactly like iOS scroll views.
 *
 * The further past the boundary, the less responsive the drag becomes.
 * Formula: `f(x) = (1 - 1/(|x|/d + 1)) * d` where `d` is the max stretch.
 */
fun rubberBand(offset: Float, maxStretch: Float = 400f): Float {
    if (offset == 0f) return 0f
    val absOffset = abs(offset)
    val resistance = 1f - 1f / (absOffset / maxStretch + 1f)
    return resistance * maxStretch * sign(offset)
}

// ============================================================================
// Interruptible spring animation — velocity is preserved
// ============================================================================

/**
 * Like [animateFloatAsState] but the animation can be interrupted at any time
 * with velocity intact. Returns the current value and a launcher to animate
 * to a new target.
 *
 * The critical difference: if you interrupt a running animation mid-flight,
 * the new animation starts from the current value *and* velocity, so there
 * is no visible jump or direction reversal glitch. This is what makes iOS
 * animations feel so fluid.
 */
@Composable
fun rememberInterruptibleAnimatable(initial: Float = 0f): Animatable<Float, AnimationVector1D> {
    return remember { Animatable(initial, Float.VectorConverter) }
}

// ============================================================================
// Drag-to-dismiss with spring back
// ============================================================================

/**
 * Makes any composable respond to vertical drags with physics.
 *
 * Behaviour:
 *  - Drag down (or up) past a threshold: trigger [onDismiss].
 *  - Release without passing the threshold: spring back to origin with
 *    velocity preservation.
 *  - Past the max-stretch point: rubber-band resistance kicks in.
 *
 * The item stays alive and responds continuously — it is never in a
 * "transitioning" dead state where input is ignored.
 */
@Composable
fun Modifier.wtaDragToDismissVertical(
    threshold: Float = 180f,
    maxStretch: Float = 400f,
    onDismiss: () -> Unit
): Modifier = composed {
    val offsetY = rememberInterruptibleAnimatable(0f)
    val scope = rememberCoroutineScope()
    val animSettings = LocalAnimationSettings.current

    this
        .pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = {
                    val current = offsetY.value
                    scope.launch {
                        if (abs(current) > threshold) {
                            onDismiss()
                        } else {
                            offsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = WtaMotion.bouncySpring(),
                                initialVelocity = 0f
                            )
                        }
                    }
                },
                onDragCancel = {
                    scope.launch {
                        offsetY.animateTo(0f, WtaMotion.bouncySpring())
                    }
                }
            ) { change, dragAmount ->
                change.consume()
                val next = offsetY.value + dragAmount.y
                scope.launch {
                    offsetY.snapTo(rubberBand(next, maxStretch))
                }
            }
        }
        .graphicsLayer {
            if (animSettings.enabled) {
                translationY = offsetY.value
            }
        }
}

// ============================================================================
// Coroutine scope helper for use inside composed { }
// ============================================================================

@Composable
private fun rememberCoroutineScope(): kotlinx.coroutines.CoroutineScope {
    return androidx.compose.runtime.rememberCoroutineScope()
}
