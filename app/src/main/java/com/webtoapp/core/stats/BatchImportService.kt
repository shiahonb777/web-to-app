package com.webtoapp.core.stats

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.repository.WebAppRepository
import java.io.InputStream





class BatchImportService(
    private val context: Context,
    private val repository: WebAppRepository
) {
    companion object {
        private const val TAG = "BatchImportService"
    }

    private val gson: Gson by lazy { GsonBuilder().setPrettyPrinting().create() }




    data class ParsedEntry(
        val name: String,
        val url: String
    )








    fun parseFromText(text: String): List<ParsedEntry> {
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("//") }
            .mapNotNull { line ->
                when {

                    line.contains("|") -> {
                        val parts = line.split("|", limit = 2)
                        val name = parts[0].trim()
                        val url = parts[1].trim()
                        if (url.isValidUrl()) ParsedEntry(name.ifBlank { extractName(url) }, normalizeUrl(url))
                        else null
                    }

                    line.isValidUrl() -> {
                        ParsedEntry(extractName(line), normalizeUrl(line))
                    }

                    line.contains(" ") -> {
                        val lastSpace = line.lastIndexOf(" ")
                        val possibleUrl = line.substring(lastSpace + 1).trim()
                        if (possibleUrl.isValidUrl()) {
                            val name = line.substring(0, lastSpace).trim()
                            ParsedEntry(name.ifBlank { extractName(possibleUrl) }, normalizeUrl(possibleUrl))
                        } else null
                    }
                    else -> null
                }
            }
            .distinctBy { it.url }
    }





    fun parseFromBookmarksHtml(input: InputStream): List<ParsedEntry> {
        return try {
            val html = input.bufferedReader().readText()
            val regex = Regex("""<A\s+HREF="([^"]+)"[^>]*>([^<]+)</A>""", RegexOption.IGNORE_CASE)
            regex.findAll(html).mapNotNull { match ->
                val url = match.groupValues[1]
                val name = match.groupValues[2].trim()
                if (url.isValidUrl()) {
                    ParsedEntry(name.ifBlank { extractName(url) }, normalizeUrl(url))
                } else null
            }.distinctBy { it.url }.toList()
        } catch (e: Exception) {
            AppLogger.e(TAG, "解析书签文件失败: ${e.message}")
            emptyList()
        }
    }




    suspend fun importEntries(entries: List<ParsedEntry>): Int {
        if (entries.isEmpty()) return 0

        val webApps = entries.map { entry ->
            WebApp(
                name = entry.name,
                url = entry.url
            )
        }

        val ids = repository.createWebApps(webApps)
        AppLogger.i(TAG, "批量导入 ${ids.size} 个应用")
        return ids.size
    }




    fun exportAsTemplate(app: WebApp): String {
        val template = AppTemplate(
            name = app.name,
            url = app.url,
            appType = app.appType.name,
            webViewConfig = app.webViewConfig,
            adBlockEnabled = app.adBlockEnabled,
            adBlockRules = app.adBlockRules,
            extensionModuleIds = app.extensionModuleIds,
            extensionEnabled = app.extensionEnabled,
            splashEnabled = app.splashEnabled,
            bgmEnabled = app.bgmEnabled,
            translateEnabled = app.translateEnabled
        )
        return gson.toJson(template)
    }




    fun parseTemplate(json: String): AppTemplate? {
        return try {
            gson.fromJson(json, AppTemplate::class.java)
        } catch (e: Exception) {
            AppLogger.e(TAG, "解析模板失败: ${e.message}")
            null
        }
    }




    suspend fun importFromTemplate(template: AppTemplate): Long {
        val app = WebApp(
            name = template.name,
            url = template.url,
            adBlockEnabled = template.adBlockEnabled,
            adBlockRules = template.adBlockRules,
            extensionModuleIds = template.extensionModuleIds,
            extensionEnabled = template.extensionEnabled || template.extensionModuleIds.isNotEmpty()
        )
        return repository.createWebApp(app)
    }



    private fun String.isValidUrl(): Boolean {
        return startsWith("http://") || startsWith("https://") ||
               matches(Regex("""^[a-zA-Z0-9][-a-zA-Z0-9]*\.[a-zA-Z]{2,}.*"""))
    }

    private fun normalizeUrl(url: String): String {
        return if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else url
    }

    private fun extractName(url: String): String {
        return try {
            val host = java.net.URL(normalizeUrl(url)).host
            host.removePrefix("www.")
                .substringBeforeLast(".")
                .replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            url.take(30)
        }
    }
}




data class AppTemplate(
    val name: String,
    val url: String,
    val appType: String = "WEB",
    val webViewConfig: com.webtoapp.data.model.WebViewConfig = com.webtoapp.data.model.WebViewConfig(),
    val adBlockEnabled: Boolean = false,
    val adBlockRules: List<String> = emptyList(),
    val extensionModuleIds: List<String> = emptyList(),
    val extensionEnabled: Boolean = false,
    val splashEnabled: Boolean = false,
    val bgmEnabled: Boolean = false,
    val translateEnabled: Boolean = false,
    val version: Int = 1
)
