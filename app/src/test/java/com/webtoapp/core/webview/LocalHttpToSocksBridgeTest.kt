package com.webtoapp.core.webview

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


/**
 * Validates that [LocalHttpToSocksBridge.Socks5Connector] emits
 * RFC 1928 / RFC 1929 compliant frames.
 *
 * We run a tiny mock SOCKS5 server on a loopback port that captures every
 * byte sent by the connector and replies with canned success responses.
 */
class LocalHttpToSocksBridgeTest {

    private val servers = mutableListOf<MockSocks5Server>()

    @After
    fun tearDown() {
        servers.forEach { it.stop() }
        servers.clear()
    }

    @Test
    fun `socks5 no-auth handshake sends RFC compliant frames`() {
        val mock = startMock(MockSocks5Server.AuthMode.NONE)

        val socket = LocalHttpToSocksBridge.Socks5Connector.connect(
            socksHost = "127.0.0.1",
            socksPort = mock.port,
            targetHost = "example.com",
            targetPort = 443
        )
        try {
            val capture = mock.awaitCapture()
            assertNotNull("Server must observe a client", capture)
            checkNotNull(capture)


            assertEquals(0x05, capture.greetingVersion)
            assertTrue(
                "Greeting must offer the no-auth method (0x00)",
                capture.greetingMethods.contains(0x00.toByte())
            )


            assertEquals("example.com", capture.targetHost)
            assertEquals(443, capture.targetPort)
            assertEquals(0x01, capture.connectCmd)
            assertEquals(0x03, capture.connectAtyp)
        } finally {
            socket.close()
        }
    }

    @Test
    fun `socks5 user-pass auth handshake sends correct credentials`() {
        val mock = startMock(MockSocks5Server.AuthMode.USERPASS_ACCEPT)

        val socket = LocalHttpToSocksBridge.Socks5Connector.connect(
            socksHost = "127.0.0.1",
            socksPort = mock.port,
            targetHost = "10.20.30.40",
            targetPort = 8080,
            username = "alice",
            password = "p@ss w0rd"
        )
        try {
            val capture = mock.awaitCapture()
            assertNotNull(capture)
            checkNotNull(capture)


            assertTrue(
                "Greeting must include user/pass method (0x02)",
                capture.greetingMethods.contains(0x02.toByte())
            )
            assertEquals("alice", capture.authUsername)
            assertEquals("p@ss w0rd", capture.authPassword)


            assertEquals("10.20.30.40", capture.targetHost)
            assertEquals(8080, capture.targetPort)
        } finally {
            socket.close()
        }
    }

    @Test
    fun `socks5 user-pass auth failure surfaces as IOException`() {
        val mock = startMock(MockSocks5Server.AuthMode.USERPASS_REJECT)

        try {
            LocalHttpToSocksBridge.Socks5Connector.connect(
                socksHost = "127.0.0.1",
                socksPort = mock.port,
                targetHost = "x.test",
                targetPort = 80,
                username = "bad",
                password = "bad"
            )
            throw AssertionError("expected IOException for rejected SOCKS5 auth")
        } catch (e: IOException) {
            assertTrue(
                "Error message should mention rejection (was: ${e.message})",
                e.message?.contains("auth", ignoreCase = true) == true
            )
        }
    }

    @Test
    fun `socks5 server rejecting CONNECT command throws IOException`() {
        val mock = startMock(MockSocks5Server.AuthMode.NONE, connectReplyCode = 0x05)

        try {
            LocalHttpToSocksBridge.Socks5Connector.connect(
                socksHost = "127.0.0.1",
                socksPort = mock.port,
                targetHost = "blocked.test",
                targetPort = 443
            )
            throw AssertionError("expected IOException when SOCKS5 CONNECT returns failure")
        } catch (e: IOException) {
            assertTrue(
                "Error message should mention CONNECT failure (was: ${e.message})",
                e.message?.contains("CONNECT", ignoreCase = true) == true
            )
        }
    }

    private fun startMock(
        authMode: MockSocks5Server.AuthMode,
        connectReplyCode: Int = 0x00
    ): MockSocks5Server {
        val server = MockSocks5Server(authMode = authMode, connectReplyCode = connectReplyCode)
        server.start()
        servers.add(server)
        return server
    }
}


/**
 * Single-shot in-process SOCKS5 server used purely to verify byte-level framing
 * emitted by the connector under test. Accepts exactly one client and records
 * every relevant field.
 */
