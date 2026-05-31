package com.webtoapp.core.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.webtoapp.core.logging.AppLogger
import java.io.File
import java.security.MessageDigest

object ApkUpdateInstaller {

    private const val TAG = "ApkUpdateInstaller"

    fun download(
        context: Context,
        url: String,
        version: String,
        expectedSha256: String? = null,
        onStarted: (() -> Unit)? = null,
        onVerificationFailed: (() -> Unit)? = null
    ) {
        try {
            val appContext = context.applicationContext
            val fileName = "web-to-app-$version.apk"

            val targetDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            File(targetDir, fileName).takeIf { it.exists() }?.delete()

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("WebToApp $version")
                setMimeType("application/vnd.android.package-archive")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )
            }

            val dm = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)
            registerCompletionReceiver(appContext, dm, downloadId, expectedSha256, onVerificationFailed)
            onStarted?.invoke()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to enqueue update download", e)
        }
    }

    private fun registerCompletionReceiver(
        context: Context,
        dm: DownloadManager,
        downloadId: Long,
        expectedSha256: String?,
        onVerificationFailed: (() -> Unit)?
    ) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id != downloadId) return
                try {
                    context.unregisterReceiver(this)
                } catch (_: Exception) {
                }
                val localUri = resolveLocalUri(dm, downloadId) ?: return
                val file = localUri.path?.let { File(it) }
                if (expectedSha256 != null && file != null && file.exists()) {
                    val actual = sha256Of(file)
                    if (actual != null && !actual.equals(expectedSha256, ignoreCase = true)) {
                        AppLogger.e(TAG, "APK integrity check failed: expected=$expectedSha256 actual=$actual")
                        file.delete()
                        onVerificationFailed?.invoke()
                        return
                    }
                    AppLogger.i(TAG, "APK integrity verified (sha256 match)")
                }
                installApk(context, localUri)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun resolveLocalUri(dm: DownloadManager, downloadId: Long): Uri? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = dm.query(query)
        cursor?.use {
            if (it.moveToFirst()) {
                val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val uriIndex = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                if (statusIndex >= 0 && it.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL && uriIndex >= 0) {
                    return it.getString(uriIndex)?.let { s -> Uri.parse(s) }
                }
            }
        }
        return null
    }

    private fun sha256Of(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to compute sha256: ${e.message}")
            null
        }
    }

    private fun installApk(context: Context, localUri: Uri) {
        try {
            val installUri = if (localUri.scheme == "file") {
                val file = File(localUri.path ?: return)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                localUri
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(installUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to launch APK installer", e)
        }
    }
}
