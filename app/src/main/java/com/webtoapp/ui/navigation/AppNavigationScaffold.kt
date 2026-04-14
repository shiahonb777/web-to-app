package com.webtoapp.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.webtoapp.ui.components.LiquidTabBar
import com.webtoapp.ui.components.LiquidTabItem
import com.webtoapp.ui.components.themedBackground

@Composable
internal fun AppNavigationScaffold(
    dependencies: AppNavigationRootDeps,
) {
    val navController = rememberNavController()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isOnDetailScreen = isDetailRoute(currentRoute)

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (shouldShowBottomBar(currentRoute)) {
                val liquidTabs = remember(com.webtoapp.core.i18n.AppStringsProvider.currentLanguage) {
                    BottomTab.entries.map { tab ->
                        LiquidTabItem(
                            selectedIcon = tab.selectedIcon,
                            unselectedIcon = tab.unselectedIcon,
                            label = tab.label()
                        )
                    }
                }
                LiquidTabBar(
                    tabs = liquidTabs,
                    selectedIndex = selectedTab,
                    onTabSelected = { index ->
                        selectedTab = index
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { scaffoldPadding ->
        val systemBarsInsets = WindowInsets.systemBars
        Box(
            modifier = Modifier
                .fillMaxSize()
                .themedBackground()
                .displayCutoutPadding()
                .padding(
                    start = with(androidx.compose.ui.platform.LocalDensity.current) {
                        systemBarsInsets.getLeft(this, LayoutDirection.Ltr).toDp()
                    },
                    end = with(androidx.compose.ui.platform.LocalDensity.current) {
                        systemBarsInsets.getRight(this, LayoutDirection.Ltr).toDp()
                    }
                )
                .padding(bottom = if (shouldShowBottomBar(currentRoute)) scaffoldPadding.calculateBottomPadding() else 0.dp)
        ) {
            NavigationTabContainer(
                tabIndex = 0,
                selectedTab = selectedTab,
                isOnDetailScreen = isOnDetailScreen,
            ) {
                HomeTabContent(
                    navController = navController,
                    dependencies = dependencies.homeTab,
                )
            }

            NavigationTabContainer(
                tabIndex = 1,
                selectedTab = selectedTab,
                isOnDetailScreen = isOnDetailScreen,
            ) {
                AppStoreTabContent(dependencies = dependencies.storeTab)
            }

            NavigationTabContainer(
                tabIndex = 2,
                selectedTab = selectedTab,
                isOnDetailScreen = isOnDetailScreen,
            ) {
                CommunityTabContent(
                    navController = navController,
                    isTabVisible = selectedTab == 2 && !isOnDetailScreen,
                )
            }

            NavigationTabContainer(
                tabIndex = 3,
                selectedTab = selectedTab,
                isOnDetailScreen = isOnDetailScreen,
            ) {
                AccountTabContent(
                    navController = navController,
                    dependencies = dependencies.accountRoutes,
                    onBackToHome = { selectedTab = 0 },
                )
            }

            NavigationTabContainer(
                tabIndex = 4,
                selectedTab = selectedTab,
                isOnDetailScreen = isOnDetailScreen,
            ) {
                MoreTabContent(navController = navController)
            }

            AppNavigationGraph(
                navController = navController,
                dependencies = dependencies,
            )
        }
    }
}

@Composable
private fun NavigationTabContainer(
    tabIndex: Int,
    selectedTab: Int,
    isOnDetailScreen: Boolean,
    content: @Composable () -> Unit,
) {
    val isActive = selectedTab == tabIndex && !isOnDetailScreen
    val slideOffsetPx = 120f
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "tab${tabIndex}Alpha",
    )
    val offsetX by animateFloatAsState(
        targetValue = if (isActive) 0f else if (tabIndex < selectedTab) -slideOffsetPx else slideOffsetPx,
        animationSpec = spring(
            dampingRatio = 0.9f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "tab${tabIndex}Offset",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (isActive) 1f else 0f)
            .graphicsLayer {
                alpha = animatedAlpha
                translationX = offsetX * density
            }
    ) {
        content()
    }
}
