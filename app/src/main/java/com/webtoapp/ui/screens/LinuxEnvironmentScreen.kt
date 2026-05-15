package com.webtoapp.ui.screens
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton

import com.webtoapp.ui.theme.AppColors
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.linux.*
import com.webtoapp.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch
import com.webtoapp.ui.design.WtaBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinuxEnvironmentScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val theme = LocalAppTheme.current


    val themeAccentColor = MaterialTheme.colorScheme.primary

    val envManager = remember { LinuxEnvironmentManager.getInstance(context) }
    val envState by envManager.state.collectAsStateWithLifecycle()
    val installProgress by envManager.installProgress.collectAsStateWithLifecycle()

    var envInfo by remember { mutableStateOf<EnvironmentInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        envManager.checkEnvironment()
        envInfo = envManager.getEnvironmentInfo()
        isLoading = false
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.buildEnvironment) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back)
                    }
                },
                actions = {
                    if (envInfo != null) {
                        IconButton(onClick = { showResetDialog = true }) {
                            Icon(Icons.Outlined.RestartAlt, Strings.btnReset)
                        }
                    }
                }
            )
        }
    ) { padding ->
        WtaBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IntroCard(themeAccentColor)

            ProjectSupportMatrixCard(themeAccentColor)

            StatusCard(envState, installProgress, themeAccentColor) {
                scope.launch {
                    envManager.initialize { _, _ -> }
                    envInfo = envManager.getEnvironmentInfo()
                }
            }

            AnimatedVisibility(visible = envInfo != null) {
                envInfo?.let { info ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        BuildToolsCard(info, themeAccentColor)
                        MoreRuntimesCard(info, themeAccentColor) {
                            scope.launch { envInfo = envManager.getEnvironmentInfo() }
                        }
                        StorageCard(info, themeAccentColor) { showClearCacheDialog = true }
                        FeaturesCard(themeAccentColor)
                        TechCard(themeAccentColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Outlined.Warning, null, tint = com.webtoapp.ui.design.WtaColors.semantic.warning) },
            title = { Text(Strings.resetEnvironment) },
            text = { Text(Strings.resetEnvConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        scope.launch {
                            envManager.reset()
                            envInfo = envManager.getEnvironmentInfo()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Error)
                ) { Text(Strings.btnReset) }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text(Strings.btnCancel) } }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            icon = { Icon(Icons.Outlined.CleaningServices, null) },
            title = { Text(Strings.clearCacheTitle) },
            text = { Text(Strings.clearCacheConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheDialog = false
                    scope.launch {
                        envManager.clearCache()
                        envInfo = envManager.getEnvironmentInfo()
                    }
                }) { Text(Strings.clean) }
            },
            dismissButton = { TextButton(onClick = { showClearCacheDialog = false }) { Text(Strings.btnCancel) } }
        )
    }
        }
}




@Composable
private fun CardContainer(
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val shape = RoundedCornerShape(theme.shapes.cardRadius)
    val bgColor = backgroundColor ?: MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
    ) {
        Column(content = content)
    }
}

