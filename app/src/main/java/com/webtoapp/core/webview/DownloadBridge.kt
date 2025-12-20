package com.webtoapp.core.webview

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * JavaScript 下载桥接
 * 用于处理 WebView 中的 Blob 下载和文件保存
 * 
 * 解决问题：
 * 1. WebView 无法处理 blob: URL 下载
 * 2. HTML 应用中的数据导出功能
 * 3. 前端生成的文件下载（如 JSON、CSV 等）
 */
class DownloadBridge(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val JS_INTERFACE_NAME = "AndroidDownload"
        
        /**
         * 生成注入到 WebView 的 JavaScript 代码
         * 拦截 Blob 下载并转发给原生代码处理
         */
        fun getInjectionScript(): String = """
            (function() {
                // 避免重复注入
                if (window._downloadBridgeInjected) return;
                window._downloadBridgeInjected = true;
                
                // 保存原始的 createElement 方法
                const originalCreateElement = document.createElement.bind(document);
                
                // 拦截 a 标签的点击事件
                document.addEventListener('click', function(e) {
                    let target = e.target;
                    // 向上查找 a 标签
                    while (target && target.tagName !== 'A') {
                        target = target.parentElement;
                    }
                    
                    if (target && target.tagName === 'A') {
                        const href = target.href || '';
                        const download = target.getAttribute('download');
                        
                        // 处理 blob: URL
                        if (href.startsWith('blob:') && download) {
                            e.preventDefault();
                            e.stopPropagation();
                            handleBlobDownload(href, download);
                            return false;
                        }
                        
                        // 处理 data: URL
                        if (href.startsWith('data:') && download) {
                            e.preventDefault();
                            e.stopPropagation();
                            handleDataUrlDownload(href, download);
                            return false;
                        }
                    }
                }, true);
                
                // 处理 Blob URL 下载
                async function handleBlobDownload(blobUrl, filename) {
                    try {
                        // 显示下载中提示
                        if (window.AndroidDownload && window.AndroidDownload.showToast) {
                            window.AndroidDownload.showToast('正在准备下载...');
                        }
                        
                        const response = await fetch(blobUrl);
                        const blob = await response.blob();
                        const reader = new FileReader();
                        
                        reader.onloadend = function() {
                            const base64Data = reader.result.split(',')[1];
                            const mimeType = blob.type || 'application/octet-stream';
                            
                            if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                                window.AndroidDownload.saveBase64File(base64Data, filename, mimeType);
                            } else {
                                console.error('AndroidDownload bridge not available');
                                alert('下载功能不可用');
                            }
                        };
                        
                        reader.onerror = function() {
                            console.error('Failed to read blob');
                            alert('读取文件失败');
                        };
                        
                        reader.readAsDataURL(blob);
                    } catch (error) {
                        console.error('Blob download error:', error);
                        alert('下载失败: ' + error.message);
                    }
                }
                
                // 处理 Data URL 下载
                function handleDataUrlDownload(dataUrl, filename) {
                    try {
                        const parts = dataUrl.split(',');
                        const meta = parts[0];
                        const base64Data = parts[1];
                        
                        // 提取 MIME 类型
                        const mimeMatch = meta.match(/data:([^;]+)/);
                        const mimeType = mimeMatch ? mimeMatch[1] : 'application/octet-stream';
                        
                        if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                            window.AndroidDownload.saveBase64File(base64Data, filename, mimeType);
                        } else {
                            console.error('AndroidDownload bridge not available');
                            alert('下载功能不可用');
                        }
                    } catch (error) {
                        console.error('Data URL download error:', error);
                        alert('下载失败: ' + error.message);
                    }
                }
                
                // 拦截 URL.createObjectURL 创建的下载
                const originalCreateObjectURL = URL.createObjectURL.bind(URL);
                const blobUrlMap = new Map();
                
                URL.createObjectURL = function(blob) {
                    const url = originalCreateObjectURL(blob);
                    if (blob instanceof Blob) {
                        blobUrlMap.set(url, blob);
                    }
                    return url;
                };
                
                // 拦截 URL.revokeObjectURL
                const originalRevokeObjectURL = URL.revokeObjectURL.bind(URL);
                URL.revokeObjectURL = function(url) {
                    blobUrlMap.delete(url);
                    return originalRevokeObjectURL(url);
                };
                
                // 提供全局下载方法供 HTML 应用调用
                window.nativeDownload = function(data, filename, mimeType) {
                    mimeType = mimeType || 'application/octet-stream';
                    
                    if (typeof data === 'string') {
                        // 字符串数据，转为 Base64
                        const base64 = btoa(unescape(encodeURIComponent(data)));
                        if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                            window.AndroidDownload.saveBase64File(base64, filename, mimeType);
                        }
                    } else if (data instanceof Blob) {
                        // Blob 数据
                        const reader = new FileReader();
                        reader.onloadend = function() {
                            const base64Data = reader.result.split(',')[1];
                            if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                                window.AndroidDownload.saveBase64File(base64Data, filename, mimeType);
                            }
                        };
                        reader.readAsDataURL(data);
                    } else if (data instanceof ArrayBuffer) {
                        // ArrayBuffer 数据
                        const bytes = new Uint8Array(data);
                        let binary = '';
                        for (let i = 0; i < bytes.byteLength; i++) {
                            binary += String.fromCharCode(bytes[i]);
                        }
                        const base64 = btoa(binary);
                        if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                            window.AndroidDownload.saveBase64File(base64, filename, mimeType);
                        }
                    }
                };
                
                // 提供 JSON 导出快捷方法
                window.nativeDownloadJSON = function(obj, filename) {
                    const json = JSON.stringify(obj, null, 2);
                    window.nativeDownload(json, filename || 'data.json', 'application/json');
                };
                
                // 提供文本导出快捷方法
                window.nativeDownloadText = function(text, filename) {
                    window.nativeDownload(text, filename || 'text.txt', 'text/plain');
                };
                
                console.log('[DownloadBridge] Injection complete');
            })();
        """.trimIndent()
    }
    
    /**
     * 显示 Toast 提示
     */
    @JavascriptInterface
    fun showToast(message: String) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 保存 Base64 编码的文件到下载目录
     * @param base64Data Base64 编码的文件数据
     * @param filename 文件名
     * @param mimeType MIME 类型
     */
    @JavascriptInterface
    fun saveBase64File(base64Data: String, filename: String, mimeType: String) {
        scope.launch(Dispatchers.IO) {
            try {
                // 解码 Base64 数据
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                
                // 确保文件名安全
                val safeFilename = sanitizeFilename(filename)
                
                // 获取下载目录
                val downloadDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ 使用应用专属目录或 MediaStore
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: context.filesDir
                } else {
                    // Android 9 及以下使用公共下载目录
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                }
                
                // 确保目录存在
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                
                // 处理文件名冲突
                var targetFile = File(downloadDir, safeFilename)
                var counter = 1
                val nameWithoutExt = safeFilename.substringBeforeLast(".")
                val ext = if (safeFilename.contains(".")) ".${safeFilename.substringAfterLast(".")}" else ""
                
                while (targetFile.exists()) {
                    targetFile = File(downloadDir, "${nameWithoutExt}_$counter$ext")
                    counter++
                }
                
                // 写入文件
                FileOutputStream(targetFile).use { fos ->
                    fos.write(decodedBytes)
                }
                
                // 通知用户
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "已保存到: ${targetFile.name}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                
                android.util.Log.d("DownloadBridge", "文件已保存: ${targetFile.absolutePath}")
                
            } catch (e: Exception) {
                android.util.Log.e("DownloadBridge", "保存文件失败", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "保存失败: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    /**
     * 直接保存文本内容到文件
     * @param content 文本内容
     * @param filename 文件名
     */
    @JavascriptInterface
    fun saveTextFile(content: String, filename: String) {
        val base64Data = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        saveBase64File(base64Data, filename, "text/plain")
    }
    
    /**
     * 直接保存 JSON 内容到文件
     * @param jsonContent JSON 字符串
     * @param filename 文件名
     */
    @JavascriptInterface
    fun saveJsonFile(jsonContent: String, filename: String) {
        val base64Data = Base64.encodeToString(jsonContent.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        val safeFilename = if (filename.endsWith(".json")) filename else "$filename.json"
        saveBase64File(base64Data, safeFilename, "application/json")
    }
    
    /**
     * 检查下载桥接是否可用
     */
    @JavascriptInterface
    fun isAvailable(): Boolean = true
    
    /**
     * 获取下载目录路径（供 JS 显示给用户）
     */
    @JavascriptInterface
    fun getDownloadPath(): String {
        val downloadDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
        return downloadDir.absolutePath
    }
    
    /**
     * 清理文件名，移除不安全字符
     */
    private fun sanitizeFilename(filename: String): String {
        // 移除路径分隔符和其他不安全字符
        var safe = filename.replace(Regex("[/\\\\:*?\"<>|]"), "_")
        
        // 限制长度
        if (safe.length > 200) {
            val ext = safe.substringAfterLast(".", "")
            val name = safe.substringBeforeLast(".")
            safe = if (ext.isNotEmpty()) {
                "${name.take(190)}.$ext"
            } else {
                name.take(200)
            }
        }
        
        // 确保不为空
        if (safe.isBlank()) {
            safe = "download_${System.currentTimeMillis()}"
        }
        
        return safe
    }
}
