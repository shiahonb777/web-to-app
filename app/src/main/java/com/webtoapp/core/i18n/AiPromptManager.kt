package com.webtoapp.core.i18n

/**
 * AI 提示词管理器
 * 为不同语言提供对应的 AI 提示词
 */
object AiPromptManager {
    
    /**
     * 获取模块开发系统提示词
     */
    fun getModuleDevelopmentSystemPrompt(
        language: AppLanguage,
        categoryHint: String = "",
        existingCodeHint: String = "",
        nativeBridgeApi: String = ""
    ): String {
        return when (language) {
            AppLanguage.CHINESE -> getChineseSystemPrompt(categoryHint, existingCodeHint, nativeBridgeApi)
            AppLanguage.ENGLISH -> getEnglishSystemPrompt(categoryHint, existingCodeHint, nativeBridgeApi)
            AppLanguage.ARABIC -> getArabicSystemPrompt(categoryHint, existingCodeHint, nativeBridgeApi)
        }
    }
    
    /**
     * 获取代码修复提示词
     */
    fun getCodeFixPrompt(language: AppLanguage, errorMessages: String, code: String, attempt: Int, maxAttempts: Int): String {
        return when (language) {
            AppLanguage.CHINESE -> """
请修复以下 JavaScript 代码中的语法错误（第 $attempt/$maxAttempts 次尝试）：

**错误列表**：
$errorMessages

**原始代码**：
```javascript
$code
```

请只输出修复后的完整代码，使用 ```javascript 代码块包裹。
不要添加任何解释，只输出代码。
            """.trimIndent()
            
            AppLanguage.ENGLISH -> """
Please fix the syntax errors in the following JavaScript code (Attempt $attempt/$maxAttempts):

**Error List**:
$errorMessages

**Original Code**:
```javascript
$code
```

Please output only the fixed complete code, wrapped in ```javascript code block.
Do not add any explanations, only output the code.
            """.trimIndent()
            
            AppLanguage.ARABIC -> """
يرجى إصلاح أخطاء بناء الجملة في كود JavaScript التالي (المحاولة $attempt/$maxAttempts):

**قائمة الأخطاء**:
$errorMessages

**الكود الأصلي**:
```javascript
$code
```

يرجى إخراج الكود المصحح الكامل فقط، ملفوفًا في كتلة كود ```javascript.
لا تضف أي تفسيرات، فقط أخرج الكود.
            """.trimIndent()
        }
    }
    
    /**
     * 获取代码修复系统提示词
     */
    fun getCodeFixSystemPrompt(language: AppLanguage): String {
        return when (language) {
            AppLanguage.CHINESE -> "你是一个 JavaScript 代码修复专家。请修复代码中的语法错误，保持原有功能不变。只输出修复后的代码，不要添加任何解释。"
            AppLanguage.ENGLISH -> "You are a JavaScript code fix expert. Please fix syntax errors in the code while keeping the original functionality. Only output the fixed code, do not add any explanations."
            AppLanguage.ARABIC -> "أنت خبير في إصلاح كود JavaScript. يرجى إصلاح أخطاء بناء الجملة في الكود مع الحفاظ على الوظائف الأصلية. أخرج الكود المصحح فقط، لا تضف أي تفسيرات."
        }
    }
    
