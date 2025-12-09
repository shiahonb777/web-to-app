# WebToApp Key Server - 实现指南

## 📋 目录

1. [项目初始化](#项目初始化)
2. [开发环境设置](#开发环境设置)
3. [核心功能实现](#核心功能实现)
4. [测试和调试](#测试和调试)
5. [部署上线](#部署上线)
6. [与 Android App 集成](#与-android-app-集成)

---

## 项目初始化

### 第一步：创建项目结构

```bash
# 项目已创建，位置在 webtoapp-key-server/
cd /Users/betty/web-to-app/webtoapp-key-server

# 查看项目结构
tree -L 3 -I 'node_modules'
```

### 第二步：初始化 Go Module

```bash
# 项目已包含 go.mod，下载依赖
go mod download
go mod tidy
```

### 第三步：创建数据目录

```bash
mkdir -p data
chmod 755 data
```

---

## 开发环境设置

### 配置环境变量

```bash
# 复制环境变量示例
cp configs/.env.example .env

# 编辑 .env（更新敏感信息）
cat > .env << 'EOF'
SERVER_PORT=8080
ENV=development
DB_TYPE=sqlite
DB_PATH=./data/keyserver.db
JWT_SECRET=dev-secret-key-change-in-production
API_KEY=dev-api-key-change-in-production
SIGNATURE_SECRET=dev-signature-secret-change-in-production
TIMESTAMP_TOLERANCE=300
EOF
```

### 本地开发运行

```bash
# 方式 1：直接运行
go run ./cmd/main.go

# 方式 2：编译后运行
make build
./bin/keyserver

# 方式 3：使用 Make
make run
```

输出示例：
```
🚀 Server running on :8080
✅ Database connected: ./data/keyserver.db
```

---

## 核心功能实现

### 当前已实现的功能

- ✅ 数据模型定义（ActivationKey, AuditLog, DeviceRecord）
- ✅ 数据库初始化和迁移
- ✅ 激活码验证逻辑
- ✅ 激活码生成
- ✅ 设备记录管理
- ✅ 审计日志记录
- ✅ 基础 API 端点

### 待完善的功能

- ⚠️ Repository 层（数据访问抽象）
- ⚠️ 完整的错误处理
- ⚠️ 详细的设备列表和查询
- ⚠️ 审计日志查询接口
- ⚠️ 单元和集成测试
- ⚠️ 速率限制具体实现
- ⚠️ 监控和告警集成

### 实现建议（优先级）

#### Priority 1: 完善核心 API（1-2 天）

1. **修复 main.go** - 添加缺失的导入和初始化
   ```go
   // 需要添加：
   - "time" 导入
   - handlers.InitHandlers() 调用
   - middleware.AuthMiddleware() 应用
   ```

2. **完善 Repository 层**
   ```go
   // internal/repository/activation.go
   - SaveActivationKey()
   - GetActivationKeyByCode()
   - ListActivationKeys()
   - UpdateActivationKeyStatus()
   - GetDeviceCountByActivationID()
   
   // internal/repository/device.go
   - SaveDeviceRecord()
   - GetDeviceByID()
   - ListDevicesByAppID()
   
   // internal/repository/audit.go
   - SaveAuditLog()
   - GetAuditLogs()
   ```

3. **完善 Handlers**
   ```go
   // ListDevices - 实现设备列表查询
   // GetDeviceInfo - 实现单个设备详情
   // GetAuditLogs - 实现日志查询
   ```

#### Priority 2: 测试和验证（1-2 天）

1. **单元测试**
   ```bash
   go test ./internal/service/... -v
   go test ./internal/api/handlers/... -v
   ```

2. **集成测试**
   ```go
   // tests/integration_test.go
   - 测试激活码验证流程
   - 测试生成激活码
   - 测试设备管理
   ```

3. **API 测试** (使用 curl 或 Postman)
   ```bash
   # 测试健康检查
   curl http://localhost:8080/health
   
   # 测试激活码验证
   curl -X POST http://localhost:8080/api/v1/activation/verify \
     -H "Content-Type: application/json" \
     -d '{...}'
   ```

#### Priority 3: 部署优化（1 天）

1. **Docker 镜像构建**
   ```bash
   make docker-build
   ```

2. **Docker Compose 部署**
   ```bash
   make docker-run
   ```

3. **环境变量配置**
   - 生产环境密钥设置
   - 数据库持久化配置
   - 日志级别调整

---

## 测试和调试

### 本地测试

#### 1. 健康检查

```bash
curl -i http://localhost:8080/health

# 预期响应：
# HTTP/1.1 200 OK
# {"status":"ok","timestamp":1702000000}
```

#### 2. 激活码验证

```bash
curl -X POST http://localhost:8080/api/v1/activation/verify \
  -H "Content-Type: application/json" \
  -d '{
    "code": "XXXX-XXXX-XXXX-XXXX",
    "app_id": "com.webtoapp.test",
    "device_id": "test_device_001",
    "device_info": {
      "model": "Test Device",
      "os_version": "Android 13",
      "app_version": "1.0.0"
    },
    "timestamp": '$(date +%s%N | cut -b1-13)'
  }'
```

#### 3. 生成激活码

```bash
curl -X POST http://localhost:8080/api/v1/activation/generate \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-api-key-change-in-production" \
  -d '{
    "app_id": "com.webtoapp.test",
    "count": 5,
    "expires_in_days": 365,
    "max_uses": 1,
    "device_limit": 5
  }'
```

### 调试技巧

#### 查看数据库内容

```bash
# 安装 sqlite3
brew install sqlite3

# 查看激活码表
sqlite3 ./data/keyserver.db "SELECT * FROM activation_keys LIMIT 10;"

# 查看审计日志
sqlite3 ./data/keyserver.db "SELECT * FROM audit_logs LIMIT 10;"

# 查看设备记录
sqlite3 ./data/keyserver.db "SELECT * FROM device_records LIMIT 10;"
```

#### 查看日志

```bash
# 实时日志
make run | tee logs.txt

# 查看特定日志
tail -f logs.txt | grep "error"
```

---

## 部署上线

### Docker 部署

#### 前置要求
- Docker 已安装
- Docker Compose 已安装

#### 部署步骤

```bash
# 1. 构建镜像
make docker-build

# 2. 启动服务
make docker-run

# 3. 查看日志
make docker-logs

# 4. 验证运行
curl http://localhost:8080/health

# 5. 停止服务
make docker-stop
```

#### 生产环境配置

编辑 `docker-compose.yml`，更新环境变量：

```yaml
environment:
  - SERVER_PORT=8080
  - ENV=production
  - JWT_SECRET=your-production-secret
  - API_KEY=your-production-api-key
  - SIGNATURE_SECRET=your-production-signature-secret
```

### Kubernetes 部署（可选）

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: webtoapp-keyserver
spec:
  replicas: 2
  selector:
    matchLabels:
      app: webtoapp-keyserver
  template:
    metadata:
      labels:
        app: webtoapp-keyserver
    spec:
      containers:
      - name: keyserver
        image: webtoapp-keyserver:latest
        ports:
        - containerPort: 8080
        env:
        - name: DB_PATH
          value: /data/keyserver.db
        volumeMounts:
        - name: data
          mountPath: /data
      volumes:
      - name: data
        persistentVolumeClaim:
          claimName: keyserver-pvc
```

---

## 与 Android App 集成

### 1. 在 Android App 中使用远程验证

#### 修改 ActivationManager

在 `app/src/main/java/com/webtoapp/core/activation/ActivationManager.kt` 中添加：

```kotlin
// 添加远程验证方法
suspend fun verifyRemoteActivationCode(
    code: String,
    deviceId: String,
    remoteServerUrl: String
): ActivationResult {
    // 调用远程服务器 API
    // 返回验证结果
}

// 修改现有验证方法支持本地 + 远程
suspend fun verifyActivationCodeWithFallback(
    appId: Long,
    code: String,
    validCodes: List<String>,
    remoteServerUrl: String? = null
): ActivationResult {
    // 先尝试本地验证
    val localResult = verifyActivationCode(appId, code, validCodes)
    if (localResult is ActivationResult.Success) {
        return localResult
    }
    
    // 如果本地验证失败，尝试远程验证
    if (remoteServerUrl != null) {
        return verifyRemoteActivationCode(code, deviceId, remoteServerUrl)
    }
    
    return localResult
}
```

#### 创建远程验证客户端

```kotlin
// new file: app/src/main/java/com/webtoapp/core/activation/RemoteActivationClient.kt

class RemoteActivationClient(
    private val serverUrl: String,
    private val okHttpClient: OkHttpClient
) {
    suspend fun verify(
        code: String,
        appId: String,
        deviceId: String
    ): ActivationResult = withContext(Dispatchers.IO) {
        try {
            val request = buildVerifyRequest(code, appId, deviceId)
            val response = okHttpClient.newCall(request).execute()
            
            return@withContext when {
                response.isSuccessful -> parseSuccessResponse(response)
                response.code == 401 -> ActivationResult.Invalid
                response.code == 429 -> ActivationResult.DeviceLimitExceeded
                else -> ActivationResult.Error
            }
        } catch (e: Exception) {
            ActivationResult.Error
        }
    }
    
    private fun buildVerifyRequest(
        code: String,
        appId: String,
        deviceId: String
    ): Request {
        val body = RequestBody.create(
            MediaType.parse("application/json"),
            """{
                "code": "$code",
                "app_id": "$appId",
                "device_id": "$deviceId",
                "timestamp": ${System.currentTimeMillis()}
            }""".toByteArray()
        )
        
        return Request.Builder()
            .url("$serverUrl/api/v1/activation/verify")
            .post(body)
            .build()
    }
}
```

### 2. 配置管理

在应用创建时添加远程服务器配置：

```kotlin
data class AppConfig(
    val name: String,
    val url: String,
    val icon: ByteArray?,
    val activationEnabled: Boolean = false,
    val activationCodes: List<String> = emptyList(),
    val remoteActivationEnabled: Boolean = false,  // 新增
    val remoteServerUrl: String? = null,           // 新增
    // ... 其他配置
)
```

### 3. UI 集成

在激活码对话框中显示验证状态：

```kotlin
@Composable
fun ActivationDialog(
    onDismiss: () -> Unit,
    onActivate: (String) -> Unit,
    isRemoteVerifying: Boolean = false
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("激活应用") },
        text = {
            Column {
                Text("请输入激活码以继续使用")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        error = null
                    },
                    enabled = !isRemoteVerifying,
                    label = { Text("激活码") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )
                
                if (isRemoteVerifying) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp)
                    )
                    Text("正在验证...", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (code.isNotBlank()) onActivate(code) },
                enabled = code.isNotBlank() && !isRemoteVerifying
            ) {
                Text("激活")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
```

### 4. 测试集成

```kotlin
// 测试激活码验证
@Test
fun testRemoteActivationVerification() = runBlocking {
    val client = RemoteActivationClient(
        serverUrl = "http://localhost:8080",
        okHttpClient = OkHttpClient()
    )
    
    // 测试成功验证
    val result = client.verify(
        code = "XXXX-XXXX-XXXX-XXXX",
        appId = "com.webtoapp.test",
        deviceId = "test_device"
    )
    
    assertTrue(result is ActivationResult.Success)
}
```

---

## 常见问题

### Q: 如何更改 Key Server 地址？

**A:** 在应用创建时填入服务器地址，或在配置中修改：

```kotlin
remoteServerUrl = "https://keyserver.yourdomain.com"
```

### Q: 离线时如何处理激活码验证？

**A:** 使用本地验证 + 缓存机制：

```kotlin
// 1. 本地验证（总是可用）
val localResult = activation.verifyActivationCode(...)

// 2. 缓存远程结果
if (localResult.success && remoteServerUrl != null) {
    cacheRemoteVerificationResult(localResult)
}

// 3. 离线时使用缓存
val cachedResult = getCachedVerificationResult()
```

### Q: 如何管理大量激活码？

**A:** 使用批量生成 API：

```bash
curl -X POST http://keyserver:8080/api/v1/activation/generate \
  -H "X-API-Key: your-key" \
  -d '{
    "app_id": "com.webtoapp.app1",
    "count": 1000,
    "expires_in_days": 365
  }'
```

### Q: 如何处理签名验证失败？

**A:** 检查 `SIGNATURE_SECRET` 是否匹配：

```kotlin
// 客户端验证
val isValid = verifySignature(response, serverSecret)
if (!isValid) {
    // 可能是中间人攻击
    logSecurityEvent("Invalid signature detected")
}
```

---

## 监控和维护

### 定期检查

- 数据库文件大小
- 审计日志数量
- 激活码使用情况
- 异常激活行为

### 数据备份

```bash
# 每天备份 SQLite 数据库
0 2 * * * cp /app/data/keyserver.db /backup/keyserver.db.$(date +\%Y\%m\%d)
```

### 性能监控

- API 响应时间（目标 < 100ms）
- 数据库查询时间（目标 < 50ms）
- 内存使用（目标 < 100MB）
- CPU 使用（目标 < 20%）

---

**下一步：** 完成上述实现步骤，测试 API，部署上线！🚀
