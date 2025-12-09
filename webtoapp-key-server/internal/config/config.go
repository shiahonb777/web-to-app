package config

import (
	"os"
	"strconv"
	"time"
)

// Config 应用配置
type Config struct {
	// 服务器配置
	Port     string
	Env      string
	LogLevel string

	// 数据库配置
	DatabasePath string
	DatabaseUrl  string

	// JWT 配置
	JWTSecret   string
	TokenExpiry time.Duration

	// 服务配置
	MaxRetries     int
	RequestTimeout time.Duration
}

// Load 从环境变量加载配置
func Load() *Config {
	cfg := &Config{
		Port:         getEnv("PORT", "8080"),
		Env:          getEnv("ENV", "development"),
		LogLevel:     getEnv("LOG_LEVEL", "info"),
		DatabasePath: getEnv("DATABASE_PATH", "./data/keyserver.db"),
		JWTSecret:    getEnv("JWT_SECRET", "your-secret-key-change-in-production"),
		MaxRetries:   getEnvInt("MAX_RETRIES", 3),
	}

	// 解析时间配置
	tokenExpiry := getEnvInt("TOKEN_EXPIRY_HOURS", 24)
	cfg.TokenExpiry = time.Duration(tokenExpiry) * time.Hour

	requestTimeout := getEnvInt("REQUEST_TIMEOUT_SECONDS", 30)
	cfg.RequestTimeout = time.Duration(requestTimeout) * time.Second

	return cfg
}

// getEnv 获取环境变量，如果不存在则返回默认值
func getEnv(key, defaultVal string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return defaultVal
}

// getEnvInt 获取整数环境变量
func getEnvInt(key string, defaultVal int) int {
	valStr := getEnv(key, "")
	if val, err := strconv.Atoi(valStr); err == nil {
		return val
	}
	return defaultVal
}
