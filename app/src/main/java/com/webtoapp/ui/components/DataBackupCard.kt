package com.webtoapp.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.backup.DataBackupManager
import kotlinx.coroutines.launch

/**
 * 数据备份卡片组件
 * 提供一键导出和导入所有应用数据的功能
 */
@Composable
fun DataBackupCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { DataBackupManager(context) }
    val repository = remember { WebToAppApplication.repository }

    // 状态
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }
    var showProgress by remember { mutableStateOf(false) }

    // 导出文件选择器
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isExporting = true
                showProgress = true

                val result = backupManager.exportAllData(
                    repository = repository,
                    outputUri = uri,
                    onProgress = { current, total, message ->
                        progressMessage = message
                    }
                )

                isExporting = false
                showProgress = false

                result.onSuccess { exportResult ->
                    Toast.makeText(
                        context,
                        Strings.exportSuccessMessage.format(exportResult.appCount, exportResult.resourceCount),
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { e ->
                    Toast.makeText(
                        context,
                        Strings.exportFailedMessage.format(e.message ?: ""),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // 导入文件选择器
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                showProgress = true

                val result = backupManager.importAllData(
                    repository = repository,
                    inputUri = uri,
                    onProgress = { current, total, message ->
                        progressMessage = message
                    }
                )

                isImporting = false
                showProgress = false

                result.onSuccess { importResult ->
                    Toast.makeText(
                        context,
                        Strings.importSuccessMessage.format(importResult.importedCount, importResult.totalCount),
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { e ->
                    Toast.makeText(
                        context,
                        Strings.importFailedMessage.format(e.message ?: ""),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Backup,
                    null,
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    Strings.dataBackupTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                Strings.dataBackupDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 进度显示
            if (showProgress) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    progressMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 导出按钮
                OutlinedButton(
                    onClick = {
                        val fileName = backupManager.generateBackupFileName()
                        exportLauncher.launch(fileName)
                    },
                    enabled = !isExporting && !isImporting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Upload,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.exportData)
                }

                // 导入按钮
                Button(
                    onClick = {
                        importLauncher.launch(arrayOf("application/zip"))
                    },
                    enabled = !isExporting && !isImporting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Download,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.importData)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 提示信息
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        Strings.dataBackupNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
