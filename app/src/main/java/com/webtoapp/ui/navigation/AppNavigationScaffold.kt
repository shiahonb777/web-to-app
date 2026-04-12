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
import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.billing.BillingManager
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.InstalledItemsTracker
import com.webtoapp.core.stats.AppHealthMonitor
import com.webtoapp.core.stats.AppStatsRepository
import com.webtoapp.core.stats.BatchImportService
import com.webtoapp.core.stats.WebsiteScreenshotService
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.ui.components.LiquidTabBar
import com.webtoapp.ui.components.LiquidTabItem
import com.webtoapp.ui.components.themedBackground
import com.webtoapp.ui.viewmodel.AuthViewModel
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun AppNavigationScaffold(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
) {
    val navController = rememberNavController()
    val webAppRepository: WebAppRepository = koinInject()
    val statsRepository: AppStatsRepository = koinInject()
    val healthMonitor: AppHealthMonitor = koinInject()
    val screenshotService: WebsiteScreenshotService = koinInject()
    val batchImportService: BatchImportService = koinInject()
    val activationManager: ActivationManager = koinInject()
    val billingManager: BillingManager = koinInject()
    val apiClient: CloudApiClient = koinInject()
    val installedItemsTracker: InstalledItemsTracker = koinInject()
    val cloudViewModel: CloudViewModel = koinViewModel()

    val graphDependencies = remember(
        viewModel,
        authViewModel,
        webAppRepository,
        statsRepository,
        healthMonitor,
        activationManager,
        billingManager,
    ) {
        AppNavigationGraphDependencies(
            viewModel = viewModel,
            authViewModel = authViewModel,
            webAppRepository = webAppRepository,
            statsRepository = statsRepository,
            healthMonitor = healthMonitor,
            activationManager = activationManager,
            billingManager = billingManager,
        )
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isOnDetailScreen = isDetailRoute(currentRoute)

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (shouldShowBottomBar(currentRoute)) {
                val liquidTabs = remember(com.webtoapp.core.i18n.Strings.currentLanguage.value) {
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
                        if (isOnDetailScreen) {
                            navController.popBackStack(TAB_HOST_ROUTE, inclusive = false)
                        }
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
                    viewModel = viewModel,
                    webAppRepository = webAppRepository,
                    healthMonitor = healthMonitor,
                    screenshotService = screenshotService,
                    batchImportService = batchImportService,
                )
            }

            NavigationTabContainer(
                tabIndex = 1,
                selectedTab = selectedTab,
                isOnDetailScreen = isOnDetailScreen,
            ) {
                AppStoreTabContent(
                    navController = navController,
                    webAppRepository = webAppRepository,
                    apiClient = apiClient,
                    installedItemsTracker = installedItemsTracker,
                    cloudViewModel = cloudViewModel,
                )
            }

            NavigationTabContainer(
                tabIndex = 2,
                selectedTab = selectedTab,
                isOnDetailScreen = isOnDetailScreen,
            ) {
                CommunityTabContent(
                    navController = navController,
                    selectedTab = selectedTab,
                    isOnDetailScreen = isOnDetailScreen,
                )
            }

            NavigationTabContainer(
                tabIndex = 3,
                selectedTab = selectedTab,
                isOnDetailScreen = isOnDetailScreen,
            ) {
                AccountTabContent(
                    navController = navController,
                    authViewModel = authViewModel,
                    onBackToHome = { selectedTab = 0 },
                )
            }

            NavigationTabContainer(
                tabIndex = 4,
                selectedTab = selectedTab,
                isOnDetailScreen = isOnDetailScreen,
            ) {
                MoreTabContent(
                    navController = navController,
                )
            }

            AppNavigationGraph(
                navController = navController,
                dependencies = graphDependencies,
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
            stiffness = Spring.StiffnessMedium
        ),
        label = "tab${tabIndex}Alpha"
    )
    val offsetX by animateFloatAsState(
        targetValue = if (isActive) 0f else if (tabIndex < selectedTab) -slideOffsetPx else slideOffsetPx,
        animationSpec = spring(
            dampingRatio = 0.9f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tab${tabIndex}Offset"
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
