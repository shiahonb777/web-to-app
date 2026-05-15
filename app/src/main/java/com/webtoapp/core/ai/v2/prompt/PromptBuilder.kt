package com.webtoapp.core.ai.v2.prompt

import com.webtoapp.core.ai.coding.AiCodingType
import com.webtoapp.core.ai.v2.tool.Tool
import com.webtoapp.core.i18n.AppLanguage

data class ProjectFileSummary(val path: String, val lines: Int, val size: Int)

object PromptBuilder {
    fun buildSystemPrompt(codingType: AiCodingType, language: AppLanguage, tools: List<Tool>, toolMode: Boolean, userRules: List<String> = emptyList(), projectFiles: List<ProjectFileSummary> = emptyList()): String {
        val cn = language != AppLanguage.ENGLISH && language != AppLanguage.ARABIC
        return buildString {
            appendLine(persona(codingType, cn))
            appendLine()
            appendLine(if(cn) "# 行为规则" else "# Behavior")
            if(toolMode) { listOf(
                if(cn)"用户要求创建/修改时，立即调用工具执行，不要只描述计划。" else "When asked to create/modify, call tools immediately. Don't just describe the plan.",
                if(cn)"修改前若不确定文件结构，先调用 list_files / read_file。" else "If unsure about file structure, call list_files / read_file first.",
                if(cn)"代码必须完整：禁止 \"...\" 或省略注释。" else "Code must be complete. Never abbreviate with \"...\" or placeholder comments.",
                if(cn)"闲聊或纯问答时直接用文字回复，不调用工具。" else "For chat/questions, reply in plain text without tool calls.",
                if(cn)"完成后用一句话总结你做了什么。" else "End with a one-sentence summary."
            ).forEachIndexed{i,r->appendLine("${i+1}. $r")} }
            else { listOf(
                if(cn)"本次不使用工具调用，直接输出代码。" else "Do not use tool calls. Output code directly.",
                if(cn)"每个文件用 `=== file: <路径> ===` 单独一行分隔，紧接完整内容。" else "Separate files with `=== file: <path> ===` lines followed by full content.",
                if(cn)"代码必须完整。" else "Code must be complete.",
                if(cn)"代码前后可有简短说明。" else "Brief explanations before/after code are fine."
            ).forEachIndexed{i,r->appendLine("${i+1}. $r")} }
            appendLine()
            if(toolMode) { appendLine(if(cn)"# 可用工具" else "# Available tools"); tools.forEach{appendLine("- ${it.name}: ${it.description}")}; appendLine() }
            appendLine(if(cn)"# 代码规范" else "# Code standards")
            codeStandards(codingType, cn).forEach{appendLine("- $it")}; appendLine()
            if(userRules.isNotEmpty()){appendLine(if(cn)"# 用户自定义规则" else "# User rules");userRules.forEachIndexed{i,r->appendLine("${i+1}. $r")};appendLine()}
            if(projectFiles.isNotEmpty()){appendLine(if(cn)"# 当前项目文件" else "# Current project files");projectFiles.take(20).forEach{appendLine("- ${it.path} (${it.lines} lines)")};if(projectFiles.size>20)appendLine("- ... (${projectFiles.size-20} more)")}
            else appendLine(if(cn)"# 当前项目文件\n（暂无文件，需要从头创建）" else "# Current project files\n(empty — start from scratch)")
        }.trimEnd()
    }

    fun buildFallbackUserMessage(codingType: AiCodingType, language: AppLanguage, userRequirement: String): String {
        val cn = language != AppLanguage.ENGLISH && language != AppLanguage.ARABIC
        val entry = codingType.defaultEntryFile
        val prefix = if(cn) "请直接输出代码（不使用工具调用）。入口文件: $entry。多文件时用 \"=== file: <path> ===\" 行分隔。" else "Output code directly (no tool calls). Entry file: $entry. Multi-file: separate with `=== file: <path> ===`."
        return "$prefix\n\n$userRequirement"
    }

    private fun persona(t: AiCodingType, cn: Boolean) = when(t) {
        AiCodingType.HTML -> if(cn)"你是移动端前端开发专家，在 Android APP 内的 WebView 中创建 HTML 页面。" else "You are a mobile frontend expert building HTML pages for an Android WebView."
        AiCodingType.FRONTEND -> if(cn)"你是前端工程师，构建 React/Vue 等单页应用。" else "You are a frontend engineer building React/Vue SPAs."
        AiCodingType.NODEJS -> if(cn)"你是 Node.js 后端工程师。" else "You are a Node.js backend engineer."
        AiCodingType.WORDPRESS -> if(cn)"你是 WordPress 主题/插件开发者。" else "You are a WordPress theme/plugin developer."
        AiCodingType.PHP -> if(cn)"你是 PHP 后端工程师。" else "You are a PHP backend engineer."
        AiCodingType.PYTHON -> if(cn)"你是 Python 工程师（Flask/Django）。" else "You are a Python engineer (Flask/Django)."
        AiCodingType.GO -> if(cn)"你是 Go 工程师。" else "You are a Go engineer."
    }
    private fun codeStandards(t: AiCodingType, cn: Boolean): List<String> = when(t) {
        AiCodingType.HTML, AiCodingType.FRONTEND -> listOf(
            if(cn)"必须包含 <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" else "Include viewport meta tag",
            if(cn)"使用相对单位 (vw/vh/%/rem)，可点击元素 ≥44×44px" else "Use relative units, tappable elements ≥44×44px",
            if(cn)"尽量单文件：CSS 在 <style>、JS 在 <script>" else "Prefer single-file: CSS in <style>, JS in <script>")
        AiCodingType.NODEJS -> listOf(if(cn)"监听 process.env.PORT || 3000" else "Listen on PORT || 3000", if(cn)"依赖通过 package.json 声明" else "Declare deps in package.json")
        AiCodingType.PHP -> listOf(if(cn)"兼容 PHP 8.x" else "Target PHP 8.x", if(cn)"使用 PDO + 预处理语句" else "Use PDO with prepared statements")
        AiCodingType.PYTHON -> listOf(if(cn)"Python 3.10+" else "Python 3.10+", if(cn)"依赖通过 requirements.txt" else "Deps in requirements.txt")
        AiCodingType.GO -> listOf(if(cn)"Go 1.21+，标准库优先" else "Go 1.21+, prefer stdlib", if(cn)"go.mod 管理依赖" else "Manage deps via go.mod")
        AiCodingType.WORDPRESS -> listOf(if(cn)"遵循 WordPress 编码规范" else "Follow WP coding standards", if(cn)"使用 WP API" else "Use WP API")
    }
}
