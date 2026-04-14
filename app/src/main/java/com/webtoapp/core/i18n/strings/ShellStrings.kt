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
    // ==================== Port Management ====================
    val portManagerTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "端口管理"
        AppLanguage.ENGLISH -> "Port Manager"
        AppLanguage.ARABIC -> "مدير المنافذ"
    }

    val portManagerRunningServices: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行中的服务"
        AppLanguage.ENGLISH -> "Running Services"
        AppLanguage.ARABIC -> "الخدمات قيد التشغيل"
    }

    val portManagerNoServices: String get() = when (lang) {
        AppLanguage.CHINESE -> "没有运行中的服务"
        AppLanguage.ENGLISH -> "No running services"
        AppLanguage.ARABIC -> "لا توجد خدمات قيد التشغيل"
    }

    val portManagerAllReleased: String get() = when (lang) {
        AppLanguage.CHINESE -> "所有端口均已释放"
        AppLanguage.ENGLISH -> "All ports released"
        AppLanguage.ARABIC -> "تم تحرير جميع المنافذ"
    }

    val portManagerKillAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "终止所有"
        AppLanguage.ENGLISH -> "Kill All"
        AppLanguage.ARABIC -> "إنهاء الكل"
    }

    val portManagerKillAllConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定要终止所有运行中的服务吗？"
        AppLanguage.ENGLISH -> "Kill all running services?"
        AppLanguage.ARABIC -> "هل تريد إنهاء جميع الخدمات قيد التشغيل؟"
    }

    val portManagerKillService: String get() = when (lang) {
        AppLanguage.CHINESE -> "终止服务"
        AppLanguage.ENGLISH -> "Kill Service"
        AppLanguage.ARABIC -> "إنهاء الخدمة"
    }

    val portManagerOpen: String get() = when (lang) {
        AppLanguage.CHINESE -> "打开"
        AppLanguage.ENGLISH -> "Open"
        AppLanguage.ARABIC -> "فتح"
    }

    val portManagerKill: String get() = when (lang) {
        AppLanguage.CHINESE -> "终止"
        AppLanguage.ENGLISH -> "Kill"
        AppLanguage.ARABIC -> "إنهاء"
    }

    val portManagerPort: String get() = when (lang) {
        AppLanguage.CHINESE -> "端口"
        AppLanguage.ENGLISH -> "Port"
        AppLanguage.ARABIC -> "المنفذ"
    }

    val portManagerType: String get() = when (lang) {
        AppLanguage.CHINESE -> "类型"
        AppLanguage.ENGLISH -> "Type"
        AppLanguage.ARABIC -> "النوع"
    }

    val portManagerProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目"
        AppLanguage.ENGLISH -> "Project"
        AppLanguage.ARABIC -> "المشروع"
    }

    val portManagerStatus: String get() = when (lang) {
        AppLanguage.CHINESE -> "状态"
        AppLanguage.ENGLISH -> "Status"
        AppLanguage.ARABIC -> "الحالة"
    }

    val portManagerResponding: String get() = when (lang) {
        AppLanguage.CHINESE -> "响应中"
        AppLanguage.ENGLISH -> "Responding"
        AppLanguage.ARABIC -> "يستجيب"
    }

    val portManagerNotResponding: String get() = when (lang) {
        AppLanguage.CHINESE -> "无响应"
        AppLanguage.ENGLISH -> "Not Responding"
        AppLanguage.ARABIC -> "لا يستجيب"
    }

    val portManagerUnknown: String get() = when (lang) {
        AppLanguage.CHINESE -> "未知"
        AppLanguage.ENGLISH -> "Unknown"
        AppLanguage.ARABIC -> "غير معروف"
    }

    val portManagerOrphanProcess: String get() = when (lang) {
        AppLanguage.CHINESE -> "未知 (残留进程)"
        AppLanguage.ENGLISH -> "Unknown (orphan process)"
        AppLanguage.ARABIC -> "غير معروف (عملية يتيمة)"
    }

    val portManagerServiceKilled: String get() = when (lang) {
        AppLanguage.CHINESE -> "已终止端口 %d 的服务"
        AppLanguage.ENGLISH -> "Killed service on port %d"
        AppLanguage.ARABIC -> "تم إنهاء الخدمة على المنفذ %d"
    }

    val portManagerAllKilled: String get() = when (lang) {
        AppLanguage.CHINESE -> "已终止 %d 个服务"
        AppLanguage.ENGLISH -> "Killed %d services"
        AppLanguage.ARABIC -> "تم إنهاء %d خدمات"
    }

    val portManagerKillFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "终止失败，请重试"
        AppLanguage.ENGLISH -> "Kill failed, please retry"
        AppLanguage.ARABIC -> "فشل الإنهاء، يرجى المحاولة مرة أخرى"
    }

    val portManagerTypeLocalHttp: String get() = when (lang) {
        AppLanguage.CHINESE -> "静态服务"
        AppLanguage.ENGLISH -> "Static Server"
        AppLanguage.ARABIC -> "خادم ثابت"
    }

    val portManagerTypeNodeJs: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js"
        AppLanguage.ENGLISH -> "Node.js"
        AppLanguage.ARABIC -> "Node.js"
    }

    val portManagerTypePhp: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP"
        AppLanguage.ENGLISH -> "PHP"
        AppLanguage.ARABIC -> "PHP"
    }

    val portManagerTypePython: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python"
        AppLanguage.ENGLISH -> "Python"
        AppLanguage.ARABIC -> "Python"
    }

    val portManagerTypeGo: String get() = when (lang) {
        AppLanguage.CHINESE -> "Go"
        AppLanguage.ENGLISH -> "Go"
        AppLanguage.ARABIC -> "Go"
    }

    val portManagerUptime: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行时长"
        AppLanguage.ENGLISH -> "Uptime"
        AppLanguage.ARABIC -> "وقت التشغيل"
    }

    val portManagerLatency: String get() = when (lang) {
        AppLanguage.CHINESE -> "延迟"
        AppLanguage.ENGLISH -> "Latency"
        AppLanguage.ARABIC -> "زمن الاستجابة"
    }

    val portManagerPortRanges: String get() = when (lang) {
        AppLanguage.CHINESE -> "端口范围"
        AppLanguage.ENGLISH -> "Port Ranges"
        AppLanguage.ARABIC -> "نطاقات المنافذ"
    }

    val portManagerAutoRefresh: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动刷新"
        AppLanguage.ENGLISH -> "Auto Refresh"
        AppLanguage.ARABIC -> "تحديث تلقائي"
    }

    val portManagerKillConfirmSingle: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定要终止此服务吗？"
        AppLanguage.ENGLISH -> "Kill this service?"
        AppLanguage.ARABIC -> "هل تريد إنهاء هذه الخدمة؟"
    }

    val portManagerProcess: String get() = when (lang) {
        AppLanguage.CHINESE -> "进程"
        AppLanguage.ENGLISH -> "Process"
        AppLanguage.ARABIC -> "العملية"
    }

    val portManagerStaleCleanedUp: String get() = when (lang) {
        AppLanguage.CHINESE -> "已清理 %d 个僵尸端口"
        AppLanguage.ENGLISH -> "Cleaned up %d stale ports"
        AppLanguage.ARABIC -> "تم تنظيف %d منافذ قديمة"
    }

    val runtimeDepsTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行时 & 依赖管理"
        AppLanguage.ENGLISH -> "Runtime & Dependencies"
        AppLanguage.ARABIC -> "وقت التشغيل والتبعيات"
    }

    val runtimeDepsSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "管理运行时环境、项目文件与缓存"
        AppLanguage.ENGLISH -> "Manage runtimes, project files & cache"
        AppLanguage.ARABIC -> "إدارة بيئات التشغيل وملفات المشاريع وذاكرة التخزين المؤقت"
    }

    val depSectionRuntimes: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行时环境"
        AppLanguage.ENGLISH -> "Runtimes"
        AppLanguage.ARABIC -> "بيئات التشغيل"
    }

    val depSectionRuntimePlugins: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行时插件"
        AppLanguage.ENGLISH -> "Runtime Plugins"
        AppLanguage.ARABIC -> "إضافات التشغيل"
    }

    val depSectionProjects: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目文件"
        AppLanguage.ENGLISH -> "Project Files"
        AppLanguage.ARABIC -> "ملفات المشاريع"
    }

    val depSectionDownload: String get() = when (lang) {
        AppLanguage.CHINESE -> "镜像源 & 下载"
        AppLanguage.ENGLISH -> "Mirror & Download"
        AppLanguage.ARABIC -> "المرآة والتنزيل"
    }

    val depSectionStorage: String get() = when (lang) {
        AppLanguage.CHINESE -> "存储空间"
        AppLanguage.ENGLISH -> "Storage"
        AppLanguage.ARABIC -> "التخزين"
    }

    val depStatusReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "已就绪"
        AppLanguage.ENGLISH -> "Ready"
        AppLanguage.ARABIC -> "جاهز"
    }

    val depStatusNotInstalled: String get() = when (lang) {
        AppLanguage.CHINESE -> "未安装"
        AppLanguage.ENGLISH -> "Not Installed"
        AppLanguage.ARABIC -> "غير مثبت"
    }

    val depStatusDownloading: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载中..."
        AppLanguage.ENGLISH -> "Downloading..."
        AppLanguage.ARABIC -> "جارٍ التنزيل..."
    }

    val depPhpRuntime: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP 运行时"
        AppLanguage.ENGLISH -> "PHP Runtime"
        AppLanguage.ARABIC -> "وقت تشغيل PHP"
    }

    val depPhpDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP ${com.webtoapp.core.wordpress.WordPressDependencyManager.PHP_VERSION} · WordPress / PHP 应用"
        AppLanguage.ENGLISH -> "PHP ${com.webtoapp.core.wordpress.WordPressDependencyManager.PHP_VERSION} · WordPress / PHP Apps"
        AppLanguage.ARABIC -> "PHP ${com.webtoapp.core.wordpress.WordPressDependencyManager.PHP_VERSION} · تطبيقات WordPress / PHP"
    }

    val depWpCore: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress 核心"
        AppLanguage.ENGLISH -> "WordPress Core"
        AppLanguage.ARABIC -> "نواة WordPress"
    }

    val depWpCoreDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress ${com.webtoapp.core.wordpress.WordPressDependencyManager.WORDPRESS_VERSION} 核心文件"
        AppLanguage.ENGLISH -> "WordPress ${com.webtoapp.core.wordpress.WordPressDependencyManager.WORDPRESS_VERSION} core files"
        AppLanguage.ARABIC -> "ملفات نواة WordPress ${com.webtoapp.core.wordpress.WordPressDependencyManager.WORDPRESS_VERSION}"
    }

    val depSqlitePlugin: String get() = when (lang) {
        AppLanguage.CHINESE -> "SQLite 数据库插件"
        AppLanguage.ENGLISH -> "SQLite Database Plugin"
        AppLanguage.ARABIC -> "إضافة قاعدة بيانات SQLite"
    }

    val depSqliteDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress SQLite 集成 v${com.webtoapp.core.wordpress.WordPressDependencyManager.SQLITE_PLUGIN_VERSION}"
        AppLanguage.ENGLISH -> "WordPress SQLite Integration v${com.webtoapp.core.wordpress.WordPressDependencyManager.SQLITE_PLUGIN_VERSION}"
        AppLanguage.ARABIC -> "تكامل WordPress SQLite v${com.webtoapp.core.wordpress.WordPressDependencyManager.SQLITE_PLUGIN_VERSION}"
    }

    val depNodeRuntime: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js 运行时"
        AppLanguage.ENGLISH -> "Node.js Runtime"
        AppLanguage.ARABIC -> "وقت تشغيل Node.js"
    }

    val depNodeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js ${com.webtoapp.core.nodejs.NodeDependencyManager.NODE_VERSION} · 全栈 / API 后端"
        AppLanguage.ENGLISH -> "Node.js ${com.webtoapp.core.nodejs.NodeDependencyManager.NODE_VERSION} · Fullstack / API Backend"
        AppLanguage.ARABIC -> "Node.js ${com.webtoapp.core.nodejs.NodeDependencyManager.NODE_VERSION} · كامل / خلفية API"
    }

    val depPythonRuntime: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python 运行时"
        AppLanguage.ENGLISH -> "Python Runtime"
        AppLanguage.ARABIC -> "وقت تشغيل Python"
    }

    val depPythonDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python ${com.webtoapp.core.python.PythonDependencyManager.PYTHON_FULL_VERSION} · 脚本 / Web 后端"
        AppLanguage.ENGLISH -> "Python ${com.webtoapp.core.python.PythonDependencyManager.PYTHON_FULL_VERSION} · Scripting / Web Backend"
        AppLanguage.ARABIC -> "Python ${com.webtoapp.core.python.PythonDependencyManager.PYTHON_FULL_VERSION} · برمجة / خلفية الويب"
    }

    val depGoInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "Go 应用"
        AppLanguage.ENGLISH -> "Go Apps"
        AppLanguage.ARABIC -> "تطبيقات Go"
    }

    val depGoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "预编译二进制 · 无需运行时"
        AppLanguage.ENGLISH -> "Pre-compiled binaries · No runtime needed"
        AppLanguage.ARABIC -> "ثنائيات مترجمة مسبقًا · لا حاجة لوقت التشغيل"
    }

    val depProjectFiles: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目文件"
        AppLanguage.ENGLISH -> "Project files"
        AppLanguage.ARABIC -> "ملفات المشروع"
    }

    val depWpProjects: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress 项目"
        AppLanguage.ENGLISH -> "WordPress Projects"
        AppLanguage.ARABIC -> "مشاريع WordPress"
    }

    val depNodeProjects: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js 项目"
        AppLanguage.ENGLISH -> "Node.js Projects"
        AppLanguage.ARABIC -> "مشاريع Node.js"
    }

    val depPythonProjects: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python 项目"
        AppLanguage.ENGLISH -> "Python Projects"
        AppLanguage.ARABIC -> "مشاريع Python"
    }

    val depGoProjects: String get() = when (lang) {
        AppLanguage.CHINESE -> "Go 项目"
        AppLanguage.ENGLISH -> "Go Projects"
        AppLanguage.ARABIC -> "مشاريع Go"
    }

    val depDocsProjects: String get() = when (lang) {
        AppLanguage.CHINESE -> "文档站点"
        AppLanguage.ENGLISH -> "Docs Sites"
        AppLanguage.ARABIC -> "مواقع التوثيق"
    }

    val depMirrorSource: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载镜像源"
        AppLanguage.ENGLISH -> "Download Mirror"
        AppLanguage.ARABIC -> "مرآة التنزيل"
    }

    val depMirrorDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "影响 PHP、WordPress 和 Node.js 的下载速度"
        AppLanguage.ENGLISH -> "Affects download speed for PHP, WordPress & Node.js"
        AppLanguage.ARABIC -> "يؤثر على سرعة تنزيل PHP و WordPress و Node.js"
    }

    val depMirrorCN: String get() = when (lang) {
        AppLanguage.CHINESE -> "国内镜像"
        AppLanguage.ENGLISH -> "China Mirror"
        AppLanguage.ARABIC -> "مرآة الصين"
    }

    val depMirrorGlobal: String get() = when (lang) {
        AppLanguage.CHINESE -> "国际源"
        AppLanguage.ENGLISH -> "Global Source"
        AppLanguage.ARABIC -> "مصدر عالمي"
    }

    val depMirrorAuto: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动"
        AppLanguage.ENGLISH -> "Auto"
        AppLanguage.ARABIC -> "تلقائي"
    }

    val depDownloadAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载全部运行时"
        AppLanguage.ENGLISH -> "Download All Runtimes"
        AppLanguage.ARABIC -> "تنزيل جميع بيئات التشغيل"
    }

    val depDownloadAllDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "一键下载所有缺失的运行时组件（约 50MB）"
        AppLanguage.ENGLISH -> "Download all missing runtime components (~50MB)"
        AppLanguage.ARABIC -> "تنزيل جميع مكونات وقت التشغيل المفقودة (~50MB)"
    }

    val depTotalStorage: String get() = when (lang) {
        AppLanguage.CHINESE -> "总占用空间"
        AppLanguage.ENGLISH -> "Total Storage"
        AppLanguage.ARABIC -> "إجمالي التخزين"
    }

    val depClearAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "清理全部缓存"
        AppLanguage.ENGLISH -> "Clear All Cache"
        AppLanguage.ARABIC -> "مسح كل ذاكرة التخزين المؤقت"
    }

    val depClearConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "将删除所有已下载的运行时和缓存文件，下次使用时需要重新下载。确定继续？"
        AppLanguage.ENGLISH -> "This will delete all downloaded runtimes and cache. They will need to be re-downloaded. Continue?"
        AppLanguage.ARABIC -> "سيؤدي هذا إلى حذف جميع بيئات التشغيل وذاكرة التخزين المؤقت المحملة. ستحتاج إلى إعادة التنزيل. هل تريد المتابعة؟"
    }

    val depClearWpCache: String get() = when (lang) {
        AppLanguage.CHINESE -> "清理 WordPress 缓存"
        AppLanguage.ENGLISH -> "Clear WordPress Cache"
        AppLanguage.ARABIC -> "مسح ذاكرة التخزين المؤقت WordPress"
    }

    val depClearNodeCache: String get() = when (lang) {
        AppLanguage.CHINESE -> "清理 Node.js 缓存"
        AppLanguage.ENGLISH -> "Clear Node.js Cache"
        AppLanguage.ARABIC -> "مسح ذاكرة التخزين المؤقت Node.js"
    }

    val depClearPythonCache: String get() = when (lang) {
        AppLanguage.CHINESE -> "清理 Python 缓存"
        AppLanguage.ENGLISH -> "Clear Python Cache"
        AppLanguage.ARABIC -> "مسح ذاكرة التخزين المؤقت Python"
    }

    val depClearGoCache: String get() = when (lang) {
        AppLanguage.CHINESE -> "清理 Go 缓存"
        AppLanguage.ENGLISH -> "Clear Go Cache"
        AppLanguage.ARABIC -> "مسح ذاكرة التخزين المؤقت Go"
    }

    val depClearPhpCache: String get() = when (lang) {
        AppLanguage.CHINESE -> "清理 PHP 缓存"
        AppLanguage.ENGLISH -> "Clear PHP Cache"
        AppLanguage.ARABIC -> "مسح ذاكرة التخزين المؤقت PHP"
    }

    val depClearSqliteCache: String get() = when (lang) {
        AppLanguage.CHINESE -> "清理 SQLite 缓存"
        AppLanguage.ENGLISH -> "Clear SQLite Cache"
        AppLanguage.ARABIC -> "مسح ذاكرة التخزين المؤقت SQLite"
    }

    val depAllReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "所有运行时已就绪"
        AppLanguage.ENGLISH -> "All runtimes ready"
        AppLanguage.ARABIC -> "جميع بيئات التشغيل جاهزة"
    }

    val depSomeNotReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "部分运行时未安装"
        AppLanguage.ENGLISH -> "Some runtimes not installed"
        AppLanguage.ARABIC -> "بعض بيئات التشغيل غير مثبتة"
    }

    val depInstall: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装"
        AppLanguage.ENGLISH -> "Install"
        AppLanguage.ARABIC -> "تثبيت"
    }
    // ==================== Isolation Options ====================
    val fingerprintProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "指纹防护"
        AppLanguage.ENGLISH -> "Fingerprint Protection"
        AppLanguage.ARABIC -> "حماية البصمة"
    }

    val networkProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "网络防护"
        AppLanguage.ENGLISH -> "Network Protection"
        AppLanguage.ARABIC -> "حماية الشبكة"
    }

    val advancedOptions: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级选项"
        AppLanguage.ENGLISH -> "Advanced Options"
        AppLanguage.ARABIC -> "خيارات متقدمة"
    }

    val expand: String get() = when (lang) {
        AppLanguage.CHINESE -> "展开"
        AppLanguage.ENGLISH -> "Expand"
        AppLanguage.ARABIC -> "توسيع"
    }

    val collapse: String get() = when (lang) {
        AppLanguage.CHINESE -> "收起"
        AppLanguage.ENGLISH -> "Collapse"
        AppLanguage.ARABIC -> "طي"
    }

    val expandAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "展开全部"
        AppLanguage.ENGLISH -> "Expand All"
        AppLanguage.ARABIC -> "توسيع الكل"
    }

    val depClearDone: String get() = when (lang) {
        AppLanguage.CHINESE -> "已清除"
        AppLanguage.ENGLISH -> "Cleared"
        AppLanguage.ARABIC -> "تم المسح"
    }

    val custom: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义"
        AppLanguage.ENGLISH -> "Custom"
        AppLanguage.ARABIC -> "مخصص"
    }

    val maximum: String get() = when (lang) {
        AppLanguage.CHINESE -> "最高"
        AppLanguage.ENGLISH -> "Maximum"
        AppLanguage.ARABIC -> "الأقصى"
    }

    val full: String get() = when (lang) {
        AppLanguage.CHINESE -> "完全"
        AppLanguage.ENGLISH -> "Full"
        AppLanguage.ARABIC -> "كامل"
    }

    val notEnabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "未启用"
        AppLanguage.ENGLISH -> "Not Enabled"
        AppLanguage.ARABIC -> "غير مفعل"
    }

    val ipRegion: String get() = when (lang) {
        AppLanguage.CHINESE -> "IP 地区"
        AppLanguage.ENGLISH -> "IP Region"
        AppLanguage.ARABIC -> "منطقة IP"
    }

    val supportedCountriesHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持：中国、美国、日本、韩国、英国、德国、法国、俄罗斯、巴西、印度、澳大利亚、加拿大、新加坡、香港、台湾、欧洲、亚洲"
        AppLanguage.ENGLISH -> "Supported: China, USA, Japan, Korea, UK, Germany, France, Russia, Brazil, India, Australia, Canada, Singapore, Hong Kong, Taiwan, Europe, Asia"
        AppLanguage.ARABIC -> "مدعوم: الصين، الولايات المتحدة، اليابان، كوريا، المملكة المتحدة، ألمانيا، فرنسا، روسيا، البرازيل، الهند، أستراليا، كندا، سنغافورة، هونغ كونغ، تايوان، أوروبا، آسيا"
    }

    val isolationDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "独立环境为每个应用创建隔离的浏览器环境，包括随机指纹、伪造 Header 和 IP 伪装，可有效防止网站追踪和检测。适用于多开、防关联等场景。"
        AppLanguage.ENGLISH -> "Isolated environment creates a separate browser environment for each app, including random fingerprint, forged headers and IP spoofing, effectively preventing website tracking and detection. Suitable for multi-instance and anti-association scenarios."
        AppLanguage.ARABIC -> "تنشئ البيئة المعزولة بيئة متصفح منفصلة لكل تطبيق، بما في ذلك البصمة العشوائية والرؤوس المزيفة وتزييف IP، مما يمنع بشكل فعال تتبع الموقع والكشف. مناسب لسيناريوهات التشغيل المتعدد ومكافحة الارتباط."
    }

    val canvasProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "Canvas 防护"
        AppLanguage.ENGLISH -> "Canvas Protection"
        AppLanguage.ARABIC -> "حماية Canvas"
    }

    val canvasProtectionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "防止 Canvas 指纹追踪"
        AppLanguage.ENGLISH -> "Prevent Canvas fingerprint tracking"
        AppLanguage.ARABIC -> "منع تتبع بصمة Canvas"
    }

    val webglProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "WebGL 防护"
        AppLanguage.ENGLISH -> "WebGL Protection"
        AppLanguage.ARABIC -> "حماية WebGL"
    }

    val webglProtectionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "伪造 WebGL 渲染器信息"
        AppLanguage.ENGLISH -> "Spoof WebGL renderer information"
        AppLanguage.ARABIC -> "تزييف معلومات عارض WebGL"
    }

    val audioProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "Audio 防护"
        AppLanguage.ENGLISH -> "Audio Protection"
        AppLanguage.ARABIC -> "حماية الصوت"
    }

    val audioProtectionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "防止 AudioContext 指纹"
        AppLanguage.ENGLISH -> "Prevent AudioContext fingerprint"
        AppLanguage.ARABIC -> "منع بصمة AudioContext"
    }

    val webrtcProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "WebRTC 防泄漏"
        AppLanguage.ENGLISH -> "WebRTC Leak Protection"
        AppLanguage.ARABIC -> "حماية تسرب WebRTC"
    }

    val webrtcProtectionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止真实 IP 通过 WebRTC 泄漏"
        AppLanguage.ENGLISH -> "Block real IP leakage through WebRTC"
        AppLanguage.ARABIC -> "منع تسرب IP الحقيقي عبر WebRTC"
    }

    val headerSpoofing: String get() = when (lang) {
        AppLanguage.CHINESE -> "Header 伪造"
        AppLanguage.ENGLISH -> "Header Spoofing"
        AppLanguage.ARABIC -> "تزييف الرؤوس"
    }

    val headerSpoofingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "伪造 HTTP 请求头"
        AppLanguage.ENGLISH -> "Spoof HTTP request headers"
        AppLanguage.ARABIC -> "تزييف رؤوس طلبات HTTP"
    }

    val ipSpoofing: String get() = when (lang) {
        AppLanguage.CHINESE -> "IP 伪装"
        AppLanguage.ENGLISH -> "IP Spoofing"
        AppLanguage.ARABIC -> "تزييف IP"
    }

    val ipSpoofingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "通过 Header 伪装 IP 地址"
        AppLanguage.ENGLISH -> "Spoof IP address through headers"
        AppLanguage.ARABIC -> "تزييف عنوان IP عبر الرؤوس"
    }

    val randomFingerprint: String get() = when (lang) {
        AppLanguage.CHINESE -> "Random fingerprint"
        AppLanguage.ENGLISH -> "Random Fingerprint"
        AppLanguage.ARABIC -> "بصمة عشوائية"
    }

    val randomFingerprintHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成随机浏览器指纹"
        AppLanguage.ENGLISH -> "Generate random browser fingerprint"
        AppLanguage.ARABIC -> "إنشاء بصمة متصفح عشوائية"
    }

    val fontProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "字体防护"
        AppLanguage.ENGLISH -> "Font Protection"
        AppLanguage.ARABIC -> "حماية الخطوط"
    }

    val fontProtectionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "防止字体指纹检测"
        AppLanguage.ENGLISH -> "Prevent font fingerprint detection"
        AppLanguage.ARABIC -> "منع اكتشاف بصمة الخطوط"
    }

    val storageIsolation: String get() = when (lang) {
        AppLanguage.CHINESE -> "存储隔离"
        AppLanguage.ENGLISH -> "Storage Isolation"
        AppLanguage.ARABIC -> "عزل التخزين"
    }

    val storageIsolationHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "独立的 Cookie 和 LocalStorage"
        AppLanguage.ENGLISH -> "Independent Cookie and LocalStorage"
        AppLanguage.ARABIC -> "Cookie و LocalStorage مستقلة"
    }

    val languageSpoofing: String get() = when (lang) {
        AppLanguage.CHINESE -> "语言伪装"
        AppLanguage.ENGLISH -> "Language Spoofing"
        AppLanguage.ARABIC -> "تزييف اللغة"
    }

    val languageSpoofingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "伪装浏览器语言"
        AppLanguage.ENGLISH -> "Spoof browser language"
        AppLanguage.ARABIC -> "تزييف لغة المتصفح"
    }

    val resolutionSpoofing: String get() = when (lang) {
        AppLanguage.CHINESE -> "分辨率伪装"
        AppLanguage.ENGLISH -> "Resolution Spoofing"
        AppLanguage.ARABIC -> "تزييف الدقة"
    }

    val resolutionSpoofingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "伪装屏幕分辨率"
        AppLanguage.ENGLISH -> "Spoof screen resolution"
        AppLanguage.ARABIC -> "تزييف دقة الشاشة"
    }

    val regenerateOnLaunch: String get() = when (lang) {
        AppLanguage.CHINESE -> "每次启动重新生成"
        AppLanguage.ENGLISH -> "Regenerate on Launch"
        AppLanguage.ARABIC -> "إعادة الإنشاء عند التشغيل"
    }

    val regenerateOnLaunchHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "每次启动应用时生成新指纹"
        AppLanguage.ENGLISH -> "Generate new fingerprint on each app launch"
        AppLanguage.ARABIC -> "إنشاء بصمة جديدة في كل تشغيل للتطبيق"
    }
    // ==================== Force-run Strings ====================
    val forcedRunSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制运行设置"
        AppLanguage.ENGLISH -> "Forced Run Settings"
        AppLanguage.ARABIC -> "إعدادات التشغيل الإجباري"
    }

    val enableForcedRun: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用强制运行"
        AppLanguage.ENGLISH -> "Enable Forced Run"
        AppLanguage.ARABIC -> "تمكين التشغيل الإجباري"
    }

    val forcedRunHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "在指定时间段内强制运行应用，无法退出"
        AppLanguage.ENGLISH -> "Force app to run during specified time, cannot exit"
        AppLanguage.ARABIC -> "إجبار التطبيق على العمل خلال الوقت المحدد، لا يمكن الخروج"
    }

    val forcedRunMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行模式"
        AppLanguage.ENGLISH -> "Run Mode"
        AppLanguage.ARABIC -> "وضع التشغيل"
    }

    val fixedTimeMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "固定时段"
        AppLanguage.ENGLISH -> "Fixed Time"
        AppLanguage.ARABIC -> "وقت ثابت"
    }

    val countdownMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "倒计时"
        AppLanguage.ENGLISH -> "Countdown"
        AppLanguage.ARABIC -> "العد التنازلي"
    }

    val fixedTimeModeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "在固定时间段内强制运行，时间到自动退出"
        AppLanguage.ENGLISH -> "Force run during fixed time period, auto exit when time ends"
        AppLanguage.ARABIC -> "التشغيل الإجباري خلال فترة زمنية محددة، الخروج التلقائي عند انتهاء الوقت"
    }

    val countdownModeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动后开始倒计时，时间到自动退出"
        AppLanguage.ENGLISH -> "Start countdown after launch, auto exit when time ends"
        AppLanguage.ARABIC -> "بدء العد التنازلي بعد التشغيل، الخروج التلقائي عند انتهاء الوقت"
    }

    val forcedRunStartTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "开始时间"
        AppLanguage.ENGLISH -> "Start Time"
        AppLanguage.ARABIC -> "وقت البدء"
    }

    val forcedRunEndTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "结束时间"
        AppLanguage.ENGLISH -> "End Time"
        AppLanguage.ARABIC -> "وقت الانتهاء"
    }

    val activeDays: String get() = when (lang) {
        AppLanguage.CHINESE -> "生效日期"
        AppLanguage.ENGLISH -> "Active Days"
        AppLanguage.ARABIC -> "أيام التفعيل"
    }

    val countdownDuration: String get() = when (lang) {
        AppLanguage.CHINESE -> "倒计时时长"
        AppLanguage.ENGLISH -> "Countdown Duration"
        AppLanguage.ARABIC -> "مدة العد التنازلي"
    }

    val minutes: String get() = when (lang) {
        AppLanguage.CHINESE -> "分钟"
        AppLanguage.ENGLISH -> "minutes"
        AppLanguage.ARABIC -> "دقائق"
    }

    val minutesShort: String get() = when (lang) {
        AppLanguage.CHINESE -> "分"
        AppLanguage.ENGLISH -> "min"
        AppLanguage.ARABIC -> "د"
    }

    val accessStartTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "可进入开始时间"
        AppLanguage.ENGLISH -> "Access Start Time"
        AppLanguage.ARABIC -> "وقت بدء الوصول"
    }

    val accessEndTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "可进入结束时间"
        AppLanguage.ENGLISH -> "Access End Time"
        AppLanguage.ARABIC -> "وقت انتهاء الوصول"
    }

    val accessDays: String get() = when (lang) {
        AppLanguage.CHINESE -> "可进入日期"
        AppLanguage.ENGLISH -> "Access Days"
        AppLanguage.ARABIC -> "أيام الوصول"
    }

    val blockSystemUI: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏蔽系统UI"
        AppLanguage.ENGLISH -> "Block System UI"
        AppLanguage.ARABIC -> "حظر واجهة النظام"
    }

    val blockBackButton: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏蔽返回键"
        AppLanguage.ENGLISH -> "Block Back Button"
        AppLanguage.ARABIC -> "حظر زر الرجوع"
    }

    val blockHomeButton: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏蔽Home键"
        AppLanguage.ENGLISH -> "Block Home Button"
        AppLanguage.ARABIC -> "حظر زر الرئيسية"
    }

    val showCountdownTimer: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示倒计时"
        AppLanguage.ENGLISH -> "Show Countdown"
        AppLanguage.ARABIC -> "عرض العد التنازلي"
    }

    val allowEmergencyExit: String get() = when (lang) {
        AppLanguage.CHINESE -> "允许紧急退出"
        AppLanguage.ENGLISH -> "Allow Emergency Exit"
        AppLanguage.ARABIC -> "السماح بالخروج الطارئ"
    }

    val emergencyExitHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "设置密码后可通过密码紧急退出"
        AppLanguage.ENGLISH -> "Set password to allow emergency exit"
        AppLanguage.ARABIC -> "تعيين كلمة مرور للسماح بالخروج الطارئ"
    }

    val emergencyPassword: String get() = when (lang) {
        AppLanguage.CHINESE -> "紧急退出密码"
        AppLanguage.ENGLISH -> "Emergency Password"
        AppLanguage.ARABIC -> "كلمة مرور الطوارئ"
    }

    val emergencyPasswordHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入4-8位数字密码"
        AppLanguage.ENGLISH -> "Enter 4-8 digit password"
        AppLanguage.ARABIC -> "أدخل كلمة مرور من 4-8 أرقام"
    }

    val forcedRunWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "警告：启用强制运行后，应用将在指定时间内无法退出。请确保已设置紧急退出密码以防万一。此功能适用于专注学习、儿童管控等场景。"
        AppLanguage.ENGLISH -> "Warning: After enabling forced run, the app cannot be exited during the specified time. Please set an emergency password just in case. This feature is suitable for focused learning, parental control, etc."
        AppLanguage.ARABIC -> "تحذير: بعد تمكين التشغيل الإجباري، لا يمكن الخروج من التطبيق خلال الوقت المحدد. يرجى تعيين كلمة مرور طوارئ احتياطياً. هذه الميزة مناسبة للتعلم المركز والرقابة الأبوية وما إلى ذلك."
    }

    val forcedRunActive: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制运行中"
        AppLanguage.ENGLISH -> "Forced Run Active"
        AppLanguage.ARABIC -> "التشغيل الإجباري نشط"
    }

    val cannotExitDuringForcedRun: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制运行期间无法退出"
        AppLanguage.ENGLISH -> "Cannot exit during forced run"
        AppLanguage.ARABIC -> "لا يمكن الخروج أثناء التشغيل الإجباري"
    }

    val enterEmergencyPassword: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入紧急退出密码"
        AppLanguage.ENGLISH -> "Enter emergency password"
        AppLanguage.ARABIC -> "أدخل كلمة مرور الطوارئ"
    }

    val wrongPassword: String get() = when (lang) {
        AppLanguage.CHINESE -> "密码错误"
        AppLanguage.ENGLISH -> "Wrong password"
        AppLanguage.ARABIC -> "كلمة مرور خاطئة"
    }

    val nextAccessTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "下次可进入时间: %s"
        AppLanguage.ENGLISH -> "Next access time: %s"
        AppLanguage.ARABIC -> "وقت الوصول التالي: %s"
    }
    // ==================== Advanced Features ====================
    val blackTechFeatures: String get() = when (lang) {
        AppLanguage.CHINESE -> "黑科技功能"
        AppLanguage.ENGLISH -> "Black Tech Features"
        AppLanguage.ARABIC -> "ميزات التقنية السوداء"
    }

    val blackTechDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "音量、灯光、性能等强制控制功能"
        AppLanguage.ENGLISH -> "Force control for volume, lights, performance, etc."
        AppLanguage.ARABIC -> "التحكم القسري في الصوت والأضواء والأداء، إلخ"
    }

    val blackTechWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "以下功能可能对设备造成影响，请谨慎使用\n⚠️ 仅部分设备支持，效果因设备而异"
        AppLanguage.ENGLISH -> "The following features may affect the device, use with caution\n⚠️ Only supported on some devices, effects vary"
        AppLanguage.ARABIC -> "قد تؤثر الميزات التالية على الجهاز، استخدمها بحذر\n⚠️ مدعومة فقط على بعض الأجهزة، تختلف النتائج"
    }

    val forceMaxVolume: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制最大音量"
        AppLanguage.ENGLISH -> "Force Max Volume"
        AppLanguage.ARABIC -> "فرض أقصى صوت"
    }

    val forceMaxVolumeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将所有音量调至最大"
        AppLanguage.ENGLISH -> "Set all volumes to maximum"
        AppLanguage.ARABIC -> "ضبط جميع مستويات الصوت على الحد الأقصى"
    }

    val forceMaxVibration: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制持续震动"
        AppLanguage.ENGLISH -> "Force Continuous Vibration"
        AppLanguage.ARABIC -> "فرض الاهتزاز المستمر"
    }

    val forceMaxVibrationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "持续最大强度震动"
        AppLanguage.ENGLISH -> "Continuous maximum vibration"
        AppLanguage.ARABIC -> "اهتزاز مستمر بأقصى قوة"
    }

    val forceFlashlight: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制闪光灯"
        AppLanguage.ENGLISH -> "Force Flashlight"
        AppLanguage.ARABIC -> "فرض الفلاش"
    }

    val forceFlashlightDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "持续开启闪光灯"
        AppLanguage.ENGLISH -> "Keep flashlight on"
        AppLanguage.ARABIC -> "إبقاء الفلاش مضاءً"
    }

    val strobeMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "爆闪模式"
        AppLanguage.ENGLISH -> "Strobe Mode"
        AppLanguage.ARABIC -> "وضع الوميض"
    }

    val strobeModeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "高频闪烁（可能引起不适）"
        AppLanguage.ENGLISH -> "High frequency flashing (may cause discomfort)"
        AppLanguage.ARABIC -> "وميض عالي التردد (قد يسبب إزعاج)"
    }

    val forceMaxPerformance: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制最大性能"
        AppLanguage.ENGLISH -> "Force Max Performance"
        AppLanguage.ARABIC -> "فرض أقصى أداء"
    }

    val forceMaxPerformanceDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "高CPU占用，耗电发热"
        AppLanguage.ENGLISH -> "High CPU usage, drains battery and heats up"
        AppLanguage.ARABIC -> "استخدام عالي للمعالج، يستنزف البطارية ويسخن"
    }

    val forceMuteMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制静音模式"
        AppLanguage.ENGLISH -> "Force Mute Mode"
        AppLanguage.ARABIC -> "فرض وضع الصامت"
    }

    val forceMuteModeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "静音所有音频"
        AppLanguage.ENGLISH -> "Mute all audio"
        AppLanguage.ARABIC -> "كتم جميع الأصوات"
    }

    val forceBlockVolumeKeys: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏蔽音量键"
        AppLanguage.ENGLISH -> "Block Volume Keys"
        AppLanguage.ARABIC -> "حظر أزرار الصوت"
    }

    val forceBlockVolumeKeysDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "禁止调节音量"
        AppLanguage.ENGLISH -> "Disable volume adjustment"
        AppLanguage.ARABIC -> "تعطيل ضبط الصوت"
    }

    val forceBlockPowerKey: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏蔽电源键"
        AppLanguage.ENGLISH -> "Block Power Key"
        AppLanguage.ARABIC -> "حظر زر الطاقة"
    }

    val forceBlockPowerKeyDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "需要辅助功能权限"
        AppLanguage.ENGLISH -> "Requires accessibility permission"
        AppLanguage.ARABIC -> "يتطلب إذن إمكانية الوصول"
    }

    val forceBlackScreen: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制全黑屏"
        AppLanguage.ENGLISH -> "Force Black Screen"
        AppLanguage.ARABIC -> "فرض الشاشة السوداء"
    }

    val forceBlackScreenDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏幕全黑且禁止滑动"
        AppLanguage.ENGLISH -> "Screen goes black and touch disabled"
        AppLanguage.ARABIC -> "الشاشة سوداء واللمس معطل"
    }

    val forceScreenRotation: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制屏幕翻转"
        AppLanguage.ENGLISH -> "Force Screen Rotation"
        AppLanguage.ARABIC -> "فرض تدوير الشاشة"
    }

    val forceScreenRotationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏幕持续横竖切换"
        AppLanguage.ENGLISH -> "Screen continuously rotates"
        AppLanguage.ARABIC -> "الشاشة تدور باستمرار"
    }

    val forceBlockTouch: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏蔽触摸"
        AppLanguage.ENGLISH -> "Block Touch"
        AppLanguage.ARABIC -> "حظر اللمس"
    }

    val forceBlockTouchDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "禁止所有触摸操作"
        AppLanguage.ENGLISH -> "Disable all touch operations"
        AppLanguage.ARABIC -> "تعطيل جميع عمليات اللمس"
    }

    val disguiseAsSystemApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "伪装系统应用"
        AppLanguage.ENGLISH -> "Disguise as System App"
        AppLanguage.ARABIC -> "التنكر كتطبيق نظام"
    }

    val disguiseAsSystemAppDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "伪装为系统应用，无法通过正常方式卸载"
        AppLanguage.ENGLISH -> "Disguise as system app, cannot be uninstalled normally"
        AppLanguage.ARABIC -> "التنكر كتطبيق نظام، لا يمكن إلغاء تثبيته بشكل طبيعي"
    }

    val multiLauncherIcons: String get() = when (lang) {
        AppLanguage.CHINESE -> "多桌面图标"
        AppLanguage.ENGLISH -> "Multi Launcher Icons"
        AppLanguage.ARABIC -> "أيقونات متعددة"
    }

    val multiLauncherIconsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建多个桌面快捷方式，删除任意一个则全部消失"
        AppLanguage.ENGLISH -> "Create multiple launcher shortcuts, deleting any one removes all"
        AppLanguage.ARABIC -> "إنشاء اختصارات متعددة، حذف أي واحد يزيل الكل"
    }

    val multiLauncherIconsCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "图标数量"
        AppLanguage.ENGLISH -> "Icon Count"
        AppLanguage.ARABIC -> "عدد الأيقونات"
    }

    val blackTechFinalWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "⚠️ 警告：启用以上功能可能导致设备发热、电量快速消耗等问题。请确保了解风险后再启用。部分功能需要特殊权限才能生效。"
        AppLanguage.ENGLISH -> "⚠️ Warning: Enabling the above features may cause device heating, rapid battery drain, etc. Please understand the risks before enabling. Some features require special permissions to work."
        AppLanguage.ARABIC -> "⚠️ تحذير: قد يؤدي تمكين الميزات أعلاه إلى تسخين الجهاز واستنزاف البطارية بسرعة وما إلى ذلك. يرجى فهم المخاطر قبل التمكين. تتطلب بعض الميزات أذونات خاصة للعمل."
    }
    // ==================== Browser Kernel Settings ====================
    val browserKernelTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "浏览器内核设置"
        AppLanguage.ENGLISH -> "Browser Kernel Settings"
        AppLanguage.ARABIC -> "إعدادات نواة المتصفح"
    }

    val browserKernelSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "管理 WebView 内核和浏览器"
        AppLanguage.ENGLISH -> "Manage WebView kernel and browsers"
        AppLanguage.ARABIC -> "إدارة نواة WebView والمتصفحات"
    }

    val currentWebViewInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前 WebView 信息"
        AppLanguage.ENGLISH -> "Current WebView Info"
        AppLanguage.ARABIC -> "معلومات WebView الحالي"
    }

    val webViewProvider: String get() = when (lang) {
        AppLanguage.CHINESE -> "WebView 提供者"
        AppLanguage.ENGLISH -> "WebView Provider"
        AppLanguage.ARABIC -> "مزود WebView"
    }

    val webViewVersion: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本"
        AppLanguage.ENGLISH -> "Version"
        AppLanguage.ARABIC -> "الإصدار"
    }

    val webViewPackage: String get() = when (lang) {
        AppLanguage.CHINESE -> "包名"
        AppLanguage.ENGLISH -> "Package"
        AppLanguage.ARABIC -> "الحزمة"
    }

    val changeWebViewProvider: String get() = when (lang) {
        AppLanguage.CHINESE -> "更改 WebView 提供者"
        AppLanguage.ENGLISH -> "Change WebView Provider"
        AppLanguage.ARABIC -> "تغيير مزود WebView"
    }

    val changeWebViewProviderDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "打开系统设置更改默认 WebView 实现（需要开发者选项）"
        AppLanguage.ENGLISH -> "Open system settings to change default WebView (Developer Options required)"
        AppLanguage.ARABIC -> "فتح إعدادات النظام لتغيير WebView الافتراضي (يتطلب خيارات المطور)"
    }

    val installedBrowsers: String get() = when (lang) {
        AppLanguage.CHINESE -> "已安装的浏览器"
        AppLanguage.ENGLISH -> "Installed Browsers"
        AppLanguage.ARABIC -> "المتصفحات المثبتة"
    }

    val installedBrowsersDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "这些浏览器可能支持作为 WebView 提供者"
        AppLanguage.ENGLISH -> "These browsers may support being WebView provider"
        AppLanguage.ARABIC -> "قد تدعم هذه المتصفحات أن تكون مزود WebView"
    }

    val noBrowserInstalled: String get() = when (lang) {
        AppLanguage.CHINESE -> "未检测到支持 WebView 的浏览器"
        AppLanguage.ENGLISH -> "No WebView-capable browser detected"
        AppLanguage.ARABIC -> "لم يتم اكتشاف متصفح يدعم WebView"
    }

    val recommendedBrowsers: String get() = when (lang) {
        AppLanguage.CHINESE -> "推荐浏览器下载"
        AppLanguage.ENGLISH -> "Recommended Browsers"
        AppLanguage.ARABIC -> "المتصفحات الموصى بها"
    }

    val recommendedBrowsersDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装这些浏览器后可在开发者选项中选择作为 WebView 提供者"
        AppLanguage.ENGLISH -> "After installing, select as WebView provider in Developer Options"
        AppLanguage.ARABIC -> "بعد التثبيت، حدد كمزود WebView في خيارات المطور"
    }

    val browserChrome: String get() = when (lang) {
        AppLanguage.CHINESE -> "Google Chrome"
        AppLanguage.ENGLISH -> "Google Chrome"
        AppLanguage.ARABIC -> "Google Chrome"
    }

    val browserChromeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Google 官方浏览器，性能优秀"
        AppLanguage.ENGLISH -> "Google's official browser, excellent performance"
        AppLanguage.ARABIC -> "متصفح Google الرسمي، أداء ممتاز"
    }

    val browserEdge: String get() = when (lang) {
        AppLanguage.CHINESE -> "Microsoft Edge"
        AppLanguage.ENGLISH -> "Microsoft Edge"
        AppLanguage.ARABIC -> "Microsoft Edge"
    }

    val browserEdgeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "微软官方浏览器，基于 Chromium"
        AppLanguage.ENGLISH -> "Microsoft's browser, Chromium-based"
        AppLanguage.ARABIC -> "متصفح Microsoft، مبني على Chromium"
    }

    val browserFirefox: String get() = when (lang) {
        AppLanguage.CHINESE -> "Mozilla Firefox"
        AppLanguage.ENGLISH -> "Mozilla Firefox"
        AppLanguage.ARABIC -> "Mozilla Firefox"
    }

    val browserFirefoxDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "开源浏览器，注重隐私保护"
        AppLanguage.ENGLISH -> "Open source, privacy-focused"
        AppLanguage.ARABIC -> "مفتوح المصدر، يركز على الخصوصية"
    }

    val browserBrave: String get() = when (lang) {
        AppLanguage.CHINESE -> "Brave"
        AppLanguage.ENGLISH -> "Brave"
        AppLanguage.ARABIC -> "Brave"
    }

    val browserBraveDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "内置广告拦截，隐私优先"
        AppLanguage.ENGLISH -> "Built-in ad blocking, privacy first"
        AppLanguage.ARABIC -> "حظر الإعلانات المدمج، الخصوصية أولاً"
    }

    val browserVia: String get() = when (lang) {
        AppLanguage.CHINESE -> "Via 浏览器"
        AppLanguage.ENGLISH -> "Via Browser"
        AppLanguage.ARABIC -> "متصفح Via"
    }

    val browserViaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "轻量级浏览器，体积小速度快"
        AppLanguage.ENGLISH -> "Lightweight browser, small and fast"
        AppLanguage.ARABIC -> "متصفح خفيف، صغير وسريع"
    }

    val browserX5: String get() = when (lang) {
        AppLanguage.CHINESE -> "腾讯 X5 内核"
        AppLanguage.ENGLISH -> "Tencent X5 Kernel"
        AppLanguage.ARABIC -> "نواة Tencent X5"
    }

    val browserX5Desc: String get() = when (lang) {
        AppLanguage.CHINESE -> "腾讯 WebView 解决方案，兼容性好"
        AppLanguage.ENGLISH -> "Tencent WebView solution, good compatibility"
        AppLanguage.ARABIC -> "حل Tencent WebView، توافق جيد"
    }

    val openPlayStore: String get() = when (lang) {
        AppLanguage.CHINESE -> "打开应用商店"
        AppLanguage.ENGLISH -> "Open Play Store"
        AppLanguage.ARABIC -> "فتح متجر Play"
    }

    val webViewNote: String get() = when (lang) {
        AppLanguage.CHINESE -> "注意：更改 WebView 提供者需要启用开发者选项"
        AppLanguage.ENGLISH -> "Note: Changing WebView provider requires Developer Options enabled"
        AppLanguage.ARABIC -> "ملاحظة: تغيير مزود WebView يتطلب تمكين خيارات المطور"
    }

    val howToEnableDeveloperOptions: String get() = when (lang) {
        AppLanguage.CHINESE -> "如何启用开发者选项？"
        AppLanguage.ENGLISH -> "How to enable Developer Options?"
        AppLanguage.ARABIC -> "كيفية تمكين خيارات المطور؟"
    }

    val developerOptionsSteps: String get() = when (lang) {
        AppLanguage.CHINESE -> "设置 → 关于手机 → 连续点击\"版本号\"7次"
        AppLanguage.ENGLISH -> "Settings → About Phone → Tap \"Build Number\" 7 times"
        AppLanguage.ARABIC -> "الإعدادات ← حول الهاتف ← انقر على \"رقم البناء\" 7 مرات"
    }

    val openDeveloperOptions: String get() = when (lang) {
        AppLanguage.CHINESE -> "打开开发者选项"
        AppLanguage.ENGLISH -> "Open Developer Options"
        AppLanguage.ARABIC -> "فتح خيارات المطور"
    }

    val currentlyUsing: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前使用中"
        AppLanguage.ENGLISH -> "Currently in use"
        AppLanguage.ARABIC -> "قيد الاستخدام حالياً"
    }

    val canBeWebViewProvider: String get() = when (lang) {
        AppLanguage.CHINESE -> "可设为 WebView 提供者"
        AppLanguage.ENGLISH -> "Can be WebView provider"
        AppLanguage.ARABIC -> "يمكن أن يكون مزود WebView"
    }

    val openInBrowser: String get() = when (lang) {
        AppLanguage.CHINESE -> "Open in browser"
        AppLanguage.ENGLISH -> "Open in browser"
        AppLanguage.ARABIC -> "فتح في المتصفح"
    }
    // ==================== Userscript Features ====================
    val userscriptTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "油猴脚本管理"
        AppLanguage.ENGLISH -> "Userscript Manager"
        AppLanguage.ARABIC -> "مدير سكريبتات المستخدم"
    }

    val userscriptSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装和管理油猴脚本"
        AppLanguage.ENGLISH -> "Install and manage userscripts"
        AppLanguage.ARABIC -> "تثبيت وإدارة سكريبتات المستخدم"
    }

    val myScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "我的脚本"
        AppLanguage.ENGLISH -> "My Scripts"
        AppLanguage.ARABIC -> "سكريبتاتي"
    }

    val scriptMarket: String get() = when (lang) {
        AppLanguage.CHINESE -> "脚本市场"
        AppLanguage.ENGLISH -> "Script Market"
        AppLanguage.ARABIC -> "سوق السكريبتات"
    }

    val noScriptsInstalled: String get() = when (lang) {
        AppLanguage.CHINESE -> "还没有安装脚本"
        AppLanguage.ENGLISH -> "No scripts installed"
        AppLanguage.ARABIC -> "لم يتم تثبيت سكريبتات"
    }

    val noScriptsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "去脚本市场发现更多实用脚本"
        AppLanguage.ENGLISH -> "Explore the script market to find useful scripts"
        AppLanguage.ARABIC -> "استكشف سوق السكريبتات للعثور على سكريبتات مفيدة"
    }

    val installScript: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装脚本"
        AppLanguage.ENGLISH -> "Install Script"
        AppLanguage.ARABIC -> "تثبيت السكريبت"
    }

    val installFromUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "从 URL 安装"
        AppLanguage.ENGLISH -> "Install from URL"
        AppLanguage.ARABIC -> "تثبيت من URL"
    }

    val installFromFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "从文件安装"
        AppLanguage.ENGLISH -> "Install from file"
        AppLanguage.ARABIC -> "تثبيت من ملف"
    }

    val scriptUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "脚本 URL"
        AppLanguage.ENGLISH -> "Script URL"
        AppLanguage.ARABIC -> "URL السكريبت"
    }

    val scriptUrlHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入 .user.js 脚本链接"
        AppLanguage.ENGLISH -> "Enter .user.js script URL"
        AppLanguage.ARABIC -> "أدخل رابط سكريبت .user.js"
    }

    val installing: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在安装..."
        AppLanguage.ENGLISH -> "Installing..."
        AppLanguage.ARABIC -> "جاري التثبيت..."
    }

    val installSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "脚本安装成功"
        AppLanguage.ENGLISH -> "Script installed successfully"
        AppLanguage.ARABIC -> "تم تثبيت السكريبت بنجاح"
    }

    val installFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "脚本安装失败"
        AppLanguage.ENGLISH -> "Failed to install script"
        AppLanguage.ARABIC -> "فشل تثبيت السكريبت"
    }

    val deleteScript: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除脚本"
        AppLanguage.ENGLISH -> "Delete Script"
        AppLanguage.ARABIC -> "حذف السكريبت"
    }

    val deleteScriptConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定要删除这个脚本吗？"
        AppLanguage.ENGLISH -> "Are you sure you want to delete this script?"
        AppLanguage.ARABIC -> "هل أنت متأكد من حذف هذا السكريبت؟"
    }

    val totalInstalls: String get() = when (lang) {
        AppLanguage.CHINESE -> "总安装量"
        AppLanguage.ENGLISH -> "Total installs"
        AppLanguage.ARABIC -> "إجمالي التثبيتات"
    }

    val dailyInstalls: String get() = when (lang) {
        AppLanguage.CHINESE -> "每日安装"
        AppLanguage.ENGLISH -> "Daily installs"
        AppLanguage.ARABIC -> "التثبيتات اليومية"
    }

    val searchScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索脚本..."
        AppLanguage.ENGLISH -> "Search scripts..."
        AppLanguage.ARABIC -> "بحث السكريبتات..."
    }

    val popularScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "热门脚本"
        AppLanguage.ENGLISH -> "Popular Scripts"
        AppLanguage.ARABIC -> "السكريبتات الشائعة"
    }

    val latestScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "最新脚本"
        AppLanguage.ENGLISH -> "Latest Scripts"
        AppLanguage.ARABIC -> "أحدث السكريبتات"
    }

    val searchBySite: String get() = when (lang) {
        AppLanguage.CHINESE -> "按网站搜索"
        AppLanguage.ENGLISH -> "Search by site"
        AppLanguage.ARABIC -> "بحث حسب الموقع"
    }

    val popularSites: String get() = when (lang) {
        AppLanguage.CHINESE -> "热门网站"
        AppLanguage.ENGLISH -> "Popular Sites"
        AppLanguage.ARABIC -> "المواقع الشائعة"
    }

    val scriptDetails: String get() = when (lang) {
        AppLanguage.CHINESE -> "脚本详情"
        AppLanguage.ENGLISH -> "Script Details"
        AppLanguage.ARABIC -> "تفاصيل السكريبت"
    }

    val scriptAuthor: String get() = when (lang) {
        AppLanguage.CHINESE -> "作者"
        AppLanguage.ENGLISH -> "Author"
        AppLanguage.ARABIC -> "المؤلف"
    }

    val scriptVersion: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本"
        AppLanguage.ENGLISH -> "Version"
        AppLanguage.ARABIC -> "الإصدار"
    }

    val matchUrls: String get() = when (lang) {
        AppLanguage.CHINESE -> "匹配网址"
        AppLanguage.ENGLISH -> "Match URLs"
        AppLanguage.ARABIC -> "عناوين URL المطابقة"
    }

    val viewCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "查看代码"
        AppLanguage.ENGLISH -> "View Code"
        AppLanguage.ARABIC -> "عرض الكود"
    }

    val exportScript: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出脚本"
        AppLanguage.ENGLISH -> "Export Script"
        AppLanguage.ARABIC -> "تصدير السكريبت"
    }

    val noUpdateAvailable: String get() = when (lang) {
        AppLanguage.CHINESE -> "已是最新版本"
        AppLanguage.ENGLISH -> "Already up to date"
        AppLanguage.ARABIC -> "محدث بالفعل"
    }

    val updateAvailable: String get() = when (lang) {
        AppLanguage.CHINESE -> "发现新版本"
        AppLanguage.ENGLISH -> "Update available"
        AppLanguage.ARABIC -> "تحديث متاح"
    }

    val greasyforkMarket: String get() = when (lang) {
        AppLanguage.CHINESE -> "Greasy Fork"
        AppLanguage.ENGLISH -> "Greasy Fork"
        AppLanguage.ARABIC -> "Greasy Fork"
    }

    val loadingScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "加载脚本中..."
        AppLanguage.ENGLISH -> "Loading scripts..."
        AppLanguage.ARABIC -> "جاري تحميل السكريبتات..."
    }

    val loadFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Load failed"
        AppLanguage.ENGLISH -> "Load failed"
        AppLanguage.ARABIC -> "فشل التحميل"
    }

    val scriptsCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "已安装 %d 个脚本"
        AppLanguage.ENGLISH -> "%d scripts installed"
        AppLanguage.ARABIC -> "تم تثبيت %d سكريبت"
    }

    val enabledScriptsCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个已启用"
        AppLanguage.ENGLISH -> "%d enabled"
        AppLanguage.ARABIC -> "%d مفعل"
    }

    val userscriptCardHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "为此应用选择要启用的油猴脚本"
        AppLanguage.ENGLISH -> "Select userscripts to enable for this app"
        AppLanguage.ARABIC -> "حدد السكريبتات لتمكينها لهذا التطبيق"
    }

    val selectedScriptsCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "已选脚本 (%d)"
        AppLanguage.ENGLISH -> "Selected Scripts (%d)"
        AppLanguage.ARABIC -> "السكريبتات المحددة (%d)"
    }

    val selectScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择脚本"
        AppLanguage.ENGLISH -> "Select Scripts"
        AppLanguage.ARABIC -> "اختر السكريبتات"
    }

    val selectUserscripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择油猴脚本"
        AppLanguage.ENGLISH -> "Select Userscripts"
        AppLanguage.ARABIC -> "اختر السكريبتات"
    }

    val searchScriptsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索脚本名称..."
        AppLanguage.ENGLISH -> "Search scripts by name..."
        AppLanguage.ARABIC -> "بحث عن السكريبتات بالاسم..."
    }

    val noMatchingScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "没有匹配的脚本"
        AppLanguage.ENGLISH -> "No matching scripts"
        AppLanguage.ARABIC -> "لا توجد سكريبتات مطابقة"
    }

    val matchRules: String get() = when (lang) {
        AppLanguage.CHINESE -> "匹配规则"
        AppLanguage.ENGLISH -> "Match rules"
        AppLanguage.ARABIC -> "قواعد المطابقة"
    }

    val userscriptPanelTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "油猴脚本"
        AppLanguage.ENGLISH -> "Userscripts"
        AppLanguage.ARABIC -> "السكريبتات"
    }

    val matchedScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "匹配的脚本"
        AppLanguage.ENGLISH -> "Matched Scripts"
        AppLanguage.ARABIC -> "السكريبتات المطابقة"
    }

    val noMatchedScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前页面没有匹配的脚本"
        AppLanguage.ENGLISH -> "No scripts matched for this page"
        AppLanguage.ARABIC -> "لا توجد سكريبتات مطابقة لهذه الصفحة"
    }

    val scriptRunning: String get() = when (lang) {
        AppLanguage.CHINESE -> "已运行"
        AppLanguage.ENGLISH -> "Running"
        AppLanguage.ARABIC -> "قيد التشغيل"
    }

    val scriptPending: String get() = when (lang) {
        AppLanguage.CHINESE -> "待运行"
        AppLanguage.ENGLISH -> "Pending"
        AppLanguage.ARABIC -> "قيد الانتظار"
    }

    val scriptDisabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "已禁用"
        AppLanguage.ENGLISH -> "Disabled"
        AppLanguage.ARABIC -> "معطل"
    }

    val runNow: String get() = when (lang) {
        AppLanguage.CHINESE -> "立即运行"
        AppLanguage.ENGLISH -> "Run Now"
        AppLanguage.ARABIC -> "تشغيل الآن"
    }

    val reloadPage: String get() = when (lang) {
        AppLanguage.CHINESE -> "刷新页面"
        AppLanguage.ENGLISH -> "Reload Page"
        AppLanguage.ARABIC -> "إعادة تحميل الصفحة"
    }
    // ==================== Embedded Browser Engine ====================
    val embeddedEngineTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "内嵌浏览器引擎"
        AppLanguage.ENGLISH -> "Embedded Browser Engine"
        AppLanguage.ARABIC -> "محرك المتصفح المضمن"
    }

    val embeddedEngineDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载独立浏览器引擎，可嵌入导出的 APK 中"
        AppLanguage.ENGLISH -> "Download standalone engine to embed in exported APKs"
        AppLanguage.ARABIC -> "تنزيل محرك مستقل لتضمينه في ملفات APK المصدرة"
    }

    val engineSystemWebView: String get() = when (lang) {
        AppLanguage.CHINESE -> "系统 WebView"
        AppLanguage.ENGLISH -> "System WebView"
        AppLanguage.ARABIC -> "WebView النظام"
    }

    val engineSystemWebViewDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用设备内置的 Android WebView，零额外体积"
        AppLanguage.ENGLISH -> "Uses built-in Android WebView, zero extra size"
        AppLanguage.ARABIC -> "يستخدم WebView المدمج في Android، بدون حجم إضافي"
    }

    val engineGeckoView: String get() = when (lang) {
        AppLanguage.CHINESE -> "GeckoView (Firefox)"
        AppLanguage.ENGLISH -> "GeckoView (Firefox)"
        AppLanguage.ARABIC -> "GeckoView (Firefox)"
    }

    val engineGeckoViewDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "内嵌 Firefox 引擎，内置隐私保护与广告拦截"
        AppLanguage.ENGLISH -> "Embedded Firefox engine with privacy protection & ad blocking"
        AppLanguage.ARABIC -> "محرك Firefox مضمن مع حماية الخصوصية وحظر الإعلانات"
    }

    val engineReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "就绪"
        AppLanguage.ENGLISH -> "Ready"
        AppLanguage.ARABIC -> "جاهز"
    }

    val engineNotDownloaded: String get() = when (lang) {
        AppLanguage.CHINESE -> "未下载"
        AppLanguage.ENGLISH -> "Not Downloaded"
        AppLanguage.ARABIC -> "لم يتم التنزيل"
    }

    val engineDownloaded: String get() = when (lang) {
        AppLanguage.CHINESE -> "已下载"
        AppLanguage.ENGLISH -> "Downloaded"
        AppLanguage.ARABIC -> "تم التنزيل"
    }

    val engineDownloadBtn: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载引擎"
        AppLanguage.ENGLISH -> "Download Engine"
        AppLanguage.ARABIC -> "تنزيل المحرك"
    }

    val engineDeleteBtn: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除引擎"
        AppLanguage.ENGLISH -> "Delete Engine"
        AppLanguage.ARABIC -> "حذف المحرك"
    }

    val engineDownloading: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在下载..."
        AppLanguage.ENGLISH -> "Downloading..."
        AppLanguage.ARABIC -> "جاري التنزيل..."
    }

    val engineCancelDownload: String get() = when (lang) {
        AppLanguage.CHINESE -> "取消下载"
        AppLanguage.ENGLISH -> "Cancel Download"
        AppLanguage.ARABIC -> "إلغاء التنزيل"
    }

    val engineDownloadComplete: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载完成"
        AppLanguage.ENGLISH -> "Download Complete"
        AppLanguage.ARABIC -> "اكتمل التنزيل"
    }

    val engineDownloadFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载失败"
        AppLanguage.ENGLISH -> "Download Failed"
        AppLanguage.ARABIC -> "فشل التنزيل"
    }

    val engineRetry: String get() = when (lang) {
        AppLanguage.CHINESE -> "重试"
        AppLanguage.ENGLISH -> "Retry"
        AppLanguage.ARABIC -> "إعادة المحاولة"
    }

    val engineEstimatedSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "预估大小"
        AppLanguage.ENGLISH -> "Est. size"
        AppLanguage.ARABIC -> "الحجم المقدر"
    }

    val engineCurrentSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "占用空间"
        AppLanguage.ENGLISH -> "Disk usage"
        AppLanguage.ARABIC -> "استخدام القرص"
    }

    val engineDeleteConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定要删除已下载的引擎文件吗？"
        AppLanguage.ENGLISH -> "Are you sure you want to delete the engine files?"
        AppLanguage.ARABIC -> "هل أنت متأكد من حذف ملفات المحرك؟"
    }

    val engineVersionLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本"
        AppLanguage.ENGLISH -> "Version"
        AppLanguage.ARABIC -> "الإصدار"
    }

    val engineDefault: String get() = when (lang) {
        AppLanguage.CHINESE -> "默认"
        AppLanguage.ENGLISH -> "Default"
        AppLanguage.ARABIC -> "افتراضي"
    }

    val engineSelectTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "浏览器引擎"
        AppLanguage.ENGLISH -> "Browser Engine"
        AppLanguage.ARABIC -> "محرك المتصفح"
    }

    val engineSelectDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择导出 APK 使用的浏览器引擎"
        AppLanguage.ENGLISH -> "Select browser engine for exported APK"
        AppLanguage.ARABIC -> "اختر محرك المتصفح لملف APK المصدر"
    }

    val engineGeckoNotDownloaded: String get() = when (lang) {
        AppLanguage.CHINESE -> "GeckoView 引擎未下载，请先在“浏览器内核”页面下载"
        AppLanguage.ENGLISH -> "GeckoView engine not downloaded. Please download it from Browser Kernel page first."
        AppLanguage.ARABIC -> "لم يتم تنزيل محرك GeckoView. يرجى تنزيله من صفحة نواة المتصفح أولاً."
    }

    val engineApkSizeWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 GeckoView 将增加 APK 体积约 %s MB"
        AppLanguage.ENGLISH -> "Using GeckoView will increase APK size by ~%s MB"
        AppLanguage.ARABIC -> "استخدام GeckoView سيزيد حجم APK بنحو %s MB"
    }
    // ==================== Performance Optimization (Performance Optimization) ====================
    val performanceOptimization: String get() = when (lang) {
        AppLanguage.CHINESE -> "性能优化"
        AppLanguage.ENGLISH -> "Performance Optimization"
        AppLanguage.ARABIC -> "تحسين الأداء"
    }

    val performanceOptimizationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "全面优化应用性能：资源压缩、构建加速、加载提速"
        AppLanguage.ENGLISH -> "Comprehensive app optimization: resource compression, build acceleration, loading speed"
        AppLanguage.ARABIC -> "تحسين شامل: ضغط الموارد، تسريع البناء، سرعة التحميل"
    }

    val perfEnabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "已启用性能优化"
        AppLanguage.ENGLISH -> "Performance optimization enabled"
        AppLanguage.ARABIC -> "تم تفعيل تحسين الأداء"
    }

    val perfDisabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "未启用性能优化"
        AppLanguage.ENGLISH -> "Performance optimization disabled"
        AppLanguage.ARABIC -> "تحسين الأداء معطل"
    }

    val perfResourceOptimize: String get() = when (lang) {
        AppLanguage.CHINESE -> "资源优化"
        AppLanguage.ENGLISH -> "Resource Optimization"
        AppLanguage.ARABIC -> "تحسين الموارد"
    }

    val perfCompressImages: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片压缩"
        AppLanguage.ENGLISH -> "Image Compression"
        AppLanguage.ARABIC -> "ضغط الصور"
    }

    val perfCompressImagesHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "压缩 PNG/JPEG 图片，支持 WebP 转换"
        AppLanguage.ENGLISH -> "Compress PNG/JPEG images, supports WebP conversion"
        AppLanguage.ARABIC -> "ضغط صور PNG/JPEG، يدعم تحويل WebP"
    }

    val perfMinifyCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码压缩"
        AppLanguage.ENGLISH -> "Code Minification"
        AppLanguage.ARABIC -> "ضغط الكود"
    }

    val perfMinifyCodeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "压缩 JS/CSS/SVG，移除注释和多余空白"
        AppLanguage.ENGLISH -> "Minify JS/CSS/SVG, remove comments and whitespace"
        AppLanguage.ARABIC -> "ضغط JS/CSS/SVG، إزالة التعليقات والمسافات"
    }

    val perfConvertWebP: String get() = when (lang) {
        AppLanguage.CHINESE -> "WebP 转换"
        AppLanguage.ENGLISH -> "WebP Conversion"
        AppLanguage.ARABIC -> "تحويل WebP"
    }

    val perfConvertWebPHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "将 PNG/JPEG 转为 WebP 格式，体积减少 30-50%"
        AppLanguage.ENGLISH -> "Convert PNG/JPEG to WebP, 30-50% size reduction"
        AppLanguage.ARABIC -> "تحويل PNG/JPEG إلى WebP، تقليل الحجم 30-50%"
    }

    val perfRemoveUnused: String get() = when (lang) {
        AppLanguage.CHINESE -> "清理无用资源"
        AppLanguage.ENGLISH -> "Remove Unused Resources"
        AppLanguage.ARABIC -> "إزالة الموارد غير المستخدمة"
    }

    val perfRemoveUnusedHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "移除模板 APK 中不需要的语言包和默认资源"
        AppLanguage.ENGLISH -> "Remove unnecessary language packs and default resources from template"
        AppLanguage.ARABIC -> "إزالة حزم اللغات والموارد الافتراضية غير الضرورية"
    }

    val perfImageQuality: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片质量"
        AppLanguage.ENGLISH -> "Image Quality"
        AppLanguage.ARABIC -> "جودة الصورة"
    }

    val perfBuildOptimize: String get() = when (lang) {
        AppLanguage.CHINESE -> "构建加速"
        AppLanguage.ENGLISH -> "Build Acceleration"
        AppLanguage.ARABIC -> "تسريع البناء"
    }

    val perfParallelProcessing: String get() = when (lang) {
        AppLanguage.CHINESE -> "并行处理"
        AppLanguage.ENGLISH -> "Parallel Processing"
        AppLanguage.ARABIC -> "المعالجة المتوازية"
    }

    val perfParallelProcessingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "利用多核 CPU 并行处理资源文件"
        AppLanguage.ENGLISH -> "Process resource files in parallel using multi-core CPU"
        AppLanguage.ARABIC -> "معالجة ملفات الموارد بالتوازي باستخدام معالج متعدد النوى"
    }

    val perfEnableCache: String get() = when (lang) {
        AppLanguage.CHINESE -> "智能缓存"
        AppLanguage.ENGLISH -> "Smart Cache"
        AppLanguage.ARABIC -> "التخزين المؤقت الذكي"
    }

    val perfEnableCacheHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "缓存已优化的资源，避免重复处理"
        AppLanguage.ENGLISH -> "Cache optimized resources to avoid reprocessing"
        AppLanguage.ARABIC -> "تخزين الموارد المحسنة مؤقتاً لتجنب إعادة المعالجة"
    }

    val perfLoadOptimize: String get() = when (lang) {
        AppLanguage.CHINESE -> "加载提速"
        AppLanguage.ENGLISH -> "Loading Speed"
        AppLanguage.ARABIC -> "سرعة التحميل"
    }

    val perfPreloadHints: String get() = when (lang) {
        AppLanguage.CHINESE -> "资源预加载"
        AppLanguage.ENGLISH -> "Resource Preloading"
        AppLanguage.ARABIC -> "التحميل المسبق للموارد"
    }

    val perfPreloadHintsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "注入 preconnect/dns-prefetch 提示，加速资源下载"
        AppLanguage.ENGLISH -> "Inject preconnect/dns-prefetch hints for faster resource loading"
        AppLanguage.ARABIC -> "حقن تلميحات preconnect/dns-prefetch لتحميل أسرع"
    }

    val perfLazyLoading: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片懒加载"
        AppLanguage.ENGLISH -> "Image Lazy Loading"
        AppLanguage.ARABIC -> "التحميل الكسول للصور"
    }

    val perfLazyLoadingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "非首屏图片延迟加载，加速首屏渲染"
        AppLanguage.ENGLISH -> "Defer off-screen images, accelerate first paint"
        AppLanguage.ARABIC -> "تأخير صور خارج الشاشة، تسريع العرض الأول"
    }

    val perfOptimizeScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "脚本优化"
        AppLanguage.ENGLISH -> "Script Optimization"
        AppLanguage.ARABIC -> "تحسين البرامج النصية"
    }

    val perfOptimizeScriptsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动添加 defer 属性，避免阻塞页面渲染"
        AppLanguage.ENGLISH -> "Auto-add defer attribute, prevent render blocking"
        AppLanguage.ARABIC -> "إضافة خاصية defer تلقائياً، منع حظر العرض"
    }

    val perfRuntimeOptimize: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行时优化"
        AppLanguage.ENGLISH -> "Runtime Optimization"
        AppLanguage.ARABIC -> "تحسين وقت التشغيل"
    }

    val perfRuntimeScript: String get() = when (lang) {
        AppLanguage.CHINESE -> "性能增强脚本"
        AppLanguage.ENGLISH -> "Performance Enhancement Script"
        AppLanguage.ARABIC -> "نص تحسين الأداء"
    }

    val perfRuntimeScriptHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "注入滚动优化、内存管理、CDN 预连接等性能脚本"
        AppLanguage.ENGLISH -> "Inject scroll optimization, memory management, CDN preconnect scripts"
        AppLanguage.ARABIC -> "حقن نصوص تحسين التمرير وإدارة الذاكرة واتصال CDN المسبق"
    }

    val perfStatsOriginalSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "原始大小"
        AppLanguage.ENGLISH -> "Original Size"
        AppLanguage.ARABIC -> "الحجم الأصلي"
    }

    val perfStatsOptimizedSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化后大小"
        AppLanguage.ENGLISH -> "Optimized Size"
        AppLanguage.ARABIC -> "الحجم المحسن"
    }

    val perfStatsSaved: String get() = when (lang) {
        AppLanguage.CHINESE -> "节省"
        AppLanguage.ENGLISH -> "Saved"
        AppLanguage.ARABIC -> "تم التوفير"
    }

    val perfStatsImages: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片已压缩"
        AppLanguage.ENGLISH -> "Images compressed"
        AppLanguage.ARABIC -> "تم ضغط الصور"
    }

    val perfStatsCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码已压缩"
        AppLanguage.ENGLISH -> "Code files minified"
        AppLanguage.ARABIC -> "تم ضغط ملفات الكود"
    }

    val perfStatsTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化耗时"
        AppLanguage.ENGLISH -> "Optimization time"
        AppLanguage.ARABIC -> "وقت التحسين"
    }

    val perfOptimizing: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在优化性能..."
        AppLanguage.ENGLISH -> "Optimizing performance..."
        AppLanguage.ARABIC -> "جارٍ تحسين الأداء..."
    }

    val perfOptimizeComplete: String get() = when (lang) {
        AppLanguage.CHINESE -> "性能优化完成"
        AppLanguage.ENGLISH -> "Performance optimization complete"
        AppLanguage.ARABIC -> "اكتمل تحسين الأداء"
    }

    val featurePerfOptimize: String get() = when (lang) {
        AppLanguage.CHINESE -> "全面性能优化（资源/构建/加载/运行时）"
        AppLanguage.ENGLISH -> "Comprehensive performance optimization (resource/build/loading/runtime)"
        AppLanguage.ARABIC -> "تحسين شامل للأداء (الموارد/البناء/التحميل/وقت التشغيل)"
    }
    // ==================== App Hardening (App Hardening) ====================
    val hardeningEnabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "已启用加固保护"
        AppLanguage.ENGLISH -> "Hardening protection enabled"
        AppLanguage.ARABIC -> "تم تفعيل حماية التعزيز"
    }

    val hardeningDisabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "未启用加固"
        AppLanguage.ENGLISH -> "Hardening disabled"
        AppLanguage.ARABIC -> "التعزيز معطل"
    }

    val hardeningLevel: String get() = when (lang) {
        AppLanguage.CHINESE -> "加固等级"
        AppLanguage.ENGLISH -> "Hardening Level"
        AppLanguage.ARABIC -> "مستوى التعزيز"
    }

    val hardeningLevelBasic: String get() = when (lang) {
        AppLanguage.CHINESE -> "基础"
        AppLanguage.ENGLISH -> "Basic"
        AppLanguage.ARABIC -> "أساسي"
    }

    val hardeningLevelStandard: String get() = when (lang) {
        AppLanguage.CHINESE -> "标准"
        AppLanguage.ENGLISH -> "Standard"
        AppLanguage.ARABIC -> "قياسي"
    }

    val hardeningLevelAdvanced: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级"
        AppLanguage.ENGLISH -> "Advanced"
        AppLanguage.ARABIC -> "متقدم"
    }

    val hardeningLevelFortress: String get() = when (lang) {
        AppLanguage.CHINESE -> "堡垒"
        AppLanguage.ENGLISH -> "Fortress"
        AppLanguage.ARABIC -> "حصن"
    }

    val hardeningLevelBasicDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "DEX 加密 + 反调试 + 签名校验"
        AppLanguage.ENGLISH -> "DEX encryption + Anti-debug + Signature verify"
        AppLanguage.ARABIC -> "تشفير DEX + مكافحة التصحيح + التحقق من التوقيع"
    }

    val hardeningLevelStandardDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "基础 + SO 保护 + 字符串加密 + 环境检测"
        AppLanguage.ENGLISH -> "Basic + SO protection + String encryption + Env detection"
        AppLanguage.ARABIC -> "أساسي + حماية SO + تشفير النصوص + كشف البيئة"
    }

    val hardeningLevelAdvancedDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "标准 + VMP + 控制流混淆 + RASP + 内存保护"
        AppLanguage.ENGLISH -> "Standard + VMP + Control flow + RASP + Memory protection"
        AppLanguage.ARABIC -> "قياسي + VMP + تدفق التحكم + RASP + حماية الذاكرة"
    }

    val hardeningLevelFortressDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "全部开启，极致保护 · 理工男的浪漫"
        AppLanguage.ENGLISH -> "All enabled, ultimate protection"
        AppLanguage.ARABIC -> "تمكين الكل، حماية قصوى"
    }

    val dexProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "DEX 保护"
        AppLanguage.ENGLISH -> "DEX Protection"
        AppLanguage.ARABIC -> "حماية DEX"
    }

    val dexEncryption: String get() = when (lang) {
        AppLanguage.CHINESE -> "DEX 加密（壳保护）"
        AppLanguage.ENGLISH -> "DEX Encryption (Packing)"
        AppLanguage.ARABIC -> "تشفير DEX (التغليف)"
    }

    val dexEncryptionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "加密 classes.dex，运行时动态解密加载"
        AppLanguage.ENGLISH -> "Encrypt classes.dex, dynamically decrypt at runtime"
        AppLanguage.ARABIC -> "تشفير classes.dex، فك التشفير ديناميكياً في وقت التشغيل"
    }

    val dexSplitting: String get() = when (lang) {
        AppLanguage.CHINESE -> "DEX 分片加载"
        AppLanguage.ENGLISH -> "DEX Splitting"
        AppLanguage.ARABIC -> "تقسيم DEX"
    }

    val dexSplittingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "将 DEX 拆分为加密片段，按需动态重组"
        AppLanguage.ENGLISH -> "Split DEX into encrypted fragments, reassemble on demand"
        AppLanguage.ARABIC -> "تقسيم DEX إلى أجزاء مشفرة، إعادة التجميع عند الطلب"
    }

    val dexVmp: String get() = when (lang) {
        AppLanguage.CHINESE -> "VMP 虚拟机保护"
        AppLanguage.ENGLISH -> "VMP (Virtual Machine Protection)"
        AppLanguage.ARABIC -> "حماية الآلة الافتراضية VMP"
    }

    val dexVmpHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "将关键代码转为自定义指令集，每次打包随机化"
        AppLanguage.ENGLISH -> "Convert critical code to custom instruction set, randomized per build"
        AppLanguage.ARABIC -> "تحويل الكود الحرج إلى مجموعة تعليمات مخصصة"
    }

    val dexControlFlow: String get() = when (lang) {
        AppLanguage.CHINESE -> "控制流平坦化"
        AppLanguage.ENGLISH -> "Control Flow Flattening"
        AppLanguage.ARABIC -> "تسطيح تدفق التحكم"
    }

    val dexControlFlowHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "打乱代码执行流程，干扰静态分析"
        AppLanguage.ENGLISH -> "Scramble code execution flow, hinder static analysis"
        AppLanguage.ARABIC -> "تشويش تدفق تنفيذ الكود"
    }

    val soProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "Native SO 保护"
        AppLanguage.ENGLISH -> "Native SO Protection"
        AppLanguage.ARABIC -> "حماية Native SO"
    }

    val soEncryption: String get() = when (lang) {
        AppLanguage.CHINESE -> "SO Section 加密"
        AppLanguage.ENGLISH -> "SO Section Encryption"
        AppLanguage.ARABIC -> "تشفير قسم SO"
    }

    val soEncryptionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "加密 .text/.rodata 等关键代码段"
        AppLanguage.ENGLISH -> "Encrypt critical sections like .text/.rodata"
        AppLanguage.ARABIC -> "تشفير الأقسام الحرجة مثل .text/.rodata"
    }

    val soElfObfuscation: String get() = when (lang) {
        AppLanguage.CHINESE -> "ELF 头混淆"
        AppLanguage.ENGLISH -> "ELF Header Obfuscation"
        AppLanguage.ARABIC -> "تشويش رأس ELF"
    }

    val soElfObfuscationHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "干扰 IDA/Ghidra 等分析工具"
        AppLanguage.ENGLISH -> "Disrupt IDA/Ghidra analysis tools"
        AppLanguage.ARABIC -> "تعطيل أدوات تحليل IDA/Ghidra"
    }

    val soSymbolStrip: String get() = when (lang) {
        AppLanguage.CHINESE -> "符号剥离 + 假符号注入"
        AppLanguage.ENGLISH -> "Symbol Strip + Fake Symbol Injection"
        AppLanguage.ARABIC -> "إزالة الرموز + حقن رموز مزيفة"
    }

    val soSymbolStripHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "移除真实符号，注入蜜罐假符号误导分析"
        AppLanguage.ENGLISH -> "Remove real symbols, inject honeypot fake symbols"
        AppLanguage.ARABIC -> "إزالة الرموز الحقيقية، حقن رموز مزيفة"
    }

    val soAntiDump: String get() = when (lang) {
        AppLanguage.CHINESE -> "反内存 Dump"
        AppLanguage.ENGLISH -> "Anti-Memory Dump"
        AppLanguage.ARABIC -> "مكافحة تفريغ الذاكرة"
    }

    val soAntiDumpHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "mprotect + inotify 多层内存保护"
        AppLanguage.ENGLISH -> "Multi-layer memory protection with mprotect + inotify"
        AppLanguage.ARABIC -> "حماية ذاكرة متعددة الطبقات"
    }

    val antiReverse: String get() = when (lang) {
        AppLanguage.CHINESE -> "反逆向工程"
        AppLanguage.ENGLISH -> "Anti-Reverse Engineering"
        AppLanguage.ARABIC -> "مكافحة الهندسة العكسية"
    }

    val antiDebugMultiLayer: String get() = when (lang) {
        AppLanguage.CHINESE -> "多层反调试"
        AppLanguage.ENGLISH -> "Multi-Layer Anti-Debug"
        AppLanguage.ARABIC -> "مكافحة التصحيح متعددة الطبقات"
    }

    val antiDebugMultiLayerHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "ptrace/时序/信号/线程 四层检测"
        AppLanguage.ENGLISH -> "ptrace/timing/signal/thread 4-layer detection"
        AppLanguage.ARABIC -> "كشف 4 طبقات: ptrace/التوقيت/الإشارة/الخيط"
    }

    val antiFridaAdvanced: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级 Frida 检测"
        AppLanguage.ENGLISH -> "Advanced Frida Detection"
        AppLanguage.ARABIC -> "كشف Frida متقدم"
    }

    val antiFridaAdvancedHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "端口/内存/进程/线程名/特征码 五维检测"
        AppLanguage.ENGLISH -> "Port/memory/process/thread/signature 5D detection"
        AppLanguage.ARABIC -> "كشف 5 أبعاد: المنفذ/الذاكرة/العملية/الخيط/التوقيع"
    }

    val antiXposedDeep: String get() = when (lang) {
        AppLanguage.CHINESE -> "深度 Xposed/LSPosed 检测"
        AppLanguage.ENGLISH -> "Deep Xposed/LSPosed Detection"
        AppLanguage.ARABIC -> "كشف عميق لـ Xposed/LSPosed"
    }

    val antiXposedDeepHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "ART Hook 检测 + 堆栈帧分析"
        AppLanguage.ENGLISH -> "ART Hook detection + Stack frame analysis"
        AppLanguage.ARABIC -> "كشف ART Hook + تحليل إطار المكدس"
    }

    val antiMagiskDetect: String get() = when (lang) {
        AppLanguage.CHINESE -> "Magisk/Shamiko 检测"
        AppLanguage.ENGLISH -> "Magisk/Shamiko Detection"
        AppLanguage.ARABIC -> "كشف Magisk/Shamiko"
    }

    val antiMagiskDetectHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测 MagiskHide/Shamiko 隐藏行为"
        AppLanguage.ENGLISH -> "Detect MagiskHide/Shamiko cloaking"
        AppLanguage.ARABIC -> "كشف إخفاء MagiskHide/Shamiko"
    }

    val antiMemoryDump: String get() = when (lang) {
        AppLanguage.CHINESE -> "反内存 Dump"
        AppLanguage.ENGLISH -> "Anti-Memory Dump"
        AppLanguage.ARABIC -> "مكافحة تفريغ الذاكرة"
    }

    val antiMemoryDumpHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "防止运行时内存提取"
        AppLanguage.ENGLISH -> "Prevent runtime memory extraction"
        AppLanguage.ARABIC -> "منع استخراج الذاكرة في وقت التشغيل"
    }

    val antiScreenCapture: String get() = when (lang) {
        AppLanguage.CHINESE -> "反截屏/录屏"
        AppLanguage.ENGLISH -> "Anti-Screenshot/Recording"
        AppLanguage.ARABIC -> "مكافحة التقاط/تسجيل الشاشة"
    }

    val antiScreenCaptureHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "FLAG_SECURE 防止屏幕内容被捕获"
        AppLanguage.ENGLISH -> "FLAG_SECURE prevents screen content capture"
        AppLanguage.ARABIC -> "FLAG_SECURE يمنع التقاط محتوى الشاشة"
    }

    val environmentDetection: String get() = when (lang) {
        AppLanguage.CHINESE -> "环境检测"
        AppLanguage.ENGLISH -> "Environment Detection"
        AppLanguage.ARABIC -> "كشف البيئة"
    }

    val detectEmulatorAdvanced: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级模拟器检测"
        AppLanguage.ENGLISH -> "Advanced Emulator Detection"
        AppLanguage.ARABIC -> "كشف محاكي متقدم"
    }

    val detectEmulatorAdvancedHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "硬件指纹/传感器/温度 多维度检测"
        AppLanguage.ENGLISH -> "Hardware fingerprint/sensor/temperature multi-dim detection"
        AppLanguage.ARABIC -> "كشف متعدد الأبعاد"
    }

    val detectVirtualApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "虚拟化环境检测"
        AppLanguage.ENGLISH -> "Virtual Environment Detection"
        AppLanguage.ARABIC -> "كشف البيئة الافتراضية"
    }

    val detectVirtualAppHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测太极/VirtualXposed/分身/多开"
        AppLanguage.ENGLISH -> "Detect VirtualXposed/Parallel Space/Cloners"
        AppLanguage.ARABIC -> "كشف VirtualXposed/التطبيقات المتوازية"
    }

    val detectUSBDebugging: String get() = when (lang) {
        AppLanguage.CHINESE -> "USB 调试检测"
        AppLanguage.ENGLISH -> "USB Debugging Detection"
        AppLanguage.ARABIC -> "كشف تصحيح USB"
    }

    val detectUSBDebuggingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测 ADB 调试状态"
        AppLanguage.ENGLISH -> "Detect ADB debugging state"
        AppLanguage.ARABIC -> "كشف حالة تصحيح ADB"
    }

    val detectVPN: String get() = when (lang) {
        AppLanguage.CHINESE -> "VPN/代理检测"
        AppLanguage.ENGLISH -> "VPN/Proxy Detection"
        AppLanguage.ARABIC -> "كشف VPN/البروكسي"
    }

    val detectVPNHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测网络代理和 VPN 连接"
        AppLanguage.ENGLISH -> "Detect network proxy and VPN connections"
        AppLanguage.ARABIC -> "كشف اتصالات البروكسي و VPN"
    }

    val detectDeveloperOptions: String get() = when (lang) {
        AppLanguage.CHINESE -> "开发者选项检测"
        AppLanguage.ENGLISH -> "Developer Options Detection"
        AppLanguage.ARABIC -> "كشف خيارات المطور"
    }

    val detectDeveloperOptionsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测开发者选项是否已开启"
        AppLanguage.ENGLISH -> "Detect if developer options are enabled"
        AppLanguage.ARABIC -> "كشف ما إذا كانت خيارات المطور مفعلة"
    }

    val codeObfuscation: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码混淆"
        AppLanguage.ENGLISH -> "Code Obfuscation"
        AppLanguage.ARABIC -> "تشويش الكود"
    }

    val stringEncryption: String get() = when (lang) {
        AppLanguage.CHINESE -> "多层字符串加密"
        AppLanguage.ENGLISH -> "Multi-Layer String Encryption"
        AppLanguage.ARABIC -> "تشفير النصوص متعدد الطبقات"
    }

    val stringEncryptionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "AES + 自定义 Base64 + XOR 三层加密"
        AppLanguage.ENGLISH -> "AES + Custom Base64 + XOR 3-layer encryption"
        AppLanguage.ARABIC -> "تشفير 3 طبقات: AES + Base64 مخصص + XOR"
    }

    val classNameObfuscation: String get() = when (lang) {
        AppLanguage.CHINESE -> "类名混淆"
        AppLanguage.ENGLISH -> "Class Name Obfuscation"
        AppLanguage.ARABIC -> "تشويش أسماء الفئات"
    }

    val classNameObfuscationHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "Unicode 相似字符替换，增加分析压力"
        AppLanguage.ENGLISH -> "Unicode confusable character replacement"
        AppLanguage.ARABIC -> "استبدال أحرف Unicode المتشابهة"
    }

    val callIndirection: String get() = when (lang) {
        AppLanguage.CHINESE -> "方法调用间接化"
        AppLanguage.ENGLISH -> "Call Indirection"
        AppLanguage.ARABIC -> "التوجيه غير المباشر للاستدعاءات"
    }

    val callIndirectionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "反射 + 动态代理包装关键调用"
        AppLanguage.ENGLISH -> "Reflection + Dynamic proxy for critical calls"
        AppLanguage.ARABIC -> "الانعكاس + الوكيل الديناميكي للاستدعاءات الحرجة"
    }

    val opaquePredicates: String get() = when (lang) {
        AppLanguage.CHINESE -> "不透明谓词注入"
        AppLanguage.ENGLISH -> "Opaque Predicate Injection"
        AppLanguage.ARABIC -> "حقن المسندات المعتمة"
    }

    val opaquePredicatesHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "注入恒真/恒假条件 + 垃圾代码路径"
        AppLanguage.ENGLISH -> "Inject always-true/false conditions + junk code paths"
        AppLanguage.ARABIC -> "حقن شروط دائماً صحيحة/خاطئة + مسارات كود وهمية"
    }

    val raspProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行时自保护 (RASP)"
        AppLanguage.ENGLISH -> "Runtime Self-Protection (RASP)"
        AppLanguage.ARABIC -> "الحماية الذاتية في وقت التشغيل (RASP)"
    }

    val dexCrcVerify: String get() = when (lang) {
        AppLanguage.CHINESE -> "DEX CRC 自校验"
        AppLanguage.ENGLISH -> "DEX CRC Self-Verification"
        AppLanguage.ARABIC -> "التحقق الذاتي من DEX CRC"
    }

    val dexCrcVerifyHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行时校验 DEX 完整性，检测热修改"
        AppLanguage.ENGLISH -> "Verify DEX integrity at runtime, detect hot-patching"
        AppLanguage.ARABIC -> "التحقق من سلامة DEX في وقت التشغيل"
    }

    val memoryIntegrity: String get() = when (lang) {
        AppLanguage.CHINESE -> "内存完整性监控"
        AppLanguage.ENGLISH -> "Memory Integrity Monitoring"
        AppLanguage.ARABIC -> "مراقبة سلامة الذاكرة"
    }

    val memoryIntegrityHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "HMAC 标记关键数据，定期验证"
        AppLanguage.ENGLISH -> "HMAC tag critical data, periodic verification"
        AppLanguage.ARABIC -> "وسم HMAC للبيانات الحرجة، التحقق الدوري"
    }

    val jniCallValidation: String get() = when (lang) {
        AppLanguage.CHINESE -> "JNI 调用链验证"
        AppLanguage.ENGLISH -> "JNI Call Chain Validation"
        AppLanguage.ARABIC -> "التحقق من سلسلة استدعاءات JNI"
    }

    val jniCallValidationHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "验证 JNI 调用来源合法性"
        AppLanguage.ENGLISH -> "Validate JNI call origin legitimacy"
        AppLanguage.ARABIC -> "التحقق من شرعية مصدر استدعاء JNI"
    }

    val timingCheck: String get() = when (lang) {
        AppLanguage.CHINESE -> "时序检测"
        AppLanguage.ENGLISH -> "Timing Check"
        AppLanguage.ARABIC -> "فحص التوقيت"
    }

    val timingCheckHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "多时间源交叉验证，反加速/减速"
        AppLanguage.ENGLISH -> "Multi-source timing cross-validation, anti-speedhack"
        AppLanguage.ARABIC -> "التحقق المتقاطع من التوقيت"
    }

    val stackTraceFilter: String get() = when (lang) {
        AppLanguage.CHINESE -> "堆栈轨迹清洗"
        AppLanguage.ENGLISH -> "Stack Trace Sanitization"
        AppLanguage.ARABIC -> "تنظيف تتبع المكدس"
    }

    val stackTraceFilterHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "过滤内部实现细节，防止逻辑推断"
        AppLanguage.ENGLISH -> "Filter internal details, prevent logic inference"
        AppLanguage.ARABIC -> "تصفية التفاصيل الداخلية"
    }

    val antiTamper: String get() = when (lang) {
        AppLanguage.CHINESE -> "防篡改"
        AppLanguage.ENGLISH -> "Anti-Tampering"
        AppLanguage.ARABIC -> "مكافحة العبث"
    }

    val multiPointSignature: String get() = when (lang) {
        AppLanguage.CHINESE -> "多点签名验证"
        AppLanguage.ENGLISH -> "Multi-Point Signature Verification"
        AppLanguage.ARABIC -> "التحقق من التوقيع متعدد النقاط"
    }

    val multiPointSignatureHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "Java + Native + 延迟 三方交叉验证"
        AppLanguage.ENGLISH -> "Java + Native + Delayed 3-way cross-verification"
        AppLanguage.ARABIC -> "التحقق المتقاطع ثلاثي الاتجاهات"
    }

    val apkChecksum: String get() = when (lang) {
        AppLanguage.CHINESE -> "APK 校验和验证"
        AppLanguage.ENGLISH -> "APK Checksum Validation"
        AppLanguage.ARABIC -> "التحقق من المجموع الاختباري لـ APK"
    }

    val apkChecksumHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "DEX + 资源 + Manifest 完整性校验"
        AppLanguage.ENGLISH -> "DEX + Resources + Manifest integrity check"
        AppLanguage.ARABIC -> "فحص سلامة DEX + الموارد + Manifest"
    }

    val resourceIntegrity: String get() = when (lang) {
        AppLanguage.CHINESE -> "资源文件完整性"
        AppLanguage.ENGLISH -> "Resource File Integrity"
        AppLanguage.ARABIC -> "سلامة ملفات الموارد"
    }

    val resourceIntegrityHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "校验资源文件的 SHA-256 哈希"
        AppLanguage.ENGLISH -> "Verify SHA-256 hash of resource files"
        AppLanguage.ARABIC -> "التحقق من تجزئة SHA-256 لملفات الموارد"
    }

    val certificatePinning: String get() = when (lang) {
        AppLanguage.CHINESE -> "证书锁定"
        AppLanguage.ENGLISH -> "Certificate Pinning"
        AppLanguage.ARABIC -> "تثبيت الشهادة"
    }

    val certificatePinningHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "防止中间人攻击，锁定服务器证书"
        AppLanguage.ENGLISH -> "Prevent MITM attacks, pin server certificates"
        AppLanguage.ARABIC -> "منع هجمات الوسيط، تثبيت شهادات الخادم"
    }

    val threatResponse: String get() = when (lang) {
        AppLanguage.CHINESE -> "威胁响应策略"
        AppLanguage.ENGLISH -> "Threat Response Strategy"
        AppLanguage.ARABIC -> "استراتيجية الاستجابة للتهديدات"
    }

    val threatResponseLogOnly: String get() = when (lang) {
        AppLanguage.CHINESE -> "仅记录日志"
        AppLanguage.ENGLISH -> "Log Only"
        AppLanguage.ARABIC -> "تسجيل فقط"
    }

    val threatResponseSilentExit: String get() = when (lang) {
        AppLanguage.CHINESE -> "静默退出"
        AppLanguage.ENGLISH -> "Silent Exit"
        AppLanguage.ARABIC -> "خروج صامت"
    }

    val threatResponseCrashRandom: String get() = when (lang) {
        AppLanguage.CHINESE -> "随机崩溃"
        AppLanguage.ENGLISH -> "Random Crash"
        AppLanguage.ARABIC -> "انهيار عشوائي"
    }

    val threatResponseDataWipe: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据擦除"
        AppLanguage.ENGLISH -> "Data Wipe"
        AppLanguage.ARABIC -> "مسح البيانات"
    }

    val threatResponseFakeData: String get() = when (lang) {
        AppLanguage.CHINESE -> "假数据注入"
        AppLanguage.ENGLISH -> "Fake Data Injection"
        AppLanguage.ARABIC -> "حقن بيانات مزيفة"
    }

    val responseDelay: String get() = when (lang) {
        AppLanguage.CHINESE -> "响应延迟"
        AppLanguage.ENGLISH -> "Response Delay"
        AppLanguage.ARABIC -> "تأخير الاستجابة"
    }

    val responseDelayHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "延迟响应更隐蔽（秒）"
        AppLanguage.ENGLISH -> "Delayed response is more stealthy (seconds)"
        AppLanguage.ARABIC -> "الاستجابة المتأخرة أكثر تخفياً (ثوانٍ)"
    }

    val enableHoneypot: String get() = when (lang) {
        AppLanguage.CHINESE -> "蜜罐陷阱"
        AppLanguage.ENGLISH -> "Honeypot Trap"
        AppLanguage.ARABIC -> "فخ مصيدة العسل"
    }

    val enableHoneypotHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "注入假数据迷惑逆向工程师"
        AppLanguage.ENGLISH -> "Inject fake data to mislead reverse engineers"
        AppLanguage.ARABIC -> "حقن بيانات مزيفة لتضليل المهندسين العكسيين"
    }

    val enableSelfDestruct: String get() = when (lang) {
        AppLanguage.CHINESE -> "自毁机制"
        AppLanguage.ENGLISH -> "Self-Destruct"
        AppLanguage.ARABIC -> "التدمير الذاتي"
    }

    val enableSelfDestructHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "严重威胁时清除所有敏感数据"
        AppLanguage.ENGLISH -> "Wipe all sensitive data under severe threats"
        AppLanguage.ARABIC -> "مسح جميع البيانات الحساسة تحت تهديدات شديدة"
    }

    val hardeningProtectionLayers: String get() = when (lang) {
        AppLanguage.CHINESE -> "保护层数"
        AppLanguage.ENGLISH -> "Protection Layers"
        AppLanguage.ARABIC -> "طبقات الحماية"
    }
    // ==================== Shields Enum i18n ====================
    val shieldsCookieAllowAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "允许所有 Cookie"
        AppLanguage.ENGLISH -> "Allow all cookies"
        AppLanguage.ARABIC -> "السماح بجميع ملفات تعريف الارتباط"
    }

    val shieldsCookieBlockCrossSite: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止跨站 Cookie"
        AppLanguage.ENGLISH -> "Block cross-site cookies"
        AppLanguage.ARABIC -> "حظر ملفات تعريف الارتباط عبر المواقع"
    }

    val shieldsCookieBlockAllThirdParty: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止所有第三方 Cookie"
        AppLanguage.ENGLISH -> "Block all third-party cookies"
        AppLanguage.ARABIC -> "حظر جميع ملفات تعريف الارتباط للجهات الخارجية"
    }

    val shieldsRefNoReferrer: String get() = when (lang) {
        AppLanguage.CHINESE -> "不发送 Referrer"
        AppLanguage.ENGLISH -> "No referrer"
        AppLanguage.ARABIC -> "عدم إرسال المُحيل"
    }

    val shieldsRefOrigin: String get() = when (lang) {
        AppLanguage.CHINESE -> "仅发送域名"
        AppLanguage.ENGLISH -> "Origin only"
        AppLanguage.ARABIC -> "إرسال النطاق فقط"
    }

    val shieldsRefStrictOriginCross: String get() = when (lang) {
        AppLanguage.CHINESE -> "跨域时仅发送域名"
        AppLanguage.ENGLISH -> "Strict origin (cross-origin)"
        AppLanguage.ARABIC -> "نطاق صارم (عبر المواقع)"
    }

    val shieldsRefSameOrigin: String get() = when (lang) {
        AppLanguage.CHINESE -> "仅同源发送"
        AppLanguage.ENGLISH -> "Same origin only"
        AppLanguage.ARABIC -> "نفس الأصل فقط"
    }

    val shieldsRefUnsafeUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "始终发送完整 URL"
        AppLanguage.ENGLISH -> "Always send full URL"
        AppLanguage.ARABIC -> "إرسال عنوان URL الكامل دائماً"
    }

    val shieldsTrackerAnalytics: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据分析"
        AppLanguage.ENGLISH -> "Analytics"
        AppLanguage.ARABIC -> "التحليلات"
    }

    val shieldsTrackerSocial: String get() = when (lang) {
        AppLanguage.CHINESE -> "社交追踪"
        AppLanguage.ENGLISH -> "Social tracking"
        AppLanguage.ARABIC -> "التتبع الاجتماعي"
    }

    val shieldsTrackerFingerprinting: String get() = when (lang) {
        AppLanguage.CHINESE -> "指纹采集"
        AppLanguage.ENGLISH -> "Fingerprinting"
        AppLanguage.ARABIC -> "بصمة الجهاز"
    }

    val shieldsTrackerCryptomining: String get() = when (lang) {
        AppLanguage.CHINESE -> "加密挖矿"
        AppLanguage.ENGLISH -> "Cryptomining"
        AppLanguage.ARABIC -> "تعدين العملات المشفرة"
    }

    val shieldsTrackerAdNetwork: String get() = when (lang) {
        AppLanguage.CHINESE -> "广告网络"
        AppLanguage.ENGLISH -> "Ad network"
        AppLanguage.ARABIC -> "شبكة الإعلانات"
    }
    // ==================== Auto-start Settings ====================
    val autoStartSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "自启动设置"
        AppLanguage.ENGLISH -> "Auto Start Settings"
        AppLanguage.ARABIC -> "إعدادات التشغيل التلقائي"
    }

    val autoStartDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "开机或定时自动启动应用"
        AppLanguage.ENGLISH -> "Auto start app on boot or scheduled time"
        AppLanguage.ARABIC -> "تشغيل التطبيق تلقائيًا عند الإقلاع أو في وقت مجدول"
    }

    val configured: String get() = when (lang) {
        AppLanguage.CHINESE -> "已配置"
        AppLanguage.ENGLISH -> "Configured"
        AppLanguage.ARABIC -> "تم التكوين"
    }

    val bootAutoStart: String get() = when (lang) {
        AppLanguage.CHINESE -> "开机自启动"
        AppLanguage.ENGLISH -> "Boot Auto Start"
        AppLanguage.ARABIC -> "التشغيل التلقائي عند الإقلاع"
    }

    val bootAutoStartHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "设备开机后自动启动此应用"
        AppLanguage.ENGLISH -> "Auto start this app after device boots"
        AppLanguage.ARABIC -> "تشغيل هذا التطبيق تلقائيًا بعد إقلاع الجهاز"
    }

    val scheduledAutoStart: String get() = when (lang) {
        AppLanguage.CHINESE -> "定时自启动"
        AppLanguage.ENGLISH -> "Scheduled Auto Start"
        AppLanguage.ARABIC -> "التشغيل التلقائي المجدول"
    }

    val scheduledAutoStartHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "在指定时间自动启动此应用"
        AppLanguage.ENGLISH -> "Auto start this app at specified time"
        AppLanguage.ARABIC -> "تشغيل هذا التطبيق تلقائيًا في الوقت المحدد"
    }

    val launchDate: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动日期"
        AppLanguage.ENGLISH -> "Launch Date"
        AppLanguage.ARABIC -> "تاريخ التشغيل"
    }

    val autoStartNote: String get() = when (lang) {
        AppLanguage.CHINESE -> "自启动功能仅在导出的 APK 中生效。部分手机需要在系统设置中授予自启动权限。"
        AppLanguage.ENGLISH -> "Auto start only works in exported APK. Some phones require granting auto start permission in system settings."
        AppLanguage.ARABIC -> "يعمل التشغيل التلقائي فقط في APK المُصدَّر. تتطلب بعض الهواتف منح إذن التشغيل التلقائي في إعدادات النظام."
    }

    val today: String get() = when (lang) {
        AppLanguage.CHINESE -> "今天"
        AppLanguage.ENGLISH -> "Today"
        AppLanguage.ARABIC -> "اليوم"
    }

    val tomorrow: String get() = when (lang) {
        AppLanguage.CHINESE -> "明天"
        AppLanguage.ENGLISH -> "Tomorrow"
        AppLanguage.ARABIC -> "غداً"
    }

    val nextLaunchTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "下次启动"
        AppLanguage.ENGLISH -> "Next Launch"
        AppLanguage.ARABIC -> "التشغيل التالي"
    }

    val exactAlarmPermissionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "⚠ 需要精确闹钟权限才能准时启动，点击前往设置"
        AppLanguage.ENGLISH -> "⚠ Exact alarm permission required for on-time launch, tap to open settings"
        AppLanguage.ARABIC -> "⚠ يلزم إذن المنبه الدقيق للتشغيل في الوقت المحدد، انقر لفتح الإعدادات"
    }

    val batteryOptimizationHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "🔋 建议关闭电池优化以保证自启动可靠触发，点击前往设置"
        AppLanguage.ENGLISH -> "🔋 Disable battery optimization for reliable auto start, tap to open settings"
        AppLanguage.ARABIC -> "🔋 قم بتعطيل تحسين البطارية لضمان التشغيل التلقائي الموثوق، انقر لفتح الإعدادات"
    }

    val bootDelay: String get() = when (lang) {
        AppLanguage.CHINESE -> "开机延迟"
        AppLanguage.ENGLISH -> "Boot Delay"
        AppLanguage.ARABIC -> "تأخير التشغيل"
    }

    val oemAutoStartHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "📱 检测到 %s 系统，请在手机管家中将本应用加入自启动白名单，否则系统可能阻止自启动"
        AppLanguage.ENGLISH -> "📱 %s system detected. Please add this app to auto-start whitelist in phone manager to prevent system from blocking auto start"
        AppLanguage.ARABIC -> "📱 تم اكتشاف نظام %s. يرجى إضافة هذا التطبيق إلى القائمة البيضاء للتشغيل التلقائي في مدير الهاتف"
    }

    val autoStartPermissionReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "✅ 所有权限已就绪，自启动功能将正常工作"
        AppLanguage.ENGLISH -> "✅ All permissions ready, auto start will work properly"
        AppLanguage.ARABIC -> "✅ جميع الأذونات جاهزة، سيعمل التشغيل التلقائي بشكل صحيح"
    }
    // ==================== Hosts Ad Blocking ====================
    val hostsAdBlock: String get() = when (lang) {
        AppLanguage.CHINESE -> "Hosts 广告拦截"
        AppLanguage.ENGLISH -> "Hosts Ad Blocking"
        AppLanguage.ARABIC -> "حظر الإعلانات عبر Hosts"
    }

    val hostsAdBlockSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 hosts 文件拦截广告域名"
        AppLanguage.ENGLISH -> "Block ad domains using hosts files"
        AppLanguage.ARABIC -> "حظر نطاقات الإعلانات باستخدام ملفات hosts"
    }

    val hostsRulesCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 条 hosts 规则"
        AppLanguage.ENGLISH -> "%d hosts rules"
        AppLanguage.ARABIC -> "%d قواعد hosts"
    }

    val noHostsRules: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无 hosts 规则"
        AppLanguage.ENGLISH -> "No hosts rules"
        AppLanguage.ARABIC -> "لا توجد قواعد hosts"
    }

    val importFromUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "从 URL 导入"
        AppLanguage.ENGLISH -> "Import from URL"
        AppLanguage.ARABIC -> "استيراد من URL"
    }

    val popularHostsSources: String get() = when (lang) {
        AppLanguage.CHINESE -> "常用 Hosts 源"
        AppLanguage.ENGLISH -> "Popular Hosts Sources"
        AppLanguage.ARABIC -> "مصادر Hosts الشائعة"
    }

    val hostsSourceAdded: String get() = when (lang) {
        AppLanguage.CHINESE -> "已添加"
        AppLanguage.ENGLISH -> "Added"
        AppLanguage.ARABIC -> "تمت الإضافة"
    }

    val importHostsUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "Hosts 文件 URL"
        AppLanguage.ENGLISH -> "Hosts File URL"
        AppLanguage.ARABIC -> "رابط ملف Hosts"
    }

    val importHostsUrlHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入 hosts 文件的 URL 地址"
        AppLanguage.ENGLISH -> "Enter URL of hosts file"
        AppLanguage.ARABIC -> "أدخل رابط ملف hosts"
    }

    val importingHosts: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在导入..."
        AppLanguage.ENGLISH -> "Importing..."
        AppLanguage.ARABIC -> "جاري الاستيراد..."
    }

    val importHostsSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "成功导入 %d 条规则"
        AppLanguage.ENGLISH -> "Successfully imported %d rules"
        AppLanguage.ARABIC -> "تم استيراد %d قاعدة بنجاح"
    }

    val importHostsFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入失败"
        AppLanguage.ENGLISH -> "Import failed"
        AppLanguage.ARABIC -> "فشل الاستيراد"
    }

    val clearHostsRules: String get() = when (lang) {
        AppLanguage.CHINESE -> "清空所有 Hosts 规则"
        AppLanguage.ENGLISH -> "Clear All Hosts Rules"
        AppLanguage.ARABIC -> "مسح جميع قواعد Hosts"
    }

    val clearHostsConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定要清空所有 hosts 规则吗？"
        AppLanguage.ENGLISH -> "Are you sure you want to clear all hosts rules?"
        AppLanguage.ARABIC -> "هل أنت متأكد من مسح جميع قواعد hosts؟"
    }

    val hostsCleared: String get() = when (lang) {
        AppLanguage.CHINESE -> "Hosts 规则已清空"
        AppLanguage.ENGLISH -> "Hosts rules cleared"
        AppLanguage.ARABIC -> "تم مسح قواعد Hosts"
    }

    val enabledSources: String get() = when (lang) {
        AppLanguage.CHINESE -> "已启用的源"
        AppLanguage.ENGLISH -> "Enabled Sources"
        AppLanguage.ARABIC -> "المصادر المفعلة"
    }

    val hostsBlockingDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "通过导入 hosts 文件来拦截广告域名\n支持标准 hosts 格式和 AdBlock 格式"
        AppLanguage.ENGLISH -> "Block ad domains by importing hosts files\nSupports standard hosts format and AdBlock format"
        AppLanguage.ARABIC -> "حظر نطاقات الإعلانات عن طريق استيراد ملفات hosts\nيدعم تنسيق hosts القياسي وتنسيق AdBlock"
    }

    val downloadAndImport: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载并导入"
        AppLanguage.ENGLISH -> "Download & Import"
        AppLanguage.ARABIC -> "تنزيل واستيراد"
    }
    // ==================== Spoofing (Device Disguise) ====================
    val deviceTypePhone: String get() = when (lang) {
        AppLanguage.CHINESE -> "手机"
        AppLanguage.ENGLISH -> "Phone"
        AppLanguage.ARABIC -> "هاتف"
    }

    val deviceTypeTablet: String get() = when (lang) {
        AppLanguage.CHINESE -> "平板"
        AppLanguage.ENGLISH -> "Tablet"
        AppLanguage.ARABIC -> "جهاز لوحي"
    }

    val deviceTypeDesktop: String get() = when (lang) {
        AppLanguage.CHINESE -> "桌面"
        AppLanguage.ENGLISH -> "Desktop"
        AppLanguage.ARABIC -> "سطح المكتب"
    }

    val deviceTypeLaptop: String get() = when (lang) {
        AppLanguage.CHINESE -> "笔记本"
        AppLanguage.ENGLISH -> "Laptop"
        AppLanguage.ARABIC -> "حاسوب محمول"
    }

    val deviceTypeWatch: String get() = when (lang) {
        AppLanguage.CHINESE -> "手表"
        AppLanguage.ENGLISH -> "Watch"
        AppLanguage.ARABIC -> "ساعة"
    }

    val deviceTypeTV: String get() = when (lang) {
        AppLanguage.CHINESE -> "电视"
        AppLanguage.ENGLISH -> "TV"
        AppLanguage.ARABIC -> "تلفزيون"
    }

    val deviceQuickSelect: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速选择"
        AppLanguage.ENGLISH -> "Quick Select"
        AppLanguage.ARABIC -> "اختيار سريع"
    }

    val devicePopularPresets: String get() = when (lang) {
        AppLanguage.CHINESE -> "热门设备"
        AppLanguage.ENGLISH -> "Popular Devices"
        AppLanguage.ARABIC -> "الأجهزة الشائعة"
    }

    val deviceCurrentDisguise: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前伪装"
        AppLanguage.ENGLISH -> "Current Disguise"
        AppLanguage.ARABIC -> "التمويه الحالي"
    }

    val deviceCustomUA: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义 User-Agent"
        AppLanguage.ENGLISH -> "Custom User-Agent"
        AppLanguage.ARABIC -> "User-Agent مخصص"
    }

    val deviceCustomUAHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入完整的 User-Agent 字符串（覆盖自动生成）"
        AppLanguage.ENGLISH -> "Enter full User-Agent string (overrides auto-generated)"
        AppLanguage.ARABIC -> "أدخل سلسلة User-Agent الكاملة (يتجاوز التوليد التلقائي)"
    }

    val deviceGeneratedUA: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动生成的 User-Agent"
        AppLanguage.ENGLISH -> "Auto-generated User-Agent"
        AppLanguage.ARABIC -> "User-Agent تم إنشاؤه تلقائيًا"
    }

    val deviceDesktopViewport: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制桌面视口"
        AppLanguage.ENGLISH -> "Force Desktop Viewport"
        AppLanguage.ARABIC -> "فرض عرض سطح المكتب"
    }

    val deviceDesktopViewportHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用桌面宽度加载网页"
        AppLanguage.ENGLISH -> "Load pages with desktop width"
        AppLanguage.ARABIC -> "تحميل الصفحات بعرض سطح المكتب"
    }

    val deviceCustomDevice: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义设备"
        AppLanguage.ENGLISH -> "Custom Device"
        AppLanguage.ARABIC -> "جهاز مخصص"
    }

    val deviceCustomDeviceHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "手动配置设备名称、型号和分辨率"
        AppLanguage.ENGLISH -> "Manually configure device name, model and resolution"
        AppLanguage.ARABIC -> "تكوين اسم الجهاز والطراز والدقة يدويًا"
    }

    val deviceCustomName: String get() = when (lang) {
        AppLanguage.CHINESE -> "设备名称"
        AppLanguage.ENGLISH -> "Device Name"
        AppLanguage.ARABIC -> "اسم الجهاز"
    }

    val deviceCustomModelId: String get() = when (lang) {
        AppLanguage.CHINESE -> "设备型号标识"
        AppLanguage.ENGLISH -> "Model Identifier"
        AppLanguage.ARABIC -> "معرف الطراز"
    }

    val deviceCustomWidth: String get() = when (lang) {
        AppLanguage.CHINESE -> "宽度 px"
        AppLanguage.ENGLISH -> "Width px"
        AppLanguage.ARABIC -> "العرض px"
    }

    val deviceCustomHeight: String get() = when (lang) {
        AppLanguage.CHINESE -> "高度 px"
        AppLanguage.ENGLISH -> "Height px"
        AppLanguage.ARABIC -> "الارتفاع px"
    }

    val deviceCustomDensity: String get() = when (lang) {
        AppLanguage.CHINESE -> "像素密度 (DPR)"
        AppLanguage.ENGLISH -> "Pixel Density (DPR)"
        AppLanguage.ARABIC -> "كثافة البكسل (DPR)"
    }

    val deviceCustomApply: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用自定义配置"
        AppLanguage.ENGLISH -> "Apply Custom Config"
        AppLanguage.ARABIC -> "تطبيق التكوين المخصص"
    }
    // ==================== v2.0 — Special Mode ====================
    val specialModes: String get() = when (lang) {
        AppLanguage.CHINESE -> "特殊模式"
        AppLanguage.ENGLISH -> "Special Modes"
        AppLanguage.ARABIC -> "الأوضاع الخاصة"
    }

    val nuclearMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "💣 核弹模式"
        AppLanguage.ENGLISH -> "💣 Nuclear Mode"
        AppLanguage.ARABIC -> "💣 الوضع النووي"
    }

    val nuclearModeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "一键全开: 最大音量 + 最大震动 + 爆闪 + 满载性能 + 屏蔽按键"
        AppLanguage.ENGLISH -> "All-in-one: Max volume + Max vibration + Strobe + Full performance + Block keys"
        AppLanguage.ARABIC -> "الكل في واحد: أقصى صوت + أقصى اهتزاز + وميض + أقصى أداء + حظر المفاتيح"
    }

    val stealthMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "🥷 隐身模式"
        AppLanguage.ENGLISH -> "🥷 Stealth Mode"
        AppLanguage.ARABIC -> "🥷 وضع التخفي"
    }

    val stealthModeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "一键隐身: 静音 + 黑屏 + 屏蔽触摸 + 断网 + 断蓝牙"
        AppLanguage.ENGLISH -> "All-hidden: Mute + Black screen + Block touch + Disable WiFi + Disable BT"
        AppLanguage.ARABIC -> "إخفاء كامل: صامت + شاشة سوداء + حظر اللمس + قطع WiFi + قطع البلوتوث"
    }

    val customAlarm: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义警报"
        AppLanguage.ENGLISH -> "Custom Alarm"
        AppLanguage.ARABIC -> "إنذار مخصص"
    }

    val customAlarmDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义闪光灯+震动节奏序列"
        AppLanguage.ENGLISH -> "Custom flashlight + vibration rhythm sequence"
        AppLanguage.ARABIC -> "تسلسل إيقاع الفلاش + الاهتزاز المخصص"
    }

    val customAlarmPattern: String get() = when (lang) {
        AppLanguage.CHINESE -> "节奏序列"
        AppLanguage.ENGLISH -> "Rhythm Pattern"
        AppLanguage.ARABIC -> "نمط الإيقاع"
    }

    val customAlarmPatternHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "格式: 亮ms,灭ms,亮ms,灭ms...\n例如: 100,100,100,100,100,800 = 三连快闪+长暗"
        AppLanguage.ENGLISH -> "Format: on_ms,off_ms,on_ms,off_ms...\nExample: 100,100,100,100,100,800 = triple flash + long dark"
        AppLanguage.ARABIC -> "التنسيق: تشغيل_مللي,إيقاف_مللي...\nمثال: 100,100,100,100,100,800 = وميض ثلاثي + ظلام طويل"
    }

    val customAlarmVibSync: String get() = when (lang) {
        AppLanguage.CHINESE -> "震动同步"
        AppLanguage.ENGLISH -> "Vibration Sync"
        AppLanguage.ARABIC -> "مزامنة الاهتزاز"
    }

    val customAlarmVibSyncDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "震动与闪光灯节奏同步"
        AppLanguage.ENGLISH -> "Vibration syncs with flashlight rhythm"
        AppLanguage.ARABIC -> "مزامنة الاهتزاز مع إيقاع الفلاش"
    }

    val forceScreenAwake: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制屏幕常亮"
        AppLanguage.ENGLISH -> "Force Screen Awake"
        AppLanguage.ARABIC -> "فرض بقاء الشاشة مضيئة"
    }

    val forceScreenAwakeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止屏幕自动关闭"
        AppLanguage.ENGLISH -> "Prevent screen from turning off automatically"
        AppLanguage.ARABIC -> "منع الشاشة من الإغلاق التلقائي"
    }

    val deviceCapability: String get() = when (lang) {
        AppLanguage.CHINESE -> "设备能力检测"
        AppLanguage.ENGLISH -> "Device Capability Check"
        AppLanguage.ARABIC -> "فحص إمكانيات الجهاز"
    }

    val deviceCapabilityDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测当前设备支持的黑科技功能"
        AppLanguage.ENGLISH -> "Detect which black tech features this device supports"
        AppLanguage.ARABIC -> "اكتشاف ميزات التقنية المتقدمة التي يدعمها هذا الجهاز"
    }

    val runDeviceCheck: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行检测"
        AppLanguage.ENGLISH -> "Run Check"
        AppLanguage.ARABIC -> "تشغيل الفحص"
    }
    // ==================== User Scripts ====================
    val userScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "User script"
        AppLanguage.ENGLISH -> "User Scripts"
        AppLanguage.ARABIC -> "سكريبتات المستخدم"
    }

    val userScriptsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "类似油猴脚本，注入自定义 JavaScript 代码"
        AppLanguage.ENGLISH -> "Tampermonkey-like custom JavaScript injection"
        AppLanguage.ARABIC -> "حقن كود JavaScript مخصص مثل Tampermonkey"
    }

    val addScript: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加脚本"
        AppLanguage.ENGLISH -> "Add Script"
        AppLanguage.ARABIC -> "إضافة سكريبت"
    }

    val editScript: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑脚本"
        AppLanguage.ENGLISH -> "Edit Script"
        AppLanguage.ARABIC -> "تعديل السكريبت"
    }

    val scriptName: String get() = when (lang) {
        AppLanguage.CHINESE -> "脚本名称"
        AppLanguage.ENGLISH -> "Script Name"
        AppLanguage.ARABIC -> "اسم السكريبت"
    }

    val scriptNamePlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入脚本名称"
        AppLanguage.ENGLISH -> "Enter script name"
        AppLanguage.ARABIC -> "أدخل اسم السكريبت"
    }

    val scriptCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "脚本代码"
        AppLanguage.ENGLISH -> "Script Code"
        AppLanguage.ARABIC -> "كود السكريبت"
    }

    val scriptCodePlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入 JavaScript 代码"
        AppLanguage.ENGLISH -> "Enter JavaScript code"
        AppLanguage.ARABIC -> "أدخل كود JavaScript"
    }

    val scriptRunAt: String get() = when (lang) {
        AppLanguage.CHINESE -> "Run at"
        AppLanguage.ENGLISH -> "Run At"
        AppLanguage.ARABIC -> "وقت التشغيل"
    }

    val scriptEnabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用脚本"
        AppLanguage.ENGLISH -> "Enable Script"
        AppLanguage.ARABIC -> "تفعيل السكريبت"
    }

    val noScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无脚本，点击添加"
        AppLanguage.ENGLISH -> "No scripts, click to add"
        AppLanguage.ARABIC -> "لا توجد سكريبتات، انقر للإضافة"
    }

    val scriptNameRequired: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入脚本名称"
        AppLanguage.ENGLISH -> "Please enter script name"
        AppLanguage.ARABIC -> "الرجاء إدخال اسم السكريبت"
    }

    val scriptCodeRequired: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入脚本代码"
        AppLanguage.ENGLISH -> "Please enter script code"
        AppLanguage.ARABIC -> "الرجاء إدخال كود السكريبت"
    }

    val scriptCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个脚本"
        AppLanguage.ENGLISH -> "%d scripts"
        AppLanguage.ARABIC -> "%d سكريبتات"
    }

    val scriptImportFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入 JS 文件"
        AppLanguage.ENGLISH -> "Import JS File"
        AppLanguage.ARABIC -> "استيراد ملف JS"
    }

    val scriptFileLoaded: String get() = when (lang) {
        AppLanguage.CHINESE -> "已导入文件 · %d 行 · %s"
        AppLanguage.ENGLISH -> "File imported · %d lines · %s"
        AppLanguage.ARABIC -> "تم استيراد الملف · %d سطر · %s"
    }

    val scriptClearCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "清除代码"
        AppLanguage.ENGLISH -> "Clear Code"
        AppLanguage.ARABIC -> "مسح الكود"
    }
    // ==================== Long-Press Menu ====================
    val longPressMenuSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "长按菜单"
        AppLanguage.ENGLISH -> "Long Press Menu"
        AppLanguage.ARABIC -> "قائمة الضغط المطول"
    }

    val longPressMenuSettingsDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "长按网页内容时显示的操作菜单"
        AppLanguage.ENGLISH -> "Action menu when long pressing web content"
        AppLanguage.ARABIC -> "قائمة الإجراءات عند الضغط المطول على محتوى الويب"
    }

    val longPressMenuStyleLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "菜单样式"
        AppLanguage.ENGLISH -> "Menu Style"
        AppLanguage.ARABIC -> "نمط القائمة"
    }

    val longPressMenuStyleDisabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "禁用"
        AppLanguage.ENGLISH -> "Disabled"
        AppLanguage.ARABIC -> "معطل"
    }

    val longPressMenuStyleDisabledDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "不显示长按菜单，使用系统默认行为"
        AppLanguage.ENGLISH -> "No long press menu, use system default"
        AppLanguage.ARABIC -> "لا توجد قائمة ضغط مطول، استخدم الافتراضي"
    }

    val longPressMenuStyleSimple: String get() = when (lang) {
        AppLanguage.CHINESE -> "简洁"
        AppLanguage.ENGLISH -> "Simple"
        AppLanguage.ARABIC -> "بسيط"
    }

    val longPressMenuStyleSimpleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "仅显示保存图片和复制链接"
        AppLanguage.ENGLISH -> "Only save image and copy link"
        AppLanguage.ARABIC -> "حفظ الصورة ونسخ الرابط فقط"
    }

    val longPressMenuStyleFull: String get() = when (lang) {
        AppLanguage.CHINESE -> "完整"
        AppLanguage.ENGLISH -> "Full"
        AppLanguage.ARABIC -> "كامل"
    }

    val longPressMenuStyleFullDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示所有可用操作"
        AppLanguage.ENGLISH -> "Show all available actions"
        AppLanguage.ARABIC -> "عرض جميع الإجراءات المتاحة"
    }

    val longPressMenuStyleIos: String get() = when (lang) {
        AppLanguage.CHINESE -> "iOS 风格"
        AppLanguage.ENGLISH -> "iOS Style"
        AppLanguage.ARABIC -> "نمط iOS"
    }

    val longPressMenuStyleIosDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "毛玻璃背景，类似 iPhone 体验"
        AppLanguage.ENGLISH -> "Frosted glass background, iPhone-like"
        AppLanguage.ARABIC -> "خلفية زجاجية مصقولة، مثل iPhone"
    }

    val longPressMenuStyleFloating: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮气泡"
        AppLanguage.ENGLISH -> "Floating Bubble"
        AppLanguage.ARABIC -> "فقاعة عائمة"
    }

    val longPressMenuStyleFloatingDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "在点击位置显示圆形气泡菜单"
        AppLanguage.ENGLISH -> "Circular bubble menu at tap position"
        AppLanguage.ARABIC -> "قائمة فقاعات دائرية في موضع النقر"
    }

    val longPressMenuStyleContext: String get() = when (lang) {
        AppLanguage.CHINESE -> "右键菜单"
        AppLanguage.ENGLISH -> "Context Menu"
        AppLanguage.ARABIC -> "قائمة السياق"
    }

    val longPressMenuStyleContextDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "类似桌面端右键菜单，紧凑高效"
        AppLanguage.ENGLISH -> "Desktop-like right-click menu, compact"
        AppLanguage.ARABIC -> "قائمة نقر يمين مثل سطح المكتب، مضغوطة"
    }

    val longPressMenuPreview: String get() = when (lang) {
        AppLanguage.CHINESE -> "样式预览"
        AppLanguage.ENGLISH -> "Style Preview"
        AppLanguage.ARABIC -> "معاينة النمط"
    }
    // ==================== v2.0 — Network Control ====================
    val networkControl: String get() = when (lang) {
        AppLanguage.CHINESE -> "网络控制"
        AppLanguage.ENGLISH -> "Network Control"
        AppLanguage.ARABIC -> "التحكم في الشبكة"
    }

    val forceWifiHotspot: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制开启 WiFi 热点"
        AppLanguage.ENGLISH -> "Force WiFi Hotspot"
        AppLanguage.ARABIC -> "فرض نقطة اتصال WiFi"
    }

    val forceWifiHotspotDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "编程化创建 WiFi 热点，自动适配 Android 版本"
        AppLanguage.ENGLISH -> "Programmatically create WiFi hotspot, auto-adapts to Android version"
        AppLanguage.ARABIC -> "إنشاء نقطة اتصال WiFi برمجياً، يتكيف تلقائياً مع إصدار أندرويد"
    }

    val hotspotSsid: String get() = when (lang) {
        AppLanguage.CHINESE -> "热点名称 (SSID)"
        AppLanguage.ENGLISH -> "Hotspot Name (SSID)"
        AppLanguage.ARABIC -> "اسم نقطة الاتصال (SSID)"
    }

    val hotspotPassword: String get() = when (lang) {
        AppLanguage.CHINESE -> "热点密码"
        AppLanguage.ENGLISH -> "Hotspot Password"
        AppLanguage.ARABIC -> "كلمة مرور نقطة الاتصال"
    }

    val hotspotPasswordHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "至少 8 位字符"
        AppLanguage.ENGLISH -> "At least 8 characters"
        AppLanguage.ARABIC -> "8 أحرف على الأقل"
    }

    val forceDisableWifi: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制关闭 WiFi"
        AppLanguage.ENGLISH -> "Force Disable WiFi"
        AppLanguage.ARABIC -> "فرض إيقاف WiFi"
    }

    val forceDisableWifiDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "断开 WiFi 连接并禁止重新连接"
        AppLanguage.ENGLISH -> "Disconnect WiFi and prevent reconnection"
        AppLanguage.ARABIC -> "قطع اتصال WiFi ومنع إعادة الاتصال"
    }

    val forceDisableBluetooth: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制关闭蓝牙"
        AppLanguage.ENGLISH -> "Force Disable Bluetooth"
        AppLanguage.ARABIC -> "فرض إيقاف البلوتوث"
    }

    val forceDisableBluetoothDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "断开所有蓝牙设备并关闭蓝牙"
        AppLanguage.ENGLISH -> "Disconnect all Bluetooth devices and turn off Bluetooth"
        AppLanguage.ARABIC -> "قطع اتصال جميع أجهزة البلوتوث وإيقاف البلوتوث"
    }

    val forceDisableMobileData: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制关闭移动数据"
        AppLanguage.ENGLISH -> "Force Disable Mobile Data"
        AppLanguage.ARABIC -> "فرض إيقاف بيانات الهاتف"
    }

    val forceDisableMobileDataDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "关闭蜂窝网络（需要系统权限，部分设备不支持）"
        AppLanguage.ENGLISH -> "Disable cellular network (requires system permission, not supported on all devices)"
        AppLanguage.ARABIC -> "إيقاف الشبكة الخلوية (يتطلب إذن النظام، غير مدعوم على جميع الأجهزة)"
    }
    // ==================== Background Running ====================
    val backgroundRunTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "Background Run"
        AppLanguage.ENGLISH -> "Background Run"
        AppLanguage.ARABIC -> "التشغيل في الخلفية"
    }

    val backgroundRunDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "退出应用后继续在后台运行"
        AppLanguage.ENGLISH -> "Keep running in background after exit"
        AppLanguage.ARABIC -> "الاستمرار في العمل في الخلفية بعد الخروج"
    }

    val backgroundRunShowNotification: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示通知"
        AppLanguage.ENGLISH -> "Show Notification"
        AppLanguage.ARABIC -> "عرض الإشعار"
    }

    val backgroundRunShowNotificationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "在通知栏显示运行状态"
        AppLanguage.ENGLISH -> "Show running status in notification bar"
        AppLanguage.ARABIC -> "عرض حالة التشغيل في شريط الإشعارات"
    }

    val backgroundRunKeepCpuAwake: String get() = when (lang) {
        AppLanguage.CHINESE -> "保持CPU唤醒"
        AppLanguage.ENGLISH -> "Keep CPU Awake"
        AppLanguage.ARABIC -> "إبقاء المعالج نشطًا"
    }

    val backgroundRunKeepCpuAwakeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "防止系统休眠，保持后台任务运行"
        AppLanguage.ENGLISH -> "Prevent system sleep, keep background tasks running"
        AppLanguage.ARABIC -> "منع سكون النظام والحفاظ على تشغيل المهام في الخلفية"
    }

    val backgroundRunNotificationTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "通知标题"
        AppLanguage.ENGLISH -> "Notification Title"
        AppLanguage.ARABIC -> "عنوان الإشعار"
    }

    val backgroundRunNotificationTitlePlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "留空使用默认标题"
        AppLanguage.ENGLISH -> "Leave empty for default title"
        AppLanguage.ARABIC -> "اتركه فارغًا للعنوان الافتراضي"
    }

    val backgroundRunNotificationContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "通知内容"
        AppLanguage.ENGLISH -> "Notification Content"
        AppLanguage.ARABIC -> "محتوى الإشعار"
    }

    val backgroundRunNotificationContentPlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "留空使用默认内容"
        AppLanguage.ENGLISH -> "Leave empty for default content"
        AppLanguage.ARABIC -> "اتركه فارغًا للمحتوى الافتراضي"
    }

    val showAdvanced: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示高级设置"
        AppLanguage.ENGLISH -> "Show Advanced"
        AppLanguage.ARABIC -> "عرض الإعدادات المتقدمة"
    }

    val hideAdvanced: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏高级设置"
        AppLanguage.ENGLISH -> "Hide Advanced"
        AppLanguage.ARABIC -> "إخفاء الإعدادات المتقدمة"
    }
    // ==================== /Spoofing ====================
    val disguiseMultiIconTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "图标与应用"
        AppLanguage.ENGLISH -> "Icons & App"
        AppLanguage.ARABIC -> "الأيقونات والتطبيق"
    }

    val disguiseMultiIconDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "多桌面图标和伪装功能"
        AppLanguage.ENGLISH -> "Multi-desktop icons and disguise features"
        AppLanguage.ARABIC -> "ميزات الأيقونات المتعددة والتنكر"
    }

    val disguiseIconCountFormat: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个图标"
        AppLanguage.ENGLISH -> "%d icons"
        AppLanguage.ARABIC -> "%d أيقونات"
    }

    val disguiseNotEnabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "未启用"
        AppLanguage.ENGLISH -> "Not enabled"
        AppLanguage.ARABIC -> "غير مفعل"
    }

    val disguiseEnableMultiIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用多图标"
        AppLanguage.ENGLISH -> "Enable Multi Icons"
        AppLanguage.ARABIC -> "تفعيل الأيقونات المتعددة"
    }

    val disguiseEnableMultiIconDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装后在桌面显示多个应用图标"
        AppLanguage.ENGLISH -> "Show multiple app icons on desktop after installation"
        AppLanguage.ARABIC -> "عرض أيقونات تطبيق متعددة على سطح المكتب بعد التثبيت"
    }

    val disguiseIconCountTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "桌面图标数量"
        AppLanguage.ENGLISH -> "Desktop Icon Count"
        AppLanguage.ARABIC -> "عدد أيقونات سطح المكتب"
    }

    val disguiseIconCountDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "设置安装后在桌面显示的图标数量（每个图标都可启动应用）"
        AppLanguage.ENGLISH -> "Set the number of icons to display on desktop (each icon can launch the app)"
        AppLanguage.ARABIC -> "تعيين عدد الأيقونات المعروضة على سطح المكتب (كل أيقونة يمكنها تشغيل التطبيق)"
    }

    val disguiseCountLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "数量"
        AppLanguage.ENGLISH -> "Count"
        AppLanguage.ARABIC -> "العدد"
    }

    val disguiseCountHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "无上限 — 数量越大影响越大"
        AppLanguage.ENGLISH -> "No limit — higher count = greater impact"
        AppLanguage.ARABIC -> "بدون حد - عدد أكبر = تأثير أكبر"
    }

    val disguiseMultiIconTip: String get() = when (lang) {
        AppLanguage.CHINESE -> "多图标功能通过 Android 原生的 activity-alias 机制实现，安装后自动显示多个桌面图标，无需任何额外权限。"
        AppLanguage.ENGLISH -> "Multi-icon feature uses Android native activity-alias mechanism, automatically showing multiple desktop icons after installation, no extra permissions required."
        AppLanguage.ARABIC -> "تستخدم ميزة الأيقونات المتعددة آلية activity-alias الأصلية في Android، مع عرض أيقونات سطح المكتب المتعددة تلقائيًا بعد التثبيت، دون الحاجة إلى أذونات إضافية."
    }
}
