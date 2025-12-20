package com.webtoapp.core.ai.htmlcoding

/**
 * HTML编程AI - 提示词与模板管理
 */
object HtmlCodingPrompts {

    /**
     * 构建系统提示词
     * 采用结构化思维链设计，提升模型输出质量
     */
    fun buildSystemPrompt(
        config: SessionConfig,
        hasImageModel: Boolean = false,
        selectedTemplate: StyleTemplate? = null,
        selectedStyle: StyleReference? = null
    ): String {
        val sb = StringBuilder()
        
        // ===== 角色定义 =====
        sb.appendLine("""
# 角色设定

你是一位专业的 **HTML/CSS/JavaScript 全栈前端开发专家**，具备以下核心能力：

- **精通现代前端技术**：HTML5、CSS3、JavaScript ES6+、SVG、Canvas
- **熟练使用主流框架**：TailwindCSS、Bootstrap、Alpine.js
- **具备优秀的UI/UX设计能力**：响应式设计、动画交互、用户体验优化
- **拥有丰富的项目经验**：能够理解需求并转化为高质量代码

你的目标是帮助用户创建**完整、可运行、高质量**的HTML项目。
        """.trimIndent())

        // ===== 核心原则 =====
        sb.appendLine("""

# 核心原则

## 1. 移动端优先（最重要）
你生成的所有代码都将运行在**手机APP**中，必须严格遵守以下移动端规范：

- **手机屏幕比例**：设计基于手机竖屏比例（9:16或类似），宽度以100vw为基准
- **触摸交互优先**：
  - 所有可点击元素最小尺寸 44x44px，确保手指可准确点击
  - 使用 touch-action 优化触摸行为
  - 按钮间距至少 8px，防止误触
  - 禁止使用 hover 作为主要交互方式（可作为增强，但不依赖）
- **视口配置**：必须包含 `<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">`
- **禁止横向滚动**：使用 `overflow-x: hidden` 和 `max-width: 100vw`
- **安全区域**：考虑手机刘海屏和底部手势区域，使用 `env(safe-area-inset-*)`
- **字体大小**：正文不小于 14px，标题不小于 18px，确保可读性
- **全屏布局**：默认使用 `min-height: 100vh` 铺满屏幕

## 2. 代码质量要求
- **完整性**：生成的代码必须是完整可运行的，包含所有必要的标签、样式和脚本
- **三文件分离**：将 HTML、CSS、JavaScript 分成三个独立文件输出，便于管理和维护
- **现代实践**：使用语义化HTML标签、CSS变量、Flexbox/Grid布局
- **移动端适配**：使用 vw/vh/rem 等相对单位，禁止使用固定像素大小的布局

## 3. 禁止事项（严格遵守）
- **禁止生成模拟数据或占位内容**（除非用户明确要求）
- **禁止省略代码**：不允许使用"..."、"// 其他代码"等省略符号
- **禁止假设外部依赖**：如需CDN资源，使用可靠的公共CDN
- **禁止生成不完整的代码片段**：每次输出必须是可直接使用的完整代码
- **禁止桌面端思维**：不要使用 hover 悬停效果作为核心交互，不要使用过小的点击目标

## 4. 思维链要求
在生成代码前，请按以下步骤思考：

```thinking
1. 【需求分析】理解用户真正想要什么
2. 【移动端考量】这是手机APP，如何适配触摸和屏幕比例
3. 【方案设计】确定技术方案和实现路径
4. 【结构规划】规划HTML结构、CSS布局、JS逻辑（三文件分离）
5. 【细节考量】考虑边界情况、触摸体验、性能
6. 【代码实现】按HTML→CSS→JS顺序分别生成完整代码
```
        """.trimIndent())

        // ===== 交互规范 =====
        sb.appendLine("""

# 交互规范

## 需求澄清
当遇到以下情况时，**必须先提出疑问和方案建议**，而不是盲目执行：

1. **需求不明确**：关键信息缺失时，列出需要确认的问题
2. **需求不合理**：技术上难以实现或存在更好方案时，说明原因并提供替代方案
3. **需求有歧义**：可能有多种理解时，列出不同解读并询问用户意图
4. **功能冲突**：新需求与已有功能冲突时，说明影响并征求意见

## 回复格式（三文件分离输出）

对于代码生成请求，**必须**按以下格式输出三个独立文件：

### 1. HTML文件（必需）
```html
<!-- 文件名: index.html -->
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>应用标题</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <!-- 页面结构 -->
    <script src="script.js"></script>
</body>
</html>
```

### 2. CSS文件（必需）
```css
/* 文件名: styles.css */
/* 移动端基础重置 */
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
    -webkit-tap-highlight-color: transparent;
}

html, body {
    width: 100%;
    min-height: 100vh;
    overflow-x: hidden;
}

/* 安全区域适配 */
body {
    padding: env(safe-area-inset-top) env(safe-area-inset-right) env(safe-area-inset-bottom) env(safe-area-inset-left);
}

/* 触摸优化 */
button, a, [onclick] {
    min-width: 44px;
    min-height: 44px;
    touch-action: manipulation;
}

/* ...其他样式... */
```

### 3. JavaScript文件（如需交互则必需，否则可为空）
```javascript
// 文件名: script.js
// 使用触摸事件而非点击事件以获得更好的响应
document.addEventListener('DOMContentLoaded', function() {
    // 初始化代码
});

// ...其他逻辑...
```

### 重要说明
- **始终输出三个文件**：即使CSS或JS内容很少，也要分开输出
- **HTML中通过link和script引用**：不要内嵌样式和脚本
- **文件名固定**：使用 index.html、styles.css、script.js
        """.trimIndent())

        // ===== 用户规则 =====
        if (config.rules.isNotEmpty()) {
            sb.appendLine("""

# 用户自定义规则（必须严格遵守）

${config.rules.mapIndexed { index, rule -> "${index + 1}. $rule" }.joinToString("\n")}
            """.trimIndent())
        }

        // ===== 模板上下文 =====
        selectedTemplate?.let { template ->
            sb.appendLine("""

# 当前选择的模板风格

- **模板名称**：${template.name}
- **风格分类**：${template.category.displayName}
- **风格描述**：${template.description}
${template.cssFramework?.let { "- **CSS框架**：$it" } ?: ""}
${template.colorScheme?.let { """
- **配色方案**：
  - 主色：${it.primary}
  - 次色：${it.secondary}
  - 背景：${it.background}
  - 文字：${it.text}
  - 强调：${it.accent}
""" } ?: ""}
- **设计提示**：${template.promptHint}

请在生成代码时参考此模板风格，保持视觉一致性。
            """.trimIndent())
        }

        // ===== 风格参考 =====
        selectedStyle?.let { style ->
            sb.appendLine("""

# 当前选择的风格参考

- **风格名称**：${style.name}
- **来源分类**：${style.category.displayName}
- **风格描述**：${style.description}
- **关键词**：${style.keywords.joinToString("、")}
- **配色提示**：${style.colorHints.joinToString("、")}
- **元素提示**：${style.elementHints.joinToString("、")}

请充分理解并模仿这种风格的视觉语言和设计感觉。
            """.trimIndent())
        }

        // ===== 图像模型配合 =====
        if (hasImageModel) {
            sb.appendLine("""

# 图像生成能力

你现在拥有**图像生成**能力。当需要生成图片时，请按以下格式输出：

## 图像生成格式

```image-gen
{
  "prompt": "详细的图像描述，使用英文",
  "negative_prompt": "不需要的元素",
  "width": 512,
  "height": 512,
  "style": "可选风格标签"
}
```

## 图像提示词编写指南

1. **主体描述**：明确描述图像主体，如 "a modern website hero image"
2. **风格修饰**：添加风格词，如 "minimalist, flat design, gradient colors"
3. **质量词**：使用 "high quality, professional, clean" 等
4. **技术规格**：如需要，指定 "vector style, icon design, illustration"

## 在代码中使用生成的图像

生成图像后，会返回图像路径。请在HTML中这样使用：

```html
<img src="[生成的图像路径]" alt="描述文字" />
```

## 最佳实践

- 图标/Logo：使用 SVG 代码直接内嵌，而非生成图片
- 背景图：可以生成，注意配合CSS背景属性使用
- 插图：详细描述场景和风格，确保与整体设计协调
            """.trimIndent())
        }

        return sb.toString()
    }

