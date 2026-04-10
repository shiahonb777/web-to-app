package main

// Fiber 性能分析面板 — 精美的 Go Web 应用示例
import (
	"fmt"
	"math/rand"
	"os"
	"runtime"
	"time"

	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/fiber/v2/middleware/logger"
)

var startTime = time.Now()

func main() {
	app := fiber.New(fiber.Config{AppName: "Fiber Dashboard v2.0", DisableStartupMessage: true})
	app.Use(logger.New())

	app.Get("/", dashboardHandler)
	app.Get("/api/users", func(c *fiber.Ctx) error {
		return c.JSON([]fiber.Map{
			{"id": 1, "name": "Alice", "role": "Admin"},
			{"id": 2, "name": "Bob", "role": "User"},
			{"id": 3, "name": "Carol", "role": "Editor"},
		})
	})
	app.Get("/api/health", func(c *fiber.Ctx) error {
		var m runtime.MemStats
		runtime.ReadMemStats(&m)
		return c.JSON(fiber.Map{
			"status":    "healthy",
			"framework": "Fiber",
			"uptime":    time.Since(startTime).String(),
			"memory_mb": m.Alloc / 1024 / 1024,
			"goroutines": runtime.NumGoroutine(),
		})
	})

	port := os.Getenv("PORT")
	if port == "" {
		port = "3000"
	}
	app.Listen(":" + port)
}

