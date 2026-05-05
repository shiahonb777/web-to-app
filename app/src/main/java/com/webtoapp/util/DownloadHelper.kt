package com.webtoapp.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.i18n.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.UnsupportedEncodingException
import java.net.URLDecoder




object DownloadHelper {

    private const val TAG = "DownloadHelper"


    private const val MAX_RETRY_COUNT = 3
    private const val RETRY_DELAY_MS = 1000L
    private val DOWNLOAD_ALLOWED_SCHEMES = setOf("http", "https")


    private val RFC5987_REGEX = Regex("""filename\*\s*=\s*([^']*)'([^']*)'(.+)""", RegexOption.IGNORE_CASE)
    private val QUOTED_FILENAME_REGEX = Regex("""filename\s*=\s*"([^"]+)""""", RegexOption.IGNORE_CASE)
    private val UNQUOTED_FILENAME_REGEX = Regex("""filename\s*=\s*([^;\s]+)""", RegexOption.IGNORE_CASE)


    private val retryCountMap = mutableMapOf<String, Int>()





    fun parseFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        var fileName: String? = null


        if (!contentDisposition.isNullOrBlank()) {
            fileName = parseContentDisposition(contentDisposition)
        }


        if (fileName.isNullOrBlank()) {
            fileName = parseFileNameFromUrl(url)
        }


        if (fileName.isNullOrBlank()) {
            fileName = URLUtil.guessFileName(url, contentDisposition, mimeType) ?: "download"
        }


        fileName = ensureExtension(fileName, mimeType)

