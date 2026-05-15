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






@Composable
private fun runtimeAccentColor(): Color = MaterialTheme.colorScheme.onSurface

@Composable
private fun runtimeAccentContainer(alpha: Float = 0.08f): Color =
    MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

@Composable
private fun runtimeMutedContainer(alpha: Float = 0.35f): Color =
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)





















@Composable
fun RuntimeSectionHeader(
    icon: ImageVector,
    title: String,
    @Suppress("UNUSED_PARAMETER")
    brandColor: Color = MaterialTheme.colorScheme.primary,
    trailing: @Composable (RowScope.() -> Unit)? = null
) {
    val accent = runtimeAccentColor()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(runtimeAccentContainer()),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (trailing != null) {
            Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
            trailing()
        }
    }
}














@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RuntimeHeroSection(
    icon: ImageVector,
    title: String,
    subtitle: String,
    @Suppress("UNUSED_PARAMETER")
    brandColor: Color,
    tags: List<Pair<String, Color>> = emptyList()
) {
    val accent = runtimeAccentColor()
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
                        colors = listOf(runtimeAccentContainer(0.10f), runtimeMutedContainer(0.45f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = runtimeAccentContainer(0.10f)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(icon, null, modifier = Modifier.size(32.dp), tint = accent)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            tags.forEach { (label, _) ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = runtimeAccentContainer(0.08f)
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
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











@Composable
fun RuntimeSuccessCard(
    title: String,
    subtitle: String? = null,
    @Suppress("UNUSED_PARAMETER")
    brandColor: Color = MaterialTheme.colorScheme.primary
) {
    val accent = runtimeAccentColor()
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = runtimeAccentContainer(0.08f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CheckCircle, null, tint = accent)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
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







@Composable
fun RuntimeBrandedLoadingCard(
    creationPhase: String,
    @Suppress("UNUSED_PARAMETER")
    brandColor: Color
) {
    val accent = runtimeAccentColor()
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = runtimeAccentContainer(0.08f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = accent)
            Spacer(modifier = Modifier.width(16.dp))
            Text(creationPhase, style = MaterialTheme.typography.bodyMedium)
        }
    }
}






@Composable
fun RuntimeBrandedErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = runtimeAccentContainer(0.08f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(12.dp))
            Text(error, modifier = Modifier.weight(weight = 1f, fill = true), color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = onDismiss) { Text(Strings.btnCancel) }
        }
    }
}







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






@Composable
fun RuntimeLoadingCard(creationPhase: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = runtimeAccentContainer(0.08f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(12.dp))
            Text(creationPhase)
        }
    }
}




@Composable
fun RuntimeErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = runtimeAccentContainer(0.08f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(12.dp))
            Text(error, modifier = Modifier.weight(weight = 1f, fill = true), color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = onDismiss) { Text(Strings.btnCancel) }
        }
    }
}
