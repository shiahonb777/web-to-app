package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object WebViewStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage
    // ==================== Download Bridge ====================
    val preparingDownload: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在准备下载: "
        AppLanguage.ENGLISH -> "Preparing download: "
        AppLanguage.ARABIC -> "جاري تحضير التحميل: "
    }

    val cannotGetFileData: String get() = when (lang) {
        AppLanguage.CHINESE -> "无法获取文件数据，请重试"
        AppLanguage.ENGLISH -> "Cannot get file data, please try again"
        AppLanguage.ARABIC -> "لا يمكن الحصول على بيانات الملف، حاول مرة أخرى"
    }

    val downloadUnavailable: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载功能不可用，请确保应用已正确配置"
        AppLanguage.ENGLISH -> "Download unavailable, please ensure the app is configured correctly"
        AppLanguage.ARABIC -> "التحميل غير متاح، يرجى التأكد من تكوين التطبيق بشكل صحيح"
    }

    val processFileFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "处理文件失败: "
        AppLanguage.ENGLISH -> "Failed to process file: "
        AppLanguage.ARABIC -> "فشل في معالجة الملف: "
    }

    val readFileFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Failed to read file"
        AppLanguage.ENGLISH -> "Failed to read file"
        AppLanguage.ARABIC -> "فشل في قراءة الملف"
    }

    val downloadFailedPrefix: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载失败: "
        AppLanguage.ENGLISH -> "Download failed: "
        AppLanguage.ARABIC -> "فشل التحميل: "
    }

    val copiedFullLog: String get() = when (lang) {
        AppLanguage.CHINESE -> "已复制完整日志"
        AppLanguage.ENGLISH -> "Full log copied"
        AppLanguage.ARABIC -> "تم نسخ السجل الكامل"
    }

    val copiedSourceCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "已复制源代码"
        AppLanguage.ENGLISH -> "Source code copied"
        AppLanguage.ARABIC -> "تم نسخ الكود المصدري"
    }

    val copyAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制全部"
        AppLanguage.ENGLISH -> "Copy all"
        AppLanguage.ARABIC -> "نسخ الكل"
    }

    val logDetails: String get() = when (lang) {
        AppLanguage.CHINESE -> "日志详情"
        AppLanguage.ENGLISH -> "Log Details"
        AppLanguage.ARABIC -> "تفاصيل السجل"
    }

    val level: String get() = when (lang) {
        AppLanguage.CHINESE -> "级别"
        AppLanguage.ENGLISH -> "Level"
        AppLanguage.ARABIC -> "المستوى"
    }

    val source: String get() = when (lang) {
        AppLanguage.CHINESE -> "来源"
        AppLanguage.ENGLISH -> "Source"
        AppLanguage.ARABIC -> "المصدر"
    }

    val messageContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "消息内容"
        AppLanguage.ENGLISH -> "Message Content"
        AppLanguage.ARABIC -> "محتوى الرسالة"
    }

    val sourceCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "源代码"
        AppLanguage.ENGLISH -> "Source Code"
        AppLanguage.ARABIC -> "الكود المصدري"
    }

    val inputJavaScriptExpression: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入 JavaScript 表达式..."
        AppLanguage.ENGLISH -> "Enter JavaScript expression..."
        AppLanguage.ARABIC -> "أدخل تعبير JavaScript..."
    }

    val pleaseSelectTextModel: String get() = when (lang) {
        AppLanguage.CHINESE -> "请先选择文本模型"
        AppLanguage.ENGLISH -> "Please select a text model first"
        AppLanguage.ARABIC -> "يرجى اختيار نموذج نصي أولاً"
    }

    val sendFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "发送失败"
        AppLanguage.ENGLISH -> "Send failed"
        AppLanguage.ARABIC -> "فشل الإرسال"
    }

    val previewFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览失败"
        AppLanguage.ENGLISH -> "Preview failed"
        AppLanguage.ARABIC -> "فشلت المعاينة"
    }

    val errorPrefix: String get() = when (lang) {
        AppLanguage.CHINESE -> "Error"
        AppLanguage.ENGLISH -> "Error"
        AppLanguage.ARABIC -> "خطأ"
    }

    val imageGenerationFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "图像生成失败"
        AppLanguage.ENGLISH -> "Image generation failed"
        AppLanguage.ARABIC -> "فشل إنشاء الصورة"
    }
    // ==================== WebView Advanced Settings ====================
    val javaScriptSetting: String get() = when (lang) {
        AppLanguage.CHINESE -> "JavaScript"
        AppLanguage.ENGLISH -> "JavaScript"
        AppLanguage.ARABIC -> "JavaScript"
    }

    val javaScriptSettingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用JavaScript执行"
        AppLanguage.ENGLISH -> "Enable JavaScript execution"
        AppLanguage.ARABIC -> "تفعيل تنفيذ JavaScript"
    }

    val domStorageSetting: String get() = when (lang) {
        AppLanguage.CHINESE -> "DOM存储"
        AppLanguage.ENGLISH -> "DOM Storage"
        AppLanguage.ARABIC -> "تخزين DOM"
    }

    val domStorageSettingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用本地存储功能"
        AppLanguage.ENGLISH -> "Enable local storage"
        AppLanguage.ARABIC -> "تفعيل التخزين المحلي"
    }

    val zoomSetting: String get() = when (lang) {
        AppLanguage.CHINESE -> "缩放功能"
        AppLanguage.ENGLISH -> "Zoom"
        AppLanguage.ARABIC -> "التكبير/التصغير"
    }

    val zoomSettingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "允许用户缩放页面"
        AppLanguage.ENGLISH -> "Allow user to zoom page"
        AppLanguage.ARABIC -> "السماح للمستخدم بتكبير/تصغير الصفحة"
    }

    val swipeRefreshSetting: String get() = when (lang) {
        AppLanguage.CHINESE -> "下拉刷新"
        AppLanguage.ENGLISH -> "Swipe Refresh"
        AppLanguage.ARABIC -> "السحب للتحديث"
    }

    val swipeRefreshSettingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "允许下拉刷新页面"
        AppLanguage.ENGLISH -> "Allow swipe down to refresh"
        AppLanguage.ARABIC -> "السماح بالسحب لأسفل للتحديث"
    }

    val desktopModeSetting: String get() = when (lang) {
        AppLanguage.CHINESE -> "桌面模式"
        AppLanguage.ENGLISH -> "Desktop Mode"
        AppLanguage.ARABIC -> "وضع سطح المكتب"
    }

    val desktopModeSettingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "以桌面版网页模式加载"
        AppLanguage.ENGLISH -> "Load as desktop website"
        AppLanguage.ARABIC -> "التحميل كموقع سطح المكتب"
    }

    val fullscreenVideoSetting: String get() = when (lang) {
        AppLanguage.CHINESE -> "全屏视频"
        AppLanguage.ENGLISH -> "Fullscreen Video"
        AppLanguage.ARABIC -> "فيديو ملء الشاشة"
    }

    val fullscreenVideoSettingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "允许视频全屏播放"
        AppLanguage.ENGLISH -> "Allow video fullscreen playback"
        AppLanguage.ARABIC -> "السماح بتشغيل الفيديو بملء الشاشة"
    }

    val hideBrowserToolbarLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏浏览器工具栏"
        AppLanguage.ENGLISH -> "Hide Browser Toolbar"
        AppLanguage.ARABIC -> "إخفاء شريط أدوات المتصفح"
    }

    val hideBrowserToolbarHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏顶部浏览器导航栏（独立于全屏模式）"
        AppLanguage.ENGLISH -> "Hide the top browser navigation bar (independent of fullscreen mode)"
        AppLanguage.ARABIC -> "إخفاء شريط التنقل العلوي للمتصفح (مستقل عن وضع ملء الشاشة)"
    }

    val externalLinksSetting: String get() = when (lang) {
        AppLanguage.CHINESE -> "外部链接"
        AppLanguage.ENGLISH -> "External Links"
        AppLanguage.ARABIC -> "الروابط الخارجية"
    }

    val externalLinksSettingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "在浏览器中打开外部链接"
        AppLanguage.ENGLISH -> "Open external links in browser"
        AppLanguage.ARABIC -> "فتح الروابط الخارجية في المتصفح"
    }

    val crossOriginIsolationSetting: String get() = when (lang) {
        AppLanguage.CHINESE -> "SharedArrayBuffer / FFmpeg"
        AppLanguage.ENGLISH -> "SharedArrayBuffer / FFmpeg"
        AppLanguage.ARABIC -> "SharedArrayBuffer / FFmpeg"
    }

    val crossOriginIsolationSettingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用跨域隔离 (COOP/COEP)，支持 FFmpeg.wasm 等库"
        AppLanguage.ENGLISH -> "Enable cross-origin isolation (COOP/COEP), supports FFmpeg.wasm and similar libraries"
        AppLanguage.ARABIC -> "تمكين العزل عبر الأصل (COOP/COEP)، يدعم FFmpeg.wasm والمكتبات المماثلة"
    }
    // ==================== CreateAppScreen Translations ====================
    val showStatusBar: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示状态栏"
        AppLanguage.ENGLISH -> "Show Status Bar"
        AppLanguage.ARABIC -> "إظهار شريط الحالة"
    }

    val showStatusBarHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "全屏模式下仍显示状态栏，可解决导航栏问题"
        AppLanguage.ENGLISH -> "Show status bar in fullscreen mode, can fix navigation bar issues"
        AppLanguage.ARABIC -> "إظهار شريط الحالة في وضع ملء الشاشة، يمكن أن يحل مشاكل شريط التنقل"
    }

    val showNavigationBar: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示导航栏"
        AppLanguage.ENGLISH -> "Show Navigation Bar"
        AppLanguage.ARABIC -> "إظهار شريط التنقل"
    }

    val showNavigationBarHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "全屏模式下仍显示底部导航栏（返回、主页、最近任务）"
        AppLanguage.ENGLISH -> "Keep bottom navigation bar visible in fullscreen mode (Back, Home, Recents)"
        AppLanguage.ARABIC -> "إبقاء شريط التنقل السفلي مرئيًا في وضع ملء الشاشة (رجوع، الرئيسية، الأخيرة)"
    }

    val showToolbar: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示顶部导航栏"
        AppLanguage.ENGLISH -> "Show Top Toolbar"
        AppLanguage.ARABIC -> "إظهار شريط الأدوات العلوي"
    }

    val showToolbarHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "全屏模式下仍显示浏览器顶部导航栏（标题、URL、返回/前进/刷新按钮）"
        AppLanguage.ENGLISH -> "Keep browser toolbar visible in fullscreen mode (title, URL, back/forward/refresh)"
        AppLanguage.ARABIC -> "إبقاء شريط أدوات المتصفح العلوي مرئيًا في وضع ملء الشاشة (العنوان، الرابط، أزرار التنقل)"
    }

    val statusBarStyleConfigLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "状态栏样式配置"
        AppLanguage.ENGLISH -> "Status Bar Style Config"
        AppLanguage.ARABIC -> "إعدادات نمط شريط الحالة"
    }

    val statusBarLightModeLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "浅色模式"
        AppLanguage.ENGLISH -> "Light Mode"
        AppLanguage.ARABIC -> "الوضع الفاتح"
    }

    val statusBarDarkModeLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "深色模式"
        AppLanguage.ENGLISH -> "Dark Mode"
        AppLanguage.ARABIC -> "الوضع الداكن"
    }

    val splashHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "设置应用启动时显示的图片或视频"
        AppLanguage.ENGLISH -> "Set image or video to display when app launches"
        AppLanguage.ARABIC -> "تعيين الصورة أو الفيديو لعرضها عند تشغيل التطبيق"
    }

    val clickToSelectImageOrVideo: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击下方按钮选择图片或视频"
        AppLanguage.ENGLISH -> "Click button below to select image or video"
        AppLanguage.ARABIC -> "انقر على الزر أدناه لاختيار صورة أو فيديو"
    }

    val displayDuration: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示时长"
        AppLanguage.ENGLISH -> "Display Duration"
        AppLanguage.ARABIC -> "مدة العرض"
    }

    val displayDurationSeconds: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示时长：%d 秒"
        AppLanguage.ENGLISH -> "Display duration: %d seconds"
        AppLanguage.ARABIC -> "مدة العرض: %d ثانية"
    }

    val exportAppTheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出应用主题"
        AppLanguage.ENGLISH -> "Export App Theme"
        AppLanguage.ARABIC -> "تصدير سمة التطبيق"
    }

    val exportAppThemeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "设置导出 APK 后应用的 UI 主题风格（激活码验证、公告弹窗等界面）"
        AppLanguage.ENGLISH -> "Set UI theme style for exported APK (activation code, announcement dialogs, etc.)"
        AppLanguage.ARABIC -> "تعيين نمط سمة واجهة المستخدم لـ APK المُصدَّر (رمز التفعيل، نوافذ الإعلانات، إلخ)"
    }

    val autoTranslateHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面加载完成后自动翻译为指定语言，支持多引擎自动降级（Google / MyMemory / LibreTranslate / Lingva）"
        AppLanguage.ENGLISH -> "Auto translate to specified language after page loads with multi-engine fallback (Google / MyMemory / LibreTranslate / Lingva)"
        AppLanguage.ARABIC -> "ترجمة تلقائية إلى اللغة المحددة بعد تحميل الصفحة مع دعم محركات متعددة (Google / MyMemory / LibreTranslate / Lingva)"
    }

    val videoCrop: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频裁剪"
        AppLanguage.ENGLISH -> "Video Crop"
        AppLanguage.ARABIC -> "قص الفيديو"
    }

    val splashPreview: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动画面预览"
        AppLanguage.ENGLISH -> "Splash Screen Preview"
        AppLanguage.ARABIC -> "معاينة شاشة البداية"
    }

    val landscapeDisplay: String get() = when (lang) {
        AppLanguage.CHINESE -> "横屏显示"
        AppLanguage.ENGLISH -> "Landscape Display"
        AppLanguage.ARABIC -> "عرض أفقي"
    }

    val landscapeDisplayHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动画面以横屏方式展示"
        AppLanguage.ENGLISH -> "Display splash screen in landscape orientation"
        AppLanguage.ARABIC -> "عرض شاشة البداية بالاتجاه الأفقي"
    }
    // ==================== User-Agent Spoofing Strings ====================
    val userAgentMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "浏览器伪装"
        AppLanguage.ENGLISH -> "Browser Disguise"
        AppLanguage.ARABIC -> "تمويه المتصفح"
    }

    val userAgentModeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "伪装为真实浏览器，绕过网站对 WebView 的检测"
        AppLanguage.ENGLISH -> "Disguise as real browser to bypass WebView detection"
        AppLanguage.ARABIC -> "تمويه كمتصفح حقيقي لتجاوز اكتشاف WebView"
    }

    val userAgentDefault: String get() = when (lang) {
        AppLanguage.CHINESE -> "System Default"
        AppLanguage.ENGLISH -> "System Default"
        AppLanguage.ARABIC -> "افتراضي النظام"
    }

    val userAgentDefaultHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "Use Android WebView default User-Agent"
        AppLanguage.ENGLISH -> "Use Android WebView default User-Agent"
        AppLanguage.ARABIC -> "استخدام User-Agent الافتراضي لـ WebView"
    }

    val userAgentChromeMobile: String get() = when (lang) {
        AppLanguage.CHINESE -> "Chrome Mobile"
        AppLanguage.ENGLISH -> "Chrome Mobile"
        AppLanguage.ARABIC -> "Chrome للجوال"
    }

    val userAgentChromeDesktop: String get() = when (lang) {
        AppLanguage.CHINESE -> "Chrome Desktop"
        AppLanguage.ENGLISH -> "Chrome Desktop"
        AppLanguage.ARABIC -> "Chrome للكمبيوتر"
    }

    val userAgentSafariMobile: String get() = when (lang) {
        AppLanguage.CHINESE -> "Safari Mobile"
        AppLanguage.ENGLISH -> "Safari Mobile"
        AppLanguage.ARABIC -> "Safari للجوال"
    }

    val userAgentSafariDesktop: String get() = when (lang) {
        AppLanguage.CHINESE -> "Safari Desktop"
        AppLanguage.ENGLISH -> "Safari Desktop"
        AppLanguage.ARABIC -> "Safari للكمبيوتر"
    }

    val userAgentFirefoxMobile: String get() = when (lang) {
        AppLanguage.CHINESE -> "Firefox Mobile"
        AppLanguage.ENGLISH -> "Firefox Mobile"
        AppLanguage.ARABIC -> "Firefox للجوال"
    }

    val userAgentFirefoxDesktop: String get() = when (lang) {
        AppLanguage.CHINESE -> "Firefox Desktop"
        AppLanguage.ENGLISH -> "Firefox Desktop"
        AppLanguage.ARABIC -> "Firefox للكمبيوتر"
    }

    val userAgentEdgeMobile: String get() = when (lang) {
        AppLanguage.CHINESE -> "Edge Mobile"
        AppLanguage.ENGLISH -> "Edge Mobile"
        AppLanguage.ARABIC -> "Edge للجوال"
    }

    val userAgentEdgeDesktop: String get() = when (lang) {
        AppLanguage.CHINESE -> "Edge Desktop"
        AppLanguage.ENGLISH -> "Edge Desktop"
        AppLanguage.ARABIC -> "Edge للكمبيوتر"
    }

    val userAgentCustom: String get() = when (lang) {
        AppLanguage.CHINESE -> "Custom"
        AppLanguage.ENGLISH -> "Custom"
        AppLanguage.ARABIC -> "مخصص"
    }

    val userAgentCustomHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入自定义 User-Agent 字符串"
        AppLanguage.ENGLISH -> "Enter custom User-Agent string"
        AppLanguage.ARABIC -> "أدخل سلسلة User-Agent مخصصة"
    }

    val mobileVersion: String get() = when (lang) {
        AppLanguage.CHINESE -> "移动版"
        AppLanguage.ENGLISH -> "Mobile"
        AppLanguage.ARABIC -> "للجوال"
    }

    val desktopVersion: String get() = when (lang) {
        AppLanguage.CHINESE -> "桌面版"
        AppLanguage.ENGLISH -> "Desktop"
        AppLanguage.ARABIC -> "للكمبيوتر"
    }

    val currentUserAgent: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前 User-Agent"
        AppLanguage.ENGLISH -> "Current User-Agent"
        AppLanguage.ARABIC -> "User-Agent الحالي"
    }

    val bypassWebViewDetection: String get() = when (lang) {
        AppLanguage.CHINESE -> "部分网站会检测 WebView 并阻止访问，选择浏览器伪装可绕过检测"
        AppLanguage.ENGLISH -> "Some sites detect WebView and block access. Browser disguise can bypass detection"
        AppLanguage.ARABIC -> "تكتشف بعض المواقع WebView وتحظر الوصول. يمكن لتمويه المتصفح تجاوز الاكتشاف"
    }
    // ==================== Floating Window ====================
    val floatingWindowTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮小窗"
        AppLanguage.ENGLISH -> "Floating Window"
        AppLanguage.ARABIC -> "النافذة العائمة"
    }

    val floatingWindowDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "以悬浮窗模式运行，可自由调整大小和透明度"
        AppLanguage.ENGLISH -> "Run in floating window mode with adjustable size and opacity"
        AppLanguage.ARABIC -> "التشغيل في وضع النافذة العائمة مع حجم وشفافية قابلين للتعديل"
    }

    val floatingWindowSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "窗口大小"
        AppLanguage.ENGLISH -> "Window Size"
        AppLanguage.ARABIC -> "حجم النافذة"
    }

    val floatingWindowSizeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "设置悬浮窗占屏幕的百分比（50%-100%）"
        AppLanguage.ENGLISH -> "Set the floating window size as percentage of screen (50%-100%)"
        AppLanguage.ARABIC -> "تعيين حجم النافذة العائمة كنسبة مئوية من الشاشة (50%-100%)"
    }

    val floatingWindowOpacity: String get() = when (lang) {
        AppLanguage.CHINESE -> "透明度"
        AppLanguage.ENGLISH -> "Opacity"
        AppLanguage.ARABIC -> "الشفافية"
    }

    val floatingWindowOpacityDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "设置悬浮窗的透明度（30%-100%）"
        AppLanguage.ENGLISH -> "Set the floating window opacity (30%-100%)"
        AppLanguage.ARABIC -> "تعيين شفافية النافذة العائمة (30%-100%)"
    }

    val floatingWindowShowTitleBar: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示标题栏"
        AppLanguage.ENGLISH -> "Show Title Bar"
        AppLanguage.ARABIC -> "عرض شريط العنوان"
    }

    val floatingWindowShowTitleBarDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示标题栏以便拖拽移动窗口位置"
        AppLanguage.ENGLISH -> "Show title bar to drag and move window position"
        AppLanguage.ARABIC -> "عرض شريط العنوان لسحب وتحريك موضع النافذة"
    }

    val floatingWindowStartMinimized: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动时最小化"
        AppLanguage.ENGLISH -> "Start Minimized"
        AppLanguage.ARABIC -> "البدء مصغرًا"
    }

    val floatingWindowStartMinimizedDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动时自动最小化为悬浮按钮，点击展开"
        AppLanguage.ENGLISH -> "Auto minimize to floating button on start, tap to expand"
        AppLanguage.ARABIC -> "تصغير تلقائي إلى زر عائم عند البدء، انقر للتوسيع"
    }

    val floatingWindowRememberPosition: String get() = when (lang) {
        AppLanguage.CHINESE -> "记住位置"
        AppLanguage.ENGLISH -> "Remember Position"
        AppLanguage.ARABIC -> "تذكر الموضع"
    }

    val floatingWindowRememberPositionDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "下次打开时恢复到上次窗口位置"
        AppLanguage.ENGLISH -> "Restore to last window position on next open"
        AppLanguage.ARABIC -> "الاستعادة إلى آخر موضع للنافذة عند الفتح التالي"
    }

    val floatingWindowNotificationChannel: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮窗服务"
        AppLanguage.ENGLISH -> "Floating Window Service"
        AppLanguage.ARABIC -> "خدمة النافذة العائمة"
    }

    val floatingWindowNotificationChannelDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮小窗运行时的通知"
        AppLanguage.ENGLISH -> "Notifications for floating window service"
        AppLanguage.ARABIC -> "إشعارات خدمة النافذة العائمة"
    }

    val floatingWindowNotificationTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮窗运行中"
        AppLanguage.ENGLISH -> "Floating Window Active"
        AppLanguage.ARABIC -> "النافذة العائمة نشطة"
    }

    val floatingWindowNotificationContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "%s 正在悬浮窗中运行"
        AppLanguage.ENGLISH -> "%s is running in floating window"
        AppLanguage.ARABIC -> "%s يعمل في النافذة العائمة"
    }

    val floatingWindowNotificationContentDefault: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用正在悬浮窗中运行"
        AppLanguage.ENGLISH -> "App is running in floating window"
        AppLanguage.ARABIC -> "التطبيق يعمل في النافذة العائمة"
    }

    val floatingWindowClose: String get() = when (lang) {
        AppLanguage.CHINESE -> "关闭"
        AppLanguage.ENGLISH -> "Close"
        AppLanguage.ARABIC -> "إغلاق"
    }

    val floatingWindowPermissionRequired: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮窗需要\"显示在其他应用上层\"权限"
        AppLanguage.ENGLISH -> "Floating window requires \"Display over other apps\" permission"
        AppLanguage.ARABIC -> "تتطلب النافذة العائمة إذن \"العرض فوق التطبيقات الأخرى\""
    }

    val floatingWindowGoToSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "前往设置授权"
        AppLanguage.ENGLISH -> "Go to Settings"
        AppLanguage.ARABIC -> "الذهاب إلى الإعدادات"
    }
    // ==================== Floating Window V2 ====================
    val fwSectionSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "窗口尺寸"
        AppLanguage.ENGLISH -> "Window Size"
        AppLanguage.ARABIC -> "حجم النافذة"
    }

    val fwWidthLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "宽度"
        AppLanguage.ENGLISH -> "Width"
        AppLanguage.ARABIC -> "العرض"
    }

    val fwHeightLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "高度"
        AppLanguage.ENGLISH -> "Height"
        AppLanguage.ARABIC -> "الارتفاع"
    }

    val fwLockAspectRatio: String get() = when (lang) {
        AppLanguage.CHINESE -> "锁定宽高比"
        AppLanguage.ENGLISH -> "Lock Aspect Ratio"
        AppLanguage.ARABIC -> "قفل نسبة العرض إلى الارتفاع"
    }

    val fwSectionAppearance: String get() = when (lang) {
        AppLanguage.CHINESE -> "外观样式"
        AppLanguage.ENGLISH -> "Appearance"
        AppLanguage.ARABIC -> "المظهر"
    }

    val fwCornerRadius: String get() = when (lang) {
        AppLanguage.CHINESE -> "圆角半径"
        AppLanguage.ENGLISH -> "Corner Radius"
        AppLanguage.ARABIC -> "نصف قطر الزاوية"
    }

    val fwBorderStyle: String get() = when (lang) {
        AppLanguage.CHINESE -> "边框样式"
        AppLanguage.ENGLISH -> "Border Style"
        AppLanguage.ARABIC -> "نمط الحدود"
    }

    val fwBorderNone: String get() = when (lang) {
        AppLanguage.CHINESE -> "无"
        AppLanguage.ENGLISH -> "None"
        AppLanguage.ARABIC -> "بدون"
    }

    val fwBorderSubtle: String get() = when (lang) {
        AppLanguage.CHINESE -> "细微"
        AppLanguage.ENGLISH -> "Subtle"
        AppLanguage.ARABIC -> "خفيف"
    }

    val fwBorderGlow: String get() = when (lang) {
        AppLanguage.CHINESE -> "发光"
        AppLanguage.ENGLISH -> "Glow"
        AppLanguage.ARABIC -> "توهج"
    }

    val fwBorderAccent: String get() = when (lang) {
        AppLanguage.CHINESE -> "主题色"
        AppLanguage.ENGLISH -> "Accent"
        AppLanguage.ARABIC -> "اللون المميز"
    }

    val fwSectionBehavior: String get() = when (lang) {
        AppLanguage.CHINESE -> "行为控制"
        AppLanguage.ENGLISH -> "Behavior"
        AppLanguage.ARABIC -> "السلوك"
    }

    val fwAutoHideTitleBar: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动隐藏标题栏"
        AppLanguage.ENGLISH -> "Auto-hide Title Bar"
        AppLanguage.ARABIC -> "إخفاء شريط العنوان تلقائياً"
    }

    val fwAutoHideTitleBarDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "3秒无操作后自动隐藏，触摸时恢复"
        AppLanguage.ENGLISH -> "Auto-hides after 3s of inactivity, restores on touch"
        AppLanguage.ARABIC -> "يختفي بعد 3 ثوانٍ من عدم النشاط، ويعود عند اللمس"
    }

    val fwEdgeSnapping: String get() = when (lang) {
        AppLanguage.CHINESE -> "边缘吸附"
        AppLanguage.ENGLISH -> "Edge Snapping"
        AppLanguage.ARABIC -> "الالتصاق بالحافة"
    }

    val fwEdgeSnappingDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "拖拽到屏幕边缘时自动贴合"
        AppLanguage.ENGLISH -> "Snaps to screen edges when dragged nearby"
        AppLanguage.ARABIC -> "ينجذب تلقائياً إلى حواف الشاشة عند السحب بالقرب منها"
    }

    val fwResizeHandle: String get() = when (lang) {
        AppLanguage.CHINESE -> "缩放手柄"
        AppLanguage.ENGLISH -> "Resize Handle"
        AppLanguage.ARABIC -> "مقبض تغيير الحجم"
    }

    val fwResizeHandleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "在窗口右下角显示拖拽缩放手柄"
        AppLanguage.ENGLISH -> "Shows a drag handle at the bottom-right corner"
        AppLanguage.ARABIC -> "يعرض مقبض سحب في الزاوية السفلية اليمنى"
    }

    val fwLockPosition: String get() = when (lang) {
        AppLanguage.CHINESE -> "锁定位置"
        AppLanguage.ENGLISH -> "Lock Position"
        AppLanguage.ARABIC -> "قفل الموضع"
    }

    val fwLockPositionDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "禁止拖拽移动窗口，防止误触"
        AppLanguage.ENGLISH -> "Prevents window from being dragged"
        AppLanguage.ARABIC -> "يمنع سحب النافذة لتجنب اللمس العرضي"
    }
    // ==================== Long-Press Menu ====================
    val longPressMenuImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片"
        AppLanguage.ENGLISH -> "Image"
        AppLanguage.ARABIC -> "صورة"
    }

    val longPressMenuVideo: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频"
        AppLanguage.ENGLISH -> "Video"
        AppLanguage.ARABIC -> "فيديو"
    }

    val longPressMenuLink: String get() = when (lang) {
        AppLanguage.CHINESE -> "链接"
        AppLanguage.ENGLISH -> "Link"
        AppLanguage.ARABIC -> "رابط"
    }

    val longPressMenuSaveImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存图片"
        AppLanguage.ENGLISH -> "Save Image"
        AppLanguage.ARABIC -> "حفظ الصورة"
    }

    val longPressMenuCopyImageLink: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制图片链接"
        AppLanguage.ENGLISH -> "Copy Image Link"
        AppLanguage.ARABIC -> "نسخ رابط الصورة"
    }

    val longPressMenuDownloadVideo: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载视频"
        AppLanguage.ENGLISH -> "Download Video"
        AppLanguage.ARABIC -> "تنزيل الفيديو"
    }

    val longPressMenuCopyVideoLink: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制视频链接"
        AppLanguage.ENGLISH -> "Copy Video Link"
        AppLanguage.ARABIC -> "نسخ رابط الفيديو"
    }

    val longPressMenuCopyLink: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制链接"
        AppLanguage.ENGLISH -> "Copy Link"
        AppLanguage.ARABIC -> "نسخ الرابط"
    }

    val longPressMenuCopyLinkAddress: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制链接地址"
        AppLanguage.ENGLISH -> "Copy Link Address"
        AppLanguage.ARABIC -> "نسخ عنوان الرابط"
    }

    val longPressMenuOpenInBrowser: String get() = when (lang) {
        AppLanguage.CHINESE -> "在浏览器中打开"
        AppLanguage.ENGLISH -> "Open in Browser"
        AppLanguage.ARABIC -> "فتح في المتصفح"
    }

    val longPressMenuImagePreview: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片预览"
        AppLanguage.ENGLISH -> "Image Preview"
        AppLanguage.ARABIC -> "معاينة الصورة"
    }
    // ==================== Toolbar Items ====================
    val toolbarItems: String get() = when (lang) {
        AppLanguage.CHINESE -> "工具栏按钮"
        AppLanguage.ENGLISH -> "Toolbar Items"
        AppLanguage.ARABIC -> "عناصر شريط الأدوات"
    }

    val addToolbarItem: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加按钮"
        AppLanguage.ENGLISH -> "Add Button"
        AppLanguage.ARABIC -> "إضافة زر"
    }

    val editToolbarItem: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑按钮"
        AppLanguage.ENGLISH -> "Edit Button"
        AppLanguage.ARABIC -> "تحرير الزر"
    }

    val toolbarItemIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "图标"
        AppLanguage.ENGLISH -> "Icon"
        AppLanguage.ARABIC -> "الرمز"
    }

    val toolbarItemLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "标签"
        AppLanguage.ENGLISH -> "Label"
        AppLanguage.ARABIC -> "التسمية"
    }

    val toolbarItemTooltip: String get() = when (lang) {
        AppLanguage.CHINESE -> "提示文字"
        AppLanguage.ENGLISH -> "Tooltip"
        AppLanguage.ARABIC -> "تلميح"
    }

    val toolbarItemAction: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击动作 (JS)"
        AppLanguage.ENGLISH -> "Click Action (JS)"
        AppLanguage.ARABIC -> "إجراء النقر (JS)"
    }

    val toolbarItemShowLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示标签"
        AppLanguage.ENGLISH -> "Show Label"
        AppLanguage.ARABIC -> "إظهار التسمية"
    }

    val customHtmlEditor: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML 编辑器"
        AppLanguage.ENGLISH -> "HTML Editor"
        AppLanguage.ARABIC -> "محرر HTML"
    }

    val customCssEditor: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS 编辑器"
        AppLanguage.ENGLISH -> "CSS Editor"
        AppLanguage.ARABIC -> "محرر CSS"
    }

    val previewUi: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览 UI"
        AppLanguage.ENGLISH -> "Preview UI"
        AppLanguage.ARABIC -> "معاينة الواجهة"
    }
    // ==================== ToolbarItem Labels ====================
    val toolbarSpeed: String get() = when (lang) {
        AppLanguage.CHINESE -> "倍速"
        AppLanguage.ENGLISH -> "Speed"
        AppLanguage.ARABIC -> "السرعة"
    }

    val toolbarSpeedTooltip: String get() = when (lang) {
        AppLanguage.CHINESE -> "调节播放速度"
        AppLanguage.ENGLISH -> "Adjust playback speed"
        AppLanguage.ARABIC -> "ضبط سرعة التشغيل"
    }

    val toolbarPiP: String get() = when (lang) {
        AppLanguage.CHINESE -> "画中画"
        AppLanguage.ENGLISH -> "PiP"
        AppLanguage.ARABIC -> "صورة داخل صورة"
    }

    val toolbarPiPTooltip: String get() = when (lang) {
        AppLanguage.CHINESE -> "开启画中画模式"
        AppLanguage.ENGLISH -> "Enable picture-in-picture mode"
        AppLanguage.ARABIC -> "تفعيل وضع صورة داخل صورة"
    }

    val toolbarLoop: String get() = when (lang) {
        AppLanguage.CHINESE -> "循环"
        AppLanguage.ENGLISH -> "Loop"
        AppLanguage.ARABIC -> "تكرار"
    }

    val toolbarLoopTooltip: String get() = when (lang) {
        AppLanguage.CHINESE -> "切换循环播放"
        AppLanguage.ENGLISH -> "Toggle loop playback"
        AppLanguage.ARABIC -> "تبديل تشغيل التكرار"
    }

    val toolbarScreenshotTooltip: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频截图"
        AppLanguage.ENGLISH -> "Video screenshot"
        AppLanguage.ARABIC -> "لقطة شاشة للفيديو"
    }

    val toolbarCopy: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制"
        AppLanguage.ENGLISH -> "Copy"
        AppLanguage.ARABIC -> "نسخ"
    }

    val toolbarCopyTooltip: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制选中内容"
        AppLanguage.ENGLISH -> "Copy selected content"
        AppLanguage.ARABIC -> "نسخ المحتوى المحدد"
    }

    val toolbarTranslate: String get() = when (lang) {
        AppLanguage.CHINESE -> "翻译"
        AppLanguage.ENGLISH -> "Translate"
        AppLanguage.ARABIC -> "ترجمة"
    }

    val toolbarTranslateTooltip: String get() = when (lang) {
        AppLanguage.CHINESE -> "翻译页面"
        AppLanguage.ENGLISH -> "Translate page"
        AppLanguage.ARABIC -> "ترجمة الصفحة"
    }

    val toolbarScreenshot: String get() = when (lang) {
        AppLanguage.CHINESE -> "截图"
        AppLanguage.ENGLISH -> "Screenshot"
        AppLanguage.ARABIC -> "لقطة شاشة"
    }

    val toolbarWebScreenshotTooltip: String get() = when (lang) {
        AppLanguage.CHINESE -> "网页截图"
        AppLanguage.ENGLISH -> "Web screenshot"
        AppLanguage.ARABIC -> "لقطة شاشة للصفحة"
    }
}
