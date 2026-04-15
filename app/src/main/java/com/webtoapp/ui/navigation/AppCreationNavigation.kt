package com.webtoapp.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
import com.webtoapp.ui.screens.LinuxEnvironmentScreen

internal fun NavGraphBuilder.addAppCreationRoutes(
    navController: NavHostController,
    dependencies: CreationRoutesDeps,
) {
    val viewModel = dependencies.viewModel
    val webAppRepository = dependencies.webAppRepository
    val activationManager = dependencies.activationManager

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
        route = Routes.CREATE_HTML_APP_WITH_IMPORT,
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
        val routeArgs = CreateHtmlRouteArgs.from(backStackEntry)
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
            importDir = routeArgs.importDir,
            importProjectName = routeArgs.projectName
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
        route = Routes.CREATE_MULTI_WEB_APP,
    ) {
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

    composable(Routes.LINUX_ENVIRONMENT) {
        LinuxEnvironmentScreen(
            envManager = dependencies.linuxEnvironmentManager,
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
}
