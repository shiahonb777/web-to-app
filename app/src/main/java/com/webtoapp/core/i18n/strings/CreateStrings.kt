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
}
