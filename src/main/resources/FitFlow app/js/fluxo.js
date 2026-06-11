// ─── TABS ────────────────────────────────
let currentTab = 'desktop';
function switchTab(name) {
  currentTab = name;
  document.querySelectorAll('.tab-btn').forEach(b=>b.classList.toggle('active',b.dataset.tab===name));
  document.querySelectorAll('.tab-panel').forEach(p=>p.classList.toggle('active',p.id==='panel-'+name));
  // Dim all boxes on both panels
  document.querySelectorAll('.tab-panel .uml').forEach(b=>{b.classList.remove('active');b.classList.add('done')});
  document.querySelectorAll('.tab-panel .mt.active').forEach(m=>m.classList.remove('active'));
  document.querySelectorAll('.conn-line.active,.conn-line.done').forEach(l=>l.classList.remove('active','done'));
  document.querySelectorAll('.conn-label.active').forEach(l=>l.classList.remove('active'));
  document.querySelectorAll('.zone.active').forEach(z=>z.classList.remove('active'));
  lastBox = null;
  lastControllerId = null;
  activeConnection = null;
  setTimeout(drawAllLines, 50);
}
document.querySelectorAll('.tab-btn').forEach(b=>b.addEventListener('click',()=>switchTab(b.dataset.tab)));

// ─── HELPERS ─
const $ = (id) => {
  const panel = document.querySelector('.tab-panel.active');
  return panel ? panel.querySelector('#'+id) : document.getElementById(id);
};
const $svg = () => {
  const panel = document.querySelector('.tab-panel.active');
  return panel ? panel.querySelector('svg') : document.getElementById('connSvg-dt');
};
const $all = (sel) => {
  const panel = document.querySelector('.tab-panel.active');
  return panel ? panel.querySelectorAll(sel) : document.querySelectorAll(sel);
};

// ─── CONNECTIONS: DESKTOP ────────────────
const CONNS_DT = [
  { from:'cb-painel',  fs:'bottom', to:'cb-treinodao', ts:'top',    label:'chama' },
  { from:'cb-painel',  fs:'bottom', to:'cb-alunodao',  ts:'top',    label:'chama' },
  { from:'cb-painel',  fs:'bottom', to:'cb-exercdao',  ts:'top',    label:'chama' },
  { from:'cb-analise', fs:'bottom', to:'cb-treinodao', ts:'top',    label:'chama' },
  { from:'cb-analise', fs:'bottom', to:'cb-alunodao',  ts:'top',    label:'chama' },
  { from:'cb-analise', fs:'bottom', to:'cb-exercdao',  ts:'top',    label:'chama' },
  { from:'cb-treinodao',fs:'bottom',to:'cb-jpa',       ts:'top',    label:'JPAUtil.getEM()' },
  { from:'cb-alunodao', fs:'bottom',to:'cb-jpa',       ts:'top',    label:'JPAUtil.getEM()' },
  { from:'cb-exercdao', fs:'bottom',to:'cb-jpa',       ts:'top',    label:'JPAUtil.getEM()' },
  { from:'cb-userdao',  fs:'bottom',to:'cb-jpa',       ts:'top',    label:'JPAUtil.getEM()' },
  { from:'cb-jpa',     fs:'right',  to:'cb-db',        ts:'left',   label:'JDBC' },
  { from:'cb-jpa',     fs:'right',  to:'cb-entity',    ts:'left',   label:'JPQL' },
  { from:'cb-painel',  fs:'bottom', to:'cb-userdao',   ts:'top',    label:'autenticar' },
  { from:'cb-usuarios',fs:'bottom', to:'cb-userdao',   ts:'top',    label:'chama' },
  { from:'cb-exercicios',fs:'bottom',to:'cb-exercdao',  ts:'top',   label:'chama' },
  { from:'cb-fichas',  fs:'bottom', to:'cb-treinodao', ts:'top',    label:'chama' },
  { from:'cb-fichas',  fs:'bottom', to:'cb-alunodao',  ts:'top',    label:'chama' },
  { from:'cb-login-fxml',      fs:'bottom',to:'cb-login',         ts:'top', label:'FXMLLoader' },
  { from:'cb-recuperar-fxml',  fs:'bottom',to:'cb-recuperar',     ts:'top', label:'FXMLLoader' },
  { from:'cb-trocarsenha-fxml',fs:'bottom',to:'cb-trocarsenha',   ts:'top', label:'FXMLLoader' },
  { from:'cb-painel-fxml',     fs:'bottom',to:'cb-painel',        ts:'top', label:'FXMLLoader' },
  { from:'cb-dashboard-fxml',  fs:'bottom',to:'cb-dashboard',     ts:'top', label:'FXMLLoader' },
  { from:'cb-usuarios-fxml',   fs:'bottom',to:'cb-usuarios',      ts:'top', label:'FXMLLoader' },
  { from:'cb-formusuario-fxml',fs:'bottom',to:'cb-formusuario',   ts:'top', label:'FXMLLoader' },
  { from:'cb-exercicios-fxml', fs:'bottom',to:'cb-exercicios',    ts:'top', label:'FXMLLoader' },
  { from:'cb-formexercicio-fxml',fs:'bottom',to:'cb-formexercicio',ts:'top',label:'FXMLLoader' },
  { from:'cb-fichas-fxml',     fs:'bottom',to:'cb-fichas',        ts:'top', label:'FXMLLoader' },
  { from:'cb-analise-fxml',    fs:'bottom',to:'cb-analise',       ts:'top', label:'FXMLLoader' },
  { from:'cb-detalhes-fxml',   fs:'bottom',to:'cb-detalhes',      ts:'top', label:'FXMLLoader' },
];

