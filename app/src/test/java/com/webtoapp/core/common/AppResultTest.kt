package com.webtoapp.core.common

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Unit tests for the shared `AppResult` error model.
 */
class AppResultTest {

    // Success cases

    @Test
    fun `Success wraps data correctly`() {
        val result = AppResult.Success("hello")
        assertThat(result.data).isEqualTo("hello")
        assertThat(result.isSuccess).isTrue()
        assertThat(result.isError).isFalse()
    }

    @Test
    fun `getOrNull returns data on success`() {
        val result: AppResult<Int> = AppResult.Success(42)
        assertThat(result.getOrNull()).isEqualTo(42)
    }

    @Test
    fun `getOrDefault returns data on success`() {
        val result: AppResult<Int> = AppResult.Success(42)
        assertThat(result.getOrDefault(0)).isEqualTo(42)
    }

    @Test
    fun `getOrThrow returns data on success`() {
        val result: AppResult<String> = AppResult.Success("ok")
        assertThat(result.getOrThrow()).isEqualTo("ok")
    }

    // Error cases

    @Test
    fun `Error wraps message and cause`() {
        val cause = RuntimeException("boom")
        val result = AppResult.Error("Failed", cause, ErrorCode.UNKNOWN)
        assertThat(result.userMessage).isEqualTo("Failed")
        assertThat(result.cause).isSameInstanceAs(cause)
        assertThat(result.isError).isTrue()
        assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun `getOrNull returns null on error`() {
        val result: AppResult<Int> = AppResult.Error("fail")
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `getOrDefault returns default on error`() {
        val result: AppResult<Int> = AppResult.Error("fail")
        assertThat(result.getOrDefault(99)).isEqualTo(99)
    }

    @Test(expected = RuntimeException::class)
    fun `getOrThrow throws on error`() {
        val result: AppResult<Int> = AppResult.Error("fail")
        result.getOrThrow()
    }

    // map / onSuccess / onError

    @Test
    fun `map transforms success data`() {
        val result: AppResult<Int> = AppResult.Success(5)
        val mapped = result.map { it * 2 }
        assertThat(mapped.getOrNull()).isEqualTo(10)
    }

    @Test
    fun `map propagates error unchanged`() {
        val error = AppResult.Error("fail", errorCode = ErrorCode.NETWORK_ERROR)
        val mapped: AppResult<String> = error.map { "should not reach" }
        assertThat(mapped.isError).isTrue()
        assertThat((mapped as AppResult.Error).errorCode).isEqualTo(ErrorCode.NETWORK_ERROR)
    }

    @Test
    fun `onSuccess invoked on success`() {
        var captured = ""
        AppResult.Success("data").onSuccess { captured = it }
        assertThat(captured).isEqualTo("data")
    }

    @Test
    fun `onSuccess not invoked on error`() {
        var invoked = false
        AppResult.Error("fail").onSuccess { invoked = true }
        assertThat(invoked).isFalse()
    }

    @Test
    fun `onError invoked on error`() {
        var captured = ""
        AppResult.Error("oops").onError { captured = it.userMessage }
        assertThat(captured).isEqualTo("oops")
    }

    @Test
    fun `onError not invoked on success`() {
        var invoked = false
        AppResult.Success(1).onError { invoked = true }
        assertThat(invoked).isFalse()
    }

    // runCatching

    @Test
    fun `runCatching returns Success on normal execution`() {
        val result = AppResult.runCatching { 42 }
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(42)
    }

    @Test
    fun `runCatching returns Error on exception`() {
        val result = AppResult.runCatching<Int>("custom msg") {
            throw IOException("disk full")
        }
        assertThat(result.isError).isTrue()
        val error = result as AppResult.Error
        assertThat(error.userMessage).isEqualTo("custom msg")
        assertThat(error.errorCode).isEqualTo(ErrorCode.IO_ERROR)
    }

    // suspendRunCatching

    @Test
    fun `suspendRunCatching returns Success`() = runTest {
        val result = AppResult.suspendRunCatching { "async data" }
        assertThat(result.getOrNull()).isEqualTo("async data")
    }

    @Test
    fun `suspendRunCatching returns Error on exception`() = runTest {
        val result = AppResult.suspendRunCatching<String>("net fail") {
            throw UnknownHostException("no dns")
        }
        assertThat(result.isError).isTrue()
        assertThat((result as AppResult.Error).errorCode).isEqualTo(ErrorCode.NETWORK_ERROR)
    }

    // ErrorCode mapping

    @Test
    fun `ErrorCode maps SocketTimeoutException to NETWORK_TIMEOUT`() {
        assertThat(ErrorCode.fromException(SocketTimeoutException())).isEqualTo(ErrorCode.NETWORK_TIMEOUT)
    }

    @Test
    fun `ErrorCode maps UnknownHostException to NETWORK_ERROR`() {
        assertThat(ErrorCode.fromException(UnknownHostException())).isEqualTo(ErrorCode.NETWORK_ERROR)
    }

    @Test
    fun `ErrorCode maps IOException to IO_ERROR`() {
        assertThat(ErrorCode.fromException(IOException())).isEqualTo(ErrorCode.IO_ERROR)
    }

    @Test
    fun `ErrorCode maps FileNotFoundException to NOT_FOUND`() {
        assertThat(ErrorCode.fromException(java.io.FileNotFoundException())).isEqualTo(ErrorCode.NOT_FOUND)
    }

    @Test
    fun `ErrorCode maps SecurityException to PERMISSION_DENIED`() {
        assertThat(ErrorCode.fromException(SecurityException())).isEqualTo(ErrorCode.PERMISSION_DENIED)
    }

    @Test
    fun `ErrorCode maps IllegalArgumentException to INVALID_INPUT`() {
        assertThat(ErrorCode.fromException(IllegalArgumentException())).isEqualTo(ErrorCode.INVALID_INPUT)
    }

    @Test
    fun `ErrorCode maps unknown exception to UNKNOWN`() {
        assertThat(ErrorCode.fromException(NullPointerException())).isEqualTo(ErrorCode.UNKNOWN)
    }

    // Chaining

    @Test
    fun `chaining onSuccess and onError works`() {
        var successVal = 0
        var errorMsg = ""

        AppResult.Success(10)
            .onSuccess { successVal = it }
            .onError { errorMsg = it.userMessage }

        assertThat(successVal).isEqualTo(10)
        assertThat(errorMsg).isEmpty()
    }
}
