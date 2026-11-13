// 印记 · 交互原型数据（纯前端模拟，非真机数据）
const TODAY = '2026-11-13';
const state = {
  tab: 'today',
  habits: [
    { id: 1, name: '早读 20 分钟', color: '#2E9E8F', cycle: '每天', done: true, streak: 12, doneDays: [1,2,3,5,6,7,8,9,10,11,12,13] },
    { id: 2, name: '喝水 8 杯', color: '#4C7EF3', cycle: '每天', done: false, streak: 5, doneDays: [8,9,10,11,12] },
    { id: 3, name: '跑步', color: '#E88B4A', cycle: '周一/三/五', done: false, streak: 3, doneDays: [2,4,6,9,11] },
  ],
  txns: [
    { id: 1, type: 'expense', amount: 32, cat: '餐饮', icon: '🍜', note: '午饭', date: '2026-11-13' },
    { id: 2, type: 'expense', amount: 6, cat: '交通', icon: '🚌', note: '地铁', date: '2026-11-13' },
    { id: 3, type: 'income', amount: 12000, cat: '工资', icon: '💰', note: '11 月', date: '2026-11-10' },
    { id: 4, type: 'expense', amount: 268, cat: '购物', icon: '🛍️', note: '日用品', date: '2026-11-09' },
  ],
  cats: [
    { n:'餐饮', i:'🍜', c:'#E88B4A' }, { n:'交通', i:'🚌', c:'#4C7EF3' },
    { n:'购物', i:'🛍️', c:'#B06AD9' }, { n:'居住', i:'🏠', c:'#2E9E8F' },
    { n:'日常', i:'🧺', c:'#7A8CA0' }, { n:'娱乐', i:'🎮', c:'#E86A8A' },
    { n:'医疗', i:'💊', c:'#4CA9C9' }, { n:'人情', i:'🎁', c:'#C9A24C' },
    { n:'学习', i:'📚', c:'#6C8CE0' }, { n:'其他', i:'📦', c:'#9AA3AF' },
  ],
  notes: [
    { id:1, text:'周末把阳台那盆绿萝换一下土 #生活 #待办', pinned:true, tag:'生活', time:'昨天 21:40' },
    { id:2, text:'《置身事内》第 3 章：地方政府的事权与财权，值得回头重读 #读书', pinned:false, tag:'读书', time:'11-11' },
    { id:3, text:'体检报告说尿酸偏高，少喝浓汤、多喝水 #健康', pinned:false, tag:'健康', time:'11-08' },
  ],
  todos: [
    { id:1, title:'交季度报表', due:'今天 18:00', done:false, overdue:true },
    { id:2, title:'给妈妈回电话', due:'今天', done:false, overdue:false },
    { id:3, title:'取快递', due:'明天', done:false, overdue:false },
    { id:4, title:'预约牙医', due:'11-10', done:false, overdue:true },
    { id:5, title:'整理书桌', due:'昨天', done:true, overdue:false },
  ],
  widget: { habits:[1,2,3], todos:[1,2,3,4] },
  addSheet: null,
  toast: '',
  detail: null,
  ledgerSeg: '支出',
  todoSeg: '今天',
  noteQuery: '',
};

const $ = (s) => document.querySelector(s);
const fmt = (n) => '¥' + Number(n).toLocaleString('zh-CN');
const esc = (s) => String(s).replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));

function toast(msg){ state.toast = msg; render(); clearTimeout(window._t); window._t = setTimeout(()=>{ state.toast=''; render(); }, 1800); }

function monthDone(){ return state.habits.filter(h=>h.done).length; }