const CONNS_MB = [
  { from:'cb-server',  fs:'bottom', to:'cb-treinodao', ts:'top',    label:'chama' },
  { from:'cb-server',  fs:'bottom', to:'cb-alunodao',  ts:'top',    label:'chama' },
  { from:'cb-server',  fs:'bottom', to:'cb-exercdao',  ts:'top',    label:'chama' },
  { from:'cb-server',  fs:'bottom', to:'cb-userdao',   ts:'top',    label:'chama' },
  { from:'cb-server',  fs:'right',  to:'cb-eventbus',  ts:'left',   label:'emit()' },
  { from:'cb-server',  fs:'right',  to:'cb-gif',       ts:'left',   label:'Giphy API' },
  { from:'cb-treinodao',fs:'bottom',to:'cb-jpa',       ts:'top',    label:'JPAUtil.getEM()' },
  { from:'cb-alunodao', fs:'bottom',to:'cb-jpa',       ts:'top',    label:'JPAUtil.getEM()' },
  { from:'cb-exercdao', fs:'bottom',to:'cb-jpa',       ts:'top',    label:'JPAUtil.getEM()' },
  { from:'cb-userdao',  fs:'bottom',to:'cb-jpa',       ts:'top',    label:'JPAUtil.getEM()' },
  { from:'cb-jpa',     fs:'right',  to:'cb-db',        ts:'left',   label:'JDBC' },
  { from:'cb-jpa',     fs:'right',  to:'cb-entity',    ts:'left',   label:'JPQL' },
];

const activeConns = () => currentTab==='desktop' ? CONNS_DT : CONNS_MB;

function anchor(box, side, lane) {
  const left = box.offsetLeft;
  const top = box.offsetTop;
  const width = box.offsetWidth;
  const height = box.offsetHeight;
  const cx = left + width / 2;
  const cy = top + height / 2;
  const d = 16;
  const offset = (lane || 0) * d;
  switch(side) {
    case 'bottom': return { x: cx + offset, y: top + height };
    case 'top':    return { x: cx + offset, y: top };
    case 'right':  return { x: left + width, y: cy + offset };
    case 'left':   return { x: left, y: cy + offset };
    default:       return { x: cx, y: cy };
  }
}

function orthogonalPath(a, b) {
  const dy = b.y - a.y, dx = b.x - a.x;
  const midY = a.y + dy/2;
  const midX = a.x + dx/2;
  if (Math.abs(dy) > Math.abs(dx)) {
    return `M ${a.x} ${a.y} L ${a.x} ${midY} L ${b.x} ${midY} L ${b.x} ${b.y}`;
  }
  return `M ${a.x} ${a.y} L ${midX} ${a.y} L ${midX} ${b.y} L ${b.x} ${b.y}`;
}

