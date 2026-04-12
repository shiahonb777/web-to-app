package com.webtoapp.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.billing.BillingManager
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.stats.AppHealthMonitor
import com.webtoapp.core.stats.AppStatsRepository
import com.webtoapp.core.stats.OverallStats
import com.webtoapp.data.model.AppType
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.ui.screens.AiCodingScreen
import com.webtoapp.ui.screens.AiHtmlCodingScreen
import com.webtoapp.ui.screens.AiSettingsScreen
import com.webtoapp.ui.screens.AboutScreen
import com.webtoapp.ui.screens.ActivationCodeScreen
import com.webtoapp.ui.screens.AppModifierScreen
import com.webtoapp.ui.screens.BrowserKernelScreen
import com.webtoapp.ui.screens.CreateAppScreen
import com.webtoapp.ui.screens.CreateFrontendAppScreen
import com.webtoapp.ui.screens.CreateGalleryAppScreenV2
import com.webtoapp.ui.screens.CreateGoAppScreen
import com.webtoapp.ui.screens.CreateHtmlAppScreen
import com.webtoapp.ui.screens.CreateMediaAppScreen
import com.webtoapp.ui.screens.CreateMultiWebAppScreen
import com.webtoapp.ui.screens.CreateNodeJsAppScreen
import com.webtoapp.ui.screens.CreatePhpAppScreen
import com.webtoapp.ui.screens.CreatePythonAppScreen
import com.webtoapp.ui.screens.CreateWordPressAppScreen
import com.webtoapp.ui.screens.DeviceManagementScreen
import com.webtoapp.ui.screens.ExtensionModuleScreen
import com.webtoapp.ui.screens.HostsAdBlockScreen
import com.webtoapp.ui.screens.LinuxEnvironmentScreen
import com.webtoapp.ui.screens.ModuleEditorScreen
import com.webtoapp.ui.screens.PortManagerScreen
import com.webtoapp.ui.screens.ProfileScreen
import com.webtoapp.ui.screens.RuntimeDepsScreen
import com.webtoapp.ui.screens.StatsScreen
import com.webtoapp.ui.screens.SubscriptionScreen
import com.webtoapp.ui.screens.TeamScreen
import com.webtoapp.ui.screens.AuthScreen
import com.webtoapp.ui.screens.aimodule.AiModuleDeveloperScreen
import com.webtoapp.ui.screens.community.FavoritesScreen
import com.webtoapp.ui.screens.community.ModuleDetailScreen
import com.webtoapp.ui.screens.community.NotificationsScreen
import com.webtoapp.ui.screens.community.PostDetailScreen
import com.webtoapp.ui.screens.community.UserProfileScreen
import com.webtoapp.ui.viewmodel.AuthViewModel
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.viewmodel.CommunityViewModel
import com.webtoapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
internal fun AppNavigationGraph(
    navController: NavHostController,
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    webAppRepository: WebAppRepository,
) {
    val statsRepository: AppStatsRepository = koinInject()
    val healthMonitor: AppHealthMonitor = koinInject()
    val activationManager: ActivationManager = koinInject()
    val billingManager: BillingManager = koinInject()

    NavHost(
        navController = navController,
        startDestination = TAB_HOST_ROUTE,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it / 3 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 5 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(250))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 5 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it / 3 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(250))
        }
    ) {
        composable(TAB_HOST_ROUTE) {}

        composable(Routes.STATS) {
            val statsScope = rememberCoroutineScope()
            val apps by viewModel.webApps.collectAsStateWithLifecycle()
            val allStats by statsRepository.allStats.collectAsState(initial = emptyList())
            val healthRecords by healthMonitor.allHealthRecords.collectAsState(initial = emptyList())
            var overallStats by remember { mutableStateOf(OverallStats()) }

            LaunchedEffect(Unit) {
                overallStats = statsRepository.getOverallStats()
            }

            StatsScreen(
                apps = apps,
                allStats = allStats,
                healthRecords = healthRecords,
                overallStats = overallStats,
                onBack = { navController.popBackStack() },
                onCheckHealth = { app ->
                    statsScope.launch {
                        healthMonitor.checkUrl(app.id, app.url)
                    }
                },
                onCheckAllHealth = {
                    statsScope.launch {
                        healthMonitor.checkApps(apps)
                        overallStats = statsRepository.getOverallStats()
                    }
                }
            )
        }

        composable(Routes.CREATE_APP) {
            CreateAppScreen(
                viewModel = viewModel,
                activationManager = activationManager,
                isEdit = false,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Routes.CREATE_MEDIA_APP) {
            CreateMediaAppScreen(
                webAppRepository = webAppRepository,
                onBack = { navController.popBackStack() },
                onCreated = { name, appType, mediaUri, mediaConfig, iconUri, themeType ->
                    viewModel.saveMediaApp(
                        name,
                        appType,
                        mediaUri,
                        mediaConfig,
                        iconUri,
                        themeType
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.CREATE_GALLERY_APP) {
            CreateGalleryAppScreenV2(
                webAppRepository = webAppRepository,
                onBack = { navController.popBackStack() },
                onCreated = { name, galleryConfig, iconUri, themeType ->
                    viewModel.saveGalleryApp(name, galleryConfig, iconUri, themeType)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "${Routes.CREATE_HTML_APP}?importDir={importDir}&projectName={projectName}",
            arguments = listOf(
                navArgument("importDir") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("projectName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val importDir = backStackEntry.arguments?.getString("importDir")?.let {
                try {
                    java.net.URLDecoder.decode(it, "UTF-8")
                } catch (_: Exception) {
                    null
                }
            }
            val projectName = backStackEntry.arguments?.getString("projectName")?.let {
                try {
                    java.net.URLDecoder.decode(it, "UTF-8")
                } catch (_: Exception) {
                    null
                }
            }
            CreateHtmlAppScreen(
                webAppRepository = webAppRepository,
                onBack = { navController.popBackStack() },
                onCreated = { name, htmlConfig, iconUri, themeType ->
                    viewModel.saveHtmlApp(name, htmlConfig, iconUri, themeType)
                    navController.popBackStack()
                },
                onZipCreated = { name, extractedDir, entryFile, iconUri, enableJs, enableStorage, landscape ->
                    viewModel.saveZipHtmlApp(
                        name = name,
                        extractedDir = extractedDir,
                        entryFile = entryFile,
                        iconUri = iconUri,
                        enableJavaScript = enableJs,
                        enableLocalStorage = enableStorage,
                        landscapeMode = landscape
                    )
                    navController.popBackStack()
                },
                importDir = importDir,
                importProjectName = projectName
            )
        }

        composable(Routes.CREATE_FRONTEND_APP) {
            CreateFrontendAppScreen(
                webAppRepository = webAppRepository,
                onBack = { navController.popBackStack() },
                onCreated = { name, outputPath, iconUri, framework ->
                    viewModel.saveFrontendApp(
                        name = name,
                        outputPath = outputPath,
                        iconUri = iconUri,
                        framework = framework.name
                    )
                    navController.popBackStack()
                },
                onNavigateToLinuxEnv = {
                    navController.navigate(Routes.LINUX_ENVIRONMENT)
                }
            )
        }

        composable(Routes.CREATE_WORDPRESS_APP) {
            CreateWordPressAppScreen(
                onBack = { navController.popBackStack() },
                onCreated = { name, wordpressConfig, iconUri, themeType ->
                    viewModel.saveWordPressApp(
                        name = name,
                        wordpressConfig = wordpressConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.CREATE_NODEJS_APP) {
            CreateNodeJsAppScreen(
                webAppRepository = webAppRepository,
                onBack = { navController.popBackStack() },
                onCreated = { name, nodejsConfig, iconUri, themeType ->
                    viewModel.saveNodeJsApp(
                        name = name,
                        nodejsConfig = nodejsConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.CREATE_PHP_APP) {
            CreatePhpAppScreen(
                webAppRepository = webAppRepository,
                onBack = { navController.popBackStack() },
                onCreated = { name, phpAppConfig, iconUri, themeType ->
                    viewModel.savePhpApp(
                        name = name,
                        phpAppConfig = phpAppConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.CREATE_PYTHON_APP) {
            CreatePythonAppScreen(
                webAppRepository = webAppRepository,
                onBack = { navController.popBackStack() },
                onCreated = { name, pythonAppConfig, iconUri, themeType ->
                    viewModel.savePythonApp(
                        name = name,
                        pythonAppConfig = pythonAppConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.CREATE_GO_APP) {
            CreateGoAppScreen(
                webAppRepository = webAppRepository,
                onBack = { navController.popBackStack() },
                onCreated = { name, goAppConfig, iconUri, themeType ->
                    viewModel.saveGoApp(
                        name = name,
                        goAppConfig = goAppConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.EDIT_PHP_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreatePhpAppScreen(
                webAppRepository = webAppRepository,
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, phpAppConfig, iconUri, themeType ->
                    viewModel.updatePhpApp(
                        appId = appId,
                        name = name,
                        phpAppConfig = phpAppConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.EDIT_PYTHON_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreatePythonAppScreen(
                webAppRepository = webAppRepository,
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, pythonAppConfig, iconUri, themeType ->
                    viewModel.updatePythonApp(
                        appId = appId,
                        name = name,
                        pythonAppConfig = pythonAppConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.EDIT_GO_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreateGoAppScreen(
                webAppRepository = webAppRepository,
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, goAppConfig, iconUri, themeType ->
                    viewModel.updateGoApp(
                        appId = appId,
                        name = name,
                        goAppConfig = goAppConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.CREATE_MULTI_WEB_APP) {
            CreateMultiWebAppScreen(
                webAppRepository = webAppRepository,
                onBack = { navController.popBackStack() },
                onCreated = { name, multiWebConfig, iconUri, themeType ->
                    viewModel.saveMultiWebApp(
                        name = name,
                        multiWebConfig = multiWebConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.EDIT_MULTI_WEB_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreateMultiWebAppScreen(
                webAppRepository = webAppRepository,
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, multiWebConfig, iconUri, themeType ->
                    viewModel.updateMultiWebApp(
                        appId = appId,
                        name = name,
                        multiWebConfig = multiWebConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.LINUX_ENVIRONMENT) {
            LinuxEnvironmentScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) {
            CreateAppScreen(
                viewModel = viewModel,
                activationManager = activationManager,
                isEdit = true,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT_WEB_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) {
            CreateAppScreen(
                viewModel = viewModel,
                activationManager = activationManager,
                isEdit = true,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT_MEDIA_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreateMediaAppScreen(
                webAppRepository = webAppRepository,
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, appType, mediaUri, mediaConfig, iconUri, themeType ->
                    viewModel.updateMediaApp(
                        appId,
                        name,
                        appType,
                        mediaUri,
                        mediaConfig,
                        iconUri,
                        themeType
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.EDIT_GALLERY_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreateGalleryAppScreenV2(
                webAppRepository = webAppRepository,
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, galleryConfig, iconUri, themeType ->
                    viewModel.updateGalleryApp(appId, name, galleryConfig, iconUri, themeType)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.EDIT_HTML_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreateHtmlAppScreen(
                webAppRepository = webAppRepository,
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, htmlConfig, iconUri, themeType ->
                    viewModel.updateHtmlApp(appId, name, htmlConfig, iconUri, themeType)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.EDIT_FRONTEND_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreateFrontendAppScreen(
                webAppRepository = webAppRepository,
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, outputPath, iconUri, framework ->
                    viewModel.updateFrontendApp(
                        appId,
                        name,
                        outputPath,
                        iconUri,
                        framework.name
                    )
                    navController.popBackStack()
                },
                onNavigateToLinuxEnv = {
                    navController.navigate(Routes.LINUX_ENVIRONMENT)
                }
            )
        }

        composable(
            route = Routes.EDIT_NODEJS_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreateNodeJsAppScreen(
                webAppRepository = webAppRepository,
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, nodejsConfig, iconUri, themeType ->
                    viewModel.updateNodeJsApp(
                        appId = appId,
                        name = name,
                        nodejsConfig = nodejsConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.PREVIEW,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            PreviewScreen(
                appId = appId,
                webAppRepository = webAppRepository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.APP_MODIFIER) {
            AppModifierScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.AI_SETTINGS) {
            AiSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.AI_HTML_CODING) {
            AiHtmlCodingScreen(
                onBack = { navController.popBackStack() },
                onExportToHtmlProject = { files, projectName ->
                    val context = navController.context
                    val tempDir = java.io.File(context.cacheDir, "ai_html_export").apply {
                        if (exists()) deleteRecursively()
                        mkdirs()
                    }

                    files.forEach { file ->
                        java.io.File(tempDir, file.name).writeText(file.content)
                    }

                    navController.navigate(
                        "${Routes.CREATE_HTML_APP}?importDir=${
                            java.net.URLEncoder.encode(tempDir.absolutePath, "UTF-8")
                        }&projectName=${
                            java.net.URLEncoder.encode(projectName, "UTF-8")
                        }"
                    )
                },
                onNavigateToAiSettings = {
                    navController.navigate(Routes.AI_SETTINGS)
                }
            )
        }

        composable(Routes.AI_CODING) {
            AiCodingScreen(
                onBack = { navController.popBackStack() },
                onExportToProject = { files, projectName, codingType ->
                    val context = navController.context
                    val tempDir = java.io.File(context.cacheDir, "ai_coding_export").apply {
                        if (exists()) deleteRecursively()
                        mkdirs()
                    }

                    files.forEach { file ->
                        java.io.File(tempDir, file.name).writeText(file.content)
                    }

                    when (codingType) {
                        com.webtoapp.core.ai.coding.AiCodingType.HTML -> {
                            navController.navigate(
                                "${Routes.CREATE_HTML_APP}?importDir=${
                                    java.net.URLEncoder.encode(tempDir.absolutePath, "UTF-8")
                                }&projectName=${
                                    java.net.URLEncoder.encode(projectName, "UTF-8")
                                }"
                            )
                        }
                        com.webtoapp.core.ai.coding.AiCodingType.FRONTEND -> {
                            navController.navigate(Routes.CREATE_FRONTEND_APP)
                        }
                        com.webtoapp.core.ai.coding.AiCodingType.NODEJS -> {
                            navController.navigate(Routes.CREATE_NODEJS_APP)
                        }
                        com.webtoapp.core.ai.coding.AiCodingType.WORDPRESS -> {
                            navController.navigate(Routes.CREATE_WORDPRESS_APP)
                        }
                        com.webtoapp.core.ai.coding.AiCodingType.PHP -> {
                            navController.navigate(Routes.CREATE_PHP_APP)
                        }
                        com.webtoapp.core.ai.coding.AiCodingType.PYTHON -> {
                            navController.navigate(Routes.CREATE_PYTHON_APP)
                        }
                        com.webtoapp.core.ai.coding.AiCodingType.GO -> {
                            navController.navigate(Routes.CREATE_GO_APP)
                        }
                    }
                },
                onNavigateToAiSettings = {
                    navController.navigate(Routes.AI_SETTINGS)
                }
            )
        }

        composable(Routes.BROWSER_KERNEL) {
            BrowserKernelScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.HOSTS_ADBLOCK) {
            HostsAdBlockScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.RUNTIME_DEPS) {
            RuntimeDepsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PORT_MANAGER) {
            PortManagerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                webAppRepository = webAppRepository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.EXTENSION_MODULES) {
            ExtensionModuleScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { moduleId ->
                    if (moduleId == null) {
                        navController.navigate(Routes.MODULE_EDITOR)
                    } else {
                        navController.navigate(Routes.editModule(moduleId))
                    }
                },
                onNavigateToAiDeveloper = {
                    navController.navigate(Routes.AI_MODULE_DEVELOPER)
                }
            )
        }

        composable(Routes.MODULE_EDITOR) {
            ModuleEditorScreen(
                moduleId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.MODULE_EDITOR_EDIT,
            arguments = listOf(navArgument("moduleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString("moduleId")
            ModuleEditorScreen(
                moduleId = moduleId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.AI_MODULE_DEVELOPER) {
            AiModuleDeveloperScreen(
                onNavigateBack = { navController.popBackStack() },
                onModuleCreated = {
                    navController.popBackStack()
                },
                onNavigateToAiSettings = {
                    navController.navigate(Routes.AI_SETTINGS)
                }
            )
        }

        composable(Routes.AUTH) {
            AuthScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onLoginSuccess = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onLogout = { navController.popBackStack() },
                onNavigateDevices = { navController.navigate(Routes.DEVICE_MANAGEMENT) },
                onNavigateActivationCode = { navController.navigate(Routes.ACTIVATION_CODE) },
                onNavigateSubscription = { navController.navigate(Routes.SUBSCRIPTION) }
            )
        }

        composable(Routes.SUBSCRIPTION) {
            SubscriptionScreen(
                billingManager = billingManager,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ACTIVATION_CODE) {
            val cloudViewModel: CloudViewModel = org.koin.androidx.compose.koinViewModel()
            ActivationCodeScreen(
                cloudViewModel = cloudViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DEVICE_MANAGEMENT) {
            val cloudViewModel: CloudViewModel = org.koin.androidx.compose.koinViewModel()
            DeviceManagementScreen(
                cloudViewModel = cloudViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TEAMS) {
            TeamScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.MODULE_DETAIL,
            arguments = listOf(navArgument("moduleId") { type = NavType.IntType })
        ) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getInt("moduleId") ?: 0
            val communityViewModel: CommunityViewModel = org.koin.androidx.compose.koinViewModel()
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            ModuleDetailScreen(
                moduleId = moduleId,
                communityViewModel = communityViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToUser = { userId -> navController.navigate(Routes.communityUser(userId)) },
                onInstallModule = { shareCode ->
                    coroutineScope.launch {
                        ExtensionManager.getInstance(context).importFromShareCode(shareCode)
                    }
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.USER_PROFILE,
            arguments = listOf(navArgument("userId") { type = NavType.IntType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getInt("userId") ?: 0
            val communityViewModel: CommunityViewModel = org.koin.androidx.compose.koinViewModel()
            UserProfileScreen(
                userId = userId,
                communityViewModel = communityViewModel,
                onBack = { navController.popBackStack() },
                onModuleClick = { moduleId -> navController.navigate(Routes.moduleDetail(moduleId)) },
                onPostClick = { postId -> navController.navigate(Routes.communityPost(postId)) },
                onNavigateToUser = { uid -> navController.navigate(Routes.communityUser(uid)) }
            )
        }

        composable(Routes.FAVORITES) {
            val communityViewModel: CommunityViewModel = org.koin.androidx.compose.koinViewModel()
            FavoritesScreen(
                communityViewModel = communityViewModel,
                onBack = { navController.popBackStack() },
                onModuleClick = { moduleId -> navController.navigate(Routes.moduleDetail(moduleId)) }
            )
        }

        composable(Routes.NOTIFICATIONS) {
            val communityViewModel: CommunityViewModel = org.koin.androidx.compose.koinViewModel()
            NotificationsScreen(
                communityViewModel = communityViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToModule = { moduleId -> navController.navigate(Routes.moduleDetail(moduleId)) },
                onNavigateToUser = { userId -> navController.navigate(Routes.communityUser(userId)) }
            )
        }

        composable(
            route = Routes.COMMUNITY_POST_DETAIL,
            arguments = listOf(navArgument("postId") { type = NavType.IntType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getInt("postId") ?: 0
            val communityViewModel: CommunityViewModel = org.koin.androidx.compose.koinViewModel()
            PostDetailScreen(
                postId = postId,
                communityViewModel = communityViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToUser = { userId -> navController.navigate(Routes.communityUser(userId)) }
            )
        }
    }
}

@Composable
internal fun PreviewScreen(
    appId: Long,
    webAppRepository: WebAppRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val webApp by webAppRepository.getWebAppById(appId).collectAsState(initial = null)
    var hasLaunched by remember { mutableStateOf(false) }

    LaunchedEffect(webApp) {
        val app = webApp
        if (app != null && !hasLaunched) {
            hasLaunched = true

            when (app.appType) {
                AppType.IMAGE,
                AppType.VIDEO -> {
                    com.webtoapp.ui.media.MediaAppActivity.startForPreview(context, app)
                }
                AppType.GALLERY -> {
                    app.galleryConfig?.let { config ->
                        com.webtoapp.ui.gallery.GalleryPlayerActivity.launch(context, config, 0)
                    }
                }
                else -> {
                    com.webtoapp.ui.webview.WebViewActivity.start(context, appId)
                }
            }
            onBack()
        }
    }
}
