package com.webtoapp.ui.screens

import android.net.Uri
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.stats.BatchImportService
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color

/**
 * importdialog
 */
@Composable
fun BatchImportDialog(
    importService: BatchImportService,
    onDismiss: () -> Unit,
    onImport: suspend (List<BatchImportService.ParsedEntry>) -> Int
) {
    val context = LocalContext.current
    val importScope = rememberCoroutineScope()
    
    var inputText by remember { mutableStateOf("") }
    var parsedEntries by remember { mutableStateOf<List<BatchImportService.ParsedEntry>>(emptyList()) }
    var isImporting by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<Int?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // fileselect
    val bookmarkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                parsedEntries = importService.parseFromBookmarksHtml(stream)
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        title = { Text(Strings.batchImportTitle) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab switch
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(Strings.batchImportFromText, style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(Icons.Outlined.TextFields, null, Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(Strings.batchImportFromBookmarks, style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(Icons.Outlined.Bookmarks, null, Modifier.size(16.dp)) }
                    )
                }
                
                when (selectedTab) {
                    0 -> {
                        // textinput
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                parsedEntries = importService.parseFromText(it)
                            },
                            label = { Text(Strings.batchImportHint) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            maxLines = 10
                        )
                    }
                    1 -> {
                        // fileimport
                        PremiumOutlinedButton(
                            onClick = { bookmarkLauncher.launch("text/html") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.FileOpen, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(Strings.batchImportFromBookmarks)
                        }
                    }
                }
                
                // preview
                if (parsedEntries.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        Strings.batchImportParsed.replace("%d", parsedEntries.size.toString()),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(parsedEntries) { entry ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Public, null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                        Text(
                                            entry.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            entry.url,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // import
                if (importResult != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                Strings.batchImportSuccess.replace("%d", importResult.toString()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (importResult != null) {
                TextButton(onClick = onDismiss) {
                    Text(Strings.close)
                }
            } else if (!isImporting) {
                PremiumButton(
                    onClick = {
                        isImporting = true
                        importScope.launch {
                            try {
                                val count = onImport(parsedEntries)
                                importResult = count
                            } finally {
                                isImporting = false
                            }
                        }
                    },
                    enabled = parsedEntries.isNotEmpty()
                ) {
                    Icon(Icons.Outlined.FileDownload, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.batchImportBtn)
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        },
        dismissButton = {
            if (!isImporting && importResult == null) {
                TextButton(onClick = onDismiss) {
                    Text(Strings.btnCancel)
                }
            }
        }
    )
}
