package com.webtoapp.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.webtoapp.core.stats.OverallStats
import com.webtoapp.ui.screens.AboutScreen
import com.webtoapp.ui.screens.AiCodingScreen
import com.webtoapp.ui.screens.AiHtmlCodingScreen
import com.webtoapp.ui.screens.AiSettingsScreen
import com.webtoapp.ui.screens.AppModifierScreen
import com.webtoapp.ui.screens.BrowserKernelScreen
import com.webtoapp.ui.screens.ExtensionModuleScreen
import com.webtoapp.ui.screens.HostsAdBlockScreen
import com.webtoapp.ui.screens.ModuleEditorScreen
import com.webtoapp.ui.screens.PortManagerScreen
import com.webtoapp.ui.screens.RuntimeDepsScreen
import com.webtoapp.ui.screens.StatsScreen
import com.webtoapp.ui.screens.aimodule.AiModuleDeveloperScreen
import kotlinx.coroutines.launch

internal fun NavGraphBuilder.addToolRoutes(
    navController: NavHostController,
    dependencies: ToolRoutesDeps,
) {
    val viewModel = dependencies.viewModel
    val statsRepository = dependencies.statsRepository
    val healthMonitor = dependencies.healthMonitor
    val webAppRepository = dependencies.webAppRepository

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
            onCheckAllHealth = {
                statsScope.launch {
                    healthMonitor.checkApps(apps)
                    overallStats = statsRepository.getOverallStats()
                }
            }
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
                AiExportCoordinator.exportHtmlProject(
                    navController = navController,
                    files = files,
                    projectName = projectName,
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
                AiExportCoordinator.exportCodingProject(
                    navController = navController,
                    files = files,
                    projectName = projectName,
                    codingType = codingType,
                )
            },
            onNavigateToAiSettings = {
                navController.navigate(Routes.AI_SETTINGS)
            }
        )
    }

    composable(Routes.BROWSER_KERNEL) {
        BrowserKernelScreen(
            engineManager = dependencies.browserKernel.engineManager,
            shields = dependencies.browserKernel.shields,
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
}
