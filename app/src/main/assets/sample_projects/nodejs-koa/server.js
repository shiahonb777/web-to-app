/**
 * Markdown 笔记应用 — 暗色主题版
 * Node.js 内置模块，零依赖
 */
const http = require('http');
const fs = require('fs');
const path = require('path');
const PORT = process.env.PORT || 3000;
const DATA_FILE = path.join(__dirname, 'notes.json');

const loadNotes = () => {
  try { if (fs.existsSync(DATA_FILE)) return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8')); } catch (e) { }
  return [
    { id: 1, title: '欢迎使用笔记应用', content: '# 欢迎\n\n这是一个支持 **Markdown** 的笔记应用。\n\n## 功能\n- 创建和编辑笔记\n- Markdown 预览\n- 搜索笔记\n- 本地持久化', createdAt: Date.now(), updatedAt: Date.now() },
    { id: 2, title: 'Markdown 语法', content: '# Markdown\n\n## 强调\n- **粗体**: `**文字**`\n- *斜体*: `*文字*`\n\n## 列表\n- 无序列表用 -\n1. 有序列表用数字\n\n## 代码\n`code`', createdAt: Date.now() - 3600000, updatedAt: Date.now() - 3600000 }
  ];
};
const saveNotes = (notes) => fs.writeFileSync(DATA_FILE, JSON.stringify(notes, null, 2));
let notes = loadNotes();
let nextId = Math.max(...notes.map(n => n.id), 0) + 1;

function parseBody(req) { return new Promise(r => { let b = ''; req.on('data', c => b += c); req.on('end', () => { try { r(JSON.parse(b)) } catch (e) { r({}) } }); }); }
function sendJson(res, data, s = 200) { res.writeHead(s, { 'Content-Type': 'application/json; charset=utf-8' }); res.end(JSON.stringify(data)); }

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  const p = url.pathname, m = req.method;

  if (m === 'GET' && p === '/') { res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' }); return res.end(getPage()); }
  if (m === 'GET' && p === '/api/notes') {
    const q = url.searchParams.get('q');
    let result = notes;
    if (q) { const ql = q.toLowerCase(); result = notes.filter(n => n.title.toLowerCase().includes(ql) || n.content.toLowerCase().includes(ql)); }
    return sendJson(res, { success: true, data: result.map(n => ({ ...n, preview: n.content.substring(0, 80) })) });
  }
  if (m === 'POST' && p === '/api/notes') {
    const { title, content } = await parseBody(req);
    const note = { id: nextId++, title: title || '无标题', content: content || '', createdAt: Date.now(), updatedAt: Date.now() };
    notes.unshift(note); saveNotes(notes);
    return sendJson(res, { success: true, data: note });
  }
  const nm = p.match(/^\/api\/notes\/(\d+)$/);
  if (m === 'GET' && nm) { const n = notes.find(x => x.id === parseInt(nm[1])); return n ? sendJson(res, { success: true, data: n }) : sendJson(res, { success: false }, 404); }
  if (m === 'PUT' && nm) {
    const n = notes.find(x => x.id === parseInt(nm[1]));
    if (!n) return sendJson(res, { success: false }, 404);
    const { title, content } = await parseBody(req);
    if (title !== undefined) n.title = title; if (content !== undefined) n.content = content; n.updatedAt = Date.now();
    saveNotes(notes); return sendJson(res, { success: true, data: n });
  }
  if (m === 'DELETE' && nm) {
    const i = notes.findIndex(x => x.id === parseInt(nm[1]));
    if (i === -1) return sendJson(res, { success: false }, 404);
    notes.splice(i, 1); saveNotes(notes); return sendJson(res, { success: true });
  }
  sendJson(res, { error: 'Not Found' }, 404);
});
server.listen(PORT, '0.0.0.0', () => console.log('Notes on http://0.0.0.0:' + PORT));

