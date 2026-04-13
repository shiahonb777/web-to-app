package com.webtoapp.core.cloud.model

data class RedeemResult(val message: String, val planType: String, val daysAdded: Int)

data class RedeemPreview(
    val currentTier: String,
    val currentPlan: String,
    val currentExpiresAt: String?,
    val currentIsLifetime: Boolean,
    val newTier: String,
    val newPlan: String,
    val newExpiresAt: String?,
    val newIsLifetime: Boolean,
    val isUpgrade: Boolean,
    val codeTier: String,
    val codePlanType: String,
    val durationDays: Int,
)

data class MyAppsResponse(
    val apps: List<AppStoreItem>,
    val count: Int,
    val quota: Int,
    val tier: String,
)

data class ActivationRecord(
    val id: Int,
    val type: String,
    val planType: String,
    val proStart: String?,
    val proEnd: String?,
    val note: String?,
    val createdAt: String?,
)

data class DeviceInfo(
    val id: Int,
    val deviceId: String,
    val deviceName: String,
    val deviceOs: String,
    val appVersion: String?,
    val ipAddress: String?,
    val country: String?,
    val lastActiveAt: String?,
    val isCurrent: Boolean = false,
)

data class AnnouncementData(
    val id: Int,
    val title: String,
    val content: String,
    val type: String,
    val actionUrl: String?,
    val actionText: String?,
    val priority: Int,
    val imageUrl: String?,
)

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val versionCode: Int,
    val versionName: String,
    val title: String?,
    val changelog: String?,
    val downloadUrl: String,
    val isForceUpdate: Boolean,
    val fileSize: Long?,
)

data class RemoteConfigItem(val key: String, val value: String, val description: String?)

data class CloudProject(
    val id: Int,
    val name: String,
    val description: String?,
    val projectKey: String,
    val packageName: String? = null,
    val githubRepo: String?,
    val giteeRepo: String?,
    val createdAt: String?,
    val isActive: Boolean = true,
    val totalInstalls: Int = 0,
    val totalOpens: Int = 0,
)

data class ProjectVersion(
    val id: Int,
    val versionCode: Int,
    val versionName: String,
    val title: String?,
    val changelog: String?,
    val downloadUrlGithub: String?,
    val downloadUrlGitee: String?,
    val isForceUpdate: Boolean = false,
    val createdAt: String?,
)

data class DirectUploadToken(
    val token: String,
    val expiresAt: String,
    val uploadUrl: String,
    val releaseId: Int,
    val owner: String,
    val repo: String,
    val tag: String,
    val contentType: String = "application/vnd.android.package-archive",
)

data class AnalyticsData(
    val totalInstalls: Int,
    val totalOpens: Int,
    val totalActive: Int,
    val totalCrashes: Int,
    val totalDownloads: Int = 0,
    val totalDevices: Int = 0,
    val avgDailyActive: Float = 0f,
    val dailyStats: List<DailyStat>,
    val countryDistribution: Map<String, Int> = emptyMap(),
    val versionDistribution: Map<String, Int> = emptyMap(),
    val deviceDistribution: Map<String, Int> = emptyMap(),
    val osDistribution: Map<String, Int> = emptyMap(),
)

data class DailyStat(
    val date: String,
    val installs: Int,
    val opens: Int,
    val active: Int,
    val crashes: Int = 0,
    val downloads: Int = 0,
)

data class BackupRecord(
    val id: Int,
    val platform: String,
    val status: String,
    val repoUrl: String?,
    val fileSize: Long,
    val createdAt: String?,
)

data class ManifestSyncResult(
    val success: Boolean,
    val manifestVersion: Int,
    val syncedAt: String?,
    val conflict: Boolean = false,
)

data class ManifestDownloadResult(
    val manifestJson: String?,
    val manifestVersion: Int,
    val syncedAt: String?,
)

data class PushHistoryItem(
    val id: Int,
    val title: String,
    val body: String,
    val targetType: String,
    val sentCount: Int,
    val createdAt: String?,
)

data class ProjectActivationCode(
    val id: Int,
    val code: String,
    val status: String,
    val maxUses: Int,
    val usedCount: Int,
    val deviceId: String?,
    val createdAt: String,
    val usedAt: String?,
)

data class ProjectAnnouncement(
    val id: Int,
    val title: String,
    val content: String,
    val isActive: Boolean,
    val priority: Int,
    val createdAt: String,
)

data class ProjectConfig(
    val id: Int,
    val key: String,
    val value: String,
    val description: String?,
    val isActive: Boolean,
)

