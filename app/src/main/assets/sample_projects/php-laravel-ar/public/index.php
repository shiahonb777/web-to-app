<?php
/**
 * مدونة Laravel المصغرة — تطبيق مدونة PHP مستقل
 * 
 * بنية Laravel MVC، بدون Composer
 * SQLite | توجيه مصغر | CRUD | واجهة متجاوبة
 */
error_reporting(E_ALL & ~E_NOTICE & ~E_WARNING);

// ==================== 引导 ====================
$basePath = dirname(__DIR__);
$uri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

// ==================== 数据库 ====================
$storagePath = $basePath . '/storage';
if (!is_dir($storagePath)) @mkdir($storagePath, 0755, true);
$dbFile = $storagePath . '/blog.sqlite';

$pdo = null;
try {
    $pdo = new PDO('sqlite:' . $dbFile);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->exec('
        CREATE TABLE IF NOT EXISTS posts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            excerpt TEXT,
            content TEXT NOT NULL,
            category TEXT DEFAULT "PHP",
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    ');
    if ($pdo->query('SELECT COUNT(*) FROM posts')->fetchColumn() == 0) {
        seedData($pdo);
    }
} catch (Exception $e) {
    $pdo = null;
}

// ==================== 路由调度 ====================
$routes = include $basePath . '/routes/web.php';
$handler = null;
$params = [];

foreach ($routes as [$rMethod, $rPath, $rHandler]) {
    if ($method !== $rMethod) continue;
    $pattern = preg_replace('/\{(\w+)\}/', '(?P<$1>[^/]+)', $rPath);
    if (preg_match('#^' . $pattern . '$#', $uri, $m)) {
        $handler = $rHandler;
        $params = array_filter($m, 'is_string', ARRAY_FILTER_USE_KEY);
        break;
    }
}

if (!$handler) $handler = 'notFound';

// ==================== 控制器 ====================
$html = '';
switch ($handler) {
    case 'home':
        $cat = $_GET['cat'] ?? '';
        $posts = getAllPosts($pdo, $cat);
        $cats = getCategories($pdo);
        $html = renderHome($posts, $cats, $cat);
        break;
    case 'show':
        $post = getPost($pdo, (int)$params['id']);
        $html = $post ? renderShow($post) : renderNotFound();
        break;
    case 'create':
        $html = renderCreate();
        break;
    case 'store':
        $title = trim($_POST['title'] ?? '');
        $excerpt = trim($_POST['excerpt'] ?? '');
        $content = trim($_POST['content'] ?? '');
        $category = trim($_POST['category'] ?? 'PHP');
        if ($title && $content && $pdo) {
            $stmt = $pdo->prepare('INSERT INTO posts (title,excerpt,content,category) VALUES (?,?,?,?)');
            $stmt->execute([$title, $excerpt ?: mb_substr(strip_tags($content), 0, 120) . '...', $content, $category]);
        }
        header('Location: /');
        exit;
    case 'delete':
        if ($pdo) {
            $pdo->prepare('DELETE FROM posts WHERE id=?')->execute([(int)$params['id']]);
        }
        header('Location: /');
        exit;
    case 'apiPosts':
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode(['posts' => getAllPosts($pdo), 'total' => count(getAllPosts($pdo))], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        exit;
    default:
        http_response_code(404);
        $html = renderNotFound();
}

echo renderLayout($html);
exit;

// ==================== 数据层 ====================
function seedData(PDO $pdo): void {
    $posts = [
        ['PHP 8.4 新特性：属性钩子与不对称可见性',
         'PHP 8.4 引入了属性钩子和不对称可见性，让面向对象编程更加优雅。',
         "PHP 8.4 是一次重要的语言更新。\n\n## 属性钩子 (Property Hooks)\n\n属性钩子允许你直接在属性声明中定义 getter 和 setter 逻辑：\n\n```php\nclass User {\n    public string \$fullName {\n        get => \$this->firstName . ' ' . \$this->lastName;\n        set(string \$value) {\n            [\$this->firstName, \$this->lastName] = explode(' ', \$value, 2);\n        }\n    }\n}\n```\n\n## 不对称可见性\n\n现在可以设置属性的读取和写入拥有不同的可见性：\n\n```php\nclass Config {\n    public private(set) string \$apiKey;\n}\n```\n\n这些特性让 PHP 的封装性更上一层楼。", 'PHP'],
        ['在 Android 设备上运行 PHP 服务器',
         '探索如何在 Android 设备上本地运行完整的 PHP HTTP 服务器。',
         "你正在看的这个博客，正是运行在你的 Android 设备上的 PHP 服务器！\n\n## 技术栈\n\n- **PHP " . phpversion() . "** (CLI SAPI)\n- **SQLite3** 内嵌式数据库\n- **自定义 HTTP 服务器** (stream_socket_server)\n- **pcntl_fork** 多进程并发\n\n## 工作原理\n\n1. PHP 二进制以 CLI 模式启动\n2. 自定义路由服务器监听 HTTP 端口\n3. 每个请求 fork 子进程处理\n4. 通过 include 执行 PHP 文件\n5. WebView 加载本地 HTTP 服务器地址\n\n这意味着所有处理都在设备本地完成，无需网络连接。", '技术'],
        ['从零构建 PHP MVC 框架',
         '理解 MVC 架构的核心原理，动手实现一个微型框架。',
         "本博客本身就是一个微型 MVC 框架的实例！\n\n## 核心组件\n\n### 1. 路由器 (Router)\n将 URL 映射到处理器函数，支持参数提取：\n```\nGET /post/{id} -> show($id)\n```\n\n### 2. 控制器 (Controller)\n接收请求，调用模型获取数据，渲染视图。\n\n### 3. 模型 (Model)\n封装数据库操作，提供 CRUD 方法。\n\n### 4. 视图 (View)\n将数据渲染为 HTML，支持布局继承。\n\n## 目录结构\n```\npublic/index.php    ← 前端控制器\nroutes/web.php      ← 路由定义\nstorage/blog.sqlite ← 数据库\n```\n\n麻雀虽小五脏俱全，这就是 MVC 的精髓。", '架构'],
        ['SQLite：移动开发的最佳数据库',
         'SQLite 无需服务器、零配置，完美适合移动和嵌入式场景。',
         "SQLite 是全球部署最广泛的数据库引擎。\n\n## 为什么选择 SQLite\n\n- **零配置**: 无需安装数据库服务器\n- **单文件**: 整个数据库就是一个文件\n- **高性能**: 对于读多写少的场景极快\n- **跨平台**: 支持所有主流操作系统\n\n## 在 PHP 中使用\n\n```php\n\$pdo = new PDO('sqlite:blog.db');\n\$pdo->exec('CREATE TABLE posts (...)');\n\$stmt = \$pdo->prepare('SELECT * FROM posts WHERE id = ?');\n\$stmt->execute([1]);\n\$post = \$stmt->fetch(PDO::FETCH_ASSOC);\n```\n\n本博客的所有数据就存储在 SQLite 中！", '数据库'],
        ['现代 PHP 开发最佳实践',
         '从类型安全到架构设计，提升 PHP 代码质量的实用指南。',
         "现代 PHP 已经不是你印象中的那个 PHP 了。\n\n## 1. 严格类型\n```php\ndeclare(strict_types=1);\nfunction add(int \$a, int \$b): int {\n    return \$a + \$b;\n}\n```\n\n## 2. 枚举类型\n```php\nenum Status: string {\n    case Active = 'active';\n    case Inactive = 'inactive';\n}\n```\n\n## 3. 只读属性\n```php\nclass User {\n    public function __construct(\n        public readonly string \$name,\n        public readonly string \$email,\n    ) {}\n}\n```\n\n## 4. 箭头函数\n```php\n\$doubled = array_map(fn(\$n) => \$n * 2, [1, 2, 3]);\n```\n\n现代 PHP 简洁、高效、类型安全。", 'PHP'],
    ];
    $stmt = $pdo->prepare('INSERT INTO posts (title, excerpt, content, category) VALUES (?, ?, ?, ?)');
    foreach ($posts as $p) $stmt->execute($p);
}

function getAllPosts(?PDO $pdo, string $category = ''): array {
    if (!$pdo) return [];
    $sql = 'SELECT * FROM posts';
    if ($category) {
        $stmt = $pdo->prepare($sql . ' WHERE category=? ORDER BY id DESC');
        $stmt->execute([$category]);
    } else {
        $stmt = $pdo->query($sql . ' ORDER BY id DESC');
    }
    return $stmt->fetchAll(PDO::FETCH_ASSOC);
}

function getPost(?PDO $pdo, int $id): ?array {
    if (!$pdo) return null;
    $stmt = $pdo->prepare('SELECT * FROM posts WHERE id=?');
    $stmt->execute([$id]);
    return $stmt->fetch(PDO::FETCH_ASSOC) ?: null;
}

function getCategories(?PDO $pdo): array {
    if (!$pdo) return [];
    return $pdo->query('SELECT DISTINCT category FROM posts ORDER BY category')->fetchAll(PDO::FETCH_COLUMN);
}

// ==================== Markdown 简易渲染 ====================
function renderMarkdown(string $text): string {
    $text = htmlspecialchars($text);
    $text = preg_replace('/```(\w*)\n(.*?)```/s', '<pre><code>$2</code></pre>', $text);
    $text = preg_replace('/`([^`]+)`/', '<code>$1</code>', $text);
    $text = preg_replace('/^## (.+)$/m', '<h3>$1</h3>', $text);
    $text = preg_replace('/^### (.+)$/m', '<h4>$1</h4>', $text);
    $text = preg_replace('/\*\*(.+?)\*\*/', '<strong>$1</strong>', $text);
    $text = preg_replace('/^- (.+)$/m', '<li>$1</li>', $text);
    $text = preg_replace('/(<li>.*<\/li>)/s', '<ul>$1</ul>', $text);
    $text = preg_replace('/\n{2,}/', '</p><p>', $text);
    return '<p>' . $text . '</p>';
}

// ==================== 视图 ====================
function renderHome(array $posts, array $cats, string $activeCat): string {
    $postCards = '';
    foreach ($posts as $p) {
        $date = date('YY-m-d', strtotime($p['created_at']));
        $postCards .= '
        <article class="card">
            <span class="card-cat">' . h($p['category']) . '</span>
            <h2><a href="/post/' . $p['id'] . '">' . h($p['title']) . '</a></h2>
            <p class="card-excerpt">' . h($p['excerpt']) . '</p>
            <div class="card-meta">
                <span>' . $date . '</span>
                <a href="/post/' . $p['id'] . '" class="btn-sm">اقرأ المزيد ←</a>
            </div>
        </article>';
    }

    $catLinks = '<a href="/" class="tag ' . (!$activeCat ? 'active' : '') . '">الكل</a>';
    foreach ($cats as $c) {
        $catLinks .= '<a href="/?cat=' . urlencode($c) . '" class="tag ' . ($activeCat === $c ? 'active' : '') . '">' . h($c) . '</a>';
    }

    $count = count($posts);
    return '
    <section class="hero">
        <h1>📝 مدونة Laravel</h1>
        <p>مدونة MVC مصغرة بلغة PHP — تعمل على جهاز Android</p>
        <div class="hero-stats">
            <span>📚 ' . $count . ' مقالات</span>
            <span>⚡ PHP ' . phpversion() . '</span>
            <span>🗄 SQLite ' . (class_exists("SQLite3") ? SQLite3::version()['versionString'] : 'N/A') . '</span>
        </div>
    </section>
    <div class="toolbar">
        <div class="tags">' . $catLinks . '</div>
        <a href="/create" class="btn">➕ مقال جديد</a>
    </div>
    <div class="grid">' . ($postCards ?: '<p class="empty">لا توجد مقالات.<a href="/create">اكتب الأول</a>吧！</p>') . '</div>';
}

function renderShow(array $post): string {
    $date = date('YY-m-d H:i', strtotime($post['created_at']));
    $content = renderMarkdown($post['content']);
    return '
    <div class="post-header">
        <a href="/" class="back">← الرجوع</a>
        <span class="card-cat">' . h($post['category']) . '</span>
    </div>
    <article class="post">
        <h1>' . h($post['title']) . '</h1>
        <div class="post-meta">📅 ' . $date . '</div>
        <div class="post-content">' . $content . '</div>
    </article>
    <div class="post-actions">
        <a href="/" class="btn">← العودة للقائمة</a>
        <a href="/delete/' . $post['id'] . '" class="btn btn-danger" onclick="return confirm(\'\u786e定حذف这مقالات吗？\')">🗑 حذف</a>
    </div>';
}

function renderCreate(): string {
    return '
    <div class="post-header">
        <a href="/" class="back">← الرجوع</a>
    </div>
    <div class="form-card">
        <h1>➕ مقال جديد</h1>
        <form method="POST" action="/store">
            <label>العنوان</label>
            <input type="text" name="title" required placeholder="输入文章العنوان...">
            <label>التصنيف</label>
            <select name="category">
                <option>PHP</option><option>技术</option><option>架构</option><option>数据库</option><option>其他</option>
            </select>
            <label>الملخص</label>
            <input type="text" name="excerpt" placeholder="اختياري">
            <label>المحتوى (Markdown)</label>
            <textarea name="content" rows="12" required placeholder="ابدأ الكتابة..."></textarea>
            <button type="submit" class="btn">🚀 نشر</button>
        </form>
    </div>';
}

function renderNotFound(): string {
    return '<div class="empty-state"><h1>404</h1><p>الصفحة غير موجودة</p><a href="/" class="btn">الرجوع</a></div>';
}

function h(string $s): string { return htmlspecialchars($s, ENT_QUOTES, 'UTF-8'); }

// ==================== 布局 ====================
function renderLayout(string $content): string {
    return '<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>مدونة Laravel</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
:root{--primary:#ef4444;--primary-light:#f87171;--bg:#0a0a1a;--card:rgba(255,255,255,0.04);--border:rgba(255,255,255,0.07);--text:#f0f0ff;--text2:rgba(240,240,255,0.55);--text3:rgba(240,240,255,0.3);--radius:14px}
body{font-family:-apple-system,BlinkMacSystemFont,Tahoma,sans-serif;background:var(--bg);color:var(--text);line-height:1.7;min-height:100vh;direction:rtl}
body::before{content:'';position:fixed;top:-40%;left:-40%;width:180%;height:180%;background:radial-gradient(ellipse at 50% 30%,rgba(239,68,68,0.04) 0%,transparent 55%);pointer-events:none}
a{color:var(--primary);text-decoration:none}
.container{max-width:460px;margin:0 auto;padding:0 14px 32px;position:relative;z-index:1}
.hero{background:transparent;color:#fff;padding:28px 20px 20px;text-align:center;margin-bottom:16px}
.hero h1{font-size:1.8em;margin-bottom:8px;background:linear-gradient(135deg,#ef4444,#f87171);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
.hero p{color:var(--text2);font-size:.95em}
.hero-stats{margin-top:14px;display:flex;justify-content:center;gap:16px;font-size:.8em;color:var(--text2)}
.toolbar{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;flex-wrap:wrap;gap:10px}
.tags{display:flex;gap:6px;flex-wrap:wrap}
.tag{padding:4px 12px;border-radius:16px;font-size:.8em;background:var(--card);border:1px solid var(--border);color:var(--text2);transition:.2s}
.tag.active,.tag:hover{background:rgba(239,68,68,0.1);color:var(--primary);border-color:rgba(239,68,68,0.2)}
.grid{display:flex;flex-direction:column;gap:12px;padding-bottom:16px}
.card{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:18px;backdrop-filter:blur(16px);transition:transform .2s}
.card:active{transform:scale(.98)}
.card h2{font-size:1.05em;margin:6px 0}
.card h2 a{color:var(--text)}
.card-cat{display:inline-block;padding:2px 10px;border-radius:10px;font-size:.72em;background:rgba(239,68,68,0.1);color:var(--primary);font-weight:600}
.card-excerpt{color:var(--text2);font-size:.88em;margin:6px 0;line-height:1.5}
.card-meta{display:flex;justify-content:space-between;align-items:center;font-size:.8em;color:var(--text3);margin-top:10px}
.btn-sm{color:var(--primary);font-weight:600}
.post-header{display:flex;justify-content:space-between;align-items:center;margin:16px 0}
.back{color:var(--text2);font-weight:500;font-size:.9em}
.post{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:24px 20px;backdrop-filter:blur(16px)}
.post h1{font-size:1.4em;margin-bottom:8px;line-height:1.3}
.post-meta{color:var(--text3);font-size:.85em;margin-bottom:20px;padding-bottom:14px;border-bottom:1px solid var(--border)}
.post-content{font-size:.92em;line-height:1.85}
.post-content h3{font-size:1.1em;margin:20px 0 8px;color:var(--primary)}
.post-content h4{font-size:1em;margin:14px 0 6px}
.post-content pre{background:rgba(0,0,0,0.3);color:#4ade80;padding:14px;border-radius:8px;overflow-x:auto;font-size:.82em;margin:10px 0;direction:ltr;text-align:left;border:1px solid rgba(34,197,94,0.1)}
.post-content code{font-family:monospace;font-size:.9em}
.post-content p code{background:rgba(255,255,255,0.05);padding:2px 6px;border-radius:4px}
.post-content ul{padding-right:20px;margin:8px 0}
.post-actions{display:flex;gap:10px;margin:20px 0 32px}
.form-card{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:24px 20px;backdrop-filter:blur(16px)}
.form-card h1{font-size:1.2em;margin-bottom:16px}
label{display:block;font-weight:600;margin:12px 0 4px;font-size:.85em;color:var(--text2)}
input[type=text],select,textarea{width:100%;padding:10px 14px;background:rgba(255,255,255,0.03);border:1px solid var(--border);border-radius:8px;font-size:.92em;font-family:inherit;color:var(--text);outline:none}
input:focus,textarea:focus,select:focus{border-color:var(--primary)}
input::placeholder,textarea::placeholder{color:var(--text3)}
textarea{resize:vertical}
select{color:var(--text2)}
.btn{display:inline-block;padding:10px 20px;background:linear-gradient(135deg,#ef4444,#dc2626);color:#fff;border:none;border-radius:8px;font-size:.9em;cursor:pointer;font-weight:600}
.btn:active{transform:scale(.95)}
.btn-danger{background:linear-gradient(135deg,#6b7280,#4b5563)}
.empty{text-align:center;padding:32px;color:var(--text3)}
.empty-state{text-align:center;padding:40px 20px}
.empty-state h1{font-size:3em;color:var(--primary);margin-bottom:10px}
footer{text-align:center;padding:16px;color:var(--text3);font-size:.75em;border-top:1px solid var(--border);margin-top:16px}
@keyframes fadeIn{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:translateY(0)}}
.card,.post,.form-card{animation:fadeIn .5s ease backwards}
</style>
</head>
<body>
<div class="container">' . $content . '</div>
<footer>Mini مدونة Laravel · PHP ' . phpversion() . ' · يعمل على Android</footer>
</body>
</html>';
}
