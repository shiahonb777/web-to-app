<?php
/**
 * Mini Slim — 自包含 REST API 仪表盘
 * 
 * 模仿 Slim 4 风格，无需 Composer 依赖
 * 特性: RESTful API | 交互式测试面板 | SQLite 存储 | JSON 响应
 */
error_reporting(E_ALL & ~E_NOTICE & ~E_WARNING);

$basePath = dirname(__DIR__);
$uri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

// ==================== 数据库 ====================
$storagePath = $basePath . '/storage';
if (!is_dir($storagePath)) @mkdir($storagePath, 0755, true);

$pdo = null;
try {
    $pdo = new PDO('sqlite:' . $storagePath . '/tasks.sqlite');
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->exec('CREATE TABLE IF NOT EXISTS tasks (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title TEXT NOT NULL,
        done INTEGER DEFAULT 0,
        priority TEXT DEFAULT "medium",
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )');
    if ($pdo->query('SELECT COUNT(*) FROM tasks')->fetchColumn() == 0) {
        $seed = [
            ['设计 REST API 接口', 1, 'high'],
            ['实现数据库 CRUD', 1, 'high'],
            ['添加输入验证', 0, 'medium'],
            ['编写 API 文档', 0, 'medium'],
            ['性能优化与缓存', 0, 'low'],
        ];
        $stmt = $pdo->prepare('INSERT INTO tasks (title,done,priority) VALUES (?,?,?)');
        foreach ($seed as $t) $stmt->execute($t);
    }
} catch (Exception $e) { $pdo = null; }

