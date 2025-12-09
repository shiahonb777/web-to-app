package domain

import (
	"time"

	"gorm.io/gorm"
)

// APIKey API 密钥模型
type APIKey struct {
	ID        uint           `gorm:"primaryKey" json:"id"`
	Name      string         `gorm:"index;not null" json:"name"`
	KeyHash   string         `gorm:"uniqueIndex;not null" json:"key_hash"`
	KeyPrefix string         `gorm:"not null" json:"key_prefix"`
	Secret    string         `gorm:"not null" json:"secret"`
	Status    string         `gorm:"type:varchar(20);default:'active'" json:"status"` // active, inactive, revoked
	Permission string        `gorm:"type:text" json:"permission"` // JSON 格式权限列表
	LastUsed  *time.Time     `json:"last_used"`
	CreatedAt time.Time      `gorm:"autoCreateTime" json:"created_at"`
	UpdatedAt time.Time      `gorm:"autoUpdateTime" json:"updated_at"`
	RevokedAt *time.Time     `json:"revoked_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
}

// TableName 表名
func (APIKey) TableName() string {
	return "api_keys"
}

// Statistics 统计数据模型
type Statistics struct {
	ID                  uint      `gorm:"primaryKey" json:"id"`
	AppID               string    `gorm:"index" json:"app_id"`
	TotalActivations    int64     `json:"total_activations"`
	SuccessfulVerifications int64 `json:"successful_verifications"`
	FailedVerifications int64     `json:"failed_verifications"`
	TotalDevices        int64     `json:"total_devices"`
	ActiveCodes         int64     `json:"active_codes"`
	RevokedCodes        int64     `json:"revoked_codes"`
	ExpiredCodes        int64     `json:"expired_codes"`
	LastUpdated         time.Time `gorm:"autoUpdateTime" json:"last_updated"`
	CreatedAt           time.Time `gorm:"autoCreateTime" json:"created_at"`
}

// TableName 表名
func (Statistics) TableName() string {
	return "statistics"
}

// DailyStats 日统计数据
type DailyStats struct {
	ID                  uint      `gorm:"primaryKey" json:"id"`
	AppID               string    `gorm:"index" json:"app_id"`
	Date                time.Time `gorm:"type:date;index" json:"date"`
	VerificationCount   int64     `json:"verification_count"`
	SuccessCount        int64     `json:"success_count"`
	FailureCount        int64     `json:"failure_count"`
	NewDevices          int64     `json:"new_devices"`
	CodesGenerated      int64     `json:"codes_generated"`
	CodesRevoked        int64     `json:"codes_revoked"`
	CreatedAt           time.Time `gorm:"autoCreateTime" json:"created_at"`
}

// TableName 表名
func (DailyStats) TableName() string {
	return "daily_stats"
}

// AuditLog 审计日志
type AdminAuditLog struct {
	ID        uint      `gorm:"primaryKey" json:"id"`
	AdminID   uint      `gorm:"index" json:"admin_id"`
	Action    string    `json:"action"` // generate_key, revoke_key, view_stats, etc
	Resource  string    `json:"resource"` // api_key, statistics, etc
	Details   string    `gorm:"type:text" json:"details"`
	Status    string    `json:"status"` // success, failure
	IPAddress string    `json:"ip_address"`
	CreatedAt time.Time `gorm:"autoCreateTime;index" json:"created_at"`
}

// TableName 表名
func (AdminAuditLog) TableName() string {
	return "admin_audit_logs"
}
