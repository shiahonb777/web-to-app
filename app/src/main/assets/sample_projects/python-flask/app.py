# -*- coding: utf-8 -*-
"""
Flask Analytics Dashboard — 精美的 Python Web 应用示例
一个现代化的数据分析仪表盘，展示 Flask 的强大功能
"""
import os
import json
import random
import math
from datetime import datetime, timedelta
from flask import Flask, render_template_string, jsonify, request

app = Flask(__name__)

# ==================== 模拟数据 ====================

def generate_weekly_data():
    """生成最近 7 天的模拟数据"""
    days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    return [
        {'day': d, 'users': random.randint(180, 420), 'orders': random.randint(30, 95), 'revenue': random.randint(2800, 9500)}
        for d in days
    ]

def generate_activities():
    """生成最近活动记录"""
    actions = [
        ('购买了高级会员计划', '💎', '#7c3aed'),
        ('完成了首笔订单', '🛒', '#3b82f6'),
        ('升级到企业版', '🚀', '#10b981'),
        ('续费了年度订阅', '🔄', '#f59e0b'),
        ('邀请了 3 位团队成员', '👥', '#ec4899'),
        ('导出了数据报告', '📊', '#06b6d4'),
        ('创建了新项目', '📁', '#8b5cf6'),
        ('完成了安全认证', '🔒', '#10b981'),
    ]
    names = ['张伟', '李娜', '王强', '刘洋', '陈静', '赵敏', '周杰', '吴芳', 'Alex Chen', 'Emily Wang']
    result = []
    now = datetime.now()
    for i in range(8):
        action, icon, color = actions[i % len(actions)]
        minutes_ago = random.randint(5, 720)
        if minutes_ago < 60:
            time_str = f'{minutes_ago} 分钟前'
        elif minutes_ago < 1440:
            time_str = f'{minutes_ago // 60} 小时前'
        else:
            time_str = f'{minutes_ago // 1440} 天前'
        result.append({
            'name': names[i % len(names)],
            'action': action,
            'icon': icon,
            'color': color,
            'time': time_str,
            'initials': names[i % len(names)][:1]
        })
    return result

# ==================== 路由 ====================

DASHBOARD_HTML = r"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<title>Flask Analytics</title>
<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
:root {
    --bg-primary: #0a0a1a;
    --bg-secondary: #111128;
    --bg-card: rgba(255,255,255,0.04);
    --bg-card-hover: rgba(255,255,255,0.08);
    --border-card: rgba(255,255,255,0.08);
    --text-primary: #f0f0ff;
    --text-secondary: rgba(240,240,255,0.6);
    --text-tertiary: rgba(240,240,255,0.35);
    --accent-purple: #7c3aed;
    --accent-blue: #3b82f6;
    --accent-green: #10b981;
    --accent-orange: #f59e0b;
    --accent-pink: #ec4899;
    --accent-cyan: #06b6d4;
    --gradient-purple: linear-gradient(135deg, #7c3aed, #a855f7);
    --gradient-blue: linear-gradient(135deg, #3b82f6, #60a5fa);
    --gradient-green: linear-gradient(135deg, #10b981, #34d399);
    --gradient-orange: linear-gradient(135deg, #f59e0b, #fbbf24);
    --shadow-glow: 0 0 30px rgba(124,58,237,0.15);
    --radius: 16px;
    --radius-sm: 10px;
}
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap');
body {
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    background: var(--bg-primary);
    color: var(--text-primary);
    min-height: 100vh;
    overflow-x: hidden;
    -webkit-font-smoothing: antialiased;
}
/* 顶部背景光效 */
body::before {
    content: '';
    position: fixed;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(ellipse at 30% 20%, rgba(124,58,237,0.08) 0%, transparent 50%),
                radial-gradient(ellipse at 70% 60%, rgba(59,130,246,0.06) 0%, transparent 50%);
    pointer-events: none;
    z-index: 0;
}
.container {
    max-width: 480px;
    margin: 0 auto;
    padding: 0 16px 32px;
    position: relative;
    z-index: 1;
}

/* ===== Header ===== */
.header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 4px 24px;
}
.header-left h1 {
    font-size: 24px;
    font-weight: 800;
    background: linear-gradient(135deg, #f0f0ff, #a78bfa);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    letter-spacing: -0.5px;
}
.header-left p {
    font-size: 13px;
    color: var(--text-secondary);
    margin-top: 2px;
}
.header-right {
    display: flex;
    align-items: center;
    gap: 12px;
}
.status-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--accent-green);
    box-shadow: 0 0 12px var(--accent-green);
    animation: pulse-dot 2s infinite;
}
@keyframes pulse-dot {
    0%, 100% { opacity: 1; box-shadow: 0 0 12px var(--accent-green); }
    50% { opacity: 0.6; box-shadow: 0 0 20px var(--accent-green); }
}
.avatar {
    width: 38px;
    height: 38px;
    border-radius: 12px;
    background: var(--gradient-purple);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    font-weight: 700;
}

/* ===== Stats Grid ===== */
.stats-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
    margin-bottom: 20px;
}
.stat-card {
    background: var(--bg-card);
    border: 1px solid var(--border-card);
    border-radius: var(--radius);
    padding: 18px 16px;
    position: relative;
    overflow: hidden;
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    transition: all 0.3s ease;
    cursor: pointer;
}
.stat-card:active {
    transform: scale(0.97);
    background: var(--bg-card-hover);
}
.stat-card::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 2px;
    border-radius: var(--radius) var(--radius) 0 0;
}
.stat-card:nth-child(1)::before { background: var(--gradient-purple); }
.stat-card:nth-child(2)::before { background: var(--gradient-green); }
.stat-card:nth-child(3)::before { background: var(--gradient-blue); }
.stat-card:nth-child(4)::before { background: var(--gradient-orange); }

