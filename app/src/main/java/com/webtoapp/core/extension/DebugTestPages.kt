package com.webtoapp.core.extension

import com.webtoapp.core.i18n.Strings

/**
 * 调试测试页面
 * 
 * 提供多种类型的测试页面，用于开发和调试扩展模块
 */
object DebugTestPages {
    
    /**
     * 获取所有测试页面
     */
    fun getAll(): List<TestPage> = listOf(
        basicHtmlPage(),
        formTestPage(),
        mediaTestPage(),
        adSimulatorPage(),
        popupTestPage(),
        scrollTestPage(),
        apiTestPage(),
        styleTestPage()
    )
    
    /**
     * 基础HTML测试页
     */
    private fun basicHtmlPage() = TestPage(
        id = "basic-html",
        name = Strings.testPageBasicHtml,
        description = Strings.testPageBasicHtmlDesc,
        icon = "📄",
        html = """
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>基础HTML测试页</title>
    <style>
        body { font-family: system-ui, sans-serif; padding: 20px; max-width: 800px; margin: 0 auto; }
        h1 { color: #333; }
        .card { background: #f5f5f5; padding: 15px; border-radius: 8px; margin: 10px 0; }
        .highlight { background: #fff3cd; padding: 10px; border-left: 4px solid #ffc107; }
        button { padding: 10px 20px; background: #007bff; color: white; border: none; border-radius: 5px; cursor: pointer; }
        button:hover { background: #0056b3; }
        a { color: #007bff; }
        img { max-width: 100%; border-radius: 8px; }
    </style>
</head>
<body>
    <h1>🧪 基础HTML测试页</h1>
    <p>这是一个用于测试扩展模块的基础页面。</p>
    
    <div class="card">
        <h2>文本内容</h2>
        <p>这是一段普通文本。<strong>这是粗体</strong>，<em>这是斜体</em>。</p>
        <p class="highlight">这是一个高亮提示框。</p>
    </div>
    
    <div class="card">
        <h2>链接</h2>
        <ul>
            <li><a href="https://example.com">示例链接1</a></li>
            <li><a href="https://test.com">示例链接2</a></li>
            <li><a href="#section">页内锚点</a></li>
        </ul>
    </div>
    
    <div class="card">
        <h2>按钮</h2>
        <button onclick="alert('按钮被点击!')">点击我</button>
        <button class="close-btn" onclick="this.parentElement.style.display='none'">关闭按钮</button>
    </div>
    
    <div class="card">
        <h2>图片</h2>
        <img src="https://via.placeholder.com/400x200" alt="测试图片">
    </div>
    
    <div class="card" id="section">
        <h2>表格</h2>
        <table border="1" style="width:100%;border-collapse:collapse;">
            <tr><th>名称</th><th>数值</th><th>状态</th></tr>
            <tr><td>项目A</td><td>100</td><td>正常</td></tr>
            <tr><td>项目B</td><td>200</td><td>警告</td></tr>
            <tr><td>项目C</td><td>300</td><td>错误</td></tr>
        </table>
    </div>
</body>
</html>
        """.trimIndent()
    )

    /**
     * 表单测试页
     */
    private fun formTestPage() = TestPage(
        id = "form-test",
        name = Strings.testPageForm,
        description = Strings.testPageFormDesc,
        icon = "📝",
        html = """
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>表单测试页</title>
    <style>
        body { font-family: system-ui, sans-serif; padding: 20px; max-width: 600px; margin: 0 auto; }
        .form-group { margin: 15px 0; }
        label { display: block; margin-bottom: 5px; font-weight: bold; }
        input, select, textarea { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 5px; box-sizing: border-box; }
        button { padding: 12px 24px; background: #28a745; color: white; border: none; border-radius: 5px; cursor: pointer; }
    </style>
</head>
<body>
    <h1>📝 表单测试页</h1>
    <form id="testForm">
        <div class="form-group">
            <label for="username">用户名</label>
            <input type="text" id="username" name="username" placeholder="请输入用户名">
        </div>
        <div class="form-group">
            <label for="email">邮箱</label>
            <input type="email" id="email" name="email" placeholder="请输入邮箱">
        </div>
        <div class="form-group">
            <label for="password">密码</label>
            <input type="password" id="password" name="password" placeholder="请输入密码">
        </div>
        <div class="form-group">
            <label for="phone">手机号</label>
            <input type="tel" id="phone" name="phone" placeholder="请输入手机号">
        </div>
        <div class="form-group">
            <label for="gender">性别</label>
            <select id="gender" name="gender">
                <option value="">请选择</option>
                <option value="male">男</option>
                <option value="female">女</option>
            </select>
        </div>
        <div class="form-group">
            <label for="bio">个人简介</label>
            <textarea id="bio" name="bio" rows="4" placeholder="请输入个人简介"></textarea>
        </div>
        <div class="form-group">
            <label><input type="checkbox" name="agree"> 我同意服务条款</label>
        </div>
        <button type="submit">提交</button>
    </form>
    <script>
        document.getElementById('testForm').onsubmit = function(e) {
            e.preventDefault();
            const data = new FormData(this);
            console.log('表单数据:', Object.fromEntries(data));
            alert('表单已提交！查看控制台获取数据。');
        };
    </script>
</body>
</html>
        """.trimIndent()
    )
    
