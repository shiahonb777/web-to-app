package com.webtoapp.core.nodejs

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellLogger

/**
 * Node.js JNI 桥接层
 *
 * 通过 JNI 调用 nodejs-mobile 的 libnode.so 共享库。
 * libnode.so 不是独立可执行文件，必须通过 dlopen + node::Start() 在进程内启动。
 *
 * 重要限制：
 * - node::Start() 只能调用一次（nodejs-mobile 限制）
 * - 调用是阻塞的，必须在后台线程中执行
 * - Node.js 退出后无法在同一进程中重启
 */
object NodeBridge {

    private const val TAG = "NodeBridge"

    /** 是否已加载 JNI 库 */
    private var jniLoaded = false

    /** Node.js 输出回调接口 */
    interface OutputCallback {
        /**
         * Node.js stdout/stderr 输出回调
         * @param line 输出行
         * @param isError 是否为 stderr
         */
        fun onOutput(line: String, isError: Boolean)
    }

    /**
     * 加载 JNI bridge 库（libnode_bridge.so）
     */
    @Synchronized
    fun loadJniBridge(): Boolean {
        if (jniLoaded) return true
        return try {
            System.loadLibrary("node_bridge")
            jniLoaded = true
            AppLogger.i(TAG, "node_bridge JNI 库加载成功")
            true
        } catch (e: UnsatisfiedLinkError) {
            AppLogger.e(TAG, "node_bridge JNI 库加载失败", e)
            ShellLogger.e(TAG, "node_bridge JNI 库加载失败: ${e.message}")
            false
        }
    }

    /**
     * 加载 libnode.so 共享库
     *
     * @param context Android Context
     * @return true 如果加载成功
     */
    fun loadNode(context: Context): Boolean {
        if (!loadJniBridge()) return false

        if (nativeIsLoaded()) {
            AppLogger.d(TAG, "libnode.so 已加载")
            return true
        }

        // 策略 1: 尝试 System.loadLibrary（适用于 nativeLibraryDir 中的 libnode.so）
        try {
            System.loadLibrary("node")
            AppLogger.i(TAG, "libnode.so 通过 System.loadLibrary 加载成功")
            ShellLogger.i(TAG, "libnode.so 通过 System.loadLibrary 加载成功")
            // System.loadLibrary 加载后，dlsym 可以在全局符号表中找到 node::Start
            // 但我们的 nativeLoadNode 使用 dlopen，所以还需要用路径加载
        } catch (e: UnsatisfiedLinkError) {
            AppLogger.d(TAG, "System.loadLibrary(\"node\") 失败: ${e.message}")
        }

        // 策略 2: 通过 dlopen 加载完整路径
        val nodePath = NodeDependencyManager.getNodeLibraryPath(context)
        if (nodePath == null) {
            AppLogger.e(TAG, "libnode.so 未找到")
            ShellLogger.e(TAG, "libnode.so 未找到")
            return false
        }

        AppLogger.i(TAG, "加载 libnode.so (dlopen): $nodePath")
        ShellLogger.i(TAG, "加载 libnode.so (dlopen): $nodePath")

        val result = nativeLoadNode(nodePath)
        if (result) {
            AppLogger.i(TAG, "libnode.so 加载成功")
            ShellLogger.i(TAG, "libnode.so 加载成功")
        } else {
            AppLogger.e(TAG, "libnode.so 加载失败")
            ShellLogger.e(TAG, "libnode.so 加载失败")
        }
        return result
    }

    /**
     * 启动 Node.js（阻塞调用，必须在后台线程中）
     *
     * @param args Node.js 参数，如 ["node", "/path/to/script.js"]
     * @param callback 输出回调
     * @return Node.js 退出码
     */
    fun startNode(args: Array<String>, callback: OutputCallback? = null): Int {
        if (!nativeIsLoaded()) {
            AppLogger.e(TAG, "libnode.so 未加载，无法启动")
            return -1
        }
        if (nativeIsStarted()) {
            AppLogger.w(TAG, "Node.js 已启动过（每个进程只能启动一次）")
            return -2
        }

        AppLogger.i(TAG, "启动 Node.js: ${args.joinToString(" ")}")
        ShellLogger.i(TAG, "启动 Node.js: ${args.joinToString(" ")}")

        // 创建一个桥接回调对象传给 JNI
        val jniCallback = callback?.let { cb ->
            object : Any() {
                @Suppress("unused") // Called from JNI
                fun onOutput(line: String, isError: Boolean) {
                    cb.onOutput(line, isError)
                }
            }
        }

        return nativeStartNode(args, jniCallback)
    }

    /**
     * Node.js 是否已启动过
     */
    fun isStarted(): Boolean {
        return jniLoaded && nativeIsStarted()
    }

    /**
     * libnode.so 是否已加载
     */
    fun isLoaded(): Boolean {
        return jniLoaded && nativeIsLoaded()
    }

    // ==================== Native 方法 ====================

    private external fun nativeLoadNode(nodePath: String): Boolean
    private external fun nativeStartNode(arguments: Array<String>, callback: Any?): Int
    private external fun nativeIsStarted(): Boolean
    private external fun nativeIsLoaded(): Boolean
}
