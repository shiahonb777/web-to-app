package com.webtoapp.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.util.FaviconFetcher
import kotlinx.coroutines.launch
import java.io.File

/**
 * icon iconselect
 * 
 * @param iconUri current inicon Uri( select)
 * @param iconPath current iniconpath( icon)
 * @param websiteUrl( for icon, only WEB type)
 * @param onSelectFromGallery from selecticon
 * @param onSelectFromLibrary fromicon selecticon( backfilepath)
 */
@Composable
fun IconPickerWithLibrary(
    iconUri: Uri? = null,
    iconPath: String? = null,
    websiteUrl: String? = null,
    onSelectFromGallery: () -> Unit,
    onSelectFromLibrary: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLibraryDialog by remember { mutableStateOf(false) }
    var showAiGeneratorDialog by remember { mutableStateOf(false) }
    var isFetchingFavicon by remember { mutableStateOf(false) }
    
    // icon
    val hasIcon = iconUri != null || iconPath != null
    
    // icon
    val canFetchFavicon = !websiteUrl.isNullOrBlank() && 
        (websiteUrl.contains(".") || websiteUrl.startsWith("http"))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Iconpreview
        Surface(
            modifier = Modifier
                .size(72.dp)
                .clip(MaterialTheme.shapes.medium)
                .border(
                    width = 2.dp,
                    color = if (hasIcon) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.medium
                )
                .clickable { onSelectFromGallery() },
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            when {
                iconUri != null -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(iconUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = AppStringsProvider.current().labelIcon,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                iconPath != null -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(iconPath))
                            .crossfade(true)
                            .build(),
                        contentDescription = AppStringsProvider.current().labelIcon,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Outlined.AddPhotoAlternate,
                            contentDescription = AppStringsProvider.current().selectIcon,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Text(
                text = AppStringsProvider.current().labelIcon,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = AppStringsProvider.current().clickToSelectOrUseButton,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // button
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Get iconbutton( onlywhen display)
                if (canFetchFavicon) {
                    FilledTonalButton(
                        onClick = {
                            if (!isFetchingFavicon && !websiteUrl.isNullOrBlank()) {
                                isFetchingFavicon = true
                                scope.launch {
                                    val iconPath = FaviconFetcher.fetchFavicon(context, websiteUrl)
                                    isFetchingFavicon = false
                                    if (iconPath != null) {
                                        onSelectFromLibrary(iconPath)
                                        Toast.makeText(context, AppStringsProvider.current().faviconFetchSuccess, Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, AppStringsProvider.current().faviconFetchFailed, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = !isFetchingFavicon,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        if (isFetchingFavicon) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Language,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStringsProvider.current().fetchWebsiteIcon, style = MaterialTheme.typography.labelMedium)
                    }
                }
                
                // Icon button
                FilledTonalButton(
                    onClick = { showLibraryDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Collections,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStringsProvider.current().iconLibrary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
    
    // Icon dialog
    if (showLibraryDialog) {
        IconLibraryDialog(
            onDismiss = { showLibraryDialog = false },
            onSelectIcon = { path ->
                onSelectFromLibrary(path)
                showLibraryDialog = false
            },
            onOpenAiGenerator = { showAiGeneratorDialog = true }
        )
    }
    
    // AI dialog
    if (showAiGeneratorDialog) {
        IconGeneratorDialog(
            onDismiss = { showAiGeneratorDialog = false },
            onIconGenerated = { path ->
                onSelectFromLibrary(path)
                showAiGeneratorDialog = false
            }
        )
    }
}
