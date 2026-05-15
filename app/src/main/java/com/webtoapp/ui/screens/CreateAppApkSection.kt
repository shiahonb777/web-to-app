package com.webtoapp.ui.screens

import com.webtoapp.ui.components.PremiumFilterChip
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.*
import com.webtoapp.ui.design.*
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import com.webtoapp.util.AppConstants
import com.webtoapp.util.ConfigPresetStorage
import com.webtoapp.util.NetworkTrustStorage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


private val PACKAGE_NAME_REGEX = AppConstants.PACKAGE_NAME_REGEX


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ApkExportSection(
    config: ApkExportConfig,
    onConfigChange: (ApkExportConfig) -> Unit,
    onOpenPermissionConfig: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val packageNameBringIntoViewRequester = remember { BringIntoViewRequester() }
    val versionNameBringIntoViewRequester = remember { BringIntoViewRequester() }
    val versionCodeBringIntoViewRequester = remember { BringIntoViewRequester() }

    val context = LocalContext.current
    var caImportError by remember { mutableStateOf<String?>(null) }
    val caPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            NetworkTrustStorage.importCertificate(context, uri)
        }.onSuccess { cert ->
            val next = config.networkTrustConfig.copy(
                customCaCertificates = config.networkTrustConfig.customCaCertificates + cert
            )
            caImportError = null
            onConfigChange(config.copy(networkTrustConfig = next))
        }.onFailure { error ->
            caImportError = error.message ?: Strings.invalidCertificate
        }
    }

    val packageName = config.customPackageName ?: ""
    val isPackageNameInvalid = packageName.isNotBlank() &&
        !packageName.matches(PACKAGE_NAME_REGEX)

    Column(verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)) {

        // ── 基本信息 ──────────────────────────────────────────
        WtaSection(
            title = Strings.apkConfigNote,
            headerStyle = WtaSectionHeaderStyle.Quiet
        ) {
            WtaSettingCard {
                Column(
                    modifier = Modifier.padding(
                        horizontal = WtaSpacing.RowHorizontal,
                        vertical = WtaSpacing.ContentGap
                    ),
                    verticalArrangement = Arrangement.spacedBy(WtaSpacing.ContentGap)
                ) {
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = {
                            onConfigChange(config.copy(customPackageName = it.ifBlank { null }))
                        },
                        label = { Text(Strings.customPackageName) },
                        placeholder = { Text(Strings.apkPackageNamePlaceholder) },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(WtaSpacing.ContentGap)
                    ) {
                        OutlinedTextField(
                            value = config.customVersionName ?: "",
                            onValueChange = {
                                onConfigChange(config.copy(customVersionName = it.ifBlank { null }))
                            },
                            label = { Text(Strings.versionName) },
                            placeholder = { Text(Strings.apkVersionNamePlaceholder) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
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
                            placeholder = { Text(Strings.apkVersionCodePlaceholder) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
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
                }
            }
        }

        // ── 架构选择 ──────────────────────────────────────────
        WtaSection(
            title = Strings.apkArchitecture,
            headerStyle = WtaSectionHeaderStyle.Quiet
        ) {
            WtaSettingCard {
                Column(
                    modifier = Modifier.padding(
                        horizontal = WtaSpacing.RowHorizontal,
                        vertical = WtaSpacing.ContentGap
                    ),
                    verticalArrangement = Arrangement.spacedBy(WtaSpacing.ContentGap)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(WtaSpacing.ContentGap)
                    ) {
                        ApkArchitecture.entries.forEach { arch ->
                            val isSelected = config.architecture == arch
                            PremiumFilterChip(
                                selected = isSelected,
                                onClick = { onConfigChange(config.copy(architecture = arch)) },
                                label = { Text(arch.displayName) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    Text(
                        text = config.architecture.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── 权限配置 ──────────────────────────────────────────
        if (onOpenPermissionConfig != null) {
            PermissionSummaryCard(
                permissions = config.runtimePermissions,
                onClick = onOpenPermissionConfig
            )
        }

        // ── 网络信任 ──────────────────────────────────────────
        NetworkTrustConfigPanel(
            config = config.networkTrustConfig,
            importError = caImportError,
            onConfigChange = { networkTrustConfig ->
                onConfigChange(config.copy(networkTrustConfig = networkTrustConfig))
            },
            onImportCertificate = {
                caPickerLauncher.launch(
                    arrayOf(
                        "application/x-x509-ca-cert",
                        "application/x-pem-file",
                        "application/octet-stream",
                        "text/plain",
                        "*/*"
                    )
                )
            }
        )

        // ── 性能优化 ──────────────────────────────────────────
        PerformanceOptimizationSection(
            config = config,
            onConfigChange = onConfigChange
        )

        // ── 自定义签名 ──────────────────────────────────────────
        CustomSigningSection()
    }
}


@Composable
private fun PerformanceOptimizationSection(
    config: ApkExportConfig,
    onConfigChange: (ApkExportConfig) -> Unit
) {
    WtaSection(
        title = Strings.performanceOptimization,
        headerStyle = WtaSectionHeaderStyle.Quiet
    ) {
        WtaSettingCard {
            WtaToggleRow(
                title = Strings.performanceOptimization,
                subtitle = if (config.performanceOptimization) Strings.perfEnabled else Strings.perfDisabled,
                icon = Icons.Outlined.Speed,
                checked = config.performanceOptimization,
                onCheckedChange = { onConfigChange(config.copy(performanceOptimization = it)) }
            )
        }

        AnimatedVisibility(
            visible = config.performanceOptimization,
            enter = CardExpandTransition,
            exit = CardCollapseTransition
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)) {
                // 资源优化
                WtaSection(
                    title = Strings.perfResourceOptimize,
                    headerStyle = WtaSectionHeaderStyle.Quiet
                ) {
                    WtaSettingCard {
                        WtaToggleRow(
                            title = Strings.perfCompressImages,
                            subtitle = Strings.perfCompressImagesHint,
                            checked = config.performanceConfig.compressImages,
                            onCheckedChange = {
                                onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(compressImages = it)))
                            }
                        )
                        WtaSectionDivider()
                        WtaToggleRow(
                            title = Strings.perfConvertWebP,
                            subtitle = Strings.perfConvertWebPHint,
                            checked = config.performanceConfig.convertToWebP,
                            onCheckedChange = {
                                onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(convertToWebP = it)))
                            }
                        )
                        WtaSectionDivider()
                        WtaToggleRow(
                            title = Strings.perfMinifyCode,
                            subtitle = Strings.perfMinifyCodeHint,
                            checked = config.performanceConfig.minifyCode,
                            onCheckedChange = {
                                onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(minifyCode = it)))
                            }
                        )
                        WtaSectionDivider()
                        WtaToggleRow(
                            title = Strings.perfRemoveUnused,
                            subtitle = Strings.perfRemoveUnusedHint,
                            checked = config.performanceConfig.removeUnusedResources,
                            onCheckedChange = {
                                onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(removeUnusedResources = it)))
                            }
                        )
                    }
                }

                // 构建优化
                WtaSection(
                    title = Strings.perfBuildOptimize,
                    headerStyle = WtaSectionHeaderStyle.Quiet
                ) {
                    WtaSettingCard {
                        WtaToggleRow(
                            title = Strings.perfParallelProcessing,
                            subtitle = Strings.perfParallelProcessingHint,
                            checked = config.performanceConfig.parallelProcessing,
                            onCheckedChange = {
                                onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(parallelProcessing = it)))
                            }
                        )
                        WtaSectionDivider()
                        WtaToggleRow(
                            title = Strings.perfEnableCache,
                            subtitle = Strings.perfEnableCacheHint,
                            checked = config.performanceConfig.enableCache,
                            onCheckedChange = {
                                onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(enableCache = it)))
                            }
                        )
                    }
                }

                // 加载优化
                WtaSection(
                    title = Strings.perfLoadOptimize,
                    headerStyle = WtaSectionHeaderStyle.Quiet
                ) {
                    WtaSettingCard {
                        WtaToggleRow(
                            title = Strings.perfPreloadHints,
                            subtitle = Strings.perfPreloadHintsHint,
                            checked = config.performanceConfig.injectPreloadHints,
                            onCheckedChange = {
                                onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(injectPreloadHints = it)))
                            }
                        )
                        WtaSectionDivider()
                        WtaToggleRow(
                            title = Strings.perfLazyLoading,
                            subtitle = Strings.perfLazyLoadingHint,
                            checked = config.performanceConfig.injectLazyLoading,
                            onCheckedChange = {
                                onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(injectLazyLoading = it)))
                            }
                        )
                        WtaSectionDivider()
                        WtaToggleRow(
                            title = Strings.perfOptimizeScripts,
                            subtitle = Strings.perfOptimizeScriptsHint,
                            checked = config.performanceConfig.optimizeScripts,
                            onCheckedChange = {
                                onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(optimizeScripts = it)))
                            }
                        )
                    }
                }

                // 运行时优化
                WtaSection(
                    title = Strings.perfRuntimeOptimize,
                    headerStyle = WtaSectionHeaderStyle.Quiet
                ) {
                    WtaSettingCard {
                        WtaToggleRow(
                            title = Strings.perfRuntimeScript,
                            subtitle = Strings.perfRuntimeScriptHint,
                            checked = config.performanceConfig.injectPerformanceScript,
                            onCheckedChange = {
                                onConfigChange(config.copy(performanceConfig = config.performanceConfig.copy(injectPerformanceScript = it)))
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun NetworkTrustConfigPanel(
    config: NetworkTrustConfig,
    importError: String?,
    onConfigChange: (NetworkTrustConfig) -> Unit,
    onImportCertificate: () -> Unit
) {
    val context = LocalContext.current
    var presets by remember { mutableStateOf(ConfigPresetStorage.loadNetworkTrust(context)) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    WtaSection(
        title = Strings.networkTrustTitle,
        description = Strings.networkTrustHint,
        headerStyle = WtaSectionHeaderStyle.Quiet
    ) {
        WtaSettingCard {
            WtaToggleRow(
                title = Strings.trustSystemCa,
                subtitle = Strings.trustSystemCaHint,
                icon = Icons.Outlined.Security,
                checked = config.trustSystemCa,
                onCheckedChange = { onConfigChange(config.copy(trustSystemCa = it)) }
            )
            WtaSectionDivider()
            WtaToggleRow(
                title = Strings.trustUserCa,
                subtitle = Strings.trustUserCaHint,
                icon = Icons.Outlined.AdminPanelSettings,
                checked = config.trustUserCa,
                onCheckedChange = { onConfigChange(config.copy(trustUserCa = it)) }
            )
            WtaSectionDivider()
            WtaToggleRow(
                title = Strings.cleartextTrafficAllowed,
                subtitle = Strings.cleartextTrafficAllowedHint,
                icon = Icons.Outlined.Http,
                checked = config.cleartextTrafficPermitted,
                onCheckedChange = { onConfigChange(config.copy(cleartextTrafficPermitted = it)) }
            )
            WtaSectionDivider()
            WtaSettingRow(
                title = Strings.importCustomCa,
                subtitle = if (config.customCaCertificates.isEmpty()) {
                    Strings.importCustomCaHint
                } else {
                    Strings.importedCertificatesCount.replace("%d", config.customCaCertificates.size.toString())
                },
                icon = Icons.Outlined.UploadFile,
                onClick = onImportCertificate
            ) {
                Text(
                    text = Strings.btnImport,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            WtaSectionDivider()
            WtaSettingRow(
                title = Strings.saveNetworkPreset,
                subtitle = Strings.saveNetworkPresetHint,
                icon = Icons.Outlined.BookmarkAdd,
                onClick = {
                    presetName = ""
                    showSavePresetDialog = true
                }
            ) {
                Text(
                    text = Strings.save,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!importError.isNullOrBlank()) {
            WtaStatusBanner(
                message = importError,
                tone = WtaStatusTone.Error
            )
        }

        // 已导入的证书列表
        config.customCaCertificates.forEach { cert ->
            WtaSettingCard {
                WtaSettingRow(
                    title = cert.displayName,
                    subtitle = "${Strings.sha256Prefix} ${cert.sha256.chunked(2).take(8).joinToString(":").uppercase()}...",
                    icon = Icons.Outlined.Badge
                ) {
                    TextButton(
                        onClick = {
                            onConfigChange(
                                config.copy(
                                    customCaCertificates = config.customCaCertificates.filterNot { it.id == cert.id }
                                )
                            )
                        }
                    ) {
                        Text(Strings.btnDelete)
                    }
                }
            }
        }

        // 已保存的预设列表
        if (presets.isNotEmpty()) {
            WtaSettingCard {
                presets.forEachIndexed { index, preset ->
                    WtaSettingRow(
                        title = preset.name,
                        subtitle = Strings.applySavedNetworkPreset,
                        icon = Icons.Outlined.Inventory2,
                        onClick = { onConfigChange(preset.config) }
                    ) {
                        TextButton(
                            onClick = {
                                presets = ConfigPresetStorage.deleteNetworkTrust(context, preset.id)
                            }
                        ) {
                            Text(Strings.btnDelete)
                        }
                    }
                    if (index != presets.lastIndex) {
                        WtaSectionDivider()
                    }
                }
            }
        }

        if (config.customCaCertificates.isNotEmpty()) {
            WtaStatusBanner(
                message = Strings.networkTrustTemplateLimitHint,
                tone = WtaStatusTone.Info
            )
        }
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text(Strings.saveNetworkPresetTitle) },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text(Strings.presetName) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        presets = ConfigPresetStorage.saveNetworkTrust(context, presetName, config)
                        showSavePresetDialog = false
                    },
                    enabled = presetName.isNotBlank()
                ) {
                    Text(Strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text(Strings.btnCancel)
                }
            }
        )
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

    WtaSection(
        title = Strings.currentSigningStatus,
        headerStyle = WtaSectionHeaderStyle.Quiet
    ) {
        // 签名状态
        WtaSettingCard {
            WtaSettingRow(
                title = when (signerType) {
                    com.webtoapp.core.apkbuilder.JarSigner.SignerType.PKCS12_CUSTOM -> Strings.signingTypeCustom
                    com.webtoapp.core.apkbuilder.JarSigner.SignerType.PKCS12_AUTO -> Strings.signingTypeAutoGenerated
                    com.webtoapp.core.apkbuilder.JarSigner.SignerType.ANDROID_KEYSTORE -> Strings.signingTypeAndroidKeyStore
                },
                subtitle = certInfo,
                icon = if (signerType == com.webtoapp.core.apkbuilder.JarSigner.SignerType.PKCS12_CUSTOM)
                    Icons.Outlined.VerifiedUser else Icons.Outlined.Shield,
                subtitleMaxLines = 5
            )
        }

        // 提示信息
        WtaStatusBanner(
            title = Strings.customSigningNote,
            message = Strings.supportedKeystoreFormats,
            tone = WtaStatusTone.Info
        )

        // 操作按钮
        WtaSettingCard {
            WtaSettingRow(
                title = Strings.importKeystore,
                subtitle = null,
                icon = Icons.Outlined.FileUpload,
                onClick = { keystorePickerLauncher.launch(arrayOf("*/*")) }
            )
            WtaSectionDivider()
            WtaSettingRow(
                title = Strings.exportKeystore,
                subtitle = null,
                icon = Icons.Outlined.FileDownload,
                onClick = { keystoreExportLauncher.launch("webtoapp_signing.p12") }
            )
            if (signerType == com.webtoapp.core.apkbuilder.JarSigner.SignerType.PKCS12_CUSTOM) {
                WtaSectionDivider()
                WtaSettingRow(
                    title = Strings.removeCustomKeystore,
                    subtitle = null,
                    icon = Icons.Outlined.Delete,
                    onClick = { showRemoveConfirmDialog = true },
                    tone = WtaRowTone.Danger
                )
            }
        }

        // 操作反馈
        snackbarMessage?.let { msg ->
            WtaStatusBanner(
                message = msg,
                tone = WtaStatusTone.Success
            )
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(3000)
                snackbarMessage = null
            }
        }
    }

    // ── 对话框 ──────────────────────────────────────────

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
