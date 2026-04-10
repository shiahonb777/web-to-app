package com.webtoapp.core.errorpage

import com.webtoapp.core.i18n.Strings

/**
 * 错误页内嵌小游戏生成器
 * 4 款精品触摸小游戏，质感优先
 */
object ErrorPageGames {

    fun getGameJs(type: MiniGameType): String = when (type) {
        MiniGameType.RANDOM -> getGameJs(randomGame())
        MiniGameType.BREAKOUT -> breakoutGame()
        MiniGameType.MAZE -> mazeGame()
        MiniGameType.STAR_CATCH -> starCatchGame()
        MiniGameType.INK_ZEN -> inkZenGame()
    }

    private fun randomGame(): MiniGameType {
        val games = listOf(MiniGameType.BREAKOUT, MiniGameType.MAZE, MiniGameType.STAR_CATCH, MiniGameType.INK_ZEN)
        return games[(Math.random() * games.size).toInt().coerceIn(0, games.size - 1)]
    }

    // ===================== 弹球消消 BREAKOUT =====================

    private fun breakoutGame() = """
    (function(){
        var C=document.getElementById('gameCanvas'),ctx=C.getContext('2d');
        var W=C.width,H=C.height;
        var dpr=window.devicePixelRatio||1;
        C.width=W*dpr;C.height=H*dpr;
        C.style.width=W+'px';C.style.height=H+'px';
        ctx.scale(dpr,dpr);
        
        // 配色
        var colors=['#667eea','#764ba2','#f093fb','#4facfe','#43e97b','#fa709a'];
        var bgColor='rgba(0,0,0,0.85)';
        
        // 挡板
        var pw=70,ph=10,px=W/2-pw/2,py=H-30;
        // 球
        var br=5,bx=W/2,by=H-50,bdx=2.5,bdy=-2.5;
        // 砖块
        var cols=6,rows=4,bw,bh=14,bp=4,bricks=[];
        bw=(W-bp*(cols+1))/cols;
        for(var r=0;r<rows;r++)for(var c=0;c<cols;c++){
            bricks.push({x:bp+c*(bw+bp),y:40+r*(bh+bp),w:bw,h:bh,alive:true,color:colors[(r*cols+c)%colors.length]});
        }
        var score=0,lives=3,running=true;
        
        // 触摸
        C.addEventListener('touchmove',function(e){e.preventDefault();var t=e.touches[0];var r=C.getBoundingClientRect();px=t.clientX-r.left-pw/2;if(px<0)px=0;if(px>W-pw)px=W-pw;},{passive:false});
        C.addEventListener('mousemove',function(e){var r=C.getBoundingClientRect();px=e.clientX-r.left-pw/2;if(px<0)px=0;if(px>W-pw)px=W-pw;});
        
        function draw(){
            ctx.fillStyle=bgColor;ctx.fillRect(0,0,W,H);
            // 砖块
            bricks.forEach(function(b){
                if(!b.alive)return;
                ctx.fillStyle=b.color;
                ctx.beginPath();
                ctx.roundRect(b.x,b.y,b.w,b.h,3);
                ctx.fill();
                // 玻璃高光
                ctx.fillStyle='rgba(255,255,255,0.2)';
                ctx.fillRect(b.x+2,b.y+1,b.w-4,b.h/3);
            });
            // 挡板
            var g=ctx.createLinearGradient(px,py,px,py+ph);
            g.addColorStop(0,'#e0e0e0');g.addColorStop(1,'#999');
            ctx.fillStyle=g;
            ctx.beginPath();ctx.roundRect(px,py,pw,ph,5);ctx.fill();
            // 球
            ctx.fillStyle='#fff';ctx.beginPath();ctx.arc(bx,by,br,0,Math.PI*2);ctx.fill();
            ctx.fillStyle='rgba(255,255,255,0.3)';ctx.beginPath();ctx.arc(bx-1.5,by-1.5,br*0.4,0,Math.PI*2);ctx.fill();
            // HUD
            ctx.fillStyle='rgba(255,255,255,0.6)';ctx.font='12px sans-serif';
            ctx.fillText('${Strings.gameScore}: '+score,8,18);
            ctx.fillText('${Strings.gameLives}: '+lives,W-60,18);
        }
        
        function update(){
            if(!running)return;
            bx+=bdx;by+=bdy;
            // 墙壁
            if(bx<br||bx>W-br)bdx=-bdx;
            if(by<br)bdy=-bdy;
            // 挡板碰撞
            if(by+br>=py&&by+br<=py+ph&&bx>=px&&bx<=px+pw){
                bdy=-Math.abs(bdy);
                var hit=(bx-(px+pw/2))/(pw/2);
                bdx=hit*3.5;
            }
            // 掉落
            if(by>H+br){
                lives--;
                if(lives<=0){running=false;return;}
                bx=W/2;by=H-50;bdx=2.5;bdy=-2.5;
            }
            // 砖块碰撞
            bricks.forEach(function(b){
                if(!b.alive)return;
                if(bx+br>b.x&&bx-br<b.x+b.w&&by+br>b.y&&by-br<b.y+b.h){
                    b.alive=false;bdy=-bdy;score+=10;
                }
            });
            // 胜利检查
            if(bricks.every(function(b){return !b.alive;})){running=false;}
        }
        
        function loop(){draw();update();if(running)requestAnimationFrame(loop);else{
            ctx.fillStyle='rgba(0,0,0,0.7)';ctx.fillRect(0,0,W,H);
            ctx.fillStyle='#fff';ctx.font='bold 18px sans-serif';ctx.textAlign='center';
            ctx.fillText(lives<=0?'${Strings.gameOver}':'${Strings.gameYouWin}',W/2,H/2-10);
            ctx.font='13px sans-serif';ctx.fillText('${Strings.gameScore}: '+score,W/2,H/2+15);
            ctx.fillText('${Strings.gameTapToRestart}',W/2,H/2+38);ctx.textAlign='left';
            C.onclick=function(){location.reload();};
        }}
        loop();
    })();
    """.trimIndent()

