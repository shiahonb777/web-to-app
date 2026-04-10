package com.webtoapp.core.crypto

import android.annotation.SuppressLint
import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellConfig
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * 安全资源加载器
 * 统一处理加密和非加密资源的加载
 */
@SuppressLint("StaticFieldLeak")
class SecureAssetLoader(private val context: Context) {
    
    companion object {
        private const val TAG = "SecureAssetLoader"
        
        @Volatile
        private var instance: SecureAssetLoader? = null
        
        fun getInstance(context: Context): SecureAssetLoader {
            return instance ?: synchronized(this) {
                instance ?: SecureAssetLoader(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val decryptor = AssetDecryptor(context)
    private val integrityChecker = IntegrityChecker(context)
    private val gson = com.webtoapp.util.GsonProvider.gson
    
    // Cache已加载的资源
    private val cache = ConcurrentHashMap<String, ByteArray>()
    
    // Yes否启用完整性检查
    var integrityCheckEnabled = true
    
    /**
     * 加载配置文件
     */
    fun loadConfig(): ShellConfig? {
        return try {
            // Execute完整性检查
            if (integrityCheckEnabled && !integrityChecker.quickCheck()) {
                AppLogger.w(TAG, "完整性检查失败，拒绝加载配置")
                return null
            }
            
            val configJson = loadAssetAsString(CryptoConstants.CONFIG_FILE)
            gson.fromJson(configJson, ShellConfig::class.java)
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "加载配置失败", e)
            null
        }
    }
    
    /**
     * 加载 HTML 文件
     */
    fun loadHtml(htmlPath: String): String {
        val fullPath = if (htmlPath.startsWith("html/")) htmlPath else "html/$htmlPath"
        return loadAssetAsString(fullPath)
    }
    
    /**
     * 加载资源为字节数组
     */
    fun loadAsset(assetPath: String): ByteArray {
        // Check缓存
        cache[assetPath]?.let { return it }
        
        val data = decryptor.loadAsset(assetPath)
        
        // Cache小文件
        if (data.size < 1024 * 1024) { // < 1MB
            cache[assetPath] = data
        }
        
        return data
    }
    
    /**
     * 加载资源为字符串
     */
    fun loadAssetAsString(assetPath: String): String {
        return String(loadAsset(assetPath), Charsets.UTF_8)
    }
    
    /**
     * 获取资源输入流
     */
    fun openAsset(assetPath: String): InputStream {
        return loadAsset(assetPath).inputStream()
    }
    
    /**
     * 检查资源是否存在
     */
    fun assetExists(assetPath: String): Boolean {
        return decryptor.assetExists(assetPath)
    }
    
    /**
     * 检查资源是否加密
     */
    fun isEncrypted(assetPath: String): Boolean {
        return decryptor.isEncrypted(assetPath)
    }
    
    /**
     * 执行完整性检查
     */
    fun checkIntegrity(): IntegrityResult {
        return integrityChecker.check()
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        cache.clear()
        decryptor.clearCache()
    }
    
    /**
     * 预加载常用资源
     */
    fun preload(assetPaths: List<String>) {
        assetPaths.forEach { path ->
            try {
                loadAsset(path)
            } catch (e: Exception) {
                AppLogger.w(TAG, "预加载失败: $path", e)
            }
        }
    }
}

/**
 * 安全配置加载器
 * 替代原有的 ShellModeManager 中的配置加载逻辑
 */
class SecureConfigLoader(private val context: Context) {
    
    companion object {
        private const val TAG = "SecureConfigLoader"
    }
    
    private val secureLoader = SecureAssetLoader.getInstance(context)
    @Volatile
    private var cachedConfig: ShellConfig? = null
    @Volatile
    private var configLoaded = false
    
    /**
     * 检查是否为 Shell 模式
     */
    fun isShellMode(): Boolean {
        return loadConfig() != null
    }
    
    /**
     * 获取配置
     */
    fun getConfig(): ShellConfig? {
        return loadConfig()
    }
    
    /**
     * 加载配置
     */
    private fun loadConfig(): ShellConfig? {
        if (configLoaded) return cachedConfig
        
        synchronized(this) {
            if (configLoaded) return cachedConfig
            
            configLoaded = true
            cachedConfig = try {
            secureLoader.loadConfig()?.also { config ->
                // Verify配置有效性
                val normalizedAppType = config.appType.trim().uppercase()
                val isValid = when {
                    normalizedAppType == "HTML" || normalizedAppType == "FRONTEND" -> {
                        // Verify entryFile 必须有文件名部分（不能只是 .html 或空字符串）
                        val entryFile = config.htmlConfig.entryFile
                        entryFile.isNotBlank() && entryFile.substringBeforeLast(".").isNotBlank()
                    }
                    normalizedAppType == "IMAGE" || normalizedAppType == "VIDEO" -> true
                    normalizedAppType == "GALLERY" -> true
                    normalizedAppType == "WORDPRESS" -> true
                    normalizedAppType == "NODEJS_APP" -> true
                    normalizedAppType == "PHP_APP" -> true
                    normalizedAppType == "PYTHON_APP" -> true
                    normalizedAppType == "GO_APP" -> true
                    else -> !config.targetUrl.isNullOrBlank()
                }
                
                if (!isValid) {
                    AppLogger.w(TAG, "配置无效")
                    return null
                }
                
                AppLogger.d(TAG, "配置加载成功: appType=${config.appType}")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "加载配置失败", e)
            null
        }
        
            return cachedConfig
        }
    }
    
    /**
     * 重新加载配置
     */
    fun reload() {
        synchronized(this) {
            configLoaded = false
            cachedConfig = null
            secureLoader.clearCache()
        }
    }
}
