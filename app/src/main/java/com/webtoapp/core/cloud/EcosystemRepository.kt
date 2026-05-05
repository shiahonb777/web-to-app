package com.webtoapp.core.cloud

import com.webtoapp.core.auth.AuthResult

class EcosystemRepository(private val api: CloudApiClient) {
    suspend fun feed(type: String, sort: String, page: Int, size: Int, search: String?) =
        api.listEcosystemFeed(type = type, sort = sort, page = page, size = size, search = search)

    suspend fun item(type: String, id: Int) = api.getEcosystemItem(type, id)

    suspend fun user(userId: Int) = api.getEcosystemUser(userId)

    suspend fun createPost(
        content: String,
        title: String?,
        tags: List<String>,
        media: List<EcosystemMedia> = emptyList()
    ) = api.createEcosystemPost(content = content, title = title, tags = tags, media = media)

    suspend fun comments(type: String, id: Int, page: Int, size: Int) =
        api.listEcosystemComments(type = type, id = id, page = page, size = size)

    suspend fun bookmarks(type: String, page: Int, size: Int) =
        api.listEcosystemBookmarks(type = type, page = page, size = size)

    suspend fun notifications(page: Int, size: Int, unreadOnly: Boolean) =
        api.listEcosystemNotifications(page = page, size = size, unreadOnly = unreadOnly)

    suspend fun markNotificationRead(notificationId: Int) =
        api.markEcosystemNotificationRead(notificationId)

    suspend fun markAllNotificationsRead() =
        api.markAllEcosystemNotificationsRead()

    suspend fun addComment(type: String, id: Int, content: String, parentId: Int? = null) =
        api.addEcosystemComment(type = type, id = id, content = content, parentId = parentId)

    suspend fun toggleLike(type: String, id: Int) = api.toggleEcosystemLike(type, id)

    suspend fun toggleBookmark(type: String, id: Int) = api.toggleEcosystemBookmark(type, id)

    suspend fun report(type: String, id: Int, reason: String, description: String?) =
        api.reportEcosystemItem(type = type, id = id, reason = reason, description = description)

    suspend fun resolveAppDownloadUrl(appId: Int): AuthResult<String> =
        api.downloadStoreApp(appId).fold(
            onSuccess = { urls ->
                val url = urls["apk_url_github"] ?: urls["apk_url_gitee"]
                if (url.isNullOrBlank()) {
                    AuthResult.Error("应用下载地址为空")
                } else {
                    val resolvedUrl = GitHubAccelerator.accelerate(url) ?: url
                    AuthResult.Success(resolvedUrl)
                }
            },
            onFailure = { error ->
                AuthResult.Error(error.message ?: "获取应用下载地址失败")
            }
        )

    suspend fun downloadModuleShareCode(moduleId: Int): AuthResult<String> =
        api.downloadStoreModule(moduleId)
}
