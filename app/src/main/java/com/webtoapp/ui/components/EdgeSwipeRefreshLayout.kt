package com.webtoapp.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * SwipeRefreshLayout, onlywhen top area pull- to- refresh.
 * default 48dp, from 48dp refresh.
 */
class EdgeSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    /** top dp refresh */
    var edgeThresholdDp: Float = 48f

    private var touchStartedInEdge = false

    private val edgeThresholdPx: Float
        get() = edgeThresholdDp * resources.displayMetrics.density

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // top area( View Y)
                touchStartedInEdge = ev.y <= edgeThresholdPx
            }
        }
        // if fromtop, intercept, View handle
        if (!touchStartedInEdge) return false
        return super.onInterceptTouchEvent(ev)
    }
}
