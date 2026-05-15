package com.webtoapp.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.backup.DataBackupManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.design.WtaButton
import com.webtoapp.ui.design.WtaButtonSize
import com.webtoapp.ui.design.WtaButtonVariant
import com.webtoapp.ui.design.WtaCard
import com.webtoapp.ui.design.WtaCardTone
import com.webtoapp.ui.design.WtaIconTitle
import com.webtoapp.ui.design.WtaMotion
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone
import kotlinx.coroutines.launch

/**
 * Data backup controls. Presents two equally weighted actions: export (to
 * zip file) and import (from zip file). While an action is in flight, a
 * progress strip slides in below the header and the buttons dim; the info
 * hint only shows when the controls are idle so it does not compete with
 * the live progress feedback.
 */
@Composable
fun DataBackupCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { DataBackupManager(context) }
    val repository = remember { WebToAppApplication.repository }

    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }
    val isBusy = isExporting || isImporting

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isExporting = true
                val result = backupManager.exportAllData(
                    repository = repository,
                    outputUri = uri,
                    onProgress = { _, _, message ->
                        progressMessage = message
                    }
                )
                isExporting = false
                progressMessage = ""
                result.onSuccess { exportResult ->
                    Toast.makeText(
                        context,
                        Strings.exportSuccess.format(
                            exportResult.appCount,
                            exportResult.resourceCount
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { e ->
                    Toast.makeText(
                        context,
                        Strings.exportFailed.format(e.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                val result = backupManager.importAllData(
                    repository = repository,
                    inputUri = uri,
                    onProgress = { _, _, message ->
                        progressMessage = message
                    }
                )
                isImporting = false
                progressMessage = ""
                result.onSuccess { importResult ->
                    Toast.makeText(
                        context,
                        Strings.importSuccess.format(
                            importResult.importedCount,
                            importResult.totalCount
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { e ->
                    Toast.makeText(
                        context,
                        Strings.importFailed.format(e.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    WtaCard(modifier = Modifier.fillMaxWidth()) {
        WtaIconTitle(
            icon = Icons.Outlined.Inventory2,
            title = Strings.dataBackupTitle,
            subtitle = Strings.dataBackupDesc
        )

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(
            visible = isBusy,
            enter = fadeIn(WtaMotion.enterTween()),
            exit = fadeOut(WtaMotion.exitTween())
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
                if (progressMessage.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        progressMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(14.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExportButton(
                isExporting = isExporting,
                enabled = !isBusy,
                onClick = {
                    val fileName = backupManager.generateBackupFileName()
                    exportLauncher.launch(fileName)
                },
                modifier = Modifier.weight(1f)
            )
            ImportButton(
                isImporting = isImporting,
                enabled = !isBusy,
                onClick = {
                    importLauncher.launch(arrayOf("application/zip"))
                },
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedVisibility(
            visible = !isBusy,
            enter = fadeIn(WtaMotion.enterTween()),
            exit = fadeOut(WtaMotion.exitTween())
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(12.dp))
                WtaStatusBanner(
                    message = Strings.dataBackupNote,
                    tone = WtaStatusTone.Info
                )
            }
        }
    }
}

@Composable
private fun ExportButton(
    isExporting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isExporting) {
        WtaCard(
            modifier = modifier,
            tone = WtaCardTone.Surface,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 10.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    Strings.exportData,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        WtaButton(
            onClick = onClick,
            text = Strings.exportData,
            variant = WtaButtonVariant.Outlined,
            size = WtaButtonSize.Medium,
            enabled = enabled,
            leadingIcon = Icons.Outlined.Upload,
            modifier = modifier
        )
    }
}

@Composable
private fun ImportButton(
    isImporting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isImporting) {
        WtaCard(
            modifier = modifier,
            tone = WtaCardTone.Highlighted,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 10.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    Strings.importData,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    } else {
        WtaButton(
            onClick = onClick,
            text = Strings.importData,
            variant = WtaButtonVariant.Primary,
            size = WtaButtonSize.Medium,
            enabled = enabled,
            leadingIcon = Icons.Outlined.Download,
            modifier = modifier
        )
    }
}