// ---------- 桌面小卡片 ----------
function widgetHTML(){
  const hs = state.widget.habits.map(id=>state.habits.find(h=>h.id===id)).filter(h=>h&&!h.done);
  const ts = state.widget.todos.map(id=>state.todos.find(t=>t.id===id)).filter(t=>t&&!t.done);
  const showH = hs.slice(0,3), showT = ts.slice(0,2);
  const more = Math.max(0, hs.length-3) + Math.max(0, ts.length-2);
  return `<div class="widget">
    <div class="wg-head" data-nav="today"><span class="wg-title">印记 · 今天</span><span class="wg-date">11月13日 周五</span></div>
    <div class="wg-sec">今日习惯</div>
    ${showH.length? showH.map(h=>`<div class="wg-row" data-wtoggle-h="${h.id}"><span class="wg-check" style="border-color:${h.color}"></span><span class="wg-txt">${esc(h.name)}</span></div>`).join('') : '<div class="wg-empty">习惯都完成啦 ✨</div>'}
    <div class="wg-sec wg-sec2">待办</div>
    ${showT.length? showT.map(t=>`<div class="wg-row" data-wtoggle-t="${t.id}"><span class="wg-check"></span><span class="wg-txt">${esc(t.title)}</span></div>`).join('') : '<div class="wg-empty">待办清空啦 ✨</div>'}
    ${more>0?`<div class="wg-more">还有 ${more} 项 · 点空白处打开 App</div>`:'<div class="wg-more">点空白处打开 App</div>'}
  </div>`;
}

// ---------- 今日 / 习惯 ----------
function todayHTML(){
  const done = monthDone(), total = state.habits.length;
  const pct = total? Math.round(done/total*100):0;
  return `
  <div class="page-head"><div><div class="hello">早上好，秋殇</div><div class="sub">11 月 13 日 · 周五</div></div><button class="icon-btn" data-act="settings">⚙</button></div>
  <div class="card progress-card">
    <div class="pc-top"><div><div class="pc-label">今日进度</div><div class="pc-num">${done}<span>/${total}</span></div></div><div class="pc-ring" style="--p:${pct}"><span>${pct}%</span></div></div>
    <div class="pc-bar"><i style="width:${pct}%"></i></div>
    <div class="pc-hint">${done===total?'今天的习惯全部完成，很棒 ✨':'点一下圆圈即可打卡，再点一下可取消'}</div>
  </div>
  <div class="sec-title">今日习惯 <button class="link" data-act="add-habit">＋ 新建</button></div>
  ${state.habits.map(h=>`
    <div class="card habit-row" data-detail="${h.id}">
      <span class="habit-check ${h.done?'on':''}" style="--c:${h.color}" data-toggle-h="${h.id}">${h.done?'✓':''}</span>
      <div class="habit-main"><div class="habit-name ${h.done?'dim':''}">${esc(h.name)}</div><div class="habit-meta">${h.cycle} · 连续 <b>${h.streak}</b> 天</div></div>
      <span class="chev">›</span>
    </div>`).join('')}
  <div class="tip">💡 点习惯名可看月历热力图、给忘记的日子补卡</div>`;
}

