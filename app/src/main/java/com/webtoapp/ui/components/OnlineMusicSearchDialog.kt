package com.webtoapp.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.bgm.ChannelStatus
import com.webtoapp.core.bgm.MusicChannel
import com.webtoapp.core.bgm.OnlineMusicApi
import com.webtoapp.core.bgm.OnlineMusicDownloader
import com.webtoapp.core.bgm.OnlineMusicTrack
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.theme.AppColors
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.BgmItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 下载日志条目
 */
data class DownloadLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val type: LogType = LogType.INFO
) {
    enum class LogType { INFO, SUCCESS, ERROR, WARNING }
}

/**
 * 在线音乐搜索对话框
 * 支持多渠道搜索、实时预览播放、下载进度条、下载日志
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineMusicSearchDialog(
    onDismiss: () -> Unit,
    onMusicDownloaded: (BgmItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 渠道状态
    var channelStatuses by remember { mutableStateOf<Map<String, ChannelStatus>>(emptyMap()) }
    var selectedChannelId by remember { mutableStateOf(OnlineMusicApi.channels.first().id) }
    var testingChannels by remember { mutableStateOf(false) }
    var testingChannelId by remember { mutableStateOf<String?>(null) }

    // 搜索状态
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<OnlineMusicTrack>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    // 预览播放状态
    var previewingTrack by remember { mutableStateOf<OnlineMusicTrack?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isLoadingPreview by remember { mutableStateOf(false) }
    var loadingPreviewTrackId by remember { mutableStateOf<String?>(null) }
    var isPlayerPrepared by remember { mutableStateOf(false) }
    var previewCurrentPosition by remember { mutableLongStateOf(0L) }
    var previewDuration by remember { mutableLongStateOf(0L) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    // 下载状态
    var downloadingTrackId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedTrackIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var downloadLogs by remember { mutableStateOf<List<DownloadLogEntry>>(emptyList()) }
    var showDownloadLog by remember { mutableStateOf(false) }
    var downloadSpeed by remember { mutableStateOf("") }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // 预览播放进度更新
    LaunchedEffect(isPreviewPlaying) {
        while (isPreviewPlaying) {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    previewCurrentPosition = mp.currentPosition.toLong()
                }
            }
            delay(200)
        }
    }

    // 初始化时检查已缓存的渠道状态
    LaunchedEffect(Unit) {
        val cached = mutableMapOf<String, ChannelStatus>()
        OnlineMusicApi.channels.forEach { ch ->
            OnlineMusicApi.getCachedStatus(ch.id)?.let { cached[ch.id] = it }
        }
        channelStatuses = cached
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    // 格式化时间
    fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    // 搜索函数
    fun performSearch() {
        if (searchQuery.isBlank()) return
        scope.launch {
            isSearching = true
            searchError = null
            hasSearched = true
            // 停止预览
            mediaPlayer?.release()
            mediaPlayer = null
            previewingTrack = null
            isPreviewPlaying = false
            isPlayerPrepared = false

            val result = OnlineMusicApi.search(selectedChannelId, searchQuery.trim())
            result.onSuccess { response ->
                searchResults = response.tracks
                if (response.tracks.isEmpty()) {
                    searchError = Strings.noMusicResults
                }
                // 检查哪些已下载
                val downloaded = mutableSetOf<String>()
                response.tracks.forEach { track ->
                    if (track.playUrl != null && OnlineMusicDownloader.isMusicDownloaded(context, track)) {
                        downloaded.add(track.id)
                    }
                }
                downloadedTrackIds = downloaded
            }.onFailure { e ->
                searchResults = emptyList()
                searchError = "${Strings.searchFailed}: ${e.message}"
            }
            isSearching = false
        }
    }

    // 预览播放函数
    fun previewTrack(track: OnlineMusicTrack) {
        if (previewingTrack?.id == track.id && isPreviewPlaying) {
            // 暂停播放
            mediaPlayer?.pause()
            isPreviewPlaying = false
            return
        } else if (previewingTrack?.id == track.id && !isPreviewPlaying && isPlayerPrepared) {
            // 继续播放
            mediaPlayer?.start()
            isPreviewPlaying = true
            return
        }

        scope.launch {
            mediaPlayer?.release()
            mediaPlayer = null
            previewingTrack = null
            isPreviewPlaying = false
            isPlayerPrepared = false
            isLoadingPreview = true
            loadingPreviewTrackId = track.id
            previewCurrentPosition = 0L
            previewDuration = 0L

            try {
                // 获取播放链接
                val detailedTrack = if (track.playUrl.isNullOrBlank()) {
                    AppLogger.i("OnlineMusicSearch", "Getting track detail for: ${track.name}")
                    val detailResult = OnlineMusicApi.getTrackDetail(track)
                    detailResult.getOrNull()
                } else {
                    track
                }

                val playUrl = detailedTrack?.playUrl
                if (playUrl.isNullOrBlank()) {
                    AppLogger.e("OnlineMusicSearch", "No play URL for: ${track.name}")
                    snackbarHostState.showSnackbar(Strings.searchFailed)
                    isLoadingPreview = false
                    loadingPreviewTrackId = null
                    return@launch
                }

                AppLogger.i("OnlineMusicSearch", "Playing: $playUrl")

                // 更新搜索结果中的播放链接
                searchResults = searchResults.map {
                    if (it.id == track.id) detailedTrack else it
                }

                withContext(Dispatchers.Main) {
                    try {
                        val mp = MediaPlayer()
                        
                        // 必须先设置所有监听器，再调用 prepareAsync
                        mp.setOnPreparedListener { player ->
                            AppLogger.i("OnlineMusicSearch", "MediaPlayer prepared, duration: ${player.duration}ms")
                            player.start()
                            previewingTrack = detailedTrack
                            previewDuration = player.duration.toLong()
                            isPreviewPlaying = true
                            isPlayerPrepared = true
                            isLoadingPreview = false
                            loadingPreviewTrackId = null
                        }
                        
                        mp.setOnCompletionListener {
                            isPreviewPlaying = false
                            previewCurrentPosition = 0L
                        }
                        
                        mp.setOnErrorListener { _, what, extra ->
                            AppLogger.e("OnlineMusicSearch", "MediaPlayer error: what=$what, extra=$extra")
                            isPreviewPlaying = false
                            isPlayerPrepared = false
                            isLoadingPreview = false
                            loadingPreviewTrackId = null
                            previewingTrack = null
                            scope.launch {
                                snackbarHostState.showSnackbar(Strings.playbackFailedWithCode.format(what))
                            }
                            true
                        }
                        
                        mp.setDataSource(playUrl)
                        mediaPlayer = mp
                        mp.prepareAsync()
                        
                        // 超时保护：15秒后如果还在加载，重置状态
                        scope.launch {
                            delay(15000)
                            if (isLoadingPreview && loadingPreviewTrackId == track.id) {
                                AppLogger.w("OnlineMusicSearch", "Preview load timeout for: ${track.name}")
                                isLoadingPreview = false
                                loadingPreviewTrackId = null
                                snackbarHostState.showSnackbar(Strings.loadingTimeout)
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("OnlineMusicSearch", "Preview failed: ${e.message}", e)
                        snackbarHostState.showSnackbar("${Strings.playbackFailed}: ${e.message}")
                        isLoadingPreview = false
                        loadingPreviewTrackId = null
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("OnlineMusicSearch", "Preview failed: ${e.message}", e)
                isLoadingPreview = false
                loadingPreviewTrackId = null
                snackbarHostState.showSnackbar("${Strings.loadFailed}: ${e.message}")
            }
        }
    }

    // 添加日志
    fun addLog(message: String, type: DownloadLogEntry.LogType = DownloadLogEntry.LogType.INFO) {
        downloadLogs = downloadLogs + DownloadLogEntry(message = message, type = type)
    }

    // 下载函数
    fun downloadTrack(track: OnlineMusicTrack) {
        scope.launch {
            downloadingTrackId = track.id
            downloadProgress = 0f
            downloadLogs = emptyList()
            showDownloadLog = true
            downloadSpeed = ""

            addLog("${Strings.startDownload}: ${track.name} - ${track.artist}")
            addLog("${Strings.musicChannelLabel}: ${OnlineMusicApi.getChannel(track.sourceChannelId)?.displayName ?: track.sourceChannelId}")

            try {
                // 先获取详情
                addLog(Strings.gettingMusicDetails)
                val detailedTrack = if (track.playUrl.isNullOrBlank()) {
                    val detailResult = OnlineMusicApi.getTrackDetail(track)
                    detailResult.getOrNull()
                } else {
                    track
                }

                if (detailedTrack?.playUrl.isNullOrBlank()) {
                    addLog(Strings.getPlayUrlFailed, DownloadLogEntry.LogType.ERROR)
                    snackbarHostState.showSnackbar(Strings.searchFailed)
                    downloadingTrackId = null
                    return@launch
                }

                addLog(Strings.getPlayUrlSuccess, DownloadLogEntry.LogType.SUCCESS)
                addLog(Strings.startDownloadMusic)

                // 更新搜索结果
                searchResults = searchResults.map {
                    if (it.id == track.id) detailedTrack!! else it
                }

                var lastProgressTime = System.currentTimeMillis()

                val bgmItem = OnlineMusicDownloader.downloadMusic(context, detailedTrack!!) { progress ->
                    downloadProgress = progress

                    // 计算下载速度
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastProgressTime
                    if (elapsed > 500) {
                        lastProgressTime = now
                    }

                    // 更新日志
                    val percent = (progress * 100).toInt()
                    if (percent > 0 && percent % 20 == 0) {
                        val logMsg = when {
                            progress <= 0.8f -> "${Strings.musicDownloading} ${percent}%"
                            progress <= 0.85f -> Strings.downloadingCoverImage
                            progress <= 0.95f -> "${Strings.coverDownloading} ${((progress - 0.8f) / 0.2f * 100).toInt()}%"
                            else -> Strings.finishing
                        }
                        // Avoid duplicate logs
                        if (downloadLogs.lastOrNull()?.message != logMsg) {
                            addLog(logMsg)
                        }
                    }
                }

                if (bgmItem != null) {
                    downloadedTrackIds = downloadedTrackIds + track.id
                    addLog(Strings.downloadCompleteSaved, DownloadLogEntry.LogType.SUCCESS)
                    if (bgmItem.coverPath != null) {
                        addLog(Strings.coverImageSaved, DownloadLogEntry.LogType.SUCCESS)
                    }
                    onMusicDownloaded(bgmItem)
                    snackbarHostState.showSnackbar(Strings.downloadSuccess)
                } else {
                    addLog(Strings.downloadFailed, DownloadLogEntry.LogType.ERROR)
                    snackbarHostState.showSnackbar(Strings.searchFailed)
                }
            } catch (e: Exception) {
                AppLogger.e("OnlineMusicSearch", "Download failed", e)
                addLog("${Strings.downloadError}: ${e.message}", DownloadLogEntry.LogType.ERROR)
                snackbarHostState.showSnackbar("${Strings.searchFailed}: ${e.message}")
            }
            downloadingTrackId = null
        }
    }

    Dialog(
        onDismissRequest = {
            mediaPlayer?.release()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                modifier = Modifier.systemBarsPadding().padding(bottom = 64.dp),
                topBar = {
                    TopAppBar(
                        title = { Text(Strings.onlineMusic) },
                        navigationIcon = {
                            IconButton(onClick = {
                                mediaPlayer?.release()
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Close, Strings.close)
                            }
                        },
                        actions = {
                            // 下载日志按钮
                            if (downloadLogs.isNotEmpty()) {
                                IconButton(onClick = { showDownloadLog = !showDownloadLog }) {
                                    BadgedBox(
                                        badge = {
                                            if (downloadingTrackId != null) {
                                                Badge { Text("") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Outlined.Terminal,
                                            Strings.downloadLog,
                                            tint = if (showDownloadLog)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // 测试全部渠道按钮
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        testingChannels = true
                                        channelStatuses = OnlineMusicApi.testAllChannels()
                                        testingChannels = false
                                    }
                                },
                                enabled = !testingChannels
                            ) {
                                if (testingChannels) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(Strings.testAllChannels)
                            }
                        }
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // ===== 渠道选择器 =====
                    Text(
                        Strings.musicChannel,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(OnlineMusicApi.channels) { channel ->
                            val status = channelStatuses[channel.id]
                            val isTesting = testingChannelId == channel.id || testingChannels
                            ChannelChip(
                                channel = channel,
                                status = status,
                                isSelected = selectedChannelId == channel.id,
                                isTesting = isTesting,
                                onClick = { selectedChannelId = channel.id },
                                onTest = {
                                    scope.launch {
                                        testingChannelId = channel.id
                                        val result = OnlineMusicApi.testChannel(channel.id)
                                        channelStatuses = channelStatuses + (channel.id to result)
                                        testingChannelId = null
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ===== 搜索栏 =====
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(Strings.searchSongName) },
                            modifier = Modifier.weight(weight = 1f, fill = true),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, null)
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // 独立搜索按钮
                        FilledIconButton(
                            onClick = { performSearch() },
                            enabled = searchQuery.isNotBlank() && !isSearching,
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Search, Strings.search)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ===== 下载日志面板 =====
                    AnimatedVisibility(
                        visible = showDownloadLog && downloadLogs.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        DownloadLogPanel(
                            logs = downloadLogs,
                            isDownloading = downloadingTrackId != null,
                            progress = downloadProgress,
                            onClose = { showDownloadLog = false },
                            onClear = { downloadLogs = emptyList() }
                        )
                    }

                    // ===== 搜索结果 =====
                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(weight = 1f, fill = true),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    Strings.searchingText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (searchError != null && searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(weight = 1f, fill = true),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.SearchOff,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    searchError ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (!hasSearched) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(weight = 1f, fill = true),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.MusicNote,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    Strings.searchOnlineMusic,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(weight = 1f, fill = true),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // 渠道来源提示
                            item {
                                val channel = OnlineMusicApi.getChannel(selectedChannelId)
                                if (channel != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.MusicNote,
                                            null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "${channel.displayName} · ${searchResults.size} ${Strings.results}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            itemsIndexed(searchResults) { _, track ->
                                OnlineMusicTrackItem(
                                    track = track,
                                    isPlaying = previewingTrack?.id == track.id && isPreviewPlaying,
                                    isPaused = previewingTrack?.id == track.id && !isPreviewPlaying && isPlayerPrepared,
                                    isLoadingPreview = isLoadingPreview && loadingPreviewTrackId == track.id,
                                    isDownloading = downloadingTrackId == track.id,
                                    downloadProgress = if (downloadingTrackId == track.id) downloadProgress else 0f,
                                    isDownloaded = downloadedTrackIds.contains(track.id),
                                    isItunes = selectedChannelId == "itunes",
                                    onPlay = { previewTrack(track) },
                                    onDownload = { downloadTrack(track) }
                                )
                            }
                        }
                    }

                    // ===== 底部迷你播放器 =====
                    AnimatedVisibility(
                        visible = previewingTrack != null && isPlayerPrepared,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        previewingTrack?.let { track ->
                            EnhancedMiniPlayer(
                                track = track,
                                isPlaying = isPreviewPlaying,
                                currentPosition = previewCurrentPosition,
                                duration = previewDuration,
                                formatDuration = ::formatDuration,
                                onPlayPause = {
                                    if (isPreviewPlaying) {
                                        mediaPlayer?.pause()
                                        isPreviewPlaying = false
                                    } else {
                                        mediaPlayer?.start()
                                        isPreviewPlaying = true
                                    }
                                },
                                onSeek = { progress ->
                                    val newPos = (progress * previewDuration).toLong()
                                    mediaPlayer?.seekTo(newPos.toInt())
                                    previewCurrentPosition = newPos
                                },
                                onStop = {
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                    previewingTrack = null
                                    isPreviewPlaying = false
                                    isPlayerPrepared = false
                                    previewCurrentPosition = 0L
                                    previewDuration = 0L
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 渠道选择芯片
 */
