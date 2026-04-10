<?php
/**
 * PHP Router Server — CLI SAPI HTTP 服务器
 *
 * 绕过 pmmpthread 对 cli-server SAPI 的限制，
 * 使用 stream_socket_server 在 CLI SAPI 下提供 HTTP 服务。
 *
 * 用法: php -n [options] php_router_server.php PORT DOCROOT ENTRYFILE
 */

// ==================== 配置 ====================
$port      = (int)($argv[1] ?? 8080);
$docRoot   = realpath($argv[2] ?? '.');
$entryFile = $argv[3] ?? 'index.php';

if (!$docRoot || !is_dir($docRoot)) {
    fwrite(STDERR, "[RouterServer] ERROR: Document root invalid: {$argv[2]}\n");
    exit(1);
}

// ==================== 启动服务器 ====================
$errno  = 0;
$errstr = '';
$server = @stream_socket_server(
    "tcp://127.0.0.1:$port", $errno, $errstr,
    STREAM_SERVER_BIND | STREAM_SERVER_LISTEN
);
if (!$server) {
    fwrite(STDERR, "[RouterServer] ERROR: Cannot bind to 127.0.0.1:$port: $errstr ($errno)\n");
    exit(1);
}

stream_set_blocking($server, true);
fwrite(STDERR, "[RouterServer] Listening on 127.0.0.1:$port\n");
fwrite(STDERR, "[RouterServer] Document root: $docRoot\n");
fwrite(STDERR, "[RouterServer] Entry file: $entryFile\n");

// 信号处理
$running  = true;
$hasPcntl = function_exists('pcntl_fork');
fwrite(STDERR, "[RouterServer] pcntl_fork=" . ($hasPcntl ? 'yes' : 'NO') . "\n");

if (function_exists('pcntl_signal')) {
    pcntl_signal(SIGTERM, function () use (&$running) { $running = false; });
    pcntl_signal(SIGINT,  function () use (&$running) { $running = false; });
    pcntl_signal(SIGCHLD, SIG_IGN); // 自动回收子进程
}

// ==================== Header Function Polyfill ====================
// pmmp PHP CLI SAPI 的 header()/headers_list() 不会真正跟踪 HTTP 头。
// 通过 disable_functions 禁用内置版本后，在此提供自定义实现，
// 将所有 header() 调用记录到全局数组，确保响应包含正确的 HTTP 头。

