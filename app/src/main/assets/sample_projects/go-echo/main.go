package main

// Echo 服务监控面板 — 精美的 Go Web 应用示例
import (
	"fmt"
	"math/rand"
	"net/http"
	"os"
	"time"

	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
)

type Status struct {
	Service string `json:"service"`
	Status  string `json:"status"`
	Uptime  string `json:"uptime"`
}

var startTime = time.Now()

func main() {
	e := echo.New()
	e.HideBanner = true
	e.Use(middleware.Logger())

	e.GET("/", dashboardHandler)
	e.GET("/health", func(c echo.Context) error {
		return c.JSON(http.StatusOK, Status{"echo-dashboard", "healthy", time.Since(startTime).String()})
	})
	e.GET("/api/services", func(c echo.Context) error {
		return c.JSON(http.StatusOK, map[string]interface{}{
			"services": []map[string]interface{}{
				{"name": "API Gateway", "status": "running", "latency": "2ms"},
				{"name": "Database", "status": "running", "latency": "5ms"},
				{"name": "Cache", "status": "running", "latency": "<1ms"},
				{"name": "Queue", "status": "running", "latency": "3ms"},
			},
		})
	})

	port := os.Getenv("PORT")
	if port == "" {
		port = "1323"
	}
	e.Logger.Fatal(e.Start(":" + port))
}

