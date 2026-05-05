package com.webtoapp.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout





class EdgeSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {


    var edgeThresholdDp: Float = 48f

    private var touchStartedInEdge = false

    private val edgeThresholdPx: Float
        get() = edgeThresholdDp * resources.displayMetrics.density

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {

                touchStartedInEdge = ev.y <= edgeThresholdPx
            }
        }

        if (!touchStartedInEdge) return false
        return super.onInterceptTouchEvent(ev)
    }
}
