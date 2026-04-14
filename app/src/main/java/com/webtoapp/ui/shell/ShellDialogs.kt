package com.webtoapp.ui.shell

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.webtoapp.core.activation.ActivationResult
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.shell.ShellRuntimeServices
import com.webtoapp.core.forcedrun.ForcedRunManager
import com.webtoapp.core.forcedrun.ForcedRunPermissionDialog
import com.webtoapp.data.model.Announcement
import com.webtoapp.ui.splash.ActivationDialog
import kotlinx.coroutines.launch

/**
 * Shell modeactivation codedialog
 */
@Composable
fun ShellActivationDialog(
    config: ShellConfig,
    onDismiss: () -> Unit,
    onActivated: () -> Unit
) {
    val context = LocalContext.current
    val activation = ShellRuntimeServices.activation

    ActivationDialog(
        onDismiss = onDismiss,
        customTitle = config.activationDialogTitle,
        customSubtitle = config.activationDialogSubtitle,
        customInputLabel = config.activationDialogInputLabel,
        customButtonText = config.activationDialogButtonText,
        onActivate = { code ->
            val scope = (context as? AppCompatActivity)?.lifecycleScope
            scope?.launch {
                val result = activation.verifyActivationCode(
                    -1L,
                    code,
                    config.activationCodes
                )
                when (result) {
                    is ActivationResult.Success -> {
                        onActivated()
                    }
                    else -> {}
                }
            }
        }
    )
}

/**
 * Shell modeannouncementdialog
 */
@Composable
fun ShellAnnouncementDialog(
    config: ShellConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val announcement = ShellRuntimeServices.announcement

    // Build Announcement
    val shellAnnouncement = Announcement(
        title = config.announcementTitle,
        content = config.announcementContent,
        linkUrl = config.announcementLink.ifEmpty { null },
        linkText = config.announcementLinkText.ifEmpty { null },
        template = try {
            com.webtoapp.data.model.AnnouncementTemplateType.valueOf(config.announcementTemplate)
        } catch (e: Exception) {
            com.webtoapp.data.model.AnnouncementTemplateType.XIAOHONGSHU
        },
        showEmoji = config.announcementShowEmoji,
        animationEnabled = config.announcementAnimationEnabled,
        requireConfirmation = config.announcementRequireConfirmation,
        allowNeverShow = config.announcementAllowNeverShow
    )

    com.webtoapp.ui.components.announcement.AnnouncementDialog(
        config = com.webtoapp.ui.components.announcement.AnnouncementConfig(
            announcement = shellAnnouncement,
            template = com.webtoapp.ui.components.announcement.AnnouncementTemplate.valueOf(
                shellAnnouncement.template.name
            ),
            showEmoji = shellAnnouncement.showEmoji,
            animationEnabled = shellAnnouncement.animationEnabled
        ),
        onDismiss = {
            onDismiss()
            val scope = (context as? AppCompatActivity)?.lifecycleScope
            scope?.launch {
                announcement.markAnnouncementShown(-1L, 1)
            }
        },
        onLinkClick = { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizeExternalUrlForIntent(url)))
            context.startActivity(intent)
        },
        onNeverShowChecked = { checked ->
            if (checked) {
                val scope = (context as? AppCompatActivity)?.lifecycleScope
                scope?.launch {
                    announcement.markNeverShow(-1L)
                }
            }
        }
    )
}

/**
 * Shell modeforce- run dialog
 */
@Composable
fun ShellForcedRunPermissionDialog(
    config: ShellConfig,
    forcedRunActive: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val forcedRunConfig = config.forcedRunConfig ?: return
    val forcedRunManager = ForcedRunManager.getInstance(context)

    ForcedRunPermissionDialog(
        protectionLevel = forcedRunConfig.protectionLevel,
        onDismiss = onDismiss,
        onContinueAnyway = {
            // Userselect,
            onDismiss()
            AppLogger.w("ShellActivity", "User skipped permission, forced run protection degraded")
        },
        onAllPermissionsGranted = {
            // Note
            onDismiss()
            AppLogger.d("ShellActivity", "Forced run permissions all granted")
            // force- run app
            if (forcedRunActive) {
                forcedRunManager.stopForcedRunMode()
                forcedRunManager.startForcedRunMode(forcedRunConfig, -1L)
            }
        }
    )
}
