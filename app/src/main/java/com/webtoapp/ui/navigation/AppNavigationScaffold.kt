package com.webtoapp.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.webtoapp.core.cloud.AppDownloadManager
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.InstalledItemsTracker
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.stats.AppHealthMonitor
import com.webtoapp.core.stats.BatchImportService
import com.webtoapp.core.stats.WebsiteScreenshotService
import com.webtoapp.data.model.AppType
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.ui.components.LiquidTabBar
import com.webtoapp.ui.components.LiquidTabItem
import com.webtoapp.ui.components.themedBackground
import com.webtoapp.ui.screens.AppStoreScreen
import com.webtoapp.ui.screens.HomeScreen
import com.webtoapp.ui.screens.MoreScreen
import com.webtoapp.ui.screens.community.CommunityScreen
import com.webtoapp.ui.screens.AuthScreen
import com.webtoapp.ui.screens.ProfileScreen
import com.webtoapp.ui.viewmodel.AuthState
import com.webtoapp.ui.viewmodel.AuthViewModel
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun AppNavigationScaffold(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
) {
    val navController = rememberNavController()
    val webAppRepository: WebAppRepository = koinInject()
    val healthMonitor: AppHealthMonitor = koinInject()
    val screenshotService: WebsiteScreenshotService = koinInject()
    val batchImportService: BatchImportService = koinInject()
    val apiClient: CloudApiClient = koinInject()
    val installedItemsTracker: InstalledItemsTracker = koinInject()
    val cloudViewModel: CloudViewModel = koinViewModel()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var previousTab by remember { mutableIntStateOf(0) }

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
                        previousTab = selectedTab
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
                HomeScreen(
                    viewModel = viewModel,
                    healthMonitor = healthMonitor,
                    screenshotService = screenshotService,
                    batchImportService = batchImportService,
                    onCreateApp = {
                        viewModel.createNewApp()
                        navController.navigate(Routes.CREATE_APP)
                    },
                    onCreateMediaApp = { navController.navigate(Routes.CREATE_MEDIA_APP) },
                    onCreateGalleryApp = { navController.navigate(Routes.CREATE_GALLERY_APP) },
                    onCreateHtmlApp = { navController.navigate(Routes.CREATE_HTML_APP) },
                    onCreateFrontendApp = { navController.navigate(Routes.CREATE_FRONTEND_APP) },
                    onCreateNodeJsApp = { navController.navigate(Routes.CREATE_NODEJS_APP) },
                    onCreateWordPressApp = { navController.navigate(Routes.CREATE_WORDPRESS_APP) },
                    onCreatePhpApp = { navController.navigate(Routes.CREATE_PHP_APP) },
                    onCreatePythonApp = { navController.navigate(Routes.CREATE_PYTHON_APP) },
                    onCreateGoApp = { navController.navigate(Routes.CREATE_GO_APP) },
                    onCreateMultiWebApp = { navController.navigate(Routes.CREATE_MULTI_WEB_APP) },
                    onEditApp = { webApp ->
                        viewModel.editApp(webApp)
                        navController.navigate(Routes.editApp(webApp.id))
                    },
                    onEditAppCore = { webApp ->
                        when (webApp.appType) {
                            AppType.WEB -> {
                                viewModel.editApp(webApp)
                                navController.navigate(Routes.editWebApp(webApp.id))
                            }
                            AppType.IMAGE,
                            AppType.VIDEO -> navController.navigate(Routes.editMediaApp(webApp.id))
                            AppType.GALLERY -> navController.navigate(Routes.editGalleryApp(webApp.id))
                            AppType.HTML -> navController.navigate(Routes.editHtmlApp(webApp.id))
                            AppType.FRONTEND -> navController.navigate(Routes.editFrontendApp(webApp.id))
                            AppType.NODEJS_APP -> navController.navigate(Routes.editNodeJsApp(webApp.id))
                            AppType.WORDPRESS -> {
                                viewModel.editApp(webApp)
                                navController.navigate(Routes.editApp(webApp.id))
                            }
                            AppType.PHP_APP -> navController.navigate(Routes.editPhpApp(webApp.id))
                            AppType.PYTHON_APP -> navController.navigate(Routes.editPythonApp(webApp.id))
                            AppType.GO_APP -> navController.navigate(Routes.editGoApp(webApp.id))
                            AppType.MULTI_WEB -> navController.navigate(Routes.editMultiWebApp(webApp.id))
                        }
                    },
                    onPreviewApp = { webApp -> navController.navigate(Routes.preview(webApp.id)) },
                    onOpenAppModifier = { navController.navigate(Routes.APP_MODIFIER) },
                    onOpenAiSettings = { navController.navigate(Routes.AI_SETTINGS) },
                    onOpenAiCoding = { navController.navigate(Routes.AI_CODING) },
                    onOpenAiHtmlCoding = { navController.navigate(Routes.AI_HTML_CODING) },
                    onOpenExtensionModules = { navController.navigate(Routes.EXTENSION_MODULES) },
                    onOpenLinuxEnvironment = { navController.navigate(Routes.LINUX_ENVIRONMENT) },
                )
            }

            NavigationTabContainer(
                tabIndex = 1,
                selectedTab = selectedTab,
                isOnDetailScreen = isOnDetailScreen,
            ) {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                val downloadManager = remember { AppDownloadManager.getInstance(context) }
                AppStoreScreen(
                    cloudViewModel = cloudViewModel,
                    apiClient = apiClient,
                    webAppRepository = webAppRepository,
                    installedTracker = installedItemsTracker,
                    onInstallModule = { shareCode ->
                        coroutineScope.launch {
                            ExtensionManager.getInstance(context).importFromShareCode(shareCode)
                        }
                    },
                    downloadManager = downloadManager
                )
            }

            NavigationTabContainer(
                tabIndex = 2,
                selectedTab = selectedTab,
                isOnDetailScreen = isOnDetailScreen,
            ) {
                CommunityScreen(
                    onNavigateToUser = { userId -> navController.navigate(Routes.communityUser(userId)) },
                    onNavigateToModule = { moduleId -> navController.navigate(Routes.moduleDetail(moduleId)) },
                    onNavigateToPost = { postId -> navController.navigate(Routes.communityPost(postId)) },
                    onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onNavigateToFavorites = { navController.navigate(Routes.FAVORITES) },
                    isTabVisible = selectedTab == 2 && !isOnDetailScreen
                )
            }

            NavigationTabContainer(
                tabIndex = 3,
                selectedTab = selectedTab,
                isOnDetailScreen = isOnDetailScreen,
            ) {
                val authState by authViewModel.authState.collectAsStateWithLifecycle()
                when (authState) {
                    is AuthState.LoggedIn -> {
                        ProfileScreen(
                            authViewModel = authViewModel,
                            onBack = { selectedTab = 0 },
                            onLogout = {},
                            onNavigateDevices = { navController.navigate(Routes.DEVICE_MANAGEMENT) },
                            onNavigateActivationCode = { navController.navigate(Routes.ACTIVATION_CODE) },
                            onNavigateTeams = { navController.navigate(Routes.TEAMS) },
                            onNavigateSubscription = { navController.navigate(Routes.SUBSCRIPTION) }
                        )
                    }
                    is AuthState.LoggedOut -> {
                        AuthScreen(
                            authViewModel = authViewModel,
                            onBack = { selectedTab = 0 },
                            onLoginSuccess = {}
                        )
                    }
                }
            }

            NavigationTabContainer(
                tabIndex = 4,
                selectedTab = selectedTab,
                isOnDetailScreen = isOnDetailScreen,
            ) {
                MoreScreen(
                    onOpenAiCoding = { navController.navigate(Routes.AI_CODING) },
                    onOpenAiSettings = { navController.navigate(Routes.AI_SETTINGS) },
                    onOpenBrowserKernel = { navController.navigate(Routes.BROWSER_KERNEL) },
                    onOpenHostsAdBlock = { navController.navigate(Routes.HOSTS_ADBLOCK) },
                    onOpenAppModifier = { navController.navigate(Routes.APP_MODIFIER) },
                    onOpenExtensionModules = { navController.navigate(Routes.EXTENSION_MODULES) },
                    onOpenLinuxEnvironment = { navController.navigate(Routes.LINUX_ENVIRONMENT) },
                    onOpenRuntimeDeps = { navController.navigate(Routes.RUNTIME_DEPS) },
                    onOpenPortManager = { navController.navigate(Routes.PORT_MANAGER) },
                    onOpenStats = { navController.navigate(Routes.STATS) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) }
                )
            }

            AppNavigationGraph(
                navController = navController,
                viewModel = viewModel,
                authViewModel = authViewModel,
                webAppRepository = webAppRepository
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
