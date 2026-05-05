package com.webtoapp.ui.splash

import android.content.Intent
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.core.logging.AppLogger
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.webtoapp.ui.theme.WebToAppTheme
import com.webtoapp.data.model.AnnouncementTemplateType
import com.webtoapp.ui.components.announcement.toUiTemplate
import com.webtoapp.util.normalizeExternalIntentUrl
import kotlinx.coroutines.delay
import java.io.File





class SplashLauncherActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
        const val EXTRA_SPLASH_TYPE = "splash_type"
        const val EXTRA_SPLASH_PATH = "splash_path"
        const val EXTRA_SPLASH_DURATION = "splash_duration"
        const val EXTRA_SPLASH_CLICK_SKIP = "splash_click_skip"
        const val EXTRA_VIDEO_START_MS = "video_start_ms"
        const val EXTRA_VIDEO_END_MS = "video_end_ms"
        const val EXTRA_SPLASH_LANDSCAPE = "splash_landscape"
        const val EXTRA_SPLASH_FILL_SCREEN = "splash_fill_screen"
        const val EXTRA_SPLASH_ENABLE_AUDIO = "splash_enable_audio"

        const val EXTRA_ACTIVATION_ENABLED = "activation_enabled"
        const val EXTRA_ACTIVATION_CODES = "activation_codes"
        const val EXTRA_ACTIVATION_REQUIRE_EVERY_TIME = "activation_require_every_time"

        const val EXTRA_ANNOUNCEMENT_ENABLED = "announcement_enabled"
        const val EXTRA_ANNOUNCEMENT_TITLE = "announcement_title"
        const val EXTRA_ANNOUNCEMENT_CONTENT = "announcement_content"
        const val EXTRA_ANNOUNCEMENT_LINK = "announcement_link"
        const val EXTRA_ANNOUNCEMENT_TEMPLATE = "announcement_template"
        const val EXTRA_ANNOUNCEMENT_SHOW_EMOJI = "announcement_show_emoji"
        const val EXTRA_ANNOUNCEMENT_ANIMATION = "announcement_animation"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        val splashType = intent.getStringExtra(EXTRA_SPLASH_TYPE) ?: "IMAGE"
        val splashPath = intent.getStringExtra(EXTRA_SPLASH_PATH)
        val splashDuration = intent.getIntExtra(EXTRA_SPLASH_DURATION, 3)
        val clickToSkip = intent.getBooleanExtra(EXTRA_SPLASH_CLICK_SKIP, true)
        val videoStartMs = intent.getLongExtra(EXTRA_VIDEO_START_MS, 0L)
        val videoEndMs = intent.getLongExtra(EXTRA_VIDEO_END_MS, 5000L)
        val isLandscape = intent.getBooleanExtra(EXTRA_SPLASH_LANDSCAPE, false)
        val fillScreen = intent.getBooleanExtra(EXTRA_SPLASH_FILL_SCREEN, true)
        val enableAudio = intent.getBooleanExtra(EXTRA_SPLASH_ENABLE_AUDIO, false)

        val activationEnabled = intent.getBooleanExtra(EXTRA_ACTIVATION_ENABLED, false)
        val activationRequireEveryTime = intent.getBooleanExtra(EXTRA_ACTIVATION_REQUIRE_EVERY_TIME, false)
        val activationCodesStr = intent.getStringExtra(EXTRA_ACTIVATION_CODES) ?: ""
        val activationCodes = if (activationCodesStr.isNotBlank()) {
            activationCodesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        val announcementEnabled = intent.getBooleanExtra(EXTRA_ANNOUNCEMENT_ENABLED, false)
        val announcementTitle = intent.getStringExtra(EXTRA_ANNOUNCEMENT_TITLE) ?: ""
        val announcementContent = intent.getStringExtra(EXTRA_ANNOUNCEMENT_CONTENT) ?: ""
        val announcementLink = intent.getStringExtra(EXTRA_ANNOUNCEMENT_LINK)
        val announcementTemplate = intent.getStringExtra(EXTRA_ANNOUNCEMENT_TEMPLATE) ?: "XIAOHONGSHU"
        val announcementShowEmoji = intent.getBooleanExtra(EXTRA_ANNOUNCEMENT_SHOW_EMOJI, true)
        val announcementAnimation = intent.getBooleanExtra(EXTRA_ANNOUNCEMENT_ANIMATION, true)


        if (targetPackage.isNullOrBlank()) {
            finish()
            return
        }


        val hasValidSplash = splashPath != null && File(splashPath).exists()


        if (hasValidSplash && isLandscape) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        setContent {
            WebToAppTheme { _ ->
                SplashLauncherScreen(
                    targetPackage = targetPackage,
                    hasValidSplash = hasValidSplash,
                    splashType = splashType,
                    splashPath = splashPath,
                    splashDuration = splashDuration,
                    clickToSkip = clickToSkip,
                    videoStartMs = videoStartMs,
                    videoEndMs = videoEndMs,
                    fillScreen = fillScreen,
                    enableAudio = enableAudio,
                    activationEnabled = activationEnabled,
                    activationRequireEveryTime = activationRequireEveryTime,
                    activationCodes = activationCodes,
                    announcementEnabled = announcementEnabled,
                    announcementTitle = announcementTitle,
                    announcementContent = announcementContent,
                    announcementLink = announcementLink,
                    announcementTemplate = announcementTemplate,
                    announcementShowEmoji = announcementShowEmoji,
                    announcementAnimation = announcementAnimation,
                    onLaunchTarget = { launchTargetApp(targetPackage) }
                )
            }
        }
    }

    private fun launchTargetApp(packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        } catch (e: Exception) {
            AppLogger.e("SplashLauncherActivity", "Operation failed", e)
        }


        finishAndRemoveTask()
    }
}




