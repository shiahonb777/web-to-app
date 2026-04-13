package com.webtoapp.ui.navigation

import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.WebApp
import com.webtoapp.ui.viewmodel.MainViewModel

internal data class AppFlowSpec(
    val appType: AppType,
    val createRoute: String,
    val editRoute: (Long) -> String,
    val previewRoute: (Long) -> String = Routes::preview,
    val prepareEdit: (MainViewModel, WebApp) -> Unit = { _, _ -> },
) {
    companion object {
        private val specs = listOf(
            AppFlowSpec(
                appType = AppType.WEB,
                createRoute = Routes.CREATE_APP,
                editRoute = Routes::editApp,
                prepareEdit = { viewModel, webApp -> viewModel.editApp(webApp) },
            ),
            AppFlowSpec(
                appType = AppType.IMAGE,
                createRoute = Routes.CREATE_MEDIA_APP,
                editRoute = Routes::editMediaApp,
            ),
            AppFlowSpec(
                appType = AppType.VIDEO,
                createRoute = Routes.CREATE_MEDIA_APP,
                editRoute = Routes::editMediaApp,
            ),
            AppFlowSpec(
                appType = AppType.GALLERY,
                createRoute = Routes.CREATE_GALLERY_APP,
                editRoute = Routes::editGalleryApp,
            ),
            AppFlowSpec(
                appType = AppType.HTML,
                createRoute = Routes.CREATE_HTML_APP,
                editRoute = Routes::editHtmlApp,
            ),
            AppFlowSpec(
                appType = AppType.FRONTEND,
                createRoute = Routes.CREATE_FRONTEND_APP,
                editRoute = Routes::editFrontendApp,
            ),
            AppFlowSpec(
                appType = AppType.WORDPRESS,
                createRoute = Routes.CREATE_WORDPRESS_APP,
                editRoute = Routes::editApp,
                prepareEdit = { viewModel, webApp -> viewModel.editApp(webApp) },
            ),
            AppFlowSpec(
                appType = AppType.NODEJS_APP,
                createRoute = Routes.CREATE_NODEJS_APP,
                editRoute = Routes::editNodeJsApp,
            ),
            AppFlowSpec(
                appType = AppType.PHP_APP,
                createRoute = Routes.CREATE_PHP_APP,
                editRoute = Routes::editPhpApp,
            ),
            AppFlowSpec(
                appType = AppType.PYTHON_APP,
                createRoute = Routes.CREATE_PYTHON_APP,
                editRoute = Routes::editPythonApp,
            ),
            AppFlowSpec(
                appType = AppType.GO_APP,
                createRoute = Routes.CREATE_GO_APP,
                editRoute = Routes::editGoApp,
            ),
            AppFlowSpec(
                appType = AppType.MULTI_WEB,
                createRoute = Routes.CREATE_MULTI_WEB_APP,
                editRoute = Routes::editMultiWebApp,
            ),
        ).associateBy(AppFlowSpec::appType)

        fun from(appType: AppType): AppFlowSpec =
            requireNotNull(specs[appType]) { "缺少 AppType=$appType 的导航映射" }
    }
}
