package com.webtoapp.core.webview

import com.webtoapp.core.dns.DnsManager
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.DnsConfig
import com.webtoapp.data.model.HostMappingEntry
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * A loopback HTTP proxy that rewires selected hosts to fixed IPs while preserving
 * the original Host header and HTTPS CONNECT target. This gives WebView a
 * hosts-like mapping layer without rewriting page URLs.
 */
object LocalHttpHostMappingBridge {

    private const val TAG = "LocalHttpHostMappingBridge"
    private const val IO_BUFFER = 8 * 1024
    private const val MAX_HEADER_BYTES = 64 * 1024
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val CLIENT_READ_TIMEOUT_MS = 60_000

    data class Config(
        val mappings: List<HostMappingEntry>,
        val dnsMode: String = "SYSTEM",
        val dnsConfig: DnsConfig = DnsConfig()
    )

    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var executor: ThreadPoolExecutor? = null
    @Volatile private var acceptThread: Thread? = null
    @Volatile private var running: Boolean = false
    @Volatile private var currentConfig: Config? = null
    @Volatile private var normalizedMappings: Map<String, String> = emptyMap()
    @Volatile private var listenPort: Int = 0
    @Volatile private var dnsManager: DnsManager? = null

    @Synchronized
    fun start(config: Config, dnsManager: DnsManager?): Int {
        val preparedMappings = buildNormalizedMappings(config.mappings)
        if (preparedMappings.isEmpty()) {
            AppLogger.w(TAG, "No valid hosts mappings, skip bridge startup")
            return -1
        }
        val normalizedConfig = config.copy(mappings = preparedMappings.entries.map { HostMappingEntry(it.key, it.value) })
        if (running && currentConfig == normalizedConfig && listenPort > 0) {
            this.dnsManager = dnsManager
            return listenPort
        }

        stopInternal()
        try {
            val socket = ServerSocket(0, 64, InetAddress.getByName("127.0.0.1"))
            serverSocket = socket
            listenPort = socket.localPort
            currentConfig = normalizedConfig
            normalizedMappings = preparedMappings
            this.dnsManager = dnsManager
            running = true
            executor = ThreadPoolExecutor(
                0,
                64,
                30L,
                TimeUnit.SECONDS,
                SynchronousQueue()
            ) { runnable ->
                Thread(runnable, "LocalHttpHostMappingBridge-Worker").apply { isDaemon = true }
            }.also { it.allowCoreThreadTimeOut(true) }

            val accept = Thread({ acceptLoop(socket) }, "LocalHttpHostMappingBridge-Accept").apply {
                isDaemon = true
            }
            acceptThread = accept
            accept.start()

            AppLogger.i(TAG, "Hosts mapping proxy listening on 127.0.0.1:$listenPort with ${preparedMappings.size} rules")
            return listenPort
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start hosts mapping bridge", e)
            stopInternal()
            return -1
        }
    }

    @Synchronized
    fun stop() {
        if (!running && serverSocket == null) return
        AppLogger.d(TAG, "Stopping hosts mapping bridge on port $listenPort")
        stopInternal()
    }

    fun getListenPort(): Int = listenPort

    fun isRunning(): Boolean = running

