/* ─── CONFIG ─────────────────────────────────────────── */
const API = '';

/* ─── STATE ──────────────────────────────────────────── */
let alunoId = 0, alunoNome = '', alunoEmail = '';
let treinoIdAtual = 0, exercicios = [], historicoRealizado = [];
let exAtivoIndex = -1, serieAtual = 1, segundos = 0, estadoTimer = 'PARADO', timerInterval = null;

/* ─── INIT ───────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
    alunoId = parseInt(localStorage.getItem('fitflow_aluno_id'));
    alunoNome = localStorage.getItem('fitflow_aluno_nome') || '';
    alunoEmail = localStorage.getItem('fitflow_aluno_email') || '';

    if (!alunoId) { window.location.href = 'login.html'; return; }

    document.getElementById('header-nome').textContent = alunoNome;

    document.querySelectorAll('.nav-item').forEach(el => {
        el.addEventListener('click', () => mudarAba(el.dataset.tab));
    });

    mudarAba('feed');
    carregarFeed();
    carregarTreino();
    carregarPerformance();
    carregarPerfil();
});

/* ─── NAVEGAÇÃO ──────────────────────────────────────── */
function mudarAba(tab) {
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    document.getElementById('screen-' + tab).classList.add('active');
    document.querySelector(`.nav-item[data-tab="${tab}"]`).classList.add('active');

    // Redesenhar chart ao mostrar Performance (canvas precisa estar visível)
    if (tab === 'performance') setTimeout(desenharGrafico, 100);
}

/* ─── API HELPER ─────────────────────────────────────── */
async function api(path, opts = {}) {
    const token = localStorage.getItem('fitflow_token');
    const headers = { 'Content-Type': 'application/json', ...(token ? { 'Authorization': 'Bearer ' + token } : {}), ...opts.headers };
    const res = await fetch(API + path, { ...opts, headers });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
}

/* ════════════════════════════════════════════════════════
   FEED
   ════════════════════════════════════════════════════════ */
async function carregarFeed() {
    const el = document.getElementById('feed-lista');
    el.innerHTML = '<div class="loading"><div class="spinner"></div>Carregando...</div>';
    try {
        const dados = await api('/api/aluno/historico?alunoId=' + alunoId);
        if (!dados.length) {
            el.innerHTML = '<div class="empty-state"><div class="icon">🏋️</div><p>Nenhum treino registrado ainda.<br>Comece hoje!</p></div>';
            return;
        }
        el.innerHTML = dados.map(d =>
            '<div class="feed-item">' +
                '<div class="date">' + d.data + (!d.lido ? ' <span class="badge-unread">NOVO</span>' : '') + '</div>' +
                '<div class="treino-name">' + d.treinoNome + '</div>' +
                '<div class="comment">' + d.comentario + '</div>' +
            '</div>'
        ).join('');
    } catch (e) {
        el.innerHTML = '<div class="empty-state"><div class="icon">⚠️</div><p>Erro ao carregar histórico.</p></div>';
    }
}

/* ════════════════════════════════════════════════════════
   TREINO
   ════════════════════════════════════════════════════════ */
async function carregarTreino() {
    const el = document.getElementById('treino-lista');
    el.innerHTML = '<div class="loading"><div class="spinner"></div>Carregando ficha...</div>';
    try {
        const data = await api('/api/ficha?alunoId=' + alunoId);
        treinoIdAtual = data.idFicha;
        exercicios = data.exercicios;
        historicoRealizado = [];
        document.getElementById('treino-titulo').textContent = data.nomeTreino;
        document.getElementById('treino-objetivo').textContent = data.objetivo || '';
        renderizarExercicios();
    } catch (e) {
        el.innerHTML = '<div class="empty-state"><div class="icon">📋</div><p>Nenhuma ficha ativa.<br>Procure seu instrutor.</p></div>';
        document.getElementById('treino-titulo').textContent = 'Sem ficha ativa';
    }
}

