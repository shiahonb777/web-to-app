package handlers

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/yingcaihuang/webtoapp-key-server/internal/domain"
	"github.com/yingcaihuang/webtoapp-key-server/internal/service"
)

var activationService *service.ActivationService
var signatureSecret string

// InitHandlers 初始化处理器
func InitHandlers(secret string) {
	activationService = service.NewActivationService()
	signatureSecret = secret
}

// VerifyActivationCode 验证激活码
// POST /api/activation/verify
func VerifyActivationCode(c *gin.Context) {
	var req domain.VerificationRequest

	// 绑定请求数据
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, domain.VerificationResponse{
			Success:   false,
			Message:   "Invalid request format",
			Timestamp: 0,
		})
		return
	}

	// 调用服务验证激活码
	response, err := activationService.VerifyActivationCode(&req, signatureSecret)
	if err != nil {
		c.JSON(http.StatusInternalServerError, domain.VerificationResponse{
			Success:   false,
			Message:   "Internal server error",
			Timestamp: 0,
		})
		return
	}

	c.JSON(http.StatusOK, response)
}

// GenerateActivationCodes 生成激活码
// POST /api/activation/generate
func GenerateActivationCodes(c *gin.Context) {
	var req domain.GenerateRequest

	// 绑定请求数据
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Invalid request format",
		})
		return
	}

	// 调用服务生成激活码
	response, err := activationService.GenerateActivationCodes(&req)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Failed to generate activation codes",
		})
		return
	}

	c.JSON(http.StatusOK, response)
}

// ListActivationCodes 列出激活码
// GET /api/activation/list
func ListActivationCodes(c *gin.Context) {
	// 获取查询参数
	appID := c.Query("app_id")
	if appID == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Missing app_id parameter",
		})
		return
	}

	page := 1
	if p, err := strconv.Atoi(c.DefaultQuery("page", "1")); err == nil && p > 0 {
		page = p
	}

	limit := 20
	if l, err := strconv.Atoi(c.DefaultQuery("limit", "20")); err == nil && l > 0 && l <= 100 {
		limit = l
	}

	req := domain.ListRequest{
		AppID:  appID,
		Status: c.Query("status"),
		Page:   page,
		Limit:  limit,
	}

	// 调用服务获取列表
	response, err := activationService.ListActivationCodes(&req)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Failed to list activation codes",
		})
		return
	}

	c.JSON(http.StatusOK, response)
}

// RevokeActivationCode 撤销激活码
// DELETE /api/activation/:app_id/:code
func RevokeActivationCode(c *gin.Context) {
	appID := c.Param("app_id")
	code := c.Param("code")

	if appID == "" || code == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "Missing required parameters",
		})
		return
	}

	// 调用服务撤销激活码
	err := activationService.RevokeActivationCode(appID, code)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "Failed to revoke activation code",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "Activation code revoked successfully",
	})
}

// HealthCheck 健康检查
// GET /api/health
func HealthCheck(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "Service is healthy",
		"version": "1.0.0",
	})
}
