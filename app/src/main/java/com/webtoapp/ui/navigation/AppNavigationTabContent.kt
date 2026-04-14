package com.webtoapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.WebApp
import com.webtoapp.ui.screens.AppStoreScreen
import com.webtoapp.ui.screens.AuthScreen
import com.webtoapp.ui.screens.HomeScreen
import com.webtoapp.ui.screens.MoreScreen
import com.webtoapp.ui.screens.ProfileScreen
import com.webtoapp.ui.screens.community.CommunityScreen
import com.webtoapp.ui.viewmodel.AuthState
import kotlinx.coroutines.launch

@Composable
internal fun HomeTabContent(
    navController: NavHostController,
    dependencies: HomeTabDeps,
) {
    val viewModel = dependencies.viewModel

    fun navigateToCreate(appType: AppType, prepare: (() -> Unit)? = null) {
        prepare?.invoke()
        navController.navigate(AppFlowSpec.from(appType).createRoute)
    }

    fun navigateToEdit(webApp: WebApp) {
        val flow = AppFlowSpec.from(webApp.appType)
        flow.prepareEdit(viewModel, webApp)
        navController.navigate(flow.editRoute(webApp.id))
    }

    HomeScreen(
        viewModel = viewModel,
        healthMonitor = dependencies.healthMonitor,
        screenshotService = dependencies.screenshotService,
        batchImportService = dependencies.batchImportService,
        onCreateApp = { navigateToCreate(AppType.WEB, viewModel::createNewApp) },
        onCreateMediaApp = { navigateToCreate(AppType.IMAGE) },
        onCreateGalleryApp = { navigateToCreate(AppType.GALLERY) },
        onCreateHtmlApp = { navigateToCreate(AppType.HTML) },
        onCreateFrontendApp = { navigateToCreate(AppType.FRONTEND) },
        onCreateNodeJsApp = { navigateToCreate(AppType.NODEJS_APP) },
        onCreateWordPressApp = { navigateToCreate(AppType.WORDPRESS) },
        onCreatePhpApp = { navigateToCreate(AppType.PHP_APP) },
        onCreatePythonApp = { navigateToCreate(AppType.PYTHON_APP) },
        onCreateGoApp = { navigateToCreate(AppType.GO_APP) },
        onCreateMultiWebApp = { navigateToCreate(AppType.MULTI_WEB) },
        onEditApp = { webApp ->
            viewModel.editApp(webApp)
            navController.navigate(Routes.editApp(webApp.id))
        },
        onEditAppCore = ::navigateToEdit,
        onPreviewApp = { webApp ->
            val flow = AppFlowSpec.from(webApp.appType)
            navController.navigate(flow.previewRoute(webApp.id))
        },
        onOpenAppModifier = { navController.navigate(Routes.APP_MODIFIER) },
        onOpenAiSettings = { navController.navigate(Routes.AI_SETTINGS) },
        onOpenAiCoding = { navController.navigate(Routes.AI_CODING) },
        onOpenAiHtmlCoding = { navController.navigate(Routes.AI_HTML_CODING) },
        onOpenExtensionModules = { navController.navigate(Routes.EXTENSION_MODULES) },
        onOpenLinuxEnvironment = { navController.navigate(Routes.LINUX_ENVIRONMENT) },
    )
}

@Composable
internal fun AppStoreTabContent(
    dependencies: StoreTabDeps,
) {
    val coroutineScope = rememberCoroutineScope()

    AppStoreScreen(
        cloudViewModel = dependencies.cloudViewModel,
        apiClient = dependencies.apiClient,
        webAppRepository = dependencies.webAppRepository,
        installedTracker = dependencies.installedItemsTracker,
        onInstallModule = { shareCode ->
            coroutineScope.launch {
                dependencies.extensionManager.importFromShareCode(shareCode)
            }
        },
        downloadManager = dependencies.downloadManager
    )
}

@Composable
internal fun CommunityTabContent(
    navController: NavHostController,
    isTabVisible: Boolean = true,
) {
    CommunityScreen(
        onNavigateToUser = { userId -> navController.navigate(Routes.communityUser(userId)) },
        onNavigateToModule = { moduleId -> navController.navigate(Routes.moduleDetail(moduleId)) },
        onNavigateToPost = { postId -> navController.navigate(Routes.communityPost(postId)) },
        onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
        onNavigateToFavorites = { navController.navigate(Routes.FAVORITES) },
        isTabVisible = isTabVisible,
    )
}

@Composable
internal fun AccountTabContent(
    navController: NavHostController,
    dependencies: AccountRoutesDeps,
    onBackToHome: () -> Unit,
) {
    val authViewModel = dependencies.authViewModel
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    when (authState) {
        is AuthState.LoggedIn -> {
            ProfileScreen(
                authViewModel = authViewModel,
                onBack = onBackToHome,
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
                onBack = onBackToHome,
                onLoginSuccess = {}
            )
        }
    }
}

@Composable
internal fun MoreTabContent(
    navController: NavHostController,
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
