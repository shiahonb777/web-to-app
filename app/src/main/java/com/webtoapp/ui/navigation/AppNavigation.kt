package com.webtoapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.webtoapp.ui.screens.ExtensionModuleScreen
import com.webtoapp.ui.screens.ModuleEditorScreen
import com.webtoapp.ui.screens.aimodule.AiModuleDeveloperScreenRefactored
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
    const val EXTENSION_MODULES = "extension_modules"
    const val MODULE_EDITOR = "module_editor"
    const val MODULE_EDITOR_EDIT = "module_editor/{moduleId}"
    const val AI_MODULE_DEVELOPER = "ai_module_developer"
    const val ABOUT = "about"

    fun editApp(appId: Long) = "edit_app/$appId"
    fun preview(appId: Long) = "preview/$appId"
    fun editModule(moduleId: String) = "module_editor/$moduleId"
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
                onOpenExtensionModules = {
                    navController.navigate(Routes.EXTENSION_MODULES)
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
                onCreated = { name, appType, mediaUri, mediaConfig, iconUri, themeType ->
                    viewModel.saveMediaApp(
                        name, appType, mediaUri, mediaConfig, iconUri, themeType
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // 创建HTML应用
        composable(Routes.CREATE_HTML_APP) {
            CreateHtmlAppScreen(
                onBack = { navController.popBackStack() },
                onCreated = { name, htmlConfig, iconUri, themeType ->
                    viewModel.saveHtmlApp(
                        name, htmlConfig, iconUri, themeType
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
                onBack = { navController.popBackStack() },
                onNavigateToAiSettings = {
                    navController.navigate(Routes.AI_SETTINGS)
                }
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
        
        // 扩展模块管理
        composable(Routes.EXTENSION_MODULES) {
            ExtensionModuleScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { moduleId ->
                    if (moduleId == null) {
                        navController.navigate(Routes.MODULE_EDITOR)
                    } else {
                        navController.navigate(Routes.editModule(moduleId))
                    }
                },
                onNavigateToAiDeveloper = {
                    navController.navigate(Routes.AI_MODULE_DEVELOPER)
                }
            )
        }
        
        // 模块编辑器 - 新建
        composable(Routes.MODULE_EDITOR) {
            ModuleEditorScreen(
                moduleId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // 模块编辑器 - 编辑
        composable(
            route = Routes.MODULE_EDITOR_EDIT,
            arguments = listOf(navArgument("moduleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString("moduleId")
            ModuleEditorScreen(
                moduleId = moduleId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // AI 模块开发器
        composable(Routes.AI_MODULE_DEVELOPER) {
            AiModuleDeveloperScreenRefactored(
                onNavigateBack = { navController.popBackStack() },
                onModuleCreated = { module ->
                    // 模块创建成功后返回
                    navController.popBackStack()
                },
                onNavigateToAiSettings = {
                    navController.navigate(Routes.AI_SETTINGS)
                }
            )
        }
    }
}

@Composable
fun PreviewScreen(appId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { com.webtoapp.WebToAppApplication.repository }
    val webApp by repository.getWebAppById(appId).collectAsState(initial = null)
    
    // 使用标记防止重复启动
    var hasLaunched by remember { mutableStateOf(false) }

    LaunchedEffect(webApp) {
        val app = webApp
        if (app != null && !hasLaunched) {
            hasLaunched = true
            
            when (app.appType) {
                // 图片/视频应用：启动 MediaAppActivity 预览
                com.webtoapp.data.model.AppType.IMAGE,
                com.webtoapp.data.model.AppType.VIDEO -> {
                    com.webtoapp.ui.media.MediaAppActivity.startForPreview(context, app)
                }
                // 网页应用和HTML应用：使用 WebViewActivity
                else -> {
                    com.webtoapp.ui.webview.WebViewActivity.start(context, appId)
                }
            }
            onBack()
        }
    }

    // 加载中显示空白或加载指示器
    // 预览界面将在对应的Activity中实现
}
