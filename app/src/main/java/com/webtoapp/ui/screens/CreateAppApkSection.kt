package com.webtoapp.ui.screens

import com.webtoapp.ui.theme.AppColors
import com.webtoapp.ui.components.PremiumOutlinedButton
import com.webtoapp.ui.components.PremiumFilterChip
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.*
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import com.webtoapp.util.AppConstants
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

private val PACKAGE_NAME_REGEX = AppConstants.PACKAGE_NAME_REGEX
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ApkExportSection(
    config: ApkExportConfig,
    onConfigChange: (ApkExportConfig) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val packageNameBringIntoViewRequester = remember { BringIntoViewRequester() }
    val versionNameBringIntoViewRequester = remember { BringIntoViewRequester() }
    val versionCodeBringIntoViewRequester = remember { BringIntoViewRequester() }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Android,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Strings.apkExportConfig,
                style = MaterialTheme.typography.titleSmall
            )
        }
        
        Text(
            text = Strings.apkConfigNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        
        val packageName = config.customPackageName ?: ""
        val isPackageNameInvalid = packageName.isNotBlank() && 
            !packageName.matches(PACKAGE_NAME_REGEX)
        
        OutlinedTextField(
            value = packageName,
            onValueChange = { 
                onConfigChange(config.copy(customPackageName = it.ifBlank { null }))
            },
            label = { Text(Strings.customPackageName) },
            placeholder = { Text("com.example.myapp") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(packageNameBringIntoViewRequester)
                .onFocusEvent { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            packageNameBringIntoViewRequester.bringIntoView()
                        }
                    }
                },
            isError = isPackageNameInvalid,
            supportingText = { 
                if (isPackageNameInvalid) {
                    Text(
                        Strings.packageNameInvalidFormat,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(Strings.packageNameHint)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = config.customVersionName ?: "",
                onValueChange = { 
                    onConfigChange(config.copy(customVersionName = it.ifBlank { null }))
                },
                label = { Text(Strings.versionName) },
                placeholder = { Text("1.0.0") },
                singleLine = true,
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .bringIntoViewRequester(versionNameBringIntoViewRequester)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                versionNameBringIntoViewRequester.bringIntoView()
                            }
                        }
                    }
            )
            
            OutlinedTextField(
                value = config.customVersionCode?.toString() ?: "",
                onValueChange = { input ->
                    val code = input.filter { it.isDigit() }.toIntOrNull()
                    onConfigChange(config.copy(customVersionCode = code))
                },
                label = { Text(Strings.versionCode) },
                placeholder = { Text("1") },
                singleLine = true,
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .bringIntoViewRequester(versionCodeBringIntoViewRequester)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                versionCodeBringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = Strings.apkArchitecture,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ApkArchitecture.entries.forEach { arch ->
                val isSelected = config.architecture == arch
                PremiumFilterChip(
                    selected = isSelected,
                    onClick = { onConfigChange(config.copy(architecture = arch)) },
                    label = { Text(arch.displayName) },
                    modifier = Modifier.weight(weight = 1f, fill = true),
                )
            }
        }
        
        Text(
            text = config.architecture.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Speed,
                null,
                tint = AppColors.Success,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Strings.performanceOptimization,
                style = MaterialTheme.typography.titleSmall
            )
        }
        
        Text(
            text = Strings.performanceOptimizationDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        
        SettingsSwitch(
            title = Strings.performanceOptimization,
            subtitle = if (config.performanceOptimization) Strings.perfEnabled else Strings.perfDisabled,
            checked = config.performanceOptimization,
            onCheckedChange = { onConfigChange(config.copy(performanceOptimization = it)) }
        )
        
        AnimatedVisibility(
            visible = config.performanceOptimization,
            enter = CardExpandTransition,
            exit = CardCollapseTransition
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                Text(
                    text = Strings.perfResourceOptimize,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.Success,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                SettingsSwitch(
                    title = Strings.perfCompressImages,
                    subtitle = Strings.perfCompressImagesHint,
                    checked = config.performanceConfig.compressImages,
                    onCheckedChange = { 
                        onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(compressImages = it)))
                    }
                )
                SettingsSwitch(
                    title = Strings.perfConvertWebP,
                    subtitle = Strings.perfConvertWebPHint,
                    checked = config.performanceConfig.convertToWebP,
                    onCheckedChange = {
                        onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(convertToWebP = it)))
                    }
                )
                SettingsSwitch(
                    title = Strings.perfMinifyCode,
                    subtitle = Strings.perfMinifyCodeHint,
                    checked = config.performanceConfig.minifyCode,
                    onCheckedChange = {
                        onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(minifyCode = it)))
                    }
                )
                SettingsSwitch(
                    title = Strings.perfRemoveUnused,
                    subtitle = Strings.perfRemoveUnusedHint,
                    checked = config.performanceConfig.removeUnusedResources,
                    onCheckedChange = {
                        onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(removeUnusedResources = it)))
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = Strings.perfBuildOptimize,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                SettingsSwitch(
                    title = Strings.perfParallelProcessing,
                    subtitle = Strings.perfParallelProcessingHint,
                    checked = config.performanceConfig.parallelProcessing,
                    onCheckedChange = {
                        onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(parallelProcessing = it)))
                    }
                )
                SettingsSwitch(
                    title = Strings.perfEnableCache,
                    subtitle = Strings.perfEnableCacheHint,
                    checked = config.performanceConfig.enableCache,
                    onCheckedChange = {
                        onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(enableCache = it)))
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = Strings.perfLoadOptimize,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.Warning,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                SettingsSwitch(
                    title = Strings.perfPreloadHints,
                    subtitle = Strings.perfPreloadHintsHint,
                    checked = config.performanceConfig.injectPreloadHints,
                    onCheckedChange = {
                        onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(injectPreloadHints = it)))
                    }
                )
                SettingsSwitch(
                    title = Strings.perfLazyLoading,
                    subtitle = Strings.perfLazyLoadingHint,
                    checked = config.performanceConfig.injectLazyLoading,
                    onCheckedChange = {
                        onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(injectLazyLoading = it)))
                    }
                )
                SettingsSwitch(
                    title = Strings.perfOptimizeScripts,
                    subtitle = Strings.perfOptimizeScriptsHint,
                    checked = config.performanceConfig.optimizeScripts,
                    onCheckedChange = {
                        onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(optimizeScripts = it)))
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = Strings.perfRuntimeOptimize,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF9C27B0),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                SettingsSwitch(
                    title = Strings.perfRuntimeScript,
                    subtitle = Strings.perfRuntimeScriptHint,
                    checked = config.performanceConfig.injectPerformanceScript,
                    onCheckedChange = {
                        onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(injectPerformanceScript = it)))
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        
        CustomSigningSection()
        
    }
}


