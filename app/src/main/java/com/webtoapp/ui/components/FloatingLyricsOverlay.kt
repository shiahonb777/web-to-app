package com.webtoapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.data.model.LrcData
import com.webtoapp.data.model.LrcLine
import com.webtoapp.data.model.LrcTheme
import kotlin.math.roundToInt

/**
 * 悬浮歌词覆盖层
 * 支持拖动、隐藏、展开/收起、暂停/播放
 */
@Composable
fun FloatingLyricsOverlay(
    lrcData: LrcData?,
    currentPosition: Long,
    isPlaying: Boolean,
    lrcTheme: LrcTheme?,
    onToggleVisibility: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (lrcData == null || lrcData.lines.isEmpty()) return
    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // 位置状态（可拖动）
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(screenHeight * 0.7f) }
    
    // 是否展开（展开显示更多歌词，收起只显示当前行）
    var isExpanded by remember { mutableStateOf(false) }
    
    // 是否最小化（只显示小图标）
    var isMinimized by remember { mutableStateOf(false) }
    
    // 当前歌词行索引
    val currentLineIndex = remember(currentPosition, lrcData) {
        lrcData.lines.indexOfLast { it.startTime <= currentPosition }
    }
    
    // 解析主题颜色（默认透明背景）
    val bgColor = remember(lrcTheme) {
        try {
            Color(android.graphics.Color.parseColor(lrcTheme?.backgroundColor ?: "#00000000"))
        } catch (e: Exception) {
            Color.Transparent
        }
    }
    val textColor = remember(lrcTheme) {
        try {
            Color(android.graphics.Color.parseColor(lrcTheme?.textColor ?: "#FFFFFF"))
        } catch (e: Exception) {
            Color.White
        }
    }
    val highlightColor = remember(lrcTheme) {
        try {
            Color(android.graphics.Color.parseColor(lrcTheme?.highlightColor ?: "#FFD700"))
        } catch (e: Exception) {
            Color(0xFFFFD700)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // 悬浮歌词窗口
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidth - 200f)
                        offsetY = (offsetY + dragAmount.y).coerceIn(0f, screenHeight - 100f)
                    }
                }
        ) {
            AnimatedContent(
                targetState = isMinimized,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "lyrics_content"
            ) { minimized ->
                if (minimized) {
                    // 最小化状态：小图标
                    MinimizedLyricsButton(
                        isPlaying = isPlaying,
                        onClick = { isMinimized = false }
                    )
                } else {
                    // 正常状态：歌词显示
                    LyricsCard(
                        lrcData = lrcData,
                        currentLineIndex = currentLineIndex,
                        isExpanded = isExpanded,
                        isPlaying = isPlaying,
                        bgColor = bgColor,
                        textColor = textColor,
                        highlightColor = highlightColor,
                        fontSize = lrcTheme?.fontSize ?: 16f,
                        onToggleExpand = { isExpanded = !isExpanded },
                        onMinimize = { isMinimized = true },
                        onHide = onToggleVisibility,
                        onTogglePlay = onTogglePlay
                    )
                }
            }
        }
    }
}

/**
 * 最小化的歌词按钮
 */
@Composable
private fun MinimizedLyricsButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .shadow(4.dp, CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = "显示歌词",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size((24 * scale).dp)
            )
        }
    }
}

/**
 * 歌词卡片（透明背景，文字带描边）
 */
@Composable
private fun LyricsCard(
    lrcData: LrcData,
    currentLineIndex: Int,
    isExpanded: Boolean,
    isPlaying: Boolean,
    bgColor: Color,
    textColor: Color,
    highlightColor: Color,
    fontSize: Float,
    onToggleExpand: () -> Unit,
    onMinimize: () -> Unit,
    onHide: () -> Unit,
    onTogglePlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 320.dp)
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // 控制按钮栏（半透明背景）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 歌曲信息
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    null,
                    tint = highlightColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = lrcData.title ?: "歌词",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 控制按钮
            Row {
                // 播放/暂停
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = highlightColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // 展开/收起
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                // 最小化
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "最小化",
                        tint = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                // 隐藏
                IconButton(
                    onClick = onHide,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "隐藏",
                        tint = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 歌词内容
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                expandVertically() + fadeIn() togetherWith shrinkVertically() + fadeOut()
            },
            label = "lyrics_expand"
        ) { expanded ->
            if (expanded) {
                // 展开模式：显示多行歌词
                ExpandedLyricsContent(
                    lines = lrcData.lines,
                    currentLineIndex = currentLineIndex,
                    textColor = textColor,
                    highlightColor = highlightColor,
                    fontSize = fontSize
                )
            } else {
                // 收起模式：只显示当前行
                CollapsedLyricsContent(
                    lines = lrcData.lines,
                    currentLineIndex = currentLineIndex,
                    highlightColor = highlightColor,
                    fontSize = fontSize
                )
            }
        }
    }
}

/**
 * 展开模式的歌词内容（带文字阴影）
 */
@Composable
private fun ExpandedLyricsContent(
    lines: List<LrcLine>,
    currentLineIndex: Int,
    textColor: Color,
    highlightColor: Color,
    fontSize: Float
) {
    // 文字阴影样式
    val textShadow = Shadow(
        color = Color.Black,
        offset = Offset(2f, 2f),
        blurRadius = 4f
    )
    
    Column(
        modifier = Modifier.heightIn(max = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 显示前后各2行
        val startIndex = (currentLineIndex - 2).coerceAtLeast(0)
        val endIndex = (currentLineIndex + 3).coerceAtMost(lines.size)
        
        for (i in startIndex until endIndex) {
            val line = lines.getOrNull(i) ?: continue
            val isCurrentLine = i == currentLineIndex
            
            Text(
                text = line.text,
                color = if (isCurrentLine) highlightColor else textColor.copy(alpha = 0.6f),
                fontSize = if (isCurrentLine) (fontSize + 2).sp else fontSize.sp,
                fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = textShadow),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
            
            // 翻译
            if (isCurrentLine && !line.translation.isNullOrBlank()) {
                Text(
                    text = line.translation,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = (fontSize - 2).sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(shadow = textShadow),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 收起模式的歌词内容（带文字阴影）
 */
@Composable
private fun CollapsedLyricsContent(
    lines: List<LrcLine>,
    currentLineIndex: Int,
    highlightColor: Color,
    fontSize: Float
) {
    val currentLine = lines.getOrNull(currentLineIndex)
    val nextLine = lines.getOrNull(currentLineIndex + 1)
    
    // 文字阴影样式
    val textShadow = Shadow(
        color = Color.Black,
        offset = Offset(2f, 2f),
        blurRadius = 4f
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 当前行
        AnimatedContent(
            targetState = currentLine,
            transitionSpec = {
                slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
            },
            label = "current_line"
        ) { line ->
            Text(
                text = line?.text ?: "♪ ♫ ♪",
                color = highlightColor,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = textShadow),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // 翻译或下一行
        if (!currentLine?.translation.isNullOrBlank()) {
            Text(
                text = currentLine?.translation ?: "",
                color = highlightColor.copy(alpha = 0.8f),
                fontSize = (fontSize - 2).sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = textShadow),
                modifier = Modifier.fillMaxWidth()
            )
        } else if (nextLine != null) {
            Text(
                text = nextLine.text,
                color = highlightColor.copy(alpha = 0.5f),
                fontSize = (fontSize - 2).sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = textShadow),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
