package com.webtoapp.core.webview

import android.content.Context
import android.util.Log
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 本地 HTTP 服务器
 * 
 * 用于提供本地 HTML 项目的文件服务，支持：
 * - 静态文件服务
 * - 正确的 MIME 类型
 * - 允许加载外部 CDN 资源（通过 HTTP 协议而非 file://）
 */
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
    private val executor = Executors.newCachedThreadPool()
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
            serverSocket = ServerSocket(port)
            actualPort = serverSocket!!.localPort
            isRunning.set(true)
            
            Log.i(TAG, "服务器启动在端口 $actualPort, 根目录: ${rootDir.absolutePath}")
            
            // Start接受连接的线程
            executor.execute {
                while (isRunning.get()) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        executor.execute { handleClient(clientSocket) }
                    } catch (e: Exception) {
                        if (isRunning.get()) {
                            Log.e(TAG, "接受连接失败", e)
                        }
                    }
                }
            }
            
            return "http://localhost:$actualPort"
        } catch (e: Exception) {
            Log.e(TAG, "启动服务器失败", e)
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
            serverSocket?.close()
            serverSocket = null
            Log.i(TAG, "服务器已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止服务器失败", e)
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
            Log.d(TAG, "请求: $requestLine")
            
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
            
            // Security检查：防止目录遍历
            if (path.contains("..")) {
                sendError(output, 403, "Forbidden")
                return
            }
            
            // Find文件
            val file = File(rootDirectory, path.removePrefix("/"))
            
            if (!file.exists()) {
                Log.w(TAG, "文件不存在: ${file.absolutePath}")
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
            Log.e(TAG, "处理请求失败", e)
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
    private fun sendFile(output: OutputStream, file: File) {
        val mimeType = getMimeType(file.name)
        val content = file.readBytes()
        
        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: $mimeType\r\n")
            append("Content-Length: ${content.size}\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Cache-Control: no-cache\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }
        
        output.write(headers.toByteArray())
        output.write(content)
        output.flush()
        
        Log.d(TAG, "发送文件: ${file.name} (${content.size} bytes, $mimeType)")
    }
    
    /**
     * 发送错误响应
     */
    private fun sendError(output: OutputStream, code: Int, message: String) {
        val body = "<html><body><h1>$code $message</h1></body></html>"
        val headers = buildString {
            append("HTTP/1.1 $code $message\r\n")
            append("Content-Type: text/html\r\n")
            append("Content-Length: ${body.length}\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }
        
        output.write(headers.toByteArray())
        output.write(body.toByteArray())
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
