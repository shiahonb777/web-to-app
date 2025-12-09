package main

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"log"

	"github.com/google/uuid"
	"github.com/yingcaihuang/webtoapp-key-server/internal/config"
	"github.com/yingcaihuang/webtoapp-key-server/internal/database"
	"github.com/yingcaihuang/webtoapp-key-server/internal/domain"
)

// hashKey 生成密钥哈希
func hashKey(key string) string {
	hash := sha256.Sum256([]byte(key))
	return hex.EncodeToString(hash[:])
}

func main() {
	// 初始化配置
	cfg := &config.Config{
		DatabasePath: "./data/keyserver.db",
	}

	// 初始化数据库
	if err := database.Init(cfg); err != nil {
		log.Fatalf("❌ 数据库初始化失败: %v", err)
	}

	// 确保 APIKey 表存在
	if !database.DB.Migrator().HasTable(&domain.APIKey{}) {
		if err := database.DB.Migrator().CreateTable(&domain.APIKey{}); err != nil {
			log.Fatalf("❌ 创建 APIKey 表失败: %v", err)
		}
		log.Println("✓ APIKey 表已创建")
	}

	// 生成密钥
	rawKey := uuid.New().String()
	secret := uuid.New().String()
	keyHash := hashKey(rawKey)
	keyPrefix := rawKey[:8] + "..."

	// 创建 API Key 对象
	apiKey := &domain.APIKey{
		Name:       "Admin Key",
		KeyHash:    keyHash,
		KeyPrefix:  keyPrefix,
		Secret:     secret,
		Status:     "active",
		Permission: "read:statistics,write:apikeys,read:logs,write:logs",
	}

	// 保存到数据库
	if err := database.DB.Create(apiKey).Error; err != nil {
		log.Fatalf("❌ 保存 API Key 失败: %v", err)
	}

	// 完整密钥（仅显示一次）
	fullKey := fmt.Sprintf("%s.%s", rawKey, secret)

	// 输出结果
	fmt.Println("\n✅ API Key 生成成功！\n")
	fmt.Println("==================================================")
	fmt.Printf("ID:        %d\n", apiKey.ID)
	fmt.Printf("名称:      %s\n", apiKey.Name)
	fmt.Printf("完整 Key:  %s\n", fullKey)
	fmt.Printf("状态:      %s\n", apiKey.Status)
	fmt.Printf("权限:      %s\n", apiKey.Permission)
	fmt.Printf("创建时间:  %v\n", apiKey.CreatedAt)
	fmt.Println("==================================================")
	fmt.Println("\n📝 注意: 完整 Key 仅显示一次，请安全保管！")
	fmt.Println("🔐 登录时使用上述完整 Key")
}
