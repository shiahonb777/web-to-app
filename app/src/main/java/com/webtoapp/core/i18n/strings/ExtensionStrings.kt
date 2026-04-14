package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object ExtensionStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage
    // ==================== Built-in Modules ====================
    val builtinVideoDownloader: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频下载"
        AppLanguage.ENGLISH -> "Video Download"
        AppLanguage.ARABIC -> "تحميل الفيديو"
    }

    val builtinVideoDownloaderDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动检测网页视频，支持 MP4 和 Blob 流下载"
        AppLanguage.ENGLISH -> "Auto-detect web videos, supports MP4 and Blob stream download"
        AppLanguage.ARABIC -> "الكشف التلقائي عن الفيديو، يدعم تحميل MP4 وBlob"
    }

    val builtinDouyinExtractor: String get() = when (lang) {
        AppLanguage.CHINESE -> "抖音视频"
        AppLanguage.ENGLISH -> "Douyin Video"
        AppLanguage.ARABIC -> "فيديو دوين"
    }

    val builtinDouyinExtractorDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "提取抖音无水印视频地址"
        AppLanguage.ENGLISH -> "Extract Douyin watermark-free video URL"
        AppLanguage.ARABIC -> "استخراج رابط فيديو دوين بدون علامة مائية"
    }

    val builtinXiaohongshuExtractor: String get() = when (lang) {
        AppLanguage.CHINESE -> "小红书"
        AppLanguage.ENGLISH -> "Xiaohongshu"
        AppLanguage.ARABIC -> "شياوهونغشو"
    }

    val builtinXiaohongshuExtractorDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "提取小红书图片和视频"
        AppLanguage.ENGLISH -> "Extract Xiaohongshu images and videos"
        AppLanguage.ARABIC -> "استخراج صور وفيديوهات شياوهونغشو"
    }

    val builtinVideoEnhancer: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频增强"
        AppLanguage.ENGLISH -> "Video Enhance"
        AppLanguage.ARABIC -> "تحسين الفيديو"
    }

    val builtinVideoEnhancerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "倍速播放、画中画、后台播放"
        AppLanguage.ENGLISH -> "Speed control, picture-in-picture, background play"
        AppLanguage.ARABIC -> "التحكم في السرعة، صورة داخل صورة، التشغيل في الخلفية"
    }

    val builtinWebAnalyzer: String get() = when (lang) {
        AppLanguage.CHINESE -> "网页分析"
        AppLanguage.ENGLISH -> "Web Analyzer"
        AppLanguage.ARABIC -> "محلل الويب"
    }

    val builtinWebAnalyzerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "查看页面元素、网络请求、性能数据"
        AppLanguage.ENGLISH -> "View page elements, network requests, performance data"
        AppLanguage.ARABIC -> "عرض عناصر الصفحة، طلبات الشبكة، بيانات الأداء"
    }

    val builtinDarkMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "深色模式"
        AppLanguage.ENGLISH -> "Dark Mode"
        AppLanguage.ARABIC -> "الوضع الداكن"
    }

    val builtinDarkModeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "智能反色，护眼阅读"
        AppLanguage.ENGLISH -> "Smart inversion, eye-friendly reading"
        AppLanguage.ARABIC -> "عكس ذكي، قراءة مريحة للعين"
    }

    val builtinPrivacyProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐私保护"
        AppLanguage.ENGLISH -> "Privacy Protection"
        AppLanguage.ARABIC -> "حماية الخصوصية"
    }

    val builtinPrivacyProtectionDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止追踪、清理指纹、保护隐私"
        AppLanguage.ENGLISH -> "Block tracking, clear fingerprints, protect privacy"
        AppLanguage.ARABIC -> "حظر التتبع، مسح البصمات، حماية الخصوصية"
    }

    val builtinContentEnhancer: String get() = when (lang) {
        AppLanguage.CHINESE -> "内容增强"
        AppLanguage.ENGLISH -> "Content Enhance"
        AppLanguage.ARABIC -> "تحسين المحتوى"
    }

    val builtinContentEnhancerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "解除复制限制、翻译、长截图"
        AppLanguage.ENGLISH -> "Remove copy restrictions, translate, long screenshot"
        AppLanguage.ARABIC -> "إزالة قيود النسخ، الترجمة، لقطة شاشة طويلة"
    }

    val builtinElementBlocker: String get() = when (lang) {
        AppLanguage.CHINESE -> "元素屏蔽器"
        AppLanguage.ENGLISH -> "Element Blocker"
        AppLanguage.ARABIC -> "مانع العناصر"
    }

    val builtinElementBlockerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "单击选择元素，双击屏蔽，去除页面烦人元素"
        AppLanguage.ENGLISH -> "Click to select, double-click to block annoying elements"
        AppLanguage.ARABIC -> "انقر للتحديد، انقر مرتين لحظر العناصر المزعجة"
    }
    // ==================== Module Triggers ====================
    val triggerAuto: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动执行"
        AppLanguage.ENGLISH -> "Auto Execute"
        AppLanguage.ARABIC -> "تنفيذ تلقائي"
    }

    val triggerAutoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面加载时自动执行"
        AppLanguage.ENGLISH -> "Execute automatically when page loads"
        AppLanguage.ARABIC -> "التنفيذ تلقائياً عند تحميل الصفحة"
    }

    val triggerManual: String get() = when (lang) {
        AppLanguage.CHINESE -> "手动触发"
        AppLanguage.ENGLISH -> "Manual Trigger"
        AppLanguage.ARABIC -> "تشغيل يدوي"
    }

    val triggerManualDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "需要用户手动触发执行"
        AppLanguage.ENGLISH -> "Requires manual trigger by user"
        AppLanguage.ARABIC -> "يتطلب تشغيل يدوي من المستخدم"
    }

    val triggerInterval: String get() = when (lang) {
        AppLanguage.CHINESE -> "定时执行"
        AppLanguage.ENGLISH -> "Interval Execute"
        AppLanguage.ARABIC -> "تنفيذ دوري"
    }

    val triggerIntervalDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "按设定间隔定时执行"
        AppLanguage.ENGLISH -> "Execute at set intervals"
        AppLanguage.ARABIC -> "التنفيذ على فترات محددة"
    }

    val triggerMutation: String get() = when (lang) {
        AppLanguage.CHINESE -> "DOM变化"
        AppLanguage.ENGLISH -> "DOM Mutation"
        AppLanguage.ARABIC -> "تغيير DOM"
    }

    val triggerMutationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测到DOM变化时执行"
        AppLanguage.ENGLISH -> "Execute when DOM changes detected"
        AppLanguage.ARABIC -> "التنفيذ عند اكتشاف تغييرات DOM"
    }

    val triggerScroll: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动触发"
        AppLanguage.ENGLISH -> "Scroll Trigger"
        AppLanguage.ARABIC -> "تشغيل بالتمرير"
    }

    val triggerScrollDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面滚动时执行"
        AppLanguage.ENGLISH -> "Execute when page scrolls"
        AppLanguage.ARABIC -> "التنفيذ عند تمرير الصفحة"
    }

    val triggerClick: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击触发"
        AppLanguage.ENGLISH -> "Click Trigger"
        AppLanguage.ARABIC -> "تشغيل بالنقر"
    }

    val triggerClickDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击指定元素时执行"
        AppLanguage.ENGLISH -> "Execute when element clicked"
        AppLanguage.ARABIC -> "التنفيذ عند النقر على العنصر"
    }

    val triggerHover: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬停触发"
        AppLanguage.ENGLISH -> "Hover Trigger"
        AppLanguage.ARABIC -> "تشغيل بالتمرير"
    }

    val triggerHoverDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "鼠标悬停时执行"
        AppLanguage.ENGLISH -> "Execute on mouse hover"
        AppLanguage.ARABIC -> "التنفيذ عند تمرير الماوس"
    }

    val triggerFocus: String get() = when (lang) {
        AppLanguage.CHINESE -> "聚焦触发"
        AppLanguage.ENGLISH -> "Focus Trigger"
        AppLanguage.ARABIC -> "تشغيل بالتركيز"
    }

    val triggerFocusDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "元素获得焦点时执行"
        AppLanguage.ENGLISH -> "Execute when element focused"
        AppLanguage.ARABIC -> "التنفيذ عند تركيز العنصر"
    }

    val triggerInput: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入触发"
        AppLanguage.ENGLISH -> "Input Trigger"
        AppLanguage.ARABIC -> "تشغيل بالإدخال"
    }

    val triggerInputDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "用户输入时执行"
        AppLanguage.ENGLISH -> "Execute on user input"
        AppLanguage.ARABIC -> "التنفيذ عند إدخال المستخدم"
    }

    val triggerVisibility: String get() = when (lang) {
        AppLanguage.CHINESE -> "可见性变化"
        AppLanguage.ENGLISH -> "Visibility Change"
        AppLanguage.ARABIC -> "تغيير الرؤية"
    }

    val triggerVisibilityDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "元素可见性变化时执行"
        AppLanguage.ENGLISH -> "Execute when visibility changes"
        AppLanguage.ARABIC -> "التنفيذ عند تغيير الرؤية"
    }
    // ==================== Extension Module Strings ====================
    val noModuleSelected: String get() = when (lang) {
        AppLanguage.CHINESE -> "未选择模块"
        AppLanguage.ENGLISH -> "No module selected"
        AppLanguage.ARABIC -> "لم يتم اختيار وحدة"
    }

    val addModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加模块"
        AppLanguage.ENGLISH -> "Add Module"
        AppLanguage.ARABIC -> "إضافة وحدة"
    }

    val searchModulesPlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索模块..."
        AppLanguage.ENGLISH -> "Search modules..."
        AppLanguage.ARABIC -> "البحث عن الوحدات..."
    }

    val filterAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "全部"
        AppLanguage.ENGLISH -> "All"
        AppLanguage.ARABIC -> "الكل"
    }

    val filterContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "过滤"
        AppLanguage.ENGLISH -> "Filter"
        AppLanguage.ARABIC -> "تصفية"
    }

    val filterStyle: String get() = when (lang) {
        AppLanguage.CHINESE -> "样式"
        AppLanguage.ENGLISH -> "Style"
        AppLanguage.ARABIC -> "النمط"
    }

    val filterFunction: String get() = when (lang) {
        AppLanguage.CHINESE -> "功能"
        AppLanguage.ENGLISH -> "Function"
        AppLanguage.ARABIC -> "الوظيفة"
    }

    val clearSelection: String get() = when (lang) {
        AppLanguage.CHINESE -> "清空选择"
        AppLanguage.ENGLISH -> "Clear Selection"
        AppLanguage.ARABIC -> "مسح الاختيار"
    }

    val quickEnable: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速启用"
        AppLanguage.ENGLISH -> "Quick Enable"
        AppLanguage.ARABIC -> "تمكين سريع"
    }

    val shareModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享模块"
        AppLanguage.ENGLISH -> "Share Module"
        AppLanguage.ARABIC -> "مشاركة الوحدة"
    }

    val sharePoster: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享海报"
        AppLanguage.ENGLISH -> "Share Poster"
        AppLanguage.ARABIC -> "مشاركة الملصق"
    }

    val savePoster: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存"
        AppLanguage.ENGLISH -> "Save"
        AppLanguage.ARABIC -> "حفظ"
    }

    val sharePosterBtn: String get() = when (lang) {
        AppLanguage.CHINESE -> "Share"
        AppLanguage.ENGLISH -> "Share"
        AppLanguage.ARABIC -> "مشاركة"
    }

    val scanQrToImport: String get() = when (lang) {
        AppLanguage.CHINESE -> "扫描二维码即可导入此扩展模块"
        AppLanguage.ENGLISH -> "Scan QR code to import this extension module"
        AppLanguage.ARABIC -> "امسح رمز QR لاستيراد هذه الوحدة"
    }

    val scanToImportModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "扫码导入模块"
        AppLanguage.ENGLISH -> "Scan to Import"
        AppLanguage.ARABIC -> "امسح للاستيراد"
    }

    val shareModuleFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享模块文件"
        AppLanguage.ENGLISH -> "Share Module File"
        AppLanguage.ARABIC -> "مشاركة ملف الوحدة"
    }

    val shareFileHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "好友收到文件后，在导入模块时选择该文件即可"
        AppLanguage.ENGLISH -> "After receiving the file, select it when importing modules"
        AppLanguage.ARABIC -> "بعد استلام الملف، حدده عند استيراد الوحدات"
    }

    val posterSavedToGallery: String get() = when (lang) {
        AppLanguage.CHINESE -> "海报已保存到相册"
        AppLanguage.ENGLISH -> "Poster saved to gallery"
        AppLanguage.ARABIC -> "تم حفظ الملصق في المعرض"
    }

    val shareModuleText: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享扩展模块「%s」- WebToApp"
        AppLanguage.ENGLISH -> "Share extension module \"%s\" - WebToApp"
        AppLanguage.ARABIC -> "مشاركة وحدة \"%s\" - WebToApp"
    }

    val shareModuleFileSubject: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享扩展模块「%s」"
        AppLanguage.ENGLISH -> "Share extension module \"%s\""
        AppLanguage.ARABIC -> "مشاركة وحدة \"%s\""
    }

    val shareModuleFileText: String get() = when (lang) {
        AppLanguage.CHINESE -> "扩展模块「%s」- WebToApp\n\n在 WebToApp 中导入该文件即可使用"
        AppLanguage.ENGLISH -> "Extension module \"%s\" - WebToApp\n\nImport this file in WebToApp to use"
        AppLanguage.ARABIC -> "وحدة \"%s\" - WebToApp\n\nاستورد هذا الملف في WebToApp للاستخدام"
    }

    val onlyEffectiveOnMatchingSites: String get() = when (lang) {
        AppLanguage.CHINESE -> "仅在 %d 个匹配规则的网站生效"
        AppLanguage.ENGLISH -> "Only effective on %d matching sites"
        AppLanguage.ARABIC -> "فعال فقط على %d مواقع مطابقة"
    }
    // ==================== Module Tags ====================
    val tagVideo: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频"
        AppLanguage.ENGLISH -> "Video"
        AppLanguage.ARABIC -> "فيديو"
    }

    val tagDownload: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载"
        AppLanguage.ENGLISH -> "Download"
        AppLanguage.ARABIC -> "تحميل"
    }

    val tagBilibili: String get() = when (lang) {
        AppLanguage.CHINESE -> "B站"
        AppLanguage.ENGLISH -> "Bilibili"
        AppLanguage.ARABIC -> "بيليبيلي"
    }

    val tagDouyin: String get() = when (lang) {
        AppLanguage.CHINESE -> "抖音"
        AppLanguage.ENGLISH -> "Douyin"
        AppLanguage.ARABIC -> "دوين"
    }

    val tagNoWatermark: String get() = when (lang) {
        AppLanguage.CHINESE -> "无水印"
        AppLanguage.ENGLISH -> "No Watermark"
        AppLanguage.ARABIC -> "بدون علامة مائية"
    }

    val tagXiaohongshu: String get() = when (lang) {
        AppLanguage.CHINESE -> "小红书"
        AppLanguage.ENGLISH -> "Xiaohongshu"
        AppLanguage.ARABIC -> "شياوهونغشو"
    }

    val tagImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片"
        AppLanguage.ENGLISH -> "Image"
        AppLanguage.ARABIC -> "صورة"
    }

    val tagSpeed: String get() = when (lang) {
        AppLanguage.CHINESE -> "倍速"
        AppLanguage.ENGLISH -> "Speed"
        AppLanguage.ARABIC -> "السرعة"
    }

    val tagPiP: String get() = when (lang) {
        AppLanguage.CHINESE -> "画中画"
        AppLanguage.ENGLISH -> "PiP"
        AppLanguage.ARABIC -> "صورة داخل صورة"
    }

    val tagDebug: String get() = when (lang) {
        AppLanguage.CHINESE -> "调试"
        AppLanguage.ENGLISH -> "Debug"
        AppLanguage.ARABIC -> "تصحيح"
    }

    val tagAnalyze: String get() = when (lang) {
        AppLanguage.CHINESE -> "分析"
        AppLanguage.ENGLISH -> "Analyze"
        AppLanguage.ARABIC -> "تحليل"
    }

    val tagDevelop: String get() = when (lang) {
        AppLanguage.CHINESE -> "开发"
        AppLanguage.ENGLISH -> "Develop"
        AppLanguage.ARABIC -> "تطوير"
    }

    val tagDark: String get() = when (lang) {
        AppLanguage.CHINESE -> "深色"
        AppLanguage.ENGLISH -> "Dark"
        AppLanguage.ARABIC -> "داكن"
    }

    val tagEyeCare: String get() = when (lang) {
        AppLanguage.CHINESE -> "护眼"
        AppLanguage.ENGLISH -> "Eye Care"
        AppLanguage.ARABIC -> "حماية العين"
    }

    val tagTheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "主题"
        AppLanguage.ENGLISH -> "Theme"
        AppLanguage.ARABIC -> "المظهر"
    }

    val tagPrivacy: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐私"
        AppLanguage.ENGLISH -> "Privacy"
        AppLanguage.ARABIC -> "الخصوصية"
    }

    val tagSecurity: String get() = when (lang) {
        AppLanguage.CHINESE -> "安全"
        AppLanguage.ENGLISH -> "Security"
        AppLanguage.ARABIC -> "الأمان"
    }

    val tagAntiTrack: String get() = when (lang) {
        AppLanguage.CHINESE -> "反追踪"
        AppLanguage.ENGLISH -> "Anti-Track"
        AppLanguage.ARABIC -> "مكافحة التتبع"
    }

    val tagAd: String get() = when (lang) {
        AppLanguage.CHINESE -> "广告"
        AppLanguage.ENGLISH -> "Ad"
        AppLanguage.ARABIC -> "إعلان"
    }

    val tagElement: String get() = when (lang) {
        AppLanguage.CHINESE -> "元素"
        AppLanguage.ENGLISH -> "Element"
        AppLanguage.ARABIC -> "عنصر"
    }

    val tagCopy: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制"
        AppLanguage.ENGLISH -> "Copy"
        AppLanguage.ARABIC -> "نسخ"
    }

    val tagTranslate: String get() = when (lang) {
        AppLanguage.CHINESE -> "翻译"
        AppLanguage.ENGLISH -> "Translate"
        AppLanguage.ARABIC -> "ترجمة"
    }

    val tagScreenshot: String get() = when (lang) {
        AppLanguage.CHINESE -> "截图"
        AppLanguage.ENGLISH -> "Screenshot"
        AppLanguage.ARABIC -> "لقطة شاشة"
    }
    // ==================== Extension Module Cards ====================
    val saveAsScheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "存为方案"
        AppLanguage.ENGLISH -> "Save as Scheme"
        AppLanguage.ARABIC -> "حفظ كمخطط"
    }

    val clearAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "清空"
        AppLanguage.ENGLISH -> "Clear"
        AppLanguage.ARABIC -> "مسح"
    }

    val selectModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择模块"
        AppLanguage.ENGLISH -> "Select Modules"
        AppLanguage.ARABIC -> "اختيار الوحدات"
    }

    val selectExtensionModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择扩展模块"
        AppLanguage.ENGLISH -> "Select Extension Modules"
        AppLanguage.ARABIC -> "اختيار الوحدات الإضافية"
    }

    val doneWithCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "完成 (%d)"
        AppLanguage.ENGLISH -> "Done (%d)"
        AppLanguage.ARABIC -> "تم (%d)"
    }

    val searchModulesHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索模块名称、描述或标签..."
        AppLanguage.ENGLISH -> "Search module name, description or tags..."
        AppLanguage.ARABIC -> "البحث عن اسم الوحدة أو الوصف أو العلامات..."
    }

    val testModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "测试模块"
        AppLanguage.ENGLISH -> "Test Module"
        AppLanguage.ARABIC -> "اختبار الوحدة"
    }

    val startTest: String get() = when (lang) {
        AppLanguage.CHINESE -> "开始测试"
        AppLanguage.ENGLISH -> "Start Test"
        AppLanguage.ARABIC -> "بدء الاختبار"
    }

    val addThisModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加此模块"
        AppLanguage.ENGLISH -> "Add This Module"
        AppLanguage.ARABIC -> "إضافة هذه الوحدة"
    }

    val allSchemes: String get() = when (lang) {
        AppLanguage.CHINESE -> "全部方案"
        AppLanguage.ENGLISH -> "All Schemes"
        AppLanguage.ARABIC -> "جميع المخططات"
    }

    val saveAsSchemeTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存为方案"
        AppLanguage.ENGLISH -> "Save as Scheme"
        AppLanguage.ARABIC -> "حفظ كمخطط"
    }

    val schemeName: String get() = when (lang) {
        AppLanguage.CHINESE -> "方案名称"
        AppLanguage.ENGLISH -> "Scheme Name"
        AppLanguage.ARABIC -> "اسم المخطط"
    }

    val inputSchemeName: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入方案名称"
        AppLanguage.ENGLISH -> "Enter scheme name"
        AppLanguage.ARABIC -> "أدخل اسم المخطط"
    }

    val descriptionOptional: String get() = when (lang) {
        AppLanguage.CHINESE -> "描述（可选）"
        AppLanguage.ENGLISH -> "Description (optional)"
        AppLanguage.ARABIC -> "الوصف (اختياري)"
    }

    val briefDescriptionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "简要描述方案用途"
        AppLanguage.ENGLISH -> "Brief description of scheme purpose"
        AppLanguage.ARABIC -> "وصف موجز لغرض المخطط"
    }

    val selectIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "Select icon"
        AppLanguage.ENGLISH -> "Select Icon"
        AppLanguage.ARABIC -> "اختيار أيقونة"
    }

    val selectedCountFormat: String get() = when (lang) {
        AppLanguage.CHINESE -> "已选 %d 个"
        AppLanguage.ENGLISH -> "%d selected"
        AppLanguage.ARABIC -> "تم اختيار %d"
    }
    // ==================== Module Editor Extras ====================
    val urlPattern: String get() = when (lang) {
        AppLanguage.CHINESE -> "URL 模式"
        AppLanguage.ENGLISH -> "URL Pattern"
        AppLanguage.ARABIC -> "نمط URL"
    }

    val regexExpression: String get() = when (lang) {
        AppLanguage.CHINESE -> "正则表达式"
        AppLanguage.ENGLISH -> "Regular Expression"
        AppLanguage.ARABIC -> "تعبير نمطي"
    }

    val excludeRule: String get() = when (lang) {
        AppLanguage.CHINESE -> "排除规则"
        AppLanguage.ENGLISH -> "Exclude Rule"
        AppLanguage.ARABIC -> "قاعدة الاستبعاد"
    }

    val noConfigItemsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无配置项\n添加配置项让用户可以自定义模块行为"
        AppLanguage.ENGLISH -> "No config items yet\nAdd config items to let users customize module behavior"
        AppLanguage.ARABIC -> "لا توجد عناصر تكوين بعد\nأضف عناصر تكوين للسماح للمستخدمين بتخصيص سلوك الوحدة"
    }

    val addConfigItem: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加配置项"
        AppLanguage.ENGLISH -> "Add Config Item"
        AppLanguage.ARABIC -> "إضافة عنصر تكوين"
    }

    val keyNamePlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "如: fontSize"
        AppLanguage.ENGLISH -> "e.g. fontSize"
        AppLanguage.ARABIC -> "مثال: fontSize"
    }

    val displayNamePlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "如: 字体大小"
        AppLanguage.ENGLISH -> "e.g. Font Size"
        AppLanguage.ARABIC -> "مثال: حجم الخط"
    }

    val explanationLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "说明"
        AppLanguage.ENGLISH -> "Description"
        AppLanguage.ARABIC -> "الوصف"
    }

    val configExplanationPlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "配置项的说明文字"
        AppLanguage.ENGLISH -> "Description text for the config item"
        AppLanguage.ARABIC -> "نص وصف عنصر التكوين"
    }

    val typeLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "类型"
        AppLanguage.ENGLISH -> "Type"
        AppLanguage.ARABIC -> "النوع"
    }

    val defaultValueLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "默认值"
        AppLanguage.ENGLISH -> "Default Value"
        AppLanguage.ARABIC -> "القيمة الافتراضية"
    }

    val requiredField: String get() = when (lang) {
        AppLanguage.CHINESE -> "必填项"
        AppLanguage.ENGLISH -> "Required"
        AppLanguage.ARABIC -> "مطلوب"
    }

    val selectTemplate: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择模板"
        AppLanguage.ENGLISH -> "Select Template"
        AppLanguage.ARABIC -> "اختر قالب"
    }

    val jsCodePlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "// 在这里编写 JavaScript 代码\nconsole.log('Hello from module!');"
        AppLanguage.ENGLISH -> "// Write JavaScript code here\nconsole.log('Hello from module!');"
        AppLanguage.ARABIC -> "// اكتب كود JavaScript هنا\nconsole.log('Hello from module!');"
    }

    val cssCodePlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "/* 在这里编写 CSS 样式 */\n.ad-banner {\n    display: none !important;\n}"
        AppLanguage.ENGLISH -> "/* Write CSS styles here */\n.ad-banner {\n    display: none !important;\n}"
        AppLanguage.ARABIC -> "/* اكتب أنماط CSS هنا */\n.ad-banner {\n    display: none !important;\n}"
    }
    // ==================== Module Management Errors ====================
    val errModuleNotFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块不存在"
        AppLanguage.ENGLISH -> "Module not found"
        AppLanguage.ARABIC -> "الوحدة غير موجودة"
    }

    val errNoModulesToExport: String get() = when (lang) {
        AppLanguage.CHINESE -> "没有找到要导出的模块"
        AppLanguage.ENGLISH -> "No modules found to export"
        AppLanguage.ARABIC -> "لم يتم العثور على وحدات للتصدير"
    }

    val errInvalidModuleFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "无效的模块文件"
        AppLanguage.ENGLISH -> "Invalid module file"
        AppLanguage.ARABIC -> "ملف وحدة غير صالح"
    }

    val errInvalidShareCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "无效的分享码"
        AppLanguage.ENGLISH -> "Invalid share code"
        AppLanguage.ARABIC -> "رمز مشاركة غير صالح"
    }

    val errInvalidModulePackage: String get() = when (lang) {
        AppLanguage.CHINESE -> "无效的模块包文件"
        AppLanguage.ENGLISH -> "Invalid module package file"
        AppLanguage.ARABIC -> "ملف حزمة وحدات غير صالح"
    }

    val errCannotOpenOutputStream: String get() = when (lang) {
        AppLanguage.CHINESE -> "无法打开输出流"
        AppLanguage.ENGLISH -> "Cannot open output stream"
        AppLanguage.ARABIC -> "لا يمكن فتح تدفق الإخراج"
    }

    val shareModuleTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "📦 WebToApp 扩展模块分享"
        AppLanguage.ENGLISH -> "📦 WebToApp Extension Module Share"
        AppLanguage.ARABIC -> "📦 مشاركة وحدة إضافية WebToApp"
    }

    val shareModuleName: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块名称"
        AppLanguage.ENGLISH -> "Module Name"
        AppLanguage.ARABIC -> "اسم الوحدة"
    }

    val shareModuleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "描述"
        AppLanguage.ENGLISH -> "Description"
        AppLanguage.ARABIC -> "الوصف"
    }

    val shareModuleCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类"
        AppLanguage.ENGLISH -> "Category"
        AppLanguage.ARABIC -> "التصنيف"
    }

    val shareModuleVersion: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本"
        AppLanguage.ENGLISH -> "Version"
        AppLanguage.ARABIC -> "الإصدار"
    }

    val shareModuleCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享码"
        AppLanguage.ENGLISH -> "Share Code"
        AppLanguage.ARABIC -> "رمز المشاركة"
    }

    val shareModuleHowTo: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用方法：复制分享码，在 WebToApp 扩展模块页面点击\"导入\" -> \"从分享码导入\""
        AppLanguage.ENGLISH -> "How to use: Copy the share code, go to WebToApp extension modules page and click \"Import\" -> \"Import from share code\""
        AppLanguage.ARABIC -> "طريقة الاستخدام: انسخ رمز المشاركة، انتقل إلى صفحة وحدات WebToApp الإضافية وانقر على \"استيراد\" -> \"استيراد من رمز المشاركة\""
    }

    val shareModuleSubject: String get() = when (lang) {
        AppLanguage.CHINESE -> "WebToApp 扩展模块"
        AppLanguage.ENGLISH -> "WebToApp Extension Module"
        AppLanguage.ARABIC -> "وحدة إضافية WebToApp"
    }
    // ==================== Module UI Types ====================
    val uiTypeFloatingButton: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮按钮"
        AppLanguage.ENGLISH -> "Floating Button"
        AppLanguage.ARABIC -> "زر عائم"
    }

    val uiTypeFloatingButtonDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "经典悬浮按钮，点击显示模块面板"
        AppLanguage.ENGLISH -> "Classic floating button, tap to show module panel"
        AppLanguage.ARABIC -> "زر عائم كلاسيكي، انقر لإظهار لوحة الوحدة"
    }

    val uiTypeFloatingToolbar: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮工具栏"
        AppLanguage.ENGLISH -> "Floating Toolbar"
        AppLanguage.ARABIC -> "شريط أدوات عائم"
    }

    val uiTypeFloatingToolbarDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "多个按钮排列的工具栏，适合快速操作"
        AppLanguage.ENGLISH -> "Multiple buttons in a row, great for quick actions"
        AppLanguage.ARABIC -> "أزرار متعددة في صف، رائع للإجراءات السريعة"
    }

    val uiTypeSidebar: String get() = when (lang) {
        AppLanguage.CHINESE -> "侧边栏"
        AppLanguage.ENGLISH -> "Sidebar"
        AppLanguage.ARABIC -> "الشريط الجانبي"
    }

    val uiTypeSidebarDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "从屏幕边缘滑出的面板，适合展示列表内容"
        AppLanguage.ENGLISH -> "Panel that slides from edge, good for list content"
        AppLanguage.ARABIC -> "لوحة تنزلق من الحافة، جيدة لمحتوى القائمة"
    }

    val uiTypeBottomBar: String get() = when (lang) {
        AppLanguage.CHINESE -> "底部操作栏"
        AppLanguage.ENGLISH -> "Bottom Bar"
        AppLanguage.ARABIC -> "شريط سفلي"
    }

    val uiTypeBottomBarDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "固定在屏幕底部的操作栏，适合视频控制等"
        AppLanguage.ENGLISH -> "Fixed bar at bottom, great for video controls"
        AppLanguage.ARABIC -> "شريط ثابت في الأسفل، رائع لعناصر تحكم الفيديو"
    }

    val uiTypeFloatingPanel: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮面板"
        AppLanguage.ENGLISH -> "Floating Panel"
        AppLanguage.ARABIC -> "لوحة عائمة"
    }

    val uiTypeFloatingPanelDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "可拖动的悬浮窗口，适合展示复杂内容"
        AppLanguage.ENGLISH -> "Draggable floating window for complex content"
        AppLanguage.ARABIC -> "نافذة عائمة قابلة للسحب للمحتوى المعقد"
    }

    val uiTypeMiniButton: String get() = when (lang) {
        AppLanguage.CHINESE -> "迷你按钮"
        AppLanguage.ENGLISH -> "Mini Button"
        AppLanguage.ARABIC -> "زر صغير"
    }

    val uiTypeMiniButtonDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "更小的悬浮按钮，不影响阅读体验"
        AppLanguage.ENGLISH -> "Smaller floating button, minimal visual impact"
        AppLanguage.ARABIC -> "زر عائم أصغر، تأثير بصري محدود"
    }

    val uiTypeCustom: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义 UI"
        AppLanguage.ENGLISH -> "Custom UI"
        AppLanguage.ARABIC -> "واجهة مخصصة"
    }

    val uiTypeCustomDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "完全自定义 HTML/CSS/JS 实现"
        AppLanguage.ENGLISH -> "Fully customized with HTML/CSS/JS"
        AppLanguage.ARABIC -> "مخصص بالكامل باستخدام HTML/CSS/JS"
    }
    // ==================== Module Timing ====================
    val runTimeDocStart: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面开始"
        AppLanguage.ENGLISH -> "Document Start"
        AppLanguage.ARABIC -> "بداية المستند"
    }

    val runTimeDocStartDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "DOM 未就绪时执行，适合拦截请求和早期修改"
        AppLanguage.ENGLISH -> "Execute before DOM ready, suitable for request interception and early modifications"
        AppLanguage.ARABIC -> "التنفيذ قبل جاهزية DOM، مناسب لاعتراض الطلبات والتعديلات المبكرة"
    }

    val runTimeDocEnd: String get() = when (lang) {
        AppLanguage.CHINESE -> "DOM 就绪"
        AppLanguage.ENGLISH -> "DOM Ready"
        AppLanguage.ARABIC -> "جاهزية DOM"
    }

    val runTimeDocEndDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "DOM 加载完成后执行（推荐），适合大多数场景"
        AppLanguage.ENGLISH -> "Execute after DOM loaded (recommended), suitable for most scenarios"
        AppLanguage.ARABIC -> "التنفيذ بعد تحميل DOM (موصى به)، مناسب لمعظم السيناريوهات"
    }

    val runTimeDocIdle: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面空闲"
        AppLanguage.ENGLISH -> "Page Idle"
        AppLanguage.ARABIC -> "صفحة خاملة"
    }

    val runTimeDocIdleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面完全加载后执行，适合后处理和性能优化"
        AppLanguage.ENGLISH -> "Execute after page fully loaded, suitable for post-processing and performance optimization"
        AppLanguage.ARABIC -> "التنفيذ بعد تحميل الصفحة بالكامل، مناسب للمعالجة اللاحقة وتحسين الأداء"
    }

    val runTimeContextMenu: String get() = when (lang) {
        AppLanguage.CHINESE -> "右键菜单"
        AppLanguage.ENGLISH -> "Context Menu"
        AppLanguage.ARABIC -> "قائمة السياق"
    }

    val runTimeContextMenuDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "右键菜单打开时执行"
        AppLanguage.ENGLISH -> "Execute when context menu opens"
        AppLanguage.ARABIC -> "التنفيذ عند فتح قائمة السياق"
    }

    val runTimeBeforeUnload: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面关闭前"
        AppLanguage.ENGLISH -> "Before Unload"
        AppLanguage.ARABIC -> "قبل إغلاق الصفحة"
    }

    val runTimeBeforeUnloadDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面即将关闭时执行，适合保存数据"
        AppLanguage.ENGLISH -> "Execute before page closes, suitable for saving data"
        AppLanguage.ARABIC -> "التنفيذ قبل إغلاق الصفحة، مناسب لحفظ البيانات"
    }
    // ==================== Extension Module ====================
    val selectedCount2: String get() = when (lang) {
        AppLanguage.CHINESE -> "已选 %d 个"
        AppLanguage.ENGLISH -> "%d selected"
        AppLanguage.ARABIC -> "تم اختيار %d"
    }

    val addCustomFeatures: String get() = when (lang) {
        AppLanguage.CHINESE -> "为应用添加自定义功能，如元素屏蔽、深色模式、自动滚动等"
        AppLanguage.ENGLISH -> "Add custom features like element blocking, dark mode, auto scroll, etc."
        AppLanguage.ARABIC -> "إضافة ميزات مخصصة مثل حظر العناصر، الوضع الداكن، التمرير التلقائي، إلخ."
    }

    val quickSelect: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速选择"
        AppLanguage.ENGLISH -> "Quick Select"
        AppLanguage.ARABIC -> "اختيار سريع"
    }

    val enableModulesFirst: String get() = when (lang) {
        AppLanguage.CHINESE -> "请先在「扩展模块」中启用需要使用的模块"
        AppLanguage.ENGLISH -> "Please enable modules in 'Extension Modules' first"
        AppLanguage.ARABIC -> "يرجى تمكين الوحدات في 'وحدات الامتداد' أولاً"
    }

    val selectedModulesCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "已选择 %d 个模块"
        AppLanguage.ENGLISH -> "%d modules selected"
        AppLanguage.ARABIC -> "تم اختيار %d وحدات"
    }

    val removeModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "Remove"
        AppLanguage.ENGLISH -> "Remove"
        AppLanguage.ARABIC -> "إزالة"
    }

    val noMatchingModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "没有找到匹配的模块"
        AppLanguage.ENGLISH -> "No matching modules found"
        AppLanguage.ARABIC -> "لم يتم العثور على وحدات مطابقة"
    }

    val willTestModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "将测试 %d 个模块"
        AppLanguage.ENGLISH -> "Will test %d modules"
        AppLanguage.ARABIC -> "سيتم اختبار %d وحدات"
    }

    val selectTestPage: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择测试页面"
        AppLanguage.ENGLISH -> "Select Test Page"
        AppLanguage.ARABIC -> "اختيار صفحة الاختبار"
    }

    val testPageHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "💡 测试页面会加载选中的模块，你可以观察模块的实际效果"
        AppLanguage.ENGLISH -> "💡 Test page will load selected modules, you can observe actual effects"
        AppLanguage.ARABIC -> "💡 ستقوم صفحة الاختبار بتحميل الوحدات المحددة، يمكنك ملاحظة التأثيرات الفعلية"
    }

    val builtInModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "内置模块"
        AppLanguage.ENGLISH -> "Built-in Module"
        AppLanguage.ARABIC -> "وحدة مدمجة"
    }

    val configurableItems: String get() = when (lang) {
        AppLanguage.CHINESE -> "可配置项 (%d)"
        AppLanguage.ENGLISH -> "Configurable Items (%d)"
        AppLanguage.ARABIC -> "عناصر قابلة للتكوين (%d)"
    }
    // ==================== Template Categories ====================
    val templateModern: String get() = when (lang) {
        AppLanguage.CHINESE -> "现代简约"
        AppLanguage.ENGLISH -> "Modern Minimal"
        AppLanguage.ARABIC -> "حديث بسيط"
    }

    val templateGlassmorphism: String get() = when (lang) {
        AppLanguage.CHINESE -> "玻璃拟态"
        AppLanguage.ENGLISH -> "Glassmorphism"
        AppLanguage.ARABIC -> "تأثير الزجاج"
    }

    val templateNeumorphism: String get() = when (lang) {
        AppLanguage.CHINESE -> "新拟物"
        AppLanguage.ENGLISH -> "Neumorphism"
        AppLanguage.ARABIC -> "نيومورفيزم"
    }

    val templateGradient: String get() = when (lang) {
        AppLanguage.CHINESE -> "渐变炫彩"
        AppLanguage.ENGLISH -> "Gradient Colors"
        AppLanguage.ARABIC -> "ألوان متدرجة"
    }

    val templateDark: String get() = when (lang) {
        AppLanguage.CHINESE -> "暗黑主题"
        AppLanguage.ENGLISH -> "Dark Theme"
        AppLanguage.ARABIC -> "السمة الداكنة"
    }

    val templateMinimal: String get() = when (lang) {
        AppLanguage.CHINESE -> "极简风格"
        AppLanguage.ENGLISH -> "Minimal Style"
        AppLanguage.ARABIC -> "أسلوب بسيط"
    }

    val templateRetro: String get() = when (lang) {
        AppLanguage.CHINESE -> "复古风格"
        AppLanguage.ENGLISH -> "Retro Style"
        AppLanguage.ARABIC -> "أسلوب كلاسيكي"
    }

    val templateCyberpunk: String get() = when (lang) {
        AppLanguage.CHINESE -> "赛博朋克"
        AppLanguage.ENGLISH -> "Cyberpunk"
        AppLanguage.ARABIC -> "سايبربانك"
    }

    val templateNature: String get() = when (lang) {
        AppLanguage.CHINESE -> "自然清新"
        AppLanguage.ENGLISH -> "Nature Fresh"
        AppLanguage.ARABIC -> "طبيعة منعشة"
    }

    val templateBusiness: String get() = when (lang) {
        AppLanguage.CHINESE -> "商务专业"
        AppLanguage.ENGLISH -> "Business Professional"
        AppLanguage.ARABIC -> "أعمال احترافية"
    }

    val templateCreative: String get() = when (lang) {
        AppLanguage.CHINESE -> "创意艺术"
        AppLanguage.ENGLISH -> "Creative Art"
        AppLanguage.ARABIC -> "فن إبداعي"
    }

    val templateGame: String get() = when (lang) {
        AppLanguage.CHINESE -> "游戏风格"
        AppLanguage.ENGLISH -> "Game Style"
        AppLanguage.ARABIC -> "أسلوب الألعاب"
    }
    // ==================== Module Preset Factory ====================
    val presetBlockElements: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏蔽指定元素"
        AppLanguage.ENGLISH -> "Block specified elements"
        AppLanguage.ARABIC -> "حظر العناصر المحددة"
    }

    val presetInjectStyle: String get() = when (lang) {
        AppLanguage.CHINESE -> "注入自定义样式"
        AppLanguage.ENGLISH -> "Inject custom styles"
        AppLanguage.ARABIC -> "حقن أنماط مخصصة"
    }

    val presetAutoClick: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动点击指定元素"
        AppLanguage.ENGLISH -> "Auto-click specified element"
        AppLanguage.ARABIC -> "النقر التلقائي على العنصر المحدد"
    }

    val presetFloatingButton: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加悬浮按钮"
        AppLanguage.ENGLISH -> "Add floating button"
        AppLanguage.ARABIC -> "إضافة زر عائم"
    }

    val tagBlock: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏蔽"
        AppLanguage.ENGLISH -> "Block"
        AppLanguage.ARABIC -> "حظر"
    }

    val tagHideElement: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏"
        AppLanguage.ENGLISH -> "Hide"
        AppLanguage.ARABIC -> "إخفاء"
    }

    val tagStyleCss: String get() = when (lang) {
        AppLanguage.CHINESE -> "样式"
        AppLanguage.ENGLISH -> "Style"
        AppLanguage.ARABIC -> "نمط"
    }

    val tagAuto: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动"
        AppLanguage.ENGLISH -> "Auto"
        AppLanguage.ARABIC -> "تلقائي"
    }

    val tagClickAction: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击"
        AppLanguage.ENGLISH -> "Click"
        AppLanguage.ARABIC -> "نقر"
    }

    val tagButton: String get() = when (lang) {
        AppLanguage.CHINESE -> "按钮"
        AppLanguage.ENGLISH -> "Button"
        AppLanguage.ARABIC -> "زر"
    }

    val tagFloatingWidget: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮"
        AppLanguage.ENGLISH -> "Floating"
        AppLanguage.ARABIC -> "عائم"
    }

    val builtInVersion: String get() = when (lang) {
        AppLanguage.CHINESE -> "内置版本"
        AppLanguage.ENGLISH -> "Built-in version"
        AppLanguage.ARABIC -> "الإصدار المدمج"
    }
    // ==================== Module Scheme Presets ====================
    val presetReading: String get() = when (lang) {
        AppLanguage.CHINESE -> "阅读增强"
        AppLanguage.ENGLISH -> "Reading Enhance"
        AppLanguage.ARABIC -> "تحسين القراءة"
    }

    val presetReadingDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "适合阅读文章、小说的模块组合"
        AppLanguage.ENGLISH -> "Module combination for reading articles and novels"
        AppLanguage.ARABIC -> "مجموعة وحدات لقراءة المقالات والروايات"
    }

    val presetAdblock: String get() = when (lang) {
        AppLanguage.CHINESE -> "广告净化"
        AppLanguage.ENGLISH -> "Ad Blocking"
        AppLanguage.ARABIC -> "حظر الإعلانات"
    }

    val presetAdblockDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏蔽广告和干扰元素"
        AppLanguage.ENGLISH -> "Block ads and distracting elements"
        AppLanguage.ARABIC -> "حظر الإعلانات والعناصر المشتتة"
    }

    val presetMedia: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体增强"
        AppLanguage.ENGLISH -> "Media Enhance"
        AppLanguage.ARABIC -> "تحسين الوسائط"
    }

    val presetMediaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频倍速、图片下载等媒体功能"
        AppLanguage.ENGLISH -> "Video speed control, image download, etc."
        AppLanguage.ARABIC -> "التحكم في سرعة الفيديو، تحميل الصور، إلخ"
    }

    val presetUtility: String get() = when (lang) {
        AppLanguage.CHINESE -> "实用工具"
        AppLanguage.ENGLISH -> "Utility Tools"
        AppLanguage.ARABIC -> "أدوات مساعدة"
    }

    val presetUtilityDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制解锁、翻译助手等实用功能"
        AppLanguage.ENGLISH -> "Copy unlock, translation assistant, etc."
        AppLanguage.ARABIC -> "فتح النسخ، مساعد الترجمة، إلخ"
    }

    val presetNight: String get() = when (lang) {
        AppLanguage.CHINESE -> "夜间模式"
        AppLanguage.ENGLISH -> "Night Mode"
        AppLanguage.ARABIC -> "الوضع الليلي"
    }

    val presetNightDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "深色主题 + 护眼功能"
        AppLanguage.ENGLISH -> "Dark theme + eye protection"
        AppLanguage.ARABIC -> "سمة داكنة + حماية العين"
    }
    // ==================== Module Config Items ====================
    val configCssSelector: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS 选择器"
        AppLanguage.ENGLISH -> "CSS Selector"
        AppLanguage.ARABIC -> "محدد CSS"
    }

    val configCssSelectorDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "要隐藏的元素选择器，每行一个"
        AppLanguage.ENGLISH -> "Element selectors to hide, one per line"
        AppLanguage.ARABIC -> "محددات العناصر للإخفاء، واحد لكل سطر"
    }

    val configCssSelectorPlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入 CSS 选择器，每行一个"
        AppLanguage.ENGLISH -> "Enter CSS selectors, one per line"
        AppLanguage.ARABIC -> "أدخل محددات CSS، واحد لكل سطر"
    }

    val configHideMethod: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏方式"
        AppLanguage.ENGLISH -> "Hide Method"
        AppLanguage.ARABIC -> "طريقة الإخفاء"
    }

    val configBlockPopups: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截弹窗"
        AppLanguage.ENGLISH -> "Block Popups"
        AppLanguage.ARABIC -> "حظر النوافذ المنبثقة"
    }

    val configBlockOverlays: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截遮罩层"
        AppLanguage.ENGLISH -> "Block Overlays"
        AppLanguage.ARABIC -> "حظر الطبقات المتراكبة"
    }

    val configAutoCloseDelay: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动关闭延迟(ms)"
        AppLanguage.ENGLISH -> "Auto Close Delay (ms)"
        AppLanguage.ARABIC -> "تأخير الإغلاق التلقائي (مللي ثانية)"
    }

    val configCssCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS代码"
        AppLanguage.ENGLISH -> "CSS Code"
        AppLanguage.ARABIC -> "كود CSS"
    }

    val configBrightness: String get() = when (lang) {
        AppLanguage.CHINESE -> "亮度(%)"
        AppLanguage.ENGLISH -> "Brightness (%)"
        AppLanguage.ARABIC -> "السطوع (%)"
    }

    val configContrast: String get() = when (lang) {
        AppLanguage.CHINESE -> "对比度(%)"
        AppLanguage.ENGLISH -> "Contrast (%)"
        AppLanguage.ARABIC -> "التباين (%)"
    }

    val configFont: String get() = when (lang) {
        AppLanguage.CHINESE -> "字体"
        AppLanguage.ENGLISH -> "Font"
        AppLanguage.ARABIC -> "الخط"
    }

    val configFontSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "字号(px)"
        AppLanguage.ENGLISH -> "Font Size (px)"
        AppLanguage.ARABIC -> "حجم الخط (بكسل)"
    }
    // ==================== Extension Tab Management ====================
    val userScriptsTab: String get() = when (lang) {
        AppLanguage.CHINESE -> "浏览器扩展"
        AppLanguage.ENGLISH -> "Browser Extensions"
        AppLanguage.ARABIC -> "إضافات المتصفح"
    }

    val noUserScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无浏览器扩展"
        AppLanguage.ENGLISH -> "No browser extensions"
        AppLanguage.ARABIC -> "لا توجد إضافات متصفح"
    }

    val noUserScriptsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入油猴脚本 (.user.js) 或 Chrome 扩展 (.crx/.zip)"
        AppLanguage.ENGLISH -> "Import userscripts (.user.js) or Chrome extensions (.crx/.zip)"
        AppLanguage.ARABIC -> "استيراد سكريبتات (.user.js) أو إضافات كروم (.crx/.zip)"
    }

    val viewSourceCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "查看源码"
        AppLanguage.ENGLISH -> "View Source"
        AppLanguage.ARABIC -> "عرض المصدر"
    }

    val cannotReadFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "无法读取文件内容"
        AppLanguage.ENGLISH -> "Cannot read file content"
        AppLanguage.ARABIC -> "لا يمكن قراءة محتوى الملف"
    }

    val imageFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片文件（不支持预览）"
        AppLanguage.ENGLISH -> "Image file (preview not supported)"
        AppLanguage.ARABIC -> "ملف صورة (المعاينة غير مدعومة)"
    }

    val binaryFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "二进制文件（不支持预览）"
        AppLanguage.ENGLISH -> "Binary file (preview not supported)"
        AppLanguage.ARABIC -> "ملف ثنائي (المعاينة غير مدعومة)"
    }

    val scriptEnabledStatus: String get() = when (lang) {
        AppLanguage.CHINESE -> "已启用"
        AppLanguage.ENGLISH -> "Enabled"
        AppLanguage.ARABIC -> "مفعل"
    }

    val grantedApis: String get() = when (lang) {
        AppLanguage.CHINESE -> "授权 API"
        AppLanguage.ENGLISH -> "Granted APIs"
        AppLanguage.ARABIC -> "واجهات API الممنوحة"
    }

    val userscriptsCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个脚本"
        AppLanguage.ENGLISH -> "%d scripts"
        AppLanguage.ARABIC -> "%d سكريبت"
    }

    val extensionsCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个模块"
        AppLanguage.ENGLISH -> "%d modules"
        AppLanguage.ARABIC -> "%d وحدة"
    }
    // ==================== Userscript / Chrome Extension Import ====================
    val importUserScript: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入油猴脚本"
        AppLanguage.ENGLISH -> "Import Userscript"
        AppLanguage.ARABIC -> "استيراد سكريبت المستخدم"
    }

    val importUserScriptHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持 .user.js 及普通 .js 文件"
        AppLanguage.ENGLISH -> "Supports .user.js and plain .js files"
        AppLanguage.ARABIC -> "يدعم ملفات .user.js و .js العادية"
    }

    val importChromeExtension: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入 Chrome 扩展"
        AppLanguage.ENGLISH -> "Import Chrome Extension"
        AppLanguage.ARABIC -> "استيراد إضافة Chrome"
    }

    val importChromeExtensionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持 .crx 和 .zip 扩展包"
        AppLanguage.ENGLISH -> "Supports .crx and .zip extension packages"
        AppLanguage.ARABIC -> "يدعم حزم الإضافات .crx و .zip"
    }

    val installUserScript: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装油猴脚本"
        AppLanguage.ENGLISH -> "Install Userscript"
        AppLanguage.ARABIC -> "تثبيت سكريبت المستخدم"
    }

    val installChromeExtension: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装 Chrome 扩展"
        AppLanguage.ENGLISH -> "Install Chrome Extension"
        AppLanguage.ARABIC -> "تثبيت إضافة Chrome"
    }

    val install: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装"
        AppLanguage.ENGLISH -> "Install"
        AppLanguage.ARABIC -> "تثبيت"
    }

    val matchingSites: String get() = when (lang) {
        AppLanguage.CHINESE -> "匹配网站"
        AppLanguage.ENGLISH -> "Matching sites"
        AppLanguage.ARABIC -> "المواقع المطابقة"
    }

    val requiredApis: String get() = when (lang) {
        AppLanguage.CHINESE -> "所需 API"
        AppLanguage.ENGLISH -> "Required APIs"
        AppLanguage.ARABIC -> "واجهات برمجة مطلوبة"
    }

    val contentScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "内容脚本"
        AppLanguage.ENGLISH -> "content scripts"
        AppLanguage.ARABIC -> "سكريبتات المحتوى"
    }

    val unsupportedApis: String get() = when (lang) {
        AppLanguage.CHINESE -> "不支持的 API"
        AppLanguage.ENGLISH -> "Unsupported APIs"
        AppLanguage.ARABIC -> "واجهات غير مدعومة"
    }
}
