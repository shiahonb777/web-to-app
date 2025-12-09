package service

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"time"

	"github.com/google/uuid"
	"github.com/yingcaihuang/webtoapp-key-server/internal/database"
	"github.com/yingcaihuang/webtoapp-key-server/internal/domain"
	"gorm.io/gorm"
)

// ActivationService 激活码服务
type ActivationService struct {
	db *gorm.DB
}

// NewActivationService 创建激活码服务
func NewActivationService() *ActivationService {
	return &ActivationService{
		db: database.GetDB(),
	}
}

// VerifyActivationCode 验证激活码
func (s *ActivationService) VerifyActivationCode(
	req *domain.VerificationRequest,
	secretKey string,
) (*domain.VerificationResponse, error) {
	// 检查激活码是否存在
	var activationKey domain.ActivationKey
	if err := s.db.Where("code = ? AND app_id = ?", req.Code, req.AppID).First(&activationKey).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return &domain.VerificationResponse{
				Success:   false,
				Message:   "Activation code not found",
				Timestamp: time.Now().Unix(),
				Code:      "CODE_NOT_FOUND",
			}, nil
		}
		return nil, err
	}

	// 检查激活码状态
	if activationKey.Status != "active" {
		return &domain.VerificationResponse{
			Success:   false,
			Message:   fmt.Sprintf("Activation code is %s", activationKey.Status),
			Timestamp: time.Now().Unix(),
			Code:      "CODE_" + activationKey.Status,
		}, nil
	}

	// 检查是否过期
	if activationKey.ExpiresAt != nil && activationKey.ExpiresAt.Before(time.Now()) {
		s.db.Model(&activationKey).Update("status", "expired")
		return &domain.VerificationResponse{
			Success:   false,
			Message:   "Activation code expired",
			Timestamp: time.Now().Unix(),
			Code:      "CODE_EXPIRED",
		}, nil
	}

	// 检查使用次数
	if activationKey.MaxUses > 0 && activationKey.UsedCount >= activationKey.MaxUses {
		s.db.Model(&activationKey).Update("status", "used")
		return &domain.VerificationResponse{
			Success:   false,
			Message:   "Activation code usage limit exceeded",
			Timestamp: time.Now().Unix(),
			Code:      "CODE_LIMIT_EXCEEDED",
		}, nil
	}

	// 检查设备限制
	if activationKey.DeviceLimit != nil {
		var deviceCount int64
		s.db.Model(&domain.DeviceRecord{}).
			Where("activation_id = ? AND app_id = ?", activationKey.ID, req.AppID).
			Distinct("device_id").
			Count(&deviceCount)

		if int(deviceCount) >= *activationKey.DeviceLimit {
			// 检查当前设备是否已激活过
			var existingRecord domain.DeviceRecord
			queryErr := s.db.Where("device_id = ? AND app_id = ?", req.DeviceID, req.AppID).
				First(&existingRecord).Error

			if queryErr != nil && !errors.Is(queryErr, gorm.ErrRecordNotFound) {
				return nil, queryErr
			}

			if errors.Is(queryErr, gorm.ErrRecordNotFound) {
				return &domain.VerificationResponse{
					Success:   false,
					Message:   "Device limit exceeded",
					Timestamp: time.Now().Unix(),
					Code:      "DEVICE_LIMIT_EXCEEDED",
				}, nil
			}
		}
	}

	// 记录设备信息或更新激活计数
	now := time.Now()
	_, _ = json.Marshal(req.DeviceInfo) // 验证可序列化

	var deviceRecord domain.DeviceRecord
	result := s.db.Where("device_id = ? AND app_id = ?", req.DeviceID, req.AppID).
		First(&deviceRecord)

	if errors.Is(result.Error, gorm.ErrRecordNotFound) {
		// 新设备激活
		deviceRecord = domain.DeviceRecord{
			DeviceID:        req.DeviceID,
			AppID:           req.AppID,
			ActivationID:    &activationKey.ID,
			DeviceName:      extractDeviceInfo(req.DeviceInfo, "device_name"),
			Model:           extractDeviceInfo(req.DeviceInfo, "model"),
			OSVersion:       extractDeviceInfo(req.DeviceInfo, "os_version"),
			AppVersion:      extractDeviceInfo(req.DeviceInfo, "app_version"),
			ActivationCount: 1,
			Status:          "active",
		}
		s.db.Create(&deviceRecord)
	} else {
		// 更新现有设备记录
		s.db.Model(&deviceRecord).Updates(map[string]interface{}{
			"activation_count":  gorm.Expr("activation_count + 1"),
			"last_activated_at": &now,
		})
	}

	// 更新激活码使用状态
	s.db.Model(&activationKey).Updates(map[string]interface{}{
		"used_count": gorm.Expr("used_count + 1"),
		"used_at":    &now,
	})

	// 记录审计日志
	s.recordAuditLog(&activationKey, req, "verify", "success", "")

	// 获取剩余使用次数
	remainingUses := activationKey.MaxUses - (activationKey.UsedCount + 1)
	if remainingUses < 0 {
		remainingUses = 0
	}

	// 计算过期时间戳（毫秒）
	var expiresAt *int64
	if activationKey.ExpiresAt != nil {
		timestamp := activationKey.ExpiresAt.UnixMilli()
		expiresAt = &timestamp
	}

	// 获取设备使用数量
	var devicesUsed int64
	s.db.Model(&domain.DeviceRecord{}).
		Where("activation_id = ?", activationKey.ID).
		Distinct("device_id").
		Count(&devicesUsed)

	// 构建响应数据
	activationData := &domain.ActivationData{
		ActivationID:  activationKey.ID,
		DevicesUsed:   int(devicesUsed),
		DeviceLimit:   activationKey.DeviceLimit,
		ExpiresAt:     expiresAt,
		RemainingUses: remainingUses,
		CreatedAt:     activationKey.CreatedAt.Unix(),
	}

	// 生成签名
	timestamp := time.Now().Unix()
	signature := s.generateSignature(activationData, timestamp, secretKey)

	return &domain.VerificationResponse{
		Success:   true,
		Message:   "Activation successful",
		Data:      activationData,
		Signature: signature,
		Timestamp: timestamp,
	}, nil
}

