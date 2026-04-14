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
import com.webtoapp.core.i18n.AppStringsProvider
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
 * download
 */
data class DownloadLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val type: LogType = LogType.INFO
) {
    enum class LogType { INFO, SUCCESS, ERROR, WARNING }
}

/**
 * dialog
 * support, preview, download, download
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineMusicSearchDialog(
    onDismiss: () -> Unit,
    onMusicDownloaded: (BgmItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // state
    var channelStatuses by remember { mutableStateOf<Map<String, ChannelStatus>>(emptyMap()) }
    var selectedChannelId by remember { mutableStateOf(OnlineMusicApi.channels.first().id) }
    var testingChannels by remember { mutableStateOf(false) }
    var testingChannelId by remember { mutableStateOf<String?>(null) }

    // state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<OnlineMusicTrack>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    // preview state
    var previewingTrack by remember { mutableStateOf<OnlineMusicTrack?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isLoadingPreview by remember { mutableStateOf(false) }
    var loadingPreviewTrackId by remember { mutableStateOf<String?>(null) }
    var isPlayerPrepared by remember { mutableStateOf(false) }
    var previewCurrentPosition by remember { mutableLongStateOf(0L) }
    var previewDuration by remember { mutableLongStateOf(0L) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    // downloadstate
    var downloadingTrackId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedTrackIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var downloadLogs by remember { mutableStateOf<List<DownloadLogEntry>>(emptyList()) }
    var showDownloadLog by remember { mutableStateOf(false) }
    var downloadSpeed by remember { mutableStateOf("") }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // preview update
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

    // initializecheck state
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

    // Note
    fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    // Note
    fun performSearch() {
        if (searchQuery.isBlank()) return
        scope.launch {
            isSearching = true
            searchError = null
            hasSearched = true
            // preview
            mediaPlayer?.release()
            mediaPlayer = null
            previewingTrack = null
            isPreviewPlaying = false
            isPlayerPrepared = false

            val result = OnlineMusicApi.search(selectedChannelId, searchQuery.trim())
            result.onSuccess { response ->
                searchResults = response.tracks
                if (response.tracks.isEmpty()) {
                    searchError = AppStringsProvider.current().noMusicResults
                }
                // check download
                val downloaded = mutableSetOf<String>()
                response.tracks.forEach { track ->
                    if (track.playUrl != null && OnlineMusicDownloader.isMusicDownloaded(context, track)) {
                        downloaded.add(track.id)
                    }
                }
                downloadedTrackIds = downloaded
            }.onFailure { e ->
                searchResults = emptyList()
                searchError = "${AppStringsProvider.current().searchFailed}: ${e.message}"
            }
            isSearching = false
        }
    }

    // preview
    fun previewTrack(track: OnlineMusicTrack) {
        if (previewingTrack?.id == track.id && isPreviewPlaying) {
            // Note
            mediaPlayer?.pause()
            isPreviewPlaying = false
            return
        } else if (previewingTrack?.id == track.id && !isPreviewPlaying && isPlayerPrepared) {
            // Note
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
                // Note
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
                    snackbarHostState.showSnackbar(AppStringsProvider.current().searchFailed)
                    isLoadingPreview = false
                    loadingPreviewTrackId = null
                    return@launch
                }

                AppLogger.i("OnlineMusicSearch", "Playing: $playUrl")

                // update in
                searchResults = searchResults.map {
                    if (it.id == track.id) detailedTrack else it
                }

                withContext(Dispatchers.Main) {
                    try {
                        val mp = MediaPlayer()
                        
                        // settings, call prepareAsync
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
                                snackbarHostState.showSnackbar(AppStringsProvider.current().playbackFailedWithCode.format(what))
                            }
                            true
                        }
                        
                        mp.setDataSource(playUrl)
                        mediaPlayer = mp
                        mp.prepareAsync()
                        
                        // 15 if load, resetstate
                        scope.launch {
                            delay(15000)
                            if (isLoadingPreview && loadingPreviewTrackId == track.id) {
                                AppLogger.w("OnlineMusicSearch", "Preview load timeout for: ${track.name}")
                                isLoadingPreview = false
                                loadingPreviewTrackId = null
                                snackbarHostState.showSnackbar(AppStringsProvider.current().loadingTimeout)
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("OnlineMusicSearch", "Preview failed: ${e.message}", e)
                        snackbarHostState.showSnackbar("${AppStringsProvider.current().playbackFailed}: ${e.message}")
                        isLoadingPreview = false
                        loadingPreviewTrackId = null
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("OnlineMusicSearch", "Preview failed: ${e.message}", e)
                isLoadingPreview = false
                loadingPreviewTrackId = null
                snackbarHostState.showSnackbar("${AppStringsProvider.current().loadFailed}: ${e.message}")
            }
        }
    }

    // Note
    fun addLog(message: String, type: DownloadLogEntry.LogType = DownloadLogEntry.LogType.INFO) {
        downloadLogs = downloadLogs + DownloadLogEntry(message = message, type = type)
    }

    // download
    fun downloadTrack(track: OnlineMusicTrack) {
        scope.launch {
            downloadingTrackId = track.id
            downloadProgress = 0f
            downloadLogs = emptyList()
            showDownloadLog = true
            downloadSpeed = ""

            addLog("${AppStringsProvider.current().startDownload}: ${track.name} - ${track.artist}")
            addLog("${AppStringsProvider.current().musicChannelLabel}: ${OnlineMusicApi.getChannel(track.sourceChannelId)?.displayName ?: track.sourceChannelId}")

            try {
                // Note
                addLog(AppStringsProvider.current().gettingMusicDetails)
                val detailedTrack = if (track.playUrl.isNullOrBlank()) {
                    val detailResult = OnlineMusicApi.getTrackDetail(track)
                    detailResult.getOrNull()
                } else {
                    track
                }

                if (detailedTrack?.playUrl.isNullOrBlank()) {
                    addLog(AppStringsProvider.current().getPlayUrlFailed, DownloadLogEntry.LogType.ERROR)
                    snackbarHostState.showSnackbar(AppStringsProvider.current().searchFailed)
                    downloadingTrackId = null
                    return@launch
                }

                addLog(AppStringsProvider.current().getPlayUrlSuccess, DownloadLogEntry.LogType.SUCCESS)
                addLog(AppStringsProvider.current().startDownloadMusic)

                // update
                searchResults = searchResults.map {
                    if (it.id == track.id) detailedTrack!! else it
                }

                var lastProgressTime = System.currentTimeMillis()

                val bgmItem = OnlineMusicDownloader.downloadMusic(context, detailedTrack!!) { progress ->
                    downloadProgress = progress

                    // download
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastProgressTime
                    if (elapsed > 500) {
                        lastProgressTime = now
                    }

                    // update
                    val percent = (progress * 100).toInt()
                    if (percent > 0 && percent % 20 == 0) {
                        val logMsg = when {
                            progress <= 0.8f -> "${AppStringsProvider.current().musicDownloading} ${percent}%"
                            progress <= 0.85f -> AppStringsProvider.current().downloadingCoverImage
                            progress <= 0.95f -> "${AppStringsProvider.current().coverDownloading} ${((progress - 0.8f) / 0.2f * 100).toInt()}%"
                            else -> AppStringsProvider.current().finishing
                        }
                        // Avoid duplicate logs
                        if (downloadLogs.lastOrNull()?.message != logMsg) {
                            addLog(logMsg)
                        }
                    }
                }

                if (bgmItem != null) {
                    downloadedTrackIds = downloadedTrackIds + track.id
                    addLog(AppStringsProvider.current().downloadCompleteSaved, DownloadLogEntry.LogType.SUCCESS)
                    if (bgmItem.coverPath != null) {
                        addLog(AppStringsProvider.current().coverImageSaved, DownloadLogEntry.LogType.SUCCESS)
                    }
                    onMusicDownloaded(bgmItem)
                    snackbarHostState.showSnackbar(AppStringsProvider.current().downloadSuccess)
                } else {
                    addLog(AppStringsProvider.current().downloadFailed, DownloadLogEntry.LogType.ERROR)
                    snackbarHostState.showSnackbar(AppStringsProvider.current().searchFailed)
                }
            } catch (e: Exception) {
                AppLogger.e("OnlineMusicSearch", "Download failed", e)
                addLog("${AppStringsProvider.current().downloadError}: ${e.message}", DownloadLogEntry.LogType.ERROR)
                snackbarHostState.showSnackbar("${AppStringsProvider.current().searchFailed}: ${e.message}")
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
                        title = { Text(AppStringsProvider.current().onlineMusic) },
                        navigationIcon = {
                            IconButton(onClick = {
                                mediaPlayer?.release()
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Close, AppStringsProvider.current().close)
                            }
                        },
                        actions = {
                            // download button
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
                                            AppStringsProvider.current().downloadLog,
                                            tint = if (showDownloadLog)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // all button
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
                                Text(AppStringsProvider.current().testAllChannels)
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
                    // ===== select =====
                    Text(
                        AppStringsProvider.current().musicChannel,
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

                    // Note
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
                            placeholder = { Text(AppStringsProvider.current().searchSongName) },
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

                        // button
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
                                Icon(Icons.Default.Search, AppStringsProvider.current().search)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ===== download panel =====
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

                    // Note
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
                                    AppStringsProvider.current().searchingText,
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
                                    AppStringsProvider.current().searchOnlineMusic,
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
                            // hint
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
                                            "${channel.displayName} · ${searchResults.size} ${AppStringsProvider.current().results}",
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

                    // ===== bottom =====
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
 * select
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
        isTesting -> AppStringsProvider.current().channelTesting
        status == null -> AppStringsProvider.current().channelUntested
        status.isAvailable -> "${AppStringsProvider.current().channelAvailable} ${status.latencyMs}ms"
        else -> AppStringsProvider.current().channelUnavailable
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
                                AppStringsProvider.current().recommendedLabel,
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
 * ( enhanced)
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
                // Note
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

                    // state
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

                // Note
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
                            AppStringsProvider.current().previewListen,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // button
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
                            AppStringsProvider.current().previewListen,
                            tint = when {
                                isPlaying -> MaterialTheme.colorScheme.primary
                                isPaused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // downloadbutton
                if (isDownloaded) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        AppStringsProvider.current().downloadSuccess,
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
                            AppStringsProvider.current().downloadToBgm,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // download( )
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
 * Note
 */
private fun formatTrackDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/**
 * enhanced( , seek, display)
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
            // Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Note
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

                // / button
                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (isPlaying) Icons.Filled.PauseCircleFilled
                        else Icons.Filled.PlayCircleFilled,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // button
                IconButton(onClick = onStop) {
                    Icon(
                        Icons.Filled.StopCircle,
                        AppStringsProvider.current().stop,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Note
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
 * download panel
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

    // scroll
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
            // Note
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
                    AppStringsProvider.current().downloadLog,
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
                            AppStringsProvider.current().clearText,
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
                        AppStringsProvider.current().collapseText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // download
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

            // list
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
