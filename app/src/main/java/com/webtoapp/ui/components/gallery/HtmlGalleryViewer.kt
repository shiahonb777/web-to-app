package com.webtoapp.ui.components.gallery

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.webtoapp.data.model.GalleryConfig
import com.webtoapp.data.model.GalleryItem
import com.webtoapp.data.model.GalleryItemType
import kotlinx.coroutines.launch
import java.io.File

/**
 * HTML画廊查看器 - 支持左右滑动切换多个HTML页面
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HtmlGalleryViewer(
    config: GalleryConfig,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val items = config.items.filter { it.type == GalleryItemType.HTML }
    if (items.isEmpty()) return
    
    val pagerState = rememberPagerState(pageCount = { items.size })
    var showControls by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    // 存储每个页面的 WebView 引用
    val webViews = remember { mutableStateMapOf<Int, WebView>() }
    
    Box(modifier = modifier.fillMaxSize()) {
        // 主内容 - HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = config.enableSwipe
        ) { page ->
            HtmlPage(
                item = items[page],
                pageIndex = page,
                isCurrentPage = page == pagerState.currentPage,
                onWebViewCreated = { webViews[page] = it }
            )
        }
        
        // 顶部标题栏
        AnimatedVisibility(
            visible = showControls && config.showTitle,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 关闭按钮
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, "关闭")
                        }
                    } else {
                        Spacer(Modifier.width(48.dp))
                    }
                    
                    // 标题和页码
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = items[pagerState.currentPage].title.ifBlank { 
                                "页面 ${pagerState.currentPage + 1}" 
                            },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (items.size > 1) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${items.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 刷新按钮
                    IconButton(
                        onClick = {
                            webViews[pagerState.currentPage]?.reload()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                }
            }
        }
        
        // 底部导航栏（当有多个页面时显示）
        if (items.size > 1) {
            AnimatedVisibility(
                visible = showControls && config.showIndicator,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 上一页
                        FilledTonalIconButton(
                            onClick = {
                                scope.launch {
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    } else if (config.loop) {
                                        pagerState.animateScrollToPage(items.size - 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0 || config.loop
                        ) {
                            Icon(Icons.Default.ChevronLeft, "上一页")
                        }
                        
                        // 页面指示器
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(items.size) { index ->
                                val isSelected = index == pagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (isSelected) 10.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }
                        
                        // 下一页
                        FilledTonalIconButton(
                            onClick = {
                                scope.launch {
                                    if (pagerState.currentPage < items.size - 1) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    } else if (config.loop) {
                                        pagerState.animateScrollToPage(0)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage < items.size - 1 || config.loop
                        ) {
                            Icon(Icons.Default.ChevronRight, "下一页")
                        }
                    }
                }
            }
        }
        
        // 边缘滑动区域用于切换页面和显示控制栏
        // 左边缘
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(40.dp)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls }
                    )
                }
        )
        
        // 右边缘
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(40.dp)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls }
                    )
                }
        )
    }
}

/**
 * 单个HTML页面
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HtmlPage(
    item: GalleryItem,
    pageIndex: Int,
    isCurrentPage: Boolean,
    onWebViewCreated: (WebView) -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasLoaded by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = item.htmlConfig?.enableJavaScript != false
                        domStorageEnabled = item.htmlConfig?.enableLocalStorage != false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        allowFileAccess = true
                        allowContentAccess = true
                        @Suppress("DEPRECATION")
                        allowFileAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        allowUniversalAccessFromFileURLs = true
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            hasLoaded = true
                        }
                    }
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            isLoading = newProgress < 100
                        }
                    }
                    
                    onWebViewCreated(this)
                }
            },
            update = { webView ->
                // 只在当前页面且未加载过时加载
                if (isCurrentPage && !hasLoaded) {
                    val htmlPath = item.path
                    val url = if (htmlPath.startsWith("file://")) {
                        htmlPath
                    } else {
                        "file://$htmlPath"
                    }
                    webView.loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 加载指示器
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
