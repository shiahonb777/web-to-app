package com.webtoapp.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WtaScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    snackbarHostState: SnackbarHostState? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    contentPadding: PaddingValues = PaddingValues(),
    content: @Composable BoxScope.(PaddingValues) -> Unit
) {
    // A top-bar scroll behaviour is wired through a nestedScroll connection
    // on the scaffold. When the main content (LazyColumn / Column with
    // verticalScroll) is scrolled, this drives the `fraction` used to fade
    // the top bar's background in and to reveal a hairline divider below it.
    // That gives the screen a subtle "content is behind a frosted panel"
    // feeling instead of the abrupt transparent-to-content transition.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Wrap with edge-swipe-to-go-back gesture if a back handler is present.
    // This gives every screen the iOS-signature interactive pop gesture.
    val screenContent: @Composable () -> Unit = {
        Scaffold(
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            snackbarHost = {
                snackbarHostState?.let { state ->
                    SnackbarHost(hostState = state) { data ->
                        WtaSnackbar(
                            message = data.visuals.message,
                            actionLabel = data.visuals.actionLabel,
                            onAction = if (data.visuals.actionLabel != null) {
                                { data.performAction() }
                            } else null,
                            onDismiss = { data.dismiss() }
                        )
                    }
                }
            },
            topBar = {
                WtaTopBar(
                    title = title,
                    subtitle = subtitle,
                    onBack = onBack,
                    actions = actions,
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition
        ) { padding ->
            WtaBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(contentPadding)
                    .imePadding()
            ) {
                content(padding)
            }
        }
    }

    if (onBack != null) {
        WtaSwipeBackContainer(onBack = onBack) {
            screenContent()
        }
    } else {
        screenContent()
    }
}

/**
 * A refined snackbar that sits slightly above the keyboard / nav bar and uses
 * the inverse surface color so it reads as a system-level notice rather than
 * blending into the content. Action buttons are in onSurfaceVariant-of-inverse
 * (a light tone on a dark pill, matching Apple's toast pattern).
 */
@Composable
private fun WtaSnackbar(
    message: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Snackbar(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        containerColor = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        actionContentColor = colors.inversePrimary,
        dismissActionContentColor = colors.inverseOnSurface.copy(alpha = 0.7f),
        action = if (actionLabel != null && onAction != null) {
            {
                androidx.compose.material3.TextButton(
                    onClick = onAction,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = colors.inversePrimary
                    )
                ) { Text(actionLabel) }
            }
        } else null
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WtaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    Column(modifier = modifier) {
        TopAppBar(
            title = {
                if (subtitle.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                if (onBack != null) {
                    WtaIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = Strings.back
                    )
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            scrollBehavior = scrollBehavior
        )

        // Hairline divider that fades in as content scrolls under the top
        // bar. Implemented as a very thin gradient so it dissolves into the
        // content rather than reading as a hard line.
        val fraction = scrollBehavior?.state?.overlappedFraction ?: 0f
        val dividerAlpha = (fraction * 2f).coerceIn(0f, 1f)
        if (dividerAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = dividerAlpha * 0.7f)
                    )
            )
        }
    }
}
