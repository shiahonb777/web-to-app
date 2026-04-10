# -*- coding: utf-8 -*-
"""
Flask Analytics Dashboard — Beautiful Python Web App Demo
A modern analytics dashboard showcasing Flask's capabilities
"""
import os
import json
import random
import math
from datetime import datetime, timedelta
from flask import Flask, render_template_string, jsonify, request

app = Flask(__name__)

def generate_weekly_data():
    days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
    return [
        {'day': d, 'users': random.randint(180, 420), 'orders': random.randint(30, 95), 'revenue': random.randint(2800, 9500)}
        for d in days
    ]

def generate_activities():
    actions = [
        ('Purchased Premium Plan', '💎', '#7c3aed'),
        ('Completed first order', '🛒', '#3b82f6'),
        ('Upgraded to Enterprise', '🚀', '#10b981'),
        ('Renewed annual subscription', '🔄', '#f59e0b'),
        ('Invited 3 team members', '👥', '#ec4899'),
        ('Exported data report', '📊', '#06b6d4'),
        ('Created new project', '📁', '#8b5cf6'),
        ('Completed security verification', '🔒', '#10b981'),
    ]
    names = ['Alex Chen', 'Emily Wang', 'James Lee', 'Sarah Kim', 'Michael Liu', 'Anna Park', 'David Wu', 'Lisa Zhang']
    result = []
    now = datetime.now()
    for i in range(8):
        action, icon, color = actions[i % len(actions)]
        minutes_ago = random.randint(5, 720)
        if minutes_ago < 60:
            time_str = f'{minutes_ago}m ago'
        elif minutes_ago < 1440:
            time_str = f'{minutes_ago // 60}h ago'
        else:
            time_str = f'{minutes_ago // 1440}d ago'
        result.append({
            'name': names[i % len(names)],
            'action': action,
            'icon': icon,
            'color': color,
            'time': time_str,
        })
    return result