    private fun stopInternal() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        try { executor?.shutdownNow() } catch (_: Exception) {}
        executor = null
        acceptThread = null
        listenPort = 0
        currentConfig = null
        normalizedMappings = emptyMap()
        dnsManager = null
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (running) {
            val client = try {
                socket.accept()
            } catch (e: Exception) {
                if (running) {
                    AppLogger.w(TAG, "accept() error: ${e.message}")
                }
                break
            }
            val ex = executor
            if (ex == null) {
                safeClose(client)
                break
            }
            try {
                ex.execute { handleClient(client) }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to dispatch mapped-host client: ${e.message}")
                safeClose(client)
            }
        }
    }

    private fun handleClient(client: Socket) {
        try {
            client.tcpNoDelay = true
            client.soTimeout = CLIENT_READ_TIMEOUT_MS
        } catch (_: Exception) {}

        try {
            val input = BufferedInputStream(client.getInputStream())
            val output = client.getOutputStream()

            val firstLine = readHttpLine(input) ?: run {
                safeClose(client)
                return
            }
            val headerLines = mutableListOf<String>()
            var headerBytes = firstLine.length
            while (true) {
                val line = readHttpLine(input) ?: break
                if (line.isEmpty()) break
                headerLines.add(line)
                headerBytes += line.length
                if (headerBytes > MAX_HEADER_BYTES) {
                    sendStatus(output, 431, "Request Header Fields Too Large")
                    safeClose(client)
                    return
                }
            }

            val parts = firstLine.split(' ', limit = 3)
            if (parts.size < 3) {
                sendStatus(output, 400, "Bad Request")
                safeClose(client)
                return
            }
            val method = parts[0]
            val target = parts[1]
            val version = parts[2]

            if (method.equals("CONNECT", ignoreCase = true)) {
                handleConnect(client, output, target)
            } else {
                handleHttpForward(client, input, output, method, target, version, headerLines)
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "client error: ${e.message}")
            safeClose(client)
        }
    }

    private fun handleConnect(
        client: Socket,
        clientOut: OutputStream,
        hostPort: String
    ) {
        val sep = hostPort.lastIndexOf(':')
        if (sep <= 0) {
            sendStatus(clientOut, 400, "Bad Request")
            safeClose(client)
            return
        }
        val host = hostPort.substring(0, sep).trim().trim('[', ']')
        val port = hostPort.substring(sep + 1).toIntOrNull() ?: 443

        val mappedIp = normalizedMappings[host.lowercase(Locale.ROOT)]
        val targetSocket = try {
            connectTarget(host = host, mappedIp = mappedIp, port = port)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Mapped CONNECT failed for $host:$port mappedIp=$mappedIp: ${e.message}")
            sendStatus(clientOut, 502, "Bad Gateway")
            safeClose(client)
            return
        }

        try {
            clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
            clientOut.flush()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to send CONNECT 200: ${e.message}")
            safeClose(client)
            safeClose(targetSocket)
            return
        }

        bridge(client, targetSocket)
    }

    private fun handleHttpForward(
        client: Socket,
        clientIn: BufferedInputStream,
        clientOut: OutputStream,
        method: String,
        target: String,
        version: String,
        headers: List<String>
    ) {
        val requestUri = try { URI(target) } catch (_: Exception) { null }
        val hostHeader = headers.firstOrNull { it.startsWith("Host:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
        val host = requestUri?.host ?: hostHeader?.substringBefore(':')?.trim()
        if (host.isNullOrBlank()) {
            sendStatus(clientOut, 400, "Bad Request")
            safeClose(client)
            return
        }

        val port = when {
            requestUri?.port != null && requestUri.port > 0 -> requestUri.port
            hostHeader?.substringAfter(':', "")?.toIntOrNull() != null -> hostHeader.substringAfter(':').toInt()
            else -> 80
        }
        val rawPath = requestUri?.rawPath
        val path = if (rawPath.isNullOrEmpty()) "/" else rawPath
        val pathAndQuery = requestUri?.rawQuery?.let { "$path?$it" } ?: path
        val mappedIp = normalizedMappings[host.lowercase(Locale.ROOT)]

        val targetSocket = try {
            connectTarget(host = host, mappedIp = mappedIp, port = port)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Mapped HTTP connect failed for $host:$port mappedIp=$mappedIp: ${e.message}")
            sendStatus(clientOut, 502, "Bad Gateway")
            safeClose(client)
            return
        }

        try {
            val targetOut = targetSocket.getOutputStream()
            val builder = StringBuilder()
            builder.append(method).append(' ').append(pathAndQuery).append(' ').append(version).append("\r\n")
            headers.forEach { line ->
                val lower = line.lowercase(Locale.ROOT)
                if (lower.startsWith("proxy-connection:")) return@forEach
                if (lower.startsWith("proxy-authorization:")) return@forEach
                builder.append(line).append("\r\n")
            }
            builder.append("\r\n")
            targetOut.write(builder.toString().toByteArray(StandardCharsets.ISO_8859_1))
            targetOut.flush()

            pumpRequestBodyIfPresent(clientIn, targetOut, headers)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to forward mapped HTTP request: ${e.message}")
            safeClose(client)
            safeClose(targetSocket)
            return
        }

        bridge(client, targetSocket)
    }

    private fun connectTarget(host: String, mappedIp: String?, port: Int): Socket {
        val socket = Socket()
        try {
            socket.tcpNoDelay = true
            socket.soTimeout = CLIENT_READ_TIMEOUT_MS
            val endpoint = when {
                !mappedIp.isNullOrBlank() -> InetSocketAddress(mappedIp, port)
                else -> InetSocketAddress(resolveInetAddress(host), port)
            }
            socket.connect(endpoint, CONNECT_TIMEOUT_MS)
            return socket
        } catch (e: Exception) {
            safeClose(socket)
            throw e
        }
    }

    private fun resolveInetAddress(host: String): InetAddress {
        val manager = dnsManager
        val config = currentConfig
        if (
            manager != null &&
            config != null &&
            config.dnsMode != "SYSTEM" &&
            config.dnsConfig.effectiveDohUrl.isNotBlank()
        ) {
            val resolved = manager.resolveWithDoh(host)
            resolved.firstOrNull()?.let { return it }
            throw IOException("No DoH DNS result for $host")
        }
        return InetAddress.getByName(host)
    }

    private fun pumpRequestBodyIfPresent(
        clientIn: BufferedInputStream,
        targetOut: OutputStream,
        headers: List<String>
    ) {
        val contentLength = headers.firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.toLongOrNull()
        if (contentLength != null && contentLength > 0) {
            copyExact(clientIn, targetOut, contentLength)
            targetOut.flush()
            return
        }

        val transferEncoding = headers.firstOrNull { it.startsWith("Transfer-Encoding:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.lowercase(Locale.ROOT)
        if (transferEncoding == "chunked") {
            forwardChunkedBody(clientIn, targetOut)
            targetOut.flush()
        }
    }

    private fun copyExact(input: InputStream, output: OutputStream, byteCount: Long) {
        var remaining = byteCount
        val buffer = ByteArray(IO_BUFFER)
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) throw IOException("Unexpected EOF while forwarding request body")
            output.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun forwardChunkedBody(input: BufferedInputStream, output: OutputStream) {
        while (true) {
            val chunkSizeLine = readHttpLine(input) ?: throw IOException("Unexpected EOF reading chunk size")
            output.write(chunkSizeLine.toByteArray(StandardCharsets.ISO_8859_1))
            output.write("\r\n".toByteArray(StandardCharsets.US_ASCII))

            val chunkSize = chunkSizeLine.substringBefore(';').trim().toIntOrNull(16)
                ?: throw IOException("Invalid chunk size: $chunkSizeLine")
            if (chunkSize == 0) {
                while (true) {
                    val trailerLine = readHttpLine(input) ?: throw IOException("Unexpected EOF reading chunk trailer")
                    output.write(trailerLine.toByteArray(StandardCharsets.ISO_8859_1))
                    output.write("\r\n".toByteArray(StandardCharsets.US_ASCII))
                    if (trailerLine.isEmpty()) {
                        return
                    }
                }
            }

            copyExact(input, output, chunkSize.toLong())
            copyExact(input, output, 2)
        }
    }

    private fun bridge(a: Socket, b: Socket) {
        val ex = executor ?: run {
            safeClose(a)
            safeClose(b)
            return
        }
        val pump = ex.submit {
            copyStream(a, b)
            try { b.shutdownOutput() } catch (_: Exception) {}
        }
        copyStream(b, a)
        try { a.shutdownOutput() } catch (_: Exception) {}
        try { pump.get(2, TimeUnit.SECONDS) } catch (_: Exception) {}
        safeClose(a)
        safeClose(b)
    }

    private fun copyStream(from: Socket, to: Socket) {
        try {
            val input = from.getInputStream()
            val output = to.getOutputStream()
            val buffer = ByteArray(IO_BUFFER)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                output.flush()
            }
        } catch (_: SocketTimeoutException) {
            // Ignore idle timeout on one side.
        } catch (_: Exception) {
            // Connection closed; ignore.
        }
    }

    private fun buildNormalizedMappings(entries: List<HostMappingEntry>): Map<String, String> {
        val result = linkedMapOf<String, String>()
        entries.forEach { entry ->
            val host = normalizeHost(entry.host) ?: return@forEach
            val ip = normalizeIpv4(entry.ip) ?: return@forEach
            result[host] = ip
        }
        return result
    }

    private fun normalizeHost(raw: String): String? {
        val host = raw.trim().trim('.').lowercase(Locale.ROOT)
        if (host.isBlank()) return null
        if (!host.contains('.')) return null
        if (host.any { it.isWhitespace() || it == '/' || it == ':' }) return null
        return host
    }

    private fun normalizeIpv4(raw: String): String? {
        val value = raw.trim()
        val parts = value.split('.')
        if (parts.size != 4) return null
        if (parts.any { part -> part.isBlank() || part.toIntOrNull() == null || part.toInt() !in 0..255 }) return null
        return parts.joinToString(".") { it.toInt().toString() }
    }

    private fun readHttpLine(input: InputStream): String? {
        val bytes = ByteArrayOutputStream()
        while (true) {
            val next = try { input.read() } catch (_: Exception) { return null }
            if (next < 0) return if (bytes.size() == 0) null else bytes.toString(StandardCharsets.ISO_8859_1.name())
            if (next == 0x0A) {
                val raw = bytes.toByteArray()
                val length = if (raw.isNotEmpty() && raw.last() == '\r'.code.toByte()) raw.size - 1 else raw.size
                return String(raw, 0, length, StandardCharsets.ISO_8859_1)
            }
            bytes.write(next)
            if (bytes.size() > MAX_HEADER_BYTES) {
                return bytes.toString(StandardCharsets.ISO_8859_1.name())
            }
        }
    }

    private fun sendStatus(out: OutputStream, code: Int, reason: String) {
        try {
            val msg = "HTTP/1.1 $code $reason\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
            out.write(msg.toByteArray(StandardCharsets.US_ASCII))
            out.flush()
        } catch (_: Exception) {}
    }

    private fun safeClose(socket: Socket) {
        try { socket.close() } catch (_: Exception) {}
    }
}
