package com.webtoapp.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

internal fun NavGraphBuilder.addPreviewRoutes(
    navController: NavHostController,
    dependencies: PreviewRoutesDeps,
) {
    composable(
        route = Routes.PREVIEW,
        arguments = listOf(navArgument("appId") { type = NavType.LongType })
    ) { backStackEntry ->
        val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
        PreviewRouteScreen(
            appId = appId,
            webAppRepository = dependencies.webAppRepository,
            onBack = { navController.popBackStack() }
        )
    }
}
