package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object ProjectStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage
    // ==================== Node.js Strings ====================
    val njsCreateTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建 Node.js 应用"
        AppLanguage.ENGLISH -> "Create Node.js App"
        AppLanguage.ARABIC -> "إنشاء تطبيق Node.js"
    }

    val njsBasicConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "基本配置"
        AppLanguage.ENGLISH -> "Basic Configuration"
        AppLanguage.ARABIC -> "الإعدادات الأساسية"
    }

    val njsSelectProjectFolder: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择项目文件夹"
        AppLanguage.ENGLISH -> "Select Project Folder"
        AppLanguage.ARABIC -> "اختر مجلد المشروع"
    }

    val njsSelectProjectDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择包含 package.json 的 Node.js 项目目录"
        AppLanguage.ENGLISH -> "Select a Node.js project directory containing package.json"
        AppLanguage.ARABIC -> "حدد مجلد مشروع Node.js يحتوي على package.json"
    }

    val njsBuildMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "构建模式"
        AppLanguage.ENGLISH -> "Build Mode"
        AppLanguage.ARABIC -> "وضع البناء"
    }

    val njsModeStatic: String get() = when (lang) {
        AppLanguage.CHINESE -> "静态前端"
        AppLanguage.ENGLISH -> "Static Frontend"
        AppLanguage.ARABIC -> "الواجهة الأمامية الثابتة"
    }

    val njsModeStaticDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "打包构建产物（dist 目录）为本地应用"
        AppLanguage.ENGLISH -> "Package build output (dist) as local app"
        AppLanguage.ARABIC -> "تغليف مخرجات البناء (dist) كتطبيق محلي"
    }

    val njsModeBackend: String get() = when (lang) {
        AppLanguage.CHINESE -> "API 后端"
        AppLanguage.ENGLISH -> "API Backend"
        AppLanguage.ARABIC -> "الخلفية API"
    }

    val njsModeBackendDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行 Node.js 服务器（Express/Fastify/Koa 等）"
        AppLanguage.ENGLISH -> "Run Node.js server (Express/Fastify/Koa etc.)"
        AppLanguage.ARABIC -> "تشغيل خادم Node.js (Express/Fastify/Koa إلخ)"
    }

    val njsModeFullstack: String get() = when (lang) {
        AppLanguage.CHINESE -> "全栈应用"
        AppLanguage.ENGLISH -> "Fullstack App"
        AppLanguage.ARABIC -> "تطبيق كامل"
    }

    val njsModeFullstackDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "前端 + API 服务器（Next.js/Nuxt.js 等）"
        AppLanguage.ENGLISH -> "Frontend + API server (Next.js/Nuxt.js etc.)"
        AppLanguage.ARABIC -> "الواجهة + خادم API (Next.js/Nuxt.js إلخ)"
    }

    val njsEntryFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "入口文件"
        AppLanguage.ENGLISH -> "Entry File"
        AppLanguage.ARABIC -> "ملف الإدخال"
    }

    val njsEntryFileHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "如 server.js, index.js, app.js"
        AppLanguage.ENGLISH -> "e.g. server.js, index.js, app.js"
        AppLanguage.ARABIC -> "مثل server.js, index.js, app.js"
    }

    val njsEnvVars: String get() = when (lang) {
        AppLanguage.CHINESE -> "环境变量"
        AppLanguage.ENGLISH -> "Environment Variables"
        AppLanguage.ARABIC -> "متغيرات البيئة"
    }

    val njsAddEnvVar: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加环境变量"
        AppLanguage.ENGLISH -> "Add Env Variable"
        AppLanguage.ARABIC -> "إضافة متغير بيئة"
    }

    val njsDownloadDeps: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载 Node.js 依赖"
        AppLanguage.ENGLISH -> "Download Node.js Dependencies"
        AppLanguage.ARABIC -> "تنزيل متطلبات Node.js"
    }

    val njsDownloading: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在下载 Node.js 运行时..."
        AppLanguage.ENGLISH -> "Downloading Node.js runtime..."
        AppLanguage.ARABIC -> "جاري تنزيل Node.js..."
    }

    val njsDownloadComplete: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js 运行时已就绪"
        AppLanguage.ENGLISH -> "Node.js runtime ready"
        AppLanguage.ARABIC -> "Node.js جاهز"
    }

    val njsDownloadFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js 下载失败"
        AppLanguage.ENGLISH -> "Node.js download failed"
        AppLanguage.ARABIC -> "فشل تنزيل Node.js"
    }

    val njsProjectDetected: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测到 Node.js 项目"
        AppLanguage.ENGLISH -> "Node.js project detected"
        AppLanguage.ARABIC -> "تم اكتشاف مشروع Node.js"
    }

    val njsFramework: String get() = when (lang) {
        AppLanguage.CHINESE -> "后端框架"
        AppLanguage.ENGLISH -> "Backend Framework"
        AppLanguage.ARABIC -> "إطار الخلفية"
    }

    val njsProjectReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js 项目已就绪"
        AppLanguage.ENGLISH -> "Node.js project ready"
        AppLanguage.ARABIC -> "مشروع Node.js جاهز"
    }

    val njsLandscapeMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "横屏模式"
        AppLanguage.ENGLISH -> "Landscape Mode"
        AppLanguage.ARABIC -> "وضع أفقي"
    }
    // ==================== Media App Page ====================
    val selectMediaType: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择媒体类型"
        AppLanguage.ENGLISH -> "Select Media Type"
        AppLanguage.ARABIC -> "اختيار نوع الوسائط"
    }

    val image: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片"
        AppLanguage.ENGLISH -> "Image"
        AppLanguage.ARABIC -> "صورة"
    }

    val video: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频"
        AppLanguage.ENGLISH -> "Video"
        AppLanguage.ARABIC -> "فيديو"
    }

    val selectImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择图片"
        AppLanguage.ENGLISH -> "Select Image"
        AppLanguage.ARABIC -> "اختيار صورة"
    }

    val selectVideo: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择视频"
        AppLanguage.ENGLISH -> "Select Video"
        AppLanguage.ARABIC -> "اختيار فيديو"
    }

    val clickToSelectImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击选择图片"
        AppLanguage.ENGLISH -> "Click to select image"
        AppLanguage.ARABIC -> "انقر لاختيار صورة"
    }

    val clickToSelectVideo: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击选择视频"
        AppLanguage.ENGLISH -> "Click to select video"
        AppLanguage.ARABIC -> "انقر لاختيار فيديو"
    }

    val videoSelected: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频已选择"
        AppLanguage.ENGLISH -> "Video selected"
        AppLanguage.ARABIC -> "تم اختيار الفيديو"
    }

    val fillScreen: String get() = when (lang) {
        AppLanguage.CHINESE -> "铺满屏幕"
        AppLanguage.ENGLISH -> "Fill Screen"
        AppLanguage.ARABIC -> "ملء الشاشة"
    }

    val fillScreenHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动裁剪以填满整个屏幕"
        AppLanguage.ENGLISH -> "Auto crop to fill entire screen"
        AppLanguage.ARABIC -> "قص تلقائي لملء الشاشة بالكامل"
    }

    val landscapeMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "横屏显示"
        AppLanguage.ENGLISH -> "Landscape Mode"
        AppLanguage.ARABIC -> "الوضع الأفقي"
    }

    val landscapeModeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "以横屏模式显示内容"
        AppLanguage.ENGLISH -> "Display content in landscape mode"
        AppLanguage.ARABIC -> "عرض المحتوى في الوضع الأفقي"
    }

    val enableAudio: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用音频"
        AppLanguage.ENGLISH -> "Enable Audio"
        AppLanguage.ARABIC -> "تفعيل الصوت"
    }

    val enableAudioHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放视频时包含声音"
        AppLanguage.ENGLISH -> "Include sound when playing video"
        AppLanguage.ARABIC -> "تضمين الصوت عند تشغيل الفيديو"
    }

    val loopPlay: String get() = when (lang) {
        AppLanguage.CHINESE -> "循环播放"
        AppLanguage.ENGLISH -> "Loop Play"
        AppLanguage.ARABIC -> "تشغيل متكرر"
    }

    val loopPlayHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频结束后自动重新播放"
        AppLanguage.ENGLISH -> "Auto replay when video ends"
        AppLanguage.ARABIC -> "إعادة التشغيل تلقائيًا عند انتهاء الفيديو"
    }

    val autoPlay: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动播放"
        AppLanguage.ENGLISH -> "Auto Play"
        AppLanguage.ARABIC -> "تشغيل تلقائي"
    }

    val autoPlayHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "打开应用时自动开始播放"
        AppLanguage.ENGLISH -> "Auto start playing when app opens"
        AppLanguage.ARABIC -> "بدء التشغيل تلقائيًا عند فتح التطبيق"
    }

    val mediaAppHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建的应用将%s，适合用作数字相框、广告展示或视频壁纸。"
        AppLanguage.ENGLISH -> "The created app will %s, suitable for digital photo frames, advertising displays, or video wallpapers."
        AppLanguage.ARABIC -> "سيقوم التطبيق المُنشأ بـ %s، مناسب للإطارات الرقمية أو عروض الإعلانات أو خلفيات الفيديو."
    }

    val fullscreenDisplayImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "全屏显示您选择的图片"
        AppLanguage.ENGLISH -> "display your selected image in fullscreen"
        AppLanguage.ARABIC -> "عرض الصورة المختارة بملء الشاشة"
    }

    val fullscreenPlayVideo: String get() = when (lang) {
        AppLanguage.CHINESE -> "全屏播放您选择的视频"
        AppLanguage.ENGLISH -> "play your selected video in fullscreen"
        AppLanguage.ARABIC -> "تشغيل الفيديو المختار بملء الشاشة"
    }
    // ==================== ZIP Import ====================
    val zipImportMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "ZIP 导入"
        AppLanguage.ENGLISH -> "ZIP Import"
        AppLanguage.ARABIC -> "استيراد ZIP"
    }

    val manualSelectMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "手动选择"
        AppLanguage.ENGLISH -> "Manual Select"
        AppLanguage.ARABIC -> "اختيار يدوي"
    }

    val selectZipFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择 ZIP 文件"
        AppLanguage.ENGLISH -> "Select ZIP File"
        AppLanguage.ARABIC -> "اختيار ملف ZIP"
    }

    val selectZipHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择包含 HTML 项目的 ZIP 文件，自动解压并配置所有资源"
        AppLanguage.ENGLISH -> "Select a ZIP file containing your HTML project, auto-extract and configure all resources"
        AppLanguage.ARABIC -> "اختر ملف ZIP يحتوي على مشروع HTML، سيتم استخراج وتكوين جميع الموارد تلقائيًا"
    }

    val zipImporting: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在解压并分析项目..."
        AppLanguage.ENGLISH -> "Extracting and analyzing project..."
        AppLanguage.ARABIC -> "جاري استخراج وتحليل المشروع..."
    }

    val zipImportSuccess: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目导入成功"
        AppLanguage.ENGLISH -> "Project imported successfully"
        AppLanguage.ARABIC -> "تم استيراد المشروع بنجاح"
    }

    val zipImportFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "ZIP 导入失败: %s"
        AppLanguage.ENGLISH -> "ZIP import failed: %s"
        AppLanguage.ARABIC -> "فشل استيراد ZIP: %s"
    }

    val zipProjectAnalysis: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目分析"
        AppLanguage.ENGLISH -> "Project Analysis"
        AppLanguage.ARABIC -> "تحليل المشروع"
    }

    val zipEntryFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "入口文件"
        AppLanguage.ENGLISH -> "Entry File"
        AppLanguage.ARABIC -> "ملف الدخول"
    }

    val zipResourceStats: String get() = when (lang) {
        AppLanguage.CHINESE -> "资源统计"
        AppLanguage.ENGLISH -> "Resource Stats"
        AppLanguage.ARABIC -> "إحصائيات الموارد"
    }

    val zipTotalFiles: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个文件"
        AppLanguage.ENGLISH -> "%d files"
        AppLanguage.ARABIC -> "%d ملفات"
    }

    val zipTotalSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "总大小: %s"
        AppLanguage.ENGLISH -> "Total size: %s"
        AppLanguage.ARABIC -> "الحجم الإجمالي: %s"
    }

    val zipChangeEntry: String get() = when (lang) {
        AppLanguage.CHINESE -> "更换入口文件"
        AppLanguage.ENGLISH -> "Change Entry File"
        AppLanguage.ARABIC -> "تغيير ملف الدخول"
    }

    val zipReimport: String get() = when (lang) {
        AppLanguage.CHINESE -> "重新导入"
        AppLanguage.ENGLISH -> "Re-import"
        AppLanguage.ARABIC -> "إعادة الاستيراد"
    }

    val zipTip: String get() = when (lang) {
        AppLanguage.CHINESE -> "提示：ZIP 项目会保留完整目录结构，所有相对路径引用（CSS/JS/图片/音视频/字体等）自动生效，无需手动配置。"
        AppLanguage.ENGLISH -> "Tip: ZIP projects preserve the full directory structure. All relative path references (CSS/JS/images/media/fonts etc.) work automatically."
        AppLanguage.ARABIC -> "تلميح: تحتفظ مشاريع ZIP ببنية المجلدات الكاملة. تعمل جميع المراجع ذات المسارات النسبية (CSS/JS/صور/وسائط/خطوط إلخ) تلقائيًا."
    }

    val zipNoHtmlWarning: String get() = when (lang) {
        AppLanguage.CHINESE -> "ZIP 中未找到 HTML 文件，请确认文件内容"
        AppLanguage.ENGLISH -> "No HTML files found in ZIP, please verify contents"
        AppLanguage.ARABIC -> "لم يتم العثور على ملفات HTML في ZIP، يرجى التحقق من المحتويات"
    }

    val zipSelectEntryTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择入口文件"
        AppLanguage.ENGLISH -> "Select Entry File"
        AppLanguage.ARABIC -> "اختيار ملف الدخول"
    }

    val zipFileTreeTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "文件列表"
        AppLanguage.ENGLISH -> "File List"
        AppLanguage.ARABIC -> "قائمة الملفات"
    }
    // ==================== PHP Enhanced Strings ====================
    val phpHeroTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP 应用"
        AppLanguage.ENGLISH -> "PHP Application"
        AppLanguage.ARABIC -> "تطبيق PHP"
    }

    val phpHeroDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将 PHP 项目打包为独立 Android 应用"
        AppLanguage.ENGLISH -> "Package PHP project as standalone Android app"
        AppLanguage.ARABIC -> "تغليف مشروع PHP كتطبيق Android مستقل"
    }

    val phpComposerDeps: String get() = when (lang) {
        AppLanguage.CHINESE -> "Composer 依赖"
        AppLanguage.ENGLISH -> "Composer Dependencies"
        AppLanguage.ARABIC -> "متطلبات Composer"
    }

    val phpRequireDeps: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行依赖"
        AppLanguage.ENGLISH -> "Dependencies"
        AppLanguage.ARABIC -> "التبعيات"
    }

    val phpRequireDevDeps: String get() = when (lang) {
        AppLanguage.CHINESE -> "开发依赖"
        AppLanguage.ENGLISH -> "Dev Dependencies"
        AppLanguage.ARABIC -> "تبعيات التطوير"
    }

    val phpDocRootSelect: String get() = when (lang) {
        AppLanguage.CHINESE -> "Web 根目录选择"
        AppLanguage.ENGLISH -> "Document Root Selection"
        AppLanguage.ARABIC -> "اختيار جذر المستند"
    }

    val phpDocRootHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "Web 服务器的根目录，通常为 public/ 或项目根目录"
        AppLanguage.ENGLISH -> "Web server root directory, usually public/ or project root"
        AppLanguage.ARABIC -> "دليل جذر خادم الويب، عادة public/ أو جذر المشروع"
    }

    val phpExtensions: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP 扩展"
        AppLanguage.ENGLISH -> "PHP Extensions"
        AppLanguage.ARABIC -> "إضافات PHP"
    }

    val phpExtensionsHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用项目所需的 PHP 扩展模块"
        AppLanguage.ENGLISH -> "Enable PHP extensions required by your project"
        AppLanguage.ARABIC -> "تمكين إضافات PHP المطلوبة لمشروعك"
    }

    val phpDatabaseConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据库配置"
        AppLanguage.ENGLISH -> "Database Configuration"
        AppLanguage.ARABIC -> "إعدادات قاعدة البيانات"
    }

    val phpSqlitePath: String get() = when (lang) {
        AppLanguage.CHINESE -> "SQLite 数据库路径"
        AppLanguage.ENGLISH -> "SQLite Database Path"
        AppLanguage.ARABIC -> "مسار قاعدة بيانات SQLite"
    }

    val phpDbDetected: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测到数据库文件"
        AppLanguage.ENGLISH -> "Database file detected"
        AppLanguage.ARABIC -> "تم اكتشاف ملف قاعدة البيانات"
    }

    val phpFrameworkTip: String get() = when (lang) {
        AppLanguage.CHINESE -> "框架提示"
        AppLanguage.ENGLISH -> "Framework Tips"
        AppLanguage.ARABIC -> "نصائح الإطار"
    }

    val phpLaravelTip: String get() = when (lang) {
        AppLanguage.CHINESE -> "Laravel 项目：已自动配置 storage/、bootstrap/cache/ 目录权限，APP_KEY 将自动生成"
        AppLanguage.ENGLISH -> "Laravel: storage/ and bootstrap/cache/ permissions auto-configured. APP_KEY will be auto-generated."
        AppLanguage.ARABIC -> "Laravel: تم تكوين أذونات storage/ و bootstrap/cache/ تلقائياً. سيتم إنشاء APP_KEY تلقائياً."
    }

    val phpThinkPhpTip: String get() = when (lang) {
        AppLanguage.CHINESE -> "ThinkPHP 项目：runtime/ 目录已自动配置，支持多应用模式"
        AppLanguage.ENGLISH -> "ThinkPHP: runtime/ directory auto-configured. Multi-app mode supported."
        AppLanguage.ARABIC -> "ThinkPHP: تم تكوين دليل runtime/ تلقائياً. وضع التطبيقات المتعددة مدعوم."
    }

    val phpCodeIgniterTip: String get() = when (lang) {
        AppLanguage.CHINESE -> "CodeIgniter 项目：writable/ 目录已自动配置，环境已设置为 production"
        AppLanguage.ENGLISH -> "CodeIgniter: writable/ directory auto-configured. Environment set to production."
        AppLanguage.ARABIC -> "CodeIgniter: تم تكوين دليل writable/ تلقائياً. تم ضبط البيئة على production."
    }

    val phpSlimTip: String get() = when (lang) {
        AppLanguage.CHINESE -> "Slim 项目：已检测到路由配置，支持 PSR-7 中间件"
        AppLanguage.ENGLISH -> "Slim: Routes detected. PSR-7 middleware supported."
        AppLanguage.ARABIC -> "Slim: تم اكتشاف المسارات. وسيط PSR-7 مدعوم."
    }

    val phpAdvancedConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "高级配置"
        AppLanguage.ENGLISH -> "Advanced Configuration"
        AppLanguage.ARABIC -> "إعدادات متقدمة"
    }

    val phpProjectRoot: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目根目录"
        AppLanguage.ENGLISH -> "Project Root"
        AppLanguage.ARABIC -> "جذر المشروع"
    }

    val phpVersion: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP 版本"
        AppLanguage.ENGLISH -> "PHP Version"
        AppLanguage.ARABIC -> "إصدار PHP"
    }

    val phpDetectedDirs: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测到的 Web 目录"
        AppLanguage.ENGLISH -> "Detected Web Directories"
        AppLanguage.ARABIC -> "أدلة الويب المكتشفة"
    }

    val phpCustomPath: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义路径"
        AppLanguage.ENGLISH -> "Custom Path"
        AppLanguage.ARABIC -> "مسار مخصص"
    }
    // ==================== Python Enhanced Strings ====================
    val pyHeroTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "Python 应用"
        AppLanguage.ENGLISH -> "Python Application"
        AppLanguage.ARABIC -> "تطبيق Python"
    }

    val pyHeroDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将 Python Web 项目打包为独立 Android 应用"
        AppLanguage.ENGLISH -> "Package Python Web project as standalone Android app"
        AppLanguage.ARABIC -> "تغليف مشروع Python Web كتطبيق Android مستقل"
    }

    val pyRequirements: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目依赖"
        AppLanguage.ENGLISH -> "Project Dependencies"
        AppLanguage.ARABIC -> "تبعيات المشروع"
    }

    val pyRequirementsFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "依赖来源"
        AppLanguage.ENGLISH -> "Dependency Source"
        AppLanguage.ARABIC -> "مصدر التبعيات"
    }

    val pyServerBuiltin: String get() = when (lang) {
        AppLanguage.CHINESE -> "内置服务器"
        AppLanguage.ENGLISH -> "Built-in Server"
        AppLanguage.ARABIC -> "الخادم المدمج"
    }

    val pyServerBuiltinDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用框架自带的开发服务器"
        AppLanguage.ENGLISH -> "Use framework's built-in development server"
        AppLanguage.ARABIC -> "استخدام خادم التطوير المدمج في الإطار"
    }

    val pyServerGunicorn: String get() = when (lang) {
        AppLanguage.CHINESE -> "Gunicorn (WSGI)"
        AppLanguage.ENGLISH -> "Gunicorn (WSGI)"
        AppLanguage.ARABIC -> "Gunicorn (WSGI)"
    }

    val pyServerGunicornDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "适用于 Flask、Django 等 WSGI 框架"
        AppLanguage.ENGLISH -> "For Flask, Django and other WSGI frameworks"
        AppLanguage.ARABIC -> "لأطر WSGI مثل Flask و Django"
    }

    val pyServerUvicorn: String get() = when (lang) {
        AppLanguage.CHINESE -> "Uvicorn (ASGI)"
        AppLanguage.ENGLISH -> "Uvicorn (ASGI)"
        AppLanguage.ARABIC -> "Uvicorn (ASGI)"
    }

    val pyServerUvicornDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "适用于 FastAPI、Starlette 等 ASGI 框架"
        AppLanguage.ENGLISH -> "For FastAPI, Starlette and other ASGI frameworks"
        AppLanguage.ARABIC -> "لأطر ASGI مثل FastAPI و Starlette"
    }

    val pyRecommended: String get() = when (lang) {
        AppLanguage.CHINESE -> "推荐"
        AppLanguage.ENGLISH -> "Recommended"
        AppLanguage.ARABIC -> "موصى به"
    }

    val pyWsgiModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "WSGI/ASGI 模块"
        AppLanguage.ENGLISH -> "WSGI/ASGI Module"
        AppLanguage.ARABIC -> "وحدة WSGI/ASGI"
    }

    val pyWsgiModuleHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "如 myapp.wsgi:application 或 main:app"
        AppLanguage.ENGLISH -> "e.g. myapp.wsgi:application or main:app"
        AppLanguage.ARABIC -> "مثل myapp.wsgi:application أو main:app"
    }

    val pyVenvDetected: String get() = when (lang) {
        AppLanguage.CHINESE -> "虚拟环境"
        AppLanguage.ENGLISH -> "Virtual Environment"
        AppLanguage.ARABIC -> "البيئة الافتراضية"
    }

    val pyVenvFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "已检测到虚拟环境"
        AppLanguage.ENGLISH -> "Virtual environment detected"
        AppLanguage.ARABIC -> "تم اكتشاف البيئة الافتراضية"
    }

    val pyVenvNotFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "未检测到虚拟环境"
        AppLanguage.ENGLISH -> "No virtual environment detected"
        AppLanguage.ARABIC -> "لم يتم اكتشاف بيئة افتراضية"
    }

    val pyDjangoSettings: String get() = when (lang) {
        AppLanguage.CHINESE -> "Django 配置"
        AppLanguage.ENGLISH -> "Django Settings"
        AppLanguage.ARABIC -> "إعدادات Django"
    }

    val pyDjangoSettingsModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "Settings 模块"
        AppLanguage.ENGLISH -> "Settings Module"
        AppLanguage.ARABIC -> "وحدة الإعدادات"
    }

    val pyDjangoStaticDir: String get() = when (lang) {
        AppLanguage.CHINESE -> "静态文件目录"
        AppLanguage.ENGLISH -> "Static Files Directory"
        AppLanguage.ARABIC -> "دليل الملفات الثابتة"
    }

    val pyDjangoAllowedHosts: String get() = when (lang) {
        AppLanguage.CHINESE -> "提示：ALLOWED_HOSTS 已自动设置为 ['*']"
        AppLanguage.ENGLISH -> "Tip: ALLOWED_HOSTS is auto-set to ['*']"
        AppLanguage.ARABIC -> "تلميح: تم ضبط ALLOWED_HOSTS تلقائياً على ['*']"
    }

    val pyFastapiConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "FastAPI 配置"
        AppLanguage.ENGLISH -> "FastAPI Settings"
        AppLanguage.ARABIC -> "إعدادات FastAPI"
    }

    val pyFastapiDocsEndpoint: String get() = when (lang) {
        AppLanguage.CHINESE -> "API 文档端点：/docs (Swagger) · /redoc (ReDoc)"
        AppLanguage.ENGLISH -> "API docs endpoints: /docs (Swagger) · /redoc (ReDoc)"
        AppLanguage.ARABIC -> "نقاط نهاية وثائق API: /docs (Swagger) · /redoc (ReDoc)"
    }

    val pyFastapiAsgiHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "推荐使用 Uvicorn 作为 ASGI 服务器"
        AppLanguage.ENGLISH -> "Uvicorn recommended as ASGI server"
        AppLanguage.ARABIC -> "يوصى باستخدام Uvicorn كخادم ASGI"
    }
    // ==================== DocsSite Enhanced Strings ====================
    val docsHeroTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "文档站点"
        AppLanguage.ENGLISH -> "Documentation Site"
        AppLanguage.ARABIC -> "موقع التوثيق"
    }

    val docsHeroDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将静态文档站点打包为独立 Android 应用"
        AppLanguage.ENGLISH -> "Package static documentation site as standalone Android app"
        AppLanguage.ARABIC -> "تغليف موقع التوثيق الثابت كتطبيق Android مستقل"
    }

    val docsStructure: String get() = when (lang) {
        AppLanguage.CHINESE -> "文档结构"
        AppLanguage.ENGLISH -> "Document Structure"
        AppLanguage.ARABIC -> "هيكل التوثيق"
    }

    val docsSearchEngine: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索引擎"
        AppLanguage.ENGLISH -> "Search Engine"
        AppLanguage.ARABIC -> "محرك البحث"
    }

    val docsSearchLocal: String get() = when (lang) {
        AppLanguage.CHINESE -> "本地搜索"
        AppLanguage.ENGLISH -> "Local Search"
        AppLanguage.ARABIC -> "البحث المحلي"
    }

    val docsSearchLocalDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用内置的全文搜索索引"
        AppLanguage.ENGLISH -> "Use built-in full-text search index"
        AppLanguage.ARABIC -> "استخدام فهرس البحث المحلي المدمج"
    }

    val docsSearchAlgolia: String get() = when (lang) {
        AppLanguage.CHINESE -> "Algolia DocSearch"
        AppLanguage.ENGLISH -> "Algolia DocSearch"
        AppLanguage.ARABIC -> "Algolia DocSearch"
    }

    val docsSearchAlgoliaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "需要网络连接和 API Key"
        AppLanguage.ENGLISH -> "Requires internet and API Key"
        AppLanguage.ARABIC -> "يتطلب اتصال بالإنترنت ومفتاح API"
    }

    val docsSearchDisabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "禁用搜索"
        AppLanguage.ENGLISH -> "Disable Search"
        AppLanguage.ARABIC -> "تعطيل البحث"
    }

    val docsThemeConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "主题配置"
        AppLanguage.ENGLISH -> "Theme Configuration"
        AppLanguage.ARABIC -> "إعدادات السمة"
    }

    val docsThemeLight: String get() = when (lang) {
        AppLanguage.CHINESE -> "浅色"
        AppLanguage.ENGLISH -> "Light"
        AppLanguage.ARABIC -> "فاتح"
    }

    val docsThemeDark: String get() = when (lang) {
        AppLanguage.CHINESE -> "深色"
        AppLanguage.ENGLISH -> "Dark"
        AppLanguage.ARABIC -> "داكن"
    }

    val docsThemeAuto: String get() = when (lang) {
        AppLanguage.CHINESE -> "跟随系统"
        AppLanguage.ENGLISH -> "Auto"
        AppLanguage.ARABIC -> "تلقائي"
    }

    val docsBasePath: String get() = when (lang) {
        AppLanguage.CHINESE -> "基础路径"
        AppLanguage.ENGLISH -> "Base Path"
        AppLanguage.ARABIC -> "المسار الأساسي"
    }

    val docsPwaConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "离线访问 (PWA)"
        AppLanguage.ENGLISH -> "Offline Access (PWA)"
        AppLanguage.ARABIC -> "الوصول دون اتصال (PWA)"
    }

    val docsPwaHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用 Service Worker 实现离线浏览"
        AppLanguage.ENGLISH -> "Enable Service Worker for offline browsing"
        AppLanguage.ARABIC -> "تمكين Service Worker للتصفح دون اتصال"
    }

    val docsPages: String get() = when (lang) {
        AppLanguage.CHINESE -> "个页面"
        AppLanguage.ENGLISH -> " pages"
        AppLanguage.ARABIC -> " صفحات"
    }

    val docsConfigFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "配置文件"
        AppLanguage.ENGLISH -> "Config File"
        AppLanguage.ARABIC -> "ملف الإعداد"
    }
    // ==================== WordPress Enhanced Strings ====================
    val wpHeroTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress 应用"
        AppLanguage.ENGLISH -> "WordPress Application"
        AppLanguage.ARABIC -> "تطبيق WordPress"
    }

    val wpHeroDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "运行离线 WordPress 站点，无需服务器"
        AppLanguage.ENGLISH -> "Run offline WordPress site, no server needed"
        AppLanguage.ARABIC -> "تشغيل موقع WordPress دون اتصال، بدون خادم"
    }

    val wpThemePanel: String get() = when (lang) {
        AppLanguage.CHINESE -> "主题管理"
        AppLanguage.ENGLISH -> "Theme Management"
        AppLanguage.ARABIC -> "إدارة السمات"
    }

    val wpActiveTheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "当前主题"
        AppLanguage.ENGLISH -> "Active Theme"
        AppLanguage.ARABIC -> "السمة النشطة"
    }

    val wpInstalledThemes: String get() = when (lang) {
        AppLanguage.CHINESE -> "已安装主题"
        AppLanguage.ENGLISH -> "Installed Themes"
        AppLanguage.ARABIC -> "السمات المثبتة"
    }

    val wpPluginPanel: String get() = when (lang) {
        AppLanguage.CHINESE -> "插件管理"
        AppLanguage.ENGLISH -> "Plugin Management"
        AppLanguage.ARABIC -> "إدارة الإضافات"
    }

    val wpInstalledPlugins: String get() = when (lang) {
        AppLanguage.CHINESE -> "已安装插件"
        AppLanguage.ENGLISH -> "Installed Plugins"
        AppLanguage.ARABIC -> "الإضافات المثبتة"
    }

    val wpAdminConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "管理员配置"
        AppLanguage.ENGLISH -> "Admin Configuration"
        AppLanguage.ARABIC -> "إعدادات المسؤول"
    }

    val wpAdminEmail: String get() = when (lang) {
        AppLanguage.CHINESE -> "管理员邮箱"
        AppLanguage.ENGLISH -> "Admin Email"
        AppLanguage.ARABIC -> "بريد المسؤول"
    }

    val wpAdminPassword: String get() = when (lang) {
        AppLanguage.CHINESE -> "管理员密码"
        AppLanguage.ENGLISH -> "Admin Password"
        AppLanguage.ARABIC -> "كلمة مرور المسؤول"
    }

    val wpPermalink: String get() = when (lang) {
        AppLanguage.CHINESE -> "固定链接结构"
        AppLanguage.ENGLISH -> "Permalink Structure"
        AppLanguage.ARABIC -> "هيكل الروابط الدائمة"
    }

    val wpPermalinkPlain: String get() = when (lang) {
        AppLanguage.CHINESE -> "朴素 (?p=123)"
        AppLanguage.ENGLISH -> "Plain (?p=123)"
        AppLanguage.ARABIC -> "عادي (?p=123)"
    }

    val wpPermalinkPostName: String get() = when (lang) {
        AppLanguage.CHINESE -> "文章名 (/post-name/)"
        AppLanguage.ENGLISH -> "Post name (/post-name/)"
        AppLanguage.ARABIC -> "اسم المنشور (/post-name/)"
    }

    val wpPermalinkNumeric: String get() = when (lang) {
        AppLanguage.CHINESE -> "数字型 (/archives/123)"
        AppLanguage.ENGLISH -> "Numeric (/archives/123)"
        AppLanguage.ARABIC -> "رقمي (/archives/123)"
    }

    val wpDbInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据库信息"
        AppLanguage.ENGLISH -> "Database Info"
        AppLanguage.ARABIC -> "معلومات قاعدة البيانات"
    }

    val wpDbType: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据库类型：SQLite（离线）"
        AppLanguage.ENGLISH -> "Database type: SQLite (offline)"
        AppLanguage.ARABIC -> "نوع قاعدة البيانات: SQLite (غير متصل)"
    }

    val wpSiteLanguage: String get() = when (lang) {
        AppLanguage.CHINESE -> "站点语言"
        AppLanguage.ENGLISH -> "Site Language"
        AppLanguage.ARABIC -> "لغة الموقع"
    }

    val wpVersionInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress 版本"
        AppLanguage.ENGLISH -> "WordPress Version"
        AppLanguage.ARABIC -> "إصدار WordPress"
    }

    val wpNoPlugins: String get() = when (lang) {
        AppLanguage.CHINESE -> "无已安装插件"
        AppLanguage.ENGLISH -> "No plugins installed"
        AppLanguage.ARABIC -> "لا توجد إضافات مثبتة"
    }

    val wpNoThemes: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用默认主题"
        AppLanguage.ENGLISH -> "Using default theme"
        AppLanguage.ARABIC -> "استخدام السمة الافتراضية"
    }
    // ==================== Media Enhanced Strings ====================
    val mediaImageInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片信息"
        AppLanguage.ENGLISH -> "Image Info"
        AppLanguage.ARABIC -> "معلومات الصورة"
    }

    val mediaVideoInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频信息"
        AppLanguage.ENGLISH -> "Video Info"
        AppLanguage.ARABIC -> "معلومات الفيديو"
    }

    val mediaDimensions: String get() = when (lang) {
        AppLanguage.CHINESE -> "尺寸"
        AppLanguage.ENGLISH -> "Dimensions"
        AppLanguage.ARABIC -> "الأبعاد"
    }

    val mediaFileSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "文件大小"
        AppLanguage.ENGLISH -> "File Size"
        AppLanguage.ARABIC -> "حجم الملف"
    }

    val mediaFormat: String get() = when (lang) {
        AppLanguage.CHINESE -> "格式"
        AppLanguage.ENGLISH -> "Format"
        AppLanguage.ARABIC -> "التنسيق"
    }

    val mediaDuration: String get() = when (lang) {
        AppLanguage.CHINESE -> "时长"
        AppLanguage.ENGLISH -> "Duration"
        AppLanguage.ARABIC -> "المدة"
    }

    val mediaResolution: String get() = when (lang) {
        AppLanguage.CHINESE -> "分辨率"
        AppLanguage.ENGLISH -> "Resolution"
        AppLanguage.ARABIC -> "الدقة"
    }

    val mediaPlaybackSpeed: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放速度"
        AppLanguage.ENGLISH -> "Playback Speed"
        AppLanguage.ARABIC -> "سرعة التشغيل"
    }

    val mediaBackgroundColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "背景颜色"
        AppLanguage.ENGLISH -> "Background Color"
        AppLanguage.ARABIC -> "لون الخلفية"
    }

    val mediaScreenLock: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏幕常亮"
        AppLanguage.ENGLISH -> "Keep Screen On"
        AppLanguage.ARABIC -> "إبقاء الشاشة مضاءة"
    }

    val mediaScreenLockHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示媒体时防止屏幕自动关闭"
        AppLanguage.ENGLISH -> "Prevent screen from turning off during media display"
        AppLanguage.ARABIC -> "منع إيقاف الشاشة أثناء عرض الوسائط"
    }

    val mediaGestureConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "手势设置"
        AppLanguage.ENGLISH -> "Gesture Settings"
        AppLanguage.ARABIC -> "إعدادات الإيماءات"
    }

    val mediaSwipeDismiss: String get() = when (lang) {
        AppLanguage.CHINESE -> "滑动退出"
        AppLanguage.ENGLISH -> "Swipe to Dismiss"
        AppLanguage.ARABIC -> "اسحب للإغلاق"
    }

    val mediaSwipeDismissHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "上下滑动关闭媒体"
        AppLanguage.ENGLISH -> "Swipe up/down to close media"
        AppLanguage.ARABIC -> "اسحب لأعلى/لأسفل لإغلاق الوسائط"
    }

    val mediaDoubleTapZoom: String get() = when (lang) {
        AppLanguage.CHINESE -> "双击缩放"
        AppLanguage.ENGLISH -> "Double-tap to Zoom"
        AppLanguage.ARABIC -> "انقر مرتين للتكبير"
    }

    val mediaDoubleTapZoomHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "双击放大/缩小图片"
        AppLanguage.ENGLISH -> "Double-tap to zoom in/out"
        AppLanguage.ARABIC -> "انقر مرتين للتكبير/التصغير"
    }

    val mediaBrightness: String get() = when (lang) {
        AppLanguage.CHINESE -> "亮度"
        AppLanguage.ENGLISH -> "Brightness"
        AppLanguage.ARABIC -> "السطوع"
    }

    val mediaContrast: String get() = when (lang) {
        AppLanguage.CHINESE -> "对比度"
        AppLanguage.ENGLISH -> "Contrast"
        AppLanguage.ARABIC -> "التباين"
    }

    val mediaSaturation: String get() = when (lang) {
        AppLanguage.CHINESE -> "饱和度"
        AppLanguage.ENGLISH -> "Saturation"
        AppLanguage.ARABIC -> "التشبع"
    }

    val mediaImageAdjust: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片调整"
        AppLanguage.ENGLISH -> "Image Adjustments"
        AppLanguage.ARABIC -> "تعديلات الصورة"
    }

    val mediaImageAdjustHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "调整图片显示效果"
        AppLanguage.ENGLISH -> "Adjust image display effects"
        AppLanguage.ARABIC -> "ضبط تأثيرات عرض الصورة"
    }

    val mediaReset: String get() = when (lang) {
        AppLanguage.CHINESE -> "重置"
        AppLanguage.ENGLISH -> "Reset"
        AppLanguage.ARABIC -> "إعادة تعيين"
    }
    // ==================== General ====================
    val dirNotExists: String get() = when (lang) {
        AppLanguage.CHINESE -> "目录不存在"
        AppLanguage.ENGLISH -> "Directory does not exist"
        AppLanguage.ARABIC -> "المجلد غير موجود"
    }

    val copyingDocsFiles: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在复制文档文件..."
        AppLanguage.ENGLISH -> "Copying docs files..."
        AppLanguage.ARABIC -> "جارٍ نسخ ملفات التوثيق..."
    }

    val projectImportFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目导入失败"
        AppLanguage.ENGLISH -> "Project import failed"
        AppLanguage.ARABIC -> "فشل استيراد المشروع"
    }

    val frameworkDetected: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测到框架"
        AppLanguage.ENGLISH -> "Framework Detected"
        AppLanguage.ARABIC -> "تم اكتشاف الإطار"
    }

    val enableSearch: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用搜索"
        AppLanguage.ENGLISH -> "Enable Search"
        AppLanguage.ARABIC -> "تمكين البحث"
    }

    val preparingEnv: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在准备环境..."
        AppLanguage.ENGLISH -> "Preparing environment..."
        AppLanguage.ARABIC -> "جارٍ تحضير البيئة..."
    }

    val startingServer: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在启动服务器..."
        AppLanguage.ENGLISH -> "Starting server..."
        AppLanguage.ARABIC -> "جارٍ تشغيل الخادم..."
    }

    val serverStartFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "服务启动失败"
        AppLanguage.ENGLISH -> "Server failed to start"
        AppLanguage.ARABIC -> "فشل تشغيل الخادم"
    }

    val phpSupportedFrameworks: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持 Laravel、ThinkPHP、CodeIgniter、Slim 和原生 PHP 项目"
        AppLanguage.ENGLISH -> "Supports Laravel, ThinkPHP, CodeIgniter, Slim and plain PHP projects"
        AppLanguage.ARABIC -> "يدعم Laravel و ThinkPHP و CodeIgniter و Slim ومشاريع PHP الأصلية"
    }

    val pySupportedFrameworks: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持 Flask、Django、FastAPI、Tornado 和原生 Python Web 项目"
        AppLanguage.ENGLISH -> "Supports Flask, Django, FastAPI, Tornado and plain Python Web projects"
        AppLanguage.ARABIC -> "يدعم Flask و Django و FastAPI و Tornado ومشاريع Python Web الأصلية"
    }

    val goSupportedFrameworks: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持 Gin、Fiber、Echo、Chi 和 net/http 项目，需包含预编译的 ARM64 二进制文件"
        AppLanguage.ENGLISH -> "Supports Gin, Fiber, Echo, Chi and net/http projects. Pre-compiled ARM64 binary required."
        AppLanguage.ARABIC -> "يدعم Gin و Fiber و Echo و Chi و net/http. يتطلب ملف ثنائي ARM64 مسبق التجميع."
    }

    val docsSiteDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择文档站点构建输出目录（dist/build/public），支持 VitePress、Hexo、Hugo、MkDocs、Docusaurus"
        AppLanguage.ENGLISH -> "Select docs site build output (dist/build/public). Supports VitePress, Hexo, Hugo, MkDocs, Docusaurus."
        AppLanguage.ARABIC -> "اختر مجلد إخراج بناء موقع التوثيق (dist/build/public). يدعم VitePress و Hexo و Hugo و MkDocs و Docusaurus."
    }

    val docsNoIndex: String get() = when (lang) {
        AppLanguage.CHINESE -> "未找到 index.html，请选择文档站点构建输出目录"
        AppLanguage.ENGLISH -> "index.html not found. Please select the docs site build output directory."
        AppLanguage.ARABIC -> "لم يتم العثور على index.html. يرجى اختيار مجلد إخراج بناء موقع التوثيق."
    }

    val docsHashRouting: String get() = when (lang) {
        AppLanguage.CHINESE -> "Hash (#/) - 推荐"
        AppLanguage.ENGLISH -> "Hash (#/) - Recommended"
        AppLanguage.ARABIC -> "Hash (#/) - موصى به"
    }

    val docsHistoryRouting: String get() = when (lang) {
        AppLanguage.CHINESE -> "History (/) - 需要服务端支持"
        AppLanguage.ENGLISH -> "History (/) - Requires server support"
        AppLanguage.ARABIC -> "History (/) - يتطلب دعم الخادم"
    }
    // ==================== PHP App Strings ====================
    val phpFrameworkDetected: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测到框架"
        AppLanguage.ENGLISH -> "Framework Detected"
        AppLanguage.ARABIC -> "تم اكتشاف الإطار"
    }

    val phpDocumentRoot: String get() = when (lang) {
        AppLanguage.CHINESE -> "Web 根目录"
        AppLanguage.ENGLISH -> "Document Root"
        AppLanguage.ARABIC -> "جذر المستند"
    }

    val phpEntryFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "入口文件"
        AppLanguage.ENGLISH -> "Entry File"
        AppLanguage.ARABIC -> "ملف الدخول"
    }

    val phpProjectReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP 项目已就绪"
        AppLanguage.ENGLISH -> "PHP project ready"
        AppLanguage.ARABIC -> "مشروع PHP جاهز"
    }

    val phpSelectProject: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择 PHP 项目目录"
        AppLanguage.ENGLISH -> "Select PHP Project Directory"
        AppLanguage.ARABIC -> "اختر مجلد مشروع PHP"
    }

    val phpNoIndexFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "未找到 PHP 入口文件"
        AppLanguage.ENGLISH -> "No PHP entry file found"
        AppLanguage.ARABIC -> "لم يتم العثور على ملف دخول PHP"
    }

    val phpAppCheckingDeps: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查 PHP 环境..."
        AppLanguage.ENGLISH -> "Checking PHP environment..."
        AppLanguage.ARABIC -> "جارٍ فحص بيئة PHP..."
    }

    val phpAppDownloading: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在下载 PHP 运行时"
        AppLanguage.ENGLISH -> "Downloading PHP runtime"
        AppLanguage.ARABIC -> "جارٍ تنزيل بيئة تشغيل PHP"
    }

    val phpAppStartingServer: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在启动 PHP 服务器..."
        AppLanguage.ENGLISH -> "Starting PHP server..."
        AppLanguage.ARABIC -> "جارٍ تشغيل خادم PHP..."
    }

    val phpAppServerError: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP 服务器启动失败"
        AppLanguage.ENGLISH -> "PHP server failed to start"
        AppLanguage.ARABIC -> "فشل تشغيل خادم PHP"
    }

    val phpAppDownloadFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP 运行时下载失败，请检查网络后重试"
        AppLanguage.ENGLISH -> "PHP runtime download failed, please check network and retry"
        AppLanguage.ARABIC -> "فشل تنزيل بيئة تشغيل PHP، يرجى التحقق من الشبكة والمحاولة مرة أخرى"
    }

    val phpAppProjectNotFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "PHP 项目文件不存在"
        AppLanguage.ENGLISH -> "PHP project files not found"
        AppLanguage.ARABIC -> "ملفات مشروع PHP غير موجودة"
    }

    val phpImportZip: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入 ZIP 压缩包"
        AppLanguage.ENGLISH -> "Import ZIP Archive"
        AppLanguage.ARABIC -> "استيراد أرشيف ZIP"
    }

    val phpExtractingZip: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在解压 ZIP 文件..."
        AppLanguage.ENGLISH -> "Extracting ZIP file..."
        AppLanguage.ARABIC -> "جارٍ استخراج ملف ZIP..."
    }

    val phpZipExtractFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "ZIP 解压失败"
        AppLanguage.ENGLISH -> "ZIP extraction failed"
        AppLanguage.ARABIC -> "فشل استخراج ZIP"
    }

    val phpZipNoPhpFiles: String get() = when (lang) {
        AppLanguage.CHINESE -> "ZIP 中未找到 PHP 项目文件"
        AppLanguage.ENGLISH -> "No PHP project files found in ZIP"
        AppLanguage.ARABIC -> "لم يتم العثور على ملفات مشروع PHP في ZIP"
    }
    // ==================== Create App ====================
    val customPackageName: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义包名"
        AppLanguage.ENGLISH -> "Custom Package Name"
        AppLanguage.ARABIC -> "اسم الحزمة المخصص"
    }

    val packageNameTooLong: String get() = when (lang) {
        AppLanguage.CHINESE -> "包名过长！最多%d字符（当前%d）"
        AppLanguage.ENGLISH -> "Package name too long! Max %d characters (current %d)"
        AppLanguage.ARABIC -> "اسم الحزمة طويل جدًا! الحد الأقصى %d حرف (الحالي %d)"
    }

    val packageNameInvalidFormat: String get() = when (lang) {
        AppLanguage.CHINESE -> "格式错误，应为小写字母开头，如：com.w2a.app"
        AppLanguage.ENGLISH -> "Invalid format, should start with lowercase letter, e.g.: com.w2a.app"
        AppLanguage.ARABIC -> "تنسيق غير صالح، يجب أن يبدأ بحرف صغير، مثال: com.w2a.app"
    }

    val packageNameHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "留空自动生成，如：com.example.myapp"
        AppLanguage.ENGLISH -> "Leave empty for auto-generation, e.g.: com.example.myapp"
        AppLanguage.ARABIC -> "اتركه فارغًا للإنشاء التلقائي، مثال: com.example.myapp"
    }

    val apkConfigNote: String get() = when (lang) {
        AppLanguage.CHINESE -> "以下配置仅在打包APK时生效"
        AppLanguage.ENGLISH -> "The following settings only take effect when building APK"
        AppLanguage.ARABIC -> "الإعدادات التالية تسري فقط عند بناء APK"
    }

    val versionName: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本名"
        AppLanguage.ENGLISH -> "Version Name"
        AppLanguage.ARABIC -> "اسم الإصدار"
    }

    val versionCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "版本号"
        AppLanguage.ENGLISH -> "Version Code"
        AppLanguage.ARABIC -> "رقم الإصدار"
    }

    val selectTheme: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择主题"
        AppLanguage.ENGLISH -> "Select Theme"
        AppLanguage.ARABIC -> "اختيار السمة"
    }

    val translateTargetLanguage: String get() = when (lang) {
        AppLanguage.CHINESE -> "翻译目标语言"
        AppLanguage.ENGLISH -> "Translation Target Language"
        AppLanguage.ARABIC -> "لغة الترجمة المستهدفة"
    }

    val adBlockRuleHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "如：ads.example.com"
        AppLanguage.ENGLISH -> "e.g.: ads.example.com"
        AppLanguage.ARABIC -> "مثال: ads.example.com"
    }

    val adBlockDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用后将自动拦截网页中的广告内容"
        AppLanguage.ENGLISH -> "When enabled, ads in web pages will be automatically blocked"
        AppLanguage.ARABIC -> "عند التفعيل، سيتم حظر الإعلانات في صفحات الويب تلقائيًا"
    }

    val customBlockRules: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义拦截规则（可选）"
        AppLanguage.ENGLISH -> "Custom Block Rules (optional)"
        AppLanguage.ARABIC -> "قواعد الحظر المخصصة (اختياري)"
    }

    val adBlockEnabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "广告拦截已开启"
        AppLanguage.ENGLISH -> "Ad Block Enabled"
        AppLanguage.ARABIC -> "تم تفعيل حظر الإعلانات"
    }

    val adBlockDisabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "广告拦截已关闭"
        AppLanguage.ENGLISH -> "Ad Block Disabled"
        AppLanguage.ARABIC -> "تم إيقاف حظر الإعلانات"
    }

    val adBlockToggleEnabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "允许用户切换"
        AppLanguage.ENGLISH -> "Allow User Toggle"
        AppLanguage.ARABIC -> "السماح للمستخدم بالتبديل"
    }

    val adBlockToggleDescription: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用后，用户可在运行时通过悬浮按钮开关广告拦截"
        AppLanguage.ENGLISH -> "When enabled, user can toggle ad blocking via floating button at runtime"
        AppLanguage.ARABIC -> "عند التفعيل، يمكن للمستخدم تبديل حظر الإعلانات عبر الزر العائم أثناء التشغيل"
    }
    // ==================== Go Enhanced Strings ====================
    val goHeroTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "Go 服务"
        AppLanguage.ENGLISH -> "Go Service"
        AppLanguage.ARABIC -> "خدمة Go"
    }

    val goHeroDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将 Go Web 服务打包为独立 Android 应用"
        AppLanguage.ENGLISH -> "Package Go Web service as standalone Android app"
        AppLanguage.ARABIC -> "تغليف خدمة Go Web كتطبيق Android مستقل"
    }

    val goModuleInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "Go 模块信息"
        AppLanguage.ENGLISH -> "Go Module Info"
        AppLanguage.ARABIC -> "معلومات وحدة Go"
    }

    val goModulePath: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块路径"
        AppLanguage.ENGLISH -> "Module Path"
        AppLanguage.ARABIC -> "مسار الوحدة"
    }

    val goVersion: String get() = when (lang) {
        AppLanguage.CHINESE -> "Go 版本"
        AppLanguage.ENGLISH -> "Go Version"
        AppLanguage.ARABIC -> "إصدار Go"
    }

    val goDependencyCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "依赖数量"
        AppLanguage.ENGLISH -> "Dependency Count"
        AppLanguage.ARABIC -> "عدد التبعيات"
    }

    val goBinaryDetection: String get() = when (lang) {
        AppLanguage.CHINESE -> "二进制文件"
        AppLanguage.ENGLISH -> "Binary Detection"
        AppLanguage.ARABIC -> "اكتشاف الملف الثنائي"
    }

    val goBinaryFound: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测到预编译二进制文件"
        AppLanguage.ENGLISH -> "Pre-compiled binary detected"
        AppLanguage.ARABIC -> "تم اكتشاف ملف ثنائي مسبق التجميع"
    }

    val goFileSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "文件大小"
        AppLanguage.ENGLISH -> "File Size"
        AppLanguage.ARABIC -> "حجم الملف"
    }

    val goTargetArch: String get() = when (lang) {
        AppLanguage.CHINESE -> "目标架构"
        AppLanguage.ENGLISH -> "Target Architecture"
        AppLanguage.ARABIC -> "البنية المستهدفة"
    }

    val goStaticFiles: String get() = when (lang) {
        AppLanguage.CHINESE -> "静态文件目录"
        AppLanguage.ENGLISH -> "Static Files Directory"
        AppLanguage.ARABIC -> "دليل الملفات الثابتة"
    }

    val goStaticFilesHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "Web 静态文件服务路径"
        AppLanguage.ENGLISH -> "Web static file serving path"
        AppLanguage.ARABIC -> "مسار خدمة الملفات الثابتة على الويب"
    }

    val goHealthCheck: String get() = when (lang) {
        AppLanguage.CHINESE -> "健康检查"
        AppLanguage.ENGLISH -> "Health Check"
        AppLanguage.ARABIC -> "فحص السلامة"
    }

    val goHealthCheckEndpoint: String get() = when (lang) {
        AppLanguage.CHINESE -> "健康检查端点"
        AppLanguage.ENGLISH -> "Health Check Endpoint"
        AppLanguage.ARABIC -> "نقطة نهاية فحص السلامة"
    }

    val goDirectDeps: String get() = when (lang) {
        AppLanguage.CHINESE -> "直接依赖"
        AppLanguage.ENGLISH -> "Direct Dependencies"
        AppLanguage.ARABIC -> "التبعيات المباشرة"
    }
    // ==================== PWA Auto Detection (PWA Auto-Detection) ====================
    val pwaAnalyzeButton: String get() = when (lang) {
        AppLanguage.CHINESE -> "分析网站"
        AppLanguage.ENGLISH -> "Analyze Site"
        AppLanguage.ARABIC -> "تحليل الموقع"
    }

    val pwaAnalyzing: String get() = when (lang) {
        AppLanguage.CHINESE -> "正在分析网站 PWA 配置..."
        AppLanguage.ENGLISH -> "Analyzing website PWA configuration..."
        AppLanguage.ARABIC -> "جاري تحليل إعدادات PWA للموقع..."
    }

    val pwaDetected: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测到 PWA 配置"
        AppLanguage.ENGLISH -> "PWA Configuration Detected"
        AppLanguage.ARABIC -> "تم اكتشاف إعدادات PWA"
    }

    val pwaNoneDetected: String get() = when (lang) {
        AppLanguage.CHINESE -> "未检测到 PWA 配置，已从页面提取基本信息"
        AppLanguage.ENGLISH -> "No PWA configuration found, basic info extracted from page"
        AppLanguage.ARABIC -> "لم يتم العثور على إعدادات PWA، تم استخراج المعلومات الأساسية"
    }

    val pwaApplyAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "一键应用"
        AppLanguage.ENGLISH -> "Apply All"
        AppLanguage.ARABIC -> "تطبيق الكل"
    }

    val pwaApplied: String get() = when (lang) {
        AppLanguage.CHINESE -> "PWA 配置已应用"
        AppLanguage.ENGLISH -> "PWA configuration applied"
        AppLanguage.ARABIC -> "تم تطبيق إعدادات PWA"
    }

    val pwaSourceManifest: String get() = when (lang) {
        AppLanguage.CHINESE -> "来源: manifest.json"
        AppLanguage.ENGLISH -> "Source: manifest.json"
        AppLanguage.ARABIC -> "المصدر: manifest.json"
    }

    val pwaSourceMeta: String get() = when (lang) {
        AppLanguage.CHINESE -> "来源: HTML Meta 标签"
        AppLanguage.ENGLISH -> "Source: HTML Meta Tags"
        AppLanguage.ARABIC -> "المصدر: علامات HTML Meta"
    }

    val pwaName: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用名称"
        AppLanguage.ENGLISH -> "App Name"
        AppLanguage.ARABIC -> "اسم التطبيق"
    }

    val pwaIcon: String get() = when (lang) {
        AppLanguage.CHINESE -> "应用图标"
        AppLanguage.ENGLISH -> "App Icon"
        AppLanguage.ARABIC -> "أيقونة التطبيق"
    }

    val pwaThemeColor: String get() = when (lang) {
        AppLanguage.CHINESE -> "主题色"
        AppLanguage.ENGLISH -> "Theme Color"
        AppLanguage.ARABIC -> "لون السمة"
    }

    val pwaDisplayMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示模式"
        AppLanguage.ENGLISH -> "Display Mode"
        AppLanguage.ARABIC -> "وضع العرض"
    }

    val pwaOrientation: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏幕方向"
        AppLanguage.ENGLISH -> "Orientation"
        AppLanguage.ARABIC -> "اتجاه الشاشة"
    }

    val pwaStartUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "起始 URL"
        AppLanguage.ENGLISH -> "Start URL"
        AppLanguage.ARABIC -> "عنوان URL البدء"
    }

    val pwaAnalysisFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "网站分析失败"
        AppLanguage.ENGLISH -> "Site analysis failed"
        AppLanguage.ARABIC -> "فشل تحليل الموقع"
    }

    val pwaIconDownloaded: String get() = when (lang) {
        AppLanguage.CHINESE -> "图标已下载"
        AppLanguage.ENGLISH -> "Icon downloaded"
        AppLanguage.ARABIC -> "تم تنزيل الأيقونة"
    }

    val pwaIconDownloadFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "图标下载失败"
        AppLanguage.ENGLISH -> "Icon download failed"
        AppLanguage.ARABIC -> "فشل تنزيل الأيقونة"
    }
    // ==================== Node.js Enhanced Strings ====================
    val njsHeroTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "Node.js 应用"
        AppLanguage.ENGLISH -> "Node.js Application"
        AppLanguage.ARABIC -> "تطبيق Node.js"
    }

    val njsHeroDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将 Node.js 项目打包为独立 Android 应用"
        AppLanguage.ENGLISH -> "Package Node.js project as standalone Android app"
        AppLanguage.ARABIC -> "تغليف مشروع Node.js كتطبيق Android مستقل"
    }

    val njsScripts: String get() = when (lang) {
        AppLanguage.CHINESE -> "NPM 脚本"
        AppLanguage.ENGLISH -> "NPM Scripts"
        AppLanguage.ARABIC -> "نصوص NPM"
    }

    val njsStartupScript: String get() = when (lang) {
        AppLanguage.CHINESE -> "启动脚本"
        AppLanguage.ENGLISH -> "Startup Script"
        AppLanguage.ARABIC -> "نص بدء التشغيل"
    }

    val njsDependencies: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目依赖"
        AppLanguage.ENGLISH -> "Project Dependencies"
        AppLanguage.ARABIC -> "تبعيات المشروع"
    }

    val njsDevDependencies: String get() = when (lang) {
        AppLanguage.CHINESE -> "开发依赖"
        AppLanguage.ENGLISH -> "Dev Dependencies"
        AppLanguage.ARABIC -> "تبعيات التطوير"
    }

    val njsTypeScript: String get() = when (lang) {
        AppLanguage.CHINESE -> "TypeScript 支持"
        AppLanguage.ENGLISH -> "TypeScript Support"
        AppLanguage.ARABIC -> "دعم TypeScript"
    }

    val njsPackageManager: String get() = when (lang) {
        AppLanguage.CHINESE -> "包管理器"
        AppLanguage.ENGLISH -> "Package Manager"
        AppLanguage.ARABIC -> "مدير الحزم"
    }

    val njsDetectedPort: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测到端口"
        AppLanguage.ENGLISH -> "Detected Port"
        AppLanguage.ARABIC -> "المنفذ المكتشف"
    }

    val njsPortOverride: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义端口"
        AppLanguage.ENGLISH -> "Custom Port"
        AppLanguage.ARABIC -> "منفذ مخصص"
    }

    val njsProjectInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "项目信息"
        AppLanguage.ENGLISH -> "Project Info"
        AppLanguage.ARABIC -> "معلومات المشروع"
    }
    // ==================== PHP Sample Projects ====================
    val samplePhpSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速体验 PHP 项目导入"
        AppLanguage.ENGLISH -> "Quick experience PHP project import"
        AppLanguage.ARABIC -> "تجربة سريعة لاستيراد مشروع PHP"
    }

    val samplePhpLaravelName: String get() = when (lang) {
        AppLanguage.CHINESE -> "Laravel 博客"
        AppLanguage.ENGLISH -> "Laravel Blog"
        AppLanguage.ARABIC -> "مدونة Laravel"
    }

    val samplePhpLaravelDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Laravel 框架博客示例，展示路由与 Blade 模板引擎"
        AppLanguage.ENGLISH -> "Laravel blog demo with routing and Blade template engine"
        AppLanguage.ARABIC -> "عرض مدونة Laravel مع التوجيه ومحرك قوالب Blade"
    }

    val samplePhpSlimName: String get() = when (lang) {
        AppLanguage.CHINESE -> "Slim REST API"
        AppLanguage.ENGLISH -> "Slim REST API"
        AppLanguage.ARABIC -> "Slim REST API"
    }

    val samplePhpSlimDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Slim 微框架 RESTful 接口示例，轻量高效"
        AppLanguage.ENGLISH -> "Slim micro-framework RESTful API demo, lightweight and fast"
        AppLanguage.ARABIC -> "عرض API RESTful بإطار Slim الخفيف والسريع"
    }

    val samplePhpVanillaName: String get() = when (lang) {
        AppLanguage.CHINESE -> "原生 PHP 应用"
        AppLanguage.ENGLISH -> "Vanilla PHP App"
        AppLanguage.ARABIC -> "تطبيق PHP أصلي"
    }

    val samplePhpVanillaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "无框架原生 PHP 开发示例，展示基本 Web 功能"
        AppLanguage.ENGLISH -> "No-framework PHP demo showcasing basic web features"
        AppLanguage.ARABIC -> "عرض PHP بدون إطار عمل يعرض ميزات الويب الأساسية"
    }

    val sampleTagMvc: String get() = when (lang) {
        AppLanguage.CHINESE -> "MVC"
        AppLanguage.ENGLISH -> "MVC"
        AppLanguage.ARABIC -> "MVC"
    }

    val sampleTagRest: String get() = when (lang) {
        AppLanguage.CHINESE -> "REST"
        AppLanguage.ENGLISH -> "REST"
        AppLanguage.ARABIC -> "REST"
    }

    val sampleTagLightweight: String get() = when (lang) {
        AppLanguage.CHINESE -> "轻量级"
        AppLanguage.ENGLISH -> "Lightweight"
        AppLanguage.ARABIC -> "خفيف الوزن"
    }

    val sampleTagNoFramework: String get() = when (lang) {
        AppLanguage.CHINESE -> "无框架"
        AppLanguage.ENGLISH -> "No Framework"
        AppLanguage.ARABIC -> "بدون إطار"
    }
    // ==================== Python Sample Projects ====================
    val samplePythonSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速体验 Python 项目导入"
        AppLanguage.ENGLISH -> "Quick experience Python project import"
        AppLanguage.ARABIC -> "تجربة سريعة لاستيراد مشروع Python"
    }

    val samplePythonFlaskName: String get() = when (lang) {
        AppLanguage.CHINESE -> "Flask 仪表盘"
        AppLanguage.ENGLISH -> "Flask Dashboard"
        AppLanguage.ARABIC -> "لوحة تحكم Flask"
    }

    val samplePythonFlaskDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Flask 框架数据仪表盘示例，展示 Jinja2 模板渲染"
        AppLanguage.ENGLISH -> "Flask dashboard demo with Jinja2 template rendering"
        AppLanguage.ARABIC -> "عرض لوحة تحكم Flask مع عرض قوالب Jinja2"
    }

    val samplePythonFastapiName: String get() = when (lang) {
        AppLanguage.CHINESE -> "FastAPI 书店"
        AppLanguage.ENGLISH -> "FastAPI Bookstore"
        AppLanguage.ARABIC -> "مكتبة FastAPI"
    }

    val samplePythonFastapiDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "FastAPI 书店 REST API 示例，自动生成 OpenAPI 文档"
        AppLanguage.ENGLISH -> "FastAPI bookstore REST API with auto-generated OpenAPI docs"
        AppLanguage.ARABIC -> "API مكتبة FastAPI مع توثيق OpenAPI تلقائي"
    }

    val samplePythonDjangoName: String get() = when (lang) {
        AppLanguage.CHINESE -> "Django 站点"
        AppLanguage.ENGLISH -> "Django Site"
        AppLanguage.ARABIC -> "موقع Django"
    }

    val samplePythonDjangoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Django 全栈站点示例，展示 ORM 与管理后台"
        AppLanguage.ENGLISH -> "Django full-stack site demo with ORM and admin panel"
        AppLanguage.ARABIC -> "عرض موقع Django كامل مع ORM ولوحة الإدارة"
    }

    val sampleTagWsgi: String get() = when (lang) {
        AppLanguage.CHINESE -> "WSGI"
        AppLanguage.ENGLISH -> "WSGI"
        AppLanguage.ARABIC -> "WSGI"
    }

    val sampleTagAsgi: String get() = when (lang) {
        AppLanguage.CHINESE -> "ASGI"
        AppLanguage.ENGLISH -> "ASGI"
        AppLanguage.ARABIC -> "ASGI"
    }

    val sampleTagOpenapi: String get() = when (lang) {
        AppLanguage.CHINESE -> "OpenAPI"
        AppLanguage.ENGLISH -> "OpenAPI"
        AppLanguage.ARABIC -> "OpenAPI"
    }

    val sampleTagOrm: String get() = when (lang) {
        AppLanguage.CHINESE -> "ORM"
        AppLanguage.ENGLISH -> "ORM"
        AppLanguage.ARABIC -> "ORM"
    }

    val sampleTagAdmin: String get() = when (lang) {
        AppLanguage.CHINESE -> "管理后台"
        AppLanguage.ENGLISH -> "Admin Panel"
        AppLanguage.ARABIC -> "لوحة الإدارة"
    }
    // ==================== Go Sample Projects ====================
    val sampleGoSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速体验 Go 项目导入"
        AppLanguage.ENGLISH -> "Quick experience Go project import"
        AppLanguage.ARABIC -> "تجربة سريعة لاستيراد مشروع Go"
    }

    val sampleGoGinName: String get() = when (lang) {
        AppLanguage.CHINESE -> "Gin REST API"
        AppLanguage.ENGLISH -> "Gin REST API"
        AppLanguage.ARABIC -> "Gin REST API"
    }

    val sampleGoGinDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Gin 框架 RESTful 接口示例，中间件与路由组"
        AppLanguage.ENGLISH -> "Gin RESTful API demo with middleware and route groups"
        AppLanguage.ARABIC -> "عرض Gin REST API مع الوسيط ومجموعات التوجيه"
    }

    val sampleGoFiberName: String get() = when (lang) {
        AppLanguage.CHINESE -> "Fiber Web 应用"
        AppLanguage.ENGLISH -> "Fiber Web App"
        AppLanguage.ARABIC -> "تطبيق Fiber Web"
    }

    val sampleGoFiberDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Fiber 框架 Web 应用示例，Express 风格高性能路由"
        AppLanguage.ENGLISH -> "Fiber web app demo with Express-style high-performance routing"
        AppLanguage.ARABIC -> "عرض Fiber بأسلوب Express مع توجيه عالي الأداء"
    }

    val sampleGoEchoName: String get() = when (lang) {
        AppLanguage.CHINESE -> "Echo 服务器"
        AppLanguage.ENGLISH -> "Echo Server"
        AppLanguage.ARABIC -> "خادم Echo"
    }

    val sampleGoEchoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Echo 框架服务器示例，极简 API 与自动绑定"
        AppLanguage.ENGLISH -> "Echo server demo with minimal API and auto-binding"
        AppLanguage.ARABIC -> "عرض خادم Echo مع API بسيط وربط تلقائي"
    }

    val sampleTagMiddleware: String get() = when (lang) {
        AppLanguage.CHINESE -> "中间件"
        AppLanguage.ENGLISH -> "Middleware"
        AppLanguage.ARABIC -> "وسيط"
    }

    val sampleTagHighPerf: String get() = when (lang) {
        AppLanguage.CHINESE -> "高性能"
        AppLanguage.ENGLISH -> "High Perf"
        AppLanguage.ARABIC -> "أداء عالي"
    }

    val sampleTagMinimalApi: String get() = when (lang) {
        AppLanguage.CHINESE -> "极简 API"
        AppLanguage.ENGLISH -> "Minimal API"
        AppLanguage.ARABIC -> "API بسيط"
    }
    // ==================== DocsSite Sample Projects ====================
    val sampleDocsSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速体验文档站点导入"
        AppLanguage.ENGLISH -> "Quick experience docs site import"
        AppLanguage.ARABIC -> "تجربة سريعة لاستيراد موقع التوثيق"
    }

    val sampleDocsVitepressName: String get() = when (lang) {
        AppLanguage.CHINESE -> "VitePress 文档"
        AppLanguage.ENGLISH -> "VitePress Docs"
        AppLanguage.ARABIC -> "وثائق VitePress"
    }

    val sampleDocsVitepressDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "VitePress 静态文档站点，支持搜索与暗色模式"
        AppLanguage.ENGLISH -> "VitePress static docs site with search and dark mode"
        AppLanguage.ARABIC -> "موقع وثائق VitePress مع البحث والوضع الداكن"
    }

    val sampleDocsMkdocsName: String get() = when (lang) {
        AppLanguage.CHINESE -> "MkDocs 指南"
        AppLanguage.ENGLISH -> "MkDocs Guide"
        AppLanguage.ARABIC -> "دليل MkDocs"
    }

    val sampleDocsMkdocsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "MkDocs Material 主题文档，Markdown 驱动"
        AppLanguage.ENGLISH -> "MkDocs Material theme docs, Markdown-powered"
        AppLanguage.ARABIC -> "وثائق MkDocs بسمة Material مدعومة بـ Markdown"
    }

    val sampleDocsHexoName: String get() = when (lang) {
        AppLanguage.CHINESE -> "Hexo 文档博客"
        AppLanguage.ENGLISH -> "Hexo Doc Blog"
        AppLanguage.ARABIC -> "مدونة وثائق Hexo"
    }

    val sampleDocsHexoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "Hexo 文档博客站点，分类与标签管理"
        AppLanguage.ENGLISH -> "Hexo doc blog site with categories and tag management"
        AppLanguage.ARABIC -> "مدونة وثائق Hexo مع التصنيفات وإدارة العلامات"
    }

    val sampleTagSearch: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索"
        AppLanguage.ENGLISH -> "Search"
        AppLanguage.ARABIC -> "بحث"
    }

    val sampleTagDarkMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "暗色模式"
        AppLanguage.ENGLISH -> "Dark Mode"
        AppLanguage.ARABIC -> "الوضع الداكن"
    }

    val sampleTagMarkdown: String get() = when (lang) {
        AppLanguage.CHINESE -> "Markdown"
        AppLanguage.ENGLISH -> "Markdown"
        AppLanguage.ARABIC -> "Markdown"
    }

    val sampleTagCategories: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类"
        AppLanguage.ENGLISH -> "Categories"
        AppLanguage.ARABIC -> "تصنيفات"
    }
    // ==================== WordPress Sample Projects ====================
    val sampleWpSubtitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "快速体验 WordPress 站点创建"
        AppLanguage.ENGLISH -> "Quick experience WordPress site creation"
        AppLanguage.ARABIC -> "تجربة سريعة لإنشاء موقع WordPress"
    }

    val sampleWpBlogName: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress 博客"
        AppLanguage.ENGLISH -> "WordPress Blog"
        AppLanguage.ARABIC -> "مدونة WordPress"
    }

    val sampleWpBlogDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "经典 WordPress 博客站点，含默认主题与文章示例"
        AppLanguage.ENGLISH -> "Classic WordPress blog site with default theme and sample posts"
        AppLanguage.ARABIC -> "مدونة WordPress كلاسيكية مع السمة الافتراضية ومقالات نموذجية"
    }

    val sampleWpWooName: String get() = when (lang) {
        AppLanguage.CHINESE -> "WooCommerce 商店"
        AppLanguage.ENGLISH -> "WooCommerce Store"
        AppLanguage.ARABIC -> "متجر WooCommerce"
    }

    val sampleWpWooDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "WooCommerce 电商站点示例，展示商品展示与购物车"
        AppLanguage.ENGLISH -> "WooCommerce e-commerce demo with product display and cart"
        AppLanguage.ARABIC -> "عرض WooCommerce للتجارة الإلكترونية مع عرض المنتجات وسلة التسوق"
    }

    val sampleWpPortfolioName: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress 作品集"
        AppLanguage.ENGLISH -> "WordPress Portfolio"
        AppLanguage.ARABIC -> "معرض أعمال WordPress"
    }

    val sampleWpPortfolioDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "WordPress 作品集/企业站点，响应式布局与自定义页面"
        AppLanguage.ENGLISH -> "WordPress portfolio/business site with responsive layout"
        AppLanguage.ARABIC -> "موقع معرض أعمال WordPress بتصميم متجاوب"
    }

    val sampleTagBlog: String get() = when (lang) {
        AppLanguage.CHINESE -> "博客"
        AppLanguage.ENGLISH -> "Blog"
        AppLanguage.ARABIC -> "مدونة"
    }

    val sampleTagEcommerce: String get() = when (lang) {
        AppLanguage.CHINESE -> "电商"
        AppLanguage.ENGLISH -> "E-Commerce"
        AppLanguage.ARABIC -> "تجارة إلكترونية"
    }

    val sampleTagPortfolio: String get() = when (lang) {
        AppLanguage.CHINESE -> "作品集"
        AppLanguage.ENGLISH -> "Portfolio"
        AppLanguage.ARABIC -> "معرض أعمال"
    }

    val sampleTagResponsive: String get() = when (lang) {
        AppLanguage.CHINESE -> "响应式"
        AppLanguage.ENGLISH -> "Responsive"
        AppLanguage.ARABIC -> "متجاوب"
    }

    val sampleTagSqlite: String get() = when (lang) {
        AppLanguage.CHINESE -> "SQLite"
        AppLanguage.ENGLISH -> "SQLite"
        AppLanguage.ARABIC -> "SQLite"
    }
    // ==================== HTML Project Optimization（Linux ） ====================
    val optimizeCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码优化"
        AppLanguage.ENGLISH -> "Code Optimization"
        AppLanguage.ARABIC -> "تحسين الكود"
    }

    val optimizeCodeHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用构建工具压缩 JS/CSS、编译 TypeScript，减小应用体积"
        AppLanguage.ENGLISH -> "Minify JS/CSS, compile TypeScript to reduce app size"
        AppLanguage.ARABIC -> "ضغط JS/CSS وتجميع TypeScript لتقليل حجم التطبيق"
    }

    val optimizing: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化中..."
        AppLanguage.ENGLISH -> "Optimizing..."
        AppLanguage.ARABIC -> "جارٍ التحسين..."
    }

    val optimizeComplete: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化完成"
        AppLanguage.ENGLISH -> "Optimization complete"
        AppLanguage.ARABIC -> "اكتمل التحسين"
    }

    val optimizeResultJs: String get() = when (lang) {
        AppLanguage.CHINESE -> "JS 文件压缩: %d 个"
        AppLanguage.ENGLISH -> "JS files minified: %d"
        AppLanguage.ARABIC -> "ملفات JS مضغوطة: %d"
    }

    val optimizeResultCss: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS 文件压缩: %d 个"
        AppLanguage.ENGLISH -> "CSS files minified: %d"
        AppLanguage.ARABIC -> "ملفات CSS مضغوطة: %d"
    }

    val optimizeResultTs: String get() = when (lang) {
        AppLanguage.CHINESE -> "TypeScript 编译: %d 个"
        AppLanguage.ENGLISH -> "TypeScript compiled: %d"
        AppLanguage.ARABIC -> "ملفات TypeScript مجمعة: %d"
    }

    val optimizeResultSaved: String get() = when (lang) {
        AppLanguage.CHINESE -> "节省空间: %s"
        AppLanguage.ENGLISH -> "Space saved: %s"
        AppLanguage.ARABIC -> "المساحة الموفرة: %s"
    }

    val optimizeFailed: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化失败: %s"
        AppLanguage.ENGLISH -> "Optimization failed: %s"
        AppLanguage.ARABIC -> "فشل التحسين: %s"
    }

    val optimizeEsbuildReady: String get() = when (lang) {
        AppLanguage.CHINESE -> "esbuild 就绪，将使用高性能压缩"
        AppLanguage.ENGLISH -> "esbuild ready, high-performance minification"
        AppLanguage.ARABIC -> "esbuild جاهز، ضغط عالي الأداء"
    }

    val optimizeFallback: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用内置压缩器（安装 esbuild 可获得更好效果）"
        AppLanguage.ENGLISH -> "Using built-in minifier (install esbuild for better results)"
        AppLanguage.ARABIC -> "استخدام الضاغط المدمج (ثبّت esbuild لنتائج أفضل)"
    }
}
