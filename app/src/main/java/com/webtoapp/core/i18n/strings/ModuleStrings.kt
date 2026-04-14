package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object ModuleStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage

    val moduleStoreSearchPlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索模块..."
        AppLanguage.ENGLISH -> "Search modules..."
        AppLanguage.ARABIC -> "البحث عن وحدات..."
    }

    val moduleStoreEmpty: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无模块\n成为第一个发布者吧"
        AppLanguage.ENGLISH -> "No modules yet\nBe the first to publish!"
        AppLanguage.ARABIC -> "لا توجد وحدات بعد\nكن أول من ينشر!"
    }

    val moduleStoreEmptySearch: String get() = when (lang) {
        AppLanguage.CHINESE -> "没有找到匹配的模块"
        AppLanguage.ENGLISH -> "No matching modules found"
        AppLanguage.ARABIC -> "لم يتم العثور على وحدات مطابقة"
    }

    val moduleStoreInstall: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装"
        AppLanguage.ENGLISH -> "Install"
        AppLanguage.ARABIC -> "تثبيت"
    }

    val moduleStoreFeatured: String get() = when (lang) {
        AppLanguage.CHINESE -> "精选"
        AppLanguage.ENGLISH -> "Featured"
        AppLanguage.ARABIC -> "مميز"
    }

    val moduleStoreSortDownloads: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载"
        AppLanguage.ENGLISH -> "Downloads"
        AppLanguage.ARABIC -> "التنزيلات"
    }

    val moduleStoreSortRating: String get() = when (lang) {
        AppLanguage.CHINESE -> "评分"
        AppLanguage.ENGLISH -> "Rating"
        AppLanguage.ARABIC -> "التقييم"
    }

    val moduleStoreSortNewest: String get() = when (lang) {
        AppLanguage.CHINESE -> "最新"
        AppLanguage.ENGLISH -> "Newest"
        AppLanguage.ARABIC -> "الأحدث"
    }

    val moduleStoreSortLikes: String get() = when (lang) {
        AppLanguage.CHINESE -> "点赞"
        AppLanguage.ENGLISH -> "Likes"
        AppLanguage.ARABIC -> "الإعجابات"
    }

    val moduleStoreCatAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "全部"
        AppLanguage.ENGLISH -> "All"
        AppLanguage.ARABIC -> "الكل"
    }

    val extensionModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "扩展模块"
        AppLanguage.ENGLISH -> "Extension Module"
        AppLanguage.ARABIC -> "وحدة إضافية"
    }

    val moduleTesting: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块测试"
        AppLanguage.ENGLISH -> "Module Testing"
        AppLanguage.ARABIC -> "اختبار الوحدة"
    }

    val moduleSchemes: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块方案"
        AppLanguage.ENGLISH -> "Module Schemes"
        AppLanguage.ARABIC -> "مخططات الوحدات"
    }

    val moduleNameRequired: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块名称 *"
        AppLanguage.ENGLISH -> "Module Name *"
        AppLanguage.ARABIC -> "اسم الوحدة *"
    }

    val moduleInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块信息"
        AppLanguage.ENGLISH -> "Module Info"
        AppLanguage.ARABIC -> "معلومات الوحدة"
    }

    val moduleGeneratedSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块生成成功"
        AppLanguage.ENGLISH -> "Module Generated Successfully"
        AppLanguage.ARABIC -> "تم إنشاء الوحدة بنجاح"
    }

    val extensionModuleSystem: String get() = when (lang) {
        AppLanguage.CHINESE -> "扩展模块系统：类油猴脚本JS/CSS注入"
        AppLanguage.ENGLISH -> "Extension module system: Tampermonkey-like JS/CSS injection"
        AppLanguage.ARABIC -> "نظام وحدات الامتداد: حقن JS/CSS مثل Tampermonkey"
    }

    val moduleDevelopmentDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI Agent 辅助开发扩展模块"
        AppLanguage.ENGLISH -> "AI Agent assisted extension module development"
        AppLanguage.ARABIC -> "تطوير وحدات الإضافة بمساعدة وكيل الذكاء الاصطناعي"
    }

    val moduleCopySuffix: String get() = when (lang) {
        AppLanguage.CHINESE -> "副本"
        AppLanguage.ENGLISH -> "Copy"
        AppLanguage.ARABIC -> "نسخة"
    }

    val extensionModuleTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "Extension module"
        AppLanguage.ENGLISH -> "Extension Modules"
        AppLanguage.ARABIC -> "الوحدات الإضافية"
    }

    val modulesSelected: String get() = when (lang) {
        AppLanguage.CHINESE -> "已选择 %d 个模块"
        AppLanguage.ENGLISH -> "%d modules selected"
        AppLanguage.ARABIC -> "تم اختيار %d وحدات"
    }

    val extensionModuleHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "扩展模块可以为应用添加自定义功能，如屏蔽元素、深色模式等"
        AppLanguage.ENGLISH -> "Extension modules can add custom features to apps, such as blocking elements, dark mode, etc."
        AppLanguage.ARABIC -> "يمكن للوحدات الإضافية إضافة ميزات مخصصة للتطبيقات، مثل حظر العناصر والوضع الداكن وما إلى ذلك"
    }

    val moduleTooLargeTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块较大，使用文件分享"
        AppLanguage.ENGLISH -> "Module too large, use file sharing"
        AppLanguage.ARABIC -> "الوحدة كبيرة جدًا، استخدم مشاركة الملفات"
    }

    val moduleTooLargeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前模块数据为 %d 字节，超过二维码容量限制。\n请使用文件方式分享给好友。"
        AppLanguage.ENGLISH -> "Current module data is %d bytes, exceeds QR code capacity.\nPlease use file sharing instead."
        AppLanguage.ARABIC -> "بيانات الوحدة %d بايت، تتجاوز سعة رمز QR.\nيرجى استخدام مشاركة الملفات."
    }

    val extensionModuleSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "Extension module"
        AppLanguage.ENGLISH -> "Extension Module"
        AppLanguage.ARABIC -> "وحدة إضافية"
    }

    val extensionModulesTab: String get() = when (lang) {
        AppLanguage.CHINESE -> "扩展模块"
        AppLanguage.ENGLISH -> "Extensions"
        AppLanguage.ARABIC -> "الإضافات"
    }

    val moduleUiConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "UI 配置"
        AppLanguage.ENGLISH -> "UI Configuration"
        AppLanguage.ARABIC -> "تكوين الواجهة"
    }

    val moduleUiType: String get() = when (lang) {
        AppLanguage.CHINESE -> "UI 类型"
        AppLanguage.ENGLISH -> "UI Type"
        AppLanguage.ARABIC -> "نوع الواجهة"
    }

    val moduleUiPosition: String get() = when (lang) {
        AppLanguage.CHINESE -> "位置"
        AppLanguage.ENGLISH -> "Position"
        AppLanguage.ARABIC -> "الموضع"
    }

    val moduleUiDraggable: String get() = when (lang) {
        AppLanguage.CHINESE -> "可拖动"
        AppLanguage.ENGLISH -> "Draggable"
        AppLanguage.ARABIC -> "قابل للسحب"
    }

    val moduleUiAutoHide: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动时自动隐藏"
        AppLanguage.ENGLISH -> "Auto hide on scroll"
        AppLanguage.ARABIC -> "إخفاء تلقائي عند التمرير"
    }

    val moduleUiCollapsible: String get() = when (lang) {
        AppLanguage.CHINESE -> "可折叠"
        AppLanguage.ENGLISH -> "Collapsible"
        AppLanguage.ARABIC -> "قابل للطي"
    }

    val moduleUiInitiallyCollapsed: String get() = when (lang) {
        AppLanguage.CHINESE -> "初始折叠"
        AppLanguage.ENGLISH -> "Initially collapsed"
        AppLanguage.ARABIC -> "مطوي مبدئيًا"
    }

    val moduleUiButtonSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "按钮大小"
        AppLanguage.ENGLISH -> "Button Size"
        AppLanguage.ARABIC -> "حجم الزر"
    }

    val moduleUiButtonColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "按钮颜色"
        AppLanguage.ENGLISH -> "Button Color"
        AppLanguage.ARABIC -> "لون الزر"
    }

    val moduleUiSidebarWidth: String get() = when (lang) {
        AppLanguage.CHINESE -> "侧边栏宽度"
        AppLanguage.ENGLISH -> "Sidebar Width"
        AppLanguage.ARABIC -> "عرض الشريط الجانبي"
    }

    val moduleUiBottomBarHeight: String get() = when (lang) {
        AppLanguage.CHINESE -> "底部栏高度"
        AppLanguage.ENGLISH -> "Bottom Bar Height"
        AppLanguage.ARABIC -> "ارتفاع الشريط السفلي"
    }

    val moduleUiPanelSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "面板大小"
        AppLanguage.ENGLISH -> "Panel Size"
        AppLanguage.ARABIC -> "حجم اللوحة"
    }

    val moduleUiPanelResizable: String get() = when (lang) {
        AppLanguage.CHINESE -> "可调整大小"
        AppLanguage.ENGLISH -> "Resizable"
        AppLanguage.ARABIC -> "قابل لتغيير الحجم"
    }

    val moduleUiPanelMinimizable: String get() = when (lang) {
        AppLanguage.CHINESE -> "可最小化"
        AppLanguage.ENGLISH -> "Minimizable"
        AppLanguage.ARABIC -> "قابل للتصغير"
    }
    // ==================== Extension Module Page ====================
    val searchModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索模块..."
        AppLanguage.ENGLISH -> "Search modules..."
        AppLanguage.ARABIC -> "البحث عن الوحدات..."
    }

    val all: String get() = when (lang) {
        AppLanguage.CHINESE -> "全部"
        AppLanguage.ENGLISH -> "All"
        AppLanguage.ARABIC -> "الكل"
    }

    val totalModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "共 %d 个模块"
        AppLanguage.ENGLISH -> "%d modules total"
        AppLanguage.ARABIC -> "إجمالي %d وحدات"
    }

    val enabledModules: String get() = when (lang) {
        AppLanguage.CHINESE -> "已启用 %d 个"
        AppLanguage.ENGLISH -> "%d enabled"
        AppLanguage.ARABIC -> "%d مفعلة"
    }

    val builtIn: String get() = when (lang) {
        AppLanguage.CHINESE -> "内置"
        AppLanguage.ENGLISH -> "Built-in"
        AppLanguage.ARABIC -> "مدمج"
    }

    val duplicate: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制"
        AppLanguage.ENGLISH -> "Duplicate"
        AppLanguage.ARABIC -> "نسخ"
    }

    val copyShareCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制分享码"
        AppLanguage.ENGLISH -> "Copy Share Code"
        AppLanguage.ARABIC -> "نسخ رمز المشاركة"
    }

    val shareQrCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享二维码"
        AppLanguage.ENGLISH -> "Share QR Code"
        AppLanguage.ARABIC -> "مشاركة رمز QR"
    }

    val shareCodeCopied: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享码已复制"
        AppLanguage.ENGLISH -> "Share code copied"
        AppLanguage.ARABIC -> "تم نسخ رمز المشاركة"
    }

    val noModulesFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "没有找到匹配的模块"
        AppLanguage.ENGLISH -> "No matching modules found"
        AppLanguage.ARABIC -> "لم يتم العثور على وحدات مطابقة"
    }

    val noModulesYet: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无模块"
        AppLanguage.ENGLISH -> "No modules yet"
        AppLanguage.ARABIC -> "لا توجد وحدات بعد"
    }

    val totalModulesLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "总模块"
        AppLanguage.ENGLISH -> "Total"
        AppLanguage.ARABIC -> "الإجمالي"
    }

    val builtInLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "内置"
        AppLanguage.ENGLISH -> "Built-in"
        AppLanguage.ARABIC -> "مدمج"
    }

    val customLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义"
        AppLanguage.ENGLISH -> "Custom"
        AppLanguage.ARABIC -> "مخصص"
    }

    val tryDifferentSearch: String get() = when (lang) {
        AppLanguage.CHINESE -> "请尝试其他搜索关键词"
        AppLanguage.ENGLISH -> "Try a different search term"
        AppLanguage.ARABIC -> "جرب مصطلح بحث مختلف"
    }

    val clearSearch: String get() = when (lang) {
        AppLanguage.CHINESE -> "清除搜索"
        AppLanguage.ENGLISH -> "Clear search"
        AppLanguage.ARABIC -> "مسح البحث"
    }

    val importModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入模块"
        AppLanguage.ENGLISH -> "Import Module"
        AppLanguage.ARABIC -> "استيراد وحدة"
    }

    val importFromFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "从文件导入"
        AppLanguage.ENGLISH -> "Import from File"
        AppLanguage.ARABIC -> "استيراد من ملف"
    }

    val selectWtamodFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择 .wtamod 或 .wtapkg 文件"
        AppLanguage.ENGLISH -> "Select .wtamod or .wtapkg file"
        AppLanguage.ARABIC -> "اختر ملف .wtamod أو .wtapkg"
    }

    val importFromShareCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "从分享码导入"
        AppLanguage.ENGLISH -> "Import from Share Code"
        AppLanguage.ARABIC -> "استيراد من رمز المشاركة"
    }

    val pasteShareCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "粘贴他人分享的模块代码"
        AppLanguage.ENGLISH -> "Paste shared module code"
        AppLanguage.ARABIC -> "لصق رمز الوحدة المشتركة"
    }

    val shareCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享码"
        AppLanguage.ENGLISH -> "Share Code"
        AppLanguage.ARABIC -> "رمز المشاركة"
    }

    val pasteShareCodeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "粘贴分享码..."
        AppLanguage.ENGLISH -> "Paste share code..."
        AppLanguage.ARABIC -> "لصق رمز المشاركة..."
    }

    val pasteFromClipboard: String get() = when (lang) {
        AppLanguage.CHINESE -> "从剪贴板粘贴"
        AppLanguage.ENGLISH -> "Paste from Clipboard"
        AppLanguage.ARABIC -> "لصق من الحافظة"
    }

    val importFromQrImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "从二维码图片导入"
        AppLanguage.ENGLISH -> "Import from QR Code Image"
        AppLanguage.ARABIC -> "استيراد من صورة رمز QR"
    }

    val selectQrImageHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择包含模块二维码的图片"
        AppLanguage.ENGLISH -> "Select image containing module QR code"
        AppLanguage.ARABIC -> "اختر صورة تحتوي على رمز QR للوحدة"
    }

    val qrCodeNotFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "未检测到二维码"
        AppLanguage.ENGLISH -> "QR code not detected"
        AppLanguage.ARABIC -> "لم يتم اكتشاف رمز QR"
    }

    val imageLoadFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片加载失败"
        AppLanguage.ENGLISH -> "Failed to load image"
        AppLanguage.ARABIC -> "فشل في تحميل الصورة"
    }

    val onlyOnMatchingUrls: String get() = when (lang) {
        AppLanguage.CHINESE -> "仅在 %d 个匹配规则的网站生效"
        AppLanguage.ENGLISH -> "Only works on %d matching URL rules"
        AppLanguage.ARABIC -> "يعمل فقط على %d قواعد URL مطابقة"
    }

    val requiresSensitivePermissions: String get() = when (lang) {
        AppLanguage.CHINESE -> "需要敏感权限"
        AppLanguage.ENGLISH -> "Requires sensitive permissions"
        AppLanguage.ARABIC -> "يتطلب أذونات حساسة"
    }

    val manualCreate: String get() = when (lang) {
        AppLanguage.CHINESE -> "手动创建"
        AppLanguage.ENGLISH -> "Manual Create"
        AppLanguage.ARABIC -> "إنشاء يدوي"
    }
    // ==================== Module Editor ====================
    val pleaseEnterModuleName: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入模块名称"
        AppLanguage.ENGLISH -> "Please enter module name"
        AppLanguage.ARABIC -> "يرجى إدخال اسم الوحدة"
    }

    val pleaseEnterCodeContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入代码内容"
        AppLanguage.ENGLISH -> "Please enter code content"
        AppLanguage.ARABIC -> "يرجى إدخال محتوى الكود"
    }

    val saveSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "Save successful"
        AppLanguage.ENGLISH -> "Save successful"
        AppLanguage.ARABIC -> "تم الحفظ بنجاح"
    }

    val pleaseEnterRequirement: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入功能需求"
        AppLanguage.ENGLISH -> "Please enter feature requirement"
        AppLanguage.ARABIC -> "يرجى إدخال متطلبات الميزة"
    }

    val jumpToModuleEditor: String get() = when (lang) {
        AppLanguage.CHINESE -> "即将跳转到模块编辑器"
        AppLanguage.ENGLISH -> "Jumping to module editor"
        AppLanguage.ARABIC -> "الانتقال إلى محرر الوحدة"
    }

    val storagePermissionRequired: String get() = when (lang) {
        AppLanguage.CHINESE -> "需要存储权限才能下载文件"
        AppLanguage.ENGLISH -> "Storage permission required to download files"
        AppLanguage.ARABIC -> "يلزم إذن التخزين لتحميل الملفات"
    }

    val frontendProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "前端项目"
        AppLanguage.ENGLISH -> "Frontend Project"
        AppLanguage.ARABIC -> "مشروع الواجهة الأمامية"
    }

    val shortcutCreatedSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "快捷方式创建成功"
        AppLanguage.ENGLISH -> "Shortcut created successfully"
        AppLanguage.ARABIC -> "تم إنشاء الاختصار بنجاح"
    }

    val projectExportedTo: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目已导出到: %s"
        AppLanguage.ENGLISH -> "Project exported to: %s"
        AppLanguage.ARABIC -> "تم تصدير المشروع إلى: %s"
    }

    val preparing: String get() = when (lang) {
        AppLanguage.CHINESE -> "准备中..."
        AppLanguage.ENGLISH -> "Preparing..."
        AppLanguage.ARABIC -> "جاري التحضير..."
    }

    val buildApkForApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "将为「%s」构建独立的 APK 安装包。"
        AppLanguage.ENGLISH -> "Will build standalone APK for \"%s\"."
        AppLanguage.ARABIC -> "سيتم بناء APK مستقل لـ \"%s\"."
    }

    val buildCompleteInstallHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "构建完成后可直接安装到设备上，无需创建快捷方式。"
        AppLanguage.ENGLISH -> "After build, can be installed directly without creating shortcut."
        AppLanguage.ARABIC -> "بعد البناء، يمكن التثبيت مباشرة دون إنشاء اختصار."
    }

    val buildFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Build failed"
        AppLanguage.ENGLISH -> "Build failed"
        AppLanguage.ARABIC -> "فشل البناء"
    }

    val inputModuleName: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入模块名称"
        AppLanguage.ENGLISH -> "Enter module name"
        AppLanguage.ARABIC -> "أدخل اسم الوحدة"
    }

    val editModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑模块"
        AppLanguage.ENGLISH -> "Edit Module"
        AppLanguage.ARABIC -> "تعديل الوحدة"
    }

    val useTemplate: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用模板"
        AppLanguage.ENGLISH -> "Use Template"
        AppLanguage.ARABIC -> "استخدام القالب"
    }

    val basicInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "基本信息"
        AppLanguage.ENGLISH -> "Basic Info"
        AppLanguage.ARABIC -> "المعلومات الأساسية"
    }

    val code: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码"
        AppLanguage.ENGLISH -> "Code"
        AppLanguage.ARABIC -> "الكود"
    }

    val advancedSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级设置"
        AppLanguage.ENGLISH -> "Advanced Settings"
        AppLanguage.ARABIC -> "الإعدادات المتقدمة"
    }

    val webViewAdvancedConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "JavaScript · 存储 · 视口 · 离线"
        AppLanguage.ENGLISH -> "JavaScript · Storage · Viewport · Offline"
        AppLanguage.ARABIC -> "جافاسكريبت · التخزين · إطار العرض · غير متصل"
    }

    val sectionWebEngine: String get() = when (lang) {
        AppLanguage.CHINESE -> "引擎"
        AppLanguage.ENGLISH -> "Engine"
        AppLanguage.ARABIC -> "المحرك"
    }

    val sectionContentDisplay: String get() = when (lang) {
        AppLanguage.CHINESE -> "内容与显示"
        AppLanguage.ENGLISH -> "Content & Display"
        AppLanguage.ARABIC -> "المحتوى والعرض"
    }

    val sectionNavigation: String get() = when (lang) {
        AppLanguage.CHINESE -> "导航与交互"
        AppLanguage.ENGLISH -> "Navigation & Interaction"
        AppLanguage.ARABIC -> "التنقل والتفاعل"
    }

    val sectionOfflinePerformance: String get() = when (lang) {
        AppLanguage.CHINESE -> "离线与性能"
        AppLanguage.ENGLISH -> "Offline & Performance"
        AppLanguage.ARABIC -> "غير متصل والأداء"
    }

    val sectionDeveloper: String get() = when (lang) {
        AppLanguage.CHINESE -> "开发者工具"
        AppLanguage.ENGLISH -> "Developer"
        AppLanguage.ARABIC -> "المطور"
    }

    val selectCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择分类"
        AppLanguage.ENGLISH -> "Select Category"
        AppLanguage.ARABIC -> "اختيار الفئة"
    }

    val runTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "执行时机"
        AppLanguage.ENGLISH -> "Run Time"
        AppLanguage.ARABIC -> "وقت التشغيل"
    }

    val requiredPermissions: String get() = when (lang) {
        AppLanguage.CHINESE -> "所需权限"
        AppLanguage.ENGLISH -> "Required Permissions"
        AppLanguage.ARABIC -> "الأذونات المطلوبة"
    }

    val sensitive: String get() = when (lang) {
        AppLanguage.CHINESE -> "敏感"
        AppLanguage.ENGLISH -> "Sensitive"
        AppLanguage.ARABIC -> "حساس"
    }

    val confirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "OK"
        AppLanguage.ENGLISH -> "Confirm"
        AppLanguage.ARABIC -> "تأكيد"
    }

    val category: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类"
        AppLanguage.ENGLISH -> "Category"
        AppLanguage.ARABIC -> "الفئة"
    }

    val codeSnippets: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码块"
        AppLanguage.ENGLISH -> "Code Snippets"
        AppLanguage.ARABIC -> "مقتطفات الكود"
    }

    val availableFunctions: String get() = when (lang) {
        AppLanguage.CHINESE -> "💡 可用函数"
        AppLanguage.ENGLISH -> "💡 Available Functions"
        AppLanguage.ARABIC -> "💡 الدوال المتاحة"
    }

    val cssTips: String get() = when (lang) {
        AppLanguage.CHINESE -> "💡 CSS 提示"
        AppLanguage.ENGLISH -> "💡 CSS Tips"
        AppLanguage.ARABIC -> "💡 نصائح CSS"
    }

    val jsFunctionsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "• getConfig(key, defaultValue) - 获取用户配置\n• __MODULE_INFO__ - 模块信息对象\n• __MODULE_CONFIG__ - 配置值对象"
        AppLanguage.ENGLISH -> "• getConfig(key, defaultValue) - Get user config\n• __MODULE_INFO__ - Module info object\n• __MODULE_CONFIG__ - Config values object"
        AppLanguage.ARABIC -> "• getConfig(key, defaultValue) - الحصول على تكوين المستخدم\n• __MODULE_INFO__ - كائن معلومات الوحدة\n• __MODULE_CONFIG__ - كائن قيم التكوين"
    }

    val cssHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "• CSS 会自动注入到页面 <head>\n• 使用 !important 确保样式生效"
        AppLanguage.ENGLISH -> "• CSS will be auto-injected into page <head>\n• Use !important to ensure styles take effect"
        AppLanguage.ARABIC -> "• سيتم حقن CSS تلقائيًا في <head> الصفحة\n• استخدم !important لضمان تطبيق الأنماط"
    }

    val javascriptCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "JavaScript 代码"
        AppLanguage.ENGLISH -> "JavaScript Code"
        AppLanguage.ARABIC -> "كود JavaScript"
    }

    val cssCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS 代码"
        AppLanguage.ENGLISH -> "CSS Code"
        AppLanguage.ARABIC -> "كود CSS"
    }

    val noSpecialPermissions: String get() = when (lang) {
        AppLanguage.CHINESE -> "无特殊权限"
        AppLanguage.ENGLISH -> "No special permissions"
        AppLanguage.ARABIC -> "لا توجد أذونات خاصة"
    }

    val urlMatchRules: String get() = when (lang) {
        AppLanguage.CHINESE -> "URL match rules"
        AppLanguage.ENGLISH -> "URL Match Rules"
        AppLanguage.ARABIC -> "قواعد مطابقة URL"
    }

    val matchAllWebsites: String get() = when (lang) {
        AppLanguage.CHINESE -> "匹配所有网站"
        AppLanguage.ENGLISH -> "Match all websites"
        AppLanguage.ARABIC -> "مطابقة جميع المواقع"
    }

    val rulesCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 条规则"
        AppLanguage.ENGLISH -> "%d rules"
        AppLanguage.ARABIC -> "%d قواعد"
    }

    val userConfigItems: String get() = when (lang) {
        AppLanguage.CHINESE -> "用户配置项"
        AppLanguage.ENGLISH -> "User Config Items"
        AppLanguage.ARABIC -> "عناصر تكوين المستخدم"
    }

    val noConfigItems: String get() = when (lang) {
        AppLanguage.CHINESE -> "无可配置项"
        AppLanguage.ENGLISH -> "No config items"
        AppLanguage.ARABIC -> "لا توجد عناصر تكوين"
    }

    val configItemsCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个配置项"
        AppLanguage.ENGLISH -> "%d config items"
        AppLanguage.ARABIC -> "%d عناصر تكوين"
    }

    val developerGuide: String get() = when (lang) {
        AppLanguage.CHINESE -> "📚 开发指南"
        AppLanguage.ENGLISH -> "📚 Developer Guide"
        AppLanguage.ARABIC -> "📚 دليل المطور"
    }

    val developerGuideContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "• URL 匹配：留空则在所有网站生效\n• 配置项：让用户自定义模块行为\n• 权限声明：告知用户模块需要的能力\n• 执行时机：控制代码何时运行"
        AppLanguage.ENGLISH -> "• URL Match: Leave empty to apply on all websites\n• Config Items: Let users customize module behavior\n• Permissions: Inform users of required capabilities\n• Run Time: Control when code runs"
        AppLanguage.ARABIC -> "• مطابقة URL: اتركه فارغًا للتطبيق على جميع المواقع\n• عناصر التكوين: السماح للمستخدمين بتخصيص سلوك الوحدة\n• الأذونات: إعلام المستخدمين بالقدرات المطلوبة\n• وقت التشغيل: التحكم في وقت تشغيل الكود"
    }

    val regex: String get() = when (lang) {
        AppLanguage.CHINESE -> "正则"
        AppLanguage.ENGLISH -> "Regex"
        AppLanguage.ARABIC -> "تعبير نمطي"
    }

    val exclude: String get() = when (lang) {
        AppLanguage.CHINESE -> "排除"
        AppLanguage.ENGLISH -> "Exclude"
        AppLanguage.ARABIC -> "استبعاد"
    }

    val include: String get() = when (lang) {
        AppLanguage.CHINESE -> "包含"
        AppLanguage.ENGLISH -> "Include"
        AppLanguage.ARABIC -> "تضمين"
    }

    val description: String get() = when (lang) {
        AppLanguage.CHINESE -> "描述"
        AppLanguage.ENGLISH -> "Description"
        AppLanguage.ARABIC -> "الوصف"
    }

    val briefModuleDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "简要描述模块功能"
        AppLanguage.ENGLISH -> "Brief description of module function"
        AppLanguage.ARABIC -> "وصف موجز لوظيفة الوحدة"
    }

    val tags: String get() = when (lang) {
        AppLanguage.CHINESE -> "标签"
        AppLanguage.ENGLISH -> "Tags"
        AppLanguage.ARABIC -> "العلامات"
    }

    val tagsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "用逗号分隔，如：广告, 屏蔽, 工具"
        AppLanguage.ENGLISH -> "Comma separated, e.g.: ads, block, tools"
        AppLanguage.ARABIC -> "مفصولة بفواصل، مثال: إعلانات، حظر، أدوات"
    }

    val yourName: String get() = when (lang) {
        AppLanguage.CHINESE -> "你的名字"
        AppLanguage.ENGLISH -> "Your name"
        AppLanguage.ARABIC -> "اسمك"
    }

    val keyNameRequired: String get() = when (lang) {
        AppLanguage.CHINESE -> "键名 *"
        AppLanguage.ENGLISH -> "Key Name *"
        AppLanguage.ARABIC -> "اسم المفتاح *"
    }

    val keyNameHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "如: fontSize"
        AppLanguage.ENGLISH -> "e.g.: fontSize"
        AppLanguage.ARABIC -> "مثال: fontSize"
    }

    val displayNameRequired: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示名称 *"
        AppLanguage.ENGLISH -> "Display Name *"
        AppLanguage.ARABIC -> "اسم العرض *"
    }

    val displayNameHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "如: 字体大小"
        AppLanguage.ENGLISH -> "e.g.: Font Size"
        AppLanguage.ARABIC -> "مثال: حجم الخط"
    }

    val configDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "说明"
        AppLanguage.ENGLISH -> "Description"
        AppLanguage.ARABIC -> "الوصف"
    }

    val configDescriptionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "配置项的说明文字"
        AppLanguage.ENGLISH -> "Description text for config item"
        AppLanguage.ARABIC -> "نص وصف عنصر التكوين"
    }

    val configType: String get() = when (lang) {
        AppLanguage.CHINESE -> "类型"
        AppLanguage.ENGLISH -> "Type"
        AppLanguage.ARABIC -> "النوع"
    }

    val defaultValue: String get() = when (lang) {
        AppLanguage.CHINESE -> "默认值"
        AppLanguage.ENGLISH -> "Default Value"
        AppLanguage.ARABIC -> "القيمة الافتراضية"
    }
    // ==================== Module Categories ====================
    val catContentFilter: String get() = when (lang) {
        AppLanguage.CHINESE -> "内容过滤"
        AppLanguage.ENGLISH -> "Content Filter"
        AppLanguage.ARABIC -> "تصفية المحتوى"
    }

    val catContentFilterDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏蔽元素、广告过滤、内容隐藏"
        AppLanguage.ENGLISH -> "Block elements, ad filtering, content hiding"
        AppLanguage.ARABIC -> "حظر العناصر، تصفية الإعلانات، إخفاء المحتوى"
    }

    val catContentEnhance: String get() = when (lang) {
        AppLanguage.CHINESE -> "内容增强"
        AppLanguage.ENGLISH -> "Content Enhance"
        AppLanguage.ARABIC -> "تحسين المحتوى"
    }

    val catContentEnhanceDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "内容优化、排版美化、阅读增强"
        AppLanguage.ENGLISH -> "Content optimization, typography beautification, reading enhancement"
        AppLanguage.ARABIC -> "تحسين المحتوى، تجميل التخطيط، تحسين القراءة"
    }

    val catStyleModifier: String get() = when (lang) {
        AppLanguage.CHINESE -> "样式修改"
        AppLanguage.ENGLISH -> "Style Modifier"
        AppLanguage.ARABIC -> "معدل الأنماط"
    }

    val catStyleModifierDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义CSS、主题美化、界面调整"
        AppLanguage.ENGLISH -> "Custom CSS, theme beautification, interface adjustment"
        AppLanguage.ARABIC -> "CSS مخصص، تجميل السمة، تعديل الواجهة"
    }

    val catTheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "主题美化"
        AppLanguage.ENGLISH -> "Theme"
        AppLanguage.ARABIC -> "السمة"
    }

    val catThemeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "深色模式、配色方案、字体替换"
        AppLanguage.ENGLISH -> "Dark mode, color schemes, font replacement"
        AppLanguage.ARABIC -> "الوضع الداكن، مخططات الألوان، استبدال الخط"
    }

    val catFunctionEnhance: String get() = when (lang) {
        AppLanguage.CHINESE -> "功能增强"
        AppLanguage.ENGLISH -> "Function Enhance"
        AppLanguage.ARABIC -> "تحسين الوظائف"
    }

    val catFunctionEnhanceDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动化操作、快捷功能、效率工具"
        AppLanguage.ENGLISH -> "Automation, shortcuts, efficiency tools"
        AppLanguage.ARABIC -> "الأتمتة، الاختصارات، أدوات الكفاءة"
    }

    val catAutomation: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动化"
        AppLanguage.ENGLISH -> "Automation"
        AppLanguage.ARABIC -> "الأتمتة"
    }

    val catAutomationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动点击、自动填表、定时任务"
        AppLanguage.ENGLISH -> "Auto click, auto fill, scheduled tasks"
        AppLanguage.ARABIC -> "النقر التلقائي، الملء التلقائي، المهام المجدولة"
    }

    val catNavigation: String get() = when (lang) {
        AppLanguage.CHINESE -> "导航辅助"
        AppLanguage.ENGLISH -> "Navigation"
        AppLanguage.ARABIC -> "المساعدة في التنقل"
    }

    val catNavigationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "返回顶部、快速跳转、页面导航"
        AppLanguage.ENGLISH -> "Back to top, quick jump, page navigation"
        AppLanguage.ARABIC -> "العودة للأعلى، القفز السريع، التنقل بين الصفحات"
    }

    val catDataExtract: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据提取"
        AppLanguage.ENGLISH -> "Data Extract"
        AppLanguage.ARABIC -> "استخراج البيانات"
    }

    val catDataExtractDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "内容抓取、数据导出、信息收集"
        AppLanguage.ENGLISH -> "Content scraping, data export, info collection"
        AppLanguage.ARABIC -> "استخراج المحتوى، تصدير البيانات، جمع المعلومات"
    }

    val catDataSave: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据保存"
        AppLanguage.ENGLISH -> "Data Save"
        AppLanguage.ARABIC -> "حفظ البيانات"
    }

    val catDataSaveDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面保存、截图、内容导出"
        AppLanguage.ENGLISH -> "Page save, screenshot, content export"
        AppLanguage.ARABIC -> "حفظ الصفحة، لقطة الشاشة، تصدير المحتوى"
    }

    val catInteraction: String get() = when (lang) {
        AppLanguage.CHINESE -> "交互增强"
        AppLanguage.ENGLISH -> "Interaction"
        AppLanguage.ARABIC -> "تحسين التفاعل"
    }

    val catInteractionDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "手势操作、快捷键、自动填表"
        AppLanguage.ENGLISH -> "Gestures, shortcuts, auto fill"
        AppLanguage.ARABIC -> "الإيماءات، الاختصارات، الملء التلقائي"
    }

    val catAccessibility: String get() = when (lang) {
        AppLanguage.CHINESE -> "无障碍"
        AppLanguage.ENGLISH -> "Accessibility"
        AppLanguage.ARABIC -> "إمكانية الوصول"
    }

    val catAccessibilityDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "辅助阅读、语音朗读、高对比度"
        AppLanguage.ENGLISH -> "Assistive reading, text-to-speech, high contrast"
        AppLanguage.ARABIC -> "القراءة المساعدة، تحويل النص إلى كلام، التباين العالي"
    }

    val catMedia: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体处理"
        AppLanguage.ENGLISH -> "Media"
        AppLanguage.ARABIC -> "معالجة الوسائط"
    }

    val catMediaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频增强、图片处理、音频控制"
        AppLanguage.ENGLISH -> "Video enhance, image processing, audio control"
        AppLanguage.ARABIC -> "تحسين الفيديو، معالجة الصور، التحكم في الصوت"
    }

    val catVideo: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频增强"
        AppLanguage.ENGLISH -> "Video"
        AppLanguage.ARABIC -> "تحسين الفيديو"
    }

    val catVideoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "倍速播放、画中画、视频下载"
        AppLanguage.ENGLISH -> "Speed control, PiP, video download"
        AppLanguage.ARABIC -> "التحكم في السرعة، صورة في صورة، تنزيل الفيديو"
    }

    val catImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片处理"
        AppLanguage.ENGLISH -> "Image"
        AppLanguage.ARABIC -> "معالجة الصور"
    }

    val catImageDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片放大、批量下载、懒加载"
        AppLanguage.ENGLISH -> "Image zoom, batch download, lazy load"
        AppLanguage.ARABIC -> "تكبير الصور، التنزيل الدفعي، التحميل الكسول"
    }

    val catAudio: String get() = when (lang) {
        AppLanguage.CHINESE -> "音频控制"
        AppLanguage.ENGLISH -> "Audio"
        AppLanguage.ARABIC -> "التحكم في الصوت"
    }

    val catAudioDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "音量控制、音频提取、播放增强"
        AppLanguage.ENGLISH -> "Volume control, audio extract, playback enhance"
        AppLanguage.ARABIC -> "التحكم في الصوت، استخراج الصوت، تحسين التشغيل"
    }

    val catSecurity: String get() = when (lang) {
        AppLanguage.CHINESE -> "安全隐私"
        AppLanguage.ENGLISH -> "Security"
        AppLanguage.ARABIC -> "الأمان والخصوصية"
    }

    val catSecurityDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐私保护、指纹防护、追踪拦截"
        AppLanguage.ENGLISH -> "Privacy protection, fingerprint defense, tracking block"
        AppLanguage.ARABIC -> "حماية الخصوصية، الدفاع عن البصمات، حظر التتبع"
    }

    val catAntiTracking: String get() = when (lang) {
        AppLanguage.CHINESE -> "反追踪"
        AppLanguage.ENGLISH -> "Anti-Tracking"
        AppLanguage.ARABIC -> "مكافحة التتبع"
    }

    val catAntiTrackingDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止追踪、Cookie管理、隐私模式"
        AppLanguage.ENGLISH -> "Block tracking, cookie management, privacy mode"
        AppLanguage.ARABIC -> "حظر التتبع، إدارة الكوكيز، وضع الخصوصية"
    }

    val catSocial: String get() = when (lang) {
        AppLanguage.CHINESE -> "社交增强"
        AppLanguage.ENGLISH -> "Social"
        AppLanguage.ARABIC -> "تحسين التواصل الاجتماعي"
    }

    val catSocialDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "评论过滤、社交优化、消息增强"
        AppLanguage.ENGLISH -> "Comment filter, social optimization, message enhance"
        AppLanguage.ARABIC -> "تصفية التعليقات، تحسين التواصل، تحسين الرسائل"
    }

    val catShopping: String get() = when (lang) {
        AppLanguage.CHINESE -> "购物助手"
        AppLanguage.ENGLISH -> "Shopping"
        AppLanguage.ARABIC -> "مساعد التسوق"
    }

    val catShoppingDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "比价工具、优惠提醒、历史价格"
        AppLanguage.ENGLISH -> "Price compare, deal alerts, price history"
        AppLanguage.ARABIC -> "مقارنة الأسعار، تنبيهات العروض، سجل الأسعار"
    }

    val catReading: String get() = when (lang) {
        AppLanguage.CHINESE -> "阅读模式"
        AppLanguage.ENGLISH -> "Reading"
        AppLanguage.ARABIC -> "وضع القراءة"
    }

    val catReadingDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "正文提取、排版优化、护眼模式"
        AppLanguage.ENGLISH -> "Content extract, typography, eye care mode"
        AppLanguage.ARABIC -> "استخراج المحتوى، التخطيط، وضع حماية العين"
    }

    val catTranslate: String get() = when (lang) {
        AppLanguage.CHINESE -> "翻译工具"
        AppLanguage.ENGLISH -> "Translate"
        AppLanguage.ARABIC -> "أدوات الترجمة"
    }

    val catTranslateDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "划词翻译、全文翻译、多语言"
        AppLanguage.ENGLISH -> "Selection translate, full page translate, multi-language"
        AppLanguage.ARABIC -> "ترجمة التحديد، ترجمة الصفحة الكاملة، متعدد اللغات"
    }

    val catDeveloper: String get() = when (lang) {
        AppLanguage.CHINESE -> "开发调试"
        AppLanguage.ENGLISH -> "Developer"
        AppLanguage.ARABIC -> "أدوات المطور"
    }

    val catDeveloperDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "调试工具、性能监控、日志输出"
        AppLanguage.ENGLISH -> "Debug tools, performance monitor, log output"
        AppLanguage.ARABIC -> "أدوات التصحيح، مراقبة الأداء، إخراج السجل"
    }

    val catOther: String get() = when (lang) {
        AppLanguage.CHINESE -> "其他"
        AppLanguage.ENGLISH -> "Other"
        AppLanguage.ARABIC -> "أخرى"
    }

    val catOtherDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "未分类的扩展模块"
        AppLanguage.ENGLISH -> "Uncategorized extension modules"
        AppLanguage.ARABIC -> "وحدات إضافية غير مصنفة"
    }

    val catUiEnhance: String get() = when (lang) {
        AppLanguage.CHINESE -> "UI增强"
        AppLanguage.ENGLISH -> "UI Enhance"
        AppLanguage.ARABIC -> "تحسين الواجهة"
    }

    val catPrivacySecurity: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐私安全"
        AppLanguage.ENGLISH -> "Privacy & Security"
        AppLanguage.ARABIC -> "الخصوصية والأمان"
    }

    val catTools: String get() = when (lang) {
        AppLanguage.CHINESE -> "工具"
        AppLanguage.ENGLISH -> "Tools"
        AppLanguage.ARABIC -> "أدوات"
    }

    val catAdBlock: String get() = when (lang) {
        AppLanguage.CHINESE -> "广告拦截"
        AppLanguage.ENGLISH -> "Ad Block"
        AppLanguage.ARABIC -> "حظر الإعلانات"
    }
    // ==================== Module Templates ====================
    val tplElementHider: String get() = when (lang) {
        AppLanguage.CHINESE -> "元素隐藏器"
        AppLanguage.ENGLISH -> "Element Hider"
        AppLanguage.ARABIC -> "إخفاء العناصر"
    }

    val tplElementHiderDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "通过 CSS 选择器隐藏页面元素，支持多种隐藏方式"
        AppLanguage.ENGLISH -> "Hide page elements via CSS selector, supports multiple hiding methods"
        AppLanguage.ARABIC -> "إخفاء عناصر الصفحة عبر محدد CSS"
    }

    val tplAdBlocker: String get() = when (lang) {
        AppLanguage.CHINESE -> "广告拦截增强"
        AppLanguage.ENGLISH -> "Ad Blocker Pro"
        AppLanguage.ARABIC -> "مانع الإعلانات المتقدم"
    }

    val tplAdBlockerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "强力广告过滤，屏蔽常见广告元素、弹窗和追踪脚本"
        AppLanguage.ENGLISH -> "Powerful ad filter, blocks common ad elements, popups and tracking scripts"
        AppLanguage.ARABIC -> "فلتر إعلانات قوي، يحظر عناصر الإعلانات والنوافذ المنبثقة"
    }

    val tplPopupBlocker: String get() = when (lang) {
        AppLanguage.CHINESE -> "弹窗拦截器"
        AppLanguage.ENGLISH -> "Popup Blocker"
        AppLanguage.ARABIC -> "مانع النوافذ المنبثقة"
    }

    val tplPopupBlockerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动关闭烦人的弹窗、对话框和通知请求"
        AppLanguage.ENGLISH -> "Auto close annoying popups, dialogs and notification requests"
        AppLanguage.ARABIC -> "إغلاق تلقائي للنوافذ المنبثقة المزعجة"
    }

    val tplCookieBanner: String get() = when (lang) {
        AppLanguage.CHINESE -> "Cookie横幅移除"
        AppLanguage.ENGLISH -> "Cookie Banner Remover"
        AppLanguage.ARABIC -> "إزالة لافتة ملفات تعريف الارتباط"
    }

    val tplCookieBannerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动移除Cookie同意横幅和GDPR提示"
        AppLanguage.ENGLISH -> "Auto remove cookie consent banners and GDPR notices"
        AppLanguage.ARABIC -> "إزالة تلقائية للافتات الموافقة على ملفات تعريف الارتباط"
    }

    val tplCssInjector: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS样式注入"
        AppLanguage.ENGLISH -> "CSS Style Injector"
        AppLanguage.ARABIC -> "حقن أنماط CSS"
    }

    val tplCssInjectorDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "向页面注入自定义CSS样式"
        AppLanguage.ENGLISH -> "Inject custom CSS styles into page"
        AppLanguage.ARABIC -> "حقن أنماط CSS مخصصة في الصفحة"
    }

    val tplDarkMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制深色模式"
        AppLanguage.ENGLISH -> "Force Dark Mode"
        AppLanguage.ARABIC -> "فرض الوضع الداكن"
    }

    val tplDarkModeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "为任意网页强制启用深色模式"
        AppLanguage.ENGLISH -> "Force enable dark mode for any webpage"
        AppLanguage.ARABIC -> "فرض تمكين الوضع الداكن لأي صفحة ويب"
    }

    val tplFontChanger: String get() = when (lang) {
        AppLanguage.CHINESE -> "字体替换器"
        AppLanguage.ENGLISH -> "Font Changer"
        AppLanguage.ARABIC -> "مغير الخط"
    }

    val tplFontChangerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "替换网页字体为指定字体"
        AppLanguage.ENGLISH -> "Replace webpage fonts with specified font"
        AppLanguage.ARABIC -> "استبدال خطوط صفحة الويب بالخط المحدد"
    }

    val tplScrollToTop: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动到顶部"
        AppLanguage.ENGLISH -> "Scroll to Top"
        AppLanguage.ARABIC -> "التمرير إلى الأعلى"
    }

    val tplScrollToTopDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加一键回到页面顶部按钮"
        AppLanguage.ENGLISH -> "Add a button to scroll back to top"
        AppLanguage.ARABIC -> "إضافة زر للعودة إلى أعلى الصفحة"
    }

    val templateColorTheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "配色主题"
        AppLanguage.ENGLISH -> "Color Theme"
        AppLanguage.ARABIC -> "نظام الألوان"
    }

    val templateColorThemeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义页面配色方案"
        AppLanguage.ENGLISH -> "Customize page color scheme"
        AppLanguage.ARABIC -> "تخصيص نظام ألوان الصفحة"
    }

    val templateBgColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "背景色"
        AppLanguage.ENGLISH -> "Background Color"
        AppLanguage.ARABIC -> "لون الخلفية"
    }

    val templateTextColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "文字色"
        AppLanguage.ENGLISH -> "Text Color"
        AppLanguage.ARABIC -> "لون النص"
    }

    val templateLinkColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "链接色"
        AppLanguage.ENGLISH -> "Link Color"
        AppLanguage.ARABIC -> "لون الرابط"
    }

    val templateLayoutFixer: String get() = when (lang) {
        AppLanguage.CHINESE -> "布局修复器"
        AppLanguage.ENGLISH -> "Layout Fixer"
        AppLanguage.ARABIC -> "مصلح التخطيط"
    }

    val templateLayoutFixerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复页面布局问题，如宽度限制、居中等"
        AppLanguage.ENGLISH -> "Fix page layout issues like width limits, centering, etc."
        AppLanguage.ARABIC -> "إصلاح مشاكل تخطيط الصفحة مثل حدود العرض والتوسيط"
    }

    val templateMaxWidth: String get() = when (lang) {
        AppLanguage.CHINESE -> "最大宽度(px)"
        AppLanguage.ENGLISH -> "Max Width (px)"
        AppLanguage.ARABIC -> "الحد الأقصى للعرض (بكسل)"
    }

    val templateCenterContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "内容居中"
        AppLanguage.ENGLISH -> "Center Content"
        AppLanguage.ARABIC -> "توسيط المحتوى"
    }

    val templateAutoClicker: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动点击器"
        AppLanguage.ENGLISH -> "Auto Clicker"
        AppLanguage.ARABIC -> "النقر التلقائي"
    }

    val templateAutoClickerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动点击指定元素，如关闭按钮、确认按钮等"
        AppLanguage.ENGLISH -> "Auto-click specified elements like close buttons, confirm buttons, etc."
        AppLanguage.ARABIC -> "النقر التلقائي على العناصر المحددة مثل أزرار الإغلاق والتأكيد"
    }

    val templateClickTarget: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击目标"
        AppLanguage.ENGLISH -> "Click Target"
        AppLanguage.ARABIC -> "هدف النقر"
    }

    val templateDelay: String get() = when (lang) {
        AppLanguage.CHINESE -> "延迟(ms)"
        AppLanguage.ENGLISH -> "Delay (ms)"
        AppLanguage.ARABIC -> "التأخير (مللي ثانية)"
    }

    val templateRepeatClick: String get() = when (lang) {
        AppLanguage.CHINESE -> "重复点击"
        AppLanguage.ENGLISH -> "Repeat Click"
        AppLanguage.ARABIC -> "تكرار النقر"
    }

    val templateFormFiller: String get() = when (lang) {
        AppLanguage.CHINESE -> "表单自动填充"
        AppLanguage.ENGLISH -> "Form Auto-Fill"
        AppLanguage.ARABIC -> "الملء التلقائي للنموذج"
    }

    val templateFormFillerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动填充表单字段"
        AppLanguage.ENGLISH -> "Auto-fill form fields"
        AppLanguage.ARABIC -> "ملء حقول النموذج تلقائياً"
    }

    val templateFieldSelector: String get() = when (lang) {
        AppLanguage.CHINESE -> "字段选择器"
        AppLanguage.ENGLISH -> "Field Selector"
        AppLanguage.ARABIC -> "محدد الحقل"
    }

    val templateFieldValue: String get() = when (lang) {
        AppLanguage.CHINESE -> "填充值"
        AppLanguage.ENGLISH -> "Fill Value"
        AppLanguage.ARABIC -> "قيمة الملء"
    }

    val templatePageModifier: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面内容修改"
        AppLanguage.ENGLISH -> "Page Content Modifier"
        AppLanguage.ARABIC -> "معدل محتوى الصفحة"
    }

    val templatePageModifierDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "修改页面文本或属性"
        AppLanguage.ENGLISH -> "Modify page text or attributes"
        AppLanguage.ARABIC -> "تعديل نص الصفحة أو السمات"
    }

    val templateTargetSelector: String get() = when (lang) {
        AppLanguage.CHINESE -> "目标选择器"
        AppLanguage.ENGLISH -> "Target Selector"
        AppLanguage.ARABIC -> "محدد الهدف"
    }

    val templateNewText: String get() = when (lang) {
        AppLanguage.CHINESE -> "新文本"
        AppLanguage.ENGLISH -> "New Text"
        AppLanguage.ARABIC -> "نص جديد"
    }

    val templateNewStyle: String get() = when (lang) {
        AppLanguage.CHINESE -> "新样式"
        AppLanguage.ENGLISH -> "New Style"
        AppLanguage.ARABIC -> "نمط جديد"
    }

    val templateCustomButton: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义悬浮按钮"
        AppLanguage.ENGLISH -> "Custom Floating Button"
        AppLanguage.ARABIC -> "زر عائم مخصص"
    }

    val templateCustomButtonDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加可自定义功能的悬浮按钮"
        AppLanguage.ENGLISH -> "Add a floating button with customizable function"
        AppLanguage.ARABIC -> "إضافة زر عائم بوظيفة قابلة للتخصيص"
    }

    val templateButtonText: String get() = when (lang) {
        AppLanguage.CHINESE -> "按钮文字"
        AppLanguage.ENGLISH -> "Button Text"
        AppLanguage.ARABIC -> "نص الزر"
    }

    val templateClickAction: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击动作"
        AppLanguage.ENGLISH -> "Click Action"
        AppLanguage.ARABIC -> "إجراء النقر"
    }

    val templatePosition: String get() = when (lang) {
        AppLanguage.CHINESE -> "位置"
        AppLanguage.ENGLISH -> "Position"
        AppLanguage.ARABIC -> "الموضع"
    }

    val templateKeyboardShortcuts: String get() = when (lang) {
        AppLanguage.CHINESE -> "键盘快捷键"
        AppLanguage.ENGLISH -> "Keyboard Shortcuts"
        AppLanguage.ARABIC -> "اختصارات لوحة المفاتيح"
    }

    val templateKeyboardShortcutsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加自定义键盘快捷键"
        AppLanguage.ENGLISH -> "Add custom keyboard shortcuts"
        AppLanguage.ARABIC -> "إضافة اختصارات لوحة مفاتيح مخصصة"
    }

    val templateShortcutsConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "快捷键配置"
        AppLanguage.ENGLISH -> "Shortcuts Config"
        AppLanguage.ARABIC -> "تكوين الاختصارات"
    }

    val templateShortcutsConfigDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "格式：键=动作，每行一个"
        AppLanguage.ENGLISH -> "Format: key=action, one per line"
        AppLanguage.ARABIC -> "الصيغة: مفتاح=إجراء، واحد في كل سطر"
    }

    val templateExtractAttrDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "留空则提取文本"
        AppLanguage.ENGLISH -> "Leave empty to extract text"
        AppLanguage.ARABIC -> "اتركه فارغاً لاستخراج النص"
    }

    val templateFilterKeywordDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "仅收集包含关键词的链接"
        AppLanguage.ENGLISH -> "Only collect links containing keyword"
        AppLanguage.ARABIC -> "جمع الروابط التي تحتوي على الكلمة المفتاحية فقط"
    }
    // ==================== Module Permissions ====================
    val permDomAccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "DOM 访问"
        AppLanguage.ENGLISH -> "DOM Access"
        AppLanguage.ARABIC -> "وصول DOM"
    }

    val permDomAccessDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "读取和修改页面元素"
        AppLanguage.ENGLISH -> "Read and modify page elements"
        AppLanguage.ARABIC -> "قراءة وتعديل عناصر الصفحة"
    }

    val permDomObserve: String get() = when (lang) {
        AppLanguage.CHINESE -> "DOM 监听"
        AppLanguage.ENGLISH -> "DOM Observe"
        AppLanguage.ARABIC -> "مراقبة DOM"
    }

    val permDomObserveDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监听页面元素变化"
        AppLanguage.ENGLISH -> "Listen for page element changes"
        AppLanguage.ARABIC -> "الاستماع لتغييرات عناصر الصفحة"
    }

    val permCssInject: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS 注入"
        AppLanguage.ENGLISH -> "CSS Inject"
        AppLanguage.ARABIC -> "حقن CSS"
    }

    val permCssInjectDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "向页面注入样式"
        AppLanguage.ENGLISH -> "Inject styles into page"
        AppLanguage.ARABIC -> "حقن الأنماط في الصفحة"
    }

    val permStorage: String get() = when (lang) {
        AppLanguage.CHINESE -> "本地存储"
        AppLanguage.ENGLISH -> "Local Storage"
        AppLanguage.ARABIC -> "التخزين المحلي"
    }

    val permStorageDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "读写 localStorage/sessionStorage"
        AppLanguage.ENGLISH -> "Read/write localStorage/sessionStorage"
        AppLanguage.ARABIC -> "قراءة/كتابة التخزين المحلي"
    }

    val permCookie: String get() = when (lang) {
        AppLanguage.CHINESE -> "Cookie"
        AppLanguage.ENGLISH -> "Cookie"
        AppLanguage.ARABIC -> "ملفات تعريف الارتباط"
    }

    val permCookieDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "读写 Cookie"
        AppLanguage.ENGLISH -> "Read/write Cookie"
        AppLanguage.ARABIC -> "قراءة/كتابة ملفات تعريف الارتباط"
    }

    val permIndexedDb: String get() = when (lang) {
        AppLanguage.CHINESE -> "IndexedDB"
        AppLanguage.ENGLISH -> "IndexedDB"
        AppLanguage.ARABIC -> "IndexedDB"
    }

    val permIndexedDbDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "访问 IndexedDB 数据库"
        AppLanguage.ENGLISH -> "Access IndexedDB database"
        AppLanguage.ARABIC -> "الوصول إلى قاعدة بيانات IndexedDB"
    }

    val permCache: String get() = when (lang) {
        AppLanguage.CHINESE -> "缓存控制"
        AppLanguage.ENGLISH -> "Cache Control"
        AppLanguage.ARABIC -> "التحكم في التخزين المؤقت"
    }

    val permCacheDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "管理浏览器缓存"
        AppLanguage.ENGLISH -> "Manage browser cache"
        AppLanguage.ARABIC -> "إدارة ذاكرة التخزين المؤقت"
    }

    val permNetwork: String get() = when (lang) {
        AppLanguage.CHINESE -> "网络请求"
        AppLanguage.ENGLISH -> "Network Request"
        AppLanguage.ARABIC -> "طلب الشبكة"
    }

    val permNetworkDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "发送 HTTP 请求"
        AppLanguage.ENGLISH -> "Send HTTP requests"
        AppLanguage.ARABIC -> "إرسال طلبات HTTP"
    }

    val permWebsocket: String get() = when (lang) {
        AppLanguage.CHINESE -> "WebSocket"
        AppLanguage.ENGLISH -> "WebSocket"
        AppLanguage.ARABIC -> "WebSocket"
    }

    val permWebsocketDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "建立 WebSocket 连接"
        AppLanguage.ENGLISH -> "Establish WebSocket connection"
        AppLanguage.ARABIC -> "إنشاء اتصال WebSocket"
    }

    val permFetchIntercept: String get() = when (lang) {
        AppLanguage.CHINESE -> "请求拦截"
        AppLanguage.ENGLISH -> "Request Intercept"
        AppLanguage.ARABIC -> "اعتراض الطلبات"
    }

    val permFetchInterceptDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截和修改网络请求"
        AppLanguage.ENGLISH -> "Intercept and modify network requests"
        AppLanguage.ARABIC -> "اعتراض وتعديل طلبات الشبكة"
    }

    val permClipboard: String get() = when (lang) {
        AppLanguage.CHINESE -> "剪贴板"
        AppLanguage.ENGLISH -> "Clipboard"
        AppLanguage.ARABIC -> "الحافظة"
    }

    val permClipboardDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "读写剪贴板内容"
        AppLanguage.ENGLISH -> "Read/write clipboard content"
        AppLanguage.ARABIC -> "قراءة/كتابة محتوى الحافظة"
    }

    val permNotification: String get() = when (lang) {
        AppLanguage.CHINESE -> "通知"
        AppLanguage.ENGLISH -> "Notification"
        AppLanguage.ARABIC -> "الإشعارات"
    }

    val permNotificationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示通知消息"
        AppLanguage.ENGLISH -> "Show notification messages"
        AppLanguage.ARABIC -> "عرض رسائل الإشعارات"
    }

    val permAlert: String get() = when (lang) {
        AppLanguage.CHINESE -> "弹窗"
        AppLanguage.ENGLISH -> "Alert"
        AppLanguage.ARABIC -> "تنبيه"
    }

    val permAlertDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示 alert/confirm/prompt"
        AppLanguage.ENGLISH -> "Show alert/confirm/prompt"
        AppLanguage.ARABIC -> "عرض تنبيه/تأكيد/مطالبة"
    }

    val permKeyboard: String get() = when (lang) {
        AppLanguage.CHINESE -> "键盘监听"
        AppLanguage.ENGLISH -> "Keyboard Listen"
        AppLanguage.ARABIC -> "الاستماع للوحة المفاتيح"
    }

    val permKeyboardDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监听键盘事件"
        AppLanguage.ENGLISH -> "Listen for keyboard events"
        AppLanguage.ARABIC -> "الاستماع لأحداث لوحة المفاتيح"
    }

    val permMouse: String get() = when (lang) {
        AppLanguage.CHINESE -> "鼠标监听"
        AppLanguage.ENGLISH -> "Mouse Listen"
        AppLanguage.ARABIC -> "الاستماع للماوس"
    }

    val permMouseDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监听鼠标事件"
        AppLanguage.ENGLISH -> "Listen for mouse events"
        AppLanguage.ARABIC -> "الاستماع لأحداث الماوس"
    }

    val permTouch: String get() = when (lang) {
        AppLanguage.CHINESE -> "触摸监听"
        AppLanguage.ENGLISH -> "Touch Listen"
        AppLanguage.ARABIC -> "الاستماع للمس"
    }

    val permTouchDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监听触摸事件"
        AppLanguage.ENGLISH -> "Listen for touch events"
        AppLanguage.ARABIC -> "الاستماع لأحداث اللمس"
    }

    val permLocation: String get() = when (lang) {
        AppLanguage.CHINESE -> "位置信息"
        AppLanguage.ENGLISH -> "Location"
        AppLanguage.ARABIC -> "الموقع"
    }

    val permLocationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取地理位置"
        AppLanguage.ENGLISH -> "Get geolocation"
        AppLanguage.ARABIC -> "الحصول على الموقع الجغرافي"
    }

    val permCamera: String get() = when (lang) {
        AppLanguage.CHINESE -> "摄像头"
        AppLanguage.ENGLISH -> "Camera"
        AppLanguage.ARABIC -> "الكاميرا"
    }

    val permCameraDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "访问摄像头"
        AppLanguage.ENGLISH -> "Access camera"
        AppLanguage.ARABIC -> "الوصول إلى الكاميرا"
    }

    val permMicrophone: String get() = when (lang) {
        AppLanguage.CHINESE -> "麦克风"
        AppLanguage.ENGLISH -> "Microphone"
        AppLanguage.ARABIC -> "الميكروفون"
    }

    val permMicrophoneDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "访问麦克风"
        AppLanguage.ENGLISH -> "Access microphone"
        AppLanguage.ARABIC -> "الوصول إلى الميكروفون"
    }

    val permDeviceInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "设备信息"
        AppLanguage.ENGLISH -> "Device Info"
        AppLanguage.ARABIC -> "معلومات الجهاز"
    }

    val permDeviceInfoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取设备信息"
        AppLanguage.ENGLISH -> "Get device information"
        AppLanguage.ARABIC -> "الحصول على معلومات الجهاز"
    }

    val permMedia: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体控制"
        AppLanguage.ENGLISH -> "Media Control"
        AppLanguage.ARABIC -> "التحكم في الوسائط"
    }

    val permMediaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "控制音视频播放"
        AppLanguage.ENGLISH -> "Control audio/video playback"
        AppLanguage.ARABIC -> "التحكم في تشغيل الصوت/الفيديو"
    }

    val permFullscreen: String get() = when (lang) {
        AppLanguage.CHINESE -> "全屏控制"
        AppLanguage.ENGLISH -> "Fullscreen Control"
        AppLanguage.ARABIC -> "التحكم في ملء الشاشة"
    }

    val permFullscreenDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "控制全屏模式"
        AppLanguage.ENGLISH -> "Control fullscreen mode"
        AppLanguage.ARABIC -> "التحكم في وضع ملء الشاشة"
    }

    val permPip: String get() = when (lang) {
        AppLanguage.CHINESE -> "画中画"
        AppLanguage.ENGLISH -> "Picture-in-Picture"
        AppLanguage.ARABIC -> "صورة داخل صورة"
    }

    val permPipDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用画中画模式"
        AppLanguage.ENGLISH -> "Enable picture-in-picture mode"
        AppLanguage.ARABIC -> "تفعيل وضع الصورة داخل الصورة"
    }

    val permScreenCapture: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏幕截图"
        AppLanguage.ENGLISH -> "Screen Capture"
        AppLanguage.ARABIC -> "لقطة الشاشة"
    }

    val permScreenCaptureDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "截取页面内容"
        AppLanguage.ENGLISH -> "Capture page content"
        AppLanguage.ARABIC -> "التقاط محتوى الصفحة"
    }

    val permDownload: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载"
        AppLanguage.ENGLISH -> "Download"
        AppLanguage.ARABIC -> "تحميل"
    }

    val permDownloadDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "触发文件下载"
        AppLanguage.ENGLISH -> "Trigger file download"
        AppLanguage.ARABIC -> "تشغيل تحميل الملف"
    }

    val permFileAccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "文件访问"
        AppLanguage.ENGLISH -> "File Access"
        AppLanguage.ARABIC -> "الوصول إلى الملفات"
    }

    val permFileAccessDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "访问本地文件"
        AppLanguage.ENGLISH -> "Access local files"
        AppLanguage.ARABIC -> "الوصول إلى الملفات المحلية"
    }

    val permEval: String get() = when (lang) {
        AppLanguage.CHINESE -> "动态执行"
        AppLanguage.ENGLISH -> "Dynamic Eval"
        AppLanguage.ARABIC -> "التنفيذ الديناميكي"
    }

    val permEvalDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "执行动态代码"
        AppLanguage.ENGLISH -> "Execute dynamic code"
        AppLanguage.ARABIC -> "تنفيذ الكود الديناميكي"
    }

    val permIframe: String get() = when (lang) {
        AppLanguage.CHINESE -> "iframe 访问"
        AppLanguage.ENGLISH -> "iframe Access"
        AppLanguage.ARABIC -> "وصول iframe"
    }

    val permIframeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "访问 iframe 内容"
        AppLanguage.ENGLISH -> "Access iframe content"
        AppLanguage.ARABIC -> "الوصول إلى محتوى iframe"
    }

    val permWindowOpen: String get() = when (lang) {
        AppLanguage.CHINESE -> "新窗口"
        AppLanguage.ENGLISH -> "New Window"
        AppLanguage.ARABIC -> "نافذة جديدة"
    }

    val permWindowOpenDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "打开新窗口/标签页"
        AppLanguage.ENGLISH -> "Open new window/tab"
        AppLanguage.ARABIC -> "فتح نافذة/علامة تبويب جديدة"
    }

    val permHistory: String get() = when (lang) {
        AppLanguage.CHINESE -> "历史记录"
        AppLanguage.ENGLISH -> "History"
        AppLanguage.ARABIC -> "السجل"
    }

    val permHistoryDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "访问浏览历史"
        AppLanguage.ENGLISH -> "Access browsing history"
        AppLanguage.ARABIC -> "الوصول إلى سجل التصفح"
    }

    val permNavigation: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面导航"
        AppLanguage.ENGLISH -> "Navigation"
        AppLanguage.ARABIC -> "التنقل"
    }

    val permNavigationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "控制页面跳转"
        AppLanguage.ENGLISH -> "Control page navigation"
        AppLanguage.ARABIC -> "التحكم في تنقل الصفحة"
    }
    // ==================== Config Item Types ====================
    val configTypeText: String get() = when (lang) {
        AppLanguage.CHINESE -> "文本"
        AppLanguage.ENGLISH -> "Text"
        AppLanguage.ARABIC -> "نص"
    }

    val configTypeTextDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "单行文本输入"
        AppLanguage.ENGLISH -> "Single-line text input"
        AppLanguage.ARABIC -> "إدخال نص من سطر واحد"
    }

    val configTypeTextarea: String get() = when (lang) {
        AppLanguage.CHINESE -> "多行文本"
        AppLanguage.ENGLISH -> "Textarea"
        AppLanguage.ARABIC -> "نص متعدد الأسطر"
    }

    val configTypeTextareaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "多行文本输入，适合代码或长文本"
        AppLanguage.ENGLISH -> "Multi-line text input, for code or long text"
        AppLanguage.ARABIC -> "إدخال نص متعدد الأسطر، للكود أو النص الطويل"
    }

    val configTypeNumber: String get() = when (lang) {
        AppLanguage.CHINESE -> "数字"
        AppLanguage.ENGLISH -> "Number"
        AppLanguage.ARABIC -> "رقم"
    }

    val configTypeNumberDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "数字输入，支持整数和小数"
        AppLanguage.ENGLISH -> "Number input, supports integers and decimals"
        AppLanguage.ARABIC -> "إدخال رقم، يدعم الأعداد الصحيحة والعشرية"
    }

    val configTypeBoolean: String get() = when (lang) {
        AppLanguage.CHINESE -> "开关"
        AppLanguage.ENGLISH -> "Switch"
        AppLanguage.ARABIC -> "مفتاح"
    }

    val configTypeBooleanDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "是/否 开关选择"
        AppLanguage.ENGLISH -> "Yes/No switch selection"
        AppLanguage.ARABIC -> "اختيار نعم/لا"
    }

    val configTypeSelect: String get() = when (lang) {
        AppLanguage.CHINESE -> "单选"
        AppLanguage.ENGLISH -> "Select"
        AppLanguage.ARABIC -> "اختيار"
    }

    val configTypeSelectDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "下拉单选列表"
        AppLanguage.ENGLISH -> "Dropdown single-select list"
        AppLanguage.ARABIC -> "قائمة منسدلة للاختيار الفردي"
    }

    val configTypeMultiSelect: String get() = when (lang) {
        AppLanguage.CHINESE -> "多选"
        AppLanguage.ENGLISH -> "Multi-Select"
        AppLanguage.ARABIC -> "اختيار متعدد"
    }

    val configTypeMultiSelectDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "多选列表"
        AppLanguage.ENGLISH -> "Multi-select list"
        AppLanguage.ARABIC -> "قائمة اختيار متعدد"
    }

    val configTypeRadio: String get() = when (lang) {
        AppLanguage.CHINESE -> "单选按钮"
        AppLanguage.ENGLISH -> "Radio"
        AppLanguage.ARABIC -> "زر راديو"
    }

    val configTypeRadioDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "单选按钮组"
        AppLanguage.ENGLISH -> "Radio button group"
        AppLanguage.ARABIC -> "مجموعة أزرار راديو"
    }

    val configTypeCheckbox: String get() = when (lang) {
        AppLanguage.CHINESE -> "复选框"
        AppLanguage.ENGLISH -> "Checkbox"
        AppLanguage.ARABIC -> "مربع اختيار"
    }

    val configTypeCheckboxDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "复选框组"
        AppLanguage.ENGLISH -> "Checkbox group"
        AppLanguage.ARABIC -> "مجموعة مربعات اختيار"
    }

    val configTypeColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "颜色"
        AppLanguage.ENGLISH -> "Color"
        AppLanguage.ARABIC -> "لون"
    }

    val configTypeColorDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "颜色选择器"
        AppLanguage.ENGLISH -> "Color picker"
        AppLanguage.ARABIC -> "منتقي الألوان"
    }

    val configTypeUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "网址"
        AppLanguage.ENGLISH -> "URL"
        AppLanguage.ARABIC -> "رابط"
    }

    val configTypeUrlDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "URL 输入，带格式验证"
        AppLanguage.ENGLISH -> "URL input with format validation"
        AppLanguage.ARABIC -> "إدخال رابط مع التحقق من التنسيق"
    }

    val configTypeEmail: String get() = when (lang) {
        AppLanguage.CHINESE -> "邮箱"
        AppLanguage.ENGLISH -> "Email"
        AppLanguage.ARABIC -> "بريد إلكتروني"
    }

    val configTypeEmailDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "邮箱输入，带格式验证"
        AppLanguage.ENGLISH -> "Email input with format validation"
        AppLanguage.ARABIC -> "إدخال بريد إلكتروني مع التحقق من التنسيق"
    }

    val configTypePassword: String get() = when (lang) {
        AppLanguage.CHINESE -> "密码"
        AppLanguage.ENGLISH -> "Password"
        AppLanguage.ARABIC -> "كلمة مرور"
    }

    val configTypePasswordDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "密码输入，内容隐藏"
        AppLanguage.ENGLISH -> "Password input, content hidden"
        AppLanguage.ARABIC -> "إدخال كلمة مرور، المحتوى مخفي"
    }

    val configTypeRegex: String get() = when (lang) {
        AppLanguage.CHINESE -> "正则表达式"
        AppLanguage.ENGLISH -> "Regex"
        AppLanguage.ARABIC -> "تعبير نمطي"
    }

    val configTypeRegexDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "正则表达式输入"
        AppLanguage.ENGLISH -> "Regular expression input"
        AppLanguage.ARABIC -> "إدخال تعبير نمطي"
    }

    val configTypeCssSelector: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS选择器"
        AppLanguage.ENGLISH -> "CSS Selector"
        AppLanguage.ARABIC -> "محدد CSS"
    }

    val configTypeCssSelectorDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS 选择器输入"
        AppLanguage.ENGLISH -> "CSS selector input"
        AppLanguage.ARABIC -> "إدخال محدد CSS"
    }

    val configTypeJavascript: String get() = when (lang) {
        AppLanguage.CHINESE -> "JavaScript"
        AppLanguage.ENGLISH -> "JavaScript"
        AppLanguage.ARABIC -> "جافا سكريبت"
    }

    val configTypeJavascriptDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "JavaScript 代码输入"
        AppLanguage.ENGLISH -> "JavaScript code input"
        AppLanguage.ARABIC -> "إدخال كود جافا سكريبت"
    }

    val configTypeJson: String get() = when (lang) {
        AppLanguage.CHINESE -> "JSON"
        AppLanguage.ENGLISH -> "JSON"
        AppLanguage.ARABIC -> "JSON"
    }

    val configTypeJsonDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "JSON 格式数据输入"
        AppLanguage.ENGLISH -> "JSON format data input"
        AppLanguage.ARABIC -> "إدخال بيانات بتنسيق JSON"
    }

    val configTypeRange: String get() = when (lang) {
        AppLanguage.CHINESE -> "滑块"
        AppLanguage.ENGLISH -> "Range"
        AppLanguage.ARABIC -> "شريط تمرير"
    }

    val configTypeRangeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "数值范围滑块"
        AppLanguage.ENGLISH -> "Numeric range slider"
        AppLanguage.ARABIC -> "شريط تمرير نطاق رقمي"
    }

    val configTypeDate: String get() = when (lang) {
        AppLanguage.CHINESE -> "日期"
        AppLanguage.ENGLISH -> "Date"
        AppLanguage.ARABIC -> "تاريخ"
    }

    val configTypeDateDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "日期选择"
        AppLanguage.ENGLISH -> "Date picker"
        AppLanguage.ARABIC -> "منتقي التاريخ"
    }

    val configTypeTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "时间"
        AppLanguage.ENGLISH -> "Time"
        AppLanguage.ARABIC -> "وقت"
    }

    val configTypeTimeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "时间选择"
        AppLanguage.ENGLISH -> "Time picker"
        AppLanguage.ARABIC -> "منتقي الوقت"
    }

    val configTypeDatetime: String get() = when (lang) {
        AppLanguage.CHINESE -> "日期时间"
        AppLanguage.ENGLISH -> "DateTime"
        AppLanguage.ARABIC -> "تاريخ ووقت"
    }

    val configTypeDatetimeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "日期时间选择"
        AppLanguage.ENGLISH -> "DateTime picker"
        AppLanguage.ARABIC -> "منتقي التاريخ والوقت"
    }

    val configTypeFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "文件"
        AppLanguage.ENGLISH -> "File"
        AppLanguage.ARABIC -> "ملف"
    }

    val configTypeFileDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "文件选择"
        AppLanguage.ENGLISH -> "File picker"
        AppLanguage.ARABIC -> "منتقي الملفات"
    }

    val configTypeImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片"
        AppLanguage.ENGLISH -> "Image"
        AppLanguage.ARABIC -> "صورة"
    }

    val configTypeImageDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片选择/上传"
        AppLanguage.ENGLISH -> "Image picker/upload"
        AppLanguage.ARABIC -> "منتقي/رفع الصور"
    }
}
