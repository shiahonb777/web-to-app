package com.webtoapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.extension.ExtensionModule
import com.webtoapp.core.extension.ModuleAuthor
import com.webtoapp.core.extension.ModuleCategory
import com.webtoapp.core.extension.ModuleRunMode
import com.webtoapp.core.extension.ModuleRunTime
import com.webtoapp.core.extension.ModuleUiConfig
import com.webtoapp.core.extension.ModuleVersion
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.screens.extensionmodule.editor.dialogs.ConfigItemsDialog
import com.webtoapp.ui.screens.extensionmodule.editor.dialogs.IconPickerDialog
import com.webtoapp.ui.screens.extensionmodule.editor.dialogs.ModuleCategoryDialog
import com.webtoapp.ui.screens.extensionmodule.editor.dialogs.ModulePermissionsDialog
import com.webtoapp.ui.screens.extensionmodule.editor.dialogs.ModuleRunAtDialog
import com.webtoapp.ui.screens.extensionmodule.editor.dialogs.ModuleRunModeDialog
import com.webtoapp.ui.screens.extensionmodule.editor.dialogs.TemplateSelectionDialog
import com.webtoapp.ui.screens.extensionmodule.editor.dialogs.UiTypeSelectionDialog
import com.webtoapp.ui.screens.extensionmodule.editor.dialogs.UrlMatchDialog
import com.webtoapp.ui.screens.extensionmodule.editor.tabs.AdvancedTab
import com.webtoapp.ui.screens.extensionmodule.editor.tabs.BasicInfoTab
import com.webtoapp.ui.screens.extensionmodule.editor.tabs.CodeTab
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleEditorScreen(
    moduleId: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extensionManager: ExtensionManager = koinInject()

    val decodedModuleId = remember(moduleId) {
        moduleId?.let {
            try {
                java.net.URLDecoder.decode(it, "UTF-8")
            } catch (_: Exception) {
                it
            }
        }
    }

    val existingModule = remember(decodedModuleId) {
        decodedModuleId?.let { id ->
            try {
                extensionManager.getAllModules().find { it.id == id }
            } catch (e: Exception) {
                AppLogger.e("ModuleEditorScreen", "Failed to load module: $id", e)
                null
            }
        }
    }

    var name by remember { mutableStateOf(existingModule?.name ?: "") }
    var description by remember { mutableStateOf(existingModule?.description ?: "") }
    var icon by remember { mutableStateOf(existingModule?.icon ?: "extension") }
    var category by remember { mutableStateOf(existingModule?.category ?: ModuleCategory.OTHER) }
    var tags by remember { mutableStateOf(existingModule?.tags?.joinToString(", ") ?: "") }
    var code by remember { mutableStateOf(existingModule?.code ?: "") }
    var cssCode by remember { mutableStateOf(existingModule?.cssCode ?: "") }
    var runAt by remember { mutableStateOf(existingModule?.runAt ?: ModuleRunTime.DOCUMENT_END) }
    var urlMatches by remember { mutableStateOf(existingModule?.urlMatches ?: emptyList()) }
    var permissions by remember { mutableStateOf(existingModule?.permissions?.toSet() ?: emptySet()) }
    var configItems by remember { mutableStateOf(existingModule?.configItems ?: emptyList()) }
    var versionName by remember { mutableStateOf(existingModule?.version?.name ?: "1.0.0") }
    var authorName by remember { mutableStateOf(existingModule?.author?.name ?: "") }
    var uiConfig by remember { mutableStateOf(existingModule?.uiConfig ?: ModuleUiConfig()) }
    var runMode by remember { mutableStateOf(existingModule?.runMode ?: ModuleRunMode.INTERACTIVE) }

    var currentTab by remember { mutableIntStateOf(0) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showRunAtDialog by remember { mutableStateOf(false) }
    var showRunModeDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showUrlMatchDialog by remember { mutableStateOf(false) }
    var showConfigItemDialog by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showUiTypeDialog by remember { mutableStateOf(false) }

    val tabs = listOf(Strings.basicInfo, Strings.code, Strings.advancedSettings)
    val tabIcons = listOf(
        Icons.Outlined.Info to Icons.Filled.Info,
        Icons.Outlined.Code to Icons.Filled.Code,
        Icons.Outlined.Settings to Icons.Filled.Settings
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (moduleId == null) Strings.createModule else Strings.editModule) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = Strings.close)
                    }
                },
                actions = {
                    if (moduleId == null) {
                        IconButton(onClick = { showTemplateDialog = true }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = Strings.useTemplate)
                        }
                    }
                    TextButton(
                        onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, Strings.pleaseEnterModuleName, Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            if (code.isBlank() && cssCode.isBlank()) {
                                Toast.makeText(context, Strings.pleaseEnterCodeContent, Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }

                            val module = ExtensionModule(
                                id = existingModule?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name,
                                description = description,
                                icon = icon,
                                category = category,
                                tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                version = ModuleVersion(
                                    code = (existingModule?.version?.code ?: 0) + 1,
                                    name = versionName
                                ),
                                author = if (authorName.isNotBlank()) ModuleAuthor(authorName) else null,
                                code = code,
                                cssCode = cssCode,
                                runAt = runAt,
                                urlMatches = urlMatches,
                                permissions = permissions.toList(),
                                configItems = configItems,
                                enabled = existingModule?.enabled ?: true,
                                builtIn = false,
                                createdAt = existingModule?.createdAt ?: System.currentTimeMillis(),
                                uiConfig = uiConfig,
                                runMode = runMode
                            )

                            scope.launch {
                                extensionManager.addModule(module).onSuccess {
                                    Toast.makeText(context, Strings.saveSuccess, Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }.onFailure { e ->
                                    Toast.makeText(context, "${Strings.saveFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text(Strings.save)
                    }
                }
            )
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = currentTab == index
                            Surface(
                                onClick = { currentTab = index },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isSelected) 1.dp else 0.dp,
                                modifier = Modifier.weight(weight = 1f, fill = true)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        if (isSelected) tabIcons[index].second else tabIcons[index].first,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        }
                                    )
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                when (currentTab) {
                    0 -> BasicInfoTab(
                        name = name,
                        onNameChange = { name = it },
                        description = description,
                        onDescriptionChange = { description = it },
                        icon = icon,
                        onIconClick = { showIconPicker = true },
                        category = category,
                        onCategoryClick = { showCategoryDialog = true },
                        tags = tags,
                        onTagsChange = { tags = it },
                        versionName = versionName,
                        onVersionNameChange = { versionName = it },
                        authorName = authorName,
                        onAuthorNameChange = { authorName = it }
                    )

                    1 -> CodeTab(
                        code = code,
                        onCodeChange = { code = it },
                        cssCode = cssCode,
                        onCssCodeChange = { cssCode = it }
                    )

                    2 -> AdvancedTab(
                        runAt = runAt,
                        onRunAtClick = { showRunAtDialog = true },
                        runMode = runMode,
                        onRunModeClick = { showRunModeDialog = true },
                        permissions = permissions,
                        onPermissionsClick = { showPermissionsDialog = true },
                        urlMatches = urlMatches,
                        onUrlMatchesClick = { showUrlMatchDialog = true },
                        configItems = configItems,
                        onConfigItemsClick = { showConfigItemDialog = true },
                        uiConfig = uiConfig,
                        onUiTypeClick = { showUiTypeDialog = true }
                    )
                }
            }

            if (showCategoryDialog) {
                ModuleCategoryDialog(
                    selectedCategory = category,
                    onCategorySelected = {
                        category = it
                        showCategoryDialog = false
                    },
                    onDismiss = { showCategoryDialog = false }
                )
            }

            if (showRunAtDialog) {
                ModuleRunAtDialog(
                    selectedRunAt = runAt,
                    onRunAtSelected = {
                        runAt = it
                        showRunAtDialog = false
                    },
                    onDismiss = { showRunAtDialog = false }
                )
            }

            if (showRunModeDialog) {
                ModuleRunModeDialog(
                    selectedRunMode = runMode,
                    onRunModeSelected = {
                        runMode = it
                        showRunModeDialog = false
                    },
                    onDismiss = { showRunModeDialog = false }
                )
            }

            if (showPermissionsDialog) {
                ModulePermissionsDialog(
                    permissions = permissions,
                    onPermissionsChange = { permissions = it },
                    onDismiss = { showPermissionsDialog = false }
                )
            }

            if (showIconPicker) {
                IconPickerDialog(
                    currentIcon = icon,
                    onIconSelected = {
                        icon = it
                        showIconPicker = false
                    },
                    onDismiss = { showIconPicker = false }
                )
            }

            if (showUrlMatchDialog) {
                UrlMatchDialog(
                    urlMatches = urlMatches,
                    onUrlMatchesChange = { urlMatches = it },
                    onDismiss = { showUrlMatchDialog = false }
                )
            }

            if (showConfigItemDialog) {
                ConfigItemsDialog(
                    configItems = configItems,
                    onConfigItemsChange = { configItems = it },
                    onDismiss = { showConfigItemDialog = false }
                )
            }

            if (showTemplateDialog) {
                TemplateSelectionDialog(
                    onTemplateSelected = { template ->
                        name = template.name
                        description = template.description
                        icon = template.icon
                        category = template.category
                        code = template.code
                        cssCode = template.cssCode
                        configItems = template.configItems
                        showTemplateDialog = false
                    },
                    onDismiss = { showTemplateDialog = false }
                )
            }

            if (showUiTypeDialog) {
                UiTypeSelectionDialog(
                    currentUiConfig = uiConfig,
                    onUiConfigChange = { uiConfig = it },
                    onDismiss = { showUiTypeDialog = false }
                )
            }
        }
    }
}