    /**
     * 媒体测试页
     */
    private fun mediaTestPage() = TestPage(
        id = "media-test",
        name = Strings.testPageMedia,
        description = Strings.testPageMediaDesc,
        icon = "🎬",
        html = """
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>媒体测试页</title>
    <style>
        body { font-family: system-ui, sans-serif; padding: 20px; max-width: 800px; margin: 0 auto; }
        .media-section { margin: 20px 0; padding: 15px; background: #f5f5f5; border-radius: 8px; }
        img { max-width: 100%; border-radius: 8px; cursor: pointer; }
        video, audio { width: 100%; }
        .gallery { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
    </style>
</head>
<body>
    <h1>🎬 媒体测试页</h1>
    
    <div class="media-section">
        <h2>图片画廊</h2>
        <div class="gallery">
            <img src="https://via.placeholder.com/300x200/FF6B6B/fff?text=Image+1" alt="图片1">
            <img src="https://via.placeholder.com/300x200/4ECDC4/fff?text=Image+2" alt="图片2">
            <img src="https://via.placeholder.com/300x200/45B7D1/fff?text=Image+3" alt="图片3">
            <img src="https://via.placeholder.com/300x200/96CEB4/fff?text=Image+4" alt="图片4">
            <img src="https://via.placeholder.com/300x200/FFEAA7/333?text=Image+5" alt="图片5">
            <img src="https://via.placeholder.com/300x200/DDA0DD/fff?text=Image+6" alt="图片6">
        </div>
    </div>
    
    <div class="media-section">
        <h2>视频播放器</h2>
        <video controls poster="https://via.placeholder.com/800x450/333/fff?text=Video+Poster">
            <source src="https://www.w3schools.com/html/mov_bbb.mp4" type="video/mp4">
            您的浏览器不支持视频播放。
        </video>
    </div>
    
    <div class="media-section">
        <h2>音频播放器</h2>
        <audio controls>
            <source src="https://www.w3schools.com/html/horse.mp3" type="audio/mpeg">
            您的浏览器不支持音频播放。
        </audio>
    </div>
</body>
</html>
        """.trimIndent()
    )

