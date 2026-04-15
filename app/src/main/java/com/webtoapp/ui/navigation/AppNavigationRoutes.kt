package com.webtoapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import com.webtoapp.core.i18n.AppStringsProvider

internal const val TAB_HOST_ROUTE = "tab_host"

enum class BottomTab(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelKey: String,
) {
    HOME(Routes.HOME, Icons.Filled.Home, Icons.Outlined.Home, "home"),
    STORE(Routes.APP_STORE, Icons.Filled.Storefront, Icons.Outlined.Storefront, "store"),
    COMMUNITY(Routes.COMMUNITY, Icons.Filled.Forum, Icons.Outlined.Forum, "community"),
    PROFILE(Routes.PROFILE_TAB, Icons.Filled.Person, Icons.Outlined.Person, "profile"),
    MORE(Routes.MORE, Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz, "more");

    fun label(): String = when (this) {
        HOME -> AppStringsProvider.current().tabHome
        STORE -> AppStringsProvider.current().tabStore
        COMMUNITY -> AppStringsProvider.current().tabCommunity
        PROFILE -> AppStringsProvider.current().tabProfile
        MORE -> AppStringsProvider.current().tabMore
    }
}

object Routes {
    const val HOME = "home"
    const val APP_STORE = "app_store"
    const val COMMUNITY = "community_tab"
    const val PROFILE_TAB = "profile_tab"
    const val MORE = "more"

    const val CREATE_APP = "create_app"
    const val CREATE_MEDIA_APP = "create_media_app"
    const val CREATE_GALLERY_APP = "create_gallery_app"
    const val CREATE_HTML_APP = "create_html_app"
    const val CREATE_HTML_APP_WITH_IMPORT = "create_html_app?importDir={importDir}&projectName={projectName}"
    const val CREATE_FRONTEND_APP = "create_frontend_app"
    const val LINUX_ENVIRONMENT = "linux_environment"

    const val EDIT_APP = "edit_app/{appId}"
    const val EDIT_MEDIA_APP = "edit_media_app/{appId}"
    const val EDIT_GALLERY_APP = "edit_gallery_app/{appId}"
    const val EDIT_HTML_APP = "edit_html_app/{appId}"
    const val EDIT_FRONTEND_APP = "edit_frontend_app/{appId}"
    const val CREATE_NODEJS_APP = "create_nodejs_app"
    const val EDIT_NODEJS_APP = "edit_nodejs_app/{appId}"
    const val CREATE_WORDPRESS_APP = "create_wordpress_app"
    const val CREATE_PHP_APP = "create_php_app"
    const val EDIT_PHP_APP = "edit_php_app/{appId}"
    const val CREATE_PYTHON_APP = "create_python_app"
    const val EDIT_PYTHON_APP = "edit_python_app/{appId}"
    const val CREATE_GO_APP = "create_go_app"
    const val EDIT_GO_APP = "edit_go_app/{appId}"
    const val CREATE_MULTI_WEB_APP = "create_multi_web_app"
    const val EDIT_MULTI_WEB_APP = "edit_multi_web_app/{appId}"

    const val PREVIEW = "preview/{appId}"
    const val APP_MODIFIER = "app_modifier"
    const val AI_SETTINGS = "ai_settings"
    const val AI_CODING = "ai_coding"
    const val AI_HTML_CODING = "ai_html_coding"

    const val BROWSER_KERNEL = "browser_kernel"
    const val HOSTS_ADBLOCK = "hosts_adblock"
    const val EXTENSION_MODULES = "extension_modules"
    const val MODULE_EDITOR = "module_editor"
    const val MODULE_EDITOR_EDIT = "module_editor/{moduleId}"
    const val AI_MODULE_DEVELOPER = "ai_module_developer"
    const val RUNTIME_DEPS = "runtime_deps"
    const val PORT_MANAGER = "port_manager"
    const val STATS = "stats"
    const val ABOUT = "about"
    const val AUTH = "auth"
    const val PROFILE = "profile"
    const val ACTIVATION_CODE = "activation_code"
    const val DEVICE_MANAGEMENT = "device_management"
    const val MODULE_STORE = "module_store"
    const val SUBSCRIPTION = "subscription"
    const val TEAMS = "teams"

    const val COMMUNITY_POST_DETAIL = "community_post/{postId}"
    const val MODULE_DETAIL = "module_detail/{moduleId}"
    const val USER_PROFILE = "community_user/{userId}"
    const val FAVORITES = "favorites"
    const val NOTIFICATIONS = "notifications"

    val TAB_ROUTES = setOf(HOME, APP_STORE, COMMUNITY, PROFILE_TAB, MORE)

    fun editApp(appId: Long) = "edit_app/$appId"
    fun editMediaApp(appId: Long) = "edit_media_app/$appId"
    fun editGalleryApp(appId: Long) = "edit_gallery_app/$appId"
    fun editHtmlApp(appId: Long) = "edit_html_app/$appId"
    fun editFrontendApp(appId: Long) = "edit_frontend_app/$appId"
    fun editNodeJsApp(appId: Long) = "edit_nodejs_app/$appId"
    fun editPhpApp(appId: Long) = "edit_php_app/$appId"
    fun editPythonApp(appId: Long) = "edit_python_app/$appId"
    fun editGoApp(appId: Long) = "edit_go_app/$appId"
    fun editMultiWebApp(appId: Long) = "edit_multi_web_app/$appId"
    fun preview(appId: Long) = "preview/$appId"
    fun editModule(moduleId: String) = "module_editor/$moduleId"
    fun moduleDetail(moduleId: Int) = "module_detail/$moduleId"
    fun communityUser(userId: Int) = "community_user/$userId"
    fun communityPost(postId: Int) = "community_post/$postId"
}

internal fun isDetailRoute(route: String?): Boolean =
    route != null && route != TAB_HOST_ROUTE && route !in Routes.TAB_ROUTES

internal fun shouldShowBottomBar(route: String?): Boolean =
    route == null || route == TAB_HOST_ROUTE || route in Routes.TAB_ROUTES

internal fun findTabIndexForRoute(route: String?): Int? {
    val index = BottomTab.entries.indexOfFirst { it.route == route }
    return index.takeIf { it >= 0 }
}