function drawAllLines() {
  const conns = activeConns();
  const svg = $svg();
  if (!svg) return;
  svg.innerHTML = '';
  const canvas = svg.closest('.canvas');
  if (!canvas) return;
  svg.style.width = canvas.offsetWidth + 'px';
  svg.style.height = canvas.offsetHeight + 'px';

  const fromLanes = {}, toLanes = {};
  conns.forEach(c => {
    const fk = c.from+'|'+c.fs, tk = c.to+'|'+c.ts;
    fromLanes[fk] = (fromLanes[fk]||0) + 1;
    toLanes[tk] = (toLanes[tk]||0) + 1;
  });
  const fromIdx = {}, toIdx = {};
  conns.forEach(c => {
    const fk = c.from+'|'+c.fs, tk = c.to+'|'+c.ts;
    fromIdx[fk] = (fromIdx[fk]||0) + 1;
    toIdx[tk] = (toIdx[tk]||0) + 1;
    c._fl = fromIdx[fk] - 1 - (fromLanes[fk]-1)/2;
    c._tl = toIdx[tk] - 1 - (toLanes[tk]-1)/2;
  });

  conns.forEach(c => {
    const fEl = $(c.from), tEl = $(c.to);
    if (!fEl||!tEl) return;
    const a = anchor(fEl, c.fs, c._fl);
    const b = anchor(tEl, c.ts, c._tl);
    const id = 'conn-'+c.from+'-'+c.to;
    const p = document.createElementNS('http://www.w3.org/2000/svg','path');
    p.setAttribute('d', orthogonalPath(a,b));
    p.setAttribute('class','conn-line');
    p.id = id;
    svg.appendChild(p);
    let defs = svg.querySelector('defs');
    if (!defs) { defs = document.createElementNS('http://www.w3.org/2000/svg','defs'); svg.prepend(defs); }
    const markerId = 'mk-'+c.from+'-'+c.to;
    let marker = svg.querySelector('#'+markerId);
    if (!marker) {
      marker = document.createElementNS('http://www.w3.org/2000/svg','marker');
      marker.setAttribute('id', markerId);
      marker.setAttribute('viewBox','0 0 10 10');
      marker.setAttribute('refX','8'); marker.setAttribute('refY','5');
      marker.setAttribute('markerWidth','7'); marker.setAttribute('markerHeight','7');
      marker.setAttribute('orient','auto');
      const poly = document.createElementNS('http://www.w3.org/2000/svg','polygon');
      poly.setAttribute('points','0,0 10,5 0,10');
      poly.setAttribute('fill','#bbb');
      marker.appendChild(poly);
      defs.appendChild(marker);
    }
    p.setAttribute('marker-end','url(#'+markerId+')');
    if (c.label) {
      const l = document.createElementNS('http://www.w3.org/2000/svg','text');
      const lx = (a.x + b.x)/2 + 6;
      const ly = (a.y + b.y)/2 - 6;
      l.setAttribute('x', lx); l.setAttribute('y', ly);
      l.setAttribute('class','conn-label');
      l.id = 'clbl-'+c.from+'-'+c.to;
      l.textContent = c.label;
      svg.appendChild(l);
    }
  });
}
window.addEventListener('resize',()=>setTimeout(drawAllLines,100));