    /**
     * 广告模拟页
     */
    private fun adSimulatorPage() = TestPage(
        id = "ad-simulator",
        name = Strings.testPageAdSimulator,
        description = Strings.testPageAdSimulatorDesc,
        icon = "🛡️",
        html = """
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>广告模拟测试页</title>
    <style>
        body { font-family: system-ui, sans-serif; padding: 20px; }
        .content { max-width: 800px; margin: 0 auto; }
        .ad-banner, .ads-container, .advertisement, [data-ad] {
            background: linear-gradient(45deg, #ff6b6b, #feca57);
            color: white; padding: 20px; margin: 15px 0; border-radius: 8px; text-align: center;
        }
        .sponsored { background: #dfe6e9; padding: 15px; margin: 10px 0; border-radius: 5px; }
        #popup-ad { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
            background: white; padding: 30px; border-radius: 10px; box-shadow: 0 10px 40px rgba(0,0,0,0.3);
            z-index: 1000; text-align: center; }
        .overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); z-index: 999; }
        .close-btn { position: absolute; top: 10px; right: 10px; cursor: pointer; font-size: 20px; }
    </style>
</head>
<body>
    <div class="content">
        <h1>🛡️ 广告模拟测试页</h1>
        <p>此页面模拟各种广告元素，用于测试广告拦截模块。</p>
        
        <div class="ad-banner">
            <h3>📢 横幅广告 (class="ad-banner")</h3>
            <p>这是一个模拟的横幅广告</p>
        </div>
        
        <p>这是正常的页面内容，不应该被隐藏。</p>
        
        <div class="ads-container">
            <h3>📢 广告容器 (class="ads-container")</h3>
            <p>这是另一个广告区域</p>
        </div>
        
        <p>更多正常内容...</p>
        
        <div class="advertisement">
            <h3>📢 广告区 (class="advertisement")</h3>
            <p>Advertisement Area</p>
        </div>
        
        <div data-ad="true">
            <h3>📢 数据广告 (data-ad="true")</h3>
            <p>Data Ad Element</p>
        </div>
        
        <div class="sponsored">
            <h4>赞助内容 (class="sponsored")</h4>
            <p>这是赞助商内容</p>
        </div>
    </div>
    
    <div class="overlay" id="overlay"></div>
    <div id="popup-ad">
        <span class="close-btn" onclick="document.getElementById('popup-ad').style.display='none';document.getElementById('overlay').style.display='none';">✕</span>
        <h2>🎁 弹窗广告</h2>
        <p>恭喜！您获得了一个测试弹窗！</p>
        <button onclick="document.getElementById('popup-ad').style.display='none';document.getElementById('overlay').style.display='none';">关闭</button>
    </div>
</body>
</html>
        """.trimIndent()
    )
    
    /**
     * 弹窗测试页
     */
    private fun popupTestPage() = TestPage(
        id = "popup-test",
        name = Strings.testPagePopup,
        description = Strings.testPagePopupDesc,
        icon = "💬",
        html = """
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>弹窗测试页</title>
    <style>
        body { font-family: system-ui, sans-serif; padding: 20px; max-width: 600px; margin: 0 auto; }
        button { padding: 12px 24px; margin: 5px; background: #007bff; color: white; border: none; border-radius: 5px; cursor: pointer; }
        .modal { display: none; position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); z-index: 1000; }
        .modal-content { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
            background: white; padding: 30px; border-radius: 10px; min-width: 300px; }
        .modal-close { position: absolute; top: 10px; right: 15px; font-size: 24px; cursor: pointer; }
        .cookie-banner { position: fixed; bottom: 0; left: 0; right: 0; background: #333; color: white; padding: 20px; z-index: 999; }
        .notification-prompt { position: fixed; top: 20px; right: 20px; background: white; padding: 20px; border-radius: 10px; box-shadow: 0 4px 20px rgba(0,0,0,0.2); z-index: 1000; }
    </style>
</head>
<body>
    <h1>💬 弹窗测试页</h1>
    <p>点击按钮测试各种弹窗：</p>
    
    <button onclick="document.getElementById('modal1').style.display='block'">打开模态框</button>
    <button onclick="alert('这是一个 Alert 弹窗')">Alert 弹窗</button>
    <button onclick="confirm('这是一个 Confirm 弹窗')">Confirm 弹窗</button>
    <button onclick="prompt('这是一个 Prompt 弹窗')">Prompt 弹窗</button>
    <button onclick="window.open('about:blank', '_blank', 'width=400,height=300')">打开新窗口</button>
    <button onclick="Notification.requestPermission()">请求通知权限</button>
    
    <div id="modal1" class="modal">
        <div class="modal-content">
            <span class="modal-close" onclick="this.parentElement.parentElement.style.display='none'">×</span>
            <h2>模态对话框</h2>
            <p>这是一个模态对话框示例。</p>
            <button onclick="this.parentElement.parentElement.style.display='none'">关闭</button>
        </div>
    </div>
    
    <div class="cookie-banner" id="cookieBanner">
        🍪 本网站使用 Cookie 来提升您的体验。
        <button onclick="this.parentElement.style.display='none'" style="margin-left:20px;">接受</button>
        <button onclick="this.parentElement.style.display='none'">拒绝</button>
    </div>
    
    <div class="notification-prompt" id="notifPrompt">
        🔔 是否允许发送通知？
        <div style="margin-top:10px;">
            <button onclick="this.parentElement.parentElement.style.display='none'">允许</button>
            <button onclick="this.parentElement.parentElement.style.display='none'">拒绝</button>
        </div>
    </div>
</body>
</html>
        """.trimIndent()
    )

