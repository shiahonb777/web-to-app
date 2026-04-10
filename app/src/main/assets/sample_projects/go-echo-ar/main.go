package main
// Echo Service Monitor (AR)
import ("fmt";"math/rand";"net/http";"os";"time";"github.com/labstack/echo/v4";"github.com/labstack/echo/v4/middleware")
var startTime = time.Now()
func main() {
	e := echo.New(); e.HideBanner = true; e.Use(middleware.Logger())
	e.GET("/", func(c echo.Context) error { return c.HTML(http.StatusOK, dash()) })
	e.GET("/health", func(c echo.Context) error { return c.JSON(http.StatusOK, map[string]interface{}{"status":"healthy"}) })
	port := os.Getenv("PORT"); if port == "" { port = "1323" }; e.Logger.Fatal(e.Start(":"+port))
}
func dash() string {
	up := time.Since(startTime).Round(time.Second)
	svcs := []struct{n,s,i,c string}{{"API Gateway","يعمل","🌐","#00d4aa"},{"قاعدة البيانات","يعمل","🗄️","#3b82f6"},{"التخزين المؤقت","يعمل","⚡","#f59e0b"},{"قائمة الرسائل","يعمل","📨","#8b5cf6"},{"خدمة المصادقة","يعمل","🔐","#ec4899"},{"التخزين","يعمل","💾","#06b6d4"}}
	sh := ""; for _,s := range svcs { sh += fmt.Sprintf(`<div style="display:flex;align-items:center;gap:12px;padding:10px 0;border-bottom:1px solid rgba(255,255,255,0.03)"><div style="width:36px;height:36px;border-radius:10px;background:%s18;display:flex;align-items:center;justify-content:center;font-size:16px">%s</div><div style="flex:1"><div style="font-size:13px;font-weight:600">%s</div><div style="font-size:11px;color:%s">● %s</div></div></div>`,s.c,s.i,s.n,s.c,s.s) }
	bars := ""; days := []string{"إثنين","ثلاثاء","أربعاء","خميس","جمعة","سبت","أحد"}
	for i,d := range days { h:=30+rand.Intn(60); bars+=fmt.Sprintf(`<div style="flex:1;display:flex;flex-direction:column;align-items:center;gap:4px;height:100%%;justify-content:flex-end"><div style="width:100%%;max-width:28px;height:%d%%;border-radius:5px 5px 3px 3px;background:linear-gradient(180deg,#22d3ee,#0891b2);transform-origin:bottom;animation:grow .7s cubic-bezier(.34,1.56,.64,1) backwards;animation-delay:%.2fs"></div><div style="font-size:9px;color:rgba(240,240,255,0.3)">%s</div></div>`,h,0.3+float64(i)*0.08,d) }
	return fmt.Sprintf(`<!DOCTYPE html><html lang="ar" dir="rtl"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0,user-scalable=no"><title>Echo</title><style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,Tahoma,sans-serif;background:#080816;color:#f0f0ff;min-height:100vh;direction:rtl}@keyframes grow{from{transform:scaleY(0)}to{transform:scaleY(1)}}</style></head><body>
<div style="max-width:460px;margin:0 auto;padding:0 14px 32px">
<div style="padding:20px 2px;display:flex;justify-content:space-between;align-items:center"><div><h1 style="font-size:22px;font-weight:800;background:linear-gradient(135deg,#06b6d4,#22d3ee);-webkit-background-clip:text;-webkit-text-fill-color:transparent">📡 Echo مراقب</h1><p style="font-size:12px;color:rgba(240,240,255,0.55)">لوحة مراقبة الخدمات</p></div><div style="font-size:11px;font-weight:600;padding:5px 12px;border-radius:20px;background:rgba(6,182,212,0.1);color:#06b6d4">الكل يعمل</div></div>
<div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px;margin-bottom:16px"><div style="background:rgba(255,255,255,0.035);border:1px solid rgba(255,255,255,0.07);border-radius:14px;padding:14px;text-align:center"><div style="font-size:20px;font-weight:800;color:#00d4aa">%s</div><div style="font-size:10px;color:rgba(240,240,255,0.55)">وقت التشغيل</div></div><div style="background:rgba(255,255,255,0.035);border:1px solid rgba(255,255,255,0.07);border-radius:14px;padding:14px;text-align:center"><div style="font-size:20px;font-weight:800;color:#06b6d4">6</div><div style="font-size:10px;color:rgba(240,240,255,0.55)">الخدمات</div></div><div style="background:rgba(255,255,255,0.035);border:1px solid rgba(255,255,255,0.07);border-radius:14px;padding:14px;text-align:center"><div style="font-size:20px;font-weight:800;color:#8b5cf6">99.9%%</div><div style="font-size:10px;color:rgba(240,240,255,0.55)">التوفر</div></div></div>
<div style="background:rgba(255,255,255,0.035);border:1px solid rgba(255,255,255,0.07);border-radius:14px;padding:18px;margin-bottom:14px"><div style="font-size:15px;font-weight:700;margin-bottom:14px">حالة الخدمات</div>%s</div>
<div style="background:rgba(255,255,255,0.035);border:1px solid rgba(255,255,255,0.07);border-radius:14px;padding:18px;margin-bottom:14px"><div style="font-size:15px;font-weight:700;margin-bottom:14px">حركة الزيارات</div><div style="display:flex;align-items:flex-end;justify-content:space-between;height:120px;gap:6px;direction:ltr">%s</div></div>
<div style="text-align:center;padding:12px;font-size:10px;color:rgba(240,240,255,0.3)">Echo v4 · Go 1.22 · يعمل</div>
</div></body></html>`,up,sh,bars)
}
