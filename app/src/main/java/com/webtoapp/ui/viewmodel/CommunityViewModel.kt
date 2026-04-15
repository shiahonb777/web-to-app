package com.webtoapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.*
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * 社区 ViewModel — 正确处理 AuthResult 包装类型
 */
data class OperationFailureReport(
    val title: String,
    val summary: String,
    val details: String
)

class CommunityViewModel(
    private val cloudApiClient: CloudApiClient
) : ViewModel() {

    companion object {
        private const val TAG = "CommunityViewModel"
    }

    // ─── 模块详情 ───
    private val _moduleDetail = MutableStateFlow<CommunityModuleDetail?>(null)
    val moduleDetail: StateFlow<CommunityModuleDetail?> = _moduleDetail.asStateFlow()

    private val _moduleDetailLoading = MutableStateFlow(false)
    val moduleDetailLoading: StateFlow<Boolean> = _moduleDetailLoading.asStateFlow()

    // ─── 评论 ───
    private val _comments = MutableStateFlow<List<ModuleComment>>(emptyList())
    val comments: StateFlow<List<ModuleComment>> = _comments.asStateFlow()

    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading.asStateFlow()

    // ─── 收藏列表 ───
    private val _favorites = MutableStateFlow<List<CommunityModuleDetail>>(emptyList())
    val favorites: StateFlow<List<CommunityModuleDetail>> = _favorites.asStateFlow()

    private val _favoritesLoading = MutableStateFlow(false)
    val favoritesLoading: StateFlow<Boolean> = _favoritesLoading.asStateFlow()

    // ─── 用户主页 ───
    private val _userProfile = MutableStateFlow<CommunityUserProfile?>(null)
    val userProfile: StateFlow<CommunityUserProfile?> = _userProfile.asStateFlow()

    private val _userModules = MutableStateFlow<List<CommunityModuleDetail>>(emptyList())
    val userModules: StateFlow<List<CommunityModuleDetail>> = _userModules.asStateFlow()

    private val _userProfileLoading = MutableStateFlow(false)
    val userProfileLoading: StateFlow<Boolean> = _userProfileLoading.asStateFlow()

    // ─── 用户帖子 ───
    private val _userPosts = MutableStateFlow<List<CommunityPostItem>>(emptyList())
    val userPosts: StateFlow<List<CommunityPostItem>> = _userPosts.asStateFlow()

    // ─── 用户在线活动 ───
    private val _userActivity = MutableStateFlow<UserActivityInfo?>(null)
    val userActivity: StateFlow<UserActivityInfo?> = _userActivity.asStateFlow()

    // ─── 通知 ───
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _notificationsLoading = MutableStateFlow(false)
    val notificationsLoading: StateFlow<Boolean> = _notificationsLoading.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // ─── 动态 Feed（占位，服务端尚未实现） ───
    private val _activityFeed = MutableStateFlow<List<FeedItem>>(emptyList())
    val activityFeed: StateFlow<List<FeedItem>> = _activityFeed.asStateFlow()

    private val _activityFeedLoading = MutableStateFlow(false)
    val activityFeedLoading: StateFlow<Boolean> = _activityFeedLoading.asStateFlow()

    // ─── 通用消息 ───
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _moduleFailureReport = MutableStateFlow<OperationFailureReport?>(null)
    val moduleFailureReport: StateFlow<OperationFailureReport?> = _moduleFailureReport.asStateFlow()

    private val _notificationFailureReport = MutableStateFlow<OperationFailureReport?>(null)
    val notificationFailureReport: StateFlow<OperationFailureReport?> = _notificationFailureReport.asStateFlow()

    private val _feedFailureReport = MutableStateFlow<OperationFailureReport?>(null)
    val feedFailureReport: StateFlow<OperationFailureReport?> = _feedFailureReport.asStateFlow()

    private val _profileFailureReport = MutableStateFlow<OperationFailureReport?>(null)
    val profileFailureReport: StateFlow<OperationFailureReport?> = _profileFailureReport.asStateFlow()

    fun clearMessage() { _message.value = null }
    fun pushMessage(message: String) { _message.value = message }
    fun clearModuleFailureReport() { _moduleFailureReport.value = null }
    fun clearNotificationFailureReport() { _notificationFailureReport.value = null }
    fun clearFeedFailureReport() { _feedFailureReport.value = null }
    fun clearProfileFailureReport() { _profileFailureReport.value = null }

    // ── Phase 1 v2: Discover 页数据 ──
    private val _discoverData = MutableStateFlow<DiscoverResponse?>(null)
    val discoverData: StateFlow<DiscoverResponse?> = _discoverData.asStateFlow()

    private val _discoverLoading = MutableStateFlow(true)
    val discoverLoading: StateFlow<Boolean> = _discoverLoading.asStateFlow()

    // ── Phase 1 v2: 当前 Tab (discover / following / feed) ──
    private val _selectedTab = MutableStateFlow("discover")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    fun setSelectedTab(tab: String) {
        _selectedTab.value = tab
        when (tab) {
            "discover" -> loadDiscover()
            "following" -> loadFollowingFeed()
            else -> loadPosts(page = 1)
        }
    }

    // ── Phase 1 v2: Post type filter ──
    private val _selectedPostType = MutableStateFlow<String?>(null)
    val selectedPostType: StateFlow<String?> = _selectedPostType.asStateFlow()

    fun setSelectedPostType(type: String?) {
        _selectedPostType.value = type
        loadPosts(page = 1)
    }

    // ─── 帖子 Feed ───
    private val _posts = MutableStateFlow<List<CommunityPostItem>>(emptyList())
    val posts: StateFlow<List<CommunityPostItem>> = _posts.asStateFlow()

    private val _feedLoading = MutableStateFlow(true)
    val feedLoading: StateFlow<Boolean> = _feedLoading.asStateFlow()

    private val _feedLoadingMore = MutableStateFlow(false)
    val feedLoadingMore: StateFlow<Boolean> = _feedLoadingMore.asStateFlow()

    private val _feedRefreshing = MutableStateFlow(false)
    val feedRefreshing: StateFlow<Boolean> = _feedRefreshing.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private var currentPage = 1
    private var hasMore = true

    fun setSelectedTag(tag: String?) {
        _selectedTag.value = tag
        loadPosts(page = 1)
    }

    fun loadPosts(page: Int = 1, append: Boolean = false) {
        viewModelScope.launch {
            if (page == 1 && !append) _feedLoading.value = true else _feedLoadingMore.value = true
            try {
                val result = cloudApiClient.listCommunityPosts(
                    tag = _selectedTag.value,
                    page = page,
                    postType = _selectedPostType.value,
                )
                when (result) {
                    is AuthResult.Success -> {
                        val newPosts = result.data.posts
                        _posts.value = if (append) _posts.value + newPosts else newPosts
                        currentPage = page
                        hasMore = newPosts.size >= 20
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load posts failed: ${result.message}")
                        postFeedFailureReport(
                            title = "社区帖子加载失败",
                            stage = when {
                                append -> "加载更多帖子"
                                _feedRefreshing.value -> "刷新帖子列表"
                                else -> "加载帖子列表"
                            },
                            summary = "社区帖子请求失败，当前列表未继续更新。",
                            serviceMessage = result.message,
                            extraContext = """
                                page: $page
                                append: $append
                                selected_tag: ${_selectedTag.value ?: "<all>"}
                                selected_post_type: ${_selectedPostType.value ?: "<all>"}
                            """.trimIndent()
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load posts", e)
                postFeedFailureReport(
                    title = "社区帖子加载失败",
                    stage = when {
                        append -> "加载更多帖子"
                        _feedRefreshing.value -> "刷新帖子列表"
                        else -> "加载帖子列表"
                    },
                    summary = "社区帖子请求发生异常，当前列表未继续更新。",
                    throwable = e,
                    extraContext = """
                        page: $page
                        append: $append
                        selected_tag: ${_selectedTag.value ?: "<all>"}
                        selected_post_type: ${_selectedPostType.value ?: "<all>"}
                    """.trimIndent()
                )
            } finally {
                _feedLoading.value = false
                _feedLoadingMore.value = false
                _feedRefreshing.value = false
            }
        }
    }

    /** Phase 1 v2: 加载发现页分区数据 */
    fun loadDiscover() {
        viewModelScope.launch {
            _discoverLoading.value = true
            try {
                val result = cloudApiClient.getDiscover()
                when (result) {
                    is AuthResult.Success -> _discoverData.value = result.data
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Discover load failed: ${result.message}")
                        postFeedFailureReport(
                            title = "社区发现页加载失败",
                            stage = "加载发现页",
                            summary = "发现页内容请求失败，未获取到最新推荐内容。",
                            serviceMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Discover load error", e)
                postFeedFailureReport(
                    title = "社区发现页加载失败",
                    stage = "加载发现页",
                    summary = "发现页内容请求发生异常，未获取到最新推荐内容。",
                    throwable = e
                )
            } finally {
                _discoverLoading.value = false
            }
        }
    }

    /** Phase 1 v2: 导入配方 */
    fun importRecipe(postId: Int, onSuccess: (RecipeImportResult) -> Unit) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.importRecipe(postId)
                when (result) {
                    is AuthResult.Success -> {
                        _message.value = "配方导入成功！"
                        updatePostCaches(postId) { post ->
                            post.copy(recipeImportCount = post.recipeImportCount + 1)
                        }
                        onSuccess(result.data)
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Recipe import failed: ${result.message}")
                        postFeedFailureReport(
                            title = "配方导入失败",
                            stage = "导入帖子配方",
                            summary = "配方导入请求失败，导入操作未完成。",
                            serviceMessage = result.message,
                            extraContext = "post_id: $postId"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Recipe import error", e)
                postFeedFailureReport(
                    title = "配方导入失败",
                    stage = "导入帖子配方",
                    summary = "配方导入请求发生异常，导入操作未完成。",
                    throwable = e,
                    extraContext = "post_id: $postId"
                )
            }
        }
    }

    fun loadMorePosts() {
        if (_feedLoadingMore.value || !hasMore || _posts.value.isEmpty()) return
        loadPosts(page = currentPage + 1, append = true)
    }

    fun refreshPosts() {
        _feedRefreshing.value = true
        loadPosts(page = 1)
    }

    fun togglePostLike(postId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.togglePostLike(postId)
                when (result) {
                    is AuthResult.Success -> {
                        updatePostCaches(postId) { post ->
                            post.copy(
                                isLiked = result.data.liked,
                                likeCount = result.data.likeCount
                            )
                        }
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Like failed: ${result.message}")
                        postFeedFailureReport(
                            title = "帖子点赞失败",
                            stage = "切换帖子点赞状态",
                            summary = "帖子点赞请求失败，当前点赞状态未被修改。",
                            serviceMessage = result.message,
                            extraContext = "post_id: $postId"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Like exception", e)
                postFeedFailureReport(
                    title = "帖子点赞失败",
                    stage = "切换帖子点赞状态",
                    summary = "帖子点赞请求发生异常，当前点赞状态未被修改。",
                    throwable = e,
                    extraContext = "post_id: $postId"
                )
            }
        }
    }

    fun sharePost(postId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.sharePost(postId)
                when (result) {
                    is AuthResult.Success -> {
                        updatePostCaches(postId) { post ->
                            post.copy(shareCount = post.shareCount + 1)
                        }
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Share failed: ${result.message}")
                        postFeedFailureReport(
                            title = "帖子分享失败",
                            stage = "提交帖子分享",
                            summary = "帖子分享请求失败，当前分享计数未被更新。",
                            serviceMessage = result.message,
                            extraContext = "post_id: $postId"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Share exception", e)
                postFeedFailureReport(
                    title = "帖子分享失败",
                    stage = "提交帖子分享",
                    summary = "帖子分享请求发生异常，当前分享计数未被更新。",
                    throwable = e,
                    extraContext = "post_id: $postId"
                )
            }
        }
    }

    fun reportPost(postId: Int, reason: String = "inappropriate") {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.reportPost(postId, reason)
                when (result) {
                    is AuthResult.Success -> _message.value = "举报已提交"
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Report post failed: ${result.message}")
                        postFeedFailureReport(
                            title = "帖子举报失败",
                            stage = "提交帖子举报",
                            summary = "帖子举报请求失败，举报未提交到服务端。",
                            serviceMessage = result.message,
                            extraContext = """
                                post_id: $postId
                                reason: $reason
                            """.trimIndent()
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Report post exception", e)
                postFeedFailureReport(
                    title = "帖子举报失败",
                    stage = "提交帖子举报",
                    summary = "帖子举报请求发生异常，举报未提交到服务端。",
                    throwable = e,
                    extraContext = """
                        post_id: $postId
                        reason: $reason
                    """.trimIndent()
                )
            }
        }
    }

    // CLI-01: 删除帖子
    fun deletePost(postId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.deletePost(postId)
                when (result) {
                    is AuthResult.Success -> {
                        removePostFromCaches(postId)
                        _message.value = "帖子已删除"
                        onSuccess()
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Delete post failed: ${result.message}")
                        postFeedFailureReport(
                            title = "帖子删除失败",
                            stage = "删除帖子",
                            summary = "帖子删除请求失败，帖子仍保持原状态。",
                            serviceMessage = result.message,
                            extraContext = "post_id: $postId"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Delete post exception", e)
                postFeedFailureReport(
                    title = "帖子删除失败",
                    stage = "删除帖子",
                    summary = "帖子删除请求发生异常，帖子仍保持原状态。",
                    throwable = e,
                    extraContext = "post_id: $postId"
                )
            }
        }
    }

    // CLI-02: 编辑帖子
    fun editPost(postId: Int, content: String, onSuccess: (CommunityPostItem) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.editPost(postId, content)
                when (result) {
                    is AuthResult.Success -> {
                        val updated = result.data
                        syncPost(updated)
                        _message.value = "帖子已更新"
                        onSuccess(updated)
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Edit post failed: ${result.message}")
                        postFeedFailureReport(
                            title = "帖子编辑失败",
                            stage = "编辑帖子",
                            summary = "帖子编辑请求失败，帖子内容未被更新。",
                            serviceMessage = result.message,
                            extraContext = """
                                post_id: $postId
                                content_length: ${content.length}
                            """.trimIndent()
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Edit post exception", e)
                postFeedFailureReport(
                    title = "帖子编辑失败",
                    stage = "编辑帖子",
                    summary = "帖子编辑请求发生异常，帖子内容未被更新。",
                    throwable = e,
                    extraContext = """
                        post_id: $postId
                        content_length: ${content.length}
                    """.trimIndent()
                )
            }
        }
    }

    fun syncPost(updated: CommunityPostItem) {
        _posts.value = _posts.value.map { if (it.id == updated.id) updated else it }
        _followingPosts.value = _followingPosts.value.map { if (it.id == updated.id) updated else it }
        _trendingPosts.value = _trendingPosts.value.map { if (it.id == updated.id) updated else it }
        _userPosts.value = _userPosts.value.map { if (it.id == updated.id) updated else it }
        _discoverData.value = _discoverData.value?.copy(
            featuredShowcases = _discoverData.value?.featuredShowcases.orEmpty().map { if (it.id == updated.id) updated else it },
            trending = _discoverData.value?.trending.orEmpty().map { if (it.id == updated.id) updated else it },
            latestTutorials = _discoverData.value?.latestTutorials.orEmpty().map { if (it.id == updated.id) updated else it },
            unansweredQuestions = _discoverData.value?.unansweredQuestions.orEmpty().map { if (it.id == updated.id) updated else it },
        )
    }

    fun removePostFromCaches(postId: Int) {
        _posts.value = _posts.value.filterNot { it.id == postId }
        _followingPosts.value = _followingPosts.value.filterNot { it.id == postId }
        _trendingPosts.value = _trendingPosts.value.filterNot { it.id == postId }
        _userPosts.value = _userPosts.value.filterNot { it.id == postId }
        _discoverData.value = _discoverData.value?.copy(
            featuredShowcases = _discoverData.value?.featuredShowcases.orEmpty().filterNot { it.id == postId },
            trending = _discoverData.value?.trending.orEmpty().filterNot { it.id == postId },
            latestTutorials = _discoverData.value?.latestTutorials.orEmpty().filterNot { it.id == postId },
            unansweredQuestions = _discoverData.value?.unansweredQuestions.orEmpty().filterNot { it.id == postId },
        )
    }

    private fun buildFailureReport(
        title: String,
        stage: String,
        summary: String,
        serviceMessage: String? = null,
        throwable: Throwable? = null,
        extraContext: String? = null
    ): OperationFailureReport {
        val details = buildString {
            appendLine("stage: $stage")
            appendLine("summary: $summary")

            if (!extraContext.isNullOrBlank()) {
                appendLine()
                appendLine("context:")
                appendLine(extraContext)
            }

            appendLine()
            appendLine("error:")
            appendLine(serviceMessage ?: throwable?.message ?: "未知错误")

            throwable?.let {
                appendLine()
                appendLine("stacktrace:")
                appendLine(Log.getStackTraceString(it))
            }

            appendLine()
            appendLine("recent_logs:")
            append(AppLogger.getRecentLogTail())
        }

        return OperationFailureReport(
            title = title,
            summary = summary,
            details = details
        )
    }

    private fun postModuleFailureReport(
        title: String,
        stage: String,
        summary: String,
        serviceMessage: String? = null,
        throwable: Throwable? = null,
        extraContext: String? = null
    ) {
        _moduleFailureReport.value = buildFailureReport(
            title = title,
            stage = stage,
            summary = summary,
            serviceMessage = serviceMessage,
            throwable = throwable,
            extraContext = extraContext
        )
    }

    private fun postNotificationFailureReport(
        title: String,
        stage: String,
        summary: String,
        serviceMessage: String? = null,
        throwable: Throwable? = null,
        extraContext: String? = null
    ) {
        _notificationFailureReport.value = buildFailureReport(
            title = title,
            stage = stage,
            summary = summary,
            serviceMessage = serviceMessage,
            throwable = throwable,
            extraContext = extraContext
        )
    }

    private fun postFeedFailureReport(
        title: String,
        stage: String,
        summary: String,
        serviceMessage: String? = null,
        throwable: Throwable? = null,
        extraContext: String? = null
    ) {
        _feedFailureReport.value = buildFailureReport(
            title = title,
            stage = stage,
            summary = summary,
            serviceMessage = serviceMessage,
            throwable = throwable,
            extraContext = extraContext
        )
    }

    private fun postProfileFailureReport(
        title: String,
        stage: String,
        summary: String,
        serviceMessage: String? = null,
        throwable: Throwable? = null,
        extraContext: String? = null
    ) {
        _profileFailureReport.value = buildFailureReport(
            title = title,
            stage = stage,
            summary = summary,
            serviceMessage = serviceMessage,
            throwable = throwable,
            extraContext = extraContext
        )
    }

    private fun updatePostCaches(
        postId: Int,
        transform: (CommunityPostItem) -> CommunityPostItem
    ) {
        fun mapPosts(posts: List<CommunityPostItem>): List<CommunityPostItem> =
            posts.map { post -> if (post.id == postId) transform(post) else post }

        _posts.value = mapPosts(_posts.value)
        _followingPosts.value = mapPosts(_followingPosts.value)
        _trendingPosts.value = mapPosts(_trendingPosts.value)
        _userPosts.value = mapPosts(_userPosts.value)
        _discoverData.value = _discoverData.value?.copy(
            featuredShowcases = mapPosts(_discoverData.value?.featuredShowcases.orEmpty()),
            trending = mapPosts(_discoverData.value?.trending.orEmpty()),
            latestTutorials = mapPosts(_discoverData.value?.latestTutorials.orEmpty()),
            unansweredQuestions = mapPosts(_discoverData.value?.unansweredQuestions.orEmpty()),
        )
    }

    // ═══ 模块详情 ═══

    fun loadModuleDetail(moduleId: Int) {
        viewModelScope.launch {
            _moduleDetailLoading.value = true
            _moduleDetail.value = null
            try {
                val result = cloudApiClient.getStoreModuleById(moduleId)
                when (result) {
                    is AuthResult.Success -> {
                        _moduleDetail.value = result.data.toCommunityDetail()
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load module detail failed: ${result.message}")
                        postModuleFailureReport(
                            title = "模块详情加载失败",
                            stage = "加载模块详情",
                            summary = "模块详情接口返回失败，已停止继续处理，未再切换到其他查询路径。",
                            serviceMessage = result.message,
                            extraContext = "module_id: $moduleId"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load module detail", e)
                postModuleFailureReport(
                    title = "模块详情加载失败",
                    stage = "加载模块详情",
                    summary = "模块详情请求发生异常，已停止继续处理。",
                    throwable = e,
                    extraContext = "module_id: $moduleId"
                )
            } finally {
                _moduleDetailLoading.value = false
            }
        }
    }

    fun voteModule(moduleId: Int, voteType: String) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.voteModule(moduleId, voteType)
                when (result) {
                    is AuthResult.Success -> loadModuleDetail(moduleId)
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Vote module failed: ${result.message}")
                        postModuleFailureReport(
                            title = "模块投票失败",
                            stage = "提交模块投票",
                            summary = "模块投票请求失败，当前投票状态未被修改。",
                            serviceMessage = result.message,
                            extraContext = """
                                module_id: $moduleId
                                vote_type: $voteType
                            """.trimIndent()
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Vote module exception", e)
                postModuleFailureReport(
                    title = "模块投票失败",
                    stage = "提交模块投票",
                    summary = "模块投票请求发生异常，当前投票状态未被修改。",
                    throwable = e,
                    extraContext = """
                        module_id: $moduleId
                        vote_type: $voteType
                    """.trimIndent()
                )
            }
        }
    }

    fun toggleFavorite(moduleId: Int) {
        viewModelScope.launch {
            try {
                val current = _moduleDetail.value
                val result = if (current?.isFavorited == true) {
                    cloudApiClient.removeFavorite(moduleId)
                } else {
                    cloudApiClient.addFavorite(moduleId)
                }
                when (result) {
                    is AuthResult.Success -> {
                        _message.value = if (current?.isFavorited == true) "已取消收藏" else "已收藏"
                        loadModuleDetail(moduleId)
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Toggle favorite failed: ${result.message}")
                        postModuleFailureReport(
                            title = "模块收藏操作失败",
                            stage = "切换模块收藏状态",
                            summary = "模块收藏状态更新失败，当前收藏状态未被修改。",
                            serviceMessage = result.message,
                            extraContext = """
                                module_id: $moduleId
                                currently_favorited: ${current?.isFavorited == true}
                            """.trimIndent()
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Toggle favorite exception", e)
                postModuleFailureReport(
                    title = "模块收藏操作失败",
                    stage = "切换模块收藏状态",
                    summary = "模块收藏状态更新发生异常，当前收藏状态未被修改。",
                    throwable = e,
                    extraContext = """
                        module_id: $moduleId
                        currently_favorited: ${_moduleDetail.value?.isFavorited == true}
                    """.trimIndent()
                )
            }
        }
    }

    fun reportModule(moduleId: Int, reason: String, details: String?) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.reportModule(moduleId, reason, details)
                when (result) {
                    is AuthResult.Success -> _message.value = "举报已提交"
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Report module failed: ${result.message}")
                        postModuleFailureReport(
                            title = "模块举报失败",
                            stage = "提交模块举报",
                            summary = "模块举报请求失败，举报未提交到服务端。",
                            serviceMessage = result.message,
                            extraContext = """
                                module_id: $moduleId
                                reason: $reason
                                details: ${details ?: "<empty>"}
                            """.trimIndent()
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Report module exception", e)
                postModuleFailureReport(
                    title = "模块举报失败",
                    stage = "提交模块举报",
                    summary = "模块举报请求发生异常，举报未提交到服务端。",
                    throwable = e,
                    extraContext = """
                        module_id: $moduleId
                        reason: $reason
                        details: ${details ?: "<empty>"}
                    """.trimIndent()
                )
            }
        }
    }

    // ═══ 评论 ═══

    fun loadComments(moduleId: Int) {
        viewModelScope.launch {
            _commentsLoading.value = true
            try {
                val result = cloudApiClient.listComments(moduleId)
                when (result) {
                    is AuthResult.Success -> _comments.value = result.data
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load comments failed: ${result.message}")
                        postModuleFailureReport(
                            title = "模块评论加载失败",
                            stage = "加载模块评论",
                            summary = "模块评论列表请求失败，未获取到最新评论数据。",
                            serviceMessage = result.message,
                            extraContext = "module_id: $moduleId"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load comments", e)
                postModuleFailureReport(
                    title = "模块评论加载失败",
                    stage = "加载模块评论",
                    summary = "模块评论列表请求发生异常，未获取到最新评论数据。",
                    throwable = e,
                    extraContext = "module_id: $moduleId"
                )
            } finally {
                _commentsLoading.value = false
            }
        }
    }

    fun addComment(
        moduleId: Int,
        content: String,
        parentId: Int? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.addComment(moduleId, content, parentId)
                when (result) {
                    is AuthResult.Success -> {
                        _message.value = "评论已发布"
                        onSuccess()
                        loadComments(moduleId)
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Add comment failed: ${result.message}")
                        postModuleFailureReport(
                            title = "模块评论提交失败",
                            stage = "提交模块评论",
                            summary = "模块评论提交失败，评论内容未写入服务端。",
                            serviceMessage = result.message,
                            extraContext = """
                                module_id: $moduleId
                                parent_id: ${parentId ?: "null"}
                                content: $content
                            """.trimIndent()
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Add comment exception", e)
                postModuleFailureReport(
                    title = "模块评论提交失败",
                    stage = "提交模块评论",
                    summary = "模块评论提交发生异常，评论内容未写入服务端。",
                    throwable = e,
                    extraContext = """
                        module_id: $moduleId
                        parent_id: ${parentId ?: "null"}
                        content: $content
                    """.trimIndent()
                )
            }
        }
    }

    // ═══ 收藏列表 ═══

    fun loadFavorites() {
        viewModelScope.launch {
            _favoritesLoading.value = true
            try {
                val result = cloudApiClient.listFavorites()
                when (result) {
                    is AuthResult.Success -> {
                        val (modules, _) = result.data
                        _favorites.value = modules.map { it.toCommunityDetail() }
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load favorites failed: ${result.message}")
                        postProfileFailureReport(
                            title = "收藏列表加载失败",
                            stage = "加载收藏列表",
                            summary = "收藏列表请求失败，未获取到最新收藏数据。",
                            serviceMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load favorites", e)
                postProfileFailureReport(
                    title = "收藏列表加载失败",
                    stage = "加载收藏列表",
                    summary = "收藏列表请求发生异常，未获取到最新收藏数据。",
                    throwable = e
                )
            } finally {
                _favoritesLoading.value = false
            }
        }
    }

    // ─── 用户团队作品 ───
    private val _userTeamWorks = MutableStateFlow<List<TeamWorkItem>>(emptyList())
    val userTeamWorks: StateFlow<List<TeamWorkItem>> = _userTeamWorks.asStateFlow()

    // ═══ 用户主页 ═══

    fun loadUserProfile(userId: Int) {
        viewModelScope.launch {
            _userProfileLoading.value = true
            try {
                val result = cloudApiClient.getUserProfile(userId)
                when (result) {
                    is AuthResult.Success -> {
                        val profile = result.data
                        _userProfile.value = profile
                        loadUserModulesInternal(profile.id)
                        loadUserTeamWorks(userId)
                        loadUserPosts(userId)
                        loadUserActivity(userId)
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load user profile failed: ${result.message}")
                        postProfileFailureReport(
                            title = "用户主页加载失败",
                            stage = "加载用户主页",
                            summary = "用户主页请求失败，未获取到用户资料。",
                            serviceMessage = result.message,
                            extraContext = "user_id: $userId"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load user profile", e)
                postProfileFailureReport(
                    title = "用户主页加载失败",
                    stage = "加载用户主页",
                    summary = "用户主页请求发生异常，未获取到用户资料。",
                    throwable = e,
                    extraContext = "user_id: $userId"
                )
            } finally {
                _userProfileLoading.value = false
            }
        }
    }

    private fun loadUserModulesInternal(userId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getUserModules(userId)
                when (result) {
                    is AuthResult.Success -> {
                        val (modules, _) = result.data
                        _userModules.value = modules.map { it.toCommunityDetail() }
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load user modules failed: ${result.message}")
                        postProfileFailureReport(
                            title = "用户模块加载失败",
                            stage = "加载用户模块",
                            summary = "用户模块请求失败，未获取到模块列表。",
                            serviceMessage = result.message,
                            extraContext = "user_id: $userId"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load user modules", e)
                postProfileFailureReport(
                    title = "用户模块加载失败",
                    stage = "加载用户模块",
                    summary = "用户模块请求发生异常，未获取到模块列表。",
                    throwable = e,
                    extraContext = "user_id: $userId"
                )
            }
        }
    }

    fun loadUserTeamWorks(userId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getUserTeamWorks(userId)
                if (result is AuthResult.Success) {
                    _userTeamWorks.value = result.data.works
                } else if (result is AuthResult.Error) {
                    AppLogger.e(TAG, "Failed to load team works: ${result.message}")
                    postProfileFailureReport(
                        title = "用户团队作品加载失败",
                        stage = "加载用户团队作品",
                        summary = "用户团队作品请求失败，未获取到团队作品列表。",
                        serviceMessage = result.message,
                        extraContext = "user_id: $userId"
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load user team works", e)
                postProfileFailureReport(
                    title = "用户团队作品加载失败",
                    stage = "加载用户团队作品",
                    summary = "用户团队作品请求发生异常，未获取到团队作品列表。",
                    throwable = e,
                    extraContext = "user_id: $userId"
                )
            }
        }
    }

    fun loadUserPosts(userId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getUserPosts(userId)
                if (result is AuthResult.Success) {
                    _userPosts.value = result.data.posts
                } else if (result is AuthResult.Error) {
                    AppLogger.e(TAG, "Failed to load user posts: ${result.message}")
                    postProfileFailureReport(
                        title = "用户帖子加载失败",
                        stage = "加载用户帖子",
                        summary = "用户帖子请求失败，未获取到帖子列表。",
                        serviceMessage = result.message,
                        extraContext = "user_id: $userId"
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load user posts", e)
                postProfileFailureReport(
                    title = "用户帖子加载失败",
                    stage = "加载用户帖子",
                    summary = "用户帖子请求发生异常，未获取到帖子列表。",
                    throwable = e,
                    extraContext = "user_id: $userId"
                )
            }
        }
    }

    fun loadUserActivity(userId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getUserActivity(userId)
                if (result is AuthResult.Success) {
                    _userActivity.value = result.data
                } else if (result is AuthResult.Error) {
                    AppLogger.e(TAG, "Failed to load user activity: ${result.message}")
                    postProfileFailureReport(
                        title = "用户在线活动加载失败",
                        stage = "加载用户在线活动",
                        summary = "用户在线活动请求失败，未获取到活动统计。",
                        serviceMessage = result.message,
                        extraContext = "user_id: $userId"
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load user activity", e)
                postProfileFailureReport(
                    title = "用户在线活动加载失败",
                    stage = "加载用户在线活动",
                    summary = "用户在线活动请求发生异常，未获取到活动统计。",
                    throwable = e,
                    extraContext = "user_id: $userId"
                )
            }
        }
    }

    // CLI-07: 乐观更新关注状态，避免全量重载6个API
    fun toggleFollow(userId: Int) {
        viewModelScope.launch {
            try {
                val current = _userProfile.value
                val result = if (current?.isFollowing == true) {
                    cloudApiClient.unfollowUser(userId)
                } else {
                    cloudApiClient.followUser(userId)
                }
                when (result) {
                    is AuthResult.Success -> {
                        val wasFollowing = current?.isFollowing == true
                        _userProfile.value = current?.copy(
                            isFollowing = !wasFollowing,
                            followerCount = current.followerCount + if (wasFollowing) -1 else 1
                        )
                        _message.value = if (wasFollowing) "已取消关注" else "已关注"
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Toggle follow failed: ${result.message}")
                        postProfileFailureReport(
                            title = "关注状态更新失败",
                            stage = "切换用户关注状态",
                            summary = "用户关注状态更新失败，当前状态未被修改。",
                            serviceMessage = result.message,
                            extraContext = """
                                user_id: $userId
                                currently_following: ${current?.isFollowing == true}
                            """.trimIndent()
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Toggle follow exception", e)
                postProfileFailureReport(
                    title = "关注状态更新失败",
                    stage = "切换用户关注状态",
                    summary = "用户关注状态更新发生异常，当前状态未被修改。",
                    throwable = e,
                    extraContext = """
                        user_id: $userId
                        currently_following: ${_userProfile.value?.isFollowing == true}
                    """.trimIndent()
                )
            }
        }
    }

    // ═══ 通知 ═══

    fun loadNotifications() {
        viewModelScope.launch {
            _notificationsLoading.value = true
            try {
                val result = cloudApiClient.listNotifications()
                when (result) {
                    is AuthResult.Success -> {
                        val (notifications, _) = result.data
                        _notifications.value = notifications
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load notifications failed: ${result.message}")
                        postNotificationFailureReport(
                            title = "通知列表加载失败",
                            stage = "加载通知列表",
                            summary = "通知列表请求失败，未获取到最新通知数据。",
                            serviceMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load notifications", e)
                postNotificationFailureReport(
                    title = "通知列表加载失败",
                    stage = "加载通知列表",
                    summary = "通知列表请求发生异常，未获取到最新通知数据。",
                    throwable = e
                )
            } finally {
                _notificationsLoading.value = false
            }
        }
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getUnreadNotificationCount()
                when (result) {
                    is AuthResult.Success -> _unreadCount.value = result.data
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load unread notification count failed: ${result.message}")
                        postNotificationFailureReport(
                            title = "未读通知计数加载失败",
                            stage = "加载未读通知计数",
                            summary = "未读通知计数请求失败，角标数量未刷新。",
                            serviceMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Load unread notification count exception", e)
                postNotificationFailureReport(
                    title = "未读通知计数加载失败",
                    stage = "加载未读通知计数",
                    summary = "未读通知计数请求发生异常，角标数量未刷新。",
                    throwable = e
                )
            }
        }
    }

    fun markNotificationRead(notificationId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                when (val result = cloudApiClient.markNotificationRead(notificationId)) {
                    is AuthResult.Success -> {
                        val wasUnread = _notifications.value.any { it.id == notificationId && !it.isRead }
                        _notifications.value = _notifications.value.map { notification ->
                            if (notification.id == notificationId) notification.copy(isRead = true) else notification
                        }
                        if (wasUnread) {
                            _unreadCount.value = (_unreadCount.value - 1).coerceAtLeast(0)
                        }
                        onSuccess()
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Mark notification read failed: ${result.message}")
                        postNotificationFailureReport(
                            title = "通知已读更新失败",
                            stage = "标记通知为已读",
                            summary = "通知已读状态更新失败，当前通知仍保持未读状态。",
                            serviceMessage = result.message,
                            extraContext = "notification_id: $notificationId"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Mark notification read exception", e)
                postNotificationFailureReport(
                    title = "通知已读更新失败",
                    stage = "标记通知为已读",
                    summary = "通知已读状态更新发生异常，当前通知仍保持未读状态。",
                    throwable = e,
                    extraContext = "notification_id: $notificationId"
                )
            }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            try {
                when (val result = cloudApiClient.markAllNotificationsRead()) {
                    is AuthResult.Success -> {
                        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
                        _unreadCount.value = 0
                        _message.value = "已全部标为已读"
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Mark all notifications read failed: ${result.message}")
                        postNotificationFailureReport(
                            title = "全部通知已读更新失败",
                            stage = "全部标记通知为已读",
                            summary = "批量更新通知已读状态失败，通知列表未被修改。",
                            serviceMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Mark all notifications read exception", e)
                postNotificationFailureReport(
                    title = "全部通知已读更新失败",
                    stage = "全部标记通知为已读",
                    summary = "批量更新通知已读状态发生异常，通知列表未被修改。",
                    throwable = e
                )
            }
        }
    }

    // ═══ 动态 Feed ═══

    fun loadFeed() {
        viewModelScope.launch {
            _activityFeedLoading.value = true
            try {
                // 使用帖子列表接口而非旧的模块 Feed 接口
                val result = cloudApiClient.listCommunityPosts(page = 1, size = 20)
                when (result) {
                    is AuthResult.Success -> {
                        _activityFeed.value = emptyList() // Feed items are separate
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load activity feed failed: ${result.message}")
                        postNotificationFailureReport(
                            title = "动态加载失败",
                            stage = "加载通知动态",
                            summary = "通知动态请求失败，未获取到最新动态内容。",
                            serviceMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load feed", e)
                postNotificationFailureReport(
                    title = "动态加载失败",
                    stage = "加载通知动态",
                    summary = "通知动态请求发生异常，未获取到最新动态内容。",
                    throwable = e
                )
            } finally {
                _activityFeedLoading.value = false
            }
        }
    }

    // ─── 关注动态 Feed ───

    private val _followingPosts = MutableStateFlow<List<CommunityPostItem>>(emptyList())
    val followingPosts: StateFlow<List<CommunityPostItem>> = _followingPosts.asStateFlow()

    private val _followingLoading = MutableStateFlow(false)
    val followingLoading: StateFlow<Boolean> = _followingLoading.asStateFlow()

    fun loadFollowingFeed(page: Int = 1) {
        viewModelScope.launch {
            _followingLoading.value = true
            try {
                val result = cloudApiClient.getFollowingFeed(page)
                when (result) {
                    is AuthResult.Success -> {
                        _followingPosts.value = result.data
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load following feed failed: ${result.message}")
                        postFeedFailureReport(
                            title = "关注动态加载失败",
                            stage = "加载关注动态",
                            summary = "关注动态请求失败，未获取到最新关注内容。",
                            serviceMessage = result.message,
                            extraContext = "page: $page"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load following feed", e)
                postFeedFailureReport(
                    title = "关注动态加载失败",
                    stage = "加载关注动态",
                    summary = "关注动态请求发生异常，未获取到最新关注内容。",
                    throwable = e,
                    extraContext = "page: $page"
                )
            } finally {
                _followingLoading.value = false
            }
        }
    }

    // ─── 趋势 Feed ───

    private val _trendingPosts = MutableStateFlow<List<CommunityPostItem>>(emptyList())
    val trendingPosts: StateFlow<List<CommunityPostItem>> = _trendingPosts.asStateFlow()

    private val _trendingLoading = MutableStateFlow(false)
    val trendingLoading: StateFlow<Boolean> = _trendingLoading.asStateFlow()

    fun loadTrendingFeed(page: Int = 1) {
        viewModelScope.launch {
            _trendingLoading.value = true
            try {
                val result = cloudApiClient.getTrendingFeed(page)
                when (result) {
                    is AuthResult.Success -> {
                        _trendingPosts.value = result.data
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load trending feed failed: ${result.message}")
                        postFeedFailureReport(
                            title = "热门动态加载失败",
                            stage = "加载热门动态",
                            summary = "热门动态请求失败，未获取到最新热门内容。",
                            serviceMessage = result.message,
                            extraContext = "page: $page"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load trending feed", e)
                postFeedFailureReport(
                    title = "热门动态加载失败",
                    stage = "加载热门动态",
                    summary = "热门动态请求发生异常，未获取到最新热门内容。",
                    throwable = e,
                    extraContext = "page: $page"
                )
            } finally {
                _trendingLoading.value = false
            }
        }
    }

    // ─── 粉丝/关注列表 ───

    private val _followersList = MutableStateFlow<List<CommunityUserProfile>>(emptyList())
    val followersList: StateFlow<List<CommunityUserProfile>> = _followersList.asStateFlow()

    private val _followingList = MutableStateFlow<List<CommunityUserProfile>>(emptyList())
    val followingList: StateFlow<List<CommunityUserProfile>> = _followingList.asStateFlow()

    fun loadUserFollowers(userId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getUserFollowers(userId)
                when (result) {
                    is AuthResult.Success -> _followersList.value = result.data
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load user followers failed: ${result.message}")
                        postProfileFailureReport(
                            title = "粉丝列表加载失败",
                            stage = "加载粉丝列表",
                            summary = "粉丝列表请求失败，未获取到最新粉丝数据。",
                            serviceMessage = result.message,
                            extraContext = "user_id: $userId"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load followers", e)
                postProfileFailureReport(
                    title = "粉丝列表加载失败",
                    stage = "加载粉丝列表",
                    summary = "粉丝列表请求发生异常，未获取到最新粉丝数据。",
                    throwable = e,
                    extraContext = "user_id: $userId"
                )
            }
        }
    }

    fun loadUserFollowing(userId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getUserFollowing(userId)
                when (result) {
                    is AuthResult.Success -> _followingList.value = result.data
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load user following failed: ${result.message}")
                        postProfileFailureReport(
                            title = "关注列表加载失败",
                            stage = "加载关注列表",
                            summary = "关注列表请求失败，未获取到最新关注数据。",
                            serviceMessage = result.message,
                            extraContext = "user_id: $userId"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load following", e)
                postProfileFailureReport(
                    title = "关注列表加载失败",
                    stage = "加载关注列表",
                    summary = "关注列表请求发生异常，未获取到最新关注数据。",
                    throwable = e,
                    extraContext = "user_id: $userId"
                )
            }
        }
    }

    // ─── 用户搜索 ───

    private val _searchResults = MutableStateFlow<List<CommunityUserProfile>>(emptyList())
    val searchResults: StateFlow<List<CommunityUserProfile>> = _searchResults.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    // CLI-03: 搜索防抖 — 使用 MutableStateFlow + debounce
    private val _searchQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    private fun initSearchDebounce() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { query ->
                    _searchLoading.value = true
                    try {
                        val result = cloudApiClient.searchUsers(query)
                        when (result) {
                            is AuthResult.Success -> _searchResults.value = result.data
                            is AuthResult.Error -> {
                                AppLogger.e(TAG, "Search users failed: ${result.message}")
                                postFeedFailureReport(
                                    title = "用户搜索失败",
                                    stage = "搜索用户",
                                    summary = "用户搜索请求失败，未返回搜索结果。",
                                    serviceMessage = result.message,
                                    extraContext = "query: $query"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "Failed to search users", e)
                        postFeedFailureReport(
                            title = "用户搜索失败",
                            stage = "搜索用户",
                            summary = "用户搜索请求发生异常，未返回搜索结果。",
                            throwable = e,
                            extraContext = "query: $query"
                        )
                    } finally {
                        _searchLoading.value = false
                    }
                }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searchQuery.value = ""
            return
        }
        _searchQuery.value = query
    }

    // ─── 帖子搜索 ───

    private val _postSearchResults = MutableStateFlow<List<CommunityPostItem>>(emptyList())
    val postSearchResults: StateFlow<List<CommunityPostItem>> = _postSearchResults.asStateFlow()

    private val _postSearchLoading = MutableStateFlow(false)
    val postSearchLoading: StateFlow<Boolean> = _postSearchLoading.asStateFlow()

    private val _postSearchQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    private fun initPostSearchDebounce() {
        viewModelScope.launch {
            _postSearchQuery
                .debounce(300)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { query ->
                    _postSearchLoading.value = true
                    try {
                        val result = cloudApiClient.listCommunityPosts(search = query)
                        when (result) {
                            is AuthResult.Success -> _postSearchResults.value = result.data.posts
                            is AuthResult.Error -> {
                                AppLogger.e(TAG, "Search posts failed: ${result.message}")
                                postFeedFailureReport(
                                    title = "帖子搜索失败",
                                    stage = "搜索帖子",
                                    summary = "帖子搜索请求失败，未返回搜索结果。",
                                    serviceMessage = result.message,
                                    extraContext = "query: $query"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "Failed to search posts", e)
                        postFeedFailureReport(
                            title = "帖子搜索失败",
                            stage = "搜索帖子",
                            summary = "帖子搜索请求发生异常，未返回搜索结果。",
                            throwable = e,
                            extraContext = "query: $query"
                        )
                    } finally {
                        _postSearchLoading.value = false
                    }
                }
        }
    }

    fun searchPosts(query: String) {
        if (query.isBlank()) {
            _postSearchResults.value = emptyList()
            _postSearchQuery.value = ""
            return
        }
        _postSearchQuery.value = query
    }

    // ─── @提及搜索 (用于评论/发帖输入框 @ 弹窗) ───

    private val _mentionResults = MutableStateFlow<List<CommunityUserProfile>>(emptyList())
    val mentionResults: StateFlow<List<CommunityUserProfile>> = _mentionResults.asStateFlow()

    private val _mentionLoading = MutableStateFlow(false)
    val mentionLoading: StateFlow<Boolean> = _mentionLoading.asStateFlow()

    private val _mentionQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    private fun initMentionSearchDebounce() {
        viewModelScope.launch {
            _mentionQuery
                .debounce(200)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { query ->
                    _mentionLoading.value = true
                    try {
                        val result = cloudApiClient.searchUsers(query, size = 6)
                        when (result) {
                            is AuthResult.Success -> _mentionResults.value = result.data
                            is AuthResult.Error -> {
                                AppLogger.e(TAG, "Search mentions failed: ${result.message}")
                                postFeedFailureReport(
                                    title = "@用户搜索失败",
                                    stage = "搜索@提及用户",
                                    summary = "@用户搜索请求失败，未返回可提及的用户结果。",
                                    serviceMessage = result.message,
                                    extraContext = "query: $query"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "Failed to search mentions", e)
                        postFeedFailureReport(
                            title = "@用户搜索失败",
                            stage = "搜索@提及用户",
                            summary = "@用户搜索请求发生异常，未返回可提及的用户结果。",
                            throwable = e,
                            extraContext = "query: $query"
                        )
                    } finally {
                        _mentionLoading.value = false
                    }
                }
        }
    }

    fun searchMentions(query: String) {
        if (query.isBlank()) {
            _mentionResults.value = emptyList()
            _mentionQuery.value = ""
            return
        }
        _mentionQuery.value = query
    }

    fun clearMentionResults() {
        _mentionResults.value = emptyList()
        _mentionQuery.value = ""
    }

    // ═══ init block — must be AFTER all StateFlow declarations ═══
    init {
        initSearchDebounce()
        initPostSearchDebounce()
        initMentionSearchDebounce()
    }

    // ─── 高级帖子搜索（支持 postType + sort 过滤） ───

    fun searchPostsFiltered(query: String, postType: String? = null, sortBy: String = "relevance") {
        if (query.isBlank()) {
            _postSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _postSearchLoading.value = true
            try {
                val result = cloudApiClient.listCommunityPosts(
                    search = query,
                    postType = postType
                )
                when (result) {
                    is AuthResult.Success -> {
                        var posts = result.data.posts
                        posts = when (sortBy) {
                            "newest" -> posts.sortedByDescending { it.createdAt }
                            "likes" -> posts.sortedByDescending { it.likeCount }
                            else -> posts
                        }
                        _postSearchResults.value = posts
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Search posts with filters failed: ${result.message}")
                        postFeedFailureReport(
                            title = "帖子搜索失败",
                            stage = "搜索帖子",
                            summary = "帖子搜索请求失败，未返回筛选后的搜索结果。",
                            serviceMessage = result.message,
                            extraContext = """
                                query: $query
                                post_type: ${postType ?: "<all>"}
                                sort_by: $sortBy
                            """.trimIndent()
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to search posts with filters", e)
                postFeedFailureReport(
                    title = "帖子搜索失败",
                    stage = "搜索帖子",
                    summary = "帖子搜索请求发生异常，未返回筛选后的搜索结果。",
                    throwable = e,
                    extraContext = """
                        query: $query
                        post_type: ${postType ?: "<all>"}
                        sort_by: $sortBy
                    """.trimIndent()
                )
            } finally {
                _postSearchLoading.value = false
            }
        }
    }

    // ─── @mention 用户名解析（点击 @mention → 导航到用户） ───

    fun resolveUserByUsername(username: String, onResolved: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.searchUsers(username, size = 5)
                if (result is AuthResult.Success) {
                    val exactMatch = result.data.find {
                        it.username.equals(username, ignoreCase = true)
                    }
                    exactMatch?.let { onResolved(it.id) }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to resolve user: $username", e)
            }
        }
    }

    // ─── 热门/精选模块 ───

    private val _trendingModules = MutableStateFlow<List<StoreModuleInfo>>(emptyList())
    val trendingModules: StateFlow<List<StoreModuleInfo>> = _trendingModules.asStateFlow()

    private val _featuredModules = MutableStateFlow<List<StoreModuleInfo>>(emptyList())
    val featuredModules: StateFlow<List<StoreModuleInfo>> = _featuredModules.asStateFlow()

    fun loadTrendingModules() {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getTrendingModules()
                when (result) {
                    is AuthResult.Success -> _trendingModules.value = result.data
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load trending modules failed: ${result.message}")
                        postFeedFailureReport(
                            title = "热门模块加载失败",
                            stage = "加载热门模块",
                            summary = "热门模块请求失败，未获取到最新热门模块列表。",
                            serviceMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load trending modules", e)
                postFeedFailureReport(
                    title = "热门模块加载失败",
                    stage = "加载热门模块",
                    summary = "热门模块请求发生异常，未获取到最新热门模块列表。",
                    throwable = e
                )
            }
        }
    }

    fun loadFeaturedModules() {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getFeaturedModules()
                when (result) {
                    is AuthResult.Success -> _featuredModules.value = result.data
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load featured modules failed: ${result.message}")
                        postFeedFailureReport(
                            title = "精选模块加载失败",
                            stage = "加载精选模块",
                            summary = "精选模块请求失败，未获取到最新精选模块列表。",
                            serviceMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load featured modules", e)
                postFeedFailureReport(
                    title = "精选模块加载失败",
                    stage = "加载精选模块",
                    summary = "精选模块请求发生异常，未获取到最新精选模块列表。",
                    throwable = e
                )
            }
        }
    }
}

// ═══ StoreModuleInfo → CommunityModuleDetail 转换 ═══
private fun StoreModuleInfo.toCommunityDetail() = CommunityModuleDetail(
    id = this.id,
    name = this.name,
    description = this.description,
    icon = this.icon,
    category = this.category,
    tags = this.tags,
    versionName = this.versionName,
    downloads = this.downloads,
    rating = this.rating,
    ratingCount = this.ratingCount,
    isFeatured = this.isFeatured,
    authorName = this.authorName,
    authorId = 0, // StoreModuleInfo 暂无 authorId
    shareCode = this.shareCode,
    userVote = null,
    isFavorited = false,
    createdAt = this.createdAt,
    updatedAt = null
)
