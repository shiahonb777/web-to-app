package com.webtoapp.core.webview

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import com.webtoapp.core.logging.AppLogger
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.webtoapp.core.i18n.Strings
import com.webtoapp.util.MediaSaver
import com.webtoapp.util.getUrlScheme
import com.webtoapp.util.isAllowedUrlScheme
import com.webtoapp.util.normalizeExternalIntentUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.TlsVersion
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeUnit







class NativeBridge(
    private val context: Context,
    private val scope: CoroutineScope,
    private val webViewProvider: () -> WebView? = { null }
) {
    companion object {
        const val JS_INTERFACE_NAME = "NativeBridge"
        private const val PRIVATE_NETWORK_MAX_RESPONSE_BYTES = 16 * 1024 * 1024
        private val SKIP_PROXY_HEADERS = setOf(
            "host",
            "connection",
            "content-length",
            "accept-encoding"
        )




        fun getApiDocumentation(): String = """
## NativeBridge API 文档

扩展模块可以通过 `window.NativeBridge` 调用以下原生能力：

### 基础功能

#### showToast(message, duration?)
显示 Toast 提示
- `message`: string - 提示内容
- `duration`: string - 可选，"short"(默认) 或 "long"
```javascript
NativeBridge.showToast('操作成功');
NativeBridge.showToast('请稍候...', 'long');
```

#### vibrate(milliseconds?)
触发震动反馈
- `milliseconds`: number - 震动时长，默认 100ms
```javascript
NativeBridge.vibrate(); // 短震动
NativeBridge.vibrate(500); // 震动 500ms
```

#### vibratePattern(pattern, repeat?)
触发模式震动
- `pattern`: string - 震动模式，逗号分隔的毫秒数，如 "100,200,100"
- `repeat`: number - 重复次数，-1 表示不重复
```javascript
NativeBridge.vibratePattern('100,200,100,200'); // 震动-暂停-震动-暂停
```

### 剪贴板

#### copyToClipboard(text)
复制文本到剪贴板
- `text`: string - 要复制的文本
- 返回: boolean - 是否成功
```javascript
const success = NativeBridge.copyToClipboard('要复制的内容');
if (success) NativeBridge.showToast('已复制');
```

#### getClipboardText()
获取剪贴板文本（需要用户授权）
- 返回: string - 剪贴板内容，失败返回空字符串
```javascript
const text = NativeBridge.getClipboardText();
```

### 分享

#### share(title, text, url?)
调用系统分享
- `title`: string - 分享标题
- `text`: string - 分享内容
- `url`: string - 可选，分享链接
```javascript
NativeBridge.share('分享标题', '分享内容', 'https://example.com');
```

#### shareImage(imageUrl, title?)
分享图片
- `imageUrl`: string - 图片 URL
- `title`: string - 可选，分享标题
```javascript
NativeBridge.shareImage('https://example.com/image.jpg', '分享图片');
```

### 外部操作

#### openUrl(url)
用系统浏览器打开链接
- `url`: string - 要打开的 URL
```javascript
NativeBridge.openUrl('https://www.google.com');
```

#### openApp(packageName)
打开其他应用
- `packageName`: string - 应用包名
- 返回: boolean - 是否成功
```javascript
NativeBridge.openApp('com.tencent.mm'); // 打开微信
```

### 媒体保存

#### saveImageToGallery(imageUrl, filename?)
保存图片到相册
- `imageUrl`: string - 图片 URL
- `filename`: string - 可选，文件名
```javascript
NativeBridge.saveImageToGallery('https://example.com/image.jpg', 'my_image.jpg');
```

#### saveVideoToGallery(videoUrl, filename?)
保存视频到相册
- `videoUrl`: string - 视频 URL
- `filename`: string - 可选，文件名
```javascript
NativeBridge.saveVideoToGallery('https://example.com/video.mp4', 'my_video.mp4');
```

### 下载功能

#### downloadVideo(url, filename)
下载视频文件
- `url`: string - 视频 URL
- `filename`: string - 文件名
```javascript
NativeBridge.downloadVideo('https://example.com/video.mp4', 'my_video.mp4');
```

#### downloadWithHeaders(url, filename, headersJson)
带自定义 Headers 下载文件（用于需要 Referer 等的资源）
- `url`: string - 文件 URL
- `filename`: string - 文件名
- `headersJson`: string - JSON 格式的 Headers
```javascript
NativeBridge.downloadWithHeaders(
    'https://example.com/video.mp4',
    'video.mp4',
    JSON.stringify({ 'Referer': 'https://example.com' })
);
```

### 设备信息

#### getDeviceInfo()
获取设备信息
- 返回: string - JSON 格式的设备信息
```javascript
const info = JSON.parse(NativeBridge.getDeviceInfo());
console.log(info.model, info.sdkVersion, info.screenWidth);
```

#### getAppInfo()
获取应用信息
- 返回: string - JSON 格式的应用信息
```javascript
const info = JSON.parse(NativeBridge.getAppInfo());
console.log(info.packageName, info.versionName);
```

### 网络状态

#### isNetworkAvailable()
检查网络是否可用
- 返回: boolean
```javascript
if (NativeBridge.isNetworkAvailable()) {
    // 有网络
}
```

#### getNetworkType()
获取网络类型
- 返回: string - "wifi", "mobile", "none", "unknown"
```javascript
const type = NativeBridge.getNetworkType();
```

### 存储

#### saveToFile(content, filename, mimeType?)
保存内容到文件
- `content`: string - 文件内容
- `filename`: string - 文件名
- `mimeType`: string - 可选，MIME 类型
```javascript
NativeBridge.saveToFile('文件内容', 'note.txt', 'text/plain');
```

### 日志

#### log(message)
输出日志到 Android Logcat
- `message`: string - 日志内容
```javascript
NativeBridge.log('调试信息');
```

### 页内查找

#### findInPage(query)
使用 Android WebView 原生查找能力搜索当前页面
- `query`: string - 搜索关键词
- 返回: string - JSON 状态，包含 `numberOfMatches`、`activeMatchOrdinal`、`doneCounting`
```javascript
const state = JSON.parse(NativeBridge.findInPage('keyword'));
```

#### findNextInPage(forward)
跳转到下一个/上一个匹配项
- `forward`: boolean - true 下一个，false 上一个
```javascript
NativeBridge.findNextInPage(true);
NativeBridge.findNextInPage(false);
```

#### clearFindInPage()
清除当前查找高亮
```javascript
NativeBridge.clearFindInPage();
```

### 屏幕方向控制

#### setOrientation(orientation)
设置屏幕方向
- `orientation`: string - "portrait"(竖屏), "landscape"(横屏), "auto"(跟随传感器)
```javascript
NativeBridge.setOrientation('landscape'); // 切换到横屏
NativeBridge.setOrientation('portrait');  // 切换回竖屏
NativeBridge.setOrientation('auto');      // 跟随传感器
```

#### getOrientation()
获取当前屏幕方向
- 返回: string - "portrait", "landscape", "unknown"
```javascript
const orientation = NativeBridge.getOrientation();
```

#### lockOrientation()
锁定当前屏幕方向
```javascript
NativeBridge.lockOrientation();
```

#### unlockOrientation()
解锁屏幕方向
```javascript
NativeBridge.unlockOrientation();
```

### 屏幕控制

#### setScreenBrightness(brightness)
设置屏幕亮度
- `brightness`: number - 亮度值 0.0-1.0，-1 表示跟随系统
```javascript
NativeBridge.setScreenBrightness(0.8); // Set为 80% 亮度
NativeBridge.setScreenBrightness(-1);  // 跟随系统
```

#### setKeepScreenOn(keepOn)
保持屏幕常亮
- `keepOn`: boolean - true 保持常亮，false 恢复正常
```javascript
NativeBridge.setKeepScreenOn(true);  // 保持常亮
NativeBridge.setKeepScreenOn(false); // 恢复正常
```

### 全屏控制

#### enterFullscreen()
进入全屏模式（隐藏状态栏和导航栏）
```javascript
NativeBridge.enterFullscreen();
```

#### exitFullscreen()
退出全屏模式
```javascript
NativeBridge.exitFullscreen();
```

#### isFullscreen()
检查是否处于全屏模式
- 返回: boolean
```javascript
if (NativeBridge.isFullscreen()) {
    // 当前是全屏模式
}
```
        """.trimIndent()

        internal fun isPrivateNetworkHost(host: String?): Boolean {
            val normalized = host
                ?.trim()
                ?.trim('[', ']')
                ?.lowercase(Locale.ROOT)
                ?: return false

            if (normalized == "localhost" || normalized == "127.0.0.1" || normalized == "10.0.2.2") {
                return true
            }
            if (normalized == "::1" || normalized == "0:0:0:0:0:0:0:1") return true
            if (normalized.endsWith(".local")) return true

            val parts = normalized.split('.')
            if (parts.size != 4) return false
            val octets = parts.map { it.toIntOrNull() ?: return false }
            if (octets.any { it !in 0..255 }) return false

            return when {
                octets[0] == 10 -> true
                octets[0] == 172 && octets[1] in 16..31 -> true
                octets[0] == 192 && octets[1] == 168 -> true
                octets[0] == 127 -> true
                else -> false
            }
        }

        internal fun isPrivateNetworkUrl(url: String): Boolean {
            val uri = runCatching { URI(url) }.getOrNull() ?: return false
            val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return false
            if (scheme != "http" && scheme != "https") return false
            return isPrivateNetworkHost(uri.host)
        }
    }

    private val privateNetworkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .connectionSpecs(
                listOf(
                    ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                        .build(),
                    ConnectionSpec.CLEARTEXT
                )
            )
            .retryOnConnectionFailure(true)
            .build()
    }



    @JavascriptInterface
    fun showToast(message: String, duration: String = "short") {
        scope.launch(Dispatchers.Main) {
            val length = if (duration == "long") Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            Toast.makeText(context, message, length).show()
        }
    }

    @JavascriptInterface
    fun vibrate(milliseconds: Long = 100) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(milliseconds)
            }
        } catch (e: Exception) {
            AppLogger.e("NativeBridge", "震动失败", e)
        }
    }

    @JavascriptInterface
    fun vibratePattern(pattern: String, repeat: Int = -1) {
        try {
            val timings = pattern.split(",").mapNotNull { it.trim().toLongOrNull() }.toLongArray()
            if (timings.isEmpty()) return

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, repeat))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, repeat)
            }
        } catch (e: Exception) {
            AppLogger.e("NativeBridge", "模式震动失败", e)
        }
    }




    @JavascriptInterface
    fun copyToClipboard(text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("text", text)
            clipboard.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            AppLogger.e("NativeBridge", "Duplication failed", e)
            false
        }
    }

    @JavascriptInterface
    fun getClipboardText(): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        } catch (e: Exception) {
            AppLogger.e("NativeBridge", "获取剪贴板失败", e)
            ""
        }
    }



    @JavascriptInterface
    fun share(title: String, text: String, url: String = "") {
        scope.launch(Dispatchers.Main) {
            try {
                val shareText = if (url.isNotBlank()) "$text\n$url" else text
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, title).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                AppLogger.e("NativeBridge", "Share failed", e)
                Toast.makeText(context, Strings.shareFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    @JavascriptInterface
    fun shareImage(imageUrl: String, title: String = Strings.shareImage) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, Strings.preparingShare, Toast.LENGTH_SHORT).show()
        }

        share(title, imageUrl)
    }




    private val ALLOWED_SCHEMES = setOf("http", "https", "tel", "mailto", "sms", "geo")

    @JavascriptInterface
    fun openUrl(url: String) {
        scope.launch(Dispatchers.Main) {
            try {
                val safeUrl = normalizeExternalIntentUrl(url)
                if (safeUrl.isEmpty()) {
                    AppLogger.w("NativeBridge", "Blocked invalid or dangerous URL in openUrl: $url")
                    Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val scheme = getUrlScheme(safeUrl)
                if (!isAllowedUrlScheme(safeUrl, ALLOWED_SCHEMES)) {
                    AppLogger.w("NativeBridge", "Blocked openUrl with disallowed scheme: $scheme")
                    Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                AppLogger.e("NativeBridge", "打开链接失败", e)
                Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
            }
        }
    }

    @JavascriptInterface
    fun openApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            AppLogger.e("NativeBridge", "打开应用失败", e)
            false
        }
    }



    @JavascriptInterface
    fun getNotificationPermissionState(): String {
        return try {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                return "denied"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (granted) "granted" else "default"
            } else {
                "granted"
            }
        } catch (e: Exception) {
            AppLogger.w("NativeBridge", "读取通知权限状态失败", e)
            "default"
        }
    }

    @JavascriptInterface
    fun requestNotificationPermission(): String {
        val current = getNotificationPermissionState()
        if (current == "granted" || current == "denied") return current

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val activity = context as? Activity
            if (activity != null) {
                scope.launch(Dispatchers.Main) {
                    try {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                            9001
                        )
                    } catch (e: Exception) {
                        AppLogger.w("NativeBridge", "请求通知权限失败", e)
                    }
                }
            }
        }
        return getNotificationPermissionState()
    }

    @JavascriptInterface
    fun showWebNotification(title: String, body: String = "", tag: String = ""): Boolean {
        return try {
            if (getNotificationPermissionState() != "granted") return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return false

            val channelId = "webapp_notifications"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    channelId,
                    Strings.webAppNotificationChannelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                manager.createNotificationChannel(channel)
            }

            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP) }
            val pendingIntent = PendingIntent.getActivity(
                context,
                (tag.ifBlank { title }).hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(context.applicationInfo.icon)
                .setContentTitle(title.ifBlank { context.applicationInfo.loadLabel(context.packageManager).toString() })
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            try {
                NotificationManagerCompat.from(context).notify((tag.ifBlank { "$title|$body" }).hashCode(), notification)
            } catch (e: SecurityException) {
                AppLogger.e("NativeBridge", "通知权限不可用", e)
                return false
            }
            true
        } catch (e: Exception) {
            AppLogger.e("NativeBridge", "显示网页通知失败", e)
            false
        }
    }



    @JavascriptInterface
    fun saveImageToGallery(imageUrl: String, filename: String = "") {
        val finalFilename = filename.ifBlank { "IMG_${System.currentTimeMillis()}.jpg" }

        scope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, Strings.savingImage, Toast.LENGTH_SHORT).show()
            }

            val result = MediaSaver.saveFromUrl(context, imageUrl, finalFilename, "image/jpeg")

            withContext(Dispatchers.Main) {
                when (result) {
                    is MediaSaver.SaveResult.Success -> {
                        Toast.makeText(context, Strings.imageSavedToGallery, Toast.LENGTH_SHORT).show()
                    }
                    is MediaSaver.SaveResult.Error -> {
                        Toast.makeText(context, Strings.saveFailedWithReason.replace("%s", result.message), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    @JavascriptInterface
    fun saveVideoToGallery(videoUrl: String, filename: String = "") {
        val finalFilename = filename.ifBlank { "VID_${System.currentTimeMillis()}.mp4" }

        scope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, Strings.savingVideo, Toast.LENGTH_SHORT).show()
            }

            val result = MediaSaver.saveFromUrl(context, videoUrl, finalFilename, "video/mp4")

            withContext(Dispatchers.Main) {
                when (result) {
                    is MediaSaver.SaveResult.Success -> {
                        Toast.makeText(context, Strings.videoSavedToGallery, Toast.LENGTH_SHORT).show()
                    }
                    is MediaSaver.SaveResult.Error -> {
                        Toast.makeText(context, Strings.saveFailedWithReason.replace("%s", result.message), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }



    @JavascriptInterface
    fun getDeviceInfo(): String {
        return try {
            val displayMetrics = context.resources.displayMetrics

            val json = org.json.JSONObject()
            json.put("model", Build.MODEL)
            json.put("manufacturer", Build.MANUFACTURER)
            json.put("brand", Build.BRAND)
            json.put("sdkVersion", Build.VERSION.SDK_INT)
            json.put("androidVersion", Build.VERSION.RELEASE)
            json.put("screenWidth", displayMetrics.widthPixels)
            json.put("screenHeight", displayMetrics.heightPixels)
            json.put("density", displayMetrics.density.toDouble())
            json.put("language", java.util.Locale.getDefault().language)
            json.toString()
        } catch (e: Exception) {
            "{}"
        }
    }

    @JavascriptInterface
    fun getAppInfo(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val json = org.json.JSONObject()
            json.put("packageName", context.packageName)
            json.put("versionName", packageInfo.versionName ?: "")
            json.put("versionCode", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode.toLong())
            json.toString()
        } catch (e: Exception) {
            "{}"
        }
    }



    @JavascriptInterface
    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    @JavascriptInterface
    fun getNetworkType(): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return "none"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "none"
            when {
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile"
                else -> "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }




    private val downloadBridge by lazy { DownloadBridge(context, scope) }

    @JavascriptInterface
    fun saveToFile(content: String, filename: String, mimeType: String = "text/plain") {
        scope.launch(Dispatchers.IO) {
            try {
                val base64Data = android.util.Base64.encodeToString(
                    content.toByteArray(Charsets.UTF_8),
                    android.util.Base64.DEFAULT
                )
                downloadBridge.saveBase64File(base64Data, filename, mimeType)
            } catch (e: Exception) {
                AppLogger.e("NativeBridge", "保存文件失败", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, Strings.saveFailed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    @JavascriptInterface
    fun log(message: String) {
        AppLogger.d("NativeBridge", "[JS] $message")
    }

    @JavascriptInterface
    fun httpRequest(requestJson: String): String {
        return try {
            val request = org.json.JSONObject(requestJson)
            val url = request.optString("url").trim()
            if (!isPrivateNetworkUrl(url)) {
                AppLogger.w("NativeBridge", "Blocked private-network bridge request to non-private URL: $url")
                return privateNetworkBridgeError("URL_NOT_ALLOWED", "Only private network HTTP(S) URLs are allowed")
            }
            val pageUrl = webViewProvider()?.url.orEmpty()
            if (!isPrivateNetworkUrl(pageUrl)) {
                AppLogger.w("NativeBridge", "Blocked private-network bridge request from non-local page: $pageUrl -> $url")
                return privateNetworkBridgeError("CALLER_NOT_ALLOWED", "Only packaged local pages can use the private network bridge")
            }

            val method = request.optString("method", "GET").uppercase(Locale.ROOT)
            if (method !in setOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")) {
                return privateNetworkBridgeError("METHOD_NOT_ALLOWED", "Unsupported HTTP method: $method")
            }

            val bodyBase64 = request.optString("bodyBase64", "")
            val bodyBytes = if (bodyBase64.isBlank()) {
                ByteArray(0)
            } else {
                Base64.decode(bodyBase64, Base64.NO_WRAP)
            }

            val headers = request.optJSONObject("headers")
            val contentType = headers?.optString("content-type")
                ?.takeIf { it.isNotBlank() }
                ?: headers?.optString("Content-Type")?.takeIf { it.isNotBlank() }
                ?: "application/octet-stream"
            val requestBody = if (method in setOf("POST", "PUT", "PATCH") || bodyBytes.isNotEmpty()) {
                bodyBytes.toRequestBody(contentType.toMediaTypeOrNull())
            } else {
                null
            }

            val builder = Request.Builder().url(url)
            headers?.keys()?.forEach { key ->
                val normalizedKey = key.lowercase(Locale.ROOT)
                if (normalizedKey !in SKIP_PROXY_HEADERS) {
                    val value = headers.optString(key)
                    if (value.isNotBlank()) builder.header(key, value)
                }
            }
            builder.header("X-WebToApp-Private-Network-Bridge", "1")
            builder.method(method, requestBody)

            privateNetworkHttpClient.newCall(builder.build()).execute().use { response ->
                val responseBody = response.body
                val bytes = responseBody?.byteStream()?.use { input ->
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        total += read
                        if (total > PRIVATE_NETWORK_MAX_RESPONSE_BYTES) {
                            return privateNetworkBridgeError(
                                "RESPONSE_TOO_LARGE",
                                "Private network response exceeds ${PRIVATE_NETWORK_MAX_RESPONSE_BYTES / 1024 / 1024}MB"
                            )
                        }
                        out.write(buffer, 0, read)
                    }
                    out.toByteArray()
                } ?: ByteArray(0)

                val responseHeaders = org.json.JSONObject()
                response.headers.names().forEach { name ->
                    responseHeaders.put(name, response.headers.values(name).joinToString(", "))
                }

                org.json.JSONObject().apply {
                    put("ok", true)
                    put("status", response.code)
                    put("statusText", response.message)
                    put("url", response.request.url.toString())
                    put("headers", responseHeaders)
                    put("bodyBase64", Base64.encodeToString(bytes, Base64.NO_WRAP))
                }.toString()
            }
        } catch (e: Exception) {
            AppLogger.e("NativeBridge", "Private network HTTP bridge request failed", e)
            privateNetworkBridgeError("REQUEST_FAILED", e.message ?: e::class.java.simpleName)
        }
    }

    private fun privateNetworkBridgeError(code: String, message: String): String {
        return org.json.JSONObject().apply {
            put("ok", false)
            put("error", code)
            put("message", message)
        }.toString()
    }



    @Volatile
    private var findQuery: String = ""
    @Volatile
    private var findActiveMatchOrdinal: Int = -1
    @Volatile
    private var findNumberOfMatches: Int = 0
    @Volatile
    private var findDoneCounting: Boolean = true

    private fun buildFindStateJson(supported: Boolean = webViewProvider() != null): String {
        return try {
            org.json.JSONObject().apply {
                put("supported", supported)
                put("query", findQuery)
                put("activeMatchOrdinal", findActiveMatchOrdinal)
                put("numberOfMatches", findNumberOfMatches)
                put("doneCounting", findDoneCounting)
                put("displayIndex", if (findNumberOfMatches > 0 && findActiveMatchOrdinal >= 0) findActiveMatchOrdinal + 1 else 0)
            }.toString()
        } catch (_: Exception) {
            """{"supported":false,"query":"","activeMatchOrdinal":-1,"numberOfMatches":0,"doneCounting":true,"displayIndex":0}"""
        }
    }

    private fun publishFindState(webView: WebView?) {
        val target = webView ?: return
        val payload = buildFindStateJson(supported = true)
        target.evaluateJavascript(
            """
            (function(){
                if (typeof window.__wtaFindInPageNativeUpdate === 'function') {
                    window.__wtaFindInPageNativeUpdate($payload);
                }
            })();
            """.trimIndent(),
            null
        )
    }

    @JavascriptInterface
    fun findInPage(query: String): String {
        val safeQuery = query.trim().take(200)
        if (safeQuery.isBlank()) return clearFindInPage()

        findQuery = safeQuery
        findActiveMatchOrdinal = -1
        findNumberOfMatches = 0
        findDoneCounting = false

        scope.launch(Dispatchers.Main) {
            try {
                val webView = webViewProvider()
                if (webView == null) {
                    findDoneCounting = true
                    return@launch
                }

                webView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
                    findActiveMatchOrdinal = if (numberOfMatches > 0) activeMatchOrdinal else -1
                    findNumberOfMatches = numberOfMatches
                    findDoneCounting = isDoneCounting
                    publishFindState(webView)
                }
                webView.findAllAsync(safeQuery)
                publishFindState(webView)
            } catch (e: Exception) {
                AppLogger.e("NativeBridge", "页内查找失败", e)
                findDoneCounting = true
            }
        }

        return buildFindStateJson()
    }

    @JavascriptInterface
    fun findNextInPage(forward: Boolean): String {
        scope.launch(Dispatchers.Main) {
            try {
                val webView = webViewProvider() ?: return@launch
                if (findQuery.isNotBlank()) {
                    webView.findNext(forward)
                    publishFindState(webView)
                }
            } catch (e: Exception) {
                AppLogger.e("NativeBridge", "页内查找跳转失败", e)
            }
        }
        return buildFindStateJson()
    }

    @JavascriptInterface
    fun clearFindInPage(): String {
        findQuery = ""
        findActiveMatchOrdinal = -1
        findNumberOfMatches = 0
        findDoneCounting = true

        scope.launch(Dispatchers.Main) {
            try {
                val webView = webViewProvider() ?: return@launch
                webView.clearMatches()
                publishFindState(webView)
            } catch (e: Exception) {
                AppLogger.e("NativeBridge", "清除页内查找失败", e)
            }
        }

        return buildFindStateJson()
    }







    @JavascriptInterface
    fun setOrientation(orientation: String) {
        scope.launch(Dispatchers.Main) {
            try {
                val activity = context as? Activity ?: return@launch
                activity.requestedOrientation = when (orientation.lowercase()) {
                    "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    "auto" -> ActivityInfo.SCREEN_ORIENTATION_USER
                    "sensor" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                    "reverse_landscape" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    "reverse_portrait" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
                AppLogger.d("NativeBridge", "屏幕方向已设置为: $orientation")
            } catch (e: Exception) {
                AppLogger.e("NativeBridge", "设置屏幕方向失败", e)
            }
        }
    }





    @JavascriptInterface
    fun getOrientation(): String {
        return try {
            val displayMetrics = context.resources.displayMetrics
            if (displayMetrics.widthPixels > displayMetrics.heightPixels) {
                "landscape"
            } else {
                "portrait"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }




    @JavascriptInterface
    fun lockOrientation() {
        scope.launch(Dispatchers.Main) {
            try {
                val activity = context as? Activity ?: return@launch
                val currentOrientation = context.resources.configuration.orientation
                activity.requestedOrientation = if (currentOrientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                AppLogger.d("NativeBridge", "屏幕方向已锁定")
            } catch (e: Exception) {
                AppLogger.e("NativeBridge", "锁定屏幕方向失败", e)
            }
        }
    }




    @JavascriptInterface
    fun unlockOrientation() {
        scope.launch(Dispatchers.Main) {
            try {
                val activity = context as? Activity ?: return@launch
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
                AppLogger.d("NativeBridge", "屏幕方向已解锁")
            } catch (e: Exception) {
                AppLogger.e("NativeBridge", "解锁屏幕方向失败", e)
            }
        }
    }








    @JavascriptInterface
    fun downloadVideo(url: String, filename: String) {
        scope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, Strings.startDownload.replace("%s", filename), Toast.LENGTH_SHORT).show()
            }

            try {
                val result = MediaSaver.saveFromUrl(context, url, filename, "video/mp4")
                withContext(Dispatchers.Main) {
                    when (result) {
                        is MediaSaver.SaveResult.Success -> {
                            Toast.makeText(context, Strings.downloadComplete.replace("%s", filename), Toast.LENGTH_SHORT).show()
                        }
                        is MediaSaver.SaveResult.Error -> {
                            Toast.makeText(context, Strings.downloadFailedWithReason.replace("%s", result.message), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("NativeBridge", "下载视频失败", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, Strings.downloadFailed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }







    @JavascriptInterface
    fun downloadWithHeaders(url: String, filename: String, headersJson: String) {
        scope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, Strings.startDownload.replace("%s", filename), Toast.LENGTH_SHORT).show()
            }

            try {

                val headers = try {
                    val map = mutableMapOf<String, String>()
                    val json = org.json.JSONObject(headersJson)
                    json.keys().forEach { key ->
                        map[key] = json.getString(key)
                    }
                    map
                } catch (e: Exception) {
                    emptyMap()
                }

                val result = MediaSaver.saveFromUrlWithHeaders(context, url, filename, headers)
                withContext(Dispatchers.Main) {
                    when (result) {
                        is MediaSaver.SaveResult.Success -> {
                            Toast.makeText(context, Strings.downloadComplete.replace("%s", filename), Toast.LENGTH_SHORT).show()
                        }
                        is MediaSaver.SaveResult.Error -> {
                            Toast.makeText(context, Strings.downloadFailedWithReason.replace("%s", result.message), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("NativeBridge", "Download failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, Strings.downloadFailed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }







    @JavascriptInterface
    fun setScreenBrightness(brightness: Float) {
        scope.launch(Dispatchers.Main) {
            try {
                val activity = context as? Activity ?: return@launch
                val layoutParams = activity.window.attributes
                layoutParams.screenBrightness = if (brightness < 0) {
                    android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                } else {
                    brightness.coerceIn(0f, 1f)
                }
                activity.window.attributes = layoutParams
                AppLogger.d("NativeBridge", "屏幕亮度已设置为: $brightness")
            } catch (e: Exception) {
                AppLogger.e("NativeBridge", "设置屏幕亮度失败", e)
            }
        }
    }





    @JavascriptInterface
    fun setKeepScreenOn(keepOn: Boolean) {
        scope.launch(Dispatchers.Main) {
            try {
                val activity = context as? Activity ?: return@launch
                if (keepOn) {
                    activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                AppLogger.d("NativeBridge", "屏幕常亮: $keepOn")
            } catch (e: Exception) {
                AppLogger.e("NativeBridge", "设置屏幕常亮失败", e)
            }
        }
    }






    @JavascriptInterface
    fun enterFullscreen() {
        scope.launch(Dispatchers.Main) {
            try {
                val activity = context as? Activity ?: return@launch
                val decorView = activity.window.decorView

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.window.insetsController?.hide(
                        android.view.WindowInsets.Type.statusBars() or
                        android.view.WindowInsets.Type.navigationBars()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    decorView.systemUiVisibility = (
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
                }
                AppLogger.d("NativeBridge", "已进入全屏模式")
            } catch (e: Exception) {
                AppLogger.e("NativeBridge", "进入全屏失败", e)
            }
        }
    }




    @JavascriptInterface
    fun exitFullscreen() {
        scope.launch(Dispatchers.Main) {
            try {
                val activity = context as? Activity ?: return@launch
                val decorView = activity.window.decorView

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.window.insetsController?.show(
                        android.view.WindowInsets.Type.statusBars() or
                        android.view.WindowInsets.Type.navigationBars()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                }
                AppLogger.d("NativeBridge", "已退出全屏模式")
            } catch (e: Exception) {
                AppLogger.e("NativeBridge", "退出全屏失败", e)
            }
        }
    }




    @JavascriptInterface
    fun isFullscreen(): Boolean {
        return try {
            val activity = context as? Activity ?: return false
            val decorView = activity.window.decorView

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val insets = decorView.rootWindowInsets
                insets?.isVisible(android.view.WindowInsets.Type.statusBars()) == false
            } else {
                @Suppress("DEPRECATION")
                (decorView.systemUiVisibility and android.view.View.SYSTEM_UI_FLAG_FULLSCREEN) != 0
            }
        } catch (e: Exception) {
            false
        }
    }
}
