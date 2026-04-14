package com.webtoapp.ui.screens

import android.net.Uri
import com.webtoapp.ui.components.PremiumOutlinedButton
import android.provider.DocumentsContract
import com.webtoapp.core.logging.AppLogger
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.linux.HtmlProjectOptimizer
import com.webtoapp.core.linux.NativeNodeEngine
import com.webtoapp.data.model.HtmlConfig
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.HtmlFileType
import com.webtoapp.ui.components.*
import com.webtoapp.util.HtmlProjectProcessor
import com.webtoapp.util.ZipProjectImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.webtoapp.ui.components.ThemedBackgroundBox
import androidx.compose.ui.graphics.Color
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * item dialog
 */
@Composable
internal fun ProjectAnalysisDialog(
    analysis: HtmlProjectProcessor.ProjectAnalysis,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.projectAnalysisResult)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // File
                item {
                    Text(
                        text = Strings.fileInfo,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        analysis.htmlFiles.forEach { file ->
                            FileInfoRow(file, "HTML")
                        }
                        analysis.cssFiles.forEach { file ->
                            FileInfoRow(file, "CSS")
                        }
                        analysis.jsFiles.forEach { file ->
                            FileInfoRow(file, "JS")
                        }
                    }
                }
                
                // list
                if (analysis.issues.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Strings.detectedIssues,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    items(analysis.issues) { issue ->
                        IssueCard(issue)
                    }
                }
                
                // Note
                if (analysis.suggestions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Strings.suggestions,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    items(analysis.suggestions) { suggestion ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Auto
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    EnhancedElevatedCard(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Outlined.AutoFixHigh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Strings.autoProcessHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.gotIt)
            }
        }
    )
}

/**
 * file
 */
@Composable
internal fun FileInfoRow(file: HtmlProjectProcessor.FileInfo, type: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                MaterialTheme.shapes.small
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = type,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.shapes.extraSmall
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = Strings.encodingAndSize.format(file.encoding ?: "UTF-8", formatHtmlProjectFileSize(file.size)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * card
 */
@Composable
internal fun IssueCard(issue: HtmlProjectProcessor.ProjectIssue) {
    val (icon, containerColor, contentColor) = when (issue.severity) {
        HtmlProjectProcessor.IssueSeverity.ERROR -> Triple(
            Icons.Outlined.Error,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        HtmlProjectProcessor.IssueSeverity.WARNING -> Triple(
            Icons.Outlined.Warning,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        HtmlProjectProcessor.IssueSeverity.INFO -> Triple(
            Icons.Outlined.Info,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
    
    EnhancedElevatedCard(
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = issue.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }
            if (issue.file != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Strings.fileLabel.format(issue.file),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            if (issue.suggestion != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${Strings.suggestions}: ${issue.suggestion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * file
 */
internal fun formatHtmlProjectFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format(java.util.Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0))
    }
}

/**
 * file
 */
