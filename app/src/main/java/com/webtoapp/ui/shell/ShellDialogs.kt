package com.webtoapp.ui.shell

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.activation.ActivationResult
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.forcedrun.ForcedRunManager
import com.webtoapp.core.forcedrun.ForcedRunPermissionDialog
import com.webtoapp.data.model.Announcement
import com.webtoapp.ui.splash.ActivationDialog
import kotlinx.coroutines.launch

/**
 * Shell 模式激活码对话框
 */
@Composable
fun ShellActivationDialog(
    config: ShellConfig,
    onDismiss: () -> Unit,
    onActivated: () -> Unit
) {
    val context = LocalContext.current
    val activation = WebToAppApplication.activation
    val announcement = WebToAppApplication.announcement

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
 * Shell 模式公告对话框
 */
@Composable
fun ShellAnnouncementDialog(
    config: ShellConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val announcement = WebToAppApplication.announcement

    // Build Announcement 对象
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
 * Shell 模式强制运行权限引导对话框
 */
@Composable
fun ShellForcedRunPermissionDialog(
    config: ShellConfig,
    forcedRunActive: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val forcedRunManager = ForcedRunManager.getInstance(context)

    ForcedRunPermissionDialog(
        protectionLevel = config.forcedRunConfig!!.protectionLevel,
        onDismiss = onDismiss,
        onContinueAnyway = {
            // User选择跳过，降级防护继续使用
            onDismiss()
            AppLogger.w("ShellActivity", "User skipped permission, forced run protection degraded")
        },
        onAllPermissionsGranted = {
            // 所有权限已授权
            onDismiss()
            AppLogger.d("ShellActivity", "Forced run permissions all granted")
            // 重新启动强制运行以应用新权限
            if (forcedRunActive) {
                forcedRunManager.stopForcedRunMode()
                config.forcedRunConfig?.let { cfg ->
                    forcedRunManager.startForcedRunMode(cfg, -1L)
                }
            }
        }
    )
}
