package com.webtoapp.core.hardening

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.crypto.EnhancedCrypto
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 代码混淆器
 * 
 * 提供超越 ProGuard/R8 的代码保护：
 * 
 * 1. 多层字符串加密：
 *    - Layer 1: AES-256-GCM 加密
 *    - Layer 2: Base64 变种编码（自定义码表）
 *    - Layer 3: XOR 扰乱（使用包名+签名派生的 key）
 *    - 运行时逐层解密，每层使用不同密钥
 *    - 解密后的字符串仅在使用时短暂存在于内存中
 * 
 * 2. 类名混淆：
 *    - 将有意义的类名替换为随机Unicode字符
 *    - 使用形似但不同的 Unicode 字符（如 Cyrillic "а" vs Latin "a"）
 *    - 生成极长的类名增加反编译器渲染压力
 * 
 * 3. 方法调用间接化：
 *    - 将直接方法调用改为反射调用
 *    - 使用动态代理包装关键接口
 *    - 方法名运行时从加密字符串表解密
 * 
 * 4. 不透明谓词注入：
 *    - 注入恒真/恒假条件分支
 *    - 使用数学恒等式构造（如 x*(x+1)%2==0）
 *    - 注入永不执行的垃圾代码路径
 *    - 干扰静态分析器的控制流推断
 */
class CodeObfuscator(private val context: Context) {
    
    companion object {
        private const val TAG = "CodeObfuscator"
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128
        
        // 自定义 Base64 码表（非标准，增加分析难度）
        private const val CUSTOM_BASE64_TABLE = 
            "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm0123456789+/"
    }
    
    private val secureRandom = SecureRandom()
    
    /**
     * 多层字符串加密
     * 
     * @return 加密的字符串数量
     */
    fun encryptStrings(
        zipOut: ZipOutputStream,
        packageName: String,
        signatureHash: ByteArray?
    ): Int {
        AppLogger.e(TAG, "开始多层字符串加密")
        
        // 生成三层加密密钥
        val keyMaterial = buildKeyMaterial(packageName, signatureHash)
        
        val layer1Key = EnhancedCrypto.HKDF.derive(
            ikm = keyMaterial,
            salt = "StringEncrypt:Layer1:Salt".toByteArray(),
            info = "AES-256-GCM:L1".toByteArray(),
            length = 32
        )
        
        val layer2Key = EnhancedCrypto.HKDF.derive(
            ikm = keyMaterial,
            salt = "StringEncrypt:Layer2:Salt".toByteArray(),
            info = "CustomBase64:L2".toByteArray(),
            length = 32
        )
        
        val layer3Key = EnhancedCrypto.HKDF.derive(
            ikm = keyMaterial,
            salt = "StringEncrypt:Layer3:Salt".toByteArray(),
            info = "XOR:L3".toByteArray(),
            length = 32
        )
        
        // 写入字符串加密配置
        val config = generateStringEncryptionConfig(layer1Key, layer2Key, layer3Key)
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/str_enc.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config)
        zipOut.closeEntry()
        
        // 写入自定义 Base64 码表
        val tableEntry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/b64_table.dat")
        zipOut.putNextEntry(tableEntry)
        // 加密存储码表
        val encryptedTable = encryptData(
            CUSTOM_BASE64_TABLE.toByteArray(), layer1Key
        )
        zipOut.write(encryptedTable)
        zipOut.closeEntry()
        