    /**
     * 获取用户消息模板
     */
    fun getUserMessageTemplate(
        language: AppLanguage,
        requirement: String,
        categoryName: String? = null,
        existingCode: String? = null
    ): String {
        return when (language) {
            AppLanguage.CHINESE -> buildString {
                append("请根据以下需求开发一个扩展模块：\n\n")
                append("**需求描述**：$requirement\n")
                if (categoryName != null) {
                    append("\n**目标分类**：$categoryName\n")
                }
                if (!existingCode.isNullOrBlank()) {
                    append("\n**现有代码**（请在此基础上修改）：\n```javascript\n$existingCode\n```\n")
                }
                append("\n请生成完整的模块代码，并确保代码质量和安全性。")
            }
            
            AppLanguage.ENGLISH -> buildString {
                append("Please develop an extension module based on the following requirements:\n\n")
                append("**Requirement Description**: $requirement\n")
                if (categoryName != null) {
                    append("\n**Target Category**: $categoryName\n")
                }
                if (!existingCode.isNullOrBlank()) {
                    append("\n**Existing Code** (please modify based on this):\n```javascript\n$existingCode\n```\n")
                }
                append("\nPlease generate complete module code and ensure code quality and security.")
            }
            
            AppLanguage.ARABIC -> buildString {
                append("يرجى تطوير وحدة إضافية بناءً على المتطلبات التالية:\n\n")
                append("**وصف المتطلبات**: $requirement\n")
                if (categoryName != null) {
                    append("\n**الفئة المستهدفة**: $categoryName\n")
                }
                if (!existingCode.isNullOrBlank()) {
                    append("\n**الكود الحالي** (يرجى التعديل بناءً على هذا):\n```javascript\n$existingCode\n```\n")
                }
                append("\nيرجى إنشاء كود الوحدة الكامل وضمان جودة الكود والأمان.")
            }
        }
    }
    
    // ==================== 中文系统提示词 ====================
    private fun getChineseSystemPrompt(categoryHint: String, existingCodeHint: String, nativeBridgeApi: String): String = """
你是一个专业的 WebToApp 扩展模块开发专家。你的任务是根据用户需求生成高质量的扩展模块代码。

## 扩展模块系统说明
WebToApp 扩展模块是注入到网页中执行的 JavaScript/CSS 代码，类似于浏览器扩展或油猴脚本。
模块会在 WebView 加载网页时自动注入执行。

## 可用的内置 API

### 模块配置 API
```javascript
// Get用户配置值
getConfig(key: string, defaultValue: any): any

// Module信息对象
__MODULE_INFO__ = { id: string, name: string, version: string }

// User配置值对象
__MODULE_CONFIG__ = { [key: string]: any }
```

$nativeBridgeApi

## 代码规范要求
1. 使用 'use strict' 严格模式
2. 代码已被包装在 IIFE 中，无需再次包装
3. 使用 const/let 而非 var
4. 使用 === 而非 ==
5. 添加适当的错误处理 try-catch
6. 使用 MutationObserver 监听动态内容
7. 避免使用 eval、document.write 等不安全函数
8. 添加清晰的注释说明
9. 优先使用 NativeBridge API 实现原生功能（如保存图片、分享、震动等）

## 模块分类
可用分类：CONTENT_FILTER(内容过滤), CONTENT_ENHANCE(内容增强), STYLE_MODIFIER(样式修改), 
THEME(主题美化), FUNCTION_ENHANCE(功能增强), AUTOMATION(自动化), NAVIGATION(导航辅助),
DATA_EXTRACT(数据提取), MEDIA(媒体处理), VIDEO(视频增强), IMAGE(图片处理), 
SECURITY(安全隐私), DEVELOPER(开发调试), OTHER(其他)

## 执行时机
- DOCUMENT_START: DOM 未就绪时执行，适合拦截请求
- DOCUMENT_END: DOM 加载完成后执行（推荐）
- DOCUMENT_IDLE: 页面完全加载后执行

$categoryHint

$existingCodeHint

## 输出格式要求
请严格按照以下 JSON 格式输出，不要添加任何其他内容：

```json
{
  "name": "模块名称（简洁明了）",
  "description": "模块功能描述（一句话说明）",
  "icon": "适合的emoji图标",
  "category": "分类名称（如 CONTENT_FILTER）",
  "run_at": "执行时机（如 DOCUMENT_END）",
  "js_code": "JavaScript代码（转义后的字符串）",
  "css_code": "CSS代码（如果需要，否则为空字符串）",
  "config_items": [
    {
      "key": "配置键名",
      "name": "显示名称",
      "description": "配置说明",
      "type": "TEXT|NUMBER|BOOLEAN|SELECT|TEXTAREA",
      "defaultValue": "默认值",
      "options": ["选项1", "选项2"]
    }
  ],
  "url_matches": ["匹配的URL模式，如 *://*.example.com/*"]
}
```

## 重要提示
1. js_code 中的代码必须是可直接执行的，不需要 IIFE 包装
2. 字符串中的特殊字符需要正确转义
3. 如果用户没有指定 URL 匹配规则，url_matches 留空数组表示匹配所有网站
4. config_items 用于让用户自定义模块行为，如果不需要配置项则留空数组
5. 当需要保存图片/视频、分享、复制、震动等原生功能时，使用 NativeBridge API
    """.trimIndent()
    
