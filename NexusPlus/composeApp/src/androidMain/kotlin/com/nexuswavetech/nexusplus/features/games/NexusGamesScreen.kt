package com.nexuswavetech.nexusplus.features.games

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nexuswavetech.nexusplus.ads.NexusAdScaffold
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

// ── Data model ────────────────────────────────────────────────────────────────

data class GameItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val category: String,
    val htmlContent: String,
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun NexusGamesScreen(onBack: () -> Unit) {
    var activeGame by remember { mutableStateOf<GameItem?>(null) }

    AnimatedVisibility(visible = activeGame == null, enter = fadeIn(), exit = fadeOut()) {
        GameHubScreen(games = NEXUS_GAMES, onBack = onBack, onLaunch = { activeGame = it })
    }
    AnimatedVisibility(visible = activeGame != null, enter = fadeIn(), exit = fadeOut()) {
        activeGame?.let { game ->
            GameWebViewScreen(game = game, onBack = { activeGame = null })
        }
    }
}

// ── Game hub grid ─────────────────────────────────────────────────────────────

@Composable
private fun GameHubScreen(
    games: List<GameItem>,
    onBack: () -> Unit,
    onLaunch: (GameItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Nexus Games", onBack = onBack)

        Text(
            text = "${games.size} offline games · No internet needed",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(games, key = { it.id }) { game ->
                GameCard(game = game, onClick = { onLaunch(game) })
            }
        }
    }
}

@Composable
private fun GameCard(game: GameItem, onClick: () -> Unit) {
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Launch ${game.name}" },
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    game.icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(game.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                game.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                game.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

// ── WebView game player ───────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun GameWebViewScreen(game: GameItem, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(game.name) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to games")
                }
            },
        )

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled  = true
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL(null, game.htmlContent, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .semantics { contentDescription = "${game.name} game running" },
        )
    }
}

// ── Built-in HTML5 games ──────────────────────────────────────────────────────

