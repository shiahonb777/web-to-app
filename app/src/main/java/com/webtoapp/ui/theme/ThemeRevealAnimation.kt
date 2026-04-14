package com.webtoapp.ui.theme

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import kotlin.math.hypot
import kotlin.math.max

// ==================== Circular Reveal State ====================

/**
 * Manages circular reveal state for light/dark mode switching.
 * 
 * Fully interruptible design.
 * - Tap again during animation: cancel current run, resnapshot, restart from new position.
 * - Spring physics keeps velocity on interrupt for natural continuity.
 * - Handles rapid repeated taps reliably.
 */
class ThemeRevealState {
    /** Whether an animation is currently running. */
    var isAnimating by mutableStateOf(false)
        internal set
    
    /** Screen snapshot captured before theme switch. */
    var snapshot: ImageBitmap? by mutableStateOf(null)
        internal set

    /** Animation progress 0f -> 1f. */
    val animationProgress = Animatable(0f)

    /** Reveal center in screen coordinates. */
    var revealCenter by mutableStateOf(Offset.Zero)
        internal set

    /** Whether switching to dark mode (controls clip direction). */
    var toDark by mutableStateOf(false)
        internal set

    /** Screen diagonal length (max reveal radius). */
    var maxRadius by mutableFloatStateOf(0f)
        internal set

    /** Current animation Job, used for cancellation. */
    internal var currentAnimationJob: Job? = null

    /** Interrupt generation counter; incremented every trigger. */
    var revealGeneration by mutableIntStateOf(0)
        internal set

    /**
     * Trigger circular reveal animation with interrupt support.
     *
     * If an animation is already running:
     * 1. Cancel current animation Job.
     * 2. Run immediate cleanup (except isAnimating flag).
     * 3. Capture a fresh snapshot of the current partial result.
     * 4. Start a new animation from the new tap position.
     */
    fun triggerReveal(
        center: Offset,
        switchToDark: Boolean,
        view: View,
        window: Window?,
        onCaptureDone: () -> Unit
    ) {
        // Interrupt: cancel current animation
        currentAnimationJob?.cancel()
        currentAnimationJob = null

        revealCenter = center
        toDark = switchToDark

        // Compute max radius as the farthest corner distance from center
        val w = view.width.toFloat()
        val h = view.height.toFloat()
        maxRadius = max(
            max(hypot(center.x, center.y), hypot(w - center.x, center.y)),
            max(hypot(center.x, h - center.y), hypot(w - center.x, h - center.y))
        )

        // Increment generation to invalidate previous LaunchedEffect
        revealGeneration++

        // Snapshot current screen (including interrupted partial state)
        captureScreen(view, window) { bitmap ->
            snapshot = bitmap.asImageBitmap()
            isAnimating = true
            onCaptureDone()
        }
    }

    /**
     * Capture current screen.
     */
    private fun captureScreen(view: View, window: Window?, onCaptured: (Bitmap) -> Unit) {
        val width = view.width
        val height = view.height
        if (width <= 0 || height <= 0) {
            onCaptured(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && window != null) {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                PixelCopy.request(
                    window,
                    android.graphics.Rect(0, 0, width, height),
                    bitmap,
                    { result ->
                        if (result == PixelCopy.SUCCESS) {
                            onCaptured(bitmap)
                        } else {
                            fallbackCapture(view, onCaptured)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } catch (e: Exception) {
                fallbackCapture(view, onCaptured)
            }
        } else {
            fallbackCapture(view, onCaptured)
        }
    }

    private fun fallbackCapture(view: View, onCaptured: (Bitmap) -> Unit) {
        try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            view.draw(canvas)
            onCaptured(bitmap)
        } catch (e: Exception) {
            onCaptured(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
        }
    }

    /**
     * Clean up resources.
     */
    internal fun cleanup() {
        isAnimating = false
        snapshot = null
        currentAnimationJob = null
    }
}

/**
 * Remember and manage ThemeRevealState.
 */
@Composable
fun rememberThemeRevealState(): ThemeRevealState {
    return remember { ThemeRevealState() }
}

// Global CompositionLocal for reveal state
val LocalThemeRevealState = staticCompositionLocalOf<ThemeRevealState?> { null }

// ==================== Circular Reveal Overlay ====================

/**
 * Circular reveal overlay - fully interruptible version.
 *
 * Interrupt mechanism:
 * - LaunchedEffect uses revealGeneration as key.
 * - New tap changes generation -> old coroutine cancels -> new coroutine starts.
 * - Animation restarts from snapTo(0f) with a fresh partial-state snapshot.
 * - Spring physics gives natural velocity continuity after interruption.
 */
@Composable
fun CircularRevealOverlay(
    revealState: ThemeRevealState,
    durationMs: Int = 600
) {
    if (!revealState.isAnimating) return

    val snap = revealState.snapshot ?: return
    val coroutineScope = rememberCoroutineScope()

    // key = revealGeneration: each interrupt cancels old effect and starts new one
    LaunchedEffect(revealState.revealGeneration) {
        revealState.animationProgress.snapTo(0f)

        // Keep Job reference for external cancellation
        val job = coroutineScope.launch {
            revealState.animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMs,
                    easing = FastOutSlowInEasing
                )
            )
            // Clean up only on normal completion (not interrupted)
            revealState.cleanup()
        }
        revealState.currentAnimationJob = job
        job.join()
    }

    val progress = revealState.animationProgress.value
    val center = revealState.revealCenter
    val maxR = revealState.maxRadius

    // Current reveal radius
    val currentRadius = maxR * progress

    Canvas(modifier = Modifier.fillMaxSize()) {
        val circlePath = Path().apply {
            addOval(
                Rect(
                    center.x - currentRadius,
                    center.y - currentRadius,
                    center.x + currentRadius,
                    center.y + currentRadius
                )
            )
        }

        // Difference mode: draw full snapshot and carve out circular area
        clipPath(circlePath, clipOp = ClipOp.Difference) {
            drawImage(snap)
        }
    }
}