function getPage() {
  return `<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0,user-scalable=no"><title>Markdown 笔记</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
:root{--bg:#0a0a1a;--card:rgba(255,255,255,0.04);--border:rgba(255,255,255,0.07);--text:#f0f0ff;--text2:rgba(240,240,255,0.55);--text3:rgba(240,240,255,0.3);--green:#10b981;--radius:14px}
body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;background:var(--bg);color:var(--text);min-height:100vh}
body::before{content:'';position:fixed;top:-40%;left:-40%;width:180%;height:180%;background:radial-gradient(ellipse at 50% 30%,rgba(16,185,129,0.05) 0%,transparent 55%);pointer-events:none}
.c{max-width:460px;margin:0 auto;padding:0 14px 32px;position:relative;z-index:1}
.hdr{padding:20px 2px;display:flex;justify-content:space-between;align-items:center}
.hdr h1{font-size:22px;font-weight:800;background:linear-gradient(135deg,#10b981,#34d399);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
.hdr p{font-size:12px;color:var(--text2);margin-top:2px}
.new-btn{background:linear-gradient(135deg,#10b981,#059669);color:#fff;border:none;border-radius:10px;padding:8px 16px;font-size:12px;font-weight:700;cursor:pointer}
.new-btn:active{transform:scale(.93)}
.search{margin-bottom:14px}
.search input{width:100%;background:var(--card);border:1px solid var(--border);border-radius:10px;padding:10px 14px;color:var(--text);font-size:14px;outline:none}
.search input:focus{border-color:var(--green)}
.search input::placeholder{color:var(--text3)}
.notes{display:flex;flex-direction:column;gap:10px}
.note{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:16px;cursor:pointer;position:relative;overflow:hidden;transition:all .2s}
.note:active{transform:scale(.98)}
.note::before{content:'';position:absolute;top:0;left:0;width:3px;height:100%}
.note:nth-child(1)::before{background:var(--green)}.note:nth-child(2)::before{background:#8b5cf6}.note:nth-child(3)::before{background:#f59e0b}.note:nth-child(4)::before{background:#06b6d4}.note:nth-child(5)::before{background:#ec4899}
.note-title{font-size:14px;font-weight:700;margin-bottom:4px;padding-right:30px}
.note-preview{font-size:12px;color:var(--text2);line-height:1.5;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.note-time{font-size:10px;color:var(--text3);margin-top:6px}
.note-del{position:absolute;top:12px;right:12px;width:24px;height:24px;border-radius:6px;background:rgba(239,68,68,0.1);border:none;color:#ef4444;font-size:12px;cursor:pointer;display:flex;align-items:center;justify-content:center}
.editor{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:16px;margin-bottom:14px;display:none}
.editor input{width:100%;background:transparent;border:none;border-bottom:1px solid var(--border);padding:8px 0;color:var(--text);font-size:16px;font-weight:700;outline:none;margin-bottom:10px}
.editor textarea{width:100%;background:transparent;border:1px solid var(--border);border-radius:8px;padding:10px;color:var(--text);font-size:13px;font-family:monospace;line-height:1.6;resize:none;outline:none;min-height:150px}
.editor textarea:focus{border-color:var(--green)}
.editor-actions{display:flex;gap:8px;margin-top:10px}
.save-btn{background:var(--green);color:#fff;border:none;border-radius:8px;padding:8px 16px;font-size:12px;font-weight:700;cursor:pointer}
.cancel-btn{background:var(--card);color:var(--text2);border:1px solid var(--border);border-radius:8px;padding:8px 16px;font-size:12px;font-weight:600;cursor:pointer}
.preview-area{margin-top:10px;padding:12px;background:rgba(0,0,0,0.2);border-radius:8px;font-size:13px;line-height:1.8;display:none}
.preview-area h1,.preview-area h2,.preview-area h3{margin:10px 0 5px}.preview-area p{margin:5px 0}.preview-area code{background:rgba(255,255,255,0.05);padding:2px 6px;border-radius:4px;font-family:monospace}.preview-area ul,.preview-area ol{margin:5px 0 5px 20px}.preview-area blockquote{border-left:3px solid var(--green);padding-left:12px;color:var(--text2);margin:8px 0}
.tabs{display:flex;gap:4px;margin-bottom:10px}
.tab{padding:6px 14px;border-radius:8px;background:transparent;border:1px solid var(--border);color:var(--text2);font-size:12px;font-weight:600;cursor:pointer}
.tab.active{background:rgba(16,185,129,0.1);color:var(--green);border-color:rgba(16,185,129,0.2)}
.empty{text-align:center;padding:40px;color:var(--text3)}
.ft{text-align:center;padding:16px}
.ft-b{font-size:10px;font-weight:600;color:var(--text3);padding:5px 12px;border-radius:20px;background:rgba(16,185,129,0.04);border:1px solid rgba(16,185,129,0.08)}
@keyframes fadeIn{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:translateY(0)}}
.note{animation:fadeIn .4s ease backwards}
</style></head><body>
<div class="c">
<div class="hdr"><div><h1>📝 Markdown 笔记</h1><p>Koa · Node.js 驱动</p></div><button class="new-btn" onclick="createNote()">+ 新建</button></div>
<div class="search"><input type="text" id="searchInput" placeholder="搜索笔记..." oninput="searchNotes()"></div>
<div class="editor" id="editor">
<input type="text" id="eTitle" placeholder="标题">
<div class="tabs"><div class="tab active" onclick="switchTab('edit')">编辑</div><div class="tab" onclick="switchTab('preview')">预览</div></div>
<textarea id="eContent" placeholder="写点什么..." oninput="updatePreview()"></textarea>
<div class="preview-area" id="previewArea"></div>
<div class="editor-actions"><button class="save-btn" onclick="saveNote()">保存</button><button class="cancel-btn" onclick="closeEditor()">取消</button></div>
</div>
<div class="notes" id="noteList"></div>
<div class="ft"><div class="ft-b">📝 Koa · Node.js · WebToApp</div></div>
</div>
<script>
var notes=[],currentId=null;
function load(){var q=document.getElementById('searchInput').value;fetch('/api/notes'+(q?'?q='+encodeURIComponent(q):'')).then(function(r){return r.json()}).then(function(j){notes=j.data||[];renderList()})}
function renderList(){var l=document.getElementById('noteList');if(notes.length===0){l.innerHTML='<div class="empty">📝 暂无笔记，点击新建开始</div>';return}l.innerHTML=notes.map(function(n,i){return '<div class="note" style="animation-delay:'+(i*.05)+'s" onclick="editNote('+n.id+')"><button class="note-del" onclick="event.stopPropagation();delNote('+n.id+')">×</button><div class="note-title">'+esc(n.title||'无标题')+'</div><div class="note-preview">'+esc(n.preview||'')+'</div><div class="note-time">'+(new Date(n.updatedAt).toLocaleString())+'</div></div>'}).join('')}
function esc(s){var d=document.createElement('div');d.textContent=s;return d.innerHTML}
function createNote(){fetch('/api/notes',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({title:'新笔记',content:''})}).then(function(r){return r.json()}).then(function(j){if(j.success){load();editNote(j.data.id)}})}
function editNote(id){fetch('/api/notes/'+id).then(function(r){return r.json()}).then(function(j){if(j.success){currentId=j.data.id;document.getElementById('eTitle').value=j.data.title;document.getElementById('eContent').value=j.data.content;document.getElementById('editor').style.display='block';switchTab('edit')}})}
function saveNote(){if(!currentId)return;fetch('/api/notes/'+currentId,{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify({title:document.getElementById('eTitle').value,content:document.getElementById('eContent').value})}).then(function(){closeEditor();load()})}
function closeEditor(){document.getElementById('editor').style.display='none';currentId=null}
function delNote(id){fetch('/api/notes/'+id,{method:'DELETE'}).then(function(){if(currentId===id)closeEditor();load()})}
function searchNotes(){load()}
function switchTab(t){document.querySelectorAll('.tab').forEach(function(e){e.classList.remove('active')});if(t==='edit'){document.querySelector('.tab:first-child').classList.add('active');document.getElementById('eContent').style.display='block';document.getElementById('previewArea').style.display='none'}else{document.querySelector('.tab:last-child').classList.add('active');document.getElementById('eContent').style.display='none';document.getElementById('previewArea').style.display='block';updatePreview()}}
function updatePreview(){var c=document.getElementById('eContent').value;document.getElementById('previewArea').innerHTML=simpleMarkdown(c)}
function simpleMarkdown(s){return s.replace(/^### (.*$)/gm,'<h3>$1</h3>').replace(/^## (.*$)/gm,'<h2>$1</h2>').replace(/^# (.*$)/gm,'<h1>$1</h1>').replace(/\\*\\*(.*?)\\*\\*/g,'<strong>$1</strong>').replace(/\\*(.*?)\\*/g,'<em>$1</em>').replace(/\`(.*?)\`/g,'<code>$1</code>').replace(/^- (.*$)/gm,'<li>$1</li>').replace(/^> (.*$)/gm,'<blockquote>$1</blockquote>').replace(/\\n/g,'<br>')}
load();
</script></body></html>`;
}
