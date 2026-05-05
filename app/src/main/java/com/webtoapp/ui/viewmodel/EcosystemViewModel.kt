package com.webtoapp.ui.viewmodel

import android.net.Uri
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.EcosystemComment
import com.webtoapp.core.cloud.EcosystemItem
import com.webtoapp.core.cloud.EcosystemMedia
import com.webtoapp.core.cloud.EcosystemNotification
import com.webtoapp.core.cloud.EcosystemRepository
import com.webtoapp.core.cloud.EcosystemUserContent
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EcosystemUploadItemState(
    val uri: Uri,
    val progress: Float = 0f,
    val uploadedUrl: String? = null,
    val error: String? = null,
    val uploading: Boolean = false
)

data class EcosystemPostComposerState(
    val uploads: List<EcosystemUploadItemState> = emptyList(),
    val uploadMessage: String? = null,
    val uploading: Boolean = false
)

data class EcosystemUiState(
    val items: List<EcosystemItem> = emptyList(),
    val selectedType: String = "all",
    val sort: String = "latest",
    val search: String = "",
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val page: Int = 1,
    val hasMore: Boolean = true,
    val message: String? = null,
    val error: String? = null
)

data class EcosystemDetailState(
    val item: EcosystemItem? = null,
    val comments: List<EcosystemComment> = emptyList(),
    val loading: Boolean = false,
    val commentLoading: Boolean = false,
    val actionLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

data class EcosystemProfileState(
    val content: EcosystemUserContent? = null,
    val loading: Boolean = false,
    val error: String? = null
)

data class EcosystemBookmarksState(
    val items: List<EcosystemItem> = emptyList(),
    val selectedType: String = "all",
    val loading: Boolean = false,
    val error: String? = null
)

data class EcosystemNotificationsState(
    val notifications: List<EcosystemNotification> = emptyList(),
    val unreadCount: Int = 0,
    val loading: Boolean = false,
    val error: String? = null
)

class EcosystemViewModel(
    private val repository: EcosystemRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(EcosystemUiState())
    val uiState: StateFlow<EcosystemUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow(EcosystemDetailState())
    val detailState: StateFlow<EcosystemDetailState> = _detailState.asStateFlow()

    private val _profileState = MutableStateFlow(EcosystemProfileState())
    val profileState: StateFlow<EcosystemProfileState> = _profileState.asStateFlow()

    private val _bookmarksState = MutableStateFlow(EcosystemBookmarksState())
    val bookmarksState: StateFlow<EcosystemBookmarksState> = _bookmarksState.asStateFlow()

    private val _notificationsState = MutableStateFlow(EcosystemNotificationsState())
    val notificationsState: StateFlow<EcosystemNotificationsState> = _notificationsState.asStateFlow()

    private val _composerState = MutableStateFlow(EcosystemPostComposerState())
    val composerState: StateFlow<EcosystemPostComposerState> = _composerState.asStateFlow()

    fun refresh() {
        loadFeed(page = 1, append = false)
    }

    fun setType(type: String) {
        if (_uiState.value.selectedType == type) return
        _uiState.value = _uiState.value.copy(selectedType = type, page = 1, hasMore = true)
        refresh()
    }

    fun setSort(sort: String) {
        if (_uiState.value.sort == sort) return
        _uiState.value = _uiState.value.copy(sort = sort, page = 1, hasMore = true)
        refresh()
    }

    fun setSearch(search: String) {
        _uiState.value = _uiState.value.copy(search = search, page = 1, hasMore = true)
        refresh()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.loading || state.loadingMore || !state.hasMore) return
        loadFeed(page = state.page + 1, append = true)
    }

    private fun loadFeed(page: Int, append: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loading = !append,
                loadingMore = append,
                error = null,
                message = null
            )
            when (val result = repository.feed(
                type = _uiState.value.selectedType,
                sort = _uiState.value.sort,
                page = page,
                size = 20,
                search = _uiState.value.search.takeIf { it.isNotBlank() }
            )) {
                is AuthResult.Success -> {
                    val nextItems = if (append) _uiState.value.items + result.data.items else result.data.items
                    _uiState.value = _uiState.value.copy(
                        items = nextItems,
                        loading = false,
                        loadingMore = false,
                        page = page,
                        hasMore = result.data.items.size >= 20,
                        error = null
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        loadingMore = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun loadDetail(type: String, id: Int) {
        viewModelScope.launch {
            _detailState.value = EcosystemDetailState(loading = true)
            val itemResult = repository.item(type, id)
            val commentResult = repository.comments(type, id, page = 1, size = 30)
            val item = (itemResult as? AuthResult.Success)?.data
            val comments = (commentResult as? AuthResult.Success)?.data?.comments.orEmpty()
            _detailState.value = EcosystemDetailState(
                item = item,
                comments = comments,
                loading = false,
                error = (itemResult as? AuthResult.Error)?.message
                    ?: (commentResult as? AuthResult.Error)?.message
            )
        }
    }

    fun createPost(
        content: String,
        title: String?,
        tags: List<String>,
        media: List<EcosystemMedia> = emptyList(),
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val result = repository.createPost(content, title, tags, media)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        items = listOf(result.data) + _uiState.value.items,
                        message = "已发布"
                    )
                    clearComposerState()
                    onDone(true)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(message = result.message)
                    onDone(false)
                }
            }
        }
    }

    suspend fun uploadPostImages(
        context: Context,
        uris: List<Uri>,
        uploadAsset: suspend (File, String, (Float) -> Unit) -> AuthResult<String>
    ): AuthResult<List<EcosystemMedia>> {
        initializeComposerUploads(uris)
        return retryFailedUploads(context, uploadAsset)
    }

    suspend fun retryFailedUploads(
        context: Context,
        uploadAsset: suspend (File, String, (Float) -> Unit) -> AuthResult<String>
    ): AuthResult<List<EcosystemMedia>> {
        val currentUploads = _composerState.value.uploads
        if (currentUploads.isEmpty()) return AuthResult.Success(emptyList())

        _composerState.value = _composerState.value.copy(uploading = true, uploadMessage = null)
        val nextUploads = currentUploads.toMutableList()

        for (index in nextUploads.indices) {
            val upload = nextUploads[index]
            if (upload.uploadedUrl != null) continue

            nextUploads[index] = upload.copy(uploading = true, error = null, progress = 0f)
            _composerState.value = _composerState.value.copy(uploads = nextUploads.toList(), uploading = true)

            val tempFile = uriToTempFile(context, upload.uri)
            if (tempFile == null) {
                nextUploads[index] = upload.copy(uploading = false, error = "无法读取图片")
                _composerState.value = _composerState.value.copy(
                    uploads = nextUploads.toList(),
                    uploading = false,
                    uploadMessage = "部分图片上传失败"
                )
                return AuthResult.Error("无法读取图片")
            }

            when (val result = uploadAsset(tempFile, "image/png") { progress ->
                nextUploads[index] = nextUploads[index].copy(progress = progress, uploading = true, error = null)
                _composerState.value = _composerState.value.copy(uploads = nextUploads.toList(), uploading = true)
            }) {
                is AuthResult.Success -> {
                    nextUploads[index] = nextUploads[index].copy(
                        progress = 1f,
                        uploadedUrl = result.data,
                        uploading = false,
                        error = null
                    )
                    _composerState.value = _composerState.value.copy(uploads = nextUploads.toList(), uploading = true)
                }
                is AuthResult.Error -> {
                    nextUploads[index] = nextUploads[index].copy(
                        uploading = false,
                        error = result.message
                    )
                    _composerState.value = _composerState.value.copy(
                        uploads = nextUploads.toList(),
                        uploading = false,
                        uploadMessage = result.message
                    )
                    return result
                }
            }
        }

        val media = nextUploads.mapNotNull { upload ->
            upload.uploadedUrl?.let { url ->
                EcosystemMedia(type = "image", url = url, thumbnailUrl = url)
            }
        }
        _composerState.value = _composerState.value.copy(
            uploads = nextUploads.toList(),
            uploading = false,
            uploadMessage = null
        )
        return AuthResult.Success(media)
    }

    fun initializeComposerUploads(uris: List<Uri>) {
        _composerState.value = EcosystemPostComposerState(
            uploads = uris.map { EcosystemUploadItemState(uri = it) }
        )
    }

    fun removeComposerUpload(uri: Uri) {
        _composerState.value = _composerState.value.copy(
            uploads = _composerState.value.uploads.filterNot { it.uri == uri }
        )
    }

    fun clearComposerState() {
        _composerState.value = EcosystemPostComposerState()
    }

    private fun uriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "ecosystem_post_${System.currentTimeMillis()}_${uri.hashCode()}.png")
            tempFile.outputStream().use { out -> input.copyTo(out) }
            input.close()
            tempFile
        } catch (_: Exception) {
            null
        }
    }

    fun addComment(type: String, id: Int, content: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(commentLoading = true, message = null)
            when (val result = repository.addComment(type, id, content)) {
                is AuthResult.Success -> {
                    _detailState.value = _detailState.value.copy(
                        comments = _detailState.value.comments + result.data,
                        commentLoading = false,
                        message = "评论已发布"
                    )
                }
                is AuthResult.Error -> _detailState.value = _detailState.value.copy(
                    commentLoading = false,
                    message = result.message
                )
            }
        }
    }

    fun loadBookmarks(type: String = _bookmarksState.value.selectedType) {
        viewModelScope.launch {
            _bookmarksState.value = _bookmarksState.value.copy(selectedType = type, loading = true, error = null)
            when (val result = repository.bookmarks(type = type, page = 1, size = 100)) {
                is AuthResult.Success -> _bookmarksState.value = _bookmarksState.value.copy(
                    items = result.data.items,
                    loading = false
                )
                is AuthResult.Error -> _bookmarksState.value = _bookmarksState.value.copy(
                    loading = false,
                    error = result.message
                )
            }
        }
    }

    fun setBookmarksType(type: String) {
        if (_bookmarksState.value.selectedType == type) return
        loadBookmarks(type)
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _notificationsState.value = _notificationsState.value.copy(loading = true, error = null)
            when (val result = repository.notifications(page = 1, size = 50, unreadOnly = false)) {
                is AuthResult.Success -> _notificationsState.value = _notificationsState.value.copy(
                    notifications = result.data.notifications,
                    unreadCount = result.data.unreadCount,
                    loading = false
                )
                is AuthResult.Error -> _notificationsState.value = _notificationsState.value.copy(
                    loading = false,
                    error = result.message
                )
            }
        }
    }

    fun markNotificationRead(notificationId: Int, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            when (val result = repository.markNotificationRead(notificationId)) {
                is AuthResult.Success -> {
                    val wasUnread = _notificationsState.value.notifications.any { it.id == notificationId && !it.isRead }
                    _notificationsState.value = _notificationsState.value.copy(
                        notifications = _notificationsState.value.notifications.map {
                            if (it.id == notificationId) it.copy(isRead = true) else it
                        },
                        unreadCount = if (wasUnread) (_notificationsState.value.unreadCount - 1).coerceAtLeast(0) else _notificationsState.value.unreadCount
                    )
                    onDone()
                }
                is AuthResult.Error -> _notificationsState.value = _notificationsState.value.copy(error = result.message)
            }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            when (val result = repository.markAllNotificationsRead()) {
                is AuthResult.Success -> _notificationsState.value = _notificationsState.value.copy(
                    notifications = _notificationsState.value.notifications.map { it.copy(isRead = true) },
                    unreadCount = 0
                )
                is AuthResult.Error -> _notificationsState.value = _notificationsState.value.copy(error = result.message)
            }
        }
    }

    fun toggleLike(type: String, id: Int) {
        viewModelScope.launch {
            val result = repository.toggleLike(type, id)
            if (result is AuthResult.Success) {
                updateItem(type, id) { item ->
                    item.copy(
                        stats = item.stats.copy(likes = result.data.count),
                        viewerState = item.viewerState.copy(liked = result.data.active)
                    )
                }
            } else if (result is AuthResult.Error) {
                _uiState.value = _uiState.value.copy(message = result.message)
                _detailState.value = _detailState.value.copy(message = result.message)
            }
        }
    }

    fun toggleBookmark(type: String, id: Int) {
        viewModelScope.launch {
            val result = repository.toggleBookmark(type, id)
            if (result is AuthResult.Success) {
                updateItem(type, id) { item -> item.copy(viewerState = item.viewerState.copy(bookmarked = result.data.active)) }
                _bookmarksState.value = _bookmarksState.value.copy(
                    items = _bookmarksState.value.items.filterNot { it.type == type && it.id == id }
                )
            } else if (result is AuthResult.Error) {
                _uiState.value = _uiState.value.copy(message = result.message)
                _detailState.value = _detailState.value.copy(message = result.message)
            }
        }
    }

    fun downloadApp(item: EcosystemItem, onDownloadUrl: suspend (String) -> Unit) {
        if (item.type != "app") return
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(actionLoading = true, message = null, error = null)
            when (val result = repository.resolveAppDownloadUrl(item.id)) {
                is AuthResult.Success -> {
                    onDownloadUrl(result.data)
                    _detailState.value = _detailState.value.copy(
                        actionLoading = false,
                        message = "已开始下载"
                    )
                }
                is AuthResult.Error -> _detailState.value = _detailState.value.copy(
                    actionLoading = false,
                    error = result.message
                )
            }
        }
    }

    fun installModule(item: EcosystemItem, onShareCode: suspend (String) -> Result<Unit>) {
        if (item.type != "module") return
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(actionLoading = true, message = null, error = null)
            when (val result = repository.downloadModuleShareCode(item.id)) {
                is AuthResult.Success -> {
                    val importResult = onShareCode(result.data)
                    _detailState.value = if (importResult.isSuccess) {
                        _detailState.value.copy(actionLoading = false, message = "模块已安装")
                    } else {
                        _detailState.value.copy(
                            actionLoading = false,
                            error = importResult.exceptionOrNull()?.message ?: "模块安装失败"
                        )
                    }
                }
                is AuthResult.Error -> _detailState.value = _detailState.value.copy(
                    actionLoading = false,
                    error = result.message
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(message = null)
        _detailState.value = _detailState.value.copy(message = null, error = null)
    }

    fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    fun loadUser(userId: Int) {
        viewModelScope.launch {
            _profileState.value = EcosystemProfileState(loading = true)
            when (val result = repository.user(userId)) {
                is AuthResult.Success -> _profileState.value = EcosystemProfileState(content = result.data)
                is AuthResult.Error -> _profileState.value = EcosystemProfileState(error = result.message)
            }
        }
    }

    fun refreshProfile() {
        val current = _profileState.value.content?.profile?.id ?: return
        loadUser(current)
    }

    fun refreshCurrentDetail() {
        val current = _detailState.value.item ?: return
        loadDetail(current.type, current.id)
    }

    fun refreshNotifications() {
        loadNotifications()
    }

    fun refreshBookmarks() {
        loadBookmarks()
    }

    private fun updateItem(type: String, id: Int, transform: (EcosystemItem) -> EcosystemItem) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map { if (it.type == type && it.id == id) transform(it) else it }
        )
        _detailState.value.item?.let { item ->
            if (item.type == type && item.id == id) {
                _detailState.value = _detailState.value.copy(item = transform(item))
            }
        }
    }
}
