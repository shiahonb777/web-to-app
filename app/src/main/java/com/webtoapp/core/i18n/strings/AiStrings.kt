package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object AiStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage

    val aiDevelop: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 开发"
        AppLanguage.ENGLISH -> "AI Develop"
        AppLanguage.ARABIC -> "تطوير AI"
    }

    val aiModuleDeveloper: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 模块开发"
        AppLanguage.ENGLISH -> "AI Module Developer"
        AppLanguage.ARABIC -> "مطور وحدات AI"
    }

    val aiGenerating: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 生成中..."
        AppLanguage.ENGLISH -> "AI Generating..."
        AppLanguage.ARABIC -> "AI يولد..."
    }

    val aiAnalyzing: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在分析需求..."
        AppLanguage.ENGLISH -> "Analyzing requirements..."
        AppLanguage.ARABIC -> "تحليل المتطلبات..."
    }

    val aiCompleted: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成完成"
        AppLanguage.ENGLISH -> "Generation completed"
        AppLanguage.ARABIC -> "اكتمل التوليد"
    }

    val aiPlanning: String get() = when (lang) {
        AppLanguage.CHINESE -> "制定开发计划..."
        AppLanguage.ENGLISH -> "Planning development..."
        AppLanguage.ARABIC -> "تخطيط التطوير..."
    }

    val aiGeneratingCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成代码中..."
        AppLanguage.ENGLISH -> "Generating code..."
        AppLanguage.ARABIC -> "توليد الكود..."
    }

    val aiChecking: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查语法中..."
        AppLanguage.ENGLISH -> "Checking syntax..."
        AppLanguage.ARABIC -> "فحص بناء الجملة..."
    }

    val aiFixing: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动修复错误..."
        AppLanguage.ENGLISH -> "Auto fixing errors..."
        AppLanguage.ARABIC -> "إصلاح الأخطاء تلقائيًا..."
    }

    val aiScanning: String get() = when (lang) {
        AppLanguage.CHINESE -> "安全扫描中..."
        AppLanguage.ENGLISH -> "Security scanning..."
        AppLanguage.ARABIC -> "فحص الأمان..."
    }

    val aiError: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 错误"
        AppLanguage.ENGLISH -> "AI Error"
        AppLanguage.ARABIC -> "خطأ AI"
    }

    val aiGenerateIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 生成图标"
        AppLanguage.ENGLISH -> "AI Generate Icon"
        AppLanguage.ARABIC -> "توليد أيقونة AI"
    }

    val aiNoValidResponse: String get() = when (lang) {
        AppLanguage.CHINESE -> "⚠️ AI 未返回有效内容"
        AppLanguage.ENGLISH -> "⚠️ AI returned no valid content"
        AppLanguage.ARABIC -> "⚠️ لم يُرجع الذكاء الاصطناعي محتوى صالحًا"
    }

    val aiGeneratedProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI生成项目"
        AppLanguage.ENGLISH -> "AI Generated Project"
        AppLanguage.ARABIC -> "مشروع مُنشأ بالذكاء الاصطناعي"
    }

    val aiHelpsGenerateWebpage: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 帮你快速生成精美网页"
        AppLanguage.ENGLISH -> "AI helps you quickly generate beautiful webpages"
        AppLanguage.ARABIC -> "يساعدك الذكاء الاصطناعي على إنشاء صفحات ويب جميلة بسرعة"
    }

    val aiThinking: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 正在思考..."
        AppLanguage.ENGLISH -> "AI is thinking..."
        AppLanguage.ARABIC -> "الذكاء الاصطناعي يفكر..."
    }

    val aiCodeAutoSaved: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI生成的代码会自动保存到这里"
        AppLanguage.ENGLISH -> "AI generated code is automatically saved here"
        AppLanguage.ARABIC -> "يتم حفظ الكود المُنشأ بالذكاء الاصطناعي تلقائيًا هنا"
    }

    val aiCodeSavedHere: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 生成的代码将保存在这里"
        AppLanguage.ENGLISH -> "AI generated code will be saved here"
        AppLanguage.ARABIC -> "سيتم حفظ الكود المُنشأ بواسطة AI هنا"
    }

    val aiModuleDeveloperTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 模块开发"
        AppLanguage.ENGLISH -> "AI Module Developer"
        AppLanguage.ARABIC -> "مطور وحدات AI"
    }

    val aiAssistant: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 模块开发助手"
        AppLanguage.ENGLISH -> "AI Module Development Assistant"
        AppLanguage.ARABIC -> "مساعد تطوير وحدات AI"
    }

    val aiAssistantDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "用自然语言描述你想要的功能\nAI 将自动生成扩展模块代码"
        AppLanguage.ENGLISH -> "Describe the feature you want in natural language\nAI will automatically generate extension module code"
        AppLanguage.ARABIC -> "صف الميزة التي تريدها بلغة طبيعية\nسيقوم AI بإنشاء كود وحدة الامتداد تلقائيًا"
    }

    val aiGenerateLyrics: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI生成歌词"
        AppLanguage.ENGLISH -> "AI Generate Lyrics"
        AppLanguage.ARABIC -> "إنشاء كلمات بالذكاء الاصطناعي"
    }

    val aiModuleDeveloperAgent: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI模块开发Agent：自然语言生成模块"
        AppLanguage.ENGLISH -> "AI module developer agent: natural language module generation"
        AppLanguage.ARABIC -> "وكيل مطور وحدة AI: إنشاء وحدة باللغة الطبيعية"
    }

    val aiIconGeneration: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI图标生成：AI生成应用图标"
        AppLanguage.ENGLISH -> "AI icon generation: AI generates app icons"
        AppLanguage.ARABIC -> "إنشاء أيقونات AI: AI ينشئ أيقونات التطبيق"
    }

    val aiHtmlCodingFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI HTML编程：AI辅助生成代码"
        AppLanguage.ENGLISH -> "AI HTML coding: AI-assisted code generation"
        AppLanguage.ARABIC -> "برمجة AI HTML: إنشاء الكود بمساعدة AI"
    }

    val aiSettingsFeature: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI设置：统一管理API密钥和模型"
        AppLanguage.ENGLISH -> "AI settings: unified API key and model management"
        AppLanguage.ARABIC -> "إعدادات AI: إدارة موحدة لمفاتيح API والنماذج"
    }

    val aiIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI图标"
        AppLanguage.ENGLISH -> "AI Icon"
        AppLanguage.ARABIC -> "أيقونة AI"
    }

    val aiModuleDevelopment: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 模块开发"
        AppLanguage.ENGLISH -> "AI Module Development"
        AppLanguage.ARABIC -> "تطوير وحدة AI"
    }

    val aiCodingDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 辅助生成和修改 HTML/CSS/JS 代码"
        AppLanguage.ENGLISH -> "AI-assisted HTML/CSS/JS code generation and modification"
        AppLanguage.ARABIC -> "إنشاء وتعديل كود HTML/CSS/JS بمساعدة الذكاء الاصطناعي"
    }

    val aiCodingImageDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML 编程中的图像生成功能"
        AppLanguage.ENGLISH -> "Image generation in HTML coding"
        AppLanguage.ARABIC -> "إنشاء الصور في برمجة HTML"
    }

    val aiImageGeneration: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 图像生成"
        AppLanguage.ENGLISH -> "AI Image Generation"
        AppLanguage.ARABIC -> "إنشاء صور بالذكاء الاصطناعي"
    }

    val aiErrorNoApiKey: String get() = when (lang) {
        AppLanguage.CHINESE -> "请先在 AI 设置中配置 API Key"
        AppLanguage.ENGLISH -> "Please configure an API Key in AI settings first"
        AppLanguage.ARABIC -> "يرجى تكوين مفتاح API في إعدادات الذكاء الاصطناعي أولاً"
    }

    val aiErrorNoModel: String get() = when (lang) {
        AppLanguage.CHINESE -> "请先在 AI 设置中添加并保存模型"
        AppLanguage.ENGLISH -> "Please add and save a model in AI settings first"
        AppLanguage.ARABIC -> "يرجى إضافة وحفظ نموذج في إعدادات الذكاء الاصطناعي أولاً"
    }

    val aiErrorNoApiKeyForModel: String get() = when (lang) {
        AppLanguage.CHINESE -> "找不到模型对应的 API Key"
        AppLanguage.ENGLISH -> "Cannot find API Key for the model"
        AppLanguage.ARABIC -> "لا يمكن العثور على مفتاح API للنموذج"
    }

    val aiErrorUnknown: String get() = when (lang) {
        AppLanguage.CHINESE -> "未知错误"
        AppLanguage.ENGLISH -> "Unknown error"
        AppLanguage.ARABIC -> "خطأ غير معروف"
    }

    val aiGeneratedModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 生成模块"
        AppLanguage.ENGLISH -> "AI Generated Module"
        AppLanguage.ARABIC -> "وحدة مولدة بالذكاء الاصطناعي"
    }

    val aiGeneratedModuleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "由 AI 生成的扩展模块"
        AppLanguage.ENGLISH -> "Extension module generated by AI"
        AppLanguage.ARABIC -> "وحدة إضافية تم إنشاؤها بواسطة الذكاء الاصطناعي"
    }

    val aiSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 设置"
        AppLanguage.ENGLISH -> "AI Settings"
        AppLanguage.ARABIC -> "إعدادات AI"
    }

    val aiGenerationServiceRunning: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 生成服务运行中"
        AppLanguage.ENGLISH -> "AI generation service running"
        AppLanguage.ARABIC -> "خدمة توليد الذكاء الاصطناعي قيد التشغيل"
    }

    val aiGenerationService: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 生成服务"
        AppLanguage.ENGLISH -> "AI Generation Service"
        AppLanguage.ARABIC -> "خدمة توليد الذكاء الاصطناعي"
    }

    val aiCodeGenerationNotification: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 代码生成通知"
        AppLanguage.ENGLISH -> "AI code generation notification"
        AppLanguage.ARABIC -> "إشعار إنشاء كود الذكاء الاصطناعي"
    }

    val aiCodingAssistant: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 编程助手"
        AppLanguage.ENGLISH -> "AI Coding Assistant"
        AppLanguage.ARABIC -> "مساعد البرمجة AI"
    }

    val aiCodingWelcome: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 帮你快速生成应用代码"
        AppLanguage.ENGLISH -> "AI helps you quickly generate app code"
        AppLanguage.ARABIC -> "يساعدك AI على إنشاء كود التطبيق بسرعة"
    }

    val aiPromptHtml1: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建一个现代个人主页，包含导航栏、英雄区、作品展示和联系方式"
        AppLanguage.ENGLISH -> "Create a modern personal homepage with navbar, hero section, portfolio and contact info"
        AppLanguage.ARABIC -> "إنشاء صفحة شخصية حديثة تحتوي على شريط تنقل وقسم بطل ومعرض أعمال ومعلومات اتصال"
    }

    val aiPromptHtml2: String get() = when (lang) {
        AppLanguage.CHINESE -> "做一个天气预报应用界面，支持搜索城市、显示当前天气和未来7天预报"
        AppLanguage.ENGLISH -> "Build a weather forecast app UI with city search, current weather and 7-day forecast"
        AppLanguage.ARABIC -> "بناء واجهة تطبيق توقعات الطقس مع بحث المدينة والطقس الحالي وتوقعات 7 أيام"
    }

    val aiPromptFrontend1: String get() = when (lang) {
        AppLanguage.CHINESE -> "用 React 创建一个待办事项应用，支持添加、删除、标记完成和本地存储"
        AppLanguage.ENGLISH -> "Create a React todo app with add, delete, mark complete and local storage support"
        AppLanguage.ARABIC -> "إنشاء تطبيق مهام React مع إضافة وحذف وتعليم إتمام ودعم التخزين المحلي"
    }

    val aiPromptFrontend2: String get() = when (lang) {
        AppLanguage.CHINESE -> "用 Vue.js 构建一个电商产品列表页面，包含筛选、排序和购物车功能"
        AppLanguage.ENGLISH -> "Build a Vue.js e-commerce product listing page with filtering, sorting and cart"
        AppLanguage.ARABIC -> "بناء صفحة منتجات تجارة إلكترونية Vue.js مع تصفية وترتيب وسلة شراء"
    }

    val aiPromptNodejs1: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建一个 Express REST API 服务，包含用户注册登录、JWT认证和CRUD接口"
        AppLanguage.ENGLISH -> "Create an Express REST API with user registration, login, JWT auth and CRUD endpoints"
        AppLanguage.ARABIC -> "إنشاء Express REST API مع تسجيل المستخدم وتسجيل الدخول ومصادقة JWT ونقاط CRUD"
    }

    val aiPromptNodejs2: String get() = when (lang) {
        AppLanguage.CHINESE -> "搭建一个 WebSocket 实时聊天服务器，支持多房间和消息广播"
        AppLanguage.ENGLISH -> "Build a WebSocket real-time chat server with multi-room support and message broadcasting"
        AppLanguage.ARABIC -> "بناء خادم دردشة WebSocket مع دعم غرف متعددة وبث الرسائل"
    }

    val aiPromptWordpress1: String get() = when (lang) {
        AppLanguage.CHINESE -> "开发一个 WordPress 自定义主题，包含首页模板、文章列表和侧边栏小工具"
        AppLanguage.ENGLISH -> "Develop a WordPress custom theme with homepage template, post listing and sidebar widgets"
        AppLanguage.ARABIC -> "تطوير سمة WordPress مخصصة مع قالب صفحة رئيسية وقائمة مقالات وودجات الشريط الجانبي"
    }

    val aiPromptWordpress2: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建一个 WordPress 插件，添加自定义文章类型和管理后台设置页面"
        AppLanguage.ENGLISH -> "Create a WordPress plugin that adds custom post types and an admin settings page"
        AppLanguage.ARABIC -> "إنشاء إضافة WordPress تضيف أنواع مقالات مخصصة وصفحة إعدادات المشرف"
    }

    val aiPromptPhp1: String get() = when (lang) {
        AppLanguage.CHINESE -> "用 Laravel 创建一个博客系统，包含文章管理、分类标签和评论功能"
        AppLanguage.ENGLISH -> "Create a Laravel blog system with post management, categories, tags and comments"
        AppLanguage.ARABIC -> "إنشاء نظام مدونة Laravel مع إدارة المقالات والفئات والعلامات والتعليقات"
    }

    val aiPromptPhp2: String get() = when (lang) {
        AppLanguage.CHINESE -> "搭建一个 PHP 用户管理系统，支持注册、登录、角色权限控制"
        AppLanguage.ENGLISH -> "Build a PHP user management system with registration, login and role-based access control"
        AppLanguage.ARABIC -> "بناء نظام إدارة مستخدمين PHP مع التسجيل وتسجيل الدخول والتحكم بالصلاحيات"
    }

    val aiPromptPython1: String get() = when (lang) {
        AppLanguage.CHINESE -> "用 FastAPI 构建一个 RESTful API 服务，包含数据库模型、自动文档和认证中间件"
        AppLanguage.ENGLISH -> "Build a FastAPI RESTful service with database models, auto-docs and auth middleware"
        AppLanguage.ARABIC -> "بناء خدمة FastAPI RESTful مع نماذج قاعدة بيانات ووثائق تلقائية ووسيط مصادقة"
    }

    val aiPromptPython2: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建一个 Flask Web 应用，实现文件上传、图片处理和下载功能"
        AppLanguage.ENGLISH -> "Create a Flask web app with file upload, image processing and download functionality"
        AppLanguage.ARABIC -> "إنشاء تطبيق Flask مع رفع الملفات ومعالجة الصور ووظيفة التنزيل"
    }

    val aiPromptGo1: String get() = when (lang) {
        AppLanguage.CHINESE -> "用 Gin 框架创建一个 RESTful API 服务，包含路由分组、中间件和数据库操作"
        AppLanguage.ENGLISH -> "Create a Gin framework RESTful API with route groups, middleware and database operations"
        AppLanguage.ARABIC -> "إنشاء Gin RESTful API مع مجموعات المسارات والوسيط وعمليات قاعدة البيانات"
    }

    val aiPromptGo2: String get() = when (lang) {
        AppLanguage.CHINESE -> "搭建一个 Go 微服务，实现 gRPC 接口和 HTTP 网关"
        AppLanguage.ENGLISH -> "Build a Go microservice with gRPC endpoints and HTTP gateway"
        AppLanguage.ARABIC -> "بناء خدمة Go مصغرة مع نقاط gRPC وبوابة HTTP"
    }
    // ==================== HTML Coding Assistant ====================
    val htmlCodingAssistant: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML编程助手"
        AppLanguage.ENGLISH -> "HTML Coding Assistant"
        AppLanguage.ARABIC -> "مساعد برمجة HTML"
    }

    val messagesCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 条消息"
        AppLanguage.ENGLISH -> "%d messages"
        AppLanguage.ARABIC -> "%d رسائل"
    }

    val modelConfigInvalid: String get() = when (lang) {
        AppLanguage.CHINESE -> "模型配置无效"
        AppLanguage.ENGLISH -> "Model configuration invalid"
        AppLanguage.ARABIC -> "تكوين النموذج غير صالح"
    }

    val generatingCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在生成代码..."
        AppLanguage.ENGLISH -> "Generating code..."
        AppLanguage.ARABIC -> "جاري إنشاء الكود..."
    }

    val codeGenerated: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码已生成，请查看下方预览"
        AppLanguage.ENGLISH -> "Code generated, see preview below"
        AppLanguage.ARABIC -> "تم إنشاء الكود، انظر المعاينة أدناه"
    }

    val debugInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "调试信息："
        AppLanguage.ENGLISH -> "Debug info:"
        AppLanguage.ARABIC -> "معلومات التصحيح:"
    }

    val textContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "文本内容"
        AppLanguage.ENGLISH -> "Text content"
        AppLanguage.ARABIC -> "محتوى النص"
    }

    val streamContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "流式内容"
        AppLanguage.ENGLISH -> "Stream content"
        AppLanguage.ARABIC -> "محتوى البث"
    }

    val thinkingContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "思考内容"
        AppLanguage.ENGLISH -> "Thinking content"
        AppLanguage.ARABIC -> "محتوى التفكير"
    }

    val htmlCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML代码"
        AppLanguage.ENGLISH -> "HTML code"
        AppLanguage.ARABIC -> "كود HTML"
    }

    val emptyText: String get() = when (lang) {
        AppLanguage.CHINESE -> "空"
        AppLanguage.ENGLISH -> "empty"
        AppLanguage.ARABIC -> "فارغ"
    }

    val characters: String get() = when (lang) {
        AppLanguage.CHINESE -> "字符"
        AppLanguage.ENGLISH -> "characters"
        AppLanguage.ARABIC -> "أحرف"
    }

    val possibleReasons: String get() = when (lang) {
        AppLanguage.CHINESE -> "可能原因："
        AppLanguage.ENGLISH -> "Possible reasons:"
        AppLanguage.ARABIC -> "الأسباب المحتملة:"
    }

    val apiFormatIncompatible: String get() = when (lang) {
        AppLanguage.CHINESE -> "1. API 返回格式不兼容"
        AppLanguage.ENGLISH -> "1. API response format incompatible"
        AppLanguage.ARABIC -> "1. تنسيق استجابة API غير متوافق"
    }

    val modelNotSupported: String get() = when (lang) {
        AppLanguage.CHINESE -> "2. 模型不支持当前请求"
        AppLanguage.ENGLISH -> "2. Model does not support current request"
        AppLanguage.ARABIC -> "2. النموذج لا يدعم الطلب الحالي"
    }

    val apiKeyQuotaInsufficient: String get() = when (lang) {
        AppLanguage.CHINESE -> "3. API Key 配额不足"
        AppLanguage.ENGLISH -> "3. API Key quota insufficient"
        AppLanguage.ARABIC -> "3. حصة مفتاح API غير كافية"
    }

    val suggestionChangeModel: String get() = when (lang) {
        AppLanguage.CHINESE -> "建议：尝试更换模型或检查 API 设置"
        AppLanguage.ENGLISH -> "Suggestion: Try changing model or check API settings"
        AppLanguage.ARABIC -> "اقتراح: جرب تغيير النموذج أو تحقق من إعدادات API"
    }

    val conversationCheckpoint: String get() = when (lang) {
        AppLanguage.CHINESE -> "对话 #%d"
        AppLanguage.ENGLISH -> "Conversation #%d"
        AppLanguage.ARABIC -> "المحادثة #%d"
    }

    val preview: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览"
        AppLanguage.ENGLISH -> "Preview"
        AppLanguage.ARABIC -> "معاينة"
    }

    val savedToPath: String get() = when (lang) {
        AppLanguage.CHINESE -> "已保存到: %s"
        AppLanguage.ENGLISH -> "Saved to: %s"
        AppLanguage.ARABIC -> "تم الحفظ في: %s"
    }

    val noCodeToExport: String get() = when (lang) {
        AppLanguage.CHINESE -> "没有可导出的代码"
        AppLanguage.ENGLISH -> "No code to export"
        AppLanguage.ARABIC -> "لا يوجد كود للتصدير"
    }

    val exportedToHtmlProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "已导出到HTML项目"
        AppLanguage.ENGLISH -> "Exported to HTML project"
        AppLanguage.ARABIC -> "تم التصدير إلى مشروع HTML"
    }

    val exportFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Export failed"
        AppLanguage.ENGLISH -> "Export failed"
        AppLanguage.ARABIC -> "فشل التصدير"
    }

    val codeLibrary: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码库"
        AppLanguage.ENGLISH -> "Code Library"
        AppLanguage.ARABIC -> "مكتبة الكود"
    }

    val rollback: String get() = when (lang) {
        AppLanguage.CHINESE -> "回退"
        AppLanguage.ENGLISH -> "Rollback"
        AppLanguage.ARABIC -> "التراجع"
    }

    val templates: String get() = when (lang) {
        AppLanguage.CHINESE -> "模板"
        AppLanguage.ENGLISH -> "Templates"
        AppLanguage.ARABIC -> "القوالب"
    }

    val sessionList: String get() = when (lang) {
        AppLanguage.CHINESE -> "会话列表"
        AppLanguage.ENGLISH -> "Session List"
        AppLanguage.ARABIC -> "قائمة الجلسات"
    }

    val startNewConversation: String get() = when (lang) {
        AppLanguage.CHINESE -> "开始新对话"
        AppLanguage.ENGLISH -> "Start New Conversation"
        AppLanguage.ARABIC -> "بدء محادثة جديدة"
    }

    val tutorial: String get() = when (lang) {
        AppLanguage.CHINESE -> "教程"
        AppLanguage.ENGLISH -> "Tutorial"
        AppLanguage.ARABIC -> "الدليل التعليمي"
    }

    val quickStart: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速开始"
        AppLanguage.ENGLISH -> "Quick Start"
        AppLanguage.ARABIC -> "البدء السريع"
    }

    val generatingImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在生成图像..."
        AppLanguage.ENGLISH -> "Generating image..."
        AppLanguage.ARABIC -> "جاري إنشاء الصورة..."
    }

    val conversationHistory: String get() = when (lang) {
        AppLanguage.CHINESE -> "对话历史"
        AppLanguage.ENGLISH -> "Conversation History"
        AppLanguage.ARABIC -> "سجل المحادثات"
    }

    val newConversation: String get() = when (lang) {
        AppLanguage.CHINESE -> "新建对话"
        AppLanguage.ENGLISH -> "New Conversation"
        AppLanguage.ARABIC -> "محادثة جديدة"
    }

    val noConversationRecords: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无对话记录"
        AppLanguage.ENGLISH -> "No conversation records"
        AppLanguage.ARABIC -> "لا توجد سجلات محادثات"
    }

    val selectStyleTemplate: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择风格模板"
        AppLanguage.ENGLISH -> "Select Style Template"
        AppLanguage.ARABIC -> "اختيار قالب النمط"
    }

    val selected: String get() = when (lang) {
        AppLanguage.CHINESE -> "已选择"
        AppLanguage.ENGLISH -> "Selected"
        AppLanguage.ARABIC -> "محدد"
    }

    val selectTemplateHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择一个风格模板，AI将根据该风格生成代码"
        AppLanguage.ENGLISH -> "Select a style template, AI will generate code based on this style"
        AppLanguage.ARABIC -> "اختر قالب نمط، سيقوم الذكاء الاصطناعي بإنشاء الكود بناءً على هذا النمط"
    }

    val designTemplates: String get() = when (lang) {
        AppLanguage.CHINESE -> "设计模板"
        AppLanguage.ENGLISH -> "Design Templates"
        AppLanguage.ARABIC -> "قوالب التصميم"
    }

    val styleReferences: String get() = when (lang) {
        AppLanguage.CHINESE -> "风格参考"
        AppLanguage.ENGLISH -> "Style References"
        AppLanguage.ARABIC -> "مراجع النمط"
    }

    val totalTemplates: String get() = when (lang) {
        AppLanguage.CHINESE -> "共 %d 个模板"
        AppLanguage.ENGLISH -> "%d templates total"
        AppLanguage.ARABIC -> "إجمالي %d قوالب"
    }

    val totalStyleReferences: String get() = when (lang) {
        AppLanguage.CHINESE -> "共 %d 个风格参考"
        AppLanguage.ENGLISH -> "%d style references total"
        AppLanguage.ARABIC -> "إجمالي %d مراجع نمط"
    }

    val usageTutorial: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用教程"
        AppLanguage.ENGLISH -> "Usage Tutorial"
        AppLanguage.ARABIC -> "دليل الاستخدام"
    }

    val chapters: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 章节"
        AppLanguage.ENGLISH -> "%d chapters"
        AppLanguage.ARABIC -> "%d فصول"
    }

    val noTutorialContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无教程内容"
        AppLanguage.ENGLISH -> "No tutorial content"
        AppLanguage.ARABIC -> "لا يوجد محتوى تعليمي"
    }

    val sections: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个小节"
        AppLanguage.ENGLISH -> "%d sections"
        AppLanguage.ARABIC -> "%d أقسام"
    }

    val codeExample: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码示例"
        AppLanguage.ENGLISH -> "Code Example"
        AppLanguage.ARABIC -> "مثال الكود"
    }

    val tips: String get() = when (lang) {
        AppLanguage.CHINESE -> "小贴士"
        AppLanguage.ENGLISH -> "Tips"
        AppLanguage.ARABIC -> "نصائح"
    }

    val versionManagement: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本管理"
        AppLanguage.ENGLISH -> "Version Management"
        AppLanguage.ARABIC -> "إدارة الإصدارات"
    }

    val saveVersion: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存版本"
        AppLanguage.ENGLISH -> "Save Version"
        AppLanguage.ARABIC -> "حفظ الإصدار"
    }

    val export: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出"
        AppLanguage.ENGLISH -> "Export"
        AppLanguage.ARABIC -> "تصدير"
    }

    val exportModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出模块"
        AppLanguage.ENGLISH -> "Export Module"
        AppLanguage.ARABIC -> "تصدير الوحدة"
    }

    val exportToDownloads: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存到 Downloads"
        AppLanguage.ENGLISH -> "Save to Downloads"
        AppLanguage.ARABIC -> "حفظ في Downloads"
    }

    val exportToDownloadsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存到默认的下载文件夹"
        AppLanguage.ENGLISH -> "Save to default Downloads folder"
        AppLanguage.ARABIC -> "حفظ في مجلد التنزيلات الافتراضي"
    }

    val exportToCustomPath: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义存储路径"
        AppLanguage.ENGLISH -> "Custom Storage Path"
        AppLanguage.ARABIC -> "مسار تخزين مخصص"
    }

    val exportToCustomPathHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择存储位置"
        AppLanguage.ENGLISH -> "Choose storage location"
        AppLanguage.ARABIC -> "اختر موقع التخزين"
    }

    val exportSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "Export successful"
        AppLanguage.ENGLISH -> "Export successful"
        AppLanguage.ARABIC -> "تم التصدير بنجاح"
    }

    val noSavedVersions: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无保存的版本\n对话中自动创建检查点，或手动保存版本"
        AppLanguage.ENGLISH -> "No saved versions\nCheckpoints are created automatically during conversation, or save manually"
        AppLanguage.ARABIC -> "لا توجد إصدارات محفوظة\nيتم إنشاء نقاط التحقق تلقائيًا أثناء المحادثة، أو احفظ يدويًا"
    }

    val manualSave: String get() = when (lang) {
        AppLanguage.CHINESE -> "手动保存 %d"
        AppLanguage.ENGLISH -> "Manual Save %d"
        AppLanguage.ARABIC -> "حفظ يدوي %d"
    }

    val editMessage: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑消息"
        AppLanguage.ENGLISH -> "Edit Message"
        AppLanguage.ARABIC -> "تعديل الرسالة"
    }

    val imagesCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 张图片"
        AppLanguage.ENGLISH -> "%d images"
        AppLanguage.ARABIC -> "%d صور"
    }

    val editWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "⚠️ 编辑后，该消息之后的对话将被删除"
        AppLanguage.ENGLISH -> "⚠️ After editing, conversations after this message will be deleted"
        AppLanguage.ARABIC -> "⚠️ بعد التعديل، سيتم حذف المحادثات بعد هذه الرسالة"
    }

    val resend: String get() = when (lang) {
        AppLanguage.CHINESE -> "重新发送"
        AppLanguage.ENGLISH -> "Resend"
        AppLanguage.ARABIC -> "إعادة الإرسال"
    }

    val saveProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存项目"
        AppLanguage.ENGLISH -> "Save Project"
        AppLanguage.ARABIC -> "حفظ المشروع"
    }

    val projectName: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目名称"
        AppLanguage.ENGLISH -> "Project Name"
        AppLanguage.ARABIC -> "اسم المشروع"
    }

    val saveLocation: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存位置"
        AppLanguage.ENGLISH -> "Save Location"
        AppLanguage.ARABIC -> "موقع الحفظ"
    }

    val willSaveFiles: String get() = when (lang) {
        AppLanguage.CHINESE -> "将保存 %d 个文件"
        AppLanguage.ENGLISH -> "Will save %d files"
        AppLanguage.ARABIC -> "سيتم حفظ %d ملفات"
    }

    val save: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存"
        AppLanguage.ENGLISH -> "Save"
        AppLanguage.ARABIC -> "حفظ"
    }

    val favorites: String get() = when (lang) {
        AppLanguage.CHINESE -> "收藏"
        AppLanguage.ENGLISH -> "Favorites"
        AppLanguage.ARABIC -> "المفضلة"
    }

    val noFavorites: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无收藏"
        AppLanguage.ENGLISH -> "No favorites"
        AppLanguage.ARABIC -> "لا توجد مفضلات"
    }

    val codeLibraryEmpty: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码库为空"
        AppLanguage.ENGLISH -> "Code library is empty"
        AppLanguage.ARABIC -> "مكتبة الكود فارغة"
    }

    val use: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用"
        AppLanguage.ENGLISH -> "Use"
        AppLanguage.ARABIC -> "استخدام"
    }

    val unfavorite: String get() = when (lang) {
        AppLanguage.CHINESE -> "取消收藏"
        AppLanguage.ENGLISH -> "Unfavorite"
        AppLanguage.ARABIC -> "إلغاء المفضلة"
    }

    val favorite: String get() = when (lang) {
        AppLanguage.CHINESE -> "收藏"
        AppLanguage.ENGLISH -> "Favorite"
        AppLanguage.ARABIC -> "مفضلة"
    }

    val exportToProjectLibrary: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出到项目库"
        AppLanguage.ENGLISH -> "Export to Project Library"
        AppLanguage.ARABIC -> "تصدير إلى مكتبة المشاريع"
    }

    val delete: String get() = when (lang) {
        AppLanguage.CHINESE -> "Delete"
        AppLanguage.ENGLISH -> "Delete"
        AppLanguage.ARABIC -> "حذف"
    }

    val conversationCheckpoints: String get() = when (lang) {
        AppLanguage.CHINESE -> "对话检查点"
        AppLanguage.ENGLISH -> "Conversation Checkpoints"
        AppLanguage.ARABIC -> "نقاط تحقق المحادثة"
    }

    val rollbackHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "回退到之前的对话状态，同时恢复代码库"
        AppLanguage.ENGLISH -> "Rollback to previous conversation state and restore code library"
        AppLanguage.ARABIC -> "التراجع إلى حالة المحادثة السابقة واستعادة مكتبة الكود"
    }

    val noCheckpoints: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无检查点"
        AppLanguage.ENGLISH -> "No checkpoints"
        AppLanguage.ARABIC -> "لا توجد نقاط تحقق"
    }

    val autoCreateCheckpointHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "每次对话后会自动创建检查点"
        AppLanguage.ENGLISH -> "Checkpoints are created automatically after each conversation"
        AppLanguage.ARABIC -> "يتم إنشاء نقاط التحقق تلقائيًا بعد كل محادثة"
    }

    val codesCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个代码"
        AppLanguage.ENGLISH -> "%d codes"
        AppLanguage.ARABIC -> "%d أكواد"
    }

    val continueDevBasedOnCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "基于这个代码继续开发:"
        AppLanguage.ENGLISH -> "Continue development based on this code:"
        AppLanguage.ARABIC -> "متابعة التطوير بناءً على هذا الكود:"
    }

    val exportedToProjectLibrary: String get() = when (lang) {
        AppLanguage.CHINESE -> "已导出到项目库"
        AppLanguage.ENGLISH -> "Exported to project library"
        AppLanguage.ARABIC -> "تم التصدير إلى مكتبة المشاريع"
    }

    val rolledBackTo: String get() = when (lang) {
        AppLanguage.CHINESE -> "已回退到: %s"
        AppLanguage.ENGLISH -> "Rolled back to: %s"
        AppLanguage.ARABIC -> "تم التراجع إلى: %s"
    }

    val rolledBackWithInputHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "已回退到: %s\n最后的消息已填入输入框，点击发送重新生成"
        AppLanguage.ENGLISH -> "Rolled back to: %s\nLast message filled in input, click send to regenerate"
        AppLanguage.ARABIC -> "تم التراجع إلى: %s\nتم ملء الرسالة الأخيرة في الإدخال، انقر إرسال لإعادة الإنشاء"
    }

    val rollbackFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "回退失败"
        AppLanguage.ENGLISH -> "Rollback failed"
        AppLanguage.ARABIC -> "فشل التراجع"
    }
    // ==================== AI Module Builder ====================
    val restart: String get() = when (lang) {
        AppLanguage.CHINESE -> "重新开始"
        AppLanguage.ENGLISH -> "Restart"
        AppLanguage.ARABIC -> "إعادة البدء"
    }

    val syntaxCheck: String get() = when (lang) {
        AppLanguage.CHINESE -> "语法检查"
        AppLanguage.ENGLISH -> "Syntax Check"
        AppLanguage.ARABIC -> "فحص بناء الجملة"
    }

    val securityScan: String get() = when (lang) {
        AppLanguage.CHINESE -> "安全扫描"
        AppLanguage.ENGLISH -> "Security Scan"
        AppLanguage.ARABIC -> "فحص الأمان"
    }

    val autoFix: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动修复"
        AppLanguage.ENGLISH -> "Auto Fix"
        AppLanguage.ARABIC -> "إصلاح تلقائي"
    }

    val codeTemplate: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码模板"
        AppLanguage.ENGLISH -> "Code Template"
        AppLanguage.ARABIC -> "قالب الكود"
    }

    val instantTest: String get() = when (lang) {
        AppLanguage.CHINESE -> "即时测试"
        AppLanguage.ENGLISH -> "Instant Test"
        AppLanguage.ARABIC -> "اختبار فوري"
    }

    val tryTheseExamples: String get() = when (lang) {
        AppLanguage.CHINESE -> "试试这些示例"
        AppLanguage.ENGLISH -> "Try these examples"
        AppLanguage.ARABIC -> "جرب هذه الأمثلة"
    }

    val exampleBlockAds: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏蔽网页上的广告弹窗和横幅"
        AppLanguage.ENGLISH -> "Block ad popups and banners on web pages"
        AppLanguage.ARABIC -> "حظر النوافذ المنبثقة واللافتات الإعلانية على صفحات الويب"
    }

    val exampleDarkMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "为网页添加深色模式"
        AppLanguage.ENGLISH -> "Add dark mode to web pages"
        AppLanguage.ARABIC -> "إضافة الوضع الداكن لصفحات الويب"
    }

    val exampleAutoScroll: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动滚动页面，方便阅读长文章"
        AppLanguage.ENGLISH -> "Auto scroll page for reading long articles"
        AppLanguage.ARABIC -> "التمرير التلقائي للصفحة لقراءة المقالات الطويلة"
    }

    val exampleUnlockCopy: String get() = when (lang) {
        AppLanguage.CHINESE -> "解除网页的复制限制"
        AppLanguage.ENGLISH -> "Remove copy restrictions on web pages"
        AppLanguage.ARABIC -> "إزالة قيود النسخ على صفحات الويب"
    }

    val exampleVideoSpeed: String get() = when (lang) {
        AppLanguage.CHINESE -> "为视频添加倍速播放控制"
        AppLanguage.ENGLISH -> "Add playback speed control for videos"
        AppLanguage.ARABIC -> "إضافة التحكم في سرعة التشغيل للفيديو"
    }

    val exampleBackToTop: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加返回顶部悬浮按钮"
        AppLanguage.ENGLISH -> "Add floating back-to-top button"
        AppLanguage.ARABIC -> "إضافة زر عائم للعودة إلى الأعلى"
    }

    val statusAnalyzing: String get() = when (lang) {
        AppLanguage.CHINESE -> "分析中"
        AppLanguage.ENGLISH -> "Analyzing"
        AppLanguage.ARABIC -> "جاري التحليل"
    }

    val statusPlanning: String get() = when (lang) {
        AppLanguage.CHINESE -> "规划中"
        AppLanguage.ENGLISH -> "Planning"
        AppLanguage.ARABIC -> "جاري التخطيط"
    }

    val statusExecuting: String get() = when (lang) {
        AppLanguage.CHINESE -> "执行中"
        AppLanguage.ENGLISH -> "Executing"
        AppLanguage.ARABIC -> "جاري التنفيذ"
    }

    val statusGenerating: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成中"
        AppLanguage.ENGLISH -> "Generating"
        AppLanguage.ARABIC -> "جاري الإنشاء"
    }

    val statusReviewing: String get() = when (lang) {
        AppLanguage.CHINESE -> "审查中"
        AppLanguage.ENGLISH -> "Reviewing"
        AppLanguage.ARABIC -> "جاري المراجعة"
    }

    val statusFixing: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复中"
        AppLanguage.ENGLISH -> "Fixing"
        AppLanguage.ARABIC -> "جاري الإصلاح"
    }

    val statusProcessing: String get() = when (lang) {
        AppLanguage.CHINESE -> "Processing"
        AppLanguage.ENGLISH -> "Processing"
        AppLanguage.ARABIC -> "جاري المعالجة"
    }

    val statusChecking: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查中"
        AppLanguage.ENGLISH -> "Checking"
        AppLanguage.ARABIC -> "جاري الفحص"
    }

    val statusScanning: String get() = when (lang) {
        AppLanguage.CHINESE -> "扫描中"
        AppLanguage.ENGLISH -> "Scanning"
        AppLanguage.ARABIC -> "جاري المسح"
    }

    val syntaxCheckingStatus: String get() = when (lang) {
        AppLanguage.CHINESE -> "语法检查中..."
        AppLanguage.ENGLISH -> "Checking syntax..."
        AppLanguage.ARABIC -> "جاري فحص بناء الجملة..."
    }

    val fixingIssuesStatus: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复检测到的问题..."
        AppLanguage.ENGLISH -> "Fixing detected issues..."
        AppLanguage.ARABIC -> "جاري إصلاح المشاكل المكتشفة..."
    }

    val securityScanningStatus: String get() = when (lang) {
        AppLanguage.CHINESE -> "安全扫描中..."
        AppLanguage.ENGLISH -> "Security scanning..."
        AppLanguage.ARABIC -> "جاري فحص الأمان..."
    }

    val codeModifiedHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码已修改，保存时将使用修改后的版本"
        AppLanguage.ENGLISH -> "Code modified, the modified version will be used when saving"
        AppLanguage.ARABIC -> "تم تعديل الكود، سيتم استخدام النسخة المعدلة عند الحفظ"
    }

    val secureStatus: String get() = when (lang) {
        AppLanguage.CHINESE -> "安全"
        AppLanguage.ENGLISH -> "Secure"
        AppLanguage.ARABIC -> "آمن"
    }

    val analyzingRequirements: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在分析需求..."
        AppLanguage.ENGLISH -> "Analyzing requirements..."
        AppLanguage.ARABIC -> "جاري تحليل المتطلبات..."
    }

    val planningDevelopment: String get() = when (lang) {
        AppLanguage.CHINESE -> "制定开发计划..."
        AppLanguage.ENGLISH -> "Planning development..."
        AppLanguage.ARABIC -> "جاري تخطيط التطوير..."
    }

    val executingToolCalls: String get() = when (lang) {
        AppLanguage.CHINESE -> "执行工具调用..."
        AppLanguage.ENGLISH -> "Executing tool calls..."
        AppLanguage.ARABIC -> "جاري تنفيذ استدعاءات الأدوات..."
    }

    val generatingCodeStatus: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成代码中..."
        AppLanguage.ENGLISH -> "Generating code..."
        AppLanguage.ARABIC -> "جاري إنشاء الكود..."
    }

    val reviewingCodeQuality: String get() = when (lang) {
        AppLanguage.CHINESE -> "审查代码质量..."
        AppLanguage.ENGLISH -> "Reviewing code quality..."
        AppLanguage.ARABIC -> "جاري مراجعة جودة الكود..."
    }

    val fixingDetectedIssues: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复检测到的问题..."
        AppLanguage.ENGLISH -> "Fixing detected issues..."
        AppLanguage.ARABIC -> "جاري إصلاح المشاكل المكتشفة..."
    }

    val categoryLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类:"
        AppLanguage.ENGLISH -> "Category:"
        AppLanguage.ARABIC -> "الفئة:"
    }

    val autoDetectCategory: String get() = when (lang) {
        AppLanguage.CHINESE -> "Auto Detect"
        AppLanguage.ENGLISH -> "Auto Detect"
        AppLanguage.ARABIC -> "كشف تلقائي"
    }

    val inputPlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "描述你想要的功能，例如：屏蔽网页上的广告弹窗..."
        AppLanguage.ENGLISH -> "Describe the feature you want, e.g.: Block ad popups on web pages..."
        AppLanguage.ARABIC -> "صف الميزة التي تريدها، مثال: حظر النوافذ المنبثقة الإعلانية على صفحات الويب..."
    }

    val startDevelopment: String get() = when (lang) {
        AppLanguage.CHINESE -> "开始开发"
        AppLanguage.ENGLISH -> "Start Development"
        AppLanguage.ARABIC -> "بدء التطوير"
    }
    // ==================== Agent Reasoning Messages ====================
    val agentAnalyzing: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在分析需求"
        AppLanguage.ENGLISH -> "Analyzing requirement"
        AppLanguage.ARABIC -> "تحليل المتطلبات"
    }

    val agentPlanning: String get() = when (lang) {
        AppLanguage.CHINESE -> "制定开发计划..."
        AppLanguage.ENGLISH -> "Creating development plan..."
        AppLanguage.ARABIC -> "إنشاء خطة التطوير..."
    }

    val agentCallingAi: String get() = when (lang) {
        AppLanguage.CHINESE -> "调用 AI 模型生成扩展模块代码..."
        AppLanguage.ENGLISH -> "Calling AI model to generate module code..."
        AppLanguage.ARABIC -> "استدعاء نموذج AI لتوليد كود الوحدة..."
    }

    val agentAiCallFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 调用失败"
        AppLanguage.ENGLISH -> "AI call failed"
        AppLanguage.ARABIC -> "فشل استدعاء AI"
    }

    val agentParsing: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码生成完成，正在解析..."
        AppLanguage.ENGLISH -> "Code generated, parsing..."
        AppLanguage.ARABIC -> "تم توليد الكود، جاري التحليل..."
    }

    val agentParseFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "无法解析 AI 生成的代码"
        AppLanguage.ENGLISH -> "Failed to parse AI-generated code"
        AppLanguage.ARABIC -> "فشل تحليل الكود المولّد بواسطة AI"
    }

    val agentSyntaxChecking: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在检查代码语法..."
        AppLanguage.ENGLISH -> "Checking code syntax..."
        AppLanguage.ARABIC -> "فحص قواعد الكود..."
    }

    val agentFoundErrors: String get() = when (lang) {
        AppLanguage.CHINESE -> "发现 %d 个语法错误，%d 个警告"
        AppLanguage.ENGLISH -> "Found %d syntax errors, %d warnings"
        AppLanguage.ARABIC -> "تم العثور على %d أخطاء نحوية، %d تحذيرات"
    }

    val agentAutoFixing: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在尝试自动修复..."
        AppLanguage.ENGLISH -> "Attempting auto-fix..."
        AppLanguage.ARABIC -> "محاولة الإصلاح التلقائي..."
    }

    val agentErrorsFixed: String get() = when (lang) {
        AppLanguage.CHINESE -> "错误已修复"
        AppLanguage.ENGLISH -> "Errors fixed"
        AppLanguage.ARABIC -> "تم إصلاح الأخطاء"
    }

    val agentSyntaxPassed: String get() = when (lang) {
        AppLanguage.CHINESE -> "语法检查通过 [PASS]"
        AppLanguage.ENGLISH -> "Syntax check passed [PASS]"
        AppLanguage.ARABIC -> "اجتاز فحص القواعد [PASS]"
    }

    val agentSecurityScanning: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在进行安全扫描..."
        AppLanguage.ENGLISH -> "Running security scan..."
        AppLanguage.ARABIC -> "تشغيل فحص أمني..."
    }

    val agentSecurityIssues: String get() = when (lang) {
        AppLanguage.CHINESE -> "[!] 发现 %d 个安全问题，风险等级: %s"
        AppLanguage.ENGLISH -> "[!] Found %d security issues, risk level: %s"
        AppLanguage.ARABIC -> "[!] تم العثور على %d مشاكل أمنية، مستوى الخطر: %s"
    }

    val agentSecurityPassed: String get() = when (lang) {
        AppLanguage.CHINESE -> "安全扫描通过 [PASS]"
        AppLanguage.ENGLISH -> "Security scan passed [PASS]"
        AppLanguage.ARABIC -> "اجتاز الفحص الأمني [PASS]"
    }

    val agentModuleCompleted: String get() = when (lang) {
        AppLanguage.CHINESE -> "[OK] 模块「%s」开发完成！"
        AppLanguage.ENGLISH -> "[OK] Module \"%s\" development complete!"
        AppLanguage.ARABIC -> "[OK] اكتمل تطوير الوحدة \"%s\"!"
    }

    val agentModuleGenerated: String get() = when (lang) {
        AppLanguage.CHINESE -> "已成功生成模块「%s」"
        AppLanguage.ENGLISH -> "Successfully generated module \"%s\""
        AppLanguage.ARABIC -> "تم توليد الوحدة \"%s\" بنجاح"
    }

    val agentAutoFixFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动修复失败，请手动检查代码"
        AppLanguage.ENGLISH -> "Auto-fix failed, please check code manually"
        AppLanguage.ARABIC -> "فشل الإصلاح التلقائي، يرجى فحص الكود يدوياً"
    }

    val agentRequestTimeout: String get() = when (lang) {
        AppLanguage.CHINESE -> "请求超时，请检查网络连接后重试"
        AppLanguage.ENGLISH -> "Request timeout, please check network and retry"
        AppLanguage.ARABIC -> "انتهت مهلة الطلب، يرجى التحقق من الشبكة وإعادة المحاولة"
    }

    val agentToolFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "工具 %s 执行失败: %s"
        AppLanguage.ENGLISH -> "Tool %s failed: %s"
        AppLanguage.ARABIC -> "فشلت الأداة %s: %s"
    }

    val versionMultiUi: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持多种UI类型"
        AppLanguage.ENGLISH -> "Supports multiple UI types"
        AppLanguage.ARABIC -> "يدعم أنواع واجهات متعددة"
    }

    val versionV4Ui: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持交互/自动运行模式与独立窗口"
        AppLanguage.ENGLISH -> "Interactive/Auto mode with independent window"
        AppLanguage.ARABIC -> "وضع تفاعلي/تلقائي مع نافذة مستقلة"
    }

    val runModeInteractive: String get() = when (lang) {
        AppLanguage.CHINESE -> "交互模式"
        AppLanguage.ENGLISH -> "Interactive"
        AppLanguage.ARABIC -> "تفاعلي"
    }

    val runModeAuto: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动模式"
        AppLanguage.ENGLISH -> "Auto"
        AppLanguage.ARABIC -> "تلقائي"
    }

    val runModeInteractiveDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "可在管理面板中操作简单UI，也可启动独立窗口使用完整UI"
        AppLanguage.ENGLISH -> "Simple UI in management panel, or launch independent window for full UI"
        AppLanguage.ARABIC -> "واجهة بسيطة في لوحة الإدارة، أو تشغيل نافذة مستقلة للواجهة الكاملة"
    }

    val runModeAutoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动加载运行，无UI操作界面，不可交互"
        AppLanguage.ENGLISH -> "Auto-load and run, no UI, non-interactive"
        AppLanguage.ARABIC -> "تحميل وتشغيل تلقائي، بدون واجهة، غير تفاعلي"
    }

    val runModeLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行方式"
        AppLanguage.ENGLISH -> "Run Mode"
        AppLanguage.ARABIC -> "وضع التشغيل"
    }

    val tagSelectedText: String get() = when (lang) {
        AppLanguage.CHINESE -> "选中"
        AppLanguage.ENGLISH -> "Selection"
        AppLanguage.ARABIC -> "تحديد"
    }

    val templateAutoRefresh: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动刷新"
        AppLanguage.ENGLISH -> "Auto Refresh"
        AppLanguage.ARABIC -> "التحديث التلقائي"
    }

    val templateAutoRefreshDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "定时自动刷新页面"
        AppLanguage.ENGLISH -> "Auto-refresh page at intervals"
        AppLanguage.ARABIC -> "تحديث الصفحة تلقائياً على فترات"
    }

    val templateRefreshInterval: String get() = when (lang) {
        AppLanguage.CHINESE -> "刷新间隔(秒)"
        AppLanguage.ENGLISH -> "Refresh Interval (sec)"
        AppLanguage.ARABIC -> "فترة التحديث (ثانية)"
    }

    val templateShowCountdown: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示倒计时"
        AppLanguage.ENGLISH -> "Show Countdown"
        AppLanguage.ARABIC -> "إظهار العد التنازلي"
    }

    val templateScrollToTop: String get() = when (lang) {
        AppLanguage.CHINESE -> "返回顶部按钮"
        AppLanguage.ENGLISH -> "Scroll to Top Button"
        AppLanguage.ARABIC -> "زر العودة للأعلى"
    }

    val templateScrollToTopDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加返回顶部悬浮按钮"
        AppLanguage.ENGLISH -> "Add a floating scroll-to-top button"
        AppLanguage.ARABIC -> "إضافة زر عائم للعودة للأعلى"
    }

    val templateShowAfterScroll: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动多少后显示(px)"
        AppLanguage.ENGLISH -> "Show After Scroll (px)"
        AppLanguage.ARABIC -> "إظهار بعد التمرير (بكسل)"
    }

    val templateDataExtractor: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据提取器"
        AppLanguage.ENGLISH -> "Data Extractor"
        AppLanguage.ARABIC -> "مستخرج البيانات"
    }

    val templateDataExtractorDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "提取页面数据并显示"
        AppLanguage.ENGLISH -> "Extract and display page data"
        AppLanguage.ARABIC -> "استخراج وعرض بيانات الصفحة"
    }

    val templateDataSelector: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据选择器"
        AppLanguage.ENGLISH -> "Data Selector"
        AppLanguage.ARABIC -> "محدد البيانات"
    }

    val templateExtractAttribute: String get() = when (lang) {
        AppLanguage.CHINESE -> "提取属性"
        AppLanguage.ENGLISH -> "Extract Attribute"
        AppLanguage.ARABIC -> "استخراج السمة"
    }

    val templateLinkCollector: String get() = when (lang) {
        AppLanguage.CHINESE -> "链接收集器"
        AppLanguage.ENGLISH -> "Link Collector"
        AppLanguage.ARABIC -> "جامع الروابط"
    }

    val templateLinkCollectorDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "收集页面上的所有链接"
        AppLanguage.ENGLISH -> "Collect all links on the page"
        AppLanguage.ARABIC -> "جمع جميع الروابط في الصفحة"
    }

    val templateFilterKeyword: String get() = when (lang) {
        AppLanguage.CHINESE -> "过滤关键词"
        AppLanguage.ENGLISH -> "Filter Keyword"
        AppLanguage.ARABIC -> "كلمة التصفية"
    }

    val templateImageGrabber: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片抓取器"
        AppLanguage.ENGLISH -> "Image Grabber"
        AppLanguage.ARABIC -> "جامع الصور"
    }

    val templateImageGrabberDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "收集页面上的所有图片"
        AppLanguage.ENGLISH -> "Collect all images on the page"
        AppLanguage.ARABIC -> "جمع جميع الصور في الصفحة"
    }

    val templateMinSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "最小尺寸(px)"
        AppLanguage.ENGLISH -> "Min Size (px)"
        AppLanguage.ARABIC -> "الحد الأدنى للحجم (بكسل)"
    }

    val templateVideoEnhancer: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频增强器"
        AppLanguage.ENGLISH -> "Video Enhancer"
        AppLanguage.ARABIC -> "محسن الفيديو"
    }

    val templateVideoEnhancerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "为视频添加倍速控制、画中画等功能"
        AppLanguage.ENGLISH -> "Add speed control, picture-in-picture, etc. for videos"
        AppLanguage.ARABIC -> "إضافة التحكم في السرعة والصورة داخل الصورة للفيديو"
    }

    val templateDefaultSpeed: String get() = when (lang) {
        AppLanguage.CHINESE -> "默认倍速"
        AppLanguage.ENGLISH -> "Default Speed"
        AppLanguage.ARABIC -> "السرعة الافتراضية"
    }

    val templateShowControlPanel: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示控制面板"
        AppLanguage.ENGLISH -> "Show Control Panel"
        AppLanguage.ARABIC -> "إظهار لوحة التحكم"
    }

    val templateImageZoomer: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片放大镜"
        AppLanguage.ENGLISH -> "Image Zoomer"
        AppLanguage.ARABIC -> "مكبر الصور"
    }

    val templateImageZoomerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击图片放大查看"
        AppLanguage.ENGLISH -> "Click image to zoom"
        AppLanguage.ARABIC -> "انقر على الصورة للتكبير"
    }

    val templateAudioController: String get() = when (lang) {
        AppLanguage.CHINESE -> "音频控制器"
        AppLanguage.ENGLISH -> "Audio Controller"
        AppLanguage.ARABIC -> "متحكم الصوت"
    }

    val templateAudioControllerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "统一控制页面上的所有音频"
        AppLanguage.ENGLISH -> "Unified control of all audio on the page"
        AppLanguage.ARABIC -> "تحكم موحد في جميع الصوتيات في الصفحة"
    }

    val templateDefaultVolume: String get() = when (lang) {
        AppLanguage.CHINESE -> "默认音量(%)"
        AppLanguage.ENGLISH -> "Default Volume (%)"
        AppLanguage.ARABIC -> "مستوى الصوت الافتراضي (%)"
    }

    val templateNotificationBlocker: String get() = when (lang) {
        AppLanguage.CHINESE -> "通知拦截器"
        AppLanguage.ENGLISH -> "Notification Blocker"
        AppLanguage.ARABIC -> "حاجب الإشعارات"
    }

    val templateNotificationBlockerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止网站请求通知权限"
        AppLanguage.ENGLISH -> "Block website notification permission requests"
        AppLanguage.ARABIC -> "حظر طلبات إذن الإشعارات من المواقع"
    }

    val templateTrackingBlocker: String get() = when (lang) {
        AppLanguage.CHINESE -> "追踪拦截器"
        AppLanguage.ENGLISH -> "Tracking Blocker"
        AppLanguage.ARABIC -> "حاجب التتبع"
    }

    val templateTrackingBlockerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止常见的追踪脚本"
        AppLanguage.ENGLISH -> "Block common tracking scripts"
        AppLanguage.ARABIC -> "حظر نصوص التتبع الشائعة"
    }

    val templateFingerprintProtector: String get() = when (lang) {
        AppLanguage.CHINESE -> "指纹保护器"
        AppLanguage.ENGLISH -> "Fingerprint Protector"
        AppLanguage.ARABIC -> "حامي البصمة"
    }

    val templateFingerprintProtectorDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "防止浏览器指纹追踪"
        AppLanguage.ENGLISH -> "Prevent browser fingerprint tracking"
        AppLanguage.ARABIC -> "منع تتبع بصمة المتصفح"
    }

    val templateConsoleLogger: String get() = when (lang) {
        AppLanguage.CHINESE -> "控制台日志"
        AppLanguage.ENGLISH -> "Console Logger"
        AppLanguage.ARABIC -> "مسجل وحدة التحكم"
    }

    val templateConsoleLoggerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "在页面上显示控制台日志"
        AppLanguage.ENGLISH -> "Display console logs on the page"
        AppLanguage.ARABIC -> "عرض سجلات وحدة التحكم على الصفحة"
    }

    val templateMaxLogs: String get() = when (lang) {
        AppLanguage.CHINESE -> "最大日志数"
        AppLanguage.ENGLISH -> "Max Logs"
        AppLanguage.ARABIC -> "الحد الأقصى للسجلات"
    }

    val templateNetworkMonitor: String get() = when (lang) {
        AppLanguage.CHINESE -> "网络监控器"
        AppLanguage.ENGLISH -> "Network Monitor"
        AppLanguage.ARABIC -> "مراقب الشبكة"
    }

    val templateNetworkMonitorDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监控页面的网络请求"
        AppLanguage.ENGLISH -> "Monitor page network requests"
        AppLanguage.ARABIC -> "مراقبة طلبات شبكة الصفحة"
    }

    val templateDomInspector: String get() = when (lang) {
        AppLanguage.CHINESE -> "DOM检查器"
        AppLanguage.ENGLISH -> "DOM Inspector"
        AppLanguage.ARABIC -> "فاحص DOM"
    }

    val templateDomInspectorDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬停查看元素信息"
        AppLanguage.ENGLISH -> "Hover to view element info"
        AppLanguage.ARABIC -> "تمرير لعرض معلومات العنصر"
    }
    // ==================== Agent Tool Descriptions ====================
    val agentToolSyntaxCheck: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查 JavaScript 或 CSS 代码的语法错误。返回错误列表和修复建议。"
        AppLanguage.ENGLISH -> "Check JavaScript or CSS code for syntax errors. Returns error list and fix suggestions."
        AppLanguage.ARABIC -> "فحص أخطاء بناء الجملة في كود JavaScript أو CSS. يُرجع قائمة الأخطاء واقتراحات الإصلاح."
    }

    val agentToolLintCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查代码风格和最佳实践，提供优化建议。"
        AppLanguage.ENGLISH -> "Check code style and best practices, provide optimization suggestions."
        AppLanguage.ARABIC -> "فحص نمط الكود وأفضل الممارسات، تقديم اقتراحات التحسين."
    }

    val agentToolSecurityScan: String get() = when (lang) {
        AppLanguage.CHINESE -> "扫描代码中的安全问题，如 XSS、不安全的 eval 使用等。"
        AppLanguage.ENGLISH -> "Scan code for security issues like XSS, unsafe eval usage, etc."
        AppLanguage.ARABIC -> "فحص الكود بحثاً عن مشاكل أمنية مثل XSS واستخدام eval غير الآمن."
    }

    val agentToolGenerateCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "根据需求描述生成 JavaScript/CSS 代码。"
        AppLanguage.ENGLISH -> "Generate JavaScript/CSS code based on requirement description."
        AppLanguage.ARABIC -> "إنشاء كود JavaScript/CSS بناءً على وصف المتطلبات."
    }

    val agentToolFixError: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动修复代码中检测到的错误。"
        AppLanguage.ENGLISH -> "Automatically fix detected errors in code."
        AppLanguage.ARABIC -> "إصلاح الأخطاء المكتشفة في الكود تلقائياً."
    }

    val agentToolRefactorCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "重构和优化代码，提高可读性和性能。"
        AppLanguage.ENGLISH -> "Refactor and optimize code, improve readability and performance."
        AppLanguage.ARABIC -> "إعادة هيكلة وتحسين الكود، تحسين قابلية القراءة والأداء."
    }

    val agentToolTestModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "在测试页面运行模块代码，返回执行结果。"
        AppLanguage.ENGLISH -> "Run module code on test page, return execution results."
        AppLanguage.ARABIC -> "تشغيل كود الوحدة على صفحة الاختبار، إرجاع نتائج التنفيذ."
    }

    val agentToolValidateConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "验证模块配置项的完整性和正确性。"
        AppLanguage.ENGLISH -> "Validate completeness and correctness of module configuration items."
        AppLanguage.ARABIC -> "التحقق من اكتمال وصحة عناصر تكوين الوحدة."
    }

    val agentToolGetTemplates: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取与需求相关的代码模板。"
        AppLanguage.ENGLISH -> "Get code templates related to requirements."
        AppLanguage.ARABIC -> "الحصول على قوالب الكود المتعلقة بالمتطلبات."
    }

    val agentToolGetSnippets: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索可用的代码片段。"
        AppLanguage.ENGLISH -> "Search for available code snippets."
        AppLanguage.ARABIC -> "البحث عن مقتطفات الكود المتاحة."
    }

    val agentToolCreateModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建新的扩展模块。"
        AppLanguage.ENGLISH -> "Create a new extension module."
        AppLanguage.ARABIC -> "إنشاء وحدة امتداد جديدة."
    }

    val agentToolPreviewModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览模块在指定页面的效果。"
        AppLanguage.ENGLISH -> "Preview module effect on specified page."
        AppLanguage.ARABIC -> "معاينة تأثير الوحدة على الصفحة المحددة."
    }

    val toolTypeSyntaxCheck: String get() = when (lang) {
        AppLanguage.CHINESE -> "语法检查"
        AppLanguage.ENGLISH -> "Syntax Check"
        AppLanguage.ARABIC -> "فحص بناء الجملة"
    }

    val toolTypeSyntaxCheckDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查 JavaScript/CSS 代码语法错误"
        AppLanguage.ENGLISH -> "Check JavaScript/CSS code syntax errors"
        AppLanguage.ARABIC -> "فحص أخطاء بناء الجملة في كود JavaScript/CSS"
    }

    val toolTypeLintCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码规范检查"
        AppLanguage.ENGLISH -> "Code Lint"
        AppLanguage.ARABIC -> "فحص معايير الكود"
    }

    val toolTypeLintCodeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查代码风格和最佳实践"
        AppLanguage.ENGLISH -> "Check code style and best practices"
        AppLanguage.ARABIC -> "فحص نمط الكود وأفضل الممارسات"
    }

    val toolTypeSecurityScan: String get() = when (lang) {
        AppLanguage.CHINESE -> "安全扫描"
        AppLanguage.ENGLISH -> "Security Scan"
        AppLanguage.ARABIC -> "فحص الأمان"
    }

    val toolTypeSecurityScanDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查潜在的安全问题"
        AppLanguage.ENGLISH -> "Check for potential security issues"
        AppLanguage.ARABIC -> "فحص المشاكل الأمنية المحتملة"
    }

    val toolTypeGenerateCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成代码"
        AppLanguage.ENGLISH -> "Generate Code"
        AppLanguage.ARABIC -> "إنشاء الكود"
    }

    val toolTypeGenerateCodeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "根据需求生成代码"
        AppLanguage.ENGLISH -> "Generate code based on requirements"
        AppLanguage.ARABIC -> "إنشاء الكود بناءً على المتطلبات"
    }

    val toolTypeRefactorCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "重构代码"
        AppLanguage.ENGLISH -> "Refactor Code"
        AppLanguage.ARABIC -> "إعادة هيكلة الكود"
    }

    val toolTypeRefactorCodeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化和重构现有代码"
        AppLanguage.ENGLISH -> "Optimize and refactor existing code"
        AppLanguage.ARABIC -> "تحسين وإعادة هيكلة الكود الحالي"
    }

    val toolTypeFixError: String get() = when (lang) {
        AppLanguage.CHINESE -> "修复错误"
        AppLanguage.ENGLISH -> "Fix Error"
        AppLanguage.ARABIC -> "إصلاح الخطأ"
    }

    val toolTypeFixErrorDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动修复检测到的错误"
        AppLanguage.ENGLISH -> "Automatically fix detected errors"
        AppLanguage.ARABIC -> "إصلاح الأخطاء المكتشفة تلقائياً"
    }

    val toolTypeTestModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "测试模块"
        AppLanguage.ENGLISH -> "Test Module"
        AppLanguage.ARABIC -> "اختبار الوحدة"
    }

    val toolTypeTestModuleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "在测试页面运行模块"
        AppLanguage.ENGLISH -> "Run module on test page"
        AppLanguage.ARABIC -> "تشغيل الوحدة على صفحة الاختبار"
    }

    val toolTypeValidateConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "验证配置"
        AppLanguage.ENGLISH -> "Validate Config"
        AppLanguage.ARABIC -> "التحقق من التكوين"
    }

    val toolTypeValidateConfigDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "验证模块配置项"
        AppLanguage.ENGLISH -> "Validate module configuration items"
        AppLanguage.ARABIC -> "التحقق من عناصر تكوين الوحدة"
    }

    val toolTypeGetTemplates: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取模板"
        AppLanguage.ENGLISH -> "Get Templates"
        AppLanguage.ARABIC -> "الحصول على القوالب"
    }

    val toolTypeGetTemplatesDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取相关代码模板"
        AppLanguage.ENGLISH -> "Get related code templates"
        AppLanguage.ARABIC -> "الحصول على قوالب الكود ذات الصلة"
    }

    val toolTypeGetSnippets: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取代码片段"
        AppLanguage.ENGLISH -> "Get Snippets"
        AppLanguage.ARABIC -> "الحصول على مقتطفات الكود"
    }

    val toolTypeGetSnippetsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取可用的代码片段"
        AppLanguage.ENGLISH -> "Get available code snippets"
        AppLanguage.ARABIC -> "الحصول على مقتطفات الكود المتاحة"
    }

    val toolTypeSearchDocs: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索文档"
        AppLanguage.ENGLISH -> "Search Docs"
        AppLanguage.ARABIC -> "البحث في المستندات"
    }

    val toolTypeSearchDocsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索相关文档和示例"
        AppLanguage.ENGLISH -> "Search related docs and examples"
        AppLanguage.ARABIC -> "البحث عن المستندات والأمثلة ذات الصلة"
    }

    val toolTypeCreateModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建模块"
        AppLanguage.ENGLISH -> "Create Module"
        AppLanguage.ARABIC -> "إنشاء وحدة"
    }

    val toolTypeCreateModuleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建新的扩展模块"
        AppLanguage.ENGLISH -> "Create a new extension module"
        AppLanguage.ARABIC -> "إنشاء وحدة امتداد جديدة"
    }

    val toolTypeUpdateModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "更新模块"
        AppLanguage.ENGLISH -> "Update Module"
        AppLanguage.ARABIC -> "تحديث الوحدة"
    }

    val toolTypeUpdateModuleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "更新现有模块"
        AppLanguage.ENGLISH -> "Update existing module"
        AppLanguage.ARABIC -> "تحديث الوحدة الحالية"
    }

    val toolTypePreviewModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览模块"
        AppLanguage.ENGLISH -> "Preview Module"
        AppLanguage.ARABIC -> "معاينة الوحدة"
    }

    val toolTypePreviewModuleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "预览模块效果"
        AppLanguage.ENGLISH -> "Preview module effect"
        AppLanguage.ARABIC -> "معاينة تأثير الوحدة"
    }
    // ==================== AI Settings ====================
    val apiKeys: String get() = when (lang) {
        AppLanguage.CHINESE -> "API 密钥"
        AppLanguage.ENGLISH -> "API Keys"
        AppLanguage.ARABIC -> "مفاتيح API"
    }

    val noApiKeysHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无 API 密钥，点击右上角添加"
        AppLanguage.ENGLISH -> "No API keys yet, click top right to add"
        AppLanguage.ARABIC -> "لا توجد مفاتيح API بعد، انقر في الأعلى للإضافة"
    }

    val testing: String get() = when (lang) {
        AppLanguage.CHINESE -> "测试中..."
        AppLanguage.ENGLISH -> "Testing..."
        AppLanguage.ARABIC -> "جاري الاختبار..."
    }

    val connectionSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "[OK] 连接成功"
        AppLanguage.ENGLISH -> "[OK] Connection successful"
        AppLanguage.ARABIC -> "[OK] الاتصال ناجح"
    }

    val test: String get() = when (lang) {
        AppLanguage.CHINESE -> "测试"
        AppLanguage.ENGLISH -> "Test"
        AppLanguage.ARABIC -> "اختبار"
    }

    val savedModels: String get() = when (lang) {
        AppLanguage.CHINESE -> "已保存的模型"
        AppLanguage.ENGLISH -> "Saved Models"
        AppLanguage.ARABIC -> "النماذج المحفوظة"
    }

    val configModelCapabilities: String get() = when (lang) {
        AppLanguage.CHINESE -> "配置模型能力标签，用于不同场景"
        AppLanguage.ENGLISH -> "Configure model capability tags for different scenarios"
        AppLanguage.ARABIC -> "تكوين علامات قدرات النموذج لسيناريوهات مختلفة"
    }

    val pleaseAddApiKeyFirst: String get() = when (lang) {
        AppLanguage.CHINESE -> "请先添加 API 密钥"
        AppLanguage.ENGLISH -> "Please add API key first"
        AppLanguage.ARABIC -> "يرجى إضافة مفتاح API أولاً"
    }

    val noSavedModelsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无已保存的模型，点击右上角添加"
        AppLanguage.ENGLISH -> "No saved models yet, click top right to add"
        AppLanguage.ARABIC -> "لا توجد نماذج محفوظة بعد، انقر في الأعلى للإضافة"
    }

    val defaultLabel: String get() = when (lang) {
        AppLanguage.CHINESE -> "默认"
        AppLanguage.ENGLISH -> "Default"
        AppLanguage.ARABIC -> "افتراضي"
    }

    val setAsDefault: String get() = when (lang) {
        AppLanguage.CHINESE -> "设为默认"
        AppLanguage.ENGLISH -> "Set as Default"
        AppLanguage.ARABIC -> "تعيين كافتراضي"
    }

    val editApiKey: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑 API 密钥"
        AppLanguage.ENGLISH -> "Edit API Key"
        AppLanguage.ARABIC -> "تعديل مفتاح API"
    }

    val addApiKey: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加 API 密钥"
        AppLanguage.ENGLISH -> "Add API Key"
        AppLanguage.ARABIC -> "إضافة مفتاح API"
    }

    val getApiKey: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取 API Key"
        AppLanguage.ENGLISH -> "Get API Key"
        AppLanguage.ARABIC -> "الحصول على مفتاح API"
    }

    val openAiCompatibleHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "OpenAI 兼容接口地址"
        AppLanguage.ENGLISH -> "OpenAI compatible endpoint"
        AppLanguage.ARABIC -> "نقطة نهاية متوافقة مع OpenAI"
    }

    val apiFormat: String get() = when (lang) {
        AppLanguage.CHINESE -> "API 格式"
        AppLanguage.ENGLISH -> "API Format"
        AppLanguage.ARABIC -> "تنسيق API"
    }

    val apiKeyAliasPlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "例如：我的 GPT-4 Key"
        AppLanguage.ENGLISH -> "e.g. My GPT-4 Key"
        AppLanguage.ARABIC -> "مثال: مفتاح GPT-4 الخاص بي"
    }

    val modelsEndpoint: String get() = when (lang) {
        AppLanguage.CHINESE -> "模型列表端点"
        AppLanguage.ENGLISH -> "Models Endpoint"
        AppLanguage.ARABIC -> "نقطة نهاية النماذج"
    }

    val modelsEndpointHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "默认: /v1/models"
        AppLanguage.ENGLISH -> "Default: /v1/models"
        AppLanguage.ARABIC -> "الافتراضي: /v1/models"
    }

    val chatEndpoint: String get() = when (lang) {
        AppLanguage.CHINESE -> "聊天端点"
        AppLanguage.ENGLISH -> "Chat Endpoint"
        AppLanguage.ARABIC -> "نقطة نهاية الدردشة"
    }

    val chatEndpointHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "默认: /v1/chat/completions"
        AppLanguage.ENGLISH -> "Default: /v1/chat/completions"
        AppLanguage.ARABIC -> "الافتراضي: /v1/chat/completions"
    }

    val selectApiKey: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择 API Key"
        AppLanguage.ENGLISH -> "Select API Key"
        AppLanguage.ARABIC -> "اختر مفتاح API"
    }

    val batchSelectModels: String get() = when (lang) {
        AppLanguage.CHINESE -> "批量选择模型"
        AppLanguage.ENGLISH -> "Batch Select Models"
        AppLanguage.ARABIC -> "تحديد النماذج دفعة واحدة"
    }

    val selectedModelsCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "已选择 %d 个模型"
        AppLanguage.ENGLISH -> "%d models selected"
        AppLanguage.ARABIC -> "تم تحديد %d نماذج"
    }

    val addSelectedModels: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加所选模型"
        AppLanguage.ENGLISH -> "Add Selected Models"
        AppLanguage.ARABIC -> "إضافة النماذج المحددة"
    }

    val searchModels: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索模型名称或 ID..."
        AppLanguage.ENGLISH -> "Search model name or ID..."
        AppLanguage.ARABIC -> "ابحث عن اسم النموذج أو المعرف..."
    }

    val noSearchResults: String get() = when (lang) {
        AppLanguage.CHINESE -> "没有找到匹配的模型"
        AppLanguage.ENGLISH -> "No matching models found"
        AppLanguage.ARABIC -> "لم يتم العثور على نماذج مطابقة"
    }

    val sortByName: String get() = when (lang) {
        AppLanguage.CHINESE -> "按名称"
        AppLanguage.ENGLISH -> "By Name"
        AppLanguage.ARABIC -> "حسب الاسم"
    }

    val sortByContext: String get() = when (lang) {
        AppLanguage.CHINESE -> "按上下文"
        AppLanguage.ENGLISH -> "By Context"
        AppLanguage.ARABIC -> "حسب السياق"
    }

    val sortByPriceLow: String get() = when (lang) {
        AppLanguage.CHINESE -> "价格低到高"
        AppLanguage.ENGLISH -> "Price Low to High"
        AppLanguage.ARABIC -> "السعر من الأقل للأعلى"
    }

    val sortByPriceHigh: String get() = when (lang) {
        AppLanguage.CHINESE -> "价格高到低"
        AppLanguage.ENGLISH -> "Price High to Low"
        AppLanguage.ARABIC -> "السعر من الأعلى للأقل"
    }

    val addModel: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加模型"
        AppLanguage.ENGLISH -> "Add Model"
        AppLanguage.ARABIC -> "إضافة نموذج"
    }

    val addModelFrom: String get() = when (lang) {
        AppLanguage.CHINESE -> "从以下供应商添加模型："
        AppLanguage.ENGLISH -> "Add model from:"
        AppLanguage.ARABIC -> "إضافة نموذج من:"
    }

    val orManualInputModelId: String get() = when (lang) {
        AppLanguage.CHINESE -> "或手动输入模型 ID"
        AppLanguage.ENGLISH -> "Or manually input model ID"
        AppLanguage.ARABIC -> "أو أدخل معرف النموذج يدويًا"
    }

    val modelIdPlaceholder: String get() = when (lang) {
        AppLanguage.CHINESE -> "例如: gpt-4o-mini"
        AppLanguage.ENGLISH -> "e.g. gpt-4o-mini"
        AppLanguage.ARABIC -> "مثال: gpt-4o-mini"
    }

    val capabilityTags: String get() = when (lang) {
        AppLanguage.CHINESE -> "能力标签"
        AppLanguage.ENGLISH -> "Capability Tags"
        AppLanguage.ARABIC -> "علامات القدرات"
    }

    val selectCapabilitiesHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择此模型支持的能力"
        AppLanguage.ENGLISH -> "Select capabilities this model supports"
        AppLanguage.ARABIC -> "اختر القدرات التي يدعمها هذا النموذج"
    }

    val editModel: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑模型"
        AppLanguage.ENGLISH -> "Edit Model"
        AppLanguage.ARABIC -> "تعديل النموذج"
    }

    val featureSceneConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "功能场景配置"
        AppLanguage.ENGLISH -> "Feature Scene Config"
        AppLanguage.ARABIC -> "تكوين سيناريو الميزة"
    }

    val selectFeaturesForCapability: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择此能力适用的功能场景"
        AppLanguage.ENGLISH -> "Select feature scenes for this capability"
        AppLanguage.ARABIC -> "اختر سيناريوهات الميزات لهذه القدرة"
    }
    // ==================== Icon Storm v2.0 ====================
    val iconStormMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "⚡ Icon Storm 模式"
        AppLanguage.ENGLISH -> "⚡ Icon Storm Mode"
        AppLanguage.ARABIC -> "⚡ وضع عاصفة الأيقونات"
    }

    val iconStormIcons: String get() = when (lang) {
        AppLanguage.CHINESE -> "个图标"
        AppLanguage.ENGLISH -> "icons"
        AppLanguage.ARABIC -> "أيقونات"
    }

    val iconStormNoLimit: String get() = when (lang) {
        AppLanguage.CHINESE -> "🔓 无上限 — 利用 activity-alias 原生机制注入无限桌面图标"
        AppLanguage.ENGLISH -> "🔓 No limit — inject unlimited desktop icons via native activity-alias"
        AppLanguage.ARABIC -> "🔓 بدون حد — حقن أيقونات غير محدودة عبر activity-alias الأصلية"
    }

    val iconStormUnlimited: String get() = when (lang) {
        AppLanguage.CHINESE -> "最小 2，无上限 ∞"
        AppLanguage.ENGLISH -> "Min 2, no upper limit ∞"
        AppLanguage.ARABIC -> "الحد الأدنى 2، بدون حد أعلى ∞"
    }

    val iconStormImpactAssessment: String get() = when (lang) {
        AppLanguage.CHINESE -> "影响评估"
        AppLanguage.ENGLISH -> "Impact Assessment"
        AppLanguage.ARABIC -> "تقييم التأثير"
    }

    val iconStormImpactPrefix: String get() = when (lang) {
        AppLanguage.CHINESE -> ""
        AppLanguage.ENGLISH -> ""
        AppLanguage.ARABIC -> ""
    }

    val iconStormImpactNone: String get() = when (lang) {
        AppLanguage.CHINESE -> "无影响"
        AppLanguage.ENGLISH -> "None"
        AppLanguage.ARABIC -> "بدون تأثير"
    }

    val iconStormImpactLight: String get() = when (lang) {
        AppLanguage.CHINESE -> "轻微"
        AppLanguage.ENGLISH -> "Light"
        AppLanguage.ARABIC -> "خفيف"
    }

    val iconStormImpactMedium: String get() = when (lang) {
        AppLanguage.CHINESE -> "中等"
        AppLanguage.ENGLISH -> "Medium"
        AppLanguage.ARABIC -> "متوسط"
    }

    val iconStormImpactHeavy: String get() = when (lang) {
        AppLanguage.CHINESE -> "大量"
        AppLanguage.ENGLISH -> "Heavy"
        AppLanguage.ARABIC -> "كثيف"
    }

    val iconStormImpactExtreme: String get() = when (lang) {
        AppLanguage.CHINESE -> "极端"
        AppLanguage.ENGLISH -> "Extreme"
        AppLanguage.ARABIC -> "شديد"
    }

    val iconStormImpactDangerous: String get() = when (lang) {
        AppLanguage.CHINESE -> "☢️ 危险"
        AppLanguage.ENGLISH -> "☢️ Dangerous"
        AppLanguage.ARABIC -> "☢️ خطير"
    }

    val iconStormAliasCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "Alias 注入数"
        AppLanguage.ENGLISH -> "Alias Injected"
        AppLanguage.ARABIC -> "عدد الأسماء المستعارة"
    }

    val iconStormManifestOverhead: String get() = when (lang) {
        AppLanguage.CHINESE -> "Manifest 增量"
        AppLanguage.ENGLISH -> "Manifest Overhead"
        AppLanguage.ARABIC -> "حجم إضافي للمانيفست"
    }

    val iconStormEffectNone: String get() = when (lang) {
        AppLanguage.CHINESE -> "✅ 设备不受影响，正常使用"
        AppLanguage.ENGLISH -> "✅ No impact on device, normal usage"
        AppLanguage.ARABIC -> "✅ لا تأثير على الجهاز، استخدام عادي"
    }

    val iconStormEffectLight: String get() = when (lang) {
        AppLanguage.CHINESE -> "💚 桌面可见多个图标，轻微影响"
        AppLanguage.ENGLISH -> "💚 Multiple icons visible on home screen, slight impact"
        AppLanguage.ARABIC -> "💚 أيقونات متعددة مرئية على الشاشة الرئيسية، تأثير طفيف"
    }

    val iconStormEffectMedium: String get() = when (lang) {
        AppLanguage.CHINESE -> "🟡 桌面图标明显增多，Launcher 可能短暂卡顿"
        AppLanguage.ENGLISH -> "🟡 Noticeable icon flood on home screen, Launcher may briefly lag"
        AppLanguage.ARABIC -> "🟡 فيضان أيقونات ملحوظ، قد يتباطأ المشغل لفترة وجيزة"
    }

    val iconStormEffectHeavy: String get() = when (lang) {
        AppLanguage.CHINESE -> "🟠 桌面几乎被图标覆盖，Launcher 出现明显延迟"
        AppLanguage.ENGLISH -> "🟠 Home screen nearly covered with icons, Launcher shows noticeable delay"
        AppLanguage.ARABIC -> "🟠 الشاشة الرئيسية مغطاة تقريباً بالأيقونات، تأخر ملحوظ في المشغل"
    }

    val iconStormEffectExtreme: String get() = when (lang) {
        AppLanguage.CHINESE -> "🔴 Launcher 严重卡顿，设备响应缓慢，可能触发 ANR"
        AppLanguage.ENGLISH -> "🔴 Launcher severely lagging, device unresponsive, may trigger ANR"
        AppLanguage.ARABIC -> "🔴 تباطؤ شديد في المشغل، الجهاز غير مستجيب، قد يسبب ANR"
    }

    val iconStormEffectDangerous: String get() = when (lang) {
        AppLanguage.CHINESE -> "☢️ 设备可能完全卡死需重启，PackageManager 可能崩溃。仅供安全研究。"
        AppLanguage.ENGLISH -> "☢️ Device may freeze completely requiring reboot. PackageManager may crash. For security research only."
        AppLanguage.ARABIC -> "☢️ قد يتجمد الجهاز بالكامل ويحتاج إعادة تشغيل. للبحث الأمني فقط."
    }

    val iconStormRandomNames: String get() = when (lang) {
        AppLanguage.CHINESE -> "随机化图标名称"
        AppLanguage.ENGLISH -> "Randomize Icon Names"
        AppLanguage.ARABIC -> "تعشير أسماء الأيقونات"
    }

    val iconStormRandomNamesDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "每个图标使用不同的随机名称，增加混淆性"
        AppLanguage.ENGLISH -> "Each icon uses a different random name for obfuscation"
        AppLanguage.ARABIC -> "كل أيقونة تستخدم اسماً عشوائياً مختلفاً للتمويه"
    }

    val iconStormNamePrefix: String get() = when (lang) {
        AppLanguage.CHINESE -> "名称前缀"
        AppLanguage.ENGLISH -> "Name Prefix"
        AppLanguage.ARABIC -> "بادئة الاسم"
    }

    val iconStormNamePrefixHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "留空则使用应用名，填写则每个图标显示: 前缀 1, 前缀 2..."
        AppLanguage.ENGLISH -> "Leave empty to use app name, or enter prefix for: Prefix 1, Prefix 2..."
        AppLanguage.ARABIC -> "اتركه فارغاً لاستخدام اسم التطبيق، أو أدخل بادئة"
    }

    val iconStormTip: String get() = when (lang) {
        AppLanguage.CHINESE -> "Icon Storm 基于 Android activity-alias 原生机制，每个 alias 都拥有独立的 MAIN/LAUNCHER intent-filter，因此 Launcher 必须为每个 alias 创建独立图标。这是 Android 框架的设计行为，非漏洞利用。极端数量下 Launcher 的处理能力成为瓶颈。"
        AppLanguage.ENGLISH -> "Icon Storm leverages Android's native activity-alias mechanism. Each alias has its own MAIN/LAUNCHER intent-filter, forcing the Launcher to create an independent icon. This is by-design Android framework behavior, not an exploit. At extreme counts, the Launcher's processing capacity becomes the bottleneck."
        AppLanguage.ARABIC -> "تستفيد عاصفة الأيقونات من آلية activity-alias الأصلية في Android. كل alias له intent-filter خاص به."
    }

    val iconStormWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "⚠️ 高数量图标可能导致目标设备的 Launcher 严重卡顿甚至冻结。本功能仅供安全研究和 Launcher 压力测试使用。请确保了解风险后再构建 APK。"
        AppLanguage.ENGLISH -> "⚠️ High icon counts may cause severe Launcher lag or device freeze on the target device. This feature is for security research and Launcher stress testing only. Please understand the risks before building the APK."
        AppLanguage.ARABIC -> "⚠️ قد تسبب الأعداد الكبيرة من الأيقونات تباطؤاً شديداً أو تجميداً للجهاز. هذه الميزة للبحث الأمني واختبار الإجهاد فقط."
    }
    // ==================== AI Provider ====================
    val providerOpenAI: String get() = "OpenAI"

    val providerOpenAIDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "文本表现出色，推理能力强，支持文本、视觉和图像生成"
        AppLanguage.ENGLISH -> "Excellent text performance, strong reasoning, supports text, vision and image generation"
        AppLanguage.ARABIC -> "أداء نصي ممتاز، قدرة استدلال قوية، يدعم النص والرؤية وتوليد الصور"
    }

    val providerOpenAIPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "GPT 5.1 系列约 \$10/ 百万token"
        AppLanguage.ENGLISH -> "GPT 5.1 series ~\$10/million tokens"
        AppLanguage.ARABIC -> "سلسلة GPT 5.1 حوالي \$10/مليون رمز"
    }

    val providerOpenRouter: String get() = "OpenRouter"

    val providerOpenRouterDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "聚合多家 AI 供应商，统一接口调用。可用同一 API Key 调用 OpenAI、Claude、Gemini 等多种模型"
        AppLanguage.ENGLISH -> "Aggregates multiple AI providers with unified API. Use one API Key to access OpenAI, Claude, Gemini and more"
        AppLanguage.ARABIC -> "يجمع مزودي الذكاء الاصطناعي المتعددين بواجهة موحدة. استخدم مفتاح API واحد للوصول إلى OpenAI وClaude وGemini والمزيد"
    }

    val providerOpenRouterPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "按模型不同计费，价格透明，有免费模型，强烈推荐"
        AppLanguage.ENGLISH -> "Pay per model, transparent pricing, free models available, highly recommended"
        AppLanguage.ARABIC -> "الدفع لكل نموذج، تسعير شفاف، نماذج مجانية متاحة، موصى به بشدة"
    }

    val providerAnthropic: String get() = "Anthropic/Claude"

    val providerAnthropicDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Claude 系列模型，擅长文本理解和代码生成且有视觉支持，编程能力强。"
        AppLanguage.ENGLISH -> "Claude models, excels at text understanding and code generation with vision support, strong programming ability."
        AppLanguage.ARABIC -> "نماذج Claude، متميزة في فهم النص وتوليد الكود مع دعم الرؤية، قدرة برمجة قوية."
    }

    val providerAnthropicPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "Claude 4.5 Sonnet 约 \$15/百万 token"
        AppLanguage.ENGLISH -> "Claude 4.5 Sonnet ~\$15/million tokens"
        AppLanguage.ARABIC -> "Claude 4.5 Sonnet حوالي \$15/مليون رمز"
    }

    val providerGoogle: String get() = "Google/Gemini"

    val providerGoogleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "★推荐★ Gemini 3.0 Pro 前端表现出色，原生多模态支持，全面顶配支持。"
        AppLanguage.ENGLISH -> "★Recommended★ Gemini 3.0 Pro excels at frontend, native multimodal support, fully featured."
        AppLanguage.ARABIC -> "★موصى به★ Gemini 3.0 Pro متميز في الواجهة الأمامية، دعم متعدد الوسائط أصلي، ميزات كاملة."
    }

    val providerGooglePricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "有免费额度，超出后按 token 计费"
        AppLanguage.ENGLISH -> "Free tier available, pay per token after limit"
        AppLanguage.ARABIC -> "مستوى مجاني متاح، الدفع لكل رمز بعد الحد"
    }

    val providerDeepSeek: String get() = "DeepSeek"

    val providerDeepSeekDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "国家队，性价比高。目前仅支持文本和图像文本生成"
        AppLanguage.ENGLISH -> "Chinese national team, cost-effective. Currently supports text and image-text generation only"
        AppLanguage.ARABIC -> "الفريق الوطني الصيني، فعال من حيث التكلفة. يدعم حاليًا توليد النص والصور-النص فقط"
    }

    val providerDeepSeekPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "极低价格，约 ¥0.4/百万 token"
        AppLanguage.ENGLISH -> "Very low price, ~¥0.4/million tokens"
        AppLanguage.ARABIC -> "سعر منخفض جدًا، حوالي ¥0.4/مليون رمز"
    }

    val providerMiniMax: String get() = "MiniMax"

    val providerMiniMaxDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "国产，支持高音质人声语音克隆/合成。文本模型性能优秀，代码agent能力较强"
        AppLanguage.ENGLISH -> "Chinese, supports high-quality voice cloning/synthesis. Excellent text model, strong code agent capability"
        AppLanguage.ARABIC -> "صيني، يدعم استنساخ/تركيب الصوت عالي الجودة. نموذج نص ممتاز، قدرة وكيل كود قوية"
    }

    val providerMiniMaxPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "价格较低，约 \$1/百万 token"
        AppLanguage.ENGLISH -> "Low price, ~\$1/million tokens"
        AppLanguage.ARABIC -> "سعر منخفض، حوالي \$1/مليون رمز"
    }

    val providerGLM: String get() = when (lang) {
        AppLanguage.CHINESE -> "智谱GLM"
        AppLanguage.ENGLISH -> "Zhipu GLM"
        AppLanguage.ARABIC -> "Zhipu GLM"
    }

    val providerGLMDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "国产，GLM-4.6 系列性能优秀，编码能力强，支持多模态"
        AppLanguage.ENGLISH -> "Chinese, GLM-4.6 series performs well, strong coding ability, supports multimodal"
        AppLanguage.ARABIC -> "صيني، سلسلة GLM-4.6 أداء جيد، قدرة برمجة قوية، يدعم متعدد الوسائط"
    }

    val providerGLMPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "价格较低，约 \$2/百万 token"
        AppLanguage.ENGLISH -> "Low price, ~\$2/million tokens"
        AppLanguage.ARABIC -> "سعر منخفض، حوالي \$2/مليون رمز"
    }

    val providerGrok: String get() = "xAI/Grok"

    val providerGrokDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "马斯克旗下 xAI 的 Grok 系列，支持文本和视觉"
        AppLanguage.ENGLISH -> "Elon Musk's xAI Grok series, supports text and vision"
        AppLanguage.ARABIC -> "سلسلة Grok من xAI التابعة لإيلون ماسك، تدعم النص والرؤية"
    }

    val providerGrokPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "价格便宜，Grok-4.1-fast 约 \$0.5/百万 token"
        AppLanguage.ENGLISH -> "Cheap, Grok-4.1-fast ~\$0.5/million tokens"
        AppLanguage.ARABIC -> "رخيص، Grok-4.1-fast حوالي \$0.5/مليون رمز"
    }

    val providerVolcano: String get() = when (lang) {
        AppLanguage.CHINESE -> "火山引擎"
        AppLanguage.ENGLISH -> "Volcano Engine"
        AppLanguage.ARABIC -> "Volcano Engine"
    }

    val providerVolcanoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "字节跳动旗下，豆包大模型生态均衡。推荐模型：doubao-1.6-pro-256k"
        AppLanguage.ENGLISH -> "ByteDance's AI platform, balanced Doubao model ecosystem. Recommended: doubao-1.6-pro-256k"
        AppLanguage.ARABIC -> "منصة ByteDance للذكاء الاصطناعي، نظام Doubao متوازن. موصى به: doubao-1.6-pro-256k"
    }

    val providerVolcanoPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "有免费额度，价格便宜"
        AppLanguage.ENGLISH -> "Free tier available, cheap pricing"
        AppLanguage.ARABIC -> "مستوى مجاني متاح، تسعير رخيص"
    }

    val providerSiliconFlow: String get() = when (lang) {
        AppLanguage.CHINESE -> "硅基流动"
        AppLanguage.ENGLISH -> "SiliconFlow"
        AppLanguage.ARABIC -> "SiliconFlow"
    }

    val providerSiliconFlowDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "国产 AI 平台，聚合多种开源模型。"
        AppLanguage.ENGLISH -> "Chinese AI platform, aggregates various open source models."
        AppLanguage.ARABIC -> "منصة ذكاء اصطناعي صينية، تجمع نماذج مفتوحة المصدر متنوعة."
    }

    val providerSiliconFlowPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "有免费额度，价格便宜"
        AppLanguage.ENGLISH -> "Free tier available, cheap pricing"
        AppLanguage.ARABIC -> "مستوى مجاني متاح، تسعير رخيص"
    }

    val providerQwen: String get() = when (lang) {
        AppLanguage.CHINESE -> "通义千问"
        AppLanguage.ENGLISH -> "Tongyi Qwen"
        AppLanguage.ARABIC -> "Tongyi Qwen"
    }

    val providerQwenDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "阿里云通义千问，支持文本、视觉、音频等多模态。Qwen3 系列推理能力强"
        AppLanguage.ENGLISH -> "Alibaba Cloud Tongyi Qwen, supports text, vision, audio multimodal. Qwen3 series has strong reasoning"
        AppLanguage.ARABIC -> "Alibaba Cloud Tongyi Qwen، يدعم النص والرؤية والصوت متعدد الوسائط. سلسلة Qwen3 لديها استدلال قوي"
    }

    val providerQwenPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "有免费额度，价格便宜，约 ¥0.5/百万 token"
        AppLanguage.ENGLISH -> "Free tier available, cheap, ~¥0.5/million tokens"
        AppLanguage.ARABIC -> "مستوى مجاني متاح، رخيص، حوالي ¥0.5/مليون رمز"
    }

    val providerCustom: String get() = when (lang) {
        AppLanguage.CHINESE -> "Custom"
        AppLanguage.ENGLISH -> "Custom"
        AppLanguage.ARABIC -> "مخصص"
    }

    val providerCustomDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "兼容 OpenAI API 格式的自定义服务。需要填写完整的 Base URL"
        AppLanguage.ENGLISH -> "Custom service compatible with OpenAI API format. Requires full Base URL"
        AppLanguage.ARABIC -> "خدمة مخصصة متوافقة مع تنسيق OpenAI API. يتطلب عنوان URL الأساسي الكامل"
    }

    val providerCustomPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "取决于服务商"
        AppLanguage.ENGLISH -> "Depends on provider"
        AppLanguage.ARABIC -> "يعتمد على المزود"
    }
    // ==================== New AI Providers (LiteLLM) ====================
    val providerMistral: String get() = "Mistral AI"

    val providerMistralDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "欧洲领先 AI 公司，Mistral Large 系列推理能力强，支持多语言和代码生成"
        AppLanguage.ENGLISH -> "Leading European AI company. Mistral Large series with strong reasoning, multilingual and code generation support"
        AppLanguage.ARABIC -> "شركة ذكاء اصطناعي أوروبية رائدة. سلسلة Mistral Large مع استدلال قوي ودعم متعدد اللغات وتوليد الكود"
    }

    val providerMistralPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "Mistral Large 约 \$2/百万 token，有免费模型"
        AppLanguage.ENGLISH -> "Mistral Large ~\$2/M tokens, free models available"
        AppLanguage.ARABIC -> "Mistral Large حوالي \$2/مليون رمز، نماذج مجانية متاحة"
    }

    val providerCohere: String get() = "Cohere"

    val providerCohereDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "企业级 AI 平台，Command R+ 系列擅长 RAG 和企业搜索，支持 100+ 语言"
        AppLanguage.ENGLISH -> "Enterprise AI platform. Command R+ series excels at RAG and enterprise search, supports 100+ languages"
        AppLanguage.ARABIC -> "منصة ذكاء اصطناعي للمؤسسات. سلسلة Command R+ متميزة في RAG والبحث المؤسسي، تدعم 100+ لغة"
    }

    val providerCoherePricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "Command R+ 约 \$2.5/百万 token"
        AppLanguage.ENGLISH -> "Command R+ ~\$2.5/M tokens"
        AppLanguage.ARABIC -> "Command R+ حوالي \$2.5/مليون رمز"
    }

    val providerAI21: String get() = "AI21 Labs"

    val providerAI21Desc: String get() = when (lang) {
        AppLanguage.CHINESE -> "以色列 AI 公司，Jamba 系列基于 SSM-Transformer 混合架构，长上下文处理出色"
        AppLanguage.ENGLISH -> "Israeli AI company. Jamba series based on SSM-Transformer hybrid, excels at long context processing"
        AppLanguage.ARABIC -> "شركة ذكاء اصطناعي إسرائيلية. سلسلة Jamba مبنية على هجين SSM-Transformer، متميزة في معالجة السياق الطويل"
    }

    val providerAI21Pricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "Jamba 系列约 \$2/百万 token"
        AppLanguage.ENGLISH -> "Jamba series ~\$2/M tokens"
        AppLanguage.ARABIC -> "سلسلة Jamba حوالي \$2/مليون رمز"
    }

    val providerGroq: String get() = "Groq"

    val providerGroqDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "超快推理引擎，基于 LPU 硬件加速。响应速度极快，适合实时对话场景"
        AppLanguage.ENGLISH -> "Ultra-fast inference engine powered by LPU hardware. Extremely fast response, ideal for real-time chat"
        AppLanguage.ARABIC -> "محرك استدلال فائق السرعة مدعوم بأجهزة LPU. استجابة سريعة للغاية، مثالي للدردشة في الوقت الفعلي"
    }

    val providerGroqPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "有免费额度，价格低廉"
        AppLanguage.ENGLISH -> "Free tier available, very affordable"
        AppLanguage.ARABIC -> "مستوى مجاني متاح، بأسعار معقولة جداً"
    }

    val providerCerebras: String get() = "Cerebras"

    val providerCerebrasDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "晶圆级芯片推理，速度极快。支持 Llama 等开源模型的超高速推理"
        AppLanguage.ENGLISH -> "Wafer-scale chip inference, extremely fast. Supports ultra-fast inference for Llama and other open-source models"
        AppLanguage.ARABIC -> "استدلال على مستوى الرقاقة، سريع للغاية. يدعم الاستدلال فائق السرعة لـ Llama ونماذج مفتوحة المصدر أخرى"
    }

    val providerCerebrasPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "有免费额度，按量计费"
        AppLanguage.ENGLISH -> "Free tier available, pay per usage"
        AppLanguage.ARABIC -> "مستوى مجاني متاح، الدفع حسب الاستخدام"
    }

    val providerSambanova: String get() = "SambaNova"

    val providerSambanovaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 芯片公司，提供高速推理服务。支持 Llama、DeepSeek 等开源模型"
        AppLanguage.ENGLISH -> "AI chip company providing high-speed inference. Supports Llama, DeepSeek and other open-source models"
        AppLanguage.ARABIC -> "شركة شرائح ذكاء اصطناعي توفر استدلال عالي السرعة. تدعم Llama وDeepSeek ونماذج مفتوحة المصدر أخرى"
    }

    val providerSambanovaPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "有免费额度，价格低廉"
        AppLanguage.ENGLISH -> "Free tier available, affordable pricing"
        AppLanguage.ARABIC -> "مستوى مجاني متاح، تسعير معقول"
    }

    val providerTogether: String get() = "Together AI"

    val providerTogetherDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "开源模型聚合平台，支持数百种开源模型。OpenAI 兼容接口，支持微调"
        AppLanguage.ENGLISH -> "Open-source model aggregator, supports hundreds of models. OpenAI-compatible API with fine-tuning support"
        AppLanguage.ARABIC -> "منصة تجميع نماذج مفتوحة المصدر، تدعم مئات النماذج. واجهة متوافقة مع OpenAI مع دعم الضبط الدقيق"
    }

    val providerTogetherPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "按模型计费，开源模型价格低"
        AppLanguage.ENGLISH -> "Pay per model, low prices for open-source models"
        AppLanguage.ARABIC -> "الدفع لكل نموذج، أسعار منخفضة للنماذج مفتوحة المصدر"
    }

    val providerPerplexity: String get() = "Perplexity"

    val providerPerplexityDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 搜索引擎，Sonar 系列模型自带在线搜索能力，适合需要实时信息的场景"
        AppLanguage.ENGLISH -> "AI search engine. Sonar series models with built-in web search, ideal for real-time information needs"
        AppLanguage.ARABIC -> "محرك بحث ذكاء اصطناعي. سلسلة Sonar مع بحث ويب مدمج، مثالي لاحتياجات المعلومات في الوقت الفعلي"
    }

    val providerPerplexityPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "Sonar Pro 约 \$3/百万 token + 搜索费用"
        AppLanguage.ENGLISH -> "Sonar Pro ~\$3/M tokens + search costs"
        AppLanguage.ARABIC -> "Sonar Pro حوالي \$3/مليون رمز + تكاليف البحث"
    }

    val providerFireworks: String get() = "Fireworks AI"

    val providerFireworksDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "高性能推理平台，支持多种开源模型。专注低延迟推理优化"
        AppLanguage.ENGLISH -> "High-performance inference platform with multiple open-source models. Focuses on low-latency inference optimization"
        AppLanguage.ARABIC -> "منصة استدلال عالية الأداء مع نماذج مفتوحة المصدر متعددة. تركز على تحسين الاستدلال منخفض الكمون"
    }

    val providerFireworksPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "按模型计费，价格有竞争力"
        AppLanguage.ENGLISH -> "Pay per model, competitive pricing"
        AppLanguage.ARABIC -> "الدفع لكل نموذج، تسعير تنافسي"
    }

    val providerDeepInfra: String get() = "DeepInfra"

    val providerDeepInfraDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "开源模型推理平台，支持数百种模型。OpenAI 兼容接口，价格低廉"
        AppLanguage.ENGLISH -> "Open-source model inference platform, supports hundreds of models. OpenAI-compatible API, affordable pricing"
        AppLanguage.ARABIC -> "منصة استدلال نماذج مفتوحة المصدر، تدعم مئات النماذج. واجهة متوافقة مع OpenAI، تسعير معقول"
    }

    val providerDeepInfraPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "按量计费，价格低廉"
        AppLanguage.ENGLISH -> "Pay per usage, affordable pricing"
        AppLanguage.ARABIC -> "الدفع حسب الاستخدام، تسعير معقول"
    }

    val providerNovita: String get() = "Novita AI"

    val providerNovitaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 模型推理平台，支持 LLM 和图像生成模型。OpenAI 兼容接口"
        AppLanguage.ENGLISH -> "AI model inference platform, supports LLM and image generation models. OpenAI-compatible API"
        AppLanguage.ARABIC -> "منصة استدلال نماذج الذكاء الاصطناعي، تدعم LLM ونماذج توليد الصور. واجهة متوافقة مع OpenAI"
    }

    val providerNovitaPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "按量计费，价格有竞争力"
        AppLanguage.ENGLISH -> "Pay per usage, competitive pricing"
        AppLanguage.ARABIC -> "الدفع حسب الاستخدام، تسعير تنافسي"
    }

    val providerMoonshot: String get() = when (lang) {
        AppLanguage.CHINESE -> "月之暗面/Kimi"
        AppLanguage.ENGLISH -> "Moonshot/Kimi"
        AppLanguage.ARABIC -> "Moonshot/Kimi"
    }

    val providerMoonshotDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "国产大模型，Kimi 系列擅长长文本处理（200K+ 上下文），编程能力强"
        AppLanguage.ENGLISH -> "Chinese LLM, Kimi series excels at long text processing (200K+ context), strong coding ability"
        AppLanguage.ARABIC -> "نموذج لغوي صيني كبير، سلسلة Kimi متميزة في معالجة النصوص الطويلة (200K+ سياق)، قدرة برمجة قوية"
    }

    val providerMoonshotPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "有免费额度，约 ¥12/百万 token"
        AppLanguage.ENGLISH -> "Free tier available, ~¥12/M tokens"
        AppLanguage.ARABIC -> "مستوى مجاني متاح، حوالي ¥12/مليون رمز"
    }

    val providerBaichuan: String get() = when (lang) {
        AppLanguage.CHINESE -> "百川智能"
        AppLanguage.ENGLISH -> "Baichuan"
        AppLanguage.ARABIC -> "Baichuan"
    }

    val providerBaichuanDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "国产大模型，擅长中文理解和生成，支持搜索增强"
        AppLanguage.ENGLISH -> "Chinese LLM, excels at Chinese language understanding and generation, supports search augmentation"
        AppLanguage.ARABIC -> "نموذج لغوي صيني كبير، متميز في فهم وتوليد اللغة الصينية، يدعم تعزيز البحث"
    }

    val providerBaichuanPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "有免费额度，价格低廉"
        AppLanguage.ENGLISH -> "Free tier available, affordable"
        AppLanguage.ARABIC -> "مستوى مجاني متاح، بأسعار معقولة"
    }

    val providerYi: String get() = when (lang) {
        AppLanguage.CHINESE -> "零一万物"
        AppLanguage.ENGLISH -> "Yi/Lingyiwanwu"
        AppLanguage.ARABIC -> "Yi/Lingyiwanwu"
    }

    val providerYiDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "李开复创办，Yi 系列模型中英文表现均衡，支持长上下文"
        AppLanguage.ENGLISH -> "Founded by Kai-Fu Lee. Yi series models balanced in Chinese-English, supports long context"
        AppLanguage.ARABIC -> "أسسها كاي-فو لي. سلسلة Yi متوازنة في الصينية-الإنجليزية، تدعم السياق الطويل"
    }

    val providerYiPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "有免费额度，价格低廉"
        AppLanguage.ENGLISH -> "Free tier available, affordable"
        AppLanguage.ARABIC -> "مستوى مجاني متاح، بأسعار معقولة"
    }

    val providerStepfun: String get() = when (lang) {
        AppLanguage.CHINESE -> "阶跃星辰"
        AppLanguage.ENGLISH -> "Stepfun"
        AppLanguage.ARABIC -> "Stepfun"
    }

    val providerStepfunDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "国产大模型，Step 系列支持超长上下文（256K），多模态能力强"
        AppLanguage.ENGLISH -> "Chinese LLM, Step series supports ultra-long context (256K), strong multimodal capability"
        AppLanguage.ARABIC -> "نموذج لغوي صيني كبير، سلسلة Step تدعم سياق فائق الطول (256K)، قدرة متعددة الوسائط قوية"
    }

    val providerStepfunPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "有免费额度，按量计费"
        AppLanguage.ENGLISH -> "Free tier available, pay per usage"
        AppLanguage.ARABIC -> "مستوى مجاني متاح، الدفع حسب الاستخدام"
    }

    val providerHunyuan: String get() = when (lang) {
        AppLanguage.CHINESE -> "腾讯混元"
        AppLanguage.ENGLISH -> "Tencent Hunyuan"
        AppLanguage.ARABIC -> "Tencent Hunyuan"
    }

    val providerHunyuanDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "腾讯自研大模型，擅长中文对话和代码生成，支持多模态"
        AppLanguage.ENGLISH -> "Tencent's LLM, excels at Chinese dialogue and code generation, supports multimodal"
        AppLanguage.ARABIC -> "نموذج Tencent اللغوي الكبير، متميز في الحوار الصيني وتوليد الكود، يدعم متعدد الوسائط"
    }

    val providerHunyuanPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "有免费额度，价格较低"
        AppLanguage.ENGLISH -> "Free tier available, low pricing"
        AppLanguage.ARABIC -> "مستوى مجاني متاح، تسعير منخفض"
    }

    val providerSpark: String get() = when (lang) {
        AppLanguage.CHINESE -> "讯飞星火"
        AppLanguage.ENGLISH -> "iFlytek Spark"
        AppLanguage.ARABIC -> "iFlytek Spark"
    }

    val providerSparkDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "科大讯飞大模型，语音识别和中文理解能力强，OpenAI 兼容接口"
        AppLanguage.ENGLISH -> "iFlytek's LLM, strong speech recognition and Chinese understanding, OpenAI-compatible API"
        AppLanguage.ARABIC -> "نموذج iFlytek اللغوي الكبير، تعرف قوي على الكلام وفهم اللغة الصينية، واجهة متوافقة مع OpenAI"
    }

    val providerSparkPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "有免费额度，按量计费"
        AppLanguage.ENGLISH -> "Free tier available, pay per usage"
        AppLanguage.ARABIC -> "مستوى مجاني متاح، الدفع حسب الاستخدام"
    }

    val providerOllama: String get() = "Ollama"

    val providerOllamaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "本地运行开源模型，完全免费。需在电脑上安装 Ollama 并确保手机可访问"
        AppLanguage.ENGLISH -> "Run open-source models locally, completely free. Install Ollama on PC and ensure mobile access"
        AppLanguage.ARABIC -> "تشغيل نماذج مفتوحة المصدر محلياً، مجاني تماماً. ثبت Ollama على الكمبيوتر وتأكد من الوصول عبر الجوال"
    }

    val providerOllamaPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "完全免费（本地运行）"
        AppLanguage.ENGLISH -> "Completely free (local)"
        AppLanguage.ARABIC -> "مجاني تماماً (محلي)"
    }

    val providerLmStudio: String get() = "LM Studio"

    val providerLmStudioDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "桌面端本地模型运行工具，提供 OpenAI 兼容 API。图形化界面管理模型"
        AppLanguage.ENGLISH -> "Desktop local model runner with OpenAI-compatible API. GUI-based model management"
        AppLanguage.ARABIC -> "أداة تشغيل نماذج محلية لسطح المكتب مع واجهة متوافقة مع OpenAI. إدارة نماذج بواجهة رسومية"
    }

    val providerLmStudioPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "完全免费（本地运行）"
        AppLanguage.ENGLISH -> "Completely free (local)"
        AppLanguage.ARABIC -> "مجاني تماماً (محلي)"
    }

    val providerVllm: String get() = "vLLM"

    val providerVllmDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "高性能 LLM 推理引擎，提供 OpenAI 兼容 API。适合自部署生产环境"
        AppLanguage.ENGLISH -> "High-performance LLM inference engine with OpenAI-compatible API. Suitable for self-hosted production"
        AppLanguage.ARABIC -> "محرك استدلال LLM عالي الأداء مع واجهة متوافقة مع OpenAI. مناسب للاستضافة الذاتية في الإنتاج"
    }

    val providerVllmPricing: String get() = when (lang) {
        AppLanguage.CHINESE -> "完全免费（自部署）"
        AppLanguage.ENGLISH -> "Completely free (self-hosted)"
        AppLanguage.ARABIC -> "مجاني تماماً (استضافة ذاتية)"
    }
    // ==================== AI Coding ====================
    val selectCodingType: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择应用类型"
        AppLanguage.ENGLISH -> "Select App Type"
        AppLanguage.ARABIC -> "اختر نوع التطبيق"
    }

    val codingTypeHtml: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML"
        AppLanguage.ENGLISH -> "HTML"
        AppLanguage.ARABIC -> "HTML"
    }

    val codingTypeFrontend: String get() = when (lang) {
        AppLanguage.CHINESE -> "前端项目"
        AppLanguage.ENGLISH -> "Frontend"
        AppLanguage.ARABIC -> "الواجهة الأمامية"
    }

    val codingTypeNodejs: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js"
        AppLanguage.ENGLISH -> "Node.js"
        AppLanguage.ARABIC -> "Node.js"
    }

    val codingTypeWordpress: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress"
        AppLanguage.ENGLISH -> "WordPress"
        AppLanguage.ARABIC -> "WordPress"
    }

    val codingTypePhp: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP"
        AppLanguage.ENGLISH -> "PHP"
        AppLanguage.ARABIC -> "PHP"
    }

    val codingTypePython: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python"
        AppLanguage.ENGLISH -> "Python"
        AppLanguage.ARABIC -> "Python"
    }

    val codingTypeGo: String get() = when (lang) {
        AppLanguage.CHINESE -> "Go"
        AppLanguage.ENGLISH -> "Go"
        AppLanguage.ARABIC -> "Go"
    }

    val codingTypeHtmlDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML/CSS/JS 网页应用"
        AppLanguage.ENGLISH -> "HTML/CSS/JS web apps"
        AppLanguage.ARABIC -> "تطبيقات ويب HTML/CSS/JS"
    }

    val codingTypeFrontendDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "React/Vue/Next.js 前端项目"
        AppLanguage.ENGLISH -> "React/Vue/Next.js frontend projects"
        AppLanguage.ARABIC -> "مشاريع الواجهة الأمامية React/Vue/Next.js"
    }

    val codingTypeNodejsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Express/Koa/Fastify 后端服务"
        AppLanguage.ENGLISH -> "Express/Koa/Fastify backend services"
        AppLanguage.ARABIC -> "خدمات خلفية Express/Koa/Fastify"
    }

    val codingTypeWordpressDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress 主题/插件开发"
        AppLanguage.ENGLISH -> "WordPress theme/plugin development"
        AppLanguage.ARABIC -> "تطوير سمات/إضافات WordPress"
    }

    val codingTypePhpDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP 后端应用开发"
        AppLanguage.ENGLISH -> "PHP backend app development"
        AppLanguage.ARABIC -> "تطوير تطبيقات PHP الخلفية"
    }

    val codingTypePythonDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Flask/Django/FastAPI 应用"
        AppLanguage.ENGLISH -> "Flask/Django/FastAPI apps"
        AppLanguage.ARABIC -> "تطبيقات Flask/Django/FastAPI"
    }

    val codingTypeGoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Go Web 服务/API 开发"
        AppLanguage.ENGLISH -> "Go web service/API development"
        AppLanguage.ARABIC -> "تطوير خدمات/واجهات Go"
    }

    val directoryTree: String get() = when (lang) {
        AppLanguage.CHINESE -> "目录树"
        AppLanguage.ENGLISH -> "Directory Tree"
        AppLanguage.ARABIC -> "شجرة الدليل"
    }

    val editCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "编辑代码"
        AppLanguage.ENGLISH -> "Edit Code"
        AppLanguage.ARABIC -> "تحرير الكود"
    }

    val saveFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存文件"
        AppLanguage.ENGLISH -> "Save File"
        AppLanguage.ARABIC -> "حفظ الملف"
    }

    val fileSaved: String get() = when (lang) {
        AppLanguage.CHINESE -> "文件已保存"
        AppLanguage.ENGLISH -> "File saved"
        AppLanguage.ARABIC -> "تم حفظ الملف"
    }

    val exportToProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出到项目"
        AppLanguage.ENGLISH -> "Export to Project"
        AppLanguage.ARABIC -> "تصدير إلى مشروع"
    }

    val previewNotSupported: String get() = when (lang) {
        AppLanguage.CHINESE -> "该应用类型不支持直接预览"
        AppLanguage.ENGLISH -> "Preview not supported for this app type"
        AppLanguage.ARABIC -> "المعاينة غير مدعومة لهذا النوع"
    }

    val noFiles: String get() = when (lang) {
        AppLanguage.CHINESE -> "暂无文件"
        AppLanguage.ENGLISH -> "No files yet"
        AppLanguage.ARABIC -> "لا توجد ملفات بعد"
    }

    val exportAllFiles: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出所有文件"
        AppLanguage.ENGLISH -> "Export All Files"
        AppLanguage.ARABIC -> "تصدير جميع الملفات"
    }

    val downloadAllFiles: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载所有文件"
        AppLanguage.ENGLISH -> "Download All Files"
        AppLanguage.ARABIC -> "تنزيل جميع الملفات"
    }

    val allFilter: String get() = when (lang) {
        AppLanguage.CHINESE -> "全部"
        AppLanguage.ENGLISH -> "All"
        AppLanguage.ARABIC -> "الكل"
    }

    val tryAskingPrompts: String get() = when (lang) {
        AppLanguage.CHINESE -> "试试这些提示"
        AppLanguage.ENGLISH -> "Try these prompts"
        AppLanguage.ARABIC -> "جرّب هذه الأوامر"
    }

    val writeToolDescHtml: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建或覆盖文件。将完整代码写入指定文件。支持 HTML/CSS/JS/JSON/SVG 等文件类型。"
        AppLanguage.ENGLISH -> "Create or overwrite a file. Write complete code to the specified file. Supports HTML/CSS/JS/JSON/SVG file types."
        AppLanguage.ARABIC -> "إنشاء أو استبدال ملف. كتابة الكود الكامل في الملف المحدد. يدعم أنواع ملفات HTML/CSS/JS/JSON/SVG."
    }

    val writeToolDescFrontend: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建或覆盖文件。支持 React/Vue/Next.js 项目的所有文件类型（JSX/TSX/CSS/JSON 等）。"
        AppLanguage.ENGLISH -> "Create or overwrite a file. Supports all React/Vue/Next.js project file types (JSX/TSX/CSS/JSON, etc.)."
        AppLanguage.ARABIC -> "إنشاء أو استبدال ملف. يدعم جميع أنواع ملفات مشاريع React/Vue/Next.js (JSX/TSX/CSS/JSON، إلخ)."
    }

    val writeToolDescNodejs: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建或覆盖文件。支持 Node.js 项目文件（JS/TS/JSON/package.json 等）。"
        AppLanguage.ENGLISH -> "Create or overwrite a file. Supports Node.js project files (JS/TS/JSON/package.json, etc.)."
        AppLanguage.ARABIC -> "إنشاء أو استبدال ملف. يدعم ملفات مشاريع Node.js (JS/TS/JSON/package.json، إلخ)."
    }

    val writeToolDescWordpress: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建或覆盖文件。支持 WordPress 主题/插件文件（PHP/CSS/JS 等）。"
        AppLanguage.ENGLISH -> "Create or overwrite a file. Supports WordPress theme/plugin files (PHP/CSS/JS, etc.)."
        AppLanguage.ARABIC -> "إنشاء أو استبدال ملف. يدعم ملفات سمات/إضافات WordPress (PHP/CSS/JS، إلخ)."
    }

    val writeToolDescPhp: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建或覆盖文件。支持 PHP 项目文件（PHP/HTML/CSS/JS 等）。"
        AppLanguage.ENGLISH -> "Create or overwrite a file. Supports PHP project files (PHP/HTML/CSS/JS, etc.)."
        AppLanguage.ARABIC -> "إنشاء أو استبدال ملف. يدعم ملفات مشاريع PHP (PHP/HTML/CSS/JS، إلخ)."
    }

    val writeToolDescPython: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建或覆盖文件。支持 Python 项目文件（.py/requirements.txt/HTML 模板等）。"
        AppLanguage.ENGLISH -> "Create or overwrite a file. Supports Python project files (.py/requirements.txt/HTML templates, etc.)."
        AppLanguage.ARABIC -> "إنشاء أو استبدال ملف. يدعم ملفات مشاريع Python (.py/requirements.txt/قوالب HTML، إلخ)."
    }

    val writeToolDescGo: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建或覆盖文件。支持 Go 项目文件（.go/go.mod/HTML 模板等）。"
        AppLanguage.ENGLISH -> "Create or overwrite a file. Supports Go project files (.go/go.mod/HTML templates, etc.)."
        AppLanguage.ARABIC -> "إنشاء أو استبدال ملف. يدعم ملفات مشاريع Go (.go/go.mod/قوالب HTML، إلخ)."
    }

    val hintModernMinimal: String get() = when (lang) {
        AppLanguage.CHINESE -> "大量留白、简洁排版、柔和阴影、圆角元素"
        AppLanguage.ENGLISH -> "Generous whitespace, clean layout, soft shadows, rounded elements"
        AppLanguage.ARABIC -> "مساحات بيضاء كبيرة، تخطيط نظيف، ظلال ناعمة، عناصر مستديرة"
    }

    val hintGlassmorphism: String get() = when (lang) {
        AppLanguage.CHINESE -> "backdrop-filter: blur()、半透明背景、渐变色、柔和边框"
        AppLanguage.ENGLISH -> "backdrop-filter: blur(), translucent backgrounds, gradients, soft borders"
        AppLanguage.ARABIC -> "backdrop-filter: blur()، خلفيات شفافة، تدرجات لونية، حدود ناعمة"
    }

    val hintNeumorphism: String get() = when (lang) {
        AppLanguage.CHINESE -> "双层阴影（亮/暗）、柔和背景色、圆角、凸起或凹陷效果"
        AppLanguage.ENGLISH -> "Dual shadows (light/dark), soft background, rounded corners, raised or inset effects"
        AppLanguage.ARABIC -> "ظلال مزدوجة (فاتح/داكن)، خلفية ناعمة، زوايا مستديرة، تأثيرات بارزة أو غائرة"
    }

    val hintDarkMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "深色背景、亮色文字、柔和发光效果、高对比度"
        AppLanguage.ENGLISH -> "Dark backgrounds, light text, soft glow effects, high contrast"
        AppLanguage.ARABIC -> "خلفيات داكنة، نص فاتح، تأثيرات توهج ناعمة، تباين عالٍ"
    }

    val hintCyberpunk: String get() = when (lang) {
        AppLanguage.CHINESE -> "霓虹色彩、发光效果、故障艺术、网格线条、科技感字体"
        AppLanguage.ENGLISH -> "Neon colors, glow effects, glitch art, grid lines, tech-style fonts"
        AppLanguage.ARABIC -> "ألوان نيون، تأثيرات توهج، فن خلل، خطوط شبكية، خطوط تقنية"
    }

    val hintGradient: String get() = when (lang) {
        AppLanguage.CHINESE -> "多彩渐变、流动感、动态背景、圆润形状"
        AppLanguage.ENGLISH -> "Colorful gradients, fluid feel, dynamic backgrounds, rounded shapes"
        AppLanguage.ARABIC -> "تدرجات ملونة، إحساس سائل، خلفيات ديناميكية، أشكال مستديرة"
    }

    val hintMinimal: String get() = when (lang) {
        AppLanguage.CHINESE -> "黑白配色、大量空白、清晰排版、无装饰"
        AppLanguage.ENGLISH -> "Black and white palette, generous whitespace, clear typography, no decoration"
        AppLanguage.ARABIC -> "لوحة أبيض وأسود، مساحات بيضاء كبيرة، طباعة واضحة، بدون زخرفة"
    }

    val hintNature: String get() = when (lang) {
        AppLanguage.CHINESE -> "绿色系、自然元素、圆润形状、柔和阴影"
        AppLanguage.ENGLISH -> "Green palette, natural elements, rounded shapes, soft shadows"
        AppLanguage.ARABIC -> "لوحة خضراء، عناصر طبيعية، أشكال مستديرة، ظلال ناعمة"
    }

    val hintCuteCartoon: String get() = when (lang) {
        AppLanguage.CHINESE -> "圆角元素、柔和阴影、可爱图标、糖果色"
        AppLanguage.ENGLISH -> "Rounded elements, soft shadows, cute icons, candy colors"
        AppLanguage.ARABIC -> "عناصر مستديرة، ظلال ناعمة، أيقونات لطيفة، ألوان حلوى"
    }

    val hintNeonGlow: String get() = when (lang) {
        AppLanguage.CHINESE -> "发光文字、霓虹边框、暗色背景、高对比度"
        AppLanguage.ENGLISH -> "Glowing text, neon borders, dark backgrounds, high contrast"
        AppLanguage.ARABIC -> "نص متوهج، حدود نيون، خلفيات داكنة، تباين عالٍ"
    }

    val colorsHarryPotter: String get() = when (lang) {
        AppLanguage.CHINESE -> "深红,金色,深棕,墨绿"
        AppLanguage.ENGLISH -> "Deep red,Gold,Dark brown,Dark green"
        AppLanguage.ARABIC -> "أحمر داكن,ذهبي,بني داكن,أخضر داكن"
    }

    val colorsGhibli: String get() = when (lang) {
        AppLanguage.CHINESE -> "天空蓝,草绿,泥土棕,夕阳橙"
        AppLanguage.ENGLISH -> "Sky blue,Grass green,Earth brown,Sunset orange"
        AppLanguage.ARABIC -> "أزرق سماوي,أخضر عشبي,بني ترابي,برتقالي غروب"
    }

    val colorsYourName: String get() = when (lang) {
        AppLanguage.CHINESE -> "黄昏橙,天际蓝,星光紫,晨曦粉"
        AppLanguage.ENGLISH -> "Twilight orange,Horizon blue,Starlight purple,Dawn pink"
        AppLanguage.ARABIC -> "برتقالي الشفق,أزرق الأفق,بنفسجي نجمي,وردي الفجر"
    }

    val colorsApple: String get() = when (lang) {
        AppLanguage.CHINESE -> "纯白,深空灰,银色,金色"
        AppLanguage.ENGLISH -> "Pure white,Space gray,Silver,Gold"
        AppLanguage.ARABIC -> "أبيض نقي,رمادي فضائي,فضي,ذهبي"
    }

    val colorsLittlePrince: String get() = when (lang) {
        AppLanguage.CHINESE -> "星空蓝,沙漠金,玫瑰红,淡紫"
        AppLanguage.ENGLISH -> "Starry blue,Desert gold,Rose red,Lavender"
        AppLanguage.ARABIC -> "أزرق نجمي,ذهبي صحراوي,أحمر وردي,لافندر"
    }

    val colorsZelda: String get() = when (lang) {
        AppLanguage.CHINESE -> "草原绿,天空蓝,山岩灰,篝火橙"
        AppLanguage.ENGLISH -> "Prairie green,Sky blue,Rock gray,Campfire orange"
        AppLanguage.ARABIC -> "أخضر السهول,أزرق السماء,رمادي الصخور,برتقالي النار"
    }

    val colorsArtDeco: String get() = when (lang) {
        AppLanguage.CHINESE -> "金色,黑色,翡翠绿,深蓝"
        AppLanguage.ENGLISH -> "Gold,Black,Emerald green,Deep blue"
        AppLanguage.ARABIC -> "ذهبي,أسود,أخضر زمردي,أزرق عميق"
    }

    val colorsJapanese: String get() = when (lang) {
        AppLanguage.CHINESE -> "靛蓝,朱红,米白,墨黑"
        AppLanguage.ENGLISH -> "Indigo,Vermillion,Cream white,Ink black"
        AppLanguage.ARABIC -> "نيلي,قرمزي,أبيض كريمي,أسود حبري"
    }

    val elementsHarryPotter: String get() = when (lang) {
        AppLanguage.CHINESE -> "盾徽,羽毛笔,蜡封,哥特字体"
        AppLanguage.ENGLISH -> "Coat of arms,Quill pen,Wax seal,Gothic font"
        AppLanguage.ARABIC -> "شعار درع,ريشة كتابة,ختم شمع,خط قوطي"
    }

    val elementsGhibli: String get() = when (lang) {
        AppLanguage.CHINESE -> "云朵,绿植,小屋,柔和光影"
        AppLanguage.ENGLISH -> "Clouds,Greenery,Cottage,Soft lighting"
        AppLanguage.ARABIC -> "غيوم,نباتات خضراء,كوخ,إضاءة ناعمة"
    }

    val elementsYourName: String get() = when (lang) {
        AppLanguage.CHINESE -> "光斑,彗星,黄昏,细腻光影"
        AppLanguage.ENGLISH -> "Light spots,Comet,Twilight,Delicate lighting"
        AppLanguage.ARABIC -> "بقع ضوء,مذنب,شفق,إضاءة دقيقة"
    }

    val elementsApple: String get() = when (lang) {
        AppLanguage.CHINESE -> "大量留白,精确对齐,微妙渐变,圆角"
        AppLanguage.ENGLISH -> "Generous whitespace,Precise alignment,Subtle gradients,Rounded corners"
        AppLanguage.ARABIC -> "مساحات بيضاء,محاذاة دقيقة,تدرجات خفيفة,زوايا مستديرة"
    }

    val elementsLittlePrince: String get() = when (lang) {
        AppLanguage.CHINESE -> "星星,玫瑰,狐狸,小行星"
        AppLanguage.ENGLISH -> "Stars,Rose,Fox,Asteroid"
        AppLanguage.ARABIC -> "نجوم,وردة,ثعلب,كويكب"
    }

    val elementsZelda: String get() = when (lang) {
        AppLanguage.CHINESE -> "希卡符文,远景山脉,卡通渲染"
        AppLanguage.ENGLISH -> "Sheikah runes,Distant mountains,Cel-shading"
        AppLanguage.ARABIC -> "رموز شيكا,جبال بعيدة,تظليل كرتوني"
    }

    val elementsArtDeco: String get() = when (lang) {
        AppLanguage.CHINESE -> "几何图案,对称布局,扇形,金属线条"
        AppLanguage.ENGLISH -> "Geometric patterns,Symmetrical layout,Fan shapes,Metallic lines"
        AppLanguage.ARABIC -> "أنماط هندسية,تخطيط متماثل,أشكال مروحية,خطوط معدنية"
    }

    val elementsJapanese: String get() = when (lang) {
        AppLanguage.CHINESE -> "波浪,樱花,和纹,毛笔字体"
        AppLanguage.ENGLISH -> "Waves,Cherry blossoms,Japanese patterns,Brush font"
        AppLanguage.ARABIC -> "موجات,أزهار الكرز,أنماط يابانية,خط فرشاة"
    }

    val requestTimeoutRetry: String get() = when (lang) {
        AppLanguage.CHINESE -> "请求超时，请检查网络连接后重试"
        AppLanguage.ENGLISH -> "Request timed out, please check network connection and retry"
        AppLanguage.ARABIC -> "انتهت مهلة الطلب، يرجى التحقق من الاتصال بالشبكة وإعادة المحاولة"
    }

    val errorOccurredPrefix: String get() = when (lang) {
        AppLanguage.CHINESE -> "发生错误: %s"
        AppLanguage.ENGLISH -> "Error occurred: %s"
        AppLanguage.ARABIC -> "حدث خطأ: %s"
    }

    val streamResponseIncomplete: String get() = when (lang) {
        AppLanguage.CHINESE -> "流式响应未正常完成"
        AppLanguage.ENGLISH -> "Stream response did not complete normally"
        AppLanguage.ARABIC -> "لم تكتمل استجابة البث بشكل طبيعي"
    }
}
