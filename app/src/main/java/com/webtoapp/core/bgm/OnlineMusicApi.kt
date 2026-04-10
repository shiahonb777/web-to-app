package com.webtoapp.core.bgm

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.webtoapp.util.GsonProvider
import com.google.gson.annotations.SerializedName
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.webtoapp.core.network.NetworkModule
import okhttp3.Request

// ==================== Data Models ====================

/**
 * 在线音乐曲目
 */
data class OnlineMusicTrack(
    val id: String,
    val name: String,
    val artist: String,
    val album: String = "",
    val coverUrl: String? = null,
    val playUrl: String? = null,
    val duration: Long = 0,
    val sourceChannelId: String = "",
    val lrcText: String? = null,
    val searchQuery: String? = null,
    val resultIndex: Int = 0
)

/**
 * 搜索响应
 */
data class MusicSearchResponse(
    val tracks: List<OnlineMusicTrack>,
    val hasMore: Boolean = false,
    val total: Int = 0
)

/**
 * 渠道连接状态
 */
data class ChannelStatus(
    val channelId: String,
    val isAvailable: Boolean,
    val latencyMs: Long = 0,
    val errorMessage: String? = null
)

// ==================== Backward Compatibility ====================

data class OnlineMusicData(
    @SerializedName("name") val name: String,
    @SerializedName("picurl") val coverUrl: String?,
    @SerializedName("id") val id: Long,
    @SerializedName("singers") val singers: List<OnlineSinger>?,
    @SerializedName("url") val url: String,
    @SerializedName("pay") val isPaid: Boolean = false
)

data class OnlineSinger(
    @SerializedName("name") val name: String,
    @SerializedName("id") val id: Long
)

fun OnlineMusicTrack.toOnlineMusicData(): OnlineMusicData {
    return OnlineMusicData(
        name = this.name,
        coverUrl = this.coverUrl,
        id = this.id.toLongOrNull() ?: 0L,
        singers = listOf(OnlineSinger(name = this.artist, id = 0)),
        url = this.playUrl ?: "",
        isPaid = false
    )
}

// ==================== Channel Interface ====================

/**
 * 音乐渠道抽象类
 */
abstract class MusicChannel {
    abstract val id: String
    abstract val displayName: String
    abstract val description: String

    abstract suspend fun search(query: String, page: Int = 1): Result<MusicSearchResponse>
    abstract suspend fun getTrackDetail(track: OnlineMusicTrack): Result<OnlineMusicTrack>
    abstract suspend fun testConnection(): ChannelStatus

    companion object {
        private val sharedGson get() = GsonProvider.gson
    }

    protected val client get() = NetworkModule.defaultClient
    protected val gson: Gson get() = sharedGson

    protected fun executeRequest(url: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val builder = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            headers.forEach { (k, v) -> builder.header(k, v) }
            val request = builder.build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) response.body?.string() else null
        } catch (e: Exception) {
            AppLogger.e("MusicChannel", "Request failed: $url", e)
            null
        }
    }

    protected fun measureLatency(testBlock: () -> Boolean): ChannelStatus {
        val start = System.currentTimeMillis()
        return try {
            val ok = testBlock()
            val latency = System.currentTimeMillis() - start
            ChannelStatus(id, ok, latency, if (!ok) "API returned error" else null)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            ChannelStatus(id, false, latency, e.message)
        }
    }
}

// ==================== Channel: NetEase Cloud Music (oiapi.net) ====================

/**
 * 网易云音乐渠道（oiapi.net 代理）
 * 搜索列表模式，返回歌曲列表
 */
class NetEaseChannel : MusicChannel() {
    override val id = "netease"
    override val displayName = "网易云音乐"
    override val description = "NetEase Cloud Music"
    private val baseUrl = "https://oiapi.net/api/Music_163"

    override suspend fun search(query: String, page: Int): Result<MusicSearchResponse> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val body = executeRequest("$baseUrl?name=$encoded")
                    ?: return@withContext Result.failure(Exception("网络请求失败"))
                val json = JsonParser.parseString(body).asJsonObject
                if (json.get("code")?.asInt != 0)
                    return@withContext Result.failure(Exception(json.get("message")?.asString ?: "搜索失败"))

