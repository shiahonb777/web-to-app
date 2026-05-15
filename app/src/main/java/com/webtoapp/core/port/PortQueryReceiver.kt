package com.webtoapp.core.port

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.webtoapp.core.logging.AppLogger
import org.json.JSONArray
import org.json.JSONObject

/**
 * 跨应用端口查询接收器。
 *
 * 宿主（Web2App 主应用）通过定向广播查询每个 Web2App 构建产物当前的端口分配，
 * 接收器把本进程内 [PortManager] 的分配状态序列化为 JSON 写入 ResultExtras 返回。
 *
 * Action: [ACTION_PORT_QUERY]
 * 返回 extras：
 *  - [EXTRA_PROTOCOL_VERSION] : Int   协议版本
 *  - [EXTRA_PACKAGE]          : String 当前应用包名
 *  - [EXTRA_PROCESS_PID]      : Int   当前进程 PID
 *  - [EXTRA_ALLOCATIONS]      : String JSON 数组，元素结构见下方
 *
 * Allocation JSON 结构（每个元素）：
 *   {
 *     "port": 18000,
 *     "owner": "nodejs:demo",
 *     "range": "NODEJS",
 *     "allocatedAt": 1715600000000,
 *     "pid": 12345,
 *     "alive": true
 *   }
 *
 * 该接收器仅响应有 [PERMISSION_QUERY] 自定义权限的调用方（即宿主），不导出公网。
 */
class PortQueryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PORT_QUERY) return

        val pendingResult = goAsync()
        try {
            val allocations = PortManager.getAllAllocations()
            val arr = JSONArray()
            for ((port, alloc) in allocations) {
                val obj = JSONObject().apply {
                    put("port", port)
                    put("owner", alloc.owner)
                    put("range", alloc.range.name)
                    put("allocatedAt", alloc.allocatedAt)
                    put("pid", alloc.pid)
                    put("alive", PortManager.isProcessAlive(port))
                }
                arr.put(obj)
            }

            val bundle = Bundle().apply {
                putInt(EXTRA_PROTOCOL_VERSION, PROTOCOL_VERSION)
                putString(EXTRA_PACKAGE, context.packageName)
                putInt(EXTRA_PROCESS_PID, android.os.Process.myPid())
                putString(EXTRA_ALLOCATIONS, arr.toString())
            }

            pendingResult.resultCode = RESULT_CODE_OK
            pendingResult.resultData = arr.toString()
            // setResultExtras 没有对应 getter，不能用属性赋值语法。
            pendingResult.setResultExtras(bundle)
        } catch (e: Exception) {
            AppLogger.w(TAG, "PortQueryReceiver failed: ${e.message}")
            pendingResult.resultCode = RESULT_CODE_ERROR
        } finally {
            pendingResult.finish()
        }
    }

    companion object {
        private const val TAG = "PortQueryReceiver"

        const val PROTOCOL_VERSION = 1

        const val ACTION_PORT_QUERY = "com.webtoapp.action.PORT_QUERY"

        const val EXTRA_PROTOCOL_VERSION = "wta.protocolVersion"
        const val EXTRA_PACKAGE = "wta.package"
        const val EXTRA_PROCESS_PID = "wta.pid"
        const val EXTRA_ALLOCATIONS = "wta.allocations"

        const val RESULT_CODE_OK = 1
        const val RESULT_CODE_ERROR = -1
    }
}