private val NEXUS_GAMES = listOf(

    // 1 ── Snake ──────────────────────────────────────────────────────────────
    GameItem(
        id = "snake", name = "Snake", description = "Classic snake — eat, grow, survive!",
        icon = Icons.Filled.LinearScale, category = "Arcade",
        htmlContent = """<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
body{margin:0;background:#121212;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;font-family:sans-serif;color:#fff;user-select:none;}
canvas{border:2px solid #4CAF50;border-radius:8px;}
#score{font-size:1.2em;margin-bottom:8px;}
#msg{position:absolute;font-size:1.4em;text-align:center;padding:16px;background:#1e1e1e;border-radius:12px;display:none;}
.btns{display:flex;gap:8px;margin-top:10px;}
button{padding:10px 20px;font-size:1em;border:none;border-radius:8px;background:#4CAF50;color:#fff;cursor:pointer;}
</style></head><body>
<div id="score">Score: 0</div>
<canvas id="c" width="300" height="300"></canvas>
<div class="btns"><button onclick="startGame()">▶ Start / Restart</button></div>
<div id="msg">Tap Start to play!<br>Swipe or use arrow keys.</div>
<script>
const C=document.getElementById('c'),X=C.getContext('2d'),SZ=15,COLS=20,ROWS=20;
let snake,dir,food,score,loop,alive;
function startGame(){
  snake=[{x:10,y:10}];dir={x:1,y:0};score=0;alive=true;
  document.getElementById('msg').style.display='none';
  placeFood();if(loop)clearInterval(loop);loop=setInterval(tick,120);
}
function placeFood(){food={x:Math.floor(Math.random()*COLS),y:Math.floor(Math.random()*ROWS)};}
function tick(){
  if(!alive)return;
  const head={x:(snake[0].x+dir.x+COLS)%COLS,y:(snake[0].y+dir.y+ROWS)%ROWS};
  if(snake.some(s=>s.x===head.x&&s.y===head.y)){die();return;}
  snake.unshift(head);
  if(head.x===food.x&&head.y===food.y){score++;document.getElementById('score').textContent='Score: '+score;placeFood();}
  else snake.pop();
  draw();
}
function draw(){
  X.fillStyle='#121212';X.fillRect(0,0,300,300);
  X.fillStyle='#4CAF50';
  snake.forEach((s,i)=>{X.fillStyle=i===0?'#8BC34A':'#4CAF50';X.fillRect(s.x*SZ,s.y*SZ,SZ-1,SZ-1);});
  X.fillStyle='#FF5722';X.fillRect(food.x*SZ,food.y*SZ,SZ-1,SZ-1);
}
function die(){alive=false;clearInterval(loop);const m=document.getElementById('msg');m.textContent='Game Over! Score: '+score+'\nTap Start to try again.';m.style.display='block';}
document.addEventListener('keydown',e=>{const k={ArrowUp:{x:0,y:-1},ArrowDown:{x:0,y:1},ArrowLeft:{x:-1,y:0},ArrowRight:{x:1,y:0}};const d=k[e.key];if(d&&!(d.x===-dir.x&&d.y===-dir.y))dir=d;});
let t0={x:0,y:0};
C.addEventListener('touchstart',e=>{t0={x:e.touches[0].clientX,y:e.touches[0].clientY};},{passive:true});
C.addEventListener('touchend',e=>{const dx=e.changedTouches[0].clientX-t0.x,dy=e.changedTouches[0].clientY-t0.y;
  if(Math.abs(dx)>Math.abs(dy)){dir=dx>0?{x:1,y:0}:{x:-1,y:0};}else{dir=dy>0?{x:0,y:1}:{x:0,y:-1};}},{passive:true});
const m=document.getElementById('msg');m.style.display='block';draw();
</script></body></html>"""
    ),

    // 2 ── 2048 ───────────────────────────────────────────────────────────────
    GameItem(
        id = "2048", name = "2048", description = "Slide tiles and reach 2048!",
        icon = Icons.Filled.Grid4x4, category = "Puzzle",
        htmlContent = """<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
body{margin:0;background:#faf8ef;display:flex;flex-direction:column;align-items:center;justify-content:flex-start;min-height:100vh;font-family:sans-serif;padding-top:16px;}
h2{color:#776e65;margin:0 0 8px;}
#score-box{font-size:1.1em;color:#fff;background:#bbada0;padding:6px 18px;border-radius:8px;margin-bottom:10px;}
#grid{display:grid;grid-template-columns:repeat(4,70px);gap:8px;background:#bbada0;padding:8px;border-radius:12px;}
.cell{width:70px;height:70px;display:flex;align-items:center;justify-content:center;font-size:1.4em;font-weight:bold;border-radius:8px;background:#cdc1b4;color:#776e65;transition:all .1s;}
button{margin-top:12px;padding:10px 24px;font-size:1em;border:none;border-radius:8px;background:#8f7a66;color:#fff;cursor:pointer;}
</style></head><body>
<h2>2048</h2><div id="score-box">Score: <span id="sc">0</span></div>
<div id="grid"></div><button onclick="init()">↺ New Game</button>
<script>
const COLORS={2:'#eee4da',4:'#ede0c8',8:'#f2b179',16:'#f59563',32:'#f67c5f',64:'#f65e3b',128:'#edcf72',256:'#edcc61',512:'#edc850',1024:'#edc53f',2048:'#edc22e'};
const DARK={8:true,16:true,32:true,64:true,128:true,256:true,512:true,1024:true,2048:true};
let board,score;
function init(){board=Array.from({length:4},()=>Array(4).fill(0));score=0;addTile();addTile();render();}
function addTile(){const empty=[];board.forEach((r,i)=>r.forEach((v,j)=>{if(!v)empty.push([i,j]);}));if(!empty.length)return;const[r,c]=empty[Math.floor(Math.random()*empty.length)];board[r][c]=Math.random()<0.9?2:4;}
function render(){const g=document.getElementById('grid');g.innerHTML='';board.flat().forEach(v=>{const d=document.createElement('div');d.className='cell';d.textContent=v||'';d.style.background=COLORS[v]||'#cdc1b4';d.style.color=DARK[v]?'#f9f6f2':'#776e65';d.style.fontSize=v>=1024?'1em':'1.4em';g.appendChild(d);});document.getElementById('sc').textContent=score;}
function slide(row){let r=row.filter(v=>v);for(let i=0;i<r.length-1;i++){if(r[i]===r[i+1]){r[i]*=2;score+=r[i];r.splice(i+1,1);}}while(r.length<4)r.push(0);return r;}
function move(dir){let moved=false;if(dir==='l'||dir==='r'){board=board.map(row=>{const n=dir==='l'?slide(row):slide([...row].reverse()).reverse();if(n.join()!==row.join())moved=true;return n;});}else{for(let c=0;c<4;c++){let col=board.map(r=>r[c]);const n=dir==='u'?slide(col):slide([...col].reverse()).reverse();n.forEach((v,r)=>{if(board[r][c]!==v)moved=true;board[r][c]=v;});}}if(moved){addTile();render();}}
document.addEventListener('keydown',e=>{const m={ArrowLeft:'l',ArrowRight:'r',ArrowUp:'u',ArrowDown:'d'};if(m[e.key])move(m[e.key]);});
let t0={x:0,y:0};
document.addEventListener('touchstart',e=>{t0={x:e.touches[0].clientX,y:e.touches[0].clientY};},{passive:true});
document.addEventListener('touchend',e=>{const dx=e.changedTouches[0].clientX-t0.x,dy=e.changedTouches[0].clientY-t0.y;
if(Math.abs(dx)>Math.abs(dy)){move(dx>0?'r':'l');}else{move(dy>0?'d':'u');}},{passive:true});
init();
</script></body></html>"""
    ),

    // 3 ── Tic-Tac-Toe ────────────────────────────────────────────────────────
    GameItem(
        id = "ttt", name = "Tic-Tac-Toe", description = "Play vs AI — can you win?",
        icon = Icons.Filled.Close, category = "Strategy",
        htmlContent = """<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
body{margin:0;background:#121212;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;font-family:sans-serif;color:#fff;}
h2{margin:0 0 8px;}
#status{margin-bottom:12px;font-size:1.1em;color:#aaa;}
#board{display:grid;grid-template-columns:repeat(3,90px);gap:6px;}
.cell{width:90px;height:90px;background:#1e1e1e;border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:2.4em;cursor:pointer;border:2px solid #333;transition:background .15s;}
.cell:hover{background:#2a2a2a;}
.cell.x{color:#4CAF50;}.cell.o{color:#FF5722;}
button{margin-top:16px;padding:10px 24px;font-size:1em;border:none;border-radius:8px;background:#4CAF50;color:#fff;cursor:pointer;}
</style></head><body>
<h2>Tic-Tac-Toe</h2><div id="status">Your turn (✕)</div>
<div id="board"></div><button onclick="reset()">↺ New Game</button>
<script>
let board,human,ai,over;
const WIN=[[0,1,2],[3,4,5],[6,7,8],[0,3,6],[1,4,7],[2,5,8],[0,4,8],[2,4,6]];
function reset(){board=Array(9).fill('');human='X';ai='O';over=false;render();document.getElementById('status').textContent='Your turn (✕)';}
function render(){const g=document.getElementById('board');g.innerHTML='';board.forEach((v,i)=>{const c=document.createElement('div');c.className='cell'+(v?' '+v.toLowerCase():'');c.textContent=v;c.onclick=()=>play(i);g.appendChild(c);});}
function checkWin(b,p){return WIN.some(([a,x,y])=>b[a]===p&&b[x]===p&&b[y]===p);}
function isDraw(b){return b.every(v=>v);}
function play(i){if(board[i]||over)return;board[i]=human;render();if(checkWin(board,human)){end('You win! 🎉');return;}if(isDraw(board)){end("It's a draw!");return;}document.getElementById('status').textContent='AI thinking…';setTimeout(()=>{const m=bestMove();board[m]=ai;render();if(checkWin(board,ai)){end('AI wins! Try again.');}else if(isDraw(board)){end("It's a draw!");}else{document.getElementById('status').textContent='Your turn (✕)';}},300);}
function bestMove(){let best=-Infinity,move=0;board.forEach((_,i)=>{if(!board[i]){board[i]=ai;const s=minimax(board,0,false);board[i]='';if(s>best){best=s;move=i;}}});return move;}
function minimax(b,d,isMax){if(checkWin(b,ai))return 10-d;if(checkWin(b,human))return d-10;if(isDraw(b))return 0;let best=isMax?-Infinity:Infinity;b.forEach((_,i)=>{if(!b[i]){b[i]=isMax?ai:human;const s=minimax(b,d+1,!isMax);b[i]='';best=isMax?Math.max(best,s):Math.min(best,s);}});return best;}
function end(msg){over=true;document.getElementById('status').textContent=msg;}
reset();
</script></body></html>"""
    ),

    // 4 ── Memory Match ────────────────────────────────────────────────────────
    GameItem(
        id = "memory", name = "Memory Match", description = "Find all matching pairs!",
        icon = Icons.Filled.Style, category = "Memory",
        htmlContent = """<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
body{margin:0;background:#121212;display:flex;flex-direction:column;align-items:center;justify-content:flex-start;padding:16px;min-height:100vh;font-family:sans-serif;color:#fff;box-sizing:border-box;}
h2{margin:0 0 6px;}
#info{margin-bottom:10px;color:#aaa;font-size:.95em;}
#grid{display:grid;grid-template-columns:repeat(4,70px);gap:8px;}
.card{width:70px;height:70px;border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:2em;cursor:pointer;transition:all .2s;background:#1e1e1e;border:2px solid #333;}
.card.flipped{background:#4CAF50;border-color:#4CAF50;}
.card.matched{background:#2E7D32;border-color:#2E7D32;pointer-events:none;}
button{margin-top:12px;padding:10px 24px;font-size:1em;border:none;border-radius:8px;background:#4CAF50;color:#fff;cursor:pointer;}
</style></head><body>
<h2>Memory Match</h2><div id="info">Moves: 0 · Pairs: 0/8</div>
<div id="grid"></div><button onclick="init()">↺ New Game</button>
<script>
const EMOJIS=['🍎','🍌','🍇','🍉','🎮','🎯','⭐','🌙'];
let cards,flipped,matched,moves,lock;
function init(){const deck=[...EMOJIS,...EMOJIS].sort(()=>Math.random()-.5);flipped=[];matched=0;moves=0;lock=false;cards=deck.map((e,i)=>({emoji:e,id:i,isFlipped:false,isMatched:false}));render();}
function render(){const g=document.getElementById('grid');g.innerHTML='';cards.forEach((c,i)=>{const d=document.createElement('div');d.className='card'+(c.isFlipped||c.isMatched?' flipped':'')+(c.isMatched?' matched':'');d.textContent=c.isFlipped||c.isMatched?c.emoji:'';d.onclick=()=>flip(i);g.appendChild(d);});document.getElementById('info').textContent=`Moves: ${'$'}{moves} · Pairs: ${'$'}{matched}/8`;}
function flip(i){if(lock||cards[i].isFlipped||cards[i].isMatched||flipped.length>=2)return;cards[i].isFlipped=true;flipped.push(i);render();if(flipped.length===2){moves++;check();}}
function check(){lock=true;const[a,b]=flipped;if(cards[a].emoji===cards[b].emoji){cards[a].isMatched=cards[b].isMatched=true;matched++;flipped=[];lock=false;render();if(matched===8)document.getElementById('info').textContent=`Completed in ${'$'}{moves} moves! 🎉`;}else{setTimeout(()=>{cards[a].isFlipped=cards[b].isFlipped=false;flipped=[];lock=false;render();},900);}}
init();
</script></body></html>"""
    ),

    // 5 ── Breakout ────────────────────────────────────────────────────────────
    GameItem(
        id = "breakout", name = "Breakout", description = "Smash all the bricks!",
        icon = Icons.Filled.SportsTennis, category = "Arcade",
        htmlContent = """<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
body{margin:0;background:#121212;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;font-family:sans-serif;color:#fff;user-select:none;}
canvas{border-radius:8px;border:2px solid #4CAF50;touch-action:none;}
#ui{margin-bottom:8px;font-size:1em;display:flex;gap:16px;}
button{margin-top:8px;padding:8px 20px;font-size:1em;border:none;border-radius:8px;background:#4CAF50;color:#fff;cursor:pointer;}
</style></head><body>
<div id="ui"><span>Score: <b id="sc">0</b></span><span>Lives: <b id="lv">3</b></span></div>
<canvas id="c" width="300" height="380"></canvas><br>
<button onclick="start()">▶ Start / Restart</button>
<script>
const C=document.getElementById('c'),X=C.getContext('2d');
const W=300,H=380,PR=50,PH=10,BR=6,BCOLS=8,BROWS=5;
let px,bx,by,bdx,bdy,bricks,score,lives,raf,running;
function start(){px=W/2-PR;bx=W/2;by=H-60;bdx=2.5;bdy=-3;lives=3;score=0;running=true;
bricks=Array.from({length:BROWS},(_,r)=>Array.from({length:BCOLS},(_,c)=>({x:c*(W/BCOLS)+2,y:r*22+40,w:W/BCOLS-4,h:16,alive:true,color:`hsl(${'$'}{r*40+180},70%,55%)`})));
cancelAnimationFrame(raf);loop();}
function loop(){if(!running)return;raf=requestAnimationFrame(loop);X.fillStyle='#121212';X.fillRect(0,0,W,H);
bricks.flat().forEach(b=>{if(!b.alive)return;X.fillStyle=b.color;X.fillRect(b.x,b.y,b.w,b.h);});
X.fillStyle='#4CAF50';X.fillRect(px,H-30,PR*2,PH);
X.beginPath();X.arc(bx,by,BR,0,Math.PI*2);X.fillStyle='#FF5722';X.fill();
bx+=bdx;by+=bdy;
if(bx-BR<0||bx+BR>W)bdx=-bdx;
if(by-BR<0)bdy=-bdy;
if(by+BR>H-30&&bx>px&&bx<px+PR*2)bdy=-Math.abs(bdy);
if(by+BR>H){lives--;document.getElementById('lv').textContent=lives;if(!lives){running=false;X.fillStyle='rgba(0,0,0,.7)';X.fillRect(0,0,W,H);X.fillStyle='#fff';X.font='bold 24px sans-serif';X.textAlign='center';X.fillText('Game Over! Score: '+score,W/2,H/2);return;}bx=W/2;by=H-60;bdx=2.5+score/200;bdy=-3;}
bricks.flat().forEach(b=>{if(!b.alive)return;if(bx>b.x&&bx<b.x+b.w&&by>b.y&&by<b.y+b.h){b.alive=false;bdy=-bdy;score+=10;document.getElementById('sc').textContent=score;}});
if(bricks.flat().every(b=>!b.alive)){running=false;X.fillStyle='rgba(0,0,0,.7)';X.fillRect(0,0,W,H);X.fillStyle='#8BC34A';X.font='bold 22px sans-serif';X.textAlign='center';X.fillText('You Win! Score: '+score,W/2,H/2);}}
C.addEventListener('mousemove',e=>{const r=C.getBoundingClientRect();px=e.clientX-r.left-PR;px=Math.max(0,Math.min(W-PR*2,px));});
C.addEventListener('touchmove',e=>{e.preventDefault();const r=C.getBoundingClientRect();px=e.touches[0].clientX-r.left-PR;px=Math.max(0,Math.min(W-PR*2,px));},{passive:false});
X.fillStyle='#fff';X.font='18px sans-serif';X.textAlign='center';X.fillText('Tap Start to play',W/2,H/2);
</script></body></html>"""
    ),

    // 6 ── Minesweeper ─────────────────────────────────────────────────────────
    GameItem(
        id = "mines", name = "Minesweeper", description = "Reveal cells, avoid the mines!",
        icon = Icons.Filled.Warning, category = "Strategy",
        htmlContent = """<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
body{margin:0;background:#121212;display:flex;flex-direction:column;align-items:center;justify-content:flex-start;padding:12px;min-height:100vh;font-family:monospace;color:#fff;box-sizing:border-box;}
h2{margin:0 0 4px;}
#info{margin-bottom:8px;color:#aaa;font-size:.9em;}
#grid{display:inline-grid;grid-template-columns:repeat(9,32px);gap:3px;}
.cell{width:32px;height:32px;background:#1e1e1e;border:1px solid #333;border-radius:4px;display:flex;align-items:center;justify-content:center;font-size:.95em;cursor:pointer;font-weight:bold;}
.cell.revealed{background:#2a2a2a;cursor:default;border-color:#222;}
.cell.flagged{background:#1e1e1e;}
.cell.mine{background:#b71c1c;}
.n1{color:#4CAF50}.n2{color:#29B6F6}.n3{color:#FF5722}.n4{color:#9C27B0}.n5{color:#F44336}.n6{color:#00BCD4}.n7{color:#FF9800}.n8{color:#E91E63}
button{margin-top:10px;padding:8px 20px;font-size:.95em;border:none;border-radius:8px;background:#4CAF50;color:#fff;cursor:pointer;}
</style></head><body>
<h2>Minesweeper</h2><div id="info">Left-click: reveal · Right-click: flag</div>
<div id="grid"></div><button onclick="init()">↺ New Game</button>
<script>
const ROWS=9,COLS=9,MINES=10;
let cells,revealed,flagged,over;
function init(){cells=Array.from({length:ROWS*COLS},()=>({mine:false,adj:0}));let m=0;while(m<MINES){const i=Math.floor(Math.random()*ROWS*COLS);if(!cells[i].mine){cells[i].mine=true;m++;}}
cells.forEach((_,i)=>{if(cells[i].mine)return;let a=0;adj(i).forEach(j=>{if(cells[j]?.mine)a++;});cells[i].adj=a;});
revealed=new Set();flagged=new Set();over=false;render();}
function adj(i){const r=Math.floor(i/COLS),c=i%COLS,res=[];for(let dr=-1;dr<=1;dr++)for(let dc=-1;dc<=1;dc++){if(!dr&&!dc)continue;const nr=r+dr,nc=c+dc;if(nr>=0&&nr<ROWS&&nc>=0&&nc<COLS)res.push(nr*COLS+nc);}return res;}
function reveal(i){if(over||revealed.has(i)||flagged.has(i))return;revealed.add(i);if(cells[i].mine){over=true;cells.forEach((_,j)=>{if(cells[j].mine)revealed.add(j);});render();document.getElementById('info').textContent='💥 Boom! Game Over.';return;}if(!cells[i].adj)adj(i).forEach(j=>reveal(j));
if(revealed.size===ROWS*COLS-MINES){over=true;document.getElementById('info').textContent='🎉 You Win!';}render();}
function flag(i){if(over||revealed.has(i))return;if(flagged.has(i))flagged.delete(i);else flagged.add(i);render();}
function render(){const g=document.getElementById('grid');g.innerHTML='';cells.forEach((_,i)=>{const d=document.createElement('div');d.className='cell';if(revealed.has(i)){d.classList.add('revealed');if(cells[i].mine){d.classList.add('mine');d.textContent='💣';}else if(cells[i].adj){d.textContent=cells[i].adj;d.classList.add('n'+cells[i].adj);}
}else if(flagged.has(i)){d.textContent='🚩';}d.onclick=()=>reveal(i);d.oncontextmenu=e=>{e.preventDefault();flag(i);};g.appendChild(d);});}
init();
</script></body></html>"""
    ),
)
