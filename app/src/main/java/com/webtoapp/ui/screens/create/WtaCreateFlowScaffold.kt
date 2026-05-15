package com.webtoapp.ui.screens.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSpacing

/**
 * Unified scaffold for every "create app" flow. Internally delegates to
 * [WtaScreen] so every create page inherits:
 *  - the edge-swipe-to-go-back gesture
 *  - the scroll-aware top bar with hairline divider
 *  - the transparent background layer
 *  - consistent title typography and padding
 *
 * This is the single source of truth for create-flow top-level layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WtaCreateFlowScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    contentScrollEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()

    WtaScreen(
        title = title,
        modifier = modifier,
        onBack = onBack,
        actions = actions,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (contentScrollEnabled) Modifier.verticalScroll(scrollState) else Modifier)
                .padding(
                    horizontal = WtaSpacing.ScreenHorizontal,
                    vertical = WtaSpacing.ScreenVertical
                ),
            verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)
        ) {
            if (contentScrollEnabled) {
                content()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun WtaCreateFlowSection(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WtaSpacing.CardGap)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        content()
    }
}