.stat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
}
.stat-label {
    font-size: 12px;
    font-weight: 500;
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.5px;
}
.stat-icon {
    width: 28px;
    height: 28px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
}
.stat-card:nth-child(1) .stat-icon { background: rgba(124,58,237,0.15); }
.stat-card:nth-child(2) .stat-icon { background: rgba(16,185,129,0.15); }
.stat-card:nth-child(3) .stat-icon { background: rgba(59,130,246,0.15); }
.stat-card:nth-child(4) .stat-icon { background: rgba(245,158,11,0.15); }

.stat-value {
    font-size: 26px;
    font-weight: 800;
    letter-spacing: -1px;
    line-height: 1;
    margin-bottom: 6px;
}
.stat-change {
    font-size: 11px;
    font-weight: 600;
    display: inline-flex;
    align-items: center;
    gap: 3px;
    padding: 2px 8px;
    border-radius: 20px;
}
.stat-change.up {
    color: var(--accent-green);
    background: rgba(16,185,129,0.1);
}
.stat-change.down {
    color: #ef4444;
    background: rgba(239,68,68,0.1);
}

/* ===== Chart Section ===== */
.section-card {
    background: var(--bg-card);
    border: 1px solid var(--border-card);
    border-radius: var(--radius);
    padding: 20px;
    margin-bottom: 20px;
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
}
.section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
}
.section-title {
    font-size: 16px;
    font-weight: 700;
    color: var(--text-primary);
}
.section-badge {
    font-size: 11px;
    font-weight: 600;
    padding: 4px 10px;
    border-radius: 20px;
    background: rgba(124,58,237,0.12);
    color: var(--accent-purple);
}

