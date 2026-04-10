/**
 * 待办事项应用 — 暗色主题版
 * Node.js 内置 http 模块，零依赖
 */
const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3000;
const DATA_FILE = path.join(__dirname, 'todos.json');

const loadTodos = () => {
  try { if (fs.existsSync(DATA_FILE)) return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8')); } catch (e) { }
  return [
    { id: 1, text: '欢迎使用待办事项 👋', done: false, createdAt: Date.now() },
    { id: 2, text: '点击圆圈完成任务 ✓', done: true, createdAt: Date.now() - 1000 },
    { id: 3, text: '点击 × 删除任务', done: false, createdAt: Date.now() - 2000 },
    { id: 4, text: '支持数据持久化', done: false, createdAt: Date.now() - 3000 }
  ];
};
const saveTodos = (todos) => fs.writeFileSync(DATA_FILE, JSON.stringify(todos, null, 2));
let todos = loadTodos();
let nextId = Math.max(...todos.map(t => t.id), 0) + 1;

function parseBody(req) {
  return new Promise((resolve) => {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => { try { resolve(JSON.parse(body)); } catch (e) { resolve({}); } });
  });
}

function sendJson(res, data, status = 200) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(JSON.stringify(data));
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  const p = url.pathname, m = req.method;

  if (m === 'GET' && p === '/') {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    return res.end(getPage());
  }
  if (m === 'GET' && p === '/api/todos') return sendJson(res, { success: true, data: todos });
  if (m === 'POST' && p === '/api/todos') {
    const { text } = await parseBody(req);
    if (!text?.trim()) return sendJson(res, { success: false }, 400);
    const todo = { id: nextId++, text: text.trim(), done: false, createdAt: Date.now() };
    todos.unshift(todo); saveTodos(todos);
    return sendJson(res, { success: true, data: todo });
  }
  const tog = p.match(/^\/api\/todos\/(\d+)\/toggle$/);
  if (m === 'PATCH' && tog) {
    const todo = todos.find(t => t.id === parseInt(tog[1]));
    if (!todo) return sendJson(res, { success: false }, 404);
    todo.done = !todo.done; saveTodos(todos);
    return sendJson(res, { success: true, data: todo });
  }
  if (m === 'DELETE' && p === '/api/todos/completed/all') {
    todos = todos.filter(t => !t.done); saveTodos(todos);
    return sendJson(res, { success: true });
  }
  const del = p.match(/^\/api\/todos\/(\d+)$/);
  if (m === 'DELETE' && del) {
    const i = todos.findIndex(t => t.id === parseInt(del[1]));
    if (i === -1) return sendJson(res, { success: false }, 404);
    todos.splice(i, 1); saveTodos(todos);
    return sendJson(res, { success: true });
  }
  sendJson(res, { error: 'Not Found' }, 404);
});

server.listen(PORT, '0.0.0.0', () => console.log(`✅ Todo on http://0.0.0.0:${PORT}`));

