package com.webtoapp.core.startup

import android.content.Context
import com.webtoapp.core.crypto.SecurityInitializer
import com.webtoapp.core.logging.AppLogger

class SecurityStartup(private val context: Context) {

    fun initialize() {
        try {
            val initialized = SecurityInitializer.initialize(context) { result ->
                AppLogger.w(
                    "WebToAppApplication",
                    "Security threat detected: level=${result.threatLevel}, block=${result.shouldBlock}, threats=${result.threats}"
                )
            }
            AppLogger.i("WebToAppApplication", "Security initializer status: $initialized")
        } catch (e: Exception) {
            AppLogger.e("WebToAppApplication", "Security initialization failed", e)
        } catch (e: Error) {
            AppLogger.e("WebToAppApplication", "Security initialization critical error", Error(e))
        }
    }

    fun shutdown() {
        SecurityInitializer.shutdown()
    }
}
