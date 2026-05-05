package com.webtoapp.core.webview

import com.webtoapp.core.logging.AppLogger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


/**
 * Lightweight in-process HTTP proxy that bridges WebView traffic to an upstream
 * SOCKS5 proxy. WebView's built-in proxy override does not reliably support the
 * `socks5://` scheme on many Android System WebView builds, so we expose a local
 * HTTP proxy on 127.0.0.1 instead and have it forward every request through a
 * manually performed SOCKS5 handshake (with optional username/password auth).
 *
 * Design goals:
 *   - Fully self-contained (no extra dependency)
 *   - Handles both HTTPS via CONNECT tunneling and plain HTTP via request
 *     line rewriting
 *   - Singleton because Android WebView's proxy override is process-global
 *   - Idempotent on repeated start with the same upstream
 *   - Daemon threads so the bridge never blocks app exit
 */
object LocalHttpToSocksBridge {

    private const val TAG = "LocalHttpToSocksBridge"
    private const val IO_BUFFER = 8 * 1024
    private const val MAX_HEADER_BYTES = 64 * 1024
    private const val SOCKS_CONNECT_TIMEOUT_MS = 15_000
    private const val CLIENT_READ_TIMEOUT_MS = 60_000

    data class Upstream(
        val host: String,
        val port: Int,
        val username: String = "",
        val password: String = ""
    )

    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var executor: ThreadPoolExecutor? = null
    @Volatile private var acceptThread: Thread? = null
    @Volatile private var running: Boolean = false
    @Volatile private var upstream: Upstream? = null
    @Volatile private var listenPort: Int = 0


    @Synchronized
    fun start(upstream: Upstream): Int {
        if (upstream.host.isBlank() || upstream.port <= 0) {
            AppLogger.w(TAG, "Invalid upstream SOCKS5 host=${upstream.host} port=${upstream.port}")
            return -1
        }
        val current = this.upstream
        if (running && current == upstream && listenPort > 0) {
            return listenPort
        }
        stopInternal()

        try {
            val socket = ServerSocket(0, 64, InetAddress.getByName("127.0.0.1"))
            serverSocket = socket
            listenPort = socket.localPort
            this.upstream = upstream
            running = true
            executor = ThreadPoolExecutor(
                0, 64, 30L, TimeUnit.SECONDS,
                SynchronousQueue()
            ) { r ->
                Thread(r, "LocalHttpToSocksBridge-Worker").apply { isDaemon = true }
            }.also { it.allowCoreThreadTimeOut(true) }

            val accept = Thread({ acceptLoop(socket) }, "LocalHttpToSocksBridge-Accept").apply {
                isDaemon = true
            }
            acceptThread = accept
            accept.start()

            AppLogger.i(
                TAG,
                "Local HTTP -> SOCKS5 bridge listening 127.0.0.1:$listenPort upstream=${upstream.host}:${upstream.port} auth=${upstream.username.isNotEmpty()}"
            )
            return listenPort
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start local HTTP -> SOCKS5 bridge", e)
            stopInternal()
            return -1
        }
    }


    @Synchronized
    fun stop() {
        if (!running && serverSocket == null) return
        AppLogger.d(TAG, "Stopping local HTTP -> SOCKS5 bridge on port $listenPort")
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
        upstream = null
    }


    private fun acceptLoop(socket: ServerSocket) {
        while (running) {
            val client: Socket = try {
                socket.accept()
            } catch (e: Exception) {
                if (running) {
                    AppLogger.w(TAG, "accept() error: ${e.message}")
                }
                break
            }
            val ex = executor
            if (ex == null) {
                try { client.close() } catch (_: Exception) {}
                break
            }
            try {
                ex.execute { handleClient(client) }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to dispatch client: ${e.message}")
                try { client.close() } catch (_: Exception) {}
            }
        }
    }


