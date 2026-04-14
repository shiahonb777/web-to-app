package com.webtoapp.ui.webview

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.webtoapp.core.golang.GoRuntime
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.nodejs.NodeRuntime
import com.webtoapp.core.php.PhpAppRuntime
import com.webtoapp.core.python.PythonRuntime
import com.webtoapp.core.webview.LocalHttpServer
import com.webtoapp.core.wordpress.WordPressDependencyManager
import com.webtoapp.core.wordpress.WordPressManager
import com.webtoapp.core.wordpress.WordPressPhpRuntime
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.model.WordPressConfig
import com.webtoapp.data.repository.WebAppRepository
import java.io.File
import kotlinx.coroutines.delay

@Composable
internal fun WordPressPreviewCoordinator(
    webApp: WebApp?,
    isActivated: Boolean,
    isActivationChecked: Boolean,
    retryTrigger: Int,
    context: Context,
    repository: WebAppRepository,
    phpRuntime: WordPressPhpRuntime,
    onWebAppChanged: (WebApp) -> Unit,
    onStateChanged: (WordPressPreviewState) -> Unit,
    loadUrl: (String) -> Unit
) {
    LaunchedEffect(webApp, isActivated, isActivationChecked, retryTrigger) {
        val app = webApp ?: return@LaunchedEffect
        if (app.appType != AppType.WORDPRESS) return@LaunchedEffect
        if (!isActivated || !isActivationChecked) return@LaunchedEffect

        onStateChanged(WordPressPreviewState.CheckingDeps)

        if (!WordPressDependencyManager.isAllReady(context)) {
            onStateChanged(WordPressPreviewState.Downloading)
            val success = WordPressDependencyManager.downloadAllDependencies(context)
            if (!success) {
                onStateChanged(WordPressPreviewState.Error(AppStringsProvider.current().wpDownloadFailed))
                return@LaunchedEffect
            }
        }

        var projectId = app.wordpressConfig?.projectId ?: ""
        val projectDir = if (projectId.isNotEmpty()) {
            WordPressManager.getProjectDir(context, projectId)
        } else {
            null
        }

        if (projectDir == null || !projectDir.exists() || !File(projectDir, "wp-includes/version.php").exists()) {
            onStateChanged(WordPressPreviewState.CreatingProject)
            val newId = WordPressManager.createProject(
                context = context,
                siteTitle = app.wordpressConfig?.siteTitle ?: "My Site",
                adminUser = app.wordpressConfig?.adminUser ?: "admin",
                adminEmail = app.wordpressConfig?.adminEmail ?: ""
            )
            if (newId == null) {
                onStateChanged(WordPressPreviewState.Error(AppStringsProvider.current().wpProjectCreateFailed))
                return@LaunchedEffect
            }
            projectId = newId
            val updatedConfig = (app.wordpressConfig ?: WordPressConfig()).copy(projectId = newId)
            val updatedApp = app.copy(wordpressConfig = updatedConfig)
            repository.updateWebApp(updatedApp)
            onWebAppChanged(updatedApp)
        }

        onStateChanged(WordPressPreviewState.StartingServer)
        val wpDir = WordPressManager.getProjectDir(context, projectId)
        WordPressManager.ensureDbPhpExists(context, wpDir)
        val port = phpRuntime.startServer(wpDir.absolutePath, app.wordpressConfig?.phpPort ?: 0)

        if (port > 0) {
            val url = "http://127.0.0.1:$port/"
            WordPressManager.autoInstallIfNeeded(
                baseUrl = "http://127.0.0.1:$port",
                siteTitle = app.wordpressConfig?.siteTitle?.takeIf { it.isNotBlank() } ?: "My Site",
                adminUser = app.wordpressConfig?.adminUser?.takeIf { it.isNotBlank() } ?: "admin",
                adminEmail = app.wordpressConfig?.adminEmail?.takeIf { it.isNotBlank() } ?: "admin@localhost.local"
            )
            onStateChanged(WordPressPreviewState.Ready(url))
            delay(200)
            loadUrl(url)
        } else {
            onStateChanged(WordPressPreviewState.Error(AppStringsProvider.current().wpServerError))
        }
    }

    DisposableEffect(phpRuntime) {
        onDispose {
            phpRuntime.stopServer()
        }
    }
}

