---
title: 安全功能文档
description: 加密系统、加固系统、隐私隔离、护盾系统、激活码验证、安全初始化流程
---

# 安全功能文档

## 一、概述

WebToApp 提供多层安全保护，覆盖导出 APK 的资源加密、代码加固、运行时防护、网络安全和隐私隔离。

**核心文件**：
- `core/crypto/` — 加密系统（15 个文件）
- `core/hardening/` — 加固系统（7 个文件）
- `core/isolation/` — 隐私隔离（4 个文件）
- `core/engine/shields/` — 护盾系统
- `core/activation/` — 激活码系统

---

## 二、加密系统 (`core/crypto/`)

### 2.1 架构

```
ApkEncryptionConfig (UI 配置)
  → toEncryptionConfig() → EncryptionConfig (核心配置)
  → AssetEncryptor.encrypt() → 加密资源写入 APK
  → Shell 运行时: AssetDecryptor.decrypt() → 明文资源
```

### 2.2 加密算法

| 组件 | 算法 | 说明 |
|------|------|------|
| 密钥派生 | PBKDF2-HMAC-SHA256 | 迭代次数由 EncryptionLevel 决定 |
| 增强密钥派生 | HKDF-SHA256 | 从主密钥派生子密钥 |
| 对称加密 | AES-256-GCM | 认证加密，防篡改 |
| 完整性校验 | HMAC-SHA256 | 资源完整性验证 |

### 2.3 密钥来源

```
密钥派生输入:
1. 签名证书 SHA-256 哈希 (自动获取，无需用户输入)
2. 包名 (com.example.app)
3. 自定义密码 (可选，用户设置)

派生过程:
PBKDF2(输入材料, salt, iterations) → 256-bit 主密钥
HKDF(主密钥, info="webtoapp-encryption") → AES-256 密钥
```

**关键安全属性**：
- 不同包名 → 不同密钥（即使密码相同）
- 不同签名 → 不同密钥
- 无自定义密码时，密钥与 APK 签名绑定，重签名后无法解密

### 2.4 加密范围

| 资源类型 | 默认 | 说明 |
|---------|------|------|
| `app_config.json` | 加密 | 包含所有配置信息 |
| HTML 文件 | 加密 | 本地 HTML 项目内容 |
| 媒体文件 | 不加密 | 图片/视频（体积大） |
| 闪屏资源 | 不加密 | 闪屏图片/视频 |
| BGM 资源 | 不加密 | 背景音乐 |

### 2.5 EncryptionLevel

| 级别 | PBKDF2 迭代 | 加密耗时 | 安全等级 |
|------|------------|---------|---------|
| FAST | 5,000 | ~50ms | 低（仅防普通用户） |
| STANDARD | 10,000 | ~100ms | 中（防大多数攻击） |
| HIGH | 50,000 | ~500ms | 高（防专业攻击） |
| PARANOID | 100,000 | ~1s | 极高（防暴力破解） |

### 2.6 EnhancedCrypto

增强加密模块提供：

```
EnhancedCrypto
├── HKDF 密钥派生
├── 流式加密/解密 (大文件)
├── SecurityKeyContainer (安全密钥容器)
│   ├── 密钥存储在内存中
│   ├── 访问需要认证
│   └── 超时自动清除
└── IntegrityVerifier (完整性验证)
    ├── HMAC-SHA256 签名
    └── CRC32 快速校验
```

### 2.7 KeyManager

```kotlin
class KeyManager(private val context: Context) {
    fun generateKeyForPackage(
        packageName: String,
        signatureHash: String,
        encryptionLevel: EncryptionLevel,
        customPassword: String?
    ): SecretKey
    
    fun getKeyForPackage(packageName: String): SecretKey?
    fun clearKey(packageName: String)
    fun clearAllKeys()
}
```

密钥生命周期：
- 构建时生成 → 写入 APK（加密后）
- 运行时从签名派生 → 解密资源
- 应用退出 → 密钥从内存清除

---

## 三、加固系统 (`core/hardening/`)

### 3.1 加固级别

| 级别 | 保护范围 | 适用场景 |
|------|---------|---------|
| BASIC | DEX 加密 + 反调试 + 反 Frida | 一般应用 |
| STANDARD | + SO 加密 + 反 Xposed + 字符串加密 | 商业应用 |
| ADVANCED | + DEX VMP + SO 混淆 + 代码混淆 | 高价值应用 |
| FORTRESS | 全部保护 + 蜜罐 + 自毁 | 极高安全需求 |

### 3.2 DEX 保护

| 技术 | 说明 |
|------|------|
| `dexEncryption` | DEX 文件加密，运行时解密加载 |
| `dexSplitting` | DEX 拆分为多个小块，增加逆向难度 |
| `dexVmp` | 虚拟机保护，将字节码转为自定义指令集 |
| `dexControlFlowFlattening` | 控制流平坦化，混淆代码逻辑 |