private class MockSocks5Server(
    private val authMode: AuthMode,
    private val connectReplyCode: Int
) {

    enum class AuthMode { NONE, USERPASS_ACCEPT, USERPASS_REJECT }

    data class Capture(
        val greetingVersion: Int,
        val greetingMethods: ByteArray,
        val authUsername: String?,
        val authPassword: String?,
        val connectCmd: Int,
        val connectAtyp: Int,
        val targetHost: String,
        val targetPort: Int
    )

    @Volatile
    var port: Int = 0
        private set

    private var serverSocket: ServerSocket? = null
    private val captureRef = AtomicReference<Capture?>()
    private val captureLatch = CountDownLatch(1)
    private lateinit var thread: Thread

    fun start() {
        val socket = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        serverSocket = socket
        port = socket.localPort
        thread = Thread({ runOnce(socket) }, "MockSocks5Server").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        try { serverSocket?.close() } catch (_: Exception) {}
        try { thread.join(500) } catch (_: Exception) {}
    }

    fun awaitCapture(): Capture? {
        captureLatch.await(5, TimeUnit.SECONDS)
        return captureRef.get()
    }

    private fun runOnce(server: ServerSocket) {
        try {
            server.accept().use { client ->
                val inp = client.getInputStream()
                val out = client.getOutputStream()
                handleClient(inp, out)
            }
        } catch (_: Exception) {
            // Server closed or test finished early; ignore.
        }
    }

    private fun handleClient(inp: InputStream, out: OutputStream) {
        val ver = inp.read()
        val nMethods = inp.read()
        if (ver < 0 || nMethods <= 0) return
        val methods = readNBytes(inp, nMethods)


        val chosenMethod: Int = when (authMode) {
            AuthMode.NONE -> if (methods.contains(0x00.toByte())) 0x00 else 0xff
            AuthMode.USERPASS_ACCEPT,
            AuthMode.USERPASS_REJECT -> if (methods.contains(0x02.toByte())) 0x02 else 0xff
        }
        out.write(byteArrayOf(0x05.toByte(), chosenMethod.toByte()))
        out.flush()
        if (chosenMethod == 0xff) return


        var username: String? = null
        var password: String? = null
        if (chosenMethod == 0x02) {
            val authVer = inp.read()
            if (authVer != 0x01) return
            val ulen = inp.read()
            val ubytes = readNBytes(inp, ulen)
            val plen = inp.read()
            val pbytes = readNBytes(inp, plen)
            username = String(ubytes, StandardCharsets.UTF_8)
            password = String(pbytes, StandardCharsets.UTF_8)
            val authStatus: Int = if (authMode == AuthMode.USERPASS_ACCEPT) 0x00 else 0x01
            out.write(byteArrayOf(0x01.toByte(), authStatus.toByte()))
            out.flush()
            if (authStatus != 0x00) return
        }


        val cmdVer = inp.read()
        val cmd = inp.read()
        inp.read() // RSV
        val atyp = inp.read()
        if (cmdVer != 0x05 || cmd < 0 || atyp < 0) return

        val host: String = when (atyp) {
            0x01 -> {
                val v4 = readNBytes(inp, 4)
                v4.joinToString(".") { (it.toInt() and 0xff).toString() }
            }
            0x03 -> {
                val len = inp.read()
                val name = readNBytes(inp, len)
                String(name, StandardCharsets.US_ASCII)
            }
            0x04 -> {
                val v6 = readNBytes(inp, 16)
                v6.joinToString(":")
            }
            else -> ""
        }
        val portHi = inp.read()
        val portLo = inp.read()
        val port = (portHi shl 8) or portLo

        captureRef.set(
            Capture(
                greetingVersion = ver,
                greetingMethods = methods,
                authUsername = username,
                authPassword = password,
                connectCmd = cmd,
                connectAtyp = atyp,
                targetHost = host,
                targetPort = port
            )
        )
        captureLatch.countDown()


        val replyCode = connectReplyCode and 0xff
        val reply = byteArrayOf(
            0x05.toByte(),
            replyCode.toByte(),
            0x00.toByte(),
            0x01.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte()
        )
        out.write(reply)
        out.flush()


        if (replyCode == 0x00) {
            try {
                while (true) {
                    val b = inp.read()
                    if (b < 0) break
                }
            } catch (_: Exception) { }
        }
    }

    private fun readNBytes(inp: InputStream, n: Int): ByteArray {
        val buf = ByteArray(n)
        var read = 0
        while (read < n) {
            val r = inp.read(buf, read, n - read)
            if (r < 0) throw IOException("EOF after $read of $n bytes")
            read += r
        }
        return buf
    }
}
