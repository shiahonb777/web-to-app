package com.webtoapp.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.R
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * " " Tab- & settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onOpenAiCoding: () -> Unit = {},
    onOpenAiSettings: () -> Unit = {},

    onOpenBrowserKernel: () -> Unit = {},
    onOpenHostsAdBlock: () -> Unit = {},
    onOpenAppModifier: () -> Unit = {},
    onOpenExtensionModules: () -> Unit = {},
    onOpenLinuxEnvironment: () -> Unit = {},
    onOpenRuntimeDeps: () -> Unit = {},
    onOpenPortManager: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        Strings.tabMore,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                // AI
                MoreSectionTitle(Strings.moreSectionAiTools)
                MoreMenuCard {
                    MoreMenuItem(
                        title = Strings.menuAiCoding,
                        icon = painterResource(R.drawable.ic_sidebar_ai_coding),
                        onClick = onOpenAiCoding
                    )
                    MoreMenuItem(
                        title = Strings.menuAiSettings,
                        icon = painterResource(R.drawable.ic_sidebar_ai_settings),
                        onClick = onOpenAiSettings
                    )
                }

                // Note
                MoreSectionTitle(Strings.moreSectionDevTools)
                MoreMenuCard {
                    MoreMenuItem(
                        title = Strings.menuExtensionModules,
                        icon = painterResource(R.drawable.ic_sidebar_extensions),
                        onClick = onOpenExtensionModules
                    )
                    MoreMenuItem(
                        title = Strings.menuAppModifier,
                        icon = painterResource(R.drawable.ic_sidebar_app_modifier),
                        onClick = onOpenAppModifier
                    )
                    MoreMenuItem(
                        title = Strings.menuLinuxEnvironment,
                        icon = painterResource(R.drawable.ic_sidebar_linux),
                        onClick = onOpenLinuxEnvironment
                    )
                    MoreMenuItem(
                        title = Strings.menuRuntimeDeps,
                        icon = painterResource(R.drawable.ic_sidebar_runtime),
                        onClick = onOpenRuntimeDeps
                    )
                    MoreMenuItem(
                        title = Strings.menuPortManager,
                        icon = painterResource(R.drawable.ic_sidebar_port),
                        onClick = onOpenPortManager
                    )
                }

                // & network
                MoreSectionTitle(Strings.moreSectionBrowser)
                MoreMenuCard {
                    MoreMenuItem(
                        title = Strings.menuBrowserKernel,
                        icon = painterResource(R.drawable.ic_sidebar_browser),
                        onClick = onOpenBrowserKernel
                    )
                    MoreMenuItem(
                        title = Strings.menuHostsAdBlock,
                        icon = painterResource(R.drawable.ic_sidebar_adblock),
                        onClick = onOpenHostsAdBlock
                    )
                }

                // Note
                MoreSectionTitle(Strings.moreSectionAppearance)
                MoreMenuCard {
                    MoreMenuItem(
                        title = Strings.menuStats,
                        icon = painterResource(R.drawable.ic_sidebar_stats),
                        onClick = onOpenStats
                    )
                }

                // Note
                MoreMenuCard {
                    MoreMenuItem(
                        title = Strings.menuAbout,
                        icon = painterResource(R.drawable.ic_sidebar_about),
                        onClick = onOpenAbout
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MoreSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun MoreMenuCard(content: @Composable ColumnScope.() -> Unit) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
private fun MoreMenuItem(
    title: String,
    icon: Painter,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "menuItemScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = title,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
