package com.webtoapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.webtoapp.core.cloud.AppDownloadManager
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.InstalledItemsTracker
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.stats.AppHealthMonitor
import com.webtoapp.core.stats.BatchImportService
import com.webtoapp.core.stats.WebsiteScreenshotService
import com.webtoapp.data.model.AppType
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.ui.screens.AppStoreScreen
import com.webtoapp.ui.screens.AuthScreen
import com.webtoapp.ui.screens.HomeScreen
import com.webtoapp.ui.screens.MoreScreen
import com.webtoapp.ui.screens.ProfileScreen
import com.webtoapp.ui.screens.community.CommunityScreen
import com.webtoapp.ui.viewmodel.AuthState
import com.webtoapp.ui.viewmodel.AuthViewModel
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
internal fun HomeTabContent(
    navController: NavHostController,
    viewModel: MainViewModel,
    webAppRepository: WebAppRepository,
    healthMonitor: AppHealthMonitor,
    screenshotService: WebsiteScreenshotService,
    batchImportService: BatchImportService,
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

@Composable
internal fun AppStoreTabContent(
    navController: NavHostController,
    webAppRepository: WebAppRepository,
    apiClient: CloudApiClient,
    installedItemsTracker: InstalledItemsTracker,
    cloudViewModel: CloudViewModel,
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

@Composable
internal fun CommunityTabContent(
    navController: NavHostController,
    selectedTab: Int,
    isOnDetailScreen: Boolean,
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

@Composable
internal fun AccountTabContent(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    onBackToHome: () -> Unit,
) {
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
