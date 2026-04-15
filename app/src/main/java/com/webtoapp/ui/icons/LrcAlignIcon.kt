package com.webtoapp.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom LRC alignment icon.
 * Design idea: a note on the left and three lyric lines on the right.
 * A timeline marker at the bottom indicates time sync.
 * Overall meaning: lyrics + time-sync alignment.
 */
val LrcAlignIcon: ImageVector
    get() = _lrcAlignIcon ?: ImageVector.Builder(
        name = "LrcAlign",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Note symbol - left stem
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(5f, 4f)
            lineTo(5f, 14f)
        }

        // Note symbol - bottom oval head
        path(
            fill = SolidColor(Color.Black),
            stroke = null
        ) {
            // Draw an oval note head
            moveTo(5f, 14f)
            curveTo(5f, 12.8f, 3f, 12.5f, 2.5f, 13.5f)
            curveTo(2f, 14.5f, 3f, 15.8f, 4.5f, 15.5f)
            curveTo(5.5f, 15.2f, 5.5f, 14.5f, 5f, 14f)
            close()
        }

        // Note flag
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(5f, 4f)
            curveTo(5f, 4f, 8f, 5f, 8f, 7.5f)
        }

        // Lyric lines - three horizontal bars
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round
        ) {
            // Lyric line 1 (long)
            moveTo(10f, 5.5f)
            lineTo(21f, 5.5f)
            // Lyric line 2 (medium)
            moveTo(10f, 9.5f)
            lineTo(19f, 9.5f)
            // Lyric line 3 (short)
            moveTo(10f, 13.5f)
            lineTo(17f, 13.5f)
        }

        // Bottom timeline
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.4f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(2f, 19.5f)
            lineTo(22f, 19.5f)
        }

        // Timeline ticks (small arc circles)
        path(
            fill = SolidColor(Color.Black),
            stroke = null
        ) {
            // Tick 1 - center (5.5, 19.5), radius 1
            moveTo(6.5f, 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 4.5f, y1 = 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 6.5f, y1 = 19.5f)
            close()
            // Tick 2 - center (11.5, 19.5), radius 1
            moveTo(12.5f, 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 10.5f, y1 = 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12.5f, y1 = 19.5f)
            close()
            // Tick 3 - center (17.5, 19.5), radius 1
            moveTo(18.5f, 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 16.5f, y1 = 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 18.5f, y1 = 19.5f)
            close()
        }

        // Sync guide lines from lyrics to timeline
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 0.9f,
            strokeLineCap = StrokeCap.Round,
            pathFillType = PathFillType.NonZero
        ) {
            // Line 1: note -> tick 1
            moveTo(5.5f, 16f)
            lineTo(5.5f, 18f)
            // Line 2: lyric line 2 end -> tick 2
            moveTo(11.5f, 14.5f)
            lineTo(11.5f, 18f)
        }

    }.build().also { _lrcAlignIcon = it }

private var _lrcAlignIcon: ImageVector? = null