@Composable
private fun ChannelChip(
    channel: MusicChannel,
    status: ChannelStatus?,
    isSelected: Boolean,
    isTesting: Boolean,
    onClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onTest: () -> Unit
) {
    val statusColor = when {
        isTesting -> MaterialTheme.colorScheme.tertiary
        status == null -> MaterialTheme.colorScheme.outline
        status.isAvailable -> AppColors.Success
        else -> MaterialTheme.colorScheme.error
    }

    val statusText = when {
        isTesting -> Strings.channelTesting
        status == null -> Strings.channelUntested
        status.isAvailable -> "${Strings.channelAvailable} ${status.latencyMs}ms"
        else -> Strings.channelUnavailable
    }

    val isRecommended = channel.id == "netease_official"

    PremiumFilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(channel.displayName, fontSize = 13.sp)
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Text(
                                Strings.recommendedLabel,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    statusText,
                    fontSize = 10.sp,
                    color = statusColor
                )
            }
        },
        leadingIcon = if (isTesting) {
            {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp
                )
            }
        } else {
            {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
        }
    )
}

/**
 * 搜索结果项（增强版）
 */
@Composable
private fun OnlineMusicTrackItem(
    track: OnlineMusicTrack,
    isPlaying: Boolean,
    isPaused: Boolean,
    isLoadingPreview: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    isDownloaded: Boolean,
    isItunes: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPlaying -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            isPaused -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "trackBg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 封面（可点击播放）
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    if (track.coverUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(track.coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Outlined.MusicNote,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 播放状态覆盖
                    if (isPlaying || isPaused || isLoadingPreview) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingPreview) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 歌名和歌手
                Column(
                    modifier = Modifier.weight(weight = 1f, fill = true)
                ) {
                    Text(
                        track.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isPlaying || isPaused) FontWeight.Bold else FontWeight.Medium,
                        color = if (isPlaying || isPaused)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (track.album.isNotBlank()) "${track.artist} · ${track.album}" else track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(weight = 1f, fill = false)
                        )
                        if (track.duration > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                formatTrackDuration(track.duration),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                    if (isItunes) {
                        Text(
                            Strings.previewListen,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 播放按钮
                IconButton(
                    onClick = onPlay,
                    enabled = !isLoadingPreview
                ) {
                    if (isLoadingPreview) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            when {
                                isPlaying -> Icons.Filled.PauseCircle
                                isPaused -> Icons.Filled.PlayCircle
                                else -> Icons.Filled.PlayCircle
                            },
                            Strings.previewListen,
                            tint = when {
                                isPlaying -> MaterialTheme.colorScheme.primary
                                isPaused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // 下载按钮
                if (isDownloaded) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        Strings.downloadSuccess,
                        tint = AppColors.Success,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 4.dp)
                    )
                } else if (isDownloading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(36.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            "${(downloadProgress * 100).toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(onClick = onDownload) {
                        Icon(
                            Icons.Outlined.Download,
                            Strings.downloadToBgm,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 下载进度条（线性）
            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

/**
 * 格式化曲目时长
 */
private fun formatTrackDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/**
 * 增强版迷你播放器（含进度条、seek、时间显示）
 */
@Composable
private fun EnhancedMiniPlayer(
    track: OnlineMusicTrack,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    formatDuration: (Long) -> String,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onStop: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            // 歌曲信息行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 封面
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.coverUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(track.coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Outlined.MusicNote, null, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        track.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 播放/暂停按钮
                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (isPlaying) Icons.Filled.PauseCircleFilled
                        else Icons.Filled.PlayCircleFilled,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // 停止按钮
                IconButton(onClick = onStop) {
                    Icon(
                        Icons.Filled.StopCircle,
                        Strings.stop,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 进度条 + 时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatDuration(currentPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )

                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = onSeek,
                    modifier = Modifier
                        .weight(weight = 1f, fill = true)
                        .height(24.dp)
                        .padding(horizontal = 4.dp)
                )

                Text(
                    formatDuration(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * 下载日志面板
 */
@Composable
private fun DownloadLogPanel(
    logs: List<DownloadLogEntry>,
    isDownloading: Boolean,
    progress: Float,
    onClose: () -> Unit,
    onClear: () -> Unit
) {
    val logListState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    // 自动滚动到最新日志
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logListState.animateScrollToItem(logs.size - 1)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(max = 200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Terminal,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    Strings.downloadLog,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(weight = 1f, fill = true))

                if (isDownloading) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (!isDownloading && logs.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            Strings.clearText,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ExpandLess,
                        Strings.collapseText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 下载进度条
            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .padding(horizontal = 12.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 日志列表
            LazyColumn(
                state = logListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = false)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(logs.size) { index ->
                    val log = logs[index]
                    val logColor = when (log.type) {
                        DownloadLogEntry.LogType.SUCCESS -> AppColors.Success
                        DownloadLogEntry.LogType.ERROR -> MaterialTheme.colorScheme.error
                        DownloadLogEntry.LogType.WARNING -> MaterialTheme.colorScheme.tertiary
                        DownloadLogEntry.LogType.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    val logIcon = when (log.type) {
                        DownloadLogEntry.LogType.SUCCESS -> "✓"
                        DownloadLogEntry.LogType.ERROR -> "✗"
                        DownloadLogEntry.LogType.WARNING -> "⚠"
                        DownloadLogEntry.LogType.INFO -> "›"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            timeFormat.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 1.dp)
                        )
                        Text(
                            " $logIcon ",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = logColor
                        )
                        Text(
                            log.message,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = logColor,
                            modifier = Modifier.weight(weight = 1f, fill = true)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}
