package com.webtoapp.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.webtoapp.ui.theme.LocalAnimationSettings
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * iOS-style interactive swipe-back gesture.
 *
 * When the user drags from the left edge (~20dp zone), the entire screen
 * follows their finger, revealing the previous screen underneath with a
 * parallax effect. When they release:
 *  - If dragged past 30% of screen width OR velocity is high: trigger pop
 *    with spring physics completing the slide-off.
 *  - Otherwise: spring back to origin, keeping velocity intact so there's
 *    no visible jump.
 *
 * The current screen is NEVER in a frozen "transitioning" state — input
 * flows through continuously.
 *
 * Usage: wrap your screen's root composable:
 * ```
 * WtaSwipeBackContainer(onBack = { navController.popBackStack() }) {
 *     YourScreenContent()
 * }
 * ```
 */
@Composable
fun WtaSwipeBackContainer(
    onBack: () -> Unit,
    enabled: Boolean = true,
    edgeWidthDp: Int = 24,
    dismissThresholdFraction: Float = 0.35f,
    velocityThreshold: Float = 1200f,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val edgeWidthPx = with(density) { edgeWidthDp.dp.toPx() }
    val dismissThresholdPx = screenWidthPx * dismissThresholdFraction

    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val animSettings = LocalAnimationSettings.current

    val swipeModifier = if (enabled && animSettings.enabled) {
        Modifier.pointerInput(Unit) {
            var dragStartedFromEdge = false
            var lastVelocity = 0f
            var lastPosition = 0f
            var lastTimestamp = 0L

            detectHorizontalDragGestures(
                onDragStart = { start ->
                    dragStartedFromEdge = start.x <= edgeWidthPx
                    if (dragStartedFromEdge) {
                        lastPosition = start.x
                        lastTimestamp = System.nanoTime()
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    if (!dragStartedFromEdge) return@detectHorizontalDragGestures

                    // Only respond to rightward drags (swipe-back direction).
                    if (dragAmount > 0 || offsetX.value > 0) {
                        change.consume()
                        val next = (offsetX.value + dragAmount).coerceAtLeast(0f)
                        scope.launch { offsetX.snapTo(next) }

                        // Track velocity manually (px / ms).
                        val now = System.nanoTime()
                        val deltaMs = (now - lastTimestamp) / 1_000_000f
                        if (deltaMs > 0f) {
                            lastVelocity = dragAmount / deltaMs * 1000f
                        }
                        lastPosition = change.position.x
                        lastTimestamp = now
                    }
                },
                onDragEnd = {
                    if (!dragStartedFromEdge) return@detectHorizontalDragGestures

                    val shouldDismiss = offsetX.value > dismissThresholdPx ||
                        lastVelocity > velocityThreshold

                    scope.launch {
                        if (shouldDismiss) {
                            // Complete the slide-off, then invoke onBack.
                            offsetX.animateTo(
                                targetValue = screenWidthPx,
                                animationSpec = spring(
                                    dampingRatio = 0.9f,
                                    stiffness = 400f
                                ),
                                initialVelocity = lastVelocity.coerceAtLeast(0f)
                            )
                            onBack()
                        } else {
                            // Spring back to origin with velocity preserved.
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.78f,
                                    stiffness = 350f
                                ),
                                initialVelocity = lastVelocity
                            )
                        }
                    }
                },
                onDragCancel = {
                    if (dragStartedFromEdge) {
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.78f,
                                    stiffness = 350f
                                )
                            )
                        }
                    }
                }
            )
        }
    } else {
        Modifier
    }

    androidx.compose.foundation.layout.Box(
        modifier = swipeModifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = offsetX.value
                // Slight scale-down as it slides off — adds depth.
                val progress = (offsetX.value / screenWidthPx).coerceIn(0f, 1f)
                val s = 1f - progress * 0.04f
                scaleX = s
                scaleY = s
            }
    ) {
        content()
    }
}
