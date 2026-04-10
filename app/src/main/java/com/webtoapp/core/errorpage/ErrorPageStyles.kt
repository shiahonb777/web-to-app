package com.webtoapp.core.errorpage

/**
 * 内置错误页视觉风格生成器
 * 每种风格包含：全屏背景、SVG 插画、动效、标题、描述
 */
object ErrorPageStyles {

    fun getStyleCss(style: ErrorPageStyle): String = when (style) {
        ErrorPageStyle.MATERIAL -> minimalCss()
        ErrorPageStyle.SATELLITE -> satelliteCss()
        ErrorPageStyle.OCEAN -> oceanCss()
        ErrorPageStyle.FOREST -> forestCss()
        ErrorPageStyle.MINIMAL -> minimalCss()
        ErrorPageStyle.NEON -> neonCss()
    }

    fun getStyleBody(style: ErrorPageStyle, title: String, subtitle: String): String = when (style) {
        ErrorPageStyle.MATERIAL -> minimalBody(title, subtitle)
        ErrorPageStyle.SATELLITE -> satelliteBody(title, subtitle)
        ErrorPageStyle.OCEAN -> oceanBody(title, subtitle)
        ErrorPageStyle.FOREST -> forestBody(title, subtitle)
        ErrorPageStyle.MINIMAL -> minimalBody(title, subtitle)
        ErrorPageStyle.NEON -> neonBody(title, subtitle)
    }

    // ===================== SATELLITE 深空卫星 =====================

    private fun satelliteCss() = """
        body { background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%); }
        .stars { position:fixed;top:0;left:0;width:100%;height:100%;pointer-events:none;overflow:hidden; }
        .stars span {
            position:absolute; display:block; width:2px; height:2px;
            background:#fff; border-radius:50%; opacity:0;
            animation: twinkle 3s infinite;
        }
        .stars span:nth-child(odd) { animation-duration:4s; }
        .stars span:nth-child(3n) { animation-duration:2.5s; width:3px;height:3px; }
        @keyframes twinkle { 0%,100%{opacity:0;} 50%{opacity:1;} }
        .illustration svg { width:140px; height:140px; }
        .illustration svg .dish { fill:none; stroke:#8b7fd4; stroke-width:2; }
        .illustration svg .signal {
            fill:none; stroke:#a78bfa; stroke-width:1.5; opacity:0;
            animation: pulse-signal 2s ease-out infinite;
        }
        .illustration svg .signal:nth-child(2) { animation-delay:0.5s; }
        .illustration svg .signal:nth-child(3) { animation-delay:1s; }
        @keyframes pulse-signal { 0%{opacity:0.8;transform:scale(0.8);} 100%{opacity:0;transform:scale(1.4);} }
        .error-title { color: #e2d9f3; }
        .error-subtitle { color: #9d8ec7; }
        .retry-btn {
            background: linear-gradient(135deg, #667eea, #764ba2);
            box-shadow: 0 4px 20px rgba(102,126,234,0.4);
        }
        .retry-btn:active { transform:scale(0.96); box-shadow: 0 2px 10px rgba(102,126,234,0.3); }
    """.trimIndent()

    private fun satelliteBody(title: String, subtitle: String) = """
        <div class="stars" id="stars"></div>
        <div class="illustration">
            <svg viewBox="0 0 140 140" xmlns="http://www.w3.org/2000/svg">
                <circle class="signal" cx="70" cy="50" r="18" transform-origin="70 50"/>
                <circle class="signal" cx="70" cy="50" r="30" transform-origin="70 50"/>
                <circle class="signal" cx="70" cy="50" r="42" transform-origin="70 50"/>
                <path class="dish" d="M42 65 Q70 20 98 65"/>
                <line class="dish" x1="70" y1="55" x2="70" y2="100"/>
                <line class="dish" x1="55" y1="100" x2="85" y2="100"/>
                <circle cx="70" cy="52" r="4" fill="#a78bfa"/>
            </svg>
        </div>
        <h1 class="error-title">$title</h1>
        <p class="error-subtitle">$subtitle</p>
        <script>
        (function(){
            var s=document.getElementById('stars');
            for(var i=0;i<50;i++){
                var sp=document.createElement('span');
                sp.style.left=Math.random()*100+'%';
                sp.style.top=Math.random()*100+'%';
                sp.style.animationDelay=Math.random()*4+'s';
                s.appendChild(sp);
            }
        })();
        </script>
    """.trimIndent()

