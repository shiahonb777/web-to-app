package com.webtoapp.ui.shell

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.webview.LongPressHandler

/**
 * 长按菜单 - 根据样式显示不同的菜单组件
 *
 * @param menuStyle 菜单样式：SIMPLE, FULL, IOS, FLOATING, CONTEXT
 * @param result 长按结果
 * @param touchX 触摸 X 坐标（用于 FLOATING 和 CONTEXT 样式定位）
 * @param touchY 触摸 Y 坐标
 * @param longPressHandler 长按处理器（保存图片、复制链接等）
 * @param onDismiss 关闭菜单回调
 */
@Composable
fun ShellLongPressMenu(
    menuStyle: String,
    result: LongPressHandler.LongPressResult,
    touchX: Float,
    touchY: Float,
    longPressHandler: LongPressHandler,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    when (menuStyle) {
        "SIMPLE" -> {
            // 简洁模式：仅保存图片和复制链接
            com.webtoapp.ui.components.SimpleLongPressMenuSheet(
                result = result,
                onDismiss = onDismiss,
                onCopyLink = { url ->
                    longPressHandler.copyToClipboard(url)
                },
                onSaveImage = { url ->
                    longPressHandler.saveImage(url) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        "FULL" -> {
            // 完整模式：BottomSheet
            com.webtoapp.ui.components.LongPressMenuSheet(
                result = result,
                onDismiss = onDismiss,
                onCopyLink = { url ->
                    longPressHandler.copyToClipboard(url)
                },
                onSaveImage = { url ->
                    longPressHandler.saveImage(url) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                onDownloadVideo = { url ->
                    longPressHandler.downloadVideo(url) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenInBrowser = { url ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizeExternalUrlForIntent(url)))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        "IOS" -> {
            // iOS 风格：毛玻璃卡片
            com.webtoapp.ui.components.IosStyleLongPressMenu(
                result = result,
                onDismiss = onDismiss,
                onCopyLink = { url ->
                    longPressHandler.copyToClipboard(url)
                },
                onSaveImage = { url ->
                    longPressHandler.saveImage(url) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                onDownloadVideo = { url ->
                    longPressHandler.downloadVideo(url) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenInBrowser = { url ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizeExternalUrlForIntent(url)))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        "FLOATING" -> {
            // 悬浮气泡风格
            com.webtoapp.ui.components.FloatingBubbleLongPressMenu(
                result = result,
                touchX = touchX,
                touchY = touchY,
                onDismiss = onDismiss,
                onCopyLink = { url ->
                    longPressHandler.copyToClipboard(url)
                },
                onSaveImage = { url ->
                    longPressHandler.saveImage(url) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                onDownloadVideo = { url ->
                    longPressHandler.downloadVideo(url) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenInBrowser = { url ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizeExternalUrlForIntent(url)))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        "CONTEXT" -> {
            // 右键菜单风格
            com.webtoapp.ui.components.ContextMenuLongPressMenu(
                result = result,
                touchX = touchX,
                touchY = touchY,
                onDismiss = onDismiss,
                onCopyLink = { url ->
                    longPressHandler.copyToClipboard(url)
                },
                onSaveImage = { url ->
                    longPressHandler.saveImage(url) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                onDownloadVideo = { url ->
                    longPressHandler.downloadVideo(url) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                onOpenInBrowser = { url ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizeExternalUrlForIntent(url)))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        else -> {
            // DISABLED 或其他情况
            onDismiss()
        }
    }
}
