package com.webtoapp.util

import android.os.Build
import android.os.SystemClock
import java.util.concurrent.TimeUnit

fun Process.isAliveCompat(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        isAlive
    } else {
        try {
            exitValue()
            false
        } catch (_: IllegalThreadStateException) {
            true
        }
    }
}

fun Process.destroyForciblyCompat() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        destroyForcibly()
    } else {
        destroy()
    }
}

fun Process.waitForCompat(timeoutMs: Long): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        return waitFor(timeoutMs, TimeUnit.MILLISECONDS)
    }

    val deadline = SystemClock.elapsedRealtime() + timeoutMs
    while (SystemClock.elapsedRealtime() < deadline) {
        try {
            exitValue()
            return true
        } catch (_: IllegalThreadStateException) {
            try {
                Thread.sleep(50)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
    }
    return false
}
