package com.webtoapp.core.ai.htmlcoding

/**
 * HTML编程AI - 提示词与模板管理
 * 
 * 优化原则：
 * 1. 简洁明确，避免冗余
 * 2. 关键规则前置
 * 3. 减少示例代码，让AI自由发挥
 */
object HtmlCodingPrompts {

    /**
     * 构建系统提示词 - 精简版
     */
    fun buildSystemPrompt(
        config: SessionConfig,
        hasImageModel: Boolean = false,
        selectedTemplate: StyleTemplate? = null,
        selectedStyle: StyleReference? = null
    ): String = buildString {
        // 角色
        appendLine("你是移动端前端开发专家，为手机APP WebView创建HTML页面。")
        appendLine()
        
        // 回复规则
        appendLine("# 回复规则")
        appendLine("使用 Markdown 格式回复：**粗体**、*斜体*、`代码`、列表、> 引用等")
        appendLine()
        
        // 核心规则
        appendLine("# 代码规范")
        appendLine("1. 输出单个完整HTML文件，CSS/JS内嵌，禁止省略代码")
        appendLine("2. 必须包含: `<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">`")
        appendLine("3. 使用相对单位(vw/vh/%/rem)，禁止固定像素宽度如width:375px")
        appendLine("4. 可点击元素最小44x44px，禁止依赖hover效果")
        appendLine("5. 使用Flexbox/Grid布局，overflow-x:hidden防止横向滚动")
        appendLine()
        
        // 用户规则
        if (config.rules.isNotEmpty()) {
            appendLine("# 用户自定义规则")
            config.rules.forEachIndexed { i, rule -> appendLine("${i + 1}. $rule") }
            appendLine()
        }
        
        // 风格模板（简化）
        selectedTemplate?.let { t ->
            appendLine("# 风格: ${t.name}")
            appendLine("${t.description}。${t.promptHint}")
            t.colorScheme?.let { c ->
                appendLine("配色: 主色${c.primary} 背景${c.background} 文字${c.text}")
            }
            appendLine()
        }
        
        // 风格参考（简化）
        selectedStyle?.let { s ->
            appendLine("# 参考风格: ${s.name}")
            appendLine("${s.description}")
            appendLine("关键词: ${s.keywords.joinToString("、")}")
            appendLine("配色: ${s.colorHints.joinToString("、")}")
            appendLine()
        }
        
        // 图像生成（简化）
        if (hasImageModel) {
            appendLine("# 图像生成")
            appendLine("使用generate_image工具生成图片，返回base64可直接用于img src")
        }
    }.trimEnd()

    /**
     * 根据模板ID获取模板
     */
    fun getTemplateById(id: String): StyleTemplate? {
        return styleTemplates.find { it.id == id }
    }

    /**
     * 根据风格ID获取风格
     */
    fun getStyleById(id: String): StyleReference? {
        return styleReferences.find { it.id == id }
    }

    /**
     * 预置的风格模板库
     */
    val styleTemplates = listOf(
        StyleTemplate(
            id = "modern-minimal",
            name = "现代简约",
            category = TemplateCategory.MODERN,
            description = "干净利落的现代设计，大量留白，强调内容",
            cssFramework = null,
            colorScheme = ColorScheme(
                primary = "#3B82F6",
                secondary = "#6366F1",
                background = "#FFFFFF",
                surface = "#F9FAFB",
                text = "#111827",
                accent = "#10B981"
            ),
            promptHint = "大量留白、简洁排版、柔和阴影、圆角元素"
        ),
        StyleTemplate(
            id = "glassmorphism",
            name = "玻璃拟态",
            category = TemplateCategory.GLASSMORPHISM,
            description = "透明模糊效果，创造深度层次感",
            colorScheme = ColorScheme(
                primary = "#667EEA",
                secondary = "#764BA2",
                background = "linear-gradient(135deg, #667EEA, #764BA2)",
                surface = "rgba(255,255,255,0.25)",
                text = "#FFFFFF",
                accent = "#F093FB"
            ),
            promptHint = "backdrop-filter: blur()、半透明背景、渐变色、柔和边框"
        ),
        StyleTemplate(
            id = "neumorphism",
            name = "新拟物化",
            category = TemplateCategory.NEUMORPHISM,
            description = "软阴影创造的凸起/凹陷效果",
            colorScheme = ColorScheme(
                primary = "#6C63FF",
                secondary = "#A29BFE",
                background = "#E0E5EC",
                surface = "#E0E5EC",
                text = "#495057",
                accent = "#6C63FF"
            ),
            promptHint = "双层阴影（亮/暗）、柔和背景色、圆角、凸起或凹陷效果"
        ),
        StyleTemplate(
            id = "dark-mode",
            name = "暗黑主题",
            category = TemplateCategory.DARK,
            description = "深色背景配亮色文字，护眼且现代",
            colorScheme = ColorScheme(
                primary = "#818CF8",
                secondary = "#34D399",
                background = "#0F172A",
                surface = "#1E293B",
                text = "#F1F5F9",
                accent = "#F472B6"
            ),
            promptHint = "深色背景、亮色文字、柔和发光效果、高对比度"
        ),
        StyleTemplate(
            id = "cyberpunk",
            name = "赛博朋克",
            category = TemplateCategory.CYBERPUNK,
            description = "霓虹灯效、科技感、未来主义",
            colorScheme = ColorScheme(
                primary = "#FF00FF",
                secondary = "#00FFFF",
                background = "#0A0A0A",
                surface = "#1A1A2E",
                text = "#EAEAEA",
                accent = "#FFE600"
            ),
            promptHint = "霓虹色彩、发光效果、故障艺术、网格线条、科技感字体"
        ),
        StyleTemplate(
            id = "gradient",
            name = "渐变炫彩",
            category = TemplateCategory.GRADIENT,
            description = "丰富的渐变色彩，活力四射",
            colorScheme = ColorScheme(
                primary = "#EC4899",
                secondary = "#8B5CF6",
                background = "linear-gradient(to right, #EC4899, #8B5CF6)",
                surface = "#FFFFFF",
                text = "#1F2937",
                accent = "#F59E0B"
            ),
            promptHint = "多彩渐变、流动感、动态背景、圆润形状"
        ),
        StyleTemplate(
            id = "minimal",
            name = "极简主义",
            category = TemplateCategory.MINIMAL,
            description = "去除一切不必要的装饰，只保留核心",
            colorScheme = ColorScheme(
                primary = "#000000",
                secondary = "#666666",
                background = "#FFFFFF",
                surface = "#FAFAFA",
                text = "#000000",
                accent = "#000000"
            ),
            promptHint = "黑白配色、大量空白、清晰排版、无装饰"
        ),
        StyleTemplate(
            id = "nature",
            name = "自然清新",
            category = TemplateCategory.NATURE,
            description = "来自自然的配色，宁静舒适",
            colorScheme = ColorScheme(
                primary = "#059669",
                secondary = "#0D9488",
                background = "#ECFDF5",
                surface = "#FFFFFF",
                text = "#064E3B",
                accent = "#F59E0B"
            ),
            promptHint = "绿色系、自然元素、圆润形状、柔和阴影"
        ),
        StyleTemplate(
            id = "cute-cartoon",
            name = "卡通可爱",
            category = TemplateCategory.CREATIVE,
            description = "萌系卡通风格，圆润可爱",
            colorScheme = ColorScheme(
                primary = "#FF6B9D",
                secondary = "#C44569",
                background = "#FFF5F7",
                surface = "#FFFFFF",
                text = "#4A4A4A",
                accent = "#FFD93D"
            ),
            promptHint = "圆角元素、柔和阴影、可爱图标、糖果色"
        ),
        StyleTemplate(
            id = "neon-glow",
            name = "霓虹灯光",
            category = TemplateCategory.DARK,
            description = "发光霓虹效果，夜店风格",
            colorScheme = ColorScheme(
                primary = "#00F5FF",
                secondary = "#FF00E4",
                background = "#0D0D0D",
                surface = "#1A1A1A",
                text = "#FFFFFF",
                accent = "#39FF14"
            ),
            promptHint = "发光文字、霓虹边框、暗色背景、高对比度"
        )
    )

    /**
     * 风格参考词库
     */
    val styleReferences = listOf(
        StyleReference(
            id = "harry-potter",
            name = "哈利波特风格",
            category = StyleReferenceCategory.MOVIE,
            keywords = listOf("魔法", "古典", "神秘", "霍格沃茨"),
            description = "魔法学院气息，古老神秘的英伦风格",
            colorHints = listOf("深红", "金色", "深棕", "墨绿"),
            elementHints = listOf("盾徽", "羽毛笔", "蜡封", "哥特字体")
        ),
        StyleReference(
            id = "ghibli",
            name = "吉卜力风格",
            category = StyleReferenceCategory.ANIME,
            keywords = listOf("宫崎骏", "自然", "温暖", "治愈"),
            description = "吉卜力动画的温暖世界",
            colorHints = listOf("天空蓝", "草绿", "泥土棕", "夕阳橙"),
            elementHints = listOf("云朵", "绿植", "小屋", "柔和光影")
        ),
        StyleReference(
            id = "your-name",
            name = "你的名字风格",
            category = StyleReferenceCategory.ANIME,
            keywords = listOf("新海诚", "光影", "青春", "唯美"),
            description = "新海诚式的光影美学",
            colorHints = listOf("黄昏橙", "天际蓝", "星光紫", "晨曦粉"),
            elementHints = listOf("光斑", "彗星", "黄昏", "细腻光影")
        ),
        StyleReference(
            id = "apple",
            name = "苹果风格",
            category = StyleReferenceCategory.BRAND,
            keywords = listOf("极简", "优雅", "科技", "高端"),
            description = "苹果公司的设计语言",
            colorHints = listOf("纯白", "深空灰", "银色", "金色"),
            elementHints = listOf("大量留白", "精确对齐", "微妙渐变", "圆角")
        ),
        StyleReference(
            id = "little-prince",
            name = "小王子风格",
            category = StyleReferenceCategory.BOOK,
            keywords = listOf("童话", "星空", "纯真", "诗意"),
            description = "充满诗意的童话风格",
            colorHints = listOf("星空蓝", "沙漠金", "玫瑰红", "淡紫"),
            elementHints = listOf("星星", "玫瑰", "狐狸", "小行星")
        ),
        StyleReference(
            id = "zelda-botw",
            name = "塞尔达荒野之息",
            category = StyleReferenceCategory.GAME,
            keywords = listOf("冒险", "自然", "卡通渲染", "探索"),
            description = "海拉鲁的广袤世界",
            colorHints = listOf("草原绿", "天空蓝", "山岩灰", "篝火橙"),
            elementHints = listOf("希卡符文", "远景山脉", "卡通渲染")
        ),
        StyleReference(
            id = "art-deco",
            name = "装饰艺术风格",
            category = StyleReferenceCategory.ART,
            keywords = listOf("几何", "对称", "奢华", "1920s"),
            description = "1920年代的装饰艺术运动",
            colorHints = listOf("金色", "黑色", "翡翠绿", "深蓝"),
            elementHints = listOf("几何图案", "对称布局", "扇形", "金属线条")
        ),
        StyleReference(
            id = "japanese",
            name = "日式和风",
            category = StyleReferenceCategory.CULTURE,
            keywords = listOf("和风", "禅意", "留白", "传统"),
            description = "日本传统美学，禅意与留白",
            colorHints = listOf("靛蓝", "朱红", "米白", "墨黑"),
            elementHints = listOf("波浪", "樱花", "和纹", "毛笔字体")
        )
    )

    /**
     * Rules 模板
     */
    val rulesTemplates = listOf(
        RulesTemplate(
            id = "chinese",
            name = "中文对话",
            description = "使用中文进行所有交流",
            rules = listOf("使用中文进行对话", "代码注释使用中文")
        ),
        RulesTemplate(
            id = "game",
            name = "游戏开发",
            description = "适合开发小游戏",
            rules = listOf(
                "使用中文进行对话",
                "游戏要有完整的开始、进行、结束流程",
                "添加分数显示和游戏说明",
                "确保触摸控制流畅"
            )
        ),
        RulesTemplate(
            id = "animation",
            name = "动画效果",
            description = "注重动画和交互效果",
            rules = listOf(
                "使用中文进行对话",
                "添加流畅的 CSS 动画",
                "使用 transition 优化交互反馈",
                "考虑性能，避免过度动画"
            )
        ),
        RulesTemplate(
            id = "form",
            name = "表单页面",
            description = "适合创建表单和数据收集页面",
            rules = listOf(
                "使用中文进行对话",
                "表单要有完整的验证逻辑",
                "输入框要有清晰的标签和提示",
                "提交按钮要有加载状态"
            )
        )
    )
}
