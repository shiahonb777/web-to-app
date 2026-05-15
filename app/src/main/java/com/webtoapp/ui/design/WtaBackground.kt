package com.webtoapp.ui.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.MaterialTheme

/**
 * Plain themed background. In the minimal monochrome design, we skip the gradient
 * theatrics of the old ThemedBackgroundBox and just use [MaterialTheme.colorScheme.background].
 * Screens wrapped in WtaScreen get this automatically.
 */
@Composable
fun WtaBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(SolidColor(bg)) },
        content = content
    )
}
