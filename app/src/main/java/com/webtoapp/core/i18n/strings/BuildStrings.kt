package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object BuildStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage
    // ==================== Action Buttons ====================
    val btnCreate: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建应用"
        AppLanguage.ENGLISH -> "Create App"
        AppLanguage.ARABIC -> "إنشاء تطبيق"
    }

    val btnPreview: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览"
        AppLanguage.ENGLISH -> "Preview"
        AppLanguage.ARABIC -> "معاينة"
    }

    val btnExport: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出APK"
        AppLanguage.ENGLISH -> "Export APK"
        AppLanguage.ARABIC -> "تصدير APK"
    }

    val btnSave: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存"
        AppLanguage.ENGLISH -> "Save"
        AppLanguage.ARABIC -> "حفظ"
    }

    val btnCancel: String get() = when (lang) {
        AppLanguage.CHINESE -> "取消"
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.ARABIC -> "إلغاء"
    }

    val btnDelete: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除"
        AppLanguage.ENGLISH -> "Delete"
        AppLanguage.ARABIC -> "حذف"
    }

    val btnEdit: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑"
        AppLanguage.ENGLISH -> "Edit"
        AppLanguage.ARABIC -> "تعديل"
    }

    val editCoreConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑核心配置"
        AppLanguage.ENGLISH -> "Edit Core Config"
        AppLanguage.ARABIC -> "تعديل الإعدادات الأساسية"
    }

    val editCommonConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑通用配置"
        AppLanguage.ENGLISH -> "Edit Common Config"
        AppLanguage.ARABIC -> "تعديل الإعدادات العامة"
    }

    val btnLaunch: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动"
        AppLanguage.ENGLISH -> "Launch"
        AppLanguage.ARABIC -> "تشغيل"
    }

    val btnShortcut: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建快捷方式"
        AppLanguage.ENGLISH -> "Create Shortcut"
        AppLanguage.ARABIC -> "إنشاء اختصار"
    }

    val btnConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确认"
        AppLanguage.ENGLISH -> "Confirm"
        AppLanguage.ARABIC -> "تأكيد"
    }

    val btnOk: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定"
        AppLanguage.ENGLISH -> "OK"
        AppLanguage.ARABIC -> "موافق"
    }

    val btnRetry: String get() = when (lang) {
        AppLanguage.CHINESE -> "重试"
        AppLanguage.ENGLISH -> "Retry"
        AppLanguage.ARABIC -> "إعادة المحاولة"
    }

    val btnImport: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入"
        AppLanguage.ENGLISH -> "Import"
        AppLanguage.ARABIC -> "استيراد"
    }

    val btnBuild: String get() = when (lang) {
        AppLanguage.CHINESE -> "构建"
        AppLanguage.ENGLISH -> "Build"
        AppLanguage.ARABIC -> "بناء"
    }

    val btnStartBuild: String get() = when (lang) {
        AppLanguage.CHINESE -> "开始构建"
        AppLanguage.ENGLISH -> "Start Build"
        AppLanguage.ARABIC -> "بدء البناء"
    }

    val btnReset: String get() = when (lang) {
        AppLanguage.CHINESE -> "重置"
        AppLanguage.ENGLISH -> "Reset"
        AppLanguage.ARABIC -> "إعادة تعيين"
    }

    val btnClearCache: String get() = when (lang) {
        AppLanguage.CHINESE -> "清理缓存"
        AppLanguage.ENGLISH -> "Clear Cache"
        AppLanguage.ARABIC -> "مسح ذاكرة التخزين المؤقت"
    }

    val help: String get() = when (lang) {
        AppLanguage.CHINESE -> "帮助"
        AppLanguage.ENGLISH -> "Help"
        AppLanguage.ARABIC -> "مساعدة"
    }

    val usageHelp: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用帮助"
        AppLanguage.ENGLISH -> "Usage Help"
        AppLanguage.ARABIC -> "مساعدة الاستخدام"
    }

    val iUnderstand: String get() = when (lang) {
        AppLanguage.CHINESE -> "我知道了"
        AppLanguage.ENGLISH -> "I Understand"
        AppLanguage.ARABIC -> "فهمت"
    }

    val selectModuleCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择模块分类"
        AppLanguage.ENGLISH -> "Select Module Category"
        AppLanguage.ARABIC -> "اختر فئة الوحدة"
    }

    val autoDetect: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动识别"
        AppLanguage.ENGLISH -> "Auto Detect"
        AppLanguage.ARABIC -> "الكشف التلقائي"
    }

    val autoDetectCategoryHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "让 AI 根据需求自动选择分类"
        AppLanguage.ENGLISH -> "Let AI automatically select category based on requirements"
        AppLanguage.ARABIC -> "دع الذكاء الاصطناعي يختار الفئة تلقائيًا بناءً على المتطلبات"
    }
    // ==================== Encryption Options ====================
    val configFileEncryption: String get() = when (lang) {
        AppLanguage.CHINESE -> "配置文件"
        AppLanguage.ENGLISH -> "Config File"
        AppLanguage.ARABIC -> "ملف التكوين"
    }

    val configFileEncryptionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "加密 app_config.json"
        AppLanguage.ENGLISH -> "Encrypt app_config.json"
        AppLanguage.ARABIC -> "تشفير app_config.json"
    }

    val htmlCssJsEncryption: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML/CSS/JS"
        AppLanguage.ENGLISH -> "HTML/CSS/JS"
        AppLanguage.ARABIC -> "HTML/CSS/JS"
    }

    val htmlCssJsEncryptionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "加密网页代码文件"
        AppLanguage.ENGLISH -> "Encrypt web code files"
        AppLanguage.ARABIC -> "تشفير ملفات كود الويب"
    }

    val mediaFileEncryption: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体文件"
        AppLanguage.ENGLISH -> "Media Files"
        AppLanguage.ARABIC -> "ملفات الوسائط"
    }

    val mediaFileEncryptionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "加密图片和视频"
        AppLanguage.ENGLISH -> "Encrypt images and videos"
        AppLanguage.ARABIC -> "تشفير الصور ومقاطع الفيديو"
    }

    val splashEncryption: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动画面"
        AppLanguage.ENGLISH -> "Splash Screen"
        AppLanguage.ARABIC -> "شاشة البداية"
    }

    val splashEncryptionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "加密启动画面资源"
        AppLanguage.ENGLISH -> "Encrypt splash screen resources"
        AppLanguage.ARABIC -> "تشفير موارد شاشة البداية"
    }

    val bgmEncryption: String get() = when (lang) {
        AppLanguage.CHINESE -> "背景音乐"
        AppLanguage.ENGLISH -> "Background Music"
        AppLanguage.ARABIC -> "موسيقى الخلفية"
    }

    val bgmEncryptionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "加密 BGM 文件"
        AppLanguage.ENGLISH -> "Encrypt BGM files"
        AppLanguage.ARABIC -> "تشفير ملفات BGM"
    }

    val encryptionStrength: String get() = when (lang) {
        AppLanguage.CHINESE -> "加密强度"
        AppLanguage.ENGLISH -> "Encryption Strength"
        AppLanguage.ARABIC -> "قوة التشفير"
    }

    val securityProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "安全保护"
        AppLanguage.ENGLISH -> "Security Protection"
        AppLanguage.ARABIC -> "الحماية الأمنية"
    }

    val integrityCheck: String get() = when (lang) {
        AppLanguage.CHINESE -> "完整性检查"
        AppLanguage.ENGLISH -> "Integrity Check"
        AppLanguage.ARABIC -> "فحص السلامة"
    }

    val integrityCheckHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "验证 APK 是否被篡改"
        AppLanguage.ENGLISH -> "Verify if APK has been tampered"
        AppLanguage.ARABIC -> "التحقق مما إذا كان APK قد تم العبث به"
    }

    val antiDebugProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "反调试保护"
        AppLanguage.ENGLISH -> "Anti-Debug Protection"
        AppLanguage.ARABIC -> "حماية مكافحة التصحيح"
    }

    val antiDebugProtectionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测并阻止调试器附加"
        AppLanguage.ENGLISH -> "Detect and block debugger attachment"
        AppLanguage.ARABIC -> "اكتشاف ومنع إرفاق المصحح"
    }

    val antiTamperProtection: String get() = when (lang) {
        AppLanguage.CHINESE -> "防篡改保护"
        AppLanguage.ENGLISH -> "Anti-Tamper Protection"
        AppLanguage.ARABIC -> "حماية مكافحة العبث"
    }

    val antiTamperProtectionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测代码和资源修改"
        AppLanguage.ENGLISH -> "Detect code and resource modifications"
        AppLanguage.ARABIC -> "اكتشاف تعديلات الكود والموارد"
    }

    val stringObfuscation: String get() = when (lang) {
        AppLanguage.CHINESE -> "字符串混淆"
        AppLanguage.ENGLISH -> "String Obfuscation"
        AppLanguage.ARABIC -> "تشويش السلاسل"
    }

    val stringObfuscationHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "混淆敏感字符串（实验性）"
        AppLanguage.ENGLISH -> "Obfuscate sensitive strings (experimental)"
        AppLanguage.ARABIC -> "تشويش السلاسل الحساسة (تجريبي)"
    }

    val securityWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "安全保护可能影响在模拟器或已 Root 设备上的运行"
        AppLanguage.ENGLISH -> "Security protection may affect running on emulators or rooted devices"
        AppLanguage.ARABIC -> "قد تؤثر الحماية الأمنية على التشغيل على المحاكيات أو الأجهزة ذات صلاحيات الجذر"
    }

    val encryptionDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "加密后的资源无法被直接查看或提取，可有效保护您的代码和内容。加密基于 AES-256-GCM 算法，密钥与应用签名绑定。"
        AppLanguage.ENGLISH -> "Encrypted resources cannot be directly viewed or extracted, effectively protecting your code and content. Encryption is based on AES-256-GCM algorithm, with keys bound to app signature."
        AppLanguage.ARABIC -> "لا يمكن عرض أو استخراج الموارد المشفرة مباشرة، مما يحمي الكود والمحتوى بشكل فعال. يعتمد التشفير على خوارزمية AES-256-GCM، مع ربط المفاتيح بتوقيع التطبيق."
    }

    val pbkdf2Iterations: String get() = when (lang) {
        AppLanguage.CHINESE -> "PBKDF2 迭代"
        AppLanguage.ENGLISH -> "PBKDF2 Iterations"
        AppLanguage.ARABIC -> "تكرارات PBKDF2"
    }
    // ==================== Custom Signature Strings ====================
    val customSigning: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义签名"
        AppLanguage.ENGLISH -> "Custom Signing"
        AppLanguage.ARABIC -> "التوقيع المخصص"
    }

    val customSigningDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入自己的签名证书，使打包的 APK 使用您的签名"
        AppLanguage.ENGLISH -> "Import your own signing certificate for built APKs"
        AppLanguage.ARABIC -> "استيراد شهادة التوقيع الخاصة بك لملفات APK المبنية"
    }

    val currentSigningStatus: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前签名状态"
        AppLanguage.ENGLISH -> "Current Signing Status"
        AppLanguage.ARABIC -> "حالة التوقيع الحالية"
    }

    val signingTypeAutoGenerated: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动生成的证书"
        AppLanguage.ENGLISH -> "Auto-generated certificate"
        AppLanguage.ARABIC -> "شهادة مولدة تلقائيًا"
    }

    val signingTypeCustom: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义证书"
        AppLanguage.ENGLISH -> "Custom certificate"
        AppLanguage.ARABIC -> "شهادة مخصصة"
    }

    val signingTypeAndroidKeyStore: String get() = when (lang) {
        AppLanguage.CHINESE -> "Android 系统密钥库"
        AppLanguage.ENGLISH -> "Android KeyStore"
        AppLanguage.ARABIC -> "مخزن مفاتيح أندرويد"
    }

    val importKeystore: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入签名文件"
        AppLanguage.ENGLISH -> "Import Keystore"
        AppLanguage.ARABIC -> "استيراد ملف التوقيع"
    }

    val exportKeystore: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出签名文件"
        AppLanguage.ENGLISH -> "Export Keystore"
        AppLanguage.ARABIC -> "تصدير ملف التوقيع"
    }

    val removeCustomKeystore: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除自定义证书"
        AppLanguage.ENGLISH -> "Remove Custom Certificate"
        AppLanguage.ARABIC -> "إزالة الشهادة المخصصة"
    }

    val keystorePassword: String get() = when (lang) {
        AppLanguage.CHINESE -> "密钥库密码"
        AppLanguage.ENGLISH -> "Keystore Password"
        AppLanguage.ARABIC -> "كلمة مرور مخزن المفاتيح"
    }

    val keystorePasswordHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "请输入签名文件的密码"
        AppLanguage.ENGLISH -> "Enter keystore password"
        AppLanguage.ARABIC -> "أدخل كلمة مرور مخزن المفاتيح"
    }

    val exportPassword: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出密码"
        AppLanguage.ENGLISH -> "Export Password"
        AppLanguage.ARABIC -> "كلمة مرور التصدير"
    }

    val exportPasswordHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "设置导出文件的密码"
        AppLanguage.ENGLISH -> "Set password for exported file"
        AppLanguage.ARABIC -> "تعيين كلمة مرور للملف المصدر"
    }

    val keystoreImportSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "签名证书导入成功"
        AppLanguage.ENGLISH -> "Keystore imported successfully"
        AppLanguage.ARABIC -> "تم استيراد مخزن المفاتيح بنجاح"
    }

    val keystoreImportFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "签名证书导入失败，请检查密码是否正确"
        AppLanguage.ENGLISH -> "Keystore import failed, please check password"
        AppLanguage.ARABIC -> "فشل استيراد مخزن المفاتيح، يرجى التحقق من كلمة المرور"
    }

    val keystoreExportSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "签名证书导出成功"
        AppLanguage.ENGLISH -> "Keystore exported successfully"
        AppLanguage.ARABIC -> "تم تصدير مخزن المفاتيح بنجاح"
    }

    val keystoreExportFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "签名证书导出失败"
        AppLanguage.ENGLISH -> "Keystore export failed"
        AppLanguage.ARABIC -> "فشل تصدير مخزن المفاتيح"
    }

    val keystoreRemoveSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "已删除自定义证书，将使用自动生成的证书"
        AppLanguage.ENGLISH -> "Custom certificate removed, will use auto-generated certificate"
        AppLanguage.ARABIC -> "تمت إزالة الشهادة المخصصة، سيتم استخدام الشهادة المولدة تلقائيًا"
    }

    val keystoreRemoveConfirm: String get() = when (lang) {
        AppLanguage.CHINESE -> "确定要删除自定义签名证书吗？删除后将使用自动生成的证书进行签名。"
        AppLanguage.ENGLISH -> "Are you sure you want to remove the custom certificate? Auto-generated certificate will be used for signing."
        AppLanguage.ARABIC -> "هل أنت متأكد من إزالة الشهادة المخصصة؟ سيتم استخدام الشهادة المولدة تلقائيًا للتوقيع."
    }

    val supportedKeystoreFormats: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持 .p12 / .pfx / .jks / .keystore 格式"
        AppLanguage.ENGLISH -> "Supports .p12 / .pfx / .jks / .keystore formats"
        AppLanguage.ARABIC -> "يدعم صيغ .p12 / .pfx / .jks / .keystore"
    }

    val customSigningNote: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义签名为全局设置，导入后所有新打包的 APK 都将使用此证书签名"
        AppLanguage.ENGLISH -> "Custom signing is a global setting. All newly built APKs will use this certificate"
        AppLanguage.ARABIC -> "التوقيع المخصص إعداد عام. جميع ملفات APK المبنية حديثًا ستستخدم هذه الشهادة"
    }
}
