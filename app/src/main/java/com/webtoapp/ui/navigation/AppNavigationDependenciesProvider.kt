package com.webtoapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.billing.BillingManager
import com.webtoapp.core.cloud.AppDownloadManager
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.InstalledItemsTracker
import com.webtoapp.core.engine.EngineManager
import com.webtoapp.core.engine.shields.BrowserShields
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.linux.LinuxEnvironmentManager
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

internal data class HomeTabDeps(
    val viewModel: MainViewModel,
    val healthMonitor: AppHealthMonitor,
    val screenshotService: WebsiteScreenshotService,
    val batchImportService: BatchImportService,
)

internal data class StoreTabDeps(
    val webAppRepository: WebAppRepository,
    val apiClient: CloudApiClient,
    val installedItemsTracker: InstalledItemsTracker,
    val downloadManager: AppDownloadManager,
    val extensionManager: ExtensionManager,
    val cloudViewModel: CloudViewModel,
)

internal data class BrowserKernelDeps(
    val engineManager: EngineManager,
    val shields: BrowserShields,
)

internal data class ToolRoutesDeps(
    val viewModel: MainViewModel,
    val statsRepository: AppStatsRepository,
    val healthMonitor: AppHealthMonitor,
    val webAppRepository: WebAppRepository,
    val browserKernel: BrowserKernelDeps,
)

internal data class CreationRoutesDeps(
    val viewModel: MainViewModel,
    val webAppRepository: WebAppRepository,
    val activationManager: ActivationManager,
    val linuxEnvironmentManager: LinuxEnvironmentManager,
)

internal data class PreviewRoutesDeps(
    val webAppRepository: WebAppRepository,
)

internal data class AccountRoutesDeps(
    val authViewModel: AuthViewModel,
    val billingManager: BillingManager,
)

internal data class AccountCloudScreenDeps(
    val cloudViewModel: CloudViewModel,
)

internal data class AppNavigationRootDeps(
    val homeTab: HomeTabDeps,
    val storeTab: StoreTabDeps,
    val toolRoutes: ToolRoutesDeps,
    val creationRoutes: CreationRoutesDeps,
    val previewRoutes: PreviewRoutesDeps,
    val accountRoutes: AccountRoutesDeps,
)

@Composable
internal fun rememberAppNavigationRootDeps(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
): AppNavigationRootDeps {
    val webAppRepository: WebAppRepository = koinInject()
    val statsRepository: AppStatsRepository = koinInject()
    val healthMonitor: AppHealthMonitor = koinInject()
    val screenshotService: WebsiteScreenshotService = koinInject()
    val batchImportService: BatchImportService = koinInject()
    val activationManager: ActivationManager = koinInject()
    val billingManager: BillingManager = koinInject()
    val apiClient: CloudApiClient = koinInject()
    val installedItemsTracker: InstalledItemsTracker = koinInject()
    val downloadManager: AppDownloadManager = koinInject()
    val extensionManager: ExtensionManager = koinInject()
    val engineManager: EngineManager = koinInject()
    val shields: BrowserShields = koinInject()
    val linuxEnvironmentManager: LinuxEnvironmentManager = koinInject()
    val cloudViewModel: CloudViewModel = koinViewModel()

    return remember(
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
        downloadManager,
        extensionManager,
        engineManager,
        shields,
        linuxEnvironmentManager,
        cloudViewModel,
    ) {
        AppNavigationRootDeps(
            homeTab = HomeTabDeps(
                viewModel = viewModel,
                healthMonitor = healthMonitor,
                screenshotService = screenshotService,
                batchImportService = batchImportService,
            ),
            storeTab = StoreTabDeps(
                webAppRepository = webAppRepository,
                apiClient = apiClient,
                installedItemsTracker = installedItemsTracker,
                downloadManager = downloadManager,
                extensionManager = extensionManager,
                cloudViewModel = cloudViewModel,
            ),
            toolRoutes = ToolRoutesDeps(
                viewModel = viewModel,
                statsRepository = statsRepository,
                healthMonitor = healthMonitor,
                webAppRepository = webAppRepository,
                browserKernel = BrowserKernelDeps(
                    engineManager = engineManager,
                    shields = shields,
                ),
            ),
            creationRoutes = CreationRoutesDeps(
                viewModel = viewModel,
                webAppRepository = webAppRepository,
                activationManager = activationManager,
                linuxEnvironmentManager = linuxEnvironmentManager,
            ),
            previewRoutes = PreviewRoutesDeps(
                webAppRepository = webAppRepository,
            ),
            accountRoutes = AccountRoutesDeps(
                authViewModel = authViewModel,
                billingManager = billingManager,
            ),
        )
    }
}

@Composable
internal fun rememberAccountCloudScreenDeps(): AccountCloudScreenDeps {
    val cloudViewModel: CloudViewModel = koinViewModel()
    return remember(cloudViewModel) {
        AccountCloudScreenDeps(cloudViewModel = cloudViewModel)
    }
}
