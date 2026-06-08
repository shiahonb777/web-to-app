package com.webtoapp.ui.shared

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.max
import kotlin.math.min

@Composable
fun AspectRatioSurface(
    videoWidth: Int,
    videoHeight: Int,
    fillScreen: Boolean,
    modifier: Modifier = Modifier,
    onSurfaceCreated: (SurfaceHolder) -> Unit,
    onSurfaceChanged: (SurfaceHolder, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onSurfaceDestroyed: (SurfaceHolder) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val containerWidth = maxWidth
        val containerHeight = maxHeight
        val containerWidthPx = with(density) { containerWidth.toPx() }
        val containerHeightPx = with(density) { containerHeight.toPx() }
        val surfaceModifier = if (
            videoWidth > 0 &&
            videoHeight > 0 &&
            containerWidthPx > 0f &&
            containerHeightPx > 0f
        ) {
            val containerRatio = containerWidthPx / containerHeightPx
            val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()
            val scale = if (fillScreen) {
                max(containerWidthPx / videoWidth, containerHeightPx / videoHeight)
            } else {
                min(containerWidthPx / videoWidth, containerHeightPx / videoHeight)
            }
            val width = with(density) { (videoWidth * scale).toDp() }
            val height = with(density) { (videoHeight * scale).toDp() }
            if (containerRatio > videoRatio && fillScreen) {
                Modifier.requiredWidth(containerWidth).requiredHeight(height)
            } else if (containerRatio < videoRatio && fillScreen) {
                Modifier.requiredWidth(width).requiredHeight(containerHeight)
            } else {
                Modifier.requiredSize(width, height)
            }
        } else {
            Modifier.fillMaxSize()
        }

        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            onSurfaceCreated(holder)
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) {
                            onSurfaceChanged(holder, format, width, height)
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            onSurfaceDestroyed(holder)
                        }
                    })
                }
            },
            modifier = surfaceModifier
        )
    }
}