@Composable
internal fun PhpAppPreviewCoordinator(
    appId: Long,
    webApp: WebApp?,
    isActivated: Boolean,
    isActivationChecked: Boolean,
    retryTrigger: Int,
    context: Context,
    phpAppRuntime: PhpAppRuntime,
    onStateChanged: (PhpAppPreviewState) -> Unit,
    loadUrl: (String) -> Unit
) {
    LaunchedEffect(webApp, isActivated, isActivationChecked, retryTrigger) {
        val app = webApp ?: return@LaunchedEffect
        if (app.appType != AppType.PHP_APP) return@LaunchedEffect
        if (!isActivated || !isActivationChecked) return@LaunchedEffect

        AppLogger.i("PhpAppPreview", "开始 PHP 应用预览流程, appId=$appId, phpAppConfig=${app.phpAppConfig}")

        val config = app.phpAppConfig
        if (config == null) {
            AppLogger.e("PhpAppPreview", "phpAppConfig 为 null，无法启动预览")
            onStateChanged(PhpAppPreviewState.Error(AppStringsProvider.current().phpAppProjectNotFound))
            return@LaunchedEffect
        }

        onStateChanged(PhpAppPreviewState.CheckingDeps)
        AppLogger.i("PhpAppPreview", "检查 PHP 依赖, isPhpReady=${WordPressDependencyManager.isPhpReady(context)}")

        if (!WordPressDependencyManager.isPhpReady(context)) {
            onStateChanged(PhpAppPreviewState.Downloading)
            val success = WordPressDependencyManager.downloadPhpDependency(context)
            if (!success) {
                onStateChanged(PhpAppPreviewState.Error(AppStringsProvider.current().phpAppDownloadFailed))
                return@LaunchedEffect
            }
        }

        val projectId = config.projectId
        AppLogger.i("PhpAppPreview", "projectId='$projectId', docRoot='${config.documentRoot}', entry='${config.entryFile}'")
        if (projectId.isBlank()) {
            AppLogger.e("PhpAppPreview", "projectId 为空")
            onStateChanged(PhpAppPreviewState.Error(AppStringsProvider.current().phpAppProjectNotFound))
            return@LaunchedEffect
        }
        val projectDir = phpAppRuntime.getProjectDir(projectId)
        AppLogger.i("PhpAppPreview", "项目目录: ${projectDir.absolutePath}, exists=${projectDir.exists()}")
        if (!projectDir.exists()) {
            onStateChanged(PhpAppPreviewState.Error(AppStringsProvider.current().phpAppProjectNotFound))
            return@LaunchedEffect
        }

        projectDir.listFiles()?.take(20)?.forEach { file ->
            AppLogger.d("PhpAppPreview", "  - ${file.name} (${if (file.isDirectory) "dir" else "${file.length()} bytes"})")
        }

        var actualDocRoot = config.documentRoot
        var actualEntryFile = config.entryFile
        var actualProjectDir = projectDir
        val docRootDir = if (actualDocRoot.isNotBlank()) File(projectDir, actualDocRoot) else projectDir
        if (!File(docRootDir, actualEntryFile).exists()) {
            AppLogger.i("PhpAppPreview", "入口文件不存在，尝试自动检测框架...")

            var detectedFramework = phpAppRuntime.detectFramework(projectDir)
            var detectedDocRoot = phpAppRuntime.detectDocumentRoot(projectDir, detectedFramework)
            var detectedEntry = phpAppRuntime.detectEntryFile(projectDir, detectedDocRoot)

            val detectedDocRootDir = if (detectedDocRoot.isNotBlank()) File(projectDir, detectedDocRoot) else projectDir
            if (!File(detectedDocRootDir, detectedEntry).exists()) {
                AppLogger.i("PhpAppPreview", "根目录未找到入口文件，扫描子目录...")
                val phpSubDir = projectDir.listFiles()
                    ?.filter { it.isDirectory && it.name != "__MACOSX" && !it.name.startsWith("._") }
                    ?.firstOrNull { sub -> sub.listFiles()?.any { it.isFile && it.extension == "php" } == true }

                if (phpSubDir != null) {
                    AppLogger.i("PhpAppPreview", "找到 PHP 子目录: ${phpSubDir.name}")
                    actualProjectDir = phpSubDir
                    detectedFramework = phpAppRuntime.detectFramework(phpSubDir)
                    detectedDocRoot = phpAppRuntime.detectDocumentRoot(phpSubDir, detectedFramework)
                    detectedEntry = phpAppRuntime.detectEntryFile(phpSubDir, detectedDocRoot)
                }
            }

            AppLogger.i("PhpAppPreview", "自动检测: framework=$detectedFramework, docRoot='$detectedDocRoot', entry='$detectedEntry', projectDir=${actualProjectDir.name}")
            actualDocRoot = detectedDocRoot
            actualEntryFile = detectedEntry
        }

        onStateChanged(PhpAppPreviewState.StartingServer)
        AppLogger.i("PhpAppPreview", "启动 PHP 服务器: docRoot='$actualDocRoot', entry='$actualEntryFile'")
        val port = phpAppRuntime.startServer(
            projectDir = actualProjectDir.absolutePath,
            documentRoot = actualDocRoot,
            entryFile = actualEntryFile,
            port = config.phpPort,
            envVars = config.envVars
        )

        if (port > 0) {
            val url = "http://127.0.0.1:$port/"
            AppLogger.i("PhpAppPreview", "PHP 服务器已启动: $url")
            onStateChanged(PhpAppPreviewState.Ready(url))
            delay(200)
            loadUrl(url)
        } else {
            AppLogger.e("PhpAppPreview", "PHP 服务器启动失败, port=$port, serverState=${phpAppRuntime.serverState.value}")
            val errorDetail = when (val state = phpAppRuntime.serverState.value) {
                is PhpAppRuntime.ServerState.Error -> state.message
                else -> AppStringsProvider.current().phpAppServerError
            }
            onStateChanged(PhpAppPreviewState.Error(errorDetail))
        }
    }

    DisposableEffect(phpAppRuntime) {
        onDispose {
            phpAppRuntime.stopServer()
        }
    }
}

