package main

import (
	"fmt"
	"log"
	"os"

	"github.com/gin-gonic/gin"
	"github.com/yingcaihuang/webtoapp-key-server/internal/config"
	"github.com/yingcaihuang/webtoapp-key-server/internal/database"
	"github.com/yingcaihuang/webtoapp-key-server/internal/api/handlers"
	"github.com/yingcaihuang/webtoapp-key-server/internal/api/middleware"
)

func main() {
	// 加载配置
	cfg := config.LoadConfig()
	
	// 初始化数据库
	db, err := database.InitDB(cfg.Database)
	if err != nil {
		log.Fatalf("Failed to initialize database: %v", err)
	}
	
	// 创建 Gin 引擎
	router := gin.Default()
	
	// 应用中间件
	router.Use(middleware.LoggingMiddleware())
	router.Use(middleware.RateLimitMiddleware())
	router.Use(middleware.CORSMiddleware(cfg.CORS))
	
	// 注册路由
	registerRoutes(router, db)
	
	// 启动服务器
	port := cfg.Server.Port
	addr := fmt.Sprintf(":%d", port)
	log.Printf("🚀 Server running on %s", addr)
	
	if err := router.Run(addr); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}

func registerRoutes(router *gin.Engine, db interface{}) {
	// 健康检查
	router.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"status": "ok",
			"timestamp": time.Now().Unix(),
		})
	})
	
	// API v1
	v1 := router.Group("/api/v1")
	{
		// 激活码相关
		activation := v1.Group("/activation")
		{
			activation.POST("/verify", handlers.VerifyActivationCode)
			activation.POST("/generate", handlers.GenerateActivationCodes)
			activation.GET("/list", handlers.ListActivationCodes)
			activation.POST("/:id/revoke", handlers.RevokeActivationCode)
		}
		
		// 设备相关
		devices := v1.Group("/devices")
		{
			devices.GET("/list", handlers.ListDevices)
			devices.GET("/:device_id", handlers.GetDeviceInfo)
		}
		
		// 审计日志
		audit := v1.Group("/audit")
		{
			audit.GET("/logs", handlers.GetAuditLogs)
		}
	}
}
