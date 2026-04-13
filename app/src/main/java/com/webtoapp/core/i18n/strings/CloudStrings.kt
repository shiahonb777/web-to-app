package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object CloudStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage

    val cloudOverview: String get() = when (lang) {
        AppLanguage.CHINESE -> "概览"
        AppLanguage.ENGLISH -> "Overview"
        AppLanguage.ARABIC -> "نظرة عامة"
    }

    val cloudSync: String get() = when (lang) {
        AppLanguage.CHINESE -> "同步"
        AppLanguage.ENGLISH -> "Sync"
        AppLanguage.ARABIC -> "المزامنة"
    }

    val cloudScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "脚本"
        AppLanguage.ENGLISH -> "Scripts"
        AppLanguage.ARABIC -> "السكريبتات"
    }

    val cloudShare: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享"
        AppLanguage.ENGLISH -> "Share"
        AppLanguage.ARABIC -> "مشاركة"
    }

    val cloudActivationCodes: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活码"
        AppLanguage.ENGLISH -> "Keys"
        AppLanguage.ARABIC -> "مفاتيح"
    }

    val cloudAnnouncements: String get() = when (lang) {
        AppLanguage.CHINESE -> "公告"
        AppLanguage.ENGLISH -> "News"
        AppLanguage.ARABIC -> "الإعلانات"
    }

    val cloudPush: String get() = when (lang) {
        AppLanguage.CHINESE -> "推送"
        AppLanguage.ENGLISH -> "Push"
        AppLanguage.ARABIC -> "الإشعارات"
    }

    val pushAnnouncement: String get() = when (lang) {
        AppLanguage.CHINESE -> "公告推送"
        AppLanguage.ENGLISH -> "Announcement"
        AppLanguage.ARABIC -> "إعلان"
    }

    val pushUpdate: String get() = when (lang) {
        AppLanguage.CHINESE -> "更新推送"
        AppLanguage.ENGLISH -> "Update"
        AppLanguage.ARABIC -> "تحديث"
    }

    val pushTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "推送标题"
        AppLanguage.ENGLISH -> "Title"
        AppLanguage.ARABIC -> "العنوان"
    }

    val pushBody: String get() = when (lang) {
        AppLanguage.CHINESE -> "推送内容"
        AppLanguage.ENGLISH -> "Content"
        AppLanguage.ARABIC -> "المحتوى"
    }

    val pushVersionName: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本号"
        AppLanguage.ENGLISH -> "Version"
        AppLanguage.ARABIC -> "الإصدار"
    }

    val pushForceUpdate: String get() = when (lang) {
        AppLanguage.CHINESE -> "强制更新"
        AppLanguage.ENGLISH -> "Force Update"
        AppLanguage.ARABIC -> "تحديث إجباري"
    }

    val pushOptionalUpdate: String get() = when (lang) {
        AppLanguage.CHINESE -> "可选更新"
        AppLanguage.ENGLISH -> "Optional"
        AppLanguage.ARABIC -> "اختياري"
    }

    val pushSendBtn: String get() = when (lang) {
        AppLanguage.CHINESE -> "发送推送"
        AppLanguage.ENGLISH -> "Send Push"
        AppLanguage.ARABIC -> "إرسال"
    }

    val pushHistory: String get() = when (lang) {
        AppLanguage.CHINESE -> "推送历史"
        AppLanguage.ENGLISH -> "Push History"
        AppLanguage.ARABIC -> "سجل الإشعارات"
    }

    val pushDailyLimit: String get() = when (lang) {
        AppLanguage.CHINESE -> "今日已用"
        AppLanguage.ENGLISH -> "Today"
        AppLanguage.ARABIC -> "اليوم"
    }

    val pushSent: String get() = when (lang) {
        AppLanguage.CHINESE -> "已发送"
        AppLanguage.ENGLISH -> "Sent"
        AppLanguage.ARABIC -> "تم الإرسال"
    }

    val pushFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "发送失败"
        AppLanguage.ENGLISH -> "Failed"
        AppLanguage.ARABIC -> "فشل"
    }

    val pushEmpty: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无推送记录"
        AppLanguage.ENGLISH -> "No push history"
        AppLanguage.ARABIC -> "لا يوجد سجل"
    }

    val cloudRemoteConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "配置"
        AppLanguage.ENGLISH -> "Config"
        AppLanguage.ARABIC -> "الإعدادات"
    }

    val cloudVersions: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本"
        AppLanguage.ENGLISH -> "Versions"
        AppLanguage.ARABIC -> "الإصدارات"
    }

    val cloudBackups: String get() = when (lang) {
        AppLanguage.CHINESE -> "备份"
        AppLanguage.ENGLISH -> "Backups"
        AppLanguage.ARABIC -> "النسخ الاحتياطي"
    }

    val authCloudService: String get() = when (lang) {
        AppLanguage.CHINESE -> "WebToApp 云服务"
        AppLanguage.ENGLISH -> "WebToApp Cloud"
        AppLanguage.ARABIC -> "WebToApp السحابية"
    }

    val authCloudDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "登录账号，解锁云端构建与项目同步"
        AppLanguage.ENGLISH -> "Sign in to unlock cloud builds & project sync"
        AppLanguage.ARABIC -> "سجل الدخول لفتح البناء السحابي ومزامنة المشاريع"
    }

    val authLogin: String get() = when (lang) {
        AppLanguage.CHINESE -> "登录"
        AppLanguage.ENGLISH -> "Sign In"
        AppLanguage.ARABIC -> "تسجيل الدخول"
    }

    val authRegister: String get() = when (lang) {
        AppLanguage.CHINESE -> "注册"
        AppLanguage.ENGLISH -> "Sign Up"
        AppLanguage.ARABIC -> "إنشاء حساب"
    }

    val authEmail: String get() = when (lang) {
        AppLanguage.CHINESE -> "邮箱"
        AppLanguage.ENGLISH -> "Email"
        AppLanguage.ARABIC -> "البريد الإلكتروني"
    }

    val authUsername: String get() = when (lang) {
        AppLanguage.CHINESE -> "用户名"
        AppLanguage.ENGLISH -> "Username"
        AppLanguage.ARABIC -> "اسم المستخدم"
    }

    val authUsernameHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "字母、数字和下划线"
        AppLanguage.ENGLISH -> "Letters, numbers and underscores"
        AppLanguage.ARABIC -> "حروف وأرقام وشرطة سفلية"
    }

    val authPassword: String get() = when (lang) {
        AppLanguage.CHINESE -> "密码"
        AppLanguage.ENGLISH -> "Password"
        AppLanguage.ARABIC -> "كلمة المرور"
    }

    val authPasswordHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "至少 6 位字符"
        AppLanguage.ENGLISH -> "At least 6 characters"
        AppLanguage.ARABIC -> "6 أحرف على الأقل"
    }

    val authConfirmPassword: String get() = when (lang) {
        AppLanguage.CHINESE -> "确认密码"
        AppLanguage.ENGLISH -> "Confirm Password"
        AppLanguage.ARABIC -> "تأكيد كلمة المرور"
    }

    val authLoggingIn: String get() = when (lang) {
        AppLanguage.CHINESE -> "登录中..."
        AppLanguage.ENGLISH -> "Signing in..."
        AppLanguage.ARABIC -> "جارٍ تسجيل الدخول..."
    }

    val authRegistering: String get() = when (lang) {
        AppLanguage.CHINESE -> "注册中..."
        AppLanguage.ENGLISH -> "Signing up..."
        AppLanguage.ARABIC -> "جارٍ إنشاء الحساب..."
    }

    val authNoAccount: String get() = when (lang) {
        AppLanguage.CHINESE -> "还没有账号？"
        AppLanguage.ENGLISH -> "Don't have an account?"
        AppLanguage.ARABIC -> "ليس لديك حساب؟"
    }

    val authRegisterNow: String get() = when (lang) {
        AppLanguage.CHINESE -> "立即注册"
        AppLanguage.ENGLISH -> "Sign up now"
        AppLanguage.ARABIC -> "سجل الآن"
    }

    val authHasAccount: String get() = when (lang) {
        AppLanguage.CHINESE -> "已有账号？"
        AppLanguage.ENGLISH -> "Already have an account?"
        AppLanguage.ARABIC -> "لديك حساب بالفعل؟"
    }

    val authLoginNow: String get() = when (lang) {
        AppLanguage.CHINESE -> "立即登录"
        AppLanguage.ENGLISH -> "Sign in now"
        AppLanguage.ARABIC -> "سجل الدخول الآن"
    }

    val authWhyRegister: String get() = when (lang) {
        AppLanguage.CHINESE -> "为什么要注册？"
        AppLanguage.ENGLISH -> "Why sign up?"
        AppLanguage.ARABIC -> "لماذا تسجيل حساب؟"
    }

    val authFeatureCloud: String get() = when (lang) {
        AppLanguage.CHINESE -> "云端项目管理，激活码批量生成"
        AppLanguage.ENGLISH -> "Cloud project management & activation codes"
        AppLanguage.ARABIC -> "إدارة المشاريع السحابية ورموز التفعيل"
    }

    val authFeatureStats: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用使用统计和数据分析仪表板"
        AppLanguage.ENGLISH -> "App usage analytics dashboard"
        AppLanguage.ARABIC -> "لوحة تحليلات استخدام التطبيق"
    }

    val authFeatureShare: String get() = when (lang) {
        AppLanguage.CHINESE -> "一键生成 APK 分享下载页"
        AppLanguage.ENGLISH -> "Generate APK share & download page"
        AppLanguage.ARABIC -> "إنشاء صفحة مشاركة وتنزيل APK"
    }

    val authFeatureBackup: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目云端备份到 GitHub/Gitee"
        AppLanguage.ENGLISH -> "Cloud backup to GitHub/Gitee"
        AppLanguage.ARABIC -> "نسخ احتياطي سحابي إلى GitHub/Gitee"
    }

    val authFreeNote: String get() = when (lang) {
        AppLanguage.CHINESE -> "基础功能完全免费，无需注册即可使用"
        AppLanguage.ENGLISH -> "Basic features are completely free, no sign-up required"
        AppLanguage.ARABIC -> "الميزات الأساسية مجانية بالكامل، بدون تسجيل"
    }

    val authProfile: String get() = when (lang) {
        AppLanguage.CHINESE -> "个人中心"
        AppLanguage.ENGLISH -> "Profile"
        AppLanguage.ARABIC -> "الملف الشخصي"
    }

    val authLogout: String get() = when (lang) {
        AppLanguage.CHINESE -> "退出登录"
        AppLanguage.ENGLISH -> "Sign Out"
        AppLanguage.ARABIC -> "تسجيل الخروج"
    }

    val authLogoutConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定要退出登录吗？退出后需要重新登录才能使用云服务。"
        AppLanguage.ENGLISH -> "Are you sure you want to sign out? You'll need to sign in again to use cloud services."
        AppLanguage.ARABIC -> "هل أنت متأكد من تسجيل الخروج؟ ستحتاج إلى تسجيل الدخول مرة أخرى لاستخدام الخدمات السحابية."
    }

    val authStatsAppsCreated: String get() = when (lang) {
        AppLanguage.CHINESE -> "已创建应用"
        AppLanguage.ENGLISH -> "Apps Created"
        AppLanguage.ARABIC -> "التطبيقات المنشأة"
    }

    val authStatsApksBuilt: String get() = when (lang) {
        AppLanguage.CHINESE -> "已构建 APK"
        AppLanguage.ENGLISH -> "APKs Built"
        AppLanguage.ARABIC -> "APK المبنية"
    }

    val authStatsMaxDevices: String get() = when (lang) {
        AppLanguage.CHINESE -> "最大设备数"
        AppLanguage.ENGLISH -> "Max Devices"
        AppLanguage.ARABIC -> "الحد الأقصى للأجهزة"
    }

    val authProActive: String get() = when (lang) {
        AppLanguage.CHINESE -> "Pro 会员已激活"
        AppLanguage.ENGLISH -> "Pro Membership Active"
        AppLanguage.ARABIC -> "عضوية Pro نشطة"
    }

    val authUltraActive: String get() = when (lang) {
        AppLanguage.CHINESE -> "Ultra 会员已激活"
        AppLanguage.ENGLISH -> "Ultra Membership Active"
        AppLanguage.ARABIC -> "عضوية Ultra نشطة"
    }

    val authLifetimeActive: String get() = when (lang) {
        AppLanguage.CHINESE -> "Pro 永久会员已激活"
        AppLanguage.ENGLISH -> "Pro Lifetime Active"
        AppLanguage.ARABIC -> "عضوية Pro الدائمة نشطة"
    }

    val authUltraLifetimeActive: String get() = when (lang) {
        AppLanguage.CHINESE -> "Ultra 永久会员已激活"
        AppLanguage.ENGLISH -> "Ultra Lifetime Active"
        AppLanguage.ARABIC -> "عضوية Ultra الدائمة نشطة"
    }

    val authUpgradeToUltra: String get() = when (lang) {
        AppLanguage.CHINESE -> "升级至 Ultra 永久"
        AppLanguage.ENGLISH -> "Upgrade to Ultra Lifetime"
        AppLanguage.ARABIC -> "الترقية إلى Ultra الدائمة"
    }

    val authUpgradeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "补差价即可升级"
        AppLanguage.ENGLISH -> "Pay the difference to upgrade"
        AppLanguage.ARABIC -> "ادفع الفرق للترقية"
    }

    val authProInactive: String get() = when (lang) {
        AppLanguage.CHINESE -> "免费版用户"
        AppLanguage.ENGLISH -> "Free Plan"
        AppLanguage.ARABIC -> "الخطة المجانية"
    }

    val authProRemaining: String get() = when (lang) {
        AppLanguage.CHINESE -> "剩余"
        AppLanguage.ENGLISH -> "Remaining"
        AppLanguage.ARABIC -> "المتبقي"
    }

    val authProDays: String get() = when (lang) {
        AppLanguage.CHINESE -> "天"
        AppLanguage.ENGLISH -> "days"
        AppLanguage.ARABIC -> "يوم"
    }

    val authMenuDevices: String get() = when (lang) {
        AppLanguage.CHINESE -> "设备管理"
        AppLanguage.ENGLISH -> "Device Management"
        AppLanguage.ARABIC -> "إدارة الأجهزة"
    }

    val authMenuDevicesMax: String get() = when (lang) {
        AppLanguage.CHINESE -> "最多绑定设备"
        AppLanguage.ENGLISH -> "Max bound devices"
        AppLanguage.ARABIC -> "الحد الأقصى للأجهزة المرتبطة"
    }

    val authMenuCloudProjects: String get() = when (lang) {
        AppLanguage.CHINESE -> "云端项目"
        AppLanguage.ENGLISH -> "Cloud Projects"
        AppLanguage.ARABIC -> "المشاريع السحابية"
    }

    val authMenuCloudAvailable: String get() = when (lang) {
        AppLanguage.CHINESE -> "可用"
        AppLanguage.ENGLISH -> "Available"
        AppLanguage.ARABIC -> "متاح"
    }

    val authMenuCloudUpgrade: String get() = when (lang) {
        AppLanguage.CHINESE -> "升级 Pro 解锁"
        AppLanguage.ENGLISH -> "Upgrade to Pro to unlock"
        AppLanguage.ARABIC -> "ترقية إلى Pro للفتح"
    }

    val authMenuSecurity: String get() = when (lang) {
        AppLanguage.CHINESE -> "安全设置"
        AppLanguage.ENGLISH -> "Security Settings"
        AppLanguage.ARABIC -> "إعدادات الأمان"
    }

    val authMenuSecurityDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "修改密码、管理登录设备"
        AppLanguage.ENGLISH -> "Change password, manage login devices"
        AppLanguage.ARABIC -> "تغيير كلمة المرور، إدارة أجهزة تسجيل الدخول"
    }

    val authForgotPassword: String get() = when (lang) {
        AppLanguage.CHINESE -> "忘记密码？"
        AppLanguage.ENGLISH -> "Forgot Password?"
        AppLanguage.ARABIC -> "نسيت كلمة المرور؟"
    }

    val authResetPasswordTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "重置密码"
        AppLanguage.ENGLISH -> "Reset Password"
        AppLanguage.ARABIC -> "إعادة تعيين كلمة المرور"
    }

    val authResetPasswordDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入邮箱，获取验证码重置密码"
        AppLanguage.ENGLISH -> "Enter email to receive verification code and reset password"
        AppLanguage.ARABIC -> "أدخل البريد الإلكتروني لاستلام رمز التحقق وإعادة تعيين كلمة المرور"
    }

    val authVerificationCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "验证码"
        AppLanguage.ENGLISH -> "Verification Code"
        AppLanguage.ARABIC -> "رمز التحقق"
    }

    val authCodePlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "6 位数字"
        AppLanguage.ENGLISH -> "6-digit code"
        AppLanguage.ARABIC -> "رمز من 6 أرقام"
    }

    val authSendCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "发送验证码"
        AppLanguage.ENGLISH -> "Send Code"
        AppLanguage.ARABIC -> "إرسال الرمز"
    }

    val authNewPassword: String get() = when (lang) {
        AppLanguage.CHINESE -> "新密码"
        AppLanguage.ENGLISH -> "New Password"
        AppLanguage.ARABIC -> "كلمة المرور الجديدة"
    }

    val authPasswordMinLength: String get() = when (lang) {
        AppLanguage.CHINESE -> "至少 6 位"
        AppLanguage.ENGLISH -> "At least 6 characters"
        AppLanguage.ARABIC -> "6 أحرف على الأقل"
    }

    val authResetPasswordBtn: String get() = when (lang) {
        AppLanguage.CHINESE -> "重置密码"
        AppLanguage.ENGLISH -> "Reset Password"
        AppLanguage.ARABIC -> "إعادة تعيين كلمة المرور"
    }

    val authRememberPassword: String get() = when (lang) {
        AppLanguage.CHINESE -> "记起密码了？"
        AppLanguage.ENGLISH -> "Remember your password?"
        AppLanguage.ARABIC -> "تذكرت كلمة المرور؟"
    }

    val authBackToLogin: String get() = when (lang) {
        AppLanguage.CHINESE -> "返回登录"
        AppLanguage.ENGLISH -> "Back to login"
        AppLanguage.ARABIC -> "العودة إلى تسجيل الدخول"
    }

    val authRegisterSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "🎉 注册成功"
        AppLanguage.ENGLISH -> "🎉 Registration successful"
        AppLanguage.ARABIC -> "🎉 تم التسجيل بنجاح"
    }

    val authWelcomeMessage: String get() = when (lang) {
        AppLanguage.CHINESE -> "欢迎加入 WebToApp！"
        AppLanguage.ENGLISH -> "Welcome to WebToApp!"
        AppLanguage.ARABIC -> "مرحباً بك في WebToApp!"
    }

    val authCloudServiceNotice: String get() = when (lang) {
        AppLanguage.CHINESE -> "目前云端相关功能（云端构建、云端同步、远程管理等）仍在开发中，当前功能尚不完善，部分服务可能暂时不可用。\n\n我们正在积极开发中，敬请期待后续更新！"
        AppLanguage.ENGLISH -> "Cloud-related features (cloud build, cloud sync, remote management, etc.) are still under development. Current features are incomplete and some services may be temporarily unavailable.\n\nWe are actively developing, please stay tuned for updates!"
        AppLanguage.ARABIC -> "الميزات المتعلقة بالسحابة (البناء السحابي، المزامنة السحابية، الإدارة عن بُعد، إلخ) لا تزال قيد التطوير. الميزات الحالية غير مكتملة وبعض الخدمات قد تكون غير متاحة مؤقتاً.\n\nنحن نطور بنشاط، يرجى ترقب التحديثات!"
    }

    val authConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确认"
        AppLanguage.ENGLISH -> "Confirm"
        AppLanguage.ARABIC -> "تأكيد"
    }

    val authPasswordResetSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "🎉 密码已重置，已自动登录"
        AppLanguage.ENGLISH -> "🎉 Password reset, auto-logged in"
        AppLanguage.ARABIC -> "🎉 تم إعادة تعيين كلمة المرور، تم تسجيل الدخول تلقائياً"
    }

    val authUsernameOrEmail: String get() = when (lang) {
        AppLanguage.CHINESE -> "用户名 / 邮箱"
        AppLanguage.ENGLISH -> "Username / Email"
        AppLanguage.ARABIC -> "اسم المستخدم / البريد الإلكتروني"
    }

    val authInputUsernameOrEmail: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入用户名或邮箱"
        AppLanguage.ENGLISH -> "Enter username or email"
        AppLanguage.ARABIC -> "أدخل اسم المستخدم أو البريد الإلكتروني"
    }

    val authOr: String get() = when (lang) {
        AppLanguage.CHINESE -> "  或  "
        AppLanguage.ENGLISH -> "  or  "
        AppLanguage.ARABIC -> "  أو  "
    }

    val authGoogleLogin: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 Google 账号登录"
        AppLanguage.ENGLISH -> "Sign in with Google"
        AppLanguage.ARABIC -> "تسجيل الدخول باستخدام Google"
    }

    val authLoggingInWithGoogle: String get() = when (lang) {
        AppLanguage.CHINESE -> "登录中..."
        AppLanguage.ENGLISH -> "Signing in..."
        AppLanguage.ARABIC -> "جارٍ تسجيل الدخول..."
    }

    val authRegisterEmail: String get() = when (lang) {
        AppLanguage.CHINESE -> "注册邮箱"
        AppLanguage.ENGLISH -> "Register Email"
        AppLanguage.ARABIC -> "تسجيل البريد الإلكتروني"
    }

    val cloudActivationCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活码兑换"
        AppLanguage.ENGLISH -> "Redeem Code"
        AppLanguage.ARABIC -> "استرداد الرمز"
    }

    val cloudRedeemTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "兑换激活码"
        AppLanguage.ENGLISH -> "Redeem Activation Code"
        AppLanguage.ARABIC -> "استرداد رمز التفعيل"
    }

    val cloudRedeemDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入激活码激活会员"
        AppLanguage.ENGLISH -> "Enter your code to activate membership"
        AppLanguage.ARABIC -> "أدخل رمزك لتفعيل العضوية"
    }

    val cloudCodePlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入激活码"
        AppLanguage.ENGLISH -> "Enter activation code"
        AppLanguage.ARABIC -> "أدخل رمز التفعيل"
    }

    val cloudRedeemBtn: String get() = when (lang) {
        AppLanguage.CHINESE -> "立即兑换"
        AppLanguage.ENGLISH -> "Redeem Now"
        AppLanguage.ARABIC -> "استرداد الآن"
    }

    val cloudRedeeming: String get() = when (lang) {
        AppLanguage.CHINESE -> "兑换中..."
        AppLanguage.ENGLISH -> "Redeeming..."
        AppLanguage.ARABIC -> "جاري الاسترداد..."
    }

    val cloudRedeemHistory: String get() = when (lang) {
        AppLanguage.CHINESE -> "兑换历史"
        AppLanguage.ENGLISH -> "Redemption History"
        AppLanguage.ARABIC -> "سجل الاسترداد"
    }

    val cloudValidUntil: String get() = when (lang) {
        AppLanguage.CHINESE -> "有效至"
        AppLanguage.ENGLISH -> "Valid until"
        AppLanguage.ARABIC -> "صالح حتى"
    }

    val cloudTierUpgrade: String get() = when (lang) {
        AppLanguage.CHINESE -> "等级升级"
        AppLanguage.ENGLISH -> "Tier Upgrade"
        AppLanguage.ARABIC -> "ترقية المستوى"
    }

    val cloudRedeemPreview: String get() = when (lang) {
        AppLanguage.CHINESE -> "兑换预览"
        AppLanguage.ENGLISH -> "Redeem Preview"
        AppLanguage.ARABIC -> "معاينة الاسترداد"
    }

    val cloudCurrentPlan: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前"
        AppLanguage.ENGLISH -> "Current"
        AppLanguage.ARABIC -> "الحالي"
    }

    val cloudAfterRedeem: String get() = when (lang) {
        AppLanguage.CHINESE -> "兑换后"
        AppLanguage.ENGLISH -> "After Redeem"
        AppLanguage.ARABIC -> "بعد الاسترداد"
    }

    val cloudLifetime: String get() = when (lang) {
        AppLanguage.CHINESE -> "终身"
        AppLanguage.ENGLISH -> "Lifetime"
        AppLanguage.ARABIC -> "مدى الحياة"
    }

    val cloudUpgradeNotice: String get() = when (lang) {
        AppLanguage.CHINESE -> "此操作将升级你的会员等级"
        AppLanguage.ENGLISH -> "This will upgrade your membership tier"
        AppLanguage.ARABIC -> "سيؤدي هذا إلى ترقية مستوى عضويتك"
    }

    val cloudConfirmRedeem: String get() = when (lang) {
        AppLanguage.CHINESE -> "确认兑换"
        AppLanguage.ENGLISH -> "Confirm Redeem"
        AppLanguage.ARABIC -> "تأكيد الاسترداد"
    }

    val cloudDeviceManagement: String get() = when (lang) {
        AppLanguage.CHINESE -> "设备管理"
        AppLanguage.ENGLISH -> "Device Management"
        AppLanguage.ARABIC -> "إدارة الأجهزة"
    }

    val cloudDeviceCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "已绑定设备"
        AppLanguage.ENGLISH -> "Bound devices"
        AppLanguage.ARABIC -> "الأجهزة المربوطة"
    }

    val cloudNoDevices: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无绑定设备"
        AppLanguage.ENGLISH -> "No bound devices"
        AppLanguage.ARABIC -> "لا توجد أجهزة مربوطة"
    }

    val cloudCurrentDevice: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前设备"
        AppLanguage.ENGLISH -> "Current"
        AppLanguage.ARABIC -> "الحالي"
    }

    val cloudLastActive: String get() = when (lang) {
        AppLanguage.CHINESE -> "最后活跃"
        AppLanguage.ENGLISH -> "Last active"
        AppLanguage.ARABIC -> "آخر نشاط"
    }

    val cloudRemoveDevice: String get() = when (lang) {
        AppLanguage.CHINESE -> "解绑设备"
        AppLanguage.ENGLISH -> "Remove Device"
        AppLanguage.ARABIC -> "إزالة الجهاز"
    }

    val cloudRemoveDeviceConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定要解绑此设备吗？解绑后该设备需要重新登录。"
        AppLanguage.ENGLISH -> "Remove this device? It will need to log in again."
        AppLanguage.ARABIC -> "إزالة هذا الجهاز؟ سيحتاج إلى تسجيل الدخول مرة أخرى."
    }

    val cloudDismiss: String get() = when (lang) {
        AppLanguage.CHINESE -> "知道了"
        AppLanguage.ENGLISH -> "Dismiss"
        AppLanguage.ARABIC -> "تجاهل"
    }

    val cloudProjects: String get() = when (lang) {
        AppLanguage.CHINESE -> "云端项目"
        AppLanguage.ENGLISH -> "Cloud Projects"
        AppLanguage.ARABIC -> "المشاريع السحابية"
    }

    val cloudCreateProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建项目"
        AppLanguage.ENGLISH -> "Create Project"
        AppLanguage.ARABIC -> "إنشاء مشروع"
    }

    val cloudNoProjects: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无云端项目"
        AppLanguage.ENGLISH -> "No cloud projects"
        AppLanguage.ARABIC -> "لا توجد مشاريع سحابية"
    }

    val cloudCreateProjectHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击下方按钮创建第一个项目"
        AppLanguage.ENGLISH -> "Tap below to create your first project"
        AppLanguage.ARABIC -> "انقر أدناه لإنشاء مشروعك الأول"
    }

    val cloudProjectName: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目名称"
        AppLanguage.ENGLISH -> "Project Name"
        AppLanguage.ARABIC -> "اسم المشروع"
    }

    val cloudProjectDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目描述"
        AppLanguage.ENGLISH -> "Description"
        AppLanguage.ARABIC -> "الوصف"
    }

    val cloudOptional: String get() = when (lang) {
        AppLanguage.CHINESE -> "可选"
        AppLanguage.ENGLISH -> "optional"
        AppLanguage.ARABIC -> "اختياري"
    }

    val cloudCreate: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建"
        AppLanguage.ENGLISH -> "Create"
        AppLanguage.ARABIC -> "إنشاء"
    }

    val cloudDeleteProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除项目"
        AppLanguage.ENGLISH -> "Delete Project"
        AppLanguage.ARABIC -> "حذف المشروع"
    }

    val cloudDeleteProjectConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定要删除此项目吗？此操作不可恢复。"
        AppLanguage.ENGLISH -> "Delete this project? This action cannot be undone."
        AppLanguage.ARABIC -> "حذف هذا المشروع؟ لا يمكن التراجع عن هذا الإجراء."
    }

    val cloudAnalytics: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据分析"
        AppLanguage.ENGLISH -> "Analytics"
        AppLanguage.ARABIC -> "التحليلات"
    }

    val cloudDays: String get() = when (lang) {
        AppLanguage.CHINESE -> "天"
        AppLanguage.ENGLISH -> "d"
        AppLanguage.ARABIC -> "يوم"
    }

    val cloudInstalls: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装"
        AppLanguage.ENGLISH -> "Installs"
        AppLanguage.ARABIC -> "التثبيتات"
    }

    val cloudOpens: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动"
        AppLanguage.ENGLISH -> "Opens"
        AppLanguage.ARABIC -> "الفتحات"
    }

    val cloudActive: String get() = when (lang) {
        AppLanguage.CHINESE -> "活跃"
        AppLanguage.ENGLISH -> "Active"
        AppLanguage.ARABIC -> "نشط"
    }

    val cloudCrashes: String get() = when (lang) {
        AppLanguage.CHINESE -> "崩溃"
        AppLanguage.ENGLISH -> "Crashes"
        AppLanguage.ARABIC -> "الأعطال"
    }

    val cloudVersionHistory: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本历史"
        AppLanguage.ENGLISH -> "Version History"
        AppLanguage.ARABIC -> "سجل الإصدارات"
    }

    val cloudNoVersions: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无发布版本"
        AppLanguage.ENGLISH -> "No published versions"
        AppLanguage.ARABIC -> "لا توجد إصدارات منشورة"
    }

    val cloudProBenefits: String get() = when (lang) {
        AppLanguage.CHINESE -> "会员特权"
        AppLanguage.ENGLISH -> "Benefits"
        AppLanguage.ARABIC -> "المزايا"
    }

    val cloudBenefitCloud: String get() = when (lang) {
        AppLanguage.CHINESE -> "云端项目管理与发布"
        AppLanguage.ENGLISH -> "Cloud project management & publishing"
        AppLanguage.ARABIC -> "إدارة ونشر المشاريع السحابية"
    }

    val cloudBenefitPriority: String get() = when (lang) {
        AppLanguage.CHINESE -> "优先构建与加速下载"
        AppLanguage.ENGLISH -> "Priority build & accelerated downloads"
        AppLanguage.ARABIC -> "بناء ذو أولوية وتنزيلات سريعة"
    }

    val cloudBenefitDevices: String get() = when (lang) {
        AppLanguage.CHINESE -> "最多绑定 5 台设备"
        AppLanguage.ENGLISH -> "Up to 5 devices"
        AppLanguage.ARABIC -> "حتى 5 أجهزة"
    }

    val cloudBenefitAnalytics: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级数据分析与统计"
        AppLanguage.ENGLISH -> "Advanced analytics & statistics"
        AppLanguage.ARABIC -> "تحليلات وإحصائيات متقدمة"
    }

    val cloudLoadingDevices: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在加载设备..."
        AppLanguage.ENGLISH -> "Loading devices..."
        AppLanguage.ARABIC -> "جاري تحميل الأجهزة..."
    }

    val cloudDevicesOnline: String get() = when (lang) {
        AppLanguage.CHINESE -> "台设备在线"
        AppLanguage.ENGLISH -> "devices online"
        AppLanguage.ARABIC -> "أجهزة متصلة"
    }

    val cloudLoadingProjects: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在加载项目..."
        AppLanguage.ENGLISH -> "Loading projects..."
        AppLanguage.ARABIC -> "جاري تحميل المشاريع..."
    }

    val cloudProjectsTotal: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目"
        AppLanguage.ENGLISH -> "Projects"
        AppLanguage.ARABIC -> "مشاريع"
    }

    val cloudShowAdvanced: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级选项"
        AppLanguage.ENGLISH -> "Advanced"
        AppLanguage.ARABIC -> "متقدم"
    }

    val cloudHideAdvanced: String get() = when (lang) {
        AppLanguage.CHINESE -> "收起"
        AppLanguage.ENGLISH -> "Hide"
        AppLanguage.ARABIC -> "إخفاء"
    }

    val cloudPreviewTierUpgrade: String get() = when (lang) {
        AppLanguage.CHINESE -> "🚀 等级升级"
        AppLanguage.ENGLISH -> "🚀 Tier Upgrade"
        AppLanguage.ARABIC -> "🚀 ترقية المستوى"
    }

    val cloudPreviewTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "兑换预览"
        AppLanguage.ENGLISH -> "Redemption Preview"
        AppLanguage.ARABIC -> "معاينة الاسترداد"
    }

    val cloudPreviewCodeLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活码"
        AppLanguage.ENGLISH -> "Activation Code"
        AppLanguage.ARABIC -> "رمز التفعيل"
    }

    val cloudPreviewCurrent: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前"
        AppLanguage.ENGLISH -> "Current"
        AppLanguage.ARABIC -> "الحالي"
    }

    val cloudPreviewAfter: String get() = when (lang) {
        AppLanguage.CHINESE -> "兑换后"
        AppLanguage.ENGLISH -> "After"
        AppLanguage.ARABIC -> "بعد"
    }

    val cloudPreviewLifetime: String get() = when (lang) {
        AppLanguage.CHINESE -> "终身"
        AppLanguage.ENGLISH -> "Lifetime"
        AppLanguage.ARABIC -> "مدى الحياة"
    }

    val cloudPreviewDays: String get() = when (lang) {
        AppLanguage.CHINESE -> "天"
        AppLanguage.ENGLISH -> " days"
        AppLanguage.ARABIC -> " يوم"
    }

    val cloudPreviewUpgradeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "✨ 此操作将升级你的会员等级"
        AppLanguage.ENGLISH -> "✨ This will upgrade your membership tier"
        AppLanguage.ARABIC -> "✨ سيتم ترقية مستوى عضويتك"
    }

    val cloudPreviewConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确认兑换"
        AppLanguage.ENGLISH -> "Confirm Redeem"
        AppLanguage.ARABIC -> "تأكيد الاسترداد"
    }

    val cloudPreviewCancel: String get() = when (lang) {
        AppLanguage.CHINESE -> "取消"
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.ARABIC -> "إلغاء"
    }

    val cloudHistoryEmpty: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无兑换记录"
        AppLanguage.ENGLISH -> "No redemption history yet"
        AppLanguage.ARABIC -> "لا يوجد سجل استرداد بعد"
    }

    val cloudCodeFormatError: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入有效的激活码格式"
        AppLanguage.ENGLISH -> "Please enter a valid activation code"
        AppLanguage.ARABIC -> "يرجى إدخال رمز تفعيل صالح"
    }



