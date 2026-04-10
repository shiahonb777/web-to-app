package com.webtoapp.ui.navigation
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.waterfall
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.*
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import org.koin.androidx.compose.koinViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.themedBackground
import com.webtoapp.ui.screens.community.CommunityScreen
import com.webtoapp.ui.screens.AiSettingsScreen
import com.webtoapp.ui.screens.AppModifierScreen
import com.webtoapp.ui.screens.AppStoreScreen
import com.webtoapp.ui.screens.BrowserKernelScreen
import com.webtoapp.ui.screens.HostsAdBlockScreen
import com.webtoapp.ui.screens.CreateAppScreen
import com.webtoapp.ui.screens.CreateHtmlAppScreen
import com.webtoapp.ui.screens.CreateMediaAppScreen
import com.webtoapp.ui.screens.CreateGalleryAppScreenV2
import com.webtoapp.ui.screens.CreateFrontendAppScreen
import com.webtoapp.ui.screens.CreateNodeJsAppScreen
import com.webtoapp.ui.screens.CreateWordPressAppScreen
import com.webtoapp.ui.screens.CreatePhpAppScreen
import com.webtoapp.ui.screens.CreatePythonAppScreen
import com.webtoapp.ui.screens.CreateGoAppScreen
import com.webtoapp.ui.screens.CreateMultiWebAppScreen
import com.webtoapp.ui.screens.RuntimeDepsScreen
import com.webtoapp.ui.screens.PortManagerScreen
import com.webtoapp.ui.screens.LinuxEnvironmentScreen
import com.webtoapp.ui.screens.HomeScreen
import com.webtoapp.ui.screens.MoreScreen
import com.webtoapp.ui.screens.AboutScreen
import com.webtoapp.ui.screens.StatsScreen
import com.webtoapp.ui.screens.AiHtmlCodingScreen
import com.webtoapp.ui.screens.AiCodingScreen

import com.webtoapp.ui.screens.ExtensionModuleScreen
import com.webtoapp.ui.screens.ModuleEditorScreen
import com.webtoapp.ui.screens.AuthScreen
import com.webtoapp.ui.screens.ProfileScreen
import com.webtoapp.ui.screens.ActivationCodeScreen
import com.webtoapp.ui.screens.DeviceManagementScreen
import com.webtoapp.ui.screens.SubscriptionScreen

import com.webtoapp.ui.screens.TeamScreen
import com.webtoapp.ui.screens.aimodule.AiModuleDeveloperScreen
import com.webtoapp.ui.screens.community.FavoritesScreen
import com.webtoapp.ui.screens.community.ModuleDetailScreen
import com.webtoapp.ui.screens.community.NotificationsScreen
import com.webtoapp.ui.screens.community.UserProfileScreen
import com.webtoapp.ui.screens.community.PostDetailScreen
import com.webtoapp.ui.viewmodel.AuthViewModel
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.viewmodel.CommunityViewModel
import com.webtoapp.ui.viewmodel.MainViewModel
import com.webtoapp.ui.webview.WebViewActivity
import com.webtoapp.ui.components.LiquidTabBar
import com.webtoapp.ui.components.LiquidTabItem

/**
 * 导航路由定义
 */
/**
 * 底部 Tab 定义
 */
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
        HOME -> Strings.tabHome
        STORE -> Strings.tabStore
        COMMUNITY -> Strings.tabCommunity
        PROFILE -> Strings.tabProfile
        MORE -> Strings.tabMore
    }
}

object Routes {
    // 底部 Tab 顶层路由
    const val HOME = "home"
    const val APP_STORE = "app_store"
    const val COMMUNITY = "community_tab"
    const val PROFILE_TAB = "profile_tab"
    const val MORE = "more"

    // 详细页面路由
    const val CREATE_APP = "create_app"
    const val CREATE_MEDIA_APP = "create_media_app"
    const val CREATE_GALLERY_APP = "create_gallery_app"
    const val CREATE_HTML_APP = "create_html_app"
    const val CREATE_HTML_APP_WITH_IMPORT = "create_html_app?importDir={importDir}&projectName={projectName}"
    const val CREATE_FRONTEND_APP = "create_frontend_app"
    const val LINUX_ENVIRONMENT = "linux_environment"
    
    // 通用编辑（共性功能：公告、广告拦截、扩展模块等）
    const val EDIT_APP = "edit_app/{appId}"
    // 各类型专用编辑（核心配置）
    const val EDIT_WEB_APP = "edit_web_app/{appId}"
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

    // 社区页面
    const val COMMUNITY_POST_DETAIL = "community_post/{postId}"
    const val MODULE_DETAIL = "module_detail/{moduleId}"
    const val USER_PROFILE = "community_user/{userId}"
    const val FAVORITES = "favorites"
    const val NOTIFICATIONS = "notifications"

    /** 顶层 Tab 路由集合，用于判断是否显示底部导航 */
    val TAB_ROUTES = setOf(HOME, APP_STORE, COMMUNITY, PROFILE_TAB, MORE)

