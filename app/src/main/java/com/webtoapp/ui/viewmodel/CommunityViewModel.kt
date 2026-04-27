package com.webtoapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.*
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.ui.viewmodel.community.FailureReportParams
import com.webtoapp.ui.viewmodel.community.ModuleStore
import com.webtoapp.ui.viewmodel.community.NotificationStore
import com.webtoapp.ui.viewmodel.community.UserStore
import kotlinx.coroutines.CoroutineScope
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
    private val cloudApiClient: CloudApiClient,
    private val tokenManager: com.webtoapp.core.auth.TokenManager
) : ViewModel() {

    companion object {
        private const val TAG = "CommunityViewModel"
    }

    // ─── Stores ───
    private val moduleStore = ModuleStore(viewModelScope, cloudApiClient, ::dispatchModuleFailureReport, ::pushMessage)
    private val notificationStore = NotificationStore(viewModelScope, cloudApiClient, ::dispatchNotificationFailureReport)
    private val userStore = UserStore(viewModelScope, cloudApiClient, ::dispatchProfileFailureReport, ::pushMessage)

    // ─── 模块详情 (delegated) ───
    val moduleDetail: StateFlow<ModuleItem?> get() = moduleStore.moduleDetail
    val moduleDetailLoading: StateFlow<Boolean> get() = moduleStore.moduleDetailLoading

    // ─── 评论 (delegated) ───
    val comments: StateFlow<List<ModuleComment>> get() = moduleStore.comments
    val commentsLoading: StateFlow<Boolean> get() = moduleStore.commentsLoading
    val ratingDistribution: StateFlow<Map<Int, Int>> get() = moduleStore.ratingDistribution
    val commentSortOrder: StateFlow<String> get() = moduleStore.commentSortOrder

    // ─── 收藏列表 (delegated) ───
    val favorites: StateFlow<List<ModuleItem>> get() = moduleStore.favorites
    val favoritesLoading: StateFlow<Boolean> get() = moduleStore.favoritesLoading

    // ─── 市场列表 (delegated) ───
    val storeModules: StateFlow<List<ModuleItem>> get() = moduleStore.storeModules
    val storeLoading: StateFlow<Boolean> get() = moduleStore.storeLoading
    val storeTotal: StateFlow<Int> get() = moduleStore.storeTotal

    // ─── 用户主页 (delegated) ───
    val userProfile: StateFlow<CommunityUserProfile?> get() = userStore.userProfile
    val userModules: StateFlow<List<ModuleItem>> get() = userStore.userModules
    val userProfileLoading: StateFlow<Boolean> get() = userStore.userProfileLoading

    // ─── 用户帖子 (delegated) ───
    val userPosts: StateFlow<List<CommunityPostItem>> get() = userStore.userPosts

    // ─── 用户在线活动 (delegated) ───
    val userActivity: StateFlow<UserActivityInfo?> get() = userStore.userActivity

    // ─── 通知 (delegated) ───
    val notifications: StateFlow<List<NotificationItem>> get() = notificationStore.notifications
    val notificationsLoading: StateFlow<Boolean> get() = notificationStore.notificationsLoading
    val unreadCount: StateFlow<Int> get() = notificationStore.unreadCount

    // ─── 动态 Feed (aliases for backward compatibility) ───
    @Deprecated("Use posts/feedLoading instead", ReplaceWith("posts"))
    val activityFeed: StateFlow<List<FeedItem>> get() = _activityFeed
    @Deprecated("Use feedLoading instead", ReplaceWith("feedLoading"))
    val activityFeedLoading: StateFlow<Boolean> get() = feedLoading
    private val _activityFeed = MutableStateFlow<List<FeedItem>>(emptyList())

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

    enum class FailureReportDomain {
        MODULE, NOTIFICATION, FEED, PROFILE
    }

    private val _failureReportFlows = mapOf(
        FailureReportDomain.MODULE to _moduleFailureReport,
        FailureReportDomain.NOTIFICATION to _notificationFailureReport,
        FailureReportDomain.FEED to _feedFailureReport,
        FailureReportDomain.PROFILE to _profileFailureReport,
    )

    fun clearFailureReport(domain: FailureReportDomain) {
        _failureReportFlows[domain]?.value = null
    }

    fun clearModuleFailureReport() { clearFailureReport(FailureReportDomain.MODULE) }
    fun clearNotificationFailureReport() { clearFailureReport(FailureReportDomain.NOTIFICATION) }
    fun clearFeedFailureReport() { clearFailureReport(FailureReportDomain.FEED) }
    fun clearProfileFailureReport() { clearFailureReport(FailureReportDomain.PROFILE) }

    private fun dispatchModuleFailureReport(p: FailureReportParams) =
        postFailureReport(FailureReportDomain.MODULE, p.title, p.stage, p.summary, p.serviceMessage, p.throwable, p.extraContext)
    private fun dispatchNotificationFailureReport(p: FailureReportParams) =
        postFailureReport(FailureReportDomain.NOTIFICATION, p.title, p.stage, p.summary, p.serviceMessage, p.throwable, p.extraContext)
    private fun dispatchProfileFailureReport(p: FailureReportParams) =
        postFailureReport(FailureReportDomain.PROFILE, p.title, p.stage, p.summary, p.serviceMessage, p.throwable, p.extraContext)

    // ── Hot tab: posts sorted by popularity ──
    private val _hotPosts = MutableStateFlow<List<CommunityPostItem>>(emptyList())
    val hotPosts: StateFlow<List<CommunityPostItem>> = _hotPosts.asStateFlow()

    private val _hotLoading = MutableStateFlow(true)
    val hotLoading: StateFlow<Boolean> = _hotLoading.asStateFlow()

    // ── Phase 1 v2: 当前 Tab (feed / discover / following) ──
    private val _selectedTab = MutableStateFlow("feed")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    // ── Sort direction for feed & discover tabs ──
    private val _feedSortAsc = MutableStateFlow(false)   // false=desc(newest/hottest first), true=asc
    val feedSortAsc: StateFlow<Boolean> = _feedSortAsc.asStateFlow()
    private val _hotSortAsc = MutableStateFlow(false)    // false=desc(hottest first), true=asc
    val hotSortAsc: StateFlow<Boolean> = _hotSortAsc.asStateFlow()

    fun toggleFeedSort() {
        _feedSortAsc.value = !_feedSortAsc.value
        loadPosts(page = 1, force = true)
    }
    fun toggleHotSort() {
        _hotSortAsc.value = !_hotSortAsc.value
        loadHotPosts()
    }

    fun setSelectedTab(tab: String) {
        _selectedTab.value = tab
        when (tab) {
            "discover" -> loadHotPosts()
            "following" -> { if (tokenManager.isLoggedIn()) loadFollowingFeed() }
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
    private var lastLoadTime = 0L  // Smart throttle: skip reload if <30s since last load

    fun setSelectedTag(tag: String?) {
        _selectedTag.value = tag
        loadPosts(page = 1)
    }

    fun loadPosts(page: Int = 1, append: Boolean = false, force: Boolean = false) {
        // Smart throttle: skip if data was loaded <30s ago (unless forced or appending)
        if (!force && !append && page == 1 && _posts.value.isNotEmpty()) {
            val elapsed = System.currentTimeMillis() - lastLoadTime
            if (elapsed < 30_000L) {
                _feedLoading.value = false
                _feedRefreshing.value = false
                return
            }
        }
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
                        _posts.value = if (append) {
                            // When appending, keep API order (already sorted by time)
                            _posts.value + newPosts
                        } else {
                            // Full refresh: apply sort direction
                            if (_feedSortAsc.value) newPosts.reversed() else newPosts
                        }
                        currentPage = page
                        hasMore = newPosts.size >= 10
                        lastLoadTime = System.currentTimeMillis()
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load posts failed: ${result.message}")
                        postFeedFailureReport(
                            title = Strings.communityPostsLoadFailed,
                            stage = when {
                                append -> Strings.loadMorePostsStage
                                _feedRefreshing.value -> Strings.refreshPostsStage
                                else -> Strings.loadPostsStage
                            },
                            summary = Strings.communityPostsFailedSummary,
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
                    title = Strings.communityPostsLoadFailed,
                    stage = when {
                        append -> Strings.loadMorePostsStage
                        _feedRefreshing.value -> Strings.refreshPostsStage
                        else -> Strings.loadPostsStage
                    },
                    summary = Strings.communityPostsExceptionSummary,
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

    /** Load hot posts — sorted by likeCount for the hot tab */
    private var hotPage = 1
    private var hotHasMore = true

    fun loadHotPosts() {
        viewModelScope.launch {
            _hotLoading.value = true
            hotPage = 1
            hotHasMore = true
            try {
                val result = cloudApiClient.listCommunityPosts(page = 1, size = 20)
                when (result) {
                    is AuthResult.Success -> {
                        val sorted = result.data.posts.sortedByDescending { it.likeCount }
                        _hotPosts.value = if (_hotSortAsc.value) sorted.reversed() else sorted
                        hotHasMore = result.data.posts.size >= 20
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Hot posts load failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Hot posts load error", e)
            } finally {
                _hotLoading.value = false
            }
        }
    }

    fun loadMoreHotPosts() {
        if (_hotLoading.value || !hotHasMore) return
        viewModelScope.launch {
            hotPage++
            try {
                val result = cloudApiClient.listCommunityPosts(page = hotPage, size = 20)
                when (result) {
                    is AuthResult.Success -> {
                        val newSorted = result.data.posts.sortedByDescending { it.likeCount }
                        val merged = _hotPosts.value + if (_hotSortAsc.value) newSorted.reversed() else newSorted
                        _hotPosts.value = merged
                        hotHasMore = result.data.posts.size >= 20
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load more hot posts failed: ${result.message}")
                        hotPage--
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Load more hot posts error", e)
                hotPage--
            }
        }
    }

    /** Phase 1 v2: 导入配方 */
    fun importRecipe(postId: Int, onSuccess: (RecipeImportResult) -> Unit) {
        viewModelScope.launch {
            if (!tokenManager.isLoggedIn()) {
                _message.value = Strings.loginRequired
                return@launch
            }
            try {
                val result = cloudApiClient.importRecipe(postId)
                when (result) {
                    is AuthResult.Success -> {
                        _message.value = Strings.recipeImportSuccess
                        updatePostCaches(postId) { post ->
                            post.copy(recipeImportCount = post.recipeImportCount + 1)
                        }
                        onSuccess(result.data)
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Recipe import failed: ${result.message}")
                        postFeedFailureReport(
                            title = Strings.recipeImportFailed,
                            stage = Strings.importRecipeStage,
                            summary = Strings.recipeImportFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "post_id: $postId"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Recipe import error", e)
                postFeedFailureReport(
                    title = Strings.recipeImportFailed,
                    stage = Strings.importRecipeStage,
                    summary = Strings.recipeImportExceptionSummary,
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

    fun refreshPosts(force: Boolean = false) {
        _feedRefreshing.value = true
        loadPosts(page = 1, force = force)
    }

    fun togglePostLike(postId: Int) {
        viewModelScope.launch {
            if (!tokenManager.isLoggedIn()) {
                _message.value = Strings.loginRequired
                return@launch
            }
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
                            title = Strings.postLikeFailed,
                            stage = Strings.toggleLikeStage,
                            summary = Strings.postLikeFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "post_id: $postId"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Like exception", e)
                postFeedFailureReport(
                    title = Strings.postLikeFailed,
                    stage = Strings.toggleLikeStage,
                    summary = Strings.postLikeExceptionSummary,
                    throwable = e,
                    extraContext = "post_id: $postId"
                )
            }
        }
    }

    fun sharePost(postId: Int) {
        viewModelScope.launch {
            if (!tokenManager.isLoggedIn()) {
                _message.value = Strings.loginRequired
                return@launch
            }
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
                            title = Strings.postShareFailed,
                            stage = Strings.submitShareStage,
                            summary = Strings.postShareFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "post_id: $postId"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Share exception", e)
                postFeedFailureReport(
                    title = Strings.postShareFailed,
                    stage = Strings.submitShareStage,
                    summary = Strings.postShareExceptionSummary,
                    throwable = e,
                    extraContext = "post_id: $postId"
                )
            }
        }
    }

    fun reportPost(postId: Int, reason: String = "inappropriate") {
        viewModelScope.launch {
            if (!tokenManager.isLoggedIn()) {
                _message.value = Strings.loginRequired
                return@launch
            }
            try {
                val result = cloudApiClient.reportPost(postId, reason)
                when (result) {
                    is AuthResult.Success -> _message.value = Strings.reportSubmitted
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Report post failed: ${result.message}")
                        postFeedFailureReport(
                            title = Strings.postReportFailed,
                            stage = Strings.submitReportStage,
                            summary = Strings.postReportFailedSummary,
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
                    title = Strings.postReportFailed,
                    stage = Strings.submitReportStage,
                    summary = Strings.postReportExceptionSummary,
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
            if (!tokenManager.isLoggedIn()) {
                _message.value = Strings.loginRequired
                return@launch
            }
            try {
                val result = cloudApiClient.deletePost(postId)
                when (result) {
                    is AuthResult.Success -> {
                        removePostFromCaches(postId)
                        _message.value = Strings.postDeleted
                        onSuccess()
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Delete post failed: ${result.message}")
                        postFeedFailureReport(
                            title = Strings.postDeleteFailed,
                            stage = Strings.deletePostStage,
                            summary = Strings.postDeleteFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "post_id: $postId"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Delete post exception", e)
                postFeedFailureReport(
                    title = Strings.postDeleteFailed,
                    stage = Strings.deletePostStage,
                    summary = Strings.postDeleteExceptionSummary,
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
                        _message.value = Strings.postUpdated
                        onSuccess(updated)
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Edit post failed: ${result.message}")
                        postFeedFailureReport(
                            title = Strings.postEditFailed,
                            stage = Strings.editPostStage,
                            summary = Strings.postEditFailedSummary,
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
                    title = Strings.postEditFailed,
                    stage = Strings.editPostStage,
                            summary = Strings.postEditExceptionSummary,
                    throwable = e,
                    extraContext = """
                        post_id: $postId
                        content_length: ${content.length}
                    """.trimIndent()
                )
            }
        }
    }

    fun syncPost(updated: CommunityPostItem) = updatePostCaches(updated.id) { updated }

    fun removePostFromCaches(postId: Int) {
        _postCacheFlows.forEach { flow -> flow.value = flow.value.filterNot { it.id == postId } }
        syncDiscoverPostCaches(postId) { list -> list.filterNot { it.id == postId } }
    }

    private fun postFailureReport(
        domain: FailureReportDomain,
        title: String,
        stage: String,
        summary: String,
        serviceMessage: String? = null,
        throwable: Throwable? = null,
        extraContext: String? = null
    ) {
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
            appendLine(serviceMessage ?: throwable?.message ?: Strings.toolUnknownError)

            throwable?.let {
                appendLine()
                appendLine("stacktrace:")
                appendLine(Log.getStackTraceString(it))
            }

            appendLine()
            appendLine("recent_logs:")
            append(AppLogger.getRecentLogTail())
        }

        _failureReportFlows[domain]?.value = OperationFailureReport(
            title = title,
            summary = summary,
            details = details
        )
    }

    private fun postModuleFailureReport(
        title: String, stage: String, summary: String,
        serviceMessage: String? = null, throwable: Throwable? = null, extraContext: String? = null
    ) = postFailureReport(FailureReportDomain.MODULE, title, stage, summary, serviceMessage, throwable, extraContext)

    private fun postNotificationFailureReport(
        title: String, stage: String, summary: String,
        serviceMessage: String? = null, throwable: Throwable? = null, extraContext: String? = null
    ) = postFailureReport(FailureReportDomain.NOTIFICATION, title, stage, summary, serviceMessage, throwable, extraContext)

    private fun postFeedFailureReport(
        title: String, stage: String, summary: String,
        serviceMessage: String? = null, throwable: Throwable? = null, extraContext: String? = null
    ) = postFailureReport(FailureReportDomain.FEED, title, stage, summary, serviceMessage, throwable, extraContext)

    private fun postProfileFailureReport(
        title: String, stage: String, summary: String,
        serviceMessage: String? = null, throwable: Throwable? = null, extraContext: String? = null
    ) = postFailureReport(FailureReportDomain.PROFILE, title, stage, summary, serviceMessage, throwable, extraContext)

    private val _postCacheFlows: List<MutableStateFlow<List<CommunityPostItem>>>
        get() = listOf(_posts, _followingPosts, _trendingPosts)

    private fun updatePostCaches(
        postId: Int,
        transform: (CommunityPostItem) -> CommunityPostItem
    ) {
        _postCacheFlows.forEach { flow ->
            flow.value = flow.value.map { post -> if (post.id == postId) transform(post) else post }
        }
        syncDiscoverPostCaches(postId) { list -> list.map { post -> if (post.id == postId) transform(post) else post } }
    }

    private fun syncDiscoverPostCaches(
        postId: Int,
        transform: (List<CommunityPostItem>) -> List<CommunityPostItem>
    ) {
        _hotPosts.value = transform(_hotPosts.value).let { posts ->
            if (_hotSortAsc.value) posts.reversed() else posts
        }
    }

    // ═══ 模块详情 (delegated) ═══

    fun loadModuleDetail(moduleId: Int) = moduleStore.loadModuleDetail(moduleId)
    fun loadModuleDetailWithComments(moduleId: Int) = moduleStore.loadModuleDetailWithComments(moduleId)
    fun setCommentSortOrder(order: String) = moduleStore.setCommentSortOrder(order)
    fun toggleModuleLike(moduleId: Int) = moduleStore.toggleModuleLike(moduleId)
    @Deprecated("Use toggleModuleLike() instead")
    fun voteModule(moduleId: Int, voteType: String) = moduleStore.voteModule(moduleId, voteType)
    fun toggleFavorite(moduleId: Int) = moduleStore.toggleFavorite(moduleId)
    fun reportModule(moduleId: Int, reason: String, details: String?) = moduleStore.reportModule(moduleId, reason, details)

    // ═══ 评论 (delegated) ═══

    fun loadComments(moduleId: Int) = moduleStore.loadComments(moduleId)
    fun addComment(moduleId: Int, content: String, parentId: Int? = null, onSuccess: () -> Unit = {}) =
        moduleStore.addComment(moduleId, content, parentId, onSuccess)

    // ═══ 收藏列表 (delegated) ═══

    fun loadFavorites() = moduleStore.loadFavorites()

    // ═══ 市场功能 (delegated) ═══

    fun loadStoreModules(category: String? = null, search: String? = null,
                         sort: String = "downloads", order: String = "desc", page: Int = 1) =
        moduleStore.loadStoreModules(category, search, sort, order, page)
    fun downloadStoreModule(moduleId: Int, onResult: (String) -> Unit, onError: ((String) -> Unit)? = null) =
        moduleStore.downloadStoreModule(moduleId, onResult, onError)
    fun reviewModule(moduleId: Int, rating: Int, comment: String? = null) =
        moduleStore.reviewModule(moduleId, rating, comment)

    // ═══ 用户主页 (delegated) ═══

    fun loadUserProfile(userId: Int) = userStore.loadUserProfile(userId)
    fun loadUserPosts(userId: Int) = userStore.loadUserPosts(userId)
    fun loadUserActivity(userId: Int) = userStore.loadUserActivity(userId)
    fun toggleFollow(userId: Int) = userStore.toggleFollow(userId)
    fun loadUserFollowers(userId: Int) = userStore.loadUserFollowers(userId)
    fun loadUserFollowing(userId: Int) = userStore.loadUserFollowing(userId)

    // ═══ 通知 (delegated) ═══

    fun loadNotifications() = notificationStore.loadNotifications()
    fun loadUnreadCount() = notificationStore.loadUnreadCount()
    fun markNotificationRead(notificationId: Int, onSuccess: () -> Unit = {}) = notificationStore.markNotificationRead(notificationId, onSuccess)
    fun markAllNotificationsRead() = notificationStore.markAllNotificationsRead()

    // ═══ 动态 Feed ═══

    fun loadFeed() = loadPosts(page = 1)

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
                            title = Strings.followingFeedLoadFailed,
                            stage = Strings.loadFollowingFeedStage,
                            summary = Strings.followingFeedFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "page: $page"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load following feed", e)
                postFeedFailureReport(
                    title = Strings.followingFeedLoadFailed,
                    stage = Strings.loadFollowingFeedStage,
                    summary = Strings.followingFeedExceptionSummary,
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
                            title = Strings.trendingFeedLoadFailed,
                            stage = Strings.loadTrendingFeedStage,
                            summary = Strings.trendingFeedFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "page: $page"
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load trending feed", e)
                postFeedFailureReport(
                    title = Strings.trendingFeedLoadFailed,
                    stage = Strings.loadTrendingFeedStage,
                    summary = Strings.trendingFeedExceptionSummary,
                    throwable = e,
                    extraContext = "page: $page"
                )
            } finally {
                _trendingLoading.value = false
            }
        }
    }

    // ─── 粉丝/关注列表 (delegated) ───

    val followersList: StateFlow<List<CommunityUserProfile>> get() = userStore.followersList
    val followingList: StateFlow<List<CommunityUserProfile>> get() = userStore.followingList

    // ─── 通用防抖搜索器 ───

    @OptIn(FlowPreview::class)
    private class DebounceSearcher<T>(
        scope: CoroutineScope,
        debounceMs: Long,
        private val doSearch: suspend (String) -> List<T>,
        private val onError: (String, Throwable) -> Unit = { _, _ -> }
    ) {
        private val _query = MutableStateFlow("")
        val results = MutableStateFlow<List<T>>(emptyList())
        val loading = MutableStateFlow(false)

        init {
            scope.launch {
                _query
                    .debounce(debounceMs)
                    .distinctUntilChanged()
                    .filter { it.isNotBlank() }
                    .collect { q ->
                        loading.value = true
                        try {
                            results.value = doSearch(q)
                        } catch (e: Exception) {
                            onError(q, e)
                        } finally {
                            loading.value = false
                        }
                    }
            }
        }

        fun search(query: String) {
            if (query.isBlank()) { results.value = emptyList(); _query.value = ""; return }
            _query.value = query
        }

        fun clear() { results.value = emptyList(); _query.value = "" }
    }

    private val userSearcher = DebounceSearcher(
        scope = viewModelScope, debounceMs = 300,
        doSearch = { query ->
            when (val r = cloudApiClient.searchUsers(query)) {
                is AuthResult.Success -> r.data
                is AuthResult.Error -> {
                    AppLogger.e(TAG, "Search users failed: ${r.message}")
                    postFeedFailureReport(Strings.userSearchFailed, Strings.searchUsersStage, Strings.userSearchFailedSummary, r.message, extraContext = "query: $query")
                    emptyList()
                }
            }
        },
        onError = { q, e ->
            AppLogger.e(TAG, "Failed to search users", e)
            postFeedFailureReport(Strings.userSearchFailed, Strings.searchUsersStage, Strings.userSearchExceptionSummary, throwable = e, extraContext = "query: $q")
        }
    )
    val searchResults: StateFlow<List<CommunityUserProfile>> get() = userSearcher.results
    val searchLoading: StateFlow<Boolean> get() = userSearcher.loading
    fun searchUsers(query: String) = userSearcher.search(query)

    private val postSearcher = DebounceSearcher(
        scope = viewModelScope, debounceMs = 300,
        doSearch = { query ->
            when (val r = cloudApiClient.listCommunityPosts(search = query)) {
                is AuthResult.Success -> r.data.posts
                is AuthResult.Error -> {
                    AppLogger.e(TAG, "Search posts failed: ${r.message}")
                    postFeedFailureReport(Strings.postSearchFailed, Strings.searchPostsStage, Strings.postSearchFailedSummary, r.message, extraContext = "query: $query")
                    emptyList()
                }
            }
        },
        onError = { q, e ->
            AppLogger.e(TAG, "Failed to search posts", e)
            postFeedFailureReport(Strings.postSearchFailed, Strings.searchPostsStage, Strings.postSearchExceptionSummary, throwable = e, extraContext = "query: $q")
        }
    )
    val postSearchResults: StateFlow<List<CommunityPostItem>> get() = postSearcher.results
    val postSearchLoading: StateFlow<Boolean> get() = postSearcher.loading
    fun searchPosts(query: String) = postSearcher.search(query)

    // ── Search UI state (survives sheet close/reopen) ──
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _searchActiveTab = MutableStateFlow("users")
    val searchActiveTab: StateFlow<String> = _searchActiveTab.asStateFlow()
    private val _searchPostType = MutableStateFlow<String?>(null)
    val searchPostType: StateFlow<String?> = _searchPostType.asStateFlow()
    private val _searchSortBy = MutableStateFlow("relevance")
    val searchSortBy: StateFlow<String> = _searchSortBy.asStateFlow()

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateSearchActiveTab(tab: String) { _searchActiveTab.value = tab }
    fun updateSearchPostType(type: String?) { _searchPostType.value = type }
    fun updateSearchSortBy(sort: String) { _searchSortBy.value = sort }

    fun clearSearchState() {
        _searchQuery.value = ""
        _searchActiveTab.value = "users"
        _searchPostType.value = null
        _searchSortBy.value = "relevance"
        userSearcher.results.value = emptyList()
        postSearcher.results.value = emptyList()
    }

    private val mentionSearcher = DebounceSearcher(
        scope = viewModelScope, debounceMs = 200,
        doSearch = { query ->
            when (val r = cloudApiClient.searchUsers(query, size = 6)) {
                is AuthResult.Success -> r.data
                is AuthResult.Error -> {
                    AppLogger.e(TAG, "Search mentions failed: ${r.message}")
                    postFeedFailureReport(Strings.mentionSearchFailed, Strings.searchMentionsStage, Strings.mentionSearchFailedSummary, r.message, extraContext = "query: $query")
                    emptyList()
                }
            }
        },
        onError = { q, e ->
            AppLogger.e(TAG, "Failed to search mentions", e)
            postFeedFailureReport(Strings.mentionSearchFailed, Strings.searchMentionsStage, Strings.mentionSearchExceptionSummary, throwable = e, extraContext = "query: $q")
        }
    )
    val mentionResults: StateFlow<List<CommunityUserProfile>> get() = mentionSearcher.results
    val mentionLoading: StateFlow<Boolean> get() = mentionSearcher.loading
    fun searchMentions(query: String) = mentionSearcher.search(query)
    fun clearMentionResults() = mentionSearcher.clear()

    // ─── 高级帖子搜索（支持 postType + sort 过滤） ───

    fun searchPostsFiltered(query: String, postType: String? = null, sortBy: String = "relevance") {
        if (query.isBlank()) {
            postSearcher.results.value = emptyList()
            return
        }
        viewModelScope.launch {
            postSearcher.loading.value = true
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
                        postSearcher.results.value = posts
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Search posts with filters failed: ${result.message}")
                        postFeedFailureReport(
                            title = Strings.postSearchFailed,
                            stage = Strings.searchPostsStage,
                            summary = Strings.postSearchFilteredFailedSummary,
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
                    title = Strings.postSearchFailed,
                    stage = Strings.searchPostsStage,
                    summary = Strings.postSearchFilteredExceptionSummary,
                    throwable = e,
                    extraContext = """
                        query: $query
                        post_type: ${postType ?: "<all>"}
                        sort_by: $sortBy
                    """.trimIndent()
                )
            } finally {
                postSearcher.loading.value = false
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
                            title = Strings.trendingModulesLoadFailed,
                            stage = Strings.loadTrendingModulesStage,
                            summary = Strings.trendingModulesFailedSummary,
                            serviceMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load trending modules", e)
                postFeedFailureReport(
                    title = Strings.trendingModulesLoadFailed,
                    stage = Strings.loadTrendingModulesStage,
                    summary = Strings.trendingModulesExceptionSummary,
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
                            title = Strings.featuredModulesLoadFailed,
                            stage = Strings.loadFeaturedModulesStage,
                            summary = Strings.featuredModulesFailedSummary,
                            serviceMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load featured modules", e)
                postFeedFailureReport(
                    title = Strings.featuredModulesLoadFailed,
                    stage = Strings.loadFeaturedModulesStage,
                    summary = Strings.featuredModulesExceptionSummary,
                    throwable = e
                )
            }
        }
    }
}
