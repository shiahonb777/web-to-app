package service

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/yingcaihuang/webtoapp-key-server/internal/domain"
	"gorm.io/gorm"
)

// APIKeyService API 密钥服务
type APIKeyService struct {
	db *gorm.DB
}

// NewAPIKeyService 创建 API Key 服务实例
func NewAPIKeyService(db *gorm.DB) *APIKeyService {
	return &APIKeyService{db: db}
}

// GenerateAPIKey 生成新的 API Key
func (s *APIKeyService) GenerateAPIKey(name string, permissions []string) (*domain.APIKey, string, error) {
	// 生成密钥
	rawKey := uuid.New().String()
	secret := uuid.New().String()
	keyHash := s.hashKey(rawKey)
	keyPrefix := rawKey[:8] + "..."

	// 转换权限为 JSON
	permissionJSON := strings.Join(permissions, ",")

	apiKey := &domain.APIKey{
		Name:       name,
		KeyHash:    keyHash,
		KeyPrefix:  keyPrefix,
		Secret:     secret,
		Status:     "active",
		Permission: permissionJSON,
	}

	if err := s.db.Create(apiKey).Error; err != nil {
		return nil, "", err
	}

	// 返回完整密钥（仅在创建时返回）
	fullKey := fmt.Sprintf("%s.%s", rawKey, secret)
	return apiKey, fullKey, nil
}

// VerifyAPIKey 验证 API Key
func (s *APIKeyService) VerifyAPIKey(key string) (*domain.APIKey, error) {
	parts := strings.Split(key, ".")
	if len(parts) != 2 {
		return nil, fmt.Errorf("invalid key format")
	}

	rawKey := parts[0]
	keyHash := s.hashKey(rawKey)

	apiKey := &domain.APIKey{}
	if err := s.db.Where("key_hash = ? AND status = ?", keyHash, "active").First(apiKey).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, fmt.Errorf("invalid or inactive api key")
		}
		return nil, err
	}

	// 更新最后使用时间
	now := time.Now()
	s.db.Model(apiKey).Update("last_used", now)

	return apiKey, nil
}

// RevokeAPIKey 撤销 API Key
func (s *APIKeyService) RevokeAPIKey(id uint) error {
	now := time.Now()
	return s.db.Model(&domain.APIKey{}).Where("id = ?", id).Updates(map[string]interface{}{
		"status":     "revoked",
		"revoked_at": now,
	}).Error
}

// ListAPIKeys 列出 API Keys
func (s *APIKeyService) ListAPIKeys(page, limit int) ([]domain.APIKey, int64, error) {
	var keys []domain.APIKey
	var total int64

	if err := s.db.Model(&domain.APIKey{}).Count(&total).Error; err != nil {
		return nil, 0, err
	}

	offset := (page - 1) * limit
	if err := s.db.Offset(offset).Limit(limit).Find(&keys).Error; err != nil {
		return nil, 0, err
	}

	return keys, total, nil
}

// GetAPIKey 获取单个 API Key
func (s *APIKeyService) GetAPIKey(id uint) (*domain.APIKey, error) {
	apiKey := &domain.APIKey{}
	if err := s.db.First(apiKey, id).Error; err != nil {
		return nil, err
	}
	return apiKey, nil
}

// UpdateAPIKey 更新 API Key
func (s *APIKeyService) UpdateAPIKey(id uint, name string, permissions []string) error {
	permissionJSON := strings.Join(permissions, ",")
	return s.db.Model(&domain.APIKey{}).Where("id = ?", id).Updates(map[string]interface{}{
		"name":       name,
		"permission": permissionJSON,
	}).Error
}

// hashKey 哈希 API Key
func (s *APIKeyService) hashKey(key string) string {
	hash := sha256.Sum256([]byte(key))
	return hex.EncodeToString(hash[:])
}

// GetAPIKeyStats 获取 API Key 的统计信息
func (s *APIKeyService) GetAPIKeyStats() (map[string]interface{}, error) {
	var total, active, revoked int64

	s.db.Model(&domain.APIKey{}).Count(&total)
	s.db.Model(&domain.APIKey{}).Where("status = ?", "active").Count(&active)
	s.db.Model(&domain.APIKey{}).Where("status = ?", "revoked").Count(&revoked)

	return map[string]interface{}{
		"total":   total,
		"active":  active,
		"revoked": revoked,
	}, nil
}