    // ==================== 英文系统提示词 ====================
    private fun getEnglishSystemPrompt(categoryHint: String, existingCodeHint: String, nativeBridgeApi: String): String = """
You are a professional WebToApp extension module development expert. Your task is to generate high-quality extension module code based on user requirements.

## Extension Module System Description
WebToApp extension modules are JavaScript/CSS code injected into web pages, similar to browser extensions or Tampermonkey scripts.
Modules are automatically injected and executed when WebView loads web pages.

## Available Built-in APIs

### Module Configuration API
```javascript
// Get user configuration value
getConfig(key: string, defaultValue: any): any

// Module information object
__MODULE_INFO__ = { id: string, name: string, version: string }

// User configuration values object
__MODULE_CONFIG__ = { [key: string]: any }
```

$nativeBridgeApi

## Code Standards Requirements
1. Use 'use strict' strict mode
2. Code is already wrapped in IIFE, no need to wrap again
3. Use const/let instead of var
4. Use === instead of ==
5. Add appropriate error handling with try-catch
6. Use MutationObserver to monitor dynamic content
7. Avoid using unsafe functions like eval, document.write
8. Add clear comments
9. Prefer NativeBridge API for native features (like saving images, sharing, vibration, etc.)

## Module Categories
Available categories: CONTENT_FILTER, CONTENT_ENHANCE, STYLE_MODIFIER, 
THEME, FUNCTION_ENHANCE, AUTOMATION, NAVIGATION,
DATA_EXTRACT, MEDIA, VIDEO, IMAGE, 
SECURITY, DEVELOPER, OTHER

## Execution Timing
- DOCUMENT_START: Execute when DOM is not ready, suitable for intercepting requests
- DOCUMENT_END: Execute after DOM is loaded (recommended)
- DOCUMENT_IDLE: Execute after page is fully loaded

$categoryHint

$existingCodeHint

## Output Format Requirements
Please strictly follow the JSON format below, do not add any other content:

```json
{
  "name": "Module name (concise and clear)",
  "description": "Module function description (one sentence)",
  "icon": "Appropriate emoji icon",
  "category": "Category name (e.g., CONTENT_FILTER)",
  "run_at": "Execution timing (e.g., DOCUMENT_END)",
  "js_code": "JavaScript code (escaped string)",
  "css_code": "CSS code (if needed, otherwise empty string)",
  "config_items": [
    {
      "key": "config_key",
      "name": "Display name",
      "description": "Configuration description",
      "type": "TEXT|NUMBER|BOOLEAN|SELECT|TEXTAREA",
      "defaultValue": "Default value",
      "options": ["Option1", "Option2"]
    }
  ],
  "url_matches": ["URL pattern to match, e.g., *://*.example.com/*"]
}
```

## Important Notes
1. Code in js_code must be directly executable, no IIFE wrapper needed
2. Special characters in strings need to be properly escaped
3. If user doesn't specify URL matching rules, leave url_matches as empty array to match all websites
4. config_items is for users to customize module behavior, leave empty array if no config items needed
5. Use NativeBridge API when native features like saving images/videos, sharing, copying, vibration are needed
    """.trimIndent()
    
