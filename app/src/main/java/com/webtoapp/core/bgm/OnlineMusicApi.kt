package com.webtoapp.core.bgm

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 网易云音乐 API 客户端
 * API 文档: https://oiapi.net/api/Music_163
 */
object OnlineMusicApi {
    
    private const val BASE_URL = "https://oiapi.net/api/Music_163"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    /**
     * Search歌曲
     * @param name 歌曲名称
     * @param n 歌曲序号（可选）
     * @return 搜索结果
     */
    suspend fun searchMusic(name: String, n: Int? = null): OnlineMusicResult {
        return withContext(Dispatchers.IO) {
            try {
                val urlBuilder = StringBuilder(BASE_URL)
                urlBuilder.append("?name=").append(java.net.URLEncoder.encode(name, "UTF-8"))
                if (n != null) {
                    urlBuilder.append("&n=").append(n)
                }
                
                val request = Request.Builder()
                    .url(urlBuilder.toString())
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && body != null) {
                    val apiResponse = gson.fromJson(body, OnlineMusicApiResponse::class.java)
                    if (apiResponse.code == 0 && apiResponse.data != null) {
                        OnlineMusicResult.Success(apiResponse.data)
                    } else {
                        OnlineMusicResult.Error(apiResponse.message ?: "获取失败")
                    }
                } else {
                    OnlineMusicResult.Error("网络请求失败: ${response.code}")
                }
            } catch (e: Exception) {
                OnlineMusicResult.Error("请求异常: ${e.message}")
            }
        }
    }
    
    /**
     * 根据歌曲ID获取详情
     * @param id 歌曲ID
     * @return 歌曲详情
     */
    suspend fun getMusicById(id: Long): OnlineMusicResult {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL?id=$id"
                
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && body != null) {
                    val apiResponse = gson.fromJson(body, OnlineMusicApiResponse::class.java)
                    if (apiResponse.code == 0 && apiResponse.data != null) {
                        OnlineMusicResult.Success(apiResponse.data)
                    } else {
                        OnlineMusicResult.Error(apiResponse.message ?: "获取失败")
                    }
                } else {
                    OnlineMusicResult.Error("网络请求失败: ${response.code}")
                }
            } catch (e: Exception) {
                OnlineMusicResult.Error("请求异常: ${e.message}")
            }
        }
    }
    
    /**
     * 获取随机歌曲（不传参数时返回随机歌曲）
     */
    suspend fun getRandomMusic(): OnlineMusicResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(BASE_URL)
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && body != null) {
                    val apiResponse = gson.fromJson(body, OnlineMusicApiResponse::class.java)
                    if (apiResponse.code == 0 && apiResponse.data != null) {
                        OnlineMusicResult.Success(apiResponse.data)
                    } else {
                        OnlineMusicResult.Error(apiResponse.message ?: "获取失败")
                    }
                } else {
                    OnlineMusicResult.Error("网络请求失败: ${response.code}")
                }
            } catch (e: Exception) {
                OnlineMusicResult.Error("请求异常: ${e.message}")
            }
        }
    }
}

/**
 * API 响应数据结构
 */
data class OnlineMusicApiResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: OnlineMusicData?
)

/**
 * 歌曲数据
 */
data class OnlineMusicData(
    @SerializedName("name") val name: String,
    @SerializedName("picurl") val coverUrl: String?,
    @SerializedName("id") val id: Long,
    @SerializedName("singers") val singers: List<OnlineSinger>?,
    @SerializedName("url") val url: String,
    @SerializedName("pay") val isPaid: Boolean = false
)

/**
 * 歌手信息
 */
data class OnlineSinger(
    @SerializedName("name") val name: String,
    @SerializedName("id") val id: Long
)

/**
 * API 请求结果
 */
sealed class OnlineMusicResult {
    data class Success(val data: OnlineMusicData) : OnlineMusicResult()
    data class Error(val message: String) : OnlineMusicResult()
}
