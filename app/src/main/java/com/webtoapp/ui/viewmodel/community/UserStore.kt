package com.webtoapp.ui.viewmodel.community

import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.CommunityPostItem
import com.webtoapp.core.cloud.CommunityUserProfile
import com.webtoapp.core.cloud.ModuleItem
import com.webtoapp.core.cloud.UserActivityInfo
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserStore(
    private val scope: CoroutineScope,
    private val api: CloudApiClient,
    private val postFailureReport: (FailureReportParams) -> Unit,
    private val onMessage: (String) -> Unit
) {
    companion object {
        private const val TAG = "UserStore"
    }

    private val _userProfile = MutableStateFlow<CommunityUserProfile?>(null)
    val userProfile: StateFlow<CommunityUserProfile?> = _userProfile.asStateFlow()

    private val _userModules = MutableStateFlow<List<ModuleItem>>(emptyList())
    val userModules: StateFlow<List<ModuleItem>> = _userModules.asStateFlow()

    private val _userProfileLoading = MutableStateFlow(false)
    val userProfileLoading: StateFlow<Boolean> = _userProfileLoading.asStateFlow()

    private val _userPosts = MutableStateFlow<List<CommunityPostItem>>(emptyList())
    val userPosts: StateFlow<List<CommunityPostItem>> = _userPosts.asStateFlow()

    private val _userActivity = MutableStateFlow<UserActivityInfo?>(null)
    val userActivity: StateFlow<UserActivityInfo?> = _userActivity.asStateFlow()

    private val _followersList = MutableStateFlow<List<CommunityUserProfile>>(emptyList())
    val followersList: StateFlow<List<CommunityUserProfile>> = _followersList.asStateFlow()

    private val _followingList = MutableStateFlow<List<CommunityUserProfile>>(emptyList())
    val followingList: StateFlow<List<CommunityUserProfile>> = _followingList.asStateFlow()

    fun loadUserProfile(userId: Int) {
        scope.launch {
            _userProfileLoading.value = true
            try {
                val result = api.getUserProfile(userId)
                when (result) {
                    is AuthResult.Success -> {
                        val profile = result.data
                        _userProfile.value = profile
                        // Load modules, posts, activity in parallel
                        val deferredModules = scope.async { loadUserModulesInternal(profile.id) }
                        val deferredPosts = scope.async { loadUserPosts(userId) }
                        val deferredActivity = scope.async { loadUserActivity(userId) }
                        deferredModules.await()
                        deferredPosts.await()
                        deferredActivity.await()
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load user profile failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.userProfileLoadFailed,
                            stage = Strings.loadUserProfileStage,
                            summary = Strings.userProfileFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "user_id: $userId"
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load user profile", e)
                postFailureReport(FailureReportParams(
                    title = Strings.userProfileLoadFailed,
                    stage = Strings.loadUserProfileStage,
                    summary = Strings.userProfileExceptionSummary,
                    throwable = e,
                    extraContext = "user_id: $userId"
                ))
            } finally {
                _userProfileLoading.value = false
            }
        }
    }

    private fun loadUserModulesInternal(userId: Int) {
        scope.launch {
            try {
                val result = api.getUserModules(userId)
                when (result) {
                    is AuthResult.Success -> {
                        val (modules, _) = result.data
                        _userModules.value = modules
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load user modules failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.userModulesLoadFailed,
                            stage = Strings.loadUserModulesStage,
                            summary = Strings.userModulesFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "user_id: $userId"
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load user modules", e)
                postFailureReport(FailureReportParams(
                    title = Strings.userModulesLoadFailed,
                    stage = Strings.loadUserModulesStage,
                    summary = Strings.userModulesExceptionSummary,
                    throwable = e,
                    extraContext = "user_id: $userId"
                ))
            }
        }
    }

    fun loadUserPosts(userId: Int) {
        scope.launch {
            try {
                val result = api.getUserPosts(userId)
                if (result is AuthResult.Success) {
                    _userPosts.value = result.data.posts
                } else if (result is AuthResult.Error) {
                    AppLogger.e(TAG, "Failed to load user posts: ${result.message}")
                    postFailureReport(FailureReportParams(
                        title = Strings.userPostsLoadFailed,
                        stage = Strings.loadUserPostsStage,
                        summary = Strings.userPostsFailedSummary,
                        serviceMessage = result.message,
                        extraContext = "user_id: $userId"
                    ))
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load user posts", e)
                postFailureReport(FailureReportParams(
                    title = Strings.userPostsLoadFailed,
                    stage = Strings.loadUserPostsStage,
                    summary = Strings.userPostsExceptionSummary,
                    throwable = e,
                    extraContext = "user_id: $userId"
                ))
            }
        }
    }

    fun loadUserActivity(userId: Int) {
        scope.launch {
            try {
                val result = api.getUserActivity(userId)
                if (result is AuthResult.Success) {
                    _userActivity.value = result.data
                } else if (result is AuthResult.Error) {
                    AppLogger.e(TAG, "Failed to load user activity: ${result.message}")
                    postFailureReport(FailureReportParams(
                        title = Strings.userActivityLoadFailed,
                        stage = Strings.loadUserActivityStage,
                        summary = Strings.userActivityFailedSummary,
                        serviceMessage = result.message,
                        extraContext = "user_id: $userId"
                    ))
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load user activity", e)
                postFailureReport(FailureReportParams(
                    title = Strings.userActivityLoadFailed,
                    stage = Strings.loadUserActivityStage,
                    summary = Strings.userActivityExceptionSummary,
                    throwable = e,
                    extraContext = "user_id: $userId"
                ))
            }
        }
    }

    fun toggleFollow(userId: Int) {
        scope.launch {
            try {
                val current = _userProfile.value
                val result = if (current?.isFollowing == true) {
                    api.unfollowUser(userId)
                } else {
                    api.followUser(userId)
                }
                when (result) {
                    is AuthResult.Success -> {
                        val wasFollowing = current?.isFollowing == true
                        _userProfile.value = current?.copy(
                            isFollowing = !wasFollowing,
                            followerCount = current.followerCount + if (wasFollowing) -1 else 1
                        )
                        onMessage(if (wasFollowing) Strings.unfollowed else Strings.followed)
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Toggle follow failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.followToggleFailed,
                            stage = Strings.toggleFollowStage,
                            summary = Strings.followToggleFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "user_id: $userId\ncurrently_following: ${current?.isFollowing == true}"
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Toggle follow exception", e)
                postFailureReport(FailureReportParams(
                    title = Strings.followToggleFailed,
                    stage = Strings.toggleFollowStage,
                    summary = Strings.followToggleExceptionSummary,
                    throwable = e,
                    extraContext = "user_id: $userId\ncurrently_following: ${_userProfile.value?.isFollowing == true}"
                ))
            }
        }
    }

    fun loadUserFollowers(userId: Int) {
        scope.launch {
            try {
                val result = api.getUserFollowers(userId)
                when (result) {
                    is AuthResult.Success -> _followersList.value = result.data
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load user followers failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.followersLoadFailed,
                            stage = Strings.loadFollowersStage,
                            summary = Strings.followersFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "user_id: $userId"
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load followers", e)
                postFailureReport(FailureReportParams(
                    title = Strings.followersLoadFailed,
                    stage = Strings.loadFollowersStage,
                    summary = Strings.followersExceptionSummary,
                    throwable = e,
                    extraContext = "user_id: $userId"
                ))
            }
        }
    }

    fun loadUserFollowing(userId: Int) {
        scope.launch {
            try {
                val result = api.getUserFollowing(userId)
                when (result) {
                    is AuthResult.Success -> _followingList.value = result.data
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load user following failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.followingListLoadFailed,
                            stage = Strings.loadFollowingListStage,
                            summary = Strings.followingListFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "user_id: $userId"
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load following", e)
                postFailureReport(FailureReportParams(
                    title = Strings.followingListLoadFailed,
                    stage = Strings.loadFollowingListStage,
                    summary = Strings.followingListExceptionSummary,
                    throwable = e,
                    extraContext = "user_id: $userId"
                ))
            }
        }
    }
}
