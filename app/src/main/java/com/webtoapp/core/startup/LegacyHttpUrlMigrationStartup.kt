package com.webtoapp.core.startup

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.AppType
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.util.isInsecureRemoteHttpUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LegacyHttpUrlMigrationStartup(
    private val context: Context,
    private val webAppRepository: WebAppRepository,
) {

    fun initialize(appScope: CoroutineScope) {
        appScope.launch {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_HTTP_URL_MIGRATED, false)) return@launch

            runCatching {
                val allApps = webAppRepository.allWebApps.first()
                val appsToUpdate = allApps
                    .filter { it.appType == AppType.WEB && isInsecureRemoteHttpUrl(it.url) }
                    .map { app ->
                        app.copy(url = app.url.replaceFirst(Regex("(?i)^http://"), "https://"))
                    }

                if (appsToUpdate.isNotEmpty()) {
                    webAppRepository.updateWebApps(appsToUpdate)
                    AppLogger.i(
                        "WebToAppApplication",
                        "Migrated ${appsToUpdate.size} legacy insecure HTTP web URL(s) to HTTPS"
                    )
                }

                prefs.edit().putBoolean(KEY_HTTP_URL_MIGRATED, true).apply()
            }.onFailure { e ->
                AppLogger.e("WebToAppApplication", "Failed to migrate legacy HTTP URLs", Exception(e))
            }
        }
    }

    private companion object {
        private const val PREFS_NAME = "security_migrations"
        private const val KEY_HTTP_URL_MIGRATED = "legacy_http_url_migrated_v1"
    }
}
