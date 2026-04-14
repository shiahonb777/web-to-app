package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object BillingStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage
    // ==================== Subscription and Billing ====================
    val selectPlan: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择套餐"
        AppLanguage.ENGLISH -> "Select Plan"
        AppLanguage.ARABIC -> "اختيار الخطة"
    }

    val unlockAllFeatures: String get() = when (lang) {
        AppLanguage.CHINESE -> "解锁全部功能"
        AppLanguage.ENGLISH -> "Unlock All Features"
        AppLanguage.ARABIC -> "فتح جميع الميزات"
    }

    val chooseYourPlan: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择适合你的订阅方案"
        AppLanguage.ENGLISH -> "Choose your subscription plan"
        AppLanguage.ARABIC -> "اختر خطة الاشتراك المناسبة لك"
    }

    val periodMonthly: String get() = when (lang) {
        AppLanguage.CHINESE -> "月度"
        AppLanguage.ENGLISH -> "Monthly"
        AppLanguage.ARABIC -> "شهري"
    }

    val periodQuarterly: String get() = when (lang) {
        AppLanguage.CHINESE -> "季度 (省 10%)"
        AppLanguage.ENGLISH -> "Quarterly (Save 10%)"
        AppLanguage.ARABIC -> "ربع سنوي (وفر 10%)"
    }

    val periodYearly: String get() = when (lang) {
        AppLanguage.CHINESE -> "年度 (省 20%)"
        AppLanguage.ENGLISH -> "Yearly (Save 20%)"
        AppLanguage.ARABIC -> "سنوي (وفر 20%)"
    }

    val periodLifetime: String get() = when (lang) {
        AppLanguage.CHINESE -> "终身"
        AppLanguage.ENGLISH -> "Lifetime"
        AppLanguage.ARABIC -> "مدى الحياة"
    }

    val currentPlan: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前方案：%s"
        AppLanguage.ENGLISH -> "Current Plan: %s"
        AppLanguage.ARABIC -> "الخطة الحالية: %s"
    }

    val validForever: String get() = when (lang) {
        AppLanguage.CHINESE -> "永久有效"
        AppLanguage.ENGLISH -> "Valid Forever"
        AppLanguage.ARABIC -> "صالح إلى الأبد"
    }

    val perMonth: String get() = when (lang) {
        AppLanguage.CHINESE -> "/月"
        AppLanguage.ENGLISH -> "/mo"
        AppLanguage.ARABIC -> "/شهر"
    }

    val perQuarter: String get() = when (lang) {
        AppLanguage.CHINESE -> "/季度"
        AppLanguage.ENGLISH -> "/qtr"
        AppLanguage.ARABIC -> "/ربع سنوي"
    }

    val perYear: String get() = when (lang) {
        AppLanguage.CHINESE -> "/年"
        AppLanguage.ENGLISH -> "/yr"
        AppLanguage.ARABIC -> "/سنة"
    }

    val oneTime: String get() = when (lang) {
        AppLanguage.CHINESE -> " 一次性"
        AppLanguage.ENGLISH -> " one-time"
        AppLanguage.ARABIC -> " لمرة واحدة"
    }

    val neverExpires: String get() = when (lang) {
        AppLanguage.CHINESE -> "永不过期"
        AppLanguage.ENGLISH -> "Never Expires"
        AppLanguage.ARABIC -> "لا تنتهي صلاحيتها أبداً"
    }

    val oneTimePurchaseLifetime: String get() = when (lang) {
        AppLanguage.CHINESE -> "一次购买，终身使用"
        AppLanguage.ENGLISH -> "One-time purchase, lifetime access"
        AppLanguage.ARABIC -> "شراء لمرة واحدة، وصول مدى الحياة"
    }

    val redeemWithActivationCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "请使用激活码兑换终身套餐"
        AppLanguage.ENGLISH -> "Please redeem with activation code for lifetime plan"
        AppLanguage.ARABIC -> "يرجى الاسترداد باستخدام رمز التفعيل لخطة مدى الحياة"
    }

    val restorePurchase: String get() = when (lang) {
        AppLanguage.CHINESE -> "恢复购买"
        AppLanguage.ENGLISH -> "Restore Purchase"
        AppLanguage.ARABIC -> "استعادة الشراء"
    }

    val subscriptionDisclaimer: String get() = when (lang) {
        AppLanguage.CHINESE -> "月度/年度订阅通过 Google Play 自动扣费，可随时取消。终身套餐通过激活码兑换，一次购买永久有效。"
        AppLanguage.ENGLISH -> "Monthly/Yearly subscriptions are billed automatically through Google Play and can be cancelled anytime. Lifetime plans are redeemed with activation code, one-time purchase for lifetime access."
        AppLanguage.ARABIC -> "الاشتراكات الشهرية/السنوية يتم خصمها تلقائياً عبر Google Play ويمكن إلغاؤها في أي وقت. خطط مدى الحياة يتم استردادها برمز التفعيل، شراء لمرة واحدة للوصول مدى الحياة."
    }

    val recommended: String get() = when (lang) {
        AppLanguage.CHINESE -> "推荐"
        AppLanguage.ENGLISH -> "Recommended"
        AppLanguage.ARABIC -> "موصى به"
    }

    val currentScheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前方案"
        AppLanguage.ENGLISH -> "Current Plan"
        AppLanguage.ARABIC -> "الخطة الحالية"
    }

    val basicPlan: String get() = when (lang) {
        AppLanguage.CHINESE -> "基础方案"
        AppLanguage.ENGLISH -> "Basic Plan"
        AppLanguage.ARABIC -> "الخطة الأساسية"
    }

    val hasHigherPlan: String get() = when (lang) {
        AppLanguage.CHINESE -> "已拥有更高级方案"
        AppLanguage.ENGLISH -> "Already have a higher plan"
        AppLanguage.ARABIC -> "لديك بالفعل خطة أعلى"
    }

    val subscribeTierName: String get() = when (lang) {
        AppLanguage.CHINESE -> "订阅 %s"
        AppLanguage.ENGLISH -> "Subscribe %s"
        AppLanguage.ARABIC -> "اشتراك %s"
    }

    val proCloudProjects: String get() = when (lang) {
        AppLanguage.CHINESE -> "云端项目管理"
        AppLanguage.ENGLISH -> "Cloud Project Management"
        AppLanguage.ARABIC -> "إدارة المشاريع السحابية"
    }

    val proCloudProjectsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "最多 5 个项目"
        AppLanguage.ENGLISH -> "Up to 5 projects"
        AppLanguage.ARABIC -> "ما يصل إلى 5 مشاريع"
    }

    val proActivationCodeSystem: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活码系统"
        AppLanguage.ENGLISH -> "Activation Code System"
        AppLanguage.ARABIC -> "نظام رمز التفعيل"
    }

    val proActivationCodeSystemDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "最多 1000 个激活码"
        AppLanguage.ENGLISH -> "Up to 1000 activation codes"
        AppLanguage.ARABIC -> "ما يصل إلى 1000 رمز تفعيل"
    }

    val proAutoUpdates: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本自动更新"
        AppLanguage.ENGLISH -> "Automatic Version Updates"
        AppLanguage.ARABIC -> "تحديثات الإصدار التلقائية"
    }

    val proAutoUpdatesDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "GitHub / Gitee / R2"
        AppLanguage.ENGLISH -> "GitHub / Gitee / R2"
        AppLanguage.ARABIC -> "GitHub / Gitee / R2"
    }

    val proAnnouncements: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用公告推送"
        AppLanguage.ENGLISH -> "App Announcement Push"
        AppLanguage.ARABIC -> "دفع إعلانات التطبيق"
    }

    val proAnnouncementsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "最多 10 条活跃公告"
        AppLanguage.ENGLISH -> "Up to 10 active announcements"
        AppLanguage.ARABIC -> "ما يصل إلى 10 إعلانات نشطة"
    }

    val proRemoteConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "远程配置"
        AppLanguage.ENGLISH -> "Remote Configuration"
        AppLanguage.ARABIC -> "التكوين عن بعد"
    }

    val proRemoteConfigDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "无限 KV 配置项"
        AppLanguage.ENGLISH -> "Unlimited KV config items"
        AppLanguage.ARABIC -> "عناصر تكوين KV غير محدودة"
    }

    val proWebhook: String get() = when (lang) {
        AppLanguage.CHINESE -> "Webhook"
        AppLanguage.ENGLISH -> "Webhook"
        AppLanguage.ARABIC -> "Webhook"
    }

    val proWebhookDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "事件通知集成"
        AppLanguage.ENGLISH -> "Event notification integration"
        AppLanguage.ARABIC -> "تكامل إشعارات الأحداث"
    }

    val proAnalytics: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据分析"
        AppLanguage.ENGLISH -> "Analytics"
        AppLanguage.ARABIC -> "التحليلات"
    }

    val proAnalyticsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "安装/打开/活跃/崩溃"
        AppLanguage.ENGLISH -> "Installs/Opens/Active/Crashes"
        AppLanguage.ARABIC -> "التنزيلات/الفتحات/النشطة/الأعطال"
    }

    val ultraIncludesAllPro: String get() = when (lang) {
        AppLanguage.CHINESE -> "包含全部 Pro 功能"
        AppLanguage.ENGLISH -> "Includes all Pro features"
        AppLanguage.ARABIC -> "يتضمن جميع ميزات Pro"
    }

    val ultraFcmPush: String get() = when (lang) {
        AppLanguage.CHINESE -> "FCM 推送通知"
        AppLanguage.ENGLISH -> "FCM Push Notifications"
        AppLanguage.ARABIC -> "إشعارات الدفع FCM"
    }

    val ultraFcmPushDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "实时触达用户"
        AppLanguage.ENGLISH -> "Real-time user engagement"
        AppLanguage.ARABIC -> "تفاعل المستخدم في الوقت الفعلي"
    }

    val ultraActivationCodeLimit: String get() = when (lang) {
        AppLanguage.CHINESE -> "激活码上限提升"
        AppLanguage.ENGLISH -> "Increased Activation Code Limit"
        AppLanguage.ARABIC -> "زيادة حد رمز التفعيل"
    }

    val ultraActivationCodeLimitDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "最多 5000 个激活码"
        AppLanguage.ENGLISH -> "Up to 5000 activation codes"
        AppLanguage.ARABIC -> "ما يصل إلى 5000 رمز تفعيل"
    }

    val ultraAnnouncementLimit: String get() = when (lang) {
        AppLanguage.CHINESE -> "公告上限提升"
        AppLanguage.ENGLISH -> "Increased Announcement Limit"
        AppLanguage.ARABIC -> "زيادة حد الإعلان"
    }

    val ultraAnnouncementLimitDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "最多 30 条活跃公告"
        AppLanguage.ENGLISH -> "Up to 30 active announcements"
        AppLanguage.ARABIC -> "ما يصل إلى 30 إعلان نشط"
    }

    val ultraProjectLimit: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目上限提升"
        AppLanguage.ENGLISH -> "Increased Project Limit"
        AppLanguage.ARABIC -> "زيادة حد المشروع"
    }

    val ultraProjectLimitDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "最多 10 个项目"
        AppLanguage.ENGLISH -> "Up to 10 projects"
        AppLanguage.ARABIC -> "ما يصل إلى 10 مشاريع"
    }

    val ultraR2Storage: String get() = when (lang) {
        AppLanguage.CHINESE -> "R2 云存储"
        AppLanguage.ENGLISH -> "R2 Cloud Storage"
        AppLanguage.ARABIC -> "التخزين السحابي R2"
    }

    val ultraR2StorageDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Cloudflare CDN 加速"
        AppLanguage.ENGLISH -> "Cloudflare CDN accelerated"
        AppLanguage.ARABIC -> "مسرع عبر Cloudflare CDN"
    }

    val ultraPrioritySupport: String get() = when (lang) {
        AppLanguage.CHINESE -> "优先技术支持"
        AppLanguage.ENGLISH -> "Priority Technical Support"
        AppLanguage.ARABIC -> "دعم فني ذو أولوية"
    }

    val ultraPrioritySupportDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "48 小时内响应"
        AppLanguage.ENGLISH -> "48-hour response time"
        AppLanguage.ARABIC -> "وقت استجابة 48 ساعة"
    }

    val freeUnlimitedApps: String get() = when (lang) {
        AppLanguage.CHINESE -> "无限创建应用"
        AppLanguage.ENGLISH -> "Unlimited App Creation"
        AppLanguage.ARABIC -> "إنشاء تطبيقات غير محدود"
    }

    val freeUnlimitedAppsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Web / 媒体 / HTML / 前端"
        AppLanguage.ENGLISH -> "Web / Media / HTML / Frontend"
        AppLanguage.ARABIC -> "Web / وسائط / HTML / واجهة أمامية"
    }

    val freeLocalBuild: String get() = when (lang) {
        AppLanguage.CHINESE -> "APK 本地构建"
        AppLanguage.ENGLISH -> "Local APK Build"
        AppLanguage.ARABIC -> "بناء APK محلي"
    }

    val freeLocalBuildDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义包名、签名"
        AppLanguage.ENGLISH -> "Custom package name, signing"
        AppLanguage.ARABIC -> "اسم الحزمة المخصص، التوقيع"
    }

    val freeExtensionSystem: String get() = when (lang) {
        AppLanguage.CHINESE -> "扩展模块系统"
        AppLanguage.ENGLISH -> "Extension Module System"
        AppLanguage.ARABIC -> "نظام الوحدة الإضافية"
    }

    val freeExtensionSystemDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义功能模块"
        AppLanguage.ENGLISH -> "Custom feature modules"
        AppLanguage.ARABIC -> "وحدات ميزات مخصصة"
    }

    val freeMarketplace: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用&模块市场"
        AppLanguage.ENGLISH -> "App & Module Marketplace"
        AppLanguage.ARABIC -> "سوق التطبيقات والوحدات"
    }

    val freeMarketplaceDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "浏览和安装社区内容"
        AppLanguage.ENGLISH -> "Browse and install community content"
        AppLanguage.ARABIC -> "تصفح وتثبيت محتوى المجتمع"
    }

    val freeAiAssistant: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 编程助手"
        AppLanguage.ENGLISH -> "AI Coding Assistant"
        AppLanguage.ARABIC -> "مساعد برمجة AI"
    }

    val freeAiAssistantDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML / 前端 / Node.js"
        AppLanguage.ENGLISH -> "HTML / Frontend / Node.js"
        AppLanguage.ARABIC -> "HTML / واجهة أمامية / Node.js"
    }

    val freeForever: String get() = when (lang) {
        AppLanguage.CHINESE -> "永久免费"
        AppLanguage.ENGLISH -> "Free Forever"
        AppLanguage.ARABIC -> "مجاني إلى الأبد"
    }

    val proUpgradeNote: String get() = when (lang) {
        AppLanguage.CHINESE -> "Pro 终身用户仅需补差价 $100"
        AppLanguage.ENGLISH -> "Pro Lifetime users only need to pay the $100 difference"
        AppLanguage.ARABIC -> "مستخدمو Pro Lifetime يحتاجون فقط لدفع الفرق 100$"
    }

    val subscriptionSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "%s 订阅成功！"
        AppLanguage.ENGLISH -> "%s subscription successful!"
        AppLanguage.ARABIC -> "اشتراك %s ناجح!"
    }

    val tierPro: String get() = when (lang) {
        AppLanguage.CHINESE -> "Pro"
        AppLanguage.ENGLISH -> "Pro"
        AppLanguage.ARABIC -> "Pro"
    }

    val tierUltra: String get() = when (lang) {
        AppLanguage.CHINESE -> "Ultra"
        AppLanguage.ENGLISH -> "Ultra"
        AppLanguage.ARABIC -> "Ultra"
    }

    val tierFree: String get() = when (lang) {
        AppLanguage.CHINESE -> "Free"
        AppLanguage.ENGLISH -> "Free"
        AppLanguage.ARABIC -> "Free"
    }

    val proMonthly: String get() = when (lang) {
        AppLanguage.CHINESE -> "Pro 月度"
        AppLanguage.ENGLISH -> "Pro Monthly"
        AppLanguage.ARABIC -> "Pro شهري"
    }

    val proQuarterly: String get() = when (lang) {
        AppLanguage.CHINESE -> "Pro 季度"
        AppLanguage.ENGLISH -> "Pro Quarterly"
        AppLanguage.ARABIC -> "Pro ربع سنوي"
    }

    val proYearly: String get() = when (lang) {
        AppLanguage.CHINESE -> "Pro 年度"
        AppLanguage.ENGLISH -> "Pro Yearly"
        AppLanguage.ARABIC -> "Pro سنوي"
    }

    val proLifetime: String get() = when (lang) {
        AppLanguage.CHINESE -> "Pro 终身"
        AppLanguage.ENGLISH -> "Pro Lifetime"
        AppLanguage.ARABIC -> "Pro مدى الحياة"
    }

    val ultraMonthly: String get() = when (lang) {
        AppLanguage.CHINESE -> "Ultra 月度"
        AppLanguage.ENGLISH -> "Ultra Monthly"
        AppLanguage.ARABIC -> "Ultra شهري"
    }

    val ultraQuarterly: String get() = when (lang) {
        AppLanguage.CHINESE -> "Ultra 季度"
        AppLanguage.ENGLISH -> "Ultra Quarterly"
        AppLanguage.ARABIC -> "Ultra ربع سنوي"
    }

    val ultraYearly: String get() = when (lang) {
        AppLanguage.CHINESE -> "Ultra 年度"
        AppLanguage.ENGLISH -> "Ultra Yearly"
        AppLanguage.ARABIC -> "Ultra سنوي"
    }

    val ultraLifetime: String get() = when (lang) {
        AppLanguage.CHINESE -> "Ultra 终身"
        AppLanguage.ENGLISH -> "Ultra Lifetime"
        AppLanguage.ARABIC -> "Ultra مدى الحياة"
    }
}