### 3.3 Native SO 保护

| 技术 | 说明 |
|------|------|
| `soEncryption` | SO 文件加密，运行时解密 |
| `soElfObfuscation` | ELF 结构混淆 |
| `soSymbolStrip` | 符号表剥离 |
| `soAntiDump` | 防内存 dump |

### 3.4 反逆向工程

| 技术 | 说明 |
|------|------|
| `antiDebugMultiLayer` | 多层反调试（ptrace 检测 + 时间检测 + 状态检测） |
| `antiFridaAdvanced` | 高级反 Frida（端口扫描 + 库检测 + 线程名检测） |
| `antiXposedDeep` | 深度反 Xposed（方法 hook 检测 + 栈帧分析） |
| `antiMagiskDetect` | Magisk 检测（多种绕过检测） |
| `antiMemoryDump` | 防内存 dump（内存保护 + 检测） |
| `antiScreenCapture` | 防截屏（FLAG_SECURE） |

### 3.5 环境检测

| 检测 | 说明 |
|------|------|
| `detectEmulatorAdvanced` | 高级模拟器检测（多维度特征） |
| `detectVirtualApp` | 虚拟空间/双开检测 |
| `detectUSBDebugging` | USB 调试检测 |
| `detectVPN` | VPN 连接检测 |
| `detectDeveloperOptions` | 开发者选项检测 |

### 3.6 RASP（运行时自我保护）

| 技术 | 说明 |
|------|------|
| `dexCrcVerify` | DEX CRC 校验（防篡改） |
| `memoryIntegrity` | 内存完整性校验 |
| `jniCallValidation` | JNI 调用验证 |
| `timingCheck` | 定时检查（防暂停调试） |
| `stackTraceFilter` | 调用栈过滤（隐藏安全检查） |

### 3.7 威胁响应

| 策略 | 行为 |
|------|------|
| LOG_ONLY | 仅记录日志，不干预 |
| SILENT_EXIT | 静默退出应用 |
| CRASH_RANDOM | 随机位置崩溃（增加分析难度） |
| DATA_WIPE | 擦除应用数据 |
| FAKE_DATA | 返回伪造数据（误导攻击者） |

额外选项：
- `responseDelay` — 延迟响应（秒），增加分析难度
- `enableHoneypot` — 蜜罐功能，记录攻击者行为
- `enableSelfDestruct` — 自毁机制，极端情况下销毁关键数据

---

## 四、隐私隔离 (`core/isolation/`)

### 4.1 IsolationConfig

```kotlin
data class IsolationConfig(
    val enabled: Boolean = false,
    val fingerprintProtection: Boolean = true,   // 指纹保护
    val userAgentSpoofing: Boolean = true,       // UA 伪装
    val canvasProtection: Boolean = true,         // Canvas 指纹保护
    val webglProtection: Boolean = false,         // WebGL 指纹保护
    val audioProtection: Boolean = false,         // Audio 指纹保护
    val fontProtection: Boolean = false,          // 字体指纹保护
    val screenProtection: Boolean = false,        // 屏幕信息保护
    val timezoneProtection: Boolean = false,      // 时区保护
    val languageProtection: Boolean = false,       // 语言保护
    val hardwareProtection: Boolean = false       // 硬件信息保护
)
```

### 4.2 保护机制

| 保护项 | 实现方式 |
|--------|---------|
| **指纹保护** | 注入 JS 覆盖 `navigator` 属性 |
| **UA 伪装** | 替换 WebView 的 User-Agent |
| **Canvas 保护** | 注入噪声到 Canvas API |
| **WebGL 保护** | 修改 WebGL 渲染器/供应商信息 |
| **Audio 保护** | 添加噪声到 AudioContext |
| **字体保护** | 限制可用字体列表 |
| **屏幕保护** | 伪造屏幕分辨率/DPR |
| **时区保护** | 覆盖 `Date.getTimezoneOffset()` |
| **语言保护** | 覆盖 `navigator.language` |
| **硬件保护** | 伪造 CPU 核心数/内存 |

---

## 五、护盾系统 (`core/engine/shields/`)

### 5.1 BrowserShields（总控制器）

```
BrowserShields
├── HttpsUpgrader      — HTTP→HTTPS 自动升级
├── TrackerBlocker     — 追踪器拦截
├── AdBlocker          — 广告拦截
├── ReaderMode         — 阅读模式
├── ShieldsStats       — 统计
└── CookieConsent       — Cookie 横幅拦截
```

### 5.2 HttpsUpgrader

```
请求流程:
1. shouldOverrideUrlLoading() → HttpsUpgrader.tryUpgrade()
2. 检查是否跳过 (localhost / IP地址 / .local 域名)
3. http:// → https:// 升级
4. 如果 SSL 错误 → onSslError()
5. 标记失败域名 → tryHttpFallback() 回退到 HTTP
6. 白名单管理 (用户可手动添加)
```