/* Chart */
.chart-container {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    height: 160px;
    padding-top: 10px;
    gap: 6px;
}
.chart-bar-group {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    height: 100%;
    justify-content: flex-end;
}
.chart-bar {
    width: 100%;
    max-width: 32px;
    border-radius: 6px 6px 4px 4px;
    position: relative;
    transition: all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
    transform-origin: bottom;
}
.chart-bar.purple {
    background: linear-gradient(180deg, #a855f7, #7c3aed);
    box-shadow: 0 0 12px rgba(124,58,237,0.3);
}
.chart-bar.blue {
    background: linear-gradient(180deg, #60a5fa, #3b82f6);
    box-shadow: 0 0 12px rgba(59,130,246,0.3);
}
.chart-bar-label {
    font-size: 10px;
    color: var(--text-tertiary);
    font-weight: 500;
}

/* ===== Activity Feed ===== */
.activity-item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 0;
    border-bottom: 1px solid rgba(255,255,255,0.04);
    transition: all 0.2s;
}
.activity-item:last-child { border-bottom: none; }
.activity-avatar {
    width: 42px;
    height: 42px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    flex-shrink: 0;
}
.activity-info {
    flex: 1;
    min-width: 0;
}
.activity-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 2px;
}
.activity-action {
    font-size: 12px;
    color: var(--text-secondary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
.activity-time {
    font-size: 11px;
    color: var(--text-tertiary);
    white-space: nowrap;
    font-weight: 500;
}

/* ===== Server Info Footer ===== */
.server-info {
    margin-top: 8px;
    padding: 16px;
    background: var(--bg-card);
    border: 1px solid var(--border-card);
    border-radius: var(--radius);
    text-align: center;
}
.server-info-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 11px;
    font-weight: 600;
    color: var(--text-tertiary);
    padding: 6px 14px;
    border-radius: 20px;
    background: rgba(124,58,237,0.06);
    border: 1px solid rgba(124,58,237,0.1);
}
.server-info-badge .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--accent-green);
    box-shadow: 0 0 8px var(--accent-green);
}

/* ===== Animations ===== */
@keyframes fadeInUp {
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
}
.stat-card { animation: fadeInUp 0.5s ease backwards; }
.stat-card:nth-child(1) { animation-delay: 0.05s; }
.stat-card:nth-child(2) { animation-delay: 0.1s; }
.stat-card:nth-child(3) { animation-delay: 0.15s; }
.stat-card:nth-child(4) { animation-delay: 0.2s; }
.section-card { animation: fadeInUp 0.5s ease 0.25s backwards; }
.activity-item { animation: fadeInUp 0.4s ease backwards; }
.activity-item:nth-child(1) { animation-delay: 0.3s; }
.activity-item:nth-child(2) { animation-delay: 0.35s; }
.activity-item:nth-child(3) { animation-delay: 0.4s; }
.activity-item:nth-child(4) { animation-delay: 0.45s; }
.activity-item:nth-child(5) { animation-delay: 0.5s; }

/* Number count-up */
@keyframes countUp {
    from { opacity: 0; transform: translateY(8px); }
    to { opacity: 1; transform: translateY(0); }
}
.stat-value { animation: countUp 0.6s ease 0.3s backwards; }

