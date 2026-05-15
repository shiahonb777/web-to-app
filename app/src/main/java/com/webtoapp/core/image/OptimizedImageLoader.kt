package com.webtoapp.core.image

import android.content.Context
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.webtoapp.core.logging.AppLogger
import okhttp3.OkHttpClient

object OptimizedImageLoader {

    private const val TAG = "OptimizedImageLoader"
    private const val DISK_CACHE_MAX_SIZE = 256L * 1024 * 1024 // 256MB
    private const val MEMORY_CACHE_MAX_SIZE = 0.25 // 25% of app memory

    private var _loader: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return _loader ?: synchronized(this) {
            _loader ?: buildImageLoader(context.applicationContext).also { _loader = it }
        }
    }

    private fun buildImageLoader(context: Context): ImageLoader {
        AppLogger.i(TAG, "Initializing optimized Coil ImageLoader")

        return ImageLoader.Builder(context)
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
            }
            .components {


                add(VideoFrameDecoder.Factory())
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(MEMORY_CACHE_MAX_SIZE)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil_image_cache"))
                    .maxSizeBytes(DISK_CACHE_MAX_SIZE)
                    .build()
            }
            .crossfade(false) // Default off for list performance; enable per-request where needed
            .respectCacheHeaders(true)
            .logger(null) // Disable debug logging in production
            .build()
    }
}
