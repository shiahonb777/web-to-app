package middleware

import (
	"log"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/yingcaihuang/webtoapp-key-server/internal/domain"
	"github.com/yingcaihuang/webtoapp-key-server/internal/service"
	"gorm.io/gorm"
)

// APIKeyAuth API Key 认证中间件
func APIKeyAuth(db *gorm.DB) gin.HandlerFunc {
	apiKeyService := service.NewAPIKeyService(db)

	return func(c *gin.Context) {
		// 获取 Authorization 头
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" {
			c.JSON(401, gin.H{
				"error": "missing authorization header",
			})
			c.Abort()
			return
		}

		// 提取 Bearer token
		parts := strings.Split(authHeader, " ")
		if len(parts) != 2 || parts[0] != "Bearer" {
			c.JSON(401, gin.H{
				"error": "invalid authorization header format",
			})
			c.Abort()
			return
		}

		apiKey := parts[1]

		// 验证 API Key
		keyRecord, err := apiKeyService.VerifyAPIKey(apiKey)
		if err != nil {
			log.Printf("API Key verification failed: %v", err)
			c.JSON(401, gin.H{
				"error": "invalid or inactive api key",
			})
			c.Abort()
			return
		}

		// 将 API Key 信息存储到上下文
		c.Set("api_key_id", keyRecord.ID)
		c.Set("api_key_name", keyRecord.Name)
		c.Set("api_key_permissions", keyRecord.Permission)

		c.Next()
	}
}

// CheckPermission 检查权限
func CheckPermission(permission string) gin.HandlerFunc {
	return func(c *gin.Context) {
		permissions, exists := c.Get("api_key_permissions")
		if !exists {
			c.JSON(403, gin.H{
				"error": "no permissions found",
			})
			c.Abort()
			return
		}

		permStr, ok := permissions.(string)
		if !ok {
			c.JSON(403, gin.H{
				"error": "invalid permissions format",
			})
			c.Abort()
			return
		}

		// 检查是否拥有该权限
		perms := strings.Split(permStr, ",")
		hasPermission := false
		for _, p := range perms {
			if p == permission {
				hasPermission = true
				break
			}
		}

		if !hasPermission {
			c.JSON(403, gin.H{
				"error": "insufficient permissions",
			})
			c.Abort()
			return
		}

		c.Next()
	}
}

// RecordAPIKeyUsage 记录 API Key 使用情况
func RecordAPIKeyUsage(db *gorm.DB) gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Next()

		// 获取 API Key ID
		apiKeyID, exists := c.Get("api_key_id")
		if !exists {
			return
		}

		id, ok := apiKeyID.(uint)
		if !ok {
			return
		}

		// 记录审计日志
		auditLog := &domain.AdminAuditLog{
			AdminID:   id,
			Action:    c.Request.Method + " " + c.Request.URL.Path,
			Resource:  c.Request.URL.Path,
			Details:   c.Request.Method,
			Status:    "success",
			IPAddress: c.ClientIP(),
		}

		db.Create(auditLog)
	}
}
