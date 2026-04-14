package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object CompatStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage
    // ==================== Enhanced Announcement UI Strings ====================
    val optional: String get() = when (lang) {
        AppLanguage.CHINESE -> "可选"
        AppLanguage.ENGLISH -> "Optional"
        AppLanguage.ARABIC -> "اختياري"
    }

    val adBlocking: String get() = when (lang) {
        AppLanguage.CHINESE -> "广告拦截"
        AppLanguage.ENGLISH -> "Ad Blocking"
        AppLanguage.ARABIC -> "حظر الإعلانات"
    }

    val enableAdBlock: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用广告拦截"
        AppLanguage.ENGLISH -> "Enable Ad Blocking"
        AppLanguage.ARABIC -> "تفعيل حظر الإعلانات"
    }

    val desktopMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "访问电脑版"
        AppLanguage.ENGLISH -> "Desktop Mode"
        AppLanguage.ARABIC -> "وضع سطح المكتب"
    }

    val fullscreenMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "全屏模式"
        AppLanguage.ENGLISH -> "Fullscreen Mode"
        AppLanguage.ARABIC -> "وضع ملء الشاشة"
    }

    val splashScreen: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动画面"
        AppLanguage.ENGLISH -> "Splash Screen"
        AppLanguage.ARABIC -> "شاشة البداية"
    }

    val backgroundMusic: String get() = when (lang) {
        AppLanguage.CHINESE -> "背景音乐"
        AppLanguage.ENGLISH -> "Background Music"
        AppLanguage.ARABIC -> "موسيقى الخلفية"
    }

    val autoTranslate: String get() = when (lang) {
        AppLanguage.CHINESE -> "网页自动翻译"
        AppLanguage.ENGLISH -> "Auto Translate"
        AppLanguage.ARABIC -> "الترجمة التلقائية"
    }

    val webViewAdvancedSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "WebView高级设置"
        AppLanguage.ENGLISH -> "WebView Advanced Settings"
        AppLanguage.ARABIC -> "إعدادات WebView المتقدمة"
    }

    val htmlApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML 应用"
        AppLanguage.ENGLISH -> "HTML App"
        AppLanguage.ARABIC -> "تطبيق HTML"
    }

    val frontendApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "前端应用"
        AppLanguage.ENGLISH -> "Frontend App"
        AppLanguage.ARABIC -> "تطبيق الواجهة الأمامية"
    }

    val entryFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "入口文件"
        AppLanguage.ENGLISH -> "Entry File"
        AppLanguage.ARABIC -> "ملف الدخول"
    }

    val totalFilesCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "共 %d 个文件"
        AppLanguage.ENGLISH -> "%d files total"
        AppLanguage.ARABIC -> "إجمالي %d ملفات"
    }

    val imageApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片应用"
        AppLanguage.ENGLISH -> "Image App"
        AppLanguage.ARABIC -> "تطبيق صور"
    }

    val videoApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频应用"
        AppLanguage.ENGLISH -> "Video App"
        AppLanguage.ARABIC -> "تطبيق فيديو"
    }

    val unknownFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "未知文件"
        AppLanguage.ENGLISH -> "Unknown File"
        AppLanguage.ARABIC -> "ملف غير معروف"
    }

    val extensionFabIconLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮按钮图标"
        AppLanguage.ENGLISH -> "FAB Icon"
        AppLanguage.ARABIC -> "أيقونة الزر العائم"
    }

    val fabIconFromGallery: String get() = when (lang) {
        AppLanguage.CHINESE -> "相册"
        AppLanguage.ENGLISH -> "Gallery"
        AppLanguage.ARABIC -> "المعرض"
    }

    val fabIconSelected: String get() = when (lang) {
        AppLanguage.CHINESE -> "已选择自定义图标"
        AppLanguage.ENGLISH -> "Custom icon selected"
        AppLanguage.ARABIC -> "تم تحديد أيقونة مخصصة"
    }

    val fabIconPreviewTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览悬浮按钮图标"
        AppLanguage.ENGLISH -> "Preview FAB Icon"
        AppLanguage.ARABIC -> "معاينة أيقونة الزر العائم"
    }

    val fabIconPreviewDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "这是你选择的图片在悬浮按钮中的效果，确认使用吗？"
        AppLanguage.ENGLISH -> "This is how your image will appear in the floating action button. Use this icon?"
        AppLanguage.ARABIC -> "هذا هو شكل صورتك في الزر العائم. هل تريد استخدام هذه الأيقونة؟"
    }

    val fabIconCustom: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义"
        AppLanguage.ENGLISH -> "Custom"
        AppLanguage.ARABIC -> "مخصص"
    }

    val fabIconChangeImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "更换"
        AppLanguage.ENGLISH -> "Change"
        AppLanguage.ARABIC -> "تغيير"
    }

    val reselect: String get() = when (lang) {
        AppLanguage.CHINESE -> "重新选择"
        AppLanguage.ENGLISH -> "Reselect"
        AppLanguage.ARABIC -> "إعادة الاختيار"
    }
    // ==================== Hardcoded Chinese fix - additional i18n entries ====================
    val adSdkNotIntegrated: String get() = when (lang) {
        AppLanguage.CHINESE -> "广告 SDK 尚未集成，当前广告配置不会生效"
        AppLanguage.ENGLISH -> "Ad SDK not integrated, current ad config will not take effect"
        AppLanguage.ARABIC -> "لم يتم دمج SDK الإعلانات، لن يسري تكوين الإعلانات الحالي"
    }

    val storagePermissionRequiredForExport: String get() = when (lang) {
        AppLanguage.CHINESE -> "需要存储权限才能导出"
        AppLanguage.ENGLISH -> "Storage permission required to export"
        AppLanguage.ARABIC -> "يلزم إذن التخزين للتصدير"
    }

    val shareImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享图片"
        AppLanguage.ENGLISH -> "Share Image"
        AppLanguage.ARABIC -> "مشاركة الصورة"
    }

    val saveFailedCannotProcessHtml: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存失败：无法处理 HTML 文件"
        AppLanguage.ENGLISH -> "Save failed: cannot process HTML file"
        AppLanguage.ARABIC -> "فشل الحفظ: لا يمكن معالجة ملف HTML"
    }

    val phpStartFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP 启动失败"
        AppLanguage.ENGLISH -> "PHP start failed"
        AppLanguage.ARABIC -> "فشل بدء PHP"
    }

    val nodeRuntimeNotFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js 运行时未找到。请使用最新版 WebToApp 重新构建此应用。"
        AppLanguage.ENGLISH -> "Node.js runtime not found. Please rebuild this app with the latest WebToApp."
        AppLanguage.ARABIC -> "لم يتم العثور على وقت تشغيل Node.js. يرجى إعادة بناء هذا التطبيق باستخدام أحدث إصدار من WebToApp."
    }

    val nodeServerStartFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js 服务器启动失败"
        AppLanguage.ENGLISH -> "Node.js server failed to start"
        AppLanguage.ARABIC -> "فشل تشغيل خادم Node.js"
    }

    val nodeStartFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js 启动失败"
        AppLanguage.ENGLISH -> "Node.js failed to start"
        AppLanguage.ARABIC -> "فشل تشغيل Node.js"
    }

    val preparingNodeEnv: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在准备 Node.js 环境..."
        AppLanguage.ENGLISH -> "Preparing Node.js environment..."
        AppLanguage.ARABIC -> "جاري تحضير بيئة Node.js..."
    }

    val startingNodeServer: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在启动 Node.js 服务器..."
        AppLanguage.ENGLISH -> "Starting Node.js server..."
        AppLanguage.ARABIC -> "جاري تشغيل خادم Node.js..."
    }

    val pythonRuntimeNotFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python 运行时未找到。请使用最新版 WebToApp 重新构建此应用，并确保已下载 Python 运行时依赖。"
        AppLanguage.ENGLISH -> "Python runtime not found. Please rebuild this app with the latest WebToApp and ensure Python runtime dependency is downloaded."
        AppLanguage.ARABIC -> "لم يتم العثور على وقت تشغيل Python. يرجى إعادة بناء هذا التطبيق باستخدام أحدث إصدار من WebToApp والتأكد من تنزيل تبعيات وقت تشغيل Python."
    }

    val pythonServerStartFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python 服务器启动失败"
        AppLanguage.ENGLISH -> "Python server failed to start"
        AppLanguage.ARABIC -> "فشل تشغيل خادم Python"
    }

    val pythonStartFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python 启动失败"
        AppLanguage.ENGLISH -> "Python failed to start"
        AppLanguage.ARABIC -> "فشل تشغيل Python"
    }

    val pythonServerTimeout: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python 服务器启动超时"
        AppLanguage.ENGLISH -> "Python server startup timeout"
        AppLanguage.ARABIC -> "انتهت مهلة تشغيل خادم Python"
    }

    val preparingPythonEnv: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在准备 Python 环境..."
        AppLanguage.ENGLISH -> "Preparing Python environment..."
        AppLanguage.ARABIC -> "جاري تحضير بيئة Python..."
    }

    val startingPythonServer: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在启动 Python 服务器..."
        AppLanguage.ENGLISH -> "Starting Python server..."
        AppLanguage.ARABIC -> "جاري تشغيل خادم Python..."
    }

    val goBinaryNotFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "未找到可执行的 Go 二进制文件。请确保项目中包含预编译的 ARM/ARM64 二进制。"
        AppLanguage.ENGLISH -> "No executable Go binary found. Please ensure the project contains a pre-compiled ARM/ARM64 binary."
        AppLanguage.ARABIC -> "لم يتم العثور على ملف ثنائي لـ Go قابل للتنفيذ. يرجى التأكد من أن المشروع يحتوي على ملف ثنائي ARM/ARM64 مترجم مسبقاً."
    }

    val goServerStartFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Go 服务器启动失败"
        AppLanguage.ENGLISH -> "Go server failed to start"
        AppLanguage.ARABIC -> "فشل تشغيل خادم Go"
    }

    val goStartFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Go 启动失败"
        AppLanguage.ENGLISH -> "Go failed to start"
        AppLanguage.ARABIC -> "فشل تشغيل Go"
    }

    val preparingGoEnv: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在准备 Go 环境..."
        AppLanguage.ENGLISH -> "Preparing Go environment..."
        AppLanguage.ARABIC -> "جاري تحضير بيئة Go..."
    }

    val startingGoServer: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在启动 Go 服务器..."
        AppLanguage.ENGLISH -> "Starting Go server..."
        AppLanguage.ARABIC -> "جاري تشغيل خادم Go..."
    }

    val wpStartFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress 启动失败"
        AppLanguage.ENGLISH -> "WordPress failed to start"
        AppLanguage.ARABIC -> "فشل تشغيل WordPress"
    }

    val tapToReturnToApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击返回应用"
        AppLanguage.ENGLISH -> "Tap to return to app"
        AppLanguage.ARABIC -> "اضغط للعودة إلى التطبيق"
    }

    val runtimeDownloadChannel: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行时下载"
        AppLanguage.ENGLISH -> "Runtime Download"
        AppLanguage.ARABIC -> "تنزيل وقت التشغيل"
    }

    val runtimeDownloadChannelDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示运行时依赖下载进度"
        AppLanguage.ENGLISH -> "Shows runtime dependency download progress"
        AppLanguage.ARABIC -> "يعرض تقدم تنزيل التبعيات"
    }

    val depDownloadRemaining: String get() = when (lang) {
        AppLanguage.CHINESE -> "剩余"
        AppLanguage.ENGLISH -> "Remaining"
        AppLanguage.ARABIC -> "المتبقي"
    }

    val depDownloadStarted: String get() = when (lang) {
        AppLanguage.CHINESE -> "开始"
        AppLanguage.ENGLISH -> "Started"
        AppLanguage.ARABIC -> "بدأ"
    }

    val depDownloadPause: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂停"
        AppLanguage.ENGLISH -> "Pause"
        AppLanguage.ARABIC -> "إيقاف مؤقت"
    }

    val depDownloadResume: String get() = when (lang) {
        AppLanguage.CHINESE -> "继续"
        AppLanguage.ENGLISH -> "Resume"
        AppLanguage.ARABIC -> "استئناف"
    }

    val depDownloadPaused: String get() = when (lang) {
        AppLanguage.CHINESE -> "已暂停"
        AppLanguage.ENGLISH -> "Paused"
        AppLanguage.ARABIC -> "متوقف مؤقتًا"
    }

    val depDownloadExtracting: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在解压"
        AppLanguage.ENGLISH -> "Extracting"
        AppLanguage.ARABIC -> "جارٍ الاستخراج"
    }

    val depDownloadComplete: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行时下载完成"
        AppLanguage.ENGLISH -> "Runtime download complete"
        AppLanguage.ARABIC -> "اكتمل تنزيل وقت التشغيل"
    }

    val depDownloadAllReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "所有依赖已就绪"
        AppLanguage.ENGLISH -> "All dependencies ready"
        AppLanguage.ARABIC -> "جميع التبعيات جاهزة"
    }

    val tapToEnterPasswordToExit: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击输入密码退出"
        AppLanguage.ENGLISH -> "Tap to enter password to exit"
        AppLanguage.ARABIC -> "اضغط لإدخال كلمة المرور للخروج"
    }

    val enterExitPassword: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入退出密码"
        AppLanguage.ENGLISH -> "Enter Exit Password"
        AppLanguage.ARABIC -> "أدخل كلمة مرور الخروج"
    }

    val enterAdminPasswordToExit: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入管理员密码以退出强制运行模式"
        AppLanguage.ENGLISH -> "Enter admin password to exit forced run mode"
        AppLanguage.ARABIC -> "أدخل كلمة مرور المسؤول للخروج من وضع التشغيل الإجباري"
    }

    val passwordLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "密码"
        AppLanguage.ENGLISH -> "Password"
        AppLanguage.ARABIC -> "كلمة المرور"
    }

    val enterPasswordPlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入密码"
        AppLanguage.ENGLISH -> "Enter password"
        AppLanguage.ARABIC -> "أدخل كلمة المرور"
    }

    val wrongPasswordAttemptsRemaining: String get() = when (lang) {
        AppLanguage.CHINESE -> "密码错误，剩余 %d 次尝试"
        AppLanguage.ENGLISH -> "Wrong password, %d attempts remaining"
        AppLanguage.ARABIC -> "كلمة مرور خاطئة، %d محاولات متبقية"
    }

    val tooManyAttemptsTryLater: String get() = when (lang) {
        AppLanguage.CHINESE -> "尝试次数过多，请稍后再试"
        AppLanguage.ENGLISH -> "Too many attempts, please try again later"
        AppLanguage.ARABIC -> "محاولات كثيرة جدًا، يرجى المحاولة لاحقًا"
    }

    val confirmExit: String get() = when (lang) {
        AppLanguage.CHINESE -> "确认退出"
        AppLanguage.ENGLISH -> "Confirm Exit"
        AppLanguage.ARABIC -> "تأكيد الخروج"
    }

    val forcedRunPermissionTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制运行权限设置"
        AppLanguage.ENGLISH -> "Forced Run Permission Settings"
        AppLanguage.ARABIC -> "إعدادات أذونات التشغيل الإجباري"
    }

    val forcedRunPermissionDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "为了确保强制运行功能有效，请授权以下权限："
        AppLanguage.ENGLISH -> "To ensure forced run works properly, please grant the following permissions:"
        AppLanguage.ARABIC -> "لضمان عمل التشغيل الإجباري بشكل صحيح، يرجى منح الأذونات التالية:"
    }

    val accessibilityService: String get() = when (lang) {
        AppLanguage.CHINESE -> "辅助功能服务"
        AppLanguage.ENGLISH -> "Accessibility Service"
        AppLanguage.ARABIC -> "خدمة إمكانية الوصول"
    }

    val accessibilityServiceDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监控窗口变化，防止用户切换应用"
        AppLanguage.ENGLISH -> "Monitor window changes to prevent app switching"
        AppLanguage.ARABIC -> "مراقبة تغييرات النافذة لمنع التبديل بين التطبيقات"
    }

    val usageAccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用情况访问"
        AppLanguage.ENGLISH -> "Usage Access"
        AppLanguage.ARABIC -> "الوصول إلى الاستخدام"
    }

    val usageAccessDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测当前前台应用，提供双重防护"
        AppLanguage.ENGLISH -> "Detect foreground app for dual protection"
        AppLanguage.ARABIC -> "اكتشاف التطبيق الأمامي للحماية المزدوجة"
    }

    val refreshPermissionStatus: String get() = when (lang) {
        AppLanguage.CHINESE -> "刷新权限状态"
        AppLanguage.ENGLISH -> "Refresh Permission Status"
        AppLanguage.ARABIC -> "تحديث حالة الأذونات"
    }

    val grant: String get() = when (lang) {
        AppLanguage.CHINESE -> "授权"
        AppLanguage.ENGLISH -> "Grant"
        AppLanguage.ARABIC -> "منح"
    }

    val granted: String get() = when (lang) {
        AppLanguage.CHINESE -> "已授权"
        AppLanguage.ENGLISH -> "Granted"
        AppLanguage.ARABIC -> "تم المنح"
    }

    val protectionBasic: String get() = when (lang) {
        AppLanguage.CHINESE -> "基础防护"
        AppLanguage.ENGLISH -> "Basic Protection"
        AppLanguage.ARABIC -> "حماية أساسية"
    }

    val protectionBasicDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "仅拦截返回键，防护效果有限"
        AppLanguage.ENGLISH -> "Back key interception only, limited protection"
        AppLanguage.ARABIC -> "اعتراض مفتاح الرجوع فقط، حماية محدودة"
    }

    val protectionStandard: String get() = when (lang) {
        AppLanguage.CHINESE -> "标准防护"
        AppLanguage.ENGLISH -> "Standard Protection"
        AppLanguage.ARABIC -> "حماية قياسية"
    }

    val protectionStandardDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "通过辅助功能监控窗口，有效阻止应用切换"
        AppLanguage.ENGLISH -> "Window monitoring via accessibility, effectively prevents app switching"
        AppLanguage.ARABIC -> "مراقبة النوافذ عبر إمكانية الوصول، يمنع التبديل بين التطبيقات بفعالية"
    }

    val protectionMaximum: String get() = when (lang) {
        AppLanguage.CHINESE -> "最强防护"
        AppLanguage.ENGLISH -> "Maximum Protection"
        AppLanguage.ARABIC -> "أقصى حماية"
    }

    val protectionMaximumDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "辅助功能 + 后台守护服务，双重防护确保万无一失"
        AppLanguage.ENGLISH -> "Accessibility + background guard service, dual protection for maximum security"
        AppLanguage.ARABIC -> "إمكانية الوصول + خدمة حراسة خلفية، حماية مزدوجة لأقصى أمان"
    }

    val permissionsReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "权限已就绪"
        AppLanguage.ENGLISH -> "Permissions Ready"
        AppLanguage.ARABIC -> "الأذونات جاهزة"
    }

    val permissionsNeeded: String get() = when (lang) {
        AppLanguage.CHINESE -> "需要授权"
        AppLanguage.ENGLISH -> "Permissions Needed"
        AppLanguage.ARABIC -> "الأذونات مطلوبة"
    }

    val start: String get() = when (lang) {
        AppLanguage.CHINESE -> "开始"
        AppLanguage.ENGLISH -> "Start"
        AppLanguage.ARABIC -> "بدء"
    }

    val skipDegradedProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "跳过（降级防护）"
        AppLanguage.ENGLISH -> "Skip (degraded protection)"
        AppLanguage.ARABIC -> "تخطي (حماية مخفضة)"
    }

    val htmlFileTooLarge: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML 文件较大（%sMB），已跳过内容分析"
        AppLanguage.ENGLISH -> "HTML file is large (%sMB), content analysis skipped"
        AppLanguage.ARABIC -> "ملف HTML كبير (%sMB)، تم تخطي تحليل المحتوى"
    }

    val resourceReferenceIssue: String get() = when (lang) {
        AppLanguage.CHINESE -> "资源引用问题"
        AppLanguage.ENGLISH -> "Resource reference issue"
        AppLanguage.ARABIC -> "مشكلة في مرجع المورد"
    }

    val htmlFileNotFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML 文件不存在"
        AppLanguage.ENGLISH -> "HTML file not found"
        AppLanguage.ARABIC -> "ملف HTML غير موجود"
    }

    val cssEncodingWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS 文件编码为 %s，建议使用 UTF-8"
        AppLanguage.ENGLISH -> "CSS file encoding is %s, UTF-8 recommended"
        AppLanguage.ARABIC -> "ترميز ملف CSS هو %s، يوصى باستخدام UTF-8"
    }

    val documentWriteWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用了 document.write()，可能导致页面加载问题"
        AppLanguage.ENGLISH -> "Uses document.write(), may cause page loading issues"
        AppLanguage.ARABIC -> "يستخدم document.write()، قد يسبب مشاكل في تحميل الصفحة"
    }

    val tagNotProperlyClosed: String get() = when (lang) {
        AppLanguage.CHINESE -> "标签 <%s> 未正确闭合"
        AppLanguage.ENGLISH -> "Tag <%s> not properly closed"
        AppLanguage.ARABIC -> "الوسم <%s> لم يُغلق بشكل صحيح"
    }

    val unexpectedClosingTag: String get() = when (lang) {
        AppLanguage.CHINESE -> "意外的闭合标签 </%s>"
        AppLanguage.ENGLISH -> "Unexpected closing tag </%s>"
        AppLanguage.ARABIC -> "وسم إغلاق غير متوقع </%s>"
    }

    val tagNotClosed: String get() = when (lang) {
        AppLanguage.CHINESE -> "标签 <%s> 未闭合"
        AppLanguage.ENGLISH -> "Tag <%s> not closed"
        AppLanguage.ARABIC -> "الوسم <%s> لم يُغلق"
    }

    val extraClosingBrace: String get() = when (lang) {
        AppLanguage.CHINESE -> "多余的闭合大括号 '}'"
        AppLanguage.ENGLISH -> "Extra closing brace '}'"
        AppLanguage.ARABIC -> "قوس إغلاق زائد '}'"
    }

    val missingClosingBraces: String get() = when (lang) {
        AppLanguage.CHINESE -> "缺少 %d 个闭合大括号 '}'"
        AppLanguage.ENGLISH -> "Missing %d closing brace(s) '}'"
        AppLanguage.ARABIC -> "نقص %d قوس(أقواس) إغلاق '}'"
    }

    val extraClosingBraces: String get() = when (lang) {
        AppLanguage.CHINESE -> "多余 %d 个闭合大括号 '}'"
        AppLanguage.ENGLISH -> "Extra %d closing brace(s) '}'"
        AppLanguage.ARABIC -> "زيادة %d قوس(أقواس) إغلاق '}'"
    }

    val missingClosingParens: String get() = when (lang) {
        AppLanguage.CHINESE -> "缺少 %d 个闭合小括号 ')'"
        AppLanguage.ENGLISH -> "Missing %d closing paren(s) ')'"
        AppLanguage.ARABIC -> "نقص %d قوس(أقواس) إغلاق ')'"
    }

    val extraClosingParens: String get() = when (lang) {
        AppLanguage.CHINESE -> "多余 %d 个闭合小括号 ')'"
        AppLanguage.ENGLISH -> "Extra %d closing paren(s) ')'"
        AppLanguage.ARABIC -> "زيادة %d قوس(أقواس) إغلاق ')'"
    }

    val missingClosingBrackets: String get() = when (lang) {
        AppLanguage.CHINESE -> "缺少 %d 个闭合方括号 ']'"
        AppLanguage.ENGLISH -> "Missing %d closing bracket(s) ']'"
        AppLanguage.ARABIC -> "نقص %d قوس(أقواس) مربع إغلاق ']'"
    }

    val extraClosingBrackets: String get() = when (lang) {
        AppLanguage.CHINESE -> "多余 %d 个闭合方括号 ']'"
        AppLanguage.ENGLISH -> "Extra %d closing bracket(s) ']'"
        AppLanguage.ARABIC -> "زيادة %d قوس(أقواس) مربع إغلاق ']'"
    }

    val gameScore: String get() = when (lang) {
        AppLanguage.CHINESE -> "得分"
        AppLanguage.ENGLISH -> "Score"
        AppLanguage.ARABIC -> "النتيجة"
    }

    val gameLives: String get() = when (lang) {
        AppLanguage.CHINESE -> "生命"
        AppLanguage.ENGLISH -> "Lives"
        AppLanguage.ARABIC -> "الأرواح"
    }

    val gameOver: String get() = when (lang) {
        AppLanguage.CHINESE -> "游戏结束"
        AppLanguage.ENGLISH -> "Game Over"
        AppLanguage.ARABIC -> "انتهت اللعبة"
    }

    val gameYouWin: String get() = when (lang) {
        AppLanguage.CHINESE -> "恭喜通关！"
        AppLanguage.ENGLISH -> "You Win!"
        AppLanguage.ARABIC -> "فزت!"
    }

    val gameTapToRestart: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击重新开始"
        AppLanguage.ENGLISH -> "Tap to restart"
        AppLanguage.ARABIC -> "اضغط لإعادة البدء"
    }

    val gameMazeComplete: String get() = when (lang) {
        AppLanguage.CHINESE -> "迷宫通关！"
        AppLanguage.ENGLISH -> "Maze Complete!"
        AppLanguage.ARABIC -> "اكتملت المتاهة!"
    }

    val gameSteps: String get() = when (lang) {
        AppLanguage.CHINESE -> "步数"
        AppLanguage.ENGLISH -> "Steps"
        AppLanguage.ARABIC -> "الخطوات"
    }

    val gameCollected: String get() = when (lang) {
        AppLanguage.CHINESE -> "收集"
        AppLanguage.ENGLISH -> "Collected"
        AppLanguage.ARABIC -> "تم جمع"
    }

    val gameCollectedStars: String get() = when (lang) {
        AppLanguage.CHINESE -> "收集了 %d 颗星星"
        AppLanguage.ENGLISH -> "Collected %d stars"
        AppLanguage.ARABIC -> "تم جمع %d نجوم"
    }

    val gameTouchToPaint: String get() = when (lang) {
        AppLanguage.CHINESE -> "触摸屏幕，随心落墨"
        AppLanguage.ENGLISH -> "Touch to paint freely"
        AppLanguage.ARABIC -> "المس للرسم بحرية"
    }

    val gameZen: String get() = when (lang) {
        AppLanguage.CHINESE -> "— 禅 —"
        AppLanguage.ENGLISH -> "— Zen —"
        AppLanguage.ARABIC -> "— زن —"
    }

    val downloadFailedHttp: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载失败: HTTP %d"
        AppLanguage.ENGLISH -> "Download failed: HTTP %d"
        AppLanguage.ARABIC -> "فشل التنزيل: HTTP %d"
    }

    val downloadReturnedEmpty: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载返回空内容"
        AppLanguage.ENGLISH -> "Download returned empty content"
        AppLanguage.ARABIC -> "التنزيل أرجع محتوى فارغ"
    }

    val downloadNameFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载 %s 失败: %s"
        AppLanguage.ENGLISH -> "Download %s failed: %s"
        AppLanguage.ARABIC -> "فشل تنزيل %s: %s"
    }

    val sizeUnknown: String get() = when (lang) {
        AppLanguage.CHINESE -> "未知"
        AppLanguage.ENGLISH -> "Unknown"
        AppLanguage.ARABIC -> "غير معروف"
    }

    val saveFailedNoHtmlInZip: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存失败：ZIP 中未找到 HTML 文件"
        AppLanguage.ENGLISH -> "Save failed: no HTML file found in ZIP"
        AppLanguage.ARABIC -> "فشل الحفظ: لم يتم العثور على ملف HTML في ZIP"
    }

    val suggestUseRelativePath: String get() = when (lang) {
        AppLanguage.CHINESE -> "建议使用相对路径，如 './style.css' 而非 '/style.css'"
        AppLanguage.ENGLISH -> "Use relative paths, e.g. './style.css' instead of '/style.css'"
        AppLanguage.ARABIC -> "استخدم مسارات نسبية، مثل './style.css' بدلاً من '/style.css'"
    }

    val suggestEnsureFileImported: String get() = when (lang) {
        AppLanguage.CHINESE -> "请确保引用的文件 '%s' 已导入"
        AppLanguage.ENGLISH -> "Ensure the referenced file '%s' has been imported"
        AppLanguage.ARABIC -> "تأكد من استيراد الملف المُشار إليه '%s'"
    }

    val suggestSaveAsUtf8: String get() = when (lang) {
        AppLanguage.CHINESE -> "将文件另存为 UTF-8 编码可避免乱码问题"
        AppLanguage.ENGLISH -> "Save the file as UTF-8 encoding to avoid character encoding issues"
        AppLanguage.ARABIC -> "احفظ الملف بترميز UTF-8 لتجنب مشاكل الترميز"
    }

    val suggestExternalFilesDetected: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测到 HTML 中引用了外部 CSS/JS 文件，请确保已导入对应文件"
        AppLanguage.ENGLISH -> "External CSS/JS files referenced in HTML detected, please ensure they have been imported"
        AppLanguage.ARABIC -> "تم اكتشاف ملفات CSS/JS خارجية مُشار إليها في HTML، يرجى التأكد من استيرادها"
    }

    val suggestUseRelativePathsForAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "建议将所有资源路径改为相对路径，以确保在应用中正常加载"
        AppLanguage.ENGLISH -> "Use relative paths for all resources to ensure proper loading in the app"
        AppLanguage.ARABIC -> "استخدم مسارات نسبية لجميع الموارد لضمان التحميل الصحيح في التطبيق"
    }

    val absolutePathWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用了绝对路径，可能导致资源无法加载"
        AppLanguage.ENGLISH -> "Absolute path used, resource may fail to load"
        AppLanguage.ARABIC -> "تم استخدام مسار مطلق، قد يفشل تحميل المورد"
    }

    val referencedFileNotExist: String get() = when (lang) {
        AppLanguage.CHINESE -> "引用的文件不存在"
        AppLanguage.ENGLISH -> "Referenced file does not exist"
        AppLanguage.ARABIC -> "الملف المُشار إليه غير موجود"
    }

    val suggestUseDomMethods: String get() = when (lang) {
        AppLanguage.CHINESE -> "建议使用 DOM 操作方法替代 document.write()"
        AppLanguage.ENGLISH -> "Use DOM manipulation methods instead of document.write()"
        AppLanguage.ARABIC -> "استخدم طرق معالجة DOM بدلاً من document.write()"
    }

    val possiblyUnclosedBraces: String get() = when (lang) {
        AppLanguage.CHINESE -> "可能存在未闭合的大括号"
        AppLanguage.ENGLISH -> "Possibly unclosed braces detected"
        AppLanguage.ARABIC -> "تم اكتشاف أقواس ربما غير مغلقة"
    }

    val suggestCheckBracesPaired: String get() = when (lang) {
        AppLanguage.CHINESE -> "请检查 JS 代码中的大括号是否正确配对"
        AppLanguage.ENGLISH -> "Check that braces in JS code are properly paired"
        AppLanguage.ARABIC -> "تحقق من أن الأقواس في كود JS مقترنة بشكل صحيح"
    }

    val notifDownloadChannel: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载"
        AppLanguage.ENGLISH -> "Downloads"
        AppLanguage.ARABIC -> "التنزيلات"
    }

    val notifDownloadChannelDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示文件下载进度和完成通知"
        AppLanguage.ENGLISH -> "Show file download progress and completion notifications"
        AppLanguage.ARABIC -> "عرض تقدم تنزيل الملفات وإشعارات الاكتمال"
    }

    val notifDownloadComplete: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载完成"
        AppLanguage.ENGLISH -> "Download complete"
        AppLanguage.ARABIC -> "اكتمل التنزيل"
    }

    val notifDownloadFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载失败"
        AppLanguage.ENGLISH -> "Download failed"
        AppLanguage.ARABIC -> "فشل التنزيل"
    }

    val notifSaving: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存中"
        AppLanguage.ENGLISH -> "Saving"
        AppLanguage.ARABIC -> "جاري الحفظ"
    }

    val notifDownloading: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载中"
        AppLanguage.ENGLISH -> "Downloading"
        AppLanguage.ARABIC -> "جاري التنزيل"
    }

    val notifSaveCompleted: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存完成"
        AppLanguage.ENGLISH -> "Save completed"
        AppLanguage.ARABIC -> "اكتمل الحفظ"
    }

    val notifSavedToGallery: String get() = when (lang) {
        AppLanguage.CHINESE -> "%s 已保存到相册"
        AppLanguage.ENGLISH -> "%s saved to gallery"
        AppLanguage.ARABIC -> "تم حفظ %s في المعرض"
    }

    val notifSaveFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存失败"
        AppLanguage.ENGLISH -> "Save failed"
        AppLanguage.ARABIC -> "فشل الحفظ"
    }

    val notifView: String get() = when (lang) {
        AppLanguage.CHINESE -> "查看"
        AppLanguage.ENGLISH -> "View"
        AppLanguage.ARABIC -> "عرض"
    }

    val notifShare: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享"
        AppLanguage.ENGLISH -> "Share"
        AppLanguage.ARABIC -> "مشاركة"
    }

    val notifOpen: String get() = when (lang) {
        AppLanguage.CHINESE -> "打开"
        AppLanguage.ENGLISH -> "Open"
        AppLanguage.ARABIC -> "فتح"
    }

    val notifShareType: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享 %s"
        AppLanguage.ENGLISH -> "Share %s"
        AppLanguage.ARABIC -> "مشاركة %s"
    }

    val mediaTypeImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片"
        AppLanguage.ENGLISH -> "Image"
        AppLanguage.ARABIC -> "صورة"
    }

    val mediaTypeVideo: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频"
        AppLanguage.ENGLISH -> "Video"
        AppLanguage.ARABIC -> "فيديو"
    }

    val failedSaveIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "图标保存失败，请重试"
        AppLanguage.ENGLISH -> "Failed to save icon, please retry"
        AppLanguage.ARABIC -> "فشل حفظ الأيقونة، يرجى المحاولة مرة أخرى"
    }

    val failedSaveSplash: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动画面保存失败，请重试"
        AppLanguage.ENGLISH -> "Failed to save splash, please retry"
        AppLanguage.ARABIC -> "فشل حفظ شاشة البداية، يرجى المحاولة مرة أخرى"
    }

    val deleteFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除失败"
        AppLanguage.ENGLISH -> "Delete failed"
        AppLanguage.ARABIC -> "فشل الحذف"
    }

    val pleaseEnterAppName: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入应用名称"
        AppLanguage.ENGLISH -> "Please enter app name"
        AppLanguage.ARABIC -> "يرجى إدخال اسم التطبيق"
    }

    val pleaseEnterWebsiteUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入网站 URL"
        AppLanguage.ENGLISH -> "Please enter website URL"
        AppLanguage.ARABIC -> "يرجى إدخال عنوان URL للموقع"
    }

    val pleaseEnterValidUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入有效的 URL"
        AppLanguage.ENGLISH -> "Please enter a valid URL"
        AppLanguage.ARABIC -> "يرجى إدخال عنوان URL صالح"
    }

    val insecureHttpWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "⚠️ 警告：HTTP 是不安全的协议，数据可能被窃听。仅在你信任的网络环境（如本地测试、内网）使用。"
        AppLanguage.ENGLISH -> "⚠️ Warning: HTTP is an insecure protocol and data may be intercepted. Only use in trusted networks (local testing, intranet)."
        AppLanguage.ARABIC -> "⚠️ تحذير: HTTP هي بروتوكول غير آمن وقد يتم اعتراض البيانات. استخدمه فقط في الشبكات الموثوقة (الاختبار المحلي، الشبكة الداخلية)."
    }

    val allowHttpCheckbox: String get() = when (lang) {
        AppLanguage.CHINESE -> "我了解风险，仍要使用 HTTP"
        AppLanguage.ENGLISH -> "I understand the risks and still want to use HTTP"
        AppLanguage.ARABIC -> "أفهم المخاطر ولا أزال أرغب في استخدام HTTP"
    }

    val pleaseSelectHtmlFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "请选择 HTML 文件"
        AppLanguage.ENGLISH -> "Please select HTML file"
        AppLanguage.ARABIC -> "يرجى اختيار ملف HTML"
    }

    val mediaFilePathEmpty: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体文件路径不能为空"
        AppLanguage.ENGLISH -> "Media file path cannot be empty"
        AppLanguage.ARABIC -> "مسار ملف الوسائط لا يمكن أن يكون فارغاً"
    }

    val failedSaveMediaFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体文件保存失败"
        AppLanguage.ENGLISH -> "Failed to save media file"
        AppLanguage.ARABIC -> "فشل حفظ ملف الوسائط"
    }

    val creationFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建失败: %s"
        AppLanguage.ENGLISH -> "Creation failed: %s"
        AppLanguage.ARABIC -> "فشل الإنشاء: %s"
    }

    val pleaseAddMediaFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "请至少添加一个媒体文件"
        AppLanguage.ENGLISH -> "Please add at least one media file"
        AppLanguage.ARABIC -> "يرجى إضافة ملف وسائط واحد على الأقل"
    }

    val updateFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "更新失败: %s"
        AppLanguage.ENGLISH -> "Update failed: %s"
        AppLanguage.ARABIC -> "فشل التحديث: %s"
    }

    val failedCreateCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建分类失败: %s"
        AppLanguage.ENGLISH -> "Failed to create category: %s"
        AppLanguage.ARABIC -> "فشل إنشاء الفئة: %s"
    }

    val failedUpdateCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "更新分类失败: %s"
        AppLanguage.ENGLISH -> "Failed to update category: %s"
        AppLanguage.ARABIC -> "فشل تحديث الفئة: %s"
    }

    val failedDeleteCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除分类失败: %s"
        AppLanguage.ENGLISH -> "Failed to delete category: %s"
        AppLanguage.ARABIC -> "فشل حذف الفئة: %s"
    }

    val moveFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "移动失败: %s"
        AppLanguage.ENGLISH -> "Move failed: %s"
        AppLanguage.ARABIC -> "فشل النقل: %s"
    }

    val cdBack: String get() = when (lang) {
        AppLanguage.CHINESE -> "返回"
        AppLanguage.ENGLISH -> "Back"
        AppLanguage.ARABIC -> "رجوع"
    }

    val cdForward: String get() = when (lang) {
        AppLanguage.CHINESE -> "前进"
        AppLanguage.ENGLISH -> "Forward"
        AppLanguage.ARABIC -> "للأمام"
    }

    val cdRefresh: String get() = when (lang) {
        AppLanguage.CHINESE -> "刷新"
        AppLanguage.ENGLISH -> "Refresh"
        AppLanguage.ARABIC -> "تحديث"
    }

    val cdHome: String get() = when (lang) {
        AppLanguage.CHINESE -> "主页"
        AppLanguage.ENGLISH -> "Home"
        AppLanguage.ARABIC -> "الصفحة الرئيسية"
    }

    val cdPause: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂停"
        AppLanguage.ENGLISH -> "Pause"
        AppLanguage.ARABIC -> "إيقاف مؤقت"
    }

    val cdPlay: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放"
        AppLanguage.ENGLISH -> "Play"
        AppLanguage.ARABIC -> "تشغيل"
    }

    val cdPrevious: String get() = when (lang) {
        AppLanguage.CHINESE -> "上一个"
        AppLanguage.ENGLISH -> "Previous"
        AppLanguage.ARABIC -> "السابق"
    }

    val cdNext: String get() = when (lang) {
        AppLanguage.CHINESE -> "下一个"
        AppLanguage.ENGLISH -> "Next"
        AppLanguage.ARABIC -> "التالي"
    }

    val cdSeekBack: String get() = when (lang) {
        AppLanguage.CHINESE -> "快退"
        AppLanguage.ENGLISH -> "Seek Back"
        AppLanguage.ARABIC -> "ترجيع"
    }

    val cdSeekForward: String get() = when (lang) {
        AppLanguage.CHINESE -> "快进"
        AppLanguage.ENGLISH -> "Seek Forward"
        AppLanguage.ARABIC -> "تقديم"
    }

    val cdSplashScreen: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动画面"
        AppLanguage.ENGLISH -> "Splash screen"
        AppLanguage.ARABIC -> "شاشة البداية"
    }

    val cdMediaContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体内容"
        AppLanguage.ENGLISH -> "Media content"
        AppLanguage.ARABIC -> "محتوى الوسائط"
    }

    val cdCover: String get() = when (lang) {
        AppLanguage.CHINESE -> "封面"
        AppLanguage.ENGLISH -> "Cover"
        AppLanguage.ARABIC -> "غلاف"
    }

    val cdPreview: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览"
        AppLanguage.ENGLISH -> "Preview"
        AppLanguage.ARABIC -> "معاينة"
    }

    val cdCopy: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制"
        AppLanguage.ENGLISH -> "Copy"
        AppLanguage.ARABIC -> "نسخ"
    }

    val cdMore: String get() = when (lang) {
        AppLanguage.CHINESE -> "更多"
        AppLanguage.ENGLISH -> "More"
        AppLanguage.ARABIC -> "المزيد"
    }

    val cdClose: String get() = when (lang) {
        AppLanguage.CHINESE -> "关闭"
        AppLanguage.ENGLISH -> "Close"
        AppLanguage.ARABIC -> "إغلاق"
    }

    val cdRemove: String get() = when (lang) {
        AppLanguage.CHINESE -> "移除"
        AppLanguage.ENGLISH -> "Remove"
        AppLanguage.ARABIC -> "إزالة"
    }

    val cdRetry: String get() = when (lang) {
        AppLanguage.CHINESE -> "重试"
        AppLanguage.ENGLISH -> "Retry"
        AppLanguage.ARABIC -> "إعادة المحاولة"
    }

    val cdCollapse: String get() = when (lang) {
        AppLanguage.CHINESE -> "折叠"
        AppLanguage.ENGLISH -> "Collapse"
        AppLanguage.ARABIC -> "طي"
    }

    val cdExpand: String get() = when (lang) {
        AppLanguage.CHINESE -> "展开"
        AppLanguage.ENGLISH -> "Expand"
        AppLanguage.ARABIC -> "توسيع"
    }

    val hideRawResponse: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏原始响应"
        AppLanguage.ENGLISH -> "Hide raw response"
        AppLanguage.ARABIC -> "إخفاء الاستجابة الأصلية"
    }

    val viewRawResponse: String get() = when (lang) {
        AppLanguage.CHINESE -> "查看原始响应"
        AppLanguage.ENGLISH -> "View raw response"
        AppLanguage.ARABIC -> "عرض الاستجابة الأصلية"
    }

    val shieldsPrivacyProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "Shields 隐私保护"
        AppLanguage.ENGLISH -> "Shields Privacy Protection"
        AppLanguage.ARABIC -> "حماية الخصوصية Shields"
    }

    val shieldsPrivacySubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截广告、跟踪器、Cookie 弹窗，自动升级 HTTPS"
        AppLanguage.ENGLISH -> "Block ads, trackers, cookie popups, auto-upgrade HTTPS"
        AppLanguage.ARABIC -> "حظر الإعلانات والمتتبعات ونوافذ الكوكيز، ترقية HTTPS تلقائياً"
    }

    val shieldsMasterSwitch: String get() = when (lang) {
        AppLanguage.CHINESE -> "Shields 总开关"
        AppLanguage.ENGLISH -> "Shields Master Switch"
        AppLanguage.ARABIC -> "المفتاح الرئيسي لـ Shields"
    }

    val shieldsEnabledWithRules: String get() = when (lang) {
        AppLanguage.CHINESE -> "已启用 · 拦截规则 %d 条"
        AppLanguage.ENGLISH -> "Enabled · %d blocking rules"
        AppLanguage.ARABIC -> "مفعّل · %d قاعدة حظر"
    }

    val shieldsDisabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "已关闭"
        AppLanguage.ENGLISH -> "Disabled"
        AppLanguage.ARABIC -> "معطّل"
    }

    val shieldsStatAds: String get() = when (lang) {
        AppLanguage.CHINESE -> "广告"
        AppLanguage.ENGLISH -> "Ads"
        AppLanguage.ARABIC -> "إعلانات"
    }

    val shieldsStatTrackers: String get() = when (lang) {
        AppLanguage.CHINESE -> "跟踪器"
        AppLanguage.ENGLISH -> "Trackers"
        AppLanguage.ARABIC -> "متتبعات"
    }

    val shieldsCollapseSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "收起详细设置"
        AppLanguage.ENGLISH -> "Collapse settings"
        AppLanguage.ARABIC -> "طي الإعدادات"
    }

    val shieldsExpandSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "展开详细设置"
        AppLanguage.ENGLISH -> "Expand settings"
        AppLanguage.ARABIC -> "توسيع الإعدادات"
    }

    val shieldsHttpsUpgradeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动将 HTTP 请求升级为加密连接"
        AppLanguage.ENGLISH -> "Automatically upgrade HTTP requests to encrypted connections"
        AppLanguage.ARABIC -> "ترقية طلبات HTTP تلقائياً إلى اتصالات مشفرة"
    }

    val sslErrorPolicyTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "SSL 错误处理"
        AppLanguage.ENGLISH -> "SSL Error Handling"
        AppLanguage.ARABIC -> "معالجة أخطاء SSL"
    }

    val sslErrorPolicyAutoFallback: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动回退到 HTTP"
        AppLanguage.ENGLISH -> "Auto Fallback to HTTP"
        AppLanguage.ARABIC -> "العودة التلقائية إلى HTTP"
    }

    val sslErrorPolicyAutoFallbackDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "遇到 SSL 错误时自动尝试 HTTP 版本，确保大多数网站正常访问"
        AppLanguage.ENGLISH -> "Auto try HTTP version on SSL error, ensures most sites work"
        AppLanguage.ARABIC -> "المحاولة التلقائية لإصدار HTTP عند خطأ SSL تضمن عمل معظم المواقع"
    }

    val sslErrorPolicyAskUser: String get() = when (lang) {
        AppLanguage.CHINESE -> "询问用户"
        AppLanguage.ENGLISH -> "Ask User"
        AppLanguage.ARABIC -> "سؤال المستخدم"
    }

    val sslErrorPolicyAskUserDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示对话框让用户选择是否继续"
        AppLanguage.ENGLISH -> "Show dialog for user to choose whether to proceed"
        AppLanguage.ARABIC -> "إظهار حوار للمستخدم ليختار المتابعة"
    }

    val sslErrorPolicyBlock: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止连接"
        AppLanguage.ENGLISH -> "Block Connection"
        AppLanguage.ARABIC -> "حظر الاتصال"
    }

    val sslErrorPolicyBlockDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "直接拒绝不安全的连接（最安全）"
        AppLanguage.ENGLISH -> "Reject insecure connections (most secure)"
        AppLanguage.ARABIC -> "رفض الاتصالات غير الآمنة (الأكثر أماناً)"
    }

    val shieldsTrackerBlocking: String get() = when (lang) {
        AppLanguage.CHINESE -> "跟踪器拦截"
        AppLanguage.ENGLISH -> "Tracker Blocking"
        AppLanguage.ARABIC -> "حظر المتتبعات"
    }

    val shieldsTrackerBlockingDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截数据分析、社交追踪、指纹采集、加密挖矿"
        AppLanguage.ENGLISH -> "Block analytics, social tracking, fingerprinting, crypto mining"
        AppLanguage.ARABIC -> "حظر التحليلات والتتبع الاجتماعي وبصمات الأجهزة والتعدين"
    }

    val shieldsCookiePopup: String get() = when (lang) {
        AppLanguage.CHINESE -> "Cookie 弹窗自动关闭"
        AppLanguage.ENGLISH -> "Auto-dismiss Cookie Popups"
        AppLanguage.ARABIC -> "إغلاق نوافذ الكوكيز تلقائياً"
    }

    val shieldsCookiePopupDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动点击\u201c拒绝\u201d或隐藏 Cookie 同意弹窗"
        AppLanguage.ENGLISH -> "Auto-click \"reject\" or hide cookie consent popups"
        AppLanguage.ARABIC -> "النقر تلقائياً على \"رفض\" أو إخفاء نوافذ موافقة الكوكيز"
    }

    val shieldsGpcDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "发送 GPC/DNT 信号，告知网站不同意数据共享"
        AppLanguage.ENGLISH -> "Send GPC/DNT signals to tell websites not to share data"
        AppLanguage.ARABIC -> "إرسال إشارات GPC/DNT لإبلاغ المواقع بعدم مشاركة البيانات"
    }

    val shieldsReaderMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "阅读模式 (SpeedReader)"
        AppLanguage.ENGLISH -> "Reader Mode (SpeedReader)"
        AppLanguage.ARABIC -> "وضع القراءة (SpeedReader)"
    }

    val shieldsReaderModeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "提取正文、去广告，极简阅读体验"
        AppLanguage.ENGLISH -> "Extract content, remove ads, minimal reading experience"
        AppLanguage.ARABIC -> "استخراج المحتوى وإزالة الإعلانات، تجربة قراءة بسيطة"
    }

    val shieldsThirdPartyCookiePolicy: String get() = when (lang) {
        AppLanguage.CHINESE -> "第三方 Cookie 策略"
        AppLanguage.ENGLISH -> "Third-party Cookie Policy"
        AppLanguage.ARABIC -> "سياسة ملفات تعريف الارتباط للجهات الخارجية"
    }

    val shieldsReferrerPolicy: String get() = when (lang) {
        AppLanguage.CHINESE -> "Referrer 策略"
        AppLanguage.ENGLISH -> "Referrer Policy"
        AppLanguage.ARABIC -> "سياسة المُحيل"
    }

    val verifyingFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "验证中: %s"
        AppLanguage.ENGLISH -> "Verifying: %s"
        AppLanguage.ARABIC -> "جاري التحقق: %s"
    }
}