    // ===================== 迷宫行者 MAZE =====================

    private fun mazeGame() = """
    (function(){
        var C=document.getElementById('gameCanvas'),ctx=C.getContext('2d');
        var W=C.width,H=C.height;
        var dpr=window.devicePixelRatio||1;
        C.width=W*dpr;C.height=H*dpr;
        C.style.width=W+'px';C.style.height=H+'px';
        ctx.scale(dpr,dpr);
        
        var cols=11,rows=13,cw=Math.floor(W/cols),ch=Math.floor(H/rows);
        var grid=[],stack=[];
        
        // 生成迷宫 (DFS)
        for(var r=0;r<rows;r++){grid[r]=[];for(var c=0;c<cols;c++)grid[r][c]={v:false,w:{t:true,r:true,b:true,l:true}};}
        function neighbors(r,c){
            var n=[];
            if(r>0&&!grid[r-1][c].v)n.push([r-1,c,'t','b']);
            if(r<rows-1&&!grid[r+1][c].v)n.push([r+1,c,'b','t']);
            if(c>0&&!grid[r][c-1].v)n.push([r,c-1,'l','r']);
            if(c<cols-1&&!grid[r][c+1].v)n.push([r,c+1,'r','l']);
            return n;
        }
        var cr=0,cc=0;grid[0][0].v=true;stack.push([0,0]);
        while(stack.length>0){
            var nb=neighbors(cr,cc);
            if(nb.length>0){
                var pick=nb[Math.floor(Math.random()*nb.length)];
                grid[cr][cc].w[pick[2]]=false;
                grid[pick[0]][pick[1]].w[pick[3]]=false;
                grid[pick[0]][pick[1]].v=true;
                stack.push([cr,cc]);cr=pick[0];cc=pick[1];
            }else{var p=stack.pop();cr=p[0];cc=p[1];}
        }
        
        // 玩家
        var px=0,py=0,trail=[[0,0]];
        var goalR=rows-1,goalC=cols-1;
        var won=false;
        var accentColor='#667eea';
        
        // 触摸滑动
        var sx=0,sy=0;
        C.addEventListener('touchstart',function(e){e.preventDefault();var t=e.touches[0];sx=t.clientX;sy=t.clientY;},{passive:false});
        C.addEventListener('touchend',function(e){e.preventDefault();var t=e.changedTouches[0];var dx=t.clientX-sx,dy=t.clientY-sy;
            if(Math.abs(dx)<15&&Math.abs(dy)<15)return;
            if(Math.abs(dx)>Math.abs(dy)){move(0,dx>0?1:-1);}else{move(dy>0?1:-1,0);}
        },{passive:false});
        document.addEventListener('keydown',function(e){
            if(e.key==='ArrowUp')move(-1,0);if(e.key==='ArrowDown')move(1,0);
            if(e.key==='ArrowLeft')move(0,-1);if(e.key==='ArrowRight')move(0,1);
        });
        
        function move(dr,dc){
            if(won)return;
            var nr=py+dr,nc=px+dc;
            if(nr<0||nr>=rows||nc<0||nc>=cols)return;
            var wall=dr===-1?'t':dr===1?'b':dc===-1?'l':'r';
            if(grid[py][px].w[wall])return;
            py=nr;px=nc;trail.push([nc,nr]);
            if(py===goalR&&px===goalC)won=true;
            draw();
        }
        
        function draw(){
            ctx.fillStyle='rgba(0,0,0,0.9)';ctx.fillRect(0,0,W,H);
            // 迷宫墙壁
            ctx.strokeStyle='rgba(255,255,255,0.2)';ctx.lineWidth=1.5;
            for(var r=0;r<rows;r++)for(var c=0;c<cols;c++){
                var x=c*cw,y=r*ch,cell=grid[r][c];
                if(cell.w.t){ctx.beginPath();ctx.moveTo(x,y);ctx.lineTo(x+cw,y);ctx.stroke();}
                if(cell.w.r){ctx.beginPath();ctx.moveTo(x+cw,y);ctx.lineTo(x+cw,y+ch);ctx.stroke();}
                if(cell.w.b){ctx.beginPath();ctx.moveTo(x,y+ch);ctx.lineTo(x+cw,y+ch);ctx.stroke();}
                if(cell.w.l){ctx.beginPath();ctx.moveTo(x,y);ctx.lineTo(x,y+ch);ctx.stroke();}
            }
            // 尾迹
            ctx.strokeStyle=accentColor;ctx.lineWidth=3;ctx.lineCap='round';ctx.lineJoin='round';
            ctx.globalAlpha=0.4;ctx.beginPath();
            trail.forEach(function(p,i){var tx=p[0]*cw+cw/2,ty=p[1]*ch+ch/2;if(i===0)ctx.moveTo(tx,ty);else ctx.lineTo(tx,ty);});
            ctx.stroke();ctx.globalAlpha=1;
            // 终点
            ctx.fillStyle='#4ade80';ctx.beginPath();
            ctx.arc(goalC*cw+cw/2,goalR*ch+ch/2,cw*0.25,0,Math.PI*2);ctx.fill();
            // 玩家
            ctx.fillStyle=accentColor;ctx.beginPath();
            ctx.arc(px*cw+cw/2,py*ch+ch/2,cw*0.3,0,Math.PI*2);ctx.fill();
            ctx.fillStyle='rgba(255,255,255,0.4)';ctx.beginPath();
            ctx.arc(px*cw+cw/2-2,py*ch+ch/2-2,cw*0.12,0,Math.PI*2);ctx.fill();
            // 胜利
            if(won){
                ctx.fillStyle='rgba(0,0,0,0.6)';ctx.fillRect(0,0,W,H);
                ctx.fillStyle='#4ade80';ctx.font='bold 18px sans-serif';ctx.textAlign='center';
                ctx.fillText('${Strings.gameMazeComplete}',W/2,H/2-5);
                ctx.fillStyle='rgba(255,255,255,0.6)';ctx.font='13px sans-serif';
                ctx.fillText('${Strings.gameSteps}: '+trail.length,W/2,H/2+18);
                ctx.fillText('${Strings.gameTapToRestart}',W/2,H/2+40);ctx.textAlign='left';
                C.onclick=function(){location.reload();};
            }
        }
        draw();
    })();
    """.trimIndent()

