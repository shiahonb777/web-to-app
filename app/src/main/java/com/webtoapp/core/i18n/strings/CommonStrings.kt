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
    // ==================== Team Collaboration ====================
    val teamTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "团队管理"
        AppLanguage.ENGLISH -> "Teams"
        AppLanguage.ARABIC -> "الفرق"
    }

    val teamCreate: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建团队"
        AppLanguage.ENGLISH -> "Create Team"
        AppLanguage.ARABIC -> "إنشاء فريق"
    }

    val teamName: String get() = when (lang) {
        AppLanguage.CHINESE -> "团队名称"
        AppLanguage.ENGLISH -> "Team Name"
        AppLanguage.ARABIC -> "اسم الفريق"
    }

    val teamDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "团队简介"
        AppLanguage.ENGLISH -> "Description"
        AppLanguage.ARABIC -> "الوصف"
    }

    val teamMembers: String get() = when (lang) {
        AppLanguage.CHINESE -> "成员"
        AppLanguage.ENGLISH -> "Members"
        AppLanguage.ARABIC -> "الأعضاء"
    }

    val teamInvite: String get() = when (lang) {
        AppLanguage.CHINESE -> "邀请成员"
        AppLanguage.ENGLISH -> "Invite"
        AppLanguage.ARABIC -> "دعوة"
    }

    val teamUsername: String get() = when (lang) {
        AppLanguage.CHINESE -> "用户名"
        AppLanguage.ENGLISH -> "Username"
        AppLanguage.ARABIC -> "اسم المستخدم"
    }

    val teamRole: String get() = when (lang) {
        AppLanguage.CHINESE -> "角色"
        AppLanguage.ENGLISH -> "Role"
        AppLanguage.ARABIC -> "الدور"
    }

    val teamRoleOwner: String get() = when (lang) {
        AppLanguage.CHINESE -> "所有者"
        AppLanguage.ENGLISH -> "Owner"
        AppLanguage.ARABIC -> "المالك"
    }

    val teamRoleAdmin: String get() = when (lang) {
        AppLanguage.CHINESE -> "管理员"
        AppLanguage.ENGLISH -> "Admin"
        AppLanguage.ARABIC -> "مسؤول"
    }

    val teamRoleEditor: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑"
        AppLanguage.ENGLISH -> "Editor"
        AppLanguage.ARABIC -> "محرر"
    }

    val teamRoleViewer: String get() = when (lang) {
        AppLanguage.CHINESE -> "查看者"
        AppLanguage.ENGLISH -> "Viewer"
        AppLanguage.ARABIC -> "مشاهد"
    }

    val teamDelete: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除团队"
        AppLanguage.ENGLISH -> "Delete Team"
        AppLanguage.ARABIC -> "حذف الفريق"
    }

    val teamEmpty: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无团队\n创建您的第一个团队"
        AppLanguage.ENGLISH -> "No teams yet\nCreate your first team"
        AppLanguage.ARABIC -> "لا توجد فرق بعد\nأنشئ فريقك الأول"
    }

    val teamQuota: String get() = when (lang) {
        AppLanguage.CHINESE -> "团队配额"
        AppLanguage.ENGLISH -> "Team Quota"
        AppLanguage.ARABIC -> "حصة الفرق"
    }

    val teamSearch: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索团队"
        AppLanguage.ENGLISH -> "Search Teams"
        AppLanguage.ARABIC -> "البحث عن فرق"
    }

    val teamDiscover: String get() = when (lang) {
        AppLanguage.CHINESE -> "发现团队"
        AppLanguage.ENGLISH -> "Discover"
        AppLanguage.ARABIC -> "اكتشاف"
    }

    val teamMyTeams: String get() = when (lang) {
        AppLanguage.CHINESE -> "我的团队"
        AppLanguage.ENGLISH -> "My Teams"
        AppLanguage.ARABIC -> "فرقي"
    }

    val teamJoin: String get() = when (lang) {
        AppLanguage.CHINESE -> "申请加入"
        AppLanguage.ENGLISH -> "Join"
        AppLanguage.ARABIC -> "انضمام"
    }

    val teamJoinMessage: String get() = when (lang) {
        AppLanguage.CHINESE -> "附言 (可选)"
        AppLanguage.ENGLISH -> "Message (optional)"
        AppLanguage.ARABIC -> "رسالة (اختياري)"
    }

    val teamJoinPending: String get() = when (lang) {
        AppLanguage.CHINESE -> "等待审核"
        AppLanguage.ENGLISH -> "Pending"
        AppLanguage.ARABIC -> "في الانتظار"
    }

    val teamJoinApprove: String get() = when (lang) {
        AppLanguage.CHINESE -> "通过"
        AppLanguage.ENGLISH -> "Approve"
        AppLanguage.ARABIC -> "موافقة"
    }

    val teamJoinReject: String get() = when (lang) {
        AppLanguage.CHINESE -> "拒绝"
        AppLanguage.ENGLISH -> "Reject"
        AppLanguage.ARABIC -> "رفض"
    }

    val teamJoinRequests: String get() = when (lang) {
        AppLanguage.CHINESE -> "加入申请"
        AppLanguage.ENGLISH -> "Join Requests"
        AppLanguage.ARABIC -> "طلبات الانضمام"
    }

    val teamJoinNoRequests: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无待审核的申请"
        AppLanguage.ENGLISH -> "No pending requests"
        AppLanguage.ARABIC -> "لا توجد طلبات معلقة"
    }

    val teamRanking: String get() = when (lang) {
        AppLanguage.CHINESE -> "贡献排名"
        AppLanguage.ENGLISH -> "Ranking"
        AppLanguage.ARABIC -> "الترتيب"
    }

    val teamContribution: String get() = when (lang) {
        AppLanguage.CHINESE -> "贡献值"
        AppLanguage.ENGLISH -> "Contribution"
        AppLanguage.ARABIC -> "المساهمة"
    }

    val teamSearchEmpty: String get() = when (lang) {
        AppLanguage.CHINESE -> "没有找到匹配的团队"
        AppLanguage.ENGLISH -> "No teams found"
        AppLanguage.ARABIC -> "لم يتم العثور على فرق"
    }

    val teamJoinSent: String get() = when (lang) {
        AppLanguage.CHINESE -> "申请已发送，等待队长审核"
        AppLanguage.ENGLISH -> "Request sent, waiting for approval"
        AppLanguage.ARABIC -> "تم إرسال الطلب، في انتظار الموافقة"
    }

    val teamJoined: String get() = when (lang) {
        AppLanguage.CHINESE -> "已加入"
        AppLanguage.ENGLISH -> "Joined"
        AppLanguage.ARABIC -> "منضم"
    }

    val teamAssociate: String get() = when (lang) {
        AppLanguage.CHINESE -> "关联团队"
        AppLanguage.ENGLISH -> "Associate Team"
        AppLanguage.ARABIC -> "ربط الفريق"
    }

    val teamLead: String get() = when (lang) {
        AppLanguage.CHINESE -> "主负责人"
        AppLanguage.ENGLISH -> "Lead"
        AppLanguage.ARABIC -> "قائد"
    }

    val teamMemberRole: String get() = when (lang) {
        AppLanguage.CHINESE -> "成员"
        AppLanguage.ENGLISH -> "Member"
        AppLanguage.ARABIC -> "عضو"
    }

    val teamContributionPoints: String get() = when (lang) {
        AppLanguage.CHINESE -> "贡献点"
        AppLanguage.ENGLISH -> "Points"
        AppLanguage.ARABIC -> "نقاط"
    }

    val teamContributionDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "贡献描述"
        AppLanguage.ENGLISH -> "Contribution Description"
        AppLanguage.ARABIC -> "وصف المساهمة"
    }

    val teamWorks: String get() = when (lang) {
        AppLanguage.CHINESE -> "团队作品"
        AppLanguage.ENGLISH -> "Team Works"
        AppLanguage.ARABIC -> "أعمال الفريق"
    }

    val teamSelectTeam: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择团队"
        AppLanguage.ENGLISH -> "Select Team"
        AppLanguage.ARABIC -> "اختر الفريق"
    }

    val teamAddContributor: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加贡献者"
        AppLanguage.ENGLISH -> "Add Contributor"
        AppLanguage.ARABIC -> "إضافة مساهم"
    }

    val teamNoTeams: String get() = when (lang) {
        AppLanguage.CHINESE -> "您还没有加入任何团队"
        AppLanguage.ENGLISH -> "You haven't joined any teams"
        AppLanguage.ARABIC -> "لم تنضم إلى أي فريق"
    }
    // ==================== About Page ====================
    val about: String get() = when (lang) {
        AppLanguage.CHINESE -> "关于"
        AppLanguage.ENGLISH -> "About"
        AppLanguage.ARABIC -> "حول"
    }

    val independentDeveloper: String get() = when (lang) {
        AppLanguage.CHINESE -> "独立开发者 · AI 爱好者"
        AppLanguage.ENGLISH -> "Independent Developer · AI Enthusiast"
        AppLanguage.ARABIC -> "مطور مستقل · متحمس للذكاء الاصطناعي"
    }

    val checkUpdate: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查更新"
        AppLanguage.ENGLISH -> "Check Update"
        AppLanguage.ARABIC -> "التحقق من التحديثات"
    }

    val checking: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在检查..."
        AppLanguage.ENGLISH -> "Checking..."
        AppLanguage.ARABIC -> "جاري التحقق..."
    }

    val downloading: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在下载..."
        AppLanguage.ENGLISH -> "Downloading..."
        AppLanguage.ARABIC -> "جاري التحميل..."
    }

    val currentVersion: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前版本"
        AppLanguage.ENGLISH -> "Current Version"
        AppLanguage.ARABIC -> "الإصدار الحالي"
    }

    val aboutThisApp: String get() = when (lang) {
        AppLanguage.CHINESE -> "关于这个应用"
        AppLanguage.ENGLISH -> "About This App"
        AppLanguage.ARABIC -> "حول هذا التطبيق"
    }

    val aboutAppDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "WebToApp 是我独立开发的一款工具，可以将网站、图片、视频快速转换成独立的 Android 应用。\n\n如果你有任何问题、建议或想法，欢迎随时联系我！"
        AppLanguage.ENGLISH -> "WebToApp is a tool I independently developed that can quickly convert websites, images, and videos into standalone Android apps.\n\nIf you have any questions, suggestions, or ideas, feel free to contact me!"
        AppLanguage.ARABIC -> "WebToApp هي أداة طورتها بشكل مستقل يمكنها تحويل المواقع والصور ومقاطع الفيديو بسرعة إلى تطبيقات Android مستقلة.\n\nإذا كان لديك أي أسئلة أو اقتراحات أو أفكار، لا تتردد في الاتصال بي!"
    }

    val socialMedia: String get() = when (lang) {
        AppLanguage.CHINESE -> "社交媒体"
        AppLanguage.ENGLISH -> "Social Media"
        AppLanguage.ARABIC -> "وسائل التواصل الاجتماعي"
    }

    val exchangeGroup: String get() = when (lang) {
        AppLanguage.CHINESE -> "交流群"
        AppLanguage.ENGLISH -> "Community Group"
        AppLanguage.ARABIC -> "مجموعة المجتمع"
    }

    val videoTutorial: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频教程"
        AppLanguage.ENGLISH -> "Video Tutorial"
        AppLanguage.ARABIC -> "فيديو تعليمي"
    }

    val openSourceRepo: String get() = when (lang) {
        AppLanguage.CHINESE -> "开源仓库"
        AppLanguage.ENGLISH -> "Open Source Repo"
        AppLanguage.ARABIC -> "مستودع مفتوح المصدر"
    }

    val joinExchangeGroup: String get() = when (lang) {
        AppLanguage.CHINESE -> "加入交流群"
        AppLanguage.ENGLISH -> "Join Community"
        AppLanguage.ARABIC -> "انضم إلى المجتمع"
    }

    val learnProgressTogether: String get() = when (lang) {
        AppLanguage.CHINESE -> "一起学习进步，获取最新消息 🚀"
        AppLanguage.ENGLISH -> "Learn together, get latest news 🚀"
        AppLanguage.ARABIC -> "تعلم معًا، احصل على آخر الأخبار 🚀"
    }

    val exchangeLearning: String get() = when (lang) {
        AppLanguage.CHINESE -> "交流学习、更新消息"
        AppLanguage.ENGLISH -> "Exchange learning, update news"
        AppLanguage.ARABIC -> "تبادل التعلم، تحديث الأخبار"
    }

    val internationalGroup: String get() = when (lang) {
        AppLanguage.CHINESE -> "国际用户交流群"
        AppLanguage.ENGLISH -> "International user group"
        AppLanguage.ARABIC -> "مجموعة المستخدمين الدوليين"
    }

    val contactAuthor: String get() = when (lang) {
        AppLanguage.CHINESE -> "联系作者"
        AppLanguage.ENGLISH -> "Contact Author"
        AppLanguage.ARABIC -> "الاتصال بالمؤلف"
    }

    val feedbackCooperation: String get() = when (lang) {
        AppLanguage.CHINESE -> "问题反馈、合作咨询、功能建议 💬"
        AppLanguage.ENGLISH -> "Feedback, cooperation, feature suggestions 💬"
        AppLanguage.ARABIC -> "ملاحظات، تعاون، اقتراحات الميزات 💬"
    }

    val feedbackCooperationShort: String get() = when (lang) {
        AppLanguage.CHINESE -> "问题反馈、合作咨询"
        AppLanguage.ENGLISH -> "Feedback, cooperation"
        AppLanguage.ARABIC -> "ملاحظات، تعاون"
    }

    val emailContact: String get() = when (lang) {
        AppLanguage.CHINESE -> "邮件联系"
        AppLanguage.ENGLISH -> "Email Contact"
        AppLanguage.ARABIC -> "الاتصال بالبريد الإلكتروني"
    }

    val internationalEmail: String get() = when (lang) {
        AppLanguage.CHINESE -> "国际邮件"
        AppLanguage.ENGLISH -> "International Email"
        AppLanguage.ARABIC -> "البريد الإلكتروني الدولي"
    }

    val updateLater: String get() = when (lang) {
        AppLanguage.CHINESE -> "稍后更新"
        AppLanguage.ENGLISH -> "Update Later"
        AppLanguage.ARABIC -> "التحديث لاحقًا"
    }

    val downloadComplete: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载完成，正在安装..."
        AppLanguage.ENGLISH -> "Download complete, installing..."
        AppLanguage.ARABIC -> "اكتمل التحميل، جاري التثبيت..."
    }

    val checkUpdateFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查更新失败"
        AppLanguage.ENGLISH -> "Check update failed"
        AppLanguage.ARABIC -> "فشل التحقق من التحديثات"
    }

    val autoCheckUpdate: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动检查更新"
        AppLanguage.ENGLISH -> "Auto Check Updates"
        AppLanguage.ARABIC -> "التحقق التلقائي من التحديثات"
    }

    val autoCheckUpdateDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动时自动检测新版本"
        AppLanguage.ENGLISH -> "Automatically check for new versions on launch"
        AppLanguage.ARABIC -> "التحقق تلقائيًا من الإصدارات الجديدة عند التشغيل"
    }

    val openSourceRepository: String get() = when (lang) {
        AppLanguage.CHINESE -> "开源仓库"
        AppLanguage.ENGLISH -> "Open Source Repository"
        AppLanguage.ARABIC -> "مستودع مفتوح المصدر"
    }

    val videoTutorialLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频教程"
        AppLanguage.ENGLISH -> "Video Tutorial"
        AppLanguage.ARABIC -> "فيديو تعليمي"
    }

    val okButton: String get() = when (lang) {
        AppLanguage.CHINESE -> "好的"
        AppLanguage.ENGLISH -> "OK"
        AppLanguage.ARABIC -> "حسنًا"
    }

    val updateLaterButton: String get() = when (lang) {
        AppLanguage.CHINESE -> "稍后更新"
        AppLanguage.ENGLISH -> "Update Later"
        AppLanguage.ARABIC -> "التحديث لاحقًا"
    }
    // ==================== Extra Strings ====================
    val seconds: String get() = when (lang) {
        AppLanguage.CHINESE -> "秒"
        AppLanguage.ENGLISH -> "seconds"
        AppLanguage.ARABIC -> "ثواني"
    }

    val allowClickToSkip: String get() = when (lang) {
        AppLanguage.CHINESE -> "允许点击跳过"
        AppLanguage.ENGLISH -> "Allow click to skip"
        AppLanguage.ARABIC -> "السماح بالنقر للتخطي"
    }

    val hotSearch: String get() = when (lang) {
        AppLanguage.CHINESE -> "热门搜索"
        AppLanguage.ENGLISH -> "Hot Search"
        AppLanguage.ARABIC -> "البحث الشائع"
    }

    val searchHistory: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索历史"
        AppLanguage.ENGLISH -> "Search History"
        AppLanguage.ARABIC -> "سجل البحث"
    }

    val musicSource: String get() = when (lang) {
        AppLanguage.CHINESE -> "音乐来源：网易云音乐"
        AppLanguage.ENGLISH -> "Music source: NetEase Cloud Music"
        AppLanguage.ARABIC -> "مصدر الموسيقى: NetEase Cloud Music"
    }

    val unknownArtist: String get() = when (lang) {
        AppLanguage.CHINESE -> "未知歌手"
        AppLanguage.ENGLISH -> "Unknown Artist"
        AppLanguage.ARABIC -> "فنان غير معروف"
    }

    val downloaded: String get() = when (lang) {
        AppLanguage.CHINESE -> "已下载"
        AppLanguage.ENGLISH -> "Downloaded"
        AppLanguage.ARABIC -> "تم التحميل"
    }

    val downloadFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载失败，请重试"
        AppLanguage.ENGLISH -> "Download failed, please retry"
        AppLanguage.ARABIC -> "فشل التحميل، يرجى المحاولة مرة أخرى"
    }

    val searching: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索中..."
        AppLanguage.ENGLISH -> "Searching..."
        AppLanguage.ARABIC -> "جاري البحث..."
    }

    val randomRecommend: String get() = when (lang) {
        AppLanguage.CHINESE -> "随机推荐"
        AppLanguage.ENGLISH -> "Random Recommend"
        AppLanguage.ARABIC -> "توصية عشوائية"
    }

    val noImageGenModel: String get() = when (lang) {
        AppLanguage.CHINESE -> "未找到支持图像生成的模型"
        AppLanguage.ENGLISH -> "No image generation model found"
        AppLanguage.ARABIC -> "لم يتم العثور على نموذج توليد الصور"
    }

    val addImageGenModelHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "请在「AI设置」中添加模型并标记「图像生成」能力"
        AppLanguage.ENGLISH -> "Please add a model in 'AI Settings' and mark 'Image Generation' capability"
        AppLanguage.ARABIC -> "يرجى إضافة نموذج في 'إعدادات AI' وتحديد قدرة 'توليد الصور'"
    }

    val referenceImages: String get() = when (lang) {
        AppLanguage.CHINESE -> "参考图片（可选，最多3张）"
        AppLanguage.ENGLISH -> "Reference images (optional, max 3)"
        AppLanguage.ARABIC -> "صور مرجعية (اختياري، بحد أقصى 3)"
    }

    val addImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "Add image"
        AppLanguage.ENGLISH -> "Add Image"
        AppLanguage.ARABIC -> "إضافة صورة"
    }

    val generatedIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成的图标"
        AppLanguage.ENGLISH -> "Generated Icon"
        AppLanguage.ARABIC -> "الأيقونة المولدة"
    }

    val presetColors: String get() = when (lang) {
        AppLanguage.CHINESE -> "预设颜色"
        AppLanguage.ENGLISH -> "Preset Colors"
        AppLanguage.ARABIC -> "الألوان المسبقة"
    }

    val customColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义颜色"
        AppLanguage.ENGLISH -> "Custom Color"
        AppLanguage.ARABIC -> "لون مخصص"
    }

    val currentSelection: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前选择"
        AppLanguage.ENGLISH -> "Current Selection"
        AppLanguage.ARABIC -> "الاختيار الحالي"
    }

    val hexColorFormat: String get() = when (lang) {
        AppLanguage.CHINESE -> "6位(RGB)或8位(ARGB)十六进制"
        AppLanguage.ENGLISH -> "6-digit (RGB) or 8-digit (ARGB) hex"
        AppLanguage.ARABIC -> "سداسي عشري 6 أرقام (RGB) أو 8 أرقام (ARGB)"
    }

    val dragToSelectArea: String get() = when (lang) {
        AppLanguage.CHINESE -> "上下拖动选择要截取的区域"
        AppLanguage.ENGLISH -> "Drag up/down to select crop area"
        AppLanguage.ARABIC -> "اسحب لأعلى/لأسفل لتحديد منطقة القص"
    }

    val loadingImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "加载图片中..."
        AppLanguage.ENGLISH -> "Loading image..."
        AppLanguage.ARABIC -> "جاري تحميل الصورة..."
    }

    val cropSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "裁剪尺寸"
        AppLanguage.ENGLISH -> "Crop Size"
        AppLanguage.ARABIC -> "حجم القص"
    }

    val originalSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "原图尺寸"
        AppLanguage.ENGLISH -> "Original Size"
        AppLanguage.ARABIC -> "الحجم الأصلي"
    }

    val statusBarHeight: String get() = when (lang) {
        AppLanguage.CHINESE -> "状态栏高度"
        AppLanguage.ENGLISH -> "Status Bar Height"
        AppLanguage.ARABIC -> "ارتفاع شريط الحالة"
    }

    val restoreDefault: String get() = when (lang) {
        AppLanguage.CHINESE -> "恢复默认"
        AppLanguage.ENGLISH -> "Restore Default"
        AppLanguage.ARABIC -> "استعادة الافتراضي"
    }

    val statusBarPreview: String get() = when (lang) {
        AppLanguage.CHINESE -> "状态栏预览"
        AppLanguage.ENGLISH -> "Status Bar Preview"
        AppLanguage.ARABIC -> "معاينة شريط الحالة"
    }

    val noImageSelected: String get() = when (lang) {
        AppLanguage.CHINESE -> "未选择图片"
        AppLanguage.ENGLISH -> "No image selected"
        AppLanguage.ARABIC -> "لم يتم اختيار صورة"
    }

    val backgroundColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "背景颜色"
        AppLanguage.ENGLISH -> "Background Color"
        AppLanguage.ARABIC -> "لون الخلفية"
    }

    val selectBackgroundImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择背景图片"
        AppLanguage.ENGLISH -> "Select Background Image"
        AppLanguage.ARABIC -> "اختيار صورة الخلفية"
    }

    val imageSelected: String get() = when (lang) {
        AppLanguage.CHINESE -> "已选择图片"
        AppLanguage.ENGLISH -> "Image Selected"
        AppLanguage.ARABIC -> "تم اختيار الصورة"
    }

    val clickToChangeOrClear: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击更换或清除"
        AppLanguage.ENGLISH -> "Click to change or clear"
        AppLanguage.ARABIC -> "انقر للتغيير أو المسح"
    }

    val changeImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "更换图片"
        AppLanguage.ENGLISH -> "Change Image"
        AppLanguage.ARABIC -> "تغيير الصورة"
    }

    val clearImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "清除图片"
        AppLanguage.ENGLISH -> "Clear Image"
        AppLanguage.ARABIC -> "مسح الصورة"
    }

    val backgroundAlpha: String get() = when (lang) {
        AppLanguage.CHINESE -> "背景透明度"
        AppLanguage.ENGLISH -> "Background Alpha"
        AppLanguage.ARABIC -> "شفافية الخلفية"
    }

    val transparent: String get() = when (lang) {
        AppLanguage.CHINESE -> "透明"
        AppLanguage.ENGLISH -> "Transparent"
        AppLanguage.ARABIC -> "شفاف"
    }

    val opaque: String get() = when (lang) {
        AppLanguage.CHINESE -> "不透明"
        AppLanguage.ENGLISH -> "Opaque"
        AppLanguage.ARABIC -> "معتم"
    }

    val inputLyrics: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入歌词"
        AppLanguage.ENGLISH -> "Input Lyrics"
        AppLanguage.ARABIC -> "إدخال كلمات الأغنية"
    }

    val previewConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览确认"
        AppLanguage.ENGLISH -> "Preview Confirm"
        AppLanguage.ARABIC -> "تأكيد المعاينة"
    }

    val inputLyricsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入歌词文本，每行一句："
        AppLanguage.ENGLISH -> "Please enter lyrics text, one line per sentence:"
        AppLanguage.ARABIC -> "يرجى إدخال نص كلمات الأغنية، سطر واحد لكل جملة:"
    }

    val lyricsPlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "在这里粘贴或输入歌词...\n\n示例：\n♪ 前奏\n第一句歌词\n第二句歌词\n♪ 间奏\n继续歌词..."
        AppLanguage.ENGLISH -> "Paste or enter lyrics here...\n\nExample:\n♪ Intro\nFirst line\nSecond line\n♪ Interlude\nContinue lyrics..."
        AppLanguage.ARABIC -> "الصق أو أدخل كلمات الأغنية هنا...\n\nمثال:\n♪ مقدمة\nالسطر الأول\nالسطر الثاني\n♪ فاصل\nمتابعة الكلمات..."
    }

    val totalLyricsLines: String get() = when (lang) {
        AppLanguage.CHINESE -> "共 %d 行歌词"
        AppLanguage.ENGLISH -> "%d lines of lyrics"
        AppLanguage.ARABIC -> "%d سطر من الكلمات"
    }

    val alignmentHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放音频，在听到每句歌词开始时点击「打点」按钮"
        AppLanguage.ENGLISH -> "Play audio, click 'Tap' button when you hear each line start"
        AppLanguage.ARABIC -> "شغل الصوت، انقر على زر 'نقر' عند سماع بداية كل سطر"
    }

    val rewind3s: String get() = when (lang) {
        AppLanguage.CHINESE -> "后退3秒"
        AppLanguage.ENGLISH -> "Rewind 3s"
        AppLanguage.ARABIC -> "إرجاع 3 ثواني"
    }

    val play: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放"
        AppLanguage.ENGLISH -> "Play"
        AppLanguage.ARABIC -> "تشغيل"
    }

    val pause: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂停"
        AppLanguage.ENGLISH -> "Pause"
        AppLanguage.ARABIC -> "إيقاف مؤقت"
    }

    val reTap: String get() = when (lang) {
        AppLanguage.CHINESE -> "重新打点"
        AppLanguage.ENGLISH -> "Re-tap"
        AppLanguage.ARABIC -> "إعادة النقر"
    }

    val undo: String get() = when (lang) {
        AppLanguage.CHINESE -> "撤销"
        AppLanguage.ENGLISH -> "Undo"
        AppLanguage.ARABIC -> "تراجع"
    }

    val redo: String get() = when (lang) {
        AppLanguage.CHINESE -> "重做"
        AppLanguage.ENGLISH -> "Redo"
        AppLanguage.ARABIC -> "إعادة"
    }

    val progress: String get() = when (lang) {
        AppLanguage.CHINESE -> "进度"
        AppLanguage.ENGLISH -> "Progress"
        AppLanguage.ARABIC -> "التقدم"
    }

    val activationSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活成功！"
        AppLanguage.ENGLISH -> "Activation successful!"
        AppLanguage.ARABIC -> "تم التفعيل بنجاح!"
    }

    val copyActivationCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制激活码"
        AppLanguage.ENGLISH -> "Copy Activation Code"
        AppLanguage.ARABIC -> "نسخ رمز التفعيل"
    }

    val noActivationCodes: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无激活码，点击上方按钮添加"
        AppLanguage.ENGLISH -> "No activation codes, click button above to add"
        AppLanguage.ARABIC -> "لا توجد رموز تفعيل، انقر على الزر أعلاه للإضافة"
    }

    val activationTypePermanent: String get() = when (lang) {
        AppLanguage.CHINESE -> "永久激活"
        AppLanguage.ENGLISH -> "Permanent"
        AppLanguage.ARABIC -> "دائم"
    }

    val activationTypePermanentDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活后永久有效，无任何限制"
        AppLanguage.ENGLISH -> "Valid forever after activation, no restrictions"
        AppLanguage.ARABIC -> "صالح إلى الأبد بعد التفعيل، بدون قيود"
    }

    val activationTypeTimeLimited: String get() = when (lang) {
        AppLanguage.CHINESE -> "时间限制"
        AppLanguage.ENGLISH -> "Time Limited"
        AppLanguage.ARABIC -> "محدود بالوقت"
    }

    val activationTypeTimeLimitedDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活后在指定时间内有效"
        AppLanguage.ENGLISH -> "Valid within specified time after activation"
        AppLanguage.ARABIC -> "صالح خلال الوقت المحدد بعد التفعيل"
    }

    val activationTypeUsageLimited: String get() = when (lang) {
        AppLanguage.CHINESE -> "次数限制"
        AppLanguage.ENGLISH -> "Usage Limited"
        AppLanguage.ARABIC -> "محدود بالاستخدام"
    }

    val activationTypeUsageLimitedDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活后可使用指定次数"
        AppLanguage.ENGLISH -> "Can be used specified number of times after activation"
        AppLanguage.ARABIC -> "يمكن استخدامه عدد محدد من المرات بعد التفعيل"
    }

    val activationTypeDeviceBound: String get() = when (lang) {
        AppLanguage.CHINESE -> "设备绑定"
        AppLanguage.ENGLISH -> "Device Bound"
        AppLanguage.ARABIC -> "مرتبط بالجهاز"
    }

    val activationTypeDeviceBoundDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活后绑定到当前设备"
        AppLanguage.ENGLISH -> "Bound to current device after activation"
        AppLanguage.ARABIC -> "مرتبط بالجهاز الحالي بعد التفعيل"
    }

    val activationTypeCombined: String get() = when (lang) {
        AppLanguage.CHINESE -> "组合限制"
        AppLanguage.ENGLISH -> "Combined"
        AppLanguage.ARABIC -> "مجمع"
    }

    val activationTypeCombinedDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "同时支持时间和次数限制"
        AppLanguage.ENGLISH -> "Supports both time and usage limits"
        AppLanguage.ARABIC -> "يدعم قيود الوقت والاستخدام معًا"
    }

    val activated: String get() = when (lang) {
        AppLanguage.CHINESE -> "已激活"
        AppLanguage.ENGLISH -> "Activated"
        AppLanguage.ARABIC -> "مفعل"
    }

    val activationExpired: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活已失效"
        AppLanguage.ENGLISH -> "Activation expired"
        AppLanguage.ARABIC -> "انتهت صلاحية التفعيل"
    }

    val activationTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活时间"
        AppLanguage.ENGLISH -> "Activation Time"
        AppLanguage.ARABIC -> "وقت التفعيل"
    }

    val remainingTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "剩余时间"
        AppLanguage.ENGLISH -> "Remaining Time"
        AppLanguage.ARABIC -> "الوقت المتبقي"
    }

    val expireTime: String get() = when (lang) {
        AppLanguage.CHINESE -> "过期时间"
        AppLanguage.ENGLISH -> "Expire Time"
        AppLanguage.ARABIC -> "وقت الانتهاء"
    }

    val remainingUsage: String get() = when (lang) {
        AppLanguage.CHINESE -> "剩余次数"
        AppLanguage.ENGLISH -> "Remaining Usage"
        AppLanguage.ARABIC -> "الاستخدامات المتبقية"
    }

    val deviceBound: String get() = when (lang) {
        AppLanguage.CHINESE -> "设备绑定：已启用"
        AppLanguage.ENGLISH -> "Device Bound: Enabled"
        AppLanguage.ARABIC -> "ربط الجهاز: مفعل"
    }

    val invalidActivationCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "无效的激活码"
        AppLanguage.ENGLISH -> "Invalid activation code"
        AppLanguage.ARABIC -> "رمز تفعيل غير صالح"
    }

    val activationSuccessHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活码验证通过，即将进入应用"
        AppLanguage.ENGLISH -> "Activation code verified, entering app shortly"
        AppLanguage.ARABIC -> "تم التحقق من رمز التفعيل، جاري الدخول إلى التطبيق"
    }

    val activationSuccessDetail: String get() = when (lang) {
        AppLanguage.CHINESE -> "您的激活码已成功验证，应用功能已解锁"
        AppLanguage.ENGLISH -> "Your activation code has been verified successfully. App features unlocked."
        AppLanguage.ARABIC -> "تم التحقق من رمز التفعيل بنجاح. تم فتح ميزات التطبيق."
    }

    val alreadyActivatedDetail: String get() = when (lang) {
        AppLanguage.CHINESE -> "无需重复激活，您可以直接使用应用的全部功能"
        AppLanguage.ENGLISH -> "No need to activate again, you can use all app features directly"
        AppLanguage.ARABIC -> "لا حاجة للتفعيل مرة أخرى، يمكنك استخدام جميع ميزات التطبيق مباشرة"
    }

    val invalidCodeSuggestion: String get() = when (lang) {
        AppLanguage.CHINESE -> "请检查激活码是否正确，注意区分大小写。如多次失败将暂时锁定输入。"
        AppLanguage.ENGLISH -> "Please check if the activation code is correct. Note it is case-insensitive. Too many failures will temporarily lock input."
        AppLanguage.ARABIC -> "يرجى التحقق مما إذا كان رمز التفعيل صحيحًا. الفشل المتكرر سيقفل الإدخال مؤقتًا."
    }

    val deviceMismatchDetail: String get() = when (lang) {
        AppLanguage.CHINESE -> "此激活码已绑定到其他设备，无法在当前设备上使用"
        AppLanguage.ENGLISH -> "This activation code is bound to another device and cannot be used on this device"
        AppLanguage.ARABIC -> "رمز التفعيل هذا مرتبط بجهاز آخر ولا يمكن استخدامه على هذا الجهاز"
    }

    val deviceMismatchSuggestion: String get() = when (lang) {
        AppLanguage.CHINESE -> "请使用绑定设备登录，或联系开发者获取新的设备绑定激活码。"
        AppLanguage.ENGLISH -> "Please use the bound device, or contact the developer for a new device-bound activation code."
        AppLanguage.ARABIC -> "يرجى استخدام الجهاز المرتبط، أو الاتصال بالمطور للحصول على رمز تفعيل جديد مرتبط بالجهاز."
    }

    val expiredDetail: String get() = when (lang) {
        AppLanguage.CHINESE -> "此激活码的有效期已过，无法继续使用"
        AppLanguage.ENGLISH -> "This activation code has expired and can no longer be used"
        AppLanguage.ARABIC -> "انتهت صلاحية رمز التفعيل هذا ولا يمكن استخدامه بعد الآن"
    }

    val expiredSuggestion: String get() = when (lang) {
        AppLanguage.CHINESE -> "请联系开发者获取新的激活码，或续期当前激活码。"
        AppLanguage.ENGLISH -> "Please contact the developer for a new activation code or to renew the current one."
        AppLanguage.ARABIC -> "يرجى الاتصال بالمطور للحصول على رمز تفعيل جديد أو لتجديد الرمز الحالي."
    }

    val usageExceededDetail: String get() = when (lang) {
        AppLanguage.CHINESE -> "此激活码的可用次数已全部用完"
        AppLanguage.ENGLISH -> "This activation code has used all its available uses"
        AppLanguage.ARABIC -> "استنفد رمز التفعيل هذا جميع استخداماته المتاحة"
    }

    val usageExceededSuggestion: String get() = when (lang) {
        AppLanguage.CHINESE -> "请联系开发者获取新的激活码以继续使用。"
        AppLanguage.ENGLISH -> "Please contact the developer for a new activation code to continue using."
        AppLanguage.ARABIC -> "يرجى الاتصال بالمطور للحصول على رمز تفعيل جديد للمتابعة في الاستخدام."
    }

    val batchGenerate: String get() = when (lang) {
        AppLanguage.CHINESE -> "批量生成"
        AppLanguage.ENGLISH -> "Batch Generate"
        AppLanguage.ARABIC -> "إنشاء دفعي"
    }

    val batchCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成数量"
        AppLanguage.ENGLISH -> "Count"
        AppLanguage.ARABIC -> "العدد"
    }

    val totalCodes: String get() = when (lang) {
        AppLanguage.CHINESE -> "共 %d 个激活码"
        AppLanguage.ENGLISH -> "%d activation codes"
        AppLanguage.ARABIC -> "%d رموز تفعيل"
    }

    val deleteAllCodes: String get() = when (lang) {
        AppLanguage.CHINESE -> "清空全部"
        AppLanguage.ENGLISH -> "Delete All"
        AppLanguage.ARABIC -> "حذف الكل"
    }

    val deleteAllCodesConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定清空所有激活码吗？此操作不可撤销。"
        AppLanguage.ENGLISH -> "Delete all activation codes? This action cannot be undone."
        AppLanguage.ARABIC -> "هل تريد حذف جميع رموز التفعيل؟ لا يمكن التراجع عن هذا الإجراء."
    }

    val copyAllCodes: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制全部"
        AppLanguage.ENGLISH -> "Copy All"
        AppLanguage.ARABIC -> "نسخ الكل"
    }

    val allCodesCopied: String get() = when (lang) {
        AppLanguage.CHINESE -> "所有激活码已复制到剪贴板"
        AppLanguage.ENGLISH -> "All activation codes copied to clipboard"
        AppLanguage.ARABIC -> "تم نسخ جميع رموز التفعيل إلى الحافظة"
    }

    val pleaseEnterActivationCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入激活码"
        AppLanguage.ENGLISH -> "Please enter activation code"
        AppLanguage.ARABIC -> "يرجى إدخال رمز التفعيل"
    }

    val permanentValid: String get() = when (lang) {
        AppLanguage.CHINESE -> "永久有效"
        AppLanguage.ENGLISH -> "Permanently valid"
        AppLanguage.ARABIC -> "صالح بشكل دائم"
    }

    val validityPeriod: String get() = when (lang) {
        AppLanguage.CHINESE -> "有效期"
        AppLanguage.ENGLISH -> "Validity Period"
        AppLanguage.ARABIC -> "فترة الصلاحية"
    }

    val days: String get() = when (lang) {
        AppLanguage.CHINESE -> "天"
        AppLanguage.ENGLISH -> "days"
        AppLanguage.ARABIC -> "أيام"
    }

    val hours: String get() = when (lang) {
        AppLanguage.CHINESE -> "小时"
        AppLanguage.ENGLISH -> "hours"
        AppLanguage.ARABIC -> "ساعات"
    }

    val note: String get() = when (lang) {
        AppLanguage.CHINESE -> "备注"
        AppLanguage.ENGLISH -> "Note"
        AppLanguage.ARABIC -> "ملاحظة"
    }

    val cloneInstallWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "克隆安装仅适用于无签名校验的应用，兼容性较差。建议优先使用「快捷方式」功能。"
        AppLanguage.ENGLISH -> "Clone install only works for apps without signature verification, with limited compatibility. It's recommended to use 'Shortcut' feature instead."
        AppLanguage.ARABIC -> "التثبيت المستنسخ يعمل فقط للتطبيقات بدون التحقق من التوقيع، مع توافق محدود. يُنصح باستخدام ميزة 'الاختصار' بدلاً من ذلك."
    }

    val enableAudioLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用音频"
        AppLanguage.ENGLISH -> "Enable Audio"
        AppLanguage.ARABIC -> "تفعيل الصوت"
    }
    // ==================== Changelog ====================
    val cookiesPersistenceFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增cookies持久化"
        AppLanguage.ENGLISH -> "Cookies persistence feature"
        AppLanguage.ARABIC -> "ميزة حفظ ملفات تعريف الارتباط"
    }

    val multiApiKeyManagement: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增多apikey管理配置"
        AppLanguage.ENGLISH -> "Multi API key management"
        AppLanguage.ARABIC -> "إدارة مفاتيح API المتعددة"
    }

    val modelNameSearchFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增模型名称搜索功能"
        AppLanguage.ENGLISH -> "Model name search feature"
        AppLanguage.ARABIC -> "ميزة البحث عن اسم النموذج"
    }

    val hideUrlPreviewFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增隐藏URL预览功能"
        AppLanguage.ENGLISH -> "Hide URL preview feature"
        AppLanguage.ARABIC -> "ميزة إخفاء معاينة URL"
    }

    val popupBlockerFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增弹窗拦截器功能"
        AppLanguage.ENGLISH -> "Popup blocker feature"
        AppLanguage.ARABIC -> "ميزة حظر النوافذ المنبثقة"
    }

    val optimizeCustomApiEndpoint: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化自定义api端点适配"
        AppLanguage.ENGLISH -> "Optimized custom API endpoint adaptation"
        AppLanguage.ARABIC -> "تحسين تكييف نقطة نهاية API المخصصة"
    }

    val optimizeModelNameDisplay: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化模型名称显示"
        AppLanguage.ENGLISH -> "Optimized model name display"
        AppLanguage.ARABIC -> "تحسين عرض اسم النموذج"
    }

    val optimizeMultiLanguageAdaptation: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化部分内容的多语言适配"
        AppLanguage.ENGLISH -> "Optimized multi-language content adaptation"
        AppLanguage.ARABIC -> "تحسين تكييف المحتوى متعدد اللغات"
    }

    val fixGalleryBuildPath: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复画廊应用构建路径问题"
        AppLanguage.ENGLISH -> "Fixed gallery app build path issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة مسار بناء تطبيق المعرض"
    }

    val fixMicrophonePermission: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复麦克风权限问题"
        AppLanguage.ENGLISH -> "Fixed microphone permission issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة إذن الميكروفون"
    }

    val fixZoomPropertyNotWorking: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复zoom属性不生效问题"
        AppLanguage.ENGLISH -> "Fixed zoom property not working issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة عدم عمل خاصية التكبير"
    }

    val fixActivationCodeLanguage: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复激活码语言显示问题"
        AppLanguage.ENGLISH -> "Fixed activation code language display issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة عرض لغة رمز التفعيل"
    }

    val fixFrontendGalleryFilename: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复前端应用和画廊应用显示文件名问题"
        AppLanguage.ENGLISH -> "Fixed frontend and gallery app filename display issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة عرض اسم ملف التطبيق الأمامي والمعرض"
    }

    val fixCoreConfigEditAppType: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复编辑核心配置功能部分应用类型失效问题"
        AppLanguage.ENGLISH -> "Fixed core config edit for some app types not working"
        AppLanguage.ARABIC -> "إصلاح مشكلة عدم عمل تحرير الإعدادات الأساسية لبعض أنواع التطبيقات"
    }

    val fixKeyboardInitIssue: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复键盘初始化问题"
        AppLanguage.ENGLISH -> "Fixed keyboard initialization issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة تهيئة لوحة المفاتيح"
    }

    val browserEngineFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增浏览器内核功能"
        AppLanguage.ENGLISH -> "Browser engine feature"
        AppLanguage.ARABIC -> "ميزة محرك المتصفح"
    }

    val browserSpoofingFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增浏览器伪装功能"
        AppLanguage.ENGLISH -> "Browser spoofing feature"
        AppLanguage.ARABIC -> "ميزة انتحال المتصفح"
    }

    val hostsBlockFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增 Hosts 拦截功能"
        AppLanguage.ENGLISH -> "Hosts blocking feature"
        AppLanguage.ARABIC -> "ميزة حظر Hosts"
    }

    val longPressMenuFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增长按菜单功能"
        AppLanguage.ENGLISH -> "Long press menu feature"
        AppLanguage.ARABIC -> "ميزة قائمة الضغط المطول"
    }

    val apkArchitectureFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增 APK 架构功能"
        AppLanguage.ENGLISH -> "APK architecture feature"
        AppLanguage.ARABIC -> "ميزة بنية APK"
    }

    val mediaGalleryFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增媒体画廊功能"
        AppLanguage.ENGLISH -> "Media gallery feature"
        AppLanguage.ARABIC -> "ميزة معرض الوسائط"
    }

    val optimizeExtensionModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化扩展模块功能"
        AppLanguage.ENGLISH -> "Optimized extension module"
        AppLanguage.ARABIC -> "تحسين وحدة الإضافات"
    }

    val optimizeEnglishArabicTranslation: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化英文、阿拉伯语翻译支持"
        AppLanguage.ENGLISH -> "Optimized English & Arabic translation"
        AppLanguage.ARABIC -> "تحسين الترجمة الإنجليزية والعربية"
    }

    val optimizeThemeInteraction: String get() = when (lang) {
        AppLanguage.CHINESE -> "强化主题交互与性能"
        AppLanguage.ENGLISH -> "Enhanced theme interaction & performance"
        AppLanguage.ARABIC -> "تحسين تفاعل وأداء السمة"
    }

    val optimizeApiConfigTest: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化 API 配置测试"
        AppLanguage.ENGLISH -> "Optimized API config testing"
        AppLanguage.ARABIC -> "تحسين اختبار تكوين API"
    }

    val fixAppNameSpaces: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复应用名称大量空格问题"
        AppLanguage.ENGLISH -> "Fixed app name excessive spaces issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة المسافات الزائدة في اسم التطبيق"
    }

    val fixAnnouncementJump: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复弹窗公告跳转问题"
        AppLanguage.ENGLISH -> "Fixed announcement popup redirect issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة إعادة توجيه الإعلان المنبثق"
    }

    val fixExternalBrowserCrash: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复调用外部浏览器崩溃问题"
        AppLanguage.ENGLISH -> "Fixed external browser launch crash"
        AppLanguage.ARABIC -> "إصلاح تعطل تشغيل المتصفح الخارجي"
    }

    val fixDownloadError: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复下载错误问题"
        AppLanguage.ENGLISH -> "Fixed download error issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة خطأ التنزيل"
    }

    val fixModuleEditCrash: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复编辑模块时崩溃问题"
        AppLanguage.ENGLISH -> "Fixed module edit crash"
        AppLanguage.ARABIC -> "إصلاح تعطل تحرير الوحدة"
    }

    val fixAiImageInvalid: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复 AI 生成图像无效问题"
        AppLanguage.ENGLISH -> "Fixed AI generated image invalid issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة صورة AI غير الصالحة"
    }

    val fixDownloaderPlayerCooperation: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复下载器与播放器协同处理问题"
        AppLanguage.ENGLISH -> "Fixed downloader and player cooperation issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة تعاون المُنزِّل والمشغل"
    }

    val faviconFetchFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增网页应用图标获取功能"
        AppLanguage.ENGLISH -> "Website favicon fetch feature"
        AppLanguage.ARABIC -> "ميزة جلب أيقونة موقع الويب"
    }

    val randomAppNameFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增随机应用名称功能"
        AppLanguage.ENGLISH -> "Random app name feature"
        AppLanguage.ARABIC -> "ميزة اسم التطبيق العشوائي"
    }

    val multiAppIconFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增多应用图标功能"
        AppLanguage.ENGLISH -> "Multi app icon feature"
        AppLanguage.ARABIC -> "ميزة أيقونات التطبيقات المتعددة"
    }

    val optimizeDataBackup: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化数据备份功能"
        AppLanguage.ENGLISH -> "Optimized data backup feature"
        AppLanguage.ARABIC -> "تحسين ميزة النسخ الاحتياطي للبيانات"
    }

    val optimizeBlackTech: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化黑科技功能"
        AppLanguage.ENGLISH -> "Optimized BlackTech feature"
        AppLanguage.ARABIC -> "تحسين ميزة التقنية السوداء"
    }

    val fixElementBlocker: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复元素屏蔽器问题"
        AppLanguage.ENGLISH -> "Fixed element blocker issues"
        AppLanguage.ARABIC -> "إصلاح مشاكل حاجب العناصر"
    }

    val fixBackgroundRunCrash: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复后台运行功能闪退问题"
        AppLanguage.ENGLISH -> "Fixed background run feature crash"
        AppLanguage.ARABIC -> "إصلاح تعطل ميزة التشغيل في الخلفية"
    }

    val fixI18nStringAdaptation: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复多语言字符串适配问题"
        AppLanguage.ENGLISH -> "Fixed multi-language string adaptation issues"
        AppLanguage.ARABIC -> "إصلاح مشاكل تكييف السلاسل متعددة اللغات"
    }

    val multiLanguageSupport: String get() = when (lang) {
        AppLanguage.CHINESE -> "多语言支持：中文、英文、阿拉伯语"
        AppLanguage.ENGLISH -> "Multi-language support: Chinese, English, Arabic"
        AppLanguage.ARABIC -> "دعم متعدد اللغات: الصينية والإنجليزية والعربية"
    }

    val shareApkFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享APK功能：支持分享已构建的APK文件"
        AppLanguage.ENGLISH -> "Share APK feature: share built APK files"
        AppLanguage.ARABIC -> "ميزة مشاركة APK: مشاركة ملفات APK المبنية"
    }

    val elementBlockerModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "元素屏蔽器扩展模块：可视化屏蔽网页元素"
        AppLanguage.ENGLISH -> "Element blocker module: visually block webpage elements"
        AppLanguage.ARABIC -> "وحدة حجب العناصر: حجب عناصر صفحة الويب بصريًا"
    }

    val forcedRunFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制运行功能：支持应用强制运行模式"
        AppLanguage.ENGLISH -> "Forced run feature: app forced run mode support"
        AppLanguage.ARABIC -> "ميزة التشغيل القسري: دعم وضع التشغيل القسري للتطبيق"
    }

    val linuxOneClickBuild: String get() = when (lang) {
        AppLanguage.CHINESE -> "Linux一键构建前端项目"
        AppLanguage.ENGLISH -> "Linux one-click frontend project build"
        AppLanguage.ARABIC -> "بناء مشروع الواجهة الأمامية بنقرة واحدة على Linux"
    }

    val frontendFrameworkToApk: String get() = when (lang) {
        AppLanguage.CHINESE -> "Vue/React/Vite转APK功能"
        AppLanguage.ENGLISH -> "Vue/React/Vite to APK feature"
        AppLanguage.ARABIC -> "ميزة تحويل Vue/React/Vite إلى APK"
    }

    val optimizeThemeFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化主题功能"
        AppLanguage.ENGLISH -> "Optimized theme functionality"
        AppLanguage.ARABIC -> "تحسين وظيفة السمة"
    }

    val optimizeAboutPageUi: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化关于页面UI"
        AppLanguage.ENGLISH -> "Optimized About page UI"
        AppLanguage.ARABIC -> "تحسين واجهة صفحة حول"
    }

    val fixFullscreenStatusBarIssue: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复全屏模式中状态栏问题"
        AppLanguage.ENGLISH -> "Fix status bar issue in fullscreen mode"
        AppLanguage.ARABIC -> "إصلاح مشكلة شريط الحالة في وضع ملء الشاشة"
    }

    val fixDeviceCrashIssue: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复部分机型/模拟器闪退问题"
        AppLanguage.ENGLISH -> "Fix crash issue on some devices/emulators"
        AppLanguage.ARABIC -> "إصلاح مشكلة التعطل على بعض الأجهزة/المحاكيات"
    }

    val statusBarStyleConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "状态栏样式配置：自定义高度/背景/透明度"
        AppLanguage.ENGLISH -> "Status bar style config: custom height/background/transparency"
        AppLanguage.ARABIC -> "تكوين نمط شريط الحالة: ارتفاع/خلفية/شفافية مخصصة"
    }

    val apkEncryptionProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "APK加密保护：配置和资源文件加密"
        AppLanguage.ENGLISH -> "APK encryption protection: config and resource file encryption"
        AppLanguage.ARABIC -> "حماية تشفير APK: تشفير ملفات التكوين والموارد"
    }

    val bootAutoStartAndScheduled: String get() = when (lang) {
        AppLanguage.CHINESE -> "开机自启动和定时自启动功能"
        AppLanguage.ENGLISH -> "Boot auto-start and scheduled auto-start features"
        AppLanguage.ARABIC -> "ميزات التشغيل التلقائي عند الإقلاع والمجدول"
    }

    val dataBackupExportImport: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据备份：一键导出/导入所有数据"
        AppLanguage.ENGLISH -> "Data backup: one-click export/import all data"
        AppLanguage.ARABIC -> "نسخ البيانات احتياطيًا: تصدير/استيراد جميع البيانات بنقرة واحدة"
    }

    val fullscreenStatusBarOverlay: String get() = when (lang) {
        AppLanguage.CHINESE -> "全屏模式状态栏透明叠加显示"
        AppLanguage.ENGLISH -> "Fullscreen mode status bar transparent overlay"
        AppLanguage.ARABIC -> "تراكب شفاف لشريط الحالة في وضع ملء الشاشة"
    }

    val fullscreenShowStatusBar: String get() = when (lang) {
        AppLanguage.CHINESE -> "全屏模式下可选择显示状态栏"
        AppLanguage.ENGLISH -> "Optional status bar display in fullscreen mode"
        AppLanguage.ARABIC -> "عرض شريط الحالة اختياري في وضع ملء الشاشة"
    }

    val fixHtmlLongPressCopy: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复HTML项目长按文字无法复制"
        AppLanguage.ENGLISH -> "Fix HTML project long press text cannot copy"
        AppLanguage.ARABIC -> "إصلاح عدم إمكانية نسخ النص بالضغط المطول في مشروع HTML"
    }

    val supportAndroid6: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持Android 6.0系统"
        AppLanguage.ENGLISH -> "Support Android 6.0 system"
        AppLanguage.ARABIC -> "دعم نظام Android 6.0"
    }

    val fixHtmlStatusBar: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复HTML应用不显示状态栏的问题"
        AppLanguage.ENGLISH -> "Fix HTML app not showing status bar issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة عدم عرض شريط الحالة في تطبيق HTML"
    }

    val fixEmptyAppName: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复部分系统应用名称显示为空"
        AppLanguage.ENGLISH -> "Fix some system app names showing empty"
        AppLanguage.ARABIC -> "إصلاح عرض أسماء بعض تطبيقات النظام فارغة"
    }

    val fixAiModuleCodeOverlay: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复AI模块开发代码块内容叠加"
        AppLanguage.ENGLISH -> "Fix AI module development code block content overlay"
        AppLanguage.ARABIC -> "إصلاح تراكب محتوى كتلة الكود في تطوير وحدة AI"
    }

    val fixAiHtmlToolCallFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复AI HTML编程工具调用失败"
        AppLanguage.ENGLISH -> "Fix AI HTML coding tool call failed"
        AppLanguage.ARABIC -> "إصلاح فشل استدعاء أداة برمجة AI HTML"
    }

    val optimizeAiHtmlPrompt: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化AI HTML编程提示词和模型兼容性"
        AppLanguage.ENGLISH -> "Optimize AI HTML coding prompts and model compatibility"
        AppLanguage.ARABIC -> "تحسين مطالبات برمجة AI HTML وتوافق النموذج"
    }

    val statusBarFollowTheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "状态栏颜色跟随主题：默认跟随主题色彩"
        AppLanguage.ENGLISH -> "Status bar color follows theme: default follows theme color"
        AppLanguage.ARABIC -> "لون شريط الحالة يتبع السمة: الافتراضي يتبع لون السمة"
    }

    val customStatusBarBgColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持自定义状态栏背景颜色"
        AppLanguage.ENGLISH -> "Support custom status bar background color"
        AppLanguage.ARABIC -> "دعم لون خلفية شريط الحالة المخصص"
    }

    val fixStatusBarTextVisibility: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复状态栏文字看不清的问题"
        AppLanguage.ENGLISH -> "Fix status bar text visibility issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة رؤية نص شريط الحالة"
    }

    val fixJsFileSelectorCompat: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复JS文件选择器兼容性问题"
        AppLanguage.ENGLISH -> "Fix JS file selector compatibility issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة توافق محدد ملفات JS"
    }

    val fixVideoFullscreenRotation: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复视频全屏未自动横屏"
        AppLanguage.ENGLISH -> "Fix video fullscreen not auto rotating to landscape"
        AppLanguage.ARABIC -> "إصلاح عدم التدوير التلقائي للفيديو بملء الشاشة إلى الوضع الأفقي"
    }

    val fixXhsImageSave: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复小红书等网站图片长按无法保存"
        AppLanguage.ENGLISH -> "Fix Xiaohongshu and similar sites image long press cannot save"
        AppLanguage.ARABIC -> "إصلاح عدم إمكانية حفظ الصور بالضغط المطول في مواقع مثل Xiaohongshu"
    }

    val newXhsImageDownloader: String get() = when (lang) {
        AppLanguage.CHINESE -> "新增小红书图片下载器模块"
        AppLanguage.ENGLISH -> "New Xiaohongshu image downloader module"
        AppLanguage.ARABIC -> "وحدة تنزيل صور Xiaohongshu الجديدة"
    }

    val fixBlobExportFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复Blob格式文件导出失败"
        AppLanguage.ENGLISH -> "Fix Blob format file export failed"
        AppLanguage.ARABIC -> "إصلاح فشل تصدير ملف بتنسيق Blob"
    }

    val fixHtmlCssJsNotWorking: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复HTML项目CSS/JS不生效"
        AppLanguage.ENGLISH -> "Fix HTML project CSS/JS not working"
        AppLanguage.ARABIC -> "إصلاح عدم عمل CSS/JS في مشروع HTML"
    }

    val fixTaskListDuplicateName: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复任务列表显示双重名称"
        AppLanguage.ENGLISH -> "Fix task list showing duplicate names"
        AppLanguage.ARABIC -> "إصلاح عرض أسماء مكررة في قائمة المهام"
    }

    val fixKnownIssues: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复数十个已知问题"
        AppLanguage.ENGLISH -> "Fix dozens of known issues"
        AppLanguage.ARABIC -> "إصلاح عشرات المشاكل المعروفة"
    }

    val optimizeAiAgentArch: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化AI Agent编程架构"
        AppLanguage.ENGLISH -> "Optimize AI Agent programming architecture"
        AppLanguage.ARABIC -> "تحسين بنية برمجة AI Agent"
    }

    val onlineMusicSearch: String get() = when (lang) {
        AppLanguage.CHINESE -> "在线音乐搜索：在线搜索下载BGM"
        AppLanguage.ENGLISH -> "Online music search: search and download BGM online"
        AppLanguage.ARABIC -> "البحث عن الموسيقى عبر الإنترنت: البحث وتنزيل BGM عبر الإنترنت"
    }

    val webAutoTranslate: String get() = when (lang) {
        AppLanguage.CHINESE -> "网页自动翻译：网页内容自动翻译"
        AppLanguage.ENGLISH -> "Web auto translate: automatic web content translation"
        AppLanguage.ARABIC -> "الترجمة التلقائية للويب: ترجمة محتوى الويب تلقائيًا"
    }

    val htmlAppFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML应用：HTML/CSS/JS转独立App"
        AppLanguage.ENGLISH -> "HTML app: convert HTML/CSS/JS to standalone app"
        AppLanguage.ARABIC -> "تطبيق HTML: تحويل HTML/CSS/JS إلى تطبيق مستقل"
    }

    val themeSystemFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "主题系统：多款精美主题+深色模式"
        AppLanguage.ENGLISH -> "Theme system: multiple beautiful themes + dark mode"
        AppLanguage.ARABIC -> "نظام السمات: سمات جميلة متعددة + الوضع الداكن"
    }

    val bgmLrcFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "背景音乐：BGM+LRC歌词同步显示"
        AppLanguage.ENGLISH -> "Background music: BGM + LRC lyrics sync display"
        AppLanguage.ARABIC -> "موسيقى الخلفية: BGM + عرض كلمات LRC متزامن"
    }

    val mediaAppFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体应用：图片/视频转独立App"
        AppLanguage.ENGLISH -> "Media app: convert images/videos to standalone app"
        AppLanguage.ARABIC -> "تطبيق الوسائط: تحويل الصور/الفيديو إلى تطبيق مستقل"
    }

    val userScriptInjection: String get() = when (lang) {
        AppLanguage.CHINESE -> "用户脚本注入：自定义JS脚本"
        AppLanguage.ENGLISH -> "User script injection: custom JS scripts"
        AppLanguage.ARABIC -> "حقن سكريبت المستخدم: سكريبتات JS مخصصة"
    }

    val splashScreenFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动画面：图片/视频启动动画"
        AppLanguage.ENGLISH -> "Splash screen: image/video startup animation"
        AppLanguage.ARABIC -> "شاشة البداية: رسوم متحركة للصور/الفيديو عند البدء"
    }

    val videoTrimFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频裁剪：可视化选择视频片段"
        AppLanguage.ENGLISH -> "Video trim: visual video segment selection"
        AppLanguage.ARABIC -> "قص الفيديو: اختيار مقطع الفيديو بصريًا"
    }

    val fixShortcutIconError: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复快捷方式图标错误问题"
        AppLanguage.ENGLISH -> "Fix shortcut icon error issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة خطأ أيقونة الاختصار"
    }

    val fullscreenModeFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "全屏模式：隐藏工具栏"
        AppLanguage.ENGLISH -> "Fullscreen mode: hide toolbar"
        AppLanguage.ARABIC -> "وضع ملء الشاشة: إخفاء شريط الأدوات"
    }

    val fixApkIconCrop: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复APK图标裁剪问题"
        AppLanguage.ENGLISH -> "Fix APK icon cropping issue"
        AppLanguage.ARABIC -> "إصلاح مشكلة قص أيقونة APK"
    }

    val fixReleaseIconNotWorking: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复Release版图标不生效"
        AppLanguage.ENGLISH -> "Fix Release version icon not working"
        AppLanguage.ARABIC -> "إصلاح عدم عمل أيقونة إصدار Release"
    }

    val fixApkPackageConflict: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复APK包名/权限冲突"
        AppLanguage.ENGLISH -> "Fix APK package name/permission conflict"
        AppLanguage.ARABIC -> "إصلاح تعارض اسم حزمة/أذونات APK"
    }

    val oneClickBuildApk: String get() = when (lang) {
        AppLanguage.CHINESE -> "一键构建独立APK安装包"
        AppLanguage.ENGLISH -> "One-click build standalone APK package"
        AppLanguage.ARABIC -> "بناء حزمة APK مستقلة بنقرة واحدة"
    }

    val cloneInstallFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "克隆安装：独立包名克隆应用"
        AppLanguage.ENGLISH -> "Clone install: clone app with independent package name"
        AppLanguage.ARABIC -> "تثبيت النسخ: نسخ التطبيق باسم حزمة مستقل"
    }

    val desktopModeFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "访问电脑版：强制桌面模式"
        AppLanguage.ENGLISH -> "Desktop mode: force desktop version"
        AppLanguage.ARABIC -> "وضع سطح المكتب: فرض إصدار سطح المكتب"
    }
    // ==================== Theme ====================
    val themeAurora: String get() = when (lang) {
        AppLanguage.CHINESE -> "极光梦境"
        AppLanguage.ENGLISH -> "Aurora Dreams"
        AppLanguage.ARABIC -> "أحلام الشفق القطبي"
    }

    val themeAuroraDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "北极光般流动的梦幻渐变，如置身极地夜空"
        AppLanguage.ENGLISH -> "Flowing aurora-like gradients, like being in the polar night sky"
        AppLanguage.ARABIC -> "تدرجات متدفقة مثل الشفق القطبي، كأنك في سماء القطب الليلية"
    }

    val themeCyberpunk: String get() = when (lang) {
        AppLanguage.CHINESE -> "赛博霓虹"
        AppLanguage.ENGLISH -> "Cyber Neon"
        AppLanguage.ARABIC -> "نيون سايبر"
    }

    val themeCyberpunkDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "霓虹闪烁的未来都市，科技与叛逆的交融"
        AppLanguage.ENGLISH -> "Neon-lit future city, fusion of tech and rebellion"
        AppLanguage.ARABIC -> "مدينة المستقبل المضاءة بالنيون، اندماج التكنولوجيا والتمرد"
    }

    val themeSakura: String get() = when (lang) {
        AppLanguage.CHINESE -> "樱花物语"
        AppLanguage.ENGLISH -> "Sakura Story"
        AppLanguage.ARABIC -> "قصة الساكورا"
    }

    val themeSakuraDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "粉色花瓣轻舞飞扬，日式和风的诗意美学"
        AppLanguage.ENGLISH -> "Pink petals dancing gracefully, Japanese poetic aesthetics"
        AppLanguage.ARABIC -> "بتلات وردية ترقص برشاقة، جماليات شعرية يابانية"
    }

    val themeOcean: String get() = when (lang) {
        AppLanguage.CHINESE -> "深海幽蓝"
        AppLanguage.ENGLISH -> "Deep Ocean Blue"
        AppLanguage.ARABIC -> "أزرق المحيط العميق"
    }

    val themeOceanDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "海洋深处的神秘光芒，波光粼粼的宁静"
        AppLanguage.ENGLISH -> "Mysterious glow from ocean depths, sparkling tranquility"
        AppLanguage.ARABIC -> "توهج غامض من أعماق المحيط، هدوء متلألئ"
    }

    val themeForest: String get() = when (lang) {
        AppLanguage.CHINESE -> "森林晨曦"
        AppLanguage.ENGLISH -> "Forest Dawn"
        AppLanguage.ARABIC -> "فجر الغابة"
    }

    val themeForestDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "阳光穿透树叶的斑驳，大自然的清新呼吸"
        AppLanguage.ENGLISH -> "Sunlight filtering through leaves, nature's fresh breath"
        AppLanguage.ARABIC -> "ضوء الشمس يتسلل عبر الأوراق، نفس الطبيعة المنعش"
    }

    val themeGalaxy: String get() = when (lang) {
        AppLanguage.CHINESE -> "星空银河"
        AppLanguage.ENGLISH -> "Galaxy Stars"
        AppLanguage.ARABIC -> "نجوم المجرة"
    }

    val themeGalaxyDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "浩瀚宇宙的璀璨星河，无垠深空的浪漫"
        AppLanguage.ENGLISH -> "Brilliant galaxy of vast universe, romance of infinite space"
        AppLanguage.ARABIC -> "مجرة رائعة من الكون الشاسع، رومانسية الفضاء اللانهائي"
    }

    val themeVolcano: String get() = when (lang) {
        AppLanguage.CHINESE -> "熔岩之心"
        AppLanguage.ENGLISH -> "Lava Heart"
        AppLanguage.ARABIC -> "قلب الحمم"
    }

    val themeVolcanoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "岩浆奔涌的炽热能量，燃烧的生命力"
        AppLanguage.ENGLISH -> "Scorching energy of flowing lava, burning vitality"
        AppLanguage.ARABIC -> "طاقة حارقة من الحمم المتدفقة، حيوية مشتعلة"
    }

    val themeFrost: String get() = when (lang) {
        AppLanguage.CHINESE -> "冰晶之境"
        AppLanguage.ENGLISH -> "Frost Crystal"
        AppLanguage.ARABIC -> "بلورة الصقيع"
    }

    val themeFrostDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "冰雪世界的纯净光辉，晶莹剔透的优雅"
        AppLanguage.ENGLISH -> "Pure radiance of ice world, crystal clear elegance"
        AppLanguage.ARABIC -> "إشراق نقي لعالم الجليد، أناقة بلورية صافية"
    }

    val themeSunset: String get() = when (lang) {
        AppLanguage.CHINESE -> "紫金黄昏"
        AppLanguage.ENGLISH -> "Purple Gold Sunset"
        AppLanguage.ARABIC -> "غروب ذهبي بنفسجي"
    }

    val themeSunsetDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "落日余晖的温暖拥抱，黄昏时分的诗意"
        AppLanguage.ENGLISH -> "Warm embrace of sunset glow, poetry of twilight"
        AppLanguage.ARABIC -> "عناق دافئ لتوهج الغروب، شعر الشفق"
    }

    val themeMinimal: String get() = when (lang) {
        AppLanguage.CHINESE -> "极简主义"
        AppLanguage.ENGLISH -> "Minimalism"
        AppLanguage.ARABIC -> "البساطة"
    }

    val themeMinimalDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "去繁就简的纯粹美学，精致细节的禅意"
        AppLanguage.ENGLISH -> "Pure aesthetics of simplicity, zen of refined details"
        AppLanguage.ARABIC -> "جماليات نقية للبساطة، زن التفاصيل المصقولة"
    }

    val themeNeonTokyo: String get() = when (lang) {
        AppLanguage.CHINESE -> "东京霓虹"
        AppLanguage.ENGLISH -> "Tokyo Neon"
        AppLanguage.ARABIC -> "نيون طوكيو"
    }

    val themeNeonTokyoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "雨夜东京的霓虹倒影，赛博朋克的浪漫"
        AppLanguage.ENGLISH -> "Neon reflections of rainy Tokyo night, cyberpunk romance"
        AppLanguage.ARABIC -> "انعكاسات النيون لليلة طوكيو الممطرة، رومانسية سايبربانك"
    }

    val themeLavender: String get() = when (lang) {
        AppLanguage.CHINESE -> "薰衣草田"
        AppLanguage.ENGLISH -> "Lavender Field"
        AppLanguage.ARABIC -> "حقل اللافندر"
    }

    val themeLavenderDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "普罗旺斯的紫色海洋，芬芳宁静的治愈"
        AppLanguage.ENGLISH -> "Purple ocean of Provence, fragrant and peaceful healing"
        AppLanguage.ARABIC -> "محيط بنفسجي من بروفانس، شفاء عطري وهادئ"
    }

    val themeKimiNoNawa: String get() = when (lang) {
        AppLanguage.CHINESE -> "你的名字"
        AppLanguage.ENGLISH -> "Your Name"
        AppLanguage.ARABIC -> "اسمك"
    }

    val themeKimiNoNawaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "黄昏天空的彗星轨迹，新海诚式的浪漫黄昏"
        AppLanguage.ENGLISH -> "Comet trails across twilight sky, Shinkai-style romantic dusk"
        AppLanguage.ARABIC -> "آثار المذنب عبر سماء الشفق، غسق رومانسي بأسلوب شينكاي"
    }

    val themeAnohana: String get() = when (lang) {
        AppLanguage.CHINESE -> "未闻花名"
        AppLanguage.ENGLISH -> "Anohana"
        AppLanguage.ARABIC -> "أنوهانا"
    }

    val themeAnohanaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "夏日午后的温暖怀旧，花瓣飘落的柔软时光"
        AppLanguage.ENGLISH -> "Warm nostalgia of summer afternoon, soft moments with falling petals"
        AppLanguage.ARABIC -> "حنين دافئ لبعد ظهر صيفي، لحظات ناعمة مع سقوط البتلات"
    }

    val themeDeathNote: String get() = when (lang) {
        AppLanguage.CHINESE -> "死亡笔记"
        AppLanguage.ENGLISH -> "Death Note"
        AppLanguage.ARABIC -> "مذكرة الموت"
    }

    val themeDeathNoteDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "死神之眼的猩红视野，黑暗中书写命运的笔触"
        AppLanguage.ENGLISH -> "Crimson gaze of Shinigami eyes, strokes of fate written in darkness"
        AppLanguage.ARABIC -> "نظرة شينيغامي القرمزية، ضربات القدر المكتوبة في الظلام"
    }

    val themeNaruto: String get() = when (lang) {
        AppLanguage.CHINESE -> "火影忍者"
        AppLanguage.ENGLISH -> "Naruto"
        AppLanguage.ARABIC -> "ناروتو"
    }

    val themeNarutoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "火之意志燃烧的木叶橙，忍道永不放弃的热血"
        AppLanguage.ENGLISH -> "Blazing Konoha orange of the Will of Fire, the unyielding nindō spirit"
        AppLanguage.ARABIC -> "برتقال كونوها المشتعل لإرادة النار، روح النيندو التي لا تستسلم"
    }

    val themeOnePiece: String get() = when (lang) {
        AppLanguage.CHINESE -> "海贼王"
        AppLanguage.ENGLISH -> "One Piece"
        AppLanguage.ARABIC -> "ون بيس"
    }

    val themeOnePieceDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "伟大航路的湛蓝深海，追逐自由与宝藏的冒险"
        AppLanguage.ENGLISH -> "Azure deep sea of the Grand Line, an adventure chasing freedom and treasure"
        AppLanguage.ARABIC -> "بحر أزرق عميق للخط الكبير، مغامرة تطارد الحرية والكنز"
    }

    val themeBoonieBears: String get() = when (lang) {
        AppLanguage.CHINESE -> "熊出没"
        AppLanguage.ENGLISH -> "Boonie Bears"
        AppLanguage.ARABIC -> "الدببة المشاغبة"
    }

    val themeBoonieBearDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "阳光森林的自然童趣，熊大熊二的欢乐冒险"
        AppLanguage.ENGLISH -> "Sunny forest fun, the joyful adventures of Briar and Bramble"
        AppLanguage.ARABIC -> "مرح الغابة المشمسة، مغامرات الدببة المبهجة"
    }

    val themeTomAndJerry: String get() = when (lang) {
        AppLanguage.CHINESE -> "猫和老鼠"
        AppLanguage.ENGLISH -> "Tom and Jerry"
        AppLanguage.ARABIC -> "توم وجيري"
    }

    val themeTomAndJerryDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "经典追逐的欢乐色调，永不过时的童年回忆"
        AppLanguage.ENGLISH -> "Joyful tones of classic chase, timeless childhood memories"
        AppLanguage.ARABIC -> "ألوان مبهجة للمطاردة الكلاسيكية، ذكريات طفولة خالدة"
    }

    val themeZarathustra: String get() = when (lang) {
        AppLanguage.CHINESE -> "查拉图斯特拉如是说"
        AppLanguage.ENGLISH -> "Thus Spoke Zarathustra"
        AppLanguage.ARABIC -> "هكذا تكلم زرادشت"
    }

    val themeZarathustraDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "超人晨曦的山巅金光，永恒轮回中意志的觉醒"
        AppLanguage.ENGLISH -> "Golden dawn on the Übermensch's summit, the will awakening in eternal recurrence"
        AppLanguage.ARABIC -> "فجر ذهبي على قمة الإنسان الأعلى، الإرادة تستيقظ في العودة الأبدية"
    }

    val themeWillAndRepresentation: String get() = when (lang) {
        AppLanguage.CHINESE -> "作为意志和表象的世界"
        AppLanguage.ENGLISH -> "The World as Will"
        AppLanguage.ARABIC -> "العالم كإرادة وتمثل"
    }

    val themeWillAndRepresentationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "叔本华的深渊凝视，意志暗涌与表象幻象的交织"
        AppLanguage.ENGLISH -> "Schopenhauer's gaze into the abyss, intertwined currents of will and veil of representation"
        AppLanguage.ARABIC -> "تحديق شوبنهاور في الهاوية، تيارات الإرادة وحجاب التمثل المتشابكة"
    }
    // ==================== Color Names ====================
    val colorRed: String get() = when (lang) {
        AppLanguage.CHINESE -> "红色"
        AppLanguage.ENGLISH -> "Red"
        AppLanguage.ARABIC -> "أحمر"
    }

    val colorPink: String get() = when (lang) {
        AppLanguage.CHINESE -> "粉色"
        AppLanguage.ENGLISH -> "Pink"
        AppLanguage.ARABIC -> "وردي"
    }

    val colorPurple: String get() = when (lang) {
        AppLanguage.CHINESE -> "紫色"
        AppLanguage.ENGLISH -> "Purple"
        AppLanguage.ARABIC -> "بنفسجي"
    }

    val colorDeepPurple: String get() = when (lang) {
        AppLanguage.CHINESE -> "深紫"
        AppLanguage.ENGLISH -> "Deep Purple"
        AppLanguage.ARABIC -> "بنفسجي داكن"
    }

    val colorIndigo: String get() = when (lang) {
        AppLanguage.CHINESE -> "靛蓝"
        AppLanguage.ENGLISH -> "Indigo"
        AppLanguage.ARABIC -> "نيلي"
    }

    val colorBlue: String get() = when (lang) {
        AppLanguage.CHINESE -> "蓝色"
        AppLanguage.ENGLISH -> "Blue"
        AppLanguage.ARABIC -> "أزرق"
    }

    val colorLightBlue: String get() = when (lang) {
        AppLanguage.CHINESE -> "浅蓝"
        AppLanguage.ENGLISH -> "Light Blue"
        AppLanguage.ARABIC -> "أزرق فاتح"
    }

    val colorCyan: String get() = when (lang) {
        AppLanguage.CHINESE -> "青色"
        AppLanguage.ENGLISH -> "Cyan"
        AppLanguage.ARABIC -> "سماوي"
    }

    val colorTeal: String get() = when (lang) {
        AppLanguage.CHINESE -> "蓝绿"
        AppLanguage.ENGLISH -> "Teal"
        AppLanguage.ARABIC -> "أزرق مخضر"
    }

    val colorGreen: String get() = when (lang) {
        AppLanguage.CHINESE -> "绿色"
        AppLanguage.ENGLISH -> "Green"
        AppLanguage.ARABIC -> "أخضر"
    }

    val colorLightGreen: String get() = when (lang) {
        AppLanguage.CHINESE -> "浅绿"
        AppLanguage.ENGLISH -> "Light Green"
        AppLanguage.ARABIC -> "أخضر فاتح"
    }

    val colorLime: String get() = when (lang) {
        AppLanguage.CHINESE -> "黄绿"
        AppLanguage.ENGLISH -> "Lime"
        AppLanguage.ARABIC -> "ليموني"
    }

    val colorYellow: String get() = when (lang) {
        AppLanguage.CHINESE -> "黄色"
        AppLanguage.ENGLISH -> "Yellow"
        AppLanguage.ARABIC -> "أصفر"
    }

    val colorAmber: String get() = when (lang) {
        AppLanguage.CHINESE -> "琥珀"
        AppLanguage.ENGLISH -> "Amber"
        AppLanguage.ARABIC -> "كهرماني"
    }

    val colorOrange: String get() = when (lang) {
        AppLanguage.CHINESE -> "橙色"
        AppLanguage.ENGLISH -> "Orange"
        AppLanguage.ARABIC -> "برتقالي"
    }

    val colorDeepOrange: String get() = when (lang) {
        AppLanguage.CHINESE -> "深橙"
        AppLanguage.ENGLISH -> "Deep Orange"
        AppLanguage.ARABIC -> "برتقالي داكن"
    }

    val colorBrown: String get() = when (lang) {
        AppLanguage.CHINESE -> "棕色"
        AppLanguage.ENGLISH -> "Brown"
        AppLanguage.ARABIC -> "بني"
    }

    val colorGrey: String get() = when (lang) {
        AppLanguage.CHINESE -> "灰色"
        AppLanguage.ENGLISH -> "Grey"
        AppLanguage.ARABIC -> "رمادي"
    }

    val colorBlueGrey: String get() = when (lang) {
        AppLanguage.CHINESE -> "蓝灰"
        AppLanguage.ENGLISH -> "Blue Grey"
        AppLanguage.ARABIC -> "رمادي مزرق"
    }

    val colorBlack: String get() = when (lang) {
        AppLanguage.CHINESE -> "黑色"
        AppLanguage.ENGLISH -> "Black"
        AppLanguage.ARABIC -> "أسود"
    }

    val colorWhite: String get() = when (lang) {
        AppLanguage.CHINESE -> "白色"
        AppLanguage.ENGLISH -> "White"
        AppLanguage.ARABIC -> "أبيض"
    }

    val colorDarkTheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "深色主题"
        AppLanguage.ENGLISH -> "Dark Theme"
        AppLanguage.ARABIC -> "سمة داكنة"
    }

    val colorLightTheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "浅色主题"
        AppLanguage.ENGLISH -> "Light Theme"
        AppLanguage.ARABIC -> "سمة فاتحة"
    }

    val colorTransparent: String get() = when (lang) {
        AppLanguage.CHINESE -> "透明"
        AppLanguage.ENGLISH -> "Transparent"
        AppLanguage.ARABIC -> "شفاف"
    }

    val colorSelected: String get() = when (lang) {
        AppLanguage.CHINESE -> "已选择"
        AppLanguage.ENGLISH -> "Selected"
        AppLanguage.ARABIC -> "محدد"
    }
}
