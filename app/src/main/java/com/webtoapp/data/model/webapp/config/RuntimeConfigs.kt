package com.webtoapp.data.model.webapp.config

enum class NodeJsBuildMode {
    STATIC,         // Static frontend build, usually from dist/
    SSR,            // Server-side rendering, such as Next.js or Nuxt.js
    API_BACKEND,    // API backend, such as Express, Fastify, or Koa
    FULLSTACK       // Combined frontend and API app
}

data class NodeJsConfig(
    val projectId: String = "",
    val projectName: String = "",
    val framework: String = "",
    val buildMode: NodeJsBuildMode = NodeJsBuildMode.API_BACKEND,
    val entryFile: String = "index.js",
    val serverPort: Int = 0,
    val envVars: Map<String, String> = emptyMap(),
    val hasNodeModules: Boolean = false,
    val nodeVersion: String = "",
    val landscapeMode: Boolean = false
)

data class WordPressConfig(
    val projectId: String = "",
    val siteTitle: String = "My Site",
    val adminUser: String = "admin",
    val adminEmail: String = "",
    val themeName: String = "",
    val plugins: List<String> = emptyList(),
    val phpPort: Int = 0,
    val landscapeMode: Boolean = false
)

data class PhpAppConfig(
    val projectId: String = "",
    val projectName: String = "",
    val framework: String = "",
    val documentRoot: String = "",
    val entryFile: String = "index.php",
    val phpPort: Int = 0,
    val envVars: Map<String, String> = emptyMap(),
    val hasComposerJson: Boolean = false,
    val landscapeMode: Boolean = false
)

data class PythonAppConfig(
    val projectId: String = "",
    val projectName: String = "",
    val framework: String = "",
    val entryFile: String = "app.py",
    val entryModule: String = "",
    val serverType: String = "builtin",
    val serverPort: Int = 0,
    val envVars: Map<String, String> = emptyMap(),
    val pythonVersion: String = "",
    val requirementsFile: String = "requirements.txt",
    val hasPipDeps: Boolean = false,
    val landscapeMode: Boolean = false
)

data class GoAppConfig(
    val projectId: String = "",
    val projectName: String = "",
    val framework: String = "",
    val binaryName: String = "",
    val serverPort: Int = 0,
    val envVars: Map<String, String> = emptyMap(),
    val staticDir: String = "",
    val hasBuildFromSource: Boolean = false,
    val landscapeMode: Boolean = false
)

data class MultiWebConfig(
    val sites: List<MultiWebSite> = emptyList(),
    val displayMode: String = "TABS",
    val refreshInterval: Int = 30,
    val showSiteIcons: Boolean = true,
    val landscapeMode: Boolean = false
)

data class MultiWebSite(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val iconEmoji: String = "",
    val faviconUrl: String = "",
    val themeColor: String = "",
    val category: String = "",
    val cssSelector: String = "",
    val linkSelector: String = "",
    val enabled: Boolean = true,
    val sortIndex: Int = 0
)

data class HtmlConfig(
    val projectId: String = "",
    val projectDir: String? = null,
    val entryFile: String = "index.html",
    val files: List<HtmlFile> = emptyList(),
    val enableJavaScript: Boolean = true,
    val enableLocalStorage: Boolean = true,
    val allowFileAccess: Boolean = true,
    val backgroundColor: String = "#FFFFFF",
    val landscapeMode: Boolean = false
) {
    fun getValidEntryFile(): String {
        return entryFile.takeIf { 
            it.isNotBlank() && it.substringBeforeLast(".").isNotBlank() 
        } ?: "index.html"
    }
}

data class HtmlFile(
    val name: String,
    val path: String,
    val type: HtmlFileType = HtmlFileType.OTHER
)

enum class HtmlFileType {
    HTML,
    CSS,
    JS,
    IMAGE,
    FONT,
    OTHER
}

