package com.webtoapp.core.errorpage

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NetworkErrorDiagnosticsTest {

    @Test
    fun `EADDRNOTAVAIL is diagnosed`() {
        val result = NetworkErrorDiagnostics.diagnose(
            rawDescription = "bind failed: EADDRNOTAVAIL (Cannot assign requested address)",
            errorCode = -1,
            failedUrl = "https://example.com",
            language = "CHINESE"
        )

        assertThat(result).isNotNull()
        assertThat(result!!.key).isEqualTo("EADDRNOTAVAIL")
        assertThat(result.severity).isEqualTo(NetworkErrorDiagnostics.Severity.ERROR)
        assertThat(result.suggestions).isNotEmpty()
        assertThat(result.retryable).isTrue()
    }

    @Test
    fun `ECONNREFUSED to loopback is diagnosed as local server down`() {
        val result = NetworkErrorDiagnostics.diagnose(
            rawDescription = "ECONNREFUSED (Connection refused)",
            errorCode = -6,
            failedUrl = "http://127.0.0.1:8080/api",
            language = "ENGLISH"
        )

        assertThat(result).isNotNull()
        assertThat(result!!.key).isEqualTo("LOCAL_CONN_REFUSED")
        assertThat(result.title).contains("Local server")
    }

    @Test
    fun `ECONNREFUSED to remote host is diagnosed as generic refused`() {
        val result = NetworkErrorDiagnostics.diagnose(
            rawDescription = "ECONNREFUSED",
            errorCode = -6,
            failedUrl = "https://api.example.com/v1",
            language = "ENGLISH"
        )

        assertThat(result).isNotNull()
        assertThat(result!!.key).isEqualTo("ECONNREFUSED")
        assertThat(result.title).isEqualTo("Connection refused")
    }

    @Test
    fun `ENETUNREACH is diagnosed`() {
        val result = NetworkErrorDiagnostics.diagnose(
            rawDescription = "Network is unreachable",
            errorCode = -1,
            failedUrl = "https://example.com",
            language = "ARABIC"
        )

        assertThat(result).isNotNull()
        assertThat(result!!.key).isEqualTo("ENETUNREACH")
        assertThat(result.title).contains("الشبكة")
    }

    @Test
    fun `DNS failure is diagnosed via error code`() {
        val result = NetworkErrorDiagnostics.diagnose(
            rawDescription = "net::ERR_NAME_NOT_RESOLVED",
            errorCode = -2,
            failedUrl = "https://nonexistent.example.com",
            language = "CHINESE"
        )

        assertThat(result).isNotNull()
        assertThat(result!!.key).isEqualTo("DNS")
    }

    @Test
    fun `timeout is diagnosed`() {
        val result = NetworkErrorDiagnostics.diagnose(
            rawDescription = "net::ERR_TIMED_OUT",
            errorCode = -7,
            failedUrl = "https://slow.example.com",
            language = "ENGLISH"
        )

        assertThat(result).isNotNull()
        assertThat(result!!.key).isEqualTo("ETIMEDOUT")
    }

    @Test
    fun `SSL error is diagnosed`() {
        val result = NetworkErrorDiagnostics.diagnose(
            rawDescription = "net::ERR_SSL_PROTOCOL_ERROR",
            errorCode = -11,
            failedUrl = "https://expired.example.com",
            language = "CHINESE"
        )

        assertThat(result).isNotNull()
        assertThat(result!!.key).isEqualTo("SSL")
    }

    @Test
    fun `cleartext blocked is diagnosed`() {
        val result = NetworkErrorDiagnostics.diagnose(
            rawDescription = "net::ERR_CLEARTEXT_NOT_PERMITTED",
            errorCode = -1,
            failedUrl = "http://example.com",
            language = "ENGLISH"
        )

        assertThat(result).isNotNull()
        assertThat(result!!.key).isEqualTo("CLEARTEXT")
    }

    @Test
    fun `EADDRINUSE is diagnosed`() {
        val result = NetworkErrorDiagnostics.diagnose(
            rawDescription = "bind failed: EADDRINUSE (Address already in use)",
            errorCode = -1,
            failedUrl = "http://127.0.0.1:3000",
            language = "CHINESE"
        )

        assertThat(result).isNotNull()
        assertThat(result!!.key).isEqualTo("EADDRINUSE")
    }

    @Test
    fun `unrecognized error returns null`() {
        val result = NetworkErrorDiagnostics.diagnose(
            rawDescription = "Something completely unknown happened",
            errorCode = -999,
            failedUrl = "https://example.com",
            language = "CHINESE"
        )

        assertThat(result).isNull()
    }

    @Test
    fun `null rawDescription returns null`() {
        val result = NetworkErrorDiagnostics.diagnose(
            rawDescription = null,
            errorCode = 0,
            failedUrl = null,
            language = "CHINESE"
        )

        assertThat(result).isNull()
    }

    @Test
    fun `EHOSTUNREACH to private IP suggests checking same LAN`() {
        val result = NetworkErrorDiagnostics.diagnose(
            rawDescription = "No route to host",
            errorCode = -1,
            failedUrl = "http://192.168.1.100:8080",
            language = "ENGLISH"
        )

        assertThat(result).isNotNull()
        assertThat(result!!.key).isEqualTo("EHOSTUNREACH")
        assertThat(result.suggestions).contains("Make sure your device and the target are on the same LAN")
    }
}
