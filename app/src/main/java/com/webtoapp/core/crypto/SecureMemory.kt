package com.webtoapp.core.crypto

import com.webtoapp.core.logging.AppLogger
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.Collections






object SecureMemory {

    private const val TAG = "SecureMemory"


    private val trackedMemory = ConcurrentHashMap<Int, ByteArray>()
    private val referenceQueue = ReferenceQueue<Any>()

    private val phantomRefs: MutableSet<PhantomReference<Any>> = Collections.synchronizedSet(mutableSetOf())
    private val cleanerThread: Thread
    private val isRunning = AtomicBoolean(true)

    private val secureRandom = SecureRandom()

    init {

        cleanerThread = Thread({
            while (isRunning.get()) {
                try {
                    val ref = referenceQueue.remove(1000)
                    if (ref != null) {

                        phantomRefs.remove(ref)
                        val id = System.identityHashCode(ref)
                        trackedMemory.remove(id)?.let { data ->
                            secureWipe(data)
                            AppLogger.d(TAG, "Auto-wiped tracked memory: ${data.size} bytes")
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





    fun secureWipe(data: ByteArray) {

        data.fill(0)


        secureRandom.nextBytes(data)


        data.fill(0)


        data.fill(0xFF.toByte())


        data.fill(0)
    }




    fun secureWipe(data: CharArray) {
        data.fill('\u0000')
        for (i in data.indices) {
            data[i] = (secureRandom.nextInt(65536)).toChar()
        }
        data.fill('\u0000')
    }





    fun allocateSecure(size: Int, owner: Any? = null): ByteArray {
        val data = ByteArray(size)

        if (owner != null) {
            val id = System.identityHashCode(owner)
            trackedMemory[id] = data
            phantomRefs.add(PhantomReference(owner, referenceQueue))
        }

        return data
    }




    fun release(data: ByteArray) {
        secureWipe(data)


        val iterator = trackedMemory.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value === data) {
                iterator.remove()
                break
            }
        }
    }




    fun releaseAll() {
        trackedMemory.values.forEach { secureWipe(it) }
        trackedMemory.clear()
    }




    fun shutdown() {
        isRunning.set(false)
        cleanerThread.interrupt()
        releaseAll()
    }
}





class SecureBytes private constructor(
    @PublishedApi internal val data: ByteArray
) : AutoCloseable {

    @PublishedApi
    @Volatile
    internal var isCleared = false

    companion object {



        fun wrap(data: ByteArray): SecureBytes {
            return SecureBytes(data.copyOf())
        }




        fun fromString(str: String): SecureBytes {
            return SecureBytes(str.toByteArray(Charsets.UTF_8))
        }




        fun allocate(size: Int): SecureBytes {
            return SecureBytes(ByteArray(size))
        }




        fun random(size: Int): SecureBytes {
            val data = ByteArray(size)
            SecureRandom().nextBytes(data)
            return SecureBytes(data)
        }
    }




    fun get(): ByteArray {
        check(!isCleared) { "SecureBytes has been cleared" }
        return data.copyOf()
    }





    inline fun <R> use(block: (ByteArray) -> R): R {
        check(!isCleared) { "SecureBytes has been cleared" }
        return block(data)
    }




    val size: Int
        get() {
            check(!isCleared) { "SecureBytes has been cleared" }
            return data.size
        }




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




    inline fun <R> use(block: (CharArray) -> R): R {
        check(!isCleared) { "SecureString has been cleared" }
        return block(chars)
    }




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





class SensitiveData<T>(
    private val data: T,
    private val encryptedStorage: ByteArray? = null
) : AutoCloseable {

    private val accessCount = AtomicInteger(0)
    private val maxAccess = 100

    @Volatile
    private var isCleared = false




    fun access(): T {
        check(!isCleared) { "Data has been cleared" }
        val count = accessCount.incrementAndGet()
        check(count <= maxAccess) { "Maximum access count exceeded" }
        return data
    }




    fun remainingAccess(): Int = maxAccess - accessCount.get()

    override fun close() {
        if (!isCleared) {

            encryptedStorage?.let { SecureMemory.secureWipe(it) }


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
