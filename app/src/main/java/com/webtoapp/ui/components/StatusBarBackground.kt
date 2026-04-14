package com.webtoapp.ui.components

import android.graphics.BitmapFactory
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.logging.AppLogger
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
 * Status bar background
 * 
 * mode displaystatus bar, status bar
 * support, and
 */
@Composable
fun StatusBarBackground(
    backgroundType: String, // "COLOR" or "IMAGE"
    backgroundColor: String?,
    backgroundImagePath: String?, // localpathor assets path
    alpha: Float,
    heightDp: Int, // 0 systemdefault
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    val topInsetPx = WindowInsets.statusBars.getTop(density)
    val systemStatusBarHeight = if (topInsetPx > 0) {
        with(density) { topInsetPx.toDp() }
    } else {
        24.dp
    }
    
    // orsystemdefault
    val actualHeight = if (heightDp > 0) heightDp.dp else systemStatusBarHeight
    
    // Load
    val imageBitmap = remember(backgroundImagePath) {
        if (backgroundType == "IMAGE" && !backgroundImagePath.isNullOrEmpty()) {
            try {
                // fromlocalfileload
                val file = File(backgroundImagePath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(backgroundImagePath)?.asImageBitmap()
                } else {
                    // from assets load
                    val assetPath = backgroundImagePath.removePrefix("assets/")
                    context.assets.open(assetPath).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("StatusBarBackground", "加载状态栏背景图片失败: $backgroundImagePath", e)
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
                // Image
                Image(
                    bitmap = imageBitmap,
                    contentDescription = AppStringsProvider.current().statusBarBackground,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 1f - alpha)),
                    contentScale = ContentScale.Crop,
                    alpha = alpha
                )
            }
            else -> {
                // Note
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
 * Status bar background
 * 
 * for mode content, display status bar
 * Box
 * 
 * fillMaxSize( )
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
    
    val topInsetPx = WindowInsets.statusBars.getTop(density)
    val systemStatusBarHeight = if (topInsetPx > 0) {
        with(density) { topInsetPx.toDp() }
    } else {
        24.dp
    }
    
    // orsystemdefault
    val actualHeight = if (heightDp > 0) heightDp.dp else systemStatusBarHeight
    
    // Load
    val imageBitmap = remember(backgroundImagePath) {
        if (backgroundType == "IMAGE" && !backgroundImagePath.isNullOrEmpty()) {
            try {
                // fromlocalfileload
                val file = File(backgroundImagePath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(backgroundImagePath)?.asImageBitmap()
                } else {
                    // from assets load
                    val assetPath = backgroundImagePath.removePrefix("assets/")
                    context.assets.open(assetPath).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("StatusBarOverlay", "加载状态栏背景图片失败: $backgroundImagePath", e)
                null
            }
        } else {
            null
        }
    }
    
    // Status bar- from top, status bararea
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(actualHeight)
    ) {
        when {
            backgroundType == "IMAGE" && imageBitmap != null -> {
                // Image
                Image(
                    bitmap = imageBitmap,
                    contentDescription = AppStringsProvider.current().statusBarBackground,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = alpha
                )
            }
            else -> {
                // Note
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