data class ProjectWebhook(
    val id: Int,
    val url: String,
    val events: List<String>,
    val secret: String?,
    val isActive: Boolean,
    val failureCount: Int,
    val lastTriggeredAt: String?,
)

data class StoreModuleInfo(
    val id: Int,
    val name: String,
    val description: String?,
    val icon: String?,
    val category: String?,
    val tags: List<String>,
    val versionName: String?,
    val downloads: Int,
    val rating: Float,
    val ratingCount: Int,
    val isFeatured: Boolean,
    val authorName: String,
    val shareCode: String? = null,
    val createdAt: String?,
    val moduleType: String = "extension",
    val likeCount: Int = 0,
    val isApproved: Boolean = true,
) {
    val downloadCount: Int get() = downloads
    val averageRating: Float get() = rating
}

data class RemoteScriptInfo(
    val id: Int,
    val name: String,
    val description: String?,
    val code: String,
    val runAt: String,
    val urlPattern: String?,
    val priority: Int,
    val isActive: Boolean,
    val version: Int,
    val createdAt: String?,
    val updatedAt: String?,
)

data class CommunityModuleDetail(
    val id: Int,
    val name: String,
    val description: String?,
    val icon: String?,
    val category: String?,
    val tags: List<String>,
    val versionName: String?,
    val downloads: Int,
    val rating: Float,
    val ratingCount: Int,
    val isFeatured: Boolean,
    val authorName: String,
    val authorId: Int,
    val shareCode: String? = null,
    val userVote: String? = null,
    val isFavorited: Boolean = false,
    val createdAt: String?,
    val updatedAt: String?,
)

data class ModuleComment(
    val id: Int,
    val content: String,
    val userId: Int,
    val userName: String,
    val userAvatar: String?,
    val parentId: Int? = null,
    val createdAt: String?,
    val updatedAt: String?,
    val replies: List<ModuleComment> = emptyList(),
)

data class CommunityUserProfile(
    val id: Int,
    val username: String,
    val displayName: String?,
    val avatarUrl: String?,
    val bio: String?,
    val appCount: Int = 0,
    val moduleCount: Int = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false,
    val isDeveloper: Boolean = false,
    val teamBadges: List<TeamBadgeInfo> = emptyList(),
    val createdAt: String? = null,
)

data class TeamBadgeInfo(
    val id: Int,
    val name: String,
    val avatarUrl: String? = null,
    val role: String = "viewer",
)

data class NotificationItem(
    val id: Int,
    val type: String,
    val title: String?,
    val content: String?,
    val refType: String?,
    val refId: Int?,
    val actorId: Int?,
    val isRead: Boolean,
    val createdAt: String?,
)

data class FeedItem(
    val id: Int,
    val type: String,
    val actorName: String,
    val actorAvatar: String?,
    val targetName: String?,
    val targetId: Int?,
    val createdAt: String?,
)

data class AppStoreListResponse(
    val total: Int,
    val page: Int,
    val size: Int,
    val apps: List<AppStoreItem>,
    val categories: List<String> = emptyList(),
)

data class AppStoreItem(
    val id: Int,
    val name: String,
    val icon: String? = null,
    val category: String = "other",
    val tags: List<String> = emptyList(),
    val versionName: String = "1.0",
    val packageName: String? = null,
    val downloads: Int = 0,
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val likeCount: Int = 0,
    val isFeatured: Boolean = false,
    val screenshots: List<String> = emptyList(),
    val authorName: String = "Unknown",
    val authorId: Int = 0,
    val createdAt: String? = null,
    val description: String? = null,
    val videoUrl: String? = null,
    val apkUrlGithub: String? = null,
    val apkUrlGitee: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val groupChatUrl: String? = null,
    val paymentQrUrl: String? = null,
    val websiteUrl: String? = null,
    val privacyPolicyUrl: String? = null,
)

data class LikeResponse(
    val liked: Boolean,
    val likeCount: Int,
)

data class AppReviewItem(
    val id: Int,
    val rating: Int,
    val comment: String? = null,
    val authorName: String = "Unknown",
    val authorId: Int = 0,
    val deviceModel: String? = null,
    val ipAddress: String? = null,
    val createdAt: String? = null,
)

data class AppReviewsResponse(
    val total: Int,
    val page: Int,
    val reviews: List<AppReviewItem>,
)

data class PushHistoryResponse(
    val total: Int,
    val page: Int,
    val dailyUsed: Int,
    val dailyLimit: Int,
    val tier: String,
    val records: List<PushHistoryItem>,
)