    /**
     * 预置的风格模板库
     */
    val styleTemplates = listOf(
        // ===== 现代简约 =====
        StyleTemplate(
            id = "modern-minimal",
            name = "现代简约",
            category = TemplateCategory.MODERN,
            description = "干净利落的现代设计，大量留白，强调内容本身",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#3B82F6",
                secondary = "#6366F1",
                background = "#FFFFFF",
                surface = "#F9FAFB",
                text = "#111827",
                accent = "#10B981"
            ),
            promptHint = "使用大量留白、简洁的排版、柔和的阴影、圆角元素",
            exampleCode = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>现代简约风格</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-white text-gray-900">
    <div class="min-h-screen flex items-center justify-center">
        <div class="max-w-md p-8 bg-gray-50 rounded-2xl shadow-lg">
            <h1 class="text-2xl font-bold mb-4">欢迎</h1>
            <p class="text-gray-600">这是一个现代简约风格的示例。</p>
        </div>
    </div>
</body>
</html>
            """.trimIndent()
        ),

        // ===== 玻璃拟态 =====
        StyleTemplate(
            id = "glassmorphism",
            name = "玻璃拟态",
            category = TemplateCategory.GLASSMORPHISM,
            description = "透明模糊效果，创造深度层次感的现代UI风格",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#667EEA",
                secondary = "#764BA2",
                background = "linear-gradient(135deg, #667EEA 0%, #764BA2 100%)",
                surface = "rgba(255, 255, 255, 0.25)",
                text = "#FFFFFF",
                accent = "#F093FB"
            ),
            promptHint = "使用backdrop-filter: blur()、半透明白色背景、渐变色背景、柔和边框",
            exampleCode = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>玻璃拟态风格</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
            font-family: system-ui, sans-serif;
        }
        .glass-card {
            background: rgba(255, 255, 255, 0.25);
            backdrop-filter: blur(10px);
            border-radius: 20px;
            border: 1px solid rgba(255, 255, 255, 0.18);
            padding: 40px;
            color: white;
            box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.37);
        }
    </style>
</head>
<body>
    <div class="glass-card">
        <h1>玻璃拟态</h1>
        <p>透明模糊的现代美感</p>
    </div>
</body>
</html>
            """.trimIndent()
        ),

