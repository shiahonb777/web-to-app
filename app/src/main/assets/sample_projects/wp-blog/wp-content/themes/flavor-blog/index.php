<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>我的博客 - WordPress 示例</title>
    <link rel="stylesheet" href="<?php echo get_stylesheet_uri(); ?>">
</head>
<body>
    <header class="site-header">
        <h1>📝 我的博客</h1>
        <p>WordPress 博客示例 · 由 WebToApp 创建</p>
    </header>
    <main class="posts-container">
        <article class="post-card">
            <h2>欢迎使用 WordPress</h2>
            <div class="post-meta">2024-01-15 · 管理员</div>
            <p>这是一篇示例文章。WordPress 是全球最流行的内容管理系统，本示例展示了如何将 WordPress 站点打包为 Android 应用。</p>
        </article>
        <article class="post-card">
            <h2>开始写作之旅</h2>
            <div class="post-meta">2024-01-14 · 管理员</div>
            <p>使用 WordPress 强大的编辑器来创作您的内容。支持富文本编辑、图片上传、分类标签等功能。</p>
        </article>
        <article class="post-card">
            <h2>SQLite 离线模式</h2>
            <div class="post-meta">2024-01-13 · 管理员</div>
            <p>本示例使用 SQLite 数据库替代 MySQL，无需网络连接即可运行完整的 WordPress 站点。</p>
        </article>
    </main>
</body>
</html>