function renderizarExercicios() {
    const el = document.getElementById('treino-lista');
    el.innerHTML = exercicios.map((ex, i) => {
        const feito = historicoRealizado.find(h => h.itemTreinoId === ex.idItem);
        const seriesInfo = ex.series.map(s => s.reps + 'x' + s.carga + 'kg').join(', ');
        return '<div class="card exercicio-card ' + (feito ? 'exercicio-done' : '') + '" id="ex-card-' + i + '">' +
            '<div class="exercicio-header" onclick="' + (!feito ? "toggleGif(" + i + ")" : "") + '">' +
                '<div>' +
                    '<div class="nome">' + (feito ? '✅ ' : '') + ex.nomeExercicio + '</div>' +
                    '<div class="info">' + ex.series.length + ' séries · ' + seriesInfo + ' · ' + ex.descanso + 's descanso</div>' +
                '</div>' +
                '<div style="display:flex;align-items:center;gap:6px;">' +
                    (ex.urlMidia ? '<span style="color:#666;font-size:14px;" title="Ver GIF">🎬</span>' : '') +
                    (!feito ? '<span style="color:#00d2ff;font-size:20px;">▶</span>' : '<span style="color:#27ae60;">✓</span>') +
                '</div>' +
            '</div>' +
            (ex.urlMidia && !feito ? '<div class="exercicio-gif" id="ex-gif-' + i + '"><img src="' + ex.urlMidia + '" alt="' + ex.nomeExercicio + '" loading="lazy"></div>' : '') +
        '</div>';
    }).join('');

    // Sempre mostrar o botão finalizar, mesmo com exercícios pendentes
    document.getElementById('treino-finalizar-area').style.display = 'block';
}

function toggleGif(i) {
    const gif = document.getElementById('ex-gif-' + i);
    if (!gif) return;
    const card = document.getElementById('ex-card-' + i);
    card.classList.toggle('open');
}

/* ─── MODO FOCO ──────────────────────────────────────── */
function abrirFoco(index) {
    exAtivoIndex = index;
    serieAtual = 1;
    segundos = 0;
    estadoTimer = 'PARADO';
    const ex = exercicios[index];
    atualizarTextosFoco();
    document.getElementById('foco-cronometro').textContent = '00:00';
    document.getElementById('foco-cronometro').style.color = '#fff';
    document.getElementById('foco-area-carga').style.display = 'none';
    document.getElementById('foco-status').textContent = 'Preparado?';
    const btn = document.getElementById('btn-foco');
    btn.className = 'btn-foco btn-foco-iniciar';
    btn.textContent = '▶ Iniciar Série 1';
    document.getElementById('tela-foco').classList.add('active');
}

function atualizarTextosFoco() {
    const ex = exercicios[exAtivoIndex];
    const serieInfo = ex.series[serieAtual - 1];
    document.getElementById('foco-nome').textContent = ex.nomeExercicio;
    document.getElementById('foco-serie').textContent = 'Série ' + serieAtual + ' de ' + ex.series.length + ' · Meta: ' + serieInfo.reps + ' reps ' + serieInfo.carga + 'kg';
    document.getElementById('foco-carga-input').value = serieInfo.carga;
    // GIF no modo foco
    const gifEl = document.getElementById('foco-gif');
    if (ex.urlMidia) {
        gifEl.src = ex.urlMidia;
        gifEl.style.display = 'block';
    } else {
        gifEl.style.display = 'none';
    }
}

function acaoFoco() {
    const ex = exercicios[exAtivoIndex];
    const btn = document.getElementById('btn-foco');
    const status = document.getElementById('foco-status');

    if (estadoTimer === 'PARADO') {
        estadoTimer = 'TREINANDO';
        segundos = 0;
        status.textContent = 'Executando... Foco!';
        document.getElementById('foco-cronometro').style.color = '#fff';
        btn.className = 'btn-foco btn-foco-parar';
        btn.innerHTML = '⏹ Concluir Série ' + serieAtual;
        timerInterval = setInterval(function () { segundos++; atualizarRelogio(); }, 1000);
    } else if (estadoTimer === 'TREINANDO') {
        clearInterval(timerInterval);
        estadoTimer = 'DESCANSANDO';
        segundos = ex.descanso;
        status.textContent = 'Descanso...';
        document.getElementById('foco-cronometro').style.color = '#f39c12';
        document.getElementById('foco-area-carga').style.display = 'block';
        btn.className = 'btn-foco btn-foco-proximo';
        btn.innerHTML = serieAtual < ex.series.length ? '⏭ Pular / Próxima Série' : '✅ Finalizar Exercício';
        timerInterval = setInterval(function () {
            segundos--;
            atualizarRelogio();
            if (segundos <= 0) { clearInterval(timerInterval); document.getElementById('foco-cronometro').textContent = 'PRONTO!'; }
        }, 1000);
    } else if (estadoTimer === 'DESCANSANDO') {
        clearInterval(timerInterval);
        const carga = parseFloat(document.getElementById('foco-carga-input').value) || 0;
        if (serieAtual < ex.series.length) {
            serieAtual++;
            estadoTimer = 'PARADO';
            segundos = 0;
            document.getElementById('foco-cronometro').textContent = '00:00';
            document.getElementById('foco-cronometro').style.color = '#fff';
            document.getElementById('foco-area-carga').style.display = 'none';
            status.textContent = 'Preparado?';
            btn.className = 'btn-foco btn-foco-iniciar';
            btn.textContent = '▶ Iniciar Série ' + serieAtual;
            atualizarTextosFoco();
        } else {
            historicoRealizado.push({ itemTreinoId: ex.idItem, carga: carga, feito: true });
            fecharFoco();
            renderizarExercicios();
        }
    }
}

