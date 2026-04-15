package com.webtoapp.ui.shell

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.webview.LongPressHandler

/**
 * long- press- display
 *
 * @param menuStyle: SIMPLE, FULL, IOS, FLOATING, CONTEXT
 * @param result long- press
 * @param touchX X( for FLOATING CONTEXT)
 * @param touchY Y
 * @param longPressHandler long- presshandle( save, )
 * @param onDismiss close
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
            // mode: onlysave
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
            // mode: BottomSheet
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
                        Toast.makeText(context, AppStringsProvider.current().cannotOpenLink, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        "IOS" -> {
            // iOS: card
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
                        Toast.makeText(context, AppStringsProvider.current().cannotOpenLink, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        "FLOATING" -> {
            // Note
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
                        Toast.makeText(context, AppStringsProvider.current().cannotOpenLink, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        "CONTEXT" -> {
            // Note
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
                        Toast.makeText(context, AppStringsProvider.current().cannotOpenLink, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        else -> {
            // DISABLED or
            onDismiss()
        }
    }
}
