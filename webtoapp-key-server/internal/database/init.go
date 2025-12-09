package database

import (
	"log"

	"github.com/yingcaihuang/webtoapp-key-server/internal/domain"
	"gorm.io/gorm"
)

// InitDatabase 初始化数据库表结构
func InitDatabase(db *gorm.DB) error {
	log.Println("初始化数据库表结构...")

	// 自动迁移所有数据模型
	if err := db.AutoMigrate(
		&domain.APIKey{},
		&domain.Statistics{},
		&domain.DailyStats{},
		&domain.AdminAuditLog{},
	); err != nil {
		log.Printf("数据库迁移失败: %v\n", err)
		return err
	}

	log.Println("数据库表结构初始化完成")
	return nil
}

// CreateIndexes 创建必要的数据库索引
func CreateIndexes(db *gorm.DB) error {
	log.Println("创建数据库索引...")

	// API Key 索引
	if err := db.Model(&domain.APIKey{}).
		Exec("CREATE INDEX IF NOT EXISTS idx_api_key_status ON api_keys(status)").Error; err != nil {
		log.Printf("创建 idx_api_key_status 索引失败: %v\n", err)
	}

	if err := db.Model(&domain.APIKey{}).
		Exec("CREATE INDEX IF NOT EXISTS idx_api_key_hash ON api_keys(key_hash)").Error; err != nil {
		log.Printf("创建 idx_api_key_hash 索引失败: %v\n", err)
	}

	if err := db.Model(&domain.APIKey{}).
		Exec("CREATE INDEX IF NOT EXISTS idx_api_key_created_at ON api_keys(created_at)").Error; err != nil {
		log.Printf("创建 idx_api_key_created_at 索引失败: %v\n", err)
	}

	// 统计索引
	if err := db.Model(&domain.Statistics{}).
		Exec("CREATE INDEX IF NOT EXISTS idx_statistics_app_id ON statistics(app_id)").Error; err != nil {
		log.Printf("创建 idx_statistics_app_id 索引失败: %v\n", err)
	}

	// 日统计索引
	if err := db.Model(&domain.DailyStats{}).
		Exec("CREATE INDEX IF NOT EXISTS idx_daily_stats_app_id ON daily_stats(app_id)").Error; err != nil {
		log.Printf("创建 idx_daily_stats_app_id 索引失败: %v\n", err)
	}

	if err := db.Model(&domain.DailyStats{}).
		Exec("CREATE INDEX IF NOT EXISTS idx_daily_stats_date ON daily_stats(date)").Error; err != nil {
		log.Printf("创建 idx_daily_stats_date 索引失败: %v\n", err)
	}

	// 审计日志索引
	if err := db.Model(&domain.AdminAuditLog{}).
		Exec("CREATE INDEX IF NOT EXISTS idx_audit_log_admin_id ON admin_audit_logs(admin_id)").Error; err != nil {
		log.Printf("创建 idx_audit_log_admin_id 索引失败: %v\n", err)
	}

	if err := db.Model(&domain.AdminAuditLog{}).
		Exec("CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp ON admin_audit_logs(timestamp)").Error; err != nil {
		log.Printf("创建 idx_audit_log_timestamp 索引失败: %v\n", err)
	}

	log.Println("数据库索引创建完成")
	return nil
}

// SeedDatabase 数据库种子数据（初始化）
func SeedDatabase(db *gorm.DB) error {
	log.Println("初始化种子数据...")

	// 可以在这里添加初始数据
	// 例如：创建默认的管理员 API Key

	log.Println("种子数据初始化完成")
	return nil
}
