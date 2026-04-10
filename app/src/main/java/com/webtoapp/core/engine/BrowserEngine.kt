package com.webtoapp.core.engine

import android.content.Context
import android.view.View
import com.webtoapp.data.model.WebViewConfig

/**
 * 浏览器引擎统一接口
 * 所有引擎实现（SystemWebView、GeckoView）都需要实现此接口
 */
interface BrowserEngine {

    /** 引擎类型 */
    val engineType: EngineType

    /**
     * 创建并配置浏览器 View
     * @param context Android Context
     * @param config WebView 配置
     * @param callback 引擎回调
     * @return 配置好的 View 实例（WebView 或 GeckoView）
     */
    fun createView(
        context: Context,
        config: WebViewConfig,
        callback: BrowserEngineCallback
    ): View

    /**
     * 加载 URL
     * @param url 要加载的地址
     */
    fun loadUrl(url: String)

    /**
     * 执行 JavaScript
     * @param script JS 代码
     * @param resultCallback 执行结果回调（可选）
     */
    fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)? = null)

    /**
     * 是否可以后退
     */
    fun canGoBack(): Boolean

    /**
     * 后退
     */
    fun goBack()

    /**
     * 是否可以前进
     */
    fun canGoForward(): Boolean

    /**
     * 前进
     */
    fun goForward()

    /**
     * 重新加载
     */
    fun reload()

    /**
     * 停止加载
     */
    fun stopLoading()

    /**
     * 获取当前 URL
     */
    fun getCurrentUrl(): String?

    /**
     * 获取当前页面标题
     */
    fun getTitle(): String?

    /**
     * 获取底层 View（用于直接操作，如设置焦点等）
     */
    fun getView(): View?

    /**
     * 请求焦点
     */
    fun requestFocus() {
        getView()?.requestFocus()
    }

    /**
     * 销毁引擎实例，释放资源
     */
    fun destroy()

    /**
     * 清除缓存
     * @param includeDiskFiles 是否包括磁盘缓存
     */
    fun clearCache(includeDiskFiles: Boolean = true)

    /**
     * 清除历史记录
     */
    fun clearHistory()

    /**
     * 获取浏览器隐私保护 Shields 实例
     * @return BrowserShields 实例，未初始化时返回 null
     */
    fun getShields(): com.webtoapp.core.engine.shields.BrowserShields? = null

    /**
     * 切换阅读模式
     * @param enabled 是否开启阅读模式
     */
    fun toggleReaderMode(enabled: Boolean) {}
}
