package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object StoreStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage
    // ==================== App Store ====================
    val storeSearchPlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索应用..."
        AppLanguage.ENGLISH -> "Search apps..."
        AppLanguage.ARABIC -> "البحث عن التطبيقات..."
    }

    val storeAllCategories: String get() = when (lang) {
        AppLanguage.CHINESE -> "全部"
        AppLanguage.ENGLISH -> "All"
        AppLanguage.ARABIC -> "الكل"
    }

    val storeAppsCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用数量"
        AppLanguage.ENGLISH -> "Apps"
        AppLanguage.ARABIC -> "التطبيقات"
    }

    val storeEmpty: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无应用\n快来发布第一个吧"
        AppLanguage.ENGLISH -> "No apps yet\nBe the first to publish!"
        AppLanguage.ARABIC -> "لا توجد تطبيقات بعد\nكن أول من ينشر!"
    }

    val storeLoadMore: String get() = when (lang) {
        AppLanguage.CHINESE -> "加载更多"
        AppLanguage.ENGLISH -> "Load More"
        AppLanguage.ARABIC -> "تحميل المزيد"
    }

    val storeSortDownloads: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载"
        AppLanguage.ENGLISH -> "Downloads"
        AppLanguage.ARABIC -> "التنزيلات"
    }

    val storeSortRating: String get() = when (lang) {
        AppLanguage.CHINESE -> "评分"
        AppLanguage.ENGLISH -> "Rating"
        AppLanguage.ARABIC -> "التقييم"
    }

    val storeSortNewest: String get() = when (lang) {
        AppLanguage.CHINESE -> "最新"
        AppLanguage.ENGLISH -> "Newest"
        AppLanguage.ARABIC -> "الأحدث"
    }

    val storeSortLikes: String get() = when (lang) {
        AppLanguage.CHINESE -> "点赞"
        AppLanguage.ENGLISH -> "Likes"
        AppLanguage.ARABIC -> "الإعجابات"
    }

    val storeReviews: String get() = when (lang) {
        AppLanguage.CHINESE -> "评价"
        AppLanguage.ENGLISH -> "reviews"
        AppLanguage.ARABIC -> "مراجعات"
    }

    val storeDownloads: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载量"
        AppLanguage.ENGLISH -> "downloads"
        AppLanguage.ARABIC -> "تنزيلات"
    }

    val storeLikes: String get() = when (lang) {
        AppLanguage.CHINESE -> "点赞"
        AppLanguage.ENGLISH -> "likes"
        AppLanguage.ARABIC -> "إعجابات"
    }

    val storeDownloadBtn: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载安装"
        AppLanguage.ENGLISH -> "Download"
        AppLanguage.ARABIC -> "تحميل"
    }

    val storeScreenshots: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用截图"
        AppLanguage.ENGLISH -> "Screenshots"
        AppLanguage.ARABIC -> "لقطات الشاشة"
    }

    val storeDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用介绍"
        AppLanguage.ENGLISH -> "About"
        AppLanguage.ARABIC -> "حول التطبيق"
    }

    val storeDeveloperInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "开发者信息"
        AppLanguage.ENGLISH -> "Developer"
        AppLanguage.ARABIC -> "المطور"
    }

    val storeEmail: String get() = when (lang) {
        AppLanguage.CHINESE -> "联系邮箱"
        AppLanguage.ENGLISH -> "Email"
        AppLanguage.ARABIC -> "البريد الإلكتروني"
    }

    val storeWebsite: String get() = when (lang) {
        AppLanguage.CHINESE -> "官网"
        AppLanguage.ENGLISH -> "Website"
        AppLanguage.ARABIC -> "الموقع"
    }

    val storeGroupChat: String get() = when (lang) {
        AppLanguage.CHINESE -> "群聊"
        AppLanguage.ENGLISH -> "Group"
        AppLanguage.ARABIC -> "المجموعة"
    }

    val storePrivacyPolicy: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐私政策"
        AppLanguage.ENGLISH -> "Privacy"
        AppLanguage.ARABIC -> "الخصوصية"
    }

    val storePhone: String get() = when (lang) {
        AppLanguage.CHINESE -> "联系电话"
        AppLanguage.ENGLISH -> "Phone"
        AppLanguage.ARABIC -> "الهاتف"
    }

    val storeReport: String get() = when (lang) {
        AppLanguage.CHINESE -> "举报"
        AppLanguage.ENGLISH -> "Report"
        AppLanguage.ARABIC -> "إبلاغ"
    }

    val storeCatTools: String get() = when (lang) {
        AppLanguage.CHINESE -> "工具"
        AppLanguage.ENGLISH -> "Tools"
        AppLanguage.ARABIC -> "أدوات"
    }

    val storeCatSocial: String get() = when (lang) {
        AppLanguage.CHINESE -> "社交"
        AppLanguage.ENGLISH -> "Social"
        AppLanguage.ARABIC -> "اجتماعي"
    }

    val storeCatEducation: String get() = when (lang) {
        AppLanguage.CHINESE -> "教育"
        AppLanguage.ENGLISH -> "Education"
        AppLanguage.ARABIC -> "تعليم"
    }

    val storeCatEntertainment: String get() = when (lang) {
        AppLanguage.CHINESE -> "娱乐"
        AppLanguage.ENGLISH -> "Entertainment"
        AppLanguage.ARABIC -> "ترفيه"
    }

    val storeCatProductivity: String get() = when (lang) {
        AppLanguage.CHINESE -> "效率"
        AppLanguage.ENGLISH -> "Productivity"
        AppLanguage.ARABIC -> "إنتاجية"
    }

    val storeCatLifestyle: String get() = when (lang) {
        AppLanguage.CHINESE -> "生活"
        AppLanguage.ENGLISH -> "Lifestyle"
        AppLanguage.ARABIC -> "نمط الحياة"
    }

    val storeCatBusiness: String get() = when (lang) {
        AppLanguage.CHINESE -> "商务"
        AppLanguage.ENGLISH -> "Business"
        AppLanguage.ARABIC -> "أعمال"
    }

    val storeCatNews: String get() = when (lang) {
        AppLanguage.CHINESE -> "资讯"
        AppLanguage.ENGLISH -> "News"
        AppLanguage.ARABIC -> "أخبار"
    }

    val storeCatFinance: String get() = when (lang) {
        AppLanguage.CHINESE -> "财务"
        AppLanguage.ENGLISH -> "Finance"
        AppLanguage.ARABIC -> "مالية"
    }

    val storeCatHealth: String get() = when (lang) {
        AppLanguage.CHINESE -> "健康"
        AppLanguage.ENGLISH -> "Health"
        AppLanguage.ARABIC -> "صحة"
    }

    val storeCatOther: String get() = when (lang) {
        AppLanguage.CHINESE -> "其他"
        AppLanguage.ENGLISH -> "Other"
        AppLanguage.ARABIC -> "أخرى"
    }

    val storeDownloadManager: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载管理"
        AppLanguage.ENGLISH -> "Download Manager"
        AppLanguage.ARABIC -> "مدير التنزيلات"
    }

    val storeDownloadedApps: String get() = when (lang) {
        AppLanguage.CHINESE -> "已下载应用"
        AppLanguage.ENGLISH -> "Downloaded Apps"
        AppLanguage.ARABIC -> "التطبيقات المحملة"
    }

    val storeMyApps: String get() = when (lang) {
        AppLanguage.CHINESE -> "我的应用"
        AppLanguage.ENGLISH -> "My Apps"
        AppLanguage.ARABIC -> "تطبيقاتي"
    }

    val storePublishApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "发布应用"
        AppLanguage.ENGLISH -> "Publish App"
        AppLanguage.ARABIC -> "نشر تطبيق"
    }

    val storeNoDownloads: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无下载任务"
        AppLanguage.ENGLISH -> "No active downloads"
        AppLanguage.ARABIC -> "لا توجد تنزيلات نشطة"
    }

    val storeNoDownloadedApps: String get() = when (lang) {
        AppLanguage.CHINESE -> "没有已下载的应用"
        AppLanguage.ENGLISH -> "No downloaded apps"
        AppLanguage.ARABIC -> "لا توجد تطبيقات محملة"
    }

    val storeNoPublishedApps: String get() = when (lang) {
        AppLanguage.CHINESE -> "还没有发布过应用"
        AppLanguage.ENGLISH -> "No published apps yet"
        AppLanguage.ARABIC -> "لم تنشر تطبيقات بعد"
    }

    val storeConfirmUnpublish: String get() = when (lang) {
        AppLanguage.CHINESE -> "确认下架"
        AppLanguage.ENGLISH -> "Confirm Unpublish"
        AppLanguage.ARABIC -> "تأكيد إلغاء النشر"
    }

    val storeInstall: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装"
        AppLanguage.ENGLISH -> "Install"
        AppLanguage.ARABIC -> "تثبيت"
    }

    val storeDelete: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除"
        AppLanguage.ENGLISH -> "Delete"
        AppLanguage.ARABIC -> "حذف"
    }

    val storeCancel: String get() = when (lang) {
        AppLanguage.CHINESE -> "取消"
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.ARABIC -> "إلغاء"
    }

    val storePublishing: String get() = when (lang) {
        AppLanguage.CHINESE -> "发布中..."
        AppLanguage.ENGLISH -> "Publishing..."
        AppLanguage.ARABIC -> "جارٍ النشر..."
    }

    val storePublishSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "🎉 应用发布成功!"
        AppLanguage.ENGLISH -> "🎉 App published successfully!"
        AppLanguage.ARABIC -> "🎉 تم نشر التطبيق بنجاح!"
    }

    val storePublishFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "发布失败"
        AppLanguage.ENGLISH -> "Publish failed"
        AppLanguage.ARABIC -> "فشل النشر"
    }

    val storeFetchingLink: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取下载链接..."
        AppLanguage.ENGLISH -> "Fetching download link..."
        AppLanguage.ARABIC -> "جارٍ جلب رابط التنزيل..."
    }

    val storeLoadFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "加载失败"
        AppLanguage.ENGLISH -> "Loading failed"
        AppLanguage.ARABIC -> "فشل التحميل"
    }

    val storeAppName: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用名称"
        AppLanguage.ENGLISH -> "App Name"
        AppLanguage.ARABIC -> "اسم التطبيق"
    }

    val storeAppDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用描述"
        AppLanguage.ENGLISH -> "Description"
        AppLanguage.ARABIC -> "الوصف"
    }

    val storeCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类"
        AppLanguage.ENGLISH -> "Category"
        AppLanguage.ARABIC -> "الفئة"
    }

    val storeTags: String get() = when (lang) {
        AppLanguage.CHINESE -> "标签"
        AppLanguage.ENGLISH -> "Tags"
        AppLanguage.ARABIC -> "العلامات"
    }

    val storeVersionName: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本名"
        AppLanguage.ENGLISH -> "Version"
        AppLanguage.ARABIC -> "الإصدار"
    }

    val storeVersionCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本号"
        AppLanguage.ENGLISH -> "Version Code"
        AppLanguage.ARABIC -> "رقم الإصدار"
    }

    val storePackageName: String get() = when (lang) {
        AppLanguage.CHINESE -> "包名"
        AppLanguage.ENGLISH -> "Package Name"
        AppLanguage.ARABIC -> "اسم الحزمة"
    }

    val storeIconUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "图标 URL"
        AppLanguage.ENGLISH -> "Icon URL"
        AppLanguage.ARABIC -> "رابط الأيقونة"
    }

    val storeScreenshotUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "截图 URL"
        AppLanguage.ENGLISH -> "Screenshot URL"
        AppLanguage.ARABIC -> "رابط لقطة الشاشة"
    }

    val storeBasicInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "基本信息"
        AppLanguage.ENGLISH -> "Basic Info"
        AppLanguage.ARABIC -> "المعلومات الأساسية"
    }

    val storeDescAndTags: String get() = when (lang) {
        AppLanguage.CHINESE -> "描述和标签"
        AppLanguage.ENGLISH -> "Description & Tags"
        AppLanguage.ARABIC -> "الوصف والعلامات"
    }

    val storeContactInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "联系信息"
        AppLanguage.ENGLISH -> "Contact Info"
        AppLanguage.ARABIC -> "معلومات الاتصال"
    }

    val storeApkLinks: String get() = when (lang) {
        AppLanguage.CHINESE -> "APK 下载链接"
        AppLanguage.ENGLISH -> "APK Download Links"
        AppLanguage.ARABIC -> "روابط تنزيل APK"
    }

    val storePublishFormSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "填写以下信息将应用发布到商店"
        AppLanguage.ENGLISH -> "Fill in the details to publish your app"
        AppLanguage.ARABIC -> "أكمل المعلومات لنشر تطبيقك"
    }

    val storeScreenshotsAdded: String get() = when (lang) {
        AppLanguage.CHINESE -> "已添加 %d 张截图"
        AppLanguage.ENGLISH -> "%d screenshot(s) added"
        AppLanguage.ARABIC -> "تمت إضافة %d لقطة شاشة"
    }

    val storeFillRequired: String get() = when (lang) {
        AppLanguage.CHINESE -> "请填写应用名称和描述"
        AppLanguage.ENGLISH -> "Please fill in app name and description"
        AppLanguage.ARABIC -> "يرجى ملء اسم التطبيق والوصف"
    }

    val storeAddScreenshot: String get() = when (lang) {
        AppLanguage.CHINESE -> "请添加至少一张截图"
        AppLanguage.ENGLISH -> "Please add at least one screenshot"
        AppLanguage.ARABIC -> "يرجى إضافة لقطة شاشة واحدة على الأقل"
    }

    val storeMyModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "我的模块"
        AppLanguage.ENGLISH -> "My Modules"
        AppLanguage.ARABIC -> "وحداتي"
    }

    val storePublishModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "发布模块"
        AppLanguage.ENGLISH -> "Publish Module"
        AppLanguage.ARABIC -> "نشر وحدة"
    }

    val storeNoPublishedModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "还没有发布过模块"
        AppLanguage.ENGLISH -> "No published modules yet"
        AppLanguage.ARABIC -> "لم تنشر وحدات بعد"
    }

    val storeModuleShareCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块分享码"
        AppLanguage.ENGLISH -> "Module Share Code"
        AppLanguage.ARABIC -> "رمز مشاركة الوحدة"
    }

    val storeModuleName: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块名称"
        AppLanguage.ENGLISH -> "Module Name"
        AppLanguage.ARABIC -> "اسم الوحدة"
    }

    val storeModuleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块描述"
        AppLanguage.ENGLISH -> "Module Description"
        AppLanguage.ARABIC -> "وصف الوحدة"
    }

    val storeModulePublishSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "将你的扩展模块分享到模块市场，让更多人使用"
        AppLanguage.ENGLISH -> "Share your extension module to the market"
        AppLanguage.ARABIC -> "شارك وحدتك في السوق ليستخدمها الآخرون"
    }

    val storeModulePublishSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块发布成功！等待审核后将在市场上线"
        AppLanguage.ENGLISH -> "Module published! It will appear after review"
        AppLanguage.ARABIC -> "تم نشر الوحدة! ستظهر بعد المراجعة"
    }

    val storeLoadingApps: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在加载应用..."
        AppLanguage.ENGLISH -> "Loading apps..."
        AppLanguage.ARABIC -> "جاري تحميل التطبيقات..."
    }

    val storeNoContentTryAgain: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无内容，请稍后再试"
        AppLanguage.ENGLISH -> "No content available. Please try again later."
        AppLanguage.ARABIC -> "لا يوجد محتوى. يرجى المحاولة لاحقاً."
    }

    val storeLoadingModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在加载模块..."
        AppLanguage.ENGLISH -> "Loading modules..."
        AppLanguage.ARABIC -> "جاري تحميل الوحدات..."
    }

    val storeNoContentForModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无可用模块"
        AppLanguage.ENGLISH -> "No modules available"
        AppLanguage.ARABIC -> "لا توجد وحدات متاحة"
    }

    val storeInstallSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装成功"
        AppLanguage.ENGLISH -> "Installed successfully"
        AppLanguage.ARABIC -> "تم التثبيت بنجاح"
    }

    val storeInstallFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装失败"
        AppLanguage.ENGLISH -> "Installation failed"
        AppLanguage.ARABIC -> "فشل التثبيت"
    }

    val storeModuleInstallSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块安装成功"
        AppLanguage.ENGLISH -> "Module installed successfully"
        AppLanguage.ARABIC -> "تم تثبيت الوحدة بنجاح"
    }

    val storeDownloading: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载中..."
        AppLanguage.ENGLISH -> "Downloading..."
        AppLanguage.ARABIC -> "جاري التحميل..."
    }

    val storeInstalled: String get() = when (lang) {
        AppLanguage.CHINESE -> "已安装"
        AppLanguage.ENGLISH -> "Installed"
        AppLanguage.ARABIC -> "مثبت"
    }

    val storeNoReviewsYet: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无评价"
        AppLanguage.ENGLISH -> "No reviews yet"
        AppLanguage.ARABIC -> "لا توجد مراجعات بعد"
    }

    val storeScreenshot: String get() = when (lang) {
        AppLanguage.CHINESE -> "截图"
        AppLanguage.ENGLISH -> "Screenshot"
        AppLanguage.ARABIC -> "لقطة شاشة"
    }

    val storeTotalDownloads: String get() = when (lang) {
        AppLanguage.CHINESE -> "总下载量"
        AppLanguage.ENGLISH -> "Total Downloads"
        AppLanguage.ARABIC -> "إجمالي التحميلات"
    }

    val storeTotalLikes: String get() = when (lang) {
        AppLanguage.CHINESE -> "总点赞数"
        AppLanguage.ENGLISH -> "Total Likes"
        AppLanguage.ARABIC -> "إجمالي الإعجابات"
    }

    val storeAverageRating: String get() = when (lang) {
        AppLanguage.CHINESE -> "平均评分"
        AppLanguage.ENGLISH -> "Average Rating"
        AppLanguage.ARABIC -> "متوسط التقييم"
    }

    val storeGet: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取"
        AppLanguage.ENGLISH -> "Get"
        AppLanguage.ARABIC -> "احصل"
    }

    val storeGetDownloadLink: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取下载链接"
        AppLanguage.ENGLISH -> "Get Download Link"
        AppLanguage.ARABIC -> "احصل على رابط التحميل"
    }

    val storeInstallApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装应用"
        AppLanguage.ENGLISH -> "Install App"
        AppLanguage.ARABIC -> "تثبيت التطبيق"
    }

    val storeNoDownloadLink: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无下载链接"
        AppLanguage.ENGLISH -> "No download link available"
        AppLanguage.ARABIC -> "لا يوجد رابط تحميل"
    }

    val storePreparingDownload: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在准备下载..."
        AppLanguage.ENGLISH -> "Preparing download..."
        AppLanguage.ARABIC -> "جاري تحضير التحميل..."
    }

    val storeDownloadFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载失败"
        AppLanguage.ENGLISH -> "Download failed"
        AppLanguage.ARABIC -> "فشل التحميل"
    }

    val storeDownloadingLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载中"
        AppLanguage.ENGLISH -> "Downloading"
        AppLanguage.ARABIC -> "جاري التحميل"
    }

    val storeCancelDownload: String get() = when (lang) {
        AppLanguage.CHINESE -> "取消下载"
        AppLanguage.ENGLISH -> "Cancel Download"
        AppLanguage.ARABIC -> "إلغاء التحميل"
    }

    val storeRedownload: String get() = when (lang) {
        AppLanguage.CHINESE -> "重新下载"
        AppLanguage.ENGLISH -> "Re-download"
        AppLanguage.ARABIC -> "إعادة التحميل"
    }

    val storeClearHistory: String get() = when (lang) {
        AppLanguage.CHINESE -> "清除记录"
        AppLanguage.ENGLISH -> "Clear History"
        AppLanguage.ARABIC -> "مسح السجل"
    }

    val storeNoDownloadHistory: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无下载记录"
        AppLanguage.ENGLISH -> "No download history"
        AppLanguage.ARABIC -> "لا يوجد سجل تحميل"
    }

    val storeNoDownloadHistoryDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载的应用将显示在这里"
        AppLanguage.ENGLISH -> "Downloaded apps will appear here"
        AppLanguage.ARABIC -> "ستظهر التطبيقات المحملة هنا"
    }

    val storeNoModuleHistory: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无模块记录"
        AppLanguage.ENGLISH -> "No module history"
        AppLanguage.ARABIC -> "لا يوجد سجل وحدات"
    }

    val storeNoModuleHistoryDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装的模块将显示在这里"
        AppLanguage.ENGLISH -> "Installed modules will appear here"
        AppLanguage.ARABIC -> "ستظهر الوحدات المثبتة هنا"
    }

    val storeModuleEnable: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用"
        AppLanguage.ENGLISH -> "Enable"
        AppLanguage.ARABIC -> "تفعيل"
    }

    val storeModuleDisable: String get() = when (lang) {
        AppLanguage.CHINESE -> "停用"
        AppLanguage.ENGLISH -> "Disable"
        AppLanguage.ARABIC -> "تعطيل"
    }

    val storeModulesEnabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "已启用模块"
        AppLanguage.ENGLISH -> "Enabled Modules"
        AppLanguage.ARABIC -> "الوحدات المفعلة"
    }

    val storeModulesInstalled: String get() = when (lang) {
        AppLanguage.CHINESE -> "已安装模块"
        AppLanguage.ENGLISH -> "Installed Modules"
        AppLanguage.ARABIC -> "الوحدات المثبتة"
    }

    val storeBeFirstToReview: String get() = when (lang) {
        AppLanguage.CHINESE -> "成为第一个评价的人"
        AppLanguage.ENGLISH -> "Be the first to review"
        AppLanguage.ARABIC -> "كن أول من يقيم"
    }

    val storeConfirmDelistTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "确认下架"
        AppLanguage.ENGLISH -> "Confirm Delisting"
        AppLanguage.ARABIC -> "تأكيد الإزالة"
    }

    val storeConfirmDelisting: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定要下架此应用吗？"
        AppLanguage.ENGLISH -> "Are you sure you want to delist this app?"
        AppLanguage.ARABIC -> "هل أنت متأكد من إزالة هذا التطبيق؟"
    }
    // ==================== Report ====================
    val storeReportAppTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "举报应用"
        AppLanguage.ENGLISH -> "Report App"
        AppLanguage.ARABIC -> "الإبلاغ عن التطبيق"
    }

    val storeReportSelectReason: String get() = when (lang) {
        AppLanguage.CHINESE -> "请选择举报原因"
        AppLanguage.ENGLISH -> "Select a reason"
        AppLanguage.ARABIC -> "اختر سبباً"
    }

    val storeReportReasonSpam: String get() = when (lang) {
        AppLanguage.CHINESE -> "垃圾内容"
        AppLanguage.ENGLISH -> "Spam"
        AppLanguage.ARABIC -> "محتوى مزعج"
    }

    val storeReportReasonInappropriate: String get() = when (lang) {
        AppLanguage.CHINESE -> "不当内容"
        AppLanguage.ENGLISH -> "Inappropriate Content"
        AppLanguage.ARABIC -> "محتوى غير لائق"
    }

    val storeReportReasonMalicious: String get() = when (lang) {
        AppLanguage.CHINESE -> "恶意软件"
        AppLanguage.ENGLISH -> "Malicious Software"
        AppLanguage.ARABIC -> "برنامج ضار"
    }

    val storeReportReasonCopyright: String get() = when (lang) {
        AppLanguage.CHINESE -> "侵犯版权"
        AppLanguage.ENGLISH -> "Copyright Violation"
        AppLanguage.ARABIC -> "انتهاك حقوق النشر"
    }

    val storeReportReasonOther: String get() = when (lang) {
        AppLanguage.CHINESE -> "其他"
        AppLanguage.ENGLISH -> "Other"
        AppLanguage.ARABIC -> "أخرى"
    }

    val storeReportDescOptional: String get() = when (lang) {
        AppLanguage.CHINESE -> "详细说明（可选）"
        AppLanguage.ENGLISH -> "Details (optional)"
        AppLanguage.ARABIC -> "التفاصيل (اختياري)"
    }

    val storeReportSubmit: String get() = when (lang) {
        AppLanguage.CHINESE -> "提交举报"
        AppLanguage.ENGLISH -> "Submit Report"
        AppLanguage.ARABIC -> "إرسال البلاغ"
    }

    val storeReportSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "举报提交成功"
        AppLanguage.ENGLISH -> "Report submitted successfully"
        AppLanguage.ARABIC -> "تم إرسال البلاغ بنجاح"
    }

    val storeReportFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "举报提交失败"
        AppLanguage.ENGLISH -> "Failed to submit report"
        AppLanguage.ARABIC -> "فشل إرسال البلاغ"
    }
}
