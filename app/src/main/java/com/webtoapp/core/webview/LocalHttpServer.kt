package com.webtoapp.core.webview

import android.annotation.SuppressLint
import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.port.PortManager
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean









@SuppressLint("StaticFieldLeak")
class LocalHttpServer(
    private val context: Context,
    private val port: Int = 0
) {
    companion object {
        private const val TAG = "LocalHttpServer"
        private const val LOOPBACK_HOST = "127.0.0.1"

        @Volatile
        private var instance: LocalHttpServer? = null

        fun getInstance(context: Context): LocalHttpServer {
            return instance ?: synchronized(this) {
                instance ?: LocalHttpServer(context.applicationContext).also { instance = it }
            }
        }

        fun shouldEnableCrossOriginIsolation(rootDir: File): Boolean {
            if (!rootDir.exists()) return false

            return rootDir.walkTopDown()
                .maxDepth(6)
                .filter { it.isFile }
                .take(2000)
                .any { file ->
                    val name = file.name.lowercase()
                    val extension = file.extension.lowercase()
                    extension == "wasm" ||
                        extension == "onnx" ||
                        name.contains("ort-wasm") ||
                        name.contains("worker")
                }
        }

        fun stablePortForPackageName(
            packageName: String,
            range: PortManager.PortRange = PortManager.PortRange.LOCAL_HTTP
        ): Int {
            val normalized = packageName.trim().lowercase()
            if (normalized.isBlank()) return range.start
            val offset = normalized.hashCode().toUInt().toInt() % range.size
            return range.start + offset
        }

        fun buildLoopbackBaseUrl(port: Int): String = "http://$LOOPBACK_HOST:$port"
    }

    private var serverSocket: ServerSocket? = null

    private var executor = createExecutor()
    private val isRunning = AtomicBoolean(false)
    private val acceptThreadName = "LocalHttpServer-Accept"


    private var rootDirectory: File? = null
    private var crossOriginIsolationEnabled: Boolean = false

    private fun createExecutor(): ThreadPoolExecutor {
        return ThreadPoolExecutor(
            1, 4, 30L, TimeUnit.SECONDS, LinkedBlockingQueue(32)
        )
    }

    private fun ensureExecutor(): ThreadPoolExecutor {
        if (executor.isShutdown || executor.isTerminated) {
            executor = createExecutor()
        }
        return executor
    }


    var actualPort: Int = 0
        private set






    @Synchronized
    fun start(rootDir: File, enableCrossOriginIsolation: Boolean = false): String {
        if (isRunning.get() && rootDirectory == rootDir && crossOriginIsolationEnabled == enableCrossOriginIsolation) {

            return buildLoopbackBaseUrl(actualPort)
        }


        stop()

        rootDirectory = rootDir
        crossOriginIsolationEnabled = enableCrossOriginIsolation

        try {

            val allocatedPort = if (port > 0) {
                PortManager.allocateForLocalHttp(rootDir.name, port)
            } else {
                PortManager.allocateForLocalHttp(rootDir.name)
            }



            // 显式绑定到 IPv4 127.0.0.1，与 buildLoopbackBaseUrl 拼出的 URL 一致。
            // InetAddress.getLoopbackAddress() 在某些设备/Emulator（开启 IPv6 优先）上会返回 ::1，
            // ServerSocket 就只监听 IPv6 loopback，而 WebView 仍按 "http://127.0.0.1:..." 连接，
            // 导致 ERR_CONNECTION_REFUSED——服务器启动成功却看似不可达。
            val bindAddress = java.net.Inet4Address.getByName(LOOPBACK_HOST)
            serverSocket = ServerSocket(allocatedPort, 50, bindAddress)
            actualPort = serverSocket?.localPort ?: allocatedPort
            isRunning.set(true)

            AppLogger.i(TAG, "服务器启动在端口 $actualPort, 根目录: ${rootDir.absolutePath}")

            // ServerSocket 构造返回时，内核已经完成 bind()+listen()，
            // 从同进程用 loopback 连接一定会进入 TCP backlog（默认 50），不会被 RST。
            // accept 线程只是消费 backlog；即使它晚几毫秒开始 accept，客户端 connect 也不会失败。
            //
            // 关键约束：本方法常在主线程或 UI 调度器里被调用（Compose LaunchedEffect 默认 Main），
            // 因此禁止在这里做任何阻塞式网络 IO（会抛 NetworkOnMainThreadException）。
            Thread({
                while (isRunning.get()) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        ensureExecutor().execute { handleClient(clientSocket) }
                    } catch (e: Exception) {
                        if (isRunning.get()) {
                            AppLogger.e(TAG, "接受连接失败", e)
                        }
                    }
                }
            }, acceptThreadName).apply { isDaemon = true }.start()

            return buildLoopbackBaseUrl(actualPort)
        } catch (e: Exception) {
            AppLogger.e(TAG, "启动服务器失败", e)
            throw e
        }
    }




    @Synchronized
    fun stop() {
        if (!isRunning.get()) return

        isRunning.set(false)
        try {

            if (actualPort > 0) {
                PortManager.release(actualPort)
            }
            serverSocket?.close()
            serverSocket = null
            actualPort = 0
            executor.shutdownNow()
            AppLogger.i(TAG, "服务器已停止")
        } catch (e: Exception) {
            AppLogger.e(TAG, "停止服务器失败", e)
        }
    }




    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 30000

            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = socket.getOutputStream()


            val requestLine = input.readLine() ?: return
            AppLogger.d(TAG, "请求: $requestLine")


            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            var path = parts[1]


            if (method != "GET") {
                sendError(output, 405, "Method Not Allowed")
                return
            }


            while (true) {
                val line = input.readLine()
                if (line.isNullOrEmpty()) break
            }


            path = URLDecoder.decode(path, "UTF-8")


            val queryIndex = path.indexOf('?')
            if (queryIndex > 0) {
                path = path.substring(0, queryIndex)
            }


            if (path == "/" || path.isEmpty()) {
                path = "/index.html"
            }


            val file = File(rootDirectory, path.removePrefix("/"))



            val rootCanonical = (rootDirectory ?: return).canonicalPath
            val fileCanonical = file.canonicalPath
            if (!fileCanonical.startsWith(rootCanonical + File.separator) && fileCanonical != rootCanonical) {
                AppLogger.w(TAG, "Path traversal blocked: $path -> $fileCanonical")
                sendError(output, 403, "Forbidden")
                return
            }

            if (!file.exists()) {


                val requestPath = path.removePrefix("/")
                val hasFileExtension = requestPath.substringAfterLast('/', requestPath).contains('.')
                if (!hasFileExtension) {
                    val indexFile = File(rootDirectory, "index.html")
                    if (indexFile.exists()) {
                        AppLogger.d(TAG, "SPA fallback to index.html for path: $path")
                        sendFile(output, indexFile)
                        return
                    }
                }

                AppLogger.w(TAG, "文件不存在: ${file.absolutePath}")
                sendError(output, 404, "Not Found")
                return
            }

            if (file.isDirectory) {

                val indexFile = File(file, "index.html")
                if (indexFile.exists()) {
                    sendFile(output, indexFile)
                } else {
                    sendError(output, 403, "Forbidden")
                }
            } else {
                sendFile(output, file)
            }

        } catch (e: Exception) {
            AppLogger.e(TAG, "处理请求失败", e)
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {

            }
        }
    }








    private val STREAM_THRESHOLD = 1 * 1024 * 1024L

    private fun sendFile(output: OutputStream, file: File) {
        val mimeType = getMimeType(file.name)
        val fileLength = file.length()

        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: $mimeType\r\n")
            append("Content-Length: $fileLength\r\n")

            append("Access-Control-Allow-Origin: ${buildLoopbackBaseUrl(actualPort)}\r\n")
            append("Access-Control-Allow-Methods: GET, HEAD, OPTIONS\r\n")
            append("Access-Control-Allow-Headers: *\r\n")
            append("Vary: Origin\r\n")
            append("Cache-Control: no-cache\r\n")
            append("X-Content-Type-Options: nosniff\r\n")
            if (crossOriginIsolationEnabled) {
                append("Cross-Origin-Opener-Policy: same-origin\r\n")
                append("Cross-Origin-Embedder-Policy: credentialless\r\n")
                append("Cross-Origin-Resource-Policy: same-origin\r\n")
                append("Origin-Agent-Cluster: ?1\r\n")
                append("Service-Worker-Allowed: /\r\n")
            }
            append("Connection: close\r\n")
            append("\r\n")
        }

        output.write(headers.toByteArray())


        if (fileLength > STREAM_THRESHOLD) {
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
        } else {
            output.write(file.readBytes())
        }
        output.flush()

        AppLogger.d(TAG, "发送文件: ${file.name} ($fileLength bytes, $mimeType)")
    }




    private fun sendError(output: OutputStream, code: Int, message: String) {
        val body = "<html><body><h1>$code $message</h1></body></html>"
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val headers = buildString {
            append("HTTP/1.1 $code $message\r\n")
            append("Content-Type: text/html; charset=utf-8\r\n")
            append("Content-Length: ${bodyBytes.size}\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }

        output.write(headers.toByteArray())
        output.write(bodyBytes)
        output.flush()
    }




    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "html", "htm" -> "text/html; charset=utf-8"
            "css" -> "text/css; charset=utf-8"
            "js", "mjs" -> "application/javascript; charset=utf-8"
            "json" -> "application/json; charset=utf-8"
            "wasm" -> "application/wasm"
            "onnx" -> "application/onnx"
            "xml" -> "application/xml; charset=utf-8"
            "txt" -> "text/plain; charset=utf-8"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "ico" -> "image/x-icon"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "ttf" -> "font/ttf"
            "otf" -> "font/otf"
            "eot" -> "application/vnd.ms-fontobject"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }
}
