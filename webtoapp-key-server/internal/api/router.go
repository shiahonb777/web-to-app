package api

import (
	"io/ioutil"

	"github.com/gin-gonic/gin"
	"github.com/yingcaihuang/webtoapp-key-server/internal/api/handlers"
	"github.com/yingcaihuang/webtoapp-key-server/internal/api/middleware"
	"github.com/yingcaihuang/webtoapp-key-server/internal/config"
	"gorm.io/gorm"
)

// SetupRouter 设置路由
func SetupRouter(cfg *config.Config, db *gorm.DB) *gin.Engine {
	router := gin.New()

	// 初始化处理器
	handlers.InitHandlers(cfg.JWTSecret)

	// 将数据库实例添加到上下文
	router.Use(func(c *gin.Context) {
		c.Set("db", db)
		c.Next()
	})

	// 应用中间件
	router.Use(middleware.CORSMiddleware())
	router.Use(middleware.LoggingMiddleware())
	router.Use(middleware.ErrorHandlingMiddleware())
	router.Use(middleware.RateLimitMiddleware(100))

	// 健康检查路由（不需要认证）
	router.GET("/api/health", handlers.HealthCheck)

	// 获取默认管理员 API Key 信息（用于登录页面显示）
	router.GET("/api/default-admin-key", handlers.GetDefaultAdminKey)

	// ===== 激活码公开 API 路由（无需认证）=====
	// 验证激活码 - 公开端点（客户端需要调用）
	router.POST("/api/activation/verify", handlers.VerifyActivationCode)

	// ===== 激活码管理 API 路由（需要 API Key 认证）=====
	activationRoutes := router.Group("/api/activation")
	activationRoutes.Use(middleware.APIKeyAuth(db))
	activationRoutes.Use(middleware.RecordAPIKeyUsage(db))
	activationRoutes.Use(middleware.AuditLoggerMiddleware(db))

	// 生成激活码（需要 API Key 认证）
	activationRoutes.POST("/generate", handlers.GenerateActivationCodes)

	// 列出激活码（需要 API Key 认证）
	activationRoutes.GET("/list", handlers.ListActivationCodes)

	// 撤销激活码（需要 API Key 认证）
	activationRoutes.DELETE("/:app_id/:code", handlers.RevokeActivationCode)

	// ===== 管理员 API 路由（需要 API Key 认证）=====
	adminHandlers := handlers.NewAdminHandlers(db)
	adminRoutes := router.Group("/api/admin")
	adminRoutes.Use(middleware.APIKeyAuth(db))
	adminRoutes.Use(middleware.RecordAPIKeyUsage(db))
	adminRoutes.Use(middleware.AuditLoggerMiddleware(db))

	// API Key 管理
	apiKeyRoutes := adminRoutes.Group("/api-keys")
	apiKeyRoutes.POST("", adminHandlers.GenerateAPIKey)      // 创建 API Key
	apiKeyRoutes.GET("", adminHandlers.ListAPIKeys)          // 列出 API Keys
	apiKeyRoutes.GET("/:id", adminHandlers.GetAPIKey)        // 获取单个 API Key
	apiKeyRoutes.PUT("/:id", adminHandlers.UpdateAPIKey)     // 更新 API Key
	apiKeyRoutes.DELETE("/:id", adminHandlers.RevokeAPIKey)  // 撤销 API Key
	apiKeyRoutes.GET("/stats", adminHandlers.GetAPIKeyStats) // API Key 统计

	// 统计数据
	statsRoutes := adminRoutes.Group("/statistics")
	statsRoutes.GET("", adminHandlers.GetStatistics)                    // 获取总体统计
	statsRoutes.GET("/dashboard", adminHandlers.GetDashboard)           // 获取仪表板数据
	statsRoutes.GET("/apps/:app_id", adminHandlers.GetAppStatistics)    // 获取应用统计
	statsRoutes.GET("/apps/:app_id/trends", adminHandlers.GetTrendData) // 获取趋势数据

	// 审计日志
	adminRoutes.GET("/logs", adminHandlers.GetLogs)

	// 管理员健康检查
	adminRoutes.GET("/health", adminHandlers.HealthCheck)

	// 静态文件服务 - 直接为前端文件提供服务
	router.GET("/", func(c *gin.Context) {
		data, err := ioutil.ReadFile("./web/index.html")
		if err != nil {
			c.JSON(404, gin.H{"error": "index.html not found"})
			return
		}
		c.Header("Content-Type", "text/html; charset=utf-8")
		c.String(200, string(data))
	})
	router.GET("/index.html", func(c *gin.Context) {
		data, err := ioutil.ReadFile("./web/index.html")
		if err != nil {
			c.JSON(404, gin.H{"error": "index.html not found"})
			return
		}
		c.Header("Content-Type", "text/html; charset=utf-8")
		c.String(200, string(data))
	})
	router.GET("/login.html", func(c *gin.Context) {
		data, err := ioutil.ReadFile("./web/login.html")
		if err != nil {
			c.JSON(404, gin.H{"error": "login.html not found"})
			return
		}
		c.Header("Content-Type", "text/html; charset=utf-8")
		c.String(200, string(data))
	})
	router.Static("/css", "./web/css")
	router.Static("/js", "./web/js")
	router.Static("/libs", "./web/libs")
	router.StaticFS("/static", gin.Dir("./web", false))

	return router
}
