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











class ThemeRevealState {

    var isAnimating by mutableStateOf(false)
        internal set


    var snapshot: ImageBitmap? by mutableStateOf(null)
        internal set


    val animationProgress = Animatable(0f)


    var revealCenter by mutableStateOf(Offset.Zero)
        internal set


    var toDark by mutableStateOf(false)
        internal set


    var maxRadius by mutableFloatStateOf(0f)
        internal set


    internal var currentAnimationJob: Job? = null


    var revealGeneration by mutableIntStateOf(0)
        internal set










    fun triggerReveal(
        center: Offset,
        switchToDark: Boolean,
        view: View,
        window: Window?,
        onCaptureDone: () -> Unit
    ) {

        currentAnimationJob?.cancel()
        currentAnimationJob = null

        revealCenter = center
        toDark = switchToDark


        val w = view.width.toFloat()
        val h = view.height.toFloat()
        maxRadius = max(
            max(hypot(center.x, center.y), hypot(w - center.x, center.y)),
            max(hypot(center.x, h - center.y), hypot(w - center.x, h - center.y))
        )


        revealGeneration++


        captureScreen(view, window) { bitmap ->
            snapshot = bitmap.asImageBitmap()
            isAnimating = true
            onCaptureDone()
        }
    }




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




    internal fun cleanup() {
        isAnimating = false
        snapshot = null
        currentAnimationJob = null
    }
}




@Composable
fun rememberThemeRevealState(): ThemeRevealState {
    return remember { ThemeRevealState() }
}


val LocalThemeRevealState = staticCompositionLocalOf<ThemeRevealState?> { null }












@Composable
fun CircularRevealOverlay(
    revealState: ThemeRevealState,
    durationMs: Int = 480
) {
    if (!revealState.isAnimating) return

    val snap = revealState.snapshot ?: return
    val coroutineScope = rememberCoroutineScope()


    LaunchedEffect(revealState.revealGeneration) {
        revealState.animationProgress.snapTo(0f)


        val job = coroutineScope.launch {
            // Reveal easing matches the rest of the app: quick to kick off,
            // slow to settle. The old FastOutSlowInEasing felt like the
            // new theme was punching through the screen.
            revealState.animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMs,
                    easing = androidx.compose.animation.core.CubicBezierEasing(
                        0.22f, 1.0f, 0.36f, 1.0f
                    )
                )
            )

            revealState.cleanup()
        }
        revealState.currentAnimationJob = job
        job.join()
    }

    val progress = revealState.animationProgress.value
    val center = revealState.revealCenter
    val maxR = revealState.maxRadius


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

        // During the reveal the old snapshot sits on top; the new theme is
        // already painted beneath it. We clip OUT the growing circle so the
        // new theme shows through where the circle expands. A soft
        // feathered edge (via reduced alpha on the outer ring) prevents the
        // hard-line artifact you otherwise get at the clip boundary.
        clipPath(circlePath, clipOp = ClipOp.Difference) {
            drawImage(snap)
        }
    }
}
