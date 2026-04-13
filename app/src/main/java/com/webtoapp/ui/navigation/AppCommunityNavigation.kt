package com.webtoapp.ui.navigation

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.ui.screens.community.FavoritesScreen
import com.webtoapp.ui.screens.community.ModuleDetailScreen
import com.webtoapp.ui.screens.community.NotificationsScreen
import com.webtoapp.ui.screens.community.PostDetailScreen
import com.webtoapp.ui.screens.community.UserProfileScreen
import com.webtoapp.ui.viewmodel.CommunityViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

internal fun NavGraphBuilder.addCommunityRoutes(
    navController: NavHostController,
) {
    composable(
        route = Routes.MODULE_DETAIL,
        arguments = listOf(navArgument("moduleId") { type = NavType.IntType })
    ) { backStackEntry ->
        val moduleId = backStackEntry.arguments?.getInt("moduleId") ?: 0
        val communityViewModel: CommunityViewModel = koinViewModel()
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        ModuleDetailScreen(
            moduleId = moduleId,
            communityViewModel = communityViewModel,
            onBack = { navController.popBackStack() },
            onNavigateToUser = { userId -> navController.navigate(Routes.communityUser(userId)) },
            onInstallModule = { shareCode ->
                coroutineScope.launch {
                    ExtensionManager.getInstance(context).importFromShareCode(shareCode)
                }
                navController.popBackStack()
            }
        )
    }

    composable(
        route = Routes.USER_PROFILE,
        arguments = listOf(navArgument("userId") { type = NavType.IntType })
    ) { backStackEntry ->
        val userId = backStackEntry.arguments?.getInt("userId") ?: 0
        val communityViewModel: CommunityViewModel = koinViewModel()
        UserProfileScreen(
            userId = userId,
            communityViewModel = communityViewModel,
            onBack = { navController.popBackStack() },
            onModuleClick = { moduleId -> navController.navigate(Routes.moduleDetail(moduleId)) },
            onPostClick = { postId -> navController.navigate(Routes.communityPost(postId)) },
            onNavigateToUser = { uid -> navController.navigate(Routes.communityUser(uid)) }
        )
    }

    composable(Routes.FAVORITES) {
        val communityViewModel: CommunityViewModel = koinViewModel()
        FavoritesScreen(
            communityViewModel = communityViewModel,
            onBack = { navController.popBackStack() },
            onModuleClick = { moduleId -> navController.navigate(Routes.moduleDetail(moduleId)) }
        )
    }

    composable(Routes.NOTIFICATIONS) {
        val communityViewModel: CommunityViewModel = koinViewModel()
        NotificationsScreen(
            communityViewModel = communityViewModel,
            onBack = { navController.popBackStack() },
            onNavigateToModule = { moduleId -> navController.navigate(Routes.moduleDetail(moduleId)) },
            onNavigateToUser = { userId -> navController.navigate(Routes.communityUser(userId)) }
        )
    }

    composable(
        route = Routes.COMMUNITY_POST_DETAIL,
        arguments = listOf(navArgument("postId") { type = NavType.IntType })
    ) { backStackEntry ->
        val postId = backStackEntry.arguments?.getInt("postId") ?: 0
        val communityViewModel: CommunityViewModel = koinViewModel()
        PostDetailScreen(
            postId = postId,
            communityViewModel = communityViewModel,
            onBack = { navController.popBackStack() },
            onNavigateToUser = { userId -> navController.navigate(Routes.communityUser(userId)) }
        )
    }
}
