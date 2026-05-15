package com.webtoapp.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.webtoapp.ui.theme.LocalIsDarkTheme

/**
 * Visual tone of a card. Defaults give a clean, flat look suited to information dense
 * screens; use [Elevated] for hero or interactive items to lift them off the background.
 */
enum class WtaCardTone {
    /** Default. Flat fill matching surface, subtle outline. */
    Surface,
    /** Slightly raised, uses surfaceContainer + soft shadow. */
    Elevated,
    /** Emphasis. Primary container fill. */
    Highlighted,
    /** For warnings/errors, uses error container. */
    Critical
}

/**
 * Primary card container. Drawn by hand rather than using Material3 Card so we
 * can apply:
 *   - A soft layered shadow instead of Material's single hard drop shadow.
 *   - A crisp 0.5dp inner edge for Surface tone that mimics the iOS "pixel
 *     sharp boundary" effect without tipping into Outlined territory.
 *   - Full control of the press state layer (we rely on [wtaPressScale] for
 *     feedback rather than a ripple).
 */
@Composable
fun WtaCard(
    modifier: Modifier = Modifier,
    tone: WtaCardTone = WtaCardTone.Surface,
    contentPadding: PaddingValues = PaddingValues(WtaSpacing.Large),
    shape: Shape = RoundedCornerShape(WtaRadius.Card),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val spec = resolveTone(tone)
    val shadowModifier = if (spec.elevation > 0.dp) {
        Modifier.wtaSoftShadow(shape, spec.elevation)
    } else Modifier

    Box(
        modifier = modifier
            .then(shadowModifier)
            .clip(shape)
            .background(spec.container)
            .then(
                if (border != null || spec.border != null) {
                    Modifier.border(
                        border ?: spec.border!!,
                        shape
                    )
                } else Modifier
            )
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun WtaCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: WtaCardTone = WtaCardTone.Surface,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(WtaSpacing.Large),
    shape: Shape = RoundedCornerShape(WtaRadius.Card),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val spec = resolveTone(tone)
    val interactionSource = remember { MutableInteractionSource() }
    val hapticClick = rememberHapticClick(onClick)
    val shadowModifier = if (spec.elevation > 0.dp) {
        Modifier.wtaSoftShadow(shape, spec.elevation)
    } else Modifier

    Box(
        modifier = modifier
            .then(shadowModifier)
            .clip(shape)
            .background(spec.container)
            .then(
                if (border != null || spec.border != null) {
                    Modifier.border(
                        border ?: spec.border!!,
                        shape
                    )
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = hapticClick
            )
            .wtaPressScale(interactionSource, pressedScale = 0.98f)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

private data class WtaCardSpec(
    val container: Color,
    val border: BorderStroke?,
    val elevation: Dp
)

@Composable
private fun resolveTone(tone: WtaCardTone): WtaCardSpec {
    val colors = MaterialTheme.colorScheme
    val isDark = LocalIsDarkTheme.current
    return when (tone) {
        WtaCardTone.Surface -> WtaCardSpec(
            container = colors.surface,
            border = BorderStroke(
                width = 0.5.dp,
                color = if (isDark) Color.White.copy(alpha = 0.06f)
                else Color.Black.copy(alpha = 0.05f)
            ),
            elevation = WtaElevation.Level1
        )
        WtaCardTone.Elevated -> WtaCardSpec(
            container = colors.surfaceContainer,
            border = null,
            elevation = WtaElevation.Level2
        )
        WtaCardTone.Highlighted -> WtaCardSpec(
            container = colors.primaryContainer,
            border = null,
            elevation = WtaElevation.Level0
        )
        WtaCardTone.Critical -> WtaCardSpec(
            container = colors.errorContainer,
            border = BorderStroke(
                width = 0.5.dp,
                color = colors.error.copy(alpha = WtaAlpha.Subtle)
            ),
            elevation = WtaElevation.Level0
        )
    }
}
