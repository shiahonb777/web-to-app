package com.webtoapp.core.cloud

data class EcosystemFeedResponse(
    val items: List<EcosystemItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 20
)

data class EcosystemItem(
    val type: String,
    val id: Int,
    val title: String,
    val summary: String = "",
    val content: String = "",
    val category: String = "other",
    val tags: List<String> = emptyList(),
    val icon: String? = null,
    val media: List<EcosystemMedia> = emptyList(),
    val author: EcosystemAuthor = EcosystemAuthor(),
    val stats: EcosystemStats = EcosystemStats(),
    val viewerState: EcosystemViewerState = EcosystemViewerState(),
    val meta: Map<String, String?> = emptyMap(),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class EcosystemAuthor(
    val id: Int = 0,
    val username: String = "?",
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val isDeveloper: Boolean = false
)

data class EcosystemMedia(
    val id: Int = 0,
    val type: String = "image",
    val url: String? = null,
    val thumbnailUrl: String? = null
)

data class EcosystemStats(
    val views: Int = 0,
    val likes: Int = 0,
    val comments: Int = 0,
    val downloads: Int = 0,
    val rating: Float = 0f,
    val ratingCount: Int = 0
)

data class EcosystemViewerState(
    val liked: Boolean = false,
    val bookmarked: Boolean = false,
    val own: Boolean = false
)

data class EcosystemComment(
    val id: Int,
    val content: String,
    val parentId: Int? = null,
    val likeCount: Int = 0,
    val author: EcosystemAuthor = EcosystemAuthor(),
    val createdAt: String? = null,
    val replies: List<EcosystemComment> = emptyList()
)

data class EcosystemCommentsResponse(
    val comments: List<EcosystemComment> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 30
)

data class EcosystemNotificationsResponse(
    val notifications: List<EcosystemNotification> = emptyList(),
    val unreadCount: Int = 0,
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 20
)

data class EcosystemNotification(
    val id: Int,
    val type: String,
    val title: String? = null,
    val content: String? = null,
    val refType: String? = null,
    val refId: Int? = null,
    val actor: EcosystemAuthor = EcosystemAuthor(),
    val isRead: Boolean = false,
    val createdAt: String? = null
)

data class EcosystemToggleResult(
    val active: Boolean,
    val count: Int = 0
)

data class EcosystemProfile(
    val id: Int = 0,
    val username: String = "?",
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0
)

data class EcosystemUserContent(
    val profile: EcosystemProfile = EcosystemProfile(),
    val posts: List<EcosystemItem> = emptyList(),
    val apps: List<EcosystemItem> = emptyList(),
    val modules: List<EcosystemItem> = emptyList()
)