function fecharFoco() {
    clearInterval(timerInterval);
    document.getElementById('tela-foco').classList.remove('active');
}

function pularExercicio() {
    if (confirm('Pular "' + exercicios[exAtivoIndex].nomeExercicio + '"? Ele será marcado como não realizado.')) {
        historicoRealizado.push({ itemTreinoId: exercicios[exAtivoIndex].idItem, carga: 0, feito: false });
        fecharFoco();
        renderizarExercicios();
    }
}

function atualizarRelogio() {
    const s = Math.max(0, segundos);
    const m = Math.floor(s / 60).toString().padStart(2, '0');
    const sec = (s % 60).toString().padStart(2, '0');
    document.getElementById('foco-cronometro').textContent = m + ':' + sec;
}

async function finalizarTreino() {
    if (historicoRealizado.length === 0 && !confirm('Nenhum exercício foi marcado como realizado. Finalizar mesmo assim?')) return;

    const comentario = document.getElementById('treino-comentario').value || 'Treino concluído.';
    // Marcar exercícios pendentes como não feitos
    const finalizados = new Set(historicoRealizado.map(h => h.itemTreinoId));
    for (const ex of exercicios) {
        if (!finalizados.has(ex.idItem)) {
            historicoRealizado.push({ itemTreinoId: ex.idItem, carga: 0, feito: false });
        }
    }

    try {
        await api('/api/treino/finalizar', {
            method: 'POST',
            body: JSON.stringify({ alunoId: alunoId, treinoId: treinoIdAtual, comentario: comentario, itensRealizados: historicoRealizado })
        });
        alert('✅ Treino finalizado com sucesso!');
        carregarTreino();
        carregarFeed();
        carregarPerformance();
        mudarAba('feed');
    } catch (e) {
        alert('Erro ao finalizar treino.');
    }
}

/* ════════════════════════════════════════════════════════
   PERFORMANCE
   ════════════════════════════════════════════════════════ */
let graficoData = null;

async function carregarPerformance() {
    try {
        const d = await api('/api/aluno/dashboard?alunoId=' + alunoId);
        document.getElementById('perf-treinos-mes').textContent = d.treinosMes;
        document.getElementById('perf-total-treinos').textContent = d.totalTreinos;
        document.getElementById('perf-streak').textContent = d.streak;
        document.getElementById('perf-ultimo-treino').textContent = d.ultimoTreino || '---';
        document.getElementById('perf-ficha-ativa').textContent = d.fichaAtiva || 'Nenhuma';
        graficoData = d.treinosPorSemana || [];
        // Aguarda o canvas estar visível para desenhar
        setTimeout(desenharGrafico, 200);
    } catch (e) { /* silently fail */ }
}

