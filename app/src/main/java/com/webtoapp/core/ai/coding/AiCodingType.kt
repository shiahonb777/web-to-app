package com.webtoapp.core.ai.coding

import com.webtoapp.core.i18n.AppStringsProvider

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
        HTML -> AppStringsProvider.current().codingTypeHtml
        FRONTEND -> AppStringsProvider.current().codingTypeFrontend
        NODEJS -> AppStringsProvider.current().codingTypeNodejs
        WORDPRESS -> AppStringsProvider.current().codingTypeWordpress
        PHP -> AppStringsProvider.current().codingTypePhp
        PYTHON -> AppStringsProvider.current().codingTypePython
        GO -> AppStringsProvider.current().codingTypeGo
    }

    /**
     * Note.
     */
    fun getDescription(): String = when (this) {
        HTML -> AppStringsProvider.current().codingTypeHtmlDesc
        FRONTEND -> AppStringsProvider.current().codingTypeFrontendDesc
        NODEJS -> AppStringsProvider.current().codingTypeNodejsDesc
        WORDPRESS -> AppStringsProvider.current().codingTypeWordpressDesc
        PHP -> AppStringsProvider.current().codingTypePhpDesc
        PYTHON -> AppStringsProvider.current().codingTypePythonDesc
        GO -> AppStringsProvider.current().codingTypeGoDesc
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
        HTML -> AppStringsProvider.current().writeToolDescHtml
        FRONTEND -> AppStringsProvider.current().writeToolDescFrontend
        NODEJS -> AppStringsProvider.current().writeToolDescNodejs
        WORDPRESS -> AppStringsProvider.current().writeToolDescWordpress
        PHP -> AppStringsProvider.current().writeToolDescPhp
        PYTHON -> AppStringsProvider.current().writeToolDescPython
        GO -> AppStringsProvider.current().writeToolDescGo
    }

    /**
     * Note.
     */
    fun getExamplePrompts(): List<String> = when (this) {
        HTML -> listOf(AppStringsProvider.current().aiPromptHtml1, AppStringsProvider.current().aiPromptHtml2)
        FRONTEND -> listOf(AppStringsProvider.current().aiPromptFrontend1, AppStringsProvider.current().aiPromptFrontend2)
        NODEJS -> listOf(AppStringsProvider.current().aiPromptNodejs1, AppStringsProvider.current().aiPromptNodejs2)
        WORDPRESS -> listOf(AppStringsProvider.current().aiPromptWordpress1, AppStringsProvider.current().aiPromptWordpress2)
        PHP -> listOf(AppStringsProvider.current().aiPromptPhp1, AppStringsProvider.current().aiPromptPhp2)
        PYTHON -> listOf(AppStringsProvider.current().aiPromptPython1, AppStringsProvider.current().aiPromptPython2)
        GO -> listOf(AppStringsProvider.current().aiPromptGo1, AppStringsProvider.current().aiPromptGo2)
    }
}
