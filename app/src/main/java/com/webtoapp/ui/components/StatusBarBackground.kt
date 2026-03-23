package com.webtoapp.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * Status bar background组件
 * 
 * 在全屏模式下显示状态栏时，渲染自定义的状态栏背景
 * 支持纯色背景和图片背景，以及透明度调整
 */
@Composable
fun StatusBarBackground(
    backgroundType: String, // "COLOR" or "IMAGE"
    backgroundColor: String?,
    backgroundImagePath: String?, // 可以是本地路径或 assets 路径
    alpha: Float,
    heightDp: Int, // 0 表示使用系统默认
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // Get系统状态栏高度
    val systemStatusBarHeight = remember {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            with(density) { context.resources.getDimensionPixelSize(resourceId).toDp() }
        } else {
            24.dp
        }
    }
    
    // 使用自定义高度或系统默认高度
    val actualHeight = if (heightDp > 0) heightDp.dp else systemStatusBarHeight
    
    // Load图片背景
    val imageBitmap = remember(backgroundImagePath) {
        if (backgroundType == "IMAGE" && !backgroundImagePath.isNullOrEmpty()) {
            try {
                // 尝试从本地文件加载
                val file = File(backgroundImagePath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(backgroundImagePath)?.asImageBitmap()
                } else {
                    // 尝试从 assets 加载
                    val assetPath = backgroundImagePath.removePrefix("assets/")
                    context.assets.open(assetPath).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("StatusBarBackground", "加载状态栏背景图片失败: $backgroundImagePath", e)
                null
            }
        } else {
            null
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(actualHeight)
            .statusBarsPadding()
    ) {
        when {
            backgroundType == "IMAGE" && imageBitmap != null -> {
                // Image背景
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "状态栏背景",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 1f - alpha)),
                    contentScale = ContentScale.Crop,
                    alpha = alpha
                )
            }
            else -> {
                // 纯色背景
                val bgColor = try {
                    val hex = backgroundColor?.removePrefix("#") ?: "000000"
                    when (hex.length) {
                        6 -> Color(android.graphics.Color.parseColor("#$hex")).copy(alpha = alpha)
                        8 -> Color(android.graphics.Color.parseColor("#$hex")).copy(alpha = alpha)
                        else -> Color.Black.copy(alpha = alpha)
                    }
                } catch (e: Exception) {
                    Color.Black.copy(alpha = alpha)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                )
            }
        }
    }
}

/**
 * Status bar background覆盖层
 * 
 * 用于在全屏模式下覆盖在内容上方，显示自定义状态栏背景
 * 这个组件应该放在 Box 的最上层
 * 
 * 注意：此组件需要放在一个使用 fillMaxSize() 的容器中
 */
@Composable
fun StatusBarOverlay(
    show: Boolean,
    backgroundType: String,
    backgroundColor: String?,
    backgroundImagePath: String?,
    alpha: Float,
    heightDp: Int,
    modifier: Modifier = Modifier
) {
    if (!show) return
    
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // Get系统状态栏高度
    val systemStatusBarHeight = remember {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            with(density) { context.resources.getDimensionPixelSize(resourceId).toDp() }
        } else {
            24.dp
        }
    }
    
    // 使用自定义高度或系统默认高度
    val actualHeight = if (heightDp > 0) heightDp.dp else systemStatusBarHeight
    
    // Load图片背景
    val imageBitmap = remember(backgroundImagePath) {
        if (backgroundType == "IMAGE" && !backgroundImagePath.isNullOrEmpty()) {
            try {
                // 尝试从本地文件加载
                val file = File(backgroundImagePath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(backgroundImagePath)?.asImageBitmap()
                } else {
                    // 尝试从 assets 加载
                    val assetPath = backgroundImagePath.removePrefix("assets/")
                    context.assets.open(assetPath).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("StatusBarOverlay", "加载状态栏背景图片失败: $backgroundImagePath", e)
                null
            }
        } else {
            null
        }
    }
    
    // Status bar背景 - 从屏幕最顶部开始，覆盖状态栏区域
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(actualHeight)
    ) {
        when {
            backgroundType == "IMAGE" && imageBitmap != null -> {
                // Image背景
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "状态栏背景",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = alpha
                )
            }
            else -> {
                // 纯色背景
                val bgColor = try {
                    val hex = backgroundColor?.removePrefix("#") ?: "000000"
                    when (hex.length) {
                        6 -> Color(android.graphics.Color.parseColor("#$hex")).copy(alpha = alpha)
                        8 -> Color(android.graphics.Color.parseColor("#$hex")).copy(alpha = alpha)
                        else -> Color.Black.copy(alpha = alpha)
                    }
                } catch (e: Exception) {
                    Color.Black.copy(alpha = alpha)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                )
            }
        }
    }
}
