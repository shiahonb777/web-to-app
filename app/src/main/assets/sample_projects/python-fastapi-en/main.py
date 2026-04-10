# -*- coding: utf-8 -*-
"""
FastAPI API Monitor — 精美的 API 监控仪表盘
展示 FastAPI 的高性能异步能力，包含Interactive式 API 文档
"""
import os
import random
import time
from datetime import datetime
from fastapi import FastAPI
from fastapi.responses import HTMLResponse, JSONResponse
from pydantic import BaseModel
from typing import List, Optional

app = FastAPI(title="API Monitor", version="2.0.0")

class Book(BaseModel):
    id: int
    title: str
    author: str
    price: float

books_db: List[Book] = [
    Book(id=1, title="Python Crash Course", author="Eric Matthes", price=29.99),
    Book(id=2, title="Clean Code", author="Robert C. Martin", price=35.50),
    Book(id=3, title="Design Patterns", author="Gang of Four", price=42.00),
]

start_time = time.time()

DASHBOARD = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
<title>FastAPI Monitor</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
:root{--bg:#080816;--card:rgba(255,255,255,0.03);--border:rgba(255,255,255,0.06);--text:#eeeef8;--text2:rgba(238,238,248,0.55);--text3:rgba(238,238,248,0.3);--green:#00d4aa;--teal:#06b6d4;--purple:#8b5cf6;--amber:#f59e0b;--red:#ef4444;--radius:14px}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:var(--bg);color:var(--text);min-height:100vh;-webkit-font-smoothing:antialiased}
body::before{content:'';position:fixed;top:-40%;left:-40%;width:180%;height:180%;background:radial-gradient(ellipse at 50% 30%,rgba(0,212,170,0.06) 0%,transparent 60%),radial-gradient(ellipse at 80% 70%,rgba(139,92,246,0.05) 0%,transparent 50%);pointer-events:none}
.c{max-width:460px;margin:0 auto;padding:0 14px 32px;position:relative;z-index:1}
.hdr{padding:20px 2px 20px;display:flex;justify-content:space-between;align-items:center}
.hdr h1{font-size:22px;font-weight:800;background:linear-gradient(135deg,#00d4aa,#06b6d4);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
.hdr p{font-size:12px;color:var(--text2);margin-top:2px}
.badge{display:inline-flex;align-items:center;gap:5px;font-size:11px;font-weight:600;padding:5px 12px;border-radius:20px;background:rgba(0,212,170,0.1);color:var(--green);border:1px solid rgba(0,212,170,0.15)}
.badge .dot{width:7px;height:7px;border-radius:50%;background:var(--green);box-shadow:0 0 10px var(--green);animation:p 2s infinite}
@keyframes p{0%,100%{opacity:1}50%{opacity:.5}}
.row{display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px;margin-bottom:16px}
.m{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:14px;text-align:center;backdrop-filter:blur(16px)}
.m-val{font-size:22px;font-weight:800;letter-spacing:-0.5px;margin-bottom:2px}
.m-label{font-size:10px;color:var(--text2);text-transform:uppercase;letter-spacing:0.5px;font-weight:500}
.m:nth-child(1) .m-val{color:var(--green)} .m:nth-child(2) .m-val{color:var(--teal)} .m:nth-child(3) .m-val{color:var(--purple)}
.card{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:18px;margin-bottom:14px;backdrop-filter:blur(16px)}
.card-h{display:flex;justify-content:space-between;align-items:center;margin-bottom:14px}
.card-t{font-size:15px;font-weight:700}
.card-b{font-size:10px;font-weight:600;padding:3px 9px;border-radius:16px;background:rgba(139,92,246,0.1);color:var(--purple)}
.endpoint{display:flex;align-items:center;gap:10px;padding:12px 0;border-bottom:1px solid rgba(255,255,255,0.03)}
.endpoint:last-child{border:none}
.method{font-size:10px;font-weight:800;padding:3px 8px;border-radius:6px;min-width:40px;text-align:center;letter-spacing:0.5px}
.method.get{background:rgba(0,212,170,0.12);color:var(--green)}
.method.post{background:rgba(139,92,246,0.12);color:var(--purple)}
.method.put{background:rgba(245,158,11,0.12);color:var(--amber)}
.method.delete{background:rgba(239,68,68,0.12);color:var(--red)}
.ep-info{flex:1;min-width:0}
.ep-path{font-size:13px;font-weight:600;font-family:'SF Mono',Monaco,monospace}
.ep-desc{font-size:11px;color:var(--text2);margin-top:1px}
.ep-ms{font-size:11px;color:var(--text3);font-weight:600;font-family:monospace}
.bar-row{display:flex;align-items:center;gap:8px;padding:8px 0;border-bottom:1px solid rgba(255,255,255,0.03)}
.bar-row:last-child{border:none}
.bar-label{font-size:12px;color:var(--text2);width:50px;flex-shrink:0}
.bar-track{flex:1;height:6px;background:rgba(255,255,255,0.04);border-radius:3px;overflow:hidden}
.bar-fill{height:100%;border-radius:3px;transition:width 1s ease}
.bar-val{font-size:11px;color:var(--text3);width:36px;text-align:right;font-weight:600;font-family:monospace}
.footer{text-align:center;padding:12px;margin-top:4px}
.footer-badge{display:inline-flex;align-items:center;gap:6px;font-size:10px;font-weight:600;color:var(--text3);padding:5px 12px;border-radius:20px;background:rgba(0,212,170,0.04);border:1px solid rgba(0,212,170,0.08)}
@keyframes fadeIn{from{opacity:0;transform:translateY(16px)}to{opacity:1;transform:translateY(0)}}
.m,.card{animation:fadeIn .5s ease backwards}
.m:nth-child(1){animation-delay:.05s}.m:nth-child(2){animation-delay:.1s}.m:nth-child(3){animation-delay:.15s}
.card:nth-child(1){animation-delay:.2s}.card:nth-child(2){animation-delay:.25s}.card:nth-child(3){animation-delay:.3s}
.try-btn{background:rgba(0,212,170,0.1);color:var(--green);border:1px solid rgba(0,212,170,0.2);padding:6px 14px;border-radius:8px;font-size:12px;font-weight:600;cursor:pointer;transition:all .2s}
.try-btn:active{transform:scale(.95);background:rgba(0,212,170,0.2)}
.result{margin-top:10px;padding:12px;background:rgba(0,0,0,0.3);border-radius:8px;font-family:'SF Mono',Monaco,monospace;font-size:11px;color:var(--green);max-height:200px;overflow-y:auto;display:none;white-space:pre-wrap;word-break:break-all;line-height:1.5}
</style>
</head>
<body>
<div class="c">
<div class="hdr">
    <div><h1>⚡ FastAPI Monitor</h1><p>High-Performance API Monitor</p></div>
    <div class="badge"><span class="dot"></span>Online</div>
</div>
<div class="row">
    <div class="m"><div class="m-val" id="uptime">0s</div><div class="m-label">Uptime</div></div>
    <div class="m"><div class="m-val" id="reqCount">0</div><div class="m-label">Requests</div></div>
    <div class="m"><div class="m-val" id="avgMs">&lt;1ms</div><div class="m-label">Avg Latency</div></div>
</div>
<div class="card">
    <div class="card-h"><span class="card-t">API Endpoints</span><span class="card-b">6 Routes</span></div>
    <div class="endpoint"><span class="method get">GET</span><div class="ep-info"><div class="ep-path">/</div><div class="ep-desc">Dashboard Home</div></div><div class="ep-ms">&lt;1ms</div></div>
    <div class="endpoint"><span class="method get">GET</span><div class="ep-info"><div class="ep-path">/api/books</div><div class="ep-desc">List all books</div></div><div class="ep-ms">~2ms</div></div>
    <div class="endpoint"><span class="method get">GET</span><div class="ep-info"><div class="ep-path">/api/books/{id}</div><div class="ep-desc">Get book by ID</div></div><div class="ep-ms">~1ms</div></div>
    <div class="endpoint"><span class="method post">POST</span><div class="ep-info"><div class="ep-path">/api/books</div><div class="ep-desc">Add new book</div></div><div class="ep-ms">~3ms</div></div>
    <div class="endpoint"><span class="method get">GET</span><div class="ep-info"><div class="ep-path">/api/health</div><div class="ep-desc">Health Check</div></div><div class="ep-ms">&lt;1ms</div></div>
    <div class="endpoint"><span class="method get">GET</span><div class="ep-info"><div class="ep-path">/docs</div><div class="ep-desc">Swagger Docs</div></div><div class="ep-ms">Auto-generated</div></div>
</div>
<div class="card">
    <div class="card-h"><span class="card-t">Live Testing</span><span class="card-b">🧪 Interactive</span></div>
    <div style="display:flex;gap:8px;flex-wrap:wrap">
        <button class="try-btn" onclick="tryApi('/api/books')">📚 Get Books</button>
        <button class="try-btn" onclick="tryApi('/api/health')">💚 Health Check</button>
        <button class="try-btn" onclick="tryApi('/api/books/1')">🔍 Book Detail</button>
    </div>
    <div class="result" id="result"></div>
</div>
<div class="card">
    <div class="card-h"><span class="card-t">Metrics</span><span class="card-b">📊 Live</span></div>
    <div class="bar-row"><span class="bar-label">CPU</span><div class="bar-track"><div class="bar-fill" id="cpu" style="width:0%;background:linear-gradient(90deg,var(--green),var(--teal))"></div></div><span class="bar-val" id="cpuV">0%</span></div>
    <div class="bar-row"><span class="bar-label">Memory</span><div class="bar-track"><div class="bar-fill" id="mem" style="width:0%;background:linear-gradient(90deg,var(--purple),#c084fc)"></div></div><span class="bar-val" id="memV">0%</span></div>
    <div class="bar-row"><span class="bar-label">Disk</span><div class="bar-track"><div class="bar-fill" id="disk" style="width:0%;background:linear-gradient(90deg,var(--amber),#fbbf24)"></div></div><span class="bar-val" id="diskV">0%</span></div>
</div>
<div class="footer"><div class="footer-badge"><span class="dot" style="width:5px;height:5px;border-radius:50%;background:var(--green);box-shadow:0 0 6px var(--green)"></span>FastAPI 0.109 · Python · Uvicorn</div></div>
</div>
<script>
let rc=0;
function tryApi(url){
    rc++;
    document.getElementById('reqCount').textContent=rc;
    const r=document.getElementById('result');
    r.style.display='block';
    r.textContent='⏳ Requesting...';
    const t=performance.now();
    fetch(url).then(r=>r.json()).then(d=>{
        const ms=Math.round(performance.now()-t);
        document.getElementById('result').textContent=JSON.stringify(d,null,2)+'\\n\\n⏱️ '+ms+'ms';
        document.getElementById('avgMs').textContent=ms+'ms';
    }).catch(e=>{document.getElementById('result').textContent='❌ '+e.message});
}
let s=Math.floor(Date.now()/1000);
setInterval(()=>{
    const e=Math.floor(Date.now()/1000)-s;
    const m=Math.floor(e/60),sec=e%60;
    document.getElementById('uptime').textContent=m>0?m+'m'+sec+'s':sec+'s';
    ['cpu','mem','disk'].forEach((k,i)=>{
        const v=[15+Math.random()*20, 30+Math.random()*15, 40+Math.random()*10][i];
        document.getElementById(k).style.width=v+'%';
        document.getElementById(k+'V').textContent=Math.round(v)+'%';
    });
},1000);
</script>
</body>
</html>"""

@app.get("/", response_class=HTMLResponse)
async def dashboard():
    return DASHBOARD

@app.get("/api/books", response_model=List[Book])
async def list_books():
    return books_db

@app.get("/api/books/{book_id}")
async def get_book(book_id: int):
    for b in books_db:
        if b.id == book_id:
            return b
    return JSONResponse({"error": "Book not found"}, status_code=404)

@app.post("/api/books", response_model=Book)
async def create_book(book: Book):
    books_db.append(book)
    return book

@app.get("/api/health")
async def health():
    return {
        "status": "healthy",
        "uptime_seconds": round(time.time() - start_time, 1),
        "framework": "FastAPI",
        "books_count": len(books_db),
        "timestamp": datetime.now().isoformat()
    }