function desenharGrafico() {
    if (!graficoData || !graficoData.length) return;
    const canvas = document.getElementById('chartCanvas');
    if (!canvas || canvas.offsetParent === null) return; // não está visível
    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    const w = rect.width;
    const h = 160;
    canvas.width = w * dpr;
    canvas.height = h * dpr;
    ctx.scale(dpr, dpr);

    const pad = { top: 16, bottom: 24, left: 8, right: 8 };
    const chartW = w - pad.left - pad.right;
    const chartH = h - pad.top - pad.bottom;

    const valores = graficoData.map(d => d.count);
    const max = Math.max(1, ...valores);
    const barW = Math.max(20, (chartW - (graficoData.length - 1) * 8) / graficoData.length);

    ctx.clearRect(0, 0, w, h);

    // Fundo
    ctx.fillStyle = '#1e1e1e';
    ctx.fillRect(0, 0, w, h);

    // Grid lines
    ctx.strokeStyle = '#2a2a2a';
    ctx.lineWidth = 1;
    for (let i = 0; i <= 4; i++) {
        const y = pad.top + (chartH / 4) * i;
        ctx.beginPath();
        ctx.moveTo(pad.left, y);
        ctx.lineTo(w - pad.right, y);
        ctx.stroke();
    }

    // Barras
    graficoData.forEach(function (d, i) {
        const x = pad.left + i * (barW + 8);
        const barH = (d.count / max) * chartH;
        const y = pad.top + chartH - barH;

        // Gradiente
        const grad = ctx.createLinearGradient(x, y, x, pad.top + chartH);
        grad.addColorStop(0, '#00d2ff');
        grad.addColorStop(1, '#3a7bd5');
        ctx.fillStyle = grad;

        // Barra arredondada (cantos superiores)
        const r = 4;
        ctx.beginPath();
        ctx.moveTo(x + r, y);
        ctx.lineTo(x + barW - r, y);
        ctx.quadraticCurveTo(x + barW, y, x + barW, y + r);
        ctx.lineTo(x + barW, pad.top + chartH);
        ctx.lineTo(x, pad.top + chartH);
        ctx.lineTo(x, y + r);
        ctx.quadraticCurveTo(x, y, x + r, y);
        ctx.closePath();
        ctx.fill();

        // Label (valor)
        ctx.fillStyle = '#fff';
        ctx.font = 'bold 11px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(d.count, x + barW / 2, y - 4);

        // Label (semana)
        ctx.fillStyle = '#888';
        ctx.font = '10px sans-serif';
        ctx.textAlign = 'center';
        const label = d.semana.length > 6 ? d.semana.replace('S', '') : d.semana;
        ctx.fillText(label, x + barW / 2, pad.top + chartH + 16);
    });
}

// Redesenhar em resize
window.addEventListener('resize', function () {
    if (document.getElementById('screen-performance').classList.contains('active')) {
        setTimeout(desenharGrafico, 200);
    }
});

/* ════════════════════════════════════════════════════════
   PERFIL
   ════════════════════════════════════════════════════════ */
async function carregarPerfil() {
    document.getElementById('perfil-nome').textContent = alunoNome;
    document.getElementById('perfil-email').textContent = alunoEmail;
    document.getElementById('perfil-avatar-inicial').textContent = alunoNome.charAt(0).toUpperCase() || '?';

    try {
        const d = await api('/api/aluno/perfil?alunoId=' + alunoId);
        document.getElementById('perfil-cpf').textContent = d.cpf || '---';
        document.getElementById('perfil-peso').textContent = d.peso ? d.peso + ' kg' : '---';
        document.getElementById('perfil-altura').textContent = d.altura ? d.altura + ' m' : '---';
        document.getElementById('perfil-imc').textContent = d.imc ? d.imc.toFixed(1) : '---';

        const avEl = document.getElementById('perfil-avaliacoes');
        if (d.avaliacoes && d.avaliacoes.length) {
            avEl.innerHTML =
                '<table class="av-table"><thead><tr><th>Data</th><th>Peso</th><th>Altura</th><th>IMC</th></tr></thead><tbody>' +
                d.avaliacoes.map(function (a) {
                    return '<tr><td>' + a.data + '</td><td>' + a.peso + ' kg</td><td>' + a.altura + ' m</td><td>' + (a.imc ? a.imc.toFixed(1) : '---') + '</td></tr>';
                }).join('') +
                '</tbody></table>';
        } else {
            avEl.innerHTML = '<div class="empty-state"><p>Nenhuma avaliação registrada.</p></div>';
        }

        document.getElementById('perfil-edit-peso').value = d.peso || '';
        document.getElementById('perfil-edit-altura').value = d.altura || '';
    } catch (e) { /* silently fail */ }
}

function toggleEditarPerfil() {
    document.getElementById('perfil-edit-form').classList.toggle('active');
}

async function salvarPerfil() {
    const peso = parseFloat(document.getElementById('perfil-edit-peso').value);
    const altura = parseFloat(document.getElementById('perfil-edit-altura').value);
    if (!peso || !altura) { alert('Preencha peso e altura.'); return; }
    try {
        await api('/api/aluno/perfil', {
            method: 'PUT',
            body: JSON.stringify({ alunoId: alunoId, peso: peso, altura: altura })
        });
        alert('Perfil atualizado!');
        toggleEditarPerfil();
        carregarPerfil();
    } catch (e) { alert('Erro ao atualizar.'); }
}

function logout() {
    localStorage.clear();
    window.location.href = 'login.html';
}