                val tracks = mutableListOf<OnlineMusicTrack>()
                val data = json.get("data")
                if (data != null && data.isJsonArray) {
                    data.asJsonArray.forEachIndexed { idx, el ->
                        val obj = el.asJsonObject
                        val songName = obj.get("name")?.asString ?: ""
                        val artistNames = obj.getAsJsonArray("singers")
                            ?.joinToString("、") { it.asJsonObject.get("name")?.asString ?: "" }
                            ?: "未知歌手"
                        tracks.add(OnlineMusicTrack(
                            id = obj.get("id")?.asString ?: "$idx",
                            name = songName,
                            artist = artistNames,
                            coverUrl = obj.get("picurl")?.asString,
                            playUrl = null,
                            sourceChannelId = id,
                            searchQuery = query,
                            resultIndex = idx + 1
                        ))
                    }
                } else if (data != null && data.isJsonObject) {
                    val obj = data.asJsonObject
                    val songName = obj.get("name")?.asString ?: ""
                    val artistNames = obj.getAsJsonArray("singers")
                        ?.joinToString("、") { it.asJsonObject.get("name")?.asString ?: "" }
                        ?: "未知歌手"
                    tracks.add(OnlineMusicTrack(
                        id = obj.get("id")?.asString ?: "0",
                        name = songName,
                        artist = artistNames,
                        coverUrl = obj.get("picurl")?.asString,
                        playUrl = obj.get("url")?.asString,
                        sourceChannelId = id,
                        searchQuery = query,
                        resultIndex = 1
                    ))
                }

