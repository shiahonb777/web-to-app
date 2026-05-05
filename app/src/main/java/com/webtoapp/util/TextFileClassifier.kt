package com.webtoapp.util









object TextFileClassifier {


    private val COMMON_TEXT_EXTENSIONS = setOf(
        "html", "htm", "css", "js", "json", "xml", "txt", "svg", "md",
        "csv", "map", "ts", "tsx", "jsx", "log", "cfg"
    )


    private val COMMON_CONFIG_EXTENSIONS = setOf(
        "yml", "yaml", "toml", "ini", "env", "lock", "sql", "sh", "bat", "cmd"
    )


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









    fun isTextFile(fileName: String, runtimeType: String? = null): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext in COMMON_TEXT_EXTENSIONS) return true
        if (ext in COMMON_CONFIG_EXTENSIONS) return true
        if (fileName.startsWith(".")) return true
        if (runtimeType != null) {
            val runtimeExts = RUNTIME_TEXT_EXTENSIONS[runtimeType]
            if (runtimeExts != null && ext in runtimeExts) return true
        }

        if (fileName == "requirements.txt" || fileName == "Pipfile") return true
        return false
    }
}
