package com.webtoapp.core.network

import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * network.
 * OkHttpClient.
 * 
 * usage.
 * NetworkModule.defaultClient.
 * AI NetworkModule.streamingClient.
 * file download.
 */
object NetworkModule {

    private const val DEFAULT_USER_AGENT = "WebToApp/1.0 (Android)"

    /** Note. */
    private val sharedConnectionPool = ConnectionPool(8, 60, TimeUnit.SECONDS)

    /** blocker. */
    private val userAgentInterceptor = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", DEFAULT_USER_AGENT)
                .build()
        )
    }

    /**
     * default.
     * read write timeout.
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
     * AI SSE.
     * read generate timeout.
     */
    val streamingClient: OkHttpClient by lazy {
        defaultClient.newBuilder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    /**
     * file dependency download.
     * read file timeout.
     */
    val downloadClient: OkHttpClient by lazy {
        defaultClient.newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * config default.
     * Note.
     */
    fun customClient(configure: OkHttpClient.Builder.() -> Unit): OkHttpClient {
        return defaultClient.newBuilder().apply(configure).build()
    }
}