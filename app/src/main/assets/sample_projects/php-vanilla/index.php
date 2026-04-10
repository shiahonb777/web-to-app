<?php
/**
 * PHP 能力展示 — 系统信息仪表盘
 * 
 * 无框架纯 PHP 应用，展示 PHP 在 Android 上的完整能力
 * 特性: 系统信息 | 扩展检测 | 表单处理 | 会话管理 | 性能测试
 */
error_reporting(E_ALL & ~E_NOTICE & ~E_WARNING);
@session_start();

// 表单处理
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (isset($_POST['session_key'], $_POST['session_val'])) {
        $_SESSION[$_POST['session_key']] = $_POST['session_val'];
    }
    if (isset($_POST['clear_session'])) {
        session_destroy(); session_start();
    }
    header('Location: /'); exit;
}

// 收集系统信息
$info = [
    'PHP 版本' => phpversion(),
    'SAPI' => php_sapi_name(),
    '操作系统' => PHP_OS . ' (' . php_uname('m') . ')',
    '服务器软件' => $_SERVER['SERVER_SOFTWARE'] ?? 'N/A',
    'Zend 版本' => zend_version(),
    '最大执行时间' => ini_get('max_execution_time') . 's',
    '内存限制' => ini_get('memory_limit'),
    '当前内存' => round(memory_get_usage(true) / 1024 / 1024, 2) . ' MB',
    '峰值内存' => round(memory_get_peak_usage(true) / 1024 / 1024, 2) . ' MB',
    '服务器时间' => date('Y-m-d H:i:s'),
    '时区' => date_default_timezone_get(),
    '进程 ID' => getmypid(),
];

$extensions = get_loaded_extensions();
sort($extensions);
$extCount = count($extensions);

// 功能检测
$features = [
    'PDO' => extension_loaded('pdo'),
    'SQLite3' => extension_loaded('pdo_sqlite') || extension_loaded('sqlite3'),
    'JSON' => extension_loaded('json'),
    'cURL' => extension_loaded('curl'),
    'OpenSSL' => extension_loaded('openssl'),
    'mbstring' => extension_loaded('mbstring'),
    'GD' => extension_loaded('gd'),
    'DOM/XML' => extension_loaded('dom'),
    'PCNTL' => extension_loaded('pcntl'),
    'Sockets' => extension_loaded('sockets'),
    'Zip' => extension_loaded('zip'),
    'BCMath' => extension_loaded('bcmath'),
];

// 性能基准
$benchStart = hrtime(true);
$n = 0; for ($i = 0; $i < 1000000; $i++) $n += $i;
$intTime = round((hrtime(true) - $benchStart) / 1e6, 2);

$benchStart = hrtime(true);
$s = ''; for ($i = 0; $i < 50000; $i++) $s .= 'a';
$md5 = md5($s);
$strTime = round((hrtime(true) - $benchStart) / 1e6, 2);

$benchStart = hrtime(true);
$arr = range(1, 10000); shuffle($arr); sort($arr);
$sortTime = round((hrtime(true) - $benchStart) / 1e6, 2);

