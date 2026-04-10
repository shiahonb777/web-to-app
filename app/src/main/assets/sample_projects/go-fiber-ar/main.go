package main
// Fiber Performance Dashboard (AR)
import ("fmt";"math/rand";"os";"runtime";"time";"github.com/gofiber/fiber/v2";"github.com/gofiber/fiber/v2/middleware/logger")
var startTime = time.Now()
func main() {
	app := fiber.New(fiber.Config{AppName:"Fiber Dashboard",DisableStartupMessage:true}); app.Use(logger.New())
	app.Get("/", func(c *fiber.Ctx) error { c.Set("Content-Type","text/html; charset=utf-8"); return c.SendString(dash()) })
	app.Get("/api/users", func(c *fiber.Ctx) error { return c.JSON([]fiber.Map{{"id":1,"name":"أحمد"},{"id":2,"name":"فاطمة"},{"id":3,"name":"محمد"}}) })
	app.Get("/api/health", func(c *fiber.Ctx) error { var m runtime.MemStats; runtime.ReadMemStats(&m); return c.JSON(fiber.Map{"status":"healthy","memory_mb":m.Alloc/1024/1024}) })
	port := os.Getenv("PORT"); if port == "" { port = "3000" }; app.Listen(":"+port)
}
func dash() string {
	up := time.Since(startTime).Round(time.Second); var m runtime.MemStats; runtime.ReadMemStats(&m); mb := m.Alloc/1024/1024; gr := runtime.NumGoroutine()
	bars := ""; days := []string{"إثنين","ثلاثاء","أربعاء","خميس","جمعة","سبت","أحد"}
	for i,d := range days { h:=25+rand.Intn(65); bars+=fmt.Sprintf(`<div style="flex:1;display:flex;flex-direction:column;align-items:center;gap:4px;height:100%%;justify-content:flex-end"><div style="width:100%%;max-width:28px;height:%d%%;border-radius:5px 5px 3px 3px;background:linear-gradient(180deg,#2dd4bf,#0d9488);transform-origin:bottom;animation:g .7s cubic-bezier(.34,1.56,.64,1) backwards;animation-delay:%.2fs"></div><div style="font-size:9px;color:rgba(240,240,255,0.3)">%s</div></div>`,h,0.3+float64(i)*0.08,d) }
	mets := []struct{l,v,i,c string}{{"زمن الاستجابة","<1ms","⚡","#f59e0b"},{"الذاكرة",fmt.Sprintf("%dMB",mb),"💾","#8b5cf6"},{"الخيوط",fmt.Sprintf("%d",gr),"🔄","#06b6d4"},{"QPS",fmt.Sprintf("%d",1000+rand.Intn(4000)),"📊","#ec4899"}}
	mh := ""; for _,mt := range mets { mh += fmt.Sprintf(`<div style="display:flex;align-items:center;gap:12px;padding:10px 0;border-bottom:1px solid rgba(255,255,255,0.03)"><div style="width:36px;height:36px;border-radius:10px;background:%s18;display:flex;align-items:center;justify-content:center;font-size:16px">%s</div><div style="flex:1"><div style="font-size:12px;color:rgba(240,240,255,0.55)">%s</div><div style="font-size:16px;font-weight:700;color:%s">%s</div></div></div>`,mt.c,mt.i,mt.l,mt.c,mt.v) }
	return fmt.Sprintf(`<!DOCTYPE html><html lang="ar" dir="rtl"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0,user-scalable=no"><title>Fiber</title><style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,Tahoma,sans-serif;background:#080816;color:#f0f0ff;min-height:100vh;direction:rtl}@keyframes g{from{transform:scaleY(0)}to{transform:scaleY(1)}}</style></head><body>
<div style="max-width:460px;margin:0 auto;padding:0 14px 32px">
<div style="padding:20px 2px;display:flex;justify-content:space-between;align-items:center"><div><h1 style="font-size:22px;font-weight:800;background:linear-gradient(135deg,#14b8a6,#2dd4bf);-webkit-background-clip:text;-webkit-text-fill-color:transparent">🔥 Fiber لوحة</h1><p style="font-size:12px;color:rgba(240,240,255,0.55)">إطار عمل Go فائق السرعة</p></div><div style="font-size:11px;font-weight:600;padding:5px 12px;border-radius:20px;background:rgba(20,184,166,0.1);color:#14b8a6">يعمل</div></div>
<div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px;margin-bottom:16px"><div style="background:rgba(255,255,255,0.035);border:1px solid rgba(255,255,255,0.07);border-radius:14px;padding:14px;text-align:center"><div style="font-size:20px;font-weight:800;color:#00d4aa">%s</div><div style="font-size:10px;color:rgba(240,240,255,0.55)">التشغيل</div></div><div style="background:rgba(255,255,255,0.035);border:1px solid rgba(255,255,255,0.07);border-radius:14px;padding:14px;text-align:center"><div style="font-size:20px;font-weight:800;color:#14b8a6">%dMB</div><div style="font-size:10px;color:rgba(240,240,255,0.55)">الذاكرة</div></div><div style="background:rgba(255,255,255,0.035);border:1px solid rgba(255,255,255,0.07);border-radius:14px;padding:14px;text-align:center"><div style="font-size:20px;font-weight:800;color:#8b5cf6">%d</div><div style="font-size:10px;color:rgba(240,240,255,0.55)">الخيوط</div></div></div>
<div style="background:rgba(255,255,255,0.035);border:1px solid rgba(255,255,255,0.07);border-radius:14px;padding:18px;margin-bottom:14px"><div style="font-size:15px;font-weight:700;margin-bottom:14px">مقاييس الأداء</div>%s</div>
<div style="background:rgba(255,255,255,0.035);border:1px solid rgba(255,255,255,0.07);border-radius:14px;padding:18px;margin-bottom:14px"><div style="font-size:15px;font-weight:700;margin-bottom:14px">الإنتاجية</div><div style="display:flex;align-items:flex-end;justify-content:space-between;height:120px;gap:6px;direction:ltr">%s</div></div>
<div style="text-align:center;padding:12px;font-size:10px;color:rgba(240,240,255,0.3)">Fiber v2 · Go · يعمل</div>
</div></body></html>`,up,mb,gr,mh,bars)
}
