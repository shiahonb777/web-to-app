package main

// Gin REST API Dashboard — Go Web App Demo (EN)
import (
	"fmt"
	"math/rand"
	"net/http"
	"os"
	"time"
	"github.com/gin-gonic/gin"
)

type Item struct {
	ID    int     `json:"id"`
	Name  string  `json:"name"`
	Price float64 `json:"price"`
}

var items = []Item{{1, "MacBook Pro", 1999.00}, {2, "iPhone 15", 999.00}, {3, "AirPods Pro", 249.00}, {4, "iPad Air", 599.00}}
var startTime = time.Now()

func main() {
	gin.SetMode(gin.ReleaseMode)
	r := gin.Default()
	r.GET("/", func(c *gin.Context) { c.Header("Content-Type", "text/html; charset=utf-8"); c.String(http.StatusOK, dashboard()) })
	api := r.Group("/api")
	{
		api.GET("/items", func(c *gin.Context) { c.JSON(http.StatusOK, gin.H{"status": "ok", "data": items}) })
		api.GET("/health", func(c *gin.Context) { c.JSON(http.StatusOK, gin.H{"status": "healthy", "uptime": time.Since(startTime).String()}) })
	}
	port := os.Getenv("PORT"); if port == "" { port = "8080" }
	r.Run(":" + port)
}

func dashboard() string {
	up := time.Since(startTime).Round(time.Second)
	bars := ""
	days := []string{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}
	for i, d := range days {
		h := 20 + rand.Intn(70)
		bars += fmt.Sprintf(`<div class="bg"><div class="b" style="height:%d%%;animation-delay:%.2fs"></div><div class="bl">%s</div></div>`, h, 0.3+float64(i)*0.08, d)
	}
	return fmt.Sprintf(`<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0,user-scalable=no"><title>Gin Dashboard</title>
<style>*{margin:0;padding:0;box-sizing:border-box}:root{--bg:#080818;--card:rgba(255,255,255,0.035);--border:rgba(255,255,255,0.07);--text:#f0f0ff;--text2:rgba(240,240,255,0.55);--text3:rgba(240,240,255,0.3);--blue:#00b4d8;--radius:14px}body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;background:var(--bg);color:var(--text);min-height:100vh}body::before{content:'';position:fixed;top:-40%%;left:-40%%;width:180%%;height:180%%;background:radial-gradient(ellipse at 50%% 30%%,rgba(0,180,216,0.06) 0%%,transparent 55%%);pointer-events:none}.c{max-width:460px;margin:0 auto;padding:0 14px 32px;position:relative;z-index:1}.hdr{padding:20px 2px;display:flex;justify-content:space-between;align-items:center}.hdr h1{font-size:22px;font-weight:800;background:linear-gradient(135deg,#00b4d8,#48cae4);-webkit-background-clip:text;-webkit-text-fill-color:transparent}.hdr p{font-size:12px;color:var(--text2);margin-top:2px}.badge{display:inline-flex;align-items:center;gap:5px;font-size:11px;font-weight:600;padding:5px 12px;border-radius:20px;background:rgba(0,180,216,0.1);color:var(--blue);border:1px solid rgba(0,180,216,0.15)}.badge .dot{width:7px;height:7px;border-radius:50%%;background:var(--blue);box-shadow:0 0 10px var(--blue);animation:p 2s infinite}@keyframes p{0%%,100%%{opacity:1}50%%{opacity:.5}}.row{display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px;margin-bottom:16px}.m{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:14px;text-align:center}.m-val{font-size:20px;font-weight:800;margin-bottom:2px}.m-label{font-size:10px;color:var(--text2);text-transform:uppercase;letter-spacing:.5px}.m:nth-child(1) .m-val{color:#00d4aa}.m:nth-child(2) .m-val{color:var(--blue)}.m:nth-child(3) .m-val{color:#8b5cf6}.card{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:18px;margin-bottom:14px}.card-h{display:flex;justify-content:space-between;align-items:center;margin-bottom:14px}.card-t{font-size:15px;font-weight:700}.card-b{font-size:10px;font-weight:600;padding:3px 9px;border-radius:16px;background:rgba(0,180,216,0.1);color:var(--blue)}.chart{display:flex;align-items:flex-end;justify-content:space-between;height:130px;gap:6px}.bg{flex:1;display:flex;flex-direction:column;align-items:center;gap:4px;height:100%%;justify-content:flex-end}.b{width:100%%;max-width:28px;border-radius:5px 5px 3px 3px;background:linear-gradient(180deg,#48cae4,#0096c7);box-shadow:0 0 10px rgba(0,180,216,0.2);transform-origin:bottom;animation:grow .7s cubic-bezier(.34,1.56,.64,1) backwards}@keyframes grow{from{transform:scaleY(0)}to{transform:scaleY(1)}}.bl{font-size:9px;color:var(--text3);font-weight:500}.try{background:rgba(0,180,216,0.1);color:var(--blue);border:1px solid rgba(0,180,216,0.2);padding:6px 14px;border-radius:8px;font-size:12px;font-weight:600;cursor:pointer}.result{margin-top:10px;padding:12px;background:rgba(0,0,0,0.3);border-radius:8px;font-family:monospace;font-size:11px;color:#00d4aa;max-height:180px;overflow-y:auto;display:none;white-space:pre-wrap;word-break:break-all}.ft{text-align:center;padding:12px}.ft-b{display:inline-flex;align-items:center;gap:6px;font-size:10px;font-weight:600;color:var(--text3);padding:5px 12px;border-radius:20px;background:rgba(0,180,216,0.04);border:1px solid rgba(0,180,216,0.08)}@keyframes fadeIn{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:translateY(0)}}.m,.card{animation:fadeIn .5s ease backwards}</style></head><body>
<div class="c"><div class="hdr"><div><h1>🚀 Gin Dashboard</h1><p>High-Performance Go Web Framework</p></div><div class="badge"><span class="dot"></span>Running</div></div>
<div class="row"><div class="m"><div class="m-val">%s</div><div class="m-label">Uptime</div></div><div class="m"><div class="m-val">%d</div><div class="m-label">Items</div></div><div class="m"><div class="m-val">4</div><div class="m-label">Routes</div></div></div>
<div class="card"><div class="card-h"><span class="card-t">Traffic Trends</span><span class="card-b">📊 Weekly</span></div><div class="chart">%s</div></div>
<div class="card"><div class="card-h"><span class="card-t">Live Testing</span><span class="card-b">🧪 Try It</span></div><div style="display:flex;gap:8px"><button class="try" onclick="tryApi('/api/items')">📦 Items</button><button class="try" onclick="tryApi('/api/health')">💚 Health</button></div><div class="result" id="r"></div></div>
<div class="ft"><div class="ft-b"><span class="dot" style="width:5px;height:5px;border-radius:50%%;background:var(--blue);box-shadow:0 0 6px var(--blue)"></span>Gin · Go 1.22 · Running</div></div></div>
<script>function tryApi(u){var r=document.getElementById('r');r.style.display='block';r.textContent='⏳ ...';fetch(u).then(function(x){return x.json()}).then(function(d){r.textContent=JSON.stringify(d,null,2)}).catch(function(e){r.textContent='❌ '+e.message})}</script>
</body></html>`, up, len(items), bars)
}
