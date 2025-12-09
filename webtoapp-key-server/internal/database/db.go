package database

import (
	"fmt"
	"log"

	"github.com/yingcaihuang/webtoapp-key-server/internal/config"
	"github.com/yingcaihuang/webtoapp-key-server/internal/domain"
	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
)

// DB 数据库实例
var DB *gorm.DB

// Init 初始化数据库
func Init(cfg *config.Config) error {
	var err error

	// 创建数据库连接
	DB, err = gorm.Open(sqlite.Open(cfg.DatabasePath), &gorm.Config{})
	if err != nil {
		return fmt.Errorf("failed to connect to database: %w", err)
	}

	log.Printf("✓ Database connected: %s", cfg.DatabasePath)

	// 自动迁移数据库表
	if err := DB.AutoMigrate(
		&domain.ActivationKey{},
		&domain.AuditLog{},
		&domain.DeviceRecord{},
		&domain.APIKey{},
		&domain.Statistics{},
		&domain.DailyStats{},
		&domain.AdminAuditLog{},
	); err != nil {
		return fmt.Errorf("failed to migrate database: %w", err)
	}

	log.Println("✓ Database tables migrated successfully")

	// 创建索引
	if err := createIndexes(); err != nil {
		return fmt.Errorf("failed to create indexes: %w", err)
	}

	return nil
}

// createIndexes 创建数据库索引
func createIndexes() error {
	// ActivationKey 索引
	DB.Exec("CREATE INDEX IF NOT EXISTS idx_activation_app_id ON activation_keys(app_id)")
	DB.Exec("CREATE INDEX IF NOT EXISTS idx_activation_status ON activation_keys(status)")
	DB.Exec("CREATE INDEX IF NOT EXISTS idx_activation_code ON activation_keys(code)")

	// AuditLog 索引
	DB.Exec("CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs(action)")
	DB.Exec("CREATE INDEX IF NOT EXISTS idx_audit_device_id ON audit_logs(device_id)")
	DB.Exec("CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at)")

	// DeviceRecord 索引
	DB.Exec("CREATE INDEX IF NOT EXISTS idx_device_app ON device_records(device_id, app_id)")

	return nil
}

// GetDB 获取数据库实例
func GetDB() *gorm.DB {
	return DB
}

// Close 关闭数据库连接
func Close() error {
	sqlDB, err := DB.DB()
	if err != nil {
		return err
	}
	return sqlDB.Close()
}
