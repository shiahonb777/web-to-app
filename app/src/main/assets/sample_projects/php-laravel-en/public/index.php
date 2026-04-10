<?php
/**
 * Mini Laravel Blog — Self-contained PHP Blog App
 * 
 * Laravel-style MVC architecture, no Composer required
 * Features: SQLite DB | Micro Router | CRUD | Responsive UI
 */
error_reporting(E_ALL & ~E_NOTICE & ~E_WARNING);

$basePath = dirname(__DIR__);
$uri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

$storagePath = $basePath . '/storage';
if (!is_dir($storagePath)) @mkdir($storagePath, 0755, true);
$dbFile = $storagePath . '/blog.sqlite';

$pdo = null;
try {
    $pdo = new PDO('sqlite:' . $dbFile);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->exec('CREATE TABLE IF NOT EXISTS posts (
        id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL,
        excerpt TEXT, content TEXT NOT NULL, category TEXT DEFAULT "PHP",
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )');
    if ($pdo->query('SELECT COUNT(*) FROM posts')->fetchColumn() == 0) seedData($pdo);
} catch (Exception $e) { $pdo = null; }

$routes = include $basePath . '/routes/web.php';
$handler = null; $params = [];
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

$html = '';
switch ($handler) {
    case 'home':
        $cat = $_GET['cat'] ?? '';
        $html = renderHome(getAllPosts($pdo, $cat), getCategories($pdo), $cat); break;
    case 'show':
        $post = getPost($pdo, (int)$params['id']);
        $html = $post ? renderShow($post) : renderNotFound(); break;
    case 'create': $html = renderCreate(); break;
    case 'store':
        $title = trim($_POST['title'] ?? ''); $excerpt = trim($_POST['excerpt'] ?? '');
        $content = trim($_POST['content'] ?? ''); $category = trim($_POST['category'] ?? 'PHP');
        if ($title && $content && $pdo) {
            $stmt = $pdo->prepare('INSERT INTO posts (title,excerpt,content,category) VALUES (?,?,?,?)');
            $stmt->execute([$title, $excerpt ?: mb_substr(strip_tags($content), 0, 120) . '...', $content, $category]);
        }
        header('Location: /'); exit;
    case 'delete':
        if ($pdo) $pdo->prepare('DELETE FROM posts WHERE id=?')->execute([(int)$params['id']]);
        header('Location: /'); exit;
    case 'apiPosts':
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode(['posts' => getAllPosts($pdo), 'total' => count(getAllPosts($pdo))], JSON_PRETTY_PRINT); exit;
    default: http_response_code(404); $html = renderNotFound();
}
echo renderLayout($html); exit;

