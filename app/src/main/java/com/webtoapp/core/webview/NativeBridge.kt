package com.webtoapp.core.webview

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.webtoapp.core.i18n.Strings
import com.webtoapp.util.MediaSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 原生能力桥接
 * 
 * 为扩展模块提供调用 Android 原生能力的 JavaScript API
 * 在 WebView 中通过 window.NativeBridge 访问
 */
class NativeBridge(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val JS_INTERFACE_NAME = "NativeBridge"
        
        /**
         * 获取 API 文档（供 AI 生成代码时参考）
         */
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
    }
    
    // ==================== 基础功能 ====================
    
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
            android.util.Log.e("NativeBridge", "震动失败", e)
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
            android.util.Log.e("NativeBridge", "模式震动失败", e)
        }
    }

    
    // ==================== 剪贴板 ====================
    
    @JavascriptInterface
    fun copyToClipboard(text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("text", text)
            clipboard.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            android.util.Log.e("NativeBridge", "Duplication failed", e)
            false
        }
    }
    
    @JavascriptInterface
    fun getClipboardText(): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        } catch (e: Exception) {
            android.util.Log.e("NativeBridge", "获取剪贴板失败", e)
            ""
        }
    }
    
    // ==================== 分享 ====================
    
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
                android.util.Log.e("NativeBridge", "Share failed", e)
                Toast.makeText(context, Strings.shareFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    @JavascriptInterface
    fun shareImage(imageUrl: String, title: String = "分享图片") {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, Strings.preparingShare, Toast.LENGTH_SHORT).show()
        }
        // Image分享需要先下载，这里简化处理
        share(title, imageUrl)
    }
    
    // ==================== 外部操作 ====================
    
    @JavascriptInterface
    fun openUrl(url: String) {
        scope.launch(Dispatchers.Main) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e("NativeBridge", "打开链接失败", e)
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
            android.util.Log.e("NativeBridge", "打开应用失败", e)
            false
        }
    }
    
    // ==================== 媒体保存 ====================
    
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
    
    // ==================== 设备信息 ====================
    
    @JavascriptInterface
    fun getDeviceInfo(): String {
        return try {
            val displayMetrics = context.resources.displayMetrics
            """
            {
                "model": "${Build.MODEL}",
                "manufacturer": "${Build.MANUFACTURER}",
                "brand": "${Build.BRAND}",
                "sdkVersion": ${Build.VERSION.SDK_INT},
                "androidVersion": "${Build.VERSION.RELEASE}",
                "screenWidth": ${displayMetrics.widthPixels},
                "screenHeight": ${displayMetrics.heightPixels},
                "density": ${displayMetrics.density},
                "language": "${java.util.Locale.getDefault().language}"
            }
            """.trimIndent()
        } catch (e: Exception) {
            "{}"
        }
    }
    
    @JavascriptInterface
    fun getAppInfo(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            """
            {
                "packageName": "${context.packageName}",
                "versionName": "${packageInfo.versionName}",
                "versionCode": ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode}
            }
            """.trimIndent()
        } catch (e: Exception) {
            "{}"
        }
    }
    
    // ==================== 网络状态 ====================
    
    @JavascriptInterface
    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                connectivityManager.activeNetworkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    @JavascriptInterface
    fun getNetworkType(): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return "none"
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "none"
                when {
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile"
                    else -> "unknown"
                }
            } else {
                @Suppress("DEPRECATION")
                when (connectivityManager.activeNetworkInfo?.type) {
                    android.net.ConnectivityManager.TYPE_WIFI -> "wifi"
                    android.net.ConnectivityManager.TYPE_MOBILE -> "mobile"
                    else -> "unknown"
                }
            }
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    // ==================== 存储 ====================
    
    @JavascriptInterface
    fun saveToFile(content: String, filename: String, mimeType: String = "text/plain") {
        scope.launch(Dispatchers.IO) {
            try {
                val downloadBridge = DownloadBridge(context, scope)
                val base64Data = android.util.Base64.encodeToString(
                    content.toByteArray(Charsets.UTF_8),
                    android.util.Base64.DEFAULT
                )
                downloadBridge.saveBase64File(base64Data, filename, mimeType)
            } catch (e: Exception) {
                android.util.Log.e("NativeBridge", "保存文件失败", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, Strings.saveFailed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // ==================== 日志 ====================
    
    @JavascriptInterface
    fun log(message: String) {
        android.util.Log.d("NativeBridge", "[JS] $message")
    }
    
    // ==================== 屏幕方向控制 ====================
    
    /**
     * 设置屏幕方向
     * @param orientation "portrait"(竖屏), "landscape"(横屏), "auto"(跟随传感器)
     */
    @JavascriptInterface
    fun setOrientation(orientation: String) {
        scope.launch(Dispatchers.Main) {
            try {
                val activity = context as? Activity ?: return@launch
                activity.requestedOrientation = when (orientation.lowercase()) {
                    "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    "auto", "sensor" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                    "reverse_landscape" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    "reverse_portrait" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
                android.util.Log.d("NativeBridge", "屏幕方向已设置为: $orientation")
            } catch (e: Exception) {
                android.util.Log.e("NativeBridge", "设置屏幕方向失败", e)
            }
        }
    }
    
    /**
     * 获取当前屏幕方向
     * @return "portrait", "landscape", "unknown"
     */
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
    
    /**
     * 锁定当前屏幕方向
     */
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
                android.util.Log.d("NativeBridge", "屏幕方向已锁定")
            } catch (e: Exception) {
                android.util.Log.e("NativeBridge", "锁定屏幕方向失败", e)
            }
        }
    }
    
    /**
     * 解锁屏幕方向（允许自动旋转）
     */
    @JavascriptInterface
    fun unlockOrientation() {
        scope.launch(Dispatchers.Main) {
            try {
                val activity = context as? Activity ?: return@launch
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                android.util.Log.d("NativeBridge", "屏幕方向已解锁")
            } catch (e: Exception) {
                android.util.Log.e("NativeBridge", "解锁屏幕方向失败", e)
            }
        }
    }
    
    // ==================== 下载功能 ====================
    
    /**
     * 下载视频文件
     * @param url 视频 URL
     * @param filename 文件名
     */
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
                android.util.Log.e("NativeBridge", "下载视频失败", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, Strings.downloadFailed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * 带自定义 Headers 下载文件
     * @param url 文件 URL
     * @param filename 文件名
     * @param headersJson JSON 格式的 Headers，如 {"Referer": "https://example.com"}
     */
    @JavascriptInterface
    fun downloadWithHeaders(url: String, filename: String, headersJson: String) {
        scope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, Strings.startDownload.replace("%s", filename), Toast.LENGTH_SHORT).show()
            }
            
            try {
                // Parse headers
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
                android.util.Log.e("NativeBridge", "Download failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, Strings.downloadFailed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // ==================== 屏幕亮度控制 ====================
    
    /**
     * 设置屏幕亮度
     * @param brightness 亮度值 0.0-1.0，-1 表示跟随系统
     */
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
                android.util.Log.d("NativeBridge", "屏幕亮度已设置为: $brightness")
            } catch (e: Exception) {
                android.util.Log.e("NativeBridge", "设置屏幕亮度失败", e)
            }
        }
    }
    
    /**
     * 保持屏幕常亮
     * @param keepOn true 保持常亮，false 恢复正常
     */
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
                android.util.Log.d("NativeBridge", "屏幕常亮: $keepOn")
            } catch (e: Exception) {
                android.util.Log.e("NativeBridge", "设置屏幕常亮失败", e)
            }
        }
    }
    
    // ==================== 全屏控制 ====================
    
    /**
     * 进入全屏模式
     */
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
                android.util.Log.d("NativeBridge", "已进入全屏模式")
            } catch (e: Exception) {
                android.util.Log.e("NativeBridge", "进入全屏失败", e)
            }
        }
    }
    
    /**
     * 退出全屏模式
     */
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
                android.util.Log.d("NativeBridge", "已退出全屏模式")
            } catch (e: Exception) {
                android.util.Log.e("NativeBridge", "退出全屏失败", e)
            }
        }
    }
    
    /**
     * 检查是否处于全屏模式
     */
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
