package com.webtoapp.util

fun <T> threadLocalCompat(initializer: () -> T): ThreadLocal<T> {
    return object : ThreadLocal<T>() {
        override fun initialValue(): T = initializer()
    }
}
