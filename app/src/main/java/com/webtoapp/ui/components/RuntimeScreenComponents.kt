package com.webtoapp.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.i18n.AppStringsProvider

/**
 * Shared Composable components for runtime Create*Screen pages
 * (Node.js, PHP, Python, Go, WordPress, Frontend).
 *
 * Eliminates ~30-line icon picker, ~40-line env-vars editor,
 * and ~25-line status cards duplicated across 4+ screens.
 */

// ==================== Icon Picker Card ====================

/**
 * Simple icon picker card used by runtime screens.
 * Shows a 64dp icon preview + an OutlinedButton to select from gallery.
 */
@Composable
fun RuntimeIconPickerCard(
    appIcon: Uri?,
    onSelectIcon: () -> Unit,
    placeholderIcon: ImageVector = Icons.Default.Code
) {
    val context = LocalContext.current
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(AppStringsProvider.current().labelIcon, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (appIcon != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(appIcon).crossfade(true).build(),
                            contentDescription = null, modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(placeholderIcon, null, modifier = Modifier.size(32.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                PremiumOutlinedButton(onClick = onSelectIcon) {
                    Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStringsProvider.current().labelIcon)
                }
            }
        }
    }
}

// ==================== Environment Variables Card ====================

/**
 * Environment variables editor card.
 * Shows existing key-value pairs with delete buttons + input row to add new ones.
 */
@Composable
fun RuntimeEnvVarsCard(
    envVars: Map<String, String>,
    newEnvKey: String,
    newEnvValue: String,
    onNewKeyChange: (String) -> Unit,
    onNewValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Terminal, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(AppStringsProvider.current().njsEnvVars, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))

            envVars.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$key = $value",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    )
                    IconButton(onClick = { onRemove(key) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newEnvKey, onValueChange = onNewKeyChange, label = { Text("Key") }, modifier = Modifier.weight(weight = 1f, fill = true), singleLine = true)
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(value = newEnvValue, onValueChange = onNewValueChange, label = { Text("Value") }, modifier = Modifier.weight(weight = 1f, fill = true), singleLine = true)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onAdd) { Icon(Icons.Default.Add, AppStringsProvider.current().njsAddEnvVar) }
            }
        }
    }
}

// ==================== Status Cards ====================

/**
 * Loading/progress card shown during project creation.
 */
@Composable
fun RuntimeLoadingCard(creationPhase: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(creationPhase)
        }
    }
}

/**
 * Error message card with dismiss button.
 */
@Composable
fun RuntimeErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(12.dp))
            Text(error, modifier = Modifier.weight(weight = 1f, fill = true))
            TextButton(onClick = onDismiss) { Text(AppStringsProvider.current().btnCancel) }
        }
    }
}
