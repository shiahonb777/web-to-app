package com.webtoapp.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.billing.BillingManager
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.InstalledItemsTracker
import com.webtoapp.core.stats.AppHealthMonitor
import com.webtoapp.core.stats.AppStatsRepository
import com.webtoapp.core.stats.BatchImportService
import com.webtoapp.core.stats.WebsiteScreenshotService
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.ui.viewmodel.AuthViewModel
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.viewmodel.MainViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun AppNavigationGraph(
    navController: NavHostController,
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
) {
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

    val dependencies = remember(
        viewModel,
        authViewModel,
        webAppRepository,
        statsRepository,
        healthMonitor,
        screenshotService,
        batchImportService,
        activationManager,
        billingManager,
        apiClient,
        installedItemsTracker,
        cloudViewModel,
    ) {
        AppNavigationGraphDependencies(
            viewModel = viewModel,
            authViewModel = authViewModel,
            webAppRepository = webAppRepository,
            statsRepository = statsRepository,
            healthMonitor = healthMonitor,
            screenshotService = screenshotService,
            batchImportService = batchImportService,
            activationManager = activationManager,
            billingManager = billingManager,
            apiClient = apiClient,
            installedItemsTracker = installedItemsTracker,
            cloudViewModel = cloudViewModel,
        )
    }

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
            dependencies = dependencies,
        )
        addAppCreationRoutes(
            navController = navController,
            dependencies = dependencies,
        )
        addPreviewRoutes(
            navController = navController,
            dependencies = dependencies,
        )
        addAccountRoutes(
            navController = navController,
            dependencies = dependencies,
        )
        addCommunityRoutes(
            navController = navController,
        )
    }
}