    fun editApp(appId: Long) = "edit_app/$appId"
    fun editWebApp(appId: Long) = "edit_web_app/$appId"
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

/**
 * 应用导航
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = koinViewModel()
    val authViewModel: AuthViewModel = koinViewModel()

    // Track the selected tab persistently
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var previousTab by remember { mutableIntStateOf(0) }

    // Track whether we're on a detail screen (pushed from a tab)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isOnDetailScreen = currentRoute != null && currentRoute != "tab_host"

    // ═══════ 自动检查更新 ═══════
    val context = LocalContext.current
    var autoUpdateInfo by remember { mutableStateOf<com.webtoapp.util.AppUpdateChecker.UpdateInfo?>(null) }
    var showAutoUpdateDialog by remember { mutableStateOf(false) }
    var isAutoDownloading by remember { mutableStateOf(false) }
    var autoDownloadId by remember { mutableStateOf(-1L) }

    // 启动时自动检查更新（仅执行一次）
    LaunchedEffect(Unit) {
        if (com.webtoapp.util.AppUpdateChecker.shouldAutoCheck(context)) {
            try {
                val (versionName, versionCode) = com.webtoapp.util.AppUpdateChecker.getCurrentVersionInfo(context)
                com.webtoapp.util.AppUpdateChecker.recordAutoCheck(context)
                val result = com.webtoapp.util.AppUpdateChecker.checkUpdate(versionName, versionCode)
                result.onSuccess { info ->
                    if (info.hasUpdate) {
                        autoUpdateInfo = info
                        showAutoUpdateDialog = true
                    }
                }
            } catch (_: Exception) {
                // 静默失败，不打扰用户
            }
        }
    }

    // 监听下载完成
    androidx.compose.runtime.DisposableEffect(autoDownloadId) {
        if (autoDownloadId == -1L) return@DisposableEffect onDispose {}
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                val id = intent?.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (id == autoDownloadId) {
                    isAutoDownloading = false
                    com.webtoapp.util.AppUpdateChecker.installApk(context, autoDownloadId)
                }
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }

    // 自动更新提示对话框
    if (showAutoUpdateDialog && autoUpdateInfo != null) {
        val info = autoUpdateInfo!!
        val (curVersion, _) = remember { com.webtoapp.util.AppUpdateChecker.getCurrentVersionInfo(context) }
        AlertDialog(
            onDismissRequest = { showAutoUpdateDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.SystemUpdate,
                    null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    Strings.newVersionFound,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                "v$curVersion",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text("→")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2196F3).copy(alpha = 0.15f)
                        ) {
                            Text(
                                info.versionName,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                        }
                    }
                    if (info.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            info.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (info.downloadUrl.isNotEmpty()) {
                            isAutoDownloading = true
                            autoDownloadId = com.webtoapp.util.AppUpdateChecker.downloadApk(
                                context,
                                info.downloadUrl,
                                info.versionName
                            )
                            if (autoDownloadId == -1L) {
                                isAutoDownloading = false
                            }
                            showAutoUpdateDialog = false
                        }
                    },
                    enabled = !isAutoDownloading
                ) {
                    if (isAutoDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(Strings.updateNow)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAutoUpdateDialog = false }
                ) {
                    Text(Strings.updateLater)
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isOnDetailScreen) {
                val liquidTabs = remember(Strings.currentLanguage.value) {
                    BottomTab.entries.map { tab ->
                        LiquidTabItem(
                            selectedIcon = tab.selectedIcon,
                            unselectedIcon = tab.unselectedIcon,
                            label = tab.label()
                        )
                    }
                }
                LiquidTabBar(
                    tabs = liquidTabs,
                    selectedIndex = selectedTab,
                    onTabSelected = { index ->
                        if (isOnDetailScreen) {
                            navController.popBackStack("tab_host", inclusive = false)
                        }
                        previousTab = selectedTab
                        selectedTab = index
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { scaffoldPadding ->

    // Calculate proper insets for landscape — system bars can appear on any side
    val systemBarsInsets = WindowInsets.systemBars
    
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .themedBackground()
            // Handle display cutout (notch/punch-hole) padding in landscape
            .displayCutoutPadding()
            // Apply system bar padding for horizontal (side nav in landscape) + top (status bar)
            .padding(
                start = with(androidx.compose.ui.platform.LocalDensity.current) {
                    systemBarsInsets.getLeft(this, androidx.compose.ui.unit.LayoutDirection.Ltr).toDp()
                },
                end = with(androidx.compose.ui.platform.LocalDensity.current) {
                    systemBarsInsets.getRight(this, androidx.compose.ui.unit.LayoutDirection.Ltr).toDp()
                }
            )
            .padding(bottom = if (!isOnDetailScreen) scaffoldPadding.calculateBottomPadding() else 0.dp)
    ) {
        // ════════════════════════════════════════════
        // Persistent Tab Content — all tabs stay alive
        // ════════════════════════════════════════════

        // 滑动方向：目标 tab 在右侧则从右往左滑入
        val slideDirection = if (selectedTab >= previousTab) 1 else -1
        val slideOffsetPx = 120f  // 偏移量（像素）
        
        // Tab 0: 首页
        val tab0Active = selectedTab == 0 && !isOnDetailScreen
        val tab0Alpha by animateFloatAsState(
            targetValue = if (tab0Active) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium), label = "tab0Alpha"
        )
        val tab0OffsetX by animateFloatAsState(
            targetValue = if (tab0Active) 0f else (if (0 < selectedTab) -slideOffsetPx else slideOffsetPx),
            animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow), label = "tab0Offset"
        )
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (tab0Active) 1f else 0f)
                .graphicsLayer {
                    alpha = tab0Alpha
                    translationX = tab0OffsetX * density
                }
        ) {
            HomeScreen(
                viewModel = viewModel,
                onCreateApp = {
                    viewModel.createNewApp()
                    navController.navigate(Routes.CREATE_APP)
                },
                onCreateMediaApp = { navController.navigate(Routes.CREATE_MEDIA_APP) },
                onCreateGalleryApp = { navController.navigate(Routes.CREATE_GALLERY_APP) },
                onCreateHtmlApp = { navController.navigate(Routes.CREATE_HTML_APP) },
                onCreateFrontendApp = { navController.navigate(Routes.CREATE_FRONTEND_APP) },
                onCreateNodeJsApp = { navController.navigate(Routes.CREATE_NODEJS_APP) },
                onCreateWordPressApp = { navController.navigate(Routes.CREATE_WORDPRESS_APP) },
                onCreatePhpApp = { navController.navigate(Routes.CREATE_PHP_APP) },
                onCreatePythonApp = { navController.navigate(Routes.CREATE_PYTHON_APP) },
                onCreateGoApp = { navController.navigate(Routes.CREATE_GO_APP) },
                onCreateMultiWebApp = { navController.navigate(Routes.CREATE_MULTI_WEB_APP) },
                onEditApp = { webApp ->
                    viewModel.editApp(webApp)
                    navController.navigate(Routes.editApp(webApp.id))
                },
                onEditAppCore = { webApp ->
                    when (webApp.appType) {
                        com.webtoapp.data.model.AppType.WEB -> {
                            viewModel.editApp(webApp)
                            navController.navigate(Routes.editWebApp(webApp.id))
                        }
                        com.webtoapp.data.model.AppType.IMAGE,
                        com.webtoapp.data.model.AppType.VIDEO -> navController.navigate(Routes.editMediaApp(webApp.id))
                        com.webtoapp.data.model.AppType.GALLERY -> navController.navigate(Routes.editGalleryApp(webApp.id))
                        com.webtoapp.data.model.AppType.HTML -> navController.navigate(Routes.editHtmlApp(webApp.id))
                        com.webtoapp.data.model.AppType.FRONTEND -> navController.navigate(Routes.editFrontendApp(webApp.id))
                        com.webtoapp.data.model.AppType.NODEJS_APP -> navController.navigate(Routes.editNodeJsApp(webApp.id))
                        com.webtoapp.data.model.AppType.WORDPRESS -> {
                            viewModel.editApp(webApp)
                            navController.navigate(Routes.editApp(webApp.id))
                        }
                        com.webtoapp.data.model.AppType.PHP_APP -> navController.navigate(Routes.editPhpApp(webApp.id))
                        com.webtoapp.data.model.AppType.PYTHON_APP -> navController.navigate(Routes.editPythonApp(webApp.id))
                        com.webtoapp.data.model.AppType.GO_APP -> navController.navigate(Routes.editGoApp(webApp.id))
                        com.webtoapp.data.model.AppType.MULTI_WEB -> navController.navigate(Routes.editMultiWebApp(webApp.id))
                    }
                },
                onPreviewApp = { webApp -> navController.navigate(Routes.preview(webApp.id)) },
                onOpenAppModifier = { navController.navigate(Routes.APP_MODIFIER) },
                onOpenAiSettings = { navController.navigate(Routes.AI_SETTINGS) },
                onOpenAiCoding = { navController.navigate(Routes.AI_CODING) },
                onOpenAiHtmlCoding = { navController.navigate(Routes.AI_HTML_CODING) },
                onOpenExtensionModules = { navController.navigate(Routes.EXTENSION_MODULES) },
                onOpenLinuxEnvironment = { navController.navigate(Routes.LINUX_ENVIRONMENT) },
            )
        }

        // Tab 1: 市场
        val tab1Active = selectedTab == 1 && !isOnDetailScreen
        val tab1Alpha by animateFloatAsState(
            targetValue = if (tab1Active) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium), label = "tab1Alpha"
        )
        val tab1OffsetX by animateFloatAsState(
            targetValue = if (tab1Active) 0f else (if (1 < selectedTab) -slideOffsetPx else slideOffsetPx),
            animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow), label = "tab1Offset"
        )
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (tab1Active) 1f else 0f)
                .graphicsLayer {
                    alpha = tab1Alpha
                    translationX = tab1OffsetX * density
                }
        ) {
            val cloudViewModel: CloudViewModel = org.koin.androidx.compose.koinViewModel()
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val downloadManager = remember { com.webtoapp.core.cloud.AppDownloadManager.getInstance(context) }
            AppStoreScreen(
                cloudViewModel = cloudViewModel,
                onInstallModule = { shareCode ->
                    coroutineScope.launch {
                        com.webtoapp.core.extension.ExtensionManager.getInstance(context)
                            .importFromShareCode(shareCode)
                    }
                },
                downloadManager = downloadManager
            )
        }

        // Tab 2: 社区
        val tab2Active = selectedTab == 2 && !isOnDetailScreen
        val tab2Alpha by animateFloatAsState(
            targetValue = if (tab2Active) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium), label = "tab2Alpha"
        )
        val tab2OffsetX by animateFloatAsState(
            targetValue = if (tab2Active) 0f else (if (2 < selectedTab) -slideOffsetPx else slideOffsetPx),
            animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow), label = "tab2Offset"
        )
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (tab2Active) 1f else 0f)
                .graphicsLayer {
                    alpha = tab2Alpha
                    translationX = tab2OffsetX * density
                }
        ) {
            CommunityScreen(
                onNavigateToUser = { userId -> navController.navigate(Routes.communityUser(userId)) },
                onNavigateToModule = { moduleId -> navController.navigate(Routes.moduleDetail(moduleId)) },
                onNavigateToPost = { postId -> navController.navigate(Routes.communityPost(postId)) },
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onNavigateToFavorites = { navController.navigate(Routes.FAVORITES) },
                isTabVisible = tab2Active
            )
        }

        // Tab 3: 我的
        val tab3Active = selectedTab == 3 && !isOnDetailScreen
        val tab3Alpha by animateFloatAsState(
            targetValue = if (tab3Active) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium), label = "tab3Alpha"
        )
        val tab3OffsetX by animateFloatAsState(
            targetValue = if (tab3Active) 0f else (if (3 < selectedTab) -slideOffsetPx else slideOffsetPx),
            animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow), label = "tab3Offset"
        )
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (tab3Active) 1f else 0f)
                .graphicsLayer {
                    alpha = tab3Alpha
                    translationX = tab3OffsetX * density
                }
        ) {
            val authState by authViewModel.authState.collectAsStateWithLifecycle()
            when (authState) {
                is com.webtoapp.ui.viewmodel.AuthState.LoggedIn -> {
                    ProfileScreen(
                        authViewModel = authViewModel,
                        onBack = { selectedTab = 0 },
                        onLogout = { /* authState → LoggedOut → shows AuthScreen */ },
                        onNavigateDevices = { navController.navigate(Routes.DEVICE_MANAGEMENT) },
                        onNavigateActivationCode = { navController.navigate(Routes.ACTIVATION_CODE) },
                        onNavigateTeams = { navController.navigate(Routes.TEAMS) },
                        onNavigateSubscription = { navController.navigate(Routes.SUBSCRIPTION) }
                    )
                }
                is com.webtoapp.ui.viewmodel.AuthState.LoggedOut -> {
                    AuthScreen(
                        authViewModel = authViewModel,
                        onBack = { selectedTab = 0 },
                        onLoginSuccess = { /* authState → LoggedIn → shows ProfileScreen */ }
                    )
                }
            }
        }

        // Tab 4: 更多
        val tab4Active = selectedTab == 4 && !isOnDetailScreen
        val tab4Alpha by animateFloatAsState(
            targetValue = if (tab4Active) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium), label = "tab4Alpha"
        )
        val tab4OffsetX by animateFloatAsState(
            targetValue = if (tab4Active) 0f else (if (4 < selectedTab) -slideOffsetPx else slideOffsetPx),
            animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow), label = "tab4Offset"
        )
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (tab4Active) 1f else 0f)
                .graphicsLayer {
                    alpha = tab4Alpha
                    translationX = tab4OffsetX * density
                }
        ) {
            MoreScreen(
                onOpenAiCoding = { navController.navigate(Routes.AI_CODING) },
                onOpenAiSettings = { navController.navigate(Routes.AI_SETTINGS) },

                onOpenBrowserKernel = { navController.navigate(Routes.BROWSER_KERNEL) },
                onOpenHostsAdBlock = { navController.navigate(Routes.HOSTS_ADBLOCK) },
                onOpenAppModifier = { navController.navigate(Routes.APP_MODIFIER) },
                onOpenExtensionModules = { navController.navigate(Routes.EXTENSION_MODULES) },
                onOpenLinuxEnvironment = { navController.navigate(Routes.LINUX_ENVIRONMENT) },
                onOpenRuntimeDeps = { navController.navigate(Routes.RUNTIME_DEPS) },
                onOpenPortManager = { navController.navigate(Routes.PORT_MANAGER) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) }
            )
        }

        // ════════════════════════════════════════════
        // NavHost — only for detail/push screens
        // ════════════════════════════════════════════
        NavHost(
            navController = navController,
            startDestination = "tab_host",
            modifier = Modifier.fillMaxSize(),
            // 全局页面切换动画：右滑入 + 淡入
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it / 3 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 5 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(250))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 5 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it / 3 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(250))
            }
        ) {
        // Invisible placeholder — tabs are rendered above
        composable("tab_host") {}
        
        // 使用统计 + 健康监控
        composable(Routes.STATS) {
            val statsRepository: com.webtoapp.core.stats.AppStatsRepository = org.koin.java.KoinJavaComponent.get(com.webtoapp.core.stats.AppStatsRepository::class.java)
            val healthMonitor: com.webtoapp.core.stats.AppHealthMonitor = org.koin.java.KoinJavaComponent.get(com.webtoapp.core.stats.AppHealthMonitor::class.java)
            val statsScope = androidx.compose.runtime.rememberCoroutineScope()
            
            val apps by viewModel.webApps.collectAsStateWithLifecycle()
            val allStats by statsRepository.allStats.collectAsState(initial = emptyList())
            val healthRecords by healthMonitor.allHealthRecords.collectAsState(initial = emptyList())
            var overallStats by remember { mutableStateOf(com.webtoapp.core.stats.OverallStats()) }
            
            LaunchedEffect(Unit) {
                overallStats = statsRepository.getOverallStats()
            }
            
            StatsScreen(
                apps = apps,
                allStats = allStats,
                healthRecords = healthRecords,
                overallStats = overallStats,
                onBack = { navController.popBackStack() },
                onCheckHealth = { app ->
                    statsScope.launch {
                        healthMonitor.checkUrl(app.id, app.url)
                    }
                },
                onCheckAllHealth = {
                    statsScope.launch {
                        healthMonitor.checkApps(apps)
                        overallStats = statsRepository.getOverallStats()
                    }
                }
            )
        }

        // Create应用
        composable(Routes.CREATE_APP) {
            CreateAppScreen(
                viewModel = viewModel,
                isEdit = false,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        
        // Create媒体应用（单图片/视频，兼容旧版）
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
        
        // Create媒体画廊应用（多图片/视频，新版）
        composable(Routes.CREATE_GALLERY_APP) {
            CreateGalleryAppScreenV2(
                onBack = { navController.popBackStack() },
                onCreated = { name, galleryConfig, iconUri, themeType ->
                    viewModel.saveGalleryApp(
                        name, galleryConfig, iconUri, themeType
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // CreateHTML应用（支持从AI编程导入）
        composable(
            route = "${Routes.CREATE_HTML_APP}?importDir={importDir}&projectName={projectName}",
            arguments = listOf(
                navArgument("importDir") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("projectName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val importDir = backStackEntry.arguments?.getString("importDir")?.let {
                try { java.net.URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { null }
            }
            val projectName = backStackEntry.arguments?.getString("projectName")?.let {
                try { java.net.URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { null }
            }
            CreateHtmlAppScreen(
                onBack = { navController.popBackStack() },
                onCreated = { name, htmlConfig, iconUri, themeType ->
                    viewModel.saveHtmlApp(
                        name, htmlConfig, iconUri, themeType
                    )
                    navController.popBackStack()
                },
                onZipCreated = { name, extractedDir, entryFile, iconUri, enableJs, enableStorage, landscape ->
                    viewModel.saveZipHtmlApp(
                        name = name,
                        extractedDir = extractedDir,
                        entryFile = entryFile,
                        iconUri = iconUri,
                        enableJavaScript = enableJs,
                        enableLocalStorage = enableStorage,
                        landscapeMode = landscape
                    )
                    navController.popBackStack()
                },
                importDir = importDir,
                importProjectName = projectName
            )
        }
        
        // Create前端项目应用（Vue/React/Node.js）
        composable(Routes.CREATE_FRONTEND_APP) {
            CreateFrontendAppScreen(
                onBack = { navController.popBackStack() },
                onCreated = { name, outputPath, iconUri, framework ->
                    // 将构建输出作为 HTML 应用保存
                    viewModel.saveFrontendApp(
                        name = name,
                        outputPath = outputPath,
                        iconUri = iconUri,
                        framework = framework.name
                    )
                    navController.popBackStack()
                },
                onNavigateToLinuxEnv = {
                    navController.navigate(Routes.LINUX_ENVIRONMENT)
                }
            )
        }
        
        // Create WordPress 应用
        composable(Routes.CREATE_WORDPRESS_APP) {
            CreateWordPressAppScreen(
                onBack = { navController.popBackStack() },
                onCreated = { name, wordpressConfig, iconUri, themeType ->
                    viewModel.saveWordPressApp(
                        name = name,
                        wordpressConfig = wordpressConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // Create Node.js 应用
        composable(Routes.CREATE_NODEJS_APP) {
            CreateNodeJsAppScreen(
                onBack = { navController.popBackStack() },
                onCreated = { name, nodejsConfig, iconUri, themeType ->
                    viewModel.saveNodeJsApp(
                        name = name,
                        nodejsConfig = nodejsConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // Create PHP 应用
        composable(Routes.CREATE_PHP_APP) {
            CreatePhpAppScreen(
                onBack = { navController.popBackStack() },
                onCreated = { name, phpAppConfig, iconUri, themeType ->
                    viewModel.savePhpApp(
                        name = name,
                        phpAppConfig = phpAppConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // Create Python 应用
        composable(Routes.CREATE_PYTHON_APP) {
            CreatePythonAppScreen(
                onBack = { navController.popBackStack() },
                onCreated = { name, pythonAppConfig, iconUri, themeType ->
                    viewModel.savePythonApp(
                        name = name,
                        pythonAppConfig = pythonAppConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // Create Go 服务
        composable(Routes.CREATE_GO_APP) {
            CreateGoAppScreen(
                onBack = { navController.popBackStack() },
                onCreated = { name, goAppConfig, iconUri, themeType ->
                    viewModel.saveGoApp(
                        name = name,
                        goAppConfig = goAppConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // Edit PHP 应用
        composable(
            route = Routes.EDIT_PHP_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreatePhpAppScreen(
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, phpAppConfig, iconUri, themeType ->
                    viewModel.updatePhpApp(
                        appId = appId,
                        name = name,
                        phpAppConfig = phpAppConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // Edit Python 应用
        composable(
            route = Routes.EDIT_PYTHON_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreatePythonAppScreen(
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, pythonAppConfig, iconUri, themeType ->
                    viewModel.updatePythonApp(
                        appId = appId,
                        name = name,
                        pythonAppConfig = pythonAppConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // Edit Go 服务
        composable(
            route = Routes.EDIT_GO_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreateGoAppScreen(
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, goAppConfig, iconUri, themeType ->
                    viewModel.updateGoApp(
                        appId = appId,
                        name = name,
                        goAppConfig = goAppConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // Create 多站点聚合应用
        composable(Routes.CREATE_MULTI_WEB_APP) {
            CreateMultiWebAppScreen(
                onBack = { navController.popBackStack() },
                onCreated = { name, multiWebConfig, iconUri, themeType ->
                    viewModel.saveMultiWebApp(
                        name = name,
                        multiWebConfig = multiWebConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // Edit 多站点聚合应用
        composable(
            route = Routes.EDIT_MULTI_WEB_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreateMultiWebAppScreen(
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, multiWebConfig, iconUri, themeType ->
                    viewModel.updateMultiWebApp(
                        appId = appId,
                        name = name,
                        multiWebConfig = multiWebConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // Linux 环境管理
        composable(Routes.LINUX_ENVIRONMENT) {
            LinuxEnvironmentScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 通用编辑（共性功能：公告、广告拦截等）
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
        
        // Web page应用专用编辑（核心配置：URL、名称、图标）
        composable(
            route = Routes.EDIT_WEB_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) {
            CreateAppScreen(
                viewModel = viewModel,
                isEdit = true,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        
        // Media应用专用编辑（单图片/视频）
        composable(
            route = Routes.EDIT_MEDIA_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreateMediaAppScreen(
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, appType, mediaUri, mediaConfig, iconUri, themeType ->
                    viewModel.updateMediaApp(
                        appId, name, appType, mediaUri, mediaConfig, iconUri, themeType
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // Media画廊应用专用编辑
        composable(
            route = Routes.EDIT_GALLERY_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreateGalleryAppScreenV2(
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, galleryConfig, iconUri, themeType ->
                    viewModel.updateGalleryApp(
                        appId, name, galleryConfig, iconUri, themeType
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // HTML应用专用编辑
        composable(
            route = Routes.EDIT_HTML_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreateHtmlAppScreen(
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, htmlConfig, iconUri, themeType ->
                    viewModel.updateHtmlApp(
                        appId, name, htmlConfig, iconUri, themeType
                    )
                    navController.popBackStack()
                }
            )
        }
        
        // 前端项目应用专用编辑
        composable(
            route = Routes.EDIT_FRONTEND_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreateFrontendAppScreen(
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, outputPath, iconUri, framework ->
                    viewModel.updateFrontendApp(
                        appId, name, outputPath, iconUri, framework.name
                    )
                    navController.popBackStack()
                },
                onNavigateToLinuxEnv = {
                    navController.navigate(Routes.LINUX_ENVIRONMENT)
                }
            )
        }
        
        // Node.js 应用专用编辑
        composable(
            route = Routes.EDIT_NODEJS_APP,
            arguments = listOf(navArgument("appId") { type = NavType.LongType })
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
            CreateNodeJsAppScreen(
                existingAppId = appId,
                onBack = { navController.popBackStack() },
                onCreated = { name, nodejsConfig, iconUri, themeType ->
                    viewModel.updateNodeJsApp(
                        appId = appId,
                        name = name,
                        nodejsConfig = nodejsConfig,
                        iconUri = iconUri,
                        themeType = themeType
                    )
                    navController.popBackStack()
                }
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

        // App修改器
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
        composable(Routes.AI_HTML_CODING) {
            AiHtmlCodingScreen(
                onBack = { navController.popBackStack() },
                onExportToHtmlProject = { files, projectName ->
                    // 将AI生成的代码导出到HTML项目创建页面
                    // Save文件到临时目录，然后导航到创建HTML应用页面
                    val context = navController.context
                    val tempDir = java.io.File(context.cacheDir, "ai_html_export").apply { 
                        if (exists()) deleteRecursively()
                        mkdirs() 
                    }
                    
                    // Save所有文件
                    files.forEach { file ->
                        java.io.File(tempDir, file.name).writeText(file.content)
                    }
                    
                    // 导航到创建HTML应用页面（带参数）
                    navController.navigate("${Routes.CREATE_HTML_APP}?importDir=${java.net.URLEncoder.encode(tempDir.absolutePath, "UTF-8")}&projectName=${java.net.URLEncoder.encode(projectName, "UTF-8")}")
                },
                onNavigateToAiSettings = {
                    navController.navigate(Routes.AI_SETTINGS)
                }
            )
        }

        // AI编程（统一入口）
        composable(Routes.AI_CODING) {
            AiCodingScreen(
                onBack = { navController.popBackStack() },
                onExportToProject = { files, projectName, codingType ->
                    val context = navController.context
                    val tempDir = java.io.File(context.cacheDir, "ai_coding_export").apply { 
                        if (exists()) deleteRecursively()
                        mkdirs() 
                    }
                    
                    files.forEach { file ->
                        java.io.File(tempDir, file.name).writeText(file.content)
                    }
                    
                    // 根据编程类型导航到对应的创建页面
                    when (codingType) {
                        com.webtoapp.core.ai.coding.AiCodingType.HTML -> {
                            navController.navigate("${Routes.CREATE_HTML_APP}?importDir=${java.net.URLEncoder.encode(tempDir.absolutePath, "UTF-8")}&projectName=${java.net.URLEncoder.encode(projectName, "UTF-8")}")
                        }
                        com.webtoapp.core.ai.coding.AiCodingType.FRONTEND -> {
                            navController.navigate(Routes.CREATE_FRONTEND_APP)
                        }
                        com.webtoapp.core.ai.coding.AiCodingType.NODEJS -> {
                            navController.navigate(Routes.CREATE_NODEJS_APP)
                        }
                        com.webtoapp.core.ai.coding.AiCodingType.WORDPRESS -> {
                            navController.navigate(Routes.CREATE_WORDPRESS_APP)
                        }
                        com.webtoapp.core.ai.coding.AiCodingType.PHP -> {
                            navController.navigate(Routes.CREATE_PHP_APP)
                        }
                        com.webtoapp.core.ai.coding.AiCodingType.PYTHON -> {
                            navController.navigate(Routes.CREATE_PYTHON_APP)
                        }
                        com.webtoapp.core.ai.coding.AiCodingType.GO -> {
                            navController.navigate(Routes.CREATE_GO_APP)
                        }
                    }
                },
                onNavigateToAiSettings = {
                    navController.navigate(Routes.AI_SETTINGS)
                }
            )
        }


        
        // 浏览器内核设置
        composable(Routes.BROWSER_KERNEL) {
            BrowserKernelScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Hosts 广告拦截
        composable(Routes.HOSTS_ADBLOCK) {
            HostsAdBlockScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 运行时 & 依赖管理
        composable(Routes.RUNTIME_DEPS) {
            RuntimeDepsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // 端口管理
        composable(Routes.PORT_MANAGER) {
            PortManagerScreen(
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
        
        // Module编辑器 - 新建
        composable(Routes.MODULE_EDITOR) {
            ModuleEditorScreen(
                moduleId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Module编辑器 - 编辑
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
            AiModuleDeveloperScreen(
                onNavigateBack = { navController.popBackStack() },
                onModuleCreated = { module ->
                    // Module创建成功后返回
                    navController.popBackStack()
                },
                onNavigateToAiSettings = {
                    navController.navigate(Routes.AI_SETTINGS)
                }
            )
        }
        
        // 登录 / 注册
        composable(Routes.AUTH) {
            AuthScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onLoginSuccess = { navController.popBackStack() }
            )
        }
        
        // 个人中心
        composable(Routes.PROFILE) {
            ProfileScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onLogout = { navController.popBackStack() },
                onNavigateDevices = { navController.navigate(Routes.DEVICE_MANAGEMENT) },
                onNavigateActivationCode = { navController.navigate(Routes.ACTIVATION_CODE) },
                onNavigateSubscription = { navController.navigate(Routes.SUBSCRIPTION) }
            )
        }
        
        // 订阅套餐页面
        composable(Routes.SUBSCRIPTION) {
            val billingManager: com.webtoapp.core.billing.BillingManager = org.koin.java.KoinJavaComponent.get(com.webtoapp.core.billing.BillingManager::class.java)
            SubscriptionScreen(
                billingManager = billingManager,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        
        // 激活码兑换
        composable(Routes.ACTIVATION_CODE) {
            val cloudViewModel: CloudViewModel = org.koin.androidx.compose.koinViewModel()
            ActivationCodeScreen(
                cloudViewModel = cloudViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        
        // 设备管理
        composable(Routes.DEVICE_MANAGEMENT) {
            val cloudViewModel: CloudViewModel = org.koin.androidx.compose.koinViewModel()
            DeviceManagementScreen(
                cloudViewModel = cloudViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        
        
        // 团队管理
        composable(Routes.TEAMS) {
            TeamScreen(
                onBack = { navController.popBackStack() }
            )
        }


        // ─── 社区页面 ───

        // 模块详情（评论区、投票、收藏）
        composable(
            route = Routes.MODULE_DETAIL,
            arguments = listOf(navArgument("moduleId") { type = NavType.IntType })
        ) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getInt("moduleId") ?: 0
            val communityViewModel: CommunityViewModel = org.koin.androidx.compose.koinViewModel()
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            ModuleDetailScreen(
                moduleId = moduleId,
                communityViewModel = communityViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToUser = { userId -> navController.navigate(Routes.communityUser(userId)) },
                onInstallModule = { shareCode ->
                    coroutineScope.launch {
                        com.webtoapp.core.extension.ExtensionManager.getInstance(context)
                            .importFromShareCode(shareCode)
                    }
                    navController.popBackStack()
                }
            )
        }

        // 用户主页
        composable(
            route = Routes.USER_PROFILE,
            arguments = listOf(navArgument("userId") { type = NavType.IntType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getInt("userId") ?: 0
            val communityViewModel: CommunityViewModel = org.koin.androidx.compose.koinViewModel()
            UserProfileScreen(
                userId = userId,
                communityViewModel = communityViewModel,
                onBack = { navController.popBackStack() },
                onModuleClick = { moduleId -> navController.navigate(Routes.moduleDetail(moduleId)) },
                onPostClick = { postId -> navController.navigate(Routes.communityPost(postId)) },
                onNavigateToUser = { uid -> navController.navigate(Routes.communityUser(uid)) }
            )
        }

        // 收藏列表
        composable(Routes.FAVORITES) {
            val communityViewModel: CommunityViewModel = org.koin.androidx.compose.koinViewModel()
            FavoritesScreen(
                communityViewModel = communityViewModel,
                onBack = { navController.popBackStack() },
                onModuleClick = { moduleId -> navController.navigate(Routes.moduleDetail(moduleId)) }
            )
        }

        // 通知与动态
        composable(Routes.NOTIFICATIONS) {
            val communityViewModel: CommunityViewModel = org.koin.androidx.compose.koinViewModel()
            NotificationsScreen(
                communityViewModel = communityViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToModule = { moduleId -> navController.navigate(Routes.moduleDetail(moduleId)) },
                onNavigateToUser = { userId -> navController.navigate(Routes.communityUser(userId)) }
            )
        }

        // 帖子详情
        composable(
            route = Routes.COMMUNITY_POST_DETAIL,
            arguments = listOf(navArgument("postId") { type = NavType.IntType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getInt("postId") ?: 0
            val communityViewModel: CommunityViewModel = org.koin.androidx.compose.koinViewModel()
            PostDetailScreen(
                postId = postId,
                communityViewModel = communityViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToUser = { userId -> navController.navigate(Routes.communityUser(userId)) }
            )
        }

    } // NavHost
    } // Box
    } // Scaffold
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
                // Image/视频应用（单媒体）：启动 MediaAppActivity 预览
                com.webtoapp.data.model.AppType.IMAGE,
                com.webtoapp.data.model.AppType.VIDEO -> {
                    com.webtoapp.ui.media.MediaAppActivity.startForPreview(context, app)
                }
                // Media画廊应用（多媒体）：启动 GalleryPlayerActivity 预览
                com.webtoapp.data.model.AppType.GALLERY -> {
                    app.galleryConfig?.let { config ->
                        com.webtoapp.ui.gallery.GalleryPlayerActivity.launch(context, config, 0)
                    }
                }
                // Web page应用和HTML应用：使用 WebViewActivity
                else -> {
                    com.webtoapp.ui.webview.WebViewActivity.start(context, appId)
                }
            }
            onBack()
        }
    }

    // Load中显示空白或加载指示器
    // 预览界面将在对应的Activity中实现
}
