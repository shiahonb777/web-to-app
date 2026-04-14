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
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.util.DownloadHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShellPermissionDelegate(private val activity: AppCompatActivity) {
    private var pendingPermissionRequest: PermissionRequest? = null
    private var pendingGeolocationOrigin: String? = null
    private var pendingGeolocationCallback: GeolocationPermissions.Callback? = null

    private var pendingDownload: PendingDownload? = null

    private data class PendingDownload(
        val url: String,
        val userAgent: String,
        val contentDisposition: String,
        val mimeType: String,
        val contentLength: Long
    )
    
    private var cameraPhotoUri: Uri? = null
    private var pendingFilePathCallback: android.webkit.ValueCallback<Array<Uri>>? = null
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
            
            val data = result.data
            if (data == null || (data.data == null && data.clipData == null)) {
                cameraPhotoUri?.let { resultUris.add(it) }
            } else {
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
            AppLogger.d("ShellPermission", "File chooser cancelled")
            callback.onReceiveValue(null)
        }
        
        pendingFilePathCallback = null
        cameraPhotoUri = null
    }
    
    private var pendingFileChooserParams: WebChromeClient.FileChooserParams? = null
    private val cameraForFileChooserPermLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        launchFileChooserIntent(pendingFileChooserParams)
        pendingFileChooserParams = null
    }

    fun handleFileChooser(
        filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean {
        pendingFilePathCallback?.onReceiveValue(null)
        pendingFilePathCallback = filePathCallback
        
        if (filePathCallback == null) return false
        
        val needsCamera = isCameraRequired(fileChooserParams)
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        if (needsCamera && !hasCameraPermission) {
            pendingFileChooserParams = fileChooserParams
            cameraForFileChooserPermLauncher.launch(Manifest.permission.CAMERA)
        } else {
            launchFileChooserIntent(fileChooserParams)
        }
        
        return true
    }
    
    private fun isCameraRequired(params: WebChromeClient.FileChooserParams?): Boolean {
        if (params == null) return true
        if (params.isCaptureEnabled) return true

        val acceptTypes = params.acceptTypes
        if (acceptTypes != null) {
            for (type in acceptTypes) {
                if (type.startsWith("image/") || type.startsWith("video/")) return true
            }
        }
        
        return true // default
    }
    
    private fun launchFileChooserIntent(params: WebChromeClient.FileChooserParams?) {
        try {
            val hasCameraPermission = ContextCompat.checkSelfPermission(
                activity, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            
            val extraIntents = mutableListOf<Intent>()
            
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
                    
                    if (cameraIntent.resolveActivity(activity.packageManager) != null) {
                        extraIntents.add(cameraIntent)
                    }

                    val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                    if (videoIntent.resolveActivity(activity.packageManager) != null) {
                        extraIntents.add(videoIntent)
                    }
                } catch (e: Exception) {
                    AppLogger.e("ShellPermission", "Camera intent creation failed", e)
                }
            }
            
            val acceptTypes = params?.acceptTypes ?: arrayOf("*/*")
            val mimeType = if (acceptTypes.isNotEmpty() && !acceptTypes[0].isNullOrBlank()) {
                acceptTypes[0]
            } else {
                "*/*"
            }
            
            val contentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
                
                if (params?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }

                if (acceptTypes.size > 1) {
                    putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes.filter { !it.isNullOrBlank() }.toTypedArray())
                    type = "*/*"
                }
            }

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
    
    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageDir = File(activity.cacheDir, "camera_photos").apply { mkdirs() }
        return File.createTempFile("IMG_${timestamp}_", ".jpg", imageDir)
    }

    private val storagePermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
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
            Toast.makeText(activity, AppStringsProvider.current().storagePermissionRequired, Toast.LENGTH_SHORT).show()
            pendingDownload?.let { download ->
                DownloadHelper.openInBrowser(activity, download.url)
            }
        }
        pendingDownload = null
    }

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

    private val locationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        pendingGeolocationCallback?.invoke(pendingGeolocationOrigin, granted, false)
        pendingGeolocationOrigin = null
        pendingGeolocationCallback = null
    }

    private val notificationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            AppLogger.d("ShellActivity", "通知权限已授予")
        } else {
            AppLogger.d("ShellActivity", "通知权限被拒绝")
        }
    }

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
                PermissionRequest.RESOURCE_MIDI_SYSEX ->
                    AppLogger.d("ShellActivity", "MIDI_SYSEX resource, no Android permission needed")
                PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID ->
                    AppLogger.d("ShellActivity", "PROTECTED_MEDIA_ID resource, no Android permission needed")
                else -> {
                    AppLogger.d("ShellActivity", "Unknown resource: $resource, will grant directly")
                }
            }
        }

        AppLogger.d("ShellActivity", "Android permissions to request: ${androidPermissions.joinToString()}")

        if (androidPermissions.isEmpty()) {
            AppLogger.d("ShellActivity", "No Android permissions needed, granting WebView request directly")
            request.grant(resources)
        } else {
            AppLogger.d("ShellActivity", "Requesting Android permissions...")
            pendingPermissionRequest = request
            permissionLauncher.launch(androidPermissions.toTypedArray())
        }
    }

    fun handleGeolocationPermission(origin: String?, callback: GeolocationPermissions.Callback?) {
        pendingGeolocationOrigin = origin
        pendingGeolocationCallback = callback
        locationPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    fun handleDownloadWithPermission(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
        webView: android.webkit.WebView?
    ) {
        val onBlobDownload: ((String, String) -> Unit) = { blobUrl, filename ->
            val safeBlobUrl = org.json.JSONObject.quote(blobUrl)
            val safeFilename = org.json.JSONObject.quote(filename)
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
                                        window.AndroidDownload.showToast('${AppStringsProvider.current().downloadFailedPrefix}' + err.message);
                                    }
                                });
                        }
                    } catch(e) {
                        console.error('[DownloadHelper] Error:', e);
                    }
                })();
            """.trimIndent(), null)
        }

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
            pendingDownload = PendingDownload(url, userAgent, contentDisposition, mimeType, contentLength)
            storagePermissionLauncher.launch(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
        }
    }
}