data class TeamListResponse(
    val teams: List<TeamItem>,
    val quotaUsed: Int = 0,
    val quotaLimit: Int = 0,
    val memberLimit: Int = 0,
    val tier: String = "free",
)

data class TeamItem(
    val id: Int,
    val name: String,
    val description: String? = null,
    val ownerName: String = "?",
    val ownerId: Int = 0,
    val memberCount: Int = 0,
    val pendingRequests: Int = 0,
    val isPublic: Boolean = true,
    val createdAt: String? = null,
)

data class TeamMemberItem(
    val id: Int,
    val userId: Int,
    val username: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val role: String = "viewer",
    val contribution: Int = 0,
    val createdAt: String? = null,
)

data class TeamSearchResponse(
    val teams: List<TeamSearchItem>,
    val total: Int = 0,
    val page: Int = 1,
)

data class TeamSearchItem(
    val id: Int,
    val name: String,
    val description: String? = null,
    val ownerName: String = "?",
    val ownerId: Int = 0,
    val memberCount: Int = 0,
    val isMember: Boolean = false,
    val hasPendingRequest: Boolean = false,
    val createdAt: String? = null,
)

data class TeamJoinRequestItem(
    val id: Int,
    val userId: Int,
    val username: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val message: String? = null,
    val status: String = "pending",
    val createdAt: String? = null,
)

data class TeamRankingItem(
    val rank: Int,
    val memberId: Int,
    val userId: Int,
    val username: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val role: String = "viewer",
    val contribution: Int = 0,
)

data class BackupListResponse(
    val backups: List<BackupItem>,
    val count: Int = 0,
    val quota: Int = 0,
    val tier: String = "free",
)

data class BackupItem(
    val filename: String,
    val size: String = "0KB",
    val sizeBytes: Long = 0,
    val downloadUrl: String? = null,
    val createdAt: String? = null,
)

data class BackupCreateResult(
    val filename: String,
    val size: Int = 0,
    val downloadUrl: String? = null,
    val projectCount: Int = 0,
    val moduleCount: Int = 0,
    val message: String = "",
)

data class ContributorInput(
    val userId: Int,
    val contributorRole: String = "member",
    val contributionPoints: Int = 0,
    val description: String? = null,
)

data class ModuleTeamInfo(
    val teamId: Int,
    val teamName: String? = null,
    val teamDescription: String? = null,
    val contributors: List<TeamContributorItem> = emptyList(),
)

data class TeamContributorItem(
    val userId: Int,
    val username: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val contributorRole: String = "member",
    val contributionPoints: Int = 0,
    val description: String? = null,
)

data class UserTeamWorksResponse(
    val works: List<TeamWorkItem>,
    val total: Int = 0,
)

data class TeamWorkItem(
    val id: Int,
    val name: String,
    val moduleType: String = "app",
    val icon: String? = null,
    val downloads: Int = 0,
    val rating: Float = 0f,
    val authorName: String = "?",
    val contributorRole: String = "member",
    val contributionPoints: Int = 0,
    val contributionDescription: String? = null,
    val teamId: Int? = null,
    val teamName: String? = null,
)

data class CommunityFeedResponse(val posts: List<CommunityPostItem>, val total: Int)

data class CommunityPostItem(
    val id: Int,
    val content: String,
    val createdAt: String? = null,
    val likeCount: Int = 0,
    val shareCount: Int = 0,
    val commentCount: Int = 0,
    val viewCount: Int = 0,
    val isLiked: Boolean = false,
    val isOwnPost: Boolean = false,
    val authorIsFollowing: Boolean = false,
    val authorId: Int = 0,
    val authorUsername: String = "?",
    val authorDisplayName: String? = null,
    val authorAvatarUrl: String? = null,
    val authorIsDeveloper: Boolean = false,
    val authorTeamBadges: List<TeamBadgeInfo> = emptyList(),
    val tags: List<String> = emptyList(),
    val media: List<PostMediaItem> = emptyList(),
    val appLinks: List<PostAppLinkItem> = emptyList(),
    val postType: String = "discussion",
    val appName: String? = null,
    val appIconUrl: String? = null,
    val sourceType: String? = null,
    val hasRecipe: Boolean = false,
    val recipeImportCount: Int = 0,
    val title: String? = null,
    val difficulty: String? = null,
    val isResolved: Boolean? = null,
)

data class PostMediaInput(
    val mediaType: String = "image",
    val urlGithub: String? = null,
    val urlGitee: String? = null,
    val thumbnailUrl: String? = null,
)

data class PostMediaItem(
    val id: Int,
    val mediaType: String = "image",
    val urlGithub: String? = null,
    val urlGitee: String? = null,
    val thumbnailUrl: String? = null,
)

