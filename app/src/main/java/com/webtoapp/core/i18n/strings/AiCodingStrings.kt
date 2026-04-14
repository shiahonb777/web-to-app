package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object AiCodingStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage
    // ==================== HTML ====================
    val downloadFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载"
        AppLanguage.ENGLISH -> "Download"
        AppLanguage.ARABIC -> "تحميل"
    }

    val exportAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出全部"
        AppLanguage.ENGLISH -> "Export All"
        AppLanguage.ARABIC -> "تصدير الكل"
    }

    val thinking: String get() = when (lang) {
        AppLanguage.CHINESE -> "思考中"
        AppLanguage.ENGLISH -> "Thinking"
        AppLanguage.ARABIC -> "جاري التفكير"
    }

    val thinkingDots: String get() = when (lang) {
        AppLanguage.CHINESE -> "思考中..."
        AppLanguage.ENGLISH -> "Thinking..."
        AppLanguage.ARABIC -> "جاري التفكير..."
    }

    val describeHtmlPage: String get() = when (lang) {
        AppLanguage.CHINESE -> "描述你想要的 HTML 页面..."
        AppLanguage.ENGLISH -> "Describe the HTML page you want..."
        AppLanguage.ARABIC -> "صف صفحة HTML التي تريدها..."
    }

    val btnSend: String get() = when (lang) {
        AppLanguage.CHINESE -> "发送"
        AppLanguage.ENGLISH -> "Send"
        AppLanguage.ARABIC -> "إرسال"
    }

    val btnRestore: String get() = when (lang) {
        AppLanguage.CHINESE -> "恢复"
        AppLanguage.ENGLISH -> "Restore"
        AppLanguage.ARABIC -> "استعادة"
    }

    val fileCountFormat: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d / %d 个文件"
        AppLanguage.ENGLISH -> "%d / %d files"
        AppLanguage.ARABIC -> "%d / %d ملفات"
    }

    val linesCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 行"
        AppLanguage.ENGLISH -> "%d lines"
        AppLanguage.ARABIC -> "%d سطر"
    }

    val filesCountShort: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个文件"
        AppLanguage.ENGLISH -> "%d files"
        AppLanguage.ARABIC -> "%d ملفات"
    }

    val rules: String get() = when (lang) {
        AppLanguage.CHINESE -> "规则"
        AppLanguage.ENGLISH -> "Rules"
        AppLanguage.ARABIC -> "القواعد"
    }

    val selectFromTemplate: String get() = when (lang) {
        AppLanguage.CHINESE -> "从模板选择"
        AppLanguage.ENGLISH -> "Select from template"
        AppLanguage.ARABIC -> "اختر من القالب"
    }

    val selectRuleTemplate: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择规则模板"
        AppLanguage.ENGLISH -> "Select rule template"
        AppLanguage.ARABIC -> "اختر قالب القاعدة"
    }

    val noImageModel: String get() = when (lang) {
        AppLanguage.CHINESE -> "不使用图像模型"
        AppLanguage.ENGLISH -> "No image model"
        AppLanguage.ARABIC -> "بدون نموذج صور"
    }

    val selectImageModel: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择图像模型"
        AppLanguage.ENGLISH -> "Select image model"
        AppLanguage.ARABIC -> "اختر نموذج الصور"
    }

    val configureMoreModels: String get() = when (lang) {
        AppLanguage.CHINESE -> "配置更多模型"
        AppLanguage.ENGLISH -> "Configure more models"
        AppLanguage.ARABIC -> "تكوين المزيد من النماذج"
    }

    val projectFiles: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目文件"
        AppLanguage.ENGLISH -> "Project Files"
        AppLanguage.ARABIC -> "ملفات المشروع"
    }

    val refresh: String get() = when (lang) {
        AppLanguage.CHINESE -> "Refresh"
        AppLanguage.ENGLISH -> "Refresh"
        AppLanguage.ARABIC -> "تحديث"
    }

    val goBack: String get() = when (lang) {
        AppLanguage.CHINESE -> "后退"
        AppLanguage.ENGLISH -> "Back"
        AppLanguage.ARABIC -> "رجوع"
    }

    val goForward: String get() = when (lang) {
        AppLanguage.CHINESE -> "前进"
        AppLanguage.ENGLISH -> "Forward"
        AppLanguage.ARABIC -> "إلى الأمام"
    }

    val noFilesYet: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无文件"
        AppLanguage.ENGLISH -> "No files yet"
        AppLanguage.ARABIC -> "لا توجد ملفات بعد"
    }

    val versionHistory: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本历史"
        AppLanguage.ENGLISH -> "Version History"
        AppLanguage.ARABIC -> "سجل الإصدارات"
    }

    val addNewRule: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加新规则..."
        AppLanguage.ENGLISH -> "Add new rule..."
        AppLanguage.ARABIC -> "إضافة قاعدة جديدة..."
    }
    // ==================== HTML AI ====================
    val styleModernMinimal: String get() = when (lang) {
        AppLanguage.CHINESE -> "现代简约"
        AppLanguage.ENGLISH -> "Modern Minimal"
        AppLanguage.ARABIC -> "حديث بسيط"
    }

    val styleModernMinimalDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "干净利落的现代设计，大量留白，强调内容"
        AppLanguage.ENGLISH -> "Clean modern design with ample whitespace, content-focused"
        AppLanguage.ARABIC -> "تصميم حديث نظيف مع مساحات بيضاء واسعة، يركز على المحتوى"
    }

    val styleGlassmorphism: String get() = when (lang) {
        AppLanguage.CHINESE -> "玻璃拟态"
        AppLanguage.ENGLISH -> "Glassmorphism"
        AppLanguage.ARABIC -> "تأثير الزجاج"
    }

    val styleGlassmorphismDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "透明模糊效果，创造深度层次感"
        AppLanguage.ENGLISH -> "Transparent blur effect, creates depth and layers"
        AppLanguage.ARABIC -> "تأثير ضبابي شفاف، يخلق عمقًا وطبقات"
    }

    val styleNeumorphism: String get() = when (lang) {
        AppLanguage.CHINESE -> "新拟物化"
        AppLanguage.ENGLISH -> "Neumorphism"
        AppLanguage.ARABIC -> "التصميم الجديد"
    }

    val styleNeumorphismDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "软阴影创造的凸起/凹陷效果"
        AppLanguage.ENGLISH -> "Soft shadows creating raised/sunken effects"
        AppLanguage.ARABIC -> "ظلال ناعمة تخلق تأثيرات بارزة/غائرة"
    }

    val styleDarkMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "暗黑主题"
        AppLanguage.ENGLISH -> "Dark Mode"
        AppLanguage.ARABIC -> "الوضع الداكن"
    }

    val styleDarkModeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "深色背景配亮色文字，护眼且现代"
        AppLanguage.ENGLISH -> "Dark background with light text, eye-friendly and modern"
        AppLanguage.ARABIC -> "خلفية داكنة مع نص فاتح، مريح للعين وعصري"
    }

    val styleCyberpunk: String get() = when (lang) {
        AppLanguage.CHINESE -> "赛博朋克"
        AppLanguage.ENGLISH -> "Cyberpunk"
        AppLanguage.ARABIC -> "سايبربانك"
    }

    val styleCyberpunkDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "霓虹灯效、科技感、未来主义"
        AppLanguage.ENGLISH -> "Neon lights, tech vibes, futurism"
        AppLanguage.ARABIC -> "أضواء نيون، أجواء تقنية، مستقبلية"
    }

    val styleGradient: String get() = when (lang) {
        AppLanguage.CHINESE -> "渐变炫彩"
        AppLanguage.ENGLISH -> "Gradient Colors"
        AppLanguage.ARABIC -> "ألوان متدرجة"
    }

    val styleGradientDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "丰富的渐变色彩，活力四射"
        AppLanguage.ENGLISH -> "Rich gradient colors, vibrant and energetic"
        AppLanguage.ARABIC -> "ألوان متدرجة غنية، حيوية ونشطة"
    }

    val styleMinimal: String get() = when (lang) {
        AppLanguage.CHINESE -> "极简主义"
        AppLanguage.ENGLISH -> "Minimalist"
        AppLanguage.ARABIC -> "بساطة متناهية"
    }

    val styleMinimalDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "去除一切不必要的装饰，只保留核心"
        AppLanguage.ENGLISH -> "Remove all unnecessary decoration, keep only essentials"
        AppLanguage.ARABIC -> "إزالة كل الزخارف غير الضرورية، الاحتفاظ بالأساسيات فقط"
    }

    val styleNature: String get() = when (lang) {
        AppLanguage.CHINESE -> "自然清新"
        AppLanguage.ENGLISH -> "Nature Fresh"
        AppLanguage.ARABIC -> "طبيعي منعش"
    }

    val styleNatureDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "来自自然的配色，宁静舒适"
        AppLanguage.ENGLISH -> "Natural color palette, calm and comfortable"
        AppLanguage.ARABIC -> "لوحة ألوان طبيعية، هادئة ومريحة"
    }

    val styleCuteCartoon: String get() = when (lang) {
        AppLanguage.CHINESE -> "卡通可爱"
        AppLanguage.ENGLISH -> "Cute Cartoon"
        AppLanguage.ARABIC -> "كرتون لطيف"
    }

    val styleCuteCartoonDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "萌系卡通风格，圆润可爱"
        AppLanguage.ENGLISH -> "Cute cartoon style, rounded and adorable"
        AppLanguage.ARABIC -> "نمط كرتوني لطيف، مستدير وجميل"
    }

    val styleNeonGlow: String get() = when (lang) {
        AppLanguage.CHINESE -> "霓虹灯光"
        AppLanguage.ENGLISH -> "Neon Glow"
        AppLanguage.ARABIC -> "توهج النيون"
    }

    val styleNeonGlowDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "发光霓虹效果，夜店风格"
        AppLanguage.ENGLISH -> "Glowing neon effect, nightclub style"
        AppLanguage.ARABIC -> "تأثير نيون متوهج، نمط الملهى الليلي"
    }
    // ==================== HTML ====================
    val toolWriteHtml: String get() = when (lang) {
        AppLanguage.CHINESE -> "写入 HTML"
        AppLanguage.ENGLISH -> "Write HTML"
        AppLanguage.ARABIC -> "كتابة HTML"
    }

    val toolWriteHtmlDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建或覆盖完整的 HTML 页面"
        AppLanguage.ENGLISH -> "Create or overwrite complete HTML page"
        AppLanguage.ARABIC -> "إنشاء أو الكتابة فوق صفحة HTML كاملة"
    }

    val toolEditHtml: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑 HTML"
        AppLanguage.ENGLISH -> "Edit HTML"
        AppLanguage.ARABIC -> "تحرير HTML"
    }

    val toolEditHtmlDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "在指定位置替换、插入或删除代码片段"
        AppLanguage.ENGLISH -> "Replace, insert or delete code at specified location"
        AppLanguage.ARABIC -> "استبدال أو إدراج أو حذف الكود في موقع محدد"
    }

    val toolGenerateImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 图像生成"
        AppLanguage.ENGLISH -> "AI Image Generation"
        AppLanguage.ARABIC -> "توليد صورة بالذكاء الاصطناعي"
    }

    val toolGenerateImageDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 AI 生成图像并嵌入到 HTML 中作为插图"
        AppLanguage.ENGLISH -> "Generate images with AI and embed them in HTML"
        AppLanguage.ARABIC -> "إنشاء صور بالذكاء الاصطناعي وتضمينها في HTML"
    }

    val toolGetConsoleLogs: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取控制台日志"
        AppLanguage.ENGLISH -> "Get Console Logs"
        AppLanguage.ARABIC -> "الحصول على سجلات وحدة التحكم"
    }

    val toolGetConsoleLogsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取页面运行时的 console.log 输出和错误信息"
        AppLanguage.ENGLISH -> "Get console.log output and error info from page runtime"
        AppLanguage.ARABIC -> "الحصول على مخرجات console.log ومعلومات الأخطاء"
    }

    val toolCheckSyntax: String get() = when (lang) {
        AppLanguage.CHINESE -> "语法检查"
        AppLanguage.ENGLISH -> "Syntax Check"
        AppLanguage.ARABIC -> "فحص بناء الجملة"
    }

    val toolCheckSyntaxDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查 HTML/CSS/JavaScript 语法错误"
        AppLanguage.ENGLISH -> "Check HTML/CSS/JavaScript syntax errors"
        AppLanguage.ARABIC -> "فحص أخطاء بناء جملة HTML/CSS/JavaScript"
    }

    val toolAutoFix: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动修复"
        AppLanguage.ENGLISH -> "Auto Fix"
        AppLanguage.ARABIC -> "إصلاح تلقائي"
    }

    val toolAutoFixDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动修复检测到的语法错误"
        AppLanguage.ENGLISH -> "Automatically fix detected syntax errors"
        AppLanguage.ARABIC -> "إصلاح أخطاء بناء الجملة المكتشفة تلقائيًا"
    }

    val toolReadCurrentCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "读取当前代码"
        AppLanguage.ENGLISH -> "Read Current Code"
        AppLanguage.ARABIC -> "قراءة الكود الحالي"
    }

    val toolReadCurrentCodeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "读取当前 HTML 代码内容，用于在编辑前了解现有代码"
        AppLanguage.ENGLISH -> "Read current HTML code content to understand existing code before editing"
        AppLanguage.ARABIC -> "قراءة محتوى كود HTML الحالي لفهم الكود الموجود قبل التحرير"
    }

    val featureReadCurrentCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "读取当前代码"
        AppLanguage.ENGLISH -> "Read Current Code"
        AppLanguage.ARABIC -> "قراءة الكود الحالي"
    }

    val readCurrentCodeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "读取当前 HTML 代码，编辑前先了解现有代码结构"
        AppLanguage.ENGLISH -> "Read current HTML code to understand existing code structure before editing"
        AppLanguage.ARABIC -> "قراءة كود HTML الحالي لفهم هيكل الكود الموجود قبل التحرير"
    }

    val required: String get() = when (lang) {
        AppLanguage.CHINESE -> "必需"
        AppLanguage.ENGLISH -> "Required"
        AppLanguage.ARABIC -> "مطلوب"
    }

    val requiresImageModel: String get() = when (lang) {
        AppLanguage.CHINESE -> "需选择图像模型"
        AppLanguage.ENGLISH -> "Requires image model"
        AppLanguage.ARABIC -> "يتطلب نموذج صورة"
    }
    // ==================== ====================
    val secEvalDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 eval() 执行动态代码"
        AppLanguage.ENGLISH -> "Using eval() to execute dynamic code"
        AppLanguage.ARABIC -> "استخدام eval() لتنفيذ كود ديناميكي"
    }

    val secEvalRec: String get() = when (lang) {
        AppLanguage.CHINESE -> "避免使用 eval()，使用更安全的替代方案"
        AppLanguage.ENGLISH -> "Avoid eval(), use safer alternatives"
        AppLanguage.ARABIC -> "تجنب eval()، استخدم بدائل أكثر أماناً"
    }

    val secInnerHtmlDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "直接设置 innerHTML 可能导致 XSS"
        AppLanguage.ENGLISH -> "Setting innerHTML directly may cause XSS"
        AppLanguage.ARABIC -> "تعيين innerHTML مباشرة قد يسبب XSS"
    }

    val secInnerHtmlRec: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 textContent 或 DOMPurify 清理 HTML"
        AppLanguage.ENGLISH -> "Use textContent or DOMPurify to sanitize HTML"
        AppLanguage.ARABIC -> "استخدم textContent أو DOMPurify لتنظيف HTML"
    }

    val secDocWriteDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "document.write 可能被滥用"
        AppLanguage.ENGLISH -> "document.write can be misused"
        AppLanguage.ARABIC -> "document.write قد يُساء استخدامه"
    }

    val secDocWriteRec: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 DOM API 操作页面"
        AppLanguage.ENGLISH -> "Use DOM API to manipulate the page"
        AppLanguage.ARABIC -> "استخدم DOM API للتعامل مع الصفحة"
    }

    val secNewFuncDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "动态创建函数存在安全风险"
        AppLanguage.ENGLISH -> "Dynamically creating functions is a security risk"
        AppLanguage.ARABIC -> "إنشاء الدوال ديناميكياً يشكل مخاطر أمنية"
    }

    val secNewFuncRec: String get() = when (lang) {
        AppLanguage.CHINESE -> "避免动态创建函数"
        AppLanguage.ENGLISH -> "Avoid dynamically creating functions"
        AppLanguage.ARABIC -> "تجنب إنشاء الدوال ديناميكياً"
    }

    val secLocationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "URL 跳转可能被利用"
        AppLanguage.ENGLISH -> "URL redirection may be exploited"
        AppLanguage.ARABIC -> "إعادة توجيه URL قد يُستغل"
    }

    val secLocationRec: String get() = when (lang) {
        AppLanguage.CHINESE -> "验证跳转目标 URL"
        AppLanguage.ENGLISH -> "Validate redirect target URL"
        AppLanguage.ARABIC -> "تحقق من عنوان URL المستهدف"
    }

    val secStorageDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "存储敏感数据需谨慎"
        AppLanguage.ENGLISH -> "Handle sensitive data storage carefully"
        AppLanguage.ARABIC -> "تعامل مع تخزين البيانات الحساسة بحذر"
    }

    val secStorageRec: String get() = when (lang) {
        AppLanguage.CHINESE -> "不要在本地存储中保存敏感信息"
        AppLanguage.ENGLISH -> "Don't store sensitive info in local storage"
        AppLanguage.ARABIC -> "لا تخزن معلومات حساسة في التخزين المحلي"
    }

    val secFetchDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "网络请求需注意安全"
        AppLanguage.ENGLISH -> "Network requests require security awareness"
        AppLanguage.ARABIC -> "طلبات الشبكة تتطلب وعياً أمنياً"
    }

    val secFetchRec: String get() = when (lang) {
        AppLanguage.CHINESE -> "确保请求目标可信，处理 CORS"
        AppLanguage.ENGLISH -> "Ensure request target is trusted, handle CORS"
        AppLanguage.ARABIC -> "تأكد من أن الهدف موثوق، تعامل مع CORS"
    }

    val secPostMsgDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "跨窗口通信需验证来源"
        AppLanguage.ENGLISH -> "Cross-window communication requires origin verification"
        AppLanguage.ARABIC -> "الاتصال عبر النوافذ يتطلب التحقق من المصدر"
    }

    val secPostMsgRec: String get() = when (lang) {
        AppLanguage.CHINESE -> "验证 message 事件的 origin"
        AppLanguage.ENGLISH -> "Verify message event origin"
        AppLanguage.ARABIC -> "تحقق من مصدر حدث الرسالة"
    }

    val secTemplateDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "模板字符串注入风险"
        AppLanguage.ENGLISH -> "Template string injection risk"
        AppLanguage.ARABIC -> "مخاطر حقن سلسلة القالب"
    }

    val secTemplateRec: String get() = when (lang) {
        AppLanguage.CHINESE -> "确保插入的变量已经过验证"
        AppLanguage.ENGLISH -> "Ensure inserted variables are validated"
        AppLanguage.ARABIC -> "تأكد من التحقق من المتغيرات المدرجة"
    }

    val secBase64Desc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Base64 不是加密"
        AppLanguage.ENGLISH -> "Base64 is not encryption"
        AppLanguage.ARABIC -> "Base64 ليس تشفيراً"
    }

    val secBase64Rec: String get() = when (lang) {
        AppLanguage.CHINESE -> "Base64 仅用于编码，不要用于安全目的"
        AppLanguage.ENGLISH -> "Base64 is for encoding only, not for security"
        AppLanguage.ARABIC -> "Base64 للترميز فقط، ليس لأغراض أمنية"
    }
    // ==================== ====================
    val paramCodeToCheck: String get() = when (lang) {
        AppLanguage.CHINESE -> "要检查的代码"
        AppLanguage.ENGLISH -> "Code to check"
        AppLanguage.ARABIC -> "الكود المراد فحصه"
    }

    val paramCodeLang: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码语言"
        AppLanguage.ENGLISH -> "Code language"
        AppLanguage.ARABIC -> "لغة الكود"
    }

    val paramCodeToScan: String get() = when (lang) {
        AppLanguage.CHINESE -> "要扫描的代码"
        AppLanguage.ENGLISH -> "Code to scan"
        AppLanguage.ARABIC -> "الكود المراد مسحه"
    }

    val paramRequirement: String get() = when (lang) {
        AppLanguage.CHINESE -> "功能需求描述"
        AppLanguage.ENGLISH -> "Feature requirement description"
        AppLanguage.ARABIC -> "وصف متطلبات الميزة"
    }

    val paramTargetLang: String get() = when (lang) {
        AppLanguage.CHINESE -> "目标语言"
        AppLanguage.ENGLISH -> "Target language"
        AppLanguage.ARABIC -> "اللغة المستهدفة"
    }

    val paramContext: String get() = when (lang) {
        AppLanguage.CHINESE -> "上下文信息，如现有代码"
        AppLanguage.ENGLISH -> "Context info, e.g. existing code"
        AppLanguage.ARABIC -> "معلومات السياق، مثل الكود الموجود"
    }

    val paramCodeWithErrors: String get() = when (lang) {
        AppLanguage.CHINESE -> "包含错误的代码"
        AppLanguage.ENGLISH -> "Code containing errors"
        AppLanguage.ARABIC -> "الكود الذي يحتوي على أخطاء"
    }

    val paramErrorList: String get() = when (lang) {
        AppLanguage.CHINESE -> "错误列表"
        AppLanguage.ENGLISH -> "Error list"
        AppLanguage.ARABIC -> "قائمة الأخطاء"
    }

    val paramCodeToRefactor: String get() = when (lang) {
        AppLanguage.CHINESE -> "要重构的代码"
        AppLanguage.ENGLISH -> "Code to refactor"
        AppLanguage.ARABIC -> "الكود المراد إعادة هيكلته"
    }

    val paramRefactorGoals: String get() = when (lang) {
        AppLanguage.CHINESE -> "重构目标"
        AppLanguage.ENGLISH -> "Refactoring goals"
        AppLanguage.ARABIC -> "أهداف إعادة الهيكلة"
    }

    val paramTestUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "测试页面 URL"
        AppLanguage.ENGLISH -> "Test page URL"
        AppLanguage.ARABIC -> "عنوان URL لصفحة الاختبار"
    }

    val paramConfigItems: String get() = when (lang) {
        AppLanguage.CHINESE -> "配置项列表"
        AppLanguage.ENGLISH -> "Config items list"
        AppLanguage.ARABIC -> "قائمة عناصر التكوين"
    }

    val paramConfigValues: String get() = when (lang) {
        AppLanguage.CHINESE -> "配置值"
        AppLanguage.ENGLISH -> "Config values"
        AppLanguage.ARABIC -> "قيم التكوين"
    }

    val paramKeywords: String get() = when (lang) {
        AppLanguage.CHINESE -> "关键词"
        AppLanguage.ENGLISH -> "Keywords"
        AppLanguage.ARABIC -> "كلمات مفتاحية"
    }

    val paramSearchKeyword: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索关键词"
        AppLanguage.ENGLISH -> "Search keywords"
        AppLanguage.ARABIC -> "كلمات البحث"
    }

    val paramSnippetCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码片段分类"
        AppLanguage.ENGLISH -> "Snippet category"
        AppLanguage.ARABIC -> "فئة مقتطف الكود"
    }

    val paramModuleIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块图标 (emoji)"
        AppLanguage.ENGLISH -> "Module icon (emoji)"
        AppLanguage.ARABIC -> "أيقونة الوحدة (emoji)"
    }

    val paramRunAt: String get() = when (lang) {
        AppLanguage.CHINESE -> "执行时机"
        AppLanguage.ENGLISH -> "Run timing"
        AppLanguage.ARABIC -> "توقيت التنفيذ"
    }

    val paramModuleId: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块 ID"
        AppLanguage.ENGLISH -> "Module ID"
        AppLanguage.ARABIC -> "معرّف الوحدة"
    }

    val paramPreviewUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览页面 URL"
        AppLanguage.ENGLISH -> "Preview page URL"
        AppLanguage.ARABIC -> "عنوان URL لصفحة المعاينة"
    }
    // ==================== ====================
    val retryAction: String get() = when (lang) {
        AppLanguage.CHINESE -> "Retry"
        AppLanguage.ENGLISH -> "Retry"
        AppLanguage.ARABIC -> "إعادة المحاولة"
    }

    val retryActionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "重新尝试上一次操作"
        AppLanguage.ENGLISH -> "Retry the last operation"
        AppLanguage.ARABIC -> "إعادة محاولة العملية الأخيرة"
    }

    val retryWithDifferentModel: String get() = when (lang) {
        AppLanguage.CHINESE -> "换个模型重试"
        AppLanguage.ENGLISH -> "Retry with Different Model"
        AppLanguage.ARABIC -> "إعادة المحاولة بنموذج مختلف"
    }

    val retryWithDifferentModelHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用其他 AI 模型重试"
        AppLanguage.ENGLISH -> "Retry using another AI model"
        AppLanguage.ARABIC -> "إعادة المحاولة باستخدام نموذج ذكاء اصطناعي آخر"
    }

    val showRawResponse: String get() = when (lang) {
        AppLanguage.CHINESE -> "查看原始响应"
        AppLanguage.ENGLISH -> "Show Raw Response"
        AppLanguage.ARABIC -> "عرض الاستجابة الأصلية"
    }

    val showRawResponseHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示 AI 返回的原始内容"
        AppLanguage.ENGLISH -> "Show original content returned by AI"
        AppLanguage.ARABIC -> "عرض المحتوى الأصلي الذي أرجعه الذكاء الاصطناعي"
    }

    val goToSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "前往设置"
        AppLanguage.ENGLISH -> "Go to Settings"
        AppLanguage.ARABIC -> "الذهاب إلى الإعدادات"
    }

    val goToSettingsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查 API Key 配置"
        AppLanguage.ENGLISH -> "Check API Key configuration"
        AppLanguage.ARABIC -> "التحقق من تكوين مفتاح API"
    }

    val manualEdit: String get() = when (lang) {
        AppLanguage.CHINESE -> "手动编辑"
        AppLanguage.ENGLISH -> "Manual Edit"
        AppLanguage.ARABIC -> "التحرير اليدوي"
    }

    val manualEditHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "手动修改代码"
        AppLanguage.ENGLISH -> "Manually modify code"
        AppLanguage.ARABIC -> "تعديل الكود يدويًا"
    }

    val dismissAction: String get() = when (lang) {
        AppLanguage.CHINESE -> "Close"
        AppLanguage.ENGLISH -> "Dismiss"
        AppLanguage.ARABIC -> "إغلاق"
    }

    val dismissActionHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "关闭错误提示"
        AppLanguage.ENGLISH -> "Dismiss error message"
        AppLanguage.ARABIC -> "إغلاق رسالة الخطأ"
    }
    // ==================== AI ====================
    val featureWriteHtml: String get() = when (lang) {
        AppLanguage.CHINESE -> "写入 HTML"
        AppLanguage.ENGLISH -> "Write HTML"
        AppLanguage.ARABIC -> "كتابة HTML"
    }

    val featureEditHtml: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑 HTML"
        AppLanguage.ENGLISH -> "Edit HTML"
        AppLanguage.ARABIC -> "تحرير HTML"
    }

    val featureGetConsoleLogs: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取控制台日志"
        AppLanguage.ENGLISH -> "Get Console Logs"
        AppLanguage.ARABIC -> "الحصول على سجلات وحدة التحكم"
    }

    val featureCheckSyntax: String get() = when (lang) {
        AppLanguage.CHINESE -> "语法检查"
        AppLanguage.ENGLISH -> "Check Syntax"
        AppLanguage.ARABIC -> "فحص بناء الجملة"
    }

    val featureAutoFix: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动修复"
        AppLanguage.ENGLISH -> "Auto Fix"
        AppLanguage.ARABIC -> "إصلاح تلقائي"
    }

    val featureIconGeneration: String get() = when (lang) {
        AppLanguage.CHINESE -> "图标生成"
        AppLanguage.ENGLISH -> "Icon Generation"
        AppLanguage.ARABIC -> "إنشاء الأيقونات"
    }

    val featureModuleDevelopment: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块开发"
        AppLanguage.ENGLISH -> "Module Development"
        AppLanguage.ARABIC -> "تطوير الوحدات"
    }

    val featureLrcGeneration: String get() = when (lang) {
        AppLanguage.CHINESE -> "歌词生成"
        AppLanguage.ENGLISH -> "LRC Generation"
        AppLanguage.ARABIC -> "إنشاء كلمات الأغاني"
    }

    val featureTranslation: String get() = when (lang) {
        AppLanguage.CHINESE -> "翻译"
        AppLanguage.ENGLISH -> "Translation"
        AppLanguage.ARABIC -> "ترجمة"
    }

    val featureGeneralChat: String get() = when (lang) {
        AppLanguage.CHINESE -> "通用对话"
        AppLanguage.ENGLISH -> "General Chat"
        AppLanguage.ARABIC -> "محادثة عامة"
    }
    // ==================== Agent ====================
    val toolErrUnknown: String get() = when (lang) {
        AppLanguage.CHINESE -> "未知工具"
        AppLanguage.ENGLISH -> "Unknown tool"
        AppLanguage.ARABIC -> "أداة غير معروفة"
    }

    val toolErrExecFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "执行失败"
        AppLanguage.ENGLISH -> "Execution failed"
        AppLanguage.ARABIC -> "فشل التنفيذ"
    }

    val toolErrChainFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "工具执行失败"
        AppLanguage.ENGLISH -> "Tool execution failed"
        AppLanguage.ARABIC -> "فشل تنفيذ الأداة"
    }

    val toolErrSyntaxCheckFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "语法检查执行失败"
        AppLanguage.ENGLISH -> "Syntax check failed"
        AppLanguage.ARABIC -> "فشل فحص القواعد"
    }

    val toolErrAutoFixFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动修复执行失败"
        AppLanguage.ENGLISH -> "Auto-fix failed"
        AppLanguage.ARABIC -> "فشل الإصلاح التلقائي"
    }

    val toolErrMaxFixAttempts: String get() = when (lang) {
        AppLanguage.CHINESE -> "已达到最大自动修复次数 (%s 次)，仍有语法错误"
        AppLanguage.ENGLISH -> "Max auto-fix attempts reached (%s), syntax errors remain"
        AppLanguage.ARABIC -> "تم الوصول إلى الحد الأقصى لمحاولات الإصلاح التلقائي (%s)، لا تزال هناك أخطاء"
    }

    val toolErrMissingCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "缺少 code 参数"
        AppLanguage.ENGLISH -> "Missing code parameter"
        AppLanguage.ARABIC -> "معامل الكود مفقود"
    }

    val toolErrUnsupportedLang: String get() = when (lang) {
        AppLanguage.CHINESE -> "不支持的语言"
        AppLanguage.ENGLISH -> "Unsupported language"
        AppLanguage.ARABIC -> "لغة غير مدعومة"
    }

    val toolErrMissingModuleName: String get() = when (lang) {
        AppLanguage.CHINESE -> "缺少模块名称"
        AppLanguage.ENGLISH -> "Missing module name"
        AppLanguage.ARABIC -> "اسم الوحدة مفقود"
    }

    val toolModuleCreated: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块创建成功"
        AppLanguage.ENGLISH -> "Module created successfully"
        AppLanguage.ARABIC -> "تم إنشاء الوحدة بنجاح"
    }

    val toolModuleCreateFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建失败"
        AppLanguage.ENGLISH -> "Creation failed"
        AppLanguage.ARABIC -> "فشل الإنشاء"
    }
    // ==================== ====================
    val syntaxBraceMismatch: String get() = when (lang) {
        AppLanguage.CHINESE -> "大括号不匹配"
        AppLanguage.ENGLISH -> "Braces mismatch"
        AppLanguage.ARABIC -> "عدم تطابق الأقواس المعقوفة"
    }

    val syntaxBraceMissing: String get() = when (lang) {
        AppLanguage.CHINESE -> "缺少 %s 个 }"
        AppLanguage.ENGLISH -> "Missing %s }"
        AppLanguage.ARABIC -> "مفقود %s }"
    }

    val syntaxBraceExtra: String get() = when (lang) {
        AppLanguage.CHINESE -> "多余 %s 个 }"
        AppLanguage.ENGLISH -> "Extra %s }"
        AppLanguage.ARABIC -> "زائد %s }"
    }

    val syntaxBraceCheckPair: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查所有 { } 是否正确配对"
        AppLanguage.ENGLISH -> "Check all { } are properly paired"
        AppLanguage.ARABIC -> "تحقق من تطابق جميع { }"
    }

    val syntaxParenMismatch: String get() = when (lang) {
        AppLanguage.CHINESE -> "小括号不匹配"
        AppLanguage.ENGLISH -> "Parentheses mismatch"
        AppLanguage.ARABIC -> "عدم تطابق الأقواس"
    }

    val syntaxParenMissing: String get() = when (lang) {
        AppLanguage.CHINESE -> "缺少 %s 个 )"
        AppLanguage.ENGLISH -> "Missing %s )"
        AppLanguage.ARABIC -> "مفقود %s )"
    }

    val syntaxParenExtra: String get() = when (lang) {
        AppLanguage.CHINESE -> "多余 %s 个 )"
        AppLanguage.ENGLISH -> "Extra %s )"
        AppLanguage.ARABIC -> "زائد %s )"
    }

    val syntaxParenCheckPair: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查所有 ( ) 是否正确配对"
        AppLanguage.ENGLISH -> "Check all ( ) are properly paired"
        AppLanguage.ARABIC -> "تحقق من تطابق جميع ( )"
    }

    val syntaxBracketMismatch: String get() = when (lang) {
        AppLanguage.CHINESE -> "方括号不匹配"
        AppLanguage.ENGLISH -> "Brackets mismatch"
        AppLanguage.ARABIC -> "عدم تطابق الأقواس المربعة"
    }

    val syntaxBracketMissing: String get() = when (lang) {
        AppLanguage.CHINESE -> "缺少 %s 个 ]"
        AppLanguage.ENGLISH -> "Missing %s ]"
        AppLanguage.ARABIC -> "مفقود %s ]"
    }

    val syntaxBracketExtra: String get() = when (lang) {
        AppLanguage.CHINESE -> "多余 %s 个 ]"
        AppLanguage.ENGLISH -> "Extra %s ]"
        AppLanguage.ARABIC -> "زائد %s ]"
    }

    val syntaxBracketCheckPair: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查所有 [ ] 是否正确配对"
        AppLanguage.ENGLISH -> "Check all [ ] are properly paired"
        AppLanguage.ARABIC -> "تحقق من تطابق جميع [ ]"
    }
    // ==================== Lint ====================
    val lintNoVar: String get() = when (lang) {
        AppLanguage.CHINESE -> "建议使用 let 或 const 代替 var"
        AppLanguage.ENGLISH -> "Prefer let or const instead of var"
        AppLanguage.ARABIC -> "يُفضل استخدام let أو const بدلاً من var"
    }

    val lintEqeqeq: String get() = when (lang) {
        AppLanguage.CHINESE -> "建议使用 === 代替 =="
        AppLanguage.ENGLISH -> "Prefer === instead of =="
        AppLanguage.ARABIC -> "يُفضل استخدام === بدلاً من =="
    }

    val lintNoEval: String get() = when (lang) {
        AppLanguage.CHINESE -> "避免使用 eval()，存在安全风险"
        AppLanguage.ENGLISH -> "Avoid eval(), security risk"
        AppLanguage.ARABIC -> "تجنب eval()، مخاطر أمنية"
    }

    val lintNoDocWrite: String get() = when (lang) {
        AppLanguage.CHINESE -> "避免使用 document.write()，可能导致页面问题"
        AppLanguage.ENGLISH -> "Avoid document.write(), may cause page issues"
        AppLanguage.ARABIC -> "تجنب document.write()، قد يسبب مشاكل في الصفحة"
    }

    val lintNoConsole: String get() = when (lang) {
        AppLanguage.CHINESE -> "生产代码中建议移除 console.log"
        AppLanguage.ENGLISH -> "Consider removing console.log in production"
        AppLanguage.ARABIC -> "يُنصح بإزالة console.log في الإنتاج"
    }

    val lintCssMissingSemicolon: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS 属性可能缺少分号"
        AppLanguage.ENGLISH -> "CSS property may be missing semicolon"
        AppLanguage.ARABIC -> "قد تكون خاصية CSS تفتقر إلى فاصلة منقوطة"
    }

    val lintCssNoImportant: String get() = when (lang) {
        AppLanguage.CHINESE -> "尽量避免过度使用 !important"
        AppLanguage.ENGLISH -> "Avoid excessive use of !important"
        AppLanguage.ARABIC -> "تجنب الاستخدام المفرط لـ !important"
    }

    val lintUseStrict: String get() = when (lang) {
        AppLanguage.CHINESE -> "建议在代码开头添加 'use strict' 启用严格模式"
        AppLanguage.ENGLISH -> "Consider adding 'use strict' at the beginning"
        AppLanguage.ARABIC -> "يُنصح بإضافة 'use strict' في البداية"
    }

    val lintUseArrowFn: String get() = when (lang) {
        AppLanguage.CHINESE -> "考虑使用箭头函数简化代码"
        AppLanguage.ENGLISH -> "Consider using arrow functions to simplify code"
        AppLanguage.ARABIC -> "فكر في استخدام الدوال السهمية لتبسيط الكود"
    }

    val lintLineTooLong: String get() = when (lang) {
        AppLanguage.CHINESE -> "部分行超过 120 字符，建议拆分以提高可读性"
        AppLanguage.ENGLISH -> "Some lines exceed 120 characters, consider splitting for readability"
        AppLanguage.ARABIC -> "بعض الأسطر تتجاوز 120 حرفاً، يُنصح بتقسيمها"
    }
    // ==================== AI ====================
    val ruleUseChinese: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用中文进行对话"
        AppLanguage.ENGLISH -> "Communicate in English"
        AppLanguage.ARABIC -> "التواصل بالعربية"
    }

    val ruleChineseComments: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码注释使用中文"
        AppLanguage.ENGLISH -> "Use English for code comments"
        AppLanguage.ARABIC -> "استخدم العربية للتعليقات"
    }

    val ruleGameFlow: String get() = when (lang) {
        AppLanguage.CHINESE -> "游戏要有完整的开始、进行、结束流程"
        AppLanguage.ENGLISH -> "Game must have complete start, play, and end flow"
        AppLanguage.ARABIC -> "يجب أن تحتوي اللعبة على تدفق كامل للبداية واللعب والنهاية"
    }

    val ruleScoreAndInstructions: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加分数显示和游戏说明"
        AppLanguage.ENGLISH -> "Add score display and game instructions"
        AppLanguage.ARABIC -> "أضف عرض النقاط وتعليمات اللعبة"
    }

    val ruleTouchControl: String get() = when (lang) {
        AppLanguage.CHINESE -> "确保触摸控制流畅"
        AppLanguage.ENGLISH -> "Ensure smooth touch controls"
        AppLanguage.ARABIC -> "تأكد من سلاسة التحكم باللمس"
    }

    val ruleCssAnimation: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加流畅的 CSS 动画"
        AppLanguage.ENGLISH -> "Add smooth CSS animations"
        AppLanguage.ARABIC -> "أضف رسوم CSS متحركة سلسة"
    }

    val ruleTransition: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 transition 优化交互反馈"
        AppLanguage.ENGLISH -> "Use transitions for interaction feedback"
        AppLanguage.ARABIC -> "استخدم الانتقالات للتفاعل"
    }

    val rulePerformance: String get() = when (lang) {
        AppLanguage.CHINESE -> "考虑性能，避免过度动画"
        AppLanguage.ENGLISH -> "Consider performance, avoid excessive animations"
        AppLanguage.ARABIC -> "راعي الأداء، تجنب الرسوم المتحركة المفرطة"
    }

    val ruleFormValidation: String get() = when (lang) {
        AppLanguage.CHINESE -> "表单要有完整的验证逻辑"
        AppLanguage.ENGLISH -> "Form must have complete validation logic"
        AppLanguage.ARABIC -> "يجب أن يحتوي النموذج على منطق تحقق كامل"
    }

    val ruleInputLabels: String get() = when (lang) {
        AppLanguage.CHINESE -> "输入框要有清晰的标签和提示"
        AppLanguage.ENGLISH -> "Input fields must have clear labels and hints"
        AppLanguage.ARABIC -> "يجب أن تحتوي حقول الإدخال على تسميات وتلميحات واضحة"
    }

    val ruleSubmitLoading: String get() = when (lang) {
        AppLanguage.CHINESE -> "提交按钮要有加载状态"
        AppLanguage.ENGLISH -> "Submit button must have loading state"
        AppLanguage.ARABIC -> "يجب أن يحتوي زر الإرسال على حالة تحميل"
    }
}
