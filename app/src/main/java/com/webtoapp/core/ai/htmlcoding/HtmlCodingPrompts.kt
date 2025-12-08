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

你是一位专业的 **移动端 HTML/CSS/JavaScript 前端开发专家**，具备以下核心能力：

- **专注移动端开发**：所有代码只针对手机端优化，不考虑桌面端
- **精通现代前端技术**：HTML5、CSS3、JavaScript ES6+、SVG、Canvas
- **熟练使用主流框架**：TailwindCSS、Bootstrap、Alpine.js
- **具备优秀的移动端UI/UX设计能力**：响应式设计、触摸交互、手势操作、移动端动画
- **拥有丰富的项目经验**：能够理解需求并转化为高质量代码

你的目标是帮助用户创建**专为手机端设计、完整可运行、高质量**的 HTML 项目。

⚠️ **重要：你生成的所有代码都只需要在手机端运行，完全不需要适配电脑/平板等桌面设备！**
        """.trimIndent())

        // ===== 核心原则 =====
        sb.appendLine("""

# 核心原则

## 1. 代码质量要求
- **完整性**：生成的代码必须是完整可运行的，包含所有必要的标签、样式和脚本
- **单文件优先**：优先将HTML、CSS、JavaScript整合在单个HTML文件中，便于预览和使用
- **现代实践**：使用语义化HTML标签、CSS变量、Flexbox/Grid布局
- **移动端优先**：**务必适配手机端操作**，不需要适配桌面端。所有交互都以触摸操作为主，按钮/点击区域不小于 44px，使用 touch-action 优化触摸体验
- **无障碍性**：添加必要的ARIA属性和alt文本

## 2. 移动端专属要求（必须严格遵守）

**🚫 禁止桌面端适配**：不要写任何 @media (min-width: 768px) 等桌面端媒体查询，不要考虑鼠标悬停效果

**✅ 必须遵守的移动端规范**：
- **视口设置**：必须包含 `<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">`
- **触摸友好**：按钮最小尺寸 44x44px，间距足够防止误触
- **字体大小**：正文不小于 16px，避免 iOS 自动缩放
- **布局方式**：使用 Flexbox/Grid，使用 vw/vh/% 相对单位，宽度用 100% 或 100vw
- **交互方式**：使用 touchstart/touchend/click，禁用长按菜单（-webkit-touch-callout: none）
- **滚动优化**：使用 -webkit-overflow-scrolling: touch，考虑安全区域（safe-area-inset）
- **设计基准**：以 375px 宽度（iPhone SE）为基准设计

## 3. 禁止事项（严格遵守）
- **禁止生成模拟数据或占位内容**（除非用户明确要求）
- **禁止省略代码**：不允许使用"..."、"// 其他代码"等省略符号
- **禁止假设外部依赖**：如需CDN资源，使用可靠的公共CDN
- **禁止生成不完整的代码片段**：每次输出必须是可直接使用的完整代码

## 4. 思维链要求
在生成代码前，请按以下步骤思考：

```thinking
1. 【需求分析】理解用户真正想要什么
2. 【方案设计】确定技术方案和实现路径
3. 【结构规划】规划HTML结构、CSS布局、JS逻辑
4. 【细节考量】考虑边界情况、兼容性、性能
5. 【代码实现】按模块顺序生成代码
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

## 回复格式

### 📝 文字说明使用 Markdown 格式
客户端会自动渲染 Markdown，请积极使用以下格式让回复更清晰：

| 格式 | 语法 | 用途 |
|------|------|------|
| **加粗** | `**文字**` | 强调重点 |
| *斜体* | `*文字*` | 术语、变量名 |
| `行内代码` | \`代码\` | 函数名、属性名 |
| 标题 | `# ## ###` | 章节结构 |
| 列表 | `- 或 1.` | 步骤说明 |
| 表格 | `\| 列 \|` | 对比信息 |
| 引用 | `> 内容` | 提示说明 |

### 📊 使用 Mermaid 绘制图表
当需要解释流程、架构、逻辑关系时，使用 Mermaid 图表会更直观：

