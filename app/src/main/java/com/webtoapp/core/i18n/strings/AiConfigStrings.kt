package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object AiConfigStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage
    // ==================== AI Module Dev Help ====================
    val helpHowToUse: String get() = when (lang) {
        AppLanguage.CHINESE -> "如何使用"
        AppLanguage.ENGLISH -> "How to Use"
        AppLanguage.ARABIC -> "كيفية الاستخدام"
    }

    val helpHowToUseContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "在输入框中用自然语言描述你想要的功能，AI 会自动分析需求并生成对应的扩展模块代码。"
        AppLanguage.ENGLISH -> "Describe the functionality you want in natural language in the input box, and AI will automatically analyze the requirements and generate the corresponding extension module code."
        AppLanguage.ARABIC -> "صف الوظيفة التي تريدها بلغة طبيعية في مربع الإدخال، وسيقوم الذكاء الاصطناعي بتحليل المتطلبات تلقائيًا وإنشاء كود وحدة الإضافة المقابل."
    }

    val helpRequirementTips: String get() = when (lang) {
        AppLanguage.CHINESE -> "需求描述技巧"
        AppLanguage.ENGLISH -> "Requirement Description Tips"
        AppLanguage.ARABIC -> "نصائح وصف المتطلبات"
    }

    val helpRequirementTipsContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "• 描述具体的功能效果\n• 说明目标网站或页面类型\n• 可以参考示例需求的写法"
        AppLanguage.ENGLISH -> "• Describe specific functionality effects\n• Specify target website or page type\n• Refer to example requirements for guidance"
        AppLanguage.ARABIC -> "• وصف تأثيرات الوظائف المحددة\n• تحديد نوع الموقع أو الصفحة المستهدفة\n• الرجوع إلى أمثلة المتطلبات للإرشاد"
    }

    val helpModelSelection: String get() = when (lang) {
        AppLanguage.CHINESE -> "模型选择"
        AppLanguage.ENGLISH -> "Model Selection"
        AppLanguage.ARABIC -> "اختيار النموذج"
    }

    val helpModelSelectionContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "可以选择不同的 AI 模型来生成代码。不同模型可能有不同的效果和速度。"
        AppLanguage.ENGLISH -> "You can choose different AI models to generate code. Different models may have different effects and speeds."
        AppLanguage.ARABIC -> "يمكنك اختيار نماذج ذكاء اصطناعي مختلفة لإنشاء الكود. قد يكون للنماذج المختلفة تأثيرات وسرعات مختلفة."
    }

    val helpCategorySelection: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类选择"
        AppLanguage.ENGLISH -> "Category Selection"
        AppLanguage.ARABIC -> "اختيار الفئة"
    }

    val helpCategorySelectionContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "可以手动选择模块分类，也可以让 AI 自动识别。手动选择可以让生成的代码更精准。"
        AppLanguage.ENGLISH -> "You can manually select module category or let AI auto-detect. Manual selection can make generated code more precise."
        AppLanguage.ARABIC -> "يمكنك اختيار فئة الوحدة يدويًا أو السماح للذكاء الاصطناعي بالكشف التلقائي. الاختيار اليدوي يجعل الكود المُنشأ أكثر دقة."
    }

    val helpAutoCheck: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动检查"
        AppLanguage.ENGLISH -> "Auto Check"
        AppLanguage.ARABIC -> "الفحص التلقائي"
    }

    val helpAutoCheckContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 会自动进行语法检查和安全扫描，确保生成的代码可以正常运行且没有安全隐患。"
        AppLanguage.ENGLISH -> "AI will automatically perform syntax checking and security scanning to ensure generated code runs properly without security risks."
        AppLanguage.ARABIC -> "سيقوم الذكاء الاصطناعي تلقائيًا بإجراء فحص بناء الجملة والمسح الأمني لضمان تشغيل الكود المُنشأ بشكل صحيح دون مخاطر أمنية."
    }

    val helpCodeEdit: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码编辑"
        AppLanguage.ENGLISH -> "Code Editing"
        AppLanguage.ARABIC -> "تحرير الكود"
    }

    val helpCodeEditContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成的代码可以直接编辑修改，保存时会使用修改后的版本。"
        AppLanguage.ENGLISH -> "Generated code can be directly edited and modified. The modified version will be used when saving."
        AppLanguage.ARABIC -> "يمكن تحرير وتعديل الكود المُنشأ مباشرة. سيتم استخدام النسخة المعدلة عند الحفظ."
    }

    val helpSaveModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存模块"
        AppLanguage.ENGLISH -> "Save Module"
        AppLanguage.ARABIC -> "حفظ الوحدة"
    }

    val helpSaveModuleContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成完成后，点击「保存」将其添加到你的模块库中，之后可以在创建应用时使用。"
        AppLanguage.ENGLISH -> "After generation is complete, click 'Save' to add it to your module library for use when creating apps."
        AppLanguage.ARABIC -> "بعد اكتمال الإنشاء، انقر على 'حفظ' لإضافته إلى مكتبة الوحدات الخاصة بك لاستخدامه عند إنشاء التطبيقات."
    }

    val howToUse: String get() = when (lang) {
        AppLanguage.CHINESE -> "如何使用"
        AppLanguage.ENGLISH -> "How to Use"
        AppLanguage.ARABIC -> "كيفية الاستخدام"
    }

    val howToUseContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "在输入框中用自然语言描述你想要的功能，AI 会自动分析需求并生成对应的扩展模块代码。"
        AppLanguage.ENGLISH -> "Describe the feature you want in natural language in the input box, AI will automatically analyze requirements and generate corresponding extension module code."
        AppLanguage.ARABIC -> "صف الميزة التي تريدها بلغة طبيعية في مربع الإدخال، سيقوم الذكاء الاصطناعي بتحليل المتطلبات تلقائيًا وإنشاء كود وحدة الإضافة المقابل."
    }

    val requirementDescriptionTips: String get() = when (lang) {
        AppLanguage.CHINESE -> "需求描述技巧"
        AppLanguage.ENGLISH -> "Requirement Description Tips"
        AppLanguage.ARABIC -> "نصائح وصف المتطلبات"
    }

    val requirementDescriptionTipsContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "• 描述具体的功能效果\n• 说明目标网站或页面类型\n• 可以参考示例需求的写法"
        AppLanguage.ENGLISH -> "• Describe specific feature effects\n• Specify target website or page type\n• Can refer to example requirements"
        AppLanguage.ARABIC -> "• وصف تأثيرات الميزة المحددة\n• تحديد الموقع أو نوع الصفحة المستهدفة\n• يمكن الرجوع إلى أمثلة المتطلبات"
    }

    val modelSelection: String get() = when (lang) {
        AppLanguage.CHINESE -> "模型选择"
        AppLanguage.ENGLISH -> "Model Selection"
        AppLanguage.ARABIC -> "اختيار النموذج"
    }

    val modelSelectionContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "可以选择不同的 AI 模型来生成代码。不同模型可能有不同的效果和速度。"
        AppLanguage.ENGLISH -> "You can choose different AI models to generate code. Different models may have different effects and speeds."
        AppLanguage.ARABIC -> "يمكنك اختيار نماذج ذكاء اصطناعي مختلفة لإنشاء الكود. قد يكون للنماذج المختلفة تأثيرات وسرعات مختلفة."
    }

    val categorySelection: String get() = when (lang) {
        AppLanguage.CHINESE -> "分类选择"
        AppLanguage.ENGLISH -> "Category Selection"
        AppLanguage.ARABIC -> "اختيار الفئة"
    }

    val categorySelectionContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "可以手动选择模块分类，也可以让 AI 自动识别。手动选择可以让生成的代码更精准。"
        AppLanguage.ENGLISH -> "You can manually select module category or let AI auto-detect. Manual selection can make generated code more precise."
        AppLanguage.ARABIC -> "يمكنك اختيار فئة الوحدة يدويًا أو السماح للذكاء الاصطناعي بالكشف التلقائي. الاختيار اليدوي يجعل الكود المُنشأ أكثر دقة."
    }

    val autoCheck: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动检查"
        AppLanguage.ENGLISH -> "Auto Check"
        AppLanguage.ARABIC -> "الفحص التلقائي"
    }

    val autoCheckContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 会自动进行语法检查和安全扫描，确保生成的代码可以正常运行且没有安全隐患。"
        AppLanguage.ENGLISH -> "AI will automatically perform syntax check and security scan to ensure generated code runs properly without security risks."
        AppLanguage.ARABIC -> "سيقوم الذكاء الاصطناعي تلقائيًا بإجراء فحص بناء الجملة والمسح الأمني لضمان تشغيل الكود المُنشأ بشكل صحيح دون مخاطر أمنية."
    }

    val codeEditing: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码编辑"
        AppLanguage.ENGLISH -> "Code Editing"
        AppLanguage.ARABIC -> "تحرير الكود"
    }

    val codeEditingContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成的代码可以直接编辑修改，保存时会使用修改后的版本。"
        AppLanguage.ENGLISH -> "Generated code can be directly edited, modified version will be used when saving."
        AppLanguage.ARABIC -> "يمكن تحرير الكود المُنشأ مباشرة، سيتم استخدام النسخة المعدلة عند الحفظ."
    }

    val saveModule: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存模块"
        AppLanguage.ENGLISH -> "Save Module"
        AppLanguage.ARABIC -> "حفظ الوحدة"
    }

    val saveModuleContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成完成后，点击「保存」将其添加到你的模块库中，之后可以在创建应用时使用。"
        AppLanguage.ENGLISH -> "After generation, click 'Save' to add it to your module library, then you can use it when creating apps."
        AppLanguage.ARABIC -> "بعد الإنشاء، انقر على 'حفظ' لإضافته إلى مكتبة الوحدات الخاصة بك، ثم يمكنك استخدامه عند إنشاء التطبيقات."
    }
    // ==================== AI Config ====================
    val textGeneration: String get() = when (lang) {
        AppLanguage.CHINESE -> "文本生成"
        AppLanguage.ENGLISH -> "Text Generation"
        AppLanguage.ARABIC -> "توليد النص"
    }

    val basicTextDialogue: String get() = when (lang) {
        AppLanguage.CHINESE -> "基础文本对话和生成"
        AppLanguage.ENGLISH -> "Basic text dialogue and generation"
        AppLanguage.ARABIC -> "حوار النص الأساسي والتوليد"
    }

    val audioUnderstanding: String get() = when (lang) {
        AppLanguage.CHINESE -> "音频理解"
        AppLanguage.ENGLISH -> "Audio Understanding"
        AppLanguage.ARABIC -> "فهم الصوت"
    }

    val understandAndTranscribeAudio: String get() = when (lang) {
        AppLanguage.CHINESE -> "理解和转录音频内容"
        AppLanguage.ENGLISH -> "Understand and transcribe audio content"
        AppLanguage.ARABIC -> "فهم ونسخ محتوى الصوت"
    }

    val imageUnderstanding: String get() = when (lang) {
        AppLanguage.CHINESE -> "图像理解"
        AppLanguage.ENGLISH -> "Image Understanding"
        AppLanguage.ARABIC -> "فهم الصور"
    }

    val understandAndAnalyzeImages: String get() = when (lang) {
        AppLanguage.CHINESE -> "理解和分析图片内容"
        AppLanguage.ENGLISH -> "Understand and analyze image content"
        AppLanguage.ARABIC -> "فهم وتحليل محتوى الصور"
    }

    val imageGeneration: String get() = when (lang) {
        AppLanguage.CHINESE -> "图像生成"
        AppLanguage.ENGLISH -> "Image Generation"
        AppLanguage.ARABIC -> "توليد الصور"
    }

    val generateImages: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成图片"
        AppLanguage.ENGLISH -> "Generate images"
        AppLanguage.ARABIC -> "إنشاء الصور"
    }

    val codeGeneration: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码生成"
        AppLanguage.ENGLISH -> "Code Generation"
        AppLanguage.ARABIC -> "توليد الكود"
    }

    val generateAndUnderstandCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成和理解代码"
        AppLanguage.ENGLISH -> "Generate and understand code"
        AppLanguage.ARABIC -> "توليد وفهم الكود"
    }

    val functionCall: String get() = when (lang) {
        AppLanguage.CHINESE -> "函数调用"
        AppLanguage.ENGLISH -> "Function Call"
        AppLanguage.ARABIC -> "استدعاء الدالة"
    }

    val supportToolCall: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持工具调用"
        AppLanguage.ENGLISH -> "Support tool call"
        AppLanguage.ARABIC -> "دعم استدعاء الأدوات"
    }

    val longContext: String get() = when (lang) {
        AppLanguage.CHINESE -> "长上下文"
        AppLanguage.ENGLISH -> "Long Context"
        AppLanguage.ARABIC -> "سياق طويل"
    }

    val supportLongTextInput: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持超长文本输入"
        AppLanguage.ENGLISH -> "Support extra long text input"
        AppLanguage.ARABIC -> "دعم إدخال نص طويل جدًا"
    }

    val goToConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "前往配置"
        AppLanguage.ENGLISH -> "Go to Config"
        AppLanguage.ARABIC -> "الذهاب إلى الإعدادات"
    }

    val retry: String get() = when (lang) {
        AppLanguage.CHINESE -> "Retry"
        AppLanguage.ENGLISH -> "Retry"
        AppLanguage.ARABIC -> "إعادة المحاولة"
    }
    // ==================== HTML AI ====================
    val styleHarryPotter: String get() = when (lang) {
        AppLanguage.CHINESE -> "哈利波特风格"
        AppLanguage.ENGLISH -> "Harry Potter Style"
        AppLanguage.ARABIC -> "نمط هاري بوتر"
    }

    val styleHarryPotterDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "霍格沃茨的魔法世界"
        AppLanguage.ENGLISH -> "The magical world of Hogwarts"
        AppLanguage.ARABIC -> "عالم هوجورتس السحري"
    }

    val styleGhibli: String get() = when (lang) {
        AppLanguage.CHINESE -> "吉卜力风格"
        AppLanguage.ENGLISH -> "Ghibli Style"
        AppLanguage.ARABIC -> "نمط جيبلي"
    }

    val styleGhibliDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "宫崎骏的温暖治愈世界"
        AppLanguage.ENGLISH -> "Miyazaki's warm and healing world"
        AppLanguage.ARABIC -> "عالم ميازاكي الدافئ والشافي"
    }

    val styleYourName: String get() = when (lang) {
        AppLanguage.CHINESE -> "你的名字风格"
        AppLanguage.ENGLISH -> "Your Name Style"
        AppLanguage.ARABIC -> "نمط اسمك"
    }

    val styleYourNameDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "新海诚的唯美光影"
        AppLanguage.ENGLISH -> "Shinkai's beautiful lighting"
        AppLanguage.ARABIC -> "إضاءة شينكاي الجميلة"
    }

    val styleApple: String get() = when (lang) {
        AppLanguage.CHINESE -> "苹果设计风格"
        AppLanguage.ENGLISH -> "Apple Design Style"
        AppLanguage.ARABIC -> "نمط تصميم آبل"
    }

    val styleAppleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "简约、优雅、科技感"
        AppLanguage.ENGLISH -> "Simple, elegant, tech-forward"
        AppLanguage.ARABIC -> "بسيط، أنيق، تقني"
    }

    val styleLittlePrince: String get() = when (lang) {
        AppLanguage.CHINESE -> "小王子风格"
        AppLanguage.ENGLISH -> "Little Prince Style"
        AppLanguage.ARABIC -> "نمط الأمير الصغير"
    }

    val styleLittlePrinceDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "充满诗意的童话风格"
        AppLanguage.ENGLISH -> "Poetic fairytale style"
        AppLanguage.ARABIC -> "نمط قصة خيالية شاعري"
    }

    val styleZeldaBotw: String get() = when (lang) {
        AppLanguage.CHINESE -> "塞尔达荒野之息"
        AppLanguage.ENGLISH -> "Zelda: Breath of the Wild"
        AppLanguage.ARABIC -> "زيلدا: نفس البرية"
    }

    val styleZeldaBotwDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "海拉鲁的广袤世界"
        AppLanguage.ENGLISH -> "The vast world of Hyrule"
        AppLanguage.ARABIC -> "عالم هايرول الشاسع"
    }

    val styleArtDeco: String get() = when (lang) {
        AppLanguage.CHINESE -> "装饰艺术风格"
        AppLanguage.ENGLISH -> "Art Deco Style"
        AppLanguage.ARABIC -> "نمط آرت ديكو"
    }

    val styleArtDecoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "1920年代的装饰艺术运动"
        AppLanguage.ENGLISH -> "1920s Art Deco movement"
        AppLanguage.ARABIC -> "حركة آرت ديكو في العشرينيات"
    }

    val styleJapanese: String get() = when (lang) {
        AppLanguage.CHINESE -> "日式和风"
        AppLanguage.ENGLISH -> "Japanese Style"
        AppLanguage.ARABIC -> "النمط الياباني"
    }

    val styleJapaneseDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "日本传统美学，禅意与留白"
        AppLanguage.ENGLISH -> "Japanese aesthetics, zen and whitespace"
        AppLanguage.ARABIC -> "الجماليات اليابانية، الزن والمساحات البيضاء"
    }
    // ==================== Session Config ====================
    val sessionConfig: String get() = when (lang) {
        AppLanguage.CHINESE -> "会话配置"
        AppLanguage.ENGLISH -> "Session Config"
        AppLanguage.ARABIC -> "إعدادات الجلسة"
    }

    val textModel: String get() = when (lang) {
        AppLanguage.CHINESE -> "文本模型"
        AppLanguage.ENGLISH -> "Text Model"
        AppLanguage.ARABIC -> "نموذج النص"
    }

    val imageModelOptional: String get() = when (lang) {
        AppLanguage.CHINESE -> "图像模型（可选）"
        AppLanguage.ENGLISH -> "Image Model (Optional)"
        AppLanguage.ARABIC -> "نموذج الصورة (اختياري)"
    }

    val temperature: String get() = when (lang) {
        AppLanguage.CHINESE -> "温度"
        AppLanguage.ENGLISH -> "Temperature"
        AppLanguage.ARABIC -> "درجة الحرارة"
    }

    val temperatureHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "低(0): 确定性输出 - 高(2): 创意性输出"
        AppLanguage.ENGLISH -> "Low(0): Deterministic - High(2): Creative"
        AppLanguage.ARABIC -> "منخفض(0): حتمي - مرتفع(2): إبداعي"
    }

    val toolbox: String get() = when (lang) {
        AppLanguage.CHINESE -> "工具包"
        AppLanguage.ENGLISH -> "Toolbox"
        AppLanguage.ARABIC -> "صندوق الأدوات"
    }

    val nEnabled: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 个已启用"
        AppLanguage.ENGLISH -> "%d enabled"
        AppLanguage.ARABIC -> "%d مفعّل"
    }

    val toolboxHint: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择 AI 可以使用的工具，启用更多工具可以增强 AI 的能力"
        AppLanguage.ENGLISH -> "Select tools for AI to use, more tools enhance AI capabilities"
        AppLanguage.ARABIC -> "اختر الأدوات التي يمكن للذكاء الاصطناعي استخدامها"
    }

    val nMessages: String get() = when (lang) {
        AppLanguage.CHINESE -> "%d 条消息"
        AppLanguage.ENGLISH -> "%d messages"
        AppLanguage.ARABIC -> "%d رسالة"
    }

    val dataBackupTitle: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据备份"
        AppLanguage.ENGLISH -> "Data Backup"
        AppLanguage.ARABIC -> "نسخ البيانات احتياطيًا"
    }

    val dataBackupDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出或导入所有应用数据，包括配置、图标、启动画面、BGM等资源文件"
        AppLanguage.ENGLISH -> "Export or import all app data including config, icons, splash screens, BGM and other resources"
        AppLanguage.ARABIC -> "تصدير أو استيراد جميع بيانات التطبيق"
    }

    val dataBackupNote: String get() = when (lang) {
        AppLanguage.CHINESE -> "导入数据会添加新应用，不会覆盖现有数据。建议在更新应用前先导出备份。"
        AppLanguage.ENGLISH -> "Importing data adds new apps without overwriting existing data. It's recommended to export backup before updating."
        AppLanguage.ARABIC -> "استيراد البيانات يضيف تطبيقات جديدة دون الكتابة فوق البيانات الموجودة."
    }

    val legalDisclaimer: String get() = when (lang) {
        AppLanguage.CHINESE -> "法律声明与免责条款"
        AppLanguage.ENGLISH -> "Legal Disclaimer"
        AppLanguage.ARABIC -> "إخلاء المسؤولية القانونية"
    }

    val disclaimerWarningText: String get() = when (lang) {
        AppLanguage.CHINESE -> "⚠️ 重要提示：请务必仔细阅读以下条款，使用本软件即表示您同意遵守所有条款。"
        AppLanguage.ENGLISH -> "⚠️ Important: Please read the following terms carefully. By using this software, you agree to comply with all terms."
        AppLanguage.ARABIC -> "⚠️ هام: يرجى قراءة الشروط التالية بعناية. باستخدام هذا البرنامج، فإنك توافق على الالتزام بجميع الشروط."
    }

    val finalUserAgreementConfirmation: String get() = when (lang) {
        AppLanguage.CHINESE -> "📋 用户确认声明"
        AppLanguage.ENGLISH -> "📋 User Confirmation Statement"
        AppLanguage.ARABIC -> "📋 بيان تأكيد المستخدم"
    }
    // ==================== AI Scenarios ====================
    val featureAiCoding: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML 编程"
        AppLanguage.ENGLISH -> "HTML Coding"
        AppLanguage.ARABIC -> "برمجة HTML"
    }

    val featureAiCodingDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 辅助生成和修改 HTML/CSS/JS 代码"
        AppLanguage.ENGLISH -> "AI-assisted HTML/CSS/JS code generation and modification"
        AppLanguage.ARABIC -> "إنشاء وتعديل كود HTML/CSS/JS بمساعدة الذكاء الاصطناعي"
    }

    val featureAiCodingImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML 编程（图像）"
        AppLanguage.ENGLISH -> "HTML Coding (Image)"
        AppLanguage.ARABIC -> "برمجة HTML (صورة)"
    }

    val featureAiCodingImageDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML 编程中的图像生成功能"
        AppLanguage.ENGLISH -> "Image generation for HTML coding"
        AppLanguage.ARABIC -> "إنشاء الصور لبرمجة HTML"
    }

    val featureIconGen: String get() = when (lang) {
        AppLanguage.CHINESE -> "图标生成"
        AppLanguage.ENGLISH -> "Icon Generation"
        AppLanguage.ARABIC -> "إنشاء الأيقونات"
    }

    val featureIconGenDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 AI 生成应用图标"
        AppLanguage.ENGLISH -> "Generate app icons using AI"
        AppLanguage.ARABIC -> "إنشاء أيقونات التطبيق باستخدام الذكاء الاصطناعي"
    }

    val featureModuleDev: String get() = when (lang) {
        AppLanguage.CHINESE -> "模块开发"
        AppLanguage.ENGLISH -> "Module Development"
        AppLanguage.ARABIC -> "تطوير الوحدات"
    }

    val featureModuleDevDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI Agent 辅助开发扩展模块"
        AppLanguage.ENGLISH -> "AI Agent-assisted extension module development"
        AppLanguage.ARABIC -> "تطوير وحدات الامتداد بمساعدة وكيل الذكاء الاصطناعي"
    }

    val featureLrcGen: String get() = when (lang) {
        AppLanguage.CHINESE -> "歌词生成"
        AppLanguage.ENGLISH -> "LRC Generation"
        AppLanguage.ARABIC -> "إنشاء كلمات الأغاني"
    }

    val featureLrcGenDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "AI 生成 LRC 歌词文件"
        AppLanguage.ENGLISH -> "AI-generated LRC lyrics files"
        AppLanguage.ARABIC -> "ملفات كلمات LRC بالذكاء الاصطناعي"
    }

    val featureTranslate: String get() = when (lang) {
        AppLanguage.CHINESE -> "翻译"
        AppLanguage.ENGLISH -> "Translation"
        AppLanguage.ARABIC -> "ترجمة"
    }

    val featureTranslateDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "网页内容翻译"
        AppLanguage.ENGLISH -> "Web content translation"
        AppLanguage.ARABIC -> "ترجمة محتوى الويب"
    }

    val featureGeneral: String get() = when (lang) {
        AppLanguage.CHINESE -> "通用对话"
        AppLanguage.ENGLISH -> "General Chat"
        AppLanguage.ARABIC -> "محادثة عامة"
    }

    val featureGeneralDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "通用 AI 对话功能"
        AppLanguage.ENGLISH -> "General AI chat functionality"
        AppLanguage.ARABIC -> "وظيفة محادثة الذكاء الاصطناعي العامة"
    }
    // ==================== Model Capabilities ====================
    val capabilityText: String get() = when (lang) {
        AppLanguage.CHINESE -> "文本生成"
        AppLanguage.ENGLISH -> "Text Generation"
        AppLanguage.ARABIC -> "إنشاء النص"
    }

    val capabilityTextDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "基础文本对话和生成"
        AppLanguage.ENGLISH -> "Basic text dialogue and generation"
        AppLanguage.ARABIC -> "حوار وإنشاء النص الأساسي"
    }

    val capabilityAudio: String get() = when (lang) {
        AppLanguage.CHINESE -> "音频理解"
        AppLanguage.ENGLISH -> "Audio Understanding"
        AppLanguage.ARABIC -> "فهم الصوت"
    }

    val capabilityAudioDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "理解和转录音频内容"
        AppLanguage.ENGLISH -> "Understand and transcribe audio content"
        AppLanguage.ARABIC -> "فهم ونسخ محتوى الصوت"
    }

    val capabilityImage: String get() = when (lang) {
        AppLanguage.CHINESE -> "图像理解"
        AppLanguage.ENGLISH -> "Image Understanding"
        AppLanguage.ARABIC -> "فهم الصور"
    }

    val capabilityImageDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "理解和分析图像内容"
        AppLanguage.ENGLISH -> "Understand and analyze image content"
        AppLanguage.ARABIC -> "فهم وتحليل محتوى الصور"
    }

    val capabilityImageGen: String get() = when (lang) {
        AppLanguage.CHINESE -> "图像生成"
        AppLanguage.ENGLISH -> "Image Generation"
        AppLanguage.ARABIC -> "إنشاء الصور"
    }

    val capabilityImageGenDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成图像和图标"
        AppLanguage.ENGLISH -> "Generate images and icons"
        AppLanguage.ARABIC -> "إنشاء الصور والأيقونات"
    }

    val capabilityVideo: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频理解"
        AppLanguage.ENGLISH -> "Video Understanding"
        AppLanguage.ARABIC -> "فهم الفيديو"
    }

    val capabilityVideoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "理解视频内容"
        AppLanguage.ENGLISH -> "Understand video content"
        AppLanguage.ARABIC -> "فهم محتوى الفيديو"
    }

    val capabilityCode: String get() = when (lang) {
        AppLanguage.CHINESE -> "代码生成"
        AppLanguage.ENGLISH -> "Code Generation"
        AppLanguage.ARABIC -> "إنشاء الكود"
    }

    val capabilityCodeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成和理解代码"
        AppLanguage.ENGLISH -> "Generate and understand code"
        AppLanguage.ARABIC -> "إنشاء وفهم الكود"
    }

    val capabilityFunctionCall: String get() = when (lang) {
        AppLanguage.CHINESE -> "函数调用"
        AppLanguage.ENGLISH -> "Function Call"
        AppLanguage.ARABIC -> "استدعاء الوظائف"
    }

    val capabilityFunctionCallDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持工具调用"
        AppLanguage.ENGLISH -> "Support tool calling"
        AppLanguage.ARABIC -> "دعم استدعاء الأدوات"
    }

    val capabilityLongContext: String get() = when (lang) {
        AppLanguage.CHINESE -> "长上下文"
        AppLanguage.ENGLISH -> "Long Context"
        AppLanguage.ARABIC -> "سياق طويل"
    }

    val capabilityLongContextDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "支持超长文本输入"
        AppLanguage.ENGLISH -> "Support extra long text input"
        AppLanguage.ARABIC -> "دعم إدخال النص الطويل جداً"
    }
}