@Composable
internal fun PythonAppPreviewCoordinator(
    appId: Long,
    webApp: WebApp?,
    isActivated: Boolean,
    isActivationChecked: Boolean,
    retryTrigger: Int,
    pythonRuntime: PythonRuntime,
    pythonHttpServer: LocalHttpServer,
    onStateChanged: (PythonAppPreviewState) -> Unit,
    loadUrl: (String) -> Unit
) {
    LaunchedEffect(webApp, isActivated, isActivationChecked, retryTrigger) {
        val app = webApp ?: return@LaunchedEffect
        if (app.appType != AppType.PYTHON_APP) return@LaunchedEffect
        if (!isActivated || !isActivationChecked) return@LaunchedEffect

        val config = app.pythonAppConfig
        if (config == null) {
            AppLogger.e("PythonAppPreview", "pythonAppConfig 为 null")
            onStateChanged(PythonAppPreviewState.Error(AppStringsProvider.current().pyProjectNotFound))
            return@LaunchedEffect
        }

        AppLogger.i("PythonAppPreview", "开始 Python 应用预览流程, appId=$appId, config=$config")
        onStateChanged(PythonAppPreviewState.Starting)

        val projectId = config.projectId
        AppLogger.i("PythonAppPreview", "projectId='$projectId', framework='${config.framework}', entry='${config.entryFile}'")
        if (projectId.isBlank()) {
            AppLogger.e("PythonAppPreview", "projectId 为空")
            onStateChanged(PythonAppPreviewState.Error(AppStringsProvider.current().pyProjectNotFound))
            return@LaunchedEffect
        }
        val projectDir = pythonRuntime.getProjectDir(projectId)
        AppLogger.i("PythonAppPreview", "项目目录: ${projectDir.absolutePath}, exists=${projectDir.exists()}")
        if (!projectDir.exists()) {
            onStateChanged(PythonAppPreviewState.Error(AppStringsProvider.current().pyProjectNotFound))
            return@LaunchedEffect
        }

        projectDir.listFiles()?.take(20)?.forEach { file ->
            AppLogger.d("PythonAppPreview", "  - ${file.name} (${if (file.isDirectory) "dir" else "${file.length()} bytes"})")
        }

        var actualProjectDir = projectDir
        var actualEntryFile = config.entryFile.ifBlank { "app.py" }
        var actualFramework = config.framework.ifBlank { "raw" }

        if (!File(actualProjectDir, actualEntryFile).exists()) {
            AppLogger.i("PythonAppPreview", "入口文件不存在: $actualEntryFile，尝试自动检测...")

            val detectedFramework = pythonRuntime.detectFramework(projectDir)
            val detectedEntry = pythonRuntime.detectEntryFile(projectDir, detectedFramework)

            if (File(projectDir, detectedEntry).exists()) {
                AppLogger.i("PythonAppPreview", "自动检测到: framework=$detectedFramework, entry=$detectedEntry")
                actualFramework = detectedFramework
                actualEntryFile = detectedEntry
            } else {
                AppLogger.i("PythonAppPreview", "根目录未找到入口文件，扫描子目录...")
                val pySubDir = projectDir.listFiles()
                    ?.filter {
                        it.isDirectory &&
                            it.name != "__MACOSX" &&
                            it.name != "__pycache__" &&
                            !it.name.startsWith("._") &&
                            it.name != "venv" &&
                            it.name != ".venv" &&
                            it.name != ".git"
                    }
                    ?.firstOrNull { sub ->
                        sub.listFiles()?.any { it.isFile && it.extension == "py" } == true
                    }

                if (pySubDir != null) {
                    AppLogger.i("PythonAppPreview", "找到 Python 子目录: ${pySubDir.name}")
                    actualProjectDir = pySubDir
                    actualFramework = pythonRuntime.detectFramework(pySubDir)
                    actualEntryFile = pythonRuntime.detectEntryFile(pySubDir, actualFramework)
                    AppLogger.i("PythonAppPreview", "子目录检测: framework=$actualFramework, entry=$actualEntryFile")
                }
            }
        }

        AppLogger.i("PythonAppPreview", "最终配置: projectDir=${actualProjectDir.absolutePath}, framework=$actualFramework, entry=$actualEntryFile")

        try {
            val candidates = listOf("dist", "build", "public", "static", "www", "templates", "")
            var docRoot: File? = null
            for (dir in candidates) {
                val candidate = if (dir.isEmpty()) actualProjectDir else File(actualProjectDir, dir)
                val hasIndex = File(candidate, "index.html").exists()
                AppLogger.d("PythonAppPreview", "检查候选: '$dir' -> ${candidate.absolutePath}, isDir=${candidate.isDirectory}, hasIndex=$hasIndex")
                if (candidate.isDirectory && hasIndex) {
                    docRoot = candidate
                    AppLogger.i("PythonAppPreview", "找到 docRoot: ${candidate.absolutePath}")
                    break
                }
            }

            if (docRoot != null) {
                val url = pythonHttpServer.start(docRoot)
                AppLogger.i("PythonAppPreview", "LocalHttpServer 已启动: $url")
                onStateChanged(PythonAppPreviewState.Ready(url))
                delay(200)
                loadUrl(url)
            } else if (pythonRuntime.isPythonAvailable()) {
                AppLogger.i("PythonAppPreview", "Python 运行时可用，启动后端服务器")
                onStateChanged(PythonAppPreviewState.StartingServer)

                val serverPort = pythonRuntime.startServer(
                    projectDir = actualProjectDir.absolutePath,
                    entryFile = actualEntryFile,
                    framework = actualFramework,
                    port = config.serverPort,
                    envVars = config.envVars,
                    installDeps = config.hasPipDeps
                )

                if (serverPort > 0) {
                    val serverUrl = "http://127.0.0.1:$serverPort"
                    AppLogger.i("PythonAppPreview", "Python 服务器已启动: $serverUrl")
                    onStateChanged(PythonAppPreviewState.Ready(serverUrl))
                    delay(200)
                    loadUrl(serverUrl)
                } else {
                    AppLogger.e("PythonAppPreview", "Python 服务器启动失败，回退到预览模式")
                    val url = pythonHttpServer.start(actualProjectDir)
                    File(actualProjectDir, "_preview_.html").delete()
                    val previewHtml = pythonRuntime.generatePreviewHtml(
                        projectDir = actualProjectDir,
                        framework = actualFramework,
                        entryFile = actualEntryFile
                    )
                    val previewFile = File(actualProjectDir, "_preview_.html")
                    previewFile.writeText(previewHtml)
                    val targetUrl = "$url/_preview_.html"
                    onStateChanged(PythonAppPreviewState.Ready(targetUrl))
                    delay(200)
                    loadUrl(targetUrl)
                }
            } else {
                AppLogger.w("PythonAppPreview", "Python 运行时不可用，生成项目预览页面")
                val url = pythonHttpServer.start(actualProjectDir)
                File(actualProjectDir, "_preview_.html").delete()

                val htmlFiles = actualProjectDir.walkTopDown()
                    .filter { it.extension == "html" && it.name != "_preview_.html" }
                    .take(1)
                    .toList()
                if (htmlFiles.isNotEmpty()) {
                    val relPath = htmlFiles.first().relativeTo(actualProjectDir).path
                    val targetUrl = "$url/$relPath"
                    onStateChanged(PythonAppPreviewState.Ready(targetUrl))
                    delay(200)
                    loadUrl(targetUrl)
                } else {
                    val previewHtml = pythonRuntime.generatePreviewHtml(
                        projectDir = actualProjectDir,
                        framework = actualFramework,
                        entryFile = actualEntryFile
                    )
                    val previewFile = File(actualProjectDir, "_preview_.html")
                    previewFile.writeText(previewHtml)
                    val targetUrl = "$url/_preview_.html"
                    onStateChanged(PythonAppPreviewState.Ready(targetUrl))
                    delay(200)
                    loadUrl(targetUrl)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("PythonAppPreview", "启动预览失败", e)
            onStateChanged(PythonAppPreviewState.Error(e.message ?: AppStringsProvider.current().pyPreviewFailed))
        }
    }

    DisposableEffect(pythonHttpServer) {
        onDispose {
            pythonHttpServer.stop()
            pythonRuntime.stopServer()
        }
    }
}

@Composable
internal fun NodeJsAppPreviewCoordinator(
    appId: Long,
    webApp: WebApp?,
    isActivated: Boolean,
    isActivationChecked: Boolean,
    retryTrigger: Int,
    nodeRuntime: NodeRuntime,
    nodeHttpServer: LocalHttpServer,
    onStateChanged: (NodeJsAppPreviewState) -> Unit,
    loadUrl: (String) -> Unit
) {
    LaunchedEffect(webApp, isActivated, isActivationChecked, retryTrigger) {
        val app = webApp ?: return@LaunchedEffect
        if (app.appType != AppType.NODEJS_APP) return@LaunchedEffect
        if (!isActivated || !isActivationChecked) return@LaunchedEffect

        val config = app.nodejsConfig
        if (config == null) {
            AppLogger.e("NodeJsAppPreview", "nodejsConfig 为 null")
            onStateChanged(NodeJsAppPreviewState.Error(AppStringsProvider.current().nodeProjectNotFound))
            return@LaunchedEffect
        }

        AppLogger.i("NodeJsAppPreview", "开始 Node.js 应用预览流程, appId=$appId, config=$config")
        onStateChanged(NodeJsAppPreviewState.Starting)

        val projectId = config.projectId
        AppLogger.i("NodeJsAppPreview", "projectId='$projectId', framework='${config.framework}', entry='${config.entryFile}'")
        if (projectId.isBlank()) {
            AppLogger.e("NodeJsAppPreview", "projectId 为空")
            onStateChanged(NodeJsAppPreviewState.Error(AppStringsProvider.current().nodeProjectNotFound))
            return@LaunchedEffect
        }
        val projectDir = nodeRuntime.getProjectDir(projectId)
        AppLogger.i("NodeJsAppPreview", "项目目录: ${projectDir.absolutePath}, exists=${projectDir.exists()}")
        if (!projectDir.exists()) {
            onStateChanged(NodeJsAppPreviewState.Error(AppStringsProvider.current().nodeProjectNotFound))
            return@LaunchedEffect
        }

        projectDir.listFiles()?.take(20)?.forEach { file ->
            AppLogger.d("NodeJsAppPreview", "  - ${file.name} (${if (file.isDirectory) "dir" else "${file.length()} bytes"})")
        }

        try {
            val candidates = listOf("dist", "build", "public", "static", "www", "")
            var foundDocRoot: File? = null
            for (dir in candidates) {
                val candidate = if (dir.isEmpty()) projectDir else File(projectDir, dir)
                val hasIndex = File(candidate, "index.html").exists()
                AppLogger.d("NodeJsAppPreview", "检查候选: '$dir' -> ${candidate.absolutePath}, isDir=${candidate.isDirectory}, hasIndex=$hasIndex")
                if (candidate.isDirectory && hasIndex) {
                    foundDocRoot = candidate
                    AppLogger.i("NodeJsAppPreview", "找到 docRoot: ${candidate.absolutePath}")
                    break
                }
            }

            val docRoot = foundDocRoot
            if (docRoot != null) {
                val url = nodeHttpServer.start(docRoot)
                AppLogger.i("NodeJsAppPreview", "LocalHttpServer 已启动: $url")
                onStateChanged(NodeJsAppPreviewState.Ready(url))
                delay(200)
                loadUrl(url)
            } else {
                AppLogger.w("NodeJsAppPreview", "未找到 index.html，尝试在项目根启动 HTTP 服务器")
                val url = nodeHttpServer.start(projectDir)
                AppLogger.i("NodeJsAppPreview", "LocalHttpServer 在项目根启动: $url")

                File(projectDir, "_preview_.html").delete()

                val htmlFiles = projectDir.walkTopDown()
                    .filter { it.extension == "html" && it.name != "_preview_.html" }
                    .take(1)
                    .toList()
                if (htmlFiles.isNotEmpty()) {
                    val relPath = htmlFiles.first().relativeTo(projectDir).path
                    val targetUrl = "$url/$relPath"
                    AppLogger.i("NodeJsAppPreview", "找到 HTML 文件: $relPath, URL=$targetUrl")
                    onStateChanged(NodeJsAppPreviewState.Ready(targetUrl))
                    delay(200)
                    loadUrl(targetUrl)
                } else {
                    AppLogger.i("NodeJsAppPreview", "无静态 HTML，生成项目预览页面")
                    val previewHtml = nodeRuntime.generatePreviewHtml(
                        projectDir = projectDir,
                        framework = config.framework,
                        entryFile = config.entryFile
                    )
                    val previewFile = File(projectDir, "_preview_.html")
                    previewFile.writeText(previewHtml)
                    val targetUrl = "$url/_preview_.html"
                    AppLogger.i("NodeJsAppPreview", "预览页面已生成: $targetUrl")
                    onStateChanged(NodeJsAppPreviewState.Ready(targetUrl))
                    delay(200)
                    loadUrl(targetUrl)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("NodeJsAppPreview", "启动预览失败", e)
            onStateChanged(NodeJsAppPreviewState.Error(e.message ?: AppStringsProvider.current().nodePreviewFailed))
        }
    }

    DisposableEffect(nodeHttpServer) {
        onDispose {
            nodeHttpServer.stop()
        }
    }
}

@Composable
internal fun GoAppPreviewCoordinator(
    appId: Long,
    webApp: WebApp?,
    isActivated: Boolean,
    isActivationChecked: Boolean,
    retryTrigger: Int,
    goRuntime: GoRuntime,
    goHttpServer: LocalHttpServer,
    onStateChanged: (GoAppPreviewState) -> Unit,
    loadUrl: (String) -> Unit
) {
    LaunchedEffect(webApp, isActivated, isActivationChecked, retryTrigger) {
        val app = webApp ?: return@LaunchedEffect
        if (app.appType != AppType.GO_APP) return@LaunchedEffect
        if (!isActivated || !isActivationChecked) return@LaunchedEffect

        val config = app.goAppConfig
        if (config == null) {
            AppLogger.e("GoAppPreview", "goAppConfig 为 null")
            onStateChanged(GoAppPreviewState.Error(AppStringsProvider.current().goProjectNotFound))
            return@LaunchedEffect
        }

        AppLogger.i("GoAppPreview", "开始 Go 应用预览流程, appId=$appId, config=$config")
        onStateChanged(GoAppPreviewState.Starting)

        val projectId = config.projectId
        AppLogger.i("GoAppPreview", "projectId='$projectId', framework='${config.framework}', binary='${config.binaryName}'")
        if (projectId.isBlank()) {
            AppLogger.e("GoAppPreview", "projectId 为空")
            onStateChanged(GoAppPreviewState.Error(AppStringsProvider.current().goProjectNotFound))
            return@LaunchedEffect
        }
        val projectDir = goRuntime.getProjectDir(projectId)
        AppLogger.i("GoAppPreview", "项目目录: ${projectDir.absolutePath}, exists=${projectDir.exists()}")
        if (!projectDir.exists()) {
            onStateChanged(GoAppPreviewState.Error(AppStringsProvider.current().goProjectNotFound))
            return@LaunchedEffect
        }

        projectDir.listFiles()?.take(20)?.forEach { file ->
            AppLogger.d("GoAppPreview", "  - ${file.name} (${if (file.isDirectory) "dir" else "${file.length()} bytes"})")
        }

        try {
            val candidates = listOf("dist", "build", "public", "static", "web", "www", "")
            var foundDocRoot: File? = null
            for (dir in candidates) {
                val candidate = if (dir.isEmpty()) projectDir else File(projectDir, dir)
                val hasIndex = File(candidate, "index.html").exists()
                AppLogger.d("GoAppPreview", "检查候选: '$dir' -> ${candidate.absolutePath}, isDir=${candidate.isDirectory}, hasIndex=$hasIndex")
                if (candidate.isDirectory && hasIndex) {
                    foundDocRoot = candidate
                    AppLogger.i("GoAppPreview", "找到 docRoot: ${candidate.absolutePath}")
                    break
                }
            }

            val docRoot = foundDocRoot
            if (docRoot != null) {
                val url = goHttpServer.start(docRoot)
                AppLogger.i("GoAppPreview", "LocalHttpServer 已启动: $url")
                onStateChanged(GoAppPreviewState.Ready(url))
                delay(200)
                loadUrl(url)
            } else if (config.binaryName.isNotBlank() && goRuntime.detectBinary(projectDir) != null) {
                AppLogger.i("GoAppPreview", "检测到 Go 二进制，启动后端服务器")
                onStateChanged(GoAppPreviewState.StartingServer)

                val serverPort = goRuntime.startServer(
                    projectDir = projectDir.absolutePath,
                    binaryName = config.binaryName,
                    port = config.serverPort,
                    envVars = config.envVars
                )

                if (serverPort > 0) {
                    val serverUrl = "http://127.0.0.1:$serverPort"
                    AppLogger.i("GoAppPreview", "Go 服务器已启动: $serverUrl")
                    onStateChanged(GoAppPreviewState.Ready(serverUrl))
                    delay(200)
                    loadUrl(serverUrl)
                } else {
                    AppLogger.e("GoAppPreview", "Go 服务器启动失败，回退到预览模式")
                    val url = goHttpServer.start(projectDir)
                    File(projectDir, "_preview_.html").delete()
                    val previewHtml = goRuntime.generatePreviewHtml(
                        projectDir = projectDir,
                        framework = config.framework,
                        binaryName = config.binaryName
                    )
                    val previewFile = File(projectDir, "_preview_.html")
                    previewFile.writeText(previewHtml)
                    val targetUrl = "$url/_preview_.html"
                    onStateChanged(GoAppPreviewState.Ready(targetUrl))
                    delay(200)
                    loadUrl(targetUrl)
                }
            } else {
                AppLogger.w("GoAppPreview", "无可执行二进制，生成项目预览页面")
                val url = goHttpServer.start(projectDir)
                File(projectDir, "_preview_.html").delete()

                val htmlFiles = projectDir.walkTopDown()
                    .filter { it.extension == "html" && it.name != "_preview_.html" }
                    .take(1)
                    .toList()
                if (htmlFiles.isNotEmpty()) {
                    val relPath = htmlFiles.first().relativeTo(projectDir).path
                    val targetUrl = "$url/$relPath"
                    onStateChanged(GoAppPreviewState.Ready(targetUrl))
                    delay(200)
                    loadUrl(targetUrl)
                } else {
                    val previewHtml = goRuntime.generatePreviewHtml(
                        projectDir = projectDir,
                        framework = config.framework,
                        binaryName = config.binaryName
                    )
                    val previewFile = File(projectDir, "_preview_.html")
                    previewFile.writeText(previewHtml)
                    val targetUrl = "$url/_preview_.html"
                    onStateChanged(GoAppPreviewState.Ready(targetUrl))
                    delay(200)
                    loadUrl(targetUrl)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("GoAppPreview", "启动预览失败", e)
            onStateChanged(GoAppPreviewState.Error(e.message ?: AppStringsProvider.current().goPreviewFailed))
        }
    }

    DisposableEffect(goHttpServer) {
        onDispose {
            goHttpServer.stop()
            goRuntime.stopServer()
        }
    }
}