// ---------- 记账 ----------
function ledgerHTML(){
  const exp = state.txns.filter(t=>t.type==='expense').reduce((s,t)=>s+t.amount,0);
  const inc = state.txns.filter(t=>t.type==='income').reduce((s,t)=>s+t.amount,0);
  const byCat = {};
  state.txns.filter(t=>t.type==='expense').forEach(t=>byCat[t.cat]=(byCat[t.cat]||0)+t.amount);
  const pie = Object.entries(byCat).sort((a,b)=>b[1]-a[1]);
  const colors = ['#E88B4A','#4C7EF3','#B06AD9','#2E9E8F','#E86A8A','#7A8CA0'];
  let acc = 0;
  const grad = pie.map((e,i)=>{ const a=acc/exp*360; acc+=e[1]; const b=acc/exp*360; return `${colors[i%colors.length]} ${a}deg ${b}deg`; }).join(',');
  return `
  <div class="page-head"><div><div class="hello">记账</div><div class="sub">2026 年 11 月</div></div><button class="icon-btn" data-act="settings">⚙</button></div>
  <div class="card sum-card">
    <div class="sum-item"><div class="sum-l">本月支出</div><div class="sum-v red">${fmt(exp)}</div></div>
    <div class="sum-div"></div>
    <div class="sum-item"><div class="sum-l">本月收入</div><div class="sum-v green">${fmt(inc)}</div></div>
    <div class="sum-div"></div>
    <div class="sum-item"><div class="sum-l">结余</div><div class="sum-v">${fmt(inc-exp)}</div></div>
  </div>
  <div class="sec-title">分类占比 <span class="sec-note">点色块看金额</span></div>
  <div class="card pie-card">
    <div class="pie" style="background:conic-gradient(${grad})"><div class="pie-hole"><b>${pie.length}</b><span>类</span></div></div>
    <div class="legend">${pie.length? pie.map((e,i)=>`<div class="lg-row" data-pie="${esc(e[0])}:${e[1]}:${exp}"><i style="background:${colors[i%colors.length]}"></i><span>${esc(e[0])}</span><b>${fmt(e[1])}</b></div>`).join('') : '<div class="muted">暂无支出</div>'}</div>
  </div>
  <div class="sec-title">最近记录 <button class="link" data-act="add-txn">＋ 记一笔</button></div>
  ${state.txns.slice().reverse().map(t=>`<div class="card txn-row"><span class="txn-ic">${t.icon}</span><div class="txn-main"><div class="txn-name">${esc(t.note||t.cat)}</div><div class="txn-meta">${t.cat} · ${t.date}</div></div><b class="${t.type==='expense'?'red':'green'}">${t.type==='expense'?'−':'+'}${fmt(t.amount)}</b></div>`).join('')}
  <button class="fab" data-act="add-txn">＋ 记一笔</button>`;
}

