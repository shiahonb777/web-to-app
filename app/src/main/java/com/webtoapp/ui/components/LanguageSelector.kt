package com.webtoapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.webtoapp.R
import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.LanguageManager
import kotlinx.coroutines.launch

/**
 * 语言选择按钮（用于 TopAppBar）
 */
@Composable
fun LanguageSelectorButton(
    onLanguageChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val languageManager = remember { LanguageManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    val currentLanguage by languageManager.currentLanguageFlow.collectAsState(initial = AppLanguage.CHINESE)
    var showDialog by remember { mutableStateOf(false) }
    
    // 语言选择按钮
    IconButton(
        onClick = { showDialog = true },
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Language,
            contentDescription = stringResource(R.string.language_settings),
            modifier = Modifier.size(20.dp)
        )
    }
    
    // 语言选择对话框
    if (showDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { language ->
                scope.launch {
                    languageManager.setLanguage(language)
                    onLanguageChanged()
                }
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * 语言选择对话框
 */
@Composable
fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.language_settings),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppLanguage.entries.forEach { language ->
                    LanguageOption(
                        language = language,
                        isSelected = language == currentLanguage,
                        onClick = { onLanguageSelected(language) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

/**
 * 单个语言选项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageOption(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            if (isSelected) {
                RadioButton(
                    selected = true,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

/**
 * 首次启动语言选择屏幕
 */
@Composable
fun FirstLaunchLanguageScreen(
    onLanguageSelected: () -> Unit
) {
    val context = LocalContext.current
    val languageManager = remember { LanguageManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    var selectedLanguage by remember { mutableStateOf<AppLanguage?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 多语言欢迎文字
            Text(
                text = "Welcome / 欢迎 / مرحبا",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Select Language / 选择语言 / اختر اللغة",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 语言选项
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppLanguage.entries.forEach { language ->
                    FirstLaunchLanguageOption(
                        language = language,
                        isSelected = language == selectedLanguage,
                        onClick = { selectedLanguage = language }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Confirm按钮
            Button(
                onClick = {
                    selectedLanguage?.let { lang ->
                        scope.launch {
                            languageManager.setLanguage(lang)
                            onLanguageSelected()
                        }
                    }
                },
                enabled = selectedLanguage != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = when (selectedLanguage) {
                        AppLanguage.CHINESE -> "确认"
                        AppLanguage.ENGLISH -> "Confirm"
                        AppLanguage.ARABIC -> "تأكيد"
                        null -> "Confirm / 确认 / تأكيد"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * 首次启动语言选项卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FirstLaunchLanguageOption(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsCard(
    onLanguageChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val languageManager = remember { LanguageManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    val currentLanguage by languageManager.currentLanguageFlow.collectAsState(initial = AppLanguage.CHINESE)
    var expanded by remember { mutableStateOf(false) }
    
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.language_settings),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = currentLanguage.nativeName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    AppLanguage.entries.forEach { language ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(language.nativeName)
                                    Text(
                                        text = language.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                scope.launch {
                                    languageManager.setLanguage(language)
                                    onLanguageChanged()
                                }
                                expanded = false
                            },
                            leadingIcon = {
                                if (language == currentLanguage) {
                                    RadioButton(selected = true, onClick = null)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