/* Bar grow */
@keyframes growUp {
    from { transform: scaleY(0); }
    to { transform: scaleY(1); }
}
.chart-bar {
    animation: growUp 0.8s cubic-bezier(0.34, 1.56, 0.64, 1) backwards;
}
</style>
</head>
<body>
<div class="container">
    <!-- Header -->
    <div class="header">
        <div class="header-left">
            <h1>Flask Analytics</h1>
            <p>{{ current_date }}</p>
        </div>
        <div class="header-right">
            <div class="status-dot"></div>
            <div class="avatar">🐍</div>
        </div>
    </div>

    <!-- Stats Grid -->
    <div class="stats-grid">
        <div class="stat-card" onclick="this.style.transform='scale(0.96)';setTimeout(()=>this.style.transform='',200)">
            <div class="stat-header">
                <span class="stat-label">用户</span>
                <div class="stat-icon">👥</div>
            </div>
            <div class="stat-value">{{ stats.users }}</div>
            <span class="stat-change up">↑ {{ stats.users_change }}%</span>
        </div>
        <div class="stat-card" onclick="this.style.transform='scale(0.96)';setTimeout(()=>this.style.transform='',200)">
            <div class="stat-header">
                <span class="stat-label">营收</span>
                <div class="stat-icon">💰</div>
            </div>
            <div class="stat-value">${{ stats.revenue_display }}</div>
            <span class="stat-change up">↑ {{ stats.revenue_change }}%</span>
        </div>
        <div class="stat-card" onclick="this.style.transform='scale(0.96)';setTimeout(()=>this.style.transform='',200)">
            <div class="stat-header">
                <span class="stat-label">订单</span>
                <div class="stat-icon">📦</div>
            </div>
            <div class="stat-value">{{ stats.orders }}</div>
            <span class="stat-change up">↑ {{ stats.orders_change }}%</span>
        </div>
        <div class="stat-card" onclick="this.style.transform='scale(0.96)';setTimeout(()=>this.style.transform='',200)">
            <div class="stat-header">
                <span class="stat-label">增长率</span>
                <div class="stat-icon">📈</div>
            </div>
            <div class="stat-value">+{{ stats.growth }}%</div>
            <span class="stat-change up">↑ 稳健增长</span>
        </div>
    </div>

    <!-- Weekly Chart -->
    <div class="section-card">
        <div class="section-header">
            <span class="section-title">本周概览</span>
            <span class="section-badge">📊 实时</span>
        </div>
        <div class="chart-container">
            {% for day in weekly_data %}
            <div class="chart-bar-group">
                <div class="chart-bar purple" style="height: {{ day.users_pct }}%; animation-delay: {{ 0.3 + loop.index0 * 0.08 }}s;"></div>
                <div class="chart-bar-label">{{ day.day }}</div>
            </div>
            {% endfor %}
        </div>
    </div>

    <!-- Activity Feed -->
    <div class="section-card" style="animation-delay: 0.35s;">
        <div class="section-header">
            <span class="section-title">最近动态</span>
            <span class="section-badge">🔔 今日</span>
        </div>
        {% for activity in activities %}
        <div class="activity-item">
            <div class="activity-avatar" style="background: {{ activity.color }}22;">
                {{ activity.icon }}
            </div>
            <div class="activity-info">
                <div class="activity-name">{{ activity.name }}</div>
                <div class="activity-action">{{ activity.action }}</div>
            </div>
            <div class="activity-time">{{ activity.time }}</div>
        </div>
        {% endfor %}
    </div>

    <!-- Server Info -->
    <div class="server-info">
        <div class="server-info-badge">
            <span class="dot"></span>
            Flask {{ flask_version }} · Python {{ python_version }} · 运行中
        </div>
    </div>
</div>

<script>
// 给柱状图添加交互式 tooltip
document.querySelectorAll('.chart-bar').forEach(bar => {
    bar.addEventListener('click', function() {
        this.style.filter = 'brightness(1.3)';
        setTimeout(() => this.style.filter = '', 300);
    });
});
</script>
</body>
</html>
"""

@app.route('/')
def index():
    import sys
    stats = {
        'users': '12,847',
        'users_change': '12.5',
        'revenue_display': '128.5K',
        'revenue_change': '8.3',
        'orders': '3,421',
        'orders_change': '15.2',
        'growth': '24.6',
    }
    weekly = generate_weekly_data()
    max_users = max(d['users'] for d in weekly) or 1
    for d in weekly:
        d['users_pct'] = int(d['users'] / max_users * 85) + 15
    flask_ver = '3.0'
    try:
        import flask as _f
        flask_ver = getattr(_f, '__version__', '3.0')
    except Exception:
        pass
    return render_template_string(
        DASHBOARD_HTML,
        stats=stats,
        weekly_data=weekly,
        activities=generate_activities()[:6],
        current_date=datetime.now().strftime('%Y年%m月%d日'),
        flask_version=flask_ver,
        python_version=f'{sys.version_info.major}.{sys.version_info.minor}',
    )

@app.route('/api/stats')
def api_stats():
    """RESTful API 示例"""
    return jsonify({
        'status': 'ok',
        'data': {
            'users': 12847,
            'orders': 3421,
            'revenue': 128500,
            'growth': 24.6,
            'weekly': generate_weekly_data(),
        },
        'meta': {
            'framework': 'Flask',
            'timestamp': datetime.now().isoformat(),
        }
    })

@app.route('/api/activities')
def api_activities():
    """活动记录 API"""
    return jsonify({
        'status': 'ok',
        'data': generate_activities(),
    })

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port)
