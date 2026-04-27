package com.webtoapp.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.repository.WebAppRepository

@Composable
internal fun PreviewRouteScreen(
    appId: Long,
    webAppRepository: WebAppRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val webApp by webAppRepository.getWebAppById(appId).collectAsState(initial = null)
    var hasLaunched by remember { mutableStateOf(false) }

    LaunchedEffect(webApp) {
        val app = webApp
        if (app != null && !hasLaunched) {
            hasLaunched = true
            PreviewStarter.start(context, appId, app)
            onBack()
        }
    }
}

internal object PreviewStarter {
    fun start(
        context: Context,
        appId: Long,
        app: WebApp,
    ) {
        when (app.appType) {
            AppType.IMAGE,
            AppType.VIDEO -> {
                com.webtoapp.ui.media.MediaAppActivity.startForPreview(context, app)
            }

            AppType.GALLERY -> {
                app.galleryConfig?.let { config ->
                    com.webtoapp.ui.gallery.GalleryPlayerActivity.launch(context, config, 0)
                }
            }

            else -> {
                com.webtoapp.ui.webview.WebViewActivity.start(context, appId)
            }
        }
    }
}
