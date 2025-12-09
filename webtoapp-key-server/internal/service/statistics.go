package service

import (
	"time"

	"github.com/yingcaihuang/webtoapp-key-server/internal/domain"
	"gorm.io/gorm"
)

// StatisticsService 统计服务
type StatisticsService struct {
	db *gorm.DB
}

// NewStatisticsService 创建统计服务实例
func NewStatisticsService(db *gorm.DB) *StatisticsService {
	return &StatisticsService{db: db}
}

// AggregateStatistics 聚合统计数据
func (s *StatisticsService) AggregateStatistics() error {
	// 这里需要根据实际的激活表结构来实现
	// 暂时使用示例数据
	return nil
}

// GetDailyStatistics 获取日统计数据
func (s *StatisticsService) GetDailyStatistics(days int) ([]domain.DailyStats, error) {
	var stats []domain.DailyStats
	startDate := time.Now().AddDate(0, 0, -days)

	if err := s.db.Where("date >= ?", startDate).Order("date DESC").Find(&stats).Error; err != nil {
		return nil, err
	}

	return stats, nil
}

// GetStatistics 获取统计信息
func (s *StatisticsService) GetStatistics() (map[string]interface{}, error) {
	var totalStats domain.Statistics
	var dailyStats []domain.DailyStats

	// 获取总体统计
	s.db.First(&totalStats)

	// 获取最近7天的数据
	dailyStats, _ = s.GetDailyStatistics(7)

	return map[string]interface{}{
		"total":      totalStats,
		"daily":      dailyStats,
		"last_7days": len(dailyStats),
	}, nil
}

// RecordActivation 记录激活事件
func (s *StatisticsService) RecordActivation(appID string) error {
	var stats domain.Statistics
	if err := s.db.Where("app_id = ?", appID).First(&stats).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			// 创建新的统计记录
			stats = domain.Statistics{
				AppID:                 appID,
				TotalActivations:      1,
				SuccessfulVerifications: 0,
				FailedVerifications:    0,
				TotalDevices:          1,
				ActiveCodes:           0,
				RevokedCodes:          0,
			}
			return s.db.Create(&stats).Error
		}
		return err
	}

	// 更新统计数据
	return s.db.Model(&stats).Updates(map[string]interface{}{
		"total_activations": gorm.Expr("total_activations + ?", 1),
		"total_devices":     gorm.Expr("total_devices + ?", 1),
	}).Error
}

// RecordVerification 记录验证事件
func (s *StatisticsService) RecordVerification(appID string, success bool) error {
	var stats domain.Statistics
	if err := s.db.Where("app_id = ?", appID).First(&stats).Error; err != nil {
		return err
	}

	updateMap := map[string]interface{}{}
	if success {
		updateMap["successful_verifications"] = gorm.Expr("successful_verifications + ?", 1)
	} else {
		updateMap["failed_verifications"] = gorm.Expr("failed_verifications + ?", 1)
	}

	return s.db.Model(&stats).Updates(updateMap).Error
}

// RecordDailyStats 记录日统计
func (s *StatisticsService) RecordDailyStats(appID string, verificationCount, successCount, failureCount, newDevices, codesGenerated, codesRevoked int) error {
	dailyStats := &domain.DailyStats{
		AppID:            appID,
		Date:             time.Now(),
		VerificationCount: verificationCount,
		SuccessCount:      successCount,
		FailureCount:      failureCount,
		NewDevices:        newDevices,
		CodesGenerated:    codesGenerated,
		CodesRevoked:      codesRevoked,
	}

	return s.db.Create(dailyStats).Error
}

// GetAppStatistics 获取应用统计信息
func (s *StatisticsService) GetAppStatistics(appID string) (*domain.Statistics, error) {
	var stats domain.Statistics
	if err := s.db.Where("app_id = ?", appID).First(&stats).Error; err != nil {
		return nil, err
	}
	return &stats, nil
}

// GetTopApps 获取排名前N的应用
func (s *StatisticsService) GetTopApps(limit int) ([]domain.Statistics, error) {
	var stats []domain.Statistics
	if err := s.db.Order("total_activations DESC").Limit(limit).Find(&stats).Error; err != nil {
		return nil, err
	}
	return stats, nil
}

// GetTrendData 获取趋势数据
func (s *StatisticsService) GetTrendData(appID string, days int) ([]domain.DailyStats, error) {
	var stats []domain.DailyStats
	startDate := time.Now().AddDate(0, 0, -days)

	if err := s.db.Where("app_id = ? AND date >= ?", appID, startDate).
		Order("date ASC").
		Find(&stats).Error; err != nil {
		return nil, err
	}

	return stats, nil
}

// ExportStatistics 导出统计数据
func (s *StatisticsService) ExportStatistics(startDate, endDate time.Time) (interface{}, error) {
	var stats []domain.Statistics
	var dailyStats []domain.DailyStats

	s.db.Find(&stats)
	s.db.Where("date BETWEEN ? AND ?", startDate, endDate).Find(&dailyStats)

	return map[string]interface{}{
		"total_stats": stats,
		"daily_stats": dailyStats,
		"export_time": time.Now(),
	}, nil
}