data class PostAppLinkInput(
    val linkType: String = "store",
    val storeModuleId: Int? = null,
    val appName: String? = null,
    val appIcon: String? = null,
    val appDescription: String? = null,
)

data class PostAppLinkItem(
    val id: Int,
    val linkType: String = "store",
    val storeModuleId: Int? = null,
    val appName: String? = null,
    val appIcon: String? = null,
    val appDescription: String? = null,
    val storeModuleDownloads: Int? = null,
    val storeModuleRating: Float? = null,
    val storeModuleType: String? = null,
)

data class PostLikeResult(val liked: Boolean, val likeCount: Int)
data class CommentLikeResult(val liked: Boolean, val likeCount: Int)
data class BookmarkResult(val bookmarked: Boolean)

data class DiscoverResponse(
    val featuredShowcases: List<CommunityPostItem> = emptyList(),
    val trending: List<CommunityPostItem> = emptyList(),
    val latestTutorials: List<CommunityPostItem> = emptyList(),
    val unansweredQuestions: List<CommunityPostItem> = emptyList(),
)

data class RecipeImportResult(
    val recipe: String,
    val appName: String? = null,
    val appIconUrl: String? = null,
    val sourceType: String? = null,
    val authorId: Int = 0,
    val authorUsername: String? = null,
)

data class CommunityTagsResponse(
    val categories: Map<String, List<String>> = emptyMap(),
    val all: List<String> = emptyList(),
)

data class PostCommentItem(
    val id: Int,
    val content: String,
    val createdAt: String? = null,
    val likeCount: Int = 0,
    val parentId: Int? = null,
    val authorId: Int = 0,
    val authorUsername: String = "?",
    val authorDisplayName: String? = null,
    val authorAvatarUrl: String? = null,
    val authorIsDeveloper: Boolean = false,
    val authorTeamBadges: List<TeamBadgeInfo> = emptyList(),
    val replies: List<PostCommentItem> = emptyList(),
)

data class CommentsResponse(
    val total: Int = 0,
    val page: Int = 1,
    val comments: List<PostCommentItem> = emptyList(),
)

data class UserActivityInfo(
    val isOnline: Boolean = false,
    val lastSeenAt: String? = null,
    val todaySeconds: Int = 0,
    val monthSeconds: Int = 0,
    val yearSeconds: Int = 0,
)

data class ActivationCode(
    val id: Int = 0,
    val code: String,
    val appId: Int = 0,
    val isUsed: Boolean = false,
    val usedByDeviceId: String? = null,
    val usedByUserId: String? = null,
    val usedAt: String? = null,
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val maxUses: Int = 1,
    val currentUses: Int = 0,
)

data class ActivationSettings(
    val enabled: Boolean = false,
    val deviceBindingEnabled: Boolean = false,
    val maxDevicesPerCode: Int = 1,
    val codes: List<ActivationCode> = emptyList(),
    val totalCodes: Int = 0,
    val usedCodes: Int = 0,
)

data class Announcement(
    val id: Int = 0,
    val appId: Int = 0,
    val title: String = "",
    val content: String = "",
    val type: String = "info",
    val isActive: Boolean = true,
    val isPinned: Boolean = false,
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val viewCount: Int = 0,
)

data class AnnouncementTemplate(
    val id: String,
    val name: String,
    val icon: String,
    val title: String,
    val content: String,
    val type: String,
)

data class UpdateConfig(
    val id: Int = 0,
    val appId: Int = 0,
    val latestVersionName: String = "",
    val latestVersionCode: Int = 0,
    val updateTitle: String = "",
    val updateContent: String = "",
    val apkUrl: String? = null,
    val sourceAppId: Int? = null,
    val sourceAppName: String? = null,
    val isForceUpdate: Boolean = false,
    val minVersionCode: Int = 0,
    val templateId: String = "simple",
    val isActive: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class UpdateTemplate(
    val id: String,
    val name: String,
    val preview: String,
    val style: String,
)

data class AppUser(
    val id: String,
    val deviceModel: String? = null,
    val osVersion: String? = null,
    val appVersion: String? = null,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val ipAddress: String? = null,
    val firstSeenAt: String? = null,
    val lastSeenAt: String? = null,
    val activationCode: String? = null,
    val isActive: Boolean = true,
)

data class GeoDistribution(
    val country: String,
    val countryCode: String,
    val count: Int,
    val percentage: Float,
    val regions: List<RegionInfo> = emptyList(),
)

data class RegionInfo(
    val region: String,
    val count: Int,
    val percentage: Float,
)
