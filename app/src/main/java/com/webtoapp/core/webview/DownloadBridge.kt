package com.webtoapp.core.webview

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.webtoapp.core.i18n.Strings
import com.webtoapp.util.DownloadNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream










class DownloadBridge(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val notificationManager = DownloadNotificationManager.getInstance(context)


    private data class ChunkedDownload(
        val filename: String,
        val mimeType: String,
        val totalSize: Long,
        val outputStream: java.io.OutputStream,
        val file: File,
        val notificationId: Int,
        var receivedChunks: Int = 0,
        var totalChunks: Int = 0
    )
    private val chunkedDownloads = java.util.concurrent.ConcurrentHashMap<String, ChunkedDownload>()
    companion object {
        const val JS_INTERFACE_NAME = "AndroidDownload"


        private val UNSAFE_FILENAME_CHARS_REGEX = Regex("[/\\\\:*?\"<>|]")





        fun getInjectionScript(): String {

            val msgPreparingDownload = Strings.preparingDownload
            val msgCannotGetFileData = Strings.cannotGetFileData
            val msgDownloadUnavailable = Strings.downloadUnavailable
            val msgProcessFileFailed = Strings.processFileFailed
            val msgReadFileFailed = Strings.readFileFailed
            val msgDownloadFailed = Strings.downloadFailedPrefix

            return """
            (function() {
                // 避免重复注入
                if (window._downloadBridgeInjected) return;
                window._downloadBridgeInjected = true;

                console.log('[DownloadBridge] Starting injection...');

                // Save原始方法
                const originalCreateElement = document.createElement.bind(document);
                const originalCreateObjectURL = URL.createObjectURL.bind(URL);
                const originalRevokeObjectURL = URL.revokeObjectURL.bind(URL);

                // Blob URL 映射表 - 暴露到 window 以便原生回调注入的 JS 也能查到
                // 页面内的 JS 常在 a.click() 之后立刻调用 URL.revokeObjectURL，
                // 等原生 onDownloadStart 回调再注入 fetch(blobUrl) 时 URL 早已失效，
                // 所以必须依赖这份缓存直接拿 Blob 对象（Blob 有引用就一直活着，不受 URL 撤销影响）
                const blobUrlMap = window.__wtaBlobMap || (window.__wtaBlobMap = new Map());

                // 拦截 URL.createObjectURL
                URL.createObjectURL = function(blob) {
                    const url = originalCreateObjectURL(blob);
                    if (blob instanceof Blob) {
                        blobUrlMap.set(url, blob);
                        console.log('[DownloadBridge] Blob URL created:', url, 'type:', blob.type, 'size:', blob.size);
                    }
                    return url;
                };

                // 拦截 URL.revokeObjectURL - 延迟 30 秒再从缓存移除
                // 这样即使页面同步 revoke 了，原生回调链路兜一圈后仍能取到 Blob
                URL.revokeObjectURL = function(url) {
                    setTimeout(() => {
                        blobUrlMap.delete(url);
                    }, 30000);
                    return originalRevokeObjectURL(url);
                };

                // 拦截 document.createElement，监控 <a> 标签的创建和点击
                document.createElement = function(tagName) {
                    const element = originalCreateElement(tagName);

                    if (tagName.toLowerCase() === 'a') {
                        // 拦截 click 方法
                        const originalClick = element.click.bind(element);
                        element.click = function() {
                            const href = element.href || '';
                            const download = element.getAttribute('download');

                            console.log('[DownloadBridge] <a>.click() intercepted:', href.substring(0, 100), 'download:', download);

                            // Handle blob: URL
                            if (href.startsWith('blob:') && download) {
                                console.log('[DownloadBridge] Handling blob download programmatically');
                                handleBlobDownload(href, download);
                                return; // 阻止默认行为
                            }

                            // Handle data: URL
                            if (href.startsWith('data:') && download) {
                                console.log('[DownloadBridge] Handling data URL download programmatically');
                                handleDataUrlDownload(href, download);
                                return; // 阻止默认行为
                            }

                            // 其他情况执行原始 click
                            return originalClick();
                        };
                    }

                    return element;
                };

                // 拦截 a 标签的点击事件（处理已存在的 a 标签）
                document.addEventListener('click', function(e) {
                    let target = e.target;
                    // 向上查找 a 标签
                    while (target && target.tagName !== 'A') {
                        target = target.parentElement;
                    }

                    if (target && target.tagName === 'A') {
                        const href = target.href || '';
                        const download = target.getAttribute('download');

                        // Handle blob: URL
                        if (href.startsWith('blob:') && download) {
                            e.preventDefault();
                            e.stopPropagation();
                            console.log('[DownloadBridge] Handling blob download from click event');
                            handleBlobDownload(href, download);
                            return false;
                        }

                        // Handle data: URL
                        if (href.startsWith('data:') && download) {
                            e.preventDefault();
                            e.stopPropagation();
                            console.log('[DownloadBridge] Handling data URL download from click event');
                            handleDataUrlDownload(href, download);
                            return false;
                        }
                    }
                }, true);

                // 大文件阈值 (10MB) - 超过此大小使用分块处理
                const LARGE_FILE_THRESHOLD = 10 * 1024 * 1024;
                // 分块大小 (512KB) - 比默认 1MB 更小，减少单块峰值内存
                // 对 250MB+ 的导出来说，1MB 分块叠加 JS 字符串 + base64 临时内存会压垮 V8 heap
                const CHUNK_SIZE = 512 * 1024;

                // Handle Blob URL 下载 - 优化版本，支持大文件分块处理
                async function handleBlobDownload(blobUrl, filename) {
                    try {
                        console.log('[DownloadBridge] handleBlobDownload:', blobUrl, filename);

                        // Show下载中提示
                        if (window.AndroidDownload && window.AndroidDownload.showToast) {
                            window.AndroidDownload.showToast('$msgPreparingDownload' + filename);
                        }

                        // 优先从缓存获取 Blob
                        let blob = blobUrlMap.get(blobUrl);

                        if (!blob) {
                            // 尝试 fetch
                            console.log('[DownloadBridge] Blob not in cache, trying fetch...');
                            try {
                                const response = await fetch(blobUrl);
                                blob = await response.blob();
                            } catch (fetchError) {
                                console.error('[DownloadBridge] Fetch failed:', fetchError);
                                alert('$msgCannotGetFileData');
                                return;
                            }
                        }

                        console.log('[DownloadBridge] Blob obtained, type:', blob.type, 'size:', blob.size);
                        const mimeType = blob.type || getMimeTypeFromFilename(filename) || 'application/octet-stream';

                        // 小文件直接处理，大文件分块处理
                        if (blob.size <= LARGE_FILE_THRESHOLD) {
                            // 小文件: 直接转Base64
                            await processSmallBlob(blob, filename, mimeType);
                        } else {
                            // 大文件: 使用分块处理避免 DOM 冻结
                            await processLargeBlobInChunks(blob, filename, mimeType);
                        }
                    } catch (error) {
                        console.error('[DownloadBridge] Blob download error:', error);
                        alert('$msgDownloadFailed' + error.message);
                    }
                }

                // 处理小文件
                async function processSmallBlob(blob, filename, mimeType) {
                    return new Promise((resolve, reject) => {
                        const reader = new FileReader();

                        reader.onloadend = function() {
                            try {
                                const base64Data = reader.result.split(',')[1];
                                console.log('[DownloadBridge] Sending to native, base64 length:', base64Data ? base64Data.length : 0);

                                if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                                    window.AndroidDownload.saveBase64File(base64Data, filename, mimeType);
                                    resolve();
                                } else {
                                    console.error('[DownloadBridge] AndroidDownload bridge not available');
                                    alert('$msgDownloadUnavailable');
                                    reject(new Error('Bridge not available'));
                                }
                            } catch (e) {
                                console.error('[DownloadBridge] Error processing blob:', e);
                                alert('$msgProcessFileFailed' + e.message);
                                reject(e);
                            }
                        };

                        reader.onerror = function() {
                            console.error('[DownloadBridge] FileReader error');
                            alert('$msgReadFileFailed');
                            reject(new Error('FileReader error'));
                        };

                        reader.readAsDataURL(blob);
                    });
                }

                // 分块处理大文件 - 使用 requestIdleCallback 避免阻塞主线程
                async function processLargeBlobInChunks(blob, filename, mimeType) {
                    console.log('[DownloadBridge] Processing large file in chunks:', blob.size, 'bytes');

                    // 通知原生端开始分块下载
                    if (window.AndroidDownload && window.AndroidDownload.startChunkedDownload) {
                        const downloadId = window.AndroidDownload.startChunkedDownload(filename, mimeType, blob.size);

                        let offset = 0;
                        const totalChunks = Math.ceil(blob.size / CHUNK_SIZE);
                        let currentChunk = 0;

                        // Efficient Base64 encoder using sub-batch processing to avoid DOM freeze
                        function uint8ToBase64(uint8Array) {
                            const SUB_BATCH = 8192;
                            const parts = [];
                            for (let i = 0; i < uint8Array.length; i += SUB_BATCH) {
                                parts.push(String.fromCharCode.apply(null, uint8Array.subarray(i, i + SUB_BATCH)));
                            }
                            return btoa(parts.join(''));
                        }

                        const processNextChunk = () => {
                            if (offset >= blob.size) {
                                // 所有分块处理完成
                                window.AndroidDownload.finishChunkedDownload(downloadId);
                                console.log('[DownloadBridge] Large file download complete');
                                return;
                            }

                            const chunk = blob.slice(offset, offset + CHUNK_SIZE);
                            chunk.arrayBuffer().then(function(arrayBuffer) {
                                const bytes = new Uint8Array(arrayBuffer);
                                const base64Chunk = uint8ToBase64(bytes);

                                // 发送分块到原生端
                                window.AndroidDownload.appendChunk(downloadId, base64Chunk, currentChunk, totalChunks);

                                offset += CHUNK_SIZE;
                                currentChunk++;

                                // 让出主线程，防止 DOM 冻结
                                setTimeout(processNextChunk, 0);
                            });
                        };

                        processNextChunk();
                    } else {
                        // 回退到普通处理 (可能导致 DOM 冻结)
                        console.warn('[DownloadBridge] Chunked download not supported, falling back to regular processing');
                        await processSmallBlob(blob, filename, mimeType);
                    }
                }

                // Handle Data URL 下载
                function handleDataUrlDownload(dataUrl, filename) {
                    try {
                        console.log('[DownloadBridge] handleDataUrlDownload:', filename);

                        const parts = dataUrl.split(',');
                        const meta = parts[0];
                        const base64Data = parts[1];

                        // 提取 MIME 类型
                        const mimeMatch = meta.match(/data:([^;]+)/);
                        const mimeType = mimeMatch ? mimeMatch[1] : getMimeTypeFromFilename(filename) || 'application/octet-stream';

                        if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                            window.AndroidDownload.saveBase64File(base64Data, filename, mimeType);
                        } else {
                            console.error('[DownloadBridge] AndroidDownload bridge not available');
                            alert('$msgDownloadUnavailable');
                        }
                    } catch (error) {
                        console.error('[DownloadBridge] Data URL download error:', error);
                        alert('$msgDownloadFailed' + error.message);
                    }
                }

                // 根据文件名猜测 MIME 类型
                function getMimeTypeFromFilename(filename) {
                    const ext = filename.split('.').pop().toLowerCase();
                    const mimeTypes = {
                        'json': 'application/json',
                        'txt': 'text/plain',
                        'html': 'text/html',
                        'htm': 'text/html',
                        'css': 'text/css',
                        'js': 'application/javascript',
                        'xml': 'application/xml',
                        'csv': 'text/csv',
                        'pdf': 'application/pdf',
                        'zip': 'application/zip',
                        'png': 'image/png',
                        'jpg': 'image/jpeg',
                        'jpeg': 'image/jpeg',
                        'gif': 'image/gif',
                        'webp': 'image/webp',
                        'svg': 'image/svg+xml',
                        'mp3': 'audio/mpeg',
                        'mp4': 'video/mp4',
                        'webm': 'video/webm'
                    };
                    return mimeTypes[ext] || null;
                }

                // 提供全局下载方法供 HTML 应用调用
                window.nativeDownload = function(data, filename, mimeType) {
                    mimeType = mimeType || getMimeTypeFromFilename(filename) || 'application/octet-stream';

                    console.log('[DownloadBridge] nativeDownload called:', filename, mimeType);

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

                // Check桥接是否可用
                window.isNativeDownloadAvailable = function() {
                    return !!(window.AndroidDownload && window.AndroidDownload.saveBase64File);
                };

                console.log('[DownloadBridge] Injection complete, bridge available:', window.isNativeDownloadAvailable());
            })();
        """.trimIndent()
        }
    }




    @JavascriptInterface
    fun showToast(message: String) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }










    @JavascriptInterface
    fun startChunkedDownload(filename: String, mimeType: String, totalSize: Long): String {
        val downloadId = java.util.UUID.randomUUID().toString()
        val safeFilename = sanitizeFilename(filename)

        try {

            val tempDir = context.cacheDir
            val tempFile = File(tempDir, "download_${downloadId}_$safeFilename")
            val outputStream = java.io.FileOutputStream(tempFile)


            val notificationId = notificationManager.showIndeterminateProgress(safeFilename)

            chunkedDownloads[downloadId] = ChunkedDownload(
                filename = safeFilename,
                mimeType = mimeType,
                totalSize = totalSize,
                outputStream = outputStream,
                file = tempFile,
                notificationId = notificationId
            )

            AppLogger.d("DownloadBridge", "Starting chunked download: id=$downloadId, file=$safeFilename, size=$totalSize")

            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, Strings.startDownload.replace("%s", safeFilename), Toast.LENGTH_SHORT).show()
            }

            return downloadId
        } catch (e: Exception) {
            AppLogger.e("DownloadBridge", "Failed to create chunked download", e)
            return ""
        }
    }








    @JavascriptInterface
    fun appendChunk(downloadId: String, base64Chunk: String, chunkIndex: Int, totalChunks: Int) {
        // 同步执行 IO 写入：这里运行在 WebView 的 JS binder 线程，
        // 这样 JS 端 setTimeout(next, 0) 只有等 appendChunk 返回后才发下一块，
        // 形成天然背压，避免协程队列堆积大量 1MB+ base64 字符串导致 OOM。
        try {
            val download = chunkedDownloads[downloadId] ?: run {
                AppLogger.e("DownloadBridge", "Download task not found: $downloadId")
                return
            }

            download.totalChunks = totalChunks

            val chunkBytes = Base64.decode(base64Chunk, Base64.DEFAULT)
            download.outputStream.write(chunkBytes)
            download.outputStream.flush()
            download.receivedChunks++

            val progress = ((download.receivedChunks.toFloat() / totalChunks) * 100).toInt()

            // 进度通知放到协程里异步更新，不阻塞 JS 线程
            if (progress % 5 == 0 || progress == 100) {
                scope.launch(Dispatchers.Main) {
                    notificationManager.updateProgress(download.notificationId, download.filename, progress)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("DownloadBridge", "Failed to write chunk $chunkIndex/$totalChunks", e)
            // 清理放到协程里，避免 JS 线程等锁
            scope.launch(Dispatchers.IO) {
                cleanupChunkedDownload(downloadId, false)
            }
        }
    }





    @JavascriptInterface
    fun finishChunkedDownload(downloadId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val download = chunkedDownloads[downloadId] ?: run {
                    AppLogger.e("DownloadBridge", "Download task not found: $downloadId")
                    return@launch
                }


                download.outputStream.flush()
                download.outputStream.close()

                AppLogger.d("DownloadBridge", "Chunked download complete, saving file: ${download.filename}")


                // 流式保存：绝不把整个文件读进内存。
                // 250MB+ 的 JSON/数据导出，readBytes() + saveFromBytes() 会直接 OOM。
                if (com.webtoapp.util.MediaSaver.isMediaFile(download.mimeType, download.filename)) {

                    val result = com.webtoapp.util.MediaSaver.saveFromFile(
                        context, download.file, download.mimeType
                    )

                    withContext(Dispatchers.Main) {
                        when (result) {
                            is com.webtoapp.util.MediaSaver.SaveResult.Success -> {
                                val isImage = download.mimeType.startsWith("image/")
                                val typeText = if (isImage) Strings.image else Strings.video
                                Toast.makeText(context, Strings.savedToGallery.replace("%s", typeText), Toast.LENGTH_SHORT).show()
                                notificationManager.showMediaSaveComplete(
                                    fileName = download.filename,
                                    uri = result.uri,
                                    mimeType = download.mimeType,
                                    isImage = isImage,
                                    progressNotificationId = download.notificationId
                                )
                            }
                            is com.webtoapp.util.MediaSaver.SaveResult.Error -> {
                                Toast.makeText(context, Strings.saveFailedWithReason.replace("%s", result.message), Toast.LENGTH_SHORT).show()
                                notificationManager.showSaveFailed(download.filename, result.message, download.notificationId)
                            }
                        }
                    }
                } else {

                    val savedFile = saveToDownloadsInternalFromFile(download.file, download.filename)

                    withContext(Dispatchers.Main) {
                        if (savedFile != null) {
                            Toast.makeText(context, Strings.savedTo.replace("%s", savedFile.name), Toast.LENGTH_LONG).show()
                            notificationManager.showSaveComplete(
                                fileName = savedFile.name,
                                filePath = savedFile.absolutePath,
                                mimeType = download.mimeType,
                                progressNotificationId = download.notificationId
                            )
                        } else {
                            Toast.makeText(context, Strings.saveFailed, Toast.LENGTH_SHORT).show()
                            notificationManager.showSaveFailed(download.filename, Strings.cannotWriteFile, download.notificationId)
                        }
                    }
                }


                cleanupChunkedDownload(downloadId, true)

            } catch (e: Exception) {
                AppLogger.e("DownloadBridge", "Failed to finish chunked download", e)
                cleanupChunkedDownload(downloadId, false)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, Strings.saveFailedWithReason.replace("%s", e.message ?: ""), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }




    private fun cleanupChunkedDownload(downloadId: String, success: Boolean) {
        chunkedDownloads.remove(downloadId)?.let { download ->
            try {
                download.outputStream.close()
            } catch (e: Exception) {  }


            if (download.file.exists()) {
                download.file.delete()
            }

            AppLogger.d("DownloadBridge", "Chunked download cleanup complete: $downloadId, success=$success")
        }
    }








    @JavascriptInterface
    fun saveBase64File(base64Data: String, filename: String, mimeType: String) {
        scope.launch(Dispatchers.IO) {

            val progressNotificationId = notificationManager.showIndeterminateProgress(filename)

            try {

                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)


                val safeFilename = sanitizeFilename(filename)


                if (com.webtoapp.util.MediaSaver.isMediaFile(mimeType, safeFilename)) {

                    val result = com.webtoapp.util.MediaSaver.saveFromBytes(
                        context, decodedBytes, safeFilename, mimeType
                    )

                    withContext(Dispatchers.Main) {
                        when (result) {
                            is com.webtoapp.util.MediaSaver.SaveResult.Success -> {
                                val isImage = mimeType.startsWith("image/")
                                val typeText = if (isImage) Strings.image else Strings.video
                                Toast.makeText(context, Strings.savedToGallery.replace("%s", typeText), Toast.LENGTH_SHORT).show()


                                notificationManager.showMediaSaveComplete(
                                    fileName = safeFilename,
                                    uri = result.uri,
                                    mimeType = mimeType,
                                    isImage = isImage,
                                    progressNotificationId = progressNotificationId
                                )
                            }
                            is com.webtoapp.util.MediaSaver.SaveResult.Error -> {
                                Toast.makeText(context, Strings.saveFailedWithReason.replace("%s", result.message), Toast.LENGTH_SHORT).show()
                                notificationManager.showSaveFailed(safeFilename, result.message, progressNotificationId)
                            }
                        }
                    }
                } else {

                    val savedFile = saveToDownloadsInternal(decodedBytes, safeFilename)

                    withContext(Dispatchers.Main) {
                        if (savedFile != null) {
                            Toast.makeText(context, Strings.savedTo.replace("%s", savedFile.name), Toast.LENGTH_LONG).show()


                            notificationManager.showSaveComplete(
                                fileName = savedFile.name,
                                filePath = savedFile.absolutePath,
                                mimeType = mimeType,
                                progressNotificationId = progressNotificationId
                            )
                        } else {
                            Toast.makeText(context, Strings.saveFailed, Toast.LENGTH_SHORT).show()
                            notificationManager.showSaveFailed(safeFilename, Strings.cannotWriteFile, progressNotificationId)
                        }
                    }
                }

            } catch (e: Exception) {
                AppLogger.e("DownloadBridge", "Failed to save file", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, Strings.saveFailedWithReason.replace("%s", e.message ?: ""), Toast.LENGTH_SHORT).show()
                    notificationManager.showSaveFailed(filename, e.message ?: Strings.unknownError, progressNotificationId)
                }
            }
        }
    }






    private fun saveToDownloadsInternal(bytes: ByteArray, filename: String): File? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            saveToDownloadsMediaStore(bytes, filename)
        } else {

            saveToDownloadsLegacy(bytes, filename)
        }
    }

    /**
     * 流式版保存：直接从临时文件拷贝到目标位置，不经过 ByteArray。
     * 用于大文件（几十 MB ~ 几百 MB），避免 OutOfMemoryError。
     */
    private fun saveToDownloadsInternalFromFile(sourceFile: File, filename: String): File? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToDownloadsMediaStoreFromFile(sourceFile, filename)
        } else {
            saveToDownloadsLegacyFromFile(sourceFile, filename)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToDownloadsMediaStoreFromFile(sourceFile: File, filename: String): File? {
        try {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, getMimeType(filename))
                put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }

            val uri = context.contentResolver.insert(
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return null

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                java.io.FileInputStream(sourceFile).use { input ->
                    input.copyTo(outputStream, bufferSize = 64 * 1024)
                }
            } ?: return null

            contentValues.clear()
            contentValues.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)

            val downloadDir = getPublicDownloadsDir()
            AppLogger.d("DownloadBridge", "流式保存到公共下载目录: ${downloadDir.absolutePath}/$filename")
            return File(downloadDir, filename)
        } catch (e: Exception) {
            AppLogger.e("DownloadBridge", "MediaStore 流式保存失败，尝试备用方案", e)
            return saveToAppPrivateDirFromFile(sourceFile, filename)
        }
    }

    private fun saveToDownloadsLegacyFromFile(sourceFile: File, filename: String): File? {
        val downloadDir = getPublicDownloadsDir()
        if (!downloadDir.exists()) downloadDir.mkdirs()

        var targetFile = File(downloadDir, filename)
        var counter = 1
        val nameWithoutExt = filename.substringBeforeLast(".")
        val ext = if (filename.contains(".")) ".${filename.substringAfterLast(".")}" else ""
        while (targetFile.exists()) {
            targetFile = File(downloadDir, "${nameWithoutExt}_$counter$ext")
            counter++
        }

        return try {
            java.io.FileInputStream(sourceFile).use { input ->
                FileOutputStream(targetFile).use { out ->
                    input.copyTo(out, bufferSize = 64 * 1024)
                }
            }
            AppLogger.d("DownloadBridge", "流式保存完成: ${targetFile.absolutePath}")
            targetFile
        } catch (e: Exception) {
            AppLogger.e("DownloadBridge", "流式写入失败", e)
            null
        }
    }

    private fun saveToAppPrivateDirFromFile(sourceFile: File, filename: String): File? {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        if (!downloadDir.exists()) downloadDir.mkdirs()

        var targetFile = File(downloadDir, filename)
        var counter = 1
        val nameWithoutExt = filename.substringBeforeLast(".")
        val ext = if (filename.contains(".")) ".${filename.substringAfterLast(".")}" else ""
        while (targetFile.exists()) {
            targetFile = File(downloadDir, "${nameWithoutExt}_$counter$ext")
            counter++
        }

        return try {
            java.io.FileInputStream(sourceFile).use { input ->
                FileOutputStream(targetFile).use { out ->
                    input.copyTo(out, bufferSize = 64 * 1024)
                }
            }
            AppLogger.d("DownloadBridge", "流式保存到应用目录: ${targetFile.absolutePath}")
            targetFile
        } catch (e: Exception) {
            AppLogger.e("DownloadBridge", "应用目录流式写入失败", e)
            null
        }
    }




    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToDownloadsMediaStore(bytes: ByteArray, filename: String): File? {
        try {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, getMimeType(filename))
                put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }

            val uri = context.contentResolver.insert(
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return null

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(bytes)
            }


            contentValues.clear()
            contentValues.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)

            val downloadDir = getPublicDownloadsDir()
            AppLogger.d("DownloadBridge", "文件已保存到公共下载目录: ${downloadDir.absolutePath}/$filename")
            return File(downloadDir, filename)

        } catch (e: Exception) {
            AppLogger.e("DownloadBridge", "MediaStore 保存失败，尝试备用方案", e)

            return saveToAppPrivateDir(bytes, filename)
        }
    }




    private fun saveToDownloadsLegacy(bytes: ByteArray, filename: String): File? {
        val downloadDir = getPublicDownloadsDir()


        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }


        var targetFile = File(downloadDir, filename)
        var counter = 1
        val nameWithoutExt = filename.substringBeforeLast(".")
        val ext = if (filename.contains(".")) ".${filename.substringAfterLast(".")}" else ""

        while (targetFile.exists()) {
            targetFile = File(downloadDir, "${nameWithoutExt}_$counter$ext")
            counter++
        }


        return try {
            FileOutputStream(targetFile).use { fos ->
                fos.write(bytes)
            }
            AppLogger.d("DownloadBridge", "文件已保存: ${targetFile.absolutePath}")
            targetFile
        } catch (e: Exception) {
            AppLogger.e("DownloadBridge", "Failed to write file", e)
            null
        }
    }




    private fun saveToAppPrivateDir(bytes: ByteArray, filename: String): File? {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir


        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }


        var targetFile = File(downloadDir, filename)
        var counter = 1
        val nameWithoutExt = filename.substringBeforeLast(".")
        val ext = if (filename.contains(".")) ".${filename.substringAfterLast(".")}" else ""

        while (targetFile.exists()) {
            targetFile = File(downloadDir, "${nameWithoutExt}_$counter$ext")
            counter++
        }


        return try {
            FileOutputStream(targetFile).use { fos ->
                fos.write(bytes)
            }
            AppLogger.d("DownloadBridge", "文件已保存到应用目录: ${targetFile.absolutePath}")
            targetFile
        } catch (e: Exception) {
            AppLogger.e("DownloadBridge", "Failed to write file", e)
            null
        }
    }




    private fun getMimeType(filename: String): String {
        val ext = filename.substringAfterLast(".", "").lowercase()
        return when (ext) {
            "json" -> "application/json"
            "txt" -> "text/plain"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "xml" -> "application/xml"
            "csv" -> "text/csv"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            else -> "application/octet-stream"
        }
    }






    @JavascriptInterface
    fun saveTextFile(content: String, filename: String) {
        val base64Data = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        saveBase64File(base64Data, filename, "text/plain")
    }






    @JavascriptInterface
    fun saveJsonFile(jsonContent: String, filename: String) {
        val base64Data = Base64.encodeToString(jsonContent.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        val safeFilename = if (filename.endsWith(".json")) filename else "$filename.json"
        saveBase64File(base64Data, safeFilename, "application/json")
    }




    @JavascriptInterface
    fun isAvailable(): Boolean = true




    @JavascriptInterface
    fun getDownloadPath(): String {
        return getPublicDownloadsDir().absolutePath
    }




    @Suppress("DEPRECATION")
    private fun getPublicDownloadsDir(): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)




    private fun sanitizeFilename(filename: String): String {

        var safe = filename.replace(UNSAFE_FILENAME_CHARS_REGEX, "_")


        if (safe.length > 200) {
            val ext = safe.substringAfterLast(".", "")
            val name = safe.substringBeforeLast(".")
            safe = if (ext.isNotEmpty()) {
                "${name.take(190)}.$ext"
            } else {
                name.take(200)
            }
        }


        if (safe.isBlank()) {
            safe = "download_${System.currentTimeMillis()}"
        }

        return safe
    }
}
