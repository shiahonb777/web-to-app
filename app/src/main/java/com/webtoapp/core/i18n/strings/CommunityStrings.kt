package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object CommunityStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage

    val tabCommunity: String get() = when (lang) {
        AppLanguage.CHINESE -> "社区"
        AppLanguage.ENGLISH -> "Community"
        AppLanguage.ARABIC -> "المجتمع"
    }

    val communityCreatePost: String get() = when (lang) {
        AppLanguage.CHINESE -> "发布帖子"
        AppLanguage.ENGLISH -> "Create Post"
        AppLanguage.ARABIC -> "إنشاء منشور"
    }

    val communityWhatsNew: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享你的 Web 作品和想法..."
        AppLanguage.ENGLISH -> "Share your web creations and ideas..."
        AppLanguage.ARABIC -> "شارك إبداعاتك وأفكارك..."
    }

    val communitySelectTags: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择相关技术标签"
        AppLanguage.ENGLISH -> "Select related tech tags"
        AppLanguage.ARABIC -> "اختر العلامات التقنية"
    }

    val communityLinkApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "关联应用"
        AppLanguage.ENGLISH -> "Link App"
        AppLanguage.ARABIC -> "ربط التطبيق"
    }

    val communityAddMedia: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加图片/视频"
        AppLanguage.ENGLISH -> "Add media"
        AppLanguage.ARABIC -> "إضافة وسائط"
    }

    val communityPublish: String get() = when (lang) {
        AppLanguage.CHINESE -> "发布"
        AppLanguage.ENGLISH -> "Post"
        AppLanguage.ARABIC -> "نشر"
    }

    val communityLike: String get() = when (lang) {
        AppLanguage.CHINESE -> "点赞"
        AppLanguage.ENGLISH -> "Like"
        AppLanguage.ARABIC -> "إعجاب"
    }

    val communityShare: String get() = when (lang) {
        AppLanguage.CHINESE -> "转发"
        AppLanguage.ENGLISH -> "Share"
        AppLanguage.ARABIC -> "مشاركة"
    }

    val communityComment: String get() = when (lang) {
        AppLanguage.CHINESE -> "评论"
        AppLanguage.ENGLISH -> "Comment"
        AppLanguage.ARABIC -> "تعليق"
    }

    val communityReport: String get() = when (lang) {
        AppLanguage.CHINESE -> "举报"
        AppLanguage.ENGLISH -> "Report"
        AppLanguage.ARABIC -> "إبلاغ"
    }

    val communityAllTags: String get() = when (lang) {
        AppLanguage.CHINESE -> "全部"
        AppLanguage.ENGLISH -> "All"
        AppLanguage.ARABIC -> "الكل"
    }

    val communityNoPosts: String get() = when (lang) {
        AppLanguage.CHINESE -> "还没有帖子，来发布第一个吧！"
        AppLanguage.ENGLISH -> "No posts yet. Be the first!"
        AppLanguage.ARABIC -> "لا توجد منشورات بعد، كن الأول!"
    }

    val communityOnline: String get() = when (lang) {
        AppLanguage.CHINESE -> "在线"
        AppLanguage.ENGLISH -> "Online"
        AppLanguage.ARABIC -> "متصل"
    }

    val communityOffline: String get() = when (lang) {
        AppLanguage.CHINESE -> "离线"
        AppLanguage.ENGLISH -> "Offline"
        AppLanguage.ARABIC -> "غير متصل"
    }

    val communityTodayOnline: String get() = when (lang) {
        AppLanguage.CHINESE -> "今日在线"
        AppLanguage.ENGLISH -> "Today"
        AppLanguage.ARABIC -> "اليوم"
    }

    val communityMonthOnline: String get() = when (lang) {
        AppLanguage.CHINESE -> "本月在线"
        AppLanguage.ENGLISH -> "This Month"
        AppLanguage.ARABIC -> "هذا الشهر"
    }

    val communityYearOnline: String get() = when (lang) {
        AppLanguage.CHINESE -> "本年在线"
        AppLanguage.ENGLISH -> "This Year"
        AppLanguage.ARABIC -> "هذا العام"
    }

    val communityPosts: String get() = when (lang) {
        AppLanguage.CHINESE -> "帖子"
        AppLanguage.ENGLISH -> "Posts"
        AppLanguage.ARABIC -> "منشورات"
    }

    val communityActivity: String get() = when (lang) {
        AppLanguage.CHINESE -> "动态"
        AppLanguage.ENGLISH -> "Activity"
        AppLanguage.ARABIC -> "النشاط"
    }

    val badgeDeveloper: String get() = when (lang) {
        AppLanguage.CHINESE -> "开发者"
        AppLanguage.ENGLISH -> "Developer"
        AppLanguage.ARABIC -> "مطور"
    }

    val badgeTeamOwner: String get() = when (lang) {
        AppLanguage.CHINESE -> "队长"
        AppLanguage.ENGLISH -> "Owner"
        AppLanguage.ARABIC -> "مالك"
    }

    val badgeTeamAdmin: String get() = when (lang) {
        AppLanguage.CHINESE -> "管理员"
        AppLanguage.ENGLISH -> "Admin"
        AppLanguage.ARABIC -> "مشرف"
    }

    val badgeTeamMember: String get() = when (lang) {
        AppLanguage.CHINESE -> "成员"
        AppLanguage.ENGLISH -> "Member"
        AppLanguage.ARABIC -> "عضو"
    }

    val communityViewApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "查看应用"
        AppLanguage.ENGLISH -> "View App"
        AppLanguage.ARABIC -> "عرض التطبيق"
    }

    val communityPostSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "帖子发布成功"
        AppLanguage.ENGLISH -> "Post published"
        AppLanguage.ARABIC -> "تم نشر المنشور"
    }

    val communityTagRequired: String get() = when (lang) {
        AppLanguage.CHINESE -> "请至少选择一个标签"
        AppLanguage.ENGLISH -> "Please select at least one tag"
        AppLanguage.ARABIC -> "يرجى اختيار علامة واحدة على الأقل"
    }

    val communityTagMaxLimit: String get() = when (lang) {
        AppLanguage.CHINESE -> "最多只能选择 3 个标签"
        AppLanguage.ENGLISH -> "You can select up to 3 tags"
        AppLanguage.ARABIC -> "يمكنك اختيار 3 علامات كحد أقصى"
    }

    val communityLoginToPost: String get() = when (lang) {
        AppLanguage.CHINESE -> "请先登录后再发帖"
        AppLanguage.ENGLISH -> "Please sign in to create a post"
        AppLanguage.ARABIC -> "يرجى تسجيل الدخول لإنشاء منشور"
    }

    val communityApplication: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用"
        AppLanguage.ENGLISH -> "Application"
        AppLanguage.ARABIC -> "تطبيق"
    }

    val communityPublishFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "发布失败"
        AppLanguage.ENGLISH -> "Publish failed"
        AppLanguage.ARABIC -> "فشل النشر"
    }

    val communityNoAppsToLink: String get() = when (lang) {
        AppLanguage.CHINESE -> "没有可关联的应用"
        AppLanguage.ENGLISH -> "No apps to link"
        AppLanguage.ARABIC -> "لا توجد تطبيقات للربط"
    }

    val communityConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定"
        AppLanguage.ENGLISH -> "OK"
        AppLanguage.ARABIC -> "حسناً"
    }

    val communityNoRepliesYet: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无回复"
        AppLanguage.ENGLISH -> "No replies yet"
        AppLanguage.ARABIC -> "لا توجد ردود بعد"
    }

    val communityBeFirstReply: String get() = when (lang) {
        AppLanguage.CHINESE -> "来发表第一条评论吧"
        AppLanguage.ENGLISH -> "Be the first to reply."
        AppLanguage.ARABIC -> "كن أول من يرد."
    }

    val communityPostNotFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "帖子不存在"
        AppLanguage.ENGLISH -> "Post not found"
        AppLanguage.ARABIC -> "المنشور غير موجود"
    }

    val communityLastSeen: String get() = when (lang) {
        AppLanguage.CHINESE -> "最后上线 %s"
        AppLanguage.ENGLISH -> "Last seen %s"
        AppLanguage.ARABIC -> "آخر ظهور %s"
    }

    val communityJoined: String get() = when (lang) {
        AppLanguage.CHINESE -> "加入于 %s"
        AppLanguage.ENGLISH -> "Joined %s"
        AppLanguage.ARABIC -> "انضم في %s"
    }

    val communityFollowing: String get() = when (lang) {
        AppLanguage.CHINESE -> "关注"
        AppLanguage.ENGLISH -> "Following"
        AppLanguage.ARABIC -> "متابَع"
    }

    val communityFollowers: String get() = when (lang) {
        AppLanguage.CHINESE -> "粉丝"
        AppLanguage.ENGLISH -> "Followers"
        AppLanguage.ARABIC -> "متابِعون"
    }

    val communityApps: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用"
        AppLanguage.ENGLISH -> "Apps"
        AppLanguage.ARABIC -> "التطبيقات"
    }

    val communityModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块"
        AppLanguage.ENGLISH -> "Modules"
        AppLanguage.ARABIC -> "الوحدات"
    }

    val communityTeamWorks: String get() = when (lang) {
        AppLanguage.CHINESE -> "团队作品"
        AppLanguage.ENGLISH -> "Team Works"
        AppLanguage.ARABIC -> "أعمال الفريق"
    }

    val communityNoModulesYet: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无模块"
        AppLanguage.ENGLISH -> "No modules yet"
        AppLanguage.ARABIC -> "لا توجد وحدات بعد"
    }

    val communityNoModulesHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "发布的模块将显示在这里"
        AppLanguage.ENGLISH -> "When they publish, they'll show up here."
        AppLanguage.ARABIC -> "عند نشرها، ستظهر هنا."
    }

    val communityNoTeamWorksYet: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无团队作品"
        AppLanguage.ENGLISH -> "No team works yet"
        AppLanguage.ARABIC -> "لا توجد أعمال فريق بعد"
    }

    val communityNoTeamWorksHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "团队贡献将显示在这里"
        AppLanguage.ENGLISH -> "Team contributions will show up here."
        AppLanguage.ARABIC -> "ستظهر مساهمات الفريق هنا."
    }

    val communityNoActivityData: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无活动数据"
        AppLanguage.ENGLISH -> "No activity data"
        AppLanguage.ARABIC -> "لا توجد بيانات نشاط"
    }

    val communityFollow: String get() = when (lang) {
        AppLanguage.CHINESE -> "关注"
        AppLanguage.ENGLISH -> "Follow"
        AppLanguage.ARABIC -> "متابعة"
    }

    val communityFeatured: String get() = when (lang) {
        AppLanguage.CHINESE -> "⭐ 精选"
        AppLanguage.ENGLISH -> "⭐ Featured"
        AppLanguage.ARABIC -> "⭐ مميز"
    }

    val communityLead: String get() = when (lang) {
        AppLanguage.CHINESE -> "🔹 负责人"
        AppLanguage.ENGLISH -> "🔹 Lead"
        AppLanguage.ARABIC -> "🔹 قائد"
    }

    val communityMember: String get() = when (lang) {
        AppLanguage.CHINESE -> "成员"
        AppLanguage.ENGLISH -> "Member"
        AppLanguage.ARABIC -> "عضو"
    }

    val communityPoints: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 积分"
        AppLanguage.ENGLISH -> "%d pts"
        AppLanguage.ARABIC -> "%d نقاط"
    }

    val communityNotifications: String get() = when (lang) {
        AppLanguage.CHINESE -> "通知"
        AppLanguage.ENGLISH -> "Notifications"
        AppLanguage.ARABIC -> "الإشعارات"
    }

    val communityTabAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "全部"
        AppLanguage.ENGLISH -> "All"
        AppLanguage.ARABIC -> "الكل"
    }

    val communityTabActivity: String get() = when (lang) {
        AppLanguage.CHINESE -> "动态"
        AppLanguage.ENGLISH -> "Activity"
        AppLanguage.ARABIC -> "النشاط"
    }

    val communityNothingYet: String get() = when (lang) {
        AppLanguage.CHINESE -> "还没有任何通知"
        AppLanguage.ENGLISH -> "Nothing to see here — yet"
        AppLanguage.ARABIC -> "لا شيء لعرضه هنا — بعد"
    }

    val communityNothingYetHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "互动消息将在这里显示"
        AppLanguage.ENGLISH -> "Interactions will show up here."
        AppLanguage.ARABIC -> "ستظهر التفاعلات هنا."
    }

    val communityNoFeedYet: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无动态"
        AppLanguage.ENGLISH -> "No activity yet"
        AppLanguage.ARABIC -> "لا يوجد نشاط بعد"
    }

    val communityNoFeedYetHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "关注其他用户以查看他们的动态"
        AppLanguage.ENGLISH -> "Follow others to see their activity here."
        AppLanguage.ARABIC -> "تابع الآخرين لرؤية نشاطهم هنا."
    }

    val communityActionPublished: String get() = when (lang) {
        AppLanguage.CHINESE -> "发布了"
        AppLanguage.ENGLISH -> "published"
        AppLanguage.ARABIC -> "نشر"
    }

    val communityActionLiked: String get() = when (lang) {
        AppLanguage.CHINESE -> "点赞了"
        AppLanguage.ENGLISH -> "liked"
        AppLanguage.ARABIC -> "أعجب بـ"
    }

    val communityActionReplied: String get() = when (lang) {
        AppLanguage.CHINESE -> "回复了"
        AppLanguage.ENGLISH -> "replied to"
        AppLanguage.ARABIC -> "رد على"
    }

    val communityActionBookmarked: String get() = when (lang) {
        AppLanguage.CHINESE -> "收藏了"
        AppLanguage.ENGLISH -> "bookmarked"
        AppLanguage.ARABIC -> "أضاف إلى المفضلة"
    }

    val communityActionFollowed: String get() = when (lang) {
        AppLanguage.CHINESE -> "关注了"
        AppLanguage.ENGLISH -> "followed"
        AppLanguage.ARABIC -> "تابع"
    }

    val communityActionInteracted: String get() = when (lang) {
        AppLanguage.CHINESE -> "互动了"
        AppLanguage.ENGLISH -> "interacted with"
        AppLanguage.ARABIC -> "تفاعل مع"
    }

    val communityBookmarks: String get() = when (lang) {
        AppLanguage.CHINESE -> "收藏夹"
        AppLanguage.ENGLISH -> "Bookmarks"
        AppLanguage.ARABIC -> "المفضلة"
    }

    val communitySaveForLater: String get() = when (lang) {
        AppLanguage.CHINESE -> "收藏你感兴趣的内容"
        AppLanguage.ENGLISH -> "Save posts for later"
        AppLanguage.ARABIC -> "احفظ المنشورات لوقت لاحق"
    }

    val communitySaveForLaterHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "收藏模块，方便以后再次查找"
        AppLanguage.ENGLISH -> "Bookmark modules to easily find them again in the future."
        AppLanguage.ARABIC -> "أضف الوحدات للمفضلة للعثور عليها بسهولة في المستقبل."
    }

    val timeJustNow: String get() = when (lang) {
        AppLanguage.CHINESE -> "刚刚"
        AppLanguage.ENGLISH -> "just now"
        AppLanguage.ARABIC -> "الآن"
    }

    val timeMinutesAgo: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d分钟前"
        AppLanguage.ENGLISH -> "%dm ago"
        AppLanguage.ARABIC -> "منذ %d دقيقة"
    }

    val timeHoursAgo: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d小时前"
        AppLanguage.ENGLISH -> "%dh ago"
        AppLanguage.ARABIC -> "منذ %d ساعة"
    }

    val timeDaysAgo: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d天前"
        AppLanguage.ENGLISH -> "%dd ago"
        AppLanguage.ARABIC -> "منذ %d يوم"
    }

    val timeWeeksAgo: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d周前"
        AppLanguage.ENGLISH -> "%dw ago"
        AppLanguage.ARABIC -> "منذ %d أسبوع"
    }

    val timeMonthsAgo: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d月前"
        AppLanguage.ENGLISH -> "%dmo ago"
        AppLanguage.ARABIC -> "منذ %d شهر"
    }

    val durationHourMinute: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d小时%d分"
        AppLanguage.ENGLISH -> "%dh %dm"
        AppLanguage.ARABIC -> "%d ساعة %d دقيقة"
    }

    val durationMinute: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d分钟"
        AppLanguage.ENGLISH -> "%dm"
        AppLanguage.ARABIC -> "%d دقيقة"
    }

    val durationLessThanMinute: String get() = when (lang) {
        AppLanguage.CHINESE -> "<1分钟"
        AppLanguage.ENGLISH -> "<1m"
        AppLanguage.ARABIC -> "أقل من دقيقة"
    }

    val communitySearch: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索"
        AppLanguage.ENGLISH -> "Search"
        AppLanguage.ARABIC -> "بحث"
    }

    val communitySearchUsers: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索用户"
        AppLanguage.ENGLISH -> "Search users"
        AppLanguage.ARABIC -> "البحث عن مستخدمين"
    }

    val communitySearchHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入用户名或昵称..."
        AppLanguage.ENGLISH -> "Enter username or display name..."
        AppLanguage.ARABIC -> "أدخل اسم المستخدم أو الاسم المعروض..."
    }

    val communityNoUsersFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "未找到用户"
        AppLanguage.ENGLISH -> "No users found"
        AppLanguage.ARABIC -> "لم يتم العثور على مستخدمين"
    }

    val communitySearchPosts: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索帖子"
        AppLanguage.ENGLISH -> "Search posts"
        AppLanguage.ARABIC -> "البحث عن منشورات"
    }

    val communitySearchPostsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入关键字搜索帖子..."
        AppLanguage.ENGLISH -> "Enter keywords to search posts..."
        AppLanguage.ARABIC -> "أدخل كلمات للبحث عن منشورات..."
    }

    val communityNoPostsFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "未找到相关帖子"
        AppLanguage.ENGLISH -> "No posts found"
        AppLanguage.ARABIC -> "لم يتم العثور على منشورات"
    }

    val communityTabUsers: String get() = when (lang) {
        AppLanguage.CHINESE -> "用户"
        AppLanguage.ENGLISH -> "Users"
        AppLanguage.ARABIC -> "المستخدمون"
    }

    val communityTabPostsSearch: String get() = when (lang) {
        AppLanguage.CHINESE -> "帖子"
        AppLanguage.ENGLISH -> "Posts"
        AppLanguage.ARABIC -> "المنشورات"
    }

    val communitySearchAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索用户或帖子"
        AppLanguage.ENGLISH -> "Search users or posts"
        AppLanguage.ARABIC -> "البحث عن مستخدمين أو منشورات"
    }

    val communityMentionSelectUser: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择要提及的用户"
        AppLanguage.ENGLISH -> "Select a user to mention"
        AppLanguage.ARABIC -> "اختر مستخدمًا للإشارة إليه"
    }

    val communityFollowersList: String get() = when (lang) {
        AppLanguage.CHINESE -> "粉丝"
        AppLanguage.ENGLISH -> "Followers"
        AppLanguage.ARABIC -> "المتابعون"
    }

    val communityFollowingList: String get() = when (lang) {
        AppLanguage.CHINESE -> "关注"
        AppLanguage.ENGLISH -> "Following"
        AppLanguage.ARABIC -> "المتابَعون"
    }

    val communityEditProfile: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑资料"
        AppLanguage.ENGLISH -> "Edit Profile"
        AppLanguage.ARABIC -> "تعديل الملف الشخصي"
    }

    val communityMutualFollow: String get() = when (lang) {
        AppLanguage.CHINESE -> "互相关注"
        AppLanguage.ENGLISH -> "Mutual"
        AppLanguage.ARABIC -> "متبادل"
    }

    val communityNoFollowers: String get() = when (lang) {
        AppLanguage.CHINESE -> "还没有粉丝"
        AppLanguage.ENGLISH -> "No followers yet"
        AppLanguage.ARABIC -> "لا يوجد متابعون بعد"
    }

    val communityNoFollowing: String get() = when (lang) {
        AppLanguage.CHINESE -> "还没有关注任何人"
        AppLanguage.ENGLISH -> "Not following anyone yet"
        AppLanguage.ARABIC -> "لا يتابع أي شخص بعد"
    }

    val communityDeletePost: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除帖子"
        AppLanguage.ENGLISH -> "Delete Post"
        AppLanguage.ARABIC -> "حذف المنشور"
    }

    val communityDeleteConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定删除这条帖子吗？"
        AppLanguage.ENGLISH -> "Delete this post?"
        AppLanguage.ARABIC -> "هل تريد حذف هذا المنشور؟"
    }

    val communityViews: String get() = when (lang) {
        AppLanguage.CHINESE -> "浏览"
        AppLanguage.ENGLISH -> "Views"
        AppLanguage.ARABIC -> "مشاهدات"
    }

    val communityEditPost: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑帖子"
        AppLanguage.ENGLISH -> "Edit Post"
        AppLanguage.ARABIC -> "تعديل المنشور"
    }

    val communityConfirmDelete: String get() = when (lang) {
        AppLanguage.CHINESE -> "确认删除"
        AppLanguage.ENGLISH -> "Delete"
        AppLanguage.ARABIC -> "حذف"
    }

    val communityDeletePostConfirmMsg: String get() = when (lang) {
        AppLanguage.CHINESE -> "此操作不可撤销，帖子将被永久删除。"
        AppLanguage.ENGLISH -> "This action cannot be undone. The post will be permanently deleted."
        AppLanguage.ARABIC -> "لا يمكن التراجع عن هذا الإجراء. سيتم حذف المنشور نهائيًا."
    }

    val communityCancel: String get() = when (lang) {
        AppLanguage.CHINESE -> "取消"
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.ARABIC -> "إلغاء"
    }

    val communitySave: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存"
        AppLanguage.ENGLISH -> "Save"
        AppLanguage.ARABIC -> "حفظ"
    }

    val communityPost: String get() = when (lang) {
        AppLanguage.CHINESE -> "帖子"
        AppLanguage.ENGLISH -> "Post"
        AppLanguage.ARABIC -> "منشور"
    }

    val communityPostYourReply: String get() = when (lang) {
        AppLanguage.CHINESE -> "发表你的回复"
        AppLanguage.ENGLISH -> "Post your reply"
        AppLanguage.ARABIC -> "اكتب ردك"
    }

    val communityShowMoreReplies: String get() = when (lang) {
        AppLanguage.CHINESE -> "展开更多回复"
        AppLanguage.ENGLISH -> "Show more replies"
        AppLanguage.ARABIC -> "عرض المزيد من الردود"
    }

    val communityReportTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "举报"
        AppLanguage.ENGLISH -> "Report"
        AppLanguage.ARABIC -> "إبلاغ"
    }

    val communityReportWhy: String get() = when (lang) {
        AppLanguage.CHINESE -> "举报原因是什么？"
        AppLanguage.ENGLISH -> "Why are you reporting this?"
        AppLanguage.ARABIC -> "لماذا تبلغ عن هذا؟"
    }

    val communityReportSubmit: String get() = when (lang) {
        AppLanguage.CHINESE -> "提交"
        AppLanguage.ENGLISH -> "Submit"
        AppLanguage.ARABIC -> "إرسال"
    }

    val communityReportSpam: String get() = when (lang) {
        AppLanguage.CHINESE -> "垃圾信息"
        AppLanguage.ENGLISH -> "Spam"
        AppLanguage.ARABIC -> "رسائل مزعجة"
    }

    val communityReportInappropriate: String get() = when (lang) {
        AppLanguage.CHINESE -> "不当内容"
        AppLanguage.ENGLISH -> "Inappropriate content"
        AppLanguage.ARABIC -> "محتوى غير لائق"
    }

    val communityReportMalicious: String get() = when (lang) {
        AppLanguage.CHINESE -> "恶意代码"
        AppLanguage.ENGLISH -> "Malicious code"
        AppLanguage.ARABIC -> "شيفرة خبيثة"
    }

    val communityReportCopyright: String get() = when (lang) {
        AppLanguage.CHINESE -> "版权侵犯"
        AppLanguage.ENGLISH -> "Copyright violation"
        AppLanguage.ARABIC -> "انتهاك حقوق النشر"
    }

    val communityReportOther: String get() = when (lang) {
        AppLanguage.CHINESE -> "其他"
        AppLanguage.ENGLISH -> "Other"
        AppLanguage.ARABIC -> "أخرى"
    }

    val communityDownloads: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 次下载"
        AppLanguage.ENGLISH -> "%d downloads"
        AppLanguage.ARABIC -> "%d تنزيل"
    }

    val communityRatings: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个评分"
        AppLanguage.ENGLISH -> "%d ratings"
        AppLanguage.ARABIC -> "%d تقييم"
    }







    val communityGroup: String get() = when (lang) {
        AppLanguage.CHINESE -> "交流群"
        AppLanguage.ENGLISH -> "Community Group"
        AppLanguage.ARABIC -> "مجموعة المجتمع"
    }

    val communityGroupDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "一起学习进步，获取最新消息 🚀"
        AppLanguage.ENGLISH -> "Learn together, get latest updates 🚀"
        AppLanguage.ARABIC -> "تعلم معًا، احصل على آخر التحديثات 🚀"
    }



    val communityTabDiscover: String get() = when (lang) {
        AppLanguage.CHINESE -> "发现"
        AppLanguage.ENGLISH -> "Discover"
        AppLanguage.ARABIC -> "اكتشف"
    }

    val communityTabFollowing: String get() = when (lang) {
        AppLanguage.CHINESE -> "关注"
        AppLanguage.ENGLISH -> "Following"
        AppLanguage.ARABIC -> "متابعة"
    }

    val communityTabFeed: String get() = when (lang) {
        AppLanguage.CHINESE -> "广场"
        AppLanguage.ENGLISH -> "Feed"
        AppLanguage.ARABIC -> "الخلاصة"
    }

    val communitySectionFeatured: String get() = when (lang) {
        AppLanguage.CHINESE -> "精选作品"
        AppLanguage.ENGLISH -> "Featured Works"
        AppLanguage.ARABIC -> "الأعمال المميزة"
    }

    val communitySectionHot: String get() = when (lang) {
        AppLanguage.CHINESE -> "热门动态"
        AppLanguage.ENGLISH -> "Hot Updates"
        AppLanguage.ARABIC -> "آخر التحديثات الساخنة"
    }

    val communitySectionTutorials: String get() = when (lang) {
        AppLanguage.CHINESE -> "最新教程"
        AppLanguage.ENGLISH -> "Latest Tutorials"
        AppLanguage.ARABIC -> "أحدث الدروس التعليمية"
    }

    val communitySectionQuestions: String get() = when (lang) {
        AppLanguage.CHINESE -> "等待解答"
        AppLanguage.ENGLISH -> "Awaiting Answers"
        AppLanguage.ARABIC -> "في انتظار الإجابات"
    }

    val communityEmptyDiscover: String get() = when (lang) {
        AppLanguage.CHINESE -> "还没有内容，成为第一个分享者吧！"
        AppLanguage.ENGLISH -> "No content yet. Be the first to share!"
        AppLanguage.ARABIC -> "لا يوجد محتوى بعد. كن أول من يشارك!"
    }

    val communityEmptyFollowing: String get() = when (lang) {
        AppLanguage.CHINESE -> "登录后查看关注内容"
        AppLanguage.ENGLISH -> "Log in to see following content"
        AppLanguage.ARABIC -> "سجل الدخول لرؤية محتوى المتابعة"
    }

    val communityEmptyFollowingNotFollowing: String get() = when (lang) {
        AppLanguage.CHINESE -> "还没有关注的人"
        AppLanguage.ENGLISH -> "Not following anyone yet"
        AppLanguage.ARABIC -> "لم تقم بمتابعة أحد بعد"
    }

    val communityEmptyFollowingSuggestion: String get() = when (lang) {
        AppLanguage.CHINESE -> "去发现页看看有趣的创作者吧"
        AppLanguage.ENGLISH -> "Check out interesting creators on Discover"
        AppLanguage.ARABIC -> "تحقق من المبدعين المثيرين للاهتمام في اكتشف"
    }

    val communityGoDiscover: String get() = when (lang) {
        AppLanguage.CHINESE -> "去发现"
        AppLanguage.ENGLISH -> "Go to Discover"
        AppLanguage.ARABIC -> "اذهب إلى اكتشف"
    }

    val communityTypeShowcase: String get() = when (lang) {
        AppLanguage.CHINESE -> "作品展示"
        AppLanguage.ENGLISH -> "Showcase"
        AppLanguage.ARABIC -> "معرض الأعمال"
    }

    val communityTypeTutorial: String get() = when (lang) {
        AppLanguage.CHINESE -> "教程"
        AppLanguage.ENGLISH -> "Tutorial"
        AppLanguage.ARABIC -> "درس تعليمي"
    }

    val communityTypeQuestion: String get() = when (lang) {
        AppLanguage.CHINESE -> "提问"
        AppLanguage.ENGLISH -> "Question"
        AppLanguage.ARABIC -> "سؤال"
    }

    val communityTypeDiscussion: String get() = when (lang) {
        AppLanguage.CHINESE -> "动态"
        AppLanguage.ENGLISH -> "Discussion"
        AppLanguage.ARABIC -> "نقاش"
    }

    val communityDifficultyBeginner: String get() = when (lang) {
        AppLanguage.CHINESE -> "入门"
        AppLanguage.ENGLISH -> "Beginner"
        AppLanguage.ARABIC -> "مبتدئ"
    }

    val communityDifficultyIntermediate: String get() = when (lang) {
        AppLanguage.CHINESE -> "进阶"
        AppLanguage.ENGLISH -> "Intermediate"
        AppLanguage.ARABIC -> "متوسط"
    }

    val communityDifficultyAdvanced: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级"
        AppLanguage.ENGLISH -> "Advanced"
        AppLanguage.ARABIC -> "متقدم"
    }

    val communityResolvedLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "已解决"
        AppLanguage.ENGLISH -> "Solved"
        AppLanguage.ARABIC -> "تم الحل"
    }

    val communityUseRecipe: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用这个配方"
        AppLanguage.ENGLISH -> "Use this recipe"
        AppLanguage.ARABIC -> "استخدام هذه الوصفة"
    }

    val communityRecipeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "一键导入项目配置到你的应用"
        AppLanguage.ENGLISH -> "One-click import project config to your app"
        AppLanguage.ARABIC -> "استيراد تكوين المشروع بنقرة واحدة إلى تطبيقك"
    }

    val communityPostTypeDiscussion: String get() = when (lang) {
        AppLanguage.CHINESE -> "💬 动态"
        AppLanguage.ENGLISH -> "💬 Discussion"
        AppLanguage.ARABIC -> "💬 نقاش"
    }

    val communityPostTypeShowcase: String get() = when (lang) {
        AppLanguage.CHINESE -> "🎨 作品"
        AppLanguage.ENGLISH -> "🎨 Showcase"
        AppLanguage.ARABIC -> "🎨 معرض"
    }

    val communityPostTypeTutorial: String get() = when (lang) {
        AppLanguage.CHINESE -> "📖 教程"
        AppLanguage.ENGLISH -> "📖 Tutorial"
        AppLanguage.ARABIC -> "📖 درس"
    }

    val communityPostTypeQuestion: String get() = when (lang) {
        AppLanguage.CHINESE -> "❓ 提问"
        AppLanguage.ENGLISH -> "❓ Question"
        AppLanguage.ARABIC -> "❓ سؤال"
    }

    val communitySourceTypeWebsite: String get() = when (lang) {
        AppLanguage.CHINESE -> "网站"
        AppLanguage.ENGLISH -> "Website"
        AppLanguage.ARABIC -> "موقع ويب"
    }

    val communitySourceTypeHtml: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML"
        AppLanguage.ENGLISH -> "HTML"
        AppLanguage.ARABIC -> "HTML"
    }

    val communitySourceTypeMedia: String get() = when (lang) {
        AppLanguage.CHINESE -> "多媒体"
        AppLanguage.ENGLISH -> "Media"
        AppLanguage.ARABIC -> "وسائط"
    }

    val communitySourceTypeFrontend: String get() = when (lang) {
        AppLanguage.CHINESE -> "前端框架"
        AppLanguage.ENGLISH -> "Frontend Framework"
        AppLanguage.ARABIC -> "إطار عمل الواجهة الأمامية"
    }

    val communitySourceTypeServer: String get() = when (lang) {
        AppLanguage.CHINESE -> "服务端"
        AppLanguage.ENGLISH -> "Backend"
        AppLanguage.ARABIC -> "الواجهة الخلفية"
    }

    val communityEnterAppName: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入应用名称"
        AppLanguage.ENGLISH -> "Please enter app name"
        AppLanguage.ARABIC -> "يرجى إدخال اسم التطبيق"
    }

    val communityEnterTutorialTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入教程标题"
        AppLanguage.ENGLISH -> "Please enter tutorial title"
        AppLanguage.ARABIC -> "يرجى إدخال عنوان الدرس"
    }

    val communityEnterQuestionTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入问题标题"
        AppLanguage.ENGLISH -> "Please enter question title"
        AppLanguage.ARABIC -> "يرجى إدخال عنوان السؤال"
    }

    val communityPostTypeLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "发布类型"
        AppLanguage.ENGLISH -> "Post Type"
        AppLanguage.ARABIC -> "نوع النشر"
    }

    val communityAppNamePlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用名称 *"
        AppLanguage.ENGLISH -> "App Name *"
        AppLanguage.ARABIC -> "اسم التطبيق *"
    }

    val communitySourceTypeLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "来源类型"
        AppLanguage.ENGLISH -> "Source Type"
        AppLanguage.ARABIC -> "نوع المصدر"
    }

    val communityRecipeJsonPlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目配方 JSON（可选，用户可一键导入）"
        AppLanguage.ENGLISH -> "Project recipe JSON (optional, users can one-click import)"
        AppLanguage.ARABIC -> "وصفة المشروع JSON (اختياري، يمكن للمستخدمين الاستيراد بنقرة واحدة)"
    }

    val daysAgo: String get() = when (lang) {
        AppLanguage.CHINESE -> "天前"
        AppLanguage.ENGLISH -> "days ago"
        AppLanguage.ARABIC -> "منذ أيام"
    }

    val hoursAgo: String get() = when (lang) {
        AppLanguage.CHINESE -> "小时前"
        AppLanguage.ENGLISH -> "hours ago"
        AppLanguage.ARABIC -> "منذ ساعات"
    }

    val minutesAgo: String get() = when (lang) {
        AppLanguage.CHINESE -> "分钟前"
        AppLanguage.ENGLISH -> "minutes ago"
        AppLanguage.ARABIC -> "منذ دقائق"
    }
}
