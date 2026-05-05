package com.webtoapp.ui.shell

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.webview.LongPressHandler











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

            onDismiss()
        }
    }
}