// ─── EVENT MAP ─────────────────────────────
function mapEvent(c,a){
  const s=(c+'.'+a).toLowerCase();
  if(s.includes('logincontroller')&&!s.includes('painel')) return {box:'cb-login',zone:'desktop'};
  if(s.includes('recuperarsenha')) return {box:'cb-recuperar',zone:'desktop'};
  if(s.includes('trocarsenha')) return {box:'cb-trocarsenha',zone:'desktop'};
  if(s.includes('painelprincipal')&&!s.includes('inicio')) return {box:'cb-painel',method:'mt-carregarDash',zone:'desktop'};
  if(s.includes('dashboardinicio')||(s.includes('painelprincipal')&&s.includes('inicio'))) return {box:'cb-dashboard',zone:'desktop'};
  if(s.includes('usuarioscontroller')) return {box:'cb-usuarios',method:'mt-usuarios-listar',zone:'desktop'};
  if(s.includes('formusuariocontroller')) return {box:'cb-formusuario',zone:'desktop'};
  if(s.includes('exercicioscontroller')) return {box:'cb-exercicios',zone:'desktop'};
  if(s.includes('formexerciciocontroller')) return {box:'cb-formexercicio',zone:'desktop'};
  if(s.includes('fichastreino')) return {box:'cb-fichas',method:'mt-fichas-listarProg',zone:'desktop'};
  if(s.includes('analisealuno')) return {box:'cb-analise',method:'mt-carregarHist',zone:'desktop'};
  if(s.includes('detalhestreinorealizado')) return {box:'cb-detalhes',zone:'desktop'};
  if(s.includes('loginhandler')) return {box:'cb-server',method:'mt-loginHandler',zone:'mobile'};
  if((s.includes('servidor')&&s.includes('ficha'))||s.includes('fichahandler')) return {box:'cb-server',method:'mt-fichaHandler',zone:'mobile'};
  if(s.includes('finalizar')&&!s.includes('dao')) return {box:'cb-server',method:'mt-finHandler',zone:'mobile'};
  if(s.includes('dashboardhandler')||(s.includes('servidor')&&s.includes('dashboard'))) return {box:'cb-server',method:'mt-dashHandler',zone:'mobile'};
  if(s.includes('historicohandler')||(s.includes('servidor')&&s.includes('historico'))) return {box:'cb-server',method:'mt-histHandler',zone:'mobile'};
  if(s.includes('perfilhandler')||(s.includes('servidor')&&s.includes('perfil'))) return {box:'cb-server',method:'mt-perfHandler',zone:'mobile'};
  if(s.includes('eventbus')||s.includes('sse')) return {box:'cb-eventbus',method:null,zone:'mobile'};
  if(s.includes('gifsearch')||s.includes('giphy')) return {box:'cb-gif',method:null,zone:'mobile'};
  if(s.includes('treinodao')){
    let m=null;
    if(s.includes('listarprogramacoes')||s.includes('ficha')) m='mt-listarProg';
    else if(s.includes('listaritens')) m='mt-listarItens';
    else if(s.includes('comentario')||s.includes('feedback')) m='mt-buscarComent';
    else if(s.includes('total')||s.includes('quantidade')) m='mt-buscarTotal';
    else if(s.includes('semana')) m='mt-buscarSemana';
    return {box:'cb-treinodao',method:m,zone:'dao'};
  }
  if(s.includes('alunodao')){
    let m=null;
    if(s.includes('avaliacao')) m='mt-buscarAvals';
    else if(s.includes('salvar')||s.includes('atualizar')) m='mt-salvar';
    else if(s.includes('todos')) m='mt-buscarTodos';
    return {box:'cb-alunodao',method:m,zone:'dao'};
  }
  if(s.includes('exerciciodao')) return {box:'cb-exercdao',method:null,zone:'dao'};
  if(s.includes('usuariodao')) return {box:'cb-userdao',method:s.includes('autenticar')?'mt-autenticar':null,zone:'dao'};
  if(s.includes('jpa')||s.includes('entitymanager')||s.includes('jpql')||s.includes('persist')||s.includes('merge')||s.includes('find')||s.includes('query')) return {box:'cb-jpa',method:'mt-getEM',zone:'infra'};
  if(s.includes('postgres')||s.includes('db')||s.includes('sql')||s.includes('select')||s.includes('insert')||s.includes('count')){
    let m=null;
    if(s.includes('insert')) m='mt-insert';
    else if(s.includes('select')) m='mt-select';
    else if(s.includes('count')) m='mt-count';
    return {box:'cb-db',method:m,zone:'infra'};
  }
  if(s.includes('entity')||s.includes('aluno')||s.includes('treino')||s.includes('usuario')) return {box:'cb-entity',method:null,zone:'infra'};
  return null;
}

