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
}