func dashboardHandler(c *fiber.Ctx) error {
	uptime := time.Since(startTime).Round(time.Second)
	var m runtime.MemStats
	runtime.ReadMemStats(&m)
	memMB := m.Alloc / 1024 / 1024
	goroutines := runtime.NumGoroutine()

	bars := ""
	days := []string{"周一", "周二", "周三", "周四", "周五", "周六", "周日"}
	for i, d := range days {
		h := 25 + rand.Intn(65)
		bars += fmt.Sprintf(`<div class="bg"><div class="b" style="height:%d%%;animation-delay:%.2fs"></div><div class="bl">%s</div></div>`, h, 0.3+float64(i)*0.08, d)
	}

	metrics := []struct{ label, value, icon, color string }{
		{"响应时间", "<1ms", "⚡", "#f59e0b"},
		{"内存使用", fmt.Sprintf("%dMB", memMB), "💾", "#8b5cf6"},
		{"协程数", fmt.Sprintf("%d", goroutines), "🔄", "#06b6d4"},
		{"QPS", fmt.Sprintf("%d", 1000+rand.Intn(4000)), "📊", "#ec4899"},
	}
	metricHTML := ""
	for _, met := range metrics {
		metricHTML += fmt.Sprintf(`<div class="perf"><div class="perf-i" style="background:%s18">%s</div><div class="perf-info"><div class="perf-l">%s</div><div class="perf-v" style="color:%s">%s</div></div></div>`, met.color, met.icon, met.label, met.color, met.value)
	}

	html := fmt.Sprintf(`<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0,user-scalable=no"><title>Fiber Dashboard</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
:root{--bg:#080816;--card:rgba(255,255,255,0.035);--border:rgba(255,255,255,0.07);--text:#f0f0ff;--text2:rgba(240,240,255,0.55);--text3:rgba(240,240,255,0.3);--teal:#14b8a6;--radius:14px}
body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;background:var(--bg);color:var(--text);min-height:100vh}
body::before{content:'';position:fixed;top:-40%%;left:-40%%;width:180%%;height:180%%;background:radial-gradient(ellipse at 50%% 30%%,rgba(20,184,166,0.06) 0%%,transparent 55%%);pointer-events:none}
.c{max-width:460px;margin:0 auto;padding:0 14px 32px;position:relative;z-index:1}
.hdr{padding:20px 2px;display:flex;justify-content:space-between;align-items:center}
.hdr h1{font-size:22px;font-weight:800;background:linear-gradient(135deg,#14b8a6,#2dd4bf);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
.hdr p{font-size:12px;color:var(--text2);margin-top:2px}
.badge{display:inline-flex;align-items:center;gap:5px;font-size:11px;font-weight:600;padding:5px 12px;border-radius:20px;background:rgba(20,184,166,0.1);color:var(--teal);border:1px solid rgba(20,184,166,0.15)}
.badge .dot{width:7px;height:7px;border-radius:50%%;background:#00d4aa;box-shadow:0 0 10px #00d4aa;animation:p 2s infinite}
@keyframes p{0%%,100%%{opacity:1}50%%{opacity:.5}}
.row{display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px;margin-bottom:16px}
.m{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:14px;text-align:center}
.m-val{font-size:20px;font-weight:800;margin-bottom:2px}
.m-label{font-size:10px;color:var(--text2);text-transform:uppercase;letter-spacing:.5px;font-weight:500}
.m:nth-child(1) .m-val{color:#00d4aa}.m:nth-child(2) .m-val{color:var(--teal)}.m:nth-child(3) .m-val{color:#8b5cf6}
.card{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:18px;margin-bottom:14px}
.card-h{display:flex;justify-content:space-between;align-items:center;margin-bottom:14px}
.card-t{font-size:15px;font-weight:700}.card-b{font-size:10px;font-weight:600;padding:3px 9px;border-radius:16px;background:rgba(20,184,166,0.1);color:var(--teal)}
.perf{display:flex;align-items:center;gap:12px;padding:10px 0;border-bottom:1px solid rgba(255,255,255,0.03)}.perf:last-child{border:none}
.perf-i{width:36px;height:36px;border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:16px;flex-shrink:0}
.perf-info{flex:1}.perf-l{font-size:12px;color:var(--text2)}.perf-v{font-size:16px;font-weight:700}
.chart{display:flex;align-items:flex-end;justify-content:space-between;height:120px;gap:6px}
.bg{flex:1;display:flex;flex-direction:column;align-items:center;gap:4px;height:100%%;justify-content:flex-end}
.b{width:100%%;max-width:28px;border-radius:5px 5px 3px 3px;background:linear-gradient(180deg,#2dd4bf,#0d9488);box-shadow:0 0 8px rgba(20,184,166,0.2);transform-origin:bottom;animation:grow .7s cubic-bezier(.34,1.56,.64,1) backwards}
@keyframes grow{from{transform:scaleY(0)}to{transform:scaleY(1)}}.bl{font-size:9px;color:var(--text3);font-weight:500}
.try{background:rgba(20,184,166,0.1);color:var(--teal);border:1px solid rgba(20,184,166,0.2);padding:6px 14px;border-radius:8px;font-size:12px;font-weight:600;cursor:pointer}.try:active{transform:scale(.95)}
.result{margin-top:10px;padding:12px;background:rgba(0,0,0,0.3);border-radius:8px;font-family:monospace;font-size:11px;color:#00d4aa;max-height:180px;overflow-y:auto;display:none;white-space:pre-wrap;word-break:break-all}
.ft{text-align:center;padding:12px}.ft-b{display:inline-flex;align-items:center;gap:6px;font-size:10px;font-weight:600;color:var(--text3);padding:5px 12px;border-radius:20px;background:rgba(20,184,166,0.04);border:1px solid rgba(20,184,166,0.08)}
@keyframes fadeIn{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:translateY(0)}}.m,.card{animation:fadeIn .5s ease backwards}
</style></head><body>
<div class="c">
<div class="hdr"><div><h1>🔥 Fiber Dashboard</h1><p>超快 Go Web 框架</p></div><div class="badge"><span class="dot"></span>运行中</div></div>
<div class="row">
<div class="m"><div class="m-val">%s</div><div class="m-label">运行时间</div></div>
<div class="m"><div class="m-val">%dMB</div><div class="m-label">内存</div></div>
<div class="m"><div class="m-val">%d</div><div class="m-label">协程</div></div>
</div>
<div class="card"><div class="card-h"><span class="card-t">性能指标</span><span class="card-b">⚡ 实时</span></div>%s</div>
<div class="card"><div class="card-h"><span class="card-t">吞吐趋势</span><span class="card-b">📊 本周</span></div><div class="chart">%s</div></div>
<div class="card"><div class="card-h"><span class="card-t">在线测试</span><span class="card-b">🧪 交互</span></div>
<div style="display:flex;gap:8px"><button class="try" onclick="tryApi('/api/users')">👥 用户列表</button><button class="try" onclick="tryApi('/api/health')">💚 健康检查</button></div>
<div class="result" id="r"></div></div>
<div class="ft"><div class="ft-b"><span class="dot" style="width:5px;height:5px;border-radius:50%%;background:#00d4aa;box-shadow:0 0 6px #00d4aa"></span>Fiber v2 · Go %s · 运行中</div></div>
</div>
<script>function tryApi(u){var r=document.getElementById('r');r.style.display='block';r.textContent='⏳ ...';fetch(u).then(function(x){return x.json()}).then(function(d){r.textContent=JSON.stringify(d,null,2)}).catch(function(e){r.textContent='❌ '+e.message})}</script>
</body></html>`, uptime, memMB, goroutines, metricHTML, bars, runtime.Version())

	c.Set("Content-Type", "text/html; charset=utf-8")
	return c.SendString(html)
}
