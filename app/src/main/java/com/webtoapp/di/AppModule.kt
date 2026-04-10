package com.webtoapp.di

import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.announcement.AnnouncementManager
import com.webtoapp.core.auth.AuthApiClient
import com.webtoapp.core.auth.AuthRepository
import com.webtoapp.core.auth.TokenManager
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.CloudRepository
import com.webtoapp.core.cloud.InstalledItemsTracker
import com.webtoapp.core.crypto.KeyManager
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.shell.ShellModeManager
import com.webtoapp.core.stats.AppStatsRepository
import com.webtoapp.core.stats.AppUsageTracker
import com.webtoapp.core.stats.AppHealthMonitor
import com.webtoapp.core.stats.WebsiteScreenshotService
import com.webtoapp.core.stats.BatchImportService
import com.webtoapp.core.billing.BillingManager
import com.webtoapp.data.database.AppDatabase
import com.webtoapp.data.repository.AppCategoryRepository
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.core.usecase.SaveAppUseCase
import com.webtoapp.ui.viewmodel.CommunityViewModel
import com.webtoapp.ui.viewmodel.AuthViewModel
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.viewmodel.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin 依赖注入模块定义
 * 
 * 集中管理所有依赖的创建和生命周期：
 * - single: 全局单例
 * - factory: 每次注入创建新实例
 * - viewModel: ViewModel 作用域
 * 
 * === 双轨 DI 模式说明 ===
 * 本项目同时使用 Koin 注入和 companion object 单例模式，原因如下：
 * 
 * 1. 编辑器模式（主 App）：通过 Koin 注入获取依赖，支持测试替换和生命周期管理。
 * 2. Shell 模式（导出的 APK）：不包含 Koin 框架，直接使用 Xxx.getInstance(context) 获取单例。
 * 
 * managerModule 中使用 `single { Xxx.getInstance(context) }` 确保 Koin 返回的实例
 * 与 getInstance() 返回的是同一个对象，避免创建重复实例。
 * 
 * 编辑器侧新代码应优先使用 Koin 注入（by inject() / get()），
 * 避免直接调用 WebToAppApplication.xxx 静态 getter。
 */
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

val useCaseModule = module {
    factory { SaveAppUseCase(get()) }
}

val viewModelModule = module {
    viewModel { MainViewModel(get(), get(), get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { CloudViewModel(get()) }
    viewModel { CommunityViewModel(get()) }
}

/**
 * 所有模块的聚合列表，在 Application.onCreate 中一次性加载
 */
val appModules = listOf(
    databaseModule,
    repositoryModule,
    managerModule,
    useCaseModule,
    viewModelModule
)
