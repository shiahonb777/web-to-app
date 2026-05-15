package com.webtoapp.ui.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.webtoapp.ui.theme.LocalIsDarkTheme

/**
 * Semantic accent colors. These are deliberately kept muted so they work with the
 * monochrome base palette without feeling candy-coloured. Use them for state
 * indicators (success, warning, error, neutral) rather than decoration.
 *
 * Access via [WtaColors.semantic].
 */
@Stable
data class WtaSemanticPalette(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,

    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,

    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,

    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,

    val neutral: Color,
    val onNeutral: Color,
    val neutralContainer: Color,
    val onNeutralContainer: Color,
)

private val LightSemantic = WtaSemanticPalette(
    success = Color(0xFF2D6A3E),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFE3F0E6),
    onSuccessContainer = Color(0xFF123F20),

    warning = Color(0xFF8A5A00),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFBEFD7),
    onWarningContainer = Color(0xFF3E2900),

    error = Color(0xFF8A1D1D),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9E4E4),
    onErrorContainer = Color(0xFF3B0F0F),

    info = Color(0xFF20486E),
    onInfo = Color(0xFFFFFFFF),
    infoContainer = Color(0xFFE1EBF5),
    onInfoContainer = Color(0xFF0F2236),

    neutral = Color(0xFF6A6A6E),
    onNeutral = Color(0xFFFFFFFF),
    neutralContainer = Color(0xFFEDEDEF),
    onNeutralContainer = Color(0xFF2A2A2D),
)

private val DarkSemantic = WtaSemanticPalette(
    success = Color(0xFF9BD0A5),
    onSuccess = Color(0xFF0E2414),
    successContainer = Color(0xFF1A3A24),
    onSuccessContainer = Color(0xFFC9E6CF),

    warning = Color(0xFFE6B865),
    onWarning = Color(0xFF2B1B00),
    warningContainer = Color(0xFF3D2B08),
    onWarningContainer = Color(0xFFF2DAB0),

    error = Color(0xFFE7A3A3),
    onError = Color(0xFF1A0808),
    errorContainer = Color(0xFF3B1515),
    onErrorContainer = Color(0xFFF4D4D4),

    info = Color(0xFFA7C0DB),
    onInfo = Color(0xFF0F2236),
    infoContainer = Color(0xFF1D3349),
    onInfoContainer = Color(0xFFD8E4F1),

    neutral = Color(0xFF9D9DA1),
    onNeutral = Color(0xFF1A1A1D),
    neutralContainer = Color(0xFF2A2A2E),
    onNeutralContainer = Color(0xFFDCDCE0),
)

/**
 * Accessor namespace for Wta design system colors. Mirrors [MaterialTheme.colorScheme]
 * but adds semantic accents on top for states that M3 does not express directly.
 */
object WtaColors {
    val semantic: WtaSemanticPalette
        @Composable
        @ReadOnlyComposable
        get() = if (LocalIsDarkTheme.current) DarkSemantic else LightSemantic
}
