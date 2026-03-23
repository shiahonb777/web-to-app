package com.webtoapp.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Virtual navigation bar配置
 */
data class VirtualNavBarConfig(
    val showBackButton: Boolean = true,
    val showForwardButton: Boolean = true,
    val showRefreshButton: Boolean = true,
    val showHomeButton: Boolean = true,
    val backgroundColor: Color = Color(0xFF1A1A1A),
    val iconColor: Color = Color.White,
    val disabledIconColor: Color = Color(0xFF666666),
    val height: Int = 48,
    val cornerRadius: Int = 24,
    val horizontalPadding: Int = 16,
    val bottomPadding: Int = 8
)

/**
 * Virtual navigation bar - 在强制运行模式下提供应用内导航
 * 
 * 包含：返回、前进、刷新、主页 按钮
 */
@Composable
fun VirtualNavigationBar(
    visible: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    config: VirtualNavBarConfig = VirtualNavBarConfig(),
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = config.horizontalPadding.dp,
                    vertical = config.bottomPadding.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(config.cornerRadius.dp)),
                shape = RoundedCornerShape(config.cornerRadius.dp),
                color = config.backgroundColor
            ) {
                Row(
                    modifier = Modifier
                        .height(config.height.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 返回按钮
                    if (config.showBackButton) {
                        NavButton(
                            icon = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            enabled = canGoBack,
                            onClick = onBack,
                            iconColor = config.iconColor,
                            disabledIconColor = config.disabledIconColor
                        )
                    }
                    
                    // 前进按钮
                    if (config.showForwardButton) {
                        NavButton(
                            icon = Icons.Filled.ArrowForward,
                            contentDescription = "Forward",
                            enabled = canGoForward,
                            onClick = onForward,
                            iconColor = config.iconColor,
                            disabledIconColor = config.disabledIconColor
                        )
                    }
                    
                    // Refresh按钮
                    if (config.showRefreshButton) {
                        NavButton(
                            icon = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            enabled = true,
                            onClick = onRefresh,
                            iconColor = config.iconColor,
                            disabledIconColor = config.disabledIconColor
                        )
                    }
                    
                    // 主页按钮
                    if (config.showHomeButton) {
                        NavButton(
                            icon = Icons.Filled.Home,
                            contentDescription = "主页",
                            enabled = true,
                            onClick = onHome,
                            iconColor = config.iconColor,
                            disabledIconColor = config.disabledIconColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    iconColor: Color,
    disabledIconColor: Color
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) iconColor else disabledIconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * 悬浮式虚拟导航栏 - 可拖动位置
 */
@Composable
fun FloatingVirtualNavigationBar(
    visible: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    config: VirtualNavBarConfig = VirtualNavBarConfig(),
    modifier: Modifier = Modifier
) {
    // 简化版本：固定在底部
    VirtualNavigationBar(
        visible = visible,
        canGoBack = canGoBack,
        canGoForward = canGoForward,
        onBack = onBack,
        onForward = onForward,
        onRefresh = onRefresh,
        onHome = onHome,
        config = config,
        modifier = modifier
    )
}
