package com.webtoapp.core.engine

import android.annotation.SuppressLint
import android.content.Context
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.engine.download.EngineFileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow





@SuppressLint("StaticFieldLeak")
class EngineManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: EngineManager? = null

        fun getInstance(context: Context): EngineManager {
            return instance ?: synchronized(this) {
                instance ?: EngineManager(context.applicationContext).also { instance = it }
            }
        }
    }

    val fileManager = EngineFileManager(context)


    private val _selectedEngine = MutableStateFlow(EngineType.SYSTEM_WEBVIEW)
    val selectedEngine: StateFlow<EngineType> = _selectedEngine.asStateFlow()




    fun selectEngine(type: EngineType) {
        _selectedEngine.value = type
    }






    fun createEngine(type: EngineType, adBlocker: AdBlocker): BrowserEngine {
        return when (type) {
            EngineType.SYSTEM_WEBVIEW -> SystemWebViewEngine(context, adBlocker)
            EngineType.GECKOVIEW -> GeckoViewEngine(context)
        }
    }




    fun isEngineAvailable(type: EngineType): Boolean {
        return when (type) {
            EngineType.SYSTEM_WEBVIEW -> true
            EngineType.GECKOVIEW -> fileManager.isEngineDownloaded(EngineType.GECKOVIEW)
        }
    }




    fun getEngineStatus(type: EngineType): EngineStatus {
        return when {
            !type.requiresDownload -> EngineStatus.READY
            fileManager.isEngineDownloaded(type) -> {
                val version = fileManager.getDownloadedVersion(type)
                EngineStatus.DOWNLOADED(version ?: "unknown")
            }
            else -> EngineStatus.NOT_DOWNLOADED
        }
    }




    fun getEngineSize(type: EngineType): Long {
        return fileManager.getEngineSize(type)
    }




    fun deleteEngine(type: EngineType): Boolean {
        return fileManager.deleteEngineFiles(type)
    }
}




sealed class EngineStatus {

    data object READY : EngineStatus()


    data class DOWNLOADED(val version: String) : EngineStatus()


    data object NOT_DOWNLOADED : EngineStatus()
}