    // ===================== 星空收集 STAR CATCH =====================

    private fun starCatchGame() = """
    (function(){
        var C=document.getElementById('gameCanvas'),ctx=C.getContext('2d');
        var W=C.width,H=C.height;
        var dpr=window.devicePixelRatio||1;
        C.width=W*dpr;C.height=H*dpr;
        C.style.width=W+'px';C.style.height=H+'px';
        ctx.scale(dpr,dpr);
        
        var cx=W/2,score=0,missed=0,maxMissed=5,running=true;
        var stars=[],particles=[];
        var starColors=['#ffd700','#ff6b6b','#48dbfb','#ff9ff3','#54a0ff'];
        var spawnRate=60,frame=0;
        
        // 触摸
        C.addEventListener('touchmove',function(e){e.preventDefault();var t=e.touches[0];var r=C.getBoundingClientRect();cx=t.clientX-r.left;},{passive:false});
        C.addEventListener('mousemove',function(e){var r=C.getBoundingClientRect();cx=e.clientX-r.left;});
        
        function spawnStar(){
            stars.push({x:Math.random()*W,y:-10,vy:1+Math.random()*1.5+score*0.02,size:6+Math.random()*6,color:starColors[Math.floor(Math.random()*starColors.length)],rot:Math.random()*Math.PI*2,rv:0.02+Math.random()*0.03});
        }
        function burst(x,y,color){
            for(var i=0;i<8;i++){
                var a=Math.PI*2*i/8;
                particles.push({x:x,y:y,vx:Math.cos(a)*2,vy:Math.sin(a)*2,life:1,color:color,size:2});
            }
        }
        function drawStar(x,y,r,rot,color){
            ctx.save();ctx.translate(x,y);ctx.rotate(rot);
            ctx.fillStyle=color;ctx.beginPath();
            for(var i=0;i<5;i++){
                var a=Math.PI*2*i/5-Math.PI/2;
                var ox=Math.cos(a)*r,oy=Math.sin(a)*r;
                var ia=a+Math.PI/5;
                var ix=Math.cos(ia)*r*0.4,iy=Math.sin(ia)*r*0.4;
                if(i===0)ctx.moveTo(ox,oy);else ctx.lineTo(ox,oy);
                ctx.lineTo(ix,iy);
            }
            ctx.closePath();ctx.fill();
            // 高光
            ctx.fillStyle='rgba(255,255,255,0.3)';ctx.beginPath();ctx.arc(-r*0.2,-r*0.2,r*0.25,0,Math.PI*2);ctx.fill();
            ctx.restore();
        }
        
        function loop(){
            ctx.fillStyle='rgba(0,0,10,0.85)';ctx.fillRect(0,0,W,H);
            // 背景星星
            ctx.fillStyle='rgba(255,255,255,0.3)';
            for(var i=0;i<20;i++){var sx=(i*137.5)%W,sy=((i*97.3+frame*0.1)%H);ctx.fillRect(sx,sy,1,1);}
            frame++;
            // 生成
            if(frame%spawnRate===0)spawnStar();
            if(score>10&&spawnRate>30)spawnRate=45;
            if(score>25)spawnRate=35;
            // 收集器
            var cw=50,ch=8;
            ctx.fillStyle='rgba(255,255,255,0.7)';
            ctx.beginPath();ctx.roundRect(cx-cw/2,H-25,cw,ch,4);ctx.fill();
            ctx.fillStyle='rgba(255,255,255,0.3)';
            ctx.beginPath();ctx.moveTo(cx-cw/2-5,H-25+ch);ctx.lineTo(cx-cw/2+5,H-25);
            ctx.lineTo(cx+cw/2-5,H-25);ctx.lineTo(cx+cw/2+5,H-25+ch);ctx.closePath();ctx.fill();
            // 星星
            for(var i=stars.length-1;i>=0;i--){
                var s=stars[i];
                s.y+=s.vy;s.rot+=s.rv;
                drawStar(s.x,s.y,s.size,s.rot,s.color);
                // 收集检测
                if(s.y>=H-30&&s.y<=H-15&&Math.abs(s.x-cx)<cw/2+s.size){
                    score++;burst(s.x,s.y,s.color);stars.splice(i,1);continue;
                }
                if(s.y>H+10){missed++;stars.splice(i,1);if(missed>=maxMissed)running=false;}
            }
            // 粒子
            for(var i=particles.length-1;i>=0;i--){
                var p=particles[i];p.x+=p.vx;p.y+=p.vy;p.life-=0.04;p.size*=0.97;
                if(p.life<=0){particles.splice(i,1);continue;}
                ctx.globalAlpha=p.life;ctx.fillStyle=p.color;ctx.beginPath();ctx.arc(p.x,p.y,p.size,0,Math.PI*2);ctx.fill();
            }
            ctx.globalAlpha=1;
            // HUD
            ctx.fillStyle='rgba(255,255,255,0.6)';ctx.font='12px sans-serif';
            ctx.fillText('${Strings.gameCollected}: '+score,8,18);
            var hearts='';for(var i=0;i<maxMissed-missed;i++)hearts+='♥';
            ctx.fillStyle='#ff6b6b';ctx.fillText(hearts,W-70,18);
            
            if(running)requestAnimationFrame(loop);else{
                ctx.fillStyle='rgba(0,0,0,0.7)';ctx.fillRect(0,0,W,H);
                ctx.fillStyle='#ffd700';ctx.font='bold 18px sans-serif';ctx.textAlign='center';
                ctx.fillText('${Strings.gameOver}',W/2,H/2-10);
                ctx.fillStyle='rgba(255,255,255,0.7)';ctx.font='13px sans-serif';
                ctx.fillText('${Strings.gameCollected} '+score+' stars',W/2,H/2+15);
                ctx.fillText('${Strings.gameTapToRestart}',W/2,H/2+38);ctx.textAlign='left';
                C.onclick=function(){location.reload();};
            }
        }
        loop();
    })();
    """.trimIndent()