DASHBOARD_HTML = r"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<title>Flask Analytics</title>
<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
:root {
    --bg-primary: #0a0a1a;
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
    --gradient-purple: linear-gradient(135deg, #7c3aed, #a855f7);
    --gradient-blue: linear-gradient(135deg, #3b82f6, #60a5fa);
    --gradient-green: linear-gradient(135deg, #10b981, #34d399);
    --gradient-orange: linear-gradient(135deg, #f59e0b, #fbbf24);
    --radius: 16px;
}
body {
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    background: var(--bg-primary);
    color: var(--text-primary);
    min-height: 100vh;
    -webkit-font-smoothing: antialiased;
}
body::before {
    content: '';
    position: fixed; top: -50%; left: -50%; width: 200%; height: 200%;
    background: radial-gradient(ellipse at 30% 20%, rgba(124,58,237,0.08) 0%, transparent 50%),
                radial-gradient(ellipse at 70% 60%, rgba(59,130,246,0.06) 0%, transparent 50%);
    pointer-events: none; z-index: 0;
}
.container { max-width: 480px; margin: 0 auto; padding: 0 16px 32px; position: relative; z-index: 1; }
.header { display: flex; justify-content: space-between; align-items: center; padding: 20px 4px 24px; }
.header-left h1 { font-size: 24px; font-weight: 800; background: linear-gradient(135deg, #f0f0ff, #a78bfa); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.header-left p { font-size: 13px; color: var(--text-secondary); margin-top: 2px; }
.header-right { display: flex; align-items: center; gap: 12px; }
.status-dot { width: 10px; height: 10px; border-radius: 50%; background: var(--accent-green); box-shadow: 0 0 12px var(--accent-green); animation: pulse-dot 2s infinite; }
@keyframes pulse-dot { 0%,100%{opacity:1;box-shadow:0 0 12px var(--accent-green)} 50%{opacity:.6;box-shadow:0 0 20px var(--accent-green)} }
.avatar { width: 38px; height: 38px; border-radius: 12px; background: var(--gradient-purple); display: flex; align-items: center; justify-content: center; font-size: 16px; }
.stats-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 20px; }
.stat-card { background: var(--bg-card); border: 1px solid var(--border-card); border-radius: var(--radius); padding: 18px 16px; position: relative; overflow: hidden; backdrop-filter: blur(20px); transition: all 0.3s; cursor: pointer; }
.stat-card:active { transform: scale(0.97); background: var(--bg-card-hover); }
.stat-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 2px; }
.stat-card:nth-child(1)::before { background: var(--gradient-purple); }
.stat-card:nth-child(2)::before { background: var(--gradient-green); }
.stat-card:nth-child(3)::before { background: var(--gradient-blue); }
.stat-card:nth-child(4)::before { background: var(--gradient-orange); }
.stat-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.stat-label { font-size: 12px; font-weight: 500; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px; }
.stat-icon { width: 28px; height: 28px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 14px; }
.stat-card:nth-child(1) .stat-icon { background: rgba(124,58,237,0.15); }
.stat-card:nth-child(2) .stat-icon { background: rgba(16,185,129,0.15); }
.stat-card:nth-child(3) .stat-icon { background: rgba(59,130,246,0.15); }
.stat-card:nth-child(4) .stat-icon { background: rgba(245,158,11,0.15); }
.stat-value { font-size: 26px; font-weight: 800; letter-spacing: -1px; line-height: 1; margin-bottom: 6px; }
.stat-change { font-size: 11px; font-weight: 600; display: inline-flex; align-items: center; gap: 3px; padding: 2px 8px; border-radius: 20px; color: var(--accent-green); background: rgba(16,185,129,0.1); }
.section-card { background: var(--bg-card); border: 1px solid var(--border-card); border-radius: var(--radius); padding: 20px; margin-bottom: 20px; backdrop-filter: blur(20px); }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.section-title { font-size: 16px; font-weight: 700; }
.section-badge { font-size: 11px; font-weight: 600; padding: 4px 10px; border-radius: 20px; background: rgba(124,58,237,0.12); color: var(--accent-purple); }
.chart-container { display: flex; align-items: flex-end; justify-content: space-between; height: 160px; padding-top: 10px; gap: 6px; }
.chart-bar-group { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6px; height: 100%; justify-content: flex-end; }
.chart-bar { width: 100%; max-width: 32px; border-radius: 6px 6px 4px 4px; background: linear-gradient(180deg, #a855f7, #7c3aed); box-shadow: 0 0 12px rgba(124,58,237,0.3); }
.chart-bar-label { font-size: 10px; color: var(--text-tertiary); font-weight: 500; }
.activity-item { display: flex; align-items: center; gap: 14px; padding: 14px 0; border-bottom: 1px solid rgba(255,255,255,0.04); }
.activity-item:last-child { border-bottom: none; }
.activity-avatar { width: 42px; height: 42px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0; }
.activity-info { flex: 1; min-width: 0; }
.activity-name { font-size: 14px; font-weight: 600; margin-bottom: 2px; }
.activity-action { font-size: 12px; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.activity-time { font-size: 11px; color: var(--text-tertiary); white-space: nowrap; font-weight: 500; }
.server-info { margin-top: 8px; padding: 16px; background: var(--bg-card); border: 1px solid var(--border-card); border-radius: var(--radius); text-align: center; }
.server-info-badge { display: inline-flex; align-items: center; gap: 6px; font-size: 11px; font-weight: 600; color: var(--text-tertiary); padding: 6px 14px; border-radius: 20px; background: rgba(124,58,237,0.06); border: 1px solid rgba(124,58,237,0.1); }
.server-info-badge .dot { width: 6px; height: 6px; border-radius: 50%; background: var(--accent-green); box-shadow: 0 0 8px var(--accent-green); }
@keyframes fadeInUp { from{opacity:0;transform:translateY(20px)} to{opacity:1;transform:translateY(0)} }
.stat-card { animation: fadeInUp 0.5s ease backwards; }
.stat-card:nth-child(1){animation-delay:.05s} .stat-card:nth-child(2){animation-delay:.1s} .stat-card:nth-child(3){animation-delay:.15s} .stat-card:nth-child(4){animation-delay:.2s}
.section-card { animation: fadeInUp 0.5s ease 0.25s backwards; }
.activity-item { animation: fadeInUp 0.4s ease backwards; }
.activity-item:nth-child(1){animation-delay:.3s} .activity-item:nth-child(2){animation-delay:.35s} .activity-item:nth-child(3){animation-delay:.4s} .activity-item:nth-child(4){animation-delay:.45s}
@keyframes growUp { from{transform:scaleY(0)} to{transform:scaleY(1)} }
.chart-bar { animation: growUp 0.8s cubic-bezier(0.34,1.56,0.64,1) backwards; transform-origin: bottom; }
</style>
</head>
<body>
<div class="container">
    <div class="header">
        <div class="header-left"><h1>Flask Analytics</h1><p>{{ current_date }}</p></div>
        <div class="header-right"><div class="status-dot"></div><div class="avatar">🐍</div></div>
    </div>
    <div class="stats-grid">
        <div class="stat-card"><div class="stat-header"><span class="stat-label">Users</span><div class="stat-icon">👥</div></div><div class="stat-value">12,847</div><span class="stat-change">↑ 12.5%</span></div>
        <div class="stat-card"><div class="stat-header"><span class="stat-label">Revenue</span><div class="stat-icon">💰</div></div><div class="stat-value">$128.5K</div><span class="stat-change">↑ 8.3%</span></div>
        <div class="stat-card"><div class="stat-header"><span class="stat-label">Orders</span><div class="stat-icon">📦</div></div><div class="stat-value">3,421</div><span class="stat-change">↑ 15.2%</span></div>
        <div class="stat-card"><div class="stat-header"><span class="stat-label">Growth</span><div class="stat-icon">📈</div></div><div class="stat-value">+24.6%</div><span class="stat-change">↑ Steady</span></div>
    </div>
    <div class="section-card">
        <div class="section-header"><span class="section-title">Weekly Overview</span><span class="section-badge">📊 Live</span></div>
        <div class="chart-container">
            {% for day in weekly_data %}
            <div class="chart-bar-group"><div class="chart-bar" style="height:{{ day.users_pct }}%;animation-delay:{{ 0.3 + loop.index0 * 0.08 }}s"></div><div class="chart-bar-label">{{ day.day }}</div></div>
            {% endfor %}
        </div>
    </div>
    <div class="section-card" style="animation-delay:.35s">
        <div class="section-header"><span class="section-title">Recent Activity</span><span class="section-badge">🔔 Today</span></div>
        {% for a in activities %}
        <div class="activity-item"><div class="activity-avatar" style="background:{{ a.color }}22">{{ a.icon }}</div><div class="activity-info"><div class="activity-name">{{ a.name }}</div><div class="activity-action">{{ a.action }}</div></div><div class="activity-time">{{ a.time }}</div></div>
        {% endfor %}
    </div>
    <div class="server-info"><div class="server-info-badge"><span class="dot"></span>Flask {{ flask_version }} · Python {{ python_version }} · Running</div></div>
</div>
</body>
</html>
"""

@app.route('/')
def index():
    import sys
    weekly = generate_weekly_data()
    max_u = max(d['users'] for d in weekly) or 1
    for d in weekly: d['users_pct'] = int(d['users'] / max_u * 85) + 15
    flask_ver = '3.0'
    try:
        import flask as _f; flask_ver = getattr(_f, '__version__', '3.0')
    except: pass
    return render_template_string(DASHBOARD_HTML, weekly_data=weekly, activities=generate_activities()[:6],
        current_date=datetime.now().strftime('%B %d, %Y'), flask_version=flask_ver, python_version=f'{sys.version_info.major}.{sys.version_info.minor}')

@app.route('/api/stats')
def api_stats():
    return jsonify({'status':'ok','data':{'users':12847,'orders':3421,'revenue':128500,'growth':24.6,'weekly':generate_weekly_data()}})

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port)
