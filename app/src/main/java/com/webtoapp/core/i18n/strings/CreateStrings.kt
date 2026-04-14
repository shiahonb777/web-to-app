package com.webtoapp.core.i18n.strings

import com.webtoapp.R
import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object CreateStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage

    val createApp: String get() = when (lang) {
        else -> Strings.resourceString(R.string.title_create_app)
    }

    val createWebApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "网页应用"
        AppLanguage.ENGLISH -> "Web App"
        AppLanguage.ARABIC -> "تطبيق ويب"
    }

    val createMediaApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体应用"
        AppLanguage.ENGLISH -> "Media App"
        AppLanguage.ARABIC -> "تطبيق وسائط"
    }

    val createHtmlApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML App"
        AppLanguage.ENGLISH -> "HTML App"
        AppLanguage.ARABIC -> "تطبيق HTML"
    }

    val createFrontendApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "前端项目"
        AppLanguage.ENGLISH -> "Frontend Project"
        AppLanguage.ARABIC -> "مشروع الواجهة الأمامية"
    }

    val createWordPressApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress 应用"
        AppLanguage.ENGLISH -> "WordPress App"
        AppLanguage.ARABIC -> "تطبيق WordPress"
    }

    val createNodeJsApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js 应用"
        AppLanguage.ENGLISH -> "Node.js App"
        AppLanguage.ARABIC -> "تطبيق Node.js"
    }

    val createPhpApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP 应用"
        AppLanguage.ENGLISH -> "PHP App"
        AppLanguage.ARABIC -> "تطبيق PHP"
    }

    val createPythonApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python 应用"
        AppLanguage.ENGLISH -> "Python App"
        AppLanguage.ARABIC -> "تطبيق Python"
    }

    val createGoApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "Go 服务"
        AppLanguage.ENGLISH -> "Go Service"
        AppLanguage.ARABIC -> "خدمة Go"
    }

    val createMultiWebApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "多站点聚合"
        AppLanguage.ENGLISH -> "Multi-Site App"
        AppLanguage.ARABIC -> "تطبيق متعدد المواقع"
    }

    val createDocsSite: String get() = when (lang) {
        AppLanguage.CHINESE -> "文档站点"
        AppLanguage.ENGLISH -> "Docs Site"
        AppLanguage.ARABIC -> "موقع التوثيق"
    }

    val createMediaAppTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建媒体应用"
        AppLanguage.ENGLISH -> "Create Media App"
        AppLanguage.ARABIC -> "إنشاء تطبيق وسائط"
    }

    val createHtmlAppTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建HTML应用"
        AppLanguage.ENGLISH -> "Create HTML App"
        AppLanguage.ARABIC -> "إنشاء تطبيق HTML"
    }

    val folderImportMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "文件夹导入"
        AppLanguage.ENGLISH -> "Folder Import"
        AppLanguage.ARABIC -> "استيراد مجلد"
    }

    val folderSelectFolder: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择项目文件夹"
        AppLanguage.ENGLISH -> "Select Project Folder"
        AppLanguage.ARABIC -> "اختيار مجلد المشروع"
    }

    val folderSelectHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择包含 HTML 项目的文件夹，自动扫描并导入所有资源文件"
        AppLanguage.ENGLISH -> "Select a folder containing your HTML project, auto-scan and import all resource files"
        AppLanguage.ARABIC -> "اختر مجلدًا يحتوي على مشروع HTML، سيتم فحص واستيراد جميع ملفات الموارد تلقائيًا"
    }

    val folderImporting: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在扫描并导入文件夹..."
        AppLanguage.ENGLISH -> "Scanning and importing folder..."
        AppLanguage.ARABIC -> "جاري فحص واستيراد المجلد..."
    }

    val folderNoHtmlWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "文件夹中未找到 HTML 文件，请确认选择的目录"
        AppLanguage.ENGLISH -> "No HTML files found in folder, please verify the selected directory"
        AppLanguage.ARABIC -> "لم يتم العثور على ملفات HTML في المجلد، يرجى التحقق من الدليل المحدد"
    }

    val folderTip: String get() = when (lang) {
        AppLanguage.CHINESE -> "提示：文件夹导入会保留完整目录结构，所有相对路径引用（CSS/JS/图片/音视频/字体等）自动生效，适合多文件 HTML 项目。"
        AppLanguage.ENGLISH -> "Tip: Folder import preserves the full directory structure. All relative path references (CSS/JS/images/media/fonts etc.) work automatically, ideal for multi-file HTML projects."
        AppLanguage.ARABIC -> "تلميح: يحتفظ استيراد المجلد ببنية المجلدات الكاملة. تعمل جميع المراجع ذات المسارات النسبية تلقائيًا، مثالي لمشاريع HTML متعددة الملفات."
    }

    val folderImportFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "文件夹导入失败: %s"
        AppLanguage.ENGLISH -> "Folder import failed: %s"
        AppLanguage.ARABIC -> "فشل استيراد المجلد: %s"
    }

    val editApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑应用"
        AppLanguage.ENGLISH -> "Edit App"
        AppLanguage.ARABIC -> "تعديل التطبيق"
    }

    val inputAppName: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入应用显示名称"
        AppLanguage.ENGLISH -> "Enter app display name"
        AppLanguage.ARABIC -> "أدخل اسم عرض التطبيق"
    }

    val activationCodeVerify: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活码验证"
        AppLanguage.ENGLISH -> "Activation Code Verification"
        AppLanguage.ARABIC -> "التحقق من رمز التفعيل"
    }

    val activationCodeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用后，用户需要输入正确的激活码才能使用应用"
        AppLanguage.ENGLISH -> "When enabled, users need to enter correct activation code to use the app"
        AppLanguage.ARABIC -> "عند التفعيل، يحتاج المستخدمون إلى إدخال رمز التفعيل الصحيح لاستخدام التطبيق"
    }

    val inputActivationCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入激活码"
        AppLanguage.ENGLISH -> "Enter activation code"
        AppLanguage.ARABIC -> "أدخل رمز التفعيل"
    }

    val announcementTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "公告标题"
        AppLanguage.ENGLISH -> "Announcement Title"
        AppLanguage.ARABIC -> "عنوان الإعلان"
    }

    val announcementContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "公告内容"
        AppLanguage.ENGLISH -> "Announcement Content"
        AppLanguage.ARABIC -> "محتوى الإعلان"
    }

    val viewDetails: String get() = when (lang) {
        AppLanguage.CHINESE -> "查看详情"
        AppLanguage.ENGLISH -> "View Details"
        AppLanguage.ARABIC -> "عرض التفاصيل"
    }

    val announcementTriggerSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "触发机制"
        AppLanguage.ENGLISH -> "Trigger Settings"
        AppLanguage.ARABIC -> "إعدادات التشغيل"
    }

    val announcementTriggerOnLaunch: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动时显示"
        AppLanguage.ENGLISH -> "Show on Launch"
        AppLanguage.ARABIC -> "عرض عند التشغيل"
    }

    val announcementTriggerOnLaunchHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用启动时显示公告"
        AppLanguage.ENGLISH -> "Show announcement when app launches"
        AppLanguage.ARABIC -> "عرض الإعلان عند تشغيل التطبيق"
    }

    val announcementTriggerOnNoNetwork: String get() = when (lang) {
        AppLanguage.CHINESE -> "无网络时显示"
        AppLanguage.ENGLISH -> "Show When Offline"
        AppLanguage.ARABIC -> "عرض عند انقطاع الاتصال"
    }

    val announcementTriggerOnNoNetworkHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测到网络连接断开时显示公告"
        AppLanguage.ENGLISH -> "Show announcement when network connection is lost"
        AppLanguage.ARABIC -> "عرض الإعلان عند فقدان الاتصال بالشبكة"
    }

    val announcementTriggerInterval: String get() = when (lang) {
        AppLanguage.CHINESE -> "定时弹窗"
        AppLanguage.ENGLISH -> "Timed Popup"
        AppLanguage.ARABIC -> "نافذة منبثقة مؤقتة"
    }

    val announcementTriggerIntervalHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "每隔指定时间自动显示公告"
        AppLanguage.ENGLISH -> "Auto show announcement at specified intervals"
        AppLanguage.ARABIC -> "عرض الإعلان تلقائيًا على فترات محددة"
    }

    val announcementIntervalDisabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "禁用"
        AppLanguage.ENGLISH -> "Disabled"
        AppLanguage.ARABIC -> "معطل"
    }

    val announcementTriggerIntervalIncludeLaunch: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动时也立即弹窗一次"
        AppLanguage.ENGLISH -> "Also show immediately on launch"
        AppLanguage.ARABIC -> "عرض فورًا عند التشغيل أيضًا"
    }

    val announcementSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义应用启动公告弹窗"
        AppLanguage.ENGLISH -> "Customize app launch announcements"
        AppLanguage.ARABIC -> "تخصيص إعلانات تشغيل التطبيق"
    }

    val announcementContentSection: String get() = when (lang) {
        AppLanguage.CHINESE -> "公告内容"
        AppLanguage.ENGLISH -> "Content"
        AppLanguage.ARABIC -> "المحتوى"
    }

    val announcementLinkSection: String get() = when (lang) {
        AppLanguage.CHINESE -> "链接设置"
        AppLanguage.ENGLISH -> "Link Settings"
        AppLanguage.ARABIC -> "إعدادات الرابط"
    }

    val announcementTriggersActive: String get() = when (lang) {
        AppLanguage.CHINESE -> "个触发器已启用"
        AppLanguage.ENGLISH -> "triggers active"
        AppLanguage.ARABIC -> "مشغلات نشطة"
    }

    val announcementAdvancedOptions: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级选项"
        AppLanguage.ENGLISH -> "Advanced Options"
        AppLanguage.ARABIC -> "خيارات متقدمة"
    }

    val announcementRequireConfirmLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "需确认"
        AppLanguage.ENGLISH -> "Require Confirm"
        AppLanguage.ARABIC -> "تأكيد مطلوب"
    }

    val announcementRequireConfirmHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "用户必须点击确认按钮才能关闭公告"
        AppLanguage.ENGLISH -> "Users must click confirm to dismiss the announcement"
        AppLanguage.ARABIC -> "يجب على المستخدمين النقر على تأكيد لإغلاق الإعلان"
    }

    val announcementAllowNeverShowLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "可关闭"
        AppLanguage.ENGLISH -> "Allow Dismiss"
        AppLanguage.ARABIC -> "السماح بالإغلاق"
    }

    val announcementAllowNeverShowHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "允许用户选择不再显示此公告"
        AppLanguage.ENGLISH -> "Allow users to permanently dismiss this announcement"
        AppLanguage.ARABIC -> "السماح للمستخدمين بإغلاق هذا الإعلان نهائيًا"
    }

    val announcementEmojiHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "在公告弹窗中显示装饰性表情图标"
        AppLanguage.ENGLISH -> "Show decorative emoji icons in announcement popup"
        AppLanguage.ARABIC -> "عرض رموز تعبيرية زخرفية في الإعلان المنبثق"
    }

    val announcementAnimationHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "公告弹出时播放入场动画效果"
        AppLanguage.ENGLISH -> "Play entrance animation when announcement appears"
        AppLanguage.ARABIC -> "تشغيل رسوم متحركة عند ظهور الإعلان"
    }

    val announcementHighPriority: String get() = when (lang) {
        AppLanguage.CHINESE -> "高优先级"
        AppLanguage.ENGLISH -> "High Priority"
        AppLanguage.ARABIC -> "أولوية عالية"
    }

    val createFirstModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建第一个模块"
        AppLanguage.ENGLISH -> "Create first module"
        AppLanguage.ARABIC -> "إنشاء أول وحدة"
    }

    val createModuleHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建你的第一个扩展模块\n为你的应用添加自定义功能"
        AppLanguage.ENGLISH -> "Create your first extension module\nAdd custom features to your apps"
        AppLanguage.ARABIC -> "أنشئ أول وحدة إضافية\nأضف ميزات مخصصة لتطبيقاتك"
    }

    val createModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建模块"
        AppLanguage.ENGLISH -> "Create Module"
        AppLanguage.ARABIC -> "إنشاء وحدة"
    }

    val activationCodeExample: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持 4-16 位字母和数字"
        AppLanguage.ENGLISH -> "4-16 alphanumeric characters"
        AppLanguage.ARABIC -> "4-16 حرفًا أو رقمًا"
    }

    val activationCodeCopied: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活码已复制"
        AppLanguage.ENGLISH -> "Activation code copied"
        AppLanguage.ARABIC -> "تم نسخ رمز التفعيل"
    }

    val activationCodeType: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活码类型"
        AppLanguage.ENGLISH -> "Activation Code Type"
        AppLanguage.ARABIC -> "نوع رمز التفعيل"
    }

    val activationCodeBoundToOtherDevice: String get() = when (lang) {
        AppLanguage.CHINESE -> "此激活码已绑定到其他设备"
        AppLanguage.ENGLISH -> "This activation code is bound to another device"
        AppLanguage.ARABIC -> "رمز التفعيل هذا مرتبط بجهاز آخر"
    }

    val activationCodeExpired: String get() = when (lang) {
        AppLanguage.CHINESE -> "Activation code expired"
        AppLanguage.ENGLISH -> "Activation code expired"
        AppLanguage.ARABIC -> "انتهت صلاحية رمز التفعيل"
    }

    val activationCodeUsageExceeded: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活码使用次数已用完"
        AppLanguage.ENGLISH -> "Activation code usage exceeded"
        AppLanguage.ARABIC -> "تم تجاوز استخدام رمز التفعيل"
    }

    val inputAnnouncementTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入公告标题"
        AppLanguage.ENGLISH -> "Enter announcement title"
        AppLanguage.ARABIC -> "أدخل عنوان الإعلان"
    }

    val inputAnnouncementContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入公告内容..."
        AppLanguage.ENGLISH -> "Enter announcement content..."
        AppLanguage.ARABIC -> "أدخل محتوى الإعلان..."
    }

    val createProjectFolder: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建项目文件夹"
        AppLanguage.ENGLISH -> "Create Project Folder"
        AppLanguage.ARABIC -> "إنشاء مجلد المشروع"
    }

    val activationCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活码"
        AppLanguage.ENGLISH -> "Activation Code"
        AppLanguage.ARABIC -> "رمز التفعيل"
    }

    val inputActivationCodeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入激活码"
        AppLanguage.ENGLISH -> "Enter activation code"
        AppLanguage.ARABIC -> "أدخل رمز التفعيل"
    }

    val announcementTemplates: String get() = when (lang) {
        AppLanguage.CHINESE -> "公告模板：10种精美公告弹窗模板"
        AppLanguage.ENGLISH -> "Announcement templates: 10 beautiful announcement popup templates"
        AppLanguage.ARABIC -> "قوالب الإعلانات: 10 قوالب منبثقة جميلة للإعلانات"
    }

    val activationCodeAnnouncementAdBlock: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活码/公告/广告拦截"
        AppLanguage.ENGLISH -> "Activation code/Announcement/Ad blocking"
        AppLanguage.ARABIC -> "رمز التفعيل/الإعلانات/حظر الإعلانات"
    }

    val announcementAgreeAndContinue: String get() = when (lang) {
        AppLanguage.CHINESE -> "我已阅读并同意"
        AppLanguage.ENGLISH -> "I have read and agree"
        AppLanguage.ARABIC -> "لقد قرأت وأوافق"
    }

    val announcementNeverShow: String get() = when (lang) {
        AppLanguage.CHINESE -> "不再显示"
        AppLanguage.ENGLISH -> "Don't show again"
        AppLanguage.ARABIC -> "لا تظهر مرة أخرى"
    }

    val announcementPleaseConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "请勾选同意后继续"
        AppLanguage.ENGLISH -> "Please check the agreement to continue"
        AppLanguage.ARABIC -> "يرجى تحديد الموافقة للمتابعة"
    }

    val createGalleryApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体画廊"
        AppLanguage.ENGLISH -> "Media Gallery"
        AppLanguage.ARABIC -> "معرض الوسائط"
    }
    // ==================== Multi-site Aggregation App ====================
    val multiWebHeroTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "多站点聚合"
        AppLanguage.ENGLISH -> "Multi-Site Aggregator"
        AppLanguage.ARABIC -> "مجمع المواقع"
    }

    val multiWebHeroDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将多个网站合并为一个应用，支持标签页、卡片和信息流三种模式"
        AppLanguage.ENGLISH -> "Combine multiple websites into one app with Tabs, Cards, or Feed mode"
        AppLanguage.ARABIC -> "ادمج عدة مواقع في تطبيق واحد مع أوضاع التبويبات والبطاقات والخلاصات"
    }

    val multiWebDisplayMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示模式"
        AppLanguage.ENGLISH -> "Display Mode"
        AppLanguage.ARABIC -> "وضع العرض"
    }

    val multiWebModeTabs: String get() = when (lang) {
        AppLanguage.CHINESE -> "标签页"
        AppLanguage.ENGLISH -> "Tabs"
        AppLanguage.ARABIC -> "تبويبات"
    }

    val multiWebModeTabsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "底部标签栏快速切换站点"
        AppLanguage.ENGLISH -> "Bottom tab bar for quick switching"
        AppLanguage.ARABIC -> "شريط تبويبات سفلي للتبديل السريع"
    }

    val multiWebModeCards: String get() = when (lang) {
        AppLanguage.CHINESE -> "卡片"
        AppLanguage.ENGLISH -> "Cards"
        AppLanguage.ARABIC -> "بطاقات"
    }

    val multiWebModeCardsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "卡片首页，点击打开全屏浏览"
        AppLanguage.ENGLISH -> "Card grid home, tap to browse"
        AppLanguage.ARABIC -> "شبكة بطاقات, انقر للتصفح"
    }

    val multiWebModeFeed: String get() = when (lang) {
        AppLanguage.CHINESE -> "信息流"
        AppLanguage.ENGLISH -> "Feed"
        AppLanguage.ARABIC -> "خلاصات"
    }

    val multiWebModeFeedDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "类似 RSS 阅读器，聚合所有站点文章"
        AppLanguage.ENGLISH -> "RSS-like reader, aggregates articles"
        AppLanguage.ARABIC -> "قارئ مثل RSS, يجمع المقالات"
    }

    val multiWebAddSite: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加站点"
        AppLanguage.ENGLISH -> "Add Site"
        AppLanguage.ARABIC -> "إضافة موقع"
    }

    val multiWebSiteName: String get() = when (lang) {
        AppLanguage.CHINESE -> "站点名称"
        AppLanguage.ENGLISH -> "Site Name"
        AppLanguage.ARABIC -> "اسم الموقع"
    }

    val multiWebSiteUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "站点 URL"
        AppLanguage.ENGLISH -> "Site URL"
        AppLanguage.ARABIC -> "رابط الموقع"
    }

    val multiWebSiteEmoji: String get() = when (lang) {
        AppLanguage.CHINESE -> "图标 Emoji"
        AppLanguage.ENGLISH -> "Icon Emoji"
        AppLanguage.ARABIC -> "رمز الأيقونة"
    }

    val multiWebSiteCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类标签"
        AppLanguage.ENGLISH -> "Category"
        AppLanguage.ARABIC -> "التصنيف"
    }

    val multiWebCssSelector: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS 选择器（Feed 模式）"
        AppLanguage.ENGLISH -> "CSS Selector (Feed mode)"
        AppLanguage.ARABIC -> "محدد CSS (وضع الخلاصة)"
    }

    val multiWebCssSelectorHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "例如：article h2 a、.post-title a"
        AppLanguage.ENGLISH -> "e.g. article h2 a, .post-title a"
        AppLanguage.ARABIC -> "مثال: article h2 a, .post-title a"
    }

    val multiWebSiteList: String get() = when (lang) {
        AppLanguage.CHINESE -> "站点列表"
        AppLanguage.ENGLISH -> "Sites"
        AppLanguage.ARABIC -> "المواقع"
    }

    val multiWebNoSites: String get() = when (lang) {
        AppLanguage.CHINESE -> "尚未添加任何站点"
        AppLanguage.ENGLISH -> "No sites added yet"
        AppLanguage.ARABIC -> "لم تتم إضافة مواقع بعد"
    }

    val multiWebSiteCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个站点"
        AppLanguage.ENGLISH -> "%d sites"
        AppLanguage.ARABIC -> "%d مواقع"
    }

    val multiWebFeedTip: String get() = when (lang) {
        AppLanguage.CHINESE -> "💡 Feed 模式会使用 CSS 选择器从每个站点提取文章标题和链接。如果不配置选择器，将自动提取页面中的链接。"
        AppLanguage.ENGLISH -> "💡 Feed mode uses CSS selectors to extract article titles and links. Without a selector, links are auto-extracted."
        AppLanguage.ARABIC -> "💡 يستخدم وضع الخلاصة محددات CSS لاستخراج العناوين والروابط. بدون محدد، يتم استخراج الروابط تلقائياً."
    }

    val multiWebQuickAdd: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速添加"
        AppLanguage.ENGLISH -> "Quick Add"
        AppLanguage.ARABIC -> "إضافة سريعة"
    }

    val multiWebQuickAddHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "粘贴网址快速添加..."
        AppLanguage.ENGLISH -> "Paste URL to quick add..."
        AppLanguage.ARABIC -> "الصق الرابط للإضافة السريعة..."
    }

    val multiWebFetchingTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在获取网站信息..."
        AppLanguage.ENGLISH -> "Fetching site info..."
        AppLanguage.ARABIC -> "جارٍ جلب معلومات الموقع..."
    }

    val multiWebBatchImport: String get() = when (lang) {
        AppLanguage.CHINESE -> "批量导入"
        AppLanguage.ENGLISH -> "Batch Import"
        AppLanguage.ARABIC -> "استيراد مجمع"
    }

    val multiWebBatchHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "每行一个URL，支持格式：\nhttps://example.com\n名称|https://example.com\n🎮|游戏|https://games.com"
        AppLanguage.ENGLISH -> "One URL per line, formats:\nhttps://example.com\nName|https://example.com\n🎮|Games|https://games.com"
        AppLanguage.ARABIC -> "رابط واحد لكل سطر, الأشكال:\nhttps://example.com\nالاسم|https://example.com\n🎮|ألعاب|https://games.com"
    }

    val multiWebImportCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入 %d 个站点"
        AppLanguage.ENGLISH -> "Import %d sites"
        AppLanguage.ARABIC -> "استيراد %d مواقع"
    }

    val multiWebModeDrawer: String get() = when (lang) {
        AppLanguage.CHINESE -> "侧边栏"
        AppLanguage.ENGLISH -> "Drawer"
        AppLanguage.ARABIC -> "درج"
    }

    val multiWebModeDrawerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "侧边导航栏，滑动切换站点"
        AppLanguage.ENGLISH -> "Side navigation drawer, swipe to switch"
        AppLanguage.ARABIC -> "درج تنقل جانبي, اسحب للتبديل"
    }

    val multiWebEditSite: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑站点"
        AppLanguage.ENGLISH -> "Edit Site"
        AppLanguage.ARABIC -> "تعديل الموقع"
    }

    val multiWebDeleteSite: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除站点"
        AppLanguage.ENGLISH -> "Delete Site"
        AppLanguage.ARABIC -> "حذف الموقع"
    }

    val multiWebDisableSite: String get() = when (lang) {
        AppLanguage.CHINESE -> "禁用站点"
        AppLanguage.ENGLISH -> "Disable Site"
        AppLanguage.ARABIC -> "تعطيل الموقع"
    }

    val multiWebEnableSite: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用站点"
        AppLanguage.ENGLISH -> "Enable Site"
        AppLanguage.ARABIC -> "تمكين الموقع"
    }

    val multiWebMoveUp: String get() = when (lang) {
        AppLanguage.CHINESE -> "上移"
        AppLanguage.ENGLISH -> "Move Up"
        AppLanguage.ARABIC -> "نقل للأعلى"
    }

    val multiWebMoveDown: String get() = when (lang) {
        AppLanguage.CHINESE -> "下移"
        AppLanguage.ENGLISH -> "Move Down"
        AppLanguage.ARABIC -> "نقل للأسفل"
    }

    val multiWebPaste: String get() = when (lang) {
        AppLanguage.CHINESE -> "粘贴"
        AppLanguage.ENGLISH -> "Paste"
        AppLanguage.ARABIC -> "لصق"
    }

    val multiWebPreview: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览"
        AppLanguage.ENGLISH -> "Preview"
        AppLanguage.ARABIC -> "معاينة"
    }

    val multiWebImportSites: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入 %d 个"
        AppLanguage.ENGLISH -> "Import %d"
        AppLanguage.ARABIC -> "استيراد %d"
    }

    val multiWebEditList: String get() = when (lang) {
        AppLanguage.CHINESE -> "修改列表"
        AppLanguage.ENGLISH -> "Edit list"
        AppLanguage.ARABIC -> "تعديل القائمة"
    }

    val multiWebBatchImportHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "每行一个URL，从剪贴板粘贴多个链接"
        AppLanguage.ENGLISH -> "One URL per line, paste multiple links from clipboard"
        AppLanguage.ARABIC -> "رابط واحد لكل سطر، الصق عدة روابط من الحافظة"
    }
    // ==================== WordPress Strings ====================
    val wpDownloadDeps: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载 WordPress 依赖"
        AppLanguage.ENGLISH -> "Download WordPress Dependencies"
        AppLanguage.ARABIC -> "تنزيل متطلبات WordPress"
    }

    val wpDownloadDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "首次使用需要下载 PHP 解释器和 WordPress 核心文件（级 25MB）"
        AppLanguage.ENGLISH -> "First use requires downloading PHP interpreter and WordPress core (~25MB)"
        AppLanguage.ARABIC -> "يتطلب الاستخدام الأول تنزيل مترجم PHP ونواة WordPress (~25MB)"
    }

    val wpMirrorCN: String get() = when (lang) {
        AppLanguage.CHINESE -> "国内镜像"
        AppLanguage.ENGLISH -> "China Mirror"
        AppLanguage.ARABIC -> "مرآة الصين"
    }

    val wpMirrorGlobal: String get() = when (lang) {
        AppLanguage.CHINESE -> "国际源"
        AppLanguage.ENGLISH -> "Global Source"
        AppLanguage.ARABIC -> "مصدر عالمي"
    }

    val wpDownloading: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在下载"
        AppLanguage.ENGLISH -> "Downloading"
        AppLanguage.ARABIC -> "جارٍ التنزيل"
    }

    val wpExtracting: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在解压"
        AppLanguage.ENGLISH -> "Extracting"
        AppLanguage.ARABIC -> "جارٍ الاستخراج"
    }

    val wpDepsReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "依赖已就绪"
        AppLanguage.ENGLISH -> "Dependencies Ready"
        AppLanguage.ARABIC -> "المتطلبات جاهزة"
    }

    val wpSiteTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "站点标题"
        AppLanguage.ENGLISH -> "Site Title"
        AppLanguage.ARABIC -> "عنوان الموقع"
    }

    val wpAdminUser: String get() = when (lang) {
        AppLanguage.CHINESE -> "管理员用户名"
        AppLanguage.ENGLISH -> "Admin Username"
        AppLanguage.ARABIC -> "اسم المستخدم المسؤول"
    }

    val wpImportTheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入主题"
        AppLanguage.ENGLISH -> "Import Theme"
        AppLanguage.ARABIC -> "استيراد السمة"
    }

    val wpImportPlugin: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入插件"
        AppLanguage.ENGLISH -> "Import Plugin"
        AppLanguage.ARABIC -> "استيراد الإضافة"
    }

    val wpImportFull: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入完整 WordPress 压缩包"
        AppLanguage.ENGLISH -> "Import Full WordPress Package"
        AppLanguage.ARABIC -> "استيراد حزمة WordPress كاملة"
    }

    val wpStartingServer: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在启动 PHP 服务器..."
        AppLanguage.ENGLISH -> "Starting PHP server..."
        AppLanguage.ARABIC -> "جارٍ تشغيل خادم PHP..."
    }

    val wpServerError: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP 服务器启动失败"
        AppLanguage.ENGLISH -> "PHP server failed to start"
        AppLanguage.ARABIC -> "فشل تشغيل خادم PHP"
    }

    val wpClearCache: String get() = when (lang) {
        AppLanguage.CHINESE -> "清理 WordPress 缓存"
        AppLanguage.ENGLISH -> "Clear WordPress Cache"
        AppLanguage.ARABIC -> "مسح ذاكرة التخزين المؤقت WordPress"
    }

    val wpMirrorSource: String get() = when (lang) {
        AppLanguage.CHINESE -> "镜像源"
        AppLanguage.ENGLISH -> "Mirror Source"
        AppLanguage.ARABIC -> "مصدر المرآة"
    }

    val wpAutoDetect: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动检测"
        AppLanguage.ENGLISH -> "Auto Detect"
        AppLanguage.ARABIC -> "كشف تلقائي"
    }

    val wpCheckingDeps: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查依赖..."
        AppLanguage.ENGLISH -> "Checking dependencies..."
        AppLanguage.ARABIC -> "جارٍ فحص المتطلبات..."
    }

    val wpCreatingProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建 WordPress 项目..."
        AppLanguage.ENGLISH -> "Creating WordPress project..."
        AppLanguage.ARABIC -> "جارٍ إنشاء مشروع WordPress..."
    }

    val wpDownloadFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "依赖下载失败，请检查网络后重试"
        AppLanguage.ENGLISH -> "Dependency download failed, please check network and retry"
        AppLanguage.ARABIC -> "فشل تنزيل المتطلبات، يرجى التحقق من الشبكة والمحاولة مرة أخرى"
    }

    val wpProjectCreateFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress 项目创建失败"
        AppLanguage.ENGLISH -> "WordPress project creation failed"
        AppLanguage.ARABIC -> "فشل إنشاء مشروع WordPress"
    }

    val wpCreateTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建 WordPress 应用"
        AppLanguage.ENGLISH -> "Create WordPress App"
        AppLanguage.ARABIC -> "إنشاء تطبيق WordPress"
    }

    val wpSiteTitleHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入站点标题"
        AppLanguage.ENGLISH -> "Enter site title"
        AppLanguage.ARABIC -> "أدخل عنوان الموقع"
    }

    val wpAdminUserHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "管理员用户名（默认 admin）"
        AppLanguage.ENGLISH -> "Admin username (default: admin)"
        AppLanguage.ARABIC -> "اسم المستخدم المسؤول (افتراضي: admin)"
    }

    val wpBasicConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "基本配置"
        AppLanguage.ENGLISH -> "Basic Configuration"
        AppLanguage.ARABIC -> "الإعدادات الأساسية"
    }

    val wpImportProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入 WordPress 项目"
        AppLanguage.ENGLISH -> "Import WordPress Project"
        AppLanguage.ARABIC -> "استيراد مشروع WordPress"
    }

    val wpImportProjectDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择完整的 WordPress 压缩包（.zip），包含主题和插件"
        AppLanguage.ENGLISH -> "Select a complete WordPress archive (.zip) with themes and plugins"
        AppLanguage.ARABIC -> "حدد أرشيف WordPress كامل (.zip) مع السمات والإضافات"
    }

    val wpOrCreateNew: String get() = when (lang) {
        AppLanguage.CHINESE -> "或者创建全新站点"
        AppLanguage.ENGLISH -> "Or create a new site"
        AppLanguage.ARABIC -> "أو أنشئ موقعًا جديدًا"
    }

    val wpCreateNewSite: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建新站点"
        AppLanguage.ENGLISH -> "Create New Site"
        AppLanguage.ARABIC -> "إنشاء موقع جديد"
    }

    val wpCreateNewSiteDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用默认 WordPress 核心创建空白站点"
        AppLanguage.ENGLISH -> "Create blank site with default WordPress core"
        AppLanguage.ARABIC -> "إنشاء موقع فارغ باستخدام نواة WordPress الافتراضية"
    }

    val wpSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress 设置"
        AppLanguage.ENGLISH -> "WordPress Settings"
        AppLanguage.ARABIC -> "إعدادات WordPress"
    }

    val wpCacheSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "缓存占用"
        AppLanguage.ENGLISH -> "Cache Size"
        AppLanguage.ARABIC -> "حجم ذاكرة التخزين المؤقت"
    }

    val wpCacheCleared: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress 缓存已清理"
        AppLanguage.ENGLISH -> "WordPress cache cleared"
        AppLanguage.ARABIC -> "تم مسح ذاكرة التخزين المؤقت WordPress"
    }

    val wpLandscapeMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "横屏模式"
        AppLanguage.ENGLISH -> "Landscape Mode"
        AppLanguage.ARABIC -> "وضع أفقي"
    }

    val wpProjectReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress 项目已就绪"
        AppLanguage.ENGLISH -> "WordPress project ready"
        AppLanguage.ARABIC -> "مشروع WordPress جاهز"
    }

    val wpImportSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress 项目导入成功"
        AppLanguage.ENGLISH -> "WordPress project imported successfully"
        AppLanguage.ARABIC -> "تم استيراد مشروع WordPress بنجاح"
    }
    // ==================== Build Environment ====================
    val buildEnvironment: String get() = when (lang) {
        AppLanguage.CHINESE -> "构建环境"
        AppLanguage.ENGLISH -> "Build Environment"
        AppLanguage.ARABIC -> "بيئة البناء"
    }

    val envReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "环境就绪"
        AppLanguage.ENGLISH -> "Environment Ready"
        AppLanguage.ARABIC -> "البيئة جاهزة"
    }

    val envNotInstalled: String get() = when (lang) {
        AppLanguage.CHINESE -> "可以使用"
        AppLanguage.ENGLISH -> "Available"
        AppLanguage.ARABIC -> "متاح"
    }

    val envDownloading: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载中"
        AppLanguage.ENGLISH -> "Downloading"
        AppLanguage.ARABIC -> "جاري التحميل"
    }

    val envInstalling: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装中"
        AppLanguage.ENGLISH -> "Installing"
        AppLanguage.ARABIC -> "جاري التثبيت"
    }

    val canBuildFrontend: String get() = when (lang) {
        AppLanguage.CHINESE -> "可以构建前端项目"
        AppLanguage.ENGLISH -> "Can build frontend projects"
        AppLanguage.ARABIC -> "يمكن بناء مشاريع الواجهة الأمامية"
    }

    val builtInPackagerReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "内置打包器已就绪"
        AppLanguage.ENGLISH -> "Built-in packager ready"
        AppLanguage.ARABIC -> "أداة التعبئة المدمجة جاهزة"
    }

    val installAdvancedBuildTool: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装高级构建工具 (esbuild)"
        AppLanguage.ENGLISH -> "Install Advanced Build Tool (esbuild)"
        AppLanguage.ARABIC -> "تثبيت أداة البناء المتقدمة (esbuild)"
    }

    val optionalEsbuildHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "可选：安装 esbuild 可获得更好的构建性能"
        AppLanguage.ENGLISH -> "Optional: Install esbuild for better build performance"
        AppLanguage.ARABIC -> "اختياري: تثبيت esbuild للحصول على أداء بناء أفضل"
    }

    val buildTools: String get() = when (lang) {
        AppLanguage.CHINESE -> "构建工具"
        AppLanguage.ENGLISH -> "Build Tools"
        AppLanguage.ARABIC -> "أدوات البناء"
    }

    val builtInPackager: String get() = when (lang) {
        AppLanguage.CHINESE -> "内置打包器"
        AppLanguage.ENGLISH -> "Built-in Packager"
        AppLanguage.ARABIC -> "أداة التعبئة المدمجة"
    }

    val pureKotlinImpl: String get() = when (lang) {
        AppLanguage.CHINESE -> "纯 Kotlin 实现，无需外部依赖"
        AppLanguage.ENGLISH -> "Pure Kotlin implementation, no external dependencies"
        AppLanguage.ARABIC -> "تنفيذ Kotlin خالص، بدون تبعيات خارجية"
    }

    val highPerfBuildTool: String get() = when (lang) {
        AppLanguage.CHINESE -> "高性能构建工具"
        AppLanguage.ENGLISH -> "High-performance build tool"
        AppLanguage.ARABIC -> "أداة بناء عالية الأداء"
    }

    val installed: String get() = when (lang) {
        AppLanguage.CHINESE -> "已安装"
        AppLanguage.ENGLISH -> "Installed"
        AppLanguage.ARABIC -> "مثبت"
    }

    val notInstalled: String get() = when (lang) {
        AppLanguage.CHINESE -> "未安装"
        AppLanguage.ENGLISH -> "Not Installed"
        AppLanguage.ARABIC -> "غير مثبت"
    }

    val ready: String get() = when (lang) {
        AppLanguage.CHINESE -> "已就绪"
        AppLanguage.ENGLISH -> "Ready"
        AppLanguage.ARABIC -> "جاهز"
    }

    val storageUsage: String get() = when (lang) {
        AppLanguage.CHINESE -> "存储使用"
        AppLanguage.ENGLISH -> "Storage Usage"
        AppLanguage.ARABIC -> "استخدام التخزين"
    }

    val cache: String get() = when (lang) {
        AppLanguage.CHINESE -> "缓存"
        AppLanguage.ENGLISH -> "Cache"
        AppLanguage.ARABIC -> "ذاكرة التخزين المؤقت"
    }

    val supportedFeatures: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持的功能"
        AppLanguage.ENGLISH -> "Supported Features"
        AppLanguage.ARABIC -> "الميزات المدعومة"
    }

    val techDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "技术说明"
        AppLanguage.ENGLISH -> "Technical Description"
        AppLanguage.ARABIC -> "الوصف التقني"
    }

    val resetEnvironment: String get() = when (lang) {
        AppLanguage.CHINESE -> "重置环境"
        AppLanguage.ENGLISH -> "Reset Environment"
        AppLanguage.ARABIC -> "إعادة تعيين البيئة"
    }

    val resetEnvConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "这将删除已下载的构建工具。确定要继续吗？"
        AppLanguage.ENGLISH -> "This will delete downloaded build tools. Are you sure?"
        AppLanguage.ARABIC -> "سيؤدي هذا إلى حذف أدوات البناء المحملة. هل أنت متأكد؟"
    }

    val clearCacheTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "清理缓存"
        AppLanguage.ENGLISH -> "Clear Cache"
        AppLanguage.ARABIC -> "مسح ذاكرة التخزين المؤقت"
    }

    val clearCacheConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "这将清理构建缓存和临时文件。"
        AppLanguage.ENGLISH -> "This will clear build cache and temporary files."
        AppLanguage.ARABIC -> "سيؤدي هذا إلى مسح ذاكرة التخزين المؤقت للبناء والملفات المؤقتة."
    }

    val clean: String get() = when (lang) {
        AppLanguage.CHINESE -> "清理"
        AppLanguage.ENGLISH -> "Clean"
        AppLanguage.ARABIC -> "تنظيف"
    }
    // ==================== Frontend Project Page ====================
    val selectProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择项目"
        AppLanguage.ENGLISH -> "Select Project"
        AppLanguage.ARABIC -> "اختيار المشروع"
    }

    val selectProjectFolder: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择项目文件夹"
        AppLanguage.ENGLISH -> "Select Project Folder"
        AppLanguage.ARABIC -> "اختيار مجلد المشروع"
    }

    val selectProjectHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择项目根目录或构建输出目录（dist/build）"
        AppLanguage.ENGLISH -> "Select project root or build output directory (dist/build)"
        AppLanguage.ARABIC -> "اختر جذر المشروع أو دليل إخراج البناء (dist/build)"
    }

    val projectAnalysis: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目分析"
        AppLanguage.ENGLISH -> "Project Analysis"
        AppLanguage.ARABIC -> "تحليل المشروع"
    }

    val framework: String get() = when (lang) {
        AppLanguage.CHINESE -> "框架"
        AppLanguage.ENGLISH -> "Framework"
        AppLanguage.ARABIC -> "إطار العمل"
    }

    val version: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本"
        AppLanguage.ENGLISH -> "Version"
        AppLanguage.ARABIC -> "الإصدار"
    }

    val packageManager: String get() = when (lang) {
        AppLanguage.CHINESE -> "包管理器"
        AppLanguage.ENGLISH -> "Package Manager"
        AppLanguage.ARABIC -> "مدير الحزم"
    }

    val dependencyCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "依赖数量"
        AppLanguage.ENGLISH -> "Dependency Count"
        AppLanguage.ARABIC -> "عدد التبعيات"
    }

    val outputDir: String get() = when (lang) {
        AppLanguage.CHINESE -> "输出目录"
        AppLanguage.ENGLISH -> "Output Directory"
        AppLanguage.ARABIC -> "دليل الإخراج"
    }

    val importProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入项目"
        AppLanguage.ENGLISH -> "Import Project"
        AppLanguage.ARABIC -> "استيراد المشروع"
    }

    val reimportProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "重新导入项目"
        AppLanguage.ENGLISH -> "Re-import Project"
        AppLanguage.ARABIC -> "إعادة استيراد المشروع"
    }

    val buildProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "构建项目"
        AppLanguage.ENGLISH -> "Build Project"
        AppLanguage.ARABIC -> "بناء المشروع"
    }

    val rebuildProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "重新构建项目"
        AppLanguage.ENGLISH -> "Rebuild Project"
        AppLanguage.ARABIC -> "إعادة بناء المشروع"
    }

    val scanningProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "扫描项目中..."
        AppLanguage.ENGLISH -> "Scanning project..."
        AppLanguage.ARABIC -> "جاري فحص المشروع..."
    }

    val importing: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入中"
        AppLanguage.ENGLISH -> "Importing"
        AppLanguage.ARABIC -> "جاري الاستيراد"
    }

    val checkingEnv: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查环境..."
        AppLanguage.ENGLISH -> "Checking environment..."
        AppLanguage.ARABIC -> "جاري فحص البيئة..."
    }

    val copyingProjectFiles: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制项目文件"
        AppLanguage.ENGLISH -> "Copying project files"
        AppLanguage.ARABIC -> "نسخ ملفات المشروع"
    }

    val installingDeps: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装依赖"
        AppLanguage.ENGLISH -> "Installing dependencies"
        AppLanguage.ARABIC -> "تثبيت التبعيات"
    }

    val building: String get() = when (lang) {
        AppLanguage.CHINESE -> "构建中"
        AppLanguage.ENGLISH -> "Building"
        AppLanguage.ARABIC -> "جاري البناء"
    }

    val processingOutput: String get() = when (lang) {
        AppLanguage.CHINESE -> "处理构建产物..."
        AppLanguage.ENGLISH -> "Processing build output..."
        AppLanguage.ARABIC -> "معالجة مخرجات البناء..."
    }

    val completed: String get() = when (lang) {
        AppLanguage.CHINESE -> "完成"
        AppLanguage.ENGLISH -> "Completed"
        AppLanguage.ARABIC -> "مكتمل"
    }

    val failed: String get() = when (lang) {
        AppLanguage.CHINESE -> "失败"
        AppLanguage.ENGLISH -> "Failed"
        AppLanguage.ARABIC -> "فشل"
    }

    val totalFiles: String get() = when (lang) {
        AppLanguage.CHINESE -> "共 %d 个文件"
        AppLanguage.ENGLISH -> "%d files total"
        AppLanguage.ARABIC -> "إجمالي %d ملفات"
    }

    val logs: String get() = when (lang) {
        AppLanguage.CHINESE -> "日志"
        AppLanguage.ENGLISH -> "Logs"
        AppLanguage.ARABIC -> "السجلات"
    }

    val importLogs: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入日志"
        AppLanguage.ENGLISH -> "Import Logs"
        AppLanguage.ARABIC -> "سجلات الاستيراد"
    }

    val importFrontendProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入前端项目"
        AppLanguage.ENGLISH -> "Import Frontend Project"
        AppLanguage.ARABIC -> "استيراد مشروع الواجهة الأمامية"
    }

    val supportVueReactVite: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持 Vue、React、Vite 等已构建的项目"
        AppLanguage.ENGLISH -> "Supports built Vue, React, Vite projects"
        AppLanguage.ARABIC -> "يدعم مشاريع Vue و React و Vite المبنية"
    }

    val usageSteps: String get() = when (lang) {
        AppLanguage.CHINESE -> "📋 使用步骤"
        AppLanguage.ENGLISH -> "📋 Usage Steps"
        AppLanguage.ARABIC -> "📋 خطوات الاستخدام"
    }

    val usageStepsContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "1. 在电脑上构建项目：npm run build\n2. 将构建输出（dist/build）复制到手机\n3. 选择项目文件夹导入"
        AppLanguage.ENGLISH -> "1. Build project on computer: npm run build\n2. Copy build output (dist/build) to phone\n3. Select project folder to import"
        AppLanguage.ARABIC -> "1. بناء المشروع على الكمبيوتر: npm run build\n2. نسخ مخرجات البناء (dist/build) إلى الهاتف\n3. اختيار مجلد المشروع للاستيراد"
    }

    val builtInEngineReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "内置构建引擎已就绪。支持导入已构建的项目，或使用 esbuild 进行简单构建。推荐在电脑上完成复杂项目的构建。"
        AppLanguage.ENGLISH -> "Built-in build engine ready. Supports importing built projects or simple builds with esbuild. Complex projects are recommended to be built on computer."
        AppLanguage.ARABIC -> "محرك البناء المدمج جاهز. يدعم استيراد المشاريع المبنية أو البناء البسيط باستخدام esbuild. يُنصح ببناء المشاريع المعقدة على الكمبيوتر."
    }
    // ==================== HTML App Page ====================
    val selectFiles: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择文件"
        AppLanguage.ENGLISH -> "Select Files"
        AppLanguage.ARABIC -> "اختيار الملفات"
    }

    val selectFilesHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "分别选择HTML、CSS、JS文件（CSS和JS为可选）"
        AppLanguage.ENGLISH -> "Select HTML, CSS, JS files separately (CSS and JS are optional)"
        AppLanguage.ARABIC -> "اختر ملفات HTML و CSS و JS بشكل منفصل (CSS و JS اختياريان)"
    }

    val htmlFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML 文件"
        AppLanguage.ENGLISH -> "HTML File"
        AppLanguage.ARABIC -> "ملف HTML"
    }

    val cssFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS 样式文件"
        AppLanguage.ENGLISH -> "CSS Style File"
        AppLanguage.ARABIC -> "ملف أنماط CSS"
    }

    val jsFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "JavaScript 脚本"
        AppLanguage.ENGLISH -> "JavaScript Script"
        AppLanguage.ARABIC -> "سكريبت JavaScript"
    }

    val enableJavaScript: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用 JavaScript"
        AppLanguage.ENGLISH -> "Enable JavaScript"
        AppLanguage.ARABIC -> "تفعيل JavaScript"
    }

    val enableJsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "允许HTML中的JavaScript代码执行"
        AppLanguage.ENGLISH -> "Allow JavaScript code execution in HTML"
        AppLanguage.ARABIC -> "السماح بتنفيذ كود JavaScript في HTML"
    }

    val enableLocalStorage: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用本地存储"
        AppLanguage.ENGLISH -> "Enable Local Storage"
        AppLanguage.ARABIC -> "تفعيل التخزين المحلي"
    }

    val enableLocalStorageHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "允许使用 localStorage 保存数据"
        AppLanguage.ENGLISH -> "Allow using localStorage to save data"
        AppLanguage.ARABIC -> "السماح باستخدام localStorage لحفظ البيانات"
    }

    val landscapeModeLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "横屏模式"
        AppLanguage.ENGLISH -> "Landscape Mode"
        AppLanguage.ARABIC -> "الوضع الأفقي"
    }

    val orientationModeLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏幕方向"
        AppLanguage.ENGLISH -> "Screen Orientation"
        AppLanguage.ARABIC -> "اتجاه الشاشة"
    }

    val orientationModeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "设置应用的屏幕旋转方式"
        AppLanguage.ENGLISH -> "Control how the app handles screen rotation"
        AppLanguage.ARABIC -> "التحكم في كيفية تعامل التطبيق مع دوران الشاشة"
    }

    val orientationPortrait: String get() = when (lang) {
        AppLanguage.CHINESE -> "锁定竖屏"
        AppLanguage.ENGLISH -> "Portrait"
        AppLanguage.ARABIC -> "عمودي"
    }

    val orientationLandscape: String get() = when (lang) {
        AppLanguage.CHINESE -> "锁定横屏"
        AppLanguage.ENGLISH -> "Landscape"
        AppLanguage.ARABIC -> "أفقي"
    }

    val orientationAuto: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动旋转"
        AppLanguage.ENGLISH -> "Auto-rotate"
        AppLanguage.ARABIC -> "تدوير تلقائي"
    }

    val orientationAutoHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "跟随设备重力感应自动旋转方向，推荐平板设备使用"
        AppLanguage.ENGLISH -> "Automatically rotate based on device orientation sensor, recommended for tablets"
        AppLanguage.ARABIC -> "التدوير تلقائيًا بناءً على مستشعر اتجاه الجهاز، موصى به للأجهزة اللوحية"
    }

    val orientationBasicLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "基础模式"
        AppLanguage.ENGLISH -> "Basic"
        AppLanguage.ARABIC -> "أساسي"
    }

    val orientationAdvancedLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级方向选项"
        AppLanguage.ENGLISH -> "Advanced Orientation"
        AppLanguage.ARABIC -> "اتجاه متقدم"
    }

    val orientationReversedLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "反向锁定"
        AppLanguage.ENGLISH -> "Reversed Lock"
        AppLanguage.ARABIC -> "قفل معكوس"
    }

    val orientationSensorLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "感应旋转"
        AppLanguage.ENGLISH -> "Sensor Rotation"
        AppLanguage.ARABIC -> "دوران بالمستشعر"
    }

    val orientationLandscapeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "锁定正向横屏，适合视频和游戏"
        AppLanguage.ENGLISH -> "Lock in landscape, ideal for video & games"
        AppLanguage.ARABIC -> "قفل أفقي، مثالي للفيديو والألعاب"
    }

    val orientationAutoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "全方向自由旋转，跟随设备重力感应"
        AppLanguage.ENGLISH -> "Free rotation in all directions, follows device sensor"
        AppLanguage.ARABIC -> "دوران حر في جميع الاتجاهات، يتبع مستشعر الجهاز"
    }

    val orientationReversePortrait: String get() = when (lang) {
        AppLanguage.CHINESE -> "反向竖屏"
        AppLanguage.ENGLISH -> "Reverse Portrait"
        AppLanguage.ARABIC -> "عمودي معكوس"
    }

    val orientationReversePortraitDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "锁定倒置竖屏，底部朝上"
        AppLanguage.ENGLISH -> "Lock upside-down portrait, bottom facing up"
        AppLanguage.ARABIC -> "قفل عمودي مقلوب، الجزء السفلي لأعلى"
    }

    val orientationReverseLandscape: String get() = when (lang) {
        AppLanguage.CHINESE -> "反向横屏"
        AppLanguage.ENGLISH -> "Reverse Landscape"
        AppLanguage.ARABIC -> "أفقي معكوس"
    }

    val orientationReverseLandscapeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "锁定反向横屏，与正向横屏镜像"
        AppLanguage.ENGLISH -> "Lock reverse landscape, mirrored from standard"
        AppLanguage.ARABIC -> "قفل أفقي معكوس، انعكاس من الوضع القياسي"
    }

    val orientationSensorPortrait: String get() = when (lang) {
        AppLanguage.CHINESE -> "感应竖屏"
        AppLanguage.ENGLISH -> "Sensor Portrait"
        AppLanguage.ARABIC -> "عمودي بالمستشعر"
    }

    val orientationSensorPortraitDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "竖屏范围内自动切换正向/反向"
        AppLanguage.ENGLISH -> "Auto-switch between portrait & reverse portrait"
        AppLanguage.ARABIC -> "التبديل تلقائيًا بين العمودي والعمودي المعكوس"
    }

    val orientationSensorPortraitHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "在正向竖屏和反向竖屏之间根据重力感应自动切换，适合需要倒置使用的场景"
        AppLanguage.ENGLISH -> "Automatically switch between upright and upside-down portrait based on sensor, useful for inverted setups"
        AppLanguage.ARABIC -> "التبديل تلقائيًا بين الوضع العمودي والمقلوب بناءً على المستشعر"
    }

    val orientationSensorLandscape: String get() = when (lang) {
        AppLanguage.CHINESE -> "感应横屏"
        AppLanguage.ENGLISH -> "Sensor Landscape"
        AppLanguage.ARABIC -> "أفقي بالمستشعر"
    }

    val orientationSensorLandscapeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "横屏范围内自动切换正向/反向"
        AppLanguage.ENGLISH -> "Auto-switch between landscape & reverse landscape"
        AppLanguage.ARABIC -> "التبديل تلقائيًا بين الأفقي والأفقي المعكوس"
    }

    val orientationSensorLandscapeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "在正向横屏和反向横屏之间根据重力感应自动切换，适合平板游戏等横屏场景"
        AppLanguage.ENGLISH -> "Automatically switch between standard and reverse landscape based on sensor, ideal for tablet gaming"
        AppLanguage.ARABIC -> "التبديل تلقائيًا بين الأفقي القياسي والمعكوس بناءً على المستشعر، مثالي لألعاب الأجهزة اللوحية"
    }

    val keepScreenOnLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "保持屏幕常亮"
        AppLanguage.ENGLISH -> "Keep Screen On"
        AppLanguage.ARABIC -> "إبقاء الشاشة مضاءة"
    }

    val keepScreenOnHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "防止屏幕自动熄灭，适用于 code-server 等长时间使用场景"
        AppLanguage.ENGLISH -> "Prevent screen from dimming, ideal for code-server and long-use scenarios"
        AppLanguage.ARABIC -> "منع إطفاء الشاشة تلقائيًا، مناسب لسيناريوهات الاستخدام الطويل"
    }

    val screenAwakeModeLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "常亮模式"
        AppLanguage.ENGLISH -> "Awake Mode"
        AppLanguage.ARABIC -> "وضع الإيقاظ"
    }

    val screenAwakeOff: String get() = when (lang) {
        AppLanguage.CHINESE -> "关闭"
        AppLanguage.ENGLISH -> "Off"
        AppLanguage.ARABIC -> "إيقاف"
    }

    val screenAwakeOffDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "跟随系统自动息屏"
        AppLanguage.ENGLISH -> "Follow system screen timeout"
        AppLanguage.ARABIC -> "اتبع مهلة شاشة النظام"
    }

    val screenAwakeAlways: String get() = when (lang) {
        AppLanguage.CHINESE -> "始终常亮"
        AppLanguage.ENGLISH -> "Always On"
        AppLanguage.ARABIC -> "تشغيل دائم"
    }

    val screenAwakeAlwaysDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏幕保持常亮，适合 code-server、数字相框"
        AppLanguage.ENGLISH -> "Screen stays on, ideal for code-server & digital frames"
        AppLanguage.ARABIC -> "تبقى الشاشة مضاءة، مثالي للخوادم والإطارات الرقمية"
    }

    val screenAwakeTimed: String get() = when (lang) {
        AppLanguage.CHINESE -> "定时常亮"
        AppLanguage.ENGLISH -> "Timed"
        AppLanguage.ARABIC -> "مؤقت"
    }

    val screenAwakeTimedDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "在指定时间后恢复系统息屏，节省电量"
        AppLanguage.ENGLISH -> "Revert to system timeout after set duration, saves battery"
        AppLanguage.ARABIC -> "العودة إلى مهلة النظام بعد المدة المحددة، يوفر البطارية"
    }

    val screenAwakeTimeoutLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "常亮时长"
        AppLanguage.ENGLISH -> "Duration"
        AppLanguage.ARABIC -> "المدة"
    }

    val screenBrightnessLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏幕亮度"
        AppLanguage.ENGLISH -> "Screen Brightness"
        AppLanguage.ARABIC -> "سطوع الشاشة"
    }

    val screenBrightnessAuto: String get() = when (lang) {
        AppLanguage.CHINESE -> "跟随系统"
        AppLanguage.ENGLISH -> "System Default"
        AppLanguage.ARABIC -> "افتراضي النظام"
    }

    val screenBrightnessManual: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义亮度"
        AppLanguage.ENGLISH -> "Custom"
        AppLanguage.ARABIC -> "مخصص"
    }

    val screenAwakeBatteryWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "始终常亮模式会增加电量消耗，建议仅在插电或对接使用场景下开启"
        AppLanguage.ENGLISH -> "Always-on mode increases battery drain. Recommended only when plugged in or for kiosk use."
        AppLanguage.ARABIC -> "وضع التشغيل الدائم يزيد استهلاك البطارية. يُوصى به فقط عند التوصيل بالشاحن."
    }

    val screenAwakeTimedHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "定时常亮兼顾使用体验与电量节省，超时后将自动恢复系统息屏策略"
        AppLanguage.ENGLISH -> "Timed mode balances usability and battery life. Screen will auto-sleep after the timeout."
        AppLanguage.ARABIC -> "الوضع المؤقت يوازن بين سهولة الاستخدام وعمر البطارية. ستنام الشاشة تلقائيًا بعد المهلة."
    }

    val keyboardAdjustModeLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "键盘调整模式"
        AppLanguage.ENGLISH -> "Keyboard Adjust Mode"
        AppLanguage.ARABIC -> "وضع ضبط لوحة المفاتيح"
    }

    val keyboardAdjustModeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "控制软键盘弹出时的页面行为。如遇到键盘遮挡输入框或动画卡顿，可尝试切换此选项"
        AppLanguage.ENGLISH -> "Control page behavior when soft keyboard appears. Switch if input fields are hidden or animation is laggy"
        AppLanguage.ARABIC -> "التحكم في سلوك الصفحة عند ظهور لوحة المفاتيح الناعمة. قم بالتبديل إذا كانت حقول الإدخال مخفية أو الرسوم متحركة بطيئة"
    }

    val keyboardAdjustResize: String get() = when (lang) {
        AppLanguage.CHINESE -> "推起页面"
        AppLanguage.ENGLISH -> "Resize Content"
        AppLanguage.ARABIC -> "تغيير حجم المحتوى"
    }

    val keyboardAdjustResizeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "键盘会向上推页面，确保输入框始终可见（在某些设备上可能有轻微卡顿）"
        AppLanguage.ENGLISH -> "Keyboard pushes page up, ensuring input fields remain visible (may be slightly laggy on some devices)"
        AppLanguage.ARABIC -> "تدفع لوحة المفاتيح الصفحة لأعلى، مما يضمن بقاء حقول الإدخال مرئية (قد تكون بطيئة قليلاً على بعض الأجهزة)"
    }

    val keyboardAdjustNothing: String get() = when (lang) {
        AppLanguage.CHINESE -> "覆盖页面"
        AppLanguage.ENGLISH -> "Overlay Content"
        AppLanguage.ARABIC -> "تراكب المحتوى"
    }

    val keyboardAdjustNothingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "键盘直接覆盖页面，无布局调整动画（更流畅，但可能遮挡输入框）"
        AppLanguage.ENGLISH -> "Keyboard overlays page directly without layout animation (smoother, but may hide input fields)"
        AppLanguage.ARABIC -> "تتراكب لوحة المفاتيح على الصفحة مباشرة بدون رسوم متحركة للتخطيط (أكثر سلاسة، لكن قد تخفي حقول الإدخال)"
    }

    val showFloatingBackButtonLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮返回按钮"
        AppLanguage.ENGLISH -> "Floating Back Button"
        AppLanguage.ARABIC -> "زر العودة العائم"
    }

    val showFloatingBackButtonHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "全屏模式下在左上角显示悬浮返回按钮。如果与网页 UI 冲突，可关闭此选项"
        AppLanguage.ENGLISH -> "Show a floating back button at top-left in fullscreen mode. Disable if it conflicts with web page UI"
        AppLanguage.ARABIC -> "عرض زر عودة عائم في الزاوية العلوية اليسرى في وضع ملء الشاشة. قم بتعطيله إذا تعارض مع واجهة صفحة الويب"
    }

    val blockSystemNavigationGestureLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏蔽系统导航手势"
        AppLanguage.ENGLISH -> "Block System Navigation Gesture"
        AppLanguage.ARABIC -> "حظر إيماءة التنقل في النظام"
    }

    val blockSystemNavigationGestureHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止屏幕边缘滑动手势（返回键效果），防止与网页内手势冲突。全屏模式下生效，默认关闭"
        AppLanguage.ENGLISH -> "Block edge swipe gestures (back navigation) to prevent conflicts with web page gestures. Effective in fullscreen mode, disabled by default"
        AppLanguage.ARABIC -> "حظر إيماءات السحب من الحافة (التنقل للخلف) لمنع التعارض مع إيماءات صفحة الويب. فعال في وضع ملء الشاشة، معطل افتراضياً"
    }

    val landscapeModeHintHtml: String get() = when (lang) {
        AppLanguage.CHINESE -> "以横屏方向显示应用内容"
        AppLanguage.ENGLISH -> "Display app content in landscape orientation"
        AppLanguage.ARABIC -> "عرض محتوى التطبيق بالاتجاه الأفقي"
    }

    val projectIssuesDetected: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测到项目问题"
        AppLanguage.ENGLISH -> "Project issues detected"
        AppLanguage.ARABIC -> "تم اكتشاف مشاكل في المشروع"
    }

    val errorsCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个错误"
        AppLanguage.ENGLISH -> "%d errors"
        AppLanguage.ARABIC -> "%d أخطاء"
    }

    val warningsCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个警告"
        AppLanguage.ENGLISH -> "%d warnings"
        AppLanguage.ARABIC -> "%d تحذيرات"
    }

    val autoFixHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用会自动修复路径问题并内联CSS/JS，但建议查看详情确认"
        AppLanguage.ENGLISH -> "App will auto-fix path issues and inline CSS/JS, but please review details"
        AppLanguage.ARABIC -> "سيقوم التطبيق بإصلاح مشاكل المسار تلقائيًا ودمج CSS/JS، لكن يُرجى مراجعة التفاصيل"
    }

    val viewAnalysisResult: String get() = when (lang) {
        AppLanguage.CHINESE -> "查看分析结果"
        AppLanguage.ENGLISH -> "View Analysis Result"
        AppLanguage.ARABIC -> "عرض نتيجة التحليل"
    }

    val htmlAppTip: String get() = when (lang) {
        AppLanguage.CHINESE -> "提示：HTML文件为必选，CSS和JS文件为可选。如果你的HTML文件中引用了CSS或JS，请分别选择对应的文件。"
        AppLanguage.ENGLISH -> "Tip: HTML file is required, CSS and JS files are optional. If your HTML references CSS or JS, please select the corresponding files."
        AppLanguage.ARABIC -> "تلميح: ملف HTML مطلوب، ملفات CSS و JS اختيارية. إذا كان HTML يشير إلى CSS أو JS، يرجى اختيار الملفات المقابلة."
    }

    val featureTip: String get() = when (lang) {
        AppLanguage.CHINESE -> "💡 激活码验证、背景音乐等功能可在创建项目后，通过项目管理界面点击「编辑」进行添加和配置。"
        AppLanguage.ENGLISH -> "💡 Features like activation code and background music can be added via 'Edit' in project management after creation."
        AppLanguage.ARABIC -> "💡 يمكن إضافة ميزات مثل رمز التفعيل والموسيقى الخلفية عبر 'تعديل' في إدارة المشروع بعد الإنشاء."
    }

    val aboutFileReference: String get() = when (lang) {
        AppLanguage.CHINESE -> "关于文件引用"
        AppLanguage.ENGLISH -> "About File References"
        AppLanguage.ARABIC -> "حول مراجع الملفات"
    }

    val fileReferenceHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "• 应用会自动将CSS和JS内联到HTML中\n• 绝对路径（如 /css/style.css）会自动转换\n• 建议使用相对路径（如 ./style.css）"
        AppLanguage.ENGLISH -> "• App will auto-inline CSS and JS into HTML\n• Absolute paths (like /css/style.css) will be auto-converted\n• Relative paths (like ./style.css) are recommended"
        AppLanguage.ARABIC -> "• سيقوم التطبيق بدمج CSS و JS تلقائيًا في HTML\n• سيتم تحويل المسارات المطلقة (مثل /css/style.css) تلقائيًا\n• يُنصح باستخدام المسارات النسبية (مثل ./style.css)"
    }

    val projectAnalysisResult: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目分析结果"
        AppLanguage.ENGLISH -> "Project Analysis Result"
        AppLanguage.ARABIC -> "نتيجة تحليل المشروع"
    }

    val fileInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "文件信息"
        AppLanguage.ENGLISH -> "File Info"
        AppLanguage.ARABIC -> "معلومات الملف"
    }

    val detectedIssues: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测到的问题"
        AppLanguage.ENGLISH -> "Detected Issues"
        AppLanguage.ARABIC -> "المشاكل المكتشفة"
    }

    val suggestions: String get() = when (lang) {
        AppLanguage.CHINESE -> "建议"
        AppLanguage.ENGLISH -> "Suggestions"
        AppLanguage.ARABIC -> "اقتراحات"
    }

    val autoProcessHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用会自动处理：路径修复、CSS/JS内联、编码转换、viewport适配"
        AppLanguage.ENGLISH -> "App will auto-process: path fixing, CSS/JS inlining, encoding conversion, viewport adaptation"
        AppLanguage.ARABIC -> "سيقوم التطبيق بالمعالجة التلقائية: إصلاح المسارات، دمج CSS/JS، تحويل الترميز، تكييف viewport"
    }

    val gotIt: String get() = when (lang) {
        AppLanguage.CHINESE -> "知道了"
        AppLanguage.ENGLISH -> "Got it"
        AppLanguage.ARABIC -> "فهمت"
    }
    // ==================== Activation Dialog ====================
    val activateApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活应用"
        AppLanguage.ENGLISH -> "Activate App"
        AppLanguage.ARABIC -> "تفعيل التطبيق"
    }

    val enterActivationCodeToContinue: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入激活码以继续使用"
        AppLanguage.ENGLISH -> "Please enter activation code to continue"
        AppLanguage.ARABIC -> "يرجى إدخال رمز التفعيل للمتابعة"
    }

    val activate: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活"
        AppLanguage.ENGLISH -> "Activate"
        AppLanguage.ARABIC -> "تفعيل"
    }

    val addActivationCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加激活码"
        AppLanguage.ENGLISH -> "Add Activation Code"
        AppLanguage.ARABIC -> "إضافة رمز التفعيل"
    }

    val useCustomCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用自定义激活码"
        AppLanguage.ENGLISH -> "Use Custom Code"
        AppLanguage.ARABIC -> "استخدام رمز مخصص"
    }

    val codeLength: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活码长度"
        AppLanguage.ENGLISH -> "Code Length"
        AppLanguage.ARABIC -> "طول الرمز"
    }

    val chars: String get() = when (lang) {
        AppLanguage.CHINESE -> "位"
        AppLanguage.ENGLISH -> "chars"
        AppLanguage.ARABIC -> "حرف"
    }

    val codeTooShort: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活码至少需要 4 个字符"
        AppLanguage.ENGLISH -> "Activation code must be at least 4 characters"
        AppLanguage.ARABIC -> "يجب أن يكون رمز التفعيل 4 أحرف على الأقل"
    }

    val batchGeneratedNote: String get() = when (lang) {
        AppLanguage.CHINESE -> "批量生成"
        AppLanguage.ENGLISH -> "Batch generated"
        AppLanguage.ARABIC -> "إنشاء دفعة"
    }

    val invalidTimeLimitConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "无效的时间限制配置"
        AppLanguage.ENGLISH -> "Invalid time limit config"
        AppLanguage.ARABIC -> "تكوين حد زمني غير صالح"
    }

    val invalidUsageLimitConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "无效的使用次数配置"
        AppLanguage.ENGLISH -> "Invalid usage limit config"
        AppLanguage.ARABIC -> "تكوين حد الاستخدام غير صالح"
    }

    val validityDays: String get() = when (lang) {
        AppLanguage.CHINESE -> "有效期（天）"
        AppLanguage.ENGLISH -> "Validity (days)"
        AppLanguage.ARABIC -> "الصلاحية (أيام)"
    }

    val usageCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用次数"
        AppLanguage.ENGLISH -> "Usage Count"
        AppLanguage.ARABIC -> "عدد الاستخدامات"
    }

    val noteOptional: String get() = when (lang) {
        AppLanguage.CHINESE -> "备注（可选）"
        AppLanguage.ENGLISH -> "Note (optional)"
        AppLanguage.ARABIC -> "ملاحظة (اختياري)"
    }

    val vipUserOnly: String get() = when (lang) {
        AppLanguage.CHINESE -> "例如：VIP用户专用"
        AppLanguage.ENGLISH -> "e.g.: VIP users only"
        AppLanguage.ARABIC -> "مثال: لمستخدمي VIP فقط"
    }

    val requireEveryLaunch: String get() = when (lang) {
        AppLanguage.CHINESE -> "每次启动都需要验证"
        AppLanguage.ENGLISH -> "Require verification every launch"
        AppLanguage.ARABIC -> "يتطلب التحقق في كل تشغيل"
    }

    val customDialogText: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义对话框文本"
        AppLanguage.ENGLISH -> "Custom Dialog Text"
        AppLanguage.ARABIC -> "نص الحوار المخصص"
    }

    val customDialogTextHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义激活对话框中显示的文本，留空使用默认文本"
        AppLanguage.ENGLISH -> "Customize text shown in activation dialog, leave empty for default"
        AppLanguage.ARABIC -> "تخصيص النص المعروض في حوار التفعيل، اتركه فارغًا للافتراضي"
    }

    val dialogTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "标题"
        AppLanguage.ENGLISH -> "Title"
        AppLanguage.ARABIC -> "العنوان"
    }

    val dialogTitleHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "默认：激活应用"
        AppLanguage.ENGLISH -> "Default: Activate App"
        AppLanguage.ARABIC -> "الافتراضي: تفعيل التطبيق"
    }

    val dialogSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "副标题"
        AppLanguage.ENGLISH -> "Subtitle"
        AppLanguage.ARABIC -> "العنوان الفرعي"
    }

    val dialogSubtitleHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "默认：请输入激活码以继续使用"
        AppLanguage.ENGLISH -> "Default: Please enter activation code to continue"
        AppLanguage.ARABIC -> "الافتراضي: يرجى إدخال رمز التفعيل للمتابعة"
    }

    val dialogInputLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入框标签"
        AppLanguage.ENGLISH -> "Input Label"
        AppLanguage.ARABIC -> "علامة الإدخال"
    }

    val dialogInputLabelHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "默认：激活码"
        AppLanguage.ENGLISH -> "Default: Activation Code"
        AppLanguage.ARABIC -> "الافتراضي: رمز التفعيل"
    }

    val dialogButtonText: String get() = when (lang) {
        AppLanguage.CHINESE -> "按钮文字"
        AppLanguage.ENGLISH -> "Button Text"
        AppLanguage.ARABIC -> "نص الزر"
    }

    val dialogButtonTextHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "默认：激活"
        AppLanguage.ENGLISH -> "Default: Activate"
        AppLanguage.ARABIC -> "الافتراضي: تفعيل"
    }

    val requireEveryLaunchHintOn: String get() = when (lang) {
        AppLanguage.CHINESE -> "每次打开应用都需要输入激活码"
        AppLanguage.ENGLISH -> "Enter activation code every time app opens"
        AppLanguage.ARABIC -> "أدخل رمز التفعيل في كل مرة يفتح فيها التطبيق"
    }

    val requireEveryLaunchHintOff: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活一次后永久有效"
        AppLanguage.ENGLISH -> "Valid permanently after one activation"
        AppLanguage.ARABIC -> "صالح بشكل دائم بعد تفعيل واحد"
    }
    // ==================== Media Gallery ====================
    val galleryApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体画廊"
        AppLanguage.ENGLISH -> "Media Gallery"
        AppLanguage.ARABIC -> "معرض الوسائط"
    }

    val galleryCreateTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建媒体画廊"
        AppLanguage.ENGLISH -> "Create Media Gallery"
        AppLanguage.ARABIC -> "إنشاء معرض الوسائط"
    }

    val galleryTabMedia: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体"
        AppLanguage.ENGLISH -> "Media"
        AppLanguage.ARABIC -> "الوسائط"
    }

    val galleryTabPlayback: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放"
        AppLanguage.ENGLISH -> "Playback"
        AppLanguage.ARABIC -> "التشغيل"
    }

    val galleryTabDisplay: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示"
        AppLanguage.ENGLISH -> "Display"
        AppLanguage.ARABIC -> "العرض"
    }

    val galleryCategories: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类管理"
        AppLanguage.ENGLISH -> "Categories"
        AppLanguage.ARABIC -> "الفئات"
    }

    val galleryMediaList: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体列表"
        AppLanguage.ENGLISH -> "Media List"
        AppLanguage.ARABIC -> "قائمة الوسائط"
    }

    val galleryItemCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "项"
        AppLanguage.ENGLISH -> "items"
        AppLanguage.ARABIC -> "عناصر"
    }

    val galleryAddMedia: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加媒体"
        AppLanguage.ENGLISH -> "Add Media"
        AppLanguage.ARABIC -> "إضافة وسائط"
    }

    val galleryClickToAdd: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击添加图片或视频"
        AppLanguage.ENGLISH -> "Click to add images or videos"
        AppLanguage.ARABIC -> "انقر لإضافة صور أو فيديوهات"
    }

    val gallerySupportTypes: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持 JPG, PNG, GIF, MP4, WebM 等格式"
        AppLanguage.ENGLISH -> "Supports JPG, PNG, GIF, MP4, WebM, etc."
        AppLanguage.ARABIC -> "يدعم JPG, PNG, GIF, MP4, WebM وغيرها"
    }

    val galleryImages: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片"
        AppLanguage.ENGLISH -> "Images"
        AppLanguage.ARABIC -> "صور"
    }

    val galleryVideos: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频"
        AppLanguage.ENGLISH -> "Videos"
        AppLanguage.ARABIC -> "فيديوهات"
    }

    val galleryEmpty: String get() = when (lang) {
        AppLanguage.CHINESE -> "无媒体文件"
        AppLanguage.ENGLISH -> "No media files"
        AppLanguage.ARABIC -> "لا توجد ملفات وسائط"
    }

    val galleryTotalSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "总大小"
        AppLanguage.ENGLISH -> "Total Size"
        AppLanguage.ARABIC -> "الحجم الكلي"
    }

    val galleryPlayMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放模式"
        AppLanguage.ENGLISH -> "Play Mode"
        AppLanguage.ARABIC -> "وضع التشغيل"
    }

    val galleryModeSequential: String get() = when (lang) {
        AppLanguage.CHINESE -> "顺序"
        AppLanguage.ENGLISH -> "Sequential"
        AppLanguage.ARABIC -> "تسلسلي"
    }

    val galleryModeShuffle: String get() = when (lang) {
        AppLanguage.CHINESE -> "随机"
        AppLanguage.ENGLISH -> "Shuffle"
        AppLanguage.ARABIC -> "عشوائي"
    }

    val galleryModeSingleLoop: String get() = when (lang) {
        AppLanguage.CHINESE -> "Single loop"
        AppLanguage.ENGLISH -> "Single Loop"
        AppLanguage.ARABIC -> "تكرار واحد"
    }

    val galleryImageSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片播放设置"
        AppLanguage.ENGLISH -> "Image Playback Settings"
        AppLanguage.ARABIC -> "إعدادات تشغيل الصور"
    }

    val galleryImageInterval: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片播放间隔"
        AppLanguage.ENGLISH -> "Image Interval"
        AppLanguage.ARABIC -> "فترة الصورة"
    }

    val galleryVideoSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频播放设置"
        AppLanguage.ENGLISH -> "Video Playback Settings"
        AppLanguage.ARABIC -> "إعدادات تشغيل الفيديو"
    }

    val galleryEnableAudioHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频播放时启用声音"
        AppLanguage.ENGLISH -> "Enable sound during video playback"
        AppLanguage.ARABIC -> "تفعيل الصوت أثناء تشغيل الفيديو"
    }

    val galleryVideoAutoNext: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动播放下一个"
        AppLanguage.ENGLISH -> "Auto Play Next"
        AppLanguage.ARABIC -> "تشغيل التالي تلقائيًا"
    }

    val galleryVideoAutoNextHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频播放完毕后自动播放下一个"
        AppLanguage.ENGLISH -> "Automatically play next after video ends"
        AppLanguage.ARABIC -> "تشغيل التالي تلقائيًا بعد انتهاء الفيديو"
    }

    val galleryGeneralSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "通用设置"
        AppLanguage.ENGLISH -> "General Settings"
        AppLanguage.ARABIC -> "الإعدادات العامة"
    }

    val galleryAutoPlay: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动播放"
        AppLanguage.ENGLISH -> "Auto Play"
        AppLanguage.ARABIC -> "تشغيل تلقائي"
    }

    val galleryAutoPlayHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "进入画廊后自动开始播放"
        AppLanguage.ENGLISH -> "Start playing automatically when entering gallery"
        AppLanguage.ARABIC -> "بدء التشغيل تلقائيًا عند الدخول إلى المعرض"
    }

    val galleryLoopHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放完所有媒体后重新开始"
        AppLanguage.ENGLISH -> "Restart from beginning after playing all media"
        AppLanguage.ARABIC -> "إعادة التشغيل من البداية بعد تشغيل كل الوسائط"
    }

    val galleryShuffleOnLoop: String get() = when (lang) {
        AppLanguage.CHINESE -> "循环时打乱顺序"
        AppLanguage.ENGLISH -> "Shuffle on Loop"
        AppLanguage.ARABIC -> "خلط عند التكرار"
    }

    val galleryShuffleOnLoopHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "每次循环时重新打乱播放顺序"
        AppLanguage.ENGLISH -> "Shuffle playback order on each loop"
        AppLanguage.ARABIC -> "خلط ترتيب التشغيل في كل تكرار"
    }

    val galleryRememberPosition: String get() = when (lang) {
        AppLanguage.CHINESE -> "记住播放位置"
        AppLanguage.ENGLISH -> "Remember Position"
        AppLanguage.ARABIC -> "تذكر الموضع"
    }

    val galleryRememberPositionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "下次打开时从上次位置继续"
        AppLanguage.ENGLISH -> "Continue from last position when reopening"
        AppLanguage.ARABIC -> "المتابعة من الموضع الأخير عند إعادة الفتح"
    }

    val galleryViewMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "视图模式"
        AppLanguage.ENGLISH -> "View Mode"
        AppLanguage.ARABIC -> "وضع العرض"
    }

    val galleryViewGrid: String get() = when (lang) {
        AppLanguage.CHINESE -> "网格"
        AppLanguage.ENGLISH -> "Grid"
        AppLanguage.ARABIC -> "شبكة"
    }

    val galleryViewList: String get() = when (lang) {
        AppLanguage.CHINESE -> "列表"
        AppLanguage.ENGLISH -> "List"
        AppLanguage.ARABIC -> "قائمة"
    }

    val galleryViewTimeline: String get() = when (lang) {
        AppLanguage.CHINESE -> "时间线"
        AppLanguage.ENGLISH -> "Timeline"
        AppLanguage.ARABIC -> "الجدول الزمني"
    }

    val galleryGridColumns: String get() = when (lang) {
        AppLanguage.CHINESE -> "网格列数"
        AppLanguage.ENGLISH -> "Grid Columns"
        AppLanguage.ARABIC -> "أعمدة الشبكة"
    }

    val gallerySortOrder: String get() = when (lang) {
        AppLanguage.CHINESE -> "排序方式"
        AppLanguage.ENGLISH -> "Sort Order"
        AppLanguage.ARABIC -> "ترتيب الفرز"
    }

    val gallerySortCustom: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义排序"
        AppLanguage.ENGLISH -> "Custom Order"
        AppLanguage.ARABIC -> "ترتيب مخصص"
    }

    val gallerySortNameAsc: String get() = when (lang) {
        AppLanguage.CHINESE -> "名称升序"
        AppLanguage.ENGLISH -> "Name (A-Z)"
        AppLanguage.ARABIC -> "الاسم (أ-ي)"
    }

    val gallerySortNameDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "名称降序"
        AppLanguage.ENGLISH -> "Name (Z-A)"
        AppLanguage.ARABIC -> "الاسم (ي-أ)"
    }

    val gallerySortDateAsc: String get() = when (lang) {
        AppLanguage.CHINESE -> "日期升序"
        AppLanguage.ENGLISH -> "Date (Oldest)"
        AppLanguage.ARABIC -> "التاريخ (الأقدم)"
    }

    val gallerySortDateDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "日期降序"
        AppLanguage.ENGLISH -> "Date (Newest)"
        AppLanguage.ARABIC -> "التاريخ (الأحدث)"
    }

    val gallerySortType: String get() = when (lang) {
        AppLanguage.CHINESE -> "按类型分组"
        AppLanguage.ENGLISH -> "By Type"
        AppLanguage.ARABIC -> "حسب النوع"
    }

    val galleryPlayerSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放器设置"
        AppLanguage.ENGLISH -> "Player Settings"
        AppLanguage.ARABIC -> "إعدادات المشغل"
    }

    val galleryShowThumbnailBar: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示缩略图栏"
        AppLanguage.ENGLISH -> "Show Thumbnail Bar"
        AppLanguage.ARABIC -> "إظهار شريط الصور المصغرة"
    }

    val galleryShowThumbnailBarHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "在播放器底部显示可点击的缩略图导航栏"
        AppLanguage.ENGLISH -> "Show clickable thumbnail navigation bar at the bottom"
        AppLanguage.ARABIC -> "إظهار شريط التنقل بالصور المصغرة القابلة للنقر في الأسفل"
    }

    val galleryShowMediaInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示媒体信息"
        AppLanguage.ENGLISH -> "Show Media Info"
        AppLanguage.ARABIC -> "إظهار معلومات الوسائط"
    }

    val galleryShowMediaInfoHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放时显示媒体名称、索引等信息"
        AppLanguage.ENGLISH -> "Show media name, index, etc. during playback"
        AppLanguage.ARABIC -> "إظهار اسم الوسائط والفهرس وغيرها أثناء التشغيل"
    }

    val galleryBackgroundColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "背景颜色"
        AppLanguage.ENGLISH -> "Background Color"
        AppLanguage.ARABIC -> "لون الخلفية"
    }

    val galleryAddCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加分类"
        AppLanguage.ENGLISH -> "Add Category"
        AppLanguage.ARABIC -> "إضافة فئة"
    }

    val galleryEditCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑分类"
        AppLanguage.ENGLISH -> "Edit Category"
        AppLanguage.ARABIC -> "تعديل الفئة"
    }

    val galleryCategoryName: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类名称"
        AppLanguage.ENGLISH -> "Category Name"
        AppLanguage.ARABIC -> "اسم الفئة"
    }

    val galleryCategoryIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类图标"
        AppLanguage.ENGLISH -> "Category Icon"
        AppLanguage.ARABIC -> "رمز الفئة"
    }

    val galleryCategoryColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类颜色"
        AppLanguage.ENGLISH -> "Category Color"
        AppLanguage.ARABIC -> "لون الفئة"
    }

    val galleryMediaDetail: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体详情"
        AppLanguage.ENGLISH -> "Media Details"
        AppLanguage.ARABIC -> "تفاصيل الوسائط"
    }

    val galleryCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类"
        AppLanguage.ENGLISH -> "Category"
        AppLanguage.ARABIC -> "الفئة"
    }

    val galleryNoCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "未分类"
        AppLanguage.ENGLISH -> "Uncategorized"
        AppLanguage.ARABIC -> "غير مصنف"
    }

    val galleryType: String get() = when (lang) {
        AppLanguage.CHINESE -> "类型"
        AppLanguage.ENGLISH -> "Type"
        AppLanguage.ARABIC -> "النوع"
    }

    val galleryDuration: String get() = when (lang) {
        AppLanguage.CHINESE -> "时长"
        AppLanguage.ENGLISH -> "Duration"
        AppLanguage.ARABIC -> "المدة"
    }

    val gallerySize: String get() = when (lang) {
        AppLanguage.CHINESE -> "大小"
        AppLanguage.ENGLISH -> "Size"
        AppLanguage.ARABIC -> "الحجم"
    }

    val galleryDimensions: String get() = when (lang) {
        AppLanguage.CHINESE -> "尺寸"
        AppLanguage.ENGLISH -> "Dimensions"
        AppLanguage.ARABIC -> "الأبعاد"
    }

    val name: String get() = when (lang) {
        AppLanguage.CHINESE -> "名称"
        AppLanguage.ENGLISH -> "Name"
        AppLanguage.ARABIC -> "الاسم"
    }

    val galleryPlayerPrevious: String get() = when (lang) {
        AppLanguage.CHINESE -> "上一个"
        AppLanguage.ENGLISH -> "Previous"
        AppLanguage.ARABIC -> "السابق"
    }

    val galleryPlayerNext: String get() = when (lang) {
        AppLanguage.CHINESE -> "下一个"
        AppLanguage.ENGLISH -> "Next"
        AppLanguage.ARABIC -> "التالي"
    }

    val galleryPlayerPause: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂停"
        AppLanguage.ENGLISH -> "Pause"
        AppLanguage.ARABIC -> "إيقاف مؤقت"
    }

    val galleryPlayerPlay: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放"
        AppLanguage.ENGLISH -> "Play"
        AppLanguage.ARABIC -> "تشغيل"
    }

    val galleryPlayerSpeed: String get() = when (lang) {
        AppLanguage.CHINESE -> "倍速"
        AppLanguage.ENGLISH -> "Speed"
        AppLanguage.ARABIC -> "السرعة"
    }

    val galleryPlayerSeekForward: String get() = when (lang) {
        AppLanguage.CHINESE -> "快进 10 秒"
        AppLanguage.ENGLISH -> "Forward 10s"
        AppLanguage.ARABIC -> "تقديم 10 ثواني"
    }

    val galleryPlayerSeekBack: String get() = when (lang) {
        AppLanguage.CHINESE -> "后退 10 秒"
        AppLanguage.ENGLISH -> "Back 10s"
        AppLanguage.ARABIC -> "رجوع 10 ثواني"
    }

    val galleryLongPressHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "长按快进"
        AppLanguage.ENGLISH -> "Long press to fast forward"
        AppLanguage.ARABIC -> "اضغط مطولاً للتقديم السريع"
    }

    val galleryDoubleTapHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "双击左侧/右侧快退/快进"
        AppLanguage.ENGLISH -> "Double tap left/right to seek"
        AppLanguage.ARABIC -> "انقر مرتين على اليسار/اليمين للتنقل"
    }
    // ==================== Announcement Template Names ====================
    val templateMinimalDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "简约清爽的设计风格"
        AppLanguage.ENGLISH -> "Clean and simple design style"
        AppLanguage.ARABIC -> "نمط تصميم نظيف وبسيط"
    }

    val templateXiaohongshu: String get() = when (lang) {
        AppLanguage.CHINESE -> "小红书"
        AppLanguage.ENGLISH -> "Xiaohongshu"
        AppLanguage.ARABIC -> "شياوهونغشو"
    }

    val templateXiaohongshuDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "精美卡片风格"
        AppLanguage.ENGLISH -> "Beautiful card style"
        AppLanguage.ARABIC -> "نمط بطاقة جميل"
    }

    val templateGradientDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "炫彩渐变背景"
        AppLanguage.ENGLISH -> "Colorful gradient background"
        AppLanguage.ARABIC -> "خلفية متدرجة ملونة"
    }

    val templateGlassmorphismDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "现代毛玻璃效果"
        AppLanguage.ENGLISH -> "Modern frosted glass effect"
        AppLanguage.ARABIC -> "تأثير الزجاج المصنفر الحديث"
    }

    val templateNeon: String get() = when (lang) {
        AppLanguage.CHINESE -> "霓虹"
        AppLanguage.ENGLISH -> "Neon"
        AppLanguage.ARABIC -> "نيون"
    }

    val templateNeonDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "赛博朋克风格"
        AppLanguage.ENGLISH -> "Cyberpunk style"
        AppLanguage.ARABIC -> "نمط سايبربانك"
    }

    val templateCute: String get() = when (lang) {
        AppLanguage.CHINESE -> "可爱"
        AppLanguage.ENGLISH -> "Cute"
        AppLanguage.ARABIC -> "لطيف"
    }

    val templateCuteDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "萌系卡通风格"
        AppLanguage.ENGLISH -> "Cute cartoon style"
        AppLanguage.ARABIC -> "نمط كرتوني لطيف"
    }

    val templateElegant: String get() = when (lang) {
        AppLanguage.CHINESE -> "优雅"
        AppLanguage.ENGLISH -> "Elegant"
        AppLanguage.ARABIC -> "أنيق"
    }

    val templateElegantDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "高端商务风格"
        AppLanguage.ENGLISH -> "Premium business style"
        AppLanguage.ARABIC -> "نمط أعمال راقي"
    }

    val templateFestive: String get() = when (lang) {
        AppLanguage.CHINESE -> "节日"
        AppLanguage.ENGLISH -> "Festive"
        AppLanguage.ARABIC -> "احتفالي"
    }

    val templateFestiveDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "喜庆节日风格"
        AppLanguage.ENGLISH -> "Festive celebration style"
        AppLanguage.ARABIC -> "نمط احتفالي"
    }

    val templateDarkDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "深色主题风格"
        AppLanguage.ENGLISH -> "Dark theme style"
        AppLanguage.ARABIC -> "نمط السمة الداكنة"
    }

    val templateNatureDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "清新自然风格"
        AppLanguage.ENGLISH -> "Fresh natural style"
        AppLanguage.ARABIC -> "نمط طبيعي منعش"
    }
}
