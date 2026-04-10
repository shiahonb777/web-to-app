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

/**
 * 本地 HTTP 服务器
 * 
 * 用于提供本地 HTML 项目的文件服务，支持：
 * - 静态文件服务
 * - 正确的 MIME 类型
 * - 允许加载外部 CDN 资源（通过 HTTP 协议而非 file://）
 */
@SuppressLint("StaticFieldLeak")
class LocalHttpServer(
    private val context: Context,
    private val port: Int = 0  // 0 表示自动分配端口
) {
    companion object {
        private const val TAG = "LocalHttpServer"
        
        @Volatile
        private var instance: LocalHttpServer? = null
        
        fun getInstance(context: Context): LocalHttpServer {
            return instance ?: synchronized(this) {
                instance ?: LocalHttpServer(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private var serverSocket: ServerSocket? = null
    // Bounded thread pool to prevent DoS via unlimited thread creation
    private val executor = ThreadPoolExecutor(
        2, 16, 60L, TimeUnit.SECONDS, LinkedBlockingQueue(64)
    )
    private val isRunning = AtomicBoolean(false)
    
    // 当前服务的根目录
    private var rootDirectory: File? = null
    
    // 实际使用的端口
    var actualPort: Int = 0
        private set
    
    /**
     * 启动服务器
     * @param rootDir 要服务的根目录
     * @return 服务器 URL (如 http://localhost:8080)
     */
    @Synchronized
    fun start(rootDir: File): String {
        if (isRunning.get() && rootDirectory == rootDir) {
            // 已经在服务同一个目录
            return "http://localhost:$actualPort"
        }
        
        // Stop之前的服务
        stop()
        
        rootDirectory = rootDir
        
        try {
            // 使用 PortManager 分配端口
            val allocatedPort = if (port > 0) {
                PortManager.allocateForLocalHttp(rootDir.name, port)
            } else {
                PortManager.allocateForLocalHttp(rootDir.name)
            }
            
            // SECURITY: Bind to loopback only — prevent other devices on the same
            // network from accessing the local file server.
            serverSocket = ServerSocket(allocatedPort, 50, InetAddress.getLoopbackAddress())
            actualPort = serverSocket?.localPort ?: allocatedPort
            isRunning.set(true)
            
            AppLogger.i(TAG, "服务器启动在端口 $actualPort, 根目录: ${rootDir.absolutePath}")
            
            // Start接受连接的线程
            executor.execute {
                while (isRunning.get()) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        executor.execute { handleClient(clientSocket) }
                    } catch (e: Exception) {
                        if (isRunning.get()) {
                            AppLogger.e(TAG, "接受连接失败", e)
                        }
                    }
                }
            }
            
            return "http://localhost:$actualPort"
        } catch (e: Exception) {
            AppLogger.e(TAG, "启动服务器失败", e)
            throw e
        }
    }
    
    /**
     * 停止服务器
     */
    @Synchronized
    fun stop() {
        if (!isRunning.get()) return
        
        isRunning.set(false)
        try {
            // 释放端口
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
    
    /**
     * 处理客户端请求
     */
    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 30000  // 30秒超时
            
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = socket.getOutputStream()
            
            // 读取请求行
            val requestLine = input.readLine() ?: return
            AppLogger.d(TAG, "请求: $requestLine")
            
            // Parse请求
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            
            val method = parts[0]
            var path = parts[1]
            
            // 只处理 GET 请求
            if (method != "GET") {
                sendError(output, 405, "Method Not Allowed")
                return
            }
            
            // 读取并忽略请求头
            while (true) {
                val line = input.readLine()
                if (line.isNullOrEmpty()) break
            }
            
            // 解码 URL
            path = URLDecoder.decode(path, "UTF-8")
            
            // 移除查询参数
            val queryIndex = path.indexOf('?')
            if (queryIndex > 0) {
                path = path.substring(0, queryIndex)
            }
            
            // Default文件
            if (path == "/" || path.isEmpty()) {
                path = "/index.html"
            }
            
            // Find文件
            val file = File(rootDirectory, path.removePrefix("/"))
            
            // SECURITY: Validate resolved path is within rootDirectory using canonicalPath.
            // This prevents path traversal via "..", symlinks, URL-encoded sequences, etc.
            val rootCanonical = (rootDirectory ?: return).canonicalPath
            val fileCanonical = file.canonicalPath
            if (!fileCanonical.startsWith(rootCanonical + File.separator) && fileCanonical != rootCanonical) {
                AppLogger.w(TAG, "Path traversal blocked: $path -> $fileCanonical")
                sendError(output, 403, "Forbidden")
                return
            }
            
            if (!file.exists()) {
                // SPA fallback: when route has no file extension, return index.html.
                // This avoids blank screens on React/Vue history mode routes.
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
                // 尝试 index.html
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
                // ignore
            }
        }
    }
    
    /**
     * 发送文件响应
     */
    /**
     * Streaming file size threshold (1 MB). Files larger than this are streamed
     * instead of being read entirely into memory to avoid OOM on large media.
     */
    private val STREAM_THRESHOLD = 1 * 1024 * 1024L
    
    private fun sendFile(output: OutputStream, file: File) {
        val mimeType = getMimeType(file.name)
        val fileLength = file.length()
        
        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: $mimeType\r\n")
            append("Content-Length: $fileLength\r\n")
            // Only allow same-origin requests (server is localhost-only anyway)
            append("Access-Control-Allow-Origin: http://localhost:$actualPort\r\n")
            append("Cache-Control: no-cache\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }
        
        output.write(headers.toByteArray())
        
        // Stream large files to avoid OOM
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
    
    /**
     * 发送错误响应
     */
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
    
    /**
     * 获取 MIME 类型
     */
    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "html", "htm" -> "text/html; charset=utf-8"
            "css" -> "text/css; charset=utf-8"
            "js", "mjs" -> "application/javascript; charset=utf-8"
            "json" -> "application/json; charset=utf-8"
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
