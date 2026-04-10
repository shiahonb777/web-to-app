package com.webtoapp.util

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.junit.Test

class ThreadLocalCompatTest {

    @Test
    fun `threadLocalCompat provides isolated value per thread`() {
        val local = threadLocalCompat { mutableListOf<Int>() }
        local.get()!!.add(1)

        val otherThreadValue = AtomicReference<List<Int>>()
        val t = thread(start = true) {
            local.get()!!.add(2)
            otherThreadValue.set(local.get()!!.toList())
        }
        t.join()

        assertThat(local.get()!!.toList()).containsExactly(1)
        assertThat(otherThreadValue.get()).containsExactly(2)
    }

    @Test
    fun `string helper functions behave as expected`() {
        assertThat("abcdef".truncate(4)).isEqualTo("a...")
        assertThat("42".toIntOrDefault()).isEqualTo(42)
        assertThat("x".toIntOrDefault(7)).isEqualTo(7)
        assertThat("99".toLongOrDefault()).isEqualTo(99L)
        assertThat("x".toLongOrDefault(5L)).isEqualTo(5L)
    }

    @Test
    fun `file size and duration format helpers cover boundaries`() {
        assertThat(999L.toFileSizeString()).isEqualTo("999 B")
        assertThat(1_024L.toFileSizeString()).contains("KB")
        assertThat(1_048_576L.toFileSizeString()).contains("MB")

        assertThat(65_000L.toDurationString()).isEqualTo("1:05")
        assertThat(3_650_000L.toDurationString()).isEqualTo("1:00:50")
    }
}

