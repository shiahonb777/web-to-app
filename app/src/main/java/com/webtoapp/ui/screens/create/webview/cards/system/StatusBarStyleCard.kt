package com.webtoapp.ui.screens.create.webview.cards.system

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.WebViewConfig
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.StatusBarConfigCard

@Composable
fun StatusBarStyleCard(
    webViewConfig: WebViewConfig,
    onWebViewConfigChange: (WebViewConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = AppStringsProvider.current().statusBarStyleConfigLabel,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                val arrowRotation by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                    label = "statusBarArrow"
                )
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { rotationZ = arrowRotation }
                )
            }

            SystemCardExpandContent(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    var selectedTab by remember { mutableIntStateOf(0) }
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(AppStringsProvider.current().statusBarLightModeLabel) },
                            icon = { Icon(Icons.Outlined.LightMode, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(AppStringsProvider.current().statusBarDarkModeLabel) },
                            icon = { Icon(Icons.Outlined.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedTab == 0) {
                        StatusBarConfigCard(
                            config = webViewConfig,
                            onConfigChange = onWebViewConfigChange
                        )
                    } else {
                        val darkMappedConfig = webViewConfig.copy(
                            statusBarColorMode = webViewConfig.statusBarColorModeDark,
                            statusBarColor = webViewConfig.statusBarColorDark,
                            statusBarDarkIcons = webViewConfig.statusBarDarkIconsDark,
                            statusBarBackgroundType = webViewConfig.statusBarBackgroundTypeDark,
                            statusBarBackgroundImage = webViewConfig.statusBarBackgroundImageDark,
                            statusBarBackgroundAlpha = webViewConfig.statusBarBackgroundAlphaDark
                        )
                        StatusBarConfigCard(
                            config = darkMappedConfig,
                            onConfigChange = { changedConfig ->
                                onWebViewConfigChange(
                                    webViewConfig.copy(
                                        statusBarColorModeDark = changedConfig.statusBarColorMode,
                                        statusBarColorDark = changedConfig.statusBarColor,
                                        statusBarDarkIconsDark = changedConfig.statusBarDarkIcons,
                                        statusBarBackgroundTypeDark = changedConfig.statusBarBackgroundType,
                                        statusBarBackgroundImageDark = changedConfig.statusBarBackgroundImage,
                                        statusBarBackgroundAlphaDark = changedConfig.statusBarBackgroundAlpha
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
