package com.webtoapp.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp







val LrcAlignIcon: ImageVector
    get() = _lrcAlignIcon ?: ImageVector.Builder(
        name = "LrcAlign",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {

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


        path(
            fill = SolidColor(Color.Black),
            stroke = null
        ) {

            moveTo(5f, 14f)
            curveTo(5f, 12.8f, 3f, 12.5f, 2.5f, 13.5f)
            curveTo(2f, 14.5f, 3f, 15.8f, 4.5f, 15.5f)
            curveTo(5.5f, 15.2f, 5.5f, 14.5f, 5f, 14f)
            close()
        }


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


        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round
        ) {

            moveTo(10f, 5.5f)
            lineTo(21f, 5.5f)

            moveTo(10f, 9.5f)
            lineTo(19f, 9.5f)

            moveTo(10f, 13.5f)
            lineTo(17f, 13.5f)
        }


        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.4f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(2f, 19.5f)
            lineTo(22f, 19.5f)
        }


        path(
            fill = SolidColor(Color.Black),
            stroke = null
        ) {

            moveTo(6.5f, 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 4.5f, y1 = 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 6.5f, y1 = 19.5f)
            close()

            moveTo(12.5f, 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 10.5f, y1 = 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12.5f, y1 = 19.5f)
            close()

            moveTo(18.5f, 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 16.5f, y1 = 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 18.5f, y1 = 19.5f)
            close()
        }


        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 0.9f,
            strokeLineCap = StrokeCap.Round,
            pathFillType = PathFillType.NonZero
        ) {

            moveTo(5.5f, 16f)
            lineTo(5.5f, 18f)

            moveTo(11.5f, 14.5f)
            lineTo(11.5f, 18f)
        }

    }.build().also { _lrcAlignIcon = it }

private var _lrcAlignIcon: ImageVector? = null
