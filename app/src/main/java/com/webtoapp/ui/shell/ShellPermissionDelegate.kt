package com.webtoapp.ui.shell

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.util.DownloadHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shell Activity 的权限处理委托
 *
 * 封装所有 ActivityResultLauncher 和权限请求逻辑。
 * 必须在 Activity.onCreate() 之前（即 Activity 初始化阶段）实例化。
 */
class ShellPermissionDelegate(private val activity: AppCompatActivity) {

    // Permission请求相关
    private var pendingPermissionRequest: PermissionRequest? = null
    private var pendingGeolocationOrigin: String? = null
    private var pendingGeolocationCallback: GeolocationPermissions.Callback? = null

    // 待下载信息（权限请求后使用）
    private var pendingDownload: PendingDownload? = null

    private data class PendingDownload(
        val url: String,
        val userAgent: String,
        val contentDisposition: String,
        val mimeType: String,
        val contentLength: Long
    )
    
    // ===== 文件选择器 (支持相机拍照 + 相册 + 文件) =====
    
    // 相机拍照临时文件 URI
    private var cameraPhotoUri: Uri? = null
    
    // WebView 文件选择回调
    private var pendingFilePathCallback: android.webkit.ValueCallback<Array<Uri>>? = null
    
    /**
     * 完整文件选择器 — 同时支持相机拍照、相册选图、文件选择
     * 
     * 核心问题：Android WebView 的 onShowFileChooser 需要一个 Intent 选择器，
     * 而不是简单的 GetMultipleContents()，因为后者不支持：
     * 1. 相机拍照（<input capture="camera">）
     * 2. 视频录制（<input capture="camcorder">）
     * 3. 正确的 MIME 类型过滤
     */
    private val fileChooserActivityLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = pendingFilePathCallback
        if (callback == null) {
            AppLogger.w("ShellPermission", "fileChooserActivityLauncher: no pending callback")
            return@registerForActivityResult
        }
        
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUris = mutableListOf<Uri>()
            