    // ===================== OCEAN 深海世界 =====================

    private fun oceanCss() = """
        body { background: linear-gradient(180deg, #0a1628 0%, #0d2847 40%, #134e6f 100%); }
        .bubbles { position:fixed;bottom:0;left:0;width:100%;height:100%;pointer-events:none;overflow:hidden; }
        .bubbles span {
            position:absolute; bottom:-20px; display:block;
            border-radius:50%; border:1px solid rgba(255,255,255,0.15);
            background: radial-gradient(circle at 30% 30%, rgba(255,255,255,0.12), transparent);
            animation: rise linear infinite;
        }
        @keyframes rise {
            0% { transform:translateY(0) translateX(0); opacity:0.6; }
            50% { opacity:0.3; }
            100% { transform:translateY(-110vh) translateX(30px); opacity:0; }
        }
        .illustration svg { width:150px; height:120px; }
        .illustration .whale {
            fill:none; stroke:#4db8c7; stroke-width:2; stroke-linecap:round;
            animation: float 4s ease-in-out infinite;
        }
        @keyframes float { 0%,100%{transform:translateY(0);} 50%{transform:translateY(-8px);} }
        .error-title { color: #b8e4f0; }
        .error-subtitle { color: #6fa8b9; }
        .retry-btn {
            background: linear-gradient(135deg, #0ea5e9, #2dd4bf);
            box-shadow: 0 4px 20px rgba(14,165,233,0.35);
        }
        .retry-btn:active { transform:scale(0.96); }
    """.trimIndent()

    private fun oceanBody(title: String, subtitle: String) = """
        <div class="bubbles" id="bubbles"></div>
        <div class="illustration">
            <svg viewBox="0 0 150 120" xmlns="http://www.w3.org/2000/svg">
                <g class="whale">
                    <path d="M30 60 Q50 35 90 50 Q120 58 130 55 Q125 48 135 42"/>
                    <path d="M30 60 Q50 80 90 68 Q120 62 130 55"/>
                    <circle cx="55" cy="52" r="2.5" fill="#4db8c7"/>
                    <path d="M90 50 Q95 42 100 48" fill="none"/>
                    <path d="M25 55 Q18 48 15 55 Q18 62 25 58"/>
                </g>
                <path d="M40 95 Q55 90 70 95 Q85 100 100 95" stroke="#1e6e82" stroke-width="1" fill="none" opacity="0.4"/>
                <path d="M20 105 Q40 100 60 105 Q80 110 100 105 Q120 100 140 105" stroke="#1e6e82" stroke-width="1" fill="none" opacity="0.25"/>
            </svg>
        </div>
        <h1 class="error-title">$title</h1>
        <p class="error-subtitle">$subtitle</p>
        <script>
        (function(){
            var b=document.getElementById('bubbles');
            for(var i=0;i<15;i++){
                var sp=document.createElement('span');
                var sz=8+Math.random()*20;
                sp.style.width=sz+'px'; sp.style.height=sz+'px';
                sp.style.left=Math.random()*100+'%';
                sp.style.animationDuration=(6+Math.random()*8)+'s';
                sp.style.animationDelay=Math.random()*6+'s';
                b.appendChild(sp);
            }
        })();
        </script>
    """.trimIndent()

    // ===================== FOREST 萤火森林 =====================

