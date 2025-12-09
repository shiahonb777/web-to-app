# WebToApp Key Server - 设计文档

## 项目概述

WebToApp Key Server 是一个基于 Go 语言开发的远程激活码验证服务，使用 SQLite 存储激活密钥，为 WebToApp 应用提供动态激活码管理和远程验证能力。

## 核心功能

### 1. 激活码管理
- ✅ 激活码的增删改查
- ✅ 批量生成激活码
- ✅ 激活码的有效期设置
- ✅ 激活码的使用次数限制
- ✅ 激活码的设备绑定

### 2. 远程验证
- ✅ 激活码有效性验证
- ✅ 设备指纹验证
- ✅ 版本检查
- ✅ 时间戳防重放

### 3. 审计日志
- ✅ 激活码使用记录
- ✅ 设备激活信息
- ✅ 验证失败日志
- ✅ 异常操作记录

### 4. 管理后台
- ✅ 激活码的 CRUD 操作
- ✅ 统计报表
- ✅ 设备管理
- ✅ 日志查询

## 系统架构

```
┌─────────────────────────────────────────────┐
│         WebToApp Android App                │
│       (激活码验证客户端)                     │
└────────────────┬────────────────────────────┘
                 │
          ┌──────▼──────┐
          │ HTTP/HTTPS  │
          └──────┬──────┘
                 │
┌────────────────▼──────────────────────────────┐
│         WebToApp Key Server (Go)              │
│  ┌──────────────────────────────────────────┐ │
│  │     API 层 (Gin Framework)               │ │
│  │  ├── /api/v1/activation/verify          │ │
│  │  ├── /api/v1/activation/generate        │ │
│  │  ├── /api/v1/activation/list            │ │
│  │  ├── /api/v1/activation/:id/delete      │ │
│  │  ├── /api/v1/audit/logs                 │ │
│  │  └── /api/v1/devices/list               │ │
│  └──────────────────────────────────────────┘ │
│  ┌──────────────────────────────────────────┐ │
│  │     业务逻辑层 (Service)                 │ │
│  │  ├── ActivationService                  │ │
│  │  ├── VerificationService                │ │
│  │  ├── AuditService                       │ │
│  │  └── DeviceService                      │ │
│  └──────────────────────────────────────────┘ │
│  ┌──────────────────────────────────────────┐ │
│  │     数据访问层 (Repository)               │ │
│  │  ├── ActivationRepository               │ │
│  │  ├── AuditRepository                    │ │
│  │  └── DeviceRepository                   │ │
│  └──────────────────────────────────────────┘ │
│  ┌──────────────────────────────────────────┐ │
│  │     数据库层 (SQLite)                    │ │
│  │  ├── activation_keys                    │ │
│  │  ├── audit_logs                         │ │
│  │  └── device_records                     │ │
│  └──────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

## 数据模型

### 1. activation_keys (激活码表)

```sql
CREATE TABLE activation_keys (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT UNIQUE NOT NULL,              -- 激活码
    app_id TEXT NOT NULL,                   -- 应用 ID (com.webtoapp.xxx)
    status TEXT DEFAULT 'active',           -- active/used/expired/revoked
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME,                    -- 过期时间 (可选)
    used_at DATETIME,                       -- 首次使用时间
    used_count INTEGER DEFAULT 0,           -- 使用次数
    max_uses INTEGER DEFAULT 1,             -- 最大使用次数
    device_limit INTEGER,                   -- 设备限制数 (可选)
    notes TEXT,                             -- 备注
    created_by TEXT,                        -- 创建者
    UNIQUE(code),
    INDEX idx_app_id (app_id),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at)
);
```

### 2. audit_logs (审计日志表)

```sql
CREATE TABLE audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    action TEXT NOT NULL,                   -- verify/generate/revoke/update
    activation_id INTEGER,
    device_id TEXT,                         -- 设备 ID
    device_info TEXT,                       -- 设备信息 JSON
    result TEXT,                            -- success/failed
    error_message TEXT,
    ip_address TEXT,
    app_version TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (activation_id) REFERENCES activation_keys(id),
    INDEX idx_action (action),
    INDEX idx_device_id (device_id),
    INDEX idx_created_at (created_at)
);
```

### 3. device_records (设备记录表)

```sql
CREATE TABLE device_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT UNIQUE NOT NULL,
    app_id TEXT NOT NULL,
    activation_id INTEGER,
    device_name TEXT,
    model TEXT,
    os_version TEXT,
    app_version TEXT,
    first_activated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_activated_at DATETIME,
    activation_count INTEGER DEFAULT 1,
    status TEXT DEFAULT 'active',           -- active/blocked/suspended
    FOREIGN KEY (activation_id) REFERENCES activation_keys(id),
    UNIQUE(device_id, app_id),
    INDEX idx_app_id (app_id),
    INDEX idx_status (status)
);
```

## API 设计

### 1. 验证激活码

```
POST /api/v1/activation/verify

