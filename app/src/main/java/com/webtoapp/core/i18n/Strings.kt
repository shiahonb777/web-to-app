package com.webtoapp.core.i18n

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.webtoapp.R
import com.webtoapp.core.i18n.strings.AiCodingStrings
import com.webtoapp.core.i18n.strings.AiConfigStrings
import com.webtoapp.core.i18n.strings.AiStrings
import com.webtoapp.core.i18n.strings.BillingStrings
import com.webtoapp.core.i18n.strings.BuildStrings
import com.webtoapp.core.i18n.strings.CloudStrings
import com.webtoapp.core.i18n.strings.CommonStrings
import com.webtoapp.core.i18n.strings.CommunityStrings
import com.webtoapp.core.i18n.strings.CompatStrings
import com.webtoapp.core.i18n.strings.CreateStrings
import com.webtoapp.core.i18n.strings.ExtensionStrings
import com.webtoapp.core.i18n.strings.ModuleStrings
import com.webtoapp.core.i18n.strings.MusicStrings
import com.webtoapp.core.i18n.strings.ProjectStrings
import com.webtoapp.core.i18n.strings.SampleStrings
import com.webtoapp.core.i18n.strings.ShellStrings
import com.webtoapp.core.i18n.strings.SnippetStrings
import com.webtoapp.core.i18n.strings.StoreStrings
import com.webtoapp.core.i18n.strings.UiStrings
import com.webtoapp.core.i18n.strings.WebViewStrings

/**
 * Note.
 * Note.
 * 
 * Note.
 */
object Strings {
    
    // Note.
    private val _currentLanguage = mutableStateOf(AppLanguage.CHINESE)
    val currentLanguage: State<AppLanguage> = _currentLanguage

    @Volatile
    private var localizedContext: Context? = null
    
    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }

    fun attachContext(baseContext: Context, language: AppLanguage = lang) {
        localizedContext = try {
            LanguageManager(baseContext.applicationContext).applyLanguage(baseContext, language)
        } catch (_: Exception) {
            baseContext
        }
    }

    private fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        val ctx = localizedContext ?: return ""
        return if (formatArgs.isNotEmpty()) ctx.getString(resId, *formatArgs) else ctx.getString(resId)
    }

    internal fun resourceString(@StringRes resId: Int, vararg formatArgs: Any): String =
        getString(resId, *formatArgs)
    
    // Get.
    private val lang: AppLanguage get() = _currentLanguage.value
    // strings 3 object.
    internal val delegateLanguage: AppLanguage get() = _currentLanguage.value
    
    // ==================== ====================
    val appTitle: String get() = "WebToApp"
    
    val typewriterText1: String get() = "WebToApp"
    val typewriterText2: String get() = when (lang) {
        AppLanguage.CHINESE -> "探索网页与APK的边界"
        AppLanguage.ENGLISH -> "Explore the boundary of Web & APK"
        AppLanguage.ARABIC -> "استكشف حدود الويب و APK"
    }
    val typewriterText3: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码的无限可能"
        AppLanguage.ENGLISH -> "Infinite possibilities of code"
        AppLanguage.ARABIC -> "إمكانيات لا حدود لها للبرمجة"
    }
    
    // ==================== ====================
    val myApps: String get() = when (lang) {
        else -> getString(R.string.title_my_apps)
    }
    
    val createApp: String get() = CreateStrings.createApp
    
    val settings: String get() = when (lang) {
        else -> getString(R.string.title_settings)
    }
    
    val search: String get() = when (lang) {
        else -> getString(R.string.search_placeholder)
    }

    val more: String get() = when (lang) {
        else -> getString(R.string.menu_more)
    }
    
    val back: String get() = when (lang) {
        else -> getString(R.string.webview_back)
    }
    
    // ==================== ====================
    val menuAiCoding: String get() = CommonStrings.menuAiCoding
    
    val menuThemeSettings: String get() = CommonStrings.menuThemeSettings
    
    val menuAiSettings: String get() = CommonStrings.menuAiSettings
    
    val menuAppModifier: String get() = CommonStrings.menuAppModifier
    
    val menuExtensionModules: String get() = CommonStrings.menuExtensionModules
    
    val menuAbout: String get() = CommonStrings.menuAbout

    // ==================== Tab ====================
    val tabHome: String get() = CommonStrings.tabHome
    val tabStore: String get() = CommonStrings.tabStore
    val marketTabApps: String get() = CommonStrings.marketTabApps
    val marketTabModules: String get() = CommonStrings.marketTabModules
    val moduleStoreSearchPlaceholder: String get() = ModuleStrings.moduleStoreSearchPlaceholder
    val moduleStoreEmpty: String get() = ModuleStrings.moduleStoreEmpty
    val moduleStoreEmptySearch: String get() = ModuleStrings.moduleStoreEmptySearch
    val moduleStoreInstall: String get() = ModuleStrings.moduleStoreInstall
    val moduleStoreFeatured: String get() = ModuleStrings.moduleStoreFeatured
    val moduleStoreSortDownloads: String get() = ModuleStrings.moduleStoreSortDownloads
    val moduleStoreSortRating: String get() = ModuleStrings.moduleStoreSortRating
    val moduleStoreSortNewest: String get() = ModuleStrings.moduleStoreSortNewest
    val moduleStoreSortLikes: String get() = ModuleStrings.moduleStoreSortLikes
    val moduleStoreCatAll: String get() = ModuleStrings.moduleStoreCatAll
    val tabProfile: String get() = CommonStrings.tabProfile
    val tabMore: String get() = CommonStrings.tabMore

    // More.
    val moreSectionAiTools: String get() = CommonStrings.moreSectionAiTools
    val moreSectionDevTools: String get() = CommonStrings.moreSectionDevTools
    val moreSectionBrowser: String get() = CommonStrings.moreSectionBrowser
    val moreSectionAppearance: String get() = CommonStrings.moreSectionAppearance
    val storeComingSoon: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用商店即将上线\n发现和分享精彩应用"
        AppLanguage.ENGLISH -> "App Store Coming Soon\nDiscover & Share Amazing Apps"
        AppLanguage.ARABIC -> "المتجر قريباً\nاكتشف وشارك التطبيقات المذهلة"
    }

    // ==================== App Store ====================
    val storeSearchPlaceholder: String get() = StoreStrings.storeSearchPlaceholder
    val storeAllCategories: String get() = StoreStrings.storeAllCategories
    val storeAppsCount: String get() = StoreStrings.storeAppsCount
    val storeEmpty: String get() = StoreStrings.storeEmpty
    val storeLoadMore: String get() = StoreStrings.storeLoadMore
    val storeSortDownloads: String get() = StoreStrings.storeSortDownloads
    val storeSortRating: String get() = StoreStrings.storeSortRating
    val storeSortNewest: String get() = StoreStrings.storeSortNewest
    val storeSortLikes: String get() = StoreStrings.storeSortLikes
    val storeReviews: String get() = StoreStrings.storeReviews
    val storeDownloads: String get() = StoreStrings.storeDownloads
    val storeLikes: String get() = StoreStrings.storeLikes
    val storeDownloadBtn: String get() = StoreStrings.storeDownloadBtn
    val storeScreenshots: String get() = StoreStrings.storeScreenshots
    val storeDescription: String get() = StoreStrings.storeDescription
    val storeDeveloperInfo: String get() = StoreStrings.storeDeveloperInfo
    val storeEmail: String get() = StoreStrings.storeEmail
    val storeWebsite: String get() = StoreStrings.storeWebsite
    val storeGroupChat: String get() = StoreStrings.storeGroupChat
    val storePrivacyPolicy: String get() = StoreStrings.storePrivacyPolicy
    val storePhone: String get() = StoreStrings.storePhone
    val storeReport: String get() = StoreStrings.storeReport
    // Store categories
    val storeCatTools: String get() = StoreStrings.storeCatTools
    val storeCatSocial: String get() = StoreStrings.storeCatSocial
    val storeCatEducation: String get() = StoreStrings.storeCatEducation
    val storeCatEntertainment: String get() = StoreStrings.storeCatEntertainment
    val storeCatProductivity: String get() = StoreStrings.storeCatProductivity
    val storeCatLifestyle: String get() = StoreStrings.storeCatLifestyle
    val storeCatBusiness: String get() = StoreStrings.storeCatBusiness
    val storeCatNews: String get() = StoreStrings.storeCatNews
    val storeCatFinance: String get() = StoreStrings.storeCatFinance
    val storeCatHealth: String get() = StoreStrings.storeCatHealth
    val storeCatOther: String get() = StoreStrings.storeCatOther

    // Note.
    val storeDownloadManager: String get() = StoreStrings.storeDownloadManager
    val storeDownloadedApps: String get() = StoreStrings.storeDownloadedApps
    val storeMyApps: String get() = StoreStrings.storeMyApps
    val storePublishApp: String get() = StoreStrings.storePublishApp
    val storeNoDownloads: String get() = StoreStrings.storeNoDownloads
    val storeNoDownloadedApps: String get() = StoreStrings.storeNoDownloadedApps
    val storeNoPublishedApps: String get() = StoreStrings.storeNoPublishedApps
    val storeConfirmUnpublish: String get() = StoreStrings.storeConfirmUnpublish
    val storeInstall: String get() = StoreStrings.storeInstall
    val storeDelete: String get() = StoreStrings.storeDelete
    val storeCancel: String get() = StoreStrings.storeCancel
    val storePublishing: String get() = StoreStrings.storePublishing
    val storePublishSuccess: String get() = StoreStrings.storePublishSuccess
    val storePublishFailed: String get() = StoreStrings.storePublishFailed
    val storeFetchingLink: String get() = StoreStrings.storeFetchingLink
    val storeLoadFailed: String get() = StoreStrings.storeLoadFailed
    val storeAppName: String get() = StoreStrings.storeAppName
    val storeAppDesc: String get() = StoreStrings.storeAppDesc
    val storeCategory: String get() = StoreStrings.storeCategory
    val storeTags: String get() = StoreStrings.storeTags
    val storeVersionName: String get() = StoreStrings.storeVersionName
    val storeVersionCode: String get() = StoreStrings.storeVersionCode
    val storePackageName: String get() = StoreStrings.storePackageName
    val storeIconUrl: String get() = StoreStrings.storeIconUrl
    val storeScreenshotUrl: String get() = StoreStrings.storeScreenshotUrl
    val storeBasicInfo: String get() = StoreStrings.storeBasicInfo
    val storeDescAndTags: String get() = StoreStrings.storeDescAndTags
    val storeContactInfo: String get() = StoreStrings.storeContactInfo
    val storeApkLinks: String get() = StoreStrings.storeApkLinks
    val storePublishFormSubtitle: String get() = StoreStrings.storePublishFormSubtitle
    val storeScreenshotsAdded: String get() = StoreStrings.storeScreenshotsAdded
    val storeFillRequired: String get() = StoreStrings.storeFillRequired
    val storeAddScreenshot: String get() = StoreStrings.storeAddScreenshot
    // Note.
    val storeMyModules: String get() = StoreStrings.storeMyModules
    val storePublishModule: String get() = StoreStrings.storePublishModule
    val storeNoPublishedModules: String get() = StoreStrings.storeNoPublishedModules
    val storeModuleShareCode: String get() = StoreStrings.storeModuleShareCode
    val storeModuleName: String get() = StoreStrings.storeModuleName
    val storeModuleDesc: String get() = StoreStrings.storeModuleDesc
    val storeModulePublishSubtitle: String get() = StoreStrings.storeModulePublishSubtitle
    val storeModulePublishSuccess: String get() = StoreStrings.storeModulePublishSuccess

    // ==================== Team Collaboration ====================
    val teamTitle: String get() = CommonStrings.teamTitle
    val teamCreate: String get() = CommonStrings.teamCreate
    val teamName: String get() = CommonStrings.teamName
    val teamDesc: String get() = CommonStrings.teamDesc
    val teamMembers: String get() = CommonStrings.teamMembers
    val teamInvite: String get() = CommonStrings.teamInvite
    val teamUsername: String get() = CommonStrings.teamUsername
    val teamRole: String get() = CommonStrings.teamRole
    val teamRoleOwner: String get() = CommonStrings.teamRoleOwner
    val teamRoleAdmin: String get() = CommonStrings.teamRoleAdmin
    val teamRoleEditor: String get() = CommonStrings.teamRoleEditor
    val teamRoleViewer: String get() = CommonStrings.teamRoleViewer
    val teamDelete: String get() = CommonStrings.teamDelete
    val teamEmpty: String get() = CommonStrings.teamEmpty
    val teamQuota: String get() = CommonStrings.teamQuota
    val teamSearch: String get() = CommonStrings.teamSearch
    val teamDiscover: String get() = CommonStrings.teamDiscover
    val teamMyTeams: String get() = CommonStrings.teamMyTeams
    val teamJoin: String get() = CommonStrings.teamJoin
    val teamJoinMessage: String get() = CommonStrings.teamJoinMessage
    val teamJoinPending: String get() = CommonStrings.teamJoinPending
    val teamJoinApprove: String get() = CommonStrings.teamJoinApprove
    val teamJoinReject: String get() = CommonStrings.teamJoinReject
    val teamJoinRequests: String get() = CommonStrings.teamJoinRequests
    val teamJoinNoRequests: String get() = CommonStrings.teamJoinNoRequests
    val teamRanking: String get() = CommonStrings.teamRanking
    val teamContribution: String get() = CommonStrings.teamContribution
    val teamSearchEmpty: String get() = CommonStrings.teamSearchEmpty
    val teamJoinSent: String get() = CommonStrings.teamJoinSent
    val teamJoined: String get() = CommonStrings.teamJoined
    val teamAssociate: String get() = CommonStrings.teamAssociate
    val teamLead: String get() = CommonStrings.teamLead
    val teamMemberRole: String get() = CommonStrings.teamMemberRole
    val teamContributionPoints: String get() = CommonStrings.teamContributionPoints
    val teamContributionDesc: String get() = CommonStrings.teamContributionDesc
    val teamWorks: String get() = CommonStrings.teamWorks
    val teamSelectTeam: String get() = CommonStrings.teamSelectTeam
    val teamAddContributor: String get() = CommonStrings.teamAddContributor
    val teamNoTeams: String get() = CommonStrings.teamNoTeams

    // ==================== ====================
    val tabCommunity: String get() = CommunityStrings.tabCommunity

    val communityCreatePost: String get() = CommunityStrings.communityCreatePost

    val communityWhatsNew: String get() = CommunityStrings.communityWhatsNew

    val communitySelectTags: String get() = CommunityStrings.communitySelectTags

    val communityLinkApp: String get() = CommunityStrings.communityLinkApp

    val communityAddMedia: String get() = CommunityStrings.communityAddMedia

    val communityPublish: String get() = CommunityStrings.communityPublish

    val communityLike: String get() = CommunityStrings.communityLike

    val communityShare: String get() = CommunityStrings.communityShare

    val communityComment: String get() = CommunityStrings.communityComment

    val communityReport: String get() = CommunityStrings.communityReport

    val communityAllTags: String get() = CommunityStrings.communityAllTags

    val communityNoPosts: String get() = CommunityStrings.communityNoPosts

    val communityOnline: String get() = CommunityStrings.communityOnline

    val communityOffline: String get() = CommunityStrings.communityOffline

    val communityTodayOnline: String get() = CommunityStrings.communityTodayOnline

    val communityMonthOnline: String get() = CommunityStrings.communityMonthOnline

    val communityYearOnline: String get() = CommunityStrings.communityYearOnline

    val communityPosts: String get() = CommunityStrings.communityPosts

    val communityActivity: String get() = CommunityStrings.communityActivity

    val badgeDeveloper: String get() = CommunityStrings.badgeDeveloper

    val badgeTeamOwner: String get() = CommunityStrings.badgeTeamOwner

    val badgeTeamAdmin: String get() = CommunityStrings.badgeTeamAdmin

    val badgeTeamMember: String get() = CommunityStrings.badgeTeamMember

    val communityViewApp: String get() = CommunityStrings.communityViewApp

    val communityPostSuccess: String get() = CommunityStrings.communityPostSuccess

    val communityTagRequired: String get() = CommunityStrings.communityTagRequired

    val communityTagMaxLimit: String get() = CommunityStrings.communityTagMaxLimit

    val communityLoginToPost: String get() = CommunityStrings.communityLoginToPost

    // ==================== Translations ====================

    val communityApplication: String get() = CommunityStrings.communityApplication

    val communityPublishFailed: String get() = CommunityStrings.communityPublishFailed

    val communityNoAppsToLink: String get() = CommunityStrings.communityNoAppsToLink

    val communityConfirm: String get() = CommunityStrings.communityConfirm

    val communityNoRepliesYet: String get() = CommunityStrings.communityNoRepliesYet

    val communityBeFirstReply: String get() = CommunityStrings.communityBeFirstReply

    val communityPostNotFound: String get() = CommunityStrings.communityPostNotFound

    val communityLastSeen: String get() = CommunityStrings.communityLastSeen

    val communityJoined: String get() = CommunityStrings.communityJoined

    val communityFollowing: String get() = CommunityStrings.communityFollowing

    val communityFollowers: String get() = CommunityStrings.communityFollowers

    val communityApps: String get() = CommunityStrings.communityApps

    val communityModules: String get() = CommunityStrings.communityModules

    val communityTeamWorks: String get() = CommunityStrings.communityTeamWorks

    val communityNoModulesYet: String get() = CommunityStrings.communityNoModulesYet

    val communityNoModulesHint: String get() = CommunityStrings.communityNoModulesHint

    val communityNoTeamWorksYet: String get() = CommunityStrings.communityNoTeamWorksYet

    val communityNoTeamWorksHint: String get() = CommunityStrings.communityNoTeamWorksHint

    val communityNoActivityData: String get() = CommunityStrings.communityNoActivityData

    val communityFollow: String get() = CommunityStrings.communityFollow

    val communityFeatured: String get() = CommunityStrings.communityFeatured

    val communityLead: String get() = CommunityStrings.communityLead

    val communityMember: String get() = CommunityStrings.communityMember

    val communityPoints: String get() = CommunityStrings.communityPoints

    val communityNotifications: String get() = CommunityStrings.communityNotifications

    val communityTabAll: String get() = CommunityStrings.communityTabAll

    val communityTabActivity: String get() = CommunityStrings.communityTabActivity

    val communityNothingYet: String get() = CommunityStrings.communityNothingYet

    val communityNothingYetHint: String get() = CommunityStrings.communityNothingYetHint

    val communityNoFeedYet: String get() = CommunityStrings.communityNoFeedYet

    val communityNoFeedYetHint: String get() = CommunityStrings.communityNoFeedYetHint

    val communityActionPublished: String get() = CommunityStrings.communityActionPublished

    val communityActionLiked: String get() = CommunityStrings.communityActionLiked

    val communityActionReplied: String get() = CommunityStrings.communityActionReplied

    val communityActionBookmarked: String get() = CommunityStrings.communityActionBookmarked

    val communityActionFollowed: String get() = CommunityStrings.communityActionFollowed

    val communityActionInteracted: String get() = CommunityStrings.communityActionInteracted

    val communityBookmarks: String get() = CommunityStrings.communityBookmarks

    val communitySaveForLater: String get() = CommunityStrings.communitySaveForLater

    val communitySaveForLaterHint: String get() = CommunityStrings.communitySaveForLaterHint

    // formatTimeAgo.
    val timeJustNow: String get() = CommunityStrings.timeJustNow

    val timeMinutesAgo: String get() = CommunityStrings.timeMinutesAgo

    val timeHoursAgo: String get() = CommunityStrings.timeHoursAgo

    val timeDaysAgo: String get() = CommunityStrings.timeDaysAgo

    val timeWeeksAgo: String get() = CommunityStrings.timeWeeksAgo

    val timeMonthsAgo: String get() = CommunityStrings.timeMonthsAgo

    // formatDuration.
    val durationHourMinute: String get() = CommunityStrings.durationHourMinute

    val durationMinute: String get() = CommunityStrings.durationMinute

    val durationLessThanMinute: String get() = CommunityStrings.durationLessThanMinute

    // ==================== ====================
    val communitySearch: String get() = CommunityStrings.communitySearch

    val communitySearchUsers: String get() = CommunityStrings.communitySearchUsers

    val communitySearchHint: String get() = CommunityStrings.communitySearchHint

    val communityNoUsersFound: String get() = CommunityStrings.communityNoUsersFound

    // Note.
    val communitySearchPosts: String get() = CommunityStrings.communitySearchPosts

    val communitySearchPostsHint: String get() = CommunityStrings.communitySearchPostsHint

    val communityNoPostsFound: String get() = CommunityStrings.communityNoPostsFound

    val communityTabUsers: String get() = CommunityStrings.communityTabUsers

    val communityTabPostsSearch: String get() = CommunityStrings.communityTabPostsSearch

    val communitySearchAll: String get() = CommunityStrings.communitySearchAll

    // Note.
    val communityMentionSelectUser: String get() = CommunityStrings.communityMentionSelectUser

    val communityFollowersList: String get() = CommunityStrings.communityFollowersList

    val communityFollowingList: String get() = CommunityStrings.communityFollowingList

    val communityEditProfile: String get() = CommunityStrings.communityEditProfile

    val communityMutualFollow: String get() = CommunityStrings.communityMutualFollow

    val communityNoFollowers: String get() = CommunityStrings.communityNoFollowers

    val communityNoFollowing: String get() = CommunityStrings.communityNoFollowing

    val communityDeletePost: String get() = CommunityStrings.communityDeletePost

    val communityDeleteConfirm: String get() = CommunityStrings.communityDeleteConfirm

    // CLI-11:.
    val communityViews: String get() = CommunityStrings.communityViews

    // CLI-02:.
    val communityEditPost: String get() = CommunityStrings.communityEditPost

    // CLI-01:.
    val communityConfirmDelete: String get() = CommunityStrings.communityConfirmDelete

    val communityDeletePostConfirmMsg: String get() = CommunityStrings.communityDeletePostConfirmMsg

    val communityCancel: String get() = CommunityStrings.communityCancel

    val communitySave: String get() = CommunityStrings.communitySave

    // ==================== ====================
    val communityPost: String get() = CommunityStrings.communityPost

    val communityPostYourReply: String get() = CommunityStrings.communityPostYourReply

    val communityShowMoreReplies: String get() = CommunityStrings.communityShowMoreReplies

    val communityReportTitle: String get() = CommunityStrings.communityReportTitle

    val communityReportWhy: String get() = CommunityStrings.communityReportWhy

    val communityReportSubmit: String get() = CommunityStrings.communityReportSubmit

    val communityReportSpam: String get() = CommunityStrings.communityReportSpam

    val communityReportInappropriate: String get() = CommunityStrings.communityReportInappropriate

    val communityReportMalicious: String get() = CommunityStrings.communityReportMalicious

    val communityReportCopyright: String get() = CommunityStrings.communityReportCopyright

    val communityReportOther: String get() = CommunityStrings.communityReportOther

    val communityDownloads: String get() = CommunityStrings.communityDownloads

    val communityRatings: String get() = CommunityStrings.communityRatings

    // ==================== ====================
    val analyticsOpens: String get() = CloudStrings.analyticsOpens
    val analyticsActiveUsers: String get() = CloudStrings.analyticsActiveUsers
    val analyticsInstalls: String get() = CloudStrings.analyticsInstalls
    val analyticsCrashes: String get() = CloudStrings.analyticsCrashes
    val analyticsDevices: String get() = CloudStrings.analyticsDevices
    val analyticsAvgDaily: String get() = CloudStrings.analyticsAvgDaily
    val analyticsTrend: String get() = CloudStrings.analyticsTrend
    val analyticsOsDistribution: String get() = CloudStrings.analyticsOsDistribution
    val analyticsDeviceDistribution: String get() = CloudStrings.analyticsDeviceDistribution
    val analyticsCountryDistribution: String get() = CloudStrings.analyticsCountryDistribution
    val analyticsVersionDistribution: String get() = CloudStrings.analyticsVersionDistribution
    val analyticsDashboard: String get() = CloudStrings.analyticsDashboard

    // ==================== Tab ====================
    val cloudOverview: String get() = CloudStrings.cloudOverview

    val cloudSync: String get() = CloudStrings.cloudSync

    val cloudScripts: String get() = CloudStrings.cloudScripts

    val cloudShare: String get() = CloudStrings.cloudShare

    val cloudActivationCodes: String get() = CloudStrings.cloudActivationCodes

    val cloudAnnouncements: String get() = CloudStrings.cloudAnnouncements

    val cloudPush: String get() = CloudStrings.cloudPush

    val pushAnnouncement: String get() = CloudStrings.pushAnnouncement

    val pushUpdate: String get() = CloudStrings.pushUpdate

    val pushTitle: String get() = CloudStrings.pushTitle

    val pushBody: String get() = CloudStrings.pushBody

    val pushVersionName: String get() = CloudStrings.pushVersionName

    val pushForceUpdate: String get() = CloudStrings.pushForceUpdate

    val pushOptionalUpdate: String get() = CloudStrings.pushOptionalUpdate

    val pushSendBtn: String get() = CloudStrings.pushSendBtn

    val pushHistory: String get() = CloudStrings.pushHistory

    val pushDailyLimit: String get() = CloudStrings.pushDailyLimit

    val pushSent: String get() = CloudStrings.pushSent

    val pushFailed: String get() = CloudStrings.pushFailed

    val pushEmpty: String get() = CloudStrings.pushEmpty

    val cloudRemoteConfig: String get() = CloudStrings.cloudRemoteConfig

    val cloudVersions: String get() = CloudStrings.cloudVersions

    val cloudBackups: String get() = CloudStrings.cloudBackups

    // ==================== ====================
    val authCloudService: String get() = CloudStrings.authCloudService

    val authCloudDesc: String get() = CloudStrings.authCloudDesc

    val authLogin: String get() = CloudStrings.authLogin

    val authRegister: String get() = CloudStrings.authRegister

    val authEmail: String get() = CloudStrings.authEmail

    val authUsername: String get() = CloudStrings.authUsername

    val authUsernameHint: String get() = CloudStrings.authUsernameHint

    val authPassword: String get() = CloudStrings.authPassword

    val authPasswordHint: String get() = CloudStrings.authPasswordHint

    val authConfirmPassword: String get() = CloudStrings.authConfirmPassword

    val authLoggingIn: String get() = CloudStrings.authLoggingIn

    val authRegistering: String get() = CloudStrings.authRegistering

    val authNoAccount: String get() = CloudStrings.authNoAccount

    val authRegisterNow: String get() = CloudStrings.authRegisterNow

    val authHasAccount: String get() = CloudStrings.authHasAccount

    val authLoginNow: String get() = CloudStrings.authLoginNow

    val authWhyRegister: String get() = CloudStrings.authWhyRegister

    val authFeatureCloud: String get() = CloudStrings.authFeatureCloud

    val authFeatureStats: String get() = CloudStrings.authFeatureStats

    val authFeatureShare: String get() = CloudStrings.authFeatureShare

    val authFeatureBackup: String get() = CloudStrings.authFeatureBackup

    val authFreeNote: String get() = CloudStrings.authFreeNote

    val authProfile: String get() = CloudStrings.authProfile

    val authLogout: String get() = CloudStrings.authLogout

    val authLogoutConfirm: String get() = CloudStrings.authLogoutConfirm

    val authStatsAppsCreated: String get() = CloudStrings.authStatsAppsCreated

    val authStatsApksBuilt: String get() = CloudStrings.authStatsApksBuilt

    val authStatsMaxDevices: String get() = CloudStrings.authStatsMaxDevices

    val authProActive: String get() = CloudStrings.authProActive

    val authUltraActive: String get() = CloudStrings.authUltraActive

    val authLifetimeActive: String get() = CloudStrings.authLifetimeActive

    val authUltraLifetimeActive: String get() = CloudStrings.authUltraLifetimeActive

    val authUpgradeToUltra: String get() = CloudStrings.authUpgradeToUltra

    val authUpgradeDesc: String get() = CloudStrings.authUpgradeDesc

    val authProInactive: String get() = CloudStrings.authProInactive

    val authProRemaining: String get() = CloudStrings.authProRemaining

    val authProDays: String get() = CloudStrings.authProDays

    val authMenuDevices: String get() = CloudStrings.authMenuDevices

    val authMenuDevicesMax: String get() = CloudStrings.authMenuDevicesMax

    val authMenuCloudProjects: String get() = CloudStrings.authMenuCloudProjects

    val authMenuCloudAvailable: String get() = CloudStrings.authMenuCloudAvailable

    val authMenuCloudUpgrade: String get() = CloudStrings.authMenuCloudUpgrade

    val authMenuSecurity: String get() = CloudStrings.authMenuSecurity

    val authMenuSecurityDesc: String get() = CloudStrings.authMenuSecurityDesc

    // ==================== ====================
    val authForgotPassword: String get() = CloudStrings.authForgotPassword

    val authResetPasswordTitle: String get() = CloudStrings.authResetPasswordTitle

    val authResetPasswordDesc: String get() = CloudStrings.authResetPasswordDesc

    val authVerificationCode: String get() = CloudStrings.authVerificationCode

    val authCodePlaceholder: String get() = CloudStrings.authCodePlaceholder

    val authSendCode: String get() = CloudStrings.authSendCode

    val authNewPassword: String get() = CloudStrings.authNewPassword

    val authPasswordMinLength: String get() = CloudStrings.authPasswordMinLength

    val authResetPasswordBtn: String get() = CloudStrings.authResetPasswordBtn

    val authRememberPassword: String get() = CloudStrings.authRememberPassword

    val authBackToLogin: String get() = CloudStrings.authBackToLogin

    val authRegisterSuccess: String get() = CloudStrings.authRegisterSuccess

    val authWelcomeMessage: String get() = CloudStrings.authWelcomeMessage

    val authCloudServiceNotice: String get() = CloudStrings.authCloudServiceNotice

    val authConfirm: String get() = CloudStrings.authConfirm

    val authPasswordResetSuccess: String get() = CloudStrings.authPasswordResetSuccess

    val authUsernameOrEmail: String get() = CloudStrings.authUsernameOrEmail

    val authInputUsernameOrEmail: String get() = CloudStrings.authInputUsernameOrEmail

    val authOr: String get() = CloudStrings.authOr

    val authGoogleLogin: String get() = CloudStrings.authGoogleLogin

    val authLoggingInWithGoogle: String get() = CloudStrings.authLoggingInWithGoogle

    val authRegisterEmail: String get() = CloudStrings.authRegisterEmail

    val menuAccount: String get() = CommonStrings.menuAccount
    val menuLoginRegister: String get() = CommonStrings.menuLoginRegister

    // ==================== ====================
    val cloudActivationCode: String get() = CloudStrings.cloudActivationCode

    val cloudRedeemTitle: String get() = CloudStrings.cloudRedeemTitle

    val cloudRedeemDesc: String get() = CloudStrings.cloudRedeemDesc

    val cloudCodePlaceholder: String get() = CloudStrings.cloudCodePlaceholder

    val cloudRedeemBtn: String get() = CloudStrings.cloudRedeemBtn

    val cloudRedeeming: String get() = CloudStrings.cloudRedeeming

    val cloudRedeemHistory: String get() = CloudStrings.cloudRedeemHistory

    val cloudValidUntil: String get() = CloudStrings.cloudValidUntil
    
    // ── Enhanced Activation Code Screen Strings ──
    
    val characterCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "个字符"
        AppLanguage.ENGLISH -> "characters"
        AppLanguage.ARABIC -> "أحرف"
    }
    
    val cloudTierUpgrade: String get() = CloudStrings.cloudTierUpgrade
    
    val cloudRedeemPreview: String get() = CloudStrings.cloudRedeemPreview
    
    val cloudCurrentPlan: String get() = CloudStrings.cloudCurrentPlan
    
    val cloudAfterRedeem: String get() = CloudStrings.cloudAfterRedeem
    
    val cloudLifetime: String get() = CloudStrings.cloudLifetime
    
    val cloudUpgradeNotice: String get() = CloudStrings.cloudUpgradeNotice
    
    val cloudConfirmRedeem: String get() = CloudStrings.cloudConfirmRedeem
    
    // ── End Enhanced Activation Code Screen Strings ──
    
    val cloudDeviceManagement: String get() = CloudStrings.cloudDeviceManagement

    val cloudDeviceCount: String get() = CloudStrings.cloudDeviceCount

    val cloudNoDevices: String get() = CloudStrings.cloudNoDevices

    val cloudCurrentDevice: String get() = CloudStrings.cloudCurrentDevice

    val cloudLastActive: String get() = CloudStrings.cloudLastActive

    val cloudRemoveDevice: String get() = CloudStrings.cloudRemoveDevice

    val cloudRemoveDeviceConfirm: String get() = CloudStrings.cloudRemoveDeviceConfirm

    val cloudDismiss: String get() = CloudStrings.cloudDismiss

    val cloudProjects: String get() = CloudStrings.cloudProjects

    val cloudCreateProject: String get() = CloudStrings.cloudCreateProject

    val cloudNoProjects: String get() = CloudStrings.cloudNoProjects

    val cloudCreateProjectHint: String get() = CloudStrings.cloudCreateProjectHint

    val cloudProjectName: String get() = CloudStrings.cloudProjectName

    val cloudProjectDesc: String get() = CloudStrings.cloudProjectDesc

    val cloudOptional: String get() = CloudStrings.cloudOptional

    val cloudCreate: String get() = CloudStrings.cloudCreate

    val cloudDeleteProject: String get() = CloudStrings.cloudDeleteProject

    val cloudDeleteProjectConfirm: String get() = CloudStrings.cloudDeleteProjectConfirm

    val cloudAnalytics: String get() = CloudStrings.cloudAnalytics

    val cloudDays: String get() = CloudStrings.cloudDays

    val cloudInstalls: String get() = CloudStrings.cloudInstalls

    val cloudOpens: String get() = CloudStrings.cloudOpens

    val cloudActive: String get() = CloudStrings.cloudActive

    val cloudCrashes: String get() = CloudStrings.cloudCrashes

    val cloudVersionHistory: String get() = CloudStrings.cloudVersionHistory

    val cloudNoVersions: String get() = CloudStrings.cloudNoVersions

    val cloudProBenefits: String get() = CloudStrings.cloudProBenefits

    val cloudBenefitCloud: String get() = CloudStrings.cloudBenefitCloud

    val cloudBenefitPriority: String get() = CloudStrings.cloudBenefitPriority

    val cloudBenefitDevices: String get() = CloudStrings.cloudBenefitDevices

    val cloudBenefitAnalytics: String get() = CloudStrings.cloudBenefitAnalytics

    val cloudLoadingDevices: String get() = CloudStrings.cloudLoadingDevices

    val cloudDevicesOnline: String get() = CloudStrings.cloudDevicesOnline

    val cloudLoadingProjects: String get() = CloudStrings.cloudLoadingProjects

    val cloudProjectsTotal: String get() = CloudStrings.cloudProjectsTotal

    val cloudShowAdvanced: String get() = CloudStrings.cloudShowAdvanced

    val cloudHideAdvanced: String get() = CloudStrings.cloudHideAdvanced

    // ==================== ====================
    val cloudPreviewTierUpgrade: String get() = CloudStrings.cloudPreviewTierUpgrade

    val cloudPreviewTitle: String get() = CloudStrings.cloudPreviewTitle

    val cloudPreviewCodeLabel: String get() = CloudStrings.cloudPreviewCodeLabel

    val cloudPreviewCurrent: String get() = CloudStrings.cloudPreviewCurrent

    val cloudPreviewAfter: String get() = CloudStrings.cloudPreviewAfter

    val cloudPreviewLifetime: String get() = CloudStrings.cloudPreviewLifetime

    val cloudPreviewDays: String get() = CloudStrings.cloudPreviewDays

    val cloudPreviewUpgradeHint: String get() = CloudStrings.cloudPreviewUpgradeHint

    val cloudPreviewConfirm: String get() = CloudStrings.cloudPreviewConfirm

    val cloudPreviewCancel: String get() = CloudStrings.cloudPreviewCancel

    val cloudHistoryEmpty: String get() = CloudStrings.cloudHistoryEmpty

    val cloudCodeFormatError: String get() = CloudStrings.cloudCodeFormatError
    
    // ==================== Usage Stats ====================
    val menuStats: String get() = CommonStrings.menuStats
    val statsTitle: String get() = UiStrings.statsTitle
    val statsTotalLaunches: String get() = UiStrings.statsTotalLaunches
    val statsTotalUsage: String get() = UiStrings.statsTotalUsage
    val statsActiveApps: String get() = UiStrings.statsActiveApps
    val statsMostUsed: String get() = UiStrings.statsMostUsed
    val statsMostTime: String get() = UiStrings.statsMostTime
    val statsRecentlyUsed: String get() = UiStrings.statsRecentlyUsed
    val statsLaunches: String get() = UiStrings.statsLaunches
    val statsNeverUsed: String get() = UiStrings.statsNeverUsed
    val statsNoData: String get() = UiStrings.statsNoData
    val statsJustNow: String get() = UiStrings.statsJustNow
    val statsMinutesAgo: String get() = UiStrings.statsMinutesAgo
    val statsHoursAgo: String get() = UiStrings.statsHoursAgo
    val statsDaysAgo: String get() = UiStrings.statsDaysAgo
    
    // ==================== ====================
    val healthTitle: String get() = CloudStrings.healthTitle
    val healthOnline: String get() = CloudStrings.healthOnline
    val healthSlow: String get() = CloudStrings.healthSlow
    val healthOffline: String get() = CloudStrings.healthOffline
    val healthUnknown: String get() = CloudStrings.healthUnknown
    val healthCheckNow: String get() = CloudStrings.healthCheckNow
    val healthChecking: String get() = CloudStrings.healthChecking
    val healthResponseTime: String get() = CloudStrings.healthResponseTime
    val healthUptime24h: String get() = CloudStrings.healthUptime24h
    val healthHistory: String get() = CloudStrings.healthHistory
    
    // ==================== ====================
    val screenshotCapture: String get() = when (lang) {
        AppLanguage.CHINESE -> "截取预览"
        AppLanguage.ENGLISH -> "Capture Preview"
        AppLanguage.ARABIC -> "التقاط معاينة"
    }
    val screenshotRefresh: String get() = when (lang) {
        AppLanguage.CHINESE -> "刷新截图"
        AppLanguage.ENGLISH -> "Refresh Screenshot"
        AppLanguage.ARABIC -> "تحديث لقطة الشاشة"
    }
    val screenshotCapturing: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在截取..."
        AppLanguage.ENGLISH -> "Capturing..."
        AppLanguage.ARABIC -> "جارٍ الالتقاط..."
    }
    
    // ==================== ====================
    val menuBatchImport: String get() = CommonStrings.menuBatchImport
    val batchImportTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "批量导入"
        AppLanguage.ENGLISH -> "Batch Import"
        AppLanguage.ARABIC -> "استيراد مجمع"
    }
    val batchImportFromText: String get() = when (lang) {
        AppLanguage.CHINESE -> "从文本导入"
        AppLanguage.ENGLISH -> "Import from Text"
        AppLanguage.ARABIC -> "استيراد من نص"
    }
    val batchImportFromBookmarks: String get() = when (lang) {
        AppLanguage.CHINESE -> "从书签文件导入"
        AppLanguage.ENGLISH -> "Import from Bookmarks"
        AppLanguage.ARABIC -> "استيراد من الإشارات المرجعية"
    }
    val batchImportHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "每行一个URL，格式：URL 或 名称|URL"
        AppLanguage.ENGLISH -> "One URL per line: URL or Name|URL"
        AppLanguage.ARABIC -> "رابط واحد لكل سطر: URL أو الاسم|URL"
    }
    val batchImportParsed: String get() = when (lang) {
        AppLanguage.CHINESE -> "已解析 %d 个URL"
        AppLanguage.ENGLISH -> "%d URLs parsed"
        AppLanguage.ARABIC -> "تم تحليل %d روابط"
    }
    val batchImportSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "成功导入 %d 个应用"
        AppLanguage.ENGLISH -> "%d apps imported successfully"
        AppLanguage.ARABIC -> "تم استيراد %d تطبيقات بنجاح"
    }
    val batchImportBtn: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入"
        AppLanguage.ENGLISH -> "Import"
        AppLanguage.ARABIC -> "استيراد"
    }
    
    // ==================== ====================
    val templateExport: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出为模板"
        AppLanguage.ENGLISH -> "Export as Template"
        AppLanguage.ARABIC -> "تصدير كقالب"
    }
    val templateImport: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入模板"
        AppLanguage.ENGLISH -> "Import Template"
        AppLanguage.ARABIC -> "استيراد قالب"
    }
    val templateExportSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "模板已导出"
        AppLanguage.ENGLISH -> "Template exported"
        AppLanguage.ARABIC -> "تم تصدير القالب"
    }
    val templateImportSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "模板导入成功"
        AppLanguage.ENGLISH -> "Template imported"
        AppLanguage.ARABIC -> "تم استيراد القالب"
    }
    
    val menuLinuxEnvironment: String get() = CommonStrings.menuLinuxEnvironment
    
    val menuLanguage: String get() = CommonStrings.menuLanguage

    // ==================== Create App ====================
    val createWebApp: String get() = CreateStrings.createWebApp
    
    val createMediaApp: String get() = CreateStrings.createMediaApp
    
    val createHtmlApp: String get() = CreateStrings.createHtmlApp
    
    val createFrontendApp: String get() = CreateStrings.createFrontendApp
    
    val editFrontendApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑前端项目"
        AppLanguage.ENGLISH -> "Edit Frontend Project"
        AppLanguage.ARABIC -> "تعديل مشروع الواجهة الأمامية"
    }
    
    val createWordPressApp: String get() = CreateStrings.createWordPressApp
    
    val createNodeJsApp: String get() = CreateStrings.createNodeJsApp
    
    val createPhpApp: String get() = CreateStrings.createPhpApp
    
    val createPythonApp: String get() = CreateStrings.createPythonApp
    
    val createGoApp: String get() = CreateStrings.createGoApp
    
    // ==================== Multi-site Aggregation App ====================
    val appTypeMultiWeb: String get() = CommonStrings.appTypeMultiWeb
    
    val createMultiWebApp: String get() = CreateStrings.createMultiWebApp
    
    val multiWebHeroTitle: String get() = CreateStrings.multiWebHeroTitle
    
    val multiWebHeroDesc: String get() = CreateStrings.multiWebHeroDesc
    
    val multiWebDisplayMode: String get() = CreateStrings.multiWebDisplayMode
    
    val multiWebModeTabs: String get() = CreateStrings.multiWebModeTabs
    
    val multiWebModeTabsDesc: String get() = CreateStrings.multiWebModeTabsDesc
    
    val multiWebModeCards: String get() = CreateStrings.multiWebModeCards
    
    val multiWebModeCardsDesc: String get() = CreateStrings.multiWebModeCardsDesc
    
    val multiWebModeFeed: String get() = CreateStrings.multiWebModeFeed
    
    val multiWebModeFeedDesc: String get() = CreateStrings.multiWebModeFeedDesc
    
    val multiWebAddSite: String get() = CreateStrings.multiWebAddSite
    
    val multiWebSiteName: String get() = CreateStrings.multiWebSiteName
    
    val multiWebSiteUrl: String get() = CreateStrings.multiWebSiteUrl
    
    val multiWebSiteEmoji: String get() = CreateStrings.multiWebSiteEmoji
    
    val multiWebSiteCategory: String get() = CreateStrings.multiWebSiteCategory
    
    val multiWebCssSelector: String get() = CreateStrings.multiWebCssSelector
    
    val multiWebCssSelectorHint: String get() = CreateStrings.multiWebCssSelectorHint
    
    val multiWebSiteList: String get() = CreateStrings.multiWebSiteList
    
    val multiWebNoSites: String get() = CreateStrings.multiWebNoSites
    
    val multiWebSiteCount: String get() = CreateStrings.multiWebSiteCount
    
    val multiWebFeedTip: String get() = CreateStrings.multiWebFeedTip
    
    val multiWebQuickAdd: String get() = CreateStrings.multiWebQuickAdd

    val multiWebQuickAddHint: String get() = CreateStrings.multiWebQuickAddHint
    
    val multiWebFetchingTitle: String get() = CreateStrings.multiWebFetchingTitle
    
    val multiWebBatchImport: String get() = CreateStrings.multiWebBatchImport
    
    val multiWebBatchHint: String get() = CreateStrings.multiWebBatchHint
    
    val multiWebImportCount: String get() = CreateStrings.multiWebImportCount
    
    val multiWebModeDrawer: String get() = CreateStrings.multiWebModeDrawer
    
    val multiWebModeDrawerDesc: String get() = CreateStrings.multiWebModeDrawerDesc
    
    val multiWebEditSite: String get() = CreateStrings.multiWebEditSite
    
    val multiWebDeleteSite: String get() = CreateStrings.multiWebDeleteSite
    
    val multiWebDisableSite: String get() = CreateStrings.multiWebDisableSite
    
    val multiWebEnableSite: String get() = CreateStrings.multiWebEnableSite

    val multiWebMoveUp: String get() = CreateStrings.multiWebMoveUp

    val multiWebMoveDown: String get() = CreateStrings.multiWebMoveDown

    val multiWebPaste: String get() = CreateStrings.multiWebPaste

    val multiWebPreview: String get() = CreateStrings.multiWebPreview

    val multiWebImportSites: String get() = CreateStrings.multiWebImportSites

    val multiWebEditList: String get() = CreateStrings.multiWebEditList

    val multiWebBatchImportHint: String get() = CreateStrings.multiWebBatchImportHint

    val createDocsSite: String get() = CreateStrings.createDocsSite
    
    // ==================== ====================
    val appTypeWeb: String get() = CommonStrings.appTypeWeb
    
    val appTypeImage: String get() = CommonStrings.appTypeImage
    
    val appTypeVideo: String get() = CommonStrings.appTypeVideo
    
    val appTypeHtml: String get() = CommonStrings.appTypeHtml
    
    val appTypeGallery: String get() = CommonStrings.appTypeGallery
    
    val appTypeFrontend: String get() = CommonStrings.appTypeFrontend
    
    val appTypeWordPress: String get() = CommonStrings.appTypeWordPress
    
    val appTypeNodeJs: String get() = CommonStrings.appTypeNodeJs
    
    val appTypePhp: String get() = CommonStrings.appTypePhp
    
    val appTypePython: String get() = CommonStrings.appTypePython
    
    val appTypeGo: String get() = CommonStrings.appTypeGo
    
    val appTypeDocsSite: String get() = CommonStrings.appTypeDocsSite
    
    // ==================== General ====================
    val dirNotExists: String get() = ProjectStrings.dirNotExists
    
    val copyingDocsFiles: String get() = ProjectStrings.copyingDocsFiles
    
    val projectImportFailed: String get() = ProjectStrings.projectImportFailed
    
    val frameworkDetected: String get() = ProjectStrings.frameworkDetected
    
    val enableSearch: String get() = ProjectStrings.enableSearch
    
    val preparingEnv: String get() = ProjectStrings.preparingEnv
    
    val startingServer: String get() = ProjectStrings.startingServer
    
    val serverStartFailed: String get() = ProjectStrings.serverStartFailed
    
    val phpSupportedFrameworks: String get() = ProjectStrings.phpSupportedFrameworks
    
    val pySupportedFrameworks: String get() = ProjectStrings.pySupportedFrameworks
    
    val goSupportedFrameworks: String get() = ProjectStrings.goSupportedFrameworks
    
    val docsSiteDesc: String get() = ProjectStrings.docsSiteDesc
    
    val docsNoIndex: String get() = ProjectStrings.docsNoIndex
    
    val docsHashRouting: String get() = ProjectStrings.docsHashRouting
    
    val docsHistoryRouting: String get() = ProjectStrings.docsHistoryRouting
    
    // ==================== PHP App Strings ====================
    val phpFrameworkDetected: String get() = ProjectStrings.phpFrameworkDetected
    
    val phpDocumentRoot: String get() = ProjectStrings.phpDocumentRoot
    
    val phpEntryFile: String get() = ProjectStrings.phpEntryFile
    
    val phpProjectReady: String get() = ProjectStrings.phpProjectReady
    
    val phpSelectProject: String get() = ProjectStrings.phpSelectProject
    
    val phpNoIndexFound: String get() = ProjectStrings.phpNoIndexFound
    
    val phpAppCheckingDeps: String get() = ProjectStrings.phpAppCheckingDeps
    
    val phpAppDownloading: String get() = ProjectStrings.phpAppDownloading
    
    val phpAppStartingServer: String get() = ProjectStrings.phpAppStartingServer
    
    val phpAppServerError: String get() = ProjectStrings.phpAppServerError
    
    val phpAppDownloadFailed: String get() = ProjectStrings.phpAppDownloadFailed
    
    val phpAppProjectNotFound: String get() = ProjectStrings.phpAppProjectNotFound
    
    val phpImportZip: String get() = ProjectStrings.phpImportZip
    
    val phpExtractingZip: String get() = ProjectStrings.phpExtractingZip
    
    val phpZipExtractFailed: String get() = ProjectStrings.phpZipExtractFailed
    
    val phpZipNoPhpFiles: String get() = ProjectStrings.phpZipNoPhpFiles
    
    // ==================== Python ====================
    val pySelectProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择 Python 项目目录"
        AppLanguage.ENGLISH -> "Select Python Project Directory"
        AppLanguage.ARABIC -> "اختر مجلد مشروع Python"
    }
    
    val pyServerType: String get() = when (lang) {
        AppLanguage.CHINESE -> "服务器类型"
        AppLanguage.ENGLISH -> "Server Type"
        AppLanguage.ARABIC -> "نوع الخادم"
    }
    
    val pyProjectReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python 项目已就绪"
        AppLanguage.ENGLISH -> "Python project ready"
        AppLanguage.ARABIC -> "مشروع Python جاهز"
    }
    
    val pyProjectNotFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python 项目文件不存在"
        AppLanguage.ENGLISH -> "Python project files not found"
        AppLanguage.ARABIC -> "ملفات مشروع Python غير موجودة"
    }
    
    val pyStartingPreview: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在启动 Python 应用预览..."
        AppLanguage.ENGLISH -> "Starting Python app preview..."
        AppLanguage.ARABIC -> "جارٍ تشغيل معاينة تطبيق Python..."
    }
    
    val pyNoStaticContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python 项目中未找到可显示的静态文件\n（后端应用需要 Python 运行时支持）"
        AppLanguage.ENGLISH -> "No displayable static files found in Python project\n(Backend apps require Python runtime support)"
        AppLanguage.ARABIC -> "لم يتم العثور على ملفات ثابتة قابلة للعرض في مشروع Python\n(تتطلب تطبيقات الواجهة الخلفية دعم بيئة تشغيل Python)"
    }
    
    val pyPreviewFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python 应用预览启动失败"
        AppLanguage.ENGLISH -> "Python app preview failed to start"
        AppLanguage.ARABIC -> "فشل تشغيل معاينة تطبيق Python"
    }
    
    // ==================== Go ====================
    val goSelectBinary: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择 Go 二进制文件"
        AppLanguage.ENGLISH -> "Select Go Binary"
        AppLanguage.ARABIC -> "اختر ملف Go الثنائي"
    }
    
    val goSelectProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择 Go 项目目录"
        AppLanguage.ENGLISH -> "Select Go Project Directory"
        AppLanguage.ARABIC -> "اختر مجلد مشروع Go"
    }
    
    val goProjectReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "Go 服务已就绪"
        AppLanguage.ENGLISH -> "Go service ready"
        AppLanguage.ARABIC -> "خدمة Go جاهزة"
    }
    
    val goProjectNotFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "Go 项目文件不存在"
        AppLanguage.ENGLISH -> "Go project files not found"
        AppLanguage.ARABIC -> "ملفات مشروع Go غير موجودة"
    }
    
    val goStartingPreview: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在启动 Go 应用预览..."
        AppLanguage.ENGLISH -> "Starting Go app preview..."
        AppLanguage.ARABIC -> "جارٍ تشغيل معاينة تطبيق Go..."
    }
    
    val goPreviewFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Go 应用预览启动失败"
        AppLanguage.ENGLISH -> "Go app preview failed to start"
        AppLanguage.ARABIC -> "فشل تشغيل معاينة تطبيق Go"
    }
    
    // ==================== Node.js ====================
    val nodeProjectNotFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js 项目文件不存在"
        AppLanguage.ENGLISH -> "Node.js project files not found"
        AppLanguage.ARABIC -> "ملفات مشروع Node.js غير موجودة"
    }
    
    val nodeStartingPreview: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在启动 Node.js 应用预览..."
        AppLanguage.ENGLISH -> "Starting Node.js app preview..."
        AppLanguage.ARABIC -> "جارٍ تشغيل معاينة تطبيق Node.js..."
    }
    
    val nodePreviewFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js 应用预览启动失败"
        AppLanguage.ENGLISH -> "Node.js app preview failed to start"
        AppLanguage.ARABIC -> "فشل تشغيل معاينة تطبيق Node.js"
    }
    
    // ==================== ====================
    val docsSelectProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择文档站点目录"
        AppLanguage.ENGLISH -> "Select Docs Site Directory"
        AppLanguage.ARABIC -> "اختر مجلد موقع التوثيق"
    }
    
    val docsGeneratorDetected: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测到生成器"
        AppLanguage.ENGLISH -> "Generator Detected"
        AppLanguage.ARABIC -> "تم اكتشاف المولد"
    }
    
    val docsRoutingMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "路由模式"
        AppLanguage.ENGLISH -> "Routing Mode"
        AppLanguage.ARABIC -> "وضع التوجيه"
    }
    
    val docsProjectReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "文档站点已就绪"
        AppLanguage.ENGLISH -> "Docs site ready"
        AppLanguage.ARABIC -> "موقع التوثيق جاهز"
    }
    
    // ==================== WordPress Strings ====================
    val wpDownloadDeps: String get() = CreateStrings.wpDownloadDeps
    
    val wpDownloadDesc: String get() = CreateStrings.wpDownloadDesc
    
    val wpMirrorCN: String get() = CreateStrings.wpMirrorCN
    
    val wpMirrorGlobal: String get() = CreateStrings.wpMirrorGlobal
    
    val wpDownloading: String get() = CreateStrings.wpDownloading
    
    val wpExtracting: String get() = CreateStrings.wpExtracting
    
    val wpDepsReady: String get() = CreateStrings.wpDepsReady
    
    val wpSiteTitle: String get() = CreateStrings.wpSiteTitle
    
    val wpAdminUser: String get() = CreateStrings.wpAdminUser
    
    val wpImportTheme: String get() = CreateStrings.wpImportTheme
    
    val wpImportPlugin: String get() = CreateStrings.wpImportPlugin
    
    val wpImportFull: String get() = CreateStrings.wpImportFull
    
    val wpStartingServer: String get() = CreateStrings.wpStartingServer
    
    val wpServerError: String get() = CreateStrings.wpServerError
    
    val wpClearCache: String get() = CreateStrings.wpClearCache
    
    val wpMirrorSource: String get() = CreateStrings.wpMirrorSource
    
    val wpAutoDetect: String get() = CreateStrings.wpAutoDetect
    
    val wpCheckingDeps: String get() = CreateStrings.wpCheckingDeps
    
    val wpCreatingProject: String get() = CreateStrings.wpCreatingProject
    
    val wpDownloadFailed: String get() = CreateStrings.wpDownloadFailed
    
    val wpProjectCreateFailed: String get() = CreateStrings.wpProjectCreateFailed
    
    val wpCreateTitle: String get() = CreateStrings.wpCreateTitle
    
    val wpSiteTitleHint: String get() = CreateStrings.wpSiteTitleHint
    
    val wpAdminUserHint: String get() = CreateStrings.wpAdminUserHint
    
    val wpBasicConfig: String get() = CreateStrings.wpBasicConfig
    
    val wpImportProject: String get() = CreateStrings.wpImportProject
    
    val wpImportProjectDesc: String get() = CreateStrings.wpImportProjectDesc
    
    val wpOrCreateNew: String get() = CreateStrings.wpOrCreateNew
    
    val wpCreateNewSite: String get() = CreateStrings.wpCreateNewSite
    
    val wpCreateNewSiteDesc: String get() = CreateStrings.wpCreateNewSiteDesc
    
    val wpSettings: String get() = CreateStrings.wpSettings
    
    val wpCacheSize: String get() = CreateStrings.wpCacheSize
    
    val wpCacheCleared: String get() = CreateStrings.wpCacheCleared
    
    val wpLandscapeMode: String get() = CreateStrings.wpLandscapeMode
    
    val wpProjectReady: String get() = CreateStrings.wpProjectReady
    
    val wpImportSuccess: String get() = CreateStrings.wpImportSuccess
    
    // ==================== Node.js Strings ====================
    val njsCreateTitle: String get() = ProjectStrings.njsCreateTitle
    
    val njsBasicConfig: String get() = ProjectStrings.njsBasicConfig
    
    val njsSelectProjectFolder: String get() = ProjectStrings.njsSelectProjectFolder
    
    val njsSelectProjectDesc: String get() = ProjectStrings.njsSelectProjectDesc
    
    val njsBuildMode: String get() = ProjectStrings.njsBuildMode
    
    val njsModeStatic: String get() = ProjectStrings.njsModeStatic
    
    val njsModeStaticDesc: String get() = ProjectStrings.njsModeStaticDesc
    
    val njsModeBackend: String get() = ProjectStrings.njsModeBackend
    
    val njsModeBackendDesc: String get() = ProjectStrings.njsModeBackendDesc
    
    val njsModeFullstack: String get() = ProjectStrings.njsModeFullstack
    
    val njsModeFullstackDesc: String get() = ProjectStrings.njsModeFullstackDesc
    
    val njsEntryFile: String get() = ProjectStrings.njsEntryFile
    
    val njsEntryFileHint: String get() = ProjectStrings.njsEntryFileHint
    
    val njsEnvVars: String get() = ProjectStrings.njsEnvVars
    
    val njsAddEnvVar: String get() = ProjectStrings.njsAddEnvVar
    
    val njsDownloadDeps: String get() = ProjectStrings.njsDownloadDeps
    
    val njsDownloading: String get() = ProjectStrings.njsDownloading
    
    val njsDownloadComplete: String get() = ProjectStrings.njsDownloadComplete
    
    val njsDownloadFailed: String get() = ProjectStrings.njsDownloadFailed
    
    val njsProjectDetected: String get() = ProjectStrings.njsProjectDetected
    
    val njsFramework: String get() = ProjectStrings.njsFramework
    
    val njsProjectReady: String get() = ProjectStrings.njsProjectReady
    
    val njsLandscapeMode: String get() = ProjectStrings.njsLandscapeMode
    
    // ==================== Action Buttons ====================
    val btnCreate: String get() = BuildStrings.btnCreate
    
    val btnPreview: String get() = BuildStrings.btnPreview
    
    val btnExport: String get() = BuildStrings.btnExport
    
    val btnSave: String get() = BuildStrings.btnSave
    
    val btnCancel: String get() = BuildStrings.btnCancel
    
    val btnDelete: String get() = BuildStrings.btnDelete
    
    val btnEdit: String get() = BuildStrings.btnEdit
    
    val editCoreConfig: String get() = BuildStrings.editCoreConfig
    
    val editCommonConfig: String get() = BuildStrings.editCommonConfig
    
    val btnLaunch: String get() = BuildStrings.btnLaunch
    
    val btnShortcut: String get() = BuildStrings.btnShortcut
    
    val btnConfirm: String get() = BuildStrings.btnConfirm
    
    val btnOk: String get() = BuildStrings.btnOk
    
    val btnRetry: String get() = BuildStrings.btnRetry
    
    val btnImport: String get() = BuildStrings.btnImport
    
    val btnBuild: String get() = BuildStrings.btnBuild
    
    val btnStartBuild: String get() = BuildStrings.btnStartBuild
    
    val btnReset: String get() = BuildStrings.btnReset
    
    val btnClearCache: String get() = BuildStrings.btnClearCache
    
    val help: String get() = BuildStrings.help
    
    val usageHelp: String get() = BuildStrings.usageHelp
    
    val iUnderstand: String get() = BuildStrings.iUnderstand
    
    val selectModuleCategory: String get() = BuildStrings.selectModuleCategory
    
    val autoDetect: String get() = BuildStrings.autoDetect
    
    val autoDetectCategoryHint: String get() = BuildStrings.autoDetectCategoryHint
    
    // ==================== AI Module Dev Help ====================
    val helpHowToUse: String get() = AiConfigStrings.helpHowToUse
    
    val helpHowToUseContent: String get() = AiConfigStrings.helpHowToUseContent
    
    val helpRequirementTips: String get() = AiConfigStrings.helpRequirementTips
    
    val helpRequirementTipsContent: String get() = AiConfigStrings.helpRequirementTipsContent
    
    val helpModelSelection: String get() = AiConfigStrings.helpModelSelection
    
    val helpModelSelectionContent: String get() = AiConfigStrings.helpModelSelectionContent
    
    val helpCategorySelection: String get() = AiConfigStrings.helpCategorySelection
    
    val helpCategorySelectionContent: String get() = AiConfigStrings.helpCategorySelectionContent
    
    val helpAutoCheck: String get() = AiConfigStrings.helpAutoCheck
    
    val helpAutoCheckContent: String get() = AiConfigStrings.helpAutoCheckContent
    
    val helpCodeEdit: String get() = AiConfigStrings.helpCodeEdit
    
    val helpCodeEditContent: String get() = AiConfigStrings.helpCodeEditContent
    
    val helpSaveModule: String get() = AiConfigStrings.helpSaveModule
    
    val helpSaveModuleContent: String get() = AiConfigStrings.helpSaveModuleContent

    // ==================== ====================
    val labelAppName: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用名称"
        AppLanguage.ENGLISH -> "App Name"
        AppLanguage.ARABIC -> "اسم التطبيق"
    }
    
    val labelUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "网站地址"
        AppLanguage.ENGLISH -> "Website URL"
        AppLanguage.ARABIC -> "عنوان الموقع"
    }
    
    val labelIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用图标"
        AppLanguage.ENGLISH -> "App Icon"
        AppLanguage.ARABIC -> "أيقونة التطبيق"
    }
    
    val labelBasicInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "基本信息"
        AppLanguage.ENGLISH -> "Basic Info"
        AppLanguage.ARABIC -> "المعلومات الأساسية"
    }
    
    val labelAdvancedConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级配置"
        AppLanguage.ENGLISH -> "Advanced Config"
        AppLanguage.ARABIC -> "الإعدادات المتقدمة"
    }
    
    val labelDisplaySettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示设置"
        AppLanguage.ENGLISH -> "Display Settings"
        AppLanguage.ARABIC -> "إعدادات العرض"
    }
    
    val labelAppInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用信息"
        AppLanguage.ENGLISH -> "App Info"
        AppLanguage.ARABIC -> "معلومات التطبيق"
    }
    
    // ==================== Hint Messages ====================
    val msgAppCreated: String get() = UiStrings.msgAppCreated
    
    val msgAppDeleted: String get() = UiStrings.msgAppDeleted
    
    val msgLoading: String get() = UiStrings.msgLoading
    
    val msgNoApps: String get() = UiStrings.msgNoApps
    
    val msgLanguageChanged: String get() = UiStrings.msgLanguageChanged
    
    val msgExportSuccess: String get() = UiStrings.msgExportSuccess
    
    val msgExportFailed: String get() = UiStrings.msgExportFailed
    
    val msgImportSuccess: String get() = UiStrings.msgImportSuccess
    
    val msgImportFailed: String get() = UiStrings.msgImportFailed
    
    val msgCopied: String get() = UiStrings.msgCopied
    
    val msgDeleted: String get() = UiStrings.msgDeleted

    // ==================== ====================
    val menuRuntimeDeps: String get() = CommonStrings.menuRuntimeDeps
    
    // ==================== Port Management ====================
    val menuPortManager: String get() = CommonStrings.menuPortManager
    
    val portManagerTitle: String get() = ShellStrings.portManagerTitle
    
    val portManagerRunningServices: String get() = ShellStrings.portManagerRunningServices
    
    val portManagerNoServices: String get() = ShellStrings.portManagerNoServices
    
    val portManagerAllReleased: String get() = ShellStrings.portManagerAllReleased
    
    val portManagerKillAll: String get() = ShellStrings.portManagerKillAll
    
    val portManagerKillAllConfirm: String get() = ShellStrings.portManagerKillAllConfirm
    
    val portManagerKillService: String get() = ShellStrings.portManagerKillService
    
    val portManagerOpen: String get() = ShellStrings.portManagerOpen
    
    val portManagerKill: String get() = ShellStrings.portManagerKill
    
    val portManagerPort: String get() = ShellStrings.portManagerPort
    
    val portManagerType: String get() = ShellStrings.portManagerType
    
    val portManagerProject: String get() = ShellStrings.portManagerProject
    
    val portManagerStatus: String get() = ShellStrings.portManagerStatus
    
    val portManagerResponding: String get() = ShellStrings.portManagerResponding
    
    val portManagerNotResponding: String get() = ShellStrings.portManagerNotResponding
    
    val portManagerUnknown: String get() = ShellStrings.portManagerUnknown
    
    val portManagerOrphanProcess: String get() = ShellStrings.portManagerOrphanProcess
    
    val portManagerServiceKilled: String get() = ShellStrings.portManagerServiceKilled
    
    val portManagerAllKilled: String get() = ShellStrings.portManagerAllKilled
    
    val portManagerKillFailed: String get() = ShellStrings.portManagerKillFailed
    
    val portManagerTypeLocalHttp: String get() = ShellStrings.portManagerTypeLocalHttp
    
    val portManagerTypeNodeJs: String get() = ShellStrings.portManagerTypeNodeJs
    
    val portManagerTypePhp: String get() = ShellStrings.portManagerTypePhp
    
    val portManagerTypePython: String get() = ShellStrings.portManagerTypePython
    
    val portManagerTypeGo: String get() = ShellStrings.portManagerTypeGo
    
    val portManagerUptime: String get() = ShellStrings.portManagerUptime
    
    val portManagerLatency: String get() = ShellStrings.portManagerLatency
    
    val portManagerPortRanges: String get() = ShellStrings.portManagerPortRanges
    
    val portManagerAutoRefresh: String get() = ShellStrings.portManagerAutoRefresh
    
    val portManagerKillConfirmSingle: String get() = ShellStrings.portManagerKillConfirmSingle
    
    val portManagerProcess: String get() = ShellStrings.portManagerProcess
    
    val portManagerStaleCleanedUp: String get() = ShellStrings.portManagerStaleCleanedUp
    
    val runtimeDepsTitle: String get() = ShellStrings.runtimeDepsTitle
    
    val runtimeDepsSubtitle: String get() = ShellStrings.runtimeDepsSubtitle
    
    val depSectionRuntimes: String get() = ShellStrings.depSectionRuntimes

    val depSectionRuntimePlugins: String get() = ShellStrings.depSectionRuntimePlugins

    val depSectionProjects: String get() = ShellStrings.depSectionProjects
    
    val depSectionDownload: String get() = ShellStrings.depSectionDownload
    
    val depSectionStorage: String get() = ShellStrings.depSectionStorage
    
    val depStatusReady: String get() = ShellStrings.depStatusReady
    
    val depStatusNotInstalled: String get() = ShellStrings.depStatusNotInstalled
    
    val depStatusDownloading: String get() = ShellStrings.depStatusDownloading
    
    val depPhpRuntime: String get() = ShellStrings.depPhpRuntime
    
    val depPhpDesc: String get() = ShellStrings.depPhpDesc
    
    val depWpCore: String get() = ShellStrings.depWpCore
    
    val depWpCoreDesc: String get() = ShellStrings.depWpCoreDesc
    
    val depSqlitePlugin: String get() = ShellStrings.depSqlitePlugin
    
    val depSqliteDesc: String get() = ShellStrings.depSqliteDesc
    
    val depNodeRuntime: String get() = ShellStrings.depNodeRuntime
    
    val depNodeDesc: String get() = ShellStrings.depNodeDesc
    
    val depPythonRuntime: String get() = ShellStrings.depPythonRuntime
    
    val depPythonDesc: String get() = ShellStrings.depPythonDesc
    
    val depGoInfo: String get() = ShellStrings.depGoInfo
    
    val depGoDesc: String get() = ShellStrings.depGoDesc
    
    val depProjectFiles: String get() = ShellStrings.depProjectFiles
    
    val depWpProjects: String get() = ShellStrings.depWpProjects
    
    val depNodeProjects: String get() = ShellStrings.depNodeProjects
    
    val depPythonProjects: String get() = ShellStrings.depPythonProjects
    
    val depGoProjects: String get() = ShellStrings.depGoProjects
    
    val depDocsProjects: String get() = ShellStrings.depDocsProjects
    
    fun depProjectCount(count: Int): String = when (lang) {
        AppLanguage.CHINESE -> "$count 个项目"
        AppLanguage.ENGLISH -> "$count project${if (count != 1) "s" else ""}"
        AppLanguage.ARABIC -> "$count مشروع"
    }
    
    val depMirrorSource: String get() = ShellStrings.depMirrorSource
    
    val depMirrorDesc: String get() = ShellStrings.depMirrorDesc
    
    val depMirrorCN: String get() = ShellStrings.depMirrorCN
    
    val depMirrorGlobal: String get() = ShellStrings.depMirrorGlobal
    
    val depMirrorAuto: String get() = ShellStrings.depMirrorAuto
    
    val depDownloadAll: String get() = ShellStrings.depDownloadAll
    
    val depDownloadAllDesc: String get() = ShellStrings.depDownloadAllDesc
    
    val depTotalStorage: String get() = ShellStrings.depTotalStorage
    
    val depClearAll: String get() = ShellStrings.depClearAll
    
    val depClearConfirm: String get() = ShellStrings.depClearConfirm
    
    val depClearWpCache: String get() = ShellStrings.depClearWpCache
    
    val depClearNodeCache: String get() = ShellStrings.depClearNodeCache
    
    val depClearPythonCache: String get() = ShellStrings.depClearPythonCache
    
    val depClearGoCache: String get() = ShellStrings.depClearGoCache

    val depClearPhpCache: String get() = ShellStrings.depClearPhpCache

    val depClearSqliteCache: String get() = ShellStrings.depClearSqliteCache

    val depAllReady: String get() = ShellStrings.depAllReady
    
    val depSomeNotReady: String get() = ShellStrings.depSomeNotReady
    
    val depInstall: String get() = ShellStrings.depInstall
    
    // ==================== ====================
    
    val depDlPause: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂停"
        AppLanguage.ENGLISH -> "Pause"
        AppLanguage.ARABIC -> "إيقاف مؤقت"
    }
    
    val depDlResume: String get() = when (lang) {
        AppLanguage.CHINESE -> "继续"
        AppLanguage.ENGLISH -> "Resume"
        AppLanguage.ARABIC -> "استئناف"
    }
    
    val depDlSpeed: String get() = when (lang) {
        AppLanguage.CHINESE -> "速度"
        AppLanguage.ENGLISH -> "Speed"
        AppLanguage.ARABIC -> "السرعة"
    }
    
    val depDlEta: String get() = when (lang) {
        AppLanguage.CHINESE -> "剩余时间"
        AppLanguage.ENGLISH -> "Time Left"
        AppLanguage.ARABIC -> "الوقت المتبقي"
    }
    
    val depDlStartTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "开始时间"
        AppLanguage.ENGLISH -> "Start Time"
        AppLanguage.ARABIC -> "وقت البدء"
    }
    
    val depDlUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载地址"
        AppLanguage.ENGLISH -> "URL"
        AppLanguage.ARABIC -> "رابط التنزيل"
    }
    
    val depDlPaused: String get() = when (lang) {
        AppLanguage.CHINESE -> "已暂停"
        AppLanguage.ENGLISH -> "Paused"
        AppLanguage.ARABIC -> "متوقف مؤقتًا"
    }
    
    // ==================== ====================
    val deleteConfirmTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "确认删除"
        AppLanguage.ENGLISH -> "Confirm Delete"
        AppLanguage.ARABIC -> "تأكيد الحذف"
    }
    
    val deleteConfirmMessage: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定要删除这个应用吗？"
        AppLanguage.ENGLISH -> "Are you sure you want to delete this app?"
        AppLanguage.ARABIC -> "هل أنت متأكد أنك تريد حذف هذا التطبيق؟"
    }
    
    // ==================== ====================
    val buildDialogTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "构建 APK"
        AppLanguage.ENGLISH -> "Build APK"
        AppLanguage.ARABIC -> "بناء APK"
    }
    
    val buildDialogBuilding: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在构建..."
        AppLanguage.ENGLISH -> "Building..."
        AppLanguage.ARABIC -> "جاري البناء..."
    }
    
    // ==================== Build Environment ====================
    val buildEnvironment: String get() = CreateStrings.buildEnvironment
    
    val envReady: String get() = CreateStrings.envReady
    
    val envNotInstalled: String get() = CreateStrings.envNotInstalled
    
    val envDownloading: String get() = CreateStrings.envDownloading
    
    val envInstalling: String get() = CreateStrings.envInstalling
    
    val canBuildFrontend: String get() = CreateStrings.canBuildFrontend
    
    val builtInPackagerReady: String get() = CreateStrings.builtInPackagerReady
    
    val installAdvancedBuildTool: String get() = CreateStrings.installAdvancedBuildTool
    
    val optionalEsbuildHint: String get() = CreateStrings.optionalEsbuildHint
    
    val buildTools: String get() = CreateStrings.buildTools
    
    val builtInPackager: String get() = CreateStrings.builtInPackager
    
    val pureKotlinImpl: String get() = CreateStrings.pureKotlinImpl
    
    val highPerfBuildTool: String get() = CreateStrings.highPerfBuildTool
    
    val installed: String get() = CreateStrings.installed
    
    val notInstalled: String get() = CreateStrings.notInstalled
    
    val ready: String get() = CreateStrings.ready
    
    val storageUsage: String get() = CreateStrings.storageUsage
    
    val cache: String get() = CreateStrings.cache
    
    val supportedFeatures: String get() = CreateStrings.supportedFeatures
    
    val techDescription: String get() = CreateStrings.techDescription
    
    val resetEnvironment: String get() = CreateStrings.resetEnvironment
    
    val resetEnvConfirm: String get() = CreateStrings.resetEnvConfirm
    
    val clearCacheTitle: String get() = CreateStrings.clearCacheTitle
    
    val clearCacheConfirm: String get() = CreateStrings.clearCacheConfirm
    
    val clean: String get() = CreateStrings.clean

    // ==================== Frontend Project Page ====================
    val selectProject: String get() = CreateStrings.selectProject
    
    val selectProjectFolder: String get() = CreateStrings.selectProjectFolder
    
    val selectProjectHint: String get() = CreateStrings.selectProjectHint
    
    val projectAnalysis: String get() = CreateStrings.projectAnalysis
    
    val framework: String get() = CreateStrings.framework
    
    val version: String get() = CreateStrings.version
    
    val packageManager: String get() = CreateStrings.packageManager
    
    val dependencyCount: String get() = CreateStrings.dependencyCount
    
    val outputDir: String get() = CreateStrings.outputDir
    
    val appConfig: String get() = CommonStrings.appConfig
    
    val importProject: String get() = CreateStrings.importProject
    
    val reimportProject: String get() = CreateStrings.reimportProject
    
    val buildProject: String get() = CreateStrings.buildProject
    
    val rebuildProject: String get() = CreateStrings.rebuildProject
    
    val scanningProject: String get() = CreateStrings.scanningProject
    
    val importing: String get() = CreateStrings.importing
    
    val checkingEnv: String get() = CreateStrings.checkingEnv
    
    val copyingProjectFiles: String get() = CreateStrings.copyingProjectFiles
    
    val installingDeps: String get() = CreateStrings.installingDeps
    
    val building: String get() = CreateStrings.building
    
    val processingOutput: String get() = CreateStrings.processingOutput
    
    val completed: String get() = CreateStrings.completed
    
    val failed: String get() = CreateStrings.failed
    
    val totalFiles: String get() = CreateStrings.totalFiles
    
    val logs: String get() = CreateStrings.logs
    
    val importLogs: String get() = CreateStrings.importLogs
    
    val importFrontendProject: String get() = CreateStrings.importFrontendProject
    
    val supportVueReactVite: String get() = CreateStrings.supportVueReactVite
    
    val usageSteps: String get() = CreateStrings.usageSteps
    
    val usageStepsContent: String get() = CreateStrings.usageStepsContent
    
    val builtInEngineReady: String get() = CreateStrings.builtInEngineReady

    // ==================== Media App Page ====================
    val createMediaAppTitle: String get() = CreateStrings.createMediaAppTitle
    
    val selectMediaType: String get() = ProjectStrings.selectMediaType
    
    val image: String get() = ProjectStrings.image
    
    val video: String get() = ProjectStrings.video
    
    val selectImage: String get() = ProjectStrings.selectImage
    
    val selectVideo: String get() = ProjectStrings.selectVideo
    
    val clickToSelectImage: String get() = ProjectStrings.clickToSelectImage
    
    val clickToSelectVideo: String get() = ProjectStrings.clickToSelectVideo
    
    val videoSelected: String get() = ProjectStrings.videoSelected
    
    val fillScreen: String get() = ProjectStrings.fillScreen
    
    val fillScreenHint: String get() = ProjectStrings.fillScreenHint
    
    val landscapeMode: String get() = ProjectStrings.landscapeMode
    
    val landscapeModeHint: String get() = ProjectStrings.landscapeModeHint
    
    val enableAudio: String get() = ProjectStrings.enableAudio
    
    val enableAudioHint: String get() = ProjectStrings.enableAudioHint
    
    val loopPlay: String get() = ProjectStrings.loopPlay
    
    val loopPlayHint: String get() = ProjectStrings.loopPlayHint
    
    val autoPlay: String get() = ProjectStrings.autoPlay
    
    val autoPlayHint: String get() = ProjectStrings.autoPlayHint
    
    val mediaAppHint: String get() = ProjectStrings.mediaAppHint
    
    val fullscreenDisplayImage: String get() = ProjectStrings.fullscreenDisplayImage
    
    val fullscreenPlayVideo: String get() = ProjectStrings.fullscreenPlayVideo

    // ==================== HTML App Page ====================
    val createHtmlAppTitle: String get() = CreateStrings.createHtmlAppTitle
    
    val selectFiles: String get() = CreateStrings.selectFiles
    
    val selectFilesHint: String get() = CreateStrings.selectFilesHint
    
    val htmlFile: String get() = CreateStrings.htmlFile
    
    val cssFile: String get() = CreateStrings.cssFile
    
    val jsFile: String get() = CreateStrings.jsFile
    
    val enableJavaScript: String get() = CreateStrings.enableJavaScript
    
    val enableJsHint: String get() = CreateStrings.enableJsHint
    
    val enableLocalStorage: String get() = CreateStrings.enableLocalStorage
    
    val enableLocalStorageHint: String get() = CreateStrings.enableLocalStorageHint
    
    val landscapeModeLabel: String get() = CreateStrings.landscapeModeLabel
    
    val orientationModeLabel: String get() = CreateStrings.orientationModeLabel
    
    val orientationModeHint: String get() = CreateStrings.orientationModeHint
    
    val orientationPortrait: String get() = CreateStrings.orientationPortrait
    
    val orientationLandscape: String get() = CreateStrings.orientationLandscape
    
    val orientationAuto: String get() = CreateStrings.orientationAuto
    
    val orientationAutoHint: String get() = CreateStrings.orientationAutoHint
    
    // Note.
    
    val orientationBasicLabel: String get() = CreateStrings.orientationBasicLabel
    
    val orientationAdvancedLabel: String get() = CreateStrings.orientationAdvancedLabel
    
    val orientationReversedLabel: String get() = CreateStrings.orientationReversedLabel
    
    val orientationSensorLabel: String get() = CreateStrings.orientationSensorLabel
    
    // Note.
    
    val orientationLandscapeDesc: String get() = CreateStrings.orientationLandscapeDesc
    
    val orientationAutoDesc: String get() = CreateStrings.orientationAutoDesc
    
    // Note.
    
    val orientationReversePortrait: String get() = CreateStrings.orientationReversePortrait
    
    val orientationReversePortraitDesc: String get() = CreateStrings.orientationReversePortraitDesc
    
    val orientationReverseLandscape: String get() = CreateStrings.orientationReverseLandscape
    
    val orientationReverseLandscapeDesc: String get() = CreateStrings.orientationReverseLandscapeDesc
    
    // Note.
    
    val orientationSensorPortrait: String get() = CreateStrings.orientationSensorPortrait
    
    val orientationSensorPortraitDesc: String get() = CreateStrings.orientationSensorPortraitDesc
    
    val orientationSensorPortraitHint: String get() = CreateStrings.orientationSensorPortraitHint
    
    val orientationSensorLandscape: String get() = CreateStrings.orientationSensorLandscape
    
    val orientationSensorLandscapeDesc: String get() = CreateStrings.orientationSensorLandscapeDesc
    
    val orientationSensorLandscapeHint: String get() = CreateStrings.orientationSensorLandscapeHint
    
    val keepScreenOnLabel: String get() = CreateStrings.keepScreenOnLabel
    
    val keepScreenOnHint: String get() = CreateStrings.keepScreenOnHint
    
    // Note.
    
    val screenAwakeModeLabel: String get() = CreateStrings.screenAwakeModeLabel
    
    val screenAwakeOff: String get() = CreateStrings.screenAwakeOff
    
    val screenAwakeOffDesc: String get() = CreateStrings.screenAwakeOffDesc
    
    val screenAwakeAlways: String get() = CreateStrings.screenAwakeAlways
    
    val screenAwakeAlwaysDesc: String get() = CreateStrings.screenAwakeAlwaysDesc
    
    val screenAwakeTimed: String get() = CreateStrings.screenAwakeTimed
    
    val screenAwakeTimedDesc: String get() = CreateStrings.screenAwakeTimedDesc
    
    fun screenAwakeTimedStatusDesc(minutes: Int): String = when (lang) {
        AppLanguage.CHINESE -> "定时常亮 ${minutes} 分钟后自动息屏"
        AppLanguage.ENGLISH -> "Screen on for $minutes min, then auto-sleep"
        AppLanguage.ARABIC -> "الشاشة مضاءة لمدة $minutes دقيقة، ثم السكون التلقائي"
    }
    
    val screenAwakeTimeoutLabel: String get() = CreateStrings.screenAwakeTimeoutLabel
    
    fun screenAwakeTimeoutValue(minutes: Int): String = when {
        minutes >= 60 -> when (lang) {
            AppLanguage.CHINESE -> "${minutes / 60} 小时${if (minutes % 60 > 0) " ${minutes % 60} 分" else ""}"
            AppLanguage.ENGLISH -> "${minutes / 60}h${if (minutes % 60 > 0) " ${minutes % 60}m" else ""}"
            AppLanguage.ARABIC -> "${minutes / 60} ساعة${if (minutes % 60 > 0) " ${minutes % 60} دقيقة" else ""}"
        }
        else -> when (lang) {
            AppLanguage.CHINESE -> "${minutes} 分钟"
            AppLanguage.ENGLISH -> "${minutes} min"
            AppLanguage.ARABIC -> "$minutes دقيقة"
        }
    }
    
    val screenBrightnessLabel: String get() = CreateStrings.screenBrightnessLabel
    
    val screenBrightnessAuto: String get() = CreateStrings.screenBrightnessAuto
    
    val screenBrightnessManual: String get() = CreateStrings.screenBrightnessManual
    
    val screenAwakeBatteryWarning: String get() = CreateStrings.screenAwakeBatteryWarning
    
    val screenAwakeTimedHint: String get() = CreateStrings.screenAwakeTimedHint

    val keyboardAdjustModeLabel: String get() = CreateStrings.keyboardAdjustModeLabel

    val keyboardAdjustModeHint: String get() = CreateStrings.keyboardAdjustModeHint

    val keyboardAdjustResize: String get() = CreateStrings.keyboardAdjustResize

    val keyboardAdjustResizeHint: String get() = CreateStrings.keyboardAdjustResizeHint

    val keyboardAdjustNothing: String get() = CreateStrings.keyboardAdjustNothing

    val keyboardAdjustNothingHint: String get() = CreateStrings.keyboardAdjustNothingHint

    val showFloatingBackButtonLabel: String get() = CreateStrings.showFloatingBackButtonLabel
    
    val showFloatingBackButtonHint: String get() = CreateStrings.showFloatingBackButtonHint

    val blockSystemNavigationGestureLabel: String get() = CreateStrings.blockSystemNavigationGestureLabel

    val blockSystemNavigationGestureHint: String get() = CreateStrings.blockSystemNavigationGestureHint

    val landscapeModeHintHtml: String get() = CreateStrings.landscapeModeHintHtml
    
    val projectIssuesDetected: String get() = CreateStrings.projectIssuesDetected
    
    val errorsCount: String get() = CreateStrings.errorsCount
    
    val warningsCount: String get() = CreateStrings.warningsCount
    
    val autoFixHint: String get() = CreateStrings.autoFixHint
    
    val viewAnalysisResult: String get() = CreateStrings.viewAnalysisResult
    
    val htmlAppTip: String get() = CreateStrings.htmlAppTip
    
    val featureTip: String get() = CreateStrings.featureTip
    
    val aboutFileReference: String get() = CreateStrings.aboutFileReference
    
    val fileReferenceHint: String get() = CreateStrings.fileReferenceHint
    
    val projectAnalysisResult: String get() = CreateStrings.projectAnalysisResult
    
    val fileInfo: String get() = CreateStrings.fileInfo
    
    val detectedIssues: String get() = CreateStrings.detectedIssues
    
    val suggestions: String get() = CreateStrings.suggestions
    
    val autoProcessHint: String get() = CreateStrings.autoProcessHint
    
    val gotIt: String get() = CreateStrings.gotIt

    // ==================== ZIP Import ====================
    
    val zipImportMode: String get() = ProjectStrings.zipImportMode
    
    val manualSelectMode: String get() = ProjectStrings.manualSelectMode
    
    val selectZipFile: String get() = ProjectStrings.selectZipFile
    
    val selectZipHint: String get() = ProjectStrings.selectZipHint
    
    val zipImporting: String get() = ProjectStrings.zipImporting
    
    val zipImportSuccess: String get() = ProjectStrings.zipImportSuccess
    
    val zipImportFailed: String get() = ProjectStrings.zipImportFailed
    
    val zipProjectAnalysis: String get() = ProjectStrings.zipProjectAnalysis
    
    val zipEntryFile: String get() = ProjectStrings.zipEntryFile
    
    val zipResourceStats: String get() = ProjectStrings.zipResourceStats
    
    val zipTotalFiles: String get() = ProjectStrings.zipTotalFiles
    
    val zipTotalSize: String get() = ProjectStrings.zipTotalSize
    
    val zipChangeEntry: String get() = ProjectStrings.zipChangeEntry
    
    val zipReimport: String get() = ProjectStrings.zipReimport
    
    val zipTip: String get() = ProjectStrings.zipTip
    
    val zipNoHtmlWarning: String get() = ProjectStrings.zipNoHtmlWarning
    
    val zipSelectEntryTitle: String get() = ProjectStrings.zipSelectEntryTitle
    
    val zipFileTreeTitle: String get() = ProjectStrings.zipFileTreeTitle

    // ==================== ====================

    val folderImportMode: String get() = CreateStrings.folderImportMode

    val folderSelectFolder: String get() = CreateStrings.folderSelectFolder

    val folderSelectHint: String get() = CreateStrings.folderSelectHint

    val folderImporting: String get() = CreateStrings.folderImporting

    val folderNoHtmlWarning: String get() = CreateStrings.folderNoHtmlWarning

    val folderTip: String get() = CreateStrings.folderTip

    val folderImportFailed: String get() = CreateStrings.folderImportFailed

    // ==================== Create App ====================
    val editApp: String get() = CreateStrings.editApp
    
    val inputAppName: String get() = CreateStrings.inputAppName
    
    val activationCodeVerify: String get() = CreateStrings.activationCodeVerify
    
    val activationCodeHint: String get() = CreateStrings.activationCodeHint
    
    val inputActivationCode: String get() = CreateStrings.inputActivationCode
    
    val popupAnnouncement: String get() = when (lang) {
        AppLanguage.CHINESE -> "弹窗公告"
        AppLanguage.ENGLISH -> "Popup Announcement"
        AppLanguage.ARABIC -> "إعلان منبثق"
    }
    
    val announcementTitle: String get() = CreateStrings.announcementTitle
    
    val announcementContent: String get() = CreateStrings.announcementContent
    
    val linkUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "链接地址（可选）"
        AppLanguage.ENGLISH -> "Link URL (optional)"
        AppLanguage.ARABIC -> "رابط URL (اختياري)"
    }
    
    val linkButtonText: String get() = when (lang) {
        AppLanguage.CHINESE -> "链接按钮文字"
        AppLanguage.ENGLISH -> "Link Button Text"
        AppLanguage.ARABIC -> "نص زر الرابط"
    }
    
    val viewDetails: String get() = CreateStrings.viewDetails
    
    val displayFrequency: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示频率"
        AppLanguage.ENGLISH -> "Display Frequency"
        AppLanguage.ARABIC -> "تكرار العرض"
    }
    
    val showOnce: String get() = when (lang) {
        AppLanguage.CHINESE -> "仅显示一次"
        AppLanguage.ENGLISH -> "Show Once Only"
        AppLanguage.ARABIC -> "عرض مرة واحدة فقط"
    }
    
    val everyLaunch: String get() = when (lang) {
        AppLanguage.CHINESE -> "每次启动"
        AppLanguage.ENGLISH -> "Every Launch"
        AppLanguage.ARABIC -> "كل تشغيل"
    }
    
    val showEmoji: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示表情"
        AppLanguage.ENGLISH -> "Show Emoji"
        AppLanguage.ARABIC -> "عرض الرموز التعبيرية"
    }
    
    val enableAnimation: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用动画"
        AppLanguage.ENGLISH -> "Enable Animation"
        AppLanguage.ARABIC -> "تفعيل الرسوم المتحركة"
    }
    
    // ==================== ====================
    val announcementTriggerSettings: String get() = CreateStrings.announcementTriggerSettings
    
    val announcementTriggerOnLaunch: String get() = CreateStrings.announcementTriggerOnLaunch
    
    val announcementTriggerOnLaunchHint: String get() = CreateStrings.announcementTriggerOnLaunchHint
    
    val announcementTriggerOnNoNetwork: String get() = CreateStrings.announcementTriggerOnNoNetwork
    
    val announcementTriggerOnNoNetworkHint: String get() = CreateStrings.announcementTriggerOnNoNetworkHint
    
    val announcementTriggerInterval: String get() = CreateStrings.announcementTriggerInterval
    
    val announcementTriggerIntervalHint: String get() = CreateStrings.announcementTriggerIntervalHint
    
    val announcementIntervalDisabled: String get() = CreateStrings.announcementIntervalDisabled
    
    val announcementTriggerIntervalIncludeLaunch: String get() = CreateStrings.announcementTriggerIntervalIncludeLaunch
    
    // ==================== Enhanced Announcement UI Strings ====================
    
    val announcementSubtitle: String get() = CreateStrings.announcementSubtitle
    
    val announcementContentSection: String get() = CreateStrings.announcementContentSection
    
    val announcementLinkSection: String get() = CreateStrings.announcementLinkSection
    
    val optional: String get() = CompatStrings.optional
    
    val announcementTriggersActive: String get() = CreateStrings.announcementTriggersActive
    
    val announcementAdvancedOptions: String get() = CreateStrings.announcementAdvancedOptions
    
    val announcementRequireConfirmLabel: String get() = CreateStrings.announcementRequireConfirmLabel
    
    val announcementRequireConfirmHint: String get() = CreateStrings.announcementRequireConfirmHint
    
    val announcementAllowNeverShowLabel: String get() = CreateStrings.announcementAllowNeverShowLabel
    
    val announcementAllowNeverShowHint: String get() = CreateStrings.announcementAllowNeverShowHint
    
    val announcementEmojiHint: String get() = CreateStrings.announcementEmojiHint
    
    val announcementAnimationHint: String get() = CreateStrings.announcementAnimationHint
    
    val announcementHighPriority: String get() = CreateStrings.announcementHighPriority
    
    val adBlocking: String get() = CompatStrings.adBlocking
    
    val enableAdBlock: String get() = CompatStrings.enableAdBlock
    
    val desktopMode: String get() = CompatStrings.desktopMode
    
    val fullscreenMode: String get() = CompatStrings.fullscreenMode
    
    val splashScreen: String get() = CompatStrings.splashScreen
    
    val backgroundMusic: String get() = CompatStrings.backgroundMusic
    
    val autoTranslate: String get() = CompatStrings.autoTranslate
    
    val webViewAdvancedSettings: String get() = CompatStrings.webViewAdvancedSettings
    
    val htmlApp: String get() = CompatStrings.htmlApp
    
    val frontendApp: String get() = CompatStrings.frontendApp
    
    val entryFile: String get() = CompatStrings.entryFile
    
    val totalFilesCount: String get() = CompatStrings.totalFilesCount
    
    val imageApp: String get() = CompatStrings.imageApp
    
    val videoApp: String get() = CompatStrings.videoApp
    
    val unknownFile: String get() = CompatStrings.unknownFile

    val extensionFabIconLabel: String get() = CompatStrings.extensionFabIconLabel
    
    val fabIconFromGallery: String get() = CompatStrings.fabIconFromGallery
    
    val fabIconSelected: String get() = CompatStrings.fabIconSelected
    
    val fabIconPreviewTitle: String get() = CompatStrings.fabIconPreviewTitle
    
    val fabIconPreviewDesc: String get() = CompatStrings.fabIconPreviewDesc
    
    val fabIconCustom: String get() = CompatStrings.fabIconCustom
    
    val fabIconChangeImage: String get() = CompatStrings.fabIconChangeImage
    
    val reselect: String get() = CompatStrings.reselect
    
    
    // ==================== Extension Module Page ====================
    val extensionModule: String get() = ModuleStrings.extensionModule
    
    val searchModules: String get() = ModuleStrings.searchModules
    
    val all: String get() = ModuleStrings.all
    
    val totalModules: String get() = ModuleStrings.totalModules
    
    val enabledModules: String get() = ModuleStrings.enabledModules
    
    val builtIn: String get() = ModuleStrings.builtIn
    
    val duplicate: String get() = ModuleStrings.duplicate
    
    val copyShareCode: String get() = ModuleStrings.copyShareCode
    
    val shareQrCode: String get() = ModuleStrings.shareQrCode
    
    val shareCodeCopied: String get() = ModuleStrings.shareCodeCopied
    
    val noModulesFound: String get() = ModuleStrings.noModulesFound
    
    val noModulesYet: String get() = ModuleStrings.noModulesYet
    
    val createFirstModule: String get() = CreateStrings.createFirstModule
    
    val totalModulesLabel: String get() = ModuleStrings.totalModulesLabel
    
    val builtInLabel: String get() = ModuleStrings.builtInLabel
    
    val customLabel: String get() = ModuleStrings.customLabel
    
    val tryDifferentSearch: String get() = ModuleStrings.tryDifferentSearch
    
    val createModuleHint: String get() = CreateStrings.createModuleHint
    
    val clearSearch: String get() = ModuleStrings.clearSearch
    
    val importModule: String get() = ModuleStrings.importModule
    
    val importFromFile: String get() = ModuleStrings.importFromFile
    
    val selectWtamodFile: String get() = ModuleStrings.selectWtamodFile
    
    val importFromShareCode: String get() = ModuleStrings.importFromShareCode
    
    val pasteShareCode: String get() = ModuleStrings.pasteShareCode
    
    val shareCode: String get() = ModuleStrings.shareCode
    
    val pasteShareCodeHint: String get() = ModuleStrings.pasteShareCodeHint
    
    val pasteFromClipboard: String get() = ModuleStrings.pasteFromClipboard
    
    val importFromQrImage: String get() = ModuleStrings.importFromQrImage
    
    val selectQrImageHint: String get() = ModuleStrings.selectQrImageHint
    
    val qrCodeNotFound: String get() = ModuleStrings.qrCodeNotFound
    
    val imageLoadFailed: String get() = ModuleStrings.imageLoadFailed
    
    val onlyOnMatchingUrls: String get() = ModuleStrings.onlyOnMatchingUrls
    
    val requiresSensitivePermissions: String get() = ModuleStrings.requiresSensitivePermissions
    
    val aiDevelop: String get() = AiStrings.aiDevelop
    
    val manualCreate: String get() = ModuleStrings.manualCreate
    
    val createModule: String get() = CreateStrings.createModule
    
    val aiModuleDeveloper: String get() = AiStrings.aiModuleDeveloper

    // ==================== Theme Settings ====================
    val themeSettings: String get() = UiStrings.themeSettings
    
    val theme: String get() = UiStrings.theme
    
    val appearance: String get() = CommonStrings.appearance
    
    val effects: String get() = UiStrings.effects
    
    val selectUiStyle: String get() = UiStrings.selectUiStyle
    
    val darkMode: String get() = UiStrings.darkMode
    
    val followSystem: String get() = UiStrings.followSystem
    
    val followSystemHint: String get() = UiStrings.followSystemHint
    
    val lightMode: String get() = UiStrings.lightMode
    
    val lightModeHint: String get() = UiStrings.lightModeHint
    
    val darkModeHint: String get() = UiStrings.darkModeHint
    
    // ==================== ====================
    val alwaysLight: String get() = when (lang) {
        AppLanguage.CHINESE -> "始终浅色"
        AppLanguage.ENGLISH -> "Always Light"
        AppLanguage.ARABIC -> "فاتح دائمًا"
    }
    
    val alwaysDark: String get() = when (lang) {
        AppLanguage.CHINESE -> "始终深色"
        AppLanguage.ENGLISH -> "Always Dark"
        AppLanguage.ARABIC -> "داكن دائمًا"
    }
    
    // ==================== Animation Speed ====================
    val speedSlow: String get() = UiStrings.speedSlow
    
    val speedNormal: String get() = UiStrings.speedNormal
    
    val speedFast: String get() = UiStrings.speedFast
    
    val speedInstant: String get() = UiStrings.speedInstant
    
    val previewEffect: String get() = UiStrings.previewEffect
    
    val button: String get() = UiStrings.button
    
    val enableAnimations: String get() = UiStrings.enableAnimations
    
    val enableAnimationsHint: String get() = UiStrings.enableAnimationsHint
    
    val particleEffects: String get() = UiStrings.particleEffects
    
    val particleEffectsHint: String get() = UiStrings.particleEffectsHint
    
    val particleNotSupported: String get() = UiStrings.particleNotSupported
    
    val hapticFeedback: String get() = UiStrings.hapticFeedback
    
    val hapticFeedbackHint: String get() = UiStrings.hapticFeedbackHint
    
    val soundFeedback: String get() = UiStrings.soundFeedback
    
    val soundFeedbackHint: String get() = UiStrings.soundFeedbackHint
    
    val animationSpeed: String get() = UiStrings.animationSpeed
    
    val currentThemeAnimStyle: String get() = UiStrings.currentThemeAnimStyle
    
    val interactionStyle: String get() = UiStrings.interactionStyle
    
    val glow: String get() = UiStrings.glow
    
    val particles: String get() = UiStrings.particles

    // ==================== About Page ====================
    val about: String get() = CommonStrings.about
    
    val independentDeveloper: String get() = CommonStrings.independentDeveloper
    
    val checkUpdate: String get() = CommonStrings.checkUpdate
    
    val checking: String get() = CommonStrings.checking
    
    val downloading: String get() = CommonStrings.downloading
    
    val currentVersion: String get() = CommonStrings.currentVersion
    
    val aboutThisApp: String get() = CommonStrings.aboutThisApp
    
    val aboutAppDescription: String get() = CommonStrings.aboutAppDescription
    
    val socialMedia: String get() = CommonStrings.socialMedia
    
    val exchangeGroup: String get() = CommonStrings.exchangeGroup
    
    val videoTutorial: String get() = CommonStrings.videoTutorial
    
    val openSourceRepo: String get() = CommonStrings.openSourceRepo
    
    val joinExchangeGroup: String get() = CommonStrings.joinExchangeGroup
    
    val learnProgressTogether: String get() = CommonStrings.learnProgressTogether
    
    val exchangeLearning: String get() = CommonStrings.exchangeLearning
    
    val internationalGroup: String get() = CommonStrings.internationalGroup
    
    val contactAuthor: String get() = CommonStrings.contactAuthor
    
    val feedbackCooperation: String get() = CommonStrings.feedbackCooperation
    
    val feedbackCooperationShort: String get() = CommonStrings.feedbackCooperationShort
    
    val emailContact: String get() = CommonStrings.emailContact
    
    val internationalEmail: String get() = CommonStrings.internationalEmail
    
    val updateLater: String get() = CommonStrings.updateLater
    
    val downloadComplete: String get() = CommonStrings.downloadComplete
    
    val checkUpdateFailed: String get() = CommonStrings.checkUpdateFailed

    val autoCheckUpdate: String get() = CommonStrings.autoCheckUpdate

    val autoCheckUpdateDesc: String get() = CommonStrings.autoCheckUpdateDesc

    // ==================== AI ====================
    val aiGenerating: String get() = AiStrings.aiGenerating
    
    val aiAnalyzing: String get() = AiStrings.aiAnalyzing
    
    val aiCompleted: String get() = AiStrings.aiCompleted
    
    val aiPlanning: String get() = AiStrings.aiPlanning
    
    val aiGeneratingCode: String get() = AiStrings.aiGeneratingCode
    
    val aiChecking: String get() = AiStrings.aiChecking
    
    val aiFixing: String get() = AiStrings.aiFixing
    
    val aiScanning: String get() = AiStrings.aiScanning
    
    val aiError: String get() = AiStrings.aiError
    
    // ==================== General ====================
    val yes: String get() = UiStrings.yes
    
    val no: String get() = UiStrings.no
    
    val error: String get() = UiStrings.error
    
    val success: String get() = UiStrings.success
    
    val close: String get() = UiStrings.close
    
    val cancel: String get() = UiStrings.cancel
    
    val copy: String get() = UiStrings.copy
    
    val share: String get() = UiStrings.share
    
    val download: String get() = UiStrings.download
    
    val remove: String get() = UiStrings.remove
    
    val clear: String get() = UiStrings.clear
    
    val add: String get() = UiStrings.add
    
    val enabled: String get() = UiStrings.enabled
    
    val disabled: String get() = UiStrings.disabled
    
    val enable: String get() = UiStrings.enable
    
    val disable: String get() = UiStrings.disable
    
    val tip: String get() = UiStrings.tip
    
    val warning: String get() = UiStrings.warning
    
    val info: String get() = UiStrings.info
    
    // ==================== ====================
    val emptyStateHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击下方按钮创建您的第一个应用"
        AppLanguage.ENGLISH -> "Tap the button below to create your first app"
        AppLanguage.ARABIC -> "اضغط على الزر أدناه لإنشاء تطبيقك الأول"
    }
    
    // ==================== ====================
    val featureImportBuiltProjects: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入已构建的 Vue/React/Angular 项目"
        AppLanguage.ENGLISH -> "Import built Vue/React/Angular projects"
        AppLanguage.ARABIC -> "استيراد مشاريع Vue/React/Angular المبنية"
    }
    
    val featureAutoDetectFramework: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动检测项目类型和框架"
        AppLanguage.ENGLISH -> "Auto detect project type and framework"
        AppLanguage.ARABIC -> "الكشف التلقائي عن نوع المشروع وإطار العمل"
    }
    
    val featureSupportViteWebpack: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持 Vite/Webpack 构建输出"
        AppLanguage.ENGLISH -> "Support Vite/Webpack build output"
        AppLanguage.ARABIC -> "دعم مخرجات بناء Vite/Webpack"
    }
    
    val featureTypeScriptSupport: String get() = when (lang) {
        AppLanguage.CHINESE -> "TypeScript 项目支持"
        AppLanguage.ENGLISH -> "TypeScript project support"
        AppLanguage.ARABIC -> "دعم مشاريع TypeScript"
    }
    
    val featureStaticAssets: String get() = when (lang) {
        AppLanguage.CHINESE -> "静态资源自动处理"
        AppLanguage.ENGLISH -> "Auto process static assets"
        AppLanguage.ARABIC -> "معالجة الأصول الثابتة تلقائيًا"
    }
    
    val featureEsbuildOptional: String get() = when (lang) {
        AppLanguage.CHINESE -> "esbuild 高性能构建（可选）"
        AppLanguage.ENGLISH -> "esbuild high-performance build (optional)"
        AppLanguage.ARABIC -> "بناء esbuild عالي الأداء (اختياري)"
    }
    val featureHtmlOptimize: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML 应用 JS/CSS 压缩优化"
        AppLanguage.ENGLISH -> "HTML app JS/CSS minification"
        AppLanguage.ARABIC -> "ضغط JS/CSS لتطبيقات HTML"
    }
    val featureNodeTsPreCompile: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js TypeScript 预编译"
        AppLanguage.ENGLISH -> "Node.js TypeScript pre-compilation"
        AppLanguage.ARABIC -> "تجميع TypeScript المسبق لـ Node.js"
    }
    
    val techDescriptionContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "本应用采用第一性原理设计，不依赖传统的 Node.js 运行时：\n\n• 内置打包器：纯 Kotlin 实现，可处理简单项目\n• esbuild：为 Android 编译的原生二进制，高性能\n• 渐进式降级：总能找到可用的构建方案\n\n推荐工作流：在电脑上完成 npm run build，然后导入构建输出。"
        AppLanguage.ENGLISH -> "This app uses first principles design, not relying on traditional Node.js runtime:\n\n• Built-in packager: Pure Kotlin implementation for simple projects\n• esbuild: Native binary compiled for Android, high performance\n• Progressive degradation: Always finds a working build solution\n\nRecommended workflow: Complete npm run build on computer, then import build output."
        AppLanguage.ARABIC -> "يستخدم هذا التطبيق تصميم المبادئ الأولى، دون الاعتماد على وقت تشغيل Node.js التقليدي:\n\n• أداة التعبئة المدمجة: تنفيذ Kotlin خالص للمشاريع البسيطة\n• esbuild: ثنائي أصلي مترجم لـ Android، أداء عالي\n• التدهور التدريجي: يجد دائمًا حل بناء يعمل\n\nسير العمل الموصى به: أكمل npm run build على الكمبيوتر، ثم استورد مخرجات البناء."
    }

    // ==================== ====================
    val appIconModifier: String get() = CommonStrings.appIconModifier
    
    val searchApps: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索应用..."
        AppLanguage.ENGLISH -> "Search apps..."
        AppLanguage.ARABIC -> "البحث عن التطبيقات..."
    }
    
    val userApps: String get() = when (lang) {
        AppLanguage.CHINESE -> "用户应用"
        AppLanguage.ENGLISH -> "User Apps"
        AppLanguage.ARABIC -> "تطبيقات المستخدم"
    }
    
    val systemApps: String get() = when (lang) {
        AppLanguage.CHINESE -> "系统应用"
        AppLanguage.ENGLISH -> "System Apps"
        AppLanguage.ARABIC -> "تطبيقات النظام"
    }
    
    val modifyApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "Modify app"
        AppLanguage.ENGLISH -> "Modify App"
        AppLanguage.ARABIC -> "تعديل التطبيق"
    }
    
    val cloneInstall: String get() = when (lang) {
        AppLanguage.CHINESE -> "克隆安装"
        AppLanguage.ENGLISH -> "Clone Install"
        AppLanguage.ARABIC -> "تثبيت نسخة"
    }
    
    val originalApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "原应用"
        AppLanguage.ENGLISH -> "Original App"
        AppLanguage.ARABIC -> "التطبيق الأصلي"
    }
    
    val useOriginalIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用原图标"
        AppLanguage.ENGLISH -> "Use Original Icon"
        AppLanguage.ARABIC -> "استخدام الأيقونة الأصلية"
    }
    
    val shortcutCreated: String get() = when (lang) {
        AppLanguage.CHINESE -> "快捷方式创建成功"
        AppLanguage.ENGLISH -> "Shortcut created successfully"
        AppLanguage.ARABIC -> "تم إنشاء الاختصار بنجاح"
    }
    
    val cloneSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "克隆成功，请确认安装"
        AppLanguage.ENGLISH -> "Clone successful, please confirm installation"
        AppLanguage.ARABIC -> "تم النسخ بنجاح، يرجى تأكيد التثبيت"
    }

    // ==================== ====================
    val resourceEncryption: String get() = when (lang) {
        AppLanguage.CHINESE -> "资源加密"
        AppLanguage.ENGLISH -> "Resource Encryption"
        AppLanguage.ARABIC -> "تشفير الموارد"
    }
    
    val encryptionEnabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "已启用加密保护"
        AppLanguage.ENGLISH -> "Encryption protection enabled"
        AppLanguage.ARABIC -> "تم تفعيل حماية التشفير"
    }
    
    val encryptionLevel: String get() = when (lang) {
        AppLanguage.CHINESE -> "加密级别"
        AppLanguage.ENGLISH -> "Encryption Level"
        AppLanguage.ARABIC -> "مستوى التشفير"
    }
    
    val basic: String get() = when (lang) {
        AppLanguage.CHINESE -> "基础"
        AppLanguage.ENGLISH -> "Basic"
        AppLanguage.ARABIC -> "أساسي"
    }
    
    val standard: String get() = when (lang) {
        AppLanguage.CHINESE -> "标准"
        AppLanguage.ENGLISH -> "Standard"
        AppLanguage.ARABIC -> "قياسي"
    }
    
    val advanced: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级"
        AppLanguage.ENGLISH -> "Advanced"
        AppLanguage.ARABIC -> "متقدم"
    }

    // ==================== ====================
    val isolatedEnvironment: String get() = when (lang) {
        AppLanguage.CHINESE -> "独立环境"
        AppLanguage.ENGLISH -> "Isolated Environment"
        AppLanguage.ARABIC -> "بيئة معزولة"
    }
    
    val antiDetectionEnabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "已启用防检测保护"
        AppLanguage.ENGLISH -> "Anti-detection protection enabled"
        AppLanguage.ARABIC -> "تم تفعيل حماية مكافحة الكشف"
    }
    
    val isolationLevel: String get() = when (lang) {
        AppLanguage.CHINESE -> "隔离级别"
        AppLanguage.ENGLISH -> "Isolation Level"
        AppLanguage.ARABIC -> "مستوى العزل"
    }

    // ==================== Activation Dialog ====================
    val activateApp: String get() = CreateStrings.activateApp
    
    val enterActivationCodeToContinue: String get() = CreateStrings.enterActivationCodeToContinue
    
    val activationCodeExample: String get() = CreateStrings.activationCodeExample
    
    val activate: String get() = CreateStrings.activate
    
    val addActivationCode: String get() = CreateStrings.addActivationCode
    
    val useCustomCode: String get() = CreateStrings.useCustomCode
    
    val codeLength: String get() = CreateStrings.codeLength
    
    val chars: String get() = CreateStrings.chars
    
    val codeTooShort: String get() = CreateStrings.codeTooShort
    
    val batchGeneratedNote: String get() = CreateStrings.batchGeneratedNote
    
    fun tooManyAttemptsWithCountdown(remaining: String): String = when (lang) {
        AppLanguage.CHINESE -> "尝试次数过多，请在 $remaining 后重试"
        AppLanguage.ENGLISH -> "Too many attempts. Try again in $remaining"
        AppLanguage.ARABIC -> "محاولات كثيرة جدًا. حاول مرة أخرى بعد $remaining"
    }
    
    val invalidTimeLimitConfig: String get() = CreateStrings.invalidTimeLimitConfig
    
    val invalidUsageLimitConfig: String get() = CreateStrings.invalidUsageLimitConfig
    
    val validityDays: String get() = CreateStrings.validityDays
    
    val usageCount: String get() = CreateStrings.usageCount
    
    val noteOptional: String get() = CreateStrings.noteOptional
    
    val vipUserOnly: String get() = CreateStrings.vipUserOnly
    
    val requireEveryLaunch: String get() = CreateStrings.requireEveryLaunch
    
    val customDialogText: String get() = CreateStrings.customDialogText
    
    val customDialogTextHint: String get() = CreateStrings.customDialogTextHint
    
    val dialogTitle: String get() = CreateStrings.dialogTitle
    
    val dialogTitleHint: String get() = CreateStrings.dialogTitleHint
    
    val dialogSubtitle: String get() = CreateStrings.dialogSubtitle
    
    val dialogSubtitleHint: String get() = CreateStrings.dialogSubtitleHint
    
    val dialogInputLabel: String get() = CreateStrings.dialogInputLabel
    
    val dialogInputLabelHint: String get() = CreateStrings.dialogInputLabelHint
    
    val dialogButtonText: String get() = CreateStrings.dialogButtonText
    
    val dialogButtonTextHint: String get() = CreateStrings.dialogButtonTextHint
    
    val requireEveryLaunchHintOn: String get() = CreateStrings.requireEveryLaunchHintOn
    
    val requireEveryLaunchHintOff: String get() = CreateStrings.requireEveryLaunchHintOff

    // ==================== ====================
    val selectColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择颜色"
        AppLanguage.ENGLISH -> "Select Color"
        AppLanguage.ARABIC -> "اختيار اللون"
    }
    
    val hexColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "十六进制颜色"
        AppLanguage.ENGLISH -> "Hex Color"
        AppLanguage.ARABIC -> "لون سداسي عشري"
    }
    
    val hexColorHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "如: FF5722 或 80FF5722"
        AppLanguage.ENGLISH -> "e.g.: FF5722 or 80FF5722"
        AppLanguage.ARABIC -> "مثال: FF5722 أو 80FF5722"
    }

    // ==================== Online Music ====================
    val onlineMusic: String get() = MusicStrings.onlineMusic
    
    val searchSongName: String get() = MusicStrings.searchSongName
    
    val paid: String get() = MusicStrings.paid
    
    val musicChannel: String get() = MusicStrings.musicChannel
    
    val testConnection: String get() = MusicStrings.testConnection
    
    val testAllChannels: String get() = MusicStrings.testAllChannels
    
    val channelAvailable: String get() = MusicStrings.channelAvailable
    
    val channelUnavailable: String get() = MusicStrings.channelUnavailable
    
    val recommendedLabel: String get() = MusicStrings.recommendedLabel
    
    val channelTesting: String get() = MusicStrings.channelTesting
    
    val channelUntested: String get() = MusicStrings.channelUntested
    
    val searchOnlineMusic: String get() = MusicStrings.searchOnlineMusic
    
    val noMusicResults: String get() = MusicStrings.noMusicResults
    
    val previewListen: String get() = MusicStrings.previewListen
    
    val downloadToBgm: String get() = MusicStrings.downloadToBgm
    
    val downloadSuccess: String get() = MusicStrings.downloadSuccess
    
    val previewNote: String get() = MusicStrings.previewNote
    
    val loadingPlayUrl: String get() = MusicStrings.loadingPlayUrl
    
    val searchFailed: String get() = MusicStrings.searchFailed
    
    val selectChannelFirst: String get() = MusicStrings.selectChannelFirst
    
    val results: String get() = MusicStrings.results

    // ==================== ====================
    val selectModel: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择模型"
        AppLanguage.ENGLISH -> "Select Model"
        AppLanguage.ARABIC -> "اختيار النموذج"
    }
    
    val describeIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "描述你想要的图标"
        AppLanguage.ENGLISH -> "Describe the icon you want"
        AppLanguage.ARABIC -> "صف الأيقونة التي تريدها"
    }
    
    val iconDescriptionExample: String get() = when (lang) {
        AppLanguage.CHINESE -> "例如：一个蓝色渐变的音乐播放器图标"
        AppLanguage.ENGLISH -> "e.g.: A blue gradient music player icon"
        AppLanguage.ARABIC -> "مثال: أيقونة مشغل موسيقى بتدرج أزرق"
    }
    
    val generationResult: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成结果"
        AppLanguage.ENGLISH -> "Generation Result"
        AppLanguage.ARABIC -> "نتيجة التوليد"
    }
    
    val useThisIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用此图标"
        AppLanguage.ENGLISH -> "Use This Icon"
        AppLanguage.ARABIC -> "استخدام هذه الأيقونة"
    }
    
    val saving: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存中..."
        AppLanguage.ENGLISH -> "Saving..."
        AppLanguage.ARABIC -> "جاري الحفظ..."
    }
    
    val generateIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成图标"
        AppLanguage.ENGLISH -> "Generate Icon"
        AppLanguage.ARABIC -> "توليد الأيقونة"
    }
    
    val regenerate: String get() = when (lang) {
        AppLanguage.CHINESE -> "重新生成"
        AppLanguage.ENGLISH -> "Regenerate"
        AppLanguage.ARABIC -> "إعادة التوليد"
    }

    // ==================== ====================
    val backgroundType: String get() = when (lang) {
        AppLanguage.CHINESE -> "背景类型"
        AppLanguage.ENGLISH -> "Background Type"
        AppLanguage.ARABIC -> "نوع الخلفية"
    }
    
    val solidColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "纯色"
        AppLanguage.ENGLISH -> "Solid Color"
        AppLanguage.ARABIC -> "لون صلب"
    }
    
    val cropStatusBarBg: String get() = when (lang) {
        AppLanguage.CHINESE -> "裁剪状态栏背景"
        AppLanguage.ENGLISH -> "Crop Status Bar Background"
        AppLanguage.ARABIC -> "قص خلفية شريط الحالة"
    }
    
    val confirmCrop: String get() = when (lang) {
        AppLanguage.CHINESE -> "确认裁剪"
        AppLanguage.ENGLISH -> "Confirm Crop"
        AppLanguage.ARABIC -> "تأكيد القص"
    }

    // ==================== ====================
    val nextStepTimeAlign: String get() = when (lang) {
        AppLanguage.CHINESE -> "下一步：时间对齐"
        AppLanguage.ENGLISH -> "Next: Time Alignment"
        AppLanguage.ARABIC -> "التالي: محاذاة الوقت"
    }
    
    val tap: String get() = when (lang) {
        AppLanguage.CHINESE -> "打点"
        AppLanguage.ENGLISH -> "Tap"
        AppLanguage.ARABIC -> "نقر"
    }
    
    val previousStep: String get() = when (lang) {
        AppLanguage.CHINESE -> "Previous"
        AppLanguage.ENGLISH -> "Previous"
        AppLanguage.ARABIC -> "السابق"
    }
    
    val nextStep: String get() = when (lang) {
        AppLanguage.CHINESE -> "Next"
        AppLanguage.ENGLISH -> "Next"
        AppLanguage.ARABIC -> "التالي"
    }
    
    val backToModify: String get() = when (lang) {
        AppLanguage.CHINESE -> "返回修改"
        AppLanguage.ENGLISH -> "Back to Modify"
        AppLanguage.ARABIC -> "العودة للتعديل"
    }
    
    val saveLrc: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存 LRC"
        AppLanguage.ENGLISH -> "Save LRC"
        AppLanguage.ARABIC -> "حفظ LRC"
    }
    
    // ==================== Extra Strings ====================
    val seconds: String get() = CommonStrings.seconds
    
    val allowClickToSkip: String get() = CommonStrings.allowClickToSkip
    
    val hotSearch: String get() = CommonStrings.hotSearch
    
    val searchHistory: String get() = CommonStrings.searchHistory
    
    val musicSource: String get() = CommonStrings.musicSource
    
    val unknownArtist: String get() = CommonStrings.unknownArtist
    
    val downloaded: String get() = CommonStrings.downloaded
    
    val downloadFailed: String get() = CommonStrings.downloadFailed
    
    val searching: String get() = CommonStrings.searching
    
    val randomRecommend: String get() = CommonStrings.randomRecommend
    
    val aiGenerateIcon: String get() = AiStrings.aiGenerateIcon
    
    val noImageGenModel: String get() = CommonStrings.noImageGenModel
    
    val addImageGenModelHint: String get() = CommonStrings.addImageGenModelHint
    
    val referenceImages: String get() = CommonStrings.referenceImages
    
    val addImage: String get() = CommonStrings.addImage
    
    val generatedIcon: String get() = CommonStrings.generatedIcon
    
    val presetColors: String get() = CommonStrings.presetColors
    
    val customColor: String get() = CommonStrings.customColor
    
    val currentSelection: String get() = CommonStrings.currentSelection
    
    val hexColorFormat: String get() = CommonStrings.hexColorFormat
    
    val dragToSelectArea: String get() = CommonStrings.dragToSelectArea
    
    val loadingImage: String get() = CommonStrings.loadingImage
    
    val cropSize: String get() = CommonStrings.cropSize
    
    val originalSize: String get() = CommonStrings.originalSize
    
    val statusBarHeight: String get() = CommonStrings.statusBarHeight
    
    val restoreDefault: String get() = CommonStrings.restoreDefault
    
    val statusBarPreview: String get() = CommonStrings.statusBarPreview
    
    val noImageSelected: String get() = CommonStrings.noImageSelected
    
    val backgroundColor: String get() = CommonStrings.backgroundColor
    
    val selectBackgroundImage: String get() = CommonStrings.selectBackgroundImage
    
    val imageSelected: String get() = CommonStrings.imageSelected
    
    val clickToChangeOrClear: String get() = CommonStrings.clickToChangeOrClear
    
    val changeImage: String get() = CommonStrings.changeImage
    
    val clearImage: String get() = CommonStrings.clearImage
    
    val backgroundAlpha: String get() = CommonStrings.backgroundAlpha
    
    val transparent: String get() = CommonStrings.transparent
    
    val opaque: String get() = CommonStrings.opaque
    
    val inputLyrics: String get() = CommonStrings.inputLyrics
    
    val timeAlignment: String get() = CommonStrings.timeAlignment
    
    val previewConfirm: String get() = CommonStrings.previewConfirm
    
    val duration: String get() = CommonStrings.duration
    
    val inputLyricsHint: String get() = CommonStrings.inputLyricsHint
    
    val lyricsPlaceholder: String get() = CommonStrings.lyricsPlaceholder
    
    val totalLyricsLines: String get() = CommonStrings.totalLyricsLines
    
    val alignmentHint: String get() = CommonStrings.alignmentHint
    
    val rewind3s: String get() = CommonStrings.rewind3s
    
    val play: String get() = CommonStrings.play
    
    val pause: String get() = CommonStrings.pause
    
    val reTap: String get() = CommonStrings.reTap
    
    val undo: String get() = CommonStrings.undo
    
    val redo: String get() = CommonStrings.redo
    
    val progress: String get() = CommonStrings.progress
    
    val activationSuccess: String get() = CommonStrings.activationSuccess
    
    val activationCodeCopied: String get() = CreateStrings.activationCodeCopied
    
    val copyActivationCode: String get() = CommonStrings.copyActivationCode
    
    val noActivationCodes: String get() = CommonStrings.noActivationCodes
    
    val activationCodeType: String get() = CreateStrings.activationCodeType
    
    // Activation.
    val activationTypePermanent: String get() = CommonStrings.activationTypePermanent
    val activationTypePermanentDesc: String get() = CommonStrings.activationTypePermanentDesc
    val activationTypeTimeLimited: String get() = CommonStrings.activationTypeTimeLimited
    val activationTypeTimeLimitedDesc: String get() = CommonStrings.activationTypeTimeLimitedDesc
    val activationTypeUsageLimited: String get() = CommonStrings.activationTypeUsageLimited
    val activationTypeUsageLimitedDesc: String get() = CommonStrings.activationTypeUsageLimitedDesc
    val activationTypeDeviceBound: String get() = CommonStrings.activationTypeDeviceBound
    val activationTypeDeviceBoundDesc: String get() = CommonStrings.activationTypeDeviceBoundDesc
    val activationTypeCombined: String get() = CommonStrings.activationTypeCombined
    val activationTypeCombinedDesc: String get() = CommonStrings.activationTypeCombinedDesc
    
    val activated: String get() = CommonStrings.activated
    
    val activationExpired: String get() = CommonStrings.activationExpired
    
    val activationTime: String get() = CommonStrings.activationTime
    
    val remainingTime: String get() = CommonStrings.remainingTime
    
    val expireTime: String get() = CommonStrings.expireTime
    
    val remainingUsage: String get() = CommonStrings.remainingUsage
    
    val deviceBound: String get() = CommonStrings.deviceBound
    
    val invalidActivationCode: String get() = CommonStrings.invalidActivationCode
    
    val activationCodeBoundToOtherDevice: String get() = CreateStrings.activationCodeBoundToOtherDevice
    
    val activationCodeExpired: String get() = CreateStrings.activationCodeExpired
    
    val activationCodeUsageExceeded: String get() = CreateStrings.activationCodeUsageExceeded
    
    val appAlreadyActivated: String get() = CommonStrings.appAlreadyActivated
    
    // ── Enhanced Activation Dialog Strings ──
    
    val activationSuccessHint: String get() = CommonStrings.activationSuccessHint
    
    val activationSuccessDetail: String get() = CommonStrings.activationSuccessDetail
    
    val appAlreadyActivatedHint: String get() = CommonStrings.appAlreadyActivatedHint
    
    val alreadyActivatedDetail: String get() = CommonStrings.alreadyActivatedDetail
    
    val invalidCodeSuggestion: String get() = CommonStrings.invalidCodeSuggestion
    
    val deviceMismatchDetail: String get() = CommonStrings.deviceMismatchDetail
    
    val deviceMismatchSuggestion: String get() = CommonStrings.deviceMismatchSuggestion
    
    val expiredDetail: String get() = CommonStrings.expiredDetail
    
    val expiredSuggestion: String get() = CommonStrings.expiredSuggestion
    
    val usageExceededDetail: String get() = CommonStrings.usageExceededDetail
    
    val usageExceededSuggestion: String get() = CommonStrings.usageExceededSuggestion

    val batchGenerate: String get() = CommonStrings.batchGenerate
    
    val batchCount: String get() = CommonStrings.batchCount
    
    val totalCodes: String get() = CommonStrings.totalCodes
    
    val deleteAllCodes: String get() = CommonStrings.deleteAllCodes
    
    val deleteAllCodesConfirm: String get() = CommonStrings.deleteAllCodesConfirm
    
    val copyAllCodes: String get() = CommonStrings.copyAllCodes
    
    val allCodesCopied: String get() = CommonStrings.allCodesCopied
    
    // ── End Enhanced Activation Dialog Strings ──
    
    val pleaseEnterActivationCode: String get() = CommonStrings.pleaseEnterActivationCode
    
    val permanentValid: String get() = CommonStrings.permanentValid
    
    val validityPeriod: String get() = CommonStrings.validityPeriod
    
    val days: String get() = CommonStrings.days
    
    val hours: String get() = CommonStrings.hours
    
    val times: String get() = CommonStrings.times
    
    val note: String get() = CommonStrings.note
    
    val cloneInstallWarning: String get() = CommonStrings.cloneInstallWarning
    
    val enableAudioLabel: String get() = CommonStrings.enableAudioLabel

    // ==================== ====================
    val iconLibrary: String get() = when (lang) {
        AppLanguage.CHINESE -> "图标库"
        AppLanguage.ENGLISH -> "Icon Library"
        AppLanguage.ARABIC -> "مكتبة الأيقونات"
    }
    
    val selectIconOrGenerate: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择图标或使用AI生成新图标"
        AppLanguage.ENGLISH -> "Select icon or generate new one with AI"
        AppLanguage.ARABIC -> "اختر أيقونة أو أنشئ واحدة جديدة بالذكاء الاصطناعي"
    }
    
    val useAiToGenerateIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用AI模型生成自定义图标"
        AppLanguage.ENGLISH -> "Use AI model to generate custom icon"
        AppLanguage.ARABIC -> "استخدم نموذج الذكاء الاصطناعي لإنشاء أيقونة مخصصة"
    }
    
    val iconLibraryEmpty: String get() = when (lang) {
        AppLanguage.CHINESE -> "图标库为空"
        AppLanguage.ENGLISH -> "Icon library is empty"
        AppLanguage.ARABIC -> "مكتبة الأيقونات فارغة"
    }
    
    val iconLibraryEmptyHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用AI生成图标后会自动保存到这里"
        AppLanguage.ENGLISH -> "Icons generated by AI will be saved here automatically"
        AppLanguage.ARABIC -> "سيتم حفظ الأيقونات المُنشأة بالذكاء الاصطناعي هنا تلقائيًا"
    }
    
    val savedIcons: String get() = when (lang) {
        AppLanguage.CHINESE -> "已保存的图标"
        AppLanguage.ENGLISH -> "Saved Icons"
        AppLanguage.ARABIC -> "الأيقونات المحفوظة"
    }
    
    val deleteIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除图标"
        AppLanguage.ENGLISH -> "Delete Icon"
        AppLanguage.ARABIC -> "حذف الأيقونة"
    }
    
    val deleteIconConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定要从图标库中删除此图标吗？"
        AppLanguage.ENGLISH -> "Are you sure you want to delete this icon from the library?"
        AppLanguage.ARABIC -> "هل أنت متأكد أنك تريد حذف هذه الأيقونة من المكتبة؟"
    }

    // ==================== Download and Save ====================
    val saveFailed: String get() = UiStrings.saveFailed
    
    val saveFailedWithReason: String get() = UiStrings.saveFailedWithReason
    
    val savedTo: String get() = UiStrings.savedTo
    
    val copiedToClipboard: String get() = UiStrings.copiedToClipboard
    
    val downloadingVideo: String get() = UiStrings.downloadingVideo
    
    val shareFailed: String get() = UiStrings.shareFailed
    
    val preparingShare: String get() = UiStrings.preparingShare
    
    val cannotOpenLink: String get() = UiStrings.cannotOpenLink

    val sslSecurityError: String get() = UiStrings.sslSecurityError

    val moduleTesting: String get() = ModuleStrings.moduleTesting

    val testingModules: String get() = UiStrings.testingModules

    val savingImage: String get() = UiStrings.savingImage
    
    val imageSavedToGallery: String get() = UiStrings.imageSavedToGallery
    
    val savingVideo: String get() = UiStrings.savingVideo
    
    val videoSavedToGallery: String get() = UiStrings.videoSavedToGallery
    
    val startDownload: String get() = UiStrings.startDownload
    
    val downloadFailedWithReason: String get() = UiStrings.downloadFailedWithReason

    // ==================== ====================
    val previewAnnouncementEffect: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览公告效果"
        AppLanguage.ENGLISH -> "Preview Announcement Effect"
        AppLanguage.ARABIC -> "معاينة تأثير الإعلان"
    }
    
    val inputAnnouncementTitle: String get() = CreateStrings.inputAnnouncementTitle
    
    val inputAnnouncementContent: String get() = CreateStrings.inputAnnouncementContent
    
    val linkAddress: String get() = when (lang) {
        AppLanguage.CHINESE -> "链接地址（可选）"
        AppLanguage.ENGLISH -> "Link URL (optional)"
        AppLanguage.ARABIC -> "عنوان الرابط (اختياري)"
    }
    
    val linkText: String get() = when (lang) {
        AppLanguage.CHINESE -> "链接文字"
        AppLanguage.ENGLISH -> "Link Text"
        AppLanguage.ARABIC -> "نص الرابط"
    }
    
    val showOnceOnly: String get() = when (lang) {
        AppLanguage.CHINESE -> "仅显示一次"
        AppLanguage.ENGLISH -> "Show once only"
        AppLanguage.ARABIC -> "عرض مرة واحدة فقط"
    }

    // ==================== AI Config ====================
    val textGeneration: String get() = AiConfigStrings.textGeneration
    
    val basicTextDialogue: String get() = AiConfigStrings.basicTextDialogue
    
    val audioUnderstanding: String get() = AiConfigStrings.audioUnderstanding
    
    val understandAndTranscribeAudio: String get() = AiConfigStrings.understandAndTranscribeAudio
    
    val imageUnderstanding: String get() = AiConfigStrings.imageUnderstanding
    
    val understandAndAnalyzeImages: String get() = AiConfigStrings.understandAndAnalyzeImages
    
    val imageGeneration: String get() = AiConfigStrings.imageGeneration
    
    val generateImages: String get() = AiConfigStrings.generateImages
    
    val codeGeneration: String get() = AiConfigStrings.codeGeneration
    
    val generateAndUnderstandCode: String get() = AiConfigStrings.generateAndUnderstandCode
    
    val functionCall: String get() = AiConfigStrings.functionCall
    
    val supportToolCall: String get() = AiConfigStrings.supportToolCall
    
    val longContext: String get() = AiConfigStrings.longContext
    
    val supportLongTextInput: String get() = AiConfigStrings.supportLongTextInput
    
    val goToConfig: String get() = AiConfigStrings.goToConfig
    
    val retry: String get() = AiConfigStrings.retry

    // ==================== General ====================
    val closeDialog: String get() = when (lang) {
        AppLanguage.CHINESE -> "关闭"
        AppLanguage.ENGLISH -> "Close"
        AppLanguage.ARABIC -> "إغلاق"
    }
    
    val deleteAction: String get() = when (lang) {
        AppLanguage.CHINESE -> "Delete"
        AppLanguage.ENGLISH -> "Delete"
        AppLanguage.ARABIC -> "حذف"
    }

    // ==================== More General Messages ====================
    val savingToGallery: String get() = UiStrings.savingToGallery
    
    val savingImageToGallery: String get() = UiStrings.savingImageToGallery
    
    val savingVideoToGallery: String get() = UiStrings.savingVideoToGallery
    
    val downloadStartFailed: String get() = UiStrings.downloadStartFailed
    
    val blobDownloadProcessing: String get() = UiStrings.blobDownloadProcessing
    
    val blobDownloadFailed: String get() = UiStrings.blobDownloadFailed
    
    val startDownloadCheckNotification: String get() = UiStrings.startDownloadCheckNotification
    
    val downloadLinkNotFound: String get() = UiStrings.downloadLinkNotFound
    
    val downloadFailedTryBrowser: String get() = UiStrings.downloadFailedTryBrowser
    
    val cannotOpenBrowser: String get() = UiStrings.cannotOpenBrowser
    
    val appliedPreset: String get() = CommonStrings.appliedPreset
    
    val presetSaved: String get() = UiStrings.presetSaved
    
    val copied: String get() = UiStrings.copied
    
    val duplicated: String get() = UiStrings.duplicated
    
    val deleted: String get() = UiStrings.deleted
    
    val shareCodeCopiedMsg: String get() = UiStrings.shareCodeCopiedMsg
    
    val cannotOpenInBrowser: String get() = UiStrings.cannotOpenInBrowser
    
    val noFilePathAvailable: String get() = UiStrings.noFilePathAvailable
    
    val copiedAllLogs: String get() = UiStrings.copiedAllLogs
    
    // ==================== ====================
    val console: String get() = when (lang) {
        AppLanguage.CHINESE -> "控制台"
        AppLanguage.ENGLISH -> "Console"
        AppLanguage.ARABIC -> "وحدة التحكم"
    }
    
    val noConsoleMessages: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无控制台消息"
        AppLanguage.ENGLISH -> "No console messages"
        AppLanguage.ARABIC -> "لا توجد رسائل في وحدة التحكم"
    }
    
    val inputJavaScript: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入 JavaScript..."
        AppLanguage.ENGLISH -> "Enter JavaScript..."
        AppLanguage.ARABIC -> "أدخل JavaScript..."
    }
    
    // ==================== Download Bridge ====================
    val preparingDownload: String get() = WebViewStrings.preparingDownload
    
    val cannotGetFileData: String get() = WebViewStrings.cannotGetFileData
    
    val downloadUnavailable: String get() = WebViewStrings.downloadUnavailable
    
    val processFileFailed: String get() = WebViewStrings.processFileFailed
    
    val readFileFailed: String get() = WebViewStrings.readFileFailed
    
    val downloadFailedPrefix: String get() = WebViewStrings.downloadFailedPrefix
    
    val copiedFullLog: String get() = WebViewStrings.copiedFullLog
    
    val copiedSourceCode: String get() = WebViewStrings.copiedSourceCode
    
    val copyAll: String get() = WebViewStrings.copyAll
    
    val logDetails: String get() = WebViewStrings.logDetails
    
    val level: String get() = WebViewStrings.level
    
    val time: String get() = CommonStrings.time
    
    val source: String get() = WebViewStrings.source
    
    val messageContent: String get() = WebViewStrings.messageContent
    
    val sourceCode: String get() = WebViewStrings.sourceCode
    
    val inputJavaScriptExpression: String get() = WebViewStrings.inputJavaScriptExpression
    
    val pleaseSelectTextModel: String get() = WebViewStrings.pleaseSelectTextModel
    
    val sendFailed: String get() = WebViewStrings.sendFailed
    
    val previewFailed: String get() = WebViewStrings.previewFailed
    
    val errorPrefix: String get() = WebViewStrings.errorPrefix
    
    val imageGenerationFailed: String get() = WebViewStrings.imageGenerationFailed

    // ==================== HTML Coding Assistant ====================
    val htmlCodingAssistant: String get() = AiStrings.htmlCodingAssistant
    
    val messagesCount: String get() = AiStrings.messagesCount
    
    val modelConfigInvalid: String get() = AiStrings.modelConfigInvalid
    
    val generatingCode: String get() = AiStrings.generatingCode
    
    val codeGenerated: String get() = AiStrings.codeGenerated
    
    val aiNoValidResponse: String get() = AiStrings.aiNoValidResponse
    
    val debugInfo: String get() = AiStrings.debugInfo
    
    val textContent: String get() = AiStrings.textContent
    
    val streamContent: String get() = AiStrings.streamContent
    
    val thinkingContent: String get() = AiStrings.thinkingContent
    
    val htmlCode: String get() = AiStrings.htmlCode
    
    val emptyText: String get() = AiStrings.emptyText
    
    val characters: String get() = AiStrings.characters
    
    val possibleReasons: String get() = AiStrings.possibleReasons
    
    val apiFormatIncompatible: String get() = AiStrings.apiFormatIncompatible
    
    val modelNotSupported: String get() = AiStrings.modelNotSupported
    
    val apiKeyQuotaInsufficient: String get() = AiStrings.apiKeyQuotaInsufficient
    
    val suggestionChangeModel: String get() = AiStrings.suggestionChangeModel
    
    val conversationCheckpoint: String get() = AiStrings.conversationCheckpoint
    
    val preview: String get() = AiStrings.preview
    
    val savedToPath: String get() = AiStrings.savedToPath
    
    val noCodeToExport: String get() = AiStrings.noCodeToExport
    
    val aiGeneratedProject: String get() = AiStrings.aiGeneratedProject
    
    val exportedToHtmlProject: String get() = AiStrings.exportedToHtmlProject
    
    val exportFailed: String get() = AiStrings.exportFailed
    
    val codeLibrary: String get() = AiStrings.codeLibrary
    
    val rollback: String get() = AiStrings.rollback
    
    val templates: String get() = AiStrings.templates
    
    val sessionList: String get() = AiStrings.sessionList
    
    val aiHelpsGenerateWebpage: String get() = AiStrings.aiHelpsGenerateWebpage
    
    val startNewConversation: String get() = AiStrings.startNewConversation
    
    val tutorial: String get() = AiStrings.tutorial
    
    val quickStart: String get() = AiStrings.quickStart
    
    val aiThinking: String get() = AiStrings.aiThinking
    
    val generatingImage: String get() = AiStrings.generatingImage
    
    val conversationHistory: String get() = AiStrings.conversationHistory
    
    val newConversation: String get() = AiStrings.newConversation
    
    val noConversationRecords: String get() = AiStrings.noConversationRecords
    
    val selectStyleTemplate: String get() = AiStrings.selectStyleTemplate
    
    val selected: String get() = AiStrings.selected
    
    val selectTemplateHint: String get() = AiStrings.selectTemplateHint
    
    val designTemplates: String get() = AiStrings.designTemplates
    
    val styleReferences: String get() = AiStrings.styleReferences
    
    val totalTemplates: String get() = AiStrings.totalTemplates
    
    val totalStyleReferences: String get() = AiStrings.totalStyleReferences
    
    val usageTutorial: String get() = AiStrings.usageTutorial
    
    val chapters: String get() = AiStrings.chapters
    
    val noTutorialContent: String get() = AiStrings.noTutorialContent
    
    val sections: String get() = AiStrings.sections
    
    val codeExample: String get() = AiStrings.codeExample
    
    val tips: String get() = AiStrings.tips
    
    val versionManagement: String get() = AiStrings.versionManagement
    
    val saveVersion: String get() = AiStrings.saveVersion
    
    val export: String get() = AiStrings.export
    
    val exportModule: String get() = AiStrings.exportModule
    
    val exportToDownloads: String get() = AiStrings.exportToDownloads
    
    val exportToDownloadsHint: String get() = AiStrings.exportToDownloadsHint
    
    val exportToCustomPath: String get() = AiStrings.exportToCustomPath
    
    val exportToCustomPathHint: String get() = AiStrings.exportToCustomPathHint
    
    val exportSuccess: String get() = AiStrings.exportSuccess
    
    val noSavedVersions: String get() = AiStrings.noSavedVersions
    
    val manualSave: String get() = AiStrings.manualSave
    
    val editMessage: String get() = AiStrings.editMessage
    
    val imagesCount: String get() = AiStrings.imagesCount
    
    val editWarning: String get() = AiStrings.editWarning
    
    val resend: String get() = AiStrings.resend
    
    val saveProject: String get() = AiStrings.saveProject
    
    val projectName: String get() = AiStrings.projectName
    
    val saveLocation: String get() = AiStrings.saveLocation
    
    val createProjectFolder: String get() = CreateStrings.createProjectFolder
    
    val willSaveFiles: String get() = AiStrings.willSaveFiles
    
    val save: String get() = AiStrings.save
    
    val favorites: String get() = AiStrings.favorites
    
    val aiCodeAutoSaved: String get() = AiStrings.aiCodeAutoSaved
    
    val noFavorites: String get() = AiStrings.noFavorites
    
    val codeLibraryEmpty: String get() = AiStrings.codeLibraryEmpty
    
    val use: String get() = AiStrings.use
    
    val unfavorite: String get() = AiStrings.unfavorite
    
    val favorite: String get() = AiStrings.favorite
    
    val exportToProjectLibrary: String get() = AiStrings.exportToProjectLibrary
    
    val delete: String get() = AiStrings.delete
    
    val conversationCheckpoints: String get() = AiStrings.conversationCheckpoints
    
    val rollbackHint: String get() = AiStrings.rollbackHint
    
    val noCheckpoints: String get() = AiStrings.noCheckpoints
    
    val autoCreateCheckpointHint: String get() = AiStrings.autoCreateCheckpointHint
    
    val codesCount: String get() = AiStrings.codesCount
    
    val continueDevBasedOnCode: String get() = AiStrings.continueDevBasedOnCode
    
    val exportedToProjectLibrary: String get() = AiStrings.exportedToProjectLibrary
    
    val rolledBackTo: String get() = AiStrings.rolledBackTo
    
    val rolledBackWithInputHint: String get() = AiStrings.rolledBackWithInputHint
    
    val rollbackFailed: String get() = AiStrings.rollbackFailed

    // ==================== Module Editor ====================
    val pleaseEnterModuleName: String get() = ModuleStrings.pleaseEnterModuleName
    
    val pleaseEnterCodeContent: String get() = ModuleStrings.pleaseEnterCodeContent
    
    val saveSuccess: String get() = ModuleStrings.saveSuccess
    
    val pleaseEnterRequirement: String get() = ModuleStrings.pleaseEnterRequirement
    
    val jumpToModuleEditor: String get() = ModuleStrings.jumpToModuleEditor
    
    val storagePermissionRequired: String get() = ModuleStrings.storagePermissionRequired
    
    val appConfigLoadFailed: String get() = CommonStrings.appConfigLoadFailed
    
    val frontendProject: String get() = ModuleStrings.frontendProject
    
    val shortcutCreatedSuccess: String get() = ModuleStrings.shortcutCreatedSuccess
    
    val projectExportedTo: String get() = ModuleStrings.projectExportedTo
    
    val preparing: String get() = ModuleStrings.preparing
    
    val buildApkForApp: String get() = ModuleStrings.buildApkForApp
    
    val buildCompleteInstallHint: String get() = ModuleStrings.buildCompleteInstallHint
    
    val buildFailed: String get() = ModuleStrings.buildFailed

    // ==================== ====================

    // ==================== Extension Module Cards ====================
    val saveAsScheme: String get() = ExtensionStrings.saveAsScheme
    
    val clearAll: String get() = ExtensionStrings.clearAll
    
    val selectModules: String get() = ExtensionStrings.selectModules
    
    val selectExtensionModules: String get() = ExtensionStrings.selectExtensionModules
    
    val doneWithCount: String get() = ExtensionStrings.doneWithCount
    
    val searchModulesHint: String get() = ExtensionStrings.searchModulesHint
    
    val testModule: String get() = ExtensionStrings.testModule
    
    val startTest: String get() = ExtensionStrings.startTest
    
    val addThisModule: String get() = ExtensionStrings.addThisModule
    
    val allSchemes: String get() = ExtensionStrings.allSchemes
    
    val moduleSchemes: String get() = ModuleStrings.moduleSchemes
    
    val saveAsSchemeTitle: String get() = ExtensionStrings.saveAsSchemeTitle
    
    val schemeName: String get() = ExtensionStrings.schemeName
    
    val inputSchemeName: String get() = ExtensionStrings.inputSchemeName
    
    val descriptionOptional: String get() = ExtensionStrings.descriptionOptional
    
    val briefDescriptionHint: String get() = ExtensionStrings.briefDescriptionHint
    
    val selectIcon: String get() = ExtensionStrings.selectIcon

    // ==================== ====================

    // ==================== ====================
    
    val tapToMark: String get() = when (lang) {
        AppLanguage.CHINESE -> "打点"
        AppLanguage.ENGLISH -> "Tap to Mark"
        AppLanguage.ARABIC -> "انقر للتحديد"
    }
    
    val goBackToModify: String get() = when (lang) {
        AppLanguage.CHINESE -> "返回修改"
        AppLanguage.ENGLISH -> "Go Back to Modify"
        AppLanguage.ARABIC -> "العودة للتعديل"
    }

    // ==================== ====================
    val exportData: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出数据"
        AppLanguage.ENGLISH -> "Export Data"
        AppLanguage.ARABIC -> "تصدير البيانات"
    }
    
    val importData: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入数据"
        AppLanguage.ENGLISH -> "Import Data"
        AppLanguage.ARABIC -> "استيراد البيانات"
    }

    // ==================== ====================
    val launchTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动时间"
        AppLanguage.ENGLISH -> "Launch Time"
        AppLanguage.ARABIC -> "وقت التشغيل"
    }
    
    val selectLaunchTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择启动时间"
        AppLanguage.ENGLISH -> "Select Launch Time"
        AppLanguage.ARABIC -> "اختيار وقت التشغيل"
    }

    // ==================== HTML ====================
    val downloadFile: String get() = AiCodingStrings.downloadFile
    
    val exportAll: String get() = AiCodingStrings.exportAll
    
    val thinking: String get() = AiCodingStrings.thinking
    
    val thinkingDots: String get() = AiCodingStrings.thinkingDots
    
    val describeHtmlPage: String get() = AiCodingStrings.describeHtmlPage
    
    val btnSend: String get() = AiCodingStrings.btnSend
    
    val btnRestore: String get() = AiCodingStrings.btnRestore
    
    val fileCountFormat: String get() = AiCodingStrings.fileCountFormat
    
    val linesCount: String get() = AiCodingStrings.linesCount
    
    val filesCountShort: String get() = AiCodingStrings.filesCountShort
    
    val rules: String get() = AiCodingStrings.rules
    
    val selectFromTemplate: String get() = AiCodingStrings.selectFromTemplate
    
    val selectRuleTemplate: String get() = AiCodingStrings.selectRuleTemplate
    
    val noImageModel: String get() = AiCodingStrings.noImageModel
    
    val selectImageModel: String get() = AiCodingStrings.selectImageModel
    
    val configureMoreModels: String get() = AiCodingStrings.configureMoreModels
    
    val projectFiles: String get() = AiCodingStrings.projectFiles
    
    val refresh: String get() = AiCodingStrings.refresh

    val goBack: String get() = AiCodingStrings.goBack

    val goForward: String get() = AiCodingStrings.goForward
    
    val noFilesYet: String get() = AiCodingStrings.noFilesYet
    
    val aiCodeSavedHere: String get() = AiStrings.aiCodeSavedHere
    
    val versionHistory: String get() = AiCodingStrings.versionHistory
    
    val addNewRule: String get() = AiCodingStrings.addNewRule

    // ==================== HTML AI ====================
    val styleModernMinimal: String get() = AiCodingStrings.styleModernMinimal
    val styleModernMinimalDesc: String get() = AiCodingStrings.styleModernMinimalDesc
    val styleGlassmorphism: String get() = AiCodingStrings.styleGlassmorphism
    val styleGlassmorphismDesc: String get() = AiCodingStrings.styleGlassmorphismDesc
    val styleNeumorphism: String get() = AiCodingStrings.styleNeumorphism
    val styleNeumorphismDesc: String get() = AiCodingStrings.styleNeumorphismDesc
    val styleDarkMode: String get() = AiCodingStrings.styleDarkMode
    val styleDarkModeDesc: String get() = AiCodingStrings.styleDarkModeDesc
    val styleCyberpunk: String get() = AiCodingStrings.styleCyberpunk
    val styleCyberpunkDesc: String get() = AiCodingStrings.styleCyberpunkDesc
    val styleGradient: String get() = AiCodingStrings.styleGradient
    val styleGradientDesc: String get() = AiCodingStrings.styleGradientDesc
    val styleMinimal: String get() = AiCodingStrings.styleMinimal
    val styleMinimalDesc: String get() = AiCodingStrings.styleMinimalDesc
    val styleNature: String get() = AiCodingStrings.styleNature
    val styleNatureDesc: String get() = AiCodingStrings.styleNatureDesc
    val styleCuteCartoon: String get() = AiCodingStrings.styleCuteCartoon
    val styleCuteCartoonDesc: String get() = AiCodingStrings.styleCuteCartoonDesc
    val styleNeonGlow: String get() = AiCodingStrings.styleNeonGlow
    val styleNeonGlowDesc: String get() = AiCodingStrings.styleNeonGlowDesc

    // ==================== HTML AI ====================
    val styleHarryPotter: String get() = AiConfigStrings.styleHarryPotter
    val styleHarryPotterDesc: String get() = AiConfigStrings.styleHarryPotterDesc
    val styleGhibli: String get() = AiConfigStrings.styleGhibli
    val styleGhibliDesc: String get() = AiConfigStrings.styleGhibliDesc
    val styleYourName: String get() = AiConfigStrings.styleYourName
    val styleYourNameDesc: String get() = AiConfigStrings.styleYourNameDesc
    val styleApple: String get() = AiConfigStrings.styleApple
    val styleAppleDesc: String get() = AiConfigStrings.styleAppleDesc
    val styleLittlePrince: String get() = AiConfigStrings.styleLittlePrince
    val styleLittlePrinceDesc: String get() = AiConfigStrings.styleLittlePrinceDesc
    val styleZeldaBotw: String get() = AiConfigStrings.styleZeldaBotw
    val styleZeldaBotwDesc: String get() = AiConfigStrings.styleZeldaBotwDesc
    val styleArtDeco: String get() = AiConfigStrings.styleArtDeco
    val styleArtDecoDesc: String get() = AiConfigStrings.styleArtDecoDesc
    val styleJapanese: String get() = AiConfigStrings.styleJapanese
    val styleJapaneseDesc: String get() = AiConfigStrings.styleJapaneseDesc

    // ==================== HTML AI ====================
    val rulesChinese: String get() = when (lang) {
        AppLanguage.CHINESE -> "中文对话"
        AppLanguage.ENGLISH -> "Chinese Dialogue"
        AppLanguage.ARABIC -> "حوار صيني"
    }
    val rulesChineseDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用中文进行所有交流"
        AppLanguage.ENGLISH -> "Use Chinese for all communication"
        AppLanguage.ARABIC -> "استخدم الصينية في جميع الاتصالات"
    }
    val rulesGame: String get() = when (lang) {
        AppLanguage.CHINESE -> "游戏开发"
        AppLanguage.ENGLISH -> "Game Development"
        AppLanguage.ARABIC -> "تطوير الألعاب"
    }
    val rulesGameDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "适合开发小游戏"
        AppLanguage.ENGLISH -> "Suitable for developing mini games"
        AppLanguage.ARABIC -> "مناسب لتطوير الألعاب الصغيرة"
    }
    val rulesAnimation: String get() = when (lang) {
        AppLanguage.CHINESE -> "动画效果"
        AppLanguage.ENGLISH -> "Animation Effects"
        AppLanguage.ARABIC -> "تأثيرات الحركة"
    }
    val rulesAnimationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "注重动画和交互效果"
        AppLanguage.ENGLISH -> "Focus on animation and interaction"
        AppLanguage.ARABIC -> "التركيز على الحركة والتفاعل"
    }
    val rulesForm: String get() = when (lang) {
        AppLanguage.CHINESE -> "表单页面"
        AppLanguage.ENGLISH -> "Form Pages"
        AppLanguage.ARABIC -> "صفحات النماذج"
    }
    val rulesFormDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "适合创建表单和数据收集页面"
        AppLanguage.ENGLISH -> "Suitable for creating forms and data collection pages"
        AppLanguage.ARABIC -> "مناسب لإنشاء النماذج وصفحات جمع البيانات"
    }

    // ==================== ====================
    val countryRegion: String get() = when (lang) {
        AppLanguage.CHINESE -> "国家/地区"
        AppLanguage.ENGLISH -> "Country/Region"
        AppLanguage.ARABIC -> "البلد/المنطقة"
    }
    
    val countryRegionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "如：日本、韩国、英国..."
        AppLanguage.ENGLISH -> "e.g.: Japan, Korea, UK..."
        AppLanguage.ARABIC -> "مثال: اليابان، كوريا، المملكة المتحدة..."
    }

    // ==================== ====================
    val searchCodeSnippets: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索代码块..."
        AppLanguage.ENGLISH -> "Search code snippets..."
        AppLanguage.ARABIC -> "البحث عن مقتطفات الكود..."
    }

    // ==================== Module Editor ====================
    val moduleNameRequired: String get() = ModuleStrings.moduleNameRequired
    
    val inputModuleName: String get() = ModuleStrings.inputModuleName
    
    val editModule: String get() = ModuleStrings.editModule
    
    val useTemplate: String get() = ModuleStrings.useTemplate
    
    val basicInfo: String get() = ModuleStrings.basicInfo
    
    val code: String get() = ModuleStrings.code
    
    val advancedSettings: String get() = ModuleStrings.advancedSettings
    
    val webViewAdvancedConfig: String get() = ModuleStrings.webViewAdvancedConfig
    
    val sectionWebEngine: String get() = ModuleStrings.sectionWebEngine
    
    val sectionContentDisplay: String get() = ModuleStrings.sectionContentDisplay
    
    val sectionNavigation: String get() = ModuleStrings.sectionNavigation
    
    val sectionOfflinePerformance: String get() = ModuleStrings.sectionOfflinePerformance
    
    val sectionDeveloper: String get() = ModuleStrings.sectionDeveloper
    
    val selectCategory: String get() = ModuleStrings.selectCategory
    
    val runTime: String get() = ModuleStrings.runTime
    
    val requiredPermissions: String get() = ModuleStrings.requiredPermissions
    
    val sensitive: String get() = ModuleStrings.sensitive
    
    val confirm: String get() = ModuleStrings.confirm
    
    val category: String get() = ModuleStrings.category
    
    val moduleInfo: String get() = ModuleStrings.moduleInfo
    
    val codeSnippets: String get() = ModuleStrings.codeSnippets
    
    val availableFunctions: String get() = ModuleStrings.availableFunctions
    
    val cssTips: String get() = ModuleStrings.cssTips
    
    val jsFunctionsHint: String get() = ModuleStrings.jsFunctionsHint
    
    val cssHint: String get() = ModuleStrings.cssHint
    
    val javascriptCode: String get() = ModuleStrings.javascriptCode
    
    val cssCode: String get() = ModuleStrings.cssCode
    
    val noSpecialPermissions: String get() = ModuleStrings.noSpecialPermissions
    
    val urlMatchRules: String get() = ModuleStrings.urlMatchRules
    
    val matchAllWebsites: String get() = ModuleStrings.matchAllWebsites
    
    val rulesCount: String get() = ModuleStrings.rulesCount
    
    val userConfigItems: String get() = ModuleStrings.userConfigItems
    
    val noConfigItems: String get() = ModuleStrings.noConfigItems
    
    val configItemsCount: String get() = ModuleStrings.configItemsCount
    
    val developerGuide: String get() = ModuleStrings.developerGuide
    
    val developerGuideContent: String get() = ModuleStrings.developerGuideContent
    
    val regex: String get() = ModuleStrings.regex
    
    val exclude: String get() = ModuleStrings.exclude
    
    val include: String get() = ModuleStrings.include
    
    val description: String get() = ModuleStrings.description
    
    val briefModuleDescription: String get() = ModuleStrings.briefModuleDescription
    
    val tags: String get() = ModuleStrings.tags
    
    val tagsHint: String get() = ModuleStrings.tagsHint
    
    val author: String get() = CommonStrings.author
    
    val yourName: String get() = ModuleStrings.yourName
    
    val keyNameRequired: String get() = ModuleStrings.keyNameRequired
    
    val keyNameHint: String get() = ModuleStrings.keyNameHint
    
    val displayNameRequired: String get() = ModuleStrings.displayNameRequired
    
    val displayNameHint: String get() = ModuleStrings.displayNameHint
    
    val configDescription: String get() = ModuleStrings.configDescription
    
    val configDescriptionHint: String get() = ModuleStrings.configDescriptionHint
    
    val configType: String get() = ModuleStrings.configType
    
    val defaultValue: String get() = ModuleStrings.defaultValue

    // ==================== AI ====================
    val provider: String get() = when (lang) {
        AppLanguage.CHINESE -> "供应商"
        AppLanguage.ENGLISH -> "Provider"
        AppLanguage.ARABIC -> "المزود"
    }
    
    val modelId: String get() = when (lang) {
        AppLanguage.CHINESE -> "模型 ID"
        AppLanguage.ENGLISH -> "Model ID"
        AppLanguage.ARABIC -> "معرف النموذج"
    }
    
    val modelIdHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "例如：gpt-4o-mini"
        AppLanguage.ENGLISH -> "e.g.: gpt-4o-mini"
        AppLanguage.ARABIC -> "مثال: gpt-4o-mini"
    }
    
    val aliasOptional: String get() = when (lang) {
        AppLanguage.CHINESE -> "别名（可选）"
        AppLanguage.ENGLISH -> "Alias (optional)"
        AppLanguage.ARABIC -> "الاسم المستعار (اختياري)"
    }
    
    val alias: String get() = when (lang) {
        AppLanguage.CHINESE -> "别名"
        AppLanguage.ENGLISH -> "Alias"
        AppLanguage.ARABIC -> "الاسم المستعار"
    }

    // ==================== Create App ====================
    val activationCode: String get() = CreateStrings.activationCode
    
    val inputActivationCodeHint: String get() = CreateStrings.inputActivationCodeHint
    
    val customPackageName: String get() = ProjectStrings.customPackageName
    
    val packageNameTooLong: String get() = ProjectStrings.packageNameTooLong
    
    val packageNameInvalidFormat: String get() = ProjectStrings.packageNameInvalidFormat
    
    val packageNameHint: String get() = ProjectStrings.packageNameHint
    
    val apkConfigNote: String get() = ProjectStrings.apkConfigNote
    
    val versionName: String get() = ProjectStrings.versionName
    
    val versionCode: String get() = ProjectStrings.versionCode
    
    val selectTheme: String get() = ProjectStrings.selectTheme
    
    val translateTargetLanguage: String get() = ProjectStrings.translateTargetLanguage
    
    val adBlockRuleHint: String get() = ProjectStrings.adBlockRuleHint
    
    val adBlockDescription: String get() = ProjectStrings.adBlockDescription
    
    val customBlockRules: String get() = ProjectStrings.customBlockRules
    
    val adBlockEnabled: String get() = ProjectStrings.adBlockEnabled
    
    val adBlockDisabled: String get() = ProjectStrings.adBlockDisabled
    
    val adBlockToggleEnabled: String get() = ProjectStrings.adBlockToggleEnabled
    
    val adBlockToggleDescription: String get() = ProjectStrings.adBlockToggleDescription

    // ==================== General ====================
    val done: String get() = UiStrings.done
    
    val edit: String get() = UiStrings.edit
    
    val newUpdate: String get() = UiStrings.newUpdate
    
    val updateNow: String get() = UiStrings.updateNow
    
    val latestVersion: String get() = UiStrings.latestVersion
    
    val networkError: String get() = UiStrings.networkError
    
    val loading: String get() = UiStrings.loading
    
    val noData: String get() = UiStrings.noData
    
    val saved: String get() = UiStrings.saved
    
    val operationSuccess: String get() = UiStrings.operationSuccess
    
    val operationFailed: String get() = UiStrings.operationFailed
    
    val unknownError: String get() = UiStrings.unknownError
    
    val pleaseWait: String get() = UiStrings.pleaseWait
    
    val processing: String get() = UiStrings.processing
    
    val on: String get() = UiStrings.on
    
    val off: String get() = UiStrings.off
    
    val selectFile: String get() = UiStrings.selectFile
    
    val selectFolder: String get() = UiStrings.selectFolder
    
    val fileNotFound: String get() = UiStrings.fileNotFound
    
    val invalidFormat: String get() = UiStrings.invalidFormat
    
    val permissionDenied: String get() = UiStrings.permissionDenied
    
    val grantPermission: String get() = UiStrings.grantPermission

    // ==================== AI Module Dev Help ====================
    val howToUse: String get() = AiConfigStrings.howToUse
    
    val howToUseContent: String get() = AiConfigStrings.howToUseContent
    
    val requirementDescriptionTips: String get() = AiConfigStrings.requirementDescriptionTips
    
    val requirementDescriptionTipsContent: String get() = AiConfigStrings.requirementDescriptionTipsContent
    
    val modelSelection: String get() = AiConfigStrings.modelSelection
    
    val modelSelectionContent: String get() = AiConfigStrings.modelSelectionContent
    
    val categorySelection: String get() = AiConfigStrings.categorySelection
    
    val categorySelectionContent: String get() = AiConfigStrings.categorySelectionContent
    
    val autoCheck: String get() = AiConfigStrings.autoCheck
    
    val autoCheckContent: String get() = AiConfigStrings.autoCheckContent
    
    val codeEditing: String get() = AiConfigStrings.codeEditing
    
    val codeEditingContent: String get() = AiConfigStrings.codeEditingContent
    
    val saveModule: String get() = AiConfigStrings.saveModule
    
    val saveModuleContent: String get() = AiConfigStrings.saveModuleContent

    // ==================== WebView Advanced Settings ====================
    val javaScriptSetting: String get() = WebViewStrings.javaScriptSetting
    
    val javaScriptSettingHint: String get() = WebViewStrings.javaScriptSettingHint
    
    val domStorageSetting: String get() = WebViewStrings.domStorageSetting
    
    val domStorageSettingHint: String get() = WebViewStrings.domStorageSettingHint
    
    val zoomSetting: String get() = WebViewStrings.zoomSetting
    
    val zoomSettingHint: String get() = WebViewStrings.zoomSettingHint
    
    val swipeRefreshSetting: String get() = WebViewStrings.swipeRefreshSetting
    
    val swipeRefreshSettingHint: String get() = WebViewStrings.swipeRefreshSettingHint
    
    val desktopModeSetting: String get() = WebViewStrings.desktopModeSetting
    
    val desktopModeSettingHint: String get() = WebViewStrings.desktopModeSettingHint
    
    val fullscreenVideoSetting: String get() = WebViewStrings.fullscreenVideoSetting
    
    val fullscreenVideoSettingHint: String get() = WebViewStrings.fullscreenVideoSettingHint

    val hideBrowserToolbarLabel: String get() = WebViewStrings.hideBrowserToolbarLabel

    val hideBrowserToolbarHint: String get() = WebViewStrings.hideBrowserToolbarHint

    val externalLinksSetting: String get() = WebViewStrings.externalLinksSetting
    
    val externalLinksSettingHint: String get() = WebViewStrings.externalLinksSettingHint
    
    val crossOriginIsolationSetting: String get() = WebViewStrings.crossOriginIsolationSetting
    
    val crossOriginIsolationSettingHint: String get() = WebViewStrings.crossOriginIsolationSettingHint
    
    // ==================== ====================
    val viewportModeTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "视口适配模式"
        AppLanguage.ENGLISH -> "Viewport Mode"
        AppLanguage.ARABIC -> "وضع منفذ العرض"
    }
    
    val viewportModeDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "解决 Unity/Canvas 游戏显示放大、UI 被裁切的问题"
        AppLanguage.ENGLISH -> "Fix Unity/Canvas games displaying zoomed in with UI clipped"
        AppLanguage.ARABIC -> "إصلاح عرض ألعاب Unity/Canvas المكبرة مع قص واجهة المستخدم"
    }
    
    val viewportModeDefault: String get() = when (lang) {
        AppLanguage.CHINESE -> "标准模式（适合大多数网页）"
        AppLanguage.ENGLISH -> "Standard (for most websites)"
        AppLanguage.ARABIC -> "قياسي (لمعظم المواقع)"
    }
    
    val viewportModeFitScreen: String get() = when (lang) {
        AppLanguage.CHINESE -> "适配屏幕（Unity/Canvas 游戏推荐）"
        AppLanguage.ENGLISH -> "Fit Screen (recommended for Unity/Canvas games)"
        AppLanguage.ARABIC -> "ملاءمة الشاشة (لألعاب Unity/Canvas)"
    }
    
    val viewportModeDesktop: String get() = when (lang) {
        AppLanguage.CHINESE -> "桌面视口（980px 宽度）"
        AppLanguage.ENGLISH -> "Desktop Viewport (980px width)"
        AppLanguage.ARABIC -> "منفذ سطح المكتب (عرض 980 بكسل)"
    }

    val viewportModeCustom: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义视口"
        AppLanguage.ENGLISH -> "Custom Viewport"
        AppLanguage.ARABIC -> "منفذ العرض المخصص"
    }

    val viewportCustomWidth: String get() = when (lang) {
        AppLanguage.CHINESE -> "视口宽度"
        AppLanguage.ENGLISH -> "Viewport Width"
        AppLanguage.ARABIC -> "عرض منفذ العرض"
    }

    val viewportCustomWidthHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入视口宽度（320-3840 像素）"
        AppLanguage.ENGLISH -> "Enter viewport width (320-3840 px)"
        AppLanguage.ARABIC -> "أدخل عرض منفذ العرض (320-3840 بكسل)"
    }

    val viewportCustomWidthPresets: String get() = when (lang) {
        AppLanguage.CHINESE -> "预设宽度"
        AppLanguage.ENGLISH -> "Preset Widths"
        AppLanguage.ARABIC -> "عرض محدد مسبقاً"
    }

    // ==================== Isolation Options ====================
    val fingerprintProtection: String get() = ShellStrings.fingerprintProtection
    
    val networkProtection: String get() = ShellStrings.networkProtection
    
    val advancedOptions: String get() = ShellStrings.advancedOptions
    
    val expand: String get() = ShellStrings.expand
    
    val collapse: String get() = ShellStrings.collapse
    
    val expandAll: String get() = ShellStrings.expandAll
    
    val depClearDone: String get() = ShellStrings.depClearDone
    
    val custom: String get() = ShellStrings.custom
    
    val maximum: String get() = ShellStrings.maximum
    
    val full: String get() = ShellStrings.full
    
    val notEnabled: String get() = ShellStrings.notEnabled
    
    val ipRegion: String get() = ShellStrings.ipRegion
    
    val supportedCountriesHint: String get() = ShellStrings.supportedCountriesHint
    
    val isolationDescription: String get() = ShellStrings.isolationDescription
    
    val canvasProtection: String get() = ShellStrings.canvasProtection
    
    val canvasProtectionHint: String get() = ShellStrings.canvasProtectionHint
    
    val webglProtection: String get() = ShellStrings.webglProtection
    
    val webglProtectionHint: String get() = ShellStrings.webglProtectionHint
    
    val audioProtection: String get() = ShellStrings.audioProtection
    
    val audioProtectionHint: String get() = ShellStrings.audioProtectionHint
    
    val webrtcProtection: String get() = ShellStrings.webrtcProtection
    
    val webrtcProtectionHint: String get() = ShellStrings.webrtcProtectionHint
    
    val headerSpoofing: String get() = ShellStrings.headerSpoofing
    
    val headerSpoofingHint: String get() = ShellStrings.headerSpoofingHint
    
    val ipSpoofing: String get() = ShellStrings.ipSpoofing
    
    val ipSpoofingHint: String get() = ShellStrings.ipSpoofingHint
    
    val randomFingerprint: String get() = ShellStrings.randomFingerprint
    
    val randomFingerprintHint: String get() = ShellStrings.randomFingerprintHint
    
    val fontProtection: String get() = ShellStrings.fontProtection
    
    val fontProtectionHint: String get() = ShellStrings.fontProtectionHint
    
    val storageIsolation: String get() = ShellStrings.storageIsolation
    
    val storageIsolationHint: String get() = ShellStrings.storageIsolationHint
    
    val timezoneSpoofing: String get() = CommonStrings.timezoneSpoofing
    
    val timezoneSpoofingHint: String get() = CommonStrings.timezoneSpoofingHint
    
    val languageSpoofing: String get() = ShellStrings.languageSpoofing
    
    val languageSpoofingHint: String get() = ShellStrings.languageSpoofingHint
    
    val resolutionSpoofing: String get() = ShellStrings.resolutionSpoofing
    
    val resolutionSpoofingHint: String get() = ShellStrings.resolutionSpoofingHint
    
    val regenerateOnLaunch: String get() = ShellStrings.regenerateOnLaunch
    
    val regenerateOnLaunchHint: String get() = ShellStrings.regenerateOnLaunchHint

    // ==================== Encryption Options ====================
    val configFileEncryption: String get() = BuildStrings.configFileEncryption
    
    val configFileEncryptionHint: String get() = BuildStrings.configFileEncryptionHint
    
    val htmlCssJsEncryption: String get() = BuildStrings.htmlCssJsEncryption
    
    val htmlCssJsEncryptionHint: String get() = BuildStrings.htmlCssJsEncryptionHint
    
    val mediaFileEncryption: String get() = BuildStrings.mediaFileEncryption
    
    val mediaFileEncryptionHint: String get() = BuildStrings.mediaFileEncryptionHint
    
    val splashEncryption: String get() = BuildStrings.splashEncryption
    
    val splashEncryptionHint: String get() = BuildStrings.splashEncryptionHint
    
    val bgmEncryption: String get() = BuildStrings.bgmEncryption
    
    val bgmEncryptionHint: String get() = BuildStrings.bgmEncryptionHint
    
    val encryptionStrength: String get() = BuildStrings.encryptionStrength
    
    val securityProtection: String get() = BuildStrings.securityProtection
    
    val integrityCheck: String get() = BuildStrings.integrityCheck
    
    val integrityCheckHint: String get() = BuildStrings.integrityCheckHint
    
    val antiDebugProtection: String get() = BuildStrings.antiDebugProtection
    
    val antiDebugProtectionHint: String get() = BuildStrings.antiDebugProtectionHint
    
    val antiTamperProtection: String get() = BuildStrings.antiTamperProtection
    
    val antiTamperProtectionHint: String get() = BuildStrings.antiTamperProtectionHint
    
    val stringObfuscation: String get() = BuildStrings.stringObfuscation
    
    val stringObfuscationHint: String get() = BuildStrings.stringObfuscationHint
    
    val securityWarning: String get() = BuildStrings.securityWarning
    
    val encryptionDescription: String get() = BuildStrings.encryptionDescription
    
    val pbkdf2Iterations: String get() = BuildStrings.pbkdf2Iterations

    // ==================== ====================
    val retryAction: String get() = AiCodingStrings.retryAction
    
    val retryActionHint: String get() = AiCodingStrings.retryActionHint
    
    val retryWithDifferentModel: String get() = AiCodingStrings.retryWithDifferentModel
    
    val retryWithDifferentModelHint: String get() = AiCodingStrings.retryWithDifferentModelHint
    
    val showRawResponse: String get() = AiCodingStrings.showRawResponse
    
    val showRawResponseHint: String get() = AiCodingStrings.showRawResponseHint
    
    val goToSettings: String get() = AiCodingStrings.goToSettings
    
    val goToSettingsHint: String get() = AiCodingStrings.goToSettingsHint
    
    val manualEdit: String get() = AiCodingStrings.manualEdit
    
    val manualEditHint: String get() = AiCodingStrings.manualEditHint
    
    val dismissAction: String get() = AiCodingStrings.dismissAction
    
    val dismissActionHint: String get() = AiCodingStrings.dismissActionHint

    // ==================== ====================
    val lightModePreview: String get() = when (lang) {
        AppLanguage.CHINESE -> "浅色模式"
        AppLanguage.ENGLISH -> "Light Mode"
        AppLanguage.ARABIC -> "الوضع الفاتح"
    }
    
    val darkModePreview: String get() = when (lang) {
        AppLanguage.CHINESE -> "深色模式"
        AppLanguage.ENGLISH -> "Dark Mode"
        AppLanguage.ARABIC -> "الوضع الداكن"
    }

    // ==================== About Page ====================
    val communityGroup: String get() = CommunityStrings.communityGroup
    
    val openSourceRepository: String get() = CommonStrings.openSourceRepository
    
    val videoTutorialLabel: String get() = CommonStrings.videoTutorialLabel
    
    val okButton: String get() = CommonStrings.okButton
    
    val updateLaterButton: String get() = CommonStrings.updateLaterButton

    // ==================== ====================
    val frameworkLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "框架"
        AppLanguage.ENGLISH -> "Framework"
        AppLanguage.ARABIC -> "إطار العمل"
    }
    
    val versionLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本"
        AppLanguage.ENGLISH -> "Version"
        AppLanguage.ARABIC -> "الإصدار"
    }
    
    val packageManagerLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "包管理器"
        AppLanguage.ENGLISH -> "Package Manager"
        AppLanguage.ARABIC -> "مدير الحزم"
    }
    
    val dependencyCountLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "依赖数量"
        AppLanguage.ENGLISH -> "Dependency Count"
        AppLanguage.ARABIC -> "عدد التبعيات"
    }
    
    val outputDirLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "输出目录"
        AppLanguage.ENGLISH -> "Output Directory"
        AppLanguage.ARABIC -> "دليل الإخراج"
    }
    
    val dependencyCountValue: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个"
        AppLanguage.ENGLISH -> "%d"
        AppLanguage.ARABIC -> "%d"
    }
    
    // ==================== Module Editor Extras ====================
    val urlPattern: String get() = ExtensionStrings.urlPattern
    
    val regexExpression: String get() = ExtensionStrings.regexExpression
    
    val excludeRule: String get() = ExtensionStrings.excludeRule
    
    val noConfigItemsHint: String get() = ExtensionStrings.noConfigItemsHint
    
    val addConfigItem: String get() = ExtensionStrings.addConfigItem
    
    val keyNamePlaceholder: String get() = ExtensionStrings.keyNamePlaceholder
    
    val displayNamePlaceholder: String get() = ExtensionStrings.displayNamePlaceholder
    
    val explanationLabel: String get() = ExtensionStrings.explanationLabel
    
    val configExplanationPlaceholder: String get() = ExtensionStrings.configExplanationPlaceholder
    
    val typeLabel: String get() = ExtensionStrings.typeLabel
    
    val defaultValueLabel: String get() = ExtensionStrings.defaultValueLabel
    
    val requiredField: String get() = ExtensionStrings.requiredField
    
    val selectTemplate: String get() = ExtensionStrings.selectTemplate
    
    val jsCodePlaceholder: String get() = ExtensionStrings.jsCodePlaceholder
    
    val cssCodePlaceholder: String get() = ExtensionStrings.cssCodePlaceholder
    
    // ==================== About Page Extras ====================
    val authorTagline: String get() = CommonStrings.authorTagline
    
    val joinCommunityGroup: String get() = UiStrings.joinCommunityGroup
    
    val communityGroupDescription: String get() = CommunityStrings.communityGroupDescription
    
    val contactAuthorDescription: String get() = UiStrings.contactAuthorDescription
    
    val welcomeStarSupport: String get() = UiStrings.welcomeStarSupport
    
    val changelog: String get() = UiStrings.changelog
    
    val latestTag: String get() = UiStrings.latestTag
    
    val newVersionFound: String get() = UiStrings.newVersionFound
    
    val updateRecommendation: String get() = UiStrings.updateRecommendation
    
    val currentVersionIs: String get() = UiStrings.currentVersionIs
    
    val openAction: String get() = UiStrings.openAction
    
    val qqGroupLabel: String get() = UiStrings.qqGroupLabel
    
    val telegramGroupLabel: String get() = UiStrings.telegramGroupLabel
    
    val exchangeLearningUpdates: String get() = UiStrings.exchangeLearningUpdates
    
    val internationalUserGroup: String get() = UiStrings.internationalUserGroup
    
    val feedbackConsultation: String get() = UiStrings.feedbackConsultation
    
    val internationalAccess: String get() = UiStrings.internationalAccess
    
    
    val authorAvatar: String get() = CommonStrings.authorAvatar
    
    // ==================== AI Module Builder ====================
    val aiModuleDeveloperTitle: String get() = AiStrings.aiModuleDeveloperTitle
    
    val restart: String get() = AiStrings.restart
    
    val aiAssistant: String get() = AiStrings.aiAssistant
    
    val aiAssistantDesc: String get() = AiStrings.aiAssistantDesc
    
    val syntaxCheck: String get() = AiStrings.syntaxCheck
    
    val securityScan: String get() = AiStrings.securityScan
    
    val autoFix: String get() = AiStrings.autoFix
    
    val codeTemplate: String get() = AiStrings.codeTemplate
    
    val instantTest: String get() = AiStrings.instantTest
    
    val tryTheseExamples: String get() = AiStrings.tryTheseExamples
    
    val exampleBlockAds: String get() = AiStrings.exampleBlockAds
    
    val exampleDarkMode: String get() = AiStrings.exampleDarkMode
    
    val exampleAutoScroll: String get() = AiStrings.exampleAutoScroll
    
    val exampleUnlockCopy: String get() = AiStrings.exampleUnlockCopy
    
    val exampleVideoSpeed: String get() = AiStrings.exampleVideoSpeed
    
    val exampleBackToTop: String get() = AiStrings.exampleBackToTop
    
    val statusAnalyzing: String get() = AiStrings.statusAnalyzing
    
    val statusPlanning: String get() = AiStrings.statusPlanning
    
    val statusExecuting: String get() = AiStrings.statusExecuting
    
    val statusGenerating: String get() = AiStrings.statusGenerating
    
    val statusReviewing: String get() = AiStrings.statusReviewing
    
    val statusFixing: String get() = AiStrings.statusFixing
    
    val statusProcessing: String get() = AiStrings.statusProcessing
    
    val statusChecking: String get() = AiStrings.statusChecking
    
    val statusScanning: String get() = AiStrings.statusScanning
    
    val syntaxCheckingStatus: String get() = AiStrings.syntaxCheckingStatus
    
    val fixingIssuesStatus: String get() = AiStrings.fixingIssuesStatus
    
    val securityScanningStatus: String get() = AiStrings.securityScanningStatus
    
    val codeModifiedHint: String get() = AiStrings.codeModifiedHint
    
    val secureStatus: String get() = AiStrings.secureStatus
    
    val analyzingRequirements: String get() = AiStrings.analyzingRequirements
    
    val planningDevelopment: String get() = AiStrings.planningDevelopment
    
    val executingToolCalls: String get() = AiStrings.executingToolCalls
    
    val generatingCodeStatus: String get() = AiStrings.generatingCodeStatus
    
    val reviewingCodeQuality: String get() = AiStrings.reviewingCodeQuality
    
    val fixingDetectedIssues: String get() = AiStrings.fixingDetectedIssues
    
    val categoryLabel: String get() = AiStrings.categoryLabel
    
    val autoDetectCategory: String get() = AiStrings.autoDetectCategory
    
    val inputPlaceholder: String get() = AiStrings.inputPlaceholder
    
    val startDevelopment: String get() = AiStrings.startDevelopment
    
    // ==================== BGM Picker ====================
    val selectBgm: String get() = MusicStrings.selectBgm
    
    val selectedMusic: String get() = MusicStrings.selectedMusic
    
    val availableMusic: String get() = MusicStrings.availableMusic
    
    val uploadMusic: String get() = MusicStrings.uploadMusic
    
    val clickArrowToReorder: String get() = MusicStrings.clickArrowToReorder
    
    val noMusicAvailable: String get() = MusicStrings.noMusicAvailable
    
    val clickToUploadMusic: String get() = MusicStrings.clickToUploadMusic
    
    val noMusicWithTag: String get() = MusicStrings.noMusicWithTag
    
    val playMode: String get() = MusicStrings.playMode
    
    val loopMode: String get() = MusicStrings.loopMode
    
    val sequentialMode: String get() = MusicStrings.sequentialMode
    
    val shuffleMode: String get() = MusicStrings.shuffleMode
    
    val volume: String get() = MusicStrings.volume
    
    val showLyrics: String get() = MusicStrings.showLyrics
    
    val lyricsTheme: String get() = MusicStrings.lyricsTheme
    
    val allTag: String get() = MusicStrings.allTag
    
    val lyricsSaved: String get() = MusicStrings.lyricsSaved
    
    // ==================== AI Module Builder ====================
    val syntaxCorrect: String get() = when (lang) {
        AppLanguage.CHINESE -> "语法正确"
        AppLanguage.ENGLISH -> "Syntax Correct"
        AppLanguage.ARABIC -> "بناء الجملة صحيح"
    }
    
    val safe: String get() = when (lang) {
        AppLanguage.CHINESE -> "安全"
        AppLanguage.ENGLISH -> "Safe"
        AppLanguage.ARABIC -> "آمن"
    }
    
    val moduleGeneratedSuccess: String get() = ModuleStrings.moduleGeneratedSuccess
    
    val developmentFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "开发失败"
        AppLanguage.ENGLISH -> "Development Failed"
        AppLanguage.ARABIC -> "فشل التطوير"
    }
    
    val lines: String get() = when (lang) {
        AppLanguage.CHINESE -> "行"
        AppLanguage.ENGLISH -> "lines"
        AppLanguage.ARABIC -> "سطر"
    }
    
    val requirementTips: String get() = when (lang) {
        AppLanguage.CHINESE -> "需求描述技巧"
        AppLanguage.ENGLISH -> "Requirement Description Tips"
        AppLanguage.ARABIC -> "نصائح وصف المتطلبات"
    }
    
    val requirementTipsContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "• 描述具体的功能效果\n• 说明目标网站或页面类型\n• 可以参考示例需求的写法"
        AppLanguage.ENGLISH -> "• Describe specific feature effects\n• Specify target website or page type\n• Refer to example requirements for guidance"
        AppLanguage.ARABIC -> "• صف تأثيرات الميزة المحددة\n• حدد الموقع أو نوع الصفحة المستهدف\n• راجع أمثلة المتطلبات للإرشاد"
    }
    
    val saveModuleTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存模块"
        AppLanguage.ENGLISH -> "Save Module"
        AppLanguage.ARABIC -> "حفظ الوحدة"
    }
    
    val notes: String get() = when (lang) {
        AppLanguage.CHINESE -> "注意事项"
        AppLanguage.ENGLISH -> "Notes"
        AppLanguage.ARABIC -> "ملاحظات"
    }
    
    val notesContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "• 需要配置 AI API 密钥才能使用\n• 复杂功能可能需要多次调整\n• 建议在测试页面验证效果"
        AppLanguage.ENGLISH -> "• AI API key configuration required\n• Complex features may need multiple adjustments\n• Recommend testing on test pages"
        AppLanguage.ARABIC -> "• مطلوب تكوين مفتاح API للذكاء الاصطناعي\n• قد تحتاج الميزات المعقدة إلى تعديلات متعددة\n• يوصى بالاختبار على صفحات الاختبار"
    }
    
    // ==================== BGM Extras ====================
    val previewLyrics: String get() = MusicStrings.previewLyrics
    
    val hasLyrics: String get() = MusicStrings.hasLyrics
    
    val aiGenerateLyrics: String get() = AiStrings.aiGenerateLyrics
    
    val editTags: String get() = MusicStrings.editTags
    
    val stop: String get() = MusicStrings.stop
    
    val moveUp: String get() = MusicStrings.moveUp
    
    val moveDown: String get() = MusicStrings.moveDown
    
    val presetMusic: String get() = MusicStrings.presetMusic
    
    val userUploaded: String get() = MusicStrings.userUploaded
    
    val uploadMusicTitle: String get() = MusicStrings.uploadMusicTitle
    
    val musicName: String get() = MusicStrings.musicName
    
    val selectMusic: String get() = MusicStrings.selectMusic
    
    val selectCoverOptional: String get() = MusicStrings.selectCoverOptional
    
    val coverTip: String get() = MusicStrings.coverTip
    
    val upload: String get() = MusicStrings.upload
    
    val editTagsTitle: String get() = MusicStrings.editTagsTitle
    
    val selectTagsHint: String get() = MusicStrings.selectTagsHint
    
    val selectLyricsTheme: String get() = MusicStrings.selectLyricsTheme
    
    val selectLyricsThemeHint: String get() = MusicStrings.selectLyricsThemeHint
    
    val sampleLyricsText: String get() = MusicStrings.sampleLyricsText
    
    val lyricsPreview: String get() = MusicStrings.lyricsPreview
    
    val lyricsUpdated: String get() = MusicStrings.lyricsUpdated
    
    val backward10s: String get() = MusicStrings.backward10s
    
    val forward10s: String get() = MusicStrings.forward10s
    
    // ==================== AI Settings ====================
    val free: String get() = when (lang) {
        AppLanguage.CHINESE -> "免费"
        AppLanguage.ENGLISH -> "Free"
        AppLanguage.ARABIC -> "مجاني"
    }
    
    val selectedCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "已选 %d 个功能"
        AppLanguage.ENGLISH -> "%d features selected"
        AppLanguage.ARABIC -> "تم اختيار %d ميزات"
    }
    
    val collapseExpand: String get() = when (lang) {
        AppLanguage.CHINESE -> "收起/展开"
        AppLanguage.ENGLISH -> "Collapse/Expand"
        AppLanguage.ARABIC -> "طي/توسيع"
    }
    
    val selectCapabilitiesForFeatures: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择此能力可用于哪些功能："
        AppLanguage.ENGLISH -> "Select which features this capability can be used for:"
        AppLanguage.ARABIC -> "اختر الميزات التي يمكن استخدام هذه القدرة لها:"
    }
    
    val selectAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "全选"
        AppLanguage.ENGLISH -> "Select All"
        AppLanguage.ARABIC -> "تحديد الكل"
    }
    
    // ==================== Changelog ====================
    // v1.9.5
    val cookiesPersistenceFeature: String get() = CommonStrings.cookiesPersistenceFeature
    
    val multiApiKeyManagement: String get() = CommonStrings.multiApiKeyManagement
    
    val modelNameSearchFeature: String get() = CommonStrings.modelNameSearchFeature
    
    val hideUrlPreviewFeature: String get() = CommonStrings.hideUrlPreviewFeature
    
    val popupBlockerFeature: String get() = CommonStrings.popupBlockerFeature
    
    val optimizeCustomApiEndpoint: String get() = CommonStrings.optimizeCustomApiEndpoint
    
    val optimizeModelNameDisplay: String get() = CommonStrings.optimizeModelNameDisplay
    
    val optimizeMultiLanguageAdaptation: String get() = CommonStrings.optimizeMultiLanguageAdaptation
    
    val fixGalleryBuildPath: String get() = CommonStrings.fixGalleryBuildPath
    
    val fixMicrophonePermission: String get() = CommonStrings.fixMicrophonePermission
    
    val fixZoomPropertyNotWorking: String get() = CommonStrings.fixZoomPropertyNotWorking
    
    val fixActivationCodeLanguage: String get() = CommonStrings.fixActivationCodeLanguage
    
    val fixFrontendGalleryFilename: String get() = CommonStrings.fixFrontendGalleryFilename
    
    val fixCoreConfigEditAppType: String get() = CommonStrings.fixCoreConfigEditAppType
    
    val fixKeyboardInitIssue: String get() = CommonStrings.fixKeyboardInitIssue
    
    // v1.9.0
    val browserEngineFeature: String get() = CommonStrings.browserEngineFeature
    
    val browserSpoofingFeature: String get() = CommonStrings.browserSpoofingFeature
    
    val hostsBlockFeature: String get() = CommonStrings.hostsBlockFeature
    
    val longPressMenuFeature: String get() = CommonStrings.longPressMenuFeature
    
    val apkArchitectureFeature: String get() = CommonStrings.apkArchitectureFeature
    
    val mediaGalleryFeature: String get() = CommonStrings.mediaGalleryFeature
    
    val optimizeExtensionModule: String get() = CommonStrings.optimizeExtensionModule
    
    val optimizeEnglishArabicTranslation: String get() = CommonStrings.optimizeEnglishArabicTranslation
    
    val optimizeThemeInteraction: String get() = CommonStrings.optimizeThemeInteraction
    
    val optimizeApiConfigTest: String get() = CommonStrings.optimizeApiConfigTest
    
    val fixAppNameSpaces: String get() = CommonStrings.fixAppNameSpaces
    
    val fixAnnouncementJump: String get() = CommonStrings.fixAnnouncementJump
    
    val fixExternalBrowserCrash: String get() = CommonStrings.fixExternalBrowserCrash
    
    val fixDownloadError: String get() = CommonStrings.fixDownloadError
    
    val fixModuleEditCrash: String get() = CommonStrings.fixModuleEditCrash
    
    val fixAiImageInvalid: String get() = CommonStrings.fixAiImageInvalid
    
    val fixDownloaderPlayerCooperation: String get() = CommonStrings.fixDownloaderPlayerCooperation
    
    // v1.8.5
    val appCategoryFeature: String get() = CommonStrings.appCategoryFeature
    
    val faviconFetchFeature: String get() = CommonStrings.faviconFetchFeature
    
    val randomAppNameFeature: String get() = CommonStrings.randomAppNameFeature
    
    val multiAppIconFeature: String get() = CommonStrings.multiAppIconFeature
    
    val optimizeDataBackup: String get() = CommonStrings.optimizeDataBackup
    
    val optimizeBlackTech: String get() = CommonStrings.optimizeBlackTech
    
    val fixElementBlocker: String get() = CommonStrings.fixElementBlocker
    
    val fixBackgroundRunCrash: String get() = CommonStrings.fixBackgroundRunCrash
    
    val fixI18nStringAdaptation: String get() = CommonStrings.fixI18nStringAdaptation
    
    // v1.8.0
    val multiLanguageSupport: String get() = CommonStrings.multiLanguageSupport
    
    val shareApkFeature: String get() = CommonStrings.shareApkFeature
    
    val elementBlockerModule: String get() = CommonStrings.elementBlockerModule
    
    val forcedRunFeature: String get() = CommonStrings.forcedRunFeature
    
    val linuxOneClickBuild: String get() = CommonStrings.linuxOneClickBuild
    
    val frontendFrameworkToApk: String get() = CommonStrings.frontendFrameworkToApk
    
    val optimizeThemeFeature: String get() = CommonStrings.optimizeThemeFeature
    
    val optimizeAboutPageUi: String get() = CommonStrings.optimizeAboutPageUi
    
    val fixFullscreenStatusBarIssue: String get() = CommonStrings.fixFullscreenStatusBarIssue
    
    val fixDeviceCrashIssue: String get() = CommonStrings.fixDeviceCrashIssue
    
    // v1.7.7
    val statusBarStyleConfig: String get() = CommonStrings.statusBarStyleConfig
    
    val apkEncryptionProtection: String get() = CommonStrings.apkEncryptionProtection
    
    val bootAutoStartAndScheduled: String get() = CommonStrings.bootAutoStartAndScheduled
    
    val dataBackupExportImport: String get() = CommonStrings.dataBackupExportImport
    
    val fullscreenStatusBarOverlay: String get() = CommonStrings.fullscreenStatusBarOverlay
    
    val fullscreenShowStatusBar: String get() = CommonStrings.fullscreenShowStatusBar
    
    val fixHtmlLongPressCopy: String get() = CommonStrings.fixHtmlLongPressCopy
    
    val supportAndroid6: String get() = CommonStrings.supportAndroid6
    
    val fixHtmlStatusBar: String get() = CommonStrings.fixHtmlStatusBar
    
    val fixEmptyAppName: String get() = CommonStrings.fixEmptyAppName
    
    val fixAiModuleCodeOverlay: String get() = CommonStrings.fixAiModuleCodeOverlay
    
    val fixAiHtmlToolCallFailed: String get() = CommonStrings.fixAiHtmlToolCallFailed
    
    val optimizeAiHtmlPrompt: String get() = CommonStrings.optimizeAiHtmlPrompt
    
    val statusBarFollowTheme: String get() = CommonStrings.statusBarFollowTheme
    
    val customStatusBarBgColor: String get() = CommonStrings.customStatusBarBgColor
    
    val fixStatusBarTextVisibility: String get() = CommonStrings.fixStatusBarTextVisibility
    
    val fixJsFileSelectorCompat: String get() = CommonStrings.fixJsFileSelectorCompat
    
    val fixVideoFullscreenRotation: String get() = CommonStrings.fixVideoFullscreenRotation
    
    val fixXhsImageSave: String get() = CommonStrings.fixXhsImageSave
    
    val newXhsImageDownloader: String get() = CommonStrings.newXhsImageDownloader
    
    val fixBlobExportFailed: String get() = CommonStrings.fixBlobExportFailed
    
    val fixHtmlCssJsNotWorking: String get() = CommonStrings.fixHtmlCssJsNotWorking
    
    val fixTaskListDuplicateName: String get() = CommonStrings.fixTaskListDuplicateName
    
    val fixKnownIssues: String get() = CommonStrings.fixKnownIssues
    
    val optimizeAiAgentArch: String get() = CommonStrings.optimizeAiAgentArch
    
    val extensionModuleSystem: String get() = ModuleStrings.extensionModuleSystem
    
    val aiModuleDeveloperAgent: String get() = AiStrings.aiModuleDeveloperAgent
    
    val aiIconGeneration: String get() = AiStrings.aiIconGeneration
    
    val onlineMusicSearch: String get() = CommonStrings.onlineMusicSearch
    
    val announcementTemplates: String get() = CreateStrings.announcementTemplates
    
    val webAutoTranslate: String get() = CommonStrings.webAutoTranslate
    
    val aiHtmlCodingFeature: String get() = AiStrings.aiHtmlCodingFeature
    
    val htmlAppFeature: String get() = CommonStrings.htmlAppFeature
    
    val themeSystemFeature: String get() = CommonStrings.themeSystemFeature
    
    val bgmLrcFeature: String get() = CommonStrings.bgmLrcFeature
    
    val aiSettingsFeature: String get() = AiStrings.aiSettingsFeature
    
    val mediaAppFeature: String get() = CommonStrings.mediaAppFeature
    
    val userScriptInjection: String get() = CommonStrings.userScriptInjection
    
    val splashScreenFeature: String get() = CommonStrings.splashScreenFeature
    
    val videoTrimFeature: String get() = CommonStrings.videoTrimFeature
    
    val fixShortcutIconError: String get() = CommonStrings.fixShortcutIconError
    
    val fullscreenModeFeature: String get() = CommonStrings.fullscreenModeFeature
    
    val fixApkIconCrop: String get() = CommonStrings.fixApkIconCrop
    
    val fixReleaseIconNotWorking: String get() = CommonStrings.fixReleaseIconNotWorking
    
    val fixApkPackageConflict: String get() = CommonStrings.fixApkPackageConflict
    
    val oneClickBuildApk: String get() = CommonStrings.oneClickBuildApk
    
    val appModifierFeature: String get() = CommonStrings.appModifierFeature
    
    val cloneInstallFeature: String get() = CommonStrings.cloneInstallFeature
    
    val desktopModeFeature: String get() = CommonStrings.desktopModeFeature
    
    // ==================== ====================
    
    // ==================== ====================
    
    val networkRequestFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "网络请求失败"
        AppLanguage.ENGLISH -> "Network request failed"
        AppLanguage.ARABIC -> "فشل طلب الشبكة"
    }
    
    val versionInfoNotFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "未找到版本信息"
        AppLanguage.ENGLISH -> "Version info not found"
        AppLanguage.ARABIC -> "لم يتم العثور على معلومات الإصدار"
    }
    
    val webToAppUpdate: String get() = when (lang) {
        AppLanguage.CHINESE -> "WebToApp 更新"
        AppLanguage.ENGLISH -> "WebToApp Update"
        AppLanguage.ARABIC -> "تحديث WebToApp"
    }
    
    val downloadingVersion: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在下载 %s ..."
        AppLanguage.ENGLISH -> "Downloading %s ..."
        AppLanguage.ARABIC -> "جاري تنزيل %s ..."
    }
    
    // ==================== ====================
    val aiIcon: String get() = AiStrings.aiIcon
    
    val icon: String get() = when (lang) {
        AppLanguage.CHINESE -> "图标"
        AppLanguage.ENGLISH -> "Icon"
        AppLanguage.ARABIC -> "أيقونة"
    }
    
    // ==================== AI ====================
    val aiModuleDevelopment: String get() = AiStrings.aiModuleDevelopment
    
    // ==================== ====================
    val availableFor: String get() = when (lang) {
        AppLanguage.CHINESE -> "可用于"
        AppLanguage.ENGLISH -> "Available for"
        AppLanguage.ARABIC -> "متاح لـ"
    }
    
    // ==================== Changelog ====================
    val materialDesign3UI: String get() = when (lang) {
        AppLanguage.CHINESE -> "Material Design 3 界面"
        AppLanguage.ENGLISH -> "Material Design 3 UI"
        AppLanguage.ARABIC -> "واجهة Material Design 3"
    }
    
    val initialVersionRelease: String get() = when (lang) {
        AppLanguage.CHINESE -> "初始版本发布"
        AppLanguage.ENGLISH -> "Initial version release"
        AppLanguage.ARABIC -> "إصدار النسخة الأولية"
    }
    
    val urlToShortcutBasic: String get() = when (lang) {
        AppLanguage.CHINESE -> "URL转快捷方式基本功能"
        AppLanguage.ENGLISH -> "URL to shortcut basic functionality"
        AppLanguage.ARABIC -> "وظيفة تحويل URL إلى اختصار الأساسية"
    }
    
    val activationCodeAnnouncementAdBlock: String get() = CreateStrings.activationCodeAnnouncementAdBlock
    
    // ==================== ====================
    
    val savedToGallery: String get() = when (lang) {
        AppLanguage.CHINESE -> "%s已保存到相册"
        AppLanguage.ENGLISH -> "%s saved to gallery"
        AppLanguage.ARABIC -> "تم حفظ %s في المعرض"
    }
    
    // ==================== ====================
    val codeBlockLibrary: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码块库"
        AppLanguage.ENGLISH -> "Code Block Library"
        AppLanguage.ARABIC -> "مكتبة كتل الكود"
    }
    
    val searchCodeBlocks: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索代码块..."
        AppLanguage.ENGLISH -> "Search code blocks..."
        AppLanguage.ARABIC -> "البحث عن كتل الكود..."
    }
    
    val hotTag: String get() = when (lang) {
        AppLanguage.CHINESE -> "🔥 热门"
        AppLanguage.ENGLISH -> "🔥 Hot"
        AppLanguage.ARABIC -> "🔥 شائع"
    }
    
    val insertCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "插入代码"
        AppLanguage.ENGLISH -> "Insert Code"
        AppLanguage.ARABIC -> "إدراج الكود"
    }
    
    val browseAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "浏览全部"
        AppLanguage.ENGLISH -> "Browse All"
        AppLanguage.ARABIC -> "تصفح الكل"
    }
    
    // ==================== ====================
    
    val enterSchemeName: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入方案名称"
        AppLanguage.ENGLISH -> "Enter scheme name"
        AppLanguage.ARABIC -> "أدخل اسم المخطط"
    }
    
    val briefDescribeScheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "简要描述方案用途"
        AppLanguage.ENGLISH -> "Briefly describe scheme purpose"
        AppLanguage.ARABIC -> "وصف موجز لغرض المخطط"
    }
    
    // ==================== ====================
    val pleaseActivateApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "请先激活应用"
        AppLanguage.ENGLISH -> "Please activate the app first"
        AppLanguage.ARABIC -> "يرجى تفعيل التطبيق أولاً"
    }
    
    val enterActivationCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入激活码"
        AppLanguage.ENGLISH -> "Enter Activation Code"
        AppLanguage.ARABIC -> "أدخل رمز التفعيل"
    }
    
    val enterCodeToContinue: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入激活码以继续使用"
        AppLanguage.ENGLISH -> "Please enter activation code to continue"
        AppLanguage.ARABIC -> "يرجى إدخال رمز التفعيل للمتابعة"
    }
    
    // ==================== ====================
    
    // ==================== ====================
    val startTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动时间"
        AppLanguage.ENGLISH -> "Start Time"
        AppLanguage.ARABIC -> "وقت البدء"
    }
    
    val selectStartTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择启动时间"
        AppLanguage.ENGLISH -> "Select Start Time"
        AppLanguage.ARABIC -> "اختيار وقت البدء"
    }
    
    // ==================== LRC ====================
    
    // ==================== Theme ====================
    val themeAurora: String get() = CommonStrings.themeAurora
    
    val themeAuroraDesc: String get() = CommonStrings.themeAuroraDesc
    
    val themeCyberpunk: String get() = CommonStrings.themeCyberpunk
    
    val themeCyberpunkDesc: String get() = CommonStrings.themeCyberpunkDesc
    
    val themeSakura: String get() = CommonStrings.themeSakura
    
    val themeSakuraDesc: String get() = CommonStrings.themeSakuraDesc
    
    val themeOcean: String get() = CommonStrings.themeOcean
    
    val themeOceanDesc: String get() = CommonStrings.themeOceanDesc
    
    val themeForest: String get() = CommonStrings.themeForest
    
    val themeForestDesc: String get() = CommonStrings.themeForestDesc
    
    val themeGalaxy: String get() = CommonStrings.themeGalaxy
    
    val themeGalaxyDesc: String get() = CommonStrings.themeGalaxyDesc
    
    val themeVolcano: String get() = CommonStrings.themeVolcano
    
    val themeVolcanoDesc: String get() = CommonStrings.themeVolcanoDesc
    
    val themeFrost: String get() = CommonStrings.themeFrost
    
    val themeFrostDesc: String get() = CommonStrings.themeFrostDesc
    
    val themeSunset: String get() = CommonStrings.themeSunset
    
    val themeSunsetDesc: String get() = CommonStrings.themeSunsetDesc
    
    val themeMinimal: String get() = CommonStrings.themeMinimal
    
    val themeMinimalDesc: String get() = CommonStrings.themeMinimalDesc
    
    val themeNeonTokyo: String get() = CommonStrings.themeNeonTokyo
    
    val themeNeonTokyoDesc: String get() = CommonStrings.themeNeonTokyoDesc
    
    val themeLavender: String get() = CommonStrings.themeLavender
    
    val themeLavenderDesc: String get() = CommonStrings.themeLavenderDesc
    
    val themeKimiNoNawa: String get() = CommonStrings.themeKimiNoNawa
    
    val themeKimiNoNawaDesc: String get() = CommonStrings.themeKimiNoNawaDesc
    
    val themeAnohana: String get() = CommonStrings.themeAnohana
    
    val themeAnohanaDesc: String get() = CommonStrings.themeAnohanaDesc
    
    val themeDeathNote: String get() = CommonStrings.themeDeathNote
    
    val themeDeathNoteDesc: String get() = CommonStrings.themeDeathNoteDesc
    
    val themeNaruto: String get() = CommonStrings.themeNaruto
    
    val themeNarutoDesc: String get() = CommonStrings.themeNarutoDesc
    
    val themeOnePiece: String get() = CommonStrings.themeOnePiece
    
    val themeOnePieceDesc: String get() = CommonStrings.themeOnePieceDesc
    
    val themeBoonieBears: String get() = CommonStrings.themeBoonieBears
    
    val themeBoonieBearDesc: String get() = CommonStrings.themeBoonieBearDesc
    
    val themeTomAndJerry: String get() = CommonStrings.themeTomAndJerry
    
    val themeTomAndJerryDesc: String get() = CommonStrings.themeTomAndJerryDesc
    
    val themeZarathustra: String get() = CommonStrings.themeZarathustra
    
    val themeZarathustraDesc: String get() = CommonStrings.themeZarathustraDesc
    
    val themeWillAndRepresentation: String get() = CommonStrings.themeWillAndRepresentation
    
    val themeWillAndRepresentationDesc: String get() = CommonStrings.themeWillAndRepresentationDesc
    
    // ==================== ====================
    val animSmooth: String get() = when (lang) {
        AppLanguage.CHINESE -> "丝滑流畅"
        AppLanguage.ENGLISH -> "Smooth"
        AppLanguage.ARABIC -> "سلس"
    }
    
    val animBouncy: String get() = when (lang) {
        AppLanguage.CHINESE -> "弹性活力"
        AppLanguage.ENGLISH -> "Bouncy"
        AppLanguage.ARABIC -> "مرن"
    }
    
    val animSnappy: String get() = when (lang) {
        AppLanguage.CHINESE -> "干脆利落"
        AppLanguage.ENGLISH -> "Snappy"
        AppLanguage.ARABIC -> "سريع"
    }
    
    val animElegant: String get() = when (lang) {
        AppLanguage.CHINESE -> "优雅缓慢"
        AppLanguage.ENGLISH -> "Elegant"
        AppLanguage.ARABIC -> "أنيق"
    }
    
    val animPlayful: String get() = when (lang) {
        AppLanguage.CHINESE -> "俏皮跳跃"
        AppLanguage.ENGLISH -> "Playful"
        AppLanguage.ARABIC -> "مرح"
    }
    
    val animDramatic: String get() = when (lang) {
        AppLanguage.CHINESE -> "戏剧张力"
        AppLanguage.ENGLISH -> "Dramatic"
        AppLanguage.ARABIC -> "درامي"
    }
    
    // ==================== ====================
    val interRipple: String get() = when (lang) {
        AppLanguage.CHINESE -> "水波涟漪"
        AppLanguage.ENGLISH -> "Ripple"
        AppLanguage.ARABIC -> "تموج"
    }
    
    val interGlow: String get() = when (lang) {
        AppLanguage.CHINESE -> "光晕扩散"
        AppLanguage.ENGLISH -> "Glow"
        AppLanguage.ARABIC -> "توهج"
    }
    
    val interScale: String get() = when (lang) {
        AppLanguage.CHINESE -> "缩放脉冲"
        AppLanguage.ENGLISH -> "Scale"
        AppLanguage.ARABIC -> "تكبير"
    }
    
    val interShake: String get() = when (lang) {
        AppLanguage.CHINESE -> "微震反馈"
        AppLanguage.ENGLISH -> "Shake"
        AppLanguage.ARABIC -> "اهتزاز"
    }
    
    val interMorph: String get() = when (lang) {
        AppLanguage.CHINESE -> "形态变换"
        AppLanguage.ENGLISH -> "Morph"
        AppLanguage.ARABIC -> "تحول"
    }
    
    val interParticle: String get() = when (lang) {
        AppLanguage.CHINESE -> "粒子迸发"
        AppLanguage.ENGLISH -> "Particle"
        AppLanguage.ARABIC -> "جسيمات"
    }
    
    // ==================== Module Categories ====================
    val catContentFilter: String get() = ModuleStrings.catContentFilter
    val catContentFilterDesc: String get() = ModuleStrings.catContentFilterDesc
    val catContentEnhance: String get() = ModuleStrings.catContentEnhance
    val catContentEnhanceDesc: String get() = ModuleStrings.catContentEnhanceDesc
    val catStyleModifier: String get() = ModuleStrings.catStyleModifier
    val catStyleModifierDesc: String get() = ModuleStrings.catStyleModifierDesc
    val catTheme: String get() = ModuleStrings.catTheme
    val catThemeDesc: String get() = ModuleStrings.catThemeDesc
    val catFunctionEnhance: String get() = ModuleStrings.catFunctionEnhance
    val catFunctionEnhanceDesc: String get() = ModuleStrings.catFunctionEnhanceDesc
    val catAutomation: String get() = ModuleStrings.catAutomation
    val catAutomationDesc: String get() = ModuleStrings.catAutomationDesc
    val catNavigation: String get() = ModuleStrings.catNavigation
    val catNavigationDesc: String get() = ModuleStrings.catNavigationDesc
    val catDataExtract: String get() = ModuleStrings.catDataExtract
    val catDataExtractDesc: String get() = ModuleStrings.catDataExtractDesc
    val catDataSave: String get() = ModuleStrings.catDataSave
    val catDataSaveDesc: String get() = ModuleStrings.catDataSaveDesc
    val catInteraction: String get() = ModuleStrings.catInteraction
    val catInteractionDesc: String get() = ModuleStrings.catInteractionDesc
    val catAccessibility: String get() = ModuleStrings.catAccessibility
    val catAccessibilityDesc: String get() = ModuleStrings.catAccessibilityDesc
    val catMedia: String get() = ModuleStrings.catMedia
    val catMediaDesc: String get() = ModuleStrings.catMediaDesc
    val catVideo: String get() = ModuleStrings.catVideo
    val catVideoDesc: String get() = ModuleStrings.catVideoDesc
    val catImage: String get() = ModuleStrings.catImage
    val catImageDesc: String get() = ModuleStrings.catImageDesc
    val catAudio: String get() = ModuleStrings.catAudio
    val catAudioDesc: String get() = ModuleStrings.catAudioDesc
    val catSecurity: String get() = ModuleStrings.catSecurity
    val catSecurityDesc: String get() = ModuleStrings.catSecurityDesc
    val catAntiTracking: String get() = ModuleStrings.catAntiTracking
    val catAntiTrackingDesc: String get() = ModuleStrings.catAntiTrackingDesc
    val catSocial: String get() = ModuleStrings.catSocial
    val catSocialDesc: String get() = ModuleStrings.catSocialDesc
    val catShopping: String get() = ModuleStrings.catShopping
    val catShoppingDesc: String get() = ModuleStrings.catShoppingDesc
    val catReading: String get() = ModuleStrings.catReading
    val catReadingDesc: String get() = ModuleStrings.catReadingDesc
    val catTranslate: String get() = ModuleStrings.catTranslate
    val catTranslateDesc: String get() = ModuleStrings.catTranslateDesc
    val catDeveloper: String get() = ModuleStrings.catDeveloper
    val catDeveloperDesc: String get() = ModuleStrings.catDeveloperDesc
    val catOther: String get() = ModuleStrings.catOther
    val catOtherDesc: String get() = ModuleStrings.catOtherDesc
    
    // ==================== Module Timing ====================
    val runTimeDocStart: String get() = ExtensionStrings.runTimeDocStart
    val runTimeDocStartDesc: String get() = ExtensionStrings.runTimeDocStartDesc
    val runTimeDocEnd: String get() = ExtensionStrings.runTimeDocEnd
    val runTimeDocEndDesc: String get() = ExtensionStrings.runTimeDocEndDesc
    val runTimeDocIdle: String get() = ExtensionStrings.runTimeDocIdle
    val runTimeDocIdleDesc: String get() = ExtensionStrings.runTimeDocIdleDesc
    val runTimeContextMenu: String get() = ExtensionStrings.runTimeContextMenu
    val runTimeContextMenuDesc: String get() = ExtensionStrings.runTimeContextMenuDesc
    val runTimeBeforeUnload: String get() = ExtensionStrings.runTimeBeforeUnload
    val runTimeBeforeUnloadDesc: String get() = ExtensionStrings.runTimeBeforeUnloadDesc
    
    // ==================== ====================
    val styleRefMovie: String get() = when (lang) {
        AppLanguage.CHINESE -> "电影"
        AppLanguage.ENGLISH -> "Movie"
        AppLanguage.ARABIC -> "فيلم"
    }
    val styleRefBook: String get() = when (lang) {
        AppLanguage.CHINESE -> "书籍"
        AppLanguage.ENGLISH -> "Book"
        AppLanguage.ARABIC -> "كتاب"
    }
    val styleRefAnime: String get() = when (lang) {
        AppLanguage.CHINESE -> "动画"
        AppLanguage.ENGLISH -> "Anime"
        AppLanguage.ARABIC -> "أنمي"
    }
    val styleRefGame: String get() = when (lang) {
        AppLanguage.CHINESE -> "游戏"
        AppLanguage.ENGLISH -> "Game"
        AppLanguage.ARABIC -> "لعبة"
    }
    val styleRefBrand: String get() = when (lang) {
        AppLanguage.CHINESE -> "品牌"
        AppLanguage.ENGLISH -> "Brand"
        AppLanguage.ARABIC -> "علامة تجارية"
    }
    val styleRefArt: String get() = when (lang) {
        AppLanguage.CHINESE -> "艺术流派"
        AppLanguage.ENGLISH -> "Art Style"
        AppLanguage.ARABIC -> "نمط فني"
    }
    val styleRefEra: String get() = when (lang) {
        AppLanguage.CHINESE -> "时代风格"
        AppLanguage.ENGLISH -> "Era Style"
        AppLanguage.ARABIC -> "نمط العصر"
    }
    val styleRefCulture: String get() = when (lang) {
        AppLanguage.CHINESE -> "文化风格"
        AppLanguage.ENGLISH -> "Cultural Style"
        AppLanguage.ARABIC -> "نمط ثقافي"
    }
    
    // ==================== Color Names ====================
    val colorRed: String get() = CommonStrings.colorRed
    
    val colorPink: String get() = CommonStrings.colorPink
    
    val colorPurple: String get() = CommonStrings.colorPurple
    
    val colorDeepPurple: String get() = CommonStrings.colorDeepPurple
    
    val colorIndigo: String get() = CommonStrings.colorIndigo
    
    val colorBlue: String get() = CommonStrings.colorBlue
    
    val colorLightBlue: String get() = CommonStrings.colorLightBlue
    
    val colorCyan: String get() = CommonStrings.colorCyan
    
    val colorTeal: String get() = CommonStrings.colorTeal
    
    val colorGreen: String get() = CommonStrings.colorGreen
    
    val colorLightGreen: String get() = CommonStrings.colorLightGreen
    
    val colorLime: String get() = CommonStrings.colorLime
    
    val colorYellow: String get() = CommonStrings.colorYellow
    
    val colorAmber: String get() = CommonStrings.colorAmber
    
    val colorOrange: String get() = CommonStrings.colorOrange
    
    val colorDeepOrange: String get() = CommonStrings.colorDeepOrange
    
    val colorBrown: String get() = CommonStrings.colorBrown
    
    val colorGrey: String get() = CommonStrings.colorGrey
    
    val colorBlueGrey: String get() = CommonStrings.colorBlueGrey
    
    val colorBlack: String get() = CommonStrings.colorBlack
    
    val colorWhite: String get() = CommonStrings.colorWhite
    
    val colorDarkTheme: String get() = CommonStrings.colorDarkTheme
    
    val colorLightTheme: String get() = CommonStrings.colorLightTheme
    
    val colorTransparent: String get() = CommonStrings.colorTransparent
    
    val colorSelected: String get() = CommonStrings.colorSelected
    
    // ==================== Extension Module ====================
    val selectedCount2: String get() = ExtensionStrings.selectedCount2
    
    val addCustomFeatures: String get() = ExtensionStrings.addCustomFeatures
    
    val quickSelect: String get() = ExtensionStrings.quickSelect
    
    val enableModulesFirst: String get() = ExtensionStrings.enableModulesFirst
    
    val selectedModulesCount: String get() = ExtensionStrings.selectedModulesCount
    
    val removeModule: String get() = ExtensionStrings.removeModule
    
    val noMatchingModules: String get() = ExtensionStrings.noMatchingModules
    
    val willTestModules: String get() = ExtensionStrings.willTestModules
    
    val selectTestPage: String get() = ExtensionStrings.selectTestPage
    
    val testPageHint: String get() = ExtensionStrings.testPageHint
    
    val builtInModule: String get() = ExtensionStrings.builtInModule
    
    val configurableItems: String get() = ExtensionStrings.configurableItems
    
    // ==================== ====================
    val mediaContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体内容"
        AppLanguage.ENGLISH -> "Media Content"
        AppLanguage.ARABIC -> "محتوى الوسائط"
    }
    
    val statusBarBackground: String get() = when (lang) {
        AppLanguage.CHINESE -> "状态栏背景"
        AppLanguage.ENGLISH -> "Status Bar Background"
        AppLanguage.ARABIC -> "خلفية شريط الحالة"
    }
    
    // ==================== ====================
    val appNeedsActivation: String get() = CommonStrings.appNeedsActivation
    
    val skip: String get() = when (lang) {
        AppLanguage.CHINESE -> "Skip"
        AppLanguage.ENGLISH -> "Skip"
        AppLanguage.ARABIC -> "تخطي"
    }
    
    // ==================== ====================
    val projectTemplateExport: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目模板导出"
        AppLanguage.ENGLISH -> "Project Template Export"
        AppLanguage.ARABIC -> "تصدير قالب المشروع"
    }
    
    // ==================== ====================
    val iKnow: String get() = when (lang) {
        AppLanguage.CHINESE -> "我知道了"
        AppLanguage.ENGLISH -> "I Know"
        AppLanguage.ARABIC -> "فهمت"
    }
    
    val gotItCute: String get() = when (lang) {
        AppLanguage.CHINESE -> "知道啦~ 💕"
        AppLanguage.ENGLISH -> "Got it~ 💕"
        AppLanguage.ARABIC -> "فهمت~ 💕"
    }
    
    val receivedGift: String get() = when (lang) {
        AppLanguage.CHINESE -> "🎁 收到啦"
        AppLanguage.ENGLISH -> "🎁 Received"
        AppLanguage.ARABIC -> "🎁 تم الاستلام"
    }
    
    val okayNature: String get() = when (lang) {
        AppLanguage.CHINESE -> "🌱 好的"
        AppLanguage.ENGLISH -> "🌱 Okay"
        AppLanguage.ARABIC -> "🌱 حسناً"
    }
    
    // ==================== ====================
    val codeBlockLibraryTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码块库"
        AppLanguage.ENGLISH -> "Code Block Library"
        AppLanguage.ARABIC -> "مكتبة كتل الكود"
    }
    
    val searchCodeBlocksPlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索代码块..."
        AppLanguage.ENGLISH -> "Search code blocks..."
        AppLanguage.ARABIC -> "البحث عن كتل الكود..."
    }
    
    val categoriesAndBlocks: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 分类 · %d 代码块"
        AppLanguage.ENGLISH -> "%d categories · %d code blocks"
        AppLanguage.ARABIC -> "%d فئات · %d كتل كود"
    }
    
    val foundResults: String get() = when (lang) {
        AppLanguage.CHINESE -> "找到 %d 个结果"
        AppLanguage.ENGLISH -> "Found %d results"
        AppLanguage.ARABIC -> "تم العثور على %d نتائج"
    }
    
    val noMatchingCodeBlocks: String get() = when (lang) {
        AppLanguage.CHINESE -> "没有找到匹配的代码块"
        AppLanguage.ENGLISH -> "No matching code blocks found"
        AppLanguage.ARABIC -> "لم يتم العثور على كتل كود مطابقة"
    }
    
    val insert: String get() = when (lang) {
        AppLanguage.CHINESE -> "插入"
        AppLanguage.ENGLISH -> "Insert"
        AppLanguage.ARABIC -> "إدراج"
    }
    
    val quickInsertCodeSnippets: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速插入常用代码片段"
        AppLanguage.ENGLISH -> "Quick insert common code snippets"
        AppLanguage.ARABIC -> "إدراج سريع لمقتطفات الكود الشائعة"
    }
    
    val codeBlocksCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个代码块"
        AppLanguage.ENGLISH -> "%d code blocks"
        AppLanguage.ARABIC -> "%d كتل كود"
    }
    
    // ==================== LRC ====================
    
    val totalLinesCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "共 %d 行歌词"
        AppLanguage.ENGLISH -> "%d lines of lyrics"
        AppLanguage.ARABIC -> "%d سطر من الكلمات"
    }
    
    // ==================== ====================
    val testModuleTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "测试模块"
        AppLanguage.ENGLISH -> "Test Module"
        AppLanguage.ARABIC -> "اختبار الوحدة"
    }
    
    val willTestModulesFormat: String get() = when (lang) {
        AppLanguage.CHINESE -> "将测试 %d 个模块"
        AppLanguage.ENGLISH -> "Will test %d modules"
        AppLanguage.ARABIC -> "سيتم اختبار %d وحدات"
    }
    
    val selectTestPageTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择测试页面"
        AppLanguage.ENGLISH -> "Select Test Page"
        AppLanguage.ARABIC -> "اختيار صفحة الاختبار"
    }
    
    val startTestBtn: String get() = when (lang) {
        AppLanguage.CHINESE -> "开始测试"
        AppLanguage.ENGLISH -> "Start Test"
        AppLanguage.ARABIC -> "بدء الاختبار"
    }
    
    val testPageHintText: String get() = when (lang) {
        AppLanguage.CHINESE -> "💡 测试页面会加载选中的模块，你可以观察模块的实际效果"
        AppLanguage.ENGLISH -> "💡 Test page will load selected modules, you can observe actual effects"
        AppLanguage.ARABIC -> "💡 ستقوم صفحة الاختبار بتحميل الوحدات المحددة، يمكنك ملاحظة التأثيرات الفعلية"
    }
    
    // ==================== Scheme Management ====================
    val quickSchemes: String get() = UiStrings.quickSchemes
    
    val allSchemesBtn: String get() = UiStrings.allSchemesBtn
    
    val builtInSchemes: String get() = UiStrings.builtInSchemes
    
    val mySchemes: String get() = UiStrings.mySchemes
    
    val schemeTip: String get() = UiStrings.schemeTip
    
    val containsModules: String get() = UiStrings.containsModules
    
    val applied: String get() = CommonStrings.applied
    
    val schemeNameLabel: String get() = UiStrings.schemeNameLabel
    
    val enterSchemeNameHint: String get() = UiStrings.enterSchemeNameHint
    
    val descriptionOptionalLabel: String get() = UiStrings.descriptionOptionalLabel
    
    val optionalLabel: String get() = UiStrings.optionalLabel
    
    val briefDescribeSchemeHint: String get() = UiStrings.briefDescribeSchemeHint
    
    val willSaveModules: String get() = UiStrings.willSaveModules
    
    val selectIconTitle: String get() = UiStrings.selectIconTitle
    
    // ==================== ====================
    
    // ==================== Extension Module Cards ====================
    
    val selectedCountFormat: String get() = ExtensionStrings.selectedCountFormat
    
    // ==================== ====================
    
    // ==================== LRC ====================
    val previewLrcHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览生成的 LRC 效果，确认无误后保存"
        AppLanguage.ENGLISH -> "Preview generated LRC effect, save after confirmation"
        AppLanguage.ARABIC -> "معاينة تأثير LRC المُنشأ، احفظ بعد التأكيد"
    }
    
    // ==================== ====================
    
    // ==================== ====================
    
    // ==================== ====================
    
    // ==================== AI ====================
    val featureWriteHtml: String get() = AiCodingStrings.featureWriteHtml
    
    val featureEditHtml: String get() = AiCodingStrings.featureEditHtml
    
    val featureGetConsoleLogs: String get() = AiCodingStrings.featureGetConsoleLogs
    
    val featureCheckSyntax: String get() = AiCodingStrings.featureCheckSyntax
    
    val featureAutoFix: String get() = AiCodingStrings.featureAutoFix
    
    val featureIconGeneration: String get() = AiCodingStrings.featureIconGeneration
    
    val featureModuleDevelopment: String get() = AiCodingStrings.featureModuleDevelopment
    
    val featureLrcGeneration: String get() = AiCodingStrings.featureLrcGeneration
    
    val featureTranslation: String get() = AiCodingStrings.featureTranslation
    
    val featureGeneralChat: String get() = AiCodingStrings.featureGeneralChat
    
    // ==================== ====================
    val coding: String get() = when (lang) {
        AppLanguage.CHINESE -> "编程"
        AppLanguage.ENGLISH -> "Coding"
        AppLanguage.ARABIC -> "البرمجة"
    }
    
    // ==================== AI ====================
    val aiCodingDesc: String get() = AiStrings.aiCodingDesc
    
    val aiCodingImageDesc: String get() = AiStrings.aiCodingImageDesc
    
    val iconGenerationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 AI 生成应用图标"
        AppLanguage.ENGLISH -> "Generate app icons using AI"
        AppLanguage.ARABIC -> "إنشاء أيقونات التطبيق باستخدام الذكاء الاصطناعي"
    }
    
    val moduleDevelopmentDesc: String get() = ModuleStrings.moduleDevelopmentDesc
    
    val lrcGenerationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 生成 LRC 歌词文件"
        AppLanguage.ENGLISH -> "AI-generated LRC lyrics files"
        AppLanguage.ARABIC -> "ملفات كلمات LRC المُنشأة بالذكاء الاصطناعي"
    }
    
    val translationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "网页内容翻译"
        AppLanguage.ENGLISH -> "Web content translation"
        AppLanguage.ARABIC -> "ترجمة محتوى الويب"
    }
    
    val generalChatDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "通用 AI 对话功能"
        AppLanguage.ENGLISH -> "General AI chat functionality"
        AppLanguage.ARABIC -> "وظيفة الدردشة العامة بالذكاء الاصطناعي"
    }
    
    // ==================== HTML ====================
    val aiImageGeneration: String get() = AiStrings.aiImageGeneration
    
    val writeHtmlDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建或覆盖完整的 HTML 页面"
        AppLanguage.ENGLISH -> "Create or overwrite complete HTML pages"
        AppLanguage.ARABIC -> "إنشاء أو استبدال صفحات HTML كاملة"
    }
    
    val editHtmlDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "在指定位置替换、插入或删除代码片段"
        AppLanguage.ENGLISH -> "Replace, insert or delete code snippets at specified locations"
        AppLanguage.ARABIC -> "استبدال أو إدراج أو حذف مقاطع الكود في المواقع المحددة"
    }
    
    val generateImageDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 AI 生成图像并嵌入到 HTML 中作为插图"
        AppLanguage.ENGLISH -> "Generate images using AI and embed them in HTML as illustrations"
        AppLanguage.ARABIC -> "إنشاء صور باستخدام الذكاء الاصطناعي وتضمينها في HTML كرسوم توضيحية"
    }
    
    val getConsoleLogsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取页面运行时的 console.log 输出和错误信息"
        AppLanguage.ENGLISH -> "Get console.log output and error messages during page runtime"
        AppLanguage.ARABIC -> "الحصول على مخرجات console.log ورسائل الخطأ أثناء تشغيل الصفحة"
    }
    
    val checkSyntaxDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查 HTML/CSS/JavaScript 语法错误"
        AppLanguage.ENGLISH -> "Check HTML/CSS/JavaScript syntax errors"
        AppLanguage.ARABIC -> "فحص أخطاء بناء جملة HTML/CSS/JavaScript"
    }
    
    val autoFixDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动修复检测到的语法错误"
        AppLanguage.ENGLISH -> "Automatically fix detected syntax errors"
        AppLanguage.ARABIC -> "إصلاح أخطاء بناء الجملة المكتشفة تلقائيًا"
    }
    
    // ==================== Template Categories ====================
    val templateModern: String get() = ExtensionStrings.templateModern
    
    val templateGlassmorphism: String get() = ExtensionStrings.templateGlassmorphism
    
    val templateNeumorphism: String get() = ExtensionStrings.templateNeumorphism
    
    val templateGradient: String get() = ExtensionStrings.templateGradient
    
    val templateDark: String get() = ExtensionStrings.templateDark
    
    val templateMinimal: String get() = ExtensionStrings.templateMinimal
    
    val templateRetro: String get() = ExtensionStrings.templateRetro
    
    val templateCyberpunk: String get() = ExtensionStrings.templateCyberpunk
    
    val templateNature: String get() = ExtensionStrings.templateNature
    
    val templateBusiness: String get() = ExtensionStrings.templateBusiness
    
    val templateCreative: String get() = ExtensionStrings.templateCreative
    
    val templateGame: String get() = ExtensionStrings.templateGame
    
    // ==================== Session Config ====================
    val sessionConfig: String get() = AiConfigStrings.sessionConfig
    
    val textModel: String get() = AiConfigStrings.textModel
    
    val imageModelOptional: String get() = AiConfigStrings.imageModelOptional
    
    val temperature: String get() = AiConfigStrings.temperature
    
    val temperatureHint: String get() = AiConfigStrings.temperatureHint
    
    val toolbox: String get() = AiConfigStrings.toolbox
    
    val nEnabled: String get() = AiConfigStrings.nEnabled
    
    val toolboxHint: String get() = AiConfigStrings.toolboxHint
    
    val nMessages: String get() = AiConfigStrings.nMessages
    
    val dataBackupTitle: String get() = AiConfigStrings.dataBackupTitle
    
    val dataBackupDesc: String get() = AiConfigStrings.dataBackupDesc
    
    val dataBackupNote: String get() = AiConfigStrings.dataBackupNote
    
    val legalDisclaimer: String get() = AiConfigStrings.legalDisclaimer
    val disclaimerWarningText: String get() = AiConfigStrings.disclaimerWarningText
    val finalUserAgreementConfirmation: String get() = AiConfigStrings.finalUserAgreementConfirmation
    
    // ==================== HTML ====================
    val toolWriteHtml: String get() = AiCodingStrings.toolWriteHtml
    val toolWriteHtmlDesc: String get() = AiCodingStrings.toolWriteHtmlDesc
    val toolEditHtml: String get() = AiCodingStrings.toolEditHtml
    val toolEditHtmlDesc: String get() = AiCodingStrings.toolEditHtmlDesc
    val toolGenerateImage: String get() = AiCodingStrings.toolGenerateImage
    val toolGenerateImageDesc: String get() = AiCodingStrings.toolGenerateImageDesc
    val toolGetConsoleLogs: String get() = AiCodingStrings.toolGetConsoleLogs
    val toolGetConsoleLogsDesc: String get() = AiCodingStrings.toolGetConsoleLogsDesc
    val toolCheckSyntax: String get() = AiCodingStrings.toolCheckSyntax
    val toolCheckSyntaxDesc: String get() = AiCodingStrings.toolCheckSyntaxDesc
    val toolAutoFix: String get() = AiCodingStrings.toolAutoFix
    val toolAutoFixDesc: String get() = AiCodingStrings.toolAutoFixDesc
    val toolReadCurrentCode: String get() = AiCodingStrings.toolReadCurrentCode
    val toolReadCurrentCodeDesc: String get() = AiCodingStrings.toolReadCurrentCodeDesc
    val featureReadCurrentCode: String get() = AiCodingStrings.featureReadCurrentCode
    val readCurrentCodeDesc: String get() = AiCodingStrings.readCurrentCodeDesc
    val required: String get() = AiCodingStrings.required
    val requiresImageModel: String get() = AiCodingStrings.requiresImageModel
    
    // ==================== Module Templates ====================
    val tplElementHider: String get() = ModuleStrings.tplElementHider
    val tplElementHiderDesc: String get() = ModuleStrings.tplElementHiderDesc
    val tplAdBlocker: String get() = ModuleStrings.tplAdBlocker
    val tplAdBlockerDesc: String get() = ModuleStrings.tplAdBlockerDesc
    val tplPopupBlocker: String get() = ModuleStrings.tplPopupBlocker
    val tplPopupBlockerDesc: String get() = ModuleStrings.tplPopupBlockerDesc
    val tplCookieBanner: String get() = ModuleStrings.tplCookieBanner
    val tplCookieBannerDesc: String get() = ModuleStrings.tplCookieBannerDesc
    val tplCssInjector: String get() = ModuleStrings.tplCssInjector
    val tplCssInjectorDesc: String get() = ModuleStrings.tplCssInjectorDesc
    val tplDarkMode: String get() = ModuleStrings.tplDarkMode
    val tplDarkModeDesc: String get() = ModuleStrings.tplDarkModeDesc
    val tplFontChanger: String get() = ModuleStrings.tplFontChanger
    val tplFontChangerDesc: String get() = ModuleStrings.tplFontChangerDesc
    val tplScrollToTop: String get() = ModuleStrings.tplScrollToTop
    val tplScrollToTopDesc: String get() = ModuleStrings.tplScrollToTopDesc
    
    // ==================== Snippet Categories ====================
    val snippetNative: String get() = SnippetStrings.snippetNative
    val snippetNativeDesc: String get() = SnippetStrings.snippetNativeDesc
    val snippetShowToast: String get() = SnippetStrings.snippetShowToast
    val snippetShowToastDesc: String get() = SnippetStrings.snippetShowToastDesc
    val snippetVibrate: String get() = SnippetStrings.snippetVibrate
    val snippetVibrateDesc: String get() = SnippetStrings.snippetVibrateDesc
    val snippetCopyToClipboard: String get() = SnippetStrings.snippetCopyToClipboard
    val snippetCopyToClipboardDesc: String get() = SnippetStrings.snippetCopyToClipboardDesc
    val snippetSaveVideoToGallery: String get() = SnippetStrings.snippetSaveVideoToGallery
    val snippetSaveVideoToGalleryDesc: String get() = SnippetStrings.snippetSaveVideoToGalleryDesc
    val snippetOpenInBrowser: String get() = SnippetStrings.snippetOpenInBrowser
    val snippetOpenInBrowserDesc: String get() = SnippetStrings.snippetOpenInBrowserDesc
    val snippetDeviceInfo: String get() = SnippetStrings.snippetDeviceInfo
    val snippetDeviceInfoDesc: String get() = SnippetStrings.snippetDeviceInfoDesc
    val snippetNetworkStatus: String get() = SnippetStrings.snippetNetworkStatus
    val snippetNetworkStatusDesc: String get() = SnippetStrings.snippetNetworkStatusDesc
    val snippetSaveFile: String get() = SnippetStrings.snippetSaveFile
    val snippetSaveFileDesc: String get() = SnippetStrings.snippetSaveFileDesc
    val snippetImageDownloadBtn: String get() = SnippetStrings.snippetImageDownloadBtn
    val snippetImageDownloadBtnDesc: String get() = SnippetStrings.snippetImageDownloadBtnDesc
    val snippetDom: String get() = SnippetStrings.snippetDom
    val snippetDomDesc: String get() = SnippetStrings.snippetDomDesc
    val snippetStyle: String get() = SnippetStrings.snippetStyle
    val snippetStyleDesc: String get() = SnippetStrings.snippetStyleDesc
    val snippetEvent: String get() = SnippetStrings.snippetEvent
    val snippetEventDesc: String get() = SnippetStrings.snippetEventDesc
    val snippetStorage: String get() = SnippetStrings.snippetStorage
    val snippetStorageDesc: String get() = SnippetStrings.snippetStorageDesc
    val snippetNetwork: String get() = SnippetStrings.snippetNetwork
    val snippetNetworkDesc: String get() = SnippetStrings.snippetNetworkDesc
    val snippetUi: String get() = SnippetStrings.snippetUi
    val snippetUiDesc: String get() = SnippetStrings.snippetUiDesc
    val snippetWidget: String get() = SnippetStrings.snippetWidget
    val snippetWidgetDesc: String get() = SnippetStrings.snippetWidgetDesc
    val snippetNotification: String get() = SnippetStrings.snippetNotification
    val snippetNotificationDesc: String get() = SnippetStrings.snippetNotificationDesc
    val snippetScroll: String get() = SnippetStrings.snippetScroll
    val snippetScrollDesc: String get() = SnippetStrings.snippetScrollDesc
    
    // DOM.
    val snippetQuerySingle: String get() = SnippetStrings.snippetQuerySingle
    val snippetQuerySingleDesc: String get() = SnippetStrings.snippetQuerySingleDesc
    val snippetQueryAll: String get() = SnippetStrings.snippetQueryAll
    val snippetQueryAllDesc: String get() = SnippetStrings.snippetQueryAllDesc
    val snippetHideElement: String get() = SnippetStrings.snippetHideElement
    val snippetHideElementDesc: String get() = SnippetStrings.snippetHideElementDesc
    val snippetRemoveElement: String get() = SnippetStrings.snippetRemoveElement
    val snippetRemoveElementDesc: String get() = SnippetStrings.snippetRemoveElementDesc
    val snippetCreateElement: String get() = SnippetStrings.snippetCreateElement
    val snippetCreateElementDesc: String get() = SnippetStrings.snippetCreateElementDesc
    val snippetModifyText: String get() = SnippetStrings.snippetModifyText
    val snippetModifyTextDesc: String get() = SnippetStrings.snippetModifyTextDesc
    val snippetModifyAttr: String get() = SnippetStrings.snippetModifyAttr
    val snippetModifyAttrDesc: String get() = SnippetStrings.snippetModifyAttrDesc
    val snippetInsertBefore: String get() = SnippetStrings.snippetInsertBefore
    val snippetInsertBeforeDesc: String get() = SnippetStrings.snippetInsertBeforeDesc
    val snippetInsertAfter: String get() = SnippetStrings.snippetInsertAfter
    val snippetInsertAfterDesc: String get() = SnippetStrings.snippetInsertAfterDesc
    val snippetCloneElement: String get() = SnippetStrings.snippetCloneElement
    val snippetCloneElementDesc: String get() = SnippetStrings.snippetCloneElementDesc
    val snippetWrapElement: String get() = SnippetStrings.snippetWrapElement
    val snippetWrapElementDesc: String get() = SnippetStrings.snippetWrapElementDesc
    val snippetReplaceElement: String get() = SnippetStrings.snippetReplaceElement
    val snippetReplaceElementDesc: String get() = SnippetStrings.snippetReplaceElementDesc
    val snippetUtil: String get() = SnippetStrings.snippetUtil
    val snippetUtilDesc: String get() = SnippetStrings.snippetUtilDesc
    val snippetData: String get() = SnippetStrings.snippetData
    val snippetDataDesc: String get() = SnippetStrings.snippetDataDesc
    val snippetSaveImageToGallery: String get() = SnippetStrings.snippetSaveImageToGallery
    val snippetSaveImageToGalleryDesc: String get() = SnippetStrings.snippetSaveImageToGalleryDesc
    val snippetShareContent: String get() = SnippetStrings.snippetShareContent
    val snippetShareContentDesc: String get() = SnippetStrings.snippetShareContentDesc
    val snippetInjectCss: String get() = SnippetStrings.snippetInjectCss
    val snippetInjectCssDesc: String get() = SnippetStrings.snippetInjectCssDesc
    val snippetModifyInline: String get() = SnippetStrings.snippetModifyInline
    val snippetModifyInlineDesc: String get() = SnippetStrings.snippetModifyInlineDesc
    val snippetAddClass: String get() = SnippetStrings.snippetAddClass
    val snippetAddClassDesc: String get() = SnippetStrings.snippetAddClassDesc
    val snippetDarkMode: String get() = SnippetStrings.snippetDarkMode
    val snippetDarkModeDesc: String get() = SnippetStrings.snippetDarkModeDesc
    val snippetSepiaMode: String get() = SnippetStrings.snippetSepiaMode
    val snippetSepiaModeDesc: String get() = SnippetStrings.snippetSepiaModeDesc
    val snippetGrayscale: String get() = SnippetStrings.snippetGrayscale
    val snippetGrayscaleDesc: String get() = SnippetStrings.snippetGrayscaleDesc
    val snippetCustomFont: String get() = SnippetStrings.snippetCustomFont
    val snippetCustomFontDesc: String get() = SnippetStrings.snippetCustomFontDesc
    val snippetFontSize: String get() = SnippetStrings.snippetFontSize
    val snippetFontSizeDesc: String get() = SnippetStrings.snippetFontSizeDesc
    val snippetHideScrollbar: String get() = SnippetStrings.snippetHideScrollbar
    val snippetHideScrollbarDesc: String get() = SnippetStrings.snippetHideScrollbarDesc
    val snippetHighlightLinks: String get() = SnippetStrings.snippetHighlightLinks
    val snippetHighlightLinksDesc: String get() = SnippetStrings.snippetHighlightLinksDesc
    val snippetMaxWidth: String get() = SnippetStrings.snippetMaxWidth
    val snippetMaxWidthDesc: String get() = SnippetStrings.snippetMaxWidthDesc
    val snippetLineHeight: String get() = SnippetStrings.snippetLineHeight
    val snippetLineHeightDesc: String get() = SnippetStrings.snippetLineHeightDesc
    
    // Note.
    val snippetClickEvent: String get() = SnippetStrings.snippetClickEvent
    val snippetClickEventDesc: String get() = SnippetStrings.snippetClickEventDesc
    val snippetKeyboardEvent: String get() = SnippetStrings.snippetKeyboardEvent
    val snippetKeyboardEventDesc: String get() = SnippetStrings.snippetKeyboardEventDesc
    val snippetScrollEvent: String get() = SnippetStrings.snippetScrollEvent
    val snippetScrollEventDesc: String get() = SnippetStrings.snippetScrollEventDesc
    val snippetMutationEvent: String get() = SnippetStrings.snippetMutationEvent
    val snippetMutationEventDesc: String get() = SnippetStrings.snippetMutationEventDesc
    val snippetResizeEvent: String get() = SnippetStrings.snippetResizeEvent
    val snippetResizeEventDesc: String get() = SnippetStrings.snippetResizeEventDesc
    val snippetCopyEvent: String get() = SnippetStrings.snippetCopyEvent
    val snippetCopyEventDesc: String get() = SnippetStrings.snippetCopyEventDesc
    val snippetContextMenu: String get() = SnippetStrings.snippetContextMenu
    val snippetContextMenuDesc: String get() = SnippetStrings.snippetContextMenuDesc
    val snippetVisibility: String get() = SnippetStrings.snippetVisibility
    val snippetVisibilityDesc: String get() = SnippetStrings.snippetVisibilityDesc
    val snippetBeforeUnload: String get() = SnippetStrings.snippetBeforeUnload
    val snippetBeforeUnloadDesc: String get() = SnippetStrings.snippetBeforeUnloadDesc
    val snippetTouchEvent: String get() = SnippetStrings.snippetTouchEvent
    val snippetTouchEventDesc: String get() = SnippetStrings.snippetTouchEventDesc
    val snippetLongPress: String get() = SnippetStrings.snippetLongPress
    val snippetLongPressDesc: String get() = SnippetStrings.snippetLongPressDesc
    
    // Storage.
    val snippetLocalSet: String get() = SnippetStrings.snippetLocalSet
    val snippetLocalSetDesc: String get() = SnippetStrings.snippetLocalSetDesc
    val snippetLocalGet: String get() = SnippetStrings.snippetLocalGet
    val snippetLocalGetDesc: String get() = SnippetStrings.snippetLocalGetDesc
    val snippetSessionStorage: String get() = SnippetStrings.snippetSessionStorage
    val snippetSessionStorageDesc: String get() = SnippetStrings.snippetSessionStorageDesc
    val snippetSetCookie: String get() = SnippetStrings.snippetSetCookie
    val snippetSetCookieDesc: String get() = SnippetStrings.snippetSetCookieDesc
    val snippetGetCookie: String get() = SnippetStrings.snippetGetCookie
    val snippetGetCookieDesc: String get() = SnippetStrings.snippetGetCookieDesc
    val snippetDeleteCookie: String get() = SnippetStrings.snippetDeleteCookie
    val snippetDeleteCookieDesc: String get() = SnippetStrings.snippetDeleteCookieDesc
    val snippetIndexedDB: String get() = SnippetStrings.snippetIndexedDB
    val snippetIndexedDBDesc: String get() = SnippetStrings.snippetIndexedDBDesc
    
    // Network.
    val snippetGetRequest: String get() = SnippetStrings.snippetGetRequest
    val snippetGetRequestDesc: String get() = SnippetStrings.snippetGetRequestDesc
    val snippetPostRequest: String get() = SnippetStrings.snippetPostRequest
    val snippetPostRequestDesc: String get() = SnippetStrings.snippetPostRequestDesc
    val snippetTimeoutRequest: String get() = SnippetStrings.snippetTimeoutRequest
    val snippetTimeoutRequestDesc: String get() = SnippetStrings.snippetTimeoutRequestDesc
    val snippetRetryRequest: String get() = SnippetStrings.snippetRetryRequest
    val snippetRetryRequestDesc: String get() = SnippetStrings.snippetRetryRequestDesc
    val snippetDownloadFile: String get() = SnippetStrings.snippetDownloadFile
    val snippetDownloadFileDesc: String get() = SnippetStrings.snippetDownloadFileDesc
    val snippetJsonp: String get() = SnippetStrings.snippetJsonp
    val snippetJsonpDesc: String get() = SnippetStrings.snippetJsonpDesc
    
    // Note.
    val snippetExtractTable: String get() = SnippetStrings.snippetExtractTable
    val snippetExtractTableDesc: String get() = SnippetStrings.snippetExtractTableDesc
    val snippetExtractLinks: String get() = SnippetStrings.snippetExtractLinks
    val snippetExtractLinksDesc: String get() = SnippetStrings.snippetExtractLinksDesc
    val snippetExtractImages: String get() = SnippetStrings.snippetExtractImages
    val snippetExtractImagesDesc: String get() = SnippetStrings.snippetExtractImagesDesc
    val snippetExportJson: String get() = SnippetStrings.snippetExportJson
    val snippetExportJsonDesc: String get() = SnippetStrings.snippetExportJsonDesc
    val snippetExportCsv: String get() = SnippetStrings.snippetExportCsv
    val snippetExportCsvDesc: String get() = SnippetStrings.snippetExportCsvDesc
    val snippetParseUrl: String get() = SnippetStrings.snippetParseUrl
    val snippetParseUrlDesc: String get() = SnippetStrings.snippetParseUrlDesc
    val snippetBuildUrl: String get() = SnippetStrings.snippetBuildUrl
    val snippetBuildUrlDesc: String get() = SnippetStrings.snippetBuildUrlDesc
    val snippetFloatingButton: String get() = SnippetStrings.snippetFloatingButton
    val snippetFloatingButtonDesc: String get() = SnippetStrings.snippetFloatingButtonDesc
    val snippetToastUi: String get() = SnippetStrings.snippetToastUi
    val snippetToastUiDesc: String get() = SnippetStrings.snippetToastUiDesc
    val snippetModal: String get() = SnippetStrings.snippetModal
    val snippetModalDesc: String get() = SnippetStrings.snippetModalDesc
    val snippetProgressBar: String get() = SnippetStrings.snippetProgressBar
    val snippetProgressBarDesc: String get() = SnippetStrings.snippetProgressBarDesc
    val snippetLoading: String get() = SnippetStrings.snippetLoading
    val snippetLoadingDesc: String get() = SnippetStrings.snippetLoadingDesc
    val snippetSnackbar: String get() = SnippetStrings.snippetSnackbar
    val snippetSnackbarDesc: String get() = SnippetStrings.snippetSnackbarDesc
    
    // Note.
    val snippetToolbar: String get() = SnippetStrings.snippetToolbar
    val snippetToolbarDesc: String get() = SnippetStrings.snippetToolbarDesc
    val snippetSidebar: String get() = SnippetStrings.snippetSidebar
    val snippetSidebarDesc: String get() = SnippetStrings.snippetSidebarDesc
    val snippetDraggable: String get() = SnippetStrings.snippetDraggable
    val snippetDraggableDesc: String get() = SnippetStrings.snippetDraggableDesc
    val snippetMiniPlayer: String get() = SnippetStrings.snippetMiniPlayer
    val snippetMiniPlayerDesc: String get() = SnippetStrings.snippetMiniPlayerDesc
    
    // Note.
    val snippetBrowserNotif: String get() = SnippetStrings.snippetBrowserNotif
    val snippetBrowserNotifDesc: String get() = SnippetStrings.snippetBrowserNotifDesc
    val snippetBadge: String get() = SnippetStrings.snippetBadge
    val snippetBadgeDesc: String get() = SnippetStrings.snippetBadgeDesc
    val snippetBanner: String get() = SnippetStrings.snippetBanner
    val snippetBannerDesc: String get() = SnippetStrings.snippetBannerDesc
    val snippetScrollToTop: String get() = SnippetStrings.snippetScrollToTop
    val snippetScrollToTopDesc: String get() = SnippetStrings.snippetScrollToTopDesc
    val snippetScrollToBottom: String get() = SnippetStrings.snippetScrollToBottom
    val snippetScrollToBottomDesc: String get() = SnippetStrings.snippetScrollToBottomDesc
    val snippetScrollToElement: String get() = SnippetStrings.snippetScrollToElement
    val snippetScrollToElementDesc: String get() = SnippetStrings.snippetScrollToElementDesc
    val snippetAutoScroll: String get() = SnippetStrings.snippetAutoScroll
    val snippetAutoScrollDesc: String get() = SnippetStrings.snippetAutoScrollDesc
    val snippetBackToTopBtn: String get() = SnippetStrings.snippetBackToTopBtn
    val snippetBackToTopBtnDesc: String get() = SnippetStrings.snippetBackToTopBtnDesc
    val snippetInfiniteScroll: String get() = SnippetStrings.snippetInfiniteScroll
    val snippetInfiniteScrollDesc: String get() = SnippetStrings.snippetInfiniteScrollDesc
    val snippetScrollReveal: String get() = SnippetStrings.snippetScrollReveal
    val snippetScrollRevealDesc: String get() = SnippetStrings.snippetScrollRevealDesc
    val snippetScrollSpy: String get() = SnippetStrings.snippetScrollSpy
    val snippetScrollSpyDesc: String get() = SnippetStrings.snippetScrollSpyDesc
    
    // Note.
    val snippetForm: String get() = SnippetStrings.snippetForm
    val snippetFormDesc: String get() = SnippetStrings.snippetFormDesc
    val snippetAutoFill: String get() = SnippetStrings.snippetAutoFill
    val snippetAutoFillDesc: String get() = SnippetStrings.snippetAutoFillDesc
    val snippetGetFormData: String get() = SnippetStrings.snippetGetFormData
    val snippetGetFormDataDesc: String get() = SnippetStrings.snippetGetFormDataDesc
    val snippetFormValidate: String get() = SnippetStrings.snippetFormValidate
    val snippetFormValidateDesc: String get() = SnippetStrings.snippetFormValidateDesc
    val snippetFormIntercept: String get() = SnippetStrings.snippetFormIntercept
    val snippetFormInterceptDesc: String get() = SnippetStrings.snippetFormInterceptDesc
    val snippetFormClear: String get() = SnippetStrings.snippetFormClear
    val snippetFormClearDesc: String get() = SnippetStrings.snippetFormClearDesc
    val snippetPasswordToggle: String get() = SnippetStrings.snippetPasswordToggle
    val snippetPasswordToggleDesc: String get() = SnippetStrings.snippetPasswordToggleDesc
    
    // Media.
    val snippetMedia: String get() = SnippetStrings.snippetMedia
    val snippetMediaDesc: String get() = SnippetStrings.snippetMediaDesc
    val snippetVideoSpeed: String get() = SnippetStrings.snippetVideoSpeed
    val snippetVideoSpeedDesc: String get() = SnippetStrings.snippetVideoSpeedDesc
    val snippetPiP: String get() = SnippetStrings.snippetPiP
    val snippetPiPDesc: String get() = SnippetStrings.snippetPiPDesc
    val snippetVideoScreenshot: String get() = SnippetStrings.snippetVideoScreenshot
    val snippetVideoScreenshotDesc: String get() = SnippetStrings.snippetVideoScreenshotDesc
    val snippetImageZoom: String get() = SnippetStrings.snippetImageZoom
    val snippetImageZoomDesc: String get() = SnippetStrings.snippetImageZoomDesc
    val snippetDownloadImages: String get() = SnippetStrings.snippetDownloadImages
    val snippetDownloadImagesDesc: String get() = SnippetStrings.snippetDownloadImagesDesc
    val snippetAudioControl: String get() = SnippetStrings.snippetAudioControl
    val snippetAudioControlDesc: String get() = SnippetStrings.snippetAudioControlDesc
    val snippetLazyLoad: String get() = SnippetStrings.snippetLazyLoad
    val snippetLazyLoadDesc: String get() = SnippetStrings.snippetLazyLoadDesc
    val snippetFullscreen: String get() = SnippetStrings.snippetFullscreen
    val snippetFullscreenDesc: String get() = SnippetStrings.snippetFullscreenDesc
    
    // Page.
    val snippetEnhance: String get() = SnippetStrings.snippetEnhance
    val snippetEnhanceDesc: String get() = SnippetStrings.snippetEnhanceDesc
    val snippetReadingMode: String get() = SnippetStrings.snippetReadingMode
    val snippetReadingModeDesc: String get() = SnippetStrings.snippetReadingModeDesc
    val snippetCopyUnlock: String get() = SnippetStrings.snippetCopyUnlock
    val snippetCopyUnlockDesc: String get() = SnippetStrings.snippetCopyUnlockDesc
    val snippetPrintFriendly: String get() = SnippetStrings.snippetPrintFriendly
    val snippetPrintFriendlyDesc: String get() = SnippetStrings.snippetPrintFriendlyDesc
    val snippetTextToSpeech: String get() = SnippetStrings.snippetTextToSpeech
    val snippetTextToSpeechDesc: String get() = SnippetStrings.snippetTextToSpeechDesc
    val snippetWordCount: String get() = SnippetStrings.snippetWordCount
    val snippetWordCountDesc: String get() = SnippetStrings.snippetWordCountDesc
    val snippetHighlightSearch: String get() = SnippetStrings.snippetHighlightSearch
    val snippetHighlightSearchDesc: String get() = SnippetStrings.snippetHighlightSearchDesc
    val snippetHideAds: String get() = SnippetStrings.snippetHideAds
    val snippetHideAdsDesc: String get() = SnippetStrings.snippetHideAdsDesc
    
    // Note.
    val snippetFilter: String get() = SnippetStrings.snippetFilter
    val snippetFilterDesc: String get() = SnippetStrings.snippetFilterDesc
    val snippetKeywordFilter: String get() = SnippetStrings.snippetKeywordFilter
    val snippetKeywordFilterDesc: String get() = SnippetStrings.snippetKeywordFilterDesc
    val snippetRemoveEmpty: String get() = SnippetStrings.snippetRemoveEmpty
    val snippetRemoveEmptyDesc: String get() = SnippetStrings.snippetRemoveEmptyDesc
    val snippetFilterComments: String get() = SnippetStrings.snippetFilterComments
    val snippetFilterCommentsDesc: String get() = SnippetStrings.snippetFilterCommentsDesc
    val snippetFilterSmallImages: String get() = SnippetStrings.snippetFilterSmallImages
    val snippetFilterSmallImagesDesc: String get() = SnippetStrings.snippetFilterSmallImagesDesc
    
    // Ad.
    val snippetAdBlock: String get() = SnippetStrings.snippetAdBlock
    val snippetAdBlockDesc: String get() = SnippetStrings.snippetAdBlockDesc
    val snippetBlockPopup: String get() = SnippetStrings.snippetBlockPopup
    val snippetBlockPopupDesc: String get() = SnippetStrings.snippetBlockPopupDesc
    val snippetRemoveOverlay: String get() = SnippetStrings.snippetRemoveOverlay
    val snippetRemoveOverlayDesc: String get() = SnippetStrings.snippetRemoveOverlayDesc
    val snippetCssAdBlock: String get() = SnippetStrings.snippetCssAdBlock
    val snippetCssAdBlockDesc: String get() = SnippetStrings.snippetCssAdBlockDesc
    val snippetAntiAdblock: String get() = SnippetStrings.snippetAntiAdblock
    val snippetAntiAdblockDesc: String get() = SnippetStrings.snippetAntiAdblockDesc
    
    // Note.
    val snippetUtility: String get() = SnippetStrings.snippetUtility
    val snippetUtilityDesc: String get() = SnippetStrings.snippetUtilityDesc
    val snippetDebounce: String get() = SnippetStrings.snippetDebounce
    val snippetDebounceDesc: String get() = SnippetStrings.snippetDebounceDesc
    val snippetThrottle: String get() = SnippetStrings.snippetThrottle
    val snippetThrottleDesc: String get() = SnippetStrings.snippetThrottleDesc
    val snippetWaitElement: String get() = SnippetStrings.snippetWaitElement
    val snippetWaitElementDesc: String get() = SnippetStrings.snippetWaitElementDesc
    val snippetCopyText: String get() = SnippetStrings.snippetCopyText
    val snippetCopyTextDesc: String get() = SnippetStrings.snippetCopyTextDesc
    val snippetFormatDate: String get() = SnippetStrings.snippetFormatDate
    val snippetFormatDateDesc: String get() = SnippetStrings.snippetFormatDateDesc
    val snippetRandomString: String get() = SnippetStrings.snippetRandomString
    val snippetRandomStringDesc: String get() = SnippetStrings.snippetRandomStringDesc
    val snippetSleep: String get() = SnippetStrings.snippetSleep
    val snippetSleepDesc: String get() = SnippetStrings.snippetSleepDesc
    val snippetRetry: String get() = SnippetStrings.snippetRetry
    val snippetRetryDesc: String get() = SnippetStrings.snippetRetryDesc
    
    // Note.
    val snippetText: String get() = SnippetStrings.snippetText
    val snippetTextDesc: String get() = SnippetStrings.snippetTextDesc
    val snippetExtractArticle: String get() = SnippetStrings.snippetExtractArticle
    val snippetExtractArticleDesc: String get() = SnippetStrings.snippetExtractArticleDesc
    val snippetReplaceText: String get() = SnippetStrings.snippetReplaceText
    val snippetReplaceTextDesc: String get() = SnippetStrings.snippetReplaceTextDesc
    val snippetTranslateSelection: String get() = SnippetStrings.snippetTranslateSelection
    val snippetTranslateSelectionDesc: String get() = SnippetStrings.snippetTranslateSelectionDesc
    val snippetHtmlToMarkdown: String get() = SnippetStrings.snippetHtmlToMarkdown
    val snippetHtmlToMarkdownDesc: String get() = SnippetStrings.snippetHtmlToMarkdownDesc
    
    // Request.
    val snippetIntercept: String get() = SnippetStrings.snippetIntercept
    val snippetInterceptDesc: String get() = SnippetStrings.snippetInterceptDesc
    val snippetInterceptFetch: String get() = SnippetStrings.snippetInterceptFetch
    val snippetInterceptFetchDesc: String get() = SnippetStrings.snippetInterceptFetchDesc
    val snippetInterceptXhr: String get() = SnippetStrings.snippetInterceptXhr
    val snippetInterceptXhrDesc: String get() = SnippetStrings.snippetInterceptXhrDesc
    val snippetInterceptWebSocket: String get() = SnippetStrings.snippetInterceptWebSocket
    val snippetInterceptWebSocketDesc: String get() = SnippetStrings.snippetInterceptWebSocketDesc
    val snippetBlockRequests: String get() = SnippetStrings.snippetBlockRequests
    val snippetBlockRequestsDesc: String get() = SnippetStrings.snippetBlockRequestsDesc
    
    // Auto.
    val snippetAutomation: String get() = SnippetStrings.snippetAutomation
    val snippetAutomationDesc: String get() = SnippetStrings.snippetAutomationDesc
    val snippetAutoClick: String get() = SnippetStrings.snippetAutoClick
    val snippetAutoClickDesc: String get() = SnippetStrings.snippetAutoClickDesc
    val snippetAutoClickInterval: String get() = SnippetStrings.snippetAutoClickInterval
    val snippetAutoClickIntervalDesc: String get() = SnippetStrings.snippetAutoClickIntervalDesc
    val snippetAutoFillSubmit: String get() = SnippetStrings.snippetAutoFillSubmit
    val snippetAutoFillSubmitDesc: String get() = SnippetStrings.snippetAutoFillSubmitDesc
    val snippetAutoRefresh: String get() = SnippetStrings.snippetAutoRefresh
    val snippetAutoRefreshDesc: String get() = SnippetStrings.snippetAutoRefreshDesc
    val snippetAutoScrollLoad: String get() = SnippetStrings.snippetAutoScrollLoad
    val snippetAutoScrollLoadDesc: String get() = SnippetStrings.snippetAutoScrollLoadDesc
    val snippetAutoLoginCheck: String get() = SnippetStrings.snippetAutoLoginCheck
    val snippetAutoLoginCheckDesc: String get() = SnippetStrings.snippetAutoLoginCheckDesc
    
    // Debug.
    val snippetDebug: String get() = SnippetStrings.snippetDebug
    val snippetDebugDesc: String get() = SnippetStrings.snippetDebugDesc
    val snippetConsolePanel: String get() = SnippetStrings.snippetConsolePanel
    val snippetConsolePanelDesc: String get() = SnippetStrings.snippetConsolePanelDesc
    val snippetElementInfo: String get() = SnippetStrings.snippetElementInfo
    val snippetElementInfoDesc: String get() = SnippetStrings.snippetElementInfoDesc
    val snippetPerformance: String get() = SnippetStrings.snippetPerformance
    val snippetPerformanceDesc: String get() = SnippetStrings.snippetPerformanceDesc
    val snippetNetworkLog: String get() = SnippetStrings.snippetNetworkLog
    val snippetNetworkLogDesc: String get() = SnippetStrings.snippetNetworkLogDesc
    
    // ==================== Module Templates ====================
    val templateColorTheme: String get() = ModuleStrings.templateColorTheme
    val templateColorThemeDesc: String get() = ModuleStrings.templateColorThemeDesc
    val templateBgColor: String get() = ModuleStrings.templateBgColor
    val templateTextColor: String get() = ModuleStrings.templateTextColor
    val templateLinkColor: String get() = ModuleStrings.templateLinkColor
    val templateLayoutFixer: String get() = ModuleStrings.templateLayoutFixer
    val templateLayoutFixerDesc: String get() = ModuleStrings.templateLayoutFixerDesc
    val templateMaxWidth: String get() = ModuleStrings.templateMaxWidth
    val templateCenterContent: String get() = ModuleStrings.templateCenterContent
    val templateAutoClicker: String get() = ModuleStrings.templateAutoClicker
    val templateAutoClickerDesc: String get() = ModuleStrings.templateAutoClickerDesc
    val templateClickTarget: String get() = ModuleStrings.templateClickTarget
    val templateDelay: String get() = ModuleStrings.templateDelay
    val templateRepeatClick: String get() = ModuleStrings.templateRepeatClick
    val templateFormFiller: String get() = ModuleStrings.templateFormFiller
    val templateFormFillerDesc: String get() = ModuleStrings.templateFormFillerDesc
    val templateFieldSelector: String get() = ModuleStrings.templateFieldSelector
    val templateFieldValue: String get() = ModuleStrings.templateFieldValue
    val templatePageModifier: String get() = ModuleStrings.templatePageModifier
    val templatePageModifierDesc: String get() = ModuleStrings.templatePageModifierDesc
    val templateTargetSelector: String get() = ModuleStrings.templateTargetSelector
    val templateNewText: String get() = ModuleStrings.templateNewText
    val templateNewStyle: String get() = ModuleStrings.templateNewStyle
    val templateCustomButton: String get() = ModuleStrings.templateCustomButton
    val templateCustomButtonDesc: String get() = ModuleStrings.templateCustomButtonDesc
    val templateButtonText: String get() = ModuleStrings.templateButtonText
    val templateClickAction: String get() = ModuleStrings.templateClickAction
    val templatePosition: String get() = ModuleStrings.templatePosition
    val templateKeyboardShortcuts: String get() = ModuleStrings.templateKeyboardShortcuts
    val templateKeyboardShortcutsDesc: String get() = ModuleStrings.templateKeyboardShortcutsDesc
    val templateShortcutsConfig: String get() = ModuleStrings.templateShortcutsConfig
    val templateShortcutsConfigDesc: String get() = ModuleStrings.templateShortcutsConfigDesc
    val templateExtractAttrDesc: String get() = ModuleStrings.templateExtractAttrDesc
    val templateFilterKeywordDesc: String get() = ModuleStrings.templateFilterKeywordDesc
    val aiErrorNoApiKey: String get() = AiStrings.aiErrorNoApiKey
    val aiErrorNoModel: String get() = AiStrings.aiErrorNoModel
    val aiErrorNoApiKeyForModel: String get() = AiStrings.aiErrorNoApiKeyForModel
    val aiErrorUnknown: String get() = AiStrings.aiErrorUnknown
    val aiGeneratedModule: String get() = AiStrings.aiGeneratedModule
    val aiGeneratedModuleDesc: String get() = AiStrings.aiGeneratedModuleDesc

    // ==================== Module Management Errors ====================
    val errModuleNotFound: String get() = ExtensionStrings.errModuleNotFound
    val errNoModulesToExport: String get() = ExtensionStrings.errNoModulesToExport
    val errInvalidModuleFile: String get() = ExtensionStrings.errInvalidModuleFile
    val errInvalidShareCode: String get() = ExtensionStrings.errInvalidShareCode
    val errInvalidModulePackage: String get() = ExtensionStrings.errInvalidModulePackage
    val errCannotOpenOutputStream: String get() = ExtensionStrings.errCannotOpenOutputStream
    val shareModuleTitle: String get() = ExtensionStrings.shareModuleTitle
    val shareModuleName: String get() = ExtensionStrings.shareModuleName
    val shareModuleDesc: String get() = ExtensionStrings.shareModuleDesc
    val shareModuleCategory: String get() = ExtensionStrings.shareModuleCategory
    val shareModuleVersion: String get() = ExtensionStrings.shareModuleVersion
    val shareModuleCode: String get() = ExtensionStrings.shareModuleCode
    val shareModuleHowTo: String get() = ExtensionStrings.shareModuleHowTo
    val shareModuleSubject: String get() = ExtensionStrings.shareModuleSubject
    val moduleCopySuffix: String get() = ModuleStrings.moduleCopySuffix

    // ==================== ====================
    val validateNameEmpty: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块名称不能为空"
        AppLanguage.ENGLISH -> "Module name cannot be empty"
        AppLanguage.ARABIC -> "لا يمكن أن يكون اسم الوحدة فارغاً"
    }
    val validateCodeEmpty: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码内容不能为空"
        AppLanguage.ENGLISH -> "Code content cannot be empty"
        AppLanguage.ARABIC -> "لا يمكن أن يكون محتوى الكود فارغاً"
    }
    val validateConfigRequired: String get() = when (lang) {
        AppLanguage.CHINESE -> "配置项 '%s' 为必填项"
        AppLanguage.ENGLISH -> "Config item '%s' is required"
        AppLanguage.ARABIC -> "عنصر التكوين '%s' مطلوب"
    }

    // ==================== Module Preset Factory ====================
    val presetBlockElements: String get() = ExtensionStrings.presetBlockElements
    val presetInjectStyle: String get() = ExtensionStrings.presetInjectStyle
    val presetAutoClick: String get() = ExtensionStrings.presetAutoClick
    val presetFloatingButton: String get() = ExtensionStrings.presetFloatingButton
    val tagBlock: String get() = ExtensionStrings.tagBlock
    val tagHideElement: String get() = ExtensionStrings.tagHideElement
    val tagStyleCss: String get() = ExtensionStrings.tagStyleCss
    val tagAuto: String get() = ExtensionStrings.tagAuto
    val tagClickAction: String get() = ExtensionStrings.tagClickAction
    val tagButton: String get() = ExtensionStrings.tagButton
    val tagFloatingWidget: String get() = ExtensionStrings.tagFloatingWidget
    val builtInVersion: String get() = ExtensionStrings.builtInVersion

    // ==================== Agent ====================
    val agentStateIdle: String get() = when (lang) {
        AppLanguage.CHINESE -> "空闲"
        AppLanguage.ENGLISH -> "Idle"
        AppLanguage.ARABIC -> "خامل"
    }
    val agentStateThinking: String get() = when (lang) {
        AppLanguage.CHINESE -> "思考中"
        AppLanguage.ENGLISH -> "Thinking"
        AppLanguage.ARABIC -> "يفكر"
    }
    val agentStateGenerating: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成中"
        AppLanguage.ENGLISH -> "Generating"
        AppLanguage.ARABIC -> "يولّد"
    }
    val agentStateToolCalling: String get() = when (lang) {
        AppLanguage.CHINESE -> "调用工具"
        AppLanguage.ENGLISH -> "Calling Tool"
        AppLanguage.ARABIC -> "استدعاء أداة"
    }
    val agentStateSyntaxCheck: String get() = when (lang) {
        AppLanguage.CHINESE -> "语法检查"
        AppLanguage.ENGLISH -> "Syntax Check"
        AppLanguage.ARABIC -> "فحص القواعد"
    }
    val agentStateFixing: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复中"
        AppLanguage.ENGLISH -> "Fixing"
        AppLanguage.ARABIC -> "يصلح"
    }
    val agentStateSecurityScan: String get() = when (lang) {
        AppLanguage.CHINESE -> "安全扫描"
        AppLanguage.ENGLISH -> "Security Scan"
        AppLanguage.ARABIC -> "فحص أمني"
    }
    val agentStateCompleted: String get() = when (lang) {
        AppLanguage.CHINESE -> "完成"
        AppLanguage.ENGLISH -> "Completed"
        AppLanguage.ARABIC -> "مكتمل"
    }
    val agentStateError: String get() = when (lang) {
        AppLanguage.CHINESE -> "错误"
        AppLanguage.ENGLISH -> "Error"
        AppLanguage.ARABIC -> "خطأ"
    }

    // ==================== ====================
    val thoughtAnalysis: String get() = when (lang) {
        AppLanguage.CHINESE -> "需求分析"
        AppLanguage.ENGLISH -> "Analysis"
        AppLanguage.ARABIC -> "تحليل"
    }
    val thoughtPlanning: String get() = when (lang) {
        AppLanguage.CHINESE -> "制定计划"
        AppLanguage.ENGLISH -> "Planning"
        AppLanguage.ARABIC -> "تخطيط"
    }
    val thoughtToolCall: String get() = when (lang) {
        AppLanguage.CHINESE -> "调用工具"
        AppLanguage.ENGLISH -> "Tool Call"
        AppLanguage.ARABIC -> "استدعاء أداة"
    }
    val thoughtToolResult: String get() = when (lang) {
        AppLanguage.CHINESE -> "工具结果"
        AppLanguage.ENGLISH -> "Tool Result"
        AppLanguage.ARABIC -> "نتيجة الأداة"
    }
    val thoughtGeneration: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成代码"
        AppLanguage.ENGLISH -> "Code Generation"
        AppLanguage.ARABIC -> "توليد الكود"
    }
    val thoughtReview: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码审查"
        AppLanguage.ENGLISH -> "Code Review"
        AppLanguage.ARABIC -> "مراجعة الكود"
    }
    val thoughtFix: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复问题"
        AppLanguage.ENGLISH -> "Fix Issues"
        AppLanguage.ARABIC -> "إصلاح المشاكل"
    }
    val thoughtConclusion: String get() = when (lang) {
        AppLanguage.CHINESE -> "总结"
        AppLanguage.ENGLISH -> "Conclusion"
        AppLanguage.ARABIC -> "خاتمة"
    }

    // ==================== Agent ====================
    val toolErrUnknown: String get() = AiCodingStrings.toolErrUnknown
    val toolErrExecFailed: String get() = AiCodingStrings.toolErrExecFailed
    val toolErrChainFailed: String get() = AiCodingStrings.toolErrChainFailed
    val toolErrSyntaxCheckFailed: String get() = AiCodingStrings.toolErrSyntaxCheckFailed
    val toolErrAutoFixFailed: String get() = AiCodingStrings.toolErrAutoFixFailed
    val toolErrMaxFixAttempts: String get() = AiCodingStrings.toolErrMaxFixAttempts
    val toolErrMissingCode: String get() = AiCodingStrings.toolErrMissingCode
    val toolErrUnsupportedLang: String get() = AiCodingStrings.toolErrUnsupportedLang
    val toolErrMissingModuleName: String get() = AiCodingStrings.toolErrMissingModuleName
    val toolModuleCreated: String get() = AiCodingStrings.toolModuleCreated
    val toolModuleCreateFailed: String get() = AiCodingStrings.toolModuleCreateFailed

    // ==================== ====================
    val syntaxBraceMismatch: String get() = AiCodingStrings.syntaxBraceMismatch
    val syntaxBraceMissing: String get() = AiCodingStrings.syntaxBraceMissing
    val syntaxBraceExtra: String get() = AiCodingStrings.syntaxBraceExtra
    val syntaxBraceCheckPair: String get() = AiCodingStrings.syntaxBraceCheckPair
    val syntaxParenMismatch: String get() = AiCodingStrings.syntaxParenMismatch
    val syntaxParenMissing: String get() = AiCodingStrings.syntaxParenMissing
    val syntaxParenExtra: String get() = AiCodingStrings.syntaxParenExtra
    val syntaxParenCheckPair: String get() = AiCodingStrings.syntaxParenCheckPair
    val syntaxBracketMismatch: String get() = AiCodingStrings.syntaxBracketMismatch
    val syntaxBracketMissing: String get() = AiCodingStrings.syntaxBracketMissing
    val syntaxBracketExtra: String get() = AiCodingStrings.syntaxBracketExtra
    val syntaxBracketCheckPair: String get() = AiCodingStrings.syntaxBracketCheckPair

    // ==================== Lint ====================
    val lintNoVar: String get() = AiCodingStrings.lintNoVar
    val lintEqeqeq: String get() = AiCodingStrings.lintEqeqeq
    val lintNoEval: String get() = AiCodingStrings.lintNoEval
    val lintNoDocWrite: String get() = AiCodingStrings.lintNoDocWrite
    val lintNoConsole: String get() = AiCodingStrings.lintNoConsole
    val lintCssMissingSemicolon: String get() = AiCodingStrings.lintCssMissingSemicolon
    val lintCssNoImportant: String get() = AiCodingStrings.lintCssNoImportant
    val lintUseStrict: String get() = AiCodingStrings.lintUseStrict
    val lintUseArrowFn: String get() = AiCodingStrings.lintUseArrowFn
    val lintLineTooLong: String get() = AiCodingStrings.lintLineTooLong

    // ==================== ====================
    val fixVarToLet: String get() = when (lang) {
        AppLanguage.CHINESE -> "将 var 替换为 let"
        AppLanguage.ENGLISH -> "Replaced var with let"
        AppLanguage.ARABIC -> "تم استبدال var بـ let"
    }
    val fixLooseEq: String get() = when (lang) {
        AppLanguage.CHINESE -> "将 == 替换为 ==="
        AppLanguage.ENGLISH -> "Replaced == with ==="
        AppLanguage.ARABIC -> "تم استبدال == بـ ==="
    }

    // ==================== ====================
    val validateConfigMissingKey: String get() = when (lang) {
        AppLanguage.CHINESE -> "配置项缺少 key"
        AppLanguage.ENGLISH -> "Config item missing key"
        AppLanguage.ARABIC -> "عنصر التكوين يفتقر إلى المفتاح"
    }
    val validateConfigMissingName: String get() = when (lang) {
        AppLanguage.CHINESE -> "配置项缺少显示名称"
        AppLanguage.ENGLISH -> "Config item missing display name"
        AppLanguage.ARABIC -> "عنصر التكوين يفتقر إلى اسم العرض"
    }
    val validateConfigRequiredNotSet: String get() = when (lang) {
        AppLanguage.CHINESE -> "必填配置项 '%s' 未设置值"
        AppLanguage.ENGLISH -> "Required config item '%s' has no value"
        AppLanguage.ARABIC -> "عنصر التكوين المطلوب '%s' بدون قيمة"
    }

    // ==================== ====================
    val secEvalDesc: String get() = AiCodingStrings.secEvalDesc
    val secEvalRec: String get() = AiCodingStrings.secEvalRec
    val secInnerHtmlDesc: String get() = AiCodingStrings.secInnerHtmlDesc
    val secInnerHtmlRec: String get() = AiCodingStrings.secInnerHtmlRec
    val secDocWriteDesc: String get() = AiCodingStrings.secDocWriteDesc
    val secDocWriteRec: String get() = AiCodingStrings.secDocWriteRec
    val secNewFuncDesc: String get() = AiCodingStrings.secNewFuncDesc
    val secNewFuncRec: String get() = AiCodingStrings.secNewFuncRec
    val secLocationDesc: String get() = AiCodingStrings.secLocationDesc
    val secLocationRec: String get() = AiCodingStrings.secLocationRec
    val secStorageDesc: String get() = AiCodingStrings.secStorageDesc
    val secStorageRec: String get() = AiCodingStrings.secStorageRec
    val secFetchDesc: String get() = AiCodingStrings.secFetchDesc
    val secFetchRec: String get() = AiCodingStrings.secFetchRec
    val secPostMsgDesc: String get() = AiCodingStrings.secPostMsgDesc
    val secPostMsgRec: String get() = AiCodingStrings.secPostMsgRec
    val secTemplateDesc: String get() = AiCodingStrings.secTemplateDesc
    val secTemplateRec: String get() = AiCodingStrings.secTemplateRec
    val secBase64Desc: String get() = AiCodingStrings.secBase64Desc
    val secBase64Rec: String get() = AiCodingStrings.secBase64Rec

    // ==================== ====================
    val paramCodeToCheck: String get() = AiCodingStrings.paramCodeToCheck
    val paramCodeLang: String get() = AiCodingStrings.paramCodeLang
    val paramCodeToScan: String get() = AiCodingStrings.paramCodeToScan
    val paramRequirement: String get() = AiCodingStrings.paramRequirement
    val paramTargetLang: String get() = AiCodingStrings.paramTargetLang
    val paramContext: String get() = AiCodingStrings.paramContext
    val paramCodeWithErrors: String get() = AiCodingStrings.paramCodeWithErrors
    val paramErrorList: String get() = AiCodingStrings.paramErrorList
    val paramCodeToRefactor: String get() = AiCodingStrings.paramCodeToRefactor
    val paramRefactorGoals: String get() = AiCodingStrings.paramRefactorGoals
    val paramTestUrl: String get() = AiCodingStrings.paramTestUrl
    val paramConfigItems: String get() = AiCodingStrings.paramConfigItems
    val paramConfigValues: String get() = AiCodingStrings.paramConfigValues
    val paramKeywords: String get() = AiCodingStrings.paramKeywords
    val paramSearchKeyword: String get() = AiCodingStrings.paramSearchKeyword
    val paramSnippetCategory: String get() = AiCodingStrings.paramSnippetCategory
    val paramModuleIcon: String get() = AiCodingStrings.paramModuleIcon
    val paramRunAt: String get() = AiCodingStrings.paramRunAt
    val paramModuleId: String get() = AiCodingStrings.paramModuleId
    val paramPreviewUrl: String get() = AiCodingStrings.paramPreviewUrl

    // ==================== Agent Reasoning Messages ====================
    val agentAnalyzing: String get() = AiStrings.agentAnalyzing
    val agentPlanning: String get() = AiStrings.agentPlanning
    val agentCallingAi: String get() = AiStrings.agentCallingAi
    val agentAiCallFailed: String get() = AiStrings.agentAiCallFailed
    val agentParsing: String get() = AiStrings.agentParsing
    val agentParseFailed: String get() = AiStrings.agentParseFailed
    val agentSyntaxChecking: String get() = AiStrings.agentSyntaxChecking
    val agentFoundErrors: String get() = AiStrings.agentFoundErrors
    val agentAutoFixing: String get() = AiStrings.agentAutoFixing
    val agentErrorsFixed: String get() = AiStrings.agentErrorsFixed
    val agentSyntaxPassed: String get() = AiStrings.agentSyntaxPassed
    val agentSecurityScanning: String get() = AiStrings.agentSecurityScanning
    val agentSecurityIssues: String get() = AiStrings.agentSecurityIssues
    val agentSecurityPassed: String get() = AiStrings.agentSecurityPassed
    val agentModuleCompleted: String get() = AiStrings.agentModuleCompleted
    val agentModuleGenerated: String get() = AiStrings.agentModuleGenerated
    val agentAutoFixFailed: String get() = AiStrings.agentAutoFixFailed
    val agentRequestTimeout: String get() = AiStrings.agentRequestTimeout
    val agentToolFailed: String get() = AiStrings.agentToolFailed
    val versionMultiUi: String get() = AiStrings.versionMultiUi
    val versionV4Ui: String get() = AiStrings.versionV4Ui
    val runModeInteractive: String get() = AiStrings.runModeInteractive
    val runModeAuto: String get() = AiStrings.runModeAuto
    val runModeInteractiveDesc: String get() = AiStrings.runModeInteractiveDesc
    val runModeAutoDesc: String get() = AiStrings.runModeAutoDesc
    val runModeLabel: String get() = AiStrings.runModeLabel
    val tagSelectedText: String get() = AiStrings.tagSelectedText
    val templateAutoRefresh: String get() = AiStrings.templateAutoRefresh
    val templateAutoRefreshDesc: String get() = AiStrings.templateAutoRefreshDesc
    val templateRefreshInterval: String get() = AiStrings.templateRefreshInterval
    val templateShowCountdown: String get() = AiStrings.templateShowCountdown
    val templateScrollToTop: String get() = AiStrings.templateScrollToTop
    val templateScrollToTopDesc: String get() = AiStrings.templateScrollToTopDesc
    val templateShowAfterScroll: String get() = AiStrings.templateShowAfterScroll
    val templateDataExtractor: String get() = AiStrings.templateDataExtractor
    val templateDataExtractorDesc: String get() = AiStrings.templateDataExtractorDesc
    val templateDataSelector: String get() = AiStrings.templateDataSelector
    val templateExtractAttribute: String get() = AiStrings.templateExtractAttribute
    val templateLinkCollector: String get() = AiStrings.templateLinkCollector
    val templateLinkCollectorDesc: String get() = AiStrings.templateLinkCollectorDesc
    val templateFilterKeyword: String get() = AiStrings.templateFilterKeyword
    val templateImageGrabber: String get() = AiStrings.templateImageGrabber
    val templateImageGrabberDesc: String get() = AiStrings.templateImageGrabberDesc
    val templateMinSize: String get() = AiStrings.templateMinSize
    val templateVideoEnhancer: String get() = AiStrings.templateVideoEnhancer
    val templateVideoEnhancerDesc: String get() = AiStrings.templateVideoEnhancerDesc
    val templateDefaultSpeed: String get() = AiStrings.templateDefaultSpeed
    val templateShowControlPanel: String get() = AiStrings.templateShowControlPanel
    val templateImageZoomer: String get() = AiStrings.templateImageZoomer
    val templateImageZoomerDesc: String get() = AiStrings.templateImageZoomerDesc
    val templateAudioController: String get() = AiStrings.templateAudioController
    val templateAudioControllerDesc: String get() = AiStrings.templateAudioControllerDesc
    val templateDefaultVolume: String get() = AiStrings.templateDefaultVolume
    val templateNotificationBlocker: String get() = AiStrings.templateNotificationBlocker
    val templateNotificationBlockerDesc: String get() = AiStrings.templateNotificationBlockerDesc
    val templateTrackingBlocker: String get() = AiStrings.templateTrackingBlocker
    val templateTrackingBlockerDesc: String get() = AiStrings.templateTrackingBlockerDesc
    val templateFingerprintProtector: String get() = AiStrings.templateFingerprintProtector
    val templateFingerprintProtectorDesc: String get() = AiStrings.templateFingerprintProtectorDesc
    val templateConsoleLogger: String get() = AiStrings.templateConsoleLogger
    val templateConsoleLoggerDesc: String get() = AiStrings.templateConsoleLoggerDesc
    val templateMaxLogs: String get() = AiStrings.templateMaxLogs
    val templateNetworkMonitor: String get() = AiStrings.templateNetworkMonitor
    val templateNetworkMonitorDesc: String get() = AiStrings.templateNetworkMonitorDesc
    val templateDomInspector: String get() = AiStrings.templateDomInspector
    val templateDomInspectorDesc: String get() = AiStrings.templateDomInspectorDesc
    
    // ==================== Built-in Modules ====================
    val builtinVideoDownloader: String get() = ExtensionStrings.builtinVideoDownloader
    val builtinVideoDownloaderDesc: String get() = ExtensionStrings.builtinVideoDownloaderDesc
    val builtinDouyinExtractor: String get() = ExtensionStrings.builtinDouyinExtractor
    val builtinDouyinExtractorDesc: String get() = ExtensionStrings.builtinDouyinExtractorDesc
    val builtinXiaohongshuExtractor: String get() = ExtensionStrings.builtinXiaohongshuExtractor
    val builtinXiaohongshuExtractorDesc: String get() = ExtensionStrings.builtinXiaohongshuExtractorDesc
    val builtinVideoEnhancer: String get() = ExtensionStrings.builtinVideoEnhancer
    val builtinVideoEnhancerDesc: String get() = ExtensionStrings.builtinVideoEnhancerDesc
    val builtinWebAnalyzer: String get() = ExtensionStrings.builtinWebAnalyzer
    val builtinWebAnalyzerDesc: String get() = ExtensionStrings.builtinWebAnalyzerDesc
    val builtinDarkMode: String get() = ExtensionStrings.builtinDarkMode
    val builtinDarkModeDesc: String get() = ExtensionStrings.builtinDarkModeDesc
    val builtinPrivacyProtection: String get() = ExtensionStrings.builtinPrivacyProtection
    val builtinPrivacyProtectionDesc: String get() = ExtensionStrings.builtinPrivacyProtectionDesc
    val builtinContentEnhancer: String get() = ExtensionStrings.builtinContentEnhancer
    val builtinContentEnhancerDesc: String get() = ExtensionStrings.builtinContentEnhancerDesc
    val builtinElementBlocker: String get() = ExtensionStrings.builtinElementBlocker
    val builtinElementBlockerDesc: String get() = ExtensionStrings.builtinElementBlockerDesc
    
    // ==================== Chrome ====================
    val builtinBewlyCat: String get() = when (lang) {
        AppLanguage.CHINESE -> "BewlyCat (B站增强)"
        AppLanguage.ENGLISH -> "BewlyCat (Bilibili Enhanced)"
        AppLanguage.ARABIC -> "BewlyCat (تحسين Bilibili)"
    }
    val builtinBewlyCatDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "为 B站提供美观的界面增强，优化首页、搜索、视频页面等"
        AppLanguage.ENGLISH -> "Beautiful UI enhancements for Bilibili: improved homepage, search, video pages and more"
        AppLanguage.ARABIC -> "تحسينات واجهة جميلة لـ Bilibili: الصفحة الرئيسية والبحث وصفحات الفيديو والمزيد"
    }
    
    // ==================== Module Triggers ====================
    val triggerAuto: String get() = ExtensionStrings.triggerAuto
    val triggerAutoDesc: String get() = ExtensionStrings.triggerAutoDesc
    val triggerManual: String get() = ExtensionStrings.triggerManual
    val triggerManualDesc: String get() = ExtensionStrings.triggerManualDesc
    val triggerInterval: String get() = ExtensionStrings.triggerInterval
    val triggerIntervalDesc: String get() = ExtensionStrings.triggerIntervalDesc
    val triggerMutation: String get() = ExtensionStrings.triggerMutation
    val triggerMutationDesc: String get() = ExtensionStrings.triggerMutationDesc
    val triggerScroll: String get() = ExtensionStrings.triggerScroll
    val triggerScrollDesc: String get() = ExtensionStrings.triggerScrollDesc
    val triggerClick: String get() = ExtensionStrings.triggerClick
    val triggerClickDesc: String get() = ExtensionStrings.triggerClickDesc
    val triggerHover: String get() = ExtensionStrings.triggerHover
    val triggerHoverDesc: String get() = ExtensionStrings.triggerHoverDesc
    val triggerFocus: String get() = ExtensionStrings.triggerFocus
    val triggerFocusDesc: String get() = ExtensionStrings.triggerFocusDesc
    val triggerInput: String get() = ExtensionStrings.triggerInput
    val triggerInputDesc: String get() = ExtensionStrings.triggerInputDesc
    val triggerVisibility: String get() = ExtensionStrings.triggerVisibility
    val triggerVisibilityDesc: String get() = ExtensionStrings.triggerVisibilityDesc
    
    // ==================== Module Permissions ====================
    val permDomAccess: String get() = ModuleStrings.permDomAccess
    val permDomAccessDesc: String get() = ModuleStrings.permDomAccessDesc
    val permDomObserve: String get() = ModuleStrings.permDomObserve
    val permDomObserveDesc: String get() = ModuleStrings.permDomObserveDesc
    val permCssInject: String get() = ModuleStrings.permCssInject
    val permCssInjectDesc: String get() = ModuleStrings.permCssInjectDesc
    val permStorage: String get() = ModuleStrings.permStorage
    val permStorageDesc: String get() = ModuleStrings.permStorageDesc
    val permCookie: String get() = ModuleStrings.permCookie
    val permCookieDesc: String get() = ModuleStrings.permCookieDesc
    val permIndexedDb: String get() = ModuleStrings.permIndexedDb
    val permIndexedDbDesc: String get() = ModuleStrings.permIndexedDbDesc
    val permCache: String get() = ModuleStrings.permCache
    val permCacheDesc: String get() = ModuleStrings.permCacheDesc
    val permNetwork: String get() = ModuleStrings.permNetwork
    val permNetworkDesc: String get() = ModuleStrings.permNetworkDesc
    val permWebsocket: String get() = ModuleStrings.permWebsocket
    val permWebsocketDesc: String get() = ModuleStrings.permWebsocketDesc
    val permFetchIntercept: String get() = ModuleStrings.permFetchIntercept
    val permFetchInterceptDesc: String get() = ModuleStrings.permFetchInterceptDesc
    val permClipboard: String get() = ModuleStrings.permClipboard
    val permClipboardDesc: String get() = ModuleStrings.permClipboardDesc
    val permNotification: String get() = ModuleStrings.permNotification
    val permNotificationDesc: String get() = ModuleStrings.permNotificationDesc
    val permAlert: String get() = ModuleStrings.permAlert
    val permAlertDesc: String get() = ModuleStrings.permAlertDesc
    val permKeyboard: String get() = ModuleStrings.permKeyboard
    val permKeyboardDesc: String get() = ModuleStrings.permKeyboardDesc
    val permMouse: String get() = ModuleStrings.permMouse
    val permMouseDesc: String get() = ModuleStrings.permMouseDesc
    val permTouch: String get() = ModuleStrings.permTouch
    val permTouchDesc: String get() = ModuleStrings.permTouchDesc
    val permLocation: String get() = ModuleStrings.permLocation
    val permLocationDesc: String get() = ModuleStrings.permLocationDesc
    val permCamera: String get() = ModuleStrings.permCamera
    val permCameraDesc: String get() = ModuleStrings.permCameraDesc
    val permMicrophone: String get() = ModuleStrings.permMicrophone
    val permMicrophoneDesc: String get() = ModuleStrings.permMicrophoneDesc
    val permDeviceInfo: String get() = ModuleStrings.permDeviceInfo
    val permDeviceInfoDesc: String get() = ModuleStrings.permDeviceInfoDesc
    val permMedia: String get() = ModuleStrings.permMedia
    val permMediaDesc: String get() = ModuleStrings.permMediaDesc
    val permFullscreen: String get() = ModuleStrings.permFullscreen
    val permFullscreenDesc: String get() = ModuleStrings.permFullscreenDesc
    val permPip: String get() = ModuleStrings.permPip
    val permPipDesc: String get() = ModuleStrings.permPipDesc
    val permScreenCapture: String get() = ModuleStrings.permScreenCapture
    val permScreenCaptureDesc: String get() = ModuleStrings.permScreenCaptureDesc
    val permDownload: String get() = ModuleStrings.permDownload
    val permDownloadDesc: String get() = ModuleStrings.permDownloadDesc
    val permFileAccess: String get() = ModuleStrings.permFileAccess
    val permFileAccessDesc: String get() = ModuleStrings.permFileAccessDesc
    val permEval: String get() = ModuleStrings.permEval
    val permEvalDesc: String get() = ModuleStrings.permEvalDesc
    val permIframe: String get() = ModuleStrings.permIframe
    val permIframeDesc: String get() = ModuleStrings.permIframeDesc
    val permWindowOpen: String get() = ModuleStrings.permWindowOpen
    val permWindowOpenDesc: String get() = ModuleStrings.permWindowOpenDesc
    val permHistory: String get() = ModuleStrings.permHistory
    val permHistoryDesc: String get() = ModuleStrings.permHistoryDesc
    val permNavigation: String get() = ModuleStrings.permNavigation
    val permNavigationDesc: String get() = ModuleStrings.permNavigationDesc
    
    // ==================== Config Item Types ====================
    val configTypeText: String get() = ModuleStrings.configTypeText
    val configTypeTextDesc: String get() = ModuleStrings.configTypeTextDesc
    val configTypeTextarea: String get() = ModuleStrings.configTypeTextarea
    val configTypeTextareaDesc: String get() = ModuleStrings.configTypeTextareaDesc
    val configTypeNumber: String get() = ModuleStrings.configTypeNumber
    val configTypeNumberDesc: String get() = ModuleStrings.configTypeNumberDesc
    val configTypeBoolean: String get() = ModuleStrings.configTypeBoolean
    val configTypeBooleanDesc: String get() = ModuleStrings.configTypeBooleanDesc
    val configTypeSelect: String get() = ModuleStrings.configTypeSelect
    val configTypeSelectDesc: String get() = ModuleStrings.configTypeSelectDesc
    val configTypeMultiSelect: String get() = ModuleStrings.configTypeMultiSelect
    val configTypeMultiSelectDesc: String get() = ModuleStrings.configTypeMultiSelectDesc
    val configTypeRadio: String get() = ModuleStrings.configTypeRadio
    val configTypeRadioDesc: String get() = ModuleStrings.configTypeRadioDesc
    val configTypeCheckbox: String get() = ModuleStrings.configTypeCheckbox
    val configTypeCheckboxDesc: String get() = ModuleStrings.configTypeCheckboxDesc
    val configTypeColor: String get() = ModuleStrings.configTypeColor
    val configTypeColorDesc: String get() = ModuleStrings.configTypeColorDesc
    val configTypeUrl: String get() = ModuleStrings.configTypeUrl
    val configTypeUrlDesc: String get() = ModuleStrings.configTypeUrlDesc
    val configTypeEmail: String get() = ModuleStrings.configTypeEmail
    val configTypeEmailDesc: String get() = ModuleStrings.configTypeEmailDesc
    val configTypePassword: String get() = ModuleStrings.configTypePassword
    val configTypePasswordDesc: String get() = ModuleStrings.configTypePasswordDesc
    val configTypeRegex: String get() = ModuleStrings.configTypeRegex
    val configTypeRegexDesc: String get() = ModuleStrings.configTypeRegexDesc
    val configTypeCssSelector: String get() = ModuleStrings.configTypeCssSelector
    val configTypeCssSelectorDesc: String get() = ModuleStrings.configTypeCssSelectorDesc
    val configTypeJavascript: String get() = ModuleStrings.configTypeJavascript
    val configTypeJavascriptDesc: String get() = ModuleStrings.configTypeJavascriptDesc
    val configTypeJson: String get() = ModuleStrings.configTypeJson
    val configTypeJsonDesc: String get() = ModuleStrings.configTypeJsonDesc
    val configTypeRange: String get() = ModuleStrings.configTypeRange
    val configTypeRangeDesc: String get() = ModuleStrings.configTypeRangeDesc
    val configTypeDate: String get() = ModuleStrings.configTypeDate
    val configTypeDateDesc: String get() = ModuleStrings.configTypeDateDesc
    val configTypeTime: String get() = ModuleStrings.configTypeTime
    val configTypeTimeDesc: String get() = ModuleStrings.configTypeTimeDesc
    val configTypeDatetime: String get() = ModuleStrings.configTypeDatetime
    val configTypeDatetimeDesc: String get() = ModuleStrings.configTypeDatetimeDesc
    val configTypeFile: String get() = ModuleStrings.configTypeFile
    val configTypeFileDesc: String get() = ModuleStrings.configTypeFileDesc
    val configTypeImage: String get() = ModuleStrings.configTypeImage
    val configTypeImageDesc: String get() = ModuleStrings.configTypeImageDesc
    
    // ==================== LRC ====================
    val lrcThemeDefault: String get() = when (lang) {
        AppLanguage.CHINESE -> "默认"
        AppLanguage.ENGLISH -> "Default"
        AppLanguage.ARABIC -> "افتراضي"
    }
    val lrcThemeKaraoke: String get() = when (lang) {
        AppLanguage.CHINESE -> "卡拉OK"
        AppLanguage.ENGLISH -> "Karaoke"
        AppLanguage.ARABIC -> "كاريوكي"
    }
    val lrcThemeNeon: String get() = when (lang) {
        AppLanguage.CHINESE -> "霓虹"
        AppLanguage.ENGLISH -> "Neon"
        AppLanguage.ARABIC -> "نيون"
    }
    val lrcThemeMinimal: String get() = when (lang) {
        AppLanguage.CHINESE -> "极简"
        AppLanguage.ENGLISH -> "Minimal"
        AppLanguage.ARABIC -> "بسيط"
    }
    val lrcThemeClassic: String get() = when (lang) {
        AppLanguage.CHINESE -> "经典"
        AppLanguage.ENGLISH -> "Classic"
        AppLanguage.ARABIC -> "كلاسيكي"
    }
    val lrcThemeDark: String get() = when (lang) {
        AppLanguage.CHINESE -> "暗夜"
        AppLanguage.ENGLISH -> "Dark"
        AppLanguage.ARABIC -> "داكن"
    }
    val lrcThemeRomantic: String get() = when (lang) {
        AppLanguage.CHINESE -> "浪漫"
        AppLanguage.ENGLISH -> "Romantic"
        AppLanguage.ARABIC -> "رومانسي"
    }
    val lrcThemeEnergetic: String get() = when (lang) {
        AppLanguage.CHINESE -> "活力"
        AppLanguage.ENGLISH -> "Energetic"
        AppLanguage.ARABIC -> "نشط"
    }
    
    // ==================== Test Page ====================
    val testPageBasicHtml: String get() = SampleStrings.testPageBasicHtml
    val testPageBasicHtmlDesc: String get() = SampleStrings.testPageBasicHtmlDesc
    val testPageForm: String get() = SampleStrings.testPageForm
    val testPageFormDesc: String get() = SampleStrings.testPageFormDesc
    val testPageMedia: String get() = SampleStrings.testPageMedia
    val testPageMediaDesc: String get() = SampleStrings.testPageMediaDesc
    val testPageAdSimulator: String get() = SampleStrings.testPageAdSimulator
    val testPageAdSimulatorDesc: String get() = SampleStrings.testPageAdSimulatorDesc
    val testPagePopup: String get() = SampleStrings.testPagePopup
    val testPagePopupDesc: String get() = SampleStrings.testPagePopupDesc
    val testPageScroll: String get() = SampleStrings.testPageScroll
    val testPageScrollDesc: String get() = SampleStrings.testPageScrollDesc
    val testPageStyle: String get() = SampleStrings.testPageStyle
    val testPageStyleDesc: String get() = SampleStrings.testPageStyleDesc
    val testPageApi: String get() = SampleStrings.testPageApi
    val testPageApiDesc: String get() = SampleStrings.testPageApiDesc
    
    // ==================== Module Scheme Presets ====================
    val presetReading: String get() = ExtensionStrings.presetReading
    val presetReadingDesc: String get() = ExtensionStrings.presetReadingDesc
    val presetAdblock: String get() = ExtensionStrings.presetAdblock
    val presetAdblockDesc: String get() = ExtensionStrings.presetAdblockDesc
    val presetMedia: String get() = ExtensionStrings.presetMedia
    val presetMediaDesc: String get() = ExtensionStrings.presetMediaDesc
    val presetUtility: String get() = ExtensionStrings.presetUtility
    val presetUtilityDesc: String get() = ExtensionStrings.presetUtilityDesc
    val presetNight: String get() = ExtensionStrings.presetNight
    val presetNightDesc: String get() = ExtensionStrings.presetNightDesc
    
    // ==================== Agent Tool Descriptions ====================
    val agentToolSyntaxCheck: String get() = AiStrings.agentToolSyntaxCheck
    val agentToolLintCode: String get() = AiStrings.agentToolLintCode
    val agentToolSecurityScan: String get() = AiStrings.agentToolSecurityScan
    val agentToolGenerateCode: String get() = AiStrings.agentToolGenerateCode
    val agentToolFixError: String get() = AiStrings.agentToolFixError
    val agentToolRefactorCode: String get() = AiStrings.agentToolRefactorCode
    val agentToolTestModule: String get() = AiStrings.agentToolTestModule
    val agentToolValidateConfig: String get() = AiStrings.agentToolValidateConfig
    val agentToolGetTemplates: String get() = AiStrings.agentToolGetTemplates
    val agentToolGetSnippets: String get() = AiStrings.agentToolGetSnippets
    val agentToolCreateModule: String get() = AiStrings.agentToolCreateModule
    val agentToolPreviewModule: String get() = AiStrings.agentToolPreviewModule
    
    // Agent.
    val toolTypeSyntaxCheck: String get() = AiStrings.toolTypeSyntaxCheck
    val toolTypeSyntaxCheckDesc: String get() = AiStrings.toolTypeSyntaxCheckDesc
    val toolTypeLintCode: String get() = AiStrings.toolTypeLintCode
    val toolTypeLintCodeDesc: String get() = AiStrings.toolTypeLintCodeDesc
    val toolTypeSecurityScan: String get() = AiStrings.toolTypeSecurityScan
    val toolTypeSecurityScanDesc: String get() = AiStrings.toolTypeSecurityScanDesc
    val toolTypeGenerateCode: String get() = AiStrings.toolTypeGenerateCode
    val toolTypeGenerateCodeDesc: String get() = AiStrings.toolTypeGenerateCodeDesc
    val toolTypeRefactorCode: String get() = AiStrings.toolTypeRefactorCode
    val toolTypeRefactorCodeDesc: String get() = AiStrings.toolTypeRefactorCodeDesc
    val toolTypeFixError: String get() = AiStrings.toolTypeFixError
    val toolTypeFixErrorDesc: String get() = AiStrings.toolTypeFixErrorDesc
    val toolTypeTestModule: String get() = AiStrings.toolTypeTestModule
    val toolTypeTestModuleDesc: String get() = AiStrings.toolTypeTestModuleDesc
    val toolTypeValidateConfig: String get() = AiStrings.toolTypeValidateConfig
    val toolTypeValidateConfigDesc: String get() = AiStrings.toolTypeValidateConfigDesc
    val toolTypeGetTemplates: String get() = AiStrings.toolTypeGetTemplates
    val toolTypeGetTemplatesDesc: String get() = AiStrings.toolTypeGetTemplatesDesc
    val toolTypeGetSnippets: String get() = AiStrings.toolTypeGetSnippets
    val toolTypeGetSnippetsDesc: String get() = AiStrings.toolTypeGetSnippetsDesc
    val toolTypeSearchDocs: String get() = AiStrings.toolTypeSearchDocs
    val toolTypeSearchDocsDesc: String get() = AiStrings.toolTypeSearchDocsDesc
    val toolTypeCreateModule: String get() = AiStrings.toolTypeCreateModule
    val toolTypeCreateModuleDesc: String get() = AiStrings.toolTypeCreateModuleDesc
    val toolTypeUpdateModule: String get() = AiStrings.toolTypeUpdateModule
    val toolTypeUpdateModuleDesc: String get() = AiStrings.toolTypeUpdateModuleDesc
    val toolTypePreviewModule: String get() = AiStrings.toolTypePreviewModule
    val toolTypePreviewModuleDesc: String get() = AiStrings.toolTypePreviewModuleDesc
    
    // ==================== ====================
    val categoryGroupContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "内容处理"
        AppLanguage.ENGLISH -> "Content"
        AppLanguage.ARABIC -> "المحتوى"
    }
    val categoryGroupAppearance: String get() = when (lang) {
        AppLanguage.CHINESE -> "外观样式"
        AppLanguage.ENGLISH -> "Appearance"
        AppLanguage.ARABIC -> "المظهر"
    }
    val categoryGroupFunction: String get() = when (lang) {
        AppLanguage.CHINESE -> "功能增强"
        AppLanguage.ENGLISH -> "Function"
        AppLanguage.ARABIC -> "الوظائف"
    }
    val categoryGroupData: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据工具"
        AppLanguage.ENGLISH -> "Data Tools"
        AppLanguage.ARABIC -> "أدوات البيانات"
    }
    val categoryGroupMedia: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体处理"
        AppLanguage.ENGLISH -> "Media"
        AppLanguage.ARABIC -> "الوسائط"
    }
    val categoryGroupSecurity: String get() = when (lang) {
        AppLanguage.CHINESE -> "安全隐私"
        AppLanguage.ENGLISH -> "Security"
        AppLanguage.ARABIC -> "الأمان"
    }
    val categoryGroupLife: String get() = when (lang) {
        AppLanguage.CHINESE -> "生活工具"
        AppLanguage.ENGLISH -> "Life Tools"
        AppLanguage.ARABIC -> "أدوات الحياة"
    }
    val categoryGroupDeveloper: String get() = when (lang) {
        AppLanguage.CHINESE -> "开发调试"
        AppLanguage.ENGLISH -> "Developer"
        AppLanguage.ARABIC -> "المطور"
    }
    val categoryGroupOther: String get() = when (lang) {
        AppLanguage.CHINESE -> "其他"
        AppLanguage.ENGLISH -> "Other"
        AppLanguage.ARABIC -> "أخرى"
    }
    
    // ==================== ====================
    val permGroupBasic: String get() = when (lang) {
        AppLanguage.CHINESE -> "基础权限"
        AppLanguage.ENGLISH -> "Basic Permissions"
        AppLanguage.ARABIC -> "الأذونات الأساسية"
    }
    val permGroupStorage: String get() = when (lang) {
        AppLanguage.CHINESE -> "存储权限"
        AppLanguage.ENGLISH -> "Storage Permissions"
        AppLanguage.ARABIC -> "أذونات التخزين"
    }
    val permGroupNetwork: String get() = when (lang) {
        AppLanguage.CHINESE -> "网络权限"
        AppLanguage.ENGLISH -> "Network Permissions"
        AppLanguage.ARABIC -> "أذونات الشبكة"
    }
    val permGroupInteraction: String get() = when (lang) {
        AppLanguage.CHINESE -> "用户交互"
        AppLanguage.ENGLISH -> "User Interaction"
        AppLanguage.ARABIC -> "تفاعل المستخدم"
    }
    val permGroupDevice: String get() = when (lang) {
        AppLanguage.CHINESE -> "设备权限"
        AppLanguage.ENGLISH -> "Device Permissions"
        AppLanguage.ARABIC -> "أذونات الجهاز"
    }
    val permGroupMediaPerm: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体权限"
        AppLanguage.ENGLISH -> "Media Permissions"
        AppLanguage.ARABIC -> "أذونات الوسائط"
    }
    val permGroupFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "文件权限"
        AppLanguage.ENGLISH -> "File Permissions"
        AppLanguage.ARABIC -> "أذونات الملفات"
    }
    val permGroupAdvanced: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级权限"
        AppLanguage.ENGLISH -> "Advanced Permissions"
        AppLanguage.ARABIC -> "الأذونات المتقدمة"
    }
    
    // ==================== AI Scenarios ====================
    val featureAiCoding: String get() = AiConfigStrings.featureAiCoding
    val featureAiCodingDesc: String get() = AiConfigStrings.featureAiCodingDesc
    val featureAiCodingImage: String get() = AiConfigStrings.featureAiCodingImage
    val featureAiCodingImageDesc: String get() = AiConfigStrings.featureAiCodingImageDesc
    val featureIconGen: String get() = AiConfigStrings.featureIconGen
    val featureIconGenDesc: String get() = AiConfigStrings.featureIconGenDesc
    val featureModuleDev: String get() = AiConfigStrings.featureModuleDev
    val featureModuleDevDesc: String get() = AiConfigStrings.featureModuleDevDesc
    val featureLrcGen: String get() = AiConfigStrings.featureLrcGen
    val featureLrcGenDesc: String get() = AiConfigStrings.featureLrcGenDesc
    val featureTranslate: String get() = AiConfigStrings.featureTranslate
    val featureTranslateDesc: String get() = AiConfigStrings.featureTranslateDesc
    val featureGeneral: String get() = AiConfigStrings.featureGeneral
    val featureGeneralDesc: String get() = AiConfigStrings.featureGeneralDesc
    
    // ==================== Model Capabilities ====================
    val capabilityText: String get() = AiConfigStrings.capabilityText
    val capabilityTextDesc: String get() = AiConfigStrings.capabilityTextDesc
    val capabilityAudio: String get() = AiConfigStrings.capabilityAudio
    val capabilityAudioDesc: String get() = AiConfigStrings.capabilityAudioDesc
    val capabilityImage: String get() = AiConfigStrings.capabilityImage
    val capabilityImageDesc: String get() = AiConfigStrings.capabilityImageDesc
    val capabilityImageGen: String get() = AiConfigStrings.capabilityImageGen
    val capabilityImageGenDesc: String get() = AiConfigStrings.capabilityImageGenDesc
    val capabilityVideo: String get() = AiConfigStrings.capabilityVideo
    val capabilityVideoDesc: String get() = AiConfigStrings.capabilityVideoDesc
    val capabilityCode: String get() = AiConfigStrings.capabilityCode
    val capabilityCodeDesc: String get() = AiConfigStrings.capabilityCodeDesc
    val capabilityFunctionCall: String get() = AiConfigStrings.capabilityFunctionCall
    val capabilityFunctionCallDesc: String get() = AiConfigStrings.capabilityFunctionCallDesc
    val capabilityLongContext: String get() = AiConfigStrings.capabilityLongContext
    val capabilityLongContextDesc: String get() = AiConfigStrings.capabilityLongContextDesc
    
    // ==================== Module Config Items ====================
    val configCssSelector: String get() = ExtensionStrings.configCssSelector
    val configCssSelectorDesc: String get() = ExtensionStrings.configCssSelectorDesc
    val configCssSelectorPlaceholder: String get() = ExtensionStrings.configCssSelectorPlaceholder
    val configHideMethod: String get() = ExtensionStrings.configHideMethod
    val configBlockPopups: String get() = ExtensionStrings.configBlockPopups
    val configBlockOverlays: String get() = ExtensionStrings.configBlockOverlays
    val configAutoCloseDelay: String get() = ExtensionStrings.configAutoCloseDelay
    val configCssCode: String get() = ExtensionStrings.configCssCode
    val configBrightness: String get() = ExtensionStrings.configBrightness
    val configContrast: String get() = ExtensionStrings.configContrast
    val configFont: String get() = ExtensionStrings.configFont
    val configFontSize: String get() = ExtensionStrings.configFontSize
    
    // ==================== ====================
    val styleMovie: String get() = when (lang) {
        AppLanguage.CHINESE -> "电影"
        AppLanguage.ENGLISH -> "Movie"
        AppLanguage.ARABIC -> "فيلم"
    }
    
    val styleBook: String get() = when (lang) {
        AppLanguage.CHINESE -> "书籍"
        AppLanguage.ENGLISH -> "Book"
        AppLanguage.ARABIC -> "كتاب"
    }
    
    val styleAnime: String get() = when (lang) {
        AppLanguage.CHINESE -> "动画"
        AppLanguage.ENGLISH -> "Anime"
        AppLanguage.ARABIC -> "أنمي"
    }
    
    val styleGame: String get() = when (lang) {
        AppLanguage.CHINESE -> "游戏"
        AppLanguage.ENGLISH -> "Game"
        AppLanguage.ARABIC -> "لعبة"
    }
    
    val styleBrand: String get() = when (lang) {
        AppLanguage.CHINESE -> "品牌"
        AppLanguage.ENGLISH -> "Brand"
        AppLanguage.ARABIC -> "علامة تجارية"
    }
    
    val styleArt: String get() = when (lang) {
        AppLanguage.CHINESE -> "艺术流派"
        AppLanguage.ENGLISH -> "Art Style"
        AppLanguage.ARABIC -> "أسلوب فني"
    }
    
    val styleEra: String get() = when (lang) {
        AppLanguage.CHINESE -> "时代风格"
        AppLanguage.ENGLISH -> "Era Style"
        AppLanguage.ARABIC -> "أسلوب العصر"
    }
    
    val styleCulture: String get() = when (lang) {
        AppLanguage.CHINESE -> "文化风格"
        AppLanguage.ENGLISH -> "Cultural Style"
        AppLanguage.ARABIC -> "أسلوب ثقافي"
    }
    
    // ==================== Theme Settings ====================
    val colorScheme: String get() = UiStrings.colorScheme
    
    val themeFeatures: String get() = UiStrings.themeFeatures
    
    val applyTheme: String get() = CommonStrings.applyTheme
    
    // ==================== ====================
    val allowSkip: String get() = when (lang) {
        AppLanguage.CHINESE -> "允许点击跳过"
        AppLanguage.ENGLISH -> "Allow Skip"
        AppLanguage.ARABIC -> "السماح بالتخطي"
    }
    
    val allowSkipHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "用户可点击屏幕跳过启动画面"
        AppLanguage.ENGLISH -> "User can tap screen to skip splash"
        AppLanguage.ARABIC -> "يمكن للمستخدم النقر على الشاشة لتخطي شاشة البداية"
    }
    
    val showTranslateButton: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示翻译按钮"
        AppLanguage.ENGLISH -> "Show Translate Button"
        AppLanguage.ARABIC -> "إظهار زر الترجمة"
    }
    
    val showTranslateButtonHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "在页面右下角显示可拖拽的翻译悬浮按钮"
        AppLanguage.ENGLISH -> "Show a draggable translate FAB at bottom right"
        AppLanguage.ARABIC -> "إظهار زر ترجمة عائم قابل للسحب في أسفل اليمين"
    }
    
    val translateEngine: String get() = when (lang) {
        AppLanguage.CHINESE -> "翻译引擎"
        AppLanguage.ENGLISH -> "Translation Engine"
        AppLanguage.ARABIC -> "محرك الترجمة"
    }
    
    val autoTranslateOnLoad: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面加载后自动翻译"
        AppLanguage.ENGLISH -> "Auto Translate on Load"
        AppLanguage.ARABIC -> "ترجمة تلقائية عند التحميل"
    }
    
    val autoTranslateOnLoadHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面加载完成后自动执行翻译，无需手动点击按钮"
        AppLanguage.ENGLISH -> "Automatically translate after page loads without manual click"
        AppLanguage.ARABIC -> "ترجمة تلقائية بعد تحميل الصفحة بدون نقر يدوي"
    }
    
    val previewAnnouncement: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览公告效果"
        AppLanguage.ENGLISH -> "Preview Announcement"
        AppLanguage.ARABIC -> "معاينة الإعلان"
    }
    
    // ==================== CreateAppScreen Translations ====================
    val showStatusBar: String get() = WebViewStrings.showStatusBar
    
    val showStatusBarHint: String get() = WebViewStrings.showStatusBarHint
    
    val showNavigationBar: String get() = WebViewStrings.showNavigationBar
    
    val showNavigationBarHint: String get() = WebViewStrings.showNavigationBarHint
    
    val showToolbar: String get() = WebViewStrings.showToolbar
    
    val showToolbarHint: String get() = WebViewStrings.showToolbarHint
    
    val statusBarStyleConfigLabel: String get() = WebViewStrings.statusBarStyleConfigLabel

    val statusBarLightModeLabel: String get() = WebViewStrings.statusBarLightModeLabel

    val statusBarDarkModeLabel: String get() = WebViewStrings.statusBarDarkModeLabel

    val splashHint: String get() = WebViewStrings.splashHint
    
    val clickToSelectImageOrVideo: String get() = WebViewStrings.clickToSelectImageOrVideo
    
    val displayDuration: String get() = WebViewStrings.displayDuration
    
    val displayDurationSeconds: String get() = WebViewStrings.displayDurationSeconds
    
    val exportAppTheme: String get() = WebViewStrings.exportAppTheme
    
    val exportAppThemeHint: String get() = WebViewStrings.exportAppThemeHint
    
    val autoTranslateHint: String get() = WebViewStrings.autoTranslateHint
    
    val videoCrop: String get() = WebViewStrings.videoCrop
    
    val splashPreview: String get() = WebViewStrings.splashPreview
    
    val landscapeDisplay: String get() = WebViewStrings.landscapeDisplay
    
    val landscapeDisplayHint: String get() = WebViewStrings.landscapeDisplayHint
    
    // ==================== Auto-start Settings ====================
    val autoStartSettings: String get() = ShellStrings.autoStartSettings

    val autoStartDescription: String get() = ShellStrings.autoStartDescription
    
    val configured: String get() = ShellStrings.configured
    
    val bootAutoStart: String get() = ShellStrings.bootAutoStart
    
    val bootAutoStartHint: String get() = ShellStrings.bootAutoStartHint
    
    val scheduledAutoStart: String get() = ShellStrings.scheduledAutoStart
    
    val scheduledAutoStartHint: String get() = ShellStrings.scheduledAutoStartHint
    
    val launchDate: String get() = ShellStrings.launchDate
    
    val autoStartNote: String get() = ShellStrings.autoStartNote
    
    val today: String get() = ShellStrings.today
    
    val tomorrow: String get() = ShellStrings.tomorrow
    
    val nextLaunchTime: String get() = ShellStrings.nextLaunchTime
    
    val exactAlarmPermissionHint: String get() = ShellStrings.exactAlarmPermissionHint
    
    val batteryOptimizationHint: String get() = ShellStrings.batteryOptimizationHint
    
    val bootDelay: String get() = ShellStrings.bootDelay
    
    val oemAutoStartHint: String get() = ShellStrings.oemAutoStartHint
    
    val autoStartPermissionReady: String get() = ShellStrings.autoStartPermissionReady
    
    // ==================== ====================
    val selectAnnouncementStyle: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择公告样式"
        AppLanguage.ENGLISH -> "Select Announcement Style"
        AppLanguage.ARABIC -> "اختيار نمط الإعلان"
    }
    
    val okGood: String get() = when (lang) {
        AppLanguage.CHINESE -> "好的 👍"
        AppLanguage.ENGLISH -> "OK 👍"
        AppLanguage.ARABIC -> "حسنًا 👍"
    }
    
    val understood: String get() = when (lang) {
        AppLanguage.CHINESE -> "了解了"
        AppLanguage.ENGLISH -> "Understood"
        AppLanguage.ARABIC -> "مفهوم"
    }
    
    val newMessage: String get() = when (lang) {
        AppLanguage.CHINESE -> "新消息"
        AppLanguage.ENGLISH -> "New Message"
        AppLanguage.ARABIC -> "رسالة جديدة"
    }
    
    val learnMore: String get() = when (lang) {
        AppLanguage.CHINESE -> "了解更多"
        AppLanguage.ENGLISH -> "Learn More"
        AppLanguage.ARABIC -> "اعرف المزيد"
    }
    
    // ==================== Announcement Template Names ====================
    
    val templateMinimalDesc: String get() = CreateStrings.templateMinimalDesc
    
    val templateXiaohongshu: String get() = CreateStrings.templateXiaohongshu
    
    val templateXiaohongshuDesc: String get() = CreateStrings.templateXiaohongshuDesc
    
    val templateGradientDesc: String get() = CreateStrings.templateGradientDesc
    
    val templateGlassmorphismDesc: String get() = CreateStrings.templateGlassmorphismDesc
    
    val templateNeon: String get() = CreateStrings.templateNeon
    
    val templateNeonDesc: String get() = CreateStrings.templateNeonDesc
    
    val templateCute: String get() = CreateStrings.templateCute
    
    val templateCuteDesc: String get() = CreateStrings.templateCuteDesc
    
    val templateElegant: String get() = CreateStrings.templateElegant
    
    val templateElegantDesc: String get() = CreateStrings.templateElegantDesc
    
    val templateFestive: String get() = CreateStrings.templateFestive
    
    val templateFestiveDesc: String get() = CreateStrings.templateFestiveDesc
    
    val templateDarkDesc: String get() = CreateStrings.templateDarkDesc
    
    val templateNatureDesc: String get() = CreateStrings.templateNatureDesc
    
    // ==================== ====================
    val langChinese: String get() = when (lang) {
        AppLanguage.CHINESE -> "中文"
        AppLanguage.ENGLISH -> "Chinese"
        AppLanguage.ARABIC -> "الصينية"
    }
    
    val langEnglish: String get() = when (lang) {
        AppLanguage.CHINESE -> "英文"
        AppLanguage.ENGLISH -> "English"
        AppLanguage.ARABIC -> "الإنجليزية"
    }
    
    val langJapanese: String get() = when (lang) {
        AppLanguage.CHINESE -> "日文"
        AppLanguage.ENGLISH -> "Japanese"
        AppLanguage.ARABIC -> "اليابانية"
    }
    
    val langArabic: String get() = when (lang) {
        AppLanguage.CHINESE -> "阿拉伯语"
        AppLanguage.ENGLISH -> "Arabic"
        AppLanguage.ARABIC -> "العربية"
    }
    
    // ==================== Translations ====================
    val systemNotification: String get() = when (lang) {
        AppLanguage.CHINESE -> "系统通知"
        AppLanguage.ENGLISH -> "System Notification"
        AppLanguage.ARABIC -> "إشعار النظام"
    }
    
    val justNow: String get() = when (lang) {
        AppLanguage.CHINESE -> "刚刚"
        AppLanguage.ENGLISH -> "Just now"
        AppLanguage.ARABIC -> "الآن"
    }
    
    val details: String get() = when (lang) {
        AppLanguage.CHINESE -> "详情"
        AppLanguage.ENGLISH -> "Details"
        AppLanguage.ARABIC -> "التفاصيل"
    }
    
    val clickToSelectOrUseButton: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击选择或使用下方按钮"
        AppLanguage.ENGLISH -> "Click to select or use button below"
        AppLanguage.ARABIC -> "انقر للاختيار أو استخدم الزر أدناه"
    }
    
    // ==================== AI Settings ====================
    val aiSettings: String get() = AiStrings.aiSettings
    
    val apiKeys: String get() = AiStrings.apiKeys
    
    val noApiKeysHint: String get() = AiStrings.noApiKeysHint
    
    val testing: String get() = AiStrings.testing
    
    val connectionSuccess: String get() = AiStrings.connectionSuccess
    
    val test: String get() = AiStrings.test
    
    val savedModels: String get() = AiStrings.savedModels
    
    val configModelCapabilities: String get() = AiStrings.configModelCapabilities
    
    val pleaseAddApiKeyFirst: String get() = AiStrings.pleaseAddApiKeyFirst
    
    val noSavedModelsHint: String get() = AiStrings.noSavedModelsHint
    
    val defaultLabel: String get() = AiStrings.defaultLabel
    
    val setAsDefault: String get() = AiStrings.setAsDefault
    
    val editApiKey: String get() = AiStrings.editApiKey
    
    val addApiKey: String get() = AiStrings.addApiKey
    
    val getApiKey: String get() = AiStrings.getApiKey
    
    val openAiCompatibleHint: String get() = AiStrings.openAiCompatibleHint
    
    val apiFormat: String get() = AiStrings.apiFormat
    
    val apiKeyAliasPlaceholder: String get() = AiStrings.apiKeyAliasPlaceholder
    
    val modelsEndpoint: String get() = AiStrings.modelsEndpoint
    
    val modelsEndpointHint: String get() = AiStrings.modelsEndpointHint
    
    val chatEndpoint: String get() = AiStrings.chatEndpoint
    
    val chatEndpointHint: String get() = AiStrings.chatEndpointHint
    
    val selectApiKey: String get() = AiStrings.selectApiKey
    
    val batchSelectModels: String get() = AiStrings.batchSelectModels
    
    val selectedModelsCount: String get() = AiStrings.selectedModelsCount
    
    val addSelectedModels: String get() = AiStrings.addSelectedModels
    
    val searchModels: String get() = AiStrings.searchModels
    
    val noSearchResults: String get() = AiStrings.noSearchResults
    
    val sortByName: String get() = AiStrings.sortByName
    
    val sortByContext: String get() = AiStrings.sortByContext
    
    val sortByPriceLow: String get() = AiStrings.sortByPriceLow
    
    val sortByPriceHigh: String get() = AiStrings.sortByPriceHigh
    
    val addModel: String get() = AiStrings.addModel
    
    val addModelFrom: String get() = AiStrings.addModelFrom
    
    val orManualInputModelId: String get() = AiStrings.orManualInputModelId
    
    val modelIdPlaceholder: String get() = AiStrings.modelIdPlaceholder
    
    val capabilityTags: String get() = AiStrings.capabilityTags
    
    val selectCapabilitiesHint: String get() = AiStrings.selectCapabilitiesHint
    
    val editModel: String get() = AiStrings.editModel
    
    val featureSceneConfig: String get() = AiStrings.featureSceneConfig
    
    val selectFeaturesForCapability: String get() = AiStrings.selectFeaturesForCapability

    // ==================== Theme Settings Strings ====================
    
    val animationDisabled: String get() = UiStrings.animationDisabled
    
    val holdToExperience: String get() = UiStrings.holdToExperience
    
    val primaryColor: String get() = UiStrings.primaryColor
    
    val secondaryColor: String get() = UiStrings.secondaryColor
    
    val accentColor: String get() = UiStrings.accentColor
    
    val animationStyle: String get() = UiStrings.animationStyle
    
    val interactionMethod: String get() = UiStrings.interactionMethod
    
    val cornerRadius: String get() = UiStrings.cornerRadius
    
    val glowEffect: String get() = UiStrings.glowEffect
    
    val particleEffect: String get() = UiStrings.particleEffect
    
    val glassmorphism: String get() = UiStrings.glassmorphism

    // ==================== BGM ====================
    
    val bgmTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "背景音乐"
        AppLanguage.ENGLISH -> "Background Music"
        AppLanguage.ARABIC -> "موسيقى الخلفية"
    }
    
    val bgmDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "为应用添加背景音乐，支持循环或顺序播放"
        AppLanguage.ENGLISH -> "Add background music to app, supports loop or sequential playback"
        AppLanguage.ARABIC -> "إضافة موسيقى خلفية للتطبيق، يدعم التشغيل المتكرر أو المتسلسل"
    }
    
    val selectedMusicCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "已选 %d 首音乐"
        AppLanguage.ENGLISH -> "%d music selected"
        AppLanguage.ARABIC -> "تم اختيار %d موسيقى"
    }
    
    val andMoreTracks: String get() = when (lang) {
        AppLanguage.CHINESE -> "还有 %d 首..."
        AppLanguage.ENGLISH -> "and %d more..."
        AppLanguage.ARABIC -> "و %d أخرى..."
    }
    
    val loopPlayback: String get() = when (lang) {
        AppLanguage.CHINESE -> "循环播放"
        AppLanguage.ENGLISH -> "Loop Playback"
        AppLanguage.ARABIC -> "تشغيل متكرر"
    }
    
    val sequentialPlayback: String get() = when (lang) {
        AppLanguage.CHINESE -> "Sequential play"
        AppLanguage.ENGLISH -> "Sequential Playback"
        AppLanguage.ARABIC -> "تشغيل متسلسل"
    }
    
    val shufflePlayback: String get() = when (lang) {
        AppLanguage.CHINESE -> "Shuffle play"
        AppLanguage.ENGLISH -> "Shuffle Playback"
        AppLanguage.ARABIC -> "تشغيل عشوائي"
    }
    
    val volumePercent: String get() = when (lang) {
        AppLanguage.CHINESE -> "音量: %d%%"
        AppLanguage.ENGLISH -> "Volume: %d%%"
        AppLanguage.ARABIC -> "مستوى الصوت: %d%%"
    }
    
    val modifyConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "修改配置"
        AppLanguage.ENGLISH -> "Modify Config"
        AppLanguage.ARABIC -> "تعديل الإعدادات"
    }

    // ==================== Extension Module Strings ====================
    
    val extensionModuleTitle: String get() = ModuleStrings.extensionModuleTitle
    
    val noModuleSelected: String get() = ExtensionStrings.noModuleSelected
    
    val modulesSelected: String get() = ModuleStrings.modulesSelected
    
    val addModule: String get() = ExtensionStrings.addModule
    
    val extensionModuleHint: String get() = ModuleStrings.extensionModuleHint
    
    val searchModulesPlaceholder: String get() = ExtensionStrings.searchModulesPlaceholder
    
    val filterAll: String get() = ExtensionStrings.filterAll
    
    val filterContent: String get() = ExtensionStrings.filterContent
    
    val filterStyle: String get() = ExtensionStrings.filterStyle
    
    val filterFunction: String get() = ExtensionStrings.filterFunction
    
    val clearSelection: String get() = ExtensionStrings.clearSelection
    
    val quickEnable: String get() = ExtensionStrings.quickEnable
    
    val shareModule: String get() = ExtensionStrings.shareModule
    
    val sharePoster: String get() = ExtensionStrings.sharePoster
    
    val savePoster: String get() = ExtensionStrings.savePoster
    
    val sharePosterBtn: String get() = ExtensionStrings.sharePosterBtn
    
    val scanQrToImport: String get() = ExtensionStrings.scanQrToImport
    
    val scanToImportModule: String get() = ExtensionStrings.scanToImportModule
    
    val moduleTooLargeTitle: String get() = ModuleStrings.moduleTooLargeTitle
    
    val moduleTooLargeDesc: String get() = ModuleStrings.moduleTooLargeDesc
    
    val shareModuleFile: String get() = ExtensionStrings.shareModuleFile
    
    val shareFileHint: String get() = ExtensionStrings.shareFileHint
    
    val posterSavedToGallery: String get() = ExtensionStrings.posterSavedToGallery
    
    val shareModuleText: String get() = ExtensionStrings.shareModuleText
    
    val shareModuleFileSubject: String get() = ExtensionStrings.shareModuleFileSubject
    
    val shareModuleFileText: String get() = ExtensionStrings.shareModuleFileText
    
    val extensionModuleSubtitle: String get() = ModuleStrings.extensionModuleSubtitle
    
    val onlyEffectiveOnMatchingSites: String get() = ExtensionStrings.onlyEffectiveOnMatchingSites
    
    // ==================== AI ====================
    val noModelConfigured: String get() = when (lang) {
        AppLanguage.CHINESE -> "未配置模型"
        AppLanguage.ENGLISH -> "No model configured"
        AppLanguage.ARABIC -> "لم يتم تكوين النموذج"
    }
    val clickToConfigureAiModel: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击配置 AI 模型"
        AppLanguage.ENGLISH -> "Click to configure AI model"
        AppLanguage.ARABIC -> "انقر لتكوين نموذج AI"
    }
    val selectedLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "已选中"
        AppLanguage.ENGLISH -> "Selected"
        AppLanguage.ARABIC -> "محدد"
    }
    val noAvailableModels: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无可用模型"
        AppLanguage.ENGLISH -> "No available models"
        AppLanguage.ARABIC -> "لا توجد نماذج متاحة"
    }
    val configureModelHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "请先在 AI 设置中配置支持模块开发功能的模型"
        AppLanguage.ENGLISH -> "Please configure a model that supports module development in AI settings"
        AppLanguage.ARABIC -> "يرجى تكوين نموذج يدعم تطوير الوحدات في إعدادات AI"
    }
    
    // ==================== AI ====================
    val ruleUseChinese: String get() = AiCodingStrings.ruleUseChinese
    val ruleChineseComments: String get() = AiCodingStrings.ruleChineseComments
    val ruleGameFlow: String get() = AiCodingStrings.ruleGameFlow
    val ruleScoreAndInstructions: String get() = AiCodingStrings.ruleScoreAndInstructions
    val ruleTouchControl: String get() = AiCodingStrings.ruleTouchControl
    val ruleCssAnimation: String get() = AiCodingStrings.ruleCssAnimation
    val ruleTransition: String get() = AiCodingStrings.ruleTransition
    val rulePerformance: String get() = AiCodingStrings.rulePerformance
    val ruleFormValidation: String get() = AiCodingStrings.ruleFormValidation
    val ruleInputLabels: String get() = AiCodingStrings.ruleInputLabels
    val ruleSubmitLoading: String get() = AiCodingStrings.ruleSubmitLoading

    // ==================== ====================
    
    val sampleProjects: String get() = when (lang) {
        AppLanguage.CHINESE -> "示例项目"
        AppLanguage.ENGLISH -> "Sample Projects"
        AppLanguage.ARABIC -> "مشاريع نموذجية"
    }
    
    val quickExperienceFrontend: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速体验前端项目导入"
        AppLanguage.ENGLISH -> "Quick experience frontend project import"
        AppLanguage.ARABIC -> "تجربة سريعة لاستيراد مشروع الواجهة الأمامية"
    }
    
    val quickExperience: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速体验"
        AppLanguage.ENGLISH -> "Quick Experience"
        AppLanguage.ARABIC -> "تجربة سريعة"
    }
    
    val run: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行"
        AppLanguage.ENGLISH -> "Run"
        AppLanguage.ARABIC -> "تشغيل"
    }

    // ==================== ====================
    
    val cannotParseImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "无法解析图片"
        AppLanguage.ENGLISH -> "Cannot parse image"
        AppLanguage.ARABIC -> "لا يمكن تحليل الصورة"
    }
    
    val cannotOpenImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "无法打开图片"
        AppLanguage.ENGLISH -> "Cannot open image"
        AppLanguage.ARABIC -> "لا يمكن فتح الصورة"
    }
    
    val loadImageFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "加载图片失败: %s"
        AppLanguage.ENGLISH -> "Failed to load image: %s"
        AppLanguage.ARABIC -> "فشل تحميل الصورة: %s"
    }
    
    val originalImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "原始图片"
        AppLanguage.ENGLISH -> "Original Image"
        AppLanguage.ARABIC -> "الصورة الأصلية"
    }

    // ==================== ====================
    
    val videoFileNotExist: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频文件不存在"
        AppLanguage.ENGLISH -> "Video file does not exist"
        AppLanguage.ARABIC -> "ملف الفيديو غير موجود"
    }
    
    val videoPreview: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频预览"
        AppLanguage.ENGLISH -> "Video Preview"
        AppLanguage.ARABIC -> "معاينة الفيديو"
    }
    
    val selectedDuration: String get() = when (lang) {
        AppLanguage.CHINESE -> "已选择: %.1f 秒"
        AppLanguage.ENGLISH -> "Selected: %.1f seconds"
        AppLanguage.ARABIC -> "المحدد: %.1f ثانية"
    }
    
    val totalDuration: String get() = when (lang) {
        AppLanguage.CHINESE -> "总时长: %.1f 秒"
        AppLanguage.ENGLISH -> "Total duration: %.1f seconds"
        AppLanguage.ARABIC -> "المدة الإجمالية: %.1f ثانية"
    }
    
    val trimRangeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "裁剪范围（拖动选择播放片段）"
        AppLanguage.ENGLISH -> "Trim range (drag to select playback segment)"
        AppLanguage.ARABIC -> "نطاق القص (اسحب لتحديد مقطع التشغيل)"
    }

    // ==================== APK ====================
    
    val apkExportConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "APK 导出配置"
        AppLanguage.ENGLISH -> "APK Export Config"
        AppLanguage.ARABIC -> "إعدادات تصدير APK"
    }
    
    val apkArchitecture: String get() = when (lang) {
        AppLanguage.CHINESE -> "APK 架构"
        AppLanguage.ENGLISH -> "APK Architecture"
        AppLanguage.ARABIC -> "بنية APK"
    }

    // ==================== Custom Signature Strings ====================
    
    val customSigning: String get() = BuildStrings.customSigning
    
    val customSigningDesc: String get() = BuildStrings.customSigningDesc
    
    val currentSigningStatus: String get() = BuildStrings.currentSigningStatus
    
    val signingTypeAutoGenerated: String get() = BuildStrings.signingTypeAutoGenerated
    
    val signingTypeCustom: String get() = BuildStrings.signingTypeCustom
    
    val signingTypeAndroidKeyStore: String get() = BuildStrings.signingTypeAndroidKeyStore
    
    val importKeystore: String get() = BuildStrings.importKeystore
    
    val exportKeystore: String get() = BuildStrings.exportKeystore
    
    val removeCustomKeystore: String get() = BuildStrings.removeCustomKeystore
    
    val keystorePassword: String get() = BuildStrings.keystorePassword
    
    val keystorePasswordHint: String get() = BuildStrings.keystorePasswordHint
    
    val exportPassword: String get() = BuildStrings.exportPassword
    
    val exportPasswordHint: String get() = BuildStrings.exportPasswordHint
    
    val keystoreImportSuccess: String get() = BuildStrings.keystoreImportSuccess
    
    val keystoreImportFailed: String get() = BuildStrings.keystoreImportFailed
    
    val keystoreExportSuccess: String get() = BuildStrings.keystoreExportSuccess
    
    val keystoreExportFailed: String get() = BuildStrings.keystoreExportFailed
    
    val keystoreRemoveSuccess: String get() = BuildStrings.keystoreRemoveSuccess
    
    val keystoreRemoveConfirm: String get() = BuildStrings.keystoreRemoveConfirm
    
    val supportedKeystoreFormats: String get() = BuildStrings.supportedKeystoreFormats
    
    val customSigningNote: String get() = BuildStrings.customSigningNote

    // ==================== User-Agent Spoofing Strings ====================
    
    val userAgentMode: String get() = WebViewStrings.userAgentMode
    
    val userAgentModeHint: String get() = WebViewStrings.userAgentModeHint
    
    val userAgentDefault: String get() = WebViewStrings.userAgentDefault
    
    val userAgentDefaultHint: String get() = WebViewStrings.userAgentDefaultHint
    
    val userAgentChromeMobile: String get() = WebViewStrings.userAgentChromeMobile
    
    val userAgentChromeDesktop: String get() = WebViewStrings.userAgentChromeDesktop
    
    val userAgentSafariMobile: String get() = WebViewStrings.userAgentSafariMobile
    
    val userAgentSafariDesktop: String get() = WebViewStrings.userAgentSafariDesktop
    
    val userAgentFirefoxMobile: String get() = WebViewStrings.userAgentFirefoxMobile
    
    val userAgentFirefoxDesktop: String get() = WebViewStrings.userAgentFirefoxDesktop
    
    val userAgentEdgeMobile: String get() = WebViewStrings.userAgentEdgeMobile
    
    val userAgentEdgeDesktop: String get() = WebViewStrings.userAgentEdgeDesktop
    
    val userAgentCustom: String get() = WebViewStrings.userAgentCustom
    
    val userAgentCustomHint: String get() = WebViewStrings.userAgentCustomHint
    
    val mobileVersion: String get() = WebViewStrings.mobileVersion
    
    val desktopVersion: String get() = WebViewStrings.desktopVersion
    
    val currentUserAgent: String get() = WebViewStrings.currentUserAgent
    
    val bypassWebViewDetection: String get() = WebViewStrings.bypassWebViewDetection

    // ==================== HTML ====================
    
    val encodingAndSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "编码: %s | 大小: %s"
        AppLanguage.ENGLISH -> "Encoding: %s | Size: %s"
        AppLanguage.ARABIC -> "الترميز: %s | الحجم: %s"
    }
    
    val fileLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "文件: %s"
        AppLanguage.ENGLISH -> "File: %s"
        AppLanguage.ARABIC -> "الملف: %s"
    }
    
    val clickToSelectFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击选择文件"
        AppLanguage.ENGLISH -> "Click to select file"
        AppLanguage.ARABIC -> "انقر لاختيار الملف"
    }
    
    val clearFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "清除"
        AppLanguage.ENGLISH -> "Clear"
        AppLanguage.ARABIC -> "مسح"
    }

    // ==================== Theme Settings Strings（ ） ====================
    
    val particle: String get() = when (lang) {
        AppLanguage.CHINESE -> "粒子"
        AppLanguage.ENGLISH -> "Particle"
        AppLanguage.ARABIC -> "جسيمات"
    }
    
    val autoSwitchBySystem: String get() = when (lang) {
        AppLanguage.CHINESE -> "根据系统设置自动切换"
        AppLanguage.ENGLISH -> "Auto switch based on system settings"
        AppLanguage.ARABIC -> "التبديل التلقائي بناءً على إعدادات النظام"
    }
    
    val alwaysUseLightTheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "始终使用浅色主题"
        AppLanguage.ENGLISH -> "Always use light theme"
        AppLanguage.ARABIC -> "استخدام السمة الفاتحة دائمًا"
    }
    
    val alwaysUseDarkTheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "始终使用深色主题"
        AppLanguage.ENGLISH -> "Always use dark theme"
        AppLanguage.ARABIC -> "استخدام السمة الداكنة دائمًا"
    }
    
    val interactionStyleLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "交互风格: %s"
        AppLanguage.ENGLISH -> "Interaction Style: %s"
        AppLanguage.ARABIC -> "نمط التفاعل: %s"
    }
    
    val clickButtonToExperience: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击下方按钮体验效果"
        AppLanguage.ENGLISH -> "Click button below to experience"
        AppLanguage.ARABIC -> "انقر على الزر أدناه للتجربة"
    }

    // ==================== ====================
    
    val dayMon: String get() = when (lang) {
        AppLanguage.CHINESE -> "一"
        AppLanguage.ENGLISH -> "Mon"
        AppLanguage.ARABIC -> "الإثنين"
    }
    
    val dayTue: String get() = when (lang) {
        AppLanguage.CHINESE -> "二"
        AppLanguage.ENGLISH -> "Tue"
        AppLanguage.ARABIC -> "الثلاثاء"
    }
    
    val dayWed: String get() = when (lang) {
        AppLanguage.CHINESE -> "三"
        AppLanguage.ENGLISH -> "Wed"
        AppLanguage.ARABIC -> "الأربعاء"
    }
    
    val dayThu: String get() = when (lang) {
        AppLanguage.CHINESE -> "四"
        AppLanguage.ENGLISH -> "Thu"
        AppLanguage.ARABIC -> "الخميس"
    }
    
    val dayFri: String get() = when (lang) {
        AppLanguage.CHINESE -> "五"
        AppLanguage.ENGLISH -> "Fri"
        AppLanguage.ARABIC -> "الجمعة"
    }
    
    val daySat: String get() = when (lang) {
        AppLanguage.CHINESE -> "六"
        AppLanguage.ENGLISH -> "Sat"
        AppLanguage.ARABIC -> "السبت"
    }
    
    val daySun: String get() = when (lang) {
        AppLanguage.CHINESE -> "日"
        AppLanguage.ENGLISH -> "Sun"
        AppLanguage.ARABIC -> "الأحد"
    }

    // ==================== AI ====================
    
    val aiGenerationServiceRunning: String get() = AiStrings.aiGenerationServiceRunning
    
    val generatingHtmlCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在生成 HTML 代码..."
        AppLanguage.ENGLISH -> "Generating HTML code..."
        AppLanguage.ARABIC -> "جاري إنشاء كود HTML..."
    }
    
    val generatingCodeChars: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在生成代码 (%d 字符)"
        AppLanguage.ENGLISH -> "Generating code (%d chars)"
        AppLanguage.ARABIC -> "جاري إنشاء الكود (%d حرف)"
    }
    
    val newFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "新文件"
        AppLanguage.ENGLISH -> "New file"
        AppLanguage.ARABIC -> "ملف جديد"
    }
    
    val fileCreatedVersion: String get() = when (lang) {
        AppLanguage.CHINESE -> "已创建文件: %s (%s)"
        AppLanguage.ENGLISH -> "File created: %s (%s)"
        AppLanguage.ARABIC -> "تم إنشاء الملف: %s (%s)"
    }
    
    val codeGenerationComplete: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码生成完成"
        AppLanguage.ENGLISH -> "Code generation complete"
        AppLanguage.ARABIC -> "اكتمل إنشاء الكود"
    }
    
    val generationFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成失败: %s"
        AppLanguage.ENGLISH -> "Generation failed: %s"
        AppLanguage.ARABIC -> "فشل الإنشاء: %s"
    }
    
    val generationComplete: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成完成"
        AppLanguage.ENGLISH -> "Generation complete"
        AppLanguage.ARABIC -> "اكتمل الإنشاء"
    }
    
    val generationCancelled: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成已取消"
        AppLanguage.ENGLISH -> "Generation cancelled"
        AppLanguage.ARABIC -> "تم إلغاء الإنشاء"
    }
    
    val aiGenerationService: String get() = AiStrings.aiGenerationService
    
    val aiCodeGenerationNotification: String get() = AiStrings.aiCodeGenerationNotification

    // ==================== APK ====================
    
    val shareApk: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享 APK"
        AppLanguage.ENGLISH -> "Share APK"
        AppLanguage.ARABIC -> "مشاركة APK"
    }
    
    val shareApkBuilding: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在构建 APK..."
        AppLanguage.ENGLISH -> "Building APK..."
        AppLanguage.ARABIC -> "جاري بناء APK..."
    }
    
    val shareApkSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "APK 已准备好分享"
        AppLanguage.ENGLISH -> "APK is ready to share"
        AppLanguage.ARABIC -> "APK جاهز للمشاركة"
    }
    
    val shareApkFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "构建 APK 失败: %s"
        AppLanguage.ENGLISH -> "Failed to build APK: %s"
        AppLanguage.ARABIC -> "فشل بناء APK: %s"
    }
    
    val shareApkTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享 %s 应用"
        AppLanguage.ENGLISH -> "Share %s app"
        AppLanguage.ARABIC -> "مشاركة تطبيق %s"
    }

    // ==================== Force-run Strings ====================
    
    val forcedRunSettings: String get() = ShellStrings.forcedRunSettings
    
    val enableForcedRun: String get() = ShellStrings.enableForcedRun
    
    val forcedRunHint: String get() = ShellStrings.forcedRunHint
    
    val forcedRunMode: String get() = ShellStrings.forcedRunMode
    
    val fixedTimeMode: String get() = ShellStrings.fixedTimeMode
    
    val countdownMode: String get() = ShellStrings.countdownMode
    
    val durationMode: String get() = CommonStrings.durationMode
    
    val fixedTimeModeHint: String get() = ShellStrings.fixedTimeModeHint
    
    val countdownModeHint: String get() = ShellStrings.countdownModeHint
    
    val durationModeHint: String get() = CommonStrings.durationModeHint
    
    val forcedRunStartTime: String get() = ShellStrings.forcedRunStartTime
    
    val forcedRunEndTime: String get() = ShellStrings.forcedRunEndTime
    
    val activeDays: String get() = ShellStrings.activeDays
    
    val countdownDuration: String get() = ShellStrings.countdownDuration
    
    val minutes: String get() = ShellStrings.minutes
    
    val minutesShort: String get() = ShellStrings.minutesShort
    
    val accessStartTime: String get() = ShellStrings.accessStartTime
    
    val accessEndTime: String get() = ShellStrings.accessEndTime
    
    val accessDays: String get() = ShellStrings.accessDays
    
    val blockSystemUI: String get() = ShellStrings.blockSystemUI
    
    val blockBackButton: String get() = ShellStrings.blockBackButton
    
    val blockHomeButton: String get() = ShellStrings.blockHomeButton
    
    val showCountdownTimer: String get() = ShellStrings.showCountdownTimer
    
    val allowEmergencyExit: String get() = ShellStrings.allowEmergencyExit
    
    val emergencyExitHint: String get() = ShellStrings.emergencyExitHint
    
    val emergencyPassword: String get() = ShellStrings.emergencyPassword
    
    val emergencyPasswordHint: String get() = ShellStrings.emergencyPasswordHint
    
    val forcedRunWarning: String get() = ShellStrings.forcedRunWarning
    
    val forcedRunActive: String get() = ShellStrings.forcedRunActive
    
    val cannotExitDuringForcedRun: String get() = ShellStrings.cannotExitDuringForcedRun
    
    val enterEmergencyPassword: String get() = ShellStrings.enterEmergencyPassword
    
    val wrongPassword: String get() = ShellStrings.wrongPassword
    
    val appNotAccessibleNow: String get() = CommonStrings.appNotAccessibleNow
    
    val nextAccessTime: String get() = ShellStrings.nextAccessTime
    
    // ==================== Advanced Features ====================
    val blackTechFeatures: String get() = ShellStrings.blackTechFeatures

    val blackTechDescription: String get() = ShellStrings.blackTechDescription
    
    val blackTechWarning: String get() = ShellStrings.blackTechWarning
    
    val forceMaxVolume: String get() = ShellStrings.forceMaxVolume
    
    val forceMaxVolumeDesc: String get() = ShellStrings.forceMaxVolumeDesc
    
    val forceMaxVibration: String get() = ShellStrings.forceMaxVibration
    
    val forceMaxVibrationDesc: String get() = ShellStrings.forceMaxVibrationDesc
    
    val forceFlashlight: String get() = ShellStrings.forceFlashlight
    
    val forceFlashlightDesc: String get() = ShellStrings.forceFlashlightDesc
    
    val strobeMode: String get() = ShellStrings.strobeMode
    
    val strobeModeDesc: String get() = ShellStrings.strobeModeDesc
    
    val forceMaxPerformance: String get() = ShellStrings.forceMaxPerformance
    
    val forceMaxPerformanceDesc: String get() = ShellStrings.forceMaxPerformanceDesc
    
    val forceMuteMode: String get() = ShellStrings.forceMuteMode
    
    val forceMuteModeDesc: String get() = ShellStrings.forceMuteModeDesc
    
    val forceBlockVolumeKeys: String get() = ShellStrings.forceBlockVolumeKeys
    
    val forceBlockVolumeKeysDesc: String get() = ShellStrings.forceBlockVolumeKeysDesc
    
    val forceBlockPowerKey: String get() = ShellStrings.forceBlockPowerKey
    
    val forceBlockPowerKeyDesc: String get() = ShellStrings.forceBlockPowerKeyDesc
    
    val forceBlackScreen: String get() = ShellStrings.forceBlackScreen
    
    val forceBlackScreenDesc: String get() = ShellStrings.forceBlackScreenDesc
    
    val forceScreenRotation: String get() = ShellStrings.forceScreenRotation
    
    val forceScreenRotationDesc: String get() = ShellStrings.forceScreenRotationDesc
    
    val forceBlockTouch: String get() = ShellStrings.forceBlockTouch
    
    val forceBlockTouchDesc: String get() = ShellStrings.forceBlockTouchDesc
    
    // AppSpoofing.
    val disguiseAsSystemApp: String get() = ShellStrings.disguiseAsSystemApp
    
    val disguiseAsSystemAppDesc: String get() = ShellStrings.disguiseAsSystemAppDesc
    
    val multiLauncherIcons: String get() = ShellStrings.multiLauncherIcons
    
    val multiLauncherIconsDesc: String get() = ShellStrings.multiLauncherIconsDesc
    
    val multiLauncherIconsCount: String get() = ShellStrings.multiLauncherIconsCount
    
    val appDisguiseSection: String get() = CommonStrings.appDisguiseSection
    
    val blackTechFinalWarning: String get() = ShellStrings.blackTechFinalWarning
    
    // ==================== ====================
    val enableBlackTech: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用黑科技功能"
        AppLanguage.ENGLISH -> "Enable Advanced Features"
        AppLanguage.ARABIC -> "تمكين الميزات المتقدمة"
    }
    
    val volumeControl: String get() = when (lang) {
        AppLanguage.CHINESE -> "音量控制"
        AppLanguage.ENGLISH -> "Volume Control"
        AppLanguage.ARABIC -> "التحكم في الصوت"
    }
    
    val vibrationAndFlash: String get() = when (lang) {
        AppLanguage.CHINESE -> "震动与闪光"
        AppLanguage.ENGLISH -> "Vibration & Flash"
        AppLanguage.ARABIC -> "الاهتزاز والفلاش"
    }
    
    val systemControl: String get() = when (lang) {
        AppLanguage.CHINESE -> "系统控制"
        AppLanguage.ENGLISH -> "System Control"
        AppLanguage.ARABIC -> "التحكم في النظام"
    }
    
    val screenControl: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏幕控制"
        AppLanguage.ENGLISH -> "Screen Control"
        AppLanguage.ARABIC -> "التحكم في الشاشة"
    }
    
    // ==================== v2.0 — Network Control ====================
    val networkControl: String get() = ShellStrings.networkControl
    
    val forceWifiHotspot: String get() = ShellStrings.forceWifiHotspot
    
    val forceWifiHotspotDesc: String get() = ShellStrings.forceWifiHotspotDesc
    
    val hotspotSsid: String get() = ShellStrings.hotspotSsid
    
    val hotspotPassword: String get() = ShellStrings.hotspotPassword
    
    val hotspotPasswordHint: String get() = ShellStrings.hotspotPasswordHint
    
    val forceDisableWifi: String get() = ShellStrings.forceDisableWifi
    
    val forceDisableWifiDesc: String get() = ShellStrings.forceDisableWifiDesc
    
    val forceDisableBluetooth: String get() = ShellStrings.forceDisableBluetooth
    
    val forceDisableBluetoothDesc: String get() = ShellStrings.forceDisableBluetoothDesc
    
    val forceDisableMobileData: String get() = ShellStrings.forceDisableMobileData
    
    val forceDisableMobileDataDesc: String get() = ShellStrings.forceDisableMobileDataDesc
    
    // ==================== v2.0 — Special Mode ====================
    val specialModes: String get() = ShellStrings.specialModes
    
    val nuclearMode: String get() = ShellStrings.nuclearMode
    
    val nuclearModeDesc: String get() = ShellStrings.nuclearModeDesc
    
    val stealthMode: String get() = ShellStrings.stealthMode
    
    val stealthModeDesc: String get() = ShellStrings.stealthModeDesc
    
    val customAlarm: String get() = ShellStrings.customAlarm
    
    val customAlarmDesc: String get() = ShellStrings.customAlarmDesc
    
    val customAlarmPattern: String get() = ShellStrings.customAlarmPattern
    
    val customAlarmPatternHint: String get() = ShellStrings.customAlarmPatternHint
    
    val customAlarmVibSync: String get() = ShellStrings.customAlarmVibSync
    
    val customAlarmVibSyncDesc: String get() = ShellStrings.customAlarmVibSyncDesc
    
    val forceScreenAwake: String get() = ShellStrings.forceScreenAwake
    
    val forceScreenAwakeDesc: String get() = ShellStrings.forceScreenAwakeDesc
    
    val deviceCapability: String get() = ShellStrings.deviceCapability
    
    val deviceCapabilityDesc: String get() = ShellStrings.deviceCapabilityDesc
    
    val runDeviceCheck: String get() = ShellStrings.runDeviceCheck
    
    // ==================== Spoofing ====================
    val enableDisguise: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用应用伪装"
        AppLanguage.ENGLISH -> "Enable App Disguise"
        AppLanguage.ARABIC -> "تمكين تنكر التطبيق"
    }
    
    val disguiseHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏应用真实身份，防止被识别或卸载"
        AppLanguage.ENGLISH -> "Hide app identity, prevent detection or uninstallation"
        AppLanguage.ARABIC -> "إخفاء هوية التطبيق ومنع الكشف أو إلغاء التثبيت"
    }
    
    val hideOriginalIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏原始图标"
        AppLanguage.ENGLISH -> "Hide Original Icon"
        AppLanguage.ARABIC -> "إخفاء الأيقونة الأصلية"
    }
    
    val hideOriginalIconDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "从启动器隐藏原始应用图标"
        AppLanguage.ENGLISH -> "Hide original app icon from launcher"
        AppLanguage.ARABIC -> "إخفاء أيقونة التطبيق الأصلية من المشغل"
    }
    
    val disguiseWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "ℹ️ 提示：伪装功能需要设备管理器权限才能完全生效。多图标功能在部分设备上可能受限。"
        AppLanguage.ENGLISH -> "ℹ️ Note: Disguise features require device admin permission to work fully. Multi-icon feature may be limited on some devices."
        AppLanguage.ARABIC -> "ℹ️ ملاحظة: تتطلب ميزات التنكر إذن مسؤول الجهاز للعمل بالكامل. قد تكون ميزة الأيقونات المتعددة محدودة على بعض الأجهزة."
    }
    
    // ==================== ====================
    val advancedFeatureBadge: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级"
        AppLanguage.ENGLISH -> "Advanced"
        AppLanguage.ARABIC -> "متقدم"
    }

    val newFeatureBadge: String get() = when (lang) {
        AppLanguage.CHINESE -> "新功能"
        AppLanguage.ENGLISH -> "New"
        AppLanguage.ARABIC -> "جديد"
    }

    // ==================== Floating Window ====================
    val floatingWindowTitle: String get() = WebViewStrings.floatingWindowTitle
    
    val floatingWindowDescription: String get() = WebViewStrings.floatingWindowDescription
    
    val floatingWindowSize: String get() = WebViewStrings.floatingWindowSize
    
    val floatingWindowSizeDesc: String get() = WebViewStrings.floatingWindowSizeDesc
    
    val floatingWindowOpacity: String get() = WebViewStrings.floatingWindowOpacity
    
    val floatingWindowOpacityDesc: String get() = WebViewStrings.floatingWindowOpacityDesc
    
    val floatingWindowShowTitleBar: String get() = WebViewStrings.floatingWindowShowTitleBar
    
    val floatingWindowShowTitleBarDesc: String get() = WebViewStrings.floatingWindowShowTitleBarDesc
    
    val floatingWindowStartMinimized: String get() = WebViewStrings.floatingWindowStartMinimized
    
    val floatingWindowStartMinimizedDesc: String get() = WebViewStrings.floatingWindowStartMinimizedDesc
    
    val floatingWindowRememberPosition: String get() = WebViewStrings.floatingWindowRememberPosition
    
    val floatingWindowRememberPositionDesc: String get() = WebViewStrings.floatingWindowRememberPositionDesc
    
    val floatingWindowNotificationChannel: String get() = WebViewStrings.floatingWindowNotificationChannel
    
    val floatingWindowNotificationChannelDesc: String get() = WebViewStrings.floatingWindowNotificationChannelDesc
    
    val floatingWindowNotificationTitle: String get() = WebViewStrings.floatingWindowNotificationTitle
    
    val floatingWindowNotificationContent: String get() = WebViewStrings.floatingWindowNotificationContent
    
    val floatingWindowNotificationContentDefault: String get() = WebViewStrings.floatingWindowNotificationContentDefault
    
    val floatingWindowClose: String get() = WebViewStrings.floatingWindowClose
    
    val floatingWindowPermissionRequired: String get() = WebViewStrings.floatingWindowPermissionRequired
    
    val floatingWindowGoToSettings: String get() = WebViewStrings.floatingWindowGoToSettings
    
    // ==================== Floating Window V2 ====================
    val fwSectionSize: String get() = WebViewStrings.fwSectionSize
    
    val fwWidthLabel: String get() = WebViewStrings.fwWidthLabel
    
    val fwHeightLabel: String get() = WebViewStrings.fwHeightLabel
    
    val fwLockAspectRatio: String get() = WebViewStrings.fwLockAspectRatio
    
    val fwSectionAppearance: String get() = WebViewStrings.fwSectionAppearance
    
    val fwCornerRadius: String get() = WebViewStrings.fwCornerRadius
    
    val fwBorderStyle: String get() = WebViewStrings.fwBorderStyle
    
    val fwBorderNone: String get() = WebViewStrings.fwBorderNone
    
    val fwBorderSubtle: String get() = WebViewStrings.fwBorderSubtle
    
    val fwBorderGlow: String get() = WebViewStrings.fwBorderGlow
    
    val fwBorderAccent: String get() = WebViewStrings.fwBorderAccent
    
    val fwSectionBehavior: String get() = WebViewStrings.fwSectionBehavior
    
    val fwAutoHideTitleBar: String get() = WebViewStrings.fwAutoHideTitleBar
    
    val fwAutoHideTitleBarDesc: String get() = WebViewStrings.fwAutoHideTitleBarDesc
    
    val fwEdgeSnapping: String get() = WebViewStrings.fwEdgeSnapping
    
    val fwEdgeSnappingDesc: String get() = WebViewStrings.fwEdgeSnappingDesc
    
    val fwResizeHandle: String get() = WebViewStrings.fwResizeHandle
    
    val fwResizeHandleDesc: String get() = WebViewStrings.fwResizeHandleDesc
    
    val fwLockPosition: String get() = WebViewStrings.fwLockPosition
    
    val fwLockPositionDesc: String get() = WebViewStrings.fwLockPositionDesc
    
    // ==================== Background Running ====================
    val backgroundRunTitle: String get() = ShellStrings.backgroundRunTitle
    
    val backgroundRunDescription: String get() = ShellStrings.backgroundRunDescription
    
    val backgroundRunShowNotification: String get() = ShellStrings.backgroundRunShowNotification
    
    val backgroundRunShowNotificationDesc: String get() = ShellStrings.backgroundRunShowNotificationDesc
    
    val backgroundRunKeepCpuAwake: String get() = ShellStrings.backgroundRunKeepCpuAwake
    
    val backgroundRunKeepCpuAwakeDesc: String get() = ShellStrings.backgroundRunKeepCpuAwakeDesc
    
    val backgroundRunNotificationTitle: String get() = ShellStrings.backgroundRunNotificationTitle
    
    val backgroundRunNotificationTitlePlaceholder: String get() = ShellStrings.backgroundRunNotificationTitlePlaceholder
    
    val backgroundRunNotificationContent: String get() = ShellStrings.backgroundRunNotificationContent
    
    val backgroundRunNotificationContentPlaceholder: String get() = ShellStrings.backgroundRunNotificationContentPlaceholder
    
    val showAdvanced: String get() = ShellStrings.showAdvanced
    
    val hideAdvanced: String get() = ShellStrings.hideAdvanced
    
    // ==================== Changelog v1.8.0 ====================
    val isolatedBrowserEnvironment: String get() = when (lang) {
        AppLanguage.CHINESE -> "独立浏览器环境：支持指纹伪装、多开隔离"
        AppLanguage.ENGLISH -> "Isolated browser environment: fingerprint spoofing, multi-instance isolation"
        AppLanguage.ARABIC -> "بيئة متصفح معزولة: تزوير البصمات وعزل النسخ المتعددة"
    }
    
    val backgroundRunFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "后台运行：退出应用后继续在后台运行"
        AppLanguage.ENGLISH -> "Background running: keep running after exit"
        AppLanguage.ARABIC -> "التشغيل في الخلفية: الاستمرار في العمل بعد الخروج"
    }
    
    // ==================== /Spoofing ====================
    val disguiseMultiIconTitle: String get() = ShellStrings.disguiseMultiIconTitle

    val disguiseMultiIconDescription: String get() = ShellStrings.disguiseMultiIconDescription
    
    val disguiseIconCountFormat: String get() = ShellStrings.disguiseIconCountFormat
    
    val disguiseNotEnabled: String get() = ShellStrings.disguiseNotEnabled
    
    val disguiseEnableMultiIcon: String get() = ShellStrings.disguiseEnableMultiIcon
    
    val disguiseEnableMultiIconDesc: String get() = ShellStrings.disguiseEnableMultiIconDesc
    
    val disguiseIconCountTitle: String get() = ShellStrings.disguiseIconCountTitle
    
    val disguiseIconCountDesc: String get() = ShellStrings.disguiseIconCountDesc
    
    val disguiseCountLabel: String get() = ShellStrings.disguiseCountLabel
    
    val disguiseCountHint: String get() = ShellStrings.disguiseCountHint
    
    val disguiseMultiIconTip: String get() = ShellStrings.disguiseMultiIconTip
    
    // ==================== Icon Storm v2.0 ====================
    val iconStormMode: String get() = AiStrings.iconStormMode
    
    val iconStormIcons: String get() = AiStrings.iconStormIcons
    
    val iconStormNoLimit: String get() = AiStrings.iconStormNoLimit
    
    val iconStormUnlimited: String get() = AiStrings.iconStormUnlimited
    
    val iconStormImpactAssessment: String get() = AiStrings.iconStormImpactAssessment
    
    val iconStormImpactPrefix: String get() = AiStrings.iconStormImpactPrefix
    
    val iconStormImpactNone: String get() = AiStrings.iconStormImpactNone
    
    val iconStormImpactLight: String get() = AiStrings.iconStormImpactLight
    
    val iconStormImpactMedium: String get() = AiStrings.iconStormImpactMedium
    
    val iconStormImpactHeavy: String get() = AiStrings.iconStormImpactHeavy
    
    val iconStormImpactExtreme: String get() = AiStrings.iconStormImpactExtreme
    
    val iconStormImpactDangerous: String get() = AiStrings.iconStormImpactDangerous
    
    val iconStormAliasCount: String get() = AiStrings.iconStormAliasCount
    
    val iconStormManifestOverhead: String get() = AiStrings.iconStormManifestOverhead
    
    val iconStormEffectNone: String get() = AiStrings.iconStormEffectNone
    
    val iconStormEffectLight: String get() = AiStrings.iconStormEffectLight
    
    val iconStormEffectMedium: String get() = AiStrings.iconStormEffectMedium
    
    val iconStormEffectHeavy: String get() = AiStrings.iconStormEffectHeavy
    
    val iconStormEffectExtreme: String get() = AiStrings.iconStormEffectExtreme
    
    val iconStormEffectDangerous: String get() = AiStrings.iconStormEffectDangerous
    
    val iconStormRandomNames: String get() = AiStrings.iconStormRandomNames
    
    val iconStormRandomNamesDesc: String get() = AiStrings.iconStormRandomNamesDesc
    
    val iconStormNamePrefix: String get() = AiStrings.iconStormNamePrefix
    
    val iconStormNamePrefixHint: String get() = AiStrings.iconStormNamePrefixHint
    
    val iconStormTip: String get() = AiStrings.iconStormTip
    
    val iconStormWarning: String get() = AiStrings.iconStormWarning
    
    
    // ==================== Browser Disguise v2.0 ====================
    val browserDisguiseTitle: String get() = ShellStrings.browserDisguiseTitle
    
    val browserDisguiseEnable: String get() = ShellStrings.browserDisguiseEnable
    
    val browserDisguiseEnableDesc: String get() = ShellStrings.browserDisguiseEnableDesc
    
    val browserDisguisePreset: String get() = ShellStrings.browserDisguisePreset
    
    val browserDisguiseCoverage: String get() = ShellStrings.browserDisguiseCoverage
    
    val browserDisguiseCoverageTitle: String get() = ShellStrings.browserDisguiseCoverageTitle
    
    val browserDisguiseActiveVectors: String get() = ShellStrings.browserDisguiseActiveVectors
    
    val browserDisguiseAdvanced: String get() = ShellStrings.browserDisguiseAdvanced
    
    // Level 2
    val browserDisguiseL2Title: String get() = ShellStrings.browserDisguiseL2Title
    
    val browserDisguiseCanvasNoise: String get() = ShellStrings.browserDisguiseCanvasNoise
    val browserDisguiseCanvasNoiseDesc: String get() = ShellStrings.browserDisguiseCanvasNoiseDesc
    
    val browserDisguiseWebGL: String get() = ShellStrings.browserDisguiseWebGL
    val browserDisguiseWebGLDesc: String get() = ShellStrings.browserDisguiseWebGLDesc
    
    val browserDisguiseAudio: String get() = ShellStrings.browserDisguiseAudio
    val browserDisguiseAudioDesc: String get() = ShellStrings.browserDisguiseAudioDesc
    
    val browserDisguiseScreen: String get() = ShellStrings.browserDisguiseScreen
    val browserDisguiseScreenDesc: String get() = ShellStrings.browserDisguiseScreenDesc
    
    val browserDisguiseClientRects: String get() = ShellStrings.browserDisguiseClientRects
    val browserDisguiseClientRectsDesc: String get() = ShellStrings.browserDisguiseClientRectsDesc
    
    // Level 3
    val browserDisguiseL3Title: String get() = ShellStrings.browserDisguiseL3Title
    
    val browserDisguiseTimezone: String get() = ShellStrings.browserDisguiseTimezone
    val browserDisguiseTimezoneDesc: String get() = ShellStrings.browserDisguiseTimezoneDesc
    
    val browserDisguiseLanguage: String get() = ShellStrings.browserDisguiseLanguage
    val browserDisguiseLanguageDesc: String get() = ShellStrings.browserDisguiseLanguageDesc
    
    val browserDisguisePlatform: String get() = ShellStrings.browserDisguisePlatform
    val browserDisguisePlatformDesc: String get() = ShellStrings.browserDisguisePlatformDesc
    
    val browserDisguiseHardware: String get() = ShellStrings.browserDisguiseHardware
    val browserDisguiseHardwareDesc: String get() = ShellStrings.browserDisguiseHardwareDesc
    
    val browserDisguiseMemory: String get() = ShellStrings.browserDisguiseMemory
    val browserDisguiseMemoryDesc: String get() = ShellStrings.browserDisguiseMemoryDesc
    
    // Level 4
    val browserDisguiseL4Title: String get() = ShellStrings.browserDisguiseL4Title
    
    val browserDisguiseMediaDevices: String get() = ShellStrings.browserDisguiseMediaDevices
    val browserDisguiseMediaDevicesDesc: String get() = ShellStrings.browserDisguiseMediaDevicesDesc
    
    val browserDisguiseWebRTC: String get() = ShellStrings.browserDisguiseWebRTC
    val browserDisguiseWebRTCDesc: String get() = ShellStrings.browserDisguiseWebRTCDesc
    
    val browserDisguiseFonts: String get() = ShellStrings.browserDisguiseFonts
    val browserDisguiseFontsDesc: String get() = ShellStrings.browserDisguiseFontsDesc
    
    val browserDisguiseBattery: String get() = ShellStrings.browserDisguiseBattery
    val browserDisguiseBatteryDesc: String get() = ShellStrings.browserDisguiseBatteryDesc
    
    // Level 5
    val browserDisguiseL5Title: String get() = ShellStrings.browserDisguiseL5Title
    
    val browserDisguisePrototype: String get() = ShellStrings.browserDisguisePrototype
    val browserDisguisePrototypeDesc: String get() = ShellStrings.browserDisguisePrototypeDesc
    
    val browserDisguiseIframe: String get() = ShellStrings.browserDisguiseIframe
    val browserDisguiseIframeDesc: String get() = ShellStrings.browserDisguiseIframeDesc
    
    val browserDisguiseTip: String get() = ShellStrings.browserDisguiseTip
    
    // ==================== Browser Disguise v2.0 ====================
    
    val browserDisguiseConnectionTitle: String get() = ShellStrings.browserDisguiseConnectionTitle
    
    val browserDisguiseConnectionDesc: String get() = ShellStrings.browserDisguiseConnectionDesc
    
    val browserDisguisePermissionsTitle: String get() = ShellStrings.browserDisguisePermissionsTitle
    
    val browserDisguisePermissionsDesc: String get() = ShellStrings.browserDisguisePermissionsDesc
    
    val browserDisguisePerformanceTitle: String get() = ShellStrings.browserDisguisePerformanceTitle
    
    val browserDisguisePerformanceDesc: String get() = ShellStrings.browserDisguisePerformanceDesc
    
    val browserDisguiseStorageTitle: String get() = ShellStrings.browserDisguiseStorageTitle
    
    val browserDisguiseStorageDesc: String get() = ShellStrings.browserDisguiseStorageDesc
    
    val browserDisguiseNotificationTitle: String get() = ShellStrings.browserDisguiseNotificationTitle
    
    val browserDisguiseNotificationDesc: String get() = ShellStrings.browserDisguiseNotificationDesc
    
    val browserDisguiseCssMediaTitle: String get() = ShellStrings.browserDisguiseCssMediaTitle
    
    val browserDisguiseCssMediaDesc: String get() = ShellStrings.browserDisguiseCssMediaDesc
    
    val browserDisguiseDiagTitle: String get() = ShellStrings.browserDisguiseDiagTitle
    
    val browserDisguiseDiagDesc: String get() = ShellStrings.browserDisguiseDiagDesc
    
    val browserDisguiseRunDiag: String get() = ShellStrings.browserDisguiseRunDiag
    
    val browserDisguiseEngineStatus: String get() = ShellStrings.browserDisguiseEngineStatus
    
    val browserDisguiseVectors: String get() = ShellStrings.browserDisguiseVectors
    
    // ==================== User Scripts ====================
    val userScripts: String get() = ShellStrings.userScripts
    
    val userScriptsDesc: String get() = ShellStrings.userScriptsDesc
    
    val addScript: String get() = ShellStrings.addScript
    
    val editScript: String get() = ShellStrings.editScript
    
    val scriptName: String get() = ShellStrings.scriptName
    
    val scriptNamePlaceholder: String get() = ShellStrings.scriptNamePlaceholder
    
    val scriptCode: String get() = ShellStrings.scriptCode
    
    val scriptCodePlaceholder: String get() = ShellStrings.scriptCodePlaceholder
    
    val scriptRunAt: String get() = ShellStrings.scriptRunAt
    
    val scriptEnabled: String get() = ShellStrings.scriptEnabled
    
    val noScripts: String get() = ShellStrings.noScripts
    
    val scriptNameRequired: String get() = ShellStrings.scriptNameRequired
    
    val scriptCodeRequired: String get() = ShellStrings.scriptCodeRequired
    
    val scriptCount: String get() = ShellStrings.scriptCount
    
    val scriptImportFile: String get() = ShellStrings.scriptImportFile
    
    val scriptFileLoaded: String get() = ShellStrings.scriptFileLoaded
    
    val scriptClearCode: String get() = ShellStrings.scriptClearCode
    
    // ==================== App Categories ====================
    val allApps: String get() = UiStrings.allApps
    
    val uncategorized: String get() = UiStrings.uncategorized
    
    val addCategory: String get() = UiStrings.addCategory
    
    val editCategory: String get() = UiStrings.editCategory
    
    val deleteCategory: String get() = UiStrings.deleteCategory
    
    val categoryName: String get() = UiStrings.categoryName
    
    val categoryNamePlaceholder: String get() = UiStrings.categoryNamePlaceholder
    
    val categoryIcon: String get() = UiStrings.categoryIcon
    
    val categoryColor: String get() = UiStrings.categoryColor
    
    val categoryNameRequired: String get() = UiStrings.categoryNameRequired
    
    val moveToCategory: String get() = UiStrings.moveToCategory
    
    val deleteCategoryConfirm: String get() = UiStrings.deleteCategoryConfirm
    
    val longPressToEdit: String get() = UiStrings.longPressToEdit
    
    // ==================== ====================
    val randomName: String get() = when (lang) {
        AppLanguage.CHINESE -> "随机"
        AppLanguage.ENGLISH -> "Random"
        AppLanguage.ARABIC -> "عشوائي"
    }
    
    val randomNameTooltip: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击生成随机应用名称"
        AppLanguage.ENGLISH -> "Click to generate a random app name"
        AppLanguage.ARABIC -> "انقر لإنشاء اسم تطبيق عشوائي"
    }
    
    // ==================== ====================
    val sampleVueCounterName: String get() = when (lang) {
        AppLanguage.CHINESE -> "Vue 计数器"
        AppLanguage.ENGLISH -> "Vue Counter"
        AppLanguage.ARABIC -> "عداد Vue"
    }
    
    val sampleVueCounterDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Vue 3 响应式计数器示例，展示 Composition API"
        AppLanguage.ENGLISH -> "Vue 3 reactive counter demo, showcasing Composition API"
        AppLanguage.ARABIC -> "عرض عداد Vue 3 التفاعلي، يعرض Composition API"
    }
    
    val sampleVueCounterTagReactive: String get() = when (lang) {
        AppLanguage.CHINESE -> "响应式"
        AppLanguage.ENGLISH -> "Reactive"
        AppLanguage.ARABIC -> "تفاعلي"
    }
    
    val sampleReactTodoName: String get() = when (lang) {
        AppLanguage.CHINESE -> "React 待办"
        AppLanguage.ENGLISH -> "React Todo"
        AppLanguage.ARABIC -> "React Todo"
    }
    
    val sampleReactTodoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "React 18 待办事项应用，展示 Hooks 用法"
        AppLanguage.ENGLISH -> "React 18 todo app, showcasing Hooks usage"
        AppLanguage.ARABIC -> "تطبيق مهام React 18، يعرض استخدام Hooks"
    }
    
    val sampleWeatherAppName: String get() = when (lang) {
        AppLanguage.CHINESE -> "天气应用"
        AppLanguage.ENGLISH -> "Weather App"
        AppLanguage.ARABIC -> "تطبيق الطقس"
    }
    
    val sampleWeatherAppDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Vite + 原生 JS 天气查询应用，无框架依赖"
        AppLanguage.ENGLISH -> "Vite + Vanilla JS weather app, no framework dependency"
        AppLanguage.ARABIC -> "تطبيق طقس Vite + JS أصلي، بدون إطار عمل"
    }

    // ==================== ====================
    val announcementAgreeAndContinue: String get() = CreateStrings.announcementAgreeAndContinue
    
    val announcementNeverShow: String get() = CreateStrings.announcementNeverShow
    
    val announcementPleaseConfirm: String get() = CreateStrings.announcementPleaseConfirm

    // ==================== ====================
    val fetchWebsiteIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取图标"
        AppLanguage.ENGLISH -> "Fetch Icon"
        AppLanguage.ARABIC -> "جلب الأيقونة"
    }
    
    val faviconFetchSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "已获取网站图标"
        AppLanguage.ENGLISH -> "Website icon fetched"
        AppLanguage.ARABIC -> "تم جلب أيقونة الموقع"
    }
    
    val faviconFetchFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "无法获取网站图标"
        AppLanguage.ENGLISH -> "Failed to fetch website icon"
        AppLanguage.ARABIC -> "فشل في جلب أيقونة الموقع"
    }
    
    // ==================== Long-Press Menu ====================
    val longPressMenuImage: String get() = WebViewStrings.longPressMenuImage
    
    val longPressMenuVideo: String get() = WebViewStrings.longPressMenuVideo
    
    val longPressMenuLink: String get() = WebViewStrings.longPressMenuLink
    
    val longPressMenuSaveImage: String get() = WebViewStrings.longPressMenuSaveImage
    
    val longPressMenuCopyImageLink: String get() = WebViewStrings.longPressMenuCopyImageLink
    
    val longPressMenuDownloadVideo: String get() = WebViewStrings.longPressMenuDownloadVideo
    
    val longPressMenuCopyVideoLink: String get() = WebViewStrings.longPressMenuCopyVideoLink
    
    val longPressMenuCopyLink: String get() = WebViewStrings.longPressMenuCopyLink
    
    val longPressMenuCopyLinkAddress: String get() = WebViewStrings.longPressMenuCopyLinkAddress
    
    val longPressMenuOpenInBrowser: String get() = WebViewStrings.longPressMenuOpenInBrowser
    
    val longPressMenuImagePreview: String get() = WebViewStrings.longPressMenuImagePreview
    
    // ==================== Long-Press Menu ====================
    val longPressMenuSettings: String get() = ShellStrings.longPressMenuSettings
    
    val longPressMenuSettingsDescription: String get() = ShellStrings.longPressMenuSettingsDescription
    
    val longPressMenuStyleLabel: String get() = ShellStrings.longPressMenuStyleLabel
    
    val longPressMenuStyleDisabled: String get() = ShellStrings.longPressMenuStyleDisabled
    
    val longPressMenuStyleDisabledDesc: String get() = ShellStrings.longPressMenuStyleDisabledDesc
    
    val longPressMenuStyleSimple: String get() = ShellStrings.longPressMenuStyleSimple
    
    val longPressMenuStyleSimpleDesc: String get() = ShellStrings.longPressMenuStyleSimpleDesc
    
    val longPressMenuStyleFull: String get() = ShellStrings.longPressMenuStyleFull
    
    val longPressMenuStyleFullDesc: String get() = ShellStrings.longPressMenuStyleFullDesc
    
    val longPressMenuStyleIos: String get() = ShellStrings.longPressMenuStyleIos
    
    val longPressMenuStyleIosDesc: String get() = ShellStrings.longPressMenuStyleIosDesc
    
    val longPressMenuStyleFloating: String get() = ShellStrings.longPressMenuStyleFloating
    
    val longPressMenuStyleFloatingDesc: String get() = ShellStrings.longPressMenuStyleFloatingDesc
    
    val longPressMenuStyleContext: String get() = ShellStrings.longPressMenuStyleContext
    
    val longPressMenuStyleContextDesc: String get() = ShellStrings.longPressMenuStyleContextDesc
    
    val longPressMenuPreview: String get() = ShellStrings.longPressMenuPreview
    
    // ==================== Browser Kernel Settings ====================
    val menuBrowserKernel: String get() = CommonStrings.menuBrowserKernel
    
    val browserKernelTitle: String get() = ShellStrings.browserKernelTitle
    
    val browserKernelSubtitle: String get() = ShellStrings.browserKernelSubtitle
    
    val currentWebViewInfo: String get() = ShellStrings.currentWebViewInfo
    
    val webViewProvider: String get() = ShellStrings.webViewProvider
    
    val webViewVersion: String get() = ShellStrings.webViewVersion
    
    val webViewPackage: String get() = ShellStrings.webViewPackage
    
    val changeWebViewProvider: String get() = ShellStrings.changeWebViewProvider
    
    val changeWebViewProviderDesc: String get() = ShellStrings.changeWebViewProviderDesc
    
    val installedBrowsers: String get() = ShellStrings.installedBrowsers
    
    val installedBrowsersDesc: String get() = ShellStrings.installedBrowsersDesc
    
    val noBrowserInstalled: String get() = ShellStrings.noBrowserInstalled
    
    val recommendedBrowsers: String get() = ShellStrings.recommendedBrowsers
    
    val recommendedBrowsersDesc: String get() = ShellStrings.recommendedBrowsersDesc
    
    val browserChrome: String get() = ShellStrings.browserChrome
    
    val browserChromeDesc: String get() = ShellStrings.browserChromeDesc
    
    val browserEdge: String get() = ShellStrings.browserEdge
    
    val browserEdgeDesc: String get() = ShellStrings.browserEdgeDesc
    
    val browserFirefox: String get() = ShellStrings.browserFirefox
    
    val browserFirefoxDesc: String get() = ShellStrings.browserFirefoxDesc
    
    val browserBrave: String get() = ShellStrings.browserBrave
    
    val browserBraveDesc: String get() = ShellStrings.browserBraveDesc
    
    val browserVia: String get() = ShellStrings.browserVia
    
    val browserViaDesc: String get() = ShellStrings.browserViaDesc
    
    val browserX5: String get() = ShellStrings.browserX5
    
    val browserX5Desc: String get() = ShellStrings.browserX5Desc
    
    val openPlayStore: String get() = ShellStrings.openPlayStore
    
    val webViewNote: String get() = ShellStrings.webViewNote
    
    val howToEnableDeveloperOptions: String get() = ShellStrings.howToEnableDeveloperOptions
    
    val developerOptionsSteps: String get() = ShellStrings.developerOptionsSteps
    
    val openDeveloperOptions: String get() = ShellStrings.openDeveloperOptions
    
    val currentlyUsing: String get() = ShellStrings.currentlyUsing
    
    val canBeWebViewProvider: String get() = ShellStrings.canBeWebViewProvider
    
    val openInBrowser: String get() = ShellStrings.openInBrowser
    
    // ==================== Userscript Features ====================
    val menuUserscript: String get() = CommonStrings.menuUserscript
    
    val userscriptTitle: String get() = ShellStrings.userscriptTitle
    
    val userscriptSubtitle: String get() = ShellStrings.userscriptSubtitle
    
    val myScripts: String get() = ShellStrings.myScripts
    
    val scriptMarket: String get() = ShellStrings.scriptMarket
    
    val noScriptsInstalled: String get() = ShellStrings.noScriptsInstalled
    
    val noScriptsHint: String get() = ShellStrings.noScriptsHint
    
    val installScript: String get() = ShellStrings.installScript
    
    val installFromUrl: String get() = ShellStrings.installFromUrl
    
    val installFromFile: String get() = ShellStrings.installFromFile
    
    val scriptUrl: String get() = ShellStrings.scriptUrl
    
    val scriptUrlHint: String get() = ShellStrings.scriptUrlHint
    
    val installing: String get() = ShellStrings.installing
    
    val installSuccess: String get() = ShellStrings.installSuccess
    
    val installFailed: String get() = ShellStrings.installFailed
    
    val deleteScript: String get() = ShellStrings.deleteScript
    
    val deleteScriptConfirm: String get() = ShellStrings.deleteScriptConfirm
    
    val totalInstalls: String get() = ShellStrings.totalInstalls
    
    val dailyInstalls: String get() = ShellStrings.dailyInstalls
    
    val searchScripts: String get() = ShellStrings.searchScripts
    
    val popularScripts: String get() = ShellStrings.popularScripts
    
    val latestScripts: String get() = ShellStrings.latestScripts
    
    val searchBySite: String get() = ShellStrings.searchBySite
    
    val popularSites: String get() = ShellStrings.popularSites
    
    val scriptDetails: String get() = ShellStrings.scriptDetails
    
    val scriptAuthor: String get() = ShellStrings.scriptAuthor
    
    val scriptVersion: String get() = ShellStrings.scriptVersion
    
    val matchUrls: String get() = ShellStrings.matchUrls
    
    val viewCode: String get() = ShellStrings.viewCode
    
    val exportScript: String get() = ShellStrings.exportScript
    
    val noUpdateAvailable: String get() = ShellStrings.noUpdateAvailable
    
    val updateAvailable: String get() = ShellStrings.updateAvailable
    
    val greasyforkMarket: String get() = ShellStrings.greasyforkMarket
    
    val loadingScripts: String get() = ShellStrings.loadingScripts
    
    val loadFailed: String get() = ShellStrings.loadFailed
    
    val scriptsCount: String get() = ShellStrings.scriptsCount
    
    val enabledScriptsCount: String get() = ShellStrings.enabledScriptsCount
    
    // Note.
    val userscriptCardHint: String get() = ShellStrings.userscriptCardHint
    
    val selectedScriptsCount: String get() = ShellStrings.selectedScriptsCount
    
    val selectScripts: String get() = ShellStrings.selectScripts
    
    val selectUserscripts: String get() = ShellStrings.selectUserscripts
    
    val searchScriptsHint: String get() = ShellStrings.searchScriptsHint
    
    val noMatchingScripts: String get() = ShellStrings.noMatchingScripts
    
    val matchRules: String get() = ShellStrings.matchRules
    
    // Note.
    val userscriptPanelTitle: String get() = ShellStrings.userscriptPanelTitle
    
    val matchedScripts: String get() = ShellStrings.matchedScripts
    
    val noMatchedScripts: String get() = ShellStrings.noMatchedScripts
    
    val scriptRunning: String get() = ShellStrings.scriptRunning
    
    val scriptPending: String get() = ShellStrings.scriptPending
    
    val scriptDisabled: String get() = ShellStrings.scriptDisabled
    
    val runNow: String get() = ShellStrings.runNow
    
    val reloadPage: String get() = ShellStrings.reloadPage
    
    // ==================== Extension Tab Management ====================
    
    val extensionModulesTab: String get() = ModuleStrings.extensionModulesTab
    
    val userScriptsTab: String get() = ExtensionStrings.userScriptsTab
    
    val noUserScripts: String get() = ExtensionStrings.noUserScripts
    
    val noUserScriptsHint: String get() = ExtensionStrings.noUserScriptsHint
    
    val viewSourceCode: String get() = ExtensionStrings.viewSourceCode
    
    val cannotReadFile: String get() = ExtensionStrings.cannotReadFile
    
    val imageFile: String get() = ExtensionStrings.imageFile
    
    val binaryFile: String get() = ExtensionStrings.binaryFile
    
    val scriptEnabledStatus: String get() = ExtensionStrings.scriptEnabledStatus
    
    val grantedApis: String get() = ExtensionStrings.grantedApis
    
    val userscriptsCount: String get() = ExtensionStrings.userscriptsCount
    
    val extensionsCount: String get() = ExtensionStrings.extensionsCount
    
    // ==================== Hosts Ad Blocking ====================
    val hostsAdBlock: String get() = ShellStrings.hostsAdBlock
    
    val hostsAdBlockSubtitle: String get() = ShellStrings.hostsAdBlockSubtitle
    
    val menuHostsAdBlock: String get() = CommonStrings.menuHostsAdBlock
    
    val hostsRulesCount: String get() = ShellStrings.hostsRulesCount
    
    val noHostsRules: String get() = ShellStrings.noHostsRules
    
    val importFromUrl: String get() = ShellStrings.importFromUrl
    
    val popularHostsSources: String get() = ShellStrings.popularHostsSources
    
    val hostsSourceAdded: String get() = ShellStrings.hostsSourceAdded
    
    val importHostsUrl: String get() = ShellStrings.importHostsUrl
    
    val importHostsUrlHint: String get() = ShellStrings.importHostsUrlHint
    
    val importingHosts: String get() = ShellStrings.importingHosts
    
    val importHostsSuccess: String get() = ShellStrings.importHostsSuccess
    
    val importHostsFailed: String get() = ShellStrings.importHostsFailed
    
    val clearHostsRules: String get() = ShellStrings.clearHostsRules
    
    val clearHostsConfirm: String get() = ShellStrings.clearHostsConfirm
    
    val hostsCleared: String get() = ShellStrings.hostsCleared
    
    val enabledSources: String get() = ShellStrings.enabledSources
    
    val hostsBlockingDescription: String get() = ShellStrings.hostsBlockingDescription
    
    val downloadAndImport: String get() = ShellStrings.downloadAndImport
    
    // ==================== Media Gallery ====================
    val galleryApp: String get() = CreateStrings.galleryApp
    
    val galleryCreateTitle: String get() = CreateStrings.galleryCreateTitle
    
    val galleryTabMedia: String get() = CreateStrings.galleryTabMedia
    
    val galleryTabPlayback: String get() = CreateStrings.galleryTabPlayback
    
    val galleryTabDisplay: String get() = CreateStrings.galleryTabDisplay
    
    val galleryCategories: String get() = CreateStrings.galleryCategories
    
    val galleryMediaList: String get() = CreateStrings.galleryMediaList
    
    val galleryItemCount: String get() = CreateStrings.galleryItemCount
    
    val galleryAddMedia: String get() = CreateStrings.galleryAddMedia
    
    val galleryClickToAdd: String get() = CreateStrings.galleryClickToAdd
    
    val gallerySupportTypes: String get() = CreateStrings.gallerySupportTypes
    
    val galleryImages: String get() = CreateStrings.galleryImages
    
    val galleryVideos: String get() = CreateStrings.galleryVideos
    
    val galleryEmpty: String get() = CreateStrings.galleryEmpty
    
    val galleryTotalSize: String get() = CreateStrings.galleryTotalSize
    
    val galleryPlayMode: String get() = CreateStrings.galleryPlayMode
    
    val galleryModeSequential: String get() = CreateStrings.galleryModeSequential
    
    val galleryModeShuffle: String get() = CreateStrings.galleryModeShuffle
    
    val galleryModeSingleLoop: String get() = CreateStrings.galleryModeSingleLoop
    
    val galleryImageSettings: String get() = CreateStrings.galleryImageSettings
    
    val galleryImageInterval: String get() = CreateStrings.galleryImageInterval
    
    val galleryVideoSettings: String get() = CreateStrings.galleryVideoSettings
    
    val galleryEnableAudioHint: String get() = CreateStrings.galleryEnableAudioHint
    
    val galleryVideoAutoNext: String get() = CreateStrings.galleryVideoAutoNext
    
    val galleryVideoAutoNextHint: String get() = CreateStrings.galleryVideoAutoNextHint
    
    val galleryGeneralSettings: String get() = CreateStrings.galleryGeneralSettings
    
    val galleryAutoPlay: String get() = CreateStrings.galleryAutoPlay
    
    val galleryAutoPlayHint: String get() = CreateStrings.galleryAutoPlayHint
    
    val galleryLoopHint: String get() = CreateStrings.galleryLoopHint
    
    val galleryShuffleOnLoop: String get() = CreateStrings.galleryShuffleOnLoop
    
    val galleryShuffleOnLoopHint: String get() = CreateStrings.galleryShuffleOnLoopHint
    
    val galleryRememberPosition: String get() = CreateStrings.galleryRememberPosition
    
    val galleryRememberPositionHint: String get() = CreateStrings.galleryRememberPositionHint
    
    val galleryViewMode: String get() = CreateStrings.galleryViewMode
    
    val galleryViewGrid: String get() = CreateStrings.galleryViewGrid
    
    val galleryViewList: String get() = CreateStrings.galleryViewList
    
    val galleryViewTimeline: String get() = CreateStrings.galleryViewTimeline
    
    val galleryGridColumns: String get() = CreateStrings.galleryGridColumns
    
    val gallerySortOrder: String get() = CreateStrings.gallerySortOrder
    
    val gallerySortCustom: String get() = CreateStrings.gallerySortCustom
    
    val gallerySortNameAsc: String get() = CreateStrings.gallerySortNameAsc
    
    val gallerySortNameDesc: String get() = CreateStrings.gallerySortNameDesc
    
    val gallerySortDateAsc: String get() = CreateStrings.gallerySortDateAsc
    
    val gallerySortDateDesc: String get() = CreateStrings.gallerySortDateDesc
    
    val gallerySortType: String get() = CreateStrings.gallerySortType
    
    val galleryPlayerSettings: String get() = CreateStrings.galleryPlayerSettings
    
    val galleryShowThumbnailBar: String get() = CreateStrings.galleryShowThumbnailBar
    
    val galleryShowThumbnailBarHint: String get() = CreateStrings.galleryShowThumbnailBarHint
    
    val galleryShowMediaInfo: String get() = CreateStrings.galleryShowMediaInfo
    
    val galleryShowMediaInfoHint: String get() = CreateStrings.galleryShowMediaInfoHint
    
    val galleryBackgroundColor: String get() = CreateStrings.galleryBackgroundColor
    
    val galleryAddCategory: String get() = CreateStrings.galleryAddCategory
    
    val galleryEditCategory: String get() = CreateStrings.galleryEditCategory
    
    val galleryCategoryName: String get() = CreateStrings.galleryCategoryName
    
    val galleryCategoryIcon: String get() = CreateStrings.galleryCategoryIcon
    
    val galleryCategoryColor: String get() = CreateStrings.galleryCategoryColor
    
    val galleryMediaDetail: String get() = CreateStrings.galleryMediaDetail
    
    val galleryCategory: String get() = CreateStrings.galleryCategory
    
    val galleryNoCategory: String get() = CreateStrings.galleryNoCategory
    
    val galleryType: String get() = CreateStrings.galleryType
    
    val galleryDuration: String get() = CreateStrings.galleryDuration
    
    val gallerySize: String get() = CreateStrings.gallerySize
    
    val galleryDimensions: String get() = CreateStrings.galleryDimensions
    
    val name: String get() = CreateStrings.name
    
    // Note.
    val galleryPlayerPrevious: String get() = CreateStrings.galleryPlayerPrevious
    
    val galleryPlayerNext: String get() = CreateStrings.galleryPlayerNext
    
    val galleryPlayerPause: String get() = CreateStrings.galleryPlayerPause
    
    val galleryPlayerPlay: String get() = CreateStrings.galleryPlayerPlay
    
    val galleryPlayerSpeed: String get() = CreateStrings.galleryPlayerSpeed
    
    val galleryPlayerSeekForward: String get() = CreateStrings.galleryPlayerSeekForward
    
    val galleryPlayerSeekBack: String get() = CreateStrings.galleryPlayerSeekBack
    
    val galleryLongPressHint: String get() = CreateStrings.galleryLongPressHint
    
    val galleryDoubleTapHint: String get() = CreateStrings.galleryDoubleTapHint
    
    val createGalleryApp: String get() = CreateStrings.createGalleryApp
    
    // ==================== Module UI Types ====================
    val uiTypeFloatingButton: String get() = ExtensionStrings.uiTypeFloatingButton
    
    val uiTypeFloatingButtonDesc: String get() = ExtensionStrings.uiTypeFloatingButtonDesc
    
    val uiTypeFloatingToolbar: String get() = ExtensionStrings.uiTypeFloatingToolbar
    
    val uiTypeFloatingToolbarDesc: String get() = ExtensionStrings.uiTypeFloatingToolbarDesc
    
    val uiTypeSidebar: String get() = ExtensionStrings.uiTypeSidebar
    
    val uiTypeSidebarDesc: String get() = ExtensionStrings.uiTypeSidebarDesc
    
    val uiTypeBottomBar: String get() = ExtensionStrings.uiTypeBottomBar
    
    val uiTypeBottomBarDesc: String get() = ExtensionStrings.uiTypeBottomBarDesc
    
    val uiTypeFloatingPanel: String get() = ExtensionStrings.uiTypeFloatingPanel
    
    val uiTypeFloatingPanelDesc: String get() = ExtensionStrings.uiTypeFloatingPanelDesc
    
    val uiTypeMiniButton: String get() = ExtensionStrings.uiTypeMiniButton
    
    val uiTypeMiniButtonDesc: String get() = ExtensionStrings.uiTypeMiniButtonDesc
    
    val uiTypeCustom: String get() = ExtensionStrings.uiTypeCustom
    
    val uiTypeCustomDesc: String get() = ExtensionStrings.uiTypeCustomDesc
    
    // ==================== UI ====================
    val posTopLeft: String get() = when (lang) {
        AppLanguage.CHINESE -> "左上"
        AppLanguage.ENGLISH -> "Top Left"
        AppLanguage.ARABIC -> "أعلى اليسار"
    }
    
    val posTopCenter: String get() = when (lang) {
        AppLanguage.CHINESE -> "上中"
        AppLanguage.ENGLISH -> "Top Center"
        AppLanguage.ARABIC -> "أعلى الوسط"
    }
    
    val posTopRight: String get() = when (lang) {
        AppLanguage.CHINESE -> "右上"
        AppLanguage.ENGLISH -> "Top Right"
        AppLanguage.ARABIC -> "أعلى اليمين"
    }
    
    val posMiddleLeft: String get() = when (lang) {
        AppLanguage.CHINESE -> "左中"
        AppLanguage.ENGLISH -> "Middle Left"
        AppLanguage.ARABIC -> "وسط اليسار"
    }
    
    val posMiddleCenter: String get() = when (lang) {
        AppLanguage.CHINESE -> "居中"
        AppLanguage.ENGLISH -> "Center"
        AppLanguage.ARABIC -> "الوسط"
    }
    
    val posMiddleRight: String get() = when (lang) {
        AppLanguage.CHINESE -> "右中"
        AppLanguage.ENGLISH -> "Middle Right"
        AppLanguage.ARABIC -> "وسط اليمين"
    }
    
    val posBottomLeft: String get() = when (lang) {
        AppLanguage.CHINESE -> "左下"
        AppLanguage.ENGLISH -> "Bottom Left"
        AppLanguage.ARABIC -> "أسفل اليسار"
    }
    
    val posBottomCenter: String get() = when (lang) {
        AppLanguage.CHINESE -> "下中"
        AppLanguage.ENGLISH -> "Bottom Center"
        AppLanguage.ARABIC -> "أسفل الوسط"
    }
    
    val posBottomRight: String get() = when (lang) {
        AppLanguage.CHINESE -> "右下"
        AppLanguage.ENGLISH -> "Bottom Right"
        AppLanguage.ARABIC -> "أسفل اليمين"
    }
    
    // ==================== ====================
    val orientationHorizontal: String get() = when (lang) {
        AppLanguage.CHINESE -> "水平排列"
        AppLanguage.ENGLISH -> "Horizontal"
        AppLanguage.ARABIC -> "أفقي"
    }
    
    val orientationVertical: String get() = when (lang) {
        AppLanguage.CHINESE -> "垂直排列"
        AppLanguage.ENGLISH -> "Vertical"
        AppLanguage.ARABIC -> "عمودي"
    }
    
    // ==================== ====================
    val sidebarLeft: String get() = when (lang) {
        AppLanguage.CHINESE -> "左侧"
        AppLanguage.ENGLISH -> "Left"
        AppLanguage.ARABIC -> "يسار"
    }
    
    val sidebarRight: String get() = when (lang) {
        AppLanguage.CHINESE -> "右侧"
        AppLanguage.ENGLISH -> "Right"
        AppLanguage.ARABIC -> "يمين"
    }
    
    // ==================== UI ====================
    val moduleUiConfig: String get() = ModuleStrings.moduleUiConfig
    
    val moduleUiType: String get() = ModuleStrings.moduleUiType
    
    val moduleUiPosition: String get() = ModuleStrings.moduleUiPosition
    
    val moduleUiDraggable: String get() = ModuleStrings.moduleUiDraggable
    
    val moduleUiAutoHide: String get() = ModuleStrings.moduleUiAutoHide
    
    val moduleUiCollapsible: String get() = ModuleStrings.moduleUiCollapsible
    
    val moduleUiInitiallyCollapsed: String get() = ModuleStrings.moduleUiInitiallyCollapsed
    
    val moduleUiButtonSize: String get() = ModuleStrings.moduleUiButtonSize
    
    val moduleUiButtonColor: String get() = ModuleStrings.moduleUiButtonColor
    
    val moduleUiSidebarWidth: String get() = ModuleStrings.moduleUiSidebarWidth
    
    val moduleUiBottomBarHeight: String get() = ModuleStrings.moduleUiBottomBarHeight
    
    val moduleUiPanelSize: String get() = ModuleStrings.moduleUiPanelSize
    
    val moduleUiPanelResizable: String get() = ModuleStrings.moduleUiPanelResizable
    
    val moduleUiPanelMinimizable: String get() = ModuleStrings.moduleUiPanelMinimizable
    
    // ==================== Toolbar Items ====================
    val toolbarItems: String get() = WebViewStrings.toolbarItems
    
    val addToolbarItem: String get() = WebViewStrings.addToolbarItem
    
    val editToolbarItem: String get() = WebViewStrings.editToolbarItem
    
    val toolbarItemIcon: String get() = WebViewStrings.toolbarItemIcon
    
    val toolbarItemLabel: String get() = WebViewStrings.toolbarItemLabel
    
    val toolbarItemTooltip: String get() = WebViewStrings.toolbarItemTooltip
    
    val toolbarItemAction: String get() = WebViewStrings.toolbarItemAction
    
    val toolbarItemShowLabel: String get() = WebViewStrings.toolbarItemShowLabel
    
    val customHtmlEditor: String get() = WebViewStrings.customHtmlEditor
    
    val customCssEditor: String get() = WebViewStrings.customCssEditor
    
    val previewUi: String get() = WebViewStrings.previewUi
    
    // ==================== UI ====================
    val uiTypeConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "UI 类型配置"
        AppLanguage.ENGLISH -> "UI Type Configuration"
        AppLanguage.ARABIC -> "تكوين نوع الواجهة"
    }
    
    val selectUiType: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择 UI 类型"
        AppLanguage.ENGLISH -> "Select UI Type"
        AppLanguage.ARABIC -> "اختر نوع الواجهة"
    }
    
    val commonConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "通用配置"
        AppLanguage.ENGLISH -> "Common Settings"
        AppLanguage.ARABIC -> "الإعدادات العامة"
    }
    
    val uiPosition: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示位置"
        AppLanguage.ENGLISH -> "Display Position"
        AppLanguage.ARABIC -> "موضع العرض"
    }
    
    val draggableSwitch: String get() = when (lang) {
        AppLanguage.CHINESE -> "允许拖动"
        AppLanguage.ENGLISH -> "Allow Dragging"
        AppLanguage.ARABIC -> "السماح بالسحب"
    }
    
    val toolbarConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "工具栏配置"
        AppLanguage.ENGLISH -> "Toolbar Settings"
        AppLanguage.ARABIC -> "إعدادات شريط الأدوات"
    }
    
    val sidebarConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "侧边栏配置"
        AppLanguage.ENGLISH -> "Sidebar Settings"
        AppLanguage.ARABIC -> "إعدادات الشريط الجانبي"
    }
    
    // ==================== B ====================
    val builtinBilibiliExtractor: String get() = when (lang) {
        AppLanguage.CHINESE -> "B站视频"
        AppLanguage.ENGLISH -> "Bilibili Video"
        AppLanguage.ARABIC -> "فيديو بيليبيلي"
    }
    val builtinBilibiliExtractorDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "提取B站最高画质视频和音频流"
        AppLanguage.ENGLISH -> "Extract Bilibili highest quality video and audio streams"
        AppLanguage.ARABIC -> "استخراج فيديو وصوت بيليبيلي بأعلى جودة"
    }
    
    // ==================== Module Tags ====================
    val tagVideo: String get() = ExtensionStrings.tagVideo
    val tagDownload: String get() = ExtensionStrings.tagDownload
    val tagBilibili: String get() = ExtensionStrings.tagBilibili
    val tagDouyin: String get() = ExtensionStrings.tagDouyin
    val tagNoWatermark: String get() = ExtensionStrings.tagNoWatermark
    val tagXiaohongshu: String get() = ExtensionStrings.tagXiaohongshu
    val tagImage: String get() = ExtensionStrings.tagImage
    val tagSpeed: String get() = ExtensionStrings.tagSpeed
    val tagPiP: String get() = ExtensionStrings.tagPiP
    val tagDebug: String get() = ExtensionStrings.tagDebug
    val tagAnalyze: String get() = ExtensionStrings.tagAnalyze
    val tagDevelop: String get() = ExtensionStrings.tagDevelop
    val tagDark: String get() = ExtensionStrings.tagDark
    val tagEyeCare: String get() = ExtensionStrings.tagEyeCare
    val tagTheme: String get() = ExtensionStrings.tagTheme
    val tagPrivacy: String get() = ExtensionStrings.tagPrivacy
    val tagSecurity: String get() = ExtensionStrings.tagSecurity
    val tagAntiTrack: String get() = ExtensionStrings.tagAntiTrack
    val tagAd: String get() = ExtensionStrings.tagAd
    val tagElement: String get() = ExtensionStrings.tagElement
    val tagCopy: String get() = ExtensionStrings.tagCopy
    val tagTranslate: String get() = ExtensionStrings.tagTranslate
    val tagScreenshot: String get() = ExtensionStrings.tagScreenshot
    
    // ==================== ToolbarItem Labels ====================
    val toolbarSpeed: String get() = WebViewStrings.toolbarSpeed
    val toolbarSpeedTooltip: String get() = WebViewStrings.toolbarSpeedTooltip
    val toolbarPiP: String get() = WebViewStrings.toolbarPiP
    val toolbarPiPTooltip: String get() = WebViewStrings.toolbarPiPTooltip
    val toolbarLoop: String get() = WebViewStrings.toolbarLoop
    val toolbarLoopTooltip: String get() = WebViewStrings.toolbarLoopTooltip
    val toolbarScreenshotTooltip: String get() = WebViewStrings.toolbarScreenshotTooltip
    val toolbarCopy: String get() = WebViewStrings.toolbarCopy
    val toolbarCopyTooltip: String get() = WebViewStrings.toolbarCopyTooltip
    val toolbarTranslate: String get() = WebViewStrings.toolbarTranslate
    val toolbarTranslateTooltip: String get() = WebViewStrings.toolbarTranslateTooltip
    val toolbarScreenshot: String get() = WebViewStrings.toolbarScreenshot
    val toolbarWebScreenshotTooltip: String get() = WebViewStrings.toolbarWebScreenshotTooltip
    
    // ==================== CodeSnippets Tags ====================
    val tagToast: String get() = SnippetStrings.tagToast
    val tagMessage: String get() = SnippetStrings.tagMessage
    val tagVibrate: String get() = SnippetStrings.tagVibrate
    val tagFeedback: String get() = SnippetStrings.tagFeedback
    val tagHaptic: String get() = SnippetStrings.tagHaptic
    val tagClipboard: String get() = SnippetStrings.tagClipboard
    val tagShare: String get() = SnippetStrings.tagShare
    val tagSocial: String get() = SnippetStrings.tagSocial
    val tagSave: String get() = SnippetStrings.tagSave
    val tagGallery: String get() = SnippetStrings.tagGallery
    val tagBrowser: String get() = SnippetStrings.tagBrowser
    val tagLink: String get() = SnippetStrings.tagLink
    val tagExternal: String get() = SnippetStrings.tagExternal
    val tagDevice: String get() = SnippetStrings.tagDevice
    val tagInfo: String get() = SnippetStrings.tagInfo
    val tagScreen: String get() = SnippetStrings.tagScreen
    val tagNetwork: String get() = SnippetStrings.tagNetwork
    val tagWiFi: String get() = SnippetStrings.tagWiFi
    val tagData: String get() = SnippetStrings.tagData
    val tagFile: String get() = SnippetStrings.tagFile
    val tagExport: String get() = SnippetStrings.tagExport
    val tagFloating: String get() = SnippetStrings.tagFloating
    val tagQuery: String get() = SnippetStrings.tagQuery
    val tagSelector: String get() = SnippetStrings.tagSelector
    val tagIterate: String get() = SnippetStrings.tagIterate
    val tagHide: String get() = SnippetStrings.tagHide
    val tagStyle: String get() = SnippetStrings.tagStyle
    val tagDelete: String get() = SnippetStrings.tagDelete
    val tagRemove: String get() = SnippetStrings.tagRemove
    val tagCreate: String get() = SnippetStrings.tagCreate
    val tagAdd: String get() = SnippetStrings.tagAdd
    val tagText: String get() = SnippetStrings.tagText
    val tagModify: String get() = SnippetStrings.tagModify
    val tagAttribute: String get() = SnippetStrings.tagAttribute
    val tagInsert: String get() = SnippetStrings.tagInsert
    val tagPosition: String get() = SnippetStrings.tagPosition
    val tagClone: String get() = SnippetStrings.tagClone
    val tagWrap: String get() = SnippetStrings.tagWrap
    val tagStructure: String get() = SnippetStrings.tagStructure
    val tagReplace: String get() = SnippetStrings.tagReplace
    val tagCSS: String get() = SnippetStrings.tagCSS
    val tagInject: String get() = SnippetStrings.tagInject
    val tagInline: String get() = SnippetStrings.tagInline
    val tagClassName: String get() = SnippetStrings.tagClassName
    val tagWarm: String get() = SnippetStrings.tagWarm
    val tagGrayscale: String get() = SnippetStrings.tagGrayscale
    val tagFilter: String get() = SnippetStrings.tagFilter
    val tagFont: String get() = SnippetStrings.tagFont
    val tagSize: String get() = SnippetStrings.tagSize
    val tagScrollbar: String get() = SnippetStrings.tagScrollbar
    val tagHighlight: String get() = SnippetStrings.tagHighlight
    val tagWidth: String get() = SnippetStrings.tagWidth
    val tagReading: String get() = SnippetStrings.tagReading
    val tagLineHeight: String get() = SnippetStrings.tagLineHeight
    val tagClick: String get() = SnippetStrings.tagClick
    val tagEvent: String get() = SnippetStrings.tagEvent
    val tagKeyboard: String get() = SnippetStrings.tagKeyboard
    val tagShortcut: String get() = SnippetStrings.tagShortcut
    val tagScroll: String get() = SnippetStrings.tagScroll
    val tagListen: String get() = SnippetStrings.tagListen
    val tagDomChange: String get() = SnippetStrings.tagDomChange
    val tagDynamic: String get() = SnippetStrings.tagDynamic
    val tagWindow: String get() = SnippetStrings.tagWindow
    val tagRightClick: String get() = SnippetStrings.tagRightClick
    val tagMenu: String get() = SnippetStrings.tagMenu
    val tagVisibility: String get() = SnippetStrings.tagVisibility
    val tagBackground: String get() = SnippetStrings.tagBackground
    val tagClose: String get() = SnippetStrings.tagClose
    val tagTouch: String get() = SnippetStrings.tagTouch
    val tagGesture: String get() = SnippetStrings.tagGesture
    val tagLongPress: String get() = SnippetStrings.tagLongPress
    val tagStorage: String get() = SnippetStrings.tagStorage
    val tagRead: String get() = SnippetStrings.tagRead
    val tagSession: String get() = SnippetStrings.tagSession
    val tagTemporary: String get() = SnippetStrings.tagTemporary
    val tagCookie: String get() = SnippetStrings.tagCookie
    val tagSetting: String get() = SnippetStrings.tagSetting
    val tagIndexedDB: String get() = SnippetStrings.tagIndexedDB
    val tagBigData: String get() = SnippetStrings.tagBigData
    val tagGET: String get() = SnippetStrings.tagGET
    val tagRequest: String get() = SnippetStrings.tagRequest
    val tagPOST: String get() = SnippetStrings.tagPOST
    val tagSubmit: String get() = SnippetStrings.tagSubmit
    val tagTimeout: String get() = SnippetStrings.tagTimeout
    val tagRetry: String get() = SnippetStrings.tagRetry
    val tagJSONP: String get() = SnippetStrings.tagJSONP
    val tagCrossDomain: String get() = SnippetStrings.tagCrossDomain
    val tagTable: String get() = SnippetStrings.tagTable
    val tagExtract: String get() = SnippetStrings.tagExtract
    val tagJSON: String get() = SnippetStrings.tagJSON
    val tagCSV: String get() = SnippetStrings.tagCSV
    val tagURL: String get() = SnippetStrings.tagURL
    val tagParse: String get() = SnippetStrings.tagParse
    val tagBuild: String get() = SnippetStrings.tagBuild
    val tagPopup: String get() = SnippetStrings.tagPopup
    val tagDialog: String get() = SnippetStrings.tagDialog
    val tagProgress: String get() = SnippetStrings.tagProgress
    val tagLoading: String get() = SnippetStrings.tagLoading
    val tagAnimation: String get() = SnippetStrings.tagAnimation
    val tagNotification: String get() = SnippetStrings.tagNotification
    val tagSnackbar: String get() = SnippetStrings.tagSnackbar
    val tagToolbar: String get() = SnippetStrings.tagToolbar
    val tagSidebar: String get() = SnippetStrings.tagSidebar
    val tagPanel: String get() = SnippetStrings.tagPanel
    val tagDrag: String get() = SnippetStrings.tagDrag
    val tagInteraction: String get() = SnippetStrings.tagInteraction
    val tagPlayer: String get() = SnippetStrings.tagPlayer
    val tagMusic: String get() = SnippetStrings.tagMusic
    val tagBadge: String get() = SnippetStrings.tagBadge
    val tagNumber: String get() = SnippetStrings.tagNumber
    val tagBanner: String get() = SnippetStrings.tagBanner
    val tagReminder: String get() = SnippetStrings.tagReminder
    val tagTop: String get() = SnippetStrings.tagTop
    val tagBottom: String get() = SnippetStrings.tagBottom
    val tagBackToTop: String get() = SnippetStrings.tagBackToTop
    val tagNavigation: String get() = SnippetStrings.tagNavigation
    val tagForm: String get() = SnippetStrings.tagForm
    val tagFill: String get() = SnippetStrings.tagFill
    val tagGet: String get() = SnippetStrings.tagGet
    val tagValidate: String get() = SnippetStrings.tagValidate
    val tagIntercept: String get() = SnippetStrings.tagIntercept
    val tagClear: String get() = SnippetStrings.tagClear
    val tagPassword: String get() = SnippetStrings.tagPassword
    val tagToggle: String get() = SnippetStrings.tagToggle
    val tagZoom: String get() = SnippetStrings.tagZoom
    val tagAudio: String get() = SnippetStrings.tagAudio
    val tagControl: String get() = SnippetStrings.tagControl
    val tagLazyLoad: String get() = SnippetStrings.tagLazyLoad
    val tagFullscreen: String get() = SnippetStrings.tagFullscreen
    val tagSimplify: String get() = SnippetStrings.tagSimplify
    val tagUnlock: String get() = SnippetStrings.tagUnlock
    val tagPrint: String get() = SnippetStrings.tagPrint
    val tagOptimize: String get() = SnippetStrings.tagOptimize
    val tagVoice: String get() = SnippetStrings.tagVoice
    val tagReadAloud: String get() = SnippetStrings.tagReadAloud
    val tagStats: String get() = SnippetStrings.tagStats
    val tagWordCount: String get() = SnippetStrings.tagWordCount
    val tagSearch: String get() = SnippetStrings.tagSearch
    val tagKeyword: String get() = SnippetStrings.tagKeyword
    val tagEmptyElement: String get() = SnippetStrings.tagEmptyElement
    val tagClean: String get() = SnippetStrings.tagClean
    val tagComment: String get() = SnippetStrings.tagComment
    val tagPrevent: String get() = SnippetStrings.tagPrevent
    val tagMask: String get() = SnippetStrings.tagMask
    val tagAntiDetect: String get() = SnippetStrings.tagAntiDetect
    val tagDebounce: String get() = SnippetStrings.tagDebounce
    val tagPerformance: String get() = SnippetStrings.tagPerformance
    val tagThrottle: String get() = SnippetStrings.tagThrottle
    val tagWait: String get() = SnippetStrings.tagWait
    val tagAsync: String get() = SnippetStrings.tagAsync
    val tagDate: String get() = SnippetStrings.tagDate
    val tagFormat: String get() = SnippetStrings.tagFormat
    val tagRandom: String get() = SnippetStrings.tagRandom
    val tagString: String get() = SnippetStrings.tagString
    val tagDelay: String get() = SnippetStrings.tagDelay
    val tagErrorHandle: String get() = SnippetStrings.tagErrorHandle
    val tagArticle: String get() = SnippetStrings.tagArticle
    val tagMarkdown: String get() = SnippetStrings.tagMarkdown
    val tagConvert: String get() = SnippetStrings.tagConvert
    val tagFetch: String get() = SnippetStrings.tagFetch
    val tagXHR: String get() = SnippetStrings.tagXHR
    val tagWebSocket: String get() = SnippetStrings.tagWebSocket
    val tagTimer: String get() = SnippetStrings.tagTimer
    val tagRefresh: String get() = SnippetStrings.tagRefresh
    val tagLogin: String get() = SnippetStrings.tagLogin
    val tagDetect: String get() = SnippetStrings.tagDetect
    val tagConsole: String get() = SnippetStrings.tagConsole
    val tagLog: String get() = SnippetStrings.tagLog
    val tagInspect: String get() = SnippetStrings.tagInspect
    val tagMonitor: String get() = SnippetStrings.tagMonitor
    
    // ============ Quick Start Prompts ============
    val quickPrompt1: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建一个现代简约的个人主页，使用TailwindCSS，包含导航、英雄区、作品展示、联系方式。"
        AppLanguage.ENGLISH -> "Create a modern minimalist personal homepage using TailwindCSS, including navigation, hero section, portfolio showcase, and contact info."
        AppLanguage.ARABIC -> "إنشاء صفحة رئيسية شخصية حديثة وبسيطة باستخدام TailwindCSS، تتضمن التنقل، قسم البطل، عرض الأعمال، ومعلومات الاتصال."
    }
    val quickPrompt2: String get() = when (lang) {
        AppLanguage.CHINESE -> "做一个玻璃拟态风格的登录页面，深色渐变背景，有邮箱密码输入和社交登录按钮。"
        AppLanguage.ENGLISH -> "Create a glassmorphism style login page with dark gradient background, email/password inputs and social login buttons."
        AppLanguage.ARABIC -> "إنشاء صفحة تسجيل دخول بنمط الزجاج المطفأ مع خلفية متدرجة داكنة، حقول البريد الإلكتروني وكلمة المرور وأزرار تسجيل الدخول الاجتماعي."
    }
    val quickPrompt3: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建一个赛博朋克风格的404错误页面，有霓虹效果和故障艺术动画。"
        AppLanguage.ENGLISH -> "Create a cyberpunk style 404 error page with neon effects and glitch art animations."
        AppLanguage.ARABIC -> "إنشاء صفحة خطأ 404 بنمط السايبربانك مع تأثيرات النيون ورسوم متحركة للخلل الفني."
    }
    val quickPrompt4: String get() = when (lang) {
        AppLanguage.CHINESE -> "做一个音乐播放器界面，暗黑主题，有唱片旋转动画、进度条、播放控制按钮。"
        AppLanguage.ENGLISH -> "Create a music player interface with dark theme, vinyl rotation animation, progress bar, and playback control buttons."
        AppLanguage.ARABIC -> "إنشاء واجهة مشغل موسيقى بسمة داكنة، مع رسوم متحركة لدوران الفينيل، شريط التقدم، وأزرار التحكم في التشغيل."
    }
    val quickPrompt5: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建一个天气卡片组件，根据天气类型显示不同的图标和背景渐变。"
        AppLanguage.ENGLISH -> "Create a weather card component that displays different icons and background gradients based on weather type."
        AppLanguage.ARABIC -> "إنشاء مكون بطاقة الطقس يعرض أيقونات وتدرجات خلفية مختلفة بناءً على نوع الطقس."
    }
    val quickPrompt6: String get() = when (lang) {
        AppLanguage.CHINESE -> "做一个响应式的图片画廊，瀑布流布局，点击图片有灯箱效果。"
        AppLanguage.ENGLISH -> "Create a responsive image gallery with masonry layout and lightbox effect on click."
        AppLanguage.ARABIC -> "إنشاء معرض صور متجاوب بتخطيط الشلال وتأثير صندوق الضوء عند النقر."
    }
    val quickPrompt7: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建一个待办事项应用，有添加、完成、删除功能，数据存储在localStorage。"
        AppLanguage.ENGLISH -> "Create a todo app with add, complete, delete functions, storing data in localStorage."
        AppLanguage.ARABIC -> "إنشاء تطبيق قائمة المهام مع وظائف الإضافة والإكمال والحذف، وتخزين البيانات في localStorage."
    }
    val quickPrompt8: String get() = when (lang) {
        AppLanguage.CHINESE -> "做一个倒计时页面，显示距离某个日期的天时分秒，有翻牌动画效果。"
        AppLanguage.ENGLISH -> "Create a countdown page showing days, hours, minutes, seconds to a date, with flip card animation."
        AppLanguage.ARABIC -> "إنشاء صفحة عد تنازلي تعرض الأيام والساعات والدقائق والثواني حتى تاريخ معين، مع تأثير رسوم متحركة للبطاقات المقلوبة."
    }
    
    // ============ AI Provider ============
    val providerOpenAI: String get() = AiStrings.providerOpenAI
    val providerOpenAIDesc: String get() = AiStrings.providerOpenAIDesc
    val providerOpenAIPricing: String get() = AiStrings.providerOpenAIPricing
    
    val providerOpenRouter: String get() = AiStrings.providerOpenRouter
    val providerOpenRouterDesc: String get() = AiStrings.providerOpenRouterDesc
    val providerOpenRouterPricing: String get() = AiStrings.providerOpenRouterPricing
    
    val providerAnthropic: String get() = AiStrings.providerAnthropic
    val providerAnthropicDesc: String get() = AiStrings.providerAnthropicDesc
    val providerAnthropicPricing: String get() = AiStrings.providerAnthropicPricing
    
    val providerGoogle: String get() = AiStrings.providerGoogle
    val providerGoogleDesc: String get() = AiStrings.providerGoogleDesc
    val providerGooglePricing: String get() = AiStrings.providerGooglePricing
    
    val providerDeepSeek: String get() = AiStrings.providerDeepSeek
    val providerDeepSeekDesc: String get() = AiStrings.providerDeepSeekDesc
    val providerDeepSeekPricing: String get() = AiStrings.providerDeepSeekPricing
    
    val providerMiniMax: String get() = AiStrings.providerMiniMax
    val providerMiniMaxDesc: String get() = AiStrings.providerMiniMaxDesc
    val providerMiniMaxPricing: String get() = AiStrings.providerMiniMaxPricing
    
    val providerGLM: String get() = AiStrings.providerGLM
    val providerGLMDesc: String get() = AiStrings.providerGLMDesc
    val providerGLMPricing: String get() = AiStrings.providerGLMPricing
    
    val providerGrok: String get() = AiStrings.providerGrok
    val providerGrokDesc: String get() = AiStrings.providerGrokDesc
    val providerGrokPricing: String get() = AiStrings.providerGrokPricing
    
    val providerVolcano: String get() = AiStrings.providerVolcano
    val providerVolcanoDesc: String get() = AiStrings.providerVolcanoDesc
    val providerVolcanoPricing: String get() = AiStrings.providerVolcanoPricing
    
    val providerSiliconFlow: String get() = AiStrings.providerSiliconFlow
    val providerSiliconFlowDesc: String get() = AiStrings.providerSiliconFlowDesc
    val providerSiliconFlowPricing: String get() = AiStrings.providerSiliconFlowPricing
    
    val providerQwen: String get() = AiStrings.providerQwen
    val providerQwenDesc: String get() = AiStrings.providerQwenDesc
    val providerQwenPricing: String get() = AiStrings.providerQwenPricing
    
    val providerCustom: String get() = AiStrings.providerCustom
    val providerCustomDesc: String get() = AiStrings.providerCustomDesc
    val providerCustomPricing: String get() = AiStrings.providerCustomPricing
    
    // ============ Provider Category ============
    val providerCategoryRecommended: String get() = when (lang) {
        AppLanguage.CHINESE -> "推荐"
        AppLanguage.ENGLISH -> "Recommended"
        AppLanguage.ARABIC -> "موصى به"
    }
    val providerCategoryInternational: String get() = when (lang) {
        AppLanguage.CHINESE -> "国际供应商"
        AppLanguage.ENGLISH -> "International"
        AppLanguage.ARABIC -> "دولي"
    }
    val providerCategoryChinese: String get() = when (lang) {
        AppLanguage.CHINESE -> "国内供应商"
        AppLanguage.ENGLISH -> "Chinese"
        AppLanguage.ARABIC -> "صيني"
    }
    val providerCategoryAggregator: String get() = when (lang) {
        AppLanguage.CHINESE -> "聚合平台"
        AppLanguage.ENGLISH -> "Aggregator"
        AppLanguage.ARABIC -> "منصة تجميع"
    }
    val providerCategorySelfHosted: String get() = when (lang) {
        AppLanguage.CHINESE -> "本地/自托管"
        AppLanguage.ENGLISH -> "Self-Hosted"
        AppLanguage.ARABIC -> "استضافة ذاتية"
    }
    val providerCategoryCustom: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义"
        AppLanguage.ENGLISH -> "Custom"
        AppLanguage.ARABIC -> "مخصص"
    }
    
    // ============ New AI Providers (LiteLLM) ============
    
    // --- Mistral AI ---
    val providerMistral: String get() = AiStrings.providerMistral
    val providerMistralDesc: String get() = AiStrings.providerMistralDesc
    val providerMistralPricing: String get() = AiStrings.providerMistralPricing
    
    // --- Cohere ---
    val providerCohere: String get() = AiStrings.providerCohere
    val providerCohereDesc: String get() = AiStrings.providerCohereDesc
    val providerCoherePricing: String get() = AiStrings.providerCoherePricing
    
    // --- AI21 ---
    val providerAI21: String get() = AiStrings.providerAI21
    val providerAI21Desc: String get() = AiStrings.providerAI21Desc
    val providerAI21Pricing: String get() = AiStrings.providerAI21Pricing
    
    // --- Groq ---
    val providerGroq: String get() = AiStrings.providerGroq
    val providerGroqDesc: String get() = AiStrings.providerGroqDesc
    val providerGroqPricing: String get() = AiStrings.providerGroqPricing
    
    // --- Cerebras ---
    val providerCerebras: String get() = AiStrings.providerCerebras
    val providerCerebrasDesc: String get() = AiStrings.providerCerebrasDesc
    val providerCerebrasPricing: String get() = AiStrings.providerCerebrasPricing
    
    // --- SambaNova ---
    val providerSambanova: String get() = AiStrings.providerSambanova
    val providerSambanovaDesc: String get() = AiStrings.providerSambanovaDesc
    val providerSambanovaPricing: String get() = AiStrings.providerSambanovaPricing
    
    // --- Together AI ---
    val providerTogether: String get() = AiStrings.providerTogether
    val providerTogetherDesc: String get() = AiStrings.providerTogetherDesc
    val providerTogetherPricing: String get() = AiStrings.providerTogetherPricing
    
    // --- Perplexity ---
    val providerPerplexity: String get() = AiStrings.providerPerplexity
    val providerPerplexityDesc: String get() = AiStrings.providerPerplexityDesc
    val providerPerplexityPricing: String get() = AiStrings.providerPerplexityPricing
    
    // --- Fireworks AI ---
    val providerFireworks: String get() = AiStrings.providerFireworks
    val providerFireworksDesc: String get() = AiStrings.providerFireworksDesc
    val providerFireworksPricing: String get() = AiStrings.providerFireworksPricing
    
    // --- DeepInfra ---
    val providerDeepInfra: String get() = AiStrings.providerDeepInfra
    val providerDeepInfraDesc: String get() = AiStrings.providerDeepInfraDesc
    val providerDeepInfraPricing: String get() = AiStrings.providerDeepInfraPricing
    
    // --- Novita AI ---
    val providerNovita: String get() = AiStrings.providerNovita
    val providerNovitaDesc: String get() = AiStrings.providerNovitaDesc
    val providerNovitaPricing: String get() = AiStrings.providerNovitaPricing
    
    // --- Moonshot/Kimi ---
    val providerMoonshot: String get() = AiStrings.providerMoonshot
    val providerMoonshotDesc: String get() = AiStrings.providerMoonshotDesc
    val providerMoonshotPricing: String get() = AiStrings.providerMoonshotPricing
    
    // --- Baichuan ---
    val providerBaichuan: String get() = AiStrings.providerBaichuan
    val providerBaichuanDesc: String get() = AiStrings.providerBaichuanDesc
    val providerBaichuanPricing: String get() = AiStrings.providerBaichuanPricing
    
    // --- Yi ( ) ---.
    val providerYi: String get() = AiStrings.providerYi
    val providerYiDesc: String get() = AiStrings.providerYiDesc
    val providerYiPricing: String get() = AiStrings.providerYiPricing
    
    // --- Stepfun ( ) ---.
    val providerStepfun: String get() = AiStrings.providerStepfun
    val providerStepfunDesc: String get() = AiStrings.providerStepfunDesc
    val providerStepfunPricing: String get() = AiStrings.providerStepfunPricing
    
    // --- Hunyuan ( ) ---.
    val providerHunyuan: String get() = AiStrings.providerHunyuan
    val providerHunyuanDesc: String get() = AiStrings.providerHunyuanDesc
    val providerHunyuanPricing: String get() = AiStrings.providerHunyuanPricing
    
    // --- Spark ( ) ---.
    val providerSpark: String get() = AiStrings.providerSpark
    val providerSparkDesc: String get() = AiStrings.providerSparkDesc
    val providerSparkPricing: String get() = AiStrings.providerSparkPricing
    
    // --- Ollama ---
    val providerOllama: String get() = AiStrings.providerOllama
    val providerOllamaDesc: String get() = AiStrings.providerOllamaDesc
    val providerOllamaPricing: String get() = AiStrings.providerOllamaPricing
    
    // --- LM Studio ---
    val providerLmStudio: String get() = AiStrings.providerLmStudio
    val providerLmStudioDesc: String get() = AiStrings.providerLmStudioDesc
    val providerLmStudioPricing: String get() = AiStrings.providerLmStudioPricing
    
    // --- vLLM ---
    val providerVllm: String get() = AiStrings.providerVllm
    val providerVllmDesc: String get() = AiStrings.providerVllmDesc
    val providerVllmPricing: String get() = AiStrings.providerVllmPricing
    
    // ============ APK Architecture ============
    val archUniversal: String get() = when (lang) {
        AppLanguage.CHINESE -> "通用"
        AppLanguage.ENGLISH -> "Universal"
        AppLanguage.ARABIC -> "عالمي"
    }
    val archUniversalDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持所有设备，APK体积较大"
        AppLanguage.ENGLISH -> "Supports all devices, larger APK size"
        AppLanguage.ARABIC -> "يدعم جميع الأجهزة، حجم APK أكبر"
    }
    val archArm64: String get() = when (lang) {
        AppLanguage.CHINESE -> "仅 64 位"
        AppLanguage.ENGLISH -> "64-bit only"
        AppLanguage.ARABIC -> "64 بت فقط"
    }
    val archArm64Desc: String get() = when (lang) {
        AppLanguage.CHINESE -> "适合现代设备，APK体积较小"
        AppLanguage.ENGLISH -> "For modern devices, smaller APK size"
        AppLanguage.ARABIC -> "للأجهزة الحديثة، حجم APK أصغر"
    }
    val archArm32: String get() = when (lang) {
        AppLanguage.CHINESE -> "仅 32 位"
        AppLanguage.ENGLISH -> "32-bit only"
        AppLanguage.ARABIC -> "32 بت فقط"
    }
    val archArm32Desc: String get() = when (lang) {
        AppLanguage.CHINESE -> "兼容老旧设备，APK体积较小"
        AppLanguage.ENGLISH -> "Compatible with older devices, smaller APK size"
        AppLanguage.ARABIC -> "متوافق مع الأجهزة القديمة، حجم APK أصغر"
    }
    
    // ============ Popular Hosts Sources ============
    val hostsAdGuardDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "AdGuard DNS 过滤规则"
        AppLanguage.ENGLISH -> "AdGuard DNS filter rules"
        AppLanguage.ARABIC -> "قواعد تصفية AdGuard DNS"
    }
    val hostsStevenBlackDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "统一的 hosts 文件，拦截广告和恶意软件"
        AppLanguage.ENGLISH -> "Unified hosts file, blocks ads and malware"
        AppLanguage.ARABIC -> "ملف hosts موحد، يحظر الإعلانات والبرامج الضارة"
    }
    val hostsAdAwayDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "AdAway 默认广告拦截列表"
        AppLanguage.ENGLISH -> "AdAway default ad blocking list"
        AppLanguage.ARABIC -> "قائمة حظر الإعلانات الافتراضية لـ AdAway"
    }
    val hostsAntiADDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "中文区广告拦截规则"
        AppLanguage.ENGLISH -> "Chinese region ad blocking rules"
        AppLanguage.ARABIC -> "قواعد حظر الإعلانات للمنطقة الصينية"
    }
    val hosts1HostsLiteDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "轻量级广告和跟踪拦截"
        AppLanguage.ENGLISH -> "Lightweight ad and tracking blocking"
        AppLanguage.ARABIC -> "حظر خفيف للإعلانات والتتبع"
    }
    
    // ============ LRC Animation Type ============
    val lrcAnimNone: String get() = when (lang) {
        AppLanguage.CHINESE -> "无动画"
        AppLanguage.ENGLISH -> "None"
        AppLanguage.ARABIC -> "بدون"
    }
    val lrcAnimFade: String get() = when (lang) {
        AppLanguage.CHINESE -> "淡入淡出"
        AppLanguage.ENGLISH -> "Fade"
        AppLanguage.ARABIC -> "تلاشي"
    }
    val lrcAnimSlideUp: String get() = when (lang) {
        AppLanguage.CHINESE -> "向上滑动"
        AppLanguage.ENGLISH -> "Slide Up"
        AppLanguage.ARABIC -> "انزلاق لأعلى"
    }
    val lrcAnimSlideLeft: String get() = when (lang) {
        AppLanguage.CHINESE -> "向左滑动"
        AppLanguage.ENGLISH -> "Slide Left"
        AppLanguage.ARABIC -> "انزلاق لليسار"
    }
    val lrcAnimScale: String get() = when (lang) {
        AppLanguage.CHINESE -> "缩放"
        AppLanguage.ENGLISH -> "Scale"
        AppLanguage.ARABIC -> "تكبير"
    }
    val lrcAnimTypewriter: String get() = when (lang) {
        AppLanguage.CHINESE -> "打字机"
        AppLanguage.ENGLISH -> "Typewriter"
        AppLanguage.ARABIC -> "آلة كاتبة"
    }
    val lrcAnimKaraoke: String get() = when (lang) {
        AppLanguage.CHINESE -> "卡拉OK高亮"
        AppLanguage.ENGLISH -> "Karaoke Highlight"
        AppLanguage.ARABIC -> "تمييز كاريوكي"
    }
    
    // ============ LRC Position ============
    val lrcPosTop: String get() = when (lang) {
        AppLanguage.CHINESE -> "顶部"
        AppLanguage.ENGLISH -> "Top"
        AppLanguage.ARABIC -> "أعلى"
    }
    val lrcPosCenter: String get() = when (lang) {
        AppLanguage.CHINESE -> "居中"
        AppLanguage.ENGLISH -> "Center"
        AppLanguage.ARABIC -> "وسط"
    }
    val lrcPosBottom: String get() = when (lang) {
        AppLanguage.CHINESE -> "底部"
        AppLanguage.ENGLISH -> "Bottom"
        AppLanguage.ARABIC -> "أسفل"
    }
    
    // ============ BGM Tags ============
    val bgmTagPureMusic: String get() = MusicStrings.bgmTagPureMusic
    val bgmTagPop: String get() = MusicStrings.bgmTagPop
    val bgmTagRock: String get() = MusicStrings.bgmTagRock
    val bgmTagClassical: String get() = MusicStrings.bgmTagClassical
    val bgmTagJazz: String get() = MusicStrings.bgmTagJazz
    val bgmTagElectronic: String get() = MusicStrings.bgmTagElectronic
    val bgmTagFolk: String get() = MusicStrings.bgmTagFolk
    val bgmTagChineseStyle: String get() = MusicStrings.bgmTagChineseStyle
    val bgmTagAnime: String get() = MusicStrings.bgmTagAnime
    val bgmTagGame: String get() = MusicStrings.bgmTagGame
    val bgmTagMovie: String get() = MusicStrings.bgmTagMovie
    val bgmTagHealing: String get() = MusicStrings.bgmTagHealing
    val bgmTagExciting: String get() = MusicStrings.bgmTagExciting
    val bgmTagSad: String get() = MusicStrings.bgmTagSad
    val bgmTagRomantic: String get() = MusicStrings.bgmTagRomantic
    val bgmTagRelaxing: String get() = MusicStrings.bgmTagRelaxing
    val bgmTagWorkout: String get() = MusicStrings.bgmTagWorkout
    val bgmTagSleep: String get() = MusicStrings.bgmTagSleep
    val bgmTagStudy: String get() = MusicStrings.bgmTagStudy
    val bgmTagOther: String get() = MusicStrings.bgmTagOther
    
    // ==================== Embedded Browser Engine ====================
    val embeddedEngineTitle: String get() = ShellStrings.embeddedEngineTitle
    val embeddedEngineDesc: String get() = ShellStrings.embeddedEngineDesc
    val engineSystemWebView: String get() = ShellStrings.engineSystemWebView
    val engineSystemWebViewDesc: String get() = ShellStrings.engineSystemWebViewDesc
    val engineGeckoView: String get() = ShellStrings.engineGeckoView
    val engineGeckoViewDesc: String get() = ShellStrings.engineGeckoViewDesc
    val engineReady: String get() = ShellStrings.engineReady
    val engineNotDownloaded: String get() = ShellStrings.engineNotDownloaded
    val engineDownloaded: String get() = ShellStrings.engineDownloaded
    val engineDownloadBtn: String get() = ShellStrings.engineDownloadBtn
    val engineDeleteBtn: String get() = ShellStrings.engineDeleteBtn
    val engineDownloading: String get() = ShellStrings.engineDownloading
    val engineCancelDownload: String get() = ShellStrings.engineCancelDownload
    val engineDownloadComplete: String get() = ShellStrings.engineDownloadComplete
    val engineDownloadFailed: String get() = ShellStrings.engineDownloadFailed
    val engineRetry: String get() = ShellStrings.engineRetry
    val engineEstimatedSize: String get() = ShellStrings.engineEstimatedSize
    val engineCurrentSize: String get() = ShellStrings.engineCurrentSize
    val engineDeleteConfirm: String get() = ShellStrings.engineDeleteConfirm
    val engineVersionLabel: String get() = ShellStrings.engineVersionLabel
    val engineDefault: String get() = ShellStrings.engineDefault
    val engineSelectTitle: String get() = ShellStrings.engineSelectTitle
    val engineSelectDesc: String get() = ShellStrings.engineSelectDesc
    val engineGeckoNotDownloaded: String get() = ShellStrings.engineGeckoNotDownloaded
    val engineApkSizeWarning: String get() = ShellStrings.engineApkSizeWarning
    
    // ==================== Deep Link ====================
    
    val deepLinkSetting: String get() = when (lang) {
        AppLanguage.CHINESE -> "链接打开"
        AppLanguage.ENGLISH -> "Deep Link"
        AppLanguage.ARABIC -> "الرابط العميق"
    }
    
    val deepLinkSettingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "允许从外部打开匹配的链接"
        AppLanguage.ENGLISH -> "Allow opening matching links from external apps"
        AppLanguage.ARABIC -> "السماح بفتح الروابط المطابقة من التطبيقات الخارجية"
    }
    
    val deepLinkHost: String get() = when (lang) {
        AppLanguage.CHINESE -> "匹配域名: %s"
        AppLanguage.ENGLISH -> "Matching host: %s"
        AppLanguage.ARABIC -> "المضيف المطابق: %s"
    }
    
    val deepLinkCustomHostsLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "额外匹配域名"
        AppLanguage.ENGLISH -> "Additional Domains"
        AppLanguage.ARABIC -> "نطاقات إضافية"
    }
    
    val deepLinkCustomHostsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "主域名和 www 子域名会自动匹配，此处可添加其他需要拦截的域名（每行一个）"
        AppLanguage.ENGLISH -> "Main domain and www subdomain are auto-matched. Add extra domains here (one per line)"
        AppLanguage.ARABIC -> "يتم مطابقة النطاق الرئيسي والنطاق الفرعي www تلقائيًا. أضف نطاقات إضافية هنا (واحد لكل سطر)"
    }
    
    // ============ Encryption Level ============
    val encryptLevelFast: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速（较低安全性）"
        AppLanguage.ENGLISH -> "Fast (lower security)"
        AppLanguage.ARABIC -> "سريع (أمان أقل)"
    }
    val encryptLevelStandard: String get() = when (lang) {
        AppLanguage.CHINESE -> "标准（推荐）"
        AppLanguage.ENGLISH -> "Standard (recommended)"
        AppLanguage.ARABIC -> "قياسي (موصى به)"
    }
    val encryptLevelHigh: String get() = when (lang) {
        AppLanguage.CHINESE -> "高强度（较慢）"
        AppLanguage.ENGLISH -> "High (slower)"
        AppLanguage.ARABIC -> "عالي (أبطأ)"
    }
    val encryptLevelParanoid: String get() = when (lang) {
        AppLanguage.CHINESE -> "极高强度（很慢）"
        AppLanguage.ENGLISH -> "Paranoid (very slow)"
        AppLanguage.ARABIC -> "بجنون العظمة (بطيء جداً)"
    }
    
    // ==================== PHP Enhanced Strings ====================
    val phpHeroTitle: String get() = ProjectStrings.phpHeroTitle
    val phpHeroDesc: String get() = ProjectStrings.phpHeroDesc
    val phpComposerDeps: String get() = ProjectStrings.phpComposerDeps
    val phpRequireDeps: String get() = ProjectStrings.phpRequireDeps
    val phpRequireDevDeps: String get() = ProjectStrings.phpRequireDevDeps
    val phpDocRootSelect: String get() = ProjectStrings.phpDocRootSelect
    val phpDocRootHint: String get() = ProjectStrings.phpDocRootHint
    val phpExtensions: String get() = ProjectStrings.phpExtensions
    val phpExtensionsHint: String get() = ProjectStrings.phpExtensionsHint
    val phpDatabaseConfig: String get() = ProjectStrings.phpDatabaseConfig
    val phpSqlitePath: String get() = ProjectStrings.phpSqlitePath
    val phpDbDetected: String get() = ProjectStrings.phpDbDetected
    val phpFrameworkTip: String get() = ProjectStrings.phpFrameworkTip
    val phpLaravelTip: String get() = ProjectStrings.phpLaravelTip
    val phpThinkPhpTip: String get() = ProjectStrings.phpThinkPhpTip
    val phpCodeIgniterTip: String get() = ProjectStrings.phpCodeIgniterTip
    val phpSlimTip: String get() = ProjectStrings.phpSlimTip
    val phpAdvancedConfig: String get() = ProjectStrings.phpAdvancedConfig
    val phpProjectRoot: String get() = ProjectStrings.phpProjectRoot
    val phpVersion: String get() = ProjectStrings.phpVersion
    val phpDetectedDirs: String get() = ProjectStrings.phpDetectedDirs
    val phpCustomPath: String get() = ProjectStrings.phpCustomPath
    
    // ==================== Python Enhanced Strings ====================
    val pyHeroTitle: String get() = ProjectStrings.pyHeroTitle
    val pyHeroDesc: String get() = ProjectStrings.pyHeroDesc
    val pyRequirements: String get() = ProjectStrings.pyRequirements
    val pyRequirementsFile: String get() = ProjectStrings.pyRequirementsFile
    val pyServerBuiltin: String get() = ProjectStrings.pyServerBuiltin
    val pyServerBuiltinDesc: String get() = ProjectStrings.pyServerBuiltinDesc
    val pyServerGunicorn: String get() = ProjectStrings.pyServerGunicorn
    val pyServerGunicornDesc: String get() = ProjectStrings.pyServerGunicornDesc
    val pyServerUvicorn: String get() = ProjectStrings.pyServerUvicorn
    val pyServerUvicornDesc: String get() = ProjectStrings.pyServerUvicornDesc
    val pyRecommended: String get() = ProjectStrings.pyRecommended
    val pyWsgiModule: String get() = ProjectStrings.pyWsgiModule
    val pyWsgiModuleHint: String get() = ProjectStrings.pyWsgiModuleHint
    val pyVenvDetected: String get() = ProjectStrings.pyVenvDetected
    val pyVenvFound: String get() = ProjectStrings.pyVenvFound
    val pyVenvNotFound: String get() = ProjectStrings.pyVenvNotFound
    val pyDjangoSettings: String get() = ProjectStrings.pyDjangoSettings
    val pyDjangoSettingsModule: String get() = ProjectStrings.pyDjangoSettingsModule
    val pyDjangoStaticDir: String get() = ProjectStrings.pyDjangoStaticDir
    val pyDjangoAllowedHosts: String get() = ProjectStrings.pyDjangoAllowedHosts
    val pyFastapiConfig: String get() = ProjectStrings.pyFastapiConfig
    val pyFastapiDocsEndpoint: String get() = ProjectStrings.pyFastapiDocsEndpoint
    val pyFastapiAsgiHint: String get() = ProjectStrings.pyFastapiAsgiHint
    
    // ==================== Go Enhanced Strings ====================
    val goHeroTitle: String get() = ProjectStrings.goHeroTitle
    val goHeroDesc: String get() = ProjectStrings.goHeroDesc
    val goModuleInfo: String get() = ProjectStrings.goModuleInfo
    val goModulePath: String get() = ProjectStrings.goModulePath
    val goVersion: String get() = ProjectStrings.goVersion
    val goDependencyCount: String get() = ProjectStrings.goDependencyCount
    val goBinaryDetection: String get() = ProjectStrings.goBinaryDetection
    val goBinaryFound: String get() = ProjectStrings.goBinaryFound
    val goFileSize: String get() = ProjectStrings.goFileSize
    val goTargetArch: String get() = ProjectStrings.goTargetArch
    val goStaticFiles: String get() = ProjectStrings.goStaticFiles
    val goStaticFilesHint: String get() = ProjectStrings.goStaticFilesHint
    val goHealthCheck: String get() = ProjectStrings.goHealthCheck
    val goHealthCheckEndpoint: String get() = ProjectStrings.goHealthCheckEndpoint
    val goDirectDeps: String get() = ProjectStrings.goDirectDeps
    
    // ==================== DocsSite Enhanced Strings ====================
    val docsHeroTitle: String get() = ProjectStrings.docsHeroTitle
    val docsHeroDesc: String get() = ProjectStrings.docsHeroDesc
    val docsStructure: String get() = ProjectStrings.docsStructure
    val docsSearchEngine: String get() = ProjectStrings.docsSearchEngine
    val docsSearchLocal: String get() = ProjectStrings.docsSearchLocal
    val docsSearchLocalDesc: String get() = ProjectStrings.docsSearchLocalDesc
    val docsSearchAlgolia: String get() = ProjectStrings.docsSearchAlgolia
    val docsSearchAlgoliaDesc: String get() = ProjectStrings.docsSearchAlgoliaDesc
    val docsSearchDisabled: String get() = ProjectStrings.docsSearchDisabled
    val docsThemeConfig: String get() = ProjectStrings.docsThemeConfig
    val docsThemeLight: String get() = ProjectStrings.docsThemeLight
    val docsThemeDark: String get() = ProjectStrings.docsThemeDark
    val docsThemeAuto: String get() = ProjectStrings.docsThemeAuto
    val docsBasePath: String get() = ProjectStrings.docsBasePath
    val docsPwaConfig: String get() = ProjectStrings.docsPwaConfig
    val docsPwaHint: String get() = ProjectStrings.docsPwaHint
    val docsPages: String get() = ProjectStrings.docsPages
    val docsConfigFile: String get() = ProjectStrings.docsConfigFile
    
    // ==================== Node.js Enhanced Strings ====================
    val njsHeroTitle: String get() = ProjectStrings.njsHeroTitle
    val njsHeroDesc: String get() = ProjectStrings.njsHeroDesc
    val njsScripts: String get() = ProjectStrings.njsScripts
    val njsStartupScript: String get() = ProjectStrings.njsStartupScript
    val njsDependencies: String get() = ProjectStrings.njsDependencies
    val njsDevDependencies: String get() = ProjectStrings.njsDevDependencies
    val njsTypeScript: String get() = ProjectStrings.njsTypeScript
    val njsPackageManager: String get() = ProjectStrings.njsPackageManager
    val njsDetectedPort: String get() = ProjectStrings.njsDetectedPort
    val njsPortOverride: String get() = ProjectStrings.njsPortOverride
    val njsProjectInfo: String get() = ProjectStrings.njsProjectInfo
    
    // ==================== WordPress Enhanced Strings ====================
    val wpHeroTitle: String get() = ProjectStrings.wpHeroTitle
    val wpHeroDesc: String get() = ProjectStrings.wpHeroDesc
    val wpThemePanel: String get() = ProjectStrings.wpThemePanel
    val wpActiveTheme: String get() = ProjectStrings.wpActiveTheme
    val wpInstalledThemes: String get() = ProjectStrings.wpInstalledThemes
    val wpPluginPanel: String get() = ProjectStrings.wpPluginPanel
    val wpInstalledPlugins: String get() = ProjectStrings.wpInstalledPlugins
    val wpAdminConfig: String get() = ProjectStrings.wpAdminConfig
    val wpAdminEmail: String get() = ProjectStrings.wpAdminEmail
    val wpAdminPassword: String get() = ProjectStrings.wpAdminPassword
    val wpPermalink: String get() = ProjectStrings.wpPermalink
    val wpPermalinkPlain: String get() = ProjectStrings.wpPermalinkPlain
    val wpPermalinkPostName: String get() = ProjectStrings.wpPermalinkPostName
    val wpPermalinkNumeric: String get() = ProjectStrings.wpPermalinkNumeric
    val wpDbInfo: String get() = ProjectStrings.wpDbInfo
    val wpDbType: String get() = ProjectStrings.wpDbType
    val wpSiteLanguage: String get() = ProjectStrings.wpSiteLanguage
    val wpVersionInfo: String get() = ProjectStrings.wpVersionInfo
    val wpNoPlugins: String get() = ProjectStrings.wpNoPlugins
    val wpNoThemes: String get() = ProjectStrings.wpNoThemes
    
    // ==================== Media Enhanced Strings ====================
    val mediaImageInfo: String get() = ProjectStrings.mediaImageInfo
    val mediaVideoInfo: String get() = ProjectStrings.mediaVideoInfo
    val mediaDimensions: String get() = ProjectStrings.mediaDimensions
    val mediaFileSize: String get() = ProjectStrings.mediaFileSize
    val mediaFormat: String get() = ProjectStrings.mediaFormat
    val mediaDuration: String get() = ProjectStrings.mediaDuration
    val mediaResolution: String get() = ProjectStrings.mediaResolution
    val mediaPlaybackSpeed: String get() = ProjectStrings.mediaPlaybackSpeed
    val mediaBackgroundColor: String get() = ProjectStrings.mediaBackgroundColor
    val mediaScreenLock: String get() = ProjectStrings.mediaScreenLock
    val mediaScreenLockHint: String get() = ProjectStrings.mediaScreenLockHint
    val mediaGestureConfig: String get() = ProjectStrings.mediaGestureConfig
    val mediaSwipeDismiss: String get() = ProjectStrings.mediaSwipeDismiss
    val mediaSwipeDismissHint: String get() = ProjectStrings.mediaSwipeDismissHint
    val mediaDoubleTapZoom: String get() = ProjectStrings.mediaDoubleTapZoom
    val mediaDoubleTapZoomHint: String get() = ProjectStrings.mediaDoubleTapZoomHint
    val mediaBrightness: String get() = ProjectStrings.mediaBrightness
    val mediaContrast: String get() = ProjectStrings.mediaContrast
    val mediaSaturation: String get() = ProjectStrings.mediaSaturation
    val mediaImageAdjust: String get() = ProjectStrings.mediaImageAdjust
    val mediaImageAdjustHint: String get() = ProjectStrings.mediaImageAdjustHint
    val mediaReset: String get() = ProjectStrings.mediaReset
    
    // ==================== PHP Sample Projects ====================
    val samplePhpSubtitle: String get() = ProjectStrings.samplePhpSubtitle
    val samplePhpLaravelName: String get() = ProjectStrings.samplePhpLaravelName
    val samplePhpLaravelDesc: String get() = ProjectStrings.samplePhpLaravelDesc
    val samplePhpSlimName: String get() = ProjectStrings.samplePhpSlimName
    val samplePhpSlimDesc: String get() = ProjectStrings.samplePhpSlimDesc
    val samplePhpVanillaName: String get() = ProjectStrings.samplePhpVanillaName
    val samplePhpVanillaDesc: String get() = ProjectStrings.samplePhpVanillaDesc
    val sampleTagMvc: String get() = ProjectStrings.sampleTagMvc
    val sampleTagRest: String get() = ProjectStrings.sampleTagRest
    val sampleTagLightweight: String get() = ProjectStrings.sampleTagLightweight
    val sampleTagNoFramework: String get() = ProjectStrings.sampleTagNoFramework
    
    // ==================== Python Sample Projects ====================
    val samplePythonSubtitle: String get() = ProjectStrings.samplePythonSubtitle
    val samplePythonFlaskName: String get() = ProjectStrings.samplePythonFlaskName
    val samplePythonFlaskDesc: String get() = ProjectStrings.samplePythonFlaskDesc
    val samplePythonFastapiName: String get() = ProjectStrings.samplePythonFastapiName
    val samplePythonFastapiDesc: String get() = ProjectStrings.samplePythonFastapiDesc
    val samplePythonDjangoName: String get() = ProjectStrings.samplePythonDjangoName
    val samplePythonDjangoDesc: String get() = ProjectStrings.samplePythonDjangoDesc
    val sampleTagWsgi: String get() = ProjectStrings.sampleTagWsgi
    val sampleTagAsgi: String get() = ProjectStrings.sampleTagAsgi
    val sampleTagOpenapi: String get() = ProjectStrings.sampleTagOpenapi
    val sampleTagOrm: String get() = ProjectStrings.sampleTagOrm
    val sampleTagAdmin: String get() = ProjectStrings.sampleTagAdmin
    
    // ==================== Go Sample Projects ====================
    val sampleGoSubtitle: String get() = ProjectStrings.sampleGoSubtitle
    val sampleGoGinName: String get() = ProjectStrings.sampleGoGinName
    val sampleGoGinDesc: String get() = ProjectStrings.sampleGoGinDesc
    val sampleGoFiberName: String get() = ProjectStrings.sampleGoFiberName
    val sampleGoFiberDesc: String get() = ProjectStrings.sampleGoFiberDesc
    val sampleGoEchoName: String get() = ProjectStrings.sampleGoEchoName
    val sampleGoEchoDesc: String get() = ProjectStrings.sampleGoEchoDesc
    val sampleTagMiddleware: String get() = ProjectStrings.sampleTagMiddleware
    val sampleTagHighPerf: String get() = ProjectStrings.sampleTagHighPerf
    val sampleTagMinimalApi: String get() = ProjectStrings.sampleTagMinimalApi
    
    // ==================== Node.js ====================
    val sampleNodeSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速体验 Node.js 项目导入"
        AppLanguage.ENGLISH -> "Quick experience Node.js project import"
        AppLanguage.ARABIC -> "تجربة سريعة لاستيراد مشروع Node.js"
    }
    val sampleNodeExpressName: String get() = when (lang) {
        AppLanguage.CHINESE -> "待办事项应用"
        AppLanguage.ENGLISH -> "Todo App"
        AppLanguage.ARABIC -> "تطبيق المهام"
    }
    val sampleNodeExpressDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "完整的待办事项应用，支持添加/完成/删除任务，Material Design 风格 UI"
        AppLanguage.ENGLISH -> "Full-featured Todo app with add/complete/delete tasks, Material Design UI"
        AppLanguage.ARABIC -> "تطبيق مهام كامل مع إضافة/إكمال/حذف المهام، واجهة Material Design"
    }
    val sampleNodeFastifyName: String get() = when (lang) {
        AppLanguage.CHINESE -> "系统监控仪表盘"
        AppLanguage.ENGLISH -> "System Monitor Dashboard"
        AppLanguage.ARABIC -> "لوحة مراقبة النظام"
    }
    val sampleNodeFastifyDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "实时系统监控仪表盘，显示 CPU/内存/请求统计，深色主题"
        AppLanguage.ENGLISH -> "Real-time system monitor showing CPU/memory/request stats, dark theme"
        AppLanguage.ARABIC -> "مراقب نظام في الوقت الفعلي يعرض إحصائيات CPU/الذاكرة/الطلبات"
    }
    val sampleNodeKoaName: String get() = when (lang) {
        AppLanguage.CHINESE -> "Markdown 笔记"
        AppLanguage.ENGLISH -> "Markdown Notes"
        AppLanguage.ARABIC -> "ملاحظات Markdown"
    }
    val sampleNodeKoaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Markdown 笔记应用，支持创建/编辑/搜索笔记，实时预览"
        AppLanguage.ENGLISH -> "Markdown notes app with create/edit/search, live preview"
        AppLanguage.ARABIC -> "تطبيق ملاحظات Markdown مع إنشاء/تحرير/بحث، معاينة مباشرة"
    }
    
    // ==================== DocsSite Sample Projects ====================
    val sampleDocsSubtitle: String get() = ProjectStrings.sampleDocsSubtitle
    val sampleDocsVitepressName: String get() = ProjectStrings.sampleDocsVitepressName
    val sampleDocsVitepressDesc: String get() = ProjectStrings.sampleDocsVitepressDesc
    val sampleDocsMkdocsName: String get() = ProjectStrings.sampleDocsMkdocsName
    val sampleDocsMkdocsDesc: String get() = ProjectStrings.sampleDocsMkdocsDesc
    val sampleDocsHexoName: String get() = ProjectStrings.sampleDocsHexoName
    val sampleDocsHexoDesc: String get() = ProjectStrings.sampleDocsHexoDesc
    val sampleTagSearch: String get() = ProjectStrings.sampleTagSearch
    val sampleTagDarkMode: String get() = ProjectStrings.sampleTagDarkMode
    val sampleTagMarkdown: String get() = ProjectStrings.sampleTagMarkdown
    val sampleTagCategories: String get() = ProjectStrings.sampleTagCategories
    
    // ==================== WordPress Sample Projects ====================
    val sampleWpSubtitle: String get() = ProjectStrings.sampleWpSubtitle
    val sampleWpBlogName: String get() = ProjectStrings.sampleWpBlogName
    val sampleWpBlogDesc: String get() = ProjectStrings.sampleWpBlogDesc
    val sampleWpWooName: String get() = ProjectStrings.sampleWpWooName
    val sampleWpWooDesc: String get() = ProjectStrings.sampleWpWooDesc
    val sampleWpPortfolioName: String get() = ProjectStrings.sampleWpPortfolioName
    val sampleWpPortfolioDesc: String get() = ProjectStrings.sampleWpPortfolioDesc
    val sampleTagBlog: String get() = ProjectStrings.sampleTagBlog
    val sampleTagEcommerce: String get() = ProjectStrings.sampleTagEcommerce
    val sampleTagPortfolio: String get() = ProjectStrings.sampleTagPortfolio
    val sampleTagResponsive: String get() = ProjectStrings.sampleTagResponsive
    val sampleTagSqlite: String get() = ProjectStrings.sampleTagSqlite
    
    // ==================== Userscript / Chrome Extension Import ====================
    val importUserScript: String get() = ExtensionStrings.importUserScript
    val importUserScriptHint: String get() = ExtensionStrings.importUserScriptHint
    val importChromeExtension: String get() = ExtensionStrings.importChromeExtension
    val importChromeExtensionHint: String get() = ExtensionStrings.importChromeExtensionHint
    val installUserScript: String get() = ExtensionStrings.installUserScript
    val installChromeExtension: String get() = ExtensionStrings.installChromeExtension
    val install: String get() = ExtensionStrings.install
    val matchingSites: String get() = ExtensionStrings.matchingSites
    val requiredApis: String get() = ExtensionStrings.requiredApis
    val contentScripts: String get() = ExtensionStrings.contentScripts
    val unsupportedApis: String get() = ExtensionStrings.unsupportedApis
    
    // ==================== HTML Project Optimization（Linux ） ====================
    val optimizeCode: String get() = ProjectStrings.optimizeCode
    val optimizeCodeHint: String get() = ProjectStrings.optimizeCodeHint
    val optimizing: String get() = ProjectStrings.optimizing
    val optimizeComplete: String get() = ProjectStrings.optimizeComplete
    val optimizeResultJs: String get() = ProjectStrings.optimizeResultJs
    val optimizeResultCss: String get() = ProjectStrings.optimizeResultCss
    val optimizeResultTs: String get() = ProjectStrings.optimizeResultTs
    val optimizeResultSaved: String get() = ProjectStrings.optimizeResultSaved
    val optimizeFailed: String get() = ProjectStrings.optimizeFailed
    val optimizeEsbuildReady: String get() = ProjectStrings.optimizeEsbuildReady
    val optimizeFallback: String get() = ProjectStrings.optimizeFallback
    
    // ==================== Node.js TypeScript ====================
    val tsPreCompile: String get() = when (lang) {
        AppLanguage.CHINESE -> "TypeScript 预编译"
        AppLanguage.ENGLISH -> "TypeScript Pre-compilation"
        AppLanguage.ARABIC -> "تجميع TypeScript المسبق"
    }
    val tsPreCompileHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动前使用 esbuild 将 TypeScript 编译为 JavaScript，提升运行速度"
        AppLanguage.ENGLISH -> "Compile TypeScript to JavaScript with esbuild before launch for faster startup"
        AppLanguage.ARABIC -> "تجميع TypeScript إلى JavaScript باستخدام esbuild قبل التشغيل لبدء أسرع"
    }
    val tsBundleMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "依赖打包"
        AppLanguage.ENGLISH -> "Bundle Dependencies"
        AppLanguage.ARABIC -> "تجميع التبعيات"
    }
    val tsBundleModeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "将项目和 node_modules 打包为单文件，大幅减小 APK 体积"
        AppLanguage.ENGLISH -> "Bundle project and node_modules into a single file to reduce APK size"
        AppLanguage.ARABIC -> "تجميع المشروع و node_modules في ملف واحد لتقليل حجم APK"
    }
    
    // ==================== Performance Optimization (Performance Optimization) ====================
    val performanceOptimization: String get() = ShellStrings.performanceOptimization
    val performanceOptimizationDesc: String get() = ShellStrings.performanceOptimizationDesc
    val perfEnabled: String get() = ShellStrings.perfEnabled
    val perfDisabled: String get() = ShellStrings.perfDisabled
    // Note.
    val perfResourceOptimize: String get() = ShellStrings.perfResourceOptimize
    val perfCompressImages: String get() = ShellStrings.perfCompressImages
    val perfCompressImagesHint: String get() = ShellStrings.perfCompressImagesHint
    val perfMinifyCode: String get() = ShellStrings.perfMinifyCode
    val perfMinifyCodeHint: String get() = ShellStrings.perfMinifyCodeHint
    val perfConvertWebP: String get() = ShellStrings.perfConvertWebP
    val perfConvertWebPHint: String get() = ShellStrings.perfConvertWebPHint
    val perfRemoveUnused: String get() = ShellStrings.perfRemoveUnused
    val perfRemoveUnusedHint: String get() = ShellStrings.perfRemoveUnusedHint
    val perfImageQuality: String get() = ShellStrings.perfImageQuality
    // Note.
    val perfBuildOptimize: String get() = ShellStrings.perfBuildOptimize
    val perfParallelProcessing: String get() = ShellStrings.perfParallelProcessing
    val perfParallelProcessingHint: String get() = ShellStrings.perfParallelProcessingHint
    val perfEnableCache: String get() = ShellStrings.perfEnableCache
    val perfEnableCacheHint: String get() = ShellStrings.perfEnableCacheHint
    // Note.
    val perfLoadOptimize: String get() = ShellStrings.perfLoadOptimize
    val perfPreloadHints: String get() = ShellStrings.perfPreloadHints
    val perfPreloadHintsHint: String get() = ShellStrings.perfPreloadHintsHint
    val perfLazyLoading: String get() = ShellStrings.perfLazyLoading
    val perfLazyLoadingHint: String get() = ShellStrings.perfLazyLoadingHint
    val perfOptimizeScripts: String get() = ShellStrings.perfOptimizeScripts
    val perfOptimizeScriptsHint: String get() = ShellStrings.perfOptimizeScriptsHint
    // Note.
    val perfRuntimeOptimize: String get() = ShellStrings.perfRuntimeOptimize
    val perfRuntimeScript: String get() = ShellStrings.perfRuntimeScript
    val perfRuntimeScriptHint: String get() = ShellStrings.perfRuntimeScriptHint
    // Note.
    val perfStatsOriginalSize: String get() = ShellStrings.perfStatsOriginalSize
    val perfStatsOptimizedSize: String get() = ShellStrings.perfStatsOptimizedSize
    val perfStatsSaved: String get() = ShellStrings.perfStatsSaved
    val perfStatsImages: String get() = ShellStrings.perfStatsImages
    val perfStatsCode: String get() = ShellStrings.perfStatsCode
    val perfStatsTime: String get() = ShellStrings.perfStatsTime
    val perfOptimizing: String get() = ShellStrings.perfOptimizing
    val perfOptimizeComplete: String get() = ShellStrings.perfOptimizeComplete
    val featurePerfOptimize: String get() = ShellStrings.featurePerfOptimize
    
    // ==================== App Hardening (App Hardening) ====================
    val appHardening: String get() = CommonStrings.appHardening
    val appHardeningDesc: String get() = CommonStrings.appHardeningDesc
    val hardeningEnabled: String get() = ShellStrings.hardeningEnabled
    val hardeningDisabled: String get() = ShellStrings.hardeningDisabled
    val hardeningLevel: String get() = ShellStrings.hardeningLevel
    
    // Note.
    val hardeningLevelBasic: String get() = ShellStrings.hardeningLevelBasic
    val hardeningLevelStandard: String get() = ShellStrings.hardeningLevelStandard
    val hardeningLevelAdvanced: String get() = ShellStrings.hardeningLevelAdvanced
    val hardeningLevelFortress: String get() = ShellStrings.hardeningLevelFortress
    val hardeningLevelBasicDesc: String get() = ShellStrings.hardeningLevelBasicDesc
    val hardeningLevelStandardDesc: String get() = ShellStrings.hardeningLevelStandardDesc
    val hardeningLevelAdvancedDesc: String get() = ShellStrings.hardeningLevelAdvancedDesc
    val hardeningLevelFortressDesc: String get() = ShellStrings.hardeningLevelFortressDesc
    
    // DEX.
    val dexProtection: String get() = ShellStrings.dexProtection
    val dexEncryption: String get() = ShellStrings.dexEncryption
    val dexEncryptionHint: String get() = ShellStrings.dexEncryptionHint
    val dexSplitting: String get() = ShellStrings.dexSplitting
    val dexSplittingHint: String get() = ShellStrings.dexSplittingHint
    val dexVmp: String get() = ShellStrings.dexVmp
    val dexVmpHint: String get() = ShellStrings.dexVmpHint
    val dexControlFlow: String get() = ShellStrings.dexControlFlow
    val dexControlFlowHint: String get() = ShellStrings.dexControlFlowHint
    
    // Native SO.
    val soProtection: String get() = ShellStrings.soProtection
    val soEncryption: String get() = ShellStrings.soEncryption
    val soEncryptionHint: String get() = ShellStrings.soEncryptionHint
    val soElfObfuscation: String get() = ShellStrings.soElfObfuscation
    val soElfObfuscationHint: String get() = ShellStrings.soElfObfuscationHint
    val soSymbolStrip: String get() = ShellStrings.soSymbolStrip
    val soSymbolStripHint: String get() = ShellStrings.soSymbolStripHint
    val soAntiDump: String get() = ShellStrings.soAntiDump
    val soAntiDumpHint: String get() = ShellStrings.soAntiDumpHint
    
    // Note.
    val antiReverse: String get() = ShellStrings.antiReverse
    val antiDebugMultiLayer: String get() = ShellStrings.antiDebugMultiLayer
    val antiDebugMultiLayerHint: String get() = ShellStrings.antiDebugMultiLayerHint
    val antiFridaAdvanced: String get() = ShellStrings.antiFridaAdvanced
    val antiFridaAdvancedHint: String get() = ShellStrings.antiFridaAdvancedHint
    val antiXposedDeep: String get() = ShellStrings.antiXposedDeep
    val antiXposedDeepHint: String get() = ShellStrings.antiXposedDeepHint
    val antiMagiskDetect: String get() = ShellStrings.antiMagiskDetect
    val antiMagiskDetectHint: String get() = ShellStrings.antiMagiskDetectHint
    val antiMemoryDump: String get() = ShellStrings.antiMemoryDump
    val antiMemoryDumpHint: String get() = ShellStrings.antiMemoryDumpHint
    val antiScreenCapture: String get() = ShellStrings.antiScreenCapture
    val antiScreenCaptureHint: String get() = ShellStrings.antiScreenCaptureHint
    
    // Note.
    val environmentDetection: String get() = ShellStrings.environmentDetection
    val detectEmulatorAdvanced: String get() = ShellStrings.detectEmulatorAdvanced
    val detectEmulatorAdvancedHint: String get() = ShellStrings.detectEmulatorAdvancedHint
    val detectVirtualApp: String get() = ShellStrings.detectVirtualApp
    val detectVirtualAppHint: String get() = ShellStrings.detectVirtualAppHint
    val detectUSBDebugging: String get() = ShellStrings.detectUSBDebugging
    val detectUSBDebuggingHint: String get() = ShellStrings.detectUSBDebuggingHint
    val detectVPN: String get() = ShellStrings.detectVPN
    val detectVPNHint: String get() = ShellStrings.detectVPNHint
    val detectDeveloperOptions: String get() = ShellStrings.detectDeveloperOptions
    val detectDeveloperOptionsHint: String get() = ShellStrings.detectDeveloperOptionsHint
    
    // Note.
    val codeObfuscation: String get() = ShellStrings.codeObfuscation
    val stringEncryption: String get() = ShellStrings.stringEncryption
    val stringEncryptionHint: String get() = ShellStrings.stringEncryptionHint
    val classNameObfuscation: String get() = ShellStrings.classNameObfuscation
    val classNameObfuscationHint: String get() = ShellStrings.classNameObfuscationHint
    val callIndirection: String get() = ShellStrings.callIndirection
    val callIndirectionHint: String get() = ShellStrings.callIndirectionHint
    val opaquePredicates: String get() = ShellStrings.opaquePredicates
    val opaquePredicatesHint: String get() = ShellStrings.opaquePredicatesHint
    
    // RASP
    val raspProtection: String get() = ShellStrings.raspProtection
    val dexCrcVerify: String get() = ShellStrings.dexCrcVerify
    val dexCrcVerifyHint: String get() = ShellStrings.dexCrcVerifyHint
    val memoryIntegrity: String get() = ShellStrings.memoryIntegrity
    val memoryIntegrityHint: String get() = ShellStrings.memoryIntegrityHint
    val jniCallValidation: String get() = ShellStrings.jniCallValidation
    val jniCallValidationHint: String get() = ShellStrings.jniCallValidationHint
    val timingCheck: String get() = ShellStrings.timingCheck
    val timingCheckHint: String get() = ShellStrings.timingCheckHint
    val stackTraceFilter: String get() = ShellStrings.stackTraceFilter
    val stackTraceFilterHint: String get() = ShellStrings.stackTraceFilterHint
    
    // Note.
    val antiTamper: String get() = ShellStrings.antiTamper
    val multiPointSignature: String get() = ShellStrings.multiPointSignature
    val multiPointSignatureHint: String get() = ShellStrings.multiPointSignatureHint
    val apkChecksum: String get() = ShellStrings.apkChecksum
    val apkChecksumHint: String get() = ShellStrings.apkChecksumHint
    val resourceIntegrity: String get() = ShellStrings.resourceIntegrity
    val resourceIntegrityHint: String get() = ShellStrings.resourceIntegrityHint
    val certificatePinning: String get() = ShellStrings.certificatePinning
    val certificatePinningHint: String get() = ShellStrings.certificatePinningHint
    
    // Note.
    val threatResponse: String get() = ShellStrings.threatResponse
    val threatResponseLogOnly: String get() = ShellStrings.threatResponseLogOnly
    val threatResponseSilentExit: String get() = ShellStrings.threatResponseSilentExit
    val threatResponseCrashRandom: String get() = ShellStrings.threatResponseCrashRandom
    val threatResponseDataWipe: String get() = ShellStrings.threatResponseDataWipe
    val threatResponseFakeData: String get() = ShellStrings.threatResponseFakeData
    val responseDelay: String get() = ShellStrings.responseDelay
    val responseDelayHint: String get() = ShellStrings.responseDelayHint
    val enableHoneypot: String get() = ShellStrings.enableHoneypot
    val enableHoneypotHint: String get() = ShellStrings.enableHoneypotHint
    val enableSelfDestruct: String get() = ShellStrings.enableSelfDestruct
    val enableSelfDestructHint: String get() = ShellStrings.enableSelfDestructHint
    val hardeningProtectionLayers: String get() = ShellStrings.hardeningProtectionLayers
    
    // ==================== AI Coding ====================
    val aiCodingAssistant: String get() = AiStrings.aiCodingAssistant
    val aiCodingWelcome: String get() = AiStrings.aiCodingWelcome
    val selectCodingType: String get() = AiStrings.selectCodingType
    val codingTypeHtml: String get() = AiStrings.codingTypeHtml
    val codingTypeFrontend: String get() = AiStrings.codingTypeFrontend
    val codingTypeNodejs: String get() = AiStrings.codingTypeNodejs
    val codingTypeWordpress: String get() = AiStrings.codingTypeWordpress
    val codingTypePhp: String get() = AiStrings.codingTypePhp
    val codingTypePython: String get() = AiStrings.codingTypePython
    val codingTypeGo: String get() = AiStrings.codingTypeGo
    val codingTypeHtmlDesc: String get() = AiStrings.codingTypeHtmlDesc
    val codingTypeFrontendDesc: String get() = AiStrings.codingTypeFrontendDesc
    val codingTypeNodejsDesc: String get() = AiStrings.codingTypeNodejsDesc
    val codingTypeWordpressDesc: String get() = AiStrings.codingTypeWordpressDesc
    val codingTypePhpDesc: String get() = AiStrings.codingTypePhpDesc
    val codingTypePythonDesc: String get() = AiStrings.codingTypePythonDesc
    val codingTypeGoDesc: String get() = AiStrings.codingTypeGoDesc
    val directoryTree: String get() = AiStrings.directoryTree
    val editCode: String get() = AiStrings.editCode
    val saveFile: String get() = AiStrings.saveFile
    val fileSaved: String get() = AiStrings.fileSaved
    val exportToProject: String get() = AiStrings.exportToProject
    val previewNotSupported: String get() = AiStrings.previewNotSupported
    val noFiles: String get() = AiStrings.noFiles
    val exportAllFiles: String get() = AiStrings.exportAllFiles
    val downloadAllFiles: String get() = AiStrings.downloadAllFiles
    val allFilter: String get() = AiStrings.allFilter
    val tryAskingPrompts: String get() = AiStrings.tryAskingPrompts
    
    // ---- AI Coding: write_file tool descriptions (per type) ----
    val writeToolDescHtml: String get() = AiStrings.writeToolDescHtml
    val writeToolDescFrontend: String get() = AiStrings.writeToolDescFrontend
    val writeToolDescNodejs: String get() = AiStrings.writeToolDescNodejs
    val writeToolDescWordpress: String get() = AiStrings.writeToolDescWordpress
    val writeToolDescPhp: String get() = AiStrings.writeToolDescPhp
    val writeToolDescPython: String get() = AiStrings.writeToolDescPython
    val writeToolDescGo: String get() = AiStrings.writeToolDescGo
    
    // ---- AI Coding: example prompts per type (2 per type) ----
    val aiPromptHtml1: String get() = AiStrings.aiPromptHtml1
    val aiPromptHtml2: String get() = AiStrings.aiPromptHtml2
    val aiPromptFrontend1: String get() = AiStrings.aiPromptFrontend1
    val aiPromptFrontend2: String get() = AiStrings.aiPromptFrontend2
    val aiPromptNodejs1: String get() = AiStrings.aiPromptNodejs1
    val aiPromptNodejs2: String get() = AiStrings.aiPromptNodejs2
    val aiPromptWordpress1: String get() = AiStrings.aiPromptWordpress1
    val aiPromptWordpress2: String get() = AiStrings.aiPromptWordpress2
    val aiPromptPhp1: String get() = AiStrings.aiPromptPhp1
    val aiPromptPhp2: String get() = AiStrings.aiPromptPhp2
    val aiPromptPython1: String get() = AiStrings.aiPromptPython1
    val aiPromptPython2: String get() = AiStrings.aiPromptPython2
    val aiPromptGo1: String get() = AiStrings.aiPromptGo1
    val aiPromptGo2: String get() = AiStrings.aiPromptGo2
    // ---- Style template promptHints (i18n) ----
    val hintModernMinimal: String get() = AiStrings.hintModernMinimal
    val hintGlassmorphism: String get() = AiStrings.hintGlassmorphism
    val hintNeumorphism: String get() = AiStrings.hintNeumorphism
    val hintDarkMode: String get() = AiStrings.hintDarkMode
    val hintCyberpunk: String get() = AiStrings.hintCyberpunk
    val hintGradient: String get() = AiStrings.hintGradient
    val hintMinimal: String get() = AiStrings.hintMinimal
    val hintNature: String get() = AiStrings.hintNature
    val hintCuteCartoon: String get() = AiStrings.hintCuteCartoon
    val hintNeonGlow: String get() = AiStrings.hintNeonGlow
    
    // ---- Style reference colorHints (i18n) ----
    val colorsHarryPotter: String get() = AiStrings.colorsHarryPotter
    val colorsGhibli: String get() = AiStrings.colorsGhibli
    val colorsYourName: String get() = AiStrings.colorsYourName
    val colorsApple: String get() = AiStrings.colorsApple
    val colorsLittlePrince: String get() = AiStrings.colorsLittlePrince
    val colorsZelda: String get() = AiStrings.colorsZelda
    val colorsArtDeco: String get() = AiStrings.colorsArtDeco
    val colorsJapanese: String get() = AiStrings.colorsJapanese
    
    // ---- Style reference elementHints (i18n) ----
    val elementsHarryPotter: String get() = AiStrings.elementsHarryPotter
    val elementsGhibli: String get() = AiStrings.elementsGhibli
    val elementsYourName: String get() = AiStrings.elementsYourName
    val elementsApple: String get() = AiStrings.elementsApple
    val elementsLittlePrince: String get() = AiStrings.elementsLittlePrince
    val elementsZelda: String get() = AiStrings.elementsZelda
    val elementsArtDeco: String get() = AiStrings.elementsArtDeco
    val elementsJapanese: String get() = AiStrings.elementsJapanese
    
    val requestTimeoutRetry: String get() = AiStrings.requestTimeoutRetry
    val errorOccurredPrefix: String get() = AiStrings.errorOccurredPrefix
    val streamResponseIncomplete: String get() = AiStrings.streamResponseIncomplete
    
    // ==================== Hardcoded Chinese fix - additional i18n entries ====================
    
    val adSdkNotIntegrated: String get() = CompatStrings.adSdkNotIntegrated
    
    val storagePermissionRequiredForExport: String get() = CompatStrings.storagePermissionRequiredForExport
    
    val shareImage: String get() = CompatStrings.shareImage
    
    val saveFailedCannotProcessHtml: String get() = CompatStrings.saveFailedCannotProcessHtml
    
    val phpStartFailed: String get() = CompatStrings.phpStartFailed

    // ---- Shell Server Modes ----

    val nodeRuntimeNotFound: String get() = CompatStrings.nodeRuntimeNotFound

    val nodeServerStartFailed: String get() = CompatStrings.nodeServerStartFailed

    val nodeStartFailed: String get() = CompatStrings.nodeStartFailed

    val preparingNodeEnv: String get() = CompatStrings.preparingNodeEnv

    val startingNodeServer: String get() = CompatStrings.startingNodeServer

    val pythonRuntimeNotFound: String get() = CompatStrings.pythonRuntimeNotFound

    val pythonServerStartFailed: String get() = CompatStrings.pythonServerStartFailed

    val pythonStartFailed: String get() = CompatStrings.pythonStartFailed

    val pythonServerTimeout: String get() = CompatStrings.pythonServerTimeout

    val preparingPythonEnv: String get() = CompatStrings.preparingPythonEnv

    val startingPythonServer: String get() = CompatStrings.startingPythonServer

    val goBinaryNotFound: String get() = CompatStrings.goBinaryNotFound

    val goServerStartFailed: String get() = CompatStrings.goServerStartFailed

    val goStartFailed: String get() = CompatStrings.goStartFailed

    val preparingGoEnv: String get() = CompatStrings.preparingGoEnv

    val startingGoServer: String get() = CompatStrings.startingGoServer

    val wpStartFailed: String get() = CompatStrings.wpStartFailed

    // ---- Background run service ----
    
    val appRunningInBackground: String get() = CommonStrings.appRunningInBackground
    
    val tapToReturnToApp: String get() = CompatStrings.tapToReturnToApp
    
    // ---- Dependency download notifications ----
    
    val runtimeDownloadChannel: String get() = CompatStrings.runtimeDownloadChannel
    
    val runtimeDownloadChannelDesc: String get() = CompatStrings.runtimeDownloadChannelDesc
    
    val depDownloadRemaining: String get() = CompatStrings.depDownloadRemaining
    
    val depDownloadStarted: String get() = CompatStrings.depDownloadStarted
    
    val depDownloadPause: String get() = CompatStrings.depDownloadPause
    
    val depDownloadResume: String get() = CompatStrings.depDownloadResume
    
    val depDownloadPaused: String get() = CompatStrings.depDownloadPaused
    
    val depDownloadExtracting: String get() = CompatStrings.depDownloadExtracting
    
    val depDownloadComplete: String get() = CompatStrings.depDownloadComplete
    
    val depDownloadAllReady: String get() = CompatStrings.depDownloadAllReady
    
    // ---- Forced run countdown overlay ----
    
    val tapToEnterPasswordToExit: String get() = CompatStrings.tapToEnterPasswordToExit
    
    val enterExitPassword: String get() = CompatStrings.enterExitPassword
    
    val enterAdminPasswordToExit: String get() = CompatStrings.enterAdminPasswordToExit
    
    val passwordLabel: String get() = CompatStrings.passwordLabel
    
    val enterPasswordPlaceholder: String get() = CompatStrings.enterPasswordPlaceholder
    
    val wrongPasswordAttemptsRemaining: String get() = CompatStrings.wrongPasswordAttemptsRemaining
    
    val tooManyAttemptsTryLater: String get() = CompatStrings.tooManyAttemptsTryLater
    
    val confirmExit: String get() = CompatStrings.confirmExit
    
    // ---- Forced run permission helper ----
    
    val forcedRunPermissionTitle: String get() = CompatStrings.forcedRunPermissionTitle
    
    val forcedRunPermissionDesc: String get() = CompatStrings.forcedRunPermissionDesc
    
    val accessibilityService: String get() = CompatStrings.accessibilityService
    
    val accessibilityServiceDesc: String get() = CompatStrings.accessibilityServiceDesc
    
    val usageAccess: String get() = CompatStrings.usageAccess
    
    val usageAccessDesc: String get() = CompatStrings.usageAccessDesc
    
    val refreshPermissionStatus: String get() = CompatStrings.refreshPermissionStatus
    
    val grant: String get() = CompatStrings.grant
    
    val granted: String get() = CompatStrings.granted
    
    val protectionBasic: String get() = CompatStrings.protectionBasic
    
    val protectionBasicDesc: String get() = CompatStrings.protectionBasicDesc
    
    val protectionStandard: String get() = CompatStrings.protectionStandard
    
    val protectionStandardDesc: String get() = CompatStrings.protectionStandardDesc
    
    val protectionMaximum: String get() = CompatStrings.protectionMaximum
    
    val protectionMaximumDesc: String get() = CompatStrings.protectionMaximumDesc
    
    val permissionsReady: String get() = CompatStrings.permissionsReady
    
    val permissionsNeeded: String get() = CompatStrings.permissionsNeeded
    
    val start: String get() = CompatStrings.start
    
    val skipDegradedProtection: String get() = CompatStrings.skipDegradedProtection
    
    // ---- HTML project processor warnings ----
    
    val htmlFileTooLarge: String get() = CompatStrings.htmlFileTooLarge
    
    val resourceReferenceIssue: String get() = CompatStrings.resourceReferenceIssue
    
    val htmlFileNotFound: String get() = CompatStrings.htmlFileNotFound
    
    val cssEncodingWarning: String get() = CompatStrings.cssEncodingWarning
    
    val documentWriteWarning: String get() = CompatStrings.documentWriteWarning
    
    // ---- HTML syntax check messages (AiCodingAgent) ----
    
    val tagNotProperlyClosed: String get() = CompatStrings.tagNotProperlyClosed
    
    val unexpectedClosingTag: String get() = CompatStrings.unexpectedClosingTag
    
    val tagNotClosed: String get() = CompatStrings.tagNotClosed
    
    val extraClosingBrace: String get() = CompatStrings.extraClosingBrace
    
    val missingClosingBraces: String get() = CompatStrings.missingClosingBraces
    
    val extraClosingBraces: String get() = CompatStrings.extraClosingBraces
    
    val missingClosingParens: String get() = CompatStrings.missingClosingParens
    
    val extraClosingParens: String get() = CompatStrings.extraClosingParens
    
    val missingClosingBrackets: String get() = CompatStrings.missingClosingBrackets
    
    val extraClosingBrackets: String get() = CompatStrings.extraClosingBrackets
    
    // ---- Error page games ----
    
    val gameScore: String get() = CompatStrings.gameScore
    
    val gameLives: String get() = CompatStrings.gameLives
    
    val gameOver: String get() = CompatStrings.gameOver
    
    val gameYouWin: String get() = CompatStrings.gameYouWin
    
    val gameTapToRestart: String get() = CompatStrings.gameTapToRestart
    
    val gameMazeComplete: String get() = CompatStrings.gameMazeComplete
    
    val gameSteps: String get() = CompatStrings.gameSteps
    
    val gameCollected: String get() = CompatStrings.gameCollected
    
    val gameCollectedStars: String get() = CompatStrings.gameCollectedStars
    
    val gameTouchToPaint: String get() = CompatStrings.gameTouchToPaint
    
    val gameZen: String get() = CompatStrings.gameZen
    
    // ---- Download engine errors ----
    
    val downloadFailedHttp: String get() = CompatStrings.downloadFailedHttp
    
    val downloadReturnedEmpty: String get() = CompatStrings.downloadReturnedEmpty
    
    val downloadNameFailed: String get() = CompatStrings.downloadNameFailed
    
    val sizeUnknown: String get() = CompatStrings.sizeUnknown
    
    // ---- MainViewModel ----
    
    val saveFailedNoHtmlInZip: String get() = CompatStrings.saveFailedNoHtmlInZip
    
    // ---- HtmlProjectProcessor suggestions ----
    
    val suggestUseRelativePath: String get() = CompatStrings.suggestUseRelativePath
    
    val suggestEnsureFileImported: String get() = CompatStrings.suggestEnsureFileImported
    
    val suggestSaveAsUtf8: String get() = CompatStrings.suggestSaveAsUtf8
    
    val suggestExternalFilesDetected: String get() = CompatStrings.suggestExternalFilesDetected
    
    val suggestUseRelativePathsForAll: String get() = CompatStrings.suggestUseRelativePathsForAll
    
    val absolutePathWarning: String get() = CompatStrings.absolutePathWarning
    
    val referencedFileNotExist: String get() = CompatStrings.referencedFileNotExist
    
    val suggestUseDomMethods: String get() = CompatStrings.suggestUseDomMethods
    
    val possiblyUnclosedBraces: String get() = CompatStrings.possiblyUnclosedBraces
    
    val suggestCheckBracesPaired: String get() = CompatStrings.suggestCheckBracesPaired
    
    // ---- DownloadNotificationManager ----
    
    val notifDownloadChannel: String get() = CompatStrings.notifDownloadChannel
    
    val notifDownloadChannelDesc: String get() = CompatStrings.notifDownloadChannelDesc
    
    val notifDownloadComplete: String get() = CompatStrings.notifDownloadComplete
    
    val notifDownloadFailed: String get() = CompatStrings.notifDownloadFailed
    
    val notifSaving: String get() = CompatStrings.notifSaving
    
    val notifDownloading: String get() = CompatStrings.notifDownloading
    
    val notifSaveCompleted: String get() = CompatStrings.notifSaveCompleted
    
    val notifSavedToGallery: String get() = CompatStrings.notifSavedToGallery
    
    val notifSaveFailed: String get() = CompatStrings.notifSaveFailed
    
    val notifView: String get() = CompatStrings.notifView
    
    val notifShare: String get() = CompatStrings.notifShare
    
    val notifOpen: String get() = CompatStrings.notifOpen
    
    val notifShareType: String get() = CompatStrings.notifShareType
    
    val mediaTypeImage: String get() = CompatStrings.mediaTypeImage
    
    val mediaTypeVideo: String get() = CompatStrings.mediaTypeVideo
    
    // ---- MainViewModel ----
    
    val failedSaveIcon: String get() = CompatStrings.failedSaveIcon
    
    val failedSaveSplash: String get() = CompatStrings.failedSaveSplash
    
    val appSavedSuccessfully: String get() = CommonStrings.appSavedSuccessfully
    
    val appDeleted: String get() = CommonStrings.appDeleted
    
    val deleteFailed: String get() = CompatStrings.deleteFailed
    
    val pleaseEnterAppName: String get() = CompatStrings.pleaseEnterAppName
    
    val pleaseEnterWebsiteUrl: String get() = CompatStrings.pleaseEnterWebsiteUrl
    
    val pleaseEnterValidUrl: String get() = CompatStrings.pleaseEnterValidUrl
    
    val insecureHttpWarning: String get() = CompatStrings.insecureHttpWarning

    val allowHttpCheckbox: String get() = CompatStrings.allowHttpCheckbox
    
    val pleaseSelectHtmlFile: String get() = CompatStrings.pleaseSelectHtmlFile
    
    val mediaFilePathEmpty: String get() = CompatStrings.mediaFilePathEmpty
    
    val failedSaveMediaFile: String get() = CompatStrings.failedSaveMediaFile
    
    val appCreatedSuccessfully: String get() = CommonStrings.appCreatedSuccessfully
    
    val creationFailed: String get() = CompatStrings.creationFailed
    
    val pleaseAddMediaFile: String get() = CompatStrings.pleaseAddMediaFile
    
    val appUpdatedSuccessfully: String get() = CommonStrings.appUpdatedSuccessfully
    
    val updateFailed: String get() = CompatStrings.updateFailed
    
    val failedCreateCategory: String get() = CompatStrings.failedCreateCategory
    
    val failedUpdateCategory: String get() = CompatStrings.failedUpdateCategory
    
    val failedDeleteCategory: String get() = CompatStrings.failedDeleteCategory
    
    val moveFailed: String get() = CompatStrings.moveFailed
    
    // ---- Accessibility contentDescription ----
    
    val cdBack: String get() = CompatStrings.cdBack
    
    val cdForward: String get() = CompatStrings.cdForward
    
    val cdRefresh: String get() = CompatStrings.cdRefresh
    
    val cdHome: String get() = CompatStrings.cdHome
    
    val cdPause: String get() = CompatStrings.cdPause
    
    val cdPlay: String get() = CompatStrings.cdPlay
    
    val cdPrevious: String get() = CompatStrings.cdPrevious
    
    val cdNext: String get() = CompatStrings.cdNext
    
    val cdSeekBack: String get() = CompatStrings.cdSeekBack
    
    val cdSeekForward: String get() = CompatStrings.cdSeekForward
    
    val cdSplashScreen: String get() = CompatStrings.cdSplashScreen
    
    val cdMediaContent: String get() = CompatStrings.cdMediaContent
    
    val cdCover: String get() = CompatStrings.cdCover
    
    val cdPreview: String get() = CompatStrings.cdPreview
    
    val cdCopy: String get() = CompatStrings.cdCopy
    
    val cdMore: String get() = CompatStrings.cdMore
    
    val cdClose: String get() = CompatStrings.cdClose
    
    val cdRemove: String get() = CompatStrings.cdRemove
    
    val cdRetry: String get() = CompatStrings.cdRetry
    
    val cdCollapse: String get() = CompatStrings.cdCollapse
    
    val cdExpand: String get() = CompatStrings.cdExpand
    
    val hideRawResponse: String get() = CompatStrings.hideRawResponse
    
    val viewRawResponse: String get() = CompatStrings.viewRawResponse
    
    // ---- BrowserKernelScreen Shields ----
    
    val shieldsPrivacyProtection: String get() = CompatStrings.shieldsPrivacyProtection
    
    val shieldsPrivacySubtitle: String get() = CompatStrings.shieldsPrivacySubtitle
    
    val shieldsMasterSwitch: String get() = CompatStrings.shieldsMasterSwitch
    
    val shieldsEnabledWithRules: String get() = CompatStrings.shieldsEnabledWithRules
    
    val shieldsDisabled: String get() = CompatStrings.shieldsDisabled
    
    val shieldsStatAds: String get() = CompatStrings.shieldsStatAds
    
    val shieldsStatTrackers: String get() = CompatStrings.shieldsStatTrackers
    
    val shieldsCollapseSettings: String get() = CompatStrings.shieldsCollapseSettings
    
    val shieldsExpandSettings: String get() = CompatStrings.shieldsExpandSettings
    
    val shieldsHttpsUpgradeDesc: String get() = CompatStrings.shieldsHttpsUpgradeDesc

    // SSL.
    val sslErrorPolicyTitle: String get() = CompatStrings.sslErrorPolicyTitle

    val sslErrorPolicyAutoFallback: String get() = CompatStrings.sslErrorPolicyAutoFallback

    val sslErrorPolicyAutoFallbackDesc: String get() = CompatStrings.sslErrorPolicyAutoFallbackDesc

    val sslErrorPolicyAskUser: String get() = CompatStrings.sslErrorPolicyAskUser

    val sslErrorPolicyAskUserDesc: String get() = CompatStrings.sslErrorPolicyAskUserDesc

    val sslErrorPolicyBlock: String get() = CompatStrings.sslErrorPolicyBlock

    val sslErrorPolicyBlockDesc: String get() = CompatStrings.sslErrorPolicyBlockDesc

    val shieldsTrackerBlocking: String get() = CompatStrings.shieldsTrackerBlocking
    
    val shieldsTrackerBlockingDesc: String get() = CompatStrings.shieldsTrackerBlockingDesc
    
    val shieldsCookiePopup: String get() = CompatStrings.shieldsCookiePopup
    
    val shieldsCookiePopupDesc: String get() = CompatStrings.shieldsCookiePopupDesc
    
    val shieldsGpcDesc: String get() = CompatStrings.shieldsGpcDesc
    
    val shieldsReaderMode: String get() = CompatStrings.shieldsReaderMode
    
    val shieldsReaderModeDesc: String get() = CompatStrings.shieldsReaderModeDesc
    
    val shieldsThirdPartyCookiePolicy: String get() = CompatStrings.shieldsThirdPartyCookiePolicy
    
    val shieldsReferrerPolicy: String get() = CompatStrings.shieldsReferrerPolicy
    
    val verifyingFile: String get() = CompatStrings.verifyingFile
    
    // ==================== PWA (PWA Offline Support) ====================
    val pwaOfflineTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "PWA 离线支持"
        AppLanguage.ENGLISH -> "PWA Offline Support"
        AppLanguage.ARABIC -> "دعم PWA بدون اتصال"
    }
    val pwaOfflineSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "注入 Service Worker 缓存层，支持离线浏览已访问页面"
        AppLanguage.ENGLISH -> "Inject Service Worker cache layer for offline browsing of visited pages"
        AppLanguage.ARABIC -> "حقن طبقة تخزين Service Worker للتصفح بدون اتصال"
    }
    val pwaOfflineStrategyLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "缓存策略"
        AppLanguage.ENGLISH -> "Cache Strategy"
        AppLanguage.ARABIC -> "استراتيجية التخزين المؤقت"
    }
    val pwaStrategyNetworkFirst: String get() = when (lang) {
        AppLanguage.CHINESE -> "网络优先（推荐）— 优先使用最新内容，离线时用缓存"
        AppLanguage.ENGLISH -> "Network First (Recommended) — Use latest content, fallback to cache offline"
        AppLanguage.ARABIC -> "الشبكة أولاً (موصى به) — استخدام أحدث المحتويات"
    }
    val pwaStrategyCacheFirst: String get() = when (lang) {
        AppLanguage.CHINESE -> "缓存优先 — 最快速度加载，内容可能过时"
        AppLanguage.ENGLISH -> "Cache First — Fastest loading, content may be stale"
        AppLanguage.ARABIC -> "التخزين المؤقت أولاً — أسرع تحميل، قد يكون المحتوى قديماً"
    }
    val pwaStrategyStaleWhileRevalidate: String get() = when (lang) {
        AppLanguage.CHINESE -> "先缓存后更新 — 立即显示缓存，后台静默更新"
        AppLanguage.ENGLISH -> "Stale While Revalidate — Show cache instantly, update in background"
        AppLanguage.ARABIC -> "عرض التخزين المؤقت فوراً مع التحديث في الخلفية"
    }
    
    // ==================== Online Music Search ( ) ====================
    val playbackFailed: String get() = MusicStrings.playbackFailed
    val playbackFailedWithCode: String get() = MusicStrings.playbackFailedWithCode
    val loadingTimeout: String get() = MusicStrings.loadingTimeout

    val musicChannelLabel: String get() = MusicStrings.musicChannelLabel
    val gettingMusicDetails: String get() = MusicStrings.gettingMusicDetails
    val getPlayUrlFailed: String get() = MusicStrings.getPlayUrlFailed
    val getPlayUrlSuccess: String get() = MusicStrings.getPlayUrlSuccess
    val startDownloadMusic: String get() = MusicStrings.startDownloadMusic
    val musicDownloading: String get() = MusicStrings.musicDownloading
    val downloadingCoverImage: String get() = MusicStrings.downloadingCoverImage
    val coverDownloading: String get() = MusicStrings.coverDownloading
    val finishing: String get() = MusicStrings.finishing
    val downloadCompleteSaved: String get() = MusicStrings.downloadCompleteSaved
    val coverImageSaved: String get() = MusicStrings.coverImageSaved
    val downloadError: String get() = MusicStrings.downloadError
    val downloadLog: String get() = MusicStrings.downloadLog
    val searchingText: String get() = MusicStrings.searchingText
    val clearText: String get() = MusicStrings.clearText
    val collapseText: String get() = MusicStrings.collapseText
    
    // ==================== Shields Enum i18n ====================
    
    // --- Cookie ---.
    val shieldsCookieAllowAll: String get() = ShellStrings.shieldsCookieAllowAll
    val shieldsCookieBlockCrossSite: String get() = ShellStrings.shieldsCookieBlockCrossSite
    val shieldsCookieBlockAllThirdParty: String get() = ShellStrings.shieldsCookieBlockAllThirdParty
    
    // --- Referrer ---.
    val shieldsRefNoReferrer: String get() = ShellStrings.shieldsRefNoReferrer
    val shieldsRefOrigin: String get() = ShellStrings.shieldsRefOrigin
    val shieldsRefStrictOriginCross: String get() = ShellStrings.shieldsRefStrictOriginCross
    val shieldsRefSameOrigin: String get() = ShellStrings.shieldsRefSameOrigin
    val shieldsRefUnsafeUrl: String get() = ShellStrings.shieldsRefUnsafeUrl
    
    // Note.
    val shieldsTrackerAnalytics: String get() = ShellStrings.shieldsTrackerAnalytics
    val shieldsTrackerSocial: String get() = ShellStrings.shieldsTrackerSocial
    val shieldsTrackerFingerprinting: String get() = ShellStrings.shieldsTrackerFingerprinting
    val shieldsTrackerCryptomining: String get() = ShellStrings.shieldsTrackerCryptomining
    val shieldsTrackerAdNetwork: String get() = ShellStrings.shieldsTrackerAdNetwork
    
    // ==================== PWA Auto Detection (PWA Auto-Detection) ====================
    
    val pwaAnalyzeButton: String get() = ProjectStrings.pwaAnalyzeButton
    val pwaAnalyzing: String get() = ProjectStrings.pwaAnalyzing
    val pwaDetected: String get() = ProjectStrings.pwaDetected
    val pwaNoneDetected: String get() = ProjectStrings.pwaNoneDetected
    val pwaApplyAll: String get() = ProjectStrings.pwaApplyAll
    val pwaApplied: String get() = ProjectStrings.pwaApplied
    val pwaSourceManifest: String get() = ProjectStrings.pwaSourceManifest
    val pwaSourceMeta: String get() = ProjectStrings.pwaSourceMeta
    val pwaName: String get() = ProjectStrings.pwaName
    val pwaIcon: String get() = ProjectStrings.pwaIcon
    val pwaThemeColor: String get() = ProjectStrings.pwaThemeColor
    val pwaDisplayMode: String get() = ProjectStrings.pwaDisplayMode
    val pwaOrientation: String get() = ProjectStrings.pwaOrientation
    val pwaStartUrl: String get() = ProjectStrings.pwaStartUrl
    val pwaAnalysisFailed: String get() = ProjectStrings.pwaAnalysisFailed
    val pwaIconDownloaded: String get() = ProjectStrings.pwaIconDownloaded
    val pwaIconDownloadFailed: String get() = ProjectStrings.pwaIconDownloadFailed
    
    // ==================== OTA (In-App Update) ====================
    
    val otaNewVersionFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "发现新版本 v%s"
        AppLanguage.ENGLISH -> "New version v%s available"
        AppLanguage.ARABIC -> "يتوفر إصدار جديد v%s"
    }
    val otaUpdateNow: String get() = when (lang) {
        AppLanguage.CHINESE -> "立即更新"
        AppLanguage.ENGLISH -> "Update Now"
        AppLanguage.ARABIC -> "التحديث الآن"
    }
    val otaLater: String get() = when (lang) {
        AppLanguage.CHINESE -> "稍后"
        AppLanguage.ENGLISH -> "Later"
        AppLanguage.ARABIC -> "لاحقاً"
    }
    val otaDownloading: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在下载更新..."
        AppLanguage.ENGLISH -> "Downloading update..."
        AppLanguage.ARABIC -> "جاري تنزيل التحديث..."
    }
    val otaDownloadComplete: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载完成，点击安装"
        AppLanguage.ENGLISH -> "Download complete, tap to install"
        AppLanguage.ARABIC -> "اكتمل التنزيل، انقر للتثبيت"
    }
    val otaDownloadFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载失败，请重试"
        AppLanguage.ENGLISH -> "Download failed, please retry"
        AppLanguage.ARABIC -> "فشل التنزيل، يرجى المحاولة مرة أخرى"
    }
    val otaInstalling: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在安装..."
        AppLanguage.ENGLISH -> "Installing..."
        AppLanguage.ARABIC -> "جاري التثبيت..."
    }
    val otaUpdateChannel: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用更新"
        AppLanguage.ENGLISH -> "App Update"
        AppLanguage.ARABIC -> "تحديث التطبيق"
    }
    val otaUpdateChannelDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示应用更新下载进度和安装通知"
        AppLanguage.ENGLISH -> "Show app update download progress and installation notifications"
        AppLanguage.ARABIC -> "عرض تقدم تنزيل التحديث وإشعارات التثبيت"
    }

    // ==================== Subscription and Billing ====================

    val selectPlan: String get() = BillingStrings.selectPlan

    val unlockAllFeatures: String get() = BillingStrings.unlockAllFeatures

    val chooseYourPlan: String get() = BillingStrings.chooseYourPlan

    val periodMonthly: String get() = BillingStrings.periodMonthly

    val periodQuarterly: String get() = BillingStrings.periodQuarterly

    val periodYearly: String get() = BillingStrings.periodYearly

    val periodLifetime: String get() = BillingStrings.periodLifetime

    val currentPlan: String get() = BillingStrings.currentPlan

    val validForever: String get() = BillingStrings.validForever

    val perMonth: String get() = BillingStrings.perMonth

    val perQuarter: String get() = BillingStrings.perQuarter

    val perYear: String get() = BillingStrings.perYear

    val oneTime: String get() = BillingStrings.oneTime

    val neverExpires: String get() = BillingStrings.neverExpires

    val oneTimePurchaseLifetime: String get() = BillingStrings.oneTimePurchaseLifetime

    val redeemWithActivationCode: String get() = BillingStrings.redeemWithActivationCode

    val restorePurchase: String get() = BillingStrings.restorePurchase

    val subscriptionDisclaimer: String get() = BillingStrings.subscriptionDisclaimer

    val recommended: String get() = BillingStrings.recommended

    val currentScheme: String get() = BillingStrings.currentScheme

    val basicPlan: String get() = BillingStrings.basicPlan

    val hasHigherPlan: String get() = BillingStrings.hasHigherPlan

    val subscribeTierName: String get() = BillingStrings.subscribeTierName

    // Pro.
    val proCloudProjects: String get() = BillingStrings.proCloudProjects

    val proCloudProjectsDesc: String get() = BillingStrings.proCloudProjectsDesc

    val proActivationCodeSystem: String get() = BillingStrings.proActivationCodeSystem

    val proActivationCodeSystemDesc: String get() = BillingStrings.proActivationCodeSystemDesc

    val proAutoUpdates: String get() = BillingStrings.proAutoUpdates

    val proAutoUpdatesDesc: String get() = BillingStrings.proAutoUpdatesDesc

    val proAnnouncements: String get() = BillingStrings.proAnnouncements

    val proAnnouncementsDesc: String get() = BillingStrings.proAnnouncementsDesc

    val proRemoteConfig: String get() = BillingStrings.proRemoteConfig

    val proRemoteConfigDesc: String get() = BillingStrings.proRemoteConfigDesc

    val proWebhook: String get() = BillingStrings.proWebhook

    val proWebhookDesc: String get() = BillingStrings.proWebhookDesc

    val proAnalytics: String get() = BillingStrings.proAnalytics

    val proAnalyticsDesc: String get() = BillingStrings.proAnalyticsDesc

    // Ultra.
    val ultraIncludesAllPro: String get() = BillingStrings.ultraIncludesAllPro

    val ultraFcmPush: String get() = BillingStrings.ultraFcmPush

    val ultraFcmPushDesc: String get() = BillingStrings.ultraFcmPushDesc

    val ultraActivationCodeLimit: String get() = BillingStrings.ultraActivationCodeLimit

    val ultraActivationCodeLimitDesc: String get() = BillingStrings.ultraActivationCodeLimitDesc

    val ultraAnnouncementLimit: String get() = BillingStrings.ultraAnnouncementLimit

    val ultraAnnouncementLimitDesc: String get() = BillingStrings.ultraAnnouncementLimitDesc

    val ultraProjectLimit: String get() = BillingStrings.ultraProjectLimit

    val ultraProjectLimitDesc: String get() = BillingStrings.ultraProjectLimitDesc

    val ultraR2Storage: String get() = BillingStrings.ultraR2Storage

    val ultraR2StorageDesc: String get() = BillingStrings.ultraR2StorageDesc

    val ultraPrioritySupport: String get() = BillingStrings.ultraPrioritySupport

    val ultraPrioritySupportDesc: String get() = BillingStrings.ultraPrioritySupportDesc

    // Free.
    val freeUnlimitedApps: String get() = BillingStrings.freeUnlimitedApps

    val freeUnlimitedAppsDesc: String get() = BillingStrings.freeUnlimitedAppsDesc

    val freeLocalBuild: String get() = BillingStrings.freeLocalBuild

    val freeLocalBuildDesc: String get() = BillingStrings.freeLocalBuildDesc

    val freeExtensionSystem: String get() = BillingStrings.freeExtensionSystem

    val freeExtensionSystemDesc: String get() = BillingStrings.freeExtensionSystemDesc

    val freeMarketplace: String get() = BillingStrings.freeMarketplace

    val freeMarketplaceDesc: String get() = BillingStrings.freeMarketplaceDesc

    val freeAiAssistant: String get() = BillingStrings.freeAiAssistant

    val freeAiAssistantDesc: String get() = BillingStrings.freeAiAssistantDesc

    val freeForever: String get() = BillingStrings.freeForever

    val proUpgradeNote: String get() = BillingStrings.proUpgradeNote

    val subscriptionSuccess: String get() = BillingStrings.subscriptionSuccess

    val tierPro: String get() = BillingStrings.tierPro

    val tierUltra: String get() = BillingStrings.tierUltra

    val tierFree: String get() = BillingStrings.tierFree

    val proMonthly: String get() = BillingStrings.proMonthly

    val proQuarterly: String get() = BillingStrings.proQuarterly

    val proYearly: String get() = BillingStrings.proYearly

    val proLifetime: String get() = BillingStrings.proLifetime

    val ultraMonthly: String get() = BillingStrings.ultraMonthly

    val ultraQuarterly: String get() = BillingStrings.ultraQuarterly

    val ultraYearly: String get() = BillingStrings.ultraYearly

    val ultraLifetime: String get() = BillingStrings.ultraLifetime

    // ==================== ====================

    val communityTabDiscover: String get() = CommunityStrings.communityTabDiscover

    val communityTabFollowing: String get() = CommunityStrings.communityTabFollowing

    val communityTabFeed: String get() = CommunityStrings.communityTabFeed

    val communitySectionFeatured: String get() = CommunityStrings.communitySectionFeatured

    val communitySectionHot: String get() = CommunityStrings.communitySectionHot

    val communitySectionTutorials: String get() = CommunityStrings.communitySectionTutorials

    val communitySectionQuestions: String get() = CommunityStrings.communitySectionQuestions

    val communityEmptyDiscover: String get() = CommunityStrings.communityEmptyDiscover

    val communityEmptyFollowing: String get() = CommunityStrings.communityEmptyFollowing

    val communityEmptyFollowingNotFollowing: String get() = CommunityStrings.communityEmptyFollowingNotFollowing

    val communityEmptyFollowingSuggestion: String get() = CommunityStrings.communityEmptyFollowingSuggestion

    val communityGoDiscover: String get() = CommunityStrings.communityGoDiscover

    val communityTypeShowcase: String get() = CommunityStrings.communityTypeShowcase

    val communityTypeTutorial: String get() = CommunityStrings.communityTypeTutorial

    val communityTypeQuestion: String get() = CommunityStrings.communityTypeQuestion

    val communityTypeDiscussion: String get() = CommunityStrings.communityTypeDiscussion

    val communityDifficultyBeginner: String get() = CommunityStrings.communityDifficultyBeginner

    val communityDifficultyIntermediate: String get() = CommunityStrings.communityDifficultyIntermediate

    val communityDifficultyAdvanced: String get() = CommunityStrings.communityDifficultyAdvanced

    val communityResolvedLabel: String get() = CommunityStrings.communityResolvedLabel

    val communityUseRecipe: String get() = CommunityStrings.communityUseRecipe

    val communityRecipeDesc: String get() = CommunityStrings.communityRecipeDesc

    val communityPostTypeDiscussion: String get() = CommunityStrings.communityPostTypeDiscussion

    val communityPostTypeShowcase: String get() = CommunityStrings.communityPostTypeShowcase

    val communityPostTypeTutorial: String get() = CommunityStrings.communityPostTypeTutorial

    val communityPostTypeQuestion: String get() = CommunityStrings.communityPostTypeQuestion

    val communitySourceTypeWebsite: String get() = CommunityStrings.communitySourceTypeWebsite

    val communitySourceTypeHtml: String get() = CommunityStrings.communitySourceTypeHtml

    val communitySourceTypeMedia: String get() = CommunityStrings.communitySourceTypeMedia

    val communitySourceTypeFrontend: String get() = CommunityStrings.communitySourceTypeFrontend

    val communitySourceTypeServer: String get() = CommunityStrings.communitySourceTypeServer

    val communityEnterAppName: String get() = CommunityStrings.communityEnterAppName

    val communityEnterTutorialTitle: String get() = CommunityStrings.communityEnterTutorialTitle

    val communityEnterQuestionTitle: String get() = CommunityStrings.communityEnterQuestionTitle

    val communityPostTypeLabel: String get() = CommunityStrings.communityPostTypeLabel

    val communityAppNamePlaceholder: String get() = CommunityStrings.communityAppNamePlaceholder

    val communitySourceTypeLabel: String get() = CommunityStrings.communitySourceTypeLabel

    val communityRecipeJsonPlaceholder: String get() = CommunityStrings.communityRecipeJsonPlaceholder

    val installApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装"
        AppLanguage.ENGLISH -> "Install"
        AppLanguage.ARABIC -> "تثبيت"
    }

    val daysAgo: String get() = CommunityStrings.daysAgo

    val hoursAgo: String get() = CommunityStrings.hoursAgo

    val minutesAgo: String get() = CommunityStrings.minutesAgo

    val inProgress: String get() = when (lang) {
        AppLanguage.CHINESE -> "进行中"
        AppLanguage.ENGLISH -> "In progress"
        AppLanguage.ARABIC -> "قيد التقدم"
    }

    // ==================== ====================

    val sortDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "降序"
        AppLanguage.ENGLISH -> "Descending"
        AppLanguage.ARABIC -> "تنازلي"
    }

    val sortAsc: String get() = when (lang) {
        AppLanguage.CHINESE -> "升序"
        AppLanguage.ENGLISH -> "Ascending"
        AppLanguage.ARABIC -> "تصاعدي"
    }

    // ==================== Module Categories ====================

    val catUiEnhance: String get() = ModuleStrings.catUiEnhance

    val catPrivacySecurity: String get() = ModuleStrings.catPrivacySecurity

    val catTools: String get() = ModuleStrings.catTools

    val catAdBlock: String get() = ModuleStrings.catAdBlock

    // ==================== App Store ====================

    val storeLoadingApps: String get() = StoreStrings.storeLoadingApps

    val storeNoContentTryAgain: String get() = StoreStrings.storeNoContentTryAgain

    val storeLoadingModules: String get() = StoreStrings.storeLoadingModules

    val storeNoContentForModules: String get() = StoreStrings.storeNoContentForModules

    val storeInstallSuccess: String get() = StoreStrings.storeInstallSuccess

    val storeInstallFailed: String get() = StoreStrings.storeInstallFailed

    val storeModuleInstallSuccess: String get() = StoreStrings.storeModuleInstallSuccess

    val storeDownloading: String get() = StoreStrings.storeDownloading

    val storeInstalled: String get() = StoreStrings.storeInstalled

    val storeNoReviewsYet: String get() = StoreStrings.storeNoReviewsYet

    val storeScreenshot: String get() = StoreStrings.storeScreenshot

    val storeTotalDownloads: String get() = StoreStrings.storeTotalDownloads

    val storeTotalLikes: String get() = StoreStrings.storeTotalLikes

    val storeAverageRating: String get() = StoreStrings.storeAverageRating

    val storeGet: String get() = StoreStrings.storeGet

    val storeGetDownloadLink: String get() = StoreStrings.storeGetDownloadLink

    val storeInstallApp: String get() = StoreStrings.storeInstallApp

    val storeNoDownloadLink: String get() = StoreStrings.storeNoDownloadLink

    val storePreparingDownload: String get() = StoreStrings.storePreparingDownload

    val storeDownloadFailed: String get() = StoreStrings.storeDownloadFailed

    val storeDownloadingLabel: String get() = StoreStrings.storeDownloadingLabel

    val storeCancelDownload: String get() = StoreStrings.storeCancelDownload

    val storeRedownload: String get() = StoreStrings.storeRedownload

    val storeClearHistory: String get() = StoreStrings.storeClearHistory

    val storeNoDownloadHistory: String get() = StoreStrings.storeNoDownloadHistory

    val storeNoDownloadHistoryDesc: String get() = StoreStrings.storeNoDownloadHistoryDesc

    val storeNoModuleHistory: String get() = StoreStrings.storeNoModuleHistory

    val storeNoModuleHistoryDesc: String get() = StoreStrings.storeNoModuleHistoryDesc

    val storeModuleEnable: String get() = StoreStrings.storeModuleEnable

    val storeModuleDisable: String get() = StoreStrings.storeModuleDisable

    val storeModulesEnabled: String get() = StoreStrings.storeModulesEnabled

    val storeModulesInstalled: String get() = StoreStrings.storeModulesInstalled

    val storeBeFirstToReview: String get() = StoreStrings.storeBeFirstToReview

    val storeConfirmDelistTitle: String get() = StoreStrings.storeConfirmDelistTitle

    val storeConfirmDelisting: String get() = StoreStrings.storeConfirmDelisting

    // ==================== ====================

    val storeReviewSubmitTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "提交评价"
        AppLanguage.ENGLISH -> "Submit Review"
        AppLanguage.ARABIC -> "إرسال التقييم"
    }

    val storeReviewRatingLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "评分"
        AppLanguage.ENGLISH -> "Rating"
        AppLanguage.ARABIC -> "التقييم"
    }

    val storeReviewCommentLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "评价内容"
        AppLanguage.ENGLISH -> "Review"
        AppLanguage.ARABIC -> "المراجعة"
    }

    val storeReviewPlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "写下你的评价..."
        AppLanguage.ENGLISH -> "Write your review..."
        AppLanguage.ARABIC -> "اكتب مراجعتك..."
    }

    val storeReviewSubmit: String get() = when (lang) {
        AppLanguage.CHINESE -> "提交"
        AppLanguage.ENGLISH -> "Submit"
        AppLanguage.ARABIC -> "إرسال"
    }

    val storeReviewCancel: String get() = when (lang) {
        AppLanguage.CHINESE -> "取消"
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.ARABIC -> "إلغاء"
    }

    val storeReviewSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "评价提交成功"
        AppLanguage.ENGLISH -> "Review submitted successfully"
        AppLanguage.ARABIC -> "تم إرسال التقييم بنجاح"
    }

    val storeReviewFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "评价提交失败"
        AppLanguage.ENGLISH -> "Failed to submit review"
        AppLanguage.ARABIC -> "فشل إرسال التقييم"
    }

    // ==================== Report ====================

    val storeReportAppTitle: String get() = StoreStrings.storeReportAppTitle

    val storeReportSelectReason: String get() = StoreStrings.storeReportSelectReason

    val storeReportReasonSpam: String get() = StoreStrings.storeReportReasonSpam

    val storeReportReasonInappropriate: String get() = StoreStrings.storeReportReasonInappropriate

    val storeReportReasonMalicious: String get() = StoreStrings.storeReportReasonMalicious

    val storeReportReasonCopyright: String get() = StoreStrings.storeReportReasonCopyright

    val storeReportReasonOther: String get() = StoreStrings.storeReportReasonOther

    val storeReportDescOptional: String get() = StoreStrings.storeReportDescOptional

    val storeReportSubmit: String get() = StoreStrings.storeReportSubmit

    val storeReportSuccess: String get() = StoreStrings.storeReportSuccess

    val storeReportFailed: String get() = StoreStrings.storeReportFailed

    // ==================== Go ====================

    val goBinarySize: String get() = when (lang) {
        AppLanguage.CHINESE -> "二进制大小"
        AppLanguage.ENGLISH -> "Binary Size"
        AppLanguage.ARABIC -> "حجم الملف الثنائي"
    }

    // ==================== About Page - ====================

    val legalDisclaimerTitle1: String get() = UiStrings.legalDisclaimerTitle1

    val legalDisclaimerContent1: String get() = UiStrings.legalDisclaimerContent1

    val legalDisclaimerTitle2: String get() = UiStrings.legalDisclaimerTitle2

    val legalDisclaimerContent2: String get() = UiStrings.legalDisclaimerContent2

    val legalDisclaimerTitle3: String get() = UiStrings.legalDisclaimerTitle3

    val legalDisclaimerContent3: String get() = UiStrings.legalDisclaimerContent3

    val legalDisclaimerTitle4: String get() = UiStrings.legalDisclaimerTitle4

    val legalDisclaimerContent4: String get() = UiStrings.legalDisclaimerContent4

    val legalDisclaimerTitle5: String get() = UiStrings.legalDisclaimerTitle5

    val legalDisclaimerContent5: String get() = UiStrings.legalDisclaimerContent5

    val legalDisclaimerTitle6: String get() = UiStrings.legalDisclaimerTitle6

    val legalDisclaimerContent6: String get() = UiStrings.legalDisclaimerContent6

    val legalDisclaimerAcceptance: String get() = UiStrings.legalDisclaimerAcceptance

    val legalDisclaimerFooter: String get() = UiStrings.legalDisclaimerFooter

    val madeWithLove: String get() = UiStrings.madeWithLove

    // ==================== ====================
    val shortcutPermissionTitle: String get() = ShellStrings.shortcutPermissionTitle

    val shortcutPermissionGoToSettings: String get() = ShellStrings.shortcutPermissionGoToSettings

    val shortcutPermissionLater: String get() = ShellStrings.shortcutPermissionLater

    val shortcutPermissionXiaomi: String get() = ShellStrings.shortcutPermissionXiaomi

    val shortcutPermissionHuawei: String get() = ShellStrings.shortcutPermissionHuawei

    val shortcutPermissionOppo: String get() = ShellStrings.shortcutPermissionOppo

    val shortcutPermissionVivo: String get() = ShellStrings.shortcutPermissionVivo

    val shortcutPermissionMeizu: String get() = ShellStrings.shortcutPermissionMeizu

    val shortcutPermissionSamsung: String get() = ShellStrings.shortcutPermissionSamsung

    val shortcutPermissionGeneric: String get() = ShellStrings.shortcutPermissionGeneric

    // ---- Error page configuration card ----
    
    val errorPageTitle: String get() = ShellStrings.errorPageTitle
    
    val errorPageSubtitle: String get() = ShellStrings.errorPageSubtitle
    
    val errorPageModeDefault: String get() = ShellStrings.errorPageModeDefault
    
    val errorPageModeDefaultDesc: String get() = ShellStrings.errorPageModeDefaultDesc
    
    val errorPageModeBuiltIn: String get() = ShellStrings.errorPageModeBuiltIn
    
    val errorPageModeBuiltInDesc: String get() = ShellStrings.errorPageModeBuiltInDesc
    
    val errorPageModeCustomHtml: String get() = ShellStrings.errorPageModeCustomHtml
    
    val errorPageModeCustomHtmlDesc: String get() = ShellStrings.errorPageModeCustomHtmlDesc
    
    val errorPageModeCustomMedia: String get() = ShellStrings.errorPageModeCustomMedia
    
    val errorPageModeCustomMediaDesc: String get() = ShellStrings.errorPageModeCustomMediaDesc
    
    val errorPageStyleLabel: String get() = ShellStrings.errorPageStyleLabel
    
    val errorPageStyleMaterial: String get() = ShellStrings.errorPageStyleMaterial
    
    val errorPageStyleSatellite: String get() = ShellStrings.errorPageStyleSatellite
    
    val errorPageStyleOcean: String get() = ShellStrings.errorPageStyleOcean
    
    val errorPageStyleForest: String get() = ShellStrings.errorPageStyleForest
    
    val errorPageStyleMinimal: String get() = ShellStrings.errorPageStyleMinimal
    
    val errorPageStyleNeon: String get() = ShellStrings.errorPageStyleNeon
    
    val errorPageMiniGameLabel: String get() = ShellStrings.errorPageMiniGameLabel
    
    val errorPageMiniGameDesc: String get() = ShellStrings.errorPageMiniGameDesc
    
    val errorPageGameRandom: String get() = ShellStrings.errorPageGameRandom
    
    val errorPageGameBreakout: String get() = ShellStrings.errorPageGameBreakout
    
    val errorPageGameMaze: String get() = ShellStrings.errorPageGameMaze
    
    val errorPageGameInkZen: String get() = ShellStrings.errorPageGameInkZen
    
    val errorPageGameStarCatch: String get() = ShellStrings.errorPageGameStarCatch
    
    val errorPageAutoRetryLabel: String get() = ShellStrings.errorPageAutoRetryLabel
    
    val errorPageAutoRetryDesc: String get() = ShellStrings.errorPageAutoRetryDesc
    
    val errorPageAutoRetryOff: String get() = ShellStrings.errorPageAutoRetryOff
    
    val errorPageRetryIntervalLabel: String get() = ShellStrings.errorPageRetryIntervalLabel
    
    val errorPageCustomHtmlHint: String get() = ShellStrings.errorPageCustomHtmlHint
    
    val errorPageCustomMediaHint: String get() = ShellStrings.errorPageCustomMediaHint
    
    val errorPageLanguageLabel: String get() = ShellStrings.errorPageLanguageLabel
    
    val errorPageLangChinese: String get() = ShellStrings.errorPageLangChinese
    
    val errorPageLangEnglish: String get() = ShellStrings.errorPageLangEnglish
    
    val errorPageLangArabic: String get() = ShellStrings.errorPageLangArabic

    // ==================== HTML Code Editor & Unsaved Changes ====================
    
    val unsavedChangesTitle: String get() = ShellStrings.unsavedChangesTitle
    val unsavedChangesMessage: String get() = ShellStrings.unsavedChangesMessage
    val discardChanges: String get() = when (lang) {
        AppLanguage.CHINESE -> "放弃"
        AppLanguage.ENGLISH -> "Discard"
        AppLanguage.ARABIC -> "تجاهل"
    }
    val keepEditing: String get() = when (lang) {
        AppLanguage.CHINESE -> "继续编辑"
        AppLanguage.ENGLISH -> "Keep Editing"
        AppLanguage.ARABIC -> "متابعة التحرير"
    }
    val writeCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "编写代码"
        AppLanguage.ENGLISH -> "Write Code"
        AppLanguage.ARABIC -> "كتابة الكود"
    }
    val writeCodeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "直接在应用内编写代码，无需外部文件"
        AppLanguage.ENGLISH -> "Write code directly in the app, no external files needed"
        AppLanguage.ARABIC -> "اكتب الكود مباشرة في التطبيق، بدون ملفات خارجية"
    }
    val htmlCodePlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "在此输入 HTML 代码..."
        AppLanguage.ENGLISH -> "Enter HTML code here..."
        AppLanguage.ARABIC -> "أدخل كود HTML هنا..."
    }
    val codeEditorTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码编辑器"
        AppLanguage.ENGLISH -> "Code Editor"
        AppLanguage.ARABIC -> "محرر الكود"
    }
    val orWriteDirectly: String get() = when (lang) {
        AppLanguage.CHINESE -> "或直接编写"
        AppLanguage.ENGLISH -> "or write directly"
        AppLanguage.ARABIC -> "أو اكتب مباشرة"
    }

    // ==================== Spoofing (Device Disguise) ====================

    val deviceDisguiseTitle: String get() = ShellStrings.deviceDisguiseTitle
    val deviceDisguiseSubtitle: String get() = ShellStrings.deviceDisguiseSubtitle
    val deviceDisguiseHint: String get() = ShellStrings.deviceDisguiseHint
    val deviceTypePhone: String get() = ShellStrings.deviceTypePhone
    val deviceTypeTablet: String get() = ShellStrings.deviceTypeTablet
    val deviceTypeDesktop: String get() = ShellStrings.deviceTypeDesktop
    val deviceTypeLaptop: String get() = ShellStrings.deviceTypeLaptop
    val deviceTypeWatch: String get() = ShellStrings.deviceTypeWatch
    val deviceTypeTV: String get() = ShellStrings.deviceTypeTV
    val deviceQuickSelect: String get() = ShellStrings.deviceQuickSelect
    val devicePopularPresets: String get() = ShellStrings.devicePopularPresets
    val deviceCurrentDisguise: String get() = ShellStrings.deviceCurrentDisguise
    val deviceCustomUA: String get() = ShellStrings.deviceCustomUA
    val deviceCustomUAHint: String get() = ShellStrings.deviceCustomUAHint
    val deviceGeneratedUA: String get() = ShellStrings.deviceGeneratedUA
    val deviceDesktopViewport: String get() = ShellStrings.deviceDesktopViewport
    val deviceDesktopViewportHint: String get() = ShellStrings.deviceDesktopViewportHint
    val deviceDisguiseOff: String get() = ShellStrings.deviceDisguiseOff
    val deviceDisguiseActive: String get() = ShellStrings.deviceDisguiseActive
    
    // Note.
    val deviceCustomDevice: String get() = ShellStrings.deviceCustomDevice
    val deviceCustomDeviceHint: String get() = ShellStrings.deviceCustomDeviceHint
    val deviceCustomName: String get() = ShellStrings.deviceCustomName
    val deviceCustomModelId: String get() = ShellStrings.deviceCustomModelId
    val deviceCustomWidth: String get() = ShellStrings.deviceCustomWidth
    val deviceCustomHeight: String get() = ShellStrings.deviceCustomHeight
    val deviceCustomDensity: String get() = ShellStrings.deviceCustomDensity
    val deviceCustomApply: String get() = ShellStrings.deviceCustomApply

}