    private fun forestCss() = """
        body { background: linear-gradient(180deg, #0a1a0a 0%, #132413 50%, #1a321a 100%); }
        .fireflies { position:fixed;top:0;left:0;width:100%;height:100%;pointer-events:none;overflow:hidden; }
        .fireflies span {
            position:absolute; display:block; width:4px; height:4px;
            background: radial-gradient(circle, #d4f77f, #a0d84a 40%, transparent 70%);
            border-radius:50%; opacity:0;
            animation: glow 4s ease-in-out infinite;
        }
        @keyframes glow {
            0%,100%{opacity:0;transform:translate(0,0);}
            30%{opacity:0.9;}
            70%{opacity:0.5;}
            50%{transform:translate(10px,-15px);}
        }
        .illustration svg { width:160px; height:130px; }
        .tree { fill:none; stroke:#3d6b3d; stroke-width:2; stroke-linecap:round; }
        .tree-trunk { stroke:#5a4a3a; stroke-width:3; }
        .error-title { color: #c4e6a4; }
        .error-subtitle { color: #7da367; }
        .retry-btn {
            background: linear-gradient(135deg, #4ade80, #22c55e);
            box-shadow: 0 4px 20px rgba(74,222,128,0.3);
        }
        .retry-btn:active { transform:scale(0.96); }
    """.trimIndent()

    private fun forestBody(title: String, subtitle: String) = """
        <div class="fireflies" id="fireflies"></div>
        <div class="illustration">
            <svg viewBox="0 0 160 130" xmlns="http://www.w3.org/2000/svg">
                <line class="tree-trunk" x1="50" y1="80" x2="50" y2="120"/>
                <path class="tree" d="M25 80 L50 40 L75 80"/>
                <path class="tree" d="M30 65 L50 30 L70 65"/>
                <line class="tree-trunk" x1="110" y1="90" x2="110" y2="120"/>
                <path class="tree" d="M88 90 L110 50 L132 90"/>
                <path class="tree" d="M92 72 L110 40 L128 72"/>
                <line x1="0" y1="120" x2="160" y2="120" stroke="#2a4a2a" stroke-width="2"/>
                <path d="M60 118 Q65 112 72 118" stroke="#2a4a2a" stroke-width="1.5" fill="none"/>
                <path d="M95 118 Q100 114 105 118" stroke="#2a4a2a" stroke-width="1" fill="none"/>
            </svg>
        </div>
        <h1 class="error-title">$title</h1>
        <p class="error-subtitle">$subtitle</p>
        <script>
        (function(){
            var f=document.getElementById('fireflies');
            for(var i=0;i<25;i++){
                var sp=document.createElement('span');
                sp.style.left=Math.random()*100+'%';
                sp.style.top=20+Math.random()*70+'%';
                sp.style.animationDelay=Math.random()*5+'s';
                sp.style.animationDuration=(3+Math.random()*3)+'s';
                f.appendChild(sp);
            }
        })();
        </script>
    """.trimIndent()

    // ===================== MINIMAL 极简线条 =====================

    private fun minimalCss() = """
        body { background: #fafafa; }
        .illustration svg { width:120px; height:120px; }
        .line-art { fill:none; stroke:#333; stroke-width:1.5; stroke-linecap:round; }
        .line-art-dot { fill:#333; }
        .line-draw {
            stroke-dasharray: 200; stroke-dashoffset: 200;
            animation: draw 2s ease forwards;
        }
        @keyframes draw { to { stroke-dashoffset:0; } }
        .error-title { color: #1a1a1a; font-weight:300; letter-spacing:2px; }
        .error-subtitle { color: #888; font-weight:300; }
        .retry-btn {
            background: #1a1a1a; color: #fff;
            box-shadow: 0 2px 12px rgba(0,0,0,0.15);
        }
        .retry-btn:active { transform:scale(0.96); background:#333; }
        .game-link { color: #999 !important; }
    """.trimIndent()

