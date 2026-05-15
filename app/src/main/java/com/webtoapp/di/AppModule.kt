package com.webtoapp.di

import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.announcement.AnnouncementManager
import com.webtoapp.core.crypto.KeyManager
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.shell.ShellModeManager
import com.webtoapp.core.stats.AppStatsRepository
import com.webtoapp.core.stats.AppUsageTracker
import com.webtoapp.core.stats.AppHealthMonitor
import com.webtoapp.core.stats.WebsiteScreenshotService
import com.webtoapp.core.stats.BatchImportService
import com.webtoapp.data.database.AppDatabase
import com.webtoapp.data.repository.AppCategoryRepository
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.core.usecase.SaveAppUseCase
import com.webtoapp.ui.viewmodel.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module





















val databaseModule = module {
    single { AppDatabase.getInstance(androidContext()) }
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
}

val useCaseModule = module {
    factory { SaveAppUseCase(get()) }
}

val viewModelModule = module {
    viewModel { MainViewModel(get(), get(), get()) }
}




val appModules = listOf(
    databaseModule,
    repositoryModule,
    managerModule,
    useCaseModule,
    viewModelModule
)
