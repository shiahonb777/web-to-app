package com.webtoapp.core.network

import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit










object NetworkModule {

    private const val DEFAULT_USER_AGENT = "WebToApp/1.0 (Android)"


    private val sharedConnectionPool = ConnectionPool(8, 60, TimeUnit.SECONDS)


    private val userAgentInterceptor = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", DEFAULT_USER_AGENT)
                .build()
        )
    }





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





    val streamingClient: OkHttpClient by lazy {
        defaultClient.newBuilder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }





    val downloadClient: OkHttpClient by lazy {
        defaultClient.newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }





    fun customClient(configure: OkHttpClient.Builder.() -> Unit): OkHttpClient {
        return defaultClient.newBuilder().apply(configure).build()
    }
}
