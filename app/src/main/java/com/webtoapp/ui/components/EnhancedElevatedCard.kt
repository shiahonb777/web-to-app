package com.webtoapp.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.webtoapp.ui.design.WtaCard
import com.webtoapp.ui.design.WtaCardTone
import com.webtoapp.ui.design.WtaRadius

/**
 * Legacy name for a default [WtaCard]. Behaviour is identical to the surface-toned
 * Wta card with no inner padding (legacy call sites supply their own padding).
 * Retained as a thin alias so existing screens do not need to be edited.
 */
@Composable
fun EnhancedElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(WtaRadius.Card),
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    WtaCard(
        modifier = modifier,
        tone = WtaCardTone.Surface,
        contentPadding = PaddingValues(0.dp),
        shape = shape,
        content = content
    )
}

@Composable
fun EnhancedElevatedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(WtaRadius.Card),
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    containerColor: Color? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit
) {
    WtaCard(
        onClick = onClick,
        modifier = modifier,
        tone = WtaCardTone.Surface,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        shape = shape,
        content = content
    )
}

/**
 * Legacy name for a [WtaCard] with [WtaCardTone.Elevated]. Retained as an alias.
 */
@Composable
fun EnhancedOutlinedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(WtaRadius.Card),
    containerColor: Color? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit
) {
    WtaCard(
        onClick = onClick,
        modifier = modifier,
        tone = WtaCardTone.Elevated,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        shape = shape,
        content = content
    )
}