    // ===================== 水墨禅境 INK ZEN =====================

    private fun inkZenGame() = """
    (function(){
        var C=document.getElementById('gameCanvas'),ctx=C.getContext('2d');
        var W=C.width,H=C.height;
        var dpr=window.devicePixelRatio||1;
        C.width=W*dpr;C.height=H*dpr;
        C.style.width=W+'px';C.style.height=H+'px';
        ctx.scale(dpr,dpr);
        
        // 宣纸背景
        ctx.fillStyle='#f5f0e8';ctx.fillRect(0,0,W,H);
        // 纸纹理
        for(var i=0;i<2000;i++){
            ctx.fillStyle='rgba(0,0,0,'+Math.random()*0.015+')';
            ctx.fillRect(Math.random()*W,Math.random()*H,Math.random()*3,1);
        }
        
        var painting=false,lastX,lastY;
        var inkDrops=[];
        var brushSize=4;
        
        // 墨滴扩散
        function addInkDrop(x,y,size){
            inkDrops.push({x:x,y:y,r:size*0.5,maxR:size*3+Math.random()*8,opacity:0.15+Math.random()*0.1,growing:true});
        }
        
        function drawBrush(x0,y0,x1,y1){
            var dx=x1-x0,dy=y1-y0,dist=Math.sqrt(dx*dx+dy*dy);
            var steps=Math.max(1,Math.floor(dist/2));
            for(var i=0;i<steps;i++){
                var t=i/steps;
                var cx=x0+dx*t,cy=y0+dy*t;
                var pressure=1-Math.min(dist/100,0.7);
                var sz=brushSize*pressure+Math.random()*1.5;
                // 主笔触
                ctx.fillStyle='rgba(20,15,10,'+(0.5+Math.random()*0.3)+')';
                ctx.beginPath();ctx.arc(cx+Math.random()*1.5-0.75,cy+Math.random()*1.5-0.75,sz,0,Math.PI*2);ctx.fill();
                // 飞白效果
                if(Math.random()<0.3){
                    ctx.fillStyle='rgba(20,15,10,0.08)';
                    ctx.beginPath();ctx.arc(cx+Math.random()*6-3,cy+Math.random()*6-3,sz*0.5,0,Math.PI*2);ctx.fill();
                }
            }
            // 偶尔墨滴
            if(Math.random()<0.05)addInkDrop(x1,y1,brushSize);
        }
        
        function updateDrops(){
            inkDrops.forEach(function(d){
                if(!d.growing)return;
                d.r+=0.15;
                if(d.r>=d.maxR){d.growing=false;return;}
                ctx.fillStyle='rgba(20,15,10,'+d.opacity*(1-d.r/d.maxR)+')';
                ctx.beginPath();ctx.arc(d.x,d.y,d.r,0,Math.PI*2);ctx.fill();
            });
        }
        
        function getPos(e){
            var r=C.getBoundingClientRect();
            var t=e.touches?e.touches[0]:e;
            return {x:t.clientX-r.left,y:t.clientY-r.top};
        }
        
        C.addEventListener('touchstart',function(e){e.preventDefault();painting=true;var p=getPos(e);lastX=p.x;lastY=p.y;addInkDrop(p.x,p.y,brushSize*1.5);},{passive:false});
        C.addEventListener('touchmove',function(e){e.preventDefault();if(!painting)return;var p=getPos(e);drawBrush(lastX,lastY,p.x,p.y);lastX=p.x;lastY=p.y;},{passive:false});
        C.addEventListener('touchend',function(){painting=false;});
        C.addEventListener('mousedown',function(e){painting=true;var p=getPos(e);lastX=p.x;lastY=p.y;addInkDrop(p.x,p.y,brushSize*1.5);});
        C.addEventListener('mousemove',function(e){if(!painting)return;var p=getPos(e);drawBrush(lastX,lastY,p.x,p.y);lastX=p.x;lastY=p.y;});
        C.addEventListener('mouseup',function(){painting=false;});
        
        // 提示文字
        ctx.fillStyle='rgba(0,0,0,0.12)';ctx.font='14px serif';ctx.textAlign='center';
        ctx.fillText('${Strings.gameTouchToPaint}',W/2,H/2);
        ctx.fillText('${Strings.gameZen}',W/2,H/2+25);ctx.textAlign='left';
        
        // 墨滴动画
        function animate(){updateDrops();requestAnimationFrame(animate);}
        animate();
    })();
    """.trimIndent()
}