**安全注意**：
- IP 地址（所有，包括公网和私网）自动跳过 HTTPS 升级
- localhost / 127.0.0.1 自动跳过
- SSL 错误时自动回退，但记录失败域名防止无限循环

### 5.3 TrackerBlocker / AdBlocker

```
拦截流程:
1. shouldInterceptRequest() → TrackerBlocker.shouldBlock(url)
2. 域名规则匹配 (EasyList 格式)
3. 路径模式匹配
4. 返回空响应 (拦截) 或放行
```

两者独立运行，规则集不同：
- TrackerBlocker — 隐私追踪器规则
- AdBlocker — 广告规则

---

## 六、激活码系统 (`core/activation/`)

### 6.1 ActivationCode

```kotlin
data class ActivationCode(
    val code: String,                              // 激活码字符串
    val type: ActivationType = PERMANENT,          // 类型
    val maxUsage: Int = -1,                        // 最大使用次数 (-1=无限)
    val expiresAt: Long? = null,                   // 过期时间
    val boundDeviceId: String? = null,             // 绑定设备 ID
    val createdAt: Long = System.currentTimeMillis()
)

enum class ActivationType {
    PERMANENT,         // 永久有效
    TIME_LIMITED,      // 时间限制
    USAGE_LIMITED,     // 使用次数限制
    DEVICE_BOUND       // 设备绑定
}
```

### 6.2 验证流程

```
ShellActivity 启动
  → initActivation()
  → 如果 activationEnabled:
    1. 检查 isActivated (本地缓存)
    2. 如果未激活 → 显示激活对话框
    3. 用户输入激活码
    4. ActivationManager.validate(code)
      → 类型检查 (PERMANENT / TIME_LIMITED / USAGE_LIMITED / DEVICE_BOUND)
      → 过期检查
      → 使用次数检查
      → 设备绑定检查 (ANDROID_ID)
    5. 验证通过 → 标记 isActivated = true
    6. 验证失败 → 显示错误信息
```

### 6.3 云端激活

如果启用了 `cloudSdkConfig.activationCodeEnabled`，激活码通过云端验证：

```
本地激活码列表为空
  → 请求 Cloud SDK API 验证
  → 返回验证结果
```

---

## 七、SecurityInitializer

主应用启动时的安全初始化：

```kotlin
object SecurityInitializer {
    fun initialize(context: Context) {
        // 1. 检查运行环境安全
        checkEnvironment(context)
        // 2. 初始化安全存储
        initSecureStorage(context)
        // 3. 配置网络安全
        configureNetworkSecurity(context)
        // 4. 启用证书锁定 (如果配置)
        enableCertificatePinning(context)
    }
}
```

### 7.1 安全存储

敏感数据（激活码、密钥、Token）使用 `EncryptedSharedPreferences`：

```kotlin
val securePrefs = EncryptedSharedPreferences.create(
    fileName,
    masterKeyAlias,
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

---

## 八、网络安全

### 8.1 HTTPS 升级

- 自动将 HTTP 请求升级为 HTTPS
- SSL 错误时回退到 HTTP（可配置）
- 白名单管理（localhost、IP、.local 自动跳过）

### 8.2 证书锁定

`AppHardeningConfig.certificatePinning = true` 时启用：

```
证书锁定流程:
1. 预置目标域名的证书指纹
2. 连接时验证服务器证书
3. 不匹配 → 威胁响应
```

### 8.3 代理支持

WebViewConfig 支持代理配置：

| 模式 | 说明 |
|------|------|
| NONE | 直连 |
| HTTP | HTTP 代理 |
| SOCKS | SOCKS 代理 |
| PAC | PAC 自动配置 |

---

## 九、安全配置最佳实践

### 9.1 一般应用

```kotlin
ApkEncryptionConfig.BASIC  // 基础加密
AppHardeningConfig.DISABLED  // 不加固
```

### 9.2 商业应用

```kotlin
ApkEncryptionConfig.FULL  // 全量加密
AppHardeningConfig.STANDARD  // 标准加固
IsolationConfig(enabled = true, fingerprintProtection = true, userAgentSpoofing = true)
```

### 9.3 高安全应用

```kotlin
ApkEncryptionConfig.MAXIMUM  // 最高加密 + 字符串混淆
AppHardeningConfig.ADVANCED  // 高级加固
IsolationConfig(enabled = true)  // 全部隐私保护
activationEnabled = true  // 激活码保护
```

### 9.4 安全注意事项

- **自定义密码**：如果用户设置了自定义密码，丢失密码将无法解密
- **签名一致性**：加密密钥依赖签名哈希，重签名后无法解密
- **PARANOID 级别**：首次解密耗时约 1 秒，可能影响启动速度
- **FORTRESS 加固**：蜜罐和自毁功能可能导致数据丢失，谨慎使用
- **allowFileAccessFromFileURLs**：仅 HTML/FRONTEND 类型可为 true，远程 URL 必须为 false
