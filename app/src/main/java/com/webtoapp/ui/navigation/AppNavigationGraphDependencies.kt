package com.webtoapp.ui.navigation

import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.billing.BillingManager
import com.webtoapp.core.stats.AppHealthMonitor
import com.webtoapp.core.stats.AppStatsRepository
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.ui.viewmodel.AuthViewModel
import com.webtoapp.ui.viewmodel.MainViewModel

internal data class AppNavigationGraphDependencies(
    val viewModel: MainViewModel,
    val authViewModel: AuthViewModel,
    val webAppRepository: WebAppRepository,
    val statsRepository: AppStatsRepository,
    val healthMonitor: AppHealthMonitor,
    val activationManager: ActivationManager,
    val billingManager: BillingManager,
)
