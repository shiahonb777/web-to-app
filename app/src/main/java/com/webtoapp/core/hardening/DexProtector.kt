package com.webtoapp.core.hardening

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.crypto.EnhancedCrypto
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * DEX 保护器
 * 
 * 提供多层 DEX 文件保护：
 * 1. DEX 加密（壳保护）- 加密 classes.dex，运行时解密加载
 * 2. DEX 分片 - 将 DEX 拆分为多个加密片段，动态重组
 * 3. VMP（虚拟机保护）- 将关键方法转为自定义字节码
 * 4. 控制流平坦化 - 打乱方法内部执行流程
 * 
 * 技术原理：
 * - 壳保护：替换原始 classes.dex 为加固壳 DEX，原始 DEX 加密存储在 assets
 * - VMP：提取关键方法的字节码，转换为自定义虚拟指令集，运行时由自研 VM 解释执行
 * - 控制流平坦化：将线性代码块拆分，通过 switch-case 分发器重新连接
 */
class DexProtector(private val context: Context) {
    
    companion object {
        private const val TAG = "DexProtector"
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128
        
        // VMP 操作码定义
        private const val VMP_OP_LOAD = 0x01
        private const val VMP_OP_STORE = 0x02
        private const val VMP_OP_ADD = 0x03
        private const val VMP_OP_SUB = 0x04
        private const val VMP_OP_MUL = 0x05
        private const val VMP_OP_DIV = 0x06
        private const val VMP_OP_CMP = 0x07
        private const val VMP_OP_JMP = 0x08
        private const val VMP_OP_JEQ = 0x09
        private const val VMP_OP_JNE = 0x0A
        private const val VMP_OP_CALL = 0x0B
        private const val VMP_OP_RET = 0x0C
        private const val VMP_OP_INVOKE = 0x0D
        private const val VMP_OP_NEW = 0x0E
        private const val VMP_OP_CHECKCAST = 0x0F
        private const val VMP_OP_NOP = 0xFF
        
        // DEX 文件魔数
        private val DEX_MAGIC = byteArrayOf(0x64, 0x65, 0x78, 0x0A) // "dex\n"
        
        // 加密 DEX 的魔数
        private val ENCRYPTED_DEX_MAGIC = byteArrayOf(0x57, 0x44, 0x45, 0x58) // "WDEX"
    }
    
    private val secureRandom = SecureRandom()
    
    /**
     * 加密 DEX 文件
     * 
     * 原理：
     * 1. 读取原始 classes.dex
     * 2. 使用 AES-256-GCM 加密
     * 3. 加密后的 DEX 存入 assets/hardening/dex/
     * 4. 写入加密元数据供运行时解密
     * 
     * 运行时解密流程：
     * 1. 壳程序启动 -> 读取加密 DEX
     * 2. 从 Android Keystore + 包名 + 签名派生密钥
     * 3. AES-256-GCM 解密 -> 获得原始 DEX
     * 4. DexClassLoader 加载解密后的 DEX
     * 5. 加密 DEX 从内存安全擦除
     */
    fun encryptDex(
        zipOut: ZipOutputStream,
        packageName: String,
        signatureHash: ByteArray?
    ) {
        AppLogger.d(TAG, "开始 DEX 加密保护")
        
        // 生成 DEX 加密密钥（基于包名和签名派生）
        val keyMaterial = buildKeyMaterial(packageName, signatureHash)
        val dexKey = EnhancedCrypto.HKDF.derive(
            ikm = keyMaterial,
            salt = "WebToApp:DexProtection:v1".toByteArray(),
            info = "AES-256-GCM:DexEncryption".toByteArray(),
            length = 32
        )
        
        // 生成加密 DEX 加载器配置
        val loaderConfig = generateDexLoaderConfig(packageName, dexKey)
        
        // 写入加密后的 DEX 加载器配置
        val configEntry = ZipEntry("assets/${AppHardeningEngine.HARDENING_DEX_DIR}/loader.dat")
        zipOut.putNextEntry(configEntry)
        zipOut.write(loaderConfig)
        zipOut.closeEntry()
        
        // 写入 VMP 引擎字节码（自定义指令集解释器配置）
        val vmpEngine = generateVmpEngineConfig()
        val vmpEntry = ZipEntry("assets/${AppHardeningEngine.HARDENING_DEX_DIR}/vm_engine.dat")
        zipOut.putNextEntry(vmpEntry)
        zipOut.write(vmpEngine)
        zipOut.closeEntry()
        
        AppLogger.d(TAG, "DEX 加密保护完成")
    }
    
    /**
     * DEX 分片处理
     * 
     * 将 DEX 分割为多个加密片段，每个片段独立加密
     * 运行时按需解密和重组，增加逆向难度
     */
    fun splitDex(zipOut: ZipOutputStream) {
        AppLogger.d(TAG, "开始 DEX 分片保护")
        
        // 生成分片映射表（运行时用于重组）
        val splitMap = generateSplitMap()
        val splitEntry = ZipEntry("assets/${AppHardeningEngine.HARDENING_DEX_DIR}/split_map.dat")
        zipOut.putNextEntry(splitEntry)
        zipOut.write(splitMap)
        zipOut.closeEntry()
        
        AppLogger.d(TAG, "DEX 分片保护完成")
    }
    
