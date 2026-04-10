package com.webtoapp.core.port

import com.google.common.truth.Truth.assertThat
import java.net.ServerSocket
import org.junit.After
import org.junit.Before
import org.junit.Test

class PortManagerTest {

    @Before
    fun before() {
        PortManager.releaseAll()
    }

    @After
    fun after() {
        PortManager.releaseAll()
    }

    @Test
    fun `allocate returns a port within requested range`() {
        val port = PortManager.allocate(PortManager.PortRange.PHP, "php:test")

        assertThat(port).isAtLeast(PortManager.PortRange.PHP.start)
        assertThat(port).isAtMost(PortManager.PortRange.PHP.end)
        assertThat(PortManager.isAllocated(port)).isTrue()
        assertThat(PortManager.getAllocation(port)?.owner).isEqualTo("php:test")
    }

    @Test
    fun `allocate uses preferred port when available`() {
        val preferred = PortManager.findAvailablePort(PortManager.PortRange.LOCAL_HTTP)

        val allocated = PortManager.allocate(
            range = PortManager.PortRange.LOCAL_HTTP,
            owner = "localhttp:test",
            preferredPort = preferred
        )

        assertThat(allocated).isEqualTo(preferred)
    }

    @Test
    fun `allocate falls back when preferred port is occupied`() {
        val preferred = PortManager.findAvailablePort(PortManager.PortRange.GENERAL)
        ServerSocket(preferred).use {
            val allocated = PortManager.allocate(
                range = PortManager.PortRange.GENERAL,
                owner = "general:test",
                preferredPort = preferred
            )

            assertThat(allocated).isNotEqualTo(preferred)
            assertThat(allocated).isAtLeast(PortManager.PortRange.GENERAL.start)
            assertThat(allocated).isAtMost(PortManager.PortRange.GENERAL.end)
        }
    }

    @Test
    fun `release removes allocation`() {
        val port = PortManager.allocate(PortManager.PortRange.NODEJS, "nodejs:test")
        assertThat(PortManager.isAllocated(port)).isTrue()

        PortManager.release(port)

        assertThat(PortManager.isAllocated(port)).isFalse()
    }

    @Test
    fun `convenience allocators register expected owner prefix`() {
        val nodePort = PortManager.allocateForNodeJs("projectA")
        val phpPort = PortManager.allocateForPhp("projectB")
        val pyPort = PortManager.allocateForPython("projectC")
        val goPort = PortManager.allocateForGo("projectD")

        assertThat(PortManager.getAllocation(nodePort)?.owner).isEqualTo("nodejs:projectA")
        assertThat(PortManager.getAllocation(phpPort)?.owner).isEqualTo("php:projectB")
        assertThat(PortManager.getAllocation(pyPort)?.owner).isEqualTo("python:projectC")
        assertThat(PortManager.getAllocation(goPort)?.owner).isEqualTo("go:projectD")
    }

    @Test
    fun `releaseAll clears all tracked allocations`() {
        PortManager.allocateForNodeJs("a")
        PortManager.allocateForPhp("b")
        PortManager.allocateForPython("c")

        assertThat(PortManager.getAllAllocations()).isNotEmpty()
        PortManager.releaseAll()
        assertThat(PortManager.getAllAllocations()).isEmpty()
    }
}

