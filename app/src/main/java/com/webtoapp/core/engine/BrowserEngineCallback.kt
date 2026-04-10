package com.webtoapp.core.engine

import android.graphics.Bitmap
import android.view.View

/**
 * 浏览器引擎统一回调接口
 * 引擎无关的回调抽象，SystemWebView 和 GeckoView 都通过此接口通知上层
 */
interface BrowserEngineCallback {

    /** 页面开始加载 */
    fun onPageStarted(url: String?)

    /** 页面加载完成 */
    fun onPageFinished(url: String?)

    /** 加载进度变化 (0-100) */
    fun onProgressChanged(progress: Int)

    /** 页面标题变化 */
    fun onTitleChanged(title: String?)

    /** 收到页面图标 */
    fun onIconReceived(icon: Bitmap?)

    /** 页面加载错误 */
    fun onError(errorCode: Int, description: String)

    /** SSL 证书错误 */
    fun onSslError(error: String)

    /** 外部链接（需要在外部浏览器打开） */
    fun onExternalLink(url: String)

    /** 进入视频全屏 */
    fun onShowCustomView(view: View?, callback: Any?)

    /** 退出视频全屏 */
    fun onHideCustomView()

    /** 下载请求 */
    fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    )

    /** 控制台消息 */
    fun onConsoleMessage(level: Int, message: String, sourceId: String, lineNumber: Int) {}

    /** 新窗口请求 */
    fun onNewWindow(resultMsg: android.os.Message?) {}
}