function seedData(PDO $pdo): void {
    $posts = [
        ['PHP 8.4: Property Hooks & Asymmetric Visibility',
         'PHP 8.4 introduces property hooks and asymmetric visibility for more elegant OOP.',
         "PHP 8.4 is a significant language update.\n\n## Property Hooks\n\nProperty hooks let you define getter and setter logic directly in property declarations:\n\n```php\nclass User {\n    public string \$fullName {\n        get => \$this->firstName . ' ' . \$this->lastName;\n        set(string \$value) {\n            [\$this->firstName, \$this->lastName] = explode(' ', \$value, 2);\n        }\n    }\n}\n```\n\n## Asymmetric Visibility\n\nYou can now set different visibility for reading and writing:\n\n```php\nclass Config {\n    public private(set) string \$apiKey;\n}\n```\n\nThese features take PHP's encapsulation to the next level.", 'PHP'],
        ['Running PHP on Android Devices',
         'Learn how to run a full PHP HTTP server locally on Android devices.',
         "The blog you're reading is running on a PHP server on your Android device!\n\n## Tech Stack\n\n- **PHP " . phpversion() . "** (CLI SAPI)\n- **SQLite3** embedded database\n- **Custom HTTP Server** (stream_socket_server)\n- **pcntl_fork** for concurrency\n\n## How It Works\n\n1. PHP binary starts in CLI mode\n2. Custom router server listens on HTTP port\n3. Each request is handled in a forked child process\n4. PHP files are executed via include\n5. WebView loads the local HTTP server\n\nEverything runs locally on the device — no internet required.", 'Tech'],
        ['Building a PHP MVC Framework from Scratch',
         'Understand the core principles of MVC architecture by building a micro framework.',
         "This blog itself is an instance of a micro MVC framework!\n\n## Core Components\n\n### 1. Router\nMaps URLs to handler functions with parameter extraction:\n```\nGET /post/{id} -> show(\$id)\n```\n\n### 2. Controller\nReceives requests, calls models, renders views.\n\n### 3. Model\nEncapsulates database operations, provides CRUD methods.\n\n### 4. View\nRenders data as HTML with layout support.\n\n## Directory Structure\n```\npublic/index.php    <- Front Controller\nroutes/web.php      <- Route Definitions\nstorage/blog.sqlite <- Database\n```\n\nSmall but complete — the essence of MVC.", 'Architecture'],
        ['SQLite: The Best Database for Mobile',
         'SQLite needs no server, zero config — perfect for mobile and embedded use cases.',
         "SQLite is the most widely deployed database engine in the world.\n\n## Why SQLite\n\n- **Zero config**: No database server needed\n- **Single file**: The entire database is one file\n- **High performance**: Extremely fast for read-heavy workloads\n- **Cross-platform**: Works on all major operating systems\n\n## Using SQLite in PHP\n\n```php\n\$pdo = new PDO('sqlite:blog.db');\n\$pdo->exec('CREATE TABLE posts (...)');\n\$stmt = \$pdo->prepare('SELECT * FROM posts WHERE id = ?');\n\$stmt->execute([1]);\n\$post = \$stmt->fetch(PDO::FETCH_ASSOC);\n```\n\nAll data in this blog is stored in SQLite!", 'Database'],
        ['Modern PHP Best Practices',
         'From type safety to architecture — a practical guide to better PHP code.',
         "Modern PHP is not the PHP you remember.\n\n## 1. Strict Types\n```php\ndeclare(strict_types=1);\nfunction add(int \$a, int \$b): int {\n    return \$a + \$b;\n}\n```\n\n## 2. Enums\n```php\nenum Status: string {\n    case Active = 'active';\n    case Inactive = 'inactive';\n}\n```\n\n## 3. Readonly Properties\n```php\nclass User {\n    public function __construct(\n        public readonly string \$name,\n        public readonly string \$email,\n    ) {}\n}\n```\n\n## 4. Arrow Functions\n```php\n\$doubled = array_map(fn(\$n) => \$n * 2, [1, 2, 3]);\n```\n\nModern PHP is clean, fast, and type-safe.", 'PHP'],
    ];
    $stmt = $pdo->prepare('INSERT INTO posts (title, excerpt, content, category) VALUES (?, ?, ?, ?)');
    foreach ($posts as $p) $stmt->execute($p);
}

function getAllPosts(?PDO $pdo, string $category = ''): array {
    if (!$pdo) return [];
    $sql = 'SELECT * FROM posts';
    if ($category) { $stmt = $pdo->prepare($sql . ' WHERE category=? ORDER BY id DESC'); $stmt->execute([$category]); }
    else { $stmt = $pdo->query($sql . ' ORDER BY id DESC'); }
    return $stmt->fetchAll(PDO::FETCH_ASSOC);
}
function getPost(?PDO $pdo, int $id): ?array {
    if (!$pdo) return null;
    $stmt = $pdo->prepare('SELECT * FROM posts WHERE id=?'); $stmt->execute([$id]);
    return $stmt->fetch(PDO::FETCH_ASSOC) ?: null;
}
function getCategories(?PDO $pdo): array {
    if (!$pdo) return [];
    return $pdo->query('SELECT DISTINCT category FROM posts ORDER BY category')->fetchAll(PDO::FETCH_COLUMN);
}
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
function h(string $s): string { return htmlspecialchars($s, ENT_QUOTES, 'UTF-8'); }

