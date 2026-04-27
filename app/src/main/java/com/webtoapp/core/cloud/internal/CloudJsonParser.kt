package com.webtoapp.core.cloud.internal

import com.google.gson.JsonObject
import com.webtoapp.core.cloud.model.AppStoreItem
import com.webtoapp.core.cloud.model.CloudProject
import com.webtoapp.core.cloud.model.CommunityPostItem
import com.webtoapp.core.cloud.model.CommunityUserProfile
import com.webtoapp.core.cloud.model.ModuleTeamInfo
import com.webtoapp.core.cloud.model.PostAppLinkItem
import com.webtoapp.core.cloud.model.PostCommentItem
import com.webtoapp.core.cloud.model.PostMediaItem
import com.webtoapp.core.cloud.model.ProjectActivationCode
import com.webtoapp.core.cloud.model.ProjectAnnouncement
import com.webtoapp.core.cloud.model.ProjectWebhook
import com.webtoapp.core.cloud.model.RemoteScriptInfo
import com.webtoapp.core.cloud.model.StoreModuleInfo
import com.webtoapp.core.cloud.model.TeamBadgeInfo
import com.webtoapp.core.cloud.model.TeamContributorItem

internal class CloudJsonParser {
    fun parseProject(obj: JsonObject): CloudProject = CloudProject(
        id = obj.get("id")?.asInt ?: 0,
        name = obj.get("project_name")?.asString ?: obj.get("name")?.asString ?: "",
        description = obj.get("description")?.asString,
        projectKey = obj.get("project_key")?.asString ?: "",
        packageName = obj.get("package_name")?.asString,
        githubRepo = obj.get("github_repo")?.asString,
        giteeRepo = obj.get("gitee_repo")?.asString,
        createdAt = obj.get("created_at")?.asString,
        isActive = obj.get("is_active")?.asBoolean ?: true,
        totalInstalls = obj.get("total_installs")?.asInt ?: 0,
        totalOpens = obj.get("total_opens")?.asInt ?: 0,
    )

    fun parseActivationCode(obj: JsonObject): ProjectActivationCode = ProjectActivationCode(
        id = obj.get("id")?.asInt ?: 0,
        code = obj.get("code")?.asString ?: "",
        status = obj.get("status")?.asString ?: "unused",
        maxUses = obj.get("max_uses")?.asInt ?: 1,
        usedCount = obj.get("used_count")?.asInt ?: 0,
        deviceId = obj.get("device_id")?.asString,
        createdAt = obj.get("created_at")?.asString ?: "",
        usedAt = obj.get("used_at")?.asString,
    )

    fun parseProjectAnnouncement(obj: JsonObject?): ProjectAnnouncement = ProjectAnnouncement(
        id = obj?.get("id")?.asInt ?: 0,
        title = obj?.get("title")?.asString ?: "",
        content = obj?.get("content")?.asString ?: "",
        isActive = obj?.get("is_active")?.asBoolean ?: true,
        priority = obj?.get("priority")?.asInt ?: 0,
        createdAt = obj?.get("created_at")?.asString ?: "",
    )

    fun parseWebhook(obj: JsonObject?): ProjectWebhook = ProjectWebhook(
        id = obj?.get("id")?.asInt ?: 0,
        url = obj?.get("url")?.asString ?: "",
        events = obj?.getAsJsonArray("events")?.map { it.asString } ?: emptyList(),
        secret = obj?.get("secret")?.asString,
        isActive = obj?.get("is_active")?.asBoolean ?: true,
        failureCount = obj?.get("failure_count")?.asInt ?: 0,
        lastTriggeredAt = obj?.get("last_triggered_at")?.asString,
    )

