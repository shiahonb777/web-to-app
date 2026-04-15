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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.i18n.Strings

/**
 * Shared Composable components for runtime Create*Screen pages
 * (Node.js, PHP, Python, Go, WordPress, Frontend).
 *
 * Eliminates ~30-line icon picker, ~40-line env-vars editor,
 * ~25-line status cards, ~60-line hero sections, and ~10-line section headers
 * duplicated across 6+ screens.
 */

// ==================== Section Header ====================

/**
 * Unified section header with icon box + title.
 * Replaces inline Row { Box+Icon + Text } patterns across all runtime screens.
 *
 * @param icon The icon to display in the header
 * @param title The section title text
 * @param brandColor Brand color for the icon box (defaults to primary)
 * @param trailing Optional trailing content (e.g. badge, count)
 */
@Composable
fun RuntimeSectionHeader(
    icon: ImageVector,
    title: String,
    brandColor: Color = MaterialTheme.colorScheme.primary,
    trailing: @Composable (RowScope.() -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(brandColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = brandColor, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (trailing != null) {
            Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
            trailing()
        }
    }
}

// ==================== Hero Section ====================

/**
 * Unified brand-colored hero section for runtime screens.
 * Replaces PythonHeroSection, WpHeroSection, NodeJsHeroSection,
 * PhpHeroSection, GoHeroSection — all sharing the same layout pattern.
 *
 * @param icon The hero icon
 * @param title Main title (e.g. "Flask Web App")
 * @param subtitle Description text
 * @param brandColor Primary brand color for gradients and accents
 * @param tags Optional technology tags displayed in a FlowRow (e.g. "Python 3.11", "PHP")
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RuntimeHeroSection(
    icon: ImageVector,
    title: String,
    subtitle: String,
    brandColor: Color,
    tags: List<Pair<String, Color>> = emptyList()
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(brandColor.copy(alpha = 0.15f), brandColor.copy(alpha = 0.05f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = brandColor.copy(alpha = 0.15f)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(icon, null, modifier = Modifier.size(32.dp), tint = brandColor)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = brandColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            tags.forEach { (label, tagColor) ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = tagColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tagColor,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== Success/Ready Card ====================

/**
 * Unified project-ready success card.
 * Replaces inline success indicators in WordPress and other runtime screens.
 *
 * @param title Success title text
 * @param subtitle Optional detail line (e.g. project ID)
 * @param brandColor Brand color for the card accent
 */
@Composable
fun RuntimeSuccessCard(
    title: String,
    subtitle: String? = null,
    brandColor: Color = MaterialTheme.colorScheme.primary
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = brandColor.copy(alpha = 0.08f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CheckCircle, null, tint = brandColor)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = brandColor,
                    fontWeight = FontWeight.Bold
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// ==================== Branded Loading Card ====================

/**
 * Loading card with brand color (for WordPress and other branded screens).
 * Falls back to RuntimeLoadingCard pattern but with custom brand color.
 */
@Composable
fun RuntimeBrandedLoadingCard(
    creationPhase: String,
    brandColor: Color
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = brandColor.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = brandColor)
            Spacer(modifier = Modifier.width(16.dp))
            Text(creationPhase, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ==================== Branded Error Card ====================

/**
 * Error card using EnhancedElevatedCard (consistent with branded screens).
 */
@Composable
fun RuntimeBrandedErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(12.dp))
            Text(error, modifier = Modifier.weight(weight = 1f, fill = true), color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onDismiss) { Text(Strings.btnCancel) }
        }
    }
}

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
            RuntimeSectionHeader(
                icon = Icons.Outlined.Image,
                title = Strings.labelIcon
            )
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
                    Text(Strings.labelIcon)
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
            RuntimeSectionHeader(
                icon = Icons.Outlined.Terminal,
                title = Strings.njsEnvVars
            )
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
                IconButton(onClick = onAdd) { Icon(Icons.Default.Add, Strings.njsAddEnvVar) }
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
            TextButton(onClick = onDismiss) { Text(Strings.btnCancel) }
        }
    }
}
