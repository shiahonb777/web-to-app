package com.webtoapp.ui.navigation

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

internal data class AppNavigationGraphDependencies(
    val viewModel: MainViewModel,
    val authViewModel: AuthViewModel,
    val webAppRepository: WebAppRepository,
    val statsRepository: AppStatsRepository,
    val healthMonitor: AppHealthMonitor,
    val screenshotService: WebsiteScreenshotService,
    val batchImportService: BatchImportService,
    val activationManager: ActivationManager,
    val billingManager: BillingManager,
    val apiClient: CloudApiClient,
    val installedItemsTracker: InstalledItemsTracker,
    val cloudViewModel: CloudViewModel,
)