// ==================== API 路由 ====================
function jsonResponse(int $code, $data): void {
    http_response_code($code);
    header('Content-Type: application/json; charset=utf-8');
    header('Access-Control-Allow-Origin: *');
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

function getInput(): array {
    // 优先使用路由服务器提供的原始请求体（CLI SAPI 下 php://input 可能不可用）
    $raw = $GLOBALS['__HTTP_RAW_BODY'] ?? file_get_contents('php://input');
    return json_decode($raw, true) ?: $_POST;
}

// API 路由
if (str_starts_with($uri, '/api/')) {
    header('Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE,OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type');
    if ($method === 'OPTIONS') { http_response_code(204); exit; }

    // GET /api/tasks
    if ($method === 'GET' && $uri === '/api/tasks') {
        $filter = $_GET['status'] ?? '';
        $sql = 'SELECT * FROM tasks';
        if ($filter === 'done') $sql .= ' WHERE done=1';
        elseif ($filter === 'pending') $sql .= ' WHERE done=0';
        $sql .= ' ORDER BY id DESC';
        $tasks = $pdo ? $pdo->query($sql)->fetchAll(PDO::FETCH_ASSOC) : [];
        foreach ($tasks as &$t) $t['done'] = (bool)$t['done'];
        jsonResponse(200, ['tasks' => $tasks, 'total' => count($tasks)]);
    }

    // POST /api/tasks
    if ($method === 'POST' && $uri === '/api/tasks') {
        $input = getInput();
        $title = trim($input['title'] ?? '');
        $priority = $input['priority'] ?? 'medium';
        if (!$title) jsonResponse(422, ['error' => '标题不能为空']);
        if ($pdo) {
            $stmt = $pdo->prepare('INSERT INTO tasks (title,priority) VALUES (?,?)');
            $stmt->execute([$title, $priority]);
            $id = $pdo->lastInsertId();
            $task = $pdo->query("SELECT * FROM tasks WHERE id=$id")->fetch(PDO::FETCH_ASSOC);
            $task['done'] = (bool)$task['done'];
            jsonResponse(201, ['task' => $task]);
        }
        jsonResponse(500, ['error' => '数据库不可用']);
    }

    // PATCH /api/tasks/{id}/toggle
    if ($method === 'PATCH' && preg_match('#^/api/tasks/(\d+)/toggle$#', $uri, $m)) {
        if ($pdo) {
            $pdo->exec("UPDATE tasks SET done = NOT done WHERE id={$m[1]}");
            $task = $pdo->query("SELECT * FROM tasks WHERE id={$m[1]}")->fetch(PDO::FETCH_ASSOC);
            if ($task) { $task['done'] = (bool)$task['done']; jsonResponse(200, ['task' => $task]); }
        }
        jsonResponse(404, ['error' => '任务不存在']);
    }

    // DELETE /api/tasks/{id}
    if ($method === 'DELETE' && preg_match('#^/api/tasks/(\d+)$#', $uri, $m)) {
        if ($pdo) {
            $pdo->exec("DELETE FROM tasks WHERE id={$m[1]}");
            jsonResponse(200, ['deleted' => true]);
        }
        jsonResponse(500, ['error' => '数据库不可用']);
    }

    // GET /api/info
    if ($method === 'GET' && $uri === '/api/info') {
        jsonResponse(200, [
            'server' => 'Mini Slim API',
            'php' => phpversion(),
            'sapi' => php_sapi_name(),
            'os' => PHP_OS,
            'extensions' => get_loaded_extensions(),
            'memory' => [
                'usage' => memory_get_usage(true),
                'peak' => memory_get_peak_usage(true),
            ],
        ]);
    }

    jsonResponse(404, ['error' => '接口不存在: ' . $method . ' ' . $uri]);
}

// ==================== 仪表盘页面 ====================
$taskCount = $pdo ? $pdo->query('SELECT COUNT(*) FROM tasks')->fetchColumn() : 0;
$doneCount = $pdo ? $pdo->query('SELECT COUNT(*) FROM tasks WHERE done=1')->fetchColumn() : 0;
?>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Slim API 仪表盘</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
:root{--primary:#22c55e;--primary-dark:#16a34a;--bg:#0a0a1a;--card:rgba(255,255,255,0.04);--border:rgba(255,255,255,0.07);--text:#f0f0ff;--text2:rgba(240,240,255,0.55);--text3:rgba(240,240,255,0.3);--radius:14px}
body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;background:var(--bg);color:var(--text);line-height:1.6;min-height:100vh}
body::before{content:'';position:fixed;top:-40%;left:-40%;width:180%;height:180%;background:radial-gradient(ellipse at 50% 30%,rgba(34,197,94,0.05) 0%,transparent 55%);pointer-events:none}
.hero{background:transparent;color:#fff;padding:28px 20px 20px;text-align:center}
.hero h1{font-size:1.8em;margin-bottom:6px;background:linear-gradient(135deg,#22c55e,#4ade80);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
.hero p{color:var(--text2);font-size:.9em}
.stats{display:flex;justify-content:center;gap:12px;margin-top:16px;flex-wrap:wrap}
.stat{text-align:center;background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:12px 16px}
.stat-num{font-size:1.3em;font-weight:800;color:var(--primary)}
.stat-label{font-size:.7em;color:var(--text2);margin-top:2px}
.container{max-width:460px;margin:0 auto;padding:0 14px 32px;position:relative;z-index:1}
.section{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:18px;margin-bottom:14px;backdrop-filter:blur(16px)}
.section h2{font-size:1.05em;margin-bottom:12px;display:flex;align-items:center;gap:8px;font-weight:700}
.endpoint{display:flex;align-items:center;gap:10px;padding:10px 0;border-bottom:1px solid rgba(255,255,255,0.03);cursor:pointer;transition:background .2s;border-radius:8px;padding:8px}
.endpoint:last-child{border:none}
.endpoint:active{background:rgba(255,255,255,0.02)}
.method{padding:3px 10px;border-radius:6px;font-size:.72em;font-weight:700;color:#fff;min-width:56px;text-align:center}
.method-get{background:#0d6efd}.method-post{background:#22c55e}.method-patch{background:#f59e0b}.method-delete{background:#ef4444}
.ep-path{font-family:monospace;font-size:.85em;flex:1;color:var(--text)}
.ep-desc{font-size:.75em;color:var(--text2)}
.tester{margin-top:12px;padding:14px;background:rgba(0,0,0,0.2);border-radius:10px;display:none}
.tester.active{display:block}
.tester-row{display:flex;gap:8px;margin-bottom:8px;flex-wrap:wrap}
.tester-row input,.tester-row select{flex:1;padding:8px 12px;background:rgba(255,255,255,0.03);border:1px solid var(--border);border-radius:6px;font-size:.85em;min-width:80px;color:var(--text);outline:none}
.tester-row select{color:var(--text2)}
.btn{padding:8px 16px;background:linear-gradient(135deg,#22c55e,#16a34a);color:#fff;border:none;border-radius:8px;font-size:.85em;cursor:pointer;font-weight:600}
.btn:active{transform:scale(.95)}
.btn-sm{padding:5px 12px;font-size:.8em}
.response{margin-top:10px;background:rgba(0,0,0,0.3);color:#4ade80;border-radius:8px;padding:14px;font-family:monospace;font-size:.78em;white-space:pre-wrap;max-height:250px;overflow:auto;display:none;border:1px solid rgba(34,197,94,0.1)}
.response.show{display:block}
.response .status{color:#60a5fa;font-weight:700}
.task{display:flex;align-items:center;gap:10px;padding:10px 0;border-bottom:1px solid rgba(255,255,255,0.03)}
.task:last-child{border:none}
.task-done .task-title{text-decoration:line-through;color:var(--text3)}
.task-title{flex:1;font-size:.9em}
.priority{padding:2px 8px;border-radius:10px;font-size:.7em;font-weight:600}
.priority-high{background:rgba(239,68,68,0.1);color:#ef4444}
.priority-medium{background:rgba(245,158,11,0.1);color:#f59e0b}
.priority-low{background:rgba(34,197,94,0.1);color:#22c55e}
.task-check{width:18px;height:18px;cursor:pointer;accent-color:var(--primary)}
.task-del{color:#ef4444;cursor:pointer;font-size:.85em;padding:4px 8px;border-radius:4px}
.add-form{display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap}
.add-form input{flex:1;padding:8px 12px;background:rgba(255,255,255,0.03);border:1px solid var(--border);border-radius:6px;font-size:.85em;min-width:120px;color:var(--text);outline:none}
.add-form input:focus{border-color:var(--primary)}
.add-form input::placeholder{color:var(--text3)}
.add-form select{padding:8px;background:rgba(255,255,255,0.03);border:1px solid var(--border);border-radius:6px;font-size:.85em;color:var(--text);outline:none}
footer{text-align:center;padding:16px;color:var(--text3);font-size:.75em}
@keyframes fadeIn{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:translateY(0)}}
.section{animation:fadeIn .5s ease backwards}
</style>
</head>
<body>
<div class="hero">
    <h1>🚀 Slim REST API</h1>
    <p>自包含 RESTful API 服务器 — 运行在你的 Android 设备上</p>
    <div class="stats">
        <div class="stat"><div class="stat-num"><?=$taskCount?></div><div class="stat-label">任务总数</div></div>
        <div class="stat"><div class="stat-num"><?=$doneCount?></div><div class="stat-label">已完成</div></div>
        <div class="stat"><div class="stat-num">6</div><div class="stat-label">API 接口</div></div>
        <div class="stat"><div class="stat-num"><?=phpversion()?></div><div class="stat-label">PHP</div></div>
    </div>
</div>
<div class="container">
    <div class="section">
        <h2>📚 API 文档</h2>
        <div class="endpoint" onclick="testApi('GET','/api/tasks')">
            <span class="method method-get">GET</span>
            <span class="ep-path">/api/tasks</span>
            <span class="ep-desc">获取任务列表</span>
        </div>
        <div class="endpoint" onclick="testApi('POST','/api/tasks','{&quot;title&quot;:&quot;新任务&quot;,&quot;priority&quot;:&quot;high&quot;}')">
            <span class="method method-post">POST</span>
            <span class="ep-path">/api/tasks</span>
            <span class="ep-desc">创建新任务</span>
        </div>
        <div class="endpoint" onclick="testApi('PATCH','/api/tasks/1/toggle')">
            <span class="method method-patch">PATCH</span>
            <span class="ep-path">/api/tasks/{id}/toggle</span>
            <span class="ep-desc">切换完成状态</span>
        </div>
        <div class="endpoint" onclick="testApi('DELETE','/api/tasks/1')">
            <span class="method method-delete">DELETE</span>
            <span class="ep-path">/api/tasks/{id}</span>
            <span class="ep-desc">删除任务</span>
        </div>
        <div class="endpoint" onclick="testApi('GET','/api/info')">
            <span class="method method-get">GET</span>
            <span class="ep-path">/api/info</span>
            <span class="ep-desc">服务器信息</span>
        </div>
        <div class="tester" id="tester">
            <div class="tester-row">
                <select id="t-method"><option>GET</option><option>POST</option><option>PATCH</option><option>DELETE</option></select>
                <input id="t-url" placeholder="/api/tasks">
                <button class="btn btn-sm" onclick="sendRequest()">发送</button>
            </div>
            <input id="t-body" placeholder='Body (JSON): {"title":"新任务"}' style="width:100%;padding:8px 12px;border:1px solid #ddd;border-radius:6px;font-size:.9em;margin-bottom:4px">
            <div class="response" id="response"></div>
        </div>
    </div>

    <div class="section">
        <h2>✅ 任务管理</h2>
        <div class="add-form">
            <input id="new-title" placeholder="输入新任务...">
            <select id="new-priority"><option value="high">高</option><option value="medium" selected>中</option><option value="low">低</option></select>
            <button class="btn btn-sm" onclick="addTask()">添加</button>
        </div>
        <div id="task-list">加载中...</div>
    </div>
</div>
<footer>Mini Slim API · PHP <?=phpversion()?> · 运行在 Android 设备上</footer>

<script>
const $ = id => document.getElementById(id);
function testApi(method, url, body) {
    $('tester').classList.add('active');
    $('t-method').value = method;
    $('t-url').value = url;
    $('t-body').value = body || '';
}
async function sendRequest() {
    const method = $('t-method').value;
    const url = $('t-url').value;
    const body = $('t-body').value;
    const resp = $('response');
    resp.classList.add('show');
    resp.innerHTML = '请求中...';
    try {
        const opts = {method, headers: {'Content-Type':'application/json'}};
        if (body && method !== 'GET') opts.body = body;
        const r = await fetch(url, opts);
        const text = await r.text();
        let pretty = text;
        try { pretty = JSON.stringify(JSON.parse(text), null, 2); } catch(e) {}
        resp.innerHTML = '<span class="status">' + r.status + ' ' + r.statusText + '</span>\n' + pretty;
    } catch(e) { resp.innerHTML = '错误: ' + e.message; }
}
function loadTasks() {
    fetch('/api/tasks').then(r => r.json()).then(data => {
        const list = $('task-list');
        if (!data.tasks.length) { list.innerHTML = '<p style="color:#999;text-align:center;padding:20px">暂无任务，添加一个吧！</p>'; return; }
        list.innerHTML = data.tasks.map(t => `
            <div class="task ${t.done?'task-done':''}">
                <input type="checkbox" class="task-check" ${t.done?'checked':''} onchange="toggleTask(${t.id})">
                <span class="task-title">${t.title}</span>
                <span class="priority priority-${t.priority}">${t.priority}</span>
                <span class="task-del" onclick="deleteTask(${t.id})">✖</span>
            </div>
        `).join('');
    });
}
function addTask() {
    const inp = $('new-title');
    const title = inp.value.trim();
    if (!title) { inp.style.borderColor='#dc3545'; inp.placeholder='请先输入任务标题！'; setTimeout(()=>{inp.style.borderColor='#ddd';inp.placeholder='输入新任务...';},2000); return; }
    const body = 'title=' + encodeURIComponent(title) + '&priority=' + encodeURIComponent($('new-priority').value);
    fetch('/api/tasks', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: body})
        .then(r => r.json()).then(data => { inp.value = ''; loadTasks(); })
        .catch(e => { console.error('addTask:', e); inp.placeholder='添加失败:'+e.message; });
}
function toggleTask(id) {
    fetch('/api/tasks/' + id + '/toggle', {method:'PATCH'}).then(() => loadTasks());
}
function deleteTask(id) {
    fetch('/api/tasks/' + id, {method:'DELETE'}).then(() => loadTasks());
}
loadTasks();
</script>
</body>
</html>
