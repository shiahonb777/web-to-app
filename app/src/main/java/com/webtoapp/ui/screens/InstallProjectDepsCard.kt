package com.webtoapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.linux.LinuxEnvironmentManager
import com.webtoapp.core.linux.LocalBuildEnvironment
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.theme.LocalAppTheme
import kotlinx.coroutines.launch
import java.io.File

/**
 * 在创建 PHP / Python / Node 应用屏幕里复用的「在 App 内安装依赖」卡片。
 *
 * 设计要点：
 * - 共用一个 Composable 处理三条线，通过 [DepsKind] 区分要装什么
 * - 检测对应运行时（PHP/Composer 或 Python 或 Node）是否就绪；未就绪时按钮不可点，给出明确指引
 * - 安装过程实时滚动日志（pip / composer / npm 的 stdout / stderr），失败时保留日志方便排查
 * - 安装完成后状态保持，避免用户疑惑"按了没反应"
 */
enum class DepsKind { PHP, PYTHON, NODE }

@Composable
fun InstallProjectDepsCard(
    kind: DepsKind,
    projectDir: String?,
    accentColor: Color,
    onOpenBuildEnvScreen: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val envManager = remember { LinuxEnvironmentManager.getInstance(context) }

    // 运行时就绪状态（每次重组都查询一次，因为用户可能在另一个屏幕装了运行时再回来）
    var phpReady by remember { mutableStateOf(false) }
    var composerReady by remember { mutableStateOf(false) }
    var pythonReady by remember { mutableStateOf(false) }
    var nodeReady by remember { mutableStateOf(false) }

    LaunchedEffect(kind) {
        when (kind) {
            DepsKind.PHP -> {
                phpReady = LocalBuildEnvironment.isPhpReady(context)
                composerReady = LocalBuildEnvironment.isComposerReady(context)
            }
            DepsKind.PYTHON -> {
                pythonReady = LocalBuildEnvironment.isPythonReady(context)
            }
            DepsKind.NODE -> {
                // Node 要装 npm 才能 install；npmReady 已经隐含 nodeReady
                nodeReady = LocalBuildEnvironment.isNpmReady(context)
            }
        }
    }

    var installing by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf<Boolean?>(null) }
    val logs = remember { mutableStateListOf<String>() }
    var logsExpanded by remember { mutableStateOf(false) }

    val theme = LocalAppTheme.current
    val shape = RoundedCornerShape(theme.shapes.cardRadius)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.CloudDownload,
                        null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    Strings.installDepsInAppTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                Strings.installDepsInAppDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 运行时就绪检查 - 未就绪时显示提示 + 跳转按钮
            val notReadyMessage = when (kind) {
                DepsKind.PHP -> when {
                    !phpReady -> Strings.phpRuntimeNotReady
                    !composerReady -> Strings.composerNotReady
                    else -> null
                }
                DepsKind.PYTHON -> if (!pythonReady) Strings.pythonRuntimeNotReady else null
                DepsKind.NODE -> if (!nodeReady) Strings.nodeRuntimeNotReady else null
            }

            if (notReadyMessage != null) {
                NotReadyHint(message = notReadyMessage, onClickOpen = onOpenBuildEnvScreen)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 主操作按钮
            val canRun = notReadyMessage == null && projectDir != null && !installing
            val buttonLabel = when {
                installing -> Strings.depsInstalling
                kind == DepsKind.PHP -> Strings.runComposerInstall
                kind == DepsKind.PYTHON -> Strings.runPipInstall
                else -> Strings.runNpmInstall
            }

            PremiumButton(
                onClick = {
                    val dirPath = projectDir
                    if (dirPath == null) {
                        success = false
                        logs.clear()
                        logs.add(Strings.noProjectSelected)
                        return@PremiumButton
                    }
                    installing = true
                    success = null
                    logs.clear()
                    val command = when (kind) {
                        DepsKind.PHP -> "$ php composer.phar install"
                        DepsKind.PYTHON -> "$ pip install -r requirements.txt"
                        DepsKind.NODE -> "$ npm install"
                    }
                    logs.add(command)
                    scope.launch {
                        val result = when (kind) {
                            DepsKind.PHP -> envManager.installPhpProjectDependencies(File(dirPath)) { line ->
                                appendLogLine(logs, line)
                            }
                            DepsKind.PYTHON -> envManager.installPythonProjectDependencies(File(dirPath)) { line ->
                                appendLogLine(logs, line)
                            }
                            DepsKind.NODE -> envManager.installNodeProjectDependencies(File(dirPath)) { line ->
                                appendLogLine(logs, line)
                            }
                        }
                        installing = false
                        success = result.isSuccess
                        if (result.isFailure) {
                            appendLogLine(logs, "[error] ${result.exceptionOrNull()?.message.orEmpty()}")
                            // 失败时自动展开日志，方便用户立即看到原因
                            logsExpanded = true
                        }
                    }
                },
                enabled = canRun,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (installing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(buttonLabel)
            }

            // 结果状态指示
            success?.let { ok ->
                Spacer(modifier = Modifier.height(12.dp))
                StatusChip(ok)
            }

            // 日志展开面板
            if (logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LogsPanel(
                    logs = logs,
                    expanded = logsExpanded,
                    onToggle = { logsExpanded = !logsExpanded },
                )
            }
        }
    }
}

/**
 * 限制日志缓冲到 500 行，防止超长依赖输出（pip 编译信息常常几千行）撑爆内存
 */
private fun appendLogLine(logs: MutableList<String>, line: String) {
    if (line.isBlank()) return
    if (logs.size >= 500) {
        logs.removeAt(0)
    }
    logs.add(line)
}

@Composable
private fun NotReadyHint(message: String, onClickOpen: () -> Unit) {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.cornerRadius * 0.5f))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Error,
                null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        TextButton(onClick = onClickOpen) {
            Text(Strings.openBuildEnvScreen)
        }
    }
}

@Composable
private fun StatusChip(success: Boolean) {
    val color = if (success) com.webtoapp.ui.design.WtaColors.semantic.success
        else com.webtoapp.ui.design.WtaColors.semantic.warning
    val icon = if (success) Icons.Filled.CheckCircle else Icons.Filled.Error
    val label = if (success) Strings.depsInstallSuccess else Strings.depsInstallFailed
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LogsPanel(
    logs: List<String>,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.cornerRadius * 0.5f))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${Strings.viewLogs} (${logs.size})",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .background(Color.Black.copy(alpha = 0.05f))
                    .padding(12.dp)
            ) {
                val scroll = rememberScrollState()
                // 自动滚动到底部，让用户看到最新输出
                LaunchedEffect(logs.size) {
                    scroll.animateScrollTo(scroll.maxValue)
                }
                Column(modifier = Modifier.verticalScroll(scroll)) {
                    logs.forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
