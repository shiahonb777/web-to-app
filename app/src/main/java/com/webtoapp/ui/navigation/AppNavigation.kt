package com.webtoapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.i18n.InitializeLanguage
import com.webtoapp.ui.screens.AboutScreen
import com.webtoapp.ui.screens.AiCodingScreen
import com.webtoapp.ui.screens.AiHtmlCodingScreen
import com.webtoapp.ui.screens.AiSettingsScreen
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
import com.webtoapp.ui.screens.CreateOfflinePackScreen
import com.webtoapp.ui.screens.CreatePhpAppScreen
import com.webtoapp.ui.screens.CreatePythonAppScreen
import com.webtoapp.ui.screens.CreateWordPressAppScreen
import com.webtoapp.ui.screens.ExtensionModuleScreen
import com.webtoapp.ui.screens.HomeScreen
import com.webtoapp.ui.screens.HostsAdBlockScreen
import com.webtoapp.ui.screens.LinuxEnvironmentScreen
import com.webtoapp.ui.screens.ModuleEditorScreen
import com.webtoapp.ui.screens.ModuleMarketScreen
import com.webtoapp.ui.screens.MoreScreen
import com.webtoapp.ui.screens.PortManagerScreen
import com.webtoapp.ui.screens.RuntimeDepsScreen
import com.webtoapp.ui.screens.StatsScreen
import com.webtoapp.ui.screens.aimodule.AiModuleDeveloperScreen
import com.webtoapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

object Routes {
    const val HOME = "home"
    const val MORE = "more"

    const val CREATE_APP = "create_app"
    const val CREATE_MEDIA_APP = "create_media_app"
    const val CREATE_GALLERY_APP = "create_gallery_app"
    const val CREATE_HTML_APP = "create_html_app"
    const val CREATE_FRONTEND_APP = "create_frontend_app"
    const val CREATE_NODEJS_APP = "create_nodejs_app"
    const val CREATE_WORDPRESS_APP = "create_wordpress_app"
    const val CREATE_PHP_APP = "create_php_app"
    const val CREATE_PYTHON_APP = "create_python_app"
    const val CREATE_GO_APP = "create_go_app"
    const val CREATE_MULTI_WEB_APP = "create_multi_web_app"
    const val CREATE_OFFLINE_PACK = "create_offline_pack"
    const val LINUX_ENVIRONMENT = "linux_environment"

    const val EDIT_APP = "edit_app/{appId}"
    const val EDIT_WEB_APP = "edit_web_app/{appId}"
    const val EDIT_MEDIA_APP = "edit_media_app/{appId}"
    const val EDIT_GALLERY_APP = "edit_gallery_app/{appId}"
    const val EDIT_HTML_APP = "edit_html_app/{appId}"
    const val EDIT_FRONTEND_APP = "edit_frontend_app/{appId}"
    const val EDIT_NODEJS_APP = "edit_nodejs_app/{appId}"
    const val EDIT_PHP_APP = "edit_php_app/{appId}"
    const val EDIT_PYTHON_APP = "edit_python_app/{appId}"
    const val EDIT_GO_APP = "edit_go_app/{appId}"
    const val EDIT_MULTI_WEB_APP = "edit_multi_web_app/{appId}"

    const val PREVIEW = "preview/{appId}"
    const val APP_MODIFIER = "app_modifier"
    const val AI_SETTINGS = "ai_settings"
    const val AI_CODING = "ai_coding"
    const val AI_HTML_CODING = "ai_html_coding"
    const val AI_CODING_V2 = "ai_coding_v2"
    const val BROWSER_KERNEL = "browser_kernel"
    const val HOSTS_ADBLOCK = "hosts_adblock"
    const val EXTENSION_MODULES = "extension_modules"
    const val MODULE_MARKET = "module_market"
    const val MODULE_EDITOR = "module_editor"
    const val MODULE_EDITOR_EDIT = "module_editor/{moduleId}"
    const val AI_MODULE_DEVELOPER = "ai_module_developer"
    const val RUNTIME_DEPS = "runtime_deps"
    const val PORT_MANAGER = "port_manager"
    const val STATS = "stats"
    const val ABOUT = "about"