    private fun minimalBody(title: String, subtitle: String) = """
        <div class="illustration">
            <svg viewBox="0 0 120 120" xmlns="http://www.w3.org/2000/svg">
                <circle class="line-art line-draw" cx="60" cy="50" r="30"/>
                <line class="line-art line-draw" x1="60" y1="40" x2="60" y2="55" style="animation-delay:0.8s"/>
                <circle class="line-art-dot" cx="60" cy="63" r="2" opacity="0">
                    <animate attributeName="opacity" from="0" to="1" begin="1.2s" dur="0.3s" fill="freeze"/>
                </circle>
                <line class="line-art" x1="30" y1="95" x2="90" y2="95" stroke-dasharray="4,4" opacity="0.3"/>
            </svg>
        </div>
        <h1 class="error-title">$title</h1>
        <p class="error-subtitle">$subtitle</p>
    """.trimIndent()

    // ===================== NEON 赛博霓虹 =====================

    private fun neonCss() = """
        body { background: #0a0a0a; }
        .neon-grid {
            position:fixed;top:0;left:0;width:100%;height:100%;pointer-events:none;
            background:
                linear-gradient(rgba(255,0,128,0.03) 1px, transparent 1px),
                linear-gradient(90deg, rgba(255,0,128,0.03) 1px, transparent 1px);
            background-size: 40px 40px;
            animation: grid-shift 8s linear infinite;
        }
        @keyframes grid-shift { from{transform:perspective(500px) rotateX(60deg) translateY(0);} to{transform:perspective(500px) rotateX(60deg) translateY(40px);} }
        .illustration svg { width:130px; height:130px; }
        .neon-stroke {
            fill:none; stroke-width:2; stroke-linecap:round;
            filter: drop-shadow(0 0 6px currentColor);
        }
        .neon-pink { stroke:#ff0080; color:#ff0080; }
        .neon-cyan { stroke:#00f0ff; color:#00f0ff; }
        .neon-blink { animation: neon-flicker 3s infinite; }
        @keyframes neon-flicker {
            0%,19%,21%,23%,25%,54%,56%,100%{opacity:1;}
            20%,24%,55%{opacity:0.4;}
        }
        .error-title {
            color: #ff0080; text-shadow: 0 0 10px rgba(255,0,128,0.6), 0 0 40px rgba(255,0,128,0.2);
            font-weight:700;
        }
        .error-subtitle { color: #00f0ff; text-shadow: 0 0 8px rgba(0,240,255,0.3); }
        .retry-btn {
            background: transparent; border: 1.5px solid #ff0080; color: #ff0080;
            text-shadow: 0 0 8px rgba(255,0,128,0.5);
            box-shadow: 0 0 15px rgba(255,0,128,0.2), inset 0 0 15px rgba(255,0,128,0.05);
        }
        .retry-btn:active { background:rgba(255,0,128,0.15); }
        .game-link { color: #00f0ff !important; text-shadow: 0 0 8px rgba(0,240,255,0.3); }
    """.trimIndent()

    private fun neonBody(title: String, subtitle: String) = """
        <div class="neon-grid"></div>
        <div class="illustration">
            <svg viewBox="0 0 130 130" xmlns="http://www.w3.org/2000/svg">
                <rect class="neon-stroke neon-cyan neon-blink" x="30" y="25" width="70" height="55" rx="5"/>
                <line class="neon-stroke neon-pink" x1="65" y1="80" x2="65" y2="95"/>
                <line class="neon-stroke neon-pink" x1="45" y1="95" x2="85" y2="95"/>
                <text x="65" y="58" text-anchor="middle" font-family="monospace" font-size="16" fill="#ff0080"
                      filter="drop-shadow(0 0 4px #ff0080)">404</text>
                <circle cx="50" cy="42" r="2" fill="#00f0ff" class="neon-blink"/>
                <circle cx="80" cy="42" r="2" fill="#00f0ff" class="neon-blink" style="animation-delay:0.5s"/>
            </svg>
        </div>
        <h1 class="error-title">$title</h1>
        <p class="error-subtitle">$subtitle</p>
    """.trimIndent()
}
