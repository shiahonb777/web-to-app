@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.webtoapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.pwa.PwaAnalysisResult
import com.webtoapp.core.pwa.PwaAnalysisState
import com.webtoapp.core.pwa.PwaDataSource
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.TranslateConfig
import com.webtoapp.data.model.TranslateEngine
import com.webtoapp.data.model.TranslateLanguage
import com.webtoapp.ui.animation.CardCollapseTransition
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.components.AppNameTextField
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.IconPickerWithLibrary
import com.webtoapp.ui.components.PremiumSwitch
import com.webtoapp.ui.components.PremiumTextField
import com.webtoapp.ui.viewmodel.EditState
import com.webtoapp.ui.viewmodel.MainViewModel

@Composable
internal fun LegacyAdCapabilityWarningCard() {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "广告 SDK 尚未集成，当前广告配置不会生效。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun BasicInfoCard(
    editState: EditState,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onSelectIcon: () -> Unit,
    onSelectIconFromLibrary: (String) -> Unit = {}
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = AppStringsProvider.current().labelBasicInfo,
                style = MaterialTheme.typography.titleMedium
            )

            IconPickerWithLibrary(
                iconUri = editState.iconUri,
                iconPath = editState.savedIconPath,
                websiteUrl = if (editState.appType == AppType.WEB) editState.url else null,
                onSelectFromGallery = onSelectIcon,
                onSelectFromLibrary = onSelectIconFromLibrary
            )

            AppNameTextField(
                value = editState.name,
                onValueChange = onNameChange
            )

            when (editState.appType) {
                AppType.WEB -> {
                    PremiumTextField(
                        value = editState.url,
                        onValueChange = onUrlChange,
                        label = { Text(AppStringsProvider.current().labelUrl) },
                        placeholder = { Text("https://example.com") },
                        leadingIcon = { Icon(Icons.Outlined.Link, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        )
                    )
                }

                AppType.HTML, AppType.FRONTEND -> {
                    val htmlConfig = editState.htmlConfig
                    val fileCount = htmlConfig?.files?.size ?: 0
                    val entryFile = htmlConfig?.entryFile?.takeIf { it.isNotBlank() } ?: "index.html"
                    val isFrontend = editState.appType == AppType.FRONTEND

                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isFrontend) Icons.Outlined.Web else Icons.Outlined.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isFrontend) AppStringsProvider.current().frontendApp else AppStringsProvider.current().htmlApp,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "${AppStringsProvider.current().entryFile}: $entryFile · ${AppStringsProvider.current().totalFilesCount.replace("%d", fileCount.toString())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                AppType.IMAGE, AppType.VIDEO -> {
                    val mediaPath = editState.url
                    val isVideo = editState.appType == AppType.VIDEO
                    val fileName = mediaPath.substringAfterLast("/", AppStringsProvider.current().unknownFile)

                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isVideo) Icons.Outlined.Videocam else Icons.Outlined.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                Text(
                                    text = if (isVideo) AppStringsProvider.current().videoApp else AppStringsProvider.current().imageApp,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                AppType.WORDPRESS -> {
                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = AppStringsProvider.current().appTypeWordPress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "PHP + SQLite",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                AppType.GALLERY -> {
                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                Text(
                                    text = AppStringsProvider.current().galleryApp,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = AppStringsProvider.current().galleryMediaList,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                AppType.NODEJS_APP -> {
                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = AppStringsProvider.current().appTypeNodeJs,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Node.js Runtime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                AppType.PHP_APP, AppType.PYTHON_APP, AppType.GO_APP -> {
                    val (label, desc) = when (editState.appType) {
                        AppType.PHP_APP -> AppStringsProvider.current().appTypePhp to "PHP Runtime"
                        AppType.PYTHON_APP -> AppStringsProvider.current().appTypePython to "Python Runtime"
                        AppType.GO_APP -> AppStringsProvider.current().appTypeGo to "Go Binary"
                        else -> "" to ""
                    }
                    EnhancedElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                AppType.MULTI_WEB -> {
                    PremiumTextField(
                        value = editState.url,
                        onValueChange = onUrlChange,
                        label = { Text(AppStringsProvider.current().labelUrl) },
                        placeholder = { Text("https://example.com") },
                        leadingIcon = { Icon(Icons.Outlined.Link, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ActivationCard(
    editState: EditState,
    onEnabledChange: (Boolean) -> Unit,
    onCodesChange: (List<String>) -> Unit
) {
    var newCode by remember { mutableStateOf("") }

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (editState.activationEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Key,
                            null,
                            tint = if (editState.activationEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = AppStringsProvider.current().activationCodeVerify,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                PremiumSwitch(
                    checked = editState.activationEnabled,
                    onCheckedChange = onEnabledChange
                )
            }

            AnimatedVisibility(
                visible = editState.activationEnabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = AppStringsProvider.current().activationCodeHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PremiumTextField(
                            value = newCode,
                            onValueChange = { newCode = it },
                            placeholder = { Text(AppStringsProvider.current().inputActivationCode) },
                            singleLine = true,
                            modifier = Modifier.weight(weight = 1f, fill = true)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (newCode.isNotBlank()) {
                                    onCodesChange(editState.activationCodes + newCode)
                                    newCode = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, AppStringsProvider.current().add)
                        }
                    }

                    editState.activationCodes.forEachIndexed { index, code ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = code,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(weight = 1f, fill = true)
                            )
                            IconButton(
                                onClick = {
                                    onCodesChange(editState.activationCodes.filterIndexed { i, _ -> i != index })
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    AppStringsProvider.current().btnDelete,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PwaAnalysisSection(
    viewModel: MainViewModel,
    editState: EditState
) {
    val pwaState by viewModel.pwaAnalysisState.collectAsStateWithLifecycle()
    var showResultCard by remember { mutableStateOf(false) }

    LaunchedEffect(pwaState) {
        if (pwaState is PwaAnalysisState.Success) {
            showResultCard = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val isAnalyzing = pwaState is PwaAnalysisState.Analyzing

        FilledTonalButton(
            onClick = {
                showResultCard = false
                viewModel.analyzePwa(editState.url)
            },
            enabled = editState.url.isNotBlank() && !isAnalyzing,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(AppStringsProvider.current().pwaAnalyzing)
            } else {
                Icon(Icons.Outlined.TravelExplore, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(AppStringsProvider.current().pwaAnalyzeButton)
            }
        }

        AnimatedVisibility(
            visible = pwaState is PwaAnalysisState.Error,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val error = (pwaState as? PwaAnalysisState.Error)?.message ?: ""
            EnhancedElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${AppStringsProvider.current().pwaAnalysisFailed}: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showResultCard && pwaState is PwaAnalysisState.Success,
            enter = CardExpandTransition,
            exit = CardCollapseTransition
        ) {
            val result = (pwaState as? PwaAnalysisState.Success)?.result
            if (result != null) {
                PwaResultCard(
                    result = result,
                    onApply = {
                        viewModel.applyPwaResult(result)
                        showResultCard = false
                        viewModel.resetPwaState()
                    },
                    onDismiss = {
                        showResultCard = false
                        viewModel.resetPwaState()
                    }
                )
            }
        }
    }
}

@Composable
private fun PwaResultCard(
    result: PwaAnalysisResult,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (result.isPwa) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (result.isPwa) Icons.Filled.CheckCircle else Icons.Outlined.Info,
                        contentDescription = null,
                        tint = if (result.isPwa) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (result.isPwa) AppStringsProvider.current().pwaDetected else AppStringsProvider.current().pwaNoneDetected,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (result.isPwa) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = when (result.source) {
                    PwaDataSource.MANIFEST -> AppStringsProvider.current().pwaSourceManifest
                    PwaDataSource.META_TAGS -> AppStringsProvider.current().pwaSourceMeta
                    PwaDataSource.NONE -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            result.suggestedName?.let { name ->
                PwaInfoRow(label = AppStringsProvider.current().pwaName, value = name)
            }

            result.suggestedIconUrl?.let { url ->
                PwaInfoRow(label = AppStringsProvider.current().pwaIcon, value = url.takeLast(60))
            }

            result.suggestedThemeColor?.let { color ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${AppStringsProvider.current().pwaThemeColor}: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val parsedColor = try {
                        Color(android.graphics.Color.parseColor(color))
                    } catch (_: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(parsedColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = color,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            result.suggestedDisplay?.let { display ->
                PwaInfoRow(label = AppStringsProvider.current().pwaDisplayMode, value = display)
            }

            result.suggestedOrientation?.let { orientation ->
                PwaInfoRow(label = AppStringsProvider.current().pwaOrientation, value = orientation)
            }

            result.startUrl?.let { url ->
                PwaInfoRow(label = AppStringsProvider.current().pwaStartUrl, value = url.takeLast(80))
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(AppStringsProvider.current().pwaApplyAll)
            }
        }
    }
}

@Composable
private fun PwaInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AppThemeCard(
    selectedTheme: String,
    onThemeChange: (String) -> Unit
) {
    val themeOptions = listOf(
        "AURORA" to AppStringsProvider.current().themeAurora,
        "CYBERPUNK" to AppStringsProvider.current().themeCyberpunk,
        "SAKURA" to AppStringsProvider.current().themeSakura,
        "OCEAN" to AppStringsProvider.current().themeOcean,
        "FOREST" to AppStringsProvider.current().themeForest,
        "GALAXY" to AppStringsProvider.current().themeGalaxy,
        "VOLCANO" to AppStringsProvider.current().themeVolcano,
        "FROST" to AppStringsProvider.current().themeFrost,
        "SUNSET" to AppStringsProvider.current().themeSunset,
        "MINIMAL" to AppStringsProvider.current().themeMinimal,
        "NEON_TOKYO" to AppStringsProvider.current().themeNeonTokyo,
        "LAVENDER" to AppStringsProvider.current().themeLavender
    )

    var expanded by remember { mutableStateOf(false) }
    val selectedDisplayName = themeOptions.find { it.first == selectedTheme }?.second ?: AppStringsProvider.current().themeAurora

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Palette,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = AppStringsProvider.current().exportAppTheme,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = AppStringsProvider.current().exportAppThemeHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                PremiumTextField(
                    value = selectedDisplayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(AppStringsProvider.current().selectTheme) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    themeOptions.forEach { (themeKey, themeName) ->
                        DropdownMenuItem(
                            text = { Text(themeName) },
                            onClick = {
                                onThemeChange(themeKey)
                                expanded = false
                            },
                            leadingIcon = {
                                if (themeKey == selectedTheme) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TranslateCard(
    enabled: Boolean,
    config: TranslateConfig,
    onEnabledChange: (Boolean) -> Unit,
    onConfigChange: (TranslateConfig) -> Unit
) {
    var langExpanded by remember { mutableStateOf(false) }
    var engineExpanded by remember { mutableStateOf(false) }

    val languageOptions = TranslateLanguage.entries.toList()
    val engineOptions = TranslateEngine.entries.toList()

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Translate,
                            null,
                            tint = if (enabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = AppStringsProvider.current().autoTranslate,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                PremiumSwitch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            AnimatedVisibility(
                visible = enabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = AppStringsProvider.current().autoTranslateHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ExposedDropdownMenuBox(
                        expanded = langExpanded,
                        onExpandedChange = { langExpanded = it }
                    ) {
                        PremiumTextField(
                            value = "${config.targetLanguage.displayName} (${config.targetLanguage.code})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(AppStringsProvider.current().translateTargetLanguage) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )

                        ExposedDropdownMenu(
                            expanded = langExpanded,
                            onDismissRequest = { langExpanded = false }
                        ) {
                            languageOptions.forEach { language ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(language.displayName)
                                            Text(
                                                text = language.code,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        onConfigChange(config.copy(targetLanguage = language))
                                        langExpanded = false
                                    },
                                    leadingIcon = {
                                        if (language == config.targetLanguage) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = engineExpanded,
                        onExpandedChange = { engineExpanded = it }
                    ) {
                        PremiumTextField(
                            value = config.preferredEngine.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(AppStringsProvider.current().translateEngine) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = engineExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )

                        ExposedDropdownMenu(
                            expanded = engineExpanded,
                            onDismissRequest = { engineExpanded = false }
                        ) {
                            engineOptions.forEach { engine ->
                                DropdownMenuItem(
                                    text = { Text(engine.displayName) },
                                    onClick = {
                                        onConfigChange(config.copy(preferredEngine = engine))
                                        engineExpanded = false
                                    },
                                    leadingIcon = {
                                        if (engine == config.preferredEngine) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(AppStringsProvider.current().showTranslateButton, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = AppStringsProvider.current().showTranslateButtonHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = config.showFloatingButton,
                            onCheckedChange = { onConfigChange(config.copy(showFloatingButton = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(AppStringsProvider.current().autoTranslateOnLoad, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = AppStringsProvider.current().autoTranslateOnLoadHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = config.autoTranslateOnLoad,
                            onCheckedChange = { onConfigChange(config.copy(autoTranslateOnLoad = it)) }
                        )
                    }
                }
            }
        }
    }
}
