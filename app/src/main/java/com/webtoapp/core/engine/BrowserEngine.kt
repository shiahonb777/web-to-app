package com.webtoapp.core.engine

import android.content.Context
import android.view.View
import com.webtoapp.data.model.WebViewConfig





interface BrowserEngine {


    val engineType: EngineType








    fun createView(
        context: Context,
        config: WebViewConfig,
        callback: BrowserEngineCallback
    ): View





    fun loadUrl(url: String)






    fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)? = null)




    fun canGoBack(): Boolean




    fun goBack()




    fun canGoForward(): Boolean




    fun goForward()




    fun reload()




    fun stopLoading()




    fun getCurrentUrl(): String?




    fun getTitle(): String?




    fun getView(): View?




    fun requestFocus() {
        getView()?.requestFocus()
    }




    fun destroy()





    fun clearCache(includeDiskFiles: Boolean = true)




    fun clearHistory()





    fun getShields(): com.webtoapp.core.engine.shields.BrowserShields? = null





    fun toggleReaderMode(enabled: Boolean) {}
}