        return fileName
    }









    private fun parseContentDisposition(contentDisposition: String): String? {

        RFC5987_REGEX.find(contentDisposition)?.let { match ->
            val encoding = match.groupValues[1].ifBlank { "UTF-8" }
            val encodedName = match.groupValues[3]
            if (encodedName.isNotBlank()) {
                try {
                    return URLDecoder.decode(encodedName, encoding)
                } catch (e: UnsupportedEncodingException) {

                }
            }
        }


        QUOTED_FILENAME_REGEX.find(contentDisposition)?.let { match ->
            val name = match.groupValues[1]
            if (name.isNotBlank()) {
                return decodeFileName(name)
            }
        }


        UNQUOTED_FILENAME_REGEX.find(contentDisposition)?.let { match ->
            val name = match.groupValues[1]
            if (name.isNotBlank()) {
                return decodeFileName(name)
            }
        }

        return null
    }




    private fun decodeFileName(name: String): String {
        var decoded = name
        try {

            if (name.contains("%")) {
                decoded = URLDecoder.decode(name, "UTF-8")
            }
        } catch (e: Exception) {

        }

        return decoded.trim().removeSurrounding("\"").removeSurrounding("'")
    }




    private fun parseFileNameFromUrl(url: String): String? {
        try {
            val uri = Uri.parse(url)
            val path = uri.path ?: return null


            val lastSegment = path.substringAfterLast('/')
            if (lastSegment.isBlank()) return null


            if (lastSegment.contains('.') && !lastSegment.startsWith('.')) {
                val decoded = try {
                    URLDecoder.decode(lastSegment, "UTF-8")
                } catch (e: Exception) {
                    lastSegment
                }

                if (decoded.length <= 255 && !decoded.contains('?') && !decoded.contains('&')) {
                    return decoded
                }
            }
        } catch (e: Exception) {

        }
        return null
    }




    private fun ensureExtension(fileName: String, mimeType: String?): String {

        val lastDot = fileName.lastIndexOf('.')
        if (lastDot > 0 && lastDot < fileName.length - 1) {
            val ext = fileName.substring(lastDot + 1).lowercase()

            if (ext.length in 2..5 && ext != "bin") {
                return fileName
            }
        }


        if (!mimeType.isNullOrBlank()) {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (!ext.isNullOrBlank()) {
                val baseName = if (lastDot > 0) fileName.substring(0, lastDot) else fileName
                return "$baseName.$ext"
            }
        }

        return fileName
    }




    enum class DownloadMethod {
        DOWNLOAD_MANAGER,
        BROWSER
    }















    fun handleDownload(
        context: Context,
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
        method: DownloadMethod = DownloadMethod.DOWNLOAD_MANAGER,
        showEnhancedNotification: Boolean = true,
        saveToGallery: Boolean = true,
        scope: CoroutineScope? = null,
        onBlobDownload: ((blobUrl: String, filename: String) -> Unit)? = null
    ) {

        if (url.startsWith("blob:")) {
            val filename = parseFileName(url, contentDisposition, mimeType)
            if (onBlobDownload != null) {
                Toast.makeText(context, Strings.blobDownloadProcessing, Toast.LENGTH_SHORT).show()
                onBlobDownload(url, filename)
            } else {

                Toast.makeText(context, Strings.blobDownloadFailed, Toast.LENGTH_SHORT).show()
            }
            return
        }


        if (url.startsWith("data:")) {
            val filename = parseFileName(url, contentDisposition, mimeType)
            if (onBlobDownload != null) {
                Toast.makeText(context, Strings.blobDownloadProcessing, Toast.LENGTH_SHORT).show()
                onBlobDownload(url, filename)
            } else {
                Toast.makeText(context, Strings.blobDownloadFailed, Toast.LENGTH_SHORT).show()
            }
            return
        }

        val safeUrl = sanitizeDownloadUrl(url)
        if (safeUrl.isEmpty()) {
            AppLogger.w(TAG, "Blocked unsafe download URL: $url")
            Toast.makeText(context, Strings.downloadFailed, Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = parseFileName(safeUrl, contentDisposition, mimeType)


        if (saveToGallery && MediaSaver.isMediaFile(mimeType, fileName) && scope != null) {
            saveMediaToGallery(context, safeUrl, fileName, mimeType, scope)
            return
        }

        when (method) {
            DownloadMethod.DOWNLOAD_MANAGER -> {
                downloadWithManager(context, safeUrl, userAgent, contentDisposition, mimeType, showEnhancedNotification)
            }
            DownloadMethod.BROWSER -> {
                openInBrowser(context, safeUrl)
            }
        }
    }




    private fun saveMediaToGallery(
        context: Context,
        url: String,
        fileName: String,
        mimeType: String,
        scope: CoroutineScope
    ) {
        val mediaType = MediaSaver.getMediaType(mimeType) ?: MediaSaver.getMediaTypeByExtension(fileName)
        val typeText = when (mediaType) {
            MediaSaver.MediaType.IMAGE -> if (Strings.currentLanguage.value == com.webtoapp.core.i18n.AppLanguage.CHINESE) "图片" else "image"
            MediaSaver.MediaType.VIDEO -> if (Strings.currentLanguage.value == com.webtoapp.core.i18n.AppLanguage.CHINESE) "视频" else "video"
            else -> if (Strings.currentLanguage.value == com.webtoapp.core.i18n.AppLanguage.CHINESE) "文件" else "file"
        }

        val notificationManager = DownloadNotificationManager.getInstance(context)
        val progressNotificationId = notificationManager.showIndeterminateProgress(fileName)

        val savingMsg = when (mediaType) {
            MediaSaver.MediaType.IMAGE -> Strings.savingImageToGallery
            MediaSaver.MediaType.VIDEO -> Strings.savingVideoToGallery
            else -> Strings.savingToGallery
        }
        Toast.makeText(context, savingMsg, Toast.LENGTH_SHORT).show()

        scope.launch(Dispatchers.Main) {
            val result = MediaSaver.saveFromUrl(context, url, fileName, mimeType)

            when (result) {
                is MediaSaver.SaveResult.Success -> {
                    val savedMsg = when (mediaType) {
                        MediaSaver.MediaType.IMAGE -> Strings.imageSavedToGallery
                        MediaSaver.MediaType.VIDEO -> Strings.videoSavedToGallery
                        else -> Strings.imageSavedToGallery
                    }
                    Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()


                    notificationManager.showMediaSaveComplete(
                        fileName = fileName,
                        uri = result.uri,
                        mimeType = mimeType,
                        isImage = mediaType == MediaSaver.MediaType.IMAGE,
                        progressNotificationId = progressNotificationId
                    )
                }
                is MediaSaver.SaveResult.Error -> {
                    Toast.makeText(context, Strings.saveFailedWithReason.replace("%s", result.message), Toast.LENGTH_SHORT).show()
                    notificationManager.showSaveFailed(fileName, result.message, progressNotificationId)
                }
            }
        }
    }




    fun downloadWithManager(
        context: Context,
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        showEnhancedNotification: Boolean = true,
        retryOnFailure: Boolean = true
    ) {
        val safeUrl = sanitizeDownloadUrl(url)
        if (safeUrl.isEmpty()) {
            AppLogger.w(TAG, "Blocked unsafe download URL in downloadWithManager: $url")
            Toast.makeText(context, Strings.downloadFailed, Toast.LENGTH_SHORT).show()
            return
        }

        try {

            val fileName = parseFileName(safeUrl, contentDisposition, mimeType)
            val originHeader = buildOriginHeader(safeUrl)

            val request = DownloadManager.Request(Uri.parse(safeUrl)).apply {

                addRequestHeader("User-Agent", userAgent)



                CookieManager.getInstance().getCookie(safeUrl)?.let { cookie ->
                    if (cookie.isNotBlank()) {
                        addRequestHeader("Cookie", cookie)
                    }
                }



                originHeader?.let { origin ->
                    addRequestHeader("Origin", origin)
                    addRequestHeader("Referer", "$origin/")
                }


                if (showEnhancedNotification) {

                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                } else {
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                }
                setTitle(fileName)
                setDescription("正在下载...")


                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)


                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or
                    DownloadManager.Request.NETWORK_MOBILE
                )


                if (mimeType.isNotBlank()) {
                    setMimeType(mimeType)
                }
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)


            if (showEnhancedNotification) {
                val notificationManager = DownloadNotificationManager.getInstance(context)
                notificationManager.trackDownload(downloadId, fileName, mimeType)
            }


            retryCountMap.remove(safeUrl)

            Toast.makeText(context, Strings.startDownload.replace("%s", fileName), Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)


            if (retryOnFailure) {
                val currentRetry = retryCountMap[safeUrl] ?: 0
                if (currentRetry < MAX_RETRY_COUNT) {
                    retryCountMap[safeUrl] = currentRetry + 1
                    Toast.makeText(
                        context,
                        "${Strings.downloadFailed}, ${Strings.retry} (${currentRetry + 1}/$MAX_RETRY_COUNT)...",
                        Toast.LENGTH_SHORT
                    ).show()


                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        downloadWithManager(context, safeUrl, userAgent, contentDisposition, mimeType, showEnhancedNotification, true)
                    }, RETRY_DELAY_MS * (currentRetry + 1))
                    return
                }
            }


            retryCountMap.remove(safeUrl)
            Toast.makeText(context, Strings.downloadFailedTryBrowser, Toast.LENGTH_SHORT).show()
            openInBrowser(context, safeUrl)
        }
    }




    fun openInBrowser(context: Context, url: String) {
        try {
            context.openUrl(url)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)
            Toast.makeText(context, Strings.cannotOpenBrowser, Toast.LENGTH_SHORT).show()
        }
    }

    private fun sanitizeDownloadUrl(rawUrl: String): String {
        val normalized = normalizeExternalIntentUrl(rawUrl)
        if (normalized.isEmpty()) return ""
        return if (isAllowedUrlScheme(normalized, DOWNLOAD_ALLOWED_SCHEMES)) normalized else ""
    }

    private fun buildOriginHeader(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme ?: return null
            val host = uri.host ?: return null
            if (!isAllowedUrlScheme(url, DOWNLOAD_ALLOWED_SCHEMES)) return null
            if (uri.port > 0) "$scheme://$host:${uri.port}" else "$scheme://$host"
        } catch (e: Exception) {
            null
        }
    }




    fun guessExtension(url: String, mimeType: String?): String {

        mimeType?.let {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
            if (!ext.isNullOrBlank()) return ".$ext"
        }


        val path = Uri.parse(url).path ?: return ""
        val lastDot = path.lastIndexOf('.')
        if (lastDot >= 0 && lastDot < path.length - 1) {
            val ext = path.substring(lastDot)
            if (ext.length <= 5) return ext
        }

        return ""
    }
}