// GenerateActivationCodes 生成激活码
func (s *ActivationService) GenerateActivationCodes(
	req *domain.GenerateRequest,
) (*domain.GenerateResponse, error) {
	var codes []domain.CodeItem

	for i := 0; i < req.Count; i++ {
		code := generateCode()

		// 计算过期时间
		var expiresAt *time.Time
		if req.ExpiresInDays != nil && *req.ExpiresInDays > 0 {
			expireTime := time.Now().AddDate(0, 0, *req.ExpiresInDays)
			expiresAt = &expireTime
		}

		// 创建激活码
		activation := domain.ActivationKey{
			Code:        code,
			AppID:       req.AppID,
			Status:      "active",
			MaxUses:     req.MaxUses,
			DeviceLimit: req.DeviceLimit,
			Notes:       req.Notes,
			ExpiresAt:   expiresAt,
		}

		if err := s.db.Create(&activation).Error; err != nil {
			log.Printf("Failed to create activation code: %v", err)
			continue
		}

		var expiresAtMs *int64
		if expiresAt != nil {
			timestamp := expiresAt.UnixMilli()
			expiresAtMs = &timestamp
		}

		codes = append(codes, domain.CodeItem{
			Code:      code,
			ID:        activation.ID,
			ExpiresAt: expiresAtMs,
		})
	}

	return &domain.GenerateResponse{
		Success:   true,
		Generated: len(codes),
		Codes:     codes,
	}, nil
}

// ListActivationCodes 列出激活码
func (s *ActivationService) ListActivationCodes(
	req *domain.ListRequest,
) (*domain.ListResponse, error) {
	var activations []domain.ActivationKey
	var total int64

	query := s.db.Where("app_id = ?", req.AppID)

	// 按状态筛选
	if req.Status != "" {
		query = query.Where("status = ?", req.Status)
	}

	// 计算总数
	if err := query.Model(&domain.ActivationKey{}).Count(&total).Error; err != nil {
		return nil, err
	}

	// 分页查询
	offset := (req.Page - 1) * req.Limit
	if err := query.Offset(offset).Limit(req.Limit).Find(&activations).Error; err != nil {
		return nil, err
	}

	return &domain.ListResponse{
		Success: true,
		Total:   total,
		Page:    req.Page,
		Limit:   req.Limit,
		Items:   activations,
	}, nil
}

// RevokeActivationCode 撤销激活码
func (s *ActivationService) RevokeActivationCode(appID, code string) error {
	return s.db.Model(&domain.ActivationKey{}).
		Where("app_id = ? AND code = ?", appID, code).
		Update("status", "revoked").Error
}

// recordAuditLog 记录审计日志
func (s *ActivationService) recordAuditLog(
	activation *domain.ActivationKey,
	req *domain.VerificationRequest,
	action, result, errorMsg string,
) {
	deviceInfo, _ := json.Marshal(req.DeviceInfo)

	auditLog := domain.AuditLog{
		Action:       action,
		ActivationID: &activation.ID,
		DeviceID:     req.DeviceID,
		Result:       result,
		ErrorMessage: errorMsg,
		DeviceInfo:   string(deviceInfo),
	}

	if err := s.db.Create(&auditLog).Error; err != nil {
		log.Printf("Failed to create audit log: %v", err)
	}
}

// generateSignature 生成响应签名
func (s *ActivationService) generateSignature(
	data *domain.ActivationData,
	timestamp int64,
	secretKey string,
) string {
	// 将数据序列化为 JSON
	dataJSON, _ := json.Marshal(data)

	// 构建签名数据：data + timestamp + secretKey
	signData := string(dataJSON) + fmt.Sprintf("%d", timestamp) + secretKey

	// 计算 HMAC-SHA256
	h := hmac.New(sha256.New, []byte(secretKey))
	h.Write([]byte(signData))

	return hex.EncodeToString(h.Sum(nil))
}

// generateCode 生成激活码 (XXXX-XXXX-XXXX-XXXX 格式)
func generateCode() string {
	uuid := uuid.New().String()
	parts := []string{
		uuid[0:4],
		uuid[5:9],
		uuid[10:14],
		uuid[15:19],
	}
	return fmt.Sprintf("%s-%s-%s-%s", parts[0], parts[1], parts[2], parts[3])
}

// extractDeviceInfo 从设备信息中提取字段
func extractDeviceInfo(deviceInfo map[string]interface{}, key string) string {
	if val, ok := deviceInfo[key]; ok {
		if strVal, ok := val.(string); ok {
			return strVal
		}
	}
	return ""
}