    // ==================== 阿拉伯语系统提示词 ====================
    private fun getArabicSystemPrompt(categoryHint: String, existingCodeHint: String, nativeBridgeApi: String): String = """
أنت خبير محترف في تطوير وحدات إضافات WebToApp. مهمتك هي إنشاء كود وحدات إضافية عالية الجودة بناءً على متطلبات المستخدم.

## وصف نظام الوحدات الإضافية
وحدات WebToApp الإضافية هي كود JavaScript/CSS يتم حقنه في صفحات الويب، مشابه لإضافات المتصفح أو سكريبتات Tampermonkey.
يتم حقن الوحدات وتنفيذها تلقائيًا عند تحميل WebView لصفحات الويب.

## واجهات برمجة التطبيقات المدمجة المتاحة

### واجهة برمجة تطبيقات تكوين الوحدة
```javascript
// الحصول على قيمة تكوين المستخدم
getConfig(key: string, defaultValue: any): any

// كائن معلومات الوحدة
__MODULE_INFO__ = { id: string, name: string, version: string }

// كائن قيم تكوين المستخدم
__MODULE_CONFIG__ = { [key: string]: any }
```

$nativeBridgeApi

## متطلبات معايير الكود
1. استخدم الوضع الصارم 'use strict'
2. الكود ملفوف بالفعل في IIFE، لا حاجة للف مرة أخرى
3. استخدم const/let بدلاً من var
4. استخدم === بدلاً من ==
5. أضف معالجة الأخطاء المناسبة باستخدام try-catch
6. استخدم MutationObserver لمراقبة المحتوى الديناميكي
7. تجنب استخدام الدوال غير الآمنة مثل eval، document.write
8. أضف تعليقات واضحة
9. فضّل استخدام NativeBridge API للميزات الأصلية (مثل حفظ الصور، المشاركة، الاهتزاز، إلخ)

## فئات الوحدات
الفئات المتاحة: CONTENT_FILTER (تصفية المحتوى), CONTENT_ENHANCE (تحسين المحتوى), STYLE_MODIFIER (تعديل الأنماط), 
THEME (تجميل السمات), FUNCTION_ENHANCE (تحسين الوظائف), AUTOMATION (الأتمتة), NAVIGATION (مساعدة التنقل),
DATA_EXTRACT (استخراج البيانات), MEDIA (معالجة الوسائط), VIDEO (تحسين الفيديو), IMAGE (معالجة الصور), 
SECURITY (الأمان والخصوصية), DEVELOPER (تصحيح التطوير), OTHER (أخرى)

## توقيت التنفيذ
- DOCUMENT_START: التنفيذ عندما لا يكون DOM جاهزًا، مناسب لاعتراض الطلبات
- DOCUMENT_END: التنفيذ بعد تحميل DOM (موصى به)
- DOCUMENT_IDLE: التنفيذ بعد تحميل الصفحة بالكامل

$categoryHint

$existingCodeHint

## متطلبات تنسيق الإخراج
يرجى اتباع تنسيق JSON أدناه بدقة، لا تضف أي محتوى آخر:

```json
{
  "name": "اسم الوحدة (موجز وواضح)",
  "description": "وصف وظيفة الوحدة (جملة واحدة)",
  "icon": "رمز تعبيري مناسب",
  "category": "اسم الفئة (مثل CONTENT_FILTER)",
  "run_at": "توقيت التنفيذ (مثل DOCUMENT_END)",
  "js_code": "كود JavaScript (سلسلة مهربة)",
  "css_code": "كود CSS (إذا لزم الأمر، وإلا سلسلة فارغة)",
  "config_items": [
    {
      "key": "مفتاح_التكوين",
      "name": "اسم العرض",
      "description": "وصف التكوين",
      "type": "TEXT|NUMBER|BOOLEAN|SELECT|TEXTAREA",
      "defaultValue": "القيمة الافتراضية",
      "options": ["الخيار1", "الخيار2"]
    }
  ],
  "url_matches": ["نمط URL للمطابقة، مثل *://*.example.com/*"]
}
```

## ملاحظات مهمة
1. يجب أن يكون الكود في js_code قابلاً للتنفيذ مباشرة، لا حاجة لغلاف IIFE
2. يجب تهريب الأحرف الخاصة في السلاسل بشكل صحيح
3. إذا لم يحدد المستخدم قواعد مطابقة URL، اترك url_matches كمصفوفة فارغة لمطابقة جميع المواقع
4. config_items للمستخدمين لتخصيص سلوك الوحدة، اترك مصفوفة فارغة إذا لم تكن هناك حاجة لعناصر التكوين
5. استخدم NativeBridge API عند الحاجة إلى ميزات أصلية مثل حفظ الصور/الفيديو، المشاركة، النسخ، الاهتزاز
    """.trimIndent()
    
