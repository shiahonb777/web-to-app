package com.webtoapp.ui.screens

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.webtoapp.R
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.design.WtaBadge
import com.webtoapp.ui.design.WtaRadius
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSection
import com.webtoapp.ui.design.WtaSectionDivider
import com.webtoapp.ui.design.WtaSettingCard
import com.webtoapp.ui.design.WtaSettingRow
import com.webtoapp.ui.design.WtaSpacing
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone
import com.webtoapp.ui.design.WtaToggleRow
import com.webtoapp.ui.theme.AppColors
import com.webtoapp.ui.theme.LocalAppTheme
import com.webtoapp.util.AppUpdateChecker
import com.webtoapp.util.openUrl
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    val (currentVersionName, currentVersionCode) = remember {
        AppUpdateChecker.getCurrentVersionInfo(context)
    }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdateChecker.UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateFailureReport by remember { mutableStateOf<UpdateFailureReport?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadId by remember { mutableLongStateOf(-1L) }
    var autoCheckEnabled by remember { mutableStateOf(AppUpdateChecker.isAutoCheckEnabled(context)) }

    DisposableEffect(downloadId) {
        if (downloadId == -1L) return@DisposableEffect onDispose {}
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (id == downloadId) {
                    isDownloading = false
                    Toast.makeText(context, Strings.downloadComplete, Toast.LENGTH_SHORT).show()
                    AppUpdateChecker.installApk(context, downloadId)
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    fun showUpdateFailureReport(
        title: String,
        stage: String,
        summary: String,
        throwable: Throwable? = null,
        extraContext: String? = null
    ) {
        updateFailureReport = buildUpdateFailureReport(
            title = title,
            stage = stage,
            summary = summary,
            currentVersionName = currentVersionName,
            currentVersionCode = currentVersionCode,
            throwable = throwable,
            extraContext = extraContext
        )
    }

    fun triggerManualUpdateCheck() {
        scope.launch {
            isCheckingUpdate = true
            updateFailureReport = null
            try {
                AppUpdateChecker.checkUpdate(currentVersionName, currentVersionCode)
                    .onSuccess { info ->
                        updateInfo = info
                        showUpdateDialog = true
                    }
                    .onFailure { error ->
                        showUpdateFailureReport(
                            title = Strings.updateCheckFailedTitle,
                            stage = Strings.updateCheckManualStage,
                            summary = Strings.updateCheckRequestFailed,
                            throwable = error,
                            extraContext = """
                                trigger: manual
                                current_version_name: v$currentVersionName
                                current_version_code: $currentVersionCode
                            """.trimIndent()
                        )
                    }
            } catch (error: Exception) {
                showUpdateFailureReport(
                    title = Strings.updateCheckFailedTitle,
                    stage = Strings.updateCheckManualStage,
                    summary = Strings.updateCheckUncaughtException,
                    throwable = error,
                    extraContext = """
                        trigger: manual
                        current_version_name: v$currentVersionName
                        current_version_code: $currentVersionCode
                    """.trimIndent()
                )
            } finally {
                isCheckingUpdate = false
            }
        }
    }

    WtaScreen(
        title = Strings.about,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        actions = {
            IconButton(onClick = { triggerManualUpdateCheck() }) {
                Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = WtaSpacing.ScreenHorizontal,
                    vertical = WtaSpacing.ScreenVertical
                ),
            verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)
        ) {
            WtaSection(title = Strings.aboutThisApp) {
                WtaSettingCard {
                    val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .drawBehind {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                listOf(glowColor, Color.Transparent)
                                            )
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(id = R.drawable.avatar_shiaho),
                                    contentDescription = Strings.authorAvatar,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Shiaho", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(Strings.authorTagline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WtaBadge(
                        text = "WebToApp",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    WtaBadge(
                        text = "v$currentVersionName",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.clickable { triggerManualUpdateCheck() }
                    )
                }
            }
                        }

                        WtaSettingRow(
                            icon = Icons.Outlined.SystemUpdate,
                            title = Strings.checkUpdate,
                            subtitle = if (isDownloading) Strings.downloading else if (isCheckingUpdate) Strings.checking else "${Strings.currentVersion} v$currentVersionName",
                            onClick = { triggerManualUpdateCheck() }
                        ) {
                            if (isDownloading || isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(18.dp))
                            }
                        }

                        WtaToggleRow(
                            title = Strings.autoCheckUpdate,
                            subtitle = Strings.autoCheckUpdateDesc,
                            icon = Icons.Outlined.Autorenew,
                            checked = autoCheckEnabled,
                            onCheckedChange = {
                                autoCheckEnabled = it
                                AppUpdateChecker.setAutoCheckEnabled(context, it)
                            }
                        )
                    }
                }
            }

            WtaSection(title = Strings.aboutThisApp) {
                WtaSettingCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            Strings.aboutAppDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            WtaSection(title = Strings.dataBackupTitle) {
                com.webtoapp.ui.components.DataBackupCard()
            }

            WtaSection(title = Strings.socialMedia) {
                WtaSettingCard {
                    SocialLinkRow(
                        icon = Icons.Outlined.Share,
                        iconColor = MaterialTheme.colorScheme.primary,
                        title = "X",
                        subtitle = "@shiaho777",
                        onClick = { context.openUrl("https://x.com/@shiaho777") }
                    )
                    WtaSectionDivider()
                    SocialLinkRow(
                        icon = Icons.Outlined.Share,
                        iconColor = AppColors.TelegramBlue,
                        title = "Telegram",
                        subtitle = Strings.contactAuthorDescription,
                        onClick = { context.openUrl("https://t.me/webtoapp777") }
                    )
                    WtaSectionDivider()
                        SocialLinkRow(
                            icon = Icons.Outlined.Share,
                            iconColor = AppColors.Gray,
                        title = "GitHub",
                        subtitle = Strings.openSourceRepository,
                        onClick = { context.openUrl("https://github.com/shiahonb777/web-to-app") }
                    )
                    WtaSectionDivider()
                        SocialLinkRow(
                            icon = Icons.Outlined.Share,
                            iconColor = AppColors.BilibiliPink,
                        title = "Bilibili",
                        subtitle = Strings.videoTutorialLabel,
                        onClick = { context.openUrl("https://b23.tv/8mGDo2N") }
                    )
                }
            }

            WtaSection(title = Strings.contactAuthor) {
                WtaSettingCard {
                        ContactLinkRow(
                            context = context,
                            iconText = "Q",
                            iconBgColor = AppColors.TelegramBlue,
                        title = Strings.qqGroupLabel,
                        value = "1041130206",
                        subtitle = Strings.exchangeLearningUpdates,
                        link = "https://qun.qq.com/universal-share/share?ac=1&authKey=85Y3%2FckhO7c13%2F1%2F4kee5U7dg5dBPQ%2BDvKyGRVxiLVIgO8WxHdq%2BviYCtfWP4IsJ&busi_data=eyJncm91cENvZGUiOiIxMDQxMTMwMjA2IiwidG9rZW4iOiI1ZUhyRWF0bWhYVjN1T2p2VDJVODRPS3lKNzRCMjlyRmgrK3Robzg1cDhrbkF0bHlYR1d4eU43eW9QUTRGOUs4IiwidWluIjoiMjcxMTY3NDE4NCJ9&data=KG-7jSMVH0EM00Ekocv3-F15tvRkal3f4yQPwRmKS7dK0h13g8VPDADK2doELNhlgyPjrFJDFANTkzbibLL1ug&svctype=4&tempid=h5_group_info",
                        copyValue = "1041130206"
                    )
                    WtaSectionDivider()
                        ContactLinkRow(
                            context = context,
                            iconText = "TG",
                            iconBgColor = AppColors.TelegramBlue,
                        title = Strings.telegramGroupLabel,
                        value = "webtoapp777",
                        subtitle = Strings.internationalUserGroup,
                        link = "https://t.me/webtoapp777",
                        copyValue = "https://t.me/webtoapp777"
                    )
                }
            }

            WtaSection(title = Strings.contactAuthor) {
                WtaSettingCard {
                        ContactLinkRow(
                            context = context,
                            iconText = "Q",
                            iconBgColor = AppColors.TelegramBlue,
                        title = "QQ",
                        value = "2711674184",
                        subtitle = Strings.feedbackConsultation,
                        link = "https://i.qq.com/2711674184",
                        copyValue = "2711674184"
                    )
                    WtaSectionDivider()
                        ContactLinkRow(
                            context = context,
                            iconText = "✉",
                            iconBgColor = AppColors.Error,
                        title = "QQ Email",
                        value = "2711674184@qq.com",
                        subtitle = Strings.emailContact,
                        link = "mailto:2711674184@qq.com",
                        copyValue = "2711674184@qq.com"
                    )
                    WtaSectionDivider()
                        ContactLinkRow(
                            context = context,
                            iconText = "G",
                            iconBgColor = AppColors.GoogleBlue,
                        title = "Gmail",
                        value = "weuwo479@gmail.com",
                        subtitle = Strings.internationalEmail,
                        link = "mailto:weuwo479@gmail.com",
                        copyValue = "weuwo479@gmail.com"
                    )
                }
            }

            WtaSection(title = Strings.openSourceRepo) {
                WtaSettingCard {
                        ContactLinkRow(
                            context = context,
                            iconText = "⌘",
                            iconBgColor = AppColors.Gray,
                        title = "GitHub",
                        value = "shiahonb777/web-to-app",
                        subtitle = Strings.internationalAccess,
                        link = "https://github.com/shiahonb777/web-to-app",
                        copyValue = "https://github.com/shiahonb777/web-to-app"
                    )
                }
            }

            WtaSection(title = Strings.changelog) {
                WtaSettingCard {
                    VersionBlock("v1.9.5", true) {
                        ChangeItem("feature", Strings.cookiesPersistenceFeature)
                        ChangeItem("feature", Strings.multiApiKeyManagement)
                        ChangeItem("feature", Strings.modelNameSearchFeature)
                        ChangeItem("feature", Strings.hideUrlPreviewFeature)
                        ChangeItem("feature", Strings.popupBlockerFeature)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    VersionBlock("v1.9.0") {
                        ChangeItem("feature", Strings.browserEngineFeature)
                        ChangeItem("feature", Strings.browserSpoofingFeature)
                        ChangeItem("feature", Strings.hostsBlockFeature)
                        ChangeItem("feature", Strings.longPressMenuFeature)
                        ChangeItem("feature", Strings.apkArchitectureFeature)
                    }
                }
            }

            WtaSection(title = Strings.legalDisclaimer) {
                WtaSettingCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        WtaStatusBanner(
                            title = Strings.disclaimerWarningText,
                            message = Strings.disclaimerWarningText,
                            tone = WtaStatusTone.Warning
                        )
                        LegalSection(Strings.legalDisclaimerTitle1, Strings.legalDisclaimerContent1)
                        LegalSection(Strings.legalDisclaimerTitle2, Strings.legalDisclaimerContent2)
                        LegalSection(Strings.legalDisclaimerTitle3, Strings.legalDisclaimerContent3)
                        LegalSection(Strings.legalDisclaimerTitle4, Strings.legalDisclaimerContent4)
                        LegalSection(
                            Strings.legalDisclaimerTitle5,
                            Strings.legalDisclaimerContent5
                        )
                        LegalSection(
                            Strings.legalDisclaimerTitle6,
                            Strings.legalDisclaimerContent6
                        )
                        WtaStatusBanner(
                            title = Strings.finalUserAgreementConfirmation,
                            message = Strings.legalDisclaimerAcceptance,
                            tone = WtaStatusTone.Info
                        )
                        Text(
                            Strings.legalDisclaimerFooter,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Text(
                text = Strings.madeWithLove,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showUpdateDialog && updateInfo != null) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            currentVersion = currentVersionName,
            currentVersionCode = currentVersionCode,
            isDownloading = isDownloading,
            onDismiss = { showUpdateDialog = false },
            onDownload = {
                if (updateInfo!!.downloadUrl.isNotEmpty()) {
                    isDownloading = true
                    downloadId = AppUpdateChecker.downloadApk(context, updateInfo!!.downloadUrl, updateInfo!!.versionName)
                    if (downloadId == -1L) {
                        isDownloading = false
                        showUpdateFailureReport(
                            title = Strings.updateDownloadStartFailedTitle,
                            stage = Strings.updateDownloadStartStage,
                            summary = Strings.updateDownloadCreateFailed,
                            extraContext = """
                                current_version_name: v$currentVersionName
                                current_version_code: $currentVersionCode
                                target_version_name: ${updateInfo!!.versionName}
                                download_url: ${updateInfo!!.downloadUrl}
                            """.trimIndent()
                        )
                    } else {
                        Toast.makeText(context, Strings.startDownloadCheckNotification, Toast.LENGTH_SHORT).show()
                        showUpdateDialog = false
                    }
                } else {
                    showUpdateFailureReport(
                        title = Strings.updateDownloadStartFailedTitle,
                        stage = Strings.updateDownloadPrepareStage,
                        summary = Strings.updateDownloadNoLink,
                        extraContext = """
                            current_version_name: v$currentVersionName
                            current_version_code: $currentVersionCode
                            target_version_name: ${updateInfo!!.versionName}
                            download_url: <empty>
                        """.trimIndent()
                    )
                }
            }
        )
    }

    updateFailureReport?.let {
        UpdateFailureReportDialog(
            report = it,
            onDismiss = { updateFailureReport = null }
        )
    }
}

@Composable
private fun SocialLinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    WtaSettingRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick
    ) {
        Icon(Icons.Outlined.OpenInNew, null, tint = iconColor, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ContactLinkRow(
    context: Context,
    iconText: String,
    iconBgColor: Color,
    title: String,
    value: String,
    subtitle: String,
    link: String,
    copyValue: String
) {
    val clipboardManager = LocalClipboardManager.current
    WtaSettingRow(
        title = title,
        subtitle = "$value · $subtitle",
        iconContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(WtaRadius.Control))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(iconText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        onClick = {
            runCatching { context.openUrl(link) }.onFailure {
                Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        FilledTonalIconButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(copyValue))
                Toast.makeText(context, "${title} ${Strings.copied}", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Filled.ContentCopy, Strings.copy, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(4.dp))
        FilledTonalIconButton(
            onClick = {
                runCatching { context.openUrl(link) }.onFailure {
                    Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Outlined.OpenInNew, Strings.openAction, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun VersionBlock(
    version: String,
    isLatest: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(version, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            if (isLatest) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(WtaRadius.Button), color = AppColors.Success) {
                    Text(Strings.latestTag, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ChangeItem(emoji: String, text: String) {
    val iconMap = mapOf(
        "feature" to Icons.Outlined.Campaign,
        "improve" to Icons.Outlined.Speed,
        "bugfix" to Icons.Outlined.Warning,
        "design" to Icons.Outlined.Palette,
        "security" to Icons.Outlined.Lock,
        "save" to Icons.Outlined.ContentCopy,
        "mobile" to Icons.Outlined.Devices,
        "celebrate" to Icons.Outlined.Info,
        "i18n" to Icons.Outlined.Language,
        "upload" to Icons.Outlined.ArrowForward,
        "module" to Icons.Outlined.Code,
        "lock" to Icons.Outlined.Warning,
        "linux" to Icons.Outlined.Terminal,
        "framework" to Icons.Outlined.Code,
        "web" to Icons.Outlined.Language,
        "play" to Icons.Filled.PlayArrow,
        "extension" to Icons.Outlined.Code,
        "ai" to Icons.Outlined.Info,
        "library" to Icons.Outlined.Info,
        "music" to Icons.Outlined.Info,
        "announce" to Icons.Outlined.Campaign
    )
    val icon = iconMap[emoji] ?: Icons.Outlined.Info
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UpdateDialog(
    updateInfo: AppUpdateChecker.UpdateInfo,
    currentVersion: String,
    currentVersionCode: Int,
    isDownloading: Boolean,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (updateInfo.hasUpdate) Icons.Outlined.SystemUpdate else Icons.Outlined.CheckCircle,
                null,
                tint = if (updateInfo.hasUpdate) AppColors.UpdateBlue else AppColors.Success,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text(if (updateInfo.hasUpdate) Strings.newVersionFound else Strings.latestVersion, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                if (updateInfo.hasUpdate) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(WtaRadius.Button), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text("v$currentVersion", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                        }
                        Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(shape = RoundedCornerShape(WtaRadius.Button), color = AppColors.Success) {
                            Text(updateInfo.versionName, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (updateInfo.releaseNotes.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(WtaRadius.Card),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
                        ) {
                            Text(updateInfo.releaseNotes, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(Strings.updateRecommendation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                } else {
                    Text("${Strings.currentVersionIs.replace("%s", currentVersion)}\nversionCode: $currentVersionCode", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        },
        confirmButton = {
            if (updateInfo.hasUpdate) {
                PremiumButton(onClick = onDownload, enabled = !isDownloading) {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isDownloading) Strings.downloading else Strings.updateNow)
                }
            } else {
                TextButton(onClick = onDismiss) { Text(Strings.btnOk) }
            }
        },
        dismissButton = {
            if (updateInfo.hasUpdate) {
                TextButton(onClick = onDismiss) { Text(Strings.updateLater) }
            }
        }
    )
}

private data class UpdateFailureReport(val title: String, val summary: String, val details: String)

private fun buildUpdateFailureReport(
    title: String,
    stage: String,
    summary: String,
    currentVersionName: String,
    currentVersionCode: Int,
    throwable: Throwable? = null,
    extraContext: String? = null
): UpdateFailureReport {
    val details = buildString {
        appendLine("stage: $stage")
        appendLine("summary: $summary")
        appendLine("current_version_name: v$currentVersionName")
        appendLine("current_version_code: $currentVersionCode")
        if (!extraContext.isNullOrBlank()) {
            appendLine()
            appendLine("context:")
            appendLine(extraContext)
        }
        appendLine()
        appendLine("error:")
        appendLine(throwable?.message ?: Strings.noExceptionObject)
        throwable?.let {
            appendLine()
            appendLine("stacktrace:")
            appendLine(Log.getStackTraceString(it))
        }
        appendLine()
        appendLine("recent_logs:")
        append(AppLogger.getRecentLogTail())
    }
    return UpdateFailureReport(title, summary, details)
}

@Composable
private fun UpdateFailureReportDialog(
    report: UpdateFailureReport,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(report.title)
                Text(report.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                shape = RoundedCornerShape(WtaRadius.Card),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = report.details,
                        modifier = Modifier.fillMaxWidth().padding(14.dp).padding(bottom = 48.dp).verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall
                    )
                    FilledTonalButton(
                        onClick = { clipboardManager.setText(AnnotatedString(report.details)) },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Strings.copy)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(Strings.close) } }
    )
}

@Composable
private fun LegalSection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
    }
}
