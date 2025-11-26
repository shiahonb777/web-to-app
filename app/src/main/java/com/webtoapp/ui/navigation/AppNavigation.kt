package com.webtoapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.webtoapp.ui.screens.CreateAppScreen
import com.webtoapp.ui.screens.HomeScreen
import com.webtoapp.ui.viewmodel.MainViewModel

/**
 * 导航路由定义
 */
object Routes {
    const val HOME = "home"
    const val CREATE_APP = "create_app"
    const val EDIT_APP = "edit_app/{appId}"
    const val PREVIEW = "preview/{appId}"

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
                onEditApp = { webApp ->
                    viewModel.editApp(webApp)
                    navController.navigate(Routes.editApp(webApp.id))
                },
                onPreviewApp = { webApp ->
                    navController.navigate(Routes.preview(webApp.id))
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
    }
}

@Composable
fun PreviewScreen(appId: Long, onBack: () -> Unit) {
    // 预览界面将在WebViewActivity中实现
    // 这里只是占位
}
