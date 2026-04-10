package com.webtoapp.core.network

import org.junit.Test
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit

/**
 * NetworkModule 单元测试
 */
class NetworkModuleTest {

    @Test
    fun `defaultClient is singleton`() {
        val client1 = NetworkModule.defaultClient
        val client2 = NetworkModule.defaultClient
        assertThat(client1).isSameInstanceAs(client2)
    }

    @Test
    fun `streamingClient is singleton`() {
        val client1 = NetworkModule.streamingClient
        val client2 = NetworkModule.streamingClient
        assertThat(client1).isSameInstanceAs(client2)
    }

    @Test
    fun `downloadClient is singleton`() {
        val client1 = NetworkModule.downloadClient
        val client2 = NetworkModule.downloadClient
        assertThat(client1).isSameInstanceAs(client2)
    }

    @Test
    fun `defaultClient has correct timeouts`() {
        val client = NetworkModule.defaultClient
        assertThat(client.connectTimeoutMillis).isEqualTo(TimeUnit.SECONDS.toMillis(15).toInt())
        assertThat(client.readTimeoutMillis).isEqualTo(TimeUnit.SECONDS.toMillis(30).toInt())
        assertThat(client.writeTimeoutMillis).isEqualTo(TimeUnit.SECONDS.toMillis(30).toInt())
    }

    @Test
    fun `streamingClient has extended read timeout`() {
        val client = NetworkModule.streamingClient
        assertThat(client.readTimeoutMillis).isEqualTo(TimeUnit.SECONDS.toMillis(300).toInt())
        assertThat(client.connectTimeoutMillis).isEqualTo(TimeUnit.SECONDS.toMillis(90).toInt())
    }

    @Test
    fun `downloadClient has extended timeouts`() {
        val client = NetworkModule.downloadClient
        assertThat(client.connectTimeoutMillis).isEqualTo(TimeUnit.SECONDS.toMillis(60).toInt())
        assertThat(client.readTimeoutMillis).isEqualTo(TimeUnit.SECONDS.toMillis(120).toInt())
    }

    @Test
    fun `all clients share same connection pool`() {
        val default = NetworkModule.defaultClient
        val streaming = NetworkModule.streamingClient
        val download = NetworkModule.downloadClient
        assertThat(default.connectionPool).isSameInstanceAs(streaming.connectionPool)
        assertThat(default.connectionPool).isSameInstanceAs(download.connectionPool)
    }

    @Test
    fun `customClient shares connection pool with default`() {
        val custom = NetworkModule.customClient {
            readTimeout(999, TimeUnit.SECONDS)
        }
        assertThat(custom.connectionPool).isSameInstanceAs(NetworkModule.defaultClient.connectionPool)
        assertThat(custom.readTimeoutMillis).isEqualTo(TimeUnit.SECONDS.toMillis(999).toInt())
    }

    @Test
    fun `defaultClient follows redirects`() {
        val client = NetworkModule.defaultClient
        assertThat(client.followRedirects).isTrue()
    }

    @Test
    fun `defaultClient retries on connection failure`() {
        val client = NetworkModule.defaultClient
        assertThat(client.retryOnConnectionFailure).isTrue()
    }

    @Test
    fun `defaultClient has user-agent interceptor`() {
        val client = NetworkModule.defaultClient
        assertThat(client.interceptors).isNotEmpty()
    }
}