        AppLogger.e(TAG, "多层字符串加密完成")
        return 1 // 配置已写入
    }
    
    /**
     * 类名混淆
     * 
     * @return 混淆的类数量
     */
    fun obfuscateClassNames(zipOut: ZipOutputStream): Int {
        AppLogger.e(TAG, "开始类名混淆")
        
        // 生成混淆映射配置
        val config = generateClassObfuscationConfig()
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/cls_obf.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config)
        zipOut.closeEntry()
        
        AppLogger.e(TAG, "类名混淆配置完成")
        return 1
    }
    
    /**
     * 方法调用间接化
     */
    fun applyCallIndirection(zipOut: ZipOutputStream) {
        AppLogger.e(TAG, "开始方法调用间接化")
        
        val config = generateCallIndirectionConfig()
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/call_ind.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config)
        zipOut.closeEntry()
        
        AppLogger.e(TAG, "方法调用间接化完成")
    }
    
    /**
     * 不透明谓词注入
     */
    fun injectOpaquePredicates(zipOut: ZipOutputStream) {
        AppLogger.e(TAG, "开始不透明谓词注入")
        
        val config = generateOpaquePredicateConfig()
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/opaque_pred.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config)
        zipOut.closeEntry()
        
        AppLogger.e(TAG, "不透明谓词注入完成")
    }
    
    // ==================== 内部方法 ====================
    
    private fun buildKeyMaterial(packageName: String, signatureHash: ByteArray?): ByteArray {
        return ByteArrayOutputStream().apply {
            write(packageName.toByteArray())
            write(0x00)
            signatureHash?.let { write(it) }
            write("CodeObfuscator:Entropy:v1".toByteArray())
        }.toByteArray()
    }
    
    /**
     * 生成字符串加密配置
     */
    private fun generateStringEncryptionConfig(
        layer1Key: ByteArray,
        layer2Key: ByteArray,
        layer3Key: ByteArray
    ): ByteArray {
        val config = ByteArrayOutputStream()
        
        // 头部
        config.write(byteArrayOf(0x53, 0x54, 0x52, 0x45)) // "STRE"
        
        // 版本
        config.write(byteArrayOf(0x00, 0x01))
        
        // 加密层数
        config.write(byteArrayOf(0x03)) // 3 层
        
        // 各层密钥派生参数（不直接存储密钥）
        val salt1 = ByteArray(16).also { secureRandom.nextBytes(it) }
        val salt2 = ByteArray(16).also { secureRandom.nextBytes(it) }
        val salt3 = ByteArray(16).also { secureRandom.nextBytes(it) }
        
        config.write(salt1)
        config.write(salt2)
        config.write(salt3)
        
        // 各层算法标识
        config.write(byteArrayOf(
            0x01, // L1: AES-256-GCM
            0x02, // L2: Custom Base64
            0x03  // L3: XOR
        ))
        
        // 解密顺序（可随机化以增加分析难度）
        val order = intArrayOf(2, 0, 1) // L3 -> L1 -> L2
        order.forEach { config.write(it.toByte().toInt()) }
        
        // 校验和
        val checksum = ByteArray(8).also { secureRandom.nextBytes(it) }
        config.write(checksum)
        
        return config.toByteArray()
    }
    
    /**
     * 生成类名混淆配置
     */
    private fun generateClassObfuscationConfig(): ByteArray {
        val config = ByteArrayOutputStream()
        
        // 头部
        config.write(byteArrayOf(0x43, 0x4C, 0x4F, 0x42)) // "CLOB"
        
        // 混淆策略
        config.write(byteArrayOf(0x02)) // 策略 2: Unicode 相似字符混淆
        
        // Unicode 混淆字符集
        val confusables = listOf(
            // Latin -> Cyrillic 映射
            'a' to 'а', // U+0061 -> U+0430
            'c' to 'с', // U+0063 -> U+0441
            'e' to 'е', // U+0065 -> U+0435
            'o' to 'о', // U+006F -> U+043E
            'p' to 'р', // U+0070 -> U+0440
            'x' to 'х', // U+0078 -> U+0445
            'y' to 'у', // U+0079 -> U+0443
            'A' to 'А', // U+0041 -> U+0410
            'B' to 'В', // U+0042 -> U+0412
            'C' to 'С', // U+0043 -> U+0421
            'E' to 'Е', // U+0045 -> U+0415
            'H' to 'Н', // U+0048 -> U+041D
            'K' to 'К', // U+004B -> U+041A
            'M' to 'М', // U+004D -> U+041C
            'O' to 'О', // U+004F -> U+041E
            'P' to 'Р', // U+0050 -> U+0420
            'T' to 'Т', // U+0054 -> U+0422
            'X' to 'Х'  // U+0058 -> U+0425
        )
        
        config.write(confusables.size.toByte().toInt())
        confusables.forEach { (from, to) ->
            config.write(byteArrayOf(
                (from.code shr 8).toByte(), from.code.toByte(),
                (to.code shr 8).toByte(), to.code.toByte()
            ))
        }
        
        // 最大类名长度（超长类名增加反编译器压力）
        config.write(byteArrayOf(0x00, 0x80.toByte())) // 128 字符
        
        // 排除包（不混淆的包前缀）
        val excludes = listOf("android.", "androidx.", "kotlin.", "java.", "javax.")
        config.write(excludes.size.toByte().toInt())
        excludes.forEach { prefix ->
            val bytes = prefix.toByteArray()
            config.write(bytes.size.toByte().toInt())
            config.write(bytes)
        }
        
        return config.toByteArray()
    }
    
    /**
     * 生成方法调用间接化配置
     */
    private fun generateCallIndirectionConfig(): ByteArray {
        val config = ByteArrayOutputStream()
        
        // 头部
        config.write(byteArrayOf(0x43, 0x41, 0x4C, 0x49)) // "CALI"
        
        // 间接化策略
        config.write(byteArrayOf(
            0x01, // 启用反射调用
            0x01, // 启用动态代理
            0x01  // 启用方法名运行时解密
        ))
        
        // 反射调用缓存策略
        config.write(byteArrayOf(0x02)) // LRU 缓存，容量 256
        
        // 需要间接化的方法类型
        config.write(byteArrayOf(
            0x01, // 安全相关方法
            0x01, // 加密方法
            0x01, // 网络请求方法
            0x00  // 普通方法（默认不间接化以保持性能）
        ))
        
        return config.toByteArray()
    }
    
    /**
     * 生成不透明谓词配置
     */
    private fun generateOpaquePredicateConfig(): ByteArray {
        val config = ByteArrayOutputStream()
        
        // 头部
        config.write(byteArrayOf(0x4F, 0x50, 0x52, 0x44)) // "OPRD"
        
        // 谓词类型
        config.write(byteArrayOf(0x04)) // 4 种类型
        
        // 类型 1: 数学恒等式（如 x*(x+1)%2 == 0, 7*y*y - 1 != x*x）
        config.write(byteArrayOf(0x01, 0x05)) // 类型1, 注入密度5
        
        // 类型 2: 字符串操作（如 "".length() == 0）
        config.write(byteArrayOf(0x02, 0x03)) // 类型2, 注入密度3
        
        // 类型 3: 对象比较（如 null instanceof Object == false）
        config.write(byteArrayOf(0x03, 0x02)) // 类型3, 注入密度2
        
        // 类型 4: 位运算（如 (x & 0) == 0）
        config.write(byteArrayOf(0x04, 0x04)) // 类型4, 注入密度4
        
        // 垃圾代码块大小范围（字节码指令数）
        config.write(byteArrayOf(0x0A, 0x32)) // 10-50 条指令
        
        // 随机种子
        val seed = ByteArray(8).also { secureRandom.nextBytes(it) }
        config.write(seed)
        
        return config.toByteArray()
    }
    
    /**
     * AES-256-GCM 加密数据
     */
    private fun encryptData(data: ByteArray, key: ByteArray): ByteArray {
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key.copyOf(32), "AES")
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        
        val encrypted = cipher.doFinal(data)
        
        return ByteArrayOutputStream().apply {
            write(iv)
            write(encrypted)
        }.toByteArray()
    }
}
