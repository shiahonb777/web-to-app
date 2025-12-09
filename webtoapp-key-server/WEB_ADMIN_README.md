# Web 管理后台开发文档

## 概述

本文档介绍 WebToApp Key Server 的 Web 管理后台系统，包括 API Key 认证、数据统计和分析功能。

## 功能特性

### 1. API Key 管理
- **生成新 Key**：支持权限配置
- **列表管理**：查看所有 API Key 及其状态
- **撤销操作**：安全撤销过期或不需要的 Key
- **权限控制**：细粒度的权限管理

### 2. 统计分析
- **实时仪表板**：显示关键性能指标
- **趋势分析**：7/30/90 天的历史数据
- **应用排名**：Top 5 应用的激活统计
- **数据导出**：支持 CSV 格式导出

### 3. 审计日志
- **操作记录**：所有 API Key 操作的完整记录
- **IP 追踪**：记录操作的 IP 地址
- **日志查询**：支持按时间、操作类型筛选

## 系统架构

```
webtoapp-key-server/
├── internal/
│   ├── domain/
│   │   └── apikey.go           # 数据模型定义
│   ├── service/
│   │   ├── apikey.go           # API Key 服务
│   │   └── statistics.go       # 统计服务
│   ├── api/
│   │   ├── handlers/
│   │   │   └── admin_handlers.go   # 管理员处理器
│   │   ├── middleware/
│   │   │   └── apikey_auth.go      # API Key 认证
│   │   └── router.go           # 路由配置
│   └── database/
│       └── init.go             # 数据库初始化
├── web/
│   ├── index.html              # 主界面
│   ├── login.html              # 登录页面
│   ├── css/
│   │   └── style.css           # 样式文件
│   └── js/
│       └── app.js              # 前端逻辑
└── ...
```

## 数据模型

### 1. APIKey
```go
type APIKey struct {
    ID        uint
    Name      string      // Key 名称
    KeyHash   string      // 哈希值（用于验证）
    KeyPrefix string      // 前缀（显示用）
    Secret    string      // 密钥
    Status    string      // active/inactive/revoked
    Permission string     // 权限列表（逗号分隔）
    LastUsed  *time.Time  // 最后使用时间
    CreatedAt time.Time
    UpdatedAt time.Time
    DeletedAt gorm.DeletedAt // 软删除
}
```

### 2. Statistics
```go
type Statistics struct {
    ID                      uint
    AppID                   string
    TotalActivations        int64
    SuccessfulVerifications int64
    FailedVerifications     int64
    TotalDevices            int64
    ActiveCodes             int64
    RevokedCodes            int64
    CreatedAt               time.Time
    UpdatedAt               time.Time
}
```

### 3. DailyStats
```go
type DailyStats struct {
    ID                uint
    AppID             string
    Date              time.Time
    VerificationCount int
    SuccessCount      int
    FailureCount      int
    NewDevices        int
    CodesGenerated    int
    CodesRevoked      int
    CreatedAt         time.Time
}
```

### 4. AdminAuditLog
```go
type AdminAuditLog struct {
    ID        uint
    AdminID   uint      // API Key ID
    Action    string    // 操作类型
    Resource  string    // 资源
    Details   string    // 详情
    Status    string    // success/failure
    IPAddress string    // IP 地址
    Timestamp time.Time
}
```

## API 端点

### API Key 管理

#### 生成新 API Key
```
POST /api/admin/api-keys
Authorization: Bearer {api_key}
Content-Type: application/json

Request:
{
  "name": "Mobile App API",
  "permissions": ["read:statistics", "write:keys"]
}

Response:
{
  "id": 1,
  "name": "Mobile App API",
  "key_prefix": "abc12345...",
  "full_key": "abc12345...xyz789",
  "status": "active",
  "created_at": "2025-01-02T10:00:00Z"
}
```

#### 列出 API Keys
```
GET /api/admin/api-keys?page=1&limit=10
Authorization: Bearer {api_key}

Response:
{
  "data": [...],
  "total": 25,
  "page": 1,
  "limit": 10
}
```

#### 获取单个 API Key
```
GET /api/admin/api-keys/{id}
Authorization: Bearer {api_key}
```

#### 更新 API Key
```
PUT /api/admin/api-keys/{id}
Authorization: Bearer {api_key}
Content-Type: application/json

Request:
{
  "name": "Updated Name",
  "permissions": ["read:statistics"]
}
```

#### 撤销 API Key
```
DELETE /api/admin/api-keys/{id}
Authorization: Bearer {api_key}
```

#### API Key 统计
```
GET /api/admin/api-keys/stats
Authorization: Bearer {api_key}

Response:
{
  "total": 10,
  "active": 8,
  "revoked": 2
}
```

### 统计分析

#### 获取总体统计
```
GET /api/admin/statistics
Authorization: Bearer {api_key}
```