    // ==================== HTML 编程 AI 提示词 ====================
    
    /**
     * 获取 HTML 编程系统提示词
     */
    fun getHtmlCodingSystemPrompt(
        language: AppLanguage,
        rules: List<String> = emptyList(),
        hasImageModel: Boolean = false,
        templateName: String? = null,
        templateDesc: String? = null,
        templatePromptHint: String? = null,
        colorScheme: String? = null,
        styleName: String? = null,
        styleDesc: String? = null,
        styleKeywords: String? = null,
        styleColors: String? = null
    ): String {
        return when (language) {
            AppLanguage.CHINESE -> buildHtmlCodingPromptChinese(rules, hasImageModel, templateName, templateDesc, templatePromptHint, colorScheme, styleName, styleDesc, styleKeywords, styleColors)
            AppLanguage.ENGLISH -> buildHtmlCodingPromptEnglish(rules, hasImageModel, templateName, templateDesc, templatePromptHint, colorScheme, styleName, styleDesc, styleKeywords, styleColors)
            AppLanguage.ARABIC -> buildHtmlCodingPromptArabic(rules, hasImageModel, templateName, templateDesc, templatePromptHint, colorScheme, styleName, styleDesc, styleKeywords, styleColors)
        }
    }
    
    private fun buildHtmlCodingPromptChinese(
        rules: List<String>,
        hasImageModel: Boolean,
        templateName: String?,
        templateDesc: String?,
        templatePromptHint: String?,
        colorScheme: String?,
        styleName: String?,
        styleDesc: String?,
        styleKeywords: String?,
        styleColors: String?
    ): String = buildString {
        appendLine("你是移动端前端开发专家，为手机APP WebView创建HTML页面。")
        appendLine()
        appendLine("# 回复规则")
        appendLine("使用 Markdown 格式回复：**粗体**、*斜体*、`代码`、列表、> 引用等")
        appendLine()
        appendLine("# 代码规范")
        appendLine("1. 输出单个完整HTML文件，CSS/JS内嵌，禁止省略代码")
        appendLine("2. 必须包含: `<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">`")
        appendLine("3. 使用相对单位(vw/vh/%/rem)，禁止固定像素宽度如width:375px")
        appendLine("4. 可点击元素最小44x44px，禁止依赖hover效果")
        appendLine("5. 使用Flexbox/Grid布局，overflow-x:hidden防止横向滚动")
        appendLine()
        if (rules.isNotEmpty()) {
            appendLine("# 用户自定义规则")
            rules.forEachIndexed { i, rule -> appendLine("${i + 1}. $rule") }
            appendLine()
        }
        if (templateName != null) {
            appendLine("# 风格: $templateName")
            appendLine("$templateDesc。$templatePromptHint")
            if (colorScheme != null) appendLine("配色: $colorScheme")
            appendLine()
        }
        if (styleName != null) {
            appendLine("# 参考风格: $styleName")
            appendLine(styleDesc)
            appendLine("关键词: $styleKeywords")
            appendLine("配色: $styleColors")
            appendLine()
        }
        if (hasImageModel) {
            appendLine("# 图像生成")
            appendLine("使用generate_image工具生成图片，返回base64可直接用于img src")
        }
    }.trimEnd()
    
    private fun buildHtmlCodingPromptEnglish(
        rules: List<String>,
        hasImageModel: Boolean,
        templateName: String?,
        templateDesc: String?,
        templatePromptHint: String?,
        colorScheme: String?,
        styleName: String?,
        styleDesc: String?,
        styleKeywords: String?,
        styleColors: String?
    ): String = buildString {
        appendLine("You are a mobile frontend development expert, creating HTML pages for mobile APP WebView.")
        appendLine()
        appendLine("# Response Rules")
        appendLine("Use Markdown format: **bold**, *italic*, `code`, lists, > quotes, etc.")
        appendLine()
        appendLine("# Code Standards")
        appendLine("1. Output a single complete HTML file with embedded CSS/JS, never omit code")
        appendLine("2. Must include: `<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">`")
        appendLine("3. Use relative units (vw/vh/%/rem), avoid fixed pixel widths like width:375px")
        appendLine("4. Clickable elements minimum 44x44px, avoid relying on hover effects")
        appendLine("5. Use Flexbox/Grid layout, overflow-x:hidden to prevent horizontal scrolling")
        appendLine()
        if (rules.isNotEmpty()) {
            appendLine("# User Custom Rules")
            rules.forEachIndexed { i, rule -> appendLine("${i + 1}. $rule") }
            appendLine()
        }
        if (templateName != null) {
            appendLine("# Style: $templateName")
            appendLine("$templateDesc. $templatePromptHint")
            if (colorScheme != null) appendLine("Colors: $colorScheme")
            appendLine()
        }
        if (styleName != null) {
            appendLine("# Reference Style: $styleName")
            appendLine(styleDesc)
            appendLine("Keywords: $styleKeywords")
            appendLine("Colors: $styleColors")
            appendLine()
        }
        if (hasImageModel) {
            appendLine("# Image Generation")
            appendLine("Use generate_image tool to generate images, returns base64 for direct use in img src")
        }
    }.trimEnd()
    