function renderHome(array $posts, array $cats, string $activeCat): string {
    $postCards = '';
    foreach ($posts as $p) {
        $date = date('M d, Y', strtotime($p['created_at']));
        $postCards .= '<article class="card"><span class="card-cat">' . h($p['category']) . '</span><h2><a href="/post/' . $p['id'] . '">' . h($p['title']) . '</a></h2><p class="card-excerpt">' . h($p['excerpt']) . '</p><div class="card-meta"><span>' . $date . '</span><a href="/post/' . $p['id'] . '" class="btn-sm">Read more &rarr;</a></div></article>';
    }
    $catLinks = '<a href="/" class="tag ' . (!$activeCat ? 'active' : '') . '">All</a>';
    foreach ($cats as $c) $catLinks .= '<a href="/?cat=' . urlencode($c) . '" class="tag ' . ($activeCat === $c ? 'active' : '') . '">' . h($c) . '</a>';
    $count = count($posts);
    return '<section class="hero"><h1>\xf0\x9f\x93\x9d Laravel Blog</h1><p>A micro MVC blog built with pure PHP &mdash; running on your Android device</p><div class="hero-stats"><span>\xf0\x9f\x93\x9a ' . $count . ' articles</span><span>\xe2\x9a\xa1 PHP ' . phpversion() . '</span><span>\xf0\x9f\x97\x84 SQLite ' . (class_exists("SQLite3") ? SQLite3::version()['versionString'] : 'N/A') . '</span></div></section><div class="toolbar"><div class="tags">' . $catLinks . '</div><a href="/create" class="btn">\xe2\x9e\x95 New Post</a></div><div class="grid">' . ($postCards ?: '<p class="empty">No posts yet. <a href="/create">Write the first one</a>!</p>') . '</div>';
}
function renderShow(array $post): string {
    $date = date('M d, Y \a\t H:i', strtotime($post['created_at']));
    return '<div class="post-header"><a href="/" class="back">&larr; Back</a><span class="card-cat">' . h($post['category']) . '</span></div><article class="post"><h1>' . h($post['title']) . '</h1><div class="post-meta">\xf0\x9f\x93\x85 ' . $date . '</div><div class="post-content">' . renderMarkdown($post['content']) . '</div></article><div class="post-actions"><a href="/" class="btn">&larr; Back to list</a><a href="/delete/' . $post['id'] . '" class="btn btn-danger" onclick="return confirm(\'Delete this post?\')">&times; Delete</a></div>';
}
function renderCreate(): string {
    return '<div class="post-header"><a href="/" class="back">&larr; Back</a></div><div class="form-card"><h1>\xe2\x9e\x95 New Post</h1><form method="POST" action="/store"><label>Title</label><input type="text" name="title" required placeholder="Enter post title..."><label>Category</label><select name="category"><option>PHP</option><option>Tech</option><option>Architecture</option><option>Database</option><option>Other</option></select><label>Excerpt</label><input type="text" name="excerpt" placeholder="Optional, auto-generated if empty"><label>Content (Markdown supported)</label><textarea name="content" rows="12" required placeholder="Start writing..."></textarea><button type="submit" class="btn">\xf0\x9f\x9a\x80 Publish</button></form></div>';
}
function renderNotFound(): string {
    return '<div class="empty-state"><h1>404</h1><p>Page not found</p><a href="/" class="btn">Go Home</a></div>';
}
function renderLayout(string $content): string {
    return '<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"><title>Laravel Blog</title><style>*{margin:0;padding:0;box-sizing:border-box}:root{--primary:#ef4444;--primary-light:#f87171;--bg:#0a0a1a;--card:rgba(255,255,255,0.04);--border:rgba(255,255,255,0.07);--text:#f0f0ff;--text2:rgba(240,240,255,0.55);--text3:rgba(240,240,255,0.3);--radius:14px}body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;background:var(--bg);color:var(--text);line-height:1.7;min-height:100vh}body::before{content:\'\';position:fixed;top:-40%;left:-40%;width:180%;height:180%;background:radial-gradient(ellipse at 50% 30%,rgba(239,68,68,0.04) 0%,transparent 55%);pointer-events:none}a{color:var(--primary);text-decoration:none}.container{max-width:460px;margin:0 auto;padding:0 14px 32px;position:relative;z-index:1}.hero{background:transparent;color:#fff;padding:28px 20px 20px;text-align:center;margin-bottom:16px}.hero h1{font-size:1.8em;margin-bottom:8px;background:linear-gradient(135deg,#ef4444,#f87171);-webkit-background-clip:text;-webkit-text-fill-color:transparent}.hero p{color:var(--text2);font-size:.95em}.hero-stats{margin-top:14px;display:flex;justify-content:center;gap:16px;font-size:.8em;color:var(--text2)}.toolbar{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;flex-wrap:wrap;gap:10px}.tags{display:flex;gap:6px;flex-wrap:wrap}.tag{padding:4px 12px;border-radius:16px;font-size:.8em;background:var(--card);border:1px solid var(--border);color:var(--text2);transition:.2s}.tag.active,.tag:hover{background:rgba(239,68,68,0.1);color:var(--primary);border-color:rgba(239,68,68,0.2)}.grid{display:flex;flex-direction:column;gap:12px;padding-bottom:16px}.card{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:18px;backdrop-filter:blur(16px);transition:transform .2s}.card:active{transform:scale(.98)}.card h2{font-size:1.05em;margin:6px 0}.card h2 a{color:var(--text)}.card-cat{display:inline-block;padding:2px 10px;border-radius:10px;font-size:.72em;background:rgba(239,68,68,0.1);color:var(--primary);font-weight:600}.card-excerpt{color:var(--text2);font-size:.88em;margin:6px 0;line-height:1.5}.card-meta{display:flex;justify-content:space-between;align-items:center;font-size:.8em;color:var(--text3);margin-top:10px}.btn-sm{color:var(--primary);font-weight:600}.post-header{display:flex;justify-content:space-between;align-items:center;margin:16px 0}.back{color:var(--text2);font-weight:500;font-size:.9em}.post{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:24px 20px;backdrop-filter:blur(16px)}.post h1{font-size:1.4em;margin-bottom:8px;line-height:1.3}.post-meta{color:var(--text3);font-size:.85em;margin-bottom:20px;padding-bottom:14px;border-bottom:1px solid var(--border)}.post-content{font-size:.92em;line-height:1.85}.post-content h3{font-size:1.1em;margin:20px 0 8px;color:var(--primary)}.post-content h4{font-size:1em;margin:14px 0 6px}.post-content pre{background:rgba(0,0,0,0.3);color:#4ade80;padding:14px;border-radius:8px;overflow-x:auto;font-size:.82em;margin:10px 0;border:1px solid rgba(34,197,94,0.1)}.post-content code{font-family:monospace;font-size:.9em}.post-content p code{background:rgba(255,255,255,0.05);padding:2px 6px;border-radius:4px}.post-content ul{padding-left:20px;margin:8px 0}.post-actions{display:flex;gap:10px;margin:20px 0 32px}.form-card{background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:24px 20px;backdrop-filter:blur(16px)}.form-card h1{font-size:1.2em;margin-bottom:16px}label{display:block;font-weight:600;margin:12px 0 4px;font-size:.85em;color:var(--text2)}input[type=text],select,textarea{width:100%;padding:10px 14px;background:rgba(255,255,255,0.03);border:1px solid var(--border);border-radius:8px;font-size:.92em;font-family:inherit;color:var(--text);outline:none}input:focus,textarea:focus,select:focus{border-color:var(--primary)}input::placeholder,textarea::placeholder{color:var(--text3)}textarea{resize:vertical}select{color:var(--text2)}.btn{display:inline-block;padding:10px 20px;background:linear-gradient(135deg,#ef4444,#dc2626);color:#fff;border:none;border-radius:8px;font-size:.9em;cursor:pointer;font-weight:600}.btn:active{transform:scale(.95)}.btn-danger{background:linear-gradient(135deg,#6b7280,#4b5563)}.empty{text-align:center;padding:32px;color:var(--text3)}.empty-state{text-align:center;padding:40px 20px}.empty-state h1{font-size:3em;color:var(--primary);margin-bottom:10px}footer{text-align:center;padding:16px;color:var(--text3);font-size:.75em;border-top:1px solid var(--border);margin-top:16px}@keyframes fadeIn{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:translateY(0)}}.card,.post,.form-card{animation:fadeIn .5s ease backwards}</style></head><body><div class="container">' . $content . '</div><footer>Mini Laravel Blog &middot; PHP ' . phpversion() . ' &middot; Running on Android</footer></body></html>';
}
