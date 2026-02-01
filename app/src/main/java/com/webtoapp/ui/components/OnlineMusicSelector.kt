package com.webtoapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.bgm.OnlineMusicApi
import com.webtoapp.core.bgm.OnlineMusicData
import com.webtoapp.core.bgm.OnlineMusicDownloader
import com.webtoapp.core.bgm.OnlineMusicResult
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.BgmItem
import kotlinx.coroutines.launch

/**
 * 在线音乐选择器对话框
 * 支持搜索网易云音乐并下载到本地
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnlineMusicSelectorDialog(
    onDismiss: () -> Unit,
    onMusicSelected: (BgmItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 搜索关键词
    var searchQuery by remember { mutableStateOf("") }

    // 搜索结果列表
    var searchResults by remember { mutableStateOf<List<OnlineMusicData>>(emptyList()) }

    // 加载状态
    var isLoading by remember { mutableStateOf(false) }

    // 错误信息
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 当前下载的音乐
    var downloadingMusic by remember { mutableStateOf<OnlineMusicData?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    // 搜索历史
    var searchHistory by remember { mutableStateOf<List<String>>(emptyList()) }

    // 热门推荐关键词
    val hotKeywords = remember {
        listOf(
            Strings.hotKeywordJayChou,
            Strings.hotKeywordJjLin,
            Strings.hotKeywordGem,
            Strings.hotKeywordJokerXue,
            Strings.hotKeywordInstrumental,
            Strings.hotKeywordLightMusic,
            Strings.hotKeywordPianoMusic,
            Strings.hotKeywordChineseStyle,
            Strings.hotKeywordAnime
        )
    }

    // 执行搜索
    fun performSearch(query: String) {
        if (query.isBlank()) return

        scope.launch {
            isLoading = true
            errorMessage = null
            searchResults = emptyList()

            // 添加到搜索历史
            if (!searchHistory.contains(query)) {
                searchHistory = (listOf(query) + searchHistory).take(10)
            }

            // 搜索多个结果（通过不同的 n 参数）
            val results = mutableListOf<OnlineMusicData>()
            for (n in 1..10) {
                when (val result = OnlineMusicApi.searchMusic(query, n)) {
                    is OnlineMusicResult.Success -> {
                        // 避免重复
                        if (results.none { it.id == result.data.id }) {
                            results.add(result.data)
                        }
                    }
                    is OnlineMusicResult.Error -> {
                        if (results.isEmpty() && n == 1) {
                            errorMessage = result.message
                        }
                    }
                }
            }

            searchResults = results
            isLoading = false
        }
    }

    // 下载音乐
    fun downloadMusic(musicData: OnlineMusicData) {
        scope.launch {
            downloadingMusic = musicData
            downloadProgress = 0f

            val bgmItem = OnlineMusicDownloader.downloadMusic(
                context = context,
                musicData = musicData,
                onProgress = { progress ->
                    downloadProgress = progress
                }
            )

            downloadingMusic = null

            if (bgmItem != null) {
                onMusicSelected(bgmItem)
            } else {
                errorMessage = Strings.downloadFailed
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 标题栏
                TopAppBar(
                    title = { Text(Strings.onlineMusic) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, Strings.close)
                        }
                    },
                    actions = {
                        // 随机推荐按钮
                        IconButton(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    when (val result = OnlineMusicApi.getRandomMusic()) {
                                        is OnlineMusicResult.Success -> {
                                            searchResults = listOf(result.data)
                                        }
                                        is OnlineMusicResult.Error -> {
                                            errorMessage = result.message
                                        }
                                    }
                                    isLoading = false
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Shuffle, Strings.randomRecommend)
                        }
                    }
                )

                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text(Strings.searchSongName) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, Strings.clear)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { performSearch(searchQuery) }
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 搜索按钮
                Button(
                    onClick = { performSearch(searchQuery) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = searchQuery.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isLoading) Strings.searching else Strings.search.replace("...", ""))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 内容区域
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        // 加载中
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        // 错误信息
                        errorMessage != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Outlined.ErrorOutline,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        errorMessage!!,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        // 搜索结果
                        searchResults.isNotEmpty() -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(searchResults) { music ->
                                    OnlineMusicItem(
                                        music = music,
                                        isDownloading = downloadingMusic?.id == music.id,
                                        downloadProgress = if (downloadingMusic?.id == music.id) downloadProgress else 0f,
                                        isDownloaded = OnlineMusicDownloader.isMusicDownloaded(context, music),
                                        onDownload = { downloadMusic(music) }
                                    )
                                }
                            }
                        }
                        // 空状态 - 显示热门推荐
                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                // 热门推荐
                                Text(
                                    Strings.hotSearch,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    hotKeywords.forEach { keyword ->
                                        SuggestionChip(
                                            onClick = {
                                                searchQuery = keyword
                                                performSearch(keyword)
                                            },
                                            label = { Text(keyword) }
                                        )
                                    }
                                }

                                // 搜索历史
                                if (searchHistory.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            Strings.searchHistory,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        TextButton(onClick = { searchHistory = emptyList() }) {
                                            Text(Strings.clear)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        searchHistory.forEach { keyword ->
                                            AssistChip(
                                                onClick = {
                                                    searchQuery = keyword
                                                    performSearch(keyword)
                                                },
                                                label = { Text(keyword) },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.History,
                                                        null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }

                                // 提示信息
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    Strings.musicSource,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 在线音乐列表项
 */
@Composable
private fun OnlineMusicItem(
    music: OnlineMusicData,
    isDownloading: Boolean,
    downloadProgress: Float,
    isDownloaded: Boolean,
    onDownload: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面图片
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(music.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 歌曲信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = music.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = music.singers?.joinToString("、") { it.name } ?: Strings.unknownArtist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 下载进度条
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = downloadProgress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 下载按钮
            when {
                isDownloading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                isDownloaded -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        Strings.downloaded,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                music.isPaid -> {
                    // 付费歌曲
                    AssistChip(
                        onClick = { },
                        label = { Text(Strings.paid) },
                        enabled = false
                    )
                }
                else -> {
                    FilledTonalIconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, Strings.download)
                    }
                }
            }
        }
    }
}

/**
 * FlowRow 布局（用于标签换行显示）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