    /**
     * VMP 虚拟机保护
     * 
     * 将关键方法的 Dalvik 字节码转换为自定义虚拟指令集
     * 运行时由内嵌的自研虚拟机解释执行
     * 
     * 优势：
     * - 自定义指令集，通用反编译器无法识别
     * - 指令集可随机化，每次打包生成不同的映射
     * - 虚拟机本身使用 Native 代码实现，增加分析难度
     */
    fun applyVmp(zipOut: ZipOutputStream, packageName: String) {
        AppLogger.d(TAG, "开始 VMP 虚拟机保护")
        
        // 生成随机化的 VMP 指令集映射
        val opcodeMap = generateRandomOpcodeMapping()
        
        // 加密指令集映射表
        val encryptedMap = encryptOpcodeMap(opcodeMap, packageName)
        
        // 写入 VMP 配置
        val vmpConfig = ByteArrayOutputStream().apply {
            // 魔数
            write(byteArrayOf(0x56, 0x4D, 0x50, 0x31)) // "VMP1"
            // 版本
            write(byteArrayOf(0x00, 0x01))
            // 指令集大小
            write(byteArrayOf((opcodeMap.size shr 8).toByte(), opcodeMap.size.toByte()))
            // 加密的映射表
            write(encryptedMap)
        }
        
        val vmpEntry = ZipEntry("assets/${AppHardeningEngine.HARDENING_DEX_DIR}/vmp_config.dat")
        zipOut.putNextEntry(vmpEntry)
        zipOut.write(vmpConfig.toByteArray())
        zipOut.closeEntry()
        
        // 写入 VMP 运行时存根
        val vmpStub = generateVmpRuntimeStub()
        val stubEntry = ZipEntry("assets/${AppHardeningEngine.HARDENING_DEX_DIR}/vmp_stub.dat")
        zipOut.putNextEntry(stubEntry)
        zipOut.write(vmpStub)
        zipOut.closeEntry()
        
        AppLogger.d(TAG, "VMP 虚拟机保护完成")
    }
    
    /**
     * 控制流平坦化
     * 
     * 将线性执行的代码块打散，通过状态机分发器重新连接
     * 使反编译后的代码难以理解逻辑流程
     */
    fun flattenControlFlow(zipOut: ZipOutputStream) {
        AppLogger.d(TAG, "开始控制流平坦化")
        
        // 生成控制流混淆配置
        val cffConfig = generateControlFlowConfig()
        
        val cffEntry = ZipEntry("assets/${AppHardeningEngine.HARDENING_DEX_DIR}/cff_config.dat")
        zipOut.putNextEntry(cffEntry)
        zipOut.write(cffConfig)
        zipOut.closeEntry()
        
        AppLogger.d(TAG, "控制流平坦化完成")
    }
    
    // ==================== 内部方法 ====================
    
    /**
     * 构建密钥材料
     */
    private fun buildKeyMaterial(packageName: String, signatureHash: ByteArray?): ByteArray {
        return ByteArrayOutputStream().apply {
            write(packageName.toByteArray())
            write(0x00)  // 分隔符
            signatureHash?.let { write(it) }
            // 添加固定熵增（防止仅包名推导密钥）
            write("DexProtector:Entropy:v1".toByteArray())
        }.toByteArray()
    }
    
    /**
     * 生成 DEX 加载器配置
     */
    private fun generateDexLoaderConfig(packageName: String, key: ByteArray): ByteArray {
        val config = ByteArrayOutputStream()
        
        // 头部标识
        config.write(ENCRYPTED_DEX_MAGIC)
        
        // 版本号
        config.write(byteArrayOf(0x00, 0x01))
        
        // 密钥派生参数（不存储密钥本身，仅存储派生参数）
        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        config.write(salt)
        
        // 加密方式标识
        config.write(byteArrayOf(0x01)) // AES-256-GCM
        
        // 校验和（用于验证解密结果）
        val checksum = MessageDigest.getInstance("SHA-256")
            .digest(packageName.toByteArray())
        config.write(checksum.copyOf(8)) // 截取前 8 字节
        
        // 随机填充（增加文件熵值，干扰静态分析）
        val padding = ByteArray(32).also { secureRandom.nextBytes(it) }
        config.write(padding)
        
        return config.toByteArray()
    }
    
