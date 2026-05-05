package com.webtoapp.core.linux

import android.content.Context
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File











object HtmlProjectOptimizer {

    private const val TAG = "HtmlProjectOptimizer"




    data class OptimizeResult(
        val success: Boolean,
        val jsFilesOptimized: Int = 0,
        val cssFilesOptimized: Int = 0,
        val tsFilesCompiled: Int = 0,
        val savedBytes: Long = 0,
        val error: String? = null
    )










    suspend fun optimizeFiles(
        context: Context,
        jsFilePath: String?,
        cssFilePath: String?,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): OptimizeResult = withContext(Dispatchers.IO) {
        var jsOptimized = 0
        var cssOptimized = 0
        var savedBytes = 0L

        try {
            val esbuildAvailable = NativeNodeEngine.isAvailable(context)


            if (jsFilePath != null) {
                val jsFile = File(jsFilePath)
                if (jsFile.exists() && jsFile.length() > 0) {
                    onProgress(Strings.htmlOptJs, 0.2f)
                    val originalSize = jsFile.length()

                    if (esbuildAvailable) {
                        val result = minifyWithEsbuild(context, jsFile)
                        if (result) {
                            savedBytes += originalSize - jsFile.length()
                            jsOptimized++
                        }
                    } else {
                        val result = minifyJsPure(jsFile)
                        if (result) {
                            savedBytes += originalSize - jsFile.length()
                            jsOptimized++
                        }
                    }
                }
            }


            if (cssFilePath != null) {
                val cssFile = File(cssFilePath)
                if (cssFile.exists() && cssFile.length() > 0) {
                    onProgress(Strings.htmlOptCss, 0.6f)
                    val originalSize = cssFile.length()

                    if (esbuildAvailable) {
                        val result = minifyWithEsbuild(context, cssFile)
                        if (result) {
                            savedBytes += originalSize - cssFile.length()
                            cssOptimized++
                        }
                    } else {
                        val result = minifyCssPure(cssFile)
                        if (result) {
                            savedBytes += originalSize - cssFile.length()
                            cssOptimized++
                        }
                    }
                }
            }

            onProgress(Strings.htmlOptComplete, 1f)

            OptimizeResult(
                success = true,
                jsFilesOptimized = jsOptimized,
                cssFilesOptimized = cssOptimized,
                savedBytes = savedBytes
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "优化失败", e)
            OptimizeResult(success = false, error = e.message)
        }
    }









    suspend fun optimizeDirectory(
        context: Context,
        projectDir: String,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): OptimizeResult = withContext(Dispatchers.IO) {
        var jsOptimized = 0
        var cssOptimized = 0
        var tsCompiled = 0
        var savedBytes = 0L

        try {
            val dir = File(projectDir)
            if (!dir.exists() || !dir.isDirectory) {
                return@withContext OptimizeResult(success = false, error = "目录不存在")
            }

            val esbuildAvailable = NativeNodeEngine.isAvailable(context)


            val jsFiles = mutableListOf<File>()
            val cssFiles = mutableListOf<File>()
            val tsFiles = mutableListOf<File>()

            dir.walkTopDown()
                .filter { it.isFile }
                .filter { !it.absolutePath.contains("/node_modules/") }
                .filter { !it.absolutePath.contains("/.git/") }
                .forEach { file ->
                    when (file.extension.lowercase()) {
                        "js", "mjs" -> jsFiles.add(file)
                        "css" -> cssFiles.add(file)
                        "ts" -> tsFiles.add(file)
                        "tsx", "jsx" -> tsFiles.add(file)
                    }
                }

            val totalFiles = jsFiles.size + cssFiles.size + tsFiles.size
            if (totalFiles == 0) {
                return@withContext OptimizeResult(success = true)
            }

            var processedCount = 0


            if (tsFiles.isNotEmpty() && esbuildAvailable) {
                onProgress(Strings.htmlOptCompileTs.format(tsFiles.size), 0.1f)

                for (tsFile in tsFiles) {
                    val originalSize = tsFile.length()
                    val outputFile = File(tsFile.parentFile, tsFile.nameWithoutExtension + ".js")

                    val result = compileTypeScriptWithEsbuild(context, tsFile, outputFile)
                    if (result) {
                        tsCompiled++

                        updateHtmlReferences(dir, tsFile.name, outputFile.name)
                    }
                    processedCount++
                    onProgress(
                        Strings.htmlOptCompileTsFile.format(tsFile.name),
                        processedCount.toFloat() / totalFiles
                    )
                }
            }


            if (jsFiles.isNotEmpty()) {
                onProgress(Strings.htmlOptCompressJs.format(jsFiles.size),
                    processedCount.toFloat() / totalFiles)

                for (jsFile in jsFiles) {
                    val originalSize = jsFile.length()

                    val result = if (esbuildAvailable) {
                        minifyWithEsbuild(context, jsFile)
                    } else {
                        minifyJsPure(jsFile)
                    }

                    if (result) {
                        savedBytes += originalSize - jsFile.length()
                        jsOptimized++
                    }
                    processedCount++
                    onProgress(
                        Strings.htmlOptCompressJsFile.format(jsFile.name),
                        processedCount.toFloat() / totalFiles
                    )
                }
            }


            if (cssFiles.isNotEmpty()) {
                onProgress(Strings.htmlOptCompressCss.format(cssFiles.size),
                    processedCount.toFloat() / totalFiles)

                for (cssFile in cssFiles) {
                    val originalSize = cssFile.length()

                    val result = if (esbuildAvailable) {
                        minifyWithEsbuild(context, cssFile)
                    } else {
                        minifyCssPure(cssFile)
                    }

                    if (result) {
                        savedBytes += originalSize - cssFile.length()
                        cssOptimized++
                    }
                    processedCount++
                    onProgress(
                        Strings.htmlOptCompressCssFile.format(cssFile.name),
                        processedCount.toFloat() / totalFiles
                    )
                }
            }

            onProgress(Strings.htmlOptOptimizeComplete, 1f)

            OptimizeResult(
                success = true,
                jsFilesOptimized = jsOptimized,
                cssFilesOptimized = cssOptimized,
                tsFilesCompiled = tsCompiled,
                savedBytes = savedBytes
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "目录优化失败", e)
            OptimizeResult(success = false, error = e.message)
        }
    }






