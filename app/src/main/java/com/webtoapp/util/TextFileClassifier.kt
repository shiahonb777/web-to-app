package com.webtoapp.util

/**
 * Unified text file classifier — replaces isTextFile, isNodeTextFile,
 * isPhpTextFile, isPythonTextFile, isGoTextFile scattered across ApkBuilder.
 *
 * Usage:
 *   TextFileClassifier.isTextFile("app.js")                       // common check
 *   TextFileClassifier.isTextFile("schema.prisma", "nodejs")      // runtime-aware check
 */
object TextFileClassifier {

    /** Common text file extensions shared by all runtimes */
    private val COMMON_TEXT_EXTENSIONS = setOf(
        "html", "htm", "css", "js", "json", "xml", "txt", "svg", "md",
        "csv", "map", "ts", "tsx", "jsx", "log", "cfg"
    )

    /** Config/build file extensions shared across most runtimes */
    private val COMMON_CONFIG_EXTENSIONS = setOf(
        "yml", "yaml", "toml", "ini", "env", "lock", "sql", "sh", "bat", "cmd"
    )

    /** Runtime-specific text file extensions */
    private val RUNTIME_TEXT_EXTENSIONS: Map<String, Set<String>> = mapOf(
        "nodejs" to setOf(
            "mjs", "cjs", "mts", "cts",
            "graphql", "gql", "prisma",
            "ejs", "hbs", "pug", "njk"
        ),
        "php" to setOf(
            "php", "phtml", "twig", "blade"
        ),
        "python" to setOf(
            "py", "pyi", "pyx", "rst"
        ),
        "go" to setOf(
            "go", "mod", "sum", "tmpl", "tpl"
        )
    )

    /**
     * Check if a file should be treated as text (compressible).
     *
     * @param fileName File name (e.g. "index.js", ".env")
     * @param runtimeType Optional runtime type: "nodejs", "php", "python", "go".
     *                    When null, only checks common extensions.
     * @return true if the file is a text file
     */
    fun isTextFile(fileName: String, runtimeType: String? = null): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext in COMMON_TEXT_EXTENSIONS) return true
        if (ext in COMMON_CONFIG_EXTENSIONS) return true
        if (fileName.startsWith(".")) return true
        if (runtimeType != null) {
            val runtimeExts = RUNTIME_TEXT_EXTENSIONS[runtimeType]
            if (runtimeExts != null && ext in runtimeExts) return true
        }
        // Special file names
        if (fileName == "requirements.txt" || fileName == "Pipfile") return true
        return false
    }
}
