package com.webtoapp.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
internal fun AppNavigationGraph(
    navController: NavHostController,
    dependencies: AppNavigationRootDeps,
) {
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

        addToolRoutes(
            navController = navController,
            dependencies = dependencies.toolRoutes,
        )
        addAppCreationRoutes(
            navController = navController,
            dependencies = dependencies.creationRoutes,
        )
        addPreviewRoutes(
            navController = navController,
            dependencies = dependencies.previewRoutes,
        )
        addAccountRoutes(
            navController = navController,
            dependencies = dependencies.accountRoutes,
        )
        addCommunityRoutes(
            navController = navController,
        )
    }
}
