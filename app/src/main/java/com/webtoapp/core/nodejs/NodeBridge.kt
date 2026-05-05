package com.webtoapp.core.nodejs

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellLogger












object NodeBridge {

    private const val TAG = "NodeBridge"


    private var jniLoaded = false


    interface OutputCallback {





        fun onOutput(line: String, isError: Boolean)
    }




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







    fun loadNode(context: Context): Boolean {
        if (!loadJniBridge()) return false

        if (nativeIsLoaded()) {
            AppLogger.d(TAG, "libnode.so 已加载")
            return true
        }


        try {
            System.loadLibrary("node")
            AppLogger.i(TAG, "libnode.so 通过 System.loadLibrary 加载成功")
            ShellLogger.i(TAG, "libnode.so 通过 System.loadLibrary 加载成功")


        } catch (e: UnsatisfiedLinkError) {
            AppLogger.d(TAG, "System.loadLibrary(\"node\") 失败: ${e.message}")
        }


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


        val jniCallback = callback?.let { cb ->
            object : Any() {
                @Suppress("unused")
                fun onOutput(line: String, isError: Boolean) {
                    cb.onOutput(line, isError)
                }
            }
        }

        return nativeStartNode(args, jniCallback)
    }




    fun isStarted(): Boolean {
        return jniLoaded && nativeIsStarted()
    }




    fun isLoaded(): Boolean {
        return jniLoaded && nativeIsLoaded()
    }



    private external fun nativeLoadNode(nodePath: String): Boolean
    private external fun nativeStartNode(arguments: Array<String>, callback: Any?): Int
    private external fun nativeIsStarted(): Boolean
    private external fun nativeIsLoaded(): Boolean
}