func dashboardHandler(c echo.Context) error {
	uptime := time.Since(startTime).Round(time.Second)
	services := []struct{ name, status, latency, icon, color string }{
		{"API Gateway", "运行中", "2ms", "🌐", "#00d4aa"},
		{"Database", "运行中", "5ms", "🗄️", "#3b82f6"},
		{"Redis Cache", "运行中", "<1ms", "⚡", "#f59e0b"},
		{"Message Queue", "运行中", "3ms", "📨", "#8b5cf6"},
		{"Auth Service", "运行中", "4ms", "🔐", "#ec4899"},
		{"Storage", "运行中", "8ms", "💾", "#06b6d4"},
	}
	svcHTML := ""
	for _, s := range services {
		svcHTML += fmt.Sprintf(`<div class="svc"><div class="svc-i" style="background:%s18">%s</div><div class="svc-info"><div class="svc-n">%s</div><div class="svc-s" style="color:%s">● %s</div></div><div class="svc-l">%s</div></div>`, s.color, s.icon, s.name, s.color, s.status, s.latency)
	}
	bars := ""
	days := []string{"周一", "周二", "周三", "周四", "周五", "周六", "周日"}
	for i, d := range days {
		h := 30 + rand.Intn(60)
		bars += fmt.Sprintf(`<div class="bg"><div class="b" style="height:%d%%;animation-delay:%.2fs"></div><div class="bl">%s</div></div>`, h, 0.3+float64(i)*0.08, d)
	}

	html := fmt.Sprintf(`<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0,user-scalable=no"><title>Echo Monitor</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
:root{--bg:#080816;--card:rgba(255,255,255,0.035);--border:rgba(255,255,255,0.07);--text:#f0f0ff;--text2:rgba(240,240,255,0.55);--text3:rgba(240,240,255,0.3);--cyan:#06b6d4;--radius:14px}
body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;background:var(--bg);color:var(--text);min-height:100vh}
body::before{content:'';position:fixed;top:-40%%;left:-40%%;width:180%%;height:180%%;background:radial-gradient(ellipse at 50%% 30%%,rgba(6,182,212,0.06) 0%%,transparent 55%%);pointer-events:none}
.c{max-width:460px;margin:0 auto;padding:0 14px 32px;position:relative;z-index:1}
.hdr{padding:20px 2px;display:flex;justify-content:space-between;align-items:center}
.hdr h1{font-size:22px;font-weight:800;background:linear-gradient(135deg,#06b6d4,#22d3ee);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
.hdr p{font-size:12px;color:var(--text2);margin-top:2px}
.badge{display:inline-flex;align-items:center;gap:5px;font-size:11px;font-weight:600;padding:5px 12px;border-radius:20px;background:rgba(6,182,212,0.1);color:var(--cyan);border:1px solid rgba(6,182,212,0.15)}
.badge .dot{width:7px;height:7px;border-radius:50%%;background:#00d4aa;box-shadow:0 0 10px #00d4aa;animation:p 2s infinite}
@keyframes p{0%%,100%%{opacity:1}50%%{opacity:.5}}
.row{display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px;margin-bottom:16px}
.m{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:14px;text-align:center}
.m-val{font-size:20px;font-weight:800;margin-bottom:2px}
.m-label{font-size:10px;color:var(--text2);text-transform:uppercase;letter-spacing:.5px;font-weight:500}
.m:nth-child(1) .m-val{color:#00d4aa}.m:nth-child(2) .m-val{color:var(--cyan)}.m:nth-child(3) .m-val{color:#8b5cf6}
.card{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:18px;margin-bottom:14px}
.card-h{display:flex;justify-content:space-between;align-items:center;margin-bottom:14px}
.card-t{font-size:15px;font-weight:700}.card-b{font-size:10px;font-weight:600;padding:3px 9px;border-radius:16px;background:rgba(6,182,212,0.1);color:var(--cyan)}
.svc{display:flex;align-items:center;gap:12px;padding:10px 0;border-bottom:1px solid rgba(255,255,255,0.03)}.svc:last-child{border:none}
.svc-i{width:36px;height:36px;border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:16px;flex-shrink:0}
.svc-info{flex:1}.svc-n{font-size:13px;font-weight:600}.svc-s{font-size:11px;font-weight:500}
.svc-l{font-size:11px;color:var(--text3);font-family:monospace;font-weight:600}
.chart{display:flex;align-items:flex-end;justify-content:space-between;height:120px;gap:6px}
.bg{flex:1;display:flex;flex-direction:column;align-items:center;gap:4px;height:100%%;justify-content:flex-end}
.b{width:100%%;max-width:28px;border-radius:5px 5px 3px 3px;background:linear-gradient(180deg,#22d3ee,#0891b2);box-shadow:0 0 8px rgba(6,182,212,0.2);transform-origin:bottom;animation:grow .7s cubic-bezier(.34,1.56,.64,1) backwards}
@keyframes grow{from{transform:scaleY(0)}to{transform:scaleY(1)}}.bl{font-size:9px;color:var(--text3);font-weight:500}
.ft{text-align:center;padding:12px}.ft-b{display:inline-flex;align-items:center;gap:6px;font-size:10px;font-weight:600;color:var(--text3);padding:5px 12px;border-radius:20px;background:rgba(6,182,212,0.04);border:1px solid rgba(6,182,212,0.08)}
@keyframes fadeIn{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:translateY(0)}}.m,.card{animation:fadeIn .5s ease backwards}
</style></head><body>
<div class="c">
<div class="hdr"><div><h1>📡 Echo Monitor</h1><p>服务监控面板</p></div><div class="badge"><span class="dot"></span>全部正常</div></div>
<div class="row">
<div class="m"><div class="m-val">%s</div><div class="m-label">运行时间</div></div>
<div class="m"><div class="m-val">6</div><div class="m-label">服务数</div></div>
<div class="m"><div class="m-val">99.9%%</div><div class="m-label">可用性</div></div>
</div>
<div class="card"><div class="card-h"><span class="card-t">服务状态</span><span class="card-b">🟢 全部在线</span></div>%s</div>
<div class="card"><div class="card-h"><span class="card-t">流量趋势</span><span class="card-b">📊 本周</span></div><div class="chart">%s</div></div>
<div class="ft"><div class="ft-b"><span class="dot" style="width:5px;height:5px;border-radius:50%%;background:#00d4aa;box-shadow:0 0 6px #00d4aa"></span>Echo v4 · Go 1.22 · 运行中</div></div>
</div></body></html>`, uptime, svcHTML, bars)

	return c.HTML(http.StatusOK, html)
}
