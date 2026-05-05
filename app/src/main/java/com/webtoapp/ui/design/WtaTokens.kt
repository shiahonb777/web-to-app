package com.webtoapp.ui.design

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
enum class WtaCapabilityLevel {
    Common,
    Advanced,
    Lab
}

@Stable
enum class WtaStatusTone {
    Info,
    Success,
    Warning,
    Error
}

@Stable
enum class WtaRowTone {
    Normal,
    Danger
}

@Stable
enum class WtaSectionHeaderStyle {
    Prominent,
    Quiet,
    Hidden
}

object WtaSpacing {
    val ScreenHorizontal: Dp = 16.dp
    val ScreenVertical: Dp = 16.dp
    val SectionGap: Dp = 18.dp
    val CardGap: Dp = 12.dp
    val RowHorizontal: Dp = 14.dp
    val RowVertical: Dp = 12.dp
    val ContentGap: Dp = 8.dp
    val IconTextGap: Dp = 12.dp
}

object WtaRadius {
    val Card: Dp = 8.dp
    val Button: Dp = 4.dp
    val Control: Dp = 8.dp
    val IconPlate: Dp = 8.dp
}

object WtaSize {
    val Icon: Dp = 20.dp
    val IconPlate: Dp = 36.dp
    val RowMinHeight: Dp = 56.dp
    val RowTrailingMaxWidth: Dp = 148.dp
    val BannerActionMaxWidth: Dp = 132.dp
    val TouchTarget: Dp = 48.dp
}

object WtaAlpha {
    const val Disabled = 0.38f
    const val Divider = 0.55f
    const val MutedContainer = 0.08f
    const val PressedContainer = 0.12f
}
