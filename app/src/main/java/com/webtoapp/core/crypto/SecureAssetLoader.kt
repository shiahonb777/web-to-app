package com.webtoapp.core.crypto

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.webtoapp.core.shell.ShellConfig
import java.io.InputStream

/**
 * 安全资源加载器
 * 统一处理加密和非加密资源的加载
 */
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
    private val gson = Gson()
    
    // Cache已加载的资源
    private val cache = mutableMapOf<String, ByteArray>()
    
    // Yes否启用完整性检查
    var integrityCheckEnabled = true
    
    /**
     * 加载配置文件
     */
    fun loadConfig(): ShellConfig? {
        return try {
            // Execute完整性检查
            if (integrityCheckEnabled && !integrityChecker.quickCheck()) {
                Log.w(TAG, "完整性检查失败，拒绝加载配置")
                return null
            }
            
            val configJson = loadAssetAsString(CryptoConstants.CONFIG_FILE)
            gson.fromJson(configJson, ShellConfig::class.java)
            
        } catch (e: Exception) {
            Log.e(TAG, "加载配置失败", e)
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
                Log.w(TAG, "预加载失败: $path", e)
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
    private var cachedConfig: ShellConfig? = null
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
        
        configLoaded = true
        cachedConfig = try {
            secureLoader.loadConfig()?.also { config ->
                // Verify配置有效性
                val isValid = when {
                    config.appType == "HTML" -> {
                        // Verify entryFile 必须有文件名部分（不能只是 .html 或空字符串）
                        val entryFile = config.htmlConfig.entryFile
                        entryFile.isNotBlank() && entryFile.substringBeforeLast(".").isNotBlank()
                    }
                    config.appType == "IMAGE" || config.appType == "VIDEO" -> true
                    else -> !config.targetUrl.isNullOrBlank()
                }
                
                if (!isValid) {
                    Log.w(TAG, "配置无效")
                    return null
                }
                
                Log.d(TAG, "配置加载成功: appType=${config.appType}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载配置失败", e)
            null
        }
        
        return cachedConfig
    }
    
    /**
     * 重新加载配置
     */
    fun reload() {
        configLoaded = false
        cachedConfig = null
        secureLoader.clearCache()
    }
}