if (!function_exists('header')) {
    $GLOBALS['__router_custom_headers'] = [];

    function header(string $string, bool $replace = true, int $response_code = 0): void {
        // HTTP 状态行: HTTP/1.1 302 Found
        if (stripos($string, 'HTTP/') === 0) {
            if (preg_match('/HTTP\/[\d.]+\s+(\d+)/', $string, $m)) {
                http_response_code((int)$m[1]);
            }
            return;
        }

        $colonPos = strpos($string, ':');
        if ($colonPos !== false && $replace) {
            $headerName = strtolower(trim(substr($string, 0, $colonPos)));
            // Set-Cookie 不受 replace 影响（多个 Set-Cookie 可共存）
            if ($headerName !== 'set-cookie') {
                $GLOBALS['__router_custom_headers'] = array_values(array_filter(
                    $GLOBALS['__router_custom_headers'],
                    function($h) use ($headerName) {
                        $p = strpos($h, ':');
                        return $p === false || strtolower(trim(substr($h, 0, $p))) !== $headerName;
                    }
                ));
            }
        }

        $GLOBALS['__router_custom_headers'][] = $string;

        // 处理 response_code 参数
        if ($response_code > 0) {
            http_response_code($response_code);
        } elseif ($colonPos !== false) {
            $headerName = strtolower(trim(substr($string, 0, $colonPos)));
            if ($headerName === 'location') {
                $currentCode = http_response_code();
                if ($currentCode < 300 || $currentCode >= 400) {
                    http_response_code(302);
                }
            }
        }
    }

    function headers_list(): array {
        return $GLOBALS['__router_custom_headers'];
    }

    function headers_sent(&$filename = null, &$line = null): bool {
        return false;
    }

    function header_remove(?string $name = null): void {
        if ($name === null) {
            $GLOBALS['__router_custom_headers'] = [];
        } else {
            $lower = strtolower($name);
            $GLOBALS['__router_custom_headers'] = array_values(array_filter(
                $GLOBALS['__router_custom_headers'],
                function($h) use ($lower) {
                    $p = strpos($h, ':');
                    return $p === false || strtolower(trim(substr($h, 0, $p))) !== $lower;
                }
            ));
        }
    }

    function setcookie(
        string $name, string $value = '', int|array $expires_or_options = 0,
        string $path = '', string $domain = '', bool $secure = false, bool $httponly = false
    ): bool {
        if (is_array($expires_or_options)) {
            $o = $expires_or_options;
            $cookie = rawurlencode($name) . '=' . rawurlencode($value);
            if (!empty($o['expires']) && $o['expires'] > 0) {
                $cookie .= '; Expires=' . gmdate('D, d M Y H:i:s T', (int)$o['expires']);
                $cookie .= '; Max-Age=' . max(0, (int)$o['expires'] - time());
            }
            if (!empty($o['path']))     $cookie .= '; Path=' . $o['path'];
            if (!empty($o['domain']))   $cookie .= '; Domain=' . $o['domain'];
            if (!empty($o['secure']))   $cookie .= '; Secure';
            if (!empty($o['httponly']))  $cookie .= '; HttpOnly';
            if (!empty($o['samesite'])) $cookie .= '; SameSite=' . $o['samesite'];
        } else {
            $cookie = rawurlencode($name) . '=' . rawurlencode($value);
            if ($expires_or_options > 0) {
                $cookie .= '; Expires=' . gmdate('D, d M Y H:i:s T', $expires_or_options);
                $cookie .= '; Max-Age=' . max(0, $expires_or_options - time());
            }
            if ($path !== '')   $cookie .= '; Path=' . $path;
            if ($domain !== '') $cookie .= '; Domain=' . $domain;
            if ($secure)        $cookie .= '; Secure';
            if ($httponly)      $cookie .= '; HttpOnly';
        }
        header('Set-Cookie: ' . $cookie, false);
        return true;
    }

    function setrawcookie(
        string $name, string $value = '', int|array $expires_or_options = 0,
        string $path = '', string $domain = '', bool $secure = false, bool $httponly = false
    ): bool {
        if (is_array($expires_or_options)) {
            $o = $expires_or_options;
            $cookie = $name . '=' . $value;
            if (!empty($o['expires']) && $o['expires'] > 0) {
                $cookie .= '; Expires=' . gmdate('D, d M Y H:i:s T', (int)$o['expires']);
                $cookie .= '; Max-Age=' . max(0, (int)$o['expires'] - time());
            }
            if (!empty($o['path']))     $cookie .= '; Path=' . $o['path'];
            if (!empty($o['domain']))   $cookie .= '; Domain=' . $o['domain'];
            if (!empty($o['secure']))   $cookie .= '; Secure';
            if (!empty($o['httponly']))  $cookie .= '; HttpOnly';
            if (!empty($o['samesite'])) $cookie .= '; SameSite=' . $o['samesite'];
        } else {
            $cookie = $name . '=' . $value;
            if ($expires_or_options > 0) {
                $cookie .= '; Expires=' . gmdate('D, d M Y H:i:s T', $expires_or_options);
                $cookie .= '; Max-Age=' . max(0, $expires_or_options - time());
            }
            if ($path !== '')   $cookie .= '; Path=' . $path;
            if ($domain !== '') $cookie .= '; Domain=' . $domain;
            if ($secure)        $cookie .= '; Secure';
            if ($httponly)      $cookie .= '; HttpOnly';
        }
        header('Set-Cookie: ' . $cookie, false);
        return true;
    }
}

// ==================== Session Polyfill ====================
// pmmp PHP 没有 session 扩展，提供基于文件的 session 实现
// 对所有 PHP 项目自动生效（包括已有项目）

$GLOBALS['__session_doc_root'] = $docRoot;