// ---------- 备忘 ----------
function notesHTML(){
  const q = state.noteQuery.trim().toLowerCase();
  const list = state.notes.filter(n=>!q || n.text.toLowerCase().includes(q));
  return `
  <div class="page-head"><div><div class="hello">备忘</div><div class="sub">想到就记，一行就够</div></div><button class="icon-btn" data-act="settings">⚙</button></div>
  <div class="card quick-note"><input id="noteInput" placeholder="记点什么… 用 #标签 归类" /><button data-act="save-note">保存</button></div>
  <div class="search"><span>🔍</span><input id="noteSearch" placeholder="搜索内容或 #标签" value="${esc(state.noteQuery)}" /></div>
  ${list.length? list.map(n=>`<div class="card note-card ${n.pinned?'pinned':''}" data-note="${n.id}">
    ${n.pinned?'<div class="pin">📌 置顶</div>':''}
    <div class="note-text">${esc(n.text).replace(/#([^\s#]+)/g,'<span class="tag">#$1</span>')}</div>
    <div class="note-foot"><span>${n.time}</span><button class="mini" data-pin="${n.id}">${n.pinned?'取消置顶':'置顶'}</button></div>
  </div>`).join('') : '<div class="empty">没有匹配的备忘</div>'}
  <button class="fab" data-act="focus-note">＋ 写一条</button>`;
}

// ---------- 待办 ----------
function todosHTML(){
  const seg = state.todoSeg;
  let list = state.todos.filter(t=>!t.done);
  if(seg==='今天') list = list.filter(t=>t.due.includes('今天')||t.overdue);
  if(seg==='已完成') list = state.todos.filter(t=>t.done);
  return `
  <div class="page-head"><div><div class="hello">待办</div><div class="sub">${state.todos.filter(t=>!t.done&&t.overdue).length} 项已逾期</div></div><button class="icon-btn" data-act="settings">⚙</button></div>
  <div class="seg">${['今天','全部','已完成'].map(s=>`<button class="${seg===s?'on':''}" data-todoseg="${s}">${s}</button>`).join('')}</div>
  ${list.length? list.map(t=>`<div class="card todo-row">
    <span class="habit-check ${t.done?'on':''}" style="--c:#2E9E8F" data-toggle-t="${t.id}">${t.done?'✓':''}</span>
    <div class="todo-main"><div class="todo-name ${t.done?'dim':''}">${esc(t.title)}</div><div class="todo-due ${t.overdue&&!t.done?'overdue':''}">${t.overdue&&!t.done?'⚠ 逾期 · ':''}${t.due}</div></div>
    ${t.done?'<button class="mini green-t" data-restore="${t.id}">恢复</button>':''}
  </div>`).join('') : '<div class="empty">这里空空的 ✨</div>'}
  <div class="tip">💡 试试输入「明早十点开会」，日期会自动识别</div>
  <button class="fab" data-act="add-todo">＋ 新建待办</button>`;
}

// ---------- 弹层 ----------
function detailHTML(){
  const h = state.habits.find(x=>x.id===state.detail);
  if(!h) return '';
  const cells = [];
  for(let d=1; d<=30; d++){
    const isToday = d===13;
    const done = h.doneDays.includes(d);
    const alpha = done?1:(isToday?0.22:0.07);
    cells.push(`<div class="hm-cell ${isToday?'hm-today':''}" title="11月${d}日${done?' 已完成':' 未完成'}" style="background:${h.color};opacity:${alpha}" data-day="${d}:${h.id}"></div>`);
  }
  return `<div class="mask" data-close="1"><div class="sheet detail-sheet">
    <div class="sheet-bar"></div>
    <div class="sheet-title">${esc(h.name)} <span class="close" data-close="1">✕</span></div>
    <div class="stat3"><div><b>${h.streak}</b><span>当前连续</span></div><div><b>${Math.max(...[h.streak,12])}</b><span>最长连续</span></div><div><b>${Math.round(h.doneDays.length/30*100)}%</b><span>本月完成率</span></div></div>
    <div class="sec-title">11 月热力图 <span class="sec-note">点格子补卡 / 取消</span></div>
    <div class="hm-week">${['一','二','三','四','五','六','日'].map(w=>`<span>${w}</span>`).join('')}</div>
    <div class="hm-grid"><div class="hm-pad" style="grid-column:span 5"></div>${cells.map(c=>`<span class="hm-wrap">${c}</span>`).join('')}</div>
    <div class="hm-legend"><span>少</span><i style="opacity:.07;background:${h.color}"></i><i style="opacity:.35;background:${h.color}"></i><i style="opacity:.65;background:${h.color}"></i><i style="opacity:1;background:${h.color}"></i><span>多</span></div>
    <div class="tip">连续天数按真实记录重算；指定周几的习惯跨周末不断链</div>
  </div></div>`;
}

function addSheetHTML(){
  const k = state.addSheet; if(!k) return '';
  let body='';
  if(k==='habit') body=`<div class="fld"><label>习惯名称</label><input id="f1" placeholder="例如：早读 20 分钟" /></div>
    <div class="fld"><label>周期</label><div class="chips">${['每天','每周一三五','工作日','自定义'].map((c,i)=>`<button class="chip ${i===0?'on':''}" data-chip="${c}">${c}</button>`).join('')}</div></div>`;
  if(k==='txn') body=`<div class="fld"><label>类型</label><div class="chips"><button class="chip on" data-ledgerseg="支出">支出</button><button class="chip" data-ledgerseg="收入">收入</button></div></div>
    <div class="fld"><label>金额</label><div class="amount"><span>¥</span><input id="f1" type="number" inputmode="decimal" placeholder="0.00" /></div></div>
    <div class="fld"><label>分类 <span class="sec-note">与键盘同屏，不用切换页面</span></label><div class="cat-grid">${state.cats.map(c=>`<button class="cat" data-cat="${c.n}"><i>${c.i}</i><span>${c.n}</span></button>`).join('')}</div></div>
    <div class="fld"><label>备注（可选）</label><input id="f2" placeholder="例如：午饭" /></div>`;
  if(k==='todo') body=`<div class="fld"><label>待办标题</label><input id="f1" placeholder="试试「明早十点交报表」" /></div><div class="hint" id="parseHint">支持识别：今天 / 明天 / 后天 / 周X / X点X分</div>`;
  if(k==='settings') body=`<div class="set-row" data-act="export"><span>📤</span><div><b>导出数据</b><small>CSV 明细 + JSON 全量备份，免费不限笔数</small></div><span class="chev">›</span></div>
    <div class="set-row" data-act="restore"><span>📥</span><div><b>备份恢复</b><small>从 JSON 备份导入</small></div><span class="chev">›</span></div>
    <div class="set-row"><span>🔒</span><div><b>纯本地存储</b><small>不联网 · 无账号 · 无广告</small></div></div>
    <div class="set-row"><span>ℹ️</span><div><b>印记 v1.0</b><small>Material 3 · Android 12+ · 桌面小卡片</small></div></div>`;
  return `<div class="mask" data-close="1"><div class="sheet">
    <div class="sheet-bar"></div>
    <div class="sheet-title">${{habit:'新建习惯',txn:'记一笔',todo:'新建待办',settings:'设置'}[k]} <span class="close" data-close="1">✕</span></div>
    ${body}
    ${k!=='settings'?`<button class="primary" data-act="save-${k}">保存</button>`:''}
  </div></div>`;
}

function render(){
  const map = { today:todayHTML, ledger:ledgerHTML, notes:notesHTML, todos:todosHTML };
  $('#screen').innerHTML = `<div class="status"><span>9:41</span><span>●●● ▮</span></div>` + (map[state.tab]||todayHTML)();
  $('#navbar').innerHTML = [['today','◎','今日'],['ledger','¥','记账'],['notes','✎','备忘'],['todos','✓','待办']].map(([k,i,l])=>`<button class="${state.tab===k?'on':''}" data-tab="${k}"><span>${i}</span>${l}</button>`).join('');
  const wh = $('#widgetHost'); if(wh) wh.innerHTML = widgetHTML();
  $('#layers').innerHTML = (state.detail?detailHTML():'') + (state.addSheet?addSheetHTML():'') + (state.toast?`<div class="toast">${state.toast}</div>`:'');
  const si = $('#noteSearch'); if(si){ si.oninput = (e)=>{ state.noteQuery=e.target.value; const p=e.target.selectionStart; render(); const n=$('#noteSearch'); if(n){n.focus(); n.setSelectionRange(p,p);} }; }
}

// ---------- 交互 ----------
document.addEventListener('click', (e)=>{
  const el = e.target.closest('[data-tab],[data-nav],[data-act],[data-toggle-h],[data-toggle-t],[data-wtoggle-h],[data-wtoggle-t],[data-detail],[data-close],[data-chip],[data-cat],[data-ledgerseg],[data-todoseg],[data-pie],[data-pin],[data-restore],[data-note],[data-day]');
  if(!el) return;
  const d = el.dataset;
  if(d.close){ state.detail=null; state.addSheet=null; render(); return; }
  if(d.tab){ state.tab=d.tab; render(); return; }
  if(d.nav){ state.tab=d.nav; window.scrollTo(0,0); render(); return; }
  if(d.todoseg){ state.todoSeg=d.todoseg; render(); return; }
  if(d.ledgerseg){ state.ledgerSeg=d.ledgerseg; [...document.querySelectorAll('[data-ledgerseg]')].forEach(b=>b.classList.toggle('on', b===el)); return; }
  if(d.chip){ [...document.querySelectorAll('.chip')].forEach(b=>b.classList.toggle('on', b===el)); return; }
  if(d.cat){ document.querySelectorAll('.cat').forEach(b=>b.classList.toggle('on', b===el)); return; }
  if(d.pie){ const [name,amt,total]=d.pie.split(':'); toast(`${name} 本月 ${fmt(amt)} · 占 ${Math.round(amt/total*100)}%`); return; }
  if(d.pin!==undefined){ const n=state.notes.find(x=>x.id==d.pin); n.pinned=!n.pinned; toast(n.pinned?'已置顶':'已取消置顶'); render(); return; }
  if(d.restore!==undefined){ const t=state.todos.find(x=>x.id==d.restore); t.done=false; toast('已恢复到待办'); render(); return; }
  if(d.toggleH!==undefined || d.wtoggleH!==undefined){
    const id = d.toggleH ?? d.wtoggleH; const h=state.habits.find(x=>x.id==id);
    h.done=!h.done;
    if(h.done && !h.doneDays.includes(13)) h.doneDays.push(13);
    if(!h.done) h.doneDays=h.doneDays.filter(x=>x!==13);
    if(d.wtoggleH!==undefined){ el.classList.add('wg-done'); setTimeout(render,260); return; }
    toast(h.done?'打卡成功 🔥':'已取消打卡'); render(); return;
  }
  if(d.toggleT!==undefined || d.wtoggleT!==undefined){
    const id = d.toggleT ?? d.wtoggleT; const t=state.todos.find(x=>x.id==id);
    t.done=!t.done;
    if(d.wtoggleT!==undefined){ el.classList.add('wg-done'); setTimeout(render,260); return; }
    toast(t.done?'已完成 · 可在「已完成」中恢复':'已恢复'); render(); return;
  }
  if(d.day){ const [day,id]=d.day.split(':').map(Number); const h=state.habits.find(x=>x.id==id); const i=h.doneDays.indexOf(day); if(i>=0){h.doneDays.splice(i,1); toast(`11月${day}日 已取消补卡`);} else {h.doneDays.push(day); toast(`11月${day}日 已补卡`);} render(); return; }
  if(d.detail!==undefined){ state.detail=+d.detail; render(); return; }
  if(d.note!==undefined){ toast('长按或点开可编辑（原型略）'); return; }
  if(d.act==='settings'){ state.addSheet='settings'; render(); return; }
  if(d.act==='add-habit'||d.act==='add-txn'||d.act==='add-todo'){ state.addSheet=d.act.replace('add-',''); render(); return; }
  if(d.act==='focus-note'){ const i=$('#noteInput'); if(i){i.focus();} return; }
  if(d.act==='save-note'){ const i=$('#noteInput'); const v=i&&i.value.trim(); if(!v){toast('先写点什么吧');return;} state.notes.unshift({id:Date.now(),text:v,pinned:false,tag:'',time:'刚刚'}); toast('已保存'); render(); return; }
  if(d.act==='export'){ toast('已导出：明细.csv + 备份.json'); return; }
  if(d.act==='restore'){ toast('已从备份恢复'); return; }
  if(d.act && d.act.startsWith('save-')){
    const k=d.act.slice(5); const v=$('#f1')?$('#f1').value.trim():'';
    if(k==='habit'){ if(!v){toast('给习惯起个名字');return;} state.habits.push({id:Date.now(),name:v,color:'#2E9E8F',cycle:'每天',done:false,streak:0,doneDays:[]}); toast('习惯已创建'); }
    if(k==='txn'){ const amt=parseFloat(v); if(!amt||amt<=0){toast('输入有效金额');return;} const cat=document.querySelector('.cat.on'); const note=$('#f2')?$('#f2').value.trim():''; state.txns.push({id:Date.now(),type:'expense',amount:amt,cat:cat?cat.dataset.cat:'其他',icon:cat?cat.querySelector('i').textContent:'📦',note:note,date:TODAY}); toast(`已记一笔 ${fmt(amt)}`); }
    if(k==='todo'){ if(!v){toast('写个标题');return;} state.todos.push({id:Date.now(),title:v.replace(/今天|明天|后天|周[一二三四五六日]|(\d{1,2})点(半|\d{1,2}分?)?/g,'').trim()||v,due:v.includes('明天')?'明天':v.includes('今天')?'今天':'今天',done:false,overdue:false}); toast('已创建 · 日期已自动识别'); }
    state.addSheet=null; render(); return;
  }
});

// 支持 ?tab=ledger / notes / todos 深链（便于单独预览某一页）
try { const _p = new URLSearchParams(location.search); if (_p.has('tab')) state.tab = _p.get('tab'); } catch (_) {}

render();