                // 智能排序：将与搜索关键词高度匹配的结果排在前面
                val sortedTracks = smartSort(tracks, query)
                Result.success(MusicSearchResponse(sortedTracks, total = sortedTracks.size))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getTrackDetail(track: OnlineMusicTrack): Result<OnlineMusicTrack> =
        withContext(Dispatchers.IO) {
            try {
                val query = track.searchQuery
                val url = if (query != null) {
                    val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                    "$baseUrl?name=$encoded&n=${track.resultIndex}"
                } else {
                    "$baseUrl?id=${track.id}"
                }
                AppLogger.i("NetEaseChannel", "Getting detail: $url")
                val body = executeRequest(url)
                    ?: return@withContext Result.failure(Exception("网络请求失败"))
                val json = JsonParser.parseString(body).asJsonObject
                if (json.get("code")?.asInt != 0)
                    return@withContext Result.failure(Exception("获取详情失败"))
                val data = json.getAsJsonObject("data")
                    ?: return@withContext Result.failure(Exception("数据为空"))
                
                val playUrl = data.get("url")?.asString
                if (playUrl.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("无法获取播放链接（可能是付费歌曲）"))
                }
                
                Result.success(track.copy(
                    playUrl = playUrl,
                    coverUrl = data.get("picurl")?.asString ?: track.coverUrl
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun testConnection(): ChannelStatus = withContext(Dispatchers.IO) {
        measureLatency {
            val body = executeRequest("$baseUrl?name=test") ?: return@measureLatency false
            val json = JsonParser.parseString(body).asJsonObject
            json.get("code")?.asInt == 0
        }
    }
}

// ==================== Channel: NetEase Official Search API ====================

/**
 * 网易云音乐 官方搜索 API
 * 使用 music.163.com/api/search 接口，搜索准确度极高
 * 结果按热门排序，能正确找到原唱歌曲
 */
class NetEaseOfficialChannel : MusicChannel() {
    override val id = "netease_official"
    override val displayName = "网易云(精准)"
    override val description = "NetEase Official Search (Accurate)"
    private val searchUrl = "https://music.163.com/api/search/get/web"
    private val detailBaseUrl = "https://oiapi.net/api/Music_163"

    override suspend fun search(query: String, page: Int): Result<MusicSearchResponse> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val offset = (page - 1) * 20
                val url = "$searchUrl?s=$encoded&type=1&offset=$offset&limit=20"
                val body = executeRequest(url, mapOf(
                    "Referer" to "https://music.163.com/"
                )) ?: return@withContext Result.failure(Exception("网络请求失败"))

                val json = JsonParser.parseString(body).asJsonObject
                val result = json.getAsJsonObject("result")
                    ?: return@withContext Result.failure(Exception("搜索无结果"))
                val songs = result.getAsJsonArray("songs")
                    ?: return@withContext Result.success(MusicSearchResponse(emptyList()))

                val totalCount = result.get("songCount")?.asInt ?: 0
                val tracks = songs.mapIndexed { idx, el ->
                    val obj = el.asJsonObject
                    val songName = obj.get("name")?.asString ?: ""
                    val artists = obj.getAsJsonArray("artists")
                        ?.joinToString("、") { it.asJsonObject.get("name")?.asString ?: "" }
                        ?: "未知歌手"
                    val album = obj.getAsJsonObject("album")?.get("name")?.asString ?: ""
                    val songId = obj.get("id")?.asString ?: "$idx"
                    val duration = obj.get("duration")?.asLong ?: 0L

                    OnlineMusicTrack(
                        id = songId,
                        name = songName,
                        artist = artists,
                        album = album,
                        coverUrl = null, // 官方搜索 API 不返回封面，在详情中获取
                        playUrl = null,
                        duration = duration,
                        sourceChannelId = id,
                        searchQuery = query,
                        resultIndex = idx + 1
                    )
                }

                Result.success(MusicSearchResponse(
                    tracks = tracks,
                    hasMore = offset + 20 < totalCount,
                    total = totalCount
                ))
            } catch (e: Exception) {
                AppLogger.e("NetEaseOfficialChannel", "Search failed", e)
                Result.failure(e)
            }
        }

    override suspend fun getTrackDetail(track: OnlineMusicTrack): Result<OnlineMusicTrack> =
        withContext(Dispatchers.IO) {
            try {
                // 通过 oiapi.net 的 id 参数获取播放链接和封面
                val url = "$detailBaseUrl?id=${track.id}"
                AppLogger.i("NetEaseOfficialChannel", "Getting detail: $url")
                val body = executeRequest(url)

                if (body != null) {
                    val json = JsonParser.parseString(body).asJsonObject
                    if (json.get("code")?.asInt == 0) {
                        val data = json.get("data")
                        val dataObj = when {
                            data != null && data.isJsonObject -> data.asJsonObject
                            data != null && data.isJsonArray && data.asJsonArray.size() > 0 ->
                                data.asJsonArray[0].asJsonObject
                            else -> null
                        }

                        if (dataObj != null) {
                            val playUrl = dataObj.get("url")?.asString
                            val coverUrl = dataObj.get("picurl")?.asString
                            if (!playUrl.isNullOrBlank()) {
                                return@withContext Result.success(track.copy(
                                    playUrl = playUrl,
                                    coverUrl = coverUrl ?: track.coverUrl
                                ))
                            }
                        }
                    }
                }

                // 备用：通过搜索 name 获取
                val fallbackQuery = "${track.name} ${track.artist.split("、").firstOrNull() ?: ""}"
                val encoded = java.net.URLEncoder.encode(fallbackQuery.trim(), "UTF-8")
                val fallbackUrl = "$detailBaseUrl?name=$encoded&n=1"
                AppLogger.i("NetEaseOfficialChannel", "Fallback detail: $fallbackUrl")
                val fallbackBody = executeRequest(fallbackUrl)
                    ?: return@withContext Result.failure(Exception("获取详情失败"))
                val fbJson = JsonParser.parseString(fallbackBody).asJsonObject
                if (fbJson.get("code")?.asInt != 0)
                    return@withContext Result.failure(Exception("获取详情失败"))
                val fbData = fbJson.getAsJsonObject("data")
                    ?: return@withContext Result.failure(Exception("数据为空"))

                val playUrl = fbData.get("url")?.asString
                if (playUrl.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("无法获取播放链接（可能是付费/VIP歌曲）"))
                }

                Result.success(track.copy(
                    playUrl = playUrl,
                    coverUrl = fbData.get("picurl")?.asString ?: track.coverUrl
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun testConnection(): ChannelStatus = withContext(Dispatchers.IO) {
        measureLatency {
            val body = executeRequest(
                "$searchUrl?s=test&type=1&offset=0&limit=1",
                mapOf("Referer" to "https://music.163.com/")
            ) ?: return@measureLatency false
            val json = JsonParser.parseString(body).asJsonObject
            json.getAsJsonObject("result")?.get("songCount")?.asInt?.let { it >= 0 } ?: false
        }
    }
}

// ==================== Channel: QiShui Music (汽水音乐/抖音) ====================

/**
 * 汽水音乐渠道
 * 注意：此 API 一次只返回一首歌，需要通过 n 参数偏移获取多个结果
 * 搜索准确度较好
 */
class QiShuiChannel : MusicChannel() {
    override val id = "qishui"
    override val displayName = "汽水音乐"
    override val description = "QiShui Music (Douyin)"
    private val baseUrl = "https://api.cenguigui.cn/api/qishui"

    override suspend fun search(query: String, page: Int): Result<MusicSearchResponse> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val tracks = mutableListOf<OnlineMusicTrack>()
                // 获取最多 8 个结果
                val startN = (page - 1) * 8 + 1
                val endN = startN + 7
                for (n in startN..endN) {
                    try {
                        val body = executeRequest("$baseUrl/?msg=$encoded&type=json&n=$n")
                            ?: continue
                        val json = JsonParser.parseString(body).asJsonObject
                        if (json.get("code")?.asInt != 200) continue
                        val data = json.getAsJsonObject("data") ?: continue
                        
                        val title = data.get("title")?.asString ?: continue
                        val singer = data.get("singer")?.asString ?: "未知歌手"
                        val musicUrl = data.get("music")?.asString
                        val cover = data.get("cover")?.asString
                        val lrc = data.get("lrc")?.asString
                        val isPay = data.get("pay")?.asString == "pay"
                        
                        // 跳过完全无关的结果（歌名或歌手不包含任何搜索词字符）
                        if (!isRelevant(title, singer, query)) {
                            AppLogger.d("QiShuiChannel", "Skipping irrelevant: $title - $singer (query=$query)")
                            continue
                        }
                        
                        tracks.add(OnlineMusicTrack(
                            id = "qishui_${n}_${title.hashCode()}",
                            name = title + if (isPay) " (替换源)" else "",
                            artist = singer,
                            coverUrl = cover,
                            playUrl = musicUrl,
                            lrcText = lrc,
                            sourceChannelId = id,
                            searchQuery = query,
                            resultIndex = n
                        ))
                    } catch (e: Exception) {
                        AppLogger.w("QiShuiChannel", "Failed to get result $n: ${e.message}")
                    }
                }
                
                if (tracks.isEmpty()) {
                    return@withContext Result.failure(Exception("未找到相关歌曲"))
                }
                Result.success(MusicSearchResponse(tracks, total = tracks.size))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getTrackDetail(track: OnlineMusicTrack): Result<OnlineMusicTrack> {
        if (!track.playUrl.isNullOrBlank()) return Result.success(track)
        
        return withContext(Dispatchers.IO) {
            try {
                val query = track.searchQuery ?: return@withContext Result.failure(Exception("无搜索关键词"))
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val body = executeRequest("$baseUrl/?msg=$encoded&type=json&n=${track.resultIndex}")
                    ?: return@withContext Result.failure(Exception("网络请求失败"))
                val json = JsonParser.parseString(body).asJsonObject
                if (json.get("code")?.asInt != 200)
                    return@withContext Result.failure(Exception("获取详情失败"))
                val data = json.getAsJsonObject("data")
                    ?: return@withContext Result.failure(Exception("数据为空"))
                
                val musicUrl = data.get("music")?.asString
                if (musicUrl.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("无法获取播放链接"))
                }
                
                Result.success(track.copy(
                    playUrl = musicUrl,
                    coverUrl = data.get("cover")?.asString ?: track.coverUrl,
                    lrcText = data.get("lrc")?.asString ?: track.lrcText
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun testConnection(): ChannelStatus = withContext(Dispatchers.IO) {
        measureLatency {
            val body = executeRequest("$baseUrl/?msg=test&type=json&n=1") ?: return@measureLatency false
            val json = JsonParser.parseString(body).asJsonObject
            json.get("code")?.asInt == 200
        }
    }
}

// ==================== Channel: iTunes Search ====================

/**
 * iTunes 搜索渠道
 * 搜索精准度极高（Apple 官方），但仅提供 30 秒预览
 */
class ITunesChannel : MusicChannel() {
    override val id = "itunes"
    override val displayName = "iTunes"
    override val description = "Apple Music (30s Preview)"
    private val baseUrl = "https://itunes.apple.com/search"

    override suspend fun search(query: String, page: Int): Result<MusicSearchResponse> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val body = executeRequest("$baseUrl?term=$encoded&media=music&limit=20")
                    ?: return@withContext Result.failure(Exception("网络请求失败"))
                val json = JsonParser.parseString(body).asJsonObject
                val results = json.getAsJsonArray("results") ?: return@withContext Result.success(
                    MusicSearchResponse(emptyList())
                )
                val tracks = results.mapIndexed { idx, el ->
                    val obj = el.asJsonObject
                    OnlineMusicTrack(
                        id = obj.get("trackId")?.asString ?: "$idx",
                        name = obj.get("trackName")?.asString ?: "",
                        artist = obj.get("artistName")?.asString ?: "Unknown",
                        album = obj.get("collectionName")?.asString ?: "",
                        coverUrl = obj.get("artworkUrl100")?.asString,
                        playUrl = obj.get("previewUrl")?.asString,
                        duration = obj.get("trackTimeMillis")?.asLong ?: 0,
                        sourceChannelId = id,
                        resultIndex = idx + 1
                    )
                }
                Result.success(MusicSearchResponse(tracks, total = json.get("resultCount")?.asInt ?: 0))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getTrackDetail(track: OnlineMusicTrack): Result<OnlineMusicTrack> {
        // iTunes 的 previewUrl 在搜索结果中就已经有了
        return Result.success(track)
    }

    override suspend fun testConnection(): ChannelStatus = withContext(Dispatchers.IO) {
        measureLatency {
            val body = executeRequest("$baseUrl?term=test&media=music&limit=1")
                ?: return@measureLatency false
            val json = JsonParser.parseString(body).asJsonObject
            (json.get("resultCount")?.asInt ?: 0) >= 0
        }
    }
}

// ==================== Utility: Smart Sorting & Relevance ====================

/**
 * 智能排序搜索结果
 * 优先排列：歌名完全匹配 > 歌名包含搜索词 > 歌手包含搜索词 > 其他
 */
private fun smartSort(tracks: List<OnlineMusicTrack>, query: String): List<OnlineMusicTrack> {
    val queryLower = query.lowercase().trim()
    val queryParts = queryLower.split(" ", "　").filter { it.isNotBlank() }

    return tracks.sortedByDescending { track ->
        val nameLower = track.name.lowercase()
        val artistLower = track.artist.lowercase()
        var score = 0

        // 歌名完全匹配
        if (nameLower == queryLower) score += 1000

        // 歌名以搜索词开头
        if (nameLower.startsWith(queryLower)) score += 500

        // 歌名包含搜索词
        if (nameLower.contains(queryLower)) score += 200

        // 歌手名完全匹配
        if (artistLower == queryLower || artistLower.contains(queryLower)) score += 100

        // 多关键词部分匹配
        queryParts.forEach { part ->
            if (nameLower.contains(part)) score += 50
            if (artistLower.contains(part)) score += 30
        }

        // 惩罚翻唱/Cover 版本（降低排序）
        val coverKeywords = listOf("cover", "翻唱", "女声版", "男声版", "dj版", "remix", "live", "钢琴版", "吉他版")
        if (coverKeywords.any { nameLower.contains(it) }) {
            score -= 80
        }

        score
    }
}

/**
 * 检查搜索结果是否与查询词相关
 * 避免 API 返回完全无关的歌曲
 */
private fun isRelevant(songName: String, artist: String, query: String): Boolean {
    val q = query.lowercase().trim()
    val name = songName.lowercase()
    val art = artist.lowercase()

    // 如果歌名或歌手名包含查询词的任一字符（中文场景），认为相关
    // 对于中文搜索，只要有一个字符匹配就可能相关
    val queryChars = q.toSet()

    // 至少 30% 的查询字符出现在歌名或歌手名中
    val matchCount = queryChars.count { c -> name.contains(c) || art.contains(c) }
    val matchRatio = if (queryChars.isNotEmpty()) matchCount.toFloat() / queryChars.size else 0f

    return matchRatio >= 0.3f
}

// ==================== API Manager ====================

/**
 * 在线音乐 API 管理器
 * 支持多渠道搜索、播放、下载
 * 
 * V2 改进：
 * - 移除不可靠的 cenguigui kuwo/kugou/netease_alt 渠道（搜索结果完全随机）
 * - 新增网易云官方搜索 API 渠道（搜索极精准，按热度排序）
 * - 智能排序：自动将最匹配的歌曲排到前面
 * - 相关性过滤：自动过滤与搜索词完全无关的结果
 */
object OnlineMusicApi {

    private const val TAG = "OnlineMusicApi"

    val channels: List<MusicChannel> = listOf(
        NetEaseOfficialChannel(),   // 精准搜索（推荐）
        QiShuiChannel(),            // 汽水音乐
        NetEaseChannel(),           // 网易云音乐（oiapi 代理）
        ITunesChannel()             // iTunes（30秒预览）
    )

    private val channelMap: Map<String, MusicChannel> = channels.associateBy { it.id }
    private val channelStatusCache = mutableMapOf<String, ChannelStatus>()

    fun getChannel(channelId: String): MusicChannel? = channelMap[channelId]

    /**
     * 测试所有渠道连接
     */
    suspend fun testAllChannels(): Map<String, ChannelStatus> {
        val results = mutableMapOf<String, ChannelStatus>()
        for (channel in channels) {
            try {
                val status = channel.testConnection()
                results[channel.id] = status
                channelStatusCache[channel.id] = status
                AppLogger.i(TAG, "Channel ${channel.displayName}: ${if (status.isAvailable) "✓" else "✗"} (${status.latencyMs}ms)")
            } catch (e: Exception) {
                val status = ChannelStatus(channel.id, false, 0, e.message)
                results[channel.id] = status
                channelStatusCache[channel.id] = status
            }
        }
        return results
    }

    /**
     * 测试单个渠道
     */
    suspend fun testChannel(channelId: String): ChannelStatus {
        val channel = channelMap[channelId]
            ?: return ChannelStatus(channelId, false, 0, "渠道不存在")
        val status = channel.testConnection()
        channelStatusCache[channelId] = status
        return status
    }

    /**
     * 获取缓存的渠道状态
     */
    fun getCachedStatus(channelId: String): ChannelStatus? = channelStatusCache[channelId]

    /**
     * 搜索音乐
     */
    suspend fun search(channelId: String, query: String, page: Int = 1): Result<MusicSearchResponse> {
        val channel = channelMap[channelId]
            ?: return Result.failure(Exception("渠道不存在: $channelId"))
        return channel.search(query, page)
    }

    /**
     * 获取曲目详情（含播放链接）
     */
    suspend fun getTrackDetail(track: OnlineMusicTrack): Result<OnlineMusicTrack> {
        val channel = channelMap[track.sourceChannelId]
            ?: return Result.failure(Exception("渠道不存在: ${track.sourceChannelId}"))
        return channel.getTrackDetail(track)
    }
}
