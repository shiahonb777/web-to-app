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
}
