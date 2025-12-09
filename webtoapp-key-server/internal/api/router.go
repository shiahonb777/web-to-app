package api

import (
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

	// 应用中间件
	router.Use(middleware.CORSMiddleware())
	router.Use(middleware.LoggingMiddleware())
	router.Use(middleware.ErrorHandlingMiddleware())
	router.Use(middleware.RateLimitMiddleware(100))

	// 健康检查路由（不需要认证）
	router.GET("/api/health", handlers.HealthCheck)

	// 认证 API 路由
	authRoutes := router.Group("/api/activation")
	authRoutes.Use(middleware.RequestSignature(cfg))

	// 验证激活码
	authRoutes.POST("/verify", handlers.VerifyActivationCode)

	// 生成激活码
	authRoutes.POST("/generate", handlers.GenerateActivationCodes)

	// 列出激活码
	authRoutes.GET("/list", handlers.ListActivationCodes)

	// 撤销激活码
	authRoutes.DELETE("/:app_id/:code", handlers.RevokeActivationCode)

	// ===== 管理员 API 路由（需要 API Key 认证）=====
	adminHandlers := handlers.NewAdminHandlers(db)
	adminRoutes := router.Group("/api/admin")
	adminRoutes.Use(middleware.APIKeyAuth(db))
	adminRoutes.Use(middleware.RecordAPIKeyUsage(db))

	// API Key 管理
	apiKeyRoutes := adminRoutes.Group("/api-keys")
	apiKeyRoutes.POST("", adminHandlers.GenerateAPIKey)                                  // 创建 API Key
	apiKeyRoutes.GET("", adminHandlers.ListAPIKeys)                                      // 列出 API Keys
	apiKeyRoutes.GET("/:id", adminHandlers.GetAPIKey)                                    // 获取单个 API Key
	apiKeyRoutes.PUT("/:id", adminHandlers.UpdateAPIKey)                                 // 更新 API Key
	apiKeyRoutes.DELETE("/:id", adminHandlers.RevokeAPIKey)                              // 撤销 API Key
	apiKeyRoutes.GET("/stats", adminHandlers.GetAPIKeyStats)                             // API Key 统计

	// 统计数据
	statsRoutes := adminRoutes.Group("/statistics")
	statsRoutes.GET("", adminHandlers.GetStatistics)                                     // 获取总体统计
	statsRoutes.GET("/dashboard", adminHandlers.GetDashboard)                            // 获取仪表板数据
	statsRoutes.GET("/apps/:app_id", adminHandlers.GetAppStatistics)                     // 获取应用统计
	statsRoutes.GET("/apps/:app_id/trends", adminHandlers.GetTrendData)                  // 获取趋势数据

	// 管理员健康检查
	adminRoutes.GET("/health", adminHandlers.HealthCheck)

	return router
}