    fun parseStoreModule(obj: JsonObject?): StoreModuleInfo = StoreModuleInfo(
        id = obj?.get("id")?.asInt ?: 0,
        name = obj?.get("name")?.let { if (it.isJsonNull) "" else it.asString } ?: "",
        description = obj?.get("description")?.let { if (it.isJsonNull) null else it.asString },
        icon = obj?.get("icon")?.let { if (it.isJsonNull) null else it.asString },
        category = obj?.get("category")?.let { if (it.isJsonNull) null else it.asString },
        tags = try {
            obj?.getAsJsonArray("tags")?.map { it.asString } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        },
        versionName = obj?.get("version_name")?.let { if (it.isJsonNull) null else it.asString },
        downloads = obj?.get("downloads")?.asInt ?: 0,
        rating = obj?.get("rating")?.asFloat ?: 0f,
        ratingCount = obj?.get("rating_count")?.asInt ?: 0,
        isFeatured = obj?.get("is_featured")?.asBoolean ?: false,
        authorName = obj?.get("author_name")?.let { if (it.isJsonNull) "" else it.asString }
            ?: obj?.getAsJsonObject("author")?.get("username")?.let { if (it.isJsonNull) "" else it.asString }
            ?: "",
        shareCode = obj?.get("share_code")?.let { if (it.isJsonNull) null else it.asString },
        createdAt = obj?.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
        moduleType = obj?.get("module_type")?.let { if (it.isJsonNull) "extension" else it.asString } ?: "extension",
        likeCount = obj?.get("like_count")?.asInt ?: 0,
        isApproved = obj?.get("is_approved")?.asBoolean ?: true,
    )

    fun parseRemoteScript(obj: JsonObject?): RemoteScriptInfo = RemoteScriptInfo(
        id = obj?.get("id")?.asInt ?: 0,
        name = obj?.get("name")?.asString ?: "",
        description = obj?.get("description")?.asString,
        code = obj?.get("code")?.asString ?: "",
        runAt = obj?.get("run_at")?.asString ?: "document_end",
        urlPattern = obj?.get("url_pattern")?.asString,
        priority = obj?.get("priority")?.asInt ?: 0,
        isActive = obj?.get("is_active")?.asBoolean ?: true,
        version = obj?.get("version")?.asInt ?: 1,
        createdAt = obj?.get("created_at")?.asString,
        updatedAt = obj?.get("updated_at")?.asString,
    )

    fun parseStoreApp(obj: JsonObject): AppStoreItem = AppStoreItem(
        id = obj.get("id")?.asInt ?: 0,
        name = obj.get("name")?.asString ?: "",
        icon = obj.get("icon")?.let { if (it.isJsonNull) null else it.asString },
        category = obj.get("category")?.asString ?: "other",
        tags = obj.getAsJsonArray("tags")?.map { it.asString } ?: emptyList(),
        versionName = obj.get("version_name")?.asString ?: "1.0",
        packageName = obj.get("package_name")?.let { if (it.isJsonNull) null else it.asString },
        downloads = obj.get("downloads")?.asInt ?: 0,
        rating = obj.get("rating")?.asFloat ?: 0f,
        ratingCount = obj.get("rating_count")?.asInt ?: 0,
        likeCount = obj.get("like_count")?.asInt ?: 0,
        isFeatured = obj.get("is_featured")?.asBoolean ?: false,
        screenshots = obj.getAsJsonArray("screenshots")?.map { it.asString } ?: emptyList(),
        authorName = obj.get("author_name")?.asString ?: "Unknown",
        authorId = obj.get("author_id")?.asInt ?: 0,
        createdAt = obj.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
        description = obj.get("description")?.let { if (it.isJsonNull) null else it.asString },
        videoUrl = obj.get("video_url")?.let { if (it.isJsonNull) null else it.asString },
        apkUrlGithub = obj.get("apk_url_github")?.let { if (it.isJsonNull) null else it.asString },
        apkUrlGitee = obj.get("apk_url_gitee")?.let { if (it.isJsonNull) null else it.asString },
        contactEmail = obj.get("contact_email")?.let { if (it.isJsonNull) null else it.asString },
        contactPhone = obj.get("contact_phone")?.let { if (it.isJsonNull) null else it.asString },
        groupChatUrl = obj.get("group_chat_url")?.let { if (it.isJsonNull) null else it.asString },
        paymentQrUrl = obj.get("payment_qr_url")?.let { if (it.isJsonNull) null else it.asString },
        websiteUrl = obj.get("website_url")?.let { if (it.isJsonNull) null else it.asString },
        privacyPolicyUrl = obj.get("privacy_policy_url")?.let { if (it.isJsonNull) null else it.asString },
    )

