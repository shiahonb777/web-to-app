package com.webtoapp.core.port

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.webtoapp.util.isAliveCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProcessPortScannerTest {

    private lateinit var context: Context

    private fun startSleepProcess(seconds: Int): Process {
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        return if (osName.contains("win")) {
            ProcessBuilder("cmd", "/c", "ping 127.0.0.1 -n ${seconds + 1} >NUL").start()
        } else {
            ProcessBuilder("sh", "-c", "sleep $seconds").start()
        }
    }

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        PortManager.releaseAll()
    }

    @After
    fun tearDown() {
        PortManager.releaseAll()
    }

    @Test
    fun `scanAllPorts reports allocated local http service metadata`() = runBlocking {
        val port = PortManager.allocate(PortManager.PortRange.LOCAL_HTTP, "localhttp:demo")
        assertThat(port).isGreaterThan(0)

        val serverSocket = ServerSocket(port)
        val serverThread = Thread {
            try {
                val socket = serverSocket.accept()
                socket.soTimeout = 1_000
                BufferedReader(InputStreamReader(socket.getInputStream())).use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                    }
                }

                socket.getOutputStream().use { out ->
                    out.write(
                        (
                            "HTTP/1.1 200 OK\r\n" +
                                "Connection: close\r\n" +
                                "Content-Length: 0\r\n\r\n"
                            ).toByteArray()
                    )
                    out.flush()
                }
                socket.close()
            } catch (_: Exception) {
            }
        }
        serverThread.start()

        try {
            val services = ProcessPortScanner.scanAllPorts(context)
            val service = services.first { it.port == port }

            assertThat(service.type).isEqualTo(ProcessPortScanner.ServiceType.LOCAL_HTTP)
            assertThat(service.owner).isEqualTo("demo")
            assertThat(service.url).isEqualTo("http://127.0.0.1:$port")
            assertThat(service.port).isEqualTo(port)
        } finally {
            serverSocket.close()
            serverThread.join(500)
        }
    }

    @Test
    fun `killProcess stops registered process and releases port allocation`() = runBlocking {
        val port = PortManager.allocate(PortManager.PortRange.NODEJS, "nodejs:test")
        val process = startSleepProcess(seconds = 30)
        PortManager.registerProcess(port, process)
        assertThat(process.isAliveCompat()).isTrue()

        val killed = ProcessPortScanner.killProcess(port)

        assertThat(killed).isTrue()
        assertThat(PortManager.isAllocated(port)).isFalse()
        assertThat(process.isAliveCompat()).isFalse()
    }

    @Test
    fun `killAllProcesses clears all tracked ports and returns killed count`() = runBlocking {
        val first = PortManager.allocate(PortManager.PortRange.PHP, "php:a")
        val second = PortManager.allocate(PortManager.PortRange.PYTHON, "python:b")
        assertThat(first).isGreaterThan(0)
        assertThat(second).isGreaterThan(0)

        val killed = ProcessPortScanner.killAllProcesses(context)

        assertThat(killed).isEqualTo(2)
        assertThat(PortManager.getAllAllocations()).isEmpty()
    }
}