    private fun handleClient(client: Socket) {
        try {
            client.tcpNoDelay = true
            client.soTimeout = CLIENT_READ_TIMEOUT_MS
        } catch (_: Exception) {}

        try {
            val input = client.getInputStream()
            val output = client.getOutputStream()

            val firstLine = readHttpLine(input) ?: run {
                safeClose(client); return
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
                    safeClose(client); return
                }
            }

            val parts = firstLine.split(' ', limit = 3)
            if (parts.size < 3) {
                sendStatus(output, 400, "Bad Request")
                safeClose(client); return
            }
            val method = parts[0]
            val target = parts[1]

            if (method.equals("CONNECT", ignoreCase = true)) {
                handleConnect(client, output, target)
            } else {
                handleHttpForward(client, output, method, target, headerLines)
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
            safeClose(client); return
        }
        val host = hostPort.substring(0, sep).trim().trim('[', ']')
        val port = hostPort.substring(sep + 1).toIntOrNull() ?: 443

        val up = upstream
        if (up == null) {
            sendStatus(clientOut, 502, "Bad Gateway")
            safeClose(client); return
        }

        val target: Socket = try {
            Socks5Connector.connect(up.host, up.port, host, port, up.username, up.password)
        } catch (e: Exception) {
            AppLogger.w(TAG, "SOCKS5 CONNECT failed for $host:$port via ${up.host}:${up.port}: ${e.message}")
            sendStatus(clientOut, 502, "Bad Gateway")
            safeClose(client); return
        }

        try {
            clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
            clientOut.flush()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to send CONNECT 200: ${e.message}")
            safeClose(client); safeClose(target); return
        }

        bridge(client, target)
    }


    private fun handleHttpForward(
        client: Socket,
        clientOut: OutputStream,
        method: String,
        target: String,
        headers: List<String>
    ) {
        val uri = try { URI(target) } catch (_: Exception) { null }
        if (uri == null || uri.host.isNullOrBlank()) {
            sendStatus(clientOut, 400, "Bad Request")
            safeClose(client); return
        }
        val host = uri.host
        val port = if (uri.port > 0) uri.port else 80
        val rawPath = uri.rawPath ?: "/"
        val path = if (rawPath.isEmpty()) "/" else rawPath
        val pathAndQuery = uri.rawQuery?.let { "$path?$it" } ?: path

        val up = upstream
        if (up == null) {
            sendStatus(clientOut, 502, "Bad Gateway")
            safeClose(client); return
        }

        val targetSocket: Socket = try {
            Socks5Connector.connect(up.host, up.port, host, port, up.username, up.password)
        } catch (e: Exception) {
            AppLogger.w(TAG, "SOCKS5 connect (HTTP) failed for $host:$port: ${e.message}")
            sendStatus(clientOut, 502, "Bad Gateway")
            safeClose(client); return
        }

        try {
            val tOut = targetSocket.getOutputStream()
            val builder = StringBuilder()
            builder.append(method).append(' ').append(pathAndQuery).append(" HTTP/1.1\r\n")
            for (h in headers) {
                val lc = h.lowercase()
                if (lc.startsWith("proxy-connection:")) continue
                if (lc.startsWith("proxy-authorization:")) continue
                builder.append(h).append("\r\n")
            }
            builder.append("\r\n")
            tOut.write(builder.toString().toByteArray(StandardCharsets.ISO_8859_1))
            tOut.flush()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to forward HTTP request line: ${e.message}")
            safeClose(client); safeClose(targetSocket); return
        }

        bridge(client, targetSocket)
    }