```mermaid
graph TD
    A[用户需求] --> B[设计方案]
    B --> C[代码实现]
    C --> D[测试预览]
```

**支持的图表类型**：
- `graph TD/LR` - 流程图（最常用）
- `sequenceDiagram` - 时序图（适合交互流程）
- `classDiagram` - 类图（适合数据结构）
- `pie` - 饼图（适合占比展示）
- `gantt` - 甘特图（适合时间规划）

### 💻 代码块格式（必须遵守）

```html
<!-- 文件名: index.html -->
<!DOCTYPE html>
<html lang="zh-CN">
...完整代码...
</html>
```

### 对于需要多个文件的项目：

```html
<!-- 文件名: index.html -->
...主页面代码...
```

```css
/* 文件名: styles.css */
...样式代码...
```

```javascript
// 文件名: script.js
...脚本代码...
```
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

        // ===== 商务专业 =====
        StyleTemplate(
            id = "business-pro",
            name = "商务专业",
            category = TemplateCategory.BUSINESS,
            description = "正式稳重的企业级设计，专业可信赖",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#1E40AF",
                secondary = "#3730A3",
                background = "#F8FAFC",
                surface = "#FFFFFF",
                text = "#1E293B",
                accent = "#DC2626"
            ),
            promptHint = "蓝色主调、清晰层级、表格数据展示、专业图表风格",
            exampleCode = null
        ),

        // ===== 温暖橙调 =====
        StyleTemplate(
            id = "warm-orange",
            name = "温暖橙调",
            category = TemplateCategory.MODERN,
            description = "活力温暖的橙色系，适合美食、生活类应用",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#EA580C",
                secondary = "#F97316",
                background = "#FFFBEB",
                surface = "#FFFFFF",
                text = "#431407",
                accent = "#65A30D"
            ),
            promptHint = "橙黄暖色、圆润卡片、温馨氛围、美食元素",
            exampleCode = null
        ),

        // ===== 科技蓝光 =====
        StyleTemplate(
            id = "tech-blue",
            name = "科技蓝光",
            category = TemplateCategory.DARK,
            description = "科技感十足的深蓝配色，适合科技产品展示",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#0EA5E9",
                secondary = "#06B6D4",
                background = "#0C1222",
                surface = "#1E293B",
                text = "#E2E8F0",
                accent = "#22D3EE"
            ),
            promptHint = "深蓝背景、霓虹蓝光、科技感线条、数据可视化风格",
            exampleCode = null
        ),

        // ===== 粉紫梦幻 =====
        StyleTemplate(
            id = "pink-dream",
            name = "粉紫梦幻",
            category = TemplateCategory.GRADIENT,
            description = "少女心满满的粉紫渐变，浪漫梦幻的视觉",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#EC4899",
                secondary = "#A855F7",
                background = "#FDF2F8",
                surface = "#FFFFFF",
                text = "#831843",
                accent = "#F472B6"
            ),
            promptHint = "粉紫渐变、柔和曲线、爱心元素、梦幻泡泡",
            exampleCode = null
        ),

        // ===== 复古棕调 =====
        StyleTemplate(
            id = "retro-brown",
            name = "复古棕调",
            category = TemplateCategory.RETRO,
            description = "怀旧复古的棕色调，带有纸张质感",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#92400E",
                secondary = "#A16207",
                background = "#FFFBEB",
                surface = "#FEF3C7",
                text = "#451A03",
                accent = "#B91C1C"
            ),
            promptHint = "羊皮纸质感、复古字体、旧报纸风格、怀旧元素",
            exampleCode = null
        ),

        // ===== 霓虹都市 =====
        StyleTemplate(
            id = "neon-city",
            name = "霓虹都市",
            category = TemplateCategory.CYBERPUNK,
            description = "夜晚都市霓虹灯的视觉效果，活力四射",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#F43F5E",
                secondary = "#8B5CF6",
                background = "#18181B",
                surface = "#27272A",
                text = "#FAFAFA",
                accent = "#22D3EE"
            ),
            promptHint = "多彩霓虹、发光文字、动态边框、城市夜景元素",
            exampleCode = null
        ),

        // ===== 森林绿意 =====
        StyleTemplate(
            id = "forest-green",
            name = "森林绿意",
            category = TemplateCategory.NATURE,
            description = "深邃的森林绿，宁静自然的氛围",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#166534",
                secondary = "#15803D",
                background = "#F0FDF4",
                surface = "#DCFCE7",
                text = "#14532D",
                accent = "#84CC16"
            ),
            promptHint = "深绿色调、树叶纹理、自然光影、环保主题",
            exampleCode = null
        ),

        // ===== 冰川蓝 =====
        StyleTemplate(
            id = "glacier-blue",
            name = "冰川蓝",
            category = TemplateCategory.GLASSMORPHISM,
            description = "清凉透彻的冰蓝色玻璃效果",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#0284C7",
                secondary = "#0891B2",
                background = "linear-gradient(135deg, #0EA5E9 0%, #06B6D4 100%)",
                surface = "rgba(255, 255, 255, 0.2)",
                text = "#FFFFFF",
                accent = "#38BDF8"
            ),
            promptHint = "冰蓝透明、霜冻效果、清凉感、水晶质感",
            exampleCode = null
        ),

        // ===== 日落渐变 =====
        StyleTemplate(
            id = "sunset-gradient",
            name = "日落渐变",
            category = TemplateCategory.GRADIENT,
            description = "温暖的日落色彩渐变，从橙到紫的过渡",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#F97316",
                secondary = "#DB2777",
                background = "linear-gradient(to right, #F97316, #DB2777)",
                surface = "#FFFFFF",
                text = "#1F2937",
                accent = "#FBBF24"
            ),
            promptHint = "日落渐变、温暖色调、柔和过渡、天空元素",
            exampleCode = null
        ),

        // ===== 极光紫 =====
        StyleTemplate(
            id = "aurora-purple",
            name = "极光紫",
            category = TemplateCategory.GRADIENT,
            description = "神秘的极光色彩，紫绿交织的梦幻效果",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#7C3AED",
                secondary = "#10B981",
                background = "linear-gradient(135deg, #7C3AED 0%, #10B981 100%)",
                surface = "rgba(124, 58, 237, 0.2)",
                text = "#FFFFFF",
                accent = "#A78BFA"
            ),
            promptHint = "极光色彩、紫绿渐变、星空元素、神秘感",
            exampleCode = null
        ),

        // ===== 像素复古 =====
        StyleTemplate(
            id = "pixel-retro",
            name = "像素复古",
            category = TemplateCategory.RETRO,
            description = "8-bit游戏风格的像素艺术效果",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#22C55E",
                secondary = "#3B82F6",
                background = "#1A1A2E",
                surface = "#16213E",
                text = "#22C55E",
                accent = "#EF4444"
            ),
            promptHint = "像素字体、8-bit风格、复古游戏元素、方块图形",
            exampleCode = null
        ),

        // ===== 金色奢华 =====
        StyleTemplate(
            id = "gold-luxury",
            name = "金色奢华",
            category = TemplateCategory.BUSINESS,
            description = "高端奢华的金色配色，适合高端品牌",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#B8860B",
                secondary = "#DAA520",
                background = "#1A1A1A",
                surface = "#2D2D2D",
                text = "#FFFFFF",
                accent = "#FFD700"
            ),
            promptHint = "金色渐变、奢华质感、优雅字体、高端装饰",
            exampleCode = null
        ),

        // ===== 海洋深蓝 =====
        StyleTemplate(
            id = "ocean-deep",
            name = "海洋深蓝",
            category = TemplateCategory.NATURE,
            description = "深邃的海洋蓝色，宁静而神秘",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#1E3A8A",
                secondary = "#1D4ED8",
                background = "#EFF6FF",
                surface = "#DBEAFE",
                text = "#1E3A8A",
                accent = "#3B82F6"
            ),
            promptHint = "海洋蓝色、波浪元素、深海感觉、水波纹理",
            exampleCode = null
        ),

        // ===== 薄荷清凉 =====
        StyleTemplate(
            id = "mint-fresh",
            name = "薄荷清凉",
            category = TemplateCategory.MODERN,
            description = "清新的薄荷绿，给人清凉舒适的感觉",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#14B8A6",
                secondary = "#2DD4BF",
                background = "#F0FDFA",
                surface = "#CCFBF1",
                text = "#134E4A",
                accent = "#0D9488"
            ),
            promptHint = "薄荷绿色、清新感觉、圆润设计、自然元素",
            exampleCode = null
        ),

        // ===== 火焰红 =====
        StyleTemplate(
            id = "flame-red",
            name = "火焰红",
            category = TemplateCategory.CREATIVE,
            description = "热情奔放的红色系，充满能量和激情",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#DC2626",
                secondary = "#EA580C",
                background = "#FEF2F2",
                surface = "#FFFFFF",
                text = "#7F1D1D",
                accent = "#F59E0B"
            ),
            promptHint = "火焰红色、热情奔放、动感线条、能量感",
            exampleCode = null
        ),

        // ===== 星空夜色 =====
        StyleTemplate(
            id = "starry-night",
            name = "星空夜色",
            category = TemplateCategory.DARK,
            description = "深邃的星空背景，点缀闪烁的星星",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#6366F1",
                secondary = "#8B5CF6",
                background = "#0F0F23",
                surface = "#1A1A3E",
                text = "#E2E8F0",
                accent = "#FBBF24"
            ),
            promptHint = "星空背景、闪烁星星、深邃夜色、银河元素",
            exampleCode = null
        ),

        // ===== 樱花粉 =====
        StyleTemplate(
            id = "sakura-pink",
            name = "樱花粉",
            category = TemplateCategory.NATURE,
            description = "日式樱花的柔和粉色，浪漫唯美",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#EC4899",
                secondary = "#F472B6",
                background = "#FDF2F8",
                surface = "#FCE7F3",
                text = "#831843",
                accent = "#BE185D"
            ),
            promptHint = "樱花粉色、日式美学、花瓣元素、柔和线条",
            exampleCode = null
        ),

        // ===== 工业金属 =====
        StyleTemplate(
            id = "industrial-metal",
            name = "工业金属",
            category = TemplateCategory.DARK,
            description = "工业风的金属质感，硬朗有力",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#71717A",
                secondary = "#A1A1AA",
                background = "#18181B",
                surface = "#27272A",
                text = "#FAFAFA",
                accent = "#F59E0B"
            ),
            promptHint = "金属质感、工业风格、螺丝铆钉元素、硬朗线条",
            exampleCode = null
        ),

        // ===== 糖果色 =====
        StyleTemplate(
            id = "candy-color",
            name = "糖果色",
            category = TemplateCategory.CREATIVE,
            description = "多彩的糖果配色，活泼可爱的视觉效果",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#F472B6",
                secondary = "#A78BFA",
                background = "#FFFFFF",
                surface = "#FDF2F8",
                text = "#1F2937",
                accent = "#34D399"
            ),
            promptHint = "多彩糖果色、圆润气泡、可爱元素、活泼跳跃",
            exampleCode = null
        ),

        // ===== 游戏风格 =====
        StyleTemplate(
            id = "game-style",
            name = "游戏界面",
            category = TemplateCategory.GAME,
            description = "游戏UI风格设计，充满冒险感",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#FBBF24",
                secondary = "#F97316",
                background = "#1E1B4B",
                surface = "#312E81",
                text = "#FFFFFF",
                accent = "#22D3EE"
            ),
            promptHint = "游戏UI、血条经验条、按钮特效、冒险元素",
            exampleCode = null
        ),

        // ===== 水墨中国风 =====
        StyleTemplate(
            id = "chinese-ink",
            name = "水墨中国风",
            category = TemplateCategory.CREATIVE,
            description = "传统水墨画风格，典雅的东方美学",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#1F2937",
                secondary = "#6B7280",
                background = "#F5F5DC",
                surface = "#FFFEF0",
                text = "#1F2937",
                accent = "#B91C1C"
            ),
            promptHint = "水墨画风、宣纸质感、毛笔字体、山水元素",
            exampleCode = null
        ),

        // ===== 赛博霓虹 =====
        StyleTemplate(
            id = "cyber-neon",
            name = "赛博霓虹",
            category = TemplateCategory.CYBERPUNK,
            description = "强烈的赛博朋克霓虹效果，视觉冲击力强",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#FF0080",
                secondary = "#00FFFF",
                background = "#000000",
                surface = "#0D0D0D",
                text = "#FFFFFF",
                accent = "#FFFF00"
            ),
            promptHint = "强霓虹效果、故障艺术、扫描线、赛博字体",
            exampleCode = null
        ),

        // ===== 莫兰迪色 =====
        StyleTemplate(
            id = "morandi",
            name = "莫兰迪色",
            category = TemplateCategory.MINIMAL,
            description = "柔和高级的莫兰迪色系，低饱和度美学",
            cssFramework = "TailwindCSS",
            colorScheme = ColorScheme(
                primary = "#9CA3AF",
                secondary = "#A8A29E",
                background = "#F5F5F4",
                surface = "#FAFAF9",
                text = "#57534E",
                accent = "#78716C"
            ),
            promptHint = "低饱和度、莫兰迪色、柔和灰调、高级质感",
            exampleCode = null
        ),

        // ===== 彩虹渐变 =====
        StyleTemplate(
            id = "rainbow-gradient",
            name = "彩虹渐变",
            category = TemplateCategory.GRADIENT,
            description = "全光谱彩虹渐变，七彩斑斓的视觉效果",
            cssFramework = "Custom CSS",
            colorScheme = ColorScheme(
                primary = "#EF4444",
                secondary = "#3B82F6",
                background = "linear-gradient(90deg, #EF4444, #F97316, #FBBF24, #22C55E, #3B82F6, #8B5CF6)",
                surface = "#FFFFFF",
                text = "#1F2937",
                accent = "#22C55E"
            ),
            promptHint = "彩虹渐变、多彩边框、光谱效果、动态色彩",
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
            name = "默认规则",
            description = "基础对话规则",
            rules = listOf(
                "使用中文进行对话",
                "生成完整可运行的代码",
                "使用现代CSS特性如Flexbox和Grid"
            )
        ),
        RulesTemplate(
            id = "strict-quality",
            name = "严格质量",
            description = "高质量代码输出规则",
            rules = listOf(
                "使用中文进行对话",
                "代码必须包含完整的HTML结构",
                "所有样式使用CSS变量便于主题切换",
                "必须考虑移动端适配",
                "添加必要的注释说明",
                "使用语义化HTML标签"
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
                "避免自定义CSS，除非必要",
                "使用Tailwind的响应式前缀"
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
                "为交互元素添加hover和active状态",
                "添加页面加载动画"
            )
        ),
        RulesTemplate(
            id = "accessibility",
            name = "无障碍优先",
            description = "注重可访问性",
            rules = listOf(
                "使用中文进行对话",
                "所有图片必须有alt属性",
                "使用ARIA标签增强可访问性",
                "确保足够的颜色对比度",
                "支持键盘导航",
                "使用语义化HTML"
            )
        ),
        RulesTemplate(
            id = "single-file",
            name = "单文件输出",
            description = "所有代码整合在一个HTML文件",
            rules = listOf(
                "使用中文进行对话",
                "将所有CSS放在<style>标签内",
                "将所有JavaScript放在<script>标签内",
                "不使用外部CSS/JS文件",
                "可以使用CDN引入第三方库"
            )
        ),
        RulesTemplate(
            id = "game-dev",
            name = "网页游戏开发",
            description = "适用于HTML5游戏开发",
            rules = listOf(
                "使用中文进行对话",
                "使用Canvas或SVG进行图形渲染",
                "实现requestAnimationFrame游戏循环",
                "支持键盘和触摸输入",
                "考虑游戏性能优化",
                "添加游戏状态管理"
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
