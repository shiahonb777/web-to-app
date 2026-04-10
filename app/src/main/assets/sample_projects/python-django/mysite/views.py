"""
Django Admin Dashboard — 精美的管理后台仪表盘
展示 Django 的 MTV 架构和模板渲染能力
"""
import os
import random
from datetime import datetime
from django.http import HttpResponse, JsonResponse

def dashboard(request):
    """主仪表盘视图"""
    stats = {
        'users': f'{random.randint(8000,15000):,}',
        'posts': f'{random.randint(2000,5000):,}',
        'comments': f'{random.randint(10000,30000):,}',
        'pageviews': f'{random.randint(80,200)}K',
    }
    models = [
        {'name': 'User', 'count': random.randint(800, 1500), 'icon': '👤', 'color': '#7c3aed'},
        {'name': 'Post', 'count': random.randint(200, 500), 'icon': '📝', 'color': '#3b82f6'},
        {'name': 'Comment', 'count': random.randint(1000, 3000), 'icon': '💬', 'color': '#10b981'},
        {'name': 'Category', 'count': random.randint(10, 30), 'icon': '📂', 'color': '#f59e0b'},
        {'name': 'Tag', 'count': random.randint(50, 150), 'icon': '🏷️', 'color': '#ec4899'},
        {'name': 'Media', 'count': random.randint(100, 400), 'icon': '🖼️', 'color': '#06b6d4'},
    ]
    logs = [
        {'action': '创建了新文章', 'user': '管理员', 'time': f'{random.randint(2,30)}分钟前', 'icon': '📝', 'color': '#3b82f6'},
        {'action': '更新了用户权限', 'user': '系统', 'time': f'{random.randint(1,5)}小时前', 'icon': '🔐', 'color': '#7c3aed'},
        {'action': '发布了 3 条评论', 'user': '张伟', 'time': f'{random.randint(1,3)}小时前', 'icon': '💬', 'color': '#10b981'},
        {'action': '上传了 5 张图片', 'user': '李娜', 'time': f'{random.randint(2,6)}小时前', 'icon': '🖼️', 'color': '#06b6d4'},
        {'action': '备份了数据库', 'user': '系统', 'time': '今天 08:00', 'icon': '💾', 'color': '#f59e0b'},
    ]
    weekly = [random.randint(40, 100) for _ in range(7)]
    max_w = max(weekly) or 1
    week_pct = [int(v / max_w * 85) + 15 for v in weekly]
    days = ['一', '二', '三', '四', '五', '六', '日']
    import django
    html = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<title>Django Admin</title>
