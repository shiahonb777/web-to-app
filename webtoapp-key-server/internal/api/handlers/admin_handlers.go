package handlers

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/yingcaihuang/webtoapp-key-server/internal/service"
	"gorm.io/gorm"
)

// AdminHandlers 管理员处理器
type AdminHandlers struct {
	apiKeyService     *service.APIKeyService
	statisticsService *service.StatisticsService
}

// NewAdminHandlers 创建管理员处理器
func NewAdminHandlers(db *gorm.DB) *AdminHandlers {
	return &AdminHandlers{
		apiKeyService:     service.NewAPIKeyService(db),
		statisticsService: service.NewStatisticsService(db),
	}
}

// GenerateAPIKey 生成 API Key
func (h *AdminHandlers) GenerateAPIKey(c *gin.Context) {
	var req struct {
		Name        string   `json:"name" binding:"required"`
		Permissions []string `json:"permissions"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "invalid request body",
		})
		return
	}

	// 默认权限
	if len(req.Permissions) == 0 {
		req.Permissions = []string{"read:statistics", "read:logs"}
	}

	apiKey, fullKey, err := h.apiKeyService.GenerateAPIKey(req.Name, req.Permissions)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": err.Error(),
		})
		return
	}

	c.JSON(http.StatusCreated, gin.H{
		"id":         apiKey.ID,
		"name":       apiKey.Name,
		"key_prefix": apiKey.KeyPrefix,
		"full_key":   fullKey,
		"status":     apiKey.Status,
		"created_at": apiKey.CreatedAt,
	})
}

// ListAPIKeys 列出 API Keys
func (h *AdminHandlers) ListAPIKeys(c *gin.Context) {
	page := c.DefaultQuery("page", "1")
	limit := c.DefaultQuery("limit", "10")

	pageNum, _ := strconv.Atoi(page)
	limitNum, _ := strconv.Atoi(limit)

	if pageNum < 1 {
		pageNum = 1
	}
	if limitNum < 1 {
		limitNum = 10
	}

	keys, total, err := h.apiKeyService.ListAPIKeys(pageNum, limitNum)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": err.Error(),
		})
		return
	}

	// 隐藏敏感信息
	for i := range keys {
		keys[i].Secret = "***"
	}

	c.JSON(http.StatusOK, gin.H{
		"data":  keys,
		"total": total,
		"page":  pageNum,
		"limit": limitNum,
	})
}

// GetAPIKey 获取单个 API Key
func (h *AdminHandlers) GetAPIKey(c *gin.Context) {
	id := c.Param("id")
	idNum, err := strconv.ParseUint(id, 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "invalid id",
		})
		return
	}

	apiKey, err := h.apiKeyService.GetAPIKey(uint(idNum))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"error": "api key not found",
		})
		return
	}

	// 隐藏敏感信息
	apiKey.Secret = "***"

	c.JSON(http.StatusOK, apiKey)
}

// RevokeAPIKey 撤销 API Key
func (h *AdminHandlers) RevokeAPIKey(c *gin.Context) {
	id := c.Param("id")
	idNum, err := strconv.ParseUint(id, 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "invalid id",
		})
		return
	}

	if err := h.apiKeyService.RevokeAPIKey(uint(idNum)); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"message": "api key revoked successfully",
	})
}

// UpdateAPIKey 更新 API Key
func (h *AdminHandlers) UpdateAPIKey(c *gin.Context) {
	id := c.Param("id")
	idNum, err := strconv.ParseUint(id, 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "invalid id",
		})
		return
	}

	var req struct {
		Name        string   `json:"name"`
		Permissions []string `json:"permissions"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "invalid request body",
		})
		return
	}

	if err := h.apiKeyService.UpdateAPIKey(uint(idNum), req.Name, req.Permissions); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"message": "api key updated successfully",
	})
}

// GetAPIKeyStats 获取 API Key 统计
func (h *AdminHandlers) GetAPIKeyStats(c *gin.Context) {
	stats, err := h.apiKeyService.GetAPIKeyStats()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, stats)
}

// GetStatistics 获取应用统计
func (h *AdminHandlers) GetStatistics(c *gin.Context) {
	stats, err := h.statisticsService.GetStatistics()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, stats)
}

// GetAppStatistics 获取单个应用统计
func (h *AdminHandlers) GetAppStatistics(c *gin.Context) {
	appID := c.Param("app_id")
	if appID == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "app_id is required",
		})
		return
	}

	stats, err := h.statisticsService.GetAppStatistics(appID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"error": "statistics not found",
		})
		return
	}

	c.JSON(http.StatusOK, stats)
}

// GetTrendData 获取趋势数据
func (h *AdminHandlers) GetTrendData(c *gin.Context) {
	appID := c.Param("app_id")
	days := c.DefaultQuery("days", "7")

	daysNum, _ := strconv.Atoi(days)
	if daysNum < 1 {
		daysNum = 7
	}

	trends, err := h.statisticsService.GetTrendData(appID, daysNum)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"app_id": appID,
		"days":   daysNum,
		"data":   trends,
	})
}

// GetDashboard 获取仪表板数据
func (h *AdminHandlers) GetDashboard(c *gin.Context) {
	// 获取 API Key 统计
	keyStats, _ := h.apiKeyService.GetAPIKeyStats()

	// 获取应用统计
	stats, _ := h.statisticsService.GetStatistics()

	// 获取排名前 5 的应用
	topApps, _ := h.statisticsService.GetTopApps(5)

	c.JSON(http.StatusOK, gin.H{
		"api_keys": keyStats,
		"stats":    stats,
		"top_apps": topApps,
	})
}

// GetLogs 获取审计日志
func (h *AdminHandlers) GetLogs(c *gin.Context) {
	page := c.DefaultQuery("page", "1")
	limit := c.DefaultQuery("limit", "20")

	pageNum, _ := strconv.Atoi(page)
	limitNum, _ := strconv.Atoi(limit)

	if pageNum < 1 {
		pageNum = 1
	}
	if limitNum < 1 || limitNum > 100 {
		limitNum = 20
	}

	// 从AdminAuditLog表获取日志
	var logs []interface{}
	var total int64

	// 这里先返回空数组，因为日志表需要通过middleware自动记录
	c.JSON(http.StatusOK, gin.H{
		"data":  logs,
		"total": total,
		"page":  pageNum,
		"limit": limitNum,
	})
}

// HealthCheck 健康检查
func (h *AdminHandlers) HealthCheck(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"status":    "ok",
		"timestamp": c.GetTime("timestamp"),
	})
}