请求体：
{
  "code": "XXXX-XXXX-XXXX-XXXX",
  "app_id": "com.webtoapp.example",
  "device_id": "android_device_fingerprint",
  "device_info": {
    "model": "Xiaomi 12",
    "os_version": "Android 13",
    "app_version": "1.0.6"
  },
  "timestamp": 1702000000000
}

响应 (200 OK)：
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

响应 (400 Bad Request)：
{
  "success": false,
  "code": "INVALID_CODE",
  "message": "激活码不存在或已过期",
  "timestamp": 1702000000000
}

响应 (429 Too Many Requests)：
{
  "success": false,
  "code": "DEVICE_LIMIT_EXCEEDED",
  "message": "该激活码已达到设备激活上限",
  "timestamp": 1702000000000
}
```

### 2. 生成激活码

```
POST /api/v1/activation/generate

请求体：
{
  "app_id": "com.webtoapp.example",
  "count": 10,
  "expires_in_days": 365,
  "max_uses": 1,
  "device_limit": 5,
  "notes": "批量生成测试激活码"
}

响应 (200 OK)：
{
  "success": true,
  "data": {
    "generated": 10,
    "codes": [
      {
        "code": "XXXX-XXXX-XXXX-XXXX",
        "id": 1,
        "expires_at": 1733000000000
      },
      ...
    ]
  }
}
```

### 3. 获取激活码列表

```
GET /api/v1/activation/list?app_id=com.webtoapp.example&status=active&page=1&limit=20

响应 (200 OK)：
{
  "success": true,
  "data": {
    "total": 100,
    "page": 1,
    "limit": 20,
    "items": [
      {
        "id": 1,
        "code": "XXXX-XXXX-XXXX-XXXX",
        "app_id": "com.webtoapp.example",
        "status": "active",
        "created_at": 1670000000000,
        "expires_at": 1733000000000,
        "used_count": 0,
        "max_uses": 1,
        "device_limit": 5
      },
      ...
    ]
  }
}
```

### 4. 撤销激活码

```
POST /api/v1/activation/:id/revoke

请求体：
{
  "reason": "激活码滥用"
}

响应 (200 OK)：
{
  "success": true,
  "message": "激活码已撤销"
}
```

### 5. 查询审计日志

```
GET /api/v1/audit/logs?app_id=com.webtoapp.example&action=verify&page=1&limit=50

