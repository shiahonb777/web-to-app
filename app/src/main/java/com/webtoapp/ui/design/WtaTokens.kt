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

/**
 * Button visual variants. Matches Material 3 conventions but keeps naming consistent
 * with the rest of the Wta design system.
 */
@Stable
enum class WtaButtonVariant {
    /** High emphasis, filled primary container */
    Primary,
    /** Medium emphasis, tonal background */
    Tonal,
    /** Medium emphasis, outlined */
    Outlined,
    /** Low emphasis, text only */
    Text,
    /** High emphasis but for destructive actions */
    Destructive
}

@Stable
enum class WtaButtonSize {
    Small,
    Medium,
    Large
}

object WtaSpacing {
    val Tiny: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val ExtraLarge: Dp = 24.dp

    val ScreenHorizontal: Dp = 16.dp
    val ScreenVertical: Dp = 16.dp
    val SectionGap: Dp = 20.dp
    val CardGap: Dp = 12.dp
    val RowHorizontal: Dp = 16.dp
    val RowVertical: Dp = 12.dp
    val ContentGap: Dp = 8.dp
    val IconTextGap: Dp = 12.dp
}

object WtaRadius {
    val Pill: Dp = 999.dp
    val Card: Dp = 14.dp
    val Button: Dp = 10.dp
    val Control: Dp = 10.dp
    val IconPlate: Dp = 10.dp
    val Chip: Dp = 8.dp
    val Badge: Dp = 6.dp
    val Dialog: Dp = 20.dp
}

object WtaSize {
    val Icon: Dp = 20.dp
    val IconSmall: Dp = 16.dp
    val IconLarge: Dp = 24.dp
    val IconPlate: Dp = 36.dp
    val IconPlateLarge: Dp = 44.dp
    val RowMinHeight: Dp = 60.dp
    val RowTrailingMaxWidth: Dp = 148.dp
    val BannerActionMaxWidth: Dp = 132.dp
    val TouchTarget: Dp = 48.dp
    val ButtonHeightSmall: Dp = 36.dp
    val ButtonHeightMedium: Dp = 44.dp
    val ButtonHeightLarge: Dp = 52.dp
    val TextFieldHeight: Dp = 56.dp
    val AvatarSmall: Dp = 32.dp
    val AvatarMedium: Dp = 40.dp
    val AvatarLarge: Dp = 56.dp
}

object WtaElevation {
    /** Flat surface, no shadow */
    val Level0: Dp = 0.dp
    /** Resting card on background */
    val Level1: Dp = 1.dp
    /** Elevated card, interactive surface */
    val Level2: Dp = 3.dp
    /** Floating surface, modal-like */
    val Level3: Dp = 6.dp
    /** Dialog / bottom sheet */
    val Level4: Dp = 12.dp
}

object WtaAlpha {
    const val Disabled = 0.38f
    const val Divider = 0.55f
    const val MutedContainer = 0.08f
    const val PressedContainer = 0.12f
    const val Subtle = 0.16f
    const val Medium = 0.32f
    const val Strong = 0.64f
}
