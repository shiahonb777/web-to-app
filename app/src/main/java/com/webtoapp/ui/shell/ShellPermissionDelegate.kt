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
        if (params == null) return false


        if (params.isCaptureEnabled) return true


        val acceptTypes = params.acceptTypes
        if (acceptTypes == null || acceptTypes.isEmpty() || (acceptTypes.size == 1 && acceptTypes[0].isNullOrBlank())) {

            return true
        }

        for (type in acceptTypes) {
            if (type.isNullOrBlank()) continue
            val lower = type.lowercase()

            if (lower.startsWith("image/") || lower.startsWith("video/")) return true

            if (lower in setOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".heic", ".heif",
                    ".bmp", ".svg", ".mp4", ".mov", ".avi", ".mkv", ".webm", ".3gp")) return true
        }

        return false
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


            val rawAcceptTypes = params?.acceptTypes ?: arrayOf("*/*")

            val resolvedMimeTypes = rawAcceptTypes
                .filter { !it.isNullOrBlank() }
                .map { type ->
                    if (type.startsWith(".")) {

                        extensionToMimeType(type.lowercase())
                    } else {
                        type
                    }
                }
                .distinct()

            val mimeType = when {
                resolvedMimeTypes.isEmpty() -> "*/*"
                resolvedMimeTypes.size == 1 -> resolvedMimeTypes[0]
                else -> "*/*"
            }

            val contentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType


                if (params?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }


                if (resolvedMimeTypes.size > 1) {
                    putExtra(Intent.EXTRA_MIME_TYPES, resolvedMimeTypes.toTypedArray())
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








    private fun extensionToMimeType(ext: String): String {
        return when (ext) {

            ".json" -> "application/json"
            ".xml" -> "application/xml"
            ".csv" -> "text/csv"
            ".txt" -> "text/plain"
            ".pdf" -> "application/pdf"
            ".doc" -> "application/msword"
            ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ".xls" -> "application/vnd.ms-excel"
            ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ".ppt" -> "application/vnd.ms-powerpoint"
            ".pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            ".rtf" -> "application/rtf"
            ".odt" -> "application/vnd.oasis.opendocument.text"
            ".ods" -> "application/vnd.oasis.opendocument.spreadsheet"

            ".html", ".htm" -> "text/html"
            ".css" -> "text/css"
            ".js" -> "application/javascript"
            ".ts" -> "application/typescript"
            ".py" -> "text/x-python"
            ".java" -> "text/x-java-source"
            ".kt" -> "text/x-kotlin"
            ".yaml", ".yml" -> "application/x-yaml"
            ".toml" -> "application/toml"
            ".ini", ".conf", ".cfg" -> "text/plain"
            ".md" -> "text/markdown"
            ".log" -> "text/plain"

            ".jpg", ".jpeg" -> "image/jpeg"
            ".png" -> "image/png"
            ".gif" -> "image/gif"
            ".webp" -> "image/webp"
            ".svg" -> "image/svg+xml"
            ".bmp" -> "image/bmp"
            ".heic", ".heif" -> "image/heif"
            ".ico" -> "image/x-icon"

            ".mp3" -> "audio/mpeg"
            ".wav" -> "audio/wav"
            ".ogg" -> "audio/ogg"
            ".flac" -> "audio/flac"
            ".aac" -> "audio/aac"
            ".m4a" -> "audio/mp4"

            ".mp4" -> "video/mp4"
            ".webm" -> "video/webm"
            ".mkv" -> "video/x-matroska"
            ".avi" -> "video/x-msvideo"
            ".mov" -> "video/quicktime"
            ".3gp" -> "video/3gpp"

            ".zip" -> "application/zip"
            ".tar" -> "application/x-tar"
            ".gz", ".gzip" -> "application/gzip"
            ".rar" -> "application/vnd.rar"
            ".7z" -> "application/x-7z-compressed"

            ".apk" -> "application/vnd.android.package-archive"
            ".wasm" -> "application/wasm"
            ".sql" -> "application/sql"
            ".db", ".sqlite" -> "application/x-sqlite3"
            else -> {

                val extWithoutDot = ext.removePrefix(".")
                android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extWithoutDot)
                    ?: "application/octet-stream"
            }
        }
    }


    @Deprecated("Use handleFileChooser() instead")
    var onFileChooserResult: ((Array<android.net.Uri>) -> Unit)? = null


    val fileChooserLauncher = activity.registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        onFileChooserResult?.invoke(uris.toTypedArray())
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
            Toast.makeText(activity, Strings.storagePermissionRequired, Toast.LENGTH_SHORT).show()

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
                    AppLogger.d("ShellActivity", "Added RECORD_AUDIO permission request (MODIFY_AUDIO_SETTINGS is normal, auto-granted)")
                }
                PermissionRequest.RESOURCE_MIDI_SYSEX -> {

                    AppLogger.d("ShellActivity", "MIDI_SYSEX resource, no Android permission needed")
                }
                PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> {

                    AppLogger.d("ShellActivity", "PROTECTED_MEDIA_ID resource, no Android permission needed")
                }
                else -> {
                    AppLogger.d("ShellActivity", "Unknown resource: $resource, will grant directly")
                }
            }
        }


        val uniquePermissions = androidPermissions.distinct()

        AppLogger.d("ShellActivity", "Android permissions to request: ${uniquePermissions.joinToString()}")

        if (uniquePermissions.isEmpty()) {

            AppLogger.d("ShellActivity", "No Android permissions needed, granting WebView request directly")
            request.grant(resources)
            return
        }


        val notGranted = uniquePermissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(
                activity, it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {

            AppLogger.d("ShellActivity", "All permissions already granted, granting WebView request directly")
            request.grant(resources)
        } else {

            AppLogger.d("ShellActivity", "Requesting Android permissions: ${notGranted.joinToString()}")
            pendingPermissionRequest = request
            permissionLauncher.launch(notGranted.toTypedArray())
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
