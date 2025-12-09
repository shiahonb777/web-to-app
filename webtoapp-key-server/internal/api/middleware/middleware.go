package middleware
package middleware

import (
	"fmt"
	"log"
	"time"

	"github.com/gin-gonic/gin"
)


































































}	}		c.Next()		}			return			c.Abort()			})				"message": "无效的 API Key",				"code":    "UNAUTHORIZED",				"success": false,			c.JSON(401, gin.H{			log.Printf("❌ 无效的 API Key: %s", key)		if key != apiKey {		key := c.GetHeader("X-API-Key")		// 验证 API Key		}			return			c.Next()		if c.Request.URL.Path == "/health" || c.Request.URL.Path == "/api/v1/activation/verify" {		// 跳过某些路由	return func(c *gin.Context) {func AuthMiddleware(apiKey string) gin.HandlerFunc {// 认证中间件}	}		c.Next()		}			return			c.AbortWithStatus(204)		if c.Request.Method == "OPTIONS" {		c.Writer.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS, GET, PUT, DELETE")		c.Writer.Header().Set("Access-Control-Allow-Headers", "Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization, accept, origin, Cache-Control, X-Requested-With")		c.Writer.Header().Set("Access-Control-Allow-Credentials", "true")		c.Writer.Header().Set("Access-Control-Allow-Origin", "*")	return func(c *gin.Context) {func CORSMiddleware(config interface{}) gin.HandlerFunc {// CORS 中间件}	}		c.Next()		// TODO: 实现真正的速率限制	return func(c *gin.Context) {func RateLimitMiddleware() gin.HandlerFunc {// 速率限制中间件（简单实现）}	})		)			param.Latency,			param.StatusCode,			param.Path,			param.Method,			param.ClientIP,			param.TimeStamp.Format(time.DateTime),			"[%s] %s %s %s %d %v\n",		return fmt.Sprintf(	return gin.LoggerWithFormatter(func(param gin.LogFormatterParams) string {func LoggingMiddleware() gin.HandlerFunc {// 日志中间件