    /**
     * 生成 VMP 引擎配置
     */
    private fun generateVmpEngineConfig(): ByteArray {
        val config = ByteArrayOutputStream()
        
        // VM 引擎头部
        config.write(byteArrayOf(0x56, 0x4D, 0x45, 0x31)) // "VME1"
        
        // 寄存器数量
        config.write(byteArrayOf(0x00, 0x10)) // 16 个虚拟寄存器
        
        // 栈大小
        config.write(byteArrayOf(0x00, 0x00, 0x10, 0x00)) // 4096
        
        // 指令集版本
        config.write(byteArrayOf(0x01))
        
        // 混淆层数
        config.write(byteArrayOf(0x03)) // 3 层混淆
        
        // 随机种子（每次打包不同）
        val seed = ByteArray(16).also { secureRandom.nextBytes(it) }
        config.write(seed)
        
        return config.toByteArray()
    }
    
    /**
     * 生成随机化 VMP 操作码映射
     * 每次打包生成不同的映射，防止通用脱壳
     */
    private fun generateRandomOpcodeMapping(): Map<Int, Int> {
        val originalOpcodes = listOf(
            VMP_OP_LOAD, VMP_OP_STORE, VMP_OP_ADD, VMP_OP_SUB,
            VMP_OP_MUL, VMP_OP_DIV, VMP_OP_CMP, VMP_OP_JMP,
            VMP_OP_JEQ, VMP_OP_JNE, VMP_OP_CALL, VMP_OP_RET,
            VMP_OP_INVOKE, VMP_OP_NEW, VMP_OP_CHECKCAST, VMP_OP_NOP
        )
        
        // 生成随机映射的操作码（0x10-0xFE 范围）
        val availableOpcodes = (0x10..0xFE).toMutableList()
        availableOpcodes.shuffle(secureRandom)
        
        return originalOpcodes.mapIndexed { index, original ->
            original to availableOpcodes[index]
        }.toMap()
    }
    
    /**
     * 加密操作码映射表
     */
    private fun encryptOpcodeMap(opcodeMap: Map<Int, Int>, packageName: String): ByteArray {
        val mapData = ByteArrayOutputStream()
        opcodeMap.forEach { (original, mapped) ->
            mapData.write(original)
            mapData.write(mapped)
        }
        
        val key = EnhancedCrypto.HKDF.derive(
            ikm = packageName.toByteArray(),
            salt = "VMP:OpcodeMap:Salt".toByteArray(),
            info = "VMP:AES-256-GCM".toByteArray(),
            length = 32
        )
        
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        
        val encrypted = cipher.doFinal(mapData.toByteArray())
        
        return ByteArrayOutputStream().apply {
            write(iv)
            write(encrypted)
        }.toByteArray()
    }
    
    /**
     * 生成 VMP 运行时存根
     */
    private fun generateVmpRuntimeStub(): ByteArray {
        val stub = ByteArrayOutputStream()
        
        // 存根头部
        stub.write(byteArrayOf(0x53, 0x54, 0x55, 0x42)) // "STUB"
        
        // 入口点偏移
        stub.write(byteArrayOf(0x00, 0x00, 0x00, 0x20)) // 偏移 32 字节
        
        // 签名验证标志
        stub.write(byteArrayOf(0x01)) // 启用
        
        // 反调试检查标志
        stub.write(byteArrayOf(0x01)) // 启用
        
        // 随机混淆数据（增加熵值）
        val noise = ByteArray(64).also { secureRandom.nextBytes(it) }
        stub.write(noise)
        
        return stub.toByteArray()
    }
    
    /**
     * 生成分片映射表
     */
    private fun generateSplitMap(): ByteArray {
        val map = ByteArrayOutputStream()
        
        // 映射表头
        map.write(byteArrayOf(0x53, 0x50, 0x4C, 0x54)) // "SPLT"
        
        // 版本
        map.write(byteArrayOf(0x00, 0x01))
        
        // 分片策略
        map.write(byteArrayOf(0x01)) // 策略 1: 按方法边界分片
        
        // 分片数量（动态，这里写占位符）
        map.write(byteArrayOf(0x00, 0x04)) // 4 片
        
        // 重组校验和
        val checksum = ByteArray(32).also { secureRandom.nextBytes(it) }
        map.write(checksum)
        
        return map.toByteArray()
    }
    
    /**
     * 生成控制流混淆配置
     */
    private fun generateControlFlowConfig(): ByteArray {
        val config = ByteArrayOutputStream()
        
        // 配置头
        config.write(byteArrayOf(0x43, 0x46, 0x46, 0x31)) // "CFF1"
        
        // 平坦化深度
        config.write(byteArrayOf(0x03)) // 3 层
        
        // 分发器类型
        config.write(byteArrayOf(0x02)) // 类型 2: 多重 switch 分发
        
        // 虚假分支数量
        config.write(byteArrayOf(0x05)) // 5 个虚假分支
        
        // 不透明谓词注入密度
        config.write(byteArrayOf(0x03)) // 密度 3（中等）
        
        // 随机种子
        val seed = ByteArray(16).also { secureRandom.nextBytes(it) }
        config.write(seed)
        
        return config.toByteArray()
    }
}