@Composable
fun CustomSigningSection() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val signer = remember { com.webtoapp.core.apkbuilder.JarSigner(context) }
    
    var signerType by remember { mutableStateOf(signer.getSignerType()) }
    var certInfo by remember { mutableStateOf(signer.getCertificateInfo()) }
    
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var showRemoveConfirmDialog by remember { mutableStateOf(false) }
    var pendingKeystoreUri by remember { mutableStateOf<Uri?>(null) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    
    val keystorePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            pendingKeystoreUri = it
            passwordInput = ""
            importError = null
            showImportPasswordDialog = true
        }
    }
    
    val keystoreExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-pkcs12")
    ) { uri: Uri? ->
        uri?.let {
            pendingKeystoreUri = it
            passwordInput = ""
            showExportPasswordDialog = true
        }
    }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Key,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Strings.customSigning,
                style = MaterialTheme.typography.titleSmall
            )
        }
        
        Text(
            text = Strings.customSigningDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        
        Surface(
            color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (signerType == com.webtoapp.core.apkbuilder.JarSigner.SignerType.PKCS12_CUSTOM)
                            Icons.Outlined.VerifiedUser else Icons.Outlined.Shield,
                        null,
                        tint = if (signerType == com.webtoapp.core.apkbuilder.JarSigner.SignerType.PKCS12_CUSTOM)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings.currentSigningStatus,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (signerType) {
                        com.webtoapp.core.apkbuilder.JarSigner.SignerType.PKCS12_CUSTOM -> Strings.signingTypeCustom
                        com.webtoapp.core.apkbuilder.JarSigner.SignerType.PKCS12_AUTO -> Strings.signingTypeAutoGenerated
                        com.webtoapp.core.apkbuilder.JarSigner.SignerType.ANDROID_KEYSTORE -> Strings.signingTypeAndroidKeyStore
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (signerType == com.webtoapp.core.apkbuilder.JarSigner.SignerType.PKCS12_CUSTOM)
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (certInfo != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = certInfo!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Outlined.Info,
                    null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(14.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = Strings.customSigningNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = Strings.supportedKeystoreFormats,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PremiumOutlinedButton(
                onClick = {
                    keystorePickerLauncher.launch(arrayOf("*/*"))
                },
                modifier = Modifier.weight(weight = 1f, fill = true),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Outlined.FileUpload,
                    null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(Strings.importKeystore, style = MaterialTheme.typography.labelMedium)
            }
            
            PremiumOutlinedButton(
                onClick = {
                    keystoreExportLauncher.launch("webtoapp_signing.p12")
                },
                modifier = Modifier.weight(weight = 1f, fill = true),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Outlined.FileDownload,
                    null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(Strings.exportKeystore, style = MaterialTheme.typography.labelMedium)
            }
        }
        
        if (signerType == com.webtoapp.core.apkbuilder.JarSigner.SignerType.PKCS12_CUSTOM) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { showRemoveConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(Strings.removeCustomKeystore)
            }
        }
        
        snackbarMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(3000)
                snackbarMessage = null
            }
        }
    }
    
    if (showImportPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showImportPasswordDialog = false
                pendingKeystoreUri = null
                passwordInput = ""
                importError = null
            },
            icon = { Icon(Icons.Outlined.Key, null) },
            title = { Text(Strings.importKeystore) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = Strings.keystorePasswordHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { 
                            passwordInput = it
                            importError = null
                        },
                        label = { Text(Strings.keystorePassword) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) 
                            androidx.compose.ui.text.input.VisualTransformation.None 
                        else 
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = importError != null,
                        supportingText = importError?.let { error ->
                            { Text(error, color = MaterialTheme.colorScheme.error) }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingKeystoreUri ?: return@TextButton
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val tempFile = java.io.File(context.cacheDir, "import_keystore_temp")
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    tempFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                
                                val success = signer.importKeystore(tempFile, passwordInput)
                                tempFile.delete()
                                
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    if (success) {
                                        signerType = signer.getSignerType()
                                        certInfo = signer.getCertificateInfo()
                                        showImportPasswordDialog = false
                                        pendingKeystoreUri = null
                                        passwordInput = ""
                                        importError = null
                                        snackbarMessage = Strings.keystoreImportSuccess
                                    } else {
                                        importError = Strings.keystoreImportFailed
                                    }
                                }
                            } catch (e: Exception) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    importError = Strings.keystoreImportFailed
                                }
                            }
                        }
                    },
                    enabled = passwordInput.isNotEmpty()
                ) {
                    Text(Strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showImportPasswordDialog = false
                    pendingKeystoreUri = null
                    passwordInput = ""
                    importError = null
                }) {
                    Text(Strings.cancel)
                }
            }
        )
    }
    
    if (showExportPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showExportPasswordDialog = false
                pendingKeystoreUri = null
                passwordInput = ""
            },
            icon = { Icon(Icons.Outlined.FileDownload, null) },
            title = { Text(Strings.exportKeystore) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = Strings.exportPasswordHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(Strings.exportPassword) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) 
                            androidx.compose.ui.text.input.VisualTransformation.None 
                        else 
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingKeystoreUri ?: return@TextButton
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val tempFile = java.io.File(context.cacheDir, "export_keystore_temp.p12")
                                val success = signer.exportPkcs12(tempFile, passwordInput)
                                
                                if (success) {
                                    context.contentResolver.openOutputStream(uri)?.use { output ->
                                        tempFile.inputStream().use { input ->
                                            input.copyTo(output)
                                        }
                                    }
                                    tempFile.delete()
                                }
                                
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    showExportPasswordDialog = false
                                    pendingKeystoreUri = null
                                    passwordInput = ""
                                    snackbarMessage = if (success) Strings.keystoreExportSuccess else Strings.keystoreExportFailed
                                }
                            } catch (e: Exception) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    showExportPasswordDialog = false
                                    snackbarMessage = Strings.keystoreExportFailed
                                }
                            }
                        }
                    },
                    enabled = passwordInput.isNotEmpty()
                ) {
                    Text(Strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showExportPasswordDialog = false
                    pendingKeystoreUri = null
                    passwordInput = ""
                }) {
                    Text(Strings.cancel)
                }
            }
        )
    }
    
    if (showRemoveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmDialog = false },
            icon = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(Strings.removeCustomKeystore) },
            text = { Text(Strings.keystoreRemoveConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val success = signer.removeCustomPkcs12()
                        if (success) {
                            signerType = signer.getSignerType()
                            certInfo = signer.getCertificateInfo()
                            snackbarMessage = Strings.keystoreRemoveSuccess
                        }
                        showRemoveConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(Strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmDialog = false }) {
                    Text(Strings.cancel)
                }
            }
        )
    }
}
