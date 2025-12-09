# Web 管理后台 - 部署指南

## 目录
1. [开发环境部署](#开发环境部署)
2. [生产环境部署](#生产环境部署)
3. [Docker 部署](#docker-部署)
4. [性能优化](#性能优化)
5. [监控和维护](#监控和维护)

---

## 开发环境部署

### 前置要求
- Go 1.20+ (推荐 1.24+)
- SQLite 3
- 现代浏览器（Chrome, Firefox, Edge, Safari）

### 安装步骤

#### 1. 克隆项目
```bash
git clone https://github.com/yingcaihuang/webtoapp-key-server.git
cd webtoapp-key-server
```

#### 2. 安装依赖
```bash
go mod download
go mod tidy
```

#### 3. 创建数据目录
```bash
mkdir -p data
```

#### 4. 修改 main.go 启用静态文件服务
```go
package main

import (
    "github.com/gin-gonic/gin"
    "github.com/yingcaihuang/webtoapp-key-server/internal/api"
    "github.com/yingcaihuang/webtoapp-key-server/internal/config"
    "github.com/yingcaihuang/webtoapp-key-server/internal/database"
    "gorm.io/driver/sqlite"
    "gorm.io/gorm"
    "log"
)

func main() {
    // 初始化数据库
    db, err := gorm.Open(sqlite.Open("data/keyserver.db"), &gorm.Config{})
    if err != nil {
        log.Fatalf("连接数据库失败: %v", err)
    }

    // 初始化表结构
    if err := database.InitDatabase(db); err != nil {
        log.Fatalf("初始化数据库失败: %v", err)
    }

    // 加载配置
    cfg := &config.Config{
        JWTSecret: "your-jwt-secret",
    }

    // 设置路由
    router := api.SetupRouter(cfg, db)

    // 提供静态文件服务
    router.Static("/", "./web")

    // 启动服务器
    if err := router.Run(":8080"); err != nil {
        log.Fatalf("启动服务器失败: %v", err)
    }
}
```

#### 5. 运行开发服务器
```bash
go run main.go
```

#### 6. 访问管理后台
打开浏览器访问 `http://localhost:8080/login.html`

---

## 生产环境部署

### 系统要求
- **CPU**: 双核以上
- **内存**: 2GB 以上
- **存储**: 至少 10GB 可用空间
- **操作系统**: Linux (CentOS 7+, Ubuntu 18.04+), macOS, Windows Server
- **网络**: 具有公网访问能力（可选）

### 1. 编译优化的二进制文件

#### Linux/macOS
```bash
# 编译生产版本
CGO_ENABLED=1 GOOS=linux GOARCH=amd64 go build -ldflags="-s -w" -o webtoapp-server main.go

# 创建启动脚本
cat > start.sh << 'EOF'
#!/bin/bash
export GIN_MODE=release
export DB_PATH="./data/keyserver.db"
./webtoapp-server
EOF

chmod +x start.sh
```

#### Windows
```bash
# 编译 Windows 版本
go build -ldflags="-s -w" -o webtoapp-server.exe main.go
```

### 2. 配置环境变量

创建 `.env` 文件：
```bash
# 应用配置
GIN_MODE=release
APP_PORT=8080

# 数据库配置
DB_PATH=./data/keyserver.db
DB_MAX_OPEN_CONNS=25
DB_MAX_IDLE_CONNS=5
DB_CONN_MAX_LIFETIME=5m

# JWT 配置
JWT_SECRET=your-super-secret-key-change-this

# 日志配置
LOG_LEVEL=info
LOG_FILE=./logs/app.log

# HTTPS 配置（可选）
CERT_FILE=./ssl/cert.pem
KEY_FILE=./ssl/key.pem
```

### 3. 配置反向代理 (Nginx)

创建 Nginx 配置文件：
```nginx
upstream webtoapp_backend {
    server localhost:8080;
    keepalive 32;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    # SSL 配置
    ssl_certificate /etc/ssl/certs/yourdomain.crt;
    ssl_certificate_key /etc/ssl/private/yourdomain.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # 日志配置
    access_log /var/log/nginx/webtoapp_access.log;
    error_log /var/log/nginx/webtoapp_error.log;

    # 静态文件缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
        proxy_pass http://webtoapp_backend;
    }

    # API 路由
    location /api/ {
        proxy_pass http://webtoapp_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时配置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Web 静态文件
    location / {
        proxy_pass http://webtoapp_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 禁止访问敏感文件
    location ~ /\. {
        deny all;
    }
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name yourdomain.com;
    return 301 https://$server_name$request_uri;
}
```

### 4. 配置 Systemd 服务

创建 `/etc/systemd/system/webtoapp.service`：
```ini
[Unit]
Description=WebToApp Key Server
After=network.target

[Service]
Type=simple
User=nobody
WorkingDirectory=/opt/webtoapp
Environment="GIN_MODE=release"
Environment="DB_PATH=./data/keyserver.db"
ExecStart=/opt/webtoapp/webtoapp-server
Restart=on-failure
RestartSec=10s

# 日志配置
StandardOutput=journal
StandardError=journal
SyslogIdentifier=webtoapp

# 资源限制
LimitNOFILE=65535
LimitNPROC=65535

# 安全配置
PrivateTmp=true
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/webtoapp/data

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
# 刷新 systemd 配置
sudo systemctl daemon-reload

# 启用自动启动
sudo systemctl enable webtoapp

# 启动服务
sudo systemctl start webtoapp

# 查看服务状态
sudo systemctl status webtoapp

# 查看日志
sudo journalctl -u webtoapp -f
```

---

## Docker 部署

### Dockerfile

创建 `Dockerfile`：
```dockerfile
# 编译阶段
FROM golang:1.24-alpine AS builder

WORKDIR /app
COPY . .

# 安装依赖
RUN apk add --no-cache sqlite-dev gcc musl-dev

# 编译
RUN CGO_ENABLED=1 GOOS=linux GOARCH=amd64 \
    go build -ldflags="-s -w" -o webtoapp-server main.go

# 运行阶段
FROM alpine:latest

RUN apk add --no-cache sqlite-libs ca-certificates tzdata

WORKDIR /app

# 复制编译好的二进制文件
COPY --from=builder /app/webtoapp-server .

# 复制前端文件
COPY --from=builder /app/web ./web

# 创建数据目录
RUN mkdir -p data logs

# 暴露端口
EXPOSE 8080

# 环境变量
ENV GIN_MODE=release
ENV DB_PATH=./data/keyserver.db

# 启动命令
CMD ["./webtoapp-server"]
```

### 构建和运行 Docker 镜像

```bash
# 构建镜像
docker build -t webtoapp-key-server:latest .

# 运行容器
docker run -d \
  --name webtoapp \
  -p 8080:8080 \
  -v webtoapp-data:/app/data \
  -e GIN_MODE=release \
  -e DB_PATH=./data/keyserver.db \
  webtoapp-key-server:latest

# 查看日志
docker logs -f webtoapp

# 停止容器
docker stop webtoapp
```

### Docker Compose 部署

创建 `docker-compose.yml`：
```yaml
version: '3.8'

services:
  webtoapp:
    build: .
    container_name: webtoapp-key-server
    ports:
      - "8080:8080"
    volumes:
      - webtoapp-data:/app/data
      - webtoapp-logs:/app/logs
    environment:
      GIN_MODE: release
      DB_PATH: ./data/keyserver.db
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  # 可选：Nginx 反向代理
  nginx:
    image: nginx:alpine
    container_name: webtoapp-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    depends_on:
      - webtoapp
    restart: unless-stopped

volumes:
  webtoapp-data:
  webtoapp-logs:
```

启动：
```bash
docker-compose up -d
```

---

## 性能优化

### 1. 数据库优化

```go
// 在 main.go 中配置数据库连接池
sqlDB, _ := db.DB()
sqlDB.SetMaxOpenConns(25)
sqlDB.SetMaxIdleConns(5)
sqlDB.SetConnMaxLifetime(5 * time.Minute)
```

### 2. 缓存策略

```go
// 使用 Redis 缓存统计数据（可选）
import "github.com/go-redis/redis/v8"

rdb := redis.NewClient(&redis.Options{
    Addr: "localhost:6379",
})

// 缓存统计数据，TTL 1 小时
rdb.Set(ctx, "stats:"+appID, data, 1*time.Hour)
```

### 3. 使用 CDN 加速

```nginx
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
    # 将静态资源上传到 CDN
    # 例如：https://cdn.yourdomain.com/static/
    expires 30d;
    add_header Cache-Control "public, immutable";
    add_header X-Content-Type-Options "nosniff";
}
```

### 4. 开启 Gzip 压缩

在 Nginx 中配置：
```nginx
gzip on;
gzip_vary on;
gzip_proxied any;
gzip_comp_level 6;
gzip_types text/plain text/css text/xml text/javascript 
           application/json application/javascript application/xml+rss 
           application/rss+xml application/atom+xml image/svg+xml 
           text/x-component text/x-cross-domain-policy;
```

---

## 监控和维护

### 1. 日志管理

```bash
# 配置日志轮转 (/etc/logrotate.d/webtoapp)
/var/log/webtoapp/*.log {
    daily
    rotate 7
    compress
    delaycompress
    missingok
    notifempty
    create 0640 nobody nobody
    sharedscripts
}
```

### 2. 监控脚本

创建 `monitor.sh`：
```bash
#!/bin/bash

# 检查服务健康状态
while true; do
    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/health)
    
    if [ "$response" != "200" ]; then
        # 发送告警（邮件、Slack 等）
        echo "服务异常，HTTP 状态码：$response"
        # 尝试重启
        systemctl restart webtoapp
    fi
    
    sleep 300  # 每 5 分钟检查一次
done
```

### 3. 数据库备份

```bash
#!/bin/bash
# 创建每日备份脚本

BACKUP_DIR="/backup/webtoapp"
DB_FILE="./data/keyserver.db"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# 复制数据库文件
cp $DB_FILE $BACKUP_DIR/keyserver_$DATE.db

# 压缩备份
gzip $BACKUP_DIR/keyserver_$DATE.db

# 清理 7 天前的备份
find $BACKUP_DIR -name "*.gz" -mtime +7 -delete

echo "数据库备份完成：$BACKUP_DIR/keyserver_$DATE.db.gz"
```

在 crontab 中配置定时备份：
```bash
# 每天凌晨 2 点执行备份
0 2 * * * /opt/webtoapp/backup.sh >> /var/log/webtoapp_backup.log 2>&1
```

### 4. Prometheus 监控（可选）

```go
import "github.com/prometheus/client_golang/prometheus"

var (
    httpRequestsTotal = prometheus.NewCounterVec(
        prometheus.CounterOpts{
            Name: "http_requests_total",
            Help: "Total HTTP requests",
        },
        []string{"method", "endpoint", "status"},
    )
)

// 在 init 中注册
prometheus.MustRegister(httpRequestsTotal)
```

---

## 故障排除

### 问题 1：端口被占用
```bash
# 查找占用端口的进程
lsof -i :8080

# 杀死进程
kill -9 <PID>
```

### 问题 2：权限问题
```bash
# 修复文件权限
chmod -R 755 /opt/webtoapp
chown -R nobody:nobody /opt/webtoapp
```

### 问题 3：数据库锁定
```bash
# SQLite 数据库被锁定，重启服务通常可以解决
systemctl restart webtoapp
```

### 问题 4：HTTPS 证书问题
```bash
# 使用 Let's Encrypt 自动获取证书
sudo apt install certbot python3-certbot-nginx
sudo certbot certonly --nginx -d yourdomain.com
```

---

## 安全建议

### 1. 防火墙配置
```bash
# 仅允许特定 IP 访问管理员接口
ufw allow from 192.168.1.0/24 to any port 8080
```

### 2. 限制请求率
```nginx
# 在 Nginx 中配置限流
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
location /api/ {
    limit_req zone=api_limit burst=20 nodelay;
}
```

### 3. 定期安全审计
- 检查访问日志
- 监控异常的 API Key 使用
- 定期更新依赖包

---

## 版本升级

### 升级步骤
```bash
# 1. 备份数据库
cp data/keyserver.db data/keyserver.db.backup

# 2. 停止旧版本
systemctl stop webtoapp

# 3. 拉取最新代码
git pull origin main

# 4. 编译新版本
go build -o webtoapp-server main.go

# 5. 迁移数据库（如需要）
./webtoapp-server --migrate

# 6. 启动新版本
systemctl start webtoapp

# 7. 验证服务
curl http://localhost:8080/api/health
```

---

**更新日期**：2025-01-02
**版本**：1.0.0