const viewMap = {
  'cb-login': 'cb-login-fxml',
  'cb-recuperar': 'cb-recuperar-fxml',
  'cb-trocarsenha': 'cb-trocarsenha-fxml',
  'cb-painel': 'cb-painel-fxml',
  'cb-dashboard': 'cb-dashboard-fxml',
  'cb-usuarios': 'cb-usuarios-fxml',
  'cb-formusuario': 'cb-formusuario-fxml',
  'cb-exercicios': 'cb-exercicios-fxml',
  'cb-formexercicio': 'cb-formexercicio-fxml',
  'cb-fichas': 'cb-fichas-fxml',
  'cb-analise': 'cb-analise-fxml',
  'cb-detalhes': 'cb-detalhes-fxml'
};

// ─── CONNECTION ANIMATION ───────
let activeConnection = null;
function activateConnection(fromBox,toBox){
  if(activeConnection){
    activeConnection.classList.remove('active');
    activeConnection.classList.add('done');
  }
  $all('.conn-label.active').forEach(l=>l.classList.remove('active'));
  const svg = $svg();
  if(svg) svg.querySelectorAll('marker polygon').forEach(p=>p.setAttribute('fill','#bbb'));

  let connId='conn-'+fromBox+'-'+toBox;
  let conn=$(connId);
  if(!conn){ connId='conn-'+toBox+'-'+fromBox; conn=$(connId); }
  if(!conn){
    const pfx='conn-'+fromBox+'-';
    const cs=$all('[id^="'+pfx+'"]');
    if(cs.length) conn=cs[0];
  }
  if(conn){
    conn.classList.remove('done');
    conn.classList.add('active');
    activeConnection = conn;
    const mid = conn.id.replace('conn-','');
    const svg2 = $svg();
    if(svg2){
      const mk = svg2.querySelector('#mk-'+mid);
      if(mk) mk.querySelector('polygon').setAttribute('fill','#1565c0');
    }
    const lbl=$('clbl-'+mid);
    if(lbl) lbl.classList.add('active');
  }
}

// ─── SET CURRENT METHOD DISPLAY ────
function setCurMethod(boxId, component, action) {
  const el = $('cur-act-' + boxId.replace('cb-', ''));
  if (el) {
    const text = component ? component + '.' + action + '()' : (action || '—');
    el.textContent = text;
  }
}

// ─── SSE + EVENT QUEUE ──────────────────
let evCount=0, es=null, lastBox=null, lastControllerId=null;
let eventQueue=[], processing=false;
const STEP_MS=1000;

function conectar(){
  es=new EventSource('/api/sse');
  es.onopen=()=>{
    document.getElementById('dot').className='dot on';
    document.getElementById('statText').textContent='Conectado';
  };
  es.onmessage=e=>{
    try{const d=JSON.parse(e.data);if(d.type==='dao_call')eventQueue.push(d);if(!processing)processarFila()}catch(_){}
  };
  es.onerror=()=>{
    document.getElementById('dot').className='dot off';
    document.getElementById('statText').textContent='Reconectando...';
    es.close();setTimeout(conectar,2000);
  };
}

function processarFila(){
  if(eventQueue.length===0){processing=false;return;}
  processing=true;
  const d=eventQueue.shift();
  renderEvent(d);
  setTimeout(processarFila,STEP_MS);
}