#### 获取仪表板数据
```
GET /api/admin/statistics/dashboard
Authorization: Bearer {api_key}
```

#### 获取应用统计
```
GET /api/admin/statistics/apps/{app_id}
Authorization: Bearer {api_key}
```

#### 获取趋势数据
```
GET /api/admin/statistics/apps/{app_id}/trends?days=7
Authorization: Bearer {api_key}
```

## 认证机制

### API Key 格式
```
{prefix}.{secret}
例如：abc12345...xyz789
```

### 请求头示例
```
Authorization: Bearer abc12345...xyz789
```

### 验证流程
1. 提取 Authorization 头中的 Bearer token
2. 分割 token 为 prefix 和 secret
3. 计算 prefix 的 SHA256 哈希
4. 在数据库中查询匹配的 hash
5. 验证 secret 是否匹配
6. 更新 last_used 时间戳

## 前端使用指南

### 访问管理后台
1. 打开浏览器访问 `http://localhost:8080/login.html`
2. 输入有效的 API Key 进行登录
3. 登录成功后自动跳转到仪表板

### 生成新 API Key
1. 在侧边栏点击 "API Key 管理"
2. 点击 "+ 生成新 Key" 按钮
3. 填写 Key 名称和选择权限
4. 点击生成
5. 复制并保存完整的 Key（仅显示一次）

### 查看统计分析
1. 在侧边栏点击 "统计分析"
2. 可选择应用和时间周期进行筛选
3. 查看图表和详细数据
4. 支持导出 CSV 格式

### 审计日志
1. 在侧边栏点击 "审计日志"
2. 可按关键词或操作类型搜索
3. 查看所有 API 操作的完整记录

## 部署说明

### 1. 数据库初始化
```go
import "github.com/yingcaihuang/webtoapp-key-server/internal/database"

// 在应用启动时调用
if err := database.InitDatabase(db); err != nil {
    log.Fatalf("初始化数据库失败: %v", err)
}

if err := database.CreateIndexes(db); err != nil {
    log.Fatalf("创建索引失败: %v", err)
}
```

### 2. 路由注册
```go
import "github.com/yingcaihuang/webtoapp-key-server/internal/api"

// 在主函数中
router := api.SetupRouter(cfg, db)
```

### 3. 静态文件服务
```go
// 在 Gin router 中添加
router.Static("/web", "./web")
```

### 4. 环境变量配置
```bash
# 可选：设置管理员 API Key
export ADMIN_API_KEY="your_secret_key"

# 数据库路径
export DB_PATH="./data/webtoapp.db"
```

## 安全建议

### 1. API Key 管理
- ✅ 定期轮换 API Key
- ✅ 为不同的应用使用不同的 Key
- ✅ 限制 Key 的权限范围
- ✅ 及时撤销不需要的 Key

### 2. 访问控制
- ✅ 使用 HTTPS 传输 API Key
- ✅ 启用 IP 白名单（可选）
- ✅ 定期审计访问日志
- ✅ 监控异常访问行为

### 3. 数据保护
- ✅ 定期备份数据库
- ✅ 使用加密存储敏感信息
- ✅ 实现访问速率限制
- ✅ 记录所有管理操作

## 故障排除

### 问题 1：登录失败
**现象**：输入正确的 API Key 但仍登录失败

**解决方案**：
1. 确认 API Key 是否有效（未被撤销）
2. 检查服务器是否正在运行
3. 查看浏览器控制台（F12）获取详细错误信息
4. 检查网络连接

### 问题 2：数据不显示
**现象**：仪表板或统计页面显示为空

**解决方案**：
1. 确认数据库已初始化：检查 `api_keys` 表是否存在
2. 检查权限是否包含 `read:statistics`
3. 查看浏览器开发工具 Network 标签，检查 API 请求
4. 查看服务器日志了解更多信息

### 问题 3：图表不显示
**现象**：统计分析页面的图表为空

**解决方案**：
1. 确认已加载 Chart.js 库
2. 检查浏览器控制台是否有 JavaScript 错误
3. 确认数据库中有统计数据
4. 尝试刷新页面

## 开发扩展

### 添加新的统计指标
在 `internal/domain/apikey.go` 中修改 `Statistics` 结构体：
```go
type Statistics struct {
    // ... 现有字段 ...
    YourNewMetric int64  // 新增指标
}
```

### 自定义权限系统
修改 `internal/api/middleware/apikey_auth.go`：
```go
func CheckPermission(permission string) gin.HandlerFunc {
    // 自定义权限检查逻辑
}
```

### 扩展前端功能
编辑 `web/js/app.js` 中的相应函数，或在 `web/index.html` 中添加新的选项卡。

## 许可证
MIT License

## 支持
如有问题，请通过以下方式联系：
- 提交 GitHub Issue
- 发送邮件至项目维护者
- 查看项目文档

---
**最后更新**：2025年1月
**版本**：1.0.0