    fun parseCommunityPost(obj: JsonObject): CommunityPostItem {
        val author = obj.getAsJsonObject("author")
        val media = obj.getAsJsonArray("media")?.map { mediaElement ->
            val mediaObject = mediaElement.asJsonObject
            PostMediaItem(
                id = mediaObject.get("id")?.asInt ?: 0,
                mediaType = mediaObject.get("media_type")?.asString ?: "image",
                urlGithub = mediaObject.get("url_github")?.let { if (it.isJsonNull) null else it.asString },
                urlGitee = mediaObject.get("url_gitee")?.let { if (it.isJsonNull) null else it.asString },
                thumbnailUrl = mediaObject.get("thumbnail_url")?.let { if (it.isJsonNull) null else it.asString },
            )
        } ?: emptyList()
        val tags = obj.getAsJsonArray("tags")?.map { it.asString } ?: emptyList()
        val appLinks = obj.getAsJsonArray("app_links")?.map { appLinkElement ->
            val appLinkObject = appLinkElement.asJsonObject
            val storeModule = appLinkObject.getAsJsonObject("store_module")
            PostAppLinkItem(
                id = appLinkObject.get("id")?.asInt ?: 0,
                linkType = appLinkObject.get("link_type")?.asString ?: "store",
                storeModuleId = appLinkObject.get("store_module_id")?.let { if (it.isJsonNull) null else it.asInt },
                appName = appLinkObject.get("app_name")?.let { if (it.isJsonNull) null else it.asString },
                appIcon = appLinkObject.get("app_icon")?.let { if (it.isJsonNull) null else it.asString },
                appDescription = appLinkObject.get("app_description")?.let { if (it.isJsonNull) null else it.asString },
                storeModuleDownloads = storeModule?.get("downloads")?.asInt,
                storeModuleRating = storeModule?.get("rating")?.asFloat,
                storeModuleType = storeModule?.get("module_type")?.asString,
            )
        } ?: emptyList()

        return CommunityPostItem(
            id = obj.get("id")?.asInt ?: 0,
            content = obj.get("content")?.asString ?: "",
            createdAt = obj.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
            likeCount = obj.get("like_count")?.asInt ?: 0,
            shareCount = obj.get("share_count")?.asInt ?: 0,
            commentCount = obj.get("comment_count")?.asInt ?: 0,
            viewCount = obj.get("view_count")?.asInt ?: 0,
            isLiked = obj.get("is_liked")?.asBoolean ?: false,
            isOwnPost = obj.get("is_own_post")?.asBoolean ?: false,
            authorIsFollowing = obj.get("author_is_following")?.asBoolean ?: false,
            authorId = author?.get("id")?.asInt ?: 0,
            authorUsername = author?.get("username")?.asString ?: "?",
            authorDisplayName = author?.get("display_name")?.let { if (it.isJsonNull) null else it.asString },
            authorAvatarUrl = author?.get("avatar_url")?.let { if (it.isJsonNull) null else it.asString },
            authorIsDeveloper = author?.get("is_developer")?.asBoolean ?: false,
            authorTeamBadges = author?.let { parseTeamBadges(it) } ?: emptyList(),
            tags = tags,
            media = media,
            appLinks = appLinks,
            postType = obj.get("post_type")?.asString ?: "discussion",
            appName = obj.get("app_name")?.let { if (it.isJsonNull) null else it.asString },
            appIconUrl = obj.get("app_icon_url")?.let { if (it.isJsonNull) null else it.asString },
            sourceType = obj.get("source_type")?.let { if (it.isJsonNull) null else it.asString },
            hasRecipe = obj.get("has_recipe")?.asBoolean ?: false,
            recipeImportCount = obj.get("recipe_import_count")?.asInt ?: 0,
            title = obj.get("title")?.let { if (it.isJsonNull) null else it.asString },
            difficulty = obj.get("difficulty")?.let { if (it.isJsonNull) null else it.asString },
            isResolved = obj.get("is_resolved")?.let { if (it.isJsonNull) null else it.asBoolean },
        )
    }