if (!function_exists('session_start')) {
    $GLOBALS['__session_file']    = null;
    $GLOBALS['__session_id']      = '';
    $GLOBALS['__session_started'] = false;
    $GLOBALS['__session_name']    = 'PHPSESSID';

    function session_start(array $options = []): bool {
        if ($GLOBALS['__session_started']) return true;

        $docRoot    = $GLOBALS['__session_doc_root'] ?? '.';
        $sessionDir = $docRoot . '/.sessions';
        if (!is_dir($sessionDir)) @mkdir($sessionDir, 0777, true);

        // 从 cookie 获取 session ID，或生成新的
        $name = $GLOBALS['__session_name'];
        $sid  = $_COOKIE[$name] ?? '';
        if (empty($sid) || !preg_match('/^[a-zA-Z0-9,-]{22,256}$/', $sid)) {
            $sid = bin2hex(random_bytes(16));
            header("Set-Cookie: $name=$sid; Path=/; HttpOnly; SameSite=Lax");
        }

        $GLOBALS['__session_id']   = $sid;
        $GLOBALS['__session_file'] = $sessionDir . '/sess_' . $sid;

        $_SESSION = [];
        if (file_exists($GLOBALS['__session_file'])) {
            $data = @file_get_contents($GLOBALS['__session_file']);
            if ($data !== false) {
                $decoded = @json_decode($data, true);
                if (is_array($decoded)) $_SESSION = $decoded;
            }
        }

        $GLOBALS['__session_started'] = true;
        return true;
    }

    function session_write_close(): bool {
        if (!$GLOBALS['__session_started']) return true;
        if ($GLOBALS['__session_file']) {
            @file_put_contents(
                $GLOBALS['__session_file'],
                json_encode($_SESSION ?? [], JSON_UNESCAPED_UNICODE)
            );
        }
        $GLOBALS['__session_started'] = false;
        return true;
    }

    function session_commit(): bool {
        return session_write_close();
    }

    function session_destroy(): bool {
        $_SESSION = [];
        if ($GLOBALS['__session_file'] && file_exists($GLOBALS['__session_file'])) {
            @unlink($GLOBALS['__session_file']);
        }
        $GLOBALS['__session_started'] = false;
        return true;
    }

    function session_id(string $id = ''): string|false {
        if ($id !== '') $GLOBALS['__session_id'] = $id;
        return $GLOBALS['__session_id'];
    }

    function session_name(string $name = ''): string|false {
        $old = $GLOBALS['__session_name'];
        if ($name !== '') $GLOBALS['__session_name'] = $name;
        return $old;
    }

    function session_status(): int {
        // PHP_SESSION_ACTIVE = 2, PHP_SESSION_NONE = 1
        return $GLOBALS['__session_started'] ? 2 : 1;
    }

    function session_regenerate_id(bool $delete_old = false): bool {
        if (!$GLOBALS['__session_started']) return false;
        $oldFile = $GLOBALS['__session_file'];
        $sid     = bin2hex(random_bytes(16));
        $GLOBALS['__session_id'] = $sid;
        $name = $GLOBALS['__session_name'];
        header("Set-Cookie: $name=$sid; Path=/; HttpOnly; SameSite=Lax");
        $docRoot = $GLOBALS['__session_doc_root'] ?? '.';
        $GLOBALS['__session_file'] = $docRoot . '/.sessions/sess_' . $sid;
        if ($delete_old && $oldFile && file_exists($oldFile)) @unlink($oldFile);
        return true;
    }

    function session_unset(): bool {
        $_SESSION = [];
        return true;
    }
}

// ==================== 主循环 ====================
while ($running) {
    if (function_exists('pcntl_signal_dispatch')) {
        pcntl_signal_dispatch();
    }

    $read = [$server]; $write = $except = null;
    $changed = @stream_select($read, $write, $except, 1);
    if ($changed === false || $changed === 0) continue;

    $client = @stream_socket_accept($server, 0);
    if (!$client) continue;

    $request = readHttpRequest($client);
    if (!$request) {
        fwrite(STDERR, "[RouterServer] Empty request, closing\n");
        fclose($client);
        continue;
    }

    fwrite(STDERR, "[RouterServer] {$request['method']} {$request['path']}\n");

    // 内置健康检查端点 — 不执行任何 PHP 文件
    if ($request['path'] === '/__health') {
        sendRawResponse($client, 200, 'OK', ['Content-Type: text/plain'], 'OK');
        fclose($client);
        continue;
    }

    $filePath = resolveFilePath($request['path'], $docRoot, $entryFile);
    $ext = strtolower(pathinfo($filePath, PATHINFO_EXTENSION));

    // 静态文件 — 主进程直接响应
    if ($ext !== 'php' && file_exists($filePath) && !is_dir($filePath)) {
        serveStaticFile($client, $filePath);
        fclose($client);
        continue;
    }

    if (!file_exists($filePath)) {
        sendRawResponse($client, 404, 'Not Found', [], 'Not Found');
        fclose($client);
        continue;
    }

    // PHP 文件 — fork 子进程隔离执行
    if ($hasPcntl) {
        $pid = pcntl_fork();
        if ($pid === -1) {
            executePHP($client, $filePath, $request, $docRoot, $port);
            fclose($client);
        } elseif ($pid === 0) {
            fclose($server);
            executePHP($client, $filePath, $request, $docRoot, $port);
            fclose($client);
            exit(0);
        } else {
            fclose($client);
        }
    } else {
        executePHP($client, $filePath, $request, $docRoot, $port);
        fclose($client);
    }
}