function renderEvent(d){
  evCount++;
  document.getElementById('evCount').textContent=evCount;
  const m=mapEvent(d.component,d.action);
  const now=Date.now(), ts=d.timestamp||now;
  
  document.getElementById('dComp').textContent=d.component+'.'+d.action+'()';
  document.getElementById('dAct').textContent=d.detail || '';
  document.getElementById('dDetail').textContent=m?'['+(m.zone||'?').toUpperCase()+']':'';
  document.getElementById('dTime').textContent = new Date(ts).toLocaleTimeString();
  
  if(!m){addLog(d.component,d.action,d.detail,ts);return;}

  if(m.zone==='desktop' && currentTab!=='desktop') switchTab('desktop');
  else if(m.zone==='mobile' && currentTab!=='mobile') switchTab('mobile');

  // Track last controller box so its FXML survives DAO/infra events
  if (m.zone === 'desktop' || m.zone === 'mobile') lastControllerId = m.box;

  // Keep previous box + last controller + their FXMLs lit
  const prevBox = lastBox;
  $all('.uml.active').forEach(b=>{
    if (prevBox && (b.id === prevBox || b.id === viewMap[prevBox])) return;
    if (lastControllerId && (b.id === lastControllerId || b.id === viewMap[lastControllerId])) return;
    b.classList.remove('active');
    b.classList.add('done');
  });
  $all('.zone.active').forEach(z=>z.classList.remove('active'));

  const zone=$('zone-'+m.zone);
  if(zone) zone.classList.add('active');

  const box=$(m.box);
  if(box){
    box.classList.remove('done');box.classList.add('active');
    $all('.mt.active').forEach(mt=>mt.classList.remove('active'));
    if(m.method){const me=$(m.method);if(me)me.classList.add('active')}
    if(lastBox&&lastBox!==m.box) activateConnection(lastBox,m.box);
    lastBox=m.box;
  }
  
  const viewId=viewMap[m.box];
  if(viewId){
    const vb=$(viewId);
    if(vb){vb.classList.remove('done');vb.classList.add('active')}
  }

  // Update current-method display for DAO / infra boxes
  const daoBoxes = ['cb-treinodao','cb-alunodao','cb-exercdao','cb-userdao','cb-jpa','cb-db','cb-entity'];
  if (daoBoxes.includes(m.box)) {
    setCurMethod(m.box, d.component, d.action);
  }
  addLog(d.component,d.action,d.detail,ts,m.zone);
}

function addLog(component,action,detail,ts,zone){
  const log=document.getElementById('logBody');
  const empty=log.querySelector('.log-empty');
  if(empty)empty.remove();
  const item=document.createElement('div');
  item.className='log-item';
  item.innerHTML='<div><span class="lc">'+component+'</span>.<span class="la">'+action+'()</span></div>'+
    (detail?'<div class="ld">'+detail+'</div>':'')+
    '<div class="lt">'+new Date(ts).toLocaleTimeString()+'</div>';
  log.insertBefore(item,log.firstChild);
  while(log.children.length>50)log.removeChild(log.lastChild);
}

function limparLog(){
  eventQueue=[];processing=false;
  document.getElementById('logBody').innerHTML='<div class="log-empty">Sistema pronto.</div>';
  document.getElementById('evCount').textContent='0';
  document.querySelectorAll('.tab-panel').forEach(p=>{
    p.querySelectorAll('.uml').forEach(b=>{b.classList.remove('active');b.classList.add('done')});
    p.querySelectorAll('.mt.active').forEach(m=>m.classList.remove('active'));
    p.querySelectorAll('.conn-line.active,.conn-line.done').forEach(l=>l.classList.remove('active','done'));
    p.querySelectorAll('.conn-label.active').forEach(l=>l.classList.remove('active'));
    p.querySelectorAll('.zone.active').forEach(z=>z.classList.remove('active'));
  });
  activeConnection=null;lastBox=null;lastControllerId=null;
}

document.querySelectorAll('.tab-panel .uml').forEach(b => b.classList.add('done'));
drawAllLines();
conectar();

// ─── ZOOM ──────────────────────────────────
let zoomLevel = 1;
function applyZoom() {
  const panel = document.querySelector('.tab-panel.active');
  if (!panel) return;
  const canvas = panel.querySelector('.canvas');
  if (!canvas) return;
  canvas.style.transform = `scale(${zoomLevel})`;
  canvas.style.transformOrigin = 'top left';
  document.getElementById('zoomLabel').textContent = Math.round(zoomLevel * 100) + '%';
  setTimeout(drawAllLines, 50);
}
function zoomIn() {
  zoomLevel = Math.min(zoomLevel * 1.25, 3);
  applyZoom();
}
function zoomOut() {
  zoomLevel = Math.max(zoomLevel / 1.25, 0.25);
  applyZoom();
}
function zoomReset() {
  zoomLevel = 1;
  applyZoom();
}
// Ctrl+scroll / pinch-to-zoom
document.addEventListener('wheel', e => {
  if (!e.ctrlKey && !e.metaKey) return;
  e.preventDefault();
  const delta = e.deltaY > 0 ? -1 : 1;
  zoomLevel *= Math.pow(1.08, delta);
  zoomLevel = Math.min(Math.max(zoomLevel, 0.25), 3);
  applyZoom();
}, { passive: false });