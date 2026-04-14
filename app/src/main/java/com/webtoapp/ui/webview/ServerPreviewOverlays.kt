package com.webtoapp.ui.webview

import androidx.compose.foundation.background
import com.webtoapp.ui.components.PremiumButton
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.wordpress.WordPressDependencyManager

/**
 * WordPress load
 */
@Composable
fun WordPressLoadingOverlay(
    state: WordPressPreviewState,
    downloadState: WordPressDependencyManager.DownloadState,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            when (state) {
                is WordPressPreviewState.CheckingDeps -> {
                    CircularProgressIndicator()
                    Text(Strings.wpCheckingDeps)
                }
                is WordPressPreviewState.Downloading -> {
                    when (val dlState = downloadState) {
                        is WordPressDependencyManager.DownloadState.Downloading -> {
                            LinearProgressIndicator(
                                progress = { dlState.progress },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                            Text("${Strings.wpDownloading}: ${dlState.currentFile}")
                            Text(
                                "${formatWpBytes(dlState.bytesDownloaded)} / ${formatWpBytes(dlState.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is WordPressDependencyManager.DownloadState.Extracting -> {
                            CircularProgressIndicator()
                            Text("${Strings.wpExtracting}: ${dlState.fileName}")
                        }
                        is WordPressDependencyManager.DownloadState.Verifying -> {
                            CircularProgressIndicator()
                            Text("${dlState.fileName}...")
                        }
                        else -> {
                            CircularProgressIndicator()
                            Text(Strings.wpDownloading)
                        }
                    }
                    // Note
                    MirrorSourceInfo()
                }
                is WordPressPreviewState.CreatingProject -> {
                    CircularProgressIndicator()
                    Text(Strings.wpCreatingProject)
                }
                is WordPressPreviewState.StartingServer -> {
                    CircularProgressIndicator()
                    Text(Strings.wpStartingServer)
                }
                is WordPressPreviewState.Error -> {
                    ErrorWithRetry(state.message, onRetry)
                }
                else -> {}
            }
        }
    }
}

/**
 * PHP appload
 */
@Composable
fun PhpAppLoadingOverlay(
    state: PhpAppPreviewState,
    downloadState: WordPressDependencyManager.DownloadState,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            when (state) {
                is PhpAppPreviewState.CheckingDeps -> {
                    CircularProgressIndicator()
                    Text(Strings.phpAppCheckingDeps)
                }
                is PhpAppPreviewState.Downloading -> {
                    when (val dlState = downloadState) {
                        is WordPressDependencyManager.DownloadState.Downloading -> {
                            LinearProgressIndicator(
                                progress = { dlState.progress },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                            Text("${Strings.phpAppDownloading}: ${dlState.currentFile}")
                            Text(
                                "${formatWpBytes(dlState.bytesDownloaded)} / ${formatWpBytes(dlState.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is WordPressDependencyManager.DownloadState.Extracting -> {
                            CircularProgressIndicator()
                            Text("${Strings.wpExtracting}: ${dlState.fileName}")
                        }
                        else -> {
                            CircularProgressIndicator()
                            Text(Strings.phpAppDownloading)
                        }
                    }
                    // Note
                    MirrorSourceInfo()
                }
                is PhpAppPreviewState.StartingServer -> {
                    CircularProgressIndicator()
                    Text(Strings.phpAppStartingServer)
                }
                is PhpAppPreviewState.Error -> {
                    ErrorWithRetry(state.message, onRetry)
                }
                else -> {}
            }
        }
    }
}

/**
 * Python appload
 */
@Composable
fun PythonAppLoadingOverlay(
    state: PythonAppPreviewState,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            when (state) {
                is PythonAppPreviewState.Starting -> {
                    CircularProgressIndicator()
                    Text(Strings.pyStartingPreview)
                }
                is PythonAppPreviewState.Error -> {
                    ErrorWithRetry(state.message, onRetry)
                }
                else -> {}
            }
        }
    }
}

/**
 * Node. js appload( with Go)
 */
@Composable
fun SimpleAppLoadingOverlay(
    isStarting: Boolean,
    startingText: String,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            if (isStarting) {
                CircularProgressIndicator()
                Text(startingText)
            } else if (errorMessage != null) {
                ErrorWithRetry(errorMessage, onRetry)
            }
        }
    }
}

// Note

@Composable
private fun ErrorWithRetry(message: String, onRetry: () -> Unit) {
    Icon(
        Icons.Outlined.Warning,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.error
    )
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center
    )
    PremiumButton(onClick = onRetry) {
        Text(Strings.btnRetry)
    }
}

@Composable
private fun MirrorSourceInfo() {
    Text(
        "${Strings.wpMirrorSource}: ${
            if (WordPressDependencyManager.getMirrorRegion() == WordPressDependencyManager.MirrorRegion.CN)
                Strings.wpMirrorCN else Strings.wpMirrorGlobal
        }",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
