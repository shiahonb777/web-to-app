package com.webtoapp.core.port

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.webtoapp.core.logging.AppLogger

/**
 * 跨应用端口释放接收器。
 *
 * 宿主向目标 Web2App 应用发送广播，由目标应用自身释放指定端口对应的进程。
 *
 * Action: [ACTION_PORT_RELEASE]
 * 接收 extras：
 *  - [EXTRA_PORT]   : Int    要释放的端口（>0 时只释放该端口）
 *  - [EXTRA_OWNER]  : String 可选，按 owner 释放
 *  - [EXTRA_RELEASE_ALL] : Boolean 可选，true 时释放本应用全部端口
 *
 * 返回 resultCode 标识是否成功，data 携带释放数量。
 */
class PortReleaseReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PORT_RELEASE) return

        val pendingResult = goAsync()
        try {
            val before = PortManager.getAllAllocations().size

            when {
                intent.getBooleanExtra(EXTRA_RELEASE_ALL, false) -> {
                    PortManager.releaseAll()
                }
                intent.hasExtra(EXTRA_OWNER) -> {
                    val owner = intent.getStringExtra(EXTRA_OWNER) ?: ""
                    if (owner.isNotBlank()) PortManager.releaseByOwner(owner)
                }
                else -> {
                    val port = intent.getIntExtra(EXTRA_PORT, -1)
                    if (port > 0) PortManager.release(port)
                }
            }

            val after = PortManager.getAllAllocations().size
            val released = (before - after).coerceAtLeast(0)

            pendingResult.resultCode = RESULT_CODE_OK
            pendingResult.resultData = released.toString()
        } catch (e: Exception) {
            AppLogger.w(TAG, "PortReleaseReceiver failed: ${e.message}")
            pendingResult.resultCode = RESULT_CODE_ERROR
        } finally {
            pendingResult.finish()
        }
    }

    companion object {
        private const val TAG = "PortReleaseReceiver"

        const val ACTION_PORT_RELEASE = "com.webtoapp.action.PORT_RELEASE"

        const val EXTRA_PORT = "wta.port"
        const val EXTRA_OWNER = "wta.owner"
        const val EXTRA_RELEASE_ALL = "wta.releaseAll"

        const val RESULT_CODE_OK = 1
        const val RESULT_CODE_ERROR = -1
    }
}