    fun parsePostComment(obj: JsonObject): PostCommentItem {
        val author = obj.getAsJsonObject("author")
        val replies = obj.getAsJsonArray("replies")?.map { parsePostComment(it.asJsonObject) } ?: emptyList()
        return PostCommentItem(
            id = obj.get("id")?.asInt ?: 0,
            content = obj.get("content")?.asString ?: "",
            createdAt = obj.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
            likeCount = obj.get("like_count")?.asInt ?: 0,
            parentId = obj.get("parent_id")?.let { if (it.isJsonNull) null else it.asInt },
            authorId = author?.get("id")?.asInt ?: 0,
            authorUsername = author?.get("username")?.asString ?: "?",
            authorDisplayName = author?.get("display_name")?.let { if (it.isJsonNull) null else it.asString },
            authorAvatarUrl = author?.get("avatar_url")?.let { if (it.isJsonNull) null else it.asString },
            authorIsDeveloper = author?.get("is_developer")?.asBoolean ?: false,
            authorTeamBadges = author?.let { parseTeamBadges(it) } ?: emptyList(),
            replies = replies,
        )
    }

    fun parseTeamBadges(authorObj: JsonObject): List<TeamBadgeInfo> {
        val array = authorObj.getAsJsonArray("team_badges") ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element.asJsonObject
            TeamBadgeInfo(
                id = obj.get("id")?.asInt ?: return@mapNotNull null,
                name = obj.get("name")?.asString ?: "?",
                avatarUrl = obj.get("avatar_url")?.let { if (it.isJsonNull) null else it.asString },
                role = obj.get("role")?.asString ?: "viewer",
            )
        }
    }

    fun parseSimpleUserProfile(obj: JsonObject): CommunityUserProfile = CommunityUserProfile(
        id = obj.get("id")?.asInt ?: 0,
        username = obj.get("username")?.asString ?: "",
        displayName = obj.get("display_name")?.let { if (it.isJsonNull) null else it.asString },
        avatarUrl = obj.get("avatar_url")?.let { if (it.isJsonNull) null else it.asString },
        bio = obj.get("bio")?.let { if (it.isJsonNull) null else it.asString },
        appCount = obj.get("published_apps_count")?.asInt ?: 0,
        moduleCount = obj.get("published_modules_count")?.asInt ?: obj.get("module_count")?.asInt ?: 0,
        followerCount = obj.get("follower_count")?.asInt ?: 0,
        followingCount = obj.get("following_count")?.asInt ?: 0,
        isFollowing = obj.get("is_following")?.asBoolean ?: false,
        isDeveloper = obj.get("is_developer")?.asBoolean ?: false,
        teamBadges = parseTeamBadges(obj),
    )

    fun parseModuleTeamInfo(data: JsonObject): ModuleTeamInfo {
        val contributors = data.getAsJsonArray("contributors")?.map { contributorElement ->
            val contributor = contributorElement.asJsonObject
            TeamContributorItem(
                userId = contributor.get("user_id")?.asInt ?: 0,
                username = contributor.get("username")?.asString ?: "?",
                displayName = contributor.get("display_name")?.let { if (it.isJsonNull) null else it.asString },
                avatarUrl = contributor.get("avatar_url")?.let { if (it.isJsonNull) null else it.asString },
                contributorRole = contributor.get("contributor_role")?.asString ?: "member",
                contributionPoints = contributor.get("contribution_points")?.asInt ?: 0,
                description = contributor.get("description")?.let { if (it.isJsonNull) null else it.asString },
            )
        } ?: emptyList()

        return ModuleTeamInfo(
            teamId = data.get("team_id")?.asInt ?: 0,
            teamName = data.get("team_name")?.let { if (it.isJsonNull) null else it.asString },
            teamDescription = data.get("team_description")?.let { if (it.isJsonNull) null else it.asString },
            contributors = contributors,
        )
    }
}
