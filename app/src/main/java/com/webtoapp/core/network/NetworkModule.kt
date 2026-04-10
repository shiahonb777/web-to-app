package com.webtoapp.core.network

import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 统一网络层管理
 * 提供共享的 OkHttpClient 实例，避免各模块重复创建连接池
 * 
 * 使用方式：
 * - 普通请求：NetworkModule.defaultClient
 * - AI 流式请求：NetworkModule.streamingClient
 * - 大文件下载：NetworkModule.downloadClient
 */
object NetworkModule {

    private const val DEFAULT_USER_AGENT = "WebToApp/1.0 (Android)"

    /** 共享连接池：最多 8 个空闲连接，存活 60 秒 */
    private val sharedConnectionPool = ConnectionPool(8, 60, TimeUnit.SECONDS)

    /** User-Agent 拦截器 */
    private val userAgentInterceptor = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", DEFAULT_USER_AGENT)
                .build()
        )
    }

    /**
     * 默认客户端 — 普通 API 请求、网页抓取
     * 连接超时 15s / 读取 30s / 写入 30s
     */
    val defaultClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(sharedConnectionPool)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor(userAgentInterceptor)
            .build()
    }

    /**
     * 流式客户端 — AI 流式响应、SSE
     * 读取超时 300s（5 分钟），适配长时间流式生成
     */
    val streamingClient: OkHttpClient by lazy {
        defaultClient.newBuilder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 下载客户端 — 大文件下载（GeckoView、依赖包、音乐等）
     * 读取超时 120s，适配大文件传输
     */
    val downloadClient: OkHttpClient by lazy {
        defaultClient.newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 基于默认客户端派生自定义配置
     * 共享连接池，仅覆盖需要的参数
     */
    fun customClient(configure: OkHttpClient.Builder.() -> Unit): OkHttpClient {
        return defaultClient.newBuilder().apply(configure).build()
    }
}
