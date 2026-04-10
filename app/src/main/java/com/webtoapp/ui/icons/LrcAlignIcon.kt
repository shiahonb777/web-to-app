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
 * 自定义 LRC 对齐图标
 * 设计理念：左边是一个音符，右边有三条代表歌词文本的线条，
 * 底部有一个时间同步指示（类似时间轴上的标记点），
 * 整体传达"歌词 + 时间同步对齐"的含义
 */
val LrcAlignIcon: ImageVector
    get() = _lrcAlignIcon ?: ImageVector.Builder(
        name = "LrcAlign",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 音符符号 - 左侧竖线（音符杆）
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

        // 音符符号 - 底部椭圆（音符头）
        path(
            fill = SolidColor(Color.Black),
            stroke = null
        ) {
            // 画一个椭圆音符头
            moveTo(5f, 14f)
            curveTo(5f, 12.8f, 3f, 12.5f, 2.5f, 13.5f)
            curveTo(2f, 14.5f, 3f, 15.8f, 4.5f, 15.5f)
            curveTo(5.5f, 15.2f, 5.5f, 14.5f, 5f, 14f)
            close()
        }

        // 音符旗帜（符尾）
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

        // 歌词文本线条 - 三条横线代表歌词行
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round
        ) {
            // 第1行歌词 (长)
            moveTo(10f, 5.5f)
            lineTo(21f, 5.5f)
            // 第2行歌词 (中)
            moveTo(10f, 9.5f)
            lineTo(19f, 9.5f)
            // 第3行歌词 (短)
            moveTo(10f, 13.5f)
            lineTo(17f, 13.5f)
        }

        // 底部时间轴线
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.4f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(2f, 19.5f)
            lineTo(22f, 19.5f)
        }

        // 时间轴上的刻度点 (用小圆弧画圆点)
        path(
            fill = SolidColor(Color.Black),
            stroke = null
        ) {
            // 刻度点1 - 圆心(5.5, 19.5), 半径1
            moveTo(6.5f, 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 4.5f, y1 = 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 6.5f, y1 = 19.5f)
            close()
            // 刻度点2 - 圆心(11.5, 19.5), 半径1
            moveTo(12.5f, 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 10.5f, y1 = 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12.5f, y1 = 19.5f)
            close()
            // 刻度点3 - 圆心(17.5, 19.5), 半径1
            moveTo(18.5f, 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 16.5f, y1 = 19.5f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 18.5f, y1 = 19.5f)
            close()
        }

        // 连接线（歌词行到时间点的同步指示虚线）
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 0.9f,
            strokeLineCap = StrokeCap.Round,
            pathFillType = PathFillType.NonZero
        ) {
            // 连接线1: 音符 -> 时间点1
            moveTo(5.5f, 16f)
            lineTo(5.5f, 18f)
            // 连接线2: 歌词2行末 -> 时间点2
            moveTo(11.5f, 14.5f)
            lineTo(11.5f, 18f)
        }

    }.build().also { _lrcAlignIcon = it }

private var _lrcAlignIcon: ImageVector? = null
