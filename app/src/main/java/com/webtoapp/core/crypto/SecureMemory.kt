package com.webtoapp.core.crypto

import android.util.Log
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 安全内存管理
 * 
 * 提供敏感数据的安全存储和自动清除功能
 */
object SecureMemory {
    
    private const val TAG = "SecureMemory"
    
    // 追踪需要清除的内存
    private val trackedMemory = ConcurrentHashMap<Int, ByteArray>()
    private val referenceQueue = ReferenceQueue<Any>()
    private val cleanerThread: Thread
    private val isRunning = AtomicBoolean(true)
    
    private val secureRandom = SecureRandom()
    
    init {
        // Start清理线程
        cleanerThread = Thread({
            while (isRunning.get()) {
                try {
                    val ref = referenceQueue.remove(1000)
                    if (ref != null) {
                        // Object被回收，清除关联的内存
                        val id = System.identityHashCode(ref)
                        trackedMemory.remove(id)?.let { data ->
                            secureWipe(data)
                            Log.d(TAG, "Auto-wiped tracked memory: ${data.size} bytes")
                        }
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }, "SecureMemory-Cleaner")
        cleanerThread.isDaemon = true
        cleanerThread.start()
    }
    
    /**
     * 安全清除字节数组
     * 使用多次覆盖确保数据被清除
     */
    fun secureWipe(data: ByteArray) {
        // 第一遍：全零
        data.fill(0)
        
        // 第二遍：随机数据
        secureRandom.nextBytes(data)
        
        // 第三遍：全零
        data.fill(0)
        
        // 第四遍：0xFF
        data.fill(0xFF.toByte())
        
        // 最终：全零
        data.fill(0)
    }
    
    /**
     * 安全清除字符数组
     */
    fun secureWipe(data: CharArray) {
        data.fill('\u0000')
        for (i in data.indices) {
            data[i] = (secureRandom.nextInt(65536)).toChar()
        }
        data.fill('\u0000')
    }
    
    /**
     * 创建安全字节数组
     * 当关联对象被回收时自动清除
     */
    fun allocateSecure(size: Int, owner: Any? = null): ByteArray {
        val data = ByteArray(size)
        
        if (owner != null) {
            val id = System.identityHashCode(owner)
            trackedMemory[id] = data
            PhantomReference(owner, referenceQueue)
        }
        
        return data
    }
    
    /**
     * 手动释放安全内存
     */
    fun release(data: ByteArray) {
        secureWipe(data)
        
        // 从追踪列表中移除
        val iterator = trackedMemory.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value === data) {
                iterator.remove()
                break
            }
        }
    }
    
    /**
     * 清除所有追踪的内存
     */
    fun releaseAll() {
        trackedMemory.values.forEach { secureWipe(it) }
        trackedMemory.clear()
    }
    
    /**
     * 关闭清理线程
     */
    fun shutdown() {
        isRunning.set(false)
        cleanerThread.interrupt()
        releaseAll()
    }
}

/**
 * 安全字节数组包装器
 * 提供自动清除功能
 */
class SecureBytes private constructor(
    @PublishedApi internal val data: ByteArray
) : AutoCloseable {
    
    @PublishedApi
    @Volatile
    internal var isCleared = false
    
    companion object {
        /**
         * 创建安全字节数组
         */
        fun wrap(data: ByteArray): SecureBytes {
            return SecureBytes(data.copyOf())
        }
        
        /**
         * 从字符串创建
         */
        fun fromString(str: String): SecureBytes {
            return SecureBytes(str.toByteArray(Charsets.UTF_8))
        }
        
        /**
         * 创建指定大小的安全字节数组
         */
        fun allocate(size: Int): SecureBytes {
            return SecureBytes(ByteArray(size))
        }
        
        /**
         * 创建随机填充的安全字节数组
         */
        fun random(size: Int): SecureBytes {
            val data = ByteArray(size)
            SecureRandom().nextBytes(data)
            return SecureBytes(data)
        }
    }
    
    /**
     * 获取数据（只读）
     */
    fun get(): ByteArray {
        check(!isCleared) { "SecureBytes has been cleared" }
        return data.copyOf()
    }
    
    /**
     * 在回调中使用数据
     * 避免数据泄露到外部
     */
    inline fun <R> use(block: (ByteArray) -> R): R {
        check(!isCleared) { "SecureBytes has been cleared" }
        return block(data)
    }
    
    /**
     * 获取大小
     */
    val size: Int
        get() {
            check(!isCleared) { "SecureBytes has been cleared" }
            return data.size
        }
    
    /**
     * 清除数据
     */
    override fun close() {
        if (!isCleared) {
            SecureMemory.secureWipe(data)
            isCleared = true
        }
    }
    
    protected fun finalize() {
        close()
    }
}

/**
 * 安全字符串包装器
 */
class SecureString private constructor(
    @PublishedApi internal val chars: CharArray
) : AutoCloseable {
    
    @PublishedApi
    @Volatile
    internal var isCleared = false
    
    companion object {
        fun wrap(str: String): SecureString {
            return SecureString(str.toCharArray())
        }
        
        fun wrap(chars: CharArray): SecureString {
            return SecureString(chars.copyOf())
        }
    }
    
    /**
     * 在回调中使用字符数组
     */
    inline fun <R> use(block: (CharArray) -> R): R {
        check(!isCleared) { "SecureString has been cleared" }
        return block(chars)
    }
    
    /**
     * 转换为字节数组
     */
    fun toBytes(): SecureBytes {
        check(!isCleared) { "SecureString has been cleared" }
        return SecureBytes.fromString(String(chars))
    }
    
    val length: Int
        get() {
            check(!isCleared) { "SecureString has been cleared" }
            return chars.size
        }
    
    override fun close() {
        if (!isCleared) {
            SecureMemory.secureWipe(chars)
            isCleared = true
        }
    }
    
    protected fun finalize() {
        close()
    }
}

/**
 * 敏感数据容器
 * 提供加密存储和访问控制
 */
class SensitiveData<T>(
    private val data: T,
    private val encryptedStorage: ByteArray? = null
) : AutoCloseable {
    
    @Volatile
    private var accessCount = 0
    private val maxAccess = 100 // Max访问次数
    
    @Volatile
    private var isCleared = false
    
    /**
     * 访问数据
     */
    fun access(): T {
        check(!isCleared) { "Data has been cleared" }
        check(accessCount < maxAccess) { "Maximum access count exceeded" }
        accessCount++
        return data
    }
    
    /**
     * 获取剩余访问次数
     */
    fun remainingAccess(): Int = maxAccess - accessCount
    
    override fun close() {
        if (!isCleared) {
            // 清除加密存储
            encryptedStorage?.let { SecureMemory.secureWipe(it) }
            
            // 尝试清除数据
            when (data) {
                is ByteArray -> SecureMemory.secureWipe(data)
                is CharArray -> SecureMemory.secureWipe(data)
                is SecureBytes -> data.close()
                is SecureString -> data.close()
            }
            
            isCleared = true
        }
    }
}