@Composable
private fun StatusCard(
    state: EnvironmentState,
    progress: InstallProgress,
    themeColor: Color,
    onInstall: () -> Unit
) {
    val isReady = state is EnvironmentState.Ready
    val isInstalling = state is EnvironmentState.Downloading || state is EnvironmentState.Installing
    val isError = state is EnvironmentState.Error


    val readyColor = themeColor

    val cardColor = when {
        isReady -> readyColor.copy(alpha = 0.15f)
        isError -> MaterialTheme.colorScheme.errorContainer
        isInstalling -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    CardContainer(backgroundColor = cardColor) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isReady -> readyColor
                                isError -> MaterialTheme.colorScheme.error
                                isInstalling -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isReady -> Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        isError -> Icon(Icons.Filled.Error, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        isInstalling -> CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color.White, strokeWidth = 3.dp)
                        else -> Icon(Icons.Outlined.Build, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        text = when (state) {
                            is EnvironmentState.Ready -> Strings.envReady
                            is EnvironmentState.NotInstalled -> Strings.envNotInstalled
                            is EnvironmentState.NodeInstalledNpmMissing -> "Node 已就绪，npm 未安装"
                            is EnvironmentState.Downloading -> "${Strings.envDownloading}: ${state.component}"
                            is EnvironmentState.Installing -> "${Strings.envInstalling}: ${state.step}"
                            is EnvironmentState.Error -> Strings.envInstallFailed
                            else -> Strings.ready
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when (state) {
                            is EnvironmentState.Ready -> Strings.canBuildFrontend
                            is EnvironmentState.NotInstalled -> Strings.builtInPackagerReady
                            is EnvironmentState.NodeInstalledNpmMissing -> "继续安装 npm / pnpm / yarn 后即可进行本地构建"
                            is EnvironmentState.Downloading -> "${(state.progress * 100).toInt()}%"
                            is EnvironmentState.Installing -> "${(state.progress * 100).toInt()}%"
                            is EnvironmentState.Error -> state.message
                            else -> Strings.canBuildFrontend
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isInstalling) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    val progressValue = when (state) {
                        is EnvironmentState.Downloading -> state.progress
                        is EnvironmentState.Installing -> state.progress
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    if (progress.currentStep.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            progress.currentStep,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(visible = state is EnvironmentState.NotInstalled || state is EnvironmentState.Error) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    PremiumButton(
                        onClick = onInstall,
                        enabled = !isInstalling,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (state is EnvironmentState.Error) Icons.Default.Refresh else Icons.Default.Download, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (state is EnvironmentState.Error) Strings.reinstallEsbuild else Strings.installAdvancedBuildTool)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (state is EnvironmentState.Error) Strings.installFailedHint
                        else Strings.optionalEsbuildHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BuildToolsCard(info: EnvironmentInfo, themeColor: Color) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Build,
                        null,
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.buildTools, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            ToolRow(
                icon = Icons.Outlined.Code,
                name = "Node.js",
                status = versionStatus(info.nodeReady, info.nodeVersion),
                description = "本地脚本运行时",
                color = if (info.nodeReady) themeColor else com.webtoapp.ui.design.WtaColors.semantic.neutral,
                isAvailable = info.nodeReady,
            )

            Spacer(modifier = Modifier.height(8.dp))

            ToolRow(
                icon = Icons.Outlined.Terminal,
                name = "npm",
                status = versionStatus(info.npmReady, info.npmVersion),
                description = "依赖安装与 npm run build",
                color = if (info.npmReady) themeColor else com.webtoapp.ui.design.WtaColors.semantic.neutral,
                isAvailable = info.npmReady
            )

            Spacer(modifier = Modifier.height(8.dp))

            ToolRow(
                icon = Icons.Outlined.DeveloperMode,
                name = "pnpm",
                status = versionStatus(info.pnpmReady, info.pnpmVersion),
                description = "pnpm 项目支持",
                color = if (info.pnpmReady) themeColor else com.webtoapp.ui.design.WtaColors.semantic.neutral,
                isAvailable = info.pnpmReady
            )

            Spacer(modifier = Modifier.height(8.dp))

            ToolRow(
                icon = Icons.Outlined.Layers,
                name = "yarn",
                status = versionStatus(info.yarnReady, info.yarnVersion),
                description = "yarn 项目支持",
                color = if (info.yarnReady) themeColor else com.webtoapp.ui.design.WtaColors.semantic.neutral,
                isAvailable = info.yarnReady
            )

            Spacer(modifier = Modifier.height(8.dp))

            ToolRow(
                icon = Icons.Outlined.Speed,
                name = "esbuild",
                status = if (info.esbuildAvailable) Strings.installed else Strings.notInstalled,
                description = "静态资源优化与轻量构建加速",
                color = if (info.esbuildAvailable) themeColor else com.webtoapp.ui.design.WtaColors.semantic.neutral,
                isAvailable = info.esbuildAvailable
            )

            Spacer(modifier = Modifier.height(8.dp))

            ToolRow(
                icon = Icons.Outlined.Code,
                name = "PHP",
                status = versionStatus(info.phpReady, info.phpVersion),
                description = Strings.toolPhpDesc,
                color = if (info.phpReady) themeColor else com.webtoapp.ui.design.WtaColors.semantic.neutral,
                isAvailable = info.phpReady
            )

            Spacer(modifier = Modifier.height(8.dp))

            ToolRow(
                icon = Icons.Outlined.Inventory2,
                name = "Composer",
                status = versionStatus(info.composerReady, info.composerVersion),
                description = Strings.toolComposerDesc,
                color = if (info.composerReady) themeColor else com.webtoapp.ui.design.WtaColors.semantic.neutral,
                isAvailable = info.composerReady
            )

            Spacer(modifier = Modifier.height(8.dp))

            ToolRow(
                icon = Icons.Outlined.Terminal,
                name = "Python",
                status = versionStatus(info.pythonReady, info.pythonVersion),
                description = Strings.toolPythonDesc,
                color = if (info.pythonReady) themeColor else com.webtoapp.ui.design.WtaColors.semantic.neutral,
                isAvailable = info.pythonReady
            )

            Spacer(modifier = Modifier.height(8.dp))

            ToolRow(
                icon = Icons.Outlined.Inventory,
                name = "pip",
                status = if (info.pipReady) (Strings.installed) else Strings.notInstalled,
                description = Strings.toolPipDesc,
                color = if (info.pipReady) themeColor else com.webtoapp.ui.design.WtaColors.semantic.neutral,
                isAvailable = info.pipReady
            )
        }
    }
}

private fun versionStatus(ready: Boolean, version: String?): String {
    return if (!ready) Strings.notInstalled else version ?: Strings.installed
}

@Composable
private fun ToolRow(
    icon: ImageVector,
    name: String,
    status: String,
    description: String,
    color: Color,
    isAvailable: Boolean
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.cornerRadius * 0.6f))
            .background(color.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        status,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StorageCard(info: EnvironmentInfo, themeColor: Color, onClearCache: () -> Unit) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Storage,
                        null,
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.storageUsage, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))


            StorageRow(Strings.buildTools, formatSize(info.storageUsed))
            Spacer(modifier = Modifier.height(8.dp))
            StorageRow(Strings.cache, formatSize(info.cacheSize))

            if (info.cacheSize > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                PremiumOutlinedButton(onClick = onClearCache, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.CleaningServices, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.btnClearCache)
                }
            }
        }
    }
}

