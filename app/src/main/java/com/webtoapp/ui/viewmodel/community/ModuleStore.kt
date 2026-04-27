package com.webtoapp.ui.viewmodel.community

import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.AppReviewItem
import com.webtoapp.core.cloud.LikeResponse
import com.webtoapp.core.cloud.ModuleComment
import com.webtoapp.core.cloud.ModuleItem
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModuleStore(
    private val scope: CoroutineScope,
    private val api: CloudApiClient,
    private val postFailureReport: (FailureReportParams) -> Unit,
    private val onMessage: (String) -> Unit
) {
    companion object {
        private const val TAG = "ModuleStore"
    }

    private val _moduleDetail = MutableStateFlow<ModuleItem?>(null)
    val moduleDetail: StateFlow<ModuleItem?> = _moduleDetail.asStateFlow()

    private val _moduleDetailLoading = MutableStateFlow(false)
    val moduleDetailLoading: StateFlow<Boolean> = _moduleDetailLoading.asStateFlow()

    private val _comments = MutableStateFlow<List<ModuleComment>>(emptyList())
    val comments: StateFlow<List<ModuleComment>> = _comments.asStateFlow()

    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading.asStateFlow()

    private val _ratingDistribution = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val ratingDistribution: StateFlow<Map<Int, Int>> = _ratingDistribution.asStateFlow()

    private val _reviewItems = MutableStateFlow<List<AppReviewItem>>(emptyList())
    val reviewItems: StateFlow<List<AppReviewItem>> = _reviewItems.asStateFlow()

    private val _commentSortOrder = MutableStateFlow("newest")
    val commentSortOrder: StateFlow<String> = _commentSortOrder.asStateFlow()

    private val _favorites = MutableStateFlow<List<ModuleItem>>(emptyList())
    val favorites: StateFlow<List<ModuleItem>> = _favorites.asStateFlow()

    private val _favoritesLoading = MutableStateFlow(false)
    val favoritesLoading: StateFlow<Boolean> = _favoritesLoading.asStateFlow()

    // ─── 市场列表状态（统一后新增） ───

    private val _storeModules = MutableStateFlow<List<ModuleItem>>(emptyList())
    val storeModules: StateFlow<List<ModuleItem>> = _storeModules.asStateFlow()

    private val _storeLoading = MutableStateFlow(false)
    val storeLoading: StateFlow<Boolean> = _storeLoading.asStateFlow()

    private val _storeTotal = MutableStateFlow(0)
    val storeTotal: StateFlow<Int> = _storeTotal.asStateFlow()

    fun loadModuleDetailWithComments(moduleId: Int) {
        val d1 = scope.async { loadModuleDetail(moduleId) }
        val d2 = scope.async { loadComments(moduleId) }
        val d3 = scope.async { loadRatingDistribution(moduleId) }
        scope.launch { d1.await(); d2.await(); d3.await() }
    }

    fun loadRatingDistribution(moduleId: Int) {
        scope.launch {
            try {
                val result = api.getModuleReviews(moduleId, page = 1, size = 100)
                result.onSuccess { response ->
                    _ratingDistribution.value = response.ratingDistribution
                    _reviewItems.value = response.reviews
                }
            } catch (_: Exception) {}
        }
    }

    fun setCommentSortOrder(order: String) {
        _commentSortOrder.value = order
        // Re-sort comments
        val current = _comments.value
        _comments.value = when (order) {
            "oldest" -> current.sortedBy { it.createdAt }
            "rating_high" -> {
                // Merge review ratings into sort if available
                val reviewMap = _reviewItems.value.associateBy { it.authorId }
                current.sortedByDescending { reviewMap[it.userId]?.rating ?: 0 }
            }
            else -> current.sortedByDescending { it.createdAt } // newest
        }
    }

    fun loadModuleDetail(moduleId: Int) {
        scope.launch {
            _moduleDetailLoading.value = true
            _moduleDetail.value = null
            try {
                val result = api.getStoreModuleById(moduleId)
                when (result) {
                    is AuthResult.Success -> _moduleDetail.value = result.data
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load module detail failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.moduleDetailLoadFailed,
                            stage = Strings.loadModuleDetailStage,
                            summary = Strings.moduleDetailFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "module_id: $moduleId"
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load module detail", e)
                postFailureReport(FailureReportParams(
                    title = Strings.moduleDetailLoadFailed,
                    stage = Strings.loadModuleDetailStage,
                    summary = Strings.moduleDetailExceptionSummary,
                    throwable = e,
                    extraContext = "module_id: $moduleId"
                ))
            } finally {
                _moduleDetailLoading.value = false
            }
        }
    }

    /** 点赞/取消点赞（统一入口，替代旧 voteModule） */
    fun toggleModuleLike(moduleId: Int) {
        scope.launch {
            try {
                val result = api.toggleModuleLike(moduleId)
                when (result) {
                    is AuthResult.Success -> {
                        onMessage(if (result.data.liked) Strings.liked else Strings.unliked)
                        // 乐观更新详情中的点赞状态
                        _moduleDetail.value?.let { current ->
                            _moduleDetail.value = current.copy(
                                isLiked = result.data.liked,
                                likeCount = result.data.likeCount
                            )
                        }
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Toggle module like failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.moduleVoteFailed,
                            stage = Strings.submitVoteStage,
                            summary = Strings.moduleVoteFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "module_id: $moduleId"
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Toggle module like exception", e)
                postFailureReport(FailureReportParams(
                    title = Strings.moduleVoteFailed,
                    stage = Strings.submitVoteStage,
                    summary = Strings.moduleVoteExceptionSummary,
                    throwable = e,
                    extraContext = "module_id: $moduleId"
                ))
            }
        }
    }

    /** @deprecated Use toggleModuleLike() instead */
    fun voteModule(moduleId: Int, voteType: String) = toggleModuleLike(moduleId)

    fun toggleFavorite(moduleId: Int) {
        scope.launch {
            try {
                val current = _moduleDetail.value
                val result = if (current?.isFavorited == true) {
                    api.removeFavorite(moduleId)
                } else {
                    api.addFavorite(moduleId)
                }
                when (result) {
                    is AuthResult.Success -> {
                        onMessage(if (current?.isFavorited == true) Strings.unfavorited else Strings.favorited)
                        loadModuleDetail(moduleId)
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Toggle favorite failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.moduleFavoriteFailed,
                            stage = Strings.toggleFavoriteStage,
                            summary = Strings.moduleFavoriteFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "module_id: $moduleId\ncurrently_favorited: ${current?.isFavorited == true}"
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Toggle favorite exception", e)
                postFailureReport(FailureReportParams(
                    title = Strings.moduleFavoriteFailed,
                    stage = Strings.toggleFavoriteStage,
                    summary = Strings.moduleFavoriteExceptionSummary,
                    throwable = e,
                    extraContext = "module_id: $moduleId\ncurrently_favorited: ${_moduleDetail.value?.isFavorited == true}"
                ))
            }
        }
    }

    fun reportModule(moduleId: Int, reason: String, details: String?) {
        scope.launch {
            try {
                val result = api.reportModule(moduleId, reason, details)
                when (result) {
                    is AuthResult.Success -> onMessage(Strings.reportSubmitted)
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Report module failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.moduleReportFailed,
                            stage = Strings.submitModuleReportStage,
                            summary = Strings.moduleReportFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "module_id: $moduleId\nreason: $reason\ndetails: ${details ?: "<empty>"}"
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Report module exception", e)
                postFailureReport(FailureReportParams(
                    title = Strings.moduleReportFailed,
                    stage = Strings.submitModuleReportStage,
                    summary = Strings.moduleReportExceptionSummary,
                    throwable = e,
                    extraContext = "module_id: $moduleId\nreason: $reason\ndetails: ${details ?: "<empty>"}"
                ))
            }
        }
    }

    fun loadComments(moduleId: Int) {
        scope.launch {
            _commentsLoading.value = true
            try {
                val result = api.listComments(moduleId)
                when (result) {
                    is AuthResult.Success -> _comments.value = result.data
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load comments failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.commentLoadFailed,
                            stage = Strings.loadCommentsStage,
                            summary = Strings.commentLoadFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "module_id: $moduleId"
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load comments", e)
                postFailureReport(FailureReportParams(
                    title = Strings.commentLoadFailed,
                    stage = Strings.loadCommentsStage,
                    summary = Strings.commentLoadExceptionSummary,
                    throwable = e,
                    extraContext = "module_id: $moduleId"
                ))
            } finally {
                _commentsLoading.value = false
            }
        }
    }

    fun addComment(moduleId: Int, content: String, parentId: Int? = null, onSuccess: () -> Unit = {}) {
        scope.launch {
            try {
                val result = api.addComment(moduleId, content, parentId)
                when (result) {
                    is AuthResult.Success -> {
                        onMessage(Strings.commentPublished)
                        onSuccess()
                        loadComments(moduleId)
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Add comment failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.commentSubmitFailed,
                            stage = Strings.submitCommentStage,
                            summary = Strings.commentSubmitFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "module_id: $moduleId\nparent_id: ${parentId ?: "null"}\ncontent: $content"
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Add comment exception", e)
                postFailureReport(FailureReportParams(
                    title = Strings.commentSubmitFailed,
                    stage = Strings.submitCommentStage,
                    summary = Strings.commentSubmitExceptionSummary,
                    throwable = e,
                    extraContext = "module_id: $moduleId\nparent_id: ${parentId ?: "null"}\ncontent: $content"
                ))
            }
        }
    }

    fun loadFavorites() {
        scope.launch {
            _favoritesLoading.value = true
            try {
                val result = api.listFavorites()
                when (result) {
                    is AuthResult.Success -> {
                        val (modules, _) = result.data
                        _favorites.value = modules
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load favorites failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.favoritesLoadFailed,
                            stage = Strings.loadFavoritesStage,
                            summary = Strings.favoritesLoadFailedSummary,
                            serviceMessage = result.message
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load favorites", e)
                postFailureReport(FailureReportParams(
                    title = Strings.favoritesLoadFailed,
                    stage = Strings.loadFavoritesStage,
                    summary = Strings.favoritesLoadExceptionSummary,
                    throwable = e
                ))
            } finally {
                _favoritesLoading.value = false
            }
        }
    }
    // ═══════════════════════════════════════════
    // 市场功能（统一后从 CloudViewModel 迁入）
    // ═══════════════════════════════════════════

    /** 加载市场模块列表 */
    fun loadStoreModules(category: String? = null, search: String? = null,
                         sort: String = "downloads", order: String = "desc", page: Int = 1) {
        scope.launch {
            _storeLoading.value = true
            try {
                val result = api.listStoreModules(category, search, sort, order, page)
                when (result) {
                    is AuthResult.Success -> {
                        _storeModules.value = result.data.first
                        _storeTotal.value = result.data.second
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load store modules failed: ${result.message}")
                        onMessage(result.message)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Load store modules exception", e)
                onMessage("Network error: ${e.message}")
            } finally {
                _storeLoading.value = false
            }
        }
    }

    /** 下载模块 */
    fun downloadStoreModule(moduleId: Int, onResult: (String) -> Unit, onError: ((String) -> Unit)? = null) {
        scope.launch {
            try {
                val result = api.downloadStoreModule(moduleId)
                when (result) {
                    is AuthResult.Success -> onResult(result.data)
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Download module failed: ${result.message}")
                        onError?.invoke(result.message) ?: onMessage(result.message)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Download module exception", e)
                onError?.invoke("Network error: ${e.message}") ?: onMessage("Network error: ${e.message}")
            }
        }
    }

    /** 星级评价（市场特有） */
    fun reviewModule(moduleId: Int, rating: Int, comment: String? = null) {
        scope.launch {
            try {
                val result = api.reviewStoreModule(moduleId, rating, comment)
                when (result) {
                    is AuthResult.Success -> {
                        onMessage(Strings.reviewSubmitted)
                        loadModuleDetail(moduleId)
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Review module failed: ${result.message}")
                        onMessage(result.message)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Review module exception", e)
                onMessage("Network error: ${e.message}")
            }
        }
    }
}
