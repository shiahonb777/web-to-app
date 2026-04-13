package com.webtoapp.core.i18n.strings

import com.webtoapp.R
import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object CommonStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage

    val timeAlignment: String get() = when (lang) {
        AppLanguage.CHINESE -> "时间对齐"
        AppLanguage.ENGLISH -> "Time Alignment"
        AppLanguage.ARABIC -> "محاذاة الوقت"
    }
    val duration: String get() = when (lang) {
        AppLanguage.CHINESE -> "时长"
        AppLanguage.ENGLISH -> "Duration"
        AppLanguage.ARABIC -> "المدة"
    }
    val times: String get() = when (lang) {
        AppLanguage.CHINESE -> "次"
        AppLanguage.ENGLISH -> "times"
        AppLanguage.ARABIC -> "مرات"
    }
    val time: String get() = when (lang) {
        AppLanguage.CHINESE -> "时间"
        AppLanguage.ENGLISH -> "Time"
        AppLanguage.ARABIC -> "الوقت"
    }
    val timezoneSpoofing: String get() = when (lang) {
        AppLanguage.CHINESE -> "时区伪装"
        AppLanguage.ENGLISH -> "Timezone Spoofing"
        AppLanguage.ARABIC -> "تزييف المنطقة الزمنية"
    }
    val timezoneSpoofingHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "伪装系统时区"
        AppLanguage.ENGLISH -> "Spoof system timezone"
        AppLanguage.ARABIC -> "تزييف المنطقة الزمنية للنظام"
    }
    val durationMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "限时进入"
        AppLanguage.ENGLISH -> "Limited Access"
        AppLanguage.ARABIC -> "وصول محدود"
    }
    val durationModeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "只能在指定时间段内进入应用，其他时间无法打开"
        AppLanguage.ENGLISH -> "Can only enter app during specified time, cannot open at other times"
        AppLanguage.ARABIC -> "يمكن الدخول للتطبيق فقط خلال الوقت المحدد، لا يمكن الفتح في أوقات أخرى"
    }
    val author: String get() = when (lang) {
        AppLanguage.CHINESE -> "作者"
        AppLanguage.ENGLISH -> "Author"
        AppLanguage.ARABIC -> "المؤلف"
    }
    val authorTagline: String get() = when (lang) {
        AppLanguage.CHINESE -> "独立开发者 · AI 爱好者"
        AppLanguage.ENGLISH -> "Indie Developer · AI Enthusiast"
        AppLanguage.ARABIC -> "مطور مستقل · متحمس للذكاء الاصطناعي"
    }
    val authorAvatar: String get() = when (lang) {
        AppLanguage.CHINESE -> "作者头像"
        AppLanguage.ENGLISH -> "Author Avatar"
        AppLanguage.ARABIC -> "صورة المؤلف"
    }
}

    val menuAiCoding: String get() = when (lang) {
        else -> Strings.resourceString(R.string.menu_ai_coding)
    }

    val menuThemeSettings: String get() = when (lang) {
        else -> Strings.resourceString(R.string.menu_theme_settings)
    }

    val menuAiSettings: String get() = when (lang) {
        else -> Strings.resourceString(R.string.menu_ai_settings)
    }

    val menuAppModifier: String get() = when (lang) {
        else -> Strings.resourceString(R.string.menu_app_modifier)
    }

    val menuExtensionModules: String get() = when (lang) {
        else -> Strings.resourceString(R.string.menu_extension_modules)
    }

    val menuAbout: String get() = when (lang) {
        else -> Strings.resourceString(R.string.menu_about)
    }

    val tabHome: String get() = when (lang) {
        AppLanguage.CHINESE -> "首页"
        AppLanguage.ENGLISH -> "Home"
        AppLanguage.ARABIC -> "الرئيسية"
    }

    val tabStore: String get() = when (lang) {
        AppLanguage.CHINESE -> "市场"
        AppLanguage.ENGLISH -> "Market"
        AppLanguage.ARABIC -> "السوق"
    }

    val marketTabApps: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用"
        AppLanguage.ENGLISH -> "Apps"
        AppLanguage.ARABIC -> "التطبيقات"
    }

    val marketTabModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块"
        AppLanguage.ENGLISH -> "Modules"
        AppLanguage.ARABIC -> "الوحدات"
    }

    val tabProfile: String get() = when (lang) {
        AppLanguage.CHINESE -> "我的"
        AppLanguage.ENGLISH -> "Profile"
        AppLanguage.ARABIC -> "حسابي"
    }

    val tabMore: String get() = when (lang) {
        AppLanguage.CHINESE -> "更多"
        AppLanguage.ENGLISH -> "More"
        AppLanguage.ARABIC -> "المزيد"
    }

    val moreSectionAiTools: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 工具"
        AppLanguage.ENGLISH -> "AI Tools"
        AppLanguage.ARABIC -> "أدوات AI"
    }

    val moreSectionDevTools: String get() = when (lang) {
        AppLanguage.CHINESE -> "开发工具"
        AppLanguage.ENGLISH -> "Dev Tools"
        AppLanguage.ARABIC -> "أدوات التطوير"
    }

    val moreSectionBrowser: String get() = when (lang) {
        AppLanguage.CHINESE -> "浏览器 & 网络"
        AppLanguage.ENGLISH -> "Browser & Network"
        AppLanguage.ARABIC -> "المتصفح والشبكة"
    }

    val moreSectionAppearance: String get() = when (lang) {
        AppLanguage.CHINESE -> "外观 & 数据"
        AppLanguage.ENGLISH -> "Appearance & Data"
        AppLanguage.ARABIC -> "المظهر والبيانات"
    }

    val menuAccount: String get() = when (lang) {
        AppLanguage.CHINESE -> "账号"
        AppLanguage.ENGLISH -> "Account"
        AppLanguage.ARABIC -> "الحساب"
    }

    val menuLoginRegister: String get() = when (lang) {
        AppLanguage.CHINESE -> "登录 / 注册"
        AppLanguage.ENGLISH -> "Sign In / Sign Up"
        AppLanguage.ARABIC -> "تسجيل الدخول / إنشاء حساب"
    }

    val menuStats: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用统计"
        AppLanguage.ENGLISH -> "Usage Stats"
        AppLanguage.ARABIC -> "إحصائيات الاستخدام"
    }

    val menuBatchImport: String get() = when (lang) {
        AppLanguage.CHINESE -> "批量导入"
        AppLanguage.ENGLISH -> "Batch Import"
        AppLanguage.ARABIC -> "استيراد مجمع"
    }

    val menuLinuxEnvironment: String get() = when (lang) {
        AppLanguage.CHINESE -> "Linux 环境"
        AppLanguage.ENGLISH -> "Linux Environment"
        AppLanguage.ARABIC -> "بيئة Linux"
    }

    val menuLanguage: String get() = when (lang) {
        AppLanguage.CHINESE -> "语言"
        AppLanguage.ENGLISH -> "Language"
        AppLanguage.ARABIC -> "اللغة"
    }

    val appTypeMultiWeb: String get() = when (lang) {
        AppLanguage.CHINESE -> "多站点"
        AppLanguage.ENGLISH -> "Multi-Site"
        AppLanguage.ARABIC -> "متعدد المواقع"
    }

    val appTypeWeb: String get() = when (lang) {
        AppLanguage.CHINESE -> "网页"
        AppLanguage.ENGLISH -> "Web"
        AppLanguage.ARABIC -> "ويب"
    }

    val appTypeImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片"
        AppLanguage.ENGLISH -> "Image"
        AppLanguage.ARABIC -> "صورة"
    }

    val appTypeVideo: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频"
        AppLanguage.ENGLISH -> "Video"
        AppLanguage.ARABIC -> "فيديو"
    }

    val appTypeHtml: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML"
        AppLanguage.ENGLISH -> "HTML"
        AppLanguage.ARABIC -> "HTML"
    }

    val appTypeGallery: String get() = when (lang) {
        AppLanguage.CHINESE -> "画廊"
        AppLanguage.ENGLISH -> "Gallery"
        AppLanguage.ARABIC -> "معرض"
    }

    val appTypeFrontend: String get() = when (lang) {
        AppLanguage.CHINESE -> "前端"
        AppLanguage.ENGLISH -> "Frontend"
        AppLanguage.ARABIC -> "الواجهة الأمامية"
    }

    val appTypeWordPress: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress"
        AppLanguage.ENGLISH -> "WordPress"
        AppLanguage.ARABIC -> "WordPress"
    }

    val appTypeNodeJs: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js"
        AppLanguage.ENGLISH -> "Node.js"
        AppLanguage.ARABIC -> "Node.js"
    }

    val appTypePhp: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP"
        AppLanguage.ENGLISH -> "PHP"
        AppLanguage.ARABIC -> "PHP"
    }

    val appTypePython: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python"
        AppLanguage.ENGLISH -> "Python"
        AppLanguage.ARABIC -> "Python"
    }

    val appTypeGo: String get() = when (lang) {
        AppLanguage.CHINESE -> "Go"
        AppLanguage.ENGLISH -> "Go"
        AppLanguage.ARABIC -> "Go"
    }

    val appTypeDocsSite: String get() = when (lang) {
        AppLanguage.CHINESE -> "文档"
        AppLanguage.ENGLISH -> "Docs"
        AppLanguage.ARABIC -> "التوثيق"
    }

    val menuRuntimeDeps: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行时管理"
        AppLanguage.ENGLISH -> "Runtime Manager"
        AppLanguage.ARABIC -> "مدير وقت التشغيل"
    }

    val menuPortManager: String get() = when (lang) {
        AppLanguage.CHINESE -> "端口管理"
        AppLanguage.ENGLISH -> "Port Manager"
        AppLanguage.ARABIC -> "مدير المنافذ"
    }

    val appConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用配置"
        AppLanguage.ENGLISH -> "App Config"
        AppLanguage.ARABIC -> "إعدادات التطبيق"
    }

    val appearance: String get() = when (lang) {
        AppLanguage.CHINESE -> "外观"
        AppLanguage.ENGLISH -> "Appearance"
        AppLanguage.ARABIC -> "المظهر"
    }

    val appIconModifier: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用修改器"
        AppLanguage.ENGLISH -> "App Modifier"
        AppLanguage.ARABIC -> "معدل التطبيق"
    }

    val appAlreadyActivated: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用已激活"
        AppLanguage.ENGLISH -> "App already activated"
        AppLanguage.ARABIC -> "التطبيق مفعل بالفعل"
    }

    val appAlreadyActivatedHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "此应用已使用有效激活码激活"
        AppLanguage.ENGLISH -> "This app has already been activated with a valid code"
        AppLanguage.ARABIC -> "تم تفعيل هذا التطبيق بالفعل برمز صالح"
    }

    val appliedPreset: String get() = when (lang) {
        AppLanguage.CHINESE -> "已应用方案"
        AppLanguage.ENGLISH -> "Preset applied"
        AppLanguage.ARABIC -> "تم تطبيق الإعداد المسبق"
    }

    val appConfigLoadFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用配置加载失败，请重新安装"
        AppLanguage.ENGLISH -> "App configuration load failed, please reinstall"
        AppLanguage.ARABIC -> "فشل تحميل تكوين التطبيق، يرجى إعادة التثبيت"
    }

    val appCategoryFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增应用分类功能"
        AppLanguage.ENGLISH -> "App category feature"
        AppLanguage.ARABIC -> "ميزة تصنيف التطبيقات"
    }

    val appModifierFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用修改器：修改图标和名称"
        AppLanguage.ENGLISH -> "App modifier: modify icon and name"
        AppLanguage.ARABIC -> "معدل التطبيق: تعديل الأيقونة والاسم"
    }

    val appNeedsActivation: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用需要激活"
        AppLanguage.ENGLISH -> "App needs activation"
        AppLanguage.ARABIC -> "التطبيق يحتاج إلى تفعيل"
    }

    val applied: String get() = when (lang) {
        AppLanguage.CHINESE -> "已应用"
        AppLanguage.ENGLISH -> "Applied"
        AppLanguage.ARABIC -> "مطبق"
    }

    val applyTheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用主题"
        AppLanguage.ENGLISH -> "Apply Theme"
        AppLanguage.ARABIC -> "تطبيق السمة"
    }

    val appNotAccessibleNow: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前时间无法进入应用"
        AppLanguage.ENGLISH -> "App not accessible at this time"
        AppLanguage.ARABIC -> "التطبيق غير متاح في هذا الوقت"
    }

    val appDisguiseSection: String get() = when (lang) {
        AppLanguage.CHINESE -> "App disguise"
        AppLanguage.ENGLISH -> "App Disguise"
        AppLanguage.ARABIC -> "تنكر التطبيق"
    }

    val menuBrowserKernel: String get() = when (lang) {
        AppLanguage.CHINESE -> "浏览器内核"
        AppLanguage.ENGLISH -> "Browser Kernel"
        AppLanguage.ARABIC -> "نواة المتصفح"
    }

    val menuUserscript: String get() = when (lang) {
        AppLanguage.CHINESE -> "油猴脚本"
        AppLanguage.ENGLISH -> "Userscripts"
        AppLanguage.ARABIC -> "سكريبتات المستخدم"
    }

    val menuHostsAdBlock: String get() = when (lang) {
        AppLanguage.CHINESE -> "Hosts 拦截"
        AppLanguage.ENGLISH -> "Hosts Blocking"
        AppLanguage.ARABIC -> "حظر Hosts"
    }

    val appHardening: String get() = when (lang) {
        AppLanguage.CHINESE -> "软件加固"
        AppLanguage.ENGLISH -> "App Hardening"
        AppLanguage.ARABIC -> "تعزيز التطبيق"
    }

    val appHardeningDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "企业级应用加固保护，远超市面加固方案"
        AppLanguage.ENGLISH -> "Enterprise-grade app hardening, surpassing market solutions"
        AppLanguage.ARABIC -> "تعزيز التطبيقات على مستوى المؤسسات"
    }

    val appRunningInBackground: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在后台运行"
        AppLanguage.ENGLISH -> "Running in background"
        AppLanguage.ARABIC -> "يعمل في الخلفية"
    }

    val appSavedSuccessfully: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用保存成功"
        AppLanguage.ENGLISH -> "App saved successfully"
        AppLanguage.ARABIC -> "تم حفظ التطبيق بنجاح"
    }

    val appDeleted: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用已删除"
        AppLanguage.ENGLISH -> "App deleted"
        AppLanguage.ARABIC -> "تم حذف التطبيق"
    }

    val appCreatedSuccessfully: String get() = when (lang) {
        AppLanguage.CHINESE -> "%s 应用创建成功"
        AppLanguage.ENGLISH -> "%s app created successfully"
        AppLanguage.ARABIC -> "تم إنشاء تطبيق %s بنجاح"
    }

    val appUpdatedSuccessfully: String get() = when (lang) {
        AppLanguage.CHINESE -> "%s 应用更新成功"
        AppLanguage.ENGLISH -> "%s app updated successfully"
        AppLanguage.ARABIC -> "تم تحديث تطبيق %s بنجاح"
    }
}
