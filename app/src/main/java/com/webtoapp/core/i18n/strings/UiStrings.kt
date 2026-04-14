package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object UiStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage
    // ==================== Animation Speed ====================
    val speedSlow: String get() = when (lang) {
        AppLanguage.CHINESE -> "慢速"
        AppLanguage.ENGLISH -> "Slow"
        AppLanguage.ARABIC -> "بطيء"
    }

    val speedNormal: String get() = when (lang) {
        AppLanguage.CHINESE -> "正常"
        AppLanguage.ENGLISH -> "Normal"
        AppLanguage.ARABIC -> "عادي"
    }

    val speedFast: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速"
        AppLanguage.ENGLISH -> "Fast"
        AppLanguage.ARABIC -> "سريع"
    }

    val speedInstant: String get() = when (lang) {
        AppLanguage.CHINESE -> "即时"
        AppLanguage.ENGLISH -> "Instant"
        AppLanguage.ARABIC -> "فوري"
    }

    val previewEffect: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览效果"
        AppLanguage.ENGLISH -> "Preview Effect"
        AppLanguage.ARABIC -> "معاينة التأثير"
    }

    val button: String get() = when (lang) {
        AppLanguage.CHINESE -> "按钮"
        AppLanguage.ENGLISH -> "Button"
        AppLanguage.ARABIC -> "زر"
    }

    val enableAnimations: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用动画"
        AppLanguage.ENGLISH -> "Enable Animations"
        AppLanguage.ARABIC -> "تفعيل الرسوم المتحركة"
    }

    val enableAnimationsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "开启界面过渡动画和交互反馈"
        AppLanguage.ENGLISH -> "Enable UI transition animations and interaction feedback"
        AppLanguage.ARABIC -> "تفعيل رسوم الانتقال والتفاعل"
    }

    val particleEffects: String get() = when (lang) {
        AppLanguage.CHINESE -> "粒子效果"
        AppLanguage.ENGLISH -> "Particle Effects"
        AppLanguage.ARABIC -> "تأثيرات الجسيمات"
    }

    val particleEffectsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示主题特有的背景粒子动画"
        AppLanguage.ENGLISH -> "Show theme-specific background particle animations"
        AppLanguage.ARABIC -> "عرض رسوم الجسيمات الخلفية الخاصة بالسمة"
    }

    val particleNotSupported: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前主题不支持粒子效果，切换其他主题可体验"
        AppLanguage.ENGLISH -> "Current theme doesn't support particles, try switching themes"
        AppLanguage.ARABIC -> "السمة الحالية لا تدعم تأثيرات الجسيمات، جرب تغيير السمة"
    }

    val hapticFeedback: String get() = when (lang) {
        AppLanguage.CHINESE -> "触觉反馈"
        AppLanguage.ENGLISH -> "Haptic Feedback"
        AppLanguage.ARABIC -> "ردود الفعل اللمسية"
    }

    val hapticFeedbackHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "交互时提供震动反馈"
        AppLanguage.ENGLISH -> "Provide vibration feedback on interaction"
        AppLanguage.ARABIC -> "توفير ردود فعل اهتزازية عند التفاعل"
    }

    val soundFeedback: String get() = when (lang) {
        AppLanguage.CHINESE -> "音效反馈"
        AppLanguage.ENGLISH -> "Sound Feedback"
        AppLanguage.ARABIC -> "ردود الفعل الصوتية"
    }

    val soundFeedbackHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "交互时播放音效（强化模式）"
        AppLanguage.ENGLISH -> "Play sound effects on interaction (enhanced mode)"
        AppLanguage.ARABIC -> "تشغيل المؤثرات الصوتية عند التفاعل (الوضع المحسّن)"
    }

    val animationSpeed: String get() = when (lang) {
        AppLanguage.CHINESE -> "动画速度"
        AppLanguage.ENGLISH -> "Animation Speed"
        AppLanguage.ARABIC -> "سرعة الرسوم المتحركة"
    }

    val currentThemeAnimStyle: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前主题动画风格"
        AppLanguage.ENGLISH -> "Current Theme Animation Style"
        AppLanguage.ARABIC -> "نمط الرسوم المتحركة للسمة الحالية"
    }

    val interactionStyle: String get() = when (lang) {
        AppLanguage.CHINESE -> "交互风格"
        AppLanguage.ENGLISH -> "Interaction Style"
        AppLanguage.ARABIC -> "نمط التفاعل"
    }

    val glow: String get() = when (lang) {
        AppLanguage.CHINESE -> "发光"
        AppLanguage.ENGLISH -> "Glow"
        AppLanguage.ARABIC -> "توهج"
    }

    val particles: String get() = when (lang) {
        AppLanguage.CHINESE -> "粒子"
        AppLanguage.ENGLISH -> "Particles"
        AppLanguage.ARABIC -> "جسيمات"
    }
    // ==================== General ====================
    val yes: String get() = when (lang) {
        AppLanguage.CHINESE -> "Yes"
        AppLanguage.ENGLISH -> "Yes"
        AppLanguage.ARABIC -> "نعم"
    }

    val no: String get() = when (lang) {
        AppLanguage.CHINESE -> "No"
        AppLanguage.ENGLISH -> "No"
        AppLanguage.ARABIC -> "لا"
    }

    val error: String get() = when (lang) {
        AppLanguage.CHINESE -> "Error"
        AppLanguage.ENGLISH -> "Error"
        AppLanguage.ARABIC -> "خطأ"
    }

    val success: String get() = when (lang) {
        AppLanguage.CHINESE -> "成功"
        AppLanguage.ENGLISH -> "Success"
        AppLanguage.ARABIC -> "نجاح"
    }

    val close: String get() = when (lang) {
        AppLanguage.CHINESE -> "关闭"
        AppLanguage.ENGLISH -> "Close"
        AppLanguage.ARABIC -> "إغلاق"
    }

    val cancel: String get() = when (lang) {
        AppLanguage.CHINESE -> "取消"
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.ARABIC -> "إلغاء"
    }

    val copy: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制"
        AppLanguage.ENGLISH -> "Copy"
        AppLanguage.ARABIC -> "نسخ"
    }

    val share: String get() = when (lang) {
        AppLanguage.CHINESE -> "Share"
        AppLanguage.ENGLISH -> "Share"
        AppLanguage.ARABIC -> "مشاركة"
    }

    val download: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载"
        AppLanguage.ENGLISH -> "Download"
        AppLanguage.ARABIC -> "تحميل"
    }

    val remove: String get() = when (lang) {
        AppLanguage.CHINESE -> "Remove"
        AppLanguage.ENGLISH -> "Remove"
        AppLanguage.ARABIC -> "إزالة"
    }

    val clear: String get() = when (lang) {
        AppLanguage.CHINESE -> "清除"
        AppLanguage.ENGLISH -> "Clear"
        AppLanguage.ARABIC -> "مسح"
    }

    val add: String get() = when (lang) {
        AppLanguage.CHINESE -> "Add"
        AppLanguage.ENGLISH -> "Add"
        AppLanguage.ARABIC -> "إضافة"
    }

    val enabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "已启用"
        AppLanguage.ENGLISH -> "Enabled"
        AppLanguage.ARABIC -> "مفعل"
    }

    val disabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "已禁用"
        AppLanguage.ENGLISH -> "Disabled"
        AppLanguage.ARABIC -> "معطل"
    }

    val enable: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用"
        AppLanguage.ENGLISH -> "Enable"
        AppLanguage.ARABIC -> "تمكين"
    }

    val disable: String get() = when (lang) {
        AppLanguage.CHINESE -> "禁用"
        AppLanguage.ENGLISH -> "Disable"
        AppLanguage.ARABIC -> "تعطيل"
    }

    val tip: String get() = when (lang) {
        AppLanguage.CHINESE -> "Notice"
        AppLanguage.ENGLISH -> "Tip"
        AppLanguage.ARABIC -> "تلميح"
    }

    val warning: String get() = when (lang) {
        AppLanguage.CHINESE -> "Warning"
        AppLanguage.ENGLISH -> "Warning"
        AppLanguage.ARABIC -> "تحذير"
    }

    val info: String get() = when (lang) {
        AppLanguage.CHINESE -> "信息"
        AppLanguage.ENGLISH -> "Info"
        AppLanguage.ARABIC -> "معلومات"
    }

    val done: String get() = when (lang) {
        AppLanguage.CHINESE -> "Done"
        AppLanguage.ENGLISH -> "Done"
        AppLanguage.ARABIC -> "تم"
    }

    val edit: String get() = when (lang) {
        AppLanguage.CHINESE -> "Edit"
        AppLanguage.ENGLISH -> "Edit"
        AppLanguage.ARABIC -> "تعديل"
    }

    val newUpdate: String get() = when (lang) {
        AppLanguage.CHINESE -> "发现新版本"
        AppLanguage.ENGLISH -> "New Update Available"
        AppLanguage.ARABIC -> "تحديث جديد متاح"
    }

    val updateNow: String get() = when (lang) {
        AppLanguage.CHINESE -> "立即更新"
        AppLanguage.ENGLISH -> "Update Now"
        AppLanguage.ARABIC -> "التحديث الآن"
    }

    val latestVersion: String get() = when (lang) {
        AppLanguage.CHINESE -> "已是最新版本"
        AppLanguage.ENGLISH -> "Already latest version"
        AppLanguage.ARABIC -> "أحدث إصدار بالفعل"
    }

    val networkError: String get() = when (lang) {
        AppLanguage.CHINESE -> "网络错误"
        AppLanguage.ENGLISH -> "Network Error"
        AppLanguage.ARABIC -> "خطأ في الشبكة"
    }

    val loading: String get() = when (lang) {
        AppLanguage.CHINESE -> "Loading..."
        AppLanguage.ENGLISH -> "Loading..."
        AppLanguage.ARABIC -> "جاري التحميل..."
    }

    val noData: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无数据"
        AppLanguage.ENGLISH -> "No data"
        AppLanguage.ARABIC -> "لا توجد بيانات"
    }

    val saved: String get() = when (lang) {
        AppLanguage.CHINESE -> "已保存"
        AppLanguage.ENGLISH -> "Saved"
        AppLanguage.ARABIC -> "تم الحفظ"
    }

    val operationSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "Operation successful"
        AppLanguage.ENGLISH -> "Operation successful"
        AppLanguage.ARABIC -> "تمت العملية بنجاح"
    }

    val operationFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Operation failed"
        AppLanguage.ENGLISH -> "Operation failed"
        AppLanguage.ARABIC -> "فشلت العملية"
    }

    val unknownError: String get() = when (lang) {
        AppLanguage.CHINESE -> "未知错误"
        AppLanguage.ENGLISH -> "Unknown error"
        AppLanguage.ARABIC -> "خطأ غير معروف"
    }

    val pleaseWait: String get() = when (lang) {
        AppLanguage.CHINESE -> "请稍候..."
        AppLanguage.ENGLISH -> "Please wait..."
        AppLanguage.ARABIC -> "يرجى الانتظار..."
    }

    val processing: String get() = when (lang) {
        AppLanguage.CHINESE -> "处理中..."
        AppLanguage.ENGLISH -> "Processing..."
        AppLanguage.ARABIC -> "جاري المعالجة..."
    }

    val on: String get() = when (lang) {
        AppLanguage.CHINESE -> "开"
        AppLanguage.ENGLISH -> "On"
        AppLanguage.ARABIC -> "تشغيل"
    }

    val off: String get() = when (lang) {
        AppLanguage.CHINESE -> "关"
        AppLanguage.ENGLISH -> "Off"
        AppLanguage.ARABIC -> "إيقاف"
    }

    val selectFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "Select file"
        AppLanguage.ENGLISH -> "Select File"
        AppLanguage.ARABIC -> "اختيار ملف"
    }

    val selectFolder: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择文件夹"
        AppLanguage.ENGLISH -> "Select Folder"
        AppLanguage.ARABIC -> "اختيار مجلد"
    }

    val fileNotFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "文件未找到"
        AppLanguage.ENGLISH -> "File not found"
        AppLanguage.ARABIC -> "الملف غير موجود"
    }

    val invalidFormat: String get() = when (lang) {
        AppLanguage.CHINESE -> "格式无效"
        AppLanguage.ENGLISH -> "Invalid format"
        AppLanguage.ARABIC -> "تنسيق غير صالح"
    }

    val permissionDenied: String get() = when (lang) {
        AppLanguage.CHINESE -> "权限被拒绝"
        AppLanguage.ENGLISH -> "Permission denied"
        AppLanguage.ARABIC -> "تم رفض الإذن"
    }

    val grantPermission: String get() = when (lang) {
        AppLanguage.CHINESE -> "授予权限"
        AppLanguage.ENGLISH -> "Grant Permission"
        AppLanguage.ARABIC -> "منح الإذن"
    }
    // ==================== More General Messages ====================
    val savingToGallery: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在保存到相册..."
        AppLanguage.ENGLISH -> "Saving to gallery..."
        AppLanguage.ARABIC -> "جاري الحفظ في المعرض..."
    }

    val savingImageToGallery: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在保存图片到相册..."
        AppLanguage.ENGLISH -> "Saving image to gallery..."
        AppLanguage.ARABIC -> "جاري حفظ الصورة في المعرض..."
    }

    val savingVideoToGallery: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在保存视频到相册..."
        AppLanguage.ENGLISH -> "Saving video to gallery..."
        AppLanguage.ARABIC -> "جاري حفظ الفيديو في المعرض..."
    }

    val downloadStartFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载启动失败"
        AppLanguage.ENGLISH -> "Download start failed"
        AppLanguage.ARABIC -> "فشل بدء التحميل"
    }

    val blobDownloadProcessing: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在处理下载..."
        AppLanguage.ENGLISH -> "Processing download..."
        AppLanguage.ARABIC -> "جاري معالجة التحميل..."
    }

    val blobDownloadFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "无法获取文件数据，请重试"
        AppLanguage.ENGLISH -> "Cannot get file data, please try again"
        AppLanguage.ARABIC -> "لا يمكن الحصول على بيانات الملف، حاول مرة أخرى"
    }

    val startDownloadCheckNotification: String get() = when (lang) {
        AppLanguage.CHINESE -> "开始下载，请查看通知栏"
        AppLanguage.ENGLISH -> "Download started, check notification"
        AppLanguage.ARABIC -> "بدأ التحميل، تحقق من الإشعارات"
    }

    val downloadLinkNotFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "未找到下载链接"
        AppLanguage.ENGLISH -> "Download link not found"
        AppLanguage.ARABIC -> "لم يتم العثور على رابط التحميل"
    }

    val downloadFailedTryBrowser: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载失败，尝试使用浏览器下载"
        AppLanguage.ENGLISH -> "Download failed, trying browser download"
        AppLanguage.ARABIC -> "فشل التحميل، جاري المحاولة عبر المتصفح"
    }

    val cannotOpenBrowser: String get() = when (lang) {
        AppLanguage.CHINESE -> "无法打开浏览器"
        AppLanguage.ENGLISH -> "Cannot open browser"
        AppLanguage.ARABIC -> "لا يمكن فتح المتصفح"
    }

    val presetSaved: String get() = when (lang) {
        AppLanguage.CHINESE -> "方案已保存"
        AppLanguage.ENGLISH -> "Preset saved"
        AppLanguage.ARABIC -> "تم حفظ الإعداد المسبق"
    }

    val copied: String get() = when (lang) {
        AppLanguage.CHINESE -> "已复制"
        AppLanguage.ENGLISH -> "Copied"
        AppLanguage.ARABIC -> "تم النسخ"
    }

    val duplicated: String get() = when (lang) {
        AppLanguage.CHINESE -> "已复制"
        AppLanguage.ENGLISH -> "Duplicated"
        AppLanguage.ARABIC -> "تم النسخ"
    }

    val deleted: String get() = when (lang) {
        AppLanguage.CHINESE -> "已删除"
        AppLanguage.ENGLISH -> "Deleted"
        AppLanguage.ARABIC -> "تم الحذف"
    }

    val shareCodeCopiedMsg: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享码已复制"
        AppLanguage.ENGLISH -> "Share code copied"
        AppLanguage.ARABIC -> "تم نسخ رمز المشاركة"
    }

    val cannotOpenInBrowser: String get() = when (lang) {
        AppLanguage.CHINESE -> "无法在外部浏览器中打开"
        AppLanguage.ENGLISH -> "Cannot open in external browser"
        AppLanguage.ARABIC -> "لا يمكن الفتح في المتصفح الخارجي"
    }

    val noFilePathAvailable: String get() = when (lang) {
        AppLanguage.CHINESE -> "没有可用的文件路径"
        AppLanguage.ENGLISH -> "No file path available"
        AppLanguage.ARABIC -> "لا يوجد مسار ملف متاح"
    }

    val copiedAllLogs: String get() = when (lang) {
        AppLanguage.CHINESE -> "已复制全部日志"
        AppLanguage.ENGLISH -> "All logs copied"
        AppLanguage.ARABIC -> "تم نسخ جميع السجلات"
    }
    // ==================== Usage Stats ====================
    val statsTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用统计"
        AppLanguage.ENGLISH -> "Usage Statistics"
        AppLanguage.ARABIC -> "إحصائيات الاستخدام"
    }

    val statsTotalLaunches: String get() = when (lang) {
        AppLanguage.CHINESE -> "总启动次数"
        AppLanguage.ENGLISH -> "Total Launches"
        AppLanguage.ARABIC -> "إجمالي عمليات الإطلاق"
    }

    val statsTotalUsage: String get() = when (lang) {
        AppLanguage.CHINESE -> "总使用时长"
        AppLanguage.ENGLISH -> "Total Usage"
        AppLanguage.ARABIC -> "إجمالي الاستخدام"
    }

    val statsActiveApps: String get() = when (lang) {
        AppLanguage.CHINESE -> "活跃应用"
        AppLanguage.ENGLISH -> "Active Apps"
        AppLanguage.ARABIC -> "التطبيقات النشطة"
    }

    val statsMostUsed: String get() = when (lang) {
        AppLanguage.CHINESE -> "最常使用"
        AppLanguage.ENGLISH -> "Most Used"
        AppLanguage.ARABIC -> "الأكثر استخدامًا"
    }

    val statsMostTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用最久"
        AppLanguage.ENGLISH -> "Most Time Spent"
        AppLanguage.ARABIC -> "الأطول استخدامًا"
    }

    val statsRecentlyUsed: String get() = when (lang) {
        AppLanguage.CHINESE -> "最近使用"
        AppLanguage.ENGLISH -> "Recently Used"
        AppLanguage.ARABIC -> "المستخدمة مؤخرًا"
    }

    val statsLaunches: String get() = when (lang) {
        AppLanguage.CHINESE -> "次"
        AppLanguage.ENGLISH -> "launches"
        AppLanguage.ARABIC -> "مرة"
    }

    val statsNeverUsed: String get() = when (lang) {
        AppLanguage.CHINESE -> "从未使用"
        AppLanguage.ENGLISH -> "Never used"
        AppLanguage.ARABIC -> "لم يُستخدم"
    }

    val statsNoData: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无使用数据"
        AppLanguage.ENGLISH -> "No usage data yet"
        AppLanguage.ARABIC -> "لا توجد بيانات استخدام بعد"
    }

    val statsJustNow: String get() = when (lang) {
        AppLanguage.CHINESE -> "刚刚"
        AppLanguage.ENGLISH -> "Just now"
        AppLanguage.ARABIC -> "الآن"
    }

    val statsMinutesAgo: String get() = when (lang) {
        AppLanguage.CHINESE -> "分钟前"
        AppLanguage.ENGLISH -> "min ago"
        AppLanguage.ARABIC -> "دقيقة مضت"
    }

    val statsHoursAgo: String get() = when (lang) {
        AppLanguage.CHINESE -> "小时前"
        AppLanguage.ENGLISH -> "h ago"
        AppLanguage.ARABIC -> "ساعة مضت"
    }

    val statsDaysAgo: String get() = when (lang) {
        AppLanguage.CHINESE -> "天前"
        AppLanguage.ENGLISH -> "d ago"
        AppLanguage.ARABIC -> "يوم مضت"
    }
    // ==================== Download and Save ====================
    val saveFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存失败"
        AppLanguage.ENGLISH -> "Save failed"
        AppLanguage.ARABIC -> "فشل الحفظ"
    }

    val saveFailedWithReason: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存失败: %s"
        AppLanguage.ENGLISH -> "Save failed: %s"
        AppLanguage.ARABIC -> "فشل الحفظ: %s"
    }

    val savedTo: String get() = when (lang) {
        AppLanguage.CHINESE -> "已保存到: %s"
        AppLanguage.ENGLISH -> "Saved to: %s"
        AppLanguage.ARABIC -> "تم الحفظ إلى: %s"
    }

    val copiedToClipboard: String get() = when (lang) {
        AppLanguage.CHINESE -> "已复制到剪贴板"
        AppLanguage.ENGLISH -> "Copied to clipboard"
        AppLanguage.ARABIC -> "تم النسخ إلى الحافظة"
    }

    val downloadingVideo: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在下载视频..."
        AppLanguage.ENGLISH -> "Downloading video..."
        AppLanguage.ARABIC -> "جاري تحميل الفيديو..."
    }

    val shareFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Share failed"
        AppLanguage.ENGLISH -> "Share failed"
        AppLanguage.ARABIC -> "فشلت المشاركة"
    }

    val preparingShare: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在准备分享..."
        AppLanguage.ENGLISH -> "Preparing to share..."
        AppLanguage.ARABIC -> "جاري التحضير للمشاركة..."
    }

    val cannotOpenLink: String get() = when (lang) {
        AppLanguage.CHINESE -> "无法打开链接"
        AppLanguage.ENGLISH -> "Cannot open link"
        AppLanguage.ARABIC -> "لا يمكن فتح الرابط"
    }

    val sslSecurityError: String get() = when (lang) {
        AppLanguage.CHINESE -> "SSL 安全错误"
        AppLanguage.ENGLISH -> "SSL security error"
        AppLanguage.ARABIC -> "خطأ في أمان SSL"
    }

    val testingModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "测试 {0} 个模块"
        AppLanguage.ENGLISH -> "Testing {0} modules"
        AppLanguage.ARABIC -> "اختبار {0} وحدات"
    }

    val savingImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在保存图片..."
        AppLanguage.ENGLISH -> "Saving image..."
        AppLanguage.ARABIC -> "جاري حفظ الصورة..."
    }

    val imageSavedToGallery: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片已保存到相册"
        AppLanguage.ENGLISH -> "Image saved to gallery"
        AppLanguage.ARABIC -> "تم حفظ الصورة في المعرض"
    }

    val savingVideo: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在保存视频..."
        AppLanguage.ENGLISH -> "Saving video..."
        AppLanguage.ARABIC -> "جاري حفظ الفيديو..."
    }

    val videoSavedToGallery: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频已保存到相册"
        AppLanguage.ENGLISH -> "Video saved to gallery"
        AppLanguage.ARABIC -> "تم حفظ الفيديو في المعرض"
    }

    val startDownload: String get() = when (lang) {
        AppLanguage.CHINESE -> "开始下载: %s"
        AppLanguage.ENGLISH -> "Start download: %s"
        AppLanguage.ARABIC -> "بدء التحميل: %s"
    }

    val downloadFailedWithReason: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载失败: %s"
        AppLanguage.ENGLISH -> "Download failed: %s"
        AppLanguage.ARABIC -> "فشل التحميل: %s"
    }
    // ==================== About Page Extras ====================
    val joinCommunityGroup: String get() = when (lang) {
        AppLanguage.CHINESE -> "加入交流群"
        AppLanguage.ENGLISH -> "Join Community"
        AppLanguage.ARABIC -> "انضم إلى المجتمع"
    }

    val contactAuthorDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "问题反馈、合作咨询、功能建议 💬"
        AppLanguage.ENGLISH -> "Feedback, collaboration, feature suggestions 💬"
        AppLanguage.ARABIC -> "ملاحظات، تعاون، اقتراحات الميزات 💬"
    }

    val welcomeStarSupport: String get() = when (lang) {
        AppLanguage.CHINESE -> "欢迎 Star ⭐ 支持一下！"
        AppLanguage.ENGLISH -> "Welcome to Star ⭐ and support!"
        AppLanguage.ARABIC -> "مرحبًا بك في Star ⭐ والدعم!"
    }

    val changelog: String get() = when (lang) {
        AppLanguage.CHINESE -> "更新日志"
        AppLanguage.ENGLISH -> "Changelog"
        AppLanguage.ARABIC -> "سجل التغييرات"
    }

    val latestTag: String get() = when (lang) {
        AppLanguage.CHINESE -> "最新"
        AppLanguage.ENGLISH -> "Latest"
        AppLanguage.ARABIC -> "الأحدث"
    }

    val newVersionFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "发现新版本"
        AppLanguage.ENGLISH -> "New Version Found"
        AppLanguage.ARABIC -> "تم العثور على إصدار جديد"
    }

    val updateRecommendation: String get() = when (lang) {
        AppLanguage.CHINESE -> "建议更新到最新版本以获得更好的体验"
        AppLanguage.ENGLISH -> "Recommend updating to the latest version for better experience"
        AppLanguage.ARABIC -> "يوصى بالتحديث إلى أحدث إصدار للحصول على تجربة أفضل"
    }

    val currentVersionIs: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前版本 v%s 已是最新版本"
        AppLanguage.ENGLISH -> "Current version v%s is already the latest"
        AppLanguage.ARABIC -> "الإصدار الحالي v%s هو الأحدث بالفعل"
    }

    val openAction: String get() = when (lang) {
        AppLanguage.CHINESE -> "Open"
        AppLanguage.ENGLISH -> "Open"
        AppLanguage.ARABIC -> "فتح"
    }

    val qqGroupLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "QQ 群"
        AppLanguage.ENGLISH -> "QQ Group"
        AppLanguage.ARABIC -> "مجموعة QQ"
    }

    val telegramGroupLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "Telegram 群"
        AppLanguage.ENGLISH -> "Telegram Group"
        AppLanguage.ARABIC -> "مجموعة Telegram"
    }

    val exchangeLearningUpdates: String get() = when (lang) {
        AppLanguage.CHINESE -> "交流学习、更新消息"
        AppLanguage.ENGLISH -> "Exchange, learn, get updates"
        AppLanguage.ARABIC -> "تبادل، تعلم، احصل على التحديثات"
    }

    val internationalUserGroup: String get() = when (lang) {
        AppLanguage.CHINESE -> "国际用户交流群"
        AppLanguage.ENGLISH -> "International user group"
        AppLanguage.ARABIC -> "مجموعة المستخدمين الدوليين"
    }

    val feedbackConsultation: String get() = when (lang) {
        AppLanguage.CHINESE -> "问题反馈、合作咨询"
        AppLanguage.ENGLISH -> "Feedback, consultation"
        AppLanguage.ARABIC -> "ملاحظات، استشارة"
    }

    val internationalAccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "国际访问"
        AppLanguage.ENGLISH -> "International access"
        AppLanguage.ARABIC -> "الوصول الدولي"
    }
    // ==================== Scheme Management ====================
    val quickSchemes: String get() = when (lang) {
        AppLanguage.CHINESE -> "快捷方案"
        AppLanguage.ENGLISH -> "Quick Schemes"
        AppLanguage.ARABIC -> "مخططات سريعة"
    }

    val allSchemesBtn: String get() = when (lang) {
        AppLanguage.CHINESE -> "全部方案"
        AppLanguage.ENGLISH -> "All Schemes"
        AppLanguage.ARABIC -> "جميع المخططات"
    }

    val builtInSchemes: String get() = when (lang) {
        AppLanguage.CHINESE -> "📦 内置方案"
        AppLanguage.ENGLISH -> "📦 Built-in Schemes"
        AppLanguage.ARABIC -> "📦 مخططات مدمجة"
    }

    val mySchemes: String get() = when (lang) {
        AppLanguage.CHINESE -> "⭐ 我的方案"
        AppLanguage.ENGLISH -> "⭐ My Schemes"
        AppLanguage.ARABIC -> "⭐ مخططاتي"
    }

    val schemeTip: String get() = when (lang) {
        AppLanguage.CHINESE -> "💡 提示：选择模块后点击「存为方案」可保存自定义方案"
        AppLanguage.ENGLISH -> "💡 Tip: Select modules and click 'Save as Scheme' to save custom scheme"
        AppLanguage.ARABIC -> "💡 نصيحة: حدد الوحدات وانقر على 'حفظ كمخطط' لحفظ مخطط مخصص"
    }

    val containsModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "包含 %d 个模块"
        AppLanguage.ENGLISH -> "Contains %d modules"
        AppLanguage.ARABIC -> "يحتوي على %d وحدات"
    }

    val schemeNameLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "方案名称"
        AppLanguage.ENGLISH -> "Scheme Name"
        AppLanguage.ARABIC -> "اسم المخطط"
    }

    val enterSchemeNameHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入方案名称"
        AppLanguage.ENGLISH -> "Enter scheme name"
        AppLanguage.ARABIC -> "أدخل اسم المخطط"
    }

    val descriptionOptionalLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "描述（可选）"
        AppLanguage.ENGLISH -> "Description (Optional)"
        AppLanguage.ARABIC -> "الوصف (اختياري)"
    }

    val optionalLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "可选"
        AppLanguage.ENGLISH -> "Optional"
        AppLanguage.ARABIC -> "اختياري"
    }

    val briefDescribeSchemeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "简要描述方案用途"
        AppLanguage.ENGLISH -> "Briefly describe scheme purpose"
        AppLanguage.ARABIC -> "وصف موجز لغرض المخطط"
    }

    val willSaveModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "将保存 %d 个模块到此方案"
        AppLanguage.ENGLISH -> "Will save %d modules to this scheme"
        AppLanguage.ARABIC -> "سيتم حفظ %d وحدات في هذا المخطط"
    }

    val selectIconTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "Select icon"
        AppLanguage.ENGLISH -> "Select Icon"
        AppLanguage.ARABIC -> "اختيار أيقونة"
    }
    // ==================== App Categories ====================
    val allApps: String get() = when (lang) {
        AppLanguage.CHINESE -> "全部"
        AppLanguage.ENGLISH -> "All"
        AppLanguage.ARABIC -> "الكل"
    }

    val uncategorized: String get() = when (lang) {
        AppLanguage.CHINESE -> "未分类"
        AppLanguage.ENGLISH -> "Uncategorized"
        AppLanguage.ARABIC -> "غير مصنف"
    }

    val addCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加分类"
        AppLanguage.ENGLISH -> "Add Category"
        AppLanguage.ARABIC -> "إضافة تصنيف"
    }

    val editCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑分类"
        AppLanguage.ENGLISH -> "Edit Category"
        AppLanguage.ARABIC -> "تعديل التصنيف"
    }

    val deleteCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除分类"
        AppLanguage.ENGLISH -> "Delete Category"
        AppLanguage.ARABIC -> "حذف التصنيف"
    }

    val categoryName: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类名称"
        AppLanguage.ENGLISH -> "Category Name"
        AppLanguage.ARABIC -> "اسم التصنيف"
    }

    val categoryNamePlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入分类名称"
        AppLanguage.ENGLISH -> "Enter category name"
        AppLanguage.ARABIC -> "أدخل اسم التصنيف"
    }

    val categoryIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类图标"
        AppLanguage.ENGLISH -> "Category Icon"
        AppLanguage.ARABIC -> "أيقونة التصنيف"
    }

    val categoryColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类颜色"
        AppLanguage.ENGLISH -> "Category Color"
        AppLanguage.ARABIC -> "لون التصنيف"
    }

    val categoryNameRequired: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入分类名称"
        AppLanguage.ENGLISH -> "Please enter category name"
        AppLanguage.ARABIC -> "الرجاء إدخال اسم التصنيف"
    }

    val moveToCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "移动到分类"
        AppLanguage.ENGLISH -> "Move to Category"
        AppLanguage.ARABIC -> "نقل إلى تصنيف"
    }

    val deleteCategoryConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定删除此分类吗？该分类下的应用将变为未分类。"
        AppLanguage.ENGLISH -> "Delete this category? Apps in this category will become uncategorized."
        AppLanguage.ARABIC -> "حذف هذا التصنيف؟ ستصبح التطبيقات في هذا التصنيف غير مصنفة."
    }

    val longPressToEdit: String get() = when (lang) {
        AppLanguage.CHINESE -> "长按编辑或删除"
        AppLanguage.ENGLISH -> "Long press to edit or delete"
        AppLanguage.ARABIC -> "اضغط مطولاً للتعديل أو الحذف"
    }
    // ==================== About Page - ====================
    val legalDisclaimerTitle1: String get() = when (lang) {
        AppLanguage.CHINESE -> "一、软件性质与用途"
        AppLanguage.ENGLISH -> "1. Nature and Purpose of Software"
        AppLanguage.ARABIC -> "أولاً: طبيعة وغرض البرنامج"
    }

    val legalDisclaimerContent1: String get() = when (lang) {
        AppLanguage.CHINESE -> "本软件为开源技术研究与教育演示工具，所有功能均基于 Android 系统公开 API 实现，旨在展示移动应用开发技术。本软件不鼓励、不支持任何非法用途。"
        AppLanguage.ENGLISH -> "This software is an open-source technology research and educational demonstration tool. All features are implemented based on Android system public APIs to demonstrate mobile application development technology. This software does not encourage or support any illegal use."
        AppLanguage.ARABIC -> "هذا البرنامج هو أداة بحث وتقنية تعليمية مفتوحة المصدر. جميع الميزات مطبقة بناءً على واجهات برمجة التطبيقات العامة لنظام Android لإظهار تقنية تطوير التطبيقات المحمولة. هذا البرنامج لا يشجع أو يدعم أي استخدام غير قانوني."
    }

    val legalDisclaimerTitle2: String get() = when (lang) {
        AppLanguage.CHINESE -> "二、用户责任与义务"
        AppLanguage.ENGLISH -> "2. User Responsibilities and Obligations"
        AppLanguage.ARABIC -> "ثانياً: مسؤوليات والتزامات المستخدم"
    }

    val legalDisclaimerContent2: String get() = when (lang) {
        AppLanguage.CHINESE -> "用户应确保在合法、正当的场景下使用本软件，包括但不限于：自我管理用于个人专注力训练、学习时间管理；企业展示用于展会、商场等场景的展示终端；家长监护在未成年人知情同意下的合理使用；教育研究用于技术学习和安全研究。严禁将本软件用于任何侵犯他人人身自由、隐私权、财产权等合法权益的行为。"
        AppLanguage.ENGLISH -> "Users should ensure to use this software in legal and proper scenarios, including but not limited to: self-management for personal focus training and learning time management; enterprise display for exhibitions and shopping malls; parental supervision with informed consent of minors; educational research for technology learning and security research. It is strictly forbidden to use this software for any acts that infringe upon the legitimate rights and interests of others such as personal freedom, privacy, and property rights."
        AppLanguage.ARABIC -> "يجب على المستخدمين ضمان استخدام هذا البرنامج في سيناريوهات قانونية ومناسبة، بما في ذلك على سبيل المثال لا الحصر: الإدارة الذاتية للتدريب على التركيز الشخصي وإدارة وقت التعلم؛ عرض المؤسسات للمعارض ومراكز التسوق؛ إشراف الوالدين بموافقة قاصر؛ البحث التعليمي لتعلم التعلم والأمن البحثي. يمنع منعا باتا استخدام هذا البرنامج لأي أفعال تنتهك الحقوق المشروعة للآخرين مثل الحرية الشخصية والخصوصية وحقوق الملكية."
    }

    val legalDisclaimerTitle3: String get() = when (lang) {
        AppLanguage.CHINESE -> "三、高级功能特别声明"
        AppLanguage.ENGLISH -> "3. Special Declaration for Advanced Features"
        AppLanguage.ARABIC -> "ثالثاً: إعلان خاص للميزات المتقدمة"
    }

    val legalDisclaimerContent3: String get() = when (lang) {
        AppLanguage.CHINESE -> "本软件包含的「强制运行」及相关硬件控制功能（以下简称「高级功能」）属于技术演示性质：1.【知情同意原则】高级功能仅应在设备所有者或使用者完全知情并明确同意的情况下启用；2.【自主控制原则】所有功能均提供紧急退出机制，用户可通过密码随时终止；3.【技术中立原则】功能本身不具有违法性，其合法性取决于使用者的具体使用方式和目的；4.【风险自担原则】启用高级功能可能造成设备发热、电池消耗加快等情况，用户需自行承担相关风险。"
        AppLanguage.ENGLISH -> "The 'Forced Run' and related hardware control functions included in this software (hereinafter referred to as 'Advanced Features') are for technical demonstration purposes: 1. [Informed Consent Principle] Advanced features should only be enabled with full knowledge and explicit consent of the device owner or user; 2. [Autonomous Control Principle] All functions provide emergency exit mechanisms, users can terminate at any time through password; 3. [Technology Neutrality Principle] The functions themselves are not illegal, their legality depends on the specific use and purpose of the user; 4. [Risk Assumption Principle] Enabling advanced features may cause device heating, faster battery consumption, etc., users need to bear relevant risks."
        AppLanguage.ARABIC -> "وظائف 'التشغيل القسري' والتحكم بالأجهزة ذات الصلة المدرجة في هذا البرنامج (ويشار إليها فيما يلي باسم 'الميزات المتقدمة') هي لأغراض العرض التقني: 1. [مبدأ الموافقة المستنيرة] يجب تمكين الميزات المتقدمة فقط بمعرفة وموافقة صريحة من مالك الجهاز أو المستخدم؛ 2. [مبدأ التحكم الذاتي] جميع الوظائف توفر آليات خروج طارئة، يمكن للمستخدمين الإنهاء في أي وقت من خلال كلمة المرور؛ 3. [مبدأ الحياد التقني] الوظائف نفسها ليست غير قانونية، قانونيتها تعتمد على الاستخدام والغرض المحدد للمستخدم؛ 4. [مبدأ افتراض المخاطر] تمكين الميزات المتقدمة قد يسبب سخونة الجهاز، استهلاك البطارية أسرع، إلخ، يحتاج المستخدمون لتحمل المخاطر ذات الصلة."
    }

    val legalDisclaimerTitle4: String get() = when (lang) {
        AppLanguage.CHINESE -> "四、免责条款"
        AppLanguage.ENGLISH -> "4. Disclaimer Clauses"
        AppLanguage.ARABIC -> "رابعاً: بنود إخلاء المسؤولية"
    }

    val legalDisclaimerContent4: String get() = when (lang) {
        AppLanguage.CHINESE -> "1.本软件按「现状」提供，开发者不对软件的适用性、可靠性、安全性作任何明示或暗示的保证；2.用户因违反法律法规或本声明使用本软件所产生的一切法律责任，由用户自行承担，与开发者无关；3.开发者不对因使用本软件导致的任何直接、间接、偶然、特殊或惩罚性损害承担责任；4.任何第三方利用本软件源代码进行的修改、分发行为，其法律责任由该第三方自行承担。"
        AppLanguage.ENGLISH -> "1. This software is provided 'as is', developers make no express or implied warranties regarding the suitability, reliability, or security of the software; 2. All legal liabilities arising from users' use of this software in violation of laws and regulations or this declaration shall be borne by users themselves and have nothing to do with developers; 3. Developers are not responsible for any direct, indirect, incidental, special or punitive damages caused by the use of this software; 4. Any modification or distribution behavior by third parties using the source code of this software shall be borne by the third party itself."
        AppLanguage.ARABIC -> "1. يتم توفير هذا البرنامج 'كما هو'، لا يقدم المطورون أي ضمانات صريحة أو ضمنية فيما يتعلق بملاءمة أو موثوقية أو أمان البرنامج؛ 2. جميع المسؤوليات القانونية الناشئة عن استخدام المستخدمين لهذا البرنامج انتهاكاً للقوانين واللوائح أو هذا الإعلان يتحملها المستخدمون أنفسهم وليس لها علاقة بالمطورين؛ 3. المطورون ليسوا مسؤولين عن أي أضرار مباشرة أو غير مباشرة أو عرضية أو خاصة أو عقابية ناجمة عن استخدام هذا البرنامج؛ 4. أي تعديل أو توزيع من قبل أطراف ثالثة باستخدام الكود المصدري لهذا البرنامج يتحمل الطرف الثالث نفسه."
    }

    val legalDisclaimerTitle5: String get() = when (lang) {
        AppLanguage.CHINESE -> "五、合规使用指引"
        AppLanguage.ENGLISH -> "5. Compliance Usage Guidelines"
        AppLanguage.ARABIC -> "خامساً: إرشادات الاستخدام المتوافق"
    }

    val legalDisclaimerContent5: String get() = when (lang) {
        AppLanguage.CHINESE -> "为确保合法合规使用，建议用户：在使用前获取设备实际使用者的书面或电子形式同意；在企业场景下制定相应的使用规范和管理制度；在教育场景下确保符合相关教育法规要求；定期检查并遵守当地法律法规的最新要求。"
        AppLanguage.ENGLISH -> "To ensure legal and compliant use, users are advised to: obtain written or electronic consent from actual device users before use; formulate corresponding usage specifications and management systems in enterprise scenarios; ensure compliance with relevant education regulatory requirements in educational scenarios; regularly check and comply with the latest requirements of local laws and regulations."
        AppLanguage.ARABIC -> "لضمان الاستخدام القانوني والمتوافق، ينصح المستخدمون بـ: الحصول على موافقة خطية أو إلكترونية من مستخدمي الجهاز الفعليين قبل الاستخدام؛ صياغة مواصفات الاستخدام وأنظمة الإدارة المقابلة في سيناريوهات المؤسسات؛ ضمان الامتثال لمتطلبات اللوائح التعليمية ذات الصلة في السيناريوهات التعليمية؛ التحقق بانتظام والامتثال لأحدث متطلبات القوانين واللوائح المحلية."
    }

    val legalDisclaimerTitle6: String get() = when (lang) {
        AppLanguage.CHINESE -> "六、知识产权声明"
        AppLanguage.ENGLISH -> "6. Intellectual Property Declaration"
        AppLanguage.ARABIC -> "سادساً: إعلان الملكية الفكرية"
    }

    val legalDisclaimerContent6: String get() = when (lang) {
        AppLanguage.CHINESE -> "本软件基于 The Unlicense 发布，已明确贡献到公共领域。任何人均可为任何目的自由使用、修改、分发或出售本软件。本软件按“现状”提供，不附带任何形式的明示或默示担保。"
        AppLanguage.ENGLISH -> "This software is released under The Unlicense and dedicated to the public domain. Anyone may use, modify, distribute, or sell it for any purpose. The software is provided \"as is\", without warranty of any kind."
        AppLanguage.ARABIC -> "هذا البرنامج منشور بموجب The Unlicense ومُهدى إلى الملكية العامة. يحق لأي شخص استخدامه أو تعديله أو توزيعه أو بيعه لأي غرض. يتم توفير البرنامج \"كما هو\" دون أي ضمان من أي نوع."
    }

        val legalDisclaimerAcceptance: String get() = when (lang) {
        AppLanguage.CHINESE -> "继续使用本软件即表示您：已年满 18 周岁或已获得法定监护人同意；已完整阅读并理解上述所有条款；同意遵守所有使用条款和当地法律法规；自愿承担使用本软件可能产生的一切风险和责任。"
        AppLanguage.ENGLISH -> "Continuing to use this software means that you: are 18 years of age or older or have obtained consent from a legal guardian; have read and understood all the above terms; agree to comply with all terms of use and local laws and regulations; voluntarily assume all risks and liabilities that may arise from the use of this software."
        AppLanguage.ARABIC -> "متابعة استخدام هذا البرنامج تعني أنك: تبلغ 18 عاماً أو أكثر أو حصلت على موافقة من الوصي القانوني؛ قد قرأت وفهمت جميع الشروط المذكورة أعلاه؛ توافق على الامتثال لجميع شروط الاستخدام والقوانين واللوائح المحلية؛ تتطوع لتحمل جميع المخاطر والمسؤوليات التي قد تنشأ عن استخدام هذا البرنامج."
    }

    val legalDisclaimerFooter: String get() = when (lang) {
        AppLanguage.CHINESE -> "本声明自发布之日起生效，开发者保留随时修改本声明的权利。最后更新：2026 年 1 月"
        AppLanguage.ENGLISH -> "This declaration takes effect from the date of release. Developers reserve the right to modify this declaration at any time. Last updated: January 2026"
        AppLanguage.ARABIC -> "يبدأ سريان هذا الإعلان من تاريخ الإصدار. يحتفظ المطورون بالحق في تعديل هذا الإعلان في أي وقت. آخر تحديث: يناير 2026"
    }

    val madeWithLove: String get() = when (lang) {
        AppLanguage.CHINESE -> "用心制作 by Shiaho"
        AppLanguage.ENGLISH -> "Made with love by Shiaho"
        AppLanguage.ARABIC -> "صنع بحب بواسطة Shiaho"
    }
    // ==================== Hint Messages ====================
    val msgAppCreated: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用创建成功"
        AppLanguage.ENGLISH -> "App created successfully"
        AppLanguage.ARABIC -> "تم إنشاء التطبيق بنجاح"
    }

    val msgAppDeleted: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用已删除"
        AppLanguage.ENGLISH -> "App deleted"
        AppLanguage.ARABIC -> "تم حذف التطبيق"
    }

    val msgLoading: String get() = when (lang) {
        AppLanguage.CHINESE -> "加载中..."
        AppLanguage.ENGLISH -> "Loading..."
        AppLanguage.ARABIC -> "جاري التحميل..."
    }

    val msgNoApps: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无应用"
        AppLanguage.ENGLISH -> "No apps yet"
        AppLanguage.ARABIC -> "لا توجد تطبيقات بعد"
    }

    val msgLanguageChanged: String get() = when (lang) {
        AppLanguage.CHINESE -> "语言已更改"
        AppLanguage.ENGLISH -> "Language changed"
        AppLanguage.ARABIC -> "تم تغيير اللغة"
    }

    val msgExportSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "APK导出成功"
        AppLanguage.ENGLISH -> "APK exported successfully"
        AppLanguage.ARABIC -> "تم تصدير APK بنجاح"
    }

    val msgExportFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "APK导出失败"
        AppLanguage.ENGLISH -> "APK export failed"
        AppLanguage.ARABIC -> "فشل تصدير APK"
    }

    val msgImportSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入成功"
        AppLanguage.ENGLISH -> "Import successful"
        AppLanguage.ARABIC -> "تم الاستيراد بنجاح"
    }

    val msgImportFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入失败"
        AppLanguage.ENGLISH -> "Import failed"
        AppLanguage.ARABIC -> "فشل الاستيراد"
    }

    val msgCopied: String get() = when (lang) {
        AppLanguage.CHINESE -> "已复制"
        AppLanguage.ENGLISH -> "Copied"
        AppLanguage.ARABIC -> "تم النسخ"
    }

    val msgDeleted: String get() = when (lang) {
        AppLanguage.CHINESE -> "已删除"
        AppLanguage.ENGLISH -> "Deleted"
        AppLanguage.ARABIC -> "تم الحذف"
    }
    // ==================== Theme Settings ====================
    val themeSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "主题设置"
        AppLanguage.ENGLISH -> "Theme Settings"
        AppLanguage.ARABIC -> "إعدادات السمة"
    }

    val theme: String get() = when (lang) {
        AppLanguage.CHINESE -> "主题"
        AppLanguage.ENGLISH -> "Theme"
        AppLanguage.ARABIC -> "السمة"
    }

    val effects: String get() = when (lang) {
        AppLanguage.CHINESE -> "效果"
        AppLanguage.ENGLISH -> "Effects"
        AppLanguage.ARABIC -> "التأثيرات"
    }

    val selectUiStyle: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择界面视觉风格"
        AppLanguage.ENGLISH -> "Select UI visual style"
        AppLanguage.ARABIC -> "اختر نمط واجهة المستخدم المرئي"
    }

    val darkMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "深色模式"
        AppLanguage.ENGLISH -> "Dark Mode"
        AppLanguage.ARABIC -> "الوضع الداكن"
    }

    val followSystem: String get() = when (lang) {
        AppLanguage.CHINESE -> "跟随系统"
        AppLanguage.ENGLISH -> "Follow System"
        AppLanguage.ARABIC -> "اتباع النظام"
    }

    val followSystemHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "根据系统设置自动切换"
        AppLanguage.ENGLISH -> "Auto switch based on system settings"
        AppLanguage.ARABIC -> "التبديل التلقائي بناءً على إعدادات النظام"
    }

    val lightMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "浅色模式"
        AppLanguage.ENGLISH -> "Light Mode"
        AppLanguage.ARABIC -> "الوضع الفاتح"
    }

    val lightModeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "始终使用浅色主题"
        AppLanguage.ENGLISH -> "Always use light theme"
        AppLanguage.ARABIC -> "استخدام السمة الفاتحة دائمًا"
    }

    val darkModeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "始终使用深色主题"
        AppLanguage.ENGLISH -> "Always use dark theme"
        AppLanguage.ARABIC -> "استخدام السمة الداكنة دائمًا"
    }

    val colorScheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "配色方案"
        AppLanguage.ENGLISH -> "Color Scheme"
        AppLanguage.ARABIC -> "نظام الألوان"
    }

    val themeFeatures: String get() = when (lang) {
        AppLanguage.CHINESE -> "主题特性"
        AppLanguage.ENGLISH -> "Theme Features"
        AppLanguage.ARABIC -> "ميزات السمة"
    }
    // ==================== Theme Settings Strings ====================
    val animationDisabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "动画已禁用"
        AppLanguage.ENGLISH -> "Animation disabled"
        AppLanguage.ARABIC -> "الرسوم المتحركة معطلة"
    }

    val holdToExperience: String get() = when (lang) {
        AppLanguage.CHINESE -> "按住体验"
        AppLanguage.ENGLISH -> "Hold to experience"
        AppLanguage.ARABIC -> "اضغط مع الاستمرار للتجربة"
    }

    val primaryColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "主色"
        AppLanguage.ENGLISH -> "Primary"
        AppLanguage.ARABIC -> "اللون الأساسي"
    }

    val secondaryColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "次色"
        AppLanguage.ENGLISH -> "Secondary"
        AppLanguage.ARABIC -> "اللون الثانوي"
    }

    val accentColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "强调"
        AppLanguage.ENGLISH -> "Accent"
        AppLanguage.ARABIC -> "لون التمييز"
    }

    val animationStyle: String get() = when (lang) {
        AppLanguage.CHINESE -> "动画风格"
        AppLanguage.ENGLISH -> "Animation Style"
        AppLanguage.ARABIC -> "نمط الرسوم المتحركة"
    }

    val interactionMethod: String get() = when (lang) {
        AppLanguage.CHINESE -> "交互方式"
        AppLanguage.ENGLISH -> "Interaction Method"
        AppLanguage.ARABIC -> "طريقة التفاعل"
    }

    val cornerRadius: String get() = when (lang) {
        AppLanguage.CHINESE -> "圆角大小"
        AppLanguage.ENGLISH -> "Corner Radius"
        AppLanguage.ARABIC -> "نصف قطر الزاوية"
    }

    val glowEffect: String get() = when (lang) {
        AppLanguage.CHINESE -> "发光效果"
        AppLanguage.ENGLISH -> "Glow Effect"
        AppLanguage.ARABIC -> "تأثير التوهج"
    }

    val particleEffect: String get() = when (lang) {
        AppLanguage.CHINESE -> "粒子效果"
        AppLanguage.ENGLISH -> "Particle Effect"
        AppLanguage.ARABIC -> "تأثير الجسيمات"
    }

    val glassmorphism: String get() = when (lang) {
        AppLanguage.CHINESE -> "玻璃拟态"
        AppLanguage.ENGLISH -> "Glassmorphism"
        AppLanguage.ARABIC -> "تأثير الزجاج"
    }
}
