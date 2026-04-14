package com.webtoapp.core.ai.coding

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

/**
 * Note.
 * 
 * Note.
 * - AI HTML <!DOCTYPE html>.
 * - WebView.
 * Note.
 */
object SimplePrompts {
    
    /**
     * Note.
     */
    fun buildSystemPrompt(): String {
        return when (Strings.currentLanguage.value) {
            AppLanguage.ENGLISH -> """
You are a mobile frontend expert, creating HTML pages in mobile APP WebView.

# Behavior Rules
1. When user asks to create/modify a webpage, directly output complete HTML code starting with <!DOCTYPE html>
2. Do not wrap code in ```html code blocks
3. Brief explanations before and after code are fine
4. Answer with Markdown format for chat and questions

# Code Standards
- Single-file HTML, CSS in <style>, JS in <script> tags
- Must include: <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
- Use relative units (vw/vh/%/rem), avoid fixed pixel widths
- Clickable elements minimum 44x44px touch area
- Code must be complete, never omit any part with ... or comments
            """.trimIndent()
            AppLanguage.ARABIC -> """
أنت خبير تطوير واجهات أمامية للجوال، تقوم بإنشاء صفحات HTML في WebView لتطبيقات الجوال.

# قواعد السلوك
1. عندما يطلب المستخدم إنشاء/تعديل صفحة ويب، أخرج كود HTML الكامل مباشرة بدءاً من <!DOCTYPE html>
2. لا تغلف الكود في كتل ```html
3. يمكن إضافة شرح مختصر قبل وبعد الكود
4. أجب بتنسيق Markdown للدردشة والأسئلة

# معايير الكود
- ملف HTML واحد، CSS في <style>، JS في <script>
- يجب تضمين: <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
- استخدم وحدات نسبية (vw/vh/%/rem)، تجنب العرض الثابت بالبكسل
- منطقة لمس العناصر القابلة للنقر بحد أدنى 44x44px
- الكود يجب أن يكون كاملاً، لا تحذف أي جزء
            """.trimIndent()
            else -> """
你是移动端前端开发专家，在手机 APP WebView 中创建 HTML 页面。

# 行为规则
1. 用户要求创建/修改网页时，直接输出完整 HTML 代码，以 <!DOCTYPE html> 开头
2. 禁止使用 ```html 代码块包裹代码
3. 代码前后可有简短说明文字
4. 闲聊或提问时用 Markdown 格式文字回答

# 代码规范
- 单文件 HTML，CSS 在 <style>、JS 在 <script> 标签内
- 必须包含: <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
- 使用相对单位 (vw/vh/%/rem)，禁止固定像素宽度
- 可点击元素最小 44x44px 触摸区域
- 代码完整，禁止用 ... 或注释省略任何部分
            """.trimIndent()
        }
    }
    
    /**
     * Note.
     */
    fun buildSystemPrompt(template: StyleTemplate? = null): String {
        val base = buildSystemPrompt()
        
        if (template == null) return base
        
        val isEnglish = Strings.currentLanguage.value == AppLanguage.ENGLISH
        val isArabic = Strings.currentLanguage.value == AppLanguage.ARABIC
        
        return buildString {
            append(base)
            appendLine()
            appendLine()
            if (isArabic) {
                appendLine("# أسلوب التصميم")
                appendLine("الأسلوب: ${template.name} - ${template.description}")
                template.colorScheme?.let {
                    appendLine("الألوان: الرئيسي ${it.primary}, الخلفية ${it.background}")
                }
            } else if (isEnglish) {
                appendLine("# Design Style")
                appendLine("Style: ${template.name} - ${template.description}")
                template.colorScheme?.let {
                    appendLine("Colors: Primary ${it.primary}, Background ${it.background}")
                }
            } else {
                appendLine("# 设计风格")
                appendLine("风格: ${template.name} - ${template.description}")
                template.colorScheme?.let {
                    appendLine("配色: 主色 ${it.primary}, 背景 ${it.background}")
                }
            }
            if (template.promptHint.isNotBlank()) {
                appendLine(template.promptHint)
            }
        }.trimEnd()
    }
}
