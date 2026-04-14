package com.webtoapp.core.common

/**
 * Note
 * Note
 *
 * Note
 * ```
 * suspend fun fetchData(): AppResult<Data> = AppResult.runCatching {
 *     api.getData()
 * }
 *
 * when (val result = fetchData()) {
 *     is AppResult.Success -> handleData(result.data)
 *     is AppResult.Error -> showError(result.userMessage)
 * }
 * ```
 */
sealed class AppResult<out T> {

    data class Success<T>(val data: T) : AppResult<T>()

    data class Error(
        val userMessage: String,
        val cause: Throwable? = null,
        val errorCode: ErrorCode = ErrorCode.UNKNOWN
    ) : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    /**
     * Note
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    /**
     * Note
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> default
    }

    /**
     * Note
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw cause ?: RuntimeException(userMessage)
    }

    /**
     * Note
     */
    inline fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    /**
     * Note
     */
    inline fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Note
     */
    inline fun onError(action: (Error) -> Unit): AppResult<T> {
        if (this is Error) action(this)
        return this
    }

    companion object {
        /**
         * Note
         */
        inline fun <T> runCatching(
            errorMessage: String = "Operation failed",
            block: () -> T
        ): AppResult<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                Error(
                    userMessage = errorMessage,
                    cause = e,
                    errorCode = ErrorCode.fromException(e)
                )
            }
        }

        /**
         * Note
         */
        suspend inline fun <T> suspendRunCatching(
            errorMessage: String = "Operation failed",
            crossinline block: suspend () -> T
        ): AppResult<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                Error(
                    userMessage = errorMessage,
                    cause = e,
                    errorCode = ErrorCode.fromException(e)
                )
            }
        }
    }
}

/**
 * Note
 */
enum class ErrorCode {
    UNKNOWN,
    NETWORK_ERROR,
    NETWORK_TIMEOUT,
    IO_ERROR,
    PARSE_ERROR,
    NOT_FOUND,
    PERMISSION_DENIED,
    INVALID_INPUT,
    STORAGE_FULL,
    CANCELLED;

    companion object {
        fun fromException(e: Throwable): ErrorCode = when (e) {
            is java.net.SocketTimeoutException -> NETWORK_TIMEOUT
            is java.net.UnknownHostException,
            is java.net.ConnectException -> NETWORK_ERROR
            is java.io.IOException -> IO_ERROR
            is com.google.gson.JsonSyntaxException,
            is com.google.gson.JsonParseException -> PARSE_ERROR
            is java.io.FileNotFoundException -> NOT_FOUND
            is SecurityException -> PERMISSION_DENIED
            is IllegalArgumentException -> INVALID_INPUT
            is kotlinx.coroutines.CancellationException -> CANCELLED
            else -> UNKNOWN
        }
    }
}