#!/bin/bash

# WebToApp Key Server 项目初始化脚本

set -e

echo "🚀 初始化 WebToApp Key Server 项目..."

# 创建项目目录结构
mkdir -p webtoapp-key-server/{cmd,internal/{api/{handlers,middleware},domain,repository,service,database,config,utils},pkg/keyserver,migrations/sqlite,configs,docker,tests/fixtures,scripts}

cd webtoapp-key-server

echo "📦 初始化 Go Module..."
go mod init github.com/yingcaihuang/webtoapp-key-server

echo "📥 下载依赖..."
go get -u github.com/gin-gonic/gin
go get -u gorm.io/gorm
go get -u gorm.io/driver/sqlite
go get -u github.com/joho/godotenv
go get -u go.uber.org/zap
go get -u github.com/golang-jwt/jwt/v5
go get -u github.com/google/uuid

echo "✅ 项目初始化完成！"
echo ""
echo "项目结构已创建："
tree -L 2 . || find . -type d -not -path '*/\.*' | head -20

echo ""
echo "📝 后续步骤："
echo "1. cd webtoapp-key-server"
echo "2. 配置 configs/config.yaml"
echo "3. 运行 go run cmd/main.go"
