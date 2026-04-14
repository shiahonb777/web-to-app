package com.webtoapp.core.ai.coding

import com.webtoapp.core.i18n.Strings

/**
 * AI Coding.
 * 
 * Note.
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
     * Note.
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
     * Note.
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
     * Note.
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
     * Note.
     */
    fun getWriteToolName(): String = "write_file"

    /**
     * Note.
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
     * Note.
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