fclose($server);
fwrite(STDERR, "[RouterServer] Stopped\n");
exit(0);

// ==================== HTTP 请求解析 ====================

function readHttpRequest($client): ?array {
    stream_set_timeout($client, 10);

    $requestLine = fgets($client, 8192);
    if (!$requestLine) return null;
    if (!preg_match('/^(\w+)\s+(\S+)\s+HTTP\/([\d.]+)/', trim($requestLine), $m)) {
        return null;
    }

    $method = strtoupper($m[1]);
    $uri    = $m[2];

    $headers = [];
    while (($line = fgets($client, 8192)) !== false) {
        $line = rtrim($line, "\r\n");
        if ($line === '') break;
        if (($pos = strpos($line, ':')) !== false) {
            $headers[strtolower(trim(substr($line, 0, $pos)))] = trim(substr($line, $pos + 1));
        }
    }

    $body = '';
    $contentLength = (int)($headers['content-length'] ?? 0);
    if ($contentLength > 0) {
        $remaining = $contentLength;
        while ($remaining > 0) {
            $chunk = fread($client, min($remaining, 65536));
            if ($chunk === false || $chunk === '') break;
            $body .= $chunk;
            $remaining -= strlen($chunk);
        }
    }

    $parts       = parse_url($uri);
    $path        = urldecode($parts['path'] ?? '/');
    $queryString = $parts['query'] ?? '';

    return compact('method', 'uri', 'path', 'queryString', 'headers', 'body');
}

// ==================== 路径解析 ====================

function resolveFilePath(string $path, string $docRoot, string $entryFile): string {
    $path = '/' . ltrim(str_replace('\\', '/', $path), '/');
    if (strpos($path, '..') !== false) return '';

    $filePath = $docRoot . $path;

    if (is_dir($filePath)) {
        foreach (['index.php', 'index.html', 'index.htm'] as $idx) {
            $c = $filePath . '/' . $idx;
            if (file_exists($c)) return $c;
        }
    }

    if (file_exists($filePath) && !is_dir($filePath)) return $filePath;

    // 框架路由: 不存在的路径 → 入口文件
    return $docRoot . '/' . $entryFile;
}

// ==================== PHP 执行 ====================

