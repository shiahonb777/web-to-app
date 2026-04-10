package com.webtoapp.core.ai.coding

import com.webtoapp.core.i18n.Strings

/**
 * AI 编程支持的应用类型
 * 
 * 7种需要代码构建的应用类型（排除网页、媒体、画廊）
 */
enum class AiCodingType(
    val icon: String,
    val supportPreview: Boolean,
    val defaultEntryFile: String,
    val fileExtensions: List<String>
) {
    HTML(
        icon = "globe",
        supportPreview = true,
        defaultEntryFile = "index.html",
        fileExtensions = listOf("html", "css", "js", "svg", "json")
    ),
    FRONTEND(
        icon = "html",
        supportPreview = true,
        defaultEntryFile = "index.html",
        fileExtensions = listOf("html", "css", "js", "jsx", "tsx", "ts", "json", "svg")
    ),
    NODEJS(
        icon = "nodejs",
        supportPreview = false,
        defaultEntryFile = "index.js",
        fileExtensions = listOf("js", "ts", "json", "html", "css", "env")
    ),
    WORDPRESS(
        icon = "code",
        supportPreview = false,
        defaultEntryFile = "index.php",
        fileExtensions = listOf("php", "css", "js", "html", "json", "sql")
    ),
    PHP(
        icon = "php",
        supportPreview = false,
        defaultEntryFile = "index.php",
        fileExtensions = listOf("php", "html", "css", "js", "json", "env")
    ),
    PYTHON(
        icon = "python",
        supportPreview = false,
        defaultEntryFile = "main.py",
        fileExtensions = listOf("py", "html", "css", "js", "json", "txt", "cfg", "ini", "yaml", "yml")
    ),
    GO(
        icon = "golang",
        supportPreview = false,
        defaultEntryFile = "main.go",
        fileExtensions = listOf("go", "html", "css", "js", "json", "mod", "sum", "yaml", "yml")
    );

    /**
     * 获取本地化显示名称
     */
    fun getDisplayName(): String = when (this) {
        HTML -> Strings.codingTypeHtml
        FRONTEND -> Strings.codingTypeFrontend
        NODEJS -> Strings.codingTypeNodejs
        WORDPRESS -> Strings.codingTypeWordpress
        PHP -> Strings.codingTypePhp
        PYTHON -> Strings.codingTypePython
        GO -> Strings.codingTypeGo
    }

    /**
     * 获取本地化描述
     */
    fun getDescription(): String = when (this) {
        HTML -> Strings.codingTypeHtmlDesc
        FRONTEND -> Strings.codingTypeFrontendDesc
        NODEJS -> Strings.codingTypeNodejsDesc
        WORDPRESS -> Strings.codingTypeWordpressDesc
        PHP -> Strings.codingTypePhpDesc
        PYTHON -> Strings.codingTypePythonDesc
        GO -> Strings.codingTypeGoDesc
    }

    /**
     * 获取主要编程语言
     */
    fun getPrimaryLanguage(): String = when (this) {
        HTML -> "html"
        FRONTEND -> "javascript"
        NODEJS -> "javascript"
        WORDPRESS -> "php"
        PHP -> "php"
        PYTHON -> "python"
        GO -> "go"
    }

    /**
     * 获取写入文件的工具名称
     */
    fun getWriteToolName(): String = "write_file"

    /**
     * 获取写入文件的工具描述（已本地化）
     */
    fun getWriteToolDescription(): String = when (this) {
        HTML -> Strings.writeToolDescHtml
        FRONTEND -> Strings.writeToolDescFrontend
        NODEJS -> Strings.writeToolDescNodejs
        WORDPRESS -> Strings.writeToolDescWordpress
        PHP -> Strings.writeToolDescPhp
        PYTHON -> Strings.writeToolDescPython
        GO -> Strings.writeToolDescGo
    }

    /**
     * 获取本地化示例提示词
     */
    fun getExamplePrompts(): List<String> = when (this) {
        HTML -> listOf(Strings.aiPromptHtml1, Strings.aiPromptHtml2)
        FRONTEND -> listOf(Strings.aiPromptFrontend1, Strings.aiPromptFrontend2)
        NODEJS -> listOf(Strings.aiPromptNodejs1, Strings.aiPromptNodejs2)
        WORDPRESS -> listOf(Strings.aiPromptWordpress1, Strings.aiPromptWordpress2)
        PHP -> listOf(Strings.aiPromptPhp1, Strings.aiPromptPhp2)
        PYTHON -> listOf(Strings.aiPromptPython1, Strings.aiPromptPython2)
        GO -> listOf(Strings.aiPromptGo1, Strings.aiPromptGo2)
    }
}
