package com.webtoapp.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.webtoapp.ui.screens.ActivationCodeScreen
import com.webtoapp.ui.screens.AuthScreen
import com.webtoapp.ui.screens.DeviceManagementScreen
import com.webtoapp.ui.screens.ProfileScreen
import com.webtoapp.ui.screens.SubscriptionScreen
import com.webtoapp.ui.screens.TeamScreen
import com.webtoapp.ui.viewmodel.CloudViewModel
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.addAccountRoutes(
    navController: NavHostController,
    dependencies: AppNavigationGraphDependencies,
) {
    val authViewModel = dependencies.authViewModel
    val billingManager = dependencies.billingManager

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
        val cloudViewModel: CloudViewModel = koinViewModel()
        ActivationCodeScreen(
            cloudViewModel = cloudViewModel,
            onBack = { navController.popBackStack() }
        )
    }

    composable(Routes.DEVICE_MANAGEMENT) {
        val cloudViewModel: CloudViewModel = koinViewModel()
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
}
