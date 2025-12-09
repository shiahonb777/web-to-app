# WebToApp Key Server

远程激活码验证服务，使用 Go 和 SQLite 实现。

## 功能特性

- ✅ 激活码远程验证
- ✅ 批量激活码生成
- ✅ 设备激活管理
- ✅ 审计日志记录
- ✅ 签名验证（HMAC-SHA256）
- ✅ 时间戳防重放
- ✅ 设备限制和使用次数控制
- ✅ RESTful API

## 快速开始

### 前置要求

- Go 1.21+
- SQLite 3
- Docker（可选）

### 本地开发

```bash
# 克隆项目
git clone https://github.com/yingcaihuang/webtoapp-key-server.git
cd webtoapp-key-server

# 安装依赖
go mod download

# 配置环境变量
cp configs/.env.example .env
# 编辑 .env 文件，更新敏感信息

# 运行应用
go run cmd/main.go
```

应用将在 `http://localhost:8080` 启动。

### Docker 部署

```bash
# 构建镜像
docker build -f docker/Dockerfile -t webtoapp-keyserver:latest .

# 使用 Docker Compose 启动
docker-compose up -d

# 查看日志
docker-compose logs -f keyserver
```

## API 文档

### 1. 验证激活码

```bash
POST /api/v1/activation/verify

curl -X POST http://localhost:8080/api/v1/activation/verify \
  -H "Content-Type: application/json" \
  -d '{
    "code": "XXXX-XXXX-XXXX-XXXX",
    "app_id": "com.webtoapp.example",
    "device_id": "android_device_id",
    "device_info": {
      "model": "Xiaomi 12",
      "os_version": "Android 13",
      "app_version": "1.0.6"
    },
    "timestamp": 1702000000000
  }'
```

**成功响应 (200):**
```json
{
  "success": true,
  "message": "激活成功",
  "data": {
    "activation_id": 123,
    "expires_at": 1733000000000,
    "device_limit": 5,
    "devices_used": 2,
    "remaining_uses": 0,
    "created_at": 1670000000000
  },
  "signature": "HMAC_SHA256_SIGNATURE",
  "timestamp": 1702000000000
}
```

**失败响应 (401):**
```json
{
  "success": false,
  "code": "INVALID_CODE",
  "message": "激活码不存在或已过期",
  "timestamp": 1702000000000
}
```

### 2. 生成激活码

需要 API Key 认证

```bash
POST /api/v1/activation/generate

curl -X POST http://localhost:8080/api/v1/activation/generate \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{
    "app_id": "com.webtoapp.example",
    "count": 10,
    "expires_in_days": 365,
    "max_uses": 1,
    "device_limit": 5,
    "notes": "批量生成测试激活码"
  }'
```

**响应 (200):**
```json
{
  "success": true,
  "generated": 10,
  "codes": [
    {
      "code": "XXXX-XXXX-XXXX-XXXX",
      "id": 1,
      "expires_at": 1733000000000
    }
  ]
}
```

### 3. 查询激活码列表

```bash
GET /api/v1/activation/list?app_id=com.webtoapp.example&status=active&page=1&limit=20

curl -X GET 'http://localhost:8080/api/v1/activation/list?app_id=com.webtoapp.example&page=1' \
  -H "X-API-Key: your-api-key"
```

### 4. 撤销激活码

```bash
POST /api/v1/activation/:id/revoke

curl -X POST http://localhost:8080/api/v1/activation/123/revoke \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{"reason": "激活码滥用"}'
```

## 数据库

### 表结构

**activation_keys** - 激活码表
- id: 主键
- code: 激活码（唯一）
- app_id: 应用 ID
- status: 状态（active/used/expired/revoked）
- created_at: 创建时间
- expires_at: 过期时间
- used_at: 首次使用时间
- used_count: 使用次数
- max_uses: 最大使用次数
- device_limit: 设备限制
- notes: 备注

**audit_logs** - 审计日志表
- id: 主键
- action: 操作类型
- activation_id: 激活码 ID
- device_id: 设备 ID
- result: 结果（success/failed）
- error_message: 错误信息
- created_at: 创建时间

**device_records** - 设备记录表
- id: 主键
- device_id: 设备 ID（唯一 + app_id）
- app_id: 应用 ID
- activation_id: 激活码 ID
- device_name: 设备名称
- first_activated_at: 首次激活时间
- last_activated_at: 最后激活时间
- activation_count: 激活次数
- status: 状态

## 配置

编辑 `configs/config.yaml` 或设置环境变量：

```yaml
server:
  port: 8080
  env: production

database:
  type: sqlite
  path: ./data/keyserver.db

api:
  api_key: your-api-key
  signature_secret: signature-secret
  timestamp_tolerance: 300  # 秒
```

## 安全建议

1. **生产环境**
   - 使用强密钥替换默认值
   - 启用 HTTPS
   - 配置适当的 CORS 策略
   - 启用速率限制

2. **数据保护**
   - 定期备份 SQLite 数据库
   - 加密存储敏感配置
   - 启用审计日志

3. **API 安全**
   - 使用 API Key 进行管理接口认证
   - 验证请求签名
   - 检查时间戳防重放

## 开发

### 项目结构

```
.
├── cmd/
│   └── main.go
├── internal/
│   ├── api/
│   │   ├── handlers/
│   │   └── middleware/
│   ├── domain/
│   ├── repository/
│   ├── service/
│   ├── database/
│   ├── config/
│   └── utils/
├── pkg/
├── configs/
├── docker/
├── go.mod
└── README.md
```

### 运行测试

```bash
go test ./...
```

### 代码格式化

```bash
go fmt ./...
```

## 日志

应用在标准输出打印结构化日志。示例：

```
[2024-12-09 10:30:45] 127.0.0.1 POST /api/v1/activation/verify 200 125ms
```

## 监控指标

- API 响应时间
- 激活成功率
- 异常激活次数
- 数据库查询性能

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License

## 支持

- 📖 [完整设计文档](../KEY_SERVER_DESIGN.md)
- 🐛 [报告问题](https://github.com/yingcaihuang/web-to-app/issues)
- 💬 [讨论区](https://github.com/yingcaihuang/web-to-app/discussions)
