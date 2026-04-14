package com.webtoapp.di

import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.announcement.AnnouncementManager
import com.webtoapp.core.auth.AuthApiClient
import com.webtoapp.core.auth.AuthRepository
import com.webtoapp.core.auth.TokenManager
import com.webtoapp.core.cloud.AppDownloadManager
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.CloudRepository
import com.webtoapp.core.cloud.InstalledItemsTracker
import com.webtoapp.core.crypto.KeyManager
import com.webtoapp.core.engine.EngineManager
import com.webtoapp.core.engine.shields.BrowserShields
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.extension.ModulePresetManager
import com.webtoapp.core.forcedrun.ForcedRunManager
import com.webtoapp.core.i18n.LanguageManager
import com.webtoapp.core.linux.LinuxEnvironmentManager
import com.webtoapp.core.shell.ShellModeManager
import com.webtoapp.core.startup.AppStartupManager
import com.webtoapp.core.startup.BackgroundServicesStartup
import com.webtoapp.core.startup.LegacyHttpUrlMigrationStartup
import com.webtoapp.core.startup.LoggingStartup
import com.webtoapp.core.startup.RuntimeWarmupStartup
import com.webtoapp.core.startup.SecurityStartup
import com.webtoapp.core.startup.ShellRuntimeStartup
import com.webtoapp.core.stats.AppStatsRepository
import com.webtoapp.core.stats.AppUsageTracker
import com.webtoapp.core.stats.AppHealthMonitor
import com.webtoapp.core.stats.WebsiteScreenshotService
import com.webtoapp.core.stats.BatchImportService
import com.webtoapp.core.webview.LocalHttpServer
import com.webtoapp.core.billing.BillingManager
import com.webtoapp.data.database.AppDatabase
import com.webtoapp.data.database.createAppDatabase
import com.webtoapp.data.repository.AppCategoryRepository
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.ui.theme.ThemeManager
import com.webtoapp.ui.viewmodel.CommunityViewModel
import com.webtoapp.ui.viewmodel.AuthViewModel
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.viewmodel.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    single { createAppDatabase(androidContext()) }
    single { get<AppDatabase>().webAppDao() }
    single { get<AppDatabase>().appCategoryDao() }
    single { get<AppDatabase>().appUsageStatsDao() }
}

val repositoryModule = module {
    single { WebAppRepository(get()) }
    single { AppCategoryRepository(get()) }
    single { AppStatsRepository(get()) }
}

val managerModule = module {
    single { ActivationManager(androidContext()) }
    single { AnnouncementManager(androidContext()) }
    single { AdBlocker() }
    single { ShellModeManager(androidContext()) }
    single { KeyManager.getInstance(androidContext()) }
    single { ExtensionManager.getInstance(androidContext()) }
    single { AppUsageTracker(get()) }
    single { AppHealthMonitor.getInstance(androidContext(), get()) }
    single { WebsiteScreenshotService.getInstance(androidContext()) }
    single { BatchImportService(androidContext(), get()) }
    single { LanguageManager(androidContext()) }
    single { ThemeManager(androidContext()) }
    single { AppDownloadManager.getInstance(androidContext()) }
    single { EngineManager.getInstance(androidContext()) }
    single { BrowserShields.getInstance(androidContext()) }
    single { LinuxEnvironmentManager.getInstance(androidContext()) }
    single { ForcedRunManager.getInstance(androidContext()) }
    single { ModulePresetManager.getInstance(androidContext()) }
    single { LocalHttpServer.getInstance(androidContext()) }
    // Auth
    single { TokenManager.getInstance(androidContext()) }
    single { AuthApiClient(get()) }
    single { AuthRepository(get(), get()) }
    // Cloud
    single { CloudApiClient(get(), androidContext()) }
    single { CloudRepository(get(), get()) }
    single { InstalledItemsTracker(androidContext()) }
    // Billing
    single { BillingManager(androidContext()) }
}

val viewModelModule = module {
    viewModel { MainViewModel(get(), get(), get(), get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { CloudViewModel(get()) }
    viewModel { CommunityViewModel(get()) }
}

val startupModule = module {
    single { LoggingStartup(androidContext()) }
    single { ShellRuntimeStartup(get(), get(), get(), get()) }
    single { SecurityStartup(androidContext()) }
    single { RuntimeWarmupStartup(get(), androidContext()) }
    single { LegacyHttpUrlMigrationStartup(androidContext(), get()) }
    single { BackgroundServicesStartup(get(), get(), get()) }
    single { AppStartupManager(get(), get(), get(), get(), get(), get(), get()) }
}

/**
 * Module list loaded once by `Application.onCreate`.
 */
val appModules = listOf(
    databaseModule,
    repositoryModule,
    managerModule,
    viewModelModule,
    startupModule
)