function executePHP($client, string $filePath, array $request, string $docRoot, int $port): void {
    $method      = $request['method'];
    $uri         = $request['uri'];
    $queryString = $request['queryString'];
    $headers     = $request['headers'];
    $body        = $request['body'];
    $path        = $request['path'];

    // 标记响应是否已发送，防止 shutdown function 重复发送
    $GLOBALS['__router_response_sent'] = false;

    // 将 body 写入临时文件并重定向 stdin (fd 0)
    // 这样 php://input 可以正确读取请求体
    $tmpInput = null;
    if (strlen($body) > 0) {
        $tmpInput = tempnam(sys_get_temp_dir(), 'phpinput_');
        file_put_contents($tmpInput, $body);
        fclose(STDIN);
        $GLOBALS['STDIN'] = fopen($tmpInput, 'r'); // 获得 fd 0
    }

    // 构建 $_SERVER
    $_SERVER = [
        'REQUEST_METHOD'    => $method,
        'REQUEST_URI'       => $uri,
        'QUERY_STRING'      => $queryString,
        'SCRIPT_FILENAME'   => $filePath,
        'SCRIPT_NAME'       => $path,
        'PHP_SELF'          => $path,
        'DOCUMENT_ROOT'     => $docRoot,
        'SERVER_NAME'       => '*********',
        'SERVER_PORT'       => (string)$port,
        'SERVER_PROTOCOL'   => 'HTTP/1.1',
        'SERVER_SOFTWARE'   => 'PHPRouterServer/1.0',
        'GATEWAY_INTERFACE' => 'CGI/1.1',
        'REDIRECT_STATUS'   => '200',
        'REMOTE_ADDR'       => '*********',
        'REMOTE_PORT'       => '0',
        'HTTP_HOST'         => $headers['host'] ?? "*********:$port",
        'CONTENT_TYPE'      => $headers['content-type'] ?? '',
        'CONTENT_LENGTH'    => (string)strlen($body),
        'REQUEST_TIME'      => time(),
        'REQUEST_TIME_FLOAT'=> microtime(true),
    ];
    foreach ($headers as $k => $v) {
        $_SERVER['HTTP_' . strtoupper(str_replace('-', '_', $k))] = $v;
    }

    // $_GET
    $_GET = [];
    parse_str($queryString, $_GET);

    // $_POST & $_FILES
    $_POST  = [];
    $_FILES = [];
    $ct = $headers['content-type'] ?? '';
    if (in_array($method, ['POST', 'PUT', 'PATCH']) && $body !== '') {
        if (stripos($ct, 'application/x-www-form-urlencoded') !== false) {
            parse_str($body, $_POST);
        } elseif (stripos($ct, 'multipart/form-data') !== false) {
            parseMultipart($ct, $body, $_POST, $_FILES);
        } elseif (stripos($ct, 'application/json') !== false) {
            // JSON body → $_POST fallback（当 php://input 在 CLI SAPI 下不可用时）
            $decoded = json_decode($body, true);
            if (is_array($decoded)) $_POST = $decoded;
        }
    }

    // $_COOKIE
    $_COOKIE = [];
    if (isset($headers['cookie'])) {
        foreach (explode(';', $headers['cookie']) as $c) {
            $c = trim($c);
            if (($eq = strpos($c, '=')) !== false) {
                $_COOKIE[trim(substr($c, 0, $eq))] = urldecode(trim(substr($c, $eq + 1)));
            }
        }
    }

    $_REQUEST = array_merge($_GET, $_POST, $_COOKIE);

    // 注册 shutdown function — 安全网：当脚本调用 exit() 或发生致命错误时发送响应
    $responseClient = $client;
    register_shutdown_function(function () use ($responseClient, $tmpInput) {
        // 如果响应已发送（正常路径），仅清理
        if (!empty($GLOBALS['__router_response_sent'])) {
            if ($tmpInput && file_exists($tmpInput)) @unlink($tmpInput);
            return;
        }
        $GLOBALS['__router_response_sent'] = true;

        $output = '';
        while (ob_get_level() > 0) {
            $output = ob_get_clean() . $output;
        }
        $code       = http_response_code() ?: 200;
        $headerList = headers_list();
        if (function_exists('header_remove')) header_remove();

        fwrite(STDERR, "[RouterServer] Shutdown handler sending response: $code (" . strlen($output) . " bytes)\n");
        sendRawResponse($responseClient, $code, httpStatusText($code), $headerList, $output);
        @fclose($responseClient);

        if ($tmpInput && file_exists($tmpInput)) @unlink($tmpInput);
    });

    ob_start();
    http_response_code(200);
    if (function_exists('header_remove')) header_remove();

    // 设置全局变量，让 PHP 脚本可以通过 $GLOBALS['__HTTP_RAW_BODY'] 获取原始请求体
    // 这是 php://input 在 CLI SAPI + fork 模式下的可靠替代方案
    $GLOBALS['__HTTP_RAW_BODY'] = $body;

    chdir($docRoot);

    try {
        include $filePath;
    } catch (\Throwable $e) {
        while (ob_get_level() > 1) ob_end_clean();
        http_response_code(500);
        fwrite(STDERR, "[RouterServer] PHP error: " . $e->getMessage() . "\n");
        echo "<h1>500 Internal Server Error</h1><pre>"
            . htmlspecialchars($e->getMessage()) . "\n"
            . htmlspecialchars($e->getTraceAsString()) . "</pre>";
    }

    // 自动保存 session 数据（模拟 PHP 原生 session 行为）
    if (function_exists('session_write_close') && ($GLOBALS['__session_started'] ?? false)) {
        session_write_close();
    }

    // 正常路径：显式发送响应（在 fclose 之前！）
    if (!$GLOBALS['__router_response_sent']) {
        $GLOBALS['__router_response_sent'] = true;
        $output = '';
        while (ob_get_level() > 0) {
            $output = ob_get_clean() . $output;
        }
        $code       = http_response_code() ?: 200;
        $headerList = headers_list();
        if (function_exists('header_remove')) header_remove();

        fwrite(STDERR, "[RouterServer] Sending response: $code (" . strlen($output) . " bytes)\n");
        sendRawResponse($client, $code, httpStatusText($code), $headerList, $output);
    }
}

// ==================== Multipart 解析 ====================