        // ===== 新拟物 =====
        StyleTemplate(
            id = "neumorphism",
            name = "新拟物化",
            category = TemplateCategory.NEUMORPHISM,
            description = "软阴影创造的凸起/凹陷效果，柔和温润的触感设计",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#6C63FF",
                secondary = "#A29BFE",
                background = "#E0E5EC",
                surface = "#E0E5EC",
                text = "#495057",
                accent = "#6C63FF"
            ),
            promptHint = "使用双层阴影（亮/暗）、柔和的背景色、圆角、凸起或凹陷的按钮效果",
            exampleCode = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>新拟物风格</title>
    <style>
        body {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            background: #E0E5EC;
            font-family: system-ui, sans-serif;
        }
        .neu-card {
            background: #E0E5EC;
            border-radius: 20px;
            padding: 40px;
            box-shadow: 9px 9px 16px #b8bcc2, -9px -9px 16px #ffffff;
        }
        .neu-button {
            background: #E0E5EC;
            border: none;
            border-radius: 12px;
            padding: 15px 30px;
            cursor: pointer;
            box-shadow: 5px 5px 10px #b8bcc2, -5px -5px 10px #ffffff;
            transition: all 0.2s;
        }
        .neu-button:active {
            box-shadow: inset 5px 5px 10px #b8bcc2, inset -5px -5px 10px #ffffff;
        }
    </style>
</head>
<body>
    <div class="neu-card">
        <h1>新拟物化</h1>
        <button class="neu-button">点击我</button>
    </div>
</body>
</html>
            """.trimIndent()
        ),

        // ===== 暗黑主题 =====
        StyleTemplate(
            id = "dark-mode",
            name = "暗黑主题",
            category = TemplateCategory.DARK,
            description = "深色背景配亮色文字，护眼且现代的设计风格",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#818CF8",
                secondary = "#34D399",
                background = "#0F172A",
                surface = "#1E293B",
                text = "#F1F5F9",
                accent = "#F472B6"
            ),
            promptHint = "深色背景、亮色文字和强调色、柔和的发光效果、高对比度",
            exampleCode = null
        ),

        // ===== 赛博朋克 =====
        StyleTemplate(
            id = "cyberpunk",
            name = "赛博朋克",
            category = TemplateCategory.CYBERPUNK,
            description = "霓虹灯效、科技感、未来主义的视觉风格",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#FF00FF",
                secondary = "#00FFFF",
                background = "#0A0A0A",
                surface = "#1A1A2E",
                text = "#EAEAEA",
                accent = "#FFE600"
            ),
            promptHint = "霓虹色彩、发光效果、故障艺术元素、网格线条、科技感字体",
            exampleCode = null
        ),

        // ===== 渐变炫彩 =====
        StyleTemplate(
            id = "gradient",
            name = "渐变炫彩",
            category = TemplateCategory.GRADIENT,
            description = "丰富的渐变色彩，活力四射的视觉效果",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#EC4899",
                secondary = "#8B5CF6",
                background = "linear-gradient(to right, #EC4899, #8B5CF6)",
                surface = "#FFFFFF",
                text = "#1F2937",
                accent = "#F59E0B"
            ),
            promptHint = "多彩渐变、流动感、动态背景、圆润形状、活力配色",
            exampleCode = null
        ),

        // ===== 极简风格 =====
        StyleTemplate(
            id = "minimal",
            name = "极简主义",
            category = TemplateCategory.MINIMAL,
            description = "去除一切不必要的装饰，只保留核心内容",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#000000",
                secondary = "#666666",
                background = "#FFFFFF",
                surface = "#FAFAFA",
                text = "#000000",
                accent = "#000000"
            ),
            promptHint = "黑白配色、大量空白、清晰的排版层次、无装饰元素",
            exampleCode = null
        ),

        // ===== 自然清新 =====
        StyleTemplate(
            id = "nature",
            name = "自然清新",
            category = TemplateCategory.NATURE,
            description = "来自自然的配色灵感，给人宁静舒适的感觉",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#059669",
                secondary = "#0D9488",
                background = "#ECFDF5",
                surface = "#FFFFFF",
                text = "#064E3B",
                accent = "#F59E0B"
            ),
            promptHint = "绿色系、自然元素、圆润形状、柔和阴影、有机线条",
            exampleCode = null
        ),

        // ===== 复古怀旧 =====
        StyleTemplate(
            id = "retro",
            name = "复古怀旧",
            category = TemplateCategory.MODERN,
            description = "80/90年代复古风格，像素艺术与怀旧色调",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#E63946",
                secondary = "#F4A261",
                background = "#FDF6E3",
                surface = "#FFFBEB",
                text = "#1D3557",
                accent = "#2A9D8F"
            ),
            promptHint = "复古色调、像素元素、CRT效果、怀旧字体、磁带/胶片质感",
            exampleCode = null
        ),

        // ===== 卡通可爱 =====
        StyleTemplate(
            id = "cute-cartoon",
            name = "卡通可爱",
            category = TemplateCategory.MODERN,
            description = "萌系卡通风格，圆润可爱的视觉效果",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#FF6B9D",
                secondary = "#C44569",
                background = "#FFF5F7",
                surface = "#FFFFFF",
                text = "#4A4A4A",
                accent = "#FFD93D"
            ),
            promptHint = "圆角元素、柔和阴影、可爱图标、糖果色、气泡效果",
            exampleCode = null
        ),

        // ===== 科技商务 =====
        StyleTemplate(
            id = "tech-business",
            name = "科技商务",
            category = TemplateCategory.MODERN,
            description = "专业科技感，适合企业和产品展示",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#2563EB",
                secondary = "#3B82F6",
                background = "#F8FAFC",
                surface = "#FFFFFF",
                text = "#0F172A",
                accent = "#06B6D4"
            ),
            promptHint = "专业排版、数据可视化、科技图标、网格系统、蓝色调",
            exampleCode = null
        ),

        // ===== 日式和风 =====
        StyleTemplate(
            id = "japanese",
            name = "日式和风",
            category = TemplateCategory.NATURE,
            description = "传统日式美学，禅意与留白的艺术",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#B91C1C",
                secondary = "#1F2937",
                background = "#FFFBEB",
                surface = "#FEF3C7",
                text = "#1F2937",
                accent = "#059669"
            ),
            promptHint = "和纹图案、留白构图、毛笔字体、樱花/波浪元素、纸张质感",
            exampleCode = null
        ),

        // ===== 蒸汽波 =====
        StyleTemplate(
            id = "vaporwave",
            name = "蒸汽波",
            category = TemplateCategory.CYBERPUNK,
            description = "80年代怀旧与未来主义的融合，迷幻视觉",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#FF71CE",
                secondary = "#01CDFE",
                background = "#1A1A2E",
                surface = "#16213E",
                text = "#FFFFFF",
                accent = "#B967FF"
            ),
            promptHint = "粉蓝紫渐变、网格地面、落日、希腊雕像、故障效果",
            exampleCode = null
        ),

        // ===== 材质设计 =====
        StyleTemplate(
            id = "material",
            name = "Material 3",
            category = TemplateCategory.MODERN,
            description = "Google Material Design 3 风格",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#6750A4",
                secondary = "#958DA5",
                background = "#FFFBFE",
                surface = "#FFFBFE",
                text = "#1C1B1F",
                accent = "#B4A7D6"
            ),
            promptHint = "圆角卡片、动态色彩、涟漪效果、elevation阴影、流畅动画",
            exampleCode = null
        ),

        // ===== 手绘素描 =====
        StyleTemplate(
            id = "hand-drawn",
            name = "手绘素描",
            category = TemplateCategory.MINIMAL,
            description = "手绘涂鸦风格，充满艺术感和个性",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#2D3748",
                secondary = "#4A5568",
                background = "#FFFEF0",
                surface = "#FFFEF0",
                text = "#1A202C",
                accent = "#E53E3E"
            ),
            promptHint = "手绘边框、涂鸦线条、铅笔质感、不规则形状、纸张纹理",
            exampleCode = null
        ),

        // ===== 霓虹灯光 =====
        StyleTemplate(
            id = "neon-glow",
            name = "霓虹灯光",
            category = TemplateCategory.DARK,
            description = "发光霓虹效果，夜店风格的炫酷视觉",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#00F5FF",
                secondary = "#FF00E4",
                background = "#0D0D0D",
                surface = "#1A1A1A",
                text = "#FFFFFF",
                accent = "#39FF14"
            ),
            promptHint = "发光文字、霓虹边框、暗色背景、高对比度、闪烁动画",
            exampleCode = null
        ),

        // ===== 水彩画风 =====
        StyleTemplate(
            id = "watercolor",
            name = "水彩画风",
            category = TemplateCategory.NATURE,
            description = "柔和水彩渲染效果，艺术感十足",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#7C9885",
                secondary = "#D4A5A5",
                background = "#FDFCFB",
                surface = "#FFFFFF",
                text = "#4A4A4A",
                accent = "#E8B4B8"
            ),
            promptHint = "水彩渐变、柔和边缘、花卉元素、淡雅色调、纸张纹理",
            exampleCode = null
        )
    )

    /**
     * 风格参考词库
     */
    val styleReferences = listOf(
        // ===== 电影风格 =====
        StyleReference(
            id = "harry-potter",
            name = "哈利波特风格",
            category = StyleReferenceCategory.MOVIE,
            keywords = listOf("魔法", "古典", "神秘", "霍格沃茨", "羊皮纸"),
            description = "充满魔法学院气息，古老神秘的英伦风格，使用羊皮纸质感和中世纪元素",
            colorHints = listOf("深红", "金色", "深棕", "墨绿", "紫色"),
            elementHints = listOf("盾徽", "羽毛笔", "蜡封", "卷轴边框", "哥特字体")
        ),
        StyleReference(
            id = "avatar",
            name = "阿凡达风格",
            category = StyleReferenceCategory.MOVIE,
            keywords = listOf("潘多拉", "生物发光", "外星", "自然", "科幻"),
            description = "潘多拉星球的奇幻世界，荧光色彩与有机形态的结合",
            colorHints = listOf("荧光蓝", "紫色", "青色", "发光绿", "深蓝"),
            elementHints = listOf("发光植物", "漂浮元素", "有机曲线", "透明质感", "粒子效果")
        ),
        StyleReference(
            id = "marvel",
            name = "漫威风格",
            category = StyleReferenceCategory.MOVIE,
            keywords = listOf("超级英雄", "漫画", "动感", "力量", "科技"),
            description = "超级英雄世界的视觉语言，充满力量感和动态效果",
            colorHints = listOf("红色", "金色", "深蓝", "黑色", "银色"),
            elementHints = listOf("动态线条", "漫画分格", "能量光效", "金属质感", "徽章图案")
        ),
        StyleReference(
            id = "blade-runner",
            name = "银翼杀手风格",
            category = StyleReferenceCategory.MOVIE,
            keywords = listOf("赛博朋克", "反乌托邦", "霓虹", "雨夜", "未来"),
            description = "经典赛博朋克美学，雨夜中的霓虹城市",
            colorHints = listOf("霓虹粉", "电子蓝", "橙红", "深黑", "雾灰"),
            elementHints = listOf("霓虹灯牌", "雨滴效果", "全息投影", "日文字符", "高对比度")
        ),
        StyleReference(
            id = "spider-man",
            name = "蜘蛛侠风格",
            category = StyleReferenceCategory.MOVIE,
            keywords = listOf("蛛网", "城市", "青春", "活力", "街头"),
            description = "纽约街头的青春活力，蛛网图案与城市元素的融合",
            colorHints = listOf("红色", "蓝色", "黑色", "白色", "金色"),
            elementHints = listOf("蛛网图案", "城市天际线", "动态姿态", "漫画效果", "街头涂鸦")
        ),

        // ===== 书籍风格 =====
        StyleReference(
            id = "zarathustra",
            name = "查拉图斯特拉风格",
            category = StyleReferenceCategory.BOOK,
            keywords = listOf("哲学", "深邃", "超人", "山巅", "寓言"),
            description = "尼采哲学的视觉表达，深邃、庄严、充满象征意义",
            colorHints = listOf("深紫", "金色", "黑色", "暗红", "铜色"),
            elementHints = listOf("山峰", "太阳", "鹰与蛇", "古典字体", "庄严布局")
        ),
        StyleReference(
            id = "charlie-ix",
            name = "查理九世风格",
            category = StyleReferenceCategory.BOOK,
            keywords = listOf("悬疑", "冒险", "诡异", "少年", "解谜"),
            description = "带有悬疑冒险色彩的少年风格，神秘而不恐怖",
            colorHints = listOf("暗紫", "墨绿", "金色", "暗红", "米黄"),
            elementHints = listOf("放大镜", "密码符号", "古老钥匙", "卷轴", "神秘图腾")
        ),
        StyleReference(
            id = "little-prince",
            name = "小王子风格",
            category = StyleReferenceCategory.BOOK,
            keywords = listOf("童话", "星空", "纯真", "水彩", "诗意"),
            description = "充满诗意的童话风格，如水彩画般温柔梦幻",
            colorHints = listOf("星空蓝", "沙漠金", "玫瑰红", "柔和黄", "淡紫"),
            elementHints = listOf("星星", "玫瑰", "狐狸", "小行星", "水彩纹理")
        ),

        // ===== 动画风格 =====
        StyleReference(
            id = "ghibli",
            name = "吉卜力风格",
            category = StyleReferenceCategory.ANIME,
            keywords = listOf("宫崎骏", "自然", "温暖", "手绘", "治愈"),
            description = "吉卜力动画的温暖世界，充满自然与人文关怀",
            colorHints = listOf("天空蓝", "草绿", "泥土棕", "云白", "夕阳橙"),
            elementHints = listOf("云朵", "绿植", "小屋", "手绘质感", "柔和光影")
        ),
        StyleReference(
            id = "cyberpunk-edgerunners",
            name = "赛博朋克：边缘行者风格",
            category = StyleReferenceCategory.ANIME,
            keywords = listOf("霓虹", "暴力美学", "夜城", "机械", "反叛"),
            description = "夜城的躁动与绚烂，强烈的视觉冲击",
            colorHints = listOf("霓虹粉", "电光蓝", "血红", "黑色", "黄色"),
            elementHints = listOf("故障效果", "霓虹轮廓", "像素元素", "暴力美学", "机械感")
        ),
        StyleReference(
            id = "your-name",
            name = "你的名字风格",
            category = StyleReferenceCategory.ANIME,
            keywords = listOf("新海诚", "光影", "青春", "唯美", "星空"),
            description = "新海诚式的光影美学，极致的背景艺术",
            colorHints = listOf("黄昏橙", "天际蓝", "星光紫", "晨曦粉", "云层白"),
            elementHints = listOf("光斑", "彗星", "黄昏", "城市远景", "细腻光影")
        ),

        // ===== 游戏风格 =====
        StyleReference(
            id = "zelda-botw",
            name = "塞尔达荒野之息风格",
            category = StyleReferenceCategory.GAME,
            keywords = listOf("冒险", "自然", "卡通渲染", "探索", "开放世界"),
            description = "海拉鲁的广袤世界，清新的卡通渲染风格",
            colorHints = listOf("草原绿", "天空蓝", "山岩灰", "篝火橙", "古迹棕"),
            elementHints = listOf("希卡符文", "远景山脉", "卡通渲染", "自然元素", "冒险图标")
        ),
        StyleReference(
            id = "hollow-knight",
            name = "空洞骑士风格",
            category = StyleReferenceCategory.GAME,
            keywords = listOf("手绘", "昆虫", "地下", "忧郁", "精致"),
            description = "圣巢的深邃世界，精致的手绘美术风格",
            colorHints = listOf("深蓝", "黑色", "灵魂白", "苔藓绿", "感染橙"),
            elementHints = listOf("昆虫", "荆棘", "水墨风格", "精致线条", "忧郁氛围")
        ),

        // ===== 艺术流派 =====
        StyleReference(
            id = "art-deco",
            name = "装饰艺术风格",
            category = StyleReferenceCategory.ART,
            keywords = listOf("几何", "对称", "奢华", "1920s", "金属"),
            description = "1920年代的装饰艺术运动，几何图案与奢华质感",
            colorHints = listOf("金色", "黑色", "翡翠绿", "深蓝", "象牙白"),
            elementHints = listOf("几何图案", "对称布局", "扇形", "金属线条", "奢华装饰")
        ),
        StyleReference(
            id = "japanese-ukiyo-e",
            name = "浮世绘风格",
            category = StyleReferenceCategory.ART,
            keywords = listOf("和风", "浪", "木刻", "传统", "平面"),
            description = "日本传统浮世绘的视觉语言，平面化的独特美感",
            colorHints = listOf("靛蓝", "朱红", "米白", "墨黑", "樱粉"),
            elementHints = listOf("波浪", "樱花", "和纹", "平涂色块", "黑色轮廓")
        ),

        // ===== 品牌风格 =====
        StyleReference(
            id = "apple",
            name = "苹果风格",
            category = StyleReferenceCategory.BRAND,
            keywords = listOf("极简", "优雅", "科技", "精致", "高端"),
            description = "苹果公司的设计语言，极致简约与精致工艺",
            colorHints = listOf("纯白", "深空灰", "银色", "金色", "午夜黑"),
            elementHints = listOf("大量留白", "精确对齐", "微妙渐变", "圆角", "高端材质")
        ),
        StyleReference(
            id = "spotify",
            name = "Spotify风格",
            category = StyleReferenceCategory.BRAND,
            keywords = listOf("音乐", "活力", "渐变", "现代", "年轻"),
            description = "Spotify的年轻活力设计，鲜明的品牌色彩",
            colorHints = listOf("Spotify绿", "深黑", "紫粉渐变", "橙黄渐变", "白色"),
            elementHints = listOf("双色渐变", "动态波形", "卡片布局", "圆形元素", "活力插画")
        )
    )

    /**
     * Rules 模板库
     */
    val rulesTemplates = listOf(
        RulesTemplate(
            id = "default",
            name = "默认规则（推荐）",
            description = "移动端APP开发规则",
            rules = listOf(
                "使用中文进行对话",
                "代码运行在手机APP中，必须针对手机屏幕比例设计",
                "所有可点击元素最小44x44px，优化触摸体验",
                "输出HTML、CSS、JS三个独立文件",
                "使用vw/vh/rem等相对单位，禁止固定像素布局",
                "禁止依赖hover交互，使用touch事件"
            )
        ),
        RulesTemplate(
            id = "strict-quality",
            name = "严格质量",
            description = "高质量移动端代码",
            rules = listOf(
                "使用中文进行对话",
                "代码必须包含完整的HTML结构和移动端viewport",
                "所有样式使用CSS变量便于主题切换",
                "严格遵守移动端触摸规范，按钮不小于44px",
                "使用语义化HTML标签",
                "输出HTML、CSS、JS三个独立文件"
            )
        ),
        RulesTemplate(
            id = "tailwind-focus",
            name = "TailwindCSS优先",
            description = "使用TailwindCSS开发",
            rules = listOf(
                "使用中文进行对话",
                "优先使用TailwindCSS工具类",
                "使用CDN引入TailwindCSS",
                "使用Tailwind的响应式前缀适配手机屏幕",
                "CSS文件仅包含自定义样式，Tailwind通过CDN引入",
                "确保触摸目标足够大"
            )
        ),
        RulesTemplate(
            id = "animation-rich",
            name = "动画丰富",
            description = "注重动画交互效果",
            rules = listOf(
                "使用中文进行对话",
                "添加流畅的过渡动画",
                "使用CSS动画而非JavaScript动画",
                "考虑动画性能，使用transform和opacity",
                "为交互元素添加active状态（非hover）",
                "添加页面加载动画",
                "输出HTML、CSS、JS三个独立文件"
            )
        ),
        RulesTemplate(
            id = "touch-game",
            name = "触摸游戏开发",
            description = "适用于手机触摸游戏",
            rules = listOf(
                "使用中文进行对话",
                "使用Canvas或SVG进行图形渲染",
                "实现requestAnimationFrame游戏循环",
                "使用touch事件处理触摸输入",
                "全屏铺满手机屏幕，禁止滚动",
                "考虑游戏性能优化",
                "输出HTML、CSS、JS三个独立文件"
            )
        ),
        RulesTemplate(
            id = "mini-app",
            name = "小工具应用",
            description = "简单实用的手机小工具",
            rules = listOf(
                "使用中文进行对话",
                "界面简洁，功能聚焦",
                "大按钮大字体，易于触摸操作",
                "全屏布局，充分利用手机屏幕",
                "快速响应，无需加载等待",
                "输出HTML、CSS、JS三个独立文件"
            )
        )
    )

    /**
     * 获取模板通过ID
     */
    fun getTemplateById(id: String): StyleTemplate? = styleTemplates.find { it.id == id }

    /**
     * 获取风格通过ID
     */
    fun getStyleById(id: String): StyleReference? = styleReferences.find { it.id == id }

    /**
     * 获取Rules模板通过ID
     */
    fun getRulesTemplateById(id: String): RulesTemplate? = rulesTemplates.find { it.id == id }

    /**
     * 按分类获取模板
     */
    fun getTemplatesByCategory(category: TemplateCategory): List<StyleTemplate> =
        styleTemplates.filter { it.category == category }

    /**
     * 按分类获取风格参考
     */
    fun getStylesByCategory(category: StyleReferenceCategory): List<StyleReference> =
        styleReferences.filter { it.category == category }
}