function getPage() {
  return `<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0,user-scalable=no"><title>待办事项</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
:root{--bg:#0a0a1a;--card:rgba(255,255,255,0.04);--border:rgba(255,255,255,0.07);--text:#f0f0ff;--text2:rgba(240,240,255,0.55);--text3:rgba(240,240,255,0.3);--purple:#a855f7;--radius:14px}
body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;background:var(--bg);color:var(--text);min-height:100vh}
body::before{content:'';position:fixed;top:-40%;left:-40%;width:180%;height:180%;background:radial-gradient(ellipse at 50% 30%,rgba(168,85,247,0.06) 0%,transparent 55%);pointer-events:none}
.c{max-width:460px;margin:0 auto;padding:0 14px 32px;position:relative;z-index:1}
.hdr{text-align:center;padding:28px 0 20px}
.hdr h1{font-size:24px;font-weight:800;background:linear-gradient(135deg,#a855f7,#ec4899);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
.hdr p{font-size:12px;color:var(--text2);margin-top:4px}
.stats{display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px;margin-bottom:16px}
.stat{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:14px;text-align:center}
.stat-val{font-size:22px;font-weight:800;color:var(--purple)}
.stat-label{font-size:10px;color:var(--text2);text-transform:uppercase;letter-spacing:.5px;margin-top:2px}
.input-row{display:flex;gap:8px;margin-bottom:16px}
.input-row input{flex:1;background:var(--card);border:1px solid var(--border);border-radius:10px;padding:12px 14px;color:var(--text);font-size:14px;outline:none}
.input-row input:focus{border-color:var(--purple)}
.input-row input::placeholder{color:var(--text3)}
.add-btn{background:linear-gradient(135deg,#a855f7,#ec4899);color:#fff;border:none;border-radius:10px;padding:0 18px;font-size:18px;font-weight:800;cursor:pointer}
.add-btn:active{transform:scale(.93)}
.filter-row{display:flex;gap:6px;margin-bottom:14px}
.filter{flex:1;padding:8px;border-radius:8px;background:var(--card);border:1px solid var(--border);color:var(--text2);font-size:12px;font-weight:600;text-align:center;cursor:pointer}
.filter.active{background:rgba(168,85,247,0.1);color:var(--purple);border-color:rgba(168,85,247,0.2)}
.todo-list{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);overflow:hidden}
.todo{display:flex;align-items:center;gap:12px;padding:14px 16px;border-bottom:1px solid rgba(255,255,255,0.03);cursor:pointer}
.todo:last-child{border:none}
.todo.done .t-text{text-decoration:line-through;opacity:.4}
.check{width:22px;height:22px;border-radius:50%;border:2px solid var(--border);display:flex;align-items:center;justify-content:center;font-size:12px;flex-shrink:0;transition:all .2s}
.todo.done .check{background:var(--purple);border-color:var(--purple);color:#fff}
.t-text{flex:1;font-size:14px;font-weight:500}
.del{width:28px;height:28px;border-radius:8px;background:rgba(239,68,68,0.1);border:none;color:#ef4444;font-size:13px;cursor:pointer;display:flex;align-items:center;justify-content:center}
.empty{text-align:center;padding:32px;color:var(--text3)}
.ft{text-align:center;padding:16px}
.ft-b{font-size:10px;font-weight:600;color:var(--text3);padding:5px 12px;border-radius:20px;background:rgba(168,85,247,0.04);border:1px solid rgba(168,85,247,0.08)}
.clr{background:rgba(168,85,247,0.1);color:var(--purple);border:1px solid rgba(168,85,247,0.2);padding:8px 16px;border-radius:8px;font-size:12px;font-weight:600;cursor:pointer;margin-top:12px}
.clr:disabled{opacity:.3;cursor:not-allowed}
@keyframes fadeIn{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:translateY(0)}}
.stat,.todo{animation:fadeIn .4s ease backwards}
</style></head><body>
<div class="c">
<div class="hdr"><h1>📝 待办事项</h1><p>Express + Node.js 驱动</p></div>
<div class="stats"><div class="stat"><div class="stat-val" id="total">0</div><div class="stat-label">全部</div></div><div class="stat"><div class="stat-val" id="active">0</div><div class="stat-label">待办</div></div><div class="stat"><div class="stat-val" id="done">0</div><div class="stat-label">完成</div></div></div>
<div class="input-row"><input type="text" id="inp" placeholder="添加新任务..." maxlength="100" onkeydown="if(event.key==='Enter')addTodo()"><button class="add-btn" onclick="addTodo()">+</button></div>
<div class="filter-row"><div class="filter active" onclick="setFilter('all',this)">全部</div><div class="filter" onclick="setFilter('active',this)">待办</div><div class="filter" onclick="setFilter('done',this)">已完成</div></div>
<div class="todo-list" id="list"></div>
<div style="text-align:center"><button class="clr" id="clrBtn" onclick="clearDone()" disabled>清除已完成</button></div>
<div class="ft"><div class="ft-b">📝 Express · Node.js · WebToApp</div></div>
</div>
<script>
var todos=[],filter='all';
function load(){fetch('/api/todos').then(function(r){return r.json()}).then(function(j){todos=j.data||[];render()})}
function render(){
var f=todos.filter(function(t){return filter==='all'?true:filter==='active'?!t.done:t.done});
var list=document.getElementById('list');
document.getElementById('total').textContent=todos.length;
document.getElementById('active').textContent=todos.filter(function(t){return !t.done}).length;
var dn=todos.filter(function(t){return t.done}).length;
document.getElementById('done').textContent=dn;
document.getElementById('clrBtn').disabled=dn===0;
if(f.length===0){list.innerHTML='<div class="empty">🎉 '+(filter==='all'?'暂无任务':'没有匹配')+'</div>';return}
list.innerHTML=f.map(function(t,i){return '<div class="todo'+(t.done?' done':'')+'" style="animation-delay:'+(i*0.04)+'s"><div class="check" onclick="tog('+t.id+')">'+(t.done?'✓':'')+'</div><div class="t-text" onclick="tog('+t.id+')">'+esc(t.text)+'</div><button class="del" onclick="del('+t.id+')">×</button></div>'}).join('')}
function esc(s){var d=document.createElement('div');d.textContent=s;return d.innerHTML}
function addTodo(){var v=document.getElementById('inp').value.trim();if(!v)return;fetch('/api/todos',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({text:v})}).then(function(r){return r.json()}).then(function(j){if(j.success){todos.unshift(j.data);document.getElementById('inp').value='';render()}})}
function tog(id){fetch('/api/todos/'+id+'/toggle',{method:'PATCH'}).then(function(r){return r.json()}).then(function(j){if(j.success){var t=todos.find(function(x){return x.id===id});if(t)t.done=j.data.done;render()}})}
function del(id){fetch('/api/todos/'+id,{method:'DELETE'}).then(function(r){if(r.ok){todos=todos.filter(function(t){return t.id!==id});render()}})}
function clearDone(){fetch('/api/todos/completed/all',{method:'DELETE'}).then(function(r){if(r.ok){todos=todos.filter(function(t){return !t.done});render()}})}
function setFilter(f,el){filter=f;document.querySelectorAll('.filter').forEach(function(e){e.classList.remove('active')});el.classList.add('active');render()}
load();
</script></body></html>`;
}