    private fun bridge(a: Socket, b: Socket) {
        val ex = executor ?: run { safeClose(a); safeClose(b); return }
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
            val inp = from.getInputStream()
            val out = to.getOutputStream()
            val buf = ByteArray(IO_BUFFER)
            while (true) {
                val n = inp.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                out.flush()
            }
        } catch (_: Exception) {
            // Connection closed; ignore.
        }
    }


    private fun readHttpLine(inp: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = try { inp.read() } catch (_: Exception) { return null }
            if (b < 0) return if (sb.isEmpty()) null else sb.toString()
            if (b == 0x0A) {
                if (sb.isNotEmpty() && sb.last() == '\r') sb.setLength(sb.length - 1)
                return sb.toString()
            }
            sb.append(b.toChar())
            if (sb.length > MAX_HEADER_BYTES) return sb.toString()
        }
    }


    private fun sendStatus(out: OutputStream, code: Int, reason: String) {
        try {
            val msg = "HTTP/1.1 $code $reason\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
            out.write(msg.toByteArray(StandardCharsets.US_ASCII))
            out.flush()
        } catch (_: Exception) {}
    }


    private fun safeClose(s: Socket) {
        try { s.close() } catch (_: Exception) {}
    }


    /**
     * Minimal RFC 1928 / RFC 1929 SOCKS5 client. We do not rely on Java's built-in
     * `Proxy.Type.SOCKS` because its username/password authentication is inconsistent
     * across JVM/Android versions and it sometimes silently downgrades.
     */
    object Socks5Connector {

        fun connect(
            socksHost: String,
            socksPort: Int,
            targetHost: String,
            targetPort: Int,
            username: String = "",
            password: String = "",
            timeoutMs: Int = SOCKS_CONNECT_TIMEOUT_MS
        ): Socket {
            val socket = Socket()
            try {
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(socksHost, socksPort), timeoutMs)
                socket.soTimeout = timeoutMs

                val out = socket.getOutputStream()
                val inp = socket.getInputStream()


                val supportsAuth = username.isNotEmpty()
                val methods = if (supportsAuth) byteArrayOf(0x00, 0x02) else byteArrayOf(0x00)
                out.write(byteArrayOf(0x05.toByte(), methods.size.toByte()) + methods)
                out.flush()

                val methodResp = ByteArray(2)
                readFully(inp, methodResp)
                if (methodResp[0].toInt() and 0xff != 0x05) {
                    throw IOException("SOCKS5 version mismatch in method reply: ${methodResp[0].toInt() and 0xff}")
                }
                when (val chosen = methodResp[1].toInt() and 0xff) {
                    0x00 -> { /* no auth */ }
                    0x02 -> {
                        if (!supportsAuth) {
                            throw IOException("SOCKS5 server requires auth but no credentials provided")
                        }
                        performUserPassAuth(inp, out, username, password)
                    }
                    0xff -> throw IOException("SOCKS5 no acceptable auth method (server rejected)")
                    else -> throw IOException("SOCKS5 unsupported auth method: $chosen")
                }


                val hostBytes = targetHost.toByteArray(StandardCharsets.US_ASCII)
                if (hostBytes.size > 255) {
                    throw IOException("SOCKS5 destination host too long")
                }
                val req = ByteArrayOutputStream(7 + hostBytes.size)
                req.write(0x05) // version
                req.write(0x01) // CMD CONNECT
                req.write(0x00) // RSV
                req.write(0x03) // ATYP DOMAINNAME
                req.write(hostBytes.size)
                req.write(hostBytes)
                req.write((targetPort shr 8) and 0xff)
                req.write(targetPort and 0xff)
                out.write(req.toByteArray())
                out.flush()


                val head = ByteArray(4)
                readFully(inp, head)
                if (head[0].toInt() and 0xff != 0x05) {
                    throw IOException("SOCKS5 version mismatch in CONNECT reply")
                }
                val rep = head[1].toInt() and 0xff
                if (rep != 0x00) {
                    throw IOException("SOCKS5 CONNECT failed: rep=$rep ${connectErrorMessage(rep)}")
                }
                val atyp = head[3].toInt() and 0xff
                val skipLen = when (atyp) {
                    0x01 -> 4
                    0x04 -> 16
                    0x03 -> {
                        val lenByte = ByteArray(1)
                        readFully(inp, lenByte)
                        lenByte[0].toInt() and 0xff
                    }
                    else -> throw IOException("SOCKS5 unknown ATYP in reply: $atyp")
                }
                val skipBuf = ByteArray(skipLen + 2) // address + port
                readFully(inp, skipBuf)


                socket.soTimeout = 0
                return socket
            } catch (e: Exception) {
                try { socket.close() } catch (_: Exception) {}
                throw e
            }
        }


        private fun performUserPassAuth(
            inp: InputStream,
            out: OutputStream,
            username: String,
            password: String
        ) {
            val u = username.toByteArray(StandardCharsets.UTF_8)
            val p = password.toByteArray(StandardCharsets.UTF_8)
            if (u.size > 255 || p.size > 255) {
                throw IOException("SOCKS5 user/pass too long")
            }
            val req = ByteArrayOutputStream(3 + u.size + p.size)
            req.write(0x01)
            req.write(u.size)
            req.write(u)
            req.write(p.size)
            req.write(p)
            out.write(req.toByteArray())
            out.flush()

            val resp = ByteArray(2)
            readFully(inp, resp)
            if (resp[0].toInt() and 0xff != 0x01) {
                throw IOException("SOCKS5 auth subnegotiation version mismatch")
            }
            val status = resp[1].toInt() and 0xff
            if (status != 0x00) {
                throw IOException("SOCKS5 user/pass auth rejected (status=$status)")
            }
        }


        private fun connectErrorMessage(rep: Int): String = when (rep) {
            0x01 -> "general SOCKS server failure"
            0x02 -> "connection not allowed by ruleset"
            0x03 -> "network unreachable"
            0x04 -> "host unreachable"
            0x05 -> "connection refused"
            0x06 -> "TTL expired"
            0x07 -> "command not supported"
            0x08 -> "address type not supported"
            else -> "unknown ($rep)"
        }


        private fun readFully(inp: InputStream, buf: ByteArray) {
            var read = 0
            while (read < buf.size) {
                val n = inp.read(buf, read, buf.size - read)
                if (n < 0) throw IOException("Unexpected EOF after $read bytes")
                read += n
            }
        }
    }
}