@Composable
fun SplashLauncherScreen(
    targetPackage: String,
    hasValidSplash: Boolean,
    splashType: String,
    splashPath: String?,
    splashDuration: Int,
    clickToSkip: Boolean,
    videoStartMs: Long,
    videoEndMs: Long,
    fillScreen: Boolean,
    enableAudio: Boolean = false,
    activationEnabled: Boolean = false,
    activationRequireEveryTime: Boolean = false,
    activationCodes: List<String> = emptyList(),
    announcementEnabled: Boolean = false,
    announcementTitle: String = "",
    announcementContent: String = "",
    announcementLink: String? = null,
    announcementTemplate: String = "XIAOHONGSHU",
    announcementShowEmoji: Boolean = true,
    announcementAnimation: Boolean = true,
    onLaunchTarget: () -> Unit
) {
    val context = LocalContext.current
    val activation = com.webtoapp.WebToAppApplication.activation
    val scope = rememberCoroutineScope()


    var isActivated by remember { mutableStateOf(!activationEnabled) }
    var showActivationDialog by remember { mutableStateOf(activationEnabled) }


    LaunchedEffect(activationRequireEveryTime) {
        if (activationEnabled && activationRequireEveryTime) {

            activation.resetActivation(-2L)
            isActivated = false
            showActivationDialog = true
        }
    }


    var showAnnouncementDialog by remember { mutableStateOf(false) }


    var showSplash by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(
        if (splashType == "VIDEO") ((videoEndMs - videoStartMs) / 1000).toInt() else splashDuration
    ) }


    LaunchedEffect(isActivated) {
        if (isActivated) {

            if (announcementEnabled && announcementTitle.isNotEmpty()) {
                showAnnouncementDialog = true
            } else {

                if (hasValidSplash && splashPath != null) {
                    showSplash = true
                } else {
                    onLaunchTarget()
                }
            }
        }
    }


    fun onAnnouncementDismiss() {
        showAnnouncementDialog = false
        if (hasValidSplash && splashPath != null) {
            showSplash = true
        } else {
            onLaunchTarget()
        }
    }


    LaunchedEffect(Unit) {
        if (!activationEnabled && !announcementEnabled && !hasValidSplash) {
            onLaunchTarget()
        }
    }


    if (showSplash && splashType == "IMAGE") {
        LaunchedEffect(countdown) {
            if (countdown > 0) {
                delay(1000L)
                countdown--
            } else {
                showSplash = false
                onLaunchTarget()
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        if (!isActivated) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = com.webtoapp.core.i18n.Strings.appNeedsActivation,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                PremiumButton(onClick = { showActivationDialog = true }) {
                    Text(com.webtoapp.core.i18n.Strings.enterActivationCode)
                }
            }
        }


        AnimatedVisibility(
            visible = showSplash && splashPath != null,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            SplashContent(
                splashType = splashType,
                splashPath = splashPath!!,
                countdown = countdown,
                clickToSkip = clickToSkip,
                videoStartMs = videoStartMs,
                videoEndMs = videoEndMs,
                fillScreen = fillScreen,
                enableAudio = enableAudio,
                onSkip = {
                    showSplash = false
                    onLaunchTarget()
                }
            )
        }


        if (!showActivationDialog && !showAnnouncementDialog && !showSplash && isActivated) {
            CircularProgressIndicator(color = Color.White)
        }
    }


    if (showActivationDialog) {
        ActivationDialog(
            onDismiss = { showActivationDialog = false },
            onActivate = { code ->
                if (activationCodes.contains(code)) {
                    isActivated = true
                    showActivationDialog = false
                }
            }
        )
    }


    if (showAnnouncementDialog) {
        AnnouncementDialog(
            title = announcementTitle,
            content = announcementContent,
            linkUrl = announcementLink,
            templateName = announcementTemplate,
            showEmoji = announcementShowEmoji,
            animationEnabled = announcementAnimation,
            onDismiss = { onAnnouncementDismiss() },
            onLinkClick = { url ->
                val safeUrl = normalizeExternalUrlForIntent(url)
                if (safeUrl.isNotEmpty()) {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(safeUrl)
                    )
                    context.startActivity(intent)
                } else {
                    AppLogger.w("SplashLauncherActivity", "Blocked invalid announcement link: $url")
                }
            }
        )
    }
}

