package com.webtoapp.core.ai.htmlcoding

/**
 * 精简的提示词 - 用于简单场景
 */
object SimplePrompts {
    
    /**
     * 构建系统提示词 - 极简版
     */
    fun buildSystemPrompt(): String = """
你是移动端前端开发专家，为手机APP WebView创建HTML页面。

# 回复规则
使用 Markdown 格式回复：**粗体**、*斜体*、`代码`、列表、> 引用等

# 代码规范
1. 输出单个完整HTML文件，CSS/JS内嵌，禁止省略代码
2. 必须包含viewport meta标签
3. 使用相对单位(vw/vh/%/rem)，禁止固定像素宽度
4. 可点击元素最小44x44px
5. 禁止hover效果，使用touch事件
    """.trimIndent()
    
    /**
     * 构建系统提示词 - 带模板
     */
    fun buildSystemPrompt(template: StyleTemplate? = null): String {
        val base = buildSystemPrompt()
        
        return if (template != null) {
            """
$base

风格: ${template.name} - ${template.description}
${template.colorScheme?.let { "配色: 主色${it.primary} 背景${it.background}" } ?: ""}
${template.promptHint}
            """.trimIndent()
        } else {
            base
        }
    }
}
