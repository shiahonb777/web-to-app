package com.webtoapp.core.engine.shields

/**
 * ReaderMode — 阅读模式 / SpeedReader
 *
 * 功能：
 * - 提取页面正文内容（类 Readability 算法）
 * - 移除广告、导航、侧边栏
 * - 生成纯净阅读 HTML
 * - 支持字体大小、背景色、行距调节
 */
class ReaderMode {

    /**
     * 阅读模式主题
     */
    enum class Theme(val bgColor: String, val textColor: String, val displayName: String) {
        LIGHT("#FFFFFF", "#1A1A1A", "浅色"),
        SEPIA("#F4ECD8", "#5B4636", "护眼"),
        DARK("#1A1A1A", "#D4D4D4", "深色"),
        BLACK("#000000", "#C8C8C8", "纯黑")
    }

    /**
     * 生成提取正文的 JavaScript 代码
     * 返回一个 JS 表达式，执行后通过回调返回提取的 HTML 内容
     */
    fun generateExtractScript(): String {
        return """
            (function() {
                'use strict';
                
                // ========== Readability-lite algorithm ==========
                
                // Scores for common content containers
                var POSITIVE_NAMES = /article|body|content|entry|main|page|post|text|blog|story/i;
                var NEGATIVE_NAMES = /combx|comment|contact|foot|footer|footnote|header|menu|meta|nav|outbrain|pager|pagination|promo|related|remark|rss|share|shoutbox|sidebar|sponsor|social|tags|tool|widget|ad-|ads-|advertisement/i;
                var BLOCK_ELEMENTS = new Set(['DIV','ARTICLE','SECTION','MAIN','P','BLOCKQUOTE','PRE','TABLE','UL','OL','FIGURE','FIGCAPTION']);
                
                function getTextLength(node) {
                    return (node.textContent || '').trim().length;
                }
                
                function scoreElement(el) {
                    var score = 0;
                    var tag = el.tagName;
                    var id = (el.id || '').toLowerCase();
                    var cls = (el.className || '').toString().toLowerCase();
                    
                    // Tag score
                    if (tag === 'ARTICLE') score += 30;
                    else if (tag === 'MAIN') score += 25;
                    else if (tag === 'SECTION') score += 5;
                    else if (tag === 'DIV') score += 0;
                    
                    // ID/class score
                    if (POSITIVE_NAMES.test(id)) score += 25;
                    if (POSITIVE_NAMES.test(cls)) score += 25;
                    if (NEGATIVE_NAMES.test(id)) score -= 25;
                    if (NEGATIVE_NAMES.test(cls)) score -= 25;
                    
                    // Text density
                    var textLen = getTextLength(el);
                    var linkLen = 0;
                    el.querySelectorAll('a').forEach(function(a) {
                        linkLen += getTextLength(a);
                    });
                    
                    var linkDensity = textLen > 0 ? linkLen / textLen : 1;
                    score += Math.min(textLen / 100, 30);
                    score -= linkDensity * 50;
                    
                    // Paragraph count bonus
                    var pCount = el.querySelectorAll('p').length;
                    score += Math.min(pCount * 3, 30);
                    
                    // Image count bonus (articles usually have images)
                    var imgCount = el.querySelectorAll('img').length;
                    if (imgCount > 0 && imgCount <= 10) score += imgCount * 2;
                    
                    return score;
                }
                
                // Find the best content node
                var candidates = document.querySelectorAll('article, main, [role="main"], div, section');
                var bestNode = null;
                var bestScore = -Infinity;
                
                for (var i = 0; i < candidates.length; i++) {
                    var el = candidates[i];
                    var textLen = getTextLength(el);
                    if (textLen < 200) continue; // Skip too-short elements
                    
                    var score = scoreElement(el);
                    if (score > bestScore) {
                        bestScore = score;
                        bestNode = el;
                    }
                }
                
                if (!bestNode) {
                    // Fallback: use body
                    bestNode = document.body;
                }
                
                // Clone and clean content
                var content = bestNode.cloneNode(true);
                
                // Remove unwanted elements
                var removeSelectors = [
                    'script', 'style', 'noscript', 'iframe', 'form',
                    'nav', 'header:not(article header)', 'footer:not(article footer)',
                    '[role="navigation"]', '[role="banner"]', '[role="complementary"]',
                    '.sidebar', '.side-bar', '.ad', '.ads', '.advertisement',
                    '.social-share', '.share-buttons', '.comments', '.comment',
                    '.related-posts', '.related-articles', '.recommended',
                    '.newsletter', '.subscribe', '.popup', '.modal',
                    '[class*="cookie"]', '[class*="consent"]', '[class*="gdpr"]',
                    '[class*="social"]', '[class*="share"]', '[class*="ad-"]',
                    '[id*="sidebar"]', '[id*="footer"]', '[id*="comment"]',
                    '[id*="related"]', '[id*="recommend"]'
                ];
                
                removeSelectors.forEach(function(sel) {
                    content.querySelectorAll(sel).forEach(function(el) {
                        el.remove();
                    });
                });
                
                // Get title
                var title = '';
                var h1 = document.querySelector('h1');
                if (h1) {
                    title = h1.textContent.trim();
                } else {
                    title = document.title || '';
                }
                
                // Get metadata
                var siteName = '';
                var metaSite = document.querySelector('meta[property="og:site_name"]');
                if (metaSite) siteName = metaSite.getAttribute('content') || '';
                
                var author = '';
                var metaAuthor = document.querySelector('meta[name="author"]');
                if (metaAuthor) author = metaAuthor.getAttribute('content') || '';
                
                // Build result
                var result = {
                    title: title,
                    siteName: siteName,
                    author: author,
                    content: content.innerHTML,
                    url: window.location.href,
                    textLength: getTextLength(content),
                    isReadable: getTextLength(content) > 500
                };
                
                return JSON.stringify(result);
            })();
        """.trimIndent()
    }

