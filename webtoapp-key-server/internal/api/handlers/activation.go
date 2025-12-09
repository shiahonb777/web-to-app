package handlers

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/yingcaihuang/webtoapp-key-server/internal/domain"
	"github.com/yingcaihuang/webtoapp-key-server/internal/service"
	"gorm.io/gorm"
)

var activationService *service.ActivationService
var signatureSecret string

// 初始化处理器
func InitHandlers(db *gorm.DB, secret string) {
	activationService = service.NewActivationService(db)
	signatureSecret = secret
}

// 验证激活码
func VerifyActivationCode(c *gin.Context) {
	var req domain.VerificationRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"code":    "INVALID_REQUEST",
			"message": "请求参数无效",
		})
		return
	}

	resp, err := activationService.VerifyCode(&req, signatureSecret)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"code":    "INTERNAL_ERROR",
			"message": "服务器内部错误",
		})
		return
	}

	statusCode := http.StatusOK
	if !resp.Success {
		// 根据错误码设置不同的状态码
		switch resp.Code {
		case "DEVICE_LIMIT_EXCEEDED":
			statusCode = http.StatusTooManyRequests
		case "TIMESTAMP_INVALID":
			statusCode = http.StatusBadRequest
		default:
			statusCode = http.StatusUnauthorized
		}
	}

	c.JSON(statusCode, resp)
}

// 生成激活码
func GenerateActivationCodes(c *gin.Context) {
	var req domain.GenerateRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "请求参数无效",
		})
		return
	}

	codes, err := activationService.GenerateActivationCodes(&req)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "生成激活码失败",
		})
		return
	}

	codeItems := make([]domain.CodeItem, len(codes))
	for i, code := range codes {
		codeItems[i] = domain.CodeItem{Code: code}
	}

	c.JSON(http.StatusOK, gin.H{
		"success":   true,
		"generated": len(codes),
		"codes":     codeItems,
	})
}

// 获取激活码列表
func ListActivationCodes(c *gin.Context) {
	appID := c.Query("app_id")
	if appID == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "缺少必要参数: app_id",
		})
		return
	}

	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	limit, _ := strconv.Atoi(c.DefaultQuery("limit", "20"))
	status := c.Query("status")

	if page < 1 {
		page = 1
	}
	if limit < 1 || limit > 100 {
		limit = 20
	}

	req := &domain.ListRequest{
		AppID:  appID,
		Status: status,
		Page:   page,
		Limit:  limit,
	}

	items, total, err := activationService.ListActivationCodes(req)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "查询失败",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"total":   total,
		"page":    page,
		"limit":   limit,
		"items":   items,
	})
}

// 撤销激活码
func RevokeActivationCode(c *gin.Context) {
	idStr := c.Param("id")
	id, err := strconv.ParseUint(idStr, 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"success": false,
			"message": "无效的激活码ID",
		})
		return
	}

	var req struct {
		Reason string `json:"reason"`
	}
	c.ShouldBindJSON(&req)

	if err := activationService.RevokeCode(id, req.Reason); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"success": false,
			"message": "撤销失败",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "激活码已撤销",
	})
}

// 获取设备列表
func ListDevices(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "功能开发中",
	})
}

// 获取设备信息
func GetDeviceInfo(c *gin.Context) {
	deviceID := c.Param("device_id")
	c.JSON(http.StatusOK, gin.H{
		"success":   true,
		"device_id": deviceID,
		"message":   "功能开发中",
	})
}

// 获取审计日志
func GetAuditLogs(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"message": "功能开发中",
	})
}
