package com.webtoapp.ui.webview

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.webview.LongPressHandler
import com.webtoapp.data.model.LongPressMenuStyle
import com.webtoapp.ui.components.LongPressMenuSheet

/**
 * WebView long- press- select display
 */
@Composable
fun WebViewLongPressMenu(
    menuStyle: LongPressMenuStyle,
    result: LongPressHandler.LongPressResult,
    touchX: Float,
    touchY: Float,
    longPressHandler: LongPressHandler,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val onCopyLink: (String) -> Unit = { url -> longPressHandler.copyToClipboard(url) }
    val onSaveImage: (String) -> Unit = { url ->
        longPressHandler.saveImage(url) { _, message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    val onDownloadVideo: (String) -> Unit = { url ->
        longPressHandler.downloadVideo(url) { _, message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    val onOpenInBrowser: (String) -> Unit = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizeExternalUrlForIntent(url)))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, AppStringsProvider.current().cannotOpenLink, Toast.LENGTH_SHORT).show()
        }
    }

    when (menuStyle) {
        LongPressMenuStyle.SIMPLE -> {
            com.webtoapp.ui.components.SimpleLongPressMenuSheet(
                result = result,
                onDismiss = onDismiss,
                onCopyLink = onCopyLink,
                onSaveImage = onSaveImage
            )
        }
        LongPressMenuStyle.FULL -> {
            LongPressMenuSheet(
                result = result,
                onDismiss = onDismiss,
                onCopyLink = onCopyLink,
                onSaveImage = onSaveImage,
                onDownloadVideo = onDownloadVideo,
                onOpenInBrowser = onOpenInBrowser
            )
        }
        LongPressMenuStyle.IOS -> {
            com.webtoapp.ui.components.IosStyleLongPressMenu(
                result = result,
                onDismiss = onDismiss,
                onCopyLink = onCopyLink,
                onSaveImage = onSaveImage,
                onDownloadVideo = onDownloadVideo,
                onOpenInBrowser = onOpenInBrowser
            )
        }
        LongPressMenuStyle.FLOATING -> {
            com.webtoapp.ui.components.FloatingBubbleLongPressMenu(
                result = result,
                touchX = touchX,
                touchY = touchY,
                onDismiss = onDismiss,
                onCopyLink = onCopyLink,
                onSaveImage = onSaveImage,
                onDownloadVideo = onDownloadVideo,
                onOpenInBrowser = onOpenInBrowser
            )
        }
        LongPressMenuStyle.CONTEXT -> {
            com.webtoapp.ui.components.ContextMenuLongPressMenu(
                result = result,
                touchX = touchX,
                touchY = touchY,
                onDismiss = onDismiss,
                onCopyLink = onCopyLink,
                onSaveImage = onSaveImage,
                onDownloadVideo = onDownloadVideo,
                onOpenInBrowser = onOpenInBrowser
            )
        }
        LongPressMenuStyle.DISABLED -> {
            // Should not reach here, but dismiss just in case
            onDismiss()
        }
    }
}