    fun editApp(appId: Long) = "edit_app/$appId"
    fun editWebApp(appId: Long) = "edit_web_app/$appId"
    fun editMediaApp(appId: Long) = "edit_media_app/$appId"
    fun editGalleryApp(appId: Long) = "edit_gallery_app/$appId"
    fun editHtmlApp(appId: Long) = "edit_html_app/$appId"
    fun editFrontendApp(appId: Long) = "edit_frontend_app/$appId"
    fun editNodeJsApp(appId: Long) = "edit_nodejs_app/$appId"
    fun editPhpApp(appId: Long) = "edit_php_app/$appId"
    fun editPythonApp(appId: Long) = "edit_python_app/$appId"
    fun editGoApp(appId: Long) = "edit_go_app/$appId"
    fun editMultiWebApp(appId: Long) = "edit_multi_web_app/$appId"
    fun preview(appId: Long) = "preview/$appId"
    fun editModule(moduleId: String) = "module_editor/$moduleId"
}

@Composable
fun AppNavigation() {
    InitializeLanguage()

    val navController = rememberNavController()
    val viewModel: MainViewModel = koinViewModel()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize(),
            enterTransition = com.webtoapp.ui.animation.pageEnterTransition,
            exitTransition = com.webtoapp.ui.animation.pageExitTransition,
            popEnterTransition = com.webtoapp.ui.animation.pagePopEnterTransition,
            popExitTransition = com.webtoapp.ui.animation.pagePopExitTransition
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = viewModel,
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
                        onCreateOfflinePack = { navController.navigate(Routes.CREATE_OFFLINE_PACK) },
                        onEditApp = { webApp ->
                            viewModel.editApp(webApp)
                            navController.navigate(Routes.editApp(webApp.id))
                        },
                        onEditAppCore = { webApp ->
                            when (webApp.appType) {
                                com.webtoapp.data.model.AppType.WEB -> {
                                    viewModel.editApp(webApp)
                                    navController.navigate(Routes.editWebApp(webApp.id))
                                }
                                com.webtoapp.data.model.AppType.IMAGE,
                                com.webtoapp.data.model.AppType.VIDEO -> navController.navigate(Routes.editMediaApp(webApp.id))
                                com.webtoapp.data.model.AppType.GALLERY -> navController.navigate(Routes.editGalleryApp(webApp.id))
                                com.webtoapp.data.model.AppType.HTML -> navController.navigate(Routes.editHtmlApp(webApp.id))
                                com.webtoapp.data.model.AppType.FRONTEND -> navController.navigate(Routes.editFrontendApp(webApp.id))
                                com.webtoapp.data.model.AppType.NODEJS_APP -> navController.navigate(Routes.editNodeJsApp(webApp.id))
                                com.webtoapp.data.model.AppType.WORDPRESS -> {
                                    viewModel.editApp(webApp)
                                    navController.navigate(Routes.editApp(webApp.id))
                                }
                                com.webtoapp.data.model.AppType.PHP_APP -> navController.navigate(Routes.editPhpApp(webApp.id))
                                com.webtoapp.data.model.AppType.PYTHON_APP -> navController.navigate(Routes.editPythonApp(webApp.id))
                                com.webtoapp.data.model.AppType.GO_APP -> navController.navigate(Routes.editGoApp(webApp.id))
                                com.webtoapp.data.model.AppType.MULTI_WEB -> navController.navigate(Routes.editMultiWebApp(webApp.id))
                            }
                        },
                        onPreviewApp = { webApp -> navController.navigate(Routes.preview(webApp.id)) },
                        onOpenAppModifier = { navController.navigate(Routes.APP_MODIFIER) },
                        onOpenAiSettings = { navController.navigate(Routes.AI_SETTINGS) },
                        onOpenAiCoding = { navController.navigate(Routes.AI_CODING) },
                        onOpenAiHtmlCoding = { navController.navigate(Routes.AI_HTML_CODING) },
                        onOpenExtensionModules = { navController.navigate(Routes.EXTENSION_MODULES) },
                        onOpenLinuxEnvironment = { navController.navigate(Routes.LINUX_ENVIRONMENT) },
                        onOpenBrowserKernel = { navController.navigate(Routes.BROWSER_KERNEL) },
                        onOpenHostsAdBlock = { navController.navigate(Routes.HOSTS_ADBLOCK) },
                        onOpenRuntimeDeps = { navController.navigate(Routes.RUNTIME_DEPS) },
                        onOpenPortManager = { navController.navigate(Routes.PORT_MANAGER) },
                        onOpenStats = { navController.navigate(Routes.STATS) },
                        onOpenAbout = { navController.navigate(Routes.ABOUT) }
                    )
            }

            composable(Routes.STATS) {
                val statsRepository: com.webtoapp.core.stats.AppStatsRepository =
                    org.koin.java.KoinJavaComponent.get(com.webtoapp.core.stats.AppStatsRepository::class.java)
                val healthMonitor: com.webtoapp.core.stats.AppHealthMonitor =
                    org.koin.java.KoinJavaComponent.get(com.webtoapp.core.stats.AppHealthMonitor::class.java)
                val statsScope = androidx.compose.runtime.rememberCoroutineScope()

                val apps by viewModel.webApps.collectAsStateWithLifecycle()
                val allStats by statsRepository.allStats.collectAsState(initial = emptyList())
                val healthRecords by healthMonitor.allHealthRecords.collectAsState(initial = emptyList())
                val overallStats = remember { androidx.compose.runtime.mutableStateOf(com.webtoapp.core.stats.OverallStats()) }

                LaunchedEffect(Unit) {
                    overallStats.value = statsRepository.getOverallStats()
                }

                StatsScreen(
                    apps = apps,
                    allStats = allStats,
                    healthRecords = healthRecords,
                    overallStats = overallStats.value,
                    onBack = { navController.popBackStack() },
                    onCheckHealth = { app ->
                        statsScope.launch {
                            healthMonitor.checkUrl(app.id, app.url)
                        }
                    },
                    onCheckAllHealth = {
                        statsScope.launch {
                            healthMonitor.checkApps(apps)
                            overallStats.value = statsRepository.getOverallStats()
                        }
                    }
                )
            }

            composable(Routes.CREATE_APP) {
                CreateAppScreen(
                    viewModel = viewModel,
                    isEdit = false,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(Routes.CREATE_MEDIA_APP) {
                CreateMediaAppScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { name, appType, mediaUri, mediaConfig, iconUri, themeType ->
                        viewModel.saveMediaApp(name, appType, mediaUri, mediaConfig, iconUri, themeType)
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.CREATE_GALLERY_APP) {
                CreateGalleryAppScreenV2(
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
                    onNavigateToLinuxEnv = { navController.navigate(Routes.LINUX_ENVIRONMENT) }
                )
            }

            composable(Routes.CREATE_WORDPRESS_APP) {
                CreateWordPressAppScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { name, wordpressConfig, iconUri, themeType ->
                        viewModel.saveWordPressApp(name, wordpressConfig, iconUri, themeType)
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.CREATE_NODEJS_APP) {
                CreateNodeJsAppScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { name, nodejsConfig, iconUri, themeType ->
                        viewModel.saveNodeJsApp(name, nodejsConfig, iconUri, themeType)
                        navController.popBackStack()
                    },
                    onOpenLinuxEnv = { navController.navigate(Routes.LINUX_ENVIRONMENT) },
                )
            }

            composable(Routes.CREATE_PHP_APP) {
                CreatePhpAppScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { name, phpAppConfig, iconUri, themeType ->
                        viewModel.savePhpApp(name, phpAppConfig, iconUri, themeType)
                        navController.popBackStack()
                    },
                    onOpenLinuxEnv = { navController.navigate(Routes.LINUX_ENVIRONMENT) },
                )
            }

            composable(Routes.CREATE_PYTHON_APP) {
                CreatePythonAppScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { name, pythonAppConfig, iconUri, themeType ->
                        viewModel.savePythonApp(name, pythonAppConfig, iconUri, themeType)
                        navController.popBackStack()
                    },
                    onOpenLinuxEnv = { navController.navigate(Routes.LINUX_ENVIRONMENT) },
                )
            }

            composable(Routes.CREATE_GO_APP) {
                CreateGoAppScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { name, goAppConfig, iconUri, themeType ->
                        viewModel.saveGoApp(name, goAppConfig, iconUri, themeType)
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
                    existingAppId = appId,
                    onBack = { navController.popBackStack() },
                    onCreated = { name, phpAppConfig, iconUri, themeType ->
                        viewModel.updatePhpApp(appId, name, phpAppConfig, iconUri, themeType)
                        navController.popBackStack()
                    },
                    onOpenLinuxEnv = { navController.navigate(Routes.LINUX_ENVIRONMENT) },
                )
            }

            composable(
                route = Routes.EDIT_PYTHON_APP,
                arguments = listOf(navArgument("appId") { type = NavType.LongType })
            ) { backStackEntry ->
                val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
                CreatePythonAppScreen(
                    existingAppId = appId,
                    onBack = { navController.popBackStack() },
                    onCreated = { name, pythonAppConfig, iconUri, themeType ->
                        viewModel.updatePythonApp(appId, name, pythonAppConfig, iconUri, themeType)
                        navController.popBackStack()
                    },
                    onOpenLinuxEnv = { navController.navigate(Routes.LINUX_ENVIRONMENT) },
                )
            }

            composable(
                route = Routes.EDIT_GO_APP,
                arguments = listOf(navArgument("appId") { type = NavType.LongType })
            ) { backStackEntry ->
                val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
                CreateGoAppScreen(
                    existingAppId = appId,
                    onBack = { navController.popBackStack() },
                    onCreated = { name, goAppConfig, iconUri, themeType ->
                        viewModel.updateGoApp(appId, name, goAppConfig, iconUri, themeType)
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.CREATE_MULTI_WEB_APP) {
                CreateMultiWebAppScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { name, multiWebConfig, iconUri, themeType ->
                        viewModel.saveMultiWebApp(name, multiWebConfig, iconUri, themeType)
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
                    existingAppId = appId,
                    onBack = { navController.popBackStack() },
                    onCreated = { name, multiWebConfig, iconUri, themeType ->
                        viewModel.updateMultiWebApp(appId, name, multiWebConfig, iconUri, themeType)
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.CREATE_OFFLINE_PACK) {
                CreateOfflinePackScreen(
                    onBack = { navController.popBackStack() },
                    onStartScrape = { name, url, maxDepth, downloadCdn, followLinks, maxFiles, maxTotalSizeMb, skipPatterns, timeoutSeconds, onProgress ->
                        viewModel.saveScrapedWebsiteApp(
                            name = name,
                            url = url,
                            iconUri = null,
                            maxDepth = maxDepth,
                            downloadCdnResources = downloadCdn,
                            followLinks = followLinks,
                            maxFiles = maxFiles,
                            maxTotalSizeMb = maxTotalSizeMb,
                            skipPatterns = skipPatterns,
                            timeoutSeconds = timeoutSeconds,
                            onProgress = onProgress
                        )
                    }
                )
            }

            composable(Routes.LINUX_ENVIRONMENT) {
                LinuxEnvironmentScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.EDIT_APP,
                arguments = listOf(navArgument("appId") { type = NavType.LongType })
            ) {
                CreateAppScreen(
                    viewModel = viewModel,
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
                    existingAppId = appId,
                    onBack = { navController.popBackStack() },
                    onCreated = { name, appType, mediaUri, mediaConfig, iconUri, themeType ->
                        viewModel.updateMediaApp(appId, name, appType, mediaUri, mediaConfig, iconUri, themeType)
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
                    existingAppId = appId,
                    onBack = { navController.popBackStack() },
                    onCreated = { name, outputPath, iconUri, framework ->
                        viewModel.updateFrontendApp(appId, name, outputPath, iconUri, framework.name)
                        navController.popBackStack()
                    },
                    onNavigateToLinuxEnv = { navController.navigate(Routes.LINUX_ENVIRONMENT) }
                )
            }

            composable(
                route = Routes.EDIT_NODEJS_APP,
                arguments = listOf(navArgument("appId") { type = NavType.LongType })
            ) { backStackEntry ->
                val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
                CreateNodeJsAppScreen(
                    existingAppId = appId,
                    onBack = { navController.popBackStack() },
                    onCreated = { name, nodejsConfig, iconUri, themeType ->
                        viewModel.updateNodeJsApp(appId, name, nodejsConfig, iconUri, themeType)
                        navController.popBackStack()
                    },
                    onOpenLinuxEnv = { navController.navigate(Routes.LINUX_ENVIRONMENT) },
                )
            }

            composable(
                route = Routes.PREVIEW,
                arguments = listOf(navArgument("appId") { type = NavType.LongType })
            ) { backStackEntry ->
                val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
                PreviewScreen(appId = appId, onBack = { navController.popBackStack() })
            }

            composable(Routes.APP_MODIFIER) {
                AppModifierScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.AI_SETTINGS) {
                AiSettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.AI_HTML_CODING) {
                AiHtmlCodingScreen(
                    onBack = { navController.popBackStack() },
                    onExportToHtmlProject = { files, projectName ->
                        val navContext = navController.context
                        val tempDir = java.io.File(navContext.cacheDir, "ai_html_export").apply {
                            if (exists()) deleteRecursively()
                            mkdirs()
                        }
                        files.forEach { file ->
                            java.io.File(tempDir, file.name).writeText(file.content)
                        }
                        navController.navigate(
                            "${Routes.CREATE_HTML_APP}?importDir=${java.net.URLEncoder.encode(tempDir.absolutePath, "UTF-8")}&projectName=${java.net.URLEncoder.encode(projectName, "UTF-8")}"
                        )
                    },
                    onNavigateToAiSettings = { navController.navigate(Routes.AI_SETTINGS) }
                )
            }

            composable(Routes.AI_CODING) {
                AiCodingScreen(
                    onBack = { navController.popBackStack() },
                    onExportToProject = { files, projectName, codingType ->
                        val navContext = navController.context
                        val tempDir = java.io.File(navContext.cacheDir, "ai_coding_export").apply {
                            if (exists()) deleteRecursively()
                            mkdirs()
                        }

                        files.forEach { file ->
                            java.io.File(tempDir, file.name).writeText(file.content)
                        }

                        when (codingType) {
                            com.webtoapp.core.ai.coding.AiCodingType.HTML -> {
                                navController.navigate(
                                    "${Routes.CREATE_HTML_APP}?importDir=${java.net.URLEncoder.encode(tempDir.absolutePath, "UTF-8")}&projectName=${java.net.URLEncoder.encode(projectName, "UTF-8")}"
                                )
                            }
                            com.webtoapp.core.ai.coding.AiCodingType.FRONTEND -> navController.navigate(Routes.CREATE_FRONTEND_APP)
                            com.webtoapp.core.ai.coding.AiCodingType.NODEJS -> navController.navigate(Routes.CREATE_NODEJS_APP)
                            com.webtoapp.core.ai.coding.AiCodingType.WORDPRESS -> navController.navigate(Routes.CREATE_WORDPRESS_APP)
                            com.webtoapp.core.ai.coding.AiCodingType.PHP -> navController.navigate(Routes.CREATE_PHP_APP)
                            com.webtoapp.core.ai.coding.AiCodingType.PYTHON -> navController.navigate(Routes.CREATE_PYTHON_APP)
                            com.webtoapp.core.ai.coding.AiCodingType.GO -> navController.navigate(Routes.CREATE_GO_APP)
                        }
                    },
                    onNavigateToAiSettings = { navController.navigate(Routes.AI_SETTINGS) }
                )
            }

            composable(Routes.BROWSER_KERNEL) {
                BrowserKernelScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.AI_CODING_V2) {
                com.webtoapp.core.ai.v2.ui.AiCodingV2Screen(
                    onBack = { navController.popBackStack() },
                    onOpenAiSettings = { navController.navigate(Routes.AI_SETTINGS) }
                )
            }

            composable(Routes.HOSTS_ADBLOCK) {
                HostsAdBlockScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.RUNTIME_DEPS) {
                RuntimeDepsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PORT_MANAGER) {
                PortManagerScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
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
                    onNavigateToAiDeveloper = { navController.navigate(Routes.AI_MODULE_DEVELOPER) },
                    onNavigateToMarket = { navController.navigate(Routes.MODULE_MARKET) }
                )
            }

            composable(Routes.MODULE_MARKET) {
                ModuleMarketScreen(onNavigateBack = { navController.popBackStack() })
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
                    onModuleCreated = { navController.popBackStack() },
                    onNavigateToAiSettings = { navController.navigate(Routes.AI_SETTINGS) }
                )
            }
        }
    }
}

@Composable
fun PreviewScreen(appId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { WebToAppApplication.repository }
    val webApp by repository.getWebAppById(appId).collectAsState(initial = null)
    val hasLaunched = remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(webApp) {
        val app = webApp
        if (app != null && !hasLaunched.value) {
            hasLaunched.value = true
            when (app.appType) {
                com.webtoapp.data.model.AppType.IMAGE,
                com.webtoapp.data.model.AppType.VIDEO -> {
                    com.webtoapp.ui.media.MediaAppActivity.startForPreview(context, app)
                }

                com.webtoapp.data.model.AppType.GALLERY -> {
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
