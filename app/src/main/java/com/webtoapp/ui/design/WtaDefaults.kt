package com.webtoapp.ui.design

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Shared defaults for Material 3 primitives used across the app.
 *
 * M3 does not expose a CompositionLocal for default OutlinedTextField or
 * TextField colours, so we provide canonical colour sets here. Call sites
 * that cannot easily migrate to [WtaTextField] (for example, ones using
 * `prefix`, `suffix`, or custom `interactionSource`) should pass
 * `colors = WtaDefaults.outlinedTextFieldColors()` instead of relying on
 * the bare Material defaults.
 *
 * The colour scheme mirrors [WtaTextField]:
 *  - Outline is soft, ~35% alpha unfocused, primary on focus, error on error.
 *  - Container is fully transparent so the field sits cleanly on any card.
 *  - Labels shift from `onSurfaceVariant` to `onSurface` on focus for a
 *    subtle weight change that matches iOS-style inputs.
 */
object WtaDefaults {

    @Composable
    fun outlinedTextFieldColors(): TextFieldColors {
        val colors = MaterialTheme.colorScheme
        return OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outline.copy(alpha = 0.35f),
            disabledBorderColor = colors.outline.copy(alpha = 0.15f),
            errorBorderColor = colors.error,

            cursorColor = colors.primary,
            errorCursorColor = colors.error,

            focusedLabelColor = colors.onSurface,
            unfocusedLabelColor = colors.onSurfaceVariant,
            disabledLabelColor = colors.onSurface.copy(alpha = WtaAlpha.Disabled),
            errorLabelColor = colors.error,

            focusedLeadingIconColor = colors.onSurface,
            unfocusedLeadingIconColor = colors.onSurfaceVariant,
            disabledLeadingIconColor = colors.onSurfaceVariant.copy(alpha = WtaAlpha.Disabled),

            focusedTrailingIconColor = colors.onSurface,
            unfocusedTrailingIconColor = colors.onSurfaceVariant,
            disabledTrailingIconColor = colors.onSurfaceVariant.copy(alpha = WtaAlpha.Disabled),

            focusedSupportingTextColor = colors.onSurfaceVariant,
            unfocusedSupportingTextColor = colors.onSurfaceVariant,
            errorSupportingTextColor = colors.error,

            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent
        )
    }

    /**
     * Filled variant, used by [WtaTextField]. Exposed for call sites that
     * need the same visual but cannot use the Wta wrapper directly.
     */
    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun filledTextFieldColors(): TextFieldColors {
        val colors = MaterialTheme.colorScheme
        return TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainerHigh.copy(alpha = 0.9f),
            unfocusedContainerColor = colors.surfaceContainerHigh.copy(alpha = 0.6f),
            disabledContainerColor = colors.surfaceContainer.copy(alpha = 0.5f),
            errorContainerColor = colors.errorContainer.copy(alpha = 0.5f),

            focusedIndicatorColor = colors.primary,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = colors.error,

            cursorColor = colors.primary,
            errorCursorColor = colors.error,

            focusedLabelColor = colors.onSurface,
            unfocusedLabelColor = colors.onSurfaceVariant,
            disabledLabelColor = colors.onSurface.copy(alpha = WtaAlpha.Disabled),
            errorLabelColor = colors.error,

            focusedLeadingIconColor = colors.onSurface,
            unfocusedLeadingIconColor = colors.onSurfaceVariant,
            focusedTrailingIconColor = colors.onSurface,
            unfocusedTrailingIconColor = colors.onSurfaceVariant
        )
    }
}
