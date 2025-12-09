package domain

import "time"

// ActivationKey 激活码模型
type ActivationKey struct {
	ID            uint64         `gorm:"primaryKey" json:"id"`
	Code          string         `gorm:"uniqueIndex;not null" json:"code"`
	AppID         string         `gorm:"index;not null" json:"app_id"`
	Status        string         `gorm:"index;default:active" json:"status"` // active/used/expired/revoked
	CreatedAt     time.Time      `gorm:"autoCreateTime" json:"created_at"`
	ExpiresAt     *time.Time     `json:"expires_at"`
	UsedAt        *time.Time     `json:"used_at"`
	MaxUses       int            `gorm:"default:1" json:"max_uses"`
	UsedCount     int            `gorm:"default:0" json:"used_count"`
	DeviceLimit   *int           `json:"device_limit"`
	Notes         string         `json:"notes"`
	CreatedBy     string         `json:"created_by"`
	AuditLogs     []AuditLog     `gorm:"foreignKey:ActivationID" json:"-"`
	DeviceRecords []DeviceRecord `gorm:"foreignKey:ActivationID" json:"-"`
}

// AuditLog 审计日志模型
type AuditLog struct {
	ID           uint64         `gorm:"primaryKey" json:"id"`
	Action       string         `gorm:"index;not null" json:"action"` // verify/generate/revoke/update
	ActivationID *uint64        `json:"activation_id"`
	DeviceID     string         `gorm:"index" json:"device_id"`
	Result       string         `json:"result"` // success/failed
	ErrorMessage string         `json:"error_message"`
	IPAddress    string         `json:"ip_address"`
	DeviceInfo   string         `json:"device_info"` // JSON string
	AppVersion   string         `json:"app_version"`
	CreatedAt    time.Time      `gorm:"autoCreateTime;index" json:"created_at"`
	Activation   *ActivationKey `gorm:"foreignKey:ActivationID" json:"-"`
}

// DeviceRecord 设备记录模型
type DeviceRecord struct {
	ID               uint64         `gorm:"primaryKey" json:"id"`
	DeviceID         string         `gorm:"uniqueIndex:idx_device_app;not null" json:"device_id"`
	AppID            string         `gorm:"uniqueIndex:idx_device_app;not null" json:"app_id"`
	ActivationID     *uint64        `json:"activation_id"`
	DeviceName       string         `json:"device_name"`
	Model            string         `json:"model"`
	OSVersion        string         `json:"os_version"`
	AppVersion       string         `json:"app_version"`
	FirstActivatedAt time.Time      `gorm:"autoCreateTime" json:"first_activated_at"`
	LastActivatedAt  *time.Time     `json:"last_activated_at"`
	ActivationCount  int            `gorm:"default:1" json:"activation_count"`
	Status           string         `gorm:"default:active" json:"status"` // active/blocked/suspended
	Activation       *ActivationKey `gorm:"foreignKey:ActivationID" json:"-"`
}

// VerificationRequest 验证请求
type VerificationRequest struct {
	Code       string                 `json:"code" binding:"required"`
	AppID      string                 `json:"app_id" binding:"required"`
	DeviceID   string                 `json:"device_id" binding:"required"`
	DeviceInfo map[string]interface{} `json:"device_info"`
	Timestamp  int64                  `json:"timestamp" binding:"required"`
}

// VerificationResponse 验证响应
type VerificationResponse struct {
	Success   bool            `json:"success"`
	Message   string          `json:"message"`
	Data      *ActivationData `json:"data,omitempty"`
	Signature string          `json:"signature"`
	Timestamp int64           `json:"timestamp"`
	Code      string          `json:"code,omitempty"` // 错误码
}

// ActivationData 激活数据
type ActivationData struct {
	ActivationID  uint64 `json:"activation_id"`
	DevicesUsed   int    `json:"devices_used"`
	DeviceLimit   *int   `json:"device_limit"`
	ExpiresAt     *int64 `json:"expires_at"`
	RemainingUses int    `json:"remaining_uses"`
	CreatedAt     int64  `json:"created_at"`
}

// GenerateRequest 生成激活码请求
type GenerateRequest struct {
	AppID         string `json:"app_id" binding:"required"`
	Count         int    `json:"count" binding:"required,min=1,max=1000"`
	ExpiresInDays *int   `json:"expires_in_days"`
	MaxUses       int    `json:"max_uses" binding:"min=0"`
	DeviceLimit   *int   `json:"device_limit"`
	Notes         string `json:"notes"`
}

// GenerateResponse 生成激活码响应
type GenerateResponse struct {
	Success   bool       `json:"success"`
	Generated int        `json:"generated"`
	Codes     []CodeItem `json:"codes"`
}

// CodeItem 激活码项
type CodeItem struct {
	Code      string `json:"code"`
	ID        uint64 `json:"id"`
	ExpiresAt *int64 `json:"expires_at"`
}

// ListRequest 列表查询请求
type ListRequest struct {
	AppID  string `form:"app_id" binding:"required"`
	Status string `form:"status"`
	Page   int    `form:"page" binding:"min=1"`
	Limit  int    `form:"limit" binding:"min=1,max=100"`
}

// ListResponse 列表查询响应
type ListResponse struct {
	Success bool            `json:"success"`
	Total   int64           `json:"total"`
	Page    int             `json:"page"`
	Limit   int             `json:"limit"`
	Items   []ActivationKey `json:"items"`
}