    private suspend fun minifyWithEsbuild(
        context: Context,
        file: File
    ): Boolean {
        val tempOutput = File(file.parentFile, "${file.nameWithoutExtension}.min.${file.extension}")

        try {
            val args = listOf(
                file.absolutePath,
                "--outfile=${tempOutput.absolutePath}",
                "--minify",
                "--allow-overwrite"
            )

            val result = NativeNodeEngine.executeEsbuild(
                context = context,
                args = args,
                workingDir = file.parentFile ?: file,
                timeout = 30_000
            )

            if (result.exitCode == 0 && tempOutput.exists() && tempOutput.length() > 0) {

                tempOutput.copyTo(file, overwrite = true)
                tempOutput.delete()
                return true
            } else {
                tempOutput.delete()
                AppLogger.w(TAG, "esbuild minify 失败: ${result.stderr}")
                return false
            }
        } catch (e: Exception) {
            tempOutput.delete()
            AppLogger.w(TAG, "esbuild minify 异常: ${e.message}")
            return false
        }
    }




    private suspend fun compileTypeScriptWithEsbuild(
        context: Context,
        tsFile: File,
        outputFile: File
    ): Boolean {
        try {
            val loader = when (tsFile.extension.lowercase()) {
                "tsx" -> "tsx"
                "jsx" -> "jsx"
                else -> "ts"
            }

            val args = listOf(
                tsFile.absolutePath,
                "--outfile=${outputFile.absolutePath}",
                "--loader:.${tsFile.extension}=$loader",
                "--minify",
                "--platform=browser",
                "--target=es2020"
            )

            val result = NativeNodeEngine.executeEsbuild(
                context = context,
                args = args,
                workingDir = tsFile.parentFile ?: tsFile,
                timeout = 30_000
            )

            return result.exitCode == 0 && outputFile.exists() && outputFile.length() > 0
        } catch (e: Exception) {
            AppLogger.w(TAG, "TypeScript 编译失败: ${e.message}")
            return false
        }
    }







    private fun minifyJsPure(file: File): Boolean {
        try {
            val content = file.readText()
            if (content.length < 100) return false

            val minified = StringBuilder()
            var i = 0
            var inString = false
            var stringChar = ' '
            var inSingleLineComment = false
            var inMultiLineComment = false
            var lastChar = ' '

            while (i < content.length) {
                val c = content[i]
                val next = if (i + 1 < content.length) content[i + 1] else ' '

                when {

                    inString -> {
                        minified.append(c)
                        if (c == stringChar && lastChar != '\\') {
                            inString = false
                        }
                    }

                    inSingleLineComment -> {
                        if (c == '\n') {
                            inSingleLineComment = false
                            minified.append('\n')
                        }
                    }

                    inMultiLineComment -> {
                        if (c == '*' && next == '/') {
                            inMultiLineComment = false
                            i++
                        }
                    }

                    c == '/' && next == '/' -> {
                        inSingleLineComment = true
                        i++
                    }
                    c == '/' && next == '*' -> {
                        inMultiLineComment = true
                        i++
                    }

                    c == '"' || c == '\'' || c == '`' -> {
                        inString = true
                        stringChar = c
                        minified.append(c)
                    }

                    c.isWhitespace() -> {
                        if (minified.isNotEmpty() && !minified.last().isWhitespace()) {

                            if (minified.last().isLetterOrDigit() || minified.last() == '_' || minified.last() == '$') {
                                minified.append(' ')
                            }
                        }
                    }
                    else -> {
                        minified.append(c)
                    }
                }

                lastChar = c
                i++
            }

            val result = minified.toString().trim()
            if (result.length < content.length) {
                file.writeText(result)
                return true
            }
            return false
        } catch (e: Exception) {
            AppLogger.w(TAG, "纯 Kotlin JS 压缩失败: ${e.message}")
            return false
        }
    }





    private fun minifyCssPure(file: File): Boolean {
        try {
            val content = file.readText()
            if (content.length < 100) return false

            var result = content

            result = result.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")

            result = result.replace(Regex("\\s+"), " ")

            result = result.replace(Regex("\\s*([{}:;,>~+])\\s*"), "$1")

            result = result.replace(Regex(";\\}"), "}")

            result = result.trim()

            if (result.length < content.length) {
                file.writeText(result)
                return true
            }
            return false
        } catch (e: Exception) {
            AppLogger.w(TAG, "纯 Kotlin CSS 压缩失败: ${e.message}")
            return false
        }
    }






    private fun updateHtmlReferences(projectDir: File, oldName: String, newName: String) {
        projectDir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in listOf("html", "htm") }
            .forEach { htmlFile ->
                try {
                    var content = htmlFile.readText()
                    if (content.contains(oldName)) {
                        content = content.replace(oldName, newName)
                        htmlFile.writeText(content)
                        AppLogger.d(TAG, "更新 HTML 引用: ${htmlFile.name} ($oldName → $newName)")
                    }
                } catch (e: Exception) {
                    AppLogger.w(TAG, "更新 HTML 引用失败: ${htmlFile.name}", e)
                }
            }
    }
}