    /**
     * 滚动测试页
     */
    private fun scrollTestPage() = TestPage(
        id = "scroll-test",
        name = Strings.testPageScroll,
        description = Strings.testPageScrollDesc,
        icon = "📜",
        html = """
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>滚动测试页</title>
    <style>
        body { font-family: system-ui, sans-serif; padding: 20px; max-width: 800px; margin: 0 auto; }
        .section { min-height: 500px; padding: 30px; margin: 20px 0; border-radius: 10px; }
        .section:nth-child(odd) { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }
        .section:nth-child(even) { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; }
        h2 { font-size: 2em; }
        .scroll-indicator { position: fixed; top: 10px; right: 10px; background: rgba(0,0,0,0.7); color: white; padding: 10px; border-radius: 5px; }
    </style>
</head>
<body>
    <div class="scroll-indicator" id="scrollIndicator">滚动: 0%</div>
    
    <h1>📜 滚动测试页</h1>
    <p>这是一个长页面，用于测试滚动相关的扩展模块。</p>
    
    <div class="section"><h2>第 1 节</h2><p>向下滚动查看更多内容...</p></div>
    <div class="section"><h2>第 2 节</h2><p>继续滚动...</p></div>
    <div class="section"><h2>第 3 节</h2><p>还有更多...</p></div>
    <div class="section"><h2>第 4 节</h2><p>快到底了...</p></div>
    <div class="section"><h2>第 5 节</h2><p>这是最后一节！</p></div>
    
    <script>
        window.addEventListener('scroll', () => {
            const scrollTop = window.scrollY;
            const docHeight = document.documentElement.scrollHeight - window.innerHeight;
            const scrollPercent = Math.round((scrollTop / docHeight) * 100);
            document.getElementById('scrollIndicator').textContent = '滚动: ' + scrollPercent + '%';
        });
    </script>
</body>
</html>
        """.trimIndent()
    )
    
    /**
     * API测试页
     */
    private fun apiTestPage() = TestPage(
        id = "api-test",
        name = "API测试页",
        description = "测试网络请求和API调用",
        icon = "🌐",
        html = """
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>API测试页</title>
    <style>
        body { font-family: system-ui, sans-serif; padding: 20px; max-width: 800px; margin: 0 auto; }
        button { padding: 10px 20px; margin: 5px; background: #007bff; color: white; border: none; border-radius: 5px; cursor: pointer; }
        .result { background: #f5f5f5; padding: 15px; border-radius: 8px; margin: 10px 0; font-family: monospace; white-space: pre-wrap; max-height: 300px; overflow-y: auto; }
        .loading { color: #666; }
        .error { color: #dc3545; }
        .success { color: #28a745; }
    </style>
</head>
<body>
    <h1>🌐 API测试页</h1>
    <p>测试各种网络请求：</p>
    
    <div>
        <button onclick="testFetch()">Fetch 请求</button>
        <button onclick="testXHR()">XHR 请求</button>
        <button onclick="testBeacon()">Beacon 请求</button>
        <button onclick="testWebSocket()">WebSocket</button>
        <button onclick="clearResult()">清除结果</button>
    </div>
    
    <div id="result" class="result">点击按钮测试网络请求...</div>
    
    <script>
        const resultEl = document.getElementById('result');
        
        function log(msg, type = '') {
            const time = new Date().toLocaleTimeString();
            resultEl.innerHTML += '<div class="' + type + '">[' + time + '] ' + msg + '</div>';
            resultEl.scrollTop = resultEl.scrollHeight;
        }
        
        function clearResult() { resultEl.innerHTML = ''; }
        
        async function testFetch() {
            log('发起 Fetch 请求...', 'loading');
            try {
                const res = await fetch('https://jsonplaceholder.typicode.com/posts/1');
                const data = await res.json();
                log('Fetch 成功: ' + JSON.stringify(data, null, 2), 'success');
            } catch(e) {
                log('Fetch 失败: ' + e.message, 'error');
            }
        }
        
        function testXHR() {
            log('发起 XHR 请求...', 'loading');
            const xhr = new XMLHttpRequest();
            xhr.open('GET', 'https://jsonplaceholder.typicode.com/users/1');
            xhr.onload = () => log('XHR 成功: ' + xhr.responseText.substring(0, 200) + '...', 'success');
            xhr.onerror = () => log('XHR 失败', 'error');
            xhr.send();
        }
        
        function testBeacon() {
            const success = navigator.sendBeacon('/beacon-test', 'test data');
            log('Beacon 发送: ' + (success ? '成功' : '失败'), success ? 'success' : 'error');
        }
        
        function testWebSocket() {
            log('尝试 WebSocket 连接...', 'loading');
            try {
                const ws = new WebSocket('wss://echo.websocket.org');
                ws.onopen = () => { log('WebSocket 已连接', 'success'); ws.send('Hello'); };
                ws.onmessage = (e) => log('WebSocket 收到: ' + e.data, 'success');
                ws.onerror = () => log('WebSocket 错误', 'error');
                ws.onclose = () => log('WebSocket 已关闭');
            } catch(e) {
                log('WebSocket 失败: ' + e.message, 'error');
            }
        }
    </script>
</body>
</html>
        """.trimIndent()
    )
    
