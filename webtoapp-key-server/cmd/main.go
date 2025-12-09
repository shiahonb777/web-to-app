package main

import (
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/yingcaihuang/webtoapp-key-server/internal/api"
	"github.com/yingcaihuang/webtoapp-key-server/internal/config"
	"github.com/yingcaihuang/webtoapp-key-server/internal/database"
)

func main() {
	// 加载配置
	cfg := config.Load()

	// 初始化数据库
	if err := database.Init(cfg); err != nil {
		log.Fatalf("Failed to initialize database: %v", err)
	}
	defer database.Close()

	// 创建路由
	router := api.SetupRouter(cfg, database.DB)

	// 启动服务器
	addr := fmt.Sprintf(":%s", cfg.Port)
	log.Printf("🚀 WebToApp Key Server starting on http://localhost:%s", cfg.Port)
	log.Printf("📊 Environment: %s", cfg.Env)
	log.Printf("🗄️  Database: %s", cfg.DatabasePath)

	// 监听关闭信号
	go func() {
		sigChan := make(chan os.Signal, 1)
		signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)
		<-sigChan
		log.Println("⛔ Server shutting down...")
		os.Exit(0)
	}()

	// 启动 HTTP 服务器
	if err := router.Run(addr); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}
