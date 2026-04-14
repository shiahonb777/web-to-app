package com.webtoapp.ui.navigation

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.webtoapp.core.i18n.Strings
import com.webtoapp.util.AppUpdateChecker

@Composable
internal fun AppNavigationEffects() {
    val context = LocalContext.current
    var autoUpdateInfo by remember { mutableStateOf<AppUpdateChecker.UpdateInfo?>(null) }
    var showAutoUpdateDialog by remember { mutableStateOf(false) }
    var isAutoDownloading by remember { mutableStateOf(false) }
    var autoDownloadId by remember { mutableStateOf(-1L) }

    LaunchedEffect(Unit) {
        if (AppUpdateChecker.shouldAutoCheck(context)) {
            try {
                val (versionName, versionCode) = AppUpdateChecker.getCurrentVersionInfo(context)
                AppUpdateChecker.recordAutoCheck(context)
                val result = AppUpdateChecker.checkUpdate(versionName, versionCode)
                result.onSuccess { info ->
                    if (info.hasUpdate) {
                        autoUpdateInfo = info
                        showAutoUpdateDialog = true
                    }
                }
            } catch (_: Exception) {
                // Ignore failures silently so the user is not disturbed
            }
        }
    }

    DisposableEffect(autoDownloadId) {
        if (autoDownloadId == -1L) return@DisposableEffect onDispose {}
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id == autoDownloadId) {
                    isAutoDownloading = false
                    AppUpdateChecker.installApk(context, autoDownloadId)
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    if (showAutoUpdateDialog && autoUpdateInfo != null) {
        val info = autoUpdateInfo!!
        val (currentVersionName, _) = remember { AppUpdateChecker.getCurrentVersionInfo(context) }
        AlertDialog(
            onDismissRequest = { showAutoUpdateDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.SystemUpdate,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = Strings.newVersionFound,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "v$currentVersionName",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text("→")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2196F3).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = info.versionName,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                        }
                    }
                    if (info.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = info.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (info.downloadUrl.isNotEmpty()) {
                            isAutoDownloading = true
                            autoDownloadId = AppUpdateChecker.downloadApk(
                                context = context,
                                downloadUrl = info.downloadUrl,
                                versionName = info.versionName
                            )
                            if (autoDownloadId == -1L) {
                                isAutoDownloading = false
                            }
                            showAutoUpdateDialog = false
                        }
                    },
                    enabled = !isAutoDownloading
                ) {
                    if (isAutoDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text(Strings.updateNow)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoUpdateDialog = false }) {
                    Text(Strings.updateLater)
                }
            }
        )
    }
}