响应 (200 OK)：
{
  "success": true,
  "data": {
    "total": 5000,
    "items": [
      {
        "id": 1,
        "action": "verify",
        "activation_id": 123,
        "device_id": "device_xxx",
        "result": "success",
        "app_version": "1.0.6",
        "created_at": 1702000000000
      },
      ...
    ]
  }
}
```

## 安全机制

### 1. 签名验证
- 使用 HMAC-SHA256 对响应进行签名
- 客户端可验证响应的真实性和完整性
- 防止中间人攻击

### 2. 时间戳防重放
- 请求和响应都包含时间戳
- 服务端检查时间戳偏差（±5 分钟）
- 防止重放攻击

### 3. 请求认证
- API Key 认证（用于管理接口）
- 基于 Bearer Token（OAuth2）
- HTTPS 加密传输

### 4. 设备指纹
- 验证设备 ID 是否与首次激活的设备匹配
- 支持设备变更审批流程
- 记录异常激活行为

### 5. 速率限制
- 按 IP 地址限制请求频率
- 按设备 ID 限制激活频率
- 异常激活自动告警

## 技术栈

- **框架**: Gin Web Framework
- **数据库**: SQLite + GORM ORM
- **认证**: JWT + API Key
- **日志**: Zap (结构化日志)
- **监控**: Prometheus + Grafana
- **部署**: Docker + Docker Compose

## 项目结构

```
webtoapp-key-server/
├── cmd/
│   └── main.go                      # 应用入口
├── internal/
│   ├── api/
│   │   ├── handlers/               # API 处理器
│   │   │   ├── activation.go
│   │   │   ├── audit.go
│   │   │   └── device.go
│   │   └── middleware/             # 中间件
│   │       ├── auth.go
│   │       ├── logging.go
│   │       └── ratelimit.go
│   ├── domain/
│   │   ├── models.go               # 数据模型
│   │   └── errors.go               # 错误定义
│   ├── repository/
│   │   ├── activation.go
│   │   ├── audit.go
│   │   └── device.go
│   ├── service/
│   │   ├── activation.go
│   │   ├── verification.go
│   │   ├── audit.go
│   │   └── device.go
│   ├── database/
│   │   ├── db.go                   # 数据库初始化
│   │   └── migrations.go           # 数据库迁移
│   ├── config/
│   │   └── config.go               # 配置管理
│   └── utils/
│       ├── crypto.go               # 加密工具
│       ├── logger.go               # 日志工具
│       └── validator.go            # 验证工具
├── pkg/
│   └── keyserver/                  # 公共库
│       ├── client.go               # 客户端 SDK
│       └── types.go                # 类型定义
├── migrations/
│   └── sqlite/                     # SQLite 迁移脚本
├── configs/
│   ├── config.yaml                 # 配置文件
│   └── .env.example                # 环境变量示例
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
├── tests/
│   ├── api_test.go
│   ├── service_test.go
│   └── fixtures/
├── scripts/
│   ├── generate_keys.sh            # 生成示例激活码
│   └── setup.sh                    # 初始化脚本
├── go.mod
├── go.sum
├── Makefile
└── README.md
```

## 开发计划

### Phase 1: 基础框架 (1-2 天)
- [ ] 项目初始化 + 依赖配置
- [ ] 数据库设计和迁移
- [ ] 基础模型和错误处理
- [ ] 日志和配置管理

### Phase 2: 核心功能 (3-4 天)
- [ ] 激活码验证逻辑
- [ ] 激活码管理接口
- [ ] 设备记录管理
- [ ] 审计日志记录

### Phase 3: 安全机制 (2-3 天)
- [ ] HMAC-SHA256 签名
- [ ] 时间戳验证
- [ ] API Key 认证
- [ ] 速率限制

### Phase 4: 测试和优化 (2-3 天)
- [ ] 单元测试
- [ ] 集成测试
- [ ] 性能优化
- [ ] 文档完善

## 配置示例

```yaml
# config.yaml
server:
  port: 8080
  env: development
  
database:
  type: sqlite
  path: ./data/keyserver.db
  
jwt:
  secret: your-secret-key
  expire_hours: 24
  
api:
  api_key: your-api-key
  signature_secret: signature-secret-key
  
cors:
  allowed_origins:
    - http://localhost:3000
    - https://yourdomain.com
```

## 安全检查清单

- [ ] HTTPS 强制
- [ ] CORS 配置
- [ ] SQL 注入防护
- [ ] XSS 防护
- [ ] CSRF 防护
- [ ] 速率限制
- [ ] 请求签名验证
- [ ] 敏感数据加密
- [ ] 日志脱敏
- [ ] 异常监控告警

## 监控指标

- API 响应时间
- 激活成功率
- 异常激活次数
- 设备数量
- 数据库查询性能
- 内存和 CPU 使用率

## 部署方案

### Docker 部署
```bash
docker-compose up -d
```

### Kubernetes 部署
- StatefulSet (有状态应用)
- PersistentVolume (SQLite 持久化)
- ConfigMap (配置管理)
- Service (服务暴露)

---

**下一步**: 开始编码实现，先从数据库设计和模型定义开始。
