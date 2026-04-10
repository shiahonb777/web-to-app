package com.webtoapp.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * 自定义 SwipeRefreshLayout，仅当触摸起点在屏幕顶部边缘区域内时才触发下拉刷新。
 * 默认阈值为 48dp，即只有从屏幕最上方 48dp 范围内往下滑才会触发刷新。
 */
class EdgeSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    /** 触摸起点必须在屏幕顶部多少 dp 以内才允许触发刷新 */
    var edgeThresholdDp: Float = 48f

    private var touchStartedInEdge = false

    private val edgeThresholdPx: Float
        get() = edgeThresholdDp * resources.displayMetrics.density

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 判断触摸起点是否在屏幕顶部边缘区域（相对于本 View 的 Y 坐标）
                touchStartedInEdge = ev.y <= edgeThresholdPx
            }
        }
        // 如果触摸不是从顶部边缘开始的，不拦截，让子 View 正常处理
        if (!touchStartedInEdge) return false
        return super.onInterceptTouchEvent(ev)
    }
}
