# 使用官方 Go 基础镜像
FROM golang:1.21-alpine AS builder

# 安装依赖
RUN apk add --no-cache git gcc musl-dev sqlite-dev

# 设置工作目录
WORKDIR /build

# 复制 go.mod 和 go.sum
COPY go.mod go.sum ./

# 下载依赖
RUN go mod download

# 复制源代码
COPY . .

# 编译应用
RUN CGO_ENABLED=1 GOOS=linux go build -o keyserver ./cmd/main.go

# 最终镜像
FROM alpine:latest

# 安装 CA 证书和 SQLite 库
RUN apk --no-cache add ca-certificates sqlite-libs

# 设置工作目录
WORKDIR /app

# 创建数据目录
RUN mkdir -p /app/data

# 从构建镜像复制二进制文件
COPY --from=builder /build/keyserver .

# 复制配置文件
COPY configs ./configs

# 暴露端口
EXPOSE 8080

# 启动应用
CMD ["./keyserver"]
