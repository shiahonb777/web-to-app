package com.webtoapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.webtoapp.ui.screens.AiSettingsScreen
import com.webtoapp.ui.screens.AppModifierScreen
import com.webtoapp.ui.screens.CreateAppScreen
import com.webtoapp.ui.screens.CreateHtmlAppScreen
import com.webtoapp.ui.screens.CreateMediaAppScreen
import com.webtoapp.ui.screens.HomeScreen
import com.webtoapp.ui.screens.AboutScreen
import com.webtoapp.ui.screens.HtmlCodingScreen
import com.webtoapp.ui.screens.ThemeSettingsScreen
import com.webtoapp.ui.viewmodel.MainViewModel
import com.webtoapp.ui.webview.WebViewActivity

/**
 * 导航路由定义
 */
object Routes {
    const val HOME = "home"
    const val CREATE_APP = "create_app"
    const val CREATE_MEDIA_APP = "create_media_app"
    const val CREATE_HTML_APP = "create_html_app"
    const val EDIT_APP = "edit_app/{appId}"
    const val PREVIEW = "preview/{appId}"
    const val APP_MODIFIER = "app_modifier"
    const val AI_SETTINGS = "ai_settings"
    const val HTML_CODING = "html_coding"
    const val THEME_SETTINGS = "theme_settings"
    const val ABOUT = "about"

    fun editApp(appId: Long) = "edit_app/$appId"
    fun preview(appId: Long) = "preview/$appId"
}

/**
 * 应用导航
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        // 主页
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onCreateApp = {
                    viewModel.createNewApp()
                    navController.navigate(Routes.CREATE_APP)
                },
                onCreateMediaApp = {
                    navController.navigate(Routes.CREATE_MEDIA_APP)
                },
                onCreateHtmlApp = {
                    navController.navigate(Routes.CREATE_HTML_APP)
                },
                onEditApp = { webApp ->
                    viewModel.editApp(webApp)
                    navController.navigate(Routes.editApp(webApp.id))
                },
                onPreviewApp = { webApp ->
                    navController.navigate(Routes.preview(webApp.id))
                },
                onOpenAppModifier = {
                    navController.navigate(Routes.APP_MODIFIER)
                },
                onOpenAiSettings = {
                    navController.navigate(Routes.AI_SETTINGS)
                },
                onOpenHtmlCoding = {
                    navController.navigate(Routes.HTML_CODING)
                },
                onOpenThemeSettings = {
                    navController.navigate(Routes.THEME_SETTINGS)
                },
                onOpenAbout = {
                    navController.navigate(Routes.ABOUT)
                }
            )
        }

        // 创建应用
        composable(Routes.CREATE_APP) {
            CreateAppScreen(
                viewModel = viewModel,
                isEdit = false,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        
        // 创建媒体应用
        composable(Routes.CREATE_MEDIA_APP) {
            CreateMediaAppScreen(
                onBack = { navController.popBackStack() },
                onCreated = { name, appType, mediaUri, mediaConfig, iconUri, 
                              activationEnabled, activationCodes, bgmEnabled, bgmConfig ->
                    viewModel.saveMediaApp(
                        name, appType, mediaUri, mediaConfig, iconUri,
                        activationEnabled, activationCodes, bgmEnabled, bgmConfig
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // 创建HTML应用
        composable(Routes.CREATE_HTML_APP) {
            CreateHtmlAppScreen(
                onBack = { navController.popBackStack() },
                onCreated = { name, htmlConfig, iconUri,
                              activationEnabled, activationCodes, bgmEnabled, bgmConfig ->
                    viewModel.saveHtmlApp(
                        name, htmlConfig, iconUri,
                        activationEnabled, activationCodes, bgmEnabled, bgmConfig
                    )
                    navController.popBackStack()
                }
            )
        }

        // 编辑应用
        composable(
            route = Routes.EDIT_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) {
            CreateAppScreen(
                viewModel = viewModel,
                isEdit = true,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // 预览
        composable(
            route = Routes.PREVIEW,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            PreviewScreen(
                appId = appId,
                onBack = { navController.popBackStack() }
            )
        }

        // 应用修改器
        composable(Routes.APP_MODIFIER) {
            AppModifierScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // AI 设置
        composable(Routes.AI_SETTINGS) {
            AiSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // HTML编程AI
        composable(Routes.HTML_CODING) {
            HtmlCodingScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 主题设置
        composable(Routes.THEME_SETTINGS) {
            ThemeSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 关于页面
        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun PreviewScreen(appId: Long, onBack: () -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(appId) {
        if (appId > 0) {
            WebViewActivity.start(context, appId)
        }
        onBack()
    }

    // 预览界面将在WebViewActivity中实现
    // 这里只是占位
}