@Composable
private fun StorageRow(label: String, value: String) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.cornerRadius * 0.5f))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FeaturesCard(themeColor: Color) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        null,
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.supportedFeatures, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val features = listOf(
                Strings.featureImportBuiltProjects,
                Strings.featureAutoDetectFramework,
                Strings.featureSupportViteWebpack,
                Strings.featureTypeScriptSupport,
                Strings.featureStaticAssets,
                Strings.featureEsbuildOptional,
                Strings.featureHtmlOptimize,
                Strings.featureNodeTsPreCompile,
                Strings.featurePerfOptimize
            )

            features.forEach { text ->
                FeatureRow(text, themeColor)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun FeatureRow(text: String, themeColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(themeColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Check,
                null,
                tint = themeColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TechCard(themeColor: Color) {
    val bgColor = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)

    CardContainer(backgroundColor = bgColor) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        null,
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.techDescription, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                Strings.techDescriptionContent,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format(java.util.Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}


/**
 * 介绍卡片：开门见山告诉用户这个功能是什么、能干什么、不能干什么。
 * 之前不少用户（包括"为什么 Go 不能直接构建"的反馈）都是因为以为这是通用的
 * "本地构建一切"，所以 UI 上需要明确说明它的实际边界——这是 Node.js 工具链。
 */
@Composable
private fun IntroCard(themeColor: Color) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        null,
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.buildEnvIntroTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                Strings.buildEnvIntroBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 项目支持矩阵：把每种 App 类型在手机上是怎样被处理的做成一份清单。
 * 关键设计目标——让 Go 应用为什么必须预编译这件事不再是隐藏知识：
 *   - 前端 → 在这里 build
 *   - 静态 / Node / PHP / Python → 不需要 build，直接打包或现场解释
 *   - Go → 必须电脑端预编译，给出明确指令和原因
 */
@Composable
private fun ProjectSupportMatrixCard(themeColor: Color) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.FactCheck,
                        null,
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.projectSupportMatrixTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            ProjectSupportRow(
                name = Strings.projectFrontend,
                badge = Strings.supportLevelOnDevice,
                description = Strings.projectFrontendDesc,
                semantic = SupportSemantic.PRIMARY,
                themeColor = themeColor,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProjectSupportRow(
                name = Strings.projectStaticHtml,
                badge = Strings.supportLevelDirectPackage,
                description = Strings.projectStaticHtmlDesc,
                semantic = SupportSemantic.OK,
                themeColor = themeColor,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProjectSupportRow(
                name = Strings.projectNodeJs,
                badge = Strings.supportLevelInterpreted,
                description = Strings.projectNodeJsDesc,
                semantic = SupportSemantic.OK,
                themeColor = themeColor,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProjectSupportRow(
                name = Strings.projectPhp,
                badge = Strings.supportLevelInterpreted,
                description = Strings.projectPhpDesc,
                semantic = SupportSemantic.OK,
                themeColor = themeColor,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProjectSupportRow(
                name = Strings.projectPython,
                badge = Strings.supportLevelInterpreted,
                description = Strings.projectPythonDesc,
                semantic = SupportSemantic.OK,
                themeColor = themeColor,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProjectSupportRow(
                name = Strings.projectGo,
                badge = Strings.supportLevelNeedsPrebuild,
                description = Strings.projectGoDesc,
                semantic = SupportSemantic.WARNING,
                themeColor = themeColor,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // "为什么 Go 需要预编译"详细说明，折叠在矩阵下方
            WhyGoExpander()
        }
    }
}

private enum class SupportSemantic { PRIMARY, OK, WARNING }

@Composable
private fun ProjectSupportRow(
    name: String,
    badge: String,
    description: String,
    semantic: SupportSemantic,
    themeColor: Color,
) {
    val theme = LocalAppTheme.current
    val badgeColor = when (semantic) {
        SupportSemantic.PRIMARY -> themeColor
        SupportSemantic.OK -> com.webtoapp.ui.design.WtaColors.semantic.success
        SupportSemantic.WARNING -> com.webtoapp.ui.design.WtaColors.semantic.warning
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.cornerRadius * 0.6f))
            .background(badgeColor.copy(alpha = 0.05f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 折叠式说明：第一次看到的用户可以选择展开看技术原因，避免主屏幕信息过载。
 */
@Composable
private fun WhyGoExpander() {
    var expanded by remember { mutableStateOf(false) }
    val theme = LocalAppTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.cornerRadius * 0.5f))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.HelpOutline,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                Strings.whyGoNeedsPrebuildTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                Strings.whyGoNeedsPrebuildBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            )
        }
    }
}


/**
 * "更多运行时"卡片：让用户可选地安装 PHP / Composer / Python，
 * 这些是相对独立的下载（PHP 几十 MB / Composer 3.5 MB / Python ~15 MB），
 * 不强制下载——只在用户明确点击时才触发。
 *
 * onChanged：安装完成后用来刷新外层 envInfo
 */
@Composable
private fun MoreRuntimesCard(
    info: EnvironmentInfo,
    themeColor: Color,
    onChanged: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val envManager = remember { LinuxEnvironmentManager.getInstance(context) }

    // 三个运行时各自维护一个"正在安装中"的状态，避免它们互相阻塞 UI
    var installingPhp by remember { mutableStateOf(false) }
    var installingComposer by remember { mutableStateOf(false) }
    var installingPython by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }

    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.CloudDownload,
                        null,
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    Strings.moreRuntimesTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                Strings.moreRuntimesDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // PHP 运行时
            RuntimeInstallRow(
                title = "PHP",
                buttonLabel = if (installingPhp) Strings.runtimeInstalling else Strings.installPhpRuntime,
                ready = info.phpReady,
                enabled = !installingPhp && !info.phpReady,
                themeColor = themeColor,
                onClick = {
                    installingPhp = true
                    lastError = null
                    scope.launch {
                        val r = envManager.installPhpRuntime { _, _ -> }
                        installingPhp = false
                        if (r.isFailure) lastError = "PHP: ${r.exceptionOrNull()?.message}"
                        onChanged()
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Composer 依赖 PHP，按钮要求 PHP 必须已就绪
            RuntimeInstallRow(
                title = "Composer",
                buttonLabel = when {
                    installingComposer -> Strings.runtimeInstalling
                    !info.phpReady -> Strings.composerNeedsPhp
                    else -> Strings.installComposerLabel
                },
                ready = info.composerReady,
                enabled = !installingComposer && !info.composerReady && info.phpReady,
                themeColor = themeColor,
                onClick = {
                    installingComposer = true
                    lastError = null
                    scope.launch {
                        val r = envManager.installComposer { _, _ -> }
                        installingComposer = false
                        if (r.isFailure) lastError = "Composer: ${r.exceptionOrNull()?.message}"
                        onChanged()
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Python 运行时（pip 随包，与 Python 同生命周期）
            RuntimeInstallRow(
                title = "Python",
                buttonLabel = if (installingPython) Strings.runtimeInstalling else Strings.installPythonRuntime,
                ready = info.pythonReady,
                enabled = !installingPython && !info.pythonReady,
                themeColor = themeColor,
                onClick = {
                    installingPython = true
                    lastError = null
                    scope.launch {
                        val r = envManager.installPythonRuntime { _, _ -> }
                        installingPython = false
                        if (r.isFailure) lastError = "Python: ${r.exceptionOrNull()?.message}"
                        onChanged()
                    }
                },
            )

            lastError?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Error,
                )
            }
        }
    }
}

@Composable
private fun RuntimeInstallRow(
    title: String,
    buttonLabel: String,
    ready: Boolean,
    enabled: Boolean,
    themeColor: Color,
    onClick: () -> Unit,
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.cornerRadius * 0.5f))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                if (ready) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        null,
                        tint = com.webtoapp.ui.design.WtaColors.semantic.success,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (ready) {
            Text(
                Strings.installed,
                style = MaterialTheme.typography.bodySmall,
                color = com.webtoapp.ui.design.WtaColors.semantic.success,
            )
        } else {
            TextButton(
                onClick = onClick,
                enabled = enabled,
            ) {
                Text(buttonLabel)
            }
        }
    }
}
