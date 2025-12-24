package com.webtoapp.core.webview

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import android.widget.Toast
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
            android.util.Log.e("NativeBridge", "复制失败", e)
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
                android.util.Log.e("NativeBridge", "分享失败", e)
                Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    @JavascriptInterface
    fun shareImage(imageUrl: String, title: String = "分享图片") {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, "正在准备分享...", Toast.LENGTH_SHORT).show()
        }
        // 图片分享需要先下载，这里简化处理
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
                Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "正在保存图片...", Toast.LENGTH_SHORT).show()
            }
            
            val result = MediaSaver.saveFromUrl(context, imageUrl, finalFilename, "image/jpeg")
            
            withContext(Dispatchers.Main) {
                when (result) {
                    is MediaSaver.SaveResult.Success -> {
                        Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
                    }
                    is MediaSaver.SaveResult.Error -> {
                        Toast.makeText(context, "保存失败: ${result.message}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "正在保存视频...", Toast.LENGTH_SHORT).show()
            }
            
            val result = MediaSaver.saveFromUrl(context, videoUrl, finalFilename, "video/mp4")
            
            withContext(Dispatchers.Main) {
                when (result) {
                    is MediaSaver.SaveResult.Success -> {
                        Toast.makeText(context, "视频已保存到相册", Toast.LENGTH_SHORT).show()
                    }
                    is MediaSaver.SaveResult.Error -> {
                        Toast.makeText(context, "保存失败: ${result.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // ==================== 日志 ====================
    
    @JavascriptInterface
    fun log(message: String) {
        android.util.Log.d("NativeBridge", "[JS] $message")
    }
}