$sessionData = $_SESSION ?? [];
?>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>PHP 能力展示</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
:root{--primary:#8b5cf6;--primary-dark:#7c3aed;--bg:#0a0a1a;--card:rgba(255,255,255,0.04);--border:rgba(255,255,255,0.07);--text:#f0f0ff;--text2:rgba(240,240,255,0.55);--text3:rgba(240,240,255,0.3);--radius:14px}
body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;background:var(--bg);color:var(--text);line-height:1.6;min-height:100vh}
body::before{content:'';position:fixed;top:-40%;left:-40%;width:180%;height:180%;background:radial-gradient(ellipse at 50% 30%,rgba(139,92,246,0.05) 0%,transparent 55%);pointer-events:none}
.hero{background:transparent;color:#fff;padding:28px 20px 20px;text-align:center}
.hero h1{font-size:1.8em;margin-bottom:6px;background:linear-gradient(135deg,#8b5cf6,#a78bfa);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
.hero p{color:var(--text2);font-size:.95em}
.stats{display:flex;justify-content:center;gap:12px;margin-top:16px;flex-wrap:wrap}
.stat{text-align:center;background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:12px 16px}
.stat-num{font-size:1.3em;font-weight:800;color:var(--primary)}
.stat-label{font-size:.7em;color:var(--text2);margin-top:2px}
.container{max-width:460px;margin:0 auto;padding:0 14px 32px;position:relative;z-index:1}
.section{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:18px;margin-bottom:14px;backdrop-filter:blur(16px)}
.section h2{font-size:1.05em;margin-bottom:12px;display:flex;align-items:center;gap:8px;font-weight:700}
.grid2{display:grid;grid-template-columns:1fr 1fr;gap:8px}
.info-row{display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid rgba(255,255,255,0.03);font-size:.85em}
.info-row:last-child{border:none}
.info-key{color:var(--text2)}
.info-val{font-weight:600;text-align:right;word-break:break-all;color:var(--primary)}
.ext-badge{display:inline-block;padding:3px 10px;border-radius:16px;font-size:.75em;margin:3px;background:rgba(255,255,255,0.03);color:var(--text3);border:1px solid var(--border)}
.ext-badge.loaded{background:rgba(16,185,129,0.1);color:#10b981;border-color:rgba(16,185,129,0.2)}
.feat{display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid rgba(255,255,255,0.03);font-size:.85em}
.feat:last-child{border:none}
.feat-yes{color:#10b981;font-weight:700}
.feat-no{color:#ef4444;font-weight:700}
.bar-wrap{height:6px;background:rgba(255,255,255,0.05);border-radius:3px;flex:1;margin:0 12px}
.bar{height:100%;border-radius:3px;background:linear-gradient(90deg,#8b5cf6,#a78bfa)}
.bench{display:flex;align-items:center;padding:8px 0;border-bottom:1px solid rgba(255,255,255,0.03);font-size:.85em}
.bench:last-child{border:none}
.bench-name{width:110px;color:var(--text2);font-size:.8em}
.bench-val{width:70px;text-align:right;font-weight:700;font-size:.85em;color:#f59e0b}
form{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px}
form input{flex:1;padding:8px 12px;background:rgba(255,255,255,0.03);border:1px solid var(--border);border-radius:8px;font-size:.9em;min-width:80px;color:var(--text);outline:none}
form input:focus{border-color:var(--primary)}
form input::placeholder{color:var(--text3)}
.btn{padding:8px 16px;background:linear-gradient(135deg,#8b5cf6,#7c3aed);color:#fff;border:none;border-radius:8px;font-size:.85em;cursor:pointer;font-weight:600}
.btn:active{transform:scale(.95)}
.btn-danger{background:linear-gradient(135deg,#ef4444,#dc2626)}
.session-item{display:flex;justify-content:space-between;padding:6px 0;font-size:.85em;border-bottom:1px solid rgba(255,255,255,0.03)}
.session-item:last-child{border:none}
footer{text-align:center;padding:16px;color:var(--text3);font-size:.75em}
@keyframes fadeIn{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:translateY(0)}}
.section{animation:fadeIn .5s ease backwards}
</style>
</head>
<body>
<div class="hero">
    <h1>🐘 PHP 能力展示</h1>
    <p>探索 PHP <?=phpversion()?> 在 Android 设备上的完整能力</p>
    <div class="stats">
        <div class="stat"><div class="stat-num"><?=phpversion()?></div><div class="stat-label">PHP 版本</div></div>
        <div class="stat"><div class="stat-num"><?=$extCount?></div><div class="stat-label">已加载扩展</div></div>
        <div class="stat"><div class="stat-num"><?=PHP_INT_SIZE * 8?>bit</div><div class="stat-label">架构</div></div>
        <div class="stat"><div class="stat-num"><?=PHP_OS?></div><div class="stat-label">操作系统</div></div>
    </div>
</div>
<div class="container">
    <div class="section">
        <h2>📊 系统信息</h2>
        <?php foreach ($info as $k => $v): ?>
        <div class="info-row"><span class="info-key"><?=$k?></span><span class="info-val"><?=$v?></span></div>
        <?php endforeach; ?>
    </div>

    <div class="section">
        <h2>⚡ 性能基准测试</h2>
        <?php
        $maxTime = max($intTime, $strTime, $sortTime, 1);
        $benchmarks = ["整数运算 (100万次)" => $intTime, "字符串操作 (5万次)" => $strTime, "数组排序 (1万元素)" => $sortTime];
        foreach ($benchmarks as $name => $time):
            $pct = min(($time / $maxTime) * 100, 100);
        ?>
        <div class="bench">
            <span class="bench-name"><?=$name?></span>
            <div class="bar-wrap"><div class="bar" style="width:<?=$pct?>%"></div></div>
            <span class="bench-val"><?=$time?> ms</span>
        </div>
        <?php endforeach; ?>
    </div>

    <div class="section">
        <h2>✅ 功能检测</h2>
        <div class="grid2">
        <?php foreach ($features as $name => $ok): ?>
            <div class="feat"><span><?=$name?></span><span class="<?=$ok?'feat-yes':'feat-no'?>"><?=$ok?'✓ 可用':'✗ 不可用'?></span></div>
        <?php endforeach; ?>
        </div>
    </div>

    <div class="section">
        <h2>🔐 会话管理 (Session)</h2>
        <form method="POST">
            <input name="session_key" placeholder="键" required>
            <input name="session_val" placeholder="值" required>
            <button class="btn" type="submit">保存</button>
        </form>
        <?php if ($sessionData): ?>
            <?php foreach ($sessionData as $k => $v): ?>
            <div class="session-item"><span style="color:var(--primary);font-weight:600"><?=htmlspecialchars($k)?></span><span><?=htmlspecialchars($v)?></span></div>
            <?php endforeach; ?>
            <form method="POST" style="margin-top:8px"><input type="hidden" name="clear_session" value="1"><button class="btn btn-danger" type="submit">清空 Session</button></form>
        <?php else: ?>
            <p style="color:var(--text2);font-size:.9em;padding:8px 0">暂无会话数据，试试添加一个！</p>
        <?php endif; ?>
    </div>

    <div class="section">
        <h2>🧩 已加载扩展 (<?=$extCount?>)</h2>
        <div>
        <?php foreach ($extensions as $ext): ?>
            <span class="ext-badge loaded"><?=$ext?></span>
        <?php endforeach; ?>
        </div>
    </div>
</div>
<footer>PHP 能力展示 · PHP <?=phpversion()?> · 运行在 Android 设备上</footer>
</body>
</html>