function parseMultipart(string $contentType, string $body, array &$post, array &$files): void {
    if (!preg_match('/boundary=(.+?)(?:;|$)/i', $contentType, $m)) return;
    $boundary = trim($m[1], '"');
    $parts    = explode("--$boundary", $body);
    array_shift($parts);

    foreach ($parts as $part) {
        $part = ltrim($part, "\r\n");
        if (str_starts_with(trim($part), '--') || empty(trim($part))) continue;

        $headerEnd = strpos($part, "\r\n\r\n");
        if ($headerEnd === false) continue;

        $ph = substr($part, 0, $headerEnd);
        $pb = rtrim(substr($part, $headerEnd + 4), "\r\n");

        if (!preg_match('/name="([^"]+)"/', $ph, $nm)) continue;
        $name = $nm[1];

        if (preg_match('/filename="([^"]*)"/', $ph, $fn) && $fn[1] !== '') {
            $tmp = tempnam(sys_get_temp_dir(), 'php_upload_');
            file_put_contents($tmp, $pb);
            $pct = 'application/octet-stream';
            if (preg_match('/Content-Type:\s*(.+)/i', $ph, $ctm)) $pct = trim($ctm[1]);
            $files[$name] = ['name'=>$fn[1],'type'=>$pct,'tmp_name'=>$tmp,'error'=>UPLOAD_ERR_OK,'size'=>strlen($pb)];
        } else {
            $post[$name] = $pb;
        }
    }
}

// ==================== 响应发送 ====================

function sendRawResponse($client, int $code, string $status, array $headers, string $body): void {
    $resp = "HTTP/1.1 $code $status\r\n";
    $hasCT = false; $hasCL = false;
    foreach ($headers as $h) {
        $resp .= "$h\r\n";
        if (stripos($h, 'content-type:') === 0) $hasCT = true;
        if (stripos($h, 'content-length:') === 0) $hasCL = true;
    }
    if (!$hasCT) $resp .= "Content-Type: text/html; charset=UTF-8\r\n";
    if (!$hasCL) $resp .= "Content-Length: " . strlen($body) . "\r\n";
    $resp .= "Connection: close\r\n\r\n" . $body;
    @fwrite($client, $resp);
}

// ==================== 静态文件服务 ====================

function serveStaticFile($client, string $filePath): void {
    $mime = getMimeType($filePath);
    $size = filesize($filePath);
    $resp = "HTTP/1.1 200 OK\r\nContent-Type: $mime\r\nContent-Length: $size\r\n"
          . "Connection: close\r\nCache-Control: public, max-age=3600\r\n\r\n";
    fwrite($client, $resp);
    $fp = fopen($filePath, 'rb');
    if ($fp) {
        while (!feof($fp)) { @fwrite($client, fread($fp, 65536)); }
        fclose($fp);
    }
}

function getMimeType(string $f): string {
    static $t = [
        'html'=>'text/html','htm'=>'text/html','css'=>'text/css',
        'js'=>'application/javascript','mjs'=>'application/javascript',
        'json'=>'application/json','xml'=>'application/xml',
        'txt'=>'text/plain','csv'=>'text/csv',
        'png'=>'image/png','jpg'=>'image/jpeg','jpeg'=>'image/jpeg',
        'gif'=>'image/gif','svg'=>'image/svg+xml','ico'=>'image/x-icon',
        'webp'=>'image/webp','avif'=>'image/avif',
        'woff'=>'font/woff','woff2'=>'font/woff2','ttf'=>'font/ttf','otf'=>'font/otf',
        'pdf'=>'application/pdf','zip'=>'application/zip',
        'mp4'=>'video/mp4','webm'=>'video/webm',
        'mp3'=>'audio/mpeg','ogg'=>'audio/ogg','wav'=>'audio/wav',
        'map'=>'application/json','wasm'=>'application/wasm',
    ];
    return $t[strtolower(pathinfo($f, PATHINFO_EXTENSION))] ?? 'application/octet-stream';
}

function httpStatusText(int $c): string {
    static $t = [
        200=>'OK',201=>'Created',204=>'No Content',
        301=>'Moved Permanently',302=>'Found',304=>'Not Modified',
        307=>'Temporary Redirect',308=>'Permanent Redirect',
        400=>'Bad Request',401=>'Unauthorized',403=>'Forbidden',
        404=>'Not Found',405=>'Method Not Allowed',422=>'Unprocessable Entity',
        500=>'Internal Server Error',502=>'Bad Gateway',503=>'Service Unavailable',
    ];
    return $t[$c] ?? 'Unknown';
}