            // 情况1: 用户从相机拍照返回（使用预设的 URI）
            val data = result.data
            if (data == null || (data.data == null && data.clipData == null)) {
                // 没有 data 意味着是从相机拍照返回
                cameraPhotoUri?.let { resultUris.add(it) }
            } else {
                // 情况2: 用户从相册/文件选择器返回
                data.data?.let { uri -> resultUris.add(uri) }
                data.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        clipData.getItemAt(i).uri?.let { resultUris.add(it) }
                    }
                }
            }
            
            AppLogger.d("ShellPermission", "File chooser result: ${resultUris.size} files")
            callback.onReceiveValue(resultUris.toTypedArray())
        } else {
            // 用户取消 — 必须传 null，否则 WebView 的文件选择器会永久失效
            AppLogger.d("ShellPermission", "File chooser cancelled")
            callback.onReceiveValue(null)
        }
        
        pendingFilePathCallback = null
        cameraPhotoUri = null
    }
    
    // 相机权限请求 launcher（文件选择器场景）
    private var pendingFileChooserParams: WebChromeClient.FileChooserParams? = null
    private val cameraForFileChooserPermLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // 无论相机权限是否授予，都继续启动文件选择器
        // 如果相机权限被拒，选择器中只有相册不会有拍照选项
        launchFileChooserIntent(pendingFileChooserParams)
        pendingFileChooserParams = null
    }
    
    /**
     * 处理 WebView 的 onShowFileChooser 回调
     * 
     * 构建一个包含相机拍照 + 相册 + 文件管理器的 Intent 选择器
     */
    fun handleFileChooser(
        filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean {
        // 取消之前未完成的回调
        pendingFilePathCallback?.onReceiveValue(null)
        pendingFilePathCallback = filePathCallback
        
        if (filePathCallback == null) return false
        
        // 检查是否需要相机权限
        val needsCamera = isCameraRequired(fileChooserParams)
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        if (needsCamera && !hasCameraPermission) {
            // 先请求相机权限，获得后再启动选择器
            pendingFileChooserParams = fileChooserParams
            cameraForFileChooserPermLauncher.launch(Manifest.permission.CAMERA)
        } else {
            launchFileChooserIntent(fileChooserParams)
        }
        
        return true
    }
    
    /**
     * 判断网页是否要求使用相机
     */
    private fun isCameraRequired(params: WebChromeClient.FileChooserParams?): Boolean {
        if (params == null) return true // 默认包含相机
        
        // capture="camera" / capture="camcorder" / capture="microphone"
        if (params.isCaptureEnabled) return true
        
        // accept 类型为 image/* 或 video/* 时也提供相机选项
        val acceptTypes = params.acceptTypes
        if (acceptTypes != null) {
            for (type in acceptTypes) {
                if (type.startsWith("image/") || type.startsWith("video/")) return true
            }
        }
        
        return true // 默认总是提供相机选项
    }
    
    /**
     * 构建并启动文件选择器 Intent（相机 + 相册 + 文件）
     */
    private fun launchFileChooserIntent(params: WebChromeClient.FileChooserParams?) {
        try {
            val hasCameraPermission = ContextCompat.checkSelfPermission(
                activity, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            
            val extraIntents = mutableListOf<Intent>()
            
            // 1. 相机拍照 Intent
            if (hasCameraPermission) {
                try {
                    val photoFile = createImageFile()
                    cameraPhotoUri = FileProvider.getUriForFile(
                        activity,
                        "${activity.packageName}.fileprovider",
                        photoFile
                    )
                    
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri)
                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                    
                    // 确认设备有相机应用
                    if (cameraIntent.resolveActivity(activity.packageManager) != null) {
                        extraIntents.add(cameraIntent)
                    }
                    
                    // 2. 视频录制 Intent
                    val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                    if (videoIntent.resolveActivity(activity.packageManager) != null) {
                        extraIntents.add(videoIntent)
                    }
                } catch (e: Exception) {
                    AppLogger.e("ShellPermission", "Camera intent creation failed", e)
                }
            }
            
            // 3. 文件/相册选择 Intent（主 Intent）
            val acceptTypes = params?.acceptTypes ?: arrayOf("*/*")
            val mimeType = if (acceptTypes.isNotEmpty() && !acceptTypes[0].isNullOrBlank()) {
                acceptTypes[0]
            } else {
                "*/*"
            }
            
            val contentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
                
                // 允许多选（如果网页支持）
                if (params?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                
                // 如果有多个 accept 类型，设置 EXTRA_MIME_TYPES
                if (acceptTypes.size > 1) {
                    putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes.filter { !it.isNullOrBlank() }.toTypedArray())
                    type = "*/*"
                }
            }
            
            // 4. 组合成 Chooser
            val chooserIntent = Intent.createChooser(contentIntent, null).apply {
                if (extraIntents.isNotEmpty()) {
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toTypedArray())
                }
            }
            
            fileChooserActivityLauncher.launch(chooserIntent)
            AppLogger.d("ShellPermission", "File chooser launched: mimeType=$mimeType, camera=${extraIntents.isNotEmpty()}")
            
        } catch (e: Exception) {
            AppLogger.e("ShellPermission", "Failed to launch file chooser", e)
            pendingFilePathCallback?.onReceiveValue(null)
            pendingFilePathCallback = null
        }
    }
    
    /**
     * 创建临时图片文件（用于相机拍照输出）
     */
    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageDir = File(activity.cacheDir, "camera_photos").apply { mkdirs() }
        return File.createTempFile("IMG_${timestamp}_", ".jpg", imageDir)
    }

    // 外部设置的文件选择回调 (保留兼容旧 API)
    @Deprecated("Use handleFileChooser() instead")
    var onFileChooserResult: ((Array<android.net.Uri>) -> Unit)? = null
    
    // 保留旧 launcher 兼容已有调用（但不再使用）
    val fileChooserLauncher = activity.registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        onFileChooserResult?.invoke(uris.toTypedArray())
    }

    // Storage权限请求
    private val storagePermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permission已授予，执行下载
            pendingDownload?.let { download ->
                DownloadHelper.handleDownload(
                    context = activity,
                    url = download.url,
                    userAgent = download.userAgent,
                    contentDisposition = download.contentDisposition,
                    mimeType = download.mimeType,
                    contentLength = download.contentLength,
                    method = DownloadHelper.DownloadMethod.DOWNLOAD_MANAGER,
                    scope = activity.lifecycleScope
                )
            }
        } else {
            Toast.makeText(activity, Strings.storagePermissionRequired, Toast.LENGTH_SHORT).show()
            // 尝试使用浏览器下载
            pendingDownload?.let { download ->
                DownloadHelper.openInBrowser(activity, download.url)
            }
        }
        pendingDownload = null
    }

    // Permission请求launcher（用于摄像头、麦克风等）
    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        AppLogger.d("ShellActivity", "Permission result received: $permissions")
        val allGranted = permissions.values.all { it }
        AppLogger.d("ShellActivity", "All permissions granted: $allGranted")
        pendingPermissionRequest?.let { request ->
            if (allGranted) {
                AppLogger.d("ShellActivity", "Granting WebView permission request")
                request.grant(request.resources)
            } else {
                AppLogger.d("ShellActivity", "Denying WebView permission request")
                request.deny()
            }
            pendingPermissionRequest = null
        } ?: run {
            AppLogger.w("ShellActivity", "pendingPermissionRequest is null!")
        }
    }

    // 位置权限请求launcher
    private val locationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        pendingGeolocationCallback?.invoke(pendingGeolocationOrigin, granted, false)
        pendingGeolocationOrigin = null
        pendingGeolocationCallback = null
    }

    // 通知权限请求launcher（Android 13+）
    private val notificationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            AppLogger.d("ShellActivity", "通知权限已授予")
        } else {
            AppLogger.d("ShellActivity", "通知权限被拒绝")
        }
    }

    // ===== Public Methods =====

    /**
     * 请求通知权限（Android 13+）
     */
    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * 处理WebView权限请求，先请求Android系统权限
     */
    fun handlePermissionRequest(request: PermissionRequest) {
        val resources = request.resources
        val androidPermissions = mutableListOf<String>()

        AppLogger.d("ShellActivity", "handlePermissionRequest called, resources: ${resources.joinToString()}")

        resources.forEach { resource ->
            AppLogger.d("ShellActivity", "Processing resource: $resource")
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                    androidPermissions.add(Manifest.permission.CAMERA)
                    AppLogger.d("ShellActivity", "Added CAMERA permission request")
                }
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                    androidPermissions.add(Manifest.permission.RECORD_AUDIO)
                    androidPermissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
                    AppLogger.d("ShellActivity", "Added RECORD_AUDIO + MODIFY_AUDIO_SETTINGS permission request")
                }
                PermissionRequest.RESOURCE_MIDI_SYSEX -> {
                    // MIDI SysEx 不需要额外 Android 运行时权限，直接授权
                    AppLogger.d("ShellActivity", "MIDI_SYSEX resource, no Android permission needed")
                }
                PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> {
                    // Protected Media ID 不需要额外 Android 运行时权限，直接授权
                    AppLogger.d("ShellActivity", "PROTECTED_MEDIA_ID resource, no Android permission needed")
                }
                else -> {
                    AppLogger.d("ShellActivity", "Unknown resource: $resource, will grant directly")
                }
            }
        }

        AppLogger.d("ShellActivity", "Android permissions to request: ${androidPermissions.joinToString()}")

        if (androidPermissions.isEmpty()) {
            // 不需要Android权限，直接授权WebView
            AppLogger.d("ShellActivity", "No Android permissions needed, granting WebView request directly")
            request.grant(resources)
        } else {
            // 需要先请求Android权限
            AppLogger.d("ShellActivity", "Requesting Android permissions...")
            pendingPermissionRequest = request
            permissionLauncher.launch(androidPermissions.toTypedArray())
        }
    }

    /**
     * 处理地理位置权限请求
     */
    fun handleGeolocationPermission(origin: String?, callback: GeolocationPermissions.Callback?) {
        pendingGeolocationOrigin = origin
        pendingGeolocationCallback = callback
        locationPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    /**
     * 处理下载（带权限检查）
     */
    fun handleDownloadWithPermission(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
        webView: android.webkit.WebView?
    ) {
        // Create Blob 下载回调
        val onBlobDownload: ((String, String) -> Unit) = { blobUrl, filename ->
            val safeBlobUrl = org.json.JSONObject.quote(blobUrl)
            val safeFilename = org.json.JSONObject.quote(filename)
            // 大文件使用分块处理避免 DOM 冻结
            webView?.evaluateJavascript("""
                (function() {
                    try {
                        const blobUrl = $safeBlobUrl;
                        const filename = $safeFilename;
                        const LARGE_FILE_THRESHOLD = 10 * 1024 * 1024;
                        const CHUNK_SIZE = 1024 * 1024;
                        
                        function uint8ToBase64(u8) {
                            const S = 8192; const p = [];
                            for (let i = 0; i < u8.length; i += S) p.push(String.fromCharCode.apply(null, u8.subarray(i, i + S)));
                            return btoa(p.join(''));
                        }
                        
                        function processChunked(blob, fname) {
                            const mimeType = blob.type || 'application/octet-stream';
                            if (!window.AndroidDownload || !window.AndroidDownload.startChunkedDownload) {
                                processSmall(blob, fname); return;
                            }
                            const did = window.AndroidDownload.startChunkedDownload(fname, mimeType, blob.size);
                            let off = 0, ci = 0; const tc = Math.ceil(blob.size / CHUNK_SIZE);
                            function next() {
                                if (off >= blob.size) { window.AndroidDownload.finishChunkedDownload(did); return; }
                                blob.slice(off, off + CHUNK_SIZE).arrayBuffer().then(function(ab) {
                                    window.AndroidDownload.appendChunk(did, uint8ToBase64(new Uint8Array(ab)), ci, tc);
                                    off += CHUNK_SIZE; ci++;
                                    setTimeout(next, 0);
                                });
                            }
                            next();
                        }
                        
                        function processSmall(blob, fname) {
                            const reader = new FileReader();
                            reader.onloadend = function() {
                                const base64Data = reader.result.split(',')[1];
                                const mimeType = blob.type || 'application/octet-stream';
                                if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                                    window.AndroidDownload.saveBase64File(base64Data, fname, mimeType);
                                }
                            };
                            reader.readAsDataURL(blob);
                        }
                        
                        if (blobUrl.startsWith('data:')) {
                            const parts = blobUrl.split(',');
                            const meta = parts[0];
                            const base64Data = parts[1];
                            const mimeMatch = meta.match(/data:([^;]+)/);
                            const mimeType = mimeMatch ? mimeMatch[1] : 'application/octet-stream';
                            if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                                window.AndroidDownload.saveBase64File(base64Data, filename, mimeType);
                            }
                        } else if (blobUrl.startsWith('blob:')) {
                            fetch(blobUrl)
                                .then(function(r) { return r.blob(); })
                                .then(function(blob) {
                                    if (blob.size > LARGE_FILE_THRESHOLD) {
                                        processChunked(blob, filename);
                                    } else {
                                        processSmall(blob, filename);
                                    }
                                })
                                .catch(function(err) {
                                    console.error('[DownloadHelper] Blob fetch failed:', err);
                                    if (window.AndroidDownload && window.AndroidDownload.showToast) {
                                        window.AndroidDownload.showToast('${Strings.downloadFailedPrefix}' + err.message);
                                    }
                                });
                        }
                    } catch(e) {
                        console.error('[DownloadHelper] Error:', e);
                    }
                })();
            """.trimIndent(), null)
        }

        // Android 10+ 不需要存储权限即可使用 DownloadManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            DownloadHelper.handleDownload(
                context = activity,
                url = url,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
                contentLength = contentLength,
                method = DownloadHelper.DownloadMethod.DOWNLOAD_MANAGER,
                scope = activity.lifecycleScope,
                onBlobDownload = onBlobDownload
            )
            return
        }

        // Android 9 及以下需要检查存储权限
        val hasPermission = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            DownloadHelper.handleDownload(
                context = activity,
                url = url,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
                contentLength = contentLength,
                method = DownloadHelper.DownloadMethod.DOWNLOAD_MANAGER,
                scope = activity.lifecycleScope,
                onBlobDownload = onBlobDownload
            )
        } else {
            // Save下载信息，请求权限
            pendingDownload = PendingDownload(url, userAgent, contentDisposition, mimeType, contentLength)
            storagePermissionLauncher.launch(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
        }
    }
}
