package com.webtoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.webtoapp.R
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSection
import com.webtoapp.ui.design.WtaSectionDivider
import com.webtoapp.ui.design.WtaSettingCard
import com.webtoapp.ui.design.WtaSettingRow
import com.webtoapp.ui.design.WtaSpacing




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
    WtaScreen(title = Strings.tabMore) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = WtaSpacing.ScreenHorizontal,
                    vertical = WtaSpacing.ScreenVertical
                ),
            verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)
        ) {

                WtaSection(title = Strings.moreSectionAiTools) {
                    WtaSettingCard {
                        MoreMenuItem(
                            title = Strings.menuAiCoding,
                            icon = painterResource(R.drawable.ic_sidebar_ai_coding),
                            onClick = onOpenAiCoding
                        )
                        WtaSectionDivider()
                        MoreMenuItem(
                            title = Strings.menuAiSettings,
                            icon = painterResource(R.drawable.ic_sidebar_ai_settings),
                            onClick = onOpenAiSettings
                        )
                    }
                }


                WtaSection(title = Strings.moreSectionDevTools) {
                    WtaSettingCard {
                        MoreMenuItem(
                            title = Strings.menuExtensionModules,
                            icon = painterResource(R.drawable.ic_sidebar_extensions),
                            onClick = onOpenExtensionModules
                        )
                        WtaSectionDivider()
                        MoreMenuItem(
                            title = Strings.menuAppModifier,
                            icon = painterResource(R.drawable.ic_sidebar_app_modifier),
                            onClick = onOpenAppModifier
                        )
                        WtaSectionDivider()
                        MoreMenuItem(
                            title = Strings.menuLinuxEnvironment,
                            icon = painterResource(R.drawable.ic_sidebar_linux),
                            onClick = onOpenLinuxEnvironment
                        )
                        WtaSectionDivider()
                        MoreMenuItem(
                            title = Strings.menuRuntimeDeps,
                            icon = painterResource(R.drawable.ic_sidebar_runtime),
                            onClick = onOpenRuntimeDeps
                        )
                        WtaSectionDivider()
                        MoreMenuItem(
                            title = Strings.menuPortManager,
                            icon = painterResource(R.drawable.ic_sidebar_port),
                            onClick = onOpenPortManager
                        )
                    }
                }


                WtaSection(title = Strings.moreSectionBrowser) {
                    WtaSettingCard {
                        MoreMenuItem(
                            title = Strings.menuBrowserKernel,
                            icon = painterResource(R.drawable.ic_sidebar_browser),
                            onClick = onOpenBrowserKernel
                        )
                        WtaSectionDivider()
                        MoreMenuItem(
                            title = Strings.menuHostsAdBlock,
                            icon = painterResource(R.drawable.ic_sidebar_adblock),
                            onClick = onOpenHostsAdBlock
                        )
                    }
                }

                WtaSection(title = Strings.moreSectionAppearance) {
                    WtaSettingCard {
                        MoreMenuItem(
                            title = Strings.menuStats,
                            icon = painterResource(R.drawable.ic_sidebar_stats),
                            onClick = onOpenStats
                        )
                    }
                }

                WtaSection(title = Strings.about) {
                WtaSettingCard {
                    MoreMenuItem(
                        title = Strings.menuAbout,
                        icon = painterResource(R.drawable.ic_sidebar_about),
                        onClick = onOpenAbout
                    )
                }
                }
        }
    }
}

@Composable
private fun MoreMenuItem(
    title: String,
    icon: Painter,
    onClick: () -> Unit
) {
    WtaSettingRow(
        title = title,
        onClick = onClick,
        iconContent = {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