    /**
     * 生成阅读模式 HTML 页面
     *
     * @param title 文章标题
     * @param siteName 站点名称
     * @param author 作者
     * @param content HTML 正文内容
     * @param url 原文 URL
     * @param theme 阅读主题
     * @param fontSize 字体大小 (px)
     * @param lineHeight 行高倍数
     */
    fun generateReaderHtml(
        title: String,
        siteName: String,
        author: String,
        content: String,
        url: String,
        theme: Theme = Theme.LIGHT,
        fontSize: Int = 18,
        lineHeight: Float = 1.8f
    ): String {
        return """
            <!DOCTYPE html>
            <html lang="zh">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0">
                <title>$title</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    
                    body {
                        background-color: ${theme.bgColor};
                        color: ${theme.textColor};
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
                        font-size: ${fontSize}px;
                        line-height: ${lineHeight};
                        padding: 20px 16px 80px;
                        max-width: 680px;
                        margin: 0 auto;
                        word-wrap: break-word;
                        overflow-wrap: break-word;
                        -webkit-text-size-adjust: 100%;
                    }
                    
                    .reader-header {
                        margin-bottom: 24px;
                        padding-bottom: 16px;
                        border-bottom: 1px solid ${if (theme == Theme.LIGHT || theme == Theme.SEPIA) "#E0E0E0" else "#333333"};
                    }
                    
                    .reader-title {
                        font-size: ${(fontSize * 1.6).toInt()}px;
                        font-weight: 700;
                        line-height: 1.3;
                        margin-bottom: 12px;
                    }
                    
                    .reader-meta {
                        font-size: ${(fontSize * 0.8).toInt()}px;
                        opacity: 0.6;
                    }
                    
                    .reader-meta a {
                        color: inherit;
                        text-decoration: underline;
                    }
                    
                    .reader-content p {
                        margin-bottom: 1em;
                    }
                    
                    .reader-content h1, .reader-content h2, .reader-content h3,
                    .reader-content h4, .reader-content h5, .reader-content h6 {
                        margin-top: 1.5em;
                        margin-bottom: 0.5em;
                        line-height: 1.3;
                    }
                    
                    .reader-content h2 { font-size: ${(fontSize * 1.3).toInt()}px; }
                    .reader-content h3 { font-size: ${(fontSize * 1.15).toInt()}px; }
                    
                    .reader-content img {
                        max-width: 100%;
                        height: auto;
                        border-radius: 8px;
                        margin: 16px 0;
                    }
                    
                    .reader-content a {
                        color: #4A90D9;
                        text-decoration: underline;
                    }
                    
                    .reader-content blockquote {
                        border-left: 3px solid ${if (theme == Theme.LIGHT || theme == Theme.SEPIA) "#CCCCCC" else "#555555"};
                        padding-left: 16px;
                        margin: 16px 0;
                        opacity: 0.85;
                        font-style: italic;
                    }
                    
                    .reader-content pre, .reader-content code {
                        font-family: 'SFMono-Regular', Consolas, 'Courier New', monospace;
                        font-size: ${(fontSize * 0.85).toInt()}px;
                        background: ${if (theme == Theme.LIGHT || theme == Theme.SEPIA) "#F5F5F5" else "#2A2A2A"};
                        border-radius: 4px;
                    }
                    
                    .reader-content pre {
                        padding: 16px;
                        overflow-x: auto;
                        margin: 16px 0;
                    }
                    
                    .reader-content code {
                        padding: 2px 6px;
                    }
                    
                    .reader-content ul, .reader-content ol {
                        padding-left: 24px;
                        margin-bottom: 1em;
                    }
                    
                    .reader-content li {
                        margin-bottom: 0.3em;
                    }
                    
                    .reader-content table {
                        width: 100%;
                        border-collapse: collapse;
                        margin: 16px 0;
                    }
                    
                    .reader-content th, .reader-content td {
                        border: 1px solid ${if (theme == Theme.LIGHT || theme == Theme.SEPIA) "#DDDDDD" else "#444444"};
                        padding: 8px 12px;
                        text-align: left;
                    }
                    
                    .reader-content figure {
                        margin: 16px 0;
                        text-align: center;
                    }
                    
                    .reader-content figcaption {
                        font-size: ${(fontSize * 0.8).toInt()}px;
                        opacity: 0.6;
                        margin-top: 8px;
                    }
                </style>
            </head>
            <body>
                <article>
                    <header class="reader-header">
                        <h1 class="reader-title">${escapeHtml(title)}</h1>
                        <div class="reader-meta">
                            ${if (siteName.isNotBlank()) "<span>$siteName</span>" else ""}
                            ${if (author.isNotBlank()) "<span> · $author</span>" else ""}
                            <br><a href="$url">查看原文</a>
                        </div>
                    </header>
                    <div class="reader-content">
                        $content
                    </div>
                </article>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * 检测当前页面是否适合阅读模式的 JS 脚本
     * 返回 "true" 或 "false"
     */
    fun generateDetectScript(): String {
        return """
            (function() {
                // Quick heuristic: check if page has enough text content
                var textLen = (document.body ? document.body.innerText : '').length;
                var pCount = document.querySelectorAll('p').length;
                var articleTag = document.querySelector('article, [role="article"]');
                
                var isReadable = textLen > 800 && pCount >= 3;
                if (articleTag) isReadable = textLen > 400;
                
                return isReadable ? 'true' : 'false';
            })();
        """.trimIndent()
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