private fun normalizeExternalUrlForIntent(rawUrl: String): String {
    return normalizeExternalIntentUrl(rawUrl)
}




@Composable
fun ActivationDialog(
    onDismiss: () -> Unit,
    onActivate: (String) -> Unit,
    customTitle: String = "",
    customSubtitle: String = "",
    customInputLabel: String = "",
    customButtonText: String = ""
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(customTitle.ifBlank { com.webtoapp.core.i18n.Strings.activateApp }) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(customSubtitle.ifBlank { com.webtoapp.core.i18n.Strings.enterCodeToContinue })
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        error = null
                    },
                    label = { Text(customInputLabel.ifBlank { com.webtoapp.core.i18n.Strings.activationCode }) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    if (code.isBlank()) {
                        error = com.webtoapp.core.i18n.Strings.pleaseEnterActivationCode
                    } else {
                        onActivate(code)
                    }
                }
            ) {
                Text(customButtonText.ifBlank { com.webtoapp.core.i18n.Strings.activate })
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(com.webtoapp.core.i18n.Strings.btnCancel)
            }
        }
    )
}





@Composable
fun AnnouncementDialog(
    title: String,
    content: String,
    linkUrl: String?,
    templateName: String = "XIAOHONGSHU",
    showEmoji: Boolean = true,
    animationEnabled: Boolean = true,
    onDismiss: () -> Unit,
    onLinkClick: (String) -> Unit
) {

        val template = try {
        AnnouncementTemplateType.valueOf(templateName).toUiTemplate()
    } catch (e: Exception) {
        com.webtoapp.ui.components.announcement.AnnouncementTemplate.MINIMAL
    }

    val announcement = com.webtoapp.data.model.Announcement(
        title = title,
        content = content,
        linkUrl = linkUrl,
        showEmoji = showEmoji,
        animationEnabled = animationEnabled
    )

    com.webtoapp.ui.components.announcement.AnnouncementDialog(
        config = com.webtoapp.ui.components.announcement.AnnouncementConfig(
            announcement = announcement,
            template = template,
            showEmoji = showEmoji,
            animationEnabled = animationEnabled
        ),
        onDismiss = onDismiss,
        onLinkClick = { url -> onLinkClick(url) }
    )
}




@Composable
fun SplashContent(
    splashType: String,
    splashPath: String,
    countdown: Int,
    clickToSkip: Boolean,
    videoStartMs: Long,
    videoEndMs: Long,
    fillScreen: Boolean,
    enableAudio: Boolean = false,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val videoDurationMs = videoEndMs - videoStartMs
    val contentScaleMode = if (fillScreen) ContentScale.Crop else ContentScale.Fit


    var videoRemainingMs by remember { mutableLongStateOf(videoDurationMs) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(
                if (clickToSkip) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSkip() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (splashType) {
            "IMAGE" -> {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(File(splashPath))
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = com.webtoapp.core.i18n.Strings.splashScreen,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScaleMode
                )
            }
            "VIDEO" -> {
                var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                var isPlayerReady by remember { mutableStateOf(false) }



                LaunchedEffect(isPlayerReady) {
                    if (!isPlayerReady) return@LaunchedEffect
                    mediaPlayer?.let { mp ->

                        while (!mp.isPlaying) {
                            delay(50)
                            if (mediaPlayer == null) return@LaunchedEffect
                        }

                        while (mp.isPlaying) {
                            val currentPos = mp.currentPosition

                            videoRemainingMs = (videoEndMs - currentPos).coerceAtLeast(0L)
                            if (currentPos >= videoEndMs) {
                                mp.pause()
                                onSkip()
                                break
                            }
                            delay(100)
                        }
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        android.view.SurfaceView(ctx).apply {
                            holder.addCallback(object : android.view.SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                    try {
                                        mediaPlayer = android.media.MediaPlayer().apply {
                                            setDataSource(splashPath)
                                            setSurface(holder.surface)

                                            val volume = if (enableAudio) 1f else 0f
                                            setVolume(volume, volume)
                                            isLooping = false
                                            setOnPreparedListener {
                                                seekTo(videoStartMs.toInt())
                                                start()
                                                isPlayerReady = true
                                            }
                                            setOnCompletionListener { onSkip() }
                                            prepareAsync()
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("SplashLauncherActivity", "Operation failed", e)
                                        onSkip()
                                    }
                                }
                                override fun surfaceChanged(h: android.view.SurfaceHolder, f: Int, w: Int, ht: Int) {}
                                override fun surfaceDestroyed(h: android.view.SurfaceHolder) {
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                DisposableEffect(Unit) {
                    onDispose {
                        mediaPlayer?.release()
                        mediaPlayer = null
                    }
                }
            }
        }


        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shape = MaterialTheme.shapes.small,
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                val displayTime = if (splashType == "VIDEO") ((videoRemainingMs + 999) / 1000).toInt() else countdown
                if (displayTime > 0) {
                    Text(
                        text = "${displayTime}s",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (clickToSkip) {
                    if (displayTime > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("|", color = Color.White.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = com.webtoapp.core.i18n.Strings.skip,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
