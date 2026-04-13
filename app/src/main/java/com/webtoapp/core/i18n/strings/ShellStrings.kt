package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object ShellStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage

    val shortcutPermissionTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "需要快捷方式权限"
        AppLanguage.ENGLISH -> "Shortcut Permission Required"
        AppLanguage.ARABIC -> "مطلوب إذن الاختصار"
    }

    val shortcutPermissionGoToSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "去设置"
        AppLanguage.ENGLISH -> "Go to Settings"
        AppLanguage.ARABIC -> "الذهاب إلى الإعدادات"
    }

    val shortcutPermissionLater: String get() = when (lang) {
        AppLanguage.CHINESE -> "稍后再说"
        AppLanguage.ENGLISH -> "Later"
        AppLanguage.ARABIC -> "لاحقاً"
    }

    val shortcutPermissionXiaomi: String
        get() = "Detected Xiaomi/Redmi phone. You need to enable 'Desktop Shortcut' permission to create app shortcuts.\n\nGo to: Settings > App Settings > App Management > WebToApp > Permission Management > Desktop Shortcut"

    val shortcutPermissionHuawei: String
        get() = "Detected Huawei/Honor phone. You need to enable 'Create Desktop Shortcut' permission.\n\nGo to: Settings > Apps > App Management > WebToApp > Permissions > Create Desktop Shortcut"

    val shortcutPermissionOppo: String
        get() = "Detected OPPO phone. You need to enable 'Desktop Shortcut' permission.\n\nGo to: Settings > App Management > WebToApp > Permissions > Desktop Shortcut"

    val shortcutPermissionVivo: String
        get() = "Detected vivo phone. You need to enable 'Desktop Shortcut' permission.\n\nGo to: i Manager > App Management > Permission Management > WebToApp > Desktop Shortcut"

    val shortcutPermissionMeizu: String
        get() = "Detected Meizu phone. You need to enable 'Desktop Shortcut' permission.\n\nGo to: Phone Manager > Permission Management > WebToApp > Desktop Shortcut"

    val shortcutPermissionSamsung: String
        get() = "Detected Samsung phone. Please ensure the home screen is unlocked for editing.\n\nYou can also long-press the app icon and select 'Add to Home Screen' to create a shortcut."

    val shortcutPermissionGeneric: String
        get() = "The current launcher may not support creating shortcuts. Please check home screen settings or app permissions.\n\nClick 'Go to Settings' to open the app details page and check for relevant permission options."

    val browserDisguiseTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "🕶️ 浏览器伪装"
        AppLanguage.ENGLISH -> "🕶️ Browser Disguise"
        AppLanguage.ARABIC -> "🕶️ تمويه المتصفح"
    }

    val browserDisguiseEnable: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用浏览器伪装"
        AppLanguage.ENGLISH -> "Enable Browser Disguise"
        AppLanguage.ARABIC -> "تفعيل تمويه المتصفح"
    }

    val browserDisguiseEnableDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "多层次反指纹技术，使 WebView 无法被网站检测"
        AppLanguage.ENGLISH -> "Multi-layer anti-fingerprinting to make WebView undetectable"
        AppLanguage.ARABIC -> "تقنية متعددة الطبقات لمكافحة البصمات لجعل WebView غير قابل للكشف"
    }

    val browserDisguisePreset: String get() = when (lang) {
        AppLanguage.CHINESE -> "伪装预设"
        AppLanguage.ENGLISH -> "Disguise Preset"
        AppLanguage.ARABIC -> "إعداد مسبق للتمويه"
    }

    val browserDisguiseCoverage: String get() = when (lang) {
        AppLanguage.CHINESE -> "覆盖率"
        AppLanguage.ENGLISH -> "Coverage"
        AppLanguage.ARABIC -> "التغطية"
    }

    val browserDisguiseCoverageTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "指纹覆盖率"
        AppLanguage.ENGLISH -> "Fingerprint Coverage"
        AppLanguage.ARABIC -> "تغطية البصمة"
    }

    val browserDisguiseActiveVectors: String get() = when (lang) {
        AppLanguage.CHINESE -> "活跃向量"
        AppLanguage.ENGLISH -> "Active Vectors"
        AppLanguage.ARABIC -> "المتجهات النشطة"
    }

    val browserDisguiseAdvanced: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级向量控制"
        AppLanguage.ENGLISH -> "Advanced Vector Controls"
        AppLanguage.ARABIC -> "عناصر التحكم المتقدمة"
    }

    val browserDisguiseL2Title: String get() = when (lang) {
        AppLanguage.CHINESE -> "Level 2 · 指纹向量伪装"
        AppLanguage.ENGLISH -> "Level 2 · Fingerprint Spoofing"
        AppLanguage.ARABIC -> "المستوى 2 · تزييف البصمات"
    }

    val browserDisguiseCanvasNoise: String get() = when (lang) {
        AppLanguage.CHINESE -> "Canvas 指纹噪声"
        AppLanguage.ENGLISH -> "Canvas Fingerprint Noise"
        AppLanguage.ARABIC -> "ضوضاء بصمة Canvas"
    }

    val browserDisguiseCanvasNoiseDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "在 toDataURL/getImageData 中注入亚像素噪声"
        AppLanguage.ENGLISH -> "Inject sub-pixel noise into toDataURL/getImageData"
        AppLanguage.ARABIC -> "حقن ضوضاء دون البكسل في toDataURL/getImageData"
    }

    val browserDisguiseWebGL: String get() = when (lang) {
        AppLanguage.CHINESE -> "WebGL 渲染器伪装"
        AppLanguage.ENGLISH -> "WebGL Renderer Spoof"
        AppLanguage.ARABIC -> "تزييف عارض WebGL"
    }

    val browserDisguiseWebGLDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "替换 GPU 渲染器/供应商信息为标准 PC 值"
        AppLanguage.ENGLISH -> "Replace GPU renderer/vendor info with standard PC values"
        AppLanguage.ARABIC -> "استبدال معلومات GPU بقيم PC قياسية"
    }

    val browserDisguiseAudio: String get() = when (lang) {
        AppLanguage.CHINESE -> "AudioContext 噪声"
        AppLanguage.ENGLISH -> "AudioContext Noise"
        AppLanguage.ARABIC -> "ضوضاء AudioContext"
    }

    val browserDisguiseAudioDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "在音频分析数据中注入微小频率偏移"
        AppLanguage.ENGLISH -> "Inject micro-frequency offsets into audio analysis data"
        AppLanguage.ARABIC -> "حقن إزاحات تردد دقيقة في بيانات تحليل الصوت"
    }

    val browserDisguiseScreen: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏幕分辨率伪装"
        AppLanguage.ENGLISH -> "Screen Resolution Spoof"
        AppLanguage.ARABIC -> "تزييف دقة الشاشة"
    }

    val browserDisguiseScreenDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "伪装 screen.width/height 和 devicePixelRatio"
        AppLanguage.ENGLISH -> "Spoof screen.width/height and devicePixelRatio"
        AppLanguage.ARABIC -> "تزييف أبعاد الشاشة ونسبة البكسل"
    }

    val browserDisguiseClientRects: String get() = when (lang) {
        AppLanguage.CHINESE -> "ClientRects 微偏移"
        AppLanguage.ENGLISH -> "ClientRects Micro-offset"
        AppLanguage.ARABIC -> "إزاحة دقيقة لـ ClientRects"
    }

    val browserDisguiseClientRectsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "防止 DOMRect 枚举指纹识别"
        AppLanguage.ENGLISH -> "Prevent DOMRect enumeration fingerprinting"
        AppLanguage.ARABIC -> "منع بصمات تعداد DOMRect"
    }

    val browserDisguiseL3Title: String get() = when (lang) {
        AppLanguage.CHINESE -> "Level 3 · 环境伪装"
        AppLanguage.ENGLISH -> "Level 3 · Environment Spoofing"
        AppLanguage.ARABIC -> "المستوى 3 · تزييف البيئة"
    }

    val browserDisguiseTimezone: String get() = when (lang) {
        AppLanguage.CHINESE -> "时区伪装"
        AppLanguage.ENGLISH -> "Timezone Spoof"
        AppLanguage.ARABIC -> "تزييف المنطقة الزمنية"
    }

    val browserDisguiseTimezoneDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "覆写 Intl.DateTimeFormat 和 Date.getTimezoneOffset"
        AppLanguage.ENGLISH -> "Override Intl.DateTimeFormat and Date.getTimezoneOffset"
        AppLanguage.ARABIC -> "تجاوز DateTimeFormat و getTimezoneOffset"
    }

    val browserDisguiseLanguage: String get() = when (lang) {
        AppLanguage.CHINESE -> "语言伪装"
        AppLanguage.ENGLISH -> "Language Spoof"
        AppLanguage.ARABIC -> "تزييف اللغة"
    }

    val browserDisguiseLanguageDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "覆写 navigator.language 和 navigator.languages"
        AppLanguage.ENGLISH -> "Override navigator.language and navigator.languages"
        AppLanguage.ARABIC -> "تجاوز لغات المتصفح"
    }

    val browserDisguisePlatform: String get() = when (lang) {
        AppLanguage.CHINESE -> "平台伪装"
        AppLanguage.ENGLISH -> "Platform Spoof"
        AppLanguage.ARABIC -> "تزييف المنصة"
    }

    val browserDisguisePlatformDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "伪装 navigator.platform 为 Win32/MacIntel 等"
        AppLanguage.ENGLISH -> "Spoof navigator.platform to Win32/MacIntel etc."
        AppLanguage.ARABIC -> "تزييف منصة المتصفح إلى Win32 أو MacIntel"
    }

    val browserDisguiseHardware: String get() = when (lang) {
        AppLanguage.CHINESE -> "CPU 核心数伪装"
        AppLanguage.ENGLISH -> "CPU Cores Spoof"
        AppLanguage.ARABIC -> "تزييف عدد أنوية المعالج"
    }

    val browserDisguiseHardwareDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "覆写 navigator.hardwareConcurrency"
        AppLanguage.ENGLISH -> "Override navigator.hardwareConcurrency"
        AppLanguage.ARABIC -> "تجاوز عدد الأنوية المتاحة"
    }

    val browserDisguiseMemory: String get() = when (lang) {
        AppLanguage.CHINESE -> "设备内存伪装"
        AppLanguage.ENGLISH -> "Device Memory Spoof"
        AppLanguage.ARABIC -> "تزييف ذاكرة الجهاز"
    }

    val browserDisguiseMemoryDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "覆写 navigator.deviceMemory"
        AppLanguage.ENGLISH -> "Override navigator.deviceMemory"
        AppLanguage.ARABIC -> "تجاوز ذاكرة الجهاز المعلنة"
    }

    val browserDisguiseL4Title: String get() = when (lang) {
        AppLanguage.CHINESE -> "Level 4 · 深度伪装"
        AppLanguage.ENGLISH -> "Level 4 · Deep Disguise"
        AppLanguage.ARABIC -> "المستوى 4 · التمويه العميق"
    }

    val browserDisguiseMediaDevices: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体设备伪装"
        AppLanguage.ENGLISH -> "Media Devices Spoof"
        AppLanguage.ARABIC -> "تزييف أجهزة الوسائط"
    }

    val browserDisguiseMediaDevicesDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "匿名化 enumerateDevices 返回结果"
        AppLanguage.ENGLISH -> "Anonymize enumerateDevices results"
        AppLanguage.ARABIC -> "إخفاء هوية نتائج الأجهزة"
    }

    val browserDisguiseWebRTC: String get() = when (lang) {
        AppLanguage.CHINESE -> "WebRTC IP 屏蔽"
        AppLanguage.ENGLISH -> "WebRTC IP Shield"
        AppLanguage.ARABIC -> "حجب IP عبر WebRTC"
    }

    val browserDisguiseWebRTCDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止 RTCPeerConnection 泄露本地 IP 地址"
        AppLanguage.ENGLISH -> "Block RTCPeerConnection from leaking local IP addresses"
        AppLanguage.ARABIC -> "منع تسريب عناوين IP المحلية عبر WebRTC"
    }

    val browserDisguiseFonts: String get() = when (lang) {
        AppLanguage.CHINESE -> "字体枚举拦截"
        AppLanguage.ENGLISH -> "Font Enumeration Block"
        AppLanguage.ARABIC -> "حظر تعداد الخطوط"
    }

    val browserDisguiseFontsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "在 measureText 中注入噪声防止字体指纹"
        AppLanguage.ENGLISH -> "Inject noise into measureText to prevent font fingerprinting"
        AppLanguage.ARABIC -> "حقن ضوضاء في قياس النص لمنع bصمات الخطوط"
    }

    val browserDisguiseBattery: String get() = when (lang) {
        AppLanguage.CHINESE -> "Battery API 屏蔽"
        AppLanguage.ENGLISH -> "Battery API Shield"
        AppLanguage.ARABIC -> "حجب واجهة البطارية"
    }

    val browserDisguiseBatteryDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "返回固定电池信息 (100%, 充电中)"
        AppLanguage.ENGLISH -> "Return fixed battery info (100%, charging)"
        AppLanguage.ARABIC -> "إرجاع معلومات بطارية ثابتة"
    }

    val browserDisguiseL5Title: String get() = when (lang) {
        AppLanguage.CHINESE -> "Level 5 · 原型链保护"
        AppLanguage.ENGLISH -> "Level 5 · Prototype Protection"
        AppLanguage.ARABIC -> "المستوى 5 · حماية سلسلة النماذج"
    }

    val browserDisguisePrototype: String get() = when (lang) {
        AppLanguage.CHINESE -> "toString 保护"
        AppLanguage.ENGLISH -> "toString Protection"
        AppLanguage.ARABIC -> "حماية toString"
    }

    val browserDisguisePrototypeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "所有 hook 函数的 toString 返回 [native code]"
        AppLanguage.ENGLISH -> "All hooked functions return [native code] via toString"
        AppLanguage.ARABIC -> "جميع الوظائف المعدلة تعيد [native code]"
    }

    val browserDisguiseIframe: String get() = when (lang) {
        AppLanguage.CHINESE -> "iframe 穿透传播"
        AppLanguage.ENGLISH -> "iframe Propagation"
        AppLanguage.ARABIC -> "انتشار iframe"
    }

    val browserDisguiseIframeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动将伪装传播到新创建的 iframe 中"
        AppLanguage.ENGLISH -> "Auto-propagate disguise into newly created iframes"
        AppLanguage.ARABIC -> "نشر التمويه تلقائياً في إطارات iframe الجديدة"
    }

    val browserDisguiseTip: String get() = when (lang) {
        AppLanguage.CHINESE -> "浏览器伪装引擎通过 JavaScript 注入修改浏览器指纹 API 的返回值。Level 1 (基础反检测) 始终启用，覆盖 WebView 标识移除和 window.chrome 补全。更高级别逐步添加 Canvas/WebGL/Audio 噪声、环境伪装和原型链保护。注意：过高的伪装级别可能影响部分网站功能。"
        AppLanguage.ENGLISH -> "The Browser Disguise Engine modifies browser fingerprint API return values via JS injection. Level 1 (basic anti-detection) is always active, covering WebView marker removal and window.chrome emulation. Higher levels progressively add Canvas/WebGL/Audio noise, environment spoofing, and prototype chain protection. Note: excessive disguise levels may affect some website functionality."
        AppLanguage.ARABIC -> "يعدل محرك تمويه المتصفح قيم إرجاع واجهات برمجة البصمات عبر حقن JS. المستوى 1 نشط دائماً. المستويات الأعلى تضيف تدريجياً ضوضاء Canvas/WebGL والتزييف البيئي وحماية سلسلة النماذج."
    }

    val browserDisguiseConnectionTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "网络连接伪装"
        AppLanguage.ENGLISH -> "Connection Spoof"
        AppLanguage.ARABIC -> "تزييف الاتصال"
    }

    val browserDisguiseConnectionDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "伪装 navigator.connection API (effectiveType/downlink/rtt)"
        AppLanguage.ENGLISH -> "Spoof navigator.connection API (effectiveType/downlink/rtt)"
        AppLanguage.ARABIC -> "تزييف واجهة navigator.connection (effectiveType/downlink/rtt)"
    }

    val browserDisguisePermissionsTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "权限 API 伪装"
        AppLanguage.ENGLISH -> "Permissions Spoof"
        AppLanguage.ARABIC -> "تزييف الأذونات"
    }

    val browserDisguisePermissionsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截 navigator.permissions.query 返回 'prompt' 状态"
        AppLanguage.ENGLISH -> "Intercept permissions.query to return 'prompt' state"
        AppLanguage.ARABIC -> "اعتراض استعلام الأذونات لإرجاع حالة 'prompt'"
    }

    val browserDisguisePerformanceTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "性能计时降噪"
        AppLanguage.ENGLISH -> "Performance Timing Noise"
        AppLanguage.ARABIC -> "ضوضاء توقيت الأداء"
    }

    val browserDisguisePerformanceDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "降低 performance.now() 精度，防止时序指纹攻击"
        AppLanguage.ENGLISH -> "Reduce performance.now() precision to prevent timing attacks"
        AppLanguage.ARABIC -> "تقليل دقة performance.now() لمنع هجمات التوقيت"
    }

    val browserDisguiseStorageTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "存储估算伪装"
        AppLanguage.ENGLISH -> "Storage Estimate Spoof"
        AppLanguage.ARABIC -> "تزييف تقدير التخزين"
    }

    val browserDisguiseStorageDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "标准化 navigator.storage.estimate() 返回值"
        AppLanguage.ENGLISH -> "Normalize navigator.storage.estimate() return values"
        AppLanguage.ARABIC -> "توحيد قيم إرجاع navigator.storage.estimate()"
    }

    val browserDisguiseNotificationTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "通知 API 补全"
        AppLanguage.ENGLISH -> "Notification API Compat"
        AppLanguage.ARABIC -> "توافق واجهة الإشعارات"
    }

    val browserDisguiseNotificationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "补全 WebView 缺失的 Notification API 并伪装权限状态"
        AppLanguage.ENGLISH -> "Polyfill missing Notification API and spoof permission state"
        AppLanguage.ARABIC -> "إكمال واجهة الإشعارات المفقودة وتزييف حالة الإذن"
    }

    val browserDisguiseCssMediaTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS 媒体查询伪装"
        AppLanguage.ENGLISH -> "CSS Media Query Spoof"
        AppLanguage.ARABIC -> "تزييف استعلام وسائط CSS"
    }

    val browserDisguiseCssMediaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截 matchMedia 伪装 prefers-color-scheme / prefers-reduced-motion"
        AppLanguage.ENGLISH -> "Intercept matchMedia to spoof prefers-color-scheme / prefers-reduced-motion"
        AppLanguage.ARABIC -> "اعتراض matchMedia لتزييف prefers-color-scheme / prefers-reduced-motion"
    }

    val browserDisguiseDiagTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "🔬 指纹诊断工具"
        AppLanguage.ENGLISH -> "🔬 Fingerprint Diagnostic"
        AppLanguage.ARABIC -> "🔬 تشخيص البصمة"
    }

    val browserDisguiseDiagDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "打开内置诊断页面，展示当前浏览器指纹的所有维度"
        AppLanguage.ENGLISH -> "Open built-in diagnostic page showing all fingerprint vectors"
        AppLanguage.ARABIC -> "فتح صفحة التشخيص المدمجة لعرض جميع أبعاد البصمة"
    }

    val browserDisguiseRunDiag: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行诊断"
        AppLanguage.ENGLISH -> "Run Diagnostic"
        AppLanguage.ARABIC -> "تشغيل التشخيص"
    }

    val browserDisguiseEngineStatus: String get() = when (lang) {
        AppLanguage.CHINESE -> "引擎状态"
        AppLanguage.ENGLISH -> "Engine Status"
        AppLanguage.ARABIC -> "حالة المحرك"
    }

    val browserDisguiseVectors: String get() = when (lang) {
        AppLanguage.CHINESE -> "向量"
        AppLanguage.ENGLISH -> "vectors"
        AppLanguage.ARABIC -> "متجهات"
    }

    val errorPageTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义无网络页面"
        AppLanguage.ENGLISH -> "Custom Offline Page"
        AppLanguage.ARABIC -> "صفحة مخصصة بدون إنترنت"
    }

    val errorPageSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "当网络不可用时展示的页面"
        AppLanguage.ENGLISH -> "Page shown when network is unavailable"
        AppLanguage.ARABIC -> "الصفحة المعروضة عند عدم توفر الشبكة"
    }

    val errorPageModeDefault: String get() = when (lang) {
        AppLanguage.CHINESE -> "系统默认"
        AppLanguage.ENGLISH -> "System Default"
        AppLanguage.ARABIC -> "الافتراضي"
    }

    val errorPageModeDefaultDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用浏览器默认的错误提示页面"
        AppLanguage.ENGLISH -> "Use browser's default error page"
        AppLanguage.ARABIC -> "استخدام صفحة الخطأ الافتراضية للمتصفح"
    }

    val errorPageModeBuiltIn: String get() = when (lang) {
        AppLanguage.CHINESE -> "内置精美风格"
        AppLanguage.ENGLISH -> "Built-in Styles"
        AppLanguage.ARABIC -> "أنماط مدمجة"
    }

    val errorPageModeBuiltInDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择一种精心设计的内置错误页风格"
        AppLanguage.ENGLISH -> "Choose a beautifully designed built-in style"
        AppLanguage.ARABIC -> "اختر نمطًا مدمجًا مصممًا بعناية"
    }

    val errorPageModeCustomHtml: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义 HTML"
        AppLanguage.ENGLISH -> "Custom HTML"
        AppLanguage.ARABIC -> "HTML مخصص"
    }

    val errorPageModeCustomHtmlDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入自定义的 HTML 页面代码"
        AppLanguage.ENGLISH -> "Enter your custom HTML page code"
        AppLanguage.ARABIC -> "أدخل كود صفحة HTML المخصصة"
    }

    val errorPageModeCustomMedia: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义媒体"
        AppLanguage.ENGLISH -> "Custom Media"
        AppLanguage.ARABIC -> "وسائط مخصصة"
    }

    val errorPageModeCustomMediaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示自定义图片或视频"
        AppLanguage.ENGLISH -> "Display a custom image or video"
        AppLanguage.ARABIC -> "عرض صورة أو فيديو مخصص"
    }

    val errorPageStyleLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择风格"
        AppLanguage.ENGLISH -> "Choose Style"
        AppLanguage.ARABIC -> "اختر النمط"
    }

    val errorPageStyleMaterial: String get() = when (lang) {
        AppLanguage.CHINESE -> "Material Design"
        AppLanguage.ENGLISH -> "Material Design"
        AppLanguage.ARABIC -> "تصميم ماتيريال"
    }

    val errorPageStyleSatellite: String get() = when (lang) {
        AppLanguage.CHINESE -> "深空卫星"
        AppLanguage.ENGLISH -> "Deep Space"
        AppLanguage.ARABIC -> "فضاء عميق"
    }

    val errorPageStyleOcean: String get() = when (lang) {
        AppLanguage.CHINESE -> "深海世界"
        AppLanguage.ENGLISH -> "Deep Ocean"
        AppLanguage.ARABIC -> "محيط عميق"
    }

    val errorPageStyleForest: String get() = when (lang) {
        AppLanguage.CHINESE -> "萤火森林"
        AppLanguage.ENGLISH -> "Firefly Forest"
        AppLanguage.ARABIC -> "غابة اليراعات"
    }

    val errorPageStyleMinimal: String get() = when (lang) {
        AppLanguage.CHINESE -> "极简线条"
        AppLanguage.ENGLISH -> "Minimalist"
        AppLanguage.ARABIC -> "بسيط"
    }

    val errorPageStyleNeon: String get() = when (lang) {
        AppLanguage.CHINESE -> "赛博霓虹"
        AppLanguage.ENGLISH -> "Cyber Neon"
        AppLanguage.ARABIC -> "نيون سيبراني"
    }

    val errorPageMiniGameLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "内嵌小游戏"
        AppLanguage.ENGLISH -> "Mini Game"
        AppLanguage.ARABIC -> "لعبة صغيرة"
    }

    val errorPageMiniGameDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "在等待网络恢复时可以玩小游戏"
        AppLanguage.ENGLISH -> "Play a game while waiting for network"
        AppLanguage.ARABIC -> "العب لعبة أثناء انتظار الشبكة"
    }

    val errorPageGameRandom: String get() = when (lang) {
        AppLanguage.CHINESE -> "随机"
        AppLanguage.ENGLISH -> "Random"
        AppLanguage.ARABIC -> "عشوائي"
    }

    val errorPageGameBreakout: String get() = when (lang) {
        AppLanguage.CHINESE -> "弹球消消"
        AppLanguage.ENGLISH -> "Breakout"
        AppLanguage.ARABIC -> "كسر الطوب"
    }

    val errorPageGameMaze: String get() = when (lang) {
        AppLanguage.CHINESE -> "迷宫行者"
        AppLanguage.ENGLISH -> "Maze Runner"
        AppLanguage.ARABIC -> "عداء المتاهة"
    }

    val errorPageGameInkZen: String get() = when (lang) {
        AppLanguage.CHINESE -> "水墨禅境"
        AppLanguage.ENGLISH -> "Ink Zen"
        AppLanguage.ARABIC -> "حبر زن"
    }

    val errorPageGameStarCatch: String get() = when (lang) {
        AppLanguage.CHINESE -> "星空收集"
        AppLanguage.ENGLISH -> "Star Catch"
        AppLanguage.ARABIC -> "جمع النجوم"
    }

    val errorPageAutoRetryLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动重试"
        AppLanguage.ENGLISH -> "Auto Retry"
        AppLanguage.ARABIC -> "إعادة المحاولة تلقائيًا"
    }

    val errorPageAutoRetryDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动每隔 %d 秒重试连接"
        AppLanguage.ENGLISH -> "Auto retry every %d seconds"
        AppLanguage.ARABIC -> "إعادة المحاولة تلقائيًا كل %d ثانية"
    }

    val errorPageAutoRetryOff: String get() = when (lang) {
        AppLanguage.CHINESE -> "关闭自动重试"
        AppLanguage.ENGLISH -> "Auto retry off"
        AppLanguage.ARABIC -> "إعادة المحاولة التلقائية معطلة"
    }

    val errorPageRetryIntervalLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "重试间隔（秒）"
        AppLanguage.ENGLISH -> "Retry interval (seconds)"
        AppLanguage.ARABIC -> "فترة إعادة المحاولة (ثوانٍ)"
    }

    val errorPageCustomHtmlHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入完整的 HTML 代码..."
        AppLanguage.ENGLISH -> "Enter complete HTML code..."
        AppLanguage.ARABIC -> "أدخل كود HTML الكامل..."
    }

    val errorPageCustomMediaHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入图片或视频的路径..."
        AppLanguage.ENGLISH -> "Enter image or video path..."
        AppLanguage.ARABIC -> "أدخل مسار الصورة أو الفيديو..."
    }

    val errorPageLanguageLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "错误页语言"
        AppLanguage.ENGLISH -> "Error Page Language"
        AppLanguage.ARABIC -> "لغة صفحة الخطأ"
    }

    val errorPageLangChinese: String get() = when (lang) {
        AppLanguage.CHINESE -> "中文"
        AppLanguage.ENGLISH -> "Chinese"
        AppLanguage.ARABIC -> "الصينية"
    }

    val errorPageLangEnglish: String get() = when (lang) {
        AppLanguage.CHINESE -> "英文"
        AppLanguage.ENGLISH -> "English"
        AppLanguage.ARABIC -> "الإنجليزية"
    }

    val errorPageLangArabic: String get() = when (lang) {
        AppLanguage.CHINESE -> "阿拉伯文"
        AppLanguage.ENGLISH -> "Arabic"
        AppLanguage.ARABIC -> "العربية"
    }

    val unsavedChangesTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "未保存的更改"
        AppLanguage.ENGLISH -> "Unsaved Changes"
        AppLanguage.ARABIC -> "تغييرات غير محفوظة"
    }

    val unsavedChangesMessage: String get() = when (lang) {
        AppLanguage.CHINESE -> "你有未保存的更改，确定要离开吗？"
        AppLanguage.ENGLISH -> "You have unsaved changes. Are you sure you want to leave?"
        AppLanguage.ARABIC -> "لديك تغييرات غير محفوظة. هل أنت متأكد من المغادرة؟"
    }

    val deviceDisguiseTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "设备伪装"
        AppLanguage.ENGLISH -> "Device Disguise"
        AppLanguage.ARABIC -> "تمويه الجهاز"
    }

    val deviceDisguiseSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "伪装为不同设备访问网页"
        AppLanguage.ENGLISH -> "Disguise as different devices to browse"
        AppLanguage.ARABIC -> "التنكر كأجهزة مختلفة للتصفح"
    }

    val deviceDisguiseHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择一个设备，自动生成对应的浏览器指纹和 User-Agent，让网站认为你正在使用该设备访问。"
        AppLanguage.ENGLISH -> "Select a device to auto-generate matching browser fingerprint and User-Agent, making websites believe you are browsing with that device."
        AppLanguage.ARABIC -> "اختر جهازًا لتوليد بصمة المتصفح و User-Agent المطابقة تلقائيًا، مما يجعل المواقع تعتقد أنك تتصفح من هذا الجهاز."
    }

    val deviceDisguiseOff: String get() = when (lang) {
        AppLanguage.CHINESE -> "关闭 · 使用真实设备"
        AppLanguage.ENGLISH -> "Off · Using real device"
        AppLanguage.ARABIC -> "مغلق · يستخدم الجهاز الحقيقي"
    }

    val deviceDisguiseActive: String get() = when (lang) {
        AppLanguage.CHINESE -> "已伪装为"
        AppLanguage.ENGLISH -> "Disguised as"
        AppLanguage.ARABIC -> "متنكر كـ"
    }
}
