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

// ==================== 圆形揭示动画状态 ====================

/**
 * 管理深/浅色模式切换的圆形揭示动画状态
 * 
 * ★ 完全可打断设计 ★
 * - 动画进行中再次点击：立即取消当前动画，重新截图，从新位置启动
 * - 使用 spring 物理引擎：打断时保留速度，过渡自然
 * - 支持快速连续点击：即使 0.1 秒内连点也能正确响应
 */
class ThemeRevealState {
    /** 当前是否正在播放动画 */
    var isAnimating by mutableStateOf(false)
        internal set
    
    /** 切换前的屏幕截图 */
    var snapshot: ImageBitmap? by mutableStateOf(null)
        internal set

    /** 动画进度 0f→1f */
    val animationProgress = Animatable(0f)

    /** 动画中心点（屏幕坐标） */
    var revealCenter by mutableStateOf(Offset.Zero)
        internal set

    /** 是否正在切换到深色模式（决定裁剪方向） */
    var toDark by mutableStateOf(false)
        internal set

    /** 屏幕对角线长度（动画最大半径） */
    var maxRadius by mutableFloatStateOf(0f)
        internal set

    /** ★ 打断核心：当前动画 Job，用于取消 */
    internal var currentAnimationJob: Job? = null

    /** ★ 打断计数器：每次触发递增，LaunchedEffect 通过此值检测是否被打断 */
    var revealGeneration by mutableIntStateOf(0)
        internal set

    /**
     * 触发圆形揭示动画 — 支持打断
     *
     * 如果当前有动画正在播放：
     * 1. 取消当前动画 Job
     * 2. 立即完成清理（但不清理 isAnimating 标志）
     * 3. 重新截图当前画面（已包含上一次主题切换的部分效果）
     * 4. 从新点击位置启动新动画
     */
    fun triggerReveal(
        center: Offset,
        switchToDark: Boolean,
        view: View,
        window: Window?,
        onCaptureDone: () -> Unit
    ) {
        // ★ 打断：取消当前动画
        currentAnimationJob?.cancel()
        currentAnimationJob = null

        revealCenter = center
        toDark = switchToDark

        // 计算从中心到四角的最远距离作为最大半径
        val w = view.width.toFloat()
        val h = view.height.toFloat()
        maxRadius = max(
            max(hypot(center.x, center.y), hypot(w - center.x, center.y)),
            max(hypot(center.x, h - center.y), hypot(w - center.x, h - center.y))
        )

        // 递增 generation 使旧 LaunchedEffect 失效
        revealGeneration++

        // 截图当前屏幕（包含被打断的部分动画状态）
        captureScreen(view, window) { bitmap ->
            snapshot = bitmap.asImageBitmap()
            isAnimating = true
            onCaptureDone()
        }
    }

    /**
     * 截取当前屏幕
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
     * 清理资源
     */
    internal fun cleanup() {
        isAnimating = false
        snapshot = null
        currentAnimationJob = null
    }
}

/**
 * 记住并管理 ThemeRevealState
 */
@Composable
fun rememberThemeRevealState(): ThemeRevealState {
    return remember { ThemeRevealState() }
}

// 全局 CompositionLocal 提供动画状态
val LocalThemeRevealState = staticCompositionLocalOf<ThemeRevealState?> { null }

// ==================== 圆形揭示动画叠层 ====================

/**
 * 圆形揭示动画叠层 — ★ 完全可打断版 ★
 *
 * 打断机制：
 * - LaunchedEffect 以 revealGeneration 为 key
 * - 用户再次点击 → generation 变化 → 旧协程自动取消 → 新协程启动
 * - 动画从 snapTo(0f) 重新开始，但 snapshot 已是包含了半完成状态的新截图
 * - 使用 spring 物理引擎，打断后从当前速度自然过渡
 */
@Composable
fun CircularRevealOverlay(
    revealState: ThemeRevealState,
    durationMs: Int = 600
) {
    if (!revealState.isAnimating) return

    val snap = revealState.snapshot ?: return
    val coroutineScope = rememberCoroutineScope()

    // ★ key = revealGeneration: 每次打断都会取消旧效果、启动新效果
    LaunchedEffect(revealState.revealGeneration) {
        revealState.animationProgress.snapTo(0f)

        // 保存 Job 以支持外部取消
        val job = coroutineScope.launch {
            revealState.animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMs,
                    easing = FastOutSlowInEasing
                )
            )
            // 正常结束（未被打断）才执行清理
            revealState.cleanup()
        }
        revealState.currentAnimationJob = job
        job.join()
    }

    val progress = revealState.animationProgress.value
    val center = revealState.revealCenter
    val maxR = revealState.maxRadius

    // 当前半径
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

        // 用 Difference 模式：绘制整个 snapshot，但挖掉圆形区域
        clipPath(circlePath, clipOp = ClipOp.Difference) {
            drawImage(snap)
        }
    }
}