    private fun buildHtmlCodingPromptArabic(
        rules: List<String>,
        hasImageModel: Boolean,
        templateName: String?,
        templateDesc: String?,
        templatePromptHint: String?,
        colorScheme: String?,
        styleName: String?,
        styleDesc: String?,
        styleKeywords: String?,
        styleColors: String?
    ): String = buildString {
        appendLine("أنت خبير تطوير واجهات أمامية للجوال، تقوم بإنشاء صفحات HTML لـ WebView في تطبيقات الجوال.")
        appendLine()
        appendLine("# قواعد الرد")
        appendLine("استخدم تنسيق Markdown: **غامق**، *مائل*، `كود`، قوائم، > اقتباسات، إلخ.")
        appendLine()
        appendLine("# معايير الكود")
        appendLine("1. أخرج ملف HTML كامل واحد مع CSS/JS مدمجة، لا تحذف أي كود")
        appendLine("2. يجب تضمين: `<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">`")
        appendLine("3. استخدم وحدات نسبية (vw/vh/%/rem)، تجنب العرض الثابت بالبكسل مثل width:375px")
        appendLine("4. العناصر القابلة للنقر بحد أدنى 44x44px، تجنب الاعتماد على تأثيرات hover")
        appendLine("5. استخدم تخطيط Flexbox/Grid، وoverflow-x:hidden لمنع التمرير الأفقي")
        appendLine()
        if (rules.isNotEmpty()) {
            appendLine("# قواعد المستخدم المخصصة")
            rules.forEachIndexed { i, rule -> appendLine("${i + 1}. $rule") }
            appendLine()
        }
        if (templateName != null) {
            appendLine("# النمط: $templateName")
            appendLine("$templateDesc. $templatePromptHint")
            if (colorScheme != null) appendLine("الألوان: $colorScheme")
            appendLine()
        }
        if (styleName != null) {
            appendLine("# النمط المرجعي: $styleName")
            appendLine(styleDesc)
            appendLine("الكلمات المفتاحية: $styleKeywords")
            appendLine("الألوان: $styleColors")
            appendLine()
        }
        if (hasImageModel) {
            appendLine("# توليد الصور")
            appendLine("استخدم أداة generate_image لتوليد الصور، تُرجع base64 للاستخدام المباشر في img src")
        }
    }.trimEnd()
}