<style>
*{{margin:0;padding:0;box-sizing:border-box}}
:root{{--bg:#0c0c1d;--card:rgba(255,255,255,0.035);--border:rgba(255,255,255,0.07);--text:#f0f0ff;--text2:rgba(240,240,255,0.55);--text3:rgba(240,240,255,0.3);--green:#0ea572;--django:#092e20;--django2:#44b78b;--radius:14px}}
body{{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:var(--bg);color:var(--text);min-height:100vh;-webkit-font-smoothing:antialiased}}
body::before{{content:'';position:fixed;top:-40%;left:-40%;width:180%;height:180%;background:radial-gradient(ellipse at 40% 25%,rgba(68,183,139,0.07) 0%,transparent 55%),radial-gradient(ellipse at 70% 70%,rgba(124,58,237,0.04) 0%,transparent 50%);pointer-events:none}}
.c{{max-width:460px;margin:0 auto;padding:0 14px 32px;position:relative;z-index:1}}
.hdr{{padding:20px 2px;display:flex;justify-content:space-between;align-items:center}}
.hdr h1{{font-size:22px;font-weight:800;background:linear-gradient(135deg,#44b78b,#2dd4a8);-webkit-background-clip:text;-webkit-text-fill-color:transparent}}
.hdr p{{font-size:12px;color:var(--text2);margin-top:2px}}
.badge{{display:inline-flex;align-items:center;gap:5px;font-size:11px;font-weight:600;padding:5px 12px;border-radius:20px;background:rgba(68,183,139,0.1);color:var(--django2);border:1px solid rgba(68,183,139,0.15)}}
.badge .dot{{width:7px;height:7px;border-radius:50%;background:var(--django2);box-shadow:0 0 10px var(--django2);animation:p 2s infinite}}
@keyframes p{{0%,100%{{opacity:1}}50%{{opacity:.5}}}}
.row4{{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:16px}}
.s{{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:16px;position:relative;overflow:hidden;backdrop-filter:blur(16px)}}
.s::before{{content:'';position:absolute;top:0;left:0;right:0;height:2px}}
.s:nth-child(1)::before{{background:linear-gradient(90deg,#7c3aed,#a855f7)}}
.s:nth-child(2)::before{{background:linear-gradient(90deg,#3b82f6,#60a5fa)}}
.s:nth-child(3)::before{{background:linear-gradient(90deg,#10b981,#34d399)}}
.s:nth-child(4)::before{{background:linear-gradient(90deg,#f59e0b,#fbbf24)}}
.s-h{{display:flex;justify-content:space-between;align-items:center;margin-bottom:8px}}
.s-label{{font-size:11px;color:var(--text2);text-transform:uppercase;letter-spacing:.5px;font-weight:500}}
.s-icon{{font-size:14px}}
.s-val{{font-size:24px;font-weight:800;letter-spacing:-0.5px}}
.s-tag{{font-size:10px;font-weight:600;color:var(--django2);background:rgba(68,183,139,0.1);padding:2px 7px;border-radius:10px;display:inline-block;margin-top:4px}}
.card{{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:18px;margin-bottom:14px;backdrop-filter:blur(16px)}}
.card-h{{display:flex;justify-content:space-between;align-items:center;margin-bottom:14px}}
.card-t{{font-size:15px;font-weight:700}}
.card-b{{font-size:10px;font-weight:600;padding:3px 9px;border-radius:16px;background:rgba(68,183,139,0.1);color:var(--django2)}}
.model{{display:flex;align-items:center;gap:12px;padding:10px 0;border-bottom:1px solid rgba(255,255,255,0.03)}}
.model:last-child{{border:none}}
.model-icon{{width:36px;height:36px;border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:16px;flex-shrink:0}}
.model-info{{flex:1}}
.model-name{{font-size:13px;font-weight:600}}
.model-count{{font-size:11px;color:var(--text2)}}
.chart{{display:flex;align-items:flex-end;justify-content:space-between;height:120px;gap:6px;margin-top:4px}}
.bar-g{{flex:1;display:flex;flex-direction:column;align-items:center;gap:4px;height:100%;justify-content:flex-end}}
.bar{{width:100%;max-width:28px;border-radius:5px 5px 3px 3px;background:linear-gradient(180deg,#44b78b,#2a9d6f);box-shadow:0 0 10px rgba(68,183,139,0.25);transform-origin:bottom;animation:grow .7s cubic-bezier(.34,1.56,.64,1) backwards}}
@keyframes grow{{from{{transform:scaleY(0)}}to{{transform:scaleY(1)}}}}
.bar-l{{font-size:9px;color:var(--text3);font-weight:500}}
.log{{display:flex;align-items:center;gap:12px;padding:11px 0;border-bottom:1px solid rgba(255,255,255,0.03)}}
.log:last-child{{border:none}}
.log-icon{{width:34px;height:34px;border-radius:9px;display:flex;align-items:center;justify-content:center;font-size:15px;flex-shrink:0}}
.log-info{{flex:1;min-width:0}}
.log-action{{font-size:13px;font-weight:600}}
.log-user{{font-size:11px;color:var(--text2)}}
.log-time{{font-size:10px;color:var(--text3);font-weight:500;white-space:nowrap}}
.ft{{text-align:center;padding:12px;margin-top:4px}}
.ft-b{{display:inline-flex;align-items:center;gap:6px;font-size:10px;font-weight:600;color:var(--text3);padding:5px 12px;border-radius:20px;background:rgba(68,183,139,0.04);border:1px solid rgba(68,183,139,0.08)}}
@keyframes fadeIn{{from{{opacity:0;transform:translateY(14px)}}to{{opacity:1;transform:translateY(0)}}}}
.s,.card{{animation:fadeIn .5s ease backwards}}
.s:nth-child(1){{animation-delay:.05s}}.s:nth-child(2){{animation-delay:.1s}}.s:nth-child(3){{animation-delay:.15s}}.s:nth-child(4){{animation-delay:.2s}}
</style>
</head>
<body>
<div class="c">
<div class="hdr">
    <div><h1>🦄 Django Admin</h1><p>管理后台仪表盘</p></div>
    <div class="badge"><span class="dot"></span>运行中</div>
</div>
<div class="row4">
    <div class="s"><div class="s-h"><span class="s-label">用户</span><span class="s-icon">👥</span></div><div class="s-val">{stats['users']}</div><span class="s-tag">↑ 12.3%</span></div>
    <div class="s"><div class="s-h"><span class="s-label">文章</span><span class="s-icon">📝</span></div><div class="s-val">{stats['posts']}</div><span class="s-tag">↑ 8.7%</span></div>
    <div class="s"><div class="s-h"><span class="s-label">评论</span><span class="s-icon">💬</span></div><div class="s-val">{stats['comments']}</div><span class="s-tag">↑ 15.1%</span></div>
    <div class="s"><div class="s-h"><span class="s-label">浏览</span><span class="s-icon">👁️</span></div><div class="s-val">{stats['pageviews']}</div><span class="s-tag">↑ 22.4%</span></div>
</div>
<div class="card">
    <div class="card-h"><span class="card-t">本周活跃</span><span class="card-b">📊 趋势</span></div>
    <div class="chart">
        {''.join(f'<div class="bar-g"><div class="bar" style="height:{week_pct[i]}%;animation-delay:{0.25+i*0.07}s"></div><div class="bar-l">周{days[i]}</div></div>' for i in range(7))}
    </div>
</div>
<div class="card">
    <div class="card-h"><span class="card-t">数据模型</span><span class="card-b">📦 {len(models)} 个</span></div>
    {''.join(f'<div class="model"><div class="model-icon" style="background:{m["color"]}18">{m["icon"]}</div><div class="model-info"><div class="model-name">{m["name"]}</div><div class="model-count">{m["count"]} 条记录</div></div></div>' for m in models)}
</div>
<div class="card">
    <div class="card-h"><span class="card-t">操作日志</span><span class="card-b">🔔 最近</span></div>
    {''.join(f'<div class="log"><div class="log-icon" style="background:{l["color"]}18">{l["icon"]}</div><div class="log-info"><div class="log-action">{l["action"]}</div><div class="log-user">{l["user"]}</div></div><div class="log-time">{l["time"]}</div></div>' for l in logs)}
</div>
<div class="ft"><div class="ft-b"><span class="dot" style="width:5px;height:5px;border-radius:50%;background:var(--django2);box-shadow:0 0 6px var(--django2)"></span>Django {django.get_version()} · Python · {datetime.now().strftime('%Y-%m-%d')}</div></div>
</div>
</body>
</html>"""
    return HttpResponse(html)

def api_stats(request):
    return JsonResponse({
        'status': 'ok',
        'data': {
            'users': random.randint(8000, 15000),
            'posts': random.randint(2000, 5000),
            'comments': random.randint(10000, 30000),
        },
        'framework': 'Django',
        'timestamp': datetime.now().isoformat()
    })