    /**
     * 样式测试页
     */
    private fun styleTestPage() = TestPage(
        id = "style-test",
        name = Strings.testPageStyle,
        description = Strings.testPageStyleDesc,
        icon = "🎨",
        html = """
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>样式测试页</title>
    <style>
        body { font-family: system-ui, sans-serif; padding: 20px; max-width: 800px; margin: 0 auto; background: #fff; color: #333; }
        .color-box { width: 100px; height: 100px; display: inline-block; margin: 10px; border-radius: 10px; }
        .typography { margin: 20px 0; padding: 20px; background: #f5f5f5; border-radius: 10px; }
        .layout-demo { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin: 20px 0; }
        .layout-item { background: #e0e0e0; padding: 20px; text-align: center; border-radius: 5px; }
        .animation-demo { padding: 20px; background: linear-gradient(45deg, #667eea, #764ba2); color: white; border-radius: 10px; }
    </style>
</head>
<body>
    <h1>🎨 样式测试页</h1>
    
    <h2>颜色</h2>
    <div class="color-box" style="background:#FF6B6B"></div>
    <div class="color-box" style="background:#4ECDC4"></div>
    <div class="color-box" style="background:#45B7D1"></div>
    <div class="color-box" style="background:#96CEB4"></div>
    <div class="color-box" style="background:#FFEAA7"></div>
    
    <h2>排版</h2>
    <div class="typography">
        <h1>标题 H1</h1>
        <h2>标题 H2</h2>
        <h3>标题 H3</h3>
        <p>这是一段正文文本。<strong>粗体文本</strong>，<em>斜体文本</em>，<u>下划线文本</u>。</p>
        <p style="font-size:12px">小号文字 (12px)</p>
        <p style="font-size:16px">正常文字 (16px)</p>
        <p style="font-size:20px">大号文字 (20px)</p>
    </div>
    
    <h2>布局</h2>
    <div class="layout-demo">
        <div class="layout-item">项目 1</div>
        <div class="layout-item">项目 2</div>
        <div class="layout-item">项目 3</div>
        <div class="layout-item">项目 4</div>
        <div class="layout-item">项目 5</div>
        <div class="layout-item">项目 6</div>
    </div>
    
    <h2>动画</h2>
    <div class="animation-demo">
        <p>这个区域可以用来测试动画和过渡效果。</p>
    </div>
</body>
</html>
        """.trimIndent()
    )
}

/**
 * 测试页面数据类
 */
data class TestPage(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val html: String
) {
    /**
     * 生成 data URL
     */
    fun toDataUrl(): String {
        val encoded = android.util.Base64.encodeToString(
            html.toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )
        return "data:text/html;base64,$encoded"
    }
}